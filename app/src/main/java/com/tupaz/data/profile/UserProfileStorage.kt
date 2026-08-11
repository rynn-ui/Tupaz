package com.tupaz.data.profile

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages persistent local user profile, onboarding state, and pending sync status.
 */
class UserProfileStorage(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) {
    companion object {
        private const val PREFS_NAME = "tupaz_user_profile_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_PROFILE_JSON = "user_profile_json"
        private const val KEY_PENDING_SYNC = "pending_sync"
        private const val KEY_LAST_SYNC_ATTEMPT = "last_sync_attempt"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getUserProfile(): UserProfile? {
        val jsonStr = prefs.getString(KEY_USER_PROFILE_JSON, null) ?: return null
        return try {
            json.decodeFromString<UserProfile>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        try {
            val content = json.encodeToString(profile)
            prefs.edit()
                .putString(KEY_USER_PROFILE_JSON, content)
                .putBoolean(KEY_PENDING_SYNC, profile.pendingSync)
                .putLong(KEY_LAST_SYNC_ATTEMPT, profile.lastSyncAttempt)
                .apply()
        } catch (_: Exception) {}
    }

    fun isPendingSync(): Boolean {
        return prefs.getBoolean(KEY_PENDING_SYNC, true)
    }

    fun setPendingSync(pending: Boolean, lastAttemptTime: Long = System.currentTimeMillis()) {
        val current = getUserProfile()
        if (current != null) {
            val updated = current.copy(
                pendingSync = pending,
                lastSyncAttempt = lastAttemptTime
            )
            saveUserProfile(updated)
        } else {
            prefs.edit()
                .putBoolean(KEY_PENDING_SYNC, pending)
                .putLong(KEY_LAST_SYNC_ATTEMPT, lastAttemptTime)
                .apply()
        }
    }

    fun createDefaultProfile(name: String, age: Int, userId: String = ""): UserProfile {
        val existing = getUserProfile()
        val now = System.currentTimeMillis()
        val firstInstallTime = existing?.firstInstall?.takeIf { it > 0L } ?: now
        val resolvedUserId = userId.ifEmpty { existing?.userId ?: "" }
        return UserProfile(
            userId = resolvedUserId,
            name = name,
            age = age,
            appVersion = "0.1.0",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE ?: "",
            firstInstall = firstInstallTime,
            lastSeen = now,
            betaUser = true,
            premium = false,
            pendingSync = true,
            lastSyncAttempt = 0L
        )
    }
}
