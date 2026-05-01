/*
  Cleanup test-like customer accounts.
  Rule:
  - local part of email is exactly 3 alphabetic chars (e.g. aaa, xyz, abc), OR
  - local part equals 'test' (case-insensitive)

  Safety:
  - Only CUSTOMER role is targeted.
  - Accounts linked to HoaDon are skipped.
*/

BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('tempdb..#CandidateTaiKhoan') IS NOT NULL DROP TABLE #CandidateTaiKhoan;

    SELECT tk.id,
           tk.email,
           LOWER(
               CASE
                   WHEN CHARINDEX('@', tk.email) > 1 THEN LEFT(tk.email, CHARINDEX('@', tk.email) - 1)
                   ELSE tk.email
               END
           ) AS localPart
    INTO #CandidateTaiKhoan
    FROM dbo.TaiKhoan tk
    WHERE UPPER(ISNULL(tk.vaiTro, '')) = 'CUSTOMER'
      AND (
            (
                LEN(
                    CASE
                        WHEN CHARINDEX('@', tk.email) > 1 THEN LEFT(tk.email, CHARINDEX('@', tk.email) - 1)
                        ELSE tk.email
                    END
                ) = 3
                AND
                CASE
                    WHEN CHARINDEX('@', tk.email) > 1 THEN LEFT(tk.email, CHARINDEX('@', tk.email) - 1)
                    ELSE tk.email
                END NOT LIKE '%[^A-Za-z]%'
            )
            OR LOWER(
                CASE
                    WHEN CHARINDEX('@', tk.email) > 1 THEN LEFT(tk.email, CHARINDEX('@', tk.email) - 1)
                    ELSE tk.email
                END
            ) = 'test'
      );

    -- Skip customers already linked to orders to avoid FK/data loss issues.
    DELETE ctk
    FROM #CandidateTaiKhoan ctk
    WHERE EXISTS (
        SELECT 1
        FROM dbo.KhachHang kh
        JOIN dbo.HoaDon hd ON hd.idKhachHang = kh.id
        WHERE kh.idTaiKhoan = ctk.id
    );

    -- Delete child rows first.
    DELETE ghct
    FROM dbo.GioHangChiTiet ghct
    JOIN dbo.GioHang gh ON gh.id = ghct.idGioHang
    JOIN dbo.KhachHang kh ON kh.id = gh.idKhachHang
    JOIN #CandidateTaiKhoan ctk ON ctk.id = kh.idTaiKhoan;

    DELETE dc
    FROM dbo.DiaChi dc
    JOIN dbo.KhachHang kh ON kh.id = dc.idKhachHang
    JOIN #CandidateTaiKhoan ctk ON ctk.id = kh.idTaiKhoan;

    DELETE gh
    FROM dbo.GioHang gh
    JOIN dbo.KhachHang kh ON kh.id = gh.idKhachHang
    JOIN #CandidateTaiKhoan ctk ON ctk.id = kh.idTaiKhoan;

    DELETE kh
    FROM dbo.KhachHang kh
    JOIN #CandidateTaiKhoan ctk ON ctk.id = kh.idTaiKhoan;

    DELETE tk
    FROM dbo.TaiKhoan tk
    JOIN #CandidateTaiKhoan ctk ON ctk.id = tk.id;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO
