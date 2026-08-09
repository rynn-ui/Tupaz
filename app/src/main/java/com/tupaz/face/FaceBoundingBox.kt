package com.tupaz.face

/**
 * Represents face bounding box coordinates and tracking identity.
 */
data class FaceBoundingBox(
    val trackId: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val confidence: Float
) {
    /**
     * Calculates Intersection over Union (IOU) ratio with another bounding box.
     */
    fun calculateIou(other: FaceBoundingBox): Float {
        val x1 = maxOf(x, other.x)
        val y1 = maxOf(y, other.y)
        val x2 = minOf(x + width, other.x + other.width)
        val y2 = minOf(y + height, other.y + other.height)

        val intersectionWidth = maxOf(0, x2 - x1)
        val intersectionHeight = maxOf(0, y2 - y1)
        val intersectionArea = intersectionWidth * intersectionHeight

        val area1 = width * height
        val area2 = other.width * other.height
        val unionArea = area1 + area2 - intersectionArea

        return if (unionArea > 0) intersectionArea.toFloat() / unionArea else 0.0f
    }
}
