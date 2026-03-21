<script setup>
import CustomerPageLayout from "../../../components/customer/CustomerPageLayout.vue"
import imgHero from "../../../assets/img/Jackets/Áo hoodie kéo khoá in hình DirtyWave.jpg?url"
import imgSideA from "../../../assets/img/Jackets/Áo bomber dáng lửng.jpg?url"
import imgSideB from "../../../assets/img/Jackets/Áo khoac coach da ASOS DirtyWave.jpg?url"
import imgSideC from "../../../assets/img/Jackets/Áo khoác coach giả da DirtyWave.jpg?url"

const chips = ["Drop mới", "Phong cách sống", "Chất liệu", "Hậu trường"]

const sidePosts = [
  {
    date: "10/03/2026",
    title: "5 item outerwear bán chạy nhất tuần này",
    excerpt: "Danh sách các mẫu được chọn nhiều nhờ độ linh hoạt trong phối đồ.",
    image: imgSideA,
  },
  {
    date: "04/03/2026",
    title: "Cách chọn hoodie zip theo form vai và chiều cao",
    excerpt: "Mẹo đơn giản để lên outfit gọn hơn mà vẫn giữ cá tính.",
    image: imgSideB,
  },
  {
    date: "27/02/2026",
    title: "Drop capsule mới: Street Layer 02 đã lên kệ",
    excerpt: "Bộ màu trung tính mới cho người theo phong cách tối giản hiện đại.",
    image: imgSideC,
  },
]

const editorial = [
  {
    title: "Dirty Notes",
    text: "Những ghi chú ngắn từ đội ngũ thiết kế về cách chọn màu và tỉ lệ outfit.",
  },
  {
    title: "Lookbook tuần",
    text: "3 outfit mẫu để bạn mặc từ sáng đến tối mà không cần đổi quá nhiều lớp.",
  },
]

const posts = [
  {
    tag: "Sản phẩm mới",
    title: "Hoodie zip DirtyWave: bản nâng cấp cho nhịp sống hằng ngày",
    excerpt: "Cải tiến nhẹ về chất vải và form vai để mặc thoải mái hơn trong thời tiết chuyển mùa.",
    image: imgSideB,
  },
  {
    tag: "Phong cách",
    title: "3 công thức phối bomber tối giản nhưng vẫn nổi bật",
    excerpt: "Gợi ý phối cùng tee trơn, quần suông và sneaker để giữ tổng thể cân bằng.",
    image: imgSideA,
  },
  {
    tag: "Chất liệu",
    title: "Vì sao cotton pha phù hợp cho jacket mặc mỗi ngày",
    excerpt: "Giữ được độ đứng form, bền màu và dễ vệ sinh khi dùng thường xuyên.",
    image: imgSideC,
  },
  {
    tag: "Dịch vụ",
    title: "Hướng dẫn đổi size online trong 3 bước tại DirtyWave",
    excerpt: "Quy trình gọn để bạn đổi đúng form mà không mất nhiều thời gian chờ xử lý.",
    image: imgHero,
  },
  {
    tag: "Xu hướng",
    title: "Streetwear 2026: silhouette rộng vừa, màu trầm và layer thông minh",
    excerpt: "Xu hướng mới tập trung vào tính ứng dụng và khả năng mặc lặp lại.",
    image: imgSideA,
  },
  {
    tag: "Hậu trường",
    title: "Một ngày phát triển mẫu tại studio DirtyWave",
    excerpt: "Từ ý tưởng ban đầu đến bản thử form cuối cùng trước khi ra mắt chính thức.",
    image: imgSideB,
  },
]
</script>

<template>
  <CustomerPageLayout breadcrumb="Tin tức" max-width="1260px">
    <header class="cp-header">
      <div>
        <h1>Tin tức & cảm hứng DirtyWave</h1>
        <p>Cập nhật drop mới, kiến thức phối đồ và câu chuyện phía sau từng collection.</p>
      </div>
      <div class="cp-chips">
        <span v-for="item in chips" :key="item">{{ item }}</span>
      </div>
    </header>

    <section class="cp-editorial">
      <article v-for="item in editorial" :key="item.title" class="cp-editorial-card">
        <h2>{{ item.title }}</h2>
        <p>{{ item.text }}</p>
      </article>
    </section>

    <section class="cp-feature-layout">
      <article class="cp-feature-card">
        <img :src="imgHero" alt="Tin nổi bật DirtyWave" />
        <div class="cp-feature-overlay">
          <span>Nổi bật</span>
          <h2>DirtyWave Drop 2026: định nghĩa lại tinh thần tối giản đường phố</h2>
          <p>
            Bộ sưu tập mới tập trung vào chất liệu bền, form dễ mặc và bảng màu trung tính để phối nhanh mỗi ngày.
          </p>
        </div>
      </article>

      <div class="cp-side-list">
        <article v-for="post in sidePosts" :key="post.title" class="cp-side-item">
          <img :src="post.image" :alt="post.title" />
          <div>
            <small>{{ post.date }}</small>
            <h3>{{ post.title }}</h3>
            <p>{{ post.excerpt }}</p>
          </div>
        </article>
      </div>
    </section>

    <section>
      <h2 class="cp-list-title">Tất cả bài viết</h2>

      <div class="cp-grid">
        <article v-for="post in posts" :key="post.title" class="cp-news-card">
          <img :src="post.image" :alt="post.title" />

          <div class="cp-news-body">
            <span class="cp-tag">{{ post.tag }}</span>
            <h3>{{ post.title }}</h3>
            <p>{{ post.excerpt }}</p>
            <button type="button">Đọc tiếp</button>
          </div>
        </article>
      </div>
    </section>
  </CustomerPageLayout>
