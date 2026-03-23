# ============================================================
# DirtyWave — Khoi dong toan bo: Frontend + Cloudflare Tunnel + Backend
# Chay: .\start-with-cloudflare.ps1
# ============================================================

$CLOUDFLARED   = "C:\Users\ASUS\Downloads\cloudflared.exe"
$FRONTEND_DIR  = "C:\Users\ASUS\Downloads\DATNvue2"
$BACKEND_DIR   = "C:\Users\ASUS\Downloads\DATN-API"
$FRONTEND_PORT = 5173
$BACKEND_PORT  = 8080

# ---------- Mail settings (thay doi neu can) ----------
$MAIL_HOST     = "smtp.gmail.com"
$MAIL_PORT     = "587"
$MAIL_USER     = "dinhtung2517@gmail.com"
$MAIL_PASS     = "cfekzokpfymwkibu"   # App Password Gmail
# -------------------------------------------------------

Write-Host ""
Write-Host "=== DirtyWave Startup ===" -ForegroundColor Cyan

# 1. Giai phong cac port cu
foreach ($port in @($FRONTEND_PORT, $BACKEND_PORT)) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 800
        Write-Host "  [OK] Da dong process tren port $port" -ForegroundColor Yellow
    }
}

# 2. Khoi dong Frontend (Vite) trong cua so rieng
Write-Host ""
Write-Host "  [1/3] Khoi dong Vite dev server (port $FRONTEND_PORT)..." -ForegroundColor Green
$vite = Start-Process powershell -ArgumentList "-NoExit -Command `"cd '$FRONTEND_DIR'; npm run dev`"" -PassThru
Start-Sleep -Seconds 5

# Kiem tra Vite da len chua
$vitePing = $null
for ($i = 0; $i -lt 12; $i++) {
    try {
        $vitePing = Invoke-WebRequest -Uri "http://localhost:$FRONTEND_PORT" -UseBasicParsing -TimeoutSec 3
        break
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $vitePing) {
    Write-Host "  [WARN] Vite chua phan hoi sau 30s, van tiep tuc..." -ForegroundColor Yellow
}

# 3. Khoi dong Cloudflare Tunnel va doc URL
Write-Host ""
Write-Host "  [2/3] Khoi dong Cloudflare Quick Tunnel..." -ForegroundColor Green

$cfLogFile = "$env:TEMP\cloudflared_tunnel.log"
if (Test-Path $cfLogFile) { Remove-Item $cfLogFile -Force }

$cfProc = Start-Process -FilePath $CLOUDFLARED `
    -ArgumentList "tunnel", "--url", "http://localhost:$FRONTEND_PORT" `
    -RedirectStandardError $cfLogFile `
    -PassThru -WindowStyle Hidden

# Doi cloudflare print URL (timeout 30s)
$tunnelUrl = $null
$waited = 0
Write-Host "  Dang lay URL tunnel" -NoNewline -ForegroundColor DarkCyan
while ($waited -lt 30 -and -not $tunnelUrl) {
    Start-Sleep -Seconds 1
    $waited++
    Write-Host "." -NoNewline -ForegroundColor DarkCyan
    if (Test-Path $cfLogFile) {
        $content = Get-Content $cfLogFile -Raw -ErrorAction SilentlyContinue
        if ($content -match 'https://[a-z0-9\-]+\.trycloudflare\.com') {
            $tunnelUrl = $matches[0].Trim()
        }
    }
}
Write-Host ""

if (-not $tunnelUrl) {
    Write-Host "  [ERROR] Khong lay duoc URL Cloudflare sau 30s." -ForegroundColor Red
    Write-Host "  Kiem tra log: $cfLogFile"
    exit 1
}

Write-Host ""
Write-Host "  ==================================================" -ForegroundColor Cyan
Write-Host "  TUNNEL URL (gui cho khach):" -ForegroundColor White
Write-Host "  $tunnelUrl" -ForegroundColor Green
Write-Host "  Link tra cuu don hang:" -ForegroundColor White
Write-Host "  $tunnelUrl/tra-cuu-don-hang" -ForegroundColor Green
Write-Host "  ==================================================" -ForegroundColor Cyan
Write-Host ""

# Luu URL ra file de tham khao
"$tunnelUrl" | Out-File "$FRONTEND_DIR\current-tunnel-url.txt" -Encoding utf8
Write-Host "  [INFO] URL da luu vao: current-tunnel-url.txt" -ForegroundColor DarkGray

# 4. Khoi dong Backend voi URL tunnel
Write-Host ""
Write-Host "  [3/3] Khoi dong Spring Boot backend..." -ForegroundColor Green

$lookupUrl = "$tunnelUrl/tra-cuu-don-hang"

$backendArgs = @(
    "-NoExit", "-Command",
    "`$env:MAIL_HOST='$MAIL_HOST'; " +
    "`$env:MAIL_PORT='$MAIL_PORT'; " +
    "`$env:MAIL_USERNAME='$MAIL_USER'; " +
    "`$env:MAIL_PASSWORD='$MAIL_PASS'; " +
    "`$env:LOOKUP_MAIL_FROM='$MAIL_USER'; " +
    "`$env:APP_LOOKUP_PUBLIC_URL='$lookupUrl'; " +
    "cd '$BACKEND_DIR'; .\mvnw.cmd spring-boot:run"
)
Start-Process powershell -ArgumentList $backendArgs

Write-Host ""
Write-Host "=== Tat ca dang khoi dong ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Frontend : http://localhost:$FRONTEND_PORT (local)" -ForegroundColor White
Write-Host "  Tunnel   : $tunnelUrl" -ForegroundColor Green
Write-Host "  Tra cuu  : $tunnelUrl/tra-cuu-don-hang" -ForegroundColor Green
Write-Host ""
Write-Host "  Khach bam link -> vao thang, KHONG co man hinh 'Visit Site'" -ForegroundColor Yellow
Write-Host "  URL se thay doi moi lan chay lai script nay." -ForegroundColor DarkGray
Write-Host ""
Write-Host "Nhan Enter de dong cua so nay (cac server van chay o cua so khac)..."
Read-Host
