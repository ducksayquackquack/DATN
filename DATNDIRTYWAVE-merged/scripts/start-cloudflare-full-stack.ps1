# DirtyWave Full Stack Launcher
# Starts: Spring Boot API + 2 Cloudflare tunnels + Vite frontend
# Usage : npm run live
#         npm run live:skip-backend   (if Spring Boot already running)

param([switch]$NoBackend)

$rootDir  = Split-Path $PSScriptRoot -Parent
$envFile  = Join-Path $rootDir '.env.local'
$logDir   = Join-Path $env:TEMP 'dirtywave-tunnels'

if (-not (Test-Path $logDir)) { New-Item -ItemType Directory $logDir -Force | Out-Null }

# ---------- helpers ----------

function Find-Cloudflared {
  $c = Get-Command cloudflared -ErrorAction SilentlyContinue
  if ($c) { return $c.Source }
  $local = 'C:\Users\ASUS\Downloads\cloudflared.exe'
  if (Test-Path $local) { return $local }
  return $null
}

function Set-Env($key, $val) {
  $txt = if (Test-Path $envFile) { Get-Content $envFile -Raw } else { '' }
  if (-not $txt) { $txt = '' }
  if ($txt -match "(?m)^$key=") {
    $txt = [regex]::Replace($txt, "(?m)^$key=.*", "$key=$val")
  } else {
    $txt = $txt.TrimEnd() + "`n$key=$val"
  }
  Set-Content $envFile $txt.TrimEnd() -Encoding UTF8
}

function Wait-Port($port, $max = 60) {
  Write-Host "  Waiting for port $port..." -ForegroundColor DarkGray
  for ($i = 0; $i -lt $max; $i++) {
    try { [void][Net.Sockets.TcpClient]::new().Connect('127.0.0.1', $port); return $true } catch {}
    Start-Sleep 1
  }
  return $false
}

function Start-CfTunnel($name, $port) {
  $cf    = Find-Cloudflared
  if (-not $cf) { Write-Host "ERROR: cloudflared not found. Run: winget install Cloudflare.cloudflared" -ForegroundColor Red; exit 1 }
  $logOut = Join-Path $logDir "$name-out.log"
  $logErr = Join-Path $logDir "$name-err.log"
  $pidf   = Join-Path $logDir "$name.pid"

  # Kill previous instance
  if (Test-Path $pidf) {
    $old = [int](Get-Content $pidf -Raw -ErrorAction SilentlyContinue)
    if ($old) { Stop-Process -Id $old -Force -ErrorAction SilentlyContinue }
  }
  '' | Set-Content $logOut -Encoding UTF8
  '' | Set-Content $logErr -Encoding UTF8

  # cloudflared prints the URL to stderr, so capture both streams separately
  $p = Start-Process $cf -ArgumentList "tunnel","--url","http://127.0.0.1:$port","--no-autoupdate" `
         -RedirectStandardOutput $logOut -RedirectStandardError $logErr `
         -WindowStyle Hidden -PassThru
  Set-Content $pidf $p.Id -Encoding ASCII

  # Wait up to 30s for URL (check both stdout and stderr)
  for ($i = 1; $i -le 30; $i++) {
    Start-Sleep 1
    $combined = (Get-Content $logOut -Raw -EA SilentlyContinue) + (Get-Content $logErr -Raw -EA SilentlyContinue)
    if ($combined -match 'https://[a-z0-9-]+\.trycloudflare\.com') { return $matches[0] }
    Write-Host "  [$name] Waiting for tunnel URL... ($i/30)" -ForegroundColor DarkGray
  }

  Write-Host "ERROR: Tunnel for $name did not start." -ForegroundColor Red
  Write-Host "stdout: $logOut" -ForegroundColor DarkGray
  Write-Host "stderr: $logErr" -ForegroundColor DarkGray
  exit 1
}

# -----------------------------------------------

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  DirtyWave  --  Full Stack Live Mode     " -ForegroundColor Cyan
Write-Host "  Free . Cloudflare Tunnels . No deploy   " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Spring Boot
if (-not $NoBackend) {
  $api = 'c:\Users\ASUS\Downloads\dirtywaveapi\DATNAPI3-merged'
  if (-not (Test-Path $api)) {
    Write-Host "ERROR: Backend not found at $api" -ForegroundColor Red
    Write-Host "Tip: use  npm run live:skip-backend  if it is already running." -ForegroundColor Yellow
    exit 1
  }
  Write-Host "Starting Spring Boot backend (new window)..." -ForegroundColor Yellow
  Start-Process powershell -ArgumentList "-NoProfile","-ExecutionPolicy","Bypass","-Command","Set-Location '$api'; .\mvnw.cmd spring-boot:run" -WindowStyle Normal
  if (-not (Wait-Port 8080 90)) {
    Write-Host "ERROR: Spring Boot did not start on :8080 in time." -ForegroundColor Red
    exit 1
  }
  Write-Host "OK  Spring Boot ready on :8080" -ForegroundColor Green
  Write-Host ""
} else {
  Write-Host "Skipping backend start (assuming :8080 already running)." -ForegroundColor DarkGray
  Write-Host ""
}

# 2. Tunnel for FRONTEND only (Vite proxy handles /api → localhost:8080)
# No separate API tunnel needed - avoids CORS issues entirely
Write-Host "Tunnelling frontend (:5173)..." -ForegroundColor Yellow
$frontUrl = Start-CfTunnel 'frontend' 5173
Write-Host "OK  Frontend tunnel -> $frontUrl" -ForegroundColor Green
Write-Host ""

# 3. Write .env.local
# Leave VITE_API_ORIGIN empty so apiOrigin.js uses same-origin proxy routing:
#   https://frontend-tunnel.com/api/X  ->  Vite proxy  ->  localhost:8080/api/X
Set-Env 'VITE_PUBLIC_APP_ORIGIN' $frontUrl
Set-Env 'VITE_API_ORIGIN'        ''
Write-Host "Updated .env.local (API calls go via Vite proxy - no CORS issues)" -ForegroundColor Cyan

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " LIVE URL - share this!"                   -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "  Website: $frontUrl" -ForegroundColor White
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "How it works:" -ForegroundColor DarkGray
Write-Host "  Browser -> $frontUrl/api/X" -ForegroundColor DarkGray
Write-Host "  -> Cloudflare -> Vite proxy -> localhost:8080" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Tunnel stays active while this terminal is open." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop." -ForegroundColor DarkGray
Write-Host ""

# 5. Start Vite (blocking)
Set-Location $rootDir
& npm run dev:clean
