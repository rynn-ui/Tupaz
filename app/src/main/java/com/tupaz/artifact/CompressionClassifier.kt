package com.tupaz.artifact

import android.util.Log

/**
 * Result data class for compression classification.
 */
data class ClassificationResult(
    val compressionScore: Float,
    val requiresScunet: Boolean
)

/**
 * Analyzes JPEG / compression blocking artifacts to gate SCUNet processing according to ADR-0004.
 */
class CompressionClassifier(
    private val gatingThreshold: Float = 0.2f
) {
    companion object {
        private const val TAG = "CompressionClassifier"
    }

    /**
     * Evaluates input frame buffer for compression artifacts.
     * @param frameBuffer Raw RGBA image bytes.
     * @param width Width in pixels.
     * @param height Height in pixels.
     * @return ClassificationResult containing compression score and SCUNet gating flag.
     */
    fun classify(
        frameBuffer: ByteArray,
        width: Int,
        height: Int
    ): ClassificationResult {
        require(width > 0 && height > 0) { "Dimensions must be positive" }

        // Synthetic score evaluation (called via JNI in full build)
        val score = if (frameBuffer.isEmpty()) 0.0f else 0.15f
        val requiresScunet = score >= gatingThreshold

        Log.d(TAG, "Compression score: $score (Threshold: $gatingThreshold, SCUNet: $requiresScunet)")
        return ClassificationResult(
            compressionScore = score,
            requiresScunet = requiresScunet
        )
    }
}
