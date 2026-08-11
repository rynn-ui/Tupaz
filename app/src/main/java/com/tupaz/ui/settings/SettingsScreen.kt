package com.tupaz.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import com.tupaz.data.settings.ButtonSizeStorage
import com.tupaz.ui.theme.ButtonSize
import com.tupaz.ui.theme.accessibleButtonSize
import com.tupaz.data.storage.AppCleanupManager
import com.tupaz.ui.theme.VercelBackground
import com.tupaz.ui.theme.VercelBorder
import com.tupaz.ui.theme.VercelBorderHighlight
import com.tupaz.ui.theme.VercelCardSurface
import com.tupaz.ui.theme.VercelSurface
import com.tupaz.ui.theme.VercelTextMuted
import com.tupaz.ui.theme.VercelTextPrimary
import com.tupaz.ui.theme.VercelTextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileStorage = remember { com.tupaz.data.profile.UserProfileStorage(context) }
    val buttonSizeStorage = remember { ButtonSizeStorage(context) }
    val syncManager = remember { com.tupaz.data.firebase.FirebaseSyncManager(context) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var userProfile by remember { mutableStateOf(profileStorage.getUserProfile()) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val selectedButtonSize by buttonSizeStorage.buttonSizeFlow.collectAsState()
    var showContactDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }

    val appVersionDisplay = "0.1.0 (Build 1)"

    Scaffold(
        modifier = modifier,
        containerColor = VercelBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VercelCardSurface)
                        .border(1.dp, VercelBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VercelTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = VercelTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VercelBorderHighlight, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VercelSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VercelCardSurface)
                                .border(1.dp, VercelBorderHighlight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = VercelTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userProfile?.name?.ifEmpty { "User Profile" } ?: "User Profile",
                                color = VercelTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Age: ${userProfile?.age ?: "Not set"}",
                                color = VercelTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Button(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground),
                        modifier = Modifier
                            .accessibleButtonSize(38.dp)
                    ) {
                        Text("Edit Profile", color = VercelBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "UI Button Size (Accessibility)",
                    color = VercelTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VercelSurface)
                        .border(1.dp, VercelBorder, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ButtonSize.entries.forEach { size ->
                        val isSelected = size == selectedButtonSize
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) VercelTextPrimary else Color.Transparent)
                                .clickable { buttonSizeStorage.saveButtonSize(size) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = size.displayName,
                                color = if (isSelected) VercelBackground else VercelTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

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
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VercelCardSurface)
                                .border(1.dp, VercelBorderHighlight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = VercelTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "App Developer",
                                color = VercelTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Developed by ryn",
                                color = VercelTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Get in touch or view source portfolio",
                                color = VercelTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Button(
                        onClick = { showContactDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Contact", color = VercelBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = VercelBackground,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VercelSurface)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "About Tupaz Engine",
                        subtitle = "Local AI models, frame processing & GPU specs",
                        onClick = { showAboutDialog = true }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "Zero data collection & on-device security",
                        onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "Usage guidelines & device output terms",
                        onClick = { showTermsDialog = true }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VercelSurface)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Delete,
                        iconTint = Color.Red,
                        iconBackground = VercelCardSurface,
                        title = "Clear App Cache",
                        subtitle = "Reset app state, clear temp files & recent projects",
                        onClick = { showClearCacheConfirmDialog = true }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = appVersionDisplay,
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            copyToClipboard(context, "App Version", appVersionDisplay)
                        }
                    )
                }
            }
        }

        if (showEditProfileDialog) {
            EditProfileDialog(
                currentName = userProfile?.name ?: "",
                currentAge = userProfile?.age?.toString() ?: "",
                onDismiss = { showEditProfileDialog = false },
                onSave = { newName, newAge ->
                    showEditProfileDialog = false
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val updated = syncManager.saveAndSyncProfile(newName, newAge)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            userProfile = updated
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (showContactDialog) {
            AppDeveloperContactDialog(onDismiss = { showContactDialog = false })
        }

        if (showAboutDialog) {
            AboutTupazEngineDialog(onDismiss = { showAboutDialog = false })
        }

        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }

        if (showTermsDialog) {
            TermsOfServiceDialog(onDismiss = { showTermsDialog = false })
        }

        if (showClearCacheConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirmDialog = false },
                title = {
                    Text(
                        text = "Clear App Cache & Reset?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VercelTextPrimary)
                    )
                },
                text = {
                    Text(
                        text = "This will stop any active processing job, delete all recent projects, remove output videos, clear temporary buffer files, and reset app state. Installed AI models will be preserved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VercelTextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showClearCacheConfirmDialog = false
                        val success = AppCleanupManager.performFullCleanup(context)
                        if (success) {
                            Toast.makeText(context, "Tupaz cleaned successfully", Toast.LENGTH_SHORT).show()
                            onNavigateHome()
                        } else {
                            Toast.makeText(context, "Cleanup encountered an error.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Clear All Data", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                        Text("Cancel", color = VercelTextSecondary)
                    }
                },
                containerColor = VercelSurface
            )
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color = VercelTextPrimary,
    iconBackground: Color = VercelCardSurface,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBackground)
                    .border(1.dp, VercelBorderHighlight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = VercelTextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = VercelTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = VercelTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AboutTupazEngineDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "About Tupaz Engine",
                    style = MaterialTheme.typography.titleLarge.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "On-Device Video Enhancement Engine",
                    style = MaterialTheme.typography.labelMedium.copy(color = VercelTextSecondary)
                )

                HorizontalDivider(color = VercelBorder, thickness = 1.dp)

                InfoSectionText(
                    heading = "100% Local On-Device Processing",
                    body = "Tupaz performs all video frame decoding, AI neural network super-resolution, and video encoding directly on your device. No video data or frames are ever uploaded to cloud servers."
                )

                InfoSectionText(
                    heading = "Redundant Frame Scanning",
                    body = "Integrates a specialized frame-difference analysis engine to detect dead or duplicate animation frames, skipping redundant AI inference passes to conserve device battery and speed up processing."
                )

                InfoSectionText(
                    heading = "Selectable AI Quality Models",
                    body = "• LOW: AnimeJaNai HD V3 SuperUltraCompact 2x (Ultra-lightweight real-time enhancement)\n" +
                            "• MEDIUM: AnimeJaNai HD V3 UltraCompact 2x (Balanced quality and processing speed)\n" +
                            "• HIGH: RealESRGAN AnimeVideo v3 2x (Maximum quality & artifact reduction)"
                )

                InfoSectionText(
                    heading = "NCNN + Vulkan Architecture",
                    body = "Neural network execution uses high-performance NCNN C++ libraries coupled with native Vulkan GPU hardware acceleration where supported by your device GPU drivers."
                )

                InfoSectionText(
                    heading = "Hardware Dependency & Thermal Protection",
                    body = "Processing speed, frame rates, and render times depend directly on your device's GPU/CPU capabilities. Tupaz includes dynamic thermal monitoring that automatically pauses inference if device temperature reaches critical safety thresholds."
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "On-Device Video Processing & Account Privacy",
                    style = MaterialTheme.typography.labelMedium.copy(color = VercelTextSecondary)
                )

                HorizontalDivider(color = VercelBorder, thickness = 1.dp)

                InfoSectionText(
                    heading = "100% On-Device Video Processing",
                    body = "Tupaz processes videos entirely on your device. No video files, frames, project files, or AI model data are uploaded to our servers."
                )

                InfoSectionText(
                    heading = "Profile & Account Data",
                    body = "Tupaz may collect limited profile and device information, such as your name, age, app version, device model, and Android version, when you provide it during setup. This information is stored locally and may be synchronized with Firebase for account/profile management."
                )

                InfoSectionText(
                    heading = "Storage Permissions",
                    body = "Media and storage permissions requested by Tupaz are strictly used to access the input video files you select and save your enhanced output videos to device storage."
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.titleLarge.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Usage Guidelines & Responsibilities",
                    style = MaterialTheme.typography.labelMedium.copy(color = VercelTextSecondary)
                )

                HorizontalDivider(color = VercelBorder, thickness = 1.dp)

                InfoSectionText(
                    heading = "Application Purpose",
                    body = "Tupaz is provided as an on-device utility for video enhancement and super-resolution upscaling."
                )

                InfoSectionText(
                    heading = "User Content Responsibility",
                    body = "You retain full ownership and sole legal responsibility for all input videos processed and output videos generated or exported using Tupaz."
                )

                InfoSectionText(
                    heading = "Hardware Performance & Output Disclaimer",
                    body = "Enhancement speed, frame rates, and visual output quality vary based on your device hardware (GPU/CPU), thermal conditions, and selected AI quality model. No guarantee of perfect results or specific frame rates is provided."
                )

                InfoSectionText(
                    heading = "Limitation of Liability",
                    body = "Tupaz is provided 'as is' without warranties of any kind. Users are responsible for maintaining backups of their original video files."
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSectionText(heading: String, body: String) {
    Column {
        Text(
            text = heading,
            color = VercelTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            color = VercelTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun AppDeveloperContactDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = VercelTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "App Developer Contact",
                        color = VercelTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                ContactDialogRow(
                    icon = Icons.Default.Email,
                    label = "Gmail",
                    value = "rudrakshallenhouse@gmail.com",
                    onCopy = { copyToClipboard(context, "Gmail", "rudrakshallenhouse@gmail.com") },
                    onOpen = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:rudrakshallenhouse@gmail.com"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            copyToClipboard(context, "Gmail", "rudrakshallenhouse@gmail.com")
                        }
                    }
                )

                ContactDialogRow(
                    icon = Icons.Default.Person,
                    label = "Discord Tag",
                    value = "@rynn0976",
                    onCopy = { copyToClipboard(context, "Discord Tag", "@rynn0976") }
                )

                ContactDialogRow(
                    icon = Icons.Default.Language,
                    label = "LinkedIn",
                    value = "linkedin.com/in/rudraksh-pandey",
                    onCopy = { copyToClipboard(context, "LinkedIn", "https://www.linkedin.com/in/rudraksh-pandey-00ab612a2") },
                    onOpen = {
                        try {
                            uriHandler.openUri("https://www.linkedin.com/in/rudraksh-pandey-00ab612a2")
                        } catch (_: Exception) {
                            copyToClipboard(context, "LinkedIn", "https://www.linkedin.com/in/rudraksh-pandey-00ab612a2")
                        }
                    }
                )

                ContactDialogRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    label = "GitHub Portfolio",
                    value = "github.com/rynn-ui",
                    onCopy = { copyToClipboard(context, "GitHub URL", "https://github.com/rynn-ui") },
                    onOpen = {
                        try {
                            uriHandler.openUri("https://github.com/rynn-ui")
                        } catch (_: Exception) {
                            copyToClipboard(context, "GitHub URL", "https://github.com/rynn-ui")
                        }
                    }
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            color = VercelTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDialogRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit,
    onOpen: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VercelCardSurface)
            .border(1.dp, VercelBorderHighlight, RoundedCornerShape(8.dp))
            .clickable { onOpen?.invoke() ?: onCopy() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VercelTextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = VercelTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    color = VercelTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = VercelTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentAge: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newAge: Int) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var age by remember { mutableStateOf(currentAge) }

    val nameTrimmed = name.trim()
    val ageInt = age.toIntOrNull()
    val isValid = nameTrimmed.isNotEmpty() && nameTrimmed.length <= 50 && ageInt != null && ageInt in 1..120

    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    color = VercelTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Column {
                    Text("Name", color = VercelTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(50) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VercelTextPrimary,
                            unfocusedBorderColor = VercelBorder,
                            focusedContainerColor = VercelCardSurface,
                            unfocusedContainerColor = VercelCardSurface,
                            cursorColor = VercelTextPrimary,
                            focusedTextColor = VercelTextPrimary,
                            unfocusedTextColor = VercelTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text("Age", color = VercelTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VercelTextPrimary,
                            unfocusedBorderColor = VercelBorder,
                            focusedContainerColor = VercelCardSurface,
                            unfocusedContainerColor = VercelCardSurface,
                            cursorColor = VercelTextPrimary,
                            focusedTextColor = VercelTextPrimary,
                            unfocusedTextColor = VercelTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = VercelTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (isValid) onSave(nameTrimmed, ageInt!!) },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
