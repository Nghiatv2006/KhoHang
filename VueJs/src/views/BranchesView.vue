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
  <div class="space-y-4">
    <!-- Toolbar -->
    <div class="flex items-center gap-3">
      <div class="relative flex-1 min-w-48">
        <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">search</span>
        <input v-model="bSearch" type="text" placeholder="Tìm theo tên, địa chỉ..." class="w-full h-10 pl-10 pr-4 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 bg-white" />
      </div>
      <button v-if="isAdmin" class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm" @click="openAdd">
        <span class="material-symbols-outlined text-base">add</span> Thêm chi nhánh
      </button>
    </div>

    <!-- Cards grid -->
    <div v-if="bLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="i in 3" :key="i" class="h-40 bg-white rounded-2xl border border-slate-100 animate-pulse" />
    </div>

    <div v-else-if="filteredBranches.length === 0" class="bg-white rounded-2xl shadow-sm border border-slate-100 py-20 text-center text-slate-400">
      <span class="material-symbols-outlined text-5xl block mb-3 opacity-40">store</span>
      <div class="font-medium">Không có chi nhánh nào</div>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="b in filteredBranches"
        :key="b.id"
        class="bg-white rounded-2xl shadow-sm border border-slate-100 p-5 card-hover group"
      >
        <!-- Header -->
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-11 h-11 rounded-xl bg-[#0052cc]/10 flex items-center justify-center">
              <span class="material-symbols-outlined text-[#0052cc] text-xl"
                style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">store</span>
            </div>
            <div>
              <div class="font-semibold text-slate-800">{{ b.name }}</div>
              <div class="text-xs text-slate-400">#{{ b.id }}</div>
            </div>
          </div>
          <div v-if="isAdmin" class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button class="w-8 h-8 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 flex items-center justify-center transition-colors" @click="openEdit(b)">
              <span class="material-symbols-outlined text-base">edit</span>
            </button>
            <button class="w-8 h-8 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors" @click="confirmDelete(b)">
              <span class="material-symbols-outlined text-base">delete</span>
            </button>
          </div>
        </div>

        <!-- Info -->
        <div class="space-y-2">
          <div class="flex items-start gap-2 text-sm text-slate-600">
            <span class="material-symbols-outlined text-slate-400 text-base flex-shrink-0 mt-0.5">location_on</span>
            <span class="leading-5">{{ b.address }}</span>
          </div>
          <div class="flex items-center gap-2 text-sm">
            <span class="material-symbols-outlined text-amber-400 text-base">warning</span>
            <span class="text-slate-600">Ngưỡng tồn kho tối thiểu:</span>
            <span class="font-semibold text-amber-600">{{ b.lowStockThreshold }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <AppModal :show="showModal" :title="editingB ? 'Sửa chi nhánh' : 'Thêm chi nhánh'" size="sm" @close="showModal = false">
      <div class="p-6 space-y-4">
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Tên chi nhánh <span class="text-red-500">*</span></label>
          <input v-model="form.name" type="text" placeholder="VD: Chi nhánh Hà Nội" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Địa chỉ <span class="text-red-500">*</span></label>
          <textarea v-model="form.address" rows="2" placeholder="Địa chỉ đầy đủ..." class="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 resize-none" />
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Ngưỡng tồn kho tối thiểu</label>
          <input v-model="form.lowStockThreshold" type="number" min="0" placeholder="5" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          <p class="text-xs text-slate-400 mt-1">Hệ thống sẽ cảnh báo khi tồn kho dưới ngưỡng này.</p>
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="saving" @click="saveBranch">
            <span v-if="saving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ saving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </AppModal>

    <ConfirmDialog :show="showDelete" title="Xóa chi nhánh" :message="`Xóa chi nhánh '${deletingB?.name}'? Thao tác không thể hoàn tác và sẽ thất bại nếu còn dữ liệu liên kết.`" confirm-text="Xóa" :danger="true" @confirm="doDelete" @cancel="showDelete = false" />
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }
</style>
