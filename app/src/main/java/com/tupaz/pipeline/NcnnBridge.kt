package com.tupaz.pipeline

import android.util.Log

/**
 * Kotlin bridge wrapper for `libtupaz.so` native pipeline calls.
 * All direct JNI invocations are encapsulated here to prevent leaks across architectural layers.
 */
open class NcnnBridge {

    companion object {
        private const val TAG = "NcnnBridge"

        init {
            try {
                val libName = System.mapLibraryName("tupaz")
                System.loadLibrary("tupaz")
                Log.i(TAG, """
                    [Tupaz] Loaded native library:
                    Library name: $libName
                    Build timestamp: 2026-08-04T22:23:00Z
                """.trimIndent())
            } catch (e: Throwable) {
                Log.e(TAG, "[Tupaz] Failed to load native library", e)
            }
        }
    }

    /**
     * Initializes the native pipeline engine and GPU context.
     * @param useGpu Whether GPU acceleration should be enabled.
     * @return True if initialized successfully.
     */
    fun init(useGpu: Boolean = true): Boolean {
        return try {
            nativeInit(useGpu)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeInit symbol not found", e)
            false
        }
    }

    /**
     * Initializes the native NCNN engine with specific model file paths.
     * @param paramPath Absolute path to .param file.
     * @param binPath Absolute path to .bin file.
     * @param useGpu Whether Vulkan GPU compute is enabled.
     * @return True if initialized successfully.
     */
    fun initModel(paramPath: String, binPath: String, useGpu: Boolean = true): Boolean {
        return try {
            nativeInitModel(paramPath, binPath, useGpu)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeInitModel symbol not found", e)
            false
        }
    }

    /**
     * Destroys the native pipeline engine and releases all allocated native handles.
     */
    fun destroy() {
        try {
            nativeDestroy()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeDestroy symbol not found", e)
        }
    }

    /**
     * Processes a single raw frame through the native NCNN pipeline.
     * @param inputFrame Raw RGBA frame buffer bytes.
     * @param width Width in pixels.
     * @param height Height in pixels.
     * @param scaleFactor Scaling multiplier (2 or 4).
     * @param mode Processing mode integer identifier.
     * @return Processed byte array or null on error.
     */
    open fun processFrame(
        inputFrame: ByteArray,
        width: Int,
        height: Int,
        scaleFactor: Int = 2,
        mode: Int = 0
    ): ByteArray? {
        return try {
            nativeProcessFrame(inputFrame, width, height, scaleFactor, mode)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeProcessFrame symbol not found", e)
            null
        }
    }

    /**
     * Queries current VRAM usage in megabytes from the native engine.
     * @return VRAM usage in megabytes.
     */
    open fun getVramUsage(): Long {
        return try {
            nativeGetVramUsage()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeGetVramUsage symbol not found", e)
            0L
        }
    }

    /**
     * Scores frame quality (blocking, noise, sharpness, overall) in native code (<1ms).
     * @return Float array [blockingScore, noiseLevel, sharpnessScore, overallQuality] or null.
     */
    open fun scoreFrameQuality(inputFrame: ByteArray, width: Int, height: Int): FloatArray? {
        return try {
            nativeScoreFrameQuality(inputFrame, width, height)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "nativeScoreFrameQuality symbol not found", e)
            null
        }
    }

    private external fun nativeInit(useGpu: Boolean): Boolean
    private external fun nativeInitModel(paramPath: String, binPath: String, useGpu: Boolean): Boolean
    private external fun nativeDestroy()
    private external fun nativeProcessFrame(
        inputFrame: ByteArray,
        width: Int,
        height: Int,
        scaleFactor: Int,
        mode: Int
    ): ByteArray?
    private external fun nativeGetVramUsage(): Long
    private external fun nativeScoreFrameQuality(
        inputFrame: ByteArray,
        width: Int,
        height: Int
    ): FloatArray?
}
