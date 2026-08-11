package com.tupaz.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tupaz.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Anonymous Authentication safely without blocking application execution.
 */
class FirebaseAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "[Tupaz-Firebase]"
    }

    private val auth: FirebaseAuth?
        get() = try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "FirebaseApp initialized")
            }
            val instance = FirebaseAuth.getInstance()
            val app = FirebaseApp.getInstance()
            Log.d(TAG, "Application ID: ${BuildConfig.APPLICATION_ID}")
            Log.d(TAG, "Firebase Project ID: ${app.options.projectId}")
            Log.d(TAG, "Firebase App ID: ${app.options.applicationId}")
            Log.d(TAG, "Current User UID: ${instance.currentUser?.uid ?: "none"}")
            instance
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase Auth initialization error: ${e.javaClass.name}: ${e.message}", e)
            null
        }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            auth?.currentUser
        } catch (e: Throwable) {
            Log.e(TAG, "getCurrentUser error: ${e.javaClass.name}: ${e.message}", e)
            null
        }
    }

    fun getUid(): String? {
        return getCurrentUser()?.uid
    }

    suspend fun getOrSignInAnonymousUser(): String? {
        val existingUser = getCurrentUser()
        if (existingUser != null) {
            Log.i(TAG, "Reusing existing Firebase anonymous user: ${existingUser.uid}")
            return existingUser.uid
        }

        Log.i(TAG, "Starting anonymous authentication...")
        val firebaseAuth = auth ?: return null
        return try {
            val result = firebaseAuth.signInAnonymously().await()
            val user = result.user
            val uid = user?.uid
            if (!uid.isNullOrEmpty()) {
                Log.i(TAG, "Anonymous authentication SUCCESS uid=$uid")
            } else {
                Log.w(TAG, "Anonymous authentication returned null UID")
            }
            uid
        } catch (e: Throwable) {
            Log.e(TAG, "Anonymous authentication FAILED: ${e.javaClass.name}: ${e.message}", e)
            null
        }
    }
}
