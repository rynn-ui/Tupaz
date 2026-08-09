#include "face_merge.h"
#include "jni_utils.h"

#include <cmath>
#include <algorithm>

namespace tupaz {
namespace face {

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
) {
    if (frame == nullptr || face_crop == nullptr || frame_width <= 0 || frame_height <= 0) {
        return;
    }

    int channels = 4; // RGBA

    for (int y = 0; y < dst_h; ++y) {
        int fy = dst_y + y;
        if (fy < 0 || fy >= frame_height) continue;

        double norm_y = static_cast<double>(y) / dst_h;
        double dist_y = std::min(norm_y, 1.0 - norm_y) * 2.0;

        for (int x = 0; x < dst_w; ++x) {
            int fx = dst_x + x;
            if (fx < 0 || fx >= frame_width) continue;

            double norm_x = static_cast<double>(x) / dst_w;
            double dist_x = std::min(norm_x, 1.0 - norm_x) * 2.0;

            // Elliptical radial alpha falloff to prevent visible square borders
            double r2 = (0.5 - norm_x) * (0.5 - norm_x) * 4.0 + (0.5 - norm_y) * (0.5 - norm_y) * 4.0;
            double alpha = (r2 >= 1.0) ? 0.0 : std::pow(1.0 - r2, 1.5);

            int cx = (x * crop_width) / dst_w;
            int cy = (y * crop_height) / dst_h;

            size_t frame_idx = static_cast<size_t>((fy * frame_width + fx) * channels);
            size_t crop_idx = static_cast<size_t>((cy * crop_width + cx) * channels);

            for (int c = 0; c < 3; ++c) {
                uint8_t orig_val = frame[frame_idx + c];
                uint8_t crop_val = face_crop[crop_idx + c];
                frame[frame_idx + c] = static_cast<uint8_t>(orig_val * (1.0 - alpha) + crop_val * alpha);
            }
        }
    }
}

} // namespace face
} // namespace tupaz
