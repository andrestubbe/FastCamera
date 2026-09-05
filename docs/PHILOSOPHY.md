# The Philosophy of FastCamera 💡

> [!IMPORTANT]
> **"Keine Kopien. Niemals. Kritischer JNI-Pfad. Native-First Performance."**

FastCamera is built on the conviction that high-framerate camera acquisition, video streaming, and machine vision in Java should run directly against OS media subsystems without intermediate byte copying or JVM garbage collection penalties.

## Core Tenets

1.  **Direct Media Subsystem Access**
    Bypass cumbersome cross-platform abstractions by talking directly to Windows native media subsystems: WinRT, MediaFoundation, and DirectShow.

2.  **Zero-Copy DirectByteBuffer Streaming**
    Map hardware video frames directly into Java via direct `ByteBuffer` views. Eliminates heap allocations and enables 60 FPS real-time feeds with 0 GC pauses.

3.  **Hardware & SIMD Color Conversion**
    Convert camera YUV formats (YUY2, NV12) to RGBA using hand-tuned AVX2 and SSE4.2 SIMD kernels, saving up to 50% CPU overhead over standard scalar conversions.

4.  **Ecosystem Synergy with FastImage**
    Bridge video frames seamlessly into `FastImage` off-heap buffers with zero latency for immediate image processing, scaling, and computer vision filters.

5.  **FastJava Blueprint Consistency**
    As part of the **FastJava** ecosystem:
    *   **Native Backend**: Direct C++ implementation.
    *   **Unified Loading**: Powered by `FastCore` for seamless zero-dependency deployment.
    *   **Production Quality**: Ultra-low latency, robust error handling, and high-framerate stability.

---
**⚡ FastCamera — Powering the next generation of Native Java.**
