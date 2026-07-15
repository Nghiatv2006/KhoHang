
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'

const toast = useToast()

// ─── State ────────────────────────────────────────────────────────────────────
const history = ref<any[]>([])
const loading = ref(true)
const exporting = ref(false)
const restoring = ref(false)
const triggeringAuto = ref(false)

const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')

// File Upload State — Branch
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const isDragging = ref(false)

// File Upload State — System Config (Admin)
const sysFileInput = ref<HTMLInputElement | null>(null)
const sysSelectedFile = ref<File | null>(null)
const sysDragging = ref(false)
const sysHistory = ref<any[]>([])
const sysLoading = ref(false)
const sysExporting = ref(false)
const sysRestoring = ref(false)

// Confirmation Modals State — Branch
const showRestoreConfirm = ref(false)
const restoreSource = ref<'file' | 'history'>('file')
const targetBackupItem = ref<any>(null)

const showDeleteConfirm = ref(false)
const targetDeleteId = ref<number | null>(null)

// Confirmation Modals State — System
const showSysRestoreConfirm = ref(false)
const sysRestoreSource = ref<'file' | 'history'>('file')
const sysTargetItem = ref<any>(null)

const showSysDeleteConfirm = ref(false)
const sysTargetDeleteId = ref<number | null>(null)

// ─── Methods ──────────────────────────────────────────────────────────────────
async function loadHistory() {
  loading.value = true
  try {
    const res = await api.get('/api/backup/history')
    if (res.ok) {
      history.value = await res.json()
    } else {
      const err = await res.json().catch(() => ({}))
      if (res.status !== 423) toast.error(err.message || 'Không thể tải lịch sử sao lưu.')
    }
  } catch (e) {
    toast.error('Lỗi kết nối máy chủ.')
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const res = await api.get('/api/backup/export')
    if (res.ok) {
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const disposition = res.headers.get('content-disposition')
      let filename = `backup_branch_${user.value?.branchId || ''}_${new Date().toISOString().slice(0, 10)}.wbk`
      if (disposition && disposition.indexOf('attachment') !== -1) {
        const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition)
        if (matches?.[1]) filename = matches[1].replace(/['"]/g, '')
      }
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
      toast.success('Tạo và tải file sao lưu thành công!')
      loadHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Lỗi xuất file sao lưu.')
    }
  } catch (e) {
    toast.error('Lỗi kết nối khi tải file sao lưu.')
  } finally {
    exporting.value = false
  }
}

function onDragOver(e: DragEvent) { e.preventDefault(); isDragging.value = true }
function onDragLeave() { isDragging.value = false }
function onDrop(e: DragEvent) {
  e.preventDefault(); isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (!file) return
  if (file.name.endsWith('.wbk') || file.name.endsWith('.json')) {
    selectedFile.value = file
  } else {
    toast.error('Chỉ hỗ trợ tệp tin .wbk (mã hoá) hoặc .json (bản cũ).')
  }
}
function triggerFileSelect() { fileInput.value?.click() }
function onFileSelected(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) selectedFile.value = f
}
function removeSelectedFile() {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function confirmRestoreFromFile() {
  if (!selectedFile.value) return
  restoreSource.value = 'file'
  showRestoreConfirm.value = true
}
function confirmRestoreFromHistory(item: any) {
  restoreSource.value = 'history'
  targetBackupItem.value = item
  showRestoreConfirm.value = true
}
function confirmDelete(id: number) {
  targetDeleteId.value = id
  showDeleteConfirm.value = true
}

async function executeRestore() {
  showRestoreConfirm.value = false
  restoring.value = true
  try {
    let res
    if (restoreSource.value === 'file') {
      if (!selectedFile.value) return
      const formData = new FormData()
      formData.append('file', selectedFile.value)
      res = await api.upload('/api/backup/import', formData)
    } else {
      if (!targetBackupItem.value) return
      res = await api.post(`/api/backup/restore/${targetBackupItem.value.id}`, {})
    }
    if (res.ok) {
      toast.success('Khôi phục dữ liệu chi nhánh thành công!')
      removeSelectedFile()
      loadHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Khôi phục dữ liệu thất bại.')
    }
  } catch (e) {
    toast.error('Lỗi kết nối khi khôi phục dữ liệu.')
  } finally {
    restoring.value = false
    targetBackupItem.value = null
  }
}

async function executeDelete() {
  if (targetDeleteId.value === null) return
  showDeleteConfirm.value = false
  try {
    const res = await api.delete(`/api/backup/history/${targetDeleteId.value}`)
    if (res.ok) {
      toast.success('Xóa bản sao lưu thành công.')
      loadHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Xóa bản sao lưu thất bại.')
    }
  } catch (e) {
    toast.error('Lỗi kết nối máy chủ.')
  } finally {
    targetDeleteId.value = null
  }
}

async function triggerScheduledBackup() {
  if (triggeringAuto.value) return
  triggeringAuto.value = true
  try {
    const res = await api.post('/api/backup/trigger-scheduled', {})
    if (res.ok) {
      const data = await res.json()
      toast.success(data.message || 'Trigger sao lưu tự động thành công.')
      loadHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Không thể trigger sao lưu.')
    }
  } catch (e) {
    toast.error('Lỗi kết nối máy chủ.')
  } finally {
    triggeringAuto.value = false
  }
}

function formatBytes(bytes: number, decimals = 1) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i]
}
function formatDateTime(dt: string) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'medium' })
}

// ─── Admin: System Config Methods ────────────────────────────────────────────
async function loadSysHistory() {
  sysLoading.value = true
  try {
    const res = await api.get('/api/backup/system/history')
    if (res.ok) sysHistory.value = await res.json()
  } catch (e) {
    toast.error('Lỗi tải lịch sử backup hệ thống.')
  } finally {
    sysLoading.value = false
  }
}

async function handleSysExport() {
  if (sysExporting.value) return
  sysExporting.value = true
  try {
    const res = await api.get('/api/backup/system/export')
    if (res.ok) {
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const disposition = res.headers.get('content-disposition')
      let filename = `system_config_${new Date().toISOString().slice(0,10)}.wbk`
      if (disposition) {
        const m = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition)
        if (m?.[1]) filename = m[1].replace(/["']/g, '')
      }
      a.download = filename
      document.body.appendChild(a); a.click(); a.remove()
      window.URL.revokeObjectURL(url)
      toast.success('Tải file System Config thành công!')
      loadSysHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Lỗi xuất System Config.')
    }
  } catch { toast.error('Lỗi kết nối.') }
  finally { sysExporting.value = false }
}

function onSysDragOver(e: DragEvent) { e.preventDefault(); sysDragging.value = true }
function onSysDragLeave() { sysDragging.value = false }
function onSysDrop(e: DragEvent) {
  e.preventDefault(); sysDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (!file) return
  if (file.name.endsWith('.wbk')) sysSelectedFile.value = file
  else toast.error('Chỉ hỗ trợ tệp tin .wbk System Config.')
}
function triggerSysFileSelect() { sysFileInput.value?.click() }
function onSysFileSelected(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) sysSelectedFile.value = f
}
function removeSysSelectedFile() {
  sysSelectedFile.value = null
  if (sysFileInput.value) sysFileInput.value.value = ''
}

