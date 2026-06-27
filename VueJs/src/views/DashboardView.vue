<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { api } from '../api'
import * as echarts from 'echarts'
import jsPDF from 'jspdf'
import html2canvas from 'html2canvas'

const products = ref<any[]>([])
const categories = ref<any[]>([])
const customers = ref<any[]>([])

const branches = ref<any[]>([])

const loading = ref(true)
const errorMsg = ref('')

const isExporting = ref(false)
const inventoryAgeData = ref<any>({})
const stocktakeDiscrepancyData = ref<any[]>([])
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))
const currentUserRole = ref(user.value.role || '')

const trendChartRef = ref<HTMLElement | null>(null)
const soldProductChartRef = ref<HTMLElement | null>(null)
const catRevenueChartRef = ref<HTMLElement | null>(null)
const branchChartRef = ref<HTMLElement | null>(null)
const runwayChartRef = ref<HTMLElement | null>(null)

let trendChartInst: echarts.ECharts | null = null
let soldProductChartInst: echarts.ECharts | null = null
let catRevenueChartInst: echarts.ECharts | null = null
let branchChartInst: echarts.ECharts | null = null
let runwayChartInst: echarts.ECharts | null = null

const receipts = ref<any[]>([])
const inventories = ref<any[]>([])

onMounted(async () => {
  try {
    const [pRes, cRes, cuRes, bRes, rRes, iRes, invAgeRes, stockDiscRes] = await Promise.allSettled([
      api.get('/api/products'),
      api.get('/api/categories'),
      api.get('/api/customers'),
      api.get('/api/branches'),
      api.get('/api/receipts'),
      api.get('/api/inventories'),
      api.get('/api/reports/dashboard/inventory-age'),
      api.get('/api/reports/dashboard/stocktake-discrepancy')
    ])
    
    if (pRes.status === 'fulfilled' && pRes.value.ok) products.value = await pRes.value.json()
    else errorMsg.value += 'Lỗi API Sản phẩm. '
    
    if (cRes.status === 'fulfilled' && cRes.value.ok) categories.value = await cRes.value.json()
    else errorMsg.value += 'Lỗi API Danh mục. '
    
    if (cuRes.status === 'fulfilled' && cuRes.value.ok) customers.value = await cuRes.value.json()
    else errorMsg.value += 'Lỗi API Khách hàng. '

    if (bRes.status === 'fulfilled' && bRes.value.ok) branches.value = await bRes.value.json()
    else errorMsg.value += 'Lỗi API Chi nhánh. '
    
    if (rRes.status === 'fulfilled' && rRes.value.ok) receipts.value = await rRes.value.json()
    else errorMsg.value += 'Lỗi API Phiếu kho. '
    
    if (iRes.status === 'fulfilled' && iRes.value.ok) inventories.value = await iRes.value.json()
    else errorMsg.value += 'Lỗi API Tồn kho. '
    if (invAgeRes.status === 'fulfilled' && invAgeRes.value.ok) inventoryAgeData.value = await invAgeRes.value.json()
    if (stockDiscRes.status === 'fulfilled' && stockDiscRes.value.ok) stocktakeDiscrepancyData.value = await stockDiscRes.value.json()
    
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
const totalValue = computed(() => {
  return inventories.value.reduce((sum, inv) => {
    const prod = products.value.find(p => p.id === inv.productId)
    const price = prod ? Number(prod.price) : 0
    return sum + price * (Number(inv.quantity) || 0)
  }, 0)
})
const totalDebt = computed(() => customers.value.reduce((s, c) => s + (Number(c.debt) || 0), 0))

// NEW DYNAMIC TREND COMPUTED PROPERTIES
const stockValueTrend = computed(() => {
  if (totalValue.value === 0) return { label: '0%', isUp: true }
  const thirtyDaysAgo = new Date()
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
  
  let netFlow = 0
  receipts.value.forEach(r => {
    if (r.status !== 'COMPLETED' || !r.createdAt) return
    const rDate = new Date(r.createdAt)
    if (rDate >= thirtyDaysAgo) {
      const rValue = (r.details || []).reduce((s: number, d: any) => s + (d.quantity * d.price), 0)
      if (r.type === 'IMPORT') {
        netFlow += rValue
      } else if (r.type === 'EXPORT') {
        netFlow -= rValue
      }
    }
  })
  const pct = (netFlow / totalValue.value) * 100
  return {
    label: `${pct >= 0 ? '+' : ''}${pct.toFixed(1)}% vs tháng trước`,
    isUp: pct >= 0
  }
})

const activeProductsPct = computed(() => {
  if (products.value.length === 0) return '0%'
  const inStockProductIds = new Set(
    inventories.value
      .filter(inv => (Number(inv.quantity) || 0) > 0)
      .map(inv => inv.productId)
  )
  const pct = (inStockProductIds.size / products.value.length) * 100
  return `${pct.toFixed(0)}% sẵn kho`
})

const customerTrend = computed(() => {
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  const fourteenDaysAgo = new Date(now.getTime() - 14 * 24 * 60 * 60 * 1000)
  
  let thisWeek = 0
  let lastWeek = 0
  
  customers.value.forEach(c => {
    if (!c.createdAt) return
    const cDate = new Date(c.createdAt)
    if (cDate >= sevenDaysAgo) {
      thisWeek++
    } else if (cDate >= fourteenDaysAgo) {
      lastWeek++
    }
  })
  
  if (lastWeek === 0) {
    return thisWeek > 0 ? { label: `+${thisWeek} tuần này`, isUp: true } : { label: 'Ổn định', isUp: true }
  }
  const pct = ((thisWeek - lastWeek) / lastWeek) * 100
  return {
    label: `${pct >= 0 ? '+' : ''}${pct.toFixed(0)}% vs tuần trước`,
    isUp: pct >= 0
  }
})

const debtTrend = computed(() => {
  const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
  let newDebtAdded = 0
  let debtPaid = 0
  receipts.value.forEach(r => {
    if (r.type !== 'EXPORT' || r.status !== 'COMPLETED' || !r.createdAt) return
    const rDate = new Date(r.createdAt)
    if (rDate >= sevenDaysAgo) {
      const rValue = (r.details || []).reduce((s: number, d: any) => s + (d.quantity * d.price), 0)
      if (r.paymentStatus === 'UNPAID' || r.paymentStatus === 'Chưa thanh toán') {
        newDebtAdded += rValue
      } else if (r.paymentStatus === 'PAID' || r.paymentStatus === 'Đã thanh toán') {
        debtPaid += rValue
      }
    }
  })
  const diff = newDebtAdded - debtPaid
  if (totalDebt.value === 0) return { label: 'Ổn định', isUp: true }
  const pct = (diff / totalDebt.value) * 100
  return {
    label: pct <= 0 ? `Giảm ${Math.abs(pct).toFixed(1)}%` : `Tăng ${pct.toFixed(1)}%`,
    isUp: pct <= 0 // Down is good
  }
})



// Lọc phiếu xuất bán thành công trong 30 ngày gần nhất
const completedExports30Days = computed(() => {
  const thirtyDaysAgo = new Date()
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
  const limitStr = thirtyDaysAgo.toISOString().substring(0, 10)
  
  return receipts.value.filter(r => {
    if (r.type !== 'EXPORT' || r.status !== 'COMPLETED' || !r.createdAt) return false
    const rDateStr = r.createdAt.substring(0, 10)
    return rDateStr >= limitStr
  })
})

// Ánh xạ tên danh mục từ ID sản phẩm
const getProductCategoryName = (productId: number) => {
  const prod = products.value.find(p => p.id === productId)
  return prod?.categoryName || 'Chưa phân loại'
}

// Top 10 sản phẩm bán chạy nhất trong 30 ngày (theo số lượng)
const topSoldProducts = computed(() => {
  const map = new Map<string, number>()
  completedExports30Days.value.forEach(r => {
    (r.details || []).forEach((d: any) => {
      const pName = d.productName || `Sản phẩm #${d.productId}`
      map.set(pName, (map.get(pName) || 0) + (Number(d.quantity) || 0))
    })
  })
  return Array.from(map.entries())
    .map(([name, qty]) => ({ name, qty }))
    .sort((a, b) => b.qty - a.qty)
    .slice(0, 10)
})

// Tỷ trọng doanh thu bán hàng theo danh mục trong 30 ngày
const categorySalesRevenue30Days = computed(() => {
  const map = new Map<string, number>()
  completedExports30Days.value.forEach(r => {
    (r.details || []).forEach((d: any) => {
      const cName = getProductCategoryName(d.productId)
      const val = (Number(d.quantity) || 0) * (Number(d.price) || 0)
      map.set(cName, (map.get(cName) || 0) + val)
    })
  })
  return Array.from(map.entries())
    .map(([name, val]) => ({ name, val }))
    .sort((a, b) => b.val - a.val)
})

// Doanh thu xuất bán theo chi nhánh trong 30 ngày
const branchSales30Days = computed(() => {
  const map = new Map<string, number>()
  completedExports30Days.value.forEach(r => {
    const bName = r.sourceBranchName || 'Chưa xác định'
    const val = (r.details || []).reduce((sum: number, d: any) => sum + (Number(d.quantity) || 0) * (Number(d.price) || 0), 0)
    map.set(bName, (map.get(bName) || 0) + val)
  })
  return Array.from(map.entries())
    .map(([name, val]) => ({ name, val }))
    .sort((a, b) => b.val - a.val)
})

const categoryRunwayData = computed(() => {
  const categoryNames = new Set<string>()
  categories.value.forEach(c => {
    if (c.name) categoryNames.add(c.name)
  })
  products.value.forEach(p => {
    const cName = p.categoryName || 'Chưa phân loại'
    categoryNames.add(cName)
  })

  const result: any[] = []
  categoryNames.forEach(cName => {
    const currentStock = inventories.value
      .filter(inv => {
        const prodCat = getProductCategoryName(inv.productId)
        return prodCat === cName
      })
      .reduce((sum, inv) => sum + (Number(inv.quantity) || 0), 0)

    let sold30Days = 0
    completedExports30Days.value.forEach(r => {
      (r.details || []).forEach((d: any) => {
        const prodCat = getProductCategoryName(d.productId)
        if (prodCat === cName) {
          sold30Days += (Number(d.quantity) || 0)
        }
      })
    })

    const dailyBurnRate = sold30Days / 30
    let daysOfCoverReal = 0
    let daysOfCoverCapped = 0
    if (currentStock > 0) {
      if (dailyBurnRate > 0) {
        daysOfCoverReal = currentStock / dailyBurnRate
        daysOfCoverCapped = Math.min(90, daysOfCoverReal)
      } else {
        daysOfCoverReal = 9999
        daysOfCoverCapped = 90
      }
    }

    result.push({
      name: cName,
      currentStock,
      sold30Days,
      dailyBurnRate,
      daysOfCoverReal,
      daysOfCoverCapped
    })
  })

  return result.sort((a, b) => a.daysOfCoverReal - b.daysOfCoverReal)
})

function initCharts() {
  if (trendChartRef.value && !trendChartInst) trendChartInst = echarts.init(trendChartRef.value)
  if (branchChartRef.value && !branchChartInst) branchChartInst = echarts.init(branchChartRef.value)
  if (catRevenueChartRef.value && !catRevenueChartInst) catRevenueChartInst = echarts.init(catRevenueChartRef.value)
  if (soldProductChartRef.value && !soldProductChartInst) soldProductChartInst = echarts.init(soldProductChartRef.value)
  if (runwayChartRef.value && !runwayChartInst) runwayChartInst = echarts.init(runwayChartRef.value)
  
  updateCharts()
  
  nextTick(() => {
    trendChartInst?.resize()
    branchChartInst?.resize()
    catRevenueChartInst?.resize()
    soldProductChartInst?.resize()
    runwayChartInst?.resize()
  })

  window.addEventListener('resize', () => {
    trendChartInst?.resize()
    branchChartInst?.resize()
    catRevenueChartInst?.resize()
    soldProductChartInst?.resize()
    runwayChartInst?.resize()
  })
}

function updateCharts() {
  // Chart 0: Trend Chart
  if (trendChartInst) {
    const dates: string[] = []
    const importValues: number[] = []
    const exportValues: number[] = []
    
    const now = new Date()
    for (let i = 29; i >= 0; i--) {
      const d = new Date(now)
      d.setDate(d.getDate() - i)
      const dateStr = d.toISOString().substring(0, 10)
      
      const day = d.getDate().toString().padStart(2, '0')
      const month = (d.getMonth() + 1).toString().padStart(2, '0')
      dates.push(`${day}/${month}`)
      
      let impSum = 0
      let expSum = 0
      receipts.value.forEach(r => {
        if (r.status !== 'COMPLETED' || !r.createdAt) return
        if (r.createdAt.substring(0, 10) === dateStr) {
          const val = (r.details || []).reduce((s: number, det: any) => s + (det.quantity * det.price), 0)
          if (r.type === 'IMPORT') impSum += val
          else if (r.type === 'EXPORT') expSum += val
        }
      })
      importValues.push(impSum)
      exportValues.push(expSum)
    }

    trendChartInst.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'line', lineStyle: { color: '#e2e8f0', width: 1, type: 'dashed' } },
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#334155', fontSize: 12 },
        formatter: function (params: any) {
          let res = `<div class="font-bold mb-1.5 text-slate-700">${params[0].name}</div>`
          params.forEach((p: any) => {
            const formattedVal = new Intl.NumberFormat('vi-VN').format(p.value) + 'đ'
            res += `<div class="flex items-center gap-4 mt-1 text-xs">
              <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${p.color}; box-shadow: 0 0 8px ${p.color}"></span>
              <span class="text-slate-500">${p.seriesName}:</span>
              <span class="font-bold text-slate-700 ml-auto">${formattedVal}</span>
            </div>`
          })
          return res
        }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
        axisLine: { lineStyle: { color: '#cbd5e1' } },
        axisLabel: { color: '#8094ae', fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: {
          color: '#8094ae',
          fontSize: 11,
          formatter: function (value: number) {
            if (value >= 1e9) return (value / 1e9).toFixed(1) + ' tỷ'
            if (value >= 1e6) return (value / 1e6).toFixed(0) + ' tr'
            if (value >= 1e3) return (value / 1e3).toFixed(0) + ' k'
            return value
          }
        }
      },
      series: [
        {
          name: 'Nhập kho',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: importValues,
          itemStyle: { color: '#05b171' },
          lineStyle: { width: 3, shadowColor: 'rgba(5, 177, 113, 0.3)', shadowBlur: 10, shadowOffsetY: 4 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(5, 177, 113, 0.2)' },
              { offset: 1, color: 'rgba(5, 177, 113, 0)' }
            ])
          }
        },
        {
          name: 'Xuất bán',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: exportValues,
          itemStyle: { color: '#6366f1' },
          lineStyle: { width: 3, shadowColor: 'rgba(99, 102, 241, 0.3)', shadowBlur: 10, shadowOffsetY: 4 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.2)' },
              { offset: 1, color: 'rgba(99, 102, 241, 0)' }
            ])
          }
        }
      ]
    })
  }



  // Chart 5: Doanh thu theo Chi nhánh
  if (branchChartInst) {
    const names = branchSales30Days.value.length > 0 ? branchSales30Days.value.map(b => b.name) : ['Trống']
    const values = branchSales30Days.value.length > 0 ? branchSales30Days.value.map(b => b.val) : [0]
    branchChartInst.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#334155', fontSize: 12 },
        formatter: function (params: any) {
          const p = params[0]
          const valFormatted = new Intl.NumberFormat('vi-VN').format(p.value) + 'đ'
          return `<div class="font-bold mb-1 text-slate-700">${p.name}</div>
                  <div class="flex items-center gap-2 mt-1 text-xs">
                    <span class="w-2.5 h-2.5 rounded-full" style="background-color: #8b5cf6"></span>
                    <span class="text-slate-500">Doanh thu:</span>
                    <span class="font-bold text-slate-700 ml-auto">${valFormatted}</span>
                  </div>`
        }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: names, axisLabel: { color: '#8094ae', width: 90, overflow: 'truncate' } },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: {
          color: '#8094ae',
          formatter: function (value: number) {
            if (value >= 1e9) return (value / 1e9).toFixed(1) + ' tỷ'
            if (value >= 1e6) return (value / 1e6).toFixed(0) + ' tr'
            if (value >= 1e3) return (value / 1e3).toFixed(0) + ' k'
            return value
          }
        }
      },
      series: [{
        name: 'Doanh thu',
        type: 'bar',
        barWidth: '45%',
        data: values,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#7c3aed' },
            { offset: 1, color: '#a78bfa' }
          ]),
          borderRadius: [6, 6, 0, 0],
          shadowColor: 'rgba(124, 58, 237, 0.15)',
          shadowBlur: 8,
          shadowOffsetY: 4
        }
      }]
    })
  }

  // Chart 6: Tỷ trọng doanh thu theo Danh mục (30 ngày)
  if (catRevenueChartInst) {
    const data = categorySalesRevenue30Days.value.map(c => ({ value: c.val, name: c.name }))
    catRevenueChartInst.setOption({
      color: ['#10b981', '#06b6d4', '#6366f1', '#f97316', '#ec4899', '#cbd5e1'],
      tooltip: {
        trigger: 'item',
        formatter: function (p: any) {
          const valFormatted = new Intl.NumberFormat('vi-VN').format(p.value) + 'đ'
          return `<div class="font-bold text-slate-700">${p.name}</div>
                  <div class="flex items-center gap-2 mt-1 text-xs">
                    <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${p.color}"></span>
                    <span class="text-slate-500">Doanh thu:</span>
                    <span class="font-bold text-slate-700">${valFormatted} (${p.percent}%)</span>
                  </div>`
        },
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#334155', fontSize: 12 }
      },
      legend: { bottom: '0%', left: 'center', textStyle: { color: '#8094ae' } },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2, shadowColor: 'rgba(0,0,0,0.02)', shadowBlur: 10 },
        label: { show: false },
        data: data.length > 0 ? data : [{ value: 0, name: 'Chưa có', itemStyle: { color: '#e2e8f0' } }]
      }]
    })
  }

  // Chart 7: Top 10 sản phẩm bán chạy nhất
  if (soldProductChartInst) {
    const names = topSoldProducts.value.length > 0 ? topSoldProducts.value.map(p => p.name) : ['Trống']
    const quantities = topSoldProducts.value.length > 0 ? topSoldProducts.value.map(p => p.qty) : [0]
    soldProductChartInst.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#334155', fontSize: 12 },
        formatter: function (params: any) {
          const p = params[0]
          return `<div class="font-bold mb-1 text-slate-700">${p.name}</div>
                  <div class="flex items-center gap-2 mt-1 text-xs">
                    <span class="w-2.5 h-2.5 rounded-full" style="background-color: #f43f5e"></span>
                    <span class="text-slate-500">Đã bán:</span>
                    <span class="font-bold text-slate-700 ml-auto">${p.value} sản phẩm</span>
                  </div>`
        }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#8094ae' } },
      yAxis: {
        type: 'category',
        data: names,
        inverse: true,
        axisLabel: { color: '#8094ae', width: 150, overflow: 'truncate' }
      },
      series: [{
        name: 'Đã bán',
        type: 'bar',
        barWidth: '55%',
        data: quantities,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#e11d48' },
            { offset: 1, color: '#fda4af' }
          ]),
        }
      }]
    })
  }

  // Chart 8: Dự báo số ngày bán hàng còn lại (Inventory Runway)
  if (runwayChartInst) {
    const dataSorted = [...categoryRunwayData.value].reverse()
    const names = dataSorted.length > 0 ? dataSorted.map(d => d.name) : ['Trống']
    const values = dataSorted.length > 0 ? dataSorted.map(d => d.daysOfCoverCapped) : [0]
    
    runwayChartInst.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#334155', fontSize: 12 },
        formatter: function (params: any) {
          const idx = params[0].dataIndex
          const item = dataSorted[idx]
          if (!item) return ''
          
          let runwayText = ''
          if (item.currentStock === 0) {
            runwayText = `<span class="font-bold text-[#ef4444]">Hết hàng (0 ngày)</span>`
          } else if (item.sold30Days === 0) {
            runwayText = `<span class="font-bold text-[#10b981]">Vô hạn (Không tiêu thụ trong 30 ngày)</span>`
          } else {
            const formattedDays = item.daysOfCoverReal.toFixed(1)
            let colorClass = 'text-[#10b981]'
            if (item.daysOfCoverReal < 7) colorClass = 'text-[#ef4444]'
            else if (item.daysOfCoverReal <= 15) colorClass = 'text-[#f59e0b]'
            runwayText = `<span class="font-bold ${colorClass}">${formattedDays} ngày</span>`
          }
          
          return `<div class="font-bold mb-1.5 text-slate-700">${item.name}</div>
                  <div class="space-y-1 text-xs">
                    <div class="flex justify-between gap-4"><span class="text-slate-500 font-medium">Tồn kho hiện tại:</span><span class="font-bold text-slate-700">${item.currentStock}</span></div>
                    <div class="flex justify-between gap-4"><span class="text-slate-500 font-medium">Đã xuất (30 ngày):</span><span class="font-bold text-slate-700">${item.sold30Days}</span></div>
                    <div class="flex justify-between gap-4"><span class="text-slate-500 font-medium">Tốc độ tiêu thụ:</span><span class="font-bold text-slate-700">${(item.dailyBurnRate).toFixed(2)}/ngày</span></div>
                    <div class="border-t border-slate-100 my-1"></div>
                    <div class="flex justify-between gap-4"><span class="text-slate-500 font-semibold">Dự kiến đủ bán:</span>${runwayText}</div>
                  </div>`
        }
      },
      grid: { left: '3%', right: '5%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: {
        type: 'value',
        max: 90,
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: {
          color: '#8094ae',
          formatter: function (value: number) {
            if (value >= 90) return '90+ ngày'
            return value + ' ngày'
          }
        }
      },
      yAxis: {
        type: 'category',
        data: names,
        axisLabel: { color: '#8094ae', width: 120, overflow: 'truncate' }
      },
      series: [{
        name: 'Số ngày bán còn lại',
        type: 'bar',
        barWidth: '55%',
        data: values,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: function (params: any) {
            const idx = params.dataIndex
            const item = dataSorted[idx]
            if (!item) return '#cbd5e1'
            if (item.currentStock === 0 || item.daysOfCoverReal < 7) {
              return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#ef4444' },
                { offset: 1, color: '#fca5a5' }
              ])
            } else if (item.daysOfCoverReal <= 15) {
              return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#f59e0b' },
                { offset: 1, color: '#fde047' }
              ])
            } else {
              return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#10b981' },
                { offset: 1, color: '#6ee7b7' }
              ])
            }
          },
          shadowColor: 'rgba(0, 0, 0, 0.05)',
          shadowBlur: 8,
          shadowOffsetX: 4
        }
      }]
    })
  }
}

