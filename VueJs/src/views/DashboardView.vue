<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick, reactive } from 'vue'
import { api } from '../api'
import * as echarts from 'echarts'

const products = ref<any[]>([])
const categories = ref<any[]>([])
const customers = ref<any[]>([])

const branches = ref<any[]>([])

const loading = ref(true)
const errorMsg = ref('')

const branchSalesDataRaw = ref<any>({})
const user = ref<any>(JSON.parse(localStorage.getItem('wh_user') || '{}'))

const trendChartRef = ref<HTMLElement | null>(null)
const catRevenueChartRef = ref<HTMLElement | null>(null)
const branchChartRef = ref<HTMLElement | null>(null)
const topSoldChartRef = ref<HTMLElement | null>(null)

let trendChartInst: echarts.ECharts | null = null
let catRevenueChartInst: echarts.ECharts | null = null
let branchChartInst: echarts.ECharts | null = null
let topSoldChartInst: echarts.ECharts | null = null

const headBranchImportChartRef = ref<HTMLElement | null>(null)
let headBranchImportChartInst: echarts.ECharts | null = null

const isHeadBranchUser = computed(() => {
  const bId = user.value?.branchId || user.value?.branch?.id
  return !bId || Number(bId) === 1 || user.value?.role === 'ADMIN'
})

const totalHeadBranchImport30Days = computed(() => {
  const now = new Date()
  let sum = 0
  const thirtyDaysAgo = new Date(now)
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
  const thirtyDaysAgoStr = thirtyDaysAgo.toISOString().substring(0, 10)
  
  receipts.value.forEach(r => {
    if (r.status !== 'COMPLETED' || !r.createdAt) return
    const receiptDateStr = r.createdAt.substring(0, 10)
    if (receiptDateStr >= thirtyDaysAgoStr) {
      if (r.type === 'IMPORT' && r.destBranchId === 1) {
        const val = (r.details || []).reduce((s: number, det: any) => s + (det.quantity * det.price), 0)
        sum += val
      }
    }
  })
  return sum
})

const receipts = ref<any[]>([])
const inventories = ref<any[]>([])

// Lọc receipts theo chi nhánh của user đăng nhập
const myReceipts = computed(() => {
  const bId = user.value?.branchId || user.value?.branch?.id
  // Nếu là Chi nhánh Tổng (id = 1) hoặc không xác định chi nhánh, hiển thị tổng gộp tất cả
  if (!bId || Number(bId) === 1) return receipts.value
  return receipts.value.filter(r => Number(r.sourceBranchId) === Number(bId) || Number(r.destBranchId) === Number(bId))
})

