package com.tupaz.ui.benchmark

import com.tupaz.benchmark.DeviceProfile

sealed interface BenchmarkUiState {
    data object Idle : BenchmarkUiState
    data class Running(val progressPercentage: Int, val currentTestName: String) : BenchmarkUiState
    data class Completed(val deviceProfile: DeviceProfile) : BenchmarkUiState
}
