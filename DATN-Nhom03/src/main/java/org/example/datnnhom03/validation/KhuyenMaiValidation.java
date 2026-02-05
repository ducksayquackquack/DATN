package org.example.datnnhom03.validation;

import org.example.datnnhom03.dto.KhuyenMaiDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KhuyenMaiValidation {

    public List<String> validate(KhuyenMaiDTO dto) {
        List<String> errors = new ArrayList<>();

        // Mã khuyến mãi
        if (dto.getMaKhuyenMai() == null || dto.getMaKhuyenMai().trim().isEmpty()) {
            errors.add("Mã khuyến mãi không được để trống");
        }

        // Tên khuyến mãi
        if (dto.getTenKhuyenMai() == null || dto.getTenKhuyenMai().trim().isEmpty()) {
            errors.add("Tên khuyến mãi không được để trống");
        }

        // Giá trị giảm (%)
        if (dto.getGiaTriGiam() == null) {
            errors.add("Giá trị giảm không được để trống");
        } else if (dto.getGiaTriGiam() <= 0) {
            errors.add("Giá trị giảm phải lớn hơn 0");
        } else if (dto.getGiaTriGiam() > 100) {
            errors.add("Giá trị giảm không được vượt quá 100%");
        }

        // Ngày bắt đầu & ngày kết thúc
        if (dto.getNgayBatDau() == null || dto.getNgayKetThuc() == null) {
            errors.add("Ngày bắt đầu và ngày kết thúc không được để trống");
        } else if (dto.getNgayKetThuc().isBefore(dto.getNgayBatDau())) {
            errors.add("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Trạng thái
        if (dto.getTrangThai() == null || dto.getTrangThai().trim().isEmpty()) {
            errors.add("Trạng thái không được để trống");
        }

        return errors;
    }
}
