// File: src/main/java/org/example/datnnhom03/Service/RegisterServiceImpl.java
package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repsotiory.KhachHangRepository;
import org.example.datnnhom03.Repsotiory.TaiKhoanRepository;
import org.example.datnnhom03.Service.RegisterService;
import org.example.datnnhom03.dto.RegisterDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterServiceImpl implements RegisterService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterServiceImpl(TaiKhoanRepository taiKhoanRepository,
                               KhachHangRepository khachHangRepository,
                               PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean register(RegisterDTO dto) {

        String email = dto.getEmail().trim();

        if (taiKhoanRepository.existsByEmail(email)) {
            return false;
        }

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setEmail(email);
        taiKhoan.setMatKhau(passwordEncoder.encode(dto.getMatKhau()));
        taiKhoan.setVaiTro("CUSTOMER");
        taiKhoan.setTrangThaiTaiKhoan("Kích hoạt");

        TaiKhoan savedTaiKhoan = taiKhoanRepository.save(taiKhoan);

        KhachHang khachHang = new KhachHang();
        khachHang.setTaiKhoan(savedTaiKhoan);
        khachHang.setTenKhachHang(dto.getTenKhachHang());
        khachHang.setSoDienThoai(dto.getSoDienThoai());
        khachHang.setMaKhachHang("KH" + System.currentTimeMillis());

        khachHangRepository.save(khachHang);

        return true;
    }
}