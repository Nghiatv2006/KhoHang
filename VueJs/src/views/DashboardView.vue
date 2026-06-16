<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { api } from '../api'

const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')

interface StatCard { label: string; value: string | number; icon: string; color: string; bg: string }

const stats = ref<StatCard[]>([
  { label: 'Tổng sản phẩm', value: '—', icon: 'inventory_2', color: 'text-blue-600', bg: 'bg-blue-50' },
  { label: 'Tổng chi nhánh', value: '—', icon: 'store', color: 'text-emerald-600', bg: 'bg-emerald-50' },
  { label: 'Nhà cung cấp', value: '—', icon: 'local_shipping', color: 'text-violet-600', bg: 'bg-violet-50' },
  { label: 'Người dùng', value: '—', icon: 'group', color: 'text-amber-600', bg: 'bg-amber-50' },
])

const products = ref<any[]>([])
const branches = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [pRes, bRes, sRes, uRes] = await Promise.allSettled([
      api.get('/api/products'),
      api.get('/api/branches'),
      api.get('/api/suppliers'),
      api.get('/api/users'),
    ])

    if (pRes.status === 'fulfilled' && pRes.value.ok) {
      const data = await pRes.value.json()
      products.value = data.slice(0, 6)
      stats.value[0].value = data.length
    }
    if (bRes.status === 'fulfilled' && bRes.value.ok) {
      const data = await bRes.value.json()
      branches.value = data
      stats.value[1].value = data.length
    }
    if (sRes.status === 'fulfilled' && sRes.value.ok) {
      const data = await sRes.value.json()
      stats.value[2].value = data.length
    }
    if (uRes.status === 'fulfilled' && uRes.value.ok) {
      const data = await uRes.value.json()
      stats.value[3].value = data.length
    }
  } finally {
    loading.value = false
  }
})

function formatCurrency(val: any) {
  if (!val) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val)
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return 'Chào buổi sáng'
  if (h < 18) return 'Chào buổi chiều'
  return 'Chào buổi tối'
})
</script>

<template>
  <div class="space-y-6">
    <!-- Greeting -->
    <div class="bg-gradient-to-r from-[#003d9b] to-[#0052cc] rounded-2xl p-6 text-white shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-blue-200 text-sm mb-1">{{ greeting }},</p>
          <h2 class="text-2xl font-bold">{{ user?.fullName || 'Người dùng' }} 👋</h2>
          <p class="text-blue-200 text-sm mt-1.5 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-base">business</span>
            {{ user?.branchName || 'Tất cả chi nhánh' }} &mdash; {{ user?.role }}
          </p>
        </div>
        <div class="w-16 h-16 rounded-2xl bg-white/10 flex items-center justify-center">
          <span class="material-symbols-outlined text-3xl" style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 48">warehouse</span>
        </div>
      </div>
    </div>

    <!-- Stats cards -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div
        v-for="(stat, i) in stats"
        :key="i"
        class="bg-white rounded-2xl p-5 shadow-sm border border-slate-100 card-hover"
      >
        <div class="flex items-start justify-between mb-3">
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center', stat.bg]">
            <span :class="['material-symbols-outlined text-xl', stat.color]"
              style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">{{ stat.icon }}</span>
          </div>
        </div>
        <div v-if="loading" class="h-8 w-16 bg-slate-100 rounded animate-pulse" />
        <div v-else class="text-2xl font-bold text-slate-800">{{ stat.value }}</div>
        <div class="text-slate-500 text-sm mt-0.5">{{ stat.label }}</div>
      </div>
    </div>

    <!-- Products + Branches grid -->
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-4">
      <!-- Recent Products -->
      <div class="lg:col-span-3 bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div class="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h3 class="font-semibold text-slate-800 flex items-center gap-2">
            <span class="material-symbols-outlined text-blue-500 text-lg"
              style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">inventory_2</span>
            Sản phẩm gần đây
          </h3>
          <RouterLink to="/products" class="text-xs text-[#0052cc] hover:underline font-medium">Xem tất cả →</RouterLink>
        </div>
        <div v-if="loading" class="p-6 space-y-3">
          <div v-for="i in 4" :key="i" class="h-10 bg-slate-50 rounded-lg animate-pulse" />
        </div>
        <div v-else-if="products.length === 0" class="p-10 text-center text-slate-400">
          <span class="material-symbols-outlined text-4xl block mb-2">inventory_2</span>
          Chưa có sản phẩm nào
        </div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-100 bg-slate-50">
              <th class="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Tên sản phẩm</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Danh mục</th>
              <th class="text-right px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Giá xuất</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="p in products"
              :key="p.id"
              class="border-b border-slate-50 hover:bg-slate-50 transition-colors"
            >
              <td class="px-6 py-3.5">
                <div class="font-medium text-slate-800">{{ p.name }}</div>
                <div class="text-xs text-slate-400 font-mono">{{ p.sku }}</div>
              </td>
              <td class="px-4 py-3.5 text-slate-600">{{ p.categoryName || '—' }}</td>
              <td class="px-6 py-3.5 text-right font-mono text-slate-700 font-medium">{{ formatCurrency(p.salePrice) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Branches -->
      <div class="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div class="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 class="font-semibold text-slate-800 flex items-center gap-2">
            <span class="material-symbols-outlined text-emerald-500 text-lg"
              style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">store</span>
            Chi nhánh
          </h3>
          <RouterLink v-if="isAdmin" to="/branches" class="text-xs text-[#0052cc] hover:underline font-medium">Quản lý →</RouterLink>
        </div>
        <div v-if="loading" class="p-4 space-y-2">
          <div v-for="i in 3" :key="i" class="h-16 bg-slate-50 rounded-xl animate-pulse" />
        </div>
        <div v-else-if="branches.length === 0" class="p-8 text-center text-slate-400 text-sm">
          Chưa có chi nhánh
        </div>
        <div v-else class="p-3 space-y-2">
          <div
            v-for="b in branches"
            :key="b.id"
            class="flex items-start gap-3 p-3 rounded-xl hover:bg-slate-50 transition-colors"
          >
            <div class="w-9 h-9 rounded-xl bg-emerald-50 flex items-center justify-center flex-shrink-0">
              <span class="material-symbols-outlined text-emerald-500 text-lg"
                style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24">store</span>
            </div>
            <div class="min-w-0">
              <div class="font-medium text-slate-800 text-sm truncate">{{ b.name }}</div>
              <div class="text-xs text-slate-400 truncate mt-0.5">{{ b.address }}</div>
              <div class="text-xs text-amber-600 mt-0.5">
                Ngưỡng tồn kho: <span class="font-semibold">{{ b.lowStockThreshold }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
