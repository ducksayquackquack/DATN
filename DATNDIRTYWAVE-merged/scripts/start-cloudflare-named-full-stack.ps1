param(
  [switch]$NoBackend,
  [int]$FrontendPort = 5173,
  [int]$BackendPort = 8080
)

$ErrorActionPreference = 'Stop'

$rootDir = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $rootDir '.env.local'
$configDir = Join-Path $rootDir '.cloudflared'
$configFile = Join-Path $configDir 'config.yml'

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

function Require-Env($name) {
  $value = [Environment]::GetEnvironmentVariable($name)
  if ([string]::IsNullOrWhiteSpace($value)) {
    throw "Missing required environment variable: $name"
  }
  return $value.Trim()
}

function Ensure-ConfigDir {
  if (-not (Test-Path $configDir)) {
    New-Item -ItemType Directory -Path $configDir | Out-Null
  }
}

function Test-PortReady($port) {
  try {
    $client = New-Object Net.Sockets.TcpClient
    $client.Connect('127.0.0.1', $port)
    $client.Dispose()
    return $true
  } catch {
    return $false
  }
}

function Wait-Port($port, $max = 60) {
  Write-Host "  Waiting for port $port..." -ForegroundColor DarkGray
  for ($i = 0; $i -lt $max; $i++) {
    if (Test-PortReady $port) { return $true }
    Start-Sleep 1
  }
  return $false
}

function Stop-FrontendPortOwner($port) {
  try {
    $processId = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
      Select-Object -First 1 -ExpandProperty OwningProcess
    if ($processId) {
      Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
  } catch {
    # Ignore port cleanup failures.
  }
}

Write-Host ''
Write-Host '==============================================' -ForegroundColor Cyan
Write-Host '  DirtyWave -- Named Tunnel Full Stack Mode  ' -ForegroundColor Cyan
Write-Host '  Stable hostname . Same-origin API proxy    ' -ForegroundColor Cyan
Write-Host '==============================================' -ForegroundColor Cyan
Write-Host ''

$cloudflared = Resolve-CloudflaredCommand
if (-not $cloudflared) {
  Write-Host 'ERROR: cloudflared not found.' -ForegroundColor Red
  Write-Host 'Install: winget install Cloudflare.cloudflared' -ForegroundColor Yellow
  exit 1
}

try {
  $tunnelId = Require-Env 'CLOUDFLARE_TUNNEL_ID'
  $hostname = Require-Env 'CLOUDFLARE_TUNNEL_HOSTNAME'
  $credentialsFile = Require-Env 'CLOUDFLARE_TUNNEL_CREDENTIALS'
  $backendPath = [Environment]::GetEnvironmentVariable('DIRTYWAVE_BACKEND_PATH')

  if (-not (Test-Path $credentialsFile)) {
    throw "Credentials file not found: $credentialsFile"
  }

  if (-not $NoBackend) {
    if ([string]::IsNullOrWhiteSpace($backendPath)) {
      $backendPath = 'C:\Users\ASUS\Downloads\dirtywaveapi\DATNAPI3-merged'
    }

    if (-not (Test-Path $backendPath)) {
      throw "Backend not found at $backendPath. Set DIRTYWAVE_BACKEND_PATH or use -NoBackend if Spring is already running."
    }

    Write-Host 'Starting Spring Boot backend (new window)...' -ForegroundColor Yellow
    Start-Process powershell -ArgumentList @(
      '-NoProfile',
      '-ExecutionPolicy', 'Bypass',
      '-Command',
      "Set-Location '$backendPath'; .\mvnw.cmd spring-boot:run"
    ) -WindowStyle Normal | Out-Null

    if (-not (Wait-Port $BackendPort 90)) {
      throw "Spring Boot did not start on :$BackendPort in time."
    }

    Write-Host "OK  Spring Boot ready on :$BackendPort" -ForegroundColor Green
    Write-Host ''
  } else {
    Write-Host "Skipping backend start (assuming :$BackendPort already running)." -ForegroundColor DarkGray
    Write-Host ''
  }

  Ensure-ConfigDir

  $configContent = @"
tunnel: $tunnelId
credentials-file: $credentialsFile

ingress:
  - hostname: $hostname
    service: http://127.0.0.1:$FrontendPort
  - service: http_status:404
"@

  Set-Content -Path $configFile -Value $configContent -Encoding UTF8

  Set-EnvValue 'VITE_PUBLIC_APP_ORIGIN' "https://$hostname"
  Set-EnvValue 'VITE_API_ORIGIN' ''

  Write-Host "Named tunnel hostname: https://$hostname" -ForegroundColor Green
  Write-Host 'API strategy: browser -> same origin -> Vite proxy -> localhost backend' -ForegroundColor Cyan
  Write-Host 'Saved .env.local -> VITE_API_ORIGIN=(empty for same-origin proxy)' -ForegroundColor Cyan
  Write-Host ''

  Stop-FrontendPortOwner $FrontendPort

  Write-Host 'Starting Cloudflare named tunnel...' -ForegroundColor Yellow
  Start-Process -FilePath $cloudflared `
    -ArgumentList @('tunnel', '--config', $configFile, 'run', $tunnelId) `
    -WorkingDirectory $rootDir | Out-Null

  Write-Host 'Starting Vite...' -ForegroundColor Yellow
  Write-Host ''
  Write-Host '==============================================' -ForegroundColor Green
  Write-Host " LIVE URL: https://$hostname" -ForegroundColor Green
  Write-Host '==============================================' -ForegroundColor Green
  Write-Host ''

  Set-Location $rootDir
  npm run dev:clean
} catch {
  Write-Host $_.Exception.Message -ForegroundColor Red
  Write-Host ''
  Write-Host 'Required setup for named tunnel:' -ForegroundColor Yellow
  Write-Host '  1. Create a Cloudflare Tunnel in Zero Trust' -ForegroundColor Yellow
  Write-Host '  2. Add a Public Hostname such as live.dirtywave.com or dirtywave.com' -ForegroundColor Yellow
  Write-Host '  3. Set environment variables:' -ForegroundColor Yellow
  Write-Host '     CLOUDFLARE_TUNNEL_ID=<tunnel-id>' -ForegroundColor Yellow
  Write-Host '     CLOUDFLARE_TUNNEL_HOSTNAME=<live.dirtywave.com>' -ForegroundColor Yellow
  Write-Host '     CLOUDFLARE_TUNNEL_CREDENTIALS=<absolute-path-to-credentials-json>' -ForegroundColor Yellow
  Write-Host '     DIRTYWAVE_BACKEND_PATH=<optional-spring-project-path>' -ForegroundColor Yellow
  exit 1
}