<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import { draftStocktakeCount, refreshStocktakeBadge } from '../utils/stocktakeStore'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))

// ─── Tabs ───────────────────────────────────────────────────────────────────
const activeTab = ref<'periodic' | 'receipt'>('periodic')
const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

const loading = ref(true)
const stocktakes = ref<any[]>([])
const searchKeyword = ref('')
const selectedStatus = ref('')
const filterDeviation = ref('')

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
const isSpaceEasterEgg = ref(false)

const canEditDraft = computed(() => {
  if (!selectedStocktake.value || selectedStocktake.value.status !== 'DRAFT') return false
  if (user.value?.role === 'STAFF') return true
  if (selectedStocktake.value.createdById === user.value?.id) return true
  return false
})

const hasDeviation = computed(() => {
  if (!selectedStocktake.value) return false
  return selectedStocktake.value.details.some((d: any) => d.actualQuantity !== d.expectedQuantity)
})

const showDetailDrawer = ref(false)

// Confirm dialogs
const showCreateConfirm = ref(false)
const showCompleteConfirm = ref(false)
const showCancelConfirm = ref(false)
const showRejectConfirm = ref(false)
const actionLoading = ref(false)

const showApproveModal = ref(false)
const approveForm = ref({ reason: '', responsibleType: 'internal', responsibleUserId: '', responsiblePersonName: '', warehouseWorkerName: '' })
const systemUsers = ref<any[]>([])
const rejectReason = ref('')

