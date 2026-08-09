package com.tupaz.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PipelineProfilerTest {

    private lateinit var profiler: PipelineProfiler

    @Before
    fun setUp() {
        profiler = PipelineProfiler()
    }

    @Test
    fun `recordStage measures latency and generateReport builds valid report`() {
        profiler.incrementFrameCount()
        profiler.recordStage("super_res") {
            Thread.sleep(10)
        }

        val report = profiler.generateReport(deviceInfo = "TestDevice", modeName = "BALANCED")
        assertNotNull(report)
        assertEquals("TestDevice", report.device)
        assertEquals("BALANCED", report.mode)
        assertEquals(1L, report.frameCount)

        val stageMetrics = report.stages["super_res"]
        assertNotNull(stageMetrics)
        assertTrue(stageMetrics!!.meanMs >= 5.0)
    }

    @Test
    fun `exportJson generates valid JSON report matching schema`() {
        profiler.incrementFrameCount()
        profiler.recordStage("denoise") {
            Thread.sleep(5)
        }

        val jsonString = profiler.exportJson()
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"device\""))
        assertTrue(jsonString.contains("\"stages\""))
        assertTrue(jsonString.contains("\"denoise\""))
    }
}
