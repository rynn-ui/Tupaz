package com.tupaz.service

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ThermalMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStatusChanged: (isPaused: Boolean, statusName: String) -> Unit
) {
    companion object {
        private const val TAG = "ThermalMonitor"
        private const val COOLDOWN_MS = 5000L // 5-second cooldown period to prevent rapid pause/resume oscillation
    }

    private var powerManager: PowerManager? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    private var isCurrentlyPaused = false
    private var cooldownJob: Job? = null

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val listener = PowerManager.OnThermalStatusChangedListener { status ->
                    handleThermalStatusChanged(status)
                }
                thermalListener = listener
                powerManager?.addThermalStatusListener(listener)

                val initialStatus = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
                handleThermalStatusChanged(initialStatus)
                Log.i(TAG, "[Tupaz-Thermal] ThermalMonitor registered successfully. Initial status=$initialStatus")
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Thermal] Failed to register PowerManager thermal status listener", e)
            }
        } else {
            Log.i(TAG, "[Tupaz-Thermal] Thermal monitoring API not available (requires Android 10+ / API 29)")
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                thermalListener?.let { listener ->
                    powerManager?.removeThermalStatusListener(listener)
                }
                thermalListener = null
                cooldownJob?.cancel()
                cooldownJob = null
                Log.i(TAG, "[Tupaz-Thermal] ThermalMonitor stopped and thermal listener unregistered.")
            } catch (e: Exception) {
                Log.w(TAG, "[Tupaz-Thermal] Failed to unregister thermal status listener", e)
            }
        }
    }

    private fun handleThermalStatusChanged(status: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val statusName = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
            PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
            PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
            PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
            else -> "UNKNOWN ($status)"
        }

        // Policy: NONE/LIGHT/MODERATE -> continue. SEVERE/CRITICAL/EMERGENCY/SHUTDOWN -> pause.
        val isSevereOrHigher = status >= PowerManager.THERMAL_STATUS_SEVERE

        if (isSevereOrHigher) {
            // Cancel any pending cooldown resume job and pause immediately
            cooldownJob?.cancel()
            cooldownJob = null

            if (!isCurrentlyPaused) {
                isCurrentlyPaused = true
                Log.w(TAG, "[Tupaz-Thermal] Thermal status $statusName ($status) >= SEVERE -> PAUSING processing")
                onStatusChanged(true, statusName)
            } else {
                onStatusChanged(true, statusName)
            }
        } else {
            // Thermal status returned to safe level (NONE, LIGHT, MODERATE)
            if (isCurrentlyPaused) {
                if (cooldownJob?.isActive != true) {
                    Log.i(TAG, "[Tupaz-Thermal] Thermal status $statusName safe. Starting $COOLDOWN_MS ms cooldown before resuming...")
                    cooldownJob = scope.launch(Dispatchers.Default) {
                        delay(COOLDOWN_MS)
                        isCurrentlyPaused = false
                        Log.i(TAG, "[Tupaz-Thermal] Cooldown complete -> RESUMING processing")
                        onStatusChanged(false, statusName)
                    }
                }
            } else {
                onStatusChanged(false, statusName)
            }
        }
    }
}
