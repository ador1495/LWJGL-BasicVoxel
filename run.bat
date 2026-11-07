@echo off
set "JDK_BIN=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot\bin"
if exist "%JDK_BIN%\java.exe" (
    set "JAVA=%JDK_BIN%\java.exe"
) else (
    set "JAVA=java"
)

"%JAVA%" -Xmx2G -Xms512M -XX:+UseG1GC -cp "bin;lib/*" com.engine.Main %*

