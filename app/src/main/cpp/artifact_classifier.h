#ifndef TUPAZ_ARTIFACT_CLASSIFIER_H_
#define TUPAZ_ARTIFACT_CLASSIFIER_H_

#include <cstdint>

namespace tupaz {
namespace artifact {

/**
 * @brief Analyzes 8x8 block boundaries to compute JPEG compression artifact score.
 * @param frame Image byte array (RGBA).
 * @param width Frame width in pixels.
 * @param height Frame height in pixels.
 * @return Double Compression score from 0.0 (pristine/uncompressed) to 1.0 (heavily compressed).
 */
double compute_blocking_score(
    const uint8_t* frame,
    int width,
    int height
);

} // namespace artifact
} // namespace tupaz

#endif // TUPAZ_ARTIFACT_CLASSIFIER_H_
