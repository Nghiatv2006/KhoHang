<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'

const props = defineProps<{
  receiptType?: 'IMPORT' | 'EXPORT' | 'TRANSFER' | 'ADJUST_OUT'
}>()

const toast = useToast()

// ──────────────────────────────────────────────────────────────
// PAGE CONFIG — Động theo receiptType prop
// ──────────────────────────────────────────────────────────────
const pageConfig = computed(() => {
  const configs: Record<string, { title: string; desc: string; icon: string; btnLabel: string }> = {
    IMPORT: {
      title: 'Quản lý Nhập Kho',
      desc: 'Theo dõi, lập và phê duyệt các phiếu nhập kho',
      icon: 'fas fa-download',
      btnLabel: 'Lập phiếu nhập'
    },
    EXPORT: {
      title: 'Quản lý Hóa Đơn',
      desc: 'Theo dõi, lập và quản lý các hóa đơn xuất bán',
      icon: 'fas fa-file-invoice-dollar',
      btnLabel: 'Lập hóa đơn'
    },
    TRANSFER: {
      title: 'Quản lý Điều Chuyển',
      desc: 'Theo dõi, lập và phê duyệt các phiếu điều chuyển kho',
      icon: 'fas fa-exchange-alt',
      btnLabel: 'Lập phiếu điều chuyển'
    },
    ADJUST_OUT: {
      title: 'Quản lý Tiêu Hủy',
      desc: 'Theo dõi, lập và quản lý các phiếu tiêu hủy hàng hóa',
      icon: 'fas fa-trash-alt',
      btnLabel: 'Lập phiếu tiêu hủy'
    }
  }
  return configs[props.receiptType || 'IMPORT'] || configs.IMPORT
})
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isManager = computed(() => user.value?.role === 'MANAGER')
// const isStaff = computed(() => user.value?.role === 'STAFF')
// const canApprove = computed(() => isAdmin.value || isManager.value)

function canApproveReceipt(r: any) {
  if (r.status === 'DRAFT') {
      if (r.type === 'ADJUST_OUT') {
          // Everyone can click the button, backend will handle routing to PENDING_ADMIN or COMPLETED
          if (r.sourceBranchId === user.value?.branchId || isAdmin.value) return true;
      }
      if (r.type === 'EXPORT' && isAdmin.value && r.sourceBranchId !== 1) return false;
      if (isAdmin.value) return true;
      if (isManager.value) {
          if (r.type === 'IMPORT' || r.type === 'ADJUST_IN' || r.type === 'TRANSFER') {
              if (r.destBranchId === user.value?.branchId) return true;
          } else {
              if (r.sourceBranchId === user.value?.branchId) return true;
          }
      }
      if (user.value?.role === 'STAFF') {
          if (r.type === 'EXPORT' && r.sourceBranchId === user.value?.branchId) return true;
      }
      return false;
  }
  if (r.status === 'PENDING_ADMIN') {
      if (r.type === 'ADJUST_OUT') {
          if (isAdmin.value || isManager.value) {
              if (isAdmin.value || r.sourceBranchId === user.value?.branchId) return true;
          }
          return false;
      }
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
    if (r.type === 'ADJUST_OUT' && r.status === 'DRAFT') {
        return "Xác nhận & Xử lý";
    }
    if (r.type === 'ADJUST_OUT' && r.status === 'PENDING_ADMIN') {
        return "Duyệt tiêu hủy";
    }
    if (r.type === 'IMPORT' && r.status === 'DRAFT' && isManager.value && !isAdmin.value) {
        return "Duyệt (Gửi Admin)";
    }
    if (r.type === 'IMPORT' && r.status === 'PENDING_ADMIN' && isAdmin.value) {
        return "Chấp nhận nhập kho";
    }
    if (r.type === 'TRANSFER' && r.status === 'DRAFT') {
        return "Duyệt (Gửi Manager chi nhánh nguồn)";
    }
    if (r.type === 'TRANSFER' && r.status === 'PENDING_ADMIN') {
        return "Duyệt điều chuyển";
    }
    return "Phê duyệt";
}

function canCancelReceipt(r: any) {
  if (r.status !== 'DRAFT' && r.status !== 'PENDING_ADMIN') return false;
  if (r.type === 'EXPORT' && isAdmin.value && r.sourceBranchId !== 1) return false;
  if (isAdmin.value) return true;
  if (!isManager.value) return false;
  
  // Manager can only cancel receipts if they are the "requesting branch" (the one who initiated it).
  // The responding branch manager cannot cancel it when it's DRAFT or PENDING_ADMIN.
  let requestingBranchId = null;
  if (['IMPORT', 'TRANSFER'].includes(r.type)) {
      requestingBranchId = r.destBranchId;
  } else {
      requestingBranchId = r.sourceBranchId;
  }
  
  return requestingBranchId === user.value?.branchId; 
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
const initToday = new Date()
const initFmt = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
const initFirstDay = new Date(initToday.getFullYear(), initToday.getMonth(), 1)

const filterType = ref(props.receiptType || '')
const filterStatus = ref('')
const searchKeyword = ref('')
const filterTimeRange = ref('this_month')
const filterStartDate = ref(initFmt(initFirstDay))
const filterEndDate = ref(initFmt(initToday))
const filterDeviation = ref('')

watch(filterTimeRange, (val) => {
  const today = new Date()
  const fmt = (d: Date) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }
  
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
  } else if (val === 'this_month') {
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
    filterStartDate.value = fmt(firstDay)
    filterEndDate.value = fmt(today)
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

// Receipts đã lọc theo receiptType prop (luôn lọc trước)
const typeFilteredReceipts = computed(() => {
  let list = receipts.value

  // Hard filter to ensure visibility rules on frontend
  list = list.filter(r => {
    // 1. Staff visibility rule: Staff can only see their own receipts, OR incoming receipts that are ready for processing
    if (user.value?.role === 'STAFF') {
      if (r.createdById !== user.value?.id) {
        const isIncoming = (r.type === 'IMPORT' || r.type === 'TRANSFER') && 
                           r.destBranchId === user.value?.branchId && 
                           ['PENDING_STOCKTAKE', 'COMPLETED', 'PENDING_SHORTFALL_MANAGER', 'PENDING_SHORTFALL_ADMIN'].includes(r.status);
        if (!isIncoming) return false;
      }
    }

    // 2. Draft visibility rule: Drafts and Pending Staff Confirm should ONLY be visible to the creator's branch.
    // For example, an IMPORT draft created by HCM (dest=HCM, source=Hanoi) should NOT be seen by Manager Hanoi yet.
    if (r.status === 'DRAFT' || r.status === 'PENDING_STAFF_CONFIRM') {
      let creatorBranchId = null;
      if (['IMPORT', 'TRANSFER'].includes(r.type)) {
        creatorBranchId = r.destBranchId;
      } else {
        creatorBranchId = r.sourceBranchId;
      }
      if (creatorBranchId !== user.value?.branchId) return false;
    }

    // 3. Admin cross-branch transfer rule
    if (r.type === 'TRANSFER') {
      if (isAdmin.value && r.sourceBranchId !== 1 && r.destBranchId !== 1) return false;
    }

    // 4. Manager visibility rule regarding Admin's receipts:
    // Managers should NOT see operations performed by Admin at the Head Warehouse (Kho Tổng),
    // UNLESS it's a transfer/import explicitly destined for the Manager's branch.
    if (user.value?.role === 'MANAGER' && r.createdByRole === 'ADMIN') {
      const isIncoming = (r.type === 'TRANSFER' || r.type === 'IMPORT') && r.destBranchId === user.value?.branchId;
      if (!isIncoming) return false;
    }

    // 5. EXPORT/IMPORT filters from HEAD
    if (r.type === 'EXPORT') {
      return r.sourceBranchId === user.value?.branchId
    }
    if (r.type === 'IMPORT' && !isAdmin.value) {
      if (r.status === 'PENDING_COMPENSATION' && r.sourceBranchId === user.value?.branchId) {
        return true
      }
      return r.destBranchId === user.value?.branchId
    }

    return true
  })

  if (props.receiptType) return list.filter(r => r.type === props.receiptType)
  return list
})

const filteredReceipts = computed(() => {
  let result = [...typeFilteredReceipts.value]
  // Nếu có prop receiptType, không cần lọc thêm theo filterType
  if (!props.receiptType && filterType.value) result = result.filter(r => r.type === filterType.value)
  if (filterStatus.value) {
    if (filterStatus.value === 'UNPAID') {
      result = result.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán'))
    } else if (filterStatus.value === 'COMPENSATION') {
      result = result.filter(r => r.code && r.code.startsWith('COMP-'))
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
    if (pRes.ok) {
      const pData = await pRes.json()
      products.value = pData.content || pData
    }
    if (bRes.ok) branches.value = await bRes.json()
    if (cRes.ok) customers.value = await cRes.json()
    if (catRes.ok) {
      const catData = await catRes.json()
      categories.value = catData.content || catData
    }
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
const isHeadBranch = computed(() => user.value?.branchId === headBranch.value?.id)
// const subBranches = computed(() => branches.value.filter(b => b.id !== headBranch.value?.id))

// ──────────────────────────────────────────────────────────────
// STATS
// ──────────────────────────────────────────────────────────────
const statDraft = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'DRAFT').length)
const statCompleted = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'COMPLETED').length)
const statCancelled = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'CANCELLED').length)

const statUnpaid = computed(() => typeFilteredReceipts.value.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán')).length)

// Nhập kho: Chờ Admin duyệt
const statPendingAdmin = computed(() => {
  if (props.receiptType === 'TRANSFER') {
    return typeFilteredReceipts.value.filter(r => r.code && r.code.startsWith('COMP-')).length;
  }
  return typeFilteredReceipts.value.filter(r => r.status === 'PENDING_ADMIN').length;
})

// ──────────────────────────────────────────────────────────────
// DETAIL PANEL
// ──────────────────────────────────────────────────────────────
const selectedReceipt = ref<any>(null)
const showDetail = ref(false)

const isSpaceEasterEgg = ref(false)

async function openDetail(receipt: any) {
  isSpaceEasterEgg.value = Math.random() < 0.004;
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
      const res = await api.get('/api/inventories')
      if (res.ok) {
        globalInventories.value = await res.json()
      }
    } catch(e) {}
  }
}

