package com.tupaz.domain.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineScaleTest {

    @Test
    fun `parseAndValidate parses valid scale factor representations correctly`() {
        assertEquals(2, PipelineScale.parseAndValidate("2x"))
        assertEquals(2, PipelineScale.parseAndValidate("2x Scale"))
        assertEquals(2, PipelineScale.parseAndValidate("2"))
        assertEquals(2, PipelineScale.parseAndValidate("2.0"))
        assertEquals(2, PipelineScale.parseAndValidate(" 2X SCALE "))
        assertEquals(3, PipelineScale.parseAndValidate("3"))
        assertEquals(4, PipelineScale.parseAndValidate("4x"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseAndValidate rejects empty string`() {
        PipelineScale.parseAndValidate("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseAndValidate rejects standalone x`() {
        PipelineScale.parseAndValidate("x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseAndValidate rejects non-numeric string`() {
        PipelineScale.parseAndValidate("abc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseAndValidate rejects zero scale`() {
        PipelineScale.parseAndValidate("0x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseAndValidate rejects negative scale`() {
        PipelineScale.parseAndValidate("-1x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseAndValidate rejects fractional scale`() {
        PipelineScale.parseAndValidate("3.5x")
    }
}
