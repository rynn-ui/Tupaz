package com.tupaz.ui.cloud

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun CloudProcessingScreen(
    viewModel: CloudProcessingViewModel,
    inputVideoUri: Uri?,
    onBack: () -> Unit,
    onJobCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadVideoToDrive(context, uri)
        }
    }

    val actionButtonText = when {
        uiState.isExportedVideoSaved -> "Open Output Folder (Google Drive)"
        !uiState.isVideoUploaded -> "Step 1: Upload Video to Google Drive (tupaz_cloud)"
        else -> "Step 2: Open Colab & Run Notebook"
    }

    // Download Pop-Up Dialog when video is saved
    if (uiState.showDownloadPopUp && uiState.isExportedVideoSaved) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDownloadPopUp() },
            containerColor = VercelSurface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Video Saved!", fontWeight = FontWeight.Bold, color = VercelTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your AI upscaled video has been processed on Google Colab T4 GPU and saved to your Google Drive folder!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VercelCardSurface)
                            .border(1.dp, VercelBorderHighlight, RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Output Directory:", fontSize = 11.sp, color = VercelTextMuted)
                            Text(
                                text = "Google Drive / tupaz_cloudexported / ${uiState.exportedFileName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = VercelTextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissDownloadPopUp()
                        viewModel.openExportedDriveFolder(context)
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Output Folder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDownloadPopUp() }) {
                    Text("Close", color = VercelTextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tupaz: AI Cloud",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = VercelTextPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(VercelCardSurface)
                                .border(1.dp, VercelBorderHighlight, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "COLAB T4 GPU",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VercelTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VercelBackground
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VercelSurface)
                    .border(1.dp, VercelBorder, RoundedCornerShape(0.dp))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            uiState.isExportedVideoSaved -> viewModel.openExportedDriveFolder(context)
                            !uiState.isVideoUploaded -> {
                                if (inputVideoUri != null) {
                                    viewModel.uploadVideoToDrive(context, inputVideoUri)
                                } else {
                                    videoPickerLauncher.launch("video/*")
                                }
                            }
                            else -> viewModel.startBackgroundColabProcessing(context)
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
                            imageVector = if (uiState.isExportedVideoSaved) Icons.Default.Folder else Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = VercelBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionButtonText,
                            style = MaterialTheme.typography.titleMedium.copy(color = VercelBackground, fontWeight = FontWeight.Bold)
                        )
                    }
                }
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

            // Google Drive Storage Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VercelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive Folders",
                                    style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = VercelTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uiState.googleAccountName,
                                        style = MaterialTheme.typography.bodySmall.copy(color = VercelTextPrimary, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📥 Input Folder: ${uiState.driveFolderNameFull}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VercelTextSecondary
                                )
                                Text(
                                    text = "📤 Output Folder: ${uiState.driveExportedFolderNameFull}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.switchGoogleAccount(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VercelTextPrimary)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = VercelTextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Google Account / Drive", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Step 1: Input Video Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VercelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Step 1: Save Video to 'tupaz_cloud'",
                                    style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (uiState.isVideoUploaded) "Saved in tupaz_cloud: ${uiState.uploadedFileName}" else "No video saved in tupaz_cloud yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isVideoUploaded) Color(0xFF10B981) else VercelTextSecondary
                                )
                            }
                            Button(
                                onClick = {
                                    if (inputVideoUri != null) {
                                        viewModel.uploadVideoToDrive(context, inputVideoUri)
                                    } else {
                                        videoPickerLauncher.launch("video/*")
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isVideoUploaded) Color(0xFF10B981) else VercelTextPrimary,
                                    contentColor = VercelBackground
                                )
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (uiState.isVideoUploaded) "Re-upload" else "Upload Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Step 2: Colab Notebook Launcher Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VercelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Step 2: Open Colab Notebook",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VercelTextPrimary
                                    )
                                )
                                Text(
                                    text = "REAL_ESRGAN_video_by_karan.ipynb · T4 GPU",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VercelTextSecondary
                                )
                            }
                            if (uiState.isColabButtonEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = VercelTextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Locked", style = MaterialTheme.typography.labelSmall, color = VercelTextMuted)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.openColabNotebook(context) },
                            enabled = uiState.isColabButtonEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VercelTextPrimary,
                                contentColor = VercelBackground,
                                disabledContainerColor = VercelCardSurface,
                                disabledContentColor = VercelTextMuted
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isColabButtonEnabled) "Open Google Colab Import Tab" else "Upload Video to Unlock Colab Button",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.shareKaranNotebook(context) },
                            enabled = uiState.isColabButtonEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VercelTextPrimary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = VercelTextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Notebook (.ipynb)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Automated Colab Code View Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VercelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = VercelTextPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Notebook Code",
                                    style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                            Button(
                                onClick = { viewModel.copyScriptToClipboard(context) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VercelCardSurface)
                                .border(1.dp, VercelBorderHighlight, RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = uiState.colabScript,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = VercelTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Exported Video Download Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VercelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isExportedVideoSaved) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (uiState.isExportedVideoSaved) Color(0xFF10B981) else VercelTextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isExportedVideoSaved) "Exported Video Ready!" else "Exported Video Link",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = VercelTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.isExportedVideoSaved)
                                "Saved in: Google Drive / tupaz_cloudexported / ${uiState.exportedFileName}"
                            else
                                "Export link activates ONLY after Colab finishes and video is saved to Google Drive / tupaz_cloudexported.",
                            style = MaterialTheme.typography.bodySmall.copy(color = VercelTextSecondary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.openExportedDriveFolder(context) },
                            enabled = uiState.isExportedVideoSaved,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White,
                                disabledContainerColor = VercelCardSurface,
                                disabledContentColor = VercelTextMuted
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isExportedVideoSaved) Icons.AutoMirrored.Filled.Launch else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isExportedVideoSaved) "Download / View Output (tupaz_cloudexported)" else "Waiting for Video in tupaz_cloudexported...",
                                style = MaterialTheme.typography.titleSmall.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
