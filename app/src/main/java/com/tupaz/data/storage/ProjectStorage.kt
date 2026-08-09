package com.tupaz.data.storage

import android.content.Context
import com.tupaz.ui.main.ProjectItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages persistent storage of recent video enhancement projects.
 */
class ProjectStorage(
    private val context: Context,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {
    private val projectsFile: File
        get() = File(context.filesDir, "recent_projects.json")

    fun loadProjects(): List<ProjectItem> {
        if (!projectsFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<ProjectItem>>(projectsFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveProject(project: ProjectItem) {
        val current = loadProjects().filterNot { it.id == project.id }
        val updated = listOf(project) + current
        try {
            projectsFile.writeText(json.encodeToString(updated))
        } catch (_: Exception) {}
    }

    fun deleteProject(projectId: String) {
        val updated = loadProjects().filterNot { it.id == projectId }
        try {
            projectsFile.writeText(json.encodeToString(updated))
        } catch (_: Exception) {}
    }
}
