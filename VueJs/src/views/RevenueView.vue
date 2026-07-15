<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { api } from '../api'

const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const branches = ref<any[]>([])
const receipts = ref<any[]>([])
const inventories = ref<any[]>([])
const summary = ref<any>(null)
const loading = ref(true)
const loadingSummary = ref(false)
const isExporting = ref(false)
const showLegend = ref(false)

// Phân quyền
const isAdmin = computed(() => user.value?.role === 'ADMIN')

// Admin: chọn chi nhánh để lọc
const selectedBranchId = ref<number | null>(null)

// Bộ lọc thời gian cho bảng
const activePeriod = ref<'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'>('month')

const customDateFrom = ref<string>('')
const customDateTo = ref<string>('')
const todayStr = ref<string>('')

// Bộ lọc trạng thái thanh toán
const filterPaymentStatus = ref<string>('')

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

async function fetchInventories() {
  try {
    const res = await api.get('/api/inventories')
    if (res.ok) inventories.value = await res.json()
  } catch (e) {
    inventories.value = []
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
  const tasks: Promise<any>[] = [fetchReceipts(), fetchSummary(), fetchInventories()]
  if (isAdmin.value) tasks.push(fetchBranches())
  await Promise.allSettled(tasks)
  loading.value = false
})

// Khi Admin đổi chi nhánh → load lại summary
watch(selectedBranchId, () => {
  fetchSummary()
})

function receiptTotal(r: any) {
  if (!r.details) return 0
  return r.details.reduce((sum: number, d: any) => sum + (Number(d.price) || 0) * (Number(d.quantity) || 0), 0)
}

function receiptProfit(r: any) {
  if (!r.details) return 0
  return r.details.reduce((sum: number, d: any) => {
    const revenue = (Number(d.price) || 0) * (Number(d.quantity) || 0)
    const cost = (Number(d.importPrice) || 0) * (Number(d.quantity) || 0)
    return sum + (revenue - cost)
  }, 0)
}

function receiptCollected(r: any) {
  const total = receiptTotal(r)
  if (r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán') return total
  return 0
}

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
    if (activeTab.value === 0 && filterPaymentStatus.value) {
      const isPaid = r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán'
      if (filterPaymentStatus.value === 'PAID' && !isPaid) return false
      if (filterPaymentStatus.value === 'UNPAID' && isPaid) return false
    }
    return true
  }).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
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

const filterCategory = ref<string>('')
const filterSearch = ref<string>('')

const uniqueCategories = computed(() => {
  const cats = new Set<string>()
  for (const p of productStats.value) {
    if (p.productCategory && p.productCategory !== '—') {
      cats.add(p.productCategory)
    }
  }
  return Array.from(cats).sort()
})

const sortedProductStats = computed(() => {
  let list = productStats.value
  
  if (filterCategory.value) {
    list = list.filter(p => p.productCategory === filterCategory.value)
  }
  
  if (filterSearch.value) {
    const term = filterSearch.value.toLowerCase()
    list = list.filter(p => p.productName.toLowerCase().includes(term) || (p.productId.toString() === term))
  }
  
  return [...list].sort((a, b) => b.profit - a.profit)
})

const totalProfitForProducts = computed(() =>
  sortedProductStats.value.reduce((s, p) => s + p.profit, 0)
)

const totalRevenueForProducts = computed(() =>
  sortedProductStats.value.reduce((s, p) => s + p.revenue, 0)
)

// ── Phân trang ─────────────────────────────────────────────────
const itemsPerPage = 10

const currentPageReceipts = ref(1)
const totalPagesReceipts = computed(() => Math.ceil(filteredReceipts.value.length / itemsPerPage) || 1)
const paginatedReceipts = computed(() => {
  const start = (currentPageReceipts.value - 1) * itemsPerPage
  return filteredReceipts.value.slice(start, start + itemsPerPage)
})

const currentPageProducts = ref(1)
const totalPagesProducts = computed(() => Math.ceil(sortedProductStats.value.length / itemsPerPage) || 1)
const paginatedProducts = computed(() => {
  const start = (currentPageProducts.value - 1) * itemsPerPage
  return sortedProductStats.value.slice(start, start + itemsPerPage)
})

