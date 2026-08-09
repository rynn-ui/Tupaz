package com.tupaz.data.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Local metadata sidecar (`meta.json`) stored alongside model weights.
 */
@Serializable
data class LocalModelMeta(
    @SerialName("model_id") val modelId: String,
    @SerialName("version") val version: String,
    @SerialName("sha256") val sha256: String,
    @SerialName("installed_at") val installedAt: Long,
    @SerialName("size_bytes") val sizeBytes: Long
)
