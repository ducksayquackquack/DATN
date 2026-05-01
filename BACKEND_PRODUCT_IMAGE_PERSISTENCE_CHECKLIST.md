# Backend Checklist: Product Persistence And Product Image Persistence

File này mô tả chính xác những gì Spring backend cần hỗ trợ để các sản phẩm tạo mới hiển thị đúng dữ liệu và đúng ảnh trên mọi máy, mọi origin, mọi tunnel.

## Mục tiêu

Backend phải trở thành nguồn dữ liệu thật cho:

- thông tin sản phẩm mới tạo
- danh sách ảnh sản phẩm
- ảnh theo màu của biến thể
- dữ liệu đủ để frontend không cần fallback ngẫu nhiên nữa

## Thực trạng hiện tại đã kiểm tra

- Frontend form admin đang gửi ảnh trong payload khi tạo/cập nhật sản phẩm.
- API đọc sản phẩm hiện không trả lại ảnh cho các sản phẩm mới như `SP023`, `SP024`, `SP025`.
- Frontend đang phải đoán ảnh theo tên sản phẩm, loại sản phẩm hoặc màu.
- Cấu hình override ảnh trong frontend hiện có thể lưu tạm trong localStorage, nên không bền vững theo máy hoặc origin.

## Payload frontend hiện đang gửi

