package com.tupaz.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tupaz.MainActivity
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import com.tupaz.pipeline.VideoUpscaler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class VideoProcessingService : Service() {

    companion object {
        private const val TAG = "VideoProcessingService"
        const val ACTION_START = "com.tupaz.service.ACTION_START"
        const val ACTION_CANCEL = "com.tupaz.service.ACTION_CANCEL"

        private const val CHANNEL_PROCESSING_ID = "tupaz_processing_channel"
        private const val CHANNEL_RESULT_ID = "tupaz_result_channel"
        private const val NOTIFICATION_ID = 1001
        private const val RESULT_NOTIFICATION_ID = 1002
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var processingJob: Job? = null
    private var lastNotificationUpdateMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Log.i(TAG, "[Tupaz-Service] VideoProcessingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "[Tupaz-Service] onStartCommand action=$action")

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification()
                executeProcessingJob()
            }
            ACTION_CANCEL -> {
                cancelProcessingJob()
            }
            else -> {
                Log.w(TAG, "[Tupaz-Service] Unknown action: $action")
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val processingChannel = NotificationChannel(
                CHANNEL_PROCESSING_ID,
                "Tupaz Video Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing progress of video AI upscaling"
                setShowBadge(false)
            }

            val resultChannel = NotificationChannel(
                CHANNEL_RESULT_ID,
                "Tupaz Enhancement Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for completed or failed video enhancements"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(processingChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildOngoingNotification(
            progressPercent = 0,
            currentFrame = 0,
            totalFrames = 0,
            stage = "Initializing AI Pipeline...",
            eta = "Calculating estimate…"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "[Tupaz-Service] Service started in foreground")
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Service] Failed startForeground call", e)
        }
    }

    private var thermalMonitor: ThermalMonitor? = null

    private fun startThermalMonitor() {
        if (thermalMonitor == null) {
            thermalMonitor = ThermalMonitor(
                context = applicationContext,
                scope = serviceScope,
                onStatusChanged = { isPaused, statusName ->
                    ProcessingManager.setThermalPause(applicationContext, isPaused, statusName)
                    val pState = ProcessingManager.state.value
                    updateNotification(
                        progressPercent = pState.progressPercentage,
                        currentFrame = pState.processedFrames,
                        totalFrames = pState.totalFrames,
                        stage = pState.currentStage,
                        eta = pState.remainingTime
                    )
                }
            )
            thermalMonitor?.start()
        }
    }

    private fun stopThermalMonitor() {
        thermalMonitor?.stop()
        thermalMonitor = null
    }

    private fun executeProcessingJob() {
        if (processingJob?.isActive == true) {
            Log.w(TAG, "[Tupaz-Service] Processing job already active, ignoring duplicate start")
            return
        }

        val config = ProcessingManager.state.value.config
        val inputUri = config.inputUri
        if (inputUri == null) {
            Log.e(TAG, "[Tupaz-Service] Input URI is null, failing job")
            ProcessingManager.failProcessing(this, "Input video file or URI not found.")
            stopForegroundAndSelf()
            return
        }

        startThermalMonitor()

        processingJob = serviceScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Log.i(TAG, "[Tupaz-Service] Starting VideoUpscaler.processAndUpscaleVideo for ${config.fileName}")

                val outputUri = VideoUpscaler.processAndUpscaleVideo(
                    context = applicationContext,
                    inputUri = inputUri,
                    targetWidth = config.targetWidth,
                    targetHeight = config.targetHeight,
                    modelName = config.modelName,
                    scaleFactor = config.modelScale,
                    onProgress = { curFrame, totFrames, stage ->
                        ProcessingManager.updateProgress(applicationContext, curFrame, totFrames, stage)

                        val now = System.currentTimeMillis()
                        if (now - lastNotificationUpdateMs > 800 || curFrame == totFrames) {
                            lastNotificationUpdateMs = now
                            val currentState = ProcessingManager.state.value
                            updateNotification(
                                progressPercent = currentState.progressPercentage,
                                currentFrame = curFrame,
                                totalFrames = totFrames,
                                stage = stage,
                                eta = currentState.remainingTime
                            )
                        }
                    },
                    isPausedCheck = { ProcessingManager.state.value.isThermallyPaused }
                )

                val elapsedMs = System.currentTimeMillis() - startTime
                val totalSecs = (elapsedMs / 1000).toInt()
                val hrs = totalSecs / 3600
                val mins = (totalSecs % 3600) / 60
                val secs = totalSecs % 60
                val realProcessingTimeStr = if (hrs > 0) {
                    String.format("%02d:%02d:%02d", hrs, mins, secs)
                } else {
                    String.format("%02d:%02d", mins, secs)
                }

                val finalOutputUri = outputUri ?: throw IllegalStateException("Pipeline output URI was null")
                val realOutputSizeStr = if (finalOutputUri.scheme == "file") {
                    val file = File(finalOutputUri.path ?: "")
                    if (file.exists() && file.length() > 0) {
                        val sizeMb = file.length().toDouble() / (1024 * 1024)
                        if (sizeMb >= 1000) String.format("%.2f GB", sizeMb / 1024)
                        else String.format("%.1f MB", sizeMb)
                    } else config.estimatedOutputSize
                } else config.estimatedOutputSize

                ProcessingManager.completeProcessing(
                    context = applicationContext,
                    outputUri = finalOutputUri,
                    realProcessingTime = realProcessingTimeStr,
                    realOutputSize = realOutputSizeStr
                )

                showCompletionNotification(config.fileName)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.i(TAG, "[Tupaz-Service] Processing job coroutine was cancelled")
                ProcessingManager.cancelProcessing(applicationContext)
            } catch (e: Throwable) {
                Log.e(TAG, "[Tupaz-Service] Processing error", e)
                val errorMsg = e.message ?: "Unknown enhancement failure"
                ProcessingManager.failProcessing(applicationContext, errorMsg)
                showFailureNotification(config.fileName, errorMsg)
            } finally {
                stopForegroundAndSelf()
            }
        }
    }

    private fun cancelProcessingJob() {
        Log.i(TAG, "[Tupaz-Service] Cancelling processing job...")
        processingJob?.cancel()
        processingJob = null
        ProcessingManager.cancelProcessing(applicationContext)
        stopForegroundAndSelf()
    }

    private fun updateNotification(
        progressPercent: Int,
        currentFrame: Int,
        totalFrames: Int,
        stage: String,
        eta: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildOngoingNotification(progressPercent, currentFrame, totalFrames, stage, eta)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildOngoingNotification(
        progressPercent: Int,
        currentFrame: Int,
        totalFrames: Int,
        stage: String,
        eta: String
    ): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "result")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, VideoProcessingService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pState = ProcessingManager.state.value
        val isThermallyPaused = pState.isThermallyPaused

        val title = if (isThermallyPaused) {
            "🌡️ Tupaz paused — device is too warm"
        } else {
            "Tupaz Enhancing Video"
        }

        val textContent = if (isThermallyPaused) {
            "Processing will resume when temperature is safe."
        } else if (totalFrames > 0) {
            "Frame $currentFrame / $totalFrames • $progressPercent% • ETA $eta"
        } else {
            "$stage • ETA $eta"
        }

        return NotificationCompat.Builder(this, CHANNEL_PROCESSING_ID)
            .setContentTitle(title)
            .setContentText(textContent)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .setProgress(100, progressPercent.coerceIn(0, 100), totalFrames <= 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCompletionNotification(fileName: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "result")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_RESULT_ID)
            .setContentTitle("Tupaz Enhancement Complete")
            .setContentText("Your enhanced video '$fileName' is ready.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification(fileName: String, errorMsg: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "result")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 3, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_RESULT_ID)
            .setContentTitle("Tupaz Enhancement Failed")
            .setContentText("Failed to process '$fileName': $errorMsg")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    private fun stopForegroundAndSelf() {
        stopThermalMonitor()
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Tupaz-Service] Error stopping foreground", e)
        }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val status = ProcessingManager.state.value.status
        Log.i(TAG, "[Tupaz-Service] onTaskRemoved called (status=$status). User swiped app away from Recents -> cancelling all processing.")
        cancelProcessingJob()
    }

    override fun onDestroy() {
        stopThermalMonitor()
        serviceScope.cancel()
        Log.i(TAG, "[Tupaz-Service] VideoProcessingService destroyed")
        super.onDestroy()
    }
}
