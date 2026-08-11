package com.tupaz.ui.result

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tupaz.data.processing.ProcessingJobConfig
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(
        ResultUiState.Processing(
            progressPercentage = 0,
            currentStage = "Initializing AI Engine...",
            elapsedTime = "00:00",
            remainingTime = "Calculating estimate…"
        )
    )
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ProcessingManager.state.collect { pState ->
                val config = pState.config
                val parts = config.durationLabel.split(":")
                val durMs = if (parts.size == 2) {
                    ((parts[0].toLongOrNull() ?: 1) * 60 + (parts[1].toLongOrNull() ?: 24)) * 1000L
                } else 84000L

                val exportCompleteState = ResultUiState.ExportComplete(
                    fileName = config.fileName,
                    originalVideoUri = config.inputUri,
                    enhancedVideoUri = pState.outputUri ?: config.inputUri,
                    originalResolution = config.origRes,
                    enhancedResolution = config.enhRes,
                    resTransition = config.resTransition,
                    resDetail = config.resDetail,
                    modelName = config.modelName,
                    modelScale = config.modelScale,
                    enhancementMode = config.enhancementMode,
                    enhancementSub = config.enhancementSub,
                    fpsLabel = config.fpsLabel,
                    fpsSub = "Original FPS",
                    durationLabel = config.durationLabel,
                    processingTime = pState.realProcessingTime.ifEmpty { config.estimatedProcessingTime },
                    outputFileSize = pState.realOutputSize.ifEmpty { config.estimatedOutputSize },
                    durationMs = durMs
                )

                when (pState.status) {
                    ProcessingStatus.IDLE -> {
                        _uiState.value = ResultUiState.Processing(
                            progressPercentage = 0,
                            currentStage = "Ready",
                            elapsedTime = "00:00",
                            remainingTime = "Calculating estimate…"
                        )
                    }
                    ProcessingStatus.PROCESSING -> {
                        _uiState.value = ResultUiState.Processing(
                            progressPercentage = pState.progressPercentage,
                            currentStage = if (pState.isThermallyPaused)
                                "🌡️ Cooling down (${pState.thermalStatusName}) — Paused"
                            else
                                pState.currentStage.ifEmpty { "AI Processing..." },
                            elapsedTime = pState.elapsedTime,
                            remainingTime = pState.remainingTime,
                            currentFrame = pState.processedFrames,
                            totalFrames = pState.totalFrames,
                            isThermallyPaused = pState.isThermallyPaused,
                            thermalStatusName = pState.thermalStatusName
                        )
                    }
                    ProcessingStatus.COMPLETED -> {
                        _uiState.value = exportCompleteState
                    }
                    ProcessingStatus.FAILED -> {
                        _uiState.value = ResultUiState.Error(
                            message = pState.errorMessage ?: "Video processing failed",
                            origin = "VideoProcessingService",
                            stackTrace = pState.errorMessage ?: "Unknown error",
                            fileName = config.fileName
                        )
                    }
                    ProcessingStatus.CANCELLED -> {
                        _uiState.value = ResultUiState.Cancelled
                    }
                }
            }
        }
    }

    fun startJob(context: Context, config: ProcessingJobConfig) {
        ProcessingManager.startProcessing(context, config)
    }

    fun cancelProcessing(context: Context) {
        ProcessingManager.cancelProcessing(context)
    }

    fun resetJob(context: Context) {
        ProcessingManager.reset(context)
    }

    fun updateSplitPosition(position: Float) {
        val current = _uiState.value
        if (current is ResultUiState.ExportComplete) {
            _uiState.value = current.copy(splitPosition = position.coerceIn(0.01f, 0.99f))
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value
        if (current is ResultUiState.ExportComplete) {
            _uiState.value = current.copy(isPlaying = !current.isPlaying)
        }
    }

    fun toggleMute() {
        val current = _uiState.value
        if (current is ResultUiState.ExportComplete) {
            _uiState.value = current.copy(isMuted = !current.isMuted)
        }
    }

    fun seekToPositionMs(positionMs: Long) {
        val current = _uiState.value
        if (current is ResultUiState.ExportComplete) {
            val clamped = positionMs.coerceIn(0L, current.durationMs)
            _uiState.value = current.copy(currentPositionMs = clamped)
        }
    }

    fun updateCurrentPosition(positionMs: Long) {
        val current = _uiState.value
        if (current is ResultUiState.ExportComplete) {
            _uiState.value = current.copy(currentPositionMs = positionMs)
        }
    }
}
