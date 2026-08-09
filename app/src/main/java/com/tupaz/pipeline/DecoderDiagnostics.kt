package com.tupaz.pipeline

import org.json.JSONObject

data class DecoderDiagnostics(
    val decoder: String,
    val expectedFrames: Int,
    val extractedSamples: Int,
    val decodedFrames: Int,
    val uniqueFrames: Int,
    val duplicateFrames: Int,
    val duplicatesSkipped: Int,
    val fallbackReason: String?,
    val confidenceScore: Int,
    val confidenceStatus: String,
    val processingTimeMs: Long,
    val device: String,
    val android: String
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("decoder", decoder)
        json.put("expectedFrames", expectedFrames)
        json.put("extractedSamples", extractedSamples)
        json.put("decodedFrames", decodedFrames)
        json.put("uniqueFrames", uniqueFrames)
        json.put("duplicateFrames", duplicateFrames)
        json.put("duplicatesSkipped", duplicatesSkipped)
        if (fallbackReason != null) {
            json.put("fallbackReason", fallbackReason)
        }
        json.put("confidenceScore", confidenceScore)
        json.put("confidenceStatus", confidenceStatus)
        json.put("processingTimeMs", processingTimeMs)
        json.put("device", device)
        json.put("android", android)
        return json.toString(2)
    }
}
