@echo off
title WareHub Frontend
cd /d "%~dp0VueJs"

:menu
cls
echo ====================================================
echo               QUAN LY WAREHUB FRONTEND
echo ====================================================
echo [1] Tai tai nguyen (npm install)
echo [2] Khoi chay Frontend (npm run dev)
echo [3] Thoat
echo ====================================================
set choice=
set /p choice="Nhap lua chon cua ban (1-3): "

if "%choice%"=="1" goto install_deps
if "%choice%"=="2" goto run_dev
if "%choice%"=="3" exit
goto menu

:install_deps
echo.
echo Dang tai tai nguyen (npm install)...
echo ====================================================
call npm install
echo ====================================================
echo Tai tai nguyen hoan tat!
pause
goto menu

:run_dev
echo.
echo Dang khoi dong VueJS (Vite dev)...
echo Tip: Nhan [Ctrl+C] va nhap [N] de mo Menu tuy chon bat ky luc nao.
echo ====================================================
call npm run dev
echo.
echo ====================================================
echo                WAREHUB FRONTEND DA DUNG
echo ====================================================
echo [1] Chay lai Frontend (npm run dev)
echo [2] Quay lai Menu chinh
echo [3] Thoat
echo ====================================================
set choice=
set /p choice="Nhap lua chon cua ban (1-3): "
if "%choice%"=="1" (
    cls
    goto run_dev
)
if "%choice%"=="2" (
    goto menu
)
exit
