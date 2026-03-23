# HƯỚNG DẪN CLOUDLFARE NAMED TUNNEL ỔN ĐỊNH

Named tunnel là cách đúng nếu bạn muốn link mail tra cứu không đổi nữa.

## Cái gì là free?

- Cloudflare Tunnel có gói miễn phí.
- Nhưng bạn vẫn cần:
  - tài khoản Cloudflare
  - một domain hoặc subdomain nằm trong Cloudflare DNS
  - tunnel đã được tạo trong Zero Trust

Nếu không có domain/subdomain trong tài khoản Cloudflare của bạn, không thể hoàn tất named tunnel thật.

## Mục tiêu

Ví dụ muốn link mail luôn là:

https://lookup.yourdomain.com/dirtywave/tra-cuu-don-hang?ma=HD...

thì `lookup.yourdomain.com` phải được map cố định vào named tunnel.

## Bước 1: Tạo tunnel trên Cloudflare

1. Vào Cloudflare Zero Trust
2. Chọn `Networks` -> `Tunnels`
3. Tạo một `Cloudflared tunnel`
4. Lưu lại:
   - `Tunnel ID`
   - file credentials `.json`

## Bước 2: Gắn hostname cố định

Trong phần Public Hostnames của tunnel:

- Hostname: `lookup`
- Domain: domain của bạn, ví dụ `dirtywave.store`
- Service: `http://localhost:5173`

Sau đó bạn sẽ có hostname cố định như:

`lookup.dirtywave.store`

## Bước 3: Khai báo biến môi trường trên máy local

Trong Windows PowerShell:

```powershell
setx CLOUDFLARE_TUNNEL_ID "YOUR_TUNNEL_ID"
setx CLOUDFLARE_TUNNEL_HOSTNAME "lookup.yourdomain.com"
setx CLOUDFLARE_TUNNEL_CREDENTIALS "C:\full\path\to\YOUR_TUNNEL_ID.json"
```

Mở terminal mới sau khi setx xong.

## Bước 4: Chạy task mới trong VS Code

Run Task -> `Start Dev (Cloudflare Named Tunnel + Vite)`

Task này sẽ:

- ghi `VITE_PUBLIC_APP_ORIGIN=https://lookup.yourdomain.com`
- chạy named tunnel bằng file config local
- chạy Vite

## Kết quả mong muốn

Từ lúc này các mail tra cứu mới sẽ dùng domain cố định của Cloudflare thay vì:

- `*.trycloudflare.com`

## Quan trọng

- Email cũ đã gửi bằng tunnel tạm sẽ không tự đổi domain.
- Bạn phải gửi lại email mới sau khi named tunnel chạy ổn định.