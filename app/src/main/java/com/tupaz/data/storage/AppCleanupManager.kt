package com.tupaz.data.storage

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.service.VideoProcessingService
import java.io.File

object AppCleanupManager {

    private const val TAG = "AppCleanupManager"

    fun performFullCleanup(context: Context): Boolean {
        return try {
            Log.i(TAG, "[Tupaz-Cleanup] Initiating full application cleanup...")

            try {
                val cancelIntent = Intent(context, VideoProcessingService::class.java).apply {
                    action = VideoProcessingService.ACTION_CANCEL
                }
                context.startService(cancelIntent)
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Cleanup] Error sending cancel intent to service", e)
            }

            try {
                val stopIntent = Intent(context, VideoProcessingService::class.java)
                context.stopService(stopIntent)
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Cleanup] Error stopping service", e)
            }

            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancelAll()
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Cleanup] Error clearing notifications", e)
            }

            ProcessingManager.reset(context)

            val projectStorage = ProjectStorage(context)
            try {
                val projects = projectStorage.loadProjects()
                for (project in projects) {
                    projectStorage.deleteProject(project.id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Cleanup] Error deleting projects", e)
            }

            try {
                val projectsFile = File(context.filesDir, "recent_projects.json")
                if (projectsFile.exists()) projectsFile.delete()
                val extProjectsFile = File(context.getExternalFilesDir(null), "recent_projects.json")
                if (extProjectsFile.exists()) extProjectsFile.delete()
            } catch (_: Exception) {}

            val modelStorage = ModelStorage(context)
            val modelsRootDir = modelStorage.getModelsRootDir()
            val protectedPaths = mutableSetOf<String>()
            try { protectedPaths.add(modelsRootDir.canonicalPath) } catch (_: Exception) { protectedPaths.add(modelsRootDir.absolutePath) }
            val internalModels = File(context.filesDir, "models")
            try { protectedPaths.add(internalModels.canonicalPath) } catch (_: Exception) { protectedPaths.add(internalModels.absolutePath) }
            val externalModels = context.getExternalFilesDir("models")
            if (externalModels != null) {
                try { protectedPaths.add(externalModels.canonicalPath) } catch (_: Exception) { protectedPaths.add(externalModels.absolutePath) }
            }

            context.cacheDir?.let { cacheDir ->
                cleanDirExceptModels(cacheDir, protectedPaths)
            }

            try {
                val thumbDir = File(context.cacheDir, "thumbnails")
                if (thumbDir.exists()) {
                    thumbDir.deleteRecursively()
                }
            } catch (_: Exception) {}

            cleanDirExceptModels(context.filesDir, protectedPaths)
            cleanDirExceptModels(context.getExternalFilesDir(null), protectedPaths)

            try {
                val prefs = context.getSharedPreferences("tupaz_processing_prefs", Context.MODE_PRIVATE)
                prefs.edit().clear().commit()
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Cleanup] Error clearing shared preferences", e)
            }

            try {
                modelStorage.ensureDefaultModelsProvisioned()
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Cleanup] Error provisioning default models post-cleanup", e)
            }

            ProcessingManager.reset(context)

            Log.i(TAG, "[Tupaz-Cleanup] Application cleanup completed successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Cleanup] Critical error during application cleanup", e)
            false
        }
    }

    private fun cleanDirExceptModels(dir: File?, protectedPaths: Set<String>) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { file ->
            try {
                val fileCanonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                val fileName = file.name

                val isProtected = protectedPaths.any { protectedPath ->
                    fileCanonical == protectedPath ||
                    fileCanonical.startsWith(protectedPath + File.separator) ||
                    protectedPath.startsWith(fileCanonical + File.separator)
                } || fileName == "models" || fileName.endsWith(".param") || fileName.endsWith(".bin")

                if (isProtected) {
                    return@forEach
                }

                if (fileName == "recent_projects.json") {
                    file.delete()
                } else if (file.isDirectory) {
                    val hasProtectedSubItem = protectedPaths.any { protectedPath ->
                        protectedPath.startsWith(fileCanonical + File.separator)
                    }
                    if (hasProtectedSubItem) {
                        cleanDirExceptModels(file, protectedPaths)
                    } else {
                        file.deleteRecursively()
                    }
                } else if (fileName.endsWith(".tmp") || fileName.endsWith(".mp4") || fileName.endsWith(".json") ||
                    fileName.startsWith("temp_") || fileName.startsWith("processed_") || fileName.startsWith("tupaz_enhanced_")) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
