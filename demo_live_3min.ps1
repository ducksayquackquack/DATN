$ErrorActionPreference = 'Stop'

Write-Output '=== DEMO 3-MIN START ==='

try {
  $status = (Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/san-pham').StatusCode
  Write-Output ("HEALTH_CHECK=OK;HTTP=" + $status)
} catch {
  Write-Output 'HEALTH_CHECK=FAIL;HTTP=DOWN'
  Write-Output 'START_HINT=Run backend first: & "C:\Users\ASUS\Downloads\DATN-API\mvnw.cmd" -f "C:\Users\ASUS\Downloads\DATN-API\pom.xml" spring-boot:run'
  exit 1
}

$lines = PowerShell -ExecutionPolicy Bypass -File '.\smoke_points_1_4.ps1'

$important = @($lines | Where-Object {
  $_ -like '=== SMOKE START*' -or
  $_ -like '=== SMOKE END*' -or
  $_ -like 'PASS | P*' -or
  $_ -like 'FAIL | P*'
})

$important | ForEach-Object { Write-Output $_ }

$fails = @($important | Where-Object { $_ -like 'FAIL |*' })
if ($fails.Count -eq 0) {
  Write-Output 'DEMO_RESULT=GREEN_ALL_4_POINTS'
  exit 0
}

Write-Output ("DEMO_RESULT=HAS_FAILURES;COUNT=" + $fails.Count)
exit 2
