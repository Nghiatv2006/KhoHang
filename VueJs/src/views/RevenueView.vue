<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { api } from '../api'

const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const branches = ref<any[]>([])
const receipts = ref<any[]>([])
const summary = ref<any>(null)
const loading = ref(true)
const loadingSummary = ref(false)
const isExporting = ref(false)

// Phân quyền
const isAdmin = computed(() => user.value?.role === 'ADMIN')

// Admin: chọn chi nhánh để lọc
const selectedBranchId = ref<number | null>(null)

// Bộ lọc thời gian cho bảng
const activePeriod = ref<'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'>('month')

const customDateFrom = ref<string>('')
const customDateTo = ref<string>('')
const todayStr = ref<string>('')

// Tab bảng: 0 = Theo Hoá đơn, 1 = Theo Sản phẩm
const activeTab = ref<0 | 1>(0)

const periodOptions = [
  { key: 'today',   label: 'Hôm nay' },
  { key: 'week',    label: 'Tuần này' },
  { key: 'month',   label: 'Tháng này' },
  { key: 'quarter', label: 'Quý này' },
  { key: 'year',    label: 'Năm nay' },
  { key: 'custom',  label: 'Tùy chỉnh' },
]

// ── Fetch dữ liệu ────────────────────────────────────────────
async function fetchSummary() {
  loadingSummary.value = true
  try {
    let url = '/api/reports/revenue/summary'
    if (isAdmin.value && selectedBranchId.value) {
      url += `?branchId=${selectedBranchId.value}`
    }
    const res = await api.get(url)
    if (res.ok) summary.value = await res.json()
  } catch (e) {
    summary.value = null
  } finally {
    loadingSummary.value = false
  }
}

async function fetchReceipts() {
  try {
    const res = await api.get('/api/receipts/completed-branch')
    if (res.ok) receipts.value = await res.json()
  } catch (e) {
    receipts.value = []
  }
}

async function fetchBranches() {
  try {
    const res = await api.get('/api/branches')
    if (res.ok) {
      const data = await res.json()
      // Loại bỏ chi nhánh tổng (isHead = true) khỏi danh sách vì không có dữ liệu bán lẻ
      branches.value = data.filter((b: any) => !b.isHead)
    }
  } catch (e) {
    branches.value = []
  }
}

onMounted(async () => {
  loading.value = true
  const tasks: Promise<any>[] = [fetchReceipts(), fetchSummary()]
  if (isAdmin.value) tasks.push(fetchBranches())
  await Promise.allSettled(tasks)
  loading.value = false
})

// Khi Admin đổi chi nhánh → load lại summary
watch(selectedBranchId, () => {
  fetchSummary()
})

