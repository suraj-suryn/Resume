@echo off
setlocal enabledelayedexpansion
title Banking Auth API - Stop Server

echo ============================================
echo   Banking Auth API - Stopping Server
echo ============================================
echo.

set FOUND=0

REM Find Java process listening on port 8080
echo Searching for process on port 8080...
for /f "tokens=5" %%a in ('netstat -aon 2^>nul ^| findstr ":8080 "') do (
    if not "%%a"=="0" (
        echo Found process PID: %%a
        taskkill /F /PID %%a >nul 2>&1
        if !errorlevel! == 0 (
            echo [OK] Process %%a stopped successfully.
        ) else (
            echo [WARN] Could not stop PID %%a - may need admin rights.
        )
        set FOUND=1
    )
)

if "%FOUND%"=="0" (
    echo No process found on port 8080. Server may already be stopped.
)

REM Also kill by process name as backup
echo.
echo Checking for leftover java processes (banking-auth-api)...
wmic process where "name='java.exe' and commandline like '%%banking-auth%%'" get processid 2>nul | findstr /r "[0-9]" > nul 2>&1
if %errorlevel% == 0 (
    wmic process where "name='java.exe' and commandline like '%%banking-auth%%'" delete >nul 2>&1
    echo [OK] Cleaned up banking-auth java process.
)

REM Remove H2 lock file if it exists
if exist "%~dp0data\banking-auth-db.mv.db.lock" (
    echo Removing H2 lock file...
    del "%~dp0data\banking-auth-db.mv.db.lock" >nul 2>&1
    echo [OK] H2 lock file removed.
)

echo.
echo ============================================
echo   Server stopped. Safe to restart.
echo ============================================
pause
