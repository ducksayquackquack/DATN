<script setup>
import { onMounted, onUnmounted } from "vue"
import CustomerPageLayout from "../../../components/customer/CustomerPageLayout.vue"
import heroImage from "../../../assets/img/Jackets/Áo khoác coach lông cừu DirtyWave.jpg?url"
import showcaseImage from "../../../assets/img/Jackets/Áo bomber da lộn DirtyWave.jpg?url"
import detailImage from "../../../assets/img/Jackets/Áo hoodie kéo khoá Jacket DirtyWave.jpg?url"

const manifesto = [
  {
    title: "Form có kiến trúc",
    text: "Mỗi đường cắt được dựng để đứng khung rõ ràng nhưng vẫn thoải mái khi mặc lâu. Tỷ lệ vai, thân và tay áo được cân chỉnh để tạo cảm giác gọn gàng nhưng không gò bó, giúp người mặc di chuyển tự nhiên trong mọi hoạt động hằng ngày. Thiết kế tập trung vào sự cân bằng giữa cấu trúc và sự linh hoạt, để chiếc áo luôn giữ được form ngay cả sau nhiều lần sử dụng.",
  },
  {
    title: "Chất liệu có tuổi thọ",
    text: "Ưu tiên bề mặt bền và cảm giác thật tay, để chiếc áo giữ được giá trị sau nhiều vòng sử dụng. Chúng tôi lựa chọn chất liệu có khả năng giữ form tốt, ít bai nhão và vẫn dễ chịu khi tiếp xúc với da. Qua thời gian, chất liệu không chỉ duy trì chất lượng mà còn phát triển một cá tính riêng, phản ánh hành trình sử dụng của người mặc.",
  },
  {
    title: "Màu dễ sống cùng tủ đồ",
    text: "Đen, trắng và đỏ điểm nhấn đủ để tạo cá tính mà không phá nhịp phối hằng ngày. Bảng màu được chọn để dễ kết hợp với nhiều phong cách và hoàn cảnh khác nhau, từ những bộ đồ tối giản đến những set đồ mang tính cá nhân hơn. Sự tiết chế trong màu sắc giúp chiếc áo trở thành một phần linh hoạt trong tủ đồ, thay vì chỉ là một món đồ mặc theo xu hướng.",
  },
]

const pillars = [
  {
    icon: "bi bi-bounding-box-circles",
    title: "Form chuẩn",
    text: "Cân lại vai, ngực và thân áo để tổng thể gọn từ góc nhìn đầu tiên.",
  },
  {
    icon: "bi bi-droplet-half",
    title: "Bề mặt chuẩn",
    text: "Vải được thử giặt và thử chuyển động trước khi thành sản phẩm chính thức.",
  },
  {
    icon: "bi bi-view-stacked",
    title: "Layer chuẩn",
    text: "Thiết kế luôn được test cùng các món nền để phối đồ không cần suy nghĩ nhiều.",
  },
  {
    icon: "bi bi-person-check",
    title: "Tư vấn chuẩn",
    text: "Đề xuất dựa trên thói quen mặc thật, không chỉ dừng ở bảng size khô cứng.",
  },
]

const rituals = [
  {
    step: "01",
    title: "Dựng bản khối",
    text: "Bắt đầu từ cấu trúc cổ, vai và độ rơi thân để định tinh thần mẫu ngay từ bản đầu.",
  },
  {
    step: "02",
    title: "Thử chuyển động",
    text: "Mặc thử trong nhịp sống thật để kiểm tra tay áo, lưng áo và độ co giãn khi di chuyển.",
  },
  {
    step: "03",
    title: "Khớp tủ đồ",
    text: "Mẫu chỉ ra mắt khi đi tốt cùng denim, tee, sơ mi và các lớp mặc cơ bản.",
  },
]

