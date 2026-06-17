<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import * as echarts from 'echarts'

const router = useRouter()
const products = ref<any[]>([])
const categories = ref<any[]>([])
const customers = ref<any[]>([])
const suppliers = ref<any[]>([])
const branches = ref<any[]>([])

const loading = ref(true)
const errorMsg = ref('')

const pieChartRef = ref<HTMLElement | null>(null)
const debtChartRef = ref<HTMLElement | null>(null)
const qtyChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)

let pieChartInst: echarts.ECharts | null = null
let debtChartInst: echarts.ECharts | null = null
let qtyChartInst: echarts.ECharts | null = null
let barChartInst: echarts.ECharts | null = null

onMounted(async () => {
  try {
    const [pRes, cRes, cuRes, sRes, bRes] = await Promise.allSettled([
      api.get('/api/products'),
      api.get('/api/categories'),
      api.get('/api/customers'),
      api.get('/api/suppliers'),
      api.get('/api/branches'),
    ])
    
    if (pRes.status === 'fulfilled' && pRes.value.ok) products.value = await pRes.value.json()
    else errorMsg.value += 'Lỗi API Sản phẩm. '
    
    if (cRes.status === 'fulfilled' && cRes.value.ok) categories.value = await cRes.value.json()
    else errorMsg.value += 'Lỗi API Danh mục. '
    
    if (cuRes.status === 'fulfilled' && cuRes.value.ok) customers.value = await cuRes.value.json()
    else errorMsg.value += 'Lỗi API Khách hàng. '
    
    if (sRes.status === 'fulfilled' && sRes.value.ok) suppliers.value = await sRes.value.json()
    else errorMsg.value += 'Lỗi API Nhà cung cấp. '
    
    if (bRes.status === 'fulfilled' && bRes.value.ok) branches.value = await bRes.value.json()
    else errorMsg.value += 'Lỗi API Chi nhánh. '
    
  } catch (err: any) {
    errorMsg.value = 'Lỗi kết nối máy chủ: ' + err.message
  } finally {
    loading.value = false
    await nextTick()
    if (!errorMsg.value) {
      initCharts()
    }
  }
})

// SAFELY PARSE DATA
const totalValue = computed(() => products.value.reduce((s, p) => s + (Number(p.price) || 0) * (Number(p.quantity) || 0), 0))
const totalQty = computed(() => products.value.reduce((s, p) => s + (Number(p.quantity) || 0), 0))
const totalDebt = computed(() => customers.value.reduce((s, c) => s + (Number(c.debt) || 0), 0))
const topProducts = computed(() => [...products.value].sort((a, b) => (Number(b.quantity) || 0) - (Number(a.quantity) || 0)).slice(0, 5))
const topCustomers = computed(() => [...customers.value].sort((a, b) => (Number(b.debt) || 0) - (Number(a.debt) || 0)).slice(0, 5))

// Revenue By Category
const revenueByCategory = computed(() => {
  const map = new Map<string, { name: string; val: number; qty: number }>()
  products.value.forEach(p => {
    const cName = p.categoryName || 'Chưa phân loại'
    if (!map.has(cName)) {
      map.set(cName, { name: cName, val: 0, qty: 0 })
    }
    const c = map.get(cName)!
    c.val += (Number(p.price) || 0) * (Number(p.quantity) || 0)
    c.qty += (Number(p.quantity) || 0)
  })
  return Array.from(map.values()).filter(c => c.val > 0 || c.qty > 0).sort((a, b) => b.val - a.val)
})

function initCharts() {
  if (pieChartRef.value) pieChartInst = echarts.init(pieChartRef.value)
  if (debtChartRef.value) debtChartInst = echarts.init(debtChartRef.value)
  if (qtyChartRef.value) qtyChartInst = echarts.init(qtyChartRef.value)
  if (barChartRef.value) barChartInst = echarts.init(barChartRef.value)
  
  updateCharts()

  window.addEventListener('resize', () => {
    pieChartInst?.resize()
    debtChartInst?.resize()
    qtyChartInst?.resize()
    barChartInst?.resize()
  })
}

