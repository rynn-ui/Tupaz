package com.tupaz.pipeline

import kotlin.math.ceil

/**
 * Data class representing a single tile partition of an image.
 */
data class TileInfo(
    val tileIndex: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val padLeft: Int,
    val padTop: Int,
    val padRight: Int,
    val padBottom: Int
)

/**
 * Tile grid layout configuration.
 */
data class TileGridConfig(
    val imageWidth: Int,
    val imageHeight: Int,
    val tileSize: Int,
    val overlap: Int,
    val tilesX: Int,
    val tilesY: Int,
    val tiles: List<TileInfo>
)

/**
 * Calculates optimal tile partitioning grids for high-resolution AI upscaling.
 * Pure Kotlin — zero Android dependencies.
 */
class TileScheduler(
    private val defaultOverlap: Int = 16
) {
    /**
     * Determines optimal tile size in pixels based on available VRAM in megabytes.
     */
    fun determineTileSize(availableVramMb: Long): Int {
        return when {
            availableVramMb > 1500 -> 512
            availableVramMb > 800 -> 256
            else -> 128
        }
    }

    /**
     * Computes complete tile grid configuration for an image.
     */
    fun computeGrid(
        imageWidth: Int,
        imageHeight: Int,
        tileSize: Int,
        overlap: Int = defaultOverlap
    ): TileGridConfig {
        require(imageWidth > 0) { "imageWidth must be positive" }
        require(imageHeight > 0) { "imageHeight must be positive" }
        require(tileSize > 0) { "tileSize must be positive" }

        val tilesX = ceil(imageWidth.toDouble() / tileSize).toInt()
        val tilesY = ceil(imageHeight.toDouble() / tileSize).toInt()
        val tiles = mutableListOf<TileInfo>()

        var index = 0
        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                val x = tx * tileSize
                val y = ty * tileSize
                val w = minOf(tileSize, imageWidth - x)
                val h = minOf(tileSize, imageHeight - y)

                val padLeft = if (x > 0) overlap else 0
                val padTop = if (y > 0) overlap else 0
                val padRight = if (x + w < imageWidth) overlap else 0
                val padBottom = if (y + h < imageHeight) overlap else 0

                tiles.add(
                    TileInfo(
                        tileIndex = index++,
                        x = x,
                        y = y,
                        width = w,
                        height = h,
                        padLeft = padLeft,
                        padTop = padTop,
                        padRight = padRight,
                        padBottom = padBottom
                    )
                )
            }
        }

        return TileGridConfig(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            tileSize = tileSize,
            overlap = overlap,
            tilesX = tilesX,
            tilesY = tilesY,
            tiles = tiles
        )
    }
}
