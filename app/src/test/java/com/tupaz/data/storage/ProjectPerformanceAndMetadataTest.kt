package com.tupaz.data.storage

import android.content.Context
import com.tupaz.domain.pipeline.AiQuality
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.main.ProjectStatus
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

class ProjectPerformanceAndMetadataTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var context: Context
    private lateinit var projectStorage: ProjectStorage

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")
        context = FakeTestContext(filesDir, cacheDir)
        projectStorage = ProjectStorage(context)
    }

    @Test
    fun testLegacyProjectJsonLoadsWithoutNewFields() {
        val legacyJson = """
            [
                {
                    "id": "proj_legacy_1",
                    "projectName": "Legacy Video",
                    "inputUriString": "file:///path/to/input.mp4",
                    "outputUriString": "file:///path/to/output.mp4",
                    "selectedQuality": "MEDIUM",
                    "selectedModel": "realesr-animevideov3-x2",
                    "createdAt": 1700000000000,
                    "updatedAt": 1700000000000,
                    "status": "COMPLETED",
                    "resolutionLabel": "1080p",
                    "fpsLabel": "30 FPS",
                    "durationLabel": "00:45",
                    "sizeLabel": "120 MB"
                }
            ]
        """.trimIndent()

        val projectsFile = File(filesDir, "recent_projects.json")
        projectsFile.writeText(legacyJson)

        val loaded = projectStorage.loadProjects()
        assertEquals(1, loaded.size)
        val proj = loaded[0]
        assertEquals("proj_legacy_1", proj.id)
        assertEquals("Legacy Video", proj.projectName)
        assertEquals("MEDIUM", proj.selectedQuality)
        assertNull(proj.inputHeight)
        assertNull(proj.outputHeight)
        assertNull(proj.completedAt)
        assertNull(proj.thumbnailPath)
        assertEquals("1080p", proj.formattedResolutionPair)
        assertEquals("MEDIUM • 2x", proj.formattedModelLabel)
    }

    @Test
    fun testNewRawMetadataSerializationAndDeserialization() {
        val project = ProjectItem(
            id = "proj_raw_1",
            projectName = "4K Anime Edit",
            inputUriString = "file:///path/input.mp4",
            outputUriString = "file:///path/output.mp4",
            selectedQuality = "HIGH",
            selectedModel = "realesr-animevideov3-x2",
            createdAt = 1770000000000L,
            completedAt = 1770003600000L,
            status = ProjectStatus.COMPLETED,
            inputWidth = 2560,
            inputHeight = 1440,
            outputWidth = 3840,
            outputHeight = 2160,
            durationMs = 102000L,
            outputSizeBytes = 402653184L,
            thumbnailPath = cacheDir.absolutePath + "/thumbnails/proj_raw_1.jpg"
        )

        projectStorage.saveProject(project)

        val loaded = projectStorage.getProject("proj_raw_1")
        assertNotNull(loaded)
        loaded!!
        assertEquals(2560, loaded.inputWidth)
        assertEquals(1440, loaded.inputHeight)
        assertEquals(3840, loaded.outputWidth)
        assertEquals(2160, loaded.outputHeight)
        assertEquals(102000L, loaded.durationMs)
        assertEquals(402653184L, loaded.outputSizeBytes)
        assertEquals(1770003600000L, loaded.completedAt)
        assertEquals(cacheDir.absolutePath + "/thumbnails/proj_raw_1.jpg", loaded.thumbnailPath)

        assertEquals("1440p → 4K", loaded.formattedResolutionPair)
        assertEquals("HIGH • 2x", loaded.formattedModelLabel)
        assertEquals("01:42", loaded.formattedDuration)
        assertEquals("384 MB", loaded.formattedOutputSize)
        assertEquals("✓ Completed", loaded.formattedStatusLabel)
    }

    @Test
    fun testResolutionFormattingShorthandsAndFallbacks() {
        val p1 = ProjectItem(id = "1", inputHeight = 1080, outputHeight = 2160)
        assertEquals("1080p → 4K", p1.formattedResolutionPair)

        val p2 = ProjectItem(id = "2", inputHeight = 720, outputHeight = 1440)
        assertEquals("720p → 1440p", p2.formattedResolutionPair)

        val p3 = ProjectItem(id = "3", inputHeight = 480)
        assertEquals("480p", p3.formattedResolutionPair)

        val p4 = ProjectItem(id = "4", origRes = "1080p", enhRes = "4K")
        assertEquals("1080p → 4K", p4.formattedResolutionPair)

        val p5 = ProjectItem(id = "5")
        assertEquals("Resolution unavailable", p5.formattedResolutionPair)
    }

    @Test
    fun testModelLabelReflectsActualSelectedQuality() {
        val lowProj = ProjectItem(id = "low", selectedQuality = "LOW", modelScale = "2x Scale")
        assertEquals("LOW • 2x", lowProj.formattedModelLabel)

        val medProj = ProjectItem(id = "med", selectedQuality = "MEDIUM", modelScale = "4x Scale")
        assertEquals("MEDIUM • 4x", medProj.formattedModelLabel)

        val highProj = ProjectItem(id = "high", selectedQuality = "HIGH", modelScale = "2x Scale")
        assertEquals("HIGH • 2x", highProj.formattedModelLabel)
    }

    @Test
    fun testRenamePreservesAllRawMetadata() {
        val original = ProjectItem(
            id = "proj_rename",
            projectName = "Original Name",
            inputHeight = 1080,
            outputHeight = 2160,
            durationMs = 60000L,
            outputSizeBytes = 1000000L,
            thumbnailPath = cacheDir.absolutePath + "/thumbnails/proj_rename.jpg"
        )
        projectStorage.saveProject(original)

        projectStorage.renameProject("proj_rename", "Renamed Name")

        val updated = projectStorage.getProject("proj_rename")
        assertNotNull(updated)
        assertEquals("Renamed Name", updated!!.projectName)
        assertEquals(1080, updated.inputHeight)
        assertEquals(2160, updated.outputHeight)
        assertEquals(60000L, updated.durationMs)
        assertEquals(1000000L, updated.outputSizeBytes)
        assertEquals(cacheDir.absolutePath + "/thumbnails/proj_rename.jpg", updated.thumbnailPath)
    }

    @Test
    fun testDeleteRemovesRecordOutputVideoAndThumbnailIdempotently() {
        val outputFile = File(cacheDir, "test_output_video.mp4")
        outputFile.writeText("dummy video content")
        val thumbDir = File(cacheDir, "thumbnails")
        thumbDir.mkdirs()
        val thumbFile = File(thumbDir, "proj_delete.jpg")
        thumbFile.writeText("dummy thumbnail content")

        val project = ProjectItem(
            id = "proj_delete",
            projectName = "Delete Test",
            outputUriString = outputFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath
        )
        projectStorage.saveProject(project)

        assertTrue(outputFile.exists())
        assertTrue(thumbFile.exists())

        projectStorage.deleteProject("proj_delete")

        assertNull(projectStorage.getProject("proj_delete"))
        assertFalse(outputFile.exists())
        assertFalse(thumbFile.exists())

        projectStorage.deleteProject("proj_delete")
    }

    @Test
    fun testUpdateStatusCancelledClearsOutputMetadata() {
        val project = ProjectItem(
            id = "proj_cancel",
            projectName = "Cancel Test",
            outputUriString = "file:///path/output.mp4",
            outputSizeBytes = 500000L,
            status = ProjectStatus.PROCESSING
        )
        projectStorage.saveProject(project)

        projectStorage.updateProjectStatus(
            projectId = "proj_cancel",
            status = ProjectStatus.CANCELLED
        )

        val updated = projectStorage.getProject("proj_cancel")
        assertNotNull(updated)
        assertEquals(ProjectStatus.CANCELLED, updated!!.status)
        assertNull(updated.outputUriString)
        assertNull(updated.outputSizeBytes)
        assertNull(updated.completedAt)
    }

    @Test
    fun testUpdateStatusFailedClearsOutputMetadata() {
        val project = ProjectItem(
            id = "proj_fail",
            projectName = "Fail Test",
            outputUriString = "file:///path/output.mp4",
            outputSizeBytes = 500000L,
            status = ProjectStatus.PROCESSING
        )
        projectStorage.saveProject(project)

        projectStorage.updateProjectStatus(
            projectId = "proj_fail",
            status = ProjectStatus.FAILED
        )

        val updated = projectStorage.getProject("proj_fail")
        assertNotNull(updated)
        assertEquals(ProjectStatus.FAILED, updated!!.status)
        assertNull(updated.outputUriString)
        assertNull(updated.outputSizeBytes)
    }

    @Test
    fun testFullAppCleanupRemovesProjectsOutputsAndThumbnailsWhilePreservingModels() {
        val projectStorage = ProjectStorage(context)
        val project = ProjectItem(
            id = "proj_cleanup",
            projectName = "Cleanup Test",
            status = ProjectStatus.COMPLETED
        )
        projectStorage.saveProject(project)

        val thumbDir = File(cacheDir, "thumbnails")
        thumbDir.mkdirs()
        val thumbFile = File(thumbDir, "proj_cleanup.jpg")
        thumbFile.writeText("thumb data")

        val modelStorage = ModelStorage(context)
        val lowParam = modelStorage.getParamFile(AiQuality.LOW.modelId)
        val lowBin = modelStorage.getBinFile(AiQuality.LOW.modelId)
        lowParam.writeText("param data")
        lowBin.writeText("bin data")

        assertTrue(thumbFile.exists())
        assertTrue(lowParam.exists())

        val result = AppCleanupManager.performFullCleanup(context)

        assertTrue(result)
        assertFalse(thumbFile.exists())
        assertTrue(lowParam.exists())
        assertTrue(projectStorage.loadProjects().isEmpty())
    }
}
