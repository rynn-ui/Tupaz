package com.tupaz.data.firebase

import android.content.Context
import android.util.Log
import com.tupaz.data.profile.UserProfile
import com.tupaz.data.profile.UserProfileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates local-first profile saving and non-blocking background Firebase synchronization.
 */
class FirebaseSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "[Tupaz-Firebase]"
    }

    private val profileStorage = UserProfileStorage(context)
    private val authManager = FirebaseAuthManager(context)
    private val userRepository = FirebaseUserRepository(context)
    private val analyticsManager = FirebaseAnalyticsManager(context)

    /**
     * Saves profile locally FIRST and marks onboarding complete.
     * Synchronous local-only operation that never touches the network.
     */
    fun saveProfileLocally(name: String, age: Int): UserProfile {
        val existingProfile = profileStorage.getUserProfile()
        val existingUid = authManager.getUid() ?: existingProfile?.userId ?: ""

        val localProfile = profileStorage.createDefaultProfile(
            name = name.trim(),
            age = age,
            userId = existingUid
        )

        profileStorage.saveUserProfile(localProfile)
        profileStorage.setOnboardingCompleted(true)
        analyticsManager.logProfileSetupStarted()

        Log.i(TAG, "Profile saved locally for '${localProfile.name}' (${localProfile.age}). Onboarding marked complete.")
        return localProfile
    }

    /**
     * Performs background Firebase Auth + Firestore synchronization asynchronously.
     */
    suspend fun syncProfileInBackground(localProfile: UserProfile): UserProfile = withContext(Dispatchers.IO) {
        val existingProfile = profileStorage.getUserProfile()
        val isFirstCreation = (existingProfile?.userId.isNullOrEmpty())

        try {
            val uid = authManager.getOrSignInAnonymousUser()
            if (!uid.isNullOrEmpty()) {
                val updatedProfile = localProfile.copy(userId = uid)
                profileStorage.saveUserProfile(updatedProfile)

                val syncSuccess = userRepository.syncUserProfile(updatedProfile, uid, isFirstCreation = isFirstCreation)
                if (syncSuccess) {
                    profileStorage.setPendingSync(pending = false, lastAttemptTime = System.currentTimeMillis())
                    analyticsManager.logProfileSetupCompleted()
                    Log.i(TAG, "Profile sync completed SUCCESS for $uid (pendingSync=false)")
                    return@withContext updatedProfile.copy(pendingSync = false)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Background Firebase sync error: ${e.javaClass.name}: ${e.message}", e)
        }

        profileStorage.setPendingSync(pending = true, lastAttemptTime = System.currentTimeMillis())
        analyticsManager.logProfileSetupSyncFailed()
        Log.i(TAG, "Firestore write FAILED or offline -> pendingSync=true")
        return@withContext localProfile.copy(pendingSync = true)
    }

    /**
     * Saves profile locally FIRST, marks onboarding complete, and triggers async Firebase sync.
     */
    suspend fun saveAndSyncProfile(name: String, age: Int): UserProfile = withContext(Dispatchers.IO) {
        val localProfile = saveProfileLocally(name, age)
        return@withContext syncProfileInBackground(localProfile)
    }

    /**
     * Retries sync in background if pendingSync is true.
     */
    suspend fun retryPendingSyncIfNeeded() = withContext(Dispatchers.IO) {
        if (!profileStorage.isPendingSync()) return@withContext

        val localProfile = profileStorage.getUserProfile() ?: return@withContext
        val now = System.currentTimeMillis()

        // Throttle retries to at most once per 60 seconds
        if (now - localProfile.lastSyncAttempt < 60_000L && localProfile.lastSyncAttempt > 0L) {
            return@withContext
        }

        Log.i(TAG, "Attempting background retry sync for '${localProfile.name}'...")
        try {
            val uid = authManager.getOrSignInAnonymousUser() ?: localProfile.userId
            if (uid.isNotEmpty()) {
                val profileToSync = localProfile.copy(userId = uid)
                val syncSuccess = userRepository.syncUserProfile(profileToSync, uid, isFirstCreation = false)
                if (syncSuccess) {
                    profileStorage.saveUserProfile(profileToSync.copy(pendingSync = false, lastSyncAttempt = now))
                    profileStorage.setPendingSync(pending = false, lastAttemptTime = now)
                    Log.i(TAG, "Background retry sync succeeded for $uid (pendingSync=false)")
                } else {
                    profileStorage.setPendingSync(pending = true, lastAttemptTime = now)
                    Log.i(TAG, "Background retry sync failed -> pendingSync=true")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Background retry sync exception: ${e.javaClass.name}: ${e.message}", e)
            profileStorage.setPendingSync(pending = true, lastAttemptTime = now)
        }
    }
}
