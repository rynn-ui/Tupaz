#ifndef TUPAZ_JNI_UTILS_H_
#define TUPAZ_JNI_UTILS_H_

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "TupazNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace tupaz {
namespace jni {

/**
 * @brief RAII wrapper for JNI byte array element access.
 * Automatically releases array elements when going out of scope.
 */
class ScopedByteArray {
public:
    ScopedByteArray(JNIEnv* env, jbyteArray array)
        : env_(env), array_(array), ptr_(nullptr) {
        if (array != nullptr) {
            ptr_ = env_->GetByteArrayElements(array, nullptr);
        }
    }

    ~ScopedByteArray() {
        if (array_ != nullptr && ptr_ != nullptr) {
            env_->ReleaseByteArrayElements(array_, ptr_, JNI_ABORT);
        }
    }

    jbyte* get() const { return ptr_; }
    bool valid() const { return ptr_ != nullptr; }

    // Disable copy
    ScopedByteArray(const ScopedByteArray&) = delete;
    ScopedByteArray& operator=(const ScopedByteArray&) = delete;

private:
    JNIEnv* env_;
    jbyteArray array_;
    jbyte* ptr_;
};

} // namespace jni
} // namespace tupaz

#endif // TUPAZ_JNI_UTILS_H_
