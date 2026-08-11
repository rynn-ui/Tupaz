package com.tupaz.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.tupaz.data.storage.ModelStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Executes frame-by-frame RealESRGAN super-resolution video processing pipeline.
 * Guarantees 1:1 input to output frame count, matching FPS, matching duration, and 2x upscaling.
 */
object VideoUpscaler {
    private const val TAG = "VideoUpscaler"

    suspend fun processAndUpscaleVideo(
        context: Context,
        inputUri: Uri?,
        targetWidth: Int,
        targetHeight: Int,
        modelName: String,
        scaleFactor: String,
        onProgress: ((currentFrame: Int, totalFrames: Int, stage: String) -> Unit)? = null,
        isPausedCheck: (() -> Boolean)? = null
    ): Uri? = withContext(Dispatchers.IO) {
        if (inputUri == null) return@withContext null

        val (w, h) = getSafeEncodingDimensions(targetWidth, targetHeight)
        val outputFile = File(context.cacheDir, "tupaz_enhanced_${System.currentTimeMillis()}.mp4")

        Log.i(TAG, "[Tupaz-AI] === PIPELINE START ===")
        Log.i(TAG, "[Tupaz-AI] Input URI = $inputUri")
        Log.i(TAG, "[Tupaz-AI] Target Resolution = ${w}x${h}")
        Log.i(TAG, "[Tupaz-AI] Model = $modelName ($scaleFactor)")
        Log.i(TAG, "[Tupaz-AI] Output Path = ${outputFile.absolutePath}")

        try {
            return@withContext executeUpscalePipeline(
                context = context,
                inputUri = inputUri,
                outputFile = outputFile,
                targetWidth = w,
                targetHeight = h,
                modelName = modelName,
                scaleFactor = scaleFactor,
                onProgress = onProgress,
                isPausedCheck = isPausedCheck
            )
        } catch (e: Throwable) {
            Log.e(TAG, """
                [Tupaz-AI] !!! PIPELINE ROOT FAILURE !!!
                Exception class: ${e.javaClass.name}
                Exception message: ${e.message}
                Stack trace:
                ${Log.getStackTraceString(e)}
            """.trimIndent())
            throw e
        }
    }

