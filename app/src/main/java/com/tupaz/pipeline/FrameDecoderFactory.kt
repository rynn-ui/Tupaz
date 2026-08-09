package com.tupaz.pipeline

import android.util.Log

data class DecoderSelection(
    val decoder: FrameDecoder,
    val decoderName: String,
    val isFallback: Boolean,
    val fallbackReason: String?
)

object FrameDecoderFactory {
    private const val TAG = "FrameDecoderFactory"

    fun selectDecoder(
        expectedFrames: Int,
        extractedSamples: Int,
        fps: Int,
        durationSec: Double,
        policy: DecoderPolicy
    ): DecoderSelection {
        val ratio = if (expectedFrames > 0) extractedSamples.toFloat() / expectedFrames else 1f

        return if (ratio >= policy.hardwareThreshold) {
            Log.i(TAG, """
                [Tupaz-AI] Video Decoder Diagnostics:
                Duration: $durationSec s
                FPS: $fps
                Expected Frames: $expectedFrames
                Extractor Samples: $extractedSamples
                Decision: Hardware Enabled
            """.trimIndent())
            DecoderSelection(
                decoder = MediaCodecFrameDecoder(),
                decoderName = "MediaCodec",
                isFallback = false,
                fallbackReason = null
            )
        } else if (ratio >= policy.warningThreshold) {
            Log.w(TAG, """
                [Tupaz-AI] Video Decoder Diagnostics:
                Duration: $durationSec s
                FPS: $fps
                Expected Frames: $expectedFrames
                Extractor Samples: $extractedSamples
                Decision: Hardware Enabled (Warning: Some samples missing)
            """.trimIndent())
            DecoderSelection(
                decoder = MediaCodecFrameDecoder(),
                decoderName = "MediaCodec",
                isFallback = false,
                fallbackReason = null
            )
        } else {
            val fallbackReason = "Extractor recovered only ${(ratio * 100).toInt()}% of expected frames"
            Log.e(TAG, """
                [Tupaz-AI] Video Decoder Diagnostics:
                Duration: $durationSec s
                FPS: $fps
                Expected Frames: $expectedFrames
                Extractor Samples: $extractedSamples
                Decision: Fallback Enabled
                Reason: $fallbackReason
            """.trimIndent())
            DecoderSelection(
                decoder = RetrieverFrameDecoder(),
                decoderName = "MediaMetadataRetriever",
                isFallback = true,
                fallbackReason = fallbackReason
            )
        }
    }
}
