package com.tupaz.benchmark

import com.tupaz.domain.pipeline.ProcessingMode

/**
 * Hardware performance tier categories.
 */
enum class DeviceTier(
    val displayName: String,
    val defaultMode: ProcessingMode
) {
    LOW("Low Tier", ProcessingMode.FAST),
    MID("Mid Tier", ProcessingMode.BALANCED),
    HIGH("High Tier", ProcessingMode.ULTRA),
    FLAGSHIP("Flagship Tier", ProcessingMode.AUTO)
}

/**
 * Device hardware profile result after running GPU benchmarks.
 */
data class DeviceProfile(
    val tier: DeviceTier,
    val benchmarkResult: BenchmarkResultData,
    val recommendedMode: ProcessingMode = tier.defaultMode
) {
    companion object {
        /**
         * Classifies benchmark result into hardware tier based on memory thresholds.
         */
        fun classify(result: BenchmarkResultData): DeviceProfile {
            val vulkanMp = result.vulkanMegapixelsPerSec
            val bw = result.memoryBandwidthGbps

            val tier = when {
                vulkanMp > 3500 && bw > 70 -> DeviceTier.FLAGSHIP
                vulkanMp in 1800.0..3500.0 && bw >= 40 -> DeviceTier.HIGH
                vulkanMp in 800.0..1800.0 && bw >= 20 -> DeviceTier.MID
                else -> DeviceTier.LOW
            }

            return DeviceProfile(
                tier = tier,
                benchmarkResult = result,
                recommendedMode = tier.defaultMode
            )
        }
    }
}
