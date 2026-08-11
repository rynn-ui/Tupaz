package com.tupaz.data.settings

import android.content.Context
import com.tupaz.ui.theme.ButtonSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ButtonSizeStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _buttonSizeFlow = MutableStateFlow(getButtonSize())
    val buttonSizeFlow: StateFlow<ButtonSize> = _buttonSizeFlow.asStateFlow()

    fun getButtonSize(): ButtonSize {
        val rawValue = prefs.getString(KEY_BUTTON_SIZE, null) ?: return ButtonSize.NORMAL
        if (rawValue.equals("MEDIUM", ignoreCase = true)) {
            prefs.edit().putString(KEY_BUTTON_SIZE, ButtonSize.NORMAL.name).apply()
            return ButtonSize.NORMAL
        }
        return ButtonSize.fromString(rawValue)
    }

    fun saveButtonSize(size: ButtonSize) {
        prefs.edit().putString(KEY_BUTTON_SIZE, size.name).apply()
        _buttonSizeFlow.value = size
    }

    companion object {
        private const val PREFS_NAME = "tupaz_settings_prefs"
        private const val KEY_BUTTON_SIZE = "button_size"
    }
}
