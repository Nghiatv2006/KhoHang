<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')

const branches = ref<any[]>([])
const bLoading = ref(true)
const bSearch = ref('')

const filteredBranches = computed(() => {
  if (!bSearch.value.trim()) return branches.value
  const kw = bSearch.value.toLowerCase()
  return branches.value.filter(b => 
    b.name?.toLowerCase().includes(kw) || 
    b.address?.toLowerCase().includes(kw) ||
    b.taxCode?.toLowerCase().includes(kw) ||
    b.managerName?.toLowerCase().includes(kw)
  )
})

const showModal = ref(false)
const editingB = ref<any>(null)
const form = reactive({ name: '', address: '', lowStockThreshold: 5, taxCode: '' })
const saving = ref(false)
const showDelete = ref(false)
const deletingB = ref<any>(null)

function openAdd() { editingB.value = null; Object.assign(form, { name: '', address: '', lowStockThreshold: 5, taxCode: '' }); showModal.value = true }
function openEdit(b: any) { editingB.value = b; Object.assign(form, { name: b.name, address: b.address, lowStockThreshold: b.lowStockThreshold, taxCode: b.taxCode || '' }); showModal.value = true }
function confirmDelete(b: any) { deletingB.value = b; showDelete.value = true }

async function saveBranch() {
  if (!form.name?.trim() || !form.address?.trim()) { toast.error('Tên và địa chỉ là bắt buộc.'); return }
  if (!form.taxCode?.trim()) { toast.error('Mã số thuế là bắt buộc.'); return }
  if (!/^[0-9A-Za-z-]{10,13}$/.test(form.taxCode.trim())) {
    toast.error('Mã số thuế không hợp lệ (phải từ 10 đến 13 ký tự).')
    return
  }
  saving.value = true
  try {
    const payload = { 
      name: form.name.trim(), 
      address: form.address.trim(), 
      lowStockThreshold: Number(form.lowStockThreshold),
      taxCode: form.taxCode.trim()
    }
    const res = editingB.value
      ? await api.put(`/api/branches/${editingB.value.id}`, payload)
      : await api.post('/api/branches', payload)
    const data = await res.json()
    if (res.ok) { toast.success(editingB.value ? 'Cập nhật chi nhánh thành công!' : 'Thêm chi nhánh thành công!'); showModal.value = false; await loadBranches() }
    else toast.error(data.message || 'Có lỗi xảy ra.')
  } catch { toast.error('Không thể kết nối.') }
  finally { saving.value = false }
}

async function doDelete() {
  if (!deletingB.value) return
  try {
    const res = await api.delete(`/api/branches/${deletingB.value.id}`)
    const data = await res.json()
    if (res.ok) { toast.success('Xóa chi nhánh thành công!'); await loadBranches() }
    else toast.error(data.message || 'Không thể xóa. ' + (data.message || ''))
  } catch { toast.error('Có lỗi xảy ra.') }
  finally { showDelete.value = false }
}

async function loadBranches() {
  bLoading.value = true
  try { const res = await api.get('/api/branches'); if (res.ok) branches.value = await res.json() }
  catch {} finally { bLoading.value = false }
}

onMounted(loadBranches)

