@echo off
setlocal enabledelayedexpansion

echo ================================================================
echo   Banking Core Microservices — Docker Startup
echo ================================================================
echo.
echo   This script builds Docker images from source and starts all
echo   services in correct dependency order.
echo.
echo   First run:  ~5-10 min  (Maven downloads ~200 MB of deps)
echo   Subsequent: ~1-2 min   (Docker layer cache used)
echo.

REM ── Prerequisite: Docker must be running ─────────────────────────────────────
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not running. Start Docker Desktop and retry.
    exit /b 1
)

REM ── Must be run from the banking-core-microservices directory ─────────────────
if not exist ".env" (
    echo [ERROR] .env file not found.
    echo         Run this script from the banking-core-microservices directory.
    exit /b 1
)
if not exist "docker-compose.yml" (
    echo [ERROR] docker-compose.yml not found.
    exit /b 1
)

echo [1/2] Building Docker images (multi-stage Maven + JRE builds)...
echo       This step is cached after the first run.
echo.
docker-compose build --parallel
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] docker-compose build failed.
    echo         Run 'docker-compose build' without --parallel to see per-service errors.
    exit /b 1
)

echo.
echo [2/2] Starting all services (startup order enforced by health checks)...
echo       Zookeeper  ^> Kafka  ^> Config Server  ^> Eureka
echo       ^> Account + Transaction + Notification  ^> API Gateway
echo.
docker-compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] docker-compose up failed. Check 'docker-compose ps' for details.
    exit /b 1
)

echo.
echo ================================================================
echo   Services starting. Health checks may take up to 3 minutes.
echo.
echo   Check status : docker-compose ps
echo   Tail logs    : docker-compose logs -f api-gateway
echo.
echo   ── Access Points (defaults — override in .env) ──────────────
echo   Kafdrop (Kafka UI) : http://localhost:9000
echo   Eureka Dashboard   : http://localhost:8761
echo   API Gateway        : http://localhost:8090
echo   Account Swagger    : http://localhost:8081/swagger-ui.html
echo   Transaction Swagger: http://localhost:8082/swagger-ui.html
echo ================================================================
