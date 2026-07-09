<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

// ─── View Mode ──────────────────────────────────────────────────────────────

// ─── Tabs ───────────────────────────────────────────────────────────────────
const activeTab = ref<'products' | 'categories'>('products')

// ─── Products ───────────────────────────────────────────────────────────────
const products = ref<any[]>([])
const categories = ref<any[]>([])
const pLoading = ref(true)
const pSearch = ref('')
const pCategoryId = ref<number | ''>('')

const filteredProducts = computed(() => {
  let list = products.value
  if (pSearch.value.trim()) {
    const kw = pSearch.value.toLowerCase()
    list = list.filter(p =>
      p.name?.toLowerCase().includes(kw) ||
      p.sku?.toLowerCase().includes(kw)
    )
  }
  if (pCategoryId.value !== '') {
    list = list.filter(p => p.categoryId === pCategoryId.value)
  }
  return list
})

// Product Modal
const showProductModal = ref(false)
const isSpaceEasterEgg = ref(false)
const editingProduct = ref<any>(null)
const productForm = reactive({
  name: '', categoryId: '' as number | '',
  importPrice: '' as any, price: '' as any, unit: 'Chiếc', imageUrl: ''
})
const pSaving = ref(false)
const uploadingImage = ref(false)

// Conflict Dialog
const showConflictDialog = ref(false)
const conflictProductId = ref<number | null>(null)
const conflictMessage = ref('')

async function handleImageUpload(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return
  
  const file = target.files[0]
  const formData = new FormData()
  formData.append('file', file)
  
  uploadingImage.value = true
  try {
    const res = await api.upload('/api/upload', formData)
    const data = await res.json()
    if (res.ok) {
      productForm.imageUrl = data.url
      toast.success('Tải ảnh lên thành công!')
    } else {
      toast.error(data.error || 'Lỗi tải ảnh')
    }
  } catch (error) {
    toast.error('Không thể kết nối máy chủ khi tải ảnh.')
  } finally {
    uploadingImage.value = false
    // Reset file input so user can select the same file again if needed
    target.value = ''
  }
}

function openAddProduct() {
  isSpaceEasterEgg.value = Math.random() < 0.004;
  editingProduct.value = null
  Object.assign(productForm, { name: '', categoryId: '', importPrice: '', price: '', unit: 'Chiếc', imageUrl: '' })
  showProductModal.value = true
}
function openEditProduct(p: any) {
  isSpaceEasterEgg.value = Math.random() < 0.004;
  editingProduct.value = p
  Object.assign(productForm, {
    name: p.name, categoryId: p.categoryId || '',
    importPrice: p.importPrice, price: p.price, unit: p.unit || 'Chiếc', imageUrl: p.imageUrl || ''
  })
  showProductModal.value = true
}
async function saveProduct(forceCreate = false) {
  if (!productForm.name?.trim()) {
    toast.error('Tên sản phẩm là bắt buộc.')
    return
  }
  if (!productForm.categoryId) {
    toast.error('Vui lòng chọn danh mục cho sản phẩm.')
    return
  }
  pSaving.value = true
  try {
    const payload = {
      name: productForm.name.trim(),
      categoryId: productForm.categoryId || null,
      importPrice: productForm.importPrice || 0,
      price: productForm.price || 0,
      unit: productForm.unit || 'Chiếc',
      imageUrl: productForm.imageUrl
    }
    const res = editingProduct.value
      ? await api.put(`/api/products/${editingProduct.value.id}`, payload)
      : await api.post('/api/products', payload)
    
    let data = {}
    try { data = await res.json() } catch {}

    if (res.ok) {
      toast.success(editingProduct.value ? 'Cập nhật sản phẩm thành công!' : 'Thêm sản phẩm thành công!')
      showProductModal.value = false
      await loadProducts()
    } else {
      toast.error((data as any).message || 'Có lỗi xảy ra.')
    }
  } catch { toast.error('Không thể kết nối máy chủ.') }
  finally { pSaving.value = false }
}

// Delete product
const showDeleteProduct = ref(false)
const deletingProduct = ref<any>(null)
function confirmDeleteProduct(p: any) { deletingProduct.value = p; showDeleteProduct.value = true }
async function doDeleteProduct() {
  if (!deletingProduct.value) return
  try {
    const res = await api.delete(`/api/products/${deletingProduct.value.id}`)
    let data = {}
    try { data = await res.json() } catch {}
    if (res.ok) { toast.success('Xóa sản phẩm thành công!'); await loadProducts() }
    else toast.error((data as any).message || 'Không thể xóa sản phẩm.')
  } catch { toast.error('Có lỗi xảy ra.') }
  finally { showDeleteProduct.value = false }
}

// Import Excel
const importingExcel = ref(false)
const pendingImportFile = ref<File | null>(null)
const importPreviewResult = ref<any>(null)
const importResult = ref<{ newCount?: number; updateCount?: number; skippedCount?: number; successCount: number; errors: string[]; updateDetails?: string[] } | null>(null)

function downloadTemplate() {
  window.open('/api/products/template', '_blank')
}

async function handleExcelImport(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return
  
  const file = target.files[0]
  pendingImportFile.value = file
  
  const formData = new FormData()
  formData.append('file', file)
  
  importingExcel.value = true
  try {
    const res = await api.upload('/api/products/import?preview=true', formData)
    const data = await res.json()
    if (res.ok) {
      importPreviewResult.value = data
    } else {
      toast.error(data.message || 'Lỗi phân tích file Excel')
      pendingImportFile.value = null
    }
  } catch (error) {
    toast.error('Không thể kết nối máy chủ.')
    pendingImportFile.value = null
  } finally {
    importingExcel.value = false
    target.value = ''
  }
}

