<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import logo from '../../assets/img/DirtyWaveLogo.png'

const router = useRouter()

const identity = ref('')
const password = ref('')
const error = ref(null)
const showPassword = ref(false)
const remember = ref(false)

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const login = async () => {
  error.value = null
  try {
    const params = new URLSearchParams()
    params.append('username', identity.value)
    params.append('password', password.value)

    const response = await axios.post(
      'http://localhost:8080/login',
      params,
      {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    )

    const role = response.data.role

    if (role === 'ROLE_ADMIN') router.push('/admin')
    else if (role === 'ROLE_EMPLOYEE') router.push('/employee/dashboard')
    else if (role === 'ROLE_CUSTOMER') router.push('/home')
    else router.push('/')
  } catch (e) {
    error.value = 'Email hoặc mật khẩu không đúng'
  }
}
</script>

<template>
  <div class="auth">

    <router-link to="/home" class="auth-brand">
      <div class="brand">
        <div class="brand-logo">
          <img :src="logo" alt="DirtyWave" />
        </div>
        <div class="brand-text">
          <div class="brand-name">DirtyWave</div>
          <div class="brand-sub">WELCOME BACK</div>
        </div>
      </div>
    </router-link>

    <div class="card auth-card">
      <div class="head">
        <div>
          <h1>Đăng nhập</h1>
          <small class="muted">Nhập thông tin để tiếp tục</small>
        </div>
        <span class="pill">Secure</span>
      </div>

      <div class="body">
        <form class="grid" autocomplete="on" @submit.prevent="login">

          <div class="field">
            <label>Email hoặc SĐT</label>
            <input
              v-model="identity"
              type="text"
              placeholder="vd: a@gmail.com / 0909xxxxxx"
              required
            />
            <small class="muted">
              Anh có thể dùng email hoặc số điện thoại.
            </small>
          </div>

          <div class="field">
            <label>Mật khẩu</label>

            <div class="pw">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="password"
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                class="iconbtn"
                @click="togglePassword"
              >
                👁️
              </button>
            </div>

            <div class="row">
              <label class="remember">
                <input type="checkbox" v-model="remember" />
                <span>Ghi nhớ đăng nhập</span>
              </label>

              <a class="link" href="#" @click.prevent>
                Quên mật khẩu?
              </a>
            </div>
          </div>

          <button class="btn primary" type="submit">
            Đăng nhập
          </button>

          <div v-if="error" class="error">
            {{ error }}
          </div>

          <div class="or">
            <span></span>
            <small class="muted">hoặc</small>
            <span></span>
          </div>

          <div class="grid cols2">
            <button class="btn" type="button">
              G • Google
            </button>
            <button class="btn" type="button">
              f • Facebook
            </button>
          </div>

          <div class="footnote">
            <small class="muted">Chưa có tài khoản?</small>
            <router-link to="/register" class="link">
              Tạo tài khoản
            </router-link>
          </div>

        </form>
      </div>
    </div>

    <div class="footer">
      © {{ new Date().getFullYear() }} DirtyWave
    </div>

  </div>
</template>

<style scoped>
@import './login.css';

.auth-card {
  width: min(720px, 96vw);
  padding: 20px 16px 28px;
}

h1 {
  font-size: 26px;
}

label {
  font-size: 16px;
  font-weight: 600;
}

.field input,
.pw input {
  font-size: 16px;
  padding: 14px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0,0,0,.15);
  background: #eef1f7;
  color: #111;
}

.pw {
  position: relative;
}

.pw input {
  padding-right: 50px;
}

.iconbtn {
  position: absolute;
  right: 14px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
}

.remember {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 15px;
}

.link {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.muted {
  font-size: 15px;
  color: rgba(0,0,0,.6);
}

.btn {
  font-size: 16px;
}

.footer {
  text-align: center;
  font-size: 15px;
  margin-top: 24px;
  color: rgba(0,0,0,.6);
}

.error {
  margin-top: 12px;
  color: #e53935;
  text-align: center;
  font-size: 15px;
}
</style>