@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   VoxelCraft 3D - High-Performance LWJGL Engine
echo ===================================================

set "JDK_BIN=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot\bin"
if exist "%JDK_BIN%\javac.exe" (
    set "JAVAC=%JDK_BIN%\javac.exe"
    set "JAVA=%JDK_BIN%\java.exe"
) else (
    set "JAVAC=javac"
    set "JAVA=java"
)

if not exist "bin" mkdir "bin"

echo [1/3] Compiling Java source files...
dir /s /b "src\main\java\*.java" > sources.txt
"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d "bin" @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

echo [2/3] Copying assets and shaders...
xcopy /s /e /y /q "src\main\resources\*" "bin\" >nul 2>&1

echo [3/3] Launching Engine...
echo ===================================================
"%JAVA%" -Xmx2G -Xms512M -XX:+UseG1GC -cp "bin;lib/*" com.engine.Main

if errorlevel 1 (
    echo.
    echo Engine closed with error code %errorlevel%
    pause
)

