package com.tupaz.data.storage

import android.content.Context
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.main.ProjectStatus
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

class ProjectStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var storage: ProjectStorage
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir
        storage = ProjectStorage(context)
    }

    @Test
    fun testCreateProjectWithDefaultName() {
        val projId = "proj_${System.currentTimeMillis()}_1"
        val project = ProjectItem(
            id = projId,
            projectName = "New Project",
            inputUriString = "file:///tmp/input.mp4",
            status = ProjectStatus.PROCESSING
        )
        storage.saveProject(project)

        val loaded = storage.loadProjects()
        assertEquals(1, loaded.size)
        assertEquals("New Project", loaded[0].projectName)
        assertEquals(projId, loaded[0].id)
        assertEquals(ProjectStatus.PROCESSING, loaded[0].status)
    }

    @Test
    fun testCreateProjectWithCustomName() {
        val projId = "proj_${System.currentTimeMillis()}_2"
        val project = ProjectItem(
            id = projId,
            projectName = "Anime Edit",
            inputUriString = "file:///tmp/anime.mp4",
            status = ProjectStatus.PROCESSING
        )
        storage.saveProject(project)

        val loaded = storage.loadProjects()
        assertEquals(1, loaded.size)
        assertEquals("Anime Edit", loaded[0].projectName)
        assertEquals(projId, loaded[0].id)
    }

    @Test
    fun testProjectHasUniqueIdSeparateFromProjectName() {
        val id1 = "proj_111"
        val id2 = "proj_222"
        val proj1 = ProjectItem(id = id1, projectName = "New Project")
        val proj2 = ProjectItem(id = id2, projectName = "New Project")

        storage.saveProject(proj1)
        storage.saveProject(proj2)

        val loaded = storage.loadProjects()
        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.id == id1 })
        assertTrue(loaded.any { it.id == id2 })
    }

    @Test
    fun testRenameProjectPreservesIdAndOutput() {
        val projId = "proj_rename_1"
        val outputUri = "file:///tmp/output_video.mp4"
        val project = ProjectItem(
            id = projId,
            projectName = "Initial Name",
            outputUriString = outputUri,
            status = ProjectStatus.COMPLETED
        )
        storage.saveProject(project)

        storage.renameProject(projId, "Renamed 4K Video")

        val fetched = storage.getProject(projId)
        assertNotNull(fetched)
        assertEquals("Renamed 4K Video", fetched!!.projectName)
        assertEquals(projId, fetched.id)
        assertEquals(outputUri, fetched.outputUriString)
        assertEquals(ProjectStatus.COMPLETED, fetched.status)
    }

    @Test
    fun testUpdateProjectStatusAndOutputUri() {
        val projId = "proj_status_1"
        val project = ProjectItem(
            id = projId,
            projectName = "Status Project",
            status = ProjectStatus.PROCESSING
        )
        storage.saveProject(project)

        val outputUri = "file:///tmp/out.mp4"
        storage.updateProjectStatus(
            projectId = projId,
            status = ProjectStatus.COMPLETED,
            outputUriString = outputUri,
            realTime = "00:45",
            realSize = "24.5 MB"
        )

        val updated = storage.getProject(projId)
        assertNotNull(updated)
        assertEquals(ProjectStatus.COMPLETED, updated!!.status)
        assertEquals(outputUri, updated.outputUriString)
        assertEquals("00:45", updated.realProcessingTime)
        assertEquals("24.5 MB", updated.realOutputSize)
    }

    @Test
    fun testDeleteProjectRemovesFromStorageAndCleansFile() {
        val dummyOutputFile = File(filesDir, "test_output_video.mp4")
        dummyOutputFile.writeText("dummy video content")
        assertTrue(dummyOutputFile.exists())

        val projId = "proj_del_1"
        val project = ProjectItem(
            id = projId,
            projectName = "Project to Delete",
            outputUriString = "file://${dummyOutputFile.absolutePath}",
            status = ProjectStatus.COMPLETED
        )
        storage.saveProject(project)
        assertEquals(1, storage.loadProjects().size)

        storage.deleteProject(projId)

        assertEquals(0, storage.loadProjects().size)
        assertNull(storage.getProject(projId))
        // Verify output file was cleaned up
        assertTrue(!dummyOutputFile.exists())
    }

    @Test
    fun testProjectPersistenceAfterStorageReload() {
        val projId = "proj_reload_1"
        val project = ProjectItem(
            id = projId,
            projectName = "Persistent Project",
            status = ProjectStatus.COMPLETED
        )
        storage.saveProject(project)

        // Instantiate new storage instance simulating app restart
        val newStorageInstance = ProjectStorage(context)
        val loaded = newStorageInstance.loadProjects()

        assertEquals(1, loaded.size)
        assertEquals(projId, loaded[0].id)
        assertEquals("Persistent Project", loaded[0].projectName)
    }
}