const branchThemes = [
  {
    // Forest Green (Rừng xanh)
    cardBg: 'bg-gradient-to-br from-[#f0fdf4] to-[#dcfce7] border-[#86efac] hover:shadow-[0_15px_30px_rgba(22,163,74,0.18)] hover:border-[#16a34a] hover:-translate-y-1.5',
    iconBg: 'bg-white border border-[#86efac] text-[#16a34a]',
    titleColor: 'text-[#15803d]',
    descBg: 'bg-white border border-[#dcfce7]',
    topBar: 'from-[#16a34a] to-[#4ade80]',
    warnBg: 'bg-[#dcfce7] border border-[#86efac] text-[#15803d]',
    icon: 'fas fa-leaf',
    watermark: 'fas fa-leaf text-[#16a34a]/10 group-hover:rotate-12 group-hover:scale-110'
  },
  {
    // Earthy Wood / Warm Sand (Đất ấm & Gỗ)
    cardBg: 'bg-gradient-to-br from-[#fffaf5] to-[#f7ebe1] border-[#e3cbb8] hover:shadow-[0_15px_30px_rgba(133,77,14,0.15)] hover:border-[#854d0e] hover:-translate-y-1.5',
    iconBg: 'bg-white border border-[#e3cbb8] text-[#854d0e]',
    titleColor: 'text-[#713f12]',
    descBg: 'bg-white border border-[#f7ebe1]',
    topBar: 'from-[#854d0e] to-[#b45309]',
    warnBg: 'bg-[#f7ebe1] border border-[#e3cbb8] text-[#713f12]',
    icon: 'fas fa-seedling',
    watermark: 'fas fa-seedling text-[#854d0e]/10 group-hover:-rotate-12 group-hover:scale-110'
  },
  {
    // Ocean Blue (Biển cả)
    cardBg: 'bg-gradient-to-br from-[#f0f9ff] to-[#e0f2fe] border-[#7dd3fc] hover:shadow-[0_15px_30px_rgba(3,105,161,0.18)] hover:border-[#0284c7] hover:-translate-y-1.5',
    iconBg: 'bg-white border border-[#7dd3fc] text-[#0284c7]',
    titleColor: 'text-[#075985]',
    descBg: 'bg-white border border-[#e0f2fe]',
    topBar: 'from-[#0284c7] to-[#0ea5e9]',
    warnBg: 'bg-[#e0f2fe] border border-[#7dd3fc] text-[#075985]',
    icon: 'fas fa-tint',
    watermark: 'fas fa-tint text-[#0284c7]/10 group-hover:scale-125 group-hover:rotate-12'
  },
  {
    // Sunset Rose (Hoàng hôn hoa lá)
    cardBg: 'bg-gradient-to-br from-[#fff5f6] to-[#ffdce0] border-[#fda4af] hover:shadow-[0_15px_30px_rgba(225,29,72,0.18)] hover:border-[#e11d48] hover:-translate-y-1.5',
    iconBg: 'bg-white border border-[#fda4af] text-[#e11d48]',
    titleColor: 'text-[#be123c]',
    descBg: 'bg-white border border-[#ffdce0]',
    topBar: 'from-[#e11d48] to-[#fb7185]',
    warnBg: 'bg-[#ffdce0] border border-[#fda4af] text-[#be123c]',
    icon: 'fas fa-sun',
    watermark: 'fas fa-sun text-[#e11d48]/10 group-hover:rotate-90 group-hover:scale-110'
  }
]

