package org.example.datnnhom03.dto.hoadon;

import java.util.List;

public class HoaDonDetailDTO {
    public HoaDonRowDTO hoaDon;
    public List<HoaDonItemDTO> items;
    public List<OrderStatusHistoryDTO> history;

    // FE dùng để disable nút sửa/xóa/thêm sp
    public boolean finalOrder;
}
