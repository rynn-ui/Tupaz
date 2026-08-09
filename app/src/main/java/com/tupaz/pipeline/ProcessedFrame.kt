package com.tupaz.pipeline

/**
 * Represents an enhanced frame buffer emitted by the AI pipeline stage channel.
 */
data class ProcessedFrame(
    val frameIndex: Long,
    val presentationTimeUs: Long,
    val width: Int,
    val height: Int,
    val data: ByteArray,
    val isEndOfStream: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedFrame) return false
        return frameIndex == other.frameIndex && isEndOfStream == other.isEndOfStream
    }

    override fun hashCode(): Int {
        var result = frameIndex.hashCode()
        result = 31 * result + isEndOfStream.hashCode()
        return result
    }
}
