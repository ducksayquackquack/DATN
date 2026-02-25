<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>Admin Login</h1>
        <p>Enter email and password to continue</p>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <form @submit.prevent="handleLogin">
        <div class="field">
          <input
            v-model="email"
            type="email"
            placeholder="Email"
            required
          />
        </div>

        <div class="field">
          <input
            v-model="password"
            type="password"
            placeholder="Password"
            required
          />
        </div>

        <button type="submit" class="btn-submit">
          Login
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "AdminLogin",

  data() {
    return {
      email: "",
      password: "",
      error: null
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

    console.log("ROLE:", role);

    if (role === "ROLE_ADMIN" || role === "ROLE_EMPLOYEE") {
      this.$router.push("/admin");
    } else if (role === "ROLE_CUSTOMER") {
      this.$router.push("/home");
    } else {
      this.$router.push("/");
    }

  } catch (err) {
    console.error(err);
    this.error = "Email or password is incorrect";
  }
}
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f6f8;
}

.login-card {
  width: 400px;
  background: #fff;
  padding: 26px;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.1);
}

.login-header h1 {
  margin-bottom: 6px;
  font-size: 20px;
}

.login-header p {
  font-size: 13px;
  color: #6b7280;
}

.field {
  margin-bottom: 12px;
}

.field input {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
}

.btn-submit {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  border: none;
}

.alert-error {
  background: #fde8e8;
  color: #991b1b;
  padding: 10px;
  border-radius: 10px;
  margin-bottom: 12px;
}
</style>