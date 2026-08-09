package com.tupaz.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tupaz.ui.benchmark.BenchmarkScreen
import com.tupaz.ui.benchmark.BenchmarkViewModel
import com.tupaz.ui.main.MainScreen
import com.tupaz.ui.main.MainViewModel
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.models.ModelManagerScreen
import com.tupaz.ui.models.ModelManagerViewModel
import com.tupaz.ui.pipeline.EnhanceVideoScreen
import com.tupaz.ui.pipeline.EnhanceVideoViewModel
import com.tupaz.ui.result.ResultScreen
import com.tupaz.ui.result.ResultViewModel
import com.tupaz.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TupazNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel()
    val enhanceViewModel: EnhanceVideoViewModel = viewModel()
    val resultViewModel: ResultViewModel = viewModel()

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                MainScreen(
                    viewModel = mainViewModel,
                    onStartEnhance = { uri: Uri? ->
                        if (uri != null) {
                            enhanceViewModel.loadVideoFromUri(uri, context)
                        }
                        navController.navigate("enhance")
                    },
                    onOpenModels = { navController.navigate("models") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenResult = { navController.navigate("result") }
                )
            }

            composable("enhance") {
                EnhanceVideoScreen(
                    viewModel = enhanceViewModel,
                    onBack = { navController.popBackStack() },
                    onStartProcessing = {
                        val state = enhanceViewModel.uiState.value
                        val scaleMult = state.selectedScaleFactor
                            .filter(Char::isDigit)
                            .toIntOrNull()
                            ?.coerceIn(1, 4)
                            ?: 2

                        val origW = if (state.videoWidth > 0) state.videoWidth else 640
                        val origH = if (state.videoHeight > 0) state.videoHeight else 360

                        val targetW = origW * scaleMult
                        val targetH = origH * scaleMult

                        val resTierLabel = if (targetH >= 2160) "4K" else if (targetH >= 1440) "2K" else if (targetH >= 720) "720p HD" else "${targetH}p"
                        val targetRes = "$resTierLabel (${targetW}x${targetH})"
                        val resTrans = "${origH}p -> $resTierLabel"
                        val resDet = "${origW}x${origH} -> ${targetW}x${targetH}"
                        val scaleLabel = "${scaleMult}x Scale"

                        val config = com.tupaz.data.processing.ProcessingJobConfig(
                            fileName = state.fileName,
                            inputUriString = state.videoUri?.toString(),
                            targetWidth = targetW,
                            targetHeight = targetH,
                            origRes = state.resolutionLabel,
                            enhRes = targetRes,
                            resTransition = resTrans,
                            resDetail = resDet,
                            modelName = state.selectedModel,
                            modelScale = scaleLabel,
                            enhancementMode = if (state.selectedAiMode == com.tupaz.ui.pipeline.AiModeSelection.AUTO) "Auto (Balanced)" else "Manual",
                            enhancementSub = "Denoise · Sharpen · Recover",
                            fpsLabel = state.fpsLabel,
                            durationLabel = state.durationLabel,
                            estimatedProcessingTime = state.estimatedOutputTime,
                            estimatedOutputSize = state.estimatedOutputSize
                        )

                        com.tupaz.data.processing.ProcessingManager.startProcessing(context, config)

                        val newProject = ProjectItem(
                            id = System.currentTimeMillis().toString(),
                            title = state.fileName.ifEmpty { "Enhanced Project" },
                            resolutionLabel = targetRes,
                            fpsLabel = state.fpsLabel,
                            durationLabel = state.durationLabel,
                            sizeLabel = state.estimatedOutputSize
                        )
                        mainViewModel.addRecentProject(newProject)

                        navController.navigate("result")
                    },
                    onOpenModelStore = { navController.navigate("models") }
                )
            }

            composable("models") {
                val modelViewModel: ModelManagerViewModel = viewModel()
                ModelManagerScreen(
                    viewModel = modelViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("benchmark") {
                val benchmarkViewModel: BenchmarkViewModel = viewModel()
                BenchmarkScreen(
                    viewModel = benchmarkViewModel,
                    onContinue = { navController.navigate("home") }
                )
            }

            composable("result") {
                ResultScreen(
                    viewModel = resultViewModel,
                    onHome = { navController.navigate("home") }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
