# FastCamera Reference

## 1. CPU Feature Model
*   **AVX2** — detected via CPUID. Enables 32-byte vector ops.
*   **SSE4.2** — detected via CPUID. 16-byte fallback.
*   **Fallback rule**: AVX2 → SSE4.2 → scalar.

## 2. API Specification

### Device Enumeration & Control
- `List<CameraDevice> enumerateDevices()`: Queries all connected cameras across WinRT, MediaFoundation, and DirectShow.
- `FastCamera open(String deviceId)`: Opens the target camera device by native ID.
- `void close()`: Releases all underlying native camera resources and media streams.

### Capture & Streaming
- `boolean startCapture(int width, int height, int fps)`: Starts continuous frame acquisition.
- `ByteBuffer startStream(int width, int height, int fps)`: Starts zero-copy streaming mapped directly to native frame memory via `DirectByteBuffer`.
- `boolean hasNewFrame()`: Non-blocking check for new frame arrival.
- `byte[] getFrame()`: Retrieves latest frame in RGBA byte array format.
- `BufferedImage takePicture()`: Captures the current camera frame as standard `BufferedImage`.
- `void stopCapture()`: Stops the capture loop.

### FastImage Ecosystem Bridge
- `FastImage captureImage()`: Returns the current frame as an off-heap `FastImage` instance.
- `FastImage getStreamImage()`: **Zero-Copy**: Wraps the camera's streaming `DirectByteBuffer` directly into `FastImage` without memory copying.

## 3. Guarantees & Contracts
*   **Zero-Copy**: All streaming operations map native memory directly via `DirectByteBuffer`.
*   **Thread-Safety**: Frame synchronization via `lockFrame()` and `unlockFrame()`.
*   **Minimal GC Pressure**: Pre-allocated native frame buffers avoid per-frame allocations.

## 4. Platform Support
| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported |

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.*

Made with ⚡ by Andre Stubbe