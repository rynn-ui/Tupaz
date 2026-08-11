package com.tupaz.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.tupaz.data.profile.UserProfile
import kotlinx.coroutines.tasks.await

/**
 * Manages Firestore interactions for users/{uid} documents.
 */
class FirebaseUserRepository(private val context: Context) {

    companion object {
        private const val TAG = "[Tupaz-Firebase]"
        const val USERS_COLLECTION = "users"
        const val CONFIG_COLLECTION = "config"
        const val APP_CONFIG_DOC = "app"
    }

    private val db: FirebaseFirestore?
        get() = try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Firestore unavailable or uninitialized: ${e.javaClass.name}: ${e.message}", e)
            null
        }

    suspend fun syncUserProfile(profile: UserProfile, uid: String, isFirstCreation: Boolean = false): Boolean {
        val firestore = db ?: return false
        Log.i(TAG, "Writing users/$uid")
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(uid)

            val data = mutableMapOf<String, Any>(
                "userId" to uid,
                "name" to profile.name,
                "age" to profile.age,
                "appVersion" to profile.appVersion,
                "deviceModel" to profile.deviceModel,
                "androidVersion" to profile.androidVersion,
                "lastSeen" to FieldValue.serverTimestamp(),
                "betaUser" to profile.betaUser,
                "premium" to profile.premium
            )

            if (isFirstCreation) {
                data["firstInstall"] = FieldValue.serverTimestamp()
            } else if (profile.firstInstall > 0L) {
                data["firstInstall"] = profile.firstInstall
            }

            userRef.set(data, SetOptions.merge()).await()
            Log.i(TAG, "Firestore write SUCCESS for users/$uid")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Firestore write FAILED for users/$uid: ${e.javaClass.name}: ${e.message}", e)
            false
        }
    }
}
