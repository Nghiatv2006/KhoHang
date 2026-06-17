<script setup lang="ts">
defineProps<{
  show: boolean
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div
        v-if="show"
        class="fixed inset-0 z-[1000] flex items-center justify-center"
        style="background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);"
        @click.self="emit('cancel')"
      >
        <Transition name="dialog">
          <div
            v-if="show"
            class="bg-white rounded-2xl shadow-2xl mx-auto overflow-hidden"
            style="width: 100%; max-width: 400px;"
          >
            <!-- Icon -->
            <div class="flex flex-col items-center pt-8 pb-4 px-6 text-center">
              <div
                :class="['w-14 h-14 rounded-full flex items-center justify-center mb-4', danger ? 'bg-red-50' : 'bg-blue-50']"
              >
                <span
                  :class="['material-symbols-outlined text-3xl', danger ? 'text-red-500' : 'text-blue-500']"
                >
                  {{ danger ? 'delete_forever' : 'help' }}
                </span>
              </div>
              <h3 class="text-lg font-semibold text-slate-800 mb-1">{{ title || 'Xác nhận' }}</h3>
              <p class="text-sm text-slate-500 leading-5">{{ message }}</p>
            </div>

            <!-- Actions -->
            <div class="flex gap-3 px-6 pb-6">
              <button
                class="flex-1 h-10 rounded-lg border border-slate-200 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors"
                @click="emit('cancel')"
              >
                {{ cancelText || 'Hủy' }}
              </button>
              <button
                :class="['flex-1 h-10 rounded-lg text-sm font-semibold text-white transition-colors', danger ? 'bg-red-500 hover:bg-red-600' : 'bg-[#0052cc] hover:bg-[#003d9b]']"
                @click="emit('confirm')"
              >
                {{ confirmText || 'Xác nhận' }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.overlay-enter-active, .overlay-leave-active { transition: opacity 0.2s; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }
.dialog-enter-active, .dialog-leave-active { transition: all 0.2s ease; }
.dialog-enter-from, .dialog-leave-to { opacity: 0; transform: scale(0.95); }
</style>
