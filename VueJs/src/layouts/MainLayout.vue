<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)
const showLogoutDialog = ref(false)

const user = ref<{
  fullName: string
  branchName: string
  roles: string[]
} | null>(null)

onMounted(() => {
  const uStr = localStorage.getItem('wh_user')
  if (uStr) {
    try {
      user.value = JSON.parse(uStr)
    } catch { }
  }
})

const hasCrudPermission = computed(() => {
  if (!user.value) return false
  // @ts-ignore
  return user.value.role === 'ADMIN'
})

const mainNavItems = computed(() => {
  const items = [
    { label: 'Tổng quan', to: '/dashboard', icon: 'fas fa-chart-pie' },
    { label: 'Phiếu Nhập', to: '/receipts', icon: 'fas fa-file-invoice' }
  ]
  if (hasCrudPermission.value) {
    items.push({ label: 'Sản phẩm', to: '/products', icon: 'fas fa-box-open' })
  }
  items.push({ label: 'Tồn kho', to: '/inventory', icon: 'fas fa-warehouse' })
  // @ts-ignore
  if (user.value) {
    items.push({ label: 'Kiểm kê kho', to: '/stocktakes', icon: 'fas fa-clipboard-list' })
  }
  return items
})

const isManagerOrAdmin = computed(() => {
  if (!user.value) return false
  // @ts-ignore
  return ['ADMIN', 'MANAGER'].includes(user.value.role)
})

const adminNavItems = computed(() => {
  const items: { label: string; to: string; icon: string }[] = []
  // @ts-ignore
  if (!user.value || user.value.role !== 'ADMIN') {
    items.push({ label: 'Đối tác', to: '/partners', icon: 'fas fa-handshake' })
  }
  if (isManagerOrAdmin.value) {
    items.push({ label: 'Nhân viên', to: '/users', icon: 'fas fa-users' })
  }
  items.push({ label: 'Chi nhánh', to: '/branches', icon: 'fas fa-building' })
  if (isManagerOrAdmin.value) {
    items.push(
      { label: 'Sao lưu & Phục hồi', to: '/backup-restore', icon: 'fas fa-database' },
      { label: 'Nhật ký hoạt động', to: '/audit-logs', icon: 'fas fa-history' }
    )
  }
  return items
})



async function logout() {
  try {
    await api.post('/api/auth/logout', {})
  } catch (e) {
    console.error('Logout API failed', e)
  }
  localStorage.removeItem('wh_user')
  showLogoutDialog.value = false
  router.push('/login')
}

