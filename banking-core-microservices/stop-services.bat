@echo off
echo ================================================================
echo   Banking Core Microservices — Docker Shutdown
echo ================================================================
echo.
echo   Stopping and removing all containers...
echo   (Kafka data and PostgreSQL volume are preserved)
echo.

docker-compose down

if %ERRORLEVEL% EQU 0 (
    echo.
    echo   All containers stopped.
    echo   Run 'start-services.bat' to restart.
) else (
    echo.
    echo [WARNING] docker-compose down returned an error.
    echo           Run 'docker ps' to check for running containers.
)
echo.
