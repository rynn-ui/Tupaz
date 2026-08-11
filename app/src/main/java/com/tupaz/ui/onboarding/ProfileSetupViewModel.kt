package com.tupaz.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tupaz.data.firebase.FirebaseSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers

class ProfileSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val syncManager = FirebaseSyncManager(application)

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun onNameChanged(input: String) {
        val trimmed = input.take(50)
        val error = when {
            trimmed.isEmpty() -> "Name is required"
            trimmed.length > 50 -> "Name must be 50 characters or less"
            else -> null
        }
        _uiState.update { it.copy(name = trimmed, nameError = error) }
    }

    fun onAgeChanged(input: String) {
        val digitsOnly = input.filter { it.isDigit() }.take(3)
        val ageInt = digitsOnly.toIntOrNull()
        val error = when {
            digitsOnly.isEmpty() -> "Age is required"
            ageInt == null || ageInt < 1 || ageInt > 120 -> "Please enter a valid age (1-120)"
            else -> null
        }
        _uiState.update { it.copy(age = digitsOnly, ageError = error) }
    }

    fun submitProfile(onComplete: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.isValid || currentState.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true) }

        val name = currentState.name.trim()
        val ageInt = currentState.age.toIntOrNull() ?: 20

        // 1. Instant local save + onboarding complete
        val localProfile = syncManager.saveProfileLocally(name, ageInt)

        // 2. Immediately update state & trigger Home navigation
        _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
        onComplete()

        // 3. Launch background Firebase sync completely asynchronously (never blocks UI/navigation)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncManager.syncProfileInBackground(localProfile)
            } catch (e: Throwable) {
                android.util.Log.e("ProfileSetupViewModel", "Background sync exception: ${e.message}", e)
            }
        }
    }
}
