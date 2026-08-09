package com.tupaz.pipeline

import android.util.Log
import com.tupaz.domain.pipeline.ProcessingMode

/**
 * Orchestrates multi-stage AI frame processing dedicated strictly to pure video quality enhancement:
 * Denoising, Super-Resolution, Compression Artifact Removal, and Sharpening.
 */
class PipelineOrchestrator(
    private val denoiser: Denoiser = Denoiser(),
    private val scunetRunner: ScunetRunner = ScunetRunner(),
    private val upscaler: NcnnUpscaler = NcnnUpscaler(),
    private val sharpen: VulkanSharpen = VulkanSharpen(),
    private val modelId: String = "realesr-animevideov3-x2"
) {
    companion object {
        private const val TAG = "PipelineOrchestrator"
    }

    /**
     * Executes quality enhancement pipeline stages on input frame according to [ProcessingMode].
     */
    fun processFrame(
        rawFrame: RawFrame,
        mode: ProcessingMode
    ): ProcessedFrame {
        if (rawFrame.isEndOfStream) {
            return ProcessedFrame(
                frameIndex = rawFrame.frameIndex,
                presentationTimeUs = rawFrame.presentationTimeUs,
                width = rawFrame.width,
                height = rawFrame.height,
                data = ByteArray(0),
                isEndOfStream = true
            )
        }

        var buffer = rawFrame.data
        var currentWidth = rawFrame.width
        var currentHeight = rawFrame.height

        // Neural Pipeline Inference - Every decoded frame passes through RealESRGAN
        when (mode) {
            ProcessingMode.FAST -> {
                buffer = upscaler.process(buffer, currentWidth, currentHeight, modelId, scaleFactor = 2)
                currentWidth *= 2
                currentHeight *= 2
            }
            ProcessingMode.BALANCED -> {
                buffer = scunetRunner.process(buffer, currentWidth, currentHeight)
                buffer = upscaler.process(buffer, currentWidth, currentHeight, modelId, scaleFactor = 2)
                currentWidth *= 2
                currentHeight *= 2
            }
            ProcessingMode.ULTRA -> {
                buffer = denoiser.process(buffer, currentWidth, currentHeight)
                buffer = upscaler.process(buffer, currentWidth, currentHeight, modelId, scaleFactor = 2)
                currentWidth *= 2
                currentHeight *= 2
            }
            ProcessingMode.ANIME -> {
                buffer = upscaler.process(buffer, currentWidth, currentHeight, modelId, scaleFactor = 2)
                currentWidth *= 2
                currentHeight *= 2
            }
            ProcessingMode.AUTO -> {
                buffer = upscaler.process(buffer, currentWidth, currentHeight, modelId, scaleFactor = 2)
                currentWidth *= 2
                currentHeight *= 2
            }
        }

        return ProcessedFrame(
            frameIndex = rawFrame.frameIndex,
            presentationTimeUs = rawFrame.presentationTimeUs,
            width = currentWidth,
            height = currentHeight,
            data = buffer,
            isEndOfStream = false
        )
    }
}