const journey = [
  {
    year: "2022",
    title: "Khởi đầu bằng outerwear",
    text: "DirtyWave bắt đầu với các mẫu jacket tối giản, tập trung vào phom và độ bền thực dụng.",
  },
  {
    year: "2024",
    title: "Mở rộng thành capsule",
    text: "Bổ sung hoodie và các lớp mặc linh hoạt để hệ sản phẩm liền mạch hơn.",
  },
  {
    year: "2026",
    title: "Chuẩn hóa trải nghiệm",
    text: "Tối ưu tư vấn size và hành trình mua online để khách hàng yên tâm hơn khi đặt hàng.",
  },
]

const metrics = [
  { value: "8.000+", label: "khách hàng đồng hành" },
  { value: "160+", label: "thiết kế phát hành" },
  { value: "96%", label: "đánh giá tích cực" },
]

let revealObserver = null

onMounted(() => {
  const items = document.querySelectorAll(".reveal")

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible")
          revealObserver?.unobserve(entry.target)
        }
      })
    },
    {
      threshold: 0.14,
      rootMargin: "0px 0px -40px 0px",
    }
  )

  items.forEach((item, index) => {
    item.style.setProperty("--reveal-delay", `${Math.min(index * 70, 420)}ms`)
    revealObserver.observe(item)
  })
})

onUnmounted(() => {
  revealObserver?.disconnect()
})
</script>

