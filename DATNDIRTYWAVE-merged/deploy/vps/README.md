# VPS Deploy Template (Nginx + Spring Service)

This folder is a ready-to-use template for a stable 24/7 deployment.

## 1) Frontend build and upload

From local machine:

```powershell
npm run build
```

Upload `dist` to VPS path `/var/www/dirtywave/current/dist`.

## 2) Nginx config

- Copy [nginx/dirtywave.conf](nginx/dirtywave.conf) to `/etc/nginx/sites-available/dirtywave.conf`
- Create symlink to `sites-enabled`
- Replace domain placeholders with real domains
- Validate and reload:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 3) Backend service (Spring)

- Put Spring JAR at `/opt/dirtywave/api/app.jar`
- Create env file from [systemd/dirtywave-api.env.example](systemd/dirtywave-api.env.example) to `/opt/dirtywave/api/.env`
- Copy [systemd/dirtywave-api.service](systemd/dirtywave-api.service) to `/etc/systemd/system/dirtywave-api.service`
- Enable service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable dirtywave-api
sudo systemctl restart dirtywave-api
sudo systemctl status dirtywave-api --no-pager
```

## 4) SSL certificates

Use certbot for both app and api domains.

```bash
sudo certbot --nginx -d app.yourdomain.com -d api.yourdomain.com
```

## 5) Frontend environment

For production build use `.env.production` in this repo:

- `VITE_PUBLIC_APP_ORIGIN=https://app.yourdomain.com`
- `VITE_API_ORIGIN=https://api.yourdomain.com`

## 6) Smoke checks

- Open `https://app.yourdomain.com`
- Call `https://api.yourdomain.com/api/...`
- Create invoice from POS and verify email lookup link points to app domain
