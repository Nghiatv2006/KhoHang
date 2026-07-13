<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'

const props = defineProps<{
  receiptType?: 'IMPORT' | 'EXPORT' | 'TRANSFER' | 'ADJUST_OUT'
}>()

const toast = useToast()

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// PAGE CONFIG ΓÇö ─Éß╗Öng theo receiptType prop
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const pageConfig = computed(() => {
  const configs: Record<string, { title: string; desc: string; icon: string; btnLabel: string }> = {
    IMPORT: {
      title: 'Quß║ún l├╜ Nhß║¡p Kho',
      desc: 'Theo d├╡i, lß║¡p v├á ph├¬ duyß╗çt c├íc phiß║┐u nhß║¡p kho',
      icon: 'fas fa-download',
      btnLabel: 'Lß║¡p phiß║┐u nhß║¡p'
    },
    EXPORT: {
      title: 'Quß║ún l├╜ H├│a ─É╞ín',
      desc: 'Theo d├╡i, lß║¡p v├á quß║ún l├╜ c├íc h├│a ─æ╞ín xuß║Ñt b├ín',
      icon: 'fas fa-file-invoice-dollar',
      btnLabel: 'Lß║¡p h├│a ─æ╞ín'
    },
    TRANSFER: {
      title: 'Quß║ún l├╜ ─Éiß╗üu Chuyß╗ân',
      desc: 'Theo d├╡i, lß║¡p v├á ph├¬ duyß╗çt c├íc phiß║┐u ─æiß╗üu chuyß╗ân kho',
      icon: 'fas fa-exchange-alt',
      btnLabel: 'Lß║¡p phiß║┐u ─æiß╗üu chuyß╗ân'
    },
    ADJUST_OUT: {
      title: 'Quß║ún l├╜ Ti├¬u Hß╗ºy',
      desc: 'Theo d├╡i, lß║¡p v├á quß║ún l├╜ c├íc phiß║┐u ti├¬u hß╗ºy h├áng h├│a',
      icon: 'fas fa-trash-alt',
      btnLabel: 'Lß║¡p phiß║┐u ti├¬u hß╗ºy'
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
          if (r.type === 'IMPORT' || r.type === 'ADJUST_IN') {
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
          if (isAdmin.value) return true;
          if (isManager.value && r.destBranchId === user.value?.branchId) return true;
          return false;
      }
  }
  return false;
}

function approveReceiptText(r: any) {
    if (r.type === 'ADJUST_OUT' && r.status === 'DRAFT') {
        return "X├íc nhß║¡n & Xß╗¡ l├╜";
    }
    if (r.type === 'ADJUST_OUT' && r.status === 'PENDING_ADMIN') {
        return "Duyß╗çt ti├¬u hß╗ºy";
    }
    if (r.type === 'IMPORT' && r.status === 'DRAFT' && isManager.value && !isAdmin.value) {
        return "Duyß╗çt (Gß╗¡i Admin)";
    }
    if (r.type === 'IMPORT' && r.status === 'PENDING_ADMIN' && isAdmin.value) {
        return "Chß║Ñp nhß║¡n nhß║¡p kho";
    }
    if (r.type === 'TRANSFER' && r.status === 'DRAFT') {
        return "Duyß╗çt (Gß╗¡i Manager chi nh├ính ─æ├¡ch)";
    }
    if (r.type === 'TRANSFER' && r.status === 'PENDING_ADMIN') {
        return "Duyß╗çt ─æiß╗üu chuyß╗ân";
    }
    return "Ph├¬ duyß╗çt";
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

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// DATA
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const receipts = ref<any[]>([])
const products = ref<any[]>([])
const branches = ref<any[]>([])
const customers = ref<any[]>([])
// const inventories = ref<any[]>([])
const categories = ref<any[]>([])
const loading = ref(true)

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// FILTER
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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

// Receipts ─æ├ú lß╗ìc theo receiptType prop (lu├┤n lß╗ìc tr╞░ß╗¢c)
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
    // Managers should NOT see operations performed by Admin at the Head Warehouse (Kho Tß╗òng),
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
  // Nß║┐u c├│ prop receiptType, kh├┤ng cß║ºn lß╗ìc th├¬m theo filterType
  if (!props.receiptType && filterType.value) result = result.filter(r => r.type === filterType.value)
  if (filterStatus.value) {
    if (filterStatus.value === 'UNPAID') {
      result = result.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Ch╞░a thanh to├ín'))
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

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// PAGINATION
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// LOAD DATA
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
    if (catRes.ok) categories.value = await catRes.json()
  } catch (e: any) {
    toast.error('Lß╗ùi tß║úi dß╗» liß╗çu: ' + e.message)
  } finally {
    loading.value = false
  }
}
onMounted(loadData)

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// HEAD BRANCH
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const headBranch = computed(() => branches.value.find(b => b.isHead) || branches.value[0] || null)
// const subBranches = computed(() => branches.value.filter(b => b.id !== headBranch.value?.id))

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// STATS
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const statDraft = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'DRAFT').length)
const statCompleted = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'COMPLETED').length)
const statCancelled = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'CANCELLED').length)

