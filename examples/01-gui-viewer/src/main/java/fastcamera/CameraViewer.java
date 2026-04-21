/**
 * FastCamera GUI Viewer - Live camera preview with JFrame
 * 
 * Displays real-time camera feed in a Swing window.
 * Uses zero-copy DirectByteBuffer for maximum performance.
 */
package fastcamera;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.List;

public class CameraViewer extends JFrame {
    
    private CameraPanel cameraPanel;
    private FastCamera camera;
    private volatile boolean running = false;
    private Thread captureThread;
    private int pictureCounter = 0;
    
    public CameraViewer() {
        super("FastCamera - Initializing...");
        
        // Setup window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);
        
        // Add keyboard listener for picture taking (SPACE key)
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    takePicture();
                }
            }
        });
        setFocusable(true);
        
        // Camera display using custom panel for 60fps performance
        cameraPanel = new CameraPanel();
        cameraPanel.setBackground(Color.BLACK);
        add(cameraPanel, BorderLayout.CENTER);
        
        // No bottom label - status shown in title
        
        setVisible(true);
        
        // Start camera in background
        SwingUtilities.invokeLater(() -> {
            try {
                startCamera();
            } catch (Exception e) {
                setTitle("FastCamera - Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Failed to start camera: " + e.getMessage(),
                    "Camera Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
    
    private void startCamera() {
        System.out.println("[DEBUG] CameraViewer.startCamera() called");
        setTitle("FastCamera - Enumerating cameras...");
        
        // Enumerate cameras
        System.out.println("[DEBUG] Calling FastCamera.enumerateDevices()...");
        List<CameraDevice> cameras = FastCamera.enumerateDevices();
        System.out.println("[DEBUG] enumerateDevices returned " + cameras.size() + " cameras");
        if (cameras.isEmpty()) {
            setTitle("FastCamera - No cameras found");
            throw new CameraException("No cameras found");
        }
        
        // Show camera selection dialog if multiple cameras
        setTitle("FastCamera - Selecting camera...");
        CameraDevice selected;
        if (cameras.size() == 1) {
            selected = cameras.get(0);
        } else {
            // Let user pick
            String[] names = cameras.stream()
                .map(c -> c.getName())
                .toArray(String[]::new);
            String picked = (String) JOptionPane.showInputDialog(
                this,
                "Select camera:",
                "Camera Selection",
                JOptionPane.QUESTION_MESSAGE,
                null,
                names,
                names[0]
            );
            if (picked == null) {
                throw new CameraException("No camera selected");
            }
            selected = cameras.stream()
                .filter(c -> c.getName().equals(picked))
                .findFirst()
                .orElse(cameras.get(0));
        }
        
        System.out.println("[DEBUG] Using camera: " + selected.getName());
        setTitle("FastCamera - Opening " + selected.getName() + "... (Press SPACE for picture)");
        
        // Open and start
        System.out.println("[DEBUG] Opening camera...");
        camera = FastCamera.open(selected.getId());
        System.out.println("[DEBUG] Camera opened, starting stream...");
        
        // Try multiple format/resolution combinations for best FPS
        // Order: BGRA first (no conversion), then MJPEG (compressed), then YUY2
        int[][] configs = {
            {320, 240, 60, FastCamera.FORMAT_BGRA},   // Try BGRA 60fps (no conversion)
            {320, 240, 30, FastCamera.FORMAT_BGRA},   // BGRA 30fps
            {320, 240, 60, FastCamera.FORMAT_MJPEG},  // MJPEG 60fps
            {320, 240, 30, FastCamera.FORMAT_MJPEG},  // MJPEG 30fps  
            {640, 360, 30, FastCamera.FORMAT_BGRA},   // BGRA higher res
            {320, 240, 30, FastCamera.FORMAT_YUY2},   // Fallback to YUY2
        };
        
        int width = 640, height = 360, fps = 30;
        final ByteBuffer[] frameBufferHolder = new ByteBuffer[1];
        
        for (int[] config : configs) {
            width = config[0];
            height = config[1];
            fps = config[2];
            int format = config[3];
            String fmtName = format == FastCamera.FORMAT_MJPEG ? "MJPEG" : "YUY2";
            System.out.println("[DEBUG] Trying " + width + "x" + height + " @ " + fps + "fps " + fmtName);
            try {
                frameBufferHolder[0] = camera.startStream(width, height, fps, format);
                if (frameBufferHolder[0] != null) {
                    System.out.println("[DEBUG] Success with " + fmtName);
                    break;
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Failed: " + e.getMessage());
            }
        }
        
        if (frameBufferHolder[0] == null) {
            throw new CameraException("No supported format found");
        }
        
        final ByteBuffer frameBuffer = frameBufferHolder[0];
        
        // Use ACTUAL dimensions from camera (may differ from requested)
        final int actualWidth = camera.getWidth();
        final int actualHeight = camera.getHeight();
        System.out.println("[DEBUG] Using actual camera resolution: " + actualWidth + "x" + actualHeight);
        
        // Use actual dimensions for display
        width = actualWidth;
        height = actualHeight;
        System.out.println("[DEBUG] Stream started");
        running = true;
        setTitle("FastCamera - " + selected.getName() + " (" + width + "x" + height + " @ " + fps + "fps)");
        
        // Setup camera display size
        cameraPanel.setPreferredSize(new Dimension(width, height));
        pack();
        
        // Capture loop in separate thread - optimized for speed with detailed FPS tracking
        captureThread = new Thread(() -> {
            System.out.println("[DEBUG] Capture thread started - optimized");
            
            // Pre-allocate images for double buffering
            BufferedImage image1 = new BufferedImage(actualWidth, actualHeight, BufferedImage.TYPE_3BYTE_BGR);
            BufferedImage image2 = new BufferedImage(actualWidth, actualHeight, BufferedImage.TYPE_3BYTE_BGR);
            boolean useFirst = true;
            
            // Get direct access to raster data
            byte[] pixels1 = ((DataBufferByte) image1.getRaster().getDataBuffer()).getData();
            byte[] pixels2 = ((DataBufferByte) image2.getRaster().getDataBuffer()).getData();
            
            // FPS tracking variables
            long frameCount = 0;
            long totalFrames = 0;
            long startTime = System.currentTimeMillis();
            long lastReportTime = startTime;
            
            // Track individual operation times
            long totalCaptureTime = 0;
            long totalRenderTime = 0;
            
            while (running) {
                try {
                    if (camera.hasNewFrame()) {
                        long captureStart = System.nanoTime();
                        
                        // Lock frame for reading
                        camera.lockFrame();
                        try {
                            // Double buffer: prepare next frame while showing current
                            final BufferedImage nextImage = useFirst ? image1 : image2;
                            final byte[] nextPixels = useFirst ? pixels1 : pixels2;
                            
                            // Fast copy from DirectByteBuffer
                            frameBuffer.get(nextPixels);
                            frameBuffer.rewind();
                            
                            long captureEnd = System.nanoTime();
                            totalCaptureTime += (captureEnd - captureStart);
                            
                            // Update display (on EDT)
                            long renderStart = System.nanoTime();
                            SwingUtilities.invokeLater(() -> {
                                cameraPanel.setImage(nextImage);
                            });
                            totalRenderTime += (System.nanoTime() - renderStart);
                            
                            useFirst = !useFirst;
                            frameCount++;
                            totalFrames++;
                        } finally {
                            camera.unlockFrame();
                        }
                    } else {
                        // Small delay to prevent busy-wait
                        Thread.sleep(1);
                    }
                    
                    // Print FPS every second with detailed timing
                    long now = System.currentTimeMillis();
                    if (now - lastReportTime >= 1000) {
                        long elapsed = now - lastReportTime;
                        double currentFps = frameCount * 1000.0 / elapsed;
                        double avgCapture = frameCount > 0 ? (totalCaptureTime / frameCount) / 1_000_000.0 : 0;
                        double avgRender = frameCount > 0 ? (totalRenderTime / frameCount) / 1_000_000.0 : 0;
                        
                        System.out.printf("[FPS] %.1f | Capture: %.2fms | Render: %.2fms | Total: %d frames%n", 
                            currentFps, avgCapture, avgRender, totalFrames);
                        
                        // Reset counters
                        frameCount = 0;
                        totalCaptureTime = 0;
                        totalRenderTime = 0;
                        lastReportTime = now;
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG] Capture thread error: " + e.getMessage());
                    break;
                }
            }
            System.out.println("[DEBUG] Capture thread ended");
        }, "Camera-Capture");
        
        captureThread.start();
    }
    
    @Override
    public void dispose() {
        running = false;
        
        if (captureThread != null) {
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (camera != null) {
            camera.stopCapture();
            camera.close();
        }
        
        super.dispose();
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }
    
    /**
     * Take a picture/screenshot of current camera frame
     * Saves as PNG file with timestamp
     */
    private void takePicture() {
        if (camera == null) {
            System.out.println("[DEBUG] No camera available for picture");
            return;
        }
        
        try {
            BufferedImage picture = camera.takePicture();
            if (picture == null) {
                System.out.println("[DEBUG] No frame available for picture");
                return;
            }
            
            // Generate filename with timestamp
            String timestamp = String.format("%04d%02d%02d_%02d%02d%02d",
                java.time.LocalDateTime.now().getYear(),
                java.time.LocalDateTime.now().getMonthValue(),
                java.time.LocalDateTime.now().getDayOfMonth(),
                java.time.LocalDateTime.now().getHour(),
                java.time.LocalDateTime.now().getMinute(),
                java.time.LocalDateTime.now().getSecond());
            
            String filename = String.format("fastcamera_picture_%s_%03d.png", timestamp, ++pictureCounter);
            
            // Save picture
            javax.imageio.ImageIO.write(picture, "PNG", new java.io.File(filename));
            System.out.println("[DEBUG] Picture saved: " + filename);
            
            // Update title to show picture taken
            String currentTitle = getTitle().replace(" (Press SPACE for picture)", "");
            setTitle(currentTitle + " [PICTURE SAVED] (Press SPACE for picture)");
            
        } catch (Exception e) {
            System.out.println("[DEBUG] Failed to take picture: " + e.getMessage());
        }
    }
    
    /**
     * Panel for displaying camera frames - optimized for 60fps
     */
    private static class CameraPanel extends JPanel {
        private BufferedImage image;
        
        public void setImage(BufferedImage img) {
            this.image = img;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
    
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Start viewer
        SwingUtilities.invokeLater(() -> {
            new CameraViewer();
        });
        
        // Keep main thread alive (non-daemon) to prevent JVM exit
        Thread keepAlive = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "KeepAlive");
        keepAlive.setDaemon(false);
        keepAlive.start();
    }
}
