package com.tupaz.data.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var modelStorage: ModelStorage

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        val filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir
        every { context.getExternalFilesDir("models") } returns null

        modelStorage = ModelStorage(context)
    }

    @Test
    fun `getModelsRootDir resolves fallback filesDir when external is null`() {
        val root = modelStorage.getModelsRootDir()
        assertNotNull(root)
        assertTrue(root.exists())
        assertEquals("models", root.name)
    }

    @Test
    fun `isModelInstalled returns false when files are missing`() {
        assertFalse(modelStorage.isModelInstalled("model_x"))
    }

    @Test
    fun `isModelInstalled returns true when param, bin, and meta exist`() {
        val modelId = "model_installed"
        val param = modelStorage.getParamFile(modelId)
        val bin = modelStorage.getBinFile(modelId)
        val meta = modelStorage.getMetaFile(modelId)

        param.writeText("param content")
        bin.writeText("bin content")

        val localMeta = LocalModelMeta(
            modelId = modelId,
            version = "1.0.0",
            sha256 = "abc123sha",
            installedAt = System.currentTimeMillis(),
            sizeBytes = 100
        )
        modelStorage.writeLocalMeta(localMeta)

        assertTrue(modelStorage.isModelInstalled(modelId))

        val readMeta = modelStorage.getLocalMeta(modelId)
        assertNotNull(readMeta)
        assertEquals("1.0.0", readMeta?.version)
        assertEquals("abc123sha", readMeta?.sha256)
    }

    @Test
    fun `deleteModel removes directory recursively`() {
        val modelId = "model_to_delete"
        val param = modelStorage.getParamFile(modelId)
        param.writeText("test")

        assertTrue(param.exists())
        assertTrue(modelStorage.deleteModel(modelId))
        assertFalse(param.exists())
    }
}
