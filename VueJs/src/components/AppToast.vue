<script setup lang="ts">
import { useToast } from '../utils/toast'

const { toasts, remove } = useToast()

const iconMap = {
  success: 'check_circle',
  error: 'cancel',
  warning: 'warning',
  info: 'info',
}

const colorMap = {
  success: 'bg-emerald-50 border-emerald-200 text-emerald-800 dark:bg-emerald-900/80 dark:border-emerald-400/50 dark:text-emerald-200',
  error: 'bg-red-50 border-red-200 text-red-800 dark:bg-red-900/80 dark:border-red-400/50 dark:text-red-200',
  warning: 'bg-amber-50 border-amber-200 text-amber-800 dark:bg-amber-900/80 dark:border-amber-400/50 dark:text-amber-200',
  info: 'bg-blue-50 border-blue-200 text-blue-800 dark:bg-blue-900/80 dark:border-blue-400/50 dark:text-blue-200',
}

const iconColorMap = {
  success: 'text-emerald-500 dark:text-emerald-300',
  error: 'text-red-500 dark:text-red-300',
  warning: 'text-amber-500 dark:text-amber-300',
  info: 'text-blue-500 dark:text-blue-300',
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-5 right-5 z-[9999] flex flex-col gap-2 w-80">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="['flex items-start gap-3 px-4 py-3 rounded-xl border shadow-lg text-sm font-medium', colorMap[toast.type]]"
        >
          <span :class="['material-symbols-outlined text-xl flex-shrink-0 mt-0.5', iconColorMap[toast.type]]">
            {{ iconMap[toast.type] }}
          </span>
          <span class="flex-1 leading-5">{{ toast.message }}</span>
          <button
            class="flex-shrink-0 opacity-50 hover:opacity-100 transition-opacity"
            @click="remove(toast.id)"
          >
            <span class="material-symbols-outlined text-base">close</span>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from { opacity: 0; transform: translateX(100%); }
.toast-leave-to { opacity: 0; transform: translateX(100%); }
</style>
