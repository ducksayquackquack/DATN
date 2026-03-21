package org.example.datnnhom03.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.DiaChi;
import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repository.DiaChiRepository;
import org.example.datnnhom03.Repository.KhachHangRepository;
import org.example.datnnhom03.Repository.TaiKhoanRepository;
import org.example.datnnhom03.Service.TaiKhoanService;
import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.example.datnnhom03.dto.TaiKhoanPasswordChangeDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanRepository repository;
    private final KhachHangRepository khachHangRepository;
    private final DiaChiRepository diaChiRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String TMP_CUSTOMER_CODE_PREFIX = "TMP-";

    private TaiKhoanDTO toDTO(TaiKhoan entity) {
        TaiKhoanDTO dto = new TaiKhoanDTO();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setVaiTro(entity.getVaiTro());
        dto.setAvatar(entity.getAvatar());
        dto.setTrangThaiHoatDong(entity.getTrangThaiHoatDong());
        dto.setTrangThaiTaiKhoan(entity.getTrangThaiTaiKhoan());
        return dto;
    }

    @Override
    public List<TaiKhoanDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TaiKhoanDTO findById(Integer id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public TaiKhoanDTO create(TaiKhoanDTO dto) {
        String normalizedEmail = normalizeEmail(firstNonBlank(dto.getEmail(), dto.getUsername(), dto.getTenDangNhap()));
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new RuntimeException("Email không hợp lệ");
        }

        String rawPassword = firstNonBlank(dto.getMatKhau(), dto.getPassword());
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }

        String normalizedRole = normalizeRole(dto.getVaiTro());

        if (repository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email đã tồn tại. Vui lòng dùng email khác.");
        }

        TaiKhoan entity = new TaiKhoan();
        entity.setEmail(normalizedEmail);
        entity.setVaiTro(normalizedRole);
        entity.setAvatar(dto.getAvatar());
        entity.setTrangThaiHoatDong(firstNonBlank(dto.getTrangThaiHoatDong(), "Hoạt động"));
        entity.setTrangThaiTaiKhoan(firstNonBlank(dto.getTrangThaiTaiKhoan(), "Kích hoạt"));

        entity.setMatKhau(passwordEncoder.encode(rawPassword));

        TaiKhoan savedAccount = repository.save(entity);

        if (isCustomerRole(normalizedRole)) {
            KhachHang customer = createOrUpdateCustomerProfile(savedAccount, dto);
            createOrUpdateDefaultAddress(customer, dto);
        }

        return toDTO(savedAccount);
    }

    @Override
    public TaiKhoanDTO update(Integer id, TaiKhoanDTO dto) {

        TaiKhoan entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getVaiTro() != null && !dto.getVaiTro().isBlank()) {
            entity.setVaiTro(normalizeRole(dto.getVaiTro()));
        }
        if (dto.getAvatar() != null) {
            entity.setAvatar(dto.getAvatar());
        }
        if (dto.getTrangThaiHoatDong() != null && !dto.getTrangThaiHoatDong().isBlank()) {
            entity.setTrangThaiHoatDong(dto.getTrangThaiHoatDong());
        }
        if (dto.getTrangThaiTaiKhoan() != null && !dto.getTrangThaiTaiKhoan().isBlank()) {
            entity.setTrangThaiTaiKhoan(dto.getTrangThaiTaiKhoan());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String newEmail = normalizeEmail(dto.getEmail());
            if (!newEmail.equalsIgnoreCase(entity.getEmail()) && repository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email đã tồn tại. Vui lòng dùng email khác.");
            }
            entity.setEmail(newEmail);
        }

        String rawPassword = firstNonBlank(dto.getMatKhau(), dto.getPassword());
        if (rawPassword != null && !rawPassword.isBlank()) {
            entity.setMatKhau(passwordEncoder.encode(rawPassword));
        }

        return toDTO(repository.save(entity));
    }

    @Override
    public void changePassword(Integer id, TaiKhoanPasswordChangeDTO dto) {
        TaiKhoan entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String currentPassword = dto == null ? null : firstNonBlank(dto.getCurrentPassword());
        String newPassword = dto == null ? null : firstNonBlank(dto.getNewPassword());

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, entity.getMatKhau())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        entity.setMatKhau(passwordEncoder.encode(newPassword));
        repository.save(entity);
    }

    @Override
    public void delete(Integer id) {
        TaiKhoan entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        entity.setTrangThaiTaiKhoan("Khóa");
        repository.save(entity);
    }

    private KhachHang createOrUpdateCustomerProfile(TaiKhoan account, TaiKhoanDTO dto) {
        KhachHang customer = khachHangRepository.findByTaiKhoan_Id(account.getId()).orElseGet(() -> {
            KhachHang kh = new KhachHang();
            kh.setTaiKhoan(account);
            kh.setMaKhachHang(TMP_CUSTOMER_CODE_PREFIX + System.nanoTime());
            return kh;
        });

        if (customer.getTaiKhoan() == null) {
            customer.setTaiKhoan(account);
        }

        String displayName = firstNonBlank(dto.getTenKhachHang(), toDisplayNameFromEmail(account.getEmail()));
        customer.setTenKhachHang(displayName);
        customer.setGioiTinh(firstNonBlank(dto.getGioiTinh(), customer.getGioiTinh(), "Khác"));
        customer.setSoDienThoai(firstNonBlank(dto.getSoDienThoai(), customer.getSoDienThoai(), ""));
        customer.setTrangThai(firstNonBlank(customer.getTrangThai(), "Hoạt động"));

        if (dto.getNgaySinh() != null && !dto.getNgaySinh().isBlank()) {
            try {
                customer.setNgaySinh(LocalDate.parse(dto.getNgaySinh().trim()));
            } catch (DateTimeParseException ignored) {
                // Keep existing date when input format is invalid.
            }
        }

        KhachHang saved = khachHangRepository.save(customer);

        if (saved.getMaKhachHang() == null
                || saved.getMaKhachHang().isBlank()
                || saved.getMaKhachHang().startsWith(TMP_CUSTOMER_CODE_PREFIX)) {
            saved.setMaKhachHang(String.format("KH%03d", saved.getId()));
            saved = khachHangRepository.save(saved);
        }

        return saved;
    }

    private void createOrUpdateDefaultAddress(KhachHang customer, TaiKhoanDTO dto) {
        if (customer == null || customer.getId() == null) return;

        DiaChi diaChi = diaChiRepository.findTopByKhachHang_IdOrderByIdDesc(customer.getId())
                .orElseGet(() -> {
                    DiaChi item = new DiaChi();
                    item.setKhachHang(customer);
                    return item;
                });

        if (diaChi.getKhachHang() == null) {
            diaChi.setKhachHang(customer);
        }

        diaChi.setDiaChiCuThe(firstNonBlank(dto.getDiaChiCuThe(), diaChi.getDiaChiCuThe(), "Chưa cập nhật"));
        diaChi.setTinhThanh(firstNonBlank(dto.getTinhThanh(), diaChi.getTinhThanh(), ""));
        diaChi.setQuanHuyen(firstNonBlank(dto.getQuanHuyen(), diaChi.getQuanHuyen(), ""));
        diaChi.setPhuongXa(firstNonBlank(dto.getPhuongXa(), diaChi.getPhuongXa(), ""));
        diaChi.setTrangThai(firstNonBlank(diaChi.getTrangThai(), "Hoạt động"));

        diaChiRepository.save(diaChi);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isCustomerRole(String role) {
        return "CUSTOMER".equalsIgnoreCase(normalizeRole(role));
    }

    private String normalizeRole(String role) {
        String normalized = firstNonBlank(role, "CUSTOMER").trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        if ("KHACH_HANG".equals(normalized) || "KHACHHANG".equals(normalized) || "USER".equals(normalized)) {
            return "CUSTOMER";
        }
        if (!"ADMIN".equals(normalized) && !"EMPLOYEE".equals(normalized) && !"CUSTOMER".equals(normalized)) {
            return "CUSTOMER";
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String toDisplayNameFromEmail(String email) {
        String localPart = firstNonBlank(email);
        if (localPart == null) return "Khách hàng";

        int atIndex = localPart.indexOf('@');
        if (atIndex > 0) {
            localPart = localPart.substring(0, atIndex);
        }

        String normalized = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) return "Khách hàng";

        StringBuilder builder = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return builder.isEmpty() ? "Khách hàng" : builder.toString();
    }
}