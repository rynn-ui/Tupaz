package com.tupaz.data.processing

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.tupaz.service.VideoProcessingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

object ProcessingManager {
    private const val TAG = "ProcessingManager"
    private const val PREFS_NAME = "tupaz_processing_prefs"
    private const val KEY_STATE_JSON = "processing_state_json"

    private val _state = MutableStateFlow(ProcessingState())
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private var isInitialized = false
    private var lastPersistTimeMs = 0L

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_STATE_JSON, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val loadedState = stateFromJson(jsonStr!!)
                if (loadedState.status == ProcessingStatus.PROCESSING) {
                    // Process was terminated while processing was active
                    val interruptedState = loadedState.copy(
                        status = ProcessingStatus.FAILED,
                        errorMessage = "Video processing was interrupted because the application process was terminated by the system."
                    )
                    _state.value = interruptedState
                    persistState(context, interruptedState)
                    Log.w(TAG, "[Tupaz-Manager] Detected interrupted job from process death -> marked FAILED")
                } else {
                    _state.value = loadedState
                    Log.i(TAG, "[Tupaz-Manager] Restored persistent state: status=${loadedState.status}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Tupaz-Manager] Error loading persistent state", e)
            }
        }
    }

    fun startProcessing(context: Context, config: ProcessingJobConfig) {
        val now = System.currentTimeMillis()
        val newState = ProcessingState(
            status = ProcessingStatus.PROCESSING,
            config = config,
            totalFrames = 0,
            processedFrames = 0,
            progressPercentage = 0,
            currentStage = "Initializing Foreground Service...",
            elapsedTime = "00:00",
            remainingTime = config.estimatedProcessingTime,
            startTimeMs = now
        )
        _state.value = newState
        persistState(context, newState)

        val serviceIntent = Intent(context, VideoProcessingService::class.java).apply {
            action = VideoProcessingService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.i(TAG, "[Tupaz-Manager] Started VideoProcessingService foreground service")
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Manager] Failed to start foreground service", e)
            failProcessing(context, "Failed to start foreground service: ${e.message}")
        }
    }

    fun updateProgress(context: Context, currentFrame: Int, totalFrames: Int, stage: String) {
        val currentState = _state.value
        if (currentState.status != ProcessingStatus.PROCESSING) return

        val startTime = currentState.startTimeMs
        val elapsedMs = if (startTime > 0) (System.currentTimeMillis() - startTime).coerceAtLeast(0L) else 0L
        val totalSecs = (elapsedMs / 1000).toInt()
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val elapsedTimeStr = if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }

        val rawProgress = if (totalFrames > 0) {
            ((currentFrame.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
        } else 0

        val fps = if (currentFrame > 0 && elapsedMs > 0) {
            (currentFrame.toFloat() / (elapsedMs / 1000.0f))
        } else 0f

        val remainingTimeStr = if (currentFrame > 0 && totalFrames > 0 && elapsedMs > 0) {
            val avgMsPerFrame = elapsedMs.toDouble() / currentFrame
            val remainingFrames = (totalFrames - currentFrame).coerceAtLeast(0)
            val remainingSec = ((remainingFrames * avgMsPerFrame) / 1000.0).toInt().coerceAtLeast(0)
            val rHrs = remainingSec / 3600
            val rMins = (remainingSec % 3600) / 60
            val rSecs = remainingSec % 60
            if (rHrs > 0) {
                String.format("~%02d:%02d:%02d", rHrs, rMins, rSecs)
            } else {
                String.format("~%02d:%02d", rMins, rSecs)
            }
        } else {
            currentState.config.estimatedProcessingTime
        }

        _state.update {
            it.copy(
                totalFrames = totalFrames,
                processedFrames = currentFrame,
                progressPercentage = rawProgress,
                currentStage = stage,
                elapsedTime = elapsedTimeStr,
                remainingTime = remainingTimeStr,
                processingFps = fps
            )
        }

        // Throttle disk persistence to avoid excessive disk I/O (at most once every 2 seconds or at key milestones)
        val now = System.currentTimeMillis()
        if (now - lastPersistTimeMs > 2000 || rawProgress == 100) {
            lastPersistTimeMs = now
            persistState(context, _state.value)
        }
    }

    fun completeProcessing(context: Context, outputUri: Uri, realProcessingTime: String, realOutputSize: String) {
        val currentState = _state.value
        val completedState = currentState.copy(
            status = ProcessingStatus.COMPLETED,
            processedFrames = currentState.totalFrames,
            progressPercentage = 100,
            currentStage = "Processing Complete",
            remainingTime = "00:00",
            outputUriString = outputUri.toString(),
            realProcessingTime = realProcessingTime,
            realOutputSize = realOutputSize
        )
        _state.value = completedState
        persistState(context, completedState)
        Log.i(TAG, "[Tupaz-Manager] Job completed successfully: outputUri=$outputUri")
    }

    fun failProcessing(context: Context, errorMessage: String) {
        val currentState = _state.value
        val failedState = currentState.copy(
            status = ProcessingStatus.FAILED,
            errorMessage = errorMessage
        )
        _state.value = failedState
        persistState(context, failedState)
        Log.e(TAG, "[Tupaz-Manager] Job failed: $errorMessage")
    }

    fun cancelProcessing(context: Context) {
        Log.i(TAG, "[Tupaz-Manager] Cancelling processing job...")
        val serviceIntent = Intent(context, VideoProcessingService::class.java).apply {
            action = VideoProcessingService.ACTION_CANCEL
        }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Manager] Error sending cancel intent to service", e)
        }

        val cancelledState = _state.value.copy(
            status = ProcessingStatus.CANCELLED,
            currentStage = "Cancelled by user",
            errorMessage = "Processing was cancelled by user."
        )
        _state.value = cancelledState
        persistState(context, cancelledState)
    }

    fun setThermalPause(context: Context, isPaused: Boolean, statusName: String) {
        val currentState = _state.value
        val updatedState = currentState.copy(
            isThermallyPaused = isPaused,
            thermalStatusName = statusName
        )
        _state.value = updatedState
        persistState(context, updatedState)
        Log.i(TAG, "[Tupaz-Manager] Thermal state updated: isPaused=$isPaused, statusName=$statusName")
    }

    fun reset(context: Context) {
        val newState = ProcessingState()
        _state.value = newState
        persistState(context, newState)
    }

    private fun persistState(context: Context, state: ProcessingState) {
        try {
            val json = stateToJson(state)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STATE_JSON, json)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Manager] Failed to persist state", e)
        }
    }

    private fun stateToJson(state: ProcessingState): String {
        val obj = JSONObject()
        obj.put("status", state.status.name)
        obj.put("totalFrames", state.totalFrames)
        obj.put("processedFrames", state.processedFrames)
        obj.put("progressPercentage", state.progressPercentage)
        obj.put("currentStage", state.currentStage)
        obj.put("elapsedTime", state.elapsedTime)
        obj.put("remainingTime", state.remainingTime)
        obj.put("outputUriString", state.outputUriString ?: "")
        obj.put("errorMessage", state.errorMessage ?: "")
        obj.put("startTimeMs", state.startTimeMs)
        obj.put("realProcessingTime", state.realProcessingTime)
        obj.put("realOutputSize", state.realOutputSize)
        obj.put("isThermallyPaused", state.isThermallyPaused)
        obj.put("thermalStatusName", state.thermalStatusName)

        val cfgObj = JSONObject()
        cfgObj.put("fileName", state.config.fileName)
        cfgObj.put("inputUriString", state.config.inputUriString ?: "")
        cfgObj.put("targetWidth", state.config.targetWidth)
        cfgObj.put("targetHeight", state.config.targetHeight)
        cfgObj.put("origRes", state.config.origRes)
        cfgObj.put("enhRes", state.config.enhRes)
        cfgObj.put("resTransition", state.config.resTransition)
        cfgObj.put("resDetail", state.config.resDetail)
        cfgObj.put("modelName", state.config.modelName)
        cfgObj.put("modelScale", state.config.modelScale)
        cfgObj.put("enhancementMode", state.config.enhancementMode)
        cfgObj.put("enhancementSub", state.config.enhancementSub)
        cfgObj.put("fpsLabel", state.config.fpsLabel)
        cfgObj.put("durationLabel", state.config.durationLabel)
        cfgObj.put("estimatedProcessingTime", state.config.estimatedProcessingTime)
        cfgObj.put("estimatedOutputSize", state.config.estimatedOutputSize)

        obj.put("config", cfgObj)
        return obj.toString()
    }

    private fun stateFromJson(jsonStr: String): ProcessingState {
        val obj = JSONObject(jsonStr)
        val statusStr = obj.optString("status", ProcessingStatus.IDLE.name)
        val status = try { ProcessingStatus.valueOf(statusStr) } catch (_: Exception) { ProcessingStatus.IDLE }

        val cfgObj = obj.optJSONObject("config") ?: JSONObject()
        val config = ProcessingJobConfig(
            fileName = cfgObj.optString("fileName", ""),
            inputUriString = cfgObj.optString("inputUriString", "").takeIf { it.isNotEmpty() },
            targetWidth = cfgObj.optInt("targetWidth", 1280),
            targetHeight = cfgObj.optInt("targetHeight", 720),
            origRes = cfgObj.optString("origRes", ""),
            enhRes = cfgObj.optString("enhRes", ""),
            resTransition = cfgObj.optString("resTransition", ""),
            resDetail = cfgObj.optString("resDetail", ""),
            modelName = cfgObj.optString("modelName", "realesr-animevideov3-x2"),
            modelScale = cfgObj.optString("modelScale", "2x Scale"),
            enhancementMode = cfgObj.optString("enhancementMode", "Auto (Balanced)"),
            enhancementSub = cfgObj.optString("enhancementSub", "Denoise · Sharpen · Recover"),
            fpsLabel = cfgObj.optString("fpsLabel", "30 FPS"),
            durationLabel = cfgObj.optString("durationLabel", "00:00"),
            estimatedProcessingTime = cfgObj.optString("estimatedProcessingTime", "Calculating estimate…"),
            estimatedOutputSize = cfgObj.optString("estimatedOutputSize", "Calculating estimate…")
        )

        return ProcessingState(
            status = status,
            config = config,
            totalFrames = obj.optInt("totalFrames", 0),
            processedFrames = obj.optInt("processedFrames", 0),
            progressPercentage = obj.optInt("progressPercentage", 0),
            currentStage = obj.optString("currentStage", ""),
            elapsedTime = obj.optString("elapsedTime", "00:00"),
            remainingTime = obj.optString("remainingTime", "Calculating estimate…"),
            outputUriString = obj.optString("outputUriString", "").takeIf { it.isNotEmpty() },
            errorMessage = obj.optString("errorMessage", "").takeIf { it.isNotEmpty() },
            startTimeMs = obj.optLong("startTimeMs", 0L),
            realProcessingTime = obj.optString("realProcessingTime", ""),
            realOutputSize = obj.optString("realOutputSize", ""),
            isThermallyPaused = obj.optBoolean("isThermallyPaused", false),
            thermalStatusName = obj.optString("thermalStatusName", "NORMAL")
        )
    }
}
