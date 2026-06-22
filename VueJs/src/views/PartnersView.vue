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



// ── CUSTOMERS ──────────────────────────────────────────────────────────────
const customers = ref<any[]>([])
const cLoading = ref(true)
const cSearch = ref('')
const cStatusFilter = ref('')

const filteredCustomers = computed(() => {
  let list = customers.value
  if (cStatusFilter.value) {
    list = list.filter(c => c.status === cStatusFilter.value)
  }
  if (!cSearch.value.trim()) return list
  const kw = cSearch.value.toLowerCase()
  return list.filter(c =>
    c.name?.toLowerCase().includes(kw) ||
    c.email?.toLowerCase().includes(kw) ||
    c.contactInfo?.toLowerCase().includes(kw)
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
function openEditC(c: any) { editingC.value = c; Object.assign(cForm, { name: c.name, email: c.email || '', phone: c.contactInfo || '', address: c.address || '', taxCode: c.taxCode || '' }); showCModal.value = true }
function openCDebt(c: any) { cDebtTarget.value = c; cDebtAmount.value = ''; showCDebtModal.value = true }
function confirmDeleteC(c: any) { deletingC.value = c; showDeleteC.value = true }

async function saveCustomer() {
  if (!cForm.name?.trim()) { toast.error('Tên khách hàng là bắt buộc.'); return }
  cSaving.value = true
  try {
    const payload = { name: cForm.name.trim(), email: cForm.email, contactInfo: cForm.phone, address: cForm.address, taxCode: cForm.taxCode }
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
  
  let amount = Number(cDebtAmount.value);
  const currentDebt = cDebtTarget.value?.debt || 0;
  
  // Tự động giới hạn số tiền giảm (số âm) không vượt quá nợ hiện tại
  if (amount < 0 && Math.abs(amount) > currentDebt) {
    amount = -currentDebt;
  }
  
  cDebtSaving.value = true
  try {
    const res = await api.patch(`/api/customers/${cDebtTarget.value.id}/adjust-debt`, { amount })
    const data = await res.json()
    if (res.ok) { toast.success('Điều chỉnh công nợ thành công!'); showCDebtModal.value = false; await loadCustomers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
  finally { cDebtSaving.value = false }
}

async function loadCustomers() {
  cLoading.value = true
  try { const res = await api.get('/api/customers'); if (res.ok) customers.value = await res.json() }
  catch {} finally { cLoading.value = false }
}

function handleDebtInput() {
  if (cDebtAmount.value && cDebtTarget.value) {
    let val = Number(cDebtAmount.value);
    const maxDebt = cDebtTarget.value.debt || 0;
    if (val < 0 && Math.abs(val) > maxDebt) {
      cDebtAmount.value = -maxDebt;
    }
  }
}

onMounted(() => { loadCustomers() })

function formatCurrency(val: any) {
  if (!val && val !== 0) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val)
}
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto">
    <!-- Header & Tabs -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0">Quản lý Khách hàng</h2>
        <p class="text-[#8094ae] text-sm mt-1">Danh sách Khách hàng của hệ thống</p>
      </div>
    </div>

    <!-- CUSTOMERS -->
    <!-- CUSTOMERS -->
    <div class="bg-slate-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#f4bd0e] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] flex items-center justify-between flex-wrap gap-4 bg-[#f8f9fa]/50">
        <div class="flex items-center gap-3 w-full md:w-auto flex-1">
          <div class="relative min-w-[300px] flex-1 md:flex-none">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input v-model="cSearch" type="text" placeholder="Tìm kiếm khách hàng..." class="w-full h-[42px] pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
          </div>
          <select v-model="cStatusFilter" class="h-[42px] px-3 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
        </div>
        <button v-if="isManager" class="bg-[#4361ee] text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2" @click="openAddC">
          <i class="fas fa-plus"></i> Thêm Khách hàng
        </button>
      </div>

      <!-- Table -->
      <div class="overflow-x-auto">
        <div v-if="cLoading" class="p-8 space-y-4"><div v-for="i in 4" :key="i" class="h-12 bg-[#f8f9fa] rounded-xl animate-pulse" /></div>
        <div v-else-if="filteredCustomers.length === 0" class="py-20 text-center text-[#8094ae]">
          <i class="fas fa-users-slash text-5xl mb-4 opacity-40"></i>
          <div class="font-bold text-[#364a63]">Chưa có khách hàng nào</div>
        </div>
        <table v-else class="w-full text-left border-collapse">
          <thead class="bg-white">
            <tr>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Khách hàng</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Liên hệ</th>
              <th class="p-4 text-right text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Công nợ</th>
              <th class="p-4 text-center text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Trạng thái</th>
              <th v-if="isManager" class="p-4 border-b border-[#f1f5f9] w-[180px]"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in filteredCustomers" :key="c.id" class="border-b border-[#f1f5f9] hover:border-transparent hover:bg-gradient-to-r hover:from-[#4361ee]/15 hover:to-[#4cc9f0]/15 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]" @dblclick="isManager ? openEditC(c) : null">
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="font-bold text-[#364a63]">{{ c.name }}</div>
                <div class="text-xs text-[#8094ae] font-mono mt-0.5">MST: {{ c.taxCode || '—' }}</div>
              </td>
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="text-[#364a63] font-medium">{{ c.contactInfo || '—' }}</div>
                <div class="text-xs text-[#8094ae]">{{ c.email || '—' }}</div>
              </td>
              <td class="p-4 text-right first:rounded-l-xl last:rounded-r-xl">
                <span :class="['font-bold font-mono', (c.debt ?? 0) > 0 ? 'text-[#f4bd0e]' : 'text-[#8094ae]']">
                  {{ formatCurrency(c.debt ?? 0) }}
                </span>
              </td>
              <td class="p-4 text-center first:rounded-l-xl last:rounded-r-xl"><StatusBadge :value="c.status" type="status" /></td>
              <td v-if="isManager" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                  <button class="w-8 h-8 rounded-lg text-[#f4bd0e] bg-white hover:bg-[#fef9c3] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" title="Điều chỉnh công nợ" @click.stop="openCDebt(c)">
                    <i class="fas fa-hand-holding-dollar text-sm"></i>
                  </button>
                  <button class="w-8 h-8 rounded-lg text-[#8094ae] bg-white hover:bg-[#e2e8f0] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" title="Đổi trạng thái" @click.stop="toggleCustomer(c)">
                    <i :class="['fas text-sm', c.status === 'ACTIVE' ? 'fa-toggle-on text-[#05b171]' : 'fa-toggle-off text-[#8094ae]']"></i>
                  </button>
                  <button class="w-8 h-8 rounded-lg text-[#0ea5e9] bg-white hover:bg-[#e0f2fe] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" title="Sửa" @click.stop="openEditC(c)">
                    <i class="fas fa-pen text-sm"></i>
                  </button>
                  <button v-if="isAdmin" class="w-8 h-8 rounded-lg text-[#ea4f52] bg-white hover:bg-[#ffe4e6] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" title="Xóa" @click.stop="confirmDeleteC(c)">
                    <i class="fas fa-trash text-sm"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── CUSTOMER RIGHT PANEL ── -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showCModal" @click="showCModal = false" class="fixed inset-0 bg-slate-900/20 backdrop-blur-[2px] z-[100]"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showCModal" class="fixed inset-y-0 right-0 z-[101] w-[450px] bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] flex flex-col border-l border-[#e2e8f0]">
          <!-- Header -->
          <div class="px-6 py-5 border-b border-[#f1f5f9] flex justify-between items-center bg-gradient-to-r from-[#f8fafc] to-white">
            <h3 class="font-bold text-[#364a63] text-lg flex items-center gap-2">
              <i class="fas fa-users text-[#f4bd0e]"></i>
              {{ editingC ? 'Sửa Khách hàng' : 'Thêm Khách hàng' }}
            </h3>
            <button @click="showCModal = false" class="text-[#8094ae] hover:text-[#ea4f52] transition-colors w-8 h-8 flex items-center justify-center rounded-full hover:bg-red-50">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- Body -->
          <div class="p-6 flex-1 overflow-y-auto space-y-5 custom-scrollbar">
            <div class="grid grid-cols-2 gap-5">
              <div class="col-span-2"><label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Tên khách hàng <span class="text-[#ea4f52]">*</span></label><input v-model="cForm.name" type="text" placeholder="Nguyễn Văn A..." class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#f4bd0e]/20 focus:border-[#f4bd0e] outline-none transition-all text-[#364a63]" /></div>
              <div><label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Email</label><input v-model="cForm.email" type="email" placeholder="email@..." class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#f4bd0e]/20 focus:border-[#f4bd0e] outline-none transition-all text-[#364a63]" /></div>
              <div><label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Số điện thoại</label><input v-model="cForm.phone" type="text" placeholder="0900..." class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#f4bd0e]/20 focus:border-[#f4bd0e] outline-none transition-all text-[#364a63]" /></div>
              <div><label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mã số thuế</label><input v-model="cForm.taxCode" type="text" placeholder="MST..." class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#f4bd0e]/20 focus:border-[#f4bd0e] outline-none transition-all font-mono text-[#364a63]" /></div>
              <div class="col-span-2"><label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Địa chỉ</label><input v-model="cForm.address" type="text" placeholder="Địa chỉ..." class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#f4bd0e]/20 focus:border-[#f4bd0e] outline-none transition-all text-[#364a63]" /></div>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="p-6 border-t border-[#f1f5f9] bg-[#f8fafc] flex gap-3">
            <button class="flex-1 h-11 bg-white border border-[#e2e8f0] hover:bg-[#f8f9fa] text-[#364a63] rounded-xl text-sm font-bold transition-colors shadow-sm" @click="showCModal = false">Hủy bỏ</button>
            <button class="flex-1 h-11 bg-[#f4bd0e] hover:bg-[#d9a80c] text-white rounded-xl text-sm font-bold transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-2" :disabled="cSaving" @click="saveCustomer">
              <i v-if="cSaving" class="fas fa-spinner fa-spin"></i>
              {{ cSaving ? 'Đang lưu...' : 'Lưu thông tin' }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <AppModal :show="showCDebtModal" title="Điều chỉnh công nợ KH" size="sm" @close="showCDebtModal = false">
      <div class="p-6 space-y-5">
        <div class="bg-[#f8f9fa] rounded-xl px-5 py-4 border border-[#e2e8f0]">
          <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wider">Khách hàng</div>
          <div class="font-bold text-[#364a63] text-lg mt-1">{{ cDebtTarget?.name }}</div>
          <div class="text-sm font-mono mt-2" :class="(cDebtTarget?.debt ?? 0) > 0 ? 'text-[#f4bd0e]' : 'text-[#05b171]'">Nợ hiện tại: {{ formatCurrency(cDebtTarget?.debt ?? 0) }}</div>
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Số tiền điều chỉnh (VNĐ)</label>
          <input v-model="cDebtAmount" @input="handleDebtInput" type="number" placeholder="VD: -500000 để giảm, 200000 để tăng" class="w-full h-11 px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all font-mono text-[#364a63]" />
          <p class="text-xs text-[#8094ae] mt-2 bg-[#eef2ff] text-[#4361ee] p-2 rounded-lg"><i class="fas fa-info-circle mr-1"></i> Nhập số âm để giảm nợ, số dương để tăng nợ.</p>
        </div>
        <div class="flex gap-3 pt-2">
          <button class="flex-1 h-11 bg-[#f8f9fa] hover:bg-[#e2e8f0] text-[#364a63] rounded-xl text-sm font-bold transition-colors" @click="showCDebtModal = false">Hủy</button>
          <button class="flex-1 h-11 bg-[#f4bd0e] hover:bg-[#d9a80c] text-white rounded-xl text-sm font-bold shadow-sm hover:shadow-md flex items-center justify-center gap-2 transition-all" :disabled="cDebtSaving" @click="adjustCDebt">
            <i v-if="cDebtSaving" class="fas fa-spinner fa-spin"></i>
            {{ cDebtSaving ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </AppModal>


    <ConfirmDialog :show="showDeleteC" title="Xóa khách hàng" :message="`Bạn có chắc muốn xóa '${deletingC?.name}'? Hành động này không thể hoàn tác.`" confirm-text="Xóa" :danger="true" @confirm="doDeleteC" @cancel="showDeleteC = false" />
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }

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
