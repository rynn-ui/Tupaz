package com.tupaz.auto

/**
 * Structural signals extracted from initial video frames during AUTO mode analysis.
 */
data class ContentSignals(
    val isAnime: Boolean,
    val compressionScore: Float,
    val faceDensityRatio: Float,
    val averageMotionDelta: Float,
    val sampleFrameWidth: Int,
    val sampleFrameHeight: Int
)
