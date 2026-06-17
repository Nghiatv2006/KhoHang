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
  return branches.value.filter(b => b.name?.toLowerCase().includes(kw) || b.address?.toLowerCase().includes(kw))
})

const showModal = ref(false)
const editingB = ref<any>(null)
const form = reactive({ name: '', address: '', lowStockThreshold: 5 })
const saving = ref(false)
const showDelete = ref(false)
const deletingB = ref<any>(null)

function openAdd() { editingB.value = null; Object.assign(form, { name: '', address: '', lowStockThreshold: 5 }); showModal.value = true }
function openEdit(b: any) { editingB.value = b; Object.assign(form, { name: b.name, address: b.address, lowStockThreshold: b.lowStockThreshold }); showModal.value = true }
function confirmDelete(b: any) { deletingB.value = b; showDelete.value = true }

async function saveBranch() {
  if (!form.name?.trim() || !form.address?.trim()) { toast.error('Tên và địa chỉ là bắt buộc.'); return }
  saving.value = true
  try {
    const payload = { name: form.name.trim(), address: form.address.trim(), lowStockThreshold: Number(form.lowStockThreshold) }
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
        v-for="b in filteredBranches"
        :key="b.id"
        class="bg-white rounded-[16px] shadow-[0_2px_10px_rgba(0,0,0,0.02)] border border-[#f1f5f9] p-6 hover:-translate-y-1 hover:shadow-lg hover:border-[#e2e8f0] transition-all duration-300 group relative overflow-hidden flex flex-col"
      >
        <!-- Decorative top bar -->
        <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#4361ee] to-[#3a0ca3] opacity-0 group-hover:opacity-100 transition-opacity"></div>
        
        <!-- Header -->
        <div class="flex items-start justify-between mb-5 relative z-10">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-xl bg-[#eef2ff] text-[#4361ee] flex items-center justify-center border border-[#dbeafe] shadow-sm">
              <i class="fas fa-store text-xl"></i>
            </div>
            <div>
              <div class="font-bold text-[#364a63] text-lg leading-tight">{{ b.name }}</div>
              <div class="text-xs font-mono text-[#8094ae] mt-1">ID: #{{ b.id }}</div>
            </div>
          </div>
          <div v-if="isAdmin" class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity bg-white/80 backdrop-blur rounded-lg p-1">
            <button class="w-8 h-8 rounded-lg text-[#0ea5e9] hover:bg-[#e0f2fe] flex items-center justify-center transition-colors cursor-pointer" @click="openEdit(b)" title="Sửa">
              <i class="fas fa-pen text-sm"></i>
            </button>
            <button class="w-8 h-8 rounded-lg text-[#ea4f52] hover:bg-[#ffe4e6] flex items-center justify-center transition-colors cursor-pointer" @click="confirmDelete(b)" title="Xóa">
              <i class="fas fa-trash text-sm"></i>
            </button>
          </div>
        </div>

        <!-- Info -->
        <div class="space-y-4 flex-1 flex flex-col justify-between">
          <div class="flex items-start gap-3 bg-[#f8f9fa] p-3 rounded-xl border border-[#f1f5f9]">
            <i class="fas fa-map-marker-alt text-[#8094ae] mt-1 flex-shrink-0"></i>
            <span class="text-sm text-[#526484] leading-relaxed">{{ b.address || 'Chưa cập nhật địa chỉ' }}</span>
          </div>
          
          <div class="flex items-center justify-between border-t border-[#f1f5f9] pt-4 mt-auto">
            <div class="text-xs font-bold text-[#8094ae] uppercase tracking-wider">Ngưỡng cảnh báo</div>
            <div class="flex items-center gap-1.5 bg-[#fff8e6] px-2.5 py-1 rounded-md border border-[#ffecb3]">
              <i class="fas fa-exclamation-triangle text-[#f59e0b] text-[10px]"></i>
              <span class="font-bold text-[#b45309] text-sm">{{ b.lowStockThreshold }}</span>
            </div>
          </div>
        </div>
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
</style>
