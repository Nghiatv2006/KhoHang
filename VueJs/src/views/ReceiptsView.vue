<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isManager = computed(() => user.value?.role === 'MANAGER')
// const isStaff = computed(() => user.value?.role === 'STAFF')
const canApprove = computed(() => isAdmin.value || isManager.value)

function canApproveReceipt(r: any) {
  if (r.status !== 'DRAFT') return false;
  if (isAdmin.value) return true;
  
  const isCrossBranchImport = r.type === 'IMPORT' && r.sourceBranchId !== r.destBranchId;
  if (isCrossBranchImport) {
      if (isManager.value && r.sourceBranchId === user.value?.branchId) return true;
      return false;
  }
  
  if (isManager.value && r.createdByRole === 'STAFF') return true;
  return false;
}

function canCancelReceipt(r: any) {
  if (r.status !== 'DRAFT') return false;
  if (isAdmin.value) return true;
  if (!isManager.value) return false;
  
  const isCrossBranchImport = r.type === 'IMPORT' && r.sourceBranchId !== r.destBranchId;
  if (isCrossBranchImport) {
      return r.sourceBranchId === user.value?.branchId || r.destBranchId === user.value?.branchId;
  }
  
  return true; 
}

// ──────────────────────────────────────────────────────────────
// DATA
// ──────────────────────────────────────────────────────────────
const receipts = ref<any[]>([])
const products = ref<any[]>([])
const branches = ref<any[]>([])
const customers = ref<any[]>([])
const loading = ref(true)

// ──────────────────────────────────────────────────────────────
// FILTER
// ──────────────────────────────────────────────────────────────
const filterType = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')
const filterTimeRange = ref('custom')
const filterStartDate = ref('')
const filterEndDate = ref('')

watch(filterTimeRange, (val) => {
  const today = new Date()
  const fmt = (d: Date) => d.toISOString().substring(0, 10)
  
  if (val === 'today') {
    filterStartDate.value = fmt(today)
    filterEndDate.value = fmt(today)
  } else if (val === 'week') {
    const weekAgo = new Date(today)
    weekAgo.setDate(weekAgo.getDate() - 7)
    filterStartDate.value = fmt(weekAgo)
    filterEndDate.value = fmt(today)
  } else if (val === 'last_week') {
    const twoWeeksAgo = new Date(today)
    twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14)
    const oneWeekAgo = new Date(today)
    oneWeekAgo.setDate(oneWeekAgo.getDate() - 7)
    filterStartDate.value = fmt(twoWeeksAgo)
    filterEndDate.value = fmt(oneWeekAgo)
  } else if (val === 'month') {
    const monthAgo = new Date(today)
    monthAgo.setMonth(monthAgo.getMonth() - 1)
    filterStartDate.value = fmt(monthAgo)
    filterEndDate.value = fmt(today)
  } else if (val === 'custom') {
    filterStartDate.value = ''
    filterEndDate.value = ''
  }
})

const filteredReceipts = computed(() => {
  let result = [...receipts.value]
  if (filterType.value) result = result.filter(r => r.type === filterType.value)
  if (filterStatus.value) {
    if (filterStatus.value === 'UNPAID') {
      result = result.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán'))
    } else {
      result = result.filter(r => r.status === filterStatus.value || r.paymentStatus === filterStatus.value)
    }
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(r =>
      r.code?.toLowerCase().includes(kw) ||
      r.createdByName?.toLowerCase().includes(kw) ||
      r.sourceBranchName?.toLowerCase().includes(kw) ||
      r.destBranchName?.toLowerCase().includes(kw)
    )
  }
  if (filterStartDate.value) {
    const start = new Date(filterStartDate.value).setHours(0, 0, 0, 0)
    result = result.filter(r => r.createdAt && new Date(r.createdAt).getTime() >= start)
  }
  if (filterEndDate.value) {
    const end = new Date(filterEndDate.value).setHours(23, 59, 59, 999)
    result = result.filter(r => r.createdAt && new Date(r.createdAt).getTime() <= end)
  }
  return result.sort((a, b) => {
    const da = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const db = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return db - da
  })
})

// ──────────────────────────────────────────────────────────────
// LOAD DATA
// ──────────────────────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const [rRes, pRes, bRes, cRes] = await Promise.all([
      api.get('/api/receipts'),
      api.get('/api/products'),
      api.get('/api/branches'),
      api.get('/api/customers'),
    ])
    if (rRes.ok) receipts.value = await rRes.json()
    if (pRes.ok) products.value = await pRes.json()
    if (bRes.ok) branches.value = await bRes.json()
    if (cRes.ok) customers.value = await cRes.json()
  } catch (e: any) {
    toast.error('Lỗi tải dữ liệu: ' + e.message)
  } finally {
    loading.value = false
  }
}
onMounted(loadData)

// ──────────────────────────────────────────────────────────────
// HEAD BRANCH
// ──────────────────────────────────────────────────────────────
const headBranch = computed(() => branches.value.find(b => b.isHead) || branches.value[0] || null)
// const subBranches = computed(() => branches.value.filter(b => b.id !== headBranch.value?.id))

