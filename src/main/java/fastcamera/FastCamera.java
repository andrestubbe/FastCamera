/**
 * @file FastCamera.java
 * @brief FastCamera main class - High-performance camera capture for Java
 *
 * @details Triple-backend camera capture with automatic selection:
 * - MediaFoundation (Windows 7+)
 * - WinRT (Windows 10+)
 * - DirectShow (Legacy fallback)
 *
 * @par Features
 * - Device enumeration with rich metadata
 * - Format probing (query all supported resolutions)
 * - Zero-copy streaming via DirectByteBuffer
 * - SIMD-accelerated color conversion
 * - Pull (blocking) or Push (callback) modes
 *
 * @par Usage
 * 1. enumerateDevices() - List available cameras
 * 2. open(deviceId) - Open specific camera
 * 3. startCapture() or startStream() - Begin streaming
 * 4. getFrame() or read DirectByteBuffer - Get frames
 * 5. stopCapture() / close() - Cleanup
 *
 * @author FastJava Team
 * @version 1.0.0
 * @copyright MIT License
 */
package fastcamera;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

/**
 * @brief Main FastCamera class for high-performance camera capture
 * @details Provides unified API across MediaFoundation, WinRT, and DirectShow backends.
 * Automatically selects best available backend for each camera.
 */
public class FastCamera {
    
    /** @defgroup Constants Property Constants */
    public static final int PROP_EXPOSURE = 0;      /**< Exposure control */
    public static final int PROP_FOCUS = 1;         /**< Focus control */
    public static final int PROP_BRIGHTNESS = 2;    /**< Brightness */
    public static final int PROP_CONTRAST = 3;      /**< Contrast */
    public static final int PROP_SATURATION = 4;    /**< Saturation */
    public static final int PROP_SHARPNESS = 5;     /**< Sharpness */
    public static final int PROP_GAIN = 6;          /**< Gain/ISO */
    
    /** @defgroup NativeFormat Native Pixel Formats */
    public static final int FORMAT_YUY2 = 0;        /**< YUY2 4:2:2 (most common) */
    public static final int FORMAT_MJPEG = 1;       /**< Motion JPEG (compressed) */
    public static final int FORMAT_NV12 = 2;        /**< NV12 planar */
    public static final int FORMAT_RGB24 = 3;       /**< RGB24 (rare) */
    
    static {
        // Native library loaded by FastCore
    }
    
    private long nativeHandle = 0;
    private ByteBuffer streamBuffer = null;
    private CameraListener listener = null;
    private int width = 0;
    private int height = 0;
    
    /**
     * @brief Camera frame callback interface
     * @details Implemented by applications for async frame delivery.
     * Called on native thread - process quickly or queue.
     */
    public interface CameraListener {
        /**
         * @brief Called when new frame available
         * @param frame RGBA byte array (width * height * 4 bytes)
         * @param width Frame width in pixels
         * @param height Frame height in pixels
         * @param timestamp Frame capture time in nanoseconds
         */
        void onFrame(byte[] frame, int width, int height, long timestamp);
    }
    
    /**
     * @brief Enumerate all available cameras
     * @return List of CameraDevice objects across all backends
     * @note Tries: WinRT → MediaFoundation → DirectShow
     */
    public static native List<CameraDevice> enumerateDevices();
    
    /**
     * @brief Open camera by device ID
     * @param deviceId Device identifier from CameraDevice
     * @return FastCamera instance ready for capture
     * @throws CameraException if device unavailable
     */
    public static FastCamera open(String deviceId) {
        FastCamera camera = new FastCamera();
        camera.nativeHandle = nativeOpen(deviceId);
        if (camera.nativeHandle == 0) {
            throw new CameraException("Failed to open camera: " + deviceId);
        }
        return camera;
    }
    
    private static native long nativeOpen(String deviceId);
    
