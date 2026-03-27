<template>
  <div class="giao-ca-container">
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
      <span>Đang tải dữ liệu ...</span>
    </div>

    <div v-if="showHandoverReminder" class="modal-overlay">
      <div class="handover-alert-modal">
        <h3>Đã tới giờ bàn giao ca</h3>
        <p>
          Ca của bạn đã tới thời điểm kết thúc nhưng chưa xác nhận bàn giao.
          Vui lòng hoàn tất bàn giao ca để tránh lệch số liệu.
        </p>
        <div class="handover-actions">
          <button class="btn-later" @click="dismissHandoverReminder">Để sau</button>
          <button class="btn-now" @click="showHandoverReminder = false">Bàn giao ngay</button>
        </div>
      </div>
    </div>

    <div v-if="!loading && !caHienTai && showStartForm && lichHomNay" class="start-panel-wrap">
      <div class="start-shift-modal">
        <div class="ss-header">
          <div class="ss-header-title">
            <i class="fa-regular fa-clock"></i>
            <span>MỞ CA LÀM VIỆC</span>
          </div>
          <button class="ss-close" @click="showStartForm = false" title="Ẩn biểu mẫu" aria-label="Đóng biểu mẫu">
            <span class="ss-close-glyph">&times;</span>
          </button>
          <div class="ss-header-sub">{{ formattedFullTime }}</div>
        </div>

        <div class="ss-body">
          <div class="ss-schedule-box">
            <div class="ss-schedule-label">LỊCH CỦA BẠN</div>
            <div class="ss-schedule-info">
              <span class="ss-shift-name">{{
                lichHomNay?.tenCa || "Chưa có lịch"
                }}</span>
              <span class="ss-shift-time" v-if="lichHomNay">
                {{ lichHomNay.gioBatDau }} - {{ lichHomNay.gioKetThuc }}
              </span>
            </div>
          </div>

          <div class="ss-user-row">
            <div class="ss-user-info">
              <i class="fa-solid fa-circle-user" style="color: #9ca3af; font-size: 1.2rem"></i>
              <strong>{{
                currentUser?.tenNhanVien || currentUser?.hoTen || "Nhân viên"
                }}</strong>
            </div>
            <div class="ss-current-time">{{ formattedTimeOnly }}</div>
          </div>

          <div class="form-group">
            <div class="d-flex justify-between mb-2">
              <label class="text-label">TIỀN MẶT ĐẦU CA <span class="text-danger">*</span></label>
            </div>
            <div class="input-money-wrapper new-input-style">
              <span class="currency-prefix">₫</span>
              <input type="text" class="form-control money-input-new" :value="formatNumber(tienBanDauInput)"
                @input="onInputMoney($event, 'start')" placeholder="0" />
            </div>
          </div>

          <div class="form-group mt-3">
            <label class="text-label mb-2">GHI CHÚ</label>
            <textarea v-model="ghiChuBanDau" class="form-control note-textarea" rows="2"
              placeholder="Nhập ghi chú tại đây..."></textarea>
          </div>

          <div v-if="errorMessage" class="error-box">
            <i class="fa-solid fa-circle-exclamation"></i> {{ errorMessage }}
          </div>
        </div>

        <div class="ss-footer">
          <button class="btn-confirm" @click="handleBatDauCa" :disabled="isSubmitting || !lichHomNay">
            <i v-if="isSubmitting" class="fa-solid fa-spinner fa-spin"></i>
            <i v-else class="fa-solid fa-circle-check"></i>
            XÁC NHẬN VÀO CA
          </button>
        </div>
      </div>
    </div>

    <div v-if="!loading && !caHienTai && !showStartForm && lichHomNay" class="start-mini-card">
      <div>
        <strong>Biểu mẫu mở ca đang được ẩn</strong>
        <p>Nhấn nút bên dưới để mở lại biểu mẫu vào ca khi cần thao tác.</p>
      </div>
      <button class="btn-reopen" @click="showStartForm = true">Mở lại biểu mẫu</button>
    </div>

    <div v-if="!loading && !caHienTai && !lichHomNay" class="start-mini-card no-schedule-card">
      <div>
        <strong>Hiện tại bạn chưa có ca hợp lệ để mở</strong>
        <p>{{ errorMessage || "Bạn chỉ có thể mở ca khi đang trong khung giờ làm việc đã được phân công." }}</p>
      </div>
    </div>

    <div v-if="!loading && caHienTai" class="ho-wrapper">
      <div class="ho-header">
        <div class="ho-header-left">
          <div class="ho-icon-box">
            <i class="fa-solid fa-file-invoice"></i>
          </div>
          <div class="ho-header-text">
            <h2>Phiếu Bàn Giao Ca</h2>
            <span class="ho-sub-text">
              #{{
                caHienTai.id
                  ? caHienTai.id.toString().substring(0, 8)
                  : "cccadd2e"
              }}
              • {{ formattedFullTime }}
            </span>
          </div>
        </div>
        <div class="ho-header-right">
          <div class="ho-badge">
            <span class="ho-role">NHÂN VIÊN TRỰC</span>
            <span class="ho-name">{{
              currentUser?.tenNhanVien || currentUser?.hoTen || "Nhân viên"
              }}</span>
          </div>
        </div>
      </div>

      <div class="ho-body">
        <div class="ho-card ho-left-col">
          <h3 class="ho-title">Tài chính trong ca</h3>

          <div class="ho-row mt-3">
            <span class="ho-label-text">Tiền mặt đầu ca</span>
            <span class="ho-val-text font-bold">{{ formatNumber(caHienTai.tienDauCaNhap) }} ₫</span>
          </div>

          <div class="ho-row">
            <span class="ho-label-text">Doanh thu Tiền mặt</span>
            <span class="ho-val-text text-success font-bold">+{{ formatNumber(caHienTai.tongTienTrongCa) }} ₫</span>
          </div>

          <div class="ho-row">
            <span class="ho-label-text">Doanh thu CK / Thẻ</span>
            <span class="ho-val-text text-primary font-bold">{{ tongTienNgoCash > 0 ? '+' : '' }}{{ formatNumber(tongTienNgoCash) }} ₫</span>
          </div>

          <div class="ho-divider">
            <i class="fa-solid fa-receipt"></i> Đã thanh toán:
            <strong>{{ caHienTai.soDonHangDaThanhToan || 0 }}</strong> đơn
          </div>

          <div class="ho-summary-box">
            <div class="ho-summary-title">TỔNG TIỀN MẶT LÝ THUYẾT</div>
            <div class="ho-summary-value">
              {{ formatNumber(tinhTongLyThuyet) }} ₫
            </div>
          </div>
        </div>

        <div class="ho-card ho-right-col">
          <h3 class="ho-title">
            <i class="fa-solid fa-clipboard-check text-success-icon"></i> Kiểm
            kê & Xác nhận
          </h3>

          <div class="form-group mt-3">
            <label class="ho-input-label">NHẬP TIỀN THỰC TẾ <span class="text-danger">*</span></label>
            <div class="ho-input-wrapper">
              <input type="text" class="ho-input-money" :value="formatNumber(tienThucTeInput)"
                @input="onInputMoney($event, 'end')" placeholder="0" />
              <span class="ho-currency-right">₫</span>
            </div>
          </div>

          <div class="ho-diff-box" :class="chenhLech >= 0
              ? chenhLech === 0
                ? 'bg-gray'
                : 'bg-success-light'
              : 'bg-danger-light'
            ">
            <div class="diff-left">
              <i v-if="chenhLech < 0" class="fa-solid fa-circle-exclamation text-danger"></i>
              <i v-else-if="chenhLech > 0" class="fa-solid fa-circle-check text-success"></i>
              <i v-else class="fa-solid fa-check text-gray"></i>
              <span class="diff-text" :class="chenhLech >= 0
                  ? chenhLech === 0
                    ? 'text-gray'
                    : 'text-success'
                  : 'text-danger'
                ">
                {{
                  chenhLech < 0 ? "Thiếu hụt" : chenhLech > 0
                    ? "Thừa tiền"
                    : "Khớp"
                }}
              </span>
            </div>
            <span class="diff-val" :class="chenhLech >= 0
                ? chenhLech === 0
                  ? 'text-gray'
                  : 'text-success'
                : 'text-danger'
              ">
              {{ chenhLech > 0 ? "+" : "" }}{{ formatNumber(chenhLech) }} ₫
            </span>
          </div>

          <div class="form-group mt-4">
            <label class="ho-input-label">GHI CHÚ</label>
            <textarea v-model="ghiChuInput" class="ho-textarea" rows="2"
              placeholder="Nhập lý do chênh lệch tiền..."></textarea>
          </div>

          <button class="btn-submit-end" @click="submitKetThucCa" :disabled="isSubmitting">
            {{ isSubmitting ? "ĐANG XỬ LÝ..." : "XÁC NHẬN ĐÓNG CA" }}
          </button>
          <div class="ho-footer-note">
            Sau khi đóng ca, bạn vẫn ở lại hệ thống để tiếp tục thao tác.
          </div>
        </div>
      </div>
    </div>

    <div v-if="showToast" class="toast-notification">
      <div class="toast-icon">
        <i class="fa-solid fa-circle-check"></i>
      </div>
      <div class="toast-body">
        <h4 class="toast-title">Thành công</h4>
        <p class="toast-msg">{{ toastMessage }}</p>
      </div>
      <div class="toast-close" @click="showToast = false">
        <i class="fa-solid fa-xmark"></i>
      </div>
      <div class="toast-progress"></div>
    </div>
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

