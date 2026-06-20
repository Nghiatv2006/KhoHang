<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'

const toast = useToast()

// ─── State ────────────────────────────────────────────────────────────────────
const logs = ref<any[]>([])
const loading = ref(true)
const users = ref<any[]>([])

const todayStr = new Date().toISOString().substring(0, 10)
const filterKeyword = ref('')
const filterUserId = ref<number | ''>('')
const filterAction = ref('')
const filterFrom = ref(todayStr)
const filterTo = ref(todayStr)
const filterTimeRange = ref('today')

// ─── Action labels ────────────────────────────────────────────────────────────
const ACTION_LABELS: Record<string, string> = {
  'LOGIN':         'Đăng nhập',
  'LOGOUT':        'Đăng xuất',
  'CREATE':        'Thêm mới',
  'UPDATE':        'Cập nhật',
  'DELETE':        'Xóa',
  'RESTORE':       'Khôi phục',
  'APPROVE':       'Duyệt phiếu',
  'CANCEL':        'Hủy phiếu',
  'SPAM_WARNING':  '⚠️ Cảnh báo SPAM',
  'LOCK_ACCOUNT':  '🔒 Khóa tài khoản',
}

const ACTION_OPTIONS = Object.entries(ACTION_LABELS).map(([value, label]) => ({ value, label }))

// ─── Computed ─────────────────────────────────────────────────────────────────
const filteredLogs = computed(() => {
  return logs.value
})

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

    const res = await api.get(`/api/audit-logs?${params.toString()}`)
    if (res.ok) {
      logs.value = await res.json()
    } else {
      toast.error('Không thể tải nhật ký.')
    }
  } catch {
    toast.error('Lỗi kết nối máy chủ.')
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const res = await api.get('/api/users')
    if (res.ok) users.value = await res.json()
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
    const oneWeekAgo = new Date(today)
    oneWeekAgo.setDate(oneWeekAgo.getDate() - 7)
    filterFrom.value = fmt(twoWeeksAgo)
    filterTo.value = fmt(oneWeekAgo)
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
  if (isWarning) return 'bg-red-100 text-red-700 border-red-200'
  const map: Record<string, string> = {
    'LOGIN':  'bg-emerald-50 text-emerald-700 border-emerald-200',
    'LOGOUT': 'bg-slate-100 text-slate-600 border-slate-200',
    'CREATE': 'bg-blue-50 text-blue-700 border-blue-200',
    'UPDATE': 'bg-amber-50 text-amber-700 border-amber-200',
    'DELETE': 'bg-red-50 text-red-600 border-red-200',
    'RESTORE': 'bg-indigo-50 text-indigo-700 border-indigo-200',
    'APPROVE': 'bg-emerald-50 text-emerald-700 border-emerald-200',
    'CANCEL': 'bg-orange-50 text-orange-700 border-orange-200',
    'SPAM_WARNING': 'bg-red-100 text-red-700 border-red-200',
    'LOCK_ACCOUNT': 'bg-red-200 text-red-900 border-red-300',
  }
  return map[action] || 'bg-slate-50 text-slate-600 border-slate-200'
}

onMounted(() => {
  loadLogs()
  loadUsers()
})

