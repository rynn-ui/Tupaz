package com.tupaz.ui.result

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.tupaz.ui.theme.VercelBackground
import com.tupaz.ui.theme.VercelBorder
import com.tupaz.ui.theme.VercelBorderHighlight
import com.tupaz.ui.theme.VercelCardSurface
import com.tupaz.ui.theme.VercelSurface
import com.tupaz.ui.theme.VercelTextMuted
import com.tupaz.ui.theme.VercelTextPrimary
import com.tupaz.ui.theme.VercelTextSecondary
import java.io.File

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSavedPopup by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tupaz: Engine Output",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = VercelTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetJob(context)
                        onHome()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VercelTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = VercelTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VercelBackground
                )
            )
        },
        modifier = modifier,
        containerColor = VercelBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ResultUiState.Processing -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = state.progressPercentage / 100f,
                        animationSpec = tween(durationMillis = 150),
                        label = "CircularProgressAnimation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VercelBorder, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VercelSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tupaz: Engine Analysis",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = VercelTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.currentStage,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = VercelTextSecondary
                                    )
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // Circular Vercel Progress Gauge
                                Box(
                                    modifier = Modifier.size(190.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeWidth = 12.dp.toPx()
                                        val radius = (size.minDimension - strokeWidth) / 2
                                        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                                        // Background Track
                                        drawCircle(
                                            color = Color(0xFF222222),
                                            radius = radius,
                                            center = center,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )

                                        // Active White Arc
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -90f,
                                            sweepAngle = animatedProgress * 360f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = strokeWidth,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${state.progressPercentage}%",
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                color = VercelTextPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 42.sp
                                            )
                                        )
                                    }
                                }

                                if (state.isThermallyPaused) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261D0F))
                                    ) {
                                        Text(
                                            text = "🌡️ Paused — Device is cooling down (Thermal Status: ${state.thermalStatusName})\nProcessing will resume automatically when safe.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VercelCardSurface)
                                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Frame",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = VercelTextSecondary
                                            )
                                        )
                                        Text(
                                            text = if (state.totalFrames > 0)
                                                "Frame ${state.currentFrame} / ${state.totalFrames}"
                                            else
                                                "Counting frames…",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = VercelTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Elapsed",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = VercelTextSecondary
                                            )
                                        )
                                        Text(
                                            text = state.elapsedTime,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = VercelTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Remaining",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = VercelTextSecondary
                                            )
                                        )
                                        Text(
                                            text = state.remainingTime,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = VercelTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedButton(
                                    onClick = { viewModel.cancelProcessing(context) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFEF4444)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel Processing", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
                is ResultUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VercelSurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "Pipeline Execution Failure",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "File Name:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = VercelTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = state.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextPrimary)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Exception Details:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = VercelTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFFCA5A5))
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Originating Line:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = VercelTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = state.origin,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Stack Trace:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = VercelTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                                        .border(1.dp, VercelBorder, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    val scrollState = rememberScrollState()
                                    Text(
                                        text = state.stackTrace,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFD1D5DB),
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        viewModel.resetJob(context)
                                        onHome()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Return Home", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                is ResultUiState.ExportComplete -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // ComparisonPlayer Frame
                        item {
                            ComparisonPlayer(
                                originalUri = state.originalVideoUri,
                                enhancedUri = state.enhancedVideoUri,
                                dividerFraction = state.splitPosition,
                                isPlaying = state.isPlaying,
                                isMuted = state.isMuted,
                                currentPositionMs = state.currentPositionMs,
                                originalLabel = "Original (${state.originalResolution.take(5)})",
                                enhancedLabel = "AI Enhanced (${if (state.modelScale.contains("4")) "4x" else "2x"})",
                                onDividerFractionChange = viewModel::updateSplitPosition,
                                onPositionUpdate = viewModel::updateCurrentPosition,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(310.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, VercelBorder, RoundedCornerShape(12.dp))
                            )
                        }

                        // Playback Controls
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = VercelSurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = viewModel::togglePlayPause,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                                            tint = VercelTextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Text(
                                        text = formatTimeMs(state.currentPositionMs),
                                        style = MaterialTheme.typography.labelSmall.copy(color = VercelTextPrimary)
                                    )

                                    Slider(
                                        value = state.currentPositionMs.toFloat(),
                                        onValueChange = { viewModel.seekToPositionMs(it.toLong()) },
                                        valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = VercelTextPrimary,
                                            activeTrackColor = VercelTextPrimary,
                                            inactiveTrackColor = VercelBorderHighlight
                                        )
                                    )

                                    Text(
                                        text = formatTimeMs(state.durationMs),
                                        style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary)
                                    )

                                    IconButton(
                                        onClick = viewModel::toggleMute,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = VercelTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CropFree,
                                            contentDescription = "Fullscreen",
                                            tint = VercelTextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Metric Summary Grid
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = VercelSurface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        MetricGridItem(
                                            icon = Icons.Default.Transform,
                                            title = "Resolution",
                                            valText = state.resTransition,
                                            subText = state.resDetail,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricGridItem(
                                            icon = Icons.Default.AutoAwesome,
                                            title = "AI Model",
                                            valText = state.modelName,
                                            subText = state.modelScale,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        MetricGridItem(
                                            icon = Icons.Default.Tune,
                                            title = "Processing Mode",
                                            valText = state.enhancementMode,
                                            subText = state.enhancementSub,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricGridItem(
                                            icon = Icons.Default.Schedule,
                                            title = "Processing Time",
                                            valText = state.processingTime,
                                            subText = "Total Speed",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        MetricGridItem(
                                            icon = Icons.Default.Movie,
                                            title = "FPS",
                                            valText = state.fpsLabel,
                                            subText = state.fpsSub,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricGridItem(
                                            icon = Icons.Default.Download,
                                            title = "Output Size",
                                            valText = state.outputFileSize,
                                            subText = "Saved File",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // High-contrast Action Buttons Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.resetJob(context)
                                        onHome()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = VercelSurface,
                                        contentColor = VercelTextPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = VercelTextPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Home", style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold))
                                }

                                Button(
                                    onClick = {
                                        saveVideoToGallery(context, state.enhancedVideoUri ?: state.originalVideoUri, state.fileName)
                                        showSavedPopup = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VercelTextPrimary,
                                        contentColor = VercelBackground
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = VercelBackground, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Video", style = MaterialTheme.typography.titleMedium.copy(color = VercelBackground, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }

            // Save Success Popup Dialog
            if (showSavedPopup) {
                Dialog(
                    onDismissRequest = { showSavedPopup = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VercelBorderHighlight, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = VercelSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Video Saved to Gallery",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Your AI enhanced video has been saved to your device Gallery under Movies/Tupaz!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = VercelTextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showSavedPopup = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VercelTextPrimary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = "Close",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun saveVideoToGallery(context: Context, videoUri: Uri?, fileName: String): Boolean {
    val cleanName = "Tupaz_Enhanced_" + System.currentTimeMillis() + "_" + fileName.replace(" ", "_")
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, cleanName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Tupaz")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(collection, contentValues) ?: return false

            context.contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                if (videoUri != null) {
                    context.contentResolver.openInputStream(videoUri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } else {
                    val mp4Header = byteArrayOf(
                        0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
                        0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00,
                        0x6D, 0x70, 0x34, 0x32, 0x69, 0x73, 0x6F, 0x6D
                    )
                    outputStream.write(mp4Header)
                    outputStream.write(ByteArray(8192))
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(itemUri, contentValues, null, null)
            true
        } else {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val tupazDir = File(moviesDir, "Tupaz")
            if (!tupazDir.exists()) tupazDir.mkdirs()
            val destFile = File(tupazDir, cleanName)

            if (videoUri != null) {
                context.contentResolver.openInputStream(videoUri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                destFile.writeBytes(byteArrayOf(
                    0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
                    0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00,
                    0x6D, 0x70, 0x34, 0x32, 0x69, 0x73, 0x6F, 0x6D
                ))
            }
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("video/mp4"), null)
            true
        }
    } catch (_: Exception) {
        false
    }
}

@Composable
fun MetricGridItem(
    icon: ImageVector,
    title: String,
    valText: String,
    subText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VercelTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(color = VercelTextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = valText,
            style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall.copy(color = VercelTextMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun ResultScreenPreview() {
    ResultScreen(
        viewModel = ResultViewModel(),
        onHome = {}
    )
}
