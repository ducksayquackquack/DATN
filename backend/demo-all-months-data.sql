-- ============================================================
-- Demo data: Distribute orders across all 12 months of 2026
-- Run against DirtyWave database (SQL Server)
-- Uses variables to preserve exact stored Unicode values
-- ============================================================

-- Capture the exact stored Unicode strings that satisfy CHECK constraints
DECLARE @dgiao   NVARCHAR(20);  -- "ÄÃ£ giao" (completed/delivered)
DECLARE @dxl     NVARCHAR(20);  -- "Chá» xá»­ lÃ½" (processing) â€“ fallback
SELECT TOP 1 @dgiao = trangThai FROM HoaDon WHERE id = 1;  -- Da giao row
SELECT TOP 1 @dxl  = trangThai FROM HoaDon WHERE id = 3;  -- Ch? x? ly row

-- â”€â”€â”€ STEP 1: Update existing demo orders (IDs 1-10) to cover months 1,2,4,5,6 â”€â”€â”€â”€

-- T1 â€“ January 2026
UPDATE HoaDon SET
  maHoaDon            = 'HD20260110090000001',
  ngayTao             = '2026-01-10 09:00:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 1550000,
  giaSauGiamGia       = 1550000,
  phuongThucThanhToan = 'CASH',
  idNhanVien          = 2,
  tenKhachHang        = N'Tráº§n HoÃ ng Nam',
  soDienThoaiNhanHang = '0973456782',
  diaChiNhanHang      = N'12 LÃª Lá»£i, Quáº­n 1, TP.HCM'
WHERE id = 1;

UPDATE HoaDon SET
  maHoaDon            = 'HD20260125143000002',
  ngayTao             = '2026-01-25 14:30:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 2100000,
  giaSauGiamGia       = 2100000,
  phuongThucThanhToan = 'CASH',
  idNhanVien          = 3,
  tenKhachHang        = N'LÃª Thu Trang',
  soDienThoaiNhanHang = '0964567893',
  diaChiNhanHang      = N'34 Nguyá»…n Huá»‡, Quáº­n 1, TP.HCM'
WHERE id = 2;

-- T2 â€“ February 2026
UPDATE HoaDon SET
  maHoaDon            = 'HD20260205103000003',
  ngayTao             = '2026-02-05 10:30:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 1800000,
  giaSauGiamGia       = 1800000,
  phuongThucThanhToan = 'COD',
  idNhanVien          = 1,
  tenKhachHang        = N'Pháº¡m VÄƒn KhÃ¡nh',
  soDienThoaiNhanHang = '0955678904',
  diaChiNhanHang      = N'56 Tráº§n HÆ°ng Äáº¡o, Quáº­n 5, TP.HCM'
WHERE id = 3;

UPDATE HoaDon SET
  maHoaDon            = 'HD20260218155000004',
  ngayTao             = '2026-02-18 15:50:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 2450000,
  giaSauGiamGia       = 2450000,
  phuongThucThanhToan = 'CASH',
  idNhanVien          = 4,
  tenKhachHang        = N'HoÃ ng Minh QuÃ¢n',
  soDienThoaiNhanHang = '0946789015',
  diaChiNhanHang      = N'78 BÃ¹i Viá»‡n, Quáº­n 1, TP.HCM'
WHERE id = 4;

-- T4 â€“ April 2026
UPDATE HoaDon SET
  maHoaDon            = 'HD20260403112000005',
  ngayTao             = '2026-04-03 11:20:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 1250000,
  giaSauGiamGia       = 1250000,
  phuongThucThanhToan = 'CASH',
  idNhanVien          = 2,
  tenKhachHang        = N'Äá»— ThÃ¹y Linh',
  soDienThoaiNhanHang = '0937890126',
  diaChiNhanHang      = N'90 VÃµ VÄƒn Táº§n, Quáº­n 3, TP.HCM'
WHERE id = 5;

