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
            "animejanai-hd-v3-superultracompact-x2" to Pair("animejanai-hd-v3-superultracompact-x2.param", "animejanai-hd-v3-superultracompact-x2.bin"),
            "animejanai-hd-v3-ultracompact-x2" to Pair("animejanai-hd-v3-ultracompact-x2.param", "animejanai-hd-v3-ultracompact-x2.bin"),
            "realesr-animevideov3-x2" to Pair("realesr-animevideov3-x2.param", "realesr-animevideov3-x2.bin")
        )

        for ((id, assetNames) in defaultModels) {
            val param = getParamFile(id)
            val bin = getBinFile(id)
            val meta = getMetaFile(id)

            val needsInstall = !param.exists() || param.length() == 0L ||
                               !bin.exists() || bin.length() == 0L

            if (needsInstall) {
                try {
                    getModelDir(id).mkdirs()

                    context.assets.open("models/${assetNames.first}").use { input ->
                        param.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    context.assets.open("models/${assetNames.second}").use { input ->
                        bin.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.util.Log.i("ModelStorage",
                        "Installed model weights for $id: param=${param.length()} bytes, bin=${bin.length()} bytes")
                } catch (e: Exception) {
                    android.util.Log.e("ModelStorage", "Failed to extract model $id from assets", e)
                }
            }

            if (!meta.exists() && param.exists() && bin.exists()) {
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
     * Checks if a model is fully installed (both .bin AND .param exist and are non-empty).
     * Strictly requires BOTH files to exist with size > 0.
     */
    fun isModelInstalled(modelId: String): Boolean {
        val bin = getBinFile(modelId)
        val param = getParamFile(modelId)
        return bin.exists() && bin.length() > 0L && param.exists() && param.length() > 0L
    }

    /**
     * Self-healing model resolver. Checks if model is installed, and if missing,
     * attempts automatic re-provisioning from bundled assets before returning status.
     */
    fun ensureModelAvailable(modelId: String): Boolean {
        if (!isModelInstalled(modelId)) {
            ensureDefaultModelsProvisioned()
        }
        return isModelInstalled(modelId)
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
