$ErrorActionPreference = 'Stop'

function Get-ErrorBody([System.Management.Automation.ErrorRecord]$err) {
  if ($err.Exception.Response -ne $null) {
    try {
      $reader = New-Object IO.StreamReader($err.Exception.Response.GetResponseStream())
      return $reader.ReadToEnd()
    } catch {
      return $err.Exception.Message
    }
  }
  return $err.Exception.Message
}

function Assert-FailRequest($name, $scriptBlock) {
  try {
    & $scriptBlock | Out-Null
    "FAIL | $name | expected HTTP 4xx but request succeeded"
  } catch {
    $body = Get-ErrorBody $_
    "PASS | $name | rejected as expected | body=$body"
  }
}

function Assert-SuccessRequest($name, $scriptBlock) {
  try {
    $res = & $scriptBlock
    "PASS | $name | success"
    return $res
  } catch {
    $body = Get-ErrorBody $_
    "FAIL | $name | request failed | body=$body"
    return $null
  }
}

function New-VariantCode($prefix) {
  return ($prefix + (Get-Date -Format 'yyyyMMddHHmmssfff') + (Get-Random -Minimum 0 -Maximum 999).ToString('000'))
}

"=== SMOKE START $(Get-Date -Format s) ==="

# Bootstrap data
$products = Invoke-RestMethod -Uri 'http://localhost:8080/api/san-pham' -Method Get
if (-not ($products -is [System.Array])) { $products = @($products) }
$firstProduct = $products | Select-Object -First 1
$firstVariant = $firstProduct.sanPhamChiTiets | Select-Object -First 1
$activeProductStatus = [string]$firstProduct.trangThai
$activeVariantStatus = [string]$firstVariant.trangThai
if ([string]::IsNullOrWhiteSpace($activeProductStatus)) { $activeProductStatus = 'Hoat dong' }
if ([string]::IsNullOrWhiteSpace($activeVariantStatus)) { $activeVariantStatus = 'Hoat dong' }

$promos = Invoke-RestMethod -Uri 'http://localhost:8080/api/khuyen-mai' -Method Get
if (-not ($promos -is [System.Array])) { $promos = @($promos) }
$firstPromo = $promos | Select-Object -First 1
$activePromoStatus = [string]$firstPromo.trangThai
if ([string]::IsNullOrWhiteSpace($activePromoStatus)) { $activePromoStatus = 'Hoat dong' }

$nhanVienRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/nhan-vien' -Method Get
if ($nhanVienRes -is [System.Array]) { $nhanVienList = $nhanVienRes } else { $nhanVienList = @($nhanVienRes) }
$khRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/khach-hang?page=0&size=50' -Method Get
if ($khRes.content) { $khList = $khRes.content } elseif ($khRes -is [System.Array]) { $khList = $khRes } else { $khList = @($khRes) }

$nhanVienId = [int]($nhanVienList[0].id)
$khachHangId = [int]($khList[0].id)
$spctId = [int]($firstVariant.id)
$spctStock = [int]($firstVariant.soLuong)
if ($spctStock -lt 1) { $spctStock = 1 }

$loaiId = [int]$firstVariant.loai.id
$chatLieuId = [int]$firstVariant.chatLieu.id
$mauSacId = [int]$firstVariant.mauSac.id
$kichThuocId = [int]$firstVariant.kichThuoc.id
$hangId = [int]$firstVariant.hang.id
$xuatSuId = [int]$firstVariant.xuatSu.id
$danhMucId = [int]$firstVariant.danhMuc.id

$futureStart = (Get-Date).AddDays(2).ToString('yyyy-MM-ddTHH:mm:ss')
$futureEnd = (Get-Date).AddDays(32).ToString('yyyy-MM-ddTHH:mm:ss')

"INFO | bootstrap | nhanVienId=$nhanVienId khachHangId=$khachHangId spctId=$spctId stock=$spctStock"

