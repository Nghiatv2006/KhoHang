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
  ADMIN: 'from-[#4361ee] to-[#3a0ca3]',
  MANAGER: 'from-[#05b171] to-[#04935e]',
  STAFF: 'from-[#8094ae] to-[#526484]',
}

onMounted(refreshUser)
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <!-- Header -->
    <div class="mb-6">
      <h2 class="text-2xl font-bold text-[#364a63] m-0">Hồ sơ cá nhân</h2>
      <p class="text-[#8094ae] text-sm mt-1">Quản lý thông tin tài khoản và bảo mật</p>
    </div>

    <!-- Profile Card -->
    <div class="bg-white rounded-[16px] shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] overflow-hidden">
      <!-- Cover gradient -->
      <div :class="['h-28 bg-gradient-to-r', roleColors[user?.role] || 'from-[#8094ae] to-[#526484]']" />

      <!-- Avatar + Info -->
      <div class="px-8 pb-8 relative">
        <div class="flex flex-col sm:flex-row sm:items-end gap-5 mb-8">
          <div class="w-24 h-24 -mt-12 rounded-2xl bg-white border-4 border-white shadow-md flex items-center justify-center text-4xl font-bold text-[#4361ee] flex-shrink-0 z-10 relative">
            {{ user?.fullName?.charAt(0) || '?' }}
          </div>
          <div class="pt-2 sm:pt-0 pb-1">
            <h2 class="text-2xl font-bold text-[#364a63] leading-tight">{{ user?.fullName }}</h2>
            <div class="text-sm text-[#8094ae] font-mono mt-1">@{{ user?.username }}</div>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="bg-[#f8f9fa] rounded-xl px-5 py-4 border border-[#f1f5f9]">
            <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wider mb-2">Vai trò hệ thống</div>
            <StatusBadge :value="user?.role" type="role" />
          </div>
          <div class="bg-[#f8f9fa] rounded-xl px-5 py-4 border border-[#f1f5f9]">
            <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wider mb-2">Trạng thái tài khoản</div>
            <StatusBadge :value="user?.status || 'ACTIVE'" type="status" />
          </div>
          <div class="bg-[#f8f9fa] rounded-xl px-5 py-4 border border-[#f1f5f9] sm:col-span-2">
            <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wider mb-2">Chi nhánh trực thuộc</div>
            <div class="flex items-center gap-3 text-sm font-bold text-[#364a63]">
              <div class="w-8 h-8 rounded-lg bg-[#eef2ff] text-[#4361ee] flex items-center justify-center">
                <i class="fas fa-store"></i>
              </div>
              {{ user?.branchName || 'Chưa phân công chi nhánh' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Change Password -->
    <div class="bg-white rounded-[16px] shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] p-8">
      <h3 class="text-lg font-bold text-[#364a63] mb-6 flex items-center gap-2 pb-4 border-b border-[#f1f5f9]">
        <i class="fas fa-lock text-[#4361ee]"></i>
        Bảo mật & Mật khẩu
      </h3>

      <div class="space-y-5 max-w-xl">
        <!-- Current Password -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mật khẩu hiện tại <span class="text-[#ea4f52]">*</span></label>
          <div class="relative">
            <i class="fas fa-key absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input
              v-model="pwForm.currentPassword"
              :type="showPwds.current ? 'text' : 'password'"
              placeholder="Nhập mật khẩu hiện tại..."
              class="w-full h-11 pl-11 pr-11 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] transition-all"
              :class="{ 'border-[#ea4f52] focus:border-[#ea4f52] focus:ring-[#ea4f52]/20': pwError }"
            />
            <button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-[#8094ae] hover:text-[#364a63] transition-colors cursor-pointer" @click="showPwds.current = !showPwds.current">
              <i :class="['fas', showPwds.current ? 'fa-eye-slash' : 'fa-eye']"></i>
            </button>
          </div>
        </div>

        <!-- New Password -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mật khẩu mới <span class="text-[#ea4f52]">*</span></label>
          <div class="relative">
            <i class="fas fa-unlock-alt absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input
              v-model="pwForm.newPassword"
              :type="showPwds.new ? 'text' : 'password'"
              placeholder="Tối thiểu 6 ký tự..."
              class="w-full h-11 pl-11 pr-11 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] transition-all"
            />
            <button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-[#8094ae] hover:text-[#364a63] transition-colors cursor-pointer" @click="showPwds.new = !showPwds.new">
              <i :class="['fas', showPwds.new ? 'fa-eye-slash' : 'fa-eye']"></i>
            </button>
          </div>
        </div>

        <!-- Confirm Password -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Xác nhận mật khẩu mới <span class="text-[#ea4f52]">*</span></label>
          <div class="relative">
            <i class="fas fa-check-circle absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input
              v-model="pwForm.confirmPassword"
              :type="showPwds.confirm ? 'text' : 'password'"
              placeholder="Nhập lại mật khẩu mới..."
              class="w-full h-11 pl-11 pr-11 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] transition-all"
              :class="{ 'border-[#ea4f52] focus:border-[#ea4f52] focus:ring-[#ea4f52]/20': pwForm.confirmPassword && pwForm.newPassword !== pwForm.confirmPassword }"
            />
            <button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-[#8094ae] hover:text-[#364a63] transition-colors cursor-pointer" @click="showPwds.confirm = !showPwds.confirm">
              <i :class="['fas', showPwds.confirm ? 'fa-eye-slash' : 'fa-eye']"></i>
            </button>
          </div>
          <p v-if="pwForm.confirmPassword && pwForm.newPassword !== pwForm.confirmPassword" class="text-xs font-bold text-[#ea4f52] mt-2">Mật khẩu không khớp.</p>
        </div>

        <!-- Error -->
        <Transition name="err">
          <div v-if="pwError" class="flex items-center gap-2 text-sm text-[#ea4f52] bg-[#ffe4e6] border border-[#fecdd3] rounded-xl px-4 py-3 font-medium">
            <i class="fas fa-exclamation-circle text-base"></i>
            {{ pwError }}
          </div>
        </Transition>

        <!-- Submit -->
        <div class="pt-2">
          <button
            class="h-11 px-6 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold transition-all flex items-center justify-center gap-2 shadow-sm hover:shadow-md hover:-translate-y-0.5"
            :disabled="pwSaving"
            :style="pwSaving ? 'opacity:0.7;cursor:not-allowed;transform:none' : ''"
            @click="changePassword"
          >
            <i v-if="pwSaving" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-save"></i>
            {{ pwSaving ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
          </button>
        </div>
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
