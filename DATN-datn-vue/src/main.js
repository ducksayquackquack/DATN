import { createApp } from "vue";
import { createPinia } from "pinia";
import axios from "axios";
import App from "./App.vue";
import router from "./router";
import "./assets/admin.css"

axios.interceptors.request.use((config) => {
  const role = String(localStorage.getItem("role") || "").trim();
  if (role) {
    config.headers = config.headers || {};
    config.headers["X-User-Role"] = role;
  }
  return config;
});

createApp(App).use(createPinia()).use(router).mount("#app");
