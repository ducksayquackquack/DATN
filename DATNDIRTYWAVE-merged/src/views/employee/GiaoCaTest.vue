<template>
  <div class="gct-page">

    <!-- LOADING OVERLAY -->
    <div v-if="loading" class="gct-loading">
      <div class="gct-spinner"></div>
      <span>Đang tải dữ liệu...</span>
    </div>

    <!-- HANDOVER REMINDER MODAL -->
    <div v-if="showHandoverReminder" class="gct-overlay">
      <div class="gct-reminder">
        <h3>Đã tới giờ bàn giao ca</h3>
        <p>Ca của bạn đã tới thời điểm kết thúc nhưng chưa xác nhận bàn giao. Vui lòng hoàn tất bàn giao ca để tránh lệch số liệu.</p>
        <div class="gct-reminder-btns">
          <button class="r-btn-later" @click="dismissHandoverReminder">Để sau</button>
          <button class="r-btn-now" @click="showHandoverReminder = false">Bàn giao ngay</button>
        </div>
      </div>
    </div>

    <!-- 24H OVERDUE WARNING -->
    <div v-if="is24hOverdue && caHienTai" class="gct-overdue-banner">
      <i class="fa-solid fa-triangle-exclamation"></i>
      <span>Ca đã mở quá <b>24 giờ</b> ({{ shiftDurationText }}). Vui lòng đóng ca ngay để tránh sai lệch số liệu.</span>
    </div>

    <!-- PHIEU THU/CHI MODAL -->
    <div v-if="showPhieuModal" class="gct-overlay" @click.self="showPhieuModal = false">
      <div class="gct-phieu-modal">
        <div class="gct-phieu-modal-header">
          <h3>{{ phieuModalType === 'THU' ? 'Tạo phiếu thu' : 'Tạo phiếu chi' }}</h3>
          <button class="gct-phieu-modal-close" @click="showPhieuModal = false">&times;</button>
        </div>
        <div class="gct-phieu-modal-body">
          <div class="gct-field">
            <label class="gct-label">Phương thức <span class="req">*</span></label>
            <select v-model="phieuForm.phuongThuc" class="gct-input gct-select">
              <option value="TIEN_MAT">Tiền mặt</option>
              <option value="CHUYEN_KHOAN">Chuyển khoản</option>
              <option value="THE">Thẻ</option>
            </select>
          </div>
          <div class="gct-field">
            <label class="gct-label">Số tiền <span class="req">*</span></label>
            <input
              type="text"
              class="gct-input"
              :value="phieuForm.soTien > 0 ? formatNumber(phieuForm.soTien) : ''"
              @input="onPhieuMoneyInput"
              placeholder="Nhập số tiền"
            />
          </div>
          <div class="gct-field">
            <label class="gct-label">Lý do</label>
            <input
              type="text"
              class="gct-input"
              v-model="phieuForm.lyDo"
              :placeholder="phieuModalType === 'THU' ? 'VD: Nhận tiền trả nợ...' : 'VD: Mua vật tư, nộp tiền...'"
            />
          </div>
        </div>
        <div class="gct-phieu-modal-footer">
          <button class="btn-dang-xuat" @click="showPhieuModal = false">Hủy</button>
          <button class="btn-mo-ca" @click="handleCreatePhieu" :disabled="phieuSubmitting">
            {{ phieuSubmitting ? 'Đang tạo...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ─────────────────── NO ACTIVE SHIFT ─────────────────── -->
    <template v-if="!loading && !caHienTai">

      <!-- Has schedule → Open shift form -->
      <div v-if="lichHomNay" class="gct-center">
        <div class="gct-mo-ca">
          <div class="gct-mo-ca-header">
            <h2>Mở ca làm việc</h2>
          </div>
          <p class="gct-mo-ca-desc">
            Vui lòng mở ca làm việc mới để có thể thực hiện được các chức năng dành cho nhân viên thu ngân
          </p>

          <div class="gct-mo-ca-form">
            <div class="gct-field">
              <label class="gct-label">Nhân viên ca <span class="req">*</span></label>
              <input
                type="text"
                class="gct-input gct-input-readonly"
                readonly
                :value="currentUser?.tenNhanVien || currentUser?.hoTen || 'Nhân viên'"
              />
            </div>

            <div class="gct-field">
              <label class="gct-label">Giờ bắt đầu</label>
              <input
                type="text"
                class="gct-input gct-input-readonly"
                readonly
                :value="formattedFullTime"
              />
            </div>

            <div class="gct-field">
              <label class="gct-label">Tiền mặt đầu ca <span class="req">*</span></label>
              <input
                type="text"
                class="gct-input"
                :value="tienBanDauInput > 0 ? formatNumber(tienBanDauInput) : ''"
                @input="onInputMoney($event, 'start')"
                placeholder="Nhập số tiền mặt đầu ca..."
              />
            </div>

            <div class="gct-field">
              <label class="gct-label">Ghi chú</label>
              <input
                type="text"
                class="gct-input"
                v-model="ghiChuBanDau"
                placeholder="Nhập ghi chú (không bắt buộc)"
              />
            </div>

            <div v-if="errorMessage" class="gct-error-msg">
              <i class="fa-solid fa-circle-exclamation"></i>
              {{ errorMessage }}
            </div>
          </div>

          <div class="gct-mo-ca-footer">
            <button class="btn-dang-xuat" @click="handleDangXuat">Đăng xuất</button>
            <button class="btn-mo-ca" @click="handleBatDauCa" :disabled="isSubmitting || !canOpenShift">
              <i v-if="isSubmitting" class="fa-solid fa-spinner fa-spin"></i>
              {{ !canOpenShift ? 'Không có quyền mở ca' : (isSubmitting ? 'Đang xử lý...' : 'Mở ca') }}
            </button>
          </div>
        </div>
      </div>

      <!-- No schedule today -->
      <div v-else class="gct-center">
        <div class="gct-no-shift">
          <i class="fa-regular fa-calendar-xmark gct-no-icon"></i>
          <h3>Chưa có ca làm việc hôm nay</h3>
          <p>{{ errorMessage || 'Bạn chưa được phân công ca làm việc hôm nay. Vui lòng liên hệ quản lý.' }}</p>
          <div class="gct-no-shift-actions">
            <button class="btn-retry" @click="handleRetryCheck">
              <i class="fa-solid fa-arrows-rotate"></i> Kiểm tra lại
            </button>
            <button class="btn-demo-access" @click="enableDemoAccess">
              <i class="fa-solid fa-flask"></i> Demo access
            </button>
          </div>
        </div>
      </div>

    </template>

    <!-- ─────────────────── ACTIVE SHIFT → DONG CA ─────────────────── -->
    <div v-if="!loading && caHienTai" class="gct-dong-ca-wrap">
      <div class="gct-dong-ca">

        <!-- Header -->
        <div class="gct-dc-header">
          <div class="gct-dc-header-top">
            <h2>Đóng ca làm việc: <span class="gct-ca-code">{{ caCode }}</span></h2>
          </div>
          <div class="gct-dc-meta">
            <span>Giờ mở ca: <b>{{ caStartedAt }}</b></span>
            <span>Nhân viên: <b>{{ currentUser?.tenNhanVien || currentUser?.hoTen || 'Nhân viên' }}</b></span>
          </div>
        </div>

        <!-- ── ĐẦU CA ── -->
        <div class="gct-dc-section">
          <div class="gct-dc-sec-head">
            <span class="sec-label">Đầu ca</span>
            <span class="sec-value">
              Tiền mặt: <b>{{ formatNumber(caHienTai.tienDauCaNhap) }}</b>
            </span>
          </div>
        </div>

        <!-- ── TRONG CA ── -->
        <div class="gct-dc-section">
          <div class="gct-dc-sec-head">
            <span class="sec-label">Trong ca</span>
            <span class="sec-value sec-hint">
              <i class="fa-regular fa-circle-question"></i>
              Tiền mặt: <b>{{ formatNumber(cashRevenue + codRevenue) }}</b>
            </span>
          </div>

          <div class="gct-table-wrap">
            <table class="gct-table">
              <thead>
                <tr>
                  <th class="th-item"></th>
                  <th>
                    Bán hàng
                    <small>{{ caHienTai.soDonHangDaThanhToan || 0 }} hóa đơn</small>
                  </th>
                  <th>
                    <div class="th-phieu-header">
                      <span>Phiếu thu</span>
                      <button class="btn-add-phieu" @click="openPhieuModal('THU')" title="Tạo phiếu thu"><i class="fa-solid fa-plus"></i> Tạo</button>
                    </div>
                    <small>{{ phieuThuList.length }} phiếu</small>
                  </th>
                  <th>
                    <div class="th-phieu-header">
                      <span>Phiếu chi</span>
                      <button class="btn-add-phieu btn-add-phieu-chi" @click="openPhieuModal('CHI')" title="Tạo phiếu chi"><i class="fa-solid fa-plus"></i> Tạo</button>
                    </div>
                    <small>{{ phieuChiList.length }} phiếu</small>
                  </th>
                  <th>
                    Trả hàng
                    <small>0 hóa đơn</small>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="td-item">1. Tiền mặt</td>
                  <td>{{ cashRevenue + codRevenue > 0 ? formatNumber(cashRevenue + codRevenue) : 0 }}</td>
                  <td>{{ phieuThuCash > 0 ? formatNumber(phieuThuCash) : 0 }}</td>
                  <td class="td-red">{{ phieuChiCash > 0 ? formatNumber(phieuChiCash) : 0 }}</td>
                  <td class="td-red">0</td>
                </tr>
                <tr>
                  <td class="td-item">2. Chuyển khoản</td>
                  <td>{{ transferRevenue > 0 ? formatNumber(transferRevenue) : 0 }}</td>
                  <td>{{ phieuThuTransfer > 0 ? formatNumber(phieuThuTransfer) : 0 }}</td>
                  <td class="td-red">{{ phieuChiTransfer > 0 ? formatNumber(phieuChiTransfer) : 0 }}</td>
                  <td class="td-red">0</td>
                </tr>
                <tr>
                  <td class="td-item">3. Thẻ</td>
                  <td>0</td>
                  <td>{{ phieuThuCard > 0 ? formatNumber(phieuThuCard) : 0 }}</td>
                  <td class="td-red">{{ phieuChiCard > 0 ? formatNumber(phieuChiCard) : 0 }}</td>
                  <td class="td-red">0</td>
                </tr>
                <tr>
                  <td class="td-item">4. Điểm</td>
                  <td>0</td>
                  <td>–</td>
                  <td class="td-red">–</td>
                  <td class="td-red">–</td>
                </tr>
                <tr class="tr-total">
                  <td class="td-item"><b>Tổng thu</b></td>
                  <td><b>{{ formatNumber(cashRevenue + codRevenue + transferRevenue) }}</b></td>
                  <td><b>{{ phieuThuTotal > 0 ? formatNumber(phieuThuTotal) : 0 }}</b></td>
                  <td class="td-red"><b>{{ phieuChiTotal > 0 ? formatNumber(phieuChiTotal) : 0 }}</b></td>
                  <td class="td-red"><b>0</b></td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Phieu list (expandable) -->
          <div v-if="phieuThuList.length || phieuChiList.length" class="gct-phieu-list-wrap">
            <div v-if="phieuThuList.length" class="gct-phieu-list">
              <h4 class="gct-phieu-list-title gct-phieu-thu-title">Chi tiết phiếu thu ({{ phieuThuList.length }})</h4>
              <div v-for="p in phieuThuList" :key="p.id" class="gct-phieu-item">
                <span class="gct-phieu-badge gct-phieu-badge-thu">THU</span>
                <span class="gct-phieu-method">{{ p.phuongThuc === 'TIEN_MAT' ? 'Tiền mặt' : p.phuongThuc === 'CHUYEN_KHOAN' ? 'CK' : 'Thẻ' }}</span>
                <span class="gct-phieu-amount">{{ formatNumber(p.soTien) }}đ</span>
                <span class="gct-phieu-reason">{{ p.lyDo || '—' }}</span>
                <button class="gct-phieu-del" @click="handleDeletePhieu(p)" title="Xóa">&times;</button>
              </div>
            </div>
            <div v-if="phieuChiList.length" class="gct-phieu-list">
              <h4 class="gct-phieu-list-title gct-phieu-chi-title">Chi tiết phiếu chi ({{ phieuChiList.length }})</h4>
              <div v-for="p in phieuChiList" :key="p.id" class="gct-phieu-item">
                <span class="gct-phieu-badge gct-phieu-badge-chi">CHI</span>
                <span class="gct-phieu-method">{{ p.phuongThuc === 'TIEN_MAT' ? 'Tiền mặt' : p.phuongThuc === 'CHUYEN_KHOAN' ? 'CK' : 'Thẻ' }}</span>
                <span class="gct-phieu-amount gct-phieu-amount-chi">{{ formatNumber(p.soTien) }}đ</span>
                <span class="gct-phieu-reason">{{ p.lyDo || '—' }}</span>
                <button class="gct-phieu-del" @click="handleDeletePhieu(p)" title="Xóa">&times;</button>
              </div>
            </div>
          </div>
        </div>

        <!-- ── BÁO CÁO CHỐT CA (iPOS style) ── -->
        <div class="gct-dc-section">
          <div class="gct-dc-sec-head">
            <span class="sec-label">Báo cáo chốt ca</span>
          </div>
          <div class="gct-report">
            <div class="gct-report-block">
              <h4 class="gct-report-title">BÁN HÀNG</h4>
              <div class="gct-report-row">
                <span>Số hóa đơn</span>
                <b>{{ caHienTai?.soDonHangDaThanhToan || 0 }}</b>
              </div>
              <div class="gct-report-row">
                <span>Doanh thu</span>
                <b>{{ formatNumber(banHangGross) }} đ</b>
              </div>
              <div class="gct-report-row">
                <span>+ Tiền mặt (1)</span>
                <b>{{ formatNumber(banHangTienMat) }} đ</b>
              </div>
              <div class="gct-report-row">
                <span>+ Chuyển khoản</span>
                <b>{{ formatNumber(banHangCK) }} đ</b>
              </div>
            </div>

            <div class="gct-report-block">
              <h4 class="gct-report-title gct-report-thu">THU</h4>
              <div class="gct-report-row">
                <span>+ Tiền mặt (2)</span>
                <b>{{ formatNumber(phieuThuCash) }} đ</b>
              </div>
              <div class="gct-report-row">
                <span>+ Chuyển khoản</span>
                <b>{{ formatNumber(phieuThuTransfer) }} đ</b>
              </div>
            </div>

            <div class="gct-report-block">
              <h4 class="gct-report-title gct-report-chi">CHI</h4>
              <div class="gct-report-row">
                <span>+ Tiền mặt (3)</span>
                <b class="td-red">{{ formatNumber(phieuChiCash) }} đ</b>
              </div>
              <div class="gct-report-row">
                <span>+ Chuyển khoản</span>
                <b class="td-red">{{ formatNumber(phieuChiTransfer) }} đ</b>
              </div>
            </div>

            <div class="gct-report-block gct-report-bangiao">
              <h4 class="gct-report-title">BÀN GIAO CA</h4>
              <div class="gct-report-row">
                <span>Tiền mặt đầu ca</span>
                <b>{{ formatNumber(caHienTai?.tienDauCaNhap || 0) }} đ</b>
              </div>
              <div class="gct-report-row">
                <span>Tiền mặt trong ca (1)+(2)-(3)</span>
                <b>{{ formatNumber(tienMatTrongCa) }} đ</b>
              </div>
              <div class="gct-report-row gct-report-highlight">
                <span>Tiền mặt cuối ca</span>
                <b>{{ formatNumber(tienMatCuoiCa) }} đ</b>
              </div>
            </div>
          </div>
        </div>

        <!-- ── CUỐI CA ── -->
        <div class="gct-dc-section">
          <div class="gct-dc-sec-head">
            <span class="sec-label">Cuối ca</span>
            <span class="sec-value sec-hint">
              <i class="fa-regular fa-circle-question"></i>
              Tiền mặt lý thuyết: <b>{{ formatNumber(tienMatCuoiCa) }}</b>
            </span>
          </div>

          <div class="gct-cuoi-ca-row">
            <div class="gct-cuoi-field">
              <label class="gct-label">Tiền mặt bàn giao thực tế <span class="req">*</span></label>
              <input
                type="text"
                class="gct-input"
                :value="tienThucTeInput > 0 ? formatNumber(tienThucTeInput) : ''"
                @input="onInputMoney($event, 'end')"
                placeholder="0"
              />
            </div>
            <div class="gct-cuoi-field">
              <label class="gct-label">Số tiền chênh lệch</label>
              <input
                type="text"
                class="gct-input gct-input-readonly"
                readonly
                :value="formatNumber(chenhLech)"
              />
            </div>
            <div class="gct-cuoi-field">
              <label class="gct-label">Ghi chú</label>
              <input
                type="text"
                class="gct-input"
                v-model="ghiChuInput"
                placeholder="Nhập ghi chú"
              />
            </div>
          </div>
        </div>

        <!-- Inline error for close shift -->
        <div v-if="errorMessage" class="gct-error-msg gct-error-body">
          <i class="fa-solid fa-circle-exclamation"></i>
          {{ errorMessage }}
        </div>

        <!-- Footer -->
        <div class="gct-dc-footer">
          <button class="btn-cap-nhat" @click="refreshRevenue" :disabled="isRefreshing">
            <i class="fa-solid fa-arrows-rotate" :class="{ 'fa-spin': isRefreshing }"></i>
            Cập nhật dữ liệu
          </button>
          <div class="gct-dc-footer-right">
            <button class="btn-dong-ca" @click="submitKetThucCa(false)" :disabled="isSubmitting">
              {{ isSubmitting ? 'Đang xử lý...' : 'Đóng ca' }}
            </button>
            <button class="btn-dong-in" @click="submitKetThucCa(true)" :disabled="isSubmitting">
              Đóng ca và in phiếu bàn giao
            </button>
          </div>
        </div>

      </div>
    </div>

    <!-- TOAST -->
    <transition name="gct-toast-slide">
      <div v-if="showToast" class="gct-toast" :class="`gct-toast-${toastType}`">
        <i :class="toastType === 'error' ? 'fa-solid fa-circle-xmark' : toastType === 'warning' ? 'fa-solid fa-triangle-exclamation' : 'fa-solid fa-circle-check'" class="gct-toast-icon"></i>
        <span>{{ toastMessage }}</span>
        <button class="gct-toast-close" @click="showToast = false">×</button>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { getAllLichLamViecFull } from "@/services/lichLamViecService";
import {
  createLichSuCa,
  getLichSuCaByNhanVien,
  updateLichSuCa,
} from "@/services/lichSuCaService";
import {
  getAllNhanVien,
  getNhanVienByTaiKhoanId,
} from "@/services/nhanVienService";
import { getAllHoaDon } from "@/services/hoaDonService";
import {
  getRevenueBucket,
  isRevenueCountableOrder,
  resolveInvoiceEmployeeId,
  resolveRevenueDateKey,
} from "@/utils/orderRevenue";
import { getPhieuByShift, createPhieu, deletePhieu } from "@/services/phieuThuChiService";

const router = useRouter();
const emit = defineEmits(["ca-started"]);

const ACTIVE_SHIFT_PREFIX = "giao-ca-active";
const currentUser = ref(null);
const idNhanVien = ref(null);

// ─── Helpers ───────────────────────────────────────────────────────────────

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  return [];
};

const normalizeDateKey = (value) => {
  if (!value) return "";
  if (Array.isArray(value) && value.length >= 3) {
    const [y, m, d] = value;
    return `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  }
  const raw = String(value).trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw;
  if (raw.includes("T")) return raw.split("T")[0];
  return "";
};

const getTodayDateKey = () => {
  const nowDate = new Date();
  const year = nowDate.getFullYear();
  const month = String(nowDate.getMonth() + 1).padStart(2, "0");
  const day = String(nowDate.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const normalizeTime = (value, fallback = "00:00") => {
  if (!value) return fallback;
  const raw = String(value).trim();
  if (/^\d{2}:\d{2}$/.test(raw)) return raw;
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5);
  return fallback;
};

const toMinutes = (timeValue) => {
  const normalized = normalizeTime(timeValue);
  const [hour, minute] = normalized.split(":");
  return Number(hour) * 60 + Number(minute);
};

const normalizeNhanVien = (payload) => {
  if (!payload) return null;
  if (Array.isArray(payload)) return payload[0] || null;
  if (Array.isArray(payload?.content)) return payload.content[0] || null;
  if (payload?.data) return normalizeNhanVien(payload.data);
  if (payload?.id || payload?.tenNhanVien || payload?.idTaiKhoan || payload?.taiKhoan) {
    return payload;
  }
  return null;
};

const getActiveStorageKey = (employeeId) => `${ACTIVE_SHIFT_PREFIX}:${employeeId}`;

const loadActiveShift = (employeeId) => {
  if (!employeeId) return null;
  const raw = localStorage.getItem(getActiveStorageKey(employeeId));
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    localStorage.removeItem(getActiveStorageKey(employeeId));
    return null;
  }
};

const saveActiveShift = (employeeId, payload) => {
  if (!employeeId) return;
  localStorage.setItem(getActiveStorageKey(employeeId), JSON.stringify(payload));
};

const clearActiveShift = (employeeId) => {
  if (!employeeId) return;
  localStorage.removeItem(getActiveStorageKey(employeeId));
};

const findCurrentSchedule = (schedules) => {
  if (!schedules.length) return null;
  const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes();
  const sorted = [...schedules].sort((a, b) => toMinutes(a.gioBatDau) - toMinutes(b.gioBatDau));
  const inProgress = sorted.find((item) => {
    const start = toMinutes(item.gioBatDau);
    const end = toMinutes(item.gioKetThuc);
    return nowMinutes >= start && nowMinutes < end;
  });
  return inProgress || null;
};

const buildCurrentCa = (activeShift, schedule) => ({
  id: activeShift.id,
  idCaLam: schedule.idCaLam,
  idLichLamViec: schedule.id,
  ngayLam: normalizeDateKey(schedule.ngayLam),
  tenCa: schedule.tenCa,
  gioBatDau: schedule.gioBatDau,
  gioKetThuc: schedule.gioKetThuc,
  tienDauCaNhap: Number(activeShift.tienDauCaNhap || 0),
  tongTienTrongCa: Number(activeShift.tongTienTrongCa || 0),
  soDonHangDaThanhToan: Number(activeShift.soDonHangDaThanhToan || 0),
  ghiChuBanDau: String(activeShift.ghiChuBanDau || ""),
});

const layThongTinUser = () => {
  const storedUser = localStorage.getItem("user") || sessionStorage.getItem("user");
  if (storedUser) {
    try {
      return JSON.parse(storedUser);
    } catch {
      return null;
    }
  }
  return null;
};

const resolveNhanVienDangNhap = async () => {
  const stored = layThongTinUser();

  if (stored?.idNhanVien) {
    currentUser.value = stored;
    return Number(stored.idNhanVien);
  }

  if (stored?.id && stored?.tenNhanVien) {
    currentUser.value = stored;
    return Number(stored.id);
  }

  const taiKhoanId = localStorage.getItem("userId");
  if (taiKhoanId) {
    try {
      const byTaiKhoan = await getNhanVienByTaiKhoanId(taiKhoanId);
      const nhanVien = normalizeNhanVien(byTaiKhoan);
      if (nhanVien?.id) {
        currentUser.value = nhanVien;
        return Number(nhanVien.id);
      }
    } catch {
      // Fallback below
    }
  }

  const allRes = await getAllNhanVien();
  const allNhanVien = extractList(allRes?.data);
  const taiKhoanIdNum = Number(localStorage.getItem("userId") || 0);

  const fallback =
    allNhanVien.find((item) => {
      if (!item?.id) return false;
      if (stored?.id && Number(item.id) === Number(stored.id)) return true;
      const mappedTaiKhoanId = Number(item?.idTaiKhoan || item?.taiKhoan?.id || 0);
      return taiKhoanIdNum > 0 && mappedTaiKhoanId === taiKhoanIdNum;
    }) || null;

  if (fallback?.id) {
    currentUser.value = fallback;
    return Number(fallback.id);
  }

  return null;
};

// ─── Reactive State ────────────────────────────────────────────────────────

const loading = ref(true);
const isSubmitting = ref(false);
const isRefreshing = ref(false);

// Revenue breakdown
const cashRevenue = ref(0);
const codRevenue = ref(0);
const transferRevenue = ref(0);

const caHienTai = ref(null);
const lichHomNay = ref(null);
const lichSuCaHienTai = ref(null);
const showHandoverReminder = ref(false);
const errorMessage = ref("");
const showToast = ref(false);
const toastMessage = ref("");
const toastType = ref("success");
let toastTimeout = null;
const isDemoAccess = ref(false);

const tienBanDauInput = ref(0);
const ghiChuBanDau = ref("");
const tienThucTeInput = ref(0);
const ghiChuInput = ref("");

const now = ref(new Date());
let timer = null;

// ─── Phieu thu/chi state ───────────────────────────────────────────────────
const phieuThuList = ref([]);
const phieuChiList = ref([]);
const showPhieuModal = ref(false);
const phieuModalType = ref("THU"); // THU or CHI
const phieuForm = ref({ phuongThuc: "TIEN_MAT", soTien: 0, lyDo: "" });
const phieuSubmitting = ref(false);

// ─── Computed ──────────────────────────────────────────────────────────────

const formattedFullTime = computed(() => {
  const d = now.value;
  const time = d.toLocaleTimeString("vi-VN", {
    hour12: false,
    hour: "2-digit",
    minute: "2-digit",
  });
  const date = `${d.getDate().toString().padStart(2, "0")}/${(d.getMonth() + 1).toString().padStart(2, "0")}/${d.getFullYear()}`;
  return `${time} ${date}`;
});

const formattedTimeOnly = computed(() => {
  return now.value.toLocaleTimeString("vi-VN", { hour12: false });
});

// CA code display: CA + last 6 digits of timestamp-based ID
const caCode = computed(() => {
  if (!caHienTai.value?.id) return "CA000000";
  const raw = String(caHienTai.value.id);
  const match = raw.match(/(\d+)$/);
  if (!match) return raw.replace(/[^A-Za-z0-9]/g, "").slice(0, 10).toUpperCase();
  const num = parseInt(match[1]) % 1000000;
  return `CA${String(num).padStart(6, "0")}`;
});

// Time when the current shift was opened
const caStartedAt = computed(() => {
  if (!caHienTai.value || !idNhanVien.value) return formattedFullTime.value;
  const active = loadActiveShift(idNhanVien.value);
  if (active?.startedAt) {
    const d = new Date(active.startedAt);
    const day = String(d.getDate()).padStart(2, "0");
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const year = d.getFullYear();
    const hh = String(d.getHours()).padStart(2, "0");
    const mm = String(d.getMinutes()).padStart(2, "0");
    return `${day}/${month}/${year} ${hh}:${mm}`;
  }
  return formattedFullTime.value;
});

const tinhTongLyThuyet = computed(() => {
  return tienMatCuoiCa.value;
});

const chenhLech = computed(() => {
  return tienThucTeInput.value - tienMatCuoiCa.value;
});

// ─── Phieu computed ────────────────────────────────────────────────────────
const phieuThuCash = computed(() => phieuThuList.value.filter((p) => p.phuongThuc === "TIEN_MAT").reduce((s, p) => s + Number(p.soTien || 0), 0));
const phieuThuTransfer = computed(() => phieuThuList.value.filter((p) => p.phuongThuc === "CHUYEN_KHOAN").reduce((s, p) => s + Number(p.soTien || 0), 0));
const phieuThuCard = computed(() => phieuThuList.value.filter((p) => p.phuongThuc === "THE").reduce((s, p) => s + Number(p.soTien || 0), 0));
const phieuThuTotal = computed(() => phieuThuCash.value + phieuThuTransfer.value + phieuThuCard.value);

const phieuChiCash = computed(() => phieuChiList.value.filter((p) => p.phuongThuc === "TIEN_MAT").reduce((s, p) => s + Number(p.soTien || 0), 0));
const phieuChiTransfer = computed(() => phieuChiList.value.filter((p) => p.phuongThuc === "CHUYEN_KHOAN").reduce((s, p) => s + Number(p.soTien || 0), 0));
const phieuChiCard = computed(() => phieuChiList.value.filter((p) => p.phuongThuc === "THE").reduce((s, p) => s + Number(p.soTien || 0), 0));
const phieuChiTotal = computed(() => phieuChiCash.value + phieuChiTransfer.value + phieuChiCard.value);

// ─── 24h overdue check ────────────────────────────────────────────────────
const is24hOverdue = computed(() => {
  if (!caHienTai.value || !idNhanVien.value) return false;
  const active = loadActiveShift(idNhanVien.value);
  if (!active?.startedAt) return false;
  const started = new Date(active.startedAt);
  return (now.value - started) > 24 * 60 * 60 * 1000;
});

const shiftDurationText = computed(() => {
  if (!caHienTai.value || !idNhanVien.value) return "";
  const active = loadActiveShift(idNhanVien.value);
  if (!active?.startedAt) return "";
  const ms = now.value - new Date(active.startedAt);
  const hours = Math.floor(ms / (1000 * 60 * 60));
  const mins = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
  return `${hours}h ${mins}m`;
});

// ─── Role check ───────────────────────────────────────────────────────────
// Per reviewer: only THU_NGAN (cashier), OWNER, ADMIN, QUAN_LY, and employee-role variants can open a shift.
const ALLOWED_SHIFT_CODES = new Set([
  "ADMIN",
  "OWNER",
  "THU_NGAN",
  "CASHIER",
  "QUAN_LY",
  "MANAGER",
  "CHU_CUA_HANG",
  "NHAN_VIEN",
  "NHANVIEN",
  "EMPLOYEE",
]);

const normalizeRoleCode = (value) => {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase()
    .replace(/^ROLE_/, "")
    .replace(/[^A-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
}

const userRole = computed(() => normalizeRoleCode(localStorage.getItem("role") || ""));

const resolveChucVuCode = (user) => {
  if (!user) return "";
  // API returns chucVu as object { id, maChucVu, tenChucVu }
  const cv = user.chucVu;
  if (cv && typeof cv === "object") {
    return normalizeRoleCode(cv.maChucVu || cv.tenChucVu || "")
  }
  // Fallback: direct string field
  return normalizeRoleCode(cv || "")
};

const canOpenShift = computed(() => {
  if (userRole.value === "ADMIN") return true;
  if (!currentUser.value) return true;
  const code = resolveChucVuCode(currentUser.value);
  if (!code) return true; // No position assigned → allow (backward-compat)
  return ALLOWED_SHIFT_CODES.has(code) || ALLOWED_SHIFT_CODES.has(userRole.value);
});

const chucVuLabel = computed(() => {
  const cv = currentUser.value?.chucVu;
  if (cv && typeof cv === "object") return cv.tenChucVu || cv.maChucVu || "";
  return String(cv || "");
});

// ─── Báo cáo chốt ca computeds (iPOS style) ──────────────────────────────
const banHangTienMat = computed(() => cashRevenue.value + codRevenue.value);
const banHangCK = computed(() => transferRevenue.value);
const banHangGross = computed(() => banHangTienMat.value + banHangCK.value);

const tienMatTrongCa = computed(() => {
  // Cash from sales + cash from thu - cash from chi
  return banHangTienMat.value + phieuThuCash.value - phieuChiCash.value;
});

const tienMatCuoiCa = computed(() => {
  return (caHienTai.value?.tienDauCaNhap || 0) + tienMatTrongCa.value;
});

// ─── Helpers / Formatters ──────────────────────────────────────────────────

const formatNumber = (num) => {
  if (!num && num !== 0) return "0";
  return new Intl.NumberFormat("vi-VN").format(num);
};

const onInputMoney = (event, type) => {
  const raw = event.target.value.replace(/\D/g, "");
  const val = Number(raw);
  if (type === "start") tienBanDauInput.value = val;
  if (type === "end") tienThucTeInput.value = val;
};

const triggerToast = (msg, type = "success") => {
  toastMessage.value = msg;
  toastType.value = type;
  showToast.value = true;
  if (toastTimeout) clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => {
    showToast.value = false;
  }, 3500);
};

// ─── Phieu Thu/Chi CRUD ────────────────────────────────────────────────────

const fetchPhieu = async () => {
  if (!idNhanVien.value || !caHienTai.value) {
    phieuThuList.value = [];
    phieuChiList.value = [];
    return;
  }
  try {
    const ngayLam = caHienTai.value.ngayLam || resolveRevenueDateKeyForShift();
    const idCaLam = caHienTai.value.idCaLam || 0;
    const res = await getPhieuByShift(idNhanVien.value, ngayLam, idCaLam);
    const list = Array.isArray(res?.data) ? res.data : [];
    phieuThuList.value = list.filter((p) => p.loai === "THU");
    phieuChiList.value = list.filter((p) => p.loai === "CHI");
  } catch {
    phieuThuList.value = [];
    phieuChiList.value = [];
  }
};

const openPhieuModal = (type) => {
  phieuModalType.value = type;
  phieuForm.value = { phuongThuc: "TIEN_MAT", soTien: 0, lyDo: "" };
  showPhieuModal.value = true;
};

const handleCreatePhieu = async () => {
  if (!phieuForm.value.soTien || Number(phieuForm.value.soTien) <= 0) {
    triggerToast("Vui lòng nhập số tiền hợp lệ", "warning");
    return;
  }
  phieuSubmitting.value = true;
  try {
    await createPhieu({
      loai: phieuModalType.value,
      phuongThuc: phieuForm.value.phuongThuc,
      soTien: Number(phieuForm.value.soTien),
      lyDo: String(phieuForm.value.lyDo || "").trim(),
      idNhanVien: idNhanVien.value,
      ngayLam: caHienTai.value?.ngayLam || resolveRevenueDateKeyForShift(),
      idCaLam: caHienTai.value?.idCaLam || 0,
      nguoiTao: currentUser.value?.tenNhanVien || currentUser.value?.hoTen || ""
    });
    showPhieuModal.value = false;
    await fetchPhieu();
    triggerToast(`Tạo phiếu ${phieuModalType.value === "THU" ? "thu" : "chi"} thành công!`);
  } catch (err) {
    triggerToast("Lỗi: " + (err?.response?.data?.message || err?.message || "Không thể tạo phiếu"), "error");
  } finally {
    phieuSubmitting.value = false;
  }
};

const handleDeletePhieu = async (phieu) => {
  if (!confirm(`Xóa phiếu ${phieu.loai === "THU" ? "thu" : "chi"} ${formatNumber(phieu.soTien)}đ?`)) return;
  try {
    await deletePhieu(phieu.id);
    await fetchPhieu();
    triggerToast("Đã xóa phiếu");
  } catch {
    triggerToast("Không thể xóa phiếu", "error");
  }
};

const onPhieuMoneyInput = (event) => {
  const raw = event.target.value.replace(/\D/g, "");
  phieuForm.value.soTien = Number(raw);
};

// ─── Revenue Fetch (extended with cash / cod / transfer breakdown) ──────────

const fetchShiftRevenue = async (employeeId, dateKey) => {
  try {
    const res = await getAllHoaDon();
    const orders = extractList(res?.data);
    let cash = 0;
    let cod = 0;
    let transfer = 0;
    let count = 0;

    for (const o of orders) {
      if (resolveInvoiceEmployeeId(o) !== Number(employeeId)) continue;
      if (!isRevenueCountableOrder(o)) continue;
      const bucket = getRevenueBucket(o);
      if (bucket === "unknown") continue;
      if (resolveRevenueDateKey(o, bucket) !== dateKey) continue;
      const amount = Math.max(0, Number(o?.thanhTien || 0) - Number(o?.phiShip || 0));
      if (amount <= 0) continue;

      count++;
      if (bucket === "cash") cash += amount;
      else if (bucket === "cod") cod += amount;
      else if (bucket === "transfer") transfer += amount;
    }

    return { cashRevenue: cash, codRevenue: cod, transferRevenue: transfer, orderCount: count };
  } catch {
    return { cashRevenue: 0, codRevenue: 0, transferRevenue: 0, orderCount: 0 };
  }
};

const resolveRevenueDateKeyForShift = () => {
  const shiftDate = normalizeDateKey(caHienTai.value?.ngayLam || lichHomNay.value?.ngayLam);
  return shiftDate || getTodayDateKey();
};

// ─── Handover Reminder ─────────────────────────────────────────────────────

const getReminderStorageKey = () => {
  if (!lichHomNay.value || !idNhanVien.value) return "";
  return `handover-remind-dismissed:${idNhanVien.value}:${lichHomNay.value.id}`;
};

const dismissHandoverReminder = () => {
  const storageKey = getReminderStorageKey();
  if (storageKey) sessionStorage.setItem(storageKey, "1");
  showHandoverReminder.value = false;
};

const evaluateHandoverReminder = () => {
  if (!caHienTai.value || !lichHomNay.value) {
    showHandoverReminder.value = false;
    return;
  }
  const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes();
  const endMinutes = toMinutes(lichHomNay.value.gioKetThuc);
  const storageKey = getReminderStorageKey();
  const dismissed = storageKey ? sessionStorage.getItem(storageKey) === "1" : false;
  if (nowMinutes >= endMinutes && !dismissed) {
    showHandoverReminder.value = true;
  }
};

const enableDemoAccess = () => {
  const nowDate = new Date();
  const currentHour = nowDate.getHours();
  const shiftEndHour = Math.min(currentHour + 5, 23);
  const today = getTodayDateKey();

  lichHomNay.value = {
    id: -1,
    idCaLam: 1,
    idNhanVien: idNhanVien.value,
    ngayLam: today,
    tenCa: "Ca demo",
    gioBatDau: `${String(currentHour).padStart(2, "0")}:00`,
    gioKetThuc: `${String(shiftEndHour).padStart(2, "0")}:59`,
  };

  isDemoAccess.value = true;
  errorMessage.value = "Đang ở chế độ demo: có thể mở/đóng ca để test giao diện mà không ghi lịch sử ca thật.";
};

const handleRetryCheck = async () => {
  await loadData();

  if (caHienTai.value) {
    triggerToast("Đã tìm thấy ca đang hoạt động.");
    return;
  }

  if (lichHomNay.value) {
    triggerToast(errorMessage.value || "Đã tìm thấy ca làm việc. Bạn có thể mở ca.");
    return;
  }

  triggerToast(errorMessage.value || "Hiện chưa có ca làm việc khả dụng.", "error");
};

// ─── Load Data ─────────────────────────────────────────────────────────────

const loadData = async () => {
  loading.value = true;
  errorMessage.value = "";
  isDemoAccess.value = false;

  try {
    if (!idNhanVien.value) {
      idNhanVien.value = await resolveNhanVienDangNhap();
      if (!idNhanVien.value) {
        router.push("/auth/staff-login");
        return;
      }
    }

    const [lichLamRes, lichSuRes] = await Promise.all([
      getAllLichLamViecFull(),
      getLichSuCaByNhanVien(idNhanVien.value),
    ]);

    const lichLamListFull = extractList(lichLamRes?.data);
    const lichLamList = lichLamListFull.filter(
      (item) => Number(item?.idNhanVien) === Number(idNhanVien.value)
    );
    const lichSuList = extractList(lichSuRes?.data);
    const todayKey = getTodayDateKey();

    const lichTrongNgay = lichLamList
      .filter((item) => normalizeDateKey(item?.ngayLam) === todayKey)
      .map((item) => ({
        ...item,
        tenCa: item?.tenCa || `Ca #${item?.idCaLam}`,
        gioBatDau: normalizeTime(item?.gioBatDau, "00:00"),
        gioKetThuc: normalizeTime(item?.gioKetThuc, "00:00"),
      }));

    const activeShift = loadActiveShift(idNhanVien.value);
    const activeShiftSchedule = activeShift
      ? lichTrongNgay.find((item) => {
          return (
            Number(item?.id) === Number(activeShift?.idLichLamViec) &&
            normalizeDateKey(item?.ngayLam) === normalizeDateKey(activeShift?.ngayLam)
          );
        })
      : null;

    lichHomNay.value = activeShiftSchedule || findCurrentSchedule(lichTrongNgay);
    lichSuCaHienTai.value = null;
    caHienTai.value = null;
    showHandoverReminder.value = false;

    if (!lichHomNay.value) {
      clearActiveShift(idNhanVien.value);
      errorMessage.value =
        "Bạn chưa được phân công ca đang diễn ra. Vui lòng liên hệ admin.";
      return;
    }

    const lichSuDaDong = lichSuList.find((item) => {
      return (
        normalizeDateKey(item?.ngay) === normalizeDateKey(lichHomNay.value?.ngayLam) &&
        Number(item?.idCaLam) === Number(lichHomNay.value?.idCaLam)
      );
    });

    if (lichSuDaDong) {
      const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes();
      const shiftEndMinutes = toMinutes(lichHomNay.value?.gioKetThuc);
      const stillWithinShift = nowMinutes < shiftEndMinutes;

      if (!stillWithinShift) {
        lichSuCaHienTai.value = lichSuDaDong;
        lichHomNay.value = null;
        clearActiveShift(idNhanVien.value);
        errorMessage.value =
          "Ca hôm nay đã được đóng. Bạn có thể xem lịch sử ở trang quản lý.";
        return;
      }

      errorMessage.value =
        "Ca này đã từng được đóng sớm. Bạn có thể mở lại ca trong thời gian còn lại của khung giờ.";
    }

    if (
      activeShift &&
      Number(activeShift?.idLichLamViec) === Number(lichHomNay.value?.id) &&
      normalizeDateKey(activeShift?.ngayLam) === normalizeDateKey(lichHomNay.value?.ngayLam)
    ) {
      caHienTai.value = buildCurrentCa(activeShift, lichHomNay.value);
      tienThucTeInput.value = caHienTai.value.tienDauCaNhap || 0;
      evaluateHandoverReminder();

      const rev = await fetchShiftRevenue(idNhanVien.value, resolveRevenueDateKeyForShift());
      cashRevenue.value = rev.cashRevenue;
      codRevenue.value = rev.codRevenue;
      transferRevenue.value = rev.transferRevenue;
      caHienTai.value.tongTienTrongCa = rev.cashRevenue + rev.codRevenue;
      caHienTai.value.soDonHangDaThanhToan = rev.orderCount;

      await fetchPhieu();

      const stored = loadActiveShift(idNhanVien.value);
      if (stored) {
        stored.tongTienTrongCa = caHienTai.value.tongTienTrongCa;
        stored.soDonHangDaThanhToan = rev.orderCount;
        saveActiveShift(idNhanVien.value, stored);
      }
    } else {
      clearActiveShift(idNhanVien.value);
    }
  } catch (error) {
    console.error("Lỗi load data:", error);
    caHienTai.value = null;
    errorMessage.value = "Không thể tải dữ liệu giao ca. Vui lòng thử lại.";
  } finally {
    loading.value = false;
  }
};

