@echo off
setlocal
echo =========================================================================
echo              Running Complete RideFlow Automated Test Suite
echo =========================================================================

cd "%~dp0\.."
call "%~dp0..\mvnw.cmd" test
if %ERRORLEVEL% EQU 0 (
    echo.
    echo =========================================================================
    echo          ALL TESTS PASSED! Multi-Module Suite 100%% Green.
    echo =========================================================================
) else (
    echo.
    echo [ERROR] Some tests failed. Review log above.
)
pause
