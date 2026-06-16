<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import StatusBadge from '../components/StatusBadge.vue'

const toast = useToast()
const currentUser = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(currentUser.value?.role))

const activeTab = ref<'users' | 'transfers'>('users')

// ── USERS ──────────────────────────────────────────────────────────────────
const users = ref<any[]>([])
const branches = ref<any[]>([])
const uLoading = ref(true)
const uSearch = ref('')
const uRoleFilter = ref('')
const uStatusFilter = ref('')

const filteredUsers = computed(() => {
  let list = users.value
  if (uSearch.value.trim()) {
    const kw = uSearch.value.toLowerCase()
    list = list.filter(u => u.fullName?.toLowerCase().includes(kw) || u.username?.toLowerCase().includes(kw))
  }
  if (uRoleFilter.value) list = list.filter(u => u.role === uRoleFilter.value)
  if (uStatusFilter.value) list = list.filter(u => u.status === uStatusFilter.value)
  return list
})

const showUserModal = ref(false)
const editingUser = ref<any>(null)
const userForm = reactive({ username: '', fullName: '', password: '', role: 'STAFF', branchId: '' as any, status: 'ACTIVE' })
const uSaving = ref(false)
const showDeleteUser = ref(false)
const deletingUser = ref<any>(null)

function openAddUser() {
  editingUser.value = null
  Object.assign(userForm, { username: '', fullName: '', password: '', role: 'STAFF', branchId: '', status: 'ACTIVE' })
  showUserModal.value = true
}
function openEditUser(u: any) {
  editingUser.value = u
  Object.assign(userForm, { username: u.username, fullName: u.fullName, password: '', role: u.role, branchId: u.branchId || '', status: u.status })
  showUserModal.value = true
}
function confirmDeleteUser(u: any) { deletingUser.value = u; showDeleteUser.value = true }

async function saveUser() {
  if (!userForm.fullName?.trim()) { toast.error('Họ tên là bắt buộc.'); return }
  if (!editingUser.value && !userForm.password) { toast.error('Mật khẩu là bắt buộc khi tạo mới.'); return }
  uSaving.value = true
  try {
    const payload: any = {
      username: userForm.username,
      fullName: userForm.fullName.trim(),
      role: userForm.role,
      branchId: userForm.branchId || null,
      status: userForm.status,
    }
    if (userForm.password) payload.password = userForm.password
    const res = editingUser.value
      ? await api.put(`/api/users/${editingUser.value.id}`, payload)
      : await api.post('/api/users', payload)
    const data = await res.json()
    if (res.ok) { toast.success(editingUser.value ? 'Cập nhật thành công!' : 'Tạo tài khoản thành công!'); showUserModal.value = false; await loadUsers() }
    else toast.error(data.message || 'Có lỗi xảy ra.')
  } catch { toast.error('Không thể kết nối.') }
  finally { uSaving.value = false }
}

async function doDeleteUser() {
  if (!deletingUser.value) return
  try {
    const res = await api.delete(`/api/users/${deletingUser.value.id}`)
    const data = await res.json()
    if (res.ok) { toast.success('Xóa người dùng thành công!'); await loadUsers() }
    else toast.error(data.message || 'Không thể xóa.')
  } catch { toast.error('Có lỗi.') }
  finally { showDeleteUser.value = false }
}

