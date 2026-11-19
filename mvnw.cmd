@echo off
set "MAVEN_CMD=C:\Users\Shashwat\maven\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) else (
    mvn %*
)
