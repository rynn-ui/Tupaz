#ifndef TUPAZ_FACE_MERGE_H_
#define TUPAZ_FACE_MERGE_H_

#include <cstdint>

namespace tupaz {
namespace face {

/**
 * @brief Blends restored face crop back into the full frame using smooth alpha feathering.
 * @param frame Full frame RGBA byte array.
 * @param frame_width Full frame width in pixels.
 * @param frame_height Full frame height in pixels.
 * @param face_crop Restored face crop RGBA byte array.
 * @param crop_width Crop width in pixels.
 * @param crop_height Crop height in pixels.
 * @param dst_x Target X position in full frame.
 * @param dst_y Target Y position in full frame.
 * @param dst_w Target width in full frame.
 * @param dst_h Target height in full frame.
 */
void merge_face_crop(
    uint8_t* frame,
    int frame_width,
    int frame_height,
    const uint8_t* face_crop,
    int crop_width,
    int crop_height,
    int dst_x,
    int dst_y,
    int dst_w,
    int dst_h
);

} // namespace face
} // namespace tupaz

#endif // TUPAZ_FACE_MERGE_H_
