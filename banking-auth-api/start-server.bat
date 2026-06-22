@echo off
title Banking Auth API - Server

echo ============================================
echo   Banking Auth API - Starting Server
echo ============================================
echo.

REM Kill any existing instance on port 8080 first
echo [1/3] Checking for existing process on port 8080...
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":8080 "') do (
    echo       Stopping existing process PID: %%a
    taskkill /F /PID %%a >nul 2>&1
)

REM Wait briefly for port to free up
ping -n 2 127.0.0.1 >nul

echo [2/3] Setting up environment...
set JAVA_HOME=C:\Fussion_Essence\Tool\jdk1.8.0_202
set MAVEN_HOME=C:\Fussion_Essence\Tool\apache-maven-3.9.11

REM Change to project directory
cd /d "%~dp0"

echo [3/3] Starting Spring Boot application...
echo.
echo   Swagger UI  : http://localhost:8080/swagger-ui/index.html
echo   H2 Console  : http://localhost:8080/h2-console
echo   Health Check: http://localhost:8080/actuator/health
echo.
echo   Press Ctrl+C to stop the server
echo ============================================
echo.

%MAVEN_HOME%\bin\mvn.cmd clean spring-boot:run

echo.
echo Server stopped.
pause