// Bypassing TS6133 unused function warning
if (false as any) {
  openDirectImportModal();
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
  disposalReason?: string
  disposalMethod?: string
  attachmentUrl?: string
}>({
  type: 'IMPORT',
  sourceBranchId: user.value?.branchId || headBranch.value?.id || '',
  destBranchId: '',
  customerId: '',
  customerName: '',
  customerPhone: '',
  paymentStatus: 'UNPAID',
  description: '',
  details: [],
  disposalReason: '',
  disposalMethod: '',
  attachmentUrl: ''
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
  categoryId: number | ''
  productId: number | ''
  batchCode: string
  isNewBatch?: boolean
  hasExpiryDate?: boolean
  manufacturingDate: string
  expirationDate: string
  quantity: number
  price: number
  isCollapsed?: boolean
}

function openCreateModal() {
  isSpaceEasterEgg.value = Math.random() < 0.004;
  const defaultType = props.receiptType || ((user.value?.branchId !== headBranch.value?.id && !isManager.value) ? 'IMPORT' : 'EXPORT');
  createForm.value = {
    type: defaultType,
    sourceBranchId: user.value?.branchId || headBranch.value?.id || '',
    destBranchId: '',
    customerId: '',
    customerName: '',
    customerPhone: '',
    paymentStatus: 'UNPAID',
    description: '',
    details: [{ categoryId: '', productId: '', batchCode: '', isNewBatch: false, hasExpiryDate: false, manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 }]
  }
  onTypeChange()
  showCreateModal.value = true
}

function addDetailRow() {
  const isExternalImport = createForm.value.type === 'IMPORT' && !createForm.value.sourceBranchId;
  const today = new Date().toISOString().split('T')[0];
    createForm.value.details.push({ categoryId: '', productId: '', batchCode: '', isNewBatch: isExternalImport, hasExpiryDate: false, manufacturingDate: today, expirationDate: today, quantity: 1, price: 0, isCollapsed: false })
}

function removeDetailRow(index: number) {
  if (createForm.value.details.length <= 1) return
  createForm.value.details.splice(index, 1)
}

// Chi nhánh hiện tại có phải chi nhánh gốc (Hà Nội) không?


function onTypeChange() {
  const t = createForm.value.type
  if (t === 'IMPORT') {
    // Chi nhánh gốc (Hà Nội): nhập từ bên ngoài hệ thống (sourceBranchId = '')
    // Chi nhánh con (HCM, ...): nhập từ chi nhánh Hà Nội (sourceBranchId = headBranch.id)
    createForm.value.sourceBranchId = isHeadBranch.value ? '' : (headBranch.value?.id || '')
    createForm.value.destBranchId = user.value?.branchId || headBranch.value?.id || ''
  } else if (t === 'EXPORT') {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  } else if (t === 'TRANSFER') {
    // Điều chuyển mới: Chi nhánh đích (cần hàng) = chi nhánh hiện tại, nguồn = người dùng chọn
    createForm.value.sourceBranchId = ''
    createForm.value.destBranchId = user.value?.branchId || ''
  } else {
    createForm.value.sourceBranchId = user.value?.branchId || headBranch.value?.id || ''
    createForm.value.destBranchId = ''
  }
  // Reset products when changing type to avoid stale/out-of-stock products
  const isExternalImport = createForm.value.type === 'IMPORT' && !createForm.value.sourceBranchId;
  createForm.value.details = [{ categoryId: '', productId: '', batchCode: '', isNewBatch: isExternalImport, hasExpiryDate: false, manufacturingDate: '', expirationDate: '', quantity: 1, price: 0 }]
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
    // When IMPORT from headBranch (sourceBranchId is empty), still need to load
    // global inventories so getBatchesForProduct can filter by headBranch
    if (createForm.value.type === 'IMPORT' && globalInventories.value.length === 0) {
      try {
        const res = await api.get('/api/inventories/global')
        if (res.ok) {
          globalInventories.value = await res.json()
        }
      } catch(e) {}
    }
  }
}, { immediate: true })

const availableProducts = computed(() => {
  const t = createForm.value.type
  if (t === 'ADJUST_IN') {
    return products.value
  }
  if (createForm.value.sourceBranchId) {
    let validInventories = sourceInventories.value.filter(inv => inv.quantity > 0)
    
    // Ràng buộc cho phiếu tiêu hủy: Chỉ hàng sắp hết hạn (<= 14 ngày) hoặc đã hết hạn
    if (t === 'ADJUST_OUT') {
      const today = new Date().getTime()
      const fourteenDaysFromNow = today + 14 * 24 * 60 * 60 * 1000
      validInventories = validInventories.filter(inv => {
        if (!inv.expirationDate || inv.expirationDate === '1970-01-01' || inv.expirationDate === '2099-12-31') return false
        const expTime = new Date(inv.expirationDate).getTime()
        return expTime <= fourteenDaysFromNow
      })
    }

    const inStockIds = new Set(validInventories.map(inv => inv.productId))
    let result = products.value.filter(p => inStockIds.has(p.id))
    
    // Ràng buộc phiếu tiêu hủy: Chỉ chọn hàng Sữa hoặc Hữu cơ
    if (t === 'ADJUST_OUT') {
      result = result.filter(p => {
        const catName = categories.value.find(c => c.id === p.categoryId)?.name?.toLowerCase() || ''
        return catName.includes('sữa') || catName.includes('hữu cơ')
      })
    }
    
    return result
  }
  if (t === 'IMPORT') {
    if (createForm.value.destBranchId === headBranch.value?.id) {
      return products.value; // Kho tổng nhập từ nhà cung cấp -> hiển thị tất cả
    }
    // Chi nhánh con nhập từ kho tổng -> chỉ hiển thị các sản phẩm có tồn kho ở kho tổng
    const headBranchId = headBranch.value?.id
    const inStockIds = new Set(
      globalInventories.value
        .filter(inv => inv.branchId === headBranchId && inv.quantity > 0)
        .map(inv => inv.productId)
    )
    return products.value.filter(p => inStockIds.has(p.id))
  }
  // TRANSFER chưa chọn chi nhánh nguồn -> không hiển thị sản phẩm nào
  if (t === 'TRANSFER' && !createForm.value.sourceBranchId) {
    return []
  }
  return products.value
})

function getAvailableProductsForRow(index: number) {
  const row = createForm.value.details[index];
  const selectedIds = new Set(
    createForm.value.details
      .map((d, i) => i !== index ? Number(d.productId) : null)
      .filter(id => id !== null && !isNaN(id))
  )
  let products = availableProducts.value.filter(p => !selectedIds.has(p.id));
  if (row.categoryId) {
    products = products.filter(p => p.categoryId === Number(row.categoryId));
  }
  return products;
}

function getAvailableCategoriesForRow(index: number) {
  const row = createForm.value.details[index];
  const selectedIds = new Set(
    createForm.value.details
      .map((d, i) => i !== index ? Number(d.productId) : null)
      .filter(id => id !== null && !isNaN(id))
  )
  const remainingProducts = availableProducts.value.filter(p => !selectedIds.has(p.id));
  const categoryIdsWithProducts = new Set(remainingProducts.map(p => p.categoryId));
  if (row.categoryId) {
    categoryIdsWithProducts.add(Number(row.categoryId));
  }
  return categories.value.filter(c => categoryIdsWithProducts.has(c.id));
}

