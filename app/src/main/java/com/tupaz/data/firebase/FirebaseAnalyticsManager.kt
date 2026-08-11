package com.tupaz.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Manages minimal non-PII analytics logging.
 */
class FirebaseAnalyticsManager(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseAnalyticsManager"
        const val EVENT_PROFILE_SETUP_STARTED = "profile_setup_started"
        const val EVENT_PROFILE_SETUP_COMPLETED = "profile_setup_completed"
        const val EVENT_PROFILE_SETUP_SYNC_FAILED = "profile_setup_sync_failed"
    }

    private val analytics: FirebaseAnalytics?
        get() = try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAnalytics.getInstance(context)
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Analytics unavailable or uninitialized: ${e.message}")
            null
        }

    fun logProfileSetupStarted() {
        try {
            analytics?.logEvent(EVENT_PROFILE_SETUP_STARTED, null)
        } catch (_: Throwable) {}
    }

    fun logProfileSetupCompleted() {
        try {
            analytics?.logEvent(EVENT_PROFILE_SETUP_COMPLETED, null)
        } catch (_: Throwable) {}
    }

    fun logProfileSetupSyncFailed() {
        try {
            analytics?.logEvent(EVENT_PROFILE_SETUP_SYNC_FAILED, null)
        } catch (_: Throwable) {}
    }
}
