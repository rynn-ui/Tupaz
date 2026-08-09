package com.tupaz.ui.pipeline

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
}
