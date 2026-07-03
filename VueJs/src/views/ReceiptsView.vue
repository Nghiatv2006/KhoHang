<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'

const props = defineProps<{ receiptType?: string }>()
const route = useRoute()

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isManager = computed(() => user.value?.role === 'MANAGER')
// const isStaff = computed(() => user.value?.role === 'STAFF')
// const canApprove = computed(() => isAdmin.value || isManager.value)

function canApproveReceipt(r: any) {
  if (r.status === 'DRAFT') {
      if (r.type === 'EXPORT' && isAdmin.value && r.sourceBranchId !== 1) return false;
      if (r.type === 'TRANSFER' && isAdmin.value) return false;
      if (isAdmin.value) return true;
      if (isManager.value) {
          if (r.type === 'TRANSFER') {
              if (r.destBranchId === user.value?.branchId) return true;
          } else {
              if (r.sourceBranchId === user.value?.branchId || r.destBranchId === user.value?.branchId) return true;
          }
      }
      if (r.type === 'EXPORT' && user.value?.role === 'STAFF') {
          if (r.sourceBranchId === user.value?.branchId) return true;
      }
      return false;
  }
  if (r.status === 'PENDING_ADMIN') {
      if (r.type === 'IMPORT') {
          if (isAdmin.value) return true;
          return false;
      }
      if (r.type === 'TRANSFER') {
          if (isManager.value && r.sourceBranchId === user.value?.branchId) return true;
          return false;
      }
  }
  return false;
}

function approveReceiptText(r: any) {
    if (r.type === 'IMPORT' && r.status === 'DRAFT' && isManager.value && !isAdmin.value) {
        return "Duyệt (Gửi Admin)";
    }
    if (r.type === 'IMPORT' && r.status === 'PENDING_ADMIN' && isAdmin.value) {
        return "Chấp nhận nhập kho";
    }
    if (r.type === 'TRANSFER' && r.status === 'DRAFT') {
        return "Duyệt xin hàng (Gửi Nguồn)";
    }
    if (r.type === 'TRANSFER' && r.status === 'PENDING_ADMIN') {
        return "Duyệt xuất kho (Chuyển đi)";
    }
    if (r.type === 'EXPORT') {
        return "Hoàn tất xuất hóa đơn";
    }
    return "Phê duyệt";
}

function canCancelReceipt(r: any) {
  if (r.status === 'CANCELLED' || r.status === 'COMPLETED') return false;
  if (r.status !== 'DRAFT' && r.status !== 'PENDING_ADMIN') {
      return false;
  }
  if (r.type === 'EXPORT' && isAdmin.value && r.sourceBranchId !== 1) return false;
  if (isAdmin.value) return true;
  
  if (user.value?.role === 'STAFF') {
      if (r.type === 'EXPORT' && r.sourceBranchId === user.value?.branchId) return true;
      return false;
  }
  
  if (!isManager.value) return false;
  
  const isCrossBranchImport = r.type === 'IMPORT' && r.sourceBranchId !== r.destBranchId;
  if (isCrossBranchImport) {
      return r.sourceBranchId === user.value?.branchId || r.destBranchId === user.value?.branchId;
  }
  
  if (r.type === 'TRANSFER') {
      return r.sourceBranchId === user.value?.branchId || r.destBranchId === user.value?.branchId;
  }
  
  return true; 
}

function canApproveShortfallManager(r: any) {
    if (r.status !== 'PENDING_SHORTFALL_MANAGER') return false;
    if (isManager.value && r.destBranchId === user.value?.branchId) return true;
    return false;
}

function canApproveShortfallAdmin(r: any) {
    if (r.status !== 'PENDING_SHORTFALL_ADMIN') return false;
    if (r.type === 'TRANSFER') {
        if (isManager.value && r.sourceBranchId === user.value?.branchId) return true;
        return false;
    }
    if (isAdmin.value) return true;
    return false;
}

// ──────────────────────────────────────────────────────────────
// DATA
// ──────────────────────────────────────────────────────────────
const receipts = ref<any[]>([])
const products = ref<any[]>([])
const branches = ref<any[]>([])
const customers = ref<any[]>([])
// const inventories = ref<any[]>([])
const categories = ref<any[]>([])
const loading = ref(true)

// ──────────────────────────────────────────────────────────────
// FILTER
// ──────────────────────────────────────────────────────────────
const defaultFilterType = computed(() => {
  if (props.receiptType) return props.receiptType
  const p = route.path
  if (p.includes('imports')) return 'IMPORT'
  if (p.includes('invoices')) return 'EXPORT'
  if (p.includes('transfers')) return 'TRANSFER'
  if (p.includes('disposals')) return 'ADJUST_OUT'
  return ''
})

const isSpecificRoute = computed(() => defaultFilterType.value !== '')

const filterType = ref(defaultFilterType.value)

watch(defaultFilterType, (val) => {
  filterType.value = val
}, { immediate: true })
const filterStatus = ref('')
const searchKeyword = ref('')
const filterTimeRange = ref('custom')
const filterStartDate = ref('')
const filterEndDate = ref('')
const filterDeviation = ref('')

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

  // Hard filter to ensure visibility rules on frontend
  result = result.filter(r => {
    if (r.type === 'TRANSFER') {
      // Admin should not see transfer between sub-branches
      if (isAdmin.value && r.sourceBranchId !== 1 && r.destBranchId !== 1) return false;
      // Source Staff should NEVER see it
      if (user.value?.role === 'STAFF' && r.sourceBranchId === user.value?.branchId) return false;
    }
    return true;
  })

  if (filterType.value) result = result.filter(r => r.type === filterType.value)
  if (filterStatus.value) {
    if (filterStatus.value === 'UNPAID') {
      result = result.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán'))
    } else if (filterStatus.value === 'COMPENSATION') {
      result = result.filter(r => r.type === 'TRANSFER' && r.code?.startsWith('COMP-'))
    } else if (filterStatus.value === 'SHORTFALL') {
      result = result.filter(r => r.status === 'PENDING_SHORTFALL_MANAGER' || r.status === 'PENDING_SHORTFALL_ADMIN')
    } else {
      result = result.filter(r => r.status === filterStatus.value || r.paymentStatus === filterStatus.value)
    }
  }
  if (filterDeviation.value) {
    if (filterDeviation.value === 'yes') {
      result = result.filter(r => r.hasDeviation)
    } else if (filterDeviation.value === 'no') {
      result = result.filter(r => !r.hasDeviation)
    }
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    result = result.filter(r => {
      const code = (r.code || '').toLowerCase()
      return code.includes(kw)
    })
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
// PAGINATION
// ──────────────────────────────────────────────────────────────
const currentPage = ref(1)
const itemsPerPage = 50

watch([filterType, filterStatus, searchKeyword, filterTimeRange, filterStartDate, filterEndDate, filterDeviation], () => {
  currentPage.value = 1
})

const paginatedReceipts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredReceipts.value.slice(start, start + itemsPerPage)
})

const totalPages = computed(() => Math.ceil(filteredReceipts.value.length / itemsPerPage) || 1)

// ──────────────────────────────────────────────────────────────
// LOAD DATA
// ──────────────────────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const [rRes, pRes, bRes, cRes, catRes] = await Promise.all([
      api.get('/api/receipts'),
      api.get('/api/products'),
      api.get('/api/branches'),
      api.get('/api/customers'),
      api.get('/api/categories')
    ])
    if (rRes.ok) receipts.value = await rRes.json()
    if (pRes.ok) products.value = await pRes.json()
    if (bRes.ok) branches.value = await bRes.json()
    if (cRes.ok) customers.value = await cRes.json()
    if (catRes.ok) categories.value = await catRes.json()
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
const statDraft = computed(() => receipts.value.filter(r => r.status === 'DRAFT').length)
const statCompleted = computed(() => receipts.value.filter(r => r.status === 'COMPLETED').length)
const statCancelled = computed(() => receipts.value.filter(r => r.status === 'CANCELLED').length)

