package com.tupaz.pipeline

import android.content.Context
import android.net.Uri

/**
 * Interface for extracting video frames.
 * Implementations are responsible for correctly interpreting video timing and metadata,
 * and emitting decoded raw frames to the provided callback.
 */
interface FrameDecoder {
    /**
     * Decodes frames from the given [inputUri] and calls [onFrame] for each frame.
     * 
     * @param expectedFrameCount The total number of frames expected to be produced.
     * @param durationMs The total duration of the video track.
     * @param fps The frames per second.
     * @return The number of decoded frames emitted.
     */
    suspend fun decode(
        context: Context,
        inputUri: Uri,
        expectedFrameCount: Int,
        durationMs: Long,
        fps: Int,
        tracker: DeviceBenchmarkTracker? = null,
        onFrame: suspend (RawFrame) -> Unit
    ): DecodeResult
}
