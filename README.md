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
