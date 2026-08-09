#include "tile_merge.h"
#include "jni_utils.h"

#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace tupaz {
namespace pipeline {

void accumulate_tile_weighted(
    float* accum_r,
    float* accum_g,
    float* accum_b,
    float* accum_w,
    int dst_width,
    int dst_height,
    const uint8_t* src_tile,
    const TileBounds& bounds
) {
    if (accum_r == nullptr || accum_g == nullptr || accum_b == nullptr || accum_w == nullptr || src_tile == nullptr) return;

    int scale = (bounds.scale_factor > 0) ? bounds.scale_factor : 1;
    int scaled_x = bounds.x * scale;
    int scaled_y = bounds.y * scale;
    int scaled_w = bounds.width * scale;
    int scaled_h = bounds.height * scale;

    // Pre-calculate 1D raised-cosine window arrays to accelerate 2D tile weighting
    std::vector<float> wy(scaled_h, 1.0f);
    std::vector<float> wx(scaled_w, 1.0f);

    if (scaled_h > 1) {
        for (int ty = 0; ty < scaled_h; ++ty) {
            wy[ty] = static_cast<float>(std::sin(M_PI * (ty + 0.5) / scaled_h));
        }
    }
    if (scaled_w > 1) {
        for (int tx = 0; tx < scaled_w; ++tx) {
            wx[tx] = static_cast<float>(std::sin(M_PI * (tx + 0.5) / scaled_w));
        }
    }

    for (int ty = 0; ty < scaled_h; ++ty) {
        int dy = scaled_y + ty;
        if (dy < 0 || dy >= dst_height) continue;

        float weight_y = wy[ty];
        const uint8_t* src_row = src_tile + (ty * scaled_w) * 4;
        size_t dst_row_idx = static_cast<size_t>(dy * dst_width);

        for (int tx = 0; tx < scaled_w; ++tx) {
            int dx = scaled_x + tx;
            if (dx < 0 || dx >= dst_width) continue;

            float w = wx[tx] * weight_y;
            if (w < 1e-4f) w = 1e-4f;

            size_t dst_idx = dst_row_idx + dx;
            size_t src_idx = tx * 4;

            accum_r[dst_idx] += src_row[src_idx] * w;
            accum_g[dst_idx] += src_row[src_idx + 1] * w;
            accum_b[dst_idx] += src_row[src_idx + 2] * w;
            accum_w[dst_idx] += w;
        }
    }
}

void normalize_tile_accumulation(
    uint8_t* dst_rgba,
    const float* accum_r,
    const float* accum_g,
    const float* accum_b,
    const float* accum_w,
    int dst_width,
    int dst_height
) {
    if (dst_rgba == nullptr || accum_r == nullptr || accum_g == nullptr || accum_b == nullptr || accum_w == nullptr) return;

    size_t total_pixels = static_cast<size_t>(dst_width * dst_height);
    for (size_t i = 0; i < total_pixels; ++i) {
        float w = accum_w[i];
        if (w < 1e-5f) w = 1.0f;

        float inv_w = 1.0f / w;
        int r = std::clamp(static_cast<int>(accum_r[i] * inv_w + 0.5f), 0, 255);
        int g = std::clamp(static_cast<int>(accum_g[i] * inv_w + 0.5f), 0, 255);
        int b = std::clamp(static_cast<int>(accum_b[i] * inv_w + 0.5f), 0, 255);

        size_t idx = i * 4;
        dst_rgba[idx + 0] = static_cast<uint8_t>(r);
        dst_rgba[idx + 1] = static_cast<uint8_t>(g);
        dst_rgba[idx + 2] = static_cast<uint8_t>(b);
        dst_rgba[idx + 3] = 255; // Alpha
    }
}

namespace {
    thread_local std::vector<uint8_t> t_filter_temp;
}

void apply_cas_sharpen(
    uint8_t* rgba,
    int width,
    int height,
    float sharpness
) {
    if (rgba == nullptr || width <= 2 || height <= 2 || sharpness <= 0.001f) return;

    size_t total_bytes = static_cast<size_t>(width * height * 4);
    if (t_filter_temp.size() < total_bytes) {
        t_filter_temp.resize(total_bytes);
    }
    std::memcpy(t_filter_temp.data(), rgba, total_bytes);

    const uint8_t* temp = t_filter_temp.data();
    float peak = -0.125f * sharpness;

    #pragma omp parallel for schedule(static)
    for (int y = 1; y < height - 1; ++y) {
        size_t row_idx = static_cast<size_t>(y * width) * 4;
        size_t top_row_idx = static_cast<size_t>((y - 1) * width) * 4;
        size_t bot_row_idx = static_cast<size_t>((y + 1) * width) * 4;

        for (int x = 1; x < width - 1; ++x) {
            size_t idx = row_idx + x * 4;
            size_t left_idx = row_idx + (x - 1) * 4;
            size_t right_idx = row_idx + (x + 1) * 4;
            size_t top_idx = top_row_idx + x * 4;
            size_t bot_idx = bot_row_idx + x * 4;

            for (int c = 0; c < 3; ++c) {
                float e = temp[idx + c];
                float a = temp[top_idx + c];
                float b = temp[left_idx + c];
                float d = temp[right_idx + c];
                float f = temp[bot_idx + c];

                float min_c = std::min({e, a, b, d, f});
                float max_c = std::max({e, a, b, d, f});

                float amp = std::clamp(std::min(min_c, 255.0f - max_c) / (max_c + 1e-5f), 0.0f, 1.0f);
                float w = std::sqrt(amp) * peak;

                float res = (a * w + b * w + d * w + f * w + e) / (1.0f + 4.0f * w);
                rgba[idx + c] = static_cast<uint8_t>(std::clamp(res, 0.0f, 255.0f));
            }
        }
    }
}

void apply_character_pop_filter(
    uint8_t* rgba,
    int width,
    int height
) {
    if (rgba == nullptr || width <= 0 || height <= 0) return;

    float center_x = width * 0.5f;
    float center_y = height * 0.5f;
    float inv_max_dist_sq = 1.0f / (center_x * center_x + center_y * center_y);

    #pragma omp parallel for schedule(static)
    for (int y = 0; y < height; ++y) {
        float dy = y - center_y;
        float dy_sq = dy * dy;
        size_t row_idx = static_cast<size_t>(y * width) * 4;

        for (int x = 0; x < width; ++x) {
            float dx = x - center_x;
            float dist_ratio = (dx * dx + dy_sq) * inv_max_dist_sq;

            size_t idx = row_idx + static_cast<size_t>(x) * 4;

            float pr = rgba[idx + 0];
            float pg = rgba[idx + 1];
            float pb = rgba[idx + 2];

            // Subtle Natural Vibrance (Preserves skin tone warmth)
            float max_c = std::max({pr, pg, pb});
            float min_c = std::min({pr, pg, pb});
            float sat = (max_c - min_c) / (max_c + 1e-5f);

            // Subtle vibrance boost only for low-saturation background details
            float vibrance = 1.02f + (1.0f - sat) * 0.03f;
            float gray = 0.299f * pr + 0.587f * pg + 0.114f * pb;

            pr = std::clamp(gray + (pr - gray) * vibrance, 0.0f, 255.0f);
            pg = std::clamp(gray + (pg - gray) * vibrance, 0.0f, 255.0f);
            pb = std::clamp(gray + (pb - gray) * vibrance, 0.0f, 255.0f);

            // Subtle Subject Radial Focus Falloff
            float focus_factor = 1.0f - (dist_ratio * 0.02f);

            rgba[idx + 0] = static_cast<uint8_t>(std::clamp(pr * focus_factor, 0.0f, 255.0f));
            rgba[idx + 1] = static_cast<uint8_t>(std::clamp(pg * focus_factor, 0.0f, 255.0f));
            rgba[idx + 2] = static_cast<uint8_t>(std::clamp(pb * focus_factor, 0.0f, 255.0f));
        }
    }
}

void apply_hair_and_eye_enhancement(
    uint8_t* rgba,
    int width,
    int height
) {
    if (rgba == nullptr || width <= 2 || height <= 2) return;

    size_t total_bytes = static_cast<size_t>(width * height * 4);
    if (t_filter_temp.size() < total_bytes) {
        t_filter_temp.resize(total_bytes);
    }
    std::memcpy(t_filter_temp.data(), rgba, total_bytes);

    const uint8_t* src = t_filter_temp.data();

    #pragma omp parallel for schedule(static)
    for (int y = 1; y < height - 1; ++y) {
        size_t row_idx = static_cast<size_t>(y * width) * 4;
        size_t top_row_idx = static_cast<size_t>((y - 1) * width) * 4;
        size_t bot_row_idx = static_cast<size_t>((y + 1) * width) * 4;

        for (int x = 1; x < width - 1; ++x) {
            size_t idx = row_idx + x * 4;
            size_t left_idx = row_idx + (x - 1) * 4;
            size_t right_idx = row_idx + (x + 1) * 4;
            size_t top_idx = top_row_idx + x * 4;
            size_t bot_idx = bot_row_idx + x * 4;
            size_t top_left_idx = top_row_idx + (x - 1) * 4;
            size_t top_right_idx = top_row_idx + (x + 1) * 4;
            size_t bot_left_idx = bot_row_idx + (x - 1) * 4;
            size_t bot_right_idx = bot_row_idx + (x + 1) * 4;

            auto get_lum_ptr = [](const uint8_t* ptr) -> float {
                return 0.299f * ptr[0] + 0.587f * ptr[1] + 0.114f * ptr[2];
            };

            float lum_tl = get_lum_ptr(src + top_left_idx);
            float lum_tr = get_lum_ptr(src + top_right_idx);
            float lum_l  = get_lum_ptr(src + left_idx);
            float lum_r  = get_lum_ptr(src + right_idx);
            float lum_bl = get_lum_ptr(src + bot_left_idx);
            float lum_br = get_lum_ptr(src + bot_right_idx);
            float lum_t  = get_lum_ptr(src + top_idx);
            float lum_b  = get_lum_ptr(src + bot_idx);

            float gx = -1.0f * lum_tl + 1.0f * lum_tr
                       - 2.0f * lum_l  + 2.0f * lum_r
                       - 1.0f * lum_bl + 1.0f * lum_br;

            float gy = -1.0f * lum_tl - 2.0f * lum_t - 1.0f * lum_tr
                       + 1.0f * lum_bl + 2.0f * lum_b + 1.0f * lum_br;

            float grad_sq = gx * gx + gy * gy;

            // 2. High-Frequency Vector Line Art & Outline Sharpening
            if (grad_sq > 1225.0f) { // 35^2
                float lum = get_lum_ptr(src + idx);
                float line_darken = (lum < 140.0f) ? (0.92f + 0.08f * (lum / 140.0f)) : 1.0f;

                for (int c = 0; c < 3; ++c) {
                    float val = src[idx + c];
                    float blur = (src[top_idx + c] + src[bot_idx + c] + src[left_idx + c] + src[right_idx + c]) * 0.25f;
                    float detail = val - blur;
                    float enhanced = (val + detail * 1.15f) * line_darken;
                    rgba[idx + c] = static_cast<uint8_t>(std::clamp(enhanced, 0.0f, 255.0f));
                }
            } else {
                rgba[idx + 0] = src[idx + 0];
                rgba[idx + 1] = src[idx + 1];
                rgba[idx + 2] = src[idx + 2];
            }
        }
    }
}

void merge_tile_cosine(
    uint8_t* dst,
    int dst_width,
    int dst_height,
    const uint8_t* src,
    const TileBounds& bounds
) {
    if (dst == nullptr || src == nullptr) return;

    int scale = (bounds.scale_factor > 0) ? bounds.scale_factor : 1;
    int scaled_x = bounds.x * scale;
    int scaled_y = bounds.y * scale;
    int scaled_w = bounds.width * scale;
    int scaled_h = bounds.height * scale;

    int channels = 4;

    for (int ty = 0; ty < scaled_h; ++ty) {
        int dy = scaled_y + ty;
        if (dy < 0 || dy >= dst_height) continue;

        for (int tx = 0; tx < scaled_w; ++tx) {
            int dx = scaled_x + tx;
            if (dx < 0 || dx >= dst_width) continue;

            size_t dst_idx = static_cast<size_t>((dy * dst_width + dx) * channels);
            size_t src_idx = static_cast<size_t>((ty * scaled_w + tx) * channels);

            dst[dst_idx + 0] = src[src_idx + 0];
            dst[dst_idx + 1] = src[src_idx + 1];
            dst[dst_idx + 2] = src[src_idx + 2];
            dst[dst_idx + 3] = 255;
        }
    }
}

} // namespace pipeline
} // namespace tupaz
