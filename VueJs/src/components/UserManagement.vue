<template>
  <div class="user-management">
    
    <!-- Header & Actions -->
    <div class="page-top staggered-in" style="animation-delay: 0ms">
      <div>
        <h2 class="section-title">Quản lý Nhân sự</h2>
        <p class="section-sub">Phân quyền, gán chi nhánh và quản lý truy cập.</p>
      </div>
      <button class="btn-primary" @click="openAddPanel">
        <span class="material-symbols-outlined text-[18px]">person_add</span>
        Tạo tài khoản mới
      </button>
    </div>

    <!-- Stats Row -->
    <div class="stats-row staggered-in" style="animation-delay: 50ms">
      <div class="stat-card glass-panel group">
        <div class="stat-lbl">TỔNG NHÂN SỰ</div>
        <div class="stat-val mono text-gray-900">{{ users.length }}</div>
      </div>
      <div class="stat-card glass-panel group">
        <div class="stat-lbl">QUẢN TRỊ (ADMIN)</div>
        <div class="stat-val mono text-red-600">{{ users.filter(u => u.role === 'ADMIN').length }}</div>
      </div>
      <div class="stat-card glass-panel group">
        <div class="stat-lbl">QUẢN LÝ (MANAGER)</div>
        <div class="stat-val mono text-green-600">{{ users.filter(u => u.role === 'MANAGER').length }}</div>
      </div>
      <div class="stat-card glass-panel group">
        <div class="stat-lbl">BỊ KHÓA</div>
        <div class="stat-val mono text-orange-500">{{ users.filter(u => u.status === 'LOCKED').length }}</div>
      </div>
    </div>

    <!-- Toolbar: Filters & Search -->
    <div class="toolbar glass-panel staggered-in" style="animation-delay: 100ms">
      <div class="search-box">
        <span class="material-symbols-outlined search-ico">search</span>
        <input v-model="filters.search" type="text" placeholder="Tìm tên hoặc username..." />
      </div>
      
      <div class="filters-grp">
        <select v-model="filters.role" class="filter-sel">
          <option value="">Tất cả vai trò</option>
          <option value="ADMIN">ADMIN</option>
          <option value="MANAGER">MANAGER</option>
          <option value="STAFF">STAFF</option>
        </select>
        
        <select v-model="filters.branchId" class="filter-sel">
          <option value="">Tất cả chi nhánh</option>
          <option value="0">Toàn hệ thống (HQ)</option>
          <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
        </select>

        <select v-model="filters.status" class="filter-sel">
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động (ACTIVE)</option>
          <option value="LOCKED">Bị khóa (LOCKED)</option>
        </select>
      </div>
    </div>

    <!-- SPLIT LAYOUT: Table (Left) + Detail Form (Right) -->
    <div class="split-layout staggered-in" style="animation-delay: 150ms">
      
      <!-- List Pane -->
      <div class="list-pane glass-panel !p-0" :class="{ 'shrinked': activePanel }">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Nhân viên</th>
                <th v-if="!activePanel">Vai Trò</th>
                <th v-if="!activePanel">Chi Nhánh</th>
                <th>Trạng Thái</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="(user, index) in filteredUsers" :key="user.id" 
                class="clickable-row table-stagger" 
                :style="{ animationDelay: (200 + index * 40) + 'ms' }"
                :class="{ 'active-row': form.id === user.id }"
                @click="openEditPanel(user)"
              >
                <td>
                  <div class="user-cell">
                    <div class="avatar" :style="{ background: getAvatarGradient(user.username) }">
                      {{ user.fullName.charAt(0).toUpperCase() }}
                    </div>
                    <div class="user-meta">
                      <span class="user-name text-gray-900">{{ user.fullName }}</span>
                      <span class="user-username mono text-gray-500">{{ user.username }}</span>
                    </div>
                  </div>
                </td>
                <td v-if="!activePanel">
                  <span class="badge" :class="roleClass(user.role)">{{ user.role }}</span>
                </td>
                <td v-if="!activePanel" class="text-gray-600">{{ getBranchName(user.branchId) }}</td>
                <td>
                  <span class="pill-badge" :class="user.status === 'ACTIVE' ? 'pill-active' : 'pill-locked'">
                    <span class="status-dot" :class="user.status === 'ACTIVE' ? 'dot-active' : 'dot-locked'"></span>
                    {{ user.status }}
                  </span>
                </td>
              </tr>
              
              <tr v-if="filteredUsers.length === 0">
                <td :colspan="activePanel ? 2 : 4">
                  <div class="empty-state">
                    <div class="empty-ico-box">
                      <span class="material-symbols-outlined text-gray-400 text-4xl">person_search</span>
                    </div>
                    <h4 class="text-gray-900">Không tìm thấy kết quả</h4>
                    <p class="text-gray-500">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
                    <button class="btn-secondary" @click="clearFilters">Xóa bộ lọc</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Detail Pane (In-flow Form) -->
      <div v-if="activePanel" class="detail-pane glass-panel !p-0 overflow-hidden">
        
        <!-- Header area with big avatar -->
        <div class="detail-hero relative border-b border-gray-100">
          <div class="absolute inset-0 bg-gradient-to-b from-gray-50 to-transparent pointer-events-none"></div>
          <button class="icon-btn close-detail" @click="closePanel" title="Đóng">
            <span class="material-symbols-outlined">close</span>
          </button>

          <div v-if="isEditMode" class="hero-avatar ring-4 ring-white" :style="{ background: getAvatarGradient(form.username) }">
            {{ form.fullName.charAt(0).toUpperCase() || '?' }}
          </div>
          <div v-else class="hero-avatar new-user ring-4 ring-white">
            <span class="material-symbols-outlined">person_add</span>
          </div>

          <h3 class="detail-title text-gray-900">{{ isEditMode ? form.fullName : 'Tài khoản mới' }}</h3>
          <p class="detail-sub mono text-gray-500">{{ isEditMode ? '@' + form.username : 'Nhập thông tin bên dưới' }}</p>
        </div>

        <div class="detail-body p-6">
          <div v-if="panelError" class="panel-alert error">
            <span class="material-symbols-outlined">error</span>
            {{ panelError }}
          </div>

          <form @submit.prevent="submitForm" class="detail-form">
            <div class="form-group">
              <label>TÊN ĐĂNG NHẬP <span class="req">*</span></label>
              <input v-model="form.username" type="text" :disabled="isEditMode" required placeholder="Nhập username..." />
            </div>

            <div class="form-group">
              <label>HỌ VÀ TÊN <span class="req">*</span></label>
              <input v-model="form.fullName" type="text" required placeholder="Nhập họ tên đầy đủ..." />
            </div>

            <div class="form-group">
              <label>MẬT KHẨU <span v-if="!isEditMode" class="req">*</span></label>
              <input v-model="form.password" type="password" :required="!isEditMode" placeholder="••••••••" />
              <span v-if="isEditMode" class="form-hint">Để trống nếu không muốn đổi mật khẩu.</span>
            </div>

            <div class="form-group">
              <label>VAI TRÒ <span class="req">*</span></label>
              <select v-model="form.role" :disabled="isEditMode && form.username === currentUser" required>
                <option value="ADMIN">ADMIN - Quản trị hệ thống</option>
                <option value="MANAGER">MANAGER - Quản lý chi nhánh</option>
                <option value="STAFF">STAFF - Nhân viên kho</option>
              </select>
            </div>

            <div v-if="form.role !== 'ADMIN'" class="form-group">
              <label>CHI NHÁNH GÁN <span class="req">*</span></label>
              <select v-model="form.branchId" required>
                <option value="">-- Chọn chi nhánh --</option>
                <option v-for="b in branches" :key="b.id" :value="b.id">{{ b.name }}</option>
              </select>
            </div>
          </form>

          <!-- Danger Actions (Edit mode only) -->
          <div v-if="isEditMode && form.username !== currentUser" class="danger-zone border-t border-gray-200 mt-6 pt-6">
            <h4 class="text-gray-900 font-semibold mb-3">Quản lý rủi ro</h4>
            <div class="danger-actions">
              <button class="btn-outline-warning" @click="toggleStatus(form.id)">
                <span class="material-symbols-outlined">{{ form.status === 'ACTIVE' ? 'lock' : 'lock_open' }}</span>
                {{ form.status === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản' }}
              </button>
              <button class="btn-outline-error" @click="deleteUser(form.id)">
                <span class="material-symbols-outlined">delete</span> Xóa tài khoản vĩnh viễn
              </button>
            </div>
          </div>
          
        </div>

        <div class="detail-footer p-5 border-t border-gray-100 bg-gray-50">
          <button class="btn-secondary" @click="closePanel">Hủy</button>
          <button class="btn-primary" @click="submitForm">{{ isEditMode ? 'Lưu thay đổi' : 'Tạo tài khoản' }}</button>
        </div>
      </div>

    </div>

    <!-- Alert Toast -->
    <div v-if="alert.show" class="toast" :class="alert.type">
      <span class="material-symbols-outlined">{{ alert.type === 'success' ? 'check_circle' : 'warning' }}</span>
      {{ alert.message }}
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const currentUser = 'admin'; 

const branches = ref([
  { id: 1, name: 'Chi nhánh Hà Nội' },
  { id: 2, name: 'Chi nhánh TP.HCM' },
  { id: 3, name: 'Chi nhánh Đà Nẵng' }
]);

const users = ref([
  { id: 1, username: 'admin', fullName: 'Quản trị viên', role: 'ADMIN', branchId: null, status: 'ACTIVE', createdAt: '11/06/2026', hasTransactions: false },
  { id: 2, username: 'manager_hn', fullName: 'Lê Cường', role: 'MANAGER', branchId: 1, status: 'ACTIVE', createdAt: '12/06/2026', hasTransactions: true },
  { id: 3, username: 'staff_hn_1', fullName: 'Đặng Thảo', role: 'STAFF', branchId: 1, status: 'ACTIVE', createdAt: '12/06/2026', hasTransactions: true },
  { id: 4, username: 'manager_hcm', fullName: 'Phạm My', role: 'MANAGER', branchId: 2, status: 'ACTIVE', createdAt: '13/06/2026', hasTransactions: false },
  { id: 5, username: 'staff_locked', fullName: 'Lý Nam', role: 'STAFF', branchId: 2, status: 'LOCKED', createdAt: '13/06/2026', hasTransactions: false }
]);

const filters = ref({ search: '', role: '', branchId: '', status: '' });
const alert = ref({ show: false, message: '', type: 'success' });

const activePanel = ref(false);
const isEditMode = ref(false);
const panelError = ref('');
const form = ref({ id: 0, username: '', fullName: '', password: '', role: 'STAFF', branchId: '' as any, status: 'ACTIVE' });

// Light Mode Premium Gradients
const getAvatarGradient = (username: string) => {
  const gradients = [
    'linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%)',
    'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
    'linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)',
    'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
    'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
    'linear-gradient(135deg, #ff9a44 0%, #fc6076 100%)'
  ];
  let hash = 0;
  for (let i = 0; i < username.length; i++) hash = username.charCodeAt(i) + ((hash << 5) - hash);
  const index = Math.abs(hash) % gradients.length;
  return gradients[index];
};

const showAlert = (msg: string, type: 'success' | 'error' = 'success') => {
  alert.value = { show: true, message: msg, type };
  setTimeout(() => { alert.value.show = false; }, 3000);
};

const getBranchName = (branchId: number | null) => {
  if (branchId === null) return 'Toàn hệ thống';
  return branches.value.find(b => b.id === branchId)?.name || '-';
};

const roleClass = (role: string) => {
  if (role === 'ADMIN') return 'badge-admin';
  if (role === 'MANAGER') return 'badge-manager';
  return 'badge-staff';
};

const filteredUsers = computed(() => {
  return users.value.filter(u => {
    const s = filters.value.search.toLowerCase();
    const matchSearch = u.username.toLowerCase().includes(s) || u.fullName.toLowerCase().includes(s);
    const matchRole = !filters.value.role || u.role === filters.value.role;
    let matchBranch = true;
    if (filters.value.branchId) {
      matchBranch = filters.value.branchId === '0' ? u.branchId === null : u.branchId === parseInt(filters.value.branchId);
    }
    const matchStatus = !filters.value.status || u.status === filters.value.status;
    return matchSearch && matchRole && matchBranch && matchStatus;
  });
});

const clearFilters = () => { filters.value = { search: '', role: '', branchId: '', status: '' }; };

const openAddPanel = () => {
  activePanel.value = true; isEditMode.value = false; panelError.value = '';
  form.value = { id: 0, username: '', fullName: '', password: '', role: 'STAFF', branchId: '', status: 'ACTIVE' };
};

const openEditPanel = (user: any) => {
  if (activePanel.value && form.value.id === user.id) {
    closePanel();
    return;
  }
  activePanel.value = true; isEditMode.value = true; panelError.value = '';
  form.value = { id: user.id, username: user.username, fullName: user.fullName, password: '', role: user.role, branchId: user.branchId || '', status: user.status };
};

const closePanel = () => { activePanel.value = false; form.value.id = 0; };

const toggleStatus = (id: number) => {
  const user = users.value.find(u => u.id === id);
  if (!user) return;
  if (user.username === currentUser) return showAlert('Không thể tự khóa tài khoản của chính mình!', 'error');
  user.status = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
  form.value.status = user.status; 
  showAlert(`Đã ${user.status === 'ACTIVE' ? 'mở khóa' : 'khóa'} tài khoản ${user.username}`);
};

const deleteUser = (id: number) => {
  const user = users.value.find(u => u.id === id);
  if (!user) return;
  if (user.username === currentUser) return showAlert('Không thể tự xóa chính mình!', 'error');
  if (user.hasTransactions) return showAlert(`Tài khoản đã phát sinh dữ liệu, chỉ có thể KHÓA.`, 'error');
  if (confirm(`Xóa vĩnh viễn tài khoản ${user.username}?`)) {
    users.value = users.value.filter(u => u.id !== user.id);
    showAlert(`Đã xóa ${user.username}`);
    closePanel();
  }
};

const submitForm = () => {
  panelError.value = '';
  if (form.value.role !== 'ADMIN' && !form.value.branchId) return panelError.value = 'Vui lòng chọn chi nhánh.';
  const finalBranch = form.value.role === 'ADMIN' ? null : parseInt(form.value.branchId);

  if (isEditMode.value) {
    const user = users.value.find(u => u.id === form.value.id);
    if (user) {
      user.fullName = form.value.fullName; user.role = form.value.role; user.branchId = finalBranch;
      showAlert(`Đã cập nhật ${user.username}`); closePanel();
    }
  } else {
    if (users.value.some(u => u.username === form.value.username)) return panelError.value = 'Tên đăng nhập đã tồn tại.';
    users.value.push({
      id: Date.now(), username: form.value.username, fullName: form.value.fullName, role: form.value.role,
      branchId: finalBranch, status: 'ACTIVE', createdAt: new Date().toLocaleDateString('vi-VN'), hasTransactions: false
    });
    showAlert(`Đã tạo tài khoản ${form.value.username}`); closePanel();
  }
};
</script>

<style scoped>
/* ── Soft Muted Light Theme (Matched with BranchManagement) ── */
* { box-sizing: border-box; }
.mono { font-family: 'Geist Mono', monospace; }

.user-management {
  display: flex; flex-direction: column; gap: 24px;
}

/* ── Staggered Animation ── */
.staggered-in { animation: slideUpFade 0.6s cubic-bezier(0.16, 1, 0.3, 1) both; }
.table-stagger { animation: slideUpFade 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; }
@keyframes slideUpFade {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Header ── */
.page-top { display: flex; justify-content: space-between; align-items: flex-end; }
.section-title { font-size: 24px; font-weight: 800; color: #111827; margin: 0 0 4px 0; letter-spacing: -0.02em; }
.section-sub { font-size: 14px; color: #6b7280; margin: 0; }

/* ── Buttons ── */
.btn-primary, .btn-secondary {
  height: 40px; padding: 0 16px; border-radius: 8px;
  font-family: inherit; font-size: 13.5px; font-weight: 600;
  cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); 
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.btn-primary { background: #b91c1c; border: 1px solid #991b1b; color: #fff; box-shadow: 0 2px 8px rgba(185,28,28,0.2); }
.btn-primary:hover { background: #991b1b; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(185,28,28,0.3); }
.btn-primary:active { transform: scale(0.96); box-shadow: 0 1px 2px rgba(185,28,28,0.2); }

.btn-secondary { background: #f3f4f6; border: 1px solid #d1d5db; color: #374151; }
.btn-secondary:hover { background: #e5e7eb; border-color: #9ca3af; transform: translateY(-1px); }
.btn-secondary:active { transform: scale(0.96); }

/* ── Stats ── */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.glass-panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}
.stat-card { padding: 22px 20px; transition: transform 0.2s, box-shadow 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.06); border-color: #d1d5db; }
.stat-lbl { font-size: 11.5px; font-weight: 700; color: #6b7280; letter-spacing: 0.05em; margin-bottom: 8px; }
.stat-val { font-size: 34px; font-weight: 800; letter-spacing: -0.02em; }

/* ── Toolbar ── */
.toolbar { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; background: #f9fafb; }
.search-box { position: relative; width: 340px; }
.search-ico { position: absolute; left: 14px; top: 10px; font-size: 20px; color: #9ca3af; }
.search-box input {
  width: 100%; height: 40px; padding: 0 16px 0 42px; border-radius: 8px;
  border: 1px solid #d1d5db; font-size: 14px; font-family: inherit; transition: all 0.2s;
  background: #ffffff; color: #111827; box-shadow: inset 0 1px 2px rgba(0,0,0,0.02);
}
.search-box input:focus { border-color: #b91c1c; outline: none; box-shadow: 0 0 0 3px rgba(185,28,28,0.1); }

.filters-grp { display: flex; gap: 12px; }
.filter-sel {
  height: 40px; padding: 0 32px 0 14px; border-radius: 8px; border: 1px solid #d1d5db;
  font-size: 13.5px; font-family: inherit; color: #374151; cursor: pointer; outline: none; background: #ffffff;
  transition: all 0.2s; font-weight: 500;
}
.filter-sel:focus { border-color: #b91c1c; box-shadow: 0 0 0 3px rgba(185,28,28,0.1); }

/* ── Split Layout ── */
.split-layout { display: flex; gap: 24px; align-items: flex-start; }
.list-pane { flex: 1; min-width: 0; transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); border-color: #e5e7eb; }
.detail-pane {
  width: 460px; flex-shrink: 0; display: flex; flex-direction: column; 
  position: sticky; top: 84px; max-height: calc(100vh - 104px);
  animation: slideInRight 0.4s cubic-bezier(0.16, 1, 0.3, 1); border-color: #d1d5db; box-shadow: -4px 0 24px rgba(0,0,0,0.03);
}
@keyframes slideInRight { from { opacity: 0; transform: translateX(30px) scale(0.98); } to { opacity: 1; transform: translateX(0) scale(1); } }

/* ── Table & Avatar ── */
.table-container { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; text-align: left; }
.data-table th {
  padding: 16px 24px; border-bottom: 2px solid #e5e7eb; background: #f3f4f6;
  font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: #6b7280; letter-spacing: 0.05em;
}
.data-table td { padding: 16px 24px; border-bottom: 1px solid #f3f4f6; font-size: 14px; color: #111827; }
.data-table tr:last-child td { border-bottom: none; }

.clickable-row { cursor: pointer; transition: all 0.2s; }
.clickable-row:hover { background: #f9fafb; transform: scale(0.998); }
.active-row { background: #fef2f2 !important; box-shadow: inset 4px 0 0 #b91c1c; }

.user-cell { display: flex; align-items: center; gap: 14px; }
.avatar {
  width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 800; font-size: 16px; box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  text-shadow: 0 1px 2px rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.4);
}
.user-meta { display: flex; flex-direction: column; }
.user-name { font-weight: 700; font-size: 14.5px; }
.user-username { font-size: 12.5px; color: #6b7280; margin-top: 2px; }

/* ── Rich Badges ── */
.badge { padding: 4px 10px; border-radius: 6px; font-size: 11.5px; font-weight: 700; font-family: 'Geist Mono'; letter-spacing: 0.05em; }
.badge-admin { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.badge-manager { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.badge-staff { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; }

.pill-badge { padding: 4px 12px; border-radius: 20px; font-size: 12.5px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; }
.pill-active { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.pill-locked { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.dot-active { background: #16a34a; box-shadow: 0 0 0 2px #dcfce7; }
.dot-locked { background: #dc2626; box-shadow: 0 0 0 2px #fef2f2; }

/* ── Detail Pane Header ── */
.detail-hero {
  padding: 36px 24px 24px; position: relative;
  display: flex; flex-direction: column; align-items: center; text-align: center;
  background: linear-gradient(180deg, #f9fafb 0%, #ffffff 100%);
}
.close-detail { position: absolute; top: 16px; right: 16px; }
.hero-avatar {
  width: 84px; height: 84px; border-radius: 24px; display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 800; font-size: 32px; box-shadow: 0 8px 24px rgba(0,0,0,0.1); text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.4);
}
.new-user { background: #f3f4f6; color: #9ca3af; box-shadow: none; border: 2px dashed #d1d5db; text-shadow: none; }
.detail-title { font-size: 24px; font-weight: 800; margin: 0 0 4px 0; color: #111827; letter-spacing: -0.02em; }
.detail-sub { font-size: 14px; color: #6b7280; margin: 0; }

/* ── Detail Body & Forms ── */
.detail-body { background: #f9fafb; flex: 1; overflow-y: auto; min-height: 0; }
.panel-alert { padding: 14px 16px; border-radius: 8px; font-size: 13.5px; display: flex; align-items: flex-start; gap: 8px; font-weight: 500; }
.panel-alert.error { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }

.detail-form { display: flex; flex-direction: column; gap: 22px; }
.form-group label { display: block; font-size: 11.5px; font-weight: 700; color: #4b5563; margin-bottom: 8px; letter-spacing: 0.05em; }
.req { color: #b91c1c; }
.form-group input, .form-group select {
  width: 100%; height: 42px; padding: 0 14px; border-radius: 8px; border: 1px solid #d1d5db;
  font-size: 14.5px; font-family: inherit; transition: all 0.2s; outline: none; background: #ffffff;
  font-weight: 500; color: #111827; box-shadow: inset 0 1px 2px rgba(0,0,0,0.02);
}
.form-group input:focus, .form-group select:focus { border-color: #b91c1c; box-shadow: 0 0 0 3px rgba(185,28,28,0.1); }
.form-group input:disabled, .form-group select:disabled { background: #f3f4f6; color: #9ca3af; cursor: not-allowed; border-color: #e5e7eb; }
.form-group select option { background: #fff; color: #111827; }
.form-hint { display: block; font-size: 12.5px; color: #6b7280; margin-top: 6px; }

/* Danger Zone */
.danger-actions { display: flex; flex-direction: column; gap: 12px; }

.btn-outline-warning, .btn-outline-error {
  background: #ffffff; height: 42px; border-radius: 8px; padding: 0 16px; font-size: 13.5px; font-weight: 600; font-family: inherit;
  display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; transition: all 0.2s; width: 100%;
}
.btn-outline-warning { border: 1px solid #fcd34d; color: #b45309; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.btn-outline-warning:hover { background: #fffbeb; transform: translateY(-1px); border-color: #f59e0b; }
.btn-outline-error { border: 1px solid #fecaca; color: #b91c1c; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.btn-outline-error:hover { background: #fef2f2; transform: translateY(-1px); border-color: #f87171; }

.icon-btn { background: transparent; border: none; cursor: pointer; color: #9ca3af; width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.icon-btn:hover { background: #e5e7eb; color: #111827; transform: scale(1.1); }
.icon-btn .material-symbols-outlined { font-size: 20px; }

/* ── Empty State ── */
.empty-state { padding: 80px 0; text-align: center; }
.empty-ico-box { width: 72px; height: 72px; background: #f3f4f6; border-radius: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; border: 1px solid #e5e7eb;}

/* ── Toast ── */
.toast {
  position: fixed; bottom: 32px; right: 32px; padding: 16px 24px; border-radius: 12px;
  font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 12px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.15); animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 200; background: #ffffff; border: 1px solid #d1d5db; color: #111827;
}
.toast.success { border-color: #bbf7d0; color: #15803d; }
.toast.error { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }

@keyframes slideUp { from { transform: translateY(30px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
</style>
