#ifndef TUPAZ_FRAME_QUALITY_SCORER_H_
#define TUPAZ_FRAME_QUALITY_SCORER_H_

#include <cstdint>

namespace tupaz {
namespace quality {

struct FrameQualityResult {
    float blockingScore;   // 0.0 (pristine) to 1.0 (heavy macroblocking)
    float noiseLevel;      // 0.0 (clean) to 1.0 (heavy noise)
    float sharpnessScore;  // 0.0 (blurry) to 1.0 (crisp high detail)
    float overallQuality;  // 0.0 (poor, needs full AI) to 1.0 (excellent, skip full AI)
};

/**
 * @brief Computes fast single-pass quality metrics on raw RGBA frame buffer (~0.5ms).
 * @param frame RGBA byte array.
 * @param width Frame width.
 * @param height Frame height.
 * @return FrameQualityResult containing metric scores.
 */
FrameQualityResult score_frame_quality(
    const uint8_t* frame,
    int width,
    int height
);

} // namespace quality
} // namespace tupaz

#endif // TUPAZ_FRAME_QUALITY_SCORER_H_
