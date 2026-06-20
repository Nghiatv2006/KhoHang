@echo off
title WareHub Backend
cd /d "%~dp0"
echo Dang nap cau hinh thong minh tu .env...
powershell -Command "$javaVer = & java -version 2>&1; if ($javaVer -notmatch 'version \"17') { $paths = @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'D:\jdk17', 'D:\Java', 'C:\Java'); $jdk17 = Get-ChildItem -Path $paths -Filter '*17*' -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer } | Select-Object -First 1; if ($jdk17) { $env:JAVA_HOME = $jdk17.FullName; echo 'Tu dong phat hien JDK 17 tai: ' + $env:JAVA_HOME } }; Get-Content .env | Where-Object { $_ -notmatch '^\s*#' -and $_ -like '*=*' } | ForEach-Object { $k, $v = $_.Split('=', 2); $v = $v.Split('#', 2)[0].Trim(); [System.Environment]::SetEnvironmentVariable($k.Trim(), $v) }; .\gradlew.bat bootRun"
pause
