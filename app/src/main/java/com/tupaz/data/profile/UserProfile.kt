package com.tupaz.data.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val age: Int = 0,
    val appVersion: String = "0.1.0",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val firstInstall: Long = 0L,
    val lastSeen: Long = 0L,
    val betaUser: Boolean = true,
    val premium: Boolean = false,
    val pendingSync: Boolean = true,
    val lastSyncAttempt: Long = 0L
)
