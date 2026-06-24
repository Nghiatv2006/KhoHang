@echo off
title WareHub Backend
cd /d "%~dp0"

:: Ep buoc Java luon su dung IPv4 de tranh loi ket noi Mail Server (smtp.gmail.com)
set "_JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true"


:loop
echo Dang nap cau hinh thong minh tu .env...
if exist "%TEMP%\set_env.bat" del "%TEMP%\set_env.bat"

powershell -Command "$validJava = $false; if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME \"bin\java.exe\"))) { $ver = & (Join-Path $env:JAVA_HOME \"bin\java.exe\") -version 2>&1; if ($ver -match \"17\") { $validJava = $true } }; if (-not $validJava) { $paths = @(\"D:\jdk17\", \"C:\Program Files\Java\", \"C:\Program Files\Eclipse Adoptium\", \"D:\Java\", \"C:\Java\"); $candidates = @(); foreach ($p in $paths) { if (Test-Path $p) { if (Test-Path (Join-Path $p \"bin\java.exe\")) { $candidates += Get-Item $p }; $candidates += Get-ChildItem -Path $p -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer } } }; $jdk17 = $candidates | Where-Object { $_.Name -match \"17\" -and (Test-Path (Join-Path $_.FullName \"bin\java.exe\")) } | Select-Object -First 1; if ($jdk17) { $env:JAVA_HOME = $jdk17.FullName } }; $lines = @(); if ($env:JAVA_HOME) { $lines += \"set \" + [char]34 + \"JAVA_HOME=\" + $env:JAVA_HOME + [char]34; echo (\"Tu dong thiet lap JAVA_HOME: \" + $env:JAVA_HOME) }; if (Test-Path .env) { Get-Content .env | Where-Object { $_ -like \"*=*\" -and -not $_.Trim().StartsWith(\"#\") } | ForEach-Object { $k, $v = $_.Split(\"=\", 2); $v = $v.Split(\"#\", 2)[0].Trim(); $lines += \"set \" + [char]34 + $k.Trim() + \"=\" + $v + [char]34 } }; $lines | Out-File -FilePath $env:TEMP\set_env.bat -Encoding ascii"

if exist "%TEMP%\set_env.bat" (
    call "%TEMP%\set_env.bat"
    del "%TEMP%\set_env.bat"
)

echo.
echo Dang khoi dong WareHub Backend...
echo Tip: Nhan [Ctrl+C] va nhap [N] de mo Menu tuy chon bat ky luc nao.
echo ==============================================================================
call .\gradlew.bat bootRun

echo.
echo ====================================================
echo                WAREHUB BACKEND DA DUNG
echo ====================================================
echo [1] Chay lai Backend (Spring Boot)
echo [2] Thoat
echo ====================================================
set choice=
set /p choice="Nhap lua chon cua ban (1-2): "
if "%choice%"=="1" (
    cls
    goto loop
)
exit
