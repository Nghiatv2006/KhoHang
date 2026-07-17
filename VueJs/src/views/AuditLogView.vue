<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'

const toast = useToast()

// ─── State ────────────────────────────────────────────────────────────────────
const logs = ref<any[]>([])
const loading = ref(true)
const initialLoading = ref(true)
const users = ref<any[]>([])

const todayStr = new Date().toISOString().substring(0, 10)
const filterKeyword = ref('')
const filterUserId = ref<number | ''>('')
const filterAction = ref('')
const filterFrom = ref(todayStr)
const filterTo = ref(todayStr)
const filterTimeRange = ref('today')

// Pagination state
const currentPage = ref(0)
const pageSize = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const showJumpInput = ref<'none' | 'left' | 'right'>('none')
const jumpPageNumber = ref<number | ''>('')

const vFocus = {
  mounted: (el: HTMLElement) => el.focus()
}

// ─── Action labels ────────────────────────────────────────────────────────────
const ACTION_LABELS: Record<string, string> = {
  'LOGIN':         'Đăng nhập',
  'LOGOUT':        'Đăng xuất',
  'CREATE':        'Thêm mới',
  'CREATE_RECEIPT':'Tạo phiếu',
  'CREATE_DISPOSAL_RECEIPT':'Tạo phiếu tiêu hủy',
  'UPDATE':        'Cập nhật',
  'DELETE':        'Xóa',
  'RESTORE':       'Khôi phục',
  'APPROVE':       'Duyệt phiếu',
  'CANCEL':        'Hủy phiếu',
  'SPAM_WARNING':  '⚠️ Cảnh báo SPAM',
  'LOCK_ACCOUNT':  '🔒 Khóa tài khoản',
  'CREATE_STOCKTAKE': 'Tạo kiểm kê',
  'UPDATE_STOCKTAKE': 'Cập nhật số liệu',
  'COMPLETE_STOCKTAKE': 'Chốt kiểm kê',
  'CANCEL_STOCKTAKE': 'Hủy kiểm kê',
  'MARK_PAID': 'Xác nhận thanh toán',
  'CONFIRM_STOCKTAKE': 'Xác nhận kiểm kê',
  'APPROVE_SHORTFALL': 'Duyệt bù hao hụt',
  'REJECT_SHORTFALL': 'Từ chối bù hao hụt',
  'COMPENSATE_SHORTFALL': 'Tạo phiếu bù',
  'CONFIRM_TRANSFER': 'Nhận điều chuyển',
  'IMPORT_EXCEL': 'Nhập từ Excel',
  'DỌN DẸP': 'Dọn dẹp hệ thống',
}

const ACTION_OPTIONS = Object.entries(ACTION_LABELS).map(([value, label]) => ({ value, label }))

// ─── Computed ─────────────────────────────────────────────────────────────────
const filteredLogs = computed(() => {
  return logs.value
})

const paginationItems = computed(() => {
  const current = currentPage.value + 1
  const total = Math.max(1, totalPages.value)
  const items: (number | string)[] = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) items.push(i)
  } else {
    if (current <= 3) {
      items.push(1, 2, 3, 4, '...right', total)
    } else if (current >= total - 2) {
      items.push(1, '...left', total - 3, total - 2, total - 1, total)
    } else {
      items.push(1, '...left', current - 1, current, current + 1, '...right', total)
    }
  }
  return items
})

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page - 1
  }
}

function openJumpInput(side: 'left' | 'right') {
  showJumpInput.value = side
  jumpPageNumber.value = ''
}

function handleJump() {
  if (!showJumpInput.value || showJumpInput.value === 'none') return;
  const p = Number(jumpPageNumber.value)
  if (p && p >= 1 && p <= totalPages.value) {
    currentPage.value = p - 1
  } else if (jumpPageNumber.value !== '') {
    toast.error(`Vui lòng nhập trang từ 1 đến ${totalPages.value}`)
  }
  showJumpInput.value = 'none'
}

