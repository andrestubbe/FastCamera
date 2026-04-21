/**
 * FastCamera Demo - Basic camera capture example
 */
package fastcamera;

import java.util.List;

public class CameraDemo {
    public static void main(String[] args) {
        System.out.println("FastCamera Demo");
        System.out.println("===============");
        
        // Enumerate cameras
        System.out.println("\n1. Enumerating cameras...");
        List<CameraDevice> cameras = FastCamera.enumerateDevices();
        
        if (cameras.isEmpty()) {
            System.out.println("No cameras found!");
            return;
        }
        
        System.out.println("Found " + cameras.size() + " camera(s):");
        for (int i = 0; i < cameras.size(); i++) {
            CameraDevice cam = cameras.get(i);
            System.out.println("  [" + i + "] " + cam.getName() + 
                " (" + cam.getBackend() + ")" +
                (cam.isAvailable() ? "" : " [IN USE]"));
        }
        
        // Find first available camera
        CameraDevice selected = null;
        for (CameraDevice cam : cameras) {
            if (cam.isAvailable()) {
                selected = cam;
                break;
            }
        }
        
        if (selected == null) {
            System.out.println("\nNo available cameras!");
            return;
        }
        
        System.out.println("\n2. Opening camera: " + selected.getName());
        
        try (FastCamera camera = FastCamera.open(selected.getId())) {
            System.out.println("3. Starting capture (640x480 @ 30fps)...");
            camera.startCapture(640, 480, 30);
            
            System.out.println("4. Capturing 100 frames...");
            long startTime = System.nanoTime();
            
            for (int i = 0; i < 100; i++) {
                byte[] frame = camera.getFrame();
                if (frame != null) {
                    // Frame is RGBA format: 640 * 480 * 4 = 1,228,800 bytes
                    int r = frame[0] & 0xFF;
                    int g = frame[1] & 0xFF;
                    int b = frame[2] & 0xFF;
                    
                    if (i % 30 == 0) {
                        System.out.println("  Frame " + i + ": First pixel RGB(" + r + "," + g + "," + b + ")");
                    }
                }
            }
            
            long endTime = System.nanoTime();
            double elapsedSec = (endTime - startTime) / 1_000_000_000.0;
            double fps = 100.0 / elapsedSec;
            
            System.out.println("5. Capture complete!");
            System.out.println("   Time: " + String.format("%.2f", elapsedSec) + "s");
            System.out.println("   Effective FPS: " + String.format("%.1f", fps));
            
            System.out.println("\n6. Stopping capture...");
            camera.stopCapture();
            
        } catch (CameraException e) {
            System.err.println("Camera error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\nDemo complete!");
    }
}
