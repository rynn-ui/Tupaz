package com.tupaz.pipeline

import com.tupaz.domain.pipeline.ProcessingMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameQualityGateTest {

    private class MockNcnnBridge(private val mockQuality: Float) : NcnnBridge() {
        override fun scoreFrameQuality(inputFrame: ByteArray, width: Int, height: Int): FloatArray {
            return floatArrayOf(0.05f, 0.05f, 0.90f, mockQuality)
        }
    }

    @Test
    fun testPristineFrame_SkipsFullPipeline() {
        val mockBridge = MockNcnnBridge(mockQuality = 0.85f)
        val qualityGate = FrameQualityGate(ncnnBridge = mockBridge)
        val rawFrame = RawFrame(1, 0, 100, 100, ByteArray(40000))

        val shouldSkip = qualityGate.shouldSkipFullPipeline(rawFrame, ProcessingMode.BALANCED)
        assertTrue(shouldSkip)
    }

    @Test
    fun testDegradedFrame_DoesNotSkipFullPipeline() {
        val mockBridge = MockNcnnBridge(mockQuality = 0.50f)
        val qualityGate = FrameQualityGate(ncnnBridge = mockBridge)
        val rawFrame = RawFrame(1, 0, 100, 100, ByteArray(40000))

        val shouldSkip = qualityGate.shouldSkipFullPipeline(rawFrame, ProcessingMode.BALANCED)
        assertFalse(shouldSkip)
    }
}
