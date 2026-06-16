<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import StatusBadge from '../components/StatusBadge.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

const activeTab = ref<'suppliers' | 'customers'>('suppliers')

// ── SUPPLIERS ──────────────────────────────────────────────────────────────
const suppliers = ref<any[]>([])
const sLoading = ref(true)
const sSearch = ref('')

const filteredSuppliers = computed(() => {
  if (!sSearch.value.trim()) return suppliers.value
  const kw = sSearch.value.toLowerCase()
  return suppliers.value.filter(s =>
    s.name?.toLowerCase().includes(kw) ||
    s.email?.toLowerCase().includes(kw) ||
    s.phone?.includes(kw)
  )
})

const showSModal = ref(false)
const editingS = ref<any>(null)
const sForm = reactive({ name: '', email: '', phone: '', address: '', taxCode: '' })
const sSaving = ref(false)
const showDeleteS = ref(false)
const deletingS = ref<any>(null)
const showDebtModal = ref(false)
const debtTarget = ref<any>(null)
const debtAmount = ref<any>('')
const debtSaving = ref(false)

function openAddS() { editingS.value = null; Object.assign(sForm, { name: '', email: '', phone: '', address: '', taxCode: '' }); showSModal.value = true }
function openEditS(s: any) { editingS.value = s; Object.assign(sForm, { name: s.name, email: s.email || '', phone: s.phone || '', address: s.address || '', taxCode: s.taxCode || '' }); showSModal.value = true }
function openDebt(s: any) { debtTarget.value = s; debtAmount.value = ''; showDebtModal.value = true }
function confirmDeleteS(s: any) { deletingS.value = s; showDeleteS.value = true }

async function saveSupplier() {
  if (!sForm.name?.trim()) { toast.error('Tên nhà cung cấp là bắt buộc.'); return }
  sSaving.value = true
  try {
    const payload = { name: sForm.name.trim(), email: sForm.email, phone: sForm.phone, address: sForm.address, taxCode: sForm.taxCode }
    const res = editingS.value
      ? await api.put(`/api/suppliers/${editingS.value.id}`, payload)
      : await api.post('/api/suppliers', payload)
    const data = await res.json()
    if (res.ok) { toast.success(editingS.value ? 'Cập nhật thành công!' : 'Thêm NCC thành công!'); showSModal.value = false; await loadSuppliers() }
    else toast.error(data.message || 'Có lỗi xảy ra.')
  } catch { toast.error('Không thể kết nối.') }
  finally { sSaving.value = false }
}

async function doDeleteS() {
  if (!deletingS.value) return
  try {
    const res = await api.delete(`/api/suppliers/${deletingS.value.id}`)
    const data = await res.json()
    if (res.ok) { toast.success('Xóa thành công!'); await loadSuppliers() }
    else toast.error(data.message || 'Không thể xóa.')
  } catch { toast.error('Có lỗi.') }
  finally { showDeleteS.value = false }
}

