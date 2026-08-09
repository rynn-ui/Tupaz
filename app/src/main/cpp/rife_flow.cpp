#include "rife_flow.h"
#include "jni_utils.h"

#include <cmath>

namespace tupaz {
namespace flow {

void compute_rife_flow(
    const uint8_t* frame_a,
    const uint8_t* frame_b,
    int width,
    int height,
    MotionVector* flow_output
) {
    if (frame_a == nullptr || frame_b == nullptr || flow_output == nullptr || width <= 0 || height <= 0) {
        return;
    }

    size_t num_pixels = static_cast<size_t>(width * height);
    int channels = 4; // RGBA

    for (size_t i = 0; i < num_pixels; ++i) {
        size_t idx = i * channels;
        float diff_r = static_cast<float>(frame_b[idx] - frame_a[idx]);
        float diff_g = static_cast<float>(frame_b[idx + 1] - frame_a[idx + 1]);

        // Synthetic flow vector estimation
        flow_output[i].dx = diff_r * 0.1f;
        flow_output[i].dy = diff_g * 0.1f;
    }
}

} // namespace flow
} // namespace tupaz