async function confirmExcelImport() {
  if (!pendingImportFile.value) return
  
  const formData = new FormData()
  formData.append('file', pendingImportFile.value)
  
  importingExcel.value = true
  try {
    const res = await api.upload('/api/products/import?preview=false', formData)
    const data = await res.json()
    if (res.ok) {
      importResult.value = data
      toast.success('Nhập và cập nhật sản phẩm thành công!')
      await loadProducts()
    } else {
      toast.error(data.message || 'Lỗi khi lưu dữ liệu Excel')
    }
  } catch (error) {
    toast.error('Không thể kết nối máy chủ.')
  } finally {
    importingExcel.value = false
    importPreviewResult.value = null
    pendingImportFile.value = null
  }
}

function cancelExcelImport() {
  importPreviewResult.value = null
  pendingImportFile.value = null
}

// ─── Categories ─────────────────────────────────────────────────────────────
const cLoading = ref(false)
const cSearch = ref('')

const filteredCategories = computed(() => {
  if (!cSearch.value.trim()) return categories.value
  const kw = cSearch.value.toLowerCase()
  return categories.value.filter(c => c.name?.toLowerCase().includes(kw))
})

const showCatModal = ref(false)
const editingCat = ref<any>(null)
const catForm = reactive({ name: '', description: '' })
const catSaving = ref(false)
const showDeleteCat = ref(false)
const deletingCat = ref<any>(null)

function openAddCat() { isSpaceEasterEgg.value = Math.random() < 0.004; editingCat.value = null; Object.assign(catForm, { name: '', description: '' }); showCatModal.value = true }
function openEditCat(c: any) { isSpaceEasterEgg.value = Math.random() < 0.004; editingCat.value = c; Object.assign(catForm, { name: c.name, description: c.description || '' }); showCatModal.value = true }
function confirmDeleteCat(c: any) { deletingCat.value = c; showDeleteCat.value = true }

async function saveCat() {
  if (!catForm.name?.trim()) { toast.error('Tên danh mục không được để trống.'); return }
  catSaving.value = true
  try {
    const payload = { name: catForm.name.trim(), description: catForm.description }
    const res = editingCat.value
      ? await api.put(`/api/categories/${editingCat.value.id}`, payload)
      : await api.post('/api/categories', payload)
    let data = {}
    try { data = await res.json() } catch {}
    if (res.ok) {
      toast.success(editingCat.value ? 'Cập nhật danh mục thành công!' : 'Thêm danh mục thành công!')
      showCatModal.value = false
      await loadCategories()
    } else toast.error((data as any).message || 'Có lỗi xảy ra.')
  } catch { toast.error('Không thể kết nối máy chủ.') }
  finally { catSaving.value = false }
}
async function doDeleteCat() {
  if (!deletingCat.value) return
  try {
    const res = await api.delete(`/api/categories/${deletingCat.value.id}`)
    let data = {}
    try { data = await res.json() } catch {}
    if (res.ok) { toast.success('Xóa danh mục thành công!'); await loadCategories() }
    else toast.error((data as any).message || 'Không thể xóa danh mục.')
  } catch { toast.error('Có lỗi xảy ra.') }
  finally { showDeleteCat.value = false }
}

// ─── Load data ───────────────────────────────────────────────────────────────
async function loadProducts() {
  pLoading.value = true
  try {
    const res = await api.get('/api/products')
    if (res.ok) {
      const data = await res.json()
      products.value = data.content || data
    }
  } catch {} finally { pLoading.value = false }
}
async function loadCategories() {
  cLoading.value = true
  try {
    const res = await api.get('/api/categories')
    if (res.ok) {
      const data = await res.json()
      categories.value = data.content || data
    }
  } catch {} finally { cLoading.value = false }
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadCategories()])
})

function formatCurrency(val: any) {
  if (!val) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val)
}

</script>

