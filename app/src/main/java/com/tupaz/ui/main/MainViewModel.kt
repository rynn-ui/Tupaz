package com.tupaz.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.tupaz.TupazApplication
import com.tupaz.data.storage.ProjectStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(
    application: Application = TupazApplication.instance
) : AndroidViewModel(application) {

    private val projectStorage = ProjectStorage(application)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        val projects = projectStorage.loadProjects()
        _uiState.update { it.copy(recentProjects = projects) }
    }

    fun setSelectedVideoUri(uri: Uri?) {
        _uiState.update { it.copy(selectedVideoUri = uri) }
    }

    fun addRecentProject(project: ProjectItem) {
        projectStorage.saveProject(project)
        loadProjects()
    }

    fun deleteRecentProject(projectId: String) {
        projectStorage.deleteProject(projectId)
        loadProjects()
    }
}
