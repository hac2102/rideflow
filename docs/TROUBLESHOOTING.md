# RideFlow — Troubleshooting Guide

### 1. Port 8080 Already in Use
**Symptom:** Server fails with `Web server failed to start. Port 8080 was already in use.`
**Resolution:**
Find and kill the process occupying port 8080:
```powershell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
```
Alternatively, adjust `server.port` in `rideflow-server/src/main/resources/application.yml`.

---

### 2. Java 21 Not Found
**Symptom:** `The system cannot find the path specified` or Java version is < 21.
**Resolution:**
Ensure Java 21 or 23 JDK is installed. Check path:
```cmd
"C:\Program Files\Java\jdk-23\bin\java.exe" -version
```
If installed in another directory, update the paths in `scripts\run_server.cmd` and `scripts\setup.cmd` or set your `JAVA_HOME` system environment variable.

---

### 3. MySQL vs. In-Memory Fallback
**Behavior:**
- If a local MySQL 8.x instance is running on port 3306 (or via `docker-compose up -d`), RideFlow connects to it and applies Flyway migrations.
- If MySQL is offline, RideFlow automatically falls back to an embedded in-memory H2 database running in MySQL Compatibility Mode (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`).
- No manual configuration is required for local offline demonstrations.

---

### 4. Maven Reactor Artifact Resolution
**Symptom:** `Could not find artifact com.rideflow:rideflow-core:jar:1.0.0`
**Resolution:**
Always compile from the project root directory or invoke `scripts\setup.cmd`. In multi-module Maven projects, sub-modules depend on the parent reactor build.
