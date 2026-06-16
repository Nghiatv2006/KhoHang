<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import StatusBadge from '../components/StatusBadge.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))

const pwForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const pwSaving = ref(false)
const showPwds = reactive({ current: false, new: false, confirm: false })
const pwError = ref('')

async function refreshUser() {
  try {
    const res = await api.get('/api/users/me')
    if (res.ok) {
      const data = await res.json()
      user.value = data
      localStorage.setItem('wh_user', JSON.stringify(data))
    }
  } catch {}
}

async function changePassword() {
  pwError.value = ''
  if (!pwForm.currentPassword || !pwForm.newPassword || !pwForm.confirmPassword) {
    pwError.value = 'Vui lòng nhập đầy đủ tất cả các trường.'; return
  }
  if (pwForm.newPassword.length < 6) {
    pwError.value = 'Mật khẩu mới phải có ít nhất 6 ký tự.'; return
  }
  if (pwForm.newPassword !== pwForm.confirmPassword) {
    pwError.value = 'Mật khẩu mới và xác nhận không khớp.'; return
  }
  pwSaving.value = true
  try {
    const res = await api.put('/api/users/me/change-password', {
      currentPassword: pwForm.currentPassword,
      newPassword: pwForm.newPassword,
    })
    const data = await res.json()
    if (res.ok) {
      toast.success('Đổi mật khẩu thành công!')
      Object.assign(pwForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    } else {
      pwError.value = data.message || 'Có lỗi xảy ra.'
    }
  } catch { pwError.value = 'Không thể kết nối máy chủ.' }
  finally { pwSaving.value = false }
}

const roleColors: Record<string, string> = {
  ADMIN: 'from-blue-500 to-blue-700',
  MANAGER: 'from-teal-500 to-teal-700',
  STAFF: 'from-slate-500 to-slate-600',
}

onMounted(refreshUser)
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-5">
    <!-- Profile Card -->
    <div class="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
      <!-- Cover gradient -->
      <div :class="['h-24 bg-gradient-to-r', roleColors[user?.role] || 'from-slate-500 to-slate-600']" />

      <!-- Avatar + Info -->
      <div class="px-8 pb-8">
        <div class="flex items-start gap-5 mb-6">
          <div class="w-20 h-20 -mt-10 rounded-2xl bg-white border-4 border-white shadow-lg flex items-center justify-center text-3xl font-bold text-[#0052cc] flex-shrink-0">
            {{ user?.fullName?.charAt(0) || '?' }}
          </div>
          <div class="pt-1.5">
            <h2 class="text-xl font-bold text-slate-800 leading-tight">{{ user?.fullName }}</h2>
            <div class="text-sm text-slate-500 font-mono mt-0.5">@{{ user?.username }}</div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="bg-slate-50 rounded-xl px-4 py-3">
            <div class="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">Vai trò</div>
            <StatusBadge :value="user?.role" type="role" />
          </div>
          <div class="bg-slate-50 rounded-xl px-4 py-3">
            <div class="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">Trạng thái</div>
            <StatusBadge :value="user?.status || 'ACTIVE'" type="status" />
          </div>
          <div class="bg-slate-50 rounded-xl px-4 py-3 col-span-2">
            <div class="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">Chi nhánh</div>
            <div class="flex items-center gap-2 text-sm text-slate-700">
              <span class="material-symbols-outlined text-emerald-500 text-base"
                style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">store</span>
              {{ user?.branchName || 'Chưa phân công chi nhánh' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Change Password -->
    <div class="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
      <h3 class="text-base font-semibold text-slate-800 mb-5 flex items-center gap-2">
        <span class="material-symbols-outlined text-[#0052cc] text-lg"
          style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">lock_reset</span>
        Đổi mật khẩu
      </h3>

      <div class="space-y-4">
        <!-- Current Password -->
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Mật khẩu hiện tại <span class="text-red-500">*</span></label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">lock</span>
            <input
              v-model="pwForm.currentPassword"
              :type="showPwds.current ? 'text' : 'password'"
              placeholder="Mật khẩu hiện tại..."
              class="w-full h-11 pl-10 pr-11 border-2 border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 transition-all"
              :class="{ 'border-red-400 focus:border-red-400 focus:ring-red-400/10': pwError }"
            />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600" @click="showPwds.current = !showPwds.current">
              <span class="material-symbols-outlined text-lg">{{ showPwds.current ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </div>
        </div>

        <!-- New Password -->
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Mật khẩu mới <span class="text-red-500">*</span></label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">lock_open</span>
            <input
              v-model="pwForm.newPassword"
              :type="showPwds.new ? 'text' : 'password'"
              placeholder="Tối thiểu 6 ký tự..."
              class="w-full h-11 pl-10 pr-11 border-2 border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 transition-all"
            />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600" @click="showPwds.new = !showPwds.new">
              <span class="material-symbols-outlined text-lg">{{ showPwds.new ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </div>
        </div>

        <!-- Confirm Password -->
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Xác nhận mật khẩu mới <span class="text-red-500">*</span></label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">lock_open</span>
            <input
              v-model="pwForm.confirmPassword"
              :type="showPwds.confirm ? 'text' : 'password'"
              placeholder="Nhập lại mật khẩu mới..."
              class="w-full h-11 pl-10 pr-11 border-2 border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 transition-all"
              :class="{ 'border-red-400 focus:border-red-400': pwForm.confirmPassword && pwForm.newPassword !== pwForm.confirmPassword }"
            />
            <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600" @click="showPwds.confirm = !showPwds.confirm">
              <span class="material-symbols-outlined text-lg">{{ showPwds.confirm ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </div>
          <p v-if="pwForm.confirmPassword && pwForm.newPassword !== pwForm.confirmPassword" class="text-xs text-red-500 mt-1">Mật khẩu không khớp.</p>
        </div>

        <!-- Error -->
        <Transition name="err">
          <div v-if="pwError" class="flex items-center gap-2 text-sm text-red-600 bg-red-50 border border-red-100 rounded-xl px-4 py-3">
            <span class="material-symbols-outlined text-base">error</span>
            {{ pwError }}
          </div>
        </Transition>

        <!-- Submit -->
        <button
          class="w-full h-11 rounded-xl text-white font-semibold text-sm transition-all flex items-center justify-center gap-2 shadow-sm"
          style="background: linear-gradient(135deg, #003d9b, #0052cc);"
          :disabled="pwSaving"
          :style="pwSaving ? 'opacity:0.7;cursor:not-allowed' : ''"
          @click="changePassword"
        >
          <span v-if="pwSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          <span v-else class="material-symbols-outlined text-lg">lock_reset</span>
          {{ pwSaving ? 'Đang cập nhật...' : 'Đổi mật khẩu' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.err-enter-active, .err-leave-active { transition: all 0.2s ease; }
.err-enter-from, .err-leave-to { opacity: 0; transform: translateY(-4px); }
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }
</style>
