<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'
import { useToast } from '../utils/toast'

const toast = useToast()
const loading = ref(true)
const inventories = ref<any[]>([])

// Filters
const searchKeyword = ref('')
const selectedBranchId = ref<number | ''>('')

// Fetch global inventories
async function fetchGlobalInventories() {
  loading.value = true
  try {
    const res = await api.get('/api/inventories/global')
    const data = await res.json()
    if (res.ok) {
      inventories.value = data
    } else {
      toast.error(data.message || 'Lỗi khi tải dữ liệu tồn kho.')
    }
  } catch (error) {
    toast.error('Không thể kết nối đến máy chủ.')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchGlobalInventories()
})

// Branches for dropdown (extract from inventories or fetch from API)
// For simplicity, extracting unique branches from fetched inventories
const availableBranches = computed(() => {
  const branchesMap = new Map<number, string>()
  inventories.value.forEach(inv => {
    if (inv.branchId) {
      branchesMap.set(inv.branchId, inv.branchName)
    }
  })
  return Array.from(branchesMap.entries()).map(([id, name]) => ({ id, name }))
})

// Filter inventories based on selected branch and group by product
const groupedInventories = computed(() => {
  let filtered = inventories.value
  
  if (selectedBranchId.value !== '') {
    filtered = filtered.filter(inv => inv.branchId === selectedBranchId.value)
  }
  
  const kw = searchKeyword.value.toLowerCase().trim()
  if (kw) {
    filtered = filtered.filter(inv => 
      (inv.productName && inv.productName.toLowerCase().includes(kw)) ||
      (inv.productCode && inv.productCode.toLowerCase().includes(kw))
    )
  }

  // Group by Product ID
  const groups: Record<number, any> = {}
  
  filtered.forEach(inv => {
    if (!groups[inv.productId]) {
      groups[inv.productId] = {
        productId: inv.productId,
        productName: inv.productName,
        productCode: inv.productCode,
        unit: inv.unit || 'Chiếc',
        price: inv.price || 0,
        totalQuantity: 0,
        branches: [] as any[]
      }
    }
    
    groups[inv.productId].totalQuantity += inv.quantity
    
    // Optional: Include expiry logic
    let branchDetail = inv.branchName
    if (inv.expirationDate) {
       branchDetail += ` (HSD: ${new Date(inv.expirationDate).toLocaleDateString('vi-VN')})`
    }
    
    groups[inv.productId].branches.push({
      branchName: inv.branchName,
      quantity: inv.quantity,
      detail: branchDetail
    })
  })

  return Object.values(groups)
})
</script>

