package com.tupaz.ui.main

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.tupaz.R
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tupaz.data.storage.ProjectThumbnailManager
import com.tupaz.ui.theme.HeroBackgroundBrush
import com.tupaz.ui.theme.MetallicBorderBrush
import com.tupaz.ui.theme.VercelBackground
import com.tupaz.ui.theme.VercelBorder
import com.tupaz.ui.theme.VercelBorderHighlight
import com.tupaz.ui.theme.VercelCardSurface
import com.tupaz.ui.theme.VercelSurface
import com.tupaz.ui.theme.VercelTextMuted
import com.tupaz.ui.theme.VercelTextPrimary
import com.tupaz.ui.theme.VercelTextSecondary

private val CardShape14 = RoundedCornerShape(14.dp)
private val CardShape12 = RoundedCornerShape(12.dp)
private val CardShape10 = RoundedCornerShape(10.dp)
private val CardShape8 = RoundedCornerShape(8.dp)
private val CardShape4 = RoundedCornerShape(4.dp)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onStartEnhance: (Uri?) -> Unit,
    onOpenModels: () -> Unit,
    onOpenProject: (ProjectItem) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenResult: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    val rememberStartEnhance = remember(onStartEnhance) { { onStartEnhance(null) } }
    val rememberDeleteProject = remember(viewModel) { { id: String -> viewModel.deleteRecentProject(id) } }
    val rememberRenameProject = remember(viewModel) { { id: String, name: String -> viewModel.renameProject(id, name) } }

    Scaffold(
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
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Tupaz Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CardShape10)
                                .border(1.dp, VercelBorderHighlight, CardShape10)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tupaz",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VercelTextPrimary,
                                        fontSize = 21.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CardShape4)
                                        .background(VercelCardSurface)
                                        .border(1.dp, VercelBorderHighlight, CardShape4)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO ENGINE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = VercelTextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "High-Performance AI Video Processing",
                                style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary, fontSize = 11.sp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CardShape10)
                            .background(VercelCardSurface)
                            .border(1.dp, VercelBorder, CardShape10)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = VercelTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item(key = "hero_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape14)
                        .background(HeroBackgroundBrush)
                        .border(1.dp, MetallicBorderBrush, CardShape14)
                        .clickable(onClick = rememberStartEnhance)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CardShape12)
                                    .background(VercelCardSurface)
                                    .border(1.dp, VercelBorderHighlight, CardShape12),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = VercelTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Enhance Video",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = VercelTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "4K Ultra-HD · 60 FPS · Denoise & Recover",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = VercelTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VercelCardSurface)
                                .border(1.dp, VercelBorderHighlight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Start Enhancement",
                                tint = VercelTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (com.tupaz.config.FeatureFlags.ENABLE_MODEL_STORE) {
                item(key = "quick_actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = "Model Store",
                            subtitle = "Browse AI Models",
                            badge = "CATALOG",
                            icon = Icons.Default.ShoppingBag,
                            onClick = onOpenModels,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "projects_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Projects",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VercelTextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            if (uiState.recentProjects.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape12)
                            .background(VercelSurface)
                            .border(1.dp, VercelBorder, CardShape12)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Recent Projects",
                                style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Enhance Video' above to select and process a video file.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VercelTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentProjects, key = { it.id }) { project ->
                    RecentProjectItemCard(
                        project = project,
                        onClick = { onOpenProject(project) },
                        onRename = { newName -> rememberRenameProject(project.id, newName) },
                        onDelete = { rememberDeleteProject(project.id) }
                    )
                }
            }

            item(key = "bottom_space") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CardShape12)
            .background(VercelSurface)
            .border(1.dp, VercelBorder, CardShape12)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CardShape8)
                        .background(VercelCardSurface)
                        .border(1.dp, VercelBorderHighlight, CardShape8),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VercelTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CardShape4)
                        .background(VercelCardSurface)
                        .border(1.dp, VercelBorderHighlight, CardShape4)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = VercelTextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = VercelTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun RecentProjectItemCard(
    project: ProjectItem,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val thumbnailBitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = project.id, key2 = project.thumbnailPath) {
        value = withContext(Dispatchers.IO) {
            try {
                val file = ProjectThumbnailManager.getOrGenerateThumbnail(context, project)
                if (file != null && file.exists() && file.length() > 0) {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                    val targetDim = 320
                    var sampleSize = 1
                    if (options.outHeight > targetDim || options.outWidth > targetDim) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while (halfHeight / sampleSize >= targetDim && halfWidth / sampleSize >= targetDim) {
                            sampleSize *= 2
                        }
                    }
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    bitmap?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
    val thumbnailBitmap = thumbnailBitmapState.value

    if (showRenameDialog) {
        var newNameText by remember { mutableStateOf(project.projectName) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = "Rename Project",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VercelTextPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a new name for this project:",
                        style = MaterialTheme.typography.bodySmall,
                        color = VercelTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = newNameText,
                        onValueChange = { newNameText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VercelTextPrimary,
                            unfocusedTextColor = VercelTextPrimary,
                            focusedBorderColor = VercelBorderHighlight,
                            unfocusedBorderColor = VercelBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalName = newNameText.trim().ifEmpty { project.projectName }
                    onRename(finalName)
                    showRenameDialog = false
                }) {
                    Text("Save", color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = VercelTextSecondary)
                }
            },
            containerColor = VercelSurface
        )
    }

    if (showDeleteConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete project?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = VercelTextPrimary)
                )
            },
            text = {
                Text(
                    text = "Deleting this project will permanently remove its saved record and associated output video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VercelTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = VercelTextSecondary)
                }
            },
            containerColor = VercelSurface
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape12)
            .background(VercelSurface)
            .border(1.dp, VercelBorder, CardShape12)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(68.dp)
                    .clip(CardShape8)
                    .background(VercelCardSurface)
                    .border(1.dp, VercelBorderHighlight, CardShape8),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap,
                        contentDescription = "Project Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = VercelTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.projectName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = VercelTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = project.formattedResolutionPair,
                    style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextSecondary, fontSize = 12.sp)
                )
                Text(
                    text = project.formattedModelLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextSecondary, fontSize = 12.sp)
                )
                Text(
                    text = "${project.formattedDuration} · ${project.formattedOutputSize}",
                    style = MaterialTheme.typography.labelSmall.copy(color = VercelTextMuted, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = project.formattedStatusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = when (project.status) {
                                ProjectStatus.COMPLETED -> Color(0xFF10B981)
                                ProjectStatus.PROCESSING -> Color(0xFF3B82F6)
                                ProjectStatus.FAILED -> Color(0xFFEF4444)
                                ProjectStatus.CANCELLED -> VercelTextMuted
                                ProjectStatus.DRAFT -> VercelTextSecondary
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = project.formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(color = VercelTextMuted, fontSize = 10.sp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Project Options",
                        tint = VercelTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(VercelSurface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = VercelTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rename", color = VercelTextPrimary)
                            }
                        },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", color = Color.Red)
                            }
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    MainScreen(
        viewModel = MainViewModel(),
        onStartEnhance = {},
        onOpenModels = {}
    )
}
