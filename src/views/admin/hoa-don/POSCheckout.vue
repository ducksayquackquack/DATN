<script setup>
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { createHoaDon, addHoaDonItem, updateHoaDon, updateHoaDonBySystemEvent } from "../../../services/hoaDonService"
import { getAllSanPham } from "../../../services/sanPhamService"
import { getAllKhachHang } from "../../../services/KhachHangService"
import { getAllNhanVien, getNhanVienByTaiKhoanId } from "../../../services/nhanVienService"
import { ArrowLeft, Plus } from "lucide-vue-next"
import { appendPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
import { validateEmployeeActiveShift } from "../../../utils/shiftGuard"

const router = useRouter()
const route = useRoute()

// Xác định panel đang dùng: admin hay nhân viên
const panelBasePath = computed(() => route.path.startsWith('/employee/') ? '/employee' : '/admin')
const isEmployeePanel = computed(() => route.path.startsWith('/employee/'))

// Trạng thái tải / lưu
const loading = ref(false)
const saving = ref(false)
const currentEmployeeId = ref(null)

// Danh sách danh mục tải từ API
const nhanVienList = ref([])
const khachHangList = ref([])
const variants = ref([])   // danh sách biến thể (màu + size) đã làm phẳng

// Form thông tin đơn hàng
const cashierId = ref(null)
const customerId = ref(null)
const paymentMethod = ref("CASH")
const orderNote = ref("")
const discount = ref(0)
const selectedVoucher = ref(null)


// Form thêm sản phẩm
const searchKeyword = ref("")
const selectedSpctId = ref(null)
const selectedQty = ref(1)
const lines = ref([])   // các dòng sản phẩm trong đơn

// ─── Helpers ─────────────────────────────────────────────────────────────────

// Chuẩn hóa response API về mảng (hỗ trợ nhiều cấu trúc trả về)
const toList = (value) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  if (Array.isArray(value?.data)) return value.data
  if (Array.isArray(value?.data?.content)) return value.data.content
  return []
}

// Format tiền tệ VND: 150000 → "150.000₫"
const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"

// Làm phẳng danh sách sản phẩm thành danh sách biến thể (1 hàng = 1 màu + size)
const flattenVariants = (products) =>
  products.flatMap((product) =>
    (product?.sanPhamChiTiets ?? []).map((v) => ({
      spctId: v.id,
      maSanPham: product.maSanPham || "",
      maSanPhamChiTiet: v.ma || "",
      tenSanPham: product.tenSanPham || "Sản phẩm",
      tenMau: v?.mauSac?.tenMau || "",
      tenSize: v?.kichThuoc?.tenKichThuoc || "",
      giaBan: Number(v?.giaBan || 0),
      soLuongTon: Number(v?.soLuong || 0)
    }))
  )

// ─── Computed ─────────────────────────────────────────────────────────────────

// Lọc biến thể theo từ khóa tìm kiếm (mã SP, tên, màu, size)
const filteredVariants = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return variants.value
  return variants.value.filter((v) =>
    [v.maSanPham, v.maSanPhamChiTiet, v.tenSanPham, v.tenMau, v.tenSize]
      .join(" ").toLowerCase().includes(kw)
  )
})

const selectedVariant = computed(() =>
  variants.value.find((v) => Number(v.spctId) === Number(selectedSpctId.value)) ?? null
)

const subtotal = computed(() =>
  lines.value.reduce((sum, l) => sum + Number(l.giaBan || 0) * Number(l.soLuong || 0), 0)
)

const grandTotal = computed(() => Math.max(subtotal.value - Number(discount.value || 0), 0))

// Trạng thái ngay sau khi tạo đơn:
//   VNPay  → CHO_LAY_HANG (chờ xác nhận thanh toán online trước khi hoàn tất)
//   Còn lại → HOAN_THANH  (thu tiền ngay tại quầy, xong luôn)
const defaultStatusCode = computed(() =>
  paymentMethod.value.toUpperCase() === "VNPAY" ? "CHO_LAY_HANG" : "HOAN_THANH"
)

