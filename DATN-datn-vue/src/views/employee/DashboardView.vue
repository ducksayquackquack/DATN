<template>
  <div class="card" style="margin-bottom:14px">
    <div class="head">
      <div>
        <h1>Thống kê</h1>
        <small class="muted">Tổng quan vận hành hôm nay</small>
      </div>

      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn" @click="toast('Demo: refresh')">
          ↻ Làm mới
        </button>

        <router-link
          class="btn primary"
          to="/employee/hoa-don/detail/HD1002"
        >
          + Tạo hoá đơn
        </router-link>
      </div>
    </div>

    <div class="body">

      <!-- KPI -->
      <div class="kpi">
        <div class="card">
          <div class="body">
            <div class="muted">Đơn mới</div>
            <div class="num">18</div>
            <div class="sub">+12% so với hôm qua</div>
          </div>
        </div>

        <div class="card">
          <div class="body">
            <div class="muted">Doanh thu</div>
            <div class="num">24.5M</div>
            <div class="sub">Chưa trừ hoàn/huỷ</div>
          </div>
        </div>

        <div class="card">
          <div class="body">
            <div class="muted">Chờ xác nhận</div>
            <div class="num">6</div>
            <div class="sub">Cần xử lý trong 30p</div>
          </div>
        </div>

        <div class="card">
          <div class="body">
            <div class="muted">Tồn kho thấp</div>
            <div class="num">9</div>
            <div class="sub">SKU dưới ngưỡng</div>
          </div>
        </div>
      </div>

      <!-- TABLE SECTION -->
      <div style="margin-top:14px" class="grid cols2">

        <!-- ĐƠN MỚI -->
        <div class="card" style="border-radius:14px">
          <div class="head">
            <h2>Đơn mới cần xử lý</h2>
            <router-link class="btn" to="/employee/hoa-don/list">
              Xem tất cả →
            </router-link>
          </div>

          <div class="body">
            <table class="table">
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Khách</th>
                  <th>Trạng thái</th>
                  <th class="right">Tổng</th>
                  <th class="right">Action</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><b>HD1012</b></td>
                  <td>Nguyễn Văn A</td>
                  <td><span class="pill warn">● Chờ xác nhận</span></td>
                  <td class="right">1.250.000₫</td>
                  <td class="right">
                    <div class="actions">
                      <button class="iconbtn" @click="toast('Demo: nhận xử lý')">✅</button>
                      <router-link
                        class="iconbtn"
                        to="/employee/hoa-don/detail/HD1012"
                      >
                        👁️
                      </router-link>
                    </div>
                  </td>
                </tr>

                <tr>
                  <td><b>HD1011</b></td>
                  <td>Trần Thị B</td>
                  <td><span class="pill warn">● Chờ xác nhận</span></td>
                  <td class="right">699.000₫</td>
                  <td class="right">
                    <div class="actions">
                      <button class="iconbtn" @click="toast('Demo: nhận xử lý')">✅</button>
                      <router-link
                        class="iconbtn"
                        to="/employee/hoa-don/detail/HD1011"
                      >
                        👁️
                      </router-link>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- TỒN KHO -->
        <div class="card" style="border-radius:14px">
          <div class="head">
            <h2>Tồn kho thấp</h2>
            <router-link class="btn" to="/employee/san-pham/list">
              Đi tới sản phẩm →
            </router-link>
          </div>

          <div class="body">
            <table class="table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Sản phẩm</th>
                  <th class="right">Tồn</th>
                  <th class="right">Action</th>
                </tr>
              </thead>

              <tbody>
                <tr>
                  <td>SP002</td>
                  <td>
                    <b>Jeans slim co giãn</b>
                    <div class="muted" style="font-size:12px">
                      Xanh • 30
                    </div>
                  </td>
                  <td class="right">
                    <span class="pill bad">0</span>
                  </td>
                  <td class="right">
                    <button class="iconbtn" @click="toast('Demo: nhập kho')">
                      📦
                    </button>
                  </td>
                </tr>

                <tr>
                  <td>SP015</td>
                  <td>
                    <b>Polo basic</b>
                    <div class="muted" style="font-size:12px">
                      Đen • L
                    </div>
                  </td>
                  <td class="right">
                    <span class="pill warn">3</span>
                  </td>
                  <td class="right">
                    <button class="iconbtn" @click="toast('Demo: tạo yêu cầu nhập')">
                      📝
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div class="grid cols2" style="margin-top:12px">
              <button class="btn" @click="toast('Demo: xuất báo cáo')">
                ⬇️ Xuất báo cáo
              </button>

              <button class="btn primary" @click="toast('Demo: tạo phiếu nhập')">
                + Tạo phiếu nhập
              </button>
            </div>
          </div>
        </div>

      </div>

      <!-- CHECKLIST + NOTE -->
      <div class="grid cols2" style="margin-top:14px">

        <div class="card" style="border-radius:14px">
          <div class="head">
            <h2>Việc cần làm</h2>
            <small class="muted">Checklist ca</small>
          </div>

          <div class="body">
            <div class="card" style="border-radius:14px; margin-bottom:10px">
              <div class="body">
                <b>Gọi xác nhận 6 đơn</b>
                <div class="muted">Trước 11:00</div>
              </div>
            </div>

            <div class="card" style="border-radius:14px; margin-bottom:10px">
              <div class="body">
                <b>Soạn hàng đơn HD1012</b>
                <div class="muted">In tem vận đơn</div>
              </div>
            </div>

            <button class="btn" @click="toast('Demo: thêm việc')">
              + Thêm việc
            </button>
          </div>
        </div>

        <div class="card" style="border-radius:14px">
          <div class="head">
            <h2>Ghi chú ca</h2>
            <small class="muted">Nội bộ</small>
          </div>

          <div class="body">
            <label>Note</label>
            <textarea
              v-model="note"
              placeholder="Ghi chú bàn giao..."
            ></textarea>

            <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:10px">
              <button class="btn primary" @click="toast('Demo: lưu note')">
                Lưu
              </button>

              <button class="btn" @click="toast('Demo: gửi quản lý')">
                Gửi quản lý
              </button>
            </div>
          </div>
        </div>

      </div>

      <div style="margin-top:14px" class="muted">
        © {{ year }} Employee Panel
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const year = new Date().getFullYear()
const note = ref('')

function toast(msg) {
  window.toast.info(msg)
}
</script>