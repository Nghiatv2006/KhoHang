<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import StatusBadge from '../components/StatusBadge.vue'

const toast = useToast()
const currentUser = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const isManager = computed(() => currentUser.value?.role === 'MANAGER')


// ── USERS ──────────────────────────────────────────────────────────────────
const users = ref<any[]>([])
const branches = ref<any[]>([])
const uLoading = ref(true)
const uSearch = ref('')
const uRoleFilter = ref('')
const uStatusFilter = ref('')

const lastActiveUserId = ref<number | null>(null)
const isInitialLoad = ref(true)

const currentPage = ref(1)
const pageSize = ref(10)

// Reset to page 1 when search or filters change
watch([uSearch, uRoleFilter, uStatusFilter], () => {
  currentPage.value = 1
})



watch(uLoading, (newVal) => {
  if (!newVal) {
    currentPage.value = 1
    setTimeout(() => {
      isInitialLoad.value = false
    }, 800)
  }
})

const filteredUsers = computed(() => {
  let list = [...users.value]
  if (uSearch.value.trim()) {
    const kw = uSearch.value.toLowerCase()
    list = list.filter(u => 
      u.fullName?.toLowerCase().includes(kw) || 
      u.username?.toLowerCase().includes(kw) ||
      u.phone?.toLowerCase().includes(kw) ||
      u.email?.toLowerCase().includes(kw)
    )
  }
  if (uRoleFilter.value) list = list.filter(u => u.role === uRoleFilter.value)
  if (uStatusFilter.value) list = list.filter(u => u.status === uStatusFilter.value)

  // Sắp xếp tùy chỉnh: 
  // - Tài khoản 'admin' (username === 'admin') luôn đứng đầu
  // - Các tài khoản khác sắp xếp theo thời gian cập nhật mới nhất (updatedAt DESC)
  list.sort((a, b) => {
    const aIsAdmin = a.username === 'admin'
    const bIsAdmin = b.username === 'admin'
    if (aIsAdmin && !bIsAdmin) return -1
    if (!aIsAdmin && bIsAdmin) return 1

    const timeA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
    const timeB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
    return timeB - timeA
  })

  return list
})

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / pageSize.value))

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredUsers.value.slice(start, end)
})

const showUserModal = ref(false)
const editingUser = ref<any>(null)
const userForm = reactive({ username: '', fullName: '', password: '', role: 'STAFF', branchId: '' as any, status: 'ACTIVE', phone: '', email: '' })
const uSaving = ref(false)
const showDeleteUser = ref(false)
const deletingUser = ref<any>(null)

// Chống tự động điền bằng readonly tạm thời
const usernameReadonly = ref(true)
const fullNameReadonly = ref(true)
const passwordReadonly = ref(true)
const emailReadonly = ref(true)
const phoneReadonly = ref(true)

// Xác định chi nhánh tổng động
const headBranch = computed(() => {
  const hb = branches.value.find((b: any) => b.isHead)
  if (hb) return hb
  return branches.value[0] || null
})

// Các vai trò được chọn dựa trên chi nhánh và vai trò đăng nhập
const selectableRoles = computed(() => {
  if (isManager.value) {
    return [{ value: 'STAFF', label: 'Nhân viên' }]
  }

  // Đối với ADMIN:
  const hb = headBranch.value
  if (!hb || !userForm.branchId) {
    return [{ value: 'ADMIN', label: 'Admin' }]
  }

  const selectedBranchId = Number(userForm.branchId)
  if (selectedBranchId === hb.id) {
    return [
      { value: 'ADMIN', label: 'Admin' },
      { value: 'STAFF', label: 'Nhân viên' }
    ]
  } else {
    return [{ value: 'MANAGER', label: 'Manager' }]
  }
})

