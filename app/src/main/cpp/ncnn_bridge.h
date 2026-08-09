#ifndef TUPAZ_NCNN_BRIDGE_H_
#define TUPAZ_NCNN_BRIDGE_H_

#include <jni.h>
#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Initialize native engine and GPU instance.
 * @param env JNI environment pointer.
 * @param clazz JNI class instance.
 * @param use_gpu Flag indicating whether to initialize GPU instance.
 * @return jboolean True if initialization succeeded, false otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeInit(
    JNIEnv* env,
    jobject clazz,
    jboolean use_gpu
);

/**
 * @brief Initialize native engine with specific model files.
 * @param env JNI environment pointer.
 * @param clazz JNI class instance.
 * @param param_path Path to model .param file.
 * @param bin_path Path to model .bin file.
 * @param use_gpu Flag indicating whether to use GPU acceleration.
 * @return jboolean True if initialization succeeded.
 */
JNIEXPORT jboolean JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeInitModel(
    JNIEnv* env,
    jobject clazz,
    jstring param_path,
    jstring bin_path,
    jboolean use_gpu
);

/**
 * @brief Destroy native engine state and free resources.
 * @param env JNI environment pointer.
 * @param clazz JNI class instance.
 */
JNIEXPORT void JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeDestroy(
    JNIEnv* env,
    jobject clazz
);

/**
 * @brief Process frame data via native NCNN inference pipeline.
 * @param env JNI environment pointer.
 * @param clazz JNI class instance.
 * @param input_frame Raw RGBA frame byte array.
 * @param width Input frame width in pixels.
 * @param height Input frame height in pixels.
 * @param scale_factor Scaling multiplier (2 or 4).
 * @param mode Processing mode integer code.
 * @return jbyteArray Upscaled output RGBA frame byte array or nullptr on error.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeProcessFrame(
    JNIEnv* env,
    jobject clazz,
    jbyteArray input_frame,
    jint width,
    jint height,
    jint scale_factor,
    jint mode
);

/**
 * @brief Query current VRAM usage in megabytes.
 * @param env JNI environment pointer.
 * @param clazz JNI class instance.
 * @return jlong VRAM usage in megabytes.
 */
JNIEXPORT jlong JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeGetVramUsage(
    JNIEnv* env,
    jobject clazz
);

/**
 * @brief Fast single-pass frame quality scoring.
 * @param env JNI environment pointer.
 * @param clazz JNI class instance.
 * @param input_frame Raw RGBA frame byte array.
 * @param width Input frame width in pixels.
 * @param height Input frame height in pixels.
 * @return jfloatArray Array of 4 floats: [blockingScore, noiseLevel, sharpnessScore, overallQuality]
 */
JNIEXPORT jfloatArray JNICALL
Java_com_tupaz_pipeline_NcnnBridge_nativeScoreFrameQuality(
    JNIEnv* env,
    jobject clazz,
    jbyteArray input_frame,
    jint width,
    jint height
);


#ifdef __cplusplus
}
#endif

#endif // TUPAZ_NCNN_BRIDGE_H_
