package com.tupaz.pipeline

import android.util.Log

/**
 * Fast Temporal Low-Motion Skipper (<0.2ms)
 * Measures frame-to-frame motion difference across consecutive video frames.
 * For low-motion / static scenes, reuses the previous AI-enhanced buffer.
 */
class TemporalFlowSkipper {
    companion object {
        private const val TAG = "TemporalFlowSkipper"
        private const val MOTION_THRESHOLD = 0.001f // 0.1% strict threshold: ensure every active frame gets AI enhancement
    }

    private var previousRawData: ByteArray? = null
    private var previousEnhancedFrame: ProcessedFrame? = null

    /**
     * Evaluates frame motion against previous raw frame. Returns cached enhanced frame if low motion.
     */
    fun processOrNull(rawFrame: RawFrame): ProcessedFrame? {
        val prevRaw = previousRawData
        val prevEnhanced = previousEnhancedFrame

        if (prevRaw != null && prevEnhanced != null && prevRaw.size == rawFrame.data.size) {
            val motion = computeFastMotionScore(prevRaw, rawFrame.data)
            if (motion <= MOTION_THRESHOLD) {
                Log.d(TAG, "[Tupaz-Flow] Frame ${rawFrame.frameIndex} low motion score (${String.format("%.3f", motion)}) -> FAST REUSE AI FRAME (~0.2ms)")
                return prevEnhanced.copy(
                    frameIndex = rawFrame.frameIndex,
                    presentationTimeUs = rawFrame.presentationTimeUs
                )
            }
        }

        return null
    }

    /**
     * Updates previous frame cache for future temporal motion comparison.
     */
    fun updateCache(rawFrame: RawFrame, enhancedFrame: ProcessedFrame) {
        if (!rawFrame.isEndOfStream && rawFrame.data.isNotEmpty()) {
            previousRawData = rawFrame.data.copyOf()
            previousEnhancedFrame = enhancedFrame
        }
    }

    fun reset() {
        previousRawData = null
        previousEnhancedFrame = null
    }

    private fun computeFastMotionScore(bytes1: ByteArray, bytes2: ByteArray): Float {
        var totalDiff = 0L
        var sampledPixels = 0
        val step = 64 // Strided sampling every 16th RGBA pixel for <0.2ms scoring

        var i = 0
        val len = bytes1.size
        while (i < len) {
            val r1 = bytes1[i].toInt() and 0xFF
            val r2 = bytes2[i].toInt() and 0xFF
            totalDiff += kotlin.math.abs(r1 - r2)
            sampledPixels++
            i += step
        }

        return if (sampledPixels > 0) (totalDiff.toFloat() / (sampledPixels * 255f)) else 1.0f
    }
}
