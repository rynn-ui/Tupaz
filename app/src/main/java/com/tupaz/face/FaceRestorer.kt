package com.tupaz.face

import android.util.Log
import com.tupaz.cache.ModelVramCache
import com.tupaz.pipeline.NcnnBridge

/**
 * Neural face restoration pipeline stage (GFPGAN / CodeFormer).
 */
class FaceRestorer(
    private val ncnnBridge: NcnnBridge = NcnnBridge(),
    private val modelVramCache: ModelVramCache? = null
) {
    companion object {
        private const val TAG = "FaceRestorer"
    }

    /**
     * Enhances eyes, mouth, and skin texture on a cropped face image buffer.
     * @param faceCropBuffer Raw face crop RGBA bytes (512x512).
     * @param width Crop width in pixels.
     * @param height Crop height in pixels.
     * @param modelId Model ID (default "gfpgan-v1.4").
     * @return Enhanced face crop RGBA bytes.
     */
    fun restoreFace(
        faceCropBuffer: ByteArray,
        width: Int = 512,
        height: Int = 512,
        modelId: String = "gfpgan-v1.4"
    ): ByteArray {
        require(width > 0 && height > 0) { "Dimensions must be positive" }

        modelVramCache?.getOrLoad(modelId)
        Log.i(TAG, "Restoring face crop ${width}x${height} using GFPGAN model $modelId")
        val restored = ncnnBridge.processFrame(faceCropBuffer, width, height, mode = 5)
        return restored ?: faceCropBuffer
    }
}
