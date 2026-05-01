SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRAN;

-- Resolve existing color IDs by maMau (ASCII-safe, always reliable).
DECLARE @colorBrown  INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M010' ORDER BY id);
DECLARE @colorRed    INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M004' ORDER BY id);
DECLARE @colorBlue   INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M002' ORDER BY id);
DECLARE @colorBlack  INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M001' ORDER BY id);
DECLARE @colorKem    INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M013' ORDER BY id);
DECLARE @colorWhite  INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M003' ORDER BY id);
DECLARE @colorYellow INT = (SELECT TOP 1 id FROM dbo.MauSac WHERE maMau = N'M005' ORDER BY id);

-- Fallback: try by tenMau with NCHAR-built strings.
IF @colorBrown  IS NULL SET @colorBrown  = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = N'N' + NCHAR(0x00E2) + N'u' ORDER BY id);
IF @colorRed    IS NULL SET @colorRed    = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = NCHAR(0x0110) + NCHAR(0x1ECF) ORDER BY id);
IF @colorBlue   IS NULL SET @colorBlue   = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = N'Xanh d' + NCHAR(0x01B0) + NCHAR(0x01A1) + N'ng' ORDER BY id);
IF @colorBlack  IS NULL SET @colorBlack  = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = NCHAR(0x0110) + N'en' ORDER BY id);
IF @colorKem    IS NULL SET @colorKem    = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = N'Kem' ORDER BY id);
IF @colorWhite  IS NULL SET @colorWhite  = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = N'Tr' + NCHAR(0x1EAF) + N'ng' ORDER BY id);
IF @colorYellow IS NULL SET @colorYellow = (SELECT TOP 1 id FROM dbo.MauSac WHERE tenMau = N'V' + NCHAR(0x00E0) + N'ng' ORDER BY id);

-- Resolve core product ids.
DECLARE @sp001 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP001' ORDER BY id);
DECLARE @sp002 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP002' ORDER BY id);
DECLARE @sp003 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP003' ORDER BY id);
DECLARE @sp004 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP004' ORDER BY id);
DECLARE @sp005 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP005' ORDER BY id);
DECLARE @sp006 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP006' ORDER BY id);
DECLARE @sp007 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP007' ORDER BY id);
DECLARE @sp008 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP008' ORDER BY id);
DECLARE @sp009 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP009' ORDER BY id);
DECLARE @sp010 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP010' ORDER BY id);
DECLARE @sp011 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP011' ORDER BY id);
DECLARE @sp015 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP015' ORDER BY id);
DECLARE @sp017 INT = (SELECT TOP 1 id FROM dbo.SanPham WHERE maSanPham = N'SP017' ORDER BY id);

-- Keep official product names (NCHAR-safe).
IF @sp001 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Bomber da l' + NCHAR(0x1ED9) + N'n DirtyWave' WHERE id = @sp001;
IF @sp002 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Bomber d' + NCHAR(0x00E1) + N'ng l' + NCHAR(0x1EED) + N'ng DirtyWave' WHERE id = @sp002;
IF @sp004 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Bomber cotton nh' + NCHAR(0x1EB9) + N' DirtyWave' WHERE id = @sp004;
IF @sp005 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Hoodie d' + NCHAR(0x00E1) + N'ng h' + NCHAR(0x1ED9) + N'p DirtyWave' WHERE id = @sp005;
IF @sp006 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Hoodie in h' + NCHAR(0x00EC) + N'nh DirtyWave' WHERE id = @sp006;
IF @sp007 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Hoodie k' + NCHAR(0x00E9) + N'o kho' + NCHAR(0x00E1) + N' DirtyWave' WHERE id = @sp007;
IF @sp008 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Coach c' + NCHAR(0x00E1) + N'ch nhi' + NCHAR(0x1EC7) + N't DirtyWave' WHERE id = @sp008;
IF @sp009 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Coach da tr' + NCHAR(0x01A1) + N'n DirtyWave' WHERE id = @sp009;
IF @sp011 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Coach l' + NCHAR(0x00F4) + N'ng c' + NCHAR(0x1EEB) + N'u DirtyWave' WHERE id = @sp011;
IF @sp015 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Coach Leopard DirtyWave' WHERE id = @sp015;
IF @sp017 IS NOT NULL UPDATE dbo.SanPham SET tenSanPham = N'Coach Tiger Stripe DirtyWave' WHERE id = @sp017;

-- Combine SP003 into SP002.
IF @sp002 IS NOT NULL
BEGIN
    UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorRed WHERE idSanPham = @sp002;

    IF @sp003 IS NOT NULL AND @sp003 <> @sp002
    BEGIN
        UPDATE dbo.SanPhamChiTiet SET idSanPham = @sp002, idMauSac = @colorBlue WHERE idSanPham = @sp003;
        UPDATE dbo.AnhMinhHoa SET maAnh = REPLACE(maAnh, N'SP003', N'SP002') WHERE maAnh LIKE N'%SP003%';
        DELETE FROM dbo.SanPham WHERE id = @sp003;
    END

    UPDATE spct SET idMauSac = CASE
        WHEN UPPER(ISNULL(spct.ma, N'')) LIKE N'SPCT003%' THEN @colorBlue
        ELSE @colorRed
    END FROM dbo.SanPhamChiTiet spct WHERE spct.idSanPham = @sp002;
END

-- Combine SP010 into SP009.
IF @sp009 IS NOT NULL
BEGIN
    UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorKem WHERE idSanPham = @sp009;

    IF @sp010 IS NOT NULL AND @sp010 <> @sp009
    BEGIN
        UPDATE dbo.SanPhamChiTiet SET idSanPham = @sp009, idMauSac = @colorBrown WHERE idSanPham = @sp010;
        UPDATE dbo.AnhMinhHoa SET maAnh = REPLACE(maAnh, N'SP010', N'SP009') WHERE maAnh LIKE N'%SP010%';
        DELETE FROM dbo.SanPham WHERE id = @sp010;
    END

    UPDATE spct SET idMauSac = CASE
        WHEN UPPER(ISNULL(spct.ma, N'')) LIKE N'SPCT010%' THEN @colorBrown
        ELSE @colorKem
    END FROM dbo.SanPhamChiTiet spct WHERE spct.idSanPham = @sp009;
END

-- Apply requested single-color products.
IF @sp001 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorBrown  WHERE idSanPham = @sp001;
IF @sp004 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorBlack  WHERE idSanPham = @sp004;
IF @sp005 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorBlack  WHERE idSanPham = @sp005;
IF @sp006 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorBlack  WHERE idSanPham = @sp006;
IF @sp007 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorBlue   WHERE idSanPham = @sp007;
IF @sp008 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorBrown  WHERE idSanPham = @sp008;
IF @sp011 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorWhite  WHERE idSanPham = @sp011;
IF @sp015 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorYellow WHERE idSanPham = @sp015;
IF @sp017 IS NOT NULL UPDATE dbo.SanPhamChiTiet SET idMauSac = @colorWhite  WHERE idSanPham = @sp017;

-- Cleanup garbled duplicate colors.
DELETE FROM dbo.MauSac WHERE maMau LIKE N'MS_%' AND NOT EXISTS (SELECT 1 FROM dbo.SanPhamChiTiet WHERE idMauSac = dbo.MauSac.id);

-- Cleanup old product shells.
DELETE sp FROM dbo.SanPham sp WHERE sp.maSanPham IN (N'SP003', N'SP010') AND NOT EXISTS (SELECT 1 FROM dbo.SanPhamChiTiet c WHERE c.idSanPham = sp.id);

COMMIT TRAN;
