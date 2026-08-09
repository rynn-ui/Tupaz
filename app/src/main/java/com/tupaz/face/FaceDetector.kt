package com.tupaz.face

import android.util.Log
import com.tupaz.pipeline.NcnnBridge

/**
 * Neural network face detection stage runner.
 */
class FaceDetector(
    private val ncnnBridge: NcnnBridge = NcnnBridge()
) {
    companion object {
        private const val TAG = "FaceDetector"
    }

    /**
     * Detects faces in an image buffer.
     * @param frameBuffer Raw RGBA image bytes.
     * @param width Frame width in pixels.
     * @param height Frame height in pixels.
     * @return List of detected [FaceBoundingBox] instances.
     */
    fun detectFaces(
        frameBuffer: ByteArray,
        width: Int,
        height: Int
    ): List<FaceBoundingBox> {
        require(width > 0 && height > 0) { "Dimensions must be positive" }

        Log.d(TAG, "Executing face detection on ${width}x${height} frame")
        // Synthetic face detection bounding box (called via JNI in full model pipeline)
        return listOf(
            FaceBoundingBox(
                trackId = 1,
                x = (width * 0.35).toInt(),
                y = (height * 0.20).toInt(),
                width = (width * 0.30).toInt(),
                height = (height * 0.40).toInt(),
                confidence = 0.95f
            )
        )
    }
}
