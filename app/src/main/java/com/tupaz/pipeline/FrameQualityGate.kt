package com.tupaz.pipeline

import android.util.Log
import com.tupaz.domain.pipeline.ProcessingMode

/**
 * Intelligent Quality Gate detecting pristine / already-high-quality frames to skip heavy AI model inference.
 */
class FrameQualityGate(
    private val ncnnBridge: NcnnBridge = NcnnBridge()
) {
    companion object {
        private const val TAG = "FrameQualityGate"
    }

    private var consecutiveSkips = 0

    /**
     * Evaluates if frame quality is high enough to skip expensive neural upscaling.
     */
    fun shouldSkipFullPipeline(rawFrame: RawFrame, mode: ProcessingMode): Boolean {
        if (rawFrame.data.isEmpty() || rawFrame.width <= 0 || rawFrame.height <= 0) {
            return false
        }

        // This gate is used only by explicitly opt-in preview/reuse paths.
        // Full exports always run the model (see PipelineOrchestrator).
        val baseThreshold = when (mode) {
            ProcessingMode.FAST -> 0.85f
            ProcessingMode.ANIME -> 0.85f
            ProcessingMode.BALANCED -> 0.85f
            ProcessingMode.AUTO -> 0.85f
            ProcessingMode.ULTRA -> 0.85f
        }

        // Hysteresis mitigation: if we're already skipping consecutive frames, lower threshold slightly to prevent flicker
        val effectiveThreshold = if (consecutiveSkips > 2) baseThreshold - 0.05f else baseThreshold

        // 2. Score frame quality via native C++ scorer (<1ms)
        val scores = ncnnBridge.scoreFrameQuality(rawFrame.data, rawFrame.width, rawFrame.height)
            ?: return false

        val overallQuality = scores.getOrElse(3) { 0.0f }
        val skip = overallQuality >= effectiveThreshold

        if (skip) {
            consecutiveSkips++
            Log.d(TAG, "Frame ${rawFrame.frameIndex} quality high (${String.format("%.2f", overallQuality)} >= ${String.format("%.2f", effectiveThreshold)}) -> FAST PATH SKIP AI")
        } else {
            consecutiveSkips = 0
        }

        return skip
    }

    fun reset() {
        consecutiveSkips = 0
    }
}
