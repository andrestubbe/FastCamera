/**
 * @file VideoFormat.java
 * @brief Video format specification
 */
package fastcamera;

/**
 * @brief Represents a supported video format
 */
public class VideoFormat {
    private final int width;
    private final int height;
    private final int fps;
    private final int pixelFormat;
    private final String formatName;
    
    public VideoFormat(int width, int height, int fps, int pixelFormat, String formatName) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.pixelFormat = pixelFormat;
        this.formatName = formatName;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getFps() {
        return fps;
    }
    
    public int getPixelFormat() {
        return pixelFormat;
    }
    
    public String getFormatName() {
        return formatName;
    }
    
    public int getFrameSizeRGBA() {
        return width * height * 4;
    }
    
    @Override
    public String toString() {
        return width + "x" + height + " @ " + fps + "fps (" + formatName + ")";
    }
}
