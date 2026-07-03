<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')

const loading = ref(true)
const inventories = ref<any[]>([])
const products = ref<any[]>([])
const branches = ref<any[]>([])
const categories = ref<any[]>([])

// Tabs: 'head' (Kho Tổng), 'sub' (Chi nhánh con)
const activeTab = ref<'head' | 'sub'>('head')

// Sub-branch filter selection (for Admin when on 'sub' tab, or Manager/Staff of sub-branch)
const selectedSubBranchId = ref<number | string>('')
const selectedCategoryId = ref<number | string>('')
const searchKeyword = ref('')
const onlyWarning = ref(false)
const onlyExpired = ref(false)

function toggleOnlyWarning() {
  onlyWarning.value = !onlyWarning.value
  if (onlyWarning.value) {
    onlyExpired.value = false
  }
}

function toggleOnlyExpired() {
  onlyExpired.value = !onlyExpired.value
  if (onlyExpired.value) {
    onlyWarning.value = false
  }
}

function clearFilters() {
  onlyWarning.value = false
  onlyExpired.value = false
  searchKeyword.value = ''
  selectedCategoryId.value = ''
}

function onWarningChange() {
  if (onlyWarning.value) {
    onlyExpired.value = false
  }
}

function onExpiredChange() {
  if (onlyExpired.value) {
    onlyWarning.value = false
  }
}

// Config Threshold Modal
const showConfigModal = ref(false)
const thresholdForm = ref<number>(5)
const configuringBranchId = ref<number | null>(null)

// Right Panel Details
const showDetailPanel = ref(false)
const selectedInv = ref<any>(null)

// Add Stock Modal state




// Find Head Branch from list
const headBranch = computed(() => {
  return branches.value.find(b => b.isHead) || branches.value.find(b => b.id === 1) || branches.value[0] || null
})

// Check if current user belongs to Head Branch
const userIsAtHeadBranch = computed(() => {
  if (isAdmin.value) return true // Admin defaults to Head Branch view
  if (!headBranch.value) return false
  return user.value?.branchId === headBranch.value.id
})

// Set default tab on load based on user role/branch
function initTab() {
  if (isAdmin.value) {
    activeTab.value = 'head'
  } else if (userIsAtHeadBranch.value) {
    activeTab.value = 'head'
  } else {
    activeTab.value = 'sub'
    selectedSubBranchId.value = user.value?.branchId || ''
  }
}

// Load Data
async function loadData() {
  loading.value = true
  try {
    const [invRes, prodRes, branchRes, catRes] = await Promise.all([
      api.get('/api/inventories'),
      api.get('/api/products'),
      api.get('/api/branches'),
      api.get('/api/categories')
    ])

    if (invRes.ok) inventories.value = await invRes.json()
    if (prodRes.ok) products.value = await prodRes.json()
    if (catRes.ok) categories.value = await catRes.json()
    if (branchRes.ok) {
      branches.value = await branchRes.json()
      initTab()
    }
  } catch (err: any) {
    toast.error('Lỗi khi tải dữ liệu: ' + err.message)
  } finally {
    loading.value = false
  }
}

// Sub-branches list
const subBranches = computed(() => {
  if (!headBranch.value) return branches.value
  return branches.value.filter(b => b.id !== headBranch.value.id)
})

// Low stock threshold active context
const activeThreshold = computed(() => {
  if (activeTab.value === 'head') {
    return headBranch.value ? headBranch.value.lowStockThreshold : 5
  }
  if (!selectedSubBranchId.value) {
    return Number(localStorage.getItem('wh_global_threshold') || '5')
  }
  const br = branches.value.find(b => b.id === Number(selectedSubBranchId.value))
  return br ? br.lowStockThreshold : 5
})

