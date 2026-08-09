package com.tupaz.ui.result

import android.net.Uri

sealed interface ResultUiState {
    data class Processing(
        val progressPercentage: Int,
        val currentStage: String,
        val elapsedTime: String,
        val remainingTime: String = "Calculating estimate…",
        val fps: Double = 30.0,
        val currentFrame: Int = 0,
        val totalFrames: Int = 0,
        val isThermallyPaused: Boolean = false,
        val thermalStatusName: String = "NORMAL"
    ) : ResultUiState

    data class ExportComplete(
        val fileName: String = "video.mp4",
        val originalVideoUri: Uri? = null,
        val enhancedVideoUri: Uri? = null,
        val originalResolution: String = "1080p (1920x1080)",
        val enhancedResolution: String = "4K (3840x2160)",
        val resTransition: String = "1080p -> 4K",
        val resDetail: String = "1920x1080 -> 3840x2160",
        val modelName: String = "RealESRGAN v4 Plus",
        val modelScale: String = "2x Upscale",
        val enhancementMode: String = "Auto (Balanced)",
        val enhancementSub: String = "Denoise · Sharpen · Recover",
        val fpsLabel: String = "30 FPS",
        val fpsSub: String = "Original FPS",
        val durationLabel: String = "01:24",
        val processingTime: String = "00:01:24",
        val outputFileSize: String = "820 MB",
        val splitPosition: Float = 0.5f,
        val isPlaying: Boolean = false,
        val isMuted: Boolean = false,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 84000L,
    ) : ResultUiState

    data class Error(
        val message: String,
        val origin: String,
        val stackTrace: String,
        val fileName: String = "video.mp4"
    ) : ResultUiState
}
