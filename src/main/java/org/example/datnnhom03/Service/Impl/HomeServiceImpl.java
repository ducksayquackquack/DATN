package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Service.HomeService;
import org.example.datnnhom03.Service.SanPhamService;
import org.example.datnnhom03.Service.DanhMucService;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.example.datnnhom03.dto.home.DanhMucDTO;
import org.example.datnnhom03.dto.home.HomeProductDTO;
import org.example.datnnhom03.dto.home.HomeResponseDTO;
import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeServiceImpl implements HomeService {

    private final SanPhamService sanPhamService;
    private final DanhMucService danhMucService;
    private final KhuyenMaiService khuyenMaiService;

    public HomeServiceImpl(SanPhamService sanPhamService,
                           DanhMucService danhMucService,
                           KhuyenMaiService khuyenMaiService) {
        this.sanPhamService = sanPhamService;
        this.danhMucService = danhMucService;
        this.khuyenMaiService = khuyenMaiService;
    }

    @Override
    public HomeResponseDTO getHomeData() {

        List<HomeProductDTO> products = new ArrayList<>();
        List<DanhMucDTO> categories = danhMucService.getActiveDanhMucDTO();
        List<SanPham> sanPhams = sanPhamService.getSanPhamHoatDong();

        for (SanPham sanPham : sanPhams) {

            SanPhamChiTiet spct =
                    sanPhamService.getOneActiveSanPhamChiTiet(sanPham.getId());

            if (spct == null) {
                continue;
            }

            HomeProductDTO dto = new HomeProductDTO();

            dto.setId(sanPham.getId());
            dto.setMaSanPham(sanPham.getMaSanPham());
            dto.setTenSanPham(sanPham.getTenSanPham());
            dto.setMoTa(sanPham.getMoTa());

            // BigDecimal -> Double
            if (spct.getGiaBan() != null) {
                dto.setGiaBan(spct.getGiaBan().doubleValue());
            }

            // DanhMuc comes from SanPhamChiTiet
            if (spct.getDanhMuc() != null) {
                dto.setIdDanhMuc(spct.getDanhMuc().getId());
            }

            if (sanPham.getIdKhuyenMai() != null) {
                dto.setGiaTriKhuyenMai(
                        khuyenMaiService.getGiaTriKhuyenMai(sanPham.getIdKhuyenMai())
                );
            }

            products.add(dto);
        }

        return new HomeResponseDTO(products, categories);
    }
}