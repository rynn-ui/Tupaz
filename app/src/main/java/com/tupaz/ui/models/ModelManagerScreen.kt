package com.tupaz.ui.models

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tupaz.data.catalog.ModelCatalogItem
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
fun ModelManagerScreen(
    viewModel: ModelManagerViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tupaz: Model Store",
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
                is ModelManagerUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Loading Model Store Catalog...",
                            style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VercelTextPrimary,
                            trackColor = VercelBorder
                        )
                    }
                }
                is ModelManagerUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadCatalog() },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is ModelManagerUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        StorageHeader(totalStorageBytes = state.totalStorageUsedBytes)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                        ) {
                            items(state.items, key = { it.catalogItem.modelId }) { itemState ->
                                ModelItemCard(
                                    itemState = itemState,
                                    onDownload = { viewModel.downloadModel(itemState.catalogItem) },
                                    onDelete = { viewModel.deleteModel(itemState.catalogItem.modelId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageHeader(
    totalStorageBytes: Long,
    modifier: Modifier = Modifier
) {
    val megabytes = totalStorageBytes / (1024 * 1024)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, VercelBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = VercelSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Total Model Storage Used",
                style = MaterialTheme.typography.titleMedium.copy(color = VercelTextSecondary)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$megabytes MB",
                style = MaterialTheme.typography.titleLarge.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun ModelItemCard(
    itemState: ModelItemUiState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = itemState.catalogItem
    Card(
        modifier = modifier
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
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(color = VercelTextPrimary, fontWeight = FontWeight.Bold)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VercelCardSurface)
                        .border(1.dp, VercelBorderHighlight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v${item.version}",
                        style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = VercelTextSecondary)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (itemState.isDownloading) {
                LinearProgressIndicator(
                    progress = { itemState.downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = VercelTextPrimary,
                    trackColor = VercelBorder
                )
                Text(
                    text = "Downloading... ${itemState.downloadProgress}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = VercelTextSecondary),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (itemState.isInstalled) {
                        if (itemState.hasUpdateAvailable) {
                            Button(
                                onClick = onDownload,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground)
                            ) {
                                Text("Update", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Delete")
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VercelTextPrimary, contentColor = VercelBackground)
                        ) {
                            Text("Download (${item.sizeBytes / (1024 * 1024)} MB)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ModelItemCardPreview() {
    val sampleItem = ModelCatalogItem(
        modelId = "realesrgan-x2plus",
        name = "RealESRGAN 2x",
        description = "General 2x image and video upscale model.",
        version = "1.0.0",
        binUrl = "",
        paramUrl = "",
        sha256 = "",
        sizeBytes = 67 * 1024 * 1024,
        requiredForModes = listOf("balanced")
    )
    ModelItemCard(
        itemState = ModelItemUiState(
            catalogItem = sampleItem,
            isInstalled = false
        ),
        onDownload = {},
        onDelete = {}
    )
}
