package com.tupaz.domain.pipeline

/**
 * Strategy mode assigned to each frame by the adaptive scheduler.
 */
enum class FrameExecutionPath {
    FULL_AI,   // High motion / keyframe -> Full neural network pipeline (~118ms)
    RIFE_FLOW  // Low motion intermediate frame -> RIFE optical flow warping (~8ms)
}

/**
 * Pure Kotlin domain scheduler balancing quality and performance per frame according to ADR-0004.
 * Zero Android framework imports.
 */
class AdaptiveFrameScheduler(
    private val keyframeInterval: Int = 5,
    private val motionThreshold: Float = 15.0f
) {
    private var frameCounter: Int = 0

    /**
     * Schedules the execution path for the current frame given the motion delta score.
     * @param motionDelta Average pixel motion magnitude between current and previous frame.
     * @param isSceneCut Flag indicating whether a scene change was detected.
     * @return [FrameExecutionPath] decision.
     */
    fun scheduleNextFrame(motionDelta: Float, isSceneCut: Boolean = false): FrameExecutionPath {
        frameCounter++

        // Scene cuts and keyframes always force full AI path
        if (isSceneCut || frameCounter % keyframeInterval == 1) {
            return FrameExecutionPath.FULL_AI
        }

        // High motion exceeds optical flow capacity -> force full AI
        if (motionDelta >= motionThreshold) {
            return FrameExecutionPath.FULL_AI
        }

        // Low-motion intermediate frame -> use fast RIFE flow path
        return FrameExecutionPath.RIFE_FLOW
    }

    /**
     * Resets internal frame counter state (called on video seek or stream start).
     */
    fun reset() {
        frameCounter = 0
    }
}
