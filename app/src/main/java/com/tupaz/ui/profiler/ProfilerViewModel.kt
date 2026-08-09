package com.tupaz.ui.profiler

import androidx.lifecycle.ViewModel
import com.tupaz.pipeline.PipelineProfiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfilerViewModel(
    private val profiler: PipelineProfiler = PipelineProfiler()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfilerUiState>(ProfilerUiState.Idle)
    val uiState: StateFlow<ProfilerUiState> = _uiState.asStateFlow()

    init {
        // Initialize dummy profiler data for live visualization
        profiler.incrementFrameCount()
        profiler.recordStage("denoise") { Thread.sleep(12) }
        profiler.recordStage("super_res") { Thread.sleep(57) }
        profiler.recordStage("face_restore") { Thread.sleep(13) }
        updateReport()
    }

    fun updateReport() {
        val report = profiler.generateReport()
        _uiState.value = ProfilerUiState.Active(report)
    }

    fun exportJson(): String {
        return profiler.exportJson()
    }
}