<template>
  <CustomerPageLayout breadcrumb="Giới thiệu" max-width="1280px">
    <div class="about-page">
      <section class="about-hero reveal">
        <div class="about-hero-copy">
          <span class="about-kicker">DirtyWave profile</span>
          <h1>Một nhịp mặc gọn, sắc và có chiều sâu</h1>
          <p>
            DirtyWave theo đuổi thời trang nam tối giản theo tinh thần premium trẻ: chỉ giữ lại chi tiết đáng tiền,
            bỏ bớt thứ thừa, để bạn mặc đẹp mà không cần cố gồng phong cách.
          </p>

          <div class="about-actions">
            <RouterLink to="/san-pham" class="about-primary-link">Xem capsule hiện tại</RouterLink>
            <a href="#about-atelier" class="about-secondary-link">Xem cách chúng tôi làm sản phẩm</a>
          </div>

          <div class="about-ribbon">
            <article v-for="item in metrics" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </article>
          </div>
        </div>

        <div class="about-hero-stage">
          <article class="about-floating-note reveal">
            <small>Season note</small>
            <strong>Thiết kế không cần nói quá to. Nó chỉ cần đủ đúng để bạn muốn mặc lại lần thứ 30.</strong>
          </article>

          <div class="about-image-stack">
            <figure class="about-image about-image-main">
              <img :src="heroImage" alt="DirtyWave outerwear" />
            </figure>

            <figure class="about-image about-image-accent">
              <img :src="showcaseImage" alt="DirtyWave styling" />
            </figure>

            <div class="about-grid-tag">Jacket Wardrobe</div>
          </div>
        </div>
      </section>

      <section class="about-manifesto reveal">
        <article class="about-manifesto-card about-manifesto-lead reveal">
          <span class="about-section-label">Manifesto</span>
          <h2>Không làm nhiều sản phẩm. Chỉ làm những món bạn muốn giữ lại trong tủ đồ lâu hơn.</h2>
          <p>
            Chúng tôi tin một chiếc áo tốt phải vừa có cấu trúc đẹp khi đứng yên,
            vừa đủ linh hoạt khi đi qua lịch trình dài từ sáng tới đêm.
          </p>

          <div class="about-manifesto-bars">
            <div class="about-manifesto-bar">
              <span>01</span>
              <strong>Less trend, more longevity</strong>
            </div>
            <div class="about-manifesto-bar">
              <span>02</span>
              <strong>Less noise, more character</strong>
            </div>
            <div class="about-manifesto-bar">
              <span>03</span>
              <strong>Less clutter, more intention</strong>
            </div>
          </div>
        </article>

        <div class="about-manifesto-list">
          <article v-for="item in manifesto" :key="item.title" class="about-manifesto-card about-manifesto-item reveal">
            <span class="about-dot"></span>
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.text }}</p>
            </div>
          </article>
        </div>
      </section>

      <section class="about-editorial reveal">
        <article class="about-story-card reveal">
          <span class="about-section-label">Story</span>
          <h2>DirtyWave bắt đầu từ một câu hỏi rất thật: vì sao nhiều chiếc áo nhìn ổn trên ảnh nhưng không sống được trong lịch mặc hằng ngày?</h2>
          <p>
            Chúng tôi phát triển thương hiệu bằng những buổi thử đồ liên tục, nơi khác biệt lớn nhất không nằm ở việc bắt trend,
            mà nằm ở cảm giác mặc khi bạn ngồi xuống, bước đi và hoạt động cả ngày.
          </p>
          <p>
            Vì vậy, hướng đi của DirtyWave là rõ ràng: tạo ra quần áo có cấu trúc, tính ứng dụng cao và bản sắc đủ riêng để bạn tự tin lâu dài.
          </p>

          <div class="about-quote-strip">
            <span>Studio thought</span>
            <strong>Tối giản không đồng nghĩa với đơn điệu. Tối giản đúng là chọn lọc kỹ hơn.</strong>
          </div>
        </article>

        <article class="about-poster-card reveal">
          <img :src="detailImage" alt="DirtyWave studio" />
          <div class="about-poster-copy">
            <small>Material direction</small>
            <strong>Ưu tiên bề mặt chắc, đứng khối đẹp và đủ nhẹ để mặc từ ngày thường tới cuối tuần.</strong>
          </div>
        </article>
      </section>

      <section class="about-pillars-section reveal">
        <div class="about-pillars-header">
          <span class="about-section-label">Core principles</span>
          <h2>Bốn nguyên tắc giúp DirtyWave giữ chất riêng nhưng vẫn cực dễ mặc</h2>
        </div>

        <div class="about-pillars-grid">
          <article v-for="item in pillars" :key="item.title" class="about-pillar-card reveal">
            <div class="about-pillar-topline">
              <i :class="item.icon"></i>
              <span>{{ item.title }}</span>
            </div>
            <p>{{ item.text }}</p>
          </article>
        </div>
      </section>

      <section id="about-atelier" class="about-atelier reveal">
        <article class="about-atelier-panel reveal">
          <span class="about-section-label">Atelier rhythm</span>
          <h2>Mỗi thiết kế đều đi qua một quy trình kiểm tra có chủ đích</h2>

          <div class="about-ritual-list">
            <article v-for="item in rituals" :key="item.step" class="about-ritual-item reveal">
              <span class="about-ritual-step">{{ item.step }}</span>
              <div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.text }}</p>
              </div>
            </article>
          </div>
        </article>

        <article class="about-atelier-visual reveal">
          <img :src="showcaseImage" alt="DirtyWave process" />

          <div class="about-swatch-panel">
            <small>Palette note</small>
            <div class="about-swatches">
              <span style="background: #111111"></span>
              <span style="background: #ffffff"></span>
              <span style="background: #c5162d"></span>
            </div>
            <p>Bảng màu chính của DirtyWave luôn xoay quanh đỏ, đen, trắng để giữ bản sắc mạnh và sạch.</p>
          </div>
        </article>
      </section>

      <section class="about-journey-section reveal">
        <div class="about-journey-header">
          <span class="about-section-label">Timeline</span>
          <h2>Hành trình ngắn, nhưng định hướng rất chắc</h2>
        </div>

        <div class="about-journey-track">
          <article v-for="item in journey" :key="item.year" class="about-journey-card reveal">
            <span class="about-journey-year">{{ item.year }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.text }}</p>
          </article>
        </div>
      </section>

      <section class="about-cta reveal">
        <div>
          <span class="about-section-label about-section-label-light">Next move</span>
          <h2>Nếu bạn thích quần áo gọn khối, chất và mặc bền, bộ sưu tập hiện tại đã sẵn sàng.</h2>
        </div>
        <RouterLink to="/san-pham" class="about-cta-link">Khám phá sản phẩm</RouterLink>
      </section>
    </div>
  </CustomerPageLayout>
