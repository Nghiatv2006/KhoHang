<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

const loading = ref(true)
const stocktakes = ref<any[]>([])
const searchKeyword = ref('')
const selectedStatus = ref('')

const filterTimeRange = ref('all')
const filterFrom = ref('')
const filterTo = ref('')

watch(filterTimeRange, (val) => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().substring(0, 10)
  
  if (val === 'all') {
    filterFrom.value = ''
    filterTo.value = ''
  } else if (val === 'today') {
    filterFrom.value = fmt(today)
    filterTo.value = fmt(today)
  } else if (val === 'week') {
    const weekAgo = new Date(today)
    weekAgo.setDate(weekAgo.getDate() - 7)
    filterFrom.value = fmt(weekAgo)
    filterTo.value = fmt(today)
  } else if (val === 'month') {
    const monthAgo = new Date(today)
    monthAgo.setMonth(monthAgo.getMonth() - 1)
    filterFrom.value = fmt(monthAgo)
    filterTo.value = fmt(today)
  }
})

watch([filterFrom, filterTo], () => {
  if (filterFrom.value && filterTo.value && filterFrom.value > filterTo.value) {
    toast.error('Từ ngày không thể lớn hơn Đến ngày!')
    filterTo.value = filterFrom.value
  }
})

// Selected stocktake details
const selectedStocktake = ref<any>(null)
const showDetailDrawer = ref(false)
const savingDraft = ref(false)

// Confirm dialogs
const showCompleteConfirm = ref(false)
const showCancelConfirm = ref(false)
const actionLoading = ref(false)

// Linked receipt modal
const showReceiptModal = ref(false)
const selectedReceipt = ref<any>(null)
const receiptLoading = ref(false)

