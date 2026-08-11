package com.tupaz.pipeline

import com.tupaz.domain.pipeline.ProcessingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PipelineOrchestratorTest {

    private class FakeNcnnBridge : NcnnBridge() {
        override fun processFrame(
            inputFrame: ByteArray,
            width: Int,
            height: Int,
            scaleFactor: Int,
            mode: Int
        ): ByteArray {
            val outW = width * scaleFactor
            val outH = height * scaleFactor
            return ByteArray(outW * outH * 4)
        }
    }

    private val fakeBridge = FakeNcnnBridge()
    private val fakeUpscaler = NcnnUpscaler(ncnnBridge = fakeBridge, alreadyInitialized = true)
    private val fakeDenoiser = Denoiser(ncnnBridge = fakeBridge)
    private val fakeScunet = ScunetRunner(ncnnBridge = fakeBridge)
    private val orchestrator = PipelineOrchestrator(
        denoiser = fakeDenoiser,
        scunetRunner = fakeScunet,
        upscaler = fakeUpscaler
    )

    @Test
    fun `calculateOutputDimensions correctly multiplies dimensions by scaleFactor`() {
        val (w2, h2) = orchestrator.calculateOutputDimensions(1920, 1080, 2)
        assertEquals(3840, w2)
        assertEquals(2160, h2)

        val (w3, h3) = orchestrator.calculateOutputDimensions(1920, 1080, 3)
        assertEquals(5760, w3)
        assertEquals(3240, h3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateOutputDimensions rejects non-positive dimensions`() {
        orchestrator.calculateOutputDimensions(0, 1080, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateOutputDimensions rejects non-positive scaleFactor`() {
        orchestrator.calculateOutputDimensions(1920, 1080, 0)
    }

    @Test
    fun `processFrame with scale 2 produces 2x output dimensions across all modes`() {
        val rawFrame = RawFrame(
            frameIndex = 1,
            presentationTimeUs = 33333,
            width = 1920,
            height = 1080,
            data = ByteArray(1920 * 1080 * 4),
            isEndOfStream = false
        )

        for (mode in ProcessingMode.entries) {
            val processed = orchestrator.processFrame(rawFrame, mode, scaleFactor = 2)
            assertNotNull(processed)
            assertEquals("Failed for mode $mode", 3840, processed.width)
            assertEquals("Failed for mode $mode", 2160, processed.height)
        }
    }

    @Test
    fun `processFrame with scale 3 produces 3x output dimensions (plumbing test)`() {
        val rawFrame = RawFrame(
            frameIndex = 1,
            presentationTimeUs = 33333,
            width = 1920,
            height = 1080,
            data = ByteArray(1920 * 1080 * 4),
            isEndOfStream = false
        )

        val processed = orchestrator.processFrame(rawFrame, ProcessingMode.AUTO, scaleFactor = 3)
        assertNotNull(processed)
        assertEquals(5760, processed.width)
        assertEquals(3240, processed.height)
    }
}
