@echo off
setlocal
echo =========================================================================
echo                   RideFlow Platform Environment Setup
echo =========================================================================

echo [1/3] Checking Java 21 LTS environment...
"C:\Program Files\Java\jdk-23\bin\java.exe" -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java 21/23 JDK is not found at C:\Program Files\Java\jdk-23.
    echo Please install Java 21 LTS or set JAVA_HOME.
    pause
    exit /b 1
)

echo.
echo [2/3] Checking Maven environment...
call "%~dp0\..\mvnw.cmd" -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven wrapper is missing.
    pause
    exit /b 1
)

echo.
echo [3/3] Compiling and packaging all RideFlow modules...
cd "%~dp0\.."
call "%~dp0\..\mvnw.cmd" clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed. Please review error messages above.
    pause
    exit /b 1
)

echo.
echo =========================================================================
echo     Setup Complete! You can now run scripts\run_all.cmd to launch.
echo =========================================================================
pause
