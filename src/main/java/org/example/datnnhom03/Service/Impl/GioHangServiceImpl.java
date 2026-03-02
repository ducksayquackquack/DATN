package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.GioHang;
import org.example.datnnhom03.Repository.GioHangRepository;
import org.example.datnnhom03.Service.GioHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GioHangServiceImpl implements GioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Override
    public GioHang create(GioHang gioHang) {
        return gioHangRepository.save(gioHang);
    }

    @Override
    public GioHang update(Integer id, GioHang gioHang) {
        GioHang gh = gioHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        gh.setKhachHang(gioHang.getKhachHang());
        return gioHangRepository.save(gh);
    }

    @Override
    public void delete(Integer id) {
        gioHangRepository.deleteById(id);
    }

    @Override
    public GioHang findById(Integer id) {
        return gioHangRepository.findById(id).orElse(null);
    }

    @Override
    public List<GioHang> findAll() {
        return gioHangRepository.findAll();
    }

    @Override
    public GioHang findByKhachHangId(Integer khachHangId) {
        return gioHangRepository.findByKhachHangId(khachHangId)
                .orElse(null);
    }
}