@echo off
echo =========================================================================
echo                 Starting RideFlow Spring Boot Server (Port 8080)
echo =========================================================================
cd "%~dp0\..\rideflow-server"

:: Ensure port 8080 is free
powershell -NoProfile -Command "$c = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue; if ($c) { $c.OwningProcess | Select-Object -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } }"

if not exist "target\rideflow-server-1.0.0.jar" (
    echo Building RideFlow executable jar...
    call "%~dp0\..\mvnw.cmd" package -DskipTests
)
"C:\Program Files\Java\jdk-23\bin\java.exe" -jar target\rideflow-server-1.0.0.jar
pause
