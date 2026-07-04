@echo off
title WareHub Backend
cd /d "%~dp0"

:: Set default environment variables in case they are missing from .env
set DB_PASSWORD=123456
set JWT_SECRET=ChucNangQuanLyKhoHangNhieuChiNhanhDuAnTotNghiep2026SuperSecretKey
set BACKUP_SECRET=WareHubBackupSecretKey2026SuperSafe123
set MAIL_USERNAME=your_email@gmail.com
set MAIL_PASSWORD=your_app_password

echo Dang nap cau hinh thong minh tu .env...
powershell -Command "$validJava = $false; if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { $ver = & (Join-Path $env:JAVA_HOME 'bin\java.exe') -version 2>&1; if ($ver -match '17') { $validJava = $true } }; if (-not $validJava) { $paths = @('D:\jdk17', 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'D:\Java', 'C:\Java'); $candidates = @(); foreach ($p in $paths) { if (Test-Path $p) { if (Test-Path (Join-Path $p 'bin\java.exe')) { $candidates += Get-Item $p }; $candidates += Get-ChildItem -Path $p -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer } } }; $jdk17 = $candidates | Where-Object { $_.Name -match '17' -and (Test-Path (Join-Path $_.FullName 'bin\java.exe')) } | Select-Object -First 1; if ($jdk17) { $env:JAVA_HOME = $jdk17.FullName; echo ('Tu dong thiet lap JAVA_HOME: ' + $env:JAVA_HOME) } }; Get-Content .env | Where-Object { $_ -notmatch '^\s*#' -and $_ -like '*=*' } | ForEach-Object { $k, $v = $_.Split('=', 2); $v = $v.Split('#', 2)[0].Trim(); [System.Environment]::SetEnvironmentVariable($k.Trim(), $v) }; .\gradlew.bat bootRun"
pause
