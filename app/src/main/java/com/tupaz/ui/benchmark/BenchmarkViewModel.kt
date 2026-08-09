package com.tupaz.ui.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tupaz.benchmark.DeviceProfile
import com.tupaz.benchmark.GpuBenchmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BenchmarkViewModel(
    private val gpuBenchmark: GpuBenchmark = GpuBenchmark()
) : ViewModel() {

    private val _uiState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle)
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    fun runBenchmark() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = BenchmarkUiState.Running(10, "Initializing GPU context...")
            delay(400)

            _uiState.value = BenchmarkUiState.Running(40, "Testing FP16 execution throughput...")
            delay(500)

            _uiState.value = BenchmarkUiState.Running(70, "Testing Vulkan compute megapixels/sec...")
            delay(500)

            _uiState.value = BenchmarkUiState.Running(90, "Testing VRAM memory bandwidth...")
            delay(300)

            val result = gpuBenchmark.runBenchmark()
            val profile = DeviceProfile.classify(result)

            _uiState.value = BenchmarkUiState.Completed(profile)
        }
    }
}