</template>

<style scoped>
.about-page {
  --about-red: #c5162d;
  --about-red-dark: #8f1121;
  --about-black: #111111;
  --about-white: #ffffff;
  --about-line: #e9d9dd;
  --about-soft: #f7f7f7;

  display: grid;
  gap: 20px;
  color: var(--about-black);
}

.about-hero,
.about-manifesto,
.about-editorial,
.about-pillars-section,
.about-atelier,
.about-journey-section,
.about-cta {
  position: relative;
}

.reveal {
  opacity: 0;
  transform: translateY(26px);
  transition:
    opacity 0.65s ease,
    transform 0.65s cubic-bezier(0.2, 0.8, 0.2, 1);
  transition-delay: var(--reveal-delay, 0ms);
}

.reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.about-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(320px, 0.96fr);
  gap: 18px;
  padding: clamp(22px, 3vw, 34px);
  border: 1px solid var(--about-line);
  border-radius: 26px;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(197, 22, 45, 0.13), transparent 35%),
    linear-gradient(145deg, #ffffff 0%, #fbfbfb 100%);
}

.about-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(197, 22, 45, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 17, 17, 0.04) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.36), transparent 82%);
  pointer-events: none;
}

.about-kicker,
.about-section-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: var(--about-red-dark);
  font-weight: 700;
}

.about-kicker::before,
.about-section-label::before {
  content: "";
  width: 28px;
  height: 1px;
  background: currentColor;
}

.about-hero-copy h1,
.about-manifesto-lead h2,
.about-story-card h2,
.about-pillars-header h2,
.about-atelier-panel h2,
.about-journey-header h2,
.about-cta h2 {
  margin: 14px 0 0;
  font-family: "Iowan Old Style", "Palatino Linotype", "Book Antiqua", serif;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.03;
}

.about-hero-copy h1 {
  max-width: 10.5em;
  font-size: clamp(40px, 6.8vw, 74px);
}

.about-hero-copy p,
.about-story-card p,
.about-pillar-card p,
.about-ritual-item p,
.about-journey-card p,
.about-swatch-panel p,
.about-manifesto-lead p,
.about-manifesto-item p {
  color: #4a4a4a;
  line-height: 1.74;
}

.about-hero-copy p {
  max-width: 640px;
  margin: 18px 0 0;
  font-size: 16px;
}

.about-actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.about-primary-link,
.about-secondary-link,
.about-cta-link {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 700;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.about-primary-link,
.about-cta-link {
  background: var(--about-black);
  color: var(--about-white);
  box-shadow: 0 14px 30px rgba(17, 17, 17, 0.16);
}

.about-primary-link:hover,
.about-cta-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(17, 17, 17, 0.24);
}

.about-secondary-link {
  border: 1px solid #d8c5c9;
  color: var(--about-red-dark);
  background: #ffffff;
}

.about-secondary-link:hover {
  transform: translateY(-2px);
  border-color: #c999a2;
}

.about-ribbon {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.about-ribbon article {
  padding: 12px 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid var(--about-line);
}

.about-ribbon strong {
  display: block;
  color: var(--about-black);
  font-size: 22px;
}

.about-ribbon span {
  display: block;
  margin-top: 4px;
  color: #565656;
  font-size: 13px;
}

.about-hero-stage {
  display: grid;
  align-content: center;
  justify-items: end;
  gap: 14px;
}

.about-floating-note {
  width: min(320px, 100%);
  padding: 16px;
  border-radius: 16px;
  background: var(--about-black);
  color: #f5f5f5;
  transform: rotate(-3deg);
  box-shadow: 0 18px 32px rgba(0, 0, 0, 0.2);
}

.about-floating-note small,
.about-poster-copy small,
.about-swatch-panel small {
  display: block;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.72);
}

.about-floating-note strong {
  line-height: 1.6;
  font-size: 14px;
}

