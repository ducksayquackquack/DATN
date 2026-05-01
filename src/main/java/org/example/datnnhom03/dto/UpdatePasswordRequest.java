package org.example.datnnhom03.dto;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    private String currentPassword;
    private String newPassword;
}