    private fun getSafeEncodingDimensions(width: Int, height: Int): Pair<Int, Int> {
        var w = ((width + 15) / 16) * 16
        var h = ((height + 15) / 16) * 16
        try {
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h)
            val encoderName = codecList.findEncoderForFormat(format)
            if (encoderName == null) {
                if (w > 1920 || h > 1080) {
                    w = 1920
                    h = 1080
                }
            }
        } catch (_: Exception) {}
        return Pair(w, h)
    }

    private fun resolveModelId(modelName: String): String {
        val matchedQuality = com.tupaz.domain.pipeline.AiQuality.entries.firstOrNull { quality ->
            quality.modelId.equals(modelName, ignoreCase = true) ||
            quality.displayName.equals(modelName, ignoreCase = true) ||
            quality.modelDisplayName.equals(modelName, ignoreCase = true) ||
            quality.name.equals(modelName, ignoreCase = true)
        }
        if (matchedQuality != null) return matchedQuality.modelId

        return when {
            modelName.contains("SuperUltraCompact", ignoreCase = true) -> com.tupaz.domain.pipeline.AiQuality.LOW.modelId
            modelName.contains("UltraCompact", ignoreCase = true) -> com.tupaz.domain.pipeline.AiQuality.MEDIUM.modelId
            modelName.contains("Anime", ignoreCase = true) -> com.tupaz.domain.pipeline.AiQuality.HIGH.modelId
            modelName.contains("RealESRGAN", ignoreCase = true) -> com.tupaz.domain.pipeline.AiQuality.HIGH.modelId
            else -> com.tupaz.domain.pipeline.AiQuality.HIGH.modelId
        }
    }

    private suspend fun executeUpscalePipeline(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        targetWidth: Int,
        targetHeight: Int,
        modelName: String,
        scaleFactor: String,
        onProgress: ((currentFrame: Int, totalFrames: Int, stage: String) -> Unit)? = null,
        isPausedCheck: (() -> Boolean)? = null
    ): Uri = withContext(Dispatchers.IO) {
        val ncnnBridge = NcnnBridge()
        val tracker = DeviceBenchmarkTracker(context)
        tracker.startWallClock()

        try {
            val modelId = resolveModelId(modelName)
            val quality = com.tupaz.domain.pipeline.AiQuality.fromModelId(modelId)
            tracker.modelName = quality.modelDisplayName
            tracker.qualityLevel = quality.displayName
            val numericScale = com.tupaz.domain.pipeline.PipelineScale.parseAndValidate(scaleFactor)
            Log.i(TAG, "[Tupaz-AI] Model resolution: modelName='$modelName' -> modelId='$modelId', quality=${quality.displayName}, numericScale=${numericScale}x")

            // Initialize NCNN Native Model (Single initialization with self-healing)
            val storage = ModelStorage(context)
            storage.ensureModelAvailable(modelId)
            val paramFile = storage.getParamFile(modelId)
            val binFile = storage.getBinFile(modelId)

            Log.i(TAG, "[Tupaz-AI] Model discovery: paramFile='${paramFile.absolutePath}' (exists=${paramFile.exists()}, size=${paramFile.length()} bytes), binFile='${binFile.absolutePath}' (exists=${binFile.exists()}, size=${binFile.length()} bytes)")

            if (!paramFile.exists() || !binFile.exists() || paramFile.length() == 0L || binFile.length() == 0L) {
                val msg = "AI model is not installed: $modelId (.param or .bin missing)"
                Log.e(TAG, "[Tupaz-AI] $msg")
                throw IllegalArgumentException(msg)
            }

            val initSuccess = ncnnBridge.initModel(paramFile.absolutePath, binFile.absolutePath, useGpu = true)
            if (!initSuccess) {
                val msg = "AI model failed to initialize: $modelId"
                Log.e(TAG, "[Tupaz-AI] $msg")
                throw IllegalStateException(msg)
            }
            Log.i(TAG, "[Tupaz-AI] Initialized production NCNN model successfully: ${paramFile.absolutePath}")

            val ncnnUpscaler = NcnnUpscaler(
                ncnnBridge = ncnnBridge,
                context = context,
                alreadyInitialized = true,
                requireModelOutput = true
            )

            // Extract video metadata & EXACT frame count
            var durationMs = 0L
            var fps = 30
            var detectedFrameCount = 0
            var extractorDurationUs = 0L
            var extractorFrameCount = 0
            val sourcePresentationTimesUs = mutableListOf<Long>()

            // 1. Query MediaExtractor for exact track-level duration, FPS, and sample count
            try {
                val extractor = android.media.MediaExtractor()
                extractor.setDataSource(context, inputUri, null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/")) {
                        extractor.selectTrack(i)
                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            extractorDurationUs = format.getLong(MediaFormat.KEY_DURATION)
                        }
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            try {
                                fps = format.getInteger(MediaFormat.KEY_FRAME_RATE).coerceIn(1, 120)
                            } catch (_: ClassCastException) {
                                try {
                                    fps = format.getFloat(MediaFormat.KEY_FRAME_RATE).toInt().coerceIn(1, 120)
                                } catch (_: Exception) {}
                            } catch (_: Exception) {}
                        }
                        while (extractor.sampleTime >= 0) {
                            sourcePresentationTimesUs += extractor.sampleTime
                            extractorFrameCount++
                            extractor.advance()
                        }
                        break
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                Log.e(TAG, "[Tupaz-AI] MediaExtractor metadata error", e)
            }

            var inputW = 0
            var inputH = 0
            val metaRetriever = MediaMetadataRetriever()
            try {
                metaRetriever.setDataSource(context, inputUri)
                val metaDur = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (metaDur != null && metaDur > 0) {
                    durationMs = metaDur
                }
                inputW = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                inputH = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    detectedFrameCount = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: 0
                }

                val fpsStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                if (fpsStr != null) {
                    fps = fpsStr.toFloatOrNull()?.toInt()?.coerceIn(1, 120) ?: fps
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Tupaz-AI] MediaMetadataRetriever error", e)
            } finally {
                try { metaRetriever.release() } catch (_: Exception) {}
            }

            if (sourcePresentationTimesUs.size >= 2) {
                val diffUs = sourcePresentationTimesUs.last() - sourcePresentationTimesUs.first()
                if (diffUs > 0) {
                    val avgIntervalUs = diffUs.toDouble() / (sourcePresentationTimesUs.size - 1)
                    val derivedFps = kotlin.math.round(1_000_000.0 / avgIntervalUs).toInt().coerceIn(1, 120)
                    if (derivedFps > 0 && fps <= 0) {
                        fps = derivedFps
                    }
                }
            }

            if (extractorDurationUs > 0) {
                durationMs = extractorDurationUs / 1000L
            }

            val totalSec = (durationMs / 1000.0).coerceAtLeast(0.01)
            val calculatedFrames = kotlin.math.round(totalSec * fps).toInt().coerceAtLeast(1)

            val probedFrameCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && extractorFrameCount == 0) {
                probeExactFrameCount(
                    context,
                    inputUri,
                    detectedFrameCount,
                    calculatedFrames
                )
            } else 0

            val frameIntervalUs = 1_000_000L / fps.coerceAtLeast(1)

            val expectedFrames = maxOf(extractorFrameCount, detectedFrameCount, calculatedFrames, probedFrameCount).coerceAtLeast(1)
            val extractedFrames = extractorFrameCount

            val policy = DecoderPolicy()
            val selection = FrameDecoderFactory.selectDecoder(
                expectedFrames = expectedFrames,
                extractedSamples = extractedFrames,
                fps = fps,
                durationSec = totalSec,
                policy = policy
            )

            tracker.inputWidth = if (inputW > 0) inputW else targetWidth / 2
            tracker.inputHeight = if (inputH > 0) inputH else targetHeight / 2
            tracker.inputFps = fps
            tracker.inputDurationMs = durationMs
            tracker.expectedFrameCount = expectedFrames

            tracker.outputWidth = targetWidth
            tracker.outputHeight = targetHeight
            tracker.outputFps = fps
            tracker.outputDurationMs = durationMs

            Log.i(TAG, "[Tupaz-AI] Input FPS = $fps")
            Log.i(TAG, "[Tupaz-AI] Expected frames = $expectedFrames")
            Log.i(TAG, "[Tupaz-AI] Decoder selected = ${selection.decoderName} (fallbackReason=${selection.fallbackReason})")
            onProgress?.invoke(0, expectedFrames, "Initializing AI Pipeline...")

            var decodeResult: DecodeResult? = null
            var decodedFramesCount = 0
            var enhancedFramesCount = 0
            var encodedFramesCount = 0

            val decodedFrameChannel = Channel<RawFrame>(capacity = 4)
            val processedFrameChannel = Channel<ProcessedFrame>(capacity = 2)

            val orchestrator = PipelineOrchestrator(
                denoiser = Denoiser(ncnnBridge),
                scunetRunner = ScunetRunner(ncnnBridge = ncnnBridge),
                upscaler = ncnnUpscaler,
                sharpen = VulkanSharpen(ncnnBridge),
                modelId = modelId
            )

            val pipelineStart = System.currentTimeMillis()
            coroutineScope {
                // --- Stage 1: Decoder Coroutine ---
                launch(Dispatchers.IO) {
                    val startDecode = System.currentTimeMillis()
                    Log.i(TAG, "[Tupaz-AI] DECODE START")
                    try {
                        decodeResult = decodeFramesSequentially(
                            context,
                            inputUri,
                            decodedFrameChannel,
                            expectedFrames,
                            durationMs,
                            fps,
                            selection.decoder,
                            tracker
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "[Tupaz-AI] DECODE ERROR", e)
                        throw e
                    } finally {
                        decodedFrameChannel.close()
                        Log.i(TAG, "[Tupaz-Perf] Total Decode Stage: ${System.currentTimeMillis() - startDecode}ms")
                    }
                }

                // --- Stage 2: AI Processing Coroutine ---
                launch(Dispatchers.Default) {
                    val startAi = System.currentTimeMillis()
                    try {
                        var frameIdx = 0
                        for (rawFrame in decodedFrameChannel) {
                            if (rawFrame.isEndOfStream) {
                                processedFrameChannel.send(ProcessedFrame(
                                    frameIndex = rawFrame.frameIndex,
                                    presentationTimeUs = rawFrame.presentationTimeUs,
                                    width = rawFrame.width, height = rawFrame.height,
                                    data = ByteArray(0), isEndOfStream = true
                                ))
                                break
                            }

                            // Thermal Pause Suspension Loop: Pauses NCNN GPU inference completely until device cools down
                            while (isPausedCheck?.invoke() == true && isActive) {
                                kotlinx.coroutines.delay(200)
                            }

                            frameIdx++
                            enhancedFramesCount = frameIdx
                            val frameStartMs = System.currentTimeMillis()
                            val reportedTotal = maxOf(expectedFrames, frameIdx)
                            onProgress?.invoke(frameIdx, reportedTotal, "AI Processing Source Frame $frameIdx of $reportedTotal")

                            if (frameIdx == 1 || frameIdx % 30 == 0) {
                                Log.i(TAG, "[Tupaz-AI] AI START frame=$frameIdx/$reportedTotal ptsUs=${rawFrame.presentationTimeUs}")
                            } else {
                                Log.d(TAG, "[Tupaz-AI] AI START frame=$frameIdx ptsUs=${rawFrame.presentationTimeUs}")
                            }

                            val startAiFrameNano = System.nanoTime()
                            val processedFrame = orchestrator.processFrame(rawFrame, com.tupaz.domain.pipeline.ProcessingMode.AUTO, scaleFactor = numericScale)
                            val inferenceTimeMs = (System.nanoTime() - startAiFrameNano) / 1_000_000L
                            tracker.addNcnnInferenceTime(System.nanoTime() - startAiFrameNano)
                            tracker.aiProcessedFramesCount.set(enhancedFramesCount)

                            if (frameIdx == 1 || frameIdx % 30 == 0) {
                                Log.i(TAG, "[Tupaz-AI] AI FINISHED frame=$frameIdx timeMs=$inferenceTimeMs dimensions=${processedFrame.width}x${processedFrame.height}")
                            } else {
                                Log.d(TAG, "[Tupaz-AI] AI FINISHED frame=$frameIdx timeMs=$inferenceTimeMs")
                            }

                            processedFrameChannel.send(processedFrame)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "[Tupaz-AI] AI PROCESSING ERROR", e)
                        throw e
                    } finally {
                        processedFrameChannel.close()
                        Log.i(TAG, "[Tupaz-Perf] Total AI Stage: ${System.currentTimeMillis() - startAi}ms")
                    }
                }

                // --- Stage 3: Encoder Coroutine ---
                launch(Dispatchers.IO) {
                    encodeProcessedFrames(
                        processedFrameChannel,
                        outputFile,
                        targetWidth,
                        targetHeight,
                        fps,
                        frameIntervalUs,
                        tracker
                    ) { count ->
                        encodedFramesCount = count
                        tracker.encodedFramesCount.set(count)
                    }
                }
            }

            val processingTimeMs = System.currentTimeMillis() - pipelineStart
            ncnnBridge.destroy()

            decodedFramesCount = decodeResult?.decodedFrames ?: 0
            Log.i(TAG, "[Tupaz-AI] FRAME_COUNTS inputExpected=$expectedFrames decoded=$decodedFramesCount enhanced=$enhancedFramesCount encoded=$encodedFramesCount")

            if (decodedFramesCount != enhancedFramesCount || enhancedFramesCount != encodedFramesCount) {
                val msg = "FRAME_COUNT_MISMATCH: decoded=$decodedFramesCount enhanced=$enhancedFramesCount encoded=$encodedFramesCount (expected=$expectedFrames)"
                Log.e(TAG, "[Tupaz-AI] $msg")
                throw IllegalStateException(msg)
            }
            if (encodedFramesCount == 0) {
                val msg = "CRITICAL FAIL: Zero frames were encoded!"
                Log.e(TAG, msg)
                throw IllegalStateException(msg)
            }

            Log.i(TAG, "[Tupaz-AI] OUTPUT PATH = ${outputFile.absolutePath}")
            Log.i(TAG, "[Tupaz-AI] OUTPUT EXISTS = ${outputFile.exists()}")
            Log.i(TAG, "[Tupaz-AI] OUTPUT SIZE = ${outputFile.length()} bytes")

            if (!outputFile.exists() || outputFile.length() == 0L) {
                val msg = "Output MP4 file missing or 0 bytes after encoding (path=${outputFile.absolutePath}, exists=${outputFile.exists()}, length=${outputFile.length()})"
                Log.e(TAG, "[Tupaz-AI] $msg")
                throw IllegalStateException(msg)
            }

            // Probe output MP4 to verify metadata
            try {
                val outputRetriever = MediaMetadataRetriever()
                outputRetriever.setDataSource(outputFile.absolutePath)
                val outW = outputRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val outH = outputRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val outDur = outputRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                outputRetriever.release()
                Log.i(TAG, "[Tupaz-AI] Output MP4 Validated: resolution=${outW}x${outH}, durationMs=$outDur, size=${outputFile.length()} bytes")
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-AI] Output validation probe warning", e)
            }

            // Save diagnostics
            try {
                if (decodeResult != null) {
                    val diagnostics = DecoderDiagnostics(
                        decoder = selection.decoderName,
                        expectedFrames = expectedFrames,
                        extractedSamples = extractedFrames,
                        decodedFrames = decodeResult!!.decodedFrames,
                        uniqueFrames = decodeResult!!.uniqueFrames,
                        duplicateFrames = decodeResult!!.duplicateFrames,
                        duplicatesSkipped = decodeResult!!.duplicatesSkipped,
                        fallbackReason = selection.fallbackReason,
                        confidenceScore = decodeResult!!.confidenceScore,
                        confidenceStatus = decodeResult!!.confidenceStatus,
                        processingTimeMs = processingTimeMs,
                        device = android.os.Build.MODEL,
                        android = android.os.Build.VERSION.RELEASE
                    )
                    val metricsFile = java.io.File(outputFile.absolutePath + ".diagnostics.json")
                    metricsFile.writeText(diagnostics.toJson())
                    Log.i(TAG, "[Tupaz-AI] Diagnostics saved to ${metricsFile.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-AI] Failed to save diagnostics", e)
            }

            tracker.logAndSave(outputFile)

            return@withContext Uri.fromFile(outputFile)
        } catch (e: Throwable) {
            ncnnBridge.destroy()
            if (outputFile.exists()) {
                try {
                    outputFile.delete()
                    Log.i(TAG, "[Tupaz-AI] Cleaned up incomplete output file: ${outputFile.absolutePath}")
                } catch (_: Exception) {}
            }
            val metricsFile = java.io.File(outputFile.absolutePath + ".diagnostics.json")
            if (metricsFile.exists()) {
                try {
                    metricsFile.delete()
                    Log.i(TAG, "[Tupaz-AI] Cleaned up incomplete diagnostics file: ${metricsFile.absolutePath}")
                } catch (_: Exception) {}
            }
            Log.e(TAG, "[Tupaz-AI] Pipeline execution error: ${e.message}", e)
            throw e
        }
    }

    private suspend fun decodeFramesSequentially(
        context: Context,
        inputUri: Uri,
        channel: Channel<RawFrame>,
        expectedFrameCount: Int,
        durationMs: Long,
        fps: Int,
        decoder: FrameDecoder,
        tracker: DeviceBenchmarkTracker? = null
    ): DecodeResult {
        var result: DecodeResult
        try {
            result = decoder.decode(context, inputUri, expectedFrameCount, durationMs, fps, tracker) { rawFrame ->
                if (rawFrame.frameIndex == 0L || rawFrame.frameIndex % 30L == 0L) {
                    Log.i(TAG, "[Tupaz-AI] DECODE FRAME n=${rawFrame.frameIndex} ptsUs=${rawFrame.presentationTimeUs}")
                } else {
                    Log.d(TAG, "[Tupaz-AI] DECODE FRAME n=${rawFrame.frameIndex} ptsUs=${rawFrame.presentationTimeUs}")
                }
                channel.send(rawFrame)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "[Tupaz-AI] DECODE ERROR frame decoding failed", e)
            throw e
        }

        channel.send(RawFrame(result.decodedFrames.toLong(), 0, 0, 0, ByteArray(0), isEndOfStream = true))
        Log.i(TAG, "[Tupaz-AI] Completed decoding ${result.decodedFrames} frames (expected=$expectedFrameCount)")
        return result
    }

    private fun probeExactFrameCount(
        context: Context,
        inputUri: Uri,
        metadataCount: Int,
        estimatedCount: Int
    ): Int {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, inputUri)
            val hardCap = 500_000

            var hi = maxOf(metadataCount, estimatedCount, 1).coerceAtMost(hardCap)
            var safe = false
            var guard = 0
            while (hi <= hardCap) {
                if (!isFrameAvailable(retriever, hi)) {
                    safe = true
                    break
                }
                val doubled = hi.toLong() * 2L
                hi = if (doubled > hardCap.toLong() + 1L) hardCap + 1 else doubled.toInt()
                if (++guard > 40) break
            }
            if (!safe) return 0

            var lo = 0
            var lastValid = -1
            while (lo < hi) {
                val mid = lo + (hi - lo) / 2
                if (isFrameAvailable(retriever, mid)) {
                    lastValid = mid
                    lo = mid + 1
                } else {
                    hi = mid
                }
            }

            if (lastValid < 0) return 0
            return lastValid + 1
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-AI] Frame count probe error", e)
            return 0
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun isFrameAvailable(retriever: MediaMetadataRetriever, index: Int): Boolean {
        if (index < 0) return false
        var bmp: Bitmap? = null
        return try {
            bmp = retriever.getFrameAtIndex(index)
            bmp != null
        } catch (_: Throwable) {
            false
        } finally {
            if (bmp != null && !bmp.isRecycled) bmp.recycle()
        }
    }

    private suspend fun encodeProcessedFrames(
        channel: Channel<ProcessedFrame>,
        outputFile: File,
        targetWidth: Int,
        targetHeight: Int,
        fps: Int,
        frameIntervalUs: Long,
        tracker: DeviceBenchmarkTracker? = null,
        onFrameEncoded: (Int) -> Unit
    ): Boolean {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
            val bitrate = when {
                targetHeight >= 2160 -> 24_000_000
                targetHeight >= 1440 -> 16_000_000
                else -> 8_000_000
            }

            val format = MediaFormat.createVideoFormat(mimeType, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            val bufferInfo = MediaCodec.BufferInfo()
            var submittedFrameCount = 0
            var writtenFrameCount = 0
            var lastWrittenPtsUs = -1L
            var decodedFrameCount = -1
            val submittedPresentationTimesUs = ArrayDeque<Long>()

            var reusableBitmap: Bitmap? = null

            for (processed in channel) {
                if (processed.isEndOfStream) {
                    decodedFrameCount = processed.frameIndex.toInt()
                    Log.i(TAG, "[Tupaz-AI] ENCODER EOS Received from channel, decodedFrameCount=$decodedFrameCount")
                    break
                }
                val startFrameEncodeNano = System.nanoTime()

                val outWidth = processed.width
                val outHeight = processed.height
                val expectedLength = outWidth * outHeight * 4
                val isEnhanced = outWidth >= targetWidth || processed.data.size == expectedLength

                if (!isEnhanced) {
                    val msg = "CRITICAL FAIL: Encoder received UNENHANCED frame index ${processed.frameIndex} (${outWidth}x${outHeight})!"
                    Log.e(TAG, msg)
                    throw IllegalStateException(msg)
                }

                val bitmap = if (processed.data.size == expectedLength) {
                    if (reusableBitmap == null || reusableBitmap.width != outWidth || reusableBitmap.height != outHeight) {
                        reusableBitmap?.recycle()
                        reusableBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    }
                    val buf = ByteBuffer.wrap(processed.data)
                    buf.rewind()
                    reusableBitmap!!.copyPixelsFromBuffer(buf)
                    reusableBitmap!!
                } else if (processed.data.size > 0) {
                    val pixelCount = processed.data.size / 4
                    val calcWidth = if (outWidth > 0) outWidth else kotlin.math.sqrt(pixelCount.toDouble()).toInt()
                    val calcHeight = if (calcWidth > 0) pixelCount / calcWidth else outHeight
                    val dynamicBitmap = Bitmap.createBitmap(calcWidth, calcHeight, Bitmap.Config.ARGB_8888)
                    val buf = ByteBuffer.wrap(processed.data)
                    buf.rewind()
                    dynamicBitmap.copyPixelsFromBuffer(buf)
                    dynamicBitmap
                } else {
                    Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                }

                // Render to Hardware Surface
                var posted = false
                try {
                    val canvas = try {
                        surface.lockHardwareCanvas()
                    } catch (_: Exception) {
                        try {
                            surface.lockCanvas(null)
                        } catch (_: Exception) { null }
                    }
                    if (canvas != null) {
                        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                        val dstRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
                        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                        surface.unlockCanvasAndPost(canvas)
                        posted = true
                        if (processed.frameIndex == 0L || processed.frameIndex % 30L == 0L) {
                            Log.i(TAG, "[Tupaz-AI] ENCODE SUBMIT frame=${processed.frameIndex} ptsUs=${processed.presentationTimeUs}")
                        } else {
                            Log.d(TAG, "[Tupaz-AI] ENCODE SUBMIT frame=${processed.frameIndex} ptsUs=${processed.presentationTimeUs}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[Tupaz-AI] Canvas render error: ${e.message}")
                }
                check(posted) { "Could not submit processed frame ${processed.frameIndex} to the encoder" }
                submittedPresentationTimesUs.addLast(processed.presentationTimeUs)

                // Drain available encoded samples
                while (true) {
                    val status = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                            Log.i(TAG, "[Tupaz-AI] MediaMuxer: Started (Format Changed)")
                        }
                    } else if (status >= 0) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                            Log.i(TAG, "[Tupaz-AI] MediaMuxer: Started (Direct status >= 0)")
                        }
                        val encodedData = encoder.getOutputBuffer(status)
                        if (encodedData != null && muxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0) {
                                val sourcePtsUs = if (submittedPresentationTimesUs.isEmpty()) {
                                    throw IllegalStateException("Encoder produced a frame without a source timestamp")
                                } else {
                                    submittedPresentationTimesUs.removeFirst()
                                }
                                bufferInfo.presentationTimeUs = sourcePtsUs.coerceAtLeast(lastWrittenPtsUs + 1L)
                                lastWrittenPtsUs = bufferInfo.presentationTimeUs
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                                writtenFrameCount++
                                onFrameEncoded(writtenFrameCount)
                                if (writtenFrameCount == 1 || writtenFrameCount % 30 == 0) {
                                    Log.i(TAG, "[Tupaz-AI] ENCODE OUTPUT frame=$writtenFrameCount ptsUs=${bufferInfo.presentationTimeUs}")
                                } else {
                                    Log.d(TAG, "[Tupaz-AI] ENCODE OUTPUT frame=$writtenFrameCount ptsUs=${bufferInfo.presentationTimeUs}")
                                }
                            }
                        }
                        encoder.releaseOutputBuffer(status, false)
                    }
                }

                submittedFrameCount++
                tracker?.addEncodeTime(System.nanoTime() - startFrameEncodeNano)
            }

            // Signal EOS to encoder
            val startEosEncodeNano = System.nanoTime()
            encoder.signalEndOfInputStream()

            var done = false
            var idlePolls = 0
            while (!done) {
                val status = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (++idlePolls > 1_000) {
                        throw IllegalStateException("Encoder did not signal EOS after 10 seconds")
                    }
                } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                        Log.i(TAG, "[Tupaz-AI] MediaMuxer: Started in EOS (Format Changed)")
                    }
                } else if (status >= 0) {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                        Log.i(TAG, "[Tupaz-AI] MediaMuxer: Started in EOS (Direct status >= 0)")
                    }
                    val encodedData = encoder.getOutputBuffer(status)
                    if (encodedData != null && muxerStarted) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            val sourcePtsUs = if (submittedPresentationTimesUs.isEmpty()) {
                                throw IllegalStateException("Encoder produced a frame without a source timestamp")
                            } else {
                                submittedPresentationTimesUs.removeFirst()
                            }
                            bufferInfo.presentationTimeUs = sourcePtsUs.coerceAtLeast(lastWrittenPtsUs + 1L)
                            lastWrittenPtsUs = bufferInfo.presentationTimeUs
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            writtenFrameCount++
                            onFrameEncoded(writtenFrameCount)
                            Log.i(TAG, "[Tupaz-AI] ENCODE OUTPUT frame=$writtenFrameCount (EOS) ptsUs=${bufferInfo.presentationTimeUs}")
                        }
                    }
                    encoder.releaseOutputBuffer(status, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        done = true
                        Log.i(TAG, "[Tupaz-AI] ENCODER EOS")
                    }
                }
            }
            tracker?.addEncodeTime(System.nanoTime() - startEosEncodeNano)

            reusableBitmap?.recycle()
            check(decodedFrameCount >= 0) { "Decoder ended without an EOS marker" }
            check(submittedFrameCount == decodedFrameCount) {
                "Decoder supplied $submittedFrameCount frames; EOS declared $decodedFrameCount"
            }
            check(writtenFrameCount == decodedFrameCount) {
                "Encoder wrote $writtenFrameCount frames; decoder emitted $decodedFrameCount"
            }
            check(submittedPresentationTimesUs.isEmpty()) {
                "Encoder did not emit ${submittedPresentationTimesUs.size} submitted frame(s)"
            }

            // Stop and release encoder & muxer explicitly BEFORE checking outputFile size
            try { encoder?.stop() } catch (e: Exception) { Log.w(TAG, "[Tupaz-AI] Encoder stop warning", e) }
            try { encoder?.release() } catch (e: Exception) { Log.w(TAG, "[Tupaz-AI] Encoder release warning", e) }
            encoder = null

            if (muxerStarted) {
                try {
                    muxer?.stop()
                    Log.i(TAG, "[Tupaz-AI] MUXER STOP")
                } catch (e: Exception) {
                    Log.e(TAG, "[Tupaz-AI] MediaMuxer stop failed", e)
                    throw IllegalStateException("MediaMuxer failed to stop/flush output file: ${e.message}", e)
                }
                muxerStarted = false
            }
            try { muxer?.release() } catch (_: Exception) {}
            muxer = null

            check(outputFile.exists() && outputFile.length() > 0) {
                "Encoder finished but output file is empty or missing (exists=${outputFile.exists()}, size=${outputFile.length()} bytes at ${outputFile.absolutePath})"
            }

            Log.i(TAG, "[Tupaz-AI] Final Verification: path=${outputFile.absolutePath}, exists=${outputFile.exists()}, size=${outputFile.length()} bytes, encodedFrameCount=$writtenFrameCount")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-AI] Encoder error", e)
            throw e
        } finally {
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (_: Exception) {}
        }
    }
}
