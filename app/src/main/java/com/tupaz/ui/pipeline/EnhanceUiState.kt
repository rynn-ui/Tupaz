package com.tupaz.ui.pipeline

import android.net.Uri

import com.tupaz.domain.pipeline.AiQuality

enum class AiModeSelection {
    AUTO,
    MANUAL
}

data class ModelOption(
    val id: String,
    val name: String,
    val description: String,
    val isInstalled: Boolean
)

data class AutoSettingValues(
    val denoise: Int = 42,
    val recoverDetail: Int = 30,
    val sharpen: Int = 50,
    val reduceNoise: Int = 35,
    val dehalo: Int = 20
)

data class EnhanceUiState(
    val projectName: String = "New Project",
    val projectId: String? = null,
    val fileName: String = "Select Video File",
    val resolutionLabel: String = "1080p (1920x1080)",
    val videoWidth: Int = 1920,
    val videoHeight: Int = 1080,
    val videoFps: Int = 30,
    val durationMs: Long = 0L,
    val fpsLabel: String = "30 FPS",
    val durationLabel: String = "00:01:24",
    val fileSizeLabel: String = "-- MB",
    val videoSizeBytes: Long = 0L,
    val videoUri: Uri? = null,
    val deviceTierLabel: String = "HIGH Tier (Snapdragon 870 / Adreno 650)",
    val devicePowerName: String = "HIGH",
    val autoSettingValues: AutoSettingValues = AutoSettingValues(),
    val selectedQuality: AiQuality = AiQuality.HIGH,
    val selectedModel: String = AiQuality.HIGH.modelDisplayName,
    val selectedModelDescription: String = AiQuality.HIGH.description,
    val availableModels: List<ModelOption> = listOf(
        ModelOption(AiQuality.LOW.modelId, AiQuality.LOW.modelDisplayName, AiQuality.LOW.description, isInstalled = false),
        ModelOption(AiQuality.MEDIUM.modelId, AiQuality.MEDIUM.modelDisplayName, AiQuality.MEDIUM.description, isInstalled = false),
        ModelOption(AiQuality.HIGH.modelId, AiQuality.HIGH.modelDisplayName, AiQuality.HIGH.description, isInstalled = false)
    ),
    val selectedScaleFactor: String = "2x",
    val availableScaleFactors: List<String> = listOf("2x"),
    val videoType: String = "Progressive",
    val availableVideoTypes: List<String> = listOf("Progressive", "Interlaced", "60FPS High Frame"),
    val selectedAiMode: AiModeSelection = AiModeSelection.AUTO,
    val denoiseValue: Float = 28f,
    val recoverDetailValue: Float = 18f,
    val sharpenValue: Float = 35f,
    val reduceNoiseValue: Float = 20f,
    val dehaloValue: Float = 10f,
    val estimatedOutputTime: String = "Calculating estimate…",
    val estimatedOutputSize: String = "Calculating estimate…"
)
