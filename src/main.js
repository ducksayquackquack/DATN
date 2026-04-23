import { createApp } from "vue";
import { createPinia } from "pinia";
import axios from "axios";
import App from "./App.vue";
import router from "./router";
import { installScopedAuthStorage } from "./utils/installAuthStorageProxy";
import "./assets/admin.css"

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
    if (error?.response) {
      console.error(`[AXIOS ERR] ${error.config?.method?.toUpperCase()} ${error.config?.url} → ${error.response.status}`, JSON.stringify(error.response.data), 'Headers:', JSON.stringify(Object.fromEntries(Object.entries(error.response.headers || {}))))
    }
    return Promise.reject(error)
  }
);

createApp(App).use(createPinia()).use(router).mount("#app");
