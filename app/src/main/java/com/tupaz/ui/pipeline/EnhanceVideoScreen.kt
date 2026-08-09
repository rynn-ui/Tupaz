package com.tupaz.ui.pipeline

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.theme.VercelBackground
import com.tupaz.ui.theme.VercelBorder
import com.tupaz.ui.theme.VercelBorderHighlight
import com.tupaz.ui.theme.VercelCardSurface
import com.tupaz.ui.theme.VercelSurface
import com.tupaz.ui.theme.VercelTextMuted
import com.tupaz.ui.theme.VercelTextPrimary
import com.tupaz.ui.theme.VercelTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhanceVideoScreen(
    viewModel: EnhanceVideoViewModel,
    onBack: () -> Unit,
    onStartProcessing: () -> Unit,
    onOpenModelStore: () -> Unit,
    onVideoImported: (ProjectItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshInstalledModels(context)
        val testFile = java.io.File(context.cacheDir, "raw_video_2.mp4")
        if (testFile.exists()) {
            viewModel.loadVideoFromUri(Uri.fromFile(testFile), context)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadVideoFromUri(uri, context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tupaz: Video Config",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = VercelTextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VercelSurface)
                    .border(1.dp, VercelBorder, RoundedCornerShape(0.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (uiState.videoUri == null) {
                            videoPickerLauncher.launch("video/*")
                        } else {
                            onStartProcessing()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VercelTextPrimary,
                        contentColor = VercelBackground
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VercelBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.videoUri == null) "Select Video to Enhance" else "Start Processing (${uiState.selectedScaleFactor})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = VercelBackground,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.videoUri == null) {
                        "Select a video file to calculate estimated time & output size"
                    } else if (uiState.estimatedOutputTime.startsWith("Calculating") || uiState.estimatedOutputSize.startsWith("Calculating")) {
                        "Calculating estimate…"
                    } else {
                        "Estimated Time: ${uiState.estimatedOutputTime} · Output Size: ${uiState.estimatedOutputSize}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = VercelTextSecondary
                )
            }
        },
        modifier = modifier,
        containerColor = VercelBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Add Video Vercel Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp))
                        .clickable { videoPickerLauncher.launch("video/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VercelSurface)
                ) {
                    if (uiState.videoUri == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VercelCardSurface)
                                    .border(1.dp, VercelBorderHighlight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Video",
                                    tint = VercelTextPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Add Video File",
                                style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap here to select a video from your device gallery",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VercelTextSecondary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VercelCardSurface)
                                    .border(1.dp, VercelBorderHighlight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = VercelTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uiState.fileName,
                                        style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.resolutionLabel} · ${uiState.fileSizeLabel}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextSecondary)
                                )
                            }
                            IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change Video",
                                    tint = VercelTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // AI Model Header & Store Link
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Model", style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.selectedModel} · Progressive Scan",
                            style = MaterialTheme.typography.bodySmall,
                            color = VercelTextSecondary
                        )
                    }
                    if (com.tupaz.config.FeatureFlags.ENABLE_MODEL_STORE) {
                        TextButton(onClick = onOpenModelStore) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = VercelTextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Model Store", color = VercelTextPrimary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // AI Mode Segmented Selector (Vercel Style)
            item {
                Column {
                    Text("AI Mode", style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextSecondary))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VercelSurface)
                            .border(1.dp, VercelBorder, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        val isAuto = uiState.selectedAiMode == AiModeSelection.AUTO
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAuto) VercelTextPrimary else Color.Transparent)
                                .clickable { viewModel.selectAiMode(AiModeSelection.AUTO) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Auto (Recommended)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isAuto) VercelBackground else VercelTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isAuto) VercelTextPrimary else Color.Transparent)
                                .clickable { viewModel.selectAiMode(AiModeSelection.MANUAL) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Manual",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (!isAuto) VercelBackground else VercelTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // Mode Content: Auto vs Manual Sliders
            if (uiState.selectedAiMode == AiModeSelection.AUTO) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = VercelSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = VercelTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Device Power: ${uiState.devicePowerName} GPU Tier",
                                    style = MaterialTheme.typography.titleSmall.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Auto Mode tuned parameter settings optimized for your device capability.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary)
                                )
                            }
                        }
                    }
                }

                item { AccordionItem("Denoise", valueLabel = "${uiState.autoSettingValues.denoise} (Auto)") }
                item { AccordionItem("Recover Detail", valueLabel = "${uiState.autoSettingValues.recoverDetail} (Auto)") }
                item { AccordionItem("Sharpen", valueLabel = "${uiState.autoSettingValues.sharpen} (Auto)") }
                item { AccordionItem("Dehalo", valueLabel = "${uiState.autoSettingValues.dehalo} (Auto)") }
            } else {
                item {
                    SliderControlItem(
                        label = "Denoise",
                        subtitle = "Remove noise while preserving detail.",
                        value = uiState.denoiseValue,
                        onValueChange = viewModel::updateDenoise
                    )
                }
                item {
                    SliderControlItem(
                        label = "Recover Detail",
                        subtitle = "Recover detail from blur.",
                        value = uiState.recoverDetailValue,
                        onValueChange = viewModel::updateRecoverDetail
                    )
                }
                item {
                    SliderControlItem(
                        label = "Sharpen",
                        subtitle = "Enhance edges and fine detail.",
                        value = uiState.sharpenValue,
                        onValueChange = viewModel::updateSharpen
                    )
                }
                item {
                    SliderControlItem(
                        label = "Dehalo",
                        subtitle = "Reduce halo and ringing artifacts.",
                        value = uiState.dehaloValue,
                        onValueChange = viewModel::updateDehalo
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun AccordionItem(title: String, valueLabel: String = "") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VercelBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = VercelSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (valueLabel.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VercelCardSurface)
                            .border(1.dp, VercelBorderHighlight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = valueLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = VercelTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VercelTextSecondary)
            }
        }
    }
}

@Composable
fun SliderControlItem(
    label: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VercelSurface)
            .border(1.dp, VercelBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VercelCardSurface)
                        .border(1.dp, VercelBorderHighlight, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(value.toInt().toString(), style = MaterialTheme.typography.labelMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { onValueChange(0f) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = VercelTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = VercelTextPrimary,
                activeTrackColor = VercelTextPrimary,
                inactiveTrackColor = VercelBorderHighlight
            )
        )
    }
}

@Preview
@Composable
fun EnhanceVideoScreenPreview() {
    EnhanceVideoScreen(
        viewModel = EnhanceVideoViewModel(),
        onBack = {},
        onStartProcessing = {},
        onOpenModelStore = {}
    )
}