// ── Tính toán bảng hóa đơn theo kỳ ─────────────────────────
function formatToYMD(d: Date) {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

todayStr.value = formatToYMD(new Date())

function getDateRange(period: string): { from: Date; to: Date } {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  let from = new Date(today)

  if (period === 'today') {
    from = today
  } else if (period === 'week') {
    const dow = today.getDay() === 0 ? 6 : today.getDay() - 1
    from = new Date(today)
    from.setDate(today.getDate() - dow)
  } else if (period === 'month') {
    from = new Date(today.getFullYear(), today.getMonth(), 1)
  } else if (period === 'quarter') {
    const q = Math.floor(today.getMonth() / 3)
    from = new Date(today.getFullYear(), q * 3, 1)
  } else if (period === 'year') {
    from = new Date(today.getFullYear(), 0, 1)
  }
  return { from, to: now }
}

function setCustomDatesFromPeriod(period: string) {
  if (period === 'custom') return
  const { from, to } = getDateRange(period)
  customDateFrom.value = formatToYMD(from)
  customDateTo.value = formatToYMD(to)
}

watch(activePeriod, (newVal) => {
  if (newVal !== 'custom') {
    setCustomDatesFromPeriod(newVal)
  }
}, { immediate: true })

function onCustomDateChange() {
  if (customDateFrom.value && customDateTo.value && customDateFrom.value > customDateTo.value) {
    const temp = customDateFrom.value
    customDateFrom.value = customDateTo.value
    customDateTo.value = temp
  }
  activePeriod.value = 'custom'
}

const filteredReceipts = computed(() => {
  const from = new Date(customDateFrom.value)
  from.setHours(0, 0, 0, 0)
  const to = new Date(customDateTo.value)
  to.setHours(23, 59, 59, 999)
  const myBranchId = user.value?.branchId || user.value?.branch?.id

  return receipts.value.filter(r => {
    if (r.type !== 'EXPORT' || r.status !== 'COMPLETED' || !r.createdAt) return false
    const rDate = new Date(r.createdAt)
    if (rDate < from || rDate > to) return false
    if (!isAdmin.value && myBranchId) {
      if (Number(r.sourceBranchId) !== Number(myBranchId)) return false
    }
    if (isAdmin.value && selectedBranchId.value) {
      if (Number(r.sourceBranchId) !== Number(selectedBranchId.value)) return false
    }
    return true
  })
})

const filteredTotal = computed(() =>
  filteredReceipts.value.reduce((sum, r) => {
    const val = (r.details || []).reduce((s: number, d: any) =>
      s + (Number(d.quantity) || 0) * (Number(d.price) || 0), 0)
    return sum + val
  }, 0)
)

// ── Tính toán Tab Sản phẩm ───────────────────────────────────
const productStats = computed(() => {
  const map = new Map<number, {
    productId: number
    productName: string
    productCategory: string
    qty: number
    revenue: number
    cost: number
    profit: number
  }>()

  for (const r of filteredReceipts.value) {
    for (const d of (r.details || [])) {
      const pid = d.productId
      if (!pid) continue
      const revenue = (Number(d.quantity) || 0) * (Number(d.price) || 0)
      const importPrice = Number(d.importPrice) || 0
      const cost = (Number(d.quantity) || 0) * importPrice
      const profit = revenue - cost

      if (!map.has(pid)) {
        map.set(pid, {
          productId: pid,
          productName: d.productName || '—',
          productCategory: d.productCategory || '—',
          qty: 0,
          revenue: 0,
          cost: 0,
          profit: 0,
        })
      }
      const entry = map.get(pid)!
      entry.qty += Number(d.quantity) || 0
      entry.revenue += revenue
      entry.cost += cost
      entry.profit += profit
    }
  }

  return Array.from(map.values())
})

const totalProfitForProducts = computed(() =>
  productStats.value.reduce((s, p) => s + p.profit, 0)
)

const sortedProductStats = computed(() => {
  return [...productStats.value].sort((a, b) => b.profit - a.profit)
})

// ── Helpers ─────────────────────────────────────────────────
function formatVND(val: number | null | undefined) {
  if (!val || val === 0) return '0 VNĐ'
  return new Intl.NumberFormat('vi-VN').format(val) + ' VNĐ'
}

function formatVNDFull(val: number | null | undefined) {
  if (!val) return '0 VNĐ'
  return new Intl.NumberFormat('vi-VN').format(val) + ' VNĐ'
}

function formatDate(str: string) {
  if (!str) return '—'
  const d = new Date(str)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function receiptTotal(r: any) {
  return (r.details || []).reduce((s: number, d: any) =>
    s + (Number(d.quantity) || 0) * (Number(d.price) || 0), 0)
}



function formatContrib(val: number, total: number) {
  if (!total || total === 0) return '—'
  return ((val / total) * 100).toFixed(1) + '%'
}

function changePctClass(pct: number | null) {
  if (pct === null || pct === undefined) return 'text-slate-400'
  return pct >= 0 ? 'text-emerald-600' : 'text-rose-500'
}

function changePctLabel(pct: number | null) {
  if (pct === null || pct === undefined) return '—'
  const sign = pct >= 0 ? '+' : ''
  return `${sign}${pct.toFixed(1)}%`
}

function changePctIcon(pct: number | null) {
  if (pct === null || pct === undefined) return 'fas fa-minus'
  return pct >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'
}

function getBranchName(branchId: number) {
  const b = branches.value.find(b => b.id === branchId)
  return b?.name || `Chi nhánh ${branchId}`
}

function profitClass(profit: number) {
  if (profit > 0) return 'text-emerald-600'
  if (profit < 0) return 'text-rose-500'
  return 'text-slate-400'
}

// ── Xuất Excel ──────────────────────────────────────────────
async function exportExcel() {
  isExporting.value = true
  try {
    const token = localStorage.getItem('wh_token')
    let url = `/api/reports/revenue/excel?startDate=${customDateFrom.value}&endDate=${customDateTo.value}&period=${activePeriod.value}`
    // Truyền branchId vào URL nếu Admin đang chọn lọc theo chi nhánh
    if (isAdmin.value && selectedBranchId.value) {
      url += `&branchId=${selectedBranchId.value}`
    }
    const res = await fetch(url, {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    if (!res.ok) throw new Error('Lỗi tải báo cáo')
    const blob = await res.blob()
    const objUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = objUrl

    // Sinh tên file tự động
    let branchNamePart = 'ToanHeThong'
    if (isAdmin.value && selectedBranchId.value) {
      const b = branches.value.find((x: any) => x.id === selectedBranchId.value)
      if (b) branchNamePart = 'CN_' + b.name.replace(/ /g, '')
    } else if (!isAdmin.value && user.value?.branch?.name) {
      branchNamePart = 'CN_' + user.value.branch.name.replace(/ /g, '')
    }

    let timePart = ''
    if (activePeriod.value === 'custom') {
      timePart = `${customDateFrom.value.replace(/-/g, '')}_${customDateTo.value.replace(/-/g, '')}`
    } else {
      const periodMap: Record<string, string> = {
        'today': 'HomNay',
        'week': 'TuanNay',
        'month': 'ThangNay',
        'quarter': 'QuyNay',
        'year': 'NamNay'
      }
      timePart = periodMap[activePeriod.value] || 'Custom'
    }

    a.download = `BaoCaoDoanhThu_${branchNamePart}_${timePart}.xlsx`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(objUrl)
    document.body.removeChild(a)
  } catch (err: any) {
    alert('Có lỗi khi xuất Excel: ' + err.message)
  } finally {
    isExporting.value = false
  }
}
</script>

<template>
  <div class="max-w-[1400px]">

    <!-- ── Page Header ── -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h2 class="text-2xl font-bold text-slate-800 m-0 tracking-tight">Báo cáo Doanh thu</h2>
        <p class="text-[#8094ae] text-sm mt-1 m-0">Thống kê doanh thu & lợi nhuận xuất bán theo thời gian thực</p>
      </div>
      <button
        @click="exportExcel"
        :disabled="isExporting || filteredReceipts.length === 0"
        class="rev-export-btn"
      >
        <i class="fas" :class="isExporting ? 'fa-spinner fa-spin' : 'fa-file-excel'"></i>
        <span>{{ isExporting ? 'Đang xuất...' : 'Xuất Excel' }}</span>
      </button>
    </div>

    <!-- ── Bộ lọc Chi nhánh (chỉ Admin) ── -->
    <div v-if="isAdmin" class="mb-6 flex items-center gap-3">
      <label class="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Chi nhánh</label>
      <select
        v-model="selectedBranchId"
        class="rev-select"
      >
        <option :value="null">Toàn hệ thống</option>
        <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
      </select>
    </div>

    <!-- ── Loading ── -->
    <div v-if="loading" class="text-center py-16 text-slate-400">
      <i class="fas fa-spinner fa-spin text-3xl mb-3"></i>
      <p class="text-sm">Đang tải dữ liệu...</p>
    </div>

    <template v-else>
      <!-- ══════════════════════════════════════════════════════
           PHẦN 1: 4 THẺ METRIC (Doanh thu + Lợi nhuận)
           ══════════════════════════════════════════════════════ -->
      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-8">

        <!-- Tuần này -->
        <div class="rev-metric-card">
          <div class="rev-metric-label">Doanh thu Tuần này</div>
          <div class="rev-metric-value" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.weekRevenue) }}
          </div>
          <div class="rev-metric-footer">
            <span :class="changePctClass(summary?.weekChangePct)" class="flex items-center gap-1 text-xs font-semibold">
              <i :class="changePctIcon(summary?.weekChangePct)" class="text-[10px]"></i>
              {{ changePctLabel(summary?.weekChangePct) }}
            </span>
            <span class="text-slate-400 text-xs">so với tuần trước</span>
          </div>
          <div class="rev-metric-sub">
            Tuần trước: {{ formatVND(summary?.lastWeekRevenue) }}
          </div>
          <!-- Thực thu -->
          <div v-if="summary?.weekCollected !== undefined && !loadingSummary" class="rev-metric-collected mt-3">
            <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Thực thu:</span>
            <span class="text-[13px] font-bold text-slate-700 ml-1">
              {{ formatVNDFull(summary?.weekCollected) }}
            </span>
          </div>
          <!-- Lợi nhuận gộp -->
          <div v-if="summary?.weekProfit !== undefined && !loadingSummary" class="rev-metric-profit">
            <span class="rev-metric-profit-label">Lợi nhuận:</span>
            <span :class="profitClass(summary?.weekProfit || 0)" class="rev-metric-profit-val">
              {{ formatVNDFull(summary?.weekProfit) }}
            </span>
          </div>
          <div class="rev-metric-accent" style="background: #6366f1"></div>
        </div>

        <!-- Tháng này -->
        <div class="rev-metric-card">
          <div class="rev-metric-label">Doanh thu Tháng này</div>
          <div class="rev-metric-value" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.monthRevenue) }}
          </div>
          <div class="rev-metric-footer">
            <span :class="changePctClass(summary?.monthChangePct)" class="flex items-center gap-1 text-xs font-semibold">
              <i :class="changePctIcon(summary?.monthChangePct)" class="text-[10px]"></i>
              {{ changePctLabel(summary?.monthChangePct) }}
            </span>
            <span class="text-slate-400 text-xs">so với tháng trước</span>
          </div>
          <div class="rev-metric-sub">
            Tháng trước: {{ formatVND(summary?.lastMonthRevenue) }}
          </div>
          <!-- Thực thu -->
          <div v-if="summary?.monthCollected !== undefined && !loadingSummary" class="rev-metric-collected mt-3">
            <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Thực thu:</span>
            <span class="text-[13px] font-bold text-slate-700 ml-1">
              {{ formatVNDFull(summary?.monthCollected) }}
            </span>
          </div>
          <!-- Lợi nhuận gộp -->
          <div v-if="summary?.monthProfit !== undefined && !loadingSummary" class="rev-metric-profit">
            <span class="rev-metric-profit-label">Lợi nhuận:</span>
            <span :class="profitClass(summary?.monthProfit || 0)" class="rev-metric-profit-val">
              {{ formatVNDFull(summary?.monthProfit) }}
            </span>
          </div>
          <div class="rev-metric-accent" style="background: #0ea5e9"></div>
        </div>

        <!-- Quý này -->
        <div class="rev-metric-card">
          <div class="rev-metric-label">Doanh thu Quý này</div>
          <div class="rev-metric-value" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.quarterRevenue) }}
          </div>
          <div class="rev-metric-footer">
            <span class="text-slate-400 text-xs">Tổng doanh thu từ đầu quý đến nay</span>
          </div>
          <!-- Thực thu -->
          <div v-if="summary?.quarterCollected !== undefined && !loadingSummary" class="rev-metric-collected mt-3">
            <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Thực thu:</span>
            <span class="text-[13px] font-bold text-slate-700 ml-1">
              {{ formatVNDFull(summary?.quarterCollected) }}
            </span>
          </div>
          <!-- Lợi nhuận gộp -->
          <div v-if="summary?.quarterProfit !== undefined && !loadingSummary" class="rev-metric-profit">
            <span class="rev-metric-profit-label">Lợi nhuận:</span>
            <span :class="profitClass(summary?.quarterProfit || 0)" class="rev-metric-profit-val">
              {{ formatVNDFull(summary?.quarterProfit) }}
            </span>
          </div>
          <div class="rev-metric-accent" style="background: #10b981"></div>
        </div>

        <!-- Năm nay -->
        <div class="rev-metric-card">
          <div class="rev-metric-label">Doanh thu Năm nay</div>
          <div class="rev-metric-value" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.yearRevenue) }}
          </div>
          <div class="rev-metric-footer">
            <span class="text-slate-400 text-xs">Tổng doanh thu từ đầu năm đến nay</span>
          </div>
          <!-- Thực thu -->
          <div v-if="summary?.yearCollected !== undefined && !loadingSummary" class="rev-metric-collected mt-3">
            <span class="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Thực thu:</span>
            <span class="text-[13px] font-bold text-slate-700 ml-1">
              {{ formatVNDFull(summary?.yearCollected) }}
            </span>
          </div>
          <!-- Lợi nhuận gộp -->
          <div v-if="summary?.yearProfit !== undefined && !loadingSummary" class="rev-metric-profit">
            <span class="rev-metric-profit-label">Lợi nhuận:</span>
            <span :class="profitClass(summary?.yearProfit || 0)" class="rev-metric-profit-val">
              {{ formatVNDFull(summary?.yearProfit) }}
            </span>
          </div>
          <div class="rev-metric-accent" style="background: #f59e0b"></div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════
           PHẦN 2: BẢNG CHI TIẾT (Tab 1: Hoá đơn / Tab 2: Sản phẩm)
           ══════════════════════════════════════════════════════ -->
      <div class="rev-table-card">

        <!-- Header bảng + bộ lọc kỳ -->
        <div class="rev-table-header">
          <!-- Tab switch -->
          <div class="rev-tabs">
            <button
              @click="activeTab = 0"
              :class="['rev-tab-btn', activeTab === 0 ? 'rev-tab-btn--active' : '']"
            >
              Theo Hoá đơn
            </button>
            <button
              @click="activeTab = 1"
              :class="['rev-tab-btn', activeTab === 1 ? 'rev-tab-btn--active' : '']"
            >
              Theo Sản phẩm
            </button>
          </div>
          <!-- Period Tabs -->
          <div class="flex items-center gap-3">
            <div class="rev-period-tabs">
              <button
                v-for="opt in periodOptions"
                :key="opt.key"
                @click="activePeriod = opt.key as any"
                :class="['rev-period-tab', activePeriod === opt.key ? 'rev-period-tab--active' : '']"
              >
                {{ opt.label }}
              </button>
            </div>
            
            <div class="flex items-center gap-2">
              <input 
                type="date" 
                v-model="customDateFrom" 
                @change="onCustomDateChange"
                :max="todayStr"
                :class="['rev-date-input', activePeriod === 'custom' ? '!border-blue-500 !text-blue-600 !bg-blue-50' : '']" 
              />
              <span class="text-slate-400 text-sm">—</span>
              <input 
                type="date" 
                v-model="customDateTo" 
                @change="onCustomDateChange"
                :max="todayStr"
                :class="['rev-date-input', activePeriod === 'custom' ? '!border-blue-500 !text-blue-600 !bg-blue-50' : '']" 
              />
            </div>
          </div>
        </div>

        <!-- Thông tin tổng -->
        <div class="rev-table-subheader">
          <template v-if="activeTab === 0">
            <span class="text-xs text-slate-400">
              {{ filteredReceipts.length }} hóa đơn &mdash; Tổng:
              <span class="font-bold text-slate-600">{{ formatVNDFull(filteredTotal) }}</span>
            </span>
          </template>
          <template v-else>
            <span class="text-xs text-slate-400">
              {{ sortedProductStats.length }} sản phẩm &mdash; Tổng lợi nhuận:
              <span class="font-bold text-emerald-600">{{ formatVNDFull(totalProfitForProducts) }}</span>
            </span>
          </template>
        </div>

        <!-- ── TAB 1: Theo Hoá đơn ── -->
        <div v-if="activeTab === 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr>
                <th class="rev-th text-left w-[15%]">Ngày</th>
                <th v-if="isAdmin && !selectedBranchId" class="rev-th text-left w-[15%]">Chi nhánh</th>
                <th class="rev-th text-left">Khách hàng</th>
                <th class="rev-th text-left w-[15%]">Số mặt hàng</th>
                <th class="rev-th text-left w-[20%]">Tổng tiền</th>
                <th class="rev-th text-center w-[15%]">TT thanh toán</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredReceipts.length === 0">
                <td :colspan="isAdmin && !selectedBranchId ? 6 : 5" class="text-center py-12 text-slate-400 text-sm">
                  <i class="fas fa-inbox text-2xl mb-2 block text-slate-300"></i>
                  Không có dữ liệu trong kỳ đã chọn
                </td>
              </tr>
              <tr
                v-for="r in filteredReceipts"
                :key="r.id"
                class="rev-tr"
              >
                <td class="rev-td text-left text-slate-500 text-xs">{{ formatDate(r.createdAt) }}</td>
                <td v-if="isAdmin && !selectedBranchId" class="rev-td text-left text-xs text-slate-600">
                  {{ getBranchName(r.sourceBranchId) }}
                </td>
                <td class="rev-td text-left text-sm text-slate-700">{{ r.customerName || '—' }}</td>
                <td class="rev-td text-left text-xs text-slate-500">
                  {{ (r.details || []).length }} sp
                </td>
                <td class="rev-td text-left font-semibold text-slate-800 text-sm">
                  {{ formatVNDFull(receiptTotal(r)) }}
                </td>
                <td class="rev-td text-center">
                  <span :class="[
                    'rev-badge',
                    r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán'
                      ? 'rev-badge--paid'
                      : 'rev-badge--unpaid'
                  ]">
                    {{ r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán' ? 'Đã TT' : 'Chưa TT' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- ── TAB 2: Theo Sản phẩm ── -->
        <div v-else class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr>
                <th class="rev-th text-left">Tên sản phẩm</th>
                <th class="rev-th text-left w-[14%]">Danh mục</th>
                <th class="rev-th text-center w-[10%]">SL bán</th>
                <th class="rev-th text-left w-[18%]">Doanh thu</th>
                <th class="rev-th text-left w-[18%]">Lợi nhuận</th>
                <th class="rev-th text-center w-[12%]">% Đóng góp LN</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="sortedProductStats.length === 0">
                <td colspan="6" class="text-center py-12 text-slate-400 text-sm">
                  <i class="fas fa-inbox text-2xl mb-2 block text-slate-300"></i>
                  Không có dữ liệu trong kỳ đã chọn
                </td>
              </tr>
              <tr
                v-for="p in sortedProductStats"
                :key="p.productId"
                class="rev-tr"
              >
                <td class="rev-td text-left text-sm font-semibold text-slate-800">{{ p.productName }}</td>
                <td class="rev-td text-left text-xs text-slate-500">{{ p.productCategory }}</td>
                <td class="rev-td text-center text-xs text-slate-600">{{ p.qty }}</td>
                <td class="rev-td text-left text-sm text-slate-700">{{ formatVNDFull(p.revenue) }}</td>
                <td class="rev-td text-left text-sm font-semibold" :class="profitClass(p.profit)">
                  {{ formatVNDFull(p.profit) }}
                </td>
                <td class="rev-td text-center">
                  <span class="rev-contrib-badge">
                    {{ formatContrib(p.profit, totalProfitForProducts) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

      </div>
    </template>
  </div>
</template>

<style scoped>
/* ── Export Button ───────────────────────────────────────── */
.rev-export-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  background: #1e3a5f;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  letter-spacing: 0.02em;
  transition: background 0.2s, transform 0.15s;
  box-shadow: 0 2px 8px rgba(30, 58, 95, 0.18);
}
.rev-export-btn:hover:not(:disabled) {
  background: #15304f;
  transform: translateY(-1px);
}
.rev-export-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* ── Branch Select ──────────────────────────────────────── */
.rev-select {
  padding: 7px 36px 7px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #334155;
  background: #fff;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  transition: border-color 0.2s;
}
.rev-select:focus {
  border-color: #6366f1;
}

/* ── Metric Cards ──────────────────────────────────────── */
.rev-metric-card {
  position: relative;
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  padding: 22px 24px 18px;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s, transform 0.2s;
}
.rev-metric-card:hover {
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.rev-metric-accent {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 3px;
  border-radius: 14px 14px 0 0;
}
.rev-metric-label {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #94a3b8;
  margin-bottom: 10px;
}
.rev-metric-value {
  font-size: 1.55rem;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin-bottom: 10px;
  transition: opacity 0.3s;
}
.rev-metric-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.rev-metric-sub {
  font-size: 0.72rem;
  color: #94a3b8;
  margin-bottom: 8px;
}
.rev-metric-profit {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #f1f5f9;
}
.rev-metric-profit-label {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #94a3b8;
  white-space: nowrap;
}
.rev-metric-profit-val {
  font-size: 0.8rem;
  font-weight: 700;
}
.rev-metric-profit-pct {
  font-size: 0.72rem;
  font-weight: 600;
  opacity: 0.75;
}

/* ── Table Card ─────────────────────────────────────────── */
.rev-table-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.rev-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 0;
  flex-wrap: wrap;
  gap: 12px;
}
.rev-table-subheader {
  padding: 8px 24px 14px;
  border-bottom: 1px solid #f1f5f9;
}

/* ── Tab Buttons ─────────────────────────────────────────── */
.rev-tabs {
  display: flex;
  gap: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.rev-tab-btn {
  padding: 6px 16px;
  font-size: 0.78rem;
  font-weight: 600;
  border: none;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.rev-tab-btn + .rev-tab-btn {
  border-left: 1px solid #e2e8f0;
}
.rev-tab-btn:hover {
  background: #e2e8f0;
  color: #334155;
}
.rev-tab-btn--active {
  background: #1e3a5f;
  color: #ffffff;
}
.rev-tab-btn--active:hover {
  background: #1e3a5f;
  color: #ffffff;
}

/* ── Period Tabs ─────────────────────────────────────────── */
.rev-period-tabs {
  display: flex;
  gap: 4px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 3px;
}
.rev-period-tab {
  padding: 5px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  border: none;
  background: transparent;
  color: #64748b;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.rev-period-tab:hover {
  background: #e2e8f0;
  color: #334155;
}
.rev-period-tab--active {
  background: #1e3a5f;
  color: #ffffff;
}
.rev-period-tab--active:hover {
  background: #1e3a5f;
  color: #ffffff;
}

/* ── Date Input ─────────────────────────────────────────── */
.rev-date-input {
  padding: 5px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.75rem;
  color: #334155;
  background: #fff;
  cursor: pointer;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}
.rev-date-input:focus {
  border-color: #6366f1;
}

.rev-date-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.6;
}
.rev-date-input::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}

/* ── Table ──────────────────────────────────────────────── */
.rev-th {
  padding: 10px 16px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #94a3b8;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  white-space: nowrap;
}
.rev-tr {
  transition: background 0.12s;
}
.rev-tr:hover {
  background: #f8fafc;
}
.rev-td {
  padding: 11px 16px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}
.rev-tr:last-child .rev-td {
  border-bottom: none;
}

/* ── Badges ─────────────────────────────────────────────── */
.rev-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}
.rev-badge--paid {
  background: #d1fae5;
  color: #065f46;
}
.rev-badge--unpaid {
  background: #fee2e2;
  color: #991b1b;
}

/* ── Contribution Badge ─────────────────────────────────── */
.rev-contrib-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 700;
  background: #eff6ff;
  color: #1d4ed8;
  letter-spacing: 0.03em;
}

/* ── Dark Mode ──────────────────────────────────────────── */
html.dark-mode .rev-metric-card,
html.dark-mode .rev-table-card {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark-mode .rev-metric-value {
  color: #f8fafc !important;
}
html.dark-mode .rev-metric-label {
  color: #64748b !important;
}
html.dark-mode .rev-metric-sub {
  color: #64748b !important;
}
html.dark-mode .rev-metric-profit {
  border-top-color: #334155 !important;
}
html.dark-mode .rev-table-header h3 {
  color: #f8fafc !important;
}
html.dark-mode .rev-th {
  background: #0f172a !important;
  color: #64748b !important;
  border-color: #334155 !important;
}
html.dark-mode .rev-td {
  border-color: #1e293b !important;
}
html.dark-mode .rev-tr:hover {
  background: #0f172a !important;
}
html.dark-mode .rev-td .text-slate-700,
html.dark-mode .rev-td .text-slate-800 {
  color: #e2e8f0 !important;
}
html.dark-mode .rev-td .text-slate-600 {
  color: #94a3b8 !important;
}
html.dark-mode .rev-select {
  background-color: #1e293b !important;
  border-color: #334155 !important;
  color: #e2e8f0 !important;
}
html.dark-mode .rev-period-tabs {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark-mode .rev-period-tab {
  color: #94a3b8 !important;
}
html.dark-mode .rev-period-tab:hover {
  background: #1e293b !important;
  color: #e2e8f0 !important;
}
html.dark-mode .rev-period-tab--active {
  background: #1e3a5f !important;
  color: #ffffff !important;
}
html.dark-mode .rev-tabs {
  border-color: #334155 !important;
}
html.dark-mode .rev-tab-btn {
  background: #0f172a !important;
  color: #94a3b8 !important;
}
html.dark-mode .rev-tab-btn + .rev-tab-btn {
  border-color: #334155 !important;
}
html.dark-mode .rev-tab-btn:hover {
  background: #1e293b !important;
  color: #e2e8f0 !important;
}
html.dark-mode .rev-tab-btn--active {
  background: #1e3a5f !important;
  color: #ffffff !important;
}
html.dark-mode .rev-contrib-badge {
  background: #1e3a5f !important;
  color: #93c5fd !important;
}
html.dark-mode .rev-export-btn {
  background: #1e3a5f !important;
}
html.dark-mode .rev-table-subheader {
  border-color: #334155 !important;
}
</style>