// Match prices, units and calculate totals
const computedInventories = computed(() => {
  const today = new Date()
  return inventories.value.map(inv => {
    const prod = products.value.find(p => p.id === inv.productId)
    const price = prod ? Number(prod.price) : 0
    const importPrice = prod ? Number(prod.importPrice) : 0
    const unit = prod ? prod.unit : 'Chiếc'
    const categoryId = prod ? prod.categoryId : null
    const categoryName = prod ? prod.categoryName : 'Mặc định'
    const br = branches.value.find(b => b.id === inv.branchId)
    const threshold = br ? br.lowStockThreshold : activeThreshold.value

    let isExpired = false
    let isExpiryWarning = false
    if (inv.hasExpiry && inv.expirationDate && !inv.expirationDate.startsWith('1970-01-01')) {
      const exp = new Date(inv.expirationDate)
      isExpired = exp < today
      if (!isExpired) {
        const diffDays = Math.ceil((exp.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
        isExpiryWarning = diffDays <= (inv.expiryWarningDays || 30)
      }
    }

    return {
      ...inv,
      price,
      importPrice,
      unit,
      categoryId,
      categoryName,
      totalValue: price * inv.quantity,
      totalImportValue: importPrice * inv.quantity,
      isWarning: inv.quantity <= threshold,
      isExpired,
      isExpiryWarning,
      threshold
    }
  })
})

// Grouped/Isolated inventories by active tab and selected sub-branch
const activeTabInventories = computed(() => {
  let result = [...computedInventories.value]

  // Tab isolation
  if (activeTab.value === 'head') {
    if (headBranch.value) {
      result = result.filter(inv => inv.branchId === headBranch.value.id)
    }
  } else {
    // Sub-branch tab
    if (headBranch.value) {
      result = result.filter(inv => inv.branchId !== headBranch.value.id)
    }
    if (selectedSubBranchId.value) {
      result = result.filter(inv => inv.branchId === Number(selectedSubBranchId.value))
    }
  }
  return result
})

// Filtered list by active tab, search keyword, and filters
const filteredInventories = computed(() => {
  let result = [...activeTabInventories.value]

  // Filter by Keyword (sku, product name or batch code)
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    result = result.filter(inv =>
      inv.productName?.toLowerCase().includes(kw) ||
      inv.productSku?.toLowerCase().includes(kw) ||
      inv.batchCode?.toLowerCase().includes(kw)
    )
  }

  // Filter by Category
  if (selectedCategoryId.value !== '') {
    result = result.filter(inv => inv.categoryId === Number(selectedCategoryId.value))
  }

  // Filter by Warning Status
  if (onlyWarning.value) {
    result = result.filter(inv => inv.isWarning)
  }

  // Filter by Expiry Status
  if (onlyExpired.value) {
    result = result.filter(inv => inv.isExpired)
  }

  // Default Sort: lastUpdated DESC
  return result.sort((a, b) => {
    const dateA = a.lastUpdated ? new Date(a.lastUpdated).getTime() : 0
    const dateB = b.lastUpdated ? new Date(b.lastUpdated).getTime() : 0
    return dateB - dateA
  })
})

// Summaries
const totalDistinctProducts = computed(() => new Set(activeTabInventories.value.map(i => i.productId)).size)
const totalInventoryValue = computed(() => activeTabInventories.value.reduce((sum, item) => sum + item.totalValue, 0))
const totalWarningsCount = computed(() => activeTabInventories.value.filter(item => item.isWarning).length)

// Expired lots count
const totalExpiredCount = computed(() => {
  return activeTabInventories.value.filter(inv => inv.isExpired).length
})

const isInitialLoad = ref(true)
let initialLoadTimeout: ReturnType<typeof setTimeout> | null = null

function startInitialLoadTimer() {
  if (initialLoadTimeout) clearTimeout(initialLoadTimeout)
  initialLoadTimeout = setTimeout(() => {
    isInitialLoad.value = false
  }, 1300)
}

watch(loading, (newVal) => {
  if (!newVal) {
    startInitialLoadTimer()
  }
})

watch([activeTab, selectedSubBranchId], () => {
  isInitialLoad.value = true
  startInitialLoadTimer()
})

function triggerInventoryAnimation() {
  isInitialLoad.value = true
  loadData()
}

onMounted(() => {
  window.addEventListener('trigger-inventory-animation', triggerInventoryAnimation)
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('trigger-inventory-animation', triggerInventoryAnimation)
})

// Open Configure Threshold
function openConfigModal() {
  if (activeTab.value === 'head') {
    thresholdForm.value = headBranch.value ? headBranch.value.lowStockThreshold : 5
    configuringBranchId.value = headBranch.value ? headBranch.value.id : null
  } else if (!selectedSubBranchId.value) {
    thresholdForm.value = Number(localStorage.getItem('wh_global_threshold') || '5')
    configuringBranchId.value = null
  } else {
    const br = branches.value.find(b => b.id === Number(selectedSubBranchId.value))
    thresholdForm.value = br ? br.lowStockThreshold : 5
    configuringBranchId.value = Number(selectedSubBranchId.value)
  }
  showConfigModal.value = true
}

// Save Threshold
async function saveThreshold() {
  if (thresholdForm.value < 0) {
    toast.error('Ngưỡng tồn kho không được nhỏ hơn 0.')
    return
  }

  try {
    if (configuringBranchId.value === null) {
      localStorage.setItem('wh_global_threshold', thresholdForm.value.toString())
      toast.success('Cập nhật ngưỡng toàn cục thành công!')
      showConfigModal.value = false
    } else {
      const br = branches.value.find(b => b.id === configuringBranchId.value)
      if (!br) return
      
      const payload = {
        name: br.name,
        address: br.address,
        taxCode: br.taxCode,
        lowStockThreshold: thresholdForm.value
      }

      const res = await api.put(`/api/branches/${configuringBranchId.value}`, payload)
      if (res.ok) {
        toast.success(`Cập nhật ngưỡng thành công!`)
        showConfigModal.value = false
        await loadData()
      } else {
        const errData = await res.json()
        toast.error(errData.message || 'Lỗi khi cập nhật.')
      }
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  }
}

// Open Right Panel Details
function openDetails(inv: any) {
  selectedInv.value = inv
  showDetailPanel.value = true
}


// Get lot status
function getLotStatus(inv: any) {
  if (!inv.hasExpiry || !inv.expirationDate || inv.expirationDate.startsWith('1970-01-01')) {
    return { label: 'Bình thường', class: 'bg-slate-100 text-slate-700' }
  }
  const today = new Date()
  const exp = new Date(inv.expirationDate)
  const diffDays = Math.ceil((exp.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))

  if (diffDays < 0) {
    return { label: 'Đã hết hạn', class: 'bg-red-100 text-red-600 border border-red-200' }
  } else if (diffDays <= (inv.expiryWarningDays || 30)) {
    return { label: `Sắp hết hạn (${diffDays} ngày)`, class: 'bg-amber-100 text-amber-700 border border-amber-200' }
  } else {
    return { label: 'Bình thường', class: 'bg-emerald-100 text-emerald-700 border border-emerald-200' }
  }
}

// Update Expiry Warning Days
async function updateExpiryWarning(inv: any) {
  if (!inv || !inv.hasExpiry) return;
  const days = Number(inv.expiryWarningDays);
  if (!days || days <= 0) {
    toast.error('Số ngày cảnh báo phải lớn hơn 0');
    return;
  }
  
  try {
    const res = await api.patch(`/api/inventories/${inv.id}/expiry-warning`, {
      expiryWarningDays: days
    });
    if (res.ok) {
      toast.success('Cập nhật số ngày cảnh báo thành công!');
      await loadData();
      // Update local selectedInv reference to reflect changes without closing modal
      const updatedInv = computedInventories.value.find(i => i.id === inv.id);
      if (updatedInv) {
        selectedInv.value = updatedInv;
      }
    } else {
      const text = await res.text();
      let errMessage = 'Lỗi khi cập nhật cảnh báo (Empty Response)';
      if (text) {
        try {
          const err = JSON.parse(text);
          errMessage = err.message || errMessage;
        } catch (e) {
          errMessage = `Lỗi server: HTTP ${res.status}`;
        }
      }
      toast.error(errMessage);
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message);
  }
}

// Formatting helpers
function formatVND(val: number) {
  return new Intl.NumberFormat('vi-VN').format(val) + 'đ'
}

function formatDate(dateStr: string) {
  if (!dateStr || dateStr.startsWith('1970-01-01')) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch {
    return dateStr
  }
}

function formatDateTime(dateTimeStr: string) {
  if (!dateTimeStr) return '-'
  try {
    const d = new Date(dateTimeStr)
    return d.toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch {
    return dateTimeStr
  }
}

// Export Excel Report
function exportExcel() {
  let targetBranchId: number | string = '';
  if (activeTab.value === 'head') {
    targetBranchId = headBranch.value ? headBranch.value.id : 1;
  } else {
    targetBranchId = selectedSubBranchId.value || user.value?.branchId;
  }
  if (!targetBranchId) {
    toast.error('Vui lòng chọn chi nhánh để xuất báo cáo');
    return;
  }
  window.open(`/api/reports/inventory/excel?branchId=${targetBranchId}`, '_blank');
}
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto font-['Inter',sans-serif]">
    
    <!-- Title & Controls -->
    <div :class="['flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6', isInitialLoad ? 'header-slide-down' : '']">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0 flex items-center gap-3">
          Quản lý Tồn kho
          <!-- Tooltip Legend -->
          <div class="relative group/legend inline-flex items-center">
            <i class="fas fa-question-circle text-[#8094ae] text-lg cursor-pointer hover:text-[#4361ee] transition-colors"></i>
            
            <!-- Legend Popup -->
            <div class="absolute left-0 sm:left-1/2 sm:-translate-x-1/2 top-full mt-3 w-64 bg-white rounded-xl shadow-[0_10px_40px_rgba(0,0,0,0.12)] border border-[#e2e8f0] p-4 opacity-0 translate-y-2 invisible group-hover/legend:opacity-100 group-hover/legend:translate-y-0 group-hover/legend:visible transition-all duration-300 z-50 pointer-events-none">
              <div class="absolute -top-2 left-4 sm:left-1/2 sm:-translate-x-1/2 w-4 h-4 bg-white border-t border-l border-[#e2e8f0] rotate-45"></div>
              
              <div class="relative z-10 space-y-3">
                <div class="text-[10px] font-extrabold text-[#8094ae] uppercase tracking-wider border-b border-slate-100 pb-2">Ý NGHĨA ĐÈN CẢNH BÁO</div>
                
                <div class="flex items-start gap-3">
                  <span class="w-2.5 h-2.5 rounded-full bg-red-500 mt-1 flex-shrink-0 shadow-sm shadow-red-200"></span>
                  <div>
                    <div class="text-xs font-bold text-[#ea4f52]">Lô hàng đã hết hạn</div>
                    <div class="text-[10px] text-slate-500 leading-tight mt-0.5">Không thể bán. Vượt quá ngày Hạn sử dụng.</div>
                  </div>
                </div>
                
                <div class="flex items-start gap-3">
                  <span class="w-2.5 h-2.5 rounded-full bg-orange-500 mt-1 flex-shrink-0 shadow-sm shadow-orange-200"></span>
                  <div>
                    <div class="text-xs font-bold text-orange-600">Sắp hết hạn sử dụng</div>
                    <div class="text-[10px] text-slate-500 leading-tight mt-0.5">Nằm trong khung số ngày cảnh báo. Cần xả hàng gấp.</div>
                  </div>
                </div>
                
                <div class="flex items-start gap-3">
                  <span class="w-2.5 h-2.5 rounded-full bg-yellow-500 mt-1 flex-shrink-0 shadow-sm shadow-yellow-200"></span>
                  <div>
                    <div class="text-xs font-bold text-yellow-600">Tồn kho thấp</div>
                    <div class="text-[10px] text-slate-500 leading-tight mt-0.5">Tổng số lượng nhỏ hơn hoặc bằng ngưỡng thiết lập. Cần nhập thêm.</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </h2>
        <p class="text-[#8094ae] text-sm mt-1">
          Theo dõi hàng hóa hiện có tại các chi nhánh và Kho Tổng (kích đúp dòng để xem chi tiết lô hàng)
        </p>
      </div>

      <!-- Controls -->
      <div class="flex items-center gap-3">
        <!-- Sub-branch filter (Only active on 'sub' tab, and for Admin or Manager/Staff of sub-branch) -->
        <div v-if="activeTab === 'sub'" class="flex items-center gap-2">
          <select 
            v-if="isAdmin"
            v-model="selectedSubBranchId" 
            class="h-[42px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] shadow-sm font-semibold"
          >
            <option value="">Tất cả Chi nhánh con</option>
            <option v-for="b in subBranches" :key="b.id" :value="b.id">
              {{ b.name }}
            </option>
          </select>
          
          <div v-else class="h-[42px] px-4 bg-[#eef2ff] border border-[#dbeafe] rounded-xl text-sm flex items-center gap-2 font-bold text-[#4361ee] shadow-sm">
            <i class="fas fa-store"></i> {{ user.branchName }}
          </div>
        </div>

        <div v-if="activeTab === 'head' && !isAdmin" class="h-[42px] px-4 bg-[#eef2ff] border border-[#dbeafe] rounded-xl text-sm flex items-center gap-2 font-bold text-[#4361ee] shadow-sm">
          <i class="fas fa-crown text-[#f59e0b]"></i> {{ headBranch?.name || 'Kho Tổng' }}
        </div>

        <!-- Configure threshold -->
        <button 
          @click="openConfigModal" 
          class="h-[42px] bg-white border border-[#e2e8f0] text-[#364a63] hover:bg-[#f8f9fa] px-4 rounded-xl text-sm font-bold shadow-sm transition-all flex items-center gap-2"
        >
          <i class="fas fa-cog text-slate-500"></i> Ngưỡng cảnh báo ({{ activeThreshold }})
        </button>

        <!-- Xuất Excel -->
        <button 
          v-if="user?.role !== 'STAFF'"
          @click="exportExcel"
          class="h-[42px] bg-white border border-[#e2e8f0] text-[#107c41] hover:bg-green-50 px-4 rounded-xl text-sm font-bold shadow-sm transition-all flex items-center gap-2"
        >
          <i class="fas fa-file-excel"></i> Xuất Excel
        </button>


      </div>
    </div>

    <!-- TABS HEAD-SUB SEPARATION (Only visible for Admin, as Admin can view both) -->
    <div v-if="isAdmin" class="flex border-b border-[#e2e8f0] mb-6">
      <button 
        @click="activeTab = 'head'"
        :class="['px-6 py-3 font-bold text-sm transition-all border-b-2 -mb-[2px] cursor-pointer flex items-center gap-2',
                 activeTab === 'head' ? 'border-[#4361ee] text-[#4361ee]' : 'border-transparent text-[#8094ae] hover:text-[#364a63]']"
      >
        <i class="fas fa-crown text-[#f59e0b]"></i> KHO TỔNG
      </button>
      <button 
        @click="activeTab = 'sub'"
        :class="['px-6 py-3 font-bold text-sm transition-all border-b-2 -mb-[2px] cursor-pointer flex items-center gap-2',
                 activeTab === 'sub' ? 'border-[#4361ee] text-[#4361ee]' : 'border-transparent text-[#8094ae] hover:text-[#364a63]']"
      >
        <i class="fas fa-store"></i> CHI NHÁNH CON
      </button>
    </div>

    <!-- Stats Cards -->
    <div :key="`stats-${activeTab}-${selectedSubBranchId}`" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
      <!-- Distinct products -->
      <div 
        @click="clearFilters"
        :class="['bg-white rounded-2xl p-6 border border-[#f1f5f9] hover:border-blue-300 hover:shadow-md cursor-pointer transition-all flex items-center justify-between gap-4 group relative', isInitialLoad ? 'stat-card-3d' : '']"
        :style="isInitialLoad ? { '--card-delay': '30ms' } : {}"
      >
        <!-- Custom Tooltip Bubble -->
        <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-3 px-3.5 py-2 bg-white border border-blue-200 text-[#4361ee] text-xs font-bold rounded-xl shadow-[0_8px_24px_rgba(67,97,238,0.12)] opacity-0 group-hover:opacity-100 scale-90 group-hover:scale-100 transition-all duration-200 pointer-events-none z-50 whitespace-nowrap flex flex-col items-center">
          <span>Xem tất cả {{ totalDistinctProducts }} mặt hàng</span>
          <div class="w-2.5 h-2.5 bg-white border-r border-b border-blue-200 rotate-45 absolute -bottom-[5.5px] left-1/2 -translate-x-1/2"></div>
        </div>

        <div class="min-w-0">
          <div class="text-[0.75rem] font-bold text-[#8094ae] uppercase tracking-wider mb-1 truncate">Mặt hàng tồn kho</div>
          <div class="text-2xl font-extrabold text-[#364a63] group-hover:text-[#4361ee] transition-colors truncate">{{ totalDistinctProducts }}</div>
        </div>
        <div class="flex-shrink-0 w-12 h-12 rounded-xl bg-blue-50 text-[#4361ee] flex items-center justify-center text-xl shadow-sm group-hover:scale-110 transition-transform">
          <i class="fas fa-box"></i>
        </div>
      </div>

      <!-- Total value -->
      <div 
        :class="['bg-white rounded-2xl p-6 border border-[#f1f5f9] shadow-sm flex items-center justify-between gap-4 group relative', isInitialLoad ? 'stat-card-3d' : '']"
        :style="isInitialLoad ? { '--card-delay': '80ms' } : {}"
      >
        <!-- Custom Tooltip Bubble -->
        <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-3 px-3.5 py-2 bg-white border border-emerald-200 text-[#05b171] text-xs font-bold rounded-xl shadow-[0_8px_24px_rgba(5,177,113,0.12)] opacity-0 group-hover:opacity-100 scale-90 group-hover:scale-100 transition-all duration-200 pointer-events-none z-50 whitespace-nowrap flex flex-col items-center">
          <span class="font-mono text-sm">{{ formatVND(totalInventoryValue) }}</span>
          <div class="w-2.5 h-2.5 bg-white border-r border-b border-emerald-200 rotate-45 absolute -bottom-[5.5px] left-1/2 -translate-x-1/2"></div>
        </div>

        <div class="min-w-0">
          <div class="text-[0.75rem] font-bold text-[#8094ae] uppercase tracking-wider mb-1 truncate">Tổng giá trị kho</div>
          <div class="text-xl lg:text-2xl font-extrabold text-[#05b171] truncate">{{ formatVND(totalInventoryValue) }}</div>
        </div>
        <div class="flex-shrink-0 w-12 h-12 rounded-xl bg-emerald-50 text-[#05b171] flex items-center justify-center text-xl shadow-sm">
          <i class="fas fa-wallet"></i>
        </div>
      </div>

      <!-- Low stock warnings (YELLOW) -->
      <div 
        @click="toggleOnlyWarning"
        :class="[
          'bg-white rounded-2xl p-6 border transition-all flex items-center justify-between gap-4 cursor-pointer select-none group relative', 
          onlyWarning 
            ? 'border-yellow-400 ring-2 ring-yellow-400/20 shadow-md' 
            : 'border-[#f1f5f9] hover:border-yellow-300 hover:shadow-md',
          isInitialLoad ? 'stat-card-3d' : ''
        ]"
        :style="isInitialLoad ? { '--card-delay': '130ms' } : {}"
      >
        <!-- Custom Tooltip Bubble -->
        <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-3 px-3.5 py-2 bg-white border border-yellow-200 text-[#d9a80c] text-xs font-bold rounded-xl shadow-[0_8px_24px_rgba(217,168,12,0.12)] opacity-0 group-hover:opacity-100 scale-90 group-hover:scale-100 transition-all duration-200 pointer-events-none z-50 whitespace-nowrap flex flex-col items-center">
          <span>{{ onlyWarning ? 'Nhấp để bỏ lọc cảnh báo' : 'Lọc danh sách cảnh báo tồn thấp' }}</span>
          <div class="w-2.5 h-2.5 bg-white border-r border-b border-yellow-200 rotate-45 absolute -bottom-[5.5px] left-1/2 -translate-x-1/2"></div>
        </div>

        <div class="min-w-0">
          <div class="text-[0.75rem] font-bold text-[#8094ae] uppercase tracking-wider mb-1 truncate">Cảnh báo tồn thấp</div>
          <div :class="['text-2xl font-extrabold transition-colors truncate', totalWarningsCount > 0 || onlyWarning ? 'text-yellow-500' : 'text-[#364a63]']">
            {{ totalWarningsCount }}
          </div>
        </div>
        <div 
          :class="['flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center text-xl shadow-sm transition-transform group-hover:scale-110', 
                   totalWarningsCount > 0 || onlyWarning ? 'bg-yellow-50 text-yellow-500' : 'bg-slate-50 text-[#8094ae]']"
        >
          <i :class="['fas fa-exclamation-triangle', { 'animate-pulse': totalWarningsCount > 0 }]"></i>
        </div>
      </div>

      <!-- Expired lots (RED) -->
      <div 
        @click="toggleOnlyExpired"
        :class="[
          'bg-white rounded-2xl p-6 border transition-all flex items-center justify-between gap-4 cursor-pointer select-none group relative', 
          onlyExpired 
            ? 'border-[#ea4f52] ring-2 ring-[#ea4f52]/20 shadow-md' 
            : 'border-[#f1f5f9] hover:border-red-300 hover:shadow-md',
          isInitialLoad ? 'stat-card-3d' : ''
        ]"
        :style="isInitialLoad ? { '--card-delay': '180ms' } : {}"
      >
        <!-- Custom Tooltip Bubble -->
        <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-3 px-3.5 py-2 bg-white border border-red-200 text-[#ea4f52] text-xs font-bold rounded-xl shadow-[0_8px_24px_rgba(234,79,82,0.12)] opacity-0 group-hover:opacity-100 scale-90 group-hover:scale-100 transition-all duration-200 pointer-events-none z-50 whitespace-nowrap flex flex-col items-center">
          <span>{{ onlyExpired ? 'Nhấp để bỏ lọc hết hạn' : 'Lọc danh sách lô hàng hết hạn' }}</span>
          <div class="w-2.5 h-2.5 bg-white border-r border-b border-red-200 rotate-45 absolute -bottom-[5.5px] left-1/2 -translate-x-1/2"></div>
        </div>

        <div class="min-w-0">
          <div class="text-[0.75rem] font-bold text-[#8094ae] uppercase tracking-wider mb-1 truncate">Lô hàng hết hạn</div>
          <div :class="['text-2xl font-extrabold transition-colors truncate', totalExpiredCount > 0 || onlyExpired ? 'text-[#ea4f52]' : 'text-[#364a63]']">
            {{ totalExpiredCount }}
          </div>
        </div>
        <div 
          :class="['flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center text-xl shadow-sm transition-transform group-hover:scale-110', 
                   totalExpiredCount > 0 || onlyExpired ? 'bg-red-50 text-[#ea4f52]' : 'bg-slate-50 text-[#8094ae]']"
        >
          <i :class="['fas fa-hourglass-end', { 'animate-pulse': totalExpiredCount > 0 }]"></i>
        </div>
      </div>
    </div>

    <!-- INVENTORY TAB (Combined Toolbar & Table) -->
    <div :key="`table-${activeTab}-${selectedSubBranchId}`" :class="['bg-indigo-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden', isInitialLoad ? 'table-card-conveyor' : '']">
      
      <!-- Search & Filters Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] flex flex-col lg:flex-row items-center justify-between gap-4 bg-[#f8f9fa]/50">
      <div class="flex flex-col sm:flex-row items-center gap-3 w-full lg:max-w-2xl">
        <div class="relative w-full sm:max-w-md">
          <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="Tìm theo tên sản phẩm hoặc lô sản xuất..." 
            class="w-full h-[42px] pl-11 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] shadow-sm font-semibold"
          />
        </div>
        <select 
          v-model="selectedCategoryId"
          class="w-full sm:w-[200px] h-[42px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] shadow-sm font-semibold"
        >
          <option value="">Tất cả danh mục</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>
      </div>

      <div class="flex flex-wrap items-center gap-6 w-full lg:w-auto justify-end">
        <label class="flex items-center gap-2 cursor-pointer select-none font-semibold text-sm text-[#364a63]">
          <input 
            v-model="onlyWarning" 
            type="checkbox" 
            class="w-5 h-5 accent-[#4361ee] cursor-pointer rounded-md border-slate-300"
            @change="onWarningChange"
          />
          Chỉ xem cảnh báo tồn thấp
        </label>

        <label class="flex items-center gap-2 cursor-pointer select-none font-semibold text-sm text-[#364a63]">
          <input 
            v-model="onlyExpired" 
            type="checkbox" 
            class="w-5 h-5 accent-[#4361ee] cursor-pointer rounded-md border-slate-300"
            @change="onExpiredChange"
          />
          Chỉ xem lô hết hạn
        </label>
      </div>
      </div>

      <!-- Inventory Table -->
      <div class="bg-white overflow-hidden">
        <div v-if="loading" class="text-center p-20 text-[#8094ae]">
          <i class="fas fa-spinner fa-spin text-3xl mb-4 text-[#4361ee]"></i>
          <p>Đang tải dữ liệu tồn kho...</p>
        </div>

        <div v-else-if="filteredInventories.length === 0" class="text-center p-20 text-[#8094ae]">
          <i class="fas fa-boxes text-5xl mb-4 opacity-40"></i>
          <div class="font-bold text-[#364a63]">Không tìm thấy tồn kho nào</div>
          <p class="text-sm mt-1">Không có sản phẩm nào thuộc phân loại này</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead class="bg-white">
              <tr>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Sản phẩm</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Lô sản xuất</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">{{ activeTab === 'head' ? 'Danh mục' : 'Chi nhánh' }}</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Đơn vị tính</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9] text-center">Số lượng</th>
                <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9] text-right">Giá trị tồn</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="(inv, index) in filteredInventories" 
                :key="inv.id" 
                :class="['border-b border-[#f1f5f9] hover:border-transparent hover:bg-gradient-to-r hover:from-[#4361ee]/15 hover:to-[#4cc9f0]/15 hover:shadow-sm transition-all duration-300 cursor-pointer select-none group hover:-translate-y-[1px]', isInitialLoad ? 'inventory-row-anim' : '']"
                :style="isInitialLoad ? { '--row-delay': `${260 + index * 20}ms` } : {}"
                @dblclick="openDetails(inv)"
              >
              <!-- Name -->
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center gap-2">
                  <span 
                    v-if="inv.isWarning" 
                    class="w-2.5 h-2.5 rounded-full bg-yellow-500 flex-shrink-0 animate-pulse shadow-sm shadow-yellow-200"
                    title="Tồn kho dưới ngưỡng cảnh báo"
                  ></span>
                  <span 
                    v-if="inv.isExpired" 
                    class="w-2.5 h-2.5 rounded-full bg-red-500 flex-shrink-0 animate-pulse shadow-sm shadow-red-200"
                    title="Lô hàng đã hết hạn sử dụng"
                  ></span>
                  <span 
                    v-else-if="inv.isExpiryWarning" 
                    class="w-2.5 h-2.5 rounded-full bg-orange-500 flex-shrink-0 animate-pulse shadow-sm shadow-orange-200"
                    title="Lô hàng sắp hết hạn sử dụng"
                  ></span>
                  <div class="font-bold text-[#364a63] group-hover:text-[#4361ee] transition-colors">{{ inv.productName }}</div>
                </div>
              </td>

              <!-- Lot Code -->
              <td class="p-4 font-bold text-[#364a63] text-xs font-mono first:rounded-l-xl last:rounded-r-xl"><div>{{ inv.batchCode }}</div></td>

              <!-- Branch / Category -->
              <td class="p-4 text-[#364a63] font-medium first:rounded-l-xl last:rounded-r-xl">
                <template v-if="activeTab === 'head'">
                  <span class="inline-block px-2.5 py-1 text-xs font-bold bg-slate-50 text-slate-600 rounded-lg border border-slate-100">
                    {{ inv.categoryName }}
                  </span>
                </template>
                <template v-else>
                  <div>{{ inv.branchName }}</div>
                </template>
              </td>

              <!-- Unit -->
              <td class="p-4 text-[#364a63] font-medium first:rounded-l-xl last:rounded-r-xl"><div>{{ inv.unit }}</div></td>

              <!-- Quantity -->
              <td class="p-4 text-center first:rounded-l-xl last:rounded-r-xl">
                <span 
                  class="inline-block px-3 py-1 text-xs font-extrabold rounded-full shadow-sm bg-slate-100 text-slate-700"
                >
                  {{ inv.quantity }}
                </span>
              </td>

              <!-- Total value -->
              <td class="p-4 text-right font-extrabold text-[#364a63] first:rounded-l-xl last:rounded-r-xl"><div>{{ formatVND(inv.totalValue) }}</div></td>
            </tr>
          </tbody>
        </table>
        
        <!-- Summary footer -->
        <div v-if="!loading && filteredInventories.length > 0" class="px-6 py-4 bg-[#f8f9fa] border-t border-[#f1f5f9] text-xs font-bold text-[#8094ae]">
          Tổng cộng: {{ filteredInventories.length }} dòng tồn kho
        </div>
      </div>
    </div>
    </div>

    <!-- Configure Low Stock Threshold Modal -->
    <AppModal 
      :show="showConfigModal" 
      :title="configuringBranchId === null ? 'Thiết lập ngưỡng cảnh báo toàn cục' : 'Thiết lập ngưỡng tồn kho tối thiểu'" 
      size="sm" 
      @close="showConfigModal = false"
    >
      <div class="p-6 space-y-4">
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Ngưỡng cảnh báo tồn kho thấp</label>
          <input 
            v-model="thresholdForm" 
            type="number" 
            min="0" 
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all" 
          />
          <p class="text-[11px] text-[#8094ae] mt-2 italic">
            <i class="fas fa-info-circle mr-1"></i>
            {{ configuringBranchId === null ? 'Ngưỡng này được lưu trên thiết bị của bạn khi xem tất cả chi nhánh.' : 'Sản phẩm sẽ bị báo đỏ khi số lượng nhỏ hơn hoặc bằng ngưỡng này.' }}
          </p>
        </div>

        <div class="flex gap-3 pt-4 border-t border-[#f1f5f9]">
          <button 
            class="flex-1 h-11 bg-[#f8f9fa] hover:bg-[#e2e8f0] text-[#364a63] rounded-xl text-sm font-bold transition-colors" 
            @click="showConfigModal = false"
          >
            Hủy bỏ
          </button>
          <button 
            class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all" 
            @click="saveThreshold"
          >
            Lưu cài đặt
          </button>
        </div>
      </div>
    </AppModal>


    <!-- ── INVENTORY DETAIL PANEL ── -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div 
          v-if="showDetailPanel" 
          @click="showDetailPanel = false" 
          class="fixed inset-0 bg-slate-900/20 backdrop-blur-[2px] z-[100]"
        ></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div 
          v-if="showDetailPanel && selectedInv" 
          class="fixed inset-y-0 right-0 z-[101] w-[450px] bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] flex flex-col border-l border-[#e2e8f0]"
        >
          <!-- Header -->
          <div class="px-6 py-5 border-b border-[#f1f5f9] flex justify-between items-center bg-white">
            <h3 class="font-bold text-[#364a63] text-lg flex items-center gap-2">
              <i class="fas fa-info-circle text-[#4361ee]"></i>
              Chi tiết lô tồn kho
            </h3>
            <button 
              @click="showDetailPanel = false" 
              class="text-[#8094ae] hover:text-[#ea4f52] transition-colors w-8 h-8 flex items-center justify-center rounded-full hover:bg-red-50"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- Body -->
          <div class="p-6 flex-1 overflow-y-auto space-y-6 custom-scrollbar text-sm">
            <!-- Header Info -->
            <div class="bg-slate-50 rounded-2xl p-5 border border-slate-100 space-y-3">
              <div class="text-xs text-[#8094ae] font-bold uppercase tracking-wider">Thông tin sản phẩm</div>
              <div>
                <h4 class="font-extrabold text-[#364a63] text-base mb-1">{{ selectedInv.productName }}</h4>
                <div class="font-mono text-xs font-semibold text-[#8094ae] flex items-center gap-2">
                  <span>SKU: {{ selectedInv.productSku }}</span>
                  <span>•</span>
                  <span>ĐVT: {{ selectedInv.unit }}</span>
                </div>
              </div>
            </div>

            <!-- Basic lot metrics -->
            <div class="grid grid-cols-2 gap-4">
              <div class="bg-slate-50 rounded-xl p-4 border border-slate-100">
                <div class="text-[10px] text-[#8094ae] font-bold uppercase tracking-wider mb-1">Số lượng</div>
                <div class="text-xl font-extrabold text-[#364a63]">
                  {{ selectedInv.quantity }} <span class="text-xs text-slate-500 font-normal">{{ selectedInv.unit }}</span>
                </div>
              </div>
              <div class="bg-slate-50 rounded-xl p-4 border border-slate-100">
                <div class="text-[10px] text-[#8094ae] font-bold uppercase tracking-wider mb-1">Giá trị tồn</div>
                <div class="text-xl font-extrabold text-emerald-600">{{ formatVND(selectedInv.totalValue) }}</div>
              </div>
            </div>

            <!-- Details list -->
            <div class="space-y-4">
              <div class="text-xs text-[#8094ae] font-bold uppercase tracking-wider border-b border-[#f1f5f9] pb-2">Chi tiết vị trí & Hạn dùng</div>
              
              <!-- Lot Code -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Lô sản xuất</span>
                <span class="font-bold text-[#364a63] font-mono text-xs">{{ selectedInv.batchCode }}</span>
              </div>

              <!-- Branch -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Chi nhánh</span>
                <span class="font-bold text-[#364a63]"><i class="fas fa-building mr-1.5 text-slate-400"></i>{{ selectedInv.branchName }}</span>
              </div>

              <!-- Low Stock Status -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Ngưỡng tồn thấp</span>
                <span class="font-bold flex items-center gap-1.5">
                  <span 
                    :class="['px-2 py-0.5 text-xs font-bold rounded-md', selectedInv.isWarning ? 'bg-red-100 text-red-600' : 'bg-slate-100 text-slate-600']"
                  >
                    {{ selectedInv.isWarning ? 'Dưới ngưỡng' : 'Đạt chuẩn' }}
                  </span>
                  <span class="text-xs text-slate-500 font-normal">(Ngưỡng: {{ selectedInv.threshold }})</span>
                </span>
              </div>

              <!-- Lot Expiry Status -->
              <div class="flex justify-between py-1 items-center">
                <span class="text-[#8094ae] font-semibold">Trạng thái hạn dùng</span>
                <span :class="['px-2 py-0.5 text-xs font-bold rounded-md', getLotStatus(selectedInv).class]">
                  {{ getLotStatus(selectedInv).label }}
                </span>
              </div>

              <!-- Expiry Warning Days -->
              <div v-if="selectedInv.hasExpiry" class="flex justify-between py-1 items-center group/warning">
                <span class="text-[#8094ae] font-semibold flex items-center gap-1">
                  Cảnh báo hết hạn trước
                  <i v-if="user?.role !== 'STAFF'" class="fas fa-pen text-[10px] text-slate-300 group-hover/warning:text-[#4361ee] transition-colors"></i>
                </span>
                <div class="flex items-center gap-1.5">
                  <input 
                    v-model="selectedInv.expiryWarningDays" 
                    type="number" 
                    min="1"
                    :disabled="user?.role === 'STAFF'"
                    @blur="updateExpiryWarning(selectedInv)"
                    @keyup.enter="($event.target as any).blur()"
                    class="w-14 h-7 px-1 text-right border border-[#e2e8f0] bg-[#f8f9fa] hover:bg-white focus:bg-white rounded text-sm font-bold text-[#4361ee] focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all shadow-sm disabled:opacity-60 disabled:cursor-not-allowed"
                  />
                  <span class="text-xs font-bold text-[#364a63]">ngày</span>
                </div>
              </div>

              <!-- MFG Date -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Ngày sản xuất (NSX)</span>
                <span class="font-bold text-[#364a63]">{{ formatDate(selectedInv.manufacturingDate) }}</span>
              </div>

              <!-- Expiration Date -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Hạn sử dụng (HSD)</span>
                <span class="font-bold text-[#364a63]">{{ selectedInv.hasExpiry ? formatDate(selectedInv.expirationDate) : '-' }}</span>
              </div>

              <!-- Unit Price -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Đơn giá bán</span>
                <span class="font-bold text-[#364a63]">{{ formatVND(selectedInv.price) }}</span>
              </div>

              <!-- Unit Import Price -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Đơn giá nhập</span>
                <span class="font-bold text-[#364a63]">{{ formatVND(selectedInv.importPrice) }}</span>
              </div>

              <!-- Total Import Value -->
              <div class="flex justify-between py-1 border-t border-dashed border-slate-100 pt-2">
                <span class="text-[#8094ae] font-semibold">Tổng giá trị nhập</span>
                <span class="font-bold text-amber-600">{{ formatVND(selectedInv.totalImportValue) }}</span>
              </div>
              <!-- Last Updated -->
              <div class="flex justify-between py-1">
                <span class="text-[#8094ae] font-semibold">Lần cuối cập nhật</span>
                <span class="font-mono text-xs font-bold text-[#364a63]">{{ formatDateTime(selectedInv.lastUpdated) }}</span>
              </div>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="p-6 border-t border-[#f1f5f9] bg-[#f8fafc] flex gap-3">
            <button 
              class="flex-1 h-11 bg-white border border-[#e2e8f0] hover:bg-[#f8f9fa] text-[#364a63] rounded-xl text-sm font-bold transition-colors shadow-sm" 
              @click="showDetailPanel = false"
            >
              Đóng
            </button>
          </div>
        </div>
      </Transition>


    </Teleport>
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.fa-spinner { animation: spin 0.8s linear infinite; }

