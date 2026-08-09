package com.tupaz.ui.result

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import com.tupaz.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
@Composable
fun ComparisonPlayer(
    originalUri: Uri?,
    enhancedUri: Uri?,
    dividerFraction: Float,
    isPlaying: Boolean,
    isMuted: Boolean,
    currentPositionMs: Long,
    originalLabel: String = "Original (1080p)",
    enhancedLabel: String = "AI Enhanced (4K)",
    onDividerFractionChange: (Float) -> Unit,
    onPositionUpdate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Master Player (Enhanced Video Clock)
    val playerEnhanced = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    // Slave Follower Player (Original Video)
    val playerOriginal = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    var isOriginalError by remember { mutableStateOf(false) }
    var isEnhancedError by remember { mutableStateOf(false) }

    LaunchedEffect(originalUri) {
        if (originalUri != null) {
            isOriginalError = false
            val currentMedia = playerOriginal.currentMediaItem
            val newMedia = MediaItem.fromUri(originalUri)
            if (currentMedia?.localConfiguration?.uri != originalUri) {
                playerOriginal.setMediaItem(newMedia)
                playerOriginal.prepare()
            }
        }
    }

    LaunchedEffect(enhancedUri, originalUri) {
        val uriToPlay = enhancedUri ?: originalUri
        if (uriToPlay != null) {
            isEnhancedError = false
            val currentMedia = playerEnhanced.currentMediaItem
            val newMedia = MediaItem.fromUri(uriToPlay)
            if (currentMedia?.localConfiguration?.uri != uriToPlay) {
                playerEnhanced.setMediaItem(newMedia)
                playerEnhanced.prepare()
            }
        }
    }

    // Master-Slave Play/Pause & Speed Synchronization
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val masterPos = playerEnhanced.currentPosition
            playerOriginal.playbackParameters = playerEnhanced.playbackParameters
            playerOriginal.seekTo(masterPos)
            playerEnhanced.play()
            playerOriginal.play()
        } else {
            playerEnhanced.pause()
            playerOriginal.pause()
        }
    }

    // Volume & Mute control
    LaunchedEffect(isMuted) {
        val vol = if (isMuted) 0f else 1f
        playerOriginal.volume = 0f
        playerEnhanced.volume = vol
    }

    // Master Seek Synchronization
    LaunchedEffect(currentPositionMs) {
        val masterDrift = kotlin.math.abs(playerEnhanced.currentPosition - currentPositionMs)
        if (masterDrift > 10) {
            playerEnhanced.seekTo(currentPositionMs)
            playerOriginal.seekTo(currentPositionMs)
        }
    }

    // Continuous Frame & 10ms Drift Resync Loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val masterPos = playerEnhanced.currentPosition
            val slavePos = playerOriginal.currentPosition
            val driftMs = kotlin.math.abs(masterPos - slavePos)

            if (driftMs > 10) {
                playerOriginal.seekTo(masterPos)
            }

            onPositionUpdate(masterPos)
            kotlinx.coroutines.delay(16) // ~60 FPS position check loop
        }
    }

    DisposableEffect(playerOriginal) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ComparisonPlayer", "Original player error: ${error.message}", error)
                isOriginalError = true
            }
        }
        playerOriginal.addListener(listener)
        onDispose {
            playerOriginal.removeListener(listener)
        }
    }

    // Auto-loop & Discontinuity Listener
    DisposableEffect(playerEnhanced) {
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                playerOriginal.seekTo(newPosition.positionMs)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerOriginal.play()
                } else {
                    playerOriginal.pause()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ComparisonPlayer", "Enhanced player error: ${error.message}", error)
                isEnhancedError = true
            }
        }
        playerEnhanced.addListener(listener)
        onDispose {
            playerEnhanced.removeListener(listener)
            playerOriginal.release()
            playerEnhanced.release()
        }
    }

    SplitComparisonView(
        dividerFraction = dividerFraction,
        originalContent = {
            if (originalUri != null && !isOriginalError) {
                AndroidView(
                    factory = {
                        val playerView = LayoutInflater.from(it).inflate(R.layout.texture_player_view, null) as androidx.media3.ui.PlayerView
                        playerView.player = playerOriginal
                        playerView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF2D1B4E), Color(0xFF1E2638))))
                    val mountainPath = Path().apply {
                        moveTo(0f, h * 0.7f)
                        lineTo(w * 0.25f, h * 0.4f)
                        lineTo(w * 0.5f, h * 0.65f)
                        lineTo(w * 0.75f, h * 0.35f)
                        lineTo(w, h * 0.75f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = mountainPath, color = Color(0xFF151928))
                }
            }
        },
        enhancedContent = {
            val uriToDisplay = enhancedUri ?: originalUri
            if (uriToDisplay != null && !isEnhancedError) {
                AndroidView(
                    factory = {
                        val playerView = LayoutInflater.from(it).inflate(R.layout.texture_player_view, null) as androidx.media3.ui.PlayerView
                        playerView.player = playerEnhanced
                        playerView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.005f
                            scaleY = 1.005f
                        }
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF6D28D9), Color(0xFF3B0764))))
                    val sharpMountain = Path().apply {
                        moveTo(0f, h * 0.7f)
                        lineTo(w * 0.25f, h * 0.38f)
                        lineTo(w * 0.5f, h * 0.62f)
                        lineTo(w * 0.75f, h * 0.35f)
                        lineTo(w, h * 0.72f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = sharpMountain, color = Color(0xFF180A30))
                }
            }
        },
        originalLabel = originalLabel,
        enhancedLabel = enhancedLabel,
        onDividerFractionChange = onDividerFractionChange,
        modifier = modifier
    )
}
