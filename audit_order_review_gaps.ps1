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

"=== ORDER GAP AUDIT START $(Get-Date -Format s) ==="

$products = Invoke-RestMethod -Uri 'http://localhost:8080/api/san-pham' -Method Get
if (-not ($products -is [System.Array])) { $products = @($products) }
$firstProduct = $products | Select-Object -First 1
$firstVariant = @($firstProduct.sanPhamChiTiets) | Select-Object -First 1
$expectedProductCode = [string]$firstProduct.maSanPham
$spctId = [int]$firstVariant.id

$nhanVienRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/nhan-vien' -Method Get
if ($nhanVienRes -is [System.Array]) { $nhanVienList = $nhanVienRes } else { $nhanVienList = @($nhanVienRes) }
$khRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/khach-hang?page=0&size=50' -Method Get
if ($khRes.content) { $khList = $khRes.content } elseif ($khRes -is [System.Array]) { $khList = $khRes } else { $khList = @($khRes) }
$nhanVienId = [int]$nhanVienList[0].id
$khachHangId = [int]$khList[0].id

# 1) Product code consistency in invoice detail
$order1Body = @{ nhanVienId=$nhanVienId; khachHangId=$khachHangId; soDienThoaiNhanHang='0912345678'; diaChiNhanHang='Audit product code'; phiShip=0; phuongThucThanhToan='COD'; orderType='ONLINE'; orderStatusCode='CHO_XAC_NHAN' } | ConvertTo-Json -Depth 6
$order1 = Invoke-RestMethod -Uri 'http://localhost:8080/api/hoa-don' -Method Post -ContentType 'application/json' -Body $order1Body
$order1Id = [int]$order1.hoaDon.id
Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}/items" -f $order1Id) -Method Post -ContentType 'application/json' -Body (@{ spctId=$spctId; soLuong=1 } | ConvertTo-Json) | Out-Null
$order1Detail = Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $order1Id) -Method Get
$itemCode = [string](@($order1Detail.items)[0].maSanPham)
if ($itemCode -eq $expectedProductCode) {
  "PASS | C1 Product code in invoice detail matches product management code | code=$itemCode"
} else {
  "FAIL | C1 Product code mismatch | invoice=$itemCode expected=$expectedProductCode"
}

# 2) Backend transition validation
Assert-FailRequest 'C2.1 POS order cannot transition to DANG_GIAO on backend' {
  $createPos = @{ nhanVienId=$nhanVienId; khachHangId=$khachHangId; soDienThoaiNhanHang='0912345678'; diaChiNhanHang='Mua tai quay'; phiShip=0; phuongThucThanhToan='CASH'; orderType='POS'; orderStatusCode='CHO_LAY_HANG'; statusNote='[POS] Audit order' } | ConvertTo-Json -Depth 6
  $created = Invoke-RestMethod -Uri 'http://localhost:8080/api/hoa-don' -Method Post -ContentType 'application/json' -Body $createPos
  $id = [int]$created.hoaDon.id
  $update = @{ orderStatusCode='DANG_GIAO'; phuongThucThanhToan='CASH'; orderType='POS'; statusNote='[POS] Audit order' } | ConvertTo-Json -Depth 6
  Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $id) -Method Put -ContentType 'application/json' -Body $update
}

Assert-FailRequest 'C2.2 ONLINE order cannot jump to HOAN_THANH from CHO_XAC_NHAN on backend' {
  $createOnline = @{ nhanVienId=$nhanVienId; khachHangId=$khachHangId; soDienThoaiNhanHang='0912345678'; diaChiNhanHang='Audit online'; phiShip=0; phuongThucThanhToan='COD'; orderType='ONLINE'; orderStatusCode='CHO_XAC_NHAN'; statusNote='[ONLINE] Audit order' } | ConvertTo-Json -Depth 6
  $created = Invoke-RestMethod -Uri 'http://localhost:8080/api/hoa-don' -Method Post -ContentType 'application/json' -Body $createOnline
  $id = [int]$created.hoaDon.id
  $update = @{ orderStatusCode='HOAN_THANH'; phuongThucThanhToan='COD'; orderType='ONLINE'; statusNote='[ONLINE] Audit order' } | ConvertTo-Json -Depth 6
  Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $id) -Method Put -ContentType 'application/json' -Body $update
}

