package org.example.datnnhom03.security;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repository.KhachHangRepository;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.example.datnnhom03.Repository.TaiKhoanRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticatedActorService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    public AuthenticatedActorService(TaiKhoanRepository taiKhoanRepository,
                                     NhanVienRepository nhanVienRepository,
                                     KhachHangRepository khachHangRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
    }

    public Optional<TaiKhoan> getTaiKhoan(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return taiKhoanRepository.findByEmail(email.trim());
    }

    public Optional<NhanVien> getNhanVien(Authentication authentication) {
        return getTaiKhoan(authentication)
                .map(TaiKhoan::getId)
                .flatMap(nhanVienRepository::findByTaiKhoan_Id);
    }

    public Optional<Integer> getNhanVienId(Authentication authentication) {
        return getNhanVien(authentication).map(NhanVien::getId);
    }

    public Optional<String> getEmail(Authentication authentication) {
        return getTaiKhoan(authentication)
                .map(TaiKhoan::getEmail)
                .filter(email -> email != null && !email.isBlank());
    }

    public Optional<KhachHang> getKhachHang(Authentication authentication) {
        return getTaiKhoan(authentication)
                .map(TaiKhoan::getId)
                .flatMap(khachHangRepository::findByTaiKhoan_Id);
    }

    public String getNormalizedRole(Authentication authentication) {
        if (authentication == null) return "";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String normalized = normalizeRole(authority.getAuthority());
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return getTaiKhoan(authentication)
                .map(TaiKhoan::getVaiTro)
                .map(this::normalizeRole)
                .orElse("");
    }

    public boolean isAdmin(Authentication authentication) {
        return "ADMIN".equals(getNormalizedRole(authentication));
    }

    public boolean isEmployeeLike(Authentication authentication) {
        String role = getNormalizedRole(authentication);
        return "ADMIN".equals(role) || "EMPLOYEE".equals(role);
    }

    public String getDisplayName(Authentication authentication) {
        return getNhanVien(authentication)
                .map(NhanVien::getTenNhanVien)
                .filter(name -> name != null && !name.isBlank())
                .or(() -> getKhachHang(authentication)
                        .map(KhachHang::getTenKhachHang)
                        .filter(name -> name != null && !name.isBlank()))
                .or(() -> getTaiKhoan(authentication).map(TaiKhoan::getEmail))
                .orElse("Người dùng DirtyWave");
    }

    public String normalizeRole(String rawRole) {
        String normalized = String.valueOf(rawRole == null ? "" : rawRole)
                .trim()
                .toUpperCase()
                .replace("ROLE_", "")
                .replace(' ', '_');
        if (normalized.equals("NHAN_VIEN") || normalized.equals("NHANVIEN") || normalized.equals("EMPLOYEE")) {
            return "EMPLOYEE";
        }
        if (normalized.equals("ADMIN")) {
            return "ADMIN";
        }
        if (normalized.equals("CUSTOMER") || normalized.equals("KHACH_HANG") || normalized.equals("KHACHHANG") || normalized.equals("USER")) {
            return "CUSTOMER";
        }
        return normalized;
    }
}
