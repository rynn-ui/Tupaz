package com.tupaz.pipeline

data class DecodeResult(
    val decodedFrames: Int,
    val uniqueFrames: Int,
    val duplicateFrames: Int,
    val duplicatesSkipped: Int,
    val fallbackReason: String?,
    val confidenceScore: Int,
    val confidenceStatus: String
)
