package com.tupaz.domain.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingModeTest {

    @Test
    fun `fromId returns correct enum instance for valid IDs`() {
        assertEquals(ProcessingMode.FAST, ProcessingMode.fromId("fast"))
        assertEquals(ProcessingMode.BALANCED, ProcessingMode.fromId("balanced"))
        assertEquals(ProcessingMode.ULTRA, ProcessingMode.fromId("ultra"))
        assertEquals(ProcessingMode.ANIME, ProcessingMode.fromId("anime"))
        assertEquals(ProcessingMode.AUTO, ProcessingMode.fromId("auto"))
    }

    @Test
    fun `fromId is case insensitive`() {
        assertEquals(ProcessingMode.BALANCED, ProcessingMode.fromId("BALANCED"))
        assertEquals(ProcessingMode.ULTRA, ProcessingMode.fromId("UlTrA"))
    }

    @Test
    fun `fromId returns BALANCED default for unknown ID`() {
        assertEquals(ProcessingMode.BALANCED, ProcessingMode.fromId("unknown_mode"))
    }

    @Test
    fun `calculateOutputDimensions computes correct scaled resolution`() {
        val (width2x, height2x) = ProcessingMode.BALANCED.calculateOutputDimensions(1920, 1080)
        assertEquals(3840, width2x)
        assertEquals(2160, height2x)

        val (width4x, height4x) = ProcessingMode.ULTRA.calculateOutputDimensions(1280, 720)
        assertEquals(5120, width4x)
        assertEquals(2880, height4x)

        val (width1x, height1x) = ProcessingMode.FAST.calculateOutputDimensions(1920, 1080)
        assertEquals(1920, width1x)
        assertEquals(1080, height1x)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateOutputDimensions throws exception for non-positive width`() {
        ProcessingMode.BALANCED.calculateOutputDimensions(0, 1080)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateOutputDimensions throws exception for non-positive height`() {
        ProcessingMode.BALANCED.calculateOutputDimensions(1920, -1)
    }

    @Test
    fun `verify Vulkan requirements per mode`() {
        assertFalse(ProcessingMode.FAST.requiresVulkan)
        assertTrue(ProcessingMode.BALANCED.requiresVulkan)
        assertTrue(ProcessingMode.ULTRA.requiresVulkan)
        assertTrue(ProcessingMode.ANIME.requiresVulkan)
        assertTrue(ProcessingMode.AUTO.requiresVulkan)
    }
}
