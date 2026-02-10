<template>
  <div class="loading-spinner" :class="{ 'loading-spinner--small': size === 'small', 'loading-spinner--large': size === 'large' }">
    <div class="spinner">
      <div class="spinner-circle"></div>
      <div class="spinner-circle"></div>
      <div class="spinner-circle"></div>
    </div>
    <p v-if="text" class="loading-text">{{ text }}</p>
  </div>
</template>

<script setup lang="ts">
interface Props {
  size?: 'small' | 'medium' | 'large'
  text?: string
}

withDefaults(defineProps<Props>(), {
  size: 'medium',
  text: ''
})
</script>

<style scoped lang="scss">
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  padding: var(--spacing-xl);
}

.spinner {
  display: flex;
  gap: 8px;
  
  .loading-spinner--small & {
    gap: 4px;
  }
  
  .loading-spinner--large & {
    gap: 12px;
  }
}

.spinner-circle {
  width: 12px;
  height: 12px;
  background: var(--accent-color);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
  
  .loading-spinner--small & {
    width: 8px;
    height: 8px;
  }
  
  .loading-spinner--large & {
    width: 16px;
    height: 16px;
  }
  
  &:nth-child(1) {
    animation-delay: -0.32s;
  }
  
  &:nth-child(2) {
    animation-delay: -0.16s;
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.loading-text {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-muted);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