</template>

<style scoped>
.cp-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 14px;
}

.cp-header h1 {
  margin: 0;
  color: #161214;
  font-size: clamp(28px, 4vw, 42px);
}

.cp-header p {
  margin: 8px 0 0;
  color: #6f6a6d;
}

.cp-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cp-chips span {
  border: 1px solid #ecdde0;
  border-radius: 999px;
  background: #fff;
  color: #605b5e;
  font-size: 12px;
  padding: 6px 10px;
}

.cp-editorial {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.cp-editorial-card {
  border-radius: 12px;
  border: 1px solid #e8d8db;
  background:
    linear-gradient(130deg, rgba(255, 255, 255, 1) 0%, rgba(255, 247, 249, 1) 100%);
  padding: 12px;
}

.cp-editorial-card h2 {
  margin: 0 0 6px;
  color: #8f1121;
  font-size: 16px;
}

.cp-editorial-card p {
  margin: 0;
  color: #5d585c;
  line-height: 1.65;
  font-size: 13px;
}

.cp-feature-layout {
  --cp-feature-height: 344px;
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 14px;
  align-items: stretch;
  height: var(--cp-feature-height);
}

.cp-feature-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  height: 100%;
  border: 1px solid #e8d8db;
}

.cp-feature-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(0.68);
}

.cp-feature-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20px;
  background: linear-gradient(180deg, transparent 0%, rgba(11, 12, 16, 0.88) 65%);
}

.cp-feature-overlay span {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background: #c5162d;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.cp-feature-overlay h2 {
  margin: 10px 0 8px;
  color: #fff;
  font-size: clamp(20px, 2.7vw, 30px);
  line-height: 1.2;
}

.cp-feature-overlay p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.65;
}

.cp-side-list {
  display: grid;
  height: 100%;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.cp-side-item {
  border: 1px solid #e8d8db;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 10px;
  padding: 8px;
  min-height: 0;
}

.cp-side-item img {
  width: 100%;
  height: 92px;
  border-radius: 8px;
  object-fit: cover;
}

.cp-side-item small {
  color: #8d8890;
  font-size: 12px;
}

.cp-side-item h3 {
  margin: 4px 0;
  color: #161214;
  font-size: 13px;
  line-height: 1.4;
}

.cp-side-item p {
  margin: 0;
  color: #6f6a6d;
  font-size: 11px;
  line-height: 1.5;
}

.cp-list-title {
  margin: 16px 0 12px;
  color: #4a4549;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 13px;
}

.cp-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.cp-news-card {
  border: 1px solid #e8d8db;
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.cp-news-card img {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.cp-news-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.cp-tag {
  display: inline-block;
  width: fit-content;
  border-radius: 999px;
  padding: 4px 10px;
  background: #ffe7eb;
  color: #8f1121;
  font-size: 12px;
  font-weight: 700;
}

.cp-news-body h3 {
  margin: 10px 0 8px;
  color: #161214;
  font-size: 18px;
  line-height: 1.4;
  min-height: calc(1.4em * 2);
}

.cp-news-body p {
  margin: 0;
  color: #5d585c;
  line-height: 1.7;
  font-size: 14px;
}

.cp-news-body button {
  margin-top: auto;
  padding-top: 12px;
  border: 0;
  background: transparent;
  color: #8f1121;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 1080px) {
  .cp-editorial {
    grid-template-columns: 1fr;
  }

  .cp-feature-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .cp-feature-card {
    height: 360px;
  }

  .cp-side-list {
    height: auto;
    grid-template-rows: none;
  }

  .cp-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .cp-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .cp-grid {
    grid-template-columns: 1fr;
  }

  .cp-side-item {
    grid-template-columns: 1fr;
  }

  .cp-side-item img {
    height: 180px;
  }
}
</style>
