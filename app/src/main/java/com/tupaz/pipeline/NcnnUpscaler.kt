package com.tupaz.pipeline

import android.content.Context
import android.util.Log
import com.tupaz.cache.ModelVramCache
import com.tupaz.data.storage.ModelStorage

/**
 * Super-resolution upscale pipeline stage executing RealESRGAN tile processing.
 */
class NcnnUpscaler(
    private val ncnnBridge: NcnnBridge = NcnnBridge(),
    private val tileScheduler: TileScheduler = TileScheduler(),
    private val modelVramCache: ModelVramCache? = null,
    private val context: Context? = null,
    alreadyInitialized: Boolean = false,
    private val requireModelOutput: Boolean = false
) {
    companion object {
        private const val TAG = "NcnnUpscaler"
    }

    private var isInitialized = alreadyInitialized

    /**
     * Executes tile-based RealESRGAN upscale on input frame buffer.
     * @param inputBuffer Raw input RGBA byte array.
     * @param width Source frame width in pixels.
     * @param height Source frame height in pixels.
     * @param modelId Target model ID to execute (e.g. "realesr-animevideov3-x2").
     * @param scaleFactor Super-resolution scale factor (e.g. 2 for 2x upscale).
     * @return Upscaled RGBA image byte array.
     */
    fun process(
        inputBuffer: ByteArray,
        width: Int,
        height: Int,
        modelId: String,
        scaleFactor: Int = 2
    ): ByteArray {
        if (inputBuffer.isEmpty() || width <= 0 || height <= 0) {
            return inputBuffer
        }

        if (!isInitialized) {
            if (context != null) {
                val storage = ModelStorage(context)
                val paramFile = storage.getParamFile(modelId)
                val binFile = storage.getBinFile(modelId)
                if (paramFile.exists() && binFile.exists()) {
                    ncnnBridge.initModel(paramFile.absolutePath, binFile.absolutePath, useGpu = true)
                } else {
                    ncnnBridge.init(useGpu = true)
                }
            } else {
                ncnnBridge.init(useGpu = true)
            }
            isInitialized = true
        }

        Log.d(TAG, "[Tupaz-AI] Executing NCNN inference on ${width}x${height} using $modelId (scale: ${scaleFactor}x)")

        val processed = ncnnBridge.processFrame(inputBuffer, width, height, scaleFactor = scaleFactor, mode = 0)
        if (processed == null) {
            val msg = "[Tupaz-AI] CRITICAL: NCNN inference returned null for ${width}x${height} at ${scaleFactor}x — RealESRGAN model failed to process frame"
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        val expectedBytes = width.toLong() * height.toLong() * scaleFactor * scaleFactor * 4L
        if (processed.size.toLong() != expectedBytes) {
            val msg = "[Tupaz-AI] NCNN returned ${processed.size} bytes; expected $expectedBytes for ${width}x${height} at ${scaleFactor}x"
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }

        val outWidth = width * scaleFactor
        val outHeight = height * scaleFactor
        Log.d(TAG, "[Tupaz-AI] NCNN inference SUCCESS: returned ${processed.size} bytes (${outWidth}x${outHeight})")
        return processed
    }
}
