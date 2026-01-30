package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Repsotiory.KhachHangRepository;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KhachHangServiceImpl implements KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Override
    public KhachHang create(KhachHang khachHang) {
        return khachHangRepository.save(khachHang);
    }

    @Override
    public KhachHang update(Integer id, KhachHang khachHang) {
        KhachHang kh = khachHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        return khachHangRepository.save(kh);
    }

    @Override
    public void delete(Integer id) {
        khachHangRepository.deleteById(id);
    }

    @Override
    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id).orElse(null);
    }

    @Override
    public List<KhachHang> findAll() {
        return khachHangRepository.findAll();
    }
}

