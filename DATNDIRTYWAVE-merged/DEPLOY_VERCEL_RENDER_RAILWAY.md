# DirtyWave Deploy Checklist: Vercel + Spring Backend (Render/Railway)

Muc tieu:
- Frontend Vue deploy tren Vercel
- Backend Spring deploy rieng (Render hoac Railway)
- Frontend tren Vercel goi API backend on dinh, khong con trang chu 0 san pham

## 1) Vercel Environment Variables chuan

Trong Vercel Project -> Settings -> Environment Variables, set toi thieu:

- VITE_PUBLIC_APP_ORIGIN = https://dirtywave.vercel.app
- VITE_API_ORIGIN = https://<spring-backend-domain>
- VITE_API_BASE_URL = https://<spring-backend-domain>/api

Bien bo sung (chi can khi dung chuc nang lien quan Node backend):

- VITE_NODE_BACKEND_URL = https://<node-backend-domain>
- VITE_HOADON_WS_URL = wss://<node-backend-domain>/ws/hoa-don
- VITE_HOADON_WS_AUTO = false

Khuyen nghi pham vi bien:
- Production: bat buoc
- Preview: nen co de test PR
- Development: tuy chon

Sau khi set bien:
1. Redeploy ban Production moi nhat
2. Hard refresh trinh duyet

## 2) Trieu chung hien tai va nguyen nhan

Neu trang chu tren Vercel hien 0 san pham:
- Thuong do frontend dang goi sai API origin
- Hoac backend chua deploy/chua chay
- Hoac domain Vercel dang bat Protection (SSO), API bi redirect/401

Du an da bo sung canh bao tren trang chu khi API loi, de khong con trang thai trong ma khong ro ly do.

## 3) Deploy backend Spring tren Render

### 3.1 Tao service
1. Login Render
2. New + -> Web Service
3. Connect repo backend Spring (repo DATN-API)

### 3.2 Build/Start command
- Build Command: ./mvnw -DskipTests clean package
- Start Command: java -Dserver.port=$PORT -jar target/*.jar

### 3.3 Bien moi truong backend
Set day du theo app Spring cua ban (DB, JWT, mail, etc.).
Bat buoc:
- SPRING_PROFILES_ACTIVE=prod
- SERVER_PORT khong can set cung, vi Render cap qua $PORT

### 3.4 Kiem tra sau deploy
- GET https://<render-domain>/api/san-pham?page=0&size=1 phai tra JSON
- Endpoint uploads neu co anh: https://<render-domain>/uploads/...

## 4) Deploy backend Spring tren Railway

### 4.1 Tao project
1. Login Railway
2. New Project -> Deploy from GitHub
3. Chon repo backend Spring

### 4.2 Build/Start
Neu Railway auto detect Java:
- Build: ./mvnw -DskipTests clean package
- Start: java -Dserver.port=$PORT -jar target/*.jar

### 4.3 Bien moi truong backend
Set day du giong Production (DB, JWT, mail...).

### 4.4 Kiem tra sau deploy
- GET https://<railway-domain>/api/san-pham?page=0&size=1 phai tra JSON

## 5) CORS cho backend Spring

Backend can allow origin:
- https://dirtywave.vercel.app

Neu co Preview URL cua Vercel thi them vao danh sach allowed origins.

## 6) End-to-end test voi domain Vercel hien co

Test tren trinh duyet:
1. Mo https://dirtywave.vercel.app/trang-chu
2. Mo DevTools -> Network
3. Filter "san-pham"
4. Request GET /api/san-pham?page=0&size=1000 phai:
   - Status 200
   - Response la JSON array co du lieu
5. UI phai hien card san pham (khong con 0)

Test nhanh bang Console:
- fetch("https://<spring-backend-domain>/api/san-pham?page=0&size=3").then(r => r.status)

## 7) Neu van 0 san pham

Checklist nhanh:
1. VITE_API_ORIGIN da set dung domain backend chua
2. Domain backend co SSL hop le chua
3. Backend endpoint /api/san-pham co tra du lieu tren internet chua
4. CORS da allow dirtywave.vercel.app chua
5. Vercel co bat Protection/SSO khien traffic bi redirect khong

## 8) Khuyen nghi de nhanh va on dinh

1. Khong deploy Spring tren Vercel serverless
2. Dung Render/Railway/VPS cho Spring
3. Neu dung free tier, uu tien goi keep-alive de giam cold start
4. Tach ro frontend domain va backend domain, cau hinh qua env
