package com.tupaz.pipeline

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.round

class DeviceBenchmarkTracker(private val context: Context) {
    companion object {
        private const val TAG = "Tupaz-Speed"
    }

    private var startWallNano: Long = 0
    private var totalWallTimeMs: Long = 0

    // Device Metadata
    var deviceModel: String = ""
        private set
    var androidVersion: String = ""
        private set
    var abi: String = ""
        private set
    var totalRamMb: Long = 0
        private set
    var availRamMb: Long = 0
        private set

    // Input Specs
    var inputWidth: Int = 0
    var inputHeight: Int = 0
    var inputFps: Int = 0
    var inputDurationMs: Long = 0
    var expectedFrameCount: Int = 0

    // Output Specs
    var outputWidth: Int = 0
    var outputHeight: Int = 0
    var outputFps: Int = 0
    var outputDurationMs: Long = 0
    var outputFileSizeBytes: Long = 0
    var modelName: String = ""
    var qualityLevel: String = ""

    // Thread-safe Timings (nanoseconds)
    val decodeNs = AtomicLong(0)
    val yuvToRgbaNs = AtomicLong(0)
    val ncnnInferenceNs = AtomicLong(0)
    val postprocessNs = AtomicLong(0)
    val encodeNs = AtomicLong(0)

    // Thread-safe Frame Counts
    val decodedFramesCount = AtomicInteger(0)
    val aiProcessedFramesCount = AtomicInteger(0)
    val encodedFramesCount = AtomicInteger(0)

    init {
        collectDeviceInfo()
    }

