<script setup lang="ts">
const props = defineProps<{
  value: string
  type?: 'role' | 'status' | 'transfer'
}>()

// Role badges
const roleConfig: Record<string, { label: string; cls: string }> = {
  ADMIN: { label: 'Admin', cls: 'bg-blue-100 text-blue-700 border-blue-200' },
  MANAGER: { label: 'Manager', cls: 'bg-teal-100 text-teal-700 border-teal-200' },
  STAFF: { label: 'Nhân viên', cls: 'bg-slate-100 text-slate-600 border-slate-200' },
}

// Status badges
const statusConfig: Record<string, { label: string; cls: string; dot: string }> = {
  ACTIVE: { label: 'Hoạt động', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  INACTIVE: { label: 'Ngừng HĐ', cls: 'bg-slate-100 text-slate-500 border-slate-200', dot: 'bg-slate-400' },
}

// Transfer request status
const transferConfig: Record<string, { label: string; cls: string }> = {
  PENDING_STAFF: { label: 'Chờ NV xác nhận', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  STAFF_CONFIRMED: { label: 'NV đã xác nhận', cls: 'bg-blue-50 text-blue-700 border-blue-200' },
  MANAGER_APPROVED: { label: 'Manager duyệt', cls: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  APPROVED: { label: 'Hoàn tất', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  REJECTED: { label: 'Từ chối', cls: 'bg-red-50 text-red-700 border-red-200' },
  CANCELLED: { label: 'Đã hủy', cls: 'bg-slate-100 text-slate-500 border-slate-200' },
}

const config = props.type === 'role'
  ? roleConfig[props.value] ?? { label: props.value, cls: 'bg-slate-100 text-slate-600 border-slate-200' }
  : props.type === 'transfer'
  ? transferConfig[props.value] ?? { label: props.value, cls: 'bg-slate-100 text-slate-600 border-slate-200' }
  : statusConfig[props.value] ?? { label: props.value, cls: 'bg-slate-100 text-slate-600 border-slate-200' }

const dotColor = (statusConfig[props.value] as any)?.dot
</script>

<template>
  <span
    :class="['inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border', (config as any).cls]"
  >
    <span
      v-if="type !== 'role' && type !== 'transfer' && dotColor"
      :class="['w-1.5 h-1.5 rounded-full flex-shrink-0', dotColor]"
    />
    {{ (config as any).label }}
  </span>
</template>
