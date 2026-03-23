param(
  [int]$Port = 5173
)

$ErrorActionPreference = 'Stop'
$rootDir = Join-Path $PSScriptRoot '..'
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

Write-Host ''
Write-Host '=== DIRTYWAVE DEV STARTER (CLOUDFLARE NAMED TUNNEL) ===' -ForegroundColor Cyan
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

  if (-not (Test-Path $credentialsFile)) {
    throw "Credentials file not found: $credentialsFile"
  }

  Ensure-ConfigDir

  $configContent = @"
tunnel: $tunnelId
credentials-file: $credentialsFile

ingress:
  - hostname: $hostname
    service: http://127.0.0.1:$Port
  - service: http_status:404
"@

  Set-Content -Path $configFile -Value $configContent -Encoding UTF8

  Set-EnvValue 'VITE_PUBLIC_APP_ORIGIN' "https://$hostname"
  Set-EnvValue 'VITE_API_ORIGIN' ''

  Write-Host "Named tunnel hostname: https://$hostname" -ForegroundColor Green
  Write-Host "Saved .env.local -> VITE_PUBLIC_APP_ORIGIN=https://$hostname" -ForegroundColor Cyan
  Write-Host "Saved .env.local -> VITE_API_ORIGIN=(auto)" -ForegroundColor Cyan
  Write-Host ''
  Write-Host 'Starting Cloudflare named tunnel...' -ForegroundColor Yellow

  Start-Process -FilePath $cloudflared `
    -ArgumentList @('tunnel', '--config', $configFile, 'run', $tunnelId) `
    -WorkingDirectory $rootDir

  Write-Host 'Starting Vite...' -ForegroundColor Yellow
  Write-Host '(Run Spring Boot backend separately on port 8080)' -ForegroundColor DarkGray
  Write-Host ''

  Set-Location $rootDir
  npm run dev
} catch {
  Write-Host $_.Exception.Message -ForegroundColor Red
  Write-Host ''
  Write-Host 'Required setup for named tunnel:' -ForegroundColor Yellow
  Write-Host '  1. Create a Cloudflare Tunnel in your Cloudflare Zero Trust dashboard' -ForegroundColor Yellow
  Write-Host '  2. Map a hostname/subdomain to that tunnel' -ForegroundColor Yellow
  Write-Host '  3. Set environment variables:' -ForegroundColor Yellow
  Write-Host '     CLOUDFLARE_TUNNEL_ID=<tunnel-id>' -ForegroundColor Yellow
  Write-Host '     CLOUDFLARE_TUNNEL_HOSTNAME=<subdomain.yourdomain.com>' -ForegroundColor Yellow
  Write-Host '     CLOUDFLARE_TUNNEL_CREDENTIALS=<absolute-path-to-credentials-json>' -ForegroundColor Yellow
  exit 1
}