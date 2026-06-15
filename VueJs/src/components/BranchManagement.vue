<template>
  <div class="branch-management">
    
    <!-- Header & Actions -->
    <div class="page-top staggered-in" style="animation-delay: 0ms">
      <div>
        <h2 class="section-title">Hệ thống Chi nhánh</h2>
        <p class="section-sub">Quản lý mạng lưới kho hàng, ngưỡng tồn kho và thông tin liên hệ.</p>
      </div>
      <button class="btn-primary" @click="openAddPanel">
        <span class="material-symbols-outlined text-[18px]">add_business</span>
        Thêm chi nhánh
      </button>
    </div>

    <!-- Stats Row -->
    <div class="stats-row staggered-in" style="animation-delay: 50ms">
      <div class="stat-card glass-panel group">
        <div class="stat-lbl">TỔNG CHI NHÁNH</div>
        <div class="stat-val mono text-gray-900">{{ branches.length }}</div>
      </div>
      <div class="stat-card glass-panel group">
        <div class="stat-lbl">TRUNG BÌNH CẢNH BÁO TỒN</div>
        <div class="stat-val mono text-gray-700">
          {{ branches.length ? Math.round(branches.reduce((sum, b) => sum + b.lowStockThreshold, 0) / branches.length) : 0 }}
        </div>
      </div>
    </div>

    <!-- Toolbar: Filters & Search -->
    <div class="toolbar glass-panel staggered-in" style="animation-delay: 100ms">
      <div class="search-box">
        <span class="material-symbols-outlined search-ico">search</span>
        <input v-model="filters.search" type="text" placeholder="Tìm theo tên chi nhánh..." />
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
                <th>Chi nhánh</th>
                <th v-if="!activePanel">Ngưỡng tồn thấp</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="(branch, index) in filteredBranches" :key="branch.id" 
                class="clickable-row table-stagger" 
                :style="{ animationDelay: (200 + index * 40) + 'ms' }"
                :class="{ 'active-row': form.id === branch.id }"
                @click="openEditPanel(branch)"
              >
                <td>
                  <div class="branch-cell">
                    <div class="branch-icon shadow-sm">
                      <span class="material-symbols-outlined text-gray-600">storefront</span>
                    </div>
                    <div class="branch-meta">
                      <span class="branch-name text-gray-900">{{ branch.name }}</span>
                      <span class="branch-address text-gray-500 line-clamp-1">{{ branch.address }}</span>
                    </div>
                  </div>
                </td>
                <td v-if="!activePanel">
                  <div class="flex items-center gap-1.5 text-gray-700">
                    <span class="material-symbols-outlined text-[16px] text-orange-500">warning</span>
                    <span class="font-mono font-semibold">{{ branch.lowStockThreshold }}</span>
                  </div>
                </td>
              </tr>
              
              <tr v-if="filteredBranches.length === 0">
                <td :colspan="activePanel ? 1 : 2">
                  <div class="empty-state">
                    <div class="empty-ico-box">
                      <span class="material-symbols-outlined text-gray-500 text-4xl">domain_disabled</span>
                    </div>
                    <h4 class="text-gray-900">Không tìm thấy chi nhánh</h4>
                    <p class="text-gray-500">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
                    <button class="btn-secondary mt-4" @click="clearFilters">Xóa bộ lọc</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Detail Pane (In-flow Form) -->
      <div v-if="activePanel" class="detail-pane glass-panel !p-0 overflow-hidden">
        
        <!-- Header area -->
        <div class="detail-hero relative border-b border-gray-200">
          <div class="absolute inset-0 bg-gradient-to-b from-gray-100 to-transparent pointer-events-none"></div>
          <button class="icon-btn close-detail" @click="closePanel" title="Đóng">
            <span class="material-symbols-outlined">close</span>
          </button>

          <div class="hero-avatar ring-4 ring-white shadow-md bg-white border border-gray-200">
            <span class="material-symbols-outlined text-4xl" :class="isEditMode ? 'text-gray-800' : 'text-gray-400'">
              {{ isEditMode ? 'storefront' : 'add_business' }}
            </span>
          </div>

          <h3 class="detail-title text-gray-900">{{ isEditMode ? form.name : 'Chi nhánh mới' }}</h3>
          <p class="detail-sub mono text-gray-600">{{ isEditMode ? 'Chỉnh sửa' : 'Nhập thông tin bên dưới' }}</p>
        </div>

        <div class="detail-body p-6 bg-gray-50/50">
          <div v-if="panelError" class="panel-alert error">
            <span class="material-symbols-outlined">error</span>
            {{ panelError }}
          </div>

          <form @submit.prevent="submitForm" class="detail-form">
            <div class="form-group">
              <label>TÊN CHI NHÁNH <span class="req">*</span></label>
              <input v-model="form.name" type="text" required placeholder="Nhập tên chi nhánh..." />
            </div>

            <div class="form-group">
              <label>ĐỊA CHỈ CHI TIẾT <span class="req">*</span></label>
              <textarea v-model="form.address" required placeholder="Nhập địa chỉ đầy đủ..." rows="3"></textarea>
            </div>

            <div class="form-group">
              <label>NGƯỠNG CẢNH BÁO TỒN KHO THẤP <span class="req">*</span></label>
              <div class="relative">
                <input v-model.number="form.lowStockThreshold" type="number" min="0" required style="padding-left: 40px;" />
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-orange-500 text-lg pointer-events-none">warning</span>
              </div>
              <span class="form-hint">Hệ thống sẽ báo động nếu sản phẩm có tồn kho ≤ mức này.</span>
            </div>
          </form>

          <!-- Danger Actions (Edit mode only) -->
          <div v-if="isEditMode" class="danger-zone border-t border-gray-200 mt-6 pt-6">
            <h4 class="text-gray-900 font-semibold mb-3">Quản lý chi nhánh</h4>
            <div class="danger-actions">
              <button class="btn-outline-error" @click="deleteBranch(form.id)">
                <span class="material-symbols-outlined">delete</span> Xóa chi nhánh vĩnh viễn
              </button>
            </div>
          </div>
          
        </div>

        <div class="detail-footer p-5 border-t border-gray-200 bg-gray-100 flex justify-end gap-3">
          <button class="btn-secondary" @click="closePanel">Hủy</button>
          <button class="btn-primary" @click="submitForm">
            <span class="material-symbols-outlined text-[18px]">save</span>
            {{ isEditMode ? 'Lưu thay đổi' : 'Tạo chi nhánh' }}
          </button>
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
import { ref, computed, onMounted } from 'vue';
import { api } from '../api';

