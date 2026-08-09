package com.tupaz.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TileSchedulerTest {

    private lateinit var scheduler: TileScheduler

    @Before
    fun setUp() {
        scheduler = TileScheduler(defaultOverlap = 16)
    }

    @Test
    fun `determineTileSize selects 512 for high VRAM`() {
        assertEquals(512, scheduler.determineTileSize(2000L))
    }

    @Test
    fun `determineTileSize selects 256 for mid VRAM`() {
        assertEquals(256, scheduler.determineTileSize(1000L))
    }

    @Test
    fun `determineTileSize selects 128 for low VRAM`() {
        assertEquals(128, scheduler.determineTileSize(500L))
    }

    @Test
    fun `computeGrid partitions image into correct tile count and bounds`() {
        // 1920x1080 image with 512x512 tile size
        val grid = scheduler.computeGrid(
            imageWidth = 1920,
            imageHeight = 1080,
            tileSize = 512,
            overlap = 16
        )

        // 1920 / 512 = 3.75 -> 4 tiles in X; 1080 / 512 = 2.11 -> 3 tiles in Y; Total = 12 tiles
        assertEquals(4, grid.tilesX)
        assertEquals(3, grid.tilesY)
        assertEquals(12, grid.tiles.size)

        // First tile (top-left)
        val firstTile = grid.tiles[0]
        assertEquals(0, firstTile.x)
        assertEquals(0, firstTile.y)
        assertEquals(512, firstTile.width)
        assertEquals(512, firstTile.height)
        assertEquals(0, firstTile.padLeft)
        assertEquals(0, firstTile.padTop)
        assertEquals(16, firstTile.padRight)
        assertEquals(16, firstTile.padBottom)

        // Last tile (bottom-right)
        val lastTile = grid.tiles.last()
        assertEquals(1536, lastTile.x)
        assertEquals(1024, lastTile.y)
        assertEquals(384, lastTile.width)
        assertEquals(56, lastTile.height)
        assertEquals(16, lastTile.padLeft)
        assertEquals(16, lastTile.padTop)
        assertEquals(0, lastTile.padRight)
        assertEquals(0, lastTile.padBottom)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `computeGrid throws exception for non-positive dimensions`() {
        scheduler.computeGrid(0, 1080, 256)
    }
}
