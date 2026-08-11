package com.tupaz.data.storage

import android.content.Context
import com.tupaz.domain.pipeline.AiQuality
import com.tupaz.pipeline.VideoUpscaler
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelRecoveryAndErrorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var modelStorage: ModelStorage

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        context = FakeTestContext(filesDir, cacheDir)

        modelStorage = ModelStorage(context)
    }

    @Test
    fun isModelInstalled_requiresBothParamAndBinWithNonZeroLength() {
        val modelId = AiQuality.HIGH.modelId
        val param = modelStorage.getParamFile(modelId)
        val bin = modelStorage.getBinFile(modelId)

        // Initially neither exists
        assertFalse(modelStorage.isModelInstalled(modelId))

        // Only param exists
        param.writeText("param content")
        assertFalse(modelStorage.isModelInstalled(modelId))

        // Empty bin created
        bin.createNewFile()
        assertFalse(modelStorage.isModelInstalled(modelId))

        // Bin has content
        bin.writeText("bin content")
        assertTrue(modelStorage.isModelInstalled(modelId))
    }

    @Test
    fun resolveModelId_resolvesLowMediumHighQualitiesCorrectly() {
        val resolveMethod = VideoUpscaler::class.java.getDeclaredMethod("resolveModelId", String::class.java)
        resolveMethod.isAccessible = true

        val lowResolved = resolveMethod.invoke(VideoUpscaler, "LOW") as String
        assertEquals(AiQuality.LOW.modelId, lowResolved)

        val medResolved = resolveMethod.invoke(VideoUpscaler, "MEDIUM") as String
        assertEquals(AiQuality.MEDIUM.modelId, medResolved)

        val highResolved = resolveMethod.invoke(VideoUpscaler, "HIGH") as String
        assertEquals(AiQuality.HIGH.modelId, highResolved)

        val lowDisplayNameResolved = resolveMethod.invoke(VideoUpscaler, AiQuality.LOW.modelDisplayName) as String
        assertEquals(AiQuality.LOW.modelId, lowDisplayNameResolved)

        val medDisplayNameResolved = resolveMethod.invoke(VideoUpscaler, AiQuality.MEDIUM.modelDisplayName) as String
        assertEquals(AiQuality.MEDIUM.modelId, medDisplayNameResolved)

        val highDisplayNameResolved = resolveMethod.invoke(VideoUpscaler, AiQuality.HIGH.modelDisplayName) as String
        assertEquals(AiQuality.HIGH.modelId, highDisplayNameResolved)
    }
}
