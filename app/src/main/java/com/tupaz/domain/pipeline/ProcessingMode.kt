package com.tupaz.domain.pipeline

/**
 * Represents execution modes for the image/video enhancement pipeline.
 * Pure Kotlin — zero Android framework dependencies.
 */
enum class ProcessingMode(
    val id: String,
    val displayName: String,
    val description: String,
    val scaleFactor: Int,
    val requiresVulkan: Boolean,
    val defaultTileSize: Int,
    val fpsMultiplier: Float
) {
    FAST(
        id = "fast",
        displayName = "Fast",
        description = "Lightweight enhancement using fast bilateral filtering and sharpening.",
        scaleFactor = 1,
        requiresVulkan = false,
        defaultTileSize = 0, // 0 = Full frame processing without tiling
        fpsMultiplier = 1.0f
    ),
    BALANCED(
        id = "balanced",
        displayName = "Balanced",
        description = "Standard 2x upscale with ESRGAN-Lite and sharpening.",
        scaleFactor = 2,
        requiresVulkan = true,
        defaultTileSize = 384,
        fpsMultiplier = 1.0f
    ),
    ULTRA(
        id = "ultra",
        displayName = "Ultra",
        description = "High quality 4x upscale with NAFNet denoiser and Vulkan sharpening.",
        scaleFactor = 4,
        requiresVulkan = true,
        defaultTileSize = 256,
        fpsMultiplier = 1.0f
    ),
    ANIME(
        id = "anime",
        displayName = "Anime / Cartoon",
        description = "Optimized 4x upscale specialized for 2D animation line art and flat fills.",
        scaleFactor = 4,
        requiresVulkan = true,
        defaultTileSize = 512,
        fpsMultiplier = 1.0f
    ),
    AUTO(
        id = "auto",
        displayName = "Auto Mode",
        description = "Automatically analyzes content and selects optimal pipeline configuration.",
        scaleFactor = 2,
        requiresVulkan = true,
        defaultTileSize = 384,
        fpsMultiplier = 1.0f
    );

    /**
     * Calculates the output dimensions given input width and height based on mode scale factor.
     */
    fun calculateOutputDimensions(inputWidth: Int, inputHeight: Int): Pair<Int, Int> {
        require(inputWidth > 0) { "Input width must be positive" }
        require(inputHeight > 0) { "Input height must be positive" }
        return Pair(inputWidth * scaleFactor, inputHeight * scaleFactor)
    }

    companion object {
        /**
         * Resolves enum by string ID or returns default BALANCED mode if unrecognized.
         */
        fun fromId(id: String): ProcessingMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BALANCED
        }
    }
}
