@echo off
echo =========================================================================
echo                 Starting RideFlow Spring Boot Server (Port 8080)
echo =========================================================================
cd "%~dp0\..\rideflow-server"
"C:\Program Files\Java\jdk-23\bin\java.exe" -jar target\rideflow-server-1.0.0.jar
pause
