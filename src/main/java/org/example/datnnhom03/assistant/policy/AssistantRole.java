package org.example.datnnhom03.assistant.policy;

public enum AssistantRole {
    ADMIN,
    EMPLOYEE;

    public static AssistantRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role không được để trống");
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.equals("STAFF") || normalized.equals("NHAN_VIEN") || normalized.equals("NHANVIEN")) {
            return EMPLOYEE;
        }
        return AssistantRole.valueOf(normalized);
    }
}