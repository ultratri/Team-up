<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  description?: string
  actionText?: string
}>()

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon">
      <span v-if="icon" class="icon-text">{{ icon }}</span>
      <svg v-else width="64" height="64" viewBox="0 0 64 64" fill="none">
        <circle cx="32" cy="32" r="30" stroke="currentColor" stroke-width="2" opacity="0.2"/>
        <path d="M32 20v24M20 32h24" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3"/>
      </svg>
    </div>
    
    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-description">{{ description }}</p>
    
    <el-button 
      v-if="actionText" 
      type="primary" 
      @click="emit('action')"
      class="empty-action"
    >
      {{ actionText }}
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  animation: fadeIn 0.5s ease;
  
  .empty-icon {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;
    color: var(--text-color-muted);
    opacity: 0.5;
    
    .icon-text {
      font-size: 48px;
    }
    
    svg {
      filter: grayscale(100%);
    }
  }
  
  .empty-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0 0 12px 0;
  }
  
  .empty-description {
    font-size: 14px;
    color: var(--text-color-muted);
    margin: 0 0 24px 0;
    max-width: 400px;
    line-height: 1.6;
  }
  
  .empty-action {
    margin-top: 8px;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