// Theo dõi chi nhánh đổi để nhảy vai trò đối với ADMIN
watch(() => userForm.branchId, (newBranchId) => {
  if (!isAdmin.value) return
  const hb = headBranch.value
  if (!hb) return

  if (!newBranchId) {
    userForm.role = 'ADMIN'
  } else {
    const selectedBranchId = Number(newBranchId)
    if (selectedBranchId === hb.id) {
      if (userForm.role === 'MANAGER') {
        userForm.role = 'STAFF'
      }
    } else {
      userForm.role = 'MANAGER'
    }
  }
})

function openAddUser() {
  editingUser.value = null
  Object.assign(userForm, { 
    username: '', 
    fullName: '', 
    password: '', 
    role: isManager.value ? 'STAFF' : 'ADMIN', 
    branchId: isManager.value ? currentUser.value.branchId : '', 
    status: 'ACTIVE',
    phone: '',
    email: ''
  })
  usernameReadonly.value = true
  fullNameReadonly.value = true
  passwordReadonly.value = true
  emailReadonly.value = true
  phoneReadonly.value = true
  showUserModal.value = true
}

function openEditUser(u: any) {
  editingUser.value = u
  Object.assign(userForm, { 
    username: u.username, 
    fullName: u.fullName, 
    password: '', 
    role: u.role, 
    branchId: u.branchId || '', 
    status: u.status,
    phone: u.phone || '',
    email: u.email || ''
  })
  usernameReadonly.value = false
  fullNameReadonly.value = false
  passwordReadonly.value = false
  emailReadonly.value = false
  phoneReadonly.value = false
  showUserModal.value = true
}
function confirmDeleteUser(u: any) { deletingUser.value = u; showDeleteUser.value = true }

