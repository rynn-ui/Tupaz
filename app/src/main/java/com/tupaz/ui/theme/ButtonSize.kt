package com.tupaz.ui.theme

enum class ButtonSize(val displayName: String, val scale: Float) {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.15f);

    companion object {
        fun fromString(value: String?): ButtonSize {
            if (value == null) return NORMAL
            return when (value.uppercase()) {
                "SMALL" -> SMALL
                "NORMAL" -> NORMAL
                "LARGE" -> LARGE
                "MEDIUM" -> NORMAL
                else -> NORMAL
            }
        }
    }
}