// ─── Xác định nhân viên đăng nhập ────────────────────────────────────────────

/**
 * Trả về ID nhân viên của người dùng hiện tại.
 * Thứ tự ưu tiên:
 *   1. Dữ liệu user đã lưu trong localStorage / sessionStorage
 *   2. Gọi API theo ID tài khoản
 *   3. Dò trong danh sách nhân viên đã tải
 */
const resolveCurrentEmployeeContext = async () => {
  try {
    const parsed = JSON.parse(localStorage.getItem("user") || sessionStorage.getItem("user") || "null")
    if (parsed?.idNhanVien) return Number(parsed.idNhanVien)
    if (parsed?.id && parsed?.tenNhanVien) return Number(parsed.id)
  } catch { /* tiếp tục fallback */ }

  const taiKhoanId = Number(localStorage.getItem("userId") || 0)
  if (taiKhoanId > 0) {
    try {
      const res = await getNhanVienByTaiKhoanId(taiKhoanId)
      const first = Array.isArray(res?.data) ? res.data[0] : res?.data
      if (first?.id) return Number(first.id)
    } catch { /* tiếp tục fallback */ }

    const mapped = nhanVienList.value.find(
      (nv) => Number(nv?.idTaiKhoan || nv?.taiKhoan?.id || 0) === taiKhoanId
    )
    if (mapped?.id) return Number(mapped.id)
  }

  return null
}

// Nhân viên phải đang trong ca hợp lệ mới được tạo đơn
const canOperateForEmployeeShift = async (employeeId) => {
  // TEMP DEMO MODE: turn off shift validation so all employees can operate.
  // const check = await validateEmployeeActiveShift(employeeId)
  // if (!check.allowed) {
  //   window.toast?.warning?.(check.reason || "Nhân viên chưa trong ca trực hợp lệ")
  //   return false
  // }
  return true
}

// ─── Thao tác giỏ hàng ───────────────────────────────────────────────────────

const applyDiscount = (amount) => { discount.value = Number(amount || 0) }

const normalizedPhone = (value) => String(value || "").replace(/\s+/g, "")

const isValidVietnamPhone = (value) => /^(0|\+84)\d{9,10}$/.test(normalizedPhone(value))

const resolveCustomerIdFromResponse = (response) => {
  const candidates = [
    response?.data?.id,
    response?.data?.data?.id,
    response?.data?.khachHang?.id,
    response?.data?.content?.id
  ]
  const found = candidates.find((id) => Number(id) > 0)
  return found ? Number(found) : null
}

// Thêm biến thể vào đơn; nếu đã có → cộng dồn số lượng
const addLine = () => {
  if (!selectedVariant.value) {
    window.toast?.warning?.("Vui lòng chọn biến thể sản phẩm")
    return
  }
  const qty = Number(selectedQty.value || 0)
  if (!Number.isFinite(qty) || qty <= 0) {
    window.toast?.warning?.("Số lượng không hợp lệ")
    return
  }
  const stock = Number(selectedVariant.value.soLuongTon || 0)
  const existed = lines.value.find((l) => Number(l.spctId) === Number(selectedVariant.value.spctId))
  const nextQty = (existed?.soLuong ?? 0) + qty
  if (nextQty > stock) {
    window.toast?.warning?.("Số lượng vượt tồn kho")
    return
  }
  if (existed) existed.soLuong = nextQty
  else lines.value.push({ ...selectedVariant.value, soLuong: qty })
  selectedQty.value = 1
}

const removeLine = (index) => lines.value.splice(index, 1)

// ─── Tạo đơn bán tại quầy (POS) ──────────────────────────────────────────────

