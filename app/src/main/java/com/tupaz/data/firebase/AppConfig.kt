package com.tupaz.data.firebase

import kotlinx.serialization.Serializable

/**
 * Model representing the config/app Firestore document fields.
 */
@Serializable
data class AppConfig(
    val betaEnabled: Boolean = true,
    val betaMessage: String = "TUPAZ is currently closed for private beta testing.",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "TUPAZ engine is currently undergoing scheduled engine maintenance.",
    val latestVersion: String = "0.1.0",
    val updateMessage: String = "A new version of TUPAZ is available. Update now to get the latest features and improvements.",
    val forceUpdate: Boolean = false,
    val updateUrl: String = ""
)