// Reset trang khi đổi bộ lọc
watch([filteredReceipts, activeTab], () => {
  currentPageReceipts.value = 1
})

watch([sortedProductStats, activeTab], () => {
  currentPageProducts.value = 1
})

function getPaginationArray(current: number, total: number) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  if (current <= 3) return [1, 2, 3, 4, '...', total]
  if (current >= total - 2) return [1, '...', total - 3, total - 2, total - 1, total]
  return [1, '...', current - 1, current, current + 1, '...', total]
}

function handleJumpPage(type: 'receipts' | 'products') {
  const total = type === 'receipts' ? totalPagesReceipts.value : totalPagesProducts.value
  const result = window.prompt(`Nhập số trang muốn chuyển đến (1 - ${total}):`)
  if (result) {
    const page = parseInt(result, 10)
    if (!isNaN(page) && page >= 1 && page <= total) {
      if (type === 'receipts') currentPageReceipts.value = page
      else currentPageProducts.value = page
    } else {
      window.alert(`Số trang không hợp lệ! Vui lòng nhập từ 1 đến ${total}.`)
    }
  }
}

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
  const arrow = pct > 0 ? '↑ ' : (pct < 0 ? '↓ ' : '')
  const sign = pct > 0 ? '+' : ''
  return `${arrow}${sign}${pct.toFixed(1)}%`
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
  <div class="space-y-6 max-w-[1400px] mx-auto font-sans">
    <!-- ── Page Header ── -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0 flex items-center gap-3">
          Báo cáo Doanh thu
        </h2>
        <p class="text-[#8094ae] text-sm mt-1">Thống kê doanh thu & lợi nhuận xuất bán theo thời gian thực</p>
      </div>
      <div class="flex items-center gap-3">
        <button
          @click="showLegend = true"
          class="h-[42px] px-4 border border-[#e2e8f0] bg-white hover:bg-[#f8f9fa] text-[var(--accent-500)] rounded-xl text-sm font-bold shadow-sm transition-all flex items-center gap-2"
        >
          <i class="fas fa-info-circle"></i>
          <span>Chú giải</span>
        </button>

        <!-- Bộ lọc Chi nhánh (chỉ Admin) -->
        <div v-if="isAdmin" class="flex items-center gap-2">
          <select
            v-model="selectedBranchId"
            class="h-[42px] px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm font-semibold focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63]"
          >
            <option :value="null">Toàn hệ thống</option>
            <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
        </div>
        
        <button
          @click="exportExcel"
          :disabled="isExporting || filteredReceipts.length === 0"
          class="h-[42px] bg-emerald-600 hover:bg-emerald-700 text-white px-5 rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center gap-2 disabled:opacity-50"
        >
          <span>{{ isExporting ? 'Đang xuất...' : 'Xuất Excel' }}</span>
        </button>
      </div>
    </div>

    <!-- ── Loading ── -->
    <div v-if="loading" class="flex items-center justify-center h-48 gap-3 text-[#8094ae]">
      <span class="font-semibold text-sm">Đang tải dữ liệu...</span>
    </div>

    <template v-else>
      <!-- ══════════════════════════════════════════════════════
           PHẦN 1: 4 THẺ METRIC
           ══════════════════════════════════════════════════════ -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">

        <!-- Tuần này -->
        <div class="bg-white rounded-2xl p-5 border border-[#f1f5f9] flex flex-col h-full hover:border-[#e2e8f0] hover:shadow-md transition-all">
          <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wide mb-2">Doanh thu Tuần này</div>
          <div class="text-2xl font-extrabold text-slate-800 mb-1" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.weekRevenue) }}
          </div>
          <div :class="changePctClass(summary?.weekChangePct)" class="text-[11px] font-bold mb-4">
            {{ changePctLabel(summary?.weekChangePct) }} <span class="text-[#8094ae] font-normal">so với tuần trước</span>
          </div>
          
          <div class="space-y-2 mt-auto text-sm" :class="loadingSummary ? 'opacity-30' : 'transition-opacity duration-300'">

            <div v-if="summary?.weekCollected !== undefined " class="flex items-center justify-between">
              <span class="text-[#8094ae] text-xs">Thực thu:</span>
              <span class="font-bold text-slate-700">{{ formatVNDFull(summary?.weekCollected) }}</span>
            </div>
            <div v-if="summary?.weekProfit !== undefined " class="flex items-center justify-between border-t border-[#f1f5f9] pt-2 mt-1">
              <span class="font-bold text-slate-600 text-[11px] uppercase tracking-wide">Lợi nhuận:</span>
              <span class="font-extrabold" :class="profitClass(summary?.weekProfit || 0)">{{ formatVNDFull(summary?.weekProfit) }}</span>
            </div>
          </div>
        </div>

        <!-- Tháng này -->
        <div class="bg-white rounded-2xl p-5 border border-[#f1f5f9] flex flex-col h-full hover:border-[#e2e8f0] hover:shadow-md transition-all">
          <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wide mb-2">Doanh thu Tháng này</div>
          <div class="text-2xl font-extrabold text-slate-800 mb-1" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.monthRevenue) }}
          </div>
          <div :class="changePctClass(summary?.monthChangePct)" class="text-[11px] font-bold mb-4">
            {{ changePctLabel(summary?.monthChangePct) }} <span class="text-[#8094ae] font-normal">so với tháng trước</span>
          </div>
          
          <div class="space-y-2 mt-auto text-sm" :class="loadingSummary ? 'opacity-30' : 'transition-opacity duration-300'">

            <div v-if="summary?.monthCollected !== undefined " class="flex items-center justify-between">
              <span class="text-[#8094ae] text-xs">Thực thu:</span>
              <span class="font-bold text-slate-700">{{ formatVNDFull(summary?.monthCollected) }}</span>
            </div>
            <div v-if="summary?.monthProfit !== undefined " class="flex items-center justify-between border-t border-[#f1f5f9] pt-2 mt-1">
              <span class="font-bold text-slate-600 text-[11px] uppercase tracking-wide">Lợi nhuận:</span>
              <span class="font-extrabold" :class="profitClass(summary?.monthProfit || 0)">{{ formatVNDFull(summary?.monthProfit) }}</span>
            </div>
          </div>
        </div>

        <!-- Quý này -->
        <div class="bg-white rounded-2xl p-5 border border-[#f1f5f9] flex flex-col h-full hover:border-[#e2e8f0] hover:shadow-md transition-all">
          <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wide mb-2">Doanh thu Quý này</div>
          <div class="text-2xl font-extrabold text-slate-800 mb-1" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.quarterRevenue) }}
          </div>
          <div class="text-[11px] font-normal text-[#8094ae] mb-4">
            Tổng doanh thu từ đầu quý đến nay
          </div>
          
          <div class="space-y-2 mt-auto text-sm" :class="loadingSummary ? 'opacity-30' : 'transition-opacity duration-300'">
            <div v-if="summary?.quarterCollected !== undefined " class="flex items-center justify-between">
              <span class="text-[#8094ae] text-xs">Thực thu:</span>
              <span class="font-bold text-slate-700">{{ formatVNDFull(summary?.quarterCollected) }}</span>
            </div>
            <div v-if="summary?.quarterProfit !== undefined " class="flex items-center justify-between border-t border-[#f1f5f9] pt-2 mt-1">
              <span class="font-bold text-slate-600 text-[11px] uppercase tracking-wide">Lợi nhuận:</span>
              <span class="font-extrabold" :class="profitClass(summary?.quarterProfit || 0)">{{ formatVNDFull(summary?.quarterProfit) }}</span>
            </div>
          </div>
        </div>

        <!-- Năm nay -->
        <div class="bg-white rounded-2xl p-5 border border-[#f1f5f9] flex flex-col h-full hover:border-[#e2e8f0] hover:shadow-md transition-all">
          <div class="text-[11px] font-bold text-[#8094ae] uppercase tracking-wide mb-2">Doanh thu Năm nay</div>
          <div class="text-2xl font-extrabold text-slate-800 mb-1" :class="loadingSummary ? 'opacity-30' : ''">
            {{ loadingSummary ? '...' : formatVND(summary?.yearRevenue) }}
          </div>
          <div class="text-[11px] font-normal text-[#8094ae] mb-4">
            Tổng doanh thu từ đầu năm đến nay
          </div>
          
          <div class="space-y-2 mt-auto text-sm" :class="loadingSummary ? 'opacity-30' : 'transition-opacity duration-300'">
            <div v-if="summary?.yearCollected !== undefined " class="flex items-center justify-between">
              <span class="text-[#8094ae] text-xs">Thực thu:</span>
              <span class="font-bold text-slate-700">{{ formatVNDFull(summary?.yearCollected) }}</span>
            </div>
            <div v-if="summary?.yearProfit !== undefined " class="flex items-center justify-between border-t border-[#f1f5f9] pt-2 mt-1">
              <span class="font-bold text-slate-600 text-[11px] uppercase tracking-wide">Lợi nhuận:</span>
              <span class="font-extrabold" :class="profitClass(summary?.yearProfit || 0)">{{ formatVNDFull(summary?.yearProfit) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════
           PHẦN 2: BẢNG CHI TIẾT
           ══════════════════════════════════════════════════════ -->
      <div class="bg-white rounded-2xl border border-[#f1f5f9] border-t-4 border-t-[var(--accent-500)] shadow-sm overflow-hidden mt-6">
        
        <!-- Toolbar -->
        <div class="p-5 border-b border-[#f1f5f9]">
          <div class="flex flex-col xl:flex-row xl:items-center justify-between gap-4">
            <!-- Tabs Mode -->
            <div class="flex bg-[#f8f9fa] p-1 rounded-xl border border-[#e2e8f0] self-start">
              <button
                @click="activeTab = 0"
                :class="['px-5 py-2 text-sm font-bold rounded-lg transition-all', activeTab === 0 ? 'bg-white text-[var(--accent-500)] shadow-sm' : 'text-[#8094ae] hover:text-slate-700']"
              >
                Theo Hoá đơn
              </button>
              <button
                @click="activeTab = 1"
                :class="['px-5 py-2 text-sm font-bold rounded-lg transition-all', activeTab === 1 ? 'bg-white text-[var(--accent-500)] shadow-sm' : 'text-[#8094ae] hover:text-slate-700']"
              >
                Theo Sản phẩm
              </button>
            </div>

            <!-- Filters -->
            <div class="flex flex-wrap items-center gap-3">
              <select
                v-if="activeTab === 0"
                v-model="filterPaymentStatus"
                class="h-[38px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63] shadow-sm font-semibold cursor-pointer"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="PAID">Đã thanh toán</option>
                <option value="UNPAID">Chưa thanh toán</option>
              </select>
              
              <select
                v-model="activePeriod"
                class="h-[38px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63] shadow-sm font-semibold cursor-pointer"
              >
                <option v-for="opt in periodOptions" :key="opt.key" :value="opt.key">
                  {{ opt.label }}
                </option>
              </select>
              
              <div class="flex items-center gap-2">
                <input 
                  type="date" 
                  v-model="customDateFrom" 
                  @change="onCustomDateChange"
                  :max="todayStr"
                  :class="['h-[38px] px-3 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-xs font-semibold focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63]', activePeriod === 'custom' ? '!border-[var(--accent-500)] !bg-blue-50 !text-[var(--accent-500)]' : '']" 
                />
                <span class="text-[#8094ae] text-sm">—</span>
                <input 
                  type="date" 
                  v-model="customDateTo" 
                  @change="onCustomDateChange"
                  :max="todayStr"
                  :class="['h-[38px] px-3 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-xs font-semibold focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63]', activePeriod === 'custom' ? '!border-[var(--accent-500)] !bg-blue-50 !text-[var(--accent-500)]' : '']" 
                />
              </div>
            </div>
          </div>
          
          <!-- Lọc Sản Phẩm (Chỉ hiện ở Tab Theo Sản Phẩm) -->
          <div v-if="activeTab === 1" class="mt-4 flex flex-col sm:flex-row gap-3">
            <div class="flex-1 relative">
              <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"></i>
              <input 
                type="text" 
                v-model="filterSearch" 
                placeholder="Tìm tên hoặc mã sản phẩm..." 
                class="w-full pl-10 pr-4 h-[38px] border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all"
              />
            </div>
            <select 
              v-model="filterCategory"
              class="w-full sm:w-[250px] h-[38px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all cursor-pointer"
            >
              <option value="">Tất cả danh mục</option>
              <option v-for="c in uniqueCategories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>

          <!-- Tổng kết nhanh -->
          <div class="mt-4 pt-4 border-t border-[#f1f5f9] flex justify-end">
            <template v-if="activeTab === 0">
              <span class="text-xs text-[#8094ae] uppercase tracking-wide font-bold">
                {{ filteredReceipts.length }} hóa đơn &mdash; Tổng tiền:
                <span class="font-extrabold text-[var(--accent-500)] text-sm ml-1">{{ formatVNDFull(filteredTotal) }}</span>
              </span>
            </template>
            <template v-else>
              <span class="text-xs text-[#8094ae] uppercase tracking-wide font-bold">
                {{ sortedProductStats.length }} sản phẩm &mdash; Doanh thu:
                <span class="font-extrabold text-[var(--accent-500)] text-sm ml-1 mr-3">{{ formatVNDFull(totalRevenueForProducts) }}</span>
                Lợi nhuận:
                <span class="font-extrabold text-emerald-600 text-sm ml-1">{{ formatVNDFull(totalProfitForProducts) }}</span>
              </span>
            </template>
          </div>
        </div>

        <Transition
          mode="out-in"
          enter-active-class="transition-opacity duration-300 ease-out"
          enter-from-class="opacity-0"
          enter-to-class="opacity-100"
          leave-active-class="transition-opacity duration-200 ease-in"
          leave-from-class="opacity-100"
          leave-to-class="opacity-0"
        >
          <!-- TAB 1: Hoá đơn -->
          <div v-if="activeTab === 0" key="tab-0" class="w-full flex flex-col">
            <div class="w-full overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
              <tr class="bg-[#f8f9fa] text-[#8094ae] text-xs uppercase tracking-wider">
                <th class="px-5 py-3 text-left font-bold w-[14%]">Mã HĐ & Ngày</th>
                <th v-if="isAdmin && !selectedBranchId" class="px-5 py-3 text-left font-bold w-[14%]">Chi nhánh</th>
                <th class="px-5 py-3 text-left font-bold w-[18%]">NV & Khách hàng</th>
                <th class="px-5 py-3 text-left font-bold w-[15%] whitespace-nowrap">Tổng tiền</th>
                <th class="px-5 py-3 text-left font-bold w-[15%] whitespace-nowrap">Thực thu/Nợ</th>
                <th class="px-5 py-3 text-left font-bold w-[14%] whitespace-nowrap">Lợi nhuận</th>
                <th class="px-5 py-3 text-center font-bold w-[10%]">Trạng thái</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[#f1f5f9]">
              <tr v-if="paginatedReceipts.length === 0">
                <td :colspan="isAdmin && !selectedBranchId ? 7 : 6" class="text-center py-12 text-[#8094ae] text-sm font-medium">
                  Không có dữ liệu trong kỳ đã chọn
                </td>
              </tr>
              <tr v-for="r in paginatedReceipts" :key="r.id" class="hover:bg-slate-50/60 transition-colors group even:bg-slate-50/20">
                <td class="px-5 py-4">
                  <div class="font-bold text-slate-800">{{ r.code }}</div>
                  <div class="text-[11px] text-[#8094ae] mt-0.5">{{ formatDate(r.createdAt) }}</div>
                </td>
                <td v-if="isAdmin && !selectedBranchId" class="px-5 py-4 text-xs font-semibold text-slate-600">
                  {{ getBranchName(r.sourceBranchId) }}
                </td>
                <td class="px-5 py-4">
                  <div class="font-bold text-[var(--accent-500)] text-[11px] tracking-wide uppercase">{{ r.createdByName || '—' }}</div>
                  <div class="text-sm font-semibold text-slate-700 mt-0.5">{{ r.customerName || 'Khách lẻ' }}</div>
                </td>
                <td class="px-5 py-4 text-sm font-extrabold text-slate-800 whitespace-nowrap">
                  {{ formatVNDFull(receiptTotal(r)) }}
                </td>
                <td class="px-5 py-4 text-sm whitespace-nowrap">
                  <div class="font-bold text-emerald-600">{{ formatVNDFull(receiptCollected(r)) }}</div>
                  <div v-if="receiptTotal(r) - receiptCollected(r) > 0" class="text-[11px] font-bold text-rose-500 mt-0.5">
                    Nợ: {{ formatVNDFull(receiptTotal(r) - receiptCollected(r)) }}
                  </div>
                </td>
                <td class="px-5 py-4 text-sm font-extrabold text-emerald-600 whitespace-nowrap">
                  {{ formatVNDFull(receiptProfit(r)) }}
                </td>
                <td class="px-5 py-4 text-center">
                  <span :class="[
                    'inline-block px-3 py-1 rounded-lg text-[11px] uppercase font-bold tracking-wider',
                    r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán'
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-rose-100 text-rose-700'
                  ]">
                    {{ r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán' ? 'Đã TT' : 'Chưa TT' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
            </div>
            <!-- Pagination Tab 1 -->
            <div class="px-5 py-3 border-t border-[#f1f5f9] flex flex-col sm:flex-row gap-3 items-center justify-between bg-white w-full">
              <span class="text-xs font-semibold text-[#8094ae]">
                Hiển thị {{ paginatedReceipts.length ? (currentPageReceipts - 1) * itemsPerPage + 1 : 0 }} - {{ Math.min(currentPageReceipts * itemsPerPage, filteredReceipts.length) }} trên tổng số {{ filteredReceipts.length }}
              </span>
              <div class="flex items-center gap-1">
                <button @click="currentPageReceipts--" :disabled="currentPageReceipts === 1" class="w-8 h-8 flex items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-40 transition-colors" title="Trang trước"><i class="fas fa-chevron-left text-[10px]"></i></button>
                <template v-for="(p, idx) in getPaginationArray(currentPageReceipts, totalPagesReceipts)" :key="idx">
                  <button v-if="p !== '...'" @click="currentPageReceipts = Number(p)" :class="['min-w-[32px] h-8 px-2 flex items-center justify-center rounded-lg text-xs font-bold transition-colors', currentPageReceipts === p ? 'bg-[var(--accent-500)] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900']">{{ p }}</button>
                  <button v-else @click="handleJumpPage('receipts')" class="min-w-[32px] h-8 px-1 flex items-center justify-center text-slate-400 hover:text-[var(--accent-500)] hover:bg-blue-50 rounded-lg transition-colors font-bold" title="Nhấn để chuyển trang">...</button>
                </template>
                <button @click="currentPageReceipts++" :disabled="currentPageReceipts === totalPagesReceipts" class="w-8 h-8 flex items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-40 transition-colors" title="Trang sau"><i class="fas fa-chevron-right text-[10px]"></i></button>
              </div>
            </div>
          </div>

          <!-- TAB 2: Sản phẩm -->
          <div v-else key="tab-1" class="w-full flex flex-col">
            <div class="w-full overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
              <tr class="bg-[#f8f9fa] text-[#8094ae] text-xs uppercase tracking-wider">
                <th class="px-5 py-3 text-left font-bold">Tên sản phẩm</th>
                <th class="px-5 py-3 text-left font-bold w-[14%]">Danh mục</th>
                <th class="px-5 py-3 text-center font-bold w-[10%]">SL bán</th>
                <th class="px-5 py-3 text-left font-bold w-[18%]">Doanh thu</th>
                <th class="px-5 py-3 text-left font-bold w-[18%]">Lợi nhuận</th>
                <th class="px-5 py-3 text-center font-bold w-[12%]">% Đóng góp LN</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[#f1f5f9]">
              <tr v-if="paginatedProducts.length === 0">
                <td colspan="6" class="text-center py-12 text-[#8094ae] text-sm font-medium">
                  Không có dữ liệu trong kỳ đã chọn
                </td>
              </tr>
              <tr v-for="p in paginatedProducts" :key="p.productId" class="hover:bg-slate-50/60 transition-colors group even:bg-slate-50/20">
                <td class="px-5 py-4 text-sm font-semibold text-slate-800">{{ p.productName }}</td>
                <td class="px-5 py-4 text-xs font-semibold text-slate-500">{{ p.productCategory }}</td>
                <td class="px-5 py-4 text-center text-xs font-bold text-[#8094ae] bg-slate-50/30">{{ p.qty }}</td>
                <td class="px-5 py-4 text-sm font-bold text-slate-700">{{ formatVNDFull(p.revenue) }}</td>
                <td class="px-5 py-4 text-sm font-extrabold" :class="profitClass(p.profit)">
                  {{ formatVNDFull(p.profit) }}
                </td>
                <td class="px-5 py-4 text-center">
                  <span class="inline-block px-3 py-1 bg-blue-50 text-blue-700 rounded-lg text-[11px] font-bold">
                    {{ formatContrib(p.profit, totalProfitForProducts) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
            </div>
            <!-- Pagination Tab 2 -->
            <div class="px-5 py-3 border-t border-[#f1f5f9] flex flex-col sm:flex-row gap-3 items-center justify-between bg-white w-full">
              <span class="text-xs font-semibold text-[#8094ae]">
                Hiển thị {{ paginatedProducts.length ? (currentPageProducts - 1) * itemsPerPage + 1 : 0 }} - {{ Math.min(currentPageProducts * itemsPerPage, sortedProductStats.length) }} trên tổng số {{ sortedProductStats.length }}
              </span>
              <div class="flex items-center gap-1">
                <button @click="currentPageProducts--" :disabled="currentPageProducts === 1" class="w-8 h-8 flex items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-40 transition-colors" title="Trang trước"><i class="fas fa-chevron-left text-[10px]"></i></button>
                <template v-for="(p, idx) in getPaginationArray(currentPageProducts, totalPagesProducts)" :key="idx">
                  <button v-if="p !== '...'" @click="currentPageProducts = Number(p)" :class="['min-w-[32px] h-8 px-2 flex items-center justify-center rounded-lg text-xs font-bold transition-colors', currentPageProducts === p ? 'bg-[var(--accent-500)] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900']">{{ p }}</button>
                  <button v-else @click="handleJumpPage('products')" class="min-w-[32px] h-8 px-1 flex items-center justify-center text-slate-400 hover:text-[var(--accent-500)] hover:bg-blue-50 rounded-lg transition-colors font-bold" title="Nhấn để chuyển trang">...</button>
                </template>
                <button @click="currentPageProducts++" :disabled="currentPageProducts === totalPagesProducts" class="w-8 h-8 flex items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-40 transition-colors" title="Trang sau"><i class="fas fa-chevron-right text-[10px]"></i></button>
              </div>
            </div>
          </div>
        </Transition>

      </div>
    </template>

    <!-- Modal Chú giải -->
    <Transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="showLegend" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm" @click="showLegend = false">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg flex flex-col max-h-[85vh] overflow-hidden" @click.stop>
          <div class="flex items-center justify-between p-5 border-b border-[#f1f5f9] shrink-0">
            <h3 class="text-lg font-bold text-slate-800 flex items-center gap-2">
              <i class="fas fa-book text-[var(--accent-500)]"></i> Chú giải Thuật ngữ
            </h3>
            <button @click="showLegend = false" class="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-rose-500 hover:bg-rose-50 transition-colors">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="p-6 space-y-5 text-sm text-slate-600 overflow-y-auto custom-scrollbar">
            <div class="space-y-1">
              <div class="font-bold text-slate-800 text-[15px]"><span class="text-[var(--accent-500)] mr-1">1.</span>Tổng tiền (Tổng doanh thu)</div>
              <p class="leading-relaxed">Tính bằng <strong class="text-slate-700">Số lượng bán × Giá bán</strong>. Ghi nhận doanh thu của tất cả các hóa đơn bán hàng thành công (Bao gồm cả khách đã thanh toán và khách đang nợ).</p>
            </div>
            
            <div class="space-y-1">
              <div class="font-bold text-slate-800 text-[15px]"><span class="text-emerald-600 mr-1">2.</span>Thực thu</div>
              <p class="leading-relaxed">Là dòng tiền thực tế thu về két. Chỉ cộng dồn tổng tiền từ các hóa đơn có trạng thái <strong class="text-emerald-600">Đã thanh toán</strong>.</p>
            </div>
            
            <div class="space-y-1">
              <div class="font-bold text-slate-800 text-[15px]"><span class="text-rose-500 mr-1">3.</span>Tiền nợ</div>
              <p class="leading-relaxed">Tính bằng <strong class="text-slate-700">Tổng tiền - Thực thu</strong>. Được hiển thị tại cột "Thực thu/Nợ" ở tab <strong class="text-slate-700">Theo Hoá đơn</strong> với 2 trường hợp:</p>
              <ul class="list-disc pl-5 space-y-1 mt-1 text-slate-600">
                <li><strong class="text-emerald-600">Khách đã thanh toán:</strong> Thực thu bằng Tổng tiền ➔ Tiền nợ = <strong class="text-slate-700">0 VNĐ</strong>.</li>
                <li><strong class="text-rose-500">Khách chưa thanh toán:</strong> Thực thu bằng 0 ➔ Tiền nợ = <strong class="text-slate-700">Tổng tiền hóa đơn</strong>.</li>
              </ul>
            </div>
            
            <div class="space-y-1">
              <div class="font-bold text-slate-800 text-[15px]"><span class="text-amber-500 mr-1">4.</span>Lợi nhuận & % Đóng góp</div>
              <ul class="list-disc pl-5 space-y-1 mt-1 text-slate-600">
                <li><strong class="text-slate-700">Lợi nhuận:</strong> Bằng <strong class="text-slate-700">(Giá bán - Giá vốn nhập kho) × Số lượng bán</strong>.</li>
                <li><strong class="text-slate-700">% Đóng góp LN:</strong> Cho biết sản phẩm này đang đóng góp bao nhiêu phần trăm vào tổng lợi nhuận của chi nhánh hiện tại.</li>
              </ul>
            </div>

            <div class="bg-blue-50/50 p-4 rounded-xl border border-blue-100 mt-2 space-y-3">
              <div>
                <div class="font-bold text-blue-800 mb-1 flex items-center gap-2">
                  <i class="fas fa-info-circle"></i> Quy tắc chốt số liệu
                </div>
                <p class="text-blue-700/80 leading-relaxed text-[13px]">Báo cáo lấy dữ liệu theo <strong class="text-blue-800">Ngày tạo hóa đơn</strong> và chỉ tính hóa đơn <strong class="text-blue-800">Hoàn thành</strong> (Tự động bỏ hóa đơn Hủy).</p>
              </div>
              <div>
                <div class="font-bold text-blue-800 mb-1 flex items-center gap-2">
                  <i class="fas fa-file-excel"></i> Lưu ý Xuất Excel (Raw Data)
                </div>
                <p class="text-blue-700/80 leading-relaxed text-[13px]">File xuất ra luôn lấy <strong>Toàn bộ dữ liệu</strong> theo Khoảng thời gian & Chi nhánh. Các bộ lọc phụ (Trạng thái thanh toán, Tên/Danh mục SP) <strong>không</strong> ảnh hưởng tới file tải về để đảm bảo tính toàn vẹn của Báo cáo tổng.</p>
              </div>
            </div>
          </div>
          <div class="p-4 bg-slate-50 border-t border-[#f1f5f9] flex justify-end shrink-0">
            <button @click="showLegend = false" class="px-6 py-2 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-xl transition-colors shadow-sm">
              Đã hiểu
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>
