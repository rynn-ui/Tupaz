package com.tupaz.pipeline

import android.content.Context
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.net.Uri
import android.util.Log

class MediaCodecFrameDecoder : FrameDecoder {
    companion object {
        private const val TAG = "MediaCodecFrameDecoder"
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
        var extractor: android.media.MediaExtractor? = null
        var decoder: MediaCodec? = null
        var decodedCount = 0
        val startDecodeNano = System.nanoTime()

        try {
            extractor = android.media.MediaExtractor().apply { setDataSource(context, inputUri, null) }
            val videoTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: throw IllegalArgumentException("Input has no video track")
            extractor.selectTrack(videoTrack)
            val inputFormat = extractor.getTrackFormat(videoTrack)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("Video track has no MIME type")
                
            inputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)

            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            Log.i(TAG, "[Tupaz-AI] Checkpoint 3/14: MediaCodec decoder initialized ($expectedFrameCount expected frames)")

            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var inputSampleCount = 0
            
            while (!outputEos) {
                if (!inputEos) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("Decoder input buffer was null")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            Log.d(TAG, "[Tupaz-AI] Extractor reached EOS (sampleSize=$sampleSize)")
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEos = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            val sampleFlags = extractor.sampleFlags
                            Log.d(TAG, "[Tupaz-AI] Extractor read sample: index=$inputSampleCount, sampleTime=$sampleTime, sampleFlags=$sampleFlags, sampleSize=$sampleSize")
                            
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                sampleTime,
                                sampleFlags
                            )
                            inputSampleCount++
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "[Tupaz-AI] Decoder format changed: ${decoder.outputFormat}")
                    }
                    else -> if (outputIndex >= 0) {
                        val isEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        val isFrame = bufferInfo.size > 0 && !isConfig
                        
                        Log.d(TAG, "[Tupaz-AI] Decoder output: outputIndex=$outputIndex, ptsUs=${bufferInfo.presentationTimeUs}, flags=${bufferInfo.flags}, size=${bufferInfo.size}, isFrame=$isFrame, isEos=$isEos")
                        
                        if (isFrame) {
                            val image = decoder.getOutputImage(outputIndex)
                                ?: throw IllegalStateException("Decoder released a frame without an image")
                            
                            val frameWidth = image.width
                            val frameHeight = image.height
                            
                            if (decodedCount == 0) {
                                Log.i(TAG, "[Tupaz-AI] Checkpoint 4/14: Frame #1 decoded successfully via MediaCodec (${frameWidth}x${frameHeight})")
                            }
                            
                            val startYuvNano = System.nanoTime()
                            val rawData = try {
                                yuv420ImageToRgba(image)
                            } finally {
                                image.close()
                            }
                            tracker?.addYuvToRgbaTime(System.nanoTime() - startYuvNano)
                            
                            decoder.releaseOutputBuffer(outputIndex, false)
                            
                            onFrame(
                                RawFrame(
                                    frameIndex = decodedCount.toLong(),
                                    presentationTimeUs = bufferInfo.presentationTimeUs,
                                    width = frameWidth,
                                    height = frameHeight,
                                    data = rawData,
                                    isEndOfStream = false
                                )
                            )
                            decodedCount++
                            tracker?.decodedFramesCount?.set(decodedCount)
                        } else {
                            decoder.releaseOutputBuffer(outputIndex, false)
                        }
                        
                        if (isEos) {
                            Log.d(TAG, "[Tupaz-AI] Decoder reached EOS")
                            outputEos = true
                        }
                    }
                }
            }
            Log.i(TAG, "[Tupaz-AI] Extraction stats: $inputSampleCount samples in, $decodedCount frames out")
            val ratio = if (expectedFrameCount > 0) decodedCount.toFloat() / expectedFrameCount else 1f
            val confidence = (ratio * 100).toInt().coerceIn(0, 100)
            val status = if (confidence >= 95) "Healthy" else "Missing frames detected"
            
            return DecodeResult(
                decodedFrames = decodedCount,
                uniqueFrames = decodedCount, // MediaCodec rarely duplicates unless specifically instructed
                duplicateFrames = 0,
                duplicatesSkipped = 0,
                fallbackReason = null,
                confidenceScore = confidence,
                confidenceStatus = status
            )
        } catch (e: Throwable) {
            Log.e(TAG, "[Tupaz-AI] Frame decoding error", e)
            throw e
        } finally {
            tracker?.addDecodeTime(System.nanoTime() - startDecodeNano)
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    private fun yuv420ImageToRgba(image: Image): ByteArray {
        require(image.format == android.graphics.ImageFormat.YUV_420_888) {
            "Unsupported image format: ${image.format}"
        }

        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val yRow = ByteArray(yRowStride)
        val uRow = ByteArray(uvRowStride)
        val vRow = ByteArray(uvRowStride)

        val rgbaBytes = ByteArray(width * height * 4)
        var outOffset = 0
        var lastUvRow = -1

        for (row in 0 until height) {
            val yRowOffset = row * yRowStride
            val yLen = minOf(yRowStride, yBuffer.capacity() - yRowOffset)
            yBuffer.position(yRowOffset)
            yBuffer.get(yRow, 0, yLen)

            val uvRow = row / 2
            if (uvRow != lastUvRow) {
                val uvRowOffset = uvRow * uvRowStride
                val uLen = minOf(uvRowStride, uBuffer.capacity() - uvRowOffset)
                val vLen = minOf(uvRowStride, vBuffer.capacity() - uvRowOffset)
                uBuffer.position(uvRowOffset)
                uBuffer.get(uRow, 0, uLen)
                vBuffer.position(uvRowOffset)
                vBuffer.get(vRow, 0, vLen)
                lastUvRow = uvRow
            }

            for (col in 0 until width) {
                val yIndex = col * yPixelStride
                val uvIndex = (col / 2) * uvPixelStride

                val y = (yRow[yIndex].toInt() and 0xFF) - 16
                val u = (uRow[uvIndex].toInt() and 0xFF) - 128
                val v = (vRow[uvIndex].toInt() and 0xFF) - 128

                val y1192 = 1192 * y
                var r = (y1192 + 1634 * v)
                var g = (y1192 - 833 * v - 400 * u)
                var b = (y1192 + 2066 * u)

                r = if (r < 0) 0 else if (r > 262143) 262143 else r
                g = if (g < 0) 0 else if (g > 262143) 262143 else g
                b = if (b < 0) 0 else if (b > 262143) 262143 else b

                rgbaBytes[outOffset++] = ((r shr 10) and 0xFF).toByte()
                rgbaBytes[outOffset++] = ((g shr 10) and 0xFF).toByte()
                rgbaBytes[outOffset++] = ((b shr 10) and 0xFF).toByte()
                rgbaBytes[outOffset++] = 255.toByte() // Alpha
            }
        }
        return rgbaBytes
    }
}