// ──────────────────────────────────────────────────────────────
// STATS
// ──────────────────────────────────────────────────────────────
const statDraft = computed(() => receipts.value.filter(r => canApproveReceipt(r)).length)
const statCompleted = computed(() => receipts.value.filter(r => r.status === 'COMPLETED').length)
const statCancelled = computed(() => receipts.value.filter(r => r.status === 'CANCELLED').length)
const statInTransit = computed(() => receipts.value.filter(r => r.paymentStatus === 'IN_TRANSIT').length)
const statUnpaid = computed(() => receipts.value.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán')).length)

// ──────────────────────────────────────────────────────────────
// DETAIL PANEL
// ──────────────────────────────────────────────────────────────
const selectedReceipt = ref<any>(null)
const showDetail = ref(false)

async function openDetail(receipt: any) {
  try {
    const res = await api.get(`/api/receipts/${receipt.id}`)
    if (res.ok) {
      selectedReceipt.value = await res.json()
      showDetail.value = true
    }
  } catch (e: any) {
    toast.error('Lỗi tải chi tiết: ' + e.message)
  }
}

// ──────────────────────────────────────────────────────────────
// CREATE DRAFT MODAL
// ──────────────────────────────────────────────────────────────
const showCreateModal = ref(false)
const submittingCreate = ref(false)

const createForm = ref<{
  type: string
  sourceBranchId: number | ''
  destBranchId: number | ''
  customerId: number | ''
  customerName: string
  customerPhone: string
  paymentStatus: string
  description: string
  details: DetailRow[]
}>({
  type: 'IMPORT',
  sourceBranchId: user.value?.branchId || headBranch.value?.id || '',
  destBranchId: '',
  customerId: '',
  customerName: '',
  customerPhone: '',
  paymentStatus: 'UNPAID',
  description: '',
  details: []
})

const showCustomerDropdown = ref(false)

const filteredCustomers = computed(() => {
  if (!createForm.value.customerName) return customers.value
  const kw = createForm.value.customerName.toLowerCase()
  return customers.value.filter(c => 
    c.name.toLowerCase().includes(kw) || 
    (c.contactInfo && c.contactInfo.toLowerCase().includes(kw))
  )
})

function selectCustomer(c: any) {
  createForm.value.customerId = c.id
  createForm.value.customerName = c.name
  createForm.value.customerPhone = c.contactInfo || ''
  showCustomerDropdown.value = false
}

function hideCustomerDropdown() {
  setTimeout(() => { showCustomerDropdown.value = false }, 200)
}

function onCustomerInput() {
  showCustomerDropdown.value = true
  createForm.value.customerId = ''
  // Không tự động xóa phone để người dùng có thể gõ tiếp
}

interface DetailRow {
  productId: number | ''
  manufacturingDate: string
  expirationDate: string
  quantity: number
  price: number
}

function openCreateModal() {
  const defaultType = (user.value?.branchId !== headBranch.value?.id) ? 'IMPORT' : 'EXPORT';
  createForm.value = {
    type: defaultType,
    sourceBranchId: user.value?.branchId || headBranch.value?.id || '',
    destBranchId: '',
    customerId: '',
    customerName: '',
    customerPhone: '',
    paymentStatus: 'UNPAID',
    description: '',
    details: [{ productId: '', manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 }]
  }
  onTypeChange()
  showCreateModal.value = true
}

function addDetailRow() {
  createForm.value.details.push({ productId: '', manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 })
}

function removeDetailRow(index: number) {
  if (createForm.value.details.length <= 1) return
  createForm.value.details.splice(index, 1)
}

function onTypeChange() {
  const t = createForm.value.type
  if (t === 'IMPORT') {
    createForm.value.sourceBranchId = headBranch.value?.id || ''
    createForm.value.destBranchId = user.value?.branchId || headBranch.value?.id || ''
  } else if (t === 'EXPORT') {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  } else if (t === 'TRANSFER') {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  } else {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  }
  createForm.value.details.forEach(d => constrainQuantity(d))
}

const sourceInventories = ref<any[]>([])
const globalInventories = ref<any[]>([])

import { watch } from 'vue'

watch(() => createForm.value.sourceBranchId, async (newVal) => {
  if (newVal) {
    try {
      const res = await api.get('/api/inventories/global')
      if (res.ok) {
        const allInventories = await res.json()
        globalInventories.value = allInventories
        sourceInventories.value = allInventories.filter((inv: any) => inv.branchId === newVal)
        createForm.value.details.forEach(d => constrainQuantity(d))
      } else {
        let errStr = 'Lỗi server';
        try {
          const text = await res.text();
          try {
            const err = JSON.parse(text);
            errStr = err.message || errStr;
          } catch {
            errStr = text || errStr;
          }
        } catch(e) {
          // ignore
        }
        toast.error('Không thể tải tồn kho chi nhánh nguồn: ' + errStr)
        sourceInventories.value = []
      }
    } catch (e: any) {
      toast.error('Lỗi kết nối: ' + e.message)
      console.error(e)
    }
  } else {
    sourceInventories.value = []
  }
}, { immediate: true })

const availableProducts = computed(() => {
  const t = createForm.value.type
  if (t === 'ADJUST_IN') {
    return products.value
  }
  if (createForm.value.sourceBranchId) {
    const inStockIds = new Set(
      sourceInventories.value
        .filter(inv => inv.quantity > 0)
        .map(inv => inv.productId)
    )
    return products.value.filter(p => inStockIds.has(p.id))
  }
  return products.value
})

function getAvailableProductsForRow(index: number) {
  const selectedIds = new Set(
    createForm.value.details
      .map((d, i) => i !== index ? Number(d.productId) : null)
      .filter(id => id !== null && !isNaN(id))
  )
  return availableProducts.value.filter(p => !selectedIds.has(p.id))
}

function onProductChange(row: DetailRow) {
  const p = products.value.find(x => x.id === Number(row.productId))
  if (p) {
    row.price = Number(p.price) || 0
    if (!p.hasExpiry) {
      row.manufacturingDate = '1970-01-01'
      row.expirationDate = '1970-01-01'
    }
  }
  constrainQuantity(row)
}

const selectedProductHasExpiry = (row: DetailRow) => {
  if (!row.productId) return false
  const p = products.value.find(x => x.id === Number(row.productId))
  return p?.hasExpiry ?? false
}

function getMaxQuantity(productId: number | string | null) {
  if (!productId) return null
  if (createForm.value.type === 'ADJUST_IN') return null
  if (!createForm.value.sourceBranchId) return null
  if (createForm.value.type === 'IMPORT' && createForm.value.sourceBranchId === createForm.value.destBranchId) return null
  
  const inv = sourceInventories.value.find(x => x.productId === Number(productId))
  const totalQty = inv ? inv.quantity : 0

  // Trừ đi số lượng đang nằm trong các phiếu nháp chờ xuất/điều chuyển
  const pendingQty = receipts.value
    .filter(r => r.status === 'DRAFT' && r.sourceBranchId === createForm.value.sourceBranchId && 
            (['EXPORT', 'TRANSFER', 'ADJUST_OUT'].includes(r.type) || 
             (r.type === 'IMPORT' && r.sourceBranchId !== r.destBranchId)))
    .flatMap(r => r.details || [])
    .filter(d => Number(d.productId) === Number(productId))
    .reduce((sum, d) => sum + Number(d.quantity), 0)

  return Math.max(0, totalQty - pendingQty)
}

function getGlobalQuantity(productId: number | string | null) {
  if (!productId) return 0
  return globalInventories.value
    .filter(x => x.productId === Number(productId))
    .reduce((sum, inv) => sum + inv.quantity, 0)
}

function constrainQuantity(d: DetailRow) {
  if (d.quantity === null || d.quantity === undefined || (d.quantity as any) === '') return;
  if (createForm.value.type === 'ADJUST_IN' || (createForm.value.type === 'IMPORT' && createForm.value.sourceBranchId === createForm.value.destBranchId)) return;
  const max = getMaxQuantity(d.productId)
  if (max !== null) {
    if (max === 0) {
      d.quantity = 0
      return
    }
    if (d.quantity > max) d.quantity = max
  }
}

function onQuantityBlur(d: DetailRow) {
  if (!d.quantity || d.quantity < 1) {
    d.quantity = 1;
  }
  const max = getMaxQuantity(d.productId)
  if (max !== null && max === 0) {
    d.quantity = 0;
  }
}

async function submitCreateDraft() {
  const f = createForm.value
  if (!f.type) { toast.error('Vui lòng chọn loại phiếu.'); return }
  if (f.details.some(d => !d.productId || d.quantity <= 0)) {
    toast.error('Vui lòng điền đầy đủ sản phẩm và số lượng hợp lệ.')
    return
  }
  if (f.type === 'EXPORT') {
    if (!f.customerName?.trim()) {
      toast.error('Vui lòng nhập tên khách hàng khi xuất bán.')
      return
    }
    if (!f.customerPhone?.trim()) {
      toast.error('Vui lòng nhập số điện thoại khách hàng khi xuất bán.')
      return
    }
  }

  const isConstrained = f.type !== 'ADJUST_IN' && !(f.type === 'IMPORT' && f.sourceBranchId === f.destBranchId)
  if (isConstrained && f.details.some(d => {
    const max = getMaxQuantity(d.productId)
    return max !== null && d.quantity > max
  })) {
    toast.error('Số lượng vượt quá tồn kho hiện tại.')
    return
  }

  const payload: any = {
    type: f.type,
    sourceBranchId: f.sourceBranchId || null,
    destBranchId: f.destBranchId || null,
    customerId: f.customerId || null,
    customerName: f.customerName || null,
    customerPhone: f.customerPhone || null,
    paymentStatus: f.paymentStatus,
    description: f.description,
    details: f.details.map(d => ({
      productId: Number(d.productId),
      quantity: d.quantity,
      price: d.price,
      manufacturingDate: d.manufacturingDate || '1970-01-01',
      expirationDate: d.expirationDate || '1970-01-01',
    }))
  }

  submittingCreate.value = true
  try {
    const res = await api.post('/api/receipts', payload)
    if (res.ok) {
      toast.success('Tạo phiếu kho nháp thành công!')
      showCreateModal.value = false
      await loadData()
    } else {
      let errMsg = `Lỗi ${res.status}: ${res.statusText}`
      try {
        const text = await res.text()
        try {
          const err = JSON.parse(text)
          errMsg = err.message || errMsg
        } catch {
          errMsg = text || errMsg
        }
      } catch (e: any) {
        errMsg = e.message || errMsg
      }
      toast.error(errMsg)
    }
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    submittingCreate.value = false
  }
}

// ──────────────────────────────────────────────────────────────
// APPROVE
// ──────────────────────────────────────────────────────────────
const approvingId = ref<number | null>(null)

async function approveReceipt(receipt: any) {
  if (!confirm(`Xác nhận PHÊ DUYỆT phiếu ${receipt.code}?\nSau khi duyệt, tồn kho sẽ được cập nhật ngay và phiếu sẽ bị khóa.`)) return
  approvingId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/approve`, {})
    if (res.ok) {
      toast.success(`Phiếu ${receipt.code} đã được phê duyệt thành công!`)
      showDetail.value = false
      await loadData()
    } else {
      let errMessage = 'Lỗi khi duyệt phiếu.'
      try {
        const err = await res.json()
        errMessage = err.message || errMessage
      } catch {
        const text = await res.text()
        errMessage = text ? text.substring(0, 100) + '...' : errMessage
      }
      toast.error(errMessage)
    }
  } catch (e: any) {
    toast.error('Lỗi: ' + e.message)
  } finally {
    approvingId.value = null
  }
}

// ──────────────────────────────────────────────────────────────
// CANCEL
// ──────────────────────────────────────────────────────────────
async function cancelReceipt(receipt: any) {
  if (!confirm(`Xác nhận HỦY phiếu ${receipt.code}?`)) return
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/cancel`, {})
    if (res.ok) {
      toast.success(`Phiếu ${receipt.code} đã được hủy.`)
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lỗi khi hủy phiếu.')
    }
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
  }
}

