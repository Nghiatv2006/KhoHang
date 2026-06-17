<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'
import AppModal from '../components/AppModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const toast = useToast()
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

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
const editingProduct = ref<any>(null)
const productForm = reactive({
  name: '', categoryId: '' as number | '',
  price: '' as any, quantity: '' as any, description: '',
  manufacturingDate: '', expirationDate: ''
})
const pSaving = ref(false)

function openAddProduct() {
  editingProduct.value = null
  Object.assign(productForm, { name: '', categoryId: '', price: '', quantity: '', description: '', manufacturingDate: '', expirationDate: '' })
  showProductModal.value = true
}
function openEditProduct(p: any) {
  editingProduct.value = p
  Object.assign(productForm, {
    name: p.name, categoryId: p.categoryId || '',
    price: p.price, quantity: p.quantity, description: p.description || '',
    manufacturingDate: p.manufacturingDate || '', expirationDate: p.expirationDate || ''
  })
  showProductModal.value = true
}
async function saveProduct() {
  if (!productForm.name?.trim()) {
    toast.error('Tên sản phẩm là bắt buộc.')
    return
  }
  pSaving.value = true
  try {
    const payload = {
      name: productForm.name.trim(),
      categoryId: productForm.categoryId || null,
      price: productForm.price || 0,
      quantity: productForm.quantity || 0,
      description: productForm.description,
      manufacturingDate: productForm.manufacturingDate || null,
      expirationDate: productForm.expirationDate || null
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

// ─── Categories ─────────────────────────────────────────────────────────────
const cLoading = ref(false)
const showCatModal = ref(false)
const editingCat = ref<any>(null)
const catForm = reactive({ name: '', description: '' })
const catSaving = ref(false)
const showDeleteCat = ref(false)
const deletingCat = ref<any>(null)

function openAddCat() { editingCat.value = null; Object.assign(catForm, { name: '', description: '' }); showCatModal.value = true }
function openEditCat(c: any) { editingCat.value = c; Object.assign(catForm, { name: c.name, description: c.description || '' }); showCatModal.value = true }
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
    if (res.ok) products.value = await res.json()
  } catch {} finally { pLoading.value = false }
}
async function loadCategories() {
  cLoading.value = true
  try {
    const res = await api.get('/api/categories')
    if (res.ok) categories.value = await res.json()
  } catch {} finally { cLoading.value = false }
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadCategories()])
})

function formatCurrency(val: any) {
  if (!val) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val)
}

