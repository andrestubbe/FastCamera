@echo off
setlocal EnableDelayedExpansion

echo ============================================
echo FastCamera Native Build Script
echo ============================================

:: Configuration
set PROJECT_NAME=fastcamera
set DLL_NAME=%PROJECT_NAME%.dll
set JAVA_VERSION=17

:: Find Java JDK (not JRE) - force JDK over JRE
set JAVA_HOME=

:: Check common JDK locations first
if exist "C:\Program Files\Java\jdk-25" set JAVA_HOME=C:\Program Files\Java\jdk-25
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-21" set JAVA_HOME=C:\Program Files\Java\jdk-21
)
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-17" set JAVA_HOME=C:\Program Files\Java\jdk-17
)
if not defined JAVA_HOME (
    for /d %%d in ("C:\Program Files\Java\jdk*") do set JAVA_HOME=%%d
)

:: Fallback to javac location if still not found
if not defined JAVA_HOME (
    for /f "delims=" %%i in ('where javac 2^>nul') do (
        for %%j in ("%%i") do set JAVA_BIN=%%~dpj
        for %%j in ("!JAVA_BIN!..") do set JAVA_HOME=%%~fj
    )
)

if not defined JAVA_HOME (
    echo ERROR: Java JDK not found. Please install JDK 17+ or set JAVA_HOME.
    exit /b 1
)

echo Java Home: %JAVA_HOME%

:: Set paths
set JNI_INCLUDE=%JAVA_HOME%\include
set JNI_WIN32=%JAVA_HOME%\include\win32

:: Check for Visual Studio in all possible locations
set VC_VARS="C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
if not exist %VC_VARS% (
    set VC_VARS="C:\Program Files (x86)\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
)
if not exist %VC_VARS% (
    set VC_VARS="C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
)

if not exist %VC_VARS% (
    echo ERROR: Visual Studio 2022 not found at expected locations.
    echo Searched:
    echo   - C:\Program Files\Microsoft Visual Studio\2022\Community
    echo   - C:\Program Files ^(x86^)\Microsoft Visual Studio\2022\Community
    echo   - C:\Program Files ^(x86^)\Microsoft Visual Studio\2022\BuildTools
    echo.
    echo Please run this from a Developer Command Prompt or install VS2022.
    exit /b 1
)

:: Call vcvars64.bat to set up environment
call %VC_VARS%
if errorlevel 1 (
    echo ERROR: Failed to initialize Visual Studio environment
    exit /b 1
)

echo Visual Studio environment loaded.

:: Create output directory
if not exist build mkdir build

:: Compile
echo.
echo Compiling %PROJECT_NAME%.cpp...

cl.exe /EHsc /MD /O2 /W3 /GL /Gw /arch:AVX2 ^
    /I"%JNI_INCLUDE%" ^
    /I"%JNI_WIN32%" ^
    /DWIN32 /DNDEBUG /D_WINDOWS /D_USRDLL /DFASTCAMERA_EXPORTS ^
    native\%PROJECT_NAME%.cpp ^
    /link ^
    /DLL /MACHINE:X64 /LTCG ^
    /DEF:native\%PROJECT_NAME%.def ^
    /OUT:build\%DLL_NAME% ^
    mfplat.lib mf.lib mfreadwrite.lib mfuuid.lib ^
    kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib ^
    advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib

if errorlevel 1 (
    echo.
    echo ERROR: Compilation failed
    exit /b 1
)

echo.
echo ============================================
echo Build successful: build\%DLL_NAME%
echo ============================================

:: Copy to Maven target for packaging
if exist target\classes (
    copy /Y build\%DLL_NAME% target\classes\
    echo Copied DLL to target\classes\
)

endlocal
