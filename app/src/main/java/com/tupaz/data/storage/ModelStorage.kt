package com.tupaz.data.storage

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages model files on device storage according to ADR-0002 and ADR-0003.
 */
class ModelStorage(
    private val context: Context,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) {

    /**
     * Resolves the primary root directory for models.
     * Prefers external files dir (`/storage/.../Android/data/com.tupaz/files/models`),
     * falling back to internal storage (`/data/user/0/com.tupaz/files/models`).
     */
    fun getModelsRootDir(): File {
        val externalDir = context.getExternalFilesDir("models")
        val root = externalDir ?: File(context.filesDir, "models")
        if (!root.exists()) {
            root.mkdirs()
        }
        return root
    }

    /**
     * Resolves the directory for a specific model by ID.
     */
    fun getModelDir(modelId: String): File {
        val dir = File(getModelsRootDir(), modelId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Resolves `.param` file path for a model.
     */
    fun getParamFile(modelId: String): File = File(getModelDir(modelId), "model.param")

    /**
     * Resolves `.bin` file path for a model.
     */
    fun getBinFile(modelId: String): File = File(getModelDir(modelId), "model.bin")

    /**
     * Resolves temporary download file (`.bin.tmp`) path.
     */
    fun getTempBinFile(modelId: String): File = File(getModelDir(modelId), "model.bin.tmp")

    /**
     * Resolves metadata sidecar file (`meta.json`) path.
     */
    fun getMetaFile(modelId: String): File = File(getModelDir(modelId), "meta.json")

    /**
     * Ensures default RealESRGAN AnimeVideo v3 models are provisioned on device storage.
     * Copies real pre-trained weights from bundled assets/models/ on first launch.
     */
    fun ensureDefaultModelsProvisioned() {
        val defaultModels = mapOf(
            "realesr-animevideov3-x2" to Pair("realesr-animevideov3-x2.param", "realesr-animevideov3-x2.bin")
        )

        for ((id, assetNames) in defaultModels) {
            val param = getParamFile(id)
            val bin = getBinFile(id)
            val meta = getMetaFile(id)

            // Check if real weights are already installed (param > 1KB means real, not dummy)
            val needsInstall = !param.exists() || param.length() < 1024L ||
                               !bin.exists() || bin.length() < 100_000L

            if (needsInstall) {
                // Copy real .param from assets
                try {
                    context.assets.open("models/${assetNames.first}").use { input ->
                        param.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Copy real .bin from assets
                    context.assets.open("models/${assetNames.second}").use { input ->
                        bin.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.util.Log.i("ModelStorage",
                        "Installed real model weights for $id: param=${param.length()} bytes, bin=${bin.length()} bytes")
                } catch (e: Exception) {
                    android.util.Log.e("ModelStorage", "Failed to extract model $id from assets", e)
                }
            }

            if (!meta.exists()) {
                val localMeta = LocalModelMeta(
                    modelId = id,
                    version = "0.2.5",
                    sha256 = "",
                    installedAt = System.currentTimeMillis(),
                    sizeBytes = bin.length() + param.length()
                )
                writeLocalMeta(localMeta)
            }
        }
    }

    /**
     * Checks if a model is fully installed (both .bin, .param, and valid meta.json exist).
     */
    fun isModelInstalled(modelId: String): Boolean {
        val bin = getBinFile(modelId)
        val param = getParamFile(modelId)
        val meta = getMetaFile(modelId)
        return bin.exists() && bin.length() > 0 && param.exists() && meta.exists()
    }

    /**
     * Reads local metadata sidecar (`meta.json`) for a model if installed.
     */
    fun getLocalMeta(modelId: String): LocalModelMeta? {
        val metaFile = getMetaFile(modelId)
        if (!metaFile.exists()) return null
        return try {
            json.decodeFromString<LocalModelMeta>(metaFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes local metadata sidecar (`meta.json`) for a model.
     */
    fun writeLocalMeta(meta: LocalModelMeta) {
        val metaFile = getMetaFile(meta.modelId)
        val content = json.encodeToString(meta)
        metaFile.writeText(content)
    }

    /**
     * Deletes all local files associated with a model.
     * @return True if deletion succeeded.
     */
    fun deleteModel(modelId: String): Boolean {
        val dir = getModelDir(modelId)
        return if (dir.exists()) dir.deleteRecursively() else true
    }

    /**
     * Calculates total storage footprint of installed models in bytes.
     */
    fun getTotalStorageSizeBytes(): Long {
        val root = getModelsRootDir()
        if (!root.exists()) return 0L
        return root.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    /**
     * Lists all installed model IDs on device.
     */
    fun getInstalledModelIds(): List<String> {
        val root = getModelsRootDir()
        val dirs = root.listFiles { file -> file.isDirectory } ?: return emptyList()
        return dirs.filter { isModelInstalled(it.name) }.map { it.name }
    }
}
