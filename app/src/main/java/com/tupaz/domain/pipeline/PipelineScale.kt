package com.tupaz.domain.pipeline

/**
 * Validates and parses scale factor representations for the video enhancement pipeline.
 *
 * Authoritative pipeline boundary validator converting scale representations
 * (e.g. "2x", "2x Scale", "2", "2.0") into validated integer scale values.
 */
object PipelineScale {

    /**
     * Parses and validates raw scale string representation.
     *
     * @param rawScale Input string representation of scale factor.
     * @return Validated integer scale factor (>= 1).
     * @throws IllegalArgumentException if format is invalid or scale is non-positive.
     */
    fun parseAndValidate(rawScale: String?): Int {
        if (rawScale.isNullOrBlank()) {
            throw IllegalArgumentException("Scale factor string cannot be null or blank")
        }

        val trimmed = rawScale.trim().lowercase()

        // Remove suffix "scale" and "x"
        var sanitized = trimmed
        if (sanitized.endsWith("scale")) {
            sanitized = sanitized.substring(0, sanitized.length - 5).trim()
        }
        if (sanitized.endsWith("x")) {
            sanitized = sanitized.substring(0, sanitized.length - 1).trim()
        }

        if (sanitized.isEmpty()) {
            throw IllegalArgumentException("Invalid scale factor format: '$rawScale'")
        }

        val numericVal = try {
            if (sanitized.contains(".")) {
                val floatVal = sanitized.toDouble()
                if (floatVal != floatVal.toLong().toDouble()) {
                    throw IllegalArgumentException("Fractional scale factor not supported: '$rawScale'")
                }
                floatVal.toInt()
            } else {
                sanitized.toInt()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid non-numeric scale factor: '$rawScale'", e)
        }

        if (numericVal <= 0) {
            throw IllegalArgumentException("Scale factor must be positive: $numericVal")
        }

        return numericVal
    }
}