function getBranchTheme(idx: number) {
  return branchThemes[idx % branchThemes.length]
}
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto">
    <!-- Header & Toolbar -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0">Quản lý Chi nhánh</h2>
        <p class="text-[#8094ae] text-sm mt-1">Danh sách cửa hàng, địa điểm và cài đặt tồn kho</p>
      </div>
      
      <div class="flex items-center gap-3">
        <div class="relative w-[300px]">
          <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
          <input v-model="bSearch" type="text" placeholder="Tìm theo tên, địa chỉ..." class="w-full h-[42px] pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] shadow-sm" />
        </div>
        <button v-if="isAdmin" class="bg-[#4361ee] text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2 h-[42px]" @click="openAdd">
          <i class="fas fa-plus"></i> Thêm chi nhánh
        </button>
      </div>
    </div>

    <!-- Cards grid -->
    <div v-if="bLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div v-for="i in 4" :key="i" class="h-[180px] bg-white rounded-[16px] border border-[#f1f5f9] shadow-sm animate-pulse" />
    </div>

    <div v-else-if="filteredBranches.length === 0" class="bg-white rounded-[16px] shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] py-20 text-center text-[#8094ae]">
      <i class="fas fa-store-slash text-5xl mb-4 opacity-40"></i>
      <div class="font-bold text-[#364a63]">Không tìm thấy chi nhánh nào</div>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div
        v-for="(b, index) in filteredBranches"
        :key="b.id"
        :class="['rounded-[16px] p-6 transition-all duration-300 group relative overflow-hidden flex flex-col cursor-pointer select-none branch-card branch-card-enter shadow-[0_8px_30px_rgba(0,0,0,0.06)] border-2', getBranchTheme(index).cardBg, `theme-${index % 4}`]"
        :style="{ animationDelay: `${index * 50}ms` }"
        @dblclick="isAdmin ? openEdit(b) : null"
      >
        <!-- Decorative top bar -->
        <div :class="['absolute top-0 left-0 w-full h-1 bg-gradient-to-r transition-opacity', getBranchTheme(index).topBar, b.isHead ? 'opacity-100' : 'opacity-0 group-hover:opacity-100']"></div>
        
        <!-- Header -->
        <div class="flex items-start justify-between mb-5 relative z-10">
          <div class="flex items-center gap-3">
            <div :class="['w-12 h-12 rounded-xl flex items-center justify-center border shadow-sm transition-all duration-300 group-hover:scale-110 group-hover:rotate-6 relative z-10 store-icon-circle', getBranchTheme(index).iconBg]">
              <i :class="[getBranchTheme(index).icon, 'text-xl']"></i>
            </div>
            <div>
              <div :class="['font-bold text-lg leading-tight transition-colors card-title', getBranchTheme(index).titleColor]">{{ b.name }}</div>
              <div class="text-xs font-mono text-[#8094ae] mt-1">ID: #{{ b.id }}</div>
              <div v-if="b.isHead" class="mt-1.5 inline-flex items-center gap-1 bg-white/80 text-[#f59e0b] text-[10px] font-bold px-2 py-0.5 rounded-md border border-[#ffecb3] shadow-sm is-head-badge">
                <i class="fas fa-crown text-[9px] text-[#f59e0b]"></i> CHI NHÁNH TỔNG
              </div>
            </div>
          </div>
          <div v-if="isAdmin" class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity bg-white/80 backdrop-blur rounded-lg p-1">
            <button class="w-8 h-8 rounded-lg text-[#0ea5e9] hover:bg-[#e0f2fe] flex items-center justify-center transition-colors cursor-pointer" @click.stop="openEdit(b)" title="Sửa">
              <i class="fas fa-pen text-sm"></i>
            </button>
            <button v-if="!b.isHead" class="w-8 h-8 rounded-lg text-[#ea4f52] hover:bg-[#ffe4e6] flex items-center justify-center transition-colors cursor-pointer" @click.stop="confirmDelete(b)" title="Xóa">
              <i class="fas fa-trash text-sm"></i>
            </button>
          </div>
        </div>


        <!-- Info -->
        <div class="space-y-3 flex-1 flex flex-col justify-between relative z-10">
          <div class="space-y-2">
            <div :class="['flex items-start gap-3 p-2.5 rounded-xl border info-capsule', getBranchTheme(index).descBg]">
              <i class="fas fa-map-marker-alt text-[#8094ae] mt-1 flex-shrink-0"></i>
              <span class="text-sm text-[#526484] leading-relaxed line-clamp-2 w-full" :title="b.address">{{ b.address || 'Chưa cập nhật địa chỉ' }}</span>
            </div>

            <div :class="['flex items-start gap-3 p-2.5 rounded-xl border info-capsule', getBranchTheme(index).descBg]">
              <i class="fas fa-file-invoice-dollar text-[#8094ae] mt-1 flex-shrink-0"></i>
              <div>
                <div class="text-[10px] text-[#8094ae] font-bold uppercase tracking-wider">Mã số thuế</div>
                <span class="text-xs text-[#364a63] font-mono leading-relaxed font-semibold">{{ b.taxCode || 'Chưa cập nhật' }}</span>
              </div>
            </div>

            <div :class="['flex items-start gap-3 p-2.5 rounded-xl border info-capsule', getBranchTheme(index).descBg]">
              <i class="fas fa-user-shield text-[#8094ae] mt-1 flex-shrink-0"></i>
              <div>
                <div class="text-[10px] text-[#8094ae] font-bold uppercase tracking-wider">Người phụ trách</div>
                <span class="text-xs text-[#364a63] font-semibold leading-relaxed">{{ b.managerName || 'Chưa phân công' }}</span>
              </div>
            </div>
          </div>
          
          <div class="flex items-center justify-between border-t border-black/5 pt-3 mt-auto">
            <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wider">Ngưỡng cảnh báo</div>
            <div :class="['flex items-center gap-1.5 px-2.5 py-1 rounded-md border warning-badge', getBranchTheme(index).warnBg]">
              <i class="fas fa-exclamation-triangle text-[10px]"></i>
              <span class="font-bold text-sm">{{ b.lowStockThreshold }}</span>
            </div>
          </div>
        </div>

        <!-- Watermark Nature Background Icon -->
        <i :class="['absolute -bottom-6 -right-6 text-9xl transition-all duration-1000 pointer-events-none z-0 nature-watermark', getBranchTheme(index).watermark]"></i>
      </div>
    </div>

    <!-- Modal -->
    <AppModal :show="showModal" :title="editingB ? 'Sửa thông tin chi nhánh' : 'Thêm chi nhánh mới'" size="sm" @close="showModal = false">
      <div class="p-6 space-y-5">
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Tên chi nhánh <span class="text-[#ea4f52]">*</span></label>
          <input v-model="form.name" type="text" placeholder="VD: Chi nhánh Hà Nội" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] transition-all" />
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mã số thuế <span class="text-[#ea4f52]">*</span></label>
          <input v-model="form.taxCode" type="text" placeholder="Mã số thuế (10 - 13 chữ số)" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] transition-all" />
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Địa chỉ <span class="text-[#ea4f52]">*</span></label>
          <textarea v-model="form.address" rows="3" placeholder="Địa chỉ đầy đủ..." class="w-full px-4 py-3 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] resize-none transition-all"></textarea>
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Ngưỡng tồn kho tối thiểu</label>
          <div class="relative">
            <i class="fas fa-boxes absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input v-model="form.lowStockThreshold" type="number" min="0" placeholder="5" class="w-full h-11 pl-11 pr-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] transition-all" />
          </div>
          <p class="text-[11px] text-[#8094ae] mt-2 italic"><i class="fas fa-info-circle mr-1"></i>Hệ thống sẽ cảnh báo khi tồn kho dưới ngưỡng này.</p>
        </div>
        <div class="flex gap-3 pt-4 border-t border-[#f1f5f9]">
          <button class="flex-1 h-11 bg-[#f8f9fa] hover:bg-[#e2e8f0] text-[#364a63] rounded-xl text-sm font-bold transition-colors" @click="showModal = false">Hủy bỏ</button>
          <button class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold shadow-sm hover:shadow-md flex items-center justify-center gap-2 transition-all" :disabled="saving" @click="saveBranch">
            <i v-if="saving" class="fas fa-spinner fa-spin"></i>
            {{ saving ? 'Đang lưu...' : 'Lưu thông tin' }}
          </button>
        </div>
      </div>
    </AppModal>


    <ConfirmDialog :show="showDelete" title="Xóa chi nhánh" :message="`Bạn có chắc muốn xóa chi nhánh '${deletingB?.name}'? Thao tác không thể hoàn tác và sẽ thất bại nếu còn dữ liệu liên kết (nhân viên, sản phẩm).`" confirm-text="Xóa" :danger="true" @confirm="doDelete" @cancel="showDelete = false" />
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }

