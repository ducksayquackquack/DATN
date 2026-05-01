param(
  [string]$FrontendPath = (Join-Path $PSScriptRoot ".."),
  [string]$BackendPath = (Join-Path $PSScriptRoot "..\..\DATN-API"),
  [string]$OutputDir = (Join-Path (Join-Path $PSScriptRoot "..") "release"),
  [switch]$SkipBackend
)

$ErrorActionPreference = "Stop"

$excludeDirSegments = @(
  "\\.git\\",
  "\\.github\\",
  "\\.vscode\\",
  "\\.idea\\",
  "\\node_modules\\",
  "\\dist\\",
  "\\build\\",
  "\\out\\",
  "\\target\\",
  "\\coverage\\",
  "\\logs\\",
  "\\tmp\\",
  "\\.cache\\"
)

$excludeFileRegex = @(
  "(^|\\\\)npm-debug\\.log$",
  "(^|\\\\)yarn-error\\.log$",
  "(^|\\\\)pnpm-debug\\.log$",
  "(^|\\\\)\\.DS_Store$"
)

function Test-ExcludedFile {
  param(
    [string]$RootPath,
    [string]$FullPath,
    [string]$FileName
  )

  $relative = $FullPath.Substring($RootPath.Length).TrimStart([char]'\', [char]'/')
  $normalized = "\\" + ($relative -replace '/', '\\')

  foreach ($segment in $excludeDirSegments) {
    if ($normalized -match [regex]::Escape($segment)) {
      return $true
    }
  }

  foreach ($regex in $excludeFileRegex) {
    if ($normalized -match $regex) {
      return $true
    }
  }

  if ($FileName -match '^\.env(\..+)?$' -and $FileName -ne '.env.example') {
    return $true
  }

  return $false
}

function New-ZipForProject {
  param(
    [string]$ProjectPath,
    [string]$ZipName
  )

  if (-not (Test-Path $ProjectPath)) {
    Write-Host "Skip: missing path $ProjectPath" -ForegroundColor Yellow
    return $null
  }

  $root = (Resolve-Path $ProjectPath).Path.TrimEnd([char]'\', [char]'/')
  $stagingRoot = Join-Path $env:TEMP ("zip-ready-" + [guid]::NewGuid().ToString("N"))
  $stagingPath = Join-Path $stagingRoot "payload"

  New-Item -ItemType Directory -Path $stagingPath -Force | Out-Null

  $files = Get-ChildItem -Path $root -Recurse -File -Force |
    Where-Object {
      -not (Test-ExcludedFile -RootPath $root -FullPath $_.FullName -FileName $_.Name)
    }

  foreach ($file in $files) {
    $relative = $file.FullName.Substring($root.Length).TrimStart([char]'\', [char]'/')
    $dest = Join-Path $stagingPath $relative
    $destDir = Split-Path $dest -Parent
    if (-not (Test-Path $destDir)) {
      New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    Copy-Item -LiteralPath $file.FullName -Destination $dest -Force
  }

  if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
  }

  $zipPath = Join-Path $OutputDir $ZipName
  if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
  }

  Compress-Archive -Path (Join-Path $stagingPath "*") -DestinationPath $zipPath -CompressionLevel Optimal
  Remove-Item -Path $stagingRoot -Recurse -Force -ErrorAction SilentlyContinue

  Write-Host "Created: $zipPath" -ForegroundColor Green
  return $zipPath
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmm"

Write-Host "" 
Write-Host "=== ZIP READY 1-CLICK ===" -ForegroundColor Cyan
Write-Host "Frontend path: $FrontendPath"
Write-Host "Backend path : $BackendPath"
Write-Host "Output path  : $OutputDir"
Write-Host ""

$frontendZip = New-ZipForProject -ProjectPath $FrontendPath -ZipName ("DATNDIRTYWAVE-merged-zipready-" + $timestamp + ".zip")

$backendZip = $null
if (-not $SkipBackend) {
  $backendZip = New-ZipForProject -ProjectPath $BackendPath -ZipName ("DATN-API-zipready-" + $timestamp + ".zip")
}

Write-Host ""
Write-Host "Done." -ForegroundColor Cyan
if ($frontendZip) { Write-Host " - Frontend: $frontendZip" }
if ($backendZip) { Write-Host " - Backend : $backendZip" }
if (-not $backendZip -and -not $SkipBackend) {
  Write-Host " - Backend zip not created (path missing). Use -BackendPath to point to DATN-API." -ForegroundColor Yellow
}
