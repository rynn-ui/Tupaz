package com.tupaz.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.main.ProjectStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Asynchronous manager for extracting and caching small video thumbnails.
 */
object ProjectThumbnailManager {

    private const val TAG = "ProjectThumbnailManager"
    private const val MAX_THUMBNAIL_DIMENSION = 320

    fun getThumbnailFile(context: Context, projectId: String): File {
        val dir = File(context.cacheDir, "thumbnails")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$projectId.jpg")
    }

    suspend fun getOrGenerateThumbnail(context: Context, project: ProjectItem): File? {
        return withContext(Dispatchers.IO) {
            val thumbFile = getThumbnailFile(context, project.id)
            if (thumbFile.exists() && thumbFile.length() > 0) {
                return@withContext thumbFile
            }

            val sourceUriStr = if (project.status == ProjectStatus.COMPLETED) {
                project.outputUriString ?: project.inputUriString
            } else {
                project.inputUriString ?: project.outputUriString
            }

            if (sourceUriStr.isNullOrEmpty()) return@withContext null

            val retriever = MediaMetadataRetriever()
            var rawFrame: Bitmap? = null
            var scaledFrame: Bitmap? = null

            try {
                if (sourceUriStr.startsWith("content://") || sourceUriStr.startsWith("file://")) {
                    retriever.setDataSource(context, Uri.parse(sourceUriStr))
                } else {
                    val file = File(sourceUriStr)
                    if (!file.exists()) return@withContext null
                    retriever.setDataSource(file.absolutePath)
                }

                rawFrame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0)

                if (rawFrame != null) {
                    val width = rawFrame.width
                    val height = rawFrame.height

                    val scale = (MAX_THUMBNAIL_DIMENSION.toFloat() / maxOf(width, height)).coerceAtMost(1.0f)
                    val targetW = (width * scale).toInt().coerceAtLeast(1)
                    val targetH = (height * scale).toInt().coerceAtLeast(1)

                    scaledFrame = Bitmap.createScaledBitmap(rawFrame, targetW, targetH, true)

                    FileOutputStream(thumbFile).use { out ->
                        scaledFrame.compress(Bitmap.CompressFormat.JPEG, 75, out)
                        out.flush()
                    }
                    return@withContext thumbFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "Thumbnail generation failed for project ${project.id}", e)
                if (thumbFile.exists()) {
                    try { thumbFile.delete() } catch (_: Exception) {}
                }
            } finally {
                try { retriever.release() } catch (_: Exception) {}
                if (scaledFrame != null && scaledFrame != rawFrame) {
                    scaledFrame.recycle()
                }
                rawFrame?.recycle()
            }
            null
        }
    }
}