    private fun collectDeviceInfo() {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        deviceModel = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
        androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalRamMb = memInfo.totalMem / (1024 * 1024)
                availRamMb = memInfo.availMem / (1024 * 1024)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query RAM info", e)
        }
    }

    fun startWallClock() {
        startWallNano = System.nanoTime()
    }

    fun stopWallClock() {
        if (startWallNano > 0) {
            totalWallTimeMs = (System.nanoTime() - startWallNano) / 1_000_000L
        }
    }

    fun addYuvToRgbaTime(nanos: Long) {
        yuvToRgbaNs.addAndGet(nanos)
    }

    fun addNcnnInferenceTime(nanos: Long) {
        ncnnInferenceNs.addAndGet(nanos)
    }

    fun addPostprocessTime(nanos: Long) {
        postprocessNs.addAndGet(nanos)
    }

    fun addDecodeTime(nanos: Long) {
        decodeNs.addAndGet(nanos)
    }

    fun addEncodeTime(nanos: Long) {
        encodeNs.addAndGet(nanos)
    }

    private fun round2(value: Double): Double {
        return (round(value * 100.0) / 100.0)
    }

    fun generateBenchmarkReport(): BenchmarkReport {
        val decodeMs = decodeNs.get() / 1_000_000L
        val yuvToRgbaMs = yuvToRgbaNs.get() / 1_000_000L
        val ncnnInferenceMs = ncnnInferenceNs.get() / 1_000_000L
        val postprocessMs = postprocessNs.get() / 1_000_000L
        val encodeMs = encodeNs.get() / 1_000_000L

        val decoded = decodedFramesCount.get()
        val aiProcessed = aiProcessedFramesCount.get()
        val encoded = encodedFramesCount.get()

        val avgMsPerFrame = if (encoded > 0 && totalWallTimeMs > 0) {
            round2(totalWallTimeMs.toDouble() / encoded)
        } else 0.0

        val ncnnMsPerFrame = if (aiProcessed > 0 && ncnnInferenceMs > 0) {
            round2(ncnnInferenceMs.toDouble() / aiProcessed)
        } else 0.0

        val processingFps = if (totalWallTimeMs > 0 && encoded > 0) {
            round2((encoded * 1000.0) / totalWallTimeMs)
        } else 0.0

        val realtimeFactor = if (inputDurationMs > 0 && totalWallTimeMs > 0) {
            round2(totalWallTimeMs.toDouble() / inputDurationMs)
        } else 0.0

        val fileValid = (outputFileSizeBytes > 0)
        val frameCountMatch = (decoded == expectedFrameCount && aiProcessed == expectedFrameCount && encoded == expectedFrameCount)
        val fpsMatch = (inputFps == outputFps)
        val durationMatch = (abs(inputDurationMs - outputDurationMs) <= 1000L)
        val overallSuccess = frameCountMatch && fpsMatch && durationMatch && fileValid

        return BenchmarkReport(
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            abi = abi,
            ramInfo = "$availRamMb MB / $totalRamMb MB",
            modelName = modelName,
            qualityLevel = qualityLevel,
            inputWidth = inputWidth,
            inputHeight = inputHeight,
            inputFps = inputFps,
            inputDurationMs = inputDurationMs,
            inputFrameCount = expectedFrameCount,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            outputFps = outputFps,
            outputDurationMs = outputDurationMs,
            outputFrameCount = encoded,
            fileSizeBytes = outputFileSizeBytes,
            decodeMs = decodeMs,
            yuvToRgbaMs = yuvToRgbaMs,
            ncnnInferenceMs = ncnnInferenceMs,
            postprocessMs = postprocessMs,
            encodeMs = encodeMs,
            totalWallTimeMs = totalWallTimeMs,
            averageMsPerFrame = avgMsPerFrame,
            ncnnMsPerFrame = ncnnMsPerFrame,
            processingFps = processingFps,
            realtimeFactor = realtimeFactor,
            decodedFrames = decoded,
            aiProcessedFrames = aiProcessed,
            encodedFrames = encoded,
            frameCountMatch = frameCountMatch,
            fpsMatch = fpsMatch,
            durationMatch = durationMatch,
            outputFileValid = fileValid,
            overallSuccess = overallSuccess
        )
    }

    fun buildJson(report: BenchmarkReport): JSONObject {
        val root = JSONObject()

        val dev = JSONObject().apply {
            put("model", report.deviceModel)
            put("androidVersion", report.androidVersion)
            put("abi", report.abi)
        }

        val aiModel = JSONObject().apply {
            put("modelName", report.modelName)
            put("quality", report.qualityLevel)
        }

        val input = JSONObject().apply {
            put("width", report.inputWidth)
            put("height", report.inputHeight)
            put("fps", report.inputFps)
            put("durationMs", report.inputDurationMs)
            put("frameCount", report.inputFrameCount)
        }

        val output = JSONObject().apply {
            put("width", report.outputWidth)
            put("height", report.outputHeight)
            put("fps", report.outputFps)
            put("durationMs", report.outputDurationMs)
            put("frameCount", report.outputFrameCount)
            put("fileSizeBytes", report.fileSizeBytes)
        }

        val timing = JSONObject().apply {
            put("decodeMs", report.decodeMs)
            put("yuvToRgbaMs", report.yuvToRgbaMs)
            put("ncnnInferenceMs", report.ncnnInferenceMs)
            put("postprocessMs", report.postprocessMs)
            put("encodeMs", report.encodeMs)
            put("totalWallTimeMs", report.totalWallTimeMs)
        }

        val performance = JSONObject().apply {
            put("averageMsPerFrame", report.averageMsPerFrame)
            put("ncnnMsPerFrame", report.ncnnMsPerFrame)
            put("processingFps", report.processingFps)
            put("realtimeFactor", report.realtimeFactor)
        }

        val correctness = JSONObject().apply {
            put("decodedFrames", report.decodedFrames)
            put("aiProcessedFrames", report.aiProcessedFrames)
            put("encodedFrames", report.encodedFrames)
            put("frameCountMatch", report.frameCountMatch)
            put("fpsMatch", report.fpsMatch)
            put("durationMatch", report.durationMatch)
            put("outputFileValid", report.outputFileValid)
            put("overallSuccess", report.overallSuccess)
        }

        root.put("device", dev)
        root.put("aiModel", aiModel)
        root.put("input", input)
        root.put("output", output)
        root.put("timing", timing)
        root.put("performance", performance)
        root.put("correctness", correctness)

        return root
    }

    fun logAndSave(outputFile: File): BenchmarkReport {
        stopWallClock()
        outputFileSizeBytes = if (outputFile.exists()) outputFile.length() else 0L
        val report = generateBenchmarkReport()
        val json = buildJson(report)

        val matchStatusStr = when {
            !report.outputFileValid -> "FAIL (Output file size 0 bytes)"
            !report.overallSuccess -> "FAIL"
            else -> "PASS"
        }

        val logcatText = """
            
            [Tupaz-Speed] ===== DEVICE BENCHMARK =====
            
            Model = ${report.modelName}
            Quality = ${report.qualityLevel}
            ms/frame = ${report.ncnnMsPerFrame}ms
            processing FPS = ${report.processingFps} FPS
            
            Device:
              Model: ${report.deviceModel}
              Android: ${report.androidVersion}
              ABI: ${report.abi}
              RAM: ${report.ramInfo}
            
            AI Model:
              Model Name: ${report.modelName}
              Quality: ${report.qualityLevel}
            
            Input:
              Resolution: ${report.inputWidth}x${report.inputHeight}
              FPS: ${report.inputFps}
              Duration: ${report.inputDurationMs}ms
              Frames: ${report.inputFrameCount}
            
            Output:
              Resolution: ${report.outputWidth}x${report.outputHeight}
              FPS: ${report.outputFps}
              Duration: ${report.outputDurationMs}ms
              Frames: ${report.outputFrameCount}
              File Size: ${report.fileSizeBytes} bytes ${if (!report.outputFileValid) "(FAIL - OUTPUT FILE EMPTY)" else ""}
            
            Timing:
              Decode: ${report.decodeMs}ms
              YUV->RGBA: ${report.yuvToRgbaMs}ms
              NCNN/Vulkan: ${report.ncnnInferenceMs}ms
              Postprocess: ${report.postprocessMs}ms
              Encode: ${report.encodeMs}ms
              Total: ${report.totalWallTimeMs}ms
            
            Performance:
              Average ms/frame: ${report.averageMsPerFrame}ms
              NCNN ms/frame: ${report.ncnnMsPerFrame}ms
              Processing FPS: ${report.processingFps} FPS
              Realtime factor: ${report.realtimeFactor}x
            
            Correctness:
              Decoded: ${report.decodedFrames}
              AI processed: ${report.aiProcessedFrames}
              Encoded: ${report.encodedFrames}
              Match: $matchStatusStr
            
        """.trimIndent()

        Log.i(TAG, logcatText)
        Log.i(TAG, "[Tupaz-Speed] JSON Report:\n${json.toString(2)}")

        try {
            val benchmarkFile1 = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.benchmark.json")
            benchmarkFile1.writeText(json.toString(2))
            Log.i(TAG, "[Tupaz-Speed] Saved benchmark JSON to ${benchmarkFile1.absolutePath}")

            val benchmarkFile2 = File(outputFile.absolutePath + ".benchmark.json")
            if (benchmarkFile2.absolutePath != benchmarkFile1.absolutePath) {
                benchmarkFile2.writeText(json.toString(2))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Speed] Failed to save benchmark JSON file", e)
        }

        return report
    }
}

data class BenchmarkReport(
    val deviceModel: String,
    val androidVersion: String,
    val abi: String,
    val ramInfo: String,
    val modelName: String = "",
    val qualityLevel: String = "",
    val inputWidth: Int,
    val inputHeight: Int,
    val inputFps: Int,
    val inputDurationMs: Long,
    val inputFrameCount: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputFps: Int,
    val outputDurationMs: Long,
    val outputFrameCount: Int,
    val fileSizeBytes: Long,
    val decodeMs: Long,
    val yuvToRgbaMs: Long,
    val ncnnInferenceMs: Long,
    val postprocessMs: Long,
    val encodeMs: Long,
    val totalWallTimeMs: Long,
    val averageMsPerFrame: Double,
    val ncnnMsPerFrame: Double,
    val processingFps: Double,
    val realtimeFactor: Double,
    val decodedFrames: Int,
    val aiProcessedFrames: Int,
    val encodedFrames: Int,
    val frameCountMatch: Boolean,
    val fpsMatch: Boolean,
    val durationMatch: Boolean,
    val outputFileValid: Boolean,
    val overallSuccess: Boolean
)
