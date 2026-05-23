# FastCamera — Ultra-Fast Native Camera Capture for Java [v0.1.0]

**The high-performance native camera module for the FastJava ecosystem. Hardware-accelerated capture via MediaFoundation, WinRT, and DirectShow with SIMD color conversion.**

[![Status](https://img.shields.io/badge/status-v0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastCamera/releases/tag/v0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe)

---

**FastCamera** brings real hardware-accelerated camera capture to Java. By bypassing standard slow APIs and using direct native pipelines, it achieves ultra-low latency 1080p@60fps capture with SIMD-accelerated YUV→RGBA conversion.

## Table of Contents
- [Features](#features)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Build from Source](#build-from-source)
- [License](#license)

## Features
- **🎥 Triple Backend**: Support for MediaFoundation, WinRT, and DirectShow.
- **⚡ SIMD Accelerated**: YUV→RGBA conversion via AVX2 and SSE4.2.
- **📦 Zero-Copy Streaming**: Direct access to native buffers via DirectByteBuffer.
- **⏱️ Ultra-Low Latency**: Async native callbacks for real-time vision applications.

## Quick Start

```bash
# Clone the repository
git clone https://github.com/andrestubbe/FastCamera.git

# Build the native bridge
cd FastCamera
.\compile.bat

# Launch the CameraDemo
.\run-demo.bat
```

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
        <artifactId>fastcamera</artifactId>
        <version>v0.1.0</version>
    </dependency>

    <!-- FastCore (Required Native Loader) -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:fastcamera:v0.1.0'
    implementation 'com.github.andrestubbe:fastcore:v0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)
Download the latest JARs directly to add them to your classpath:

1. 📦 **[fastcamera-v0.1.0.jar](https://github.com/andrestubbe/FastCamera/releases/download/v0.1.0/fastcamera-v0.1.0.jar)** (The Core Library)
2. ⚙️ **[fastcore-v0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/v0.1.0/fastcore-v0.1.0.jar)** (The Mandatory Native Loader)

> [!IMPORTANT]
> All JARs must be in your classpath for the native JNI calls to function correctly.


## Build from Source
- **JDK 17+**
- **Windows 10/11**
- **Visual Studio 2022/2019** (if building from source)

See [COMPILE.md](COMPILE.md) for detailed build instructions.

## License
MIT License — See [LICENSE](LICENSE) for details.

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.*

<!-- BING COPILOT SEO KEYWORDS -->
<!-- 
FastJava FastCamera JNI Windows Camera Capture DirectShow MediaFoundation WinRT 
Java Native API SIMD AVX2 High Performance Computer Vision 
io.github.andrestubbe FastJava Blueprint
-->