const currentTime = ref('')
let timer: ReturnType<typeof setInterval>

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="flex min-h-screen bg-[#f5f6fa] font-['Inter',sans-serif] text-[0.95rem] overflow-x-hidden text-[#364a63]">
    
    <!-- LEFT SIDEBAR -->
    <nav 
      class="fixed top-0 left-0 h-screen z-50 flex flex-col bg-gradient-to-b from-[#eef2ff] to-[#e0e7ff] transition-all duration-300 ease-in-out"
      :class="isCollapsed ? 'w-[80px] -ml-[80px]' : 'w-[280px]'"
      style="box-shadow: 0 0 15px rgba(0,0,0,0.05);"
    >
      <div class="flex flex-col justify-between h-full overflow-y-auto pb-4">
        <div>
          <!-- Sidebar Header -->
          <div class="flex items-center h-[90px] px-7 mb-4">
            <i class="fas fa-boxes text-[#4361ee] text-3xl mr-3"></i>
            <span class="text-2xl font-extrabold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-[#4361ee] via-[#f72585] to-[#4cc9f0] animate-gradient-x">
              WAREHUB
            </span>
          </div>

          <div class="text-[0.75rem] font-extrabold uppercase tracking-widest text-[#8094ae] px-7 mt-6 mb-2">Menu Chính</div>
          <RouterLink
            v-for="item in mainNavItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group"
            :class="route.path.startsWith(item.to) && item.to !== '/' ? 'bg-gradient-to-br from-[#4361ee] to-[#4cc9f0] text-white shadow-[0_6px_15px_rgba(67,97,238,0.35)]' : 'text-[#364a63] hover:translate-x-1 hover:shadow-[-4px_4px_10px_rgba(67,97,238,0.05)] hover:text-[#4361ee]'"
          >
            <i :class="item.icon" class="w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>{{ item.label }}</span>
          </RouterLink>

          <div class="text-[0.75rem] font-extrabold uppercase tracking-widest text-[#8094ae] px-7 mt-6 mb-2">Quản lý</div>
          <RouterLink
            v-for="item in adminNavItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group"
            :class="route.path.startsWith(item.to) ? 'bg-gradient-to-br from-[#4361ee] to-[#4cc9f0] text-white shadow-[0_6px_15px_rgba(67,97,238,0.35)]' : 'text-[#364a63] hover:translate-x-1 hover:shadow-[-4px_4px_10px_rgba(67,97,238,0.05)] hover:text-[#4361ee]'"
          >
            <i :class="item.icon" class="w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>{{ item.label }}</span>
          </RouterLink>
        </div>

        <div>
          <hr class="mx-8 my-2 border-t border-black/10">
          <a @click="router.push('/profile')" class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 text-[#364a63] hover:translate-x-1 hover:shadow-[-4px_4px_10px_rgba(67,97,238,0.05)] hover:text-[#4361ee] cursor-pointer group">
            <i class="fas fa-user-circle w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i> 
            <span>Hồ sơ cá nhân</span>
          </a>
          <a @click="showLogoutDialog = true" class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 text-[#ef476f] hover:bg-[#fff0f3] hover:text-[#d90429] hover:translate-x-1 cursor-pointer group">
            <i class="fas fa-sign-out-alt w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i> 
            <span>Đăng xuất</span>
          </a>
        </div>
      </div>
    </nav>

    <!-- MAIN CONTENT -->
    <main 
      class="flex-1 flex flex-col transition-all duration-300 ease-in-out p-8"
      :class="isCollapsed ? 'ml-0' : 'ml-[280px]'"
    >
      <!-- Topbar -->
      <div class="bg-white rounded-[16px] px-6 py-4 mb-8 flex justify-between items-center shadow-[0_2px_10px_rgba(0,0,0,0.02)]">
        <div class="flex items-center">
          <button @click="isCollapsed = !isCollapsed" class="text-[#364a63] hover:bg-[#f5f6fa] p-2 rounded-full transition-colors mr-4 text-xl">
            <i class="fas fa-bars"></i>
          </button>
          <div>
            <h4 class="text-[1.25rem] font-bold text-gray-900 m-0 leading-tight">Kho Hàng Trung Tâm</h4>
            <small class="text-[#8094ae]">{{ currentTime }}</small>
          </div>
        </div>
        
        <div class="flex items-center gap-4">
          <!-- User Pill -->
          <div class="flex items-center bg-[#f8f9fa] px-4 py-2 rounded-full">
            <div class="w-8 h-8 rounded-full bg-[#4361ee] text-white flex items-center justify-center font-bold">
              {{ user?.fullName?.charAt(0) || 'U' }}
            </div>
            <span class="ml-2 font-bold text-[0.875rem] text-[#364a63]">
              {{ user?.fullName || 'Admin' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Page Content -->
      <div class="flex-1 min-w-0">
        <RouterView />
      </div>
    </main>

    <!-- Logout Dialog -->
    <div v-if="showLogoutDialog" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[2000] flex items-center justify-center">
      <div class="bg-white p-8 rounded-2xl w-full max-w-md shadow-[0_10px_25px_rgba(0,0,0,0.1)]">
        <h3 class="font-bold text-xl mb-2 text-gray-900">Xác nhận đăng xuất</h3>
        <p class="text-[#8094ae] mb-6">Bạn có chắc chắn muốn thoát phiên làm việc?</p>
        <div class="flex gap-4 justify-end">
          <button @click="showLogoutDialog = false" class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63]">Hủy</button>
          <button @click="logout" class="px-5 py-2.5 bg-[#ea4f52] text-white border-none rounded-xl font-semibold">Đăng xuất</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes gradient-x {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}
.animate-gradient-x {
  background-size: 200% auto;
  animation: gradient-x 3s linear infinite;
}
</style>