.about-image-stack {
  position: relative;
  min-height: 560px;
  width: 100%;
}

.about-image {
  position: absolute;
  margin: 0;
  overflow: hidden;
  border-radius: 22px;
  box-shadow: 0 20px 46px rgba(0, 0, 0, 0.15);
}

.about-image img,
.about-poster-card img,
.about-atelier-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.about-image-main {
  inset: 32px 36px 74px 18px;
}

.about-image-main::after,
.about-image-accent::after,
.about-poster-card::after,
.about-atelier-visual::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 42%, rgba(0, 0, 0, 0.35));
}

.about-image-accent {
  width: 44%;
  height: 42%;
  right: 0;
  bottom: 0;
  border: 6px solid #ffffff;
}

.about-grid-tag {
  position: absolute;
  left: 0;
  bottom: 16px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #dfc9ce;
  color: var(--about-red-dark);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.about-manifesto {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 14px;
}

.about-manifesto-card,
.about-story-card,
.about-poster-card,
.about-pillar-card,
.about-atelier-panel,
.about-atelier-visual,
.about-journey-card {
  border-radius: 22px;
  border: 1px solid var(--about-line);
  background: #ffffff;
}

.about-manifesto-lead {
  padding: 24px;
  background:
    radial-gradient(circle at 90% 10%, rgba(197, 22, 45, 0.1), transparent 32%),
    #ffffff;
}

.about-manifesto-lead h2 {
  font-size: clamp(31px, 4vw, 50px);
  max-width: 13ch;
}

.about-manifesto-lead p {
  margin: 14px 0 0;
  max-width: 52ch;
}

.about-manifesto-bars {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.about-manifesto-bar {
  min-height: 52px;
  border: 1px solid #ebd9dd;
  border-left: 3px solid var(--about-red);
  border-radius: 12px;
  background: #fff8f9;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.about-manifesto-bar span {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--about-black);
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
}

.about-manifesto-bar strong {
  font-size: 13px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #222222;
}

.about-manifesto-list {
  display: grid;
  gap: 12px;
}

.about-manifesto-item {
  min-height: 124px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px;
}

.about-dot {
  margin-top: 8px;
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--about-red);
  box-shadow: 0 0 0 6px rgba(197, 22, 45, 0.11);
}

.about-manifesto-item h3 {
  margin: 0;
  font-size: 19px;
  color: #1f1f1f;
}

.about-manifesto-item p {
  margin: 6px 0 0;
  font-size: 14px;
}

.about-editorial {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(300px, 0.96fr);
  gap: 16px;
}

.about-story-card {
  padding: 24px;
}

.about-story-card h2,
.about-pillars-header h2,
.about-atelier-panel h2,
.about-journey-header h2,
.about-cta h2 {
  font-size: clamp(29px, 3.4vw, 44px);
}

.about-quote-strip {
  margin-top: 20px;
  padding: 14px 16px;
  border-left: 4px solid var(--about-red);
  border-radius: 0 14px 14px 0;
  background: #fff5f6;
}

.about-quote-strip span {
  display: block;
  margin-bottom: 6px;
  color: var(--about-red-dark);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.about-quote-strip strong {
  color: #222222;
  line-height: 1.65;
}

.about-poster-card,
.about-atelier-visual {
  overflow: hidden;
  min-height: 100%;
}

.about-poster-card {
  position: relative;
  min-height: 470px;
}

.about-poster-copy {
  position: absolute;
  right: 16px;
  bottom: 16px;
  left: 16px;
  padding: 16px;
  border-radius: 14px;
  background: rgba(17, 17, 17, 0.73);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
  color: #ffffff;
}

.about-poster-copy strong {
  line-height: 1.68;
}

.about-pillars-section {
  padding: 24px;
  border-radius: 24px;
  background:
    radial-gradient(circle at 12% 12%, rgba(197, 22, 45, 0.09), transparent 30%),
    #ffffff;
  border: 1px solid var(--about-line);
}

.about-pillars-header {
  display: grid;
  gap: 12px;
}

.about-pillars-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.about-pillar-card {
  padding: 16px;
  background: #ffffff;
}

.about-pillar-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #1f1f1f;
  font-weight: 700;
}

.about-pillar-topline i {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--about-black);
  color: var(--about-white);
  font-size: 17px;
}

