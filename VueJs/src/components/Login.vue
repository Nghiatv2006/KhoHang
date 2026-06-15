<template>
  <div class="page-bg">
    <div class="container">
      <div class="login-wrapper">
        <div class="login-card">

          <!-- Logo & Header -->
          <div class="login-header">
            <div class="brand-icon">
              <span class="material-symbols-outlined" style="font-size: 36px; font-variation-settings: 'FILL' 1;">warehouse</span>
            </div>
            <h2 class="title-fw-bold">BranchOS</h2>
            <p class="text-muted">Đăng nhập để quản lý kho hàng!</p>
          </div>

          <!-- Alert Messages -->
          <div v-if="error" class="alert-danger">
            <span class="material-symbols-outlined alert-ico">error</span>
            <span>{{ error }}</span>
          </div>

          <!-- Form -->
          <form @submit.prevent="handleLogin" novalidate>

            <!-- Username Floating Label -->
            <div class="form-floating">
              <input 
                type="text" 
                class="form-control" 
                id="usernameInput" 
                v-model="username"
                placeholder="Username" 
                required 
                autofocus
              >
              <label for="usernameInput">
                <span class="material-symbols-outlined input-ico">person</span>
                Tên đăng nhập
              </label>
            </div>

            <!-- Password Floating Label -->
            <div class="form-floating mb-1">
              <input 
                type="password" 
                class="form-control" 
                id="passwordInput" 
                v-model="password"
                placeholder="Password" 
                required
              >
              <label for="passwordInput">
                <span class="material-symbols-outlined input-ico">lock</span>
                Mật khẩu
              </label>
            </div>

            <!-- Nút Quên mật khẩu NỔI BẬT -->
            <a href="#" class="forgot-password-btn">
              Quên mật khẩu?
            </a>

            <!-- Nút Đăng nhập -->
            <button type="submit" class="btn-login" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span v-else>ĐĂNG NHẬP NGAY</span>
            </button>
          </form>

          <!-- Footer removed as per project requirements -->

          <!-- Test Account -->
          <div class="test-account">
            Test: <b>admin/123456</b> hoặc <b>manager/123456</b>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const emit = defineEmits<{ (e: 'login-success', role: string): void }>();

const username = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

const handleLogin = () => {
  error.value = '';
  if (!username.value || !password.value) {
    error.value = 'Vui lòng nhập đầy đủ thông tin.';
    return;
  }

  loading.value = true;
  setTimeout(() => {
    loading.value = false;
    if ((username.value === 'admin' || username.value === 'manager') && password.value === '123456') {
      emit('login-success', username.value);
    } else {
      error.value = 'Tên đăng nhập hoặc mật khẩu không chính xác';
    }
  }, 1200);
};
</script>

<style scoped>
/* Reset & Fonts */
* {
  box-sizing: border-box;
}

.page-bg {
  font-family: 'Geist', 'Nunito', sans-serif;
  /* Ảnh nền kho hàng tạo độ sâu */
  background: linear-gradient(rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0.6)), url('https://images.unsplash.com/photo-1553413077-190dd305871c?w=1920&q=80&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.container {
  width: 100%;
  max-width: 420px;
}

.login-card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.4);
  overflow: hidden;
  border: none;
  padding: 40px 32px;
}

@media (max-width: 576px) {
  .login-card {
    padding: 32px 24px;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.brand-icon {
  width: 80px;
  height: 80px;
  background: #ffeaa7;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
  color: #d63031;
  box-shadow: 0 5px 15px rgba(214, 48, 49, 0.2);
}

.title-fw-bold {
  font-weight: 800;
  color: #212529;
  margin: 0 0 8px 0;
  font-size: 28px;
}

.text-muted {
  color: #6c757d;
  margin: 0;
}

/* Alert */
.alert-danger {
  background-color: #f8d7da;
  color: #842029;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.alert-ico {
  font-size: 20px;
}

/* Floating Label Customization */
.form-floating {
  position: relative;
  margin-bottom: 16px;
}

.form-floating.mb-1 {
  margin-bottom: 4px;
}

.form-control {
  display: block;
  width: 100%;
  padding: 24px 15px 10px 15px; /* Padding top để chừa chỗ cho label nổi lên */
  font-size: 1rem;
  font-weight: 500;
  line-height: 1.5;
  color: #212529;
  background-color: #fff;
  border-radius: 10px;
  border: 1px solid #dfe6e9;
  height: 60px;
  transition: border-color .15s ease-in-out, box-shadow .15s ease-in-out;
  outline: none;
}

.form-control:focus {
  border-color: #d63031;
  box-shadow: 0 0 0 0.25rem rgba(214, 48, 49, 0.15);
}

.form-control::placeholder {
  color: transparent; /* Ẩn placeholder mặc định để dùng label */
}

.form-floating > label {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  padding: 18px 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
  border: 1px solid transparent;
  transform-origin: 0 0;
  transition: opacity .1s ease-in-out, transform .1s ease-in-out;
  color: #6c757d;
  display: flex;
  align-items: center;
}

.input-ico {
  font-size: 18px;
  margin-right: 8px;
  color: #6c757d;
}

/* Logic nổi label lên khi focus hoặc có chữ */
.form-control:focus ~ label,
.form-control:not(:placeholder-shown) ~ label {
  opacity: 0.65;
  transform: scale(.85) translateY(-14px) translateX(15px);
  padding: 18px 0; /* Reset padding để căn chỉnh chuẩn khi scale */
}

/* Nút quên mật khẩu nổi bật */
.forgot-password-btn {
  display: block;
  text-align: right;
  margin-top: 10px;
  margin-bottom: 25px;
  color: #d63031;
  font-weight: 700;
  font-size: 0.95rem;
  text-decoration: none;
  transition: all 0.2s;
}

.forgot-password-btn:hover {
  color: #b00;
  text-decoration: underline;
}

/* Nút Đăng nhập */
.btn-login {
  width: 100%;
  background: linear-gradient(to right, #d63031, #ff7675);
  border: none;
  color: white;
  font-weight: 800;
  padding: 14px;
  border-radius: 10px;
  font-size: 1.1rem;
  letter-spacing: 1px;
  transition: transform 0.2s;
  box-shadow: 0 5px 15px rgba(214, 48, 49, 0.3);
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 56px;
}

.btn-login:hover:not(:disabled) {
  transform: translateY(-2px);
  background: linear-gradient(to right, #c0392b, #d63031);
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Footer & Links */

.test-account {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #dee2e6;
  text-align: center;
  color: #6c757d;
  font-size: 14px;
  opacity: 0.75;
}
</style>