// ─── Load ────────────────────────────────────────────────────────────────────
async function loadLogs() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (filterKeyword.value.trim()) params.set('keyword', filterKeyword.value.trim())
    if (filterUserId.value !== '') params.set('userId', String(filterUserId.value))
    if (filterAction.value) params.set('action', filterAction.value)
    if (filterFrom.value) params.set('from', filterFrom.value + 'T00:00:00')
    if (filterTo.value) params.set('to', filterTo.value + 'T23:59:59')
    params.set('page', String(currentPage.value))
    params.set('size', String(pageSize.value))

    const res = await api.get(`/api/audit-logs?${params.toString()}`)
    if (res.ok) {
      const data = await res.json()
      logs.value = data.content || []
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
    } else {
      toast.error('Không thể tải nhật ký.')
    }
  } catch {
    toast.error('Lỗi kết nối máy chủ.')
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

async function loadUsers() {
  try {
    const [resUsers, resMe] = await Promise.all([
      api.get('/api/users'),
      api.get('/api/users/me')
    ])
    
    if (resUsers.ok && resMe.ok) {
      const allUsers = await resUsers.json()
      const currentUser = await resMe.json()
      
      if (currentUser.role === 'ADMIN') {
        // Ở chi nhánh tổng (ADMIN), chỉ hiện ADMIN và STAFF của chi nhánh tổng
        // Lọc bỏ các MANAGER của chi nhánh con
        users.value = allUsers.filter((u: any) => u.role !== 'MANAGER')
      } else {
        // Ở chi nhánh con (MANAGER), giữ nguyên (backend đã tự động chỉ trả về MANAGER và STAFF của chi nhánh đó)
        users.value = allUsers
      }
    } else if (resUsers.ok) {
      users.value = await resUsers.json()
    }
  } catch {}
}

watch(filterTimeRange, (val) => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().substring(0, 10)
  
  if (val === 'today') {
    filterFrom.value = fmt(today)
    filterTo.value = fmt(today)
  } else if (val === 'week') {
    const weekAgo = new Date(today)
    weekAgo.setDate(weekAgo.getDate() - 7)
    filterFrom.value = fmt(weekAgo)
    filterTo.value = fmt(today)
  } else if (val === 'last_week') {
    const twoWeeksAgo = new Date(today)
    twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14)
    filterFrom.value = fmt(twoWeeksAgo)
    filterTo.value = fmt(today)
  } else if (val === 'month') {
    const monthAgo = new Date(today)
    monthAgo.setMonth(monthAgo.getMonth() - 1)
    filterFrom.value = fmt(monthAgo)
    filterTo.value = fmt(today)
  }
})

function resetFilters() {
  filterKeyword.value = ''
  filterUserId.value = ''
  filterAction.value = ''
  filterTimeRange.value = 'today'
  currentPage.value = 0
  pageSize.value = 20
  // filterFrom & filterTo will auto-update via watcher on filterTimeRange
}

function formatDateTime(dt: string) {
  if (!dt) return '—'
  const d = new Date(dt)
  return d.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'medium' })
}

function actionLabel(action: string) {
  return ACTION_LABELS[action] || action
}

