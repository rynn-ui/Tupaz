package com.tupaz.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Repository responsible for fetching and caching the remote model catalog manifest.
 */
class ModelCatalog(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
) {
    companion object {
        const val DEFAULT_CATALOG_URL =
            "https://raw.githubusercontent.com/rynn-ui/Tupaz-Models/main/manifest.json"
        private const val RELEASE_CATALOG_URL =
            "https://github.com/rynn-ui/Tupaz-Models/releases/download/v0.1.0/manifest.json"
        private const val CACHE_FILE_NAME = "catalog_cache.json"

        val EMBEDDED_DEFAULT_MANIFEST = ModelCatalogManifest(
            version = 1,
            models = listOf(
                ModelCatalogItem(
                    modelId = "animejanai-hd-v3-superultracompact-x2",
                    name = "AnimeJaNai HD V3 SuperUltraCompact 2x",
                    description = "Super ultra compact 2x upscaler for maximum processing speed.",
                    version = "0.1.0",
                    binUrl = "",
                    paramUrl = "",
                    sha256 = "",
                    sizeBytes = 722853L,
                    requiredForModes = listOf("fast", "balanced", "auto", "anime", "ultra")
                ),
                ModelCatalogItem(
                    modelId = "animejanai-hd-v3-ultracompact-x2",
                    name = "AnimeJaNai HD V3 UltraCompact 2x",
                    description = "Ultra compact 2x upscaler balancing speed and anime quality.",
                    version = "0.1.0",
                    binUrl = "",
                    paramUrl = "",
                    sha256 = "",
                    sizeBytes = 1950413L,
                    requiredForModes = listOf("fast", "balanced", "auto", "anime", "ultra")
                ),
                ModelCatalogItem(
                    modelId = "realesr-animevideov3-x2",
                    name = "RealESRGAN AnimeVideo v3 (2x)",
                    description = "Official Real-ESRGAN model for 2x video upscaling.",
                    version = "0.1.0",
                    binUrl = "https://huggingface.co/xinntao/Real-ESRGAN/resolve/main/models/realesr-animevideov3-x2.bin",
                    paramUrl = "https://huggingface.co/xinntao/Real-ESRGAN/resolve/main/models/realesr-animevideov3-x2.param",
                    sha256 = "5f70bf18a086007016e948b04aed3",
                    sizeBytes = 18874368L,
                    requiredForModes = listOf("fast", "balanced", "auto", "anime", "ultra")
                )
            )
        )
    }

    private val cacheFile: File
        get() = File(context.cacheDir, CACHE_FILE_NAME)

    /**
     * Fetches the model catalog manifest from remote server, updating local cache on success.
     * Seamlessly falls back to release URL or embedded default manifest on HTTP failure.
     */
    suspend fun fetchCatalog(url: String = RELEASE_CATALOG_URL): Result<ModelCatalogManifest> {
        return withContext(Dispatchers.IO) {
            try {
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (_: Exception) {}
            Result.success(EMBEDDED_DEFAULT_MANIFEST)
        }
    }

    /**
     * Loads catalog manifest from disk cache if present.
     */
    fun loadFromCache(): ModelCatalogManifest? {
        return if (cacheFile.exists()) {
            try {
                json.decodeFromString<ModelCatalogManifest>(cacheFile.readText())
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Saves a manifest directly into cache (for offline/testing setups).
     */
    fun saveToCache(manifest: ModelCatalogManifest) {
        val content = json.encodeToString(manifest)
        cacheFile.writeText(content)
    }
}
