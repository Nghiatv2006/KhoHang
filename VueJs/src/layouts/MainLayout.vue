<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { draftStocktakeCount, refreshStocktakeBadge } from '../utils/stocktakeStore'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)
const showLogoutDialog = ref(false)

// ── Dark Mode Toggle ──────────────────────────────────────────
const isDark = ref(false)

function initTheme() {
  const saved = localStorage.getItem('wh_theme')
  if (saved === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark-mode')
  }
}

function toggleTheme() {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark-mode')
    localStorage.setItem('wh_theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark-mode')
    localStorage.setItem('wh_theme', 'light')
  }
}
// ──────────────────────────────────────────────────────────────

const user = ref<{
  fullName: string
  branchName: string
  roles: string[]
  role?: string
  branchId?: number
  branch?: any
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

// ── Stocktake draft badge (dùng chung store) ─────────────────
let stocktakeTimer: ReturnType<typeof setInterval>
// ─────────────────────────────────────────────────────────────

// ──────────────────────────────────────────────────────────────
// NOTIFICATION BADGES — Đếm phiếu cần xử lý
// ──────────────────────────────────────────────────────────────
const badgeImport = ref(0)
const badgeInvoice = ref(0)
const badgeTransfer = ref(0)
const badgeDisposal = ref(0)
const badgeReceiptStocktake = ref(0)

async function loadBadgeCounts() {
  try {
    const res = await api.get('/api/receipts')
    if (!res.ok) return
    const receipts: any[] = await res.json()
    const myBranchId = user.value?.branchId || user.value?.branch?.id

    const isManager = user.value?.role === 'MANAGER';
    const isAdmin = user.value?.role === 'ADMIN';
    const isStaff = user.value?.role === 'STAFF';

    // Nhập kho
    badgeImport.value = receipts.filter(r => {
      if (r.type !== 'IMPORT') return false;
      const isDest = myBranchId && Number(r.destBranchId) === Number(myBranchId);
      if (r.status === 'DRAFT') return (isManager && isDest) || isAdmin;
      if (r.status === 'PENDING_ADMIN') return isAdmin;
      if (r.status === 'PENDING_STOCKTAKE') return isStaff && isDest;
      if (r.status === 'PENDING_SHORTFALL_MANAGER') return isManager && isDest;
      if (r.status === 'PENDING_SHORTFALL_ADMIN') return isAdmin;
      
      const isSource = myBranchId && Number(r.sourceBranchId) === Number(myBranchId);
      if (r.status === 'PENDING_COMPENSATION') return (isManager && isSource) || isAdmin;
      return false;
    }).length

    // Hóa đơn
    badgeInvoice.value = receipts.filter(r => {
      if (r.type !== 'EXPORT') return false;
      const isSource = myBranchId && Number(r.sourceBranchId) === Number(myBranchId);
      if (r.status === 'DRAFT') return (isManager && isSource) || isAdmin;
      if (r.status === 'PENDING_ADMIN') return isAdmin;
      if (r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán')) {
        return isSource;
      }
      return false;
    }).length

    // Điều chuyển
    badgeTransfer.value = receipts.filter(r => {
      if (r.type !== 'TRANSFER') return false;
      const isSource = myBranchId && Number(r.sourceBranchId) === Number(myBranchId);
      const isDest = myBranchId && Number(r.destBranchId) === Number(myBranchId);
      if (r.status === 'DRAFT') return (isManager && isSource) || isAdmin;
      if (r.status === 'PENDING_ADMIN') return (isManager && isDest) || isAdmin;
      if (r.status === 'PENDING_STOCKTAKE') return isStaff && isDest;
      return false;
    }).length

    // Tiêu hủy
    badgeDisposal.value = receipts.filter(r => {
      if (r.type !== 'DISPOSAL') return false;
      const isSource = myBranchId && Number(r.sourceBranchId) === Number(myBranchId);
      if (r.status === 'DRAFT') return (isManager && isSource) || isAdmin;
      if (r.status === 'PENDING_ADMIN') return (isManager && isSource) || isAdmin;
      if (r.status === 'PENDING_STOCKTAKE') return isAdmin;
      return false;
    }).length

    // Kiểm kê nhận hàng
    badgeReceiptStocktake.value = receipts.filter(r => {
      if (r.status !== 'PENDING_STOCKTAKE') return false;
      if (isAdmin) return true;
      return myBranchId && Number(r.destBranchId) === Number(myBranchId);
    }).length
  } catch (e) {
    // silent fail
  }
}

let badgeTimer: ReturnType<typeof setInterval>

const mainNavItems = computed(() => {
  const items: { label: string; to: string; icon: string; badge?: number }[] = [
    { label: 'Tổng quan', to: '/dashboard', icon: 'fas fa-chart-pie' },
    { label: 'Nhập kho', to: '/imports', icon: 'fas fa-download', badge: badgeImport.value },
    { label: 'Hóa đơn', to: '/invoices', icon: 'fas fa-file-invoice-dollar', badge: badgeInvoice.value },
    { label: 'Điều chuyển', to: '/transfers', icon: 'fas fa-exchange-alt', badge: badgeTransfer.value }
  ]
  items.push({ label: 'Tiêu hủy', to: '/disposals', icon: 'fas fa-trash-alt', badge: badgeDisposal.value })
  if (hasCrudPermission.value) {
    items.push({ label: 'Sản phẩm', to: '/products', icon: 'fas fa-box-open' })
  }
  items.push({ label: 'Tồn kho', to: '/inventory', icon: 'fas fa-warehouse' })
  // Doanh thu — chỉ hiển thị với ADMIN và MANAGER
  if (isManagerOrAdmin.value) {
    items.push({ label: 'Doanh thu', to: '/revenue', icon: 'fas fa-chart-line' })
  }

  return items
})

// Kiểm kê kho được render riêng để gắn badge
const showStocktakeNav = computed(() => !!user.value)

const isManagerOrAdmin = computed(() => {
  if (!user.value) return false
  // @ts-ignore
  return ['ADMIN', 'MANAGER'].includes(user.value.role)
})

const totalStocktakeBadge = computed(() => {
  const periodicCount = draftStocktakeCount.value
  const receiptCount = badgeReceiptStocktake.value
  return periodicCount + receiptCount
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

// Refresh badge ngay khi user rời khỏi trang /stocktakes
watch(() => route.path, (newPath, oldPath) => {
  if (oldPath?.startsWith('/stocktakes') && !newPath?.startsWith('/stocktakes')) {
    refreshStocktakeBadge()
  }
})

onMounted(() => {
  initTheme()
  updateTime()
  timer = setInterval(updateTime, 1000)
  // Check ngay lúc mount; chỉ poll nếu là manager/admin
  if (isManagerOrAdmin.value) {
    refreshStocktakeBadge()
    stocktakeTimer = setInterval(refreshStocktakeBadge, 10_000)
  }
  // Load badge counts
  loadBadgeCounts()
  badgeTimer = setInterval(loadBadgeCounts, 3000) // Refresh mỗi 3 giây
})

function handleNavClick(to: string) {
  if (to === '/users' && route.path === '/users') {
    window.dispatchEvent(new CustomEvent('trigger-users-animation'))
  } else if (to === '/stocktakes' && route.path === '/stocktakes') {
    window.dispatchEvent(new CustomEvent('trigger-stocktakes-animation'))
  } else if (to === '/inventory' && route.path === '/inventory') {
    window.dispatchEvent(new CustomEvent('trigger-inventory-animation'))
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (stocktakeTimer) clearInterval(stocktakeTimer)
  if (badgeTimer) clearInterval(badgeTimer)
})
</script>

<template>
  <div data-section="layout-root" class="flex min-h-screen bg-[#f5f6fa] font-['Inter',sans-serif] text-[0.95rem] overflow-x-hidden text-[#364a63]">
    
    <!-- LEFT SIDEBAR -->
    <nav 
      data-section="sidebar"
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
            @click="handleNavClick(item.to)"
            class="relative flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group"
            :class="route.path.startsWith(item.to) && item.to !== '/' ? 'nav-active' : 'nav-idle'"
          >
            <i :class="item.icon" class="w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>{{ item.label }}</span>
            <!-- Notification Badge (Messenger/TikTok style) -->
            <span
              v-if="item.badge && item.badge > 0"
              class="absolute top-1/2 -translate-y-1/2 right-4 min-w-[20px] h-[20px] flex items-center justify-center px-1 text-[10px] font-extrabold text-white bg-[#ef476f] rounded-full shadow-[0_2px_8px_rgba(239,71,111,0.5)] animate-badge-pop"
            >
              {{ item.badge > 99 ? '99+' : item.badge }}
            </span>
          </RouterLink>

          <!-- Kiểm kê kho – render riêng để gắn badge -->
          <RouterLink
            v-if="showStocktakeNav"
            to="/stocktakes"
            @click="handleNavClick('/stocktakes')"
            class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group relative"
            :class="route.path.startsWith('/stocktakes') ? 'nav-active' : 'nav-idle'"
          >
            <i class="fas fa-clipboard-list w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span class="flex-1">Kiểm kê kho</span>
            <span
              v-if="totalStocktakeBadge > 0"
              class="stocktake-badge numeric"
              title="Có phiếu kiểm kê đang chờ xử lý"
            >
              {{ totalStocktakeBadge > 99 ? '99+' : totalStocktakeBadge }}
            </span>
          </RouterLink>

          <div class="text-[0.75rem] font-extrabold uppercase tracking-widest text-[#8094ae] px-7 mt-6 mb-2">Quản lý</div>
          <RouterLink
            v-for="item in adminNavItems"
            :key="item.to"
            :to="item.to"
            @click="handleNavClick(item.to)"
            class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group"
            :class="route.path.startsWith(item.to) ? 'nav-active' : 'nav-idle'"
          >
            <i :class="item.icon" class="w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>{{ item.label }}</span>
          </RouterLink>
        </div>

        <div>
          <hr data-section="sidebar-divider" class="mx-8 my-2 border-t border-black/10">
          <a @click="router.push('/profile')" data-section="profile-link" class="nav-idle flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 text-[#364a63] cursor-pointer group">
            <i class="fas fa-user-circle w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>Hồ sơ cá nhân</span>
          </a>
          <a @click="showLogoutDialog = true" data-section="logout-link" class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 text-[#ef476f] hover:bg-[#fff0f3] hover:text-[#d90429] hover:translate-x-1 cursor-pointer group">
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
      <div data-section="topbar" class="bg-white rounded-[16px] px-6 py-4 mb-8 flex justify-between items-center shadow-[0_2px_10px_rgba(0,0,0,0.02)]">
        <div class="flex items-center">
          <button @click="isCollapsed = !isCollapsed" data-section="menu-btn" class="text-[#364a63] hover:bg-[#f5f6fa] p-2 rounded-full transition-colors mr-4 text-xl">
            <i class="fas fa-bars"></i>
          </button>
          <div>
            <h4 class="text-[1.25rem] font-bold text-gray-900 m-0 leading-tight">Kho Hàng Trung Tâm</h4>
            <small class="text-[#8094ae]">{{ currentTime }}</small>
          </div>
        </div>
        
        <div class="flex items-center gap-4">
          <!-- Day/Night Theme Toggle -->
          <div class="theme-toggle-wrapper">
            <input type="checkbox" id="theme-toggle-checkbox" :checked="isDark" @change="toggleTheme">
            <label for="theme-toggle-checkbox" class="theme-toggle-label">
              <div class="theme-knob"></div>
              <div class="theme-stars">
                <div class="theme-star theme-star-1"></div>
                <div class="theme-star theme-star-2"></div>
                <div class="theme-star theme-star-3"></div>
                <div class="theme-star theme-star-4"></div>
              </div>
              <div class="theme-clouds">
                <div class="theme-cloud theme-cloud-1"></div>
                <div class="theme-cloud theme-cloud-2"></div>
                <div class="theme-cloud theme-cloud-3"></div>
              </div>
            </label>
          </div>

          <!-- User Pill -->
          <div data-section="user-pill" class="flex items-center bg-[#f8f9fa] px-4 py-2 rounded-full">
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
      <div data-section="logout-dialog" class="bg-white p-8 rounded-2xl w-full max-w-md shadow-[0_10px_25px_rgba(0,0,0,0.1)]">
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
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.animate-gradient-x {
  background-size: 200% auto;
  animation: gradient-x 3s linear infinite;
}

/* ── Nav item hover & active ─────────────────────────────── */
.nav-idle {
  color: #364a63;
  position: relative;
}
.nav-idle:hover {
  background: rgba(99, 102, 241, 0.1);
  color: #4361ee;
  transform: translateX(4px);
}

.nav-active {
  background: linear-gradient(135deg, #4361ee, #4cc9f0);
  color: white;
  box-shadow: 0 6px 18px rgba(67, 97, 238, 0.3);
  position: relative;
  overflow: hidden;
}
/* Shimmer lướt qua item đang active */
.nav-active::after {
  content: '';
  position: absolute;
  top: 0; left: -80%;
  width: 50%; height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.35), transparent);
  animation: shimmer 2s ease-in-out infinite;
  pointer-events: none;
}
@keyframes shimmer {
  0%   { left: -80%; }
  100% { left: 130%; }
}

/* Badge số lượng phiếu DRAFT */
.stocktake-badge.numeric {
  position: absolute;
  top: 50%;
  right: 16px;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f59e0b;
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  box-shadow: 0 2px 5px rgba(245,158,11,0.4);
  z-index: 10;
}

@keyframes badge-pop {
  0% { transform: scale(0); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}
.animate-badge-pop {
  animation: badge-pop 0.4s ease-out;
}

/* ── Day/Night Theme Toggle (Compact for Topbar) ─────────── */
.theme-toggle-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

#theme-toggle-checkbox {
  display: none;
}

.theme-toggle-label {
  width: 72px;
  height: 32px;
  background: #63bce4;
  border-radius: 100px;
  display: block;
  position: relative;
  cursor: pointer;
  box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.15), 0 4px 10px rgba(0, 0, 0, 0.08);
  transition: background 0.5s ease-in-out;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

/* Sun / Moon Knob */
.theme-knob {
  width: 24px;
  height: 24px;
  background: #ffdd42;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: all 0.5s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  box-shadow: 0 0 10px rgba(255, 221, 66, 0.8);
  z-index: 2;
}

/* Craters */
.theme-knob::before,
.theme-knob::after {
  content: '';
  position: absolute;
  background: #d6d6d6;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.4s;
}
.theme-knob::before {
  width: 5px;
  height: 5px;
  top: 6px;
  left: 5px;
}
.theme-knob::after {
  width: 7px;
  height: 7px;
  top: 11px;
  left: 13px;
}

/* Clouds */
.theme-clouds {
  position: absolute;
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
}
.theme-cloud {
  position: absolute;
  background: #fff;
  border-radius: 50%;
}
.theme-cloud-1 {
  width: 18px;
  height: 18px;
  top: 16px;
  left: 36px;
}
.theme-cloud-2 {
  width: 14px;
  height: 14px;
  top: 11px;
  left: 46px;
}
.theme-cloud-3 {
  width: 22px;
  height: 12px;
  top: 21px;
  left: 40px;
  border-radius: 10px;
}

/* Stars */
.theme-stars {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.5s ease;
}
.theme-star {
  position: absolute;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 0 3px #fff;
}
.theme-star-1 { width: 2px; height: 2px; top: 7px; left: 14px; }
.theme-star-2 { width: 2px; height: 2px; top: 18px; left: 24px; }
.theme-star-3 { width: 1px; height: 1px; top: 10px; left: 32px; }
.theme-star-4 { width: 2px; height: 2px; top: 22px; left: 10px; }

/* ── Checked State (Night Mode) ───────────────────────────── */
#theme-toggle-checkbox:checked + .theme-toggle-label {
  background: #1a1e2e;
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-knob {
  transform: translateX(40px) rotate(360deg);
  background: #fff;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-knob::before,
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-knob::after {
  opacity: 1;
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-clouds {
  transform: translateY(50px);
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-stars {
  opacity: 1;
  transform: translateY(0);
}

/* ── Star twinkle animation ───────────────────────────────── */
@keyframes twinkle {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-star-1 {
  animation: twinkle 1.5s ease-in-out infinite;
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-star-2 {
  animation: twinkle 2s ease-in-out 0.3s infinite;
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-star-3 {
  animation: twinkle 1.8s ease-in-out 0.6s infinite;
}
#theme-toggle-checkbox:checked + .theme-toggle-label .theme-star-4 {
  animation: twinkle 2.2s ease-in-out 0.9s infinite;
}
</style>

<style>
/* ═══════════════════════════════════════════════════════════════
   DARK MODE OVERRIDES — only active when html.dark-mode is set
   Uses data-section attributes for reliable targeting
   ═══════════════════════════════════════════════════════════════ */

/* Root layout wrapper */
html.dark-mode [data-section="layout-root"] {
  background: #0f172a !important;
  color: #e8ecf1 !important;
}

/* Sidebar */
html.dark-mode [data-section="sidebar"] {
  background: linear-gradient(to bottom, #1e293b, #0f172a) !important;
  box-shadow: 0 0 20px rgba(0,0,0,0.4) !important;
}

/* Sidebar section titles (MENU CHÍNH, QUẢN LÝ) */
html.dark-mode [data-section="sidebar"] .text-\[0\.75rem\] {
  color: #8ea4bc !important;
}

/* Sidebar nav idle items */
html.dark-mode .nav-idle {
  color: #e2e8f0 !important;
}
html.dark-mode .nav-idle:hover {
  background: rgba(99, 102, 241, 0.15) !important;
  color: #a5b4fc !important;
}

/* Profile link */
html.dark-mode [data-section="profile-link"] {
  color: #e2e8f0 !important;
}

/* Sidebar divider */
html.dark-mode [data-section="sidebar-divider"] {
  border-color: rgba(255, 255, 255, 0.1) !important;
}

/* Logout link */
html.dark-mode [data-section="logout-link"]:hover {
  background: rgba(239, 71, 111, 0.12) !important;
}

/* Topbar */
html.dark-mode [data-section="topbar"] {
  background: #1e293b !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.3) !important;
}
html.dark-mode [data-section="topbar"] h4 {
  color: #ffffff !important;
}
html.dark-mode [data-section="topbar"] small {
  color: #b8c5d4 !important;
}

/* Menu button */
html.dark-mode [data-section="menu-btn"] {
  color: #b8c5d4 !important;
}
html.dark-mode [data-section="menu-btn"]:hover {
  background: #334155 !important;
}

/* User pill */
html.dark-mode [data-section="user-pill"] {
  background: #334155 !important;
}
html.dark-mode [data-section="user-pill"] span {
  color: #f1f5f9 !important;
}

/* Logout dialog */
html.dark-mode [data-section="logout-dialog"] {
  background: #1e293b !important;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5) !important;
}
html.dark-mode [data-section="logout-dialog"] h3 {
  color: #ffffff !important;
}
html.dark-mode [data-section="logout-dialog"] p {
  color: #b8c5d4 !important;
}
html.dark-mode [data-section="logout-dialog"] button:first-child {
  background: #334155 !important;
  border-color: #475569 !important;
  color: #f1f5f9 !important;
}

/* Main content area */
html.dark-mode main {
  transition: background 0.4s ease;
}

/* Scrollbar */
html.dark-mode ::-webkit-scrollbar-thumb {
  background: #475569;
}
html.dark-mode ::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

/* ── Global text color overrides for dark mode ────────────────── */

/* All headings → near white */
html.dark-mode h1, html.dark-mode h2, html.dark-mode h3,
html.dark-mode h4, html.dark-mode h5, html.dark-mode h6 {
  color: #f8fafc !important;
}

/* Tailwind gray text classes → much brighter */
html.dark-mode .text-gray-900 {
  color: #f8fafc !important;
}
html.dark-mode .text-gray-800 {
  color: #f1f5f9 !important;
}
html.dark-mode .text-gray-700 {
  color: #e2e8f0 !important;
}
html.dark-mode .text-gray-600 {
  color: #cbd5e1 !important;
}
html.dark-mode .text-gray-500 {
  color: #b8c5d4 !important;
}
html.dark-mode .text-gray-400 {
  color: #94a3b8 !important;
}

/* Hardcoded text colors used throughout this app */
html.dark-mode .text-\[\#364a63\] {
  color: #e2e8f0 !important;
}
html.dark-mode .text-\[\#8094ae\] {
  color: #a3b8cc !important;
}
html.dark-mode .text-\[\#526484\] {
  color: #b8c5d4 !important;
}

/* ── Global background / border overrides ─────────────────────── */
html.dark-mode .bg-white {
  background: #1e293b !important;
}
html.dark-mode .bg-\[\#f5f6fa\] {
  background: #0f172a !important;
}
html.dark-mode .bg-\[\#f8f9fa\] {
  background: #334155 !important;
}
html.dark-mode .border-gray-200,
html.dark-mode .border-gray-300 {
  border-color: #334155 !important;
}
html.dark-mode .border-\[\#e2e8f0\] {
  border-color: #475569 !important;
}
html.dark-mode .bg-gray-50,
html.dark-mode .bg-gray-100 {
  background: #0f172a !important;
}

/* ── Tables ───────────────────────────────────────────────────── */
html.dark-mode table {
  color: #e2e8f0 !important;
}
html.dark-mode table thead,
html.dark-mode .bg-slate-200 {
  background: #1a2332 !important;
}
html.dark-mode table thead th {
  color: #b8c5d4 !important;
}
html.dark-mode table tbody tr {
  border-color: #1e293b !important;
}
html.dark-mode .even\:bg-slate-50\/60:nth-child(even) {
  background: rgba(255, 255, 255, 0.02) !important;
}
html.dark-mode table tbody tr:hover {
  background: #253449 !important;
}
html.dark-mode table tbody td {
  color: #e2e8f0 !important;
}

/* ── Form elements ────────────────────────────────────────────── */
html.dark-mode input,
html.dark-mode select,
html.dark-mode textarea {
  background: #0f172a !important;
  color: #f1f5f9 !important;
  border-color: #334155 !important;
}
html.dark-mode input::placeholder,
html.dark-mode textarea::placeholder {
  color: #8ea4bc !important;
}

/* ── Clean card dark mode ─────────────────────────────────────── */
html.dark-mode .clean-card {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark-mode .clean-card:hover {
  border-color: #475569 !important;
}

/* ── Misc text overrides ──────────────────────────────────────── */
html.dark-mode .text-black {
  color: #f1f5f9 !important;
}

/* ── Dashboard Specific Overrides ────────────────────────────── */
/* Header background of Trend charts (originally bg-[#f8f9fa]/50) */
html.dark-mode .bg-\[\#f8f9fa\]\/50 {
  background: rgba(255, 255, 255, 0.03) !important;
  border-color: #334155 !important;
}

/* Global dashboard card borders */
html.dark-mode .border-\[\#f1f5f9\] {
  border-color: #334155 !important;
}

/* Branch & Category Sales Cards (violet-50 and emerald-50) */
html.dark-mode .bg-violet-50,
html.dark-mode .bg-emerald-50 {
  background: #1e293b !important;
}

/* Top 5 Products Card (from-[#f8fafc] to-[#f1f5f9]) */
html.dark-mode .from-\[\#f8fafc\] {
  --tw-gradient-from: #1e293b !important;
}
html.dark-mode .to-\[\#f1f5f9\] {
  --tw-gradient-to: #1e293b !important;
}

/* Top 5 card header & decoration & body */
html.dark-mode .bg-white\/60,
html.dark-mode .bg-white\/40 {
  background: rgba(255, 255, 255, 0.03) !important;
}
html.dark-mode .border-indigo-50\/50 {
  border-color: #334155 !important;
}
html.dark-mode .bg-indigo-100 {
  background: #334155 !important;
}

/* Text slate classes */
html.dark-mode .text-slate-800 { color: #f8fafc !important; }
html.dark-mode .text-slate-700 { color: #e2e8f0 !important; }
html.dark-mode .text-slate-600 { color: #cbd5e1 !important; }
html.dark-mode .text-slate-500 { color: #b8c5d4 !important; }

/* ── Container Backgrounds for Views ──────────────────────── */
html.dark-mode .bg-indigo-50,
html.dark-mode .bg-sky-50,
html.dark-mode .bg-sky-50\/50,
html.dark-mode .bg-indigo-50\/50,
html.dark-mode .bg-\[\#f8fafc\],
html.dark-mode .bg-\[\#f8f9fa\],
html.dark-mode .bg-\[\#f8f9fa\]\/50,
html.dark-mode .bg-\[\#f1f5f9\],
html.dark-mode .bg-white\/60,
html.dark-mode .bg-slate-50,
html.dark-mode .bg-\[\#eef2ff\] {
  background: #1a2332 !important;
  border-color: #334155 !important;
}

/* Warning blocks */
html.dark-mode .bg-\[\#fff8e6\],
html.dark-mode .warning-banner {
  background: rgba(245, 158, 11, 0.1) !important;
  border-color: rgba(245, 158, 11, 0.2) !important;
}

/* ── Backup View Custom Cards ────────────────────────────── */
html.dark-mode .card--backup,
html.dark-mode .card--restore,
html.dark-mode .card--history,
html.dark-mode .card--system-export,
html.dark-mode .card--system-import,
html.dark-mode .card--system-history,
html.dark-mode .history-col,
html.dark-mode .sys-section {
  background: #1e293b !important;
  border-color: #334155 !important;
}

html.dark-mode .btn--ghost {
  background: rgba(59, 130, 246, 0.1) !important;
  color: #60a5fa !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
  box-shadow: none !important;
}

html.dark-mode .btn--ghost:hover {
  background: rgba(59, 130, 246, 0.2) !important;
  color: #93c5fd !important;
  border-color: rgba(59, 130, 246, 0.5) !important;
}

html.dark-mode .btn--cancel {
  background: #1e293b !important;
  color: #e2e8f0 !important;
  border-color: #334155 !important;
}

html.dark-mode .btn--cancel:hover {
  background: #334155 !important;
}

html.dark-mode .history-footer {
  background: rgba(255, 255, 255, 0.03) !important;
  border-top-color: #334155 !important;
}

html.dark-mode .history-item {
  background: rgba(255, 255, 255, 0.05) !important;
  border-bottom-color: #334155 !important;
}

html.dark-mode .history-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

html.dark-mode .empty-state__icon {
  background: rgba(255, 255, 255, 0.1) !important;
}

html.dark-mode .skeleton-row {
  background: rgba(255, 255, 255, 0.05) !important;
}

html.dark-mode .dropzone {
  background: rgba(255, 255, 255, 0.03) !important;
  border-color: #475569 !important;
}

html.dark-mode .dropzone:hover {
  background: rgba(16, 185, 129, 0.1) !important;
}

html.dark-mode .dropzone--active,
html.dark-mode .dropzone--filled {
  background: rgba(16, 185, 129, 0.15) !important;
  border-color: #10b981 !important;
}

html.dark-mode .page-header__title,
html.dark-mode .card__title,
html.dark-mode .dropzone__filename,
html.dark-mode .history-item__filename {
  color: #f8fafc !important;
}

html.dark-mode .dropzone__label,
html.dark-mode .empty-state__title {
  color: #f1f5f9 !important;
}

html.dark-mode .card__desc,
html.dark-mode .dropzone__filesize,
html.dark-mode .history-item__meta,
html.dark-mode .history-footer {
  color: #cbd5e1 !important;
}

html.dark-mode .page-header__sub,
html.dark-mode .card__subtitle,
html.dark-mode .empty-state__sub,
html.dark-mode .dropzone__hint,
html.dark-mode .history-item__meta i {
  color: #94a3b8 !important;
}

html.dark-mode .warning-banner {
  color: #fbbf24 !important;
}

html.dark-mode .warning-banner strong {
  color: #fef3c7 !important;
}

html.dark-mode .card__glow {
  display: none !important;
}

/* ── Badges, Icon Backgrounds, and Rows ──────────────────────── */
html.dark-mode .bg-green-50,
html.dark-mode .bg-emerald-50,
html.dark-mode .bg-emerald-50\/50 {
  background: rgba(16, 185, 129, 0.15) !important;
  border-color: rgba(16, 185, 129, 0.2) !important;
}
html.dark-mode .bg-purple-50,
html.dark-mode .bg-purple-50\/50 {
  background: rgba(168, 85, 247, 0.15) !important;
  border-color: rgba(168, 85, 247, 0.2) !important;
}
html.dark-mode .bg-red-50,
html.dark-mode .bg-rose-50,
html.dark-mode .bg-rose-50\/50,
html.dark-mode .bg-rose-50\/40 {
  background: rgba(244, 63, 94, 0.15) !important;
  border-color: rgba(244, 63, 94, 0.2) !important;
}
html.dark-mode .bg-sky-50,
html.dark-mode .bg-blue-50 {
  background: rgba(14, 165, 233, 0.15) !important;
  border-color: rgba(14, 165, 233, 0.2) !important;
}
html.dark-mode .bg-yellow-50,
html.dark-mode .bg-amber-50 {
  background: rgba(245, 158, 11, 0.15) !important;
  border-color: rgba(245, 158, 11, 0.2) !important;
}
html.dark-mode .bg-slate-100 {
  background: rgba(255, 255, 255, 0.05) !important;
  border-color: #334155 !important;
}
html.dark-mode .bg-slate-200 {
  background: #1e293b !important;
  color: #e2e8f0 !important;
}

/* Tint Text Colors for Contrast in Dark Mode */
html.dark-mode .text-green-600, html.dark-mode .text-emerald-600, html.dark-mode .text-emerald-700, html.dark-mode .text-emerald-800 { color: #34d399 !important; }
html.dark-mode .text-purple-600, html.dark-mode .text-purple-700, html.dark-mode .text-purple-800 { color: #c084fc !important; }
html.dark-mode .text-red-600, html.dark-mode .text-rose-600, html.dark-mode .text-red-500, html.dark-mode .text-rose-500, html.dark-mode .text-rose-700, html.dark-mode .text-rose-800 { color: #fb7185 !important; }
html.dark-mode .text-sky-600, html.dark-mode .text-blue-600, html.dark-mode .text-sky-700, html.dark-mode .text-blue-700, html.dark-mode .text-sky-800, html.dark-mode .text-blue-800 { color: #38bdf8 !important; }
html.dark-mode .text-yellow-600, html.dark-mode .text-amber-600, html.dark-mode .text-yellow-500, html.dark-mode .text-amber-500, html.dark-mode .text-amber-700, html.dark-mode .text-amber-800 { color: #fbbf24 !important; }

/* Table Row Hovers & Alternating Rows */
html.dark-mode table tbody tr {
  background-color: #1a2332 !important;
}

html.dark-mode table tbody tr.even\:bg-slate-50\/60:nth-child(even),
html.dark-mode .even\:bg-slate-50\/60:nth-child(even) {
  background-color: #1e293b !important;
}

html.dark-mode .hover\:bg-\[\#f8f9fa\]:hover,
html.dark-mode .hover\:bg-\[\#f8f9fa\]\/50:hover,
html.dark-mode .hover\:bg-\[\#f8f9fa\]\/80:hover {
  background-color: #253449 !important;
}

html.dark-mode .hover\:bg-rose-100\/50:hover,
html.dark-mode .hover\:bg-rose-100\/40:hover {
  background-color: rgba(244, 63, 94, 0.2) !important;
}

/* ── Checkboxes ──────────────────────────────────────────────────────── */
html.dark-mode input[type="checkbox"] {
  color-scheme: dark;
}

/* ── Backup & Restore Actions ────────────────────────────────────────── */
html.dark-mode .badge-auto {
  background: rgba(99, 102, 241, 0.2) !important;
  color: #818cf8 !important;
  border-color: rgba(99, 102, 241, 0.4) !important;
}

html.dark-mode .badge-manual {
  background: rgba(16, 185, 129, 0.2) !important;
  color: #34d399 !important;
  border-color: rgba(16, 185, 129, 0.4) !important;
}

html.dark-mode .action-btn--restore {
  background: rgba(67, 97, 238, 0.2) !important;
  color: #60a5fa !important;
  border-color: rgba(67, 97, 238, 0.4) !important;
}

html.dark-mode .action-btn--delete {
  background: rgba(239, 68, 68, 0.15) !important;
  color: #f87171 !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
}
</style>

