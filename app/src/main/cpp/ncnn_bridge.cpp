#include "ncnn_bridge.h"
#include "jni_utils.h"
#include "tile_merge.h"
#include "frame_quality_scorer.h"

#include <mutex>
#include <atomic>
#include <chrono>
#include <vector>
#include <string>
#include <cmath>
#include <algorithm>
#include <cstring>

#if __has_include(<net.h>)
#include <net.h>
#include <gpu.h>
#include <cpu.h>
#define TUPAZ_HAS_NCNN 1
#elif __has_include(<ncnn/net.h>)
#include <ncnn/net.h>
#include <ncnn/gpu.h>
#include <ncnn/cpu.h>
#define TUPAZ_HAS_NCNN 1
#else
#define TUPAZ_HAS_NCNN 0
#endif

namespace {
    std::mutex g_engine_mutex;
    std::atomic<bool> g_initialized{false};
    std::atomic<bool> g_gpu_enabled{false};
    std::string g_param_file;
    std::string g_bin_file;

    // Persistent Accumulator Buffers (reallocated only when output resolution changes)
    std::vector<float> g_accum_r;
    std::vector<float> g_accum_g;
    std::vector<float> g_accum_b;
    std::vector<float> g_accum_w;
    std::vector<uint8_t> g_out_buffer;
    std::vector<uint8_t> g_tile_in_buf;
    std::vector<uint8_t> g_tile_out_buf;
    std::atomic<int> g_frame_counter{0};

#if TUPAZ_HAS_NCNN
    ncnn::Net* g_net = nullptr;
    bool g_vulkan_gpu_init = false;
#endif
}

JNIEXPORT jboolean JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeInit(
    JNIEnv* env,
    jobject clazz,
    jboolean use_gpu
) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    g_gpu_enabled.store(use_gpu == JNI_TRUE);

#if TUPAZ_HAS_NCNN
    if (use_gpu && !g_vulkan_gpu_init) {
        if (ncnn::get_gpu_count() > 0) {
            ncnn::create_gpu_instance();
            g_vulkan_gpu_init = true;
        } else {
            g_gpu_enabled.store(false);
            LOGI("No Vulkan GPU detected, falling back to CPU mode.");
        }
    }
#endif

    g_initialized.store(true);
    LOGI("Native NCNN bridge initialized (GPU: %s)", g_gpu_enabled.load() ? "enabled" : "disabled (CPU fallback)");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeInitModel(
    JNIEnv* env,
    jobject clazz,
    jstring param_path,
    jstring bin_path,
    jboolean use_gpu
) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);

    if (param_path != nullptr) {
        const char* p_str = env->GetStringUTFChars(param_path, nullptr);
        if (p_str != nullptr) {
            g_param_file = p_str;
            env->ReleaseStringUTFChars(param_path, p_str);
        }
    }

    if (bin_path != nullptr) {
        const char* b_str = env->GetStringUTFChars(bin_path, nullptr);
        if (b_str != nullptr) {
            g_bin_file = b_str;
            env->ReleaseStringUTFChars(bin_path, b_str);
        }
    }

    g_gpu_enabled.store(use_gpu == JNI_TRUE);

    LOGI("[Tupaz-Native] nativeInitModel checkpoint 6: Loading model files (param=%s, bin=%s, use_gpu=%d)",
         g_param_file.c_str(), g_bin_file.c_str(), use_gpu);

    FILE* fp_p = fopen(g_param_file.c_str(), "rb");
    long p_size = -1;
    if (fp_p) {
        fseek(fp_p, 0, SEEK_END);
        p_size = ftell(fp_p);
        fclose(fp_p);
    }

    FILE* fp_b = fopen(g_bin_file.c_str(), "rb");
    long b_size = -1;
    if (fp_b) {
        fseek(fp_b, 0, SEEK_END);
        b_size = ftell(fp_b);
        fclose(fp_b);
    }

    LOGI("[Tupaz-Native] Model file discovery: paramExists=%s (%ld bytes), binExists=%s (%ld bytes)",
         (p_size >= 0 ? "YES" : "NO"), p_size,
         (b_size >= 0 ? "YES" : "NO"), b_size);

    if (p_size < 0 || b_size < 0) {
        LOGE("[Tupaz-Native] CRITICAL FAIL: Model files NOT found or unreadable!");
        g_initialized.store(false);
        return JNI_FALSE;
    }