// Auto-search on any filter change with debounce
let debounceTimer: any = null
watch([filterKeyword, filterUserId, filterAction, filterFrom, filterTo, filterTimeRange], () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  
  // Validate thời gian
  if (filterFrom.value && filterTo.value && filterFrom.value > filterTo.value) {
    toast.error('Từ ngày không thể lớn hơn Đến ngày!')
    filterTo.value = filterFrom.value // Tự động fix lại cho bằng nhau để không bị lỗi
    return
  }

  debounceTimer = setTimeout(() => {
    loadLogs()
  }, 300)
})
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[#364a63] flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-[#4361ee] to-[#4cc9f0] flex items-center justify-center">
            <i class="fas fa-history text-white"></i>
          </div>
          Nhật ký hoạt động
        </h1>
        <p class="text-sm text-[#8094ae] mt-1">Theo dõi và truy vết toàn bộ hành động trong chi nhánh của bạn</p>
      </div>
      <div class="flex items-center gap-2">
        <button v-if="filterKeyword || filterUserId || filterAction || filterTimeRange !== 'today'"
          @click="resetFilters"
          class="flex items-center gap-2 px-4 py-2.5 bg-red-50 text-red-500 border border-red-200 hover:bg-red-100 rounded-xl text-sm font-semibold transition-all shadow-sm">
          <i class="fas fa-times"></i> Bỏ lọc
        </button>
        <button @click="loadLogs" class="flex items-center gap-2 px-4 py-2.5 bg-white border border-[#e2e8f0] rounded-xl text-sm font-semibold text-[#364a63] hover:bg-[#f8f9fa] transition-all shadow-sm">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i>
          Làm mới
        </button>
      </div>
    </div>



    <!-- Bộ lọc nâng cao -->
    <div class="bg-white rounded-2xl border border-[#e2e8f0] shadow-sm p-5">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Tìm kiếm đa năng -->
        <div class="lg:col-span-2 relative">
          <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae] text-sm"></i>
          <input v-model="filterKeyword" type="text" placeholder="Tìm kiếm mô tả, mã phiếu, tên sản phẩm..."
            class="w-full pl-10 pr-4 h-11 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all" />
        </div>
        <!-- Lọc nhân viên -->
        <div>
          <select v-model="filterUserId" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
            <option value="">-- Tất cả nhân viên --</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName }} ({{ u.username }})</option>
          </select>
        </div>
        <!-- Lọc hành động -->
        <div>
          <select v-model="filterAction" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
            <option value="">-- Tất cả hành động --</option>
            <option v-for="opt in ACTION_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <!-- Thời gian và Ngày -->
        <div class="lg:col-span-4 grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Thời gian</label>
            <select v-model="filterTimeRange" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="today">Hôm nay</option>
              <option value="week">7 ngày qua</option>
              <option value="last_week">Tuần trước (14 ngày qua)</option>
              <option value="month">30 ngày qua</option>
              <option value="custom">Tùy chọn ngày...</option>
            </select>
          </div>
          <!-- Từ ngày -->
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Từ ngày</label>
            <input v-model="filterFrom" type="date" :disabled="filterTimeRange !== 'custom'"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed" />
          </div>
          <!-- Đến ngày -->
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Đến ngày</label>
            <input v-model="filterTo" type="date" :disabled="filterTimeRange !== 'custom'"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed" />
          </div>
        </div>
      </div>
    </div>

    <!-- Bảng Log -->
    <div class="bg-white rounded-2xl border border-[#e2e8f0] shadow-sm overflow-hidden">
      <!-- Loading -->
      <div v-if="loading" class="p-8 space-y-3">
        <div v-for="i in 8" :key="i" class="h-14 bg-[#f8f9fa] rounded-xl animate-pulse"></div>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredLogs.length === 0" class="py-20 text-center text-[#8094ae]">
        <i class="fas fa-clipboard-list text-5xl mb-4 opacity-30"></i>
        <div class="font-bold text-[#364a63]">Không có nhật ký nào</div>
        <div class="text-sm mt-1">Thử thay đổi bộ lọc hoặc chọn khoảng thời gian khác</div>
      </div>

      <!-- Table -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-collapse text-sm">
          <thead class="bg-[#f8f9fa] border-b border-[#f1f5f9]">
            <tr>
              <th class="px-4 py-3 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Thời gian</th>
              <th class="px-4 py-3 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Người thực hiện</th>
              <th class="px-4 py-3 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Hành động</th>
              <th class="px-4 py-3 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Mô tả chi tiết</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="log in filteredLogs"
              :key="log.id"
              :class="['border-b border-[#f1f5f9] transition-colors',
                log.isWarning
                  ? 'bg-red-50 hover:bg-red-100'
                  : 'hover:bg-[#f8f9fa]']"
            >
              <!-- Thời gian -->
              <td class="px-4 py-3 whitespace-nowrap">
                <div class="font-mono text-xs text-[#8094ae]">{{ formatDateTime(log.createdAt) }}</div>
              </td>
              <!-- Người thực hiện -->
              <td class="px-4 py-3">
                <div class="flex items-center gap-2.5">
                  <div :class="['w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
                    log.isWarning ? 'bg-red-200 text-red-700' : 'bg-[#eef2ff] text-[#4361ee]']">
                    {{ log.userFullName ? log.userFullName.charAt(0).toUpperCase() : '?' }}
                  </div>
                  <div>
                    <div class="font-semibold text-[#364a63] text-xs">{{ log.userFullName || '[Đã xóa]' }}</div>
                    <div class="text-[#8094ae] text-[11px] font-mono">{{ log.username }}</div>
                  </div>
                </div>
              </td>
              <!-- Hành động -->
              <td class="px-4 py-3">
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold border', actionColor(log.action, log.isWarning)]">
                  {{ actionLabel(log.action) }}
                </span>
              </td>
              <!-- Mô tả -->
              <td class="px-4 py-3">
                <div :class="['text-sm leading-snug', log.isWarning ? 'text-red-700 font-semibold' : 'text-[#364a63]']">
                  {{ log.details }}
                </div>
                <div v-if="log.entityId && log.entityName !== 'users'" class="text-[11px] text-[#8094ae] font-mono mt-0.5">
                  {{ log.entityName }} #{{ log.entityId }}
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="px-6 py-4 bg-[#f8f9fa] border-t border-[#f1f5f9] text-xs font-bold text-[#8094ae]">
          Hiển thị {{ filteredLogs.length }} dòng nhật ký
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.fa-spin { animation: spin 0.8s linear infinite; }
</style>
