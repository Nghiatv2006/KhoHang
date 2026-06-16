<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const showPwd = ref(false)

async function handleLogin() {
  if (!form.username.trim() || !form.password) {
    errorMsg.value = 'Vui lòng nhập đầy đủ thông tin.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/api/auth/login', {
      username: form.username.trim(),
      password: form.password,
    })
    const data = await res.json()
    if (res.ok) {
      localStorage.setItem('wh_user', JSON.stringify(data))
      router.push('/dashboard')
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
  <div class="min-h-screen flex bg-white font-sans">
    <!-- Left Panel (Login Form) -->
    <div class="w-full lg:w-[45%] xl:w-[40%] flex flex-col justify-center px-8 sm:px-16 lg:px-24 xl:px-32 relative bg-white z-10">
      
      <!-- Top Brand -->
      <div class="absolute top-10 left-10 flex items-center gap-3">
        <div class="w-10 h-10 bg-[#0047b3] rounded-lg flex items-center justify-center shadow-md">
          <span class="material-symbols-outlined text-white text-xl" style="font-variation-settings: 'FILL' 1">inventory_2</span>
        </div>
        <span class="text-xl font-bold text-slate-800 tracking-tight">Warehouse_Management</span>
      </div>

      <div class="mx-auto mt-16 lg:mt-0" style="width: 100%; max-width: 384px;">
        <h1 class="text-[32px] font-bold text-slate-900 mb-3 tracking-tight">Đăng nhập</h1>
        <p class="text-slate-500 mb-10 text-[15px]">Truy cập hệ thống quản lý chi nhánh và kho bãi.</p>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- Error Alert -->
          <div v-if="errorMsg" class="p-3 bg-red-50 text-red-600 text-sm rounded-lg border border-red-100 flex items-center gap-2">
            <span class="material-symbols-outlined text-base">error</span>
            {{ errorMsg }}
          </div>

          <!-- Username Field -->
          <div class="space-y-1.5">
            <label class="block text-[13px] font-bold text-slate-700">Email hoặc Tên đăng nhập</label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <span class="material-symbols-outlined text-[20px]">mail</span>
              </div>
              <input
                v-model="form.username"
                type="text"
                placeholder="nhanvien@congty.com"
                class="w-full h-11 pl-10 pr-4 bg-white border border-slate-300 rounded-md text-sm text-slate-800 placeholder-slate-400 focus:border-[#0047b3] focus:ring-1 focus:ring-[#0047b3] outline-none transition-all"
              />
            </div>
          </div>

          <!-- Password Field -->
          <div class="space-y-1.5 pt-1">
            <div class="flex items-center justify-between">
              <label class="block text-[13px] font-bold text-slate-700">Mật khẩu</label>
              <a href="#" class="text-[13px] font-semibold text-[#0047b3] hover:underline">Quên mật khẩu?</a>
            </div>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <span class="material-symbols-outlined text-[20px]">lock</span>
              </div>
              <input
                v-model="form.password"
                :type="showPwd ? 'text' : 'password'"
                placeholder="••••••••"
                class="w-full h-11 pl-10 pr-10 bg-white border border-slate-300 rounded-md text-sm text-slate-800 placeholder-slate-400 focus:border-[#0047b3] focus:ring-1 focus:ring-[#0047b3] outline-none transition-all tracking-[0.2em]"
              />
              <button
                type="button"
                tabindex="-1"
                class="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 transition-colors"
                @click="showPwd = !showPwd"
              >
                <span class="material-symbols-outlined text-[20px]">{{ showPwd ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <!-- Remember Me -->
          <div class="flex items-center gap-2.5 pt-2 pb-1">
            <input type="checkbox" id="remember" class="w-4 h-4 text-[#0047b3] rounded border-slate-300 focus:ring-[#0047b3]" />
            <label for="remember" class="text-[13px] text-slate-600 cursor-pointer select-none">Duy trì đăng nhập trên thiết bị này</label>
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full h-11 bg-[#0047b3] hover:bg-[#003d9b] text-white text-[15px] font-semibold rounded-md flex items-center justify-center gap-2 transition-colors shadow-sm"
          >
            <template v-if="loading">
              <span class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              Đang kết nối...
            </template>
            <template v-else>
              Đăng nhập hệ thống
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </template>
          </button>
        </form>

        <p class="text-[13px] text-slate-500 text-center mt-8 leading-relaxed max-w-[280px] mx-auto">
          Bằng việc đăng nhập, bạn đồng ý với <RouterLink to="/privacy-policy" class="text-[#0047b3] hover:underline font-medium">Chính sách bảo mật</RouterLink> của chúng tôi.
        </p>
      </div>
    </div>

    <!-- Right Panel (Hero Image & Copy) -->
    <div class="hidden lg:flex flex-1 relative bg-slate-900 items-center">
      <!-- Background Image -->
      <div class="absolute inset-0">
        <img 
          src="https://images.unsplash.com/photo-1586528116311-ad8ed7c80a30?q=80&w=2070&auto=format&fit=crop" 
          alt="Warehouse" 
          class="w-full h-full object-cover opacity-30 mix-blend-luminosity"
        />
        <div class="absolute inset-0 bg-gradient-to-r from-slate-900 via-slate-900/80 to-slate-900/50"></div>
        <div class="absolute inset-0 bg-[#002a66]/40 mix-blend-multiply"></div>
      </div>

      <!-- Content -->
      <div class="relative z-10 pl-20 pr-12 text-white" style="width: 100%; max-width: 672px;">
        
        <div class="flex items-center gap-2.5 text-slate-300 mb-8 uppercase tracking-widest text-xs font-bold bg-white/10 w-fit px-4 py-2 rounded-full backdrop-blur-sm border border-white/10">
          <span class="material-symbols-outlined text-lg">account_balance</span>
          Phiên bản Doanh nghiệp
        </div>

        <h2 class="text-[3.25rem] font-bold leading-[1.1] mb-6 tracking-tight">Kiểm soát toàn diện chuỗi cung ứng.</h2>
        
        <p class="text-lg text-slate-300/90 leading-relaxed mb-16 font-light" style="max-width: 576px;">
          Hệ thống cốt lõi dành cho quản lý tổng kho và đa chi nhánh. Theo dõi luồng hàng hóa theo thời gian thực với độ chính xác tuyệt đối.
        </p>

        <div class="flex items-center gap-16 border-t border-white/10 pt-10">
          <div>
            <div class="text-[2.5rem] font-bold mb-1 font-mono tracking-tight">99.9%</div>
            <div class="text-[15px] font-medium text-slate-400">Uptime Hệ thống</div>
          </div>
          <div>
            <div class="text-[2.5rem] font-bold mb-1 font-mono tracking-tight">256-bit</div>
            <div class="text-[15px] font-medium text-slate-400">Mã hóa Dữ liệu</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Optional animations can go here */
</style>