// ─── Refresh Revenue (Cập nhật dữ liệu button) ────────────────────────────

const refreshRevenue = async () => {
  if (!caHienTai.value || isRefreshing.value) return;
  isRefreshing.value = true;
  errorMessage.value = "";
  try {
    const dateKey = resolveRevenueDateKeyForShift();
    const rev = await fetchShiftRevenue(idNhanVien.value, dateKey);
    cashRevenue.value = rev.cashRevenue;
    codRevenue.value = rev.codRevenue;
    transferRevenue.value = rev.transferRevenue;
    caHienTai.value.soDonHangDaThanhToan = rev.orderCount;
    caHienTai.value.tongTienTrongCa = rev.cashRevenue + rev.codRevenue;

    const stored = loadActiveShift(idNhanVien.value);
    if (stored) {
      stored.tongTienTrongCa = caHienTai.value.tongTienTrongCa;
      stored.soDonHangDaThanhToan = rev.orderCount;
      saveActiveShift(idNhanVien.value, stored);
    }
    await fetchPhieu();
    triggerToast("Đã cập nhật dữ liệu thành công!");
  } catch {
    triggerToast("Không thể cập nhật dữ liệu. Vui lòng thử lại.", "error");
  } finally {
    isRefreshing.value = false;
  }
};

