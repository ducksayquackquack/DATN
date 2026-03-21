package org.example.datnnhom03.dto.hoadon;

public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.message = msg;
        return r;
    }
}