async function toggleSupplier(s: any) {
  try {
    const res = await api.patch(`/api/suppliers/${s.id}/toggle-status`, {})
    const data = await res.json()
    if (res.ok) { toast.success('Cập nhật trạng thái thành công!'); await loadSuppliers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
}

async function adjustDebt() {
  if (!debtAmount.value) { toast.error('Vui lòng nhập số tiền.'); return }
  debtSaving.value = true
  try {
    const res = await api.patch(`/api/suppliers/${debtTarget.value.id}/adjust-debt`, { amount: Number(debtAmount.value) })
    const data = await res.json()
    if (res.ok) { toast.success('Điều chỉnh công nợ thành công!'); showDebtModal.value = false; await loadSuppliers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
  finally { debtSaving.value = false }
}

// ── CUSTOMERS ──────────────────────────────────────────────────────────────
const customers = ref<any[]>([])
const cLoading = ref(true)
const cSearch = ref('')

const filteredCustomers = computed(() => {
  if (!cSearch.value.trim()) return customers.value
  const kw = cSearch.value.toLowerCase()
  return customers.value.filter(c =>
    c.name?.toLowerCase().includes(kw) ||
    c.email?.toLowerCase().includes(kw) ||
    c.phone?.includes(kw)
  )
})

const showCModal = ref(false)
const editingC = ref<any>(null)
const cForm = reactive({ name: '', email: '', phone: '', address: '', taxCode: '' })
const cSaving = ref(false)
const showDeleteC = ref(false)
const deletingC = ref<any>(null)
const showCDebtModal = ref(false)
const cDebtTarget = ref<any>(null)
const cDebtAmount = ref<any>('')
const cDebtSaving = ref(false)

function openAddC() { editingC.value = null; Object.assign(cForm, { name: '', email: '', phone: '', address: '', taxCode: '' }); showCModal.value = true }
function openEditC(c: any) { editingC.value = c; Object.assign(cForm, { name: c.name, email: c.email || '', phone: c.phone || '', address: c.address || '', taxCode: c.taxCode || '' }); showCModal.value = true }
function openCDebt(c: any) { cDebtTarget.value = c; cDebtAmount.value = ''; showCDebtModal.value = true }
function confirmDeleteC(c: any) { deletingC.value = c; showDeleteC.value = true }

async function saveCustomer() {
  if (!cForm.name?.trim()) { toast.error('Tên khách hàng là bắt buộc.'); return }
  cSaving.value = true
  try {
    const payload = { name: cForm.name.trim(), email: cForm.email, phone: cForm.phone, address: cForm.address, taxCode: cForm.taxCode }
    const res = editingC.value
      ? await api.put(`/api/customers/${editingC.value.id}`, payload)
      : await api.post('/api/customers', payload)
    const data = await res.json()
    if (res.ok) { toast.success(editingC.value ? 'Cập nhật thành công!' : 'Thêm khách hàng thành công!'); showCModal.value = false; await loadCustomers() }
    else toast.error(data.message || 'Có lỗi xảy ra.')
  } catch { toast.error('Không thể kết nối.') }
  finally { cSaving.value = false }
}

async function doDeleteC() {
  if (!deletingC.value) return
  try {
    const res = await api.delete(`/api/customers/${deletingC.value.id}`)
    const data = await res.json()
    if (res.ok) { toast.success('Xóa thành công!'); await loadCustomers() }
    else toast.error(data.message || 'Không thể xóa.')
  } catch { toast.error('Có lỗi.') }
  finally { showDeleteC.value = false }
}

async function toggleCustomer(c: any) {
  try {
    const res = await api.patch(`/api/customers/${c.id}/toggle-status`, {})
    const data = await res.json()
    if (res.ok) { toast.success('Cập nhật trạng thái thành công!'); await loadCustomers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
}

async function adjustCDebt() {
  if (!cDebtAmount.value) { toast.error('Vui lòng nhập số tiền.'); return }
  cDebtSaving.value = true
  try {
    const res = await api.patch(`/api/customers/${cDebtTarget.value.id}/adjust-debt`, { amount: Number(cDebtAmount.value) })
    const data = await res.json()
    if (res.ok) { toast.success('Điều chỉnh công nợ thành công!'); showCDebtModal.value = false; await loadCustomers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
  finally { cDebtSaving.value = false }
}

// Load
async function loadSuppliers() {
  sLoading.value = true
  try { const res = await api.get('/api/suppliers'); if (res.ok) suppliers.value = await res.json() }
  catch {} finally { sLoading.value = false }
}
async function loadCustomers() {
  cLoading.value = true
  try { const res = await api.get('/api/customers'); if (res.ok) customers.value = await res.json() }
  catch {} finally { cLoading.value = false }
}

onMounted(() => { loadSuppliers(); loadCustomers() })

function formatCurrency(val: any) {
  if (!val && val !== 0) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val)
}
</script>

<template>
  <div class="space-y-4">
    <!-- Tabs -->
    <div class="flex items-center gap-1 bg-white rounded-xl p-1 border border-slate-100 shadow-sm w-fit">
      <button
        v-for="tab in [{ key: 'suppliers', label: 'Nhà cung cấp', icon: 'local_shipping' }, { key: 'customers', label: 'Khách hàng', icon: 'shopping_bag' }]"
        :key="tab.key"
        :class="['flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-medium transition-all', activeTab === tab.key ? 'bg-[#0052cc] text-white shadow' : 'text-slate-500 hover:text-slate-700']"
        @click="activeTab = tab.key as any"
      >
        <span class="material-symbols-outlined text-base">{{ tab.icon }}</span>
        {{ tab.label }}
      </button>
    </div>

    <!-- SUPPLIERS -->
    <template v-if="activeTab === 'suppliers'">
      <div class="flex items-center gap-3">
        <div class="relative flex-1 min-w-48">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">search</span>
          <input v-model="sSearch" type="text" placeholder="Tìm nhà cung cấp..." class="w-full h-10 pl-10 pr-4 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 bg-white" />
        </div>
        <button v-if="isManager" class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm" @click="openAddS">
          <span class="material-symbols-outlined text-base">add</span> Thêm NCC
        </button>
      </div>

      <div class="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div v-if="sLoading" class="p-6 space-y-3"><div v-for="i in 4" :key="i" class="h-14 bg-slate-50 rounded-xl animate-pulse" /></div>
        <div v-else-if="filteredSuppliers.length === 0" class="py-16 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl block mb-2 opacity-40">local_shipping</span>
          Chưa có nhà cung cấp nào
        </div>
        <table v-else class="w-full text-sm">
          <thead><tr class="border-b border-slate-100 bg-slate-50">
            <th class="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase">Nhà cung cấp</th>
            <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Liên hệ</th>
            <th class="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Công nợ</th>
            <th class="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Trạng thái</th>
            <th v-if="isManager" class="px-6 py-3" />
          </tr></thead>
          <tbody>
            <tr v-for="s in filteredSuppliers" :key="s.id" class="border-b border-slate-50 hover:bg-slate-50/80 transition-colors">
              <td class="px-6 py-4">
                <div class="font-semibold text-slate-800">{{ s.name }}</div>
                <div class="text-xs text-slate-400 mt-0.5">{{ s.taxCode || '—' }}</div>
              </td>
              <td class="px-4 py-4">
                <div class="text-slate-700">{{ s.phone || '—' }}</div>
                <div class="text-xs text-slate-400">{{ s.email || '—' }}</div>
              </td>
              <td class="px-4 py-4 text-right">
                <span :class="['font-semibold font-mono', (s.debt ?? 0) > 0 ? 'text-red-600' : 'text-slate-600']">
                  {{ formatCurrency(s.debt ?? 0) }}
                </span>
              </td>
              <td class="px-4 py-4 text-center">
                <StatusBadge :value="s.status" type="status" />
              </td>
              <td v-if="isManager" class="px-6 py-4">
                <div class="flex items-center justify-end gap-1">
                  <button class="w-8 h-8 rounded-lg hover:bg-amber-50 text-slate-400 hover:text-amber-600 flex items-center justify-center transition-colors" title="Điều chỉnh công nợ" @click="openDebt(s)">
                    <span class="material-symbols-outlined text-base">payments</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center transition-colors" @click="toggleSupplier(s)">
                    <span class="material-symbols-outlined text-base">{{ s.status === 'ACTIVE' ? 'toggle_on' : 'toggle_off' }}</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 flex items-center justify-center transition-colors" @click="openEditS(s)">
                    <span class="material-symbols-outlined text-base">edit</span>
                  </button>
                  <button v-if="isAdmin" class="w-8 h-8 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors" @click="confirmDeleteS(s)">
                    <span class="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- CUSTOMERS -->
    <template v-if="activeTab === 'customers'">
      <div class="flex items-center gap-3">
        <div class="relative flex-1 min-w-48">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">search</span>
          <input v-model="cSearch" type="text" placeholder="Tìm khách hàng..." class="w-full h-10 pl-10 pr-4 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 bg-white" />
        </div>
        <button v-if="isManager" class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm" @click="openAddC">
          <span class="material-symbols-outlined text-base">add</span> Thêm KH
        </button>
      </div>

      <div class="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div v-if="cLoading" class="p-6 space-y-3"><div v-for="i in 4" :key="i" class="h-14 bg-slate-50 rounded-xl animate-pulse" /></div>
        <div v-else-if="filteredCustomers.length === 0" class="py-16 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl block mb-2 opacity-40">shopping_bag</span>
          Chưa có khách hàng nào
        </div>
        <table v-else class="w-full text-sm">
          <thead><tr class="border-b border-slate-100 bg-slate-50">
            <th class="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase">Khách hàng</th>
            <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Liên hệ</th>
            <th class="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Công nợ</th>
            <th class="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Trạng thái</th>
            <th v-if="isManager" class="px-6 py-3" />
          </tr></thead>
          <tbody>
            <tr v-for="c in filteredCustomers" :key="c.id" class="border-b border-slate-50 hover:bg-slate-50/80 transition-colors">
              <td class="px-6 py-4">
                <div class="font-semibold text-slate-800">{{ c.name }}</div>
                <div class="text-xs text-slate-400 mt-0.5">{{ c.taxCode || '—' }}</div>
              </td>
              <td class="px-4 py-4">
                <div class="text-slate-700">{{ c.phone || '—' }}</div>
                <div class="text-xs text-slate-400">{{ c.email || '—' }}</div>
              </td>
              <td class="px-4 py-4 text-right">
                <span :class="['font-semibold font-mono', (c.debt ?? 0) > 0 ? 'text-amber-600' : 'text-slate-600']">
                  {{ formatCurrency(c.debt ?? 0) }}
                </span>
              </td>
              <td class="px-4 py-4 text-center"><StatusBadge :value="c.status" type="status" /></td>
              <td v-if="isManager" class="px-6 py-4">
                <div class="flex items-center justify-end gap-1">
                  <button class="w-8 h-8 rounded-lg hover:bg-amber-50 text-slate-400 hover:text-amber-600 flex items-center justify-center transition-colors" title="Điều chỉnh công nợ" @click="openCDebt(c)">
                    <span class="material-symbols-outlined text-base">payments</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center transition-colors" @click="toggleCustomer(c)">
                    <span class="material-symbols-outlined text-base">{{ c.status === 'ACTIVE' ? 'toggle_on' : 'toggle_off' }}</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 flex items-center justify-center transition-colors" @click="openEditC(c)">
                    <span class="material-symbols-outlined text-base">edit</span>
                  </button>
                  <button v-if="isAdmin" class="w-8 h-8 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors" @click="confirmDeleteC(c)">
                    <span class="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- Supplier Modal -->
    <AppModal :show="showSModal" :title="editingS ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp'" @close="showSModal = false">
      <div class="p-6 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2">
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Tên nhà cung cấp <span class="text-red-500">*</span></label>
            <input v-model="sForm.name" type="text" placeholder="Tên..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          </div>
          <div><label class="block text-xs font-semibold text-slate-600 mb-1.5">Email</label><input v-model="sForm.email" type="email" placeholder="email@..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
          <div><label class="block text-xs font-semibold text-slate-600 mb-1.5">SĐT</label><input v-model="sForm.phone" type="text" placeholder="0900..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
          <div><label class="block text-xs font-semibold text-slate-600 mb-1.5">Mã số thuế</label><input v-model="sForm.taxCode" type="text" placeholder="MST..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 font-mono" /></div>
          <div class="col-span-2"><label class="block text-xs font-semibold text-slate-600 mb-1.5">Địa chỉ</label><input v-model="sForm.address" type="text" placeholder="Địa chỉ..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showSModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="sSaving" @click="saveSupplier">
            <span v-if="sSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ sSaving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Customer Modal -->
    <AppModal :show="showCModal" :title="editingC ? 'Sửa khách hàng' : 'Thêm khách hàng'" @close="showCModal = false">
      <div class="p-6 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2"><label class="block text-xs font-semibold text-slate-600 mb-1.5">Tên khách hàng <span class="text-red-500">*</span></label><input v-model="cForm.name" type="text" placeholder="Tên..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
          <div><label class="block text-xs font-semibold text-slate-600 mb-1.5">Email</label><input v-model="cForm.email" type="email" placeholder="email@..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
          <div><label class="block text-xs font-semibold text-slate-600 mb-1.5">SĐT</label><input v-model="cForm.phone" type="text" placeholder="0900..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
          <div><label class="block text-xs font-semibold text-slate-600 mb-1.5">Mã số thuế</label><input v-model="cForm.taxCode" type="text" placeholder="MST..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 font-mono" /></div>
          <div class="col-span-2"><label class="block text-xs font-semibold text-slate-600 mb-1.5">Địa chỉ</label><input v-model="cForm.address" type="text" placeholder="Địa chỉ..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" /></div>
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showCModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="cSaving" @click="saveCustomer">
            <span v-if="cSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ cSaving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Debt modals -->
    <AppModal :show="showDebtModal" title="Điều chỉnh công nợ NCC" size="sm" @close="showDebtModal = false">
      <div class="p-6 space-y-4">
        <div class="bg-slate-50 rounded-xl px-4 py-3">
          <div class="text-xs text-slate-500">Nhà cung cấp</div>
          <div class="font-semibold text-slate-800">{{ debtTarget?.name }}</div>
          <div class="text-sm text-red-600 font-mono mt-1">Nợ hiện tại: {{ formatCurrency(debtTarget?.debt ?? 0) }}</div>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Số tiền điều chỉnh (VNĐ)</label>
          <input v-model="debtAmount" type="number" placeholder="VD: -500000 để giảm, 200000 để tăng" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 font-mono" />
          <p class="text-xs text-slate-400 mt-1">Nhập số âm để giảm nợ, số dương để tăng nợ.</p>
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showDebtModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="debtSaving" @click="adjustDebt">
            <span v-if="debtSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ debtSaving ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </AppModal>

    <AppModal :show="showCDebtModal" title="Điều chỉnh công nợ KH" size="sm" @close="showCDebtModal = false">
      <div class="p-6 space-y-4">
        <div class="bg-slate-50 rounded-xl px-4 py-3">
          <div class="text-xs text-slate-500">Khách hàng</div>
          <div class="font-semibold text-slate-800">{{ cDebtTarget?.name }}</div>
          <div class="text-sm text-amber-600 font-mono mt-1">Nợ hiện tại: {{ formatCurrency(cDebtTarget?.debt ?? 0) }}</div>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Số tiền điều chỉnh (VNĐ)</label>
          <input v-model="cDebtAmount" type="number" placeholder="VD: -500000 để giảm, 200000 để tăng" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 font-mono" />
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showCDebtModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="cDebtSaving" @click="adjustCDebt">
            <span v-if="cDebtSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ cDebtSaving ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </AppModal>

    <ConfirmDialog :show="showDeleteS" title="Xóa nhà cung cấp" :message="`Xóa '${deletingS?.name}'?`" confirm-text="Xóa" :danger="true" @confirm="doDeleteS" @cancel="showDeleteS = false" />
    <ConfirmDialog :show="showDeleteC" title="Xóa khách hàng" :message="`Xóa '${deletingC?.name}'?`" confirm-text="Xóa" :danger="true" @confirm="doDeleteC" @cancel="showDeleteC = false" />
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }
</style>
