package com.tupaz.ui.main

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
enum class ProjectStatus {
    DRAFT,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class ProjectItem(
    val id: String,
    val projectName: String = "New Project",
    val inputUriString: String? = null,
    val outputUriString: String? = null,
    val selectedQuality: String = "HIGH",
    val selectedModel: String = "realesr-animevideov3-x2",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val resolutionLabel: String = "",
    val fpsLabel: String = "",
    val durationLabel: String = "",
    val sizeLabel: String = "",
    val targetWidth: Int = 1280,
    val targetHeight: Int = 720,
    val origRes: String = "",
    val enhRes: String = "",
    val resTransition: String = "",
    val resDetail: String = "",
    val modelScale: String = "2x Scale",
    val enhancementMode: String = "Auto (Balanced)",
    val enhancementSub: String = "Denoise · Sharpen · Recover",
    val estimatedProcessingTime: String = "Calculating estimate…",
    val estimatedOutputSize: String = "Calculating estimate…",
    val realProcessingTime: String = "",
    val realOutputSize: String = "",
    val inputWidth: Int? = null,
    val inputHeight: Int? = null,
    val outputWidth: Int? = null,
    val outputHeight: Int? = null,
    val durationMs: Long? = null,
    val outputSizeBytes: Long? = null,
    val completedAt: Long? = null,
    val thumbnailPath: String? = null
) {
    val title: String get() = projectName
    val inputUri: Uri? get() = inputUriString?.let { Uri.parse(it) }
    val outputUri: Uri? get() = outputUriString?.let { Uri.parse(it) }

    val formattedResolutionPair: String
        get() {
            fun toShorthand(h: Int): String = when (h) {
                2160 -> "4K"
                1440 -> "1440p"
                1080 -> "1080p"
                720 -> "720p"
                480 -> "480p"
                else -> "${h}p"
            }
            val inH = inputHeight
            val outH = outputHeight
            if (inH != null && outH != null && inH > 0 && outH > 0) {
                return "${toShorthand(inH)} → ${toShorthand(outH)}"
            }
            if (inH != null && inH > 0) {
                return toShorthand(inH)
            }
            if (origRes.isNotBlank() && enhRes.isNotBlank()) {
                return "$origRes → $enhRes"
            }
            if (resolutionLabel.isNotBlank()) {
                return resolutionLabel
            }
            return "Resolution unavailable"
        }

    val formattedModelLabel: String
        get() {
            val scaleClean = modelScale.replace(" Scale", "").ifBlank { "2x" }
            val qualityClean = selectedQuality.ifBlank { "HIGH" }
            return "$qualityClean • $scaleClean"
        }

    val formattedDuration: String
        get() {
            val ms = durationMs
            if (ms != null && ms > 0) {
                val totalSec = ms / 1000
                val hrs = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                return if (hrs > 0) {
                    "%02d:%02d:%02d".format(hrs, mins, secs)
                } else {
                    "%02d:%02d".format(mins, secs)
                }
            }
            if (durationLabel.isNotBlank()) return durationLabel
            return "--:--"
        }

    val formattedOutputSize: String
        get() {
            val bytes = outputSizeBytes
            if (bytes != null && bytes > 0) {
                val mb = bytes / (1024.0 * 1024.0)
                return if (mb >= 1000.0) {
                    "%.1f GB".format(mb / 1024.0)
                } else {
                    "%.0f MB".format(mb)
                }
            }
            if (realOutputSize.isNotBlank()) return realOutputSize
            if (sizeLabel.isNotBlank()) return sizeLabel
            return "-- MB"
        }

    val formattedDate: String
        get() {
            val timestamp = completedAt ?: createdAt
            return try {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                sdf.format(java.util.Date(timestamp))
            } catch (_: Exception) {
                "Aug 10, 2026"
            }
        }

    val formattedStatusLabel: String
        get() = when (status) {
            ProjectStatus.COMPLETED -> "✓ Completed"
            ProjectStatus.PROCESSING -> "Processing..."
            ProjectStatus.CANCELLED -> "Cancelled"
            ProjectStatus.FAILED -> "Failed"
            ProjectStatus.DRAFT -> "Draft"
        }
}

data class MainUiState(
    val recentProjects: List<ProjectItem> = emptyList(),
    val selectedVideoUri: Uri? = null
)