const router = useRouter();
const emit = defineEmits(["ca-started"]);

const ACTIVE_SHIFT_PREFIX = "giao-ca-active";
const currentUser = ref(null);
const idNhanVien = ref(null);

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  return [];
};

const normalizeDateKey = (value) => {
  if (!value) return "";
  // Handle LocalDate serialized as Java array [2026, 3, 15]
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
      // Fallback below.
    }
  }

  const allRes = await getAllNhanVien();
  const allNhanVien = extractList(allRes?.data);
  const taiKhoanIdNum = Number(localStorage.getItem("userId") || 0);

  const fallback = allNhanVien.find((item) => {
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

const loading = ref(true);
const isSubmitting = ref(false);
const tongTienNgoCash = ref(0);
const caHienTai = ref(null);
const lichHomNay = ref(null);
const lichSuCaHienTai = ref(null);
const showStartForm = ref(true);
const showHandoverReminder = ref(false);
const errorMessage = ref("");
const showToast = ref(false);
const toastMessage = ref("");
let toastTimeout = null;

const tienBanDauInput = ref(0);
const ghiChuBanDau = ref("");
const tienThucTeInput = ref(0);
const ghiChuInput = ref("");

const now = ref(new Date());
let timer = null;

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

// Fetches actual cash & non-cash revenue from HoaDon for an employee on a given date.
const fetchShiftRevenue = async (employeeId, dateKey) => {
  try {
    const res = await getAllHoaDon();
    const orders = extractList(res?.data);
    let cashRevenue = 0;
    let nonCashRevenue = 0;
    let orderCount = 0;
    for (const o of orders) {
      if (resolveInvoiceEmployeeId(o) !== Number(employeeId)) continue;
      if (!isRevenueCountableOrder(o)) continue;

      const revenueBucket = getRevenueBucket(o);
      if (revenueBucket === "unknown") continue;
      if (resolveRevenueDateKey(o, revenueBucket) !== dateKey) continue;

      const amount = Math.max(0, Number(o?.thanhTien || 0) - Number(o?.phiShip || 0));
      if (amount <= 0) continue;

      orderCount++;
      if (revenueBucket === "cash") {
        cashRevenue += amount;
      } else {
        nonCashRevenue += amount;
      }
    }
    return { cashRevenue, nonCashRevenue, orderCount };
  } catch {
    return { cashRevenue: 0, nonCashRevenue: 0, orderCount: 0 };
  }
};

const triggerToast = (msg) => {
  toastMessage.value = msg;
  showToast.value = true;
  if (toastTimeout) clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => {
    showToast.value = false;
  }, 3000);
};

const tinhTongLyThuyet = computed(() => {
  if (!caHienTai.value) return 0;
  const banDau = caHienTai.value.tienDauCaNhap || 0;
  const doanhThu = caHienTai.value.tongTienTrongCa || 0;
  return banDau + doanhThu;
});

const chenhLech = computed(() => {
  return tienThucTeInput.value - tinhTongLyThuyet.value;
});

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

const resolveRevenueDateKeyForShift = () => {
  const shiftDate = normalizeDateKey(caHienTai.value?.ngayLam || lichHomNay.value?.ngayLam);
  return shiftDate || getTodayDateKey();
};

const loadData = async () => {
  loading.value = true;
  errorMessage.value = "";

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

    // /full already has tenCa, gioBatDau, gioKetThuc joined — filter by employee
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
    showStartForm.value = true;
    showHandoverReminder.value = false;

    if (!lichHomNay.value) {
      clearActiveShift(idNhanVien.value);
      showStartForm.value = false;
      errorMessage.value = "Bạn chưa được phân công ca đang diễn ra. Vui lòng liên hệ admin.";
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
        errorMessage.value = "Ca hôm nay đã được đóng. Bạn có thể xem lịch sử ở trang quản lý.";
        return;
      }

      errorMessage.value = "Ca này đã từng được đóng sớm. Bạn có thể mở lại ca trong thời gian còn lại của khung giờ.";
    }

    if (
      activeShift &&
      Number(activeShift?.idLichLamViec) === Number(lichHomNay.value?.id) &&
      normalizeDateKey(activeShift?.ngayLam) === normalizeDateKey(lichHomNay.value?.ngayLam)
    ) {
      caHienTai.value = buildCurrentCa(activeShift, lichHomNay.value);
      tienThucTeInput.value = Math.max(tienThucTeInput.value, caHienTai.value.tienDauCaNhap || 0);
      evaluateHandoverReminder();
      const rev = await fetchShiftRevenue(idNhanVien.value, resolveRevenueDateKeyForShift());
      caHienTai.value.tongTienTrongCa = rev.cashRevenue;
      caHienTai.value.soDonHangDaThanhToan = rev.orderCount;
      tongTienNgoCash.value = rev.nonCashRevenue;
      const stored = loadActiveShift(idNhanVien.value);
      if (stored) {
        stored.tongTienTrongCa = rev.cashRevenue;
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

const handleBatDauCa = async () => {
  errorMessage.value = "";
  if (tienBanDauInput.value < 0) return alert("Tiền không hợp lệ");
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
      startedAt: new Date().toISOString(),
    };

    saveActiveShift(idNhanVien.value, activePayload);
    caHienTai.value = buildCurrentCa(activePayload, lichHomNay.value);
    tienThucTeInput.value = caHienTai.value.tienDauCaNhap || 0;
    showHandoverReminder.value = false;
    const rev = await fetchShiftRevenue(idNhanVien.value, resolveRevenueDateKeyForShift());
    caHienTai.value.tongTienTrongCa = rev.cashRevenue;
    caHienTai.value.soDonHangDaThanhToan = rev.orderCount;
    tongTienNgoCash.value = rev.nonCashRevenue;
    activePayload.tongTienTrongCa = rev.cashRevenue;
    activePayload.soDonHangDaThanhToan = rev.orderCount;
    saveActiveShift(idNhanVien.value, activePayload);

    triggerToast("Bắt đầu ca làm việc thành công!");
    emit("ca-started");
  } catch (error) {
    console.log("FULL ERROR:", error);
    errorMessage.value =
      error?.data?.message ||
      error?.message ||
      "Không thể bắt đầu ca vào lúc này.";
  } finally {
    isSubmitting.value = false;
  }
};

const submitKetThucCa = async () => {
  if (!caHienTai.value || !idNhanVien.value || !lichHomNay.value) return;

  const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes();
  const shiftEndMinutes = toMinutes(lichHomNay.value.gioKetThuc);
  const isEarlyClose = nowMinutes < shiftEndMinutes;

  if (!ghiChuInput.value.trim()) {
    const reasonMessage = isEarlyClose
      ? "Bạn đang đóng ca trước giờ kết thúc. Vui lòng nhập lý do vào ghi chú."
      : "Vui lòng nhập lý do đóng ca vào ghi chú.";
    alert(reasonMessage);
    return;
  }

  isSubmitting.value = true;
  try {
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
    const earlyCloseFlag = isEarlyClose
      ? `Đóng sớm lúc ${formattedTimeOnly.value} (kết ca ${normalizeTime(lichHomNay.value?.gioKetThuc)})`
      : "";
    const ghiChuTongHop = [
      caHienTai.value?.ghiChuBanDau ? `Đầu ca: ${caHienTai.value.ghiChuBanDau}` : "",
      `Tiền đầu ca: ${formatNumber(caHienTai.value?.tienDauCaNhap || 0)}đ`,
      `Tiền lý thuyết: ${formatNumber(tongLyThuyet)}đ`,
      `Tiền thực tế: ${formatNumber(tienThucTeInput.value)}đ`,
      `Chênh lệch: ${chenh >= 0 ? "+" : ""}${formatNumber(chenh)}đ`,
      earlyCloseFlag,
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
      doanhThu: caHienTai.value?.tongTienTrongCa || 0,
      tienChuyenKhoan: tongTienNgoCash.value || 0,
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
    await loadData();
  } catch (error) {
    alert("Lỗi: " + (error.response?.data?.message || error.message));
  } finally {
    isSubmitting.value = false;
  }
};

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
/* --- BASE STYLES --- */
.giao-ca-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: "Be Vietnam Pro", "Segoe UI", Tahoma, sans-serif;
  color: #374151;
}

.d-flex {
  display: flex;
}

.justify-between {
  justify-content: space-between;
}

.mb-2 {
  margin-bottom: 8px;
}

.mt-3 {
  margin-top: 16px;
}

.mt-4 {
  margin-top: 24px;
}

.font-bold {
  font-weight: 700;
}

.text-danger {
  color: #ef4444;
}

.text-success {
  color: #10b981;
}

.text-primary {
  color: #3b82f6;
}

.text-gray {
  color: #6b7280;
}

.ho-wrapper {
  width: 1100px;
  max-width: 98%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ho-header {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.ho-header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.ho-icon-box {
  background-color: #f7e7e6;
  color: #ff4d4f;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1.2rem;
}

.ho-header-text h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #111827;
}

.ho-sub-text {
  font-size: 0.85rem;
  color: #9ca3af;
}

.ho-header-right {
  display: flex;
  align-items: center;
}

.ho-badge {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  border: 1px solid #f3f4f6;
}

.ho-role {
  font-size: 0.7rem;
  font-weight: 700;
  color: #9ca3af;
  letter-spacing: 0.5px;
}

.ho-name {
  font-size: 0.95rem;
  color: #111827;
}

.ho-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.ho-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.ho-title {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 1.1rem;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 8px;
}

.text-success-icon {
  color: #ff4d4f;
  font-size: 1.2rem;
}

.ho-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
}

.ho-label-text {
  color: #6b7280;
  font-size: 0.95rem;
}

.ho-val-text {
  font-size: 1rem;
}

.ho-divider {
  text-align: center;
  color: #6b7280;
  font-size: 0.9rem;
  padding: 16px 0;
  border-top: 1px dashed #e5e7eb;
  border-bottom: 1px dashed #e5e7eb;
  margin: 10px 0 20px 0;
}

.ho-divider i {
  color: #9ca3af;
  margin-right: 5px;
}

.ho-summary-box {
  background: #fef0f0;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.ho-summary-title {
  color: #ff4d4f;
  font-weight: 500;
}

.ho-summary-value {
  color: #ff4d4f;
  font-weight: 500;
}

.ho-summary-note {
  color: #ff4d4f;
}

.ho-input-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #9ca3af;
  margin-bottom: 8px;
  display: block;
}