.about-pillar-card p,
.about-journey-card p,
.about-swatch-panel p {
  margin: 0;
}

.about-atelier {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.about-atelier-panel,
.about-atelier-visual {
  padding: 24px;
}

.about-ritual-list {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.about-ritual-item {
  display: grid;
  grid-template-columns: 66px 1fr;
  gap: 12px;
  align-items: start;
}

.about-ritual-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  border-radius: 12px;
  background: var(--about-black);
  color: var(--about-white);
  font-size: 17px;
  font-weight: 700;
}

.about-ritual-item h3,
.about-journey-card h3 {
  margin: 0 0 6px;
  color: #1e1e1e;
  font-size: 20px;
}

.about-ritual-item p {
  margin: 0;
}

.about-atelier-visual {
  position: relative;
  display: flex;
  align-items: flex-end;
  min-height: 520px;
}

.about-atelier-visual img {
  position: absolute;
  inset: 0;
}

.about-swatch-panel {
  position: relative;
  width: min(300px, 100%);
  padding: 16px;
  border-radius: 14px;
  background: rgba(17, 17, 17, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px);
  color: #ffffff;
}

.about-swatches {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.about-swatches span {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.38);
}

.about-swatch-panel p {
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.7;
}

.about-journey-section {
  padding: 24px;
  border-radius: 24px;
  background: #ffffff;
  border: 1px solid var(--about-line);
}

.about-journey-header {
  display: grid;
  gap: 12px;
}

.about-journey-track {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.about-journey-card {
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(255, 243, 245, 0.8));
}

.about-journey-year {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 78px;
  height: 34px;
  margin-bottom: 14px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--about-black);
  color: var(--about-white);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.about-cta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 26px;
  border-radius: 24px;
  overflow: hidden;
  background:
    radial-gradient(circle at 14% 22%, rgba(255, 255, 255, 0.2), transparent 24%),
    linear-gradient(130deg, #0f0f0f 0%, #2f0f14 55%, #7f101f 100%);
  color: #ffffff;
}

.about-section-label-light {
  color: rgba(255, 255, 255, 0.76);
}

.about-section-label-light::before {
  background: currentColor;
}

.about-cta h2 {
  max-width: 18ch;
  margin-top: 14px;
}

.about-cta-link {
  background: #ffffff;
  color: #320f14;
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.22);
  white-space: nowrap;
}

.about-cta-link:hover {
  background: #fff5f6;
}

@media (max-width: 1120px) {
  .about-hero,
  .about-manifesto,
  .about-editorial,
  .about-atelier,
  .about-journey-track {
    grid-template-columns: 1fr;
  }

  .about-pillars-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .about-image-stack {
    min-height: 500px;
  }

  .about-cta {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 760px) {
  .about-page {
    gap: 16px;
  }

  .about-hero,
  .about-pillars-section,
  .about-journey-section,
  .about-cta,
  .about-story-card,
  .about-manifesto-lead,
  .about-atelier-panel,
  .about-atelier-visual {
    padding: 20px;
    border-radius: 18px;
  }

  .about-ribbon,
  .about-pillars-grid,
  .about-journey-track {
    grid-template-columns: 1fr;
  }

  .about-image-stack {
    min-height: 420px;
  }

  .about-image-main {
    inset: 24px 0 86px 0;
  }

  .about-image-accent {
    width: 48%;
    height: 38%;
  }

  .about-grid-tag {
    left: 8px;
  }

  .about-ritual-item {
    grid-template-columns: 1fr;
  }

  .about-ritual-step {
    width: 66px;
  }

  .about-atelier-visual {
    min-height: 420px;
  }

  .about-swatch-panel {
    width: 100%;
  }
}
</style>
