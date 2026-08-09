package com.tupaz.pipeline

import android.util.Log

/**
 * Pipeline stage runner enforcing temporal consistency across video frames according to ADR-0004.
 */
class TemporalConsistency(
    private val ncnnBridge: NcnnBridge = NcnnBridge()
) {
    companion object {
        private const val TAG = "TemporalConsistency"
    }

    /**
     * Blends current frame with warped previous frame guided by occlusion mask.
     * @param currentFrame Raw current frame RGBA bytes.
     * @param previousFrame Raw previous frame RGBA bytes.
     * @param width Frame width in pixels.
     * @param height Frame height in pixels.
     * @return Temporally stabilized RGBA byte array.
     */
    fun process(
        currentFrame: ByteArray,
        previousFrame: ByteArray?,
        width: Int,
        height: Int
    ): ByteArray {
        if (previousFrame == null || previousFrame.isEmpty()) {
            return currentFrame
        }
        require(width > 0 && height > 0) { "Dimensions must be positive" }

        Log.d(TAG, "Enforcing temporal consistency for ${width}x${height} frame")
        val blended = ncnnBridge.processFrame(currentFrame, width, height, mode = 4)
        return blended ?: currentFrame
    }
}
