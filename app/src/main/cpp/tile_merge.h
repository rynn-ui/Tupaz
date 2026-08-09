#ifndef TUPAZ_TILE_MERGE_H_
#define TUPAZ_TILE_MERGE_H_

#include <cstdint>
#include <vector>

namespace tupaz {
namespace pipeline {

struct TileBounds {
    int x;
    int y;
    int width;
    int height;
    int scale_factor;
};

/**
 * @brief Accumulates an upscaled tile into RGB float accumulators using smooth sine/cosine weighting.
 */
void accumulate_tile_weighted(
    float* accum_r,
    float* accum_g,
    float* accum_b,
    float* accum_w,
    int dst_width,
    int dst_height,
    const uint8_t* src_tile,
    const TileBounds& bounds
);

/**
 * @brief Normalizes accumulated RGB float buffers into final RGBA byte destination.
 */
void normalize_tile_accumulation(
    uint8_t* dst_rgba,
    const float* accum_r,
    const float* accum_g,
    const float* accum_b,
    const float* accum_w,
    int dst_width,
    int dst_height
);

/**
 * @brief Applies Contrast-Adaptive Sharpening (CAS) on RGBA frame buffer for studio visual clarity.
 */
void apply_cas_sharpen(
    uint8_t* rgba,
    int width,
    int height,
    float sharpness
);

/**
 * @brief Applies character pop-out 3D contrast & vibrance filter to separate subjects from background.
 */
void apply_character_pop_filter(
    uint8_t* rgba,
    int width,
    int height
);

/**
 * @brief Enhances eyelashes, eyes, hair strands, and facial outlines while reducing compression noise.
 */
void apply_hair_and_eye_enhancement(
    uint8_t* rgba,
    int width,
    int height
);

/**
 * @brief Legacy tile merge function for backwards compatibility.
 */
void merge_tile_cosine(
    uint8_t* dst,
    int dst_width,
    int dst_height,
    const uint8_t* src,
    const TileBounds& bounds
);

} // namespace pipeline
} // namespace tupaz

#endif // TUPAZ_TILE_MERGE_H_
