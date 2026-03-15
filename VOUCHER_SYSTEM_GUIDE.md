# 🎫 Hướng Dẫn Sử Dụng Hệ Thống Phiếu Giảm Giá

## 📋 Tổng Quan

Hệ thống phiếu giảm giá tự động cho phép:
- ✅ Tự động chọn phiếu giảm giá tốt nhất khi bán hàng
- ✅ Gợi ý mua thêm để được giảm giá tốt hơn
- ✅ Thông báo real-time khi có phiếu mới tốt hơn
- ✅ Quản lý phiếu giảm giá công khai và cá nhân

## 🚀 Cách Sử Dụng

### 1. Quản Lý Phiếu Giảm Giá

#### Truy cập trang quản lý:
```
http://localhost:5174/admin/khuyen-mai/list
```

Trang này có 2 tab:
- **📢 Đợt khuyến mãi**: Quản lý các đợt khuyến mãi cũ (giữ nguyên)
- **🎫 Phiếu giảm giá**: Quản lý phiếu giảm giá mới (tích hợp POS)

#### Tạo phiếu giảm giá mới:
1. Click tab "🎫 Phiếu giảm giá"
2. Click nút "Tạo phiếu giảm giá"
3. Điền thông tin:
   - **Mã phiếu**: VD: SUMMER2024
   - **Tên phiếu**: VD: Giảm giá mùa hè
   - **Loại phiếu**: 
     - Công khai: Áp dụng cho tất cả đơn hàng
     - Cá nhân: Chỉ cho khách hàng cụ thể
   - **Hình thức giảm**:
     - Phần trăm (%): Giảm theo %
     - Số tiền cố định: Giảm số tiền cụ thể
   - **Giá trị giảm**: VD: 20 (cho 20%) hoặc 50000 (cho 50,000đ)
   - **Giảm tối đa**: Chỉ áp dụng cho giảm theo % (VD: giảm 20% nhưng tối đa 100,000đ)
   - **Đơn hàng tối thiểu**: Giá trị đơn hàng tối thiểu để áp dụng
   - **Số lượng sử dụng**: Số lần phiếu có thể được sử dụng
   - **Thời gian**: Ngày bắt đầu và kết thúc

4. Click "Lưu"

### 2. Sử Dụng Trong Bán Hàng (POS)

#### Truy cập trang tạo hóa đơn:
```
http://localhost:5174/admin/hoa-don/detail/create
```

#### Quy trình tự động:
1. **Thêm sản phẩm vào đơn hàng**
   - Hệ thống tự động tải tất cả phiếu giảm giá đang hoạt động
   - Tự động chọn phiếu tốt nhất (giảm nhiều nhất)

2. **Hiển thị phiếu đã chọn**
   - Thẻ phiếu màu tím gradient
   - Hiển thị tên phiếu, mã phiếu, số tiền giảm
   - Nút "×" để bỏ áp dụng phiếu

3. **Gợi ý mua thêm** (nếu có)
   ```
   💡 Gợi ý mua thêm để được giảm giá tốt hơn:
   
   [Phiếu ABC] 🔥 Đáng mua
   Mua thêm 50,000₫ để được giảm 100,000₫
   ```

4. **Thông báo phiếu mới tốt hơn**
   - Khi admin thêm phiếu mới tốt hơn
   - Modal popup tự động hiện ra
   - So sánh phiếu cũ vs phiếu mới
   - Cho phép chọn "Giữ phiếu cũ" hoặc "Áp dụng phiếu mới"

5. **Chọn phiếu thủ công**
   - Click nút "🎁 Chọn phiếu giảm giá"
   - Xem danh sách tất cả phiếu khả dụng
   - Sắp xếp theo giá trị giảm (cao nhất trước)
   - Click để chọn phiếu

6. **Tính toán tự động**
   - Tạm tính: Tổng giá sản phẩm
   - Phí ship: Nhập thủ công
   - Giảm giá: Tự động tính (màu đỏ)
   - Tổng: Tạm tính + Phí ship - Giảm giá

7. **Lưu hóa đơn**
   - Click "Lưu"
   - Hệ thống lưu hóa đơn kèm phiếu giảm giá
   - Giảm số lượng sử dụng của phiếu

## 🔄 Cơ Chế Tự Động

### Auto-Select Best Voucher
```javascript
// Khi thêm sản phẩm:
1. Lọc phiếu khả dụng (đủ điều kiện)
2. Tính giá trị giảm cho từng phiếu
3. Sắp xếp theo giá trị giảm (cao → thấp)
4. Chọn phiếu đầu tiên (giảm nhiều nhất)
5. Áp dụng tự động
```

