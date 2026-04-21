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
    
    private final CameraPanel cameraPanel;
    private FastCamera camera;
    private volatile boolean running = false;
    private Thread captureThread;
    
    public CameraViewer() {
        super("FastCamera - Live Preview");
        
        // Setup window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);
        
        // Camera panel for displaying frames
        cameraPanel = new CameraPanel();
        add(cameraPanel, BorderLayout.CENTER);
        
        // Info label at bottom
        JLabel infoLabel = new JLabel("Connecting to camera...", SwingConstants.CENTER);
        add(infoLabel, BorderLayout.SOUTH);
        
        setVisible(true);
        
        // Start camera in background
        SwingUtilities.invokeLater(() -> {
            try {
                startCamera();
            } catch (Exception e) {
                infoLabel.setText("Error: " + e.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Failed to start camera: " + e.getMessage(),
                    "Camera Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void startCamera() {
        // Enumerate cameras
        List<CameraDevice> cameras = FastCamera.enumerateDevices();
        if (cameras.isEmpty()) {
            throw new CameraException("No cameras found");
        }
        
        // Find first available
        CameraDevice selected = cameras.stream()
            .filter(CameraDevice::isAvailable)
            .findFirst()
            .orElseThrow(() -> new CameraException("No available cameras"));
        
        System.out.println("Using camera: " + selected.getName());
        
        // Open and start
        camera = FastCamera.open(selected.getId());
        
        // Use zero-copy streaming for best performance
        int width = 640;
        int height = 480;
        int fps = 30;
        
        ByteBuffer frameBuffer = camera.startStream(width, height, fps);
        running = true;
        
        // Setup camera panel size
        cameraPanel.setPreferredSize(new Dimension(width, height));
        pack();
        
        // Capture loop in separate thread
        captureThread = new Thread(() -> {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            
            long frameCount = 0;
            long startTime = System.currentTimeMillis();
            
            while (running) {
                if (camera.hasNewFrame()) {
                    // Lock frame for reading
                    camera.lockFrame();
                    try {
                        // Copy from DirectByteBuffer to image
                        frameBuffer.get(pixels);
                        frameBuffer.rewind();
                        
                        // Update display (on EDT)
                        SwingUtilities.invokeLater(() -> {
                            cameraPanel.setImage(image);
                        });
                        
                        frameCount++;
                    } finally {
                        camera.unlockFrame();
                    }
                    
                    // Print FPS every second
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= 1000) {
                        double fps_actual = frameCount * 1000.0 / elapsed;
                        System.out.printf("FPS: %.1f%n", fps_actual);
                        frameCount = 0;
                        startTime = System.currentTimeMillis();
                    }
                } else {
                    // Small delay to prevent busy-wait
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
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
    
    /**
     * Panel for displaying camera frames
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
                // Draw image scaled to panel size
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            } else {
                // Draw placeholder
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                g.drawString("No camera signal", 10, 20);
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
    }
}
