package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Repository.PTTTRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PhuongThucThanhToanService {

    private final PTTTRepository repo;

    public PhuongThucThanhToanService(PTTTRepository repo) {
        this.repo = repo;
    }

    public List<PTTT> findAll() {
        return repo.findAll();
    }

    public void toggleTrangThai(Integer id) {
        PTTT pttt = repo.findById(id).orElseThrow();

        if ("Hoạt động".equalsIgnoreCase(pttt.getTrangThai())) {
            pttt.setTrangThai("Ngừng");
        } else {
            pttt.setTrangThai("Hoạt động");
        }

        pttt.setNgaySua(LocalDateTime.now());
        repo.save(pttt);
    }
}