# Point 1 - validation boundary
Assert-FailRequest 'P1.1 Product create missing tenSanPham' {
  $body = @{ maSanPham='SP999991'; tenSanPham=''; moTa='invalid'; trangThai=$activeProductStatus; sanPhamChiTiets=@(@{ ma='SP999991-01'; giaBan=100000; soLuong=2; loai=@{id=1}; chatLieu=@{id=1}; mauSac=@{id=1}; kichThuoc=@{id=1}; hang=@{id=1}; xuatSu=@{id=1}; danhMuc=@{id=1}; trangThai=$activeVariantStatus }) } | ConvertTo-Json -Depth 8
  Invoke-RestMethod -Uri 'http://localhost:8080/api/san-pham' -Method Post -ContentType 'application/json' -Body $body
}

Assert-FailRequest 'P1.2 Product create invalid giaBan<=0' {
  $body = @{ maSanPham='SP999992'; tenSanPham='Smoke invalid gia'; moTa='invalid'; trangThai=$activeProductStatus; sanPhamChiTiets=@(@{ ma='SP999992-01'; giaBan=0; soLuong=2; loai=@{id=1}; chatLieu=@{id=1}; mauSac=@{id=1}; kichThuoc=@{id=1}; hang=@{id=1}; xuatSu=@{id=1}; danhMuc=@{id=1}; trangThai=$activeVariantStatus }) } | ConvertTo-Json -Depth 8
  Invoke-RestMethod -Uri 'http://localhost:8080/api/san-pham' -Method Post -ContentType 'application/json' -Body $body
}

Assert-FailRequest 'P1.3 KhuyenMai create percent >100' {
  $body = @{ maKhuyenMai='KM999991'; tenKhuyenMai='Smoke invalid percent'; giaTri=120; donViGiam='PERCENT'; ngayBatDau=$futureStart; ngayKetThuc=$futureEnd; trangThai=$activePromoStatus } | ConvertTo-Json -Depth 6
  Invoke-RestMethod -Uri 'http://localhost:8080/api/khuyen-mai' -Method Post -ContentType 'application/json' -Body $body
}

Assert-FailRequest 'P1.4 KhuyenMai create invalid donViGiam' {
  $body = @{ maKhuyenMai='KM999992'; tenKhuyenMai='Smoke invalid unit'; giaTri=50; donViGiam='USD'; ngayBatDau=$futureStart; ngayKetThuc=$futureEnd; trangThai=$activePromoStatus } | ConvertTo-Json -Depth 6
  Invoke-RestMethod -Uri 'http://localhost:8080/api/khuyen-mai' -Method Post -ContentType 'application/json' -Body $body
}

# Point 2 - stock guard
$createOrderBody = @{ nhanVienId=$nhanVienId; khachHangId=$khachHangId; soDienThoaiNhanHang='0912345678'; diaChiNhanHang='Smoke test order'; phiShip=0; phuongThucThanhToan='COD'; orderType='ONLINE'; orderStatusCode='CHO_XAC_NHAN' } | ConvertTo-Json -Depth 6
$orderCreate = Invoke-RestMethod -Uri 'http://localhost:8080/api/hoa-don' -Method Post -ContentType 'application/json' -Body $createOrderBody
$orderId = [int]($orderCreate.hoaDon.id)
"INFO | stock-test | createdOrderId=$orderId"

Assert-FailRequest 'P2.1 Add item quantity > stock rejected by backend' {
  $body = @{ spctId=$spctId; soLuong=($spctStock + 9999) } | ConvertTo-Json
  Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}/items" -f $orderId) -Method Post -ContentType 'application/json' -Body $body
}

$okAddBody = @{ spctId=$spctId; soLuong=1 } | ConvertTo-Json
$detailAfterAdd = Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}/items" -f $orderId) -Method Post -ContentType 'application/json' -Body $okAddBody
$itemId = [int]($detailAfterAdd.items | Select-Object -First 1).id
"INFO | stock-test | createdItemId=$itemId"

