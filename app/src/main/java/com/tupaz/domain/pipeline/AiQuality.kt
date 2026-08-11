package com.tupaz.domain.pipeline

enum class AiQuality(
    val modelId: String,
    val displayName: String,
    val modelDisplayName: String,
    val description: String,
    val paramFileName: String,
    val binFileName: String
) {
    LOW(
        modelId = "animejanai-hd-v3-superultracompact-x2",
        displayName = "LOW",
        modelDisplayName = "AnimeJaNai HD V3 SuperUltraCompact 2x",
        description = "AnimeJaNai HD V3 SuperUltraCompact 2x",
        paramFileName = "animejanai-hd-v3-superultracompact-x2.param",
        binFileName = "animejanai-hd-v3-superultracompact-x2.bin"
    ),
    MEDIUM(
        modelId = "animejanai-hd-v3-ultracompact-x2",
        displayName = "MEDIUM",
        modelDisplayName = "AnimeJaNai HD V3 UltraCompact 2x",
        description = "AnimeJaNai HD V3 UltraCompact 2x",
        paramFileName = "animejanai-hd-v3-ultracompact-x2.param",
        binFileName = "animejanai-hd-v3-ultracompact-x2.bin"
    ),
    HIGH(
        modelId = "realesr-animevideov3-x2",
        displayName = "HIGH",
        modelDisplayName = "RealESRGAN AnimeVideo v3 2x",
        description = "RealESRGAN AnimeVideo v3 2x",
        paramFileName = "realesr-animevideov3-x2.param",
        binFileName = "realesr-animevideov3-x2.bin"
    );

    companion object {
        fun fromModelId(modelId: String): AiQuality =
            entries.firstOrNull { it.modelId.equals(modelId, ignoreCase = true) } ?: HIGH

        fun fromDisplayName(name: String): AiQuality =
            entries.firstOrNull { 
                it.displayName.equals(name, ignoreCase = true) || 
                it.modelDisplayName.equals(name, ignoreCase = true) ||
                it.name.equals(name, ignoreCase = true)
            } ?: HIGH
    }
}
