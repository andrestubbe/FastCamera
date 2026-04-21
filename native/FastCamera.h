/**
 * @file FastCamera.h
 * @brief FastCamera JNI Header - High-performance camera capture
 *
 * @details Triple-backend camera capture with SIMD acceleration:
 * - MediaFoundation (primary): Windows 7+, modern devices
 * - WinRT (optional): Windows 10+, async APIs
 * - DirectShow (fallback): Legacy compatibility
 *
 * @par Features
 * - Device enumeration across all backends
 * - Format probing (query supported resolutions)
 * - Zero-copy streaming via DirectByteBuffer
 * - SIMD color conversion (YUV→RGBA)
 *
 * @par Architecture
 * - Runtime backend auto-selection
 * - Unified device ID format
 * - Thread-safe capture loop
 *
 * @author FastJava Team
 * @version 1.0.0
 * @copyright MIT License
 */

#ifndef FASTCAMERA_H
#define FASTCAMERA_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/** @defgroup Enumeration Device Enumeration
 *  @brief Camera discovery and format probing
 *  @{ */

/**
 * @brief Enumerate all available cameras
 * @param env JNI environment
 * @return Array of CameraDevice objects
 * @note Tries: WinRT → MediaFoundation → DirectShow
 */
JNIEXPORT jobjectArray JNICALL Java_fastcamera_FastCamera_nativeEnumerateDevices
    (JNIEnv* env, jclass clazz);

/**
 * @brief Get supported formats for a device
 * @param env JNI environment
 * @param deviceId Device unique identifier
 * @return Array of VideoFormat objects
 */
JNIEXPORT jobjectArray JNICALL Java_fastcamera_FastCamera_nativeGetSupportedFormats
    (JNIEnv* env, jclass clazz, jstring deviceId);

/** @} */

/** @defgroup Capture Capture Control
 *  @brief Open, start, stop, close camera
 *  @{ */

/**
 * @brief Open camera device
 * @param env JNI environment
 * @param deviceId Device identifier from enumeration
 * @return Native handle or 0 on failure
 */
JNIEXPORT jlong JNICALL Java_fastcamera_FastCamera_nativeOpen
    (JNIEnv* env, jclass clazz, jstring deviceId);

/**
 * @brief Start capture with specified format
 * @param env JNI environment
 * @param handle Native camera handle
 * @param width Desired width
 * @param height Desired height
 * @param fps Desired framerate
 * @param format Native pixel format (0=YUY2, 1=MJPEG, 2=NV12)
 * @return JNI_TRUE if started
 */
JNIEXPORT jboolean JNICALL Java_fastcamera_FastCamera_nativeStartCapture
    (JNIEnv* env, jclass clazz, jlong handle, jint width, jint height, jint fps, jint format);

/**
 * @brief Stop capture
 * @param env JNI environment
 * @param handle Native camera handle
 */
JNIEXPORT void JNICALL Java_fastcamera_FastCamera_nativeStopCapture
    (JNIEnv* env, jclass clazz, jlong handle);

/**
 * @brief Close camera and release resources
 * @param env JNI environment
 * @param handle Native camera handle
 */
JNIEXPORT void JNICALL Java_fastcamera_FastCamera_nativeClose
    (JNIEnv* env, jclass clazz, jlong handle);

/** @} */

/** @defgroup FrameAccess Frame Access
 *  @brief Get frames via pull or zero-copy
 *  @{ */

/**
 * @brief Get frame (blocking pull model)
 * @param env JNI environment
 * @param handle Native camera handle
 * @param rgbaBuffer Pre-allocated RGBA buffer
 * @return JNI_TRUE if frame copied, JNI_FALSE if no new frame
 */
JNIEXPORT jboolean JNICALL Java_fastcamera_FastCamera_nativeGetFrame
    (JNIEnv* env, jclass clazz, jlong handle, jbyteArray rgbaBuffer);

/**
 * @brief Start zero-copy streaming
 * @param env JNI environment
 * @param handle Native camera handle
 * @param width Frame width
 * @param height Frame height
 * @return DirectByteBuffer pointing to native frame memory
 */
JNIEXPORT jobject JNICALL Java_fastcamera_FastCamera_nativeStartStream
    (JNIEnv* env, jclass clazz, jlong handle, jint width, jint height);

/**
 * @brief Check if new frame available (for zero-copy)
 * @param env JNI environment
 * @param handle Native camera handle
 * @return JNI_TRUE if new frame ready
 */
JNIEXPORT jboolean JNICALL Java_fastcamera_FastCamera_nativeHasNewFrame
    (JNIEnv* env, jclass clazz, jlong handle);

/**
 * @brief Lock frame for reading (thread-safe)
 * @param env JNI environment
 * @param handle Native camera handle
 * @return JNI_TRUE if frame locked
 */
JNIEXPORT jboolean JNICALL Java_fastcamera_FastCamera_nativeLockFrame
    (JNIEnv* env, jclass clazz, jlong handle);

/**
 * @brief Unlock frame after reading
 * @param env JNI environment
 * @param handle Native camera handle
 */
JNIEXPORT void JNICALL Java_fastcamera_FastCamera_nativeUnlockFrame
    (JNIEnv* env, jclass clazz, jlong handle);

/** @} */

/** @defgroup Properties Camera Properties
 *  @brief Exposure, focus, brightness control
 *  @{ */

/**
 * @brief Set camera property
 * @param env JNI environment
 * @param handle Native camera handle
 * @param property Property ID (0=exposure, 1=focus, 2=brightness, 3=contrast, 4=saturation)
 * @param value Property value
 * @param auto Auto mode flag
 * @return JNI_TRUE if set
 */
JNIEXPORT jboolean JNICALL Java_fastcamera_FastCamera_nativeSetProperty
    (JNIEnv* env, jclass clazz, jlong handle, jint property, jint value, jboolean autoMode);

/**
 * @brief Get camera property
 * @param env JNI environment
 * @param handle Native camera handle
 * @param property Property ID
 * @param outValue Output value array [value, min, max, default, flags]
 * @return JNI_TRUE if got
 */
JNIEXPORT jboolean JNICALL Java_fastcamera_FastCamera_nativeGetProperty
    (JNIEnv* env, jclass clazz, jlong handle, jint property, jintArray outValue);

/** @} */

#ifdef __cplusplus
}
#endif

#endif // FASTCAMERA_H
