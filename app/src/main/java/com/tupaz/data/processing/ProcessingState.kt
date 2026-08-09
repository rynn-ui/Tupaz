package com.tupaz.data.processing

import android.net.Uri

enum class ProcessingStatus {
    IDLE,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ProcessingJobConfig(
    val fileName: String = "",
    val inputUriString: String? = null,
    val targetWidth: Int = 1280,
    val targetHeight: Int = 720,
    val origRes: String = "",
    val enhRes: String = "",
    val resTransition: String = "",
    val resDetail: String = "",
    val modelName: String = "realesr-animevideov3-x2",
    val modelScale: String = "2x Scale",
    val enhancementMode: String = "Auto (Balanced)",
    val enhancementSub: String = "Denoise · Sharpen · Recover",
    val fpsLabel: String = "30 FPS",
    val durationLabel: String = "00:00",
    val estimatedProcessingTime: String = "Calculating estimate…",
    val estimatedOutputSize: String = "Calculating estimate…"
) {
    val inputUri: Uri?
        get() = inputUriString?.let { Uri.parse(it) }
}

data class ProcessingState(
    val status: ProcessingStatus = ProcessingStatus.IDLE,
    val config: ProcessingJobConfig = ProcessingJobConfig(),
    val totalFrames: Int = 0,
    val processedFrames: Int = 0,
    val progressPercentage: Int = 0,
    val currentStage: String = "",
    val elapsedTime: String = "00:00",
    val remainingTime: String = "Calculating estimate…",
    val processingFps: Float = 0f,
    val outputUriString: String? = null,
    val errorMessage: String? = null,
    val startTimeMs: Long = 0L,
    val realProcessingTime: String = "",
    val realOutputSize: String = "",
    val isThermallyPaused: Boolean = false,
    val thermalStatusName: String = "NORMAL"
) {
    val outputUri: Uri?
        get() = outputUriString?.let { Uri.parse(it) }
}
