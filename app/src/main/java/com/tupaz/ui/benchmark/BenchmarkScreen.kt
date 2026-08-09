package com.tupaz.ui.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tupaz.benchmark.BenchmarkResultData
import com.tupaz.benchmark.DeviceProfile
import com.tupaz.benchmark.DeviceTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Device Hardware Setup") }) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is BenchmarkUiState.Idle -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Optimize Tupaz for Your GPU",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Run a fast 4-second benchmark to determine the best default mode for your device.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.runBenchmark() }) {
                            Text("Start GPU Benchmark")
                        }
                    }
                }
                is BenchmarkUiState.Running -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.currentTestName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progressPercentage / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.progressPercentage}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                is BenchmarkUiState.Completed -> {
                    BenchmarkResultView(
                        profile = state.deviceProfile,
                        onContinue = onContinue
                    )
                }
            }
        }
    }
}

@Composable
fun BenchmarkResultView(
    profile: DeviceProfile,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Benchmark Complete!",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Hardware Tier: ${profile.tier.displayName}",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recommended Mode: ${profile.recommendedMode.displayName}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Vulkan Compute: %.1f MP/s".format(profile.benchmarkResult.vulkanMegapixelsPerSec),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "VRAM Bandwidth: %.1f GB/s".format(profile.benchmarkResult.memoryBandwidthGbps),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply and Continue")
        }
    }
}

@Preview
@Composable
fun BenchmarkResultViewPreview() {
    val sampleProfile = DeviceProfile(
        tier = DeviceTier.MID,
        benchmarkResult = BenchmarkResultData(
            fp16Tflops = 2.5,
            vulkanMegapixelsPerSec = 1450.0,
            memoryBandwidthGbps = 32.0,
            totalTimeMs = 1200.0
        )
    )
    BenchmarkResultView(profile = sampleProfile, onContinue = {})
}
