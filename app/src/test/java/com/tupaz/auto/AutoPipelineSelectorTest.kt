package com.tupaz.auto

import com.tupaz.benchmark.DeviceTier
import com.tupaz.domain.pipeline.ProcessingMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AutoPipelineSelectorTest {

    private lateinit var selector: AutoPipelineSelector

    @Before
    fun setUp() {
        selector = AutoPipelineSelector()
    }

    @Test
    fun `selectMode returns ANIME for anime signals on MID tier`() {
        val signals = ContentSignals(
            isAnime = true,
            compressionScore = 0.1f,
            faceDensityRatio = 0.0f,
            averageMotionDelta = 5.0f,
            sampleFrameWidth = 1920,
            sampleFrameHeight = 1080
        )
        val mode = selector.selectMode(signals, DeviceTier.MID)
        assertEquals(ProcessingMode.ANIME, mode)
    }

    @Test
    fun `selectMode returns FAST for LOW hardware tier regardless of signals`() {
        val signals = ContentSignals(
            isAnime = true,
            compressionScore = 0.8f,
            faceDensityRatio = 0.5f,
            averageMotionDelta = 10.0f,
            sampleFrameWidth = 1920,
            sampleFrameHeight = 1080
        )
        val mode = selector.selectMode(signals, DeviceTier.LOW)
        assertEquals(ProcessingMode.FAST, mode)
    }

    @Test
    fun `selectMode returns BALANCED for compressed real-world video on MID tier`() {
        val signals = ContentSignals(
            isAnime = false,
            compressionScore = 0.4f,
            faceDensityRatio = 0.2f,
            averageMotionDelta = 5.0f,
            sampleFrameWidth = 1920,
            sampleFrameHeight = 1080
        )
        val mode = selector.selectMode(signals, DeviceTier.MID)
        assertEquals(ProcessingMode.BALANCED, mode)
    }

    @Test
    fun `selectMode returns ULTRA for clean content on FLAGSHIP tier`() {
        val signals = ContentSignals(
            isAnime = false,
            compressionScore = 0.05f,
            faceDensityRatio = 0.1f,
            averageMotionDelta = 2.0f,
            sampleFrameWidth = 3840,
            sampleFrameHeight = 2160
        )
        val mode = selector.selectMode(signals, DeviceTier.FLAGSHIP)
        assertEquals(ProcessingMode.ULTRA, mode)
    }
}