onMounted(async () => {
  try {
    const [pRes, cRes, cuRes, bRes, rRes, iRes, bsRes] = await Promise.allSettled([
      api.get('/api/products'),
      api.get('/api/categories'),
      api.get('/api/customers'),
      api.get('/api/branches'),
      api.get('/api/receipts/completed-branch'),
      api.get('/api/inventories'),
      api.get('/api/reports/dashboard/branch-sales')
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

    if (bsRes && bsRes.status === 'fulfilled' && bsRes.value.ok) branchSalesDataRaw.value = await bsRes.value.json()

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

// Dữ liệu đã được load xong

// Lọc phiếu xuất bán thành công trong 30 ngày gần nhất
const completedExports30Days = computed(() => {
  const thirtyDaysAgo = new Date()
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
  const limitStr = thirtyDaysAgo.toISOString().substring(0, 10)
  
  return myReceipts.value.filter(r => {
    if (r.type !== 'EXPORT' || r.status !== 'COMPLETED' || !r.createdAt) return false
    const rDateStr = r.createdAt.substring(0, 10)
    return rDateStr >= limitStr
  })
})

// Tính lợi nhuận (Xuất - Nhập) trong 30 ngày gần nhất
const totalProfit30Days = computed(() => {
  const thirtyDaysAgo = new Date()
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
  const limitStr = thirtyDaysAgo.toISOString().substring(0, 10)
  
  let revenue = 0
  let cost = 0
  
  myReceipts.value.forEach(r => {
    if (r.status !== 'COMPLETED' || !r.createdAt) return
    const rDateStr = r.createdAt.substring(0, 10)
    if (rDateStr >= limitStr) {
      const val = (r.details || []).reduce((s: number, d: any) => s + (Number(d.quantity) || 0) * (Number(d.price) || 0), 0)
      if (r.type === 'EXPORT') {
        revenue += val
      } else if (r.type === 'IMPORT') {
        cost += val
      }
    }
  })
  return revenue - cost
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
    .slice(0, 5)
})

// Tỷ trọng doanh thu bán hàng theo danh mục trong 30 ngày
const categorySalesRevenue30Days = computed(() => {
  const map = new Map<string, number>()
  let totalRevenue = 0
  
  completedExports30Days.value.forEach(r => {
    (r.details || []).forEach((d: any) => {
      const cName = getProductCategoryName(d.productId)
      const val = (Number(d.quantity) || 0) * (Number(d.price) || 0)
      map.set(cName, (map.get(cName) || 0) + val)
      totalRevenue += val
    })
  })
  
  const threshold = totalRevenue * 0.05
  let othersVal = 0
  const othersDetails: {name: string, val: number, pct: string}[] = []
  const result: any[] = []
  
  for (const [name, val] of map.entries()) {
    // Náº¿u tá»· trá»ng dÆ°á»›i 5% thÃ¬ gá»™p vÃ o nhÃ³m KhÃ¡c
    if (val < threshold) {
      othersVal += val
      othersDetails.push({ name, val, pct: ((val / totalRevenue) * 100).toFixed(1) })
    } else {
      result.push({ name, val })
    }
  }
  
  if (othersVal > 0) {
    othersDetails.sort((a, b) => b.val - a.val)
    result.push({ name: 'Danh mục khác', val: othersVal, details: othersDetails })
  }
  
  return result.sort((a, b) => b.val - a.val)
})

// Doanh thu xuất bán theo chi nhánh trong 30 ngày (Tổng số)
const branchSales30Days = computed(() => {
  const result: {name: string, val: number}[] = []
  if (branchSalesDataRaw.value) {
    Object.entries(branchSalesDataRaw.value).forEach(([bIdStr, dataArr]) => {
      const bId = Number(bIdStr)
      const bName = branches.value.find(b => b.id === bId)?.name || `Chi nhánh ${bId}`
      const total = (dataArr as number[]).reduce((sum, v) => sum + v, 0)
      // Loáº¡i bá» hoÃ n toÃ n Chi nhÃ¡nh 1 (Kho tá»•ng HÃ  Ná»™i) vÃ¬ khÃ´ng phÃ¡t sinh doanh thu xuáº¥t bÃ¡n láº»
      if (bId !== 1 && total > 0) {
        result.push({ name: bName, val: total })
      }
    })
  }
  return result.sort((a, b) => b.val - a.val)
})

// Removed inventoryValueData computed

const revealedCharts = reactive(new Set<string>())

function initCharts() {
  if (trendChartRef.value && !trendChartInst) trendChartInst = echarts.init(trendChartRef.value)
  if (branchChartRef.value && !branchChartInst) branchChartInst = echarts.init(branchChartRef.value)
  if (catRevenueChartRef.value && !catRevenueChartInst) catRevenueChartInst = echarts.init(catRevenueChartRef.value)
  if (topSoldChartRef.value && !topSoldChartInst) topSoldChartInst = echarts.init(topSoldChartRef.value)
  if (isHeadBranchUser.value && headBranchImportChartRef.value && !headBranchImportChartInst) {
    headBranchImportChartInst = echarts.init(headBranchImportChartRef.value)
  }
  
  // Thiết lập IntersectionObserver cho hiệu ứng cuộn
  const observer = new IntersectionObserver((entries) => {
    let shouldUpdate = false
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-revealed')
        const chartId = entry.target.getAttribute('data-reveal-id')
        if (chartId && !revealedCharts.has(chartId)) {
          revealedCharts.add(chartId)
          shouldUpdate = true
        }
      }
    })
    if (shouldUpdate) updateCharts()
  }, { threshold: 0.05, rootMargin: '0px 0px -40px 0px' })

  nextTick(() => {
    document.querySelectorAll('.scroll-reveal-card').forEach(el => observer.observe(el))
  })
  
  updateCharts()
  
  nextTick(() => {
    trendChartInst?.resize()
    branchChartInst?.resize()
    catRevenueChartInst?.resize()
    topSoldChartInst?.resize()
    headBranchImportChartInst?.resize()
  })

  window.addEventListener('resize', () => {
    trendChartInst?.resize()
    branchChartInst?.resize()
    catRevenueChartInst?.resize()
    topSoldChartInst?.resize()
    headBranchImportChartInst?.resize()
  })
}

function updateCharts() {
  // Chart 0: Trend Chart
  if (trendChartInst && revealedCharts.has('trend')) {
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
      myReceipts.value.forEach(r => {
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
        axisPointer: { type: 'line', lineStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#334155' : '#e2e8f0', width: 1, type: 'dashed' } },
        backgroundColor: document.documentElement.classList.contains('dark-mode') ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.98)',
        borderColor: document.documentElement.classList.contains('dark-mode') ? '#475569' : '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#f8fafc' : '#334155', fontSize: 12 },
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
        splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.15)' } },
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
          lineStyle: { width: 5, shadowColor: 'rgba(5, 177, 113, 0.3)', shadowBlur: 10, shadowOffsetY: 4 },
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
          lineStyle: { width: 5, shadowColor: 'rgba(99, 102, 241, 0.3)', shadowBlur: 10, shadowOffsetY: 4 },
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



  // Chart 5: Doanh thu theo Chi nhÃ¡nh (Combo Cá»™t + ÄÆ°á»ng)
  if (branchChartInst && revealedCharts.has('branch')) {
    const data = branchSales30Days.value
    const names = data.length > 0 ? data.map(b => b.name) : ['Trống']
    const values = data.length > 0 ? data.map(b => b.val) : [0]
    const colors = ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ec4899']

    branchChartInst.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: document.documentElement.classList.contains('dark-mode') ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.98)',
        borderColor: document.documentElement.classList.contains('dark-mode') ? '#475569' : '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#f8fafc' : '#334155', fontSize: 12 },
        formatter: function (params: any) {
          const valFormatted = new Intl.NumberFormat('vi-VN').format(params[0].value) + 'đ'
          return `<div class="font-bold mb-1 text-slate-700">${params[0].name}</div>
                  <div class="flex items-center gap-2 mt-1 text-xs">
                    <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${params[0].color}"></span>
                    <span class="text-slate-500">Doanh thu:</span>
                    <span class="font-bold text-slate-700 ml-auto">${valFormatted}</span>
                  </div>`
        }
      },
      legend: {
        show: true,
        bottom: '0%',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#64748b', fontSize: 12 }
      },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: names, axisLabel: { color: '#8094ae', width: 90, overflow: 'truncate' } },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.15)' } },
        axisLine: { show: false },
        axisTick: { show: false },
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
      series: [
        {
          name: 'Doanh thu (Cột)',
          type: 'bar',
          barMaxWidth: 50,
          data: values.map((val, idx) => ({
            value: val,
            itemStyle: {
              color: colors[idx % colors.length],
              borderRadius: [6, 6, 0, 0]
            }
          }))
        },
        {
          name: 'Đường xu hướng',
          type: 'line',
          smooth: true,
          symbolSize: 8,
          itemStyle: { color: '#f59e0b' },
          lineStyle: { width: 3, type: 'dashed' },
          data: values
        }
      ]
    }, true)
  }

  // Chart 6: Tỷ trọng doanh thu theo Danh mục (30 ngày)
  if (catRevenueChartInst && revealedCharts.has('cat')) {
    const data = categorySalesRevenue30Days.value.map((c: any) => ({ value: c.val, name: c.name, details: c.details }))
    const totalRev = data.reduce((s, item) => s + item.value, 0)
    
    let totalRevStr = '0đ'
    if (totalRev >= 1e9) totalRevStr = (totalRev / 1e9).toFixed(1) + ' Tỷ'
    else if (totalRev >= 1e6) totalRevStr = (totalRev / 1e6).toFixed(1) + ' Tr'
    else totalRevStr = new Intl.NumberFormat('vi-VN').format(totalRev) + 'đ'

    catRevenueChartInst.setOption({
      color: ['#10b981', '#06b6d4', '#6366f1', '#f97316', '#ec4899', '#8b5cf6', '#f43f5e'],
      tooltip: {
        trigger: 'item',
        formatter: function (p: any) {
          const valFormatted = new Intl.NumberFormat('vi-VN').format(p.value) + 'đ'
          let html = `<div class="font-bold text-slate-700">${p.name}</div>
                  <div class="flex items-center gap-2 mt-1 text-xs">
                    <span class="w-2.5 h-2.5 rounded-full" style="background-color: ${p.color}"></span>
                    <span class="text-slate-500">Doanh thu:</span>
                    <span class="font-bold text-slate-700">${valFormatted}</span>
                  </div>
                  <div class="text-xs text-slate-400 mt-1">Tỷ trọng: ${p.percent}%</div>`
          
          if (p.data.details && p.data.details.length > 0) {
            html += `<div class="mt-2 pt-2 border-t border-slate-100">
                       <div class="text-xs font-semibold text-slate-500 mb-1">Gồm các danh mục:</div>`
            p.data.details.forEach((d: any) => {
               html += `<div class="flex justify-between items-center text-xs mt-1">
                          <span class="text-slate-600 truncate max-w-[120px]" title="${d.name}">- ${d.name}</span>
                          <span class="text-slate-700 font-medium ml-3">${d.pct}%</span>
                        </div>`
            })
            html += `</div>`
          }
          return html
        },
        backgroundColor: document.documentElement.classList.contains('dark-mode') ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.98)',
        borderColor: document.documentElement.classList.contains('dark-mode') ? '#475569' : '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#f8fafc' : '#334155', fontSize: 12 },
        padding: [10, 15]
      },
      legend: { show: false }, // Ẩn legend ở dưới, dùng label chỉa ra ngoài cho Pro
      graphic: {
        elements: [
          {
            type: 'text',
            left: 'center',
            top: '46%',
            style: {
              text: 'Tổng thu\n' + totalRevStr,
              textAlign: 'center',
              fill: document.documentElement.classList.contains('dark-mode') ? '#cbd5e1' : '#475569',
              fontSize: 14,
              fontWeight: 'bold',
              lineHeight: 22
            }
          }
        ]
      },
      series: [{
        type: 'pie',
        radius: ['50%', '75%'], // Donut chuẩn, không dùng roseType để tránh méo mó
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { 
          borderRadius: 6, 
          borderColor: '#ffffff', 
          borderWidth: 3, 
          shadowColor: 'rgba(0, 0, 0, 0.05)', 
          shadowBlur: 10,
          shadowOffsetY: 2
        },
        label: {
          show: true,
          position: 'outside',
          formatter: function(params: any) {
            let val = params.value;
            let valStr = '';
            if (val >= 1e9) valStr = (val / 1e9).toFixed(1) + ' Tỷ';
            else if (val >= 1e6) valStr = (val / 1e6).toFixed(1) + ' Tr';
            else valStr = new Intl.NumberFormat('vi-VN').format(val) + 'đ';
            return `{b|${params.name}}\n{c|${valStr}} {d|(${params.percent}%)}`;
          },
          rich: {
            b: { color: document.documentElement.classList.contains('dark-mode') ? '#cbd5e1' : '#475569', fontSize: 13, fontWeight: 'bold', padding: [0, 0, 4, 0] },
            c: { color: document.documentElement.classList.contains('dark-mode') ? '#a5b4fc' : '#6366f1', fontSize: 13, fontWeight: 'bold' },
            d: { color: document.documentElement.classList.contains('dark-mode') ? '#6ee7b7' : '#10b981', fontSize: 13, fontWeight: 'bold' }
          }
        },
        labelLine: {
          show: true,
          length: 10,
          length2: 15,
          smooth: true,
          lineStyle: { width: 1.5, type: 'solid', color: '#cbd5e1' }
        },
        animation: true,
        animationType: 'expansion', // QuÃ©t mÃ u theo chiá»u kim Ä‘á»“ng há»“
        animationEasing: 'cubicOut',
        animationDuration: 1200,
        data: data.length > 0 ? data : [{ value: 0, name: 'Chưa có', itemStyle: { color: '#e2e8f0' } }]
      }]
    }, true)
  }

  // Chart 7: Top 5 Bán Chạy (Nightingale Rose Chart)
  if (topSoldChartInst && revealedCharts.has('top')) {
    const data = topSoldProducts.value
    // Cấu trúc dữ liệu cho Pie Chart
    const pieData = data.length > 0 
      ? data.map(d => ({ name: d.name, value: d.qty }))
      : [{ name: 'Chưa có dữ liệu', value: 0 }]
      
    topSoldChartInst.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '<div class="font-bold mb-1">{b}</div><div class="flex justify-between gap-4"><span class="text-slate-500">Đã bán:</span> <span class="text-indigo-600 font-bold">{c} sp</span></div><div class="flex justify-between gap-4"><span class="text-slate-500">Tỷ trọng:</span> <span class="text-emerald-500 font-bold">{d}%</span></div>',
        backgroundColor: document.documentElement.classList.contains('dark-mode') ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
        borderColor: document.documentElement.classList.contains('dark-mode') ? '#475569' : '#e2e8f0',
        textStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#f8fafc' : '#1e293b' },
        padding: [10, 15]
      },
      // TiÃªu Ä‘á» Typography xá»‹n xÃ² á»Ÿ giá»¯a tÃ¢m
      color: ['#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f59e0b', '#10b981'],
      title: {
        text: 'Tổng Top 5\n' + new Intl.NumberFormat('vi-VN').format(data.reduce((s, d) => s + d.qty, 0)),
        left: 'center',
        top: 'center',
        textStyle: {
          color: document.documentElement.classList.contains('dark-mode') ? '#f8fafc' : '#334155',
          fontSize: 16,
          fontWeight: '900',
          lineHeight: 24
        }
      },
      legend: { show: false },
      series: [
        {
          name: 'Đã xuất bán',
          type: 'pie',
          radius: ['55%', '85%'],
          center: ['50%', '50%'],
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 4,
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.05)'
          },
          label: {
            show: true,
            formatter: '{b|{b}}\n{c|{c} sp}',
            rich: {
              b: { color: document.documentElement.classList.contains('dark-mode') ? '#cbd5e1' : '#1e293b', fontSize: 13, fontWeight: '800', padding: [0, 0, 4, 0] },
              c: { color: document.documentElement.classList.contains('dark-mode') ? '#a5b4fc' : '#4f46e5', fontSize: 15, fontWeight: '900', textShadowBlur: 4, textShadowColor: 'rgba(79, 70, 229, 0.2)' }
            }
          },
          labelLine: {
            smooth: 0.6, // Tăng độ uốn lượn cong vút
            length: 30, // Kéo dài đoạn đầu
            length2: 80, // Kéo dài cực mạnh đoạn nối ngang để vươn hẳn ra xa
            lineStyle: { 
              width: 2.5, // Nét đậm hơn chút
              type: 'dashed' 
            }
          },
          emphasis: {
            scale: true,
            scaleSize: 10,
            itemStyle: {
              shadowBlur: 20,
              shadowOffsetX: 0,
              shadowColor: 'rgba(99, 102, 241, 0.4)'
            }
          },
          data: pieData,
          animation: true,
          animationType: 'expansion', // QuÃ©t mÃ u theo chiá»u kim Ä‘á»“ng há»“
          animationEasing: 'cubicOut',
          animationDuration: 1200,
          animationDelay: function (idx: number) {
            return idx * 100;
          }
        }
      ]
    }, true)
  }

  // Chart: Head Branch Import Trend
  if (isHeadBranchUser.value && headBranchImportChartInst && revealedCharts.has('head-import')) {
    const dates: string[] = []
    const importValues: number[] = []
    
    const now = new Date()
    for (let i = 29; i >= 0; i--) {
      const d = new Date(now)
      d.setDate(d.getDate() - i)
      const dateStr = d.toISOString().substring(0, 10)
      
      const day = d.getDate().toString().padStart(2, '0')
      const month = (d.getMonth() + 1).toString().padStart(2, '0')
      dates.push(`${day}/${month}`)
      
      let impSum = 0
      receipts.value.forEach(r => {
        if (r.status !== 'COMPLETED' || !r.createdAt) return
        if (r.createdAt.substring(0, 10) === dateStr) {
          if (r.type === 'IMPORT' && r.destBranchId === 1) {
            const val = (r.details || []).reduce((s: number, det: any) => s + (det.quantity * det.price), 0)
            impSum += val
          }
        }
      })
      importValues.push(impSum)
    }

    headBranchImportChartInst.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'line', lineStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#334155' : '#e2e8f0', width: 1, type: 'dashed' } },
        backgroundColor: document.documentElement.classList.contains('dark-mode') ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.98)',
        borderColor: document.documentElement.classList.contains('dark-mode') ? '#475569' : '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: document.documentElement.classList.contains('dark-mode') ? '#f8fafc' : '#334155', fontSize: 12 },
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
        splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.15)' } },
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
          lineStyle: { width: 5, shadowColor: 'rgba(5, 177, 113, 0.3)', shadowBlur: 10, shadowOffsetY: 4 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(5, 177, 113, 0.2)' },
              { offset: 1, color: 'rgba(5, 177, 113, 0)' }
            ])
          }
        }
      ]
    }, true)
  }
}