    /**
     * @brief Start capture with specified parameters
     * @param width Desired frame width
     * @param height Desired frame height  
     * @param fps Target framerate (may be adjusted to available)
     * @return true if capture started
     */
    public boolean startCapture(int width, int height, int fps) {
        return startCapture(width, height, fps, FORMAT_YUY2);
    }
    
    /**
     * @brief Start capture with specific pixel format
     * @param width Desired frame width
     * @param height Desired frame height
     * @param fps Target framerate
     * @param format Native pixel format constant
     * @return true if capture started
     */
    public boolean startCapture(int width, int height, int fps, int format) {
        this.width = width;
        this.height = height;
        return nativeStartCapture(nativeHandle, width, height, fps, format);
    }
    
    private native boolean nativeStartCapture(long handle, int width, int height, int fps, int format);
    
    /**
     * @brief Start zero-copy streaming
     * @param width Frame width
     * @param height Frame height
     * @param fps Target framerate
     * @return DirectByteBuffer mapped to native frame memory
     * @note Fastest option - no JNI array copy
     */
    public ByteBuffer startStream(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        streamBuffer = nativeStartStream(nativeHandle, width, height);
        return streamBuffer;
    }
    
    private native ByteBuffer nativeStartStream(long handle, int width, int height);
    
    /**
     * @brief Check if new frame available (for streaming mode)
     * @return true if unread frame waiting
     */
    public boolean hasNewFrame() {
        return nativeHasNewFrame(nativeHandle);
    }
    
    private native boolean nativeHasNewFrame(long handle);
    
    /**
     * @brief Get frame (blocking pull model)
     * @return RGBA byte array or null if no frame
     * @note Blocks until frame available or timeout
     */
    public byte[] getFrame() {
        byte[] frame = new byte[width * height * 4]; // RGBA
        if (nativeGetFrame(nativeHandle, frame)) {
            return frame;
        }
        return null;
    }
    
    private native boolean nativeGetFrame(long handle, byte[] rgbaBuffer);
    
    /**
     * @brief Set async frame listener
     * @param listener Callback for frame delivery
     * @note Listener called on native thread
     */
    public void setListener(CameraListener listener) {
        this.listener = listener;
    }
    
    /**
     * @brief Stop capture
     */
    public void stopCapture() {
        nativeStopCapture(nativeHandle);
    }
    
    private native void nativeStopCapture(long handle);
    
    /**
     * @brief Close camera and release resources
     */
    public void close() {
        if (nativeHandle != 0) {
            nativeClose(nativeHandle);
            nativeHandle = 0;
        }
        streamBuffer = null;
    }
    
    private native void nativeClose(long handle);
    
    /**
     * @brief Set camera property
     * @param property Property ID (PROP_* constant)
     * @param value Property value
     * @param auto Auto mode
     * @return true if property set
     */
    public boolean setProperty(int property, int value, boolean auto) {
        return nativeSetProperty(nativeHandle, property, value, auto);
    }
    
    private native boolean nativeSetProperty(long handle, int property, int value, boolean auto);
    
    /**
     * @brief Get camera property range
     * @param property Property ID
     * @return Property info [value, min, max, default, flags]
     */
    public PropertyInfo getProperty(int property) {
        int[] values = new int[5];
        if (nativeGetProperty(nativeHandle, property, values)) {
            return new PropertyInfo(values[0], values[1], values[2], values[3], values[4]);
        }
        return null;
    }
    
    private native boolean nativeGetProperty(long handle, int property, int[] outValue);
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    /**
     * @brief Property information container
     */
    public static class PropertyInfo {
        public final int value;     /**< Current value */
        public final int min;       /**< Minimum value */
        public final int max;       /**< Maximum value */
        public final int defaultValue; /**< Default value */
        public final int flags;     /**< Capability flags */
        
        PropertyInfo(int value, int min, int max, int defaultValue, int flags) {
            this.value = value;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.flags = flags;
        }
        
        public boolean isAutoSupported() {
            return (flags & 0x01) != 0;
        }
        
        public boolean isManualSupported() {
            return (flags & 0x02) != 0;
        }
    }
}
