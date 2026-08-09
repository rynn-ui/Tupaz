package com.tupaz.pipeline

/**
 * Represents an uncompressed frame buffer emitted by the video decoder channel.
 */
data class RawFrame(
    val frameIndex: Long,
    val presentationTimeUs: Long,
    val width: Int,
    val height: Int,
    val data: ByteArray,
    val isEndOfStream: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawFrame) return false
        return frameIndex == other.frameIndex && isEndOfStream == other.isEndOfStream
    }

    override fun hashCode(): Int {
        var result = frameIndex.hashCode()
        result = 31 * result + isEndOfStream.hashCode()
        return result
    }
}