.ho-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  color: #9ca3af;
}

.ho-currency {
  position: absolute;
  left: 15px;
  color: #9ca3af;
  font-weight: 700;
  font-size: 1.2rem;
}

.ho-currency-right {
  position: absolute;
  right: 15px;
  color: #9ca3af;
  font-weight: 700;
  font-size: 1.2rem;
}

.ho-input-money {
  width: 100%;
  padding: 14px 40px 14px 40px;
  font-size: 1.2rem;
  font-weight: 700;
  border: 1px solid #e8ebf1;
  background: white;
  border-radius: 8px;
  text-align: right;
  color: #9ca3af;
}

.ho-input-money:focus {
  outline: none;
  border: 1px solid #ff4d4f;
}

.ho-diff-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 15px;
  font-weight: 700;
}

.diff-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
}

.diff-val {
  font-size: 1.05rem;
}

.bg-danger-light {
  background-color: #fef2f2;
}

.bg-success-light {
  background-color: #f0fdf4;
}

.bg-gray {
  background-color: #f3f4f6;
}

.ho-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 8px;
  resize: none;
  font-family: inherit;
  font-size: 0.95rem;
  color: #9ca3af;
}

.ho-textarea:focus {
  outline: none;
  border-color: #ff4d4f;
}

.btn-submit-end {
  width: 100%;
  background: linear-gradient(90deg, #ff4d4f 0%, #111827 100%);
  box-shadow: 0 10px 18px rgba(255, 77, 79, 0.16);
  color: white;
  border: none;
  padding: 15px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 1.05rem;
  cursor: pointer;
  margin-top: 25px;
  transition: 0.2s;
}

.btn-submit-end:hover {
  background: linear-gradient(90deg, #ff4d4f 0%, #111827 100%);
  box-shadow: 0 10px 18px rgba(255, 77, 80, 0.541);
}

.btn-submit-end:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.ho-footer-note {
  text-align: center;
  font-size: 0.8rem;
  color: #9ca3af;
  margin-top: 12px;
}

.start-panel-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 16px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.start-shift-modal {
  background: white;
  width: 620px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.ss-header {
  background: linear-gradient(90deg, #ff4d4f 0%, #111827 100%);
  color: white;
  padding: 20px 25px;
  text-align: left;
  position: relative;
}

.ss-close {
  position: absolute;
  right: 14px;
  top: 12px;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  cursor: pointer;
  display: grid;
  place-items: center;
  line-height: 1;
}

.ss-close-glyph {
  font-size: 18px;
  font-weight: 700;
  transform: translateY(-1px);
}

.ss-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ss-header-sub {
  font-size: 0.8rem;
  color: #a7f3d0;
  margin-top: 5px;
  margin-left: 30px;
}

.ss-body {
  padding: 25px;
}

.ss-schedule-box {
  background-color: #fdf0f0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 15px;
}

.ss-schedule-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #ff4d4f;
  margin-bottom: 6px;
}

.ss-schedule-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ss-shift-name {
  font-weight: 700;
  font-size: 1.1rem;
  color: #111827;
}

.ss-shift-time {
  background-color: #fadbd1;
  color: #ff4d4f;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.85rem;
}

.ss-user-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 20px;
  border-bottom: 1px dashed #e5e7eb;
  margin-bottom: 20px;
}

.ss-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #374151;
}

.ss-current-time {
  color: #6b7280;
  font-size: 0.9rem;
}

.text-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #9ca3af;
}