const statUnpaid = computed(() => typeFilteredReceipts.value.filter(r => r.type === 'EXPORT' && r.status === 'COMPLETED' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Ch╞░a thanh to├ín')).length)

// Nhß║¡p kho: Chß╗¥ Admin duyß╗çt
const statPendingAdmin = computed(() => typeFilteredReceipts.value.filter(r => r.status === 'PENDING_ADMIN').length)

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// DETAIL PANEL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
    toast.error('Lß╗ùi tß║úi chi tiß║┐t: ' + e.message)
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// DIRECT IMPORT MODAL (TH├èM Sß║óN PHß║¿M)
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
  if (!form.categoryId) { toast.error('Vui l├▓ng chß╗ìn danh mß╗Ñc.'); return }
  if (!form.productId) { toast.error('Vui l├▓ng chß╗ìn sß║ún phß║⌐m.'); return }
  if (!form.batchCode.trim()) { toast.error('Vui l├▓ng nhß║¡p m├ú l├┤ sß║ún xuß║Ñt.'); return }
  if (form.quantity <= 0) { toast.error('Sß╗æ l╞░ß╗úng nhß║¡p phß║úi lß╗¢n h╞ín 0.'); return }
  if (!form.manufacturingDate) { toast.error('Vui l├▓ng nhß║¡p Ng├áy sß║ún xuß║Ñt (NSX).'); return }

  const today = new Date(); today.setHours(0, 0, 0, 0);
  const mfgDate = new Date(form.manufacturingDate); mfgDate.setHours(0, 0, 0, 0);
  if (mfgDate > today) { toast.error('Ng├áy sß║ún xuß║Ñt (NSX) kh├┤ng ─æ╞░ß╗úc lß╗¢n h╞ín ng├áy hiß╗çn tß║íi.'); return }

  if (form.hasExpiry) {
    if (!form.expirationDate) { toast.error('Vui l├▓ng nhß║¡p Hß║ín sß╗¡ dß╗Ñng (HSD).'); return }
    if (new Date(form.expirationDate) < new Date(form.manufacturingDate)) { toast.error('Hß║ín sß╗¡ dß╗Ñng kh├┤ng ─æ╞░ß╗úc nhß╗Å h╞ín Ng├áy sß║ún xuß║Ñt.'); return }
    if (!form.expiryWarningDays || Number(form.expiryWarningDays) <= 0) { toast.error('Sß╗æ ng├áy cß║únh b├ío hß║ín d├╣ng phß║úi lß╗¢n h╞ín 0.'); return }
  }

  const payload: any = {
    type: 'IMPORT',
    sourceBranchId: null,
    destBranchId: headBranch.value?.id || 1,
    description: 'Th├¬m sß║ún phß║⌐m trß╗▒c tiß║┐p v├áo Kho Tß╗òng',
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
         toast.success('─É├ú tß║ío v├á tß╗▒ ─æß╗Öng chuyß╗ân sang Chß╗¥ kiß╗âm k├¬ th├ánh c├┤ng!')
      } else {
         toast.success('─É├ú tß║ío Phiß║┐u nhß║¡p h├áng mß╗¢i! H├úy duyß╗çt phiß║┐u ─æß╗â cß╗Öng tß╗ôn kho.')
      }
      showDirectImportModal.value = false
      await loadData()
    } else {
      const errData = await res.json()
      toast.error(errData.message || 'Lß╗ùi khi tß║ío phiß║┐u.')
    }
  } catch (err: any) {
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + err.message)
  } finally {
    submittingDirectImport.value = false
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// CREATE DRAFT MODAL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
  // Kh├┤ng tß╗▒ ─æß╗Öng x├│a phone ─æß╗â ng╞░ß╗¥i d├╣ng c├│ thß╗â g├╡ tiß║┐p
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

// Chi nh├ính hiß╗çn tß║íi c├│ phß║úi chi nh├ính gß╗æc (H├á Nß╗Öi) kh├┤ng?
const isHeadBranch = computed(() => user.value?.branchId === headBranch.value?.id)

function onTypeChange() {
  const t = createForm.value.type
  if (t === 'IMPORT') {
    // Chi nh├ính gß╗æc (H├á Nß╗Öi): nhß║¡p tß╗½ b├¬n ngo├ái hß╗ç thß╗æng (sourceBranchId = '')
    // Chi nh├ính con (HCM, ...): nhß║¡p tß╗½ chi nh├ính H├á Nß╗Öi (sourceBranchId = headBranch.id)
    createForm.value.sourceBranchId = isHeadBranch.value ? '' : (headBranch.value?.id || '')
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
        let errStr = 'Lß╗ùi server';
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
        toast.error('Kh├┤ng thß╗â tß║úi tß╗ôn kho chi nh├ính nguß╗ôn: ' + errStr)
        sourceInventories.value = []
      }
    } catch (e: any) {
      toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
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
    
    // R├áng buß╗Öc cho phiß║┐u ti├¬u hß╗ºy: Chß╗ë h├áng sß║»p hß║┐t hß║ín (<= 14 ng├áy) hoß║╖c ─æ├ú hß║┐t hß║ín
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
    
    // R├áng buß╗Öc phiß║┐u ti├¬u hß╗ºy: Chß╗ë chß╗ìn h├áng Sß╗»a hoß║╖c Hß╗»u c╞í
    if (t === 'ADJUST_OUT') {
      result = result.filter(p => {
        const catName = categories.value.find(c => c.id === p.categoryId)?.name?.toLowerCase() || ''
        return catName.includes('sß╗»a') || catName.includes('hß╗»u c╞í')
      })
    }
    
    return result
  }
  if (t === 'IMPORT') {
    if (createForm.value.destBranchId === headBranch.value?.id) {
      return products.value; // Kho tß╗òng nhß║¡p tß╗½ nh├á cung cß║Ñp -> hiß╗ân thß╗ï tß║Ñt cß║ú
    }
    // Chi nh├ính con nhß║¡p tß╗½ kho tß╗òng -> chß╗ë hiß╗ân thß╗ï c├íc sß║ún phß║⌐m c├│ tß╗ôn kho ß╗ƒ kho tß╗òng
    const headBranchId = headBranch.value?.id
    const inStockIds = new Set(
      globalInventories.value
        .filter(inv => inv.branchId === headBranchId && inv.quantity > 0)
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
  // IMPORT without sourceBranchId means source is headBranch (Kho Tß╗òng)
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
      if (createForm.value.type === 'DISPOSAL' && createForm.value.disposalReason === 'H├áng hß║┐t hß║ín sß╗¡ dß╗Ñng') {
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

  // X├íc ─æß╗ïnh chi nh├ính nguß╗ôn thß╗▒c tß║┐ (nß║┐u IMPORT m├á kh├┤ng c├│ sourceBranchId, tß╗⌐c l├á tß╗½ Kho Tß╗òng)
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

  // Trß╗½ ─æi sß╗æ l╞░ß╗úng ─æang nß║▒m trong c├íc phiß║┐u nh├íp
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
  if (!createForm.value.sourceBranchId && createForm.value.type !== 'IMPORT') { toast.error('Vui l├▓ng chß╗ìn loß║íi phiß║┐u.'); return }
  const f = createForm.value
  if (f.details.some(d => !d.productId || d.quantity <= 0 || !d.batchCode?.trim())) {
    toast.error('Vui l├▓ng ─æiß╗ün ─æß║ºy ─æß╗º sß║ún phß║⌐m, l├┤ sß║ún xuß║Ñt v├á sß╗æ l╞░ß╗úng hß╗úp lß╗ç.')
    return
  }
  if (f.type === 'EXPORT') {
    if (!f.customerName?.trim()) {
      toast.error('Vui l├▓ng nhß║¡p t├¬n kh├ích h├áng khi xuß║Ñt b├ín.')
      return
    }
    if (!f.customerPhone?.trim()) {
      toast.error('Vui l├▓ng nhß║¡p sß╗æ ─æiß╗çn thoß║íi kh├ích h├áng khi xuß║Ñt b├ín.')
      return
    }
  }
  if (f.type === 'ADJUST_OUT') {
    if (!f.description?.trim()) {
      toast.error('Vui l├▓ng nhß║¡p l├╜ do ti├¬u hß╗ºy.')
      return
    }
  }

  const isConstrained = f.type !== 'ADJUST_IN' && !(f.type === 'IMPORT' && f.sourceBranchId === f.destBranchId)
  if (isConstrained && f.details.some(d => {
    const max = getMaxQuantity(d)
    return max !== null && d.quantity > max
  })) {
    toast.error('Sß╗æ l╞░ß╗úng v╞░ß╗út qu├í tß╗ôn kho hiß╗çn tß║íi.')
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
      if (isAdmin.value && data.id) {
         await api.post(`/api/receipts/${data.id}/approve`, {})
         if (payload.type === 'IMPORT' || payload.type === 'TRANSFER') {
            await api.post(`/api/receipts/${data.id}/approve`, {})
         }
         toast.success('─É├ú tß║ío v├á tß╗▒ ─æß╗Öng chuyß╗ân trß║íng th├íi phiß║┐u th├ánh c├┤ng!')
      } else {
         toast.success('Tß║ío phiß║┐u kho nh├íp th├ánh c├┤ng!')
      }
      showCreateModal.value = false
      await loadData()
    } else {
      let errMsg = `Lß╗ùi ${res.status}: ${res.statusText}`
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
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    submittingCreate.value = false
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// APPROVE
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const approvingId = ref<number | null>(null)

async function approveReceipt(receipt: any) {
  if (approvingId.value === receipt.id) return
  if (!confirm(`X├íc nhß║¡n ph├¬ duyß╗çt phiß║┐u ${receipt.code}?`)) return
  approvingId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/approve`, {})
    if (res.ok) {
      toast.success(`Phiß║┐u ${receipt.code} ─æ├ú ─æ╞░ß╗úc ph├¬ duyß╗çt th├ánh c├┤ng!`)
      showDetail.value = false
      await loadData()
    } else {
      let errMessage = 'Lß╗ùi khi duyß╗çt phiß║┐u.'
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
    toast.error('Lß╗ùi: ' + e.message)
  } finally {
    approvingId.value = null
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// CANCEL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
    toast.error('Vui l├▓ng nhß║¡p l├╜ do hß╗ºy phiß║┐u.')
    return
  }
  
  const receipt = receiptToCancel.value
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/cancel?reason=${encodeURIComponent(cancelReason.value.trim())}`, {})
    if (res.ok) {
      toast.success(`Phiß║┐u ${receipt.code} ─æ├ú ─æ╞░ß╗úc hß╗ºy.`)
      showCancelModal.value = false
      receiptToCancel.value = null
      cancelReason.value = ''
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lß╗ùi khi hß╗ºy phiß║┐u.')
    }
  } catch (e: any) {
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// MARK AS PAID
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const markingPaidId = ref<number | null>(null)

async function markAsPaid(receipt: any) {
  if (markingPaidId.value === receipt.id) return
  if (!confirm(`X├íc nhß║¡n THANH TO├üN cho phiß║┐u ${receipt.code}? C├┤ng nß╗ú kh├ích h├áng sß║╜ ─æ╞░ß╗úc trß╗½ t╞░╞íng ß╗⌐ng.`)) return
  markingPaidId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/mark-paid`, {})
    if (res.ok) {
      toast.success(`Phiß║┐u ${receipt.code} ─æ├ú ─æ╞░ß╗úc thanh to├ín th├ánh c├┤ng!`)
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lß╗ùi khi x├íc nhß║¡n thanh to├ín.')
    }
  } catch (e: any) {
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    markingPaidId.value = null
  }
}


// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// CONFIRM TRANSFER MODAL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
    toast.error('Sß╗æ l╞░ß╗úng nhß║¡n kh├┤ng ─æ╞░ß╗úc ├óm.'); return
  }
  if (confirmItems.value.some(i => i.actualQuantity < i.sentQty && (!i.shortfallReason || i.shortfallReason.trim() === ''))) {
    toast.error('Vui l├▓ng nhß║¡p ─æß║ºy ─æß╗º l├╜ do cho c├íc sß║ún phß║⌐m bß╗ï hao hß╗Ñt.'); return
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
      toast.success('X├íc nhß║¡n nhß║¡n h├áng th├ánh c├┤ng! Tß╗ôn kho ─æ├ú ─æ╞░ß╗úc cß║¡p nhß║¡t.')
      showConfirmModal.value = false
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lß╗ùi khi x├íc nhß║¡n nhß║¡n h├áng.')
    }
  } catch (e: any) {
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    submittingConfirm.value = false
  }
}



// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// CONFIRM STOCKTAKE MODAL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
    toast.error('Sß╗æ l╞░ß╗úng thß╗▒c tß║┐ kh├┤ng ─æ╞░ß╗úc ├óm.'); return
  }
  if (stocktakeItems.value.some(i => i.actualQuantity < i.sentQty && (!i.shortfallReason || i.shortfallReason.trim() === ''))) {
    toast.error('Vui l├▓ng nhß║¡p ─æß║ºy ─æß╗º l├╜ do cho c├íc sß║ún phß║⌐m bß╗ï hao hß╗Ñt.'); return
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
      toast.success('X├íc nhß║¡n kiß╗âm k├¬ th├ánh c├┤ng! H├áng ─æ├ú ─æ╞░ß╗úc cß╗Öng v├áo kho.')
      showStocktakeModal.value = false
      showDetail.value = false
      await loadData()
    } else {
      const err = await res.json()
      toast.error(err.message || 'Lß╗ùi khi x├íc nhß║¡n kiß╗âm k├¬.')
    }
  } catch (e: any) {
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    submittingStocktake.value = false
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// APPROVE SHORTFALL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
  if (!confirm(`X├íc nhß║¡n ${isApproved ? 'DUYß╗åT' : 'Tß╗¬ CHß╗ÉI'} hao hß╗Ñt cho phiß║┐u ${receipt.code}?`)) return
  approvingShortfallId.value = receipt.id
  try {
    const payload = { isApproved }
    const res = await api.post(`/api/receipts/${receipt.id}/approve-shortfall`, payload)
    if (res.ok) {
      toast.success(`─É├ú ${isApproved ? 'duyß╗çt' : 'tß╗½ chß╗æi'} hao hß╗Ñt phiß║┐u ${receipt.code}.`)
      showDetail.value = false
      await loadData()
    } else {
      let errMessage = 'Lß╗ùi khi xß╗¡ l├╜ hao hß╗Ñt.'
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
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    approvingShortfallId.value = null
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// COMPENSATE SHORTFALL
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const compensatingId = ref<number | null>(null)

function canCompensate(r: any) {
  if (r.status === 'PENDING_COMPENSATION') {
    if (isManager.value && r.sourceBranchId === user.value?.branchId) return true;
  }
  return false;
}

async function compensateShortfall(receipt: any) {
  if (compensatingId.value === receipt.id) return
  if (!confirm(`X├íc nhß║¡n tß║ío Phiß║┐u ─æiß╗üu chuyß╗ân b├╣ sß╗æ l╞░ß╗úng hao hß╗Ñt cho phiß║┐u ${receipt.code}?`)) return
  compensatingId.value = receipt.id
  try {
    const res = await api.post(`/api/receipts/${receipt.id}/compensate-shortfall`, {})
    if (res.ok) {
      toast.success(`─É├ú tß║ío th├ánh c├┤ng Phiß║┐u ─æiß╗üu chuyß╗ân b├╣.`)
      showDetail.value = false
      await loadData()
    } else {
      let errMessage = 'Lß╗ùi khi tß║ío phiß║┐u b├╣ hao hß╗Ñt.'
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
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    compensatingId.value = null
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// HELPERS
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
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
  return new Intl.NumberFormat('vi-VN').format(v) + '─æ'
}

function typeLabel(t: string) {
  const map: Record<string, string> = {
    IMPORT: 'Nhß║¡p kho', EXPORT: 'Xuß║Ñt b├ín', TRANSFER: '─Éiß╗üu chuyß╗ân',
    ADJUST_IN: 'T─âng tß╗ôn kho', ADJUST_OUT: 'Giß║úm tß╗ôn kho', DISPOSAL: 'Ti├¬u hß╗ºy'
  }
  return map[t] || t
}
function typeClass(t: string) {
  const map: Record<string, string> = {
    IMPORT: 'bg-blue-50 text-blue-700 border border-blue-200',
    EXPORT: 'bg-purple-50 text-purple-700 border border-purple-200',
    TRANSFER: 'bg-amber-50 text-amber-700 border border-amber-200',
    ADJUST_IN: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
    ADJUST_OUT: 'bg-rose-50 text-rose-700 border border-rose-200',
    DISPOSAL: 'bg-red-50 text-red-700 border border-red-200'
  }
  return map[t] || 'bg-slate-50 text-slate-600 border border-slate-200'
}
function statusClass(_r?: any) {
  return 'sky-status-badge';
}

function statusLabel(r: any) {
  if (r?.type === 'EXPORT' && r?.status === 'COMPLETED' && r?.paymentStatus !== 'PAID') {
    return 'Ch╞░a thanh to├ín';
  }
  const s = r?.status;
  if (s === 'DRAFT') return 'Chß╗¥ duyß╗çt';
  if (s === 'PENDING_STAFF_CONFIRM') return 'Chß╗¥ Staff x├íc nhß║¡n';
  if (s === 'PENDING_ADMIN') {
    if (r?.type === 'TRANSFER') {
      if (r.sourceBranchId === user.value?.branchId) return '─É├ú duyß╗çt';
      if (r.destBranchId === user.value?.branchId) {
        if (isManager.value) return 'Chß╗¥ duyß╗çt';
        return 'Chß╗¥ Manager';
      }
      return 'Chß╗¥ Manager';
    }
    if (r?.type === 'DISPOSAL') {
      return 'Chß╗¥ Quß║ún l├╜ duyß╗çt';
    }
    return 'Chß╗¥ Admin';
  }
  if (s === 'PENDING_STOCKTAKE') {
    if (r?.type === 'TRANSFER') {
      if (r.sourceBranchId === user.value?.branchId) {
        return '─Éang chuyß╗ân (Chß╗¥ ─æ├¡ch KK)';
      }
      return 'Chß╗¥ kiß╗âm k├¬';
    }
    if (r?.type === 'DISPOSAL') return 'Chß╗¥ Admin duyß╗çt cuß╗æi';
    return 'Chß╗¥ kiß╗âm k├¬';
  }
  if (s === 'PENDING_SHORTFALL_MANAGER') return 'Thiß║┐u hß╗Ñt (Chß╗¥ Manager)';
  if (s === 'PENDING_SHORTFALL_ADMIN') {
    if (r?.type === 'TRANSFER') return 'Thiß║┐u hß╗Ñt (Chß╗¥ Manager Nguß╗ôn)';
    return 'B├ío thiß║┐u hß╗Ñt';
  }
  if (s === 'PENDING_COMPENSATION') return 'Chß╗¥ ─æiß╗üu chuyß╗ân b├╣';
  if (s === 'COMPLETED') {
    // H├│a ─æ╞ín (EXPORT) ch╞░a thanh to├ín ΓåÆ hiß╗çn "Ch╞░a thanh to├ín" thay v├¼ "─É├ú duyß╗çt"
    if (r?.type === 'EXPORT' && (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Ch╞░a thanh to├ín')) {
      return 'Ch╞░a thanh to├ín';
    }
    return '─É├ú duyß╗çt';
  }
  if (s === 'CANCELLED') return '─É├ú hß╗ºy';
  if (s === 'RETURN') return 'Trß║ú h├áng';
  return s;
}
function paymentStatusLabel(p: string) {
  const map: Record<string, string> = {
    UNPAID: 'Ch╞░a thanh to├ín', PAID: '─É├ú thanh to├ín',
    IN_TRANSIT: '─Éang vß║¡n chuyß╗ân', RECEIVED: '─É├ú nhß║¡n h├áng'
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
  if (!receipt.customerId) return 'ΓÇö'
  const c = customers.value.find(x => x.id === receipt.customerId)
  return c ? `${c.name} - ${c.contactInfo || 'Kh├┤ng c├│ S─ÉT'}` : 'ΓÇö'
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// EXPORT FUNCTIONS
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
const exportingExcel = ref(false)

async function exportExcel() {
  if (!isAdmin.value && !isManager.value) {
    toast.error('Bß║ín kh├┤ng c├│ quyß╗ün xuß║Ñt danh s├ích h├│a ─æ╞ín.')
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
      const err = await res.json().catch(() => ({ message: 'Lß╗ùi xuß║Ñt Excel' }))
      toast.error(err.message || 'Lß╗ùi xuß║Ñt Excel')
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
    toast.success('Xuß║Ñt file Excel th├ánh c├┤ng!')
  } catch (e: any) {
    toast.error('Lß╗ùi kß║┐t nß╗æi: ' + e.message)
  } finally {
    exportingExcel.value = false
  }
}

// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ
// EDIT RECEIPT ΓÇö State & Functions
// ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

// Kiß╗âm tra Staff c├│ thß╗â tß╗▒ sß╗¡a kh├┤ng (chß╗ë DRAFT, chß╗º phiß║┐u)
function canStaffEdit(r: any) {
  if (!r) return false
  return user.value?.role === 'STAFF'
    && r.createdById === user.value?.id
    && r.status === 'DRAFT'
}

// Kiß╗âm tra Manager c├│ thß╗â sß╗¡a kh├┤ng (DRAFT hoß║╖c PENDING_ADMIN, c├╣ng chi nh├ính lß║¡p)
function canManagerEdit(r: any) {
  if (!r) return false
  if (user.value?.role !== 'MANAGER') return false
  if (r.status !== 'DRAFT' && r.status !== 'PENDING_ADMIN') return false

  // Manager chß╗ë ─æ╞░ß╗úc ph├⌐p sß╗¡a phiß║┐u khi chi nh├ính cß╗ºa hß╗ì l├á n╞íi "khß╗ƒi tß║ío" (y├¬u cß║ºu) phiß║┐u ─æ├│.
  // Kh├┤ng ─æ╞░ß╗úc ph├⌐p sß╗¡a phiß║┐u do chi nh├ính kh├íc gß╗¡i tß╗¢i (khi ch╞░a ─æ╞░ß╗úc Admin duyß╗çt).
  let requestingBranchId = null;
  if (['IMPORT', 'TRANSFER'].includes(r.type)) {
      requestingBranchId = r.destBranchId;
  } else {
      requestingBranchId = r.sourceBranchId;
  }
  
  return requestingBranchId === user.value?.branchId
}

// Kiß╗âm tra Staff c├│ thß╗â x├íc nhß║¡n thay ─æß╗òi cß╗ºa Manager kh├┤ng
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

  // Tß║úi mß╗¢i inventory to├án cß╗Ñc nß║┐u c├│ thay ─æß╗òi
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
    toast.error('Vui l├▓ng nhß║¡p l├╜ do chß╗ënh sß╗¡a.')
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
      // Cß║¡p nhß║¡t selectedReceipt
      selectedReceipt.value = updated
      // Cß║¡p nhß║¡t trong danh s├ích
      const idx = receipts.value.findIndex(r => r.id === updated.id)
      if (idx !== -1) receipts.value[idx] = updated
      showEditModal.value = false
      if (editMode.value === 'manager' && updated.status === 'PENDING_STAFF_CONFIRM') {
        toast.success('─É├ú gß╗¡i chß╗ënh sß╗¡a xuß╗æng Staff x├íc nhß║¡n.')
      } else {
        toast.success('─É├ú l╞░u chß╗ënh sß╗¡a th├ánh c├┤ng.')
      }
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Lß╗ùi khi l╞░u chß╗ënh sß╗¡a.')
    }
  } catch (e: any) {
    toast.error('Lß╗ùi: ' + e.message)
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
      toast.success('─É├ú x├íc nhß║¡n thay ─æß╗òi. Phiß║┐u ─æ├ú vß╗ü trß║íng th├íi Chß╗¥ duyß╗çt.')
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Lß╗ùi khi x├íc nhß║¡n.')
    }
  } catch (e: any) {
    toast.error('Lß╗ùi: ' + e.message)
  } finally {
    submittingAcknowledge.value = false
  }
}

// Tß╗▒ ─æß╗Öng tß║úi v├á lß╗ìc lß╗ïch sß╗¡ khi mß╗ƒ detail
watch(selectedReceipt, async () => {
  editHistoryList.value = []
  showEditHistory.value = false
  hasSeenEditHistory.value = false
  if (selectedReceipt.value) {
    try {
      const res = await api.get(`/api/receipts/${selectedReceipt.value.id}/edit-history`)
      if (res.ok) {
        let logs = await res.json()
        // Admin kh├┤ng ─æ╞░ß╗úc xem lß╗ïch sß╗¡ trao ─æß╗òi nß╗Öi bß╗Ö cß╗ºa chi nh├ính con
        if (isAdmin.value) {
          logs = logs.filter((log: any) => log.direction === 'MANAGER_TO_ADMIN')
        }
        editHistoryList.value = logs
        // Tß╗▒ ─æß╗Öng mß╗ƒ panel lß╗ïch sß╗¡ nß║┐u c├│ (─æß╗â b├ío hiß╗çu cho Admin/Manager biß║┐t phiß║┐u ─æ├ú bß╗ï sß╗¡a)
        if (logs.length > 0) {
          showEditHistory.value = true
          hasSeenEditHistory.value = true
        }
      }
    } catch(e) {
      console.error('Lß╗ùi tß║úi lß╗ïch sß╗¡:', e)
    }
  }
})

function directionLabel(dir: string) {
  const map: Record<string, string> = {
    STAFF_EDIT: 'Nh├ón vi├¬n tß╗▒ sß╗¡a',
    MANAGER_TO_STAFF: 'Manager gß╗¡i xuß╗æng Staff',
    MANAGER_TO_ADMIN: 'Manager ghi cho Admin'
  }
  return map[dir] || dir
}

const editModalTitle = computed(() => {
  if (editMode.value === 'staff') return 'Chß╗ënh sß╗¡a phiß║┐u'
  if (selectedReceipt.value?.status === 'DRAFT') return 'Sß╗¡a & Gß╗¡i xuß╗æng Staff'
  return 'Chß╗ënh sß╗¡a (Ghi ch├║ cho Admin)'
})

</script>

<style scoped>
/* SKY THEME STATUS BADGE (─Éß║úm bß║úo hoß║ít ─æß╗Öng ─æß╗Öc lß║¡p kh├┤ng phß╗Ñ thuß╗Öc Tailwind config) */
.sky-status-badge {
  background-color: #3b82f6; /* bg-blue-500 */
  color: white;
  border-color: #60a5fa; /* border-blue-400 */
  box-shadow: 0 0 12px rgba(59,130,246,0.4);
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 130px;
  justify-content: center;
}

.sky-status-badge .sun-icon,
.sky-status-badge .moon-icon {
  display: block;
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1); /* Bouncy effect */
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
          <span>{{ exportingExcel ? '─Éang xuß║Ñt...' : 'Xuß║Ñt Excel' }}</span>
        </button>

        <button
          v-if="user?.role === 'STAFF'"
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
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Chß╗¥ duyß╗çt</div>
          <div class="text-2xl font-extrabold text-yellow-500">{{ statDraft }}</div>
        </div>
      </div>
      <div @click="filterStatus = filterStatus === 'COMPLETED' ? '' : 'COMPLETED'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'COMPLETED' ? 'border-green-400 ring-2 ring-green-200' : 'border-[#f1f5f9] hover:border-green-300']">
        <div class="w-12 h-12 rounded-xl bg-green-50 flex items-center justify-center text-green-500 text-xl">
          <i class="fas fa-check-circle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">─É├ú duyß╗çt</div>
          <div class="text-2xl font-extrabold text-green-500">{{ statCompleted }}</div>
        </div>
      </div>

      <div @click="filterStatus = filterStatus === 'CANCELLED' ? '' : 'CANCELLED'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'CANCELLED' ? 'border-red-400 ring-2 ring-red-200' : 'border-[#f1f5f9] hover:border-red-300']">
        <div class="w-12 h-12 rounded-xl bg-red-50 flex items-center justify-center text-red-400 text-xl">
          <i class="fas fa-times-circle"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">─É├ú hß╗ºy</div>
          <div class="text-2xl font-extrabold text-red-400">{{ statCancelled }}</div>
        </div>
      </div>

      <!-- H├│a ─æ╞ín: card Ch╞░a thanh to├ín -->
      <div v-if="receiptType === 'EXPORT'" @click="filterStatus = filterStatus === 'UNPAID' ? '' : 'UNPAID'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'UNPAID' ? 'border-orange-400 ring-2 ring-orange-200' : 'border-[#f1f5f9] hover:border-orange-300']">
        <div class="w-12 h-12 rounded-xl bg-orange-50 flex items-center justify-center text-orange-400 text-xl">
          <i class="fas fa-file-invoice-dollar"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Ch╞░a thanh to├ín</div>
          <div class="text-2xl font-extrabold text-orange-400">{{ statUnpaid }}</div>
        </div>
      </div>

      <!-- Nhß║¡p kho / ─Éiß╗üu chuyß╗ân: card Chß╗¥ Admin -->
      <div v-if="receiptType === 'IMPORT' || receiptType === 'TRANSFER'" @click="filterStatus = filterStatus === 'PENDING_ADMIN' ? '' : 'PENDING_ADMIN'"
        :class="['bg-white rounded-2xl p-5 border transition-all cursor-pointer flex items-center gap-4', filterStatus === 'PENDING_ADMIN' ? 'border-blue-400 ring-2 ring-blue-200' : 'border-[#f1f5f9] hover:border-blue-300']">
        <div class="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center text-blue-500 text-xl">
          <i class="fas fa-shield-alt"></i>
        </div>
        <div>
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wide">Chß╗¥ Admin</div>
          <div class="text-2xl font-extrabold text-blue-500">{{ statPendingAdmin }}</div>
        </div>
      </div>
    </div>

    <!-- TABLE CARD -->
    <div class="bg-white rounded-2xl border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-sm overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9]">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4" :class="receiptType ? 'lg:grid-cols-4' : 'lg:grid-cols-5'">
          <!-- T├¼m kiß║┐m ─æa n─âng -->
          <div class="lg:col-span-2 relative">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae] text-sm"></i>
            <input v-model="searchKeyword" type="text" placeholder="T├¼m kiß║┐m theo m├ú phiß║┐u..."
              class="w-full h-11 pl-10 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all" />
          </div>
          <!-- Lß╗ìc loß║íi phiß║┐u (chß╗ë hiß╗çn khi kh├┤ng c├│ receiptType prop) -->
          <div v-if="!receiptType">
            <select v-model="filterType"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="">-- Tß║Ñt cß║ú loß║íi phiß║┐u --</option>
              <option value="IMPORT">Nhß║¡p kho</option>
              <option value="EXPORT">Xuß║Ñt b├ín</option>
              <option value="TRANSFER">─Éiß╗üu chuyß╗ân</option>
            </select>
          </div>
          <!-- Lß╗ìc trß║íng th├íi -->
          <div>
            <select v-model="filterStatus"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="">-- Tß║Ñt cß║ú trß║íng th├íi --</option>
              <option value="DRAFT">Chß╗¥ duyß╗çt</option>
              <option value="COMPLETED">─É├ú duyß╗çt</option>
              <option value="CANCELLED">─É├ú hß╗ºy</option>
              <option value="RECEIVED">─É├ú nhß║¡n h├áng</option>
            </select>
          </div>
          <!-- Lß╗ìc hao hß╗Ñt / ch├¬nh lß╗çch -->
          <div>
            <select v-model="filterDeviation"
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
              <option value="">-- Tß║Ñt cß║ú ch├¬nh lß╗çch --</option>
              <option value="yes">C├│ ch├¬nh lß╗çch / Hao hß╗Ñt</option>
              <option value="no">Khß╗¢p sß╗æ l╞░ß╗úng</option>
            </select>
          </div>
          <!-- Thß╗¥i gian v├á Ng├áy -->
          <div class="lg:col-span-4 grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Thß╗¥i gian</label>
              <select v-model="filterTimeRange" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
                <option value="today">H├┤m nay</option>
                <option value="week">7 ng├áy qua</option>
                <option value="last_week">Tuß║ºn tr╞░ß╗¢c (14 ng├áy qua)</option>
                <option value="this_month">Th├íng n├áy</option>
                <option value="month">30 ng├áy qua</option>
                <option value="custom">T├╣y chß╗ìn...</option>
              </select>
            </div>
            <!-- Tß╗½ ng├áy -->
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">Tß╗½ ng├áy</label>
              <input v-model="filterStartDate" type="date" :disabled="filterTimeRange !== 'custom'"
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed" />
            </div>
            <!-- ─Éß║┐n ng├áy -->
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-1.5">─Éß║┐n ng├áy</label>
              <input v-model="filterEndDate" type="date" :disabled="filterTimeRange !== 'custom'"
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] disabled:opacity-50 disabled:bg-gray-100 disabled:cursor-not-allowed" />
            </div>
            <!-- N├║t X├│a lß╗ìc -->
            <div class="flex items-end">
              <button v-if="filterType || filterStatus || searchKeyword || filterDeviation || filterTimeRange !== 'this_month'"
                @click="filterType = ''; filterStatus = ''; searchKeyword = ''; filterTimeRange = 'this_month'; filterDeviation = ''"
                class="w-full h-11 flex items-center justify-center gap-2 px-6 bg-white border border-[#e2e8f0] rounded-xl text-sm font-semibold text-[#8094ae] hover:text-[#364a63] hover:bg-[#f8f9fa] transition-all shadow-sm">
                <i class="fas fa-times"></i> X├│a lß╗ìc
              </button>
            </div>
          </div>
        </div>

      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center h-48 gap-3 text-[#8094ae]">
        <i class="fas fa-spinner fa-spin text-2xl"></i>
        <span class="font-semibold">─Éang tß║úi dß╗» liß╗çu...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredReceipts.length === 0" class="text-center py-16 text-[#8094ae]">
        <i class="fas fa-inbox text-5xl mb-4 opacity-30"></i>
        <p class="font-semibold">Kh├┤ng c├│ phiß║┐u kho n├áo ph├╣ hß╗úp</p>
      </div>

      <!-- Table -->
      <div v-else class="w-full">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-[#f8f9fa] text-[#8094ae] text-xs uppercase tracking-wider">
              <th class="px-5 py-3 text-left font-bold">M├ú phiß║┐u</th>
              <th v-if="!receiptType" class="px-5 py-3 text-left font-bold">Loß║íi</th>
              <th class="px-5 py-3 text-center font-bold">Trß║íng th├íi</th>
              <th class="px-5 py-3 text-left font-bold">Ch├¬nh lß╗çch</th>
              <th class="px-5 py-3 text-left font-bold">Chi nh├ính nguß╗ôn</th>
              <th class="px-5 py-3 text-left font-bold">{{ $route.path === '/invoices' ? 'Kh├ích h├áng' : 'Chi nh├ính ─æ├¡ch' }}</th>
              <th class="px-5 py-3 text-left font-bold">Ng╞░ß╗¥i lß║¡p</th>
              <th class="px-5 py-3 text-left font-bold">Ng├áy tß║ío</th>
              <th class="px-5 py-3 text-center font-bold">Thao t├íc</th>
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
                  <div :class="['sky-status-badge relative overflow-hidden inline-flex items-center px-3 py-1.5 rounded-xl border text-[11px] uppercase tracking-wider font-bold shadow-sm group', statusClass(r)]">
                    <i class="fas fa-sun sun-icon absolute -right-1 -top-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <i class="fas fa-moon moon-icon absolute -right-1 -bottom-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <span class="relative z-10">{{ statusLabel(r) }}</span>
                  </div>
                  <span v-if="r.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold mt-1', paymentStatusClass(r.paymentStatus)]">
                    ≡ƒôª ─É├ú nhß║¡n h├áng
                  </span>
                </div>
              </td>
              <td class="px-5 py-4">
                <div v-if="r.hasDeviation" class="flex flex-col max-w-[200px]" :title="r.deviationSummary">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-600 border border-rose-100 w-fit">
                    ΓÜá∩╕Å Lß╗çch sß╗æ l╞░ß╗úng
                  </span>
                  <span class="text-xs text-rose-500 mt-1 font-medium truncate" :title="r.deviationSummary">
                    {{ r.deviationSummary }}
                  </span>
                </div>
                <div v-else-if="r.status === 'COMPLETED' || r.status === 'PENDING_COMPENSATION' || r.paymentStatus === 'RECEIVED'" class="text-xs text-emerald-600 font-medium">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-100 w-fit">
                    Khß╗¢p
                  </span>
                </div>
                <div v-else class="text-xs text-slate-400">ΓÇö</div>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#364a63] font-medium">{{ r.sourceBranchName || 'ΓÇö' }}</span>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#364a63] font-medium" v-if="r.type === 'EXPORT'">{{ getCustomerName(r) }}</span>
                <span class="text-[#364a63] font-medium" v-else>{{ r.destBranchName || 'ΓÇö' }}</span>
              </td>
              <td class="px-5 py-4">
                <div class="text-[#8094ae]">{{ r.createdByName }}</div>
                <div v-if="r.stocktakeByName" class="text-xs text-purple-600 mt-1 font-semibold" title="Ng╞░ß╗¥i kiß╗âm k├¬"><i class="fas fa-clipboard-check"></i> {{ r.stocktakeByName }}</div>
                <div v-else-if="r.status === 'COMPLETED' && (r.type === 'IMPORT' || r.type === 'TRANSFER') && r.createdByRole === 'STAFF'" class="text-xs text-purple-600 mt-1 font-semibold opacity-60" title="Ng╞░ß╗¥i kiß╗âm k├¬ (Dß╗» liß╗çu c┼⌐)"><i class="fas fa-clipboard-check"></i> {{ r.createdByName }}</div>
              </td>
              <td class="px-5 py-4">
                <span class="text-[#8094ae] text-xs">{{ formatDateTime(r.createdAt) }}</span>
              </td>
              <td class="px-5 py-4">
                <div class="flex items-center justify-center gap-2">
                  <button @click.stop="openDetail(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-[#f1f5f9] hover:bg-[#4361ee] hover:text-white text-[#8094ae] transition-all"
                    title="Xem chi tiß║┐t">
                    <i class="fas fa-eye text-xs"></i>
                  </button>

                  <button v-if="canApproveReceipt(r)"
                    @click.stop="approveReceipt(r)"
                    :disabled="approvingId === r.id"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-green-50 hover:bg-green-500 hover:text-white text-green-600 transition-all disabled:opacity-50"
                    title="Ph├¬ duyß╗çt">
                    <i class="fas fa-check text-xs"></i>
                  </button>
                  <button v-if="canConfirmStocktake(r)"
                    @click.stop="openStocktakeModal(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-purple-50 hover:bg-purple-500 hover:text-white text-purple-600 transition-all"
                    title="Thß╗▒c hiß╗çn kiß╗âm k├¬">
                    <i class="fas fa-boxes text-xs"></i>
                  </button>
                  <button v-if="canCancelReceipt(r)"
                    @click.stop="confirmCancelReceipt(r)"
                    class="w-8 h-8 flex items-center justify-center rounded-lg bg-red-50 hover:bg-red-500 hover:text-white text-red-500 transition-all"
                    title="Hß╗ºy phiß║┐u">
                    <i class="fas fa-times text-xs"></i>
                  </button>
                  <button v-if="canConfirmTransfer(r)"
                    @click.stop="openConfirmTransferModal(r)"
                    class="h-8 px-3 flex items-center justify-center rounded-lg bg-sky-50 hover:bg-sky-500 hover:text-white text-sky-600 transition-all text-xs font-bold"
                    title="X├íc nhß║¡n nhß║¡n h├áng">
                    <i class="fas fa-truck-loading mr-1"></i>X├íc nhß║¡n
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
          Hiß╗ân thß╗ï <span class="font-bold text-[#364a63]">{{ (currentPage - 1) * itemsPerPage + 1 }}</span> - <span class="font-bold text-[#364a63]">{{ Math.min(currentPage * itemsPerPage, filteredReceipts.length) }}</span> trong sß╗æ <span class="font-bold text-[#364a63]">{{ filteredReceipts.length }}</span> phiß║┐u
        </div>
        <div class="flex items-center gap-2">
          <button @click="currentPage--" :disabled="currentPage === 1"
            class="px-3 py-1.5 flex items-center justify-center rounded-lg border border-[#e2e8f0] bg-white text-[#364a63] font-medium text-sm hover:bg-[#f8f9fa] transition-all disabled:opacity-50 disabled:cursor-not-allowed">
            <i class="fas fa-chevron-left mr-1.5 text-[10px]"></i> Tr╞░ß╗¢c
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

    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <!-- DETAIL PANEL MODAL -->
    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
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
                {{ selectedReceipt?.type === 'EXPORT' ? 'Chi tiß║┐t h├│a ─æ╞ín' : selectedReceipt?.type === 'TRANSFER' ? 'Chi tiß║┐t phiß║┐u ─æiß╗üu chuyß╗ân' : selectedReceipt?.type === 'ADJUST_OUT' ? 'Chi tiß║┐t phiß║┐u ti├¬u hß╗ºy' : 'Chi tiß║┐t phiß║┐u kho' }}
              </div>
              <div class="font-mono font-bold text-xl">{{ selectedReceipt?.code }}</div>
            </div>
            <button @click="showDetail = false" class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="overflow-y-auto flex-1 p-6 space-y-5 custom-scrollbar">
            <!-- ΓöÇΓöÇ BANNER L├¥ DO Hß╗ªY ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ -->
            <div v-if="selectedReceipt.description && selectedReceipt.description.includes('[L├╜ do hß╗ºy]')" class="bg-red-50 border border-red-200 rounded-2xl p-4 flex gap-4 shadow-sm relative overflow-hidden group">
              <div class="absolute inset-0 bg-gradient-to-r from-red-500/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
              <div class="w-10 h-10 rounded-full bg-red-100 border border-red-200 flex items-center justify-center shrink-0">
                <i class="fas fa-ban text-red-600 text-lg"></i>
              </div>
              <div class="flex-1">
                <div class="text-sm font-bold text-red-700 uppercase tracking-wide mb-1 flex items-center gap-2">
                  L├╜ do hß╗ºy phiß║┐u
                </div>
                <div class="text-sm text-red-600 font-medium whitespace-pre-line">
                  {{ selectedReceipt.description.split('\n').find((l: string) => l.startsWith('[L├╜ do hß╗ºy]'))?.replace('[L├╜ do hß╗ºy]', '').trim() }}
                </div>
              </div>
            </div>

            <!-- Meta info -->
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Loß║íi phiß║┐u</div>
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', typeClass(selectedReceipt.type)]">
                  {{ typeLabel(selectedReceipt.type) }}
                </span>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Trß║íng th├íi</div>
                <div class="flex flex-col items-start gap-1">
                  <div :class="['sky-status-badge relative overflow-hidden inline-flex items-center px-3 py-1.5 rounded-xl border text-[11px] uppercase tracking-wider font-bold shadow-sm group', statusClass(selectedReceipt)]">
                    <i class="fas fa-sun sun-icon absolute -right-1 -top-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <i class="fas fa-moon moon-icon absolute -right-1 -bottom-1 text-yellow-300 text-xl drop-shadow-[0_0_8px_rgba(253,224,71,0.8)]"></i>
                    <span class="relative z-10">{{ statusLabel(selectedReceipt) }}</span>
                  </div>
                  <span v-if="selectedReceipt.paymentStatus === 'RECEIVED'" :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', paymentStatusClass(selectedReceipt.paymentStatus)]">
                    ≡ƒôª ─É├ú nhß║¡n h├áng
                  </span>
                </div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nh├ính nguß╗ôn</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.sourceBranchName || 'ΓÇö' }}</div>
              </div>
              <div v-if="selectedReceipt.type === 'EXPORT'">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Kh├ích h├áng</div>
                <div class="font-semibold text-[#364a63]">{{ getCustomerName(selectedReceipt) }}</div>
              </div>
              <div v-else>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Chi nh├ính ─æ├¡ch</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.destBranchName || 'ΓÇö' }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ng╞░ß╗¥i lß║¡p</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.createdByName }}</div>
                <div v-if="selectedReceipt.stocktakeByName" class="text-xs text-purple-600 font-bold mt-1 flex items-center gap-1.5" title="Ng╞░ß╗¥i kiß╗âm k├¬"><i class="fas fa-clipboard-check"></i> Kiß╗âm k├¬: {{ selectedReceipt.stocktakeByName }}</div>
                <div v-else-if="selectedReceipt.status === 'COMPLETED' && (selectedReceipt.type === 'IMPORT' || selectedReceipt.type === 'TRANSFER') && selectedReceipt.createdByRole === 'STAFF'" class="text-xs text-purple-600 font-bold mt-1 flex items-center gap-1.5 opacity-60" title="Ng╞░ß╗¥i kiß╗âm k├¬ (Dß╗» liß╗çu c┼⌐)"><i class="fas fa-clipboard-check"></i> Kiß╗âm k├¬: {{ selectedReceipt.createdByName }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ng╞░ß╗¥i duyß╗çt</div>
                <div class="font-semibold text-[#364a63]">{{ selectedReceipt.approvedByName || 'ΓÇö' }}</div>
              </div>
              <div>
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ng├áy tß║ío</div>
                <div class="font-semibold text-[#364a63]">{{ formatDateTime(selectedReceipt.createdAt) }}</div>
              </div>
              <div v-if="selectedReceipt.type === 'EXPORT'">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Thanh to├ín</div>
                <span :class="['inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold', paymentStatusClass(selectedReceipt.paymentStatus)]">
                  {{ paymentStatusLabel(selectedReceipt.paymentStatus) }}
                </span>
              </div>
              <div v-if="selectedReceipt.description && selectedReceipt.description.split('\n').filter((l: string) => !l.startsWith('[L├╜ do hß╗ºy]')).join('\n').trim()">
                <div class="text-xs font-bold text-[#8094ae] uppercase mb-1">Ghi ch├║</div>
                <div class="text-[#364a63] text-xs whitespace-pre-line">{{ selectedReceipt.description.split('\n').filter((l: string) => !l.startsWith('[L├╜ do hß╗ºy]')).join('\n').trim() }}</div>
              </div>
            </div>

            <!-- Detail lines -->
            <div>
              <div class="text-xs font-bold text-[#8094ae] uppercase mb-3">Danh s├ích h├áng h├│a</div>
              <div class="rounded-xl border border-[#f1f5f9] overflow-hidden">
                <table class="w-full text-xs">
                  <thead>
                    <tr class="bg-[#f8f9fa] text-[#8094ae] uppercase">
                      <th class="px-4 py-2.5 text-left font-bold">Sß║ún phß║⌐m</th>
                      <th class="px-4 py-2.5 text-center font-bold">NSX</th>
                      <th class="px-4 py-2.5 text-center font-bold">HSD</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null)">SL Gß╗¡i</th>
                      <th class="px-4 py-2.5 text-right font-bold text-teal-600" v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null)">SL Nhß║¡n</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-else>SL</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">─É╞ín gi├í</th>
                      <th class="px-4 py-2.5 text-right font-bold" v-if="selectedReceipt.type !== 'IMPORT' && selectedReceipt.type !== 'TRANSFER'">Th├ánh tiß╗ün</th>
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
                      <td :colspan="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null) ? 6 : 5" class="px-4 py-2.5 text-right font-bold text-[#8094ae] text-xs uppercase">Tß╗òng cß╗Öng</td>
                      <td class="px-4 py-2.5 text-right font-extrabold text-[#4361ee]">
                        {{ formatVND((selectedReceipt.details || []).reduce((s: number, d: any) => s + d.quantity * d.price, 0)) }}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
              
              <!-- Shortfall reasons section -->
              <div v-if="selectedReceipt.details?.some((x: any) => x.receivedQuantity !== null && x.receivedQuantity < x.quantity)" class="mt-4 p-4 bg-red-50 rounded-xl border border-red-100">
                <div class="text-xs font-bold text-red-600 uppercase mb-2 flex items-center gap-1.5"><i class="fas fa-exclamation-circle"></i> L├╜ do hao hß╗Ñt</div>
                <div class="space-y-1.5">
                  <div v-for="d in selectedReceipt.details.filter((x: any) => x.receivedQuantity !== null && x.receivedQuantity < x.quantity)" :key="'reason-'+d.id" class="text-sm">
                    <span class="font-bold text-red-700">- {{ d.productName }} (Thiß║┐u {{ d.quantity - d.receivedQuantity }}):</span>
                    <span class="text-red-600 ml-1 whitespace-pre-wrap break-words">{{ d.shortfallReason }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- ΓöÇΓöÇ Banner cß║únh b├ío: Chß╗¥ Staff x├íc nhß║¡n ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ -->
            <div v-if="selectedReceipt.status === 'PENDING_STAFF_CONFIRM' && canStaffAcknowledge(selectedReceipt)"
              class="mt-6 p-4 bg-amber-50 border border-amber-300 rounded-2xl flex items-start gap-3">
              <i class="fas fa-exclamation-triangle text-amber-500 mt-0.5 text-lg flex-shrink-0"></i>
              <div class="flex-1">
                <div class="font-bold text-amber-800 text-sm">Manager ─æ├ú ─æiß╗üu chß╗ënh phiß║┐u n├áy</div>
                <div class="text-amber-700 text-xs mt-0.5">Vui l├▓ng xem lß╗ïch sß╗¡ chß╗ënh sß╗¡a b├¬n d╞░ß╗¢i v├á x├íc nhß║¡n ─æß╗â gß╗¡i lß║íi l├¬n Manager.</div>
              </div>
            </div>

            <!-- ΓöÇΓöÇ Khu vß╗▒c chß╗ënh sß╗¡a phiß║┐u ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ -->
            <div class="mt-6 pt-5 border-t flex flex-wrap items-center gap-3">
              <!-- N├║t Sß╗¡a phiß║┐u (Staff) -->
              <button v-if="canStaffEdit(selectedReceipt)"
                @click="openEditModal('staff')"
                class="h-9 px-4 bg-[#eef2ff] hover:bg-[#4361ee] hover:text-white text-[#4361ee] border border-[#4361ee]/30 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
                <i class="fas fa-pen"></i> Sß╗¡a phiß║┐u
              </button>
              <!-- N├║t Sß╗¡a + Gß╗¡i xuß╗æng (Manager khi DRAFT) -->
              <button v-if="canManagerEdit(selectedReceipt) && selectedReceipt.status === 'DRAFT'"
                @click="openEditModal('manager')"
                class="h-9 px-4 bg-orange-50 hover:bg-orange-500 hover:text-white text-orange-600 border border-orange-300 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
                <i class="fas fa-pen-to-square"></i> Sß╗¡a & Gß╗¡i Staff
              </button>
              <!-- N├║t Sß╗¡a (Manager khi PENDING_ADMIN ΓÇö ghi l├╜ do cho Admin) -->
              <button v-if="canManagerEdit(selectedReceipt) && selectedReceipt.status === 'PENDING_ADMIN'"
                @click="openEditModal('manager')"
                class="h-9 px-4 bg-blue-50 hover:bg-blue-500 hover:text-white text-blue-600 border border-blue-300 rounded-xl text-xs font-bold transition-all flex items-center gap-2">
                <i class="fas fa-pen-to-square"></i> {{ selectedReceipt?.type === 'TRANSFER' ? 'Sß╗¡a (Ghi ch├║ cho Chi nh├ính nguß╗ôn)' : 'Sß╗¡a (Ghi ch├║ cho Admin)' }}
              </button>
              <!-- N├║t X├íc nhß║¡n thay ─æß╗òi (Staff) -->
              <button v-if="canStaffAcknowledge(selectedReceipt)"
                @click="staffAcknowledgeEdit()" :disabled="submittingAcknowledge"
                class="h-9 px-4 bg-amber-400 hover:bg-amber-500 text-white rounded-xl text-xs font-bold transition-all flex items-center gap-2 shadow-sm">
                <i class="fas fa-check" v-if="!submittingAcknowledge"></i>
                <i class="fas fa-spinner fa-spin" v-else></i>
                X├íc nhß║¡n thay ─æß╗òi
              </button>
              <!-- N├║t Lß╗ïch sß╗¡ chß╗ënh sß╗¡a -->
              <button v-if="editHistoryList.length > 0" @click.prevent="toggleEditHistory" type="button"
                class="h-9 px-3 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5 ml-auto border border-slate-200 relative">
                <i class="fas fa-history text-xs"></i>
                Lß╗ïch sß╗¡ sß╗¡a ({{ editHistoryList.length }})
                <i class="fas fa-chevron-down text-[10px] transition-transform" :class="showEditHistory ? 'rotate-180' : ''"></i>
                <span v-if="!showEditHistory && !hasSeenEditHistory" class="absolute -top-1 -right-1 flex h-3 w-3">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-3 w-3 bg-red-500 border-2 border-white"></span>
                </span>
              </button>
            </div>

            <!-- ΓöÇΓöÇ Panel lß╗ïch sß╗¡ chß╗ënh sß╗¡a ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ -->
            <Transition name="slide-down">
              <div v-if="showEditHistory" class="mt-2 rounded-2xl border border-slate-200 overflow-hidden">
                <div class="bg-slate-50 px-4 py-2.5 flex items-center gap-2 border-b border-slate-200">
                  <i class="fas fa-history text-slate-400 text-xs"></i>
                  <span class="text-xs font-bold text-slate-500 uppercase tracking-wider">Lß╗ïch sß╗¡ chß╗ënh sß╗¡a</span>
                </div>
                <div v-if="editHistoryList.length === 0" class="px-4 py-5 text-center text-xs text-slate-400">
                  Ch╞░a c├│ lß╗ïch sß╗¡ chß╗ënh sß╗¡a n├áo.
                </div>
                <div v-else class="divide-y divide-slate-100">
                  <div v-for="log in editHistoryList" :key="log.id" class="px-4 py-3">
                    <div class="flex items-start justify-between gap-3">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-2 flex-wrap mb-1">
                          <span class="text-xs font-bold text-slate-700">{{ log.editorName }}</span>
                          <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold"
                            :class="log.editorRole === 'STAFF' ? 'bg-blue-50 text-blue-600' : 'bg-orange-50 text-orange-600'">
                            {{ log.editorRole === 'STAFF' ? 'Nh├ón vi├¬n' : 'Quß║ún l├╜' }}
                          </span>
                          <span class="text-[10px] text-slate-400">{{ directionLabel(log.direction) }}</span>
                        </div>
                        <div class="text-xs text-slate-600 mb-1">
                          <span class="font-semibold text-slate-500">L├╜ do:</span> {{ log.editReason }}
                        </div>
                        <div v-if="log.changes" class="text-xs text-slate-500">
                          <span class="font-semibold">Thay ─æß╗òi:</span> {{ log.changes }}
                        </div>
                        <div v-if="log.acknowledgedAt" class="mt-1 text-[10px] text-green-600 flex items-center gap-1">
                          <i class="fas fa-check-circle"></i>
                          {{ log.acknowledgedByName }} ─æ├ú x├íc nhß║¡n l├║c {{ formatDateTime(log.acknowledgedAt) }}
                        </div>
                        <div v-else-if="log.direction === 'MANAGER_TO_STAFF'" class="mt-1 text-[10px] text-amber-500 flex items-center gap-1">
                          <i class="fas fa-clock"></i> Chß╗¥ Staff x├íc nhß║¡n
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
                <i class="fas fa-ban"></i> Hß╗ºy phiß║┐u
              </button>
            </div>
            <div v-else-if="canCancelReceipt(selectedReceipt)" class="mt-8 pt-5 border-t flex gap-4">
              <button @click="confirmCancelReceipt(selectedReceipt)"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Hß╗ºy phiß║┐u
              </button>
            </div>

            <!-- Mark as Paid -->
            <div v-if="selectedReceipt.status === 'COMPLETED' && selectedReceipt.type === 'EXPORT' && (selectedReceipt.paymentStatus === 'UNPAID' || selectedReceipt.paymentStatus === 'Ch╞░a thanh to├ín')" class="mt-8 pt-5 border-t">
              <button @click="markAsPaid(selectedReceipt)" :disabled="markingPaidId === selectedReceipt.id"
                class="px-5 py-2.5 bg-[#f59e0b] hover:bg-[#d97706] text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-hand-holding-usd" v-if="markingPaidId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i>
                X├íc nhß║¡n ─É├ú Thanh To├ín
              </button>
              <p class="text-xs text-gray-500 mt-2"><i class="fas fa-info-circle"></i> C├┤ng nß╗ú kh├ích h├áng sß║╜ ─æ╞░ß╗úc cß║Ñn trß╗½ t╞░╞íng ß╗⌐ng khi x├íc nhß║¡n.</p>
            </div>

            <!-- Confirm Receive (Transfer) -->
            <div v-if="canConfirmTransfer(selectedReceipt)" class="mt-8 pt-5 border-t">
              <button @click="openConfirmTransferModal(selectedReceipt)"
                class="px-5 py-2.5 bg-sky-500 hover:bg-sky-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-box-open"></i> X├íc nhß║¡n Nhß║¡n H├áng & Cß╗Öng Kho
              </button>
            </div>

            <!-- Confirm Stocktake (Import) -->
            <div v-if="canConfirmStocktake(selectedReceipt)" class="mt-8 pt-5 border-t">
              <button @click="openStocktakeModal(selectedReceipt)"
                class="px-5 py-2.5 bg-purple-500 hover:bg-purple-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-boxes"></i> Thß╗▒c hiß╗çn Kiß╗âm k├¬ & Chß║Ñp nhß║¡n
              </button>
              <p class="text-xs text-gray-500 mt-2"><i class="fas fa-info-circle"></i> Vui l├▓ng ─æß║┐m lß║íi thß╗▒c tß║┐ h├áng h├│a tß║íi kho tr╞░ß╗¢c khi x├íc nhß║¡n cß╗Öng kho.</p>
            </div>

            <!-- Approve Shortfall (Hao hß╗Ñt) -->
            <div v-if="canApproveShortfall(selectedReceipt)" class="mt-8 pt-5 border-t flex flex-wrap gap-4">
              <button @click="approveShortfall(selectedReceipt, true)" :disabled="approvingShortfallId === selectedReceipt.id"
                class="px-5 py-2.5 bg-orange-500 hover:bg-orange-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-check-circle" v-if="approvingShortfallId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i> 
                Duyß╗çt Hao Hß╗Ñt
              </button>
              <button v-if="selectedReceipt.status === 'PENDING_SHORTFALL_MANAGER'" @click="approveShortfall(selectedReceipt, false)" :disabled="approvingShortfallId === selectedReceipt.id"
                class="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-ban"></i> Tß╗½ chß╗æi Hao Hß╗Ñt
              </button>
            </div>

            <!-- Compensate Shortfall (─Éiß╗üu chuyß╗ân b├╣) -->
            <div v-if="canCompensate(selectedReceipt)" class="mt-8 pt-5 border-t">
              <button @click="compensateShortfall(selectedReceipt)" :disabled="compensatingId === selectedReceipt.id"
                class="px-5 py-2.5 bg-indigo-500 hover:bg-indigo-600 text-white rounded-xl font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2">
                <i class="fas fa-truck-loading" v-if="compensatingId !== selectedReceipt.id"></i>
                <i class="fas fa-spinner fa-spin" v-else></i>
                Tß║ío Phiß║┐u ─Éiß╗üu Chuyß╗ân B├╣
              </button>
              <p class="text-xs text-gray-500 mt-2"><i class="fas fa-info-circle"></i> Sß║╜ tß║ío mß╗Öt phiß║┐u ─Éiß╗üu chuyß╗ân mß╗¢i c├│ sß╗æ l╞░ß╗úng bß║▒ng ─æ├║ng sß╗æ l╞░ß╗úng hao hß╗Ñt. Phiß║┐u c┼⌐ sß║╜ ─æ╞░ß╗úc ─æ├│ng.</p>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <!-- CREATE DRAFT MODAL -->
    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
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
                {{ createForm.type === 'EXPORT' ? 'Lß║¡p h├│a ─æ╞ín' : createForm.type === 'TRANSFER' ? 'Lß║¡p phiß║┐u ─æiß╗üu chuyß╗ân' : createForm.type === 'ADJUST_OUT' ? 'Lß║¡p phiß║┐u ti├¬u hß╗ºy' : 'Lß║¡p phiß║┐u kho' }}
              </div>
              <div class="font-bold text-xl">{{ createForm.type === 'TRANSFER' ? 'Tß║ío phiß║┐u xin h├áng (DRAFT)' : 'Tß║ío phiß║┐u nh├íp (DRAFT)' }}</div>
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
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Loß║íi phiß║┐u <span class="text-red-500">*</span></label>
                  <select v-model="createForm.type" @change="onTypeChange"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="IMPORT">≡ƒôÑ Nhß║¡p kho</option>
                    <option value="EXPORT">≡ƒôñ Xuß║Ñt b├ín</option>
                    <option value="TRANSFER">≡ƒöä ─Éiß╗üu chuyß╗ân</option>
                  </select>
                </div>
                <div v-else>
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Loß║íi phiß║┐u</label>
                  <input type="text" disabled
                    :value="receiptType === 'IMPORT' ? '≡ƒôÑ Nhß║¡p kho' : receiptType === 'EXPORT' ? '≡ƒôñ H├│a ─æ╞ín' : receiptType === 'ADJUST_OUT' ? '≡ƒùæ∩╕Å Ti├¬u hß╗ºy' : '≡ƒöä ─Éiß╗üu chuyß╗ân'"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm bg-[#f1f5f9] text-[#8094ae] cursor-not-allowed" />
                </div>
                <div v-if="createForm.type === 'IMPORT'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nh├ính nguß╗ôn</label>
                  <select v-model="createForm.sourceBranchId" disabled
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae] cursor-not-allowed">
                    <!-- Chi nh├ính gß╗æc (H├á Nß╗Öi): nguß╗ôn = b├¬n ngo├ái hß╗ç thß╗æng -->
                    <option v-if="isHeadBranch" value="">-- B├¬n ngo├ái hß╗ç thß╗æng --</option>
                    <!-- Chi nh├ính con: nguß╗ôn = chi nh├ính H├á Nß╗Öi -->
                    <option v-if="!isHeadBranch && headBranch" :value="headBranch.id">{{ headBranch.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'IMPORT' || createForm.type === 'TRANSFER' || createForm.type === 'ADJUST_IN'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Chi nh├ính ─æ├¡ch</label>
                  <select v-model="createForm.destBranchId" :disabled="createForm.type === 'IMPORT'"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-[#f1f5f9] disabled:text-[#8094ae]">
                    <option value="">-- Chß╗ìn chi nh├ính --</option>
                    <option v-for="b in branches.filter(x => x.id !== createForm.sourceBranchId)" :key="b.id" :value="b.id">{{ b.name }}</option>
                  </select>
                </div>
                <div v-if="createForm.type === 'EXPORT'">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Kh├ích h├áng <span class="text-red-500">*</span></label>
                  <div class="relative">
                    <input v-model="createForm.customerName" @focus="showCustomerDropdown = true" @blur="hideCustomerDropdown" @input="onCustomerInput" type="text" placeholder="Nhß║¡p t├¬n kh├ích h├áng..."
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
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Sß╗æ ─æiß╗çn thoß║íi <span class="text-red-500">*</span></label>
                  <input v-model="createForm.customerPhone" @input="createForm.customerId = ''" type="text" placeholder="Nhß║¡p sß╗æ ─æiß╗çn thoß║íi..."
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div v-if="createForm.type === 'EXPORT'" class="col-span-2 sm:col-span-1">
                  <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Trß║íng th├íi thanh to├ín</label>
                  <select v-model="createForm.paymentStatus"
                    class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none">
                    <option value="UNPAID">Ch╞░a thanh to├ín</option>
                    <option value="PAID">─É├ú thanh to├ín</option>
                  </select>
                </div>
                <div class="col-span-2">
                  <div class="flex justify-between items-center mb-1.5">
                    <label class="block text-xs font-bold text-[#8094ae] uppercase">
                      {{ createForm.type === 'ADJUST_OUT' ? 'L├╜ do ti├¬u hß╗ºy' : 'Ghi ch├║' }}
                      <span v-if="createForm.type === 'ADJUST_OUT'" class="text-red-500">*</span>
                    </label>
                    <span class="text-[10px] text-[#8094ae]">{{ createForm.description?.length || 0 }}/500</span>
                  </div>
                  <textarea v-model="createForm.description" maxlength="500" :placeholder="createForm.type === 'ADJUST_OUT' ? 'Nhß║¡p l├╜ do ti├¬u hß╗ºy (bß║»t buß╗Öc)...' : 'Ghi ch├║ (tuß╗│ chß╗ìn)...'"
                    class="w-full h-20 p-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none resize-y"></textarea>
                </div>
              </div>

              <!-- Detail rows -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <div class="text-xs font-bold text-[#8094ae] uppercase">Danh s├ích h├áng h├│a</div>
                  <button @click="addDetailRow"
                    class="h-8 px-3 bg-[#eef2ff] hover:bg-[#4361ee] hover:text-white text-[#4361ee] rounded-lg text-xs font-bold transition-all flex items-center gap-1">
                    <i class="fas fa-plus"></i> Th├¬m d├▓ng
                  </button>
                </div>
                <div class="space-y-3">
                  <div v-for="(d, idx) in createForm.details" :key="idx"
                    class="border border-[#e2e8f0] rounded-xl p-4 bg-white space-y-3">
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-bold text-[#8094ae]">D├▓ng {{ idx + 1 }}</span>
                      <button v-if="createForm.details.length > 1" @click="removeDetailRow(idx)"
                        class="w-6 h-6 flex items-center justify-center rounded bg-red-50 text-red-400 hover:bg-red-500 hover:text-white transition-all">
                        <i class="fas fa-times text-xs"></i>
                      </button>
                    </div>
                    <div class="space-y-4">
                      <!-- Sß║ún phß║⌐m & L├┤ sß║ún xuß║Ñt (Chiß║┐m to├án bß╗Ö chiß╗üu ngang) -->
                      <div class="grid grid-cols-12 gap-4">
                        <div class="col-span-12 lg:col-span-6">
                          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Sß║ún phß║⌐m <span class="text-red-500">*</span></label>
                          <select v-model="d.productId" @change="onProductChange(d)"
                            class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                            <option value="">-- Chß╗ìn sß║ún phß║⌐m --</option>
                            <option v-for="p in getAvailableProductsForRow(idx)" :key="p.id" :value="p.id">{{ p.name }} ({{ p.sku }})</option>
                          </select>
                        </div>
                        
                        <div class="col-span-12 lg:col-span-6" v-if="d.productId">
                          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">L├┤ sß║ún xuß║Ñt <span class="text-red-500">*</span></label>
                          <div class="flex gap-2">
                            <select v-if="!d.isNewBatch" v-model="d.batchCode" @change="onBatchChange(d)"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none bg-white">
                              <option value="">-- Chß╗ìn l├┤ --</option>
                              <option v-for="b in getBatchesForProduct(d.productId)" :key="b.batchCode" :value="b.batchCode">
                                {{ b.batchCode }} (Tß╗ôn: {{ b.quantity }})
                              </option>
                            </select>
                            <input v-else v-model="d.batchCode" type="text" placeholder="Nhß║¡p m├ú l├┤ mß╗¢i..."
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium outline-none focus:border-[#4361ee]" />
                            
                            <button v-if="createForm.type === 'IMPORT' && createForm.sourceBranchId === createForm.destBranchId" @click="d.isNewBatch = !d.isNewBatch; d.batchCode = ''" 
                                    class="px-3 h-10 border border-[#e2e8f0] rounded-xl text-xs font-bold bg-white hover:bg-gray-50 whitespace-nowrap shadow-sm transition-all text-[#364a63]">
                              <i :class="d.isNewBatch ? 'fas fa-list text-[#4361ee] mr-1' : 'fas fa-plus text-[#10b981] mr-1'"></i> {{ d.isNewBatch ? 'Chß╗ìn l├┤ c├│ sß║╡n' : 'Tß║ío l├┤ mß╗¢i' }}
                            </button>
                          </div>
                        </div>
                      </div>

                      <!-- Th├┤ng tin Sß╗æ l╞░ß╗úng, Tiß╗ün & NSX/HSD (Nh├│m trong khung nß╗ün x├ím nhß║ít ─æß╗â dß╗à nh├¼n) -->
                      <div v-if="d.productId" class="p-4 bg-[#f8f9fa] rounded-xl border border-[#e2e8f0] space-y-4">
                        <!-- Row 1: Sß╗æ l╞░ß╗úng, ─É╞ín gi├í, Th├ánh tiß╗ün -->
                        <div :class="(createForm.type === 'IMPORT' || createForm.type === 'TRANSFER') ? 'grid grid-cols-1' : 'grid grid-cols-3 gap-5'">
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Sß╗æ l╞░ß╗úng <span class="text-red-500">*</span></label>
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
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">─É╞ín gi├í</label>
                            <input v-model.number="d.price" type="number" min="0" readonly
                              class="w-full h-10 px-3 border border-[#e2e8f0] bg-gray-100 rounded-xl text-sm font-bold outline-none cursor-not-allowed text-[#8094ae]" />
                          </div>
                          
                          <div v-if="createForm.type !== 'IMPORT' && createForm.type !== 'TRANSFER'">
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Th├ánh tiß╗ün</label>
                            <div class="w-full h-10 px-3 border border-transparent flex items-center text-sm font-bold text-[#4361ee] bg-[#eef2ff] rounded-xl overflow-x-auto whitespace-nowrap hide-scrollbar">
                              {{ formatVND(d.quantity * (d.price || 0)) }}
                            </div>
                          </div>
                        </div>

                        <!-- Row 2: NSX, HSD -->
                        <div v-if="selectedProductHasExpiry(d) && (d.isNewBatch || d.batchCode)" class="grid grid-cols-2 gap-5 pt-4 border-t border-[#e2e8f0]">
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Ng├áy sß║ún xuß║Ñt</label>
                            <input v-model="d.manufacturingDate" type="date" :disabled="!d.isNewBatch"
                              class="w-full h-10 px-3 border border-[#e2e8f0] rounded-xl text-sm font-medium focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none disabled:bg-gray-100 disabled:text-gray-500" />
                          </div>
                          <div>
                            <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Hß║ín sß╗¡ dß╗Ñng</label>
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
              Hß╗ºy
            </button>
            <button @click="submitCreateDraft" :disabled="submittingCreate"
              class="px-6 py-2.5 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl font-bold text-sm transition-all disabled:opacity-60 flex items-center gap-2">
              <i class="fas fa-spinner fa-spin" v-if="submittingCreate"></i>
              <i class="fas fa-save" v-else></i>
              L╞░u nh├íp
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <!-- STOCKTAKE MODAL (IMPORT) -->
    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <Teleport to="body">
      <div v-if="showStocktakeModal" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[110] flex items-center justify-center p-4">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-purple-500 to-fuchsia-400 text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">Kiß╗âm k├¬ & Nhß║¡p kho</div>
              <div class="font-bold text-lg">Phiß║┐u: {{ stocktakeReceipt?.code }}</div>
            </div>
            <button @click="showStocktakeModal = false" class="w-9 h-9 flex items-center justify-center rounded-xl bg-white/20 hover:bg-white/30 transition-all">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div class="bg-purple-50 border border-purple-200 rounded-xl p-4 text-sm text-purple-700">
              <i class="fas fa-info-circle mr-2"></i>
              Nhß║¡p <strong>sß╗æ l╞░ß╗úng thß╗▒c ─æß║┐m</strong> tß║íi kho. Nß║┐u c├│ hao hß╗Ñt, vui l├▓ng ghi r├╡ l├╜ do ─æß╗â ─æß╗æi so├ít. H├áng h├│a sß║╜ ─æ╞░ß╗úc cß╗Öng v├áo kho t╞░╞íng ß╗⌐ng vß╗¢i sß╗æ l╞░ß╗úng thß╗▒c ─æß║┐m.
            </div>

            <div class="space-y-3 custom-scrollbar max-h-[50vh] overflow-y-auto">
              <div v-for="item in stocktakeItems" :key="item.receiptDetailId"
                class="border border-[#e2e8f0] rounded-xl p-4">
                <div class="flex items-center justify-between mb-2">
                  <span class="font-semibold text-sm text-[#364a63]">{{ item.productName }}</span>
                  <span class="text-xs text-[#8094ae]">Sß╗æ tr├¬n phiß║┐u: <strong>{{ item.sentQty }}</strong></span>
                </div>
                <div class="flex items-center gap-3">
                  <label class="text-xs text-[#8094ae] whitespace-nowrap">Thß╗▒c ─æß║┐m:</label>
                  <input v-model.number="item.actualQuantity" type="number" :min="0" :max="item.sentQty" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                    @input="item.actualQuantity = item.actualQuantity > item.sentQty ? item.sentQty : (item.actualQuantity < 0 ? 0 : item.actualQuantity)"
                    class="flex-1 h-9 px-3 border rounded-lg text-sm focus:ring-2 focus:ring-purple-400/20 focus:border-purple-400 outline-none"
                    :class="item.actualQuantity < item.sentQty ? 'border-amber-400 bg-amber-50' : 'border-[#e2e8f0]'" />
                  <span v-if="item.actualQuantity < item.sentQty"
                    class="text-xs font-bold text-amber-600 whitespace-nowrap">
                    ΓÜá∩╕Å Hao hß╗Ñt: {{ item.sentQty - item.actualQuantity }}
                  </span>
                  <span v-else class="text-xs font-bold text-green-600 whitespace-nowrap">Γ£à ─Éß╗º</span>
                </div>
                <div v-if="item.actualQuantity < item.sentQty" class="mt-3 bg-red-50 p-3 rounded-lg border border-red-100 flex items-start gap-3">
                  <label class="text-xs font-bold text-red-600 whitespace-nowrap mt-2">L├╜ do <span class="text-red-500">*</span></label>
                  <textarea v-model="item.shortfallReason" rows="2" placeholder="VD: H╞░ hß╗Ång, thiß║┐u h├áng..."
                    class="flex-1 px-3 py-2 border border-red-200 rounded-lg text-sm focus:ring-2 focus:ring-red-400/20 focus:border-red-400 outline-none bg-white resize-none"></textarea>
                </div>
              </div>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-[#f1f5f9] flex justify-end gap-3 bg-[#f8f9fa]/50">
            <button @click="showStocktakeModal = false"
              class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63] text-sm hover:bg-[#f1f5f9] transition-all">
              ─É├│ng
            </button>
            <button @click="submitConfirmStocktake" :disabled="submittingStocktake"
              class="px-6 py-2.5 bg-purple-500 hover:bg-purple-600 text-white rounded-xl font-bold text-sm transition-all disabled:opacity-60 flex items-center gap-2">
              <i class="fas fa-spinner fa-spin" v-if="submittingStocktake"></i>
              <i class="fas fa-boxes" v-else></i>
              X├íc nhß║¡n Kiß╗âm k├¬
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <!-- CONFIRM TRANSFER MODAL -->
    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <Teleport to="body">
      <div v-if="showConfirmModal && confirmingReceipt"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[110] flex items-center justify-center p-4">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden">
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-sky-500 to-teal-400 text-white">
            <div>
              <div class="text-xs font-bold opacity-70 uppercase">X├íc nhß║¡n nhß║¡n h├áng</div>
              <div class="font-bold text-lg">Phiß║┐u: {{ confirmingReceipt.code }}</div>
            </div>
            <button @click="showConfirmModal = false" class="w-9 h-9 flex items-center justify-center rounded-xl bg-white/20 hover:bg-white/30 transition-all">
              <i class="fas fa-times"></i>
            </button>
          </div>

          <div class="p-6 space-y-4">
            <div class="bg-sky-50 border border-sky-200 rounded-xl p-4 text-sm text-sky-700">
              <i class="fas fa-info-circle mr-2"></i>
              Nhß║¡p <strong>sß╗æ l╞░ß╗úng thß╗▒c tß║┐ nhß║¡n ─æ╞░ß╗úc</strong> cho tß╗½ng sß║ún phß║⌐m. Hao hß╗Ñt trong qu├í tr├¼nh vß║¡n chuyß╗ân (nß║┐u c├│) sß║╜ ─æ╞░ß╗úc ghi nhß║¡n trß╗▒c tiß║┐p v├áo chi tiß║┐t phiß║┐u n├áy ─æß╗â ─æß╗æi so├ít.
            </div>

            <div class="space-y-3">
              <div v-for="item in confirmItems" :key="item.receiptDetailId"
                class="border border-[#e2e8f0] rounded-xl p-4">
                <div class="flex items-center justify-between mb-2">
                  <span class="font-semibold text-sm text-[#364a63]">{{ item.productName }}</span>
                  <span class="text-xs text-[#8094ae]">Sß╗æ ─æ├ú xuß║Ñt: <strong>{{ item.sentQty }}</strong></span>
                </div>
                <div class="flex items-center gap-3">
                  <label class="text-xs text-[#8094ae] whitespace-nowrap">Sß╗æ l╞░ß╗úng nhß║¡n:</label>
                  <input v-model.number="item.actualQuantity" type="number" :min="0" :max="item.sentQty" @keydown="(e) => { if(['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault() }"
                    @input="item.actualQuantity = item.actualQuantity > item.sentQty ? item.sentQty : (item.actualQuantity < 0 ? 0 : item.actualQuantity)"
                    class="flex-1 h-9 px-3 border rounded-lg text-sm focus:ring-2 focus:ring-sky-400/20 focus:border-sky-400 outline-none"
                    :class="item.actualQuantity < item.sentQty ? 'border-amber-400 bg-amber-50' : 'border-[#e2e8f0]'" />
                  <span v-if="item.actualQuantity < item.sentQty"
                    class="text-xs font-bold text-amber-600 whitespace-nowrap">
                    ΓÜá∩╕Å Hao hß╗Ñt: {{ item.sentQty - item.actualQuantity }}
                  </span>
                  <span v-else class="text-xs font-bold text-green-600 whitespace-nowrap">Γ£à ─Éß╗º</span>
                </div>
                <!-- Input for shortfall reason -->
                <div v-if="item.actualQuantity < item.sentQty" class="mt-3 bg-red-50 p-3 rounded-lg border border-red-100 flex items-start gap-3">
                  <label class="text-xs font-bold text-red-600 whitespace-nowrap mt-2">L├╜ do hao hß╗Ñt <span class="text-red-500">*</span></label>
                  <textarea v-model="item.shortfallReason" rows="2" placeholder="VD: R╞íi vß╗í, ß║⌐m mß╗æc, thiß║┐u h├áng..."
                    class="flex-1 px-3 py-2 border border-red-200 rounded-lg text-sm focus:ring-2 focus:ring-red-400/20 focus:border-red-400 outline-none bg-white resize-none"></textarea>
                </div>
              </div>
            </div>
          </div>

          <div class="px-6 py-4 border-t border-[#f1f5f9] flex justify-end gap-3 bg-[#f8f9fa]/50">
            <button @click="showConfirmModal = false"
              class="px-5 py-2.5 border border-[#e2e8f0] bg-white rounded-xl font-semibold text-[#364a63] text-sm hover:bg-[#f1f5f9] transition-all">
              Hß╗ºy
            </button>
            <button @click="submitConfirmTransfer" :disabled="submittingConfirm"
              class="px-6 py-2.5 bg-sky-500 hover:bg-sky-600 text-white rounded-xl font-bold text-sm transition-all disabled:opacity-60 flex items-center gap-2">
              <i class="fas fa-spinner fa-spin" v-if="submittingConfirm"></i>
              <i class="fas fa-check-double" v-else></i>
              X├íc nhß║¡n nhß║¡n h├áng
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <!-- DIRECT IMPORT MODAL (TH├èM Sß║óN PHß║¿M) -->
    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <AppModal 
      :show="showDirectImportModal" 
      title="Th├¬m sß║ún phß║⌐m (Tß║ío Phiß║┐u Nhß║¡p)" 
      size="md" 
      @close="showDirectImportModal = false"
    >
      <div class="p-6 space-y-4 text-sm">
        <!-- D├▓ng 1: Danh mß╗Ñc -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Danh mß╗Ñc</label>
          <select 
            v-model="directImportForm.categoryId" 
            @change="directImportForm.productId = ''; directImportForm.manufacturingDate = ''; directImportForm.expirationDate = ''"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all shadow-sm"
          >
            <option value="">-- Chß╗ìn danh mß╗Ñc --</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">
              {{ c.name }}
            </option>
          </select>
        </div>

        <!-- D├▓ng 2: Sß║ún phß║⌐m -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Sß║ún phß║⌐m</label>
          <select 
            v-model="directImportForm.productId" 
            :disabled="!directImportForm.categoryId"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all shadow-sm disabled:bg-[#f1f5f9] disabled:text-slate-400"
          >
            <option value="">-- Chß╗ìn sß║ún phß║⌐m --</option>
            <option v-for="p in filteredProductsForDirectImport" :key="p.id" :value="p.id">
              [{{ p.sku }}] {{ p.name }}
            </option>
          </select>
        </div>

        <!-- D├▓ng 3: ─É╞ín vß╗ï t├¡nh, Gi├í nhß║¡p & Gi├í b├ín -->
        <div class="grid grid-cols-3 gap-4">
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">─É╞ín vß╗ï t├¡nh</label>
            <input 
              :value="selectedProductInDirectImport ? selectedProductInDirectImport.unit : '-'" 
              type="text" 
              disabled 
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#eef2ff] text-[#4361ee] rounded-xl text-sm outline-none font-extrabold transition-all" 
            />
          </div>
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Gi├í nhß║¡p</label>
            <input 
              v-model.number="directImportForm.price" 
              type="number" 
              min="0"
              disabled
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none text-slate-500 font-semibold transition-all" 
            />
          </div>
          <div>
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Gi├í b├ín</label>
            <input 
              :value="selectedProductInDirectImport ? formatVND(selectedProductInDirectImport.price) : '-'" 
              type="text" 
              disabled 
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none text-slate-500 font-semibold transition-all" 
            />
          </div>
        </div>

        <!-- D├▓ng 4: M├ú l├┤ sß║ún xuß║Ñt & Sß╗æ l╞░ß╗úng nhß║¡p -->
        <div class="grid grid-cols-12 gap-4">
          <div class="col-span-8">
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">M├ú l├┤ sß║ún xuß║Ñt</label>
            <div class="flex gap-2">
              <select v-if="!directImportForm.isNewBatch && directImportBatches.length > 0" 
                v-model="directImportForm.batchCode" 
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all">
                <option value="">-- Chß╗ìn l├┤ --</option>
                <option v-for="b in directImportBatches" :key="b.batchCode" :value="b.batchCode">
                  {{ b.batchCode }} (Tß╗ôn: {{ b.quantity }})
                </option>
              </select>
              <input v-else 
                v-model="directImportForm.batchCode" 
                type="text" 
                placeholder="V├¡ dß╗Ñ: BATCH-01, MILK-2026..." 
                class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all" 
              />
              <button @click="directImportForm.isNewBatch = !directImportForm.isNewBatch; directImportForm.batchCode = ''" 
                class="px-3 rounded-xl border border-[#e2e8f0] bg-[#f8f9fa] text-[#4361ee] hover:bg-[#eef2ff] transition-all flex items-center justify-center font-bold text-xs"
                title="Chuyß╗ân ─æß╗òi nhß║¡p l├┤ mß╗¢i/c┼⌐">
                <i class="fas fa-sync-alt"></i>
              </button>
            </div>
          </div>
          <div class="col-span-4">
            <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Sß╗æ l╞░ß╗úng nhß║¡p</label>
            <input 
              v-model.number="directImportForm.quantity" 
              type="number" 
              min="1" 
              class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all" 
            />
          </div>
        </div>

        <!-- Ng├áy sß║ún xuß║Ñt (NSX) - Bß║»t buß╗Öc -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Ng├áy sß║ún xuß║Ñt (NSX)</label>
          <input 
            v-model="directImportForm.manufacturingDate" 
            type="date" 
            :disabled="isMfgDateLocked"
            class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all disabled:opacity-60 disabled:cursor-not-allowed" 
          />
        </div>
        <!-- Checkbox quß║ún l├╜ theo hß║ín d├╣ng -->
        <div class="flex items-center gap-2 py-1">
          <input 
            id="directImportHasExpiry" 
            v-model="directImportForm.hasExpiry" 
            type="checkbox" 
            :disabled="isExpDateLocked"
            class="w-5 h-5 accent-[#4361ee] cursor-pointer rounded-md border-slate-300 disabled:opacity-60 disabled:cursor-not-allowed" 
          />
          <label for="directImportHasExpiry" class="cursor-pointer select-none font-bold text-xs text-[#8094ae] uppercase tracking-wider">
            Sß║ún phß║⌐m quß║ún l├╜ theo hß║ín d├╣ng
          </label>
        </div>

        <!-- C├íc tr╞░ß╗¥ng hß║ín d├╣ng (chß╗ë hiß╗çn khi hasExpiry ─æ╞░ß╗úc t├¡ch chß╗ìn) -->
        <Transition name="fade">
          <div v-if="directImportForm.hasExpiry" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Hß║ín sß╗¡ dß╗Ñng (HSD)</label>
                <input 
                  v-model="directImportForm.expirationDate" 
                  type="date" 
                  :disabled="isExpDateLocked"
                  class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] font-semibold transition-all disabled:opacity-60 disabled:cursor-not-allowed" 
                />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Sß╗æ ng├áy cß║únh b├ío hß║ín d├╣ng</label>
                <input 
                  v-model.number="directImportForm.expiryWarningDays" 
                  type="number" 
                  min="1"
                  placeholder="Mß║╖c ─æß╗ïnh: 30" 
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
            Hß╗ºy bß╗Å
          </button>
          <button 
            class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold shadow-sm hover:shadow-md transition-all flex items-center justify-center gap-2" 
            :disabled="submittingDirectImport"
            @click="submitDirectImport"
          >
            <i v-if="submittingDirectImport" class="fas fa-spinner fa-spin"></i>
            X├íc nhß║¡n tß║ío Phiß║┐u
          </button>
        </div>
      </div>
    </AppModal>

    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <!-- EDIT RECEIPT MODAL -->
    <!-- ΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉΓòÉ -->
    <AppModal :show="showEditModal" @close="showEditModal = false" :title="editModalTitle">
      <div class="space-y-6 p-4 sm:p-5">
        <!-- L├╜ do chß╗ënh sß╗¡a -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">L├╜ do chß╗ënh sß╗¡a <span class="text-red-500">*</span></label>
          <textarea v-model="editForm.editReason" rows="2"
            placeholder="Nhß║¡p l├╜ do chß╗ënh sß╗¡a (bß║»t buß╗Öc)..."
            class="w-full px-4 py-3 border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white rounded-xl text-sm focus:ring-0 focus:border-[#4361ee] dark:focus:border-blue-500 outline-none resize-none transition-colors leading-relaxed"
          ></textarea>
        </div>

        <!-- Ghi ch├║ phiß║┐u -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-1.5">Ghi ch├║ phiß║┐u</label>
          <textarea v-model="editForm.description" rows="2"
            placeholder="Ghi ch├║ phiß║┐u (tuß╗│ chß╗ìn)..."
            class="w-full px-4 py-3 border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white rounded-xl text-sm focus:ring-0 focus:border-[#4361ee] dark:focus:border-blue-500 outline-none resize-none transition-colors leading-relaxed"
          ></textarea>
        </div>

        <!-- Danh s├ích sß║ún phß║⌐m -->
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase mb-2">Cß║¡p nhß║¡t sß╗æ l╞░ß╗úng</label>
          <div class="rounded-xl border-2 border-slate-200 dark:border-slate-700 overflow-hidden">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 dark:bg-slate-800/50">
                <tr>
                  <th class="px-5 py-3 text-left text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Sß║ún phß║⌐m</th>
                  <th class="px-5 py-3 text-center text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider w-28">Sß╗æ l╞░ß╗úng c┼⌐</th>
                  <th class="px-5 py-3 text-center text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider w-36">Sß╗æ l╞░ß╗úng mß╗¢i</th>
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

        <!-- Th├┤ng b├ío kß╗▒ n─âng cho Manager -->
        <div v-if="editMode === 'manager' && selectedReceipt?.status === 'DRAFT'"
          class="p-3 bg-orange-50 border border-orange-200 rounded-xl text-xs text-orange-700 flex items-start gap-2">
          <i class="fas fa-info-circle mt-0.5 flex-shrink-0"></i>
          <span>Sau khi l╞░u, phiß║┐u sß╕¥ c─æß╗òi sang trß║íng th├íi <strong>Chß╗¥ Staff x├íc nhß║¡n</strong>. Staff sß║╜ nhß║¡n th├┤ng b├ío v├á phß║úi x├íc nhß║¡n tr╞░ß╗¢c khi gß╗¡i lß║íi l├¬n bß║ín.</span>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2">
          <button @click="showEditModal = false"
            class="h-10 px-5 border border-[#e2e8f0] rounded-xl text-sm font-semibold text-[#8094ae] hover:bg-[#f8f9fa] transition-all">
            Hß╗ºy
          </button>
          <button @click="submitEditReceipt()" :disabled="submittingEdit"
            class="h-10 px-6 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-semibold shadow-sm hover:shadow-md transition-all flex items-center gap-2 disabled:opacity-60">
            <i class="fas fa-save" v-if="!submittingEdit"></i>
            <i class="fas fa-spinner fa-spin" v-else></i>
            {{ editMode === 'manager' && selectedReceipt?.status === 'DRAFT' ? 'L╞░u & Gß╗¡i xuß╗æng Staff' : 'L╞░u thay ─æß╗òi' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- ΓöÇΓöÇ Modal Hß╗ºy Phiß║┐u ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ -->
    <Transition name="fade">
      <div v-if="showCancelModal" class="fixed inset-0 z-[120] flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4" @click.stop>
        <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-xl w-full max-w-md overflow-hidden border border-slate-200 dark:border-slate-700" @click.stop>
          <div class="bg-red-50 dark:bg-red-900/20 px-6 py-4 border-b border-red-100 dark:border-red-900/30 flex items-center justify-between">
            <h3 class="text-base font-bold text-red-700 dark:text-red-400 flex items-center gap-2">
              <i class="fas fa-exclamation-triangle"></i>
              Hß╗ºy phiß║┐u {{ receiptToCancel?.code }}
            </h3>
            <button @click="showCancelModal = false" class="text-red-400 hover:text-red-600 dark:hover:text-red-300 transition-colors">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="p-6">
            <p class="text-sm text-slate-600 dark:text-slate-300 mb-4">H├ánh ─æß╗Öng n├áy kh├┤ng thß╗â ho├án t├íc. Vui l├▓ng ghi r├╡ l├╜ do hß╗ºy phiß║┐u b├¬n d╞░ß╗¢i ─æß╗â l╞░u vß║┐t hß╗ç thß╗æng:</p>
            <textarea v-model="cancelReason" rows="3"
              class="w-full p-3 text-sm border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white rounded-xl focus:border-red-500 focus:ring-1 focus:ring-red-500 outline-none placeholder:text-slate-400 dark:placeholder:text-slate-500 transition-all"
              placeholder="Nhß║¡p l├╜ do hß╗ºy phiß║┐u..."></textarea>
          </div>
          <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-100 dark:border-slate-700 flex items-center justify-end gap-3">
            <button @click="showCancelModal = false" class="px-5 py-2.5 text-sm font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 bg-slate-100 dark:bg-slate-800/80 rounded-xl transition-colors">
              ─É├│ng
            </button>
            <button @click="executeCancelReceipt" class="px-5 py-2.5 text-sm font-bold text-white bg-red-500 hover:bg-red-600 rounded-xl shadow-sm shadow-red-500/20 transition-all flex items-center gap-2">
              <i class="fas fa-trash"></i> X├íc nhß║¡n hß╗ºy
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
