<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterView, RouterLink, useRoute, useRouter } from 'vue-router'
import AppToast from '../components/AppToast.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { api } from '../api'

const route = useRoute()
const router = useRouter()

const user = ref<any>(null)
const showLogoutDialog = ref(false)
const sidebarCollapsed = ref(false)

onMounted(async () => {
  const stored = localStorage.getItem('wh_user')
  if (stored) user.value = JSON.parse(stored)
  // Sync from server
  try {
    const res = await api.get('/api/users/me')
    if (res.ok) {
      const data = await res.json()
      user.value = data
      localStorage.setItem('wh_user', JSON.stringify(data))
    }
  } catch {}
})

// Lắng nghe auth-failed
const handleAuthFailed = () => router.push('/login')
onMounted(() => window.addEventListener('auth-failed', handleAuthFailed))
onUnmounted(() => window.removeEventListener('auth-failed', handleAuthFailed))

const role = computed(() => user.value?.role || '')
const isAdmin = computed(() => role.value === 'ADMIN')
const isManager = computed(() => role.value === 'MANAGER' || isAdmin.value)

const navItems = computed(() => {
  const items = [
    { to: '/dashboard', icon: 'dashboard', label: 'Tổng quan' },
    { to: '/products', icon: 'inventory_2', label: 'Sản phẩm' },
    { to: '/partners', icon: 'handshake', label: 'Đối tác' },
    ...(isAdmin.value ? [{ to: '/branches', icon: 'store', label: 'Chi nhánh' }] : []),
    ...(isManager.value ? [{ to: '/users', icon: 'group', label: 'Nhân viên' }] : []),
  ]
  return items
})

const pageTitles: Record<string, string> = {
  '/dashboard': 'Tổng quan',
  '/products': 'Sản phẩm & Danh mục',
  '/partners': 'Đối tác',
  '/branches': 'Chi nhánh',
  '/users': 'Quản lý Nhân viên',
  '/profile': 'Hồ sơ cá nhân',
}
const pageTitle = computed(() => pageTitles[route.path] || 'Quản lý Kho')

async function doLogout() {
  await api.post('/api/auth/logout', {})
  localStorage.removeItem('wh_user')
  router.push('/login')
}

const roleLabel: Record<string, string> = { ADMIN: 'Quản trị viên', MANAGER: 'Quản lý', STAFF: 'Nhân viên' }
</script>