.branch-card {
  opacity: 0;
  transform: translate3d(0, 100vh, 0);
  will-change: transform, opacity;
}

.branch-card-enter {
  animation: slideUpBranch 0.9s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes slideUpBranch {
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

/* ── Dark Mode Overrides for Nature Cards ── */
html.dark-mode .branch-card {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4) !important;
}

/* Theme 0 (Forest) Dark Mode */
html.dark-mode .branch-card.theme-0 {
  background: linear-gradient(135deg, rgba(6, 47, 21, 0.4) 0%, rgba(30, 41, 59, 0.8) 100%) !important;
  border-color: rgba(34, 197, 94, 0.3) !important;
}
html.dark-mode .branch-card.theme-0:hover {
  border-color: #22c55e !important;
  box-shadow: 0 15px 30px rgba(34, 197, 94, 0.2) !important;
}
html.dark-mode .theme-0 .store-icon-circle {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(34, 197, 94, 0.3) !important;
  color: #4ade80 !important;
}
html.dark-mode .theme-0 .card-title {
  color: #4ade80 !important;
}
html.dark-mode .theme-0 .info-capsule {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(34, 197, 94, 0.15) !important;
}
html.dark-mode .theme-0 .warning-badge {
  background-color: rgba(34, 197, 94, 0.15) !important;
  border-color: rgba(34, 197, 94, 0.3) !important;
  color: #4ade80 !important;
}
html.dark-mode .theme-0 .nature-watermark {
  color: rgba(34, 197, 94, 0.28) !important;
}

/* Theme 1 (Earth) Dark Mode */
html.dark-mode .branch-card.theme-1 {
  background: linear-gradient(135deg, rgba(69, 26, 3, 0.3) 0%, rgba(30, 41, 59, 0.8) 100%) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
}
html.dark-mode .branch-card.theme-1:hover {
  border-color: #f59e0b !important;
  box-shadow: 0 15px 30px rgba(245, 158, 11, 0.15) !important;
}
html.dark-mode .theme-1 .store-icon-circle {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
  color: #fcd34d !important;
}
html.dark-mode .theme-1 .card-title {
  color: #fcd34d !important;
}
html.dark-mode .theme-1 .info-capsule {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(245, 158, 11, 0.15) !important;
}
html.dark-mode .theme-1 .warning-badge {
  background-color: rgba(245, 158, 11, 0.15) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
  color: #fcd34d !important;
}
html.dark-mode .theme-1 .nature-watermark {
  color: rgba(245, 158, 11, 0.25) !important;
}

/* Theme 2 (Ocean) Dark Mode */
html.dark-mode .branch-card.theme-2 {
  background: linear-gradient(135deg, rgba(3, 73, 124, 0.3) 0%, rgba(30, 41, 59, 0.8) 100%) !important;
  border-color: rgba(14, 165, 233, 0.3) !important;
}
html.dark-mode .branch-card.theme-2:hover {
  border-color: #0284c7 !important;
  box-shadow: 0 15px 30px rgba(14, 165, 233, 0.2) !important;
}
html.dark-mode .theme-2 .store-icon-circle {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(14, 165, 233, 0.3) !important;
  color: #38bdf8 !important;
}
html.dark-mode .theme-2 .card-title {
  color: #38bdf8 !important;
}
html.dark-mode .theme-2 .info-capsule {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(14, 165, 233, 0.15) !important;
}
html.dark-mode .theme-2 .warning-badge {
  background-color: rgba(14, 165, 233, 0.15) !important;
  border-color: rgba(14, 165, 233, 0.3) !important;
  color: #38bdf8 !important;
}
html.dark-mode .theme-2 .nature-watermark {
  color: rgba(14, 165, 233, 0.28) !important;
}

/* Theme 3 (Sunset) Dark Mode */
html.dark-mode .branch-card.theme-3 {
  background: linear-gradient(135deg, rgba(159, 18, 57, 0.3) 0%, rgba(30, 41, 59, 0.8) 100%) !important;
  border-color: rgba(244, 63, 94, 0.3) !important;
}
html.dark-mode .branch-card.theme-3:hover {
  border-color: #e11d48 !important;
  box-shadow: 0 15px 30px rgba(244, 63, 94, 0.2) !important;
}
html.dark-mode .theme-3 .store-icon-circle {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(244, 63, 94, 0.3) !important;
  color: #fda4af !important;
}
html.dark-mode .theme-3 .card-title {
  color: #fda4af !important;
}
html.dark-mode .theme-3 .info-capsule {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border-color: rgba(244, 63, 94, 0.15) !important;
}
html.dark-mode .theme-3 .warning-badge {
  background-color: rgba(244, 63, 94, 0.15) !important;
  border-color: rgba(244, 63, 94, 0.3) !important;
  color: #fda4af !important;
}
html.dark-mode .theme-3 .nature-watermark {
  color: rgba(244, 63, 94, 0.28) !important;
}

/* General Dark Mode Capsule text adjustments */
html.dark-mode .info-capsule span {
  color: #f1f5f9 !important;
}
html.dark-mode .info-capsule div {
  color: #94a3b8 !important;
}
html.dark-mode .is-head-badge {
  background-color: rgba(30, 41, 59, 0.8) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
}
</style>
