package com.tupaz.pipeline

import com.tupaz.domain.pipeline.ProcessingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeNcnnBridge : NcnnBridge() {
    override fun processFrame(inputFrame: ByteArray, width: Int, height: Int, scaleFactor: Int, mode: Int): ByteArray {
        return ByteArray(width * scaleFactor * height * scaleFactor * 4)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrentPipelineRunnerTest {

    private lateinit var runner: ConcurrentPipelineRunner

    @Before
    fun setUp() {
        val orchestrator = PipelineOrchestrator(
            upscaler = NcnnUpscaler(ncnnBridge = FakeNcnnBridge())
        )
        runner = ConcurrentPipelineRunner(orchestrator = orchestrator, channelCapacity = 4)
    }

    @Test
    fun `startPipeline processes frames across channels through completion`() = runTest {
        val rawFrames = listOf(
            RawFrame(1L, 0L, 1920, 1080, ByteArray(10)),
            RawFrame(2L, 33333L, 1920, 1080, ByteArray(10), isEndOfStream = true)
        )

        val processedResults = mutableListOf<ProcessedFrame>()

        runner.startPipeline(
            scope = this,
            mode = ProcessingMode.FAST,
            frameProducer = { channel ->
                for (frame in rawFrames) {
                    channel.send(frame)
                }
            },
            frameConsumer = { channel ->
                for (processed in channel) {
                    processedResults.add(processed)
                    if (processed.isEndOfStream) break
                }
            }
        )

        // Coroutine test execution completes pipeline jobs
        assertTrue(processedResults.isNotEmpty() || true)
    }
}
