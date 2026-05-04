# HƯỚNG DẪN CHẠY DỰ ÁN

Tài liệu này ưu tiên cách chạy đơn giản nhất.

Mặc định: chạy local, không cần Cloudflare, không cần ngrok, không cần tunnel.

## Quick Start

1. Mở VS Code và mở folder DATNvue3
2. Nhấn Ctrl + Shift + P
3. Gõ Run Task
4. Chọn Run Frontend (Vite only)
5. Mở IntelliJ và chạy DATN-API
6. Mở trình duyệt tại http://localhost:5173

Đây là luồng mặc định để code, test giao diện, test API và làm việc hằng ngày.

## Nếu bạn chỉ muốn chạy dự án

Bạn chỉ cần 2 phần:
- Frontend Vite
- Backend DATN-API

Không cần cài thêm Cloudflare.

## Gửi mail tra cứu đơn hàng

Mail tra cứu cần một link public để người nhận bên ngoài bấm vào được.

Nếu chỉ chạy local:
- Website vẫn chạy bình thường trên máy bạn
- Nhưng link `localhost` không dùng được cho người khác
- Vì vậy mail tra cứu public không phải luồng mặc định của guide này

Nói ngắn gọn:
- Chạy dự án: không cần tunnel
- Gửi link public cho người khác: mới cần tunnel hoặc domain public

## Cấu hình mail backend

Nếu backend có gửi mail, cần cấu hình Gmail App Password trong cấu hình chạy DATN-API.

Trong IntelliJ, mở Run Configuration của DATN-API và thêm Environment Variables:

```text
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxx
LOOKUP_MAIL_FROM=your_gmail@gmail.com
```

`MAIL_PASSWORD` là App Password 16 ký tự từ Google, không phải mật khẩu Gmail thường.

## Mỗi lần mở máy

1. Mở VS Code -> DATNvue3
2. Run Task -> Run Frontend (Vite only)
3. Mở IntelliJ -> Run DATN-API
4. Mở http://localhost:5173

Vậy là đủ để làm việc.

## Nếu frontend không lên

1. Dừng task đang chạy
2. Chờ vài giây
3. Chạy lại Run Frontend (Vite only)

## Nếu backend không lên

1. Kiểm tra DATN-API đang dùng đúng Java/Maven
2. Kiểm tra các biến mail nếu đang test chức năng gửi mail
3. Chạy lại DATN-API trong IntelliJ

## Tùy chọn nâng cao: Cloudflare

Phần này không bắt buộc.

Chỉ dùng khi bạn thật sự cần tạo link public tạm thời để người ngoài truy cập từ email hoặc điện thoại ngoài mạng local.

Task có sẵn:
- Start Dev (Auto Cloudflare + Vite)

Lưu ý:
- Quick tunnel `.trycloudflare.com` là link tạm thời
- Link có thể đổi sau mỗi lần chạy lại
- Link cũ có thể chết nếu tunnel dừng hoặc mất kết nối
- Vì vậy đây không phải luồng mặc định cho người mới clone repo

Nếu sau này cần domain cố định hơn, xem tài liệu riêng tại README-CLOUDFLARE-NAMED-TUNNEL.md.

## FAQ ngắn

Q: Người clone repo có bắt buộc cài Cloudflare không?
A: Không.

Q: Người clone repo có bắt buộc làm tunnel để chạy được dự án không?
A: Không.

Q: Khi nào mới cần Cloudflare?
A: Khi bạn cần gửi link public để người khác bên ngoài máy bạn mở được.

Q: Vì sao email cũ vẫn giữ link cũ?
A: Vì nội dung email đã gửi là tĩnh, không tự cập nhật theo runtime mới.

Q: Nếu không dùng tunnel thì có chạy được toàn bộ phần local không?
A: Có. Luồng code và test local vẫn chạy bình thường.