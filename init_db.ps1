# WareHub Database Setup Script
$ErrorActionPreference = "Stop"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "             KHOI TAO DU AN WAREHUB DATABASE        " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Load .env file
$envPath = Join-Path $PSScriptRoot ".env"
if (Test-Path $envPath) {
    Write-Host "Dang nap cau hinh tu .env..." -ForegroundColor Gray
    Get-Content $envPath | Where-Object { $_ -like "*=*" -and -not $_.Trim().StartsWith("#") } | ForEach-Object {
        $k, $v = $_.Split("=", 2)
        $v = $v.Split("#", 2)[0].Trim()
        $k = $k.Trim()
        # Set environment variable for this process
        [System.Environment]::SetEnvironmentVariable($k, $v, [System.EnvironmentVariableTarget]::Process)
    }
} else {
    Write-Host "[CANH BAO] Khong tim thay file .env tai $envPath. Se dung cac gia tri mac dinh." -ForegroundColor Yellow
}

# Get variables with fallbacks
$dbHost = [System.Environment]::GetEnvironmentVariable("DB_HOST")
if (-not $dbHost) { $dbHost = "localhost" }
$dbPort = [System.Environment]::GetEnvironmentVariable("DB_PORT")
if (-not $dbPort) { $dbPort = "5432" }
$dbName = [System.Environment]::GetEnvironmentVariable("DB_NAME")
if (-not $dbName) { $dbName = "warehouse_db" }
$dbUser = [System.Environment]::GetEnvironmentVariable("DB_USERNAME")
if (-not $dbUser) { $dbUser = "postgres" }
$dbPass = [System.Environment]::GetEnvironmentVariable("DB_PASSWORD")

Write-Host "Host:     $dbHost"
Write-Host "Port:     $dbPort"
Write-Host "User:     $dbUser"
Write-Host "Database: $dbName"
Write-Host ""

# 2. Find psql
$psqlPath = "psql"
$hasPsql = $false
try {
    $null = Get-Command psql -ErrorAction Stop
    $hasPsql = $true
} catch {
    $resolvedPaths = @()
    # Tim theo wildcard voi cac duong dan Program Files pho bien
    $resolvedPaths += Resolve-Path "C:\Program Files\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue
    $resolvedPaths += Resolve-Path "C:\Program Files (x86)\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue
    $resolvedPaths += Resolve-Path "D:\Program Files\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue
    $resolvedPaths += Resolve-Path "D:\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue
    
    # Kiem tra truc tiep mot so thu muc khac
    $directPaths = @(
        "D:\PostgreSQL\bin\psql.exe",
        "D:\Database\PostgreSQL\bin\psql.exe"
    )
    foreach ($p in $directPaths) {
        if (Test-Path $p) {
            $resolvedPaths += [PSCustomObject]@{ Path = $p }
        }
    }

    if ($resolvedPaths.Count -gt 0) {
        $psqlPath = $resolvedPaths[0].Path
        $hasPsql = $true
    }
}

if (-not $hasPsql) {
    Write-Host "[LOI] Khong tim thay cong cu 'psql' tren he thong!" -ForegroundColor Red
    Write-Host "Vui long:" -ForegroundColor Yellow
    Write-Host "1. Cai dat PostgreSQL (neu chua cai)." -ForegroundColor Yellow
    Write-Host "2. Hoac them thu muc 'bin' cua PostgreSQL vao bien moi truong PATH." -ForegroundColor Yellow
    Write-Host "3. Hoac kiem tra duong dan mac dinh C:\Program Files\PostgreSQL\." -ForegroundColor Yellow
    Read-Host "Nhan Enter de thoat"
    exit 1
}

# 3. Set password env for psql and force UTF-8 client encoding
$env:PGPASSWORD = $dbPass
$env:PGCLIENTENCODING = 'utf-8'

# 3.5. Kiem tra va tu dong bat dich vu PostgreSQL (neu dang dung)
Write-Host "Dang kiem tra trang thai dich vu PostgreSQL..." -ForegroundColor Gray
$pgService = Get-Service | Where-Object { $_.Name -like "postgresql*" -or $_.DisplayName -like "*postgres*" } | Select-Object -First 1
if ($pgService) {
    if ($pgService.Status -ne "Running") {
        Write-Host "Dich vu PostgreSQL ($($pgService.DisplayName)) dang dung. Dang tu dong khoi chay..." -ForegroundColor Yellow
        try {
            Start-Service $pgService.Name
            Write-Host "Khoi chay dich vu PostgreSQL thanh cong!" -ForegroundColor Green
            # Cho 3 giay de DB san sang ket noi
            Start-Sleep -Seconds 3
        } catch {
            Write-Host "[CANH BAO] Khong the tu dong bat dich vu. Vui long kiem tra quyen Administrator." -ForegroundColor Red
        }
    } else {
        Write-Host "Dich vu PostgreSQL ($($pgService.DisplayName)) dang hoat dong." -ForegroundColor Green
    }
} else {
    Write-Host "[CANH BAO] Khong tim thay dich vu Windows PostgreSQL. Tiep tuc..." -ForegroundColor Yellow
}

# 4. Create database if it doesn't exist
Write-Host ""
Write-Host "Dang kiem tra va khoi tao co so du lieu '$dbName'..." -ForegroundColor Gray
$dbExists = & $psqlPath -h $dbHost -p $dbPort -U $dbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$dbName'"
if ($dbExists -ne "1") {
    Write-Host "Database '$dbName' chua ton tai. Dang tao..." -ForegroundColor Yellow
    & $psqlPath -h $dbHost -p $dbPort -U $dbUser -d postgres -c "CREATE DATABASE $dbName"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[LOI] Khong the tao database '$dbName'!" -ForegroundColor Red
        Read-Host "Nhan Enter de thoat"
        exit 1
    }
    Write-Host "Tao database '$dbName' thanh cong." -ForegroundColor Green
} else {
    Write-Host "Database '$dbName' da ton tai." -ForegroundColor Green
}

# 5. Run full_schema.sql
Write-Host "Dang nap cau truc bang (full_schema.sql)..." -ForegroundColor Gray
Write-Host "====================================================" -ForegroundColor Gray
$schemaPath = Join-Path $PSScriptRoot "full_schema.sql"
& $psqlPath -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $schemaPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "[LOI] Nap full_schema.sql that bai!" -ForegroundColor Red
    Read-Host "Nhan Enter de thoat"
    exit 1
}

# 6. Run seed_data.sql
Write-Host ""
Write-Host "Dang nap du lieu mau (seed_data.sql)..." -ForegroundColor Gray
Write-Host "====================================================" -ForegroundColor Gray
$seedPath = Join-Path $PSScriptRoot "seed_data.sql"
& $psqlPath -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $seedPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "[LOI] Nap seed_data.sql that bai!" -ForegroundColor Red
    Read-Host "Nhan Enter de thoat"
    exit 1
}

# Clean password and encoding env variables
$env:PGPASSWORD = $null
$env:PGCLIENTENCODING = $null

Write-Host ""
Write-Host "====================================================" -ForegroundColor Green
Write-Host "             KHOI TAO DATABASE HOAN TAT!            " -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Green
Write-Host "Ban da co the khoi dong ung dung WareHub." -ForegroundColor Green
Write-Host ""
Read-Host "Nhan Enter de dong cua so"
