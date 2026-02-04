/* ============================================================
   CLEAN CREATE DATABASE
   ============================================================ */


CREATE DATABASE DirtyWave3;
GO
USE DirtyWave3;
GO

SET NOCOUNT ON;
GO

/* ============================================================
   1) DDL - TABLES + CONSTRAINTS (final form, no later ALTER)
   ============================================================ */
BEGIN TRY
    BEGIN TRAN;

    /* ---------- CHUC VU ---------- */
    CREATE TABLE dbo.ChucVu (
        id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChucVu PRIMARY KEY,
        maChucVu      NVARCHAR(50)  NOT NULL,
        tenChucVu     NVARCHAR(255) NOT NULL,
        ngaySua       DATETIME2(0)  NULL,
        trangThai     NVARCHAR(255) NULL,
        CONSTRAINT UQ_ChucVu_maChucVu UNIQUE (maChucVu)
    );

    /* ---------- TAI KHOAN ---------- */
    CREATE TABLE dbo.TaiKhoan (
        id                 INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TaiKhoan PRIMARY KEY,
        vaiTro              NVARCHAR(50)  NOT NULL,
        email               NVARCHAR(100) NOT NULL,
        matKhau             NVARCHAR(255) NOT NULL,
        avatar              NVARCHAR(255) NULL,
        trangThaiHoatDong   NVARCHAR(255) NULL,
        trangThaiTaiKhoan   NVARCHAR(20)  NOT NULL CONSTRAINT DF_TaiKhoan_trangThaiTaiKhoan DEFAULT(N'Kích hoạt'),

        CONSTRAINT UQ_TaiKhoan_email UNIQUE (email),
        CONSTRAINT CK_TaiKhoan_trangThaiTaiKhoan CHECK (trangThaiTaiKhoan IN (N'Kích hoạt', N'Khóa')),
        CONSTRAINT CK_TaiKhoan_vaiTro CHECK (vaiTro IN (N'ADMIN', N'EMPLOYEE', N'CUSTOMER'))
    );

    /* ---------- NHAN VIEN ---------- */
    CREATE TABLE dbo.NhanVien (
        id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_NhanVien PRIMARY KEY,
        idTaiKhoan        INT NOT NULL,
        idChucVu          INT NOT NULL,
        maNhanVien        NVARCHAR(50)  NOT NULL,
        tenNhanVien       NVARCHAR(255) NOT NULL,
        gioiTinh          NVARCHAR(10)  NULL,
        ngaySinh          DATE          NULL,
        diaChi            NVARCHAR(255) NULL,
        soDienThoai       NVARCHAR(20)  NULL,
        trangThaiHoatDong NVARCHAR(255) NULL,
        anh               NVARCHAR(255) NULL,

        CONSTRAINT UQ_NhanVien_maNhanVien UNIQUE (maNhanVien),
        CONSTRAINT UQ_NhanVien_idTaiKhoan UNIQUE (idTaiKhoan), -- 1-1 with TaiKhoan
        CONSTRAINT FK_NhanVien_TaiKhoan FOREIGN KEY (idTaiKhoan) REFERENCES dbo.TaiKhoan(id),
        CONSTRAINT FK_NhanVien_ChucVu   FOREIGN KEY (idChucVu)   REFERENCES dbo.ChucVu(id)
    );

    /* ---------- KHACH HANG ---------- */
    CREATE TABLE dbo.KhachHang (
        id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_KhachHang PRIMARY KEY,
        idTaiKhoan    INT NOT NULL,
        maKhachHang   NVARCHAR(50)  NOT NULL,
        tenKhachHang  NVARCHAR(255) NOT NULL,
        gioiTinh      NVARCHAR(50)  NULL,
        ngaySinh      DATE          NULL,
        soDienThoai   NVARCHAR(20)  NULL,
        trangThai     NVARCHAR(255) NULL,

        CONSTRAINT UQ_KhachHang_maKhachHang UNIQUE (maKhachHang),
        CONSTRAINT UQ_KhachHang_idTaiKhoan UNIQUE (idTaiKhoan), -- 1-1 with TaiKhoan
        CONSTRAINT FK_KhachHang_TaiKhoan FOREIGN KEY (idTaiKhoan) REFERENCES dbo.TaiKhoan(id)
    );

    /* ---------- DIA CHI ---------- */
    CREATE TABLE dbo.DiaChi (
        id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DiaChi PRIMARY KEY,
        idKhachHang   INT NOT NULL,
        diaChiCuThe   NVARCHAR(255) NULL,
        tinhThanh     NVARCHAR(255) NULL,
        quanHuyen     NVARCHAR(255) NULL,
        phuongXa      NVARCHAR(255) NULL,
        trangThai     NVARCHAR(255) NULL,

        CONSTRAINT FK_DiaChi_KhachHang FOREIGN KEY (idKhachHang) REFERENCES dbo.KhachHang(id)
    );

    /* ---------- GIO HANG ---------- */
    CREATE TABLE dbo.GioHang (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GioHang PRIMARY KEY,
        idKhachHang INT NOT NULL,

        CONSTRAINT UQ_GioHang_idKhachHang UNIQUE (idKhachHang), -- 1 cart per customer
        CONSTRAINT FK_GioHang_KhachHang FOREIGN KEY (idKhachHang) REFERENCES dbo.KhachHang(id)
    );

    /* ---------- MASTER DATA ---------- */
    CREATE TABLE dbo.ChatLieu (
        id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChatLieu PRIMARY KEY,
        maChatLieu   NVARCHAR(50)  NOT NULL,
        tenChatLieu  NVARCHAR(255) NOT NULL,
        ngayTao      DATETIME2(0)  NULL,
        ngaySua      DATETIME2(0)  NULL,
        trangThai    NVARCHAR(255) NULL,
        CONSTRAINT UQ_ChatLieu_maChatLieu UNIQUE (maChatLieu)
    );

    CREATE TABLE dbo.MauSac (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MauSac PRIMARY KEY,
        maMau     NVARCHAR(50)  NOT NULL,
        tenMau    NVARCHAR(255) NOT NULL,
        ngayTao   DATETIME2(0)  NULL,
        ngaySua   DATETIME2(0)  NULL,
        trangThai NVARCHAR(255) NULL,
        CONSTRAINT UQ_MauSac_maMau UNIQUE (maMau)
    );

    CREATE TABLE dbo.KichThuoc (
        id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_KichThuoc PRIMARY KEY,
        maKichThuoc  NVARCHAR(50)  NOT NULL,
        tenKichThuoc NVARCHAR(255) NOT NULL,
        ngayTao      DATETIME2(0)  NULL,
        ngaySua      DATETIME2(0)  NULL,
        trangThai    NVARCHAR(255) NULL,
        CONSTRAINT UQ_KichThuoc_maKichThuoc UNIQUE (maKichThuoc)
    );

    CREATE TABLE dbo.Hang (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Hang PRIMARY KEY,
        maHang    NVARCHAR(50)  NOT NULL,
        tenHang   NVARCHAR(255) NOT NULL,
        ngayTao   DATETIME2(0)  NULL,
        ngaySua   DATETIME2(0)  NULL,
        trangThai NVARCHAR(255) NULL,
        CONSTRAINT UQ_Hang_maHang UNIQUE (maHang)
    );

    CREATE TABLE dbo.XuatSu (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_XuatSu PRIMARY KEY,
        maXuatSu  NVARCHAR(50)  NOT NULL,
        tenXuatSu NVARCHAR(255) NOT NULL,
        ngayTao   DATETIME2(0)  NULL,
        ngaySua   DATETIME2(0)  NULL,
        trangThai NVARCHAR(255) NULL,
        CONSTRAINT UQ_XuatSu_maXuatSu UNIQUE (maXuatSu)
    );

    CREATE TABLE dbo.DanhMuc (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DanhMuc PRIMARY KEY,
        maDanhMuc  NVARCHAR(50)  NOT NULL,
        tenDanhMuc NVARCHAR(255) NOT NULL,
        ngayTao    DATETIME2(0)  NULL,
        ngaySua    DATETIME2(0)  NULL,
        trangThai  NVARCHAR(255) NULL,
        CONSTRAINT UQ_DanhMuc_maDanhMuc UNIQUE (maDanhMuc)
    );

    CREATE TABLE dbo.Loai (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Loai PRIMARY KEY,
        maLoai    NVARCHAR(50)  NOT NULL,
        tenLoai   NVARCHAR(255) NOT NULL,
        ngayTao   DATETIME2(0)  NULL,
        ngaySua   DATETIME2(0)  NULL,
        trangThai NVARCHAR(255) NULL,
        CONSTRAINT UQ_Loai_maLoai UNIQUE (maLoai)
    );

    /* ---------- KHUYEN MAI ---------- */
    CREATE TABLE dbo.KhuyenMai (
        id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_KhuyenMai PRIMARY KEY,
        maKhuyenMai  NVARCHAR(50)  NOT NULL,
        tenKhuyenMai NVARCHAR(255) NOT NULL,
        giaTri       DECIMAL(18,2) NULL,
        ngayBatDau   DATE          NULL,
        ngayKetThuc  DATE          NULL,
        trangThai    NVARCHAR(255) NULL,
        CONSTRAINT UQ_KhuyenMai_maKhuyenMai UNIQUE (maKhuyenMai)
    );

    /* ---------- SAN PHAM ---------- */
    CREATE TABLE dbo.SanPham (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SanPham PRIMARY KEY,
        idKhuyenMai INT NULL,
        maSanPham   NVARCHAR(50)  NOT NULL,
        tenSanPham  NVARCHAR(255) NOT NULL,
        moTa        NVARCHAR(MAX) NULL,
        ngayTao     DATETIME2(0)  NULL,
        ngaySua     DATETIME2(0)  NULL,
        nguoiTao    NVARCHAR(255) NULL,
        nguoiSua    NVARCHAR(255) NULL,
        trangThai   NVARCHAR(255) NULL,

        CONSTRAINT UQ_SanPham_maSanPham UNIQUE (maSanPham),
        CONSTRAINT FK_SanPham_KhuyenMai FOREIGN KEY (idKhuyenMai) REFERENCES dbo.KhuyenMai(id)
    );

    /* ---------- SAN PHAM CHI TIET ---------- */
    CREATE TABLE dbo.SanPhamChiTiet (
        id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SanPhamChiTiet PRIMARY KEY,
        ma            NVARCHAR(50)  NOT NULL,
        giaNhap       DECIMAL(18,2) NULL,
        giaBan        DECIMAL(18,2) NULL,
        soLuong       INT           NULL,
        idSanPham     INT NOT NULL,
        idChatLieu    INT NOT NULL,
        idMauSac      INT NOT NULL,
        idKichThuoc   INT NOT NULL,
        idHang        INT NOT NULL,
        idXuatSu      INT NOT NULL,
        idDanhMuc     INT NOT NULL,
        idLoai        INT NOT NULL,
        ngayTao       DATETIME2(0)  NULL,
        ngaySua       DATETIME2(0)  NULL,
        nguoiTao      NVARCHAR(255) NULL,
        nguoiSua      NVARCHAR(255) NULL,
        trangThai     NVARCHAR(255) NULL,
        barcode       NVARCHAR(100) NULL,

        CONSTRAINT UQ_SanPhamChiTiet_ma UNIQUE (ma),
        CONSTRAINT FK_SPCT_SanPham  FOREIGN KEY (idSanPham)   REFERENCES dbo.SanPham(id),
        CONSTRAINT FK_SPCT_ChatLieu FOREIGN KEY (idChatLieu)  REFERENCES dbo.ChatLieu(id),
        CONSTRAINT FK_SPCT_MauSac   FOREIGN KEY (idMauSac)    REFERENCES dbo.MauSac(id),
        CONSTRAINT FK_SPCT_KichThuoc FOREIGN KEY (idKichThuoc) REFERENCES dbo.KichThuoc(id),
        CONSTRAINT FK_SPCT_Hang     FOREIGN KEY (idHang)      REFERENCES dbo.Hang(id),
        CONSTRAINT FK_SPCT_XuatSu   FOREIGN KEY (idXuatSu)    REFERENCES dbo.XuatSu(id),
        CONSTRAINT FK_SPCT_DanhMuc  FOREIGN KEY (idDanhMuc)   REFERENCES dbo.DanhMuc(id),
        CONSTRAINT FK_SPCT_Loai     FOREIGN KEY (idLoai)      REFERENCES dbo.Loai(id)
    );

    /* ---------- ANH MINH HOA (standalone) ---------- */
    CREATE TABLE dbo.AnhMinhHoa (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AnhMinhHoa PRIMARY KEY,
        maAnh      NVARCHAR(50)  NOT NULL,
        anhMinhHoa NVARCHAR(255) NULL,
        ngayThem   DATETIME2(0)  NULL,
        trangThai  NVARCHAR(255) NULL,
        CONSTRAINT UQ_AnhMinhHoa_maAnh UNIQUE (maAnh)
    );

    /* ---------- GIO HANG CHI TIET ---------- */
    CREATE TABLE dbo.GioHangChiTiet (
        id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GioHangChiTiet PRIMARY KEY,
        idGioHang         INT NOT NULL,
        idSanPhamChiTiet  INT NOT NULL,
        soLuong           INT NULL,
        ngayTao           DATETIME2(0) NULL,
        trangThai         NVARCHAR(255) NULL,

        CONSTRAINT FK_GHCT_GioHang FOREIGN KEY (idGioHang) REFERENCES dbo.GioHang(id),
        CONSTRAINT FK_GHCT_SPCT    FOREIGN KEY (idSanPhamChiTiet) REFERENCES dbo.SanPhamChiTiet(id),
        CONSTRAINT UQ_GHCT_uniqueLine UNIQUE (idGioHang, idSanPhamChiTiet) -- prevent duplicate lines
    );

    /* ---------- HOA DON ---------- */
    CREATE TABLE dbo.HoaDon (
        id                    INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_HoaDon PRIMARY KEY,
        idNhanVien             INT NOT NULL,
        idKhachHang            INT NOT NULL,
        maHoaDon               NVARCHAR(50)  NOT NULL,
        phiShip                DECIMAL(18,2) NULL,
        giaSauGiamGia          DECIMAL(18,2) NULL,
        thanhTien              DECIMAL(18,2) NULL,
        tenKhachHang           NVARCHAR(255) NULL,
        soDienThoaiNhanHang    NVARCHAR(20)  NULL,
        diaChiNhanHang         NVARCHAR(255) NULL,
        ngayNhanHangDuKien     DATE          NULL,
        ngayNhanHangMongMuon   DATE          NULL,
        ngayTao                DATETIME2(0)  NULL,
        ngayHuy                DATETIME2(0)  NULL,
        trangThai              NVARCHAR(50)  NULL,

        CONSTRAINT UQ_HoaDon_maHoaDon UNIQUE (maHoaDon),
        CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (idNhanVien) REFERENCES dbo.NhanVien(id),
        CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (idKhachHang) REFERENCES dbo.KhachHang(id),
        CONSTRAINT CK_HoaDon_TrangThai CHECK (
            trangThai IN (N'Chờ xác nhận', N'Chờ xử lý', N'Đang giao', N'Đã giao', N'Đã hủy')
        )
    );

    /* ---------- HOA DON CHI TIET ---------- */
    CREATE TABLE dbo.HoaDonChiTiet (
        id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_HoaDonChiTiet PRIMARY KEY,
        idHoaDon         INT NOT NULL,
        idSanPhamChiTiet INT NOT NULL,
        soLuong          INT NULL,
        thanhTien        DECIMAL(18,2) NULL,
        trangThai        NVARCHAR(255) NULL,

        CONSTRAINT FK_HDCT_HoaDon FOREIGN KEY (idHoaDon) REFERENCES dbo.HoaDon(id),
        CONSTRAINT FK_HDCT_SPCT   FOREIGN KEY (idSanPhamChiTiet) REFERENCES dbo.SanPhamChiTiet(id),
        CONSTRAINT UQ_HDCT_uniqueLine UNIQUE (idHoaDon, idSanPhamChiTiet)
    );

    /* ---------- PHUONG THUC THANH TOAN ---------- */
    CREATE TABLE dbo.PhuongThucThanhToan (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PhuongThucThanhToan PRIMARY KEY,
        ma        NVARCHAR(50)  NOT NULL,
        ten       NVARCHAR(255) NOT NULL,
        moTa      NVARCHAR(255) NULL,
        trangThai NVARCHAR(255) NULL,
        ngayTao   DATETIME2(0)  NULL,
        ngaySua   DATETIME2(0)  NULL,

        CONSTRAINT UQ_PTTT_ma UNIQUE (ma)
    );

    /* ---------- PTTT CHI TIET ---------- */
    CREATE TABLE dbo.PTTTChiTiet (
        id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PTTTChiTiet PRIMARY KEY,
        idHoaDon    INT NOT NULL,
        idPTTT      INT NOT NULL,
        soTien      DECIMAL(18,2) NULL,
        maGiaoDich  NVARCHAR(100) NULL,
        trangThai   NVARCHAR(255) NULL,
        ngayTao     DATETIME2(0)  NULL,
        ngaySua     DATETIME2(0)  NULL,

        CONSTRAINT FK_PTTTCT_HoaDon FOREIGN KEY (idHoaDon) REFERENCES dbo.HoaDon(id),
        CONSTRAINT FK_PTTTCT_PTTT   FOREIGN KEY (idPTTT)  REFERENCES dbo.PhuongThucThanhToan(id)
    );

    /* ---------- LICH SU HOA DON ---------- */
    CREATE TABLE dbo.LichSuHoaDon (
        id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LichSuHoaDon PRIMARY KEY,
        idHoaDon   INT NOT NULL,
        trangThai  NVARCHAR(255) NULL,
        ngayTao    DATETIME2(0)  NULL,
        ngaySua    DATETIME2(0)  NULL,
        taoBoi     NVARCHAR(255) NULL,
        suaBoi     NVARCHAR(255) NULL,

        CONSTRAINT FK_LichSuHoaDon_HoaDon FOREIGN KEY (idHoaDon) REFERENCES dbo.HoaDon(id)
    );

    /* ---------- Indexes (best-practice for FK joins) ---------- */
    CREATE INDEX IX_NhanVien_idTaiKhoan      ON dbo.NhanVien(idTaiKhoan);
    CREATE INDEX IX_NhanVien_idChucVu        ON dbo.NhanVien(idChucVu);

    CREATE INDEX IX_KhachHang_idTaiKhoan     ON dbo.KhachHang(idTaiKhoan);

    CREATE INDEX IX_DiaChi_idKhachHang       ON dbo.DiaChi(idKhachHang);
    CREATE INDEX IX_GioHang_idKhachHang      ON dbo.GioHang(idKhachHang);

    CREATE INDEX IX_SPCT_idSanPham           ON dbo.SanPhamChiTiet(idSanPham);
    CREATE INDEX IX_SPCT_idMauSac            ON dbo.SanPhamChiTiet(idMauSac);
    CREATE INDEX IX_SPCT_idKichThuoc         ON dbo.SanPhamChiTiet(idKichThuoc);

    CREATE INDEX IX_GHCT_idGioHang           ON dbo.GioHangChiTiet(idGioHang);
    CREATE INDEX IX_GHCT_idSanPhamChiTiet    ON dbo.GioHangChiTiet(idSanPhamChiTiet);

    CREATE INDEX IX_HoaDon_idNhanVien        ON dbo.HoaDon(idNhanVien);
    CREATE INDEX IX_HoaDon_idKhachHang       ON dbo.HoaDon(idKhachHang);
    CREATE INDEX IX_HDCT_idHoaDon            ON dbo.HoaDonChiTiet(idHoaDon);

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

