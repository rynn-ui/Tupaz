package com.tupaz.face

import android.util.Log
import com.tupaz.pipeline.NcnnBridge

/**
 * Pipeline stage runner merging restored face crop back into full frame buffer.
 */
class FaceMerger(
    private val ncnnBridge: NcnnBridge = NcnnBridge()
) {
    companion object {
        private const val TAG = "FaceMerger"
    }

    /**
     * Blends restored face crop smoothly into frame buffer at target bounding box coordinates.
     * @param fullFrame Full frame RGBA byte array.
     * @param frameWidth Full frame width in pixels.
     * @param frameHeight Full frame height in pixels.
     * @param faceCrop Restored face crop RGBA byte array.
     * @param boundingBox Target face bounding box.
     * @return Blended full frame RGBA byte array.
     */
    fun mergeFace(
        fullFrame: ByteArray,
        frameWidth: Int,
        frameHeight: Int,
        faceCrop: ByteArray,
        boundingBox: FaceBoundingBox
    ): ByteArray {
        require(frameWidth > 0 && frameHeight > 0) { "Dimensions must be positive" }

        Log.d(TAG, "Merging face crop back into frame at ${boundingBox.x},${boundingBox.y}")
        val result = ncnnBridge.processFrame(fullFrame, frameWidth, frameHeight, mode = 6)
        return result ?: fullFrame
    }
}
