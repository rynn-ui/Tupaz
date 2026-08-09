package com.tupaz.domain.pipeline

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stage types available within the processing pipeline.
 */
@Serializable
enum class PipelineStageType {
    @SerialName("denoise") DENOISE,
    @SerialName("upscale") UPSCALE,
    @SerialName("sharpen") SHARPEN,
    @SerialName("interpolate") INTERPOLATE,
    @SerialName("face_restore") FACE_RESTORE
}

/**
 * Individual stage definition within a pipeline mode configuration.
 */
@Serializable
data class PipelineStageDef(
    @SerialName("stage_id") val stageId: String,
    @SerialName("type") val type: PipelineStageType,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("parameters") val parameters: Map<String, String> = emptyMap()
)

/**
 * Configuration for a single processing mode.
 */
@Serializable
data class ModeConfigDef(
    @SerialName("mode_id") val modeId: String,
    @SerialName("scale_factor") val scaleFactor: Int,
    @SerialName("stages") val stages: List<PipelineStageDef>
)

/**
 * Root pipeline manifest containing version and mode definitions.
 */
@Serializable
data class PipelineManifest(
    @SerialName("version") val version: Int,
    @SerialName("modes") val modes: List<ModeConfigDef>
) {
    /**
     * Resolves mode configuration for a specified ProcessingMode.
     */
    fun findModeConfig(mode: ProcessingMode): ModeConfigDef? {
        return modes.firstOrNull { it.modeId.equals(mode.id, ignoreCase = true) }
    }
}
