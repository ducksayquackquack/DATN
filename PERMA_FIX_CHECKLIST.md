# PERMA FIX CHECKLIST (DATNvue2)

Muc tieu: chay on dinh, khong phu thuoc tunnel tam thoi, giam toi da thao tac tay.

## 0) Ket luan nhanh

- Ngrok/Quick tunnel KHONG phai perma fix. URL se het han/ngat ket noi.
- Perma fix that su = domain co dinh + server 24/7 + process manager + reverse proxy + SSL.
- Neu ban chi dev tren may ca nhan: dung local host la on dinh nhat.

---

## 1) Kich hoat mode ON DINH cho local (khuyen dung ngay)

- [ ] Mo frontend bang local URL: http://localhost:5173 (khong mo link tunnel cu).
- [ ] Dam bao backend chay o http://localhost:8080.
- [ ] Dat `VITE_API_ORIGIN` trong `.env.local` = `http://localhost:8080`.
- [ ] Dat `VITE_PUBLIC_APP_ORIGIN` trong `.env.local` = `http://localhost:5173` khi chi test noi bo.
- [ ] Khong dung `VITE_PUBLIC_APP_ORIGIN` la ngrok URL neu khong can chia se ben ngoai.
- [ ] Neu da tung set runtime override, xoa key localStorage: `dirtywave:api-origin`.

Lenh goi y (frontend):

```powershell
npm run dev
```

Backend (repo DATN-API):

```powershell
.\mvnw.cmd spring-boot:run
```

---

## 2) Bao mat (bat buoc lam)

- [ ] KHONG hard-code mail password trong script.
- [ ] Chuyen `MAIL_USERNAME`, `MAIL_PASSWORD` sang bien moi truong (User/System env).
- [ ] Rotate ngay App Password Gmail da lo trong script cu.
- [ ] Them file `secrets.local.ps1` (gitignored) neu can dung script local.

---

## 3) Neu can 24/7 that su (production/staging)

### 3.1 Ha tang

- [ ] Co VPS/Cloud VM always-on (Ubuntu khuyen dung).
- [ ] Domain co dinh (VD: app.yourdomain.com, api.yourdomain.com).
- [ ] SSL cert (Let's Encrypt) qua Nginx/Caddy.

### 3.2 Deploy backend (DATN-API)

- [ ] Build jar backend.
- [ ] Chay bang systemd service (tu khoi dong lai khi crash/reboot).
- [ ] Expose qua `api.yourdomain.com`.
- [ ] Health endpoint + restart policy + log rotation.

### 3.3 Deploy frontend (DATNvue2)

- [ ] Build frontend: `npm run build`.
- [ ] Serve static qua Nginx/Caddy (khong dung vite dev cho production).
- [ ] Dat env build:
  - [ ] `VITE_API_ORIGIN=https://api.yourdomain.com`
  - [ ] `VITE_PUBLIC_APP_ORIGIN=https://app.yourdomain.com`

### 3.4 Mail link va lookup

- [ ] `APP_LOOKUP_PUBLIC_URL` tro den domain app that (khong tro den tunnel).
- [ ] Test gui mail + click link tren 4G/Wifi ben ngoai mang noi bo.

---

## 4) Van hanh khong can "weird extra steps"

- [ ] Tach 2 profile ro rang:
  - [ ] Local profile (localhost only)
  - [ ] Production profile (domain that)
- [ ] Khong doi URL tay moi ngay.
- [ ] Chi dung tunnel cho demo ngan han.
- [ ] Thiet lap monitoring toi thieu:
  - [ ] Uptime check cho app/api
  - [ ] Alert khi 5xx hoac timeout

---

## 5) VS Code extension co giup duoc khong?

Co, nhung chi giup TU DONG HOA thao tac dev, khong bien tunnel tam thanh 24/7.

Nen dung:

- [ ] `ms-vscode.remote-server` (Remote - SSH): deploy/chay server tu VS Code tren VPS.
- [ ] `humao.rest-client` hoac Thunder Client: test API nhanh.
- [ ] Task runner trong VS Code (`tasks.json`): one-click start frontend/backend local.
- [ ] Docker extension (neu ban dockerize sau nay).

Khong extension nao co the bien free ngrok URL thanh domain co dinh 24/7.

---

## 6) Acceptance test (done = on dinh)

- [ ] Tat may/ngu may, mo lai -> app va api van khoi dong dung cach da dinh.
- [ ] Tao hoa don voi khach moi (email moi) thanh cong tren POS/Hoa don.
- [ ] Gui mail tra cuu va mo link thanh cong sau 24h (khong doi URL).
- [ ] Khong can sua tay origin moi lan lam viec.

---

## 7) De xuat lo trinh 3 buoc cho ban

- [ ] Buoc 1 (hom nay): chot local stable profile + don dep secret.
- [ ] Buoc 2 (1-2 ngay): len VPS + domain + SSL + reverse proxy.
- [ ] Buoc 3 (sau do): monitoring + backup + runbook su co.