function confirmSysRestoreFromFile() {
  if (!sysSelectedFile.value) return
  sysRestoreSource.value = 'file'
  showSysRestoreConfirm.value = true
}
function confirmSysRestoreFromHistory(item: any) {
  sysRestoreSource.value = 'history'
  sysTargetItem.value = item
  showSysRestoreConfirm.value = true
}
function confirmSysDelete(id: number) {
  sysTargetDeleteId.value = id
  showSysDeleteConfirm.value = true
}

async function executeSysRestore() {
  showSysRestoreConfirm.value = false
  sysRestoring.value = true
  try {
    let res
    if (sysRestoreSource.value === 'file') {
      if (!sysSelectedFile.value) return
      const fd = new FormData()
      fd.append('file', sysSelectedFile.value)
      res = await api.upload('/api/backup/system/import', fd)
    } else {
      if (!sysTargetItem.value) return
      res = await api.post(`/api/backup/system/restore/${sysTargetItem.value.id}`, {})
    }
    if (res.ok) {
      toast.success('Phục hồi cấu hình hệ thống thành công!')
      removeSysSelectedFile()
      loadSysHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Phục hồi thất bại.')
    }
  } catch { toast.error('Lỗi kết nối.') }
  finally { sysRestoring.value = false; sysTargetItem.value = null }
}

async function executeSysDelete() {
  if (sysTargetDeleteId.value === null) return
  showSysDeleteConfirm.value = false
  try {
    const res = await api.delete(`/api/backup/system/history/${sysTargetDeleteId.value}`)
    if (res.ok) { toast.success('Đã xóa bản sao lưu hệ thống.'); loadSysHistory() }
    else { const err = await res.json().catch(() => ({})); toast.error(err.message || 'Xóa thất bại.') }
  } catch { toast.error('Lỗi kết nối.') }
  finally { sysTargetDeleteId.value = null }
}

// Confirmation Modals State — Wipe
const showWipeConfirm = ref(false)
const wiping = ref(false)

async function executeBranchWipe() {
  showWipeConfirm.value = false
  wiping.value = true
  try {
    const res = await api.post('/api/backup/wipe-branch-data', {})
    if (res.ok) {
      toast.success('Đã xóa toàn bộ dữ liệu giao dịch chi nhánh!')
      loadHistory()
    } else {
      const err = await res.json().catch(() => ({}))
      toast.error(err.message || 'Xóa dữ liệu thất bại.')
    }
  } catch (e) {
    toast.error('Lỗi kết nối máy chủ.')
  } finally {
    wiping.value = false
  }
}

onMounted(() => {
  loadHistory()
  if (isAdmin.value) loadSysHistory()
})
</script>

