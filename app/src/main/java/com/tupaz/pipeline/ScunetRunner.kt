package com.tupaz.pipeline

import android.util.Log
import com.tupaz.artifact.CompressionClassifier
import com.tupaz.cache.ModelVramCache

/**
 * SCUNet artifact restoration pipeline stage runner.
 * Gated by [CompressionClassifier] to skip clean content (~60% compute savings).
 */
class ScunetRunner(
    private val ncnnBridge: NcnnBridge = NcnnBridge(),
    private val classifier: CompressionClassifier = CompressionClassifier(),
    private val modelVramCache: ModelVramCache? = null
) {
    companion object {
        private const val TAG = "ScunetRunner"
    }

    /**
     * Executes SCUNet deblocking conditionally based on compression gating.
     */
    fun process(
        inputBuffer: ByteArray,
        width: Int,
        height: Int,
        modelId: String = "scunet-lite"
    ): ByteArray {
        val classification = classifier.classify(inputBuffer, width, height)

        if (!classification.requiresScunet) {
            Log.i(TAG, "Skipping SCUNet — content is clean (score: ${classification.compressionScore})")
            return inputBuffer
        }

        modelVramCache?.getOrLoad(modelId)
        Log.i(TAG, "Executing SCUNet deblocking (score: ${classification.compressionScore})")
        val result = ncnnBridge.processFrame(inputBuffer, width, height, mode = 3)
        if (result == null) {
            val msg = "[Tupaz-AI] SCUNet native processFrame returned NULL (${width}x${height})."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        return result
    }
}
