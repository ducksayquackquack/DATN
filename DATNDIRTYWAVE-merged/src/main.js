import { createApp } from "vue";
import { createPinia } from "pinia";
import axios from "axios";
import App from "./App.vue";
import router from "./router";
import { installScopedAuthStorage } from "./utils/installAuthStorageProxy";
import "./assets/admin.css"

const VITE_CHUNK_RELOAD_FLAG = "dw:vite-preload-reload";
const VITE_CHUNK_RELOAD_WINDOW_MS = 15000;

window.addEventListener("vite:preloadError", (event) => {
  event.preventDefault();
  try {
    const lastReload = Number(sessionStorage.getItem(VITE_CHUNK_RELOAD_FLAG) || 0);
    const isRecent = Number.isFinite(lastReload) && lastReload > 0 && (Date.now() - lastReload) < VITE_CHUNK_RELOAD_WINDOW_MS;
    if (isRecent) return;
    sessionStorage.setItem(VITE_CHUNK_RELOAD_FLAG, String(Date.now()));
  } catch {
    // Ignore storage errors; hard reload below still works.
  }
  window.location.reload();
});

installScopedAuthStorage();

axios.interceptors.request.use((config) => {
  const role = String(localStorage.getItem("role") || "").trim();
  if (role) {
    config.headers = config.headers || {};
    config.headers["X-User-Role"] = role;
  }
  // Debug: log all outgoing requests
  if (config.method === 'post' || config.method === 'put') {
    console.warn(`[AXIOS ${config.method.toUpperCase()}] ${config.url}`, JSON.stringify(config.data))
  }
  return config;
});

axios.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (error?.config?.__silentErrors) {
      return Promise.reject(error)
    }
    if (error?.response) {
      const rawBody = typeof error.response.data === 'string'
        ? error.response.data
        : JSON.stringify(error.response.data)
      const compactBody = String(rawBody || '').slice(0, 320)
      console.error(
        `[AXIOS ERR] ${error.config?.method?.toUpperCase()} ${error.config?.url} → ${error.response.status}`,
        compactBody,
        'Headers:',
        JSON.stringify(Object.fromEntries(Object.entries(error.response.headers || {})))
      )
    }
    return Promise.reject(error)
  }
);

createApp(App).use(createPinia()).use(router).mount("#app");
