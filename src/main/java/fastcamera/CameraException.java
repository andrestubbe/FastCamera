/**
 * @file CameraException.java
 * @brief Camera operation exception
 */
package fastcamera;

/**
 * @brief Exception thrown for camera errors
 */
public class CameraException extends RuntimeException {
    public CameraException(String message) {
        super(message);
    }
    
    public CameraException(String message, Throwable cause) {
        super(message, cause);
    }
}
