#include "artifact_classifier.h"
#include "jni_utils.h"

#include <cmath>
#include <algorithm>

namespace tupaz {
namespace artifact {

double compute_blocking_score(
    const uint8_t* frame,
    int width,
    int height
) {
    if (frame == nullptr || width < 16 || height < 16) {
        return 0.0;
    }

    double boundary_diff = 0.0;
    double internal_diff = 0.0;
    size_t boundary_count = 0;
    size_t internal_count = 0;

    int channels = 4; // RGBA

    // Inspect 8x8 block boundary vs internal gradients
    for (int y = 8; y < height - 8; y += 8) {
        for (int x = 8; x < width - 8; x += 8) {
            size_t b_idx1 = static_cast<size_t>((y * width + (x - 1)) * channels);
            size_t b_idx2 = static_cast<size_t>((y * width + x) * channels);

            double diff_b = std::abs(frame[b_idx1] - frame[b_idx2]);
            boundary_diff += diff_b;
            boundary_count++;

            size_t i_idx1 = static_cast<size_t>((y * width + (x + 2)) * channels);
            size_t i_idx2 = static_cast<size_t>((y * width + (x + 3)) * channels);

            double diff_i = std::abs(frame[i_idx1] - frame[i_idx2]);
            internal_diff += diff_i;
            internal_count++;
        }
    }

    if (boundary_count == 0 || internal_count == 0) return 0.0;

    double avg_boundary = boundary_diff / boundary_count;
    double avg_internal = internal_diff / internal_count;

    double ratio = (avg_internal > 1e-3) ? (avg_boundary / avg_internal) : 1.0;
    double score = std::clamp((ratio - 1.0) / 2.0, 0.0, 1.0);

    return score;
}

} // namespace artifact
} // namespace tupaz
