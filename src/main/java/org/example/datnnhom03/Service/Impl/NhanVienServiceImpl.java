package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Model.ChucVu;
import org.example.datnnhom03.Repository.ChucVuRepository;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.example.datnnhom03.Repository.TaiKhoanRepository;
import org.example.datnnhom03.Service.NhanVienService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class NhanVienServiceImpl implements NhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final ChucVuRepository chucVuRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public NhanVienServiceImpl(NhanVienRepository nhanVienRepository,
                               TaiKhoanRepository taiKhoanRepository,
                               ChucVuRepository chucVuRepository) {
        this.nhanVienRepository = nhanVienRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.chucVuRepository = chucVuRepository;
    }

    @Override
    public NhanVien create(NhanVien nhanVien) {
        Integer taiKhoanId = nhanVien.getTaiKhoan() != null ? nhanVien.getTaiKhoan().getId() : null;
        TaiKhoan taiKhoan;
        if (taiKhoanId != null) {
            taiKhoan = taiKhoanRepository.findById(taiKhoanId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        } else {
            taiKhoan = resolveOrCreateTaiKhoan(nhanVien);
        }

        ChucVu chucVu = resolveChucVu(nhanVien, taiKhoan.getVaiTro());

        nhanVien.setTaiKhoan(taiKhoan);
        nhanVien.setChucVu(chucVu);

        if (nhanVien.getTrangThaiHoatDong() == null || nhanVien.getTrangThaiHoatDong().isBlank()) {
            nhanVien.setTrangThaiHoatDong("Hoạt động");
        }

        if (nhanVien.getMaNhanVien() == null || nhanVien.getMaNhanVien().isBlank()) {
            nhanVien.setMaNhanVien("TMP-" + System.nanoTime());
        }

        NhanVien saved = nhanVienRepository.save(nhanVien);
        if (saved.getMaNhanVien() == null || saved.getMaNhanVien().startsWith("TMP-")) {
            saved.setMaNhanVien(String.format("NV%03d", saved.getId()));
            saved = nhanVienRepository.save(saved);
        }
        return saved;
    }

    @Override
    public NhanVien update(Integer id, NhanVien nhanVien) {
        NhanVien nv = nhanVienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        nv.setMaNhanVien(nhanVien.getMaNhanVien());
        nv.setTenNhanVien(nhanVien.getTenNhanVien());
        nv.setGioiTinh(nhanVien.getGioiTinh());
        nv.setNgaySinh(nhanVien.getNgaySinh());
        nv.setDiaChi(nhanVien.getDiaChi());
        nv.setSoDienThoai(nhanVien.getSoDienThoai());
        nv.setTrangThaiHoatDong(nhanVien.getTrangThaiHoatDong());
        nv.setAnh(nhanVien.getAnh());

        if (nhanVien.getTaiKhoan() != null && nhanVien.getTaiKhoan().getId() != null) {
            TaiKhoan taiKhoan = taiKhoanRepository.findById(nhanVien.getTaiKhoan().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
            nv.setTaiKhoan(taiKhoan);
        } else if (nhanVien.getEmail() != null && !nhanVien.getEmail().isBlank()) {
            TaiKhoan taiKhoan = resolveOrCreateTaiKhoan(nhanVien);
            nv.setTaiKhoan(taiKhoan);
        }

        if (nhanVien.getChucVu() != null && nhanVien.getChucVu().getId() != null) {
            ChucVu chucVu = chucVuRepository.findById(nhanVien.getChucVu().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ"));
            nv.setChucVu(chucVu);
        } else if (nhanVien.getChucVu() != null) {
            String vaiTro = nv.getTaiKhoan() != null ? nv.getTaiKhoan().getVaiTro() : null;
            nv.setChucVu(resolveChucVu(nhanVien, vaiTro));
        }

        return nhanVienRepository.save(nv);
    }

    private TaiKhoan resolveOrCreateTaiKhoan(NhanVien nhanVien) {
        String email = nhanVien.getEmail() == null ? null : nhanVien.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Thiếu id tài khoản hoặc email để tạo tài khoản nhân viên");
        }

        TaiKhoan existing = taiKhoanRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            return existing;
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setEmail(email);
        taiKhoan.setVaiTro(resolveAccountRole(nhanVien));
        taiKhoan.setTrangThaiHoatDong("Hoạt động");
        taiKhoan.setTrangThaiTaiKhoan("Kích hoạt");

        String rawPassword = (nhanVien.getMatKhau() == null || nhanVien.getMatKhau().isBlank())
                ? "123456"
                : nhanVien.getMatKhau().trim();
        taiKhoan.setMatKhau(passwordEncoder.encode(rawPassword));

        return taiKhoanRepository.save(taiKhoan);
    }

    private String resolveAccountRole(NhanVien nhanVien) {
        if (nhanVien.getVaiTro() != null && !nhanVien.getVaiTro().isBlank()) {
            String normalized = nhanVien.getVaiTro().trim().toUpperCase(Locale.ROOT);
            if (normalized.contains("ADMIN")) return "ADMIN";
            if (normalized.contains("EMPLOYEE") || normalized.contains("STAFF") || normalized.contains("NHAN VIEN")) {
                return "EMPLOYEE";
            }
        }

        if (nhanVien.getChucVu() != null) {
            String ma = nhanVien.getChucVu().getMaChucVu();
            String ten = nhanVien.getChucVu().getTenChucVu();
            String probe = (ma != null ? ma : "") + " " + (ten != null ? ten : "");
            String normalized = probe.toUpperCase(Locale.ROOT);
            if (normalized.contains("ADMIN") || normalized.contains("QUAN TRI")) return "ADMIN";
        }

        return "EMPLOYEE";
    }

    private ChucVu resolveChucVu(NhanVien nhanVien, String vaiTroFallback) {
        if (nhanVien.getChucVu() != null) {
            if (nhanVien.getChucVu().getId() != null) {
                return chucVuRepository.findById(nhanVien.getChucVu().getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ"));
            }

            String maChucVu = nhanVien.getChucVu().getMaChucVu();
            if (maChucVu != null && !maChucVu.isBlank()) {
                return chucVuRepository.findByMaChucVuIgnoreCase(maChucVu)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ theo mã: " + maChucVu));
            }

            String tenChucVu = nhanVien.getChucVu().getTenChucVu();
            if (tenChucVu != null && !tenChucVu.isBlank()) {
                String normalized = tenChucVu.trim().toUpperCase(Locale.ROOT);
                if (normalized.contains("ADMIN") || normalized.contains("QUAN TRI")) {
                    return findByDefaultMa("ADMIN");
                }
                if (normalized.contains("EMPLOYEE") || normalized.contains("STAFF") || normalized.contains("NHAN VIEN")) {
                    return findByDefaultMa("EMPLOYEE");
                }

                return chucVuRepository.findFirstByTenChucVuContainingIgnoreCase(tenChucVu.trim())
                        .orElseGet(() -> findByDefaultMa(vaiTroFallback));
            }
        }

        return findByDefaultMa(vaiTroFallback);
    }

    private ChucVu findByDefaultMa(String vaiTro) {
        String normalizedRole = vaiTro == null ? "EMPLOYEE" : vaiTro.trim().toUpperCase(Locale.ROOT);
        String ma = "ADMIN".equals(normalizedRole) ? "ADMIN" : "EMPLOYEE";
        return chucVuRepository.findByMaChucVuIgnoreCase(ma)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ mặc định: " + ma));
    }

    @Override
    public void delete(Integer id) {
        nhanVienRepository.deleteById(id);
    }

    @Override
    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id).orElse(null);
    }

    @Override
    public List<NhanVien> findAll() {
        return nhanVienRepository.findAll();
    }
}