UPDATE HoaDon SET
  maHoaDon            = 'HD20260422163000006',
  ngayTao             = '2026-04-22 16:30:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 2850000,
  giaSauGiamGia       = 2850000,
  phuongThucThanhToan = 'COD',
  idNhanVien          = 5,
  tenKhachHang        = N'BÃ¹i Äá»©c Anh',
  soDienThoaiNhanHang = '0928901237',
  diaChiNhanHang      = N'15 Äinh TiÃªn HoÃ ng, BÃ¬nh Tháº¡nh, TP.HCM'
WHERE id = 6;

-- T5 â€“ May 2026
UPDATE HoaDon SET
  maHoaDon            = 'HD20260512093000007',
  ngayTao             = '2026-05-12 09:30:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 1950000,
  giaSauGiamGia       = 1950000,
  phuongThucThanhToan = 'CASH',
  idNhanVien          = 3,
  tenKhachHang        = N'VÅ© Thanh Huyá»n',
  soDienThoaiNhanHang = '0919012348',
  diaChiNhanHang      = N'22 LÃ½ Tá»± Trá»ng, Quáº­n 1, TP.HCM'
WHERE id = 7;

UPDATE HoaDon SET
  maHoaDon            = 'HD20260528173000008',
  ngayTao             = '2026-05-28 17:30:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 2300000,
  giaSauGiamGia       = 2300000,
  phuongThucThanhToan = 'VNPAY',
  idNhanVien          = 1,
  tenKhachHang        = N'Nguyá»…n Quá»‘c Báº£o',
  soDienThoaiNhanHang = '0900123459',
  diaChiNhanHang      = N'77 Hai BÃ  TrÆ°ng, Quáº­n 3, TP.HCM'
WHERE id = 8;

-- T6 â€“ June 2026
UPDATE HoaDon SET
  maHoaDon            = 'HD20260608102000009',
  ngayTao             = '2026-06-08 10:20:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 1600000,
  giaSauGiamGia       = 1600000,
  phuongThucThanhToan = 'CASH',
  idNhanVien          = 4,
  tenKhachHang        = N'Phan Minh Äá»©c',
  soDienThoaiNhanHang = '0391234560',
  diaChiNhanHang      = N'44 Nguyá»…n ÄÃ¬nh Chiá»ƒu, Quáº­n 3, TP.HCM'
WHERE id = 9;

UPDATE HoaDon SET
  maHoaDon            = 'HD20260620145000010',
  ngayTao             = '2026-06-20 14:50:00',
  OrderStatusId       = 6,
  trangThai           = @dgiao,
  thanhTien           = 2200000,
  giaSauGiamGia       = 2200000,
  phuongThucThanhToan = 'COD',
  idNhanVien          = 2,
  tenKhachHang        = N'Nguyá»…n Thá»‹ Mai',
  soDienThoaiNhanHang = '0982345671',
  diaChiNhanHang      = N'8 Phan XÃ­ch Long, PhÃº Nhuáº­n, TP.HCM'
WHERE id = 10;


-- â”€â”€â”€ STEP 2: Insert new orders for months 7, 8, 9, 10, 11, 12 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

-- T7 â€“ July 2026
INSERT INTO HoaDon (idNhanVien, idKhachHang, OrderStatusId, maHoaDon, phiShip, giaSauGiamGia, thanhTien,
  tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayTao, trangThai, phuongThucThanhToan)
VALUES
(3, 1, 6, 'HD20260715091500011', 0, 1700000, 1700000,
 N'Nguyá»…n Thá»‹ Mai', '0982345671', N'12 LÃª Lá»£i, Quáº­n 1, TP.HCM',
 '2026-07-15 09:15:00', @dgiao, 'CASH'),

(5, 3, 6, 'HD20260728161000012', 30000, 1940000, 1970000,
 N'LÃª Thu Trang', '0964567893', N'34 Tráº§n PhÃº, Quáº­n 5, TP.HCM',
 '2026-07-28 16:10:00', @dgiao, 'COD'),

