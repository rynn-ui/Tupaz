package com.tupaz.config

/**
 * Feature flags controlling visibility of experimental or multi-model features.
 * Keeps underlying architectural foundations intact while presenting a streamlined
 * single-model production interface for RealESRGAN AnimeVideo v3 (2x).
 */
object FeatureFlags {
    /**
     * Set to true to enable multi-model dropdown selection in the UI.
     */
    const val ENABLE_MULTI_MODEL_SELECTION: Boolean = false

    /**
     * Set to true to enable Model Store / Manager screen navigation.
     */
    const val ENABLE_MODEL_STORE: Boolean = false

    /**
     * Set to true to enable Google Colab cloud processing.
     */
    const val ENABLE_CLOUD_PROCESSING: Boolean = false

    /**
     * Default model ID used for all video enhancement operations.
     */
    const val DEFAULT_MODEL_ID: String = "realesr-animevideov3-x2"
}