<template>
  <div class="flex h-screen bg-[#f8fafc] overflow-hidden">
    <!-- Sidebar -->
    <aside
      :class="['flex flex-col flex-shrink-0 transition-all duration-300 relative', sidebarCollapsed ? 'w-16' : 'w-64']"
      style="background: #0f172a;"
    >
      <!-- Logo -->
      <div class="flex items-center gap-3 px-4 h-16 border-b border-white/10 flex-shrink-0 relative overflow-hidden">
        <div class="absolute inset-0 bg-gradient-to-r from-blue-500/10 to-transparent pointer-events-none"></div>

        <div class="relative w-8 h-8 rounded-lg bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center flex-shrink-0 shadow-[0_0_12px_rgba(37,99,235,0.4)] border border-white/10">
          <span class="material-symbols-outlined text-white text-[18px]" style="font-variation-settings: 'FILL' 1">inventory_2</span>
        </div>
        
        <Transition name="fade-text">
          <div v-if="!sidebarCollapsed" class="overflow-hidden">
            <div class="text-white font-bold text-[13px] leading-tight whitespace-nowrap tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-200">Warehouse_Management</div>
            <div class="text-[#38bdf8] text-[10px] whitespace-nowrap font-bold tracking-[0.15em] uppercase mt-0.5">Đa Chi Nhánh</div>
          </div>
        </Transition>
      </div>

      <!-- Nav -->
      <nav class="flex-1 py-4 overflow-y-auto">
        <div class="px-2 space-y-0.5">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            :class="[
              'flex items-center gap-3 rounded-lg transition-all duration-150 group',
              sidebarCollapsed ? 'px-2 py-3 justify-center' : 'px-3 py-2.5',
              route.path === item.to
                ? 'bg-[#0052cc] text-white shadow-md'
                : 'text-slate-400 hover:bg-white/5 hover:text-white'
            ]"
            :title="sidebarCollapsed ? item.label : ''"
          >
            <span
              class="material-symbols-outlined text-xl flex-shrink-0"
              :style="route.path === item.to ? 'font-variation-settings: \'FILL\' 1, \'wght\' 400, \'GRAD\' 0, \'opsz\' 24' : ''"
            >{{ item.icon }}</span>
            <Transition name="fade-text">
              <span v-if="!sidebarCollapsed" class="text-sm font-medium whitespace-nowrap overflow-hidden">{{ item.label }}</span>
            </Transition>
          </RouterLink>
        </div>
      </nav>

      <!-- User + actions -->
      <div class="border-t border-white/5 p-3 flex-shrink-0">
        <!-- Profile link -->
        <RouterLink
          to="/profile"
          :class="[
            'flex items-center gap-3 rounded-lg p-2 transition-colors mb-1',
            route.path === '/profile' ? 'bg-white/10' : 'hover:bg-white/5'
          ]"
        >
          <div class="w-8 h-8 rounded-full bg-[#0052cc] flex items-center justify-center flex-shrink-0 text-white text-sm font-bold">
            {{ user?.fullName?.charAt(0) || '?' }}
          </div>
          <Transition name="fade-text">
            <div v-if="!sidebarCollapsed" class="overflow-hidden min-w-0">
              <div class="text-white text-xs font-semibold truncate">{{ user?.fullName || '...' }}</div>
              <div class="text-slate-400 text-xs truncate">{{ roleLabel[role] || role }}</div>
            </div>
          </Transition>
        </RouterLink>

        <!-- Logout -->
        <button
          :class="['w-full flex items-center gap-3 rounded-lg p-2 text-slate-400 hover:text-red-400 hover:bg-red-500/5 transition-colors', sidebarCollapsed ? 'justify-center' : '']"
          :title="sidebarCollapsed ? 'Đăng xuất' : ''"
          @click="showLogoutDialog = true"
        >
          <span class="material-symbols-outlined text-xl flex-shrink-0">logout</span>
          <Transition name="fade-text">
            <span v-if="!sidebarCollapsed" class="text-sm whitespace-nowrap">Đăng xuất</span>
          </Transition>
        </button>
      </div>

      <!-- Collapse toggle -->
      <button
        class="absolute -right-3 top-20 w-6 h-6 bg-white border border-slate-200 rounded-full flex items-center justify-center shadow-sm hover:shadow-md transition-shadow z-10"
        @click="sidebarCollapsed = !sidebarCollapsed"
      >
        <span class="material-symbols-outlined text-slate-400 text-sm">
          {{ sidebarCollapsed ? 'chevron_right' : 'chevron_left' }}
        </span>
      </button>
    </aside>

    <!-- Main -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- Top Header -->
      <header class="h-16 bg-white border-b border-slate-100 flex items-center px-6 gap-4 flex-shrink-0 shadow-sm">
        <h1 class="text-lg font-semibold text-slate-800 flex-1">{{ pageTitle }}</h1>
        <div class="flex items-center gap-2 text-xs text-slate-400">
          <span class="material-symbols-outlined text-base">business</span>
          <span>{{ user?.branchName || 'Tất cả chi nhánh' }}</span>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-y-auto p-6">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>

    <!-- Toast & Dialogs -->
    <AppToast />
    <ConfirmDialog
      :show="showLogoutDialog"
      title="Đăng xuất"
      message="Bạn có chắc muốn đăng xuất khỏi hệ thống không?"
      confirm-text="Đăng xuất"
      :danger="true"
      @confirm="doLogout"
      @cancel="showLogoutDialog = false"
    />
  </div>
</template>

<style scoped>
.fade-text-enter-active, .fade-text-leave-active { transition: opacity 0.15s, width 0.2s; }
.fade-text-enter-from, .fade-text-leave-to { opacity: 0; }
.page-enter-active, .page-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.page-enter-from { opacity: 0; transform: translateY(6px); }
.page-leave-to { opacity: 0; }
</style>
