package org.example.datnnhom03.validation;

import org.example.datnnhom03.dto.KichThuocDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KichThuocValidation {

    public List<String> validate(KichThuocDTO dto) {
        List<String> errors = new ArrayList<>();

        // Validate tên kích thước
        if (dto.getTenKichThuoc() == null || dto.getTenKichThuoc().trim().isEmpty()) {
            errors.add("Tên kích thước không được để trống");
        } else if (dto.getTenKichThuoc().length() > 50) {
            errors.add("Tên kích thước không được vượt quá 50 ký tự");
        }

        // Validate trạng thái (nếu có dùng)
        if (dto.getTrangThai() == null || dto.getTrangThai().trim().isEmpty()) {
            errors.add("Trạng thái không được để trống");
        }

        return errors;
    }
}