watch([products, customers, categories, receipts, inventories], () => {
  if (!loading.value) {
    updateCharts()
  }
}, { deep: true })


async function exportPDF() {
  isExporting.value = true;
  try {
    const pdfEl = document.getElementById('pdf-report-template');
    if (!pdfEl) return;
    
    // Make visible temporarily out of bounds
    pdfEl.style.left = '0';
    
    // --- Render ECharts for PDF ---
    // 1. Trend Chart (Line)
    const tChart = echarts.init(document.getElementById('pdf-trend-chart'));
    const tOpt = trendChartInst.getOption();
    tOpt.animation = false;
    if(tOpt.xAxis && tOpt.xAxis[0]) {
      tOpt.xAxis[0].axisLabel = { ...tOpt.xAxis[0].axisLabel, hideOverlap: true, rotate: 15 };
    }
    tChart.setOption(tOpt);

    // 2. Category Revenue Chart (Pie)
    const cChart = echarts.init(document.getElementById('pdf-cat-revenue-chart'));
    const cOpt = catRevenueChartInst.getOption();
    cOpt.animation = false;
    cOpt.legend[0].show = true;
    cOpt.legend[0].bottom = 0;
    cOpt.series[0].radius = ['35%', '50%']; 
    cOpt.series[0].center = ['50%', '45%'];
    cOpt.series[0].label = { show: true, formatter: '{b}\n{c}đ ({d}%)', position: 'outside', fontSize: 10 };
    cOpt.series[0].labelLine = { length: 10, length2: 15 };
    cChart.setOption(cOpt);

    // 3. Branch Chart (Bar Horizontal)
    const bChart = echarts.init(document.getElementById('pdf-branch-chart'));
    const bOpt = branchChartInst.getOption();
    bOpt.animation = false;
    bOpt.series[0].barMaxWidth = 30; // Prevent super thick bars
    bOpt.series[0].label = { show: true, position: 'right', formatter: '{c}đ', fontSize: 10 };
    bChart.setOption(bOpt);

    // 4. Inventory Age Chart (Pie)
    const iChart = echarts.init(document.getElementById('pdf-inventory-age-chart'));
    iChart.setOption({
      animation: false,
      color: ['#10b981', '#f59e0b', '#ef4444'],
      legend: { show: false }, 
      series: [{
        type: 'pie', 
        radius: ['35%', '55%'],
        center: ['50%', '50%'],
        label: { show: true, formatter: '{b}\n{c} mặt hàng ({d}%)', fontSize: 11, lineHeight: 16 },
        labelLine: { show: true, length: 15, length2: 20 },
        data: [
          { name: 'Luân chuyển tốt (<30 ngày)', value: inventoryAgeData.value?.freshItems || 0 },
          { name: 'Tồn kho chậm (30-90 ngày)', value: inventoryAgeData.value?.slowItems || 0 },
          { name: 'Tồn đọng / Dead stock (>90 ngày)', value: inventoryAgeData.value?.deadItems || 0 }
        ]
      }]
    });

    // 5. Stocktake Chart (Bar Vertical)
    const sChart = echarts.init(document.getElementById('pdf-stocktake-chart'));
    const stData = stocktakeDiscrepancyData.value || [];
    sChart.setOption({
      animation: false,
      color: ['#6366f1', '#f43f5e'],
      legend: { bottom: 0 },
      xAxis: { 
        type: 'category', 
        data: stData.map(d => new Date(d.date).toLocaleDateString('vi-VN')),
        axisLabel: { hideOverlap: true }
      },
      yAxis: { type: 'value' },
      series: [
        { name: 'Lệch thừa', type: 'bar', barMaxWidth: 40, label: {show: true, position:'top', fontSize: 10}, data: stData.map(d => d.surplusValue) },
        { name: 'Lệch thiếu', type: 'bar', barMaxWidth: 40, label: {show: true, position:'top', fontSize: 10}, data: stData.map(d => d.missingValue) }
      ]
    });

    // Wait for rendering
    await new Promise(r => setTimeout(r, 1000));

    // Capture Pages
    const canvas1 = await html2canvas(document.getElementById('pdf-page-1'), { scale: 2, useCORS: true, logging: false });
    const img1 = canvas1.toDataURL('image/png');

    const canvas2 = await html2canvas(document.getElementById('pdf-page-2'), { scale: 2, useCORS: true, logging: false });
    const img2 = canvas2.toDataURL('image/png');

    // Create PDF
    const pdf = new jsPDF('p', 'mm', 'a4');
    const pdfWidth = 210;
    const pdfHeight = 297;
    
    pdf.addImage(img1, 'PNG', 0, 0, pdfWidth, pdfHeight);
    pdf.addPage();
    pdf.addImage(img2, 'PNG', 0, 0, pdfWidth, pdfHeight);
    
    pdf.save('Bao_Cao_Phan_Tich.pdf');

    // Clean up
    tChart.dispose();
    cChart.dispose();
    bChart.dispose();
    iChart.dispose();
    sChart.dispose();
    
    pdfEl.style.left = '-9999px';
  } catch (err) {
    console.error('Lỗi khi xuất PDF:', err);
    alert('Có lỗi xảy ra khi xuất file PDF');
  } finally {
    isExporting.value = false;
  }
}

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

    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-slate-800">Tổng quan Dashboard</h2>
      <button v-if="['ADMIN', 'MANAGER'].includes(currentUserRole)"
        @click="exportPDF" :disabled="isExporting"
        class="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white font-bold rounded-lg shadow-md disabled:opacity-50 flex items-center transition-colors">
        <i class="fas" :class="isExporting ? 'fa-spinner fa-spin' : 'fa-file-pdf'"></i>
        <span class="ml-2">{{ isExporting ? 'Đang xuất PDF...' : 'Xuất PDF Báo cáo' }}</span>
      </button>
    </div>

    <div v-if="errorMsg" class="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl flex items-start shadow-sm">
      <i class="fas fa-exclamation-triangle text-xl mr-3 mt-0.5"></i>
      <div>
        <h4 class="font-bold mb-1">Cảnh báo tải dữ liệu</h4>
        <p class="text-sm m-0">{{ errorMsg }}</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
      <!-- Card 1: Blue (Tổng giá trị kho) -->
      <div class="bg-gradient-to-br from-[#4361ee] to-[#3b5bdb] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(67,97,238,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300 flex flex-col justify-between">
        <div>
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl bg-white/20 text-white backdrop-blur-sm">
              <i class="fas fa-wallet"></i>
            </div>
            <!-- Trend Badge -->
            <span :class="['px-2.5 py-1 rounded-full text-xs font-bold flex items-center gap-1 backdrop-blur-md border border-white/10', stockValueTrend.isUp ? 'bg-emerald-500/25 text-emerald-200' : 'bg-red-500/25 text-red-200']">
              <i :class="['fas', stockValueTrend.isUp ? 'fa-arrow-trend-up' : 'fa-arrow-trend-down']"></i>
              {{ stockValueTrend.label }}
            </span>
          </div>
          <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ formatVND(totalValue) }}</div>
          <div class="text-[0.875rem] font-medium text-white/80">Tổng giá trị kho</div>
        </div>
      </div>

      <!-- Card 2: Green (Tổng mặt hàng) -->
      <div class="bg-gradient-to-br from-[#05b171] to-[#049d63] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(5,177,113,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300 flex flex-col justify-between">
        <div>
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl bg-white/20 text-white backdrop-blur-sm">
              <i class="fas fa-check-circle"></i>
            </div>
            <!-- Status Badge -->
            <span class="px-2.5 py-1 rounded-full text-xs font-bold flex items-center gap-1 backdrop-blur-md border border-white/10 bg-emerald-500/25 text-emerald-200">
              <i class="fas fa-boxes-packing"></i>
              {{ activeProductsPct }}
            </span>
          </div>
          <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ products.length }}</div>
          <div class="text-[0.875rem] font-medium text-white/80">Tổng mặt hàng</div>
        </div>
      </div>

      <!-- Card 3: Cyan (Khách hàng) -->
      <div class="bg-gradient-to-br from-[#0ea5e9] to-[#0284c7] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(14,165,233,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300 flex flex-col justify-between">
        <div>
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl bg-white/20 text-white backdrop-blur-sm">
              <i class="fas fa-users"></i>
            </div>
            <!-- Trend Badge -->
            <span :class="['px-2.5 py-1 rounded-full text-xs font-bold flex items-center gap-1 backdrop-blur-md border border-white/10', customerTrend.isUp ? 'bg-emerald-500/25 text-emerald-200' : 'bg-red-500/25 text-red-200']">
              <i class="fas fa-user-plus"></i>
              {{ customerTrend.label }}
            </span>
          </div>
          <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ customers.length }}</div>
          <div class="text-[0.875rem] font-medium text-white/80">Khách hàng</div>
        </div>
      </div>

      <!-- Card 4: Yellow/Orange (Phải thu - AR) -->
      <div class="bg-gradient-to-br from-[#f4bd0e] to-[#d97706] text-white rounded-[16px] p-6 shadow-[0_4px_15px_rgba(244,189,14,0.25)] hover:-translate-y-1 hover:shadow-lg transition-transform duration-300 flex flex-col justify-between">
        <div>
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center text-2xl bg-white/20 text-white backdrop-blur-sm">
              <i class="fas fa-building"></i>
            </div>
            <!-- Debt Status Badge -->
            <span :class="['px-2.5 py-1 rounded-full text-xs font-bold flex items-center gap-1 backdrop-blur-md border border-white/10', debtTrend.isUp ? 'bg-emerald-500/25 text-emerald-200' : 'bg-amber-500/25 text-amber-200']">
              <i :class="['fas', debtTrend.isUp ? 'fa-shield-halved' : 'fa-triangle-exclamation']"></i>
              {{ debtTrend.label }}
            </span>
          </div>
          <div class="text-[1.75rem] font-extrabold leading-tight mb-1">{{ formatVND(totalDebt) }}</div>
          <div class="text-[0.875rem] font-medium text-white/80">Phải thu (AR)</div>
        </div>
      </div>
    </div>

    <div class="space-y-6">
      <!-- Line Chart (Trend) -->
      <div class="bg-white rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9] flex justify-between items-center bg-[#f8f9fa]/50">
          <h6 class="font-bold text-[#364a63] m-0">
            <i class="fas fa-chart-line text-[#4361ee] mr-2"></i>Xu hướng Nhập - Xuất kho (30 ngày gần nhất)
          </h6>
          <div class="flex items-center gap-4 text-xs font-semibold text-[#8094ae]">
            <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full bg-[#10b981]"></span>Nhập kho</span>
            <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full bg-[#6366f1]"></span>Xuất bán</span>
          </div>
        </div>
        <div class="p-4 relative" style="height: 350px;">
          <div ref="trendChartRef" class="w-full h-full"></div>
        </div>
      </div>

      <!-- Branch Sales & Category Sales Revenue Share -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Branch Sales -->
        <div class="bg-violet-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#8b5cf6] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
          <div class="p-6 border-b border-[#f1f5f9]">
            <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-store text-[#8b5cf6] mr-2"></i>Doanh thu xuất bán theo Chi nhánh (30 ngày)</h6>
          </div>
          <div class="p-4 relative" style="height: 350px;">
            <div ref="branchChartRef" class="w-full h-full"></div>
          </div>
        </div>

        <!-- Category Sales Share -->
        <div class="bg-emerald-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#10b981] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
          <div class="p-6 border-b border-[#f1f5f9]">
            <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-pie text-[#10b981] mr-2"></i>Tỷ trọng doanh thu theo Danh mục (30 ngày)</h6>
          </div>
          <div class="p-4 relative" style="height: 350px;">
            <div ref="catRevenueChartRef" class="w-full h-full"></div>
          </div>
        </div>
      </div>

      <!-- Top Sold Products (Horizontal Bar Chart) -->
      <div class="bg-rose-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#f43f5e] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9] flex justify-between items-center">
          <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-fire text-[#f43f5e] mr-2"></i>Top 10 sản phẩm bán chạy nhất (30 ngày)</h6>
          <span v-if="topSoldProducts.length > 0" class="px-3 py-1 bg-white shadow-sm text-[#f43f5e] rounded-full text-xs font-bold">
            {{ topSoldProducts.length }} mặt hàng
          </span>
        </div>
        <div class="p-4 relative" style="height: 400px;">
          <div ref="soldProductChartRef" class="w-full h-full"></div>
        </div>
      </div>

      <!-- Inventory Runway (Horizontal Bar Chart) -->
      <div class="bg-amber-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#f59e0b] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col">
        <div class="p-6 border-b border-[#f1f5f9] flex justify-between items-center">
          <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-hourglass-half text-[#f59e0b] mr-2"></i>Dự báo số ngày bán hàng còn lại theo Danh mục (Inventory Runway)</h6>
          <span class="px-3 py-1 bg-white shadow-sm text-[#f59e0b] rounded-full text-xs font-bold">
            Theo tốc độ bán 30 ngày qua
          </span>
        </div>
        <div class="p-4 relative" style="height: 400px;">
          <div ref="runwayChartRef" class="w-full h-full"></div>
        </div>
      </div>
    </div>



  </div>


  <!-- Hidden PDF Report Template: Fixed Pixel Dimensions for A4 (794x1123px) -->
  <div id="pdf-report-template" style="position: absolute; left: -9999px; top: 0; width: 794px; font-family: sans-serif; color: #334155; z-index: -1000;">
    
    <!-- Trang 1 -->
    <div id="pdf-page-1" style="width: 794px; height: 1123px; padding: 40px; box-sizing: border-box; background: white; overflow: hidden; display: flex; flex-direction: column;">
      <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #4361ee; padding-bottom: 10px; margin-bottom: 30px;">
        <div style="font-size: 24px; font-weight: bold; color: #4361ee;">WAREHUB</div>
        <div style="text-align: right;">
          <div style="font-size: 18px; font-weight: bold;">BÁO CÁO KẾT QUẢ KINH DOANH</div>
          <div style="font-size: 12px; color: #64748b;">Ngày xuất: {{ new Date().toLocaleDateString('vi-VN') }}</div>
        </div>
      </div>
      
      <!-- Trend (Full width, moderate height) -->
      <div style="margin-bottom: 25px; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; flex: none;">
        <div style="font-weight: bold; font-size: 14px; margin-bottom: 10px;">Xu hướng Nhập - Xuất kho (30 ngày gần nhất)</div>
        <div id="pdf-trend-chart" style="width: 100%; height: 280px;"></div>
      </div>

      <!-- Split Row: Category and Branch -->
      <div style="display: flex; gap: 20px; flex: none;">
        <div style="flex: 1; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px;">
          <div style="font-weight: bold; font-size: 14px; margin-bottom: 10px;">Tỷ trọng Doanh thu theo Danh mục</div>
          <div id="pdf-cat-revenue-chart" style="width: 100%; height: 280px;"></div>
        </div>
        <div style="flex: 1; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px;">
          <div style="font-weight: bold; font-size: 14px; margin-bottom: 10px;">Doanh thu theo Chi nhánh</div>
          <div id="pdf-branch-chart" style="width: 100%; height: 280px;"></div>
        </div>
      </div>
    </div>
    
    <!-- Trang 2 -->
    <div id="pdf-page-2" style="width: 794px; height: 1123px; padding: 40px; box-sizing: border-box; background: white; overflow: hidden; display: flex; flex-direction: column; margin-top: 50px;">
      <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #4361ee; padding-bottom: 10px; margin-bottom: 30px;">
        <div style="font-size: 24px; font-weight: bold; color: #4361ee;">WAREHUB</div>
        <div style="text-align: right;">
          <div style="font-size: 18px; font-weight: bold;">KIỂM SOÁT TỒN KHO & KIỂM KÊ</div>
        </div>
      </div>
      
      <!-- Inventory Age (Full width bounding box, pie will center and scale to height) -->
      <div style="margin-bottom: 25px; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; flex: none;">
        <div style="font-weight: bold; font-size: 14px; margin-bottom: 10px;">Cơ cấu Tuổi thọ Tồn kho (Toàn hệ thống)</div>
        <div id="pdf-inventory-age-chart" style="width: 100%; height: 280px;"></div>
      </div>

      <!-- Stocktake (Full width) -->
      <div style="margin-bottom: 40px; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; flex: none;">
        <div style="font-weight: bold; font-size: 14px; margin-bottom: 10px;">Lịch sử Chênh lệch Kiểm kê</div>
        <div id="pdf-stocktake-chart" style="width: 100%; height: 280px;"></div>
      </div>
      
      <!-- Signatures -->
      <div style="margin-top: auto; display: flex; justify-content: space-between; padding: 0 80px; margin-bottom: 80px;">
        <div style="text-align: center;">
          <div style="font-weight: bold; font-size: 16px; margin-bottom: 120px;">Người lập biểu</div>
          <div style="color: #64748b; font-size: 14px;">(Ký, ghi rõ họ tên)</div>
        </div>
        <div style="text-align: center;">
          <div style="font-weight: bold; font-size: 16px; margin-bottom: 120px;">Giám đốc phê duyệt</div>
          <div style="color: #64748b; font-size: 14px;">(Ký, ghi rõ họ tên)</div>
        </div>
      </div>
    </div>
  </div>

</template>