const branches = ref<any[]>([]);
const filters = ref({ search: '' });
const alert = ref({ show: false, message: '', type: 'success' });

const activePanel = ref(false);
const isEditMode = ref(false);
const panelError = ref('');
const form = ref({ id: 0, name: '', address: '', lowStockThreshold: 5 });

const showAlert = (msg: string, type: 'success' | 'error' = 'success') => {
  alert.value = { show: true, message: msg, type };
  setTimeout(() => { alert.value.show = false; }, 3000);
};

const fetchBranches = async () => {
  try {
    const res = await api.get('/api/branches');
    if (res.ok) {
      branches.value = await res.json();
    }
  } catch (error) {
    showAlert('Không thể kết nối đến máy chủ', 'error');
  }
};

onMounted(() => {
  fetchBranches();
});

const filteredBranches = computed(() => {
  return branches.value.filter(b => {
    const s = filters.value.search.toLowerCase();
    return b.name.toLowerCase().includes(s) || b.address.toLowerCase().includes(s);
  });
});

const clearFilters = () => { filters.value = { search: '' }; };

const openAddPanel = () => {
  activePanel.value = true; isEditMode.value = false; panelError.value = '';
  form.value = { id: 0, name: '', address: '', lowStockThreshold: 5 };
};

const openEditPanel = (branch: any) => {
  if (activePanel.value && form.value.id === branch.id) {
    closePanel();
    return;
  }
  activePanel.value = true; isEditMode.value = true; panelError.value = '';
  form.value = { ...branch };
};

const closePanel = () => { activePanel.value = false; form.value.id = 0; };

const deleteBranch = async (id: number) => {
  const branch = branches.value.find(b => b.id === id);
  if (!branch) return;
  if (confirm(`Bạn có chắc chắn muốn xóa chi nhánh ${branch.name}? Hành động này không thể hoàn tác.`)) {
    try {
      const res = await api.delete(`/api/branches/${id}`);
      if (res.ok) {
        showAlert(`Đã xóa chi nhánh ${branch.name}`);
        closePanel();
        fetchBranches();
      } else {
        const data = await res.json();
        showAlert(data.message || 'Chi nhánh này đang có dữ liệu, không thể xóa!', 'error');
      }
    } catch (e) {
      showAlert('Lỗi kết nối đến máy chủ', 'error');
    }
  }
};

const submitForm = async () => {
  panelError.value = '';
  if (!form.value.name || !form.value.address) return panelError.value = 'Vui lòng điền đầy đủ thông tin.';
  if (form.value.lowStockThreshold < 0) return panelError.value = 'Ngưỡng tồn kho không được âm.';

  try {
    let res;
    if (isEditMode.value) {
      res = await api.put(`/api/branches/${form.value.id}`, {
        name: form.value.name,
        address: form.value.address,
        lowStockThreshold: form.value.lowStockThreshold
      });
    } else {
      res = await api.post('/api/branches', {
        name: form.value.name,
        address: form.value.address,
        lowStockThreshold: form.value.lowStockThreshold
      });
    }

    if (res.ok) {
      showAlert(isEditMode.value ? 'Đã cập nhật chi nhánh' : 'Đã tạo chi nhánh mới');
      closePanel();
      fetchBranches();
    } else {
      const data = await res.json();
      panelError.value = data.message || 'Lưu dữ liệu thất bại';
    }
  } catch (error) {
    panelError.value = 'Lỗi kết nối máy chủ.';
  }
};
</script>

