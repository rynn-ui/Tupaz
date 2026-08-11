package com.tupaz.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class ModelDownloadIntegrityTest {

    private fun validateSha256(computedSha256: String, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) return false
        val isFormatValid = expectedSha256.length == 64 &&
                expectedSha256.all { c -> c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F' }
        return isFormatValid && computedSha256.equals(expectedSha256, ignoreCase = true)
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `exact 64-character SHA-256 match passes validation`() {
        val sampleData = "Tupaz AI Model Weights Baseline".toByteArray()
        val computedHash = sha256(sampleData)

        assertTrue(validateSha256(computedHash, computedHash))
        assertTrue(validateSha256(computedHash, computedHash.uppercase()))
    }

    @Test
    fun `mismatched SHA-256 fails validation`() {
        val sampleData = "Tupaz AI Model Weights Baseline".toByteArray()
        val computedHash = sha256(sampleData)
        val wrongHash = sha256("Corrupted Weights Data".toByteArray())

        assertFalse(validateSha256(computedHash, wrongHash))
    }

    @Test
    fun `empty or null expected SHA-256 fails validation`() {
        val sampleData = "Tupaz AI Model Weights Baseline".toByteArray()
        val computedHash = sha256(sampleData)

        assertFalse(validateSha256(computedHash, ""))
        assertFalse(validateSha256(computedHash, null))
    }

    @Test
    fun `prefix-only expected SHA-256 fails validation`() {
        val sampleData = "Tupaz AI Model Weights Baseline".toByteArray()
        val computedHash = sha256(sampleData)
        val prefixHash = computedHash.take(32) // 32 chars instead of 64

        assertFalse(validateSha256(computedHash, prefixHash))
    }

    @Test
    fun `empty-file hash bypass string fails if computed hash differs`() {
        val sampleData = "Tupaz AI Model Weights Baseline".toByteArray()
        val computedHash = sha256(sampleData)
        val emptyFileHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        assertFalse(validateSha256(computedHash, emptyFileHash))
    }
}