async function saveUser() {
  if (!userForm.fullName?.trim()) { toast.error('Họ tên là bắt buộc.'); return }
  if (!editingUser.value && !userForm.password) { toast.error('Mật khẩu là bắt buộc khi tạo mới.'); return }
  
  if (userForm.email?.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userForm.email.trim())) {
    toast.error('Định dạng email không hợp lệ.')
    return
  }
  let cleanPhone = null
  if (userForm.phone?.trim()) {
    cleanPhone = userForm.phone.trim().replace(/[-. ]/g, '')
    if (!/^(0|\+84|84)[0-9]{9,11}$/.test(cleanPhone)) {
      toast.error('Số điện thoại không hợp lệ (phải bắt đầu bằng 0, 84 hoặc +84 và gồm 10-12 chữ số).')
      return
    }
  }
  
  uSaving.value = true
  try {
    const payload: any = {
      username: userForm.username,
      fullName: userForm.fullName.trim(),
      role: userForm.role,
      branchId: userForm.branchId || null,
      status: userForm.status,
      phone: cleanPhone,
      email: userForm.email?.trim() || null,
    }
    if (userForm.password) payload.password = userForm.password
    const res = editingUser.value
      ? await api.put(`/api/users/${editingUser.value.id}`, payload)
      : await api.post('/api/users', payload)
    const data = await res.json()
    if (res.ok) { 
      toast.success(editingUser.value ? 'Cập nhật thành công!' : 'Tạo tài khoản thành công!')
      showUserModal.value = false
      if (data && data.id) {
        lastActiveUserId.value = data.id
      }
      await loadUsers() 
    }
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

function triggerUsersAnimation() {
  isInitialLoad.value = true
  loadUsers()
}

onMounted(async () => {
  window.addEventListener('trigger-users-animation', triggerUsersAnimation)
  await loadUsers()
  try { const res = await api.get('/api/branches'); if (res.ok) branches.value = await res.json() } catch {}
})

onUnmounted(() => {
  window.removeEventListener('trigger-users-animation', triggerUsersAnimation)
})



</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto">
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2">
      <div class="header-animate">
        <h2 class="text-2xl font-bold text-[#364a63] m-0 flex flex-wrap">
          <span v-for="(char, idx) in 'Quản lý Nhân viên'.split('')" :key="idx" 
            :style="{ animationDelay: `${idx * 30}ms` }"
            class="char-pop inline-block whitespace-pre"
          >{{ char }}</span>
        </h2>
        <p class="text-[#8094ae] text-sm mt-1">Quản lý tài khoản, vai trò và phân công chi nhánh</p>
      </div>
      
    </div>

    <!-- USERS TAB -->
    <div :class="['bg-indigo-50 rounded-[16px] border border-[#f1f5f9] shadow-[0_2px_10px_rgba(0,0,0,0.02)] container-animate overflow-hidden', isInitialLoad ? 'overflow-visible' : 'overflow-hidden']">
      <!-- Neon Liquid Gradient Top Line -->
      <div class="h-[4px] w-full bg-gradient-to-r from-[#4361ee] via-[#f72585] to-[#4cc9f0] bg-[length:200%_auto] animate-neon-sweep rounded-t-[16px]"></div>
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] flex items-center justify-between flex-wrap gap-4 bg-[#f8f9fa]/50 toolbar-animate">
        <div class="flex items-center gap-3 flex-1 min-w-[300px]">
          <div class="relative w-[250px]">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input v-model="uSearch" type="text" placeholder="Tìm theo tên, username..." class="w-full h-[42px] pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
          </div>
          <select v-if="isAdmin" v-model="uRoleFilter" class="h-[42px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] cursor-pointer">
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
        <button v-if="isAdmin || isManager" class="bg-[#4361ee] text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2 btn-shimmer" @click="openAddUser">
          <i class="fas fa-user-plus"></i> Thêm nhân viên
        </button>
      </div>

      <div :class="[isInitialLoad ? 'overflow-visible' : 'overflow-x-auto']">
        <Transition name="fade-layout" mode="out-in">
          <div v-if="uLoading" key="loading" class="p-8 space-y-4"><div v-for="i in 5" :key="i" class="h-12 bg-[#f8f9fa] rounded-xl animate-pulse" /></div>
          <div v-else-if="filteredUsers.length === 0" key="empty" class="py-20 text-center text-[#8094ae]">
            <i class="fas fa-users-slash text-5xl mb-4 opacity-40"></i>
            <div class="font-bold text-[#364a63]">Không tìm thấy nhân viên nào</div>
          </div>
          <div v-else key="content">
            <table class="w-full text-left border-collapse">
          <thead class="bg-white">
            <tr>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Nhân viên</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Vai trò</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Chi nhánh</th>
              <th class="p-4 text-center text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Trạng thái</th>
              <th v-if="isAdmin || isManager" class="p-4 border-b border-[#f1f5f9] w-[140px]"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(u, index) in paginatedUsers" :key="u.id" :class="['border-b border-[#f1f5f9] hover:border-transparent hover:shadow-sm transition-all duration-300 cursor-pointer group', u.id === lastActiveUserId ? 'bg-[#4361ee]/5 font-semibold' : '', isInitialLoad ? (index % 2 === 0 ? 'fly-in-left-anim' : 'fly-in-right-anim') : '', `role-row-${u.role}`]" :style="isInitialLoad ? { '--delay': `${index * 30}ms` } : {}" @dblclick="isAdmin || isManager ? openEditUser(u) : null">
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0 user-avatar group-hover:rotate-[360deg] group-hover:scale-110 group-hover:shadow-md transition-all duration-700 ease-out">
                    {{ u.fullName?.charAt(0) || '?' }}
                  </div>
                  <div>
                    <div class="font-bold text-[#364a63]">{{ u.fullName }}</div>
                    <div class="flex items-center gap-2 flex-wrap text-xs text-[#8094ae] mt-0.5 font-mono">
                      <span>@{{ u.username }}</span>
                      <span v-if="u.phone" class="text-slate-300">•</span>
                      <span v-if="u.phone" class="flex items-center gap-1"><i class="fas fa-phone-alt text-[10px]"></i>{{ u.phone }}</span>
                      <span v-if="u.email" class="text-slate-300">•</span>
                      <span v-if="u.email" class="flex items-center gap-1"><i class="far fa-envelope text-[10px]"></i>{{ u.email }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl"><StatusBadge :value="u.role" type="role" /></td>
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div>
                  <span v-if="u.branchName" class="font-medium text-[#364a63]"><i class="fas fa-store text-[#8094ae] mr-1"></i> {{ u.branchName }}</span>
                  <span v-else class="text-[#8094ae] text-sm italic">Chưa phân công</span>
                </div>
              </td>
              <td class="p-4 text-center first:rounded-l-xl last:rounded-r-xl"><StatusBadge :value="u.status" type="status" /></td>
              <td v-if="isAdmin || isManager" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div v-if="u.id !== currentUser.id" class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
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
            <div class="px-6 py-4 bg-[#f8f9fa] border-t border-[#f1f5f9] flex flex-col sm:flex-row items-center justify-between gap-4 text-xs font-bold text-[#8094ae]">
              <div>
                Hiển thị {{ Math.min((currentPage - 1) * pageSize + 1, filteredUsers.length) }} - {{ Math.min(currentPage * pageSize, filteredUsers.length) }} của {{ filteredUsers.length }} nhân viên
              </div>
              
              <div v-if="totalPages > 1" class="flex items-center gap-1.5">
                <!-- Prev Button -->
                <button 
                  :disabled="currentPage === 1" 
                  @click="currentPage--"
                  class="w-8 h-8 rounded-lg border border-[#e2e8f0] bg-white hover:bg-[#e2e8f0]/40 flex items-center justify-center text-[#8094ae] disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer"
                >
                  <i class="fas fa-chevron-left text-[10px]"></i>
                </button>
                
                <!-- Page Buttons -->
                <button 
                  v-for="p in totalPages" 
                  :key="p" 
                  @click="currentPage = p"
                  :class="[
                    'w-8 h-8 rounded-lg border flex items-center justify-center transition-all cursor-pointer font-bold',
                    currentPage === p 
                      ? 'bg-[#4361ee] border-[#4361ee] text-white shadow-sm shadow-[#4361ee]/20' 
                      : 'bg-white border-[#e2e8f0] hover:bg-[#e2e8f0]/40 text-[#364a63]'
                  ]"
                >
                  {{ p }}
                </button>
                
                <!-- Next Button -->
                <button 
                  :disabled="currentPage === totalPages" 
                  @click="currentPage++"
                  class="w-8 h-8 rounded-lg border border-[#e2e8f0] bg-white hover:bg-[#e2e8f0]/40 flex items-center justify-center text-[#8094ae] disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer"
                >
                  <i class="fas fa-chevron-right text-[10px]"></i>
                </button>
              </div>
            </div>
          </div>
        </Transition>
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
          <div class="px-6 py-5 border-b border-[#f1f5f9] flex justify-between items-center bg-[#f8f9fa]">
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
            <!-- Fake inputs to prevent browser autofill -->
            <div style="position: absolute; left: -9999px; top: -9999px;">
              <input type="text" name="fake_username_prevent_autofill" />
              <input type="password" name="fake_password_prevent_autofill" />
              <input type="email" name="fake_email_prevent_autofill" />
              <input type="tel" name="fake_phone_prevent_autofill" />
            </div>

            <div class="grid grid-cols-2 gap-5">
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Tên đăng nhập <span class="text-[#ea4f52]">*</span></label>
                <input v-model="userForm.username" type="text" :disabled="!!editingUser" :readonly="usernameReadonly && !editingUser" @focus="usernameReadonly = false" autocomplete="off" placeholder="username" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] font-mono disabled:opacity-60 disabled:cursor-not-allowed text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Họ và tên <span class="text-[#ea4f52]">*</span></label>
                <input v-model="userForm.fullName" type="text" :readonly="fullNameReadonly" @focus="fullNameReadonly = false" autocomplete="off" placeholder="Họ tên đầy đủ" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63]" />
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mật khẩu <span v-if="!editingUser" class="text-[#ea4f52]">*</span></label>
                <input v-model="userForm.password" type="password" :readonly="passwordReadonly" @focus="passwordReadonly = false" autocomplete="new-password" :placeholder="editingUser ? 'Để trống nếu không đổi' : 'Nhập mật khẩu'" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Số điện thoại</label>
                <input v-model="userForm.phone" type="text" :readonly="phoneReadonly" @focus="phoneReadonly = false" autocomplete="off" placeholder="Số điện thoại" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Email</label>
                <input v-model="userForm.email" type="email" :readonly="emailReadonly" @focus="emailReadonly = false" autocomplete="off" placeholder="Email" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Chi nhánh</label>
                <select v-model="userForm.branchId" :disabled="isManager" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed">
                  <option value="">-- Chọn chi nhánh --</option>
                  <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Vai trò</label>
                <select v-model="userForm.role" :disabled="selectableRoles.length <= 1" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] text-[#364a63] cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed">
                  <option v-for="r in selectableRoles" :key="r.value" :value="r.value">{{ r.label }}</option>
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

@keyframes flyInLeft {
  0% {
    transform: translate3d(-100vw, -100vh, -600px) rotateX(75deg) rotateY(-75deg) rotateZ(-45deg) scale(0.5);
    opacity: 0;
  }
  60% {
    transform: translate3d(20px, 15px, 50px) rotateX(-10deg) rotateY(10deg) rotateZ(3deg) scale(1.02);
    opacity: 0.9;
  }
  85% {
    transform: translate3d(-5px, -5px, -10px) rotateX(3deg) rotateY(-3deg) rotateZ(-1deg) scale(0.99);
    opacity: 1;
  }
  100% {
    transform: translate3d(0, 0, 0) rotateX(0deg) rotateY(0deg) rotateZ(0deg) scale(1);
    opacity: 1;
  }
}

@keyframes flyInRight {
  0% {
    transform: translate3d(100vw, -100vh, -600px) rotateX(75deg) rotateY(75deg) rotateZ(45deg) scale(0.5);
    opacity: 0;
  }
  60% {
    transform: translate3d(-20px, 15px, 50px) rotateX(-10deg) rotateY(-10deg) rotateZ(-3deg) scale(1.02);
    opacity: 0.9;
  }
  85% {
    transform: translate3d(5px, -5px, -10px) rotateX(3deg) rotateY(3deg) rotateZ(1deg) scale(0.99);
    opacity: 1;
  }
  100% {
    transform: translate3d(0, 0, 0) rotateX(0deg) rotateY(0deg) rotateZ(0deg) scale(1);
    opacity: 1;
  }
}

.fly-in-left-anim {
  animation: flyInLeft 0.23s cubic-bezier(0.15, 0.85, 0.3, 1.05) both;
  animation-delay: var(--delay, 0ms);
  will-change: transform, opacity;
}

.fly-in-right-anim {
  animation: flyInRight 0.23s cubic-bezier(0.15, 0.85, 0.3, 1.05) both;
  animation-delay: var(--delay, 0ms);
  will-change: transform, opacity;
}

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

/* Additional premium effects */
@keyframes slideDownHeader {
  0% {
    transform: translateY(-20px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

.header-animate {
  animation: slideDownHeader 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes charPopIn {
  0% {
    transform: translateY(15px) scale(0.6);
    opacity: 0;
    filter: blur(4px);
  }
  60% {
    transform: translateY(-4px) scale(1.08);
    opacity: 1;
    filter: blur(0);
  }
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.char-pop {
  animation: charPopIn 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

@keyframes toolbarPop {
  0% {
    transform: translateY(-10px) scale(0.99);
    opacity: 0;
  }
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.toolbar-animate {
  animation: toolbarPop 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 150ms;
}

@keyframes containerSlideUp {
  0% {
    transform: translateY(20px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
  }
}

.container-animate {
  animation: containerSlideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 220ms;
}

.btn-shimmer {
  position: relative;
  overflow: hidden;
}
.btn-shimmer::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -60%;
  width: 30%;
  height: 200%;
  background: rgba(255, 255, 255, 0.25);
  transform: rotate(30deg);
  transition: none;
  animation: shimmer 3s infinite;
}
@keyframes shimmer {
  0% { left: -60%; }
  30% { left: 140%; }
  100% { left: 140%; }
}

.fade-layout-enter-active, .fade-layout-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}
.fade-layout-enter-from, .fade-layout-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* Table row left glowing border on hover */
tbody tr td:first-child {
  box-shadow: inset 0 0 0 transparent;
  transition: box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ── Role Row styles (Light mode defaults) ── */
.role-row-ADMIN .user-avatar {
  background-color: #fffbeb;
  border: 1px solid #fde68a;
  color: #b45309;
}
.role-row-ADMIN:hover {
  background: linear-gradient(to right, rgba(245, 158, 11, 0.08), rgba(225, 29, 72, 0.08)) !important;
}
.role-row-ADMIN:hover td:first-child {
  box-shadow: inset 4px 0 0 0 #f59e0b !important;
}

.role-row-MANAGER .user-avatar {
  background-color: #f0f2ff;
  border: 1px solid #c7d2fe;
  color: #4361ee;
}
.role-row-MANAGER:hover {
  background: linear-gradient(to right, rgba(67, 97, 238, 0.08), rgba(114, 9, 183, 0.08)) !important;
}
.role-row-MANAGER:hover td:first-child {
  box-shadow: inset 4px 0 0 0 #4361ee !important;
}

.role-row-STAFF .user-avatar {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #16a34a;
}
.role-row-STAFF:hover {
  background: linear-gradient(to right, rgba(16, 185, 129, 0.08), rgba(6, 182, 212, 0.08)) !important;
}
.role-row-STAFF:hover td:first-child {
  box-shadow: inset 4px 0 0 0 #10b981 !important;
}

/* ── Dark Mode Overrides for Role Rows ── */
html.dark-mode .role-row-ADMIN .user-avatar {
  background-color: rgba(120, 53, 15, 0.3) !important;
  border-color: rgba(180, 83, 9, 0.5) !important;
  color: #fcd34d !important;
}
html.dark-mode .role-row-ADMIN:hover {
  background: linear-gradient(to right, rgba(120, 53, 15, 0.2), rgba(30, 41, 59, 0.8)) !important;
}

html.dark-mode .role-row-MANAGER .user-avatar {
  background-color: rgba(30, 27, 75, 0.4) !important;
  border-color: rgba(67, 97, 238, 0.5) !important;
  color: #818cf8 !important;
}
html.dark-mode .role-row-MANAGER:hover {
  background: linear-gradient(to right, rgba(30, 27, 75, 0.3), rgba(30, 41, 59, 0.8)) !important;
}

html.dark-mode .role-row-STAFF .user-avatar {
  background-color: rgba(6, 78, 59, 0.3) !important;
  border-color: rgba(16, 185, 129, 0.5) !important;
  color: #34d399 !important;
}
html.dark-mode .role-row-STAFF:hover {
  background: linear-gradient(to right, rgba(6, 78, 59, 0.2), rgba(30, 41, 59, 0.8)) !important;
}

@keyframes neonSweep {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.animate-neon-sweep {
  animation: neonSweep 4s ease infinite;
}

/* ── Role Avatar Pulsing Halos ── */
@keyframes rolePulseAdmin {
  0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(245, 158, 11, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}
@keyframes rolePulseManager {
  0% { box-shadow: 0 0 0 0 rgba(67, 97, 238, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(67, 97, 238, 0); }
  100% { box-shadow: 0 0 0 0 rgba(67, 97, 238, 0); }
}
@keyframes rolePulseStaff {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.role-row-ADMIN .user-avatar {
  animation: rolePulseAdmin 2s infinite ease-in-out;
}
.role-row-MANAGER .user-avatar {
  animation: rolePulseManager 2s infinite ease-in-out;
}
.role-row-STAFF .user-avatar {
  animation: rolePulseStaff 2s infinite ease-in-out;
}

</style>
