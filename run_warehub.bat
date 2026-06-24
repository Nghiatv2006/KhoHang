@echo off
title WareHub Starter
echo ====================================================
echo             KHOI DONG DU AN WAREHUB
echo ====================================================
echo.
echo [1/2] Dang khoi dong Backend (Spring Boot)...
start "WareHub_Backend" cmd /c "%~dp0run_backend.bat"
echo.
echo [2/2] Dang khoi dong Frontend (VueJS)...
start "WareHub_Frontend" cmd /c "%~dp0run_frontend.bat"
echo.
echo ====================================================
echo Khoi dong hoan tat!
echo Cua so nay se tu dong dong sau 5 giay.
echo ====================================================
timeout /t 5 > nul
exit