/**
 * LUỒNG TẠO ĐƠN POS:
 *
 *  Bước 1 — Validate:
 *    - Panel nhân viên: tự gán cashierId từ đăng nhập
 *    - Kiểm tra nhân viên đang trong ca trực hợp lệ
 *    - Phải có ít nhất 1 sản phẩm trong đơn
 *
 *  Bước 2 — Tạo hóa đơn nháp:
 *    - Gọi createHoaDon() với orderType "POS", statusCode "CHO_LAY_HANG"
 *
 *  Bước 3 — Thêm sản phẩm:
 *    - Gọi addHoaDonItem() cho từng dòng trong giỏ
 *
 *  Bước 4 — Cập nhật thông tin & ghi chú thanh toán:
 *    - Gọi updateHoaDon() với tổng tiền, giảm giá, ghi chú
 *    - VNPay: gắn tag PAYMENT_FLOW để nhân viên biết cần xác nhận thêm
 *
 *  Bước 5 — Chuyển trạng thái cuối:
 *    - Tiền mặt / Chuyển khoản → gọi HOAN_TAT_POS → trạng thái HOAN_THANH
 *    - VNPay → giữ nguyên CHO_LAY_HANG, chờ khách xác nhận thanh toán
 *
 *  Bước 6 — Điều hướng đến trang chi tiết hóa đơn vừa tạo
 */
const submitPosOrder = async () => {
  // Bước 1: validate
  if (isEmployeePanel.value) {
    if (!currentEmployeeId.value) { window.toast?.error?.("Không xác định được nhân viên đăng nhập"); return }
    cashierId.value = Number(currentEmployeeId.value)
  }
  if (!cashierId.value) { window.toast?.warning?.("Vui lòng chọn nhân viên bán hàng"); return }
  // TEMP DEMO MODE: disable shift guard.
  // if (!await canOperateForEmployeeShift(cashierId.value)) return
  if (!lines.value.length) { window.toast?.warning?.("Đơn bán tại quầy phải có ít nhất 1 sản phẩm"); return }

  saving.value = true
  try {
    const selectedCustomer = khachHangList.value.find((kh) => Number(kh.id) === Number(customerId.value)) ?? null

    // Bước 2: tạo hóa đơn nháp
    const createRes = await createHoaDon({
      nhanVienId: Number(cashierId.value),
      khachHangId: selectedCustomer?.id ?? null,
      soDienThoaiNhanHang: selectedCustomer?.soDienThoai || "",
      diaChiNhanHang: "Mua tại quầy",
      phiShip: 0,
      phuongThucThanhToan: paymentMethod.value,
      orderType: "POS"
    })
    const orderId = createRes?.data?.hoaDon?.id ?? createRes?.data?.id
    if (!orderId) throw new Error("Không lấy được mã hóa đơn bán tại quầy")

    // Bước 3: thêm từng sản phẩm (validate tồn kho lần cuối trước khi gửi)
    for (const line of lines.value) {
      const stockCap = Number(line.soLuongTon || 0)
      const qty = Number(line.soLuong || 0)
      if (stockCap > 0 && qty > stockCap) {
        window.toast?.error?.(`Sản phẩm "${line.tenSanPham || line.maSanPhamChiTiet}" vượt tồn kho (${qty} > ${stockCap})`)
        saving.value = false
        return
      }
      await addHoaDonItem(orderId, { spctId: line.spctId, soLuong: qty, giaBan: Number(line.giaBan) })
    }

    const isVnpay = paymentMethod.value.toUpperCase() === "VNPAY"

    // Bước 4: cập nhật thông tin & ghi chú
    try {
      await updateHoaDon(orderId, {
        nhanVienId: Number(cashierId.value),
        khachHangId: selectedCustomer?.id ?? null,
        soDienThoaiNhanHang: selectedCustomer?.soDienThoai || "",
        diaChiNhanHang: "Mua tại quầy",
        phiShip: 0,
        giaSauGiamGia: Number(discount.value || 0),
        thanhTien: Number(grandTotal.value || 0),
        phuongThucThanhToan: paymentMethod.value,
        orderType: "POS",
        // VNPay: gắn tag để hệ thống biết nhân viên đã xác nhận, chờ khách thanh toán
        statusNote: isVnpay
          ? appendPaymentFlowTag(
              `[POS] ${orderNote.value || "Đơn bán tại quầy"}`,
              PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED,
              "Nhân viên thu ngân đã xác nhận thanh toán VNPay tại quầy"
            )
          : `[POS] ${orderNote.value || "Đơn bán tại quầy"}`
      })
    } catch (updateErr) {
      console.warn("updateHoaDon failed, retrying without extra fields:", updateErr)
      try {
        await updateHoaDon(orderId, {
          nhanVienId: Number(cashierId.value),
          khachHangId: selectedCustomer?.id ?? null,
          phuongThucThanhToan: paymentMethod.value,
          statusNote: `[POS] ${orderNote.value || "Đơn bán tại quầy"}`
        })
      } catch { /* order was already created, continue */ }
    }

    // Bước 5: chuyển trạng thái cuối
    if (!isVnpay) {
      // Tiền mặt / Chuyển khoản → hoàn tất ngay
      try {
        const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: createRes?.data?.hoaDon?.maHoaDon || createRes?.data?.maHoaDon })
        await updateHoaDonBySystemEvent(orderId, "HOAN_TAT_POS", "Đã hoàn tất bán hàng tại quầy", trackingUrl)
        window.toast?.success?.("Tạo đơn bán tại quầy thành công")
      } catch {
        window.toast?.warning?.("Đơn đã tạo nhưng chưa hoàn tất — vào chi tiết bấm 'Hoàn tất bán hàng tại quầy'")
      }
    } else {
      // VNPay → giữ CHO_LAY_HANG, chờ khách xác nhận thanh toán
      window.toast?.success?.("Tạo đơn bán tại quầy thành công — chờ khách xác nhận VNPay")
    }

    // Bước 6: chuyển đến trang chi tiết
    router.push(`${panelBasePath.value}/hoa-don/detail/${orderId}`)
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error.message || "Không thể tạo đơn bán tại quầy")
  } finally {
    saving.value = false
  }
}

