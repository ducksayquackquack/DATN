USE DirtyWave;
GO

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_HoaDon_TrangThai'
      AND parent_object_id = OBJECT_ID('dbo.HoaDon')
)
BEGIN
    ALTER TABLE dbo.HoaDon
    DROP CONSTRAINT CK_HoaDon_TrangThai;

    PRINT 'Dropped CK_HoaDon_TrangThai';
END
GO

ALTER TABLE dbo.HoaDon WITH NOCHECK
ADD CONSTRAINT CK_HoaDon_TrangThai
CHECK (
    trangThai IN (
        N'Chờ xác nhận',
        N'Chờ xử lý',
        N'Đang giao',
        N'Đã giao',
        N'Đã hủy',
        N'Hoàn thành',
        N'Giao thất bại',
        N'Hoàn về',
        N'Cho xac nhan',
        N'Cho xu ly',
        N'Dang giao',
        N'Da giao',
        N'Da huy',
        N'Hoan thanh',
        N'Giao that bai',
        N'Hoan ve',
        N'Ch? xác nh?n',
        N'Ch? x? ly',
        N'Da h?y'
    )
);
GO

PRINT 'Created CK_HoaDon_TrangThai with expanded statuses';
GO
