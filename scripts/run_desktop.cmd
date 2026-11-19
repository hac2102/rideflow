@echo off
echo =========================================================================
echo              Launching RideFlow JavaFX Operations Dashboard
echo =========================================================================
cd "%~dp0\..\rideflow-desktop"
"C:\Users\Shashwat\maven\bin\mvn.cmd" javafx:run
pause
