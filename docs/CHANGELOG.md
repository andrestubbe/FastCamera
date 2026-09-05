# Changelog

All notable changes to this project will be documented in this file.

## [0.1.1] - 2026-09-04

### Added
- **FastImage Ecosystem Integration**:
  - Direct bridge methods to `FastImage 0.1.2`:
    - `camera.captureImage()`: Capture current frame as a SIMD-accelerated off-heap `FastImage`.
    - `camera.getStreamImage()`: Zero-copy wrapping of the camera's streaming `DirectByteBuffer` directly into `FastImage`.
  - Enables GC-free chained post-processing (bilinear/area-average downscaling, Dual-Kawase blur, grayscale conversion).
- Standardized project README and Maven dependency chain.

## [0.1.0] - 2026-05-23

### Added
- Initial release
- Standardized FastJava ecosystem module