<template>
  <div class="bk">

    <!-- RESTORE OVERLAY -->
    <Transition name="bk-overlay">
      <div v-if="restoring" class="bk-overlay">
        <div class="bk-overlay__inner">
          <div class="bk-spinner">
            <div class="bk-spinner__ring bk-spinner__ring--1"></div>
            <div class="bk-spinner__ring bk-spinner__ring--2"></div>
            <i class="fas fa-database bk-spinner__icon"></i>
          </div>
          <h2 class="bk-overlay__title">Đang khôi phục dữ liệu...</h2>
          <p class="bk-overlay__desc">
            Hệ thống đang giải mã và phục hồi dữ liệu. Chi nhánh tạm thời bị khóa giao dịch.<br>
            <strong>Vui lòng không đóng hoặc tải lại trang!</strong>
          </p>
          <div class="bk-progress"><div class="bk-progress__bar"></div></div>
        </div>
      </div>
    </Transition>

    <!-- PAGE HEADER -->
    <header class="bk-header">
      <div class="bk-header__left">
        <div class="bk-header__icon"><i class="fas fa-database"></i></div>
        <div>
          <h1 class="bk-header__title">Sao lưu &amp; Phục hồi</h1>
          <p class="bk-header__sub">{{ isAdmin ? 'Quản lý cấu hình toàn hệ thống' : 'Quản lý dữ liệu chi nhánh' }}</p>
        </div>
      </div>
      <div class="bk-header__actions">
        <button v-if="isAdmin" @click="triggerScheduledBackup" :disabled="triggeringAuto" class="bk-btn bk-btn--accent">
          <i class="fas fa-magic" :class="{ 'fa-spin': triggeringAuto }"></i>
          <span>Auto Backup</span>
        </button>
        <button @click="isAdmin ? loadSysHistory() : loadHistory()" class="bk-btn bk-btn--ghost">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': isAdmin ? sysLoading : loading }"></i>
          <span>Làm mới</span>
        </button>
      </div>
    </header>

    <!-- SECURITY STRIP -->
    <div class="bk-strip">
      <span class="bk-strip__tag"><i class="fas fa-lock"></i> AES-256-GCM</span>
      <span class="bk-strip__tag"><i class="fas fa-fingerprint"></i> HMAC-SHA256</span>
      <span class="bk-strip__tag"><i class="fas fa-file-archive"></i> .wbk</span>
      <span class="bk-strip__tag"><i class="fas fa-compress-arrows-alt"></i> GZIP</span>
      <span class="bk-strip__tag"><i class="fas fa-calendar-alt"></i> 14 ngày</span>
    </div>

    <!-- WARNING -->
    <div class="bk-alert bk-alert--warn">
      <i class="fas fa-shield-alt"></i>
      <div>
        <strong>Lưu ý:</strong> Khôi phục sẽ <strong>xoá sạch và ghi đè</strong>
        toàn bộ dữ liệu giao dịch chi nhánh. Chi nhánh bị <em>khoá tạm thời</em> trong suốt quá trình.
      </div>
    </div>

    <!-- MAIN GRID -->
    <div class="bk-grid">
      <!-- LEFT: Operations -->
      <div class="bk-ops">
        <!-- Backup Card -->
        <section class="bk-card">
          <div class="bk-card__head">
            <div class="bk-card__icon bk-card__icon--accent"><i class="fas fa-cloud-download-alt"></i></div>
            <div>
              <h2 class="bk-card__title">SAO LƯU DỮ LIỆU</h2>
              <p class="bk-card__sub">Tải file .wbk mã hoá về máy</p>
            </div>
          </div>
          <p class="bk-card__desc">
            Toàn bộ dữ liệu chi nhánh được ký bằng <strong>HMAC-SHA256</strong>
            và mã hoá bằng <strong>AES-256-GCM</strong> trước khi tải xuống.
          </p>
          <button @click="handleExport" :disabled="exporting" class="bk-action bk-action--accent">
            <i class="fas fa-file-download" :class="{ 'bk-bounce': exporting }"></i>
            <span>{{ exporting ? 'Đang tạo bản sao lưu...' : 'Tải xuống tệp tin sao lưu' }}</span>
          </button>
        </section>

        <!-- Restore from File Card -->
        <section class="bk-card">
          <div class="bk-card__head">
            <div class="bk-card__icon bk-card__icon--emerald"><i class="fas fa-cloud-upload-alt"></i></div>
            <div>
              <h2 class="bk-card__title">PHỤC HỒI TỪ TỆP TIN</h2>
              <p class="bk-card__sub">Upload file .wbk để restore</p>
            </div>
          </div>
          <p class="bk-card__desc">
            Kéo thả hoặc chọn tệp <code>.wbk</code> từ máy tính.
            Hệ thống tự động giải mã và xác thực trước khi ghi đè dữ liệu.
          </p>

          <div class="bk-drop" :class="{ 'bk-drop--active': isDragging, 'bk-drop--filled': selectedFile }"
            @dragover="onDragOver" @dragleave="onDragLeave" @drop="onDrop" @click="triggerFileSelect">
            <input type="file" ref="fileInput" @change="onFileSelected" accept=".wbk,.json" class="hidden" />
            <div v-if="!selectedFile" class="bk-drop__empty">
              <i class="fas fa-cloud-upload-alt bk-drop__icon"></i>
              <p class="bk-drop__label">Kéo thả hoặc <span class="bk-drop__link">click để chọn</span></p>
              <p class="bk-drop__hint">Hỗ trợ <code>.wbk</code> &amp; <code>.json</code></p>
            </div>
            <div v-else class="bk-drop__file">
              <div class="bk-drop__file-icon"><i class="fas fa-file-shield"></i></div>
              <div class="bk-drop__file-info">
                <div class="bk-drop__name" :title="selectedFile.name">{{ selectedFile.name }}</div>
                <div class="bk-drop__size">{{ formatBytes(selectedFile.size) }}</div>
              </div>
              <button @click.stop="removeSelectedFile" class="bk-drop__remove"><i class="fas fa-times"></i></button>
            </div>
          </div>

          <button @click="confirmRestoreFromFile" :disabled="!selectedFile || restoring" class="bk-action bk-action--emerald">
            <i class="fas fa-undo-alt"></i>
            <span>Phục hồi từ tệp tin</span>
          </button>
        </section>

        <!-- Wipe Branch Data Card -->
        <section class="bk-card bk-card--danger">
          <div class="bk-card__head">
            <div class="bk-card__icon bk-card__icon--danger"><i class="fas fa-skull-crossbones"></i></div>
            <div>
              <h2 class="bk-card__title">XÓA DỮ LIỆU (DEMO)</h2>
              <p class="bk-card__sub">Test khôi phục từ bản sao lưu</p>
            </div>
          </div>
          <p class="bk-card__desc">
            Xóa <strong>toàn bộ dữ liệu giao dịch</strong> của chi nhánh.
            <strong>Chỉ giữ lại tài khoản bạn đang đăng nhập.</strong>
          </p>
          <button @click="showWipeConfirm = true" :disabled="wiping" class="bk-action bk-action--danger">
            <i class="fas fa-trash-alt" :class="{ 'fa-spin': wiping }"></i>
            <span>{{ wiping ? 'Đang xóa dữ liệu...' : 'Xóa toàn bộ dữ liệu chi nhánh' }}</span>
          </button>
        </section>
      </div>

      <!-- RIGHT: Server History -->
      <section class="bk-card bk-history">
        <div class="bk-history__head">
          <div class="bk-card__icon bk-card__icon--slate"><i class="fas fa-history"></i></div>
          <div>
            <h2 class="bk-card__title">BẢN SAO LƯU TRÊN MÁY CHỦ</h2>
            <p class="bk-card__sub">Auto (14 ngày) &amp; Manual của chi nhánh</p>
          </div>
        </div>
        <div v-if="loading" class="bk-skel">
          <div v-for="i in 5" :key="i" class="bk-skel__row"></div>
        </div>
        <div v-else-if="history.length === 0" class="bk-empty">
          <i class="fas fa-database bk-empty__icon"></i>
          <div class="bk-empty__title">Chưa có bản sao lưu nào</div>
          <div class="bk-empty__sub">Bản sao lưu tự động (01:00 AM) và thủ công sẽ hiện ở đây</div>
        </div>
        <div v-else class="bk-list">
          <div v-for="item in history" :key="item.id" class="bk-row">
            <div class="bk-row__type" :class="item.backupType === 'AUTO' ? 'bk-row__type--auto' : 'bk-row__type--manual'">
              <i :class="item.backupType === 'AUTO' ? 'fas fa-robot' : 'fas fa-user'"></i>
            </div>
            <div class="bk-row__info">
              <div class="bk-row__name" :title="item.filename">{{ item.filename }}</div>
              <div class="bk-row__meta">
                <span><i class="fas fa-clock"></i> {{ formatDateTime(item.createdAt) }}</span>
                <span><i class="fas fa-weight"></i> {{ formatBytes(item.fileSize) }}</span>
                <span><i class="fas fa-user-circle"></i> {{ item.createdBy ? item.createdBy.fullName : 'Hệ thống' }}</span>
              </div>
            </div>
            <span class="bk-badge" :class="item.backupType === 'AUTO' ? 'bk-badge--auto' : 'bk-badge--manual'">
              {{ item.backupType === 'AUTO' ? 'Tự động' : 'Thủ công' }}
            </span>
            <div class="bk-row__actions">
              <button @click="confirmRestoreFromHistory(item)" class="bk-row-btn bk-row-btn--restore" title="Khôi phục từ bản này">
                <i class="fas fa-undo-alt"></i><span>Khôi phục</span>
              </button>
              <button @click="confirmDelete(item.id)" class="bk-row-btn bk-row-btn--delete" title="Xóa bản sao lưu">
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>
          </div>
        </div>
        <div v-if="!loading" class="bk-history__foot">
          <span><i class="fas fa-layer-group"></i> {{ history.length }} bản sao lưu</span>
          <span><i class="fas fa-calendar-times"></i> Tự động dọn dẹp sau 14 ngày</span>
        </div>
      </section>
    </div>

    <!-- ADMIN: SYSTEM CONFIG -->
    <template v-if="isAdmin">
      <Transition name="bk-overlay">
        <div v-if="sysRestoring" class="bk-overlay">
          <div class="bk-overlay__inner">
            <div class="bk-spinner">
              <div class="bk-spinner__ring bk-spinner__ring--1"></div>
              <div class="bk-spinner__ring bk-spinner__ring--2"></div>
              <i class="fas fa-server bk-spinner__icon"></i>
            </div>
            <h2 class="bk-overlay__title">Đang phục hồi cấu hình hệ thống...</h2>
            <p class="bk-overlay__desc">
              Hệ thống đang cập nhật lại danh mục, sản phẩm, chi nhánh và tài khoản Admin.<br>
              <strong>Vui lòng không đóng hoặc tải lại trang!</strong>
            </p>
            <div class="bk-progress"><div class="bk-progress__bar"></div></div>
          </div>
        </div>
      </Transition>

      <div class="bk-alert bk-alert--info">
        <i class="fas fa-info-circle"></i>
        <div>
          Phục hồi System Config sẽ <strong>UPSERT</strong> lại danh mục, sản phẩm, chi nhánh và nhân viên.
          Dữ liệu giao dịch kho <em>không bị ảnh hưởng</em>.
        </div>
      </div>

      <div class="bk-grid">
        <div class="bk-ops">
          <section class="bk-card">
            <div class="bk-card__head">
              <div class="bk-card__icon bk-card__icon--accent"><i class="fas fa-cloud-download-alt"></i></div>
              <div>
                <h2 class="bk-card__title">SAO LƯU CẤU HÌNH HỆ THỐNG</h2>
                <p class="bk-card__sub">Tải file .wbk mã hoá về máy</p>
              </div>
            </div>
            <p class="bk-card__desc">
              Chi nhánh, danh mục, sản phẩm và tài khoản — ký <strong>HMAC-SHA256</strong> và mã hoá <strong>AES-256-GCM</strong>.
            </p>
            <button @click="handleSysExport" :disabled="sysExporting" class="bk-action bk-action--accent">
              <i class="fas fa-file-download" :class="{ 'bk-bounce': sysExporting }"></i>
              <span>{{ sysExporting ? 'Đang tạo bản sao lưu...' : 'Tải xuống System Config' }}</span>
            </button>
          </section>

          <section class="bk-card">
            <div class="bk-card__head">
              <div class="bk-card__icon bk-card__icon--amber"><i class="fas fa-cloud-upload-alt"></i></div>
              <div>
                <h2 class="bk-card__title">PHỤC HỒI TỪ TỆP TIN</h2>
                <p class="bk-card__sub">Upload file .wbk để restore hệ thống</p>
              </div>
            </div>
            <p class="bk-card__desc">
              Kéo thả hoặc chọn tệp <code>.wbk</code> từ máy tính.
              Hệ thống xác thực HMAC trước khi UPSERT lại cấu hình.
            </p>
            <div class="bk-drop" :class="{ 'bk-drop--active': sysDragging, 'bk-drop--filled': sysSelectedFile }"
              @dragover="onSysDragOver" @dragleave="onSysDragLeave" @drop="onSysDrop" @click="triggerSysFileSelect">
              <input type="file" ref="sysFileInput" @change="onSysFileSelected" accept=".wbk" class="hidden" />
              <div v-if="!sysSelectedFile" class="bk-drop__empty">
                <i class="fas fa-cloud-upload-alt bk-drop__icon"></i>
                <p class="bk-drop__label">Kéo thả hoặc <span class="bk-drop__link">click để chọn</span></p>
                <p class="bk-drop__hint">Chỉ hỗ trợ <code>.wbk</code> System Config</p>
              </div>
              <div v-else class="bk-drop__file">
                <div class="bk-drop__file-icon"><i class="fas fa-file-shield"></i></div>
                <div class="bk-drop__file-info">
                  <div class="bk-drop__name" :title="sysSelectedFile.name">{{ sysSelectedFile.name }}</div>
                  <div class="bk-drop__size">{{ formatBytes(sysSelectedFile.size) }}</div>
                </div>
                <button @click.stop="removeSysSelectedFile" class="bk-drop__remove"><i class="fas fa-times"></i></button>
              </div>
            </div>
            <button @click="confirmSysRestoreFromFile" :disabled="!sysSelectedFile || sysRestoring" class="bk-action bk-action--amber">
              <i class="fas fa-undo-alt"></i>
              <span>Phục hồi từ tệp tin</span>
            </button>
          </section>
        </div>

        <section class="bk-card bk-history">
          <div class="bk-history__head">
            <div class="bk-card__icon bk-card__icon--slate"><i class="fas fa-history"></i></div>
            <div>
              <h2 class="bk-card__title">BẢN SAO LƯU HỆ THỐNG</h2>
              <p class="bk-card__sub">System Config đã lưu trên máy chủ</p>
            </div>
          </div>
          <div v-if="sysLoading" class="bk-skel">
            <div v-for="i in 5" :key="i" class="bk-skel__row"></div>
          </div>
          <div v-else-if="sysHistory.length === 0" class="bk-empty">
            <i class="fas fa-server bk-empty__icon"></i>
            <div class="bk-empty__title">Chưa có bản sao lưu nào</div>
            <div class="bk-empty__sub">Bấm "Tải xuống System Config" để tạo bản đầu tiên</div>
          </div>
          <div v-else class="bk-list">
            <div v-for="item in sysHistory" :key="item.id" class="bk-row">
              <div class="bk-row__type bk-row__type--system"><i class="fas fa-server"></i></div>
              <div class="bk-row__info">
                <div class="bk-row__name" :title="item.filename">{{ item.filename }}</div>
                <div class="bk-row__meta">
                  <span><i class="fas fa-clock"></i> {{ formatDateTime(item.createdAt) }}</span>
                  <span><i class="fas fa-weight"></i> {{ formatBytes(item.fileSize) }}</span>
                  <span><i class="fas fa-user-shield"></i> {{ item.createdBy ? item.createdBy.fullName : 'Hệ thống' }}</span>
                </div>
              </div>
              <span class="bk-badge bk-badge--manual">Thủ công</span>
              <div class="bk-row__actions">
                <button @click="confirmSysRestoreFromHistory(item)" class="bk-row-btn bk-row-btn--restore" title="Khôi phục">
                  <i class="fas fa-undo-alt"></i><span>Khôi phục</span>
                </button>
                <button @click="confirmSysDelete(item.id)" class="bk-row-btn bk-row-btn--delete" title="Xóa">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>
          </div>
          <div v-if="!sysLoading" class="bk-history__foot">
            <span><i class="fas fa-layer-group"></i> {{ sysHistory.length }} bản sao lưu</span>
            <span><i class="fas fa-shield-alt"></i> AES-256-GCM</span>
          </div>
        </section>
      </div>

      <!-- System Restore Confirm Modal -->
      <Transition name="bk-modal">
        <div v-if="showSysRestoreConfirm" class="bk-backdrop" @click.self="showSysRestoreConfirm = false">
          <div class="bk-modal">
            <div class="bk-modal__head bk-modal__head--accent">
              <div class="bk-modal__head-icon"><i class="fas fa-server"></i></div>
              <div>
                <h3 class="bk-modal__title">Xác nhận Phục hồi System Config</h3>
                <p class="bk-modal__sub">Kiểm tra kỹ trước khi xác nhận</p>
              </div>
              <button @click="showSysRestoreConfirm = false" class="bk-modal__close"><i class="fas fa-times"></i></button>
            </div>
            <div class="bk-modal__body">
              <div class="bk-info">
                <div class="bk-info__row">
                  <span class="bk-info__label">Nguồn</span>
                  <span class="bk-info__value">{{ sysRestoreSource === 'file' ? 'File cục bộ tải lên' : 'Bản lưu trên Server' }}</span>
                </div>
                <template v-if="sysRestoreSource === 'file' && sysSelectedFile">
                  <div class="bk-info__row">
                    <span class="bk-info__label">Tên file</span>
                    <span class="bk-info__value bk-info__value--mono">{{ sysSelectedFile.name }}</span>
                  </div>
                </template>
                <template v-if="sysRestoreSource === 'history' && sysTargetItem">
                  <div class="bk-info__row">
                    <span class="bk-info__label">Tên file</span>
                    <span class="bk-info__value bk-info__value--mono">{{ sysTargetItem.filename }}</span>
                  </div>
                  <div class="bk-info__row">
                    <span class="bk-info__label">Ngày tạo</span>
                    <span class="bk-info__value">{{ formatDateTime(sysTargetItem.createdAt) }}</span>
                  </div>
                </template>
                <div class="bk-info__row">
                  <span class="bk-info__label">Phạm vi</span>
                  <span class="bk-info__value">Branches, Categories, Products, Users</span>
                </div>
              </div>
              <div class="bk-alert bk-alert--warn" style="margin:0">
                <i class="fas fa-exclamation-triangle"></i>
                <span>Hệ thống sẽ UPSERT lại dữ liệu cấu hình. Dữ liệu giao dịch kho <strong>không bị xóa</strong>.</span>
              </div>
            </div>
            <div class="bk-modal__foot">
              <button @click="showSysRestoreConfirm = false" class="bk-btn bk-btn--cancel">Huỷ bỏ</button>
              <button @click="executeSysRestore" class="bk-btn bk-btn--confirm-accent">
                <i class="fas fa-check-circle"></i> Xác nhận &amp; Phục hồi
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- System Delete Confirm Modal -->
      <Transition name="bk-modal">
        <div v-if="showSysDeleteConfirm" class="bk-backdrop" @click.self="showSysDeleteConfirm = false">
          <div class="bk-modal bk-modal--sm">
            <div class="bk-modal__body bk-modal__body--center">
              <div class="bk-del-icon"><i class="fas fa-server"></i></div>
              <h3 class="bk-modal__title" style="text-align:center">Xóa bản sao lưu hệ thống?</h3>
              <p class="bk-del-desc">Tệp tin trên ổ đĩa máy chủ sẽ bị xóa vĩnh viễn.</p>
            </div>
            <div class="bk-modal__foot">
              <button @click="showSysDeleteConfirm = false" class="bk-btn bk-btn--cancel">Huỷ</button>
              <button @click="executeSysDelete" class="bk-btn bk-btn--confirm-danger">
                <i class="fas fa-trash-alt"></i> Xác nhận xoá
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </template>

    <!-- RESTORE CONFIRM MODAL -->
    <Transition name="bk-modal">
      <div v-if="showRestoreConfirm" class="bk-backdrop" @click.self="showRestoreConfirm = false">
        <div class="bk-modal">
          <div class="bk-modal__head bk-modal__head--danger">
            <div class="bk-modal__head-icon"><i class="fas fa-exclamation-triangle"></i></div>
            <div>
              <h3 class="bk-modal__title">Cảnh báo khôi phục dữ liệu</h3>
              <p class="bk-modal__sub bk-modal__sub--danger">Thao tác có nguy cơ mất dữ liệu hiện tại</p>
            </div>
          </div>
          <div class="bk-modal__body">
            <div class="bk-info">
              <div class="bk-info__row">
                <span class="bk-info__label">Nguồn khôi phục</span>
                <span class="bk-info__value">{{ restoreSource === 'file' ? 'Tệp tin cục bộ tải lên' : 'Bản lưu trên máy chủ' }}</span>
              </div>
              <template v-if="restoreSource === 'file' && selectedFile">
                <div class="bk-info__row">
                  <span class="bk-info__label">Tên file</span>
                  <span class="bk-info__value bk-info__value--mono">{{ selectedFile.name }}</span>
                </div>
                <div class="bk-info__row">
                  <span class="bk-info__label">Kích thước</span>
                  <span class="bk-info__value">{{ formatBytes(selectedFile.size) }}</span>
                </div>
              </template>
              <template v-if="restoreSource === 'history' && targetBackupItem">
                <div class="bk-info__row">
                  <span class="bk-info__label">Tên file</span>
                  <span class="bk-info__value bk-info__value--mono">{{ targetBackupItem.filename }}</span>
                </div>
                <div class="bk-info__row">
                  <span class="bk-info__label">Ngày tạo</span>
                  <span class="bk-info__value">{{ formatDateTime(targetBackupItem.createdAt) }}</span>
                </div>
              </template>
            </div>
            <div class="bk-alert bk-alert--error" style="margin:0">
              <i class="fas fa-radiation-alt"></i>
              <span>
                Toàn bộ dữ liệu chi nhánh sẽ bị <strong>XOÁ SẠCH VÀ GHI ĐÈ</strong>.
                Chi nhánh bị <strong>KHOÁ GIAO DỊCH</strong> trong quá trình khôi phục.
                <em>Hành động này không thể hoàn tác!</em>
              </span>
            </div>
          </div>
          <div class="bk-modal__foot">
            <button @click="showRestoreConfirm = false" class="bk-btn bk-btn--cancel">Huỷ bỏ</button>
            <button @click="executeRestore" class="bk-btn bk-btn--confirm-danger">
              <i class="fas fa-check-circle"></i> Xác nhận &amp; Khôi phục
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- DELETE CONFIRM MODAL -->
    <Transition name="bk-modal">
      <div v-if="showDeleteConfirm" class="bk-backdrop" @click.self="showDeleteConfirm = false">
        <div class="bk-modal bk-modal--sm">
          <div class="bk-modal__body bk-modal__body--center">
            <div class="bk-del-icon"><i class="fas fa-trash-alt"></i></div>
            <h3 class="bk-modal__title" style="text-align:center">Xác nhận xoá bản sao lưu</h3>
            <p class="bk-del-desc">Tệp tin vật lý trên ổ đĩa máy chủ sẽ bị xoá vĩnh viễn.</p>
          </div>
          <div class="bk-modal__foot">
            <button @click="showDeleteConfirm = false" class="bk-btn bk-btn--cancel">Huỷ</button>
            <button @click="executeDelete" class="bk-btn bk-btn--confirm-danger">
              <i class="fas fa-trash-alt"></i> Xác nhận xoá
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- WIPE CONFIRM MODAL -->
    <Transition name="bk-modal">
      <div v-if="showWipeConfirm" class="bk-backdrop" @click.self="showWipeConfirm = false">
        <div class="bk-modal">
          <div class="bk-modal__head bk-modal__head--danger">
            <div class="bk-modal__head-icon"><i class="fas fa-skull-crossbones"></i></div>
            <div>
              <h3 class="bk-modal__title">Xóa toàn bộ dữ liệu chi nhánh</h3>
              <p class="bk-modal__sub bk-modal__sub--danger">Thao tác chỉ dành cho Demo/Test</p>
            </div>
          </div>
          <div class="bk-modal__body">
            <div class="bk-info">
              <div class="bk-info__row">
                <span class="bk-info__label">Hành động</span>
                <span class="bk-info__value">Xóa sạch dữ liệu giao dịch chi nhánh</span>
              </div>
              <div class="bk-info__row">
                <span class="bk-info__label">Dữ liệu bị xóa</span>
                <span class="bk-info__value">Tồn kho, Phiếu kho, Kiểm kê, Khách hàng, Nhân viên</span>
              </div>
              <div class="bk-info__row">
                <span class="bk-info__label">Giữ lại</span>
                <span class="bk-info__value">Tài khoản đang đăng nhập, Thông tin chi nhánh, Bản sao lưu</span>
              </div>
            </div>
            <div class="bk-alert bk-alert--error" style="margin:0">
              <i class="fas fa-radiation-alt"></i>
              <span>
                Toàn bộ dữ liệu sẽ bị <strong>XÓA SẠCH</strong>.
                Bạn có thể <strong>khôi phục lại</strong> từ bản sao lưu đã tạo trước đó.
              </span>
            </div>
          </div>
          <div class="bk-modal__foot">
            <button @click="showWipeConfirm = false" class="bk-btn bk-btn--cancel">Huỷ bỏ</button>
            <button @click="executeBranchWipe" class="bk-btn bk-btn--confirm-danger">
              <i class="fas fa-skull-crossbones"></i> Xác nhận xóa sạch
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<style scoped>
/* ═══════════════════════════════════════════════════════════════
   BACKUP & RESTORE — REDESIGNED
   Uses CSS custom properties from style.css (accent-*, n-*)
   ═══════════════════════════════════════════════════════════════ */
.bk { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; position: relative; }

/* ── OVERLAY ── */
.bk-overlay {
  position: fixed; inset: 0; background: rgba(2, 6, 23, 0.92);
  backdrop-filter: blur(12px); z-index: 9999;
  display: flex; align-items: center; justify-content: center;
}
.bk-overlay__inner {
  display: flex; flex-direction: column; align-items: center;
  gap: 1rem; text-align: center; color: white; padding: 2rem; max-width: 460px;
}
.bk-overlay__title { font-size: 1.35rem; font-weight: 800; letter-spacing: -0.02em; margin: 0; }
.bk-overlay__desc { font-size: 0.82rem; color: rgba(255,255,255,0.55); line-height: 1.7; margin: 0; }
.bk-overlay__desc strong { color: #f59e0b; }

.bk-spinner { position: relative; width: 72px; height: 72px; display: flex; align-items: center; justify-content: center; }
.bk-spinner__ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 2px solid transparent; animation: bk-spin 1.2s linear infinite;
}
.bk-spinner__ring--1 { border-top-color: var(--accent-500); animation-duration: 1s; }
.bk-spinner__ring--2 { inset: 8px; border-right-color: var(--accent-300); animation-duration: 1.5s; animation-direction: reverse; }
.bk-spinner__icon { font-size: 1.1rem; color: var(--accent-400); position: relative; z-index: 1; animation: bk-pulse 1.5s ease-in-out infinite; }

