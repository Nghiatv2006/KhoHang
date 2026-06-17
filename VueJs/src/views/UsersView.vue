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


// Load
async function loadUsers() {
  uLoading.value = true
  try { const res = await api.get('/api/users'); if (res.ok) users.value = await res.json() }
  catch {} finally { uLoading.value = false }
}

onMounted(async () => {
  await loadUsers()
  try { const res = await api.get('/api/branches'); if (res.ok) branches.value = await res.json() } catch {}
})



function formatDate(dt: string) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto">
    <!-- Header & Tabs -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0">Quản lý Nhân viên</h2>
        <p class="text-[#8094ae] text-sm mt-1">Quản lý tài khoản, vai trò và phân công chi nhánh</p>
      </div>
      
    </div>

    <!-- USERS TAB -->
    <div class="bg-indigo-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] flex items-center justify-between flex-wrap gap-4 bg-[#f8f9fa]/50">
        <div class="flex items-center gap-3 flex-1 min-w-[300px]">
          <div class="relative w-[250px]">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input v-model="uSearch" type="text" placeholder="Tìm theo tên, username..." class="w-full h-[42px] pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
          </div>
          <select v-model="uRoleFilter" class="h-[42px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] cursor-pointer">
            <option value="">Tất cả vai trò</option>
            <option value="ADMIN">Admin</option>
            <option value="MANAGER">Manager</option>
            <option value="STAFF">Nhân viên</option>
          </select>
          <select v-model="uStatusFilter" class="h-[42px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] cursor-pointer">
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Ngừng HĐ</option>
          </select>
        </div>
        <button v-if="isAdmin" class="bg-[#4361ee] text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2" @click="openAddUser">
          <i class="fas fa-user-plus"></i> Thêm nhân viên
        </button>
      </div>

      <!-- Table -->
      <div class="overflow-x-auto">
        <div v-if="uLoading" class="p-8 space-y-4"><div v-for="i in 5" :key="i" class="h-12 bg-[#f8f9fa] rounded-xl animate-pulse" /></div>
        <div v-else-if="filteredUsers.length === 0" class="py-20 text-center text-[#8094ae]">
          <i class="fas fa-users-slash text-5xl mb-4 opacity-40"></i>
          <div class="font-bold text-[#364a63]">Không tìm thấy nhân viên nào</div>
        </div>
        <table v-else class="w-full text-left border-collapse">
          <thead class="bg-white">
            <tr>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Nhân viên</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Vai trò</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Chi nhánh</th>
              <th class="p-4 text-center text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Trạng thái</th>
              <th v-if="isAdmin" class="p-4 border-b border-[#f1f5f9] w-[140px]"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id" class="border-b border-[#f1f5f9] hover:border-transparent hover:bg-gradient-to-r hover:from-[#4361ee]/15 hover:to-[#4cc9f0]/15 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]" @dblclick="isAdmin ? openEditUser(u) : null">
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full bg-[#eef2ff] border border-[#dbeafe] flex items-center justify-center text-[#4361ee] font-bold text-sm flex-shrink-0 group-hover:bg-white group-hover:border-[#4361ee]/30 transition-colors">
                    {{ u.fullName?.charAt(0) || '?' }}
                  </div>
                  <div>
                    <div class="font-bold text-[#364a63]">{{ u.fullName }}</div>
                    <div class="text-xs text-[#8094ae] font-mono mt-0.5">@{{ u.username }}</div>
                  </div>
                </div>
              </td>
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl"><StatusBadge :value="u.role" type="role" /></td>
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <span v-if="u.branchName" class="font-medium text-[#364a63]"><i class="fas fa-store text-[#8094ae] mr-1"></i> {{ u.branchName }}</span>
                <span v-else class="text-[#8094ae] text-sm italic">Chưa phân công</span>
              </td>
              <td class="p-4 text-center first:rounded-l-xl last:rounded-r-xl"><StatusBadge :value="u.status" type="status" /></td>
              <td v-if="isAdmin" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                  <button class="w-8 h-8 rounded-lg text-[#8094ae] bg-white hover:bg-[#e2e8f0] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="toggleUser(u)" :title="u.status === 'ACTIVE' ? 'Vô hiệu hóa' : 'Kích hoạt'">
                    <i :class="['fas text-sm', u.status === 'ACTIVE' ? 'fa-toggle-on text-[#05b171]' : 'fa-toggle-off text-[#8094ae]']"></i>
                  </button>
                  <button class="w-8 h-8 rounded-lg text-[#0ea5e9] bg-white hover:bg-[#e0f2fe] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="openEditUser(u)" title="Sửa">
                    <i class="fas fa-pen text-sm"></i>
                  </button>
                  <button class="w-8 h-8 rounded-lg text-[#ea4f52] bg-white hover:bg-[#ffe4e6] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="confirmDeleteUser(u)" title="Xóa">
                    <i class="fas fa-trash text-sm"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!uLoading && filteredUsers.length > 0" class="px-6 py-4 bg-[#f8f9fa] border-t border-[#f1f5f9] text-xs font-bold text-[#8094ae]">
          Tổng cộng: {{ filteredUsers.length }} nhân viên
        </div>
      </div>
    </div>

        </div>
      </div>
    </div>

    <!-- ── USER RIGHT PANEL ── -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showUserModal" @click="showUserModal = false" class="fixed inset-0 bg-slate-900/20 backdrop-blur-[2px] z-[100]"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showUserModal" class="fixed inset-y-0 right-0 z-[101] w-[450px] bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] flex flex-col border-l border-[#e2e8f0]">
          <!-- Header -->
          <div class="px-6 py-5 border-b border-[#f1f5f9] flex justify-between items-center bg-gradient-to-r from-[#f8fafc] to-white">
            <h3 class="font-bold text-[#364a63] text-lg flex items-center gap-2">
              <i class="fas fa-user-circle text-[#4361ee]"></i>
              {{ editingUser ? 'Sửa nhân viên' : 'Thêm nhân viên' }}
            </h3>
            <button @click="showUserModal = false" class="text-[#8094ae] hover:text-[#ea4f52] transition-colors w-8 h-8 flex items-center justify-center rounded-full hover:bg-red-50">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- Body -->
          <div class="p-6 flex-1 overflow-y-auto space-y-5 custom-scrollbar">
            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Tên đăng nhập <span class="text-[#ea4f52]">*</span></label>
                <input v-model="userForm.username" type="text" :disabled="!!editingUser" placeholder="username" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] font-mono disabled:opacity-60 disabled:cursor-not-allowed text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Họ và tên <span class="text-[#ea4f52]">*</span></label>
                <input v-model="userForm.fullName" type="text" placeholder="Họ tên đầy đủ" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63]" />
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mật khẩu <span v-if="!editingUser" class="text-[#ea4f52]">*</span></label>
                <input v-model="userForm.password" type="password" :placeholder="editingUser ? 'Để trống nếu không đổi' : 'Nhập mật khẩu'" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Vai trò</label>
                <select v-model="userForm.role" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] cursor-pointer">
                  <option value="ADMIN">Admin</option>
                  <option value="MANAGER">Manager</option>
                  <option value="STAFF">Nhân viên</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Chi nhánh</label>
                <select v-model="userForm.branchId" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] cursor-pointer">
                  <option value="">-- Chưa phân công --</option>
                  <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
                </select>
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Trạng thái</label>
                <select v-model="userForm.status" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] cursor-pointer">
                  <option value="ACTIVE">Hoạt động</option>
                  <option value="INACTIVE">Ngừng hoạt động</option>
                </select>
              </div>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="p-6 border-t border-[#f1f5f9] bg-[#f8fafc] flex gap-3">
            <button class="flex-1 h-11 bg-white border border-[#e2e8f0] hover:bg-[#f8f9fa] text-[#364a63] rounded-xl text-sm font-bold transition-colors shadow-sm" @click="showUserModal = false">Hủy bỏ</button>
            <button class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-2" :disabled="uSaving" @click="saveUser">
              <i v-if="uSaving" class="fas fa-spinner fa-spin"></i>
              {{ uSaving ? 'Đang lưu...' : 'Lưu thông tin' }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>


    <ConfirmDialog :show="showDeleteUser" title="Xóa nhân viên" :message="`Bạn có chắc muốn xóa tài khoản '${deletingUser?.fullName}'? Hành động này không thể hoàn tác.`" confirm-text="Xóa" :danger="true" @confirm="doDeleteUser" @cancel="showDeleteUser = false" />
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
