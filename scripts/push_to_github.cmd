@echo off
setlocal
title RideFlow - Push to GitHub Repository
echo =========================================================================
echo                  RideFlow - Upload to GitHub (Public)
echo =========================================================================
echo.

set "REPO_URL=https://github.com/shashwatt22/rideflow"

echo 1. Opening GitHub in your browser to create the empty public repository...
echo    - Repository name: rideflow
echo    - Visibility: Public
echo    - DO NOT check "Add a README file" or .gitignore (the project already has them).
echo.
start "" "https://github.com/new?name=rideflow"

echo Press any key AFTER creating the repository on GitHub...
pause >nul

echo.
echo 2. Pushing local commits to GitHub (origin/main)...
cd /d "%~dp0.."
git branch -M main
git remote remove origin 2>nul
git remote add origin %REPO_URL%.git
git push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo =========================================================================
    echo   SUCCESS! RideFlow has been uploaded to GitHub!
    echo =========================================================================
    echo Shareable Public Repository Link for your teammates:
    echo %REPO_URL%
    echo =========================================================================
    start "" "%REPO_URL%"
) else (
    echo.
    echo [ERROR] Push failed. Make sure:
    echo 1. You created the repository named 'rideflow' under your account.
    echo 2. You authenticated via the GitHub login popup window.
)

echo.
pause
