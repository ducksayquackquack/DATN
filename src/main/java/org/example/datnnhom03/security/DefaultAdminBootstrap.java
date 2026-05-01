package org.example.datnnhom03.security;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repository.TaiKhoanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminBootstrap implements CommandLineRunner {

    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@dirtywave.com";

        if (taiKhoanRepository.existsByEmail(adminEmail)) {
            return;
        }

        TaiKhoan admin = new TaiKhoan();
        admin.setEmail(adminEmail);
        admin.setVaiTro("ADMIN");
        admin.setMatKhau(passwordEncoder.encode("123456"));
        admin.setTrangThaiHoatDong("Hoạt động");
        admin.setTrangThaiTaiKhoan("Kích hoạt");

        taiKhoanRepository.save(admin);
        System.out.println("[BOOTSTRAP] Created default admin account: " + adminEmail);
    }
}