function updateCharts() {
  // Chart 1
  if (pieChartInst) {
    const data = revenueByCategory.value.map(c => ({ value: c.val, name: c.name }))
    pieChartInst.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} đ ({d}%)', backgroundColor: 'rgba(0, 0, 0, 0.8)', textStyle: { color: '#fff' } },
      legend: { bottom: '0%', left: 'center', textStyle: { color: '#8094ae' } },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: data.length > 0 ? data : [{ value: 0, name: 'Chưa có', itemStyle: { color: '#e2e8f0' } }]
      }]
    })
  }

  // Chart 2
  if (debtChartInst) {
    let othersDebt = 0;
    const sorted = [...customers.value].sort((a, b) => (Number(b.debt) || 0) - (Number(a.debt) || 0));
    const top5 = sorted.slice(0, 5);
    const others = sorted.slice(5);
    others.forEach(c => othersDebt += Number(c.debt) || 0);
    const data = top5.map(c => ({ value: Number(c.debt) || 0, name: c.name || 'Khách vãng lai' })).filter(d => d.value > 0);
    if (othersDebt > 0) data.push({ value: othersDebt, name: 'Khách hàng khác' });

    debtChartInst.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} đ ({d}%)', backgroundColor: 'rgba(0, 0, 0, 0.8)', textStyle: { color: '#fff' } },
      legend: { bottom: '0%', left: 'center', textStyle: { color: '#8094ae' } },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: data.length > 0 ? data : [{ value: 0, name: 'Không có nợ', itemStyle: { color: '#e2e8f0' } }]
      }]
    })
  }

  // Chart 3
  if (qtyChartInst) {
    const names = revenueByCategory.value.length > 0 ? revenueByCategory.value.map(c => c.name) : ['Trống']
    const quantities = revenueByCategory.value.length > 0 ? revenueByCategory.value.map(c => c.qty) : [0]
    qtyChartInst.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(0, 0, 0, 0.8)', textStyle: { color: '#fff' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: { type: 'category', data: names, axisLabel: { color: '#8094ae', width: 80, overflow: 'truncate' } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } } },
      series: [{ name: 'Số lượng', type: 'bar', data: quantities, itemStyle: { color: '#0ea5e9', borderRadius: [4, 4, 0, 0] } }]
    })
  }

  // Chart 4
  if (barChartInst) {
    const names = topProducts.value.length > 0 ? topProducts.value.map(p => p.name) : ['Trống']
    const quantities = topProducts.value.length > 0 ? topProducts.value.map(p => Number(p.quantity) || 0) : [0]
    barChartInst.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(0, 0, 0, 0.8)', textStyle: { color: '#fff' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } } },
      yAxis: { type: 'category', data: names, axisLabel: { color: '#8094ae', width: 100, overflow: 'truncate' } },
      series: [{ name: 'Tồn kho', type: 'bar', data: quantities, itemStyle: { color: '#4361ee', borderRadius: [0, 4, 4, 0] } }]
    })
  }
}

watch([products, customers, categories], () => {
  if (!loading.value) {
    updateCharts()
  }
}, { deep: true })

function formatVND(val: number) {
  if (!val) return '0đ'
  return new Intl.NumberFormat('vi-VN').format(val) + 'đ'
}
</script>

