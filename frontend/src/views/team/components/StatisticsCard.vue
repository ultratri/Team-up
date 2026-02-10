<template>
  <div 
    class="statistics-card"
    tabindex="0"
    role="article"
    :aria-label="`${label}: ${formattedValue}`"
    @keydown.enter="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <div class="card-icon" :style="{ background: iconBackground }" role="img" :aria-label="`${label} 图标`">
      <component :is="icon" class="icon" />
    </div>
    <div class="card-content">
      <div class="card-value" aria-live="polite">{{ formattedValue }}</div>
      <div class="card-label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'

interface Props {
  icon: Component
  value: number | string
  label: string
  iconBackground?: string
  suffix?: string
}

const props = withDefaults(defineProps<Props>(), {
  iconBackground: 'linear-gradient(135deg, var(--accent-color), var(--accent-color-dark))',
  suffix: ''
})

// Emits
const emit = defineEmits<{
  click: []
}>()

// Handle click event
const handleClick = () => {
  emit('click')
}

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    // 格式化数字，添加千位分隔符
    const formatted = props.value.toLocaleString()
    return props.suffix ? `${formatted}${props.suffix}` : formatted
  }
  return props.value
})
</script>

<style scoped lang="scss">
.statistics-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-card);
  box-shadow: var(--shadow-card);
  transition: all 0.3s ease;
  cursor: pointer;
  outline: none;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-card-hover);
  }

  &:focus {
    outline: 2px solid var(--accent-color);
    outline-offset: 2px;
  }

  &:active {
    transform: translateY(0);
  }

  .card-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 12px;
    flex-shrink: 0;

    .icon {
      width: 28px;
      height: 28px;
      color: #fff;
    }
  }

  .card-content {
    flex: 1;
    min-width: 0;

    .card-value {
      font-size: 24px;
      font-weight: 600;
      color: var(--text-color);
      line-height: 1.2;
      margin-bottom: 4px;
    }

    .card-label {
      font-size: 14px;
      color: var(--text-color-muted);
      line-height: 1.4;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .statistics-card {
    padding: 16px;

    .card-icon {
      width: 48px;
      height: 48px;

      .icon {
        width: 24px;
        height: 24px;
      }
    }

    .card-content {
      .card-value {
        font-size: 20px;
      }

      .card-label {
        font-size: 13px;
      }
    }
  }
}
</style>