/* ============================================================
   2) SEED DATA (gọn, không ALTER/DROP constraint)
   ============================================================ */
BEGIN TRY
    BEGIN TRAN;

    /* 1) CHỨC VỤ */
    INSERT INTO dbo.ChucVu(maChucVu, tenChucVu, ngaySua, trangThai)
    VALUES
    (N'ADMIN',    N'Quản trị hệ thống', SYSDATETIME(), N'Hoạt động'),
    (N'EMPLOYEE', N'Nhân viên bán hàng', SYSDATETIME(), N'Hoạt động');

    DECLARE @ChucVuEmployeeId INT = (SELECT id FROM dbo.ChucVu WHERE maChucVu = N'EMPLOYEE');

    /* 2) TÀI KHOẢN (1 admin + 10 employee + 10 customer) */
    INSERT INTO dbo.TaiKhoan(vaiTro, email, matKhau, avatar, trangThaiHoatDong, trangThaiTaiKhoan)
    VALUES (N'ADMIN', N'admin@dirtywave.com', N'123456', NULL, N'Hoạt động', N'Kích hoạt');

    DECLARE @i INT = 1;
    WHILE @i <= 10
    BEGIN
        INSERT INTO dbo.TaiKhoan(vaiTro, email, matKhau, avatar, trangThaiHoatDong, trangThaiTaiKhoan)
        VALUES (N'EMPLOYEE',
                CONCAT(N'employee', RIGHT('00' + CAST(@i AS VARCHAR(2)), 2), N'@dirtywave.com'),
                N'123456', NULL, N'Hoạt động', N'Kích hoạt');

        INSERT INTO dbo.TaiKhoan(vaiTro, email, matKhau, avatar, trangThaiHoatDong, trangThaiTaiKhoan)
        VALUES (N'CUSTOMER',
                CONCAT(N'customer', RIGHT('00' + CAST(@i AS VARCHAR(2)), 2), N'@dirtywave.com'),
                N'123456', NULL, N'Hoạt động', N'Kích hoạt');

        SET @i += 1;
    END

    /* 3) NHÂN VIÊN */
    ;WITH E AS (
        SELECT id AS idTaiKhoan,
               ROW_NUMBER() OVER (ORDER BY id) AS rn
        FROM dbo.TaiKhoan
        WHERE vaiTro = N'EMPLOYEE'
    )
    INSERT INTO dbo.NhanVien(
        idTaiKhoan, idChucVu, maNhanVien, tenNhanVien,
        gioiTinh, ngaySinh, diaChi, soDienThoai, trangThaiHoatDong, anh
    )
    SELECT
        e.idTaiKhoan,
        @ChucVuEmployeeId,
        CONCAT(N'NV', RIGHT('000' + CAST(e.rn AS VARCHAR(3)), 3)),
        CONCAT(N'Nhân viên ', RIGHT('00' + CAST(e.rn AS VARCHAR(2)), 2)),
        CASE WHEN e.rn % 2 = 0 THEN N'Nữ' ELSE N'Nam' END,
        DATEFROMPARTS(1995 + (e.rn % 5), 1 + (e.rn % 12), 1 + (e.rn % 28)),
        N'Hà Nội',
        CONCAT(N'09000000', RIGHT('00' + CAST(e.rn AS VARCHAR(2)), 2)),
        N'Hoạt động',
        NULL
    FROM E e;

    /* 4) KHÁCH HÀNG */
    ;WITH C AS (
        SELECT id AS idTaiKhoan,
               ROW_NUMBER() OVER (ORDER BY id) AS rn
        FROM dbo.TaiKhoan
        WHERE vaiTro = N'CUSTOMER'
    )
    INSERT INTO dbo.KhachHang(
        idTaiKhoan, maKhachHang, tenKhachHang,
        gioiTinh, ngaySinh, soDienThoai, trangThai
    )
    SELECT
        c.idTaiKhoan,
        CONCAT(N'KH', RIGHT('000' + CAST(c.rn AS VARCHAR(3)), 3)),
        CONCAT(N'Khách hàng ', RIGHT('00' + CAST(c.rn AS VARCHAR(2)), 2)),
        CASE WHEN c.rn % 2 = 0 THEN N'Nam' ELSE N'Nữ' END,
        DATEFROMPARTS(2000 + (c.rn % 5), 1 + (c.rn % 12), 1 + (c.rn % 28)),
        CONCAT(N'09100000', RIGHT('00' + CAST(c.rn AS VARCHAR(2)), 2)),
        N'Hoạt động'
    FROM C c;

    /* 5) ĐỊA CHỈ + GIỎ HÀNG */
    INSERT INTO dbo.DiaChi(idKhachHang, diaChiCuThe, tinhThanh, quanHuyen, phuongXa, trangThai)
    SELECT
        kh.id,
        CONCAT(N'Số ', kh.id, N' Đường ABC'),
        N'Hà Nội',
        N'Cầu Giấy',
        N'Dịch Vọng',
        N'Hoạt động'
    FROM dbo.KhachHang kh;

    INSERT INTO dbo.GioHang(idKhachHang)
    SELECT id FROM dbo.KhachHang;

    /* 6) MASTER DATA */
    INSERT INTO dbo.ChatLieu(maChatLieu, tenChatLieu, ngayTao, ngaySua, trangThai)
    VALUES
    (N'CL001', N'Vải dù',        SYSDATETIME(), SYSDATETIME(), N'Hoạt động'),
    (N'CL002', N'Polyester',     SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.MauSac(maMau, tenMau, ngayTao, ngaySua, trangThai)
    VALUES
    (N'M001', N'Đen',       SYSDATETIME(), SYSDATETIME(), N'Hoạt động'),
    (N'M002', N'Xanh navy', SYSDATETIME(), SYSDATETIME(), N'Hoạt động'),
    (N'M003', N'Trắng',     SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.KichThuoc(maKichThuoc, tenKichThuoc, ngayTao, ngaySua, trangThai)
    VALUES
    (N'S', N'S', SYSDATETIME(), SYSDATETIME(), N'Hoạt động'),
    (N'M', N'M', SYSDATETIME(), SYSDATETIME(), N'Hoạt động'),
    (N'L', N'L', SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.Hang(maHang, tenHang, ngayTao, ngaySua, trangThai)
    VALUES (N'DW', N'DirtyWave', SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.XuatSu(maXuatSu, tenXuatSu, ngayTao, ngaySua, trangThai)
    VALUES (N'VN', N'Việt Nam', SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.DanhMuc(maDanhMuc, tenDanhMuc, ngayTao, ngaySua, trangThai)
    VALUES (N'DM001', N'Áo khoác', SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.Loai(maLoai, tenLoai, ngayTao, ngaySua, trangThai)
    VALUES (N'L001', N'Áo khoác gió', SYSDATETIME(), SYSDATETIME(), N'Hoạt động');

    INSERT INTO dbo.KhuyenMai(maKhuyenMai, tenKhuyenMai, giaTri, ngayBatDau, ngayKetThuc, trangThai)
    VALUES (N'KM001', N'Giảm 20%', 20.00, '2024-01-01', '2026-12-31', N'Hoạt động');

    /* 7) SẢN PHẨM + SPCT */
    DECLARE @KM1  INT = (SELECT id FROM dbo.KhuyenMai WHERE maKhuyenMai = N'KM001');

    INSERT INTO dbo.SanPham(idKhuyenMai, maSanPham, tenSanPham, moTa, ngayTao, nguoiTao, trangThai)
    VALUES
    (@KM1, N'SP001', N'Áo khoác gió DirtyWave',    N'Áo khoác chống gió', SYSDATETIME(), N'Admin', N'Hoạt động'),
    (NULL, N'SP002', N'Áo khoác bomber DirtyWave', N'Bomber basic',       SYSDATETIME(), N'Admin', N'Hoạt động');

    DECLARE @SP1 INT = (SELECT id FROM dbo.SanPham WHERE maSanPham = N'SP001');
    DECLARE @SP2 INT = (SELECT id FROM dbo.SanPham WHERE maSanPham = N'SP002');

    DECLARE @CL1 INT = (SELECT id FROM dbo.ChatLieu WHERE maChatLieu = N'CL001');
    DECLARE @CL2 INT = (SELECT id FROM dbo.ChatLieu WHERE maChatLieu = N'CL002');

    DECLARE @M1  INT = (SELECT id FROM dbo.MauSac WHERE maMau = N'M001');
    DECLARE @M2  INT = (SELECT id FROM dbo.MauSac WHERE maMau = N'M002');
    DECLARE @M3  INT = (SELECT id FROM dbo.MauSac WHERE maMau = N'M003');

    DECLARE @S   INT = (SELECT id FROM dbo.KichThuoc WHERE maKichThuoc = N'S');
    DECLARE @Msz INT = (SELECT id FROM dbo.KichThuoc WHERE maKichThuoc = N'M');
    DECLARE @L   INT = (SELECT id FROM dbo.KichThuoc WHERE maKichThuoc = N'L');

    DECLARE @H1   INT = (SELECT id FROM dbo.Hang    WHERE maHang   = N'DW');
    DECLARE @XS   INT = (SELECT id FROM dbo.XuatSu  WHERE maXuatSu = N'VN');
    DECLARE @DM   INT = (SELECT id FROM dbo.DanhMuc WHERE maDanhMuc = N'DM001');
    DECLARE @LOAI INT = (SELECT id FROM dbo.Loai   WHERE maLoai   = N'L001');

    INSERT INTO dbo.SanPhamChiTiet(
        ma, giaNhap, giaBan, soLuong,
        idSanPham, idChatLieu, idMauSac, idKichThuoc,
        idHang, idXuatSu, idDanhMuc, idLoai,
        ngayTao, trangThai, barcode
    )
    VALUES
    (N'SPCT001', 300000, 499000, 30, @SP1, @CL1, @M1,  @Msz, @H1, @XS, @DM, @LOAI, SYSDATETIME(), N'Hoạt động', N'BC-SPCT001'),
    (N'SPCT002', 300000, 499000, 20, @SP1, @CL1, @M2,  @L,   @H1, @XS, @DM, @LOAI, SYSDATETIME(), N'Hoạt động', N'BC-SPCT002'),
    (N'SPCT003', 350000, 599000, 25, @SP2, @CL2, @M1,  @S,   @H1, @XS, @DM, @LOAI, SYSDATETIME(), N'Hoạt động', N'BC-SPCT003'),
    (N'SPCT004', 350000, 599000, 15, @SP2, @CL2, @M3,  @Msz, @H1, @XS, @DM, @LOAI, SYSDATETIME(), N'Hoạt động', N'BC-SPCT004');

    /* 8) GIỎ HÀNG CHI TIẾT (mẫu) */
    DECLARE @SPCT1 INT = (SELECT id FROM dbo.SanPhamChiTiet WHERE ma = N'SPCT001');
    DECLARE @SPCT2 INT = (SELECT id FROM dbo.SanPhamChiTiet WHERE ma = N'SPCT002');

    ;WITH GH AS (
        SELECT gh.id AS idGioHang, ROW_NUMBER() OVER (ORDER BY gh.id) AS rn
        FROM dbo.GioHang gh
    )
    INSERT INTO dbo.GioHangChiTiet(idGioHang, idSanPhamChiTiet, soLuong, ngayTao, trangThai)
    SELECT
        gh.idGioHang,
        CASE WHEN gh.rn % 2 = 0 THEN @SPCT1 ELSE @SPCT2 END,
        1 + (gh.rn % 3),
        SYSDATETIME(),
        N'Hoạt động'
    FROM GH gh;

    /* 9) HÓA ĐƠN (10 trạng thái đúng CHECK) */
    DECLARE @Status TABLE(rn INT PRIMARY KEY, trangThai NVARCHAR(50), ngayHuy DATETIME2(0) NULL);
    INSERT INTO @Status(rn, trangThai, ngayHuy) VALUES
    (1,  N'Đã giao',      NULL),
    (2,  N'Đang giao',    NULL),
    (3,  N'Chờ xử lý',    NULL),
    (4,  N'Chờ xác nhận', NULL),
    (5,  N'Đã hủy',       DATEADD(DAY, -1, SYSDATETIME())),
    (6,  N'Đang giao',    NULL),
    (7,  N'Chờ xử lý',    NULL),
    (8,  N'Đã giao',      NULL),
    (9,  N'Chờ xác nhận', NULL),
    (10, N'Đã giao',      NULL);

    ;WITH NV AS (
        SELECT id, ROW_NUMBER() OVER (ORDER BY id) rn
        FROM dbo.NhanVien
    ),
    KH AS (
        SELECT id, tenKhachHang, soDienThoai, ROW_NUMBER() OVER (ORDER BY id) rn
        FROM dbo.KhachHang
    ),
    MAP AS (
        SELECT s.rn, nv.id AS idNhanVien, kh.id AS idKhachHang,
               kh.tenKhachHang, kh.soDienThoai,
               s.trangThai, s.ngayHuy
        FROM @Status s
        JOIN NV nv ON nv.rn = s.rn
        JOIN KH kh ON kh.rn = s.rn
    )
    INSERT INTO dbo.HoaDon(
        idNhanVien, idKhachHang, maHoaDon,
        phiShip, giaSauGiamGia, thanhTien,
        tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang,
        ngayNhanHangDuKien, ngayNhanHangMongMuon, ngayTao, ngayHuy, trangThai
    )
    SELECT
        m.idNhanVien,
        m.idKhachHang,
        CONCAT(N'HD2024010', RIGHT('00' + CAST(m.rn AS VARCHAR(2)), 2)) AS maHoaDon,
        CAST(30000 AS DECIMAL(18,2)) AS phiShip,
        CAST(0 AS DECIMAL(18,2)) AS giaSauGiamGia,
        CAST(500000 + (m.rn * 10000) AS DECIMAL(18,2)) AS thanhTien,
        m.tenKhachHang,
        m.soDienThoai,
        ISNULL(dc.diaChiCuThe, N'Hà Nội') AS diaChiNhanHang,
        DATEADD(DAY, m.rn, CAST(SYSDATETIME() AS DATE)) AS ngayNhanHangDuKien,
        DATEADD(DAY, m.rn - 1, CAST(SYSDATETIME() AS DATE)) AS ngayNhanHangMongMuon,
        DATEADD(DAY, -m.rn, SYSDATETIME()) AS ngayTao,
        m.ngayHuy,
        m.trangThai
    FROM MAP m
    OUTER APPLY (
        SELECT TOP 1 diaChiCuThe
        FROM dbo.DiaChi
        WHERE idKhachHang = m.idKhachHang
        ORDER BY id DESC
    ) dc;

    /* 10) LỊCH SỬ HÓA ĐƠN (mẫu) */
    INSERT INTO dbo.LichSuHoaDon(idHoaDon, trangThai, ngayTao, taoBoi)
    SELECT hd.id, CONCAT(N'Tạo hóa đơn - ', hd.trangThai), SYSDATETIME(), N'System'
    FROM dbo.HoaDon hd
    WHERE hd.maHoaDon LIKE N'HD2024010%';

    /* 11) (Tuỳ chọn) Phương thức thanh toán mẫu */
    INSERT INTO dbo.PhuongThucThanhToan(ma, ten, moTa, trangThai, ngayTao, ngaySua)
    VALUES
    (N'CASH',  N'Tiền mặt',      N'Thanh toán khi nhận hàng', N'Hoạt động', SYSDATETIME(), SYSDATETIME()),
    (N'BANK',  N'Chuyển khoản',  N'Chuyển khoản ngân hàng',   N'Hoạt động', SYSDATETIME(), SYSDATETIME()),
    (N'VNPAY', N'VNPAY',         N'Cổng thanh toán VNPAY',     N'Hoạt động', SYSDATETIME(), SYSDATETIME());

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO