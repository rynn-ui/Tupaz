package com.tupaz.auto

import com.tupaz.benchmark.DeviceTier
import com.tupaz.domain.pipeline.ProcessingMode

/**
 * Pure Kotlin domain selector mapping content signals and hardware tier to optimal processing mode.
 * Zero Android framework dependencies.
 */
class AutoPipelineSelector {

    /**
     * Determines optimal processing mode based on content signals and device performance tier.
     */
    fun selectMode(signals: ContentSignals, tier: DeviceTier): ProcessingMode {
        // Rule 1: Anime content always maps to ANIME mode (if device is MID+)
        if (signals.isAnime && tier != DeviceTier.LOW) {
            return ProcessingMode.ANIME
        }

        // Rule 2: Low hardware tier forces FAST mode to maintain real-time throughput
        if (tier == DeviceTier.LOW) {
            return ProcessingMode.FAST
        }

        // Rule 3: Heavy compression on MID/HIGH tier maps to BALANCED mode with SCUNet
        if (signals.compressionScore > 0.2f && (tier == DeviceTier.MID || tier == DeviceTier.HIGH)) {
            return ProcessingMode.BALANCED
        }

        // Rule 4: Clean content on HIGH or FLAGSHIP tier maps to ULTRA mode
        if (tier == DeviceTier.HIGH || tier == DeviceTier.FLAGSHIP) {
            return ProcessingMode.ULTRA
        }

        return ProcessingMode.BALANCED
    }
}
