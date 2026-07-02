<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { draftStocktakeCount, refreshStocktakeBadge } from '../utils/stocktakeStore'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)
const showLogoutDialog = ref(false)

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
      if (r.type !== 'ADJUST_OUT') return false;
      const isSource = myBranchId && Number(r.sourceBranchId) === Number(myBranchId);
      if (r.status === 'DRAFT') return (isManager && isSource) || isAdmin;
      if (r.status === 'PENDING_ADMIN') return (isManager && isSource) || isAdmin;
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
  // @ts-ignore
  if (!user.value || user.value.role !== 'ADMIN') {
    items.push({ label: 'Tiêu hủy', to: '/disposals', icon: 'fas fa-trash-alt', badge: badgeDisposal.value })
  }
  if (hasCrudPermission.value) {
    items.push({ label: 'Sản phẩm', to: '/products', icon: 'fas fa-box-open' })
  }
  items.push({ label: 'Tồn kho', to: '/inventory', icon: 'fas fa-warehouse' })
  // @ts-ignore
  if (!user.value || user.value.role !== 'ADMIN') {
    items.push({ label: 'Tồn kho HT', to: '/global-inventory', icon: 'fas fa-globe' })
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

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (stocktakeTimer) clearInterval(stocktakeTimer)
  if (badgeTimer) clearInterval(badgeTimer)
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
            class="relative flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group"
            :class="route.path.startsWith(item.to) && item.to !== '/' ? 'nav-active' : 'nav-idle'"
          >
            <i :class="item.icon" class="w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>{{ item.label }}</span>
            <!-- Notification Badge (Messenger/TikTok style) -->
            <span
              v-if="item.badge && item.badge > 0"
              class="absolute top-1.5 right-2 min-w-[20px] h-[20px] flex items-center justify-center px-1 text-[10px] font-extrabold text-white bg-[#ef476f] rounded-full shadow-[0_2px_8px_rgba(239,71,111,0.5)] animate-badge-pop"
            >
              {{ item.badge > 99 ? '99+' : item.badge }}
            </span>
          </RouterLink>

          <!-- Kiểm kê kho – render riêng để gắn badge -->
          <RouterLink
            v-if="showStocktakeNav"
            to="/stocktakes"
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
            class="flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 group"
            :class="route.path.startsWith(item.to) ? 'nav-active' : 'nav-idle'"
          >
            <i :class="item.icon" class="w-6 text-[1.2rem] mr-3 text-center transition-transform duration-300 group-hover:scale-110 group-hover:rotate-6"></i>
            <span>{{ item.label }}</span>
          </RouterLink>
        </div>

        <div>
          <hr class="mx-8 my-2 border-t border-black/10">
          <a @click="router.push('/profile')" class="nav-idle flex items-center font-semibold px-6 py-[0.9rem] mx-4 my-1 rounded-xl transition-all duration-300 text-[#364a63] cursor-pointer group">
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
  top: 6px;
  right: 12px;
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
</style>
