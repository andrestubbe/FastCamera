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

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

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
    public static final int FORMAT_BGRA = 4;        /**< BGRA (direct to Java, no conversion) */
    
    static {
        System.out.println("[DEBUG] FastCamera static initializer");
        try {
            // Try multiple methods to load the DLL
            String[] paths = {
                "fastcamera",  // loadLibrary
                "target/classes/fastcamera.dll",
                "../../../build/fastcamera.dll",
                "C:/Users/andre/Documents/FastJava/2026-04-21-Work-FastCamera/build/fastcamera.dll"
            };
            
            for (String path : paths) {
                try {
                    System.out.println("[DEBUG] Trying to load: " + path);
                    if (path.endsWith(".dll")) {
                        System.load(new java.io.File(path).getAbsolutePath());
                    } else {
                        System.loadLibrary(path);
                    }
                    System.out.println("[DEBUG] Successfully loaded: " + path);
                    break;
                } catch (Exception e) {
                    System.out.println("[DEBUG] Failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] All load attempts failed: " + e.getMessage());
            e.printStackTrace();
        }
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
    public static List<CameraDevice> enumerateDevices() {
        CameraDevice[] devices = nativeEnumerateDevices();
        return devices != null ? java.util.Arrays.asList(devices) : new java.util.ArrayList<>();
    }
    
    private static native CameraDevice[] nativeEnumerateDevices();
    
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
        return startStream(width, height, fps, FORMAT_YUY2);
    }
    
    /**
     * @brief Start zero-copy streaming with specific format
     * @param width Frame width
     * @param height Frame height
     * @param fps Target framerate
     * @param format Pixel format (FORMAT_YUY2, FORMAT_NV12, FORMAT_MJPEG)
     * @return DirectByteBuffer mapped to native frame memory
     * @note Fastest option - no JNI array copy
     */
    public ByteBuffer startStream(int width, int height, int fps, int format) {
        streamBuffer = nativeStartStream(nativeHandle, width, height, fps, format);
        // Get actual dimensions (camera may provide different resolution)
        this.width = nativeGetActualWidth(nativeHandle);
        this.height = nativeGetActualHeight(nativeHandle);
        System.out.println("[DEBUG] Camera actual resolution: " + this.width + "x" + this.height);
        return streamBuffer;
    }
    
    private native ByteBuffer nativeStartStream(long handle, int width, int height, int fps, int format);
    private native int nativeGetActualWidth(long handle);
    private native int nativeGetActualHeight(long handle);
    
    /**
     * @brief Get actual width from camera (may differ from requested)
     * @return Actual frame width
     */
    public int getWidth() { return width; }
    
    /**
     * @brief Get actual height from camera (may differ from requested)
     * @return Actual frame height
     */
    public int getHeight() { return height; }
    
    /**
     * @brief Take a picture/snapshot of current camera frame
     * @return BufferedImage containing the current frame, or null if no frame available
     */
    public BufferedImage takePicture() {
        if (!hasNewFrame()) {
            return null; // No frame available
        }
        
        lockFrame();
        try {
            byte[] frameData = getFrame();
            if (frameData == null) {
                return null;
            }
            
            int width = getWidth();
            int height = getHeight();
            
            // Create BufferedImage from frame data (BGR format)
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] imagePixels = ((java.awt.image.DataBufferByte)image.getRaster().getDataBuffer()).getData();
            System.arraycopy(frameData, 0, imagePixels, 0, Math.min(frameData.length, imagePixels.length));
            
            return image;
        } finally {
            unlockFrame();
        }
    }
    
    /**
     * @brief Check if new frame available (for streaming mode)
     * @return true if unread frame waiting
     */
    public boolean hasNewFrame() {
        return nativeHasNewFrame(nativeHandle);
    }
    
    private native boolean nativeHasNewFrame(long handle);
    
    /**
     * @brief Lock frame buffer for reading (thread-safe)
     * @note Always pair with unlockFrame()
     */
    public void lockFrame() {
        nativeLockFrame(nativeHandle);
    }
    
    private native void nativeLockFrame(long handle);
    
    /**
     * @brief Unlock frame buffer after reading
     */
    public void unlockFrame() {
        nativeUnlockFrame(nativeHandle);
    }
    
    private native void nativeUnlockFrame(long handle);
    
    /**
     * @brief Get frame (blocking pull model)
     * @return BGR byte array or null if no frame
     * @note Blocks until frame available or timeout
     */
    public byte[] getFrame() {
        byte[] frame = new byte[width * height * 3]; // BGR format
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
