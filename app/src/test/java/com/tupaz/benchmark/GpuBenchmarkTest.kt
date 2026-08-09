package com.tupaz.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuBenchmarkTest {

    @Test
    fun `DeviceProfile classify returns FLAGSHIP for high scores`() {
        val result = BenchmarkResultData(
            fp16Tflops = 8.0,
            vulkanMegapixelsPerSec = 4000.0,
            memoryBandwidthGbps = 85.0,
            totalTimeMs = 800.0
        )
        val profile = DeviceProfile.classify(result)

        assertEquals(DeviceTier.FLAGSHIP, profile.tier)
    }

    @Test
    fun `DeviceProfile classify returns HIGH for high tier scores`() {
        val result = BenchmarkResultData(
            fp16Tflops = 5.0,
            vulkanMegapixelsPerSec = 2500.0,
            memoryBandwidthGbps = 50.0,
            totalTimeMs = 1200.0
        )
        val profile = DeviceProfile.classify(result)

        assertEquals(DeviceTier.HIGH, profile.tier)
    }

    @Test
    fun `DeviceProfile classify returns MID for mid tier scores`() {
        val result = BenchmarkResultData(
            fp16Tflops = 2.4,
            vulkanMegapixelsPerSec = 1200.0,
            memoryBandwidthGbps = 30.0,
            totalTimeMs = 1500.0
        )
        val profile = DeviceProfile.classify(result)

        assertEquals(DeviceTier.MID, profile.tier)
    }

    @Test
    fun `DeviceProfile classify returns LOW for low tier scores`() {
        val result = BenchmarkResultData(
            fp16Tflops = 0.5,
            vulkanMegapixelsPerSec = 500.0,
            memoryBandwidthGbps = 15.0,
            totalTimeMs = 2500.0
        )
        val profile = DeviceProfile.classify(result)

        assertEquals(DeviceTier.LOW, profile.tier)
    }
}