<template>
  <div class="h-full flex flex-col font-['Nunito',sans-serif] bg-[#f8f9fa] animate-fade-in relative z-10 p-4 md:p-6 lg:p-8">
    <div class="max-w-[1400px] w-full mx-auto flex flex-col h-full">
      
      <!-- Header -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <h1 class="text-2xl md:text-3xl font-extrabold text-[#1e293b] tracking-tight mb-1">Tồn kho Hệ thống</h1>
          <p class="text-sm text-[#64748b] font-medium">Tổng quan số lượng sản phẩm trên toàn bộ chi nhánh</p>
        </div>
      </div>

      <!-- Filters -->
      <div class="bg-white rounded-2xl shadow-[0_2px_10px_rgba(0,0,0,0.02)] p-4 md:p-5 mb-6 border border-[#e2e8f0] flex flex-col md:flex-row gap-4 items-center">
        <!-- Search input -->
        <div class="relative w-full md:w-[350px]">
          <i class="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-[#94a3b8]"></i>
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="Tìm theo tên hoặc mã sản phẩm..." 
            class="w-full h-11 pl-11 pr-4 bg-[#f1f5f9] border-transparent rounded-xl text-sm focus:bg-white focus:border-[#4361ee] focus:ring-2 focus:ring-[#4361ee]/20 outline-none transition-all text-[#334155] font-semibold placeholder:font-normal"
          />
        </div>

        <!-- Branch dropdown -->
        <div class="relative w-full md:w-[250px]">
          <select 
            v-model="selectedBranchId"
            class="w-full h-11 pl-4 pr-10 bg-[#f1f5f9] border-transparent rounded-xl text-sm focus:bg-white focus:border-[#4361ee] focus:ring-2 focus:ring-[#4361ee]/20 outline-none transition-all text-[#334155] font-semibold appearance-none"
          >
            <option value="">Tất cả chi nhánh</option>
            <option v-for="b in availableBranches" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select>
          <i class="fas fa-chevron-down absolute right-4 top-1/2 -translate-y-1/2 text-[#94a3b8] pointer-events-none text-xs"></i>
        </div>
        
        <button @click="fetchGlobalInventories" class="ml-auto w-full md:w-auto h-11 px-5 bg-white border border-[#e2e8f0] text-[#64748b] rounded-xl hover:bg-[#f8f9fa] hover:text-[#4361ee] hover:border-[#4361ee]/30 transition-all text-sm font-bold flex items-center justify-center gap-2">
          <i class="fas fa-sync-alt" :class="{'fa-spin': loading}"></i> Làm mới
        </button>
      </div>

      <!-- Main Content / Table -->
      <div class="flex-1 bg-white rounded-2xl shadow-[0_5px_20px_rgba(0,0,0,0.03)] border border-[#e2e8f0] overflow-hidden flex flex-col relative z-20">
        
        <!-- Loading overlay -->
        <div v-if="loading && groupedInventories.length === 0" class="absolute inset-0 z-10 bg-white/60 backdrop-blur-sm flex flex-col items-center justify-center">
          <div class="w-12 h-12 border-4 border-[#4361ee]/20 border-t-[#4361ee] rounded-full animate-spin mb-3"></div>
          <p class="text-[#64748b] font-semibold animate-pulse">Đang tải dữ liệu...</p>
        </div>

        <div class="overflow-x-auto flex-1 custom-scrollbar">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-[#f8f9fa] border-b border-[#e2e8f0]">
                <th class="py-4 px-5 text-xs font-extrabold text-[#64748b] uppercase tracking-wider sticky top-0 bg-[#f8f9fa] z-10">Mã SKU</th>
                <th class="py-4 px-5 text-xs font-extrabold text-[#64748b] uppercase tracking-wider sticky top-0 bg-[#f8f9fa] z-10">Sản phẩm</th>
                <th class="py-4 px-5 text-center text-xs font-extrabold text-[#64748b] uppercase tracking-wider sticky top-0 bg-[#f8f9fa] z-10">Tổng tồn kho</th>
                <th class="py-4 px-5 text-xs font-extrabold text-[#64748b] uppercase tracking-wider sticky top-0 bg-[#f8f9fa] z-10">Phân bổ tại Chi nhánh</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[#e2e8f0]">
              <tr v-if="!loading && groupedInventories.length === 0">
                <td colspan="4" class="py-12 text-center">
                  <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-[#f1f5f9] text-[#94a3b8] mb-4">
                    <i class="fas fa-box-open text-2xl"></i>
                  </div>
                  <h3 class="text-lg font-bold text-[#1e293b] mb-1">Không tìm thấy dữ liệu</h3>
                  <p class="text-[#64748b]">Thử điều chỉnh bộ lọc để xem kết quả.</p>
                </td>
              </tr>
              
              <tr v-for="item in groupedInventories" :key="item.productId" class="hover:bg-[#f8f9fa]/50 transition-colors group">
                <td class="py-4 px-5 align-top">
                  <span class="inline-block px-2.5 py-1 bg-[#f1f5f9] text-[#475569] font-mono text-xs font-bold rounded-lg border border-[#e2e8f0]">{{ item.productCode }}</span>
                </td>
                <td class="py-4 px-5 align-top">
                  <div class="font-bold text-[#1e293b] text-sm mb-0.5">{{ item.productName }}</div>
                  <div class="text-xs text-[#94a3b8] font-medium">Đơn vị: {{ item.unit }}</div>
                </td>
                <td class="py-4 px-5 align-top text-center">
                  <div class="text-lg font-extrabold" :class="item.totalQuantity > 0 ? 'text-[#10b981]' : 'text-[#ea4f52]'">
                    {{ item.totalQuantity }}
                  </div>
                </td>
                <td class="py-4 px-5 align-top">
                  <div class="flex flex-wrap gap-2">
                    <div 
                      v-for="(b, idx) in item.branches" 
                      :key="idx" 
                      class="px-3 py-1.5 rounded-lg border text-xs font-bold flex items-center gap-2"
                      :class="b.quantity > 0 ? 'bg-blue-50 border-blue-100 text-blue-700' : 'bg-red-50 border-red-100 text-red-600'"
                    >
                      <span>{{ b.detail }}</span>
                      <span class="px-1.5 py-0.5 rounded bg-white/60 tabular-nums">{{ b.quantity }}</span>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { height: 8px; width: 8px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background-color: #cbd5e1; border-radius: 20px; }
.custom-scrollbar:hover::-webkit-scrollbar-thumb { background-color: #94a3b8; }
</style>