(1, 5, 6, 'HD20260710134500013', 0, 2600000, 2600000,
 N'HoÃ ng Minh QuÃ¢n', '0946789015', N'66 Nguyá»…n Thá»‹ Minh Khai, Quáº­n 3, TP.HCM',
 '2026-07-10 13:45:00', @dgiao, 'VNPAY');

-- T8 â€“ August 2026
INSERT INTO HoaDon (idNhanVien, idKhachHang, OrderStatusId, maHoaDon, phiShip, giaSauGiamGia, thanhTien,
  tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayTao, trangThai, phuongThucThanhToan)
VALUES
(2, 6, 6, 'HD20260805102500014', 0, 1150000, 1150000,
 N'Äá»— ThÃ¹y Linh', '0937890126', N'90 VÃµ VÄƒn Táº§n, Quáº­n 3, TP.HCM',
 '2026-08-05 10:25:00', @dgiao, 'CASH'),

(4, 4, 6, 'HD20260820173000015', 0, 2250000, 2250000,
 N'Pháº¡m VÄƒn KhÃ¡nh', '0955678904', N'56 Tráº§n HÆ°ng Äáº¡o, Quáº­n 5, TP.HCM',
 '2026-08-20 17:30:00', @dgiao, 'CASH'),

(6, 2, 6, 'HD20260812091000016', 25000, 900000, 925000,
 N'Tráº§n HoÃ ng Nam', '0973456782', N'12 Nguyá»…n Huá»‡, Quáº­n 1, TP.HCM',
 '2026-08-12 09:10:00', @dgiao, 'COD');

-- T9 â€“ September 2026
INSERT INTO HoaDon (idNhanVien, idKhachHang, OrderStatusId, maHoaDon, phiShip, giaSauGiamGia, thanhTien,
  tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayTao, trangThai, phuongThucThanhToan)
VALUES
(1, 7, 6, 'HD20260910111500017', 0, 3100000, 3100000,
 N'BÃ¹i Äá»©c Anh', '0928901237', N'15 Äinh TiÃªn HoÃ ng, BÃ¬nh Tháº¡nh, TP.HCM',
 '2026-09-10 11:15:00', @dgiao, 'VNPAY'),

(3, 8, 6, 'HD20260925154500018', 30000, 1350000, 1380000,
 N'VÅ© Thanh Huyá»n', '0919012348', N'22 LÃ½ Tá»± Trá»ng, Quáº­n 1, TP.HCM',
 '2026-09-25 15:45:00', @dgiao, 'COD'),

(5, 9, 6, 'HD20260918083000019', 0, 2400000, 2400000,
 N'Nguyá»…n Quá»‘c Báº£o', '0900123459', N'77 Hai BÃ  TrÆ°ng, Quáº­n 3, TP.HCM',
 '2026-09-18 08:30:00', @dgiao, 'CASH');

-- T10 â€“ October 2026
INSERT INTO HoaDon (idNhanVien, idKhachHang, OrderStatusId, maHoaDon, phiShip, giaSauGiamGia, thanhTien,
  tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayTao, trangThai, phuongThucThanhToan)
VALUES
(2, 10, 6, 'HD20261005093000020', 0, 1450000, 1450000,
 N'Phan Minh Äá»©c', '0391234560', N'44 Nguyá»…n ÄÃ¬nh Chiá»ƒu, Quáº­n 3, TP.HCM',
 '2026-10-05 09:30:00', @dgiao, 'CASH'),

(4, 1, 6, 'HD20261018162000021', 0, 2750000, 2750000,
 N'Nguyá»…n Thá»‹ Mai', '0982345671', N'8 Phan XÃ­ch Long, PhÃº Nhuáº­n, TP.HCM',
 '2026-10-18 16:20:00', @dgiao, 'VNPAY'),

