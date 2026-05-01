USE DirtyWave;
GO

IF COL_LENGTH('dbo.HoaDon', 'paidAt') IS NULL
BEGIN
    ALTER TABLE dbo.HoaDon
    ADD paidAt DATETIME2 NULL;

    PRINT 'Added dbo.HoaDon.paidAt';
END
ELSE
BEGIN
    PRINT 'dbo.HoaDon.paidAt already exists';
END
GO

IF COL_LENGTH('dbo.HoaDon', 'cashCollectedAt') IS NULL
BEGIN
    ALTER TABLE dbo.HoaDon
    ADD cashCollectedAt DATETIME2 NULL;

    PRINT 'Added dbo.HoaDon.cashCollectedAt';
END
ELSE
BEGIN
    PRINT 'dbo.HoaDon.cashCollectedAt already exists';
END
GO

-- Backfill: POS cash orders can be considered collected at creation time if no explicit value yet.
UPDATE dbo.HoaDon
SET paidAt = COALESCE(paidAt, ngayTao),
    cashCollectedAt = COALESCE(cashCollectedAt, ngayTao)
WHERE UPPER(ISNULL(phuongThucThanhToan, '')) = 'CASH'
  AND UPPER(ISNULL(statusNote, '')) LIKE '[POS]%';
GO
