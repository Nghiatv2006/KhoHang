<script setup lang="ts">
defineProps<{
  show: boolean
  title: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
}>()

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div
        v-if="show"
        class="fixed inset-0 z-[1100] flex items-center justify-center p-4"
        style="background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);"
        @click.self="emit('close')"
      >
        <Transition name="dialog">
          <div
            v-if="show"
            class="bg-white rounded-2xl shadow-2xl flex flex-col mx-auto"
            :style="{
              width: '100%',
              maxWidth: size === 'sm' ? '400px' : size === 'lg' ? '800px' : size === 'xl' ? '1140px' : '500px',
              maxHeight: '90vh'
            }"
          >
            <!-- Header -->
            <div class="flex items-center justify-between px-6 py-4 border-b border-slate-100 flex-shrink-0">
              <h2 class="text-base font-semibold text-slate-800">{{ title }}</h2>
              <button
                class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                @click="emit('close')"
              >
                <span class="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <!-- Body -->
            <div class="overflow-y-auto flex-1">
              <slot />
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
.dialog-enter-active, .dialog-leave-active { transition: all 0.22s ease; }
.dialog-enter-from, .dialog-leave-to { opacity: 0; transform: scale(0.96) translateY(-8px); }
</style>
