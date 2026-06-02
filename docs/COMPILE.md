# Building FastCamera

## Prerequisites

- Windows 7 or later
- Java JDK 17+
- Apache Maven 3.9+
- Visual Studio 2022 (C++ workload)

## Quick Build

```batch
# Clone repository
git clone https://github.com/andrestubbe/fastcamera.git
cd fastcamera

# Build native DLL (requires Visual Studio)
compile.bat

# Build Java library
mvn clean package

# Run demo
cd examples/00-basic-usage
mvn compile exec:java
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