function formatDate(val: any) {
  if (!val) return '—'
  return new Date(val).toLocaleDateString('vi-VN')
}
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto">
    <!-- Header & Tabs -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-2">
      <div>
        <h2 class="text-2xl font-bold text-[#364a63] m-0">Quản lý Sản phẩm</h2>
        <p class="text-[#8094ae] text-sm mt-1">Quản lý danh mục và danh sách sản phẩm trong kho</p>
      </div>
      
      <!-- Tabs -->
      <div class="flex items-center gap-6 border-b border-[#e2e8f0]">
        <button
          v-for="tab in [{ key: 'products', label: 'Sản phẩm', icon: 'fas fa-box' }, { key: 'categories', label: 'Danh mục', icon: 'fas fa-tags' }]"
          :key="tab.key"
          :class="[
            'flex items-center gap-2 pb-3 px-1 text-sm font-bold transition-colors relative',
            activeTab === tab.key ? 'text-[#4361ee]' : 'text-[#8094ae] hover:text-[#364a63]'
          ]"
          @click="activeTab = tab.key as any"
        >
          <i :class="tab.icon"></i>
          {{ tab.label }}
          <div v-if="activeTab === tab.key" class="absolute bottom-[-1px] left-0 w-full h-[2px] bg-[#4361ee] rounded-t-full"></div>
        </button>
      </div>
    </div>

    <!-- ── PRODUCTS TAB ── -->
    <div v-if="activeTab === 'products'" class="bg-indigo-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] flex items-center justify-between flex-wrap gap-4 bg-[#f8f9fa]/50">
        <div class="flex items-center gap-3 flex-1 min-w-[300px]">
          <div class="relative w-[300px]">
            <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#8094ae]"></i>
            <input
              v-model="pSearch"
              type="text"
              placeholder="Tìm theo tên, SKU..."
              class="w-full h-[42px] pl-11 pr-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]"
            />
          </div>
          <select
            v-model="pCategoryId"
            class="h-[42px] px-4 border border-[#e2e8f0] bg-white rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63] cursor-pointer"
          >
            <option value="">Tất cả danh mục</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <button
          v-if="isAdmin"
          class="bg-[#4361ee] text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2"
          @click="openAddProduct"
        >
          <i class="fas fa-plus"></i>
          Thêm sản phẩm
        </button>
      </div>

      <!-- Table -->
      <div class="overflow-x-auto">
        <div v-if="pLoading" class="p-8 space-y-4">
          <div v-for="i in 5" :key="i" class="h-12 bg-[#f8f9fa] rounded-xl animate-pulse" />
        </div>
        <div v-else-if="filteredProducts.length === 0" class="py-20 text-center text-[#8094ae]">
          <i class="fas fa-box-open text-5xl mb-4 opacity-40"></i>
          <div class="font-bold text-[#364a63]">Không có sản phẩm nào</div>
          <div class="text-sm mt-1">Thêm sản phẩm mới để bắt đầu quản lý</div>
        </div>
        <table v-else class="w-full text-left border-collapse">
          <thead class="bg-white">
            <tr>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Sản phẩm</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Danh mục</th>
              <th class="p-4 text-right text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Số lượng</th>
              <th class="p-4 text-right text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Đơn giá</th>
              <th class="p-4 text-right text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Hạn sử dụng</th>
              <th v-if="isAdmin" class="p-4 border-b border-[#f1f5f9] w-[100px]"></th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="p in filteredProducts"
              :key="p.id"
              class="border-b border-[#f1f5f9] hover:border-transparent hover:bg-gradient-to-r hover:from-[#4361ee]/15 hover:to-[#4cc9f0]/15 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]"
              @dblclick="isAdmin ? openEditProduct(p) : null"
            >
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="font-bold text-[#364a63]">{{ p.name }}</div>
                <div class="text-xs text-[#8094ae] font-mono mt-0.5">{{ p.sku }}</div>
              </td>
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <span v-if="p.categoryName" class="px-3 py-1 bg-[#eef2ff] text-[#4361ee] rounded-full text-xs font-bold">{{ p.categoryName }}</span>
                <span v-else class="text-[#8094ae]">—</span>
              </td>
              <td class="p-4 text-right font-mono font-medium first:rounded-l-xl last:rounded-r-xl">
                <span :class="p.quantity > 0 ? 'text-[#05b171]' : 'text-[#ea4f52]'">{{ p.quantity || 0 }}</span>
              </td>
              <td class="p-4 text-right font-mono font-bold text-[#364a63] first:rounded-l-xl last:rounded-r-xl">{{ formatCurrency(p.price) }}</td>
              <td class="p-4 text-right font-mono text-[#8094ae] text-xs first:rounded-l-xl last:rounded-r-xl">{{ formatDate(p.expirationDate) }}</td>
              <td v-if="isAdmin" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                  <button class="w-8 h-8 rounded-lg text-[#0ea5e9] bg-white hover:bg-[#e0f2fe] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="openEditProduct(p)" title="Sửa">
                    <i class="fas fa-pen text-sm"></i>
                  </button>
                  <button class="w-8 h-8 rounded-lg text-[#ea4f52] bg-white hover:bg-[#ffe4e6] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="confirmDeleteProduct(p)" title="Xóa">
                    <i class="fas fa-trash text-sm"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!pLoading && filteredProducts.length > 0" class="px-6 py-4 bg-[#f8f9fa] border-t border-[#f1f5f9] text-xs font-bold text-[#8094ae]">
          Tổng cộng: {{ filteredProducts.length }} sản phẩm
        </div>
      </div>
    </div>

    <!-- ── CATEGORIES TAB ── -->
    <div v-if="activeTab === 'categories'" class="bg-sky-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#0ea5e9] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden">
      <!-- Toolbar -->
      <div class="p-5 border-b border-[#f1f5f9] flex justify-between items-center bg-[#f8f9fa]/50">
        <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-tags text-[#4361ee] mr-2"></i>Danh sách danh mục</h6>
        <button
          v-if="isManager"
          class="bg-[#4361ee] text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all text-sm flex items-center gap-2"
          @click="openAddCat"
        >
          <i class="fas fa-plus"></i>
          Thêm danh mục
        </button>
      </div>

      <!-- Table -->
      <div class="overflow-x-auto">
        <div v-if="cLoading" class="p-8 space-y-4">
          <div v-for="i in 4" :key="i" class="h-12 bg-[#f8f9fa] rounded-xl animate-pulse" />
        </div>
        <div v-else-if="categories.length === 0" class="py-20 text-center text-[#8094ae]">
          <i class="fas fa-layer-group text-5xl mb-4 opacity-40"></i>
          <div class="font-bold text-[#364a63]">Chưa có danh mục nào</div>
        </div>
        <table v-else class="w-full text-left border-collapse">
          <thead class="bg-white">
            <tr>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Tên danh mục</th>
              <th class="p-4 text-[0.75rem] uppercase font-bold text-[#8094ae] tracking-wider border-b border-[#f1f5f9]">Mô tả</th>
              <th v-if="isManager" class="p-4 border-b border-[#f1f5f9] w-[100px]"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categories" :key="c.id" class="border-b border-[#f1f5f9] hover:border-transparent hover:bg-gradient-to-r hover:from-[#4361ee]/15 hover:to-[#4cc9f0]/15 hover:shadow-sm transition-all duration-300 cursor-pointer group hover:-translate-y-[1px]" @dblclick="isAdmin ? openEditCat(c) : null">
              <td class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-lg bg-[#eef2ff] flex items-center justify-center">
                    <i class="fas fa-tag text-[#4361ee] text-sm"></i>
                  </div>
                  <span class="font-bold text-[#364a63]">{{ c.name }}</span>
                </div>
              </td>
              <td class="p-4 text-sm text-[#8094ae] first:rounded-l-xl last:rounded-r-xl">{{ c.description || '—' }}</td>
              <td v-if="isManager" class="p-4 first:rounded-l-xl last:rounded-r-xl">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                  <button class="w-8 h-8 rounded-lg text-[#0ea5e9] bg-white hover:bg-[#e0f2fe] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="openEditCat(c)" title="Sửa">
                    <i class="fas fa-pen text-sm"></i>
                  </button>
                  <button class="w-8 h-8 rounded-lg text-[#ea4f52] bg-white hover:bg-[#ffe4e6] flex items-center justify-center transition-colors cursor-pointer shadow-sm border border-[#e2e8f0]/50" @click.stop="confirmDeleteCat(c)" title="Xóa">
                    <i class="fas fa-trash text-sm"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── PRODUCT RIGHT PANEL ── -->
    <Teleport to="body">
      <!-- Backdrop -->
      <Transition name="fade">
        <div v-if="showProductModal" @click="showProductModal = false" class="fixed inset-0 bg-slate-900/20 backdrop-blur-[2px] z-[100]"></div>
      </Transition>

      <!-- Panel -->
      <Transition name="slide-panel">
        <div v-if="showProductModal" class="fixed inset-y-0 right-0 z-[101] w-[450px] bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] flex flex-col border-l border-[#e2e8f0]">
          <!-- Header -->
          <div class="px-6 py-5 border-b border-[#f1f5f9] flex justify-between items-center bg-gradient-to-r from-[#f8fafc] to-white">
            <h3 class="font-bold text-[#364a63] text-lg flex items-center gap-2">
              <i class="fas fa-box text-[#4361ee]"></i>
              {{ editingProduct ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới' }}
            </h3>
            <button @click="showProductModal = false" class="text-[#8094ae] hover:text-[#ea4f52] transition-colors w-8 h-8 flex items-center justify-center rounded-full hover:bg-red-50">
              <i class="fas fa-times"></i>
            </button>
          </div>
          
          <!-- Body -->
          <div class="p-6 flex-1 overflow-y-auto space-y-5 custom-scrollbar">
            <div class="grid grid-cols-2 gap-5">
              <div class="col-span-2">
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Tên sản phẩm <span class="text-[#ea4f52]">*</span></label>
                <input v-model="productForm.name" type="text" placeholder="Nhập tên sản phẩm" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
              </div>
              <div class="col-span-2">
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Danh mục</label>
                <select v-model="productForm.categoryId" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]">
                  <option value="">-- Không phân loại --</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Đơn giá (VNĐ) <span class="text-[#ea4f52]">*</span></label>
                <input v-model="productForm.price" type="number" min="0" placeholder="0" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Số lượng <span class="text-[#ea4f52]">*</span></label>
                <input v-model="productForm.quantity" type="number" min="0" placeholder="0" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Ngày sản xuất</label>
                <input v-model="productForm.manufacturingDate" type="date" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
              </div>
              <div>
                <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Hạn sử dụng</label>
                <input v-model="productForm.expirationDate" type="date" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
              </div>
            </div>
            <div>
              <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mô tả chi tiết</label>
              <textarea v-model="productForm.description" rows="4" placeholder="Mô tả sản phẩm..." class="w-full px-4 py-3 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all resize-none text-[#364a63]"></textarea>
            </div>
          </div>
          
          <!-- Footer -->
          <div class="p-6 border-t border-[#f1f5f9] bg-[#f8fafc] flex gap-3">
            <button class="flex-1 h-11 bg-white border border-[#e2e8f0] hover:bg-[#f8f9fa] text-[#364a63] rounded-xl text-sm font-bold transition-colors shadow-sm" @click="showProductModal = false">Hủy bỏ</button>
            <button class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-2" :disabled="pSaving" @click="saveProduct">
              <i v-if="pSaving" class="fas fa-spinner fa-spin"></i>
              {{ pSaving ? 'Đang lưu...' : 'Lưu sản phẩm' }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ── CATEGORY MODAL ── -->
    <AppModal :show="showCatModal" :title="editingCat ? 'Sửa danh mục' : 'Thêm danh mục'" size="sm" @close="showCatModal = false">
      <div class="p-6 space-y-5">
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Tên danh mục <span class="text-[#ea4f52]">*</span></label>
          <input v-model="catForm.name" type="text" placeholder="Nhập tên danh mục" class="w-full h-11 px-4 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all text-[#364a63]" />
        </div>
        <div>
          <label class="block text-xs font-bold text-[#8094ae] uppercase tracking-wider mb-2">Mô tả</label>
          <textarea v-model="catForm.description" rows="3" placeholder="Mô tả..." class="w-full px-4 py-3 border border-[#e2e8f0] bg-[#f8f9fa] rounded-xl text-sm focus:ring-2 focus:ring-[#4361ee]/20 focus:border-[#4361ee] outline-none transition-all resize-none text-[#364a63]"></textarea>
        </div>
        <div class="flex gap-3 pt-4 border-t border-[#f1f5f9]">
          <button class="flex-1 h-11 bg-[#f8f9fa] hover:bg-[#e2e8f0] text-[#364a63] rounded-xl text-sm font-bold transition-colors" @click="showCatModal = false">Hủy bỏ</button>
          <button class="flex-1 h-11 bg-[#4361ee] hover:bg-[#3a0ca3] text-white rounded-xl text-sm font-bold transition-all shadow-sm hover:shadow-md flex items-center justify-center gap-2" :disabled="catSaving" @click="saveCat">
            <i v-if="catSaving" class="fas fa-spinner fa-spin"></i>
            {{ catSaving ? 'Đang lưu...' : 'Lưu danh mục' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- Confirm dialogs -->
    <ConfirmDialog :show="showDeleteProduct" title="Xóa sản phẩm" :message="`Bạn có chắc muốn xóa sản phẩm '${deletingProduct?.name}'? Thao tác này không thể hoàn tác.`" confirm-text="Xóa" :danger="true" @confirm="doDeleteProduct" @cancel="showDeleteProduct = false" />
    <ConfirmDialog :show="showDeleteCat" title="Xóa danh mục" :message="`Bạn có chắc muốn xóa danh mục '${deletingCat?.name}'?`" confirm-text="Xóa" :danger="true" @confirm="doDeleteCat" @cancel="showDeleteCat = false" />
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