<template>
  <div v-if="loading" class="text-center p-12 text-[#8094ae]">
    <i class="fas fa-spinner fa-spin text-3xl mb-4 text-[#4361ee]"></i>
    <p>Đang tải dữ liệu...</p>
  </div>
  
  <div v-else class="max-w-[1400px]">

    <div v-if="errorMsg" class="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl flex items-start shadow-sm">
      <i class="fas fa-exclamation-triangle text-xl mr-3 mt-0.5"></i>
      <div>
        <h4 class="font-bold mb-1">Cảnh báo tải dữ liệu</h4>
        <p class="text-sm m-0">{{ errorMsg }}</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
      <!-- Card 1: Blue -->
      <div class="bg-gradient-to-br from-[#4361ee] to-[#3b5bdb] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(67,97,238,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl mb-4 bg-white/20 text-white backdrop-blur-sm">
          <i class="fas fa-wallet"></i>
        </div>
        <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ formatVND(totalValue) }}</div>
        <div class="text-[0.875rem] font-medium text-white/80">Tổng giá trị kho</div>
      </div>

      <!-- Card 2: Green -->
      <div class="bg-gradient-to-br from-[#05b171] to-[#049d63] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(5,177,113,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl mb-4 bg-white/20 text-white backdrop-blur-sm">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ products.length }}</div>
        <div class="text-[0.875rem] font-medium text-white/80">Tổng mặt hàng</div>
      </div>

      <!-- Card 3: Cyan -->
      <div class="bg-gradient-to-br from-[#0ea5e9] to-[#0284c7] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(14,165,233,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl mb-4 bg-white/20 text-white backdrop-blur-sm">
          <i class="fas fa-users"></i>
        </div>
        <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ customers.length }}</div>
        <div class="text-[0.875rem] font-medium text-white/80">Khách hàng</div>
      </div>

      <!-- Card 4: Yellow/Orange -->
      <div class="bg-gradient-to-br from-[#f4bd0e] to-[#d97706] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(244,189,14,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl mb-4 bg-white/20 text-white backdrop-blur-sm">
          <i class="fas fa-building"></i>
        </div>
        <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ formatVND(totalDebt) }}</div>
        <div class="text-[0.875rem] font-medium text-white/80">Phải thu (AR)</div>
      </div>
    </div>

    <!-- Tables Row -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-8">
      
      <!-- Top Selling -->
      <div class="lg:col-span-7">
        <div class="bg-red-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#ea4f52] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden h-full flex flex-col">
          <div class="p-6 border-b border-[#f1f5f9] flex items-center">
            <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-fire text-[#ea4f52] mr-2"></i>Mặt hàng tồn nhiều nhất</h6>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="border-b border-[#f1f5f9]">
                  <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae]">Tên sản phẩm</th>
                  <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae] text-center">Tồn kho</th>
                  <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae] text-right">Giá trị</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in topProducts" :key="item.id" class="border-b border-[#f1f5f9] hover:bg-white/60 transition-colors">
                  <td class="p-4 font-bold text-[#364a63]">{{ item.name }}</td>
                  <td class="p-4 text-center">
                    <span class="inline-block px-3 py-1 bg-white text-[#4361ee] font-bold text-xs rounded-full shadow-sm">
                      {{ item.quantity }}
                    </span>
                  </td>
                  <td class="p-4 text-right font-bold text-[#05b171]">
                    {{ formatVND((Number(item.price) || 0) * (Number(item.quantity) || 0)) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- VIP Customers -->
      <div class="lg:col-span-5">
        <div class="bg-amber-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#f4bd0e] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden h-full flex flex-col">
          <div class="p-6 border-b border-[#f1f5f9] flex items-center">
            <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-crown text-[#f4bd0e] mr-2"></i>Khách nợ nhiều nhất</h6>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <tbody>
                <tr v-for="(item, index) in topCustomers" :key="item.id" class="border-b border-[#f1f5f9] hover:bg-white/60 transition-colors">
                  <td class="p-4 w-[60px]">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
                         :class="index === 0 ? 'bg-[#f4bd0e] text-black shadow-sm' : 'bg-white text-[#8094ae] shadow-sm'">
                      {{ index + 1 }}
                    </div>
                  </td>
                  <td class="p-4">
                    <div class="font-bold text-[#364a63]">{{ item.name }}</div>
                    <div class="text-xs text-[#8094ae] mt-0.5">{{ item.phone }}</div>
                  </td>
                  <td class="p-4 text-right font-bold text-[#4361ee]">
                    {{ formatVND(Number(item.debt) || 0) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Revenue Category -->
    <div class="bg-sky-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#0ea5e9] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden mb-8">
      <div class="p-6 border-b border-[#f1f5f9] flex items-center">
        <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-pie text-[#0ea5e9] mr-2"></i>Giá trị kho theo danh mục</h6>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-[#f1f5f9]">
              <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae]">Danh mục</th>
              <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae]">Tỷ trọng</th>
              <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae] text-center">Số lượng</th>
              <th class="p-4 text-[0.75rem] uppercase font-semibold text-[#8094ae] text-right">Tổng tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in revenueByCategory" :key="item.name" class="border-b border-[#f1f5f9] hover:bg-white/60 transition-colors">
              <td class="p-4 font-bold text-[#364a63]">{{ item.name }}</td>
              <td class="p-4 w-[40%]">
                <div class="flex items-center gap-4">
                  <div class="flex-1 h-1.5 bg-white rounded-full overflow-hidden shadow-inner">
                    <div class="h-full bg-[#4361ee] rounded-full" :style="{ width: `${totalValue > 0 ? (item.val / totalValue) * 100 : 0}%` }"></div>
                  </div>
                  <span class="font-bold text-[#4361ee] text-sm">{{ totalValue > 0 ? ((item.val / totalValue) * 100).toFixed(0) : 0 }}%</span>
                </div>
              </td>
              <td class="p-4 text-center font-medium text-[#364a63]">{{ item.qty }}</td>
              <td class="p-4 text-right font-bold text-[#364a63]">
                {{ formatVND(item.val) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- BIỂU ĐỒ ROW 1 (Pie Charts) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <!-- Category Distribution (Donut Chart) -->
      <div class="bg-sky-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#0ea5e9] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9]">
          <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-pie text-[#0ea5e9] mr-2"></i>Tỷ trọng giá trị danh mục</h6>
        </div>
        <div class="p-4 relative" style="height: 350px;">
          <div ref="pieChartRef" class="w-full h-full"></div>
        </div>
      </div>

      <!-- Debt Distribution (Donut Chart) -->
      <div class="bg-amber-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#f4bd0e] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9]">
          <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-pie text-[#f4bd0e] mr-2"></i>Tỷ trọng công nợ khách hàng</h6>
        </div>
        <div class="p-4 relative" style="height: 350px;">
          <div ref="debtChartRef" class="w-full h-full"></div>
        </div>
      </div>
    </div>

    <!-- BIỂU ĐỒ ROW 2 (Bar Charts) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <!-- Quantity by Category (Bar Chart) -->
      <div class="bg-sky-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#0ea5e9] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9]">
          <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-bar text-[#0ea5e9] mr-2"></i>Số lượng tồn theo danh mục</h6>
        </div>
        <div class="p-4 relative" style="height: 350px;">
          <div ref="qtyChartRef" class="w-full h-full"></div>
        </div>
      </div>

      <!-- Low Stock (Bar Chart) -->
      <div class="bg-indigo-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9] flex justify-between items-center">
          <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-bar text-[#4361ee] mr-2"></i>Sản phẩm tồn nhiều nhất</h6>
          <span v-if="topProducts.length > 0" class="px-3 py-1 bg-white shadow-sm text-[#4361ee] rounded-full text-xs font-bold">
            {{ topProducts.length }} mặt hàng
          </span>
        </div>
        <div class="p-4 relative" style="height: 350px;">
          <div ref="barChartRef" class="w-full h-full"></div>
        </div>
      </div>
    </div>

  </div>
</template>

