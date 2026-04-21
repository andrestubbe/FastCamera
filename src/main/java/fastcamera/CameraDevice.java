/**
 * @file CameraDevice.java
 * @brief Camera device information
 */
package fastcamera;

import java.util.List;

/**
 * @brief Represents a discovered camera device
 * @details Contains metadata for a camera from any backend.
 */
public class CameraDevice {
    private final String id;
    private final String name;
    private final String backend;
    private final boolean available;
    private final List<VideoFormat> formats;
    
    public CameraDevice(String id, String name, String backend, boolean available, List<VideoFormat> formats) {
        this.id = id;
        this.name = name;
        this.backend = backend;
        this.available = available;
        this.formats = formats;
    }
    
    /**
     * @return Unique device identifier (use with FastCamera.open())
     */
    public String getId() {
        return id;
    }
    
    /**
     * @return Friendly name (e.g., "Logitech C920")
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return Backend name ("MediaFoundation", "WinRT", "DirectShow")
     */
    public String getBackend() {
        return backend;
    }
    
    /**
     * @return true if camera is not in use by another application
     */
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * @return List of supported video formats
     */
    public List<VideoFormat> getSupportedFormats() {
        return formats;
    }
    
    /**
     * @brief Find best format matching requirements
     * @param minWidth Minimum width
     * @param minHeight Minimum height
     * @param minFps Minimum FPS
     * @return Best matching format or null
     */
    public VideoFormat findBestFormat(int minWidth, int minHeight, int minFps) {
        VideoFormat best = null;
        for (VideoFormat fmt : formats) {
            if (fmt.getWidth() >= minWidth && fmt.getHeight() >= minHeight && fmt.getFps() >= minFps) {
                if (best == null || fmt.getWidth() < best.getWidth()) {
                    best = fmt;
                }
            }
        }
        return best;
    }
    
    @Override
    public String toString() {
        return name + " [" + backend + "]" + (available ? "" : " (in use)");
    }
}
