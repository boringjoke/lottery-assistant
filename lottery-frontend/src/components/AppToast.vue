<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'

type ToastType = 'success' | 'error' | 'info'

const props = withDefaults(defineProps<{
  message: string
  type?: ToastType
  duration?: number
}>(), {
  type: 'info',
  duration: 2200,
})

const emit = defineEmits<{
  close: []
}>()

let closeTimer: ReturnType<typeof setTimeout> | null = null

const visible = computed(() => props.message.trim().length > 0)
const role = computed(() => props.type === 'error' ? 'alert' : 'status')
const iconText = computed(() => {
  if (props.type === 'success') {
    return '✓'
  }
  if (props.type === 'error') {
    return '!'
  }

  return 'i'
})

function clearCloseTimer() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}

function closeToast() {
  clearCloseTimer()
  emit('close')
}

watch(
  () => props.message,
  (message) => {
    clearCloseTimer()
    if (!message.trim() || props.duration <= 0) {
      return
    }

    closeTimer = setTimeout(() => {
      emit('close')
    }, props.duration)
  },
  { immediate: true },
)

onBeforeUnmount(clearCloseTimer)
</script>

<template>
  <Transition name="toast-fade">
    <div v-if="visible" class="app-toast" :class="`app-toast--${type}`" :role="role">
      <span class="app-toast__icon" aria-hidden="true">{{ iconText }}</span>
      <span class="app-toast__message">{{ message }}</span>
      <button class="app-toast__close" type="button" aria-label="关闭提示" @click="closeToast">×</button>
    </div>
  </Transition>
</template>

<style scoped>
.app-toast {
  position: fixed;
  top: 82px;
  left: 50%;
  z-index: 1200;
  display: flex;
  max-width: min(420px, calc(100vw - 32px));
  min-height: 44px;
  align-items: center;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
  box-shadow: 0 18px 42px rgb(15 23 42 / 0.16);
  color: #0f172a;
  transform: translateX(-50%);
}

.app-toast--success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.app-toast--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.app-toast--info {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.app-toast__icon {
  display: inline-flex;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #ffffff;
  color: currentColor;
  font-size: 13px;
  font-weight: 900;
  box-shadow: inset 0 0 0 1px currentColor;
}

.app-toast__message {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: 14px;
  font-weight: 900;
}

.app-toast__close {
  display: inline-flex;
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: currentColor;
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
  opacity: 0.72;
}

.app-toast__close:hover {
  background: rgb(255 255 255 / 0.72);
  opacity: 1;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition:
    opacity 0.18s,
    transform 0.18s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}

@media (max-width: 640px) {
  .app-toast {
    top: 16px;
  }
}
</style>
