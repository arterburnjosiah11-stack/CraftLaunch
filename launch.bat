@echo off
title CraftLaunch - Minecraft Launcher
echo ===================================
echo   CraftLaunch v4.0 Minecraft Launcher
echo ===================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found!
    echo.
    echo Please install Java 17 or newer:
    echo   https://adoptium.net/
    echo.
    echo After installing, restart this launcher.
    pause
    exit /b 1
)

echo [OK] Java found.
echo Starting CraftLaunch...
echo.

javaw -jar "%~dp0CraftLaunch.jar"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Launcher failed to start.
    pause
)
