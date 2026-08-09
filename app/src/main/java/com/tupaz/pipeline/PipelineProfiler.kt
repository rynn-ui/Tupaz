package com.tupaz.pipeline

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class StageMetricsData(
    val meanMs: Double,
    val p99Ms: Double,
    val pct: Double
)

@Serializable
data class ProfilerExportReport(
    val device: String,
    val mode: String,
    val frameCount: Long,
    val flowSkipPct: Double,
    val scunetSkipPct: Double,
    val qualitySkipPct: Double = 0.0,
    val stages: Map<String, StageMetricsData>
)

/**
 * Nanosecond precision performance profiler measuring stage latency according to RULES.md Rule 7 and PERFORMANCE.md.
 */
class PipelineProfiler(
    private val json: Json = Json { prettyPrint = true }
) {
    @PublishedApi
    internal val stageTimings = ConcurrentHashMap<String, MutableList<Long>>()
    private var totalFramesProcessed: Long = 0
    private var flowSkippedFrames: Long = 0
    private var scunetSkippedFrames: Long = 0
    private var qualitySkippedFrames: Long = 0

    /**
     * Measures block execution time for a stage.
     */
    inline fun <T> recordStage(stageName: String, block: () -> T): T {
        val start = System.nanoTime()
        return try {
            block()
        } finally {
            val durationNs = System.nanoTime() - start
            stageTimings.getOrPut(stageName) { mutableListOf() }.add(durationNs)
        }
    }

    fun incrementFrameCount() {
        totalFramesProcessed++
    }

    fun recordFlowSkip() {
        flowSkippedFrames++
    }

    fun recordScunetSkip() {
        scunetSkippedFrames++
    }

    fun recordQualitySkip() {
        qualitySkippedFrames++
    }

    /**
     * Generates profiler statistics report.
     */
    fun generateReport(deviceInfo: String = "Snapdragon 870 / Adreno 650", modeName: String = "BALANCED"): ProfilerExportReport {
        val totalTimeNs = stageTimings.values.flatMap { it }.sum().toDouble()
        val stageMetrics = mutableMapOf<String, StageMetricsData>()

        for ((stageName, timings) in stageTimings) {
            if (timings.isEmpty()) continue
            val sorted = timings.sorted()
            val meanMs = (sorted.average() / 1e6)
            val p99Index = (sorted.size * 0.99).toInt().coerceAtMost(sorted.size - 1)
            val p99Ms = sorted[p99Index] / 1e6
            val pct = if (totalTimeNs > 0) (sorted.sum().toDouble() / totalTimeNs) * 100.0 else 0.0

            stageMetrics[stageName] = StageMetricsData(
                meanMs = (meanMs * 10.0).toInt() / 10.0,
                p99Ms = (p99Ms * 10.0).toInt() / 10.0,
                pct = (pct * 10.0).toInt() / 10.0
            )
        }

        val flowSkipPct = if (totalFramesProcessed > 0) (flowSkippedFrames.toDouble() / totalFramesProcessed) * 100.0 else 0.0
        val scunetSkipPct = if (totalFramesProcessed > 0) (scunetSkippedFrames.toDouble() / totalFramesProcessed) * 100.0 else 0.0
        val qualitySkipPct = if (totalFramesProcessed > 0) (qualitySkippedFrames.toDouble() / totalFramesProcessed) * 100.0 else 0.0

        return ProfilerExportReport(
            device = deviceInfo,
            mode = modeName,
            frameCount = totalFramesProcessed,
            flowSkipPct = (flowSkipPct * 10.0).toInt() / 10.0,
            scunetSkipPct = (scunetSkipPct * 10.0).toInt() / 10.0,
            qualitySkipPct = (qualitySkipPct * 10.0).toInt() / 10.0,
            stages = stageMetrics
        )
    }

    /**
     * Exports profiler metrics as JSON string matching `memory/performance.md` schema.
     */
    fun exportJson(deviceInfo: String = "Snapdragon 870 / Adreno 650", modeName: String = "BALANCED"): String {
        val report = generateReport(deviceInfo, modeName)
        return json.encodeToString(report)
    }

    fun reset() {
        stageTimings.clear()
        totalFramesProcessed = 0
        flowSkippedFrames = 0
        scunetSkippedFrames = 0
        qualitySkippedFrames = 0
    }
}
