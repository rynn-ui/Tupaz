package com.tupaz.data.storage

import android.content.Context
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.main.ProjectStatus
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

    fun getProject(projectId: String): ProjectItem? {
        return loadProjects().find { it.id == projectId }
    }

    fun saveProject(project: ProjectItem) {
        val current = loadProjects().filterNot { it.id == project.id }
        val updated = listOf(project) + current
        try {
            projectsFile.writeText(json.encodeToString(updated))
        } catch (_: Exception) {}
    }

    fun updateProjectStatus(
        projectId: String,
        status: ProjectStatus,
        outputUriString: String? = null,
        realTime: String? = null,
        realSize: String? = null,
        outputSizeBytes: Long? = null,
        outputWidth: Int? = null,
        outputHeight: Int? = null,
        completedAt: Long? = null,
        thumbnailPath: String? = null
    ) {
        if (projectId.isBlank()) return
        val current = loadProjects()
        val updated = current.map { item ->
            if (item.id == projectId) {
                val isCancelled = (status == ProjectStatus.CANCELLED)
                val isFailed = (status == ProjectStatus.FAILED)
                val now = System.currentTimeMillis()
                item.copy(
                    status = status,
                    outputUriString = if (isCancelled || isFailed) null else (outputUriString ?: item.outputUriString),
                    realProcessingTime = if (isCancelled || isFailed) "" else (realTime ?: item.realProcessingTime),
                    realOutputSize = if (isCancelled || isFailed) "" else (realSize ?: item.realOutputSize),
                    outputSizeBytes = if (isCancelled || isFailed) null else (outputSizeBytes ?: item.outputSizeBytes),
                    outputWidth = if (isCancelled || isFailed) null else (outputWidth ?: item.outputWidth),
                    outputHeight = if (isCancelled || isFailed) null else (outputHeight ?: item.outputHeight),
                    completedAt = if (isCancelled || isFailed) null else (completedAt ?: item.completedAt ?: if (status == ProjectStatus.COMPLETED) now else null),
                    thumbnailPath = thumbnailPath ?: item.thumbnailPath,
                    updatedAt = now
                )
            } else {
                item
            }
        }
        try {
            projectsFile.writeText(json.encodeToString(updated))
        } catch (_: Exception) {}
    }

    fun renameProject(projectId: String, newName: String) {
        if (projectId.isBlank() || newName.isBlank()) return
        val current = loadProjects()
        val updated = current.map { item ->
            if (item.id == projectId) {
                item.copy(projectName = newName, updatedAt = System.currentTimeMillis())
            } else {
                item
            }
        }
        try {
            projectsFile.writeText(json.encodeToString(updated))
        } catch (_: Exception) {}
    }

    fun deleteProject(projectId: String) {
        if (projectId.isBlank()) return
        val projects = loadProjects()
        val target = projects.find { it.id == projectId }

        if (target != null) {
            target.outputUriString?.let { uriStr ->
                try {
                    val rawPath = if (uriStr.startsWith("file://")) {
                        uriStr.removePrefix("file://")
                    } else uriStr

                    val cleanPath = if (rawPath.startsWith("/") && rawPath.length > 2 && rawPath[2] == ':') {
                        rawPath.substring(1)
                    } else rawPath

                    val file = File(cleanPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }

            try {
                val thumbFile = File(context.cacheDir, "thumbnails/$projectId.jpg")
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            } catch (_: Exception) {}

            target.thumbnailPath?.let { path ->
                try {
                    val f = File(path)
                    if (f.exists()) f.delete()
                } catch (_: Exception) {}
            }

            try {
                context.cacheDir?.listFiles()?.forEach { file ->
                    val name = file.name
                    if (name.contains(projectId)) {
                        file.deleteRecursively()
                    }
                }
            } catch (_: Exception) {}
        }

        val updated = projects.filterNot { it.id == projectId }
        try {
            projectsFile.writeText(json.encodeToString(updated))
        } catch (_: Exception) {}
    }
}
