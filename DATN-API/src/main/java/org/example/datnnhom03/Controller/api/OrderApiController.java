package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.PhuongThucThanhToan;
import org.example.datnnhom03.Model.TrangThaiHoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@CrossOrigin("*")
public class OrderApiController {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    // =============================
    // TẠO ĐƠN HÀNG (COD)
    // =============================
    @PostMapping("/create")
    public String createOrder(@RequestBody Map<String,Object> data){

        try{

            HoaDon hd = new HoaDon();

            hd.setMaHoaDon("HD" + System.currentTimeMillis());

            hd.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);

            hd.setPhuongThucThanhToan(PhuongThucThanhToan.TIEN_MAT);

            hd.setNgayTao(LocalDateTime.now());

            Double total =
                    Double.parseDouble(data.get("total").toString());

            hd.setThanhTien(total);

            hoaDonRepository.save(hd);

            return "SUCCESS";

        }catch(Exception e){

            return "ERROR: " + e.getMessage();
        }
    }

}