async function loadSystemUsers(specificBranchId?: number) {
  try {
    const branchId = specificBranchId || user.value?.branchId || user.value?.branch?.id || ''
    const res = await api.get(`/api/users?branchId=${branchId}`)
    if (res.ok) {
      const data = await res.json()
      if (specificBranchId) {
        systemUsers.value = data.filter((u: any) => u.branchId === specificBranchId)
      } else {
        systemUsers.value = data
      }
    }
  } catch (err) {}
}

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
  showCreateConfirm.value = false
  actionLoading.value = true
  try {
    const res = await api.post('/api/stocktakes', { notes: 'Phiên kiểm kê mới khởi tạo' })
    if (res.ok) {
      toast.success('Khởi tạo phiên kiểm kê thành công!')
      const newSt = await res.json()
      await loadStocktakes()
      await refreshStocktakeBadge()   // ↠ cập nhật badge ngay
      openDetail(newSt)
    } else {
      const err = await res.text()
      toast.error(err || 'Khởi tạo phiên kiểm kê thất bại.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    actionLoading.value = false
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
      isSpaceEasterEgg.value = Math.random() < 0.004
      showDetailDrawer.value = true
    } else {
      toast.error('Không thể tải chi tiết phiên kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  }
}



// Save Draft or Save Manager Edit (Manual)
async function saveStocktake() {
  if (!selectedStocktake.value) return
  actionLoading.value = true
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
      toast.success('Đã lưu số liệu kiểm kê!')
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
    } else {
      const err = await res.text()
      toast.error(err || 'Lưu số liệu thất bại.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}

// Premium Auto-Save Feature
let autoSaveTimeout: ReturnType<typeof setTimeout> | null = null;
function triggerAutoSave() {
  if (autoSaveTimeout) clearTimeout(autoSaveTimeout);
  autoSaveTimeout = setTimeout(async () => {
    if (!selectedStocktake.value) return;
    try {
      const payload = {
        notes: selectedStocktake.value.notes,
        details: selectedStocktake.value.details.map((d: any) => ({
          id: d.id,
          actualQuantity: Number(d.actualQuantity)
        }))
      }
      // Silent save in background
      await api.put(`/api/stocktakes/${selectedStocktake.value.id}`, payload)
    } catch (e) {
      console.error('Auto save failed', e)
    }
  }, 1000);
}

// Complete Stocktake
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
      toast.success('Nộp kiểm kê thành công!')
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
      await refreshStocktakeBadge()   // ↠ cập nhật badge ngay
    } else {
      const err = await res.text()
      toast.error(err || 'Không thể nộp phiên kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}

async function openApproveModal() {
  approveForm.value.warehouseWorkerName = ''
  if (selectedStocktake.value?.branchId) {
    await loadSystemUsers(selectedStocktake.value.branchId)
  }
  showApproveModal.value = true
}

async function approveStocktakeDeviation() {
  if (!selectedStocktake.value) return
  if (!approveForm.value.reason) {
    toast.error('Vui lòng nhập lý do (VD: Đếm ngu, Mất hàng...)')
    return
  }
  showApproveModal.value = false
  actionLoading.value = true
  try {
    let responsibleName = ''
    let responsibleUserId: number | null = null

    if (!hasDeviation.value) {
      if (!approveForm.value.warehouseWorkerName.trim()) {
        toast.error('Vui lòng nhập tên thủ kho đếm sai.')
        actionLoading.value = false
        return
      }
      const creatorName = selectedStocktake.value?.createdByName || 'Nhân viên hệ thống'
      responsibleName = `NV Lập phiếu: ${creatorName} & Thủ kho đếm: ${approveForm.value.warehouseWorkerName.trim()}`
    } else {
      if (approveForm.value.responsibleType === 'internal') {
        if (!approveForm.value.responsibleUserId) {
          responsibleName = 'Công ty tự chịu trách nhiệm'
        } else {
          responsibleUserId = Number(approveForm.value.responsibleUserId)
          const emp = systemUsers.value.find((u: any) => u.id === responsibleUserId)
          responsibleName = emp ? emp.fullName : ''
        }
      } else {
        if (!approveForm.value.responsiblePersonName.trim()) {
          toast.error('Vui lòng nhập tên người chịu trách nhiệm (Thủ kho, Khách hàng...).')
          actionLoading.value = false
          return
        }
        responsibleName = approveForm.value.responsiblePersonName.trim()
      }
    }

    // Tự động lưu số liệu trước khi duyệt
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
      toast.error(err || 'Lưu số liệu thất bại trước khi duyệt.')
      actionLoading.value = false
      return
    }

    const res = await api.patch(`/api/stocktakes/${selectedStocktake.value.id}/approve`, {
      reason: approveForm.value.reason,
      responsiblePersonName: responsibleName,
      responsibleUserId: responsibleUserId
    })
    if (res.ok) {
      toast.success('Duyệt kiểm kê thành công!')
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
    } else {
      const err = await res.text()
      toast.error(err || 'Duyệt kiểm kê thất bại.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}


// Reject Stocktake (Yêu cầu đếm lại)
async function rejectStocktake() {
  if (!selectedStocktake.value) return
  if (!rejectReason.value.trim()) {
    toast.error('Vui lòng nhập lý do yêu cầu đếm lại.')
    return
  }
  showRejectConfirm.value = false
  actionLoading.value = true
  try {
    const currentDate = new Date()
    const pad = (n: number) => n.toString().padStart(2, '0')
    const timeStr = `${pad(currentDate.getHours())}:${pad(currentDate.getMinutes())} ${pad(currentDate.getDate())}/${pad(currentDate.getMonth()+1)}`
    
    const oldNotes = selectedStocktake.value.notes || ''
    const newNotes = oldNotes ? `${oldNotes}\n[${timeStr} - QUẢN LÝ YÊU CẦU ĐẾM LẠI]: ${rejectReason.value.trim()}` : `[${timeStr} - QUẢN LÝ YÊU CẦU ĐẾM LẠI]: ${rejectReason.value.trim()}`
    
    const payload = {
      notes: newNotes,
      details: selectedStocktake.value.details.map((d: any) => ({
        id: d.id,
        actualQuantity: Number(d.actualQuantity)
      }))
    }
    const saveRes = await api.put(`/api/stocktakes/${selectedStocktake.value.id}`, payload)
    if (!saveRes.ok) {
       toast.error('Lỗi khi lưu lý do đếm lại.')
       actionLoading.value = false
       return
    }

    const res = await api.patch(`/api/stocktakes/${selectedStocktake.value.id}/reject`, {})
    if (res.ok) {
      toast.success('Đã yêu cầu nhân viên đếm lại!')
      rejectReason.value = ''
      await loadingDetail(selectedStocktake.value.id)
      await loadStocktakes()
      await refreshStocktakeBadge()
    } else {
      const err = await res.text()
      toast.error(err || 'Không thể yêu cầu đếm lại.')
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
      await refreshStocktakeBadge()   // ↠ cập nhật badge ngay
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
    
    let deviationMatch = true
    if (filterDeviation.value === 'yes') {
      deviationMatch = !!st.hasDeviation
    } else if (filterDeviation.value === 'no') {
      deviationMatch = !st.hasDeviation
    }
    
    return (codeMatch || noteMatch) && statusMatch && timeMatch && deviationMatch
  }).sort((a, b) => {
    const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return timeB - timeA
  })
})

// ──────────────────────────────────────────────────────────────
// PAGINATION
// ──────────────────────────────────────────────────────────────
const currentPagePeriodic = ref(1)
const currentPageReceipt = ref(1)
const itemsPerPage = 50

watch([activeTab, searchKeyword, selectedStatus, filterDeviation, filterTimeRange, filterFrom, filterTo], () => {
  currentPagePeriodic.value = 1
})

const paginatedStocktakes = computed(() => {
  const start = (currentPagePeriodic.value - 1) * itemsPerPage
  return filteredStocktakes.value.slice(start, start + itemsPerPage)
})

const totalPagesPeriodic = computed(() => Math.ceil(filteredStocktakes.value.length / itemsPerPage) || 1)




// Formatting Helpers
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

function printBlindCount() {
  if (!selectedStocktake.value) return;
  
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    toast.error('Trình duyệt đã chặn popup. Vui lòng cho phép popup để in.');
    return;
  }
  
  let html = `
    <html>
      <head>
        <title>Phiếu Kiểm Đếm Kho - ${selectedStocktake.value.code}</title>
        <style>
          body { font-family: sans-serif; padding: 20px; }
          h1 { text-align: center; }
          table { width: 100%; border-collapse: collapse; margin-top: 20px; }
          th, td { border: 1px solid #000; padding: 8px; text-align: left; }
          .qty-box { width: 100px; height: 30px; }
          .header-info { margin-bottom: 20px; }
        </style>
      </head>
      <body>
        <h1>PHIẾU KIỂM ĐẾM KHO</h1>
        <div class="header-info">
          <p><strong>Mã phiên kiểm kê:</strong> ${selectedStocktake.value.code}</p>
          <p><strong>Chi nhánh:</strong> ${selectedStocktake.value.branchName}</p>
          <p><strong>Ngày tạo:</strong> ${formatDateTime(selectedStocktake.value.createdAt)}</p>
          <p><strong>Người thực hiện:</strong> ........................................</p>
        </div>
        <table>
          <thead>
            <tr>
              <th>STT</th>
              <th>Sản phẩm</th>
              <th>SKU</th>
              <th>Lô SX</th>
              <th>HSD</th>
              <th>Số lượng đếm được</th>
            </tr>
          </thead>
          <tbody>
            ${selectedStocktake.value.details.map((d: any, i: number) => `
              <tr>
                <td>${i + 1}</td>
                <td>${d.productName}</td>
                <td>${d.productSku}</td>
                <td>${d.batchCode}</td>
                <td>${formatDate(d.expirationDate)}</td>
                <td><div class="qty-box"></div></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        <script>
          window.onload = function() { window.print(); window.close(); }
        <\/script>
      </body>
    </html>
  `;
  
  printWindow.document.write(html);
  printWindow.document.close();
}

function getStatusBadgeClass(status: string) {
  switch (status) {
    case 'DRAFT': return 'bg-slate-100 text-slate-700 border-slate-200'
    case 'PENDING_APPROVAL': return 'bg-orange-100 text-orange-700 border-orange-200'
    case 'COMPLETED': return 'bg-emerald-100 text-emerald-700 border-emerald-200'
    case 'CANCELLED': return 'bg-red-100 text-red-700 border-red-200'
    default: return 'bg-slate-100 text-slate-600'
  }
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'DRAFT': return 'Đang kiểm đếm'
    case 'PENDING_APPROVAL': return 'Chờ duyệt'
    case 'COMPLETED': return 'Đã hoàn tất'
    case 'CANCELLED': return 'Đã hủy'
    default: return status
  }
}

// ─── TAB 2: KIỂM KÊ KHI NHẬN HÀNG (PENDING_STOCKTAKE receipts) ──────────────
const receiptStocktakes = ref<any[]>([])
const pendingReceiptStocktakesCount = computed(() => {
  return receiptStocktakes.value.filter(r => r.status === 'PENDING_STOCKTAKE').length
})
const rsLoading = ref(false)
const rsSearch = ref('')
const rsFilterType = ref('') // 'IMPORT' | 'TRANSFER' | ''

watch([activeTab, rsFilterType, rsSearch], () => {
  currentPageReceipt.value = 1
})

async function loadReceiptStocktakes() {
  rsLoading.value = true
  try {
    const res = await api.get('/api/receipts')
    if (res.ok) {
      const all: any[] = await res.json()
      receiptStocktakes.value = all.filter((r: any) => {
        const allowedStatuses = [
          'PENDING_STOCKTAKE',
          'PENDING_SHORTFALL_MANAGER',
          'PENDING_SHORTFALL_ADMIN',
          'PENDING_COMPENSATION',
          'COMPLETED'
        ]
        if (!allowedStatuses.includes(r.status)) return false
        // Admin thấy tất cả phiếu chờ kiểm kê / đã kiểm kê
        if (user.value?.role === 'ADMIN') return true
        const uBranchId = user.value?.branchId || user.value?.branch?.id
        return Number(r.destBranchId) === Number(uBranchId)
      })
    } else {
      toast.error('Không thể tải danh sách phiếu chờ kiểm kê.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    rsLoading.value = false
  }
}

const filteredReceiptStocktakes = computed(() => {
  let list = receiptStocktakes.value
  if (rsFilterType.value) list = list.filter(r => r.type === rsFilterType.value)
  if (rsSearch.value.trim()) {
    const kw = rsSearch.value.trim().toLowerCase()
    list = list.filter(r => (r.code || '').toLowerCase().includes(kw))
  }
  return list.sort((a, b) => {
    const da = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const db = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return db - da
  })
})

const paginatedReceiptStocktakes = computed(() => {
  const start = (currentPageReceipt.value - 1) * itemsPerPage
  return filteredReceiptStocktakes.value.slice(start, start + itemsPerPage)
})

const totalPagesReceipt = computed(() => Math.ceil(filteredReceiptStocktakes.value.length / itemsPerPage) || 1)

// Xem chi tiết phiếu nhận hàng
const selectedRsReceipt = ref<any>(null)
const showRsDrawer = ref(false)
const rsDetailLoading = ref(false)

async function openRsDetail(r: any) {
  isSpaceEasterEgg.value = Math.random() < 0.004
  rsDetailLoading.value = true
  showRsDrawer.value = true
  selectedRsReceipt.value = null
  try {
    const res = await api.get(`/api/receipts/${r.id}`)
    if (res.ok) {
      const data = await res.json()
      selectedRsReceipt.value = {
        ...data,
        // map items for confirmation
        confirmItems: (data.details || []).map((d: any) => ({
          receiptDetailId: d.id,
          productName: d.productName,
          productSku: d.productSku,
          batchCode: d.batchCode,
          sentQty: d.quantity,
          actualQuantity: d.receivedQuantity !== null && d.receivedQuantity !== undefined ? d.receivedQuantity : d.quantity,
          shortfallReason: d.shortfallReason || ''
        }))
      }
    } else {
      toast.error('Không thể tải chi tiết phiếu.')
      showRsDrawer.value = false
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
    showRsDrawer.value = false
  } finally {
    rsDetailLoading.value = false
  }
}

// Submit xác nhận kiểm kê nhận hàng
const submittingRs = ref(false)

async function submitRsConfirm() {
  if (!selectedRsReceipt.value || submittingRs.value) return
  const items = selectedRsReceipt.value.confirmItems
  if (items.some((i: any) => i.actualQuantity < 0)) {
    toast.error('Số lượng thực tế không được âm.'); return
  }
  if (items.some((i: any) => i.actualQuantity > i.sentQty)) {
    toast.error('Số lượng thực tế không được vượt quá số lượng trên phiếu.'); return
  }
  if (items.some((i: any) => i.actualQuantity < i.sentQty && (!i.shortfallReason || i.shortfallReason.trim() === ''))) {
    toast.error('Vui lòng nhập lý do hao hụt cho sản phẩm bị thiếu.'); return
  }
  submittingRs.value = true
  try {
    const payload = {
      items: items.map((i: any) => ({
        receiptDetailId: i.receiptDetailId,
        actualQuantity: i.actualQuantity,
        shortfallReason: i.actualQuantity < i.sentQty ? i.shortfallReason : null
      }))
    }
    const res = await api.post(`/api/receipts/${selectedRsReceipt.value.id}/confirm-stocktake`, payload)
    if (res.ok) {
      toast.success('Xác nhận kiểm kê thành công! Hàng đã được cộng vào kho.')
      showRsDrawer.value = false
      await loadReceiptStocktakes()
    } else {
      let msg = 'Lỗi khi xác nhận kiểm kê.'
      try { const e = await res.json(); msg = e.message || msg } catch {}
      toast.error(msg)
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    submittingRs.value = false
  }
}

function rsTypeLabel(t: string) {
  return t === 'IMPORT' ? 'Nhập kho' : t === 'TRANSFER' ? 'Điều chuyển' : t
}
function rsTypeClass(t: string) {
  return t === 'IMPORT' ? 'bg-blue-100 text-blue-700' : 'bg-orange-100 text-orange-700'
}
function rsStatusClass(s: string) {
  const map: Record<string, string> = {
    PENDING_STOCKTAKE: 'bg-purple-100 text-purple-700 border border-purple-300',
    PENDING_SHORTFALL_MANAGER: 'bg-orange-100 text-orange-700 border border-orange-300',
    PENDING_SHORTFALL_ADMIN: 'bg-rose-100 text-rose-700 border border-rose-300',
    PENDING_COMPENSATION: 'bg-indigo-100 text-indigo-700 border border-indigo-300',
    COMPLETED: 'bg-green-100 text-green-700 border border-green-300',
    CANCELLED: 'bg-red-100 text-red-600 border border-red-300'
  }
  return map[s] || 'bg-gray-100 text-gray-600'
}
function rsStatusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_STOCKTAKE: '📦 Chờ kiểm kê',
    PENDING_SHORTFALL_MANAGER: '⚠️ Thiếu hụt (Chờ duyệt)',
    PENDING_SHORTFALL_ADMIN: '⚠️ Thiếu hụt (Chờ duyệt)',
    PENDING_COMPENSATION: '⏳ Chờ điều chuyển bù',
    COMPLETED: '✅ Đã hoàn tất',
    CANCELLED: '❌ Đã hủy'
  }
  return map[s] || s
}

const isInitialLoad = ref(true)

watch([loading, rsLoading], ([newL, newRL]) => {
  if (!newL && !newRL) {
    setTimeout(() => {
      isInitialLoad.value = false
    }, 1000)
  }
})

watch(activeTab, () => {
  isInitialLoad.value = true
})

function triggerStocktakesAnimation() {
  isInitialLoad.value = true
  loadStocktakes()
  loadReceiptStocktakes()
}

onMounted(() => {
  window.addEventListener('trigger-stocktakes-animation', triggerStocktakesAnimation)
  if (isManager.value) {
    loadSystemUsers()
  }
  Promise.all([loadStocktakes(), loadReceiptStocktakes()])
})

onUnmounted(() => {
  window.removeEventListener('trigger-stocktakes-animation', triggerStocktakesAnimation)
})
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto font-['Inter',sans-serif]">

    <!-- PAGE HEADER + TABS -->
    <div :class="['flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2', isInitialLoad ? 'header-slide-down' : '']">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0">Kiểm kê kho</h2>
        <p class="text-[#8094ae] text-sm mt-1">Quản lý kiểm kê định kỳ và xác nhận hàng nhận về</p>
      </div>

      <!-- Tabs -->
      <div class="flex items-center gap-6 border-b border-[#e2e8f0]">
        <button
          v-for="tab in [
            { key: 'periodic', label: 'Kiểm kê định kỳ', icon: 'fas fa-clipboard-list',
              badge: draftStocktakeCount },
            { key: 'receipt',  label: 'Kiểm kê nhận hàng', icon: 'fas fa-truck-loading',
              badge: pendingReceiptStocktakesCount }
          ]"
          :key="tab.key"
          :class="[
            'flex items-center gap-2 pb-3 px-1 text-sm font-bold transition-colors relative',
            activeTab === tab.key ? 'text-[var(--accent-500)]' : 'text-[#8094ae] hover:text-[#364a63]'
          ]"
          @click="activeTab = tab.key as any"
        >
          <i :class="tab.icon"></i>
          {{ tab.label }}
          <span
            v-if="(tab as any).badge > 0"
            class="ml-1 inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full text-[10px] font-bold bg-purple-500 text-white"
          >{{ (tab as any).badge }}</span>
          <div v-if="activeTab === tab.key" class="absolute bottom-[-1px] left-0 w-full h-[2px] bg-[var(--accent-500)] rounded-t-full"></div>
        </button>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════
         TAB 1: KIỂM KÊ ĐỊNH KỲ
    ════════════════════════════════════════════════════════ -->
    <template v-if="activeTab === 'periodic'">

    <!-- Toolbar: Filter + Khởi tạo -->
    <div :class="['bg-indigo-50 rounded-[10px] border border-[#f1f5f9] border-t-4 border-t-[var(--accent-500)] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden', isInitialLoad ? 'accordion-filter-expand' : '']">
      <div class="p-5 border-b border-[#f1f5f9] bg-white/60 space-y-4">
        <div class="flex flex-col md:flex-row gap-3">
          <div class="relative flex-1">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input v-model="searchKeyword" type="text" placeholder="Tìm theo mã kiểm kê hoặc ghi chú..."
              class="w-full h-11 pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63]" />
          </div>
          <select v-model="selectedStatus" class="h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm text-[#364a63] font-medium outline-none">
            <option value="">Tất cả trạng thái</option>
            <option value="DRAFT">Đang kiểm đếm</option>
            <option value="PENDING_APPROVAL">Chờ duyệt</option>
            <option value="COMPLETED">Đã hoàn tất</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
          <select v-model="filterDeviation" class="h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm text-[#364a63] font-medium outline-none">
            <option value="">Tất cả chênh lệch</option>
            <option value="yes">Có chênh lệch</option>
            <option value="no">Khớp số lượng</option>
          </select>
          <button v-if="user?.role === 'STAFF'" @click="showCreateConfirm = true"
            class="h-11 px-5 bg-gradient-to-r from-[var(--accent-500)] to-[var(--accent-300)] hover:from-[var(--accent-700)] hover:to-[var(--accent-500)] text-white rounded-xl font-bold transition-all shadow-md flex items-center gap-2 whitespace-nowrap">
            <i class="fas fa-plus"></i> Khởi tạo kiểm kê
          </button>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 pt-3 border-t border-slate-100">
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Thời gian</label>
            <select v-model="filterTimeRange" class="w-full h-10 px-3 border border-[#e2e8f0] bg-white rounded-xl text-sm text-[#364a63] outline-none">
              <option value="all">Tất cả</option>
              <option value="today">Hôm nay</option>
              <option value="week">7 ngày qua</option>
              <option value="month">30 ngày qua</option>
              <option value="custom">Tùy chọn...</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Từ ngày</label>
            <input v-model="filterFrom" type="date" :disabled="filterTimeRange !== 'custom'"
              class="w-full h-10 px-3 border border-[#e2e8f0] bg-white rounded-xl text-sm text-[#364a63] outline-none disabled:opacity-50 disabled:cursor-not-allowed" />
          </div>
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Đến ngày</label>
            <input v-model="filterTo" type="date" :disabled="filterTimeRange !== 'custom'"
              class="w-full h-10 px-3 border border-[#e2e8f0] bg-white rounded-xl text-sm text-[#364a63] outline-none disabled:opacity-50 disabled:cursor-not-allowed" />
          </div>
          <div class="flex items-end">
            <button v-if="searchKeyword || selectedStatus || filterDeviation || filterTimeRange !== 'all'"
              @click="searchKeyword=''; selectedStatus=''; filterDeviation=''; filterTimeRange='all'; filterFrom=''; filterTo=''"
              class="w-full h-10 flex items-center justify-center gap-2 border border-[#e2e8f0] bg-white rounded-xl text-sm font-semibold text-[#8094ae] hover:text-[#364a63] transition-all">
              <i class="fas fa-times"></i> Xóa lọc
            </button>
          </div>
        </div>
      </div>
    </div><!-- end bg-indigo-50 filter card -->

    <!-- STOCKTAKE TABLE -->
    <div :class="['bg-white rounded-[10px] shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] overflow-hidden', isInitialLoad ? 'accordion-table-expand' : '']">
      <div v-if="loading" class="flex flex-col items-center justify-center py-16">
        <i class="fas fa-spinner fa-spin text-3xl text-[var(--accent-500)] mb-4"></i>
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
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Chênh lệch</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider pr-8 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(st, index) in paginatedStocktakes"
              :key="st.id"
              :class="[
                'border-b border-[#f1f5f9] hover:border-transparent hover:bg-[#f8f9fa] transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]',
                st.hasDeviation && st.status === 'COMPLETED' ? 'bg-rose-50/50 hover:bg-rose-100/50' : '',
                isInitialLoad ? 'accordion-row-anim' : ''
              ]"
              :style="isInitialLoad ? { '--row-delay': `${90 + index * 20}ms` } : {}"
              @dblclick="openDetail(st)"
            >
              <td class="p-4 pl-8 font-mono font-bold text-[var(--accent-500)]"><div>{{ st.code }}</div></td>
              <td class="p-4 text-sm font-semibold text-[#364a63]"><div>{{ st.branchName }}</div></td>
              <td class="p-4 text-sm text-[#364a63]"><div>{{ st.createdByName }}</div></td>
              <td class="p-4 text-sm text-[#8094ae] font-mono"><div>{{ formatDateTime(st.createdAt) }}</div></td>
              <td class="p-4 text-sm text-slate-500 max-w-[200px] truncate" :title="st.notes"><div>{{ st.notes || '-' }}</div></td>
              <td class="p-4">
                <span :class="['inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold border whitespace-nowrap', getStatusBadgeClass(st.status)]">
                  {{ getStatusLabel(st.status) }}
                </span>
              </td>
              <td class="p-4">
                <div v-if="st.hasDeviation" class="flex flex-col max-w-[200px]" :title="st.deviationSummary">
                  <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-50 text-rose-600 border border-rose-100 w-fit">
                    ⚠️ Lệch số lượng
                  </span>
                  <span class="text-xs text-rose-500 mt-1 font-medium truncate" :title="st.deviationSummary">
                    {{ st.deviationSummary }}
                  </span>
                </div>
                <div v-else-if="st.status === 'COMPLETED'" class="text-[11px] text-emerald-600 font-bold">
                  <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-100 w-fit">
                    ✓ Khớp
                  </span>
                </div>
                <div v-else class="text-xs text-slate-400"><div>—</div></div>
              </td>
              <td class="p-4 pr-8 text-right">
                <button
                  @click="openDetail(st)"
                  class="h-8 px-4 bg-slate-100 hover:bg-[var(--accent-500)] hover:text-white text-slate-700 rounded-lg text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 inline-flex"
                >
                  <i class="fas" :class="st.status === 'DRAFT' ? 'fa-pen' : 'fa-eye'"></i>
                  {{ st.status === 'DRAFT' ? 'Kiểm đếm' : 'Xem chi tiết' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="filteredStocktakes.length > 0" class="px-6 py-4 border-t border-[#e2e8f0] flex flex-col sm:flex-row items-center justify-between bg-white rounded-b-2xl gap-4">
        <div class="text-sm text-[#8094ae]">
          Trang <span class="font-bold text-[#364a63]">{{ currentPagePeriodic }}/{{ totalPagesPeriodic }}</span> - Hiển thị <span class="font-bold text-[#364a63]">{{ paginatedStocktakes.length }}/{{ filteredStocktakes.length }}</span> phiên kiểm kê
        </div>
        <div class="flex items-center gap-2">
          <button @click="currentPagePeriodic--" :disabled="currentPagePeriodic === 1"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            <i class="fas fa-chevron-left mr-1.5 text-[10px]"></i> Trước
          </button>
          <div class="px-4 py-1.5 flex items-center justify-center rounded-lg bg-[#f8f9fa] text-[#364a63] font-bold text-sm border border-[#e2e8f0]">
            {{ currentPagePeriodic }} / {{ totalPagesPeriodic }}
          </div>
          <button @click="currentPagePeriodic++" :disabled="currentPagePeriodic === totalPagesPeriodic"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            Sau <i class="fas fa-chevron-right ml-1.5 text-[10px]"></i>
          </button>
        </div>
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
        <div class="theme-modal-header relative overflow-hidden flex items-center justify-between px-8 py-6 transition-colors duration-500" :class="{ 'easter-egg-space': isSpaceEasterEgg }">
          <template v-if="isSpaceEasterEgg">
            <!-- Space Easter Egg Decor -->
            <div class="absolute inset-0 pointer-events-none">
              <i class="fas fa-rocket absolute top-4 right-32 text-white/80 text-4xl animate-[bounce_3s_infinite] -rotate-45"></i>
              <i class="fas fa-meteor absolute -top-4 right-16 text-orange-400/60 text-6xl rotate-[120deg] drop-shadow-[0_0_15px_rgba(251,146,60,0.8)]"></i>
              <i class="fas fa-user-astronaut absolute bottom-2 right-64 text-white/60 text-3xl animate-[bounce_4s_infinite]"></i>
              <i class="fas fa-star absolute top-2 right-48 text-white/90 text-[8px] animate-pulse"></i>
              <i class="fas fa-star absolute bottom-4 right-20 text-white/70 text-[6px] animate-pulse" style="animation-delay: 1s"></i>
              <i class="fas fa-satellite absolute top-8 right-80 text-white/50 text-2xl animate-[spin_20s_linear_infinite]"></i>
            </div>
          </template>
          <template v-else>
            <!-- Light Mode Decor: Sun & Clouds -->
            <div :key="'light-st-detail-' + selectedStocktake?.id" class="theme-light-decor absolute inset-0 pointer-events-none transition-all duration-500">
              <i class="fas fa-sun absolute -top-12 right-8 text-yellow-300 text-[140px] opacity-10 animate-[spin_40s_linear_infinite]"></i>
              <i class="fas fa-sun absolute top-3 right-24 text-yellow-300 text-5xl drop-shadow-[0_0_20px_rgba(253,224,71,0.8)] animate-[spin_20s_linear_infinite]"></i>
              <i class="fas fa-cloud absolute top-8 right-44 text-white/50 text-5xl drop-shadow-sm"></i>
              <i class="fas fa-cloud absolute top-2 right-64 text-white/40 text-3xl"></i>
              <i class="fas fa-cloud absolute -bottom-2 right-28 text-white/30 text-7xl"></i>
            </div>

            <!-- Dark Mode Decor: Moon & Stars -->
            <div :key="'dark-st-detail-' + selectedStocktake?.id" class="theme-dark-decor absolute inset-0 pointer-events-none transition-all duration-500">
              <i class="fas fa-moon absolute -top-8 right-12 text-blue-100 text-[120px] opacity-[0.03] -rotate-12"></i>
              <i class="fas fa-moon absolute top-3 right-24 text-yellow-200 text-4xl drop-shadow-[0_0_15px_rgba(254,240,138,0.5)] -rotate-12"></i>
              <i class="fas fa-star absolute top-4 right-48 text-white/80 text-[8px] animate-pulse"></i>
              <i class="fas fa-star absolute top-8 right-60 text-white/60 text-[10px] animate-pulse" style="animation-delay: 1s"></i>
              <i class="fas fa-star absolute top-3 right-72 text-white/90 text-[6px] animate-pulse" style="animation-delay: 0.5s"></i>
              <i class="fas fa-star absolute bottom-4 right-36 text-white/50 text-[12px] animate-pulse" style="animation-delay: 1.5s"></i>
              <i class="fas fa-star absolute bottom-2 right-56 text-white/70 text-[8px] animate-pulse"></i>
            </div>
          </template>

          <div class="relative z-10 text-white drop-shadow-md">
            <div class="flex items-center gap-3">
              <h3 class="text-lg font-bold text-white m-0">Phiên kiểm kê: {{ selectedStocktake.code }}</h3>
              <span :class="['inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold border border-white/20 bg-white/10 text-white']">
                {{ getStatusLabel(selectedStocktake.status) }}
              </span>
            </div>
            <div class="text-xs text-white/80 mt-1">
              Người tạo: <span class="font-bold text-white">{{ selectedStocktake.createdByName }}</span>
              | Chi nhánh: <span class="font-bold text-white">{{ selectedStocktake.branchName }}</span>
              | Thời gian: <span class="font-mono font-bold text-white">{{ formatDateTime(selectedStocktake.createdAt) }}</span>
            </div>
          </div>
          <button
            @click="showDetailDrawer = false"
            class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10"
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
              @input="triggerAutoSave"
              :disabled="selectedStocktake.status !== 'DRAFT'"
              rows="2"
              placeholder="Nhập ghi chú hoặc lý do kiểm kê đợt này..."
              class="w-full p-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none transition-all text-[#364a63] disabled:opacity-75 disabled:cursor-not-allowed"
            ></textarea>
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
                        v-if="canEditDraft"
                        v-model.number="d.actualQuantity"
                        @input="triggerAutoSave"
                        type="number"
                        min="0"
                        class="w-20 h-9 border border-[#e2e8f0] bg-white rounded-lg text-center font-mono font-bold text-[#364a63] focus:ring-2 focus:ring-[var(--accent-500)]/20 focus:border-[var(--accent-500)] outline-none"
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
                        class="text-xs font-mono font-bold text-[var(--accent-500)] hover:underline"
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
          <div class="flex gap-3">
            <button
              @click="showDetailDrawer = false"
              class="px-6 h-11 border border-slate-200 bg-white hover:bg-slate-50 text-[#364a63] rounded-xl font-bold transition-all text-sm"
            >
              Đóng panel
            </button>
            
            <button
              v-if="selectedStocktake.status === 'DRAFT' || selectedStocktake.status === 'PENDING_APPROVAL'"
              @click="saveStocktake"
              class="hidden"
            >
            </button>

            <button
              v-if="selectedStocktake.status === 'DRAFT'"
              @click="printBlindCount"
              class="px-6 h-11 border border-slate-200 bg-white hover:bg-slate-50 text-[#364a63] rounded-xl font-bold transition-all text-sm flex items-center gap-2"
            >
              <i class="fas fa-print"></i> In phiếu kiểm đếm
            </button>
          </div>
          
          <div v-if="selectedStocktake.status === 'DRAFT'" class="flex gap-3">


            <!-- Complete for Staff/Manager -->
            <button
              v-if="canEditDraft"
              @click="showCompleteConfirm = true"
              class="px-6 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl font-bold transition-all text-sm flex items-center gap-1.5 shadow-sm hover:shadow-md"
            >
              <i class="fas fa-paper-plane"></i>
              Hoàn tất & Nộp
            </button>
            
            <!-- Cancel for Manager -->
            <template v-if="isManager">
              <button
                @click="showCancelConfirm = true"
                class="px-6 h-11 bg-red-50 hover:bg-red-100 text-[#ef476f] border border-red-200 rounded-xl font-bold transition-all text-sm flex items-center gap-1.5"
              >
                <i class="fas fa-ban"></i>
                Hủy bỏ
              </button>
            </template>
          </div>

          <div v-if="selectedStocktake.status === 'PENDING_APPROVAL' && isManager" class="flex gap-3">

            <button
              @click="showCancelConfirm = true"
              class="px-6 h-11 bg-slate-50 hover:bg-slate-100 text-slate-500 border border-slate-200 rounded-xl font-bold transition-all text-sm flex items-center gap-1.5"
            >
              <i class="fas fa-ban"></i>
              Hủy bỏ (Vĩnh viễn)
            </button>
            <button
              @click="showRejectConfirm = true"
              class="px-6 h-11 bg-orange-50 hover:bg-orange-100 text-[#f77f00] border border-orange-200 rounded-xl font-bold transition-all text-sm flex items-center gap-1.5"
            >
              <i class="fas fa-undo"></i>
              Yêu cầu đếm lại
            </button>
            <button
              @click="openApproveModal"
              class="px-6 h-11 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl font-bold transition-all text-sm flex items-center gap-1.5 shadow-sm hover:shadow-md"
            >
              <i class="fas fa-check-double"></i>
              {{ hasDeviation ? 'Duyệt chênh lệch' : 'Duyệt kiểm kê' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    </template><!-- end periodic tab -->

    <!-- ═══════════════════════════════════════════════════
         TAB 2: KIỂM KÊ NHẬN HÀNG (PENDING_STOCKTAKE)
    ════════════════════════════════════════════════════ -->
    <template v-if="activeTab === 'receipt'">
    <div :class="['bg-purple-50 rounded-[10px] border border-[#f1f5f9] border-t-4 border-t-purple-500 shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden', isInitialLoad ? 'accordion-filter-expand' : '']">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] bg-white/60 flex flex-col md:flex-row gap-3 items-center">
        <div class="relative flex-1">
          <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
          <input v-model="rsSearch" type="text" placeholder="Tìm theo mã phiếu..."
            class="w-full h-11 pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-purple-300 focus:border-purple-400 outline-none transition-all text-[#364a63]" />
        </div>
        <select v-model="rsFilterType" class="h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm text-[#364a63] font-medium outline-none">
          <option value="">Tất cả loại phiếu</option>
          <option value="IMPORT">Nhập kho</option>
          <option value="TRANSFER">Điều chuyển</option>
        </select>
        <button @click="loadReceiptStocktakes" class="h-11 px-5 bg-white border border-[#e2e8f0] hover:bg-slate-50 text-[#364a63] rounded-xl font-semibold text-sm flex items-center gap-2 shadow-sm">
          <i class="fas fa-sync-alt" :class="rsLoading ? 'fa-spin' : ''"></i> Làm mới
        </button>
      </div>

      <!-- Table -->
      <div class="overflow-x-auto">
        <div v-if="rsLoading" class="flex flex-col items-center justify-center py-16">
          <i class="fas fa-spinner fa-spin text-3xl text-purple-500 mb-4"></i>
          <span class="text-sm text-[#8094ae]">Đang tải danh sách...</span>
        </div>
        <div v-else-if="filteredReceiptStocktakes.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
          <div class="w-16 h-16 rounded-full bg-purple-100 flex items-center justify-center mb-4">
            <i class="fas fa-truck-loading text-2xl text-purple-400"></i>
          </div>
          <h4 class="text-base font-bold text-[#364a63]">Không có phiếu chờ kiểm kê</h4>
          <p class="text-[#8094ae] text-xs mt-1">Tất cả phiếu nhập kho / điều chuyển đã được xác nhận.</p>
        </div>
        <table v-else class="w-full border-collapse text-left">
          <thead>
            <tr class="border-b border-[#f1f5f9] bg-white">
              <th class="p-4 pl-8 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Mã phiếu</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Loại</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Chi nhánh gửi</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Chi nhánh nhận</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Người lập</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Ngày tạo</th>
              <th class="p-4 text-xs font-bold text-[#8094ae] uppercase tracking-wider">Trạng thái</th>
              <th class="p-4 pr-8 text-xs font-bold text-[#8094ae] uppercase tracking-wider text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, index) in paginatedReceiptStocktakes" :key="r.id"
              :class="[
                'border-b border-[#f1f5f9] hover:bg-purple-50/60 transition-all duration-[350ms] cursor-pointer',
                r.status !== 'PENDING_STOCKTAKE' ? 'bg-slate-50/40' : 'bg-white',
                isInitialLoad ? 'accordion-row-anim' : ''
              ]"
              :style="isInitialLoad ? { '--row-delay': `${90 + index * 20}ms` } : {}"
              @dblclick="openRsDetail(r)">
              <td :class="['p-4 pl-8 font-mono font-bold', r.status === 'PENDING_STOCKTAKE' ? 'text-purple-600' : 'text-slate-500']"><div>{{ r.code }}</div></td>
              <td class="p-4">
                <span :class="['inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold', rsTypeClass(r.type)]">
                  {{ rsTypeLabel(r.type) }}
                </span>
              </td>
              <td class="p-4 text-sm text-[#364a63]"><div>{{ r.sourceBranchName || '—' }}</div></td>
              <td class="p-4 text-sm font-semibold text-[#364a63]"><div>{{ r.destBranchName || '—' }}</div></td>
              <td class="p-4 text-sm text-[#364a63]"><div>{{ r.createdByName }}</div></td>
              <td class="p-4 text-sm text-[#8094ae] font-mono"><div>{{ formatDateTime(r.createdAt) }}</div></td>
              <td class="p-4 text-sm">
                <span :class="['inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border', rsStatusClass(r.status)]">
                  {{ rsStatusLabel(r.status) }}
                </span>
              </td>
              <td class="p-4 pr-8 text-right">
                <button @click="openRsDetail(r)"
                  :class="[
                    'h-8 px-4 rounded-lg text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 inline-flex',
                    r.status === 'PENDING_STOCKTAKE'
                      ? 'bg-purple-600 hover:bg-purple-700 text-white'
                      : 'bg-slate-100 hover:bg-slate-200 text-[#526484]'
                  ]">
                  <i :class="r.status === 'PENDING_STOCKTAKE' ? 'fas fa-clipboard-check' : 'fas fa-check-circle text-emerald-500'"></i>
                  {{ r.status === 'PENDING_STOCKTAKE' ? 'Kiểm kê ngay' : 'Xem kết quả' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="filteredReceiptStocktakes.length > 0" class="px-6 py-4 border-t border-[#e2e8f0] flex flex-col sm:flex-row items-center justify-between bg-white rounded-b-2xl gap-4">
        <div class="text-sm text-[#8094ae]">
          Trang <span class="font-bold text-[#364a63]">{{ currentPageReceipt }}/{{ totalPagesReceipt }}</span> - Hiển thị <span class="font-bold text-[#364a63]">{{ paginatedReceiptStocktakes.length }}/{{ filteredReceiptStocktakes.length }}</span> phiếu
        </div>
        <div class="flex items-center gap-2">
          <button @click="currentPageReceipt--" :disabled="currentPageReceipt === 1"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            <i class="fas fa-chevron-left mr-1.5 text-[10px]"></i> Trước
          </button>
          <div class="px-4 py-1.5 flex items-center justify-center rounded-lg bg-[#f8f9fa] text-[#364a63] font-bold text-sm border border-[#e2e8f0]">
            {{ currentPageReceipt }} / {{ totalPagesReceipt }}
          </div>
          <button @click="currentPageReceipt++" :disabled="currentPageReceipt === totalPagesReceipt"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            Sau <i class="fas fa-chevron-right ml-1.5 text-[10px]"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- RECEIPT STOCKTAKE DRAWER -->
    <div v-if="showRsDrawer && selectedRsReceipt" class="fixed inset-0 z-[1000] flex justify-end"
      style="background:rgba(0,0,0,0.3);backdrop-filter:blur(2px);" @click.self="showRsDrawer=false">
      <div class="w-full max-w-[860px] bg-white h-full flex flex-col shadow-2xl animate-slide-in">
        <!-- Header -->
        <div class="theme-modal-header relative overflow-hidden flex items-center justify-between px-8 py-6 transition-colors duration-500" :class="{ 'easter-egg-space': isSpaceEasterEgg }">
          <template v-if="isSpaceEasterEgg">
            <!-- Space Easter Egg Decor -->
            <div class="absolute inset-0 pointer-events-none">
              <i class="fas fa-rocket absolute top-4 right-32 text-white/80 text-4xl animate-[bounce_3s_infinite] -rotate-45"></i>
              <i class="fas fa-meteor absolute -top-4 right-16 text-orange-400/60 text-6xl rotate-[120deg] drop-shadow-[0_0_15px_rgba(251,146,60,0.8)]"></i>
              <i class="fas fa-user-astronaut absolute bottom-2 right-64 text-white/60 text-3xl animate-[bounce_4s_infinite]"></i>
              <i class="fas fa-star absolute top-2 right-48 text-white/90 text-[8px] animate-pulse"></i>
              <i class="fas fa-star absolute bottom-4 right-20 text-white/70 text-[6px] animate-pulse" style="animation-delay: 1s"></i>
              <i class="fas fa-satellite absolute top-8 right-80 text-white/50 text-2xl animate-[spin_20s_linear_infinite]"></i>
            </div>
          </template>
          <template v-else>
            <!-- Light Mode Decor: Sun & Clouds -->
            <div :key="'light-rs-detail-' + selectedRsReceipt?.id" class="theme-light-decor absolute inset-0 pointer-events-none transition-all duration-500">
              <i class="fas fa-sun absolute -top-12 right-8 text-yellow-300 text-[140px] opacity-10 animate-[spin_40s_linear_infinite]"></i>
              <i class="fas fa-sun absolute top-3 right-24 text-yellow-300 text-5xl drop-shadow-[0_0_20px_rgba(253,224,71,0.8)] animate-[spin_20s_linear_infinite]"></i>
              <i class="fas fa-cloud absolute top-8 right-44 text-white/50 text-5xl drop-shadow-sm"></i>
              <i class="fas fa-cloud absolute top-2 right-64 text-white/40 text-3xl"></i>
              <i class="fas fa-cloud absolute -bottom-2 right-28 text-white/30 text-7xl"></i>
            </div>

            <!-- Dark Mode Decor: Moon & Stars -->
            <div :key="'dark-rs-detail-' + selectedRsReceipt?.id" class="theme-dark-decor absolute inset-0 pointer-events-none transition-all duration-500">
              <i class="fas fa-moon absolute -top-8 right-12 text-blue-100 text-[120px] opacity-[0.03] -rotate-12"></i>
              <i class="fas fa-moon absolute top-3 right-24 text-yellow-200 text-4xl drop-shadow-[0_0_15px_rgba(254,240,138,0.5)] -rotate-12"></i>
              <i class="fas fa-star absolute top-4 right-48 text-white/80 text-[8px] animate-pulse"></i>
              <i class="fas fa-star absolute top-8 right-60 text-white/60 text-[10px] animate-pulse" style="animation-delay: 1s"></i>
              <i class="fas fa-star absolute top-3 right-72 text-white/90 text-[6px] animate-pulse" style="animation-delay: 0.5s"></i>
              <i class="fas fa-star absolute bottom-4 right-36 text-white/50 text-[12px] animate-pulse" style="animation-delay: 1.5s"></i>
              <i class="fas fa-star absolute bottom-2 right-56 text-white/70 text-[8px] animate-pulse"></i>
            </div>
          </template>

          <div class="relative z-10 text-white drop-shadow-md">
            <div class="flex items-center gap-3">
              <h3 class="text-lg font-bold text-white m-0">Xác nhận kiểm kê: {{ selectedRsReceipt.code }}</h3>
              <span :class="['px-2.5 py-0.5 rounded-full text-[11px] font-bold border border-white/20 bg-white/10 text-white']">
                {{ rsTypeLabel(selectedRsReceipt.type) }}
              </span>
            </div>
            <div class="text-xs text-white/80 mt-1">
              Từ: <span class="font-bold text-white">{{ selectedRsReceipt.sourceBranchName || '—' }}</span>
              → Đến: <span class="font-bold text-white">{{ selectedRsReceipt.destBranchName || '—' }}</span>
              | Lập bởi: <span class="font-bold text-white">{{ selectedRsReceipt.createdByName }}</span>
            </div>
          </div>
          <button @click="showRsDrawer=false" class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10">
            <i class="fas fa-times text-lg"></i>
          </button>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto p-8">
          <div v-if="rsDetailLoading" class="flex items-center justify-center py-16">
            <i class="fas fa-spinner fa-spin text-2xl text-purple-400"></i>
          </div>
          <div v-else class="space-y-4">
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wider">
            {{ selectedRsReceipt.status === 'PENDING_STOCKTAKE' ? 'Nhập số lượng thực tế nhận được' : 'Thông tin số lượng thực tế nhận được' }}
          </div>
          <div class="border border-[#e2e8f0] rounded-xl overflow-hidden">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="bg-[#f8f9fa] border-b border-[#e2e8f0]">
                  <th class="p-3 pl-5 text-xs font-bold text-[#8094ae]">Sản phẩm</th>
                  <th class="p-3 text-xs font-bold text-[#8094ae]">Lô SX</th>
                  <th class="p-3 text-right text-xs font-bold text-[#8094ae]">SL gửi</th>
                  <th class="p-3 text-center text-xs font-bold text-[#8094ae] w-[120px]">SL thực nhận</th>
                  <th class="p-3 text-xs font-bold text-[#8094ae]">Lý do hao hụt</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedRsReceipt.confirmItems" :key="item.receiptDetailId"
                  :class="['border-b border-[#f1f5f9] last:border-b-0', item.actualQuantity < item.sentQty ? 'bg-amber-50/50' : '']">
                  <td class="p-3 pl-5">
                    <div class="font-bold text-[#364a63]">{{ item.productName }}</div>
                    <div class="text-[11px] font-mono text-[#8094ae]">{{ item.productSku }}</div>
                  </td>
                  <td class="p-3 font-mono text-xs">{{ item.batchCode }}</td>
                  <td class="p-3 text-right font-mono font-bold">{{ item.sentQty }}</td>
                  <td class="p-3 text-center">
                    <input v-model.number="item.actualQuantity" type="number" :min="0" :max="item.sentQty"
                      :disabled="selectedRsReceipt.status !== 'PENDING_STOCKTAKE'"
                      class="w-20 h-9 border rounded-lg text-center font-mono font-bold text-[#364a63] outline-none focus:ring-2 disabled:bg-slate-50 disabled:text-slate-500 disabled:border-slate-200"
                      :class="item.actualQuantity < item.sentQty ? 'border-amber-400 focus:ring-amber-200' : 'border-[#e2e8f0] focus:ring-[var(--accent-500)]/20'" />
                  </td>
                  <td class="p-3">
                    <textarea v-if="selectedRsReceipt.status === 'PENDING_STOCKTAKE' && item.actualQuantity < item.sentQty" v-model="item.shortfallReason"
                      placeholder="Bắt buộc nhập lý do..." maxlength="200" rows="2"
                      class="w-full py-1.5 px-3 border border-amber-400 bg-amber-50 rounded-lg text-xs outline-none focus:ring-2 focus:ring-amber-200 resize-none"></textarea>
                    <span v-else-if="item.actualQuantity < item.sentQty" class="text-xs text-amber-700 font-semibold bg-amber-50 px-2.5 py-1.5 rounded-lg border border-amber-200 block whitespace-pre-wrap">
                      {{ item.shortfallReason || 'Không có lý do' }}
                    </span>
                    <span v-else class="text-xs text-slate-400">—</span>
                  </td>
                </tr>
              </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="px-8 py-5 border-t border-[#f1f5f9] bg-[#f8f9fa] flex gap-3 justify-between items-center">
          <button @click="showRsDrawer=false" class="px-6 h-11 border border-slate-200 bg-white hover:bg-slate-50 text-[#364a63] rounded-xl font-bold text-sm">
            Đóng
          </button>
          <button v-if="selectedRsReceipt.status === 'PENDING_STOCKTAKE'" @click="submitRsConfirm" :disabled="submittingRs || rsDetailLoading"
            class="px-8 h-11 bg-purple-600 hover:bg-purple-700 text-white rounded-xl font-bold text-sm flex items-center gap-2 shadow-sm disabled:opacity-60">
            <i v-if="submittingRs" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-clipboard-check"></i>
            {{ submittingRs ? 'Đang xác nhận...' : 'Xác nhận kiểm kê & Nhập kho' }}
          </button>
        </div>
      </div>
    </div>
    </template><!-- end receipt tab -->

    <!-- RECEIPT DETAIL MODAL -->
    <AppModal
      :show="showReceiptModal"
      title="Chi tiết phiếu điều chỉnh kho"
      size="lg"
      @close="showReceiptModal = false"
    >
      <div v-if="receiptLoading" class="flex flex-col items-center justify-center py-16">
        <i class="fas fa-spinner fa-spin text-2xl text-[var(--accent-500)] mb-3"></i>
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
              <span v-if="selectedReceipt.type === 'ADJUST_IN'" class="text-blue-600">Tăng tồn kho</span>
              <span v-else class="text-amber-600">Giảm tồn kho</span>
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
                  <th class="p-3 text-xs font-bold text-[#8094ae] text-right pr-5">Số lượng</th>
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
                  <td class="p-3 text-right font-mono font-bold pr-5">{{ item.quantity }}</td>
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

    <!-- CREATE CONFIRMATION -->
    <ConfirmDialog
      :show="showCreateConfirm"
      title="Khởi tạo kiểm kê"
      message="Bạn có chắc chắn muốn khởi tạo một đợt kiểm kê mới? Thao tác này sẽ chốt số liệu hệ thống hiện tại để chuẩn bị đối chiếu thực tế."
      type="info"
      :loading="actionLoading"
      @confirm="createStocktake"
      @cancel="showCreateConfirm = false"
    />

    <!-- DUYỆT HOÀN TẤT CONFIRMATION -->
    <ConfirmDialog
      :show="showCompleteConfirm"
      title="Xác nhận nộp kiểm kê"
      message="Hành động này sẽ nộp số liệu thực tế kiểm đếm. Nếu có chênh lệch, phiếu sẽ chuyển sang chờ duyệt. Bạn có chắc chắn muốn nộp?"
      confirmText="Nộp kiểm kê"
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

    <!-- REJECT MODAL -->
    <AppModal
      :show="showRejectConfirm"
      title="Yêu cầu đếm lại"
      size="sm"
      @close="showRejectConfirm = false"
    >
      <div class="p-6 space-y-4">
        <p class="text-sm text-slate-600">Bạn có chắc chắn muốn trả phiếu kiểm kê này về trạng thái Đang kiểm đếm để nhân viên thực hiện đếm lại?</p>
        <div>
          <label class="block text-sm font-bold text-[#364a63] mb-1">Lý do đếm lại <span class="text-rose-500">*</span></label>
          <textarea
            v-model="rejectReason"
            rows="3"
            placeholder="VD: Cột B hàng vẫn còn, hãy đếm kỹ lại..."
            class="w-full p-3 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 outline-none"
          ></textarea>
        </div>
      </div>
      <div class="px-6 py-4 border-t border-[#f1f5f9] bg-[#f8f9fa] flex justify-end gap-3 rounded-b-2xl">
        <button
          @click="showRejectConfirm = false"
          class="px-5 h-10 border border-[#e2e8f0] bg-white hover:bg-slate-50 text-[#364a63] rounded-xl font-bold text-sm transition-all"
        >
          Hủy
        </button>
        <button
          @click="rejectStocktake"
          :disabled="actionLoading"
          class="px-5 h-10 bg-orange-500 hover:bg-orange-600 text-white rounded-xl font-bold text-sm transition-all flex items-center gap-2"
        >
          <i class="fas fa-undo"></i>
          Xác nhận trả về
        </button>
      </div>
    </AppModal>

    <!-- APPROVE DEVIATION MODAL -->
    <AppModal
      :show="showApproveModal"
      :title="hasDeviation ? 'Duyệt chênh lệch tồn kho' : 'Chốt sổ (Không lệch tồn)'"
      size="md"
      @close="showApproveModal = false"
    >
      <div class="p-6 space-y-4">
        <div>
          <label class="block text-sm font-bold text-[#364a63] mb-1">
            {{ hasDeviation ? 'Lý do chênh lệch' : 'Lý do (Ghi chú phạt lỗi)' }} <span class="text-rose-500">*</span>
          </label>
          <textarea
            v-model="approveForm.reason"
            rows="3"
            placeholder="VD: Hàng hỏng, mất mát do lỗi nhân viên..."
            class="w-full p-3 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none"
          ></textarea>
        </div>
        <div v-if="hasDeviation">
          <label class="block text-sm font-bold text-[#364a63] mb-1">Loại đối tượng chịu trách nhiệm</label>
          <div class="flex gap-4 mb-3">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="radio" v-model="approveForm.responsibleType" value="internal" class="text-[#4361ee] focus:ring-[#4361ee]" />
              <span class="text-sm text-slate-700 font-medium">Chọn từ danh sách nhân viên hệ thống</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="radio" v-model="approveForm.responsibleType" value="external" class="text-[#4361ee] focus:ring-[#4361ee]" />
              <span class="text-sm text-slate-700 font-medium">Tự nhập tên (Thủ kho, Khách hàng, Người ngoài...)</span>
            </label>
          </div>
          
          <template v-if="approveForm.responsibleType === 'internal'">
            <select v-model="approveForm.responsibleUserId" class="w-full p-3 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none text-[#364a63]">
              <option value="">-- Bỏ qua (Công ty tự chịu rủi ro) --</option>
              <option v-for="u in systemUsers" :key="u.id" :value="u.id">
                {{ u.fullName }} ({{ u.role }}) - {{ u.username }}
              </option>
            </select>
          </template>
          
          <template v-else>
            <input
              v-model="approveForm.responsiblePersonName"
              type="text"
              placeholder="VD: Ông Bảo (Thủ kho), Khách hàng, Nhà cung cấp X..."
              class="w-full p-3 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none"
            />
          </template>

          <div class="text-xs text-slate-500 mt-2 italic flex items-start gap-1">
            <i class="fas fa-info-circle mt-0.5 text-[#f77f00]/60"></i>
            <span>Hệ thống sẽ tự động sinh các Phiếu Nhập/Xuất điều chỉnh để làm cân bằng tồn kho. Thông tin trách nhiệm sẽ được ghi nhận vào các phiếu này.</span>
          </div>
        </div>

        <div v-else>
          <label class="block text-sm font-bold text-[#364a63] mb-3">Thông tin đối tượng chịu trách nhiệm (Phạt KPI)</label>
          
          <div class="space-y-3 p-4 bg-slate-50 border border-slate-200 rounded-xl">
            <div>
              <span class="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">Nhân viên hệ thống (Người lập phiếu/Nhập liệu)</span>
              <div class="px-3 py-2.5 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-600 font-medium">
                {{ selectedStocktake?.createdByName || 'Không xác định' }}
              </div>
            </div>
            
            <div>
              <span class="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">Nhân viên kho (Người trực tiếp đếm) <span class="text-rose-500">*</span></span>
              <input
                v-model="approveForm.warehouseWorkerName"
                type="text"
                placeholder="Nhập tên thủ kho / nhân viên đếm hàng..."
                class="w-full p-2.5 border border-[#e2e8f0] bg-white rounded-lg text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all"
              />
            </div>
          </div>

          <div class="text-xs text-slate-500 mt-2 italic flex items-start gap-1">
            <i class="fas fa-info-circle mt-0.5 text-[#4361ee]/60"></i>
            <span>Hệ thống KHÔNG sinh phiếu điều chỉnh kho vì số lượng thực tế đã khớp hoàn toàn. Biên bản quy trách nhiệm sẽ được lưu vĩnh viễn vào lịch sử phiếu này.</span>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
          <button @click="showApproveModal = false" class="px-5 py-2.5 rounded-lg border border-slate-200 text-slate-600 font-bold text-sm">Hủy</button>
          <button @click="approveStocktakeDeviation" :disabled="actionLoading" class="px-5 py-2.5 rounded-lg bg-emerald-500 text-white font-bold text-sm shadow-sm flex items-center gap-2">
            <i v-if="actionLoading" class="fas fa-spinner fa-spin"></i> {{ hasDeviation ? 'Duyệt chênh lệch' : 'Chốt sổ & Lưu lỗi' }}
          </button>
        </div>
      </div>
    </AppModal>

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

/* Dynamic Modal Header Styles */
.theme-modal-header {
  background: linear-gradient(135deg, #38bdf8 0%, #0284c7 100%);
}
.theme-light-decor {
  opacity: 1;
  transform: translateY(0);
}
.theme-dark-decor {
  opacity: 0;
  transform: translateY(20px);
}

/* ── Accordion Entrance Animations ── */
@keyframes slideDownHeader {
  0% { transform: translateY(-30px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}
.header-slide-down {
  animation: slideDownHeader 0.34s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes accordionExpand {
  0% { transform: scaleY(0); opacity: 0; }
  100% { transform: scaleY(1); opacity: 1; }
}
.accordion-filter-expand {
  transform-origin: top;
  animation: accordionExpand 0.34s cubic-bezier(0.16, 1, 0.3, 1) both;
  will-change: transform, opacity;
}
.accordion-table-expand {
  transform-origin: top;
  animation: accordionExpand 0.34s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 60ms;
  will-change: transform, opacity;
}

/* ── Horizontal Meet-in-the-Middle Cell Slide ── */
@keyframes slideFromLeft {
  0% { transform: translate3d(-50px, 0, 0); opacity: 0; }
  100% { transform: translate3d(0, 0, 0); opacity: 1; }
}
@keyframes slideFromRight {
  0% { transform: translate3d(50px, 0, 0); opacity: 0; }
  100% { transform: translate3d(0, 0, 0); opacity: 1; }
}

.accordion-row-anim td:nth-child(-n+4) > * {
  animation: slideFromLeft 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: var(--row-delay, 0ms);
  will-change: transform, opacity;
}
.accordion-row-anim td:nth-child(n+5) > * {
  animation: slideFromRight 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: var(--row-delay, 0ms);
  will-change: transform, opacity;
}
</style>

<style>
/* Dark Mode Overrides for Stocktake Modal Header */
html.dark-mode .theme-modal-header {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}
html.dark-mode .theme-modal-header.easter-egg-space {
  background: linear-gradient(135deg, #090a0f 0%, #1b1130 50%, #0c0817 100%) !important;
}
html.dark-mode .theme-light-decor {
  opacity: 0;
  transform: translateY(-20px);
}
html.dark-mode .theme-dark-decor {
  opacity: 1;
  transform: translateY(0);
}
.theme-modal-header.easter-egg-space {
  background: linear-gradient(135deg, #090a0f 0%, #1b1130 50%, #0c0817 100%) !important;
}
</style>
