package com.tupaz.ui.navigation

import android.content.Context
import com.tupaz.data.processing.ProcessingJobConfig
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StartupNavigationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        val filesDir = tempFolder.newFolder("files")
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { context.filesDir } returns filesDir
        every { context.getSharedPreferences(any(), any()) } returns prefs
    }

    private fun resolveDestination(
        navigateToExtra: String?,
        currentStatus: ProcessingStatus,
        hasActiveCompletion: Boolean,
        hasSeenPermissionIntro: Boolean = true,
        isOnboardingCompleted: Boolean = true
    ): String {
        return if (navigateToExtra == "result" ||
            currentStatus == ProcessingStatus.PROCESSING ||
            hasActiveCompletion) {
            "result"
        } else if (!hasSeenPermissionIntro) {
            "permissions"
        } else if (!isOnboardingCompleted) {
            "onboarding"
        } else {
            "home"
        }
    }

    @Test
    fun testFirstLaunchWithUnseenPermissionIntro_goesToPermissions() {
        val dest = resolveDestination(
            navigateToExtra = null,
            currentStatus = ProcessingStatus.IDLE,
            hasActiveCompletion = false,
            hasSeenPermissionIntro = false,
            isOnboardingCompleted = false
        )
        assertEquals("permissions", dest)
    }

    @Test
    fun testFirstLaunchPermissionIntroCompletedWithIncompleteOnboarding_goesToOnboarding() {
        val dest = resolveDestination(
            navigateToExtra = null,
            currentStatus = ProcessingStatus.IDLE,
            hasActiveCompletion = false,
            hasSeenPermissionIntro = true,
            isOnboardingCompleted = false
        )
        assertEquals("onboarding", dest)
    }

    @Test
    fun testNormalAppLaunchWithCompletedStateInPrefs_goesToHome() {
        assertFalse(ProcessingManager.consumeActiveSessionCompletion())

        val dest = resolveDestination(
            navigateToExtra = null,
            currentStatus = ProcessingStatus.COMPLETED,
            hasActiveCompletion = false,
            hasSeenPermissionIntro = true,
            isOnboardingCompleted = true
        )
        assertEquals("home", dest)
    }

    @Test
    fun testActiveProcessingOnLaunch_goesToResult() {
        val dest = resolveDestination(
            navigateToExtra = null,
            currentStatus = ProcessingStatus.PROCESSING,
            hasActiveCompletion = false,
            hasSeenPermissionIntro = false,
            isOnboardingCompleted = false
        )
        assertEquals("result", dest)
    }

    @Test
    fun testActiveSessionCompletion_goesToResultOnceThenHome() {
        val config = ProcessingJobConfig(projectId = "p1", projectName = "Test Project")
        ProcessingManager.startProcessing(context, config)
        ProcessingManager.completeProcessing(context, mockk(relaxed = true), "00:30", "15 MB")

        val hasActiveCompletion = ProcessingManager.consumeActiveSessionCompletion()
        val dest = resolveDestination(
            navigateToExtra = null,
            currentStatus = ProcessingManager.state.value.status,
            hasActiveCompletion = hasActiveCompletion,
            hasSeenPermissionIntro = true,
            isOnboardingCompleted = true
        )
        assertEquals("result", dest)

        val secondLaunchHasActiveCompletion = ProcessingManager.consumeActiveSessionCompletion()
        val secondDest = resolveDestination(
            navigateToExtra = null,
            currentStatus = ProcessingManager.state.value.status,
            hasActiveCompletion = secondLaunchHasActiveCompletion,
            hasSeenPermissionIntro = true,
            isOnboardingCompleted = true
        )
        assertEquals("home", secondDest)
    }

    @Test
    fun testNotificationLaunch_goesToResult() {
        val dest = resolveDestination(
            navigateToExtra = "result",
            currentStatus = ProcessingStatus.COMPLETED,
            hasActiveCompletion = false,
            hasSeenPermissionIntro = false,
            isOnboardingCompleted = false
        )
        assertEquals("result", dest)
    }
}
