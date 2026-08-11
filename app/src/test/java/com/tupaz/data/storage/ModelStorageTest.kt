package com.tupaz.data.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun getModelsRootDir_resolvesFallbackFilesDir_whenExternalIsNull() {
        val root = modelStorage.getModelsRootDir()
        assertNotNull(root)
        assertTrue(root.exists())
        assertEquals("models", root.name)
    }

    @Test
    fun isModelInstalled_returnsFalse_whenFilesAreMissing() {
        assertFalse(modelStorage.isModelInstalled("model_x"))
    }

    @Test
    fun isModelInstalled_returnsFalse_whenOnlyParamExists() {
        val modelId = "model_partial_param"
        val param = modelStorage.getParamFile(modelId)
        param.writeText("param content")

        assertFalse(modelStorage.isModelInstalled(modelId))
    }

    @Test
    fun isModelInstalled_returnsFalse_whenOnlyBinExists() {
        val modelId = "model_partial_bin"
        val bin = modelStorage.getBinFile(modelId)
        bin.writeText("bin content")

        assertFalse(modelStorage.isModelInstalled(modelId))
    }

    @Test
    fun isModelInstalled_returnsFalse_whenFilesAreEmpty() {
        val modelId = "model_empty"
        val param = modelStorage.getParamFile(modelId)
        val bin = modelStorage.getBinFile(modelId)
        param.createNewFile()
        bin.createNewFile()

        assertFalse(modelStorage.isModelInstalled(modelId))
    }

    @Test
    fun isModelInstalled_returnsTrue_whenBothParamAndBinExistNonEmpty() {
        val modelId = "model_installed"
        val param = modelStorage.getParamFile(modelId)
        val bin = modelStorage.getBinFile(modelId)

        param.writeText("param content")
        bin.writeText("bin content")

        assertTrue(modelStorage.isModelInstalled(modelId))
    }

    @Test
    fun deleteModel_removesDirectoryRecursively() {
        val modelId = "model_to_delete"
        val param = modelStorage.getParamFile(modelId)
        param.writeText("test")

        assertTrue(param.exists())
        assertTrue(modelStorage.deleteModel(modelId))
        assertFalse(param.exists())
    }
}
