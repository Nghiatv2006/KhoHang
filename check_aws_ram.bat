@echo off
title Kiem tra RAM AWS EC2 - WareHub
echo ===================================================
echo   KIEM TRA DUNG LUONG RAM TREN AWS EC2 (WAREHUB)
echo ===================================================
echo.
ssh -o StrictHostKeyChecking=no -i "C:\Users\Nghia\Downloads\KhoHang.pem" ubuntu@18.140.91.111 "free -h"
echo.
echo ===================================================
pause
