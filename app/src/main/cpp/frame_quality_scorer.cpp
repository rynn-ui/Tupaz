#include "frame_quality_scorer.h"
#include "artifact_classifier.h"
#include <cmath>
#include <algorithm>

namespace tupaz {
namespace quality {

FrameQualityResult score_frame_quality(
    const uint8_t* frame,
    int width,
    int height
) {
    FrameQualityResult result{0.0f, 0.0f, 0.0f, 0.0f};

    if (frame == nullptr || width < 16 || height < 16) {
        return result;
    }

    // 1. Blocking score from existing artifact classifier
    double blocking = tupaz::artifact::compute_blocking_score(frame, width, height);
    result.blockingScore = static_cast<float>(blocking);

    // 2. Strided single-pass analysis for Noise & Sharpness (sample every 2nd row & col for <1ms speed)
    int stride = 2;
    int channels = 4; // RGBA
    double grad_sum = 0.0;
    double var_sum = 0.0;
    size_t count = 0;

    for (int y = stride; y < height - stride; y += stride) {
        size_t row_offset = static_cast<size_t>(y * width) * channels;
        size_t prev_row_offset = static_cast<size_t>((y - stride) * width) * channels;
        size_t next_row_offset = static_cast<size_t>((y + stride) * width) * channels;

        for (int x = stride; x < width - stride; x += stride) {
            size_t idx = row_offset + x * channels;

            // Luminance estimation (Green channel + Fast Approx)
            float c = frame[idx + 1];
            float l = frame[idx - stride * channels + 1];
            float r = frame[idx + stride * channels + 1];
            float t = frame[prev_row_offset + x * channels + 1];
            float b = frame[next_row_offset + x * channels + 1];

            // Horizontal & Vertical Gradients (Sharpness indicator)
            float dx = std::abs(r - l);
            float dy = std::abs(b - t);
            float grad = dx + dy;
            grad_sum += grad;

            // Laplacian 2nd-derivative (Noise estimate in local uniform regions)
            float laplacian = std::abs(4.0f * c - l - r - t - b);
            if (grad < 15.0f) { // Only sample flat regions for noise measurement
                var_sum += laplacian;
            }

            count++;
        }
    }

    if (count > 0) {
        float avg_grad = static_cast<float>(grad_sum / count);
        float avg_noise = static_cast<float>(var_sum / count);

        // Normalize sharpness (typical high quality frame avg grad ~ 15-40+)
        result.sharpnessScore = std::clamp(avg_grad / 30.0f, 0.0f, 1.0f);

        // Normalize noise (clean frame avg laplacian in flat regions < 3.0)
        result.noiseLevel = std::clamp(avg_noise / 20.0f, 0.0f, 1.0f);
    }

    // Composite quality formula: high quality = low blocking + low noise + reasonable sharpness
    // Quality = (1 - blocking) * 0.4 + (1 - noise) * 0.3 + (sharpness) * 0.3
    float quality = (1.0f - result.blockingScore) * 0.4f +
                    (1.0f - result.noiseLevel) * 0.3f +
                    result.sharpnessScore * 0.3f;

    result.overallQuality = std::clamp(quality, 0.0f, 1.0f);
    return result;
}

} // namespace quality
} // namespace tupaz
