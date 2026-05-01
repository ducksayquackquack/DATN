# HƯỚNG DẪN CLOUDFLARE NAMED TUNNEL ỔN ĐỊNH

Named tunnel là cách đúng nếu bạn muốn URL ổn định như `live.dirtywave.com` hoặc `dirtywave.com` thay vì `*.trycloudflare.com`.

## Cái gì là free?

- Cloudflare Tunnel có gói miễn phí.
- Nhưng bạn vẫn cần:
  - tài khoản Cloudflare
  - một domain hoặc subdomain nằm trong Cloudflare DNS
  - tunnel đã được tạo trong Zero Trust

Nếu không có domain/subdomain trong tài khoản Cloudflare của bạn, không thể hoàn tất named tunnel thật.

## Mục tiêu

Ví dụ muốn website luôn là:

`https://live.dirtywave.com`

hoặc:

`https://dirtywave.com`

thì hostname đó phải được map cố định vào named tunnel.

Nếu bạn dùng link tra cứu đơn hàng, nó có thể chạy ổn định kiểu:

`https://live.dirtywave.com/tra-cuu-don-hang?ma=HD...`

## Bước 1: Tạo tunnel trên Cloudflare

1. Vào Cloudflare Zero Trust
2. Chọn `Networks` -> `Tunnels`
3. Tạo một `Cloudflared tunnel`
4. Lưu lại:
   - `Tunnel ID`
   - file credentials `.json`

## Bước 2: Gắn hostname cố định

Trong phần Public Hostnames của tunnel:

- Hostname: `live` hoặc để trống nếu dùng apex/root domain
- Domain: domain của bạn, ví dụ `dirtywave.com`
- Service: `http://localhost:5173`

Sau đó bạn sẽ có hostname cố định như:

`live.dirtywave.com`

Nếu muốn dùng root domain `dirtywave.com`, Cloudflare vẫn hỗ trợ, nhưng chỉ nên dùng khi website chính của bạn thực sự đi qua tunnel đó.

## Bước 3: Khai báo biến môi trường trên máy local

Trong Windows PowerShell:

```powershell
setx CLOUDFLARE_TUNNEL_ID "YOUR_TUNNEL_ID"
setx CLOUDFLARE_TUNNEL_HOSTNAME "live.dirtywave.com"
setx CLOUDFLARE_TUNNEL_CREDENTIALS "C:\full\path\to\YOUR_TUNNEL_ID.json"
setx DIRTYWAVE_BACKEND_PATH "C:\Users\ASUS\Downloads\dirtywaveapi\DATNAPI3-merged"
```

Mở terminal mới sau khi setx xong.

## Bước 4: Chạy frontend hoặc full stack

Nếu chỉ muốn frontend + tunnel:

```powershell
npm run dev:cloudflare:named
```

Nếu muốn full stack ổn định luôn:

```powershell
npm run live:named
```

Nếu Spring Boot đã chạy sẵn ở IntelliJ:

```powershell
npm run live:named:skip-backend
```

Flow `live:named` sẽ:

- ghi `VITE_PUBLIC_APP_ORIGIN=https://live.dirtywave.com`
- để `VITE_API_ORIGIN` rỗng để frontend gọi API qua same-origin proxy
- chạy named tunnel bằng file config local
- chạy Vite
- tùy chọn tự khởi động Spring Boot

## Kết quả mong muốn

Từ lúc này website và các link mới sẽ dùng domain cố định thay vì:

- `*.trycloudflare.com`

Ví dụ:

- `https://live.dirtywave.com`
- `https://dirtywave.com`

## Quan trọng

- Email cũ đã gửi bằng tunnel tạm sẽ không tự đổi domain.
- Bạn phải gửi lại email mới sau khi named tunnel chạy ổn định.
- Frontend hiện dùng same-origin API proxy qua Vite, nên không cần API tunnel riêng.
- Ảnh sản phẩm mới chỉ đúng hoàn toàn khi backend lưu và trả URL ảnh thật.