.bk-progress { width: 280px; height: 3px; background: rgba(255,255,255,0.08); border-radius: 99px; overflow: hidden; }
.bk-progress__bar {
  height: 100%; width: 35%; border-radius: 99px;
  background: linear-gradient(90deg, var(--accent-500), var(--accent-300));
  animation: bk-slide 1.8s ease-in-out infinite;
}

/* ── HEADER ── */
.bk-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.bk-header__left { display: flex; align-items: center; gap: 0.75rem; }
.bk-header__icon {
  width: 42px; height: 42px; border-radius: 10px;
  background: var(--accent-600); color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem; flex-shrink: 0;
}
.bk-header__title { font-size: 1.35rem; font-weight: 800; color: var(--n-900); margin: 0; letter-spacing: -0.02em; font-family: var(--font-sans); }
.bk-header__sub { font-size: 0.78rem; color: var(--n-400); margin: 0.15rem 0 0; font-weight: 500; }
.bk-header__actions { display: flex; gap: 0.5rem; }

/* ── SECURITY STRIP ── */
.bk-strip { display: flex; flex-wrap: wrap; gap: 0.35rem; }
.bk-strip__tag {
  display: inline-flex; align-items: center; gap: 0.3rem;
  padding: 0.2rem 0.55rem; border-radius: 5px;
  background: var(--n-100); border: 1px solid var(--n-200);
  font-size: 0.68rem; font-weight: 700; color: var(--n-500);
  letter-spacing: 0.03em; text-transform: uppercase;
}
.bk-strip__tag i { font-size: 0.6rem; }