function actionColor(action: string, isWarning: boolean) {
  if (isWarning) return 'bg-rose-100 text-rose-700 border-rose-200 dark:bg-rose-900/30 dark:text-rose-400 dark:border-rose-800/50'
  const map: Record<string, string> = {
    'LOGIN':  'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-400 dark:border-emerald-800/50',
    'LOGOUT': 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
    'CREATE': 'bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-900/30 dark:text-indigo-400 dark:border-indigo-800/50',
    'CREATE_RECEIPT': 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800/50',
    'CREATE_DISPOSAL_RECEIPT': 'bg-fuchsia-50 text-fuchsia-700 border-fuchsia-200 dark:bg-fuchsia-900/30 dark:text-fuchsia-400 dark:border-fuchsia-800/50',
    'UPDATE': 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/30 dark:text-amber-400 dark:border-amber-800/50',
    'DELETE': 'bg-rose-50 text-rose-600 border-rose-200 dark:bg-rose-900/30 dark:text-rose-400 dark:border-rose-800/50',
    'RESTORE': 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-900/30 dark:text-sky-400 dark:border-sky-800/50',
    'APPROVE': 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-400 dark:border-emerald-800/50',
    'CANCEL': 'bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-900/30 dark:text-orange-400 dark:border-orange-800/50',
    'SPAM_WARNING': 'bg-rose-100 text-rose-700 border-rose-200 dark:bg-rose-900/30 dark:text-rose-400 dark:border-rose-800/50',
    'LOCK_ACCOUNT': 'bg-rose-200 text-rose-900 border-rose-300 dark:bg-rose-900/50 dark:text-rose-300 dark:border-rose-700',
    'CREATE_STOCKTAKE': 'bg-teal-50 text-teal-700 border-teal-200 dark:bg-teal-900/30 dark:text-teal-400 dark:border-teal-800/50',
    'UPDATE_STOCKTAKE': 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-900/30 dark:text-amber-400 dark:border-amber-800/50',
    'COMPLETE_STOCKTAKE': 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-400 dark:border-emerald-800/50',
    'CANCEL_STOCKTAKE': 'bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-900/30 dark:text-orange-400 dark:border-orange-800/50',
    'MARK_PAID': 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800/50',
    'CONFIRM_STOCKTAKE': 'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-900/30 dark:text-purple-400 dark:border-purple-800/50',
    'APPROVE_SHORTFALL': 'bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-900/30 dark:text-indigo-400 dark:border-indigo-800/50',
    'REJECT_SHORTFALL': 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-900/30 dark:text-rose-400 dark:border-rose-800/50',
    'COMPENSATE_SHORTFALL': 'bg-sky-50 text-sky-700 border-sky-200 dark:bg-sky-900/30 dark:text-sky-400 dark:border-sky-800/50',
    'CONFIRM_TRANSFER': 'bg-teal-50 text-teal-700 border-teal-200 dark:bg-teal-900/30 dark:text-teal-400 dark:border-teal-800/50',
    'IMPORT_EXCEL': 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800/50',
    'DỌN DẸP': 'bg-fuchsia-50 text-fuchsia-700 border-fuchsia-200 dark:bg-fuchsia-900/30 dark:text-fuchsia-400 dark:border-fuchsia-800/50',
  }
  return map[action] || 'bg-slate-50 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700'
}

onMounted(() => {
  loadLogs()
  loadUsers()
})

// Auto-search on any filter change with debounce
let debounceTimer: any = null

function triggerSearch() {
  // Validate thời gian
  if (filterFrom.value && filterTo.value && filterFrom.value > filterTo.value) {
    toast.error('Từ ngày không thể lớn hơn Đến ngày!')
    filterTo.value = filterFrom.value
    return
  }

  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    loadLogs()
  }, 300)
}

// ─── FAB Scroll Logic ─────────────────────────────────────────────────────────
const isAtTop = ref(true)

function handleScroll() {
  // Đang ở gần đỉnh (dưới 300px) thì coi là at top
  isAtTop.value = window.scrollY < 300
}

