package com.tupaz.pipeline

import android.util.Log

/**
 * Vulkan Contrast Adaptive Sharpening (CAS) pipeline stage wrapper.
 */
class VulkanSharpen(
    private val ncnnBridge: NcnnBridge = NcnnBridge()
) {
    companion object {
        private const val TAG = "VulkanSharpen"
    }

    /**
     * Applies Contrast Adaptive Sharpening to an input image buffer.
     * @param inputBuffer Image byte array (RGBA).
     * @param width Width in pixels.
     * @param height Height in pixels.
     * @param sharpness Strength factor between 0.0f (subtle) and 1.0f (strong).
     * @return Sharpened RGBA image buffer.
     */
    fun process(
        inputBuffer: ByteArray,
        width: Int,
        height: Int,
        sharpness: Float = 0.4f
    ): ByteArray {
        require(width > 0 && height > 0) { "Dimensions must be positive" }
        require(sharpness in 0.0f..1.0f) { "Sharpness must be between 0.0 and 1.0" }

        Log.i(TAG, "Applying Vulkan CAS sharpening (${width}x${height}, sharpness=$sharpness)")
        val result = ncnnBridge.processFrame(inputBuffer, width, height, scaleFactor = 1, mode = 1)
        if (result == null) {
            val msg = "[Tupaz-AI] VulkanSharpen native processFrame returned NULL (${width}x${height})."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        return result
    }
}
