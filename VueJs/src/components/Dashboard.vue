<template>
  <div class="min-h-screen text-[var(--text-main)] font-sans flex flex-col">
    <!-- Top Navigation Bar -->
    <header class="sticky top-0 z-50 glass-panel border-b border-x-0 border-t-0 rounded-none h-16 bg-white/90">
      <div class="max-w-[1600px] mx-auto px-6 h-full flex items-center justify-between">
        <!-- Brand -->
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-red-50 flex items-center justify-center border border-red-200 shadow-sm">
            <span class="material-symbols-outlined text-red-600 text-lg">warehouse</span>
          </div>
          <h1 class="text-lg font-bold tracking-tight text-gray-900">BranchOS</h1>
          <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-gray-100 text-gray-600 border border-gray-200 tracking-wider">PRO</span>
        </div>

        <!-- Links -->
        <nav class="hidden md:flex gap-8 h-full">
          <a href="#" @click.prevent="currentTab = 'overview'" class="relative flex items-center text-sm font-semibold h-full transition-colors" :class="currentTab === 'overview' ? 'text-gray-900' : 'text-gray-500 hover:text-gray-900'">
            Tổng quan
            <div v-if="currentTab === 'overview'" class="absolute bottom-0 left-0 w-full h-[2px] bg-red-600"></div>
          </a>
          <a href="#" @click.prevent="currentTab = 'users'" class="relative flex items-center text-sm font-semibold h-full transition-colors" :class="currentTab === 'users' ? 'text-gray-900' : 'text-gray-500 hover:text-gray-900'">
            Người dùng
            <div v-if="currentTab === 'users'" class="absolute bottom-0 left-0 w-full h-[2px] bg-red-600"></div>
          </a>
          <a href="#" @click.prevent="currentTab = 'branches'" class="relative flex items-center text-sm font-semibold h-full transition-colors" :class="currentTab === 'branches' ? 'text-gray-900' : 'text-gray-500 hover:text-gray-900'">
            Chi nhánh
            <div v-if="currentTab === 'branches'" class="absolute bottom-0 left-0 w-full h-[2px] bg-red-600"></div>
          </a>
          <a href="#" class="flex items-center text-sm font-semibold h-full text-gray-300 cursor-not-allowed">Kho hàng</a>
          <a href="#" class="flex items-center text-sm font-semibold h-full text-gray-300 cursor-not-allowed">Cài đặt</a>
        </nav>

        <!-- Right actions -->
        <div class="flex items-center gap-5">
          <div class="relative hidden lg:block">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">search</span>
            <input type="text" placeholder="Tìm kiếm hệ thống..." class="w-64 h-9 pl-9 pr-12 bg-gray-50 border border-gray-200 rounded-full text-sm text-gray-800 focus:bg-white focus:border-red-300 focus:ring-2 focus:ring-red-100 outline-none transition-all placeholder:text-gray-400" />
            <kbd class="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] font-mono bg-white border border-gray-200 px-1.5 py-0.5 rounded text-gray-500 pointer-events-none shadow-sm">⌘K</kbd>
          </div>
          
          <button class="w-9 h-9 rounded-full bg-white border border-gray-200 flex items-center justify-center text-gray-500 hover:text-gray-900 hover:bg-gray-50 transition-all relative group shadow-sm">
            <span class="material-symbols-outlined text-xl group-hover:scale-110 transition-transform">notifications</span>
            <span class="absolute top-2 right-2 w-2 h-2 rounded-full bg-red-500 border-2 border-white"></span>
          </button>

          <div class="w-[1px] h-5 bg-gray-200"></div>

          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-full bg-red-100 flex items-center justify-center text-red-600 font-bold text-sm border border-red-200">
              AD
            </div>
            <div class="hidden sm:flex flex-col">
              <span class="text-sm font-bold text-gray-900 leading-tight">Admin</span>
              <span class="text-[10px] text-gray-500 uppercase tracking-wider">Quản trị viên</span>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 max-w-[1600px] w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
      
      <div v-if="currentTab === 'overview'" class="flex flex-col gap-8 fade-in">
        <!-- Page Header -->
        <div class="flex justify-between items-end">
          <div>
            <h2 class="text-3xl font-bold text-gray-900 tracking-tight mb-2">Tổng Quan Hệ Thống</h2>
            <p class="text-gray-500 text-sm flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-green-500"></span>
              Hệ thống đang hoạt động ổn định
            </p>
          </div>
          <div class="flex gap-3">
            <button class="px-4 py-2 rounded-lg bg-white border border-gray-200 text-gray-700 text-sm font-semibold hover:bg-gray-50 transition-colors flex items-center gap-2 shadow-sm">
              Xuất báo cáo
            </button>
            <button class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-semibold shadow-md hover:shadow-lg transition-all flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px]">add</span> Tạo phiếu mới
            </button>
          </div>
        </div>

        <!-- KPI Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Card 1 -->
          <div class="glass-panel p-5 relative overflow-hidden group hover:-translate-y-1 transition-transform duration-300">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-gray-500 tracking-wider uppercase">Giá trị tồn kho</span>
              <div class="w-8 h-8 rounded-lg bg-gray-50 flex items-center justify-center text-gray-500 border border-gray-200"><span class="material-symbols-outlined text-lg">payments</span></div>
            </div>
            <div class="text-3xl font-bold text-gray-900 font-mono tracking-tight mb-2">1.428T ₫</div>
            <div class="text-xs font-medium text-green-600 flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">trending_up</span> +12.4% so với tháng trước
            </div>
          </div>
          
          <!-- Card 2 -->
          <div class="glass-panel p-5 relative overflow-hidden group hover:-translate-y-1 transition-transform duration-300">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-gray-500 tracking-wider uppercase">Lô hàng hoạt động</span>
              <div class="w-8 h-8 rounded-lg bg-gray-50 flex items-center justify-center text-gray-500 border border-gray-200"><span class="material-symbols-outlined text-lg">inventory_2</span></div>
            </div>
            <div class="text-3xl font-bold text-gray-900 font-mono tracking-tight mb-2">284</div>
            <div class="text-xs font-medium text-gray-500">Thuộc 42 danh mục</div>
          </div>

          <!-- Card 3 -->
          <div class="glass-panel p-5 relative overflow-hidden group hover:-translate-y-1 transition-transform duration-300 border-orange-200 bg-orange-50/30">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-orange-600 tracking-wider uppercase">Chờ phê duyệt</span>
              <div class="w-8 h-8 rounded-lg bg-orange-100 flex items-center justify-center text-orange-600 border border-orange-200"><span class="material-symbols-outlined text-lg">pending_actions</span></div>
            </div>
            <div class="text-3xl font-bold text-orange-600 font-mono tracking-tight mb-2">14</div>
            <div class="text-xs font-medium text-orange-600 flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">warning</span> 5 phiếu xử lý gấp
            </div>
          </div>

          <!-- Card 4 -->
          <div class="glass-panel p-5 relative overflow-hidden group hover:-translate-y-1 transition-transform duration-300">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-gray-500 tracking-wider uppercase">Tổng nợ NCC</span>
              <div class="w-8 h-8 rounded-lg bg-gray-50 flex items-center justify-center text-gray-500 border border-gray-200"><span class="material-symbols-outlined text-lg">account_balance</span></div>
            </div>
            <div class="text-3xl font-bold text-gray-900 font-mono tracking-tight mb-2">312M ₫</div>
            <div class="text-xs font-medium text-red-600 flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">error</span> 5 đối tác quá hạn
            </div>
          </div>
        </div>

        <!-- Charts Grid -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Area Chart (Large) -->
          <div class="glass-panel bg-white p-6 lg:col-span-2 flex flex-col">
            <div class="flex justify-between items-center mb-6">
              <h3 class="text-base font-bold text-gray-900">Doanh Thu & Chi Phí Nhập Kho</h3>
              <div class="flex items-center gap-4 text-xs font-medium text-gray-500">
                <div class="flex items-center gap-2"><span class="w-2 h-2 rounded-full bg-red-500"></span>Nhập kho</div>
                <div class="flex items-center gap-2"><span class="w-2 h-2 rounded-full bg-gray-300"></span>Xuất kho</div>
              </div>
            </div>
            
            <div class="flex-1 w-full relative min-h-[240px]">
              <svg viewBox="0 0 600 220" class="w-full h-full overflow-visible">
                <defs>
                  <linearGradient id="grad-light-red" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#ef4444" stop-opacity="0.2"/>
                    <stop offset="100%" stop-color="#ef4444" stop-opacity="0.0"/>
                  </linearGradient>
                </defs>
                <!-- Grids -->
                <line x1="40" y1="20" x2="580" y2="20" stroke="#f4f4f5" stroke-width="1"/>
                <line x1="40" y1="70" x2="580" y2="70" stroke="#f4f4f5" stroke-width="1"/>
                <line x1="40" y1="120" x2="580" y2="120" stroke="#f4f4f5" stroke-width="1"/>
                <line x1="40" y1="170" x2="580" y2="170" stroke="#f4f4f5" stroke-width="1"/>
                
                <!-- Axis text -->
                <g class="font-mono text-[10px] fill-gray-400 font-medium">
                  <text x="30" y="24" text-anchor="end">400M</text>
                  <text x="30" y="74" text-anchor="end">250M</text>
                  <text x="30" y="124" text-anchor="end">100M</text>
                  <text x="30" y="174" text-anchor="end">0M</text>
                  
                  <text x="80" y="195" text-anchor="middle">T1</text>
                  <text x="170" y="195" text-anchor="middle">T2</text>
                  <text x="260" y="195" text-anchor="middle">T3</text>
                  <text x="350" y="195" text-anchor="middle">T4</text>
                  <text x="440" y="195" text-anchor="middle">T5</text>
                  <text x="530" y="195" text-anchor="middle">T6</text>
                </g>

                <!-- Area & Line -->
                <path d="M 80,150 Q 125,100 170,120 T 260,60 T 350,90 T 440,40 T 530,30 L 530,170 L 80,170 Z" fill="url(#grad-light-red)"/>
                <path d="M 80,150 Q 125,100 170,120 T 260,60 T 350,90 T 440,40 T 530,30" fill="none" stroke="#ef4444" stroke-width="2"/>
                
                <path d="M 80,165 Q 125,130 170,140 T 260,110 T 350,130 T 440,70 T 530,55" fill="none" stroke="#d4d4d8" stroke-width="2" stroke-dasharray="4 4"/>

                <!-- Points -->
                <g class="cursor-pointer">
                  <circle cx="80" cy="150" r="4" fill="#fff" stroke="#ef4444" stroke-width="2" class="hover:r-5 transition-all"/>
                  <circle cx="170" cy="120" r="4" fill="#fff" stroke="#ef4444" stroke-width="2" class="hover:r-5 transition-all"/>
                  <circle cx="260" cy="60" r="4" fill="#fff" stroke="#ef4444" stroke-width="2" class="hover:r-5 transition-all"/>
                  <circle cx="350" cy="90" r="4" fill="#fff" stroke="#ef4444" stroke-width="2" class="hover:r-5 transition-all"/>
                  <circle cx="440" cy="40" r="4" fill="#fff" stroke="#ef4444" stroke-width="2" class="hover:r-5 transition-all"/>
                  <circle cx="530" cy="30" r="4" fill="#fff" stroke="#ef4444" stroke-width="2" class="hover:r-5 transition-all"/>
                </g>
              </svg>
            </div>
          </div>

          <!-- Doughnut Chart -->
          <div class="glass-panel bg-white p-6 flex flex-col items-center">
            <h3 class="text-base font-bold text-gray-900 mb-6 w-full text-left">Tỷ Trọng Tồn Kho</h3>
            <div class="relative w-40 h-40 flex items-center justify-center">
              <svg viewBox="0 0 180 180" class="w-full h-full -rotate-90">
                <circle cx="90" cy="90" r="70" fill="none" stroke="#ef4444" stroke-width="20" stroke-dasharray="176 440" stroke-dashoffset="0" class="cursor-pointer"/>
                <circle cx="90" cy="90" r="70" fill="none" stroke="#d4d4d8" stroke-width="20" stroke-dasharray="154 440" stroke-dashoffset="-176" class="cursor-pointer"/>
                <circle cx="90" cy="90" r="70" fill="none" stroke="#f4f4f5" stroke-width="20" stroke-dasharray="110 440" stroke-dashoffset="-330" class="cursor-pointer"/>
              </svg>
              <div class="absolute inset-0 flex flex-col items-center justify-center">
                <span class="text-[10px] font-bold text-gray-400 tracking-wider">TỔNG TỒN</span>
                <span class="text-xl font-bold text-gray-900 font-mono">1.428T</span>
              </div>
            </div>
            
            <div class="w-full mt-8 space-y-3">
              <div class="flex justify-between text-sm items-center">
                <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-sm bg-red-500"></span><span class="text-gray-700">Hà Nội</span></div>
                <span class="font-mono font-bold text-gray-900">40%</span>
              </div>
              <div class="flex justify-between text-sm items-center">
                <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-sm bg-gray-300"></span><span class="text-gray-700">TP.HCM</span></div>
                <span class="font-mono font-bold text-gray-900">35%</span>
              </div>
              <div class="flex justify-between text-sm items-center">
                <div class="flex items-center gap-2"><span class="w-3 h-3 rounded-sm bg-gray-100 border border-gray-200"></span><span class="text-gray-700">Đà Nẵng</span></div>
                <span class="font-mono font-bold text-gray-900">25%</span>
              </div>
            </div>
          </div>
        </div>

      </div>

      <div v-else-if="currentTab === 'users'" class="flex-1 w-full fade-in">
        <UserManagement />
      </div>

      <div v-else-if="currentTab === 'branches'" class="flex-1 w-full fade-in">
        <BranchManagement />
      </div>

    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import UserManagement from './UserManagement.vue';
import BranchManagement from './BranchManagement.vue';

const emit = defineEmits(['logout']);
const currentTab = ref('overview');
</script>

<style scoped>
.fade-in {
  animation: fadeIn 0.4s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