async function toggleUser(u: any) {
  try {
    const res = await api.patch(`/api/users/${u.id}/toggle-status`, {})
    const data = await res.json()
    if (res.ok) { toast.success('Cập nhật trạng thái thành công!'); await loadUsers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
}

// ── TRANSFER REQUESTS ──────────────────────────────────────────────────────
const transfers = ref<any[]>([])
const tLoading = ref(true)
const showTransferModal = ref(false)
const transferForm = reactive({ staffId: '' as any, targetBranchId: '' as any, reason: '' })
const tSaving = ref(false)

async function loadTransfers() {
  tLoading.value = true
  try { const res = await api.get('/api/users/transfer-requests'); if (res.ok) transfers.value = await res.json() }
  catch {} finally { tLoading.value = false }
}

async function createTransfer() {
  if (!transferForm.staffId || !transferForm.targetBranchId) { toast.error('Vui lòng điền đầy đủ thông tin.'); return }
  tSaving.value = true
  try {
    const res = await api.post('/api/users/transfer-requests', {
      staffId: Number(transferForm.staffId),
      targetBranchId: Number(transferForm.targetBranchId),
      reason: transferForm.reason,
    })
    const data = await res.json()
    if (res.ok) { toast.success('Tạo yêu cầu điều chuyển thành công!'); showTransferModal.value = false; await loadTransfers() }
    else toast.error(data.message || 'Có lỗi xảy ra.')
  } catch { toast.error('Không thể kết nối.') }
  finally { tSaving.value = false }
}

async function approveTransfer(id: number) {
  try {
    const res = await api.post(`/api/users/transfer-requests/${id}/approve`, {})
    const data = await res.json()
    if (res.ok) { toast.success('Đã phê duyệt yêu cầu!'); await loadTransfers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
}

async function rejectTransfer(id: number) {
  try {
    const res = await api.post(`/api/users/transfer-requests/${id}/reject`, {})
    const data = await res.json()
    if (res.ok) { toast.success('Đã từ chối yêu cầu!'); await loadTransfers() }
    else toast.error(data.message || 'Có lỗi.')
  } catch { toast.error('Có lỗi.') }
}

// Load
async function loadUsers() {
  uLoading.value = true
  try { const res = await api.get('/api/users'); if (res.ok) users.value = await res.json() }
  catch {} finally { uLoading.value = false }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadTransfers()])
  try { const res = await api.get('/api/branches'); if (res.ok) branches.value = await res.json() } catch {}
})



function formatDate(dt: string) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<template>
  <div class="space-y-4">
    <!-- Tabs -->
    <div class="flex items-center gap-1 bg-white rounded-xl p-1 border border-slate-100 shadow-sm w-fit">
      <button
        v-for="tab in [{ key: 'users', label: 'Nhân viên', icon: 'group' }, { key: 'transfers', label: 'Điều chuyển', icon: 'swap_horiz' }]"
        :key="tab.key"
        :class="['flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-medium transition-all', activeTab === tab.key ? 'bg-[#0052cc] text-white shadow' : 'text-slate-500 hover:text-slate-700']"
        @click="activeTab = tab.key as any"
      >
        <span class="material-symbols-outlined text-base">{{ tab.icon }}</span>
        {{ tab.label }}
        <span v-if="tab.key === 'transfers' && transfers.filter(t => ['PENDING_STAFF','STAFF_CONFIRMED','MANAGER_APPROVED'].includes(t.status)).length > 0"
          class="bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
          {{ transfers.filter(t => ['PENDING_STAFF','STAFF_CONFIRMED','MANAGER_APPROVED'].includes(t.status)).length }}
        </span>
      </button>
    </div>

    <!-- USERS TAB -->
    <template v-if="activeTab === 'users'">
      <div class="flex items-center gap-3 flex-wrap">
        <div class="relative flex-1 min-w-48">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">search</span>
          <input v-model="uSearch" type="text" placeholder="Tìm theo tên, username..." class="w-full h-10 pl-10 pr-4 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 bg-white" />
        </div>
        <select v-model="uRoleFilter" class="h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white text-slate-700">
          <option value="">Tất cả vai trò</option>
          <option value="ADMIN">Admin</option>
          <option value="MANAGER">Manager</option>
          <option value="STAFF">Nhân viên</option>
        </select>
        <select v-model="uStatusFilter" class="h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white text-slate-700">
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Ngừng HĐ</option>
        </select>
        <button v-if="isAdmin" class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm" @click="openAddUser">
          <span class="material-symbols-outlined text-base">person_add</span> Thêm nhân viên
        </button>
      </div>

      <div class="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div v-if="uLoading" class="p-6 space-y-3"><div v-for="i in 5" :key="i" class="h-14 bg-slate-50 rounded-xl animate-pulse" /></div>
        <div v-else-if="filteredUsers.length === 0" class="py-16 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl block mb-2 opacity-40">group</span>
          Không có nhân viên nào
        </div>
        <table v-else class="w-full text-sm">
          <thead><tr class="border-b border-slate-100 bg-slate-50">
            <th class="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase">Nhân viên</th>
            <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Vai trò</th>
            <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Chi nhánh</th>
            <th class="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Trạng thái</th>
            <th v-if="isAdmin" class="px-6 py-3" />
          </tr></thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id" class="border-b border-slate-50 hover:bg-slate-50/80 transition-colors">
              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-[#0052cc]/10 flex items-center justify-center text-[#0052cc] font-bold text-sm flex-shrink-0">
                    {{ u.fullName?.charAt(0) || '?' }}
                  </div>
                  <div>
                    <div class="font-semibold text-slate-800">{{ u.fullName }}</div>
                    <div class="text-xs text-slate-400 font-mono">@{{ u.username }}</div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-4"><StatusBadge :value="u.role" type="role" /></td>
              <td class="px-4 py-4 text-slate-600"><span v-if="u.branchName">{{ u.branchName }}</span><span v-else class="text-slate-300">Chưa phân công</span></td>
              <td class="px-4 py-4 text-center"><StatusBadge :value="u.status" type="status" /></td>
              <td v-if="isAdmin" class="px-6 py-4">
                <div class="flex items-center justify-end gap-1">
                  <button class="w-8 h-8 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center transition-colors" @click="toggleUser(u)" :title="u.status === 'ACTIVE' ? 'Vô hiệu hóa' : 'Kích hoạt'">
                    <span class="material-symbols-outlined text-base">{{ u.status === 'ACTIVE' ? 'toggle_on' : 'toggle_off' }}</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 flex items-center justify-center transition-colors" @click="openEditUser(u)">
                    <span class="material-symbols-outlined text-base">edit</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors" @click="confirmDeleteUser(u)">
                    <span class="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!uLoading && filteredUsers.length > 0" class="px-6 py-3 border-t border-slate-100 text-xs text-slate-400">
          {{ filteredUsers.length }} nhân viên
        </div>
      </div>
    </template>

    <!-- TRANSFERS TAB -->
    <template v-if="activeTab === 'transfers'">
      <div class="flex justify-end">
        <button v-if="isManager" class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm" @click="showTransferModal = true">
          <span class="material-symbols-outlined text-base">swap_horiz</span> Tạo yêu cầu điều chuyển
        </button>
      </div>

      <!-- Timeline-style transfer list -->
      <div v-if="tLoading" class="p-6 space-y-3 bg-white rounded-2xl"><div v-for="i in 3" :key="i" class="h-24 bg-slate-50 rounded-xl animate-pulse" /></div>
      <div v-else-if="transfers.length === 0" class="bg-white rounded-2xl shadow-sm border border-slate-100 py-16 text-center text-slate-400">
        <span class="material-symbols-outlined text-5xl block mb-2 opacity-40">swap_horiz</span>
        Không có yêu cầu điều chuyển nào
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="t in transfers"
          :key="t.id"
          class="bg-white rounded-2xl shadow-sm border border-slate-100 p-5"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap mb-2">
                <span class="font-semibold text-slate-800">{{ t.staffName || `Nhân viên #${t.staffId}` }}</span>
                <span class="material-symbols-outlined text-slate-400 text-base">arrow_forward</span>
                <span class="font-semibold text-[#0052cc]">{{ t.targetBranchName || `Chi nhánh #${t.targetBranchId}` }}</span>
                <StatusBadge :value="t.status" type="transfer" />
              </div>
              <div class="text-sm text-slate-500 mb-1">{{ t.reason || 'Không có lý do' }}</div>
              <div class="text-xs text-slate-400 flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">schedule</span>
                {{ formatDate(t.createdAt) }}
                <span v-if="t.fromBranchName" class="ml-2">Từ: {{ t.fromBranchName }}</span>
              </div>
              <!-- 3-step progress -->
              <div class="flex items-center gap-2 mt-3">
                <div v-for="(step, i) in [
                  { label: 'NV xác nhận', done: ['STAFF_CONFIRMED','MANAGER_APPROVED','APPROVED'].includes(t.status) },
                  { label: 'Manager duyệt', done: ['MANAGER_APPROVED','APPROVED'].includes(t.status) },
                  { label: 'Admin phê duyệt', done: t.status === 'APPROVED' },
                ]" :key="i" class="flex items-center gap-1">
                  <div :class="['w-5 h-5 rounded-full flex items-center justify-center text-xs', step.done ? 'bg-emerald-500' : t.status === 'REJECTED' || t.status === 'CANCELLED' ? 'bg-red-100' : 'bg-slate-100']">
                    <span class="material-symbols-outlined text-xs" :class="step.done ? 'text-white' : 'text-slate-400'">
                      {{ step.done ? 'check' : t.status === 'REJECTED' || t.status === 'CANCELLED' ? 'close' : 'circle' }}
                    </span>
                  </div>
                  <span class="text-xs text-slate-500 whitespace-nowrap">{{ step.label }}</span>
                  <div v-if="i < 2" class="w-8 h-px bg-slate-200" />
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div v-if="['PENDING_STAFF','STAFF_CONFIRMED','MANAGER_APPROVED'].includes(t.status)" class="flex flex-col gap-2 flex-shrink-0">
              <button
                class="h-8 px-4 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg text-xs font-semibold transition-colors flex items-center gap-1"
                @click="approveTransfer(t.id)"
              >
                <span class="material-symbols-outlined text-sm">check</span> Duyệt
              </button>
              <button
                class="h-8 px-4 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1"
                @click="rejectTransfer(t.id)"
              >
                <span class="material-symbols-outlined text-sm">close</span> Từ chối
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- User Modal -->
    <AppModal :show="showUserModal" :title="editingUser ? 'Sửa nhân viên' : 'Thêm nhân viên'" @close="showUserModal = false">
      <div class="p-6 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Tên đăng nhập <span class="text-red-500">*</span></label>
            <input v-model="userForm.username" type="text" :disabled="!!editingUser" placeholder="username" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 font-mono disabled:bg-slate-50 disabled:text-slate-400" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Họ và tên <span class="text-red-500">*</span></label>
            <input v-model="userForm.fullName" type="text" placeholder="Họ tên đầy đủ" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Mật khẩu {{ editingUser ? '(để trống nếu không đổi)' : '*' }}</label>
            <input v-model="userForm.password" type="password" placeholder="Mật khẩu..." class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Vai trò</label>
            <select v-model="userForm.role" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white">
              <option value="ADMIN">Admin</option>
              <option value="MANAGER">Manager</option>
              <option value="STAFF">Nhân viên</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Chi nhánh</label>
            <select v-model="userForm.branchId" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white">
              <option value="">-- Không có --</option>
              <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Trạng thái</label>
            <select v-model="userForm.status" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white">
              <option value="ACTIVE">Hoạt động</option>
              <option value="INACTIVE">Ngừng hoạt động</option>
            </select>
          </div>
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showUserModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="uSaving" @click="saveUser">
            <span v-if="uSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ uSaving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Transfer Request Modal -->
    <AppModal :show="showTransferModal" title="Tạo yêu cầu điều chuyển nhân sự" @close="showTransferModal = false">
      <div class="p-6 space-y-4">
        <div class="bg-blue-50 rounded-xl px-4 py-3 text-sm text-blue-700 flex items-start gap-2">
          <span class="material-symbols-outlined text-base mt-0.5">info</span>
          <span>Quy trình 3 bước: Nhân viên xác nhận → Manager duyệt → Admin phê duyệt</span>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Nhân viên cần điều chuyển <span class="text-red-500">*</span></label>
          <select v-model="transferForm.staffId" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white">
            <option value="">-- Chọn nhân viên --</option>
            <option v-for="u in users.filter(u => u.role === 'STAFF')" :key="u.id" :value="u.id">
              {{ u.fullName }} ({{ u.branchName || 'Chưa phân công' }})
            </option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Chi nhánh đích <span class="text-red-500">*</span></label>
          <select v-model="transferForm.targetBranchId" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white">
            <option value="">-- Chọn chi nhánh --</option>
            <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Lý do điều chuyển</label>
          <textarea v-model="transferForm.reason" rows="2" placeholder="Nhập lý do..." class="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 resize-none" />
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50" @click="showTransferModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold flex items-center justify-center gap-2" :disabled="tSaving" @click="createTransfer">
            <span v-if="tSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ tSaving ? 'Đang tạo...' : 'Tạo yêu cầu' }}
          </button>
        </div>
      </div>
    </AppModal>

    <ConfirmDialog :show="showDeleteUser" title="Xóa nhân viên" :message="`Bạn có chắc muốn xóa tài khoản '${deletingUser?.fullName}'?`" confirm-text="Xóa" :danger="true" @confirm="doDeleteUser" @cancel="showDeleteUser = false" />
  </div>
</template>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }
</style>