// ─── Open Shift ────────────────────────────────────────────────────────────

const handleBatDauCa = async () => {
  errorMessage.value = "";
  if (!canOpenShift.value) {
    errorMessage.value = "Chức vụ của bạn không được phép mở ca. Vui lòng liên hệ quản lý.";
    return;
  }
  if (tienBanDauInput.value < 0) {
    errorMessage.value = "Tiền mặt đầu ca không hợp lệ.";
    return;
  }
  if (!lichHomNay.value?.id || !lichHomNay.value?.idCaLam) {
    errorMessage.value = "Bạn không có ca hợp lệ để mở.";
    return;
  }

  isSubmitting.value = true;
  try {
    const activePayload = {
      id: `CA-${Date.now()}`,
      idNhanVien: idNhanVien.value,
      idCaLam: lichHomNay.value.idCaLam,
      idLichLamViec: lichHomNay.value.id,
      ngayLam: normalizeDateKey(lichHomNay.value.ngayLam),
      tienDauCaNhap: tienBanDauInput.value,
      tongTienTrongCa: 0,
      soDonHangDaThanhToan: 0,
      ghiChuBanDau: String(ghiChuBanDau.value || "").trim(),
      isDemoAccess: isDemoAccess.value,
      startedAt: new Date().toISOString(),
    };

    saveActiveShift(idNhanVien.value, activePayload);
    caHienTai.value = buildCurrentCa(activePayload, lichHomNay.value);
    tienThucTeInput.value = caHienTai.value.tienDauCaNhap || 0;
    showHandoverReminder.value = false;

    const rev = await fetchShiftRevenue(idNhanVien.value, resolveRevenueDateKeyForShift());
    cashRevenue.value = rev.cashRevenue;
    codRevenue.value = rev.codRevenue;
    transferRevenue.value = rev.transferRevenue;
    caHienTai.value.tongTienTrongCa = rev.cashRevenue + rev.codRevenue;
    caHienTai.value.soDonHangDaThanhToan = rev.orderCount;
    activePayload.tongTienTrongCa = caHienTai.value.tongTienTrongCa;
    activePayload.soDonHangDaThanhToan = rev.orderCount;
    saveActiveShift(idNhanVien.value, activePayload);

    triggerToast("Mở ca làm việc thành công!");
    emit("ca-started");
  } catch (error) {
    errorMessage.value =
      error?.data?.message ||
      error?.message ||
      "Không thể mở ca vào lúc này.";
  } finally {
    isSubmitting.value = false;
  }
};

