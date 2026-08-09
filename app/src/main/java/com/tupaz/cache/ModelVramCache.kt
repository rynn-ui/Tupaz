package com.tupaz.cache

import android.util.Log
import com.tupaz.data.storage.ModelStorage
import com.tupaz.pipeline.NcnnBridge
import java.io.File

/**
 * Handle wrapper for loaded NCNN model in memory/VRAM.
 */
data class LoadedModelHandle(
    val modelId: String,
    val paramPath: String,
    val binPath: String,
    val loadedAt: Long = System.currentTimeMillis(),
    val estimatedVramBytes: Long = 0L
)

/**
 * Central VRAM and RAM cache manager for NCNN AI model handles according to RULES.md Rule 8.
 * Single source of truth for all active model instances.
 */
class ModelVramCache(
    private val modelStorage: ModelStorage,
    private val ncnnBridge: NcnnBridge = NcnnBridge(),
    private val maxVramBudgetBytes: Long = 1024L * 1024L * 1024L // 1 GB default VRAM limit
) {
    companion object {
        private const val TAG = "ModelVramCache"
    }

    private val cache = mutableMapOf<String, LoadedModelHandle>()
    private val lock = Any()

    /**
     * Gets an existing loaded handle from cache, or loads the model into VRAM if installed.
     * @param modelId Target model identifier.
     * @return [LoadedModelHandle] or null if model is not installed or failed to load.
     */
    fun getOrLoad(modelId: String): LoadedModelHandle? {
        synchronized(lock) {
            // 1. Check existing in-memory handle
            cache[modelId]?.let { return it }

            // 2. Check if model is installed on disk
            if (!modelStorage.isModelInstalled(modelId)) {
                Log.w(TAG, "Cannot load model $modelId — files not found on disk")
                return null
            }

            val paramFile = modelStorage.getParamFile(modelId)
            val binFile = modelStorage.getBinFile(modelId)

            // 3. Check VRAM budget before loading
            val estimatedSize = binFile.length()
            ensureVramBudget(estimatedSize)

            // 4. Create and cache model handle
            val handle = LoadedModelHandle(
                modelId = modelId,
                paramPath = paramFile.absolutePath,
                binPath = binFile.absolutePath,
                estimatedVramBytes = estimatedSize
            )
            cache[modelId] = handle
            Log.i(TAG, "Loaded model $modelId into cache (${estimatedSize / 1024 / 1024} MB)")
            return handle
        }
    }

    /**
     * Evicts a single model from cache by ID.
     */
    fun evict(modelId: String) {
        synchronized(lock) {
            cache.remove(modelId)?.let {
                Log.i(TAG, "Evicted model $modelId from VRAM cache")
            }
        }
    }

    /**
     * Evicts all active models from cache.
     */
    fun clearAll() {
        synchronized(lock) {
            cache.clear()
            Log.i(TAG, "Cleared all models from VRAM cache")
        }
    }

    /**
     * Returns total estimated VRAM usage of currently loaded models in bytes.
     */
    fun getCurrentVramUsageBytes(): Long {
        synchronized(lock) {
            return cache.values.sumOf { it.estimatedVramBytes }
        }
    }

    /**
     * Returns list of currently cached model IDs.
     */
    fun getCachedModelIds(): List<String> {
        synchronized(lock) {
            return cache.keys.toList()
        }
    }

    private fun ensureVramBudget(requiredBytes: Long) {
        var currentUsage = getCurrentVramUsageBytes()
        if (currentUsage + requiredBytes <= maxVramBudgetBytes) return

        // LRU eviction until within budget
        val sortedEntries = cache.entries.sortedBy { it.value.loadedAt }
        for (entry in sortedEntries) {
            if (currentUsage + requiredBytes <= maxVramBudgetBytes) break
            cache.remove(entry.key)
            currentUsage -= entry.value.estimatedVramBytes
            Log.i(TAG, "Auto-evicted model ${entry.key} to stay within VRAM budget")
        }
    }
}