function onProductChange(row: DetailRow) {
  const p = products.value.find(x => x.id === Number(row.productId))
  row.batchCode = ''
  row.isNewBatch = (createForm.value.type === 'IMPORT' && !createForm.value.sourceBranchId) ? true : false;
  if (p) {
    row.categoryId = p.categoryId || ''
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

  // Tự động dọn dẹp danh mục bị kẹt cho các dòng khác chưa chọn sản phẩm
  createForm.value.details.forEach((d, idx) => {
    if (d !== row && d.categoryId && !d.productId) {
      const availProds = getAvailableProductsForRow(idx).filter(p => p.categoryId === Number(d.categoryId));
      if (availProds.length === 0) {
        d.categoryId = '';
      }
    }
  });
}

function getBatchesForProduct(productId: number | string | null) {
  if (!productId) return []
  if (createForm.value.type === 'ADJUST_IN') {
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
  // IMPORT without sourceBranchId means source is headBranch (Kho Tổng)
  // Only show batches from headBranch, not from all branches
  if (createForm.value.type === 'IMPORT' && !createForm.value.sourceBranchId) {
    const headBranchId = headBranch.value?.id
    const uniqueBatches = new Map()
    globalInventories.value
      .filter(inv => inv.productId === Number(productId) && inv.branchId === headBranchId)
      .forEach(inv => {
        if (!uniqueBatches.has(inv.batchCode)) {
          uniqueBatches.set(inv.batchCode, { ...inv })
        } else {
          const existing = uniqueBatches.get(inv.batchCode)
          existing.quantity += inv.quantity
        }
      })
    return Array.from(uniqueBatches.values())
  }
  let batches = sourceInventories.value
    .filter(inv => {
      if (inv.productId !== Number(productId) || inv.quantity <= 0) return false;
      if (createForm.value.type === 'DISPOSAL' && createForm.value.disposalReason === 'Hàng hết hạn sử dụng') {
        const todayStr = new Date().toISOString().substring(0, 10);
        if (!inv.hasExpiry || !inv.expirationDate || inv.expirationDate.substring(0, 10) >= todayStr) return false;
      }
      return true;
    })

  if (createForm.value.type === 'ADJUST_OUT') {
    const today = new Date().getTime()
    const fourteenDaysFromNow = today + 14 * 24 * 60 * 60 * 1000
    batches = batches.filter(inv => {
      if (!inv.expirationDate || inv.expirationDate === '1970-01-01' || inv.expirationDate === '2099-12-31') return false
      const expTime = new Date(inv.expirationDate).getTime()
      return expTime <= fourteenDaysFromNow
    })
  }

  return batches.map(inv => {
      const pendingQty = receipts.value
        .filter(r => r.status === 'DRAFT' && Number(r.sourceBranchId) === Number(createForm.value.sourceBranchId) && 
                (['EXPORT', 'TRANSFER', 'ADJUST_OUT', 'DISPOSAL'].includes(r.type) || 
                 (r.type === 'IMPORT' && Number(r.sourceBranchId) !== Number(r.destBranchId))))
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
    if (inv.expirationDate && inv.expirationDate !== '1970-01-01' && !inv.expirationDate.startsWith('2099')) {
      row.hasExpiryDate = true
      row.expirationDate = inv.expirationDate
    } else {
      row.hasExpiryDate = false
    }
  }
  constrainQuantity(row)
}

// const selectedProductHasExpiry = (row: DetailRow) => {
//   if (!row.productId) return false
//   const p = products.value.find(x => x.id === Number(row.productId))
//   return p?.hasExpiry ?? false
// }

function getMaxQuantity(row: DetailRow) {
  if (!row.productId) return null
  if (createForm.value.type === 'ADJUST_IN') return null

  // Xác định chi nhánh nguồn thực tế (nếu IMPORT mà không có sourceBranchId, tức là từ Kho Tổng)
  let effectiveSourceId = createForm.value.sourceBranchId
  let useGlobal = false
  if (createForm.value.type === 'IMPORT' && !effectiveSourceId) {
    effectiveSourceId = headBranch.value?.id
    useGlobal = true
  }

  if (!effectiveSourceId) return null
  if (createForm.value.type === 'IMPORT' && Number(effectiveSourceId) === Number(createForm.value.destBranchId)) return null

  if (!row.isNewBatch && !row.batchCode) return null;

  const sourceBranchIdNum = Number(effectiveSourceId)
  const invList = useGlobal ? globalInventories.value : sourceInventories.value

  let totalQty = 0;
  if (row.batchCode && !row.isNewBatch) {
    const inv = invList.find(x => x.productId === Number(row.productId) && x.batchCode === row.batchCode && Number(x.branchId) === sourceBranchIdNum)
    totalQty = inv ? inv.quantity : 0;
  } else {
    totalQty = invList
      .filter(x => x.productId === Number(row.productId) && Number(x.branchId) === sourceBranchIdNum)
      .reduce((sum, inv) => sum + inv.quantity, 0)
  }

  // Trừ đi số lượng đang nằm trong các phiếu nháp
  const pendingQty = receipts.value
    .filter(r => r.status === 'DRAFT' && Number(r.sourceBranchId) === sourceBranchIdNum &&
            (['EXPORT', 'TRANSFER', 'ADJUST_OUT', 'DISPOSAL'].includes(r.type) ||
             (r.type === 'IMPORT' && Number(r.sourceBranchId) !== Number(r.destBranchId))))
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
  const qty = Number(d.quantity)
  if (max !== null) {
    if (max === 0) {
      d.quantity = 0
      return
    }
    if (qty > max) d.quantity = max
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
  for (const d of f.details) {
    if (d.hasExpiryDate && d.manufacturingDate && d.expirationDate) {
      if (new Date(d.expirationDate) < new Date(d.manufacturingDate)) {
        toast.error('Hạn sử dụng không được nhỏ hơn Ngày sản xuất.');
        return;
      }
    }
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
  if (f.type === 'ADJUST_OUT') {
    if (!f.description?.trim()) {
      toast.error('Vui lòng nhập lý do tiêu hủy.')
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
      expirationDate: d.hasExpiryDate ? (d.expirationDate || '2099-12-31') : '2099-12-31',
    }))
  }

  submittingCreate.value = true
  try {
    const res = await api.post('/api/receipts', payload)
    if (res.ok) {
      const data = await res.json()
      if (isAdmin.value && data.id) {
         await api.post(`/api/receipts/${data.id}/approve`, {})
         if (payload.type === 'IMPORT' || payload.type === 'TRANSFER') {
            await api.post(`/api/receipts/${data.id}/approve`, {})
         }
         toast.success('Đã tạo và tự động chuyển trạng thái phiếu thành công!')
      } else {
         toast.success('Tạo phiếu kho nháp thành công!')
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
// CANCEL
// ──────────────────────────────────────────────────────────────
const showCancelModal = ref(false)
const cancelReason = ref('')
const receiptToCancel = ref<any>(null)

function confirmCancelReceipt(receipt: any) {
  receiptToCancel.value = receipt
  cancelReason.value = ''
  showCancelModal.value = true
}

async function executeCancelReceipt() {
  if (!receiptToCancel.value) return
  if (!cancelReason.value.trim()) {
    toast.error('Vui lòng nhập lý do hủy phiếu.')
    return
  }
  
  const receipt = receiptToCancel.value
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/cancel?reason=${encodeURIComponent(cancelReason.value.trim())}`, {})
    if (res.ok) {
      toast.success(`Phiếu ${receipt.code} đã được hủy.`)
      showCancelModal.value = false
      receiptToCancel.value = null
      cancelReason.value = ''
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
// APPROVE SHORTFALL
// ──────────────────────────────────────────────────────────────
const approvingShortfallId = ref<number | null>(null)

function canApproveShortfall(r: any) {
  if (r.status === 'PENDING_SHORTFALL_MANAGER') {
    if (isManager.value && !isAdmin.value && r.destBranchId === user.value?.branchId) return true;
  }
  if (r.status === 'PENDING_SHORTFALL_ADMIN') {
    if (isAdmin.value) return true;
  }
  return false;
}

async function approveShortfall(receipt: any, isApproved: boolean) {
  if (approvingShortfallId.value === receipt.id) return
  if (!confirm(`Xác nhận ${isApproved ? 'DUYỆT' : 'TỪ CHỐI'} hao hụt cho phiếu ${receipt.code}?`)) return
  approvingShortfallId.value = receipt.id
  try {
    const payload = { isApproved }
    const res = await api.post(`/api/receipts/${receipt.id}/approve-shortfall`, payload)
    if (res.ok) {
      toast.success(`Đã ${isApproved ? 'duyệt' : 'từ chối'} hao hụt phiếu ${receipt.code}.`)
      showDetail.value = false
      await loadData()
    } else {
      let errMessage = 'Lỗi khi xử lý hao hụt.'
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
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    approvingShortfallId.value = null
  }
}

// ──────────────────────────────────────────────────────────────
// COMPENSATE SHORTFALL
// ──────────────────────────────────────────────────────────────
const compensatingId = ref<number | null>(null)

function canCompensate(r: any) {
  if (r.status === 'PENDING_COMPENSATION') {
    if (isManager.value && r.sourceBranchId === user.value?.branchId) return true;
  }
  return false;
}

async function compensateShortfall(receipt: any) {
  if (compensatingId.value === receipt.id) return
  if (!confirm(`Xác nhận tạo Phiếu điều chuyển bù số lượng hao hụt cho phiếu ${receipt.code}?`)) return
  compensatingId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/compensate-shortfall`, {})
    if (res.ok) {
      toast.success(`Đã tạo thành công Phiếu điều chuyển bù.`)
      showDetail.value = false
      await loadData()
    } else {
      let errMessage = 'Lỗi khi tạo phiếu bù hao hụt.'
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
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    compensatingId.value = null
  }
}

// ──────────────────────────────────────────────────────────────
// HELPERS
// ──────────────────────────────────────────────────────────────
function formatDate(s: string) {
  if (!s || s.startsWith('1970-01-01') || s.startsWith('2099-12-31')) return '-'
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
    ADJUST_IN: 'Tăng tồn kho', ADJUST_OUT: 'Giảm tồn kho', DISPOSAL: 'Tiêu hủy'
  }
  return map[t] || t
}
function typeClass(t: string) {
  const map: Record<string, string> = {
    IMPORT: 'bg-blue-100 text-blue-800 border border-blue-300 dark:bg-blue-900/50 dark:text-blue-300 dark:border-blue-800',
    EXPORT: 'bg-purple-100 text-purple-800 border border-purple-300 dark:bg-purple-900/50 dark:text-purple-300 dark:border-purple-800',
    TRANSFER: 'bg-amber-100 text-amber-800 border border-amber-300 dark:bg-amber-900/50 dark:text-amber-300 dark:border-amber-800',
    ADJUST_IN: 'bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-900/50 dark:text-emerald-300 dark:border-emerald-800',
    ADJUST_OUT: 'bg-rose-100 text-rose-800 border border-rose-300 dark:bg-rose-900/50 dark:text-rose-300 dark:border-rose-800',
    DISPOSAL: 'bg-red-100 text-red-800 border border-red-300 dark:bg-red-900/50 dark:text-red-300 dark:border-red-800'
  }
  return map[t] || 'bg-slate-100 text-slate-800 border border-slate-300 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700'
}
function statusClass(_r?: any) {
  return 'sky-status-badge';
}

function statusLabel(r: any) {
  if (r?.type === 'EXPORT' && r?.status === 'COMPLETED' && r?.paymentStatus !== 'PAID') {
    return 'Chưa thanh toán';
  }
  const s = r?.status;
  if (s === 'DRAFT') return 'Chờ duyệt';
  if (s === 'PENDING_STAFF_CONFIRM') return 'Chờ Staff xác nhận';
  if (s === 'PENDING_ADMIN') {
    if (r?.type === 'TRANSFER') {
      if (r.sourceBranchId === user.value?.branchId) return 'Đã duyệt';
      if (r.destBranchId === user.value?.branchId) {
        if (isManager.value) return 'Chờ duyệt';
        return 'Chờ Manager';
      }
      return 'Chờ Manager';
    }
    if (r?.type === 'DISPOSAL') {
      return 'Chờ Quản lý duyệt';
    }
    return 'Chờ Admin';
  }
  if (s === 'PENDING_STOCKTAKE') {
    if (r?.type === 'TRANSFER') {
      if (r.sourceBranchId === user.value?.branchId) {
        return 'Đang chuyển (Chờ đích KK)';
      }
      return 'Chờ kiểm kê';
    }
    if (r?.type === 'DISPOSAL') return 'Chờ Admin duyệt cuối';
    return 'Chờ kiểm kê';
  }
  if (s === 'PENDING_SHORTFALL_MANAGER') return 'Thiếu hụt (Chờ Manager)';
  if (s === 'PENDING_SHORTFALL_ADMIN') {
    if (r?.type === 'TRANSFER') return 'Thiếu hụt (Chờ Manager Nguồn)';
    return 'Báo thiếu hụt';
  }
  if (s === 'PENDING_COMPENSATION') return 'Chờ điều chuyển bù';
  if (s === 'COMPLETED') {
    // Hóa đơn (EXPORT) chưa thanh toán → hiện "Chưa thanh toán" thay vì "Đã duyệt"
    if (r?.type === 'EXPORT' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán')) {
      return 'Chưa thanh toán';
    }
    return 'Đã duyệt';
  }
  if (s === 'CANCELLED') return 'Đã hủy';
  if (s === 'RETURN') return 'Trả hàng';
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
    UNPAID: 'bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-900/50 dark:text-amber-300 dark:border-amber-800',
    PAID: 'bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-900/50 dark:text-emerald-300 dark:border-emerald-800',
    IN_TRANSIT: 'bg-sky-100 text-sky-800 border-sky-300 dark:bg-sky-900/50 dark:text-sky-300 dark:border-sky-800',
    RECEIVED: 'bg-teal-100 text-teal-800 border-teal-300 dark:bg-teal-900/50 dark:text-teal-300 dark:border-teal-800'
  }
  return map[p] || 'bg-slate-100 text-slate-800 border-slate-300 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700'
}

// Can the current user confirm transfer for this receipt? (Deprecated, use confirmStocktake instead)
function canConfirmTransfer(_receipt: any) {
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

// ──────────────────────────────────────────────────────────────
// EXPORT FUNCTIONS
// ──────────────────────────────────────────────────────────────
const exportingExcel = ref(false)

async function exportExcel() {
  if (!isAdmin.value && !isManager.value) {
    toast.error('Bạn không có quyền xuất danh sách hóa đơn.')
    return
  }
  exportingExcel.value = true
  try {
    let url = '/api/invoices/export-excel'
    const params: string[] = []
    if (filterStartDate.value) params.push(`startDate=${filterStartDate.value}`)
    if (filterEndDate.value)   params.push(`endDate=${filterEndDate.value}`)
    if (params.length > 0) url += '?' + params.join('&')

    const res = await api.get(url)
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Lỗi xuất Excel' }))
      toast.error(err.message || 'Lỗi xuất Excel')
      return
    }
    const blob = await res.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    if (!filterStartDate.value && !filterEndDate.value) {
      a.download = 'Danh_Sach_Hoa_Don_Ban_Hang.xlsx'
    } else {
      const from = filterStartDate.value?.replace(/-/g, '') || 'TuDau'
      const to   = filterEndDate.value?.replace(/-/g, '')   || 'DenNay'
      a.download = `Danh_Sach_Hoa_Don_${from}_${to}.xlsx`
    }
    a.click()
    URL.revokeObjectURL(blobUrl)
    toast.success('Xuất file Excel thành công!')
  } catch (e: any) {
    toast.error('Lỗi kết nối: ' + e.message)
  } finally {
    exportingExcel.value = false
  }
}

// ──────────────────────────────────────────────────────────────
// EDIT RECEIPT — State & Functions
// ──────────────────────────────────────────────────────────────

// Kiểm tra Staff có thể tự sửa không (chỉ DRAFT, chủ phiếu)
function canStaffEdit(r: any) {
  if (!r) return false
  return user.value?.role === 'STAFF'
    && r.createdById === user.value?.id
    && r.status === 'DRAFT'
}

// Kiểm tra Manager có thể sửa không (DRAFT hoặc PENDING_ADMIN, cùng chi nhánh lập)
function canManagerEdit(r: any) {
  if (!r) return false
  if (user.value?.role !== 'MANAGER') return false
  if (r.status !== 'DRAFT' && r.status !== 'PENDING_ADMIN') return false

  // Manager chỉ được phép sửa phiếu khi chi nhánh của họ là nơi "khởi tạo" (yêu cầu) phiếu đó.
  // Không được phép sửa phiếu do chi nhánh khác gửi tới (khi chưa được Admin duyệt).
  let requestingBranchId = null;
  if (['IMPORT', 'TRANSFER'].includes(r.type)) {
      requestingBranchId = r.destBranchId;
  } else {
      requestingBranchId = r.sourceBranchId;
  }
  
  return requestingBranchId === user.value?.branchId
}

// Kiểm tra Staff có thể xác nhận thay đổi của Manager không
function canStaffAcknowledge(r: any) {
  if (!r) return false
  return user.value?.role === 'STAFF'
    && r.createdById === user.value?.id
    && r.status === 'PENDING_STAFF_CONFIRM'
}

// State cho Edit Modal
const showEditModal = ref(false)
const editMode = ref<'staff' | 'manager'>('staff')
const submittingEdit = ref(false)
const editForm = ref<{
  description: string
  editReason: string
  details: { 
    detailId: number; 
    productId: number;
    batchCode: string;
    productName: string; 
    quantity: number; 
    originalQty: number;
    maxQty: number | null;
  }[]
}>({
  description: '',
  editReason: '',
  details: []
})

// State cho Edit History panel
const showEditHistory = ref(false)
const hasSeenEditHistory = ref(false)
const editHistoryList = ref<any[]>([])


function toggleEditHistory() {
  showEditHistory.value = !showEditHistory.value
  if (showEditHistory.value) {
    hasSeenEditHistory.value = true
  }
}

// State cho Acknowledge
const submittingAcknowledge = ref(false)

function constrainEditQuantity(d: any) {
  if (d.quantity === null || d.quantity === undefined || String(d.quantity) === '') return;
  const qty = Number(d.quantity)
  if (d.maxQty !== null) {
    if (d.maxQty === 0) {
      d.quantity = 0
      return
    }
    if (qty > d.maxQty) d.quantity = d.maxQty
  }
}

function onEditQuantityBlur(d: any) {
  if (!d.quantity || d.quantity < 1) {
    d.quantity = 1;
  }
  if (d.maxQty !== null && d.maxQty === 0) {
    d.quantity = 0;
  }
}

async function openEditModal(mode: 'staff' | 'manager') {
  if (!selectedReceipt.value) return
  
  editMode.value = mode
  editForm.value = {
    description: selectedReceipt.value.description || '',
    editReason: '',
    details: (selectedReceipt.value.details || []).map((d: any) => ({
      detailId: d.id,
      productId: Number(d.productId),
      batchCode: d.batchCode,
      productName: d.productName || `SP #${d.productId}`,
      quantity: d.quantity,
      originalQty: d.quantity,
      maxQty: null as number | null
    }))
  }
  showEditModal.value = true

  // Tải mới inventory toàn cục nếu có thay đổi
  try {
    const res = await api.get('/api/inventories/global')
    if (res.ok) {
      globalInventories.value = await res.json()
    }
  } catch(e) {}

  const r = selectedReceipt.value
  let effectiveSourceId = r.sourceBranchId
  if (r.type === 'IMPORT' && !effectiveSourceId) {
    effectiveSourceId = headBranch.value?.id
  }

  editForm.value.details.forEach(d => {
    if (r.type === 'ADJUST_IN') {
      d.maxQty = null
      return
    }
    if (!effectiveSourceId) {
      d.maxQty = null
      return
    }
    if (r.type === 'IMPORT' && Number(effectiveSourceId) === Number(r.destBranchId)) {
      d.maxQty = null
      return
    }

    const sourceBranchIdNum = Number(effectiveSourceId)
    const inv = globalInventories.value.find((x: any) => 
      x.productId === d.productId && 
      x.batchCode === d.batchCode && 
      x.branchId === sourceBranchIdNum
    )
    const totalQty = inv ? inv.quantity : 0

    const pendingQty = receipts.value
      .filter(otherR => otherR.id !== r.id && otherR.status === 'DRAFT' && Number(otherR.sourceBranchId) === sourceBranchIdNum &&
              (['EXPORT', 'TRANSFER', 'ADJUST_OUT', 'DISPOSAL'].includes(otherR.type) ||
               (otherR.type === 'IMPORT' && Number(otherR.sourceBranchId) !== Number(otherR.destBranchId))))
      .flatMap(otherR => otherR.details || [])
      .filter(otherD => Number(otherD.productId) === d.productId && otherD.batchCode === d.batchCode)
      .reduce((sum, otherD) => sum + Number(otherD.quantity), 0)

    d.maxQty = Math.max(0, totalQty - pendingQty)
  })
}

async function submitEditReceipt() {
  if (!editForm.value.editReason.trim()) {
    toast.error('Vui lòng nhập lý do chỉnh sửa.')
    return
  }
  if (!selectedReceipt.value) return

  const payload = {
    description: editForm.value.description,
    editReason: editForm.value.editReason.trim(),
    details: editForm.value.details.map(d => ({ detailId: d.detailId, quantity: d.quantity }))
  }

  const endpoint = editMode.value === 'staff'
    ? `/api/receipts/${selectedReceipt.value.id}/edit-staff`
    : `/api/receipts/${selectedReceipt.value.id}/edit-manager`

  submittingEdit.value = true
  try {
    const res = await api.put(endpoint, payload)
    if (res.ok) {
      const updated = await res.json()
      // Cập nhật selectedReceipt
      selectedReceipt.value = updated
      // Cập nhật trong danh sách
      const idx = receipts.value.findIndex(r => r.id === updated.id)
      if (idx !== -1) receipts.value[idx] = updated
      showEditModal.value = false
      if (editMode.value === 'manager' && updated.status === 'PENDING_STAFF_CONFIRM') {
        toast.success('Đã gửi chỉnh sửa xuống Staff xác nhận.')
      } else {
        toast.success('Đã lưu chỉnh sửa thành công.')
      }
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Lỗi khi lưu chỉnh sửa.')
    }
  } catch (e: any) {
    toast.error('Lỗi: ' + e.message)
  } finally {
    submittingEdit.value = false
  }
}

async function staffAcknowledgeEdit() {
  if (!selectedReceipt.value) return
  submittingAcknowledge.value = true
  try {
    const res = await api.post(`/api/receipts/${selectedReceipt.value.id}/acknowledge-edit`, {})
    if (res.ok) {
      const updated = await res.json()
      selectedReceipt.value = updated
      const idx = receipts.value.findIndex(r => r.id === updated.id)
      if (idx !== -1) receipts.value[idx] = updated
      toast.success('Đã xác nhận thay đổi. Phiếu đã về trạng thái Chờ duyệt.')
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Lỗi khi xác nhận.')
    }
  } catch (e: any) {
    toast.error('Lỗi: ' + e.message)
  } finally {
    submittingAcknowledge.value = false
  }
}

// Tự động tải và lọc lịch sử khi mở detail
watch(selectedReceipt, async () => {
  editHistoryList.value = []
  showEditHistory.value = false
  hasSeenEditHistory.value = false
  if (selectedReceipt.value) {
    try {
      const res = await api.get(`/api/receipts/${selectedReceipt.value.id}/edit-history`)
      if (res.ok) {
        let logs = await res.json()
        // Admin không được xem lịch sử trao đổi nội bộ của chi nhánh con
        if (isAdmin.value) {
          logs = logs.filter((log: any) => log.direction === 'MANAGER_TO_ADMIN')
        }
        editHistoryList.value = logs
        // Tự động mở panel lịch sử nếu có (để báo hiệu cho Admin/Manager biết phiếu đã bị sửa)
        if (logs.length > 0) {
          showEditHistory.value = true
          hasSeenEditHistory.value = true
        }
      }
    } catch(e) {
      console.error('Lỗi tải lịch sử:', e)
    }
  }
})

function directionLabel(dir: string) {
  const map: Record<string, string> = {
    STAFF_EDIT: 'Nhân viên tự sửa',
    MANAGER_TO_STAFF: 'Manager gửi xuống Staff',
    MANAGER_TO_ADMIN: 'Manager ghi cho Admin'
  }
  return map[dir] || dir
}

const editModalTitle = computed(() => {
  if (editMode.value === 'staff') return 'Chỉnh sửa phiếu'
  if (selectedReceipt.value?.status === 'DRAFT') return 'Sửa & Gửi xuống Staff'
  return 'Chỉnh sửa (Ghi chú cho Admin)'
})

</script>

<style>
/* ────────────────────────────────────────────────────────────── */
/* SKY STATUS BADGE (Sun/Moon Easter Egg)                         */
/* ────────────────────────────────────────────────────────────── */
.sky-status-badge {
  background-color: #f1f5f9; /* default light sky */
  color: #475569;
  border-color: #cbd5e1;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sky-status-badge .sun-icon,
.sky-status-badge .moon-icon {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* Light Mode: Sun is UP, Moon is DOWN */
.sky-status-badge .sun-icon {
  opacity: 1;
  transform: translateY(0) rotate(45deg) scale(1);
}
.sky-status-badge .moon-icon {
  opacity: 0;
  transform: translateY(20px) rotate(0deg) scale(0.5);
}

/* Hover effect in Light Mode */
.sky-status-badge:hover .sun-icon {
  transform: translateY(0) rotate(90deg) scale(1.15);
}

/* Dark Mode: Sun is DOWN, Moon is UP */
html.dark-mode .sky-status-badge {
  background-color: #0f172a; /* bg-slate-900 */
  color: #bfdbfe; /* text-blue-200 */
  border-color: #334155; /* border-slate-700 */
  box-shadow: 0 0 12px rgba(15,23,42,0.8);
}

html.dark-mode .sky-status-badge .sun-icon {
  opacity: 0;
  transform: translateY(20px) rotate(90deg) scale(0.5);
}
html.dark-mode .sky-status-badge .moon-icon {
  opacity: 1;
  transform: translateY(0) rotate(-12deg) scale(1);
}

/* Hover effect in Dark Mode */
html.dark-mode .sky-status-badge:hover .moon-icon {
  transform: translateY(0) rotate(0deg) scale(1.15);
}
</style>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto font-sans">

    <!-- HEADER -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0 flex items-center gap-3">
          <i :class="pageConfig.icon" class="text-[#4361ee]"></i>
          {{ pageConfig.title }}
        </h2>
        <p class="text-[#8094ae] text-sm mt-1">{{ pageConfig.desc }}</p>
      </div>
      <div class="flex items-center gap-3">
        <button
          v-if="receiptType === 'EXPORT' && (isAdmin || isManager)"
          @click="exportExcel"
          :disabled="exportingExcel"
          class="h-[42px] bg-emerald-600 hover:bg-emerald-700 text-white px-5 rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center gap-2 disabled:opacity-50"
        >
          <i class="fas fa-file-excel"></i>
          <span>{{ exportingExcel ? 'Đang xuất...' : 'Xuất Excel' }}</span>
        </button>

        <button
          v-if="user?.role === 'STAFF' && !(receiptType === 'TRANSFER' && isHeadBranch)"
          @click="openCreateModal"
          class="h-[42px] bg-[#4361ee] hover:bg-[#3a0ca3] text-white px-5 rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center gap-2"
        >
          <i class="fas fa-plus"></i> {{ pageConfig.btnLabel }}
        </button>
      </div>
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

      <!-- Hóa đơn: card Chưa thanh toán -->
      <div v-if="receiptType === 'EXPORT'" @click="filterStatus = filterStatus === 'UNPAID' ? '' : 'UNPAID'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'UNPAID' ? 'border-orange-400 ring-2 ring-orange-200' : 'border-[#f1f5f9] hover:border-orange-300']">
        <div class="w-12 h-12 rounded-xl bg-orange-50 flex items-center justify-center text-orange-400 text-xl">
          <i class="fas fa-file-invoice-dollar"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Chưa thanh toán</div>
          <div class="text-2xl font-extrabold text-orange-400">{{ statUnpaid }}</div>
        </div>
      </div>

      <!-- Nhập kho / Điều chuyển: card Chờ Admin -->
      <div v-if="receiptType === 'IMPORT' || receiptType === 'TRANSFER'" @click="filterStatus = filterStatus === (receiptType === 'TRANSFER' ? 'COMPENSATION' : 'PENDING_ADMIN') ? '' : (receiptType === 'TRANSFER' ? 'COMPENSATION' : 'PENDING_ADMIN')"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === (receiptType === 'TRANSFER' ? 'COMPENSATION' : 'PENDING_ADMIN') ? 'border-blue-400 ring-2 ring-blue-200' : 'border-[#f1f5f9] hover:border-blue-300']">
        <div class="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center text-blue-500 text-xl">
          <i :class="receiptType === 'TRANSFER' ? 'fas fa-truck-loading' : 'fas fa-shield-alt'"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">{{ receiptType === 'TRANSFER' ? 'Điều chuyển bù' : 'Chờ Admin' }}</div>
          <div class="text-2xl font-extrabold text-blue-500">{{ statPendingAdmin }}</div>
        </div>
      </div>
    </div>

    <!-- TABLE CARD -->
    <div class="bg-white rounded-2xl border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-sm overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9]">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4" :class="receiptType ? 'lg:grid-cols-4' : 'lg:grid-cols-5'">
          <!-- Tìm kiếm đa năng -->
          <div class="lg:col-span-2 relative">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae] text-sm"></i>
            <input v-model="searchKeyword" type="text" placeholder="Tìm kiếm theo mã phiếu..."
              class="w-full h-11 pl-10 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all" />
          </div>
          <!-- Lọc loại phiếu (chỉ hiện khi không có receiptType prop) -->
          <div v-if="!receiptType">
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
                <option value="this_month">Tháng này</option>
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
              <button v-if="filterType || filterStatus || searchKeyword || filterDeviation || filterTimeRange !== 'this_month'"
                @click="filterType = ''; filterStatus = ''; searchKeyword = ''; filterTimeRange = 'this_month'; filterDeviation = ''"
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
      <div v-else class="w-full">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-[#f8f9fa] text-[#8094ae] text-xs uppercase tracking-wider">
              <th class="px-5 py-3 text-left font-bold">Mã phiếu</th>
              <th v-if="!receiptType" class="px-5 py-3 text-left font-bold">Loại</th>
              <th class="px-5 py-3 text-center font-bold">Trạng thái</th>
              <th class="px-5 py-3 text-left font-bold">Chênh lệch</th>
              <th class="px-5 py-3 text-left font-bold">Chi nhánh nguồn</th>
              <th class="px-5 py-3 text-left font-bold">{{ $route.path === '/invoices' ? 'Khách hàng' : 'Chi nhánh đích' }}</th>
              <th class="px-5 py-3 text-left font-bold">Người lập</th>
              <th class="px-5 py-3 text-left font-bold">Ngày tạo</th>
              <th class="px-5 py-3 text-center font-bold">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[#f1f5f9]">
            <tr v-for="r in paginatedReceipts" :key="r.id"
              @dblclick="openDetail(r)"
              :class="[
                'receipt-row hover:bg-slate-50/60 cursor-pointer transition-colors group even:bg-slate-50/20',
                r.hasDeviation && (r.status === 'PENDING_SHORTFALL_MANAGER' || r.status === 'PENDING_SHORTFALL_ADMIN') ? 'bg-rose-50/40 hover:bg-rose-100/40' : ''
              ]">
              <td class="px-5 py-4">
                <span class="font-mono font-bold text-[#4361ee] text-xs">{{ r.code }}</span>
              </td>
              <td v-if="!receiptType" class="px-5 py-4">
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', typeClass(r.type)]">
                  {{ typeLabel(r.type) }}
                </span>
              </td>
              <td class="px-5 py-4">
                <div class="flex flex-col items-center gap-1">
                  <div :class="['sky-status-badge relative overflow-hidden inline-flex items-center justify-center min-w-[130px] px-3 py-1.5 rounded-xl border text-[11px] uppercase tracking-wider font-bold shadow-sm group', statusClass(r)]">
                    <i class="fas fa-sun sun-icon absolute -right-1 -top-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <i class="fas fa-moon moon-icon absolute -right-1 -bottom-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <span class="relative z-10">{{ statusLabel(r) }}</span>
                  </div>
                  <span v-if="r.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold mt-1', paymentStatusClass(r.paymentStatus)]">
                    📦 Đã nhận hàng
                  </span>
                </div>
              </td>
              <td class="px-5 py-4">
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
                    Khớp
                  </span>
                </div>
                <div v-else class="text-xs text-slate-400">—</div>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#364a63] font-medium">{{ r.sourceBranchName || 'Bên ngoài hệ thống' }}</span>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#364a63] font-medium" v-if="r.type === 'EXPORT'">{{ getCustomerName(r) }}</span>
                <span class="text-[#364a63] font-medium" v-else>{{ r.destBranchName || '—' }}</span>
              </td>
              <td class="px-5 py-4">
                <div class="text-[#8094ae]">{{ r.createdByName }}</div>
                <div v-if="r.stocktakeByName" class="text-xs text-purple-600 mt-1 font-semibold" title="Người kiểm kê"><i class="fas fa-clipboard-check"></i> {{ r.stocktakeByName }}</div>
                <div v-else-if="r.status === 'COMPLETED' && (r.type === 'IMPORT' || r.type === 'TRANSFER') && r.createdByRole === 'STAFF'" class="text-xs text-purple-600 mt-1 font-semibold opacity-60" title="Người kiểm kê (Dữ liệu cũ)"><i class="fas fa-clipboard-check"></i> {{ r.createdByName }}</div>
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
                  <button v-if="canConfirmStocktake(r)"
                    @click.stop="openStocktakeModal(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-purple-50 hover:bg-purple-500 hover:text-white text-purple-600 transition-all"
                    title="Thực hiện kiểm kê">
                    <i class="fas fa-boxes text-xs"></i>
                  </button>
                  <button v-if="canCancelReceipt(r)"
                    @click.stop="confirmCancelReceipt(r)"
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
              <div :key="'light-detail-' + selectedReceipt?.id" class="theme-light-decor absolute inset-0 pointer-events-none transition-all duration-500">
                <i class="fas fa-sun absolute -top-12 right-8 text-yellow-300 text-[140px] opacity-10 animate-[spin_40s_linear_infinite]"></i>
                <i class="fas fa-sun absolute top-3 right-24 text-yellow-300 text-5xl drop-shadow-[0_0_20px_rgba(253,224,71,0.8)] animate-[spin_20s_linear_infinite]"></i>
                <i class="fas fa-cloud absolute top-8 right-44 text-white/50 text-5xl drop-shadow-sm"></i>
                <i class="fas fa-cloud absolute top-2 right-64 text-white/40 text-3xl"></i>
                <i class="fas fa-cloud absolute -bottom-2 right-28 text-white/30 text-7xl"></i>
              </div>

              <!-- Dark Mode Decor: Moon & Stars -->
              <div :key="'dark-detail-' + selectedReceipt?.id" class="theme-dark-decor absolute inset-0 pointer-events-none transition-all duration-500">
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
              <div class="text-xs font-bold opacity-90 uppercase tracking-wider mb-1">
                {{ selectedReceipt?.type === 'EXPORT' ? 'Chi tiết hóa đơn' : selectedReceipt?.type === 'TRANSFER' ? 'Chi tiết phiếu điều chuyển' : selectedReceipt?.type === 'ADJUST_OUT' ? 'Chi tiết phiếu tiêu hủy' : 'Chi tiết phiếu kho' }}
              </div>
              <div class="font-mono font-bold text-xl">{{ selectedReceipt?.code }}</div>
            </div>
            <button @click="showDetail = false" class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="overflow-y-auto flex-1 p-6 space-y-5 custom-scrollbar">
            <!-- ── BANNER LÝ DO HỦY ─────────────────────────────── -->
            <div v-if="selectedReceipt.description && selectedReceipt.description.includes('[Lý do hủy]')" class="bg-red-50 border border-red-200 rounded-2xl p-4 flex gap-4 shadow-sm relative overflow-hidden group">
              <div class="absolute inset-0 bg-gradient-to-r from-red-500/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
              <div class="w-10 h-10 rounded-full bg-red-100 border border-red-200 flex items-center justify-center shrink-0">
                <i class="fas fa-ban text-red-600 text-lg"></i>
              </div>
              <div class="flex-1">
                <div class="text-sm font-bold text-red-700 uppercase tracking-wide mb-1 flex items-center gap-2">
                  Lý do hủy phiếu
                </div>
                <div class="text-sm text-red-600 font-medium whitespace-pre-line">
                  {{ selectedReceipt.description.split('\n').find((l: string) => l.startsWith('[Lý do hủy]'))?.replace('[Lý do hủy]', '').trim() }}
                </div>
              </div>
            </div>

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
                <div class="flex flex-col items-start gap-1">
                  <div :class="['sky-status-badge relative overflow-hidden inline-flex items-center justify-center min-w-[130px] px-3 py-1.5 rounded-xl border text-[11px] uppercase tracking-wider font-bold shadow-sm group', statusClass(selectedReceipt)]">
                    <i class="fas fa-sun sun-icon absolute -right-1 -top-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <i class="fas fa-moon moon-icon absolute -right-1 -bottom-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <span class="relative z-10">{{ statusLabel(selectedReceipt) }}</span>
                  </div>
                  <span v-if="selectedReceipt.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', paymentStatusClass(selectedReceipt.paymentStatus)]">
                    📦 Đã nhận hàng
                  </span>
                </div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nhánh nguồn</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.sourceBranchName || 'Bên ngoài hệ thống' }}</div>
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
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Người duyệt</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.approvedByName || '—' }}</div>
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
              <div v-if="selectedReceipt.description && selectedReceipt.description.split('\n').filter((l: string) => !l.startsWith('[Lý do hủy]')).join('\n').trim()">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ghi chú</div>
                <div class="text-[#364a63] text-xs whitespace-pre-line">{{ selectedReceipt.description.split('\n').filter((l: string) => !l.startsWith('[Lý do hủy]')).join('\n').trim() }}</div>
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
                      <th class="px-4 py-2.5 text-right font-bold" v-else>SL</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">Đơn giá</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-[#f1f5f9]">
                    <tr v-for="d in selectedReceipt.details" :key="d.id" class="hover:bg-[#f8f9fa]/50">
                      <td class="px-4 py-3">
                        <div class="text-[10px] text-[#8094ae] uppercase tracking-wider mb-0.5 font-bold" v-if="d.productCategory">{{ d.productCategory }}</div>
                        <div class="font-semibold text-[#364a63]">{{ d.productName }}</div>
                        <div class="text-[11px] text-[#8094ae] mt-1" v-if="d.batchCode">
                          <i class="fas fa-box-open mr-1 opacity-70"></i> Lô: <span class="font-bold text-[#4361ee]">{{ d.batchCode }}</span>
                        </div>
                      </td>
                      <td class="px-4 py-3 text-center text-[#8094ae]">{{ formatDate(d.manufacturingDate) }}</td>
                      <td class="px-4 py-3 text-center text-[#8094ae]">{{ formatDate(d.expirationDate) }}</td>
                      <td class="px-4 py-3 text-right font-bold" v-if="d.receivedQuantity !== null">{{ d.quantity }}</td>
                      <td class="px-4 py-3 text-right font-bold text-teal-600" v-if="d.receivedQuantity !== null">
                        {{ d.receivedQuantity !== null ? d.receivedQuantity : d.quantity }}
                        <span v-if="d.receivedQuantity !== null && d.receivedQuantity < d.quantity" class="text-xs text-amber-500 block">
                          (-{{ d.quantity - d.receivedQuantity }})
                        </span>
                      </td>
                      <td class="px-4 py-3 text-right font-bold" v-else>{{ d.quantity }}</td>
                      <td class="px-4 py-3 text-right" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">{{ formatVND(d.price) }}</td>
<td class="px-4 py-3 text-right font-bold text-[#4361ee]" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">{{ formatVND(d.quantity * d.price) }}</td>
                    </tr>
                  </tbody>
                  <tfoot v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">
                    <tr class="bg-[#f8f9fa]">
                      <td :colspan="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null) ? 6 : 5" class="px-4 py-2.5 text-right font-bold text-[#8094ae] text-xs uppercase">Tổng cộng</td>
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
                    <span class="font-bold text-red-700">- {{ d.productName }} (Thiếu {{ d.quantity - d.receivedQuantity }}):</span>
                    <span class="text-red-600 ml-1 whitespace-pre-wrap break-words">{{ d.shortfallReason }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- ── Banner cảnh báo: Chờ Staff xác nhận ────────────────── -->
            <div v-if="selectedReceipt.status === 'PENDING_STAFF_CONFIRM' && canStaffAcknowledge(selectedReceipt)"
              class="mt-6 p-4 bg-amber-50 border border-amber-300 rounded-2xl flex items-start gap-3">
              <i class="fas fa-exclamation-triangle text-amber-500 mt-0.5 text-lg flex-shrink-0"></i>
              <div class="flex-1">
                <div class="font-bold text-amber-800 text-sm">Manager đã điều chỉnh phiếu này</div>
                <div class="text-amber-700 text-xs mt-0.5">Vui lòng xem lịch sử chỉnh sửa bên dưới và xác nhận để gửi lại lên Manager.</div>
              </div>
            </div>

            <!-- ── Khu vực chỉnh sửa phiếu ────────────────────────────── -->
            <div class="mt-6 pt-5 border-t flex flex-wrap items-center gap-3">
              <!-- Nút Sửa phiếu (Staff) -->
              <button v-if="canStaffEdit(selectedReceipt)"
                @click="openEditModal('staff')"
                class="h-9 px-4 bg-[#eef2ff] hover:bg-[#4361ee] hover:text-white text-[#4361ee] border border-[#4361ee]/30 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
                <i class="fas fa-pen"></i> Sửa phiếu
              </button>
              <!-- Nút Sửa + Gửi xuống (Manager khi DRAFT) -->
              <button v-if="canManagerEdit(selectedReceipt) && selectedReceipt.status === 'DRAFT'"
                @click="openEditModal('manager')"
                class="h-9 px-4 bg-orange-50 hover:bg-orange-500 hover:text-white text-orange-600 border border-orange-300 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
                <i class="fas fa-pen-to-square"></i> Sửa & Gửi Staff
              </button>
              <!-- Nút Sửa (Manager khi PENDING_ADMIN — ghi lý do cho Admin) -->
              <button v-if="canManagerEdit(selectedReceipt) && selectedReceipt.status === 'PENDING_ADMIN'"
                @click="openEditModal('manager')"
                class="h-9 px-4 bg-blue-50 hover:bg-blue-500 hover:text-white text-blue-600 border border-blue-300 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
                <i class="fas fa-pen-to-square"></i> {{ selectedReceipt?.type === 'TRANSFER' ? 'Sửa (Ghi chú cho Chi nhánh nguồn)' : 'Sửa (Ghi chú cho Admin)' }}
              </button>
              <!-- Nút Xác nhận thay đổi (Staff) -->
              <button v-if="canStaffAcknowledge(selectedReceipt)"
                @click="staffAcknowledgeEdit()" :disabled="submittingAcknowledge"
                class="h-9 px-4 bg-amber-400 hover:bg-amber-500 text-white rounded-xl text-xs font-bold transition-all flex items-center gap-2 shadow-sm">
                <i class="fas fa-check" v-if="!submittingAcknowledge"></i>
                <i class="fas fa-spinner fa-spin" v-else></i>
                Xác nhận thay đổi
              </button>
              <!-- Nút Lịch sử chỉnh sửa -->
              <button v-if="editHistoryList.length > 0" @click.prevent="toggleEditHistory" type="button"
                class="h-9 px-3 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5 ml-auto border border-slate-200 relative">
                <i class="fas fa-history text-xs"></i>
                Lịch sử sửa ({{ editHistoryList.length }})
                <i class="fas fa-chevron-down text-[10px] transition-transform" :class="showEditHistory ? 'rotate-180' : ''"></i>
                <span v-if="!showEditHistory && !hasSeenEditHistory" class="absolute -top-1 -right-1 flex h-3 w-3">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-3 w-3 bg-red-500 border-2 border-white"></span>
                </span>
              </button>
            </div>

            <!-- ── Panel lịch sử chỉnh sửa ─────────────────────────────── -->
            <Transition name="slide-down">
              <div v-if="showEditHistory" class="mt-2 rounded-2xl border border-slate-200 overflow-hidden">
                <div class="bg-slate-50 px-4 py-2.5 flex items-center gap-2 border-b border-slate-200">
                  <i class="fas fa-history text-slate-400 text-xs"></i>
                  <span class="text-xs font-bold text-slate-500 uppercase tracking-wider">Lịch sử chỉnh sửa</span>
                </div>
                <div v-if="editHistoryList.length === 0" class="px-4 py-5 text-center text-xs text-slate-400">
                  Chưa có lịch sử chỉnh sửa nào.
                </div>
                <div v-else class="divide-y divide-slate-100">
                  <div v-for="log in editHistoryList" :key="log.id" class="px-4 py-3">
                    <div class="flex items-start justify-between gap-3">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-2 flex-wrap mb-1">
                          <span class="text-xs font-bold text-slate-700">{{ log.editorName }}</span>
                          <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold"
                            :class="log.editorRole === 'STAFF' ? 'bg-blue-50 text-blue-600' : 'bg-orange-50 text-orange-600'">
                            {{ log.editorRole === 'STAFF' ? 'Nhân viên' : 'Quản lý' }}
                          </span>
                          <span class="text-[10px] text-slate-400">{{ directionLabel(log.direction) }}</span>
                        </div>
                        <div class="text-xs text-slate-600 mb-1">
                          <span class="font-semibold text-slate-500">Lý do:</span> {{ log.editReason }}
                        </div>
                        <div v-if="log.changes" class="text-xs text-slate-500">
                          <span class="font-semibold">Thay đổi:</span> {{ log.changes }}
                        </div>
                        <div v-if="log.acknowledgedAt" class="mt-1 text-[10px] text-green-600 flex items-center gap-1">
                          <i class="fas fa-check-circle"></i>
                          {{ log.acknowledgedByName }} đã xác nhận lúc {{ formatDateTime(log.acknowledgedAt) }}
                        </div>
                        <div v-else-if="log.direction === 'MANAGER_TO_STAFF'" class="mt-1 text-[10px] text-amber-500 flex items-center gap-1">
                          <i class="fas fa-clock"></i> Chờ Staff xác nhận
                        </div>
                      </div>
                      <div class="text-[10px] text-slate-400 whitespace-nowrap flex-shrink-0">
                        {{ formatDateTime(log.createdAt) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>

            <!-- Approve & Cancel actions -->
            <div v-if="canApproveReceipt(selectedReceipt)" class="mt-8 pt-5 border-t flex flex-wrap gap-4">
              <button @click="approveReceipt(selectedReceipt)" :disabled="approvingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-[#10b981] hover:bg-[#059669] text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-check-circle" v-if="approvingId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i> 
                {{ approveReceiptText(selectedReceipt) }}
              </button>
              <button @click="confirmCancelReceipt(selectedReceipt)" v-if="canCancelReceipt(selectedReceipt)"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Hủy phiếu
              </button>
            </div>
            <div v-else-if="canCancelReceipt(selectedReceipt)" class="mt-8 pt-5 border-t flex gap-4">
              <button @click="confirmCancelReceipt(selectedReceipt)"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Hủy phiếu
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

            <!-- Approve Shortfall (Hao hụt) -->
            <div v-if="canApproveShortfall(selectedReceipt)" class="mt-8 pt-5 border-t flex flex-wrap gap-4">
              <button @click="approveShortfall(selectedReceipt, true)" :disabled="approvingShortfallId === selectedReceipt.id"
                class="px-5 py-2.5 bg-orange-500 hover:bg-orange-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-check-circle" v-if="approvingShortfallId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i> 
                Duyệt Hao Hụt
              </button>
              <button v-if="selectedReceipt.status === 'PENDING_SHORTFALL_MANAGER'" @click="approveShortfall(selectedReceipt, false)" :disabled="approvingShortfallId === selectedReceipt.id"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Từ chối Hao Hụt
              </button>
            </div>

            <!-- Compensate Shortfall (Điều chuyển bù) -->
            <div v-if="canCompensate(selectedReceipt)" class="mt-8 pt-5 border-t">
              <button @click="compensateShortfall(selectedReceipt)" :disabled="compensatingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-indigo-500 hover:bg-indigo-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-truck-loading" v-if="compensatingId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i>
                Tạo Phiếu Điều Chuyển Bù
              </button>
              <p class="text-xs text-gray-500 mt-2"><i class="fas fa-info-circle"></i> Sẽ tạo một phiếu Điều chuyển mới có số lượng bằng đúng số lượng hao hụt. Phiếu cũ sẽ được đóng.</p>
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
              <div :key="'light-' + createForm.type" class="theme-light-decor absolute inset-0 pointer-events-none transition-all duration-500">
                <i class="fas fa-sun absolute -top-12 right-8 text-yellow-300 text-[140px] opacity-10 animate-[spin_40s_linear_infinite]"></i>
                <i class="fas fa-sun absolute top-3 right-24 text-yellow-300 text-5xl drop-shadow-[0_0_20px_rgba(253,224,71,0.8)] animate-[spin_20s_linear_infinite]"></i>
                <i class="fas fa-cloud absolute top-8 right-44 text-white/50 text-5xl drop-shadow-sm"></i>
                <i class="fas fa-cloud absolute top-2 right-64 text-white/40 text-3xl"></i>
                <i class="fas fa-cloud absolute -bottom-2 right-28 text-white/30 text-7xl"></i>
              </div>

              <!-- Dark Mode Decor: Moon & Stars -->
              <div :key="'dark-' + createForm.type" class="theme-dark-decor absolute inset-0 pointer-events-none transition-all duration-500">
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
              <div class="text-xs font-bold opacity-90 uppercase tracking-wider mb-1">
                {{ createForm.type === 'EXPORT' ? 'Lập hóa đơn' : createForm.type === 'TRANSFER' ? 'Lập phiếu điều chuyển' : createForm.type === 'ADJUST_OUT' ? 'Lập phiếu tiêu hủy' : 'Lập phiếu kho' }}
              </div>
              <div class="font-bold text-xl">{{ createForm.type === 'TRANSFER' ? 'Tạo phiếu xin hàng (DRAFT)' : 'Tạo phiếu nháp (DRAFT)' }}</div>
            </div>
            <button @click="showCreateModal = false" class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <!-- Body -->
          <div class="overflow-y-auto flex-1 p-6 bg-[#f8f9fa] custom-scrollbar">
            <div class="space-y-6">
              <!-- Type & Branches -->
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div v-if="!receiptType">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Loại phiếu <span class="text-red-500">*</span></label>
                  <select v-model="createForm.type" @change="onTypeChange"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="IMPORT">Nhập kho</option>
                    <option value="EXPORT">Xuất bán</option>
                    <option value="TRANSFER">Điều chuyển</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh nguồn</label>
                  <select v-model="createForm.sourceBranchId" disabled
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae] cursor-not-allowed">
                    <option v-if="isHeadBranch" value="">-- Bên ngoài hệ thống --</option>
                    <option v-if="!isHeadBranch && headBranch" :value="headBranch.id">{{ headBranch.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'TRANSFER'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh đích</label>
                  <select v-model="createForm.destBranchId" disabled
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm bg-[#f1f5f9] text-[#8094ae] cursor-not-allowed">
                    <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'TRANSFER'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nhánh nguồn <span class="text-red-500">*</span></label>
                  <select v-model="createForm.sourceBranchId"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="">-- Chọn chi nhánh --</option>
                    <option v-for="b in branches.filter(x => !x.isHead && x.id !== (user?.branchId || 0))" :key="b.id" :value="b.id">{{ b.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT' || createForm.type === 'ADJUST_IN'">
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
                <div v-if="createForm.type === 'EXPORT'" class="col-span-2 sm:col-span-1">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Trạng thái thanh toán</label>
                  <select v-model="createForm.paymentStatus"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="UNPAID">Chưa thanh toán</option>
                    <option value="PAID">Đã thanh toán</option>
                  </select>
                </div>
                <div class="col-span-2">
                  <div class="flex justify-between items-center mb-1.5">
                    <label class="block text-xs font-bold text-[#8094ae] uppercase">
                      {{ createForm.type === 'ADJUST_OUT' ? 'Lý do tiêu hủy' : 'Ghi chú' }}
                      <span v-if="createForm.type === 'ADJUST_OUT'" class="text-red-500">*</span>
                    </label>
                    <span class="text-[10px] text-[#8094ae]">{{ createForm.description?.length || 0 }}/500</span>
                  </div>
                  <textarea v-model="createForm.description" maxlength="500" :placeholder="createForm.type === 'ADJUST_OUT' ? 'Nhập lý do tiêu hủy (bắt buộc)...' : 'Ghi chú (tuỳ chọn)...'"
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
                        <div class="col-span-12 lg:col-span-4">
                          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Danh mục</label>
                          <select v-model="d.categoryId" @change="d.productId = ''; onProductChange(d)"
                            class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                            <option value="">-- Tất cả --</option>
                            <option v-for="c in getAvailableCategoriesForRow(idx)" :key="c.id" :value="c.id">{{ c.name }}</option>
                          </select>
                        </div>
                        <div class="col-span-12 lg:col-span-4">
                          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Sản phẩm <span class="text-red-500">*</span></label>
                            <select v-if="getAvailableProductsForRow(idx).length > 0" v-model="d.productId" @change="onProductChange(d)"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                              <option value="">-- Chọn sản phẩm --</option>
                              <option v-for="p in getAvailableProductsForRow(idx)" :key="p.id" :value="p.id">{{ p.name }} ({{ p.sku }})</option>
                            </select>
                            <div v-else class="w-full h-10 px-3 border border-dashed border-[#cbd5e1] bg-[#f8fafc] rounded-xl text-sm text-[#94a3b8] flex items-center justify-center italic">
                              Chưa có sản phẩm
                            </div>
                        </div>
                        
                        <div class="col-span-12 lg:col-span-4" v-if="d.productId">
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
                            
                            <button v-if="createForm.type === 'IMPORT' && createForm.sourceBranchId === createForm.destBranchId && createForm.sourceBranchId" @click="d.isNewBatch = !d.isNewBatch; d.batchCode = ''" 
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
                            <div class="flex items-center h-10 bg-white border rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-[#4361ee]/20 transition-colors"
                              :class="getMaxQuantity(d) !== null && d.quantity > getMaxQuantity(d)! ? 'border-red-400 focus-within:border-red-400' : 'border-[#e2e8f0] focus-within:border-[#4361ee]'">
                              <input v-model.number="d.quantity" type="number" min="1"
                                :max="getMaxQuantity(d) !== null ? getMaxQuantity(d)! : undefined"
                                @input="constrainQuantity(d)" @blur="onQuantityBlur(d)"
                                @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
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
                        <div v-show="!d.isCollapsed" v-if="(d.isNewBatch || d.batchCode)" class="grid grid-cols-2 gap-5 pt-4 border-t border-[#e2e8f0]">
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Ngày sản xuất</label>
                            <input v-model="d.manufacturingDate" type="date" :disabled="!d.isNewBatch"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-gray-100 disabled:text-gray-500" />
                          </div>
                          <div>
                            <label class="flex items-center gap-2 text-xs font-bold text-[#8094ae] uppercase mb-1.5" :class="d.isNewBatch ? 'cursor-pointer' : ''">
                              <input type="checkbox" v-model="d.hasExpiryDate" class="w-3.5 h-3.5 rounded border-gray-300 text-[#4361ee] focus:ring-[#4361ee]" :disabled="!d.isNewBatch" />
                              Hạn sử dụng
                            </label>
                            <input v-if="d.hasExpiryDate" v-model="d.expirationDate" type="date" :disabled="!d.isNewBatch"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-gray-100 disabled:text-gray-500" />
                            <div v-else class="w-full h-10 px-3 border border-dashed border-[#cbd5e1] bg-[#f8fafc] rounded-xl text-sm text-[#94a3b8] flex items-center italic" :class="{'opacity-50 cursor-not-allowed': !d.isNewBatch}">
                              Không quản lý HSD
                            </div>
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
                  <input v-model.number="item.actualQuantity" type="number" :min="0" :max="item.sentQty" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                    @input="item.actualQuantity = item.actualQuantity > item.sentQty ? item.sentQty : (item.actualQuantity < 0 ? 0 : item.actualQuantity)"
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
                  <input v-model.number="item.actualQuantity" type="number" :min="0" :max="item.sentQty" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                    @input="item.actualQuantity = item.actualQuantity > item.sentQty ? item.sentQty : (item.actualQuantity < 0 ? 0 : item.actualQuantity)"
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

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- EDIT RECEIPT MODAL -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <AppModal :show="showEditModal" @close="showEditModal = false" :title="editModalTitle">
      <div class="space-y-6 p-4 sm:p-5">
        <!-- Lý do chỉnh sửa -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Lý do chỉnh sửa <span class="text-red-500">*</span></label>
          <textarea v-model="editForm.editReason" rows="2"
            placeholder="Nhập lý do chỉnh sửa (bắt buộc)..."
            class="w-full px-4 py-3 border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white rounded-xl text-sm focus:ring-0 focus:border-[#4361ee] dark:focus:border-blue-500 outline-none resize-none transition-colors leading-relaxed"
          ></textarea>
        </div>

        <!-- Ghi chú phiếu -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Ghi chú phiếu</label>
          <textarea v-model="editForm.description" rows="2"
            placeholder="Ghi chú phiếu (tuỳ chọn)..."
            class="w-full px-4 py-3 border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white rounded-xl text-sm focus:ring-0 focus:border-[#4361ee] dark:focus:border-blue-500 outline-none resize-none transition-colors leading-relaxed"
          ></textarea>
        </div>

        <!-- Danh sách sản phẩm -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-2">Cập nhật số lượng</label>
          <div class="rounded-xl border-2 border-slate-200 dark:border-slate-700 overflow-hidden">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 dark:bg-slate-800/50">
                <tr>
                  <th class="px-5 py-3 text-left text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Sản phẩm</th>
                  <th class="px-5 py-3 text-center text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider w-28">Số lượng cũ</th>
                  <th class="px-5 py-3 text-center text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider w-36">Số lượng mới</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-700/50">
                <tr v-for="(d, idx) in editForm.details" :key="idx">
                  <td class="px-5 py-3.5 text-sm text-slate-700 dark:text-slate-300 font-semibold">{{ d.productName }}</td>
                  <td class="px-5 py-3.5 text-center font-medium text-slate-500 dark:text-slate-400">{{ d.originalQty }}</td>
                  <td class="px-5 py-3.5">
                    <div class="flex items-center h-10 bg-white dark:bg-slate-900 border-2 rounded-lg overflow-hidden focus-within:border-[#4361ee] dark:focus-within:border-blue-500 transition-colors"
                      :class="d.maxQty !== null && d.quantity > d.maxQty ? 'border-red-400 focus-within:border-red-400' : (d.quantity !== d.originalQty ? 'border-amber-400 focus-within:border-amber-400 dark:border-amber-500 dark:bg-amber-900/20 bg-amber-50' : 'border-slate-200 dark:border-slate-700')">
                      <input v-model.number="d.quantity" type="number" min="1"
                        :max="d.maxQty !== null ? d.maxQty : undefined"
                        @input="constrainEditQuantity(d)" @blur="onEditQuantityBlur(d)"
                        @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                        class="w-full h-full px-3 text-center text-sm font-bold text-slate-900 dark:text-white outline-none bg-transparent" />
                      <div v-if="d.maxQty !== null" 
                           class="px-2 h-full flex items-center bg-slate-50 dark:bg-slate-800 border-l-2 border-inherit text-[10px] font-bold text-slate-500 dark:text-slate-400 whitespace-nowrap">
                        / {{ d.maxQty }}
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Thông báo kự năng cho Manager -->
        <div v-if="editMode === 'manager' && selectedReceipt?.status === 'DRAFT'"
          class="p-3 bg-orange-50 border border-orange-200 rounded-xl text-xs text-orange-700 flex items-start gap-2">
          <i class="fas fa-info-circle mt-0.5 flex-shrink-0"></i>
          <span>Sau khi lưu, phiếu sḝ cđổi sang trạng thái <strong>Chờ Staff xác nhận</strong>. Staff sẽ nhận thông báo và phải xác nhận trước khi gửi lại lên bạn.</span>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2">
          <button @click="showEditModal = false"
            class="h-10 px-5 border border-[#e2e8f0] rounded-xl text-sm font-semibold text-[#8094ae] hover:bg-[#f8f9fa] transition-all">
            Hủy
          </button>
          <button @click="submitEditReceipt()" :disabled="submittingEdit"
            class="h-10 px-6 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2 disabled:opacity-60">
            <i class="fas fa-save" v-if="!submittingEdit"></i>
            <i class="fas fa-spinner fa-spin" v-else></i>
            {{ editMode === 'manager' && selectedReceipt?.status === 'DRAFT' ? 'Lưu & Gửi xuống Staff' : 'Lưu thay đổi' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- ── Modal Hủy Phiếu ─────────────────────────────── -->
    <Transition name="fade">
      <div v-if="showCancelModal" class="fixed inset-0 z-[120] flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4" @click.stop>
        <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-xl w-full max-w-md overflow-hidden border border-slate-200 dark:border-slate-700" @click.stop>
          <div class="bg-red-50 dark:bg-red-900/20 px-6 py-4 border-b border-red-100 dark:border-red-900/30 flex items-center justify-between">
            <h3 class="text-base font-bold text-red-700 dark:text-red-400 flex items-center gap-2">
              <i class="fas fa-exclamation-triangle"></i>
              Hủy phiếu {{ receiptToCancel?.code }}
            </h3>
            <button @click="showCancelModal = false" class="text-red-400 hover:text-red-600 dark:hover:text-red-300 transition-colors">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="p-6">
            <p class="text-sm text-slate-600 dark:text-slate-300 mb-4">Hành động này không thể hoàn tác. Vui lòng ghi rõ lý do hủy phiếu bên dưới để lưu vết hệ thống:</p>
            <textarea v-model="cancelReason" rows="3"
              class="w-full p-3 text-sm border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white rounded-xl focus:border-red-500 focus:ring-1 focus:ring-red-500 outline-none placeholder:text-slate-400 dark:placeholder:text-slate-500 transition-all"
              placeholder="Nhập lý do hủy phiếu..."></textarea>
          </div>
          <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-100 dark:border-slate-700 flex items-center justify-end gap-3">
            <button @click="showCancelModal = false" class="px-5 py-2.5 text-sm font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 bg-slate-100 dark:bg-slate-800/80 rounded-xl transition-colors">
              Đóng
            </button>
            <button @click="executeCancelReceipt" class="px-5 py-2.5 text-sm font-bold text-white bg-red-500 hover:bg-red-600 rounded-xl shadow-sm shadow-red-500/20 transition-all flex items-center gap-2">
              <i class="fas fa-trash"></i> Xác nhận hủy
            </button>
          </div>
        </div>
      </div>
    </Transition>

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
/* Slide-down animation for edit history panel */
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  max-height: 0;
}
.slide-down-enter-to, .slide-down-leave-from {
  opacity: 1;
  max-height: 600px;
}
</style>

<style>
/* Dark Mode Overrides for Receipt Modal Header */
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

@keyframes soft-running-light {
  0% {
    box-shadow: 
      inset 2px 0 10px rgba(56, 189, 248, 0.1), 
      0 4px 12px rgba(0, 0, 0, 0.05);
    outline: 1px solid rgba(56, 189, 248, 0.1);
  }
  33% {
    box-shadow: 
      inset 0 2px 10px rgba(56, 189, 248, 0.15), 
      0 4px 12px rgba(0, 0, 0, 0.05);
    outline: 1px solid rgba(56, 189, 248, 0.3);
  }
  66% {
    box-shadow: 
      inset -2px 0 10px rgba(56, 189, 248, 0.1), 
      0 4px 12px rgba(0, 0, 0, 0.05);
    outline: 1px solid rgba(56, 189, 248, 0.1);
  }
  100% {
    box-shadow: 
      inset 2px 0 10px rgba(56, 189, 248, 0.1), 
      0 4px 12px rgba(0, 0, 0, 0.05);
    outline: 1px solid rgba(56, 189, 248, 0.1);
  }
}

.receipt-row {
  transition: transform 0.3s ease, background-color 0.3s ease;
}

.receipt-row:hover {
  transform: translateY(-2px);
  z-index: 50;
  position: relative;
  background-color: #ffffff !important;
  animation: soft-running-light 2s linear infinite;
  border-radius: 8px;
}
html.dark-mode .receipt-row:hover {
  background-color: #1e293b !important;
}
</style>
