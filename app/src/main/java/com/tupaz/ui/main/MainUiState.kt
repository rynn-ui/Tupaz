package com.tupaz.ui.main

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class ProjectItem(
    val id: String,
    val title: String,
    val resolutionLabel: String,
    val fpsLabel: String,
    val durationLabel: String,
    val sizeLabel: String
)

data class MainUiState(
    val recentProjects: List<ProjectItem> = emptyList(),
    val selectedVideoUri: Uri? = null
)