.new-input-style {
  position: relative;
}

.currency-prefix {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  font-weight: 600;
}

.money-input-new {
  width: 100%;
  padding: 12px 15px 12px 35px;
  font-size: 1.25rem;
  font-weight: 400;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  color: #111827;
}

.money-input-new:focus,
.note-textarea:focus {
  outline: none;
  border-color: #ff4d4f;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.note-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-family: inherit;
  resize: none;
}

.ss-footer {
  display: flex;
  padding: 20px 25px;
  gap: 15px;
  background: #fff;
  border-top: 1px solid #f3f4f6;
}

.start-mini-card {
  width: min(640px, 95%);
  margin: 28px auto 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.start-mini-card p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.btn-reopen {
  border: none;
  border-radius: 8px;
  padding: 10px 14px;
  color: #fff;
  background: linear-gradient(90deg, #ff4d4f 0%, #111827 100%);
  cursor: pointer;
  font-weight: 700;
}

.handover-alert-modal {
  background: #fff;
  border-radius: 12px;
  width: min(460px, 92%);
  padding: 18px;
  border: 1px solid #fee2e2;
}

.handover-alert-modal h3 {
  margin: 0;
  color: #b91c1c;
}

.handover-alert-modal p {
  margin: 10px 0 14px;
  color: #374151;
  line-height: 1.45;
}

.handover-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-later,
.btn-now {
  border: none;
  border-radius: 8px;
  padding: 9px 12px;
  font-weight: 700;
  cursor: pointer;
}

.btn-later {
  background: #e5e7eb;
  color: #374151;
}

.btn-now {
  background: #dc2626;
  color: #fff;
}

.btn-confirm {
  flex: 1;
  background: linear-gradient(90deg, #ff4d4f 0%, #111827 100%);
  color: white;
  border: none;
  padding: 12px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  transition: 0.2s;
}

.btn-confirm:hover {
  background-color: #047857;
}

.btn-confirm:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.error-box {
  margin-top: 15px;
  padding: 10px;
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  color: #dc2626;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: white;
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

/* Toast */
.toast-notification {
  position: fixed;
  top: 90px;
  right: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: flex-start;
  gap: 15px;
  border-left: 5px solid #10b981;
  z-index: 9999;
  animation: slideInLeft 0.5s ease forwards;
  min-width: 300px;
  overflow: hidden;
}

.toast-icon {
  font-size: 24px;
  color: #10b981;
  display: flex;
  align-items: center;
  height: 100%;
  margin-top: 2px;
}

.toast-body {
  flex: 1;
}

.toast-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.toast-msg {
  margin: 5px 0 0;
  font-size: 14px;
  color: #666;
}

.toast-close {
  cursor: pointer;
  font-size: 18px;
  color: #999;
  transition: 0.3s;
}

.toast-close:hover {
  color: #333;
}

.toast-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  width: 100%;
  background: #10b981;
  animation: progress 3s linear forwards;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(100%);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes progress {
  from {
    width: 100%;
  }

  to {
    width: 0%;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}
</style>