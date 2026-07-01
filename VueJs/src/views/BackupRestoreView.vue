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
  <div class="backup-page">

    <!-- ═══ RESTORE OVERLAY ═══════════════════════════════════════════════════ -->
    <Transition name="overlay">
      <div v-if="restoring" class="restore-overlay">
        <div class="restore-overlay__inner">
          <div class="restore-spinner">
            <div class="spinner-ring ring-1"></div>
            <div class="spinner-ring ring-2"></div>
            <div class="spinner-ring ring-3"></div>
            <i class="fas fa-database spinner-icon"></i>
          </div>
          <h2 class="restore-overlay__title">Đang khôi phục dữ liệu...</h2>
          <p class="restore-overlay__desc">
            Hệ thống đang giải mã và phục hồi dữ liệu. Chi nhánh tạm thời bị khóa giao dịch.<br>
            <strong>Vui lòng không đóng hoặc tải lại trang!</strong>
          </p>
          <div class="restore-progress-bar">
            <div class="restore-progress-bar__fill"></div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ═══ PAGE HEADER ═══════════════════════════════════════════════════════ -->
    <div class="page-header">
      <div class="page-header__left">
        <div class="page-header__icon-wrap">
          <i class="fas fa-database"></i>
          <div class="page-header__icon-glow"></div>
        </div>
        <div>
          <h1 class="page-header__title">Sao lưu &amp; Phục hồi</h1>
          <p class="page-header__sub">
            {{ isAdmin ? 'Quản lý cấu hình toàn hệ thống' : 'Quản lý dữ liệu chi nhánh' }}
            với <span class="badge-enc">GZIP</span> + <span class="badge-enc">AES-256-GCM</span>
          </p>
        </div>
      </div>
      <div class="page-header__actions">
        <button v-if="isAdmin" @click="triggerScheduledBackup" :disabled="triggeringAuto" class="btn btn--admin">
          <i class="fas fa-magic" :class="{ 'fa-spin': triggeringAuto }"></i>
          <span>Auto Backup</span>
        </button>
        <button @click="isAdmin ? loadSysHistory() : loadHistory()" class="btn btn--ghost">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': isAdmin ? sysLoading : loading }"></i>
          <span>Làm mới</span>
        </button>
      </div>
    </div>

    <!-- ═══ WARNING BANNER ════════════════════════════════════════════════════ -->
    <div class="warning-banner">
      <div class="warning-banner__icon"><i class="fas fa-shield-alt"></i></div>
      <div class="warning-banner__text">
        <strong>Lưu ý quan trọng:</strong> Hành động khôi phục sẽ <strong>xoá sạch và ghi đè</strong>
        toàn bộ dữ liệu giao dịch chi nhánh (tồn kho, phiếu kho, kiểm kê, khách hàng).
        Chi nhánh sẽ bị <em>khoá tạm thời</em> trong suốt quá trình phục hồi.
      </div>
    </div>

    <!-- ═══ SECURITY BADGES ═══════════════════════════════════════════════════ -->
    <div class="sec-badges">
      <div class="sec-badge"><i class="fas fa-lock"></i><span>AES-256-GCM</span></div>
      <div class="sec-badge"><i class="fas fa-fingerprint"></i><span>HMAC-SHA256</span></div>
      <div class="sec-badge"><i class="fas fa-file-archive"></i><span>Định dạng .wbk</span></div>
      <div class="sec-badge"><i class="fas fa-calendar-alt"></i><span>Lưu trữ 14 ngày</span></div>
      <div class="sec-badge sec-badge--gzip"><i class="fas fa-compress-arrows-alt"></i><span>GZIP Nén</span></div>
    </div>

    <!-- ═══ MAIN GRID (Manager only) ═══════════════════════════════════════════ -->
    <div class="main-grid">

      <!-- LEFT: Operations -->
      <div class="ops-col">

        <!-- ── Backup Card ── -->
        <div class="card card--backup">
          <div class="card__glow card__glow--blue"></div>
          <div class="card__header">
            <div class="card__icon card__icon--blue">
              <i class="fas fa-cloud-download-alt"></i>
            </div>
            <div>
              <h2 class="card__title">Sao lưu dữ liệu</h2>
              <p class="card__subtitle">Tải file .wbk mã hoá về máy</p>
            </div>
          </div>
          <p class="card__desc">
            Toàn bộ dữ liệu chi nhánh được ký bằng <strong>HMAC-SHA256</strong>
            và mã hoá bằng <strong>AES-256-GCM</strong> trước khi tải xuống.
            File <code>.wbk</code> không thể đọc trực tiếp bằng text editor.
          </p>
          <button @click="handleExport" :disabled="exporting" class="btn-action btn-action--blue">
            <span class="btn-action__icon">
              <i class="fas fa-file-download" :class="{ 'animate-bounce-icon': exporting }"></i>
            </span>
            <span>{{ exporting ? 'Đang tạo bản sao lưu...' : 'Tải xuống tệp tin sao lưu' }}</span>
            <span v-if="!exporting" class="btn-action__arrow">→</span>
          </button>
        </div>

        <!-- ── Restore from File Card ── -->
        <div class="card card--restore">
          <div class="card__glow card__glow--green"></div>
          <div class="card__header">
            <div class="card__icon card__icon--green">
              <i class="fas fa-cloud-upload-alt"></i>
            </div>
            <div>
              <h2 class="card__title">Phục hồi từ tệp tin</h2>
              <p class="card__subtitle">Upload file .wbk để restore</p>
            </div>
          </div>
          <p class="card__desc">
            Kéo thả hoặc chọn tệp <code>.wbk</code> từ máy tính.
            Hệ thống tự động giải mã AES-256-GCM và xác thực chữ ký HMAC
            trước khi ghi đè dữ liệu.
          </p>

          <!-- Dropzone -->
          <div
            class="dropzone"
            :class="{ 'dropzone--active': isDragging, 'dropzone--filled': selectedFile }"
            @dragover="onDragOver"
            @dragleave="onDragLeave"
            @drop="onDrop"
            @click="triggerFileSelect"
          >
            <input type="file" ref="fileInput" @change="onFileSelected" accept=".wbk,.json" class="hidden" />

            <div v-if="!selectedFile" class="dropzone__empty">
              <div class="dropzone__upload-anim">
                <i class="fas fa-cloud-upload-alt dropzone__icon"></i>
                <div class="dropzone__ripple"></div>
              </div>
              <p class="dropzone__label">Kéo thả hoặc <span class="dropzone__click">click để chọn</span></p>
              <p class="dropzone__hint">Hỗ trợ <code>.wbk</code> (mã hoá) &amp; <code>.json</code> (bản cũ)</p>
            </div>

            <div v-else class="dropzone__filled-content">
              <div class="dropzone__file-icon">
                <i class="fas fa-file-shield"></i>
              </div>
              <div class="dropzone__file-info">
                <div class="dropzone__filename" :title="selectedFile.name">{{ selectedFile.name }}</div>
                <div class="dropzone__filesize">{{ formatBytes(selectedFile.size) }}</div>
              </div>
              <button @click.stop="removeSelectedFile" class="dropzone__remove">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>

          <button @click="confirmRestoreFromFile" :disabled="!selectedFile || restoring"
            class="btn-action btn-action--green">
            <span class="btn-action__icon"><i class="fas fa-undo-alt"></i></span>
            <span>Phục hồi từ tệp tin</span>
            <span v-if="selectedFile" class="btn-action__arrow">→</span>
          </button>
        </div>

        <!-- ── Wipe Branch Data Card (Demo) ── -->
        <div class="card card--wipe">
          <div class="card__glow card__glow--red"></div>
          <div class="card__header">
            <div class="card__icon card__icon--red">
              <i class="fas fa-skull-crossbones"></i>
            </div>
            <div>
              <h2 class="card__title">Xóa dữ liệu (Demo)</h2>
              <p class="card__subtitle">Test khôi phục từ bản sao lưu</p>
            </div>
          </div>
          <p class="card__desc">
            Xóa <strong>toàn bộ dữ liệu giao dịch</strong> của chi nhánh gồm:
            tồn kho, phiếu kho, kiểm kê, khách hàng và nhân viên khác.
            <strong>Chỉ giữ lại tài khoản bạn đang đăng nhập.</strong>
            Dùng để test quá trình khôi phục từ bản sao lưu.
          </p>
          <button @click="showWipeConfirm = true" :disabled="wiping"
            class="btn-action btn-action--red">
            <span class="btn-action__icon">
              <i class="fas fa-trash-alt" :class="{ 'fa-spin': wiping }"></i>
            </span>
            <span>{{ wiping ? 'Đang xóa dữ liệu...' : 'Xóa toàn bộ dữ liệu chi nhánh' }}</span>
            <span v-if="!wiping" class="btn-action__arrow">→</span>
          </button>
        </div>

      </div>

      <!-- RIGHT: Server History -->
      <div class="history-col card">
        <div class="card__glow card__glow--indigo"></div>
        <div class="history-header">
          <div class="card__icon card__icon--indigo">
            <i class="fas fa-history"></i>
          </div>
          <div>
            <h2 class="card__title">Bản sao lưu trên máy chủ</h2>
            <p class="card__subtitle">Auto (14 ngày) &amp; Manual của chi nhánh</p>
          </div>
        </div>

        <!-- Loading skeleton -->
        <div v-if="loading" class="history-skeleton">
          <div v-for="i in 5" :key="i" class="skeleton-row"></div>
        </div>

        <!-- Empty state -->
        <div v-else-if="history.length === 0" class="empty-state">
          <div class="empty-state__icon">
            <i class="fas fa-database"></i>
          </div>
          <div class="empty-state__title">Chưa có bản sao lưu nào</div>
          <div class="empty-state__sub">
            Bản sao lưu tự động (01:00 AM hằng ngày) và thủ công<br>sẽ xuất hiện ở đây
          </div>
        </div>

        <!-- History list -->
        <div v-else class="history-list">
          <div
            v-for="(item, idx) in history"
            :key="item.id"
            class="history-item"
            :style="{ animationDelay: `${idx * 0.05}s` }"
          >
            <!-- Type badge + icon -->
            <div class="history-item__left">
              <div class="history-item__type-icon"
                :class="item.backupType === 'AUTO' ? 'type-auto' : 'type-manual'">
                <i :class="item.backupType === 'AUTO' ? 'fas fa-robot' : 'fas fa-user'"></i>
              </div>
            </div>

            <!-- Info -->
            <div class="history-item__info">
              <div class="history-item__filename" :title="item.filename">{{ item.filename }}</div>
              <div class="history-item__meta">
                <span><i class="fas fa-clock"></i> {{ formatDateTime(item.createdAt) }}</span>
                <span><i class="fas fa-weight"></i> {{ formatBytes(item.fileSize) }}</span>
                <span>
                  <i class="fas fa-user-circle"></i>
                  {{ item.createdBy ? item.createdBy.fullName : 'Hệ thống' }}
                </span>
              </div>
            </div>

            <!-- Badge -->
            <span class="history-item__badge" :class="item.backupType === 'AUTO' ? 'badge-auto' : 'badge-manual'">
              {{ item.backupType === 'AUTO' ? 'Tự động' : 'Thủ công' }}
            </span>

            <!-- Actions -->
            <div class="history-item__actions">
              <button @click="confirmRestoreFromHistory(item)" class="action-btn action-btn--restore"
                title="Khôi phục từ bản này">
                <i class="fas fa-undo-alt"></i>
                <span>Khôi phục</span>
              </button>
              <button @click="confirmDelete(item.id)" class="action-btn action-btn--delete"
                title="Xóa bản sao lưu">
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div v-if="!loading" class="history-footer">
          <span><i class="fas fa-layer-group"></i> {{ history.length }} bản sao lưu</span>
          <span><i class="fas fa-calendar-times"></i> Tự động dọn dẹp sau 14 ngày</span>
        </div>
      </div>

    </div>

    <!-- ═══ ADMIN: SYSTEM CONFIG BACKUP ═══════════════════════════════════════ -->
    <template v-if="isAdmin">

      <!-- System Restore Overlay -->
      <Transition name="overlay">
        <div v-if="sysRestoring" class="restore-overlay">
          <div class="restore-overlay__inner">
            <div class="restore-spinner">
              <div class="spinner-ring ring-1"></div>
              <div class="spinner-ring ring-2"></div>
              <div class="spinner-ring ring-3"></div>
              <i class="fas fa-server spinner-icon"></i>
            </div>
            <h2 class="restore-overlay__title">Đang phục hồi cấu hình hệ thống...</h2>
            <p class="restore-overlay__desc">
              Hệ thống đang cập nhật lại danh mục, sản phẩm, chi nhánh và tài khoản Admin.<br>
              <strong>Vui lòng không đóng hoặc tải lại trang!</strong>
            </p>
            <div class="restore-progress-bar"><div class="restore-progress-bar__fill"></div></div>
          </div>
        </div>
      </Transition>

      <!-- Security Badges (Admin) -->
      <div class="sec-badges">
        <div class="sec-badge"><i class="fas fa-lock"></i><span>AES-256-GCM</span></div>
        <div class="sec-badge"><i class="fas fa-fingerprint"></i><span>HMAC-SHA256</span></div>
        <div class="sec-badge"><i class="fas fa-file-archive"></i><span>Định dạng .wbk</span></div>
        <div class="sec-badge sec-badge--admin"><i class="fas fa-server"></i><span>ADMIN ONLY</span></div>
      </div>

      <!-- Warning Banner (Admin) -->
      <div class="warning-banner warning-banner--teal">
        <div class="warning-banner__icon"><i class="fas fa-info-circle"></i></div>
        <div class="warning-banner__text">
          Phục hồi System Config sẽ <strong>UPSERT</strong> lại danh mục, sản phẩm, chi nhánh và nhân viên.
          Dữ liệu giao dịch kho của mọi chi nhánh (bao gồm cả chi nhánh tổng) <em>không bị ảnh hưởng</em>.
        </div>
      </div>

      <!-- Main Grid (Admin — same layout as Manager) -->
      <div class="main-grid">

        <!-- LEFT -->
        <div class="ops-col">

          <!-- Export Card -->
          <div class="card card--backup card--system-export">
            <div class="card__glow card__glow--teal"></div>
            <div class="card__header">
              <div class="card__icon card__icon--teal">
                <i class="fas fa-cloud-download-alt"></i>
              </div>
              <div>
                <h2 class="card__title">Sao lưu cấu hình hệ thống</h2>
                <p class="card__subtitle">Tải file .wbk mã hoá về máy</p>
              </div>
            </div>
            <p class="card__desc">
              Gom toàn bộ <strong>chi nhánh</strong>, <strong>danh mục</strong>,
              <strong>sản phẩm</strong> và <strong>tài khoản người dùng (Admin & Nhân viên)</strong>,
              ký <strong>HMAC-SHA256</strong> và mã hoá <strong>AES-256-GCM</strong>.
              File <code>.wbk</code> không thể đọc bằng text editor.
            </p>
            <button @click="handleSysExport" :disabled="sysExporting" class="btn-action btn-action--teal">
              <span class="btn-action__icon">
                <i class="fas fa-file-download" :class="{ 'animate-bounce-icon': sysExporting }"></i>
              </span>
              <span>{{ sysExporting ? 'Đang tạo bản sao lưu...' : 'Tải xuống System Config' }}</span>
              <span v-if="!sysExporting" class="btn-action__arrow">→</span>
            </button>
          </div>

          <!-- Import Card -->
          <div class="card card--restore card--system-import">
            <div class="card__glow card__glow--amber"></div>
            <div class="card__header">
              <div class="card__icon card__icon--amber">
                <i class="fas fa-cloud-upload-alt"></i>
              </div>
              <div>
                <h2 class="card__title">Phục hồi từ tệp tin</h2>
                <p class="card__subtitle">Upload file .wbk để restore hệ thống</p>
              </div>
            </div>
            <p class="card__desc">
              Kéo thả hoặc chọn tệp <code>.wbk</code> từ máy tính.
              Hệ thống xác thực HMAC trước khi UPSERT lại cấu hình. Dữ liệu kho chi nhánh an toàn.
            </p>

            <!-- Dropzone -->
            <div
              class="dropzone"
              :class="{ 'dropzone--active': sysDragging, 'dropzone--filled': sysSelectedFile }"
              @dragover="onSysDragOver" @dragleave="onSysDragLeave" @drop="onSysDrop"
              @click="triggerSysFileSelect"
            >
              <input type="file" ref="sysFileInput" @change="onSysFileSelected" accept=".wbk" class="hidden" />

              <div v-if="!sysSelectedFile" class="dropzone__empty">
                <div class="dropzone__upload-anim">
                  <i class="fas fa-cloud-upload-alt dropzone__icon"></i>
                  <div class="dropzone__ripple"></div>
                </div>
                <p class="dropzone__label">Kéo thả hoặc <span class="dropzone__click">click để chọn</span></p>
                <p class="dropzone__hint">Chỉ hỗ trợ <code>.wbk</code> System Config</p>
              </div>

              <div v-else class="dropzone__filled-content">
                <div class="dropzone__file-icon"><i class="fas fa-file-shield"></i></div>
                <div class="dropzone__file-info">
                  <div class="dropzone__filename" :title="sysSelectedFile.name">{{ sysSelectedFile.name }}</div>
                  <div class="dropzone__filesize">{{ formatBytes(sysSelectedFile.size) }}</div>
                </div>
                <button @click.stop="removeSysSelectedFile" class="dropzone__remove">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>

            <button @click="confirmSysRestoreFromFile" :disabled="!sysSelectedFile || sysRestoring"
              class="btn-action btn-action--amber">
              <span class="btn-action__icon"><i class="fas fa-undo-alt"></i></span>
              <span>Phục hồi từ tệp tin</span>
              <span v-if="sysSelectedFile" class="btn-action__arrow">→</span>
            </button>
          </div>

        </div>

        <!-- RIGHT: History -->
        <div class="history-col card card--system-history">
          <div class="card__glow card__glow--teal"></div>
          <div class="history-header">
            <div class="card__icon card__icon--teal">
              <i class="fas fa-history"></i>
            </div>
            <div>
              <h2 class="card__title">Bản sao lưu hệ thống</h2>
              <p class="card__subtitle">System Config đã lưu trên máy chủ</p>
            </div>
          </div>

          <!-- Loading skeleton -->
          <div v-if="sysLoading" class="history-skeleton">
            <div v-for="i in 5" :key="i" class="skeleton-row"></div>
          </div>

          <!-- Empty state -->
          <div v-else-if="sysHistory.length === 0" class="empty-state">
            <div class="empty-state__icon"><i class="fas fa-server"></i></div>
            <div class="empty-state__title">Chưa có bản sao lưu nào</div>
            <div class="empty-state__sub">Bấm "Tải xuống System Config" để tạo bản đầu tiên</div>
          </div>

          <!-- History list -->
          <div v-else class="history-list">
            <div
              v-for="(item, idx) in sysHistory" :key="item.id"
              class="history-item" :style="{ animationDelay: `${idx * 0.05}s` }"
            >
              <div class="history-item__left">
                <div class="history-item__type-icon type-system">
                  <i class="fas fa-server"></i>
                </div>
              </div>
              <div class="history-item__info">
                <div class="history-item__filename" :title="item.filename">{{ item.filename }}</div>
                <div class="history-item__meta">
                  <span><i class="fas fa-clock"></i> {{ formatDateTime(item.createdAt) }}</span>
                  <span><i class="fas fa-weight"></i> {{ formatBytes(item.fileSize) }}</span>
                  <span><i class="fas fa-user-shield"></i> {{ item.createdBy ? item.createdBy.fullName : 'Hệ thống' }}</span>
                </div>
              </div>
              <span class="history-item__badge badge-manual">Thủ công</span>
              <div class="history-item__actions">
                <button @click="confirmSysRestoreFromHistory(item)" class="action-btn action-btn--restore" title="Khôi phục">
                  <i class="fas fa-undo-alt"></i><span>Khôi phục</span>
                </button>
                <button @click="confirmSysDelete(item.id)" class="action-btn action-btn--delete" title="Xóa">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>
          </div>

          <div v-if="!sysLoading" class="history-footer">
            <span><i class="fas fa-layer-group"></i> {{ sysHistory.length }} bản sao lưu</span>
            <span><i class="fas fa-shield-alt"></i> AES-256-GCM</span>
          </div>
        </div>

      </div>



      <!-- ═══ SYSTEM RESTORE CONFIRM MODAL ═══ -->
      <Transition name="modal">
        <div v-if="showSysRestoreConfirm" class="modal-backdrop" @click.self="showSysRestoreConfirm = false">
          <div class="modal">
            <div class="modal__header modal__header--teal">
              <div class="modal__header-icon"><i class="fas fa-server"></i></div>
              <div>
                <h3 class="modal__title">Xác nhận Phục hồi System Config</h3>
                <p class="modal__header-sub">Kiểm tra kỹ trước khi xác nhận</p>
              </div>
              <button @click="showSysRestoreConfirm = false" class="modal__close"><i class="fas fa-times"></i></button>
            </div>
            <div class="modal__body">
              <div class="modal__info-grid">
                <div class="modal__info-row">
                  <span class="modal__info-label">Nguồn</span>
                  <span class="modal__info-value">
                    {{ sysRestoreSource === 'file' ? '📂 File cục bộ tải lên' : '🖥️ Bản lưu trên Server' }}
                  </span>
                </div>
                <template v-if="sysRestoreSource === 'file' && sysSelectedFile">
                  <div class="modal__info-row">
                    <span class="modal__info-label">Tên file</span>
                    <span class="modal__info-value modal__info-value--mono">{{ sysSelectedFile.name }}</span>
                  </div>
                </template>
                <template v-if="sysRestoreSource === 'history' && sysTargetItem">
                  <div class="modal__info-row">
                    <span class="modal__info-label">Tên file</span>
                    <span class="modal__info-value modal__info-value--mono">{{ sysTargetItem.filename }}</span>
                  </div>
                  <div class="modal__info-row">
                    <span class="modal__info-label">Ngày tạo</span>
                    <span class="modal__info-value">{{ formatDateTime(sysTargetItem.createdAt) }}</span>
                  </div>
                </template>
                <div class="modal__info-row">
                  <span class="modal__info-label">Phạm vi</span>
                  <span class="modal__info-value">Branches · Categories · Products · Users (Admin & Staff)</span>
                </div>
              </div>
              <div class="modal__alert modal__alert--warning">
                <i class="fas fa-exclamation-triangle"></i>
                <span>Hệ thống sẽ UPSERT lại dữ liệu cấu hình. Dữ liệu giao dịch kho <strong>không bị xóa</strong>.</span>
              </div>
            </div>
            <div class="modal__footer">
              <button @click="showSysRestoreConfirm = false" class="btn btn--cancel">Huỷ bỏ</button>
              <button @click="executeSysRestore" class="btn btn--confirm-teal">
                <i class="fas fa-check-circle"></i> Xác nhận & Phục hồi
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- ═══ SYSTEM DELETE CONFIRM MODAL ═══ -->
      <Transition name="modal">
        <div v-if="showSysDeleteConfirm" class="modal-backdrop" @click.self="showSysDeleteConfirm = false">
          <div class="modal modal--sm">
            <div class="modal__body modal__body--center">
              <div class="modal__delete-icon">
                <i class="fas fa-server"></i>
                <div class="modal__delete-ring"></div>
              </div>
              <h3 class="modal__title modal__title--center">Xóa bản sao lưu hệ thống?</h3>
              <p class="modal__delete-desc">Tệp tin trên ổ đĩa máy chủ sẽ bị xóa vĩnh viễn.</p>
            </div>
            <div class="modal__footer">
              <button @click="showSysDeleteConfirm = false" class="btn btn--cancel">Huỷ</button>
              <button @click="executeSysDelete" class="btn btn--confirm-danger">
                <i class="fas fa-trash-alt"></i> Xác nhận xoá
              </button>
            </div>
          </div>
        </div>
      </Transition>

    </template>


    <Transition name="modal">
      <div v-if="showRestoreConfirm" class="modal-backdrop" @click.self="showRestoreConfirm = false">
        <div class="modal modal--danger">
          <div class="modal__header modal__header--danger">
            <div class="modal__header-icon modal__header-icon--danger">
              <i class="fas fa-exclamation-triangle"></i>
            </div>
            <div>
              <h3 class="modal__title">Cảnh báo khôi phục dữ liệu</h3>
              <p class="modal__subtitle modal__subtitle--danger">Thao tác có nguy cơ mất dữ liệu hiện tại</p>
            </div>
          </div>

          <div class="modal__body">
            <div class="modal__info-grid">
              <div class="modal__info-row">
                <span class="modal__info-label">Nguồn khôi phục</span>
                <span class="modal__info-value">
                  {{ restoreSource === 'file' ? '📂 Tệp tin cục bộ tải lên' : '🖥️ Bản lưu trên máy chủ' }}
                </span>
              </div>
              <template v-if="restoreSource === 'file' && selectedFile">
                <div class="modal__info-row">
                  <span class="modal__info-label">Tên file</span>
                  <span class="modal__info-value modal__info-value--mono">{{ selectedFile.name }}</span>
                </div>
                <div class="modal__info-row">
                  <span class="modal__info-label">Kích thước</span>
                  <span class="modal__info-value">{{ formatBytes(selectedFile.size) }}</span>
                </div>
              </template>
              <template v-if="restoreSource === 'history' && targetBackupItem">
                <div class="modal__info-row">
                  <span class="modal__info-label">Tên file</span>
                  <span class="modal__info-value modal__info-value--mono">{{ targetBackupItem.filename }}</span>
                </div>
                <div class="modal__info-row">
                  <span class="modal__info-label">Ngày tạo</span>
                  <span class="modal__info-value">{{ formatDateTime(targetBackupItem.createdAt) }}</span>
                </div>
              </template>
            </div>

            <div class="modal__alert modal__alert--danger">
              <i class="fas fa-radiation-alt"></i>
              <span>
                Toàn bộ dữ liệu chi nhánh sẽ bị <strong>XOÁ SẠCH VÀ GHI ĐÈ</strong>.
                Chi nhánh bị <strong>KHOÁ GIAO DỊCH</strong> trong quá trình khôi phục.
                <em>Hành động này không thể hoàn tác!</em>
              </span>
            </div>
          </div>

          <div class="modal__footer">
            <button @click="showRestoreConfirm = false" class="btn btn--cancel">Huỷ bỏ</button>
            <button @click="executeRestore" class="btn btn--confirm-danger">
              <i class="fas fa-check-circle"></i> Xác nhận &amp; Khôi phục
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ═══ CONFIRM DELETE MODAL ════════════════════════════════════════════════ -->
    <Transition name="modal">
      <div v-if="showDeleteConfirm" class="modal-backdrop" @click.self="showDeleteConfirm = false">
        <div class="modal modal--sm">
          <div class="modal__body modal__body--center">
            <div class="modal__delete-icon">
              <i class="fas fa-trash-alt"></i>
              <div class="modal__delete-ring"></div>
            </div>
            <h3 class="modal__title modal__title--center">Xác nhận xoá bản sao lưu</h3>
            <p class="modal__delete-desc">
              Tệp tin vật lý trên ổ đĩa máy chủ sẽ bị xoá vĩnh viễn và không thể khôi phục lại.
            </p>
          </div>
          <div class="modal__footer">
            <button @click="showDeleteConfirm = false" class="btn btn--cancel">Huỷ</button>
            <button @click="executeDelete" class="btn btn--confirm-danger">
              <i class="fas fa-trash-alt"></i> Xác nhận xoá
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ═══ CONFIRM WIPE MODAL ═════════════════════════════════════════════════ -->
    <Transition name="modal">
      <div v-if="showWipeConfirm" class="modal-backdrop" @click.self="showWipeConfirm = false">
        <div class="modal modal--danger">
          <div class="modal__header modal__header--danger">
            <div class="modal__header-icon modal__header-icon--danger">
              <i class="fas fa-skull-crossbones"></i>
            </div>
            <div>
              <h3 class="modal__title">Xóa toàn bộ dữ liệu chi nhánh</h3>
              <p class="modal__subtitle modal__subtitle--danger">Thao tác chỉ dành cho Demo/Test</p>
            </div>
          </div>

          <div class="modal__body">
            <div class="modal__info-grid">
              <div class="modal__info-row">
                <span class="modal__info-label">Hành động</span>
                <span class="modal__info-value">Xóa sạch dữ liệu giao dịch chi nhánh</span>
              </div>
              <div class="modal__info-row">
                <span class="modal__info-label">Dữ liệu bị xóa</span>
                <span class="modal__info-value">Tồn kho · Phiếu kho · Kiểm kê · Khách hàng · Nhân viên</span>
              </div>
              <div class="modal__info-row">
                <span class="modal__info-label">Giữ lại</span>
                <span class="modal__info-value">Tài khoản đang đăng nhập · Thông tin chi nhánh · Bản sao lưu</span>
              </div>
            </div>

            <div class="modal__alert modal__alert--danger">
              <i class="fas fa-radiation-alt"></i>
              <span>
                Toàn bộ dữ liệu sẽ bị <strong>XÓA SẠCH</strong>.
                Chi nhánh bị <strong>KHÓA GIAO DỊCH</strong> trong quá trình xóa.
                Bạn có thể <strong>khôi phục lại</strong> từ bản sao lưu đã tạo trước đó.
              </span>
            </div>
          </div>

          <div class="modal__footer">
            <button @click="showWipeConfirm = false" class="btn btn--cancel">Huỷ bỏ</button>
            <button @click="executeBranchWipe" class="btn btn--confirm-danger">
              <i class="fas fa-skull-crossbones"></i> Xác nhận xóa sạch
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<style scoped>
/* ═══════════════════════════════════════════════════════════════════════════
   DESIGN TOKENS
═══════════════════════════════════════════════════════════════════════════ */
.backup-page {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  font-family: 'Inter', sans-serif;
  position: relative;
}

/* ═══ RESTORE OVERLAY ═══════════════════════════════════════════════════════ */
.restore-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.92);
  backdrop-filter: blur(16px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.restore-overlay__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
  text-align: center;
  color: white;
  padding: 2rem;
  max-width: 480px;
}
.restore-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.spinner-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 3px solid transparent;
  animation: spin-ring 1.2s linear infinite;
}
.ring-1 { border-top-color: #4361ee; animation-duration: 1s; }
.ring-2 { inset: 8px; border-right-color: #0ea5e9; animation-duration: 1.4s; animation-direction: reverse; }
.ring-3 { inset: 16px; border-bottom-color: #4cc9f0; animation-duration: 1.8s; }
.spinner-icon { font-size: 1.25rem; color: #4cc9f0; position: relative; z-index: 1; animation: pulse-icon 1.5s ease-in-out infinite; }
.restore-overlay__title { font-size: 1.5rem; font-weight: 800; letter-spacing: -0.02em; }
.restore-overlay__desc { font-size: 0.85rem; color: rgba(255,255,255,0.6); line-height: 1.7; }
.restore-overlay__desc strong { color: #f97316; }
.restore-progress-bar {
  width: 320px;
  height: 4px;
  background: rgba(255,255,255,0.1);
  border-radius: 99px;
  overflow: hidden;
}
.restore-progress-bar__fill {
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, #4361ee, #4cc9f0);
  border-radius: 99px;
  animation: progress-slide 1.8s ease-in-out infinite;
}

/* ═══ PAGE HEADER ════════════════════════════════════════════════════════════ */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.page-header__left { display: flex; align-items: center; gap: 1rem; }
.page-header__icon-wrap {
  position: relative;
  width: 48px; height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #4361ee, #4cc9f0);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.25rem; color: white;
  box-shadow: 0 8px 20px rgba(67,97,238,0.35);
  flex-shrink: 0;
}
.page-header__icon-glow {
  position: absolute;
  inset: -4px;
  border-radius: 18px;
  background: linear-gradient(135deg, #4361ee, #4cc9f0);
  opacity: 0.2;
  filter: blur(8px);
  z-index: -1;
  animation: glow-pulse 2.5s ease-in-out infinite;
}
.page-header__title { font-size: 1.5rem; font-weight: 800; color: #1e293b; letter-spacing: -0.02em; margin: 0; }
.page-header__sub { font-size: 0.8rem; color: #8094ae; margin: 0.2rem 0 0; display: flex; align-items: center; gap: 0.4rem; }
.page-header__actions { display: flex; gap: 0.5rem; align-items: center; }

.badge-enc {
  display: inline-flex;
  align-items: center;
  background: linear-gradient(135deg, rgba(67,97,238,0.12), rgba(76,201,240,0.12));
  color: #4361ee;
  border: 1px solid rgba(67,97,238,0.25);
  border-radius: 6px;
  padding: 1px 6px;
  font-size: 0.7rem;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  letter-spacing: 0.03em;
}

/* ═══ BUTTONS ════════════════════════════════════════════════════════════════ */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.btn:active { transform: scale(0.97); }

.btn--admin {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: white;
  box-shadow: 0 4px 12px rgba(37,99,235,0.3);
}
.btn--admin:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37,99,235,0.4);
}
.btn--admin:disabled { opacity: 0.5; cursor: not-allowed; }

.btn--ghost {
  background: linear-gradient(135deg, #ffffff, #f8fafc);
  color: #2563eb;
  border: 1.5px solid #bfdbfe;
  box-shadow: 0 2px 6px rgba(37,99,235,0.08);
  font-weight: 700;
}
.btn--ghost:hover {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #1d4ed8;
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(37,99,235,0.15);
}

.btn--cancel {
  background: white;
  color: #64748b;
  border: 1px solid #e2e8f0;
}
.btn--cancel:hover { background: #f8fafc; }

.btn--confirm-danger {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: white;
  box-shadow: 0 4px 12px rgba(220,38,38,0.3);
}
.btn--confirm-danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(220,38,38,0.4);
}

/* ═══ WARNING BANNER ═════════════════════════════════════════════════════════ */
.warning-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border: 1px solid #fcd34d;
  border-left: 4px solid #f59e0b;
  border-radius: 12px;
  padding: 0.875rem 1rem;
  font-size: 0.82rem;
  color: #92400e;
  line-height: 1.6;
}
.warning-banner__icon {
  width: 32px; height: 32px;
  background: rgba(245,158,11,0.15);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #d97706;
  flex-shrink: 0;
}

/* ═══ SECURITY BADGES ════════════════════════════════════════════════════════ */
.sec-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.sec-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.7rem;
  background: linear-gradient(135deg, rgba(67,97,238,0.06), rgba(76,201,240,0.06));
  border: 1px solid rgba(67,97,238,0.18);
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #4361ee;
  transition: all 0.2s ease;
}
.sec-badge:hover {
  background: linear-gradient(135deg, rgba(67,97,238,0.12), rgba(76,201,240,0.12));
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(67,97,238,0.15);
}
.sec-badge i { font-size: 0.7rem; }

/* ═══ MAIN GRID ════════════════════════════════════════════════════════════ */
.main-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.25rem;
}
@media (min-width: 1024px) {
  .main-grid { grid-template-columns: 5fr 7fr; }
}
.ops-col { display: flex; flex-direction: column; gap: 1.25rem; }

/* ═══ CARD BASE ══════════════════════════════════════════════════════════════ */
.card {
  background: white;
  border: 1px solid #cbd5e1; /* Đậm hơn xíu */
  border-radius: 18px;
  padding: 1.5rem;
  position: relative;
  overflow: hidden;
  transition: box-shadow 0.3s ease, transform 0.3s ease, border-color 0.3s ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.card:hover {
  box-shadow: 0 10px 30px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.card__glow {
  position: absolute;
  width: 200px; height: 200px;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.15;
  pointer-events: none;
  top: -40px; right: -40px;
  transition: opacity 0.3s, transform 0.3s;
}
.card:hover .card__glow { opacity: 0.25; transform: scale(1.1); }
.card__glow--blue { background: #4361ee; }
.card__glow--green { background: #10b981; }
.card__glow--indigo { background: #6366f1; }

.card--backup {
  background: linear-gradient(145deg, #ffffff, #f5f8ff);
  border: 1px solid rgba(67, 97, 238, 0.3);
}
.card--backup:hover { border-color: rgba(67, 97, 238, 0.6); }

.card--restore {
  background: linear-gradient(145deg, #ffffff, #f0fdf4);
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.card--restore:hover { border-color: rgba(16, 185, 129, 0.6); }

.history-col {
  background: linear-gradient(180deg, #ffffff, #f5f3ff 400px);
  border: 1px solid rgba(99, 102, 241, 0.3);
}
.history-col:hover { border-color: rgba(99, 102, 241, 0.6); }

.card__header { display: flex; align-items: center; gap: 0.875rem; margin-bottom: 0.875rem; }
.card__icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.25rem;
  flex-shrink: 0;
  transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1);
}
.card:hover .card__icon { transform: scale(1.1) rotate(-3deg); }
.card__icon--blue { background: linear-gradient(135deg, rgba(67,97,238,0.15), rgba(76,201,240,0.15)); color: #4361ee; border: 1px solid rgba(67,97,238,0.3); }
.card__icon--green { background: linear-gradient(135deg, rgba(16,185,129,0.15), rgba(6,214,160,0.15)); color: #10b981; border: 1px solid rgba(16,185,129,0.3); }
.card__icon--indigo { background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(129,140,248,0.15)); color: #6366f1; border: 1px solid rgba(99,102,241,0.3); }

.card__title { font-size: 1.1rem; font-weight: 800; color: #1e293b; margin: 0; }
.card__subtitle { font-size: 0.8rem; color: #64748b; font-weight: 500; margin: 0.15rem 0 0; }
.card__desc { font-size: 0.85rem; color: #475569; font-weight: 500; line-height: 1.65; margin-bottom: 1rem; }
.card__desc code {
  background: rgba(67,97,238,0.1); color: #3a52ce;
  padding: 2px 6px; border-radius: 6px;
  font-size: 0.8rem; font-weight: 700;
}

/* ═══ ACTION BUTTONS (large) ══════════════════════════════════════════════════ */
.btn-action {
  width: 100%;
  display: flex; align-items: center; justify-content: center; gap: 0.6rem;
  padding: 0.85rem 1.25rem;
  border-radius: 12px;
  font-size: 0.88rem; font-weight: 700;
  cursor: pointer; border: none;
  transition: all 0.25s cubic-bezier(0.34,1.56,0.64,1);
  position: relative; overflow: hidden;
  letter-spacing: 0.01em;
}
.btn-action::after {
  content: '';
  position: absolute; inset: 0;
  background: rgba(255,255,255,0.15);
  opacity: 0;
  transition: opacity 0.2s;
}
.btn-action:hover::after { opacity: 1; }
.btn-action:hover:not(:disabled) { transform: translateY(-2px); }
.btn-action:active:not(:disabled) { transform: scale(0.98); }
.btn-action:disabled { opacity: 0.45; cursor: not-allowed; filter: grayscale(0.3); }

.btn-action--blue {
  background: linear-gradient(135deg, #4361ee, #4cc9f0);
  color: white;
  box-shadow: 0 6px 16px rgba(67,97,238,0.35);
}
.btn-action--blue:hover:not(:disabled) { box-shadow: 0 10px 24px rgba(67,97,238,0.45); }

.btn-action--green {
  background: linear-gradient(135deg, #10b981, #06d6a0);
  color: white;
  box-shadow: 0 6px 16px rgba(16,185,129,0.35);
}
.btn-action--green:hover:not(:disabled) { box-shadow: 0 10px 24px rgba(16,185,129,0.45); }

.btn-action--teal {
  background: linear-gradient(135deg, #0d9488, #0f766e);
  color: white;
  box-shadow: 0 6px 16px rgba(13,148,136,0.35);
}
.btn-action--teal:hover:not(:disabled) { box-shadow: 0 10px 24px rgba(13,148,136,0.45); }

.btn-action--amber {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
  box-shadow: 0 6px 16px rgba(245,158,11,0.35);
}
.btn-action--amber:hover:not(:disabled) { box-shadow: 0 10px 24px rgba(245,158,11,0.45); }

.btn-action__icon { font-size: 1rem; }
.btn-action__arrow { margin-left: auto; font-size: 0.9rem; transition: transform 0.2s ease; }
.btn-action:hover .btn-action__arrow { transform: translateX(3px); }

/* ═══ DROPZONE ════════════════════════════════════════════════════════════════ */
.dropzone {
  border: 2px dashed #94a3b8; /* Đậm hơn chút */
  border-radius: 14px;
  padding: 1.5rem 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  min-height: 150px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 1rem;
  background: rgba(255,255,255,0.8);
}
.dropzone:hover { border-color: #10b981; background: rgba(16,185,129,0.05); }
.dropzone--active {
  border-color: #10b981;
  background: rgba(16,185,129,0.1);
  box-shadow: 0 0 0 4px rgba(16,185,129,0.15);
  transform: scale(1.02);
}
.dropzone--filled { border-style: solid; border-color: #10b981; background: rgba(16,185,129,0.08); }
.dropzone__empty { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
.dropzone__upload-anim { position: relative; display: inline-flex; align-items: center; justify-content: center; }
.dropzone__icon {
  font-size: 2.5rem; color: #64748b;
  transition: all 0.3s ease;
  position: relative; z-index: 1;
}
.dropzone:hover .dropzone__icon { color: #10b981; transform: translateY(-3px); }
.dropzone--active .dropzone__icon { color: #10b981; animation: float-up 0.8s ease-in-out infinite alternate; }
.dropzone__ripple {
  position: absolute;
  width: 70px; height: 70px;
  border-radius: 50%;
  border: 2px solid #10b981;
  opacity: 0;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%) scale(0);
}
.dropzone--active .dropzone__ripple { animation: ripple-out 1.5s ease-out infinite; }
.dropzone__label { font-size: 0.95rem; font-weight: 700; color: #334155; }
.dropzone__click { color: #10b981; text-decoration: underline; text-decoration-style: dashed; }
.dropzone__hint { font-size: 0.8rem; color: #64748b; font-weight: 500; }
.dropzone__hint code { font-weight: 800; color: #059669; }

.dropzone__filled-content {
  display: flex; align-items: center; gap: 0.875rem;
  width: 100%; padding: 0.25rem;
}
.dropzone__file-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: white; font-size: 1.25rem;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(16,185,129,0.4);
}
.dropzone__file-info { flex: 1; text-align: left; min-width: 0; }
.dropzone__filename {
  font-size: 0.9rem; font-weight: 800; color: #0f172a;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.dropzone__filesize { font-size: 0.8rem; color: #475569; font-weight: 600; font-family: 'Courier New', monospace; margin-top: 2px; }
.dropzone__remove {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: rgba(239,68,68,0.15);
  color: #dc2626; border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.9rem; flex-shrink: 0;
  transition: all 0.2s ease;
}
.dropzone__remove:hover { background: #dc2626; color: white; transform: scale(1.1); }

/* ═══ HISTORY COLUMN ══════════════════════════════════════════════════════════ */
.history-col { display: flex; flex-direction: column; gap: 0; padding: 0; }
.history-header {
  display: flex; align-items: center; gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
}

/* Skeleton */
.history-skeleton { padding: 1rem 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
.skeleton-row {
  height: 60px; border-radius: 10px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

/* Empty */
.empty-state {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 3rem 1rem; gap: 0.75rem;
}
.empty-state__icon {
  width: 64px; height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
  color: #94a3b8; font-size: 1.5rem;
  display: flex; align-items: center; justify-content: center;
  animation: float-idle 3s ease-in-out infinite;
}
.empty-state__title { font-size: 0.95rem; font-weight: 700; color: #475569; }
.empty-state__sub { font-size: 0.78rem; color: #94a3b8; text-align: center; line-height: 1.6; }

/* History list */
.history-list { flex: 1; display: flex; flex-direction: column; overflow-y: auto; max-height: 520px; }
.history-item {
  display: flex; align-items: center; gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(99,102,241,0.1);
  transition: all 0.25s ease;
  animation: slide-in-row 0.35s ease-out both;
  background: rgba(255,255,255,0.4);
}
.history-item:last-child { border-bottom: none; }
.history-item:hover {
  background: linear-gradient(90deg, rgba(99,102,241,0.05), rgba(99,102,241,0.01));
  padding-left: 1.75rem;
}
.history-item:hover .history-item__actions { opacity: 1; }

.history-item__left { flex-shrink: 0; }
.history-item__type-icon {
  width: 40px; height: 40px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1rem;
  transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1);
}
.history-item:hover .history-item__type-icon { transform: scale(1.15) rotate(-5deg); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.type-auto { background: rgba(99,102,241,0.15); color: #4f46e5; border: 1px solid rgba(99,102,241,0.3); }
.type-manual { background: rgba(16,185,129,0.15); color: #059669; border: 1px solid rgba(16,185,129,0.3); }

.history-item__info { flex: 1; min-width: 0; }
.history-item__filename {
  font-size: 0.85rem; font-weight: 800; color: #0f172a;
  font-family: 'Courier New', monospace;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.history-item__meta {
  display: flex; flex-wrap: wrap; gap: 0.75rem;
  margin-top: 0.35rem; font-size: 0.75rem; color: #475569; font-weight: 600;
}
.history-item__meta span { display: flex; align-items: center; gap: 0.3rem; }
.history-item__meta i { font-size: 0.7rem; color: #64748b; }

.history-item__badge {
  padding: 0.25rem 0.7rem; border-radius: 6px;
  font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em;
  flex-shrink: 0;
}
.badge-auto { background: rgba(99,102,241,0.15); color: #4f46e5; border: 1px solid rgba(99,102,241,0.3); }
.badge-manual { background: rgba(16,185,129,0.15); color: #059669; border: 1px solid rgba(16,185,129,0.3); }

.history-item__actions {
  display: flex; gap: 0.4rem; flex-shrink: 0;
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.action-btn {
  display: inline-flex; align-items: center; gap: 0.3rem;
  padding: 0.4rem 0.8rem; border-radius: 8px;
  font-size: 0.8rem; font-weight: 700; cursor: pointer; border: none;
  transition: all 0.2s cubic-bezier(0.34,1.56,0.64,1);
}
.action-btn:active { transform: scale(0.95); }

.action-btn--restore {
  background: rgba(67,97,238,0.12);
  color: #3b5bdb;
  border: 1px solid rgba(67,97,238,0.3);
}
.action-btn--restore:hover {
  background: #4361ee; color: white;
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(67,97,238,0.4);
}

.action-btn--delete {
  background: rgba(239,68,68,0.12);
  color: #dc2626;
  border: 1px solid rgba(239,68,68,0.3);
  padding: 0.4rem 0.65rem;
}
.action-btn--delete:hover {
  background: #ef4444; color: white;
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(239,68,68,0.4);
}

/* History footer */
.history-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: auto;
  padding: 0.75rem 1.5rem;
  border-top: 1px solid #f1f5f9;
  font-size: 0.72rem; font-weight: 600; color: #94a3b8;
  background: #fafbfc;
  border-radius: 0 0 18px 18px;
}
.history-footer i { margin-right: 0.25rem; }

/* ═══ MODALS ══════════════════════════════════════════════════════════════════ */
.modal-backdrop {
  position: fixed; inset: 0;
  background: rgba(2,6,23,0.6);
  backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex; align-items: center; justify-content: center; padding: 1rem;
}
.modal {
  background: white; border-radius: 20px;
  width: 100%; max-width: 520px;
  box-shadow: 0 25px 60px rgba(0,0,0,0.2);
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.5);
}
.modal--sm { max-width: 400px; }

.modal__header {
  display: flex; align-items: center; gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
}
.modal__header--danger { background: linear-gradient(135deg, #fff1f2, #fef2f2); }
.modal__header-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem; flex-shrink: 0;
}
.modal__header-icon--danger { background: rgba(239,68,68,0.12); color: #dc2626; }
.modal__title { font-size: 1.05rem; font-weight: 800; color: #0f172a; margin: 0; }
.modal__title--center { text-align: center; }
.modal__subtitle { font-size: 0.75rem; margin: 0.2rem 0 0; }
.modal__subtitle--danger { color: #dc2626; }

.modal__body { padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
.modal__body--center { align-items: center; text-align: center; }

.modal__info-grid { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; }
.modal__info-row {
  display: grid; grid-template-columns: 120px 1fr;
  padding: 0.6rem 1rem; font-size: 0.8rem; gap: 0.5rem;
  border-bottom: 1px solid #f1f5f9;
}
.modal__info-row:last-child { border-bottom: none; }
.modal__info-label { color: #64748b; font-weight: 600; }
.modal__info-value { color: #1e293b; font-weight: 700; }
.modal__info-value--mono { font-family: 'Courier New', monospace; font-size: 0.75rem; word-break: break-all; }

.modal__alert {
  display: flex; gap: 0.6rem; align-items: flex-start;
  padding: 0.875rem 1rem; border-radius: 10px; font-size: 0.8rem; line-height: 1.6;
}
.modal__alert--danger {
  background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.2);
  color: #b91c1c;
}
.modal__alert i { margin-top: 2px; flex-shrink: 0; }
.modal__alert strong { color: #dc2626; }

.modal__delete-icon {
  position: relative;
  width: 72px; height: 72px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(239,68,68,0.08);
  border-radius: 50%;
  color: #ef4444; font-size: 1.6rem;
  margin: 0.5rem auto 0.25rem;
}
.modal__delete-ring {
  position: absolute; inset: -6px;
  border: 2px dashed rgba(239,68,68,0.3);
  border-radius: 50%;
  animation: spin-ring 8s linear infinite;
}
.modal__delete-desc { font-size: 0.83rem; color: #64748b; line-height: 1.6; max-width: 280px; }

.modal__footer {
  display: flex; gap: 0.75rem; justify-content: flex-end;
  padding: 1rem 1.5rem;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

/* ═══ ANIMATIONS ══════════════════════════════════════════════════════════════ */
@keyframes spin-ring { to { transform: rotate(360deg); } }
@keyframes pulse-icon { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.7; transform: scale(0.9); } }
@keyframes progress-slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}
@keyframes glow-pulse {
  0%, 100% { opacity: 0.2; transform: scale(1); }
  50% { opacity: 0.35; transform: scale(1.1); }
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@keyframes float-up {
  from { transform: translateY(0); }
  to { transform: translateY(-6px); }
}
@keyframes float-idle {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}
@keyframes ripple-out {
  from { opacity: 0.6; transform: translate(-50%,-50%) scale(0.5); }
  to { opacity: 0; transform: translate(-50%,-50%) scale(2); }
}
@keyframes slide-in-row {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}
@keyframes animate-bounce-icon {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.animate-bounce-icon { animation: animate-bounce-icon 0.6s ease-in-out infinite; }

/* ─── Vue Transitions ─── */
.overlay-enter-active { transition: all 0.25s ease; }
.overlay-leave-active { transition: all 0.2s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }

.modal-enter-active { transition: all 0.25s cubic-bezier(0.34,1.56,0.64,1); }
.modal-leave-active { transition: all 0.18s ease; }
.modal-enter-from { opacity: 0; transform: scale(0.93); }
.modal-leave-to { opacity: 0; transform: scale(0.96); }

.fa-spin { animation: spin-ring 0.85s linear infinite; }

/* ═══ ADMIN: SYSTEM CONFIG SECTION ══════════════════════════════════════════ */
.sys-section {
  display: flex; flex-direction: column; gap: 1rem;
  padding: 1.5rem;
  background: linear-gradient(135deg, #f0fdfa 0%, #f0fdf4 100%);
  border: 2px solid #99f6e4;
  border-radius: 20px;
  position: relative;
}

.sys-section__header {
  display: flex; align-items: center; gap: 1rem;
}
.sys-section__icon-wrap {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #0d9488, #0f766e);
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 1.3rem;
  box-shadow: 0 4px 16px rgba(13,148,136,0.35);
  flex-shrink: 0;
}
.sys-section__title {
  font-size: 1.15rem; font-weight: 800; color: #0f766e; margin: 0 0 0.15rem;
}
.sys-section__sub { font-size: 0.8rem; color: #0d9488; margin: 0; }
.sys-section__badge {
  margin-left: auto; padding: 0.3rem 0.75rem;
  background: linear-gradient(135deg, #0d9488, #0f766e);
  color: white; border-radius: 20px;
  font-size: 0.65rem; font-weight: 800; letter-spacing: 0.08em;
  box-shadow: 0 2px 10px rgba(13,148,136,0.4);
}

.sys-grid {
  display: grid; grid-template-columns: 1fr 1.3fr; gap: 1rem;
}
.sys-ops { display: flex; flex-direction: column; gap: 1rem; }

/* System card variants */
.card--system-export { background: linear-gradient(135deg, #f0fdfa 0%, #ccfbf1 100%); border: 1.5px solid #99f6e4; }
.card--system-export:hover { border-color: #0d9488; box-shadow: 0 16px 40px rgba(13,148,136,0.18); }
.card--system-import { background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%); border: 1.5px solid #fcd34d; }
.card--system-import:hover { border-color: #f59e0b; box-shadow: 0 16px 40px rgba(245,158,11,0.18); }
.card--system-history { background: linear-gradient(135deg, #f0fdfa 0%, #f0fdf4 100%); border: 1.5px solid #99f6e4; }
.card--system-history:hover { border-color: #0d9488; box-shadow: 0 16px 40px rgba(13,148,136,0.18); }

/* Icon colors */
.card__icon--teal { background: linear-gradient(135deg, #0d9488, #0f766e); color: white; }
.card__glow--teal { background: radial-gradient(circle at 50% 0%, rgba(13,148,136,0.15) 0%, transparent 70%); }
.card__icon--amber { background: linear-gradient(135deg, #f59e0b, #d97706); }
.card__glow--amber { background: radial-gradient(circle at 50% 0%, rgba(245,158,11,0.15) 0%, transparent 70%); }
.card__icon--slate { background: linear-gradient(135deg, #475569, #334155); }
.card__glow--slate { background: radial-gradient(circle at 50% 0%, rgba(71,85,105,0.1) 0%, transparent 70%); }

/* Sys action buttons */
.btn--sys-export {
  background: linear-gradient(135deg, #0d9488 0%, #0f766e 100%);
  color: white; border: none;
  box-shadow: 0 4px 16px rgba(13,148,136,0.3);
}
.btn--sys-export:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(13,148,136,0.45);
}
.btn--sys-restore {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white; border: none;
  box-shadow: 0 4px 16px rgba(245,158,11,0.3);
}
.btn--sys-restore:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245,158,11,0.45);
}

/* type-system badge */
.type-system {
  background: linear-gradient(135deg, #0d9488, #0f766e);
  color: white;
  box-shadow: 0 2px 8px rgba(13,148,136,0.3);
}

/* warning-banner--teal */
.warning-banner--teal {
  background: linear-gradient(135deg, #f0fdfa, #ccfbf1);
  border-color: #99f6e4;
  color: #115e59;
}
.warning-banner--teal .warning-banner__icon { color: #0d9488; }

/* sys-scope-text */
.sys-scope-text {
  font-size: 0.82rem; color: #64748b; line-height: 1.7; margin: 0;
  background: rgba(13,148,136,0.05);
  border: 1px solid rgba(13,148,136,0.15);
  border-radius: 10px; padding: 0.75rem 1rem;
}
.sys-scope-text i { color: #0d9488; margin-right: 0.35rem; }
.sys-scope-text code {
  background: rgba(13,148,136,0.12); color: #115e59;
  padding: 0.1rem 0.4rem; border-radius: 4px; font-size: 0.78rem;
}

/* modal header teal variant */
.modal__header--teal {
  background: linear-gradient(135deg, #0d9488 0%, #0f766e 100%);
}
.modal__alert--warning {
  display: flex; align-items: flex-start; gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: #fffbeb; border: 1px solid #fcd34d;
  border-radius: 10px; color: #92400e; font-size: 0.82rem;
}
.modal__alert--warning i { color: #f59e0b; margin-top: 0.1rem; flex-shrink: 0; }
.btn--confirm-teal {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.7rem 1.5rem; border-radius: 10px; font-weight: 700;
  background: linear-gradient(135deg, #0d9488, #0f766e);
  color: white; border: none; cursor: pointer;
  transition: all 0.2s ease;
}
.btn--confirm-teal:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(13,148,136,0.45);
}

/* Responsive sys-grid */
@media (max-width: 900px) {
  .sys-grid { grid-template-columns: 1fr; }
}

/* ═══ WIPE CARD ═════════════════════════════════════════════════════════════ */
.card--wipe {
  border: 1px solid rgba(239, 68, 68, 0.25);
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.03) 0%, rgba(185, 28, 28, 0.05) 100%);
}
.card--wipe:hover { border-color: rgba(239, 68, 68, 0.5); }
.card__glow--red {
  background: radial-gradient(ellipse at top left, rgba(239, 68, 68, 0.15) 0%, transparent 60%);
}
.card__icon--red {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  box-shadow: 0 6px 16px rgba(239, 68, 68, 0.35);
}
.btn-action--red {
  background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.25);
}
.btn-action--red:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.4);
}
.btn-action--red:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* GZIP security badge */
.sec-badge--gzip {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.2);
  color: #059669;
}

</style>
