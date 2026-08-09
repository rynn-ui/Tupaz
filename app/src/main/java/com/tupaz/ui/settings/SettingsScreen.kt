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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedButtonSize by remember { mutableStateOf("Medium") }
    var showContactDialog by remember { mutableStateOf(false) }

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
            // Top Bar
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

            // Section 1: UI Button Size (Accessibility)
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
                    listOf("Small", "Medium", "Large").forEach { size ->
                        val isSelected = size == selectedButtonSize
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) VercelTextPrimary else Color.Transparent)
                                .clickable { selectedButtonSize = size },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = size,
                                color = if (isSelected) VercelBackground else VercelTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Section 2: App Developer Card
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

            // Section 3: Information & Resources Card Group
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
                        subtitle = "Learn more about duplicate frame scanning & AI models",
                        onClick = {
                            Toast.makeText(context, "Tupaz AI Engine v0.2.0 Active", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = {
                            Toast.makeText(context, "All processing occurs 100% locally on your device.", Toast.LENGTH_LONG).show()
                        }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "Rules and guidelines",
                        onClick = {
                            Toast.makeText(context, "Open-source AI Video Enhancement Engine.", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help & FAQ",
                        subtitle = "Get help and find answers",
                        onClick = {
                            Toast.makeText(context, "Visit github.com/rynn-ui for docs.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Section 4: Cache & App Info Group
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
                        subtitle = "Free up storage space",
                        onClick = {
                            try {
                                context.cacheDir.deleteRecursively()
                                Toast.makeText(context, "App cache cleared successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cache already clean.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(color = VercelBorder, thickness = 1.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "0.2.0 (Build 2)",
                        trailingIcon = Icons.Default.ContentCopy,
                        onClick = {
                            copyToClipboard(context, "App Version", "0.2.0 (Build 2)")
                        }
                    )
                }
            }
        }

        // Developer Contact Dialog Popup
        if (showContactDialog) {
            AppDeveloperContactDialog(
                onDismiss = { showContactDialog = false }
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