<style scoped>
/* ── Soft Muted Light Theme ── */
* { box-sizing: border-box; }
.mono { font-family: 'Geist Mono', monospace; }

.branch-management {
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
.stats-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
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

/* ── Split Layout ── */
.split-layout { display: flex; gap: 24px; align-items: flex-start; }
.list-pane { flex: 1; min-width: 0; transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); border-color: #e5e7eb; }
.detail-pane {
  width: 460px; flex-shrink: 0; display: flex; flex-direction: column; 
  position: sticky; top: 84px; max-height: calc(100vh - 104px);
  animation: slideInRight 0.4s cubic-bezier(0.16, 1, 0.3, 1); border-color: #d1d5db; box-shadow: -4px 0 24px rgba(0,0,0,0.03);
}
@keyframes slideInRight { from { opacity: 0; transform: translateX(30px) scale(0.98); } to { opacity: 1; transform: translateX(0) scale(1); } }

/* ── Table ── */
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

.branch-cell { display: flex; align-items: center; gap: 14px; }
.branch-icon {
  width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
  background: #f3f4f6; border: 1px solid #e5e7eb; color: #4b5563;
}
.active-row .branch-icon { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }

.branch-meta { display: flex; flex-direction: column; }
.branch-name { font-weight: 700; font-size: 14.5px; }
.branch-address { font-size: 12.5px; color: #6b7280; margin-top: 2px; }
.line-clamp-1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }

/* ── Detail Pane Header ── */
.detail-hero {
  padding: 36px 24px 24px; position: relative;
  display: flex; flex-direction: column; align-items: center; text-align: center;
  background: linear-gradient(180deg, #f9fafb 0%, #ffffff 100%);
}
.close-detail { position: absolute; top: 16px; right: 16px; }
.hero-avatar {
  width: 84px; height: 84px; border-radius: 24px; display: flex; align-items: center; justify-content: center;
  margin-bottom: 20px;
}
.detail-title { font-size: 24px; font-weight: 800; margin: 0 0 4px 0; color: #111827; letter-spacing: -0.02em; }
.detail-sub { font-size: 14px; color: #6b7280; margin: 0; }

/* ── Detail Body & Forms ── */
.detail-body { background: #f9fafb; flex: 1; overflow-y: auto; min-height: 0; }
.panel-alert { padding: 14px 16px; border-radius: 8px; font-size: 13.5px; display: flex; align-items: flex-start; gap: 8px; font-weight: 500; }
.panel-alert.error { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }

.detail-form { display: flex; flex-direction: column; gap: 22px; }
.form-group label { display: block; font-size: 11.5px; font-weight: 700; color: #4b5563; margin-bottom: 8px; letter-spacing: 0.05em; }
.req { color: #b91c1c; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 12px 14px; border-radius: 8px; border: 1px solid #d1d5db;
  font-size: 14.5px; font-family: inherit; transition: all 0.2s; outline: none; background: #ffffff;
  font-weight: 500; color: #111827; box-shadow: inset 0 1px 2px rgba(0,0,0,0.02);
}
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: #b91c1c; box-shadow: 0 0 0 3px rgba(185,28,28,0.1); }
.form-hint { display: block; font-size: 12.5px; color: #6b7280; margin-top: 6px; }

/* Danger Zone */
.danger-actions { display: flex; flex-direction: column; gap: 12px; }

.btn-outline-error {
  background: #ffffff; height: 42px; border-radius: 8px; padding: 0 16px; font-size: 13.5px; font-weight: 600; font-family: inherit;
  display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; transition: all 0.2s; width: 100%;
}
.btn-outline-error { border: 1px solid #fecaca; color: #b91c1c; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }
.btn-outline-error:hover { background: #fef2f2; transform: translateY(-1px); border-color: #f87171; }

.icon-btn { background: transparent; border: none; cursor: pointer; color: #9ca3af; width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.icon-btn:hover { background: #e5e7eb; color: #111827; transform: scale(1.1); }

/* ── Empty State ── */
.empty-state { padding: 80px 0; text-align: center; }
.empty-ico-box { width: 72px; height: 72px; background: #f3f4f6; border-radius: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; border: 1px solid #e5e7eb; }

/* ── Toast ── */
.toast {
  position: fixed; bottom: 32px; right: 32px; padding: 16px 24px; border-radius: 12px;
  font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 12px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.15); animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 200; background: #ffffff; border: 1px solid #d1d5db; color: #111827;
}
.toast.error { background: #fef2f2; color: #b91c1c; border-color: #fecaca; }

@keyframes slideUp { from { transform: translateY(30px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
</style>