Assert-FailRequest 'P2.2 Update item quantity > stock rejected by backend' {
  $body = @{ soLuong=($spctStock + 9999) } | ConvertTo-Json
  Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}/items/{1}" -f $orderId, $itemId) -Method Put -ContentType 'application/json' -Body $body
}

# Point 3 - soft-delete
$softName = "Smoke SoftDelete " + (Get-Date -Format 'yyyyMMddHHmmss')
$softPayload = @{ tenSanPham=$softName; moTa='smoke soft delete'; trangThai=$activeProductStatus; sanPhamChiTiets=@(@{ ma=(New-VariantCode 'SV'); giaNhap=120000; giaBan=199000; soLuong=5; loai=@{id=$loaiId}; chatLieu=@{id=$chatLieuId}; mauSac=@{id=$mauSacId}; kichThuoc=@{id=$kichThuocId}; hang=@{id=$hangId}; xuatSu=@{id=$xuatSuId}; danhMuc=@{id=$danhMucId}; trangThai=$activeVariantStatus }) } | ConvertTo-Json -Depth 8
try {
  $createdSoftRaw = Invoke-RestMethod -Uri 'http://localhost:8080/api/san-pham' -Method Post -ContentType 'application/json' -Body $softPayload
  $createdSoft = $createdSoftRaw
  if ($createdSoftRaw.sanPham) { $createdSoft = $createdSoftRaw.sanPham }

  $softId = 0
  $softCode = ''
  if ($createdSoft.id) { $softId = [int]$createdSoft.id }
  if ($createdSoft.maSanPham) { $softCode = [string]$createdSoft.maSanPham }

  if ($softId -gt 0) {
    "PASS | P3.1 Create product without code (server should generate) | id=$softId code=$softCode"
    "INFO | soft-delete | createdId=$softId code=$softCode"

    Invoke-RestMethod -Uri ("http://localhost:8080/api/san-pham/{0}" -f $softId) -Method Delete | Out-Null
    "PASS | P3.2 Delete product endpoint call (soft-delete path) | id=$softId"

    $afterDelete = Invoke-RestMethod -Uri ("http://localhost:8080/api/san-pham/{0}" -f $softId) -Method Get
    $statusOk = [string]$afterDelete.trangThai
    $variantsAfter = @($afterDelete.sanPhamChiTiets)
    $inactiveVariants = @($variantsAfter | Where-Object { [string]$_.trangThai -ne $activeVariantStatus })
    if ($statusOk -ne $activeProductStatus) {
      "PASS | P3.3 Product remains and is inactive after delete | status=$statusOk"
    } else {
      "FAIL | P3.3 Product status not inactive after delete | status=$statusOk"
    }
    if ($variantsAfter.Count -eq 0 -or $inactiveVariants.Count -eq $variantsAfter.Count) {
      "PASS | P3.4 Variant statuses inactive after product delete | totalVariants=$($variantsAfter.Count) inactive=$($inactiveVariants.Count)"
    } else {
      "FAIL | P3.4 Not all variants inactive after product delete | totalVariants=$($variantsAfter.Count) inactive=$($inactiveVariants.Count)"
    }
  } else {
    "FAIL | P3.1 Create product response missing id | raw=$($createdSoftRaw | ConvertTo-Json -Depth 4 -Compress)"
    "FAIL | P3.2/P3.3/P3.4 skipped due create failure"
  }
} catch {
  $body = Get-ErrorBody $_
  "FAIL | P3.1/P3.2/P3.3/P3.4 execution error | body=$body"
}