<template>
  <div class="products-view-container space-y-6 max-w-[1400px] mx-auto relative">
    


    <!-- Main Content -->
    <div class="relative z-10">
    <!-- Header & Tabs -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2">
      <div>
        <h2 class="text-2xl font-bold text-slate-800 dark:text-white m-0 transition-colors">Quản lý Sản phẩm</h2>
        <p class="text-slate-500 dark:text-slate-400 text-sm mt-1 transition-colors">Quản lý danh mục và danh sách sản phẩm trong kho</p>
      </div>
      
      <!-- Tabs -->
      <div class="flex items-center gap-6 border-b border-slate-200 dark:border-slate-700/50">
        <button
          v-for="tab in [{ key: 'products', label: 'Sản phẩm', icon: 'fas fa-box' }, { key: 'categories', label: 'Danh mục', icon: 'fas fa-tags' }]"
          :key="tab.key"
          :class="[
            'flex items-center gap-2 pb-3 px-1 text-sm font-bold transition-colors relative',
            activeTab === tab.key ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
          ]"
          @click="activeTab = tab.key as any"
        >
          <i :class="tab.icon"></i>
          {{ tab.label }}
          <div v-if="activeTab === tab.key" class="absolute bottom-[-1px] left-0 w-full h-[2px] bg-indigo-600 dark:bg-indigo-500 rounded-t-full transition-all"></div>
        </button>
      </div>
    </div>

    <!-- ── PRODUCTS TAB ── -->
    <Transition name="fade" mode="out-in">
      <div v-if="activeTab === 'products'" class="bg-indigo-50/50 dark:bg-slate-800/40 rounded-[16px] border border-slate-100 dark:border-slate-700/50 border-t-4 border-t-indigo-500 shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden transition-colors">
        <!-- Toolbar -->
        <div class="p-5 border-b border-slate-100 dark:border-slate-700/50 flex items-center justify-between flex-wrap gap-4 bg-slate-50 dark:bg-slate-800/80 transition-colors">
          <div class="flex items-center gap-3 flex-1 min-w-[300px]">
            <div class="relative w-[300px]">
              <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500"></i>
              <input
                v-model="pSearch"
                type="text"
                placeholder="Tìm theo tên, SKU..."
                class="w-full h-[42px] pl-11 pr-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200"
              />
            </div>
            <select
              v-model="pCategoryId"
              class="h-[42px] px-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition-all text-slate-700 dark:text-slate-200 cursor-pointer"
            >
              <option value="">Tất cả danh mục</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          
          <div class="flex items-center gap-4">


            <div v-if="isManager" class="flex items-center gap-2">
              <button
                class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-600 text-slate-700 dark:text-slate-200 px-4 py-2.5 rounded-xl font-semibold shadow-sm hover:bg-slate-50 dark:hover:bg-slate-700 transition-all text-sm flex items-center gap-2"
                @click="downloadTemplate"
              >
                <i class="fas fa-download text-emerald-500"></i>
                <span class="hidden xl:inline">Tải mẫu Excel</span>
              </button>
              
              <label class="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-600 text-slate-700 dark:text-slate-200 px-4 py-2.5 rounded-xl font-semibold shadow-sm hover:bg-slate-50 dark:hover:bg-slate-700 transition-all text-sm flex items-center gap-2 cursor-pointer" :class="{'opacity-50 cursor-not-allowed': importingExcel}">
                <i v-if="importingExcel" class="fas fa-spinner fa-spin text-emerald-500"></i>
                <i v-else class="fas fa-file-excel text-emerald-500"></i>
                <span class="hidden lg:inline">{{ importingExcel ? 'Đang nhập...' : 'Nhập Excel' }}</span>
                <input type="file" accept=".xlsx, .xls" class="hidden" @change="handleExcelImport" :disabled="importingExcel" />
              </label>
              
              <button
                class="bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2"
                @click="openAddProduct"
              >
                <i class="fas fa-plus"></i>
                Thêm <span class="hidden sm:inline">sản phẩm</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Content Area -->
        <div class="p-0 transition-colors">
          <div v-if="pLoading" class="p-8 space-y-4">
            <div v-for="i in 5" :key="i" class="h-12 bg-slate-100 dark:bg-slate-800 rounded-xl animate-pulse" />
          </div>
          <div v-else-if="filteredProducts.length === 0" class="py-24 text-center text-slate-400 dark:text-slate-500 flex flex-col items-center">
            <div class="w-24 h-24 mb-6 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center animate-pulse-slow">
              <i class="fas fa-box-open text-4xl opacity-50"></i>
            </div>
            <div class="font-bold text-slate-700 dark:text-slate-300 text-lg">Không tìm thấy sản phẩm nào</div>
            <div class="text-sm mt-2 max-w-[300px]">Hãy thêm sản phẩm mới hoặc thay đổi từ khóa tìm kiếm để xem kết quả.</div>
          </div>
          
          <!-- View: List (Table) -->
          <div v-else class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead class="bg-white dark:bg-slate-800/50">
                <tr>
                  <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Sản phẩm</th>
                  <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Danh mục</th>
                  <th class="p-4 text-center text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Đơn vị</th>
                  <th class="p-4 text-right text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Giá nhập</th>
                  <th class="p-4 text-right text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Giá bán</th>
                  <th v-if="isManager" class="p-4 border-b border-slate-100 dark:border-slate-700/50 w-[100px]"></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="p in filteredProducts"
                  :key="p.id"
                  class="border-b border-slate-100 dark:border-slate-700/50 hover:border-transparent hover:bg-slate-50 dark:hover:bg-slate-700/50 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]"
                  @click="isManager ? openEditProduct(p) : null"
                >
                  <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                    <div class="flex items-center gap-3">
                      <img v-if="p.imageUrl" :src="p.imageUrl" class="w-10 h-10 rounded-lg object-cover border border-slate-200 dark:border-slate-600" />
                      <div v-else class="w-10 h-10 rounded-lg bg-slate-100 dark:bg-slate-700 flex items-center justify-center text-slate-400 border border-slate-200 dark:border-slate-600">
                        <i class="fas fa-box"></i>
                      </div>
                      <div>
                        <div class="font-bold text-slate-800 dark:text-slate-200">{{ p.name }}</div>
                        <div class="text-xs text-slate-500 dark:text-slate-400 font-mono mt-0.5">{{ p.sku }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                    <span v-if="p.categoryId || p.categoryName" class="px-3 py-1 bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 rounded-full text-xs font-bold">{{ p.categoryName || categories.find(c => c.id === p.categoryId)?.name || 'Không xác định' }}</span>
                    <span v-else class="text-slate-400 dark:text-slate-500">—</span>
                  </td>
                  <td class="p-4 text-center font-medium text-slate-700 dark:text-slate-300 first:rounded-l-xl last:rounded-r-xl">
                    <span class="px-3 py-1 bg-slate-100 dark:bg-slate-700 rounded-full text-xs font-bold">{{ p.unit || 'Chiếc' }}</span>
                  </td>
                  <td class="p-4 text-right font-mono font-bold text-slate-500 dark:text-slate-400 first:rounded-l-xl last:rounded-r-xl">{{ formatCurrency(p.importPrice) }}</td>
                  <td class="p-4 text-right font-mono font-bold text-indigo-600 dark:text-indigo-400 first:rounded-l-xl last:rounded-r-xl">{{ formatCurrency(p.price) }}</td>
                  <td v-if="isManager" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                    <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                      <button class="w-8 h-8 rounded-lg text-sky-500 dark:text-sky-400 bg-white dark:bg-slate-800 hover:bg-sky-50 dark:hover:bg-sky-900/30 flex items-center justify-center transition-colors shadow-sm border border-slate-200 dark:border-slate-600" @click.stop="openEditProduct(p)" title="Sửa">
                        <i class="fas fa-pen text-sm"></i>
                      </button>
                      <button class="w-8 h-8 rounded-lg text-rose-500 dark:text-rose-400 bg-white dark:bg-slate-800 hover:bg-rose-50 dark:hover:bg-rose-900/30 flex items-center justify-center transition-colors shadow-sm border border-slate-200 dark:border-slate-600" @click.stop="confirmDeleteProduct(p)" title="Xóa">
                        <i class="fas fa-trash text-sm"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          

          
          <div v-if="!pLoading && filteredProducts.length > 0" class="px-6 py-4 bg-slate-50 dark:bg-slate-800/80 border-t border-slate-100 dark:border-slate-700/50 text-xs font-bold text-slate-500 dark:text-slate-400 transition-colors">
            Tổng cộng: {{ filteredProducts.length }} sản phẩm
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── CATEGORIES TAB ── -->
    <Transition name="fade" mode="out-in">
      <div v-if="activeTab === 'categories'" class="bg-sky-50/50 dark:bg-slate-800/40 rounded-[16px] border border-slate-100 dark:border-slate-700/50 border-t-4 border-t-sky-500 shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden transition-colors">
        <!-- Toolbar -->
        <div class="p-5 border-b border-slate-100 dark:border-slate-700/50 flex items-center justify-between flex-wrap gap-4 bg-slate-50 dark:bg-slate-800/80 transition-colors">
          <div class="flex items-center gap-3 flex-1 min-w-[300px]">
            <div class="relative w-[300px]">
              <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500"></i>
              <input
                v-model="cSearch"
                type="text"
                placeholder="Tìm danh mục..."
                class="w-full h-[42px] pl-11 pr-4 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 rounded-xl text-sm focus:ring-2 focus:ring-sky-500/20 focus:border-sky-500 outline-none transition-all text-slate-700 dark:text-slate-200"
              />
            </div>
          </div>
          
          <div class="flex items-center gap-4">
            <button
              v-if="isManager"
              class="bg-sky-500 hover:bg-sky-600 dark:bg-sky-500 dark:hover:bg-sky-400 text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2"
              @click="openAddCat"
            >
              <i class="fas fa-plus"></i>
              Thêm <span class="hidden sm:inline">danh mục</span>
            </button>
          </div>
        </div>

        <!-- Content Area -->
        <div class="p-0 transition-colors">
          <div v-if="cLoading" class="p-8 space-y-4">
            <div v-for="i in 4" :key="i" class="h-12 bg-slate-100 dark:bg-slate-800 rounded-xl animate-pulse" />
          </div>
          <div v-else-if="filteredCategories.length === 0" class="py-24 text-center text-slate-400 dark:text-slate-500 flex flex-col items-center">
            <div class="w-24 h-24 mb-6 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center animate-pulse-slow">
              <i class="fas fa-layer-group text-4xl opacity-50"></i>
            </div>
            <div class="font-bold text-slate-700 dark:text-slate-300 text-lg">{{ cSearch ? 'Không tìm thấy danh mục' : 'Chưa có danh mục nào' }}</div>
            <div class="text-sm mt-2 max-w-[300px]">Thêm danh mục mới để bắt đầu tổ chức sản phẩm.</div>
          </div>
          
          <!-- View: List -->
          <div v-else class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead class="bg-white dark:bg-slate-800/50">
                <tr>
                  <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Tên danh mục</th>
                  <th class="p-4 text-[0.75rem] uppercase font-bold text-slate-500 dark:text-slate-400 tracking-wider border-b border-slate-100 dark:border-slate-700/50">Mô tả</th>
                  <th v-if="isManager" class="p-4 border-b border-slate-100 dark:border-slate-700/50 w-[100px]"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in filteredCategories" :key="c.id" class="border-b border-slate-100 dark:border-slate-700/50 hover:border-transparent hover:bg-slate-50 dark:hover:bg-slate-700/50 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]" @click="isManager ? openEditCat(c) : null">
                  <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                    <div class="flex items-center gap-3">
                      <div class="w-10 h-10 rounded-lg bg-sky-50 dark:bg-sky-900/30 flex items-center justify-center border border-sky-100 dark:border-sky-800/50">
                        <i class="fas fa-tag text-sky-500 dark:text-sky-400"></i>
                      </div>
                      <span class="font-bold text-slate-800 dark:text-slate-200">{{ c.name }}</span>
                    </div>
                  </td>
                  <td class="p-4 text-sm text-slate-500 dark:text-slate-400 first:rounded-l-xl last:rounded-r-xl">{{ c.description || '—' }}</td>
                  <td v-if="isManager" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                    <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                      <button class="w-8 h-8 rounded-lg text-sky-500 dark:text-sky-400 bg-white dark:bg-slate-800 hover:bg-sky-50 dark:hover:bg-sky-900/30 flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-slate-200 dark:border-slate-600" @click.stop="openEditCat(c)" title="Sửa">
                        <i class="fas fa-pen text-sm"></i>
                      </button>
                      <button class="w-8 h-8 rounded-lg text-rose-500 dark:text-rose-400 bg-white dark:bg-slate-800 hover:bg-rose-50 dark:hover:bg-rose-900/30 flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-slate-200 dark:border-slate-600" @click.stop="confirmDeleteCat(c)" title="Xóa">
                        <i class="fas fa-trash text-sm"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <div v-if="!cLoading && filteredCategories.length > 0" class="px-6 py-4 bg-slate-50 dark:bg-slate-800/80 border-t border-slate-100 dark:border-slate-700/50 text-xs font-bold text-slate-500 dark:text-slate-400 transition-colors">
            Tổng cộng: {{ filteredCategories.length }} danh mục
          </div>
        </div>
      </div>
    </Transition>

    <!-- ── PRODUCT RIGHT PANEL (Glassmorphism + Dark Mode) ── -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showProductModal" @click="showProductModal = false" class="fixed inset-0 bg-slate-900/40 dark:bg-slate-900/70 backdrop-blur-sm z-[100] transition-colors"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showProductModal" class="fixed inset-y-0 right-0 z-[101] w-full sm:w-[450px] bg-white dark:bg-slate-800 shadow-[-15px_0_40px_rgba(0,0,0,0.15)] flex flex-col border-l border-slate-200 dark:border-slate-700/80 transition-colors">
          <!-- Header -->
          <div class="theme-modal-header relative overflow-hidden flex items-center justify-between px-8 py-6 transition-colors duration-500" :class="{ 'easter-egg-space': isSpaceEasterEgg }">
            <template v-if="isSpaceEasterEgg">
              <!-- Space Easter Egg Decor -->
              <div class="absolute inset-0 pointer-events-none">
                <i class="fas fa-rocket absolute top-4 right-32 text-white/80 text-4xl animate-[bounce_3s_infinite] -rotate-45"></i>
                <i class="fas fa-meteor absolute -top-4 right-16 text-orange-400/60 text-6xl rotate-[120deg] drop-shadow-[0_0_15px_rgba(251,146,60,0.8)]"></i>
                <i class="fas fa-user-astronaut absolute bottom-2 right-64 text-white/60 text-3xl animate-[bounce_4s_infinite]"></i>
                <i class="fas fa-star absolute top-2 right-48 text-white/90 text-[8px] animate-pulse"></i>
                <i class="fas fa-star absolute bottom-4 right-20 text-white/70 text-[6px] animate-pulse" style="animation-delay: 1s"></i>
                <i class="fas fa-satellite absolute top-8 right-80 text-white/50 text-2xl animate-[spin_20s_linear_infinite]"></i>
              </div>
            </template>
            <template v-else>
              <!-- Light Mode Decor: Sun & Clouds -->
              <div :key="'light-product-modal'" class="theme-light-decor absolute inset-0 pointer-events-none transition-all duration-500">
                <i class="fas fa-sun absolute -top-12 right-8 text-yellow-300 text-[140px] opacity-10 animate-[spin_40s_linear_infinite]"></i>
                <i class="fas fa-sun absolute top-3 right-24 text-yellow-300 text-5xl drop-shadow-[0_0_20px_rgba(253,224,71,0.8)] animate-[spin_20s_linear_infinite]"></i>
                <i class="fas fa-cloud absolute top-8 right-44 text-white/50 text-5xl drop-shadow-sm"></i>
                <i class="fas fa-cloud absolute top-2 right-64 text-white/40 text-3xl"></i>
                <i class="fas fa-cloud absolute -bottom-2 right-28 text-white/30 text-7xl"></i>
              </div>

              <!-- Dark Mode Decor: Moon & Stars -->
              <div :key="'dark-product-modal'" class="theme-dark-decor absolute inset-0 pointer-events-none transition-all duration-500">
                <i class="fas fa-moon absolute -top-8 right-12 text-blue-100 text-[120px] opacity-[0.03] -rotate-12"></i>
                <i class="fas fa-moon absolute top-3 right-24 text-yellow-200 text-4xl drop-shadow-[0_0_15px_rgba(254,240,138,0.5)] -rotate-12"></i>
                <i class="fas fa-star absolute top-4 right-48 text-white/80 text-[8px] animate-pulse"></i>
                <i class="fas fa-star absolute top-8 right-60 text-white/60 text-[10px] animate-pulse" style="animation-delay: 1s"></i>
                <i class="fas fa-star absolute top-3 right-72 text-white/90 text-[6px] animate-pulse" style="animation-delay: 0.5s"></i>
                <i class="fas fa-star absolute bottom-4 right-36 text-white/50 text-[12px] animate-pulse" style="animation-delay: 1.5s"></i>
                <i class="fas fa-star absolute bottom-2 right-56 text-white/70 text-[8px] animate-pulse"></i>
              </div>
            </template>

            <h3 class="font-bold text-white text-lg flex items-center gap-2 relative z-10 drop-shadow-md">
              <i class="fas fa-box text-white"></i>
              {{ editingProduct ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới' }}
            </h3>
            <button @click="showProductModal = false" class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- Body -->
          <div class="p-6 flex-1 overflow-y-auto space-y-6 custom-scrollbar bg-white dark:bg-slate-800 transition-colors">
            <div class="grid grid-cols-2 gap-5">
              <div class="col-span-2">
                <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Tên sản phẩm <span class="text-rose-500">*</span></label>
                <input v-model="productForm.name" type="text" placeholder="Nhập tên sản phẩm" class="w-full h-12 px-4 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 outline-none transition-all text-slate-800 dark:text-slate-200 shadow-sm inset-shadow" />
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Danh mục</label>
                <select v-model="productForm.categoryId" class="w-full h-12 px-4 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 outline-none transition-all text-slate-800 dark:text-slate-200 shadow-sm cursor-pointer">
                  <option value="">-- Không phân loại --</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Giá nhập (VNĐ)</label>
                <div class="relative">
                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">₫</span>
                  <input v-model="productForm.importPrice" type="number" min="0" placeholder="0" class="w-full h-12 pl-8 pr-4 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 outline-none transition-all text-slate-800 dark:text-slate-200 shadow-sm font-mono" />
                </div>
              </div>
              <div>
                <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Giá bán (VNĐ) <span class="text-rose-500">*</span></label>
                <div class="relative">
                  <span class="absolute left-4 top-1/2 -translate-y-1/2 text-indigo-500 font-bold">₫</span>
                  <input v-model="productForm.price" type="number" min="0" placeholder="0" class="w-full h-12 pl-8 pr-4 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 outline-none transition-all text-indigo-700 dark:text-indigo-400 shadow-sm font-mono font-bold" />
                </div>
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Đơn vị tính <span class="text-rose-500">*</span></label>
                <input v-model="productForm.unit" type="text" placeholder="Chiếc, Hộp..." class="w-full h-12 px-4 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 outline-none transition-all text-slate-800 dark:text-slate-200 shadow-sm" />
              </div>
            </div>

            <!-- Image Upload Section -->
            <div class="pt-2">
              <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">Hình ảnh minh họa</label>
              
              <div v-if="productForm.imageUrl" class="relative w-full aspect-video rounded-2xl border-2 border-slate-200 dark:border-slate-700 overflow-hidden group shadow-sm bg-slate-50 dark:bg-slate-900/50">
                <img :src="productForm.imageUrl" class="w-full h-full object-contain p-2" />
                <!-- Hover Overlay -->
                <div class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-4">
                  <button @click.prevent="productForm.imageUrl = ''" class="bg-rose-500 hover:bg-rose-600 text-white px-4 py-2 rounded-xl font-bold shadow-lg transition-transform hover:scale-105 flex items-center gap-2">
                    <i class="fas fa-trash-alt"></i> Xóa ảnh
                  </button>
                </div>
              </div>

              <div v-else class="relative w-full aspect-[21/9] rounded-2xl border-2 border-dashed border-slate-300 dark:border-slate-600 hover:border-indigo-500 dark:hover:border-indigo-400 bg-slate-50 dark:bg-slate-800/50 transition-colors cursor-pointer group flex flex-col items-center justify-center">
                <input type="file" accept="image/*" class="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" @change="handleImageUpload" :disabled="uploadingImage" />
                <div class="w-12 h-12 rounded-full bg-indigo-50 dark:bg-indigo-900/30 text-indigo-500 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <i v-if="uploadingImage" class="fas fa-spinner fa-spin text-xl"></i>
                  <i v-else class="fas fa-cloud-upload-alt text-xl"></i>
                </div>
                <div class="font-bold text-slate-700 dark:text-slate-300">{{ uploadingImage ? 'Đang tải lên...' : 'Bấm để tải ảnh lên' }}</div>
                <div class="text-xs text-slate-400 mt-1">Hỗ trợ JPG, PNG (Max 5MB)</div>
              </div>
            </div>

          </div>
          
          <!-- Footer -->
          <div class="p-6 border-t border-slate-100 dark:border-slate-700/50 bg-slate-50/80 dark:bg-slate-900/50 backdrop-blur-md flex gap-3">
            <button class="flex-1 h-12 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-sm font-bold transition-colors shadow-sm" @click="showProductModal = false">Hủy bỏ</button>
            <button class="flex-[2] h-12 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white rounded-xl text-sm font-bold transition-all shadow-md hover:shadow-lg flex items-center justify-center gap-2" :disabled="pSaving" @click="saveProduct(false)">
              <i v-if="pSaving" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-check"></i>
              {{ pSaving ? 'Đang lưu...' : 'Lưu sản phẩm' }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ── CATEGORY RIGHT PANEL (Glassmorphism + Dark Mode) ── -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showCatModal" @click="showCatModal = false" class="fixed inset-0 bg-slate-900/40 dark:bg-slate-900/70 backdrop-blur-sm z-[100] transition-colors"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showCatModal" class="fixed inset-y-0 right-0 z-[101] w-full sm:w-[400px] bg-white dark:bg-slate-800 shadow-[-15px_0_40px_rgba(0,0,0,0.15)] flex flex-col border-l border-slate-200 dark:border-slate-700/80 transition-colors">
          <!-- Header -->
          <div class="theme-modal-header relative overflow-hidden flex items-center justify-between px-8 py-6 transition-colors duration-500" :class="{ 'easter-egg-space': isSpaceEasterEgg }">
            <template v-if="isSpaceEasterEgg">
              <!-- Space Easter Egg Decor -->
              <div class="absolute inset-0 pointer-events-none">
                <i class="fas fa-rocket absolute top-4 right-32 text-white/80 text-4xl animate-[bounce_3s_infinite] -rotate-45"></i>
                <i class="fas fa-meteor absolute -top-4 right-16 text-orange-400/60 text-6xl rotate-[120deg] drop-shadow-[0_0_15px_rgba(251,146,60,0.8)]"></i>
                <i class="fas fa-user-astronaut absolute bottom-2 right-64 text-white/60 text-3xl animate-[bounce_4s_infinite]"></i>
                <i class="fas fa-star absolute top-2 right-48 text-white/90 text-[8px] animate-pulse"></i>
                <i class="fas fa-star absolute bottom-4 right-20 text-white/70 text-[6px] animate-pulse" style="animation-delay: 1s"></i>
                <i class="fas fa-satellite absolute top-8 right-80 text-white/50 text-2xl animate-[spin_20s_linear_infinite]"></i>
              </div>
            </template>
            <template v-else>
              <!-- Light Mode Decor: Sun & Clouds -->
              <div :key="'light-cat-modal'" class="theme-light-decor absolute inset-0 pointer-events-none transition-all duration-500">
                <i class="fas fa-sun absolute -top-12 right-8 text-yellow-300 text-[140px] opacity-10 animate-[spin_40s_linear_infinite]"></i>
                <i class="fas fa-sun absolute top-3 right-24 text-yellow-300 text-5xl drop-shadow-[0_0_20px_rgba(253,224,71,0.8)] animate-[spin_20s_linear_infinite]"></i>
                <i class="fas fa-cloud absolute top-8 right-44 text-white/50 text-5xl drop-shadow-sm"></i>
                <i class="fas fa-cloud absolute top-2 right-64 text-white/40 text-3xl"></i>
                <i class="fas fa-cloud absolute -bottom-2 right-28 text-white/30 text-7xl"></i>
              </div>

              <!-- Dark Mode Decor: Moon & Stars -->
              <div :key="'dark-cat-modal'" class="theme-dark-decor absolute inset-0 pointer-events-none transition-all duration-500">
                <i class="fas fa-moon absolute -top-8 right-12 text-blue-100 text-[120px] opacity-[0.03] -rotate-12"></i>
                <i class="fas fa-moon absolute top-3 right-24 text-yellow-200 text-4xl drop-shadow-[0_0_15px_rgba(254,240,138,0.5)] -rotate-12"></i>
                <i class="fas fa-star absolute top-4 right-48 text-white/80 text-[8px] animate-pulse"></i>
                <i class="fas fa-star absolute top-8 right-60 text-white/60 text-[10px] animate-pulse" style="animation-delay: 1s"></i>
                <i class="fas fa-star absolute top-3 right-72 text-white/90 text-[6px] animate-pulse" style="animation-delay: 0.5s"></i>
                <i class="fas fa-star absolute bottom-4 right-36 text-white/50 text-[12px] animate-pulse" style="animation-delay: 1.5s"></i>
                <i class="fas fa-star absolute bottom-2 right-56 text-white/70 text-[8px] animate-pulse"></i>
              </div>
            </template>

            <h3 class="font-bold text-white text-lg flex items-center gap-2 relative z-10 drop-shadow-md">
              <i class="fas fa-tags text-white"></i>
              {{ editingCat ? 'Sửa danh mục' : 'Thêm danh mục' }}
            </h3>
            <button @click="showCatModal = false" class="relative z-10 w-9 h-9 flex items-center justify-center rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-all shadow-sm border border-white/10">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- Body -->
          <div class="p-6 flex-1 overflow-y-auto space-y-6 custom-scrollbar bg-white dark:bg-slate-800 transition-colors">
            <div>
              <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Tên danh mục <span class="text-rose-500">*</span></label>
              <input v-model="catForm.name" type="text" placeholder="Nhập tên danh mục" class="w-full h-12 px-4 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-sky-500/30 focus:border-sky-500 outline-none transition-all text-slate-800 dark:text-slate-200 shadow-sm inset-shadow" />
            </div>
            <div>
              <label class="block text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">Mô tả</label>
              <textarea v-model="catForm.description" rows="4" placeholder="Mô tả danh mục..." class="w-full px-4 py-3 border border-slate-200 dark:border-slate-600/80 bg-slate-50 dark:bg-slate-900/50 rounded-xl text-sm focus:ring-2 focus:ring-sky-500/30 focus:border-sky-500 outline-none transition-all resize-none text-slate-800 dark:text-slate-200 shadow-sm"></textarea>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="p-6 border-t border-slate-100 dark:border-slate-700/50 bg-slate-50/80 dark:bg-slate-900/50 backdrop-blur-md flex gap-3">
            <button class="flex-1 h-12 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-sm font-bold transition-colors shadow-sm" @click="showCatModal = false">Hủy bỏ</button>
            <button class="flex-[2] h-12 bg-sky-500 hover:bg-sky-600 dark:bg-sky-600 dark:hover:bg-sky-500 text-white rounded-xl text-sm font-bold transition-all shadow-md hover:shadow-lg flex items-center justify-center gap-2" :disabled="catSaving" @click="saveCat">
              <i v-if="catSaving" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-check"></i>
              {{ catSaving ? 'Đang lưu...' : 'Lưu danh mục' }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Confirm dialogs -->
    <ConfirmDialog :show="showDeleteProduct" title="Xóa sản phẩm" :message="`Bạn có chắc muốn xóa sản phẩm '${deletingProduct?.name}'? Thao tác này không thể hoàn tác.`" confirm-text="Xóa" :danger="true" @confirm="doDeleteProduct" @cancel="showDeleteProduct = false" />
    <ConfirmDialog :show="showDeleteCat" title="Xóa danh mục" :message="`Bạn có chắc muốn xóa danh mục '${deletingCat?.name}'?`" confirm-text="Xóa" :danger="true" @confirm="doDeleteCat" @cancel="showDeleteCat = false" />

    <!-- Import Preview Modal -->
    <AppModal :show="!!importPreviewResult" title="Phân tích File Excel" size="md" @close="cancelExcelImport">
      <div class="p-6">
        <div class="text-slate-700 dark:text-slate-300 font-medium mb-4">Hệ thống đã đọc xong file Excel, thống kê trước khi thực hiện:</div>
        
        <div class="space-y-3 mb-6">
          <div v-if="importPreviewResult?.newCount > 0" class="flex items-center gap-3 p-3 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 rounded-xl border border-blue-100 dark:border-blue-800/50">
            <i class="fas fa-plus-circle text-lg"></i>
            <div>Sẽ <strong>thêm mới</strong>: {{ importPreviewResult.newCount }} sản phẩm.</div>
          </div>
          
          <div v-if="importPreviewResult?.updateCount > 0" class="p-3 bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 rounded-xl border border-amber-100 dark:border-amber-800/50">
            <div class="flex items-center gap-3 mb-2">
              <i class="fas fa-edit text-lg"></i>
              <div>Sẽ <strong>cập nhật</strong>: {{ importPreviewResult.updateCount }} sản phẩm.</div>
            </div>
            <div class="pl-7">
              <ul class="space-y-2 text-sm max-h-[150px] overflow-y-auto custom-scrollbar pr-2">
                <li v-for="(detail, idx) in importPreviewResult.updateDetails" :key="idx" class="border-b border-amber-200/50 dark:border-amber-700/50 last:border-0 pb-1.5 last:pb-0">
                  <div class="font-bold text-amber-900 dark:text-amber-300 mb-0.5">{{ detail.name }}</div>
                  <div class="text-amber-700/90 dark:text-amber-400/90 pl-3 border-l-2 border-amber-300 dark:border-amber-600 ml-1 text-[13px] leading-relaxed">
                    <div v-for="(change, cIdx) in detail.changes" :key="cIdx">• Cập nhật: {{ change }}</div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <div v-if="importPreviewResult?.skippedCount > 0" class="flex items-center gap-3 p-3 bg-slate-50 dark:bg-slate-800/50 text-slate-600 dark:text-slate-400 rounded-xl border border-slate-200 dark:border-slate-700">
            <i class="fas fa-forward text-lg"></i>
            <div><strong>Bỏ qua</strong>: {{ importPreviewResult.skippedCount }} sản phẩm (Dữ liệu không đổi).</div>
          </div>

          <div v-if="importPreviewResult?.errors?.length > 0" class="flex items-start gap-3 p-3 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 rounded-xl border border-red-100 dark:border-red-800/50">
            <i class="fas fa-exclamation-triangle text-lg mt-0.5"></i>
            <div class="flex-1">
              <div class="font-bold mb-1">Cảnh báo: {{ importPreviewResult.errors.length }} dòng bị lỗi sẽ không được nhập:</div>
              <ul class="list-disc pl-5 space-y-1 text-sm max-h-[150px] overflow-y-auto custom-scrollbar">
                <li v-for="(err, idx) in importPreviewResult.errors" :key="idx">{{ err }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700/50">
          <button class="px-5 py-2.5 rounded-xl font-bold text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 transition-colors" @click="cancelExcelImport">
            Hủy bỏ
          </button>
          <button class="bg-indigo-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-sm hover:bg-indigo-700 transition-colors flex items-center gap-2" @click="confirmExcelImport" :disabled="importingExcel">
            <i v-if="importingExcel" class="fas fa-spinner fa-spin"></i>
            {{ importingExcel ? 'Đang xử lý...' : 'Xác nhận Nhập & Cập nhật' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Import Result Modal -->
    <AppModal :show="!!importResult" title="Kết quả Nhập Excel" size="md" @close="importResult = null">
      <div class="p-6">
        <div v-if="importResult?.errors && importResult.errors.length === 0" class="flex flex-col items-center justify-center py-6 text-emerald-500">
          <i class="fas fa-check-circle text-5xl mb-4"></i>
          <h4 class="text-lg font-bold text-slate-800 dark:text-slate-200">Thành công!</h4>
          <p class="text-slate-500 dark:text-slate-400">Đã nhập/cập nhật thành công {{ importResult?.successCount }} sản phẩm.</p>
        </div>
        <div v-else class="space-y-4">
          <div class="flex items-center gap-3 p-4 bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-400 rounded-xl border border-emerald-100 dark:border-emerald-800/50">
            <i class="fas fa-check-circle text-xl"></i>
            <div>
              <div class="font-bold">Đã nhập/cập nhật: {{ importResult?.successCount }} sản phẩm</div>
            </div>
          </div>
          
          <div class="flex items-start gap-3 p-4 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 rounded-xl border border-red-100 dark:border-red-800/50">
            <i class="fas fa-exclamation-triangle text-xl mt-0.5"></i>
            <div class="flex-1">
              <div class="font-bold mb-2">Bỏ qua {{ importResult?.errors.length }} dòng bị lỗi:</div>
              <ul class="list-disc pl-5 space-y-1 text-sm max-h-[200px] overflow-y-auto custom-scrollbar">
                <li v-for="(err, idx) in importResult?.errors" :key="idx">{{ err }}</li>
              </ul>
            </div>
          </div>
        </div>
        
        <div class="mt-6 flex justify-end">
          <button class="bg-indigo-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-sm hover:bg-indigo-700 transition-colors" @click="importResult = null">
            Đóng
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Conflict Dialog removed -->
    </div>
  </div>
</template>

<style scoped>
/* Scoped overrides here if any */
</style>

<style>
</style>
<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 0.8s linear infinite; }

.slide-panel-enter-active, .slide-panel-leave-active {
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-panel-enter-from, .slide-panel-leave-to {
  transform: translateX(100%);
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.4s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

@keyframes spin-very-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes moon-rock {
  0%, 100% { transform: rotate(-15deg); }
  50% { transform: rotate(5deg); }
}
.animate-spin-very-slow { animation: spin-very-slow 60s linear infinite; }
.animate-moon-rock { animation: moon-rock 8s ease-in-out infinite; }
</style>

<style>
/* Dynamic Modal Header Styles */
.theme-modal-header {
  background: linear-gradient(135deg, #38bdf8 0%, #0284c7 100%);
}
.theme-light-decor {
  opacity: 1;
  transform: translateY(0);
}
.theme-dark-decor {
  opacity: 0;
  transform: translateY(20px);
}
html.dark-mode .theme-modal-header {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}
html.dark-mode .theme-modal-header.easter-egg-space {
  background: linear-gradient(135deg, #090a0f 0%, #1b1130 50%, #0c0817 100%) !important;
}
html.dark-mode .theme-light-decor {
  opacity: 0;
  transform: translateY(-20px);
}
html.dark-mode .theme-dark-decor {
  opacity: 1;
  transform: translateY(0);
}
.theme-modal-header.easter-egg-space {
  background: linear-gradient(135deg, #090a0f 0%, #1b1130 50%, #0c0817 100%) !important;
}
</style>