Tại save flow trong [src/views/admin/san-pham/ProductForm.vue](src/views/admin/san-pham/ProductForm.vue#L2528), frontend đang gửi các field sau:

```json
{
  "maSanPham": "SP025",
  "tenSanPham": "Magenta Hoodie DirtyWave",
  "moTa": "",
  "trangThai": "Hoạt động",
  "anh": "...",
  "anhChinh": "...",
  "hinhAnh": "...",
  "images": ["...", "..."],
  "listAnh": ["...", "..."],
  "mauSacHinhAnhs": [
    { "colorId": 7, "image": "...", "order": 0 },
    { "colorId": 6, "image": "...", "order": 1 }
  ],
  "anhTheoMauSac": [
    { "colorId": 7, "image": "...", "order": 0 },
    { "colorId": 6, "image": "...", "order": 1 }
  ],
  "colorImages": [
    { "colorId": 7, "image": "...", "order": 0 },
    { "colorId": 6, "image": "...", "order": 1 }
  ],
  "sanPhamChiTiets": [...],
  "deletedVariantIds": [...],
  "sanPhamChiTietIdsXoa": [...],
  "xoaSanPhamChiTietIds": [...]
}
```

## Bắt buộc backend phải làm

1. Persist trường ảnh sản phẩm chính

- Lưu `anh`, `anhChinh`, `hinhAnh`
- Chọn một trường canonical trong DB, ví dụ `anh_chinh`
- Khi đọc ra API, map lại đầy đủ cho frontend cũ:
  - `anh`
  - `anhChinh`
  - `hinhAnh`

2. Persist gallery ảnh sản phẩm

- Lưu `images` hoặc `listAnh` thành bảng con, ví dụ `san_pham_hinh_anh`
- Mỗi record nên có:
  - `id`
  - `san_pham_id`
  - `duong_dan_anh`
  - `thu_tu`
  - `la_anh_chinh`

3. Persist ảnh theo màu

- Lưu `colorImages`, `mauSacHinhAnhs`, `anhTheoMauSac` vào bảng ví dụ `san_pham_mau_sac_hinh_anh`
- Tối thiểu mỗi record cần có:
  - `id`
  - `san_pham_id`
  - `mau_sac_id`
  - `duong_dan_anh`
  - `thu_tu`

4. Trả lại đầy đủ ở API đọc sản phẩm

- `GET /api/san-pham`
- `GET /api/san-pham/{id}`

Phải trả ít nhất:

```json
{
  "id": 25,
  "maSanPham": "SP025",
  "tenSanPham": "Magenta Hoodie DirtyWave",
  "anh": "/uploads/products/sp025/main.jpg",
  "anhChinh": "/uploads/products/sp025/main.jpg",
  "hinhAnh": "/uploads/products/sp025/main.jpg",
  "images": [
    "/uploads/products/sp025/main.jpg",
    "/uploads/products/sp025/detail-1.jpg"
  ],
  "listAnh": [
    "/uploads/products/sp025/main.jpg",
    "/uploads/products/sp025/detail-1.jpg"
  ],
  "colorImages": [
    { "colorId": 7, "image": "/uploads/products/sp025/pink.jpg", "order": 0 },
    { "colorId": 6, "image": "/uploads/products/sp025/purple.jpg", "order": 1 }
  ],
  "anhTheoMauSac": [
    { "colorId": 7, "image": "/uploads/products/sp025/pink.jpg", "order": 0 },
    { "colorId": 6, "image": "/uploads/products/sp025/purple.jpg", "order": 1 }
  ],
  "mauSacHinhAnhs": [
    { "colorId": 7, "image": "/uploads/products/sp025/pink.jpg", "order": 0 },
    { "colorId": 6, "image": "/uploads/products/sp025/purple.jpg", "order": 1 }
  ],
  "sanPhamChiTiets": [...]
}
```

5. Serve thư mục uploads công khai

- Backend phải expose static files cho `/uploads/**`
- URL phải truy cập được từ frontend qua:
  - local
  - Cloudflare tunnel
  - domain thật

Ví dụ Spring MVC config:

```java
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:/absolute/path/to/uploads/");
    }
}
```

6. Nếu dùng upload file thật, cần endpoint upload

Ví dụ:

- `POST /api/uploads/products`
- nhận `MultipartFile`
- trả về đường dẫn chuẩn như `/uploads/products/sp025/main.jpg`

Response khuyến nghị:

```json
{
  "url": "/uploads/products/sp025/main.jpg",
  "fileName": "main.jpg",
  "contentType": "image/jpeg"
}
```

7. Không trả dữ liệu rỗng cho sản phẩm mới tạo

Sau khi `POST /api/san-pham` hoặc `PUT /api/san-pham/{id}` thành công:

- response nên chứa `id` sản phẩm
- tốt hơn nữa là trả object sản phẩm đã hydrate đầy đủ ảnh

## Mapping DTO backend nên có

Product response DTO nên có các field này để frontend hiện tại hoạt động ngay mà không phải đổi API shape lớn:

- `id`
- `maSanPham`
- `tenSanPham`
- `moTa`
- `trangThai`
- `anh`
- `anhChinh`
- `hinhAnh`
- `images`
- `listAnh`
- `colorImages`
- `anhTheoMauSac`
- `mauSacHinhAnhs`
- `sanPhamChiTiets`

## Các chỗ frontend đang đọc ảnh

Các view khách hàng đang ưu tiên các field ảnh như sau:

- [src/views/customer/HomeView.vue](src/views/customer/HomeView.vue#L535)
- [src/views/customer/pages/CustomerProductsPage.vue](src/views/customer/pages/CustomerProductsPage.vue#L384)
- [src/views/customer/ProductDetail.vue](src/views/customer/ProductDetail.vue#L400)

Các key frontend đang dò tìm bao gồm:

- `anh`
- `hinhAnh`
- `image`
- `imageUrl`
- `images`
- `listAnh`
- `anhChinh`
- `duongDanAnh`

## Checklist DB khuyến nghị

1. Thêm bảng gallery ảnh nếu chưa có
2. Thêm bảng ảnh theo màu nếu chưa có
3. Ràng buộc foreign key về `san_pham` và `mau_sac`
4. Có cột `thu_tu` để giữ thứ tự ảnh
5. Có cột `created_at`, `updated_at` nếu project đang theo convention audit

## Checklist API test sau khi sửa backend

1. Tạo sản phẩm mới với ít nhất 2 ảnh gallery và 2 ảnh theo màu
2. Gọi `GET /api/san-pham/{id}`
3. Xác nhận response có đủ `images` và `colorImages`
4. Mở trực tiếp từng URL `/uploads/...` trên browser
5. Kiểm tra local frontend hiển thị đúng ảnh
6. Kiểm tra tunnel `live.dirtywave.com` hiển thị đúng y hệt local
7. Kiểm tra máy khác hoặc tab ẩn danh vẫn thấy đúng ảnh

## Tiêu chí hoàn thành

Chỉ được coi là xong khi:

- sản phẩm tạo mới không còn phụ thuộc localStorage
- ảnh đúng trên trang chủ
- ảnh đúng ở danh sách sản phẩm
- ảnh đúng ở trang chi tiết
- ảnh đúng trên domain thật và trên máy khác
- dữ liệu sản phẩm mới đọc lại từ API vẫn chính xác sau khi restart frontend/backend