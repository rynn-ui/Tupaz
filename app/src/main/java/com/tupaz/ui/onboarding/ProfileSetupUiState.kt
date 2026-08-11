package com.tupaz.ui.onboarding

data class ProfileSetupUiState(
    val name: String = "",
    val age: String = "",
    val nameError: String? = null,
    val ageError: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false
) {
    val isValid: Boolean
        get() {
            val nameTrimmed = name.trim()
            val ageInt = age.toIntOrNull()
            return nameTrimmed.isNotEmpty() &&
                    nameTrimmed.length <= 50 &&
                    ageInt != null &&
                    ageInt in 1..120
        }
}
