package com.tupaz.pipeline

data class DecoderPolicy(
    val hardwareThreshold: Float = 0.95f,
    val warningThreshold: Float = 0.80f
)
