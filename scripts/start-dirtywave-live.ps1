param(
  [switch]$NoBackend
)

$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent

function Has-NamedTunnelConfig {
  $required = @(
    'CLOUDFLARE_TUNNEL_ID',
    'CLOUDFLARE_TUNNEL_HOSTNAME',
    'CLOUDFLARE_TUNNEL_CREDENTIALS'
  )

  foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value)) {
      return $false
    }
  }

  return $true
}

Write-Host ''
Write-Host '==============================================' -ForegroundColor Cyan
Write-Host '  DirtyWave Live Launcher (Smart Mode)       ' -ForegroundColor Cyan
Write-Host '==============================================' -ForegroundColor Cyan

$namedScript = Join-Path $PSScriptRoot 'start-cloudflare-named-full-stack.ps1'
$quickScript = Join-Path $PSScriptRoot 'start-cloudflare-full-stack.ps1'

if (Has-NamedTunnelConfig) {
  Write-Host ''
  Write-Host 'Detected Named Tunnel environment variables.' -ForegroundColor Green
  Write-Host 'Using stable hostname mode (live.yourdomain.com / dirtywave.com).' -ForegroundColor Green
  Write-Host ''

  if ($NoBackend) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File $namedScript -NoBackend
  } else {
    & powershell -NoProfile -ExecutionPolicy Bypass -File $namedScript
  }
  exit $LASTEXITCODE
}

Write-Host ''
Write-Host 'Named Tunnel is not configured yet.' -ForegroundColor Yellow
Write-Host 'Falling back to Quick Tunnel (random URL) so you can demo right now.' -ForegroundColor Yellow
Write-Host 'To get stable URL later, set: CLOUDFLARE_TUNNEL_ID, CLOUDFLARE_TUNNEL_HOSTNAME, CLOUDFLARE_TUNNEL_CREDENTIALS' -ForegroundColor DarkYellow
Write-Host ''

if ($NoBackend) {
  & powershell -NoProfile -ExecutionPolicy Bypass -File $quickScript -NoBackend
} else {
  & powershell -NoProfile -ExecutionPolicy Bypass -File $quickScript
}

exit $LASTEXITCODE