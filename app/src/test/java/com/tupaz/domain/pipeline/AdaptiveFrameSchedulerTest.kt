package com.tupaz.domain.pipeline

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AdaptiveFrameSchedulerTest {

    private lateinit var scheduler: AdaptiveFrameScheduler

    @Before
    fun setUp() {
        scheduler = AdaptiveFrameScheduler(keyframeInterval = 5, motionThreshold = 15.0f)
    }

    @Test
    fun `first frame always schedules FULL_AI keyframe path`() {
        val path = scheduler.scheduleNextFrame(motionDelta = 2.0f, isSceneCut = false)
        assertEquals(FrameExecutionPath.FULL_AI, path)
    }

    @Test
    fun `intermediate low-motion frame schedules RIFE_FLOW path`() {
        scheduler.scheduleNextFrame(motionDelta = 2.0f) // Frame 1: FULL_AI
        val pathFrame2 = scheduler.scheduleNextFrame(motionDelta = 2.0f) // Frame 2: RIFE_FLOW
        assertEquals(FrameExecutionPath.RIFE_FLOW, pathFrame2)
    }

    @Test
    fun `high motion exceeding threshold forces FULL_AI path`() {
        scheduler.scheduleNextFrame(motionDelta = 2.0f) // Frame 1: FULL_AI
        val pathHighMotion = scheduler.scheduleNextFrame(motionDelta = 25.0f) // Frame 2 high motion
        assertEquals(FrameExecutionPath.FULL_AI, pathHighMotion)
    }

    @Test
    fun `scene cut forces FULL_AI path`() {
        scheduler.scheduleNextFrame(motionDelta = 2.0f) // Frame 1
        val pathSceneCut = scheduler.scheduleNextFrame(motionDelta = 2.0f, isSceneCut = true)
        assertEquals(FrameExecutionPath.FULL_AI, pathSceneCut)
    }

    @Test
    fun `reset resets keyframe counter`() {
        scheduler.scheduleNextFrame(motionDelta = 2.0f) // Frame 1
        scheduler.scheduleNextFrame(motionDelta = 2.0f) // Frame 2
        scheduler.reset()

        // After reset, next frame should be Frame 1 -> FULL_AI
        val pathAfterReset = scheduler.scheduleNextFrame(motionDelta = 2.0f)
        assertEquals(FrameExecutionPath.FULL_AI, pathAfterReset)
    }
}