### Real-time Polling
```javascript
// Mỗi 30 giây:
1. Kiểm tra phiếu mới từ server
2. So sánh với phiếu đang dùng
3. Nếu phiếu mới tốt hơn >5%:
   - Hiển thị modal thông báo
   - Cho phép người dùng chọn
```

### Smart Suggestions
```javascript
// Tính toán gợi ý:
1. Lọc phiếu chưa đủ điều kiện
2. Tính số tiền cần mua thêm
3. Tính giá trị giảm tiềm năng
4. Đánh giá "đáng mua" nếu:
   - Giá trị giảm > 10% số tiền cần mua thêm
5. Hiển thị top 3 gợi ý
```

## 📊 Ví Dụ Thực Tế

### Ví dụ 1: Giảm theo %
```
Phiếu: SUMMER20
- Giảm: 20%
- Giảm tối đa: 100,000đ
- Đơn tối thiểu: 200,000đ

Đơn hàng: 500,000đ
→ Giảm: 100,000đ (20% của 500k = 100k, đã đạt max)
→ Thanh toán: 400,000đ
```

### Ví dụ 2: Giảm cố định
```
Phiếu: FLASH50K
- Giảm: 50,000đ
- Đơn tối thiểu: 300,000đ

Đơn hàng: 350,000đ
→ Giảm: 50,000đ
→ Thanh toán: 300,000đ
```

### Ví dụ 3: Gợi ý mua thêm
```
Đơn hiện tại: 180,000đ

Phiếu MEGA100:
- Giảm: 100,000đ
- Đơn tối thiểu: 200,000đ

→ Gợi ý: "Mua thêm 20,000đ để được giảm 100,000đ 🔥 Đáng mua"
```

## 🛠️ API Backend Cần Có

### 1. Lấy phiếu đang hoạt động
```
GET /api/phieu-giam-gia/active
Response: Array<Voucher>
```

### 2. Lấy phiếu khả dụng cho đơn hàng
```
POST /api/phieu-giam-gia/applicable
Body: { subtotal, customerId }
Response: Array<Voucher>
```

### 3. Áp dụng phiếu
```
POST /api/phieu-giam-gia/{id}/apply
Body: { subtotal, customerId }
Response: { discount, finalAmount }
```

### 4. CRUD phiếu giảm giá
```
GET    /api/phieu-giam-gia
GET    /api/phieu-giam-gia/{id}
POST   /api/phieu-giam-gia
PUT    /api/phieu-giam-gia/{id}
DELETE /api/phieu-giam-gia/{id}
```

## 🎨 Giao Diện

### Màu sắc chính:
- Primary: Gradient tím (#667eea → #764ba2)
- Success: Xanh lá (#22c55e)
- Warning: Vàng (#f59e0b)
- Danger: Đỏ (#dc2626)

### Icons:
- Material Icons Outlined
- Emoji cho các trạng thái đặc biệt

## 🐛 Xử Lý Lỗi

### Nếu không có phiếu nào:
- Hiển thị nút "🎁 Chọn phiếu giảm giá"
- Click vào sẽ thấy "Không có phiếu giảm giá khả dụng"

### Nếu phiếu không còn đủ điều kiện:
- Tự động bỏ phiếu khi giá trị đơn giảm
- Thông báo cho người dùng

### Nếu API lỗi:
- Log error ra console
- Không crash app
- Cho phép tiếp tục bán hàng không có phiếu

## 📝 Checklist Triển Khai

- [x] Service layer với logic tính toán
- [x] Component VoucherSelector
- [x] Tích hợp vào HoaDonDetail
- [x] Trang quản lý phiếu (List + Form)
- [x] Router configuration
- [ ] Backend API endpoints
- [ ] Testing với dữ liệu thật
- [ ] Xử lý edge cases
- [ ] Tối ưu performance

## 🎯 Tính Năng Nâng Cao (Tương Lai)

1. **WebSocket thay vì polling**
   - Real-time notification tức thì
   - Giảm tải server

2. **Phiếu giảm giá cá nhân**
   - Gửi email cho khách hàng
   - QR code để quét

3. **Lịch sử sử dụng phiếu**
   - Tracking ai đã dùng phiếu nào
   - Báo cáo hiệu quả

4. **Combo phiếu**
   - Áp dụng nhiều phiếu cùng lúc
   - Ưu tiên theo quy tắc

5. **A/B Testing**
   - Test hiệu quả các loại phiếu
   - Tối ưu chiến lược giảm giá

---

**Lưu ý**: Đảm bảo backend API đã sẵn sàng trước khi test đầy đủ tính năng!