.slide-panel-enter-active, .slide-panel-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-panel-enter-from, .slide-panel-leave-to {
  transform: translateX(100%);
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

/* ── 3D Isometric Box Stack Animations ── */
@keyframes slideDownHeader {
  0% { transform: translateY(-30px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}
.header-slide-down {
  animation: slideDownHeader 0.45s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes boxStack3D {
  0% {
    transform: perspective(1000px) rotateX(20deg) rotateY(-15deg) translate3d(0, -60px, -150px);
    opacity: 0;
  }
  100% {
    transform: perspective(1000px) rotateX(0deg) rotateY(0deg) translate3d(0, 0, 0);
    opacity: 1;
  }
}
.stat-card-3d {
  animation: boxStack3D 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  animation-delay: var(--card-delay, 0ms);
  will-change: transform, opacity;
}

@keyframes conveyorSlide {
  0% {
    transform: perspective(1000px) translate3d(0, 40px, -200px) scale(0.92);
    opacity: 0;
  }
  100% {
    transform: perspective(1000px) translate3d(0, 0, 0) scale(1);
    opacity: 1;
  }
}
.table-card-conveyor {
  animation: conveyorSlide 0.45s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 160ms;
  will-change: transform, opacity;
}

@keyframes rowSlideUp {
  0% {
    transform: translateY(20px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}
.inventory-row-anim {
  animation: rowSlideUp 0.33s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: var(--row-delay, 0ms);
  will-change: transform, opacity;
}
</style>
