package com.tupaz.ui.models

import com.tupaz.data.catalog.ModelCatalogItem

/**
 * Single item representation in the Model Manager screen.
 */
data class ModelItemUiState(
    val catalogItem: ModelCatalogItem,
    val isInstalled: Boolean,
    val localVersion: String? = null,
    val hasUpdateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val localSizeBytes: Long = 0L
)

/**
 * UI state for Model Manager screen.
 */
sealed interface ModelManagerUiState {
    data object Loading : ModelManagerUiState
    data class Error(val message: String) : ModelManagerUiState
    data class Success(
        val items: List<ModelItemUiState>,
        val totalStorageUsedBytes: Long
    ) : ModelManagerUiState
}