// ─── Close Shift ───────────────────────────────────────────────────────────

const submitKetThucCa = async (printAfter = false) => {
  if (!caHienTai.value || !idNhanVien.value || !lichHomNay.value) return;

  isSubmitting.value = true;
  errorMessage.value = "";

  try {
    if (isDemoAccess.value || caHienTai.value?.isDemoAccess) {
      clearActiveShift(idNhanVien.value);
      caHienTai.value = null;
      lichHomNay.value = null;
      showHandoverReminder.value = false;
      isDemoAccess.value = false;
      triggerToast("Đã đóng ca demo. Không có dữ liệu nào được ghi vào lịch sử ca.");
      return;
    }

    const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes();
    const shiftEndMinutes = toMinutes(lichHomNay.value.gioKetThuc);
    const isEarlyClose = nowMinutes < shiftEndMinutes;

    const lichSuRes = await getLichSuCaByNhanVien(idNhanVien.value);
    const lichSuList = extractList(lichSuRes?.data);

    const existing = lichSuList.find((item) => {
      return (
        normalizeDateKey(item?.ngay) === normalizeDateKey(lichHomNay.value?.ngayLam) &&
        Number(item?.idCaLam) === Number(lichHomNay.value?.idCaLam)
      );
    });

    const tongLyThuyet = tinhTongLyThuyet.value;
    const chenh = chenhLech.value;
    const earlyCloseNote = isEarlyClose
      ? `Đóng sớm lúc ${formattedTimeOnly.value} (kết ca ${normalizeTime(lichHomNay.value?.gioKetThuc)})`
      : "";

    const ghiChuTongHop = [
      caHienTai.value?.ghiChuBanDau ? `Đầu ca: ${caHienTai.value.ghiChuBanDau}` : "",
      `Tiền đầu ca: ${formatNumber(caHienTai.value?.tienDauCaNhap || 0)}đ`,
      `Tiền lý thuyết: ${formatNumber(tongLyThuyet)}đ`,
      `Tiền thực tế: ${formatNumber(tienThucTeInput.value)}đ`,
      `Chênh lệch: ${chenh >= 0 ? "+" : ""}${formatNumber(chenh)}đ`,
      phieuThuList.value.length ? `Phiếu thu: ${phieuThuList.value.length} (${formatNumber(phieuThuTotal.value)}đ)` : "",
      phieuChiList.value.length ? `Phiếu chi: ${phieuChiList.value.length} (${formatNumber(phieuChiTotal.value)}đ)` : "",
      earlyCloseNote,
      ghiChuInput.value.trim(),
    ]
      .filter(Boolean)
      .join(" | ");

    const payload = {
      idNhanVien: idNhanVien.value,
      idCaLam: lichHomNay.value.idCaLam,
      ngay: normalizeDateKey(lichHomNay.value.ngayLam),
      tienCa: tienThucTeInput.value,
      tienDauCa: caHienTai.value?.tienDauCaNhap || 0,
      doanhThu: cashRevenue.value + codRevenue.value,
      tienChuyenKhoan: transferRevenue.value || 0,
      ghiChu: ghiChuTongHop,
      trangThai: "Hoàn thành",
    };

    if (existing?.id) {
      await updateLichSuCa(existing.id, payload);
    } else {
      await createLichSuCa(payload);
    }

    clearActiveShift(idNhanVien.value);
    showHandoverReminder.value = false;
    triggerToast("Kết thúc ca làm việc thành công!");

    if (printAfter) {
      setTimeout(() => window.print(), 600);
    }

    await loadData();
  } catch (error) {
    triggerToast("Lỗi: " + (error.response?.data?.message || error.message || "Không thể đóng ca."), "error");
  } finally {
    isSubmitting.value = false;
  }
};

