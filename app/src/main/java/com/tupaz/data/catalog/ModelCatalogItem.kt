package com.tupaz.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Metadata definition for a remote model hosted in GitHub Releases repository.
 */
@Serializable
data class ModelCatalogItem(
    @SerialName("model_id") val modelId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("version") val version: String,
    @SerialName("bin_url") val binUrl: String,
    @SerialName("param_url") val paramUrl: String,
    @SerialName("sha256") val sha256: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("required_for_modes") val requiredForModes: List<String> = emptyList()
)

/**
 * Root catalog manifest structure.
 */
@Serializable
data class ModelCatalogManifest(
    @SerialName("version") val version: Int,
    @SerialName("models") val models: List<ModelCatalogItem>
)
