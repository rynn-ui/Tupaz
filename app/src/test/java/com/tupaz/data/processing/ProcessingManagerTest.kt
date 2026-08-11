package com.tupaz.data.processing

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessingManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor
        every { context.getSharedPreferences(any(), any()) } returns prefs
        ProcessingManager.reset(context)
    }

    @After
    fun tearDown() {
        ProcessingManager.reset(context)
    }

    @Test
    fun `startProcessing starts job when idle`() {
        val config = ProcessingJobConfig(
            projectId = "proj_A",
            projectName = "Project A",
            fileName = "input_a.mp4",
            inputUriString = "file:///tmp/input_a.mp4"
        )

        val started = ProcessingManager.startProcessing(context, config)
        assertTrue(started)
        assertEquals(ProcessingStatus.PROCESSING, ProcessingManager.state.value.status)
        assertEquals("proj_A", ProcessingManager.state.value.config.projectId)
    }

    @Test
    fun `startProcessing rejects second job while first job is processing`() {
        val configA = ProcessingJobConfig(
            projectId = "proj_A",
            projectName = "Project A",
            fileName = "input_a.mp4"
        )
        val configB = ProcessingJobConfig(
            projectId = "proj_B",
            projectName = "Project B",
            fileName = "input_b.mp4"
        )

        val startedA = ProcessingManager.startProcessing(context, configA)
        assertTrue(startedA)
        assertEquals("proj_A", ProcessingManager.state.value.config.projectId)

        // Attempting to start job B while job A is active must be rejected
        val startedB = ProcessingManager.startProcessing(context, configB)
        assertFalse(startedB)

        // State must remain associated with job A
        assertEquals(ProcessingStatus.PROCESSING, ProcessingManager.state.value.status)
        assertEquals("proj_A", ProcessingManager.state.value.config.projectId)
        assertEquals("input_a.mp4", ProcessingManager.state.value.config.fileName)
    }

    @Test
    fun `cancelProcessing affects current active job`() {
        val config = ProcessingJobConfig(
            projectId = "proj_cancel",
            projectName = "Cancel Project",
            fileName = "cancel.mp4"
        )

        ProcessingManager.startProcessing(context, config)
        assertEquals(ProcessingStatus.PROCESSING, ProcessingManager.state.value.status)

        ProcessingManager.cancelProcessing(context)
        assertEquals(ProcessingStatus.CANCELLED, ProcessingManager.state.value.status)
        assertEquals("proj_cancel", ProcessingManager.state.value.config.projectId)
    }
}
