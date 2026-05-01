# ZIP-READY 1-CLICK CHECKLIST (2 PROJECTS)

Muc tieu: tao ban zip gon, de gui cho nguoi khac chay nhanh, khong bi kem file rac (node_modules, build, secrets).

## 1) Project A: DATNDIRTYWAVE-merged (frontend + scripts + backend Node mail)

### Include
- src/
- public/
- backend/ (chi source, khong include backend/node_modules)
- scripts/
- deploy/
- index.html
- package.json
- package-lock.json (neu co)
- vite.config.js
- README*.md
- *.sql
- *.code-workspace (neu can)
- .env.example (neu co)

### Exclude
- node_modules/
- backend/node_modules/
- dist/
- coverage/
- .git/
- .github/
- .vscode/
- .idea/
- logs/
- tmp/
- .cache/
- .env
- .env.local
- .env.* (giu lai duy nhat .env.example)
- npm-debug.log / yarn-error.log / pnpm-debug.log

## 2) Project B: DATN-API (Spring)

### Include
- src/
- pom.xml
- mvnw
- mvnw.cmd
- .mvn/
- README*.md
- application*.yml hoac application*.properties (ban da bo secrets)
- SQL migration scripts (neu tach rieng)

### Exclude
- target/
- .git/
- .idea/
- .vscode/
- logs/
- .env
- *.keystore, *.jks, *.p12
- credentials files chua secret

## 3) One-click command

Chay lenh nay trong project DATNDIRTYWAVE-merged:

powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\create-zip-ready.ps1

Mac dinh script tao 2 file zip vao thu muc release/:
- DATNDIRTYWAVE-merged-zipready-YYYYMMDD-HHmm.zip
- DATN-API-zipready-YYYYMMDD-HHmm.zip (neu tim thay folder DATN-API)

Neu DATN-API nam o cho khac:

powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\create-zip-ready.ps1 -BackendPath "D:\path\to\DATN-API"

Neu tam thoi chi can zip frontend repo:

powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\create-zip-ready.ps1 -SkipBackend

## 4) Quick verify truoc khi gui

- Mo file zip va kiem tra KHONG co node_modules, dist, target, .git, .env local.
- Co day du package.json, pom.xml, source code, scripts can thiet.
- Nguoi nhan co the chay bang:
  - Frontend repo: npm install ; npm --prefix backend install ; npm run dev
  - Spring API repo: .\\mvnw.cmd spring-boot:run
