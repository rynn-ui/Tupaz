package com.tupaz.cache

import com.tupaz.data.storage.ModelStorage
import com.tupaz.pipeline.NcnnBridge
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelVramCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storage: ModelStorage
    private lateinit var ncnnBridge: NcnnBridge
    private lateinit var vramCache: ModelVramCache

    @Before
    fun setUp() {
        storage = mockk(relaxed = true)
        ncnnBridge = mockk(relaxed = true)

        // Set maximum budget to 100 MB for test
        vramCache = ModelVramCache(
            modelStorage = storage,
            ncnnBridge = ncnnBridge,
            maxVramBudgetBytes = 100 * 1024 * 1024L
        )
    }

    @Test
    fun `getOrLoad returns null if model is not installed on disk`() {
        every { storage.isModelInstalled("model_uninstalled") } returns false

        val handle = vramCache.getOrLoad("model_uninstalled")
        assertNull(handle)
    }

    @Test
    fun `getOrLoad loads model when installed and caches handle`() {
        val modelId = "model_installed"
        val paramFile = tempFolder.newFile("model.param")
        val binFile = tempFolder.newFile("model.bin")
        binFile.writeText("weights")

        every { storage.isModelInstalled(modelId) } returns true
        every { storage.getParamFile(modelId) } returns paramFile
        every { storage.getBinFile(modelId) } returns binFile

        val handle1 = vramCache.getOrLoad(modelId)
        assertNotNull(handle1)
        assertEquals(modelId, handle1?.modelId)
        assertEquals(paramFile.absolutePath, handle1?.paramPath)

        // Second call should return cached instance
        val handle2 = vramCache.getOrLoad(modelId)
        assertEquals(handle1, handle2)
        assertEquals(1, vramCache.getCachedModelIds().size)
    }

    @Test
    fun `evict removes model from cache`() {
        val modelId = "model_evict"
        val paramFile = tempFolder.newFile("evict.param")
        val binFile = tempFolder.newFile("evict.bin")

        every { storage.isModelInstalled(modelId) } returns true
        every { storage.getParamFile(modelId) } returns paramFile
        every { storage.getBinFile(modelId) } returns binFile

        vramCache.getOrLoad(modelId)
        assertEquals(1, vramCache.getCachedModelIds().size)

        vramCache.evict(modelId)
        assertEquals(0, vramCache.getCachedModelIds().size)
    }
}
