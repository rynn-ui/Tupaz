package com.tupaz.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

class RetrieverFrameDecoder : FrameDecoder {
    companion object {
        private const val TAG = "RetrieverFrameDecoder"
    }

    override suspend fun decode(
        context: Context,
        inputUri: Uri,
        expectedFrameCount: Int,
        durationMs: Long,
        fps: Int,
        tracker: DeviceBenchmarkTracker?,
        onFrame: suspend (RawFrame) -> Unit
    ): DecodeResult {
        val retriever = MediaMetadataRetriever()
        var decodedCount = 0
        val startDecodeNano = System.nanoTime()
        var uniqueCount = 0
        var duplicateCount = 0
        var duplicatesSkipped = 0
        
        try {
            retriever.setDataSource(context, inputUri)
            Log.i(TAG, "[Tupaz-AI] Checkpoint 3/14: Retriever fallback decoder initialized ($expectedFrameCount expected frames)")

            val frameIntervalUs = 1_000_000L / fps.coerceAtLeast(1)
            var prevHash: Long? = null
            
            for (i in 0 until expectedFrameCount) {
                val timeUs = i * frameIntervalUs
                var bitmap: Bitmap? = null
                try {
                    // OPTION_CLOSEST is the only flag that reconstructs non-sync frames (B/P frames).
                    // However, some buggy device decoders silently fallback to OPTION_CLOSEST_SYNC 
                    // or just return the previous frame. Thus, we validate uniqueness via dHash.
                    bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    if (bitmap == null) {
                        Log.w(TAG, "[Tupaz-AI] getFrameAtTime returned null at ${timeUs}us")
                        continue
                    }
                    
                    if (decodedCount == 0) {
                        Log.i(TAG, "[Tupaz-AI] Checkpoint 4/14: Frame #1 decoded successfully via Retriever (${bitmap.width}x${bitmap.height})")
                    }
                    
                    val currentHash = calculateDHash(bitmap)
                    val isDuplicate = prevHash != null && isDuplicateHash(prevHash!!, currentHash)
                    
                    if (isDuplicate) {
                        Log.w(TAG, "[Tupaz-AI] Duplicate frame detected at index $i (${timeUs}us)")
                        duplicateCount++
                        // We do not skip it here so the video timing doesn't break,
                        // but we record it for metrics.
                    } else {
                        uniqueCount++
                    }
                    prevHash = currentHash
                    val rawData = bitmapToRgba(bitmap)
                    
                    onFrame(
                        RawFrame(
                            frameIndex = decodedCount.toLong(),
                            presentationTimeUs = timeUs,
                            width = bitmap.width,
                            height = bitmap.height,
                            data = rawData,
                            isEndOfStream = false
                        )
                    )
                    decodedCount++
                    tracker?.decodedFramesCount?.set(decodedCount)
                } finally {
                    if (bitmap != null && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-AI] Retriever fallback decode error", e)
        } finally {
            tracker?.addDecodeTime(System.nanoTime() - startDecodeNano)
            try { retriever.release() } catch (_: Exception) {}
        }
        
        Log.i(TAG, "[Tupaz-AI] Retriever fallback stats: $expectedFrameCount requested, $decodedCount returned, $uniqueCount unique.")
        
        val ratio = if (expectedFrameCount > 0) uniqueCount.toFloat() / expectedFrameCount else 1f
        val confidence = (ratio * 100).toInt().coerceIn(0, 100)
        val status = if (confidence >= 90) "Healthy" else "Possible duplicate frames"
        
        return DecodeResult(
            decodedFrames = decodedCount,
            uniqueFrames = uniqueCount,
            duplicateFrames = duplicateCount,
            duplicatesSkipped = duplicatesSkipped,
            fallbackReason = null, // Set by factory
            confidenceScore = confidence,
            confidenceStatus = status
        )
    }

    private fun calculateDHash(bitmap: Bitmap): Long {
        var scaled: Bitmap? = null
        try {
            scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
            val pixels = IntArray(9 * 8)
            scaled.getPixels(pixels, 0, 9, 0, 0, 9, 8)
            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val idx1 = y * 9 + x
                    val idx2 = idx1 + 1
                    
                    val p1 = pixels[idx1]
                    val p2 = pixels[idx2]
                    
                    // Simple grayscale: just take green channel for speed since it dominates luminance
                    val g1 = (p1 shr 8) and 0xFF
                    val g2 = (p2 shr 8) and 0xFF
                    
                    if (g1 > g2) {
                        hash = hash or (1L shl (y * 8 + x))
                    }
                }
            }
            return hash
        } finally {
            if (scaled != null && !scaled.isRecycled && scaled != bitmap) {
                scaled.recycle()
            }
        }
    }

    private fun isDuplicateHash(hash1: Long, hash2: Long): Boolean {
        val xor = hash1 xor hash2
        var hammingDistance = 0
        var temp = xor
        while (temp != 0L) {
            hammingDistance++
            temp = temp and (temp - 1)
        }
        return hammingDistance <= 3 // Threshold of 3 bits difference is perceptually identical
    }

    private fun bitmapToRgba(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = ByteArray(width * height * 4)
        var out = 0
        for (pixel in pixels) {
            output[out++] = ((pixel shr 16) and 0xFF).toByte()
            output[out++] = ((pixel shr 8) and 0xFF).toByte()
            output[out++] = (pixel and 0xFF).toByte()
            output[out++] = ((pixel shr 24) and 0xFF).toByte()
        }
        return output
    }
}