Assert-FailRequest 'C2.3 VNPAY order cannot complete before employee-confirm tag on backend' {
  $createVnpay = @{ nhanVienId=$nhanVienId; khachHangId=$khachHangId; soDienThoaiNhanHang='0912345678'; diaChiNhanHang='Mua tai quay'; phiShip=0; phuongThucThanhToan='VNPAY'; orderType='POS'; orderStatusCode='CHO_LAY_HANG'; statusNote='[POS] Don VNPAY cho khach xac nhan' } | ConvertTo-Json -Depth 6
  $created = Invoke-RestMethod -Uri 'http://localhost:8080/api/hoa-don' -Method Post -ContentType 'application/json' -Body $createVnpay
  $id = [int]$created.hoaDon.id
  $update = @{ orderStatusCode='HOAN_THANH'; phuongThucThanhToan='VNPAY'; orderType='POS'; statusNote='[POS] Don VNPAY cho khach xac nhan' } | ConvertTo-Json -Depth 6
  Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $id) -Method Put -ContentType 'application/json' -Body $update
}

# 3) Backend source-of-truth fields for admin/customer sync
$flowCreate = @{ nhanVienId=$nhanVienId; khachHangId=$khachHangId; soDienThoaiNhanHang='0912345678'; diaChiNhanHang='Audit online'; phiShip=0; phuongThucThanhToan='VNPAY'; orderType='ONLINE'; orderStatusCode='CHO_XAC_NHAN'; statusNote='[ONLINE] Don VNPAY cho khach xac nhan' } | ConvertTo-Json -Depth 6
$flowOrder = Invoke-RestMethod -Uri 'http://localhost:8080/api/hoa-don' -Method Post -ContentType 'application/json' -Body $flowCreate
$flowId = [int]$flowOrder.hoaDon.id
$flowDetail1 = Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $flowId) -Method Get
if ($flowDetail1.hoaDon.paymentFlowCode -and $flowDetail1.hoaDon.paymentFlowLabel -and $flowDetail1.hoaDon.paymentFlowTone) {
  "PASS | C3.1 Backend detail exposes canonical payment flow fields | code=$($flowDetail1.hoaDon.paymentFlowCode)"
} else {
  "FAIL | C3.1 Backend detail missing canonical payment flow fields"
}

$customerNote = '[ONLINE] Don VNPAY cho nhan vien kiem tra | [PAYMENT_FLOW:VNPAY_CUSTOMER_CONFIRMED] Khach hang da bam Xac nhan thanh toan.'
Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $flowId) -Method Put -ContentType 'application/json' -Body (@{ orderStatusCode='CHO_XAC_NHAN'; phuongThucThanhToan='VNPAY'; orderType='ONLINE'; statusNote=$customerNote } | ConvertTo-Json -Depth 6) | Out-Null
$flowDetail2 = Invoke-RestMethod -Uri ("http://localhost:8080/api/hoa-don/{0}" -f $flowId) -Method Get
if ([string]$flowDetail2.hoaDon.paymentFlowCode -eq 'WAIT_EMPLOYEE') {
  "PASS | C3.2 Backend recalculates payment flow state after customer confirmation | code=$($flowDetail2.hoaDon.paymentFlowCode)"
} else {
  "FAIL | C3.2 Backend payment flow state not updated from status note tags | code=$($flowDetail2.hoaDon.paymentFlowCode)"
}

"=== ORDER GAP AUDIT END $(Get-Date -Format s) ==="
