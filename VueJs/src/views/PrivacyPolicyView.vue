<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const goBack = () => router.push('/login')
</script>

<template>
  <div class="min-h-screen bg-slate-50 py-12 px-4 sm:px-6 lg:px-8 font-sans text-slate-800">
    <div class="max-w-3xl mx-auto bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden transform transition-all duration-300 hover:shadow-md">
      <!-- Header -->
      <div class="bg-gradient-to-r from-[#0047b3] to-indigo-700 px-8 py-12 text-white relative overflow-hidden">
        <!-- Abstract Background Shapes -->
        <div class="absolute -top-20 -right-20 w-64 h-64 bg-white/10 rounded-full blur-3xl"></div>
        <div class="absolute -bottom-20 -left-20 w-64 h-64 bg-blue-400/20 rounded-full blur-3xl"></div>
        
        <div class="relative z-10 flex flex-col items-center text-center">
          <div class="w-16 h-16 bg-white/10 backdrop-blur-md rounded-2xl flex items-center justify-center mb-6 shadow-lg border border-white/20">
            <span class="material-symbols-outlined text-4xl" style="font-variation-settings: 'FILL' 1">shield_locked</span>
          </div>
          <h1 class="text-[32px] font-bold tracking-tight mb-2">Chính Sách Bảo Mật</h1>
          <p class="text-blue-100/80 font-medium text-sm tracking-wide uppercase">Cập nhật lần cuối: 16/06/2026</p>
        </div>
      </div>

      <!-- Content -->
      <div class="p-8 sm:p-12">
        <p class="text-lg text-slate-600 font-medium mb-10 leading-relaxed border-l-4 border-blue-500 pl-4 bg-blue-50/50 py-2 rounded-r-lg">
          Chào mừng bạn đến với Warehouse_Management. Việc bảo vệ dữ liệu cá nhân và thông tin hoạt động doanh nghiệp của bạn là ưu tiên hàng đầu và là nguyên tắc cốt lõi của chúng tôi.
        </p>

        <div class="space-y-8">
          <!-- Section 1 -->
          <section>
            <h2 class="text-xl font-bold text-slate-800 mb-3 flex items-center gap-2.5">
              <span class="material-symbols-outlined text-[#0047b3] bg-blue-50 p-1.5 rounded-lg">database</span>
              1. Thu thập thông tin
            </h2>
            <p class="text-slate-600 leading-relaxed pl-11">
              Chúng tôi thu thập các thông tin cần thiết nhằm mục đích vận hành và tối ưu hóa hệ thống kho bãi, bao gồm: Họ và tên, địa chỉ email, số điện thoại nội bộ, chi nhánh trực thuộc và mọi nhật ký hoạt động xuất nhập tồn kho do người dùng cung cấp.
            </p>
          </section>

          <!-- Section 2 -->
          <section>
            <h2 class="text-xl font-bold text-slate-800 mb-3 flex items-center gap-2.5">
              <span class="material-symbols-outlined text-[#0047b3] bg-blue-50 p-1.5 rounded-lg">lock</span>
              2. Tiêu chuẩn bảo mật dữ liệu
            </h2>
            <p class="text-slate-600 leading-relaxed pl-11">
              Mọi dữ liệu nghiệp vụ trên hệ thống Warehouse_Management đều được mã hóa chặt chẽ theo tiêu chuẩn <strong>256-bit AES</strong> tại máy chủ lưu trữ. Chúng tôi sử dụng giao thức kết nối an toàn (HTTPS/TLS 1.3) để đảm bảo thông tin nội bộ của doanh nghiệp tuyệt đối không bị đánh cắp hay rò rỉ.
            </p>
          </section>

          <!-- Section 3 -->
          <section>
            <h2 class="text-xl font-bold text-slate-800 mb-3 flex items-center gap-2.5">
              <span class="material-symbols-outlined text-[#0047b3] bg-blue-50 p-1.5 rounded-lg">security</span>
              3. Phân quyền và Chia sẻ
            </h2>
            <p class="text-slate-600 leading-relaxed pl-11">
              Hệ thống tuân thủ nguyên tắc "Không tin cậy" (Zero Trust). Tuyệt đối không chia sẻ, bán hoặc trao đổi thông tin dữ liệu cho bất kỳ bên thứ ba nào. Dữ liệu chỉ được hiển thị và truy cập bởi nhân viên và cấp quản lý nội bộ tùy theo mức độ phân quyền tài khoản (Admin, Manager, Staff).
            </p>
          </section>

          <!-- Section 4 -->
          <section>
            <h2 class="text-xl font-bold text-slate-800 mb-3 flex items-center gap-2.5">
              <span class="material-symbols-outlined text-[#0047b3] bg-blue-50 p-1.5 rounded-lg">cookie</span>
              4. Cookie và Theo dõi phiên làm việc
            </h2>
            <p class="text-slate-600 leading-relaxed pl-11">
              Nhằm duy trì trạng thái xác thực bảo mật an toàn, hệ thống sử dụng cookie phiên (session cookies) có mã hóa. Bằng việc "Duy trì đăng nhập", bạn đồng ý lưu trữ chứng chỉ điện tử an toàn trên thiết bị của mình.
            </p>
          </section>
        </div>
      </div>

      <!-- Footer / Action -->
      <div class="bg-slate-50 px-8 py-8 border-t border-slate-100 flex justify-center">
        <button 
          @click="goBack" 
          class="flex items-center gap-2 bg-white border border-slate-300 text-slate-700 px-6 py-2.5 rounded-lg font-semibold hover:bg-slate-100 hover:text-[#0047b3] hover:border-[#0047b3]/30 transition-all shadow-sm"
        >
          <span class="material-symbols-outlined text-[20px]">arrow_back</span>
          Trở về màn hình Đăng nhập
        </button>
      </div>
    </div>
  </div>
</template>
