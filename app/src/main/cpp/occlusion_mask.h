#ifndef TUPAZ_OCCLUSION_MASK_H_
#define TUPAZ_OCCLUSION_MASK_H_

#include <cstdint>
#include "rife_flow.h"

namespace tupaz {
namespace flow {

/**
 * @brief Performs forward-backward optical flow consistency check to detect occluded pixels.
 * @param forward_flow Forward motion vectors (width x height).
 * @param backward_flow Backward motion vectors (width x height).
 * @param width Image width in pixels.
 * @param height Image height in pixels.
 * @param occlusion_mask_out Output confidence mask (0 = occluded, 255 = confident).
 */
void compute_occlusion_mask(
    const MotionVector* forward_flow,
    const MotionVector* backward_flow,
    int width,
    int height,
    uint8_t* occlusion_mask_out
);

} // namespace flow
} // namespace tupaz

#endif // TUPAZ_OCCLUSION_MASK_H_