watch([products, customers, categories, receipts, inventories, branches, branchSalesDataRaw], () => {
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

    <div class="mb-6">
      <h2 class="text-2xl font-bold text-slate-800">Tổng quan Dashboard</h2>
    </div>

    <div v-if="errorMsg" class="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl flex items-start shadow-sm">
      <i class="fas fa-exclamation-triangle text-xl mr-3 mt-0.5"></i>
      <div>
        <h4 class="font-bold mb-1">Cảnh báo tải dữ liệu</h4>
        <p class="text-sm m-0">{{ errorMsg }}</p>
      </div>
    </div>

    <div class="space-y-6">

      <!-- Line Chart (Trend) -->
      <div data-reveal-id="trend" class="scroll-reveal-card bg-white rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#4361ee] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col relative">
        <div class="p-6 border-b border-[#f1f5f9] flex justify-between items-center bg-[#f8f9fa]/50">
          <div>
            <h6 class="font-bold text-[#364a63] m-0">
              <i class="fas fa-chart-line text-[#4361ee] mr-2"></i>Xu hướng Nhập - Xuất kho (30 ngày gần nhất)
            </h6>
            <div class="mt-2 text-sm">
              <span class="text-[#8094ae] mr-2">Ước tính lợi nhuận gộp:</span>
              <span :class="['font-extrabold text-lg', totalProfit30Days >= 0 ? 'text-emerald-500' : 'text-rose-500']">
                {{ totalProfit30Days > 0 ? '+' : '' }}{{ formatVND(totalProfit30Days) }}
              </span>
            </div>
          </div>
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
        <div data-reveal-id="branch" class="scroll-reveal-card bg-violet-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#8b5cf6] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col" style="transition-delay: 100ms">
          <div class="p-6 border-b border-[#f1f5f9]">
            <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-store text-[#8b5cf6] mr-2"></i>Doanh thu xuất bán theo Chi nhánh (30 ngày)</h6>
          </div>
          <div class="p-4 relative" style="height: 350px;">
            <div ref="branchChartRef" class="w-full h-full"></div>
          </div>
        </div>

        <!-- Category Sales Share -->
        <div data-reveal-id="cat" class="scroll-reveal-card bg-emerald-50 rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#10b981] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col" style="transition-delay: 200ms">
          <div class="p-6 border-b border-[#f1f5f9]">
            <h6 class="font-bold text-[#364a63] m-0"><i class="fas fa-chart-pie text-[#10b981] mr-2"></i>Tỷ trọng doanh thu theo Danh mục (30 ngày)</h6>
          </div>
          <div class="p-4 relative" style="height: 350px;">
            <div ref="catRevenueChartRef" class="w-full h-full"></div>
          </div>
        </div>
      </div>

      <!-- Top 5 Bán Chạy -->
      <div data-reveal-id="top" class="scroll-reveal-card bg-gradient-to-br from-[#f8fafc] to-[#f1f5f9] rounded-[16px] border border-indigo-100/50 border-t-4 border-t-indigo-500 shadow-[0_8px_30px_rgba(0,0,0,0.04)] overflow-hidden flex flex-col relative group/card" style="transition-delay: 300ms">
        <div class="absolute -top-24 -right-24 w-48 h-48 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div class="absolute -bottom-24 -left-24 w-48 h-48 bg-purple-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div class="p-6 border-b border-indigo-50/50 flex justify-between items-center bg-white/60 backdrop-blur-sm relative z-10">
          <h6 class="font-bold text-slate-800 m-0 tracking-tight flex items-center">
            <span class="w-8 h-8 rounded-lg bg-indigo-100 flex items-center justify-center mr-3 shadow-sm border border-indigo-200/50">
              <i class="fas fa-gem text-indigo-600 text-sm"></i>
            </span>
            Top 5 Sản phẩm Bán chạy (30 ngày)
          </h6>
        </div>
        <div class="p-4 relative bg-white/40 backdrop-blur-sm z-10" style="height: 350px;">
          <div ref="topSoldChartRef" class="w-full h-full"></div>
        </div>
      </div>

      <!-- Xu hướng Nhập kho Chi nhánh Tổng (Chỉ hiển thị cho chi nhánh tổng) -->
      <div v-if="isHeadBranchUser" data-reveal-id="head-import" class="scroll-reveal-card bg-white rounded-[16px] border border-[#f1f5f9] border-t-4 border-t-[#10b981] shadow-[0_2px_10px_rgba(0,0,0,0.02)] overflow-hidden flex flex-col relative" style="transition-delay: 400ms">
        <div class="p-6 border-b border-[#f1f5f9] flex justify-between items-center bg-[#f8f9fa]/50">
          <div>
            <h6 class="font-bold text-[#364a63] m-0">
              <i class="fas fa-arrow-down text-[#10b981] mr-2"></i>Xu hướng Nhập kho Chi nhánh Tổng (30 ngày gần nhất)
            </h6>
            <div class="mt-2 text-sm">
              <span class="text-[#8094ae] mr-2">Tổng giá trị nhập kho:</span>
              <span class="font-extrabold text-lg text-emerald-500">
                {{ formatVND(totalHeadBranchImport30Days) }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-4 text-xs font-semibold text-[#8094ae]">
            <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-full bg-[#05b171]"></span>Nhập kho</span>
          </div>
        </div>
        <div class="p-4 relative" style="height: 350px;">
          <div ref="headBranchImportChartRef" class="w-full h-full"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scroll-reveal-card {
  opacity: 0;
  transform: translateY(40px);
  will-change: transform, opacity;
  transition:
    opacity 0.65s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.65s cubic-bezier(0.22, 1, 0.36, 1);
}
.scroll-reveal-card.is-revealed {
  opacity: 1;
  transform: translateY(0);
}
</style>