# Point 4 - backend auto code
$prodCodes = @()
$prodErr = @()
1..3 | ForEach-Object {
  $name = "Smoke AutoCode Product " + $_ + ' ' + (Get-Date -Format 'yyyyMMddHHmmssfff')
  $payload = @{ tenSanPham=$name; moTa='auto code'; trangThai=$activeProductStatus; sanPhamChiTiets=@(@{ ma=(New-VariantCode 'AV'); giaNhap=90000; giaBan=159000; soLuong=3; loai=@{id=$loaiId}; chatLieu=@{id=$chatLieuId}; mauSac=@{id=$mauSacId}; kichThuoc=@{id=$kichThuocId}; hang=@{id=$hangId}; xuatSu=@{id=$xuatSuId}; danhMuc=@{id=$danhMucId}; trangThai=$activeVariantStatus }) } | ConvertTo-Json -Depth 8
  try {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/san-pham' -Method Post -ContentType 'application/json' -Body $payload
    $prodCodes += [string]$r.maSanPham
  } catch {
    $prodErr += (Get-ErrorBody $_)
  }
}
$uniqueProdCodes = ($prodCodes | Select-Object -Unique)
if ($prodErr.Count -eq 0 -and $uniqueProdCodes.Count -eq $prodCodes.Count -and (($prodCodes | Where-Object { $_ -match '^SP\d+$' }).Count -eq $prodCodes.Count)) {
  "PASS | P4.1 Product backend auto-code generated unique codes | codes=$($prodCodes -join ',')"
} else {
  "FAIL | P4.1 Product auto-code uniqueness/format failed | codes=$($prodCodes -join ',') errors=$($prodErr -join ' || ')"
}

$kmCodes = @()
$kmErr = @()
1..3 | ForEach-Object {
  $payload = @{ tenKhuyenMai=("Smoke AutoCode KM " + $_ + ' ' + (Get-Date -Format 'yyyyMMddHHmmssfff')); giaTri=10; donViGiam='PERCENT'; ngayBatDau=$futureStart; ngayKetThuc=$futureEnd; trangThai=$activePromoStatus } | ConvertTo-Json -Depth 6
  try {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/khuyen-mai' -Method Post -ContentType 'application/json' -Body $payload
    $kmCodes += [string]$r.maKhuyenMai
  } catch {
    $kmErr += (Get-ErrorBody $_)
  }
}
$uniqueKmCodes = ($kmCodes | Select-Object -Unique)
if ($kmErr.Count -eq 0 -and $uniqueKmCodes.Count -eq $kmCodes.Count -and (($kmCodes | Where-Object { $_ -match '^KM\d{3,}$' }).Count -eq $kmCodes.Count)) {
  "PASS | P4.2 KhuyenMai backend auto-code generated unique codes | codes=$($kmCodes -join ',')"
} else {
  "FAIL | P4.2 KhuyenMai auto-code uniqueness/format failed | codes=$($kmCodes -join ',') errors=$($kmErr -join ' || ')"
}

$jobScript = {
  param($idx)
  $body = @{ tenKhuyenMai=("Smoke KM Burst " + $idx + ' ' + (Get-Date -Format 'yyyyMMddHHmmssfff')); giaTri=5; donViGiam='PERCENT'; ngayBatDau=$using:futureStart; ngayKetThuc=$using:futureEnd; trangThai=$using:activePromoStatus } | ConvertTo-Json -Depth 6
  try {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/khuyen-mai' -Method Post -ContentType 'application/json' -Body $body
    return $r.maKhuyenMai
  } catch {
    return "ERR:" + $_.Exception.Message
  }
}
$jobs = @()
1..5 | ForEach-Object { $jobs += Start-Job -ScriptBlock $jobScript -ArgumentList $_ }
Wait-Job -Job $jobs | Out-Null
$burstCodes = @($jobs | Receive-Job)
$jobs | Remove-Job -Force | Out-Null
$burstOkCodes = $burstCodes | Where-Object { $_ -is [string] -and $_ -match '^KM\d{3,}$' }
$burstErr = $burstCodes | Where-Object { $_ -like 'ERR:*' }
if ($burstErr.Count -eq 0 -and ($burstOkCodes | Select-Object -Unique).Count -eq $burstOkCodes.Count) {
  "PASS | P4.3 Burst create race-safety evidence (no duplicate/error) | codes=$($burstOkCodes -join ',')"
} else {
  "FAIL | P4.3 Burst create had duplicates/errors | raw=$($burstCodes -join ',')"
}

"=== SMOKE END $(Get-Date -Format s) ==="
