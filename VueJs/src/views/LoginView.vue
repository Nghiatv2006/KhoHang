<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const banMsg = ref('') // Thông báo bị phạt spam
const showPwd = ref(false)

async function handleLogin() {
  if (!form.username.trim() || !form.password) {
    errorMsg.value = 'Vui lòng nhập đầy đủ thông tin.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  banMsg.value = ''
  try {
    const res = await api.post('/api/auth/login', {
      username: form.username.trim(),
      password: form.password,
    })
    const data = await res.json()
    if (res.ok) {
      localStorage.setItem('wh_user', JSON.stringify(data))
      router.push('/dashboard')
    } else if (res.status === 429) {
      // Bị phạt spam
      const banUntilRaw = data.banUntil
      if (banUntilRaw) {
        const dt = new Date(banUntilRaw).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'medium' })
        banMsg.value = `⚠️ Tài khoản bị tạm khóa do thao tác quá nhanh. Vui lòng thử lại sau: ${dt}`
      } else {
        banMsg.value = data.message || 'Thất bại. Vui lòng thử lại sau.'
      }
    } else {
      errorMsg.value = data.message || 'Tên đăng nhập hoặc mật khẩu không đúng.'
    }
  } catch (err: any) {
    errorMsg.value = 'Không thể kết nối đến máy chủ. ' + (err.message || '')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex relative font-['Nunito',sans-serif] overflow-hidden bg-[#0a192f]">
    
    <!-- Global Animated Background -->
    <div class="absolute inset-0 z-0">
      <img 
        src="https://images.unsplash.com/photo-1553413077-190dd305871c?q=80&w=2070&auto=format&fit=crop" 
        alt="Enterprise Warehouse" 
        class="w-full h-full object-cover scale-105 animate-slow-pan opacity-70 mix-blend-luminosity"
      />
    </div>

    <!-- Left Panel Overlay & Content (Form) -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-4 relative z-10">
      
      <!-- Lớp phủ 30% -->
      <div class="absolute inset-0 bg-black/30 backdrop-blur-[2px] border-r border-white/10"></div>
      
      <!-- Login Card -->
      <div class="relative z-20 bg-white/95 backdrop-blur-xl rounded-[20px] shadow-[0_15px_40px_rgba(0,0,0,0.1)] p-8 md:p-12 w-full max-w-[480px] border border-white/60">
        
        <!-- Logo & Header -->
        <div class="text-center mb-8">
          <div class="w-[80px] h-[80px] bg-[#ffeaa7] rounded-full flex items-center justify-center mx-auto mb-4 text-[#d63031] shadow-[0_5px_15px_rgba(214,48,49,0.2)]">
            <i class="fas fa-boxes fa-3x"></i>
          </div>
          <h2 class="text-[32px] font-extrabold text-gray-900 mb-2">Warehouse Pro</h2>
          <p class="text-gray-500 font-medium">Đăng nhập hệ thống quản trị</p>
        </div>

        <!-- Alert Messages -->
        <div v-if="banMsg" class="bg-amber-50 border border-amber-300 text-amber-800 px-4 py-3 rounded-xl shadow-sm mb-6 flex items-start gap-3 text-sm font-semibold">
          <i class="fas fa-ban text-amber-500 text-lg mt-0.5 shrink-0"></i>
          <span>{{ banMsg }}</span>
        </div>
        <div v-else-if="errorMsg" class="bg-[#f8d7da] text-[#721c24] px-4 py-3 rounded-xl shadow-sm border-0 mb-6 flex items-center gap-3 text-sm font-semibold">
          <i class="fas fa-exclamation-triangle text-lg"></i> <span>{{ errorMsg }}</span>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleLogin">
          
          <!-- Smooth Custom Floating Label: Username -->
          <div class="form-floating-custom mb-5">
            <input 
              type="text" 
              id="usernameInput" 
              v-model="form.username" 
              placeholder=" " 
              required 
              autofocus
            />
            <label for="usernameInput">Tên đăng nhập</label>
            <i class="fas fa-user icon"></i>
          </div>

          <!-- Smooth Custom Floating Label: Password -->
          <div class="form-floating-custom mb-2">
            <input 
              :type="showPwd ? 'text' : 'password'" 
              id="passwordInput" 
              v-model="form.password" 
              placeholder=" " 
              required 
              style="padding-right: 3rem;"
            />
            <label for="passwordInput">Mật khẩu</label>
            <i class="fas fa-lock icon"></i>
            <button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#d63031] transition-colors z-10" @click="showPwd = !showPwd">
              <i :class="['fas', showPwd ? 'fa-eye-slash' : 'fa-eye', 'text-lg']"></i>
            </button>
          </div>

          <!-- Nút Quên mật khẩu -->
          <a href="#" class="block text-right mt-3 mb-6 text-[#d63031] font-bold text-[0.95rem] transition-all hover:text-[#b00000] hover:underline">
            Quên mật khẩu?
          </a>

          <!-- Nút Đăng nhập -->
          <button 
            type="submit" 
            :disabled="loading"
            class="w-full bg-gradient-to-r from-[#d63031] to-[#ff7675] hover:from-[#c0392b] hover:to-[#d63031] text-white font-[800] p-[14px] rounded-[10px] text-[1.1rem] tracking-[1px] transition-transform hover:-translate-y-[2px] shadow-[0_5px_15px_rgba(214,48,49,0.3)] flex items-center justify-center gap-2"
          >
            <i v-if="loading" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-sign-in-alt"></i>
            {{ loading ? 'ĐANG XỬ LÝ...' : 'ĐĂNG NHẬP NGAY' }}
          </button>
        </form>

      </div>
    </div>

    <!-- Right Panel Overlay & Content -->
    <div class="hidden lg:flex flex-1 relative items-center justify-center z-10">
      
      <!-- Dark Blue Enterprise Overlay -->
      <div class="absolute inset-0 bg-gradient-to-tr from-[#0a192f]/95 via-[#112240]/85 to-[#0047b3]/40 mix-blend-multiply"></div>

      <!-- Professional Content -->
      <div class="relative z-20 px-16 max-w-2xl text-left border-l-4 border-[#00a8ff] pl-8 ml-8">
        <div class="text-[#00a8ff] mb-4 uppercase tracking-[0.2em] text-sm font-extrabold flex items-center gap-2">
          <i class="fas fa-server"></i> Phiên bản Doanh nghiệp
        </div>

        <h2 class="text-[3.5rem] font-extrabold mb-6 tracking-tight text-white leading-[1.15]">
          Kiểm soát toàn diện <br/>
          <span class="pro-gradient-text">chuỗi cung ứng</span>
        </h2>
        
        <p class="text-[1.15rem] text-slate-300 font-light leading-relaxed mb-10">
          Hệ thống lõi cung cấp khả năng hiển thị thời gian thực, quản lý tồn kho đa chi nhánh và tối ưu hóa luồng hàng hóa với độ trễ bằng 0.
        </p>

        <div class="grid grid-cols-2 gap-8 pt-8 border-t border-white/10">
          <div>
            <div class="text-3xl font-bold mb-1 font-mono tracking-tight text-white">99.99%</div>
            <div class="text-sm font-semibold text-slate-400 uppercase tracking-wide">Uptime Hệ thống</div>
          </div>
          <div>
            <div class="text-3xl font-bold mb-1 font-mono tracking-tight text-white">AES-256</div>
            <div class="text-sm font-semibold text-slate-400 uppercase tracking-wide">Mã hóa Dữ liệu</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;500;600;700;800&display=swap');

/* --- CUSTOM SMOOTH FLOATING LABELS --- */
.form-floating-custom {
  position: relative;
  width: 100%;
}

.form-floating-custom input {
  width: 100%;
  height: 55px;
  border-radius: 10px;
  border: 1px solid #dfe6e9;
  padding: 1.25rem 1rem 0.25rem 2.8rem; /* pb is smaller to push text up slightly, pt is larger */
  font-size: 1rem;
  font-weight: 600;
  color: #2d3436;
  outline: none;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
  background-color: transparent;
}

.form-floating-custom input:focus {
  border-color: #d63031;
  box-shadow: 0 0 0 0.25rem rgba(214, 48, 49, 0.15);
}

.form-floating-custom label {
  position: absolute;
  top: 0;
  left: 2.8rem;
  height: 100%;
  padding: 1rem 0;
  pointer-events: none;
  transform-origin: 0 0;
  transition: opacity 0.15s ease-in-out, transform 0.15s ease-in-out;
  color: #636e72;
  font-weight: 600;
  font-size: 1rem;
}

.form-floating-custom input:focus ~ label,
.form-floating-custom input:not(:placeholder-shown) ~ label {
  opacity: 0.7;
  transform: scale(0.8) translateY(-0.65rem);
}

.form-floating-custom .icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: #b2bec3;
  font-size: 1.1rem;
  transition: color 0.15s ease-in-out;
  pointer-events: none;
}

.form-floating-custom input:focus ~ .icon {
  color: #d63031;
}

/* Fix input autofill background */
input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus, 
input:-webkit-autofill:active{
    -webkit-box-shadow: 0 0 0 50px white inset !important;
    -webkit-text-fill-color: #2d3436 !important;
}

/* --- PROFESSIONAL RIGHT PANEL ANIMATIONS --- */
@keyframes slowPan {
  0% { transform: scale(1.0) translate(0, 0); }
  100% { transform: scale(1.15) translate(-5%, 2%); }
}

.animate-slow-pan {
  animation: slowPan 8s ease-in-out infinite alternate;
}

.pro-gradient-text {
  background: linear-gradient(to right, #00a8ff, #9c88ff, #fbc531, #e84118, #00a8ff);
  background-size: 300% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  color: transparent;
  animation: shine 12s ease-in-out infinite alternate;
}

@keyframes shine {
  to {
    background-position: 300% center;
  }
}
</style>
