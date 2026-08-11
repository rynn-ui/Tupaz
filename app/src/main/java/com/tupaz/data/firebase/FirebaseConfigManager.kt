package com.tupaz.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.tupaz.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class AppConfigState {
    data object Loading : AppConfigState()
    data class BetaClosed(val message: String) : AppConfigState()
    data class Maintenance(val message: String) : AppConfigState()
    data class UpdateRequired(val message: String, val latestVersion: String, val updateUrl: String) : AppConfigState()
    data class UpdateAvailable(val message: String, val latestVersion: String, val updateUrl: String) : AppConfigState()
    data object Ready : AppConfigState()
}

class FirebaseConfigManager(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    companion object {
        private const val TAG = "[Tupaz-FirebaseConfig]"
        private const val PREFS_NAME = "tupaz_config_prefs"
        private const val KEY_CONFIG_JSON = "cached_config_json"

        /**
         * Compares two semantic version strings (e.g. "0.10.0" > "0.2.0").
         * Returns true if current is strictly lower than latest.
         */
        fun isVersionLower(current: String, latest: String): Boolean {
            val currentParts = parseVersionParts(current)
            val latestParts = parseVersionParts(latest)

            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (c < l) return true
                if (c > l) return false
            }
            return false
        }

        private fun parseVersionParts(version: String): List<Int> {
            return version.split("-").firstOrNull()
                ?.split(".")
                ?.mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
                ?: emptyList()
        }

        /**
         * Evaluates startup state based on strict priority:
         * 1. BETA CLOSED (betaEnabled == false)
         * 2. MAINTENANCE (maintenanceMode == true)
         * 3. UPDATE (currentVersion < latestVersion) -> UpdateRequired if forceUpdate else UpdateAvailable
         * 4. NORMAL APP (Ready)
         */
        fun evaluateState(config: AppConfig, currentAppVersion: String = BuildConfig.VERSION_NAME): AppConfigState {
            if (!config.betaEnabled) {
                return AppConfigState.BetaClosed(config.betaMessage)
            }
            if (config.maintenanceMode) {
                return AppConfigState.Maintenance(config.maintenanceMessage)
            }
            if (isVersionLower(currentAppVersion, config.latestVersion)) {
                return if (config.forceUpdate) {
                    AppConfigState.UpdateRequired(config.updateMessage, config.latestVersion, config.updateUrl)
                } else {
                    AppConfigState.UpdateAvailable(config.updateMessage, config.latestVersion, config.updateUrl)
                }
            }
            return AppConfigState.Ready
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow<AppConfigState>(AppConfigState.Loading)
    val configState: StateFlow<AppConfigState> = _configState.asStateFlow()

    fun getCachedConfig(): AppConfig? {
        val jsonStr = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
        return try {
            json.decodeFromString<AppConfig>(jsonStr)
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveCachedConfig(config: AppConfig) {
        try {
            val str = json.encodeToString(config)
            prefs.edit().putString(KEY_CONFIG_JSON, str).apply()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to cache AppConfig locally: ${e.message}")
        }
    }

    suspend fun fetchAndEvaluateConfig(
        currentAppVersion: String = BuildConfig.VERSION_NAME,
        timeoutMs: Long = 3000L
    ): AppConfigState = withContext(Dispatchers.IO) {
        val cached = getCachedConfig()

        // Initial state from cache if available, else sensible default OPEN config (never locks out fresh launch)
        val initialConfig = cached ?: AppConfig(
            betaEnabled = true,
            maintenanceMode = false
        )
        val initialEvaluatedState = evaluateState(initialConfig, currentAppVersion)
        _configState.value = initialEvaluatedState

        Log.i(TAG, "Config fetch started")
        Log.i(TAG, "Application ID = ${BuildConfig.APPLICATION_ID}")

        val remoteConfig = withTimeoutOrNull(timeoutMs) {
            try {
                val app = if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)!!
                } else {
                    FirebaseApp.getInstance()
                }
                Log.i(TAG, "Project ID = ${app.options.projectId}")
                Log.i(TAG, "Config path = config/app")

                val firestore = FirebaseFirestore.getInstance()
                val doc = firestore.collection("config").document("app").get().await()
                val exists = doc.exists()
                Log.i(TAG, "Config document exists = $exists")

                if (exists) {
                    val betaEnabled = doc.getBoolean("betaEnabled") ?: true
                    val betaMsg = doc.getString("betaMessage") ?: "TUPAZ is currently closed for private beta testing."
                    val maintMode = doc.getBoolean("maintenanceMode") ?: false
                    val maintMsg = doc.getString("maintenanceMessage") ?: "TUPAZ engine is currently undergoing scheduled engine maintenance."
                    val latestVer = doc.getString("latestVersion") ?: currentAppVersion
                    val updateMsg = doc.getString("updateMessage") ?: "A new version of TUPAZ is available. Update now to get the latest features and improvements."
                    val forceUpd = doc.getBoolean("forceUpdate") ?: false
                    val updUrl = doc.getString("updateUrl") ?: ""

                    Log.i(TAG, "betaEnabled = $betaEnabled")
                    Log.i(TAG, "maintenanceMode = $maintMode")
                    Log.i(TAG, "latestVersion = $latestVer")
                    Log.i(TAG, "forceUpdate = $forceUpd")
                    Log.i(TAG, "updateUrl = '$updUrl'")

                    AppConfig(
                        betaEnabled = betaEnabled,
                        betaMessage = betaMsg,
                        maintenanceMode = maintMode,
                        maintenanceMessage = maintMsg,
                        latestVersion = latestVer,
                        updateMessage = updateMsg,
                        forceUpdate = forceUpd,
                        updateUrl = updUrl
                    )
                } else {
                    Log.w(TAG, "config/app document does not exist in Firestore")
                    null
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Config fetch FAILED: ${e.javaClass.name}: ${e.message}", e)
                null
            }
        }

        val finalConfig = remoteConfig ?: initialConfig
        if (remoteConfig != null) {
            saveCachedConfig(remoteConfig)
            Log.i(TAG, "Using fresh Firebase config")
        } else if (cached != null) {
            Log.i(TAG, "Using cached config")
        } else {
            Log.i(TAG, "Using default offline fallback config")
        }

        val finalState = evaluateState(finalConfig, currentAppVersion)
        _configState.value = finalState
        Log.i(TAG, "Startup gate = ${finalState.javaClass.simpleName}")
        return@withContext finalState
    }
}
