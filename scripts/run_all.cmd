@echo off
setlocal
echo =========================================================================
echo                Launching Full RideFlow Operations Platform
echo =========================================================================

echo [1/2] Starting Spring Boot REST Server in background window...
start "RideFlow Server :8080" cmd /c "%~dp0run_server.cmd"

echo Waiting for RideFlow Backend to initialize on port 8080...
:WAIT_LOOP
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/simulation/state' -TimeoutSec 2 -UseBasicParsing; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Backend server is online and responding!
    goto LAUNCH_DESKTOP
)
timeout /t 1 /nobreak >nul
goto WAIT_LOOP

:LAUNCH_DESKTOP
echo.
echo [2/2] Launching JavaFX Operations Dashboard...
cd "%~dp0..\rideflow-desktop"
call "%~dp0run_desktop.cmd"
