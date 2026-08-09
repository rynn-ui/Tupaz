#include "occlusion_mask.h"
#include "jni_utils.h"

#include <cmath>
#include <algorithm>

namespace tupaz {
namespace flow {

void compute_occlusion_mask(
    const MotionVector* forward_flow,
    const MotionVector* backward_flow,
    int width,
    int height,
    uint8_t* occlusion_mask_out
) {
    if (forward_flow == nullptr || backward_flow == nullptr || occlusion_mask_out == nullptr) {
        return;
    }

    size_t num_pixels = static_cast<size_t>(width * height);

    for (size_t i = 0; i < num_pixels; ++i) {
        float f_dx = forward_flow[i].dx;
        float f_dy = forward_flow[i].dy;

        float b_dx = backward_flow[i].dx;
        float b_dy = backward_flow[i].dy;

        // Sum of forward and backward flow should be close to zero if non-occluded
        float sum_dx = f_dx + b_dx;
        float sum_dy = f_dy + b_dy;
        float error_sq = sum_dx * sum_dx + sum_dy * sum_dy;

        if (error_sq > 4.0f) {
            occlusion_mask_out[i] = 0; // Occluded / ghosting risk
        } else {
            occlusion_mask_out[i] = 255; // Valid flow
        }
    }
}

} // namespace flow
} // namespace tupaz
