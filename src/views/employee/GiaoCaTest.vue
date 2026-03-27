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
            <button class="btn-mo-ca" @click="handleBatDauCa" :disabled="isSubmitting">
              <i v-if="isSubmitting" class="fa-solid fa-spinner fa-spin"></i>
              {{ isSubmitting ? 'Đang xử lý...' : 'Mở ca' }}
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
          <button class="btn-retry" @click="loadData">
            <i class="fa-solid fa-arrows-rotate"></i> Kiểm tra lại
          </button>
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
                    Phiếu thu
                    <small>0 phiếu</small>
                  </th>
                  <th>
                    Phiếu chi
                    <small>0 phiếu</small>
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
                  <td>0</td>
                  <td class="td-red">0</td>
                  <td class="td-red">0</td>
                </tr>
                <tr>
                  <td class="td-item">2. Chuyển khoản</td>
                  <td>{{ transferRevenue > 0 ? formatNumber(transferRevenue) : 0 }}</td>
                  <td>0</td>
                  <td class="td-red">0</td>
                  <td class="td-red">0</td>
                </tr>
                <tr>
                  <td class="td-item">3. Thẻ</td>
                  <td>0</td>
                  <td>0</td>
                  <td class="td-red">0</td>
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
                  <td><b>0</b></td>
                  <td class="td-red"><b>0</b></td>
                  <td class="td-red"><b>0</b></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- ── CUỐI CA ── -->
        <div class="gct-dc-section">
          <div class="gct-dc-sec-head">
            <span class="sec-label">Cuối ca</span>
            <span class="sec-value sec-hint">
              <i class="fa-regular fa-circle-question"></i>
              Tiền mặt: <b>{{ formatNumber(tinhTongLyThuyet) }}</b>
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
      <div v-if="showToast" class="gct-toast">
        <i class="fa-solid fa-circle-check gct-toast-icon"></i>
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
let toastTimeout = null;

const tienBanDauInput = ref(0);
const ghiChuBanDau = ref("");
const tienThucTeInput = ref(0);
const ghiChuInput = ref("");

const now = ref(new Date());
let timer = null;

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
  if (!caHienTai.value) return 0;
  return (caHienTai.value.tienDauCaNhap || 0) + (cashRevenue.value + codRevenue.value);
});

const chenhLech = computed(() => {
  return tienThucTeInput.value - tinhTongLyThuyet.value;
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

const triggerToast = (msg) => {
  toastMessage.value = msg;
  showToast.value = true;
  if (toastTimeout) clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => {
    showToast.value = false;
  }, 3000);
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

// ─── Load Data ─────────────────────────────────────────────────────────────

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
    triggerToast("Đã cập nhật dữ liệu thành công!");
  } catch {
    errorMessage.value = "Không thể cập nhật dữ liệu. Vui lòng thử lại.";
  } finally {
    isRefreshing.value = false;
  }
};

// ─── Open Shift ────────────────────────────────────────────────────────────

const handleBatDauCa = async () => {
  errorMessage.value = "";
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
    errorMessage.value = "Lỗi: " + (error.response?.data?.message || error.message || "Không thể đóng ca.");
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
  background: #f0f2f5;
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
  border-top-color: #1677ff;
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
  border-bottom-color: #1677ff;
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
  border: 1px solid #1677ff;
  color: #1677ff;
  background: #fff;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-dang-xuat:hover {
  background: #e6f4ff;
}

.btn-mo-ca {
  background: #1677ff;
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
  background: #0958d9;
}

.btn-mo-ca:disabled {
  background: #93c5fd;
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

.btn-retry {
  background: #1677ff;
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-retry:hover {
  background: #0958d9;
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
  color: #1677ff;
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
  color: #1677ff;
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
  color: #1677ff;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
}

.btn-cap-nhat:hover:not(:disabled) {
  color: #0958d9;
}

.btn-cap-nhat:disabled {
  color: #93c5fd;
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
  background: #1677ff;
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
  background: #0958d9;
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
  border-left: 4px solid #10b981;
  z-index: 9999;
  min-width: 260px;
  font-size: 0.9rem;
  color: #374151;
}

.gct-toast-icon {
  color: #10b981;
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
</style>
