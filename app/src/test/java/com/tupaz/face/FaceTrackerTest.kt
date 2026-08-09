package com.tupaz.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FaceTrackerTest {

    private lateinit var tracker: FaceTracker

    @Before
    fun setUp() {
        tracker = FaceTracker(iouThreshold = 0.4f)
    }

    @Test
    fun `FaceBoundingBox IOU calculation returns 1 for identical boxes`() {
        val box1 = FaceBoundingBox(1, 100, 100, 200, 200, 0.9f)
        val box2 = FaceBoundingBox(2, 100, 100, 200, 200, 0.9f)

        assertEquals(1.0f, box1.calculateIou(box2), 0.001f)
    }

    @Test
    fun `updateDetections preserves trackId across frames for overlapping boxes`() {
        val frame1Box = FaceBoundingBox(0, 100, 100, 200, 200, 0.9f)
        val trackedFrame1 = tracker.updateDetections(listOf(frame1Box))

        assertEquals(1, trackedFrame1.size)
        val initialTrackId = trackedFrame1[0].trackId

        // Slightly moved box in frame 2
        val frame2Box = FaceBoundingBox(0, 110, 105, 200, 200, 0.9f)
        val trackedFrame2 = tracker.updateDetections(listOf(frame2Box))

        assertEquals(1, trackedFrame2.size)
        assertEquals(initialTrackId, trackedFrame2[0].trackId)
    }

    @Test
    fun `reset clears tracking history`() {
        val frame1Box = FaceBoundingBox(0, 100, 100, 200, 200, 0.9f)
        tracker.updateDetections(listOf(frame1Box))
        assertEquals(1, tracker.getTrackedFaces().size)

        tracker.reset()
        assertTrue(tracker.getTrackedFaces().isEmpty())
    }
}
