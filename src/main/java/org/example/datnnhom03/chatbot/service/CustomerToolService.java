package org.example.datnnhom03.chatbot.service;

import org.example.datnnhom03.Model.*;
import org.example.datnnhom03.Repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;



@Service
public class CustomerToolService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PTTTRepository ptttRepository;
    private final KhuyenMaiRepository khuyenMaiRepository;
    private final KhuyenMaiSanPhamRepository khuyenMaiSanPhamRepository;

    public CustomerToolService(SanPhamRepository sanPhamRepository,
                               SanPhamChiTietRepository sanPhamChiTietRepository,
                               HoaDonRepository hoaDonRepository,
                               HoaDonChiTietRepository hoaDonChiTietRepository,
                               PhieuGiamGiaRepository phieuGiamGiaRepository,
                               KhuyenMaiRepository khuyenMaiRepository,
                               PTTTRepository ptttRepository,
                               KhuyenMaiSanPhamRepository khuyenMaiSanPhamRepository) {
        this.sanPhamRepository = sanPhamRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
        this.ptttRepository = ptttRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.khuyenMaiSanPhamRepository = khuyenMaiSanPhamRepository;
    }

    public List<Map<String, Object>> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();

        return khuyenMaiRepository.findAll().stream()
                .filter(km -> km.getNgayBatDau() == null || !km.getNgayBatDau().isAfter(now))
                .filter(km -> km.getNgayKetThuc() == null || !km.getNgayKetThuc().isBefore(now))
                .filter(km -> {
                    String status = normalize(km.getTrangThai());
                    return status.isBlank()
                            || status.contains("dang dien ra")
                            || status.contains("dang hoat dong")
                            || status.contains("hoat dong")
                            || status.contains("active");
                })
                .sorted(Comparator.comparing(KhuyenMai::getGiaTri, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(km -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ma", km.getMaKhuyenMai());
                    row.put("ten", km.getTenKhuyenMai());
                    row.put("giaTri", km.getGiaTri());
                    row.put("hinhThuc", km.getDonViGiam());
                    row.put("ngayBatDau", km.getNgayBatDau());
                    row.put("ngayKetThuc", km.getNgayKetThuc());
                    row.put("trangThai", km.getTrangThai());
                    return row;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchProducts(Map<String, Object> entities) {
        String category = safe(entities.get("category"));
        String color = safe(entities.get("color"));
        Integer maxPrice = intValue(entities.get("maxPrice"));
        String purpose = safe(entities.get("purpose"));
        String style = safe(entities.get("style"));
        int requestedCount = Optional.ofNullable(intValue(entities.get("requestedCount"))).filter(v -> v > 0).orElse(3);

        List<SanPham> allProducts = sanPhamRepository.findAll();
        List<Map.Entry<SanPham, Integer>> scored = new ArrayList<>();
        for (SanPham product : allProducts) {
            if (!isActive(product)) continue;
            List<SanPhamChiTiet> variants = variantsOf(product);
            if (variants.isEmpty()) continue;

            int score = 0;
            String productText = normalize(product.getTenSanPham()) + " " + normalize(product.getMoTa());
            String productCategoryText = normalize(productText + " "
                    + variants.stream().map(v -> nameOf(v.getLoai()) + " " + nameOf(v.getDanhMuc()))
                    .collect(Collectors.joining(" ")));

            if (!category.isBlank()) {
                if (!productCategoryText.contains(normalize(category))) {
                    continue;
                }
                score += 8;
            }
            if (!purpose.isBlank() && containsAny(normalize(purpose), "di lam")) score += 3;
            if (!style.isBlank() && containsAny(normalize(style), "de phoi")) score += 3;
            if (containsAny(productText, "bomber", "hoodie", "coach", "ao khoac")) score += 1;

            List<SanPhamChiTiet> availableVariants = variants.stream()
                    .filter(this::isVariantActive)
                    .filter(v -> v.getSoLuong() != null && v.getSoLuong() > 0)
                    .filter(v -> v.getGiaBan() != null)
                    .collect(Collectors.toList());
            if (availableVariants.isEmpty()) continue;

            if (!color.isBlank()) {
                availableVariants = availableVariants.stream()
                        .filter(v -> normalize(nameOf(v.getMauSac())).contains(normalize(color)))
                        .collect(Collectors.toList());
                if (availableVariants.isEmpty()) continue;
                score += 4;
            }

            if (maxPrice != null) {
                BigDecimal cheapest = availableVariants.stream()
                        .map(SanPhamChiTiet::getGiaBan)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                if (cheapest == null || cheapest.intValue() > maxPrice) {
                    continue;
                }
                score += 2;
            }

            score += availableVariants.stream().map(v -> Optional.ofNullable(v.getSoLuong()).orElse(0)).max(Integer::compareTo).orElse(0) / 10;
            scored.add(Map.entry(product, score));
        }

        return scored.stream()
                .sorted(Comparator.<Map.Entry<SanPham, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(requestedCount)
                .map(entry -> toProductPreview(entry.getKey(), variantsOf(entry.getKey())))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> lookupStock(Map<String, Object> entities) {
        String productCode = safe(entities.get("productCode"));
        String variantCode = safe(entities.get("variantCode"));
        String category = safe(entities.get("category"));
        String color = safe(entities.get("color"));
        String size = safe(entities.get("size"));

        return sanPhamChiTietRepository.findAll().stream()
                .filter(spct -> matchesVariant(spct, productCode, variantCode, category, color, size))
                .sorted(Comparator.comparing((SanPhamChiTiet spct) -> Optional.ofNullable(spct.getSoLuong()).orElse(0)).reversed())
                .limit(10)
                .map(this::toVariantPreview)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> findOrders(Map<String, Object> entities, ChatSession session) {
        String orderCode = safe(entities.get("orderCode"));
        String phone = safe(entities.get("phone"));
        if (phone.isBlank() && session != null) {
            phone = safe(session.getCustomerPhone());
        }

        List<HoaDon> matchedOrders = new ArrayList<>();
        if (!orderCode.isBlank()) {
            hoaDonRepository.findFirstByMaHoaDonIgnoreCase(orderCode)
                    .filter(this::isTrackableOrder)
                    .ifPresent(matchedOrders::add);
            if (!matchedOrders.isEmpty()) {
                return matchedOrders.stream().map(this::toOrderPreview).collect(Collectors.toList());
            }
        }

        final String finalPhone = digitsOnly(phone);
        if (orderCode.isBlank() && finalPhone.isBlank()) {
            return List.of();
        }

        return hoaDonRepository.findAll().stream()
                .filter(order -> !finalPhone.isBlank() && digitsOnly(order.getSoDienThoaiNhanHang()).contains(finalPhone))
                .filter(this::isTrackableOrder)
                .sorted(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .map(this::toOrderPreview)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getActiveVouchers() {
        LocalDate today = LocalDate.now();
        return phieuGiamGiaRepository.findAll().stream()
                .filter(v -> {
                    String status = normalize(v.getTrangThai());
                    return status.isBlank()
                            || status.contains("hoat dong")
                            || status.contains("active")
                            || status.contains("kich hoat");
                })
                .filter(v -> v.getSoLuongSuDung() == null || v.getSoLuongSuDung() > 0)
                .filter(v -> v.getNgayBatDau() == null || !v.getNgayBatDau().isAfter(today))
                .filter(v -> v.getNgayKetThuc() == null || !v.getNgayKetThuc().isBefore(today))
                .sorted(Comparator.comparing(PhieuGiamGia::getGiaTriGiamGia, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(v -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ma", v.getMaPhieuGiamGia());
                    row.put("ten", v.getTenPhieuGiamGia());
                    row.put("giaTri", v.getGiaTriGiamGia());
                    row.put("hinhThuc", Boolean.TRUE.equals(v.getHinhThucGiam()) ? "%" : "VND");
                    row.put("hoaDonToiThieu", v.getHoaDonToiThieu());
                    return row;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPaymentMethods() {
        return ptttRepository.findAll().stream()
                .filter(this::isPaymentMethodActive)
                .map(pttt -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ma", pttt.getMa());
                    row.put("ten", pttt.getTen());
                    row.put("moTa", pttt.getMoTa());
                    return row;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBestSellers(int limit) {
        Map<Integer, ProductSalesAgg> aggregateMap = new LinkedHashMap<>();

        Set<Integer> completedOrderIds = hoaDonRepository.findAll().stream()
                .filter(this::isCompletedOrder)
                .map(HoaDon::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (HoaDonChiTiet item : hoaDonChiTietRepository.findAll()) {
            if (item.getHoaDon() == null || item.getHoaDon().getId() == null || !completedOrderIds.contains(item.getHoaDon().getId())) {
                continue;
            }
            SanPhamChiTiet spct = item.getSanPhamChiTiet();
            if (spct == null || spct.getSanPham() == null || spct.getSanPham().getId() == null) continue;
            if (!isVariantActive(spct) || !isActive(spct.getSanPham())) continue;
            SanPham product = spct.getSanPham();
            ProductSalesAgg agg = aggregateMap.computeIfAbsent(product.getId(), id -> new ProductSalesAgg(product));
            agg.quantity += Optional.ofNullable(item.getSoLuong()).orElse(0);
            agg.revenue = agg.revenue.add(Optional.ofNullable(item.getThanhTien()).orElse(BigDecimal.ZERO));
        }

        return aggregateMap.values().stream()
                .sorted(Comparator.comparingInt((ProductSalesAgg agg) -> agg.quantity).reversed())
                .limit(limit)
                .map(agg -> {
                    Map<String, Object> row = toProductPreview(agg.product, variantsOf(agg.product));
                    row.put("totalQuantity", agg.quantity);
                    row.put("totalRevenue", agg.revenue);
                    return row;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> adviseSize(Map<String, Object> entities) {
        Integer heightCm = intValue(entities.get("heightCm"));
        Integer weightKg = intValue(entities.get("weightKg"));
        String recommended = "M";
        String confidence = "MEDIUM";

        if (weightKg == null && heightCm == null) {
            return Map.of(
                    "found", false,
                    "summary", "Anh/chị gửi giúp em chiều cao và cân nặng, ví dụ 1m70 65kg, để em gợi ý size sát hơn nhé."
            );
        }

        if (weightKg != null) {
            if (weightKg < 55) recommended = "S";
            else if (weightKg < 66) recommended = "M";
            else if (weightKg < 76) recommended = "L";
            else recommended = "XL";
            confidence = "HIGH";
        }

        if (heightCm != null) {
            if (heightCm >= 178 && "M".equals(recommended)) recommended = "L";
            if (heightCm <= 162 && "L".equals(recommended)) recommended = "M";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("size", recommended);
        result.put("confidence", confidence);
        result.put("heightCm", heightCm);
        result.put("weightKg", weightKg);
        String fitNote = "Nếu anh/chị thích mặc oversize thì có thể tăng thêm 1 size.";
        if (heightCm != null && weightKg != null && weightKg >= 75 && heightCm >= 175) {
            fitNote = "Nếu anh/chị thích form gọn thì giữ đúng size này, còn thích rộng tay hơn có thể tăng thêm 1 size.";
        }
        result.put("summary", "Với thông tin hiện tại, em nghiêng về size " + recommended + ". " + fitNote);
        return result;
    }

    private boolean matchesVariant(SanPhamChiTiet spct, String productCode, String variantCode, String category, String color, String size) {
        if (spct == null || spct.getSanPham() == null) return false;
        if (!isActive(spct.getSanPham())) return false;
        if (!isVariantActive(spct)) return false;
        if (Optional.ofNullable(spct.getSoLuong()).orElse(0) <= 0) return false;

        String maSpct = normalize(spct.getMa());
        String maSp = normalize(spct.getSanPham().getMaSanPham());
        String tenSp = normalize(spct.getSanPham().getTenSanPham());
        String mau = normalize(nameOf(spct.getMauSac()));
        String kichThuoc = normalize(nameOf(spct.getKichThuoc()));
        String loai = normalize(nameOf(spct.getLoai()));
        String danhMuc = normalize(nameOf(spct.getDanhMuc()));

        if (!variantCode.isBlank()) return maSpct.equals(normalize(variantCode));
        if (!productCode.isBlank() && !maSp.equals(normalize(productCode))) return false;
        if (!color.isBlank() && !mau.contains(normalize(color))) return false;
        if (!size.isBlank() && !kichThuoc.equals(normalize(size))) return false;
        if (!category.isBlank() && !(tenSp.contains(normalize(category)) || loai.contains(normalize(category)) || danhMuc.contains(normalize(category)))) {
            return false;
        }
        return !productCode.isBlank() || !variantCode.isBlank() || !category.isBlank() || !color.isBlank() || !size.isBlank();
    }

    private Map<String, Object> toVariantPreview(SanPhamChiTiet spct) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ma", spct.getMa());
        row.put("maSanPham", spct.getSanPham() == null ? null : spct.getSanPham().getMaSanPham());
        row.put("tenSanPham", spct.getSanPham() == null ? null : spct.getSanPham().getTenSanPham());
        row.put("soLuong", spct.getSoLuong());
        row.put("giaBan", spct.getGiaBan());
        row.put("mauSac", nameOf(spct.getMauSac()));
        row.put("kichThuoc", nameOf(spct.getKichThuoc()));
        row.put("loai", nameOf(spct.getLoai()));
        return row;
    }

    private Map<String, Object> toProductPreview(SanPham product, List<SanPhamChiTiet> variants) {
        List<SanPhamChiTiet> availableVariants = variants.stream()
                .filter(this::isVariantActive)
                .filter(v -> Optional.ofNullable(v.getSoLuong()).orElse(0) > 0)
                .collect(Collectors.toList());
        BigDecimal minPrice = availableVariants.stream()
                .map(SanPhamChiTiet::getGiaBan)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        int totalStock = availableVariants.stream().map(v -> Optional.ofNullable(v.getSoLuong()).orElse(0)).reduce(0, Integer::sum);

        Set<String> colors = new LinkedHashSet<>();
        Set<String> sizes = new LinkedHashSet<>();
        availableVariants.forEach(v -> {
            addIfText(colors, nameOf(v.getMauSac()));
            addIfText(sizes, nameOf(v.getKichThuoc()));
        });

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.getId());
        row.put("maSanPham", product.getMaSanPham());
        row.put("tenSanPham", product.getTenSanPham());
        row.put("moTa", product.getMoTa());
        row.put("giaTu", minPrice);
        row.put("tongTon", totalStock);
        row.put("mauSac", new ArrayList<>(colors));
        row.put("kichThuoc", new ArrayList<>(sizes));
        return row;
    }

    private Map<String, Object> toOrderPreview(HoaDon order) {
        OrderTotals totals = calculateOrderTotals(order);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("maHoaDon", order.getMaHoaDon());
        row.put("trangThai", order.getTrangThai());
        row.put("ngayTao", order.getNgayTao());
        row.put("thanhTien", order.getThanhTien());
        row.put("ngayNhanHangDuKien", order.getNgayNhanHangDuKien());
        row.put("phuongThucThanhToan", order.getPhuongThucThanhToan());
        row.put("tenKhachHang", order.getTenKhachHang());
        row.put("phiShip", totals.shippingFee);
        row.put("diaChiNhanHang", order.getDiaChiNhanHang());
        row.put("statusNote", order.getStatusNote());
        row.put("tamTinh", totals.subtotal);
        row.put("giamGia", totals.discount);
        row.put("tongThanhToan", totals.grandTotal);
        row.put("soMatHang", totals.itemCount);
        return row;
    }

    private List<SanPhamChiTiet> variantsOf(SanPham product) {
        if (product.getSanPhamChiTiets() != null && !product.getSanPhamChiTiets().isEmpty()) {
            return product.getSanPhamChiTiets();
        }
        return sanPhamChiTietRepository.findBySanPhamId(product.getId());
    }

    private boolean isTrackableOrder(HoaDon order) {
        String status = normalize(order.getTrangThai());
        if (status.isBlank()) {
            return false;
        }
        if (containsAny(status,
                "da giao", "đã giao", "hoan thanh", "hoàn thành",
                "da huy", "đã hủy", "huy", "hủy",
                "that bai", "thất bại", "khong thanh cong", "không thành công",
                "hoan tra", "hoàn trả", "return", "refunded")) {
            return false;
        }
        return containsAny(status,
                "cho xac nhan", "chờ xác nhận", "cho xu ly", "chờ xử lý",
                "dat mua", "đặt mua", "packed", "dong goi", "đóng gói",
                "cho lay hang", "chờ lấy hàng", "dang giao", "đang giao",
                "van chuyen", "vận chuyển", "giao hang", "giao hàng",
                "shipping", "transit", "in_transit", "picked_up", "cho giao");
    }

    private boolean isCompletedOrder(HoaDon order) {
        String status = normalize(order.getTrangThai());
        return containsAny(status, "da giao", "đã giao", "hoan thanh", "hoàn thành");
    }

    private OrderTotals calculateOrderTotals(HoaDon order) {
        List<HoaDonChiTiet> lines = order.getId() == null ? List.of() : hoaDonChiTietRepository.findByHoaDon_Id(order.getId());
        BigDecimal subtotal = lines.stream()
                .map(item -> item.getThanhTien() == null ? BigDecimal.ZERO : item.getThanhTien())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = order.getPhiShip() == null ? BigDecimal.ZERO : order.getPhiShip();
        BigDecimal grandTotal = order.getThanhTien() != null
                ? order.getThanhTien()
                : subtotal.add(shipping);
        BigDecimal discountBase = subtotal.add(shipping).subtract(grandTotal);
        BigDecimal discount = discountBase.compareTo(BigDecimal.ZERO) > 0 ? discountBase : BigDecimal.ZERO;
        int itemCount = lines.stream().map(HoaDonChiTiet::getSoLuong).filter(Objects::nonNull).reduce(0, Integer::sum);
        return new OrderTotals(subtotal, shipping, discount, grandTotal, itemCount);
    }


    private boolean isPaymentMethodActive(PTTT pttt) {
        String status = normalize(pttt.getTrangThai());
        return status.isBlank() || containsAny(status, "hoat dong", "hoạt động", "active", "hien", "hiện");
    }

    private boolean isActive(SanPham product) {
        if (product == null) return false;
        String status = normalize(product.getTrangThai());

        if (status.contains("ngung")
                || status.contains("khong hoat dong")
                || status.contains("inactive")
                || status.contains("disabled")
                || status.contains("tat")) {
            return false;
        }

        return status.isBlank()
                || status.contains("hoat dong")
                || status.contains("active");
    }

    private boolean isVariantActive(SanPhamChiTiet variant) {
        if (variant == null) return false;
        String status = normalize(variant.getTrangThai());

        if (status.contains("ngung")
                || status.contains("khong hoat dong")
                || status.contains("inactive")
                || status.contains("disabled")
                || status.contains("tat")) {
            return false;
        }

        return status.isBlank()
                || status.contains("hoat dong")
                || status.contains("active");
    }

    private String nameOf(Object entity) {
        if (entity == null) return "";
        try {
            return String.valueOf(entity.getClass().getMethod("getTenMau").invoke(entity));
        } catch (Exception ignored) {
        }
        try {
            return String.valueOf(entity.getClass().getMethod("getTenKichThuoc").invoke(entity));
        } catch (Exception ignored) {
        }
        try {
            return String.valueOf(entity.getClass().getMethod("getTenLoai").invoke(entity));
        } catch (Exception ignored) {
        }
        try {
            return String.valueOf(entity.getClass().getMethod("getTenDanhMuc").invoke(entity));
        } catch (Exception ignored) {
        }
        return "";
    }

    private void addIfText(Set<String> target, String value) {
        if (value != null && !value.isBlank()) target.add(value.trim());
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean containsAny(String text, String... values) {
        if (text == null || text.isBlank()) return false;
        for (String value : values) {
            if (value != null && !value.isBlank() && normalize(text).contains(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String s = text.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        s = s.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        s = s.replaceAll("[ìíịỉĩ]", "i");
        s = s.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        s = s.replaceAll("[ùúụủũưừứựửữ]", "u");
        s = s.replaceAll("[ỳýỵỷỹ]", "y");
        s = s.replaceAll("[đ]", "d");
        return s;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String digitsOnly(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("[^0-9]", "");
    }

    private static class OrderTotals {
        private final BigDecimal subtotal;
        private final BigDecimal shippingFee;
        private final BigDecimal discount;
        private final BigDecimal grandTotal;
        private final int itemCount;

        private OrderTotals(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discount, BigDecimal grandTotal, int itemCount) {
            this.subtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
            this.shippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
            this.discount = discount == null ? BigDecimal.ZERO : discount;
            this.grandTotal = grandTotal == null ? BigDecimal.ZERO : grandTotal;
            this.itemCount = itemCount;
        }
    }

    private static class ProductSalesAgg {
        private final SanPham product;
        private int quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductSalesAgg(SanPham product) {
            this.product = product;
        }

    }

    public List<Map<String, Object>> getPromotionsByProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return getActivePromotions();
        }

        LocalDateTime now = LocalDateTime.now();

        return khuyenMaiSanPhamRepository.findBySanPham_MaSanPhamIgnoreCase(productCode.trim())
                .stream()
                .filter(link -> link.getSanPham() != null && isActive(link.getSanPham()))
                .map(KhuyenMaiSanPham::getKhuyenMai)
                .filter(Objects::nonNull)
                .filter(km -> km.getNgayBatDau() == null || !km.getNgayBatDau().isAfter(now))
                .filter(km -> km.getNgayKetThuc() == null || !km.getNgayKetThuc().isBefore(now))
                .filter(km -> {
                    String status = normalize(km.getTrangThai());
                    return status.isBlank()
                            || status.contains("dang dien ra")
                            || status.contains("dang hoat dong")
                            || status.contains("hoat dong")
                            || status.contains("active");
                })
                .sorted(Comparator.comparing(KhuyenMai::getGiaTri, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(km -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ma", km.getMaKhuyenMai());
                    row.put("ten", km.getTenKhuyenMai());
                    row.put("giaTri", km.getGiaTri());
                    row.put("hinhThuc", km.getDonViGiam());
                    row.put("ngayBatDau", km.getNgayBatDau());
                    row.put("ngayKetThuc", km.getNgayKetThuc());
                    row.put("trangThai", km.getTrangThai());
                    return row;
                })
                .toList();
    }

    public List<Map<String, Object>> getPromotedProducts(int limit) {
        LocalDateTime now = LocalDateTime.now();

        return khuyenMaiSanPhamRepository.findAll().stream()
                .filter(link -> link.getSanPham() != null)
                .filter(link -> isActive(link.getSanPham()))
                .filter(link -> variantsOf(link.getSanPham()).stream()
                        .anyMatch(v -> isVariantActive(v) && Optional.ofNullable(v.getSoLuong()).orElse(0) > 0))
                .filter(link -> link.getKhuyenMai() != null)
                .filter(link -> {
                    KhuyenMai km = link.getKhuyenMai();
                    return (km.getNgayBatDau() == null || !km.getNgayBatDau().isAfter(now))
                            && (km.getNgayKetThuc() == null || !km.getNgayKetThuc().isBefore(now));
                })
                .filter(link -> {
                    String status = normalize(link.getKhuyenMai().getTrangThai());
                    return status.isBlank()
                            || status.contains("dang dien ra")
                            || status.contains("dang hoat dong")
                            || status.contains("hoat dong")
                            || status.contains("active");
                })
                .limit(Math.max(1, limit))
                .map(link -> {
                    SanPham sp = link.getSanPham();
                    KhuyenMai km = link.getKhuyenMai();

                    Map<String, Object> row = toProductPreview(sp, variantsOf(sp));
                    row.put("promotionCode", km.getMaKhuyenMai());
                    row.put("promotionName", km.getTenKhuyenMai());
                    row.put("discountValue", km.getGiaTri());
                    row.put("discountUnit", km.getDonViGiam());
                    row.put("promotionText", km.getTenKhuyenMai() + " - giảm " + formatDiscount(km.getGiaTri(), km.getDonViGiam()));
                    return row;
                })
                .collect(Collectors.toList());
    }

    private String formatDiscount(Object value, Object unit) {
        String unitText = safe(unit);
        String valueText = safe(value);

        try {
            BigDecimal amount = new BigDecimal(String.valueOf(value)).stripTrailingZeros();
            valueText = amount.toPlainString();
        } catch (Exception ignored) {
        }

        if (unitText.equalsIgnoreCase("PERCENT") || unitText.equals("%")) {
            return valueText + "%";
        }

        if (unitText.equalsIgnoreCase("VND") || unitText.equalsIgnoreCase("VNĐ") || unitText.equalsIgnoreCase("đ")) {
            return valueText + "đ";
        }

        return valueText + (unitText.isBlank() ? "" : " " + unitText);
    }
}
