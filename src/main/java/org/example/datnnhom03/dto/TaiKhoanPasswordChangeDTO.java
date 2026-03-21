package org.example.datnnhom03.dto;

import lombok.Data;

@Data
public class TaiKhoanPasswordChangeDTO {
    private String currentPassword;
    private String newPassword;
}
