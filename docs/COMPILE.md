# Building FastCamera 🛠️

Complete build guide for compiling the native C++ MediaFoundation/WinRT/DirectShow camera capture engine and packaging the Java JAR.

---

## Prerequisites

* **Windows 10 or 11 (64-bit)**
* **JDK 17+** ([Eclipse Adoptium](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/))
* **Visual Studio 2022 or 2026** (Community, Professional, or Enterprise) with "Desktop development with C++" workload
* **Windows 10/11 SDK** (installed with Visual Studio)
* **Maven 3.9+**

---

## Automated One-Click Build

FastCamera includes an automated compilation script with Visual Studio and JDK discovery:

```cmd
# In the FastCamera repository root:
compile.bat
```

What `compile.bat` does automatically:
1. Detects Visual Studio 2026 / 2022 Community via `vswhere.exe`.
2. Initializes the 64-bit developer environment (`vcvars64.bat`).
3. Compiles `native/FastCamera.cpp` with AVX2 SIMD flags and links MediaFoundation libraries (`mfplat.lib`, `mf.lib`, `mfreadwrite.lib`, `mfuuid.lib`).
4. Deploys `fastcamera.dll` directly to:
   - `build/fastcamera.dll`
   - `src/main/resources/native/fastcamera.dll`
   - `%USERPROFILE%\.fastcore\native\fastcamera\fastcamera.dll`

---

## Maven Java Packaging

```bash
mvn clean install -DskipTests
```

---

## JMH Benchmarking

To build and execute the official JMH benchmark suite:

```cmd
run-benchmark.bat
```

## Native Build Details

### Using compile.bat (Recommended)

The `compile.bat` script:
1. Locates Java JNI headers
2. Sets up Visual Studio environment
3. Compiles with AVX2 optimizations
4. Links MediaFoundation libraries
5. Outputs to `build/fastcamera.dll`

### Manual Compilation

```batch
# Set up Visual Studio environment
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"

# Compile
cl.exe /EHsc /MD /O2 /arch:AVX2 /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" \
    native\FastCamera.cpp /link /DLL /OUT:fastcamera.dll \
    mfplat.lib mf.lib mfreadwrite.lib mfuuid.lib
```

## Java Build

### Standard Maven

```bash
mvn clean package              # Build JAR
mvn clean package -DskipTests  # Skip tests
mvn test                       # Run tests
```

### With Native DLL

Copy the built DLL to resources:
```batch
copy build\fastcamera.dll src\main\resources\
mvn clean package
```

## Troubleshooting

### "Java not found"
- Set `JAVA_HOME` environment variable
- Or run from Developer Command Prompt

### "Visual Studio not found"
- Install Visual Studio 2022 with C++ workload
- Or manually specify path in `compile.bat`

### "MediaFoundation errors"
- Windows 7: Install Platform Update
- Ensure mfplat.dll is present (Windows 7+)

## Release Checklist

- [ ] Version updated in `pom.xml`
- [ ] Native DLL built and tested
- [ ] All unit tests passing
- [ ] Example runs successfully
- [ ] Git tag created
- [ ] GitHub Release with JAR asset
