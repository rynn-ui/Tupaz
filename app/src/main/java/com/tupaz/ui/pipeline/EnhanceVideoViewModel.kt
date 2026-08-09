package com.tupaz.ui.pipeline

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import com.tupaz.data.storage.ModelStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EnhanceVideoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EnhanceUiState())
    val uiState: StateFlow<EnhanceUiState> = _uiState.asStateFlow()

    fun refreshInstalledModels(context: Context) {
        val storage = ModelStorage(context)
        storage.ensureDefaultModelsProvisioned()
        val defaultModels = listOf(
            ModelOption("realesr-animevideov3-x2", "RealESRGAN AnimeVideo v3 (2x)", "Official Real-ESRGAN model for 2x video upscaling.", isInstalled = storage.isModelInstalled("realesr-animevideov3-x2"))
        )

        val installedModels = defaultModels.filter { it.isInstalled }
        val selectedModelName = if (installedModels.any { it.name == _uiState.value.selectedModel }) {
            _uiState.value.selectedModel
        } else if (installedModels.isNotEmpty()) {
            installedModels.first().name
        } else {
            "No Model Installed"
        }

        val selectedDesc = installedModels.find { it.name == selectedModelName }?.description
            ?: "Please download an AI model from Model Store to start processing."
        val detectedScale = if (selectedModelName.contains("4x") || selectedModelName.contains("x4")) "4x" else "2x"

        _uiState.update {
            it.copy(
                availableModels = defaultModels,
                selectedModel = selectedModelName,
                selectedModelDescription = selectedDesc,
                selectedScaleFactor = detectedScale
            )
        }
    }

    fun loadVideoFromUri(uri: Uri, context: Context) {
        try {
            if (uri.scheme == "content") {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        } catch (_: Exception) {}

        var name = "selected_video.mp4"
        var sizeBytes = 0L
        var durationMs = 0L
        var width = 1920
        var height = 1080
        var fps = 30

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment ?: name
        }

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)

            if (durStr != null) durationMs = durStr.toLongOrNull() ?: 0L
            if (wStr != null) width = wStr.toIntOrNull() ?: 1920
            if (hStr != null) height = hStr.toIntOrNull() ?: 1080
            if (fpsStr != null) fps = fpsStr.toFloatOrNull()?.toInt()?.coerceIn(1, 120) ?: 30
            retriever.release()
        } catch (_: Exception) {}

        val sizeMb = if (sizeBytes > 0) sizeBytes / (1024.0 * 1024.0) else 0.0
        val sizeFormatted = if (sizeMb >= 1000.0) "%.2f GB".format(sizeMb / 1024.0) else if (sizeMb > 0) "%.1f MB".format(sizeMb) else "-- MB"

        val totalSec = if (durationMs > 0) (durationMs / 1000).toInt() else 0
        val durMin = totalSec / 60
        val durSec = totalSec % 60
        val durationFormatted = "%02d:%02d".format(durMin, durSec)

        val resLabel = "${height}p (${width}x${height})"

        _uiState.update { currentState ->
            val (estTime, estSize) = calculateExactEstimates(
                sizeBytes,
                durationMs,
                width,
                height,
                fps,
                currentState.selectedModel,
                currentState.selectedAiMode,
                currentState.selectedScaleFactor
            )
            currentState.copy(
                fileName = name,
                resolutionLabel = resLabel,
                videoWidth = width,
                videoHeight = height,
                videoFps = fps,
                durationMs = durationMs,
                fpsLabel = "$fps FPS",
                durationLabel = durationFormatted,
                fileSizeLabel = sizeFormatted,
                videoSizeBytes = sizeBytes,
                videoUri = uri,
                estimatedOutputTime = estTime,
                estimatedOutputSize = estSize
            )
        }
        refreshInstalledModels(context)
    }

    fun selectModel(modelName: String) {
        val detectedScale = if (modelName.contains("4x") || modelName.contains("x4")) "4x" else "2x"
        _uiState.update { currentState ->
            val description = currentState.availableModels.find { it.name == modelName }?.description
                ?: "Best balance of quality and performance."
            val (estTime, estSize) = calculateExactEstimates(
                currentState.videoSizeBytes,
                currentState.durationMs,
                currentState.videoWidth,
                currentState.videoHeight,
                currentState.videoFps,
                modelName,
                currentState.selectedAiMode,
                detectedScale
            )
            currentState.copy(
                selectedModel = modelName,
                selectedModelDescription = description,
                selectedScaleFactor = detectedScale,
                estimatedOutputTime = estTime,
                estimatedOutputSize = estSize
            )
        }
    }

    fun selectScaleFactor(scale: String) {
        _uiState.update { currentState ->
            val (estTime, estSize) = calculateExactEstimates(
                currentState.videoSizeBytes,
                currentState.durationMs,
                currentState.videoWidth,
                currentState.videoHeight,
                currentState.videoFps,
                currentState.selectedModel,
                currentState.selectedAiMode,
                scale
            )
            currentState.copy(
                selectedScaleFactor = scale,
                estimatedOutputTime = estTime,
                estimatedOutputSize = estSize
            )
        }
    }

    fun selectVideoType(type: String) {
        _uiState.update { it.copy(videoType = type) }
    }

    fun selectAiMode(mode: AiModeSelection) {
        _uiState.update { currentState ->
            val (estTime, estSize) = calculateExactEstimates(
                currentState.videoSizeBytes,
                currentState.durationMs,
                currentState.videoWidth,
                currentState.videoHeight,
                currentState.videoFps,
                currentState.selectedModel,
                mode,
                currentState.selectedScaleFactor
            )
            currentState.copy(
                selectedAiMode = mode,
                estimatedOutputTime = estTime,
                estimatedOutputSize = estSize
            )
        }
    }

    fun updateDenoise(value: Float) {
        _uiState.update { it.copy(denoiseValue = value) }
    }

    fun updateRecoverDetail(value: Float) {
        _uiState.update { it.copy(recoverDetailValue = value) }
    }

    fun updateSharpen(value: Float) {
        _uiState.update { it.copy(sharpenValue = value) }
    }

    fun updateReduceNoise(value: Float) {
        _uiState.update { it.copy(reduceNoiseValue = value) }
    }

    fun updateDehalo(value: Float) {
        _uiState.update { it.copy(dehaloValue = value) }
    }

    private fun calculateExactEstimates(
        sizeBytes: Long,
        durationMs: Long,
        width: Int,
        height: Int,
        fps: Int,
        modelName: String,
        mode: AiModeSelection,
        scaleFactorStr: String = "2x"
    ): Pair<String, String> {
        if (durationMs <= 0L || width <= 0 || height <= 0) {
            return Pair("Calculating estimate…", "Calculating estimate…")
        }

        val durationSec = durationMs / 1000.0
        val effectiveFps = if (fps > 0) fps else 30
        val totalFrames = (durationSec * effectiveFps).toInt().coerceAtLeast(1)

        val scaleFactor = scaleFactorStr.filter { it.isDigit() }.toIntOrNull() ?: 2
        val targetWidth = width * scaleFactor
        val targetHeight = height * scaleFactor

        val resRatio = (width.toDouble() * height) / (1920.0 * 1080.0)
        val modelSpeedMultiplier = when {
            modelName.contains("Anime", ignoreCase = true) -> 1.0
            modelName.contains("v4", ignoreCase = true) -> 1.2
            else -> 1.0
        }
        val modeMultiplier = if (mode == AiModeSelection.AUTO) 1.0 else 1.15
        val scaleTimeMultiplier = if (scaleFactor == 4) 2.2 else 1.0

        val secPerFrame = (0.18 * resRatio * modelSpeedMultiplier * modeMultiplier * scaleTimeMultiplier).coerceAtLeast(0.04)
        val estTotalSec = (totalFrames * secPerFrame).toInt().coerceAtLeast(1)

        val estMin = estTotalSec / 60
        val estSec = estTotalSec % 60
        val timeFormatted = if (estTotalSec >= 3600) {
            val estHrs = estTotalSec / 3600
            "%02d:%02d:%02d".format(estHrs, estMin % 60, estSec)
        } else {
            "%02d:%02d".format(estMin, estSec)
        }

        val targetPixels = targetWidth.toLong() * targetHeight
        val bitsPerPixelPerFrame = 0.08
        val targetBitrateBps = (targetPixels * effectiveFps * bitsPerPixelPerFrame).toLong().coerceIn(2_000_000L, 30_000_000L)
        val calculatedSizeBytes = (durationSec * (targetBitrateBps / 8.0)).toLong()

        val finalSizeBytes = if (sizeBytes > 0) {
            val scaleRatio = scaleFactor.toDouble() * scaleFactor.toDouble()
            val inputScaledEst = (sizeBytes * (scaleRatio * 0.4)).toLong()
            (calculatedSizeBytes * 0.6 + inputScaledEst * 0.4).toLong()
        } else {
            calculatedSizeBytes
        }

        val sizeMb = finalSizeBytes / (1024.0 * 1024.0)
        val sizeFormatted = if (sizeMb >= 1000.0) {
            "~%.1f GB".format(sizeMb / 1024.0)
        } else {
            "~%.0f MB".format(sizeMb.coerceAtLeast(1.0))
        }

        return Pair(timeFormatted, sizeFormatted)
    }
}