// ──────────────────────────────────────────────────────────────
// MARK AS PAID
// ──────────────────────────────────────────────────────────────
const markingPaidId = ref<number | null>(null)

async function markAsPaid(receipt: any) {
  if (!confirm(`Xác nhận THANH TOÁN cho phiếu ${receipt.code}? Công nợ khách hàng sẽ được trừ tương ứng.`)) return
  markingPaidId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/mark-paid`, {})
    if (res.ok) {
      toast.success(`Phiếu ${receipt.code} đã được thanh toán thành công!`)
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lỗi khi xác nhận thanh toán.')
    }
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    markingPaidId.value = null
  }
}


// ──────────────────────────────────────────────────────────────
// CONFIRM TRANSFER MODAL
// ──────────────────────────────────────────────────────────────
const showConfirmModal = ref(false)
const confirmingReceipt = ref<any>(null)
const confirmItems = ref<{ receiptDetailId: number; productName: string; sentQty: number; actualQuantity: number }[]>([])
const submittingConfirm = ref(false)

function openConfirmTransferModal(receipt: any) {
  confirmingReceipt.value = receipt
  confirmItems.value = (receipt.details || []).map((d: any) => ({
    receiptDetailId: d.id,
    productName: d.productName,
    sentQty: d.quantity,
    actualQuantity: d.quantity
  }))
  showConfirmModal.value = true
}

async function submitConfirmTransfer() {
  if (confirmItems.value.some(i => i.actualQuantity < 0)) {
    toast.error('Số lượng nhận không được âm.'); return
  }
  submittingConfirm.value = true
  try {
    const payload = {
      items: confirmItems.value.map(i => ({
        receiptDetailId: i.receiptDetailId,
        actualQuantity: i.actualQuantity
      }))
    }
    const res = await api.post(`/api/receipts/${confirmingReceipt.value.id}/confirm-transfer`, payload)
    if (res.ok) {
      toast.success('Xác nhận nhận hàng thành công! Tồn kho đã được cập nhật.')
      showConfirmModal.value = false
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lỗi khi xác nhận nhận hàng.')
    }
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    submittingConfirm.value = false
  }
}

// ──────────────────────────────────────────────────────────────
// HELPERS
// ──────────────────────────────────────────────────────────────
function formatDate(s: string) {
  if (!s || s.startsWith('1970-01-01')) return '-'
  try { return new Date(s).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) }
  catch { return s }
}
function formatDateTime(s: string) {
  if (!s) return '-'
  try { return new Date(s).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) }
  catch { return s }
}
function formatVND(v: number) {
  return new Intl.NumberFormat('vi-VN').format(v) + 'đ'
}

function typeLabel(t: string) {
  const map: Record<string, string> = {
    IMPORT: 'Nhập kho', EXPORT: 'Xuất bán', TRANSFER: 'Điều chuyển',
    ADJUST_IN: 'Cân bằng +', ADJUST_OUT: 'Cân bằng -'
  }
  return map[t] || t
}
function typeClass(t: string) {
  const map: Record<string, string> = {
    IMPORT: 'bg-blue-100 text-blue-700',
    EXPORT: 'bg-purple-100 text-purple-700',
    TRANSFER: 'bg-orange-100 text-orange-700',
    ADJUST_IN: 'bg-emerald-100 text-emerald-700',
    ADJUST_OUT: 'bg-rose-100 text-rose-700'
  }
  return map[t] || 'bg-gray-100 text-gray-600'
}
function statusClass(s: string) {
  const map: Record<string, string> = {
    DRAFT: 'bg-yellow-100 text-yellow-700 border border-yellow-300',
    COMPLETED: 'bg-green-100 text-green-700 border border-green-300',
    CANCELLED: 'bg-red-100 text-red-600 border border-red-300'
  }
  return map[s] || 'bg-gray-100 text-gray-600'
}
function paymentStatusLabel(p: string) {
  const map: Record<string, string> = {
    UNPAID: 'Chưa thanh toán', PAID: 'Đã thanh toán',
    IN_TRANSIT: 'Đang vận chuyển', RECEIVED: 'Đã nhận hàng'
  }
  return map[p] || p
}
function paymentStatusClass(p: string) {
  const map: Record<string, string> = {
    UNPAID: 'bg-amber-50 text-amber-600',
    PAID: 'bg-green-50 text-green-600',
    IN_TRANSIT: 'bg-sky-50 text-sky-600',
    RECEIVED: 'bg-teal-50 text-teal-700'
  }
  return map[p] || ''
}

// Can the current user confirm transfer for this receipt?
function canConfirmTransfer(receipt: any) {
  if (receipt.type !== 'TRANSFER') return false
  if (receipt.status !== 'COMPLETED') return false
  if (receipt.paymentStatus !== 'IN_TRANSIT') return false
  if (isAdmin.value) return true
  // Must be dest branch
  return receipt.destBranchId === user.value?.branchId
}

function getCustomerName(id: number | null | undefined) {
  if (!id) return '—'
  const c = customers.value.find((x: any) => x.id === id)
  return c ? c.name : '—'
}

function getCustomerContactInfo(id: number | null | undefined) {
  if (!id) return ''
  const c = customers.value.find((x: any) => x.id === id)
  return c && c.contactInfo ? c.contactInfo : ''
}
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto font-['Inter',sans-serif]">

    <!-- HEADER -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0 flex items-center gap-3">
          <i class="fas fa-file-invoice text-[#4361ee]"></i>
          Quản lý Phiếu Kho
        </h2>
        <p class="text-[#8094ae] text-sm mt-1">Theo dõi, lập và phê duyệt các phiếu nhập/xuất/điều chuyển kho</p>
      </div>
      <button
        v-if="!isAdmin"
        @click="openCreateModal"
        class="h-[42px] bg-[#4361ee] hover:bg-[#3a0ca3] text-white px-5 rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center gap-2"
      >
        <i class="fas fa-plus"></i> Lập phiếu nháp
      </button>
    </div>

    <!-- STAT CARDS -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div @click="filterStatus = filterStatus === 'DRAFT' ? '' : 'DRAFT'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'DRAFT' ? 'border-yellow-400 ring-2 ring-yellow-200' : 'border-[#f1f5f9] hover:border-yellow-300']">
        <div class="w-12 h-12 rounded-xl bg-yellow-50 flex items-center justify-center text-yellow-500 text-xl">
          <i class="fas fa-pencil-alt"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Chờ duyệt</div>
          <div class="text-2xl font-extrabold text-yellow-500">{{ statDraft }}</div>
        </div>
      </div>
      <div @click="filterStatus = filterStatus === 'COMPLETED' ? '' : 'COMPLETED'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'COMPLETED' ? 'border-green-400 ring-2 ring-green-200' : 'border-[#f1f5f9] hover:border-green-300']">
        <div class="w-12 h-12 rounded-xl bg-green-50 flex items-center justify-center text-green-500 text-xl">
          <i class="fas fa-check-circle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Đã duyệt</div>
          <div class="text-2xl font-extrabold text-green-500">{{ statCompleted }}</div>
        </div>
      </div>

      <div @click="filterStatus = filterStatus === 'CANCELLED' ? '' : 'CANCELLED'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'CANCELLED' ? 'border-red-400 ring-2 ring-red-200' : 'border-[#f1f5f9] hover:border-red-300']">
        <div class="w-12 h-12 rounded-xl bg-red-50 flex items-center justify-center text-red-400 text-xl">
          <i class="fas fa-times-circle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Đã hủy</div>
          <div class="text-2xl font-extrabold text-red-400">{{ statCancelled }}</div>
        </div>
      </div>
      <div @click="filterStatus = filterStatus === 'UNPAID' ? '' : 'UNPAID'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'UNPAID' ? 'border-orange-400 ring-2 ring-orange-200' : 'border-[#f1f5f9] hover:border-orange-300']">
        <div class="w-12 h-12 rounded-xl bg-orange-50 flex items-center justify-center text-orange-400 text-xl">
          <i class="fas fa-file-invoice-dollar"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Chưa thanh toán</div>
          <div class="text-2xl font-extrabold text-orange-400">{{ statUnpaid }}</div>
        </div>
      </div>
    </div>

    <!-- TABLE CARD -->
    <div class="bg-white rounded-2xl border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-sm overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9]">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Tìm kiếm đa năng -->
          <div class="lg:col-span-2 relative">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae] text-sm"></i>
            <input v-model="searchKeyword" type="text" placeholder="Tìm kiếm mã phiếu, người lập, chi nhánh..."
              class="w-full h-11 pl-10 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all" />
          </div>
          <!-- Lọc loại phiếu -->
          <div>
            <select v-model="filterType"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="">-- Tất cả loại phiếu --</option>
              <option value="IMPORT">Nhập kho</option>
              <option value="EXPORT">Xuất bán</option>
              <option value="TRANSFER">Điều chuyển</option>
            </select>
          </div>
          <!-- Lọc trạng thái -->
          <div>
            <select v-model="filterStatus"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="">-- Tất cả trạng thái --</option>
              <option value="DRAFT">Chờ duyệt</option>
              <option value="COMPLETED">Đã duyệt</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="IN_TRANSIT">Đang vận chuyển</option>
              <option value="RECEIVED">Đã nhận hàng</option>
            </select>
          </div>
          <!-- Thời gian và Ngày -->
          <div class="lg:col-span-4 grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Thời gian</label>
              <select v-model="filterTimeRange" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
                <option value="today">Hôm nay</option>
                <option value="week">7 ngày qua</option>
                <option value="last_week">Tuần trước (14 ngày qua)</option>
                <option value="month">30 ngày qua</option>
                <option value="custom">Tùy chọn...</option>
              </select>
            </div>
            <!-- Từ ngày -->
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Từ ngày</label>
              <input v-model="filterStartDate" type="date" :disabled="filterTimeRange !== 'custom'"
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed" />
            </div>
            <!-- Đến ngày -->
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Đến ngày</label>
              <input v-model="filterEndDate" type="date" :disabled="filterTimeRange !== 'custom'"
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed" />
            </div>
            <!-- Nút Xóa lọc -->
            <div class="flex items-end">
              <button v-if="filterType || filterStatus || searchKeyword || filterStartDate || filterEndDate || filterTimeRange !== 'custom'"
                @click="filterType = ''; filterStatus = ''; searchKeyword = ''; filterTimeRange = 'custom'; filterStartDate = ''; filterEndDate = ''"
                class="w-full h-11 flex items-center justify-center gap-2 px-6 bg-white border border-[#e2e8f0] rounded-xl text-sm font-semibold text-[#8094ae] hover:text-[#364a63] hover:bg-[#f8f9fa] transition-all shadow-sm">
                <i class="fas fa-times"></i> Xóa lọc
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center h-48 gap-3 text-[#8094ae]">
        <i class="fas fa-spinner fa-spin text-2xl"></i>
        <span class="font-semibold">Đang tải dữ liệu...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredReceipts.length === 0" class="text-center py-16 text-[#8094ae]">
        <i class="fas fa-inbox text-5xl mb-4 opacity-30"></i>
        <p class="font-semibold">Không có phiếu kho nào phù hợp</p>
      </div>

      <!-- Table -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-[#f8f9fa] text-[#8094ae] text-xs uppercase tracking-wider">
              <th class="px-5 py-3 text-left font-bold">Mã phiếu</th>
              <th class="px-5 py-3 text-left font-bold">Loại</th>
              <th class="px-5 py-3 text-left font-bold">Trạng thái</th>
              <th class="px-5 py-3 text-left font-bold">Chi nhánh nguồn</th>
              <th class="px-5 py-3 text-left font-bold">Đích / Khách hàng</th>
              <th class="px-5 py-3 text-left font-bold">Người lập</th>
              <th class="px-5 py-3 text-left font-bold">Ngày tạo</th>
              <th class="px-5 py-3 text-center font-bold">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[#f1f5f9]">
            <tr v-for="r in filteredReceipts" :key="r.id"
              @dblclick="openDetail(r)"
              class="hover:bg-[#f8f9fa]/80 cursor-pointer transition-colors group">
              <td class="px-5 py-4">
                <span class="font-mono font-bold text-[#4361ee] text-xs">{{ r.code }}</span>
              </td>
              <td class="px-5 py-4">
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', typeClass(r.type)]">
                  {{ typeLabel(r.type) }}
                </span>
              </td>
              <td class="px-5 py-4">
                <div class="flex flex-col gap-1">
                  <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-bold', statusClass(r.status)]">
                    {{ r.status === 'DRAFT' ? '⏳ Chờ duyệt' : r.status === 'COMPLETED' ? '✅ Đã duyệt' : '❌ Đã hủy' }}
                  </span>
                  <span v-if="r.paymentStatus === 'IN_TRANSIT'" :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold', paymentStatusClass(r.paymentStatus)]">
                    🚚 Đang vận chuyển
                  </span>
                  <span v-else-if="r.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold', paymentStatusClass(r.paymentStatus)]">
                    📦 Đã nhận hàng
                  </span>
                </div>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#364a63] font-medium">{{ r.sourceBranchName || '—' }}</span>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#364a63] font-medium" v-if="r.type === 'EXPORT'">{{ getCustomerName(r.customerId) }}</span>
                <span class="text-[#364a63] font-medium" v-else>{{ r.destBranchName || '—' }}</span>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#8094ae]">{{ r.createdByName }}</span>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#8094ae] text-xs">{{ formatDateTime(r.createdAt) }}</span>
              </td>
              <td class="px-5 py-4">
                <div class="flex items-center justify-center gap-2">
                  <button @click.stop="openDetail(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-[#f1f5f9] hover:bg-[#4361ee] hover:text-white text-[#8094ae] transition-all"
                    title="Xem chi tiết">
                    <i class="fas fa-eye text-xs"></i>
                  </button>
                  <button v-if="canApproveReceipt(r)"
                    @click.stop="approveReceipt(r)"
                    :disabled="approvingId === r.id"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-green-50 hover:bg-green-500 hover:text-white text-green-600 transition-all disabled:opacity-50"
                    title="Phê duyệt">
                    <i class="fas fa-check text-xs"></i>
                  </button>
                  <button v-if="canCancelReceipt(r)"
                    @click.stop="cancelReceipt(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-red-50 hover:bg-red-500 hover:text-white text-red-500 transition-all"
                    title="Hủy phiếu">
                    <i class="fas fa-times text-xs"></i>
                  </button>
                  <button v-if="canConfirmTransfer(r)"
                    @click.stop="openConfirmTransferModal(r)"
                    class="h-8 px-3 flex items-center justify-center rounded-lg bg-sky-50 hover:bg-sky-500 hover:text-white text-sky-600 transition-all text-xs font-bold"
                    title="Xác nhận nhận hàng">
                    <i class="fas fa-truck-loading mr-1"></i>Xác nhận
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- DETAIL PANEL MODAL -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showDetail && selectedReceipt" @click="showDetail = false" class="fixed inset-0 bg-slate-900/20 backdrop-blur-[2px] z-[100]"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showDetail && selectedReceipt" class="fixed inset-y-0 right-0 z-[101] w-full max-w-[700px] bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] flex flex-col border-l border-[#e2e8f0]">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-[#4361ee] to-[#4cc9f0] text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">Chi tiết phiếu kho</div>
              <div class="font-mono font-bold text-lg">{{ selectedReceipt.code }}</div>
            </div>
            <button @click="showDetail = false" class="w-9 h-9 flex items-center justify-center rounded-xl bg-white/20 hover:bg-white/30 transition-all">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="overflow-y-auto flex-1 p-6 space-y-5 custom-scrollbar">
            <!-- Meta info -->
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Loại phiếu</div>
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', typeClass(selectedReceipt.type)]">
                  {{ typeLabel(selectedReceipt.type) }}
                </span>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Trạng thái</div>
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', statusClass(selectedReceipt.status)]">
                  {{ selectedReceipt.status }}
                </span>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nhánh nguồn</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.sourceBranchName || '—' }}</div>
              </div>
              <div v-if="selectedReceipt.type === 'EXPORT'">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Khách hàng</div>
                <div class="font-semibold text-[#364a63]">{{ getCustomerName(selectedReceipt.customerId) }}</div>
                <div class="text-xs text-[#8094ae] mt-0.5" v-if="getCustomerContactInfo(selectedReceipt.customerId)">{{ getCustomerContactInfo(selectedReceipt.customerId) }}</div>
              </div>
              <div v-else>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nhánh đích</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.destBranchName || '—' }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Người lập</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.createdByName }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ngày tạo</div>
                <div class="font-semibold text-[#364a63]">{{ formatDateTime(selectedReceipt.createdAt) }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Thanh toán</div>
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', paymentStatusClass(selectedReceipt.paymentStatus)]">
                  {{ paymentStatusLabel(selectedReceipt.paymentStatus) }}
                </span>
              </div>
              <div v-if="selectedReceipt.description">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ghi chú</div>
                <div class="text-[#364a63] text-xs">{{ selectedReceipt.description }}</div>
              </div>
            </div>

            <!-- Detail lines -->
            <div>
              <div class="text-xs font-bold text-[#8094ae] uppercase mb-3">Danh sách hàng hóa</div>
              <div class="rounded-xl border border-[#f1f5f9] overflow-hidden">
                <table class="w-full text-xs">
                  <thead>
                    <tr class="bg-[#f8f9fa] text-[#8094ae] uppercase">
                      <th class="px-4 py-2.5 text-left font-bold">Sản phẩm</th>
                      <th class="px-4 py-2.5 text-center font-bold">NSX</th>
                      <th class="px-4 py-2.5 text-center font-bold">HSD</th>
                      <th class="px-4 py-2.5 text-right font-bold">SL</th>
                      <th class="px-4 py-2.5 text-right font-bold">Đơn giá</th>
                      <th class="px-4 py-2.5 text-right font-bold">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-[#f1f5f9]">
                    <tr v-for="d in selectedReceipt.details" :key="d.id" class="hover:bg-[#f8f9fa]/50">
                      <td class="px-4 py-3 font-semibold text-[#364a63]">{{ d.productName }}</td>
                      <td class="px-4 py-3 text-center text-[#8094ae]">{{ formatDate(d.manufacturingDate) }}</td>
                      <td class="px-4 py-3 text-center text-[#8094ae]">{{ formatDate(d.expirationDate) }}</td>
                      <td class="px-4 py-3 text-right font-bold">{{ d.quantity }}</td>
                      <td class="px-4 py-3 text-right">{{ formatVND(d.price) }}</td>
                      <td class="px-4 py-3 text-right font-bold text-[#4361ee]">{{ formatVND(d.quantity * d.price) }}</td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr class="bg-[#f8f9fa]">
                      <td colspan="5" class="px-4 py-2.5 text-right font-bold text-[#8094ae] text-xs uppercase">Tổng cộng</td>
                      <td class="px-4 py-2.5 text-right font-extrabold text-[#4361ee]">
                        {{ formatVND((selectedReceipt.details || []).reduce((s: number, d: any) => s + d.quantity * d.price, 0)) }}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="px-6 py-4 border-t border-[#f1f5f9] flex items-center justify-between gap-3 bg-[#f8f9fa]/50">
            <button @click="showDetail = false" class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63] text-sm transition-all hover:bg-[#f1f5f9]">
              Đóng
            </button>
            <div class="flex gap-2">
              <button v-if="selectedReceipt.status === 'DRAFT' && canApprove"
                @click="cancelReceipt(selectedReceipt)"
                class="px-5 py-2.5 bg-red-50 border border-red-200 text-red-600 rounded-xl font-bold text-sm hover:bg-red-500 hover:text-white transition-all">
                <i class="fas fa-times mr-2"></i>Hủy phiếu
              </button>
              <button v-if="canApproveReceipt(selectedReceipt)"
                @click="approveReceipt(selectedReceipt)"
                :disabled="approvingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-[#4361ee] text-white rounded-xl font-bold text-sm hover:bg-[#3a0ca3] transition-all disabled:opacity-60 flex items-center gap-2">
                <i class="fas fa-check-circle"></i>Phê duyệt
              </button>
              <button v-if="selectedReceipt.type === 'EXPORT' && selectedReceipt.status === 'COMPLETED' && selectedReceipt.paymentStatus === 'UNPAID' && canApprove"
                @click="markAsPaid(selectedReceipt)"
                :disabled="markingPaidId === selectedReceipt.id"
                class="px-5 py-2.5 bg-emerald-500 text-white rounded-xl font-bold text-sm hover:bg-emerald-600 transition-all disabled:opacity-60 flex items-center gap-2">
                <i class="fas fa-hand-holding-usd"></i>Xác nhận thanh toán
              </button>
              <button v-if="canConfirmTransfer(selectedReceipt)"
                @click="openConfirmTransferModal(selectedReceipt)"
                class="px-5 py-2.5 bg-sky-500 text-white rounded-xl font-bold text-sm hover:bg-sky-600 transition-all flex items-center gap-2">
                <i class="fas fa-truck-loading"></i>Xác nhận nhận hàng
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- CREATE DRAFT MODAL -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showCreateModal" @click="showCreateModal = false" class="fixed inset-0 bg-slate-900/20 backdrop-blur-[2px] z-[100]"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showCreateModal" class="fixed inset-y-0 right-0 z-[101] w-full max-w-[800px] bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] flex flex-col border-l border-[#e2e8f0]">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-[#4361ee] to-[#4cc9f0] text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">Lập phiếu kho</div>
              <div class="font-bold text-lg">Tạo phiếu nháp (DRAFT)</div>
            </div>
            <button @click="showCreateModal = false" class="w-9 h-9 flex items-center justify-center rounded-xl bg-white/20 hover:bg-white/30 transition-all">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <!-- Body -->
          <div class="overflow-y-auto flex-1 p-6 bg-[#f8f9fa] custom-scrollbar">
            <div class="space-y-6">
              <!-- Type & Branches -->
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Loại phiếu <span class="text-red-500">*</span></label>
                  <select v-model="createForm.type" @change="onTypeChange"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="IMPORT" v-if="user?.branchId !== headBranch?.id">📥 Nhập kho</option>
                    <option value="EXPORT">📤 Xuất bán</option>
                    <option value="TRANSFER">🔄 Điều chuyển</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh nguồn</label>
                  <select v-model="createForm.sourceBranchId" disabled
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae] cursor-not-allowed">
                    <option v-if="headBranch" :value="headBranch.id">{{ headBranch.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT' || createForm.type === 'TRANSFER' || createForm.type === 'ADJUST_IN'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh đích</label>
                  <select v-model="createForm.destBranchId" :disabled="createForm.type === 'IMPORT'"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae]">
                    <option value="">-- Chọn chi nhánh --</option>
                    <option v-for="b in branches.filter(x => x.id !== createForm.sourceBranchId)" :key="b.id" :value="b.id">{{ b.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'EXPORT'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Khách hàng <span class="text-red-500">*</span></label>
                  <div class="relative">
                    <input v-model="createForm.customerName" @focus="showCustomerDropdown = true" @blur="hideCustomerDropdown" @input="onCustomerInput" type="text" placeholder="Nhập tên khách hàng..."
                      class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                    <div v-if="showCustomerDropdown && filteredCustomers.length > 0" class="absolute z-10 w-full mt-1 bg-white border border-[#e2e8f0] rounded-xl shadow-lg max-h-48 overflow-y-auto">
                       <div v-for="c in filteredCustomers" :key="c.id" @mousedown.prevent="selectCustomer(c)" class="px-3 py-2.5 hover:bg-[#f8f9fa] cursor-pointer text-sm border-b border-[#f1f5f9] last:border-0">
                          <div class="font-bold text-[#364a63]">{{ c.name }}</div>
                          <div class="text-[#8094ae] text-xs mt-0.5" v-if="c.contactInfo"><i class="fas fa-phone-alt mr-1"></i> {{ c.contactInfo }}</div>
                       </div>
                    </div>
                  </div>
                </div>
                <div v-if="createForm.type === 'EXPORT'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Số điện thoại <span class="text-red-500">*</span></label>
                  <input v-model="createForm.customerPhone" @input="createForm.customerId = ''" type="text" placeholder="Nhập số điện thoại..."
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Trạng thái thanh toán</label>
                  <select v-model="createForm.paymentStatus"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="UNPAID">Chưa thanh toán</option>
                    <option value="PAID">Đã thanh toán</option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Ghi chú</label>
                  <input v-model="createForm.description" type="text" maxlength="500" placeholder="Ghi chú (tuỳ chọn)..."
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                </div>
              </div>

              <!-- Detail rows -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <div class="text-xs font-bold text-[#8094ae] uppercase">Danh sách hàng hóa</div>
                  <button @click="addDetailRow"
                    class="h-8 px-3 bg-[#eef2ff] hover:bg-[#4361ee] hover:text-white text-[#4361ee] rounded-lg text-xs font-bold transition-all flex items-center gap-1">
                    <i class="fas fa-plus"></i> Thêm dòng
                  </button>
                </div>
                <div class="space-y-3">
                  <div v-for="(d, idx) in createForm.details" :key="idx"
                    class="border border-[#e2e8f0] rounded-xl p-4 bg-white space-y-3">
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-bold text-[#8094ae]">Dòng {{ idx + 1 }}</span>
                      <button v-if="createForm.details.length > 1" @click="removeDetailRow(idx)"
                        class="w-6 h-6 flex items-center justify-center rounded bg-red-50 text-red-400 hover:bg-red-500 hover:text-white transition-all">
                        <i class="fas fa-times text-xs"></i>
                      </button>
                    </div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                      <div>
                        <label class="block text-xs text-[#8094ae] mb-1">Sản phẩm <span class="text-red-500">*</span></label>
                        <select v-model="d.productId" @change="onProductChange(d)"
                          class="w-full h-9 px-3 border border-[#e2e8f0] rounded-lg text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                          <option value="">-- Chọn sản phẩm --</option>
                          <option v-for="p in getAvailableProductsForRow(idx)" :key="p.id" :value="p.id">{{ p.name }} ({{ p.code }})</option>
                        </select>
                      </div>
                      <div :class="(createForm.type === 'IMPORT' || createForm.type === 'TRANSFER') ? 'grid grid-cols-1 gap-2' : 'grid grid-cols-3 gap-2'">
                        <div>
                          <label class="block text-xs text-[#8094ae] mb-1">Số lượng <span class="text-red-500">*</span></label>
                          <div class="relative">
                            <input v-model.number="d.quantity" type="number" min="1" @input="constrainQuantity(d)" @blur="onQuantityBlur(d)" @keypress="(e) => { if(!/[0-9]/.test(e.key)) e.preventDefault() }"
                              class="w-full h-9 px-3 border border-[#e2e8f0] rounded-lg text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none pr-12" />
                            <span v-if="(createForm.type === 'IMPORT' && createForm.sourceBranchId === createForm.destBranchId) || createForm.type === 'ADJUST_IN'" class="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-[#8094ae] font-bold">
                              / {{ getGlobalQuantity(d.productId) }}
                            </span>
                            <span v-else-if="getMaxQuantity(d.productId) !== null" class="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-[#8094ae] font-bold">
                              / {{ getMaxQuantity(d.productId) }}
                            </span>
                          </div>
                        </div>
                        <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'">
                          <label class="block text-xs text-[#8094ae] mb-1">Đơn giá</label>
                          <input v-model.number="d.price" type="number" min="0"
                            class="w-full h-9 px-3 border border-[#e2e8f0] rounded-lg text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                        </div>
                        <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'">
                          <label class="block text-xs text-[#8094ae] mb-1">Thành tiền</label>
                          <div class="w-full h-9 px-3 border border-transparent flex items-center text-sm font-bold text-[#4361ee] truncate">
                            {{ formatVND(d.quantity * (d.price || 0)) }}
                          </div>
                        </div>
                      </div>
                    </div>
                    <div v-if="selectedProductHasExpiry(d)" class="grid grid-cols-2 gap-3">
                      <div>
                        <label class="block text-xs text-[#8094ae] mb-1">Ngày sản xuất (NSX)</label>
                        <input v-model="d.manufacturingDate" type="date"
                          class="w-full h-9 px-3 border border-[#e2e8f0] rounded-lg text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                      </div>
                      <div>
                        <label class="block text-xs text-[#8094ae] mb-1">Hạn sử dụng (HSD)</label>
                        <input v-model="d.expirationDate" type="date"
                          class="w-full h-9 px-3 border border-[#e2e8f0] rounded-lg text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer actions -->
          <div class="px-6 py-4 border-t border-[#f1f5f9] flex justify-end gap-3 bg-white">
            <button @click="showCreateModal = false"
              class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63] text-sm hover:bg-[#f1f5f9] transition-all">
              Hủy
            </button>
            <button @click="submitCreateDraft" :disabled="submittingCreate"
              class="px-6 py-2.5 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl font-bold text-sm transition-all disabled:opacity-60 flex items-center gap-2">
              <i class="fas fa-spinner fa-spin" v-if="submittingCreate"></i>
              <i class="fas fa-save" v-else></i>
              Lưu nháp
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- CONFIRM TRANSFER MODAL -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <div v-if="showConfirmModal && confirmingReceipt"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[1000] flex items-center justify-center p-4">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-sky-500 to-teal-400 text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">Xác nhận nhận hàng</div>
              <div class="font-bold text-lg">Phiếu: {{ confirmingReceipt.code }}</div>
            </div>
            <button @click="showConfirmModal = false" class="w-9 h-9 flex items-center justify-center rounded-xl bg-white/20 hover:bg-white/30 transition-all">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div class="bg-sky-50 border border-sky-200 rounded-xl p-4 text-sm text-sky-700">
              <i class="fas fa-info-circle mr-2"></i>
              Nhập <strong>số lượng thực tế nhận được</strong> cho từng sản phẩm. Nếu ít hơn số xuất đi, hệ thống sẽ tự động tạo phiếu hao hụt (ADJUST_OUT).
            </div>

            <div class="space-y-3">
              <div v-for="item in confirmItems" :key="item.receiptDetailId"
                class="border border-[#e2e8f0] rounded-xl p-4">
                <div class="flex items-center justify-between mb-2">
                  <span class="font-semibold text-sm text-[#364a63]">{{ item.productName }}</span>
                  <span class="text-xs text-[#8094ae]">Số đã xuất: <strong>{{ item.sentQty }}</strong></span>
                </div>
                <div class="flex items-center gap-3">
                  <label class="text-xs text-[#8094ae] whitespace-nowrap">Số lượng nhận:</label>
                  <input v-model.number="item.actualQuantity" type="number" :min="0" :max="item.sentQty"
                    class="flex-1 h-9 px-3 border rounded-lg text-sm focus:ring-2 focus:ring-sky-400/20 focus:border-sky-400 outline-none"
                    :class="item.actualQuantity < item.sentQty ? 'border-amber-400 bg-amber-50' : 'border-[#e2e8f0]'" />
                  <span v-if="item.actualQuantity < item.sentQty"
                    class="text-xs font-bold text-amber-600 whitespace-nowrap">
                    ⚠️ Hao hụt: {{ item.sentQty - item.actualQuantity }}
                  </span>
                  <span v-else class="text-xs font-bold text-green-600 whitespace-nowrap">✅ Đủ</span>
                </div>
              </div>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-[#f1f5f9] flex justify-end gap-3 bg-[#f8f9fa]/50">
            <button @click="showConfirmModal = false"
              class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63] text-sm hover:bg-[#f1f5f9] transition-all">
              Hủy
            </button>
            <button @click="submitConfirmTransfer" :disabled="submittingConfirm"
              class="px-6 py-2.5 bg-sky-500 hover:bg-sky-600 text-white rounded-xl font-bold text-sm transition-all disabled:opacity-60 flex items-center gap-2">
              <i class="fas fa-spinner fa-spin" v-if="submittingConfirm"></i>
              <i class="fas fa-check-double" v-else></i>
              Xác nhận nhận hàng
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
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
</style>