// ─── Logout ────────────────────────────────────────────────────────────────

const handleDangXuat = () => {
  localStorage.removeItem("role");
  localStorage.removeItem("userId");
  localStorage.removeItem("userEmail");
  router.push("/auth/staff-login");
};

// ─── Lifecycle ─────────────────────────────────────────────────────────────

onMounted(() => {
  loadData();
  timer = setInterval(() => {
    now.value = new Date();
    evaluateHandoverReminder();
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
  if (toastTimeout) clearTimeout(toastTimeout);
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════
   PAGE
═══════════════════════════════════════════════ */
.gct-page {
  min-height: 100vh;
  background: #fff;
  width: 100%;
  font-family: "Be Vietnam Pro", "Segoe UI", Tahoma, sans-serif;
  color: #374151;
  display: flex;
  flex-direction: column;
}

/* ═══════════════════════════════════════════════
   LOADING
═══════════════════════════════════════════════ */
.gct-loading {
  position: fixed;
  inset: 0;
  background: #fff;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #6b7280;
  font-size: 0.95rem;
}

.gct-spinner {
  width: 36px;
  height: 36px;
  border: 4px solid #e5e7eb;
  border-top-color: #b11226;
  border-radius: 50%;
  animation: gct-spin 0.8s linear infinite;
}

@keyframes gct-spin {
  to { transform: rotate(360deg); }
}

/* ═══════════════════════════════════════════════
   OVERLAY & REMINDER MODAL
═══════════════════════════════════════════════ */
.gct-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 900;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gct-reminder {
  background: #fff;
  border-radius: 10px;
  width: min(460px, 92vw);
  padding: 24px;
  border-top: 4px solid #ef4444;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

.gct-reminder h3 {
  margin: 0 0 10px;
  color: #b91c1c;
  font-size: 1.1rem;
}

.gct-reminder p {
  margin: 0 0 18px;
  color: #374151;
  line-height: 1.5;
  font-size: 0.95rem;
}

.gct-reminder-btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.r-btn-later {
  border: 1px solid #d1d5db;
  background: #f9fafb;
  color: #374151;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.r-btn-now {
  border: none;
  background: #dc2626;
  color: #fff;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

/* ═══════════════════════════════════════════════
   CENTER WRAPPER (for Mở ca / No shift cards)
═══════════════════════════════════════════════ */
.gct-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  background: #fff;
}

/* ═══════════════════════════════════════════════
   MỞ CA CARD
═══════════════════════════════════════════════ */
.gct-mo-ca {
  background: #fff;
  border-radius: 10px;
  width: min(520px, 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.gct-mo-ca-header {
  padding: 20px 28px 0;
}

.gct-mo-ca-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #111827;
}

.gct-mo-ca-desc {
  margin: 8px 28px 0;
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.5;
}

.gct-mo-ca-form {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.gct-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gct-label {
  font-size: 0.85rem;
  color: #374151;
  font-weight: 500;
}

.gct-input {
  border: none;
  border-bottom: 1px solid #d1d5db;
  padding: 8px 0;
  font-size: 0.95rem;
  color: #111827;
  background: transparent;
  outline: none;
  width: 100%;
  transition: border-color 0.2s;
}

.gct-input:focus {
  border-bottom-color: #b11226;
}

.gct-input-readonly {
  color: #6b7280;
  cursor: default;
}

.gct-mo-ca-footer {
  padding: 16px 28px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid #f3f4f6;
}

.btn-dang-xuat {
  border: 1px solid #111827;
  color: #111827;
  background: #fff;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-dang-xuat:hover {
  background: #f3f4f6;
}

.btn-mo-ca {
  background: #b11226;
  color: #fff;
  border: none;
  padding: 9px 24px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.2s;
}

.btn-mo-ca:hover:not(:disabled) {
  background: #8f0e1f;
}

.btn-mo-ca:disabled {
  background: #e4a4ad;
  cursor: not-allowed;
}

/* ═══════════════════════════════════════════════
   NO SHIFT CARD
═══════════════════════════════════════════════ */
.gct-no-shift {
  background: #fff;
  border-radius: 10px;
  width: min(480px, 100%);
  padding: 48px 32px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.gct-no-icon {
  font-size: 3rem;
  color: #d1d5db;
}

.gct-no-shift h3 {
  margin: 0;
  color: #374151;
  font-size: 1.1rem;
}

.gct-no-shift p {
  margin: 0;
  color: #9ca3af;
  font-size: 0.9rem;
  line-height: 1.5;
}

.gct-no-shift-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.gct-no-shift-actions button {
  margin-top: 0;
  min-height: 36px;
}

.btn-retry {
  background: #b11226;
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-retry:hover {
  background: #8f0e1f;
}

.btn-demo-access {
  background: #111827;
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-demo-access:hover {
  background: #1f2937;
}

/* ═══════════════════════════════════════════════
   ERROR MESSAGE
═══════════════════════════════════════════════ */
.gct-error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  padding: 10px 14px;
  color: #dc2626;
  font-size: 0.88rem;
}

.gct-error-body {
  margin: 0 0 12px;
}

/* ═══════════════════════════════════════════════
   ĐÓNG CA PANEL
═══════════════════════════════════════════════ */
.gct-dong-ca-wrap {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 28px 16px;
  background: #fff;
}

.gct-dong-ca {
  background: #fff;
  border-radius: 10px;
  width: min(1050px, 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* ── Header ── */
.gct-dc-header {
  padding: 18px 28px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.gct-dc-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.gct-dc-header h2 {
  margin: 0 0 6px;
  font-size: 1.15rem;
  font-weight: 700;
  color: #111827;
}

.gct-ca-code {
  color: #b11226;
}

.gct-dc-meta {
  display: flex;
  gap: 32px;
  font-size: 0.88rem;
  color: #6b7280;
}

.gct-dc-meta b {
  color: #374151;
}

/* ── Section base ── */
.gct-dc-section {
  border-bottom: 1px solid #f0f0f0;
}

.gct-dc-sec-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 28px;
  background: #fafafa;
}

.sec-label {
  font-weight: 700;
  font-size: 0.95rem;
  color: #374151;
}

.sec-value {
  font-size: 0.9rem;
  color: #374151;
}

.sec-value b {
  color: #111827;
}

.sec-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #b11226;
  font-size: 0.88rem;
}

.sec-hint i {
  font-size: 0.85rem;
}

/* ── Table (Trong ca) ── */
.gct-table-wrap {
  padding: 0 28px 16px;
  overflow-x: auto;
}

.gct-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
  min-width: 600px;
}

.gct-table thead tr {
  background: #f5f6fa;
}

.gct-table th {
  padding: 10px 14px;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border: 1px solid #e8eaed;
  white-space: nowrap;
  line-height: 1.4;
}

.gct-table th small {
  display: block;
  font-weight: 400;
  color: #9ca3af;
  font-size: 0.8rem;
}

.gct-table td {
  padding: 9px 14px;
  border: 1px solid #e8eaed;
  color: #374151;
}

.th-item {
  width: 140px;
}

.td-item {
  color: #6b7280;
  font-size: 0.85rem;
}

.td-red {
  color: #ef4444;
}

.tr-total td {
  background: #f9fafb;
  border-top: 1px solid #d1d5db;
}

/* ── Cuối ca ── */
.gct-cuoi-ca-row {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr;
  gap: 20px;
  padding: 16px 28px 20px;
}

.gct-cuoi-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* ── Footer ── */
.gct-dc-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
  gap: 10px;
}

.gct-dc-footer-right {
  display: flex;
  gap: 10px;
}

.btn-cap-nhat {
  border: none;
  background: transparent;
  color: #b11226;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
}

.btn-cap-nhat:hover:not(:disabled) {
  color: #8f0e1f;
}

.btn-cap-nhat:disabled {
  color: #d89da6;
  cursor: not-allowed;
}

.btn-dong-ca {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-dong-ca:hover:not(:disabled) {
  background: #f3f4f6;
}

.btn-dong-ca:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-dong-in {
  background: #b11226;
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-dong-in:hover:not(:disabled) {
  background: #8f0e1f;
}

.btn-dong-in:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ═══════════════════════════════════════════════
   SHARED FORM HELPERS
═══════════════════════════════════════════════ */
.req {
  color: #ef4444;
}

/* ═══════════════════════════════════════════════
   TOAST
═══════════════════════════════════════════════ */
.gct-toast {
  position: fixed;
  top: 80px;
  right: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: center;
  gap: 10px;
  border-left: 4px solid #111827;
  z-index: 9999;
  min-width: 260px;
  font-size: 0.9rem;
  color: #374151;
}

.gct-toast-success { border-left-color: #111827; }
.gct-toast-success .gct-toast-icon { color: #111827; }

.gct-toast-error { border-left-color: #dc2626; }
.gct-toast-error .gct-toast-icon { color: #dc2626; }

.gct-toast-warning { border-left-color: #d97706; }
.gct-toast-warning .gct-toast-icon { color: #d97706; }

.gct-toast-icon {
  color: #111827;
  font-size: 1.1rem;
}

.gct-toast-close {
  margin-left: auto;
  border: none;
  background: transparent;
  font-size: 1.1rem;
  cursor: pointer;
  color: #9ca3af;
  line-height: 1;
}

.gct-toast-slide-enter-active,
.gct-toast-slide-leave-active {
  transition: all 0.3s ease;
}

.gct-toast-slide-enter-from,
.gct-toast-slide-leave-to {
  opacity: 0;
  transform: translateX(60px);
}

/* ═══════════════════════════════════════════════
   RESPONSIVE
═══════════════════════════════════════════════ */
@media (max-width: 640px) {
  .gct-cuoi-ca-row {
    grid-template-columns: 1fr;
  }

  .gct-dc-meta {
    flex-direction: column;
    gap: 4px;
  }

  .gct-dc-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .gct-dc-footer-right {
    flex-direction: column;
  }

  .btn-dong-ca,
  .btn-dong-in {
    width: 100%;
    text-align: center;
  }
}

/* ═══════════════════════════════════════════════
   24H OVERDUE BANNER
═══════════════════════════════════════════════ */
.gct-overdue-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fef2f2;
  border-bottom: 2px solid #ef4444;
  padding: 12px 28px;
  color: #b91c1c;
  font-size: 0.9rem;
  font-weight: 500;
}

.gct-overdue-banner i {
  font-size: 1.1rem;
  color: #ef4444;
}

/* ═══════════════════════════════════════════════
   PHIEU MODAL
═══════════════════════════════════════════════ */
.gct-phieu-modal {
  background: #fff;
  border-radius: 10px;
  width: min(440px, 92vw);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.gct-phieu-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.gct-phieu-modal-header h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #111827;
}

.gct-phieu-modal-close {
  border: none;
  background: transparent;
  font-size: 1.5rem;
  cursor: pointer;
  color: #9ca3af;
  line-height: 1;
}

.gct-phieu-modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.gct-phieu-modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid #f0f0f0;
}

.gct-select {
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 8px 34px 8px 10px;
  font-size: 0.95rem;
}

/* ═══════════════════════════════════════════════
   PHIEU TABLE HEADER BUTTONS
═══════════════════════════════════════════════ */
.th-phieu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.btn-add-phieu {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 6px;
  border: none;
  background: #b11226;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.2px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.btn-add-phieu i {
  font-size: 0.65rem;
}

.btn-add-phieu:hover {
  background: #8f0e1f;
  box-shadow: 0 2px 8px rgba(177, 18, 38, 0.25);
}

.btn-add-phieu-chi {
  background: #374151;
}

.btn-add-phieu-chi:hover {
  background: #111827;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

/* ═══════════════════════════════════════════════
   PHIEU LIST
═══════════════════════════════════════════════ */
.gct-phieu-list-wrap {
  padding: 0 28px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gct-phieu-list-title {
  font-size: 0.85rem;
  font-weight: 600;
  margin: 0 0 6px;
  color: #374151;
}

.gct-phieu-thu-title { color: #111827; }
.gct-phieu-chi-title { color: #dc2626; }

.gct-phieu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 0.85rem;
}

.gct-phieu-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.gct-phieu-badge-thu {
  background: #f3f4f6;
  color: #111827;
}

.gct-phieu-badge-chi {
  background: #fee2e2;
  color: #991b1b;
}

.gct-phieu-method {
  color: #6b7280;
  min-width: 60px;
}

.gct-phieu-amount {
  font-weight: 600;
  color: #111827;
  min-width: 80px;
}

.gct-phieu-amount-chi {
  color: #dc2626;
}

.gct-phieu-reason {
  flex: 1;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gct-phieu-del {
  border: none;
  background: transparent;
  color: #d1d5db;
  font-size: 1.1rem;
  cursor: pointer;
  line-height: 1;
  padding: 0 2px;
}

.gct-phieu-del:hover {
  color: #ef4444;
}

/* ═══════════════════════════════════════════════
   BÁO CÁO CHỐT CA (iPOS style)
═══════════════════════════════════════════════ */
.gct-report {
  padding: 16px 28px 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 28px;
}

.gct-report-block {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fafbfc;
}

.gct-report-bangiao {
  grid-column: 1 / -1;
  background: #fff5f6;
  border-color: #f2c4cb;
}

.gct-report-title {
  margin: 0 0 10px;
  font-size: 0.82rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #b11226;
  border-left: 3px solid #b11226;
  padding-left: 8px;
}

.gct-report-thu {
  color: #b11226;
  border-left-color: #b11226;
}

.gct-report-chi {
  color: #dc2626;
  border-left-color: #dc2626;
}

.gct-report-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 0.88rem;
  color: #374151;
}

.gct-report-row span {
  color: #6b7280;
}

.gct-report-row b {
  color: #111827;
}

.gct-report-highlight {
  margin-top: 6px;
  padding-top: 8px;
  border-top: 2px solid #b11226;
  font-size: 0.95rem;
}

.gct-report-highlight span {
  font-weight: 600;
  color: #b11226;
}

.gct-report-highlight b {
  color: #b11226;
  font-size: 1.05rem;
}

@media (max-width: 640px) {
  .gct-report {
    grid-template-columns: 1fr;
  }
  .gct-report-bangiao {
    grid-column: 1;
  }
}
</style>