function toggleScroll() {
  if (isAtTop.value) {
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  handleScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

watch([filterKeyword, filterUserId, filterAction, filterFrom, filterTo, filterTimeRange, pageSize], () => {
  currentPage.value = 0
  triggerSearch()
})

watch(currentPage, () => {
  triggerSearch()
  window.scrollTo({ top: 0, behavior: 'smooth' })
})
</script>

<template>
  <div class="p-6 space-y-6 max-w-[1400px] mx-auto relative z-10 transition-colors">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2">
      <div>
        <h2 class="text-2xl font-bold text-slate-800 dark:text-white m-0 transition-colors">Nhật ký hoạt động</h2>
        <p class="text-slate-500 dark:text-slate-400 text-sm mt-1 transition-colors">Theo dõi và truy vết toàn bộ hành động trong hệ thống</p>
      </div>
      <div class="flex items-center gap-2">
        <button v-if="filterKeyword || filterUserId || filterAction || filterTimeRange !== 'today'"
          @click="resetFilters"
          class="flex items-center gap-2 px-4 py-2.5 bg-rose-50 text-rose-500 border border-rose-200 hover:bg-rose-100 dark:bg-rose-900/30 dark:border-rose-800 dark:text-rose-400 dark:hover:bg-rose-900/50 rounded-xl text-sm font-semibold transition-all shadow-sm">
          <i class="fas fa-times"></i> Bỏ lọc
        </button>
        <button @click="loadLogs" class="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 dark:bg-slate-800 dark:border-slate-600 rounded-xl text-sm font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 transition-all shadow-sm">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i>
          Làm mới
        </button>
      </div>
    </div>

    <!-- Main Card -->
    <div class="bg-indigo-50/50 dark:bg-slate-800/40 rounded-[16px] border border-slate-100 dark:border-slate-700/50 border-t-4 border-t-indigo-500 shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden transition-colors relative" ref="tableTopRef">
      
      <!-- Toolbar (Filters) -->
      <div class="p-5 border-b border-slate-100 dark:border-slate-700/50 bg-slate-50 dark:bg-slate-800/80 transition-colors">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Tìm kiếm đa năng -->
          <div class="lg:col-span-2 relative">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500"></i>
            <input v-model="filterKeyword" type="text" placeholder="Tìm kiếm mô tả, tài khoản..."
              class="w-full h-[42px] pl-11 pr-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200" />
          </div>
          <!-- Lọc nhân viên -->
          <div>
            <select v-model="filterUserId" class="w-full h-[42px] px-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200 cursor-pointer">
              <option value="">-- Tất cả nhân viên --</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName }} ({{ u.username }})</option>
            </select>
          </div>
          <!-- Lọc hành động -->
          <div>
            <select v-model="filterAction" class="w-full h-[42px] px-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200 cursor-pointer">
              <option value="">-- Tất cả hành động --</option>
              <option v-for="opt in ACTION_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <!-- Thời gian và Ngày -->
          <div class="lg:col-span-4 grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Khoảng thời gian</label>
              <select v-model="filterTimeRange" class="w-full h-[42px] px-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200 cursor-pointer">
                <option value="today">Hôm nay</option>
                <option value="week">7 ngày qua</option>
                <option value="last_week">14 ngày qua</option>
                <option value="month">30 ngày qua</option>
                <option value="custom">Tùy chọn ngày...</option>
              </select>
            </div>
            <!-- Từ ngày -->
            <div>
              <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Từ ngày</label>
              <input v-model="filterFrom" type="date" :disabled="filterTimeRange !== 'custom'"
                class="w-full h-[42px] px-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200 disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-800 disabled:cursor-not-allowed cursor-pointer" />
            </div>
            <!-- Đến ngày -->
            <div>
              <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-1.5">Đến ngày</label>
              <input v-model="filterTo" type="date" :disabled="filterTimeRange !== 'custom'"
                class="w-full h-[42px] px-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200 disabled:opacity-50 disabled:bg-slate-100 dark:disabled:bg-slate-800 disabled:cursor-not-allowed cursor-pointer" />
            </div>
          </div>
        </div>
      </div>

      <!-- Top Loading Bar -->
      <div v-if="loading && !initialLoading" class="absolute top-[auto] left-0 right-0 h-1 bg-indigo-100 dark:bg-indigo-900/50 z-20 overflow-hidden">
        <div class="h-full bg-indigo-500 animate-progress"></div>
      </div>

      <!-- Content Area -->
      <div class="p-0 transition-colors">
        <!-- Loading -->
        <div v-if="initialLoading" class="p-8 space-y-4">
          <div v-for="i in 8" :key="i" class="h-12 bg-slate-100 dark:bg-slate-800 rounded-xl animate-pulse"></div>
        </div>

        <!-- Empty -->
        <div v-else-if="filteredLogs.length === 0" class="py-24 text-center text-slate-400 dark:text-slate-500 flex flex-col items-center">
          <div class="w-24 h-24 mb-6 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center animate-pulse-slow">
            <i class="fas fa-history text-4xl opacity-50"></i>
          </div>
          <div class="font-bold text-slate-700 dark:text-slate-300 text-lg">Không có nhật ký nào</div>
          <div class="text-sm mt-2 max-w-[300px]">Thử thay đổi bộ lọc hoặc chọn khoảng thời gian khác.</div>
        </div>

        <!-- Table -->
        <div v-else class="overflow-x-auto">
          <table class="w-full text-left border-collapse text-sm">
            <thead class="bg-white dark:bg-slate-800/50">
              <tr>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Thời gian</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Người thực hiện</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Hành động</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Mô tả chi tiết</th>
              </tr>
            </thead>
            <tbody :class="{'opacity-50 pointer-events-none': loading, 'transition-opacity duration-300': true}">
              <tr
                v-for="(log, index) in filteredLogs"
                :key="log.id"
                :style="{ animationDelay: `${index * 0.04}s` }"
                :class="['animate-fade-in-up opacity-0 border-b border-slate-100 dark:border-slate-700/50 transition-all duration-300 group hover:-translate-y-[1px] cursor-pointer',
                  log.isWarning
                    ? 'bg-rose-50/50 hover:bg-rose-100/50 dark:bg-rose-900/10 dark:hover:bg-rose-900/20 hover:border-transparent'
                    : 'hover:bg-slate-50 dark:hover:bg-slate-700/50 hover:border-transparent']"
              >
                <!-- Thời gian -->
                <td class="p-4 whitespace-nowrap first:rounded-l-xl last:rounded-r-xl">
                  <div class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ formatDateTime(log.createdAt) }}</div>
                </td>
                <!-- Người thực hiện -->
                <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                  <div class="flex items-center gap-3">
                    <div :class="['w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold shrink-0 border',
                      log.isWarning ? 'bg-rose-100 text-rose-600 border-rose-200 dark:bg-rose-900/30 dark:border-rose-800' : 'bg-indigo-50 text-indigo-600 border-indigo-100 dark:bg-indigo-900/30 dark:border-indigo-800']">
                      {{ log.userFullName ? log.userFullName.charAt(0).toUpperCase() : '?' }}
                    </div>
                    <div>
                      <div class="font-bold text-slate-800 dark:text-slate-200">{{ log.userFullName || '[Đã xóa]' }}</div>
                      <div class="text-[11px] text-slate-500 dark:text-slate-400 font-mono mt-0.5">{{ log.username }}</div>
                    </div>
                  </div>
                </td>
                <!-- Hành động -->
                <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                  <span :class="['inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold border', actionColor(log.action, log.isWarning)]">
                    {{ actionLabel(log.action) }}
                  </span>
                </td>
                <!-- Mô tả -->
                <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                  <div :class="['text-sm leading-snug', log.isWarning ? 'text-rose-600 dark:text-rose-400 font-semibold' : 'text-slate-700 dark:text-slate-300']">
                    <template v-if="log.details && log.details.includes('\n')">
                      <div v-if="!log.expanded && log.details.split('\n').length > 2">
                        <div>{{ log.details.split('\n')[0] }}</div>
                        <button @click.stop="log.expanded = true" class="text-indigo-500 hover:text-indigo-700 dark:text-indigo-400 dark:hover:text-indigo-300 hover:underline text-[11px] mt-1 font-bold transition-colors">
                          <span v-if="log.action === 'CONFIRM_STOCKTAKE'">
                            Xem chi tiết (+{{ log.details.split('\n').length - 1 }} sản phẩm thiếu)
                          </span>
                          <span v-else-if="log.action === 'IMPORT_EXCEL'">
                            Xem chi tiết (+{{ log.details.split('\n').length - 1 }} sản phẩm)
                          </span>
                          <span v-else-if="log.action === 'APPROVE' && log.details.includes('Tiêu hủy')">
                            Xem chi tiết (+{{ log.details.split('\n').length - 1 }} sản phẩm tiêu hủy)
                          </span>
                          <span v-else>
                            Xem chi tiết (+{{ log.details.split('\n').length - 1 }} thay đổi)
                          </span>
                        </button>
                      </div>
                      <div v-else class="space-y-1">
                        <div v-for="(line, i) in log.details.split('\n')" :key="i" :class="{'font-bold text-indigo-600 dark:text-indigo-400 mb-1': Number(i) === 0, 'pl-2 border-l-2 border-slate-200 dark:border-slate-700 text-[11.5px]': Number(i) > 0}">{{ line }}</div>
                        <button v-if="log.details.split('\n').length > 2" @click.stop="log.expanded = false" class="text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-300 hover:underline text-[11px] mt-2 font-bold transition-colors">
                          Thu gọn
                        </button>
                      </div>
                    </template>
                    <template v-else>
                      {{ log.details }}
                    </template>
                  </div>
                  <div v-if="log.entityId && log.entityName !== 'users'" class="text-[11px] text-slate-400 dark:text-slate-500 font-mono mt-1">
                    {{ log.entityName }} #{{ log.entityId }}
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Pagination -->
          <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/80 border-t border-slate-100 dark:border-slate-700/50 flex flex-wrap items-center justify-between gap-4 transition-colors">
            <div class="flex items-center gap-4 text-xs font-bold text-slate-500 dark:text-slate-400">
              <div class="flex items-center gap-2">
                <span>Số dòng:</span>
                <select v-model="pageSize" class="h-8 px-2 border border-slate-200 dark:border-slate-600 rounded-lg bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 cursor-pointer transition-all">
                  <option :value="10">10</option>
                  <option :value="20">20</option>
                  <option :value="50">50</option>
                  <option :value="100">100</option>
                </select>
              </div>
            </div>
            <div class="flex items-center gap-1.5" v-if="totalPages > 0">
              <button @click="currentPage--" :disabled="currentPage === 0" class="w-8 h-8 flex items-center justify-center rounded-lg border border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm cursor-pointer">
                <i class="fas fa-chevron-left text-xs"></i>
              </button>
              
              <template v-for="(item, index) in paginationItems" :key="index">
                <button v-if="typeof item === 'number'"
                        @click="goToPage(item)"
                        :class="['w-8 h-8 flex items-center justify-center rounded-lg text-xs font-bold transition-all shadow-sm border cursor-pointer',
                          currentPage + 1 === item ? 'bg-indigo-600 text-white border-indigo-600 dark:bg-indigo-500 dark:border-indigo-500' : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800']">
                  {{ item }}
                </button>
                
                <div v-else-if="typeof item === 'string' && item.startsWith('...')" class="relative flex items-center justify-center w-8 h-8">
                  <button @click="openJumpInput(item.replace('...', '') as 'left' | 'right')"
                          class="w-8 h-8 flex items-center justify-center text-slate-400 dark:text-slate-500 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer" title="Nhảy đến trang">
                    <i class="fas fa-ellipsis-h text-[10px]"></i>
                  </button>
                  <div v-if="showJumpInput === item.replace('...', '')" class="absolute z-50 bottom-full mb-2 bg-white dark:bg-slate-900 shadow-[0_4px_15px_rgba(0,0,0,0.15)] border border-slate-300 dark:border-slate-600 transform -translate-x-1/2 left-1/2 w-[140px] flex flex-col">
                    <div class="bg-slate-700 dark:bg-slate-800 text-white font-bold text-[13px] px-2.5 py-1.5 text-left">
                      Chuyển trang...
                    </div>
                    <div class="p-2 flex items-center gap-2">
                      <input v-model="jumpPageNumber" @keyup.enter="handleJump" @blur="handleJump" v-focus
                             type="number" min="1" :max="totalPages"
                             class="flex-1 w-0 h-[28px] px-1 text-center text-[13px] font-semibold text-slate-800 dark:text-slate-200 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-[3px] outline-none focus:border-indigo-500" />
                      <button @mousedown.prevent="handleJump" class="w-[36px] h-[28px] bg-slate-700 dark:bg-slate-800 hover:bg-slate-800 text-white font-bold text-[13px] rounded-[3px] transition-colors cursor-pointer">
                        Đi
                      </button>
                    </div>
                  </div>
                </div>
              </template>

              <button @click="currentPage++" :disabled="currentPage >= totalPages - 1" class="w-8 h-8 flex items-center justify-center rounded-lg border border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm cursor-pointer">
                <i class="fas fa-chevron-right text-xs"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Floating Action Button (Thang máy) -->
    <button
      @click="toggleScroll"
      class="fixed bottom-8 right-8 w-12 h-12 flex items-center justify-center bg-indigo-600 text-white rounded-full shadow-lg shadow-indigo-500/30 hover:bg-indigo-500 transition-all z-50 hover:-translate-y-1 active:translate-y-0"
      :title="isAtTop ? 'Cuộn xuống cuối' : 'Cuộn lên trên'"
    >
      <i :class="isAtTop ? 'fas fa-arrow-down' : 'fas fa-arrow-up'"></i>
    </button>
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.fa-spin { animation: spin 0.8s linear infinite; }

@keyframes progress {
  0% { transform: translateX(-100%); width: 50%; }
  100% { transform: translateX(200%); width: 50%; }
}
.animate-progress {
  animation: progress 1s infinite linear;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.animate-fade-in-up {
  animation: fadeInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
</style>
