param(
  [int]$Port = 5173,
  [switch]$SkipBackend
)

$ErrorActionPreference = 'Stop'
$envFile = Join-Path (Join-Path $PSScriptRoot '..') '.env.local'
$rootDir = Join-Path $PSScriptRoot '..'
$backendDir = Join-Path $rootDir 'backend'
$cfLogFile = Join-Path $env:TEMP 'dirtywave-cloudflare-tunnel.log'
$cfErrLogFile = Join-Path $env:TEMP 'dirtywave-cloudflare-tunnel.err.log'
$cfPidFile = Join-Path $env:TEMP 'dirtywave-cloudflare-tunnel.pid'
$backendPidFile = Join-Path $env:TEMP 'dirtywave-node-backend.pid'

function Stop-PortProcess($port) {
  $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($conn -and $conn.OwningProcess) {
    try {
      Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
      Start-Sleep -Milliseconds 400
      Write-Host "  Released port $port (PID $($conn.OwningProcess))" -ForegroundColor DarkGray
    } catch {
    }
  }
}

function Stop-StaleBackendProcess {
  if (Test-Path $backendPidFile) {
    $rawPid = Get-Content $backendPidFile -Raw -ErrorAction SilentlyContinue
    $pidValue = 0
    [void][int]::TryParse($rawPid.ToString().Trim(), [ref]$pidValue)
    if ($pidValue -gt 0) {
      try {
        Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
      } catch {
      }
    }
    Remove-Item $backendPidFile -Force -ErrorAction SilentlyContinue
  }
}

function Resolve-CloudflaredCommand {
  $cmd = Get-Command cloudflared -ErrorAction SilentlyContinue
  if ($cmd) {
    return $cmd.Source
  }

  $fallback = 'C:\Users\ASUS\Downloads\cloudflared.exe'
  if (Test-Path $fallback) {
    return $fallback
  }

  return $null
}

function Set-EnvValue($key, $value) {
  $content = ''
  if (Test-Path $envFile) {
    $content = Get-Content $envFile -Raw -ErrorAction SilentlyContinue
    if ($null -eq $content) {
      $content = ''
    }
  }

  if ($content -match "(?m)^$key=") {
    $content = [Regex]::Replace($content, "(?m)^$key=.*$", "$key=$value")
  } else {
    $content = $content.TrimEnd() + "`n$key=$value"
  }

  Set-Content -Path $envFile -Value $content.TrimEnd() -Encoding UTF8
}

function Stop-StaleTunnelProcess {
  if (Test-Path $cfPidFile) {
    $rawPid = Get-Content $cfPidFile -Raw -ErrorAction SilentlyContinue
    $pidValue = 0
    [void][int]::TryParse($rawPid.ToString().Trim(), [ref]$pidValue)
    if ($pidValue -gt 0) {
      try {
        $staleProcess = Get-Process -Id $pidValue -ErrorAction Stop
        Stop-Process -Id $staleProcess.Id -Force -ErrorAction SilentlyContinue
      } catch {
      }
    }
    Remove-Item $cfPidFile -Force -ErrorAction SilentlyContinue
  }
}

function Read-TunnelLogContent {
  $parts = @()
  foreach ($path in @($cfLogFile, $cfErrLogFile)) {
    if (Test-Path $path) {
      $parts += (Get-Content $path -Raw -ErrorAction SilentlyContinue)
    }
  }
  return ($parts -join "`n")
}

function Read-TunnelUrlFromLog {
  $content = Read-TunnelLogContent
  if ($content -match 'https://[a-z0-9\-]+\.trycloudflare\.com') {
    return $matches[0]
  }

  return $null
}

Write-Host ''
Write-Host '=== DIRTYWAVE DEV STARTER (CLOUDFLARE) ===' -ForegroundColor Cyan
Write-Host ''

if (-not $SkipBackend) {
  if (-not (Test-Path (Join-Path $backendDir 'package.json'))) {
    Write-Host "ERROR: Backend folder missing package.json at $backendDir" -ForegroundColor Red
    exit 1
  }

  Stop-StaleBackendProcess
  Stop-PortProcess 3000

  Write-Host 'Starting Node backend on :3000...' -ForegroundColor Yellow
  $backendProc = Start-Process -FilePath 'powershell' `
    -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', "Set-Location '$backendDir'; npm run dev") `
    -WindowStyle Hidden `
    -PassThru

  Set-Content -Path $backendPidFile -Value $backendProc.Id -Encoding ASCII
  Start-Sleep -Seconds 1
}

$cloudflared = Resolve-CloudflaredCommand
if (-not $cloudflared) {
  Write-Host 'ERROR: cloudflared not found.' -ForegroundColor Red
  Write-Host 'Install: winget install Cloudflare.cloudflared' -ForegroundColor Yellow
  exit 1
}

if (Test-Path $cfLogFile) {
  Remove-Item $cfLogFile -Force -ErrorAction SilentlyContinue
}
if (Test-Path $cfErrLogFile) {
  Remove-Item $cfErrLogFile -Force -ErrorAction SilentlyContinue
}

Stop-StaleTunnelProcess

Write-Host 'Starting Cloudflare tunnel...' -ForegroundColor Yellow
$cfProc = Start-Process -FilePath $cloudflared `
  -ArgumentList @('tunnel', '--url', "http://127.0.0.1:$Port", '--no-autoupdate') `
  -RedirectStandardOutput $cfLogFile `
  -RedirectStandardError $cfErrLogFile `
  -WindowStyle Hidden `
  -PassThru

Set-Content -Path $cfPidFile -Value $cfProc.Id -Encoding ASCII

$tunnelUrl = $null
for ($i = 1; $i -le 25; $i++) {
  Start-Sleep -Seconds 1
  $tunnelUrl = Read-TunnelUrlFromLog
  if ($tunnelUrl) { break }
  Write-Host "  Waiting tunnel URL... ($i/25)" -ForegroundColor DarkGray
}

if (-not $tunnelUrl) {
  Write-Host 'ERROR: Cannot read Cloudflare URL from log.' -ForegroundColor Red
  Write-Host "Check logs: $cfLogFile ; $cfErrLogFile" -ForegroundColor Yellow
  if ($cfProc -and -not $cfProc.HasExited) {
    Stop-Process -Id $cfProc.Id -Force -ErrorAction SilentlyContinue
  }
  Remove-Item $cfPidFile -Force -ErrorAction SilentlyContinue
  exit 1
}

Set-EnvValue 'VITE_PUBLIC_APP_ORIGIN' $tunnelUrl
Set-EnvValue 'VITE_API_ORIGIN' ''

Write-Host "Tunnel URL: $tunnelUrl" -ForegroundColor Green
Write-Host "Saved .env.local -> VITE_PUBLIC_APP_ORIGIN=$tunnelUrl" -ForegroundColor Cyan
Write-Host "Saved .env.local -> VITE_API_ORIGIN=(auto)" -ForegroundColor Cyan
Write-Host "Cloudflared PID: $($cfProc.Id)" -ForegroundColor DarkGray
Write-Host ''
Write-Host 'Starting Vite...' -ForegroundColor Yellow
if ($SkipBackend) {
  Write-Host '(Node backend is skipped; ensure mail backend is already running on port 3000)' -ForegroundColor DarkGray
} else {
  Write-Host '(Node backend started automatically on port 3000)' -ForegroundColor DarkGray
}
Write-Host ''

Set-Location $rootDir
npm run dev
