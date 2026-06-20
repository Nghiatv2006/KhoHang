@echo off
title WareHub Backend
cd /d d:\IT\Hehe
echo Dang nap cau hinh thong minh tu .env...
powershell -Command "Get-Content .env | Where-Object { $_ -notmatch '^\s*#' -and $_ -like '*=*' } | ForEach-Object { $k, $v = $_.Split('=', 2); $v = $v.Split('#', 2)[0].Trim(); [System.Environment]::SetEnvironmentVariable($k.Trim(), $v) }; $env:JAVA_HOME='D:\jdk17'; .\gradlew.bat bootRun"
pause
