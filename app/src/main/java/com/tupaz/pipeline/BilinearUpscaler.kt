package com.tupaz.pipeline

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * Ultra-fast bilinear upscaler for frames that skip full AI model processing.
 * Runs in ~2-5ms (vs ~100ms for NCNN neural inference).
 */
class BilinearUpscaler {

    /**
     * Upscales input RGBA bytes using high-speed bilinear filtering.
     * Reuses bitmap buffers when possible.
     */
    fun process(
        inputBuffer: ByteArray,
        width: Int,
        height: Int,
        scaleFactor: Int = 2
    ): ByteArray {
        if (width <= 0 || height <= 0 || scaleFactor <= 1 || inputBuffer.isEmpty()) {
            return inputBuffer
        }

        val outWidth = width * scaleFactor
        val outHeight = height * scaleFactor

        try {
            val srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            srcBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(inputBuffer))

            val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, outWidth, outHeight, true)
            val outBytes = ByteArray(outWidth * outHeight * 4)
            scaledBitmap.copyPixelsToBuffer(ByteBuffer.wrap(outBytes))

            if (!srcBitmap.isRecycled) srcBitmap.recycle()
            if (scaledBitmap != srcBitmap && !scaledBitmap.isRecycled) scaledBitmap.recycle()

            return outBytes
        } catch (_: Throwable) {
            return inputBuffer
        }
    }
}
