<script setup>
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { createHoaDon, addHoaDonItem, updateHoaDon, updateHoaDonBySystemEvent } from "../../../services/hoaDonService"
import { getAllSanPham } from "../../../services/sanPhamService"
import { getAllKhachHang } from "../../../services/KhachHangService"
import { getAllNhanVien, getNhanVienByTaiKhoanId } from "../../../services/nhanVienService"
import { ArrowLeft, Plus } from "lucide-vue-next"
import { appendPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
import { validateEmployeeActiveShift } from "../../../utils/shiftGuard"

const router = useRouter()
const route = useRoute()
const panelBasePath = computed(() => (route.path.startsWith('/employee/') ? '/employee' : '/admin'))
const isEmployeePanel = computed(() => route.path.startsWith('/employee/'))

const loading = ref(false)
const saving = ref(false)
const currentEmployeeId = ref(null)

const nhanVienList = ref([])
const khachHangList = ref([])
const variants = ref([])

const cashierId = ref(null)
const customerId = ref(null)
const paymentMethod = ref("CASH")
const orderNote = ref("")
const discount = ref(0)
const selectedVoucher = ref(null)

const applyDiscount = (amount) => { discount.value = Number(amount || 0) }

const searchKeyword = ref("")
const selectedSpctId = ref(null)
const selectedQty = ref(1)

const lines = ref([])

const toList = (value) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  if (Array.isArray(value?.data)) return value.data
  if (Array.isArray(value?.data?.content)) return value.data.content
  return []
}

const resolveCurrentEmployeeContext = async () => {
  const storedUserRaw = localStorage.getItem("user") || sessionStorage.getItem("user")
  if (storedUserRaw) {
    try {
      const parsed = JSON.parse(storedUserRaw)
      if (parsed?.idNhanVien) return Number(parsed.idNhanVien)
      if (parsed?.id && parsed?.tenNhanVien) return Number(parsed.id)
    } catch {
      // Continue fallback checks.
    }
  }

  const taiKhoanId = Number(localStorage.getItem("userId") || 0)
  if (taiKhoanId > 0) {
    try {
      const byTaiKhoan = await getNhanVienByTaiKhoanId(taiKhoanId)
      const fromApi = byTaiKhoan?.data
      if (Array.isArray(fromApi) && fromApi[0]?.id) return Number(fromApi[0].id)
      if (fromApi?.id) return Number(fromApi.id)
    } catch {
      // Continue fallback checks.
    }

    const mapped = nhanVienList.value.find((item) => {
      const mappedTaiKhoanId = Number(item?.idTaiKhoan || item?.taiKhoan?.id || 0)
      return mappedTaiKhoanId === taiKhoanId
    })
    if (mapped?.id) return Number(mapped.id)
  }

  return null
}

const canOperateForEmployeeShift = async (employeeId) => {
  const check = await validateEmployeeActiveShift(employeeId)
  if (!check.allowed) {
    window.toast?.warning?.(check.reason || "Nhân viên chưa trong ca trực hợp lệ")
    return false
  }
  return true
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"
}

const flattenVariants = (products) => {
  return products.flatMap((product) => {
    const spct = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
    return spct.map((variant) => ({
      spctId: variant.id,
      maSanPham: product.maSanPham || "",
      maSanPhamChiTiet: variant.ma || "",
      tenSanPham: product.tenSanPham || "Sản phẩm",
      tenMau: variant?.mauSac?.tenMau || "",
      tenSize: variant?.kichThuoc?.tenKichThuoc || "",
      giaBan: Number(variant?.giaBan || 0),
      soLuongTon: Number(variant?.soLuong || 0)
    }))
  })
}

const filteredVariants = computed(() => {
  const keyword = String(searchKeyword.value || "").trim().toLowerCase()
  if (!keyword) return variants.value
  return variants.value.filter((item) => {
    return [
      item.maSanPham,
      item.maSanPhamChiTiet,
      item.tenSanPham,
      item.tenMau,
      item.tenSize
    ].join(" ").toLowerCase().includes(keyword)
  })
})

const selectedVariant = computed(() => {
  return variants.value.find((item) => Number(item.spctId) === Number(selectedSpctId.value)) || null
})

const subtotal = computed(() => {
  return lines.value.reduce((sum, line) => sum + Number(line.giaBan || 0) * Number(line.soLuong || 0), 0)
})

const grandTotal = computed(() => {
  return Math.max(subtotal.value - Number(discount.value || 0), 0)
})

const defaultStatusCode = computed(() => {
  const method = String(paymentMethod.value || "").toUpperCase()
  if (method === "VNPAY") return "CHO_LAY_HANG"
  return "HOAN_THANH"
})

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

  if (qty > selectedVariant.value.soLuongTon) {
    window.toast?.warning?.("Số lượng vượt tồn kho")
    return
  }

  const existed = lines.value.find((line) => Number(line.spctId) === Number(selectedVariant.value.spctId))
  if (existed) {
    const nextQty = existed.soLuong + qty
    if (nextQty > selectedVariant.value.soLuongTon) {
      window.toast?.warning?.("Số lượng vượt tồn kho")
      return
    }
    existed.soLuong = nextQty
  } else {
    lines.value.push({
      ...selectedVariant.value,
      soLuong: qty
    })
  }

  selectedQty.value = 1
}

