package com.tupaz.data.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigGatingTest {

    private val currentVersion = "0.1.0"

    @Test
    fun testState1_betaClosed_whenBetaEnabledIsFalse() {
        val config = AppConfig(
            betaEnabled = false,
            betaMessage = "App is closed",
            maintenanceMode = true, // Priority test: betaEnabled=false overrides maintenance
            forceUpdate = true
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.BetaClosed)
        assertEquals("App is closed", (state as AppConfigState.BetaClosed).message)
    }

    @Test
    fun testState2_maintenance_whenBetaEnabledIsTrueAndMaintenanceIsTrue() {
        val config = AppConfig(
            betaEnabled = true,
            maintenanceMode = true,
            maintenanceMessage = "Under maintenance",
            forceUpdate = true // Priority test: maintenanceMode=true overrides forceUpdate
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.Maintenance)
        assertEquals("Under maintenance", (state as AppConfigState.Maintenance).message)
    }

    @Test
    fun testState3_optionalUpdate_whenNewerVersionAvailableAndForceUpdateIsFalse() {
        val config = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.2.0",
            updateMessage = "New update available",
            forceUpdate = false
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.UpdateAvailable)
        val available = state as AppConfigState.UpdateAvailable
        assertEquals("New update available", available.message)
        assertEquals("0.2.0", available.latestVersion)
    }

    @Test
    fun testState4_mandatoryUpdate_whenNewerVersionAvailableAndForceUpdateIsTrue() {
        val config = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.2.0",
            updateMessage = "Mandatory update required",
            forceUpdate = true
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.UpdateRequired)
        val required = state as AppConfigState.UpdateRequired
        assertEquals("Mandatory update required", required.message)
        assertEquals("0.2.0", required.latestVersion)
    }

    @Test
    fun testState5_normalApp_whenVersionIsUpToDate() {
        val config = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.1.0",
            forceUpdate = false
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.Ready)
    }

    @Test
    fun testPriorityOrder_betaClosedOverridesEverything() {
        val config = AppConfig(
            betaEnabled = false,
            betaMessage = "Closed Priority Test",
            maintenanceMode = true,
            latestVersion = "99.0.0",
            forceUpdate = true
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.BetaClosed)
    }

    @Test
    fun testPriorityOrder_maintenanceOverridesUpdate() {
        val config = AppConfig(
            betaEnabled = true,
            maintenanceMode = true,
            maintenanceMessage = "Maintenance Priority Test",
            latestVersion = "99.0.0",
            forceUpdate = true
        )
        val state = FirebaseConfigManager.evaluateState(config, currentVersion)
        assertTrue(state is AppConfigState.Maintenance)
    }

    @Test
    fun testSemanticVersionComparison() {
        // 0.10.0 > 0.2.0 -> false (0.1.0 is lower than 0.10.0, so 0.1.0 < 0.10.0 is true)
        assertTrue(FirebaseConfigManager.isVersionLower("0.1.0", "0.10.0"))
        assertFalse(FirebaseConfigManager.isVersionLower("0.10.0", "0.2.0"))

        // Standard comparisons
        assertTrue(FirebaseConfigManager.isVersionLower("0.1.0", "0.2.0"))
        assertFalse(FirebaseConfigManager.isVersionLower("0.2.0", "0.1.0"))
        assertFalse(FirebaseConfigManager.isVersionLower("0.1.0", "0.1.0"))
        assertTrue(FirebaseConfigManager.isVersionLower("1.9.9", "2.0.0"))
    }

    @Test
    fun testStaleCacheTransition_whenFirebaseDisablesBeta() {
        val staleCacheConfig = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.1.0"
        )
        val initialCachedState = FirebaseConfigManager.evaluateState(staleCacheConfig, currentVersion)
        assertTrue("Initial stale cache evaluates to Ready", initialCachedState is AppConfigState.Ready)

        // Remote Firestore fetch completes and returns betaEnabled = false
        val remoteConfig = AppConfig(
            betaEnabled = false,
            betaMessage = "App is closed remotely by admin"
        )
        val updatedState = FirebaseConfigManager.evaluateState(remoteConfig, currentVersion)
        assertTrue("Updated remote config immediately transitions state to BetaClosed", updatedState is AppConfigState.BetaClosed)
        assertEquals("App is closed remotely by admin", (updatedState as AppConfigState.BetaClosed).message)
    }

    @Test
    fun testForceUpdateIgnored_whenInstalledVersionEqualOrNewer() {
        val configEqual = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.2.0",
            forceUpdate = true
        )
        val stateEqual = FirebaseConfigManager.evaluateState(configEqual, "0.2.0")
        assertTrue("forceUpdate must be ignored when installed version equals remote version", stateEqual is AppConfigState.Ready)

        val configNewer = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.2.0",
            forceUpdate = true
        )
        val stateNewer = FirebaseConfigManager.evaluateState(configNewer, "0.3.0")
        assertTrue("forceUpdate must be ignored when installed version is newer than remote version", stateNewer is AppConfigState.Ready)
    }

    @Test
    fun testFreshInstall_withNoCache_defaultsToOpenGate() {
        val defaultConfig = AppConfig()
        assertTrue("Default config for fresh install must have betaEnabled = true", defaultConfig.betaEnabled)
        assertFalse("Default config for fresh install must have maintenanceMode = false", defaultConfig.maintenanceMode)

        val state = FirebaseConfigManager.evaluateState(defaultConfig, "0.1.0")
        assertTrue("Default fresh install config evaluates to Ready (OPEN gate)", state is AppConfigState.Ready)
    }

    @Test
    fun testUpdateUrlAndMessageExtractedFromConfig() {
        val config = AppConfig(
            betaEnabled = true,
            maintenanceMode = false,
            latestVersion = "0.2.0",
            updateMessage = "Custom Firestore update message",
            updateUrl = "https://custom-update-link.com/app",
            forceUpdate = false
        )
        val state = FirebaseConfigManager.evaluateState(config, "0.1.0")
        assertTrue(state is AppConfigState.UpdateAvailable)
        val available = state as AppConfigState.UpdateAvailable
        assertEquals("Custom Firestore update message", available.message)
        assertEquals("https://custom-update-link.com/app", available.updateUrl)
    }
}
