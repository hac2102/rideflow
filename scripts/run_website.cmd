@echo off
REM Serves the RideFlow website at http://localhost:8000 and opens it in the browser.
cd /d "%~dp0..\docs"
start "" http://localhost:8000
where python >nul 2>nul && (python -m http.server 8000 & goto :eof)
where py >nul 2>nul && (py -m http.server 8000 & goto :eof)
echo Python was not found. Open docs\index.html directly, or install Python from https://www.python.org/downloads/
pause
