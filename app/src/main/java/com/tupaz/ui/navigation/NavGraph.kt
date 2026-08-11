package com.tupaz.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tupaz.data.firebase.AppConfigState
import com.tupaz.data.firebase.FirebaseConfigManager
import com.tupaz.ui.gating.StartupGatingScreen
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

    val configManager = remember { FirebaseConfigManager(context) }
    val configState by configManager.configState.collectAsState()
    var userSkippedOptionalUpdate by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        configManager.fetchAndEvaluateConfig()
    }

    val isGated = when (configState) {
        is AppConfigState.BetaClosed,
        is AppConfigState.Maintenance,
        is AppConfigState.UpdateRequired -> true
        is AppConfigState.UpdateAvailable -> !userSkippedOptionalUpdate
        else -> false
    }

    if (isGated) {
        StartupGatingScreen(
            state = configState,
            onContinueToApp = { userSkippedOptionalUpdate = true },
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier
        ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("permissions") {
                com.tupaz.ui.permission.PermissionOnboardingScreen(
                    onContinue = {
                        val profileStorage = com.tupaz.data.profile.UserProfileStorage(context)
                        val isOnboardingCompleted = profileStorage.isOnboardingCompleted()
                        val nextDest = if (!isOnboardingCompleted) "onboarding" else "home"
                        navController.navigate(nextDest) {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                )
            }

            composable("onboarding") {
                com.tupaz.ui.onboarding.ProfileSetupScreen(
                    onContinue = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                MainScreen(
                    viewModel = mainViewModel,
                    onStartEnhance = { uri: Uri? ->
                        enhanceViewModel.resetProject()
                        if (uri != null) {
                            enhanceViewModel.loadVideoFromUri(uri, context)
                        }
                        navController.navigate("enhance")
                    },
                    onOpenProject = { project: ProjectItem ->
                        when (project.status) {
                            com.tupaz.ui.main.ProjectStatus.COMPLETED -> {
                                val outUri = project.outputUri
                                val outputValid = if (outUri != null && outUri.scheme == "file") {
                                    val file = java.io.File(outUri.path ?: "")
                                    file.exists() && file.length() > 0
                                } else outUri != null

                                if (outputValid && outUri != null) {
                                    val config = com.tupaz.data.processing.ProcessingJobConfig(
                                        projectId = project.id,
                                        projectName = project.projectName,
                                        fileName = project.projectName,
                                        inputUriString = project.inputUriString,
                                        targetWidth = project.targetWidth,
                                        targetHeight = project.targetHeight,
                                        origRes = project.origRes,
                                        enhRes = project.enhRes,
                                        resTransition = project.resTransition,
                                        resDetail = project.resDetail,
                                        modelName = project.selectedModel,
                                        modelScale = project.modelScale,
                                        enhancementMode = project.enhancementMode,
                                        enhancementSub = project.enhancementSub,
                                        fpsLabel = project.fpsLabel,
                                        durationLabel = project.durationLabel,
                                        estimatedProcessingTime = project.estimatedProcessingTime,
                                        estimatedOutputSize = project.estimatedOutputSize
                                    )
                                    com.tupaz.data.processing.ProcessingManager.loadCompletedProject(
                                        context,
                                        config,
                                        outUri,
                                        project.realProcessingTime,
                                        project.realOutputSize
                                    )
                                    navController.navigate("result")
                                } else {
                                    com.tupaz.data.storage.ProjectStorage(context).updateProjectStatus(
                                        project.id,
                                        com.tupaz.ui.main.ProjectStatus.FAILED
                                    )
                                    enhanceViewModel.setProjectName(project.projectName)
                                    project.inputUri?.let { uri -> enhanceViewModel.loadVideoFromUri(uri, context) }
                                    navController.navigate("enhance")
                                }
                            }
                            com.tupaz.ui.main.ProjectStatus.PROCESSING -> {
                                navController.navigate("result")
                            }
                            else -> {
                                enhanceViewModel.setProjectName(project.projectName)
                                project.inputUri?.let { uri -> enhanceViewModel.loadVideoFromUri(uri, context) }
                                navController.navigate("enhance")
                            }
                        }
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
                        val projId = state.projectId ?: "proj_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}"
                        val projName = state.projectName.trim().ifEmpty { "New Project" }

                        val scaleMult = com.tupaz.domain.pipeline.PipelineScale.parseAndValidate(state.selectedScaleFactor)

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
                            projectId = projId,
                            projectName = projName,
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

                        val newProject = ProjectItem(
                            id = projId,
                            projectName = projName,
                            inputUriString = state.videoUri?.toString(),
                            outputUriString = null,
                            selectedQuality = state.selectedQuality.name,
                            selectedModel = state.selectedModel,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            status = com.tupaz.ui.main.ProjectStatus.PROCESSING,
                            resolutionLabel = targetRes,
                            fpsLabel = state.fpsLabel,
                            durationLabel = state.durationLabel,
                            sizeLabel = state.estimatedOutputSize,
                            targetWidth = targetW,
                            targetHeight = targetH,
                            origRes = state.resolutionLabel,
                            enhRes = targetRes,
                            resTransition = resTrans,
                            resDetail = resDet,
                            modelScale = scaleLabel,
                            enhancementMode = config.enhancementMode,
                            enhancementSub = config.enhancementSub,
                            estimatedProcessingTime = state.estimatedOutputTime,
                            estimatedOutputSize = state.estimatedOutputSize,
                            inputWidth = state.videoWidth,
                            inputHeight = state.videoHeight,
                            outputWidth = targetW,
                            outputHeight = targetH,
                            durationMs = state.durationMs
                        )
                        mainViewModel.addRecentProject(newProject)

                        com.tupaz.data.processing.ProcessingManager.startProcessing(context, config)

                        navController.navigate("result")
                    },
                    onOpenModelStore = { navController.navigate("models") }
                )
            }

            composable("models") {
                val modelViewModel: ModelManagerViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? com.tupaz.TupazApplication
                                ?: com.tupaz.TupazApplication.instance
                            ModelManagerViewModel(app)
                        }
                    }
                )
                ModelManagerScreen(
                    viewModel = modelViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("benchmark") {
                val benchmarkViewModel: BenchmarkViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            BenchmarkViewModel()
                        }
                    }
                )
                BenchmarkScreen(
                    viewModel = benchmarkViewModel,
                    onContinue = { navController.navigate("home") }
                )
            }

            composable("result") {
                ResultScreen(
                    viewModel = resultViewModel,
                    onHome = {
                        mainViewModel.loadProjects()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onCancelled = {
                        mainViewModel.loadProjects()
                        if (!navController.popBackStack()) {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateHome = {
                        mainViewModel.loadProjects()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
}

