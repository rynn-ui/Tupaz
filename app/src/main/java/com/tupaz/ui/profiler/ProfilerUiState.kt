package com.tupaz.ui.profiler

import com.tupaz.pipeline.ProfilerExportReport

sealed interface ProfilerUiState {
    data object Idle : ProfilerUiState
    data class Active(val report: ProfilerExportReport) : ProfilerUiState
}
