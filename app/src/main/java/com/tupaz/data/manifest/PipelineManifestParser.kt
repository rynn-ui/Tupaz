package com.tupaz.data.manifest

import com.tupaz.domain.pipeline.PipelineManifest
import kotlinx.serialization.json.Json

/**
 * Parser utility for reading and deserializing pipeline manifest configurations.
 */
class PipelineManifestParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
) {
    /**
     * Parses JSON string into [PipelineManifest].
     * @throws IllegalArgumentException if JSON string is invalid or malformed.
     */
    fun parse(jsonContent: String): PipelineManifest {
        require(jsonContent.isNotBlank()) { "JSON content must not be blank" }
        return try {
            json.decodeFromString<PipelineManifest>(jsonContent)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse pipeline manifest JSON", e)
        }
    }
}
