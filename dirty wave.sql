CREATE DATABASE DirtyWave;
GO
USE DirtyWave;
GO

CREATE TABLE ChucVu (
    id INT IDENTITY PRIMARY KEY,
    maChucVu NVARCHAR(50),
    tenChucVu NVARCHAR(100),
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE TaiKhoan (
    id INT IDENTITY PRIMARY KEY,
    vaiTro NVARCHAR(20),
    email NVARCHAR(100) UNIQUE,
    matKhau NVARCHAR(255),
    avatar NVARCHAR(255),
    trangThai NVARCHAR(20)
);

CREATE TABLE NhanVien (
    id INT IDENTITY PRIMARY KEY,
    idTaiKhoan INT,
    idChucVu INT,
    maNhanVien NVARCHAR(50),
    tenNhanVien NVARCHAR(100),
    gioiTinh NVARCHAR(10),
    ngaySinh DATE,
    diaChi NVARCHAR(255),
    soDienThoai NVARCHAR(20),
    trangThai NVARCHAR(20),
    anh NVARCHAR(255),
    FOREIGN KEY (idTaiKhoan) REFERENCES TaiKhoan(id),
    FOREIGN KEY (idChucVu) REFERENCES ChucVu(id)
);

CREATE TABLE KhachHang (
    id INT IDENTITY PRIMARY KEY,
    idTaiKhoan INT,
    maKhachHang NVARCHAR(50),
    tenKhachHang NVARCHAR(100),
    gioiTinh NVARCHAR(10),
    ngaySinh DATE,
    soDienThoai NVARCHAR(20),
    trangThai NVARCHAR(20),
    FOREIGN KEY (idTaiKhoan) REFERENCES TaiKhoan(id)
);

CREATE TABLE DiaChi (
    id INT IDENTITY PRIMARY KEY,
    idKhachHang INT,
    diaChiCuThe NVARCHAR(255),
    tinhThanh NVARCHAR(100),
    quanHuyen NVARCHAR(100),
    phuongXa NVARCHAR(100),
    trangThai NVARCHAR(20),
    FOREIGN KEY (idKhachHang) REFERENCES KhachHang(id)
);

CREATE TABLE GioHang (
    id INT IDENTITY PRIMARY KEY,
    idKhachHang INT,
    FOREIGN KEY (idKhachHang) REFERENCES KhachHang(id)
);

CREATE TABLE ChatLieu (
    id INT IDENTITY PRIMARY KEY,
    maChatLieu NVARCHAR(50),
    tenChatLieu NVARCHAR(100),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE MauSac (
    id INT IDENTITY PRIMARY KEY,
    maMau NVARCHAR(50),
    tenMau NVARCHAR(100),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE KichThuoc (
    id INT IDENTITY PRIMARY KEY,
    maKichThuoc NVARCHAR(50),
    tenKichThuoc NVARCHAR(50),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE Hang (
    id INT IDENTITY PRIMARY KEY,
    maHang NVARCHAR(50),
    tenHang NVARCHAR(100),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE XuatSu (
    id INT IDENTITY PRIMARY KEY,
    maXuatSu NVARCHAR(50),
    tenXuatSu NVARCHAR(100),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE DanhMuc (
    id INT IDENTITY PRIMARY KEY,
    maDanhMuc NVARCHAR(50),
    tenDanhMuc NVARCHAR(100),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE Loai (
    id INT IDENTITY PRIMARY KEY,
    maLoai NVARCHAR(50),
    tenLoai NVARCHAR(100),
    ngayTao DATETIME,
    ngaySua DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE SanPham (
    id INT IDENTITY PRIMARY KEY,
    idKhuyenMai INT,
    maSanPham NVARCHAR(50),
    tenSanPham NVARCHAR(255),
    moTa NVARCHAR(MAX),
    ngayTao DATETIME,
    ngaySua DATETIME,
    nguoiTao NVARCHAR(100),
    nguoiSua NVARCHAR(100),
    trangThai NVARCHAR(20)
);

CREATE TABLE SanPhamChiTiet (
    id INT IDENTITY PRIMARY KEY,
    ma NVARCHAR(50),
    giaNhap DECIMAL(10,2),
    giaBan DECIMAL(10,2),
    soLuong INT,
    idSanPham INT,
    idChatLieu INT,
    idMauSac INT,
    idKichThuoc INT,
    idHang INT,
    idXuatSu INT,
    idDanhMuc INT,
    idLoai INT,
    ngayTao DATETIME,
    ngaySua DATETIME,
    nguoiTao NVARCHAR(100),
    nguoiSua NVARCHAR(100),
    trangThai NVARCHAR(20),
    barcode NVARCHAR(100),
    FOREIGN KEY (idSanPham) REFERENCES SanPham(id),
    FOREIGN KEY (idChatLieu) REFERENCES ChatLieu(id),
    FOREIGN KEY (idMauSac) REFERENCES MauSac(id),
    FOREIGN KEY (idKichThuoc) REFERENCES KichThuoc(id),
    FOREIGN KEY (idHang) REFERENCES Hang(id),
    FOREIGN KEY (idXuatSu) REFERENCES XuatSu(id),
    FOREIGN KEY (idDanhMuc) REFERENCES DanhMuc(id),
    FOREIGN KEY (idLoai) REFERENCES Loai(id)
);

CREATE TABLE AnhMinhHoa (
    id INT IDENTITY PRIMARY KEY,
    maAnh NVARCHAR(50),
    anhMinhHoa NVARCHAR(255),
    ngayThem DATETIME,
    trangThai NVARCHAR(20)
);

CREATE TABLE GioHangChiTiet (
    id INT IDENTITY PRIMARY KEY,
    idGioHang INT,
    idSanPhamChiTiet INT,
    soLuong INT,
    ngayTao DATETIME,
    trangThai NVARCHAR(20),
    FOREIGN KEY (idGioHang) REFERENCES GioHang(id),
    FOREIGN KEY (idSanPhamChiTiet) REFERENCES SanPhamChiTiet(id)
);

CREATE TABLE KhuyenMai (
    id INT IDENTITY PRIMARY KEY,
    maKhuyenMai NVARCHAR(50),
    tenKhuyenMai NVARCHAR(255),
    giaTri DECIMAL(10,2),
    ngayBatDau DATE,
    ngayKetThuc DATE,
    trangThai NVARCHAR(20)
);

CREATE TABLE HoaDon (
    id INT IDENTITY PRIMARY KEY,
    idNhanVien INT,
    idKhachHang INT,
    maHoaDon NVARCHAR(50),
    phiShip DECIMAL(10,2),
    giaSauGiamGia DECIMAL(10,2),
    thanhTien DECIMAL(10,2),
    tenKhachHang NVARCHAR(100),
    soDienThoaiNhanHang NVARCHAR(20),
    diaChiNhanHang NVARCHAR(255),
    ngayNhanHangDuKien DATE,
    ngayNhanHangMongMuon DATE,
    ngayTao DATETIME,
    ngayHuy DATETIME,
    trangThai NVARCHAR(20),
    FOREIGN KEY (idNhanVien) REFERENCES NhanVien(id),
    FOREIGN KEY (idKhachHang) REFERENCES KhachHang(id)
);

CREATE TABLE HoaDonChiTiet (
    id INT IDENTITY PRIMARY KEY,
    idHoaDon INT,
    idSanPhamChiTiet INT,
    soLuong INT,
    thanhTien DECIMAL(10,2),
    trangThai NVARCHAR(20),
    FOREIGN KEY (idHoaDon) REFERENCES HoaDon(id),
    FOREIGN KEY (idSanPhamChiTiet) REFERENCES SanPhamChiTiet(id)
);

CREATE TABLE PhuongThucThanhToan (
    id INT IDENTITY PRIMARY KEY,
    ma NVARCHAR(50),
    ten NVARCHAR(100),
    moTa NVARCHAR(255),
    trangThai NVARCHAR(20),
    ngayTao DATETIME,
    ngaySua DATETIME
);

CREATE TABLE PTTTChiTiet (
    id INT IDENTITY PRIMARY KEY,
    idHoaDon INT,
    idPTTT INT,
    soTien DECIMAL(10,2),
    maGiaoDich NVARCHAR(100),
    trangThai NVARCHAR(20),
    ngayTao DATETIME,
    ngaySua DATETIME,
    FOREIGN KEY (idHoaDon) REFERENCES HoaDon(id),
    FOREIGN KEY (idPTTT) REFERENCES PhuongThucThanhToan(id)
);

CREATE TABLE LichSuHoaDon (
    id INT IDENTITY PRIMARY KEY,
    idHoaDon INT,
    trangThai NVARCHAR(50),
    ngayTao DATETIME,
    ngaySua DATETIME,
    taoBoi NVARCHAR(100),
    suaBoi NVARCHAR(100),
    FOREIGN KEY (idHoaDon) REFERENCES HoaDon(id)
);

INSERT INTO ChucVu (maChucVu, tenChucVu, ngaySua, trangThai)
VALUES
('ADMIN', N'Qu?n tr? h? th?ng', GETDATE(), N'Ho?t ??ng'),
('EMPLOYEE', N'Nhân viên bán hàng', GETDATE(), N'Ho?t ??ng');

INSERT INTO TaiKhoan (vaiTro, email, matKhau, avatar, trangThai)
VALUES
('ADMIN', 'admin@dirtywave.com', '123456', NULL, N'Ho?t ??ng'),
('EMPLOYEE', 'employee@dirtywave.com', '123456', NULL, N'Ho?t ??ng'),
('CUSTOMER', 'customer@dirtywave.com', '123456', NULL, N'Ho?t ??ng');

INSERT INTO NhanVien (
    idTaiKhoan, idChucVu, maNhanVien, tenNhanVien,
    gioiTinh, ngaySinh, diaChi, soDienThoai, trangThai, anh
)
VALUES
(2, 2, 'NV001', N'Nguy?n V?n A', N'Nam', '1998-01-01', N'Hà N?i', '0900000001', N'Ho?t ??ng', NULL);

INSERT INTO KhachHang (
    idTaiKhoan, maKhachHang, tenKhachHang,
    gioiTinh, ngaySinh, soDienThoai, trangThai
)
VALUES
(3, 'KH001', N'Tr?n Th? B', N'N?', '2002-05-10', '0900000002', N'Ho?t ??ng');

INSERT INTO DiaChi (idKhachHang, diaChiCuThe, tinhThanh, quanHuyen, phuongXa, trangThai)
VALUES
(1, N'123 ???ng ABC', N'Hà N?i', N'C?u Gi?y', N'D?ch V?ng', N'Ho?t ??ng');

INSERT INTO GioHang (idKhachHang)
VALUES (1);

INSERT INTO ChatLieu (maChatLieu, tenChatLieu, ngayTao, ngaySua, trangThai)
VALUES
('CL01', N'V?i dù', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO MauSac (maMau, tenMau, ngayTao, ngaySua, trangThai)
VALUES
('M01', N'?en', GETDATE(), GETDATE(), N'Ho?t ??ng'),
('M02', N'Xanh', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO KichThuoc (maKichThuoc, tenKichThuoc, ngayTao, ngaySua, trangThai)
VALUES
('S', 'S', GETDATE(), GETDATE(), N'Ho?t ??ng'),
('M', 'M', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO Hang (maHang, tenHang, ngayTao, ngaySua, trangThai)
VALUES
('DW', 'DirtyWave', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO XuatSu (maXuatSu, tenXuatSu, ngayTao, ngaySua, trangThai)
VALUES
('VN', N'Vi?t Nam', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO DanhMuc (maDanhMuc, tenDanhMuc, ngayTao, ngaySua, trangThai)
VALUES
('DM01', N'Áo khoác', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO Loai (maLoai, tenLoai, ngayTao, ngaySua, trangThai)
VALUES
('L01', N'Áo khoác gió', GETDATE(), GETDATE(), N'Ho?t ??ng');

INSERT INTO SanPham (
    idKhuyenMai, maSanPham, tenSanPham, moTa,
    ngayTao, nguoiTao, trangThai
)
VALUES
(NULL, 'SP01', N'Áo khoác gió DirtyWave', N'Áo khoác ch?ng gió', GETDATE(), N'Admin', N'Ho?t ??ng');

INSERT INTO SanPhamChiTiet (
    ma, giaNhap, giaBan, soLuong,
    idSanPham, idChatLieu, idMauSac, idKichThuoc,
    idHang, idXuatSu, idDanhMuc, idLoai,
    ngayTao, trangThai, barcode
)
VALUES
('SPCT01', 300000, 499000, 30, 1, 1, 1, 1, 1, 1, 1, 1, GETDATE(), N'Ho?t ??ng', 'DW-SPCT01'),
('SPCT02', 300000, 499000, 20, 1, 1, 2, 2, 1, 1, 1, 1, GETDATE(), N'Ho?t ??ng', 'DW-SPCT02');

INSERT INTO GioHangChiTiet (idGioHang, idSanPhamChiTiet, soLuong, ngayTao, trangThai)
VALUES
(1, 1, 1, GETDATE(), N'Ho?t ??ng'),
(1, 2, 2, GETDATE(), N'Ho?t ??ng');

INSERT INTO HoaDon (
    idNhanVien, idKhachHang, maHoaDon,
    phiShip, giaSauGiamGia, thanhTien,
    tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang,
    ngayNhanHangDuKien, ngayTao, trangThai
)
VALUES
(1, 1, 'HD001', 30000, 0, 1527000,
 N'Tr?n Th? B', '0900000002', N'123 ???ng ABC',
 '2026-02-01', GETDATE(), N'Ch? x? lý');

INSERT INTO HoaDonChiTiet (idHoaDon, idSanPhamChiTiet, soLuong, thanhTien, trangThai)
VALUES
(1, 1, 1, 499000, N'Ho?t ??ng'),
(1, 2, 2, 998000, N'Ho?t ??ng');

INSERT INTO PhuongThucThanhToan (ma, ten, moTa, trangThai, ngayTao)
VALUES
('COD', N'Thanh toán khi nh?n hàng', N'Ti?n m?t', N'Ho?t ??ng', GETDATE());

INSERT INTO PTTTChiTiet (
    idHoaDon, idPTTT, soTien, maGiaoDich, trangThai, ngayTao
)
VALUES
(1, 1, 1527000, 'GD001', N'Hoàn t?t', GETDATE());

INSERT INTO LichSuHoaDon (
    idHoaDon, trangThai, ngayTao, taoBoi
)
VALUES
(1, N'T?o hóa ??n', GETDATE(), N'Nguy?n V?n A');

-- Xóa dữ liệu cũ (nếu cần)
DELETE FROM HoaDonChiTiet;
DELETE FROM HoaDon;
DELETE FROM GioHangChiTiet;
DELETE FROM GioHang;
DELETE FROM SanPhamChiTiet;
DELETE FROM SanPham;
DELETE FROM Loai;
DELETE FROM DanhMuc;
DELETE FROM XuatSu;
DELETE FROM Hang;
DELETE FROM KichThuoc;
DELETE FROM MauSac;
DELETE FROM ChatLieu;
DELETE FROM DiaChi;
DELETE FROM KhachHang;
DELETE FROM NhanVien;
DELETE FROM TaiKhoan;
DELETE FROM ChucVu;

-- Cập nhật lại bảng DanhMuc - Chỉ có danh mục áo khoác
INSERT INTO DanhMuc (maDanhMuc, tenDanhMuc, ngayTao, ngaySua, trangThai) VALUES
('AK001', N'Áo khoác gió', GETDATE(), GETDATE(), N'Hoạt động'),
('AK002', N'Áo khoác da', GETDATE(), GETDATE(), N'Hoạt động'),
('AK003', N'Áo khoác len', GETDATE(), GETDATE(), N'Hoạt động'),
('AK004', N'Áo khoác dù', GETDATE(), GETDATE(), N'Hoạt động'),
('AK005', N'Áo khoác bomber', GETDATE(), GETDATE(), N'Hoạt động'),
('AK006', N'Áo khoác hoodie', GETDATE(), GETDATE(), N'Hoạt động'),
('AK007', N'Áo khoác blazer', GETDATE(), GETDATE(), N'Hoạt động'),
('AK008', N'Áo khoác vest', GETDATE(), GETDATE(), N'Hoạt động'),
('AK009', N'Áo khoác parka', GETDATE(), GETDATE(), N'Hoạt động'),
('AK010', N'Áo khoác lông vũ', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng Loai - Các loại áo khoác chi tiết
INSERT INTO Loai (maLoai, tenLoai, ngayTao, ngaySua, trangThai) VALUES
('L001', N'Áo khoác gió chống nước', GETDATE(), GETDATE(), N'Hoạt động'),
('L002', N'Áo khoác gió thể thao', GETDATE(), GETDATE(), N'Hoạt động'),
('L003', N'Áo khoác da bò', GETDATE(), GETDATE(), N'Hoạt động'),
('L004', N'Áo khoác da cừu', GETDATE(), GETDATE(), N'Hoạt động'),
('L005', N'Áo khoác len cardigan', GETDATE(), GETDATE(), N'Hoạt động'),
('L006', N'Áo khoác len dáng dài', GETDATE(), GETDATE(), N'Hoạt động'),
('L007', N'Áo khoác bomber thêu', GETDATE(), GETDATE(), N'Hoạt động'),
('L008', N'Áo khoác bomber in hình', GETDATE(), GETDATE(), N'Hoạt động'),
('L009', N'Áo khoác hoodie có mũ', GETDATE(), GETDATE(), N'Hoạt động'),
('L010', N'Áo khoác hoodie không mũ', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng ChatLieu với chất liệu áo khoác
INSERT INTO ChatLieu (maChatLieu, tenChatLieu, ngayTao, ngaySua, trangThai) VALUES
('CL001', N'Vải dù chống nước', GETDATE(), GETDATE(), N'Hoạt động'),
('CL002', N'Polyester', GETDATE(), GETDATE(), N'Hoạt động'),
('CL003', N'Da bò thật', GETDATE(), GETDATE(), N'Hoạt động'),
('CL004', N'Da PU', GETDATE(), GETDATE(), N'Hoạt động'),
('CL005', N'Len merino', GETDATE(), GETDATE(), N'Hoạt động'),
('CL006', N'Len cashmere', GETDATE(), GETDATE(), N'Hoạt động'),
('CL007', N'Nỉ fleece', GETDATE(), GETDATE(), N'Hoạt động'),
('CL008', N'Nỉ bông', GETDATE(), GETDATE(), N'Hoạt động'),
('CL009', N'Lông vũ', GETDATE(), GETDATE(), N'Hoạt động'),
('CL010', N'Vải kaki', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng MauSac
INSERT INTO MauSac (maMau, tenMau, ngayTao, ngaySua, trangThai) VALUES
('M001', N'Đen', GETDATE(), GETDATE(), N'Hoạt động'),
('M002', N'Nâu', GETDATE(), GETDATE(), N'Hoạt động'),
('M003', N'Xám', GETDATE(), GETDATE(), N'Hoạt động'),
('M004', N'Xanh navy', GETDATE(), GETDATE(), N'Hoạt động'),
('M005', N'Xanh rêu', GETDATE(), GETDATE(), N'Hoạt động'),
('M006', N'Be', GETDATE(), GETDATE(), N'Hoạt động'),
('M007', N'Trắng', GETDATE(), GETDATE(), N'Hoạt động'),
('M008', N'Đỏ', GETDATE(), GETDATE(), N'Hoạt động'),
('M009', N'Xanh ngọc', GETDATE(), GETDATE(), N'Hoạt động'),
('M010', N'Cam', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng KichThuoc (chỉ size áo khoác)
INSERT INTO KichThuoc (maKichThuoc, tenKichThuoc, ngayTao, ngaySua, trangThai) VALUES
('S', 'S (44-46)', GETDATE(), GETDATE(), N'Hoạt động'),
('M', 'M (48-50)', GETDATE(), GETDATE(), N'Hoạt động'),
('L', 'L (52-54)', GETDATE(), GETDATE(), N'Hoạt động'),
('XL', 'XL (56-58)', GETDATE(), GETDATE(), N'Hoạt động'),
('XXL', 'XXL (60-62)', GETDATE(), GETDATE(), N'Hoạt động'),
('XS', 'XS (40-42)', GETDATE(), GETDATE(), N'Hoạt động'),
('3XL', '3XL (64-66)', GETDATE(), GETDATE(), N'Hoạt động'),
('4XL', '4XL (68-70)', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng Hang (chỉ thương hiệu áo khoác)
INSERT INTO Hang (maHang, tenHang, ngayTao, ngaySua, trangThai) VALUES
('DirtyWave', 'DirtyWave', GETDATE(), GETDATE(), N'Hoạt động'),
('TheNorthFace', 'The North Face', GETDATE(), GETDATE(), N'Hoạt động'),
('Columbia', 'Columbia', GETDATE(), GETDATE(), N'Hoạt động'),
('Patagonia', 'Patagonia', GETDATE(), GETDATE(), N'Hoạt động'),
('Superdry', 'Superdry', GETDATE(), GETDATE(), N'Hoạt động'),
('Alpha', 'Alpha Industries', GETDATE(), GETDATE(), N'Hoạt động'),
('Levis', 'Levi''s', GETDATE(), GETDATE(), N'Hoạt động'),
('Zara', 'Zara', GETDATE(), GETDATE(), N'Hoạt động'),
('Uniqlo', 'Uniqlo', GETDATE(), GETDATE(), N'Hoạt động'),
('H&M', 'H&M', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng XuatSu
INSERT INTO XuatSu (maXuatSu, tenXuatSu, ngayTao, ngaySua, trangThai) VALUES
('VN', N'Việt Nam', GETDATE(), GETDATE(), N'Hoạt động'),
('CN', N'Trung Quốc', GETDATE(), GETDATE(), N'Hoạt động'),
('US', N'Mỹ', GETDATE(), GETDATE(), N'Hoạt động'),
('JP', N'Nhật Bản', GETDATE(), GETDATE(), N'Hoạt động'),
('KR', N'Hàn Quốc', GETDATE(), GETDATE(), N'Hoạt động'),
('DE', N'Đức', GETDATE(), GETDATE(), N'Hoạt động'),
('UK', N'Anh', GETDATE(), GETDATE(), N'Hoạt động'),
('IT', N'Ý', GETDATE(), GETDATE(), N'Hoạt động'),
('FR', N'Pháp', GETDATE(), GETDATE(), N'Hoạt động'),
('CA', N'Canada', GETDATE(), GETDATE(), N'Hoạt động');

-- Cập nhật bảng KhuyenMai
INSERT INTO KhuyenMai (maKhuyenMai, tenKhuyenMai, giaTri, ngayBatDau, ngayKetThuc, trangThai) VALUES
('KM001', N'Giảm 20% cho áo khoác gió', 20.00, '2024-01-01', '2024-12-31', N'Hoạt động'),
('KM002', N'Giảm 15% cho áo khoác da', 15.00, '2024-02-01', '2024-02-28', N'Hoạt động'),
('KM003', N'Mua 2 áo khoác giảm 30%', 30.00, '2024-03-01', '2024-03-31', N'Hoạt động'),
('KM004', N'Giảm 25% cho áo khoác len', 25.00, '2024-04-01', '2024-04-30', N'Hoạt động'),
('KM005', N'Giảm 40% cho áo khoác bomber', 40.00, '2024-05-01', '2024-05-31', N'Hoạt động'),
('KM006', N'Giảm 35% cho áo khoác hoodie', 35.00, '2024-06-01', '2024-06-30', N'Hoạt động'),
('KM007', N'Free ship cho hóa đơn trên 1 triệu', 0.00, '2024-07-01', '2024-07-31', N'Hoạt động'),
('KM008', N'Giảm 50% cho áo khoác cuối mùa', 50.00, '2024-08-01', '2024-08-31', N'Hoạt động'),
('KM009', N'Giảm 10% cho khách hàng thân thiết', 10.00, '2024-09-01', '2024-09-30', N'Hoạt động'),
('KM010', N'Giảm 45% cho áo khoác parka', 45.00, '2024-10-01', '2024-10-31', N'Hoạt động');

-- Cập nhật bảng SanPham - Chỉ có áo khoác
INSERT INTO SanPham (idKhuyenMai, maSanPham, tenSanPham, moTa, ngayTao, ngaySua, nguoiTao, nguoiSua, trangThai) VALUES
(1, 'AK001', N'Áo khoác gió chống nước DirtyWave Pro', N'Áo khoác gió cao cấp chống nước, chống gió, phù hợp đi du lịch và thể thao', GETDATE(), GETDATE(), N'Nguyễn Văn Tuấn', N'Nguyễn Văn Tuấn', N'Hoạt động'),
(2, 'AK002', N'Áo khoác da bò thật Vintage', N'Áo khoác da bò thật 100%, phong cách vintage, bền đẹp theo thời gian', GETDATE(), GETDATE(), N'Phạm Thị Mai', N'Phạm Thị Mai', N'Hoạt động'),
(3, 'AK003', N'Áo khoác len cardigan cao cấp', N'Áo khoác len mỏng dễ phối đồ, chất liệu len merino mềm mại', GETDATE(), GETDATE(), N'Trần Thị Linh', N'Trần Thị Linh', N'Hoạt động'),
(4, 'AK004', N'Áo khoác bomber thêu hoa văn', N'Áo khoác bomber thêu tay hoa văn độc đáo, form rộng thoải mái', GETDATE(), GETDATE(), N'Lê Văn Hùng', N'Lê Văn Hùng', N'Hoạt động'),
(5, 'AK005', N'Áo khoác hoodie có mũ trùm', N'Áo khoác hoodie nỉ dày dặn, có mũ trùm và túi kangaroo', GETDATE(), GETDATE(), N'Võ Thị An', N'Võ Thị An', N'Hoạt động'),
(6, 'AK006', N'Áo khoác blazer văn phòng', N'Áo khoác blazer lịch sự, phù hợp đi làm và các buổi tiệc', GETDATE(), GETDATE(), N'Đỗ Văn Đạt', N'Đỗ Văn Đạt', N'Hoạt động'),
(7, 'AK007', N'Áo khoác vest nam cao cấp', N'Áo khoác vest may đo chuẩn form, chất liệu vải cao cấp không nhăn', GETDATE(), GETDATE(), N'Hoàng Thị Lan', N'Hoàng Thị Lan', N'Hoạt động'),
(8, 'AK008', N'Áo khoác parka chống rét', N'Áo khoác parka dày dặn, có lớp lông vũ giữ nhiệt, chống rét hiệu quả', GETDATE(), GETDATE(), N'Trần Minh Anh', N'Trần Minh Anh', N'Hoạt động'),
(9, 'AK009', N'Áo khoác dù thể thao', N'Áo khoác dù nhẹ, gọn, dễ mang theo, phù hợp hoạt động ngoài trời', GETDATE(), GETDATE(), N'Nguyễn Thị Thảo', N'Nguyễn Thị Thảo', N'Hoạt động'),
(10, 'AK010', N'Áo khoác lông vũ siêu nhẹ', N'Áo khoác lông vũ nhẹ nhưng ấm, có thể gấp gọn thành túi nhỏ', GETDATE(), GETDATE(), N'Nguyễn Văn Tuấn', N'Nguyễn Văn Tuấn', N'Hoạt động');

-- Cập nhật bảng SanPhamChiTiet - Các phiên bản của áo khoác
INSERT INTO SanPhamChiTiet (ma, giaNhap, giaBan, soLuong, idSanPham, idChatLieu, idMauSac, idKichThuoc, idHang, idXuatSu, idDanhMuc, idLoai, ngayTao, ngaySua, nguoiTao, nguoiSua, trangThai, barcode) VALUES
('AKCT001', 350000, 699000, 30, 1, 1, 1, 2, 1, 1, 1, 1, GETDATE(), GETDATE(), N'Nguyễn Văn Tuấn', N'Nguyễn Văn Tuấn', N'Hoạt động', 'DWAK001M'),
('AKCT002', 350000, 699000, 25, 1, 1, 4, 3, 1, 1, 1, 1, GETDATE(), GETDATE(), N'Nguyễn Văn Tuấn', N'Nguyễn Văn Tuấn', N'Hoạt động', 'DWAK001L'),
('AKCT003', 850000, 1599000, 15, 2, 3, 2, 2, 1, 2, 2, 3, GETDATE(), GETDATE(), N'Phạm Thị Mai', N'Phạm Thị Mai', N'Hoạt động', 'DWAK002M'),
('AKCT004', 850000, 1599000, 10, 2, 3, 1, 3, 1, 2, 2, 3, GETDATE(), GETDATE(), N'Phạm Thị Mai', N'Phạm Thị Mai', N'Hoạt động', 'DWAK002L'),
('AKCT005', 450000, 899000, 40, 3, 5, 3, 2, 1, 3, 3, 5, GETDATE(), GETDATE(), N'Trần Thị Linh', N'Trần Thị Linh', N'Hoạt động', 'DWAK003M'),
('AKCT006', 450000, 899000, 35, 3, 5, 7, 3, 1, 3, 3, 5, GETDATE(), GETDATE(), N'Trần Thị Linh', N'Trần Thị Linh', N'Hoạt động', 'DWAK003L'),
('AKCT007', 300000, 599000, 50, 4, 2, 8, 1, 1, 4, 5, 7, GETDATE(), GETDATE(), N'Lê Văn Hùng', N'Lê Văn Hùng', N'Hoạt động', 'DWAK004S'),
('AKCT008', 300000, 599000, 45, 4, 2, 1, 2, 1, 4, 5, 7, GETDATE(), GETDATE(), N'Lê Văn Hùng', N'Lê Văn Hùng', N'Hoạt động', 'DWAK004M'),
('AKCT009', 250000, 499000, 60, 5, 7, 1, 3, 1, 5, 6, 9, GETDATE(), GETDATE(), N'Võ Thị An', N'Võ Thị An', N'Hoạt động', 'DWAK005L'),
('AKCT010', 250000, 499000, 55, 5, 7, 4, 4, 1, 5, 6, 9, GETDATE(), GETDATE(), N'Võ Thị An', N'Võ Thị An', N'Hoạt động', 'DWAK005XL');

-- Chèn dữ liệu mẫu cho AnhMinhHoa (ảnh áo khoác)
INSERT INTO AnhMinhHoa (maAnh, anhMinhHoa, ngayThem, trangThai) VALUES
('AKIMG001', 'aokhoac_gio_den.jpg', GETDATE(), N'Hoạt động'),
('AKIMG002', 'aokhoac_gio_xanh.jpg', GETDATE(), N'Hoạt động'),
('AKIMG003', 'aokhoac_da_brown.jpg', GETDATE(), N'Hoạt động'),
('AKIMG004', 'aokhoac_da_black.jpg', GETDATE(), N'Hoạt động'),
('AKIMG005', 'aokhoac_len_gray.jpg', GETDATE(), N'Hoạt động'),
('AKIMG006', 'aokhoac_len_white.jpg', GETDATE(), N'Hoạt động'),
('AKIMG007', 'aokhoac_bomber_red.jpg', GETDATE(), N'Hoạt động'),
('AKIMG008', 'aokhoac_bomber_black.jpg', GETDATE(), N'Hoạt động'),
('AKIMG009', 'aokhoac_hoodie_black.jpg', GETDATE(), N'Hoạt động'),
('AKIMG010', 'aokhoac_hoodie_navy.jpg', GETDATE(), N'Hoạt động');

-- Chèn dữ liệu mẫu cho các bảng còn lại (giữ nguyên như cũ nhưng có thể cập nhật tên sản phẩm)

-- Tạo 10 hóa đơn mẫu với sản phẩm áo khoác
INSERT INTO HoaDon (idNhanVien, idKhachHang, maHoaDon, phiShip, giaSauGiamGia, thanhTien, tenKhachHang, soDienThoaiNhanHang, diaChiNhanHang, ngayNhanHangDuKien, ngayNhanHangMongMuon, ngayTao, ngayHuy, trangThai) VALUES
(1, 1, 'HD202401001', 30000, 139800, 1258200, N'Trần Minh Đức', '0912345678', N'123 Nguyễn Văn Cừ, Q.1, TP.HCM', '2024-01-10', '2024-01-09', '2024-01-05', NULL, N'Đã giao'),
(2, 2, 'HD202401002', 0, 0, 1599000, N'Hoàng Thị Lan', '0913456789', N'45 Lý Thường Kiệt, Q.10, TP.HCM', '2024-01-11', '2024-01-10', '2024-01-06', NULL, N'Đang giao'),
(3, 3, 'HD202401003', 25000, 179800, 1769200, N'Lê Văn Hùng', '0914567890', N'78 Trần Phú, Ba Đình, Hà Nội', '2024-01-12', '2024-01-11', '2024-01-07', NULL, N'Chờ xử lý'),
(4, 4, 'HD202401004', 30000, 0, 1898000, N'Võ Thị An', '0915678901', N'56 Lê Duẩn, Hải Châu, Đà Nẵng', '2024-01-13', '2024-01-12', '2024-01-08', NULL, N'Đã giao'),
(5, 5, 'HD202401005', 0, 99800, 399200, N'Nguyễn Thị Thảo', '0916789012', N'34 Nguyễn Văn Linh, Hồng Bàng, Hải Phòng', '2024-01-14', '2024-01-13', '2024-01-09', '2024-01-10', N'Đã hủy'),
(6, 6, 'HD202401006', 30000, 0, 1018000, N'Đỗ Văn Đạt', '0917890123', N'89 Hùng Vương, Ninh Kiều, Cần Thơ', '2024-01-15', '2024-01-14', '2024-01-10', NULL, N'Đang giao'),
(7, 7, 'HD202401007', 25000, 119800, 1438200, N'Phạm Văn Nam', '0918901234', N'12 Trần Hưng Đạo, Q.5, TP.HCM', '2024-01-16', '2024-01-15', '2024-01-11', NULL, N'Chờ xử lý'),
(8, 8, 'HD202401008', 0, 0, 1298000, N'Trần Thị Hương', '0919012345', N'23 Phan Đình Phùng, Hoàn Kiếm, Hà Nội', '2024-01-17', '2024-01-16', '2024-01-12', NULL, N'Đã giao'),
(9, 9, 'HD202401009', 30000, 89900, 1408900, N'Lê Thị Mai', '0910123456', N'67 Nguyễn Tri Phương, Thanh Khê, Đà Nẵng', '2024-01-18', '2024-01-17', '2024-01-13', NULL, N'Đang giao'),
(10, 10, 'HD202401010', 0, 0, 699000, N'Nguyễn Văn Khải', '0911234567', N'90 Lý Tự Trọng, Q.1, TP.HCM', '2024-01-19', '2024-01-18', '2024-01-14', NULL, N'Đã giao');

-- Chi tiết hóa đơn với áo khoác
INSERT INTO HoaDonChiTiet (idHoaDon, idSanPhamChiTiet, soLuong, thanhTien, trangThai) VALUES
(1, 1, 2, 1398000, N'Hoạt động'),
(2, 3, 1, 1599000, N'Hoạt động'),
(3, 5, 2, 1798000, N'Hoạt động'),
(4, 2, 1, 699000, N'Hoạt động'),
(4, 4, 1, 1599000, N'Hoạt động'),
(5, 7, 2, 1198000, N'Hoạt động'),
(6, 8, 2, 1198000, N'Hoạt động'),
(7, 9, 3, 1497000, N'Hoạt động'),
(8, 10, 2, 998000, N'Hoạt động'),
(9, 6, 1, 899000, N'Hoạt động'),
(9, 1, 1, 699000, N'Hoạt động'),
(10, 2, 1, 699000, N'Hoạt động');