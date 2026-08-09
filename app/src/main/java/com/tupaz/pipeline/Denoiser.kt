package com.tupaz.pipeline

import android.util.Log
import com.tupaz.cache.ModelVramCache

/**
 * NAFNet neural network denoising pipeline stage.
 */
class Denoiser(
    private val ncnnBridge: NcnnBridge = NcnnBridge(),
    private val modelVramCache: ModelVramCache? = null
) {
    companion object {
        private const val TAG = "Denoiser"
    }

    /**
     * Denoises input image buffer using NAFNet model.
     * @param inputBuffer Image byte array (RGBA).
     * @param width Width in pixels.
     * @param height Height in pixels.
     * @param modelId Model identifier (default "nafnet-width32").
     * @return Denoised RGBA image bytes.
     */
    fun process(
        inputBuffer: ByteArray,
        width: Int,
        height: Int,
        modelId: String = "nafnet-width32"
    ): ByteArray {
        require(width > 0 && height > 0) { "Dimensions must be positive" }

        modelVramCache?.getOrLoad(modelId)
        Log.i(TAG, "Denoising image ${width}x${height} with NAFNet model $modelId")
        val result = ncnnBridge.processFrame(inputBuffer, width, height, mode = 2)
        if (result == null) {
            val msg = "[Tupaz-AI] Denoiser NAFNet native processFrame returned NULL (${width}x${height})."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        return result
    }
}
