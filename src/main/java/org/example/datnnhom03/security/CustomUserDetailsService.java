package org.example.datnnhom03.security;

import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repsotiory.TaiKhoanRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final TaiKhoanRepository taiKhoanRepository;

    public CustomUserDetailsService(TaiKhoanRepository taiKhoanRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email)
                );

        // Chặn đăng nhập nếu tài khoản bị khóa
        if ("Khóa".equalsIgnoreCase(taiKhoan.getTrangThaiTaiKhoan())) {
            throw new DisabledException("Tài khoản đã bị khóa");
        }

        return new User(
                taiKhoan.getEmail(),
                taiKhoan.getMatKhau(),
                List.of(new SimpleGrantedAuthority("ROLE_" + taiKhoan.getVaiTro()))
        );
    }
}
