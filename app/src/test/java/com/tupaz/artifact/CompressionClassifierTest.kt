package com.tupaz.artifact

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompressionClassifierTest {

    private lateinit var classifier: CompressionClassifier

    @Before
    fun setUp() {
        classifier = CompressionClassifier(gatingThreshold = 0.2f)
    }

    @Test
    fun `classify returns requiresScunet false for clean frame score below threshold`() {
        val dummyFrame = ByteArray(1920 * 1080 * 4)
        val result = classifier.classify(dummyFrame, 1920, 1080)

        assertNotNull(result)
        assertTrue(result.compressionScore < 0.2f)
        assertFalse(result.requiresScunet)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `classify throws exception for invalid non-positive width`() {
        classifier.classify(ByteArray(10), 0, 1080)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `classify throws exception for invalid non-positive height`() {
        classifier.classify(ByteArray(10), 1920, -5)
    }
}
