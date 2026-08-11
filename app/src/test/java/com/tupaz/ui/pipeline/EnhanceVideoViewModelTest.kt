package com.tupaz.ui.pipeline

import com.tupaz.domain.pipeline.AiQuality
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EnhanceVideoViewModelTest {

    private lateinit var viewModel: EnhanceVideoViewModel

    @Before
    fun setUp() {
        viewModel = EnhanceVideoViewModel()
    }

    @Test
    fun `initial state defaults to AUTO mode`() {
        assertEquals(AiModeSelection.AUTO, viewModel.uiState.value.selectedAiMode)
    }

    @Test
    fun `selectAiMode updates mode state to MANUAL`() {
        viewModel.selectAiMode(AiModeSelection.MANUAL)
        assertEquals(AiModeSelection.MANUAL, viewModel.uiState.value.selectedAiMode)
    }

    @Test
    fun `updateDenoise updates slider state`() {
        viewModel.updateDenoise(45f)
        assertEquals(45f, viewModel.uiState.value.denoiseValue)
    }

    @Test
    fun `updateSharpen updates slider state`() {
        viewModel.updateSharpen(60f)
        assertEquals(60f, viewModel.uiState.value.sharpenValue)
    }

    @Test
    fun `prior selections persist when later settings are updated`() {
        viewModel.setProjectName("My Project")
        viewModel.selectQuality(AiQuality.MEDIUM)
        viewModel.selectScaleFactor("2x")
        viewModel.selectAiMode(AiModeSelection.MANUAL)
        viewModel.updateDenoise(45f)
        viewModel.updateSharpen(60f)

        val state = viewModel.uiState.value
        assertEquals("My Project", state.projectName)
        assertEquals(AiQuality.MEDIUM, state.selectedQuality)
        assertEquals("2x", state.selectedScaleFactor)
        assertEquals(AiModeSelection.MANUAL, state.selectedAiMode)
        assertEquals(45f, state.denoiseValue)
        assertEquals(60f, state.sharpenValue)
    }

    @Test
    fun `resetProject clears all persisted state back to defaults`() {
        viewModel.setProjectName("Custom Name")
        viewModel.selectQuality(AiQuality.LOW)
        viewModel.updateDenoise(100f)
        viewModel.updateSharpen(80f)

        viewModel.resetProject()

        val defaults = EnhanceUiState()
        val state = viewModel.uiState.value
        assertEquals(defaults.projectName, state.projectName)
        assertEquals(defaults.selectedQuality, state.selectedQuality)
        assertEquals(defaults.denoiseValue, state.denoiseValue)
        assertEquals(defaults.sharpenValue, state.sharpenValue)
        assertEquals(defaults.selectedAiMode, state.selectedAiMode)
    }
}