const removeLine = (index) => {
  lines.value.splice(index, 1)
}

const submitPosOrder = async () => {
  if (isEmployeePanel.value) {
    if (!currentEmployeeId.value) {
      window.toast?.error?.("Không xác định được nhân viên đăng nhập")
      return
    }
    cashierId.value = Number(currentEmployeeId.value)
  }

  if (!cashierId.value) {
    window.toast?.warning?.("Vui lòng chọn nhân viên bán hàng")
    return
  }

  const canOperate = await canOperateForEmployeeShift(cashierId.value)
  if (!canOperate) return

  if (!lines.value.length) {
    window.toast?.warning?.("Đơn bán tại quầy phải có ít nhất 1 sản phẩm")
    return
  }

  saving.value = true
  try {
    const selectedCustomer = khachHangList.value.find((item) => Number(item.id) === Number(customerId.value)) || null

    const createRes = await createHoaDon({
      nhanVienId: Number(cashierId.value),
      khachHangId: selectedCustomer ? Number(selectedCustomer.id) : null,
      soDienThoaiNhanHang: selectedCustomer?.soDienThoai || "",
      diaChiNhanHang: "Mua tại quầy",
      phiShip: 0,
      phuongThucThanhToan: paymentMethod.value,
      orderType: "POS",
      orderStatusCode: "CHO_LAY_HANG"
    })

    const orderId = createRes?.data?.hoaDon?.id || createRes?.data?.id
    if (!orderId) throw new Error("Không lấy được mã hóa đơn bán tại quầy")

    for (const line of lines.value) {
      await addHoaDonItem(orderId, {
        spctId: line.spctId,
        soLuong: Number(line.soLuong),
        giaBan: Number(line.giaBan)
      })
    }

    const isVnpay = String(paymentMethod.value || "").toUpperCase() === "VNPAY"
    await updateHoaDon(orderId, {
      nhanVienId: Number(cashierId.value),
      khachHangId: selectedCustomer ? Number(selectedCustomer.id) : null,
      soDienThoaiNhanHang: selectedCustomer?.soDienThoai || "",
      diaChiNhanHang: "Mua tại quầy",
      phiShip: 0,
      giaSauGiamGia: Number(discount.value || 0),
      thanhTien: Number(grandTotal.value || 0),
      phuongThucThanhToan: paymentMethod.value,
      orderType: "POS",
      statusNote: isVnpay
        ? appendPaymentFlowTag(
          `[POS] ${orderNote.value || "Đơn bán tại quầy"}`,
          PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED,
          "Nhân viên thu ngân đã xác nhận thanh toán VNPay tại quầy"
        )
        : `[POS] ${orderNote.value || "Đơn bán tại quầy"}`
    })

    if (!isVnpay) {
      try {
        await updateHoaDonBySystemEvent(orderId, "HOAN_TAT_POS", "Đã hoàn tất bán hàng tại quầy")
        window.toast?.success?.("Tạo đơn bán tại quầy thành công")
      } catch {
        window.toast?.warning?.("Đơn đã tạo nhưng chưa hoàn tất — vào chi tiết bấm 'Hoàn tất bán hàng tại quầy'")
      }
    } else {
      window.toast?.success?.("Tạo đơn bán tại quầy thành công")
    }
    router.push(`${panelBasePath.value}/hoa-don/detail/${orderId}`)
  } catch (error) {
    console.error("Counter-sale submit failed:", error)
    window.toast?.error?.(error?.response?.data?.message || error.message || "Không thể tạo đơn bán tại quầy")
  } finally {
    saving.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [nvRes, khRes, spRes] = await Promise.all([
      getAllNhanVien(),
      getAllKhachHang(0, 200),
      getAllSanPham()
    ])

    nhanVienList.value = toList(nvRes?.data)
    khachHangList.value = toList(khRes?.data)
    variants.value = flattenVariants(toList(spRes?.data))

    if (isEmployeePanel.value) {
      currentEmployeeId.value = await resolveCurrentEmployeeContext()
      if (!currentEmployeeId.value) {
        throw new Error("Không xác định được nhân viên đăng nhập")
      }
      cashierId.value = Number(currentEmployeeId.value)
    }

    if (!cashierId.value && nhanVienList.value.length) {
      cashierId.value = Number(nhanVienList.value[0].id)
    }

    if (!selectedSpctId.value && variants.value.length) {
      selectedSpctId.value = variants.value[0].spctId
    }
  } catch (error) {
    console.error("Load counter-sale data failed:", error)
    window.toast?.error?.("Không thể tải dữ liệu bán hàng tại quầy")
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