const statUnpaid = computed(() => receipts.value.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán')).length)
const statCompensation = computed(() => receipts.value.filter(r => r.type === 'TRANSFER' && r.code?.startsWith('COMP-')).length)
const statShortfall = computed(() => receipts.value.filter(r => r.type === 'IMPORT' && (r.status === 'PENDING_SHORTFALL_MANAGER' || r.status === 'PENDING_SHORTFALL_ADMIN')).length)

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
// DIRECT IMPORT MODAL (THÊM SẢN PHẨM)
// ──────────────────────────────────────────────────────────────
const showDirectImportModal = ref(false)
const submittingDirectImport = ref(false)
const directImportForm = ref({
  categoryId: '' as number | '',
  productId: '' as number | '',
  batchCode: '',
  isNewBatch: true,
  hasExpiry: false,
  expiryWarningDays: 30,
  manufacturingDate: '',
  expirationDate: '',
  quantity: 1,
  price: 0
})

const filteredProductsForDirectImport = computed(() => {
  if (!directImportForm.value.categoryId) return []
  return products.value.filter(p => p.categoryId === Number(directImportForm.value.categoryId))
})

const selectedProductInDirectImport = computed(() => {
  if (!directImportForm.value.productId) return null
  return products.value.find(p => p.id === Number(directImportForm.value.productId)) || null
})

watch(() => directImportForm.value.productId, (newVal) => {
  if (!newVal) {
    directImportForm.value.price = 0
  } else {
    directImportForm.value.price = selectedProductInDirectImport.value?.importPrice || 0
  }
})

async function openDirectImportModal() {
  directImportForm.value = {
    categoryId: '',
    productId: '',
    batchCode: '',
    isNewBatch: true,
    hasExpiry: false,
    expiryWarningDays: 30,
    manufacturingDate: '',
    expirationDate: '',
    quantity: 1,
    price: 0
  }
  showDirectImportModal.value = true
  if (globalInventories.value.length === 0) {
    try {
      const res = await api.get('/api/inventories/global')
      if (res.ok) {
        globalInventories.value = await res.json()
      }
    } catch(e) {}
  }
}

async function submitDirectImport() {
  const form = directImportForm.value
  if (!form.categoryId) { toast.error('Vui lòng chọn danh mục.'); return }
  if (!form.productId) { toast.error('Vui lòng chọn sản phẩm.'); return }
  if (!form.batchCode.trim()) { toast.error('Vui lòng nhập mã lô sản xuất.'); return }
  if (form.quantity <= 0) { toast.error('Số lượng nhập phải lớn hơn 0.'); return }
  if (!form.manufacturingDate) { toast.error('Vui lòng nhập Ngày sản xuất (NSX).'); return }

  const today = new Date(); today.setHours(0, 0, 0, 0);
  const mfgDate = new Date(form.manufacturingDate); mfgDate.setHours(0, 0, 0, 0);
  if (mfgDate > today) { toast.error('Ngày sản xuất (NSX) không được lớn hơn ngày hiện tại.'); return }

  if (form.hasExpiry) {
    if (!form.expirationDate) { toast.error('Vui lòng nhập Hạn sử dụng (HSD).'); return }
    if (new Date(form.expirationDate) < new Date(form.manufacturingDate)) { toast.error('Hạn sử dụng không được nhỏ hơn Ngày sản xuất.'); return }
    if (!form.expiryWarningDays || Number(form.expiryWarningDays) <= 0) { toast.error('Số ngày cảnh báo hạn dùng phải lớn hơn 0.'); return }
  }

  const payload: any = {
    type: 'IMPORT',
    sourceBranchId: null,
    destBranchId: headBranch.value?.id || 1,
    description: 'Thêm sản phẩm trực tiếp vào Kho Tổng',
    details: [{
      productId: Number(form.productId),
      batchCode: form.batchCode.trim(),
      quantity: form.quantity,
      price: form.price,
      manufacturingDate: form.manufacturingDate,
      expirationDate: form.hasExpiry ? form.expirationDate : '2099-12-31',
    }]
  }

  submittingDirectImport.value = true
  try {
    const res = await api.post('/api/receipts', payload)
    if (res.ok) {
      const data = await res.json()
      if (isAdmin.value && data.id) {
         await api.post(`/api/receipts/${data.id}/approve`, {})
         if (payload.type === 'IMPORT' || payload.type === 'TRANSFER') {
            await api.post(`/api/receipts/${data.id}/approve`, {})
         }
         toast.success('Đã tạo và tự động chuyển sang Chờ kiểm kê thành công!')
      } else {
         toast.success('Đã tạo Phiếu nhập hàng mới! Hãy duyệt phiếu để cộng tồn kho.')
      }
      showDirectImportModal.value = false
      await loadData()
    } else {
      const errData = await res.json()
      toast.error(errData.message || 'Lỗi khi tạo phiếu.')
    }
  } catch (err: any) {
    toast.error('Lỗi kết nối: ' + err.message)
  } finally {
    submittingDirectImport.value = false
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
  batchCode: string
  isNewBatch?: boolean
  manufacturingDate: string
  expirationDate: string
  quantity: number
  price: number
}

function openCreateModal() {
  let defaultType = (user.value?.branchId !== headBranch.value?.id && !isManager.value) ? 'IMPORT' : 'EXPORT';
  if (isSpecificRoute.value) {
    if (route.path === '/imports') defaultType = 'IMPORT';
    else if (route.path === '/invoices') defaultType = 'EXPORT';
    else if (route.path === '/transfers') defaultType = 'TRANSFER';
  }
  createForm.value = {
    type: defaultType,
    sourceBranchId: user.value?.branchId || headBranch.value?.id || '',
    destBranchId: '',
    customerId: '',
    customerName: '',
    customerPhone: '',
    paymentStatus: 'UNPAID',
    description: '',
    details: [{ productId: '', batchCode: '', isNewBatch: false, manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 }]
  }
  onTypeChange()
  showCreateModal.value = true
}

function addDetailRow() {
  createForm.value.details.push({ productId: '', batchCode: '', isNewBatch: false, manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 })
}

function removeDetailRow(index: number) {
  if (createForm.value.details.length <= 1) return
  createForm.value.details.splice(index, 1)
}

function onTypeChange() {
  const t = createForm.value.type
  if (t === 'IMPORT') {
    createForm.value.sourceBranchId = ''
    createForm.value.destBranchId = user.value?.branchId || headBranch.value?.id || ''
  } else if (t === 'EXPORT') {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  } else if (t === 'TRANSFER') {
    createForm.value.sourceBranchId = ''
    createForm.value.destBranchId = user.value?.branchId || headBranch.value?.id || ''
  } else {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  }
  // Reset products when changing type to avoid stale/out-of-stock products
  createForm.value.details = [{ productId: '', batchCode: '', manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 }]
}

const sourceInventories = ref<any[]>([])
const globalInventories = ref<any[]>([])

function isValidDate(d: any): boolean {
  return !!d && typeof d === 'string' && !d.startsWith('1970') && d !== ''
}

const directImportBatches = computed(() => {
  if (!directImportForm.value.productId) return []
  const uniqueBatches = new Map()
  globalInventories.value.filter(inv => inv.productId === Number(directImportForm.value.productId)).forEach(inv => {
    if (!uniqueBatches.has(inv.batchCode)) {
      uniqueBatches.set(inv.batchCode, { ...inv })
    } else {
      const existing = uniqueBatches.get(inv.batchCode)
      existing.quantity += inv.quantity
      // Prefer valid dates over epoch dates
      if (!isValidDate(existing.manufacturingDate) && isValidDate(inv.manufacturingDate)) {
        existing.manufacturingDate = inv.manufacturingDate
      }
      if (!isValidDate(existing.expirationDate) && isValidDate(inv.expirationDate)) {
        existing.expirationDate = inv.expirationDate
      }
      if (inv.hasExpiry) existing.hasExpiry = true
    }
  })
  return Array.from(uniqueBatches.values())
})

const existingBatchInDirectImport = computed(() => {
  return directImportBatches.value.find(b => b.batchCode === directImportForm.value.batchCode)
})

function parseToInputDate(dateStr: string | undefined): string {
  if (!dateStr || dateStr.startsWith('1970')) return ''
  // If DD/MM/YYYY
  const parts = dateStr.split('/')
  if (parts.length === 3 && parts[2].length === 4) {
    return `${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}`
  }
  // Try ISO parsing
  try {
    const d = new Date(dateStr)
    if (!isNaN(d.getTime())) return d.toISOString().substring(0, 10)
  } catch (e) {}
  return dateStr.substring(0, 10)
}

const isMfgDateLocked = computed(() => {
  const b = existingBatchInDirectImport.value
  return !!b && !!parseToInputDate(b.manufacturingDate)
})

const isExpDateLocked = computed(() => {
  const b = existingBatchInDirectImport.value
  return !!b && !!parseToInputDate(b.expirationDate)
})

watch(() => directImportForm.value.productId, (newVal) => {
  if (newVal) {
    if (directImportBatches.value.length > 0) {
      directImportForm.value.isNewBatch = false
    } else {
      directImportForm.value.isNewBatch = true
    }
  }
})

watch(() => directImportForm.value.batchCode, (newBatch) => {
  if (!newBatch) return;
  const existing = existingBatchInDirectImport.value
  if (existing) {
    const mDate = parseToInputDate(existing.manufacturingDate)
    if (mDate) {
      directImportForm.value.manufacturingDate = mDate
    }
    const eDate = parseToInputDate(existing.expirationDate)
    if (eDate) {
      directImportForm.value.expirationDate = eDate
      directImportForm.value.hasExpiry = true
    } else {
      directImportForm.value.hasExpiry = false
    }
    directImportForm.value.price = existing.importPrice || directImportForm.value.price
  }
})

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
  row.batchCode = ''
  row.isNewBatch = false
  if (p) {
    row.quantity = 1 // Reset quantity to 1 when changing to a valid product
    row.price = Number(p.price) || 0
    if (!p.hasExpiry) {
      row.manufacturingDate = '1970-01-01'
      row.expirationDate = '1970-01-01'
    } else {
      row.manufacturingDate = ''
      row.expirationDate = ''
    }
  } else {
    row.quantity = 0 // Reset to 0 if no product is selected
    row.price = 0
  }
  constrainQuantity(row)
}

function getBatchesForProduct(productId: number | string | null) {
  if (!productId) return []
  if (createForm.value.type === 'ADJUST_IN' || (createForm.value.type === 'IMPORT' && !createForm.value.sourceBranchId)) {
    const uniqueBatches = new Map()
    globalInventories.value.filter(inv => inv.productId === Number(productId)).forEach(inv => {
      if (!uniqueBatches.has(inv.batchCode)) {
        uniqueBatches.set(inv.batchCode, { ...inv })
      } else {
        const existing = uniqueBatches.get(inv.batchCode)
        existing.quantity += inv.quantity
      }
    })
    return Array.from(uniqueBatches.values())
  }
  return sourceInventories.value
    .filter(inv => inv.productId === Number(productId) && inv.quantity > 0)
    .map(inv => {
      const pendingQty = receipts.value
        .filter(r => r.status === 'DRAFT' && r.sourceBranchId === createForm.value.sourceBranchId && 
                (['EXPORT', 'TRANSFER', 'ADJUST_OUT'].includes(r.type) || 
                 (r.type === 'IMPORT' && r.sourceBranchId !== r.destBranchId)))
        .flatMap(r => r.details || [])
        .filter(d => Number(d.productId) === Number(productId) && d.batchCode === inv.batchCode)
        .reduce((sum, d) => sum + Number(d.quantity), 0);
      return { ...inv, quantity: Math.max(0, inv.quantity - pendingQty) };
    });
}

function onBatchChange(row: DetailRow) {
  if (!row.batchCode) return
  const inv = globalInventories.value.find(x => x.productId === Number(row.productId) && x.batchCode === row.batchCode)
  if (inv) {
    if (inv.manufacturingDate && inv.manufacturingDate !== '1970-01-01') row.manufacturingDate = inv.manufacturingDate
    if (inv.expirationDate && inv.expirationDate !== '1970-01-01') row.expirationDate = inv.expirationDate
  }
  constrainQuantity(row)
}

const selectedProductHasExpiry = (row: DetailRow) => {
  if (!row.productId) return false
  const p = products.value.find(x => x.id === Number(row.productId))
  return p?.hasExpiry ?? false
}

function getMaxQuantity(row: DetailRow) {
  if (!row.productId) return null
  if (createForm.value.type === 'ADJUST_IN') return null
  if (!createForm.value.sourceBranchId) return null
  if (createForm.value.type === 'IMPORT' && createForm.value.sourceBranchId === createForm.value.destBranchId) return null
  
  if (!row.isNewBatch && !row.batchCode) return null;

  let totalQty = 0;
  if (row.batchCode && !row.isNewBatch) {
    const inv = sourceInventories.value.find(x => x.productId === Number(row.productId) && x.batchCode === row.batchCode)
    totalQty = inv ? inv.quantity : 0;
  } else {
    totalQty = sourceInventories.value
      .filter(x => x.productId === Number(row.productId))
      .reduce((sum, inv) => sum + inv.quantity, 0)
  }

  // Trừ đi số lượng đang nằm trong các phiếu nháp chờ xuất/điều chuyển
  const pendingQty = receipts.value
    .filter(r => r.status === 'DRAFT' && r.sourceBranchId === createForm.value.sourceBranchId && 
            (['EXPORT', 'TRANSFER', 'ADJUST_OUT'].includes(r.type) || 
             (r.type === 'IMPORT' && r.sourceBranchId !== r.destBranchId)))
    .flatMap(r => r.details || [])
    .filter(d => Number(d.productId) === Number(row.productId) && (!row.batchCode || d.batchCode === row.batchCode))
    .reduce((sum, d) => sum + Number(d.quantity), 0)

  return Math.max(0, totalQty - pendingQty)
}

function getGlobalQuantity(row: DetailRow) {
  if (!row.productId) return null;
  if (!row.isNewBatch && !row.batchCode) return null;
  if (row.batchCode && !row.isNewBatch) {
    const inv = globalInventories.value.find(x => x.productId === Number(row.productId) && x.batchCode === row.batchCode)
    return inv ? inv.quantity : 0
  }
  return globalInventories.value
    .filter(x => x.productId === Number(row.productId))
    .reduce((sum, inv) => sum + inv.quantity, 0)
}

function constrainQuantity(d: DetailRow) {
  if (d.quantity === null || d.quantity === undefined || (d.quantity as any) === '') return;
  if (createForm.value.type === 'ADJUST_IN' || (createForm.value.type === 'IMPORT' && createForm.value.sourceBranchId === createForm.value.destBranchId)) return;
  const max = getMaxQuantity(d)
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
  if (createForm.value.type === 'ADJUST_IN' || (createForm.value.type === 'IMPORT' && createForm.value.sourceBranchId === createForm.value.destBranchId)) return;
  
  const max = getMaxQuantity(d)
  if (max !== null && max === 0) {
    d.quantity = 0;
  }
}

async function submitCreateDraft() {
  if (submittingCreate.value) return
  if (!createForm.value.sourceBranchId && createForm.value.type !== 'IMPORT') { toast.error('Vui lòng chọn loại phiếu.'); return }
  const f = createForm.value
  if (f.details.some(d => !d.productId || d.quantity <= 0 || !d.batchCode?.trim())) {
    toast.error('Vui lòng điền đầy đủ sản phẩm, lô sản xuất và số lượng hợp lệ.')
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
    const max = getMaxQuantity(d)
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
      batchCode: d.batchCode,
      quantity: d.quantity,
      price: d.price,
      manufacturingDate: d.manufacturingDate || '1970-01-01',
      expirationDate: d.expirationDate || '2099-12-31',
    }))
  }

  submittingCreate.value = true
  try {
    const res = await api.post('/api/receipts', payload)
    if (res.ok) {
      const data = await res.json()
      if (data.id && isAdmin.value && payload.type !== 'EXPORT') {
         await api.post(`/api/receipts/${data.id}/approve`, {})
         if (payload.type === 'IMPORT' || payload.type === 'TRANSFER') {
            await api.post(`/api/receipts/${data.id}/approve`, {})
         }
         toast.success('Đã tạo và tự động chuyển trạng thái phiếu thành công!')
      } else {
         toast.success('Tạo phiếu thành công!')
      }
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
  if (approvingId.value === receipt.id) return
  if (!confirm(`Xác nhận phê duyệt phiếu ${receipt.code}?`)) return
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
// SHORTFALL APPROVAL
// ──────────────────────────────────────────────────────────────
async function approveShortfall(receipt: any, isApproved: boolean) {
  if (approvingId.value === receipt.id) return
  if (!confirm(isApproved ? `Xác nhận DUYỆT báo thiếu cho phiếu ${receipt.code}?` : `Xác nhận TỪ CHỐI báo thiếu cho phiếu ${receipt.code}?`)) return
  approvingId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/approve-shortfall`, { isApproved })
    if (res.ok) {
      toast.success(isApproved ? `Phiếu ${receipt.code} đã được duyệt thiếu hụt.` : `Đã từ chối thiếu hụt phiếu ${receipt.code}.`)
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lỗi khi thao tác phiếu.')
    }
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
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
  if (markingPaidId.value === receipt.id) return
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
const confirmItems = ref<{ receiptDetailId: number; productName: string; sentQty: number; actualQuantity: number; shortfallReason?: string }[]>([])
const submittingConfirm = ref(false)

function openConfirmTransferModal(receipt: any) {
  confirmingReceipt.value = receipt
  confirmItems.value = (receipt.details || []).map((d: any) => ({
    receiptDetailId: d.id,
    productName: d.productName,
    sentQty: d.quantity,
    actualQuantity: d.quantity,
    shortfallReason: ''
  }))
  showConfirmModal.value = true
}

async function submitConfirmTransfer() {
  if (submittingConfirm.value) return
  if (confirmItems.value.some(i => i.actualQuantity < 0)) {
    toast.error('Số lượng nhận không được âm.'); return
  }
  if (confirmItems.value.some(i => i.actualQuantity > i.sentQty)) {
    toast.error('Số lượng nhận không được vượt quá số lượng trên phiếu.'); return
  }
  if (confirmItems.value.some(i => i.actualQuantity < i.sentQty && (!i.shortfallReason || i.shortfallReason.trim() === ''))) {
    toast.error('Vui lòng nhập đầy đủ lý do cho các sản phẩm bị hao hụt.'); return
  }
  submittingConfirm.value = true
  try {
    const payload = {
      items: confirmItems.value.map(i => ({
        receiptDetailId: i.receiptDetailId,
        actualQuantity: i.actualQuantity,
        shortfallReason: i.actualQuantity < i.sentQty ? i.shortfallReason : null
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
// CONFIRM STOCKTAKE MODAL
// ──────────────────────────────────────────────────────────────
const showStocktakeModal = ref(false)
const stocktakeReceipt = ref<any>(null)
const stocktakeItems = ref<{ receiptDetailId: number; productName: string; sentQty: number; actualQuantity: number; shortfallReason?: string }[]>([])
const submittingStocktake = ref(false)

function canConfirmStocktake(receipt: any) {
  if (receipt.type !== 'IMPORT' && receipt.type !== 'TRANSFER') return false
  if (receipt.status !== 'PENDING_STOCKTAKE') return false
  if (user.value?.role !== 'STAFF') return false;
  return receipt.destBranchId === user.value?.branchId
}

function openStocktakeModal(receipt: any) {
  stocktakeReceipt.value = receipt
  stocktakeItems.value = (receipt.details || []).map((d: any) => ({
    receiptDetailId: d.id,
    productName: d.productName,
    sentQty: d.quantity,
    actualQuantity: d.quantity,
    shortfallReason: ''
  }))
  showStocktakeModal.value = true
}

async function submitConfirmStocktake() {
  if (submittingStocktake.value) return
  if (stocktakeItems.value.some(i => i.actualQuantity < 0)) {
    toast.error('Số lượng thực tế không được âm.'); return
  }
  if (stocktakeItems.value.some(i => i.actualQuantity > i.sentQty)) {
    toast.error('Số lượng thực đếm không được vượt quá số lượng trên phiếu.'); return
  }
  if (stocktakeItems.value.some(i => i.actualQuantity < i.sentQty && (!i.shortfallReason || i.shortfallReason.trim() === ''))) {
    toast.error('Vui lòng nhập đầy đủ lý do cho các sản phẩm bị hao hụt.'); return
  }
  submittingStocktake.value = true
  try {
    const payload = {
      items: stocktakeItems.value.map(i => ({
        receiptDetailId: i.receiptDetailId,
        actualQuantity: i.actualQuantity,
        shortfallReason: i.actualQuantity < i.sentQty ? i.shortfallReason : null
      }))
    }
    const res = await api.post(`/api/receipts/${stocktakeReceipt.value.id}/confirm-stocktake`, payload)
    if (res.ok) {
      toast.success('Xác nhận kiểm kê thành công! Hàng đã được cộng vào kho.')
      showStocktakeModal.value = false
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lỗi khi xác nhận kiểm kê.')
    }
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    submittingStocktake.value = false
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

function typeLabel(r: any) {
  if (r?.type === 'TRANSFER' && r?.code?.startsWith('COMP-')) return 'Điều chuyển bù';
  const t = r?.type;
  const map: Record<string, string> = {
    IMPORT: 'Nhập kho', EXPORT: 'Xuất bán', TRANSFER: 'Điều chuyển',
    ADJUST_IN: 'Tăng tồn kho', ADJUST_OUT: 'Giảm tồn kho'
  }
  return map[t] || t
}
function typeClass(r: any) {
  if (r?.type === 'TRANSFER' && r?.code?.startsWith('COMP-')) return 'bg-pink-100 text-pink-700'
  const map: Record<string, string> = {
    IMPORT: 'bg-blue-100 text-blue-700',
    EXPORT: 'bg-purple-100 text-purple-700',
    TRANSFER: 'bg-orange-100 text-orange-700',
    ADJUST_IN: 'bg-emerald-100 text-emerald-700',
    ADJUST_OUT: 'bg-rose-100 text-rose-700'
  }
  return map[r?.type] || 'bg-gray-100 text-gray-600'
}
function statusClass(r: any) {
  const s = r?.status;
  if (r?.type === 'EXPORT' && s === 'COMPLETED' && r?.paymentStatus !== 'PAID') {
    return 'bg-orange-100 text-orange-700 border border-orange-300';
  }
  if (r?.type === 'TRANSFER' && s === 'PENDING_ADMIN') {
    if (r.sourceBranchId === user.value?.branchId) {
      return 'bg-green-600 text-white shadow-sm';
    }
    return 'bg-orange-500 text-white shadow-sm';
  }
  if ((s === 'PENDING_ADMIN' || s === 'PENDING_STOCKTAKE') && r?.type === 'TRANSFER') {
    return 'bg-orange-500 text-white shadow-sm';
  }
  // Hóa đơn (EXPORT) chưa thanh toán → màu đỏ thay vì xanh
  if (s === 'COMPLETED' && r?.type === 'EXPORT' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán' || r.paymentStatus === 'UNPAID')) {
    return 'bg-red-500 text-white shadow-sm';
  }
  const map: Record<string, string> = {
    DRAFT: 'bg-yellow-500 text-white shadow-sm',
    PENDING_ADMIN: 'bg-blue-500 text-white shadow-sm',
    PENDING_STOCKTAKE: 'bg-purple-500 text-white shadow-sm',
    PENDING_SHORTFALL_MANAGER: 'bg-orange-500 text-white shadow-sm',
    PENDING_SHORTFALL_ADMIN: 'bg-rose-500 text-white shadow-sm',
    PENDING_COMPENSATION: 'bg-indigo-500 text-white shadow-sm',
    COMPLETED: 'bg-green-600 text-white shadow-sm',
    CANCELLED: 'bg-red-500 text-white shadow-sm',
    RETURN: 'bg-amber-600 text-white shadow-sm'
  }
  return map[s] || 'bg-slate-500 text-white shadow-sm'
}

function statusLabel(r: any) {
  if (r?.type === 'EXPORT' && r?.status === 'COMPLETED' && r?.paymentStatus !== 'PAID') {
    return '💸 Chưa thanh toán';
  }
  const s = r?.status;
  if (s === 'DRAFT') return '⏳ Chờ duyệt';
  if (s === 'PENDING_ADMIN') {
    if (r?.type === 'TRANSFER') {
      return '⏳ Chờ Manager Nguồn';
    }
    return '🛡️ Chờ Admin';
  }
  if (s === 'PENDING_STOCKTAKE') {
    if (r?.type === 'TRANSFER' && r.sourceBranchId === user.value?.branchId) {
      return '📦 Đang chuyển (Chờ đích KK)';
    }
    return '📦 Chờ kiểm kê';
  }
  if (s === 'PENDING_SHORTFALL_MANAGER') return '⚠️ Thiếu hụt (Chờ Manager)';
  if (s === 'PENDING_SHORTFALL_ADMIN') {
    if (r?.type === 'TRANSFER') return '🚨 Thiếu hụt (Chờ Manager Nguồn)';
    return '🚨 Báo thiếu hụt';
  }
  if (s === 'COMPLETED') return '✅ Đã duyệt';
  if (s === 'CANCELLED') return '❌ Đã hủy';
  if (s === 'RETURN') return '🔄 Trả hàng';
  return s;
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

// Can the current user confirm transfer for this receipt? (Deprecated, use confirmStocktake instead)
function canConfirmTransfer(r: any) {
  if (r.type !== 'TRANSFER' || r.status !== 'COMPLETED') return false;
  if (r.paymentStatus !== 'IN_TRANSIT' && r.paymentStatus !== 'Đang vận chuyển') return false;
  if (isAdmin.value) return false;
  if (r.destBranchId === user.value?.branchId) return true;
  return false;
}

function getCustomerName(receipt: any) {
  if (receipt.customerName) {
    if (receipt.customerPhone) {
      return `${receipt.customerName} - ${receipt.customerPhone}`;
    }
    return receipt.customerName;
  }
  if (!receipt.customerId) return '—'
  const c = customers.value.find(x => x.id === receipt.customerId)
  return c ? `${c.name} - ${c.contactInfo || 'Không có SĐT'}` : '—'
}

const pageTitle = computed(() => {
  if (route.path.includes('imports')) return 'Quản lý Nhập Kho'
  if (route.path.includes('invoices')) return 'Quản lý Hóa Đơn (Xuất Bán)'
  if (route.path.includes('transfers')) return 'Quản lý Điều Chuyển'
  if (route.path.includes('disposals')) return 'Quản lý Tiêu Hủy'
  return 'Quản lý Phiếu Kho'
})

const pageIcon = computed(() => {
  if (route.path.includes('imports')) return 'fas fa-download text-[#4361ee]'
  if (route.path.includes('invoices')) return 'fas fa-file-invoice-dollar text-[#4361ee]'
  if (route.path.includes('transfers')) return 'fas fa-exchange-alt text-[#4361ee]'
  if (route.path.includes('disposals')) return 'fas fa-trash-alt text-[#4361ee]'
  return 'fas fa-file-invoice text-[#4361ee]'
})

</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto font-['Inter',sans-serif]">

    <!-- HEADER -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0 flex items-center gap-3">
          <i :class="pageIcon"></i>
          {{ pageTitle }}
        </h2>
        <p class="text-[#8094ae] text-sm mt-1">Theo dõi, lập và phê duyệt các phiếu nhập/xuất/điều chuyển kho</p>
      </div>
      <div class="flex items-center gap-3">
        <button
          v-if="isAdmin"
          @click="openDirectImportModal"
          class="h-[42px] bg-[#05b171] hover:bg-[#04965e] text-white px-5 rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center gap-2"
        >
          <i class="fas fa-box-open"></i> Thêm sản phẩm
        </button>
        <button
          v-if="user?.role === 'STAFF'"
          @click="openCreateModal"
          class="h-[42px] bg-[#4361ee] hover:bg-[#3a0ca3] text-white px-5 rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center gap-2"
        >
          <i class="fas fa-plus"></i> Lập phiếu nháp
        </button>
      </div>
    </div>

    <!-- STAT CARDS -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div @click="filterStatus = filterStatus === 'DRAFT' ? '' : 'DRAFT'"
        :class="['bg-white rounded-2xl p-6 border transition-all duration-300 cursor-pointer flex items-center gap-5 hover:-translate-y-1 hover:shadow-lg', filterStatus === 'DRAFT' ? 'border-yellow-400 ring-2 ring-yellow-200 shadow-md' : 'border-[#f1f5f9] hover:border-yellow-300']">
        <div class="w-14 h-14 rounded-2xl bg-yellow-500/10 border border-yellow-500/30 flex items-center justify-center text-yellow-500 text-2xl shadow-sm">
          <i class="fas fa-pencil-alt"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide mb-1">Chờ duyệt</div>
          <div class="text-3xl font-black text-yellow-500">{{ statDraft }}</div>
        </div>
      </div>
      <div @click="filterStatus = filterStatus === 'COMPLETED' ? '' : 'COMPLETED'"
        :class="['bg-white rounded-2xl p-6 border transition-all duration-300 cursor-pointer flex items-center gap-5 hover:-translate-y-1 hover:shadow-lg', filterStatus === 'COMPLETED' ? 'border-green-400 ring-2 ring-green-200 shadow-md' : 'border-[#f1f5f9] hover:border-green-300']">
        <div class="w-14 h-14 rounded-2xl bg-green-500/10 border border-green-500/30 flex items-center justify-center text-green-500 text-2xl shadow-sm">
          <i class="fas fa-check-circle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide mb-1">Đã duyệt</div>
          <div class="text-3xl font-black text-green-500">{{ statCompleted }}</div>
        </div>
      </div>

      <div @click="filterStatus = filterStatus === 'CANCELLED' ? '' : 'CANCELLED'"
        :class="['bg-white rounded-2xl p-6 border transition-all duration-300 cursor-pointer flex items-center gap-5 hover:-translate-y-1 hover:shadow-lg', filterStatus === 'CANCELLED' ? 'border-red-400 ring-2 ring-red-200 shadow-md' : 'border-[#f1f5f9] hover:border-red-300']">
        <div class="w-14 h-14 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-500 text-2xl shadow-sm">
          <i class="fas fa-times-circle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide mb-1">Đã hủy</div>
          <div class="text-3xl font-black text-red-500">{{ statCancelled }}</div>
        </div>
      </div>
      <div v-if="filterType === 'EXPORT' || !filterType" @click="filterStatus = filterStatus === 'UNPAID' ? '' : 'UNPAID'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'UNPAID' ? 'border-orange-400 ring-2 ring-orange-200' : 'border-[#f1f5f9] hover:border-orange-300']">
        <div class="w-12 h-12 rounded-xl bg-orange-50 flex items-center justify-center text-orange-400 text-xl">
          <i class="fas fa-file-invoice-dollar"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide mb-1">Chưa thanh toán</div>
          <div class="text-3xl font-black text-orange-500">{{ statUnpaid }}</div>
        </div>
      </div>
      <div v-else-if="filterType === 'TRANSFER'" @click="filterStatus = filterStatus === 'COMPENSATION' ? '' : 'COMPENSATION'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'COMPENSATION' ? 'border-pink-400 ring-2 ring-pink-200' : 'border-[#f1f5f9] hover:border-pink-300']">
        <div class="w-12 h-12 rounded-xl bg-pink-50 flex items-center justify-center text-pink-400 text-xl">
          <i class="fas fa-exchange-alt"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Điều chuyển bù</div>
          <div class="text-2xl font-extrabold text-pink-400">{{ statCompensation }}</div>
        </div>
      </div>
      <div v-else-if="filterType === 'IMPORT'" @click="filterStatus = filterStatus === 'SHORTFALL' ? '' : 'SHORTFALL'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'SHORTFALL' ? 'border-red-400 ring-2 ring-red-200' : 'border-[#f1f5f9] hover:border-red-300']">
        <div class="w-12 h-12 rounded-xl bg-red-50 flex items-center justify-center text-red-400 text-xl">
          <i class="fas fa-exclamation-triangle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Thiếu hụt</div>
          <div class="text-2xl font-extrabold text-red-400">{{ statShortfall }}</div>
        </div>
      </div>
    </div>

    <!-- TABLE CARD -->
    <div class="bg-white rounded-2xl border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-sm overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9]">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <!-- Tìm kiếm đa năng -->
          <div class="lg:col-span-2 relative">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae] text-sm"></i>
            <input v-model="searchKeyword" type="text" placeholder="Tìm kiếm theo mã phiếu..."
              class="w-full h-11 pl-10 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all" />
          </div>
          <!-- Lọc loại phiếu -->
          <div v-if="!isSpecificRoute">
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
              <option value="RECEIVED">Đã nhận hàng</option>
            </select>
          </div>
          <!-- Lọc hao hụt / chênh lệch -->
          <div>
            <select v-model="filterDeviation"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="">-- Tất cả chênh lệch --</option>
              <option value="yes">Có chênh lệch / Hao hụt</option>
              <option value="no">Khớp số lượng</option>
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
              <button v-if="(!isSpecificRoute && filterType) || filterStatus || searchKeyword || filterStartDate || filterEndDate || filterDeviation || filterTimeRange !== 'custom'"
                @click="filterType = isSpecificRoute ? defaultFilterType : ''; filterStatus = ''; searchKeyword = ''; filterTimeRange = 'custom'; filterStartDate = ''; filterEndDate = ''; filterDeviation = ''"
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
              <th v-if="!isSpecificRoute" class="px-5 py-3 text-left font-bold">Loại</th>
              <th class="px-5 py-3 text-left font-bold">Trạng thái</th>
              <th class="px-5 py-3 text-left font-bold">Chênh lệch</th>
              <th class="px-5 py-3 text-left font-bold">Chi nhánh nguồn</th>
              <th class="px-5 py-3 text-left font-bold">Đích / Khách hàng</th>
              <th class="px-5 py-3 text-left font-bold">Người lập</th>
              <th class="px-5 py-3 text-left font-bold">Người duyệt</th>
              <th class="px-5 py-3 text-left font-bold">Ngày tạo</th>
              <th class="px-5 py-3 text-center font-bold">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[#f1f5f9]">
            <tr v-for="r in paginatedReceipts" :key="r.id"
              @dblclick="openDetail(r)"
              :class="[
                'hover:bg-[#f8f9fa]/80 cursor-pointer transition-colors group even:bg-slate-50/60',
                r.hasDeviation && (r.status === 'PENDING_SHORTFALL_MANAGER' || r.status === 'PENDING_SHORTFALL_ADMIN') ? 'bg-rose-50/40 hover:bg-rose-100/40' : ''
              ]">
              <td class="px-6 py-5">
                <span class="font-mono font-bold text-[#4361ee] text-xs">{{ r.code }}</span>
              </td>
              <td v-if="!isSpecificRoute" class="px-5 py-4">
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', typeClass(r)]">
                  {{ typeLabel(r) }}
                </span>
              </td>
              <td class="px-6 py-5">
                <div class="flex flex-col gap-1">
                  <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-bold', statusClass(r)]">
                    {{ statusLabel(r) }}
                  </span>
                  <span v-if="r.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold mt-1', paymentStatusClass(r.paymentStatus)]">
                    📦 Đã nhận hàng
                  </span>
                </div>
              </td>
              <td class="px-6 py-5">
                <div v-if="r.hasDeviation" class="flex flex-col max-w-[200px]" :title="r.deviationSummary">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-600 border border-rose-100 w-fit">
                    ⚠️ Lệch số lượng
                  </span>
                  <span class="text-xs text-rose-500 mt-1 font-medium truncate" :title="r.deviationSummary">
                    {{ r.deviationSummary }}
                  </span>
                </div>
                <div v-else-if="r.status === 'COMPLETED' || r.status === 'PENDING_COMPENSATION' || r.paymentStatus === 'RECEIVED'" class="text-xs text-emerald-600 font-medium">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-100 w-fit">
                    ✓ Khớp
                  </span>
                </div>
                <div v-else class="text-xs text-slate-400">—</div>
              </td>
              <td class="px-6 py-5">
                <span class="text-[#364a63] font-medium">{{ r.sourceBranchName || '—' }}</span>
              </td>
              <td class="px-6 py-5">
                <span class="text-[#364a63] font-medium" v-if="r.type === 'EXPORT'">{{ getCustomerName(r) }}</span>
                <span class="text-[#364a63] font-medium" v-else>{{ r.destBranchName || '—' }}</span>
              </td>
              <td class="px-6 py-5">
                <div class="text-[#8094ae]">{{ r.createdByName }}</div>
                <div v-if="r.stocktakeByName" class="text-xs text-purple-600 mt-1 font-semibold" title="Người kiểm kê"><i class="fas fa-clipboard-check"></i> {{ r.stocktakeByName }}</div>
                <div v-else-if="r.status === 'COMPLETED' && (r.type === 'IMPORT' || r.type === 'TRANSFER') && r.createdByRole === 'STAFF'" class="text-xs text-purple-600 mt-1 font-semibold opacity-60" title="Người kiểm kê (Dữ liệu cũ)"><i class="fas fa-clipboard-check"></i> {{ r.createdByName }}</div>
              </td>
              <td class="px-6 py-5">
                <span class="text-[#364a63] font-medium">{{ r.approvedByName || '—' }}</span>
              </td>
              <td class="px-6 py-5">
                <span class="text-[#8094ae] text-xs">{{ formatDateTime(r.createdAt) }}</span>
              </td>
              <td class="px-6 py-5">
                <div class="flex items-center justify-center gap-2">
                  <button @click.stop="openDetail(r)"
                    class="w-9 h-9 flex items-center justify-center rounded-lg bg-slate-600 hover:bg-[#4361ee] text-white transition-all shadow-sm"
                    title="Xem chi tiết">
                    <i class="fas fa-eye text-sm"></i>
                  </button>
                  <button v-if="canApproveReceipt(r)"
                    @click.stop="approveReceipt(r)"
                    :disabled="approvingId === r.id"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-green-50 hover:bg-green-500 hover:text-white text-green-600 transition-all disabled:opacity-50"
                    title="Phê duyệt">
                    <i class="fas fa-check text-xs"></i>
                  </button>
                  <button v-if="canConfirmStocktake(r)"
                    @click.stop="openStocktakeModal(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-purple-50 hover:bg-purple-500 hover:text-white text-purple-600 transition-all"
                    title="Thực hiện kiểm kê">
                    <i class="fas fa-boxes text-xs"></i>
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

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="px-6 py-4 border-t border-[#e2e8f0] flex flex-col sm:flex-row items-center justify-between bg-white rounded-b-2xl gap-4">
        <div class="text-sm text-[#8094ae]">
          Hiển thị <span class="font-bold text-[#364a63]">{{ (currentPage - 1) * itemsPerPage + 1 }}</span> - <span class="font-bold text-[#364a63]">{{ Math.min(currentPage * itemsPerPage, filteredReceipts.length) }}</span> trong số <span class="font-bold text-[#364a63]">{{ filteredReceipts.length }}</span> phiếu
        </div>
        <div class="flex items-center gap-2">
          <button @click="currentPage--" :disabled="currentPage === 1"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            <i class="fas fa-chevron-left mr-1.5 text-[10px]"></i> Trước
          </button>
          <div class="px-4 py-1.5 flex items-center justify-center rounded-lg bg-[#f8f9fa] text-[#364a63] font-bold text-sm border border-[#e2e8f0]">
            {{ currentPage }} / {{ totalPages }}
          </div>
          <button @click="currentPage++" :disabled="currentPage === totalPages"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            Sau <i class="fas fa-chevron-right ml-1.5 text-[10px]"></i>
          </button>
        </div>
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
              <div class="text-xs font-bold opacity-70 uppercase">{{ selectedReceipt?.type === 'EXPORT' ? 'Chi tiết hóa đơn' : 'Chi tiết phiếu kho' }}</div>
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
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', typeClass(selectedReceipt)]">
                  {{ typeLabel(selectedReceipt) }}
                </span>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Trạng thái</div>
                <div class="flex flex-col items-start gap-1">
                  <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', statusClass(selectedReceipt)]">
                    {{ statusLabel(selectedReceipt) }}
                  </span>
                  <span v-if="selectedReceipt.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', paymentStatusClass(selectedReceipt.paymentStatus)]">
                    📦 Đã nhận hàng
                  </span>
                </div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nhánh nguồn</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.sourceBranchName || '—' }}</div>
              </div>
              <div v-if="selectedReceipt.type === 'EXPORT'">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Khách hàng</div>
                <div class="font-semibold text-[#364a63]">{{ getCustomerName(selectedReceipt) }}</div>
              </div>
              <div v-else>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nhánh đích</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.destBranchName || '—' }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Người lập</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.createdByName }}</div>
                <div v-if="selectedReceipt.stocktakeByName" class="text-xs text-purple-600 font-bold mt-1 flex items-center gap-1.5" title="Người kiểm kê"><i class="fas fa-clipboard-check"></i> Kiểm kê: {{ selectedReceipt.stocktakeByName }}</div>
                <div v-else-if="selectedReceipt.status === 'COMPLETED' && (selectedReceipt.type === 'IMPORT' || selectedReceipt.type === 'TRANSFER') && selectedReceipt.createdByRole === 'STAFF'" class="text-xs text-purple-600 font-bold mt-1 flex items-center gap-1.5 opacity-60" title="Người kiểm kê (Dữ liệu cũ)"><i class="fas fa-clipboard-check"></i> Kiểm kê: {{ selectedReceipt.createdByName }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ngày tạo</div>
                <div class="font-semibold text-[#364a63]">{{ formatDateTime(selectedReceipt.createdAt) }}</div>
              </div>
              <div v-if="selectedReceipt.type === 'EXPORT'">
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
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null)">SL Gửi</th>
                      <th class="px-4 py-2.5 text-right font-bold text-teal-600" v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null)">SL Nhận</th>
                      <th class="px-4 py-2.5 text-right font-bold text-amber-500" v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null)">Thiếu</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-else>SL</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">Đơn giá</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-[#f1f5f9]">
                    <tr v-for="d in selectedReceipt.details" :key="d.id" class="hover:bg-[#f8f9fa]/50">
                      <td class="px-4 py-3">
                        <div class="font-semibold text-[#364a63]">{{ d.productName }}</div>
                      </td>
                      <td class="px-4 py-3 text-center text-[#8094ae]">{{ formatDate(d.manufacturingDate) }}</td>
                      <td class="px-4 py-3 text-center text-[#8094ae]">{{ formatDate(d.expirationDate) }}</td>
                      <td class="px-4 py-3 text-right font-bold" v-if="d.receivedQuantity !== null">{{ d.quantity }}</td>
                      <td class="px-4 py-3 text-right font-bold text-teal-600" v-if="d.receivedQuantity !== null">
                        {{ d.receivedQuantity }}
                      </td>
                      <td class="px-4 py-3 text-right font-bold text-amber-500" v-if="d.receivedQuantity !== null">
                        {{ d.quantity > d.receivedQuantity ? (d.quantity - d.receivedQuantity) : '-' }}
                      </td>
                      <td class="px-4 py-3 text-right font-bold" v-else>{{ d.quantity }}</td>
                      <td class="px-4 py-3 text-right" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">{{ formatVND(d.price) }}</td>
<td class="px-4 py-3 text-right font-bold text-[#4361ee]" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">{{ formatVND(d.quantity * d.price) }}</td>
                    </tr>
                  </tbody>
                  <tfoot v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">
                    <tr class="bg-[#f8f9fa]">
                      <td :colspan="(selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null)) ? 7 : 5" class="px-4 py-2.5 text-right font-bold text-[#8094ae] text-xs uppercase">Tổng cộng</td>
                      <td class="px-4 py-2.5 text-right font-extrabold text-[#4361ee]">
                        {{ formatVND((selectedReceipt.details || []).reduce((s: number, d: any) => s + d.quantity * d.price, 0)) }}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
              
              <!-- Shortfall reasons section -->
              <div v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null && x.receivedQuantity < x.quantity)" class="mt-4 p-4 bg-red-50 rounded-xl border border-red-100">
                <div class="text-xs font-bold text-red-600 uppercase mb-2 flex items-center gap-1.5"><i class="fas fa-exclamation-circle"></i> Lý do hao hụt</div>
                <div class="space-y-1.5">
                  <div v-for="d in selectedReceipt.details.filter((x: any) => x.receivedQuantity !== null && x.receivedQuantity < x.quantity)" :key="'reason-'+d.id" class="text-sm">
                    <span class="font-bold text-red-700">- {{ d.productName }} (Thiếu: {{ d.quantity - d.receivedQuantity }}):</span>
                    <span class="text-red-600 ml-1 whitespace-pre-wrap break-words">{{ d.shortfallReason }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Approve & Cancel actions -->
            <div v-if="canApproveReceipt(selectedReceipt)" class="mt-8 pt-5 border-t flex flex-wrap gap-4">
              <button @click="approveReceipt(selectedReceipt)" :disabled="approvingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-[#10b981] hover:bg-[#059669] text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-check-circle" v-if="approvingId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i> 
                {{ approveReceiptText(selectedReceipt) }}
              </button>
              <button @click="cancelReceipt(selectedReceipt)" v-if="canCancelReceipt(selectedReceipt)"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Hủy phiếu
              </button>
            </div>
            <div v-else-if="canCancelReceipt(selectedReceipt)" class="mt-8 pt-5 border-t flex gap-4">
              <button @click="cancelReceipt(selectedReceipt)"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Hủy phiếu
              </button>
            </div>

            <!-- Shortfall Approval Actions -->
            <div v-if="canApproveShortfallManager(selectedReceipt) || canApproveShortfallAdmin(selectedReceipt)" class="mt-8 pt-5 border-t flex flex-wrap gap-4">
              <button @click="approveShortfall(selectedReceipt, true)" :disabled="approvingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-check-double" v-if="approvingId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i> 
                {{ canApproveShortfallAdmin(selectedReceipt) ? 'Duyệt báo thiếu' : (selectedReceipt.type === 'TRANSFER' ? 'Duyệt báo thiếu (Gửi Manager Nguồn)' : 'Duyệt báo thiếu (Gửi Admin)') }}
              </button>
              <button @click="approveShortfall(selectedReceipt, false)" :disabled="approvingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-gray-500 hover:bg-gray-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-times"></i> Từ chối
              </button>
            </div>

            <!-- Mark as Paid -->
            <div v-if="selectedReceipt.status === 'COMPLETED' && selectedReceipt.type === 'EXPORT' && (selectedReceipt.paymentStatus === 'UNPAID' || selectedReceipt.paymentStatus === 'Chưa thanh toán')" class="mt-8 pt-5 border-t">
              <button @click="markAsPaid(selectedReceipt)" :disabled="markingPaidId === selectedReceipt.id"
                class="px-5 py-2.5 bg-[#f59e0b] hover:bg-[#d97706] text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-hand-holding-usd" v-if="markingPaidId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i>
                Xác nhận Đã Thanh Toán
              </button>
              <p class="text-xs text-gray-500 mt-2"><i class="fas fa-info-circle"></i> Công nợ khách hàng sẽ được cấn trừ tương ứng khi xác nhận.</p>
            </div>

            <!-- Confirm Receive (Transfer) -->
            <div v-if="canConfirmTransfer(selectedReceipt)" class="mt-8 pt-5 border-t">
              <button @click="openConfirmTransferModal(selectedReceipt)"
                class="px-5 py-2.5 bg-sky-500 hover:bg-sky-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-box-open"></i> Xác nhận Nhận Hàng & Cộng Kho
              </button>
            </div>

            <!-- Confirm Stocktake (Import) -->
            <div v-if="canConfirmStocktake(selectedReceipt)" class="mt-8 pt-5 border-t">
              <button @click="openStocktakeModal(selectedReceipt)"
                class="px-5 py-2.5 bg-purple-500 hover:bg-purple-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-boxes"></i> Thực hiện Kiểm kê & Chấp nhận
              </button>
              <p class="text-xs text-gray-500 mt-2"><i class="fas fa-info-circle"></i> Vui lòng đếm lại thực tế hàng hóa tại kho trước khi xác nhận cộng kho.</p>
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
          <div class="flex items-center justify-between px-6 py-4 bg-[#1e293b] text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">Lập phiếu kho</div>
              <div class="font-bold text-lg">{{ createForm.type === 'TRANSFER' ? 'Tạo phiếu xin hàng (DRAFT)' : 'Tạo phiếu nháp (DRAFT)' }}</div>
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
                <div v-if="!isSpecificRoute">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Loại phiếu <span class="text-red-500">*</span></label>
                  <select v-model="createForm.type" @change="onTypeChange"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="IMPORT" v-if="user?.branchId !== headBranch?.id && !isManager">📥 Nhập kho</option>
                    <option value="EXPORT">📤 Xuất bán</option>
                    <option value="TRANSFER">🔄 Điều chuyển</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT' || createForm.type === 'TRANSFER'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh nguồn <span class="text-red-500" v-if="createForm.type === 'TRANSFER'">*</span></label>
                  <select v-model="createForm.sourceBranchId" :disabled="createForm.type === 'IMPORT'"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae] cursor-not-allowed">
                    <option value="" v-if="createForm.type === 'IMPORT'">{{ headBranch ? headBranch.name : '-- Kho Tổng --' }}</option>
                    <option value="" v-if="createForm.type === 'TRANSFER'">-- Chọn chi nhánh nguồn --</option>
                    <option v-for="b in branches.filter(x => x.id !== createForm.destBranchId)" :key="b.id" :value="b.id" v-show="createForm.type === 'TRANSFER'">{{ b.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT' || createForm.type === 'TRANSFER' || createForm.type === 'ADJUST_IN'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh đích</label>
                  <select v-model="createForm.destBranchId" :disabled="createForm.type === 'IMPORT' || createForm.type === 'TRANSFER'"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae]">
                    <option value="">-- Chọn chi nhánh --</option>
                    <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
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
                <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'" class="col-span-2 sm:col-span-1">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Trạng thái thanh toán</label>
                  <select v-model="createForm.paymentStatus"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="UNPAID">Chưa thanh toán</option>
                    <option value="PAID">Đã thanh toán</option>
                  </select>
                </div>
                <div class="col-span-2">
                  <div class="flex justify-between items-center mb-1.5">
                    <label class="block text-xs font-bold text-[#8094ae] uppercase">Ghi chú</label>
                    <span class="text-[10px] text-[#8094ae]">{{ createForm.description?.length || 0 }}/500</span>
                  </div>
                  <textarea v-model="createForm.description" maxlength="500" placeholder="Ghi chú (tuỳ chọn)..."
                    class="w-full h-20 p-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none resize-y"></textarea>
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
                    <div class="space-y-4">
                      <!-- Sản phẩm & Lô sản xuất (Chiếm toàn bộ chiều ngang) -->
                      <div class="grid grid-cols-12 gap-4">
                        <div class="col-span-12 lg:col-span-6">
                          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Sản phẩm <span class="text-red-500">*</span></label>
                          <select v-model="d.productId" @change="onProductChange(d)"
                            class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                            <option value="">-- Chọn sản phẩm --</option>
                            <option v-for="p in getAvailableProductsForRow(idx)" :key="p.id" :value="p.id">{{ p.name }} ({{ p.sku }})</option>
                          </select>
                        </div>
                        
                        <div class="col-span-12 lg:col-span-6" v-if="d.productId">
                          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Lô sản xuất <span class="text-red-500">*</span></label>
                          <div class="flex gap-2">
                            <select v-if="!d.isNewBatch" v-model="d.batchCode" @change="onBatchChange(d)"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                              <option value="">-- Chọn lô --</option>
                              <option v-for="b in getBatchesForProduct(d.productId)" :key="b.batchCode" :value="b.batchCode">
                                {{ b.batchCode }} (Tồn: {{ b.quantity }})
                              </option>
                            </select>
                            <input v-else v-model="d.batchCode" type="text" placeholder="Nhập mã lô mới..."
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium outline-none focus:border-[#4361ee]" />
                            
                            <button v-if="createForm.type === 'IMPORT' && createForm.sourceBranchId === createForm.destBranchId" @click="d.isNewBatch = !d.isNewBatch; d.batchCode = ''" 
                                    class="px-3 h-10 border border-[#e2e8f0] rounded-xl text-xs font-bold bg-white hover:bg-gray-50 whitespace-nowrap shadow-sm transition-all text-[#364a63]">
                              <i :class="d.isNewBatch ? 'fas fa-list text-[#4361ee] mr-1' : 'fas fa-plus text-[#10b981] mr-1'"></i> {{ d.isNewBatch ? 'Chọn lô có sẵn' : 'Tạo lô mới' }}
                            </button>
                          </div>
                        </div>
                      </div>

                      <!-- Thông tin Số lượng, Tiền & NSX/HSD (Nhóm trong khung nền xám nhạt để dễ nhìn) -->
                      <div v-if="d.productId" class="p-4 bg-[#f8f9fa] rounded-xl border border-[#e2e8f0] space-y-4">
                        <!-- Row 1: Số lượng, Đơn giá, Thành tiền -->
                        <div :class="(createForm.type === 'IMPORT' || createForm.type === 'TRANSFER') ? 'grid grid-cols-1' : 'grid grid-cols-3 gap-5'">
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Số lượng <span class="text-red-500">*</span></label>
                            <div class="flex items-center h-10 bg-white border border-[#e2e8f0] rounded-xl overflow-hidden focus-within:border-[#4361ee] focus-within:ring-2 focus-within:ring-[#4361ee]/20">
                              <input v-model.number="d.quantity" type="number" min="1" @input="constrainQuantity(d)" @blur="onQuantityBlur(d)" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                                :disabled="!d.productId || (!d.isNewBatch && !d.batchCode)"
                                class="w-full h-full px-3 text-sm font-bold outline-none disabled:bg-gray-100 disabled:text-gray-400 bg-transparent" />
                              <div v-if="((createForm.type === 'IMPORT' && createForm.sourceBranchId === createForm.destBranchId) || createForm.type === 'ADJUST_IN') && getGlobalQuantity(d) !== null" 
                                   class="px-3 h-full flex items-center bg-gray-50 border-l border-[#e2e8f0] text-xs font-bold text-[#8094ae] whitespace-nowrap">
                                / {{ getGlobalQuantity(d) }}
                              </div>
                              <div v-else-if="getMaxQuantity(d) !== null" 
                                   class="px-3 h-full flex items-center bg-gray-50 border-l border-[#e2e8f0] text-xs font-bold text-[#8094ae] whitespace-nowrap">
                                / {{ getMaxQuantity(d) }}
                              </div>
                            </div>
                          </div>
                          
                          <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'">
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Đơn giá</label>
                            <input v-model.number="d.price" type="number" min="0" readonly
                              class="w-full h-10 px-3 border border-[#e2e8f0] bg-gray-100 rounded-xl text-sm font-bold outline-none cursor-not-allowed text-[#8094ae]" />
                          </div>
                          
                          <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'">
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Thành tiền</label>
                            <div class="w-full h-10 px-3 border border-transparent flex items-center text-sm font-bold text-[#4361ee] bg-[#eef2ff] rounded-xl overflow-x-auto whitespace-nowrap hide-scrollbar">
                              {{ formatVND(d.quantity * (d.price || 0)) }}
                            </div>
                          </div>
                        </div>

                        <!-- Row 2: NSX, HSD -->
                        <div v-if="selectedProductHasExpiry(d) && (d.isNewBatch || d.batchCode)" class="grid grid-cols-2 gap-5 pt-4 border-t border-[#e2e8f0]">
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Ngày sản xuất</label>
                            <input v-model="d.manufacturingDate" type="date" :disabled="!d.isNewBatch"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-gray-100 disabled:text-gray-500" />
                          </div>
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Hạn sử dụng</label>
                            <input v-model="d.expirationDate" type="date" :disabled="!d.isNewBatch"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-gray-100 disabled:text-gray-500" />
                          </div>
                        </div>
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
    <!-- STOCKTAKE MODAL (IMPORT) -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <div v-if="showStocktakeModal" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[110] flex items-center justify-center p-4">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-purple-500 to-fuchsia-400 text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">Kiểm kê & Nhập kho</div>
              <div class="font-bold text-lg">Phiếu: {{ stocktakeReceipt?.code }}</div>
            </div>
            <button @click="showStocktakeModal = false" class="w-9 h-9 flex items-center justify-center rounded-xl bg-white/20 hover:bg-white/30 transition-all">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div class="bg-purple-50 border border-purple-200 rounded-xl p-4 text-sm text-purple-700">
              <i class="fas fa-info-circle mr-2"></i>
              Nhập <strong>số lượng thực đếm</strong> tại kho. Nếu có hao hụt, vui lòng ghi rõ lý do để đối soát. Hàng hóa sẽ được cộng vào kho tương ứng với số lượng thực đếm.
            </div>

            <div class="space-y-3 custom-scrollbar max-h-[50vh] overflow-y-auto">
              <div v-for="item in stocktakeItems" :key="item.receiptDetailId"
                class="border border-[#e2e8f0] rounded-xl p-4">
                <div class="flex items-center justify-between mb-2">
                  <span class="font-semibold text-sm text-[#364a63]">{{ item.productName }}</span>
                  <span class="text-xs text-[#8094ae]">Số trên phiếu: <strong>{{ item.sentQty }}</strong></span>
                </div>
                <div class="flex items-center gap-3">
                  <label class="text-xs text-[#8094ae] whitespace-nowrap">Thực đếm:</label>
                  <input v-model.number="item.actualQuantity" type="number" :min="0" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                    @input="(e) => { const target = e.target as HTMLInputElement; let v = parseInt(target.value, 10); if(isNaN(v)||v<0) v=0; if(v>item.sentQty) v=item.sentQty; item.actualQuantity = v; target.value = String(v); }"
                    class="flex-1 h-9 px-3 border rounded-lg text-sm focus:ring-2 focus:ring-purple-400/20 focus:border-purple-400 outline-none"
                    :class="item.actualQuantity < item.sentQty ? 'border-amber-400 bg-amber-50' : 'border-[#e2e8f0]'" />
                  <span v-if="item.actualQuantity < item.sentQty"
                    class="text-xs font-bold text-amber-600 whitespace-nowrap">
                    ⚠️ Hao hụt: {{ item.sentQty - item.actualQuantity }}
                  </span>
                  <span v-else class="text-xs font-bold text-green-600 whitespace-nowrap">✅ Đủ</span>
                </div>
                <div v-if="item.actualQuantity < item.sentQty" class="mt-3 bg-red-50 p-3 rounded-lg border border-red-100 flex items-start gap-3">
                  <label class="text-xs font-bold text-red-600 whitespace-nowrap mt-2">Lý do <span class="text-red-500">*</span></label>
                  <textarea v-model="item.shortfallReason" rows="2" placeholder="VD: Hư hỏng, thiếu hàng..."
                    class="flex-1 px-3 py-2 border border-red-200 rounded-lg text-sm focus:ring-2 focus:ring-red-400/20 focus:border-red-400 outline-none bg-white resize-none"></textarea>
                </div>
              </div>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-[#f1f5f9] flex justify-end gap-3 bg-[#f8f9fa]/50">
            <button @click="showStocktakeModal = false"
              class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63] text-sm hover:bg-[#f1f5f9] transition-all">
              Đóng
            </button>
            <button @click="submitConfirmStocktake" :disabled="submittingStocktake"
              class="px-6 py-2.5 bg-purple-500 hover:bg-purple-600 text-white rounded-xl font-bold text-sm transition-all disabled:opacity-60 flex items-center gap-2">
              <i class="fas fa-spinner fa-spin" v-if="submittingStocktake"></i>
              <i class="fas fa-boxes" v-else></i>
              Xác nhận Kiểm kê
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- CONFIRM TRANSFER MODAL -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <Teleport to="body">
      <div v-if="showConfirmModal && confirmingReceipt"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[110] flex items-center justify-center p-4">
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
              Nhập <strong>số lượng thực tế nhận được</strong> cho từng sản phẩm. Hao hụt trong quá trình vận chuyển (nếu có) sẽ được ghi nhận trực tiếp vào chi tiết phiếu này để đối soát.
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
                  <input v-model.number="item.actualQuantity" type="number" :min="0" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                    @input="(e) => { const target = e.target as HTMLInputElement; let v = parseInt(target.value, 10); if(isNaN(v)||v<0) v=0; if(v>item.sentQty) v=item.sentQty; item.actualQuantity = v; target.value = String(v); }"
                    class="flex-1 h-9 px-3 border rounded-lg text-sm focus:ring-2 focus:ring-sky-400/20 focus:border-sky-400 outline-none"
                    :class="item.actualQuantity < item.sentQty ? 'border-amber-400 bg-amber-50' : 'border-[#e2e8f0]'" />
                  <span v-if="item.actualQuantity < item.sentQty"
                    class="text-xs font-bold text-amber-600 whitespace-nowrap">
                    ⚠️ Hao hụt: {{ item.sentQty - item.actualQuantity }}
                  </span>
                  <span v-else class="text-xs font-bold text-green-600 whitespace-nowrap">✅ Đủ</span>
                </div>
                <!-- Input for shortfall reason -->
                <div v-if="item.actualQuantity < item.sentQty" class="mt-3 bg-red-50 p-3 rounded-lg border border-red-100 flex items-start gap-3">
                  <label class="text-xs font-bold text-red-600 whitespace-nowrap mt-2">Lý do hao hụt <span class="text-red-500">*</span></label>
                  <textarea v-model="item.shortfallReason" rows="2" placeholder="VD: Rơi vỡ, ẩm mốc, thiếu hàng..."
                    class="flex-1 px-3 py-2 border border-red-200 rounded-lg text-sm focus:ring-2 focus:ring-red-400/20 focus:border-red-400 outline-none bg-white resize-none"></textarea>
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

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- DIRECT IMPORT MODAL (THÊM SẢN PHẨM) -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <AppModal 
      :show="showDirectImportModal" 
      title="Thêm sản phẩm (Tạo Phiếu Nhập)" 
      size="md" 
      @close="showDirectImportModal = false"
    >
      <div class="p-6 space-y-4 text-sm">
        <!-- Dòng 1: Danh mục -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Danh mục</label>
          <select 
            v-model="directImportForm.categoryId" 
            @change="directImportForm.productId = ''; directImportForm.manufacturingDate = ''; directImportForm.expirationDate = ''"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all shadow-sm"
          >
            <option value="">-- Chọn danh mục --</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
        </div>

        <!-- Dòng 2: Sản phẩm -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Sản phẩm</label>
          <select 
            v-model="directImportForm.productId" 
            :disabled="!directImportForm.categoryId"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all shadow-sm disabled:bg-[#f1f5f9] disabled:text-slate-400"
          >
            <option value="">-- Chọn sản phẩm --</option>
            <option v-for="p in filteredProductsForDirectImport" :key="p.id" :value="p.id">
              [{{ p.sku }}] {{ p.name }}
            </option>
          </select>
        </div>

        <!-- Dòng 3: Đơn vị tính, Giá nhập & Giá bán -->
        <div class="grid grid-cols-3 gap-4">
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Đơn vị tính</label>
            <input 
              :value="selectedProductInDirectImport ? selectedProductInDirectImport.unit : '-'" 
              type="text" 
              disabled 
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#eef2ff] text-[#4361ee] rounded-xl text-sm outline-none font-extrabold transition-all" 
            />
          </div>
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Giá nhập</label>
            <input 
              v-model.number="directImportForm.price" 
              type="number" 
              min="0"
              disabled
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none text-slate-500 font-semibold transition-all" 
            />
          </div>
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Giá bán</label>
            <input 
              :value="selectedProductInDirectImport ? formatVND(selectedProductInDirectImport.price) : '-'" 
              type="text" 
              disabled 
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none text-slate-500 font-semibold transition-all" 
            />
          </div>
        </div>

        <!-- Dòng 4: Mã lô sản xuất & Số lượng nhập -->
        <div class="grid grid-cols-12 gap-4">
          <div class="col-span-8">
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mã lô sản xuất</label>
            <div class="flex gap-2">
              <select v-if="!directImportForm.isNewBatch && directImportBatches.length > 0" 
                v-model="directImportForm.batchCode" 
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all">
                <option value="">-- Chọn lô --</option>
                <option v-for="b in directImportBatches" :key="b.batchCode" :value="b.batchCode">
                  {{ b.batchCode }} (Tồn: {{ b.quantity }})
                </option>
              </select>
              <input v-else 
                v-model="directImportForm.batchCode" 
                type="text" 
                placeholder="Ví dụ: BATCH-01, MILK-2026..." 
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all" 
              />
              <button @click="directImportForm.isNewBatch = !directImportForm.isNewBatch; directImportForm.batchCode = ''" 
                class="px-3 rounded-xl border border-[#e2e8f0] bg-[#f8f9fa] text-[#4361ee] hover:bg-[#eef2ff] transition-all flex items-center justify-center font-bold text-xs"
                title="Chuyển đổi nhập lô mới/cũ">
                <i class="fas fa-sync-alt"></i>
              </button>
            </div>
          </div>
          <div class="col-span-4">
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Số lượng nhập</label>
            <input 
              v-model.number="directImportForm.quantity" 
              type="number" 
              min="1" 
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all" 
            />
          </div>
        </div>

        <!-- Ngày sản xuất (NSX) - Bắt buộc -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Ngày sản xuất (NSX)</label>
          <input 
            v-model="directImportForm.manufacturingDate" 
            type="date" 
            :disabled="isMfgDateLocked"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all disabled:opacity-60 disabled:cursor-not-allowed" 
          />
        </div>
        <!-- Checkbox quản lý theo hạn dùng -->
        <div class="flex items-center gap-2 py-1">
          <input 
            id="directImportHasExpiry" 
            v-model="directImportForm.hasExpiry" 
            type="checkbox" 
            :disabled="isExpDateLocked"
            class="w-5 h-5 accent-[#4361ee] cursor-pointer rounded-md border-slate-300 disabled:opacity-60 disabled:cursor-not-allowed" 
          />
          <label for="directImportHasExpiry" class="cursor-pointer select-none font-bold text-xs text-[#8094ae] uppercase tracking-wider">
            Sản phẩm quản lý theo hạn dùng
          </label>
        </div>

        <!-- Các trường hạn dùng (chỉ hiện khi hasExpiry được tích chọn) -->
        <Transition name="fade">
          <div v-if="directImportForm.hasExpiry" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Hạn sử dụng (HSD)</label>
                <input 
                  v-model="directImportForm.expirationDate" 
                  type="date" 
                  :disabled="isExpDateLocked"
                  class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all disabled:opacity-60 disabled:cursor-not-allowed" 
                />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Số ngày cảnh báo hạn dùng</label>
                <input 
                  v-model.number="directImportForm.expiryWarningDays" 
                  type="number" 
                  min="1"
                  placeholder="Mặc định: 30" 
                  class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all" 
                />
              </div>
            </div>
          </div>
        </Transition>

        <div class="flex gap-3 pt-4 border-t border-[#f1f5f9]">
          <button 
            class="flex-1 h-11 bg-[#f8f9fa] hover:bg-[#e2e8f0] text-[#364a63] rounded-xl text-sm font-bold transition-colors" 
            @click="showDirectImportModal = false"
          >
            Hủy bỏ
          </button>
          <button 
            class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center justify-center gap-2" 
            :disabled="submittingDirectImport"
            @click="submitDirectImport"
          >
            <i v-if="submittingDirectImport" class="fas fa-spinner fa-spin"></i>
            Xác nhận tạo Phiếu
          </button>
        </div>
      </div>
    </AppModal>

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