#if TUPAZ_HAS_NCNN
    if (use_gpu && !g_vulkan_gpu_init) {
        int gpu_cnt = ncnn::get_gpu_count();
        LOGI("[Tupaz-Native] Checkpoint 8: Vulkan GPU count: %d", gpu_cnt);
        if (gpu_cnt > 0) {
            ncnn::create_gpu_instance();
            g_vulkan_gpu_init = true;
            LOGI("[Tupaz-Native] Vulkan GPU instance created successfully.");
        } else {
            g_gpu_enabled.store(false);
            LOGI("[Tupaz-Native] No Vulkan GPU detected, falling back to CPU mode.");
        }
    }

    if (g_net != nullptr) {
        delete g_net;
        g_net = nullptr;
    }

    g_net = new ncnn::Net();
    g_net->opt.use_vulkan_compute = g_gpu_enabled.load();
    g_net->opt.use_fp16_packed = true;
    g_net->opt.use_fp16_storage = true;
    g_net->opt.use_fp16_arithmetic = true;
    g_net->opt.use_packing_layout = true;
    g_net->opt.num_threads = ncnn::get_big_cpu_count();

    int p_ret = g_net->load_param(g_param_file.c_str());
    int b_ret = g_net->load_model(g_bin_file.c_str());
    LOGI("[Tupaz-Native] NCNN load_param result: %d, load_model result: %d", p_ret, b_ret);

    if (p_ret != 0 || b_ret != 0) {
        LOGE("[Tupaz-Native] CRITICAL FAIL: NCNN loadModel failed (p_ret=%d, b_ret=%d)", p_ret, b_ret);
        delete g_net;
        g_net = nullptr;
        g_initialized.store(false);
        return JNI_FALSE;
    }

    // One-time Vulkan runtime diagnostics (logged once at model load)
    if (g_gpu_enabled.load() && ncnn::get_gpu_count() > 0) {
        const ncnn::GpuInfo& gpu_info = ncnn::get_gpu_info(0);
        const char* dev_name = gpu_info.device_name();
        LOGI("[Tupaz-Vulkan] === RUNTIME DIAGNOSTICS ===");
        LOGI("[Tupaz-Vulkan] use_vulkan_compute = %s", g_net->opt.use_vulkan_compute ? "TRUE" : "FALSE");
        LOGI("[Tupaz-Vulkan] GPU device name = %s", dev_name ? dev_name : "unknown");
        LOGI("[Tupaz-Vulkan] Vulkan GPU count = %d", ncnn::get_gpu_count());
        LOGI("[Tupaz-Vulkan] use_fp16_packed = %s", g_net->opt.use_fp16_packed ? "TRUE" : "FALSE");
        LOGI("[Tupaz-Vulkan] use_fp16_storage = %s", g_net->opt.use_fp16_storage ? "TRUE" : "FALSE");
        LOGI("[Tupaz-Vulkan] use_fp16_arithmetic = %s", g_net->opt.use_fp16_arithmetic ? "TRUE" : "FALSE");
        LOGI("[Tupaz-Vulkan] use_packing_layout = %s", g_net->opt.use_packing_layout ? "TRUE" : "FALSE");
        LOGI("[Tupaz-Vulkan] num_threads = %d", g_net->opt.num_threads);
    } else {
        LOGI("[Tupaz-Vulkan] GPU DISABLED - running in CPU fallback mode");
    }
#else
    LOGE("[Tupaz-Native] CRITICAL FAIL: TUPAZ_HAS_NCNN is 0! NCNN headers/libraries were NOT compiled into build!");
    g_initialized.store(false);
    return JNI_FALSE;
