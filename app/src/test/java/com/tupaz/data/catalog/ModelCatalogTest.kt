package com.tupaz.data.catalog

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelCatalogTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var catalog: ModelCatalog

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        val cacheDir = tempFolder.newFolder("cache")
        every { context.cacheDir } returns cacheDir

        catalog = ModelCatalog(context)
    }

    @Test
    fun `loadFromCache returns null when cache file does not exist`() {
        assertNull(catalog.loadFromCache())
    }

    @Test
    fun `saveToCache writes manifest and loadFromCache retrieves it`() {
        val sampleItem = ModelCatalogItem(
            modelId = "esrgan_2x",
            name = "ESRGAN 2x",
            description = "Upscale model",
            version = "1.2.0",
            binUrl = "https://example.com/model.bin",
            paramUrl = "https://example.com/model.param",
            sha256 = "1234567890abcdef",
            sizeBytes = 1048576,
            requiredForModes = listOf("balanced")
        )
        val manifest = ModelCatalogManifest(
            version = 1,
            models = listOf(sampleItem)
        )

        catalog.saveToCache(manifest)

        val cached = catalog.loadFromCache()
        assertNotNull(cached)
        assertEquals(1, cached?.version)
        assertEquals(1, cached?.models?.size)
        assertEquals("esrgan_2x", cached?.models?.get(0)?.modelId)
        assertEquals("1.2.0", cached?.models?.get(0)?.version)
    }
}
