package com.tupaz.ui.profiler

import androidx.compose.foundation.background
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
import com.tupaz.pipeline.ProfilerExportReport
import com.tupaz.pipeline.StageMetricsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilerScreen(
    viewModel: ProfilerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pipeline Profiler") }) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ProfilerUiState.Idle -> {
                    Text(
                        text = "No active profiler data recorded",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProfilerUiState.Active -> {
                    ProfilerReportContent(
                        report = state.report,
                        onExportJson = { viewModel.exportJson() }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilerReportContent(
    report: ProfilerExportReport,
    onExportJson: () -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device: ${report.device}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Mode: ${report.mode} | Frames: ${report.frameCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Flow Skip: ${report.flowSkipPct}%", style = MaterialTheme.typography.labelSmall)
                    Text("SCUNet Skip: ${report.scunetSkipPct}%", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Stage Latency Breakdown",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(report.stages.entries.toList()) { (stageName, metrics) ->
                StageMetricRow(stageName = stageName, metrics = metrics)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onExportJson() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Profiler JSON")
        }
    }
}

@Composable
fun StageMetricRow(
    stageName: String,
    metrics: StageMetricsData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stageName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${metrics.meanMs} ms (${metrics.pct}%)",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (metrics.pct / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "P99: ${metrics.p99Ms} ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun ProfilerReportContentPreview() {
    val sampleReport = ProfilerExportReport(
        device = "Snapdragon 870 / Adreno 650",
        mode = "BALANCED",
        frameCount = 1440,
        flowSkipPct = 43.2,
        scunetSkipPct = 61.0,
        stages = mapOf(
            "denoise" to StageMetricsData(12.1, 18.4, 10.3),
            "super_res" to StageMetricsData(57.3, 82.1, 48.5),
            "face_restore" to StageMetricsData(13.2, 24.0, 11.2)
        )
    )
    ProfilerReportContent(report = sampleReport, onExportJson = { "" })
}
