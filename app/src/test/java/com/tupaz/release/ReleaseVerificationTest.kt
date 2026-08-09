package com.tupaz.release

import com.tupaz.benchmark.BenchmarkResultData
import com.tupaz.benchmark.DeviceProfile
import com.tupaz.benchmark.DeviceTier
import com.tupaz.domain.pipeline.ProcessingMode
import com.tupaz.pipeline.NcnnBridge
import com.tupaz.pipeline.NcnnUpscaler
import com.tupaz.pipeline.VulkanSharpen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReleaseVerificationTest {

    private lateinit var upscaler: NcnnUpscaler
    private lateinit var sharpen: VulkanSharpen

    @Before
    fun setUp() {
        val bridge = object : NcnnBridge() {
            override fun processFrame(inputFrame: ByteArray, width: Int, height: Int, scaleFactor: Int, mode: Int): ByteArray {
                return ByteArray(width * scaleFactor * height * scaleFactor * 4)
            }
        }
        upscaler = NcnnUpscaler(ncnnBridge = bridge)
        sharpen = VulkanSharpen(ncnnBridge = bridge)
    }

    @Test
    fun `verify all 5 processing modes calculate valid output resolution math`() {
        val inputW = 1920
        val inputH = 1080

        for (mode in ProcessingMode.entries) {
            val (outW, outH) = mode.calculateOutputDimensions(inputW, inputH)
            assertTrue(outW >= inputW)
            assertTrue(outH >= inputH)
            assertEquals(inputW * mode.scaleFactor, outW)
            assertEquals(inputH * mode.scaleFactor, outH)
        }
    }

    @Test
    fun `verify end-to-end stage processing runs without throwing exceptions`() {
        val dummyFrame = ByteArray(64 * 64 * 4)
        val upscaled = upscaler.process(dummyFrame, 64, 64, "realesr-animevideov3-x2", scaleFactor = 2)
        assertNotNull(upscaled)

        val sharpened = sharpen.process(dummyFrame, 64, 64, sharpness = 0.5f)
        assertNotNull(sharpened)
    }

    @Test
    fun `verify device tier classification logic maps to expected default modes`() {
        val flagship = DeviceProfile.classify(BenchmarkResultData(10.0, 4000.0, 80.0, 500.0))
        assertEquals(DeviceTier.FLAGSHIP, flagship.tier)
        assertEquals(ProcessingMode.AUTO, flagship.recommendedMode)

        val low = DeviceProfile.classify(BenchmarkResultData(0.5, 400.0, 15.0, 3000.0))
        assertEquals(DeviceTier.LOW, low.tier)
        assertEquals(ProcessingMode.FAST, low.recommendedMode)
    }
}
