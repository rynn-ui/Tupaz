package com.tupaz.data.permission

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight local storage for tracking permission onboarding introduction state.
 */
class PermissionStorage(context: Context) {
    companion object {
        private const val PREFS_NAME = "tupaz_permission_prefs"
        private const val KEY_PERMISSION_INTRO_COMPLETED = "permission_intro_completed"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasCompletedPermissionIntro(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_INTRO_COMPLETED, false)
    }

    fun setPermissionIntroCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSION_INTRO_COMPLETED, completed).apply()
    }
}
