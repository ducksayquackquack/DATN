<script setup>
import { computed } from "vue"
import { Facebook, Instagram, Mail, MapPin, Phone, Youtube } from "lucide-vue-next"

const year = computed(() => new Date().getFullYear())

const quickLinks = [
  { label: "Chính sách bảo mật", href: "#" },
  { label: "Chính sách vận chuyển", href: "#" },
  { label: "Chính sách đổi trả", href: "#" },
  { label: "Quy định sử dụng", href: "#" },
]

const supportTimes = [
  "Thứ 2 - Thứ 6: 09:00 - 21:00",
  "Thứ 7 - Chủ nhật: 09:00 - 22:00",
  "Tư vấn online: 24/7",
]
</script>

<template>
  <footer class="cf-footer" id="policy">
    <div class="cf-container">
      <div class="cf-grid">
        <section class="cf-brand">
          <h3>DirtyWave</h3>
          <p>Streetwear cho nam theo phong cách tối giản, dễ mặc mỗi ngày.</p>

          <div class="cf-socials" aria-label="Kênh mạng xã hội">
            <a href="#" aria-label="Facebook">
              <Facebook :size="16" />
            </a>
            <a href="#" aria-label="Youtube">
              <Youtube :size="16" />
            </a>
            <a href="#" aria-label="TikTok">
              <span>Tk</span>
            </a>
            <a href="#" aria-label="Instagram">
              <Instagram :size="16" />
            </a>
          </div>
        </section>

        <section>
          <h4>Liên hệ</h4>
          <ul class="cf-list">
            <li>
              <Phone :size="15" />
              <span>Hotline: 1900 6789</span>
            </li>
            <li>
              <Mail :size="15" />
              <span>Email: support@dirtywave.vn</span>
            </li>
            <li>
              <MapPin :size="15" />
              <span>Số 1, Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</span>
            </li>
          </ul>
        </section>

        <section>
          <h4>Kết nối</h4>
          <ul class="cf-links">
            <li v-for="item in quickLinks" :key="item.label">
              <a :href="item.href">{{ item.label }}</a>
            </li>
          </ul>
        </section>

        <section>
          <h4>Giờ hỗ trợ</h4>
          <ul class="cf-schedule">
            <li v-for="slot in supportTimes" :key="slot">{{ slot }}</li>
          </ul>

          <div class="cf-note">
            <strong>Gợi ý nhanh:</strong>
            <span>Inbox fanpage để được tư vấn size và kiểm tra tồn kho theo mẫu.</span>
          </div>
        </section>
      </div>

      <div class="cf-bottom">
        <small>&copy; {{ year }} DirtyWave. All rights reserved.</small>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.cf-footer {
  margin-top: auto;
  background: linear-gradient(145deg, #2f343b 0%, #3a3f46 100%) !important;
  color: #e5e7eb !important;
  border-top: 1px solid rgba(255, 255, 255, 0.12) !important;
}

.cf-container {
  width: min(1260px, calc(100% - 36px));
  margin: 0 auto;
  padding: 44px 0 18px;
}

.cf-grid {
  display: grid;
  grid-template-columns: 1.05fr 1fr 0.9fr 1.2fr;
  gap: 24px;
}

.cf-grid h3,
.cf-grid h4 {
  margin: 0;
  color: #ffffff;
}

.cf-grid h3 {
  font-size: 28px;
  letter-spacing: 0.01em;
}

.cf-grid h4 {
  font-size: 20px;
}

.cf-brand p {
  margin: 10px 0 0;
  color: #cfd4dc;
  max-width: 280px;
  line-height: 1.6;
}

.cf-socials {
  margin-top: 18px;
  display: flex;
  gap: 10px;
}

.cf-socials a {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  text-decoration: none;
  color: #f3f4f6;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.05);
  transition: transform 0.2s ease, background 0.2s ease;
}

.cf-socials a:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.15);
}

.cf-socials span {
  font-size: 12px;
  font-weight: 700;
}

.cf-list,
.cf-links {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 11px;
}

.cf-list li {
  display: grid;
  grid-template-columns: 16px 1fr;
  gap: 8px;
  align-items: start;
  color: #d6d9de;
  line-height: 1.5;
}

.cf-links a {
  text-decoration: none;
  color: #d6d9de;
}

.cf-links a:hover {
  color: #ffffff;
}

.cf-schedule {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 9px;
  color: #d6d9de;
}

.cf-note {
  margin-top: 14px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  padding: 10px;
  display: grid;
  gap: 6px;
}

.cf-note strong {
  color: #ffffff;
  font-size: 14px;
}

.cf-note span {
  color: #d6d9de;
  font-size: 13px;
  line-height: 1.5;
}

.cf-bottom {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.cf-bottom small {
  color: #ffffff !important;
}

@media (max-width: 1080px) {
  .cf-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .cf-container {
    width: min(1260px, calc(100% - 26px));
    padding-top: 30px;
  }

  .cf-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .cf-grid h3 {
    font-size: 24px;
  }

  .cf-grid h4 {
    font-size: 18px;
  }
}
</style>