(1, 3, 6, 'HD20261025113000022', 30000, 1070000, 1100000,
 N'LÃª Thu Trang', '0964567893', N'34 Nguyá»…n Huá»‡, Quáº­n 1, TP.HCM',
 '2026-10-25 11:30:00', @dgiao, 'COD');

-- T11 â€“ November 2026
INSERT INTO HoaDon (idNhanVien, idKhachHang, OrderStatusId, maHoaDon, phiShip, giaSauGiamGia, thanhTien,
  tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayTao, trangThai, phuongThucThanhToan)
VALUES
(3, 5, 6, 'HD20261110100000023', 0, 1900000, 1900000,
 N'HoÃ ng Minh QuÃ¢n', '0946789015', N'66 Nguyá»…n Thá»‹ Minh Khai, Quáº­n 3, TP.HCM',
 '2026-11-10 10:00:00', @dgiao, 'CASH'),

(5, 6, 6, 'HD20261118145500024', 0, 2500000, 2500000,
 N'Äá»— ThÃ¹y Linh', '0937890126', N'90 VÃµ VÄƒn Táº§n, Quáº­n 3, TP.HCM',
 '2026-11-18 14:55:00', @dgiao, 'VNPAY'),

(2, 4, 6, 'HD20261128171000025', 30000, 1270000, 1300000,
 N'Pháº¡m VÄƒn KhÃ¡nh', '0955678904', N'56 Tráº§n HÆ°ng Äáº¡o, Quáº­n 5, TP.HCM',
 '2026-11-28 17:10:00', @dgiao, 'COD');

-- T12 â€“ December 2026 (Year-end high sales)
INSERT INTO HoaDon (idNhanVien, idKhachHang, OrderStatusId, maHoaDon, phiShip, giaSauGiamGia, thanhTien,
  tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayTao, trangThai, phuongThucThanhToan)
VALUES
(1, 2, 6, 'HD20261205093000026', 0, 3500000, 3500000,
 N'Tráº§n HoÃ ng Nam', '0973456782', N'12 LÃª Lá»£i, Quáº­n 1, TP.HCM',
 '2026-12-05 09:30:00', @dgiao, 'CASH'),

(3, 7, 6, 'HD20261210161500027', 0, 2800000, 2800000,
 N'BÃ¹i Äá»©c Anh', '0928901237', N'15 Äinh TiÃªn HoÃ ng, BÃ¬nh Tháº¡nh, TP.HCM',
 '2026-12-10 16:15:00', @dgiao, 'VNPAY'),

(4, 9, 6, 'HD20261218104000028', 30000, 1670000, 1700000,
 N'Nguyá»…n Quá»‘c Báº£o', '0900123459', N'77 Hai BÃ  TrÆ°ng, Quáº­n 3, TP.HCM',
 '2026-12-18 10:40:00', @dgiao, 'COD'),

(2, 8, 6, 'HD20261225150000029', 0, 4200000, 4200000,
 N'VÅ© Thanh Huyá»n', '0919012348', N'22 LÃ½ Tá»± Trá»ng, Quáº­n 1, TP.HCM',
 '2026-12-25 15:00:00', @dgiao, 'CASH'),

(5, 10, 6, 'HD20261231093000030', 0, 2150000, 2150000,
 N'Phan Minh Äá»©c', '0391234560', N'44 Nguyá»…n ÄÃ¬nh Chiá»ƒu, Quáº­n 3, TP.HCM',
 '2026-12-31 09:30:00', @dgiao, 'CASH');


-- â”€â”€â”€ VERIFY â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
SELECT
  MONTH(ngayTao) AS thang,
  COUNT(*) AS so_don,
  SUM(thanhTien) AS tong_doanh_thu
FROM HoaDon
WHERE YEAR(ngayTao) = 2026
GROUP BY MONTH(ngayTao)
ORDER BY thang;