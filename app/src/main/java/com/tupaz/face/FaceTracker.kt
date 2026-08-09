package com.tupaz.face

import android.util.Log

/**
 * Pure Kotlin IOU face tracker maintaining bounding box persistence across keyframes according to RULES.md and PERFORMANCE.md.
 * Reduces full face detection calls from every frame (~8ms) to ~1ms between keyframes.
 */
class FaceTracker(
    private val iouThreshold: Float = 0.4f
) {
    companion object {
        private const val TAG = "FaceTracker"
    }

    private var activeTrackedFaces = mutableListOf<FaceBoundingBox>()
    private var nextTrackId = 1

    /**
     * Updates tracker state with newly detected face bounding boxes.
     */
    fun updateDetections(detections: List<FaceBoundingBox>): List<FaceBoundingBox> {
        val updatedTracks = mutableListOf<FaceBoundingBox>()

        for (det in detections) {
            val matchedTrack = activeTrackedFaces.firstOrNull { it.calculateIou(det) >= iouThreshold }
            val trackId = matchedTrack?.trackId ?: nextTrackId++

            updatedTracks.add(det.copy(trackId = trackId))
        }

        activeTrackedFaces = updatedTracks
        Log.d(TAG, "Updated face tracker state: ${activeTrackedFaces.size} active tracks")
        return activeTrackedFaces
    }

    /**
     * Gets currently tracked face positions for intermediate frames without running neural detector.
     */
    fun getTrackedFaces(): List<FaceBoundingBox> = activeTrackedFaces.toList()

    /**
     * Resets internal tracking state (called on scene cut or seek).
     */
    fun reset() {
        activeTrackedFaces.clear()
        nextTrackId = 1
    }
}
