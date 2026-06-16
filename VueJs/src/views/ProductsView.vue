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
  <div class="space-y-4">
    <!-- Tabs -->
    <div class="flex items-center gap-1 bg-white rounded-xl p-1 border border-slate-100 shadow-sm w-fit">
      <button
        v-for="tab in [{ key: 'products', label: 'Sản phẩm', icon: 'inventory_2' }, { key: 'categories', label: 'Danh mục', icon: 'category' }]"
        :key="tab.key"
        :class="['flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-medium transition-all duration-150', activeTab === tab.key ? 'bg-[#0052cc] text-white shadow' : 'text-slate-500 hover:text-slate-700']"
        @click="activeTab = tab.key as any"
      >
        <span class="material-symbols-outlined text-base">{{ tab.icon }}</span>
        {{ tab.label }}
      </button>
    </div>

    <!-- ── PRODUCTS TAB ── -->
    <div v-if="activeTab === 'products'" class="space-y-4">
      <!-- Toolbar -->
      <div class="flex items-center gap-3 flex-wrap">
        <div class="relative flex-1 min-w-48">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-lg">search</span>
          <input
            v-model="pSearch"
            type="text"
            placeholder="Tìm theo tên, SKU..."
            class="w-full h-10 pl-10 pr-4 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 bg-white"
          />
        </div>
        <select
          v-model="pCategoryId"
          class="h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white text-slate-700 min-w-40"
        >
          <option value="">Tất cả danh mục</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <button
          v-if="isAdmin"
          id="add-product-btn"
          class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm"
          @click="openAddProduct"
        >
          <span class="material-symbols-outlined text-base">add</span>
          Thêm sản phẩm
        </button>
      </div>

      <!-- Table -->
      <div class="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div v-if="pLoading" class="p-8 space-y-3">
          <div v-for="i in 5" :key="i" class="h-12 bg-slate-50 rounded-xl animate-pulse" />
        </div>
        <div v-else-if="filteredProducts.length === 0" class="py-20 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl block mb-3 opacity-40">inventory_2</span>
          <div class="font-medium">Không có sản phẩm nào</div>
          <div class="text-sm mt-1">Thêm sản phẩm mới để bắt đầu</div>
        </div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-100 bg-slate-50">
              <th class="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Sản phẩm</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Danh mục</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Số lượng</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Đơn giá</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Hạn sử dụng</th>
              <th v-if="isAdmin" class="px-6 py-3" />
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="p in filteredProducts"
              :key="p.id"
              class="border-b border-slate-50 hover:bg-slate-50/80 transition-colors"
            >
              <td class="px-6 py-4">
                <div class="font-semibold text-slate-800">{{ p.name }}</div>
                <div class="text-xs text-slate-400 font-mono mt-0.5">{{ p.sku }}</div>
              </td>
              <td class="px-4 py-4 text-slate-600">
                <span v-if="p.categoryName" class="px-2.5 py-1 bg-blue-50 text-blue-600 rounded-md text-xs font-medium">{{ p.categoryName }}</span>
                <span v-else class="text-slate-400">—</span>
              </td>
              <td class="px-4 py-4 text-right font-mono text-slate-700 font-semibold">{{ p.quantity || 0 }}</td>
              <td class="px-4 py-4 text-right font-mono font-bold text-[#0052cc]">{{ formatCurrency(p.price) }}</td>
              <td class="px-4 py-4 text-right font-mono text-slate-500 text-xs">{{ formatDate(p.expirationDate) }}</td>
              <td v-if="isAdmin" class="px-6 py-4">
                <div class="flex items-center justify-end gap-1">
                  <button class="w-8 h-8 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 flex items-center justify-center transition-colors" @click="openEditProduct(p)">
                    <span class="material-symbols-outlined text-base">edit</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors" @click="confirmDeleteProduct(p)">
                    <span class="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!pLoading && filteredProducts.length > 0" class="px-6 py-3 border-t border-slate-100 text-xs text-slate-400">
          {{ filteredProducts.length }} sản phẩm
        </div>
      </div>
    </div>

    <!-- ── CATEGORIES TAB ── -->
    <div v-if="activeTab === 'categories'" class="space-y-4">
      <div class="flex justify-end">
        <button
          v-if="isManager"
          id="add-category-btn"
          class="h-10 px-4 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-medium flex items-center gap-2 transition-colors shadow-sm"
          @click="openAddCat"
        >
          <span class="material-symbols-outlined text-base">add</span>
          Thêm danh mục
        </button>
      </div>
      <div class="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div v-if="cLoading" class="p-8 space-y-3">
          <div v-for="i in 4" :key="i" class="h-14 bg-slate-50 rounded-xl animate-pulse" />
        </div>
        <div v-else-if="categories.length === 0" class="py-16 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl block mb-3 opacity-40">category</span>
          Chưa có danh mục nào
        </div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-100 bg-slate-50">
              <th class="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Tên danh mục</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Mô tả</th>
              <th v-if="isManager" class="px-6 py-3" />
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categories" :key="c.id" class="border-b border-slate-50 hover:bg-slate-50/80 transition-colors">
              <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                  <div class="w-7 h-7 rounded-lg bg-blue-50 flex items-center justify-center">
                    <span class="material-symbols-outlined text-blue-500 text-sm">category</span>
                  </div>
                  <span class="font-medium text-slate-800">{{ c.name }}</span>
                </div>
              </td>
              <td class="px-4 py-4 text-slate-500">{{ c.description || '—' }}</td>
              <td v-if="isManager" class="px-6 py-4">
                <div class="flex items-center justify-end gap-1">
                  <button class="w-8 h-8 rounded-lg hover:bg-blue-50 text-slate-400 hover:text-blue-600 flex items-center justify-center transition-colors" @click="openEditCat(c)">
                    <span class="material-symbols-outlined text-base">edit</span>
                  </button>
                  <button class="w-8 h-8 rounded-lg hover:bg-red-50 text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors" @click="confirmDeleteCat(c)">
                    <span class="material-symbols-outlined text-base">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── PRODUCT MODAL ── -->
    <AppModal :show="showProductModal" :title="editingProduct ? 'Sửa sản phẩm' : 'Thêm sản phẩm mới'" @close="showProductModal = false">
      <div class="p-6 space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2 sm:col-span-1">
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Tên sản phẩm <span class="text-red-500">*</span></label>
            <input v-model="productForm.name" type="text" placeholder="Nhập tên sản phẩm" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          </div>
          <div class="col-span-2 sm:col-span-1">
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Danh mục</label>
            <select v-model="productForm.categoryId" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] bg-white">
              <option value="">-- Không có --</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Đơn giá (VNĐ) <span class="text-red-500">*</span></label>
            <input v-model="productForm.price" type="number" min="0" placeholder="0" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Số lượng <span class="text-red-500">*</span></label>
            <input v-model="productForm.quantity" type="number" min="0" placeholder="0" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Ngày sản xuất</label>
            <input v-model="productForm.manufacturingDate" type="date" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 text-slate-700" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-600 mb-1.5">Hạn sử dụng</label>
            <input v-model="productForm.expirationDate" type="date" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 text-slate-700" />
          </div>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Mô tả</label>
          <textarea v-model="productForm.description" rows="2" placeholder="Mô tả sản phẩm..." class="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 resize-none" />
        </div>
        <div class="flex gap-3 pt-2">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50 transition-colors" @click="showProductModal = false">Hủy</button>
          <button
            class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold transition-colors flex items-center justify-center gap-2"
            :disabled="pSaving" @click="saveProduct"
          >
            <span v-if="pSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ pSaving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </AppModal>

    <!-- ── CATEGORY MODAL ── -->
    <AppModal :show="showCatModal" :title="editingCat ? 'Sửa danh mục' : 'Thêm danh mục'" size="sm" @close="showCatModal = false">
      <div class="p-6 space-y-4">
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Tên danh mục <span class="text-red-500">*</span></label>
          <input v-model="catForm.name" type="text" placeholder="Nhập tên danh mục" class="w-full h-10 px-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10" />
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 mb-1.5">Mô tả</label>
          <textarea v-model="catForm.description" rows="2" placeholder="Mô tả..." class="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none focus:border-[#0052cc] focus:ring-2 focus:ring-[#0052cc]/10 resize-none" />
        </div>
        <div class="flex gap-3">
          <button class="flex-1 h-10 border border-slate-200 rounded-xl text-sm text-slate-600 hover:bg-slate-50 transition-colors" @click="showCatModal = false">Hủy</button>
          <button class="flex-1 h-10 bg-[#0052cc] hover:bg-[#003d9b] text-white rounded-xl text-sm font-semibold transition-colors flex items-center justify-center gap-2" :disabled="catSaving" @click="saveCat">
            <span v-if="catSaving" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            {{ catSaving ? 'Đang lưu...' : 'Lưu' }}
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
</style>
