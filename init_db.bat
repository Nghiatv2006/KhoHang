@echo off
title WareHub Database Setup
cd /d "%~dp0"

:: Kiem tra quyen Administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Yeu cau quyen Administrator de tu dong kiem tra va khoi chay PostgreSQL...
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

:: Neu da co quyen Admin, chay script PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0init_db.ps1"