#endif

    g_initialized.store(true);
    LOGI("[Tupaz-Native] Checkpoint 7: Model loaded successfully! (GPU: %s)", g_gpu_enabled.load() ? "enabled" : "disabled");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeDestroy(
    JNIEnv* env,
    jobject clazz
) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    if (!g_initialized.load()) {
        return;
    }

#if TUPAZ_HAS_NCNN
    if (g_net != nullptr) {
        delete g_net;
        g_net = nullptr;
    }
    if (g_vulkan_gpu_init) {
        ncnn::destroy_gpu_instance();
        g_vulkan_gpu_init = false;
    }
#endif

    g_initialized.store(false);
    g_gpu_enabled.store(false);
    g_param_file.clear();
    g_bin_file.clear();
    LOGI("Native NCNN bridge destroyed");
}

JNIEXPORT jbyteArray JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeProcessFrame(
    JNIEnv* env,
    jobject clazz,
    jbyteArray input_frame,
    jint width,
    jint height,
    jint scale_factor,
    jint mode
) {
    if (!g_initialized.load()) {
        LOGE("nativeProcessFrame called before initialization");
        return nullptr;
    }

    if (input_frame == nullptr || width <= 0 || height <= 0) {
        LOGE("Invalid parameters passed to nativeProcessFrame");
        return nullptr;
    }

    int scale = (scale_factor >= 4) ? 4 : ((scale_factor <= 1) ? 1 : 2);
    int out_width = width * scale;
    int out_height = height * scale;
    size_t out_pixels = static_cast<size_t>(out_width * out_height);
    size_t out_length = out_pixels * 4;

    tupaz::jni::ScopedByteArray scoped_input(env, input_frame);
    if (!scoped_input.valid()) {
        LOGE("Failed to lock input frame byte array");
        return nullptr;
    }

    const uint8_t* in_ptr = reinterpret_cast<const uint8_t*>(scoped_input.get());

    // Validate input buffer length matches declared dimensions (prevents OOB reads)
    jsize input_len = env->GetArrayLength(input_frame);
    size_t expected_input_len = static_cast<size_t>(width) * static_cast<size_t>(height) * 4u;
    if (static_cast<size_t>(input_len) < expected_input_len) {
        LOGE("nativeProcessFrame input buffer too small: got %d bytes, expected %zu",
             static_cast<int>(input_len), expected_input_len);
        return nullptr;
    }

    // Lock global buffer access for thread safety
    std::lock_guard<std::mutex> buffer_lock(g_engine_mutex);

    // Reuse persistent buffers to prevent heap churn (malloc/free of ~56MB per frame)
    if (g_accum_r.size() < out_pixels) {
        g_accum_r.resize(out_pixels);
        g_accum_g.resize(out_pixels);
        g_accum_b.resize(out_pixels);
        g_accum_w.resize(out_pixels);
    }
    if (g_out_buffer.size() < out_length) {
        g_out_buffer.resize(out_length);
    }

    // IEEE 754: float 0.0f is all-zero bytes, safe for memset
    std::memset(g_accum_r.data(), 0, out_pixels * sizeof(float));
    std::memset(g_accum_g.data(), 0, out_pixels * sizeof(float));
    std::memset(g_accum_b.data(), 0, out_pixels * sizeof(float));
    std::memset(g_accum_w.data(), 0, out_pixels * sizeof(float));
    std::memset(g_out_buffer.data(), 0, out_length);

    float* accum_r_ptr = g_accum_r.data();
    float* accum_g_ptr = g_accum_g.data();
    float* accum_b_ptr = g_accum_b.data();
    float* accum_w_ptr = g_accum_w.data();
    uint8_t* out_buf_ptr = g_out_buffer.data();

    // Profiling state (set in the NCNN path, checked after post-processing)
    auto frame_perf_start = std::chrono::high_resolution_clock::now();
    auto frame_perf_infer_done = frame_perf_start;
    int current_frame_idx = -1;
    bool is_single_tile = false;

    if (scale == 1) {
        // Filter-only pass (e.g. CAS sharpen / post-process stages).
        // The loaded NCNN model (realesr-animevideov3-x2) is a FIXED 2x upscaler;
        // running it with scale==1 would emit a 2x tensor into a 1x-sized tile_out
        // buffer => heap buffer overflow / SIGSEGV. Copy input 1:1 instead and let
        // the enhancement filters below run on this buffer.
        std::memcpy(out_buf_ptr, in_ptr, expected_input_len);
    } else {
    // Optimal Tile Size Selection:
    // For <= 640x480, use 640px tile (single tile, zero blending overhead)
    // For 720p: tile_size=768 → step=736 ≥ 720 → tiles_y=1, only 2 tiles (eliminates tiny bottom-row remainders)
    // For 1080p+, use 512px tiles to stay within Vulkan VRAM limits
    int tile_size = 512;
    if (width <= 640 && height <= 480) tile_size = 640;
    else if (width <= 1280 && height <= 720) tile_size = 768;
    else if (width * height >= 1920 * 1080) tile_size = 512;
    else tile_size = 384;

    int pad = 16; // 16px tile padding for edge artifact prevention
    int step_size = tile_size - pad * 2;
    if (step_size <= 0) step_size = tile_size / 2;

    int tiles_x = (width + step_size - 1) / step_size;
    int tiles_y = (height + step_size - 1) / step_size;
    is_single_tile = (tiles_x == 1 && tiles_y == 1);
    current_frame_idx = g_frame_counter.fetch_add(1);
    frame_perf_start = std::chrono::high_resolution_clock::now();

    LOGI("[Tupaz-NCNN] Frame %d: %dx%d -> %dx%d (%dx scale, tile_size=%d, step=%d, tiles=%dx%d=%d, single_tile=%s, GPU: %s)",
         current_frame_idx, width, height, out_width, out_height, scale, tile_size, step_size,
         tiles_x, tiles_y, tiles_x * tiles_y, is_single_tile ? "YES" : "NO",
         g_gpu_enabled.load() ? "Vulkan" : "CPU");

#if !TUPAZ_HAS_NCNN
    LOGE("CRITICAL FAIL: TUPAZ_HAS_NCNN is 0! NCNN engine is not compiled!");
    return nullptr;
#else
    if (g_net == nullptr) {
        LOGE("CRITICAL FAIL: Model initialized = false (g_net is nullptr!)");
        return nullptr;
    }
    LOGI("Model initialized = true");
    LOGI("Input size = %dx%d", width, height);

    const float norm_vals[3] = { 1.0f / 255.0f, 1.0f / 255.0f, 1.0f / 255.0f };
    const float denorm_vals[3] = { 255.0f, 255.0f, 255.0f };

    // Accumulators for Frame 0 summary logging
    double tot_extract = 0, tot_rgba2rgb = 0, tot_norm = 0, tot_create_ex = 0;
    double tot_input = 0, tot_extract_gpu = 0, tot_out_conv = 0, tot_blend = 0;

    // Process tiles sequentially — Vulkan GPU handles parallelism internally
    for (int ty = 0; ty < tiles_y; ++ty) {
        for (int tx = 0; tx < tiles_x; ++tx) {
            int tile_idx = ty * tiles_x + tx;
            int cur_x = tx * step_size;
            int cur_y = ty * step_size;
            int cur_w = std::min(tile_size, width - cur_x);
            int cur_h = std::min(tile_size, height - cur_y);

            auto p0 = std::chrono::high_resolution_clock::now();

            // 1. Extract input tile with padding for edge-aware inference
            int tile_in_x = std::max(0, cur_x - pad);
            int tile_in_y = std::max(0, cur_y - pad);
            int tile_in_w = std::min(width, cur_x + cur_w + pad) - tile_in_x;
            int tile_in_h = std::min(height, cur_y + cur_h + pad) - tile_in_y;

            // Persistent tile input buffer — reused across tiles and frames
            size_t tile_in_needed = static_cast<size_t>(tile_in_w) * tile_in_h * 4;
            if (g_tile_in_buf.size() < tile_in_needed) {
                g_tile_in_buf.resize(tile_in_needed);
            }
            for (int row = 0; row < tile_in_h; ++row) {
                const uint8_t* src_row = in_ptr + static_cast<size_t>((tile_in_y + row) * width + tile_in_x) * 4;
                uint8_t* dst_row = g_tile_in_buf.data() + static_cast<size_t>(row * tile_in_w) * 4;
                std::memcpy(dst_row, src_row, tile_in_w * 4);
            }

            auto p1 = std::chrono::high_resolution_clock::now();

            // 2. RGBA -> RGB conversion
            ncnn::Mat in_mat = ncnn::Mat::from_pixels(g_tile_in_buf.data(), ncnn::Mat::PIXEL_RGBA2RGB, tile_in_w, tile_in_h);

            auto p2 = std::chrono::high_resolution_clock::now();

            // 3. Normalization
            in_mat.substract_mean_normalize(0, norm_vals);

            auto p3 = std::chrono::high_resolution_clock::now();

            // 4. Create Extractor
            ncnn::Extractor ex = g_net->create_extractor();
            ex.set_light_mode(true);

            auto p4 = std::chrono::high_resolution_clock::now();

            // 5. Input blob
            if (ex.input("data", in_mat) != 0) {
                LOGE("CRITICAL FAIL: NCNN input blob 'data' failed for tile %d,%d", tx, ty);
                return nullptr;
            }

            auto p5 = std::chrono::high_resolution_clock::now();

            // 6. Extract / Vulkan GPU Inference
            ncnn::Mat out_mat;
            if (ex.extract("output", out_mat) != 0 || out_mat.empty()) {
                LOGE("CRITICAL FAIL: NCNN output blob 'output' extraction failed for tile %d,%d", tx, ty);
                return nullptr;
            }

            auto p6 = std::chrono::high_resolution_clock::now();

            // 7. Output Conversion
            int out_tile_w = cur_w * scale;
            int out_tile_h = cur_h * scale;

            if (is_single_tile && !out_mat.empty()) {
                // Single-tile fast path: write directly to output buffer
                if (out_mat.w == out_width && out_mat.h == out_height) {
                    out_mat.substract_mean_normalize(0, denorm_vals);
                    out_mat.to_pixels(out_buf_ptr, ncnn::Mat::PIXEL_RGB2RGBA);
                } else {
                    LOGE("[Tupaz-NCNN] Single-tile output %dx%d != expected %dx%d",
                         out_mat.w, out_mat.h, out_width, out_height);
                    return nullptr;
                }
            } else {
                // Multi-tile path: persistent output buffer + weighted blending
                size_t tile_out_needed = static_cast<size_t>(out_tile_w) * out_tile_h * 4;
                if (g_tile_out_buf.size() < tile_out_needed) {
                    g_tile_out_buf.resize(tile_out_needed);
                }
                std::memset(g_tile_out_buf.data(), 0, tile_out_needed);

                if (!out_mat.empty()) {
                    int pad_top = (cur_y - tile_in_y) * scale;
                    int pad_bottom = (tile_in_y + tile_in_h - cur_y - cur_h) * scale;
                    int pad_left = (cur_x - tile_in_x) * scale;
                    int pad_right = (tile_in_x + tile_in_w - cur_x - cur_w) * scale;

                    int expected_out_w = pad_left + out_tile_w + pad_right;
                    int expected_out_h = pad_top + out_tile_h + pad_bottom;

                    if (out_mat.w == expected_out_w && out_mat.h == expected_out_h) {
                        ncnn::Mat cropped_mat;
                        ncnn::copy_cut_border(out_mat, cropped_mat, pad_top, pad_bottom, pad_left, pad_right);
                        cropped_mat.substract_mean_normalize(0, denorm_vals);
                        cropped_mat.to_pixels(g_tile_out_buf.data(), ncnn::Mat::PIXEL_RGB2RGBA);
                    } else {
                        LOGE("NCNN output dimensions %dx%d differ from expected %dx%d",
                             out_mat.w, out_mat.h, expected_out_w, expected_out_h);
                        return nullptr;
                    }
                }
            }

            auto p7 = std::chrono::high_resolution_clock::now();

            // 8. Tile Blending
            if (!is_single_tile) {
                tupaz::pipeline::TileBounds bounds{cur_x, cur_y, cur_w, cur_h, scale};
                tupaz::pipeline::accumulate_tile_weighted(
                    accum_r_ptr, accum_g_ptr, accum_b_ptr, accum_w_ptr,
                    out_width, out_height, g_tile_out_buf.data(), bounds
                );
            }

            auto p8 = std::chrono::high_resolution_clock::now();

            double extract_ms = std::chrono::duration<double, std::milli>(p1 - p0).count();
            double rgba2rgb_ms = std::chrono::duration<double, std::milli>(p2 - p1).count();
            double norm_ms = std::chrono::duration<double, std::milli>(p3 - p2).count();
            double create_ex_ms = std::chrono::duration<double, std::milli>(p4 - p3).count();
            double input_ms = std::chrono::duration<double, std::milli>(p5 - p4).count();
            double extract_ms_gpu = std::chrono::duration<double, std::milli>(p6 - p5).count();
            double out_conv_ms = std::chrono::duration<double, std::milli>(p7 - p6).count();
            double blend_ms = std::chrono::duration<double, std::milli>(p8 - p7).count();
            double tile_total_ms = std::chrono::duration<double, std::milli>(p8 - p0).count();

            if (current_frame_idx == 0) {
                LOGI("[Tupaz-TilePerf] tile=%d (%dx%d tile_in) extract=%.2fms rgba2rgb=%.2fms norm=%.2fms create_ex=%.2fms input=%.2fms extract_gpu=%.2fms out_conv=%.2fms blend=%.2fms total=%.2fms",
                     tile_idx, tile_in_w, tile_in_h,
                     extract_ms, rgba2rgb_ms, norm_ms, create_ex_ms, input_ms, extract_ms_gpu, out_conv_ms, blend_ms, tile_total_ms);

                tot_extract += extract_ms;
                tot_rgba2rgb += rgba2rgb_ms;
                tot_norm += norm_ms;
                tot_create_ex += create_ex_ms;
                tot_input += input_ms;
                tot_extract_gpu += extract_ms_gpu;
                tot_out_conv += out_conv_ms;
                tot_blend += blend_ms;
            }
        }
    }

    if (current_frame_idx == 0) {
        LOGI("[Tupaz-TilePerfSummary] Frame 0 totals across %d tiles: tile_extract=%.2fms, rgba2rgb=%.2fms, norm=%.2fms, create_ex=%.2fms, input=%.2fms, extract_gpu=%.2fms, out_conv=%.2fms, blend=%.2fms",
             tiles_x * tiles_y, tot_extract, tot_rgba2rgb, tot_norm, tot_create_ex, tot_input, tot_extract_gpu, tot_out_conv, tot_blend);
    }
#endif


    // Normalize weighted tile accumulation (skip for single-tile fast path)
    if (!is_single_tile) {
        tupaz::pipeline::normalize_tile_accumulation(
            out_buf_ptr,
            accum_r_ptr, accum_g_ptr, accum_b_ptr, accum_w_ptr,
            out_width, out_height
        );
    }

    frame_perf_infer_done = std::chrono::high_resolution_clock::now();
    }

    // Gentle Contrast Adaptive Sharpening (0.10f for natural visual clarity without over-exposure)
    tupaz::pipeline::apply_cas_sharpen(out_buf_ptr, out_width, out_height, 0.10f);

    // Eyelash, Pupil, Hair Strand & Vector Line Art Refinement (Preserves smooth skin 100%)
    tupaz::pipeline::apply_hair_and_eye_enhancement(out_buf_ptr, out_width, out_height);

    // Natural Color Vibrance & Subject Separation (No skin whitening)
    tupaz::pipeline::apply_character_pop_filter(out_buf_ptr, out_width, out_height);

    // Frame performance breakdown (first 2 frames only to avoid log spam)
    if (current_frame_idx >= 0 && current_frame_idx < 2) {
        auto frame_perf_end = std::chrono::high_resolution_clock::now();
        long total_ms = std::chrono::duration_cast<std::chrono::milliseconds>(frame_perf_end - frame_perf_start).count();
        long infer_ms = std::chrono::duration_cast<std::chrono::milliseconds>(frame_perf_infer_done - frame_perf_start).count();
        long post_ms = std::chrono::duration_cast<std::chrono::milliseconds>(frame_perf_end - frame_perf_infer_done).count();
        LOGI("[Tupaz-Perf] Frame %d breakdown: total=%ldms, ncnn+tiling=%ldms, postprocess=%ldms, tiles=%s",
             current_frame_idx, total_ms, infer_ms, post_ms, is_single_tile ? "1(fast)" : "multi");
    }

    // Safely allocate JNI byte array after neural inference completes
    jbyteArray output_array = env->NewByteArray(static_cast<jsize>(out_length));
    if (output_array == nullptr) {
        LOGE("NewByteArray failed for size %zu (OOM) - downsampling to 2x for JNI safety", out_length);
        // Fallback for 4x high-res OOM: downsample 4x tensor to 2x resolution safely
        int safe_w = width * 2;
        int safe_h = height * 2;
        size_t safe_len = static_cast<size_t>(safe_w * safe_h * 4);
        jbyteArray safe_array = env->NewByteArray(static_cast<jsize>(safe_len));
        if (safe_array == nullptr) {
            LOGE("Fallback NewByteArray allocation failed for size %zu", safe_len);
            return nullptr;
        }

        std::vector<uint8_t> safe_buf(safe_len, 255);
        for (int y = 0; y < safe_h; ++y) {
            int src_y = y * 2;
            for (int x = 0; x < safe_w; ++x) {
                int src_x = x * 2;
                size_t src_i = (src_y * out_width + src_x) * 4;
                size_t dst_i = (y * safe_w + x) * 4;
                safe_buf[dst_i + 0] = out_buf_ptr[src_i + 0];
                safe_buf[dst_i + 1] = out_buf_ptr[src_i + 1];
                safe_buf[dst_i + 2] = out_buf_ptr[src_i + 2];
                safe_buf[dst_i + 3] = 255;
            }
        }
        env->SetByteArrayRegion(safe_array, 0, static_cast<jsize>(safe_len), reinterpret_cast<const jbyte*>(safe_buf.data()));
        return safe_array;
    }

    env->SetByteArrayRegion(output_array, 0, static_cast<jsize>(out_length), reinterpret_cast<const jbyte*>(out_buf_ptr));
    return output_array;
}

JNIEXPORT jlong JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeGetVramUsage(
    JNIEnv* env,
    jobject clazz
) {
    if (!g_initialized.load()) {
        return 0;
    }
    return g_gpu_enabled.load() ? 256L : 64L;
}

JNIEXPORT jfloatArray JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeScoreFrameQuality(
    JNIEnv* env,
    jobject clazz,
    jbyteArray input_frame,
    jint width,
    jint height
) {
    if (input_frame == nullptr || width <= 0 || height <= 0) {
        return nullptr;
    }

    tupaz::jni::ScopedByteArray scoped_input(env, input_frame);
    if (!scoped_input.valid()) {
        return nullptr;
    }

    const uint8_t* in_ptr = reinterpret_cast<const uint8_t*>(scoped_input.get());
    tupaz::quality::FrameQualityResult res = tupaz::quality::score_frame_quality(in_ptr, width, height);

    jfloatArray result_array = env->NewFloatArray(4);
    if (result_array == nullptr) return nullptr;

    jfloat scores[4] = { res.blockingScore, res.noiseLevel, res.sharpnessScore, res.overallQuality };
    env->SetFloatArrayRegion(result_array, 0, 4, scores);

    return result_array;
}

