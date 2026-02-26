<template>
  <div class="auth">

    <div class="auth-brand">
      <div class="logo"></div>
      <b>DENIM STORE</b>
    </div>

    <div class="auth-card">
      <div class="head">
        <div>
          <h1>Đăng nhập</h1>
          <small>Nhập thông tin để tiếp tục</small>
        </div>
        <span class="pill">Secure</span>
      </div>

      <div class="body">

        <div v-if="error" class="alert-error">
          {{ error }}
        </div>

        <form @submit.prevent="handleLogin">

          <!-- EMAIL -->
          <div class="field">
            <label>Email</label>
            <input
              v-model="email"
              type="email"
              placeholder="vd: a@gmail.com"
              required
            />
            <small>Anh có thể dùng email để đăng nhập.</small>
          </div>

          <!-- PASSWORD -->
          <div class="field">
            <label>Mật khẩu</label>

            <div class="pw">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                class="eye"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? "🙈" : "👁️" }}
              </button>
            </div>

            <!-- REMEMBER + FORGOT -->
            <div class="remember-row">
              <label class="remember">
                <input type="checkbox" v-model="remember" />
                <span>Ghi nhớ đăng nhập</span>
              </label>

              <a href="#" class="forgot">Quên mật khẩu?</a>
            </div>
          </div>

          <!-- LOGIN BUTTON -->
          <button class="btn-login" type="submit">
            Đăng nhập
          </button>

          <!-- DIVIDER -->
          <div class="divider">
            <span></span>
            <p>hoặc</p>
            <span></span>
          </div>

          <!-- SOCIAL -->
          <div class="social">
            <button type="button">G • Google</button>
            <button type="button">f • Facebook</button>
          </div>

          <!-- REGISTER -->
          <div class="register">
            <span>Chưa có tài khoản?</span>
            <a href="#">Tạo tài khoản</a>
          </div>

          <!-- QUICK LINKS -->
          <div class="quick">
            <span>Đi nhanh:</span>
            <button type="button">Customer</button>
            <button type="button">Employee</button>
            <button type="button">Admin</button>
          </div>

        </form>
      </div>
    </div>

    <div class="copyright">
      © {{ year }} Denim Store
    </div>

  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "Login",
  data() {
    return {
      email: "",
      password: "",
      error: null,
      showPassword: false,
      remember: false,
      year: new Date().getFullYear()
    };
  },
  methods: {
    async handleLogin() {
      this.error = null;

      try {
        const params = new URLSearchParams();
        params.append("username", this.email);
        params.append("password", this.password);

        const response = await axios.post(
          "http://localhost:8080/login",
          params,
          {
            withCredentials: true,
            headers: {
              "Content-Type": "application/x-www-form-urlencoded"
            }
          }
        );

        const role = response.data.role;

        if (role === "ROLE_ADMIN" || role === "ROLE_EMPLOYEE") {
          this.$router.push("/admin");
        } else if (role === "ROLE_CUSTOMER") {
          this.$router.push("/home");
        } else {
          this.$router.push("/");
        }

      } catch (err) {
        this.error = "Email hoặc mật khẩu không đúng";
      }
    }
  }
};
</script>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 40px 20px;
  background:
    radial-gradient(900px 700px at 20% -10%, rgba(94,234,212,.15), transparent 60%),
    radial-gradient(900px 700px at 90% 10%, rgba(255,204,0,.12), transparent 60%),
    #0b0c10;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  margin-bottom: 20px;
  font-size: 18px;
}

.logo {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffcc00, #5eead4);
}

.auth-card {
  width: 520px;
  background: rgba(18,20,26,.95);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0,0,0,.4);
  color: white;
  overflow: hidden;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid rgba(255,255,255,.08);
}

.body {
  padding: 24px;
}

.field {
  margin-bottom: 20px;
}

.field label {
  font-size: 14px;
  display: block;
  margin-bottom: 6px;
}

.field input {
  width: 100%;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255,255,255,.1);
  background: rgba(255,255,255,.05);
  color: white;
}

.field small {
  font-size: 12px;
  opacity: .6;
}

.pw {
  display: flex;
  gap: 10px;
}

.eye {
  width: 46px;
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,.1);
  background: rgba(255,255,255,.05);
  cursor: pointer;
}

.remember-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 17px;
}

.remember {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.remember input {
  width: 30px;
  height: 13px;
  accent-color: #ffcc00;
  cursor: pointer;
}

.forgot {
  font-size: 13px;
  color: #60a5fa;
  text-decoration: none;
}

.forgot:hover {
  text-decoration: underline;
}

.btn-login {
  width: 100%;
  padding: 14px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #ffcc00, #5eead4);
  font-weight: 600;
  margin-top: 18px;
  cursor: pointer;
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 22px 0;
}

.divider span {
  flex: 1;
  height: 1px;
  background: rgba(255,255,255,.08);
}

.social {
  display: flex;
  gap: 12px;
}

.social button {
  flex: 1;
  padding: 12px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,.1);
  background: transparent;
  color: white;
  cursor: pointer;
}

.register {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
}

.register span {
  margin-right: 6px;
}

.register a {
  font-weight: 600;
  color: #60a5fa;
}

.quick {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.quick button {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,.1);
  background: transparent;
  color: white;
  font-size: 13px;
  cursor: pointer;
}

.pill {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255,204,0,.15);
  border: 1px solid rgba(255,204,0,.3);
}

.alert-error {
  background: rgba(220,38,38,.15);
  color: #fecaca;
  padding: 10px;
  border-radius: 12px;
  margin-bottom: 16px;
  font-size: 13px;
}

.copyright {
  margin-top: 24px;
  color: rgba(255,255,255,.5);
  font-size: 13px;
}
</style>