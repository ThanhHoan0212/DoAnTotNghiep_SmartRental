@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========================================================
echo   KHOI DONG SMART RENTAL BACKEND (SPRING BOOT)
echo ========================================================

rem Kiem tra va giai phong port 8080 neu dang bi chiem
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo [INFO] Dang tat tien trinh cu dang chiem port 8080 (PID: %%a)...
    taskkill /f /pid %%a >nul 2>&1
)

if not exist "target\smart-rental-backend-0.0.1-SNAPSHOT.jar" (
    echo [INFO] Dang bien dich va dong goi ung dung backend...
    call mvnw.cmd package -DskipTests
)

echo [INFO] Dang khoi dong Spring Boot Backend tren cong 8080...
java -jar target\smart-rental-backend-0.0.1-SNAPSHOT.jar
pause
