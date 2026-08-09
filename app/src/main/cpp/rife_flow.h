#ifndef TUPAZ_RIFE_FLOW_H_
#define TUPAZ_RIFE_FLOW_H_

#include <cstdint>
#include <vector>

namespace tupaz {
namespace flow {

struct MotionVector {
    float dx;
    float dy;
};

/**
 * @brief Computes dense optical flow field between two consecutive frames using RIFE-Lite.
 * @param frame_a First frame RGBA buffer.
 * @param frame_b Second frame RGBA buffer.
 * @param width Frame width in pixels.
 * @param height Frame height in pixels.
 * @param flow_output Output motion vector buffer (width x height).
 */
void compute_rife_flow(
    const uint8_t* frame_a,
    const uint8_t* frame_b,
    int width,
    int height,
    MotionVector* flow_output
);

} // namespace flow
} // namespace tupaz

#endif // TUPAZ_RIFE_FLOW_H_
