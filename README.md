# FastCamera 0.1.1 [ALPHA-2026-05-17] — Ultra-Fast Native Camera Capture for Java

[![Status](https://img.shields.io/badge/status-0.1.1-brightgreen.svg)](https://github.com/andrestubbe/FastCamera/releases/tag/0.1.1)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastCamera)

---

**📸 The high-performance native camera module for the FastJava ecosystem. Hardware-accelerated capture via
MediaFoundation, WinRT, and DirectShow with SIMD color conversion.**

**FastCamera** brings real hardware-accelerated camera capture to Java. By bypassing standard slow APIs and using direct
native pipelines, it achieves ultra-low latency 1080p@60fps capture with SIMD-accelerated YUV→RGBA conversion.

---

[![FastFileIndex Showcase](docs/screenshot.png)](https://www.youtube.com/watch?v=BZsqQl7WqWk)

---

## Quick Start

```java
import fastcamera.FastCamera;
import fastcamera.CameraDevice;
import fastimage.FastImage;
import java.nio.ByteBuffer;
import java.util.List;

public class Demo {
    public static void main(String[] args) {
        // 1. Enumerate available cameras across WinRT, MediaFoundation, and DirectShow
        List<CameraDevice> devices = FastCamera.enumerateDevices();
        if (devices.isEmpty()) {
            System.out.println("No cameras detected.");
            return;
        }

        // 2. Open primary camera
        CameraDevice dev = devices.get(0);
        FastCamera camera = FastCamera.open(dev.getId());

        // 3. Start high-performance streaming (e.g. 1080p @ 60fps)
        ByteBuffer directBuffer = camera.startStream(1920, 1080, 60);

        // 4. Capture single frame directly as SIMD-accelerated FastImage
        FastImage frameImage = camera.captureImage();
        if (frameImage != null) {
            // Apply SIMD filters with zero JVM garbage collection
            FastImage processed = frameImage.resize(640, 360).grayscale();
        }

        // 5. Clean up native resources
        camera.stopCapture();
        camera.close();
    }
}
```

---

## Table of Contents

- [Why FastCamera?](#why-fastcamera)
- [Quick Start](#quick-start)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Architecture & Hardware Pipeline](#architecture--hardware-pipeline)
- [Performance Benchmarks](#performance-benchmarks)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastCamera?

Capturing camera and webcam video in standard Java usually involves bloated multi-megabyte wrappers, JNI overhead, or slow OpenCV/JavaCV bridges that force unnecessary memory copies:

1. **Slow Format Conversion**: Most webcams output hardware YUV (YUY2/NV12) or MJPEG. Standard Java converts these formats on the CPU using slow scalar loops, burning 30–50% CPU just for color conversion.
2. **Heavy Heap Allocation**: Creating new image objects or byte arrays per frame creates extreme JVM Garbage Collection pressure at 60 FPS, resulting in dropped frames and unpredictable stutter.
3. **Fragile Backend Support**: Many Java camera libraries depend on outdated 32-bit DirectShow filters or fail on modern Windows 10/11 WinRT camera permissions.

**FastCamera** eliminates these pain points with a clean, native-first architecture:
- **Triple Native Backend**: Automatically selects between **WinRT**, **MediaFoundation**, and **DirectShow** for maximum device compatibility.
- **Zero-Copy Streaming**: Direct native frame mapping exposes raw video buffers to Java via `DirectByteBuffer` with 0 GC overhead.
- **FastImage Ecosystem Bridge**: Seamlessly wrap or capture video frames directly into off-heap `FastImage` instances for SIMD filtering.

---

## Key Features

- 🎥 **Triple Native Engine** — Automatic hardware-accelerated pipeline selection (WinRT, MediaFoundation, DirectShow).
- ⚙️ **SIMD Color Conversion** — High-speed YUV→RGBA conversion leveraging AVX2 and SSE4.2 vector instructions.
- 📥 **Zero-Copy Streaming** — Direct access to native video memory via `DirectByteBuffer` with 0 heap bytes allocated.
- 🖼️ **FastImage Ecosystem Bridge** — Instant zero-copy interoperability with `FastImage` for SIMD resize, blur, and vision filtering.
- ⏱️ **Ultra-Low Latency** — Async native capture callbacks delivering stable 1080p @ 60 FPS.
- 🔗 **FastCore Integration** — Unified native DLL loading and extraction without manual environment setup.

---

## Real-World Use Cases

- 👁️ **Computer Vision & AI Tracking**: Stream raw camera frames directly to YOLO, OpenCV, or TensorRT with zero latency.
- 🎙️ **Live Streaming & Virtual Camera Overlays**: Process and filter webcam video with real-time Kawase background blur via `FastImage`.
- 🏭 **Industrial Inspection & OCR**: High-framerate capture for barcode scanning, text extraction, and optical inspection.
- 🤖 **Autonomous Robotics & Drones**: Low-overhead visual feedback loop running on resource-constrained JVM runtimes.

---

## Architecture & Hardware Pipeline

```
┌────────────────────────────────────────────────────────┐
│                   Java Application                     │
└───────────────┬────────────────────────┬───────────────┘
                │ Direct JNI             │ Zero-Copy Wrap
                ▼                        ▼
┌───────────────────────────────┐ ┌──────────────────────┐
│  fastcamera.dll (Native C++)  │ │      FastImage       │
├───────────────┬───────────────┤ │  (SIMD / Off-Heap)   │
│ WinRT / MF /  │ AVX2 YUV-RGBA │ └──────────┬───────────┘
│ DirectShow    │ SIMD Kernel   │            │
└───────┬───────┴───────┬───────┘            ▼
        │               └─────────────► Off-Heap Processing
        ▼
┌───────────────────────────────┐
│ UVC Camera / Video Capture HW │
└───────────────────────────────┘
```

---

## Performance Benchmarks

Measured on official [JMH Benchmark](examples/Benchmark) (Throughput in `ops/ms`):

```text
Benchmark                             Mode  Cnt  Score   Error   Units
Benchmark.benchmarkEnumerateDevices  thrpt    3  0.802          ops/ms
```

> **High-Performance Native Interop**: Native MediaFoundation device probing and capability negotiation executes in **~1.2 ms** with complete metadata extraction and zero JVM heap pollution.

---

## API Quick Reference

| Method | Description | Docs |
|--------|-------------|------|
| `enumerateDevices()` | Queries all connected camera devices. | [Reference 📖](docs/REFERENCE.md) |
| `open(deviceId)` | Opens target camera device by native ID. | [Reference 📖](docs/REFERENCE.md) |
| `startStream(w, h, fps)` | **Zero-Copy:** Maps native frame memory to `DirectByteBuffer`. | [Reference 📖](docs/REFERENCE.md) |
| `captureImage()` | **FastImage Bridge:** Captures frame to off-heap `FastImage`. | [Reference 📖](docs/REFERENCE.md) |
| `getStreamImage()` | **Zero-Copy FastImage:** Wraps streaming buffer directly. | [Reference 📖](docs/REFERENCE.md) |
| `takePicture()` | Captures current frame as standard `BufferedImage`. | [Reference 📖](docs/REFERENCE.md) |
| `close()` | Releases camera hardware and streams. | [Reference 📖](docs/REFERENCE.md) |

---


## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the dependencies to your `pom.xml`:

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastCamera Library -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCamera</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastImage Frame Bridge -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastImage</artifactId>
        <version>0.1.2</version>
    </dependency>

    <!-- FastCore (Required Native Loader) -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastCamera:0.1.1'
    implementation 'com.github.andrestubbe:FastImage:0.1.2'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly to add them to your classpath:

1. 📦 **[FastCamera-0.1.1.jar](https://github.com/andrestubbe/FastCamera/releases/tag/0.1.1)** (The Core Library)
2. ⚡ **[FastImage-0.1.2.jar](https://github.com/andrestubbe/FastImage/releases/tag/0.1.2)** (The SIMD Image Engine)
3. ⚙️ **[FastCore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/tag/0.1.0)** (The Mandatory Native Loader)

---

## Documentation

* **[COMPILE.md](docs/COMPILE.md)**: Full compilation guide (MSVC C++17 build chain + JNI Setup).
* **[REFERENCE.md](docs/REFERENCE.md)**: Full API descriptions, border configurations, and codepoint index.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The engineering rationale for zero-allocation performance.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and planned features.

---

## Platform Support

| Platform      | Status            |
|---------------|-------------------|
| Windows 10/11 | ✅ Fully Supported |
| Linux         | 🔗 Planned        |
| macOS         | 🔗 Planned        |

---

## License

MIT License  See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastFileIndex](https://github.com/andrestubbe/FastFileIndex) - Binary file indexing with mmap support
- [FastFileSearch](https://github.com/andrestubbe/FastFileSearch) - Prefix Trie, N-Gram index, and Ranking engine
- [FastFileWatch](https://github.com/andrestubbe/FastFileWatch) - USN Journal-based live file monitoring
- [FastCore](https://github.com/andrestubbe/FastCore) - Unified JNI loader and platform abstraction

---

**Part of the FastJava Ecosystem** — *Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋*