/* ── ALERT ── */
.bk-alert {
  display: flex; align-items: flex-start; gap: 0.6rem;
  padding: 0.7rem 0.875rem; border-radius: 8px;
  font-size: 0.8rem; line-height: 1.6; font-weight: 500;
}
.bk-alert i { margin-top: 2px; flex-shrink: 0; font-size: 0.85rem; }
.bk-alert--warn { background: var(--warning-bg); border: 1px solid var(--warning-border); color: var(--warning-text); }
.bk-alert--info { background: var(--accent-50); border: 1px solid var(--accent-200); color: var(--accent-800); }
.bk-alert--error { background: var(--danger-bg); border: 1px solid var(--danger-border); color: var(--danger-text); }

/* ── GRID ── */
.bk-grid { display: grid; grid-template-columns: 1fr; gap: 1rem; }
@media (min-width: 1024px) { .bk-grid { grid-template-columns: 5fr 7fr; } }
.bk-ops { display: flex; flex-direction: column; gap: 1rem; }

/* ── CARD ── */
.bk-card {
  background: var(--bg-card); border: 1px solid var(--n-200);
  border-radius: 10px; padding: 1.25rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.bk-card:hover { border-color: var(--n-300); box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.bk-card--danger { border-color: rgba(239, 68, 68, 0.2); }
.bk-card--danger:hover { border-color: rgba(239, 68, 68, 0.35); }

.bk-card__head { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
.bk-card__icon {
  width: 38px; height: 38px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1rem; flex-shrink: 0; color: white;
}
.bk-card__icon--accent { background: var(--accent-600); }
.bk-card__icon--emerald { background: #059669; }
.bk-card__icon--amber { background: #d97706; }
.bk-card__icon--slate { background: var(--n-600); }
.bk-card__icon--danger { background: #dc2626; }

.bk-card__title {
  font-size: 0.7rem; font-weight: 700; color: var(--n-400);
  margin: 0; letter-spacing: 0.08em; text-transform: uppercase;
}
.bk-card__sub { font-size: 0.78rem; color: var(--n-500); font-weight: 500; margin: 0.1rem 0 0; }
.bk-card__desc { font-size: 0.82rem; color: var(--n-600); font-weight: 500; line-height: 1.6; margin-bottom: 0.875rem; }
.bk-card__desc code {
  background: var(--accent-50); color: var(--accent-700);
  padding: 1px 5px; border-radius: 4px; font-size: 0.78rem; font-weight: 700;
  font-family: var(--font-mono);
}

/* ── ACTION BUTTONS ── */
.bk-action {
  width: 100%; display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  padding: 0.625rem 1rem; border-radius: 8px;
  font-size: 0.84rem; font-weight: 700; cursor: pointer; border: none; color: white;
  transition: all 0.2s ease;
}
.bk-action:hover:not(:disabled) { transform: translateY(-1px); }
.bk-action:active:not(:disabled) { transform: scale(0.98); }
.bk-action:disabled { opacity: 0.45; cursor: not-allowed; }
.bk-action--accent { background: var(--accent-600); box-shadow: 0 2px 8px rgba(13, 148, 136,0.25); }
.bk-action--accent:hover:not(:disabled) { background: var(--accent-700); box-shadow: 0 4px 14px rgba(13, 148, 136,0.35); }
.bk-action--emerald { background: #059669; box-shadow: 0 2px 8px rgba(5,150,105,0.25); }
.bk-action--emerald:hover:not(:disabled) { background: #047857; box-shadow: 0 4px 14px rgba(5,150,105,0.35); }
.bk-action--amber { background: #d97706; box-shadow: 0 2px 8px rgba(217,119,6,0.25); }
.bk-action--amber:hover:not(:disabled) { background: #b45309; box-shadow: 0 4px 14px rgba(217,119,6,0.35); }
.bk-action--danger { background: #dc2626; box-shadow: 0 2px 8px rgba(220,38,38,0.25); }
.bk-action--danger:hover:not(:disabled) { background: #b91c1c; box-shadow: 0 4px 14px rgba(220,38,38,0.35); }

/* ── BUTTONS ── */
.bk-btn {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.45rem 0.875rem; border-radius: 8px;
  font-size: 0.8rem; font-weight: 600; cursor: pointer; border: none;
  transition: all 0.15s ease;
}
.bk-btn:active { transform: scale(0.97); }
.bk-btn--accent { background: var(--accent-600); color: white; }
.bk-btn--accent:hover { background: var(--accent-700); }
.bk-btn--accent:disabled { opacity: 0.5; cursor: not-allowed; }
.bk-btn--ghost {
  background: var(--bg-card); color: var(--accent-600);
  border: 1px solid var(--n-200); font-weight: 700;
}
.bk-btn--ghost:hover { background: var(--n-50); border-color: var(--n-300); }
.bk-btn--cancel { background: var(--bg-card); color: var(--n-500); border: 1px solid var(--n-200); }
.bk-btn--cancel:hover { background: var(--n-50); }
.bk-btn--confirm-danger { background: #dc2626; color: white; }
.bk-btn--confirm-danger:hover { background: #b91c1c; }
.bk-btn--confirm-accent { background: var(--accent-600); color: white; }
.bk-btn--confirm-accent:hover { background: var(--accent-700); }

/* ── DROPZONE ── */
.bk-drop {
  border: 2px dashed var(--n-300); border-radius: 8px;
  padding: 1.25rem 0.875rem; text-align: center; cursor: pointer;
  transition: all 0.2s ease; min-height: 130px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 0.875rem; background: var(--n-50);
}
.bk-drop:hover { border-color: var(--accent-400); background: var(--accent-50); }
.bk-drop--active { border-color: var(--accent-500); background: var(--accent-50); box-shadow: 0 0 0 3px rgba(20, 184, 166,0.12); }
.bk-drop--filled { border-style: solid; border-color: #059669; background: rgba(5,150,105,0.06); }
.bk-drop__empty { display: flex; flex-direction: column; align-items: center; gap: 0.35rem; }
.bk-drop__icon { font-size: 2rem; color: var(--n-400); transition: color 0.2s ease; }
.bk-drop:hover .bk-drop__icon { color: var(--accent-500); }
.bk-drop__label { font-size: 0.88rem; font-weight: 700; color: var(--n-700); margin: 0; }
.bk-drop__link { color: var(--accent-600); text-decoration: underline; text-decoration-style: dashed; }
.bk-drop__hint { font-size: 0.75rem; color: var(--n-400); font-weight: 500; margin: 0; }
.bk-drop__hint code { font-weight: 800; color: #059669; font-family: var(--font-mono); }

.bk-drop__file { display: flex; align-items: center; gap: 0.75rem; width: 100%; padding: 0.2rem; }
.bk-drop__file-icon {
  width: 42px; height: 42px; border-radius: 8px;
  background: #059669; color: white; font-size: 1.1rem;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.bk-drop__file-info { flex: 1; text-align: left; min-width: 0; }
.bk-drop__name {
  font-size: 0.85rem; font-weight: 700; color: var(--n-900);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.bk-drop__size { font-size: 0.75rem; color: var(--n-500); font-weight: 600; font-family: var(--font-mono); margin-top: 1px; }
.bk-drop__remove {
  width: 28px; height: 28px; border-radius: 6px;
  background: rgba(239,68,68,0.1); color: #dc2626;
  border: none; cursor: pointer; display: flex; align-items: center; justify-content: center;
  font-size: 0.8rem; flex-shrink: 0; transition: all 0.15s ease;
}
.bk-drop__remove:hover { background: #dc2626; color: white; }

/* ── HISTORY ── */
.bk-history { padding: 0; display: flex; flex-direction: column; }
.bk-history__head {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 1rem 1.25rem; border-bottom: 1px solid var(--n-200);
}
.bk-history__foot {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0.6rem 1.25rem; border-top: 1px solid var(--n-200);
  font-size: 0.7rem; font-weight: 600; color: var(--n-400); margin-top: auto;
}
.bk-history__foot i { margin-right: 0.2rem; }

.bk-skel { padding: 0.875rem 1.25rem; display: flex; flex-direction: column; gap: 0.6rem; }
.bk-skel__row {
  height: 52px; border-radius: 6px;
  background: linear-gradient(90deg, var(--n-100) 25%, var(--n-200) 50%, var(--n-100) 75%);
  background-size: 200% 100%; animation: bk-shimmer 1.4s ease-in-out infinite;
}

.bk-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 2.5rem 1rem; gap: 0.5rem;
}
.bk-empty__icon { font-size: 1.5rem; color: var(--n-300); margin-bottom: 0.25rem; }
.bk-empty__title { font-size: 0.88rem; font-weight: 700; color: var(--n-600); }
.bk-empty__sub { font-size: 0.75rem; color: var(--n-400); text-align: center; line-height: 1.5; }

.bk-list { flex: 1; display: flex; flex-direction: column; overflow-y: auto; max-height: 520px; }
.bk-row {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.75rem 1.25rem; border-bottom: 1px solid var(--n-100);
  transition: background 0.15s ease;
}
.bk-row:last-child { border-bottom: none; }
.bk-row:hover { background: var(--n-50); }
.bk-row:hover .bk-row__actions { opacity: 1; }

.bk-row__type {
  width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem; flex-shrink: 0;
}
.bk-row__type--auto { background: rgba(99,102,241,0.1); color: #6366f1; }
.bk-row__type--manual { background: rgba(5,150,105,0.1); color: #059669; }
.bk-row__type--system { background: var(--accent-50); color: var(--accent-700); }

.bk-row__info { flex: 1; min-width: 0; }
.bk-row__name {
  font-size: 0.8rem; font-weight: 700; color: var(--n-800);
  font-family: var(--font-mono);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.bk-row__meta {
  display: flex; flex-wrap: wrap; gap: 0.6rem;
  margin-top: 0.2rem; font-size: 0.7rem; color: var(--n-500); font-weight: 600;
}
.bk-row__meta span { display: flex; align-items: center; gap: 0.2rem; }
.bk-row__meta i { font-size: 0.6rem; color: var(--n-400); }

.bk-badge {
  padding: 0.2rem 0.5rem; border-radius: 4px;
  font-size: 0.65rem; font-weight: 700; text-transform: uppercase;
  letter-spacing: 0.05em; flex-shrink: 0;
}
.bk-badge--auto { background: rgba(99,102,241,0.1); color: #6366f1; }
.bk-badge--manual { background: rgba(5,150,105,0.1); color: #059669; }

.bk-row__actions { display: flex; gap: 0.3rem; flex-shrink: 0; opacity: 0.4; transition: opacity 0.15s ease; }

.bk-row-btn {
  display: inline-flex; align-items: center; gap: 0.25rem;
  padding: 0.3rem 0.6rem; border-radius: 6px;
  font-size: 0.75rem; font-weight: 700; cursor: pointer; border: none;
  transition: all 0.15s ease;
}
.bk-row-btn:active { transform: scale(0.95); }
.bk-row-btn--restore { background: var(--accent-50); color: var(--accent-700); }
.bk-row-btn--restore:hover { background: var(--accent-600); color: white; }
.bk-row-btn--delete { background: rgba(239,68,68,0.08); color: #dc2626; padding: 0.3rem 0.5rem; }
.bk-row-btn--delete:hover { background: #ef4444; color: white; }

/* ── MODALS ── */
.bk-backdrop {
  position: fixed; inset: 0; background: rgba(2,6,23,0.55);
  backdrop-filter: blur(6px); z-index: 2000;
  display: flex; align-items: center; justify-content: center; padding: 1rem;
}
.bk-modal {
  background: var(--bg-card); border-radius: 12px;
  width: 100%; max-width: 480px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.15);
  overflow: hidden; border: 1px solid var(--n-200);
}
.bk-modal--sm { max-width: 380px; }
.bk-modal__head {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 1rem 1.25rem; border-bottom: 1px solid var(--n-200);
}
.bk-modal__head--danger { background: var(--danger-bg); }
.bk-modal__head--accent { background: var(--accent-50); }
.bk-modal__head-icon {
  width: 38px; height: 38px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1rem; flex-shrink: 0;
  background: rgba(239,68,68,0.1); color: #dc2626;
}
.bk-modal__head--accent .bk-modal__head-icon { background: rgba(13, 148, 136,0.1); color: var(--accent-700); }
.bk-modal__title { font-size: 0.95rem; font-weight: 800; color: var(--n-900); margin: 0; }
.bk-modal__sub { font-size: 0.72rem; margin: 0.15rem 0 0; color: var(--n-500); font-weight: 500; }
.bk-modal__sub--danger { color: #dc2626; }
.bk-modal__close {
  margin-left: auto; width: 30px; height: 30px; border-radius: 6px;
  background: none; border: none; cursor: pointer; color: var(--n-400);
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s ease;
}
.bk-modal__close:hover { background: var(--n-100); }
.bk-modal__body { padding: 1rem 1.25rem; display: flex; flex-direction: column; gap: 0.875rem; }
.bk-modal__body--center { align-items: center; text-align: center; }
.bk-modal__foot {
  display: flex; gap: 0.6rem; justify-content: flex-end;
  padding: 0.875rem 1.25rem; background: var(--n-50); border-top: 1px solid var(--n-200);
}

.bk-del-icon {
  width: 60px; height: 60px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(239,68,68,0.08); color: #ef4444; font-size: 1.4rem;
  margin: 0.25rem auto;
}
.bk-del-desc { font-size: 0.8rem; color: var(--n-500); line-height: 1.5; max-width: 260px; margin: 0; }

.bk-info { background: var(--n-50); border: 1px solid var(--n-200); border-radius: 8px; overflow: hidden; }
.bk-info__row {
  display: grid; grid-template-columns: 110px 1fr;
  padding: 0.5rem 0.875rem; font-size: 0.78rem; gap: 0.4rem;
  border-bottom: 1px solid var(--n-100);
}
.bk-info__row:last-child { border-bottom: none; }
.bk-info__label { color: var(--n-500); font-weight: 600; }
.bk-info__value { color: var(--n-800); font-weight: 700; }
.bk-info__value--mono { font-family: var(--font-mono); font-size: 0.72rem; word-break: break-all; }

/* ── ANIMATIONS ── */
@keyframes bk-spin { to { transform: rotate(360deg); } }
@keyframes bk-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
@keyframes bk-slide { 0% { transform: translateX(-100%); } 100% { transform: translateX(400%); } }
@keyframes bk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
@keyframes bk-bounce-anim { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-3px); } }
.bk-bounce { animation: bk-bounce-anim 0.6s ease-in-out infinite; }

.bk-overlay-enter-active { transition: all 0.2s ease; }
.bk-overlay-leave-active { transition: all 0.15s ease; }
.bk-overlay-enter-from, .bk-overlay-leave-to { opacity: 0; }

.bk-modal-enter-active { transition: all 0.2s cubic-bezier(0.34,1.56,0.64,1); }
.bk-modal-leave-active { transition: all 0.15s ease; }
.bk-modal-enter-from { opacity: 0; transform: scale(0.95); }
.bk-modal-leave-to { opacity: 0; transform: scale(0.97); }

.fa-spin { animation: bk-spin 0.85s linear infinite; }

/* ── DARK MODE ── */
html.dark-mode .bk-card { background: var(--n-800); border-color: var(--n-700); }
html.dark-mode .bk-card:hover { border-color: var(--n-600); }
html.dark-mode .bk-header__title { color: var(--n-50); }
html.dark-mode .bk-card__title { color: var(--n-400); }
html.dark-mode .bk-card__sub, html.dark-mode .bk-card__desc { color: var(--n-400); }
html.dark-mode .bk-card__desc code { background: rgba(20, 184, 166,0.12); color: var(--accent-300); }
html.dark-mode .bk-drop { background: rgba(255,255,255,0.03); border-color: var(--n-600); }
html.dark-mode .bk-drop:hover { background: rgba(20, 184, 166,0.08); border-color: var(--accent-500); }
html.dark-mode .bk-drop__icon { color: var(--n-500); }
html.dark-mode .bk-drop__label { color: var(--n-200); }
html.dark-mode .bk-drop__name { color: var(--n-100); }
html.dark-mode .bk-row { border-bottom-color: var(--n-700); }
html.dark-mode .bk-row:hover { background: rgba(255,255,255,0.04); }
html.dark-mode .bk-row__name { color: var(--n-100); }
html.dark-mode .bk-row__meta { color: var(--n-400); }
html.dark-mode .bk-history__head { border-bottom-color: var(--n-700); }
html.dark-mode .bk-history__foot { border-top-color: var(--n-700); background: rgba(255,255,255,0.02); color: var(--n-500); }
html.dark-mode .bk-skel__row { background: rgba(255,255,255,0.05); }
html.dark-mode .bk-empty__icon { color: var(--n-600); }
html.dark-mode .bk-strip__tag { background: var(--n-800); border-color: var(--n-700); color: var(--n-400); }
html.dark-mode .bk-modal { background: var(--n-800); border-color: var(--n-700); }
html.dark-mode .bk-modal__head { border-bottom-color: var(--n-700); }
html.dark-mode .bk-modal__foot { background: var(--n-900); border-top-color: var(--n-700); }
html.dark-mode .bk-modal__title { color: var(--n-50); }
html.dark-mode .bk-info { background: var(--n-900); border-color: var(--n-700); }
html.dark-mode .bk-info__row { border-bottom-color: var(--n-800); }
html.dark-mode .bk-info__value { color: var(--n-200); }
html.dark-mode .bk-btn--ghost { background: var(--n-800); border-color: var(--n-600); color: var(--accent-400); }
html.dark-mode .bk-btn--cancel { background: var(--n-700); border-color: var(--n-600); color: var(--n-200); }

.hidden { display: none; }
</style>