// ─── Tải dữ liệu ban đầu ──────────────────────────────────────────────────────

const loadData = async () => {
  loading.value = true
  try {
    // Tải song song: nhân viên, khách hàng, sản phẩm
    const [nvRes, khRes, spRes] = await Promise.all([
      getAllNhanVien(),
      getAllKhachHang(0, 200),
      getAllSanPham()
    ])
    nhanVienList.value = toList(nvRes?.data)
    khachHangList.value = toList(khRes?.data)
    variants.value = flattenVariants(toList(spRes?.data))

    // Panel nhân viên: tự động xác định cashierId từ thông tin đăng nhập
    if (isEmployeePanel.value) {
      currentEmployeeId.value = await resolveCurrentEmployeeContext()
      if (!currentEmployeeId.value) throw new Error("Không xác định được nhân viên đăng nhập")
      cashierId.value = Number(currentEmployeeId.value)
    }

    // Chọn mặc định để form không trống khi mới vào
    if (!cashierId.value && nhanVienList.value.length) cashierId.value = Number(nhanVienList.value[0].id)
    if (!selectedSpctId.value && variants.value.length) selectedSpctId.value = variants.value[0].spctId
  } catch (error) {
    window.toast?.error?.(error?.message || "Không thể tải dữ liệu bán hàng tại quầy")
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <main class="wrap pos-wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Bán hàng tại quầy</h1>
          <small class="muted">Luồng thu ngân: chọn sản phẩm, chọn khách, nhận thanh toán, tạo hóa đơn ngay.</small>
        </div>
        <button class="btn" type="button" @click="router.push(`${panelBasePath}/hoa-don/list`)">
          <ArrowLeft :size="16" />
          <span>Quay lại hóa đơn</span>
        </button>
      </div>

      <div class="body">
        <section v-if="!loading" class="pos-layout">
          <div class="pos-main-column">
            <article class="panel">
              <div class="panel-head">
                <h2>Thông tin đơn bán tại quầy</h2>
              </div>
              <div class="panel-body">
                <div class="form-grid">
                  <div class="field">
                    <label>Nhân viên bán hàng</label>
                    <select v-model.number="cashierId" :disabled="isEmployeePanel">
                      <option :value="null">Chọn nhân viên</option>
                      <option v-for="nv in nhanVienList" :key="nv.id" :value="Number(nv.id)">
                        {{ nv.tenNhanVien || `NV #${nv.id}` }}
                      </option>
                    </select>
                  </div>

                  <div class="field">
                    <label>Khách hàng (tùy chọn)</label>
                    <select v-model.number="customerId">
                      <option :value="null">Khách lẻ</option>
                      <option v-for="kh in khachHangList" :key="kh.id" :value="Number(kh.id)">
                        {{ kh.tenKhachHang || `KH #${kh.id}` }}
                      </option>
                    </select>

                  </div>

                  <div class="field">
                    <label>Phương thức thanh toán</label>
                    <select v-model="paymentMethod">
                      <option value="CASH">Tiền mặt</option>
                      <option value="BANK">Chuyển khoản</option>
                      <option value="VNPAY">VNPay</option>
                    </select>
                  </div>

                  <div class="field field-full">
                    <label>Giảm giá (voucher)</label>
                    <VoucherSelector
                      :subtotal="subtotal"
                      :customer-id="customerId"
                      :auto-select="true"
                      @update:voucher="selectedVoucher = $event"
                      @discount-changed="applyDiscount"
                    />
                  </div>

                  <div class="field field-full">
                    <label>Ghi chú thu ngân</label>
                    <input v-model="orderNote" type="text" placeholder="Ví dụ: Mua trực tiếp tại quầy tầng 1" />
                  </div>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-head">
                <h2>Thêm sản phẩm</h2>
              </div>
              <div class="panel-body">
                <div class="field field-full search-field">
                  <label>Tìm nhanh biến thể</label>
                  <input v-model="searchKeyword" type="text" placeholder="Tìm theo mã SP, mã SPCT, tên sản phẩm..." />
                </div>

                <div class="add-row">
                  <div class="field">
                    <label>Biến thể sản phẩm</label>
                    <select v-model.number="selectedSpctId">
                      <option :value="null">Chọn biến thể sản phẩm</option>
                      <option v-for="v in filteredVariants" :key="v.spctId" :value="v.spctId">
                        {{ v.maSanPham }} / {{ v.maSanPhamChiTiet }} - {{ v.tenSanPham }} - {{ v.tenMau }} {{ v.tenSize }} - {{ formatCurrency(v.giaBan) }} - Tồn {{ v.soLuongTon }}
                      </option>
                    </select>
                  </div>
                  <div class="field qty-field">
                    <label>Số lượng</label>
                    <input v-model.number="selectedQty" type="number" min="1" />
                  </div>
                  <div class="field action-field">
                    <label>&nbsp;</label>
                    <button class="btn" type="button" @click="addLine">
                      <Plus :size="16" />
                      <span>Thêm dòng</span>
                    </button>
                  </div>
                </div>

                <div class="table-wrap">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>Sản phẩm</th>
                        <th class="right">Đơn giá</th>
                        <th class="center">SL</th>
                        <th class="right">Thành tiền</th>
                        <th class="center">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="!lines.length">
                        <td colspan="5" class="table-empty">Chưa có sản phẩm trong đơn bán tại quầy</td>
                      </tr>
                      <tr v-for="(line, idx) in lines" :key="line.spctId">
                        <td>
                          <div class="line-title">{{ line.tenSanPham }}</div>
                          <div class="line-meta">{{ line.maSanPham }} / {{ line.maSanPhamChiTiet }} - {{ line.tenMau }} {{ line.tenSize }}</div>
                        </td>
                        <td class="right">{{ formatCurrency(line.giaBan) }}</td>
                        <td class="center">{{ line.soLuong }}</td>
                        <td class="right">{{ formatCurrency(line.soLuong * line.giaBan) }}</td>
                        <td class="center">
                          <button class="btn danger btn-sm" type="button" @click="removeLine(idx)">Xóa</button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </article>
          </div>

          <aside class="panel summary-panel">
            <div class="panel-head">
              <h2>Tổng kết đơn</h2>
            </div>
            <div class="panel-body summary-body">
              <div class="summary-row"><span>Loại đơn</span><strong>Bán hàng tại quầy</strong></div>
              <div class="summary-row"><span>Trạng thái sau tạo</span><strong>{{ defaultStatusCode }}</strong></div>
              <div class="summary-row"><span>Tạm tính</span><strong>{{ formatCurrency(subtotal) }}</strong></div>
              <div class="summary-row"><span>Giảm giá</span><strong>-{{ formatCurrency(discount) }}</strong></div>
              <div class="summary-row total"><span>Tổng thu</span><strong>{{ formatCurrency(grandTotal) }}</strong></div>

              <button class="btn primary checkout-btn" type="button" :disabled="saving || !lines.length" @click="submitPosOrder">
                {{ saving ? 'Đang tạo đơn...' : 'Hoàn tất bán hàng tại quầy' }}
              </button>
            </div>
          </aside>
        </section>

        <section v-else class="loading-state">Đang tải dữ liệu bán hàng tại quầy...</section>
      </div>
    </div>
  </main>
</template>

<style scoped>
.pos-wrap {
  padding-top: 28px;
}

.pos-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 20px;
  align-items: start;
}

.pos-main-column {
  display: grid;
  gap: 20px;
}

.panel {
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fff;
  overflow: hidden;
}

.panel-head {
  padding: 18px 22px;
  border-bottom: 1px solid var(--line);
}

.panel-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.panel-body {
  padding: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  margin: 0;
}

.field-full {
  grid-column: 1 / -1;
}

.search-field {
  margin-bottom: 14px;
}

.add-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px 150px;
  gap: 12px;
  align-items: end;
}

