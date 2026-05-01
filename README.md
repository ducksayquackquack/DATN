# DATN — Hướng dẫn chạy dự án

## Chế độ dùng nhanh cho người clone repo

Người clone có thể chạy local bình thường, không cần tunnel.

1. Mở VS Code -> mở folder DATNvue3
2. Run Task -> Run Frontend (Vite only)
3. Mở IntelliJ -> Run DATN-API
4. Mở http://localhost:5173

## Khi cần link public để gửi mail tra cứu

Dùng Cloudflare:

- Task: Start Dev (Auto Cloudflare + Vite)
- Hướng dẫn: README-CLOUDFLARE.md

Nếu cần domain ổn định không đổi:

- Task: Start Dev (Cloudflare Named Tunnel + Vite)
- Hướng dẫn: README-CLOUDFLARE-NAMED-TUNNEL.md

## Deploy Vercel + Backend Spring

Nếu frontend đang chạy trên Vercel nhưng trang chủ không lên sản phẩm hoặc API chậm:

- Xem checklist chuẩn: DEPLOY_VERCEL_RENDER_RAILWAY.md
- File này có đầy đủ:
	- Environment Variables cần set trên Vercel
	- Checklist deploy Spring backend trên Render hoặc Railway
	- Quy trình test end-to-end với domain Vercel hiện có

## Cài một lần duy nhất (mail)

Trong backend, cần cấu hình SMTP để gửi mail:

MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxx
LOOKUP_MAIL_FROM=your_gmail@gmail.com

## Lệnh cơ bản

```powershell
npm install
npm run dev
npm run build
```

## Chạy ổn định khi đóng gói gửi người khác

Không cần Cloudflare để test tính năng gửi mail tài khoản nhân viên ở local.

1. Mở terminal tại thư mục dự án và cài package:

npm install
npm --prefix backend install

2. Chạy frontend + Cloudflare + backend Node mail bằng 1 lệnh:

powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-cloudflare-frontend.ps1

Ghi chú:
- Script sẽ tự cố gắng khởi động backend Node port 3000 (mail endpoint).
- Nếu chỉ muốn chạy frontend + cloudflare, thêm cờ:

powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-cloudflare-frontend.ps1 -SkipBackend

3. Nếu chỉ chạy local (không cloudflare):

npm --prefix backend run dev
npm run dev
