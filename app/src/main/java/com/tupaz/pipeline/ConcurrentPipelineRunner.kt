package com.tupaz.pipeline

import android.util.Log
import com.tupaz.domain.pipeline.ProcessingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Executes a 3-channel concurrent pipeline (Decoder -> AI Pipeline -> Encoder) using coroutine Channels
 * with capacity back-pressure according to RULES.md Rule 9 and ADR-0001.
 */
class ConcurrentPipelineRunner(
    private val orchestrator: PipelineOrchestrator = PipelineOrchestrator(),
    private val channelCapacity: Int = 4
) {
    companion object {
        private const val TAG = "ConcurrentPipelineRunner"
    }

    /**
     * Starts concurrent pipeline processing over decoder frame producer and encoder frame consumer.
     * @param scope CoroutineScope executing jobs.
     * @param mode Selected processing mode.
     * @param frameProducer Function feeding raw frames from decoder thread (Dispatchers.IO).
     * @param frameConsumer Function writing processed frames to encoder thread (Dispatchers.IO).
     */
    fun startPipeline(
        scope: CoroutineScope,
        mode: ProcessingMode,
        frameProducer: suspend (Channel<RawFrame>) -> Unit,
        frameConsumer: suspend (Channel<ProcessedFrame>) -> Unit
    ) {
        val decoderChannel = Channel<RawFrame>(capacity = channelCapacity)
        val aiChannel = Channel<ProcessedFrame>(capacity = channelCapacity)

        // Stage 1: Decoder Producer Job (Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            Log.i(TAG, "Started Decoder stage on Dispatchers.IO")
            try {
                frameProducer(decoderChannel)
            } finally {
                decoderChannel.close()
            }
        }

        // Stage 2: AI Processing Job (Dispatchers.Default)
        scope.launch(Dispatchers.Default) {
            Log.i(TAG, "Started AI Processing stage on Dispatchers.Default")
            try {
                for (rawFrame in decoderChannel) {
                    val processed = orchestrator.processFrame(rawFrame, mode)
                    aiChannel.send(processed)
                    if (rawFrame.isEndOfStream) break
                }
            } finally {
                aiChannel.close()
            }
        }

        // Stage 3: Encoder Consumer Job (Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            Log.i(TAG, "Started Encoder stage on Dispatchers.IO")
            frameConsumer(aiChannel)
        }
    }
}