.qty-field,
.action-field {
  min-width: 0;
}

.action-field .btn {
  width: 100%;
}

.table-wrap {
  margin-top: 18px;
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.line-title {
  font-weight: 600;
  color: var(--text);
}

.line-meta {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.table-empty {
  text-align: center;
  color: #9ca3af;
  padding: 24px 16px;
}

.summary-panel {
  position: sticky;
  top: 88px;
}

.summary-body {
  display: grid;
  gap: 10px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: baseline;
  color: #334155;
}

.summary-row strong {
  color: var(--text);
}

.summary-row.total {
  margin-top: 6px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
  font-size: 18px;
}

.checkout-btn {
  margin-top: 10px;
  width: 100%;
}

.btn-sm {
  min-height: 34px;
  padding: 8px 10px;
  font-size: 13px;
}

.quick-customer {
  margin-top: 8px;
}

.quick-customer-form {
  margin-top: 10px;
  display: grid;
  gap: 8px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  padding: 10px;
}

.quick-customer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.loading-state {
  padding: 14px 4px;
  color: #475569;
  font-weight: 500;
}

@media (max-width: 900px) {
  .pos-layout {
    grid-template-columns: 1fr;
  }

  .summary-panel {
    position: static;
  }

  .form-grid,
  .add-row {
    grid-template-columns: 1fr;
  }

  .panel-body {
    padding: 18px;
  }
}
</style>