// Fetch all stocktakes
async function loadStocktakes() {
  loading.value = true
  try {
    const res = await api.get('/api/stocktakes')
    if (res.ok) {
      stocktakes.value = await res.json()
    } else {
      toast.error('Không thể tải danh sách phiên kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    loading.value = false
  }
}

// Create new stocktake
async function createStocktake() {
  try {
    const res = await api.post('/api/stocktakes', { notes: 'Phiên kiểm kê mới khởi tạo' })
    if (res.ok) {
      toast.success('Khởi tạo phiên kiểm kê thành công!')
      const newSt = await res.json()
      await loadStocktakes()
      openDetail(newSt)
    } else {
      const err = await res.text()
      toast.error(err || 'Khởi tạo phiên kiểm kê thất bại.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  }
}

// Open detail panel
async function openDetail(st: any) {
  loadingDetail(st.id)
}

async function loadingDetail(id: number) {
  try {
    const res = await api.get(`/api/stocktakes/${id}`)
    if (res.ok) {
      selectedStocktake.value = await res.json()
      showDetailDrawer.value = true
    } else {
      toast.error('Không thể tải chi tiết phiên kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  }
}

// Save draft quantities
async function saveDraft() {
  if (!selectedStocktake.value) return
  savingDraft.value = true
  try {
    const payload = {
      notes: selectedStocktake.value.notes,
      details: selectedStocktake.value.details.map((d: any) => ({
        id: d.id,
        actualQuantity: Number(d.actualQuantity)
      }))
    }
    const res = await api.put(`/api/stocktakes/${selectedStocktake.value.id}`, payload)
    if (res.ok) {
      toast.success('Lưu số liệu kiểm kê thành công!')
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
    } else {
      const err = await res.text()
      toast.error(err || 'Không thể lưu bản nháp.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    savingDraft.value = false
  }
}

// Complete/Approve Stocktake
async function completeStocktake() {
  if (!selectedStocktake.value) return
  showCompleteConfirm.value = false
  actionLoading.value = true
  try {
    // Save draft first to make sure current actual quantities are sent
    const payload = {
      notes: selectedStocktake.value.notes,
      details: selectedStocktake.value.details.map((d: any) => ({
        id: d.id,
        actualQuantity: Number(d.actualQuantity)
      }))
    }
    const saveRes = await api.put(`/api/stocktakes/${selectedStocktake.value.id}`, payload)
    if (!saveRes.ok) {
      const err = await saveRes.text()
      toast.error(err || 'Lưu số liệu nháp thất bại trước khi hoàn tất.')
      actionLoading.value = false
      return
    }

    const res = await api.patch(`/api/stocktakes/${selectedStocktake.value.id}/complete`, {})
    if (res.ok) {
      toast.success('Hoàn tất kiểm kê và cập nhật tồn kho thành công!')
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
    } else {
      const err = await res.text()
      toast.error(err || 'Không thể duyệt hoàn tất phiên kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}

// Cancel Stocktake
async function cancelStocktake() {
  if (!selectedStocktake.value) return
  showCancelConfirm.value = false
  actionLoading.value = true
  try {
    const res = await api.patch(`/api/stocktakes/${selectedStocktake.value.id}/cancel`, {})
    if (res.ok) {
      toast.success('Đã hủy bỏ phiên kiểm kê.')
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
    } else {
      const err = await res.text()
      toast.error(err || 'Không thể hủy phiên kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}

// View linked adjustment receipt
async function viewReceipt(receiptId: number) {
  receiptLoading.value = true
  showReceiptModal.value = true
  selectedReceipt.value = null
  try {
    const res = await api.get(`/api/receipts/${receiptId}`)
    if (res.ok) {
      selectedReceipt.value = await res.json()
    } else {
      toast.error('Không thể tải chi tiết phiếu điều chỉnh.')
      showReceiptModal.value = false
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
    showReceiptModal.value = false
  } finally {
    receiptLoading.value = false
  }
}

// Filtered stocktakes
const filteredStocktakes = computed(() => {
  return stocktakes.value.filter(st => {
    const codeMatch = st.code.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const noteMatch = (st.notes || '').toLowerCase().includes(searchKeyword.value.toLowerCase())
    const statusMatch = selectedStatus.value ? st.status === selectedStatus.value : true
    
    let timeMatch = true
    if (filterFrom.value || filterTo.value) {
      if (!st.createdAt) {
        timeMatch = false
      } else {
        const itemDateStr = st.createdAt.substring(0, 10)
        if (filterFrom.value && itemDateStr < filterFrom.value) timeMatch = false
        if (filterTo.value && itemDateStr > filterTo.value) timeMatch = false
      }
    }
    
    return (codeMatch || noteMatch) && statusMatch && timeMatch
  }).sort((a, b) => {
    const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return timeB - timeA
  })
})

const adjustmentReceipts = computed(() => {
  if (!selectedStocktake.value || !selectedStocktake.value.details) return []
  const map = new Map()
  for (const d of selectedStocktake.value.details) {
    if (d.adjustmentReceiptId) {
      map.set(d.adjustmentReceiptId, {
        id: d.adjustmentReceiptId,
        code: d.adjustmentReceiptCode,
        type: d.adjustmentReceiptCode?.startsWith('AI') ? 'ADJUST_IN' : 'ADJUST_OUT'
      })
    }
  }
  return Array.from(map.values())
})

// Formatting Helpers
function formatVND(val: number) {
  return new Intl.NumberFormat('vi-VN').format(val) + ' đ'
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

function getStatusBadgeClass(status: string) {
  switch (status) {
    case 'DRAFT': return 'bg-slate-100 text-slate-700 border-slate-200'
    case 'COMPLETED': return 'bg-emerald-100 text-emerald-700 border-emerald-200'
    case 'CANCELLED': return 'bg-red-100 text-red-700 border-red-200'
    default: return 'bg-slate-100 text-slate-600'
  }
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'DRAFT': return 'Lưu nháp'
    case 'COMPLETED': return 'Đã hoàn tất'
    case 'CANCELLED': return 'Đã hủy'
    default: return status
  }
}

onMounted(loadStocktakes)
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto font-['Inter',sans-serif]">
    
    <!-- PAGE HEADER -->
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0">Kiểm kê kho</h2>
        <p class="text-[#8094ae] text-sm mt-1">Định kỳ đối chiếu số liệu tồn kho phần mềm và hàng thực tế</p>
      </div>
      <div>
        <button
          @click="createStocktake"
          class="h-11 px-6 bg-gradient-to-r from-[#4361ee] to-[#4cc9f0] hover:from-[#3a0ca3] hover:to-[#4361ee] text-white rounded-xl font-bold transition-all shadow-md flex items-center gap-2 hover:-translate-y-0.5"
        >
          <i class="fas fa-plus"></i>
          Khởi tạo kiểm kê
        </button>
      </div>
    </div>

    <!-- FILTER & SEARCH BAR -->
    <div class="bg-white rounded-[16px] p-6 shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] space-y-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="relative flex-1">
          <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Tìm theo mã kiểm kê hoặc ghi chú..."
            class="w-full h-11 pl-11 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]"
          />
        </div>
        <div class="w-full md:w-[200px]">
          <select
            v-model="selectedStatus"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] font-medium"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="DRAFT">Lưu nháp</option>
            <option value="COMPLETED">Đã hoàn tất</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>
      
      <!-- Date filters row -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 border-t border-slate-100">
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Thời gian</label>
          <select
            v-model="filterTimeRange"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] font-medium"
          >
            <option value="all">Tất cả thời gian</option>
            <option value="today">Hôm nay</option>
            <option value="week">7 ngày qua</option>
            <option value="month">30 ngày qua</option>
            <option value="custom">Tùy chọn ngày...</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Từ ngày</label>
          <input
            v-model="filterFrom"
            type="date"
            :disabled="filterTimeRange !== 'custom'"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed"
          />
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Đến ngày</label>
          <input
            v-model="filterTo"
            type="date"
            :disabled="filterTimeRange !== 'custom'"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed"
          />
        </div>
      </div>
    </div>

    <!-- STOCKTAKE TABLE -->
    <div class="bg-white rounded-[16px] shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] overflow-hidden">
      <div v-if="loading" class="flex flex-col items-center justify-center py-16">
        <i class="fas fa-spinner fa-spin text-3xl text-[#4361ee] mb-4"></i>
        <span class="text-sm text-[#8094ae]">Đang tải danh sách...</span>
      </div>

      <div v-else-if="filteredStocktakes.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
        <div class="w-16 h-16 rounded-full bg-[#f8f9fa] flex items-center justify-center mb-4 text-[#8094ae]">
          <i class="fas fa-clipboard-list text-2xl"></i>
        </div>
        <h4 class="text-base font-bold text-[#364a63]">Không tìm thấy dữ liệu</h4>
        <p class="text-[#8094ae] text-xs max-w-xs mt-1">Chưa có phiên kiểm kê nào được lập hoặc không khớp bộ lọc.</p>
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead>
            <tr class="border-b border-[#f1f5f9]">
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider pl-8">Mã kiểm kê</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Chi nhánh</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Người tạo</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Ngày tạo</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Ghi chú</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Trạng thái</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider pr-8 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="st in filteredStocktakes"
              :key="st.id"
              class="border-b border-[#f1f5f9] hover:border-transparent hover:bg-gradient-to-r hover:from-[#4361ee]/15 hover:to-[#4cc9f0]/15 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]"
              @dblclick="openDetail(st)"
            >
              <td class="p-4 pl-8 font-mono font-bold text-[#4361ee]">{{ st.code }}</td>
              <td class="p-4 text-sm font-semibold text-[#364a63]">{{ st.branchName }}</td>
              <td class="p-4 text-sm text-[#364a63]">{{ st.createdByName }}</td>
              <td class="p-4 text-sm text-[#8094ae] font-mono">{{ formatDateTime(st.createdAt) }}</td>
              <td class="p-4 text-sm text-slate-500 max-w-[200px] truncate" :title="st.notes">{{ st.notes || '-' }}</td>
              <td class="p-4">
                <span :class="['inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold border', getStatusBadgeClass(st.status)]">
                  {{ getStatusLabel(st.status) }}
                </span>
              </td>
              <td class="p-4 pr-8 text-right">
                <button
                  @click="openDetail(st)"
                  class="h-8 px-4 bg-slate-100 hover:bg-[#4361ee] hover:text-white text-slate-700 rounded-lg text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 inline-flex"
                >
                  <i class="fas" :class="st.status === 'DRAFT' ? 'fa-pen' : 'fa-eye'"></i>
                  {{ st.status === 'DRAFT' ? 'Kiểm đếm' : 'Xem chi tiết' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- DETAIL DRAWER (SLIDING PANEL) -->
    <div
      v-if="showDetailDrawer && selectedStocktake"
      class="fixed inset-0 z-[1000] flex justify-end"
      style="background: rgba(0,0,0,0.3); backdrop-filter: blur(2px);"
      @click.self="showDetailDrawer = false"
    >
      <div class="w-full max-w-[950px] bg-white h-full flex flex-col shadow-2xl relative animate-slide-in">
        
        <!-- Drawer Header -->
        <div class="px-8 py-5 border-b border-[#f1f5f9] flex justify-between items-center bg-[#f8f9fa]">
          <div>
            <div class="flex items-center gap-3">
              <h3 class="text-lg font-bold text-[#364a63] m-0">Phiên kiểm kê: {{ selectedStocktake.code }}</h3>
              <span :class="['inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold border', getStatusBadgeClass(selectedStocktake.status)]">
                {{ getStatusLabel(selectedStocktake.status) }}
              </span>
            </div>
            <div class="text-xs text-[#8094ae] mt-1">
              Người tạo: <span class="font-bold text-[#364a63]">{{ selectedStocktake.createdByName }}</span>
              | Chi nhánh: <span class="font-bold text-[#364a63]">{{ selectedStocktake.branchName }}</span>
              | Thời gian: <span class="font-mono font-bold">{{ formatDateTime(selectedStocktake.createdAt) }}</span>
            </div>
          </div>
          <button
            @click="showDetailDrawer = false"
            class="w-9 h-9 rounded-xl hover:bg-slate-200 text-slate-400 hover:text-slate-700 flex items-center justify-center transition-colors"
          >
            <i class="fas fa-times text-lg"></i>
          </button>
        </div>

        <!-- Drawer Body -->
        <div class="flex-1 overflow-y-auto p-8 space-y-6">
          
          <!-- Notes section -->
          <div class="space-y-2">
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider">Ghi chú kiểm kê</label>
            <textarea
              v-model="selectedStocktake.notes"
              :disabled="selectedStocktake.status !== 'DRAFT'"
              rows="2"
              placeholder="Nhập ghi chú hoặc lý do kiểm kê đợt này..."
              class="w-full p-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-75 disabled:cursor-not-allowed"
            ></textarea>
          </div>

          <!-- Adjustment Receipts links -->
          <div v-if="adjustmentReceipts.length > 0" class="bg-blue-50 border border-blue-100 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center text-blue-600">
                <i class="fas fa-file-invoice-dollar text-lg"></i>
              </div>
              <div>
                <div class="text-xs font-bold text-blue-800 uppercase tracking-wide">Phiếu điều chỉnh kho liên kết</div>
                <div class="text-xs text-blue-600 mt-0.5">Phiên kiểm kê này có chênh lệch và đã sinh các phiếu cân bằng tồn kho:</div>
              </div>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="r in adjustmentReceipts"
                :key="r.id"
                @click="viewReceipt(r.id)"
                class="px-4 py-2 bg-white hover:bg-blue-600 hover:text-white text-blue-600 border border-blue-200 rounded-xl text-xs font-bold transition-all shadow-sm flex items-center gap-1.5"
              >
                <i class="fas" :class="r.type === 'ADJUST_IN' ? 'fa-plus-circle text-emerald-500' : 'fa-minus-circle text-amber-500'"></i>
                {{ r.code }} ({{ r.type === 'ADJUST_IN' ? 'Cân bằng Tăng' : 'Cân bằng Giảm' }})
              </button>
            </div>
          </div>

          <!-- Items Table -->
          <div class="space-y-3">
            <div class="flex justify-between items-center">
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider">Danh sách lô hàng kiểm đếm</label>
              <div v-if="selectedStocktake.status === 'DRAFT'" class="text-xs text-[#8094ae] italic">
                * Nhập số lượng thực tế đếm được vào cột "Thực tế"
              </div>
            </div>

            <div class="border border-[#e2e8f0] rounded-xl overflow-hidden">
              <table class="w-full border-collapse text-left text-sm">
                <thead>
                  <tr class="bg-[#f8f9fa] border-b border-[#e2e8f0]">
                    <th class="p-3 text-xs font-bold text-[#8094ae] pl-6">Sản phẩm</th>
                    <th class="p-3 text-xs font-bold text-[#8094ae]">Lô SX</th>
                    <th class="p-3 text-xs font-bold text-[#8094ae]">Hạn sử dụng</th>
                    <th class="p-3 text-xs font-bold text-[#8094ae] text-right">Sổ sách</th>
                    <th class="p-3 text-xs font-bold text-[#8094ae] w-[140px] text-center">Thực tế</th>
                    <th class="p-3 text-xs font-bold text-[#8094ae] text-center">Chênh lệch</th>
                    <th class="p-3 text-xs font-bold text-[#8094ae] pr-6 text-center">Phiếu điều chỉnh</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="d in selectedStocktake.details"
                    :key="d.id"
                    class="border-b border-[#f1f5f9] last:border-b-0 hover:bg-slate-50/50"
                  >
                    <td class="p-3 pl-6">
                      <div class="font-bold text-[#364a63]">{{ d.productName }}</div>
                      <div class="text-[11px] font-mono text-[#8094ae] mt-0.5">SKU: {{ d.productSku }} | ĐVT: {{ d.productUnit }}</div>
                    </td>
                    <td class="p-3 font-mono text-xs">{{ d.batchCode }}</td>
                    <td class="p-3 font-mono text-xs text-[#8094ae]">{{ formatDate(d.expirationDate) }}</td>
                    <td class="p-3 text-right font-mono font-bold">{{ d.expectedQuantity }}</td>
                    <td class="p-3 text-center">
                      <input
                        v-if="selectedStocktake.status === 'DRAFT'"
                        v-model.number="d.actualQuantity"
                        type="number"
                        min="0"
                        class="w-20 h-9 border border-[#e2e8f0] bg-white rounded-lg text-center font-mono font-bold text-[#364a63] focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none"
                      />
                      <span v-else class="font-mono font-bold text-[#364a63]">{{ d.actualQuantity }}</span>
                    </td>
                    <td class="p-3 text-center">
                      <span
                        v-if="d.actualQuantity > d.expectedQuantity"
                        class="inline-flex items-center px-2 py-0.5 rounded-lg text-xs font-bold bg-blue-50 text-blue-600 border border-blue-100"
                      >
                        +{{ d.actualQuantity - d.expectedQuantity }} (Thừa)
                      </span>
                      <span
                        v-else-if="d.actualQuantity < d.expectedQuantity"
                        class="inline-flex items-center px-2 py-0.5 rounded-lg text-xs font-bold bg-amber-50 text-amber-600 border border-amber-100"
                      >
                        {{ d.actualQuantity - d.expectedQuantity }} (Thiếu)
                      </span>
                      <span v-else class="text-slate-400 text-xs font-medium">Khớp</span>
                    </td>
                    <td class="p-3 pr-6 text-center">
                      <button
                        v-if="d.adjustmentReceiptId"
                        @click="viewReceipt(d.adjustmentReceiptId)"
                        class="text-xs font-mono font-bold text-[#4361ee] hover:underline"
                      >
                        {{ d.adjustmentReceiptCode }}
                      </button>
                      <span v-else class="text-xs text-slate-400">-</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="px-8 py-5 border-t border-[#f1f5f9] bg-[#f8f9fa] flex flex-col sm:flex-row gap-4 justify-between items-center">
          <div>
            <button
              @click="showDetailDrawer = false"
              class="px-6 h-11 border border-slate-200 bg-white hover:bg-slate-50 text-[#364a63] rounded-xl font-bold transition-all text-sm"
            >
              Đóng panel
            </button>
          </div>
          
          <div v-if="selectedStocktake.status === 'DRAFT'" class="flex gap-3">
            <button
              @click="saveDraft"
              :disabled="savingDraft"
              class="px-6 h-11 bg-slate-100 hover:bg-[#4361ee]/10 text-[#4361ee] rounded-xl font-bold transition-all text-sm flex items-center gap-2"
            >
              <i v-if="savingDraft" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-save"></i>
              Lưu bản nháp
            </button>

            <!-- Manager only complete/cancel -->
            <template v-if="isManager">
              <button
                @click="showCancelConfirm = true"
                class="px-6 h-11 bg-red-50 hover:bg-red-100 text-[#ef476f] border border-red-200 rounded-xl font-bold transition-all text-sm flex items-center gap-1.5"
              >
                <i class="fas fa-ban"></i>
                Hủy bỏ
              </button>
              
              <button
                @click="showCompleteConfirm = true"
                class="px-6 h-11 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl font-bold transition-all text-sm flex items-center gap-1.5 shadow-sm hover:shadow-md"
              >
                <i class="fas fa-check-circle"></i>
                Duyệt hoàn tất
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- RECEIPT DETAIL MODAL -->
    <AppModal
      :show="showReceiptModal"
      title="Chi tiết phiếu điều chỉnh kho"
      size="lg"
      @close="showReceiptModal = false"
    >
      <div v-if="receiptLoading" class="flex flex-col items-center justify-center py-16">
        <i class="fas fa-spinner fa-spin text-2xl text-[#4361ee] mb-3"></i>
        <span class="text-sm text-[#8094ae]">Đang tải dữ liệu phiếu...</span>
      </div>

      <div v-else-if="!selectedReceipt" class="p-8 text-center text-slate-500">
        Không thể tìm thấy thông tin phiếu này.
      </div>

      <div v-else class="p-6 space-y-6">
        <!-- Receipt general details -->
        <div class="grid grid-cols-2 gap-4 bg-[#f8f9fa] p-4 rounded-xl text-sm">
          <div>
            <div class="text-slate-400 text-xs uppercase font-bold mb-1">Mã phiếu</div>
            <div class="font-mono font-bold text-[#364a63]">{{ selectedReceipt.code }}</div>
          </div>
          <div>
            <div class="text-slate-400 text-xs uppercase font-bold mb-1">Loại phiếu</div>
            <div class="font-bold text-[#364a63]">
              <span v-if="selectedReceipt.type === 'ADJUST_IN'" class="text-blue-600">Cân bằng Tăng (ADJUST_IN)</span>
              <span v-else class="text-amber-600">Cân bằng Giảm (ADJUST_OUT)</span>
            </div>
          </div>
          <div>
            <div class="text-slate-400 text-xs uppercase font-bold mb-1">Người lập</div>
            <div class="font-semibold text-[#364a63]">{{ selectedReceipt.createdByName }}</div>
          </div>
          <div>
            <div class="text-slate-400 text-xs uppercase font-bold mb-1">Thời gian lập</div>
            <div class="font-mono text-[#8094ae]">{{ formatDateTime(selectedReceipt.createdAt) }}</div>
          </div>
          <div class="col-span-2">
            <div class="text-slate-400 text-xs uppercase font-bold mb-1">Lý do điều chỉnh</div>
            <div class="text-[#364a63]">{{ selectedReceipt.description || '-' }}</div>
          </div>
        </div>

        <!-- Receipt items -->
        <div class="space-y-2">
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wider">Danh sách chi tiết hàng hóa</div>
          <div class="border border-slate-100 rounded-xl overflow-hidden">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-100">
                  <th class="p-3 text-xs font-bold text-[#8094ae] pl-5">Sản phẩm</th>
                  <th class="p-3 text-xs font-bold text-[#8094ae]">Lô SX</th>
                  <th class="p-3 text-xs font-bold text-[#8094ae] text-right">Số lượng</th>
                  <th class="p-3 text-xs font-bold text-[#8094ae] text-right">Giá nhập</th>
                  <th class="p-3 text-xs font-bold text-[#8094ae] pr-5 text-right">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in selectedReceipt.details"
                  :key="item.id"
                  class="border-b border-slate-100 last:border-b-0"
                >
                  <td class="p-3 pl-5">
                    <div class="font-bold text-[#364a63]">{{ item.productName }}</div>
                    <div class="text-[11px] font-mono text-[#8094ae]">SKU: {{ item.productSku }}</div>
                  </td>
                  <td class="p-3 font-mono text-xs">{{ item.batchCode }}</td>
                  <td class="p-3 text-right font-mono font-bold">{{ item.quantity }}</td>
                  <td class="p-3 text-right font-mono">{{ formatVND(item.price) }}</td>
                  <td class="p-3 pr-5 text-right font-mono font-bold text-[#364a63]">
                    {{ formatVND(item.price * item.quantity) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Receipt close -->
        <div class="flex justify-end pt-2">
          <button
            @click="showReceiptModal = false"
            class="px-6 h-10 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg font-bold text-sm"
          >
            Đóng cửa sổ
          </button>
        </div>
      </div>
    </AppModal>

    <!-- DUYỆT HOÀN TẤT CONFIRMATION -->
    <ConfirmDialog
      :show="showCompleteConfirm"
      title="Xác nhận duyệt kiểm kê"
      message="Hành động này sẽ chốt số liệu thực tế kiểm đếm, tự động sinh phiếu điều chỉnh kho ADJUST_IN / ADJUST_OUT và cập nhật lại số lượng tồn kho. Bạn có chắc chắn muốn hoàn tất?"
      confirmText="Duyệt hoàn tất"
      cancelText="Hủy"
      @confirm="completeStocktake"
      @cancel="showCompleteConfirm = false"
    />

    <!-- HỦY PHIÊN CONFIRMATION -->
    <ConfirmDialog
      :show="showCancelConfirm"
      title="Xác nhận hủy phiên kiểm kê"
      message="Bạn có chắc chắn muốn hủy phiên kiểm kê này? Dữ liệu thực tế kiểm đếm sẽ bị loại bỏ và tồn kho sẽ giữ nguyên."
      confirmText="Hủy phiên"
      cancelText="Hủy"
      danger
      @confirm="cancelStocktake"
      @cancel="showCancelConfirm = false"
    />

  </div>
</template>

<style scoped>
@keyframes slide-in {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.animate-slide-in {
  animation: slide-in 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
</style>
