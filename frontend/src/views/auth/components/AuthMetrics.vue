<template>
  <div class="auth-metrics">
    <div 
      v-for="(item, index) in metrics" 
      :key="index" 
      class="auth-metric"
      :style="{ animationDelay: `${index * 0.15}s` }"
    >
      <div class="auth-metric__value">
        {{ displayedValues[index] }}<span class="auth-metric__suffix">{{ item.suffix }}</span>
      </div>
      <h3 class="auth-metric__label">{{ item.label }}</h3>
      <p class="auth-metric__desc">{{ item.desc }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'

const metrics = [
  { label: '活跃用户', value: 2000, suffix: '+', desc: '来自各高校的优秀开发者' },
  { label: '成功组队', value: 500, suffix: '+', desc: '孵化出众多优秀项目' },
  { label: '项目完成率', value: 95, suffix: '%', desc: '高效的协作流程保障' }
]

const displayedValues = reactive(metrics.map(() => 0))

const animateValue = (index: number, end: number, duration: number = 2000) => {
  let startTimestamp: number | null = null;
  const step = (timestamp: number) => {
    if (!startTimestamp) startTimestamp = timestamp;
    const progress = Math.min((timestamp - startTimestamp) / duration, 1);
    // Ease out expo
    const easeProgress = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
    
    displayedValues[index] = Math.floor(easeProgress * end);
    
    if (progress < 1) {
      window.requestAnimationFrame(step);
    }
  };
  window.requestAnimationFrame(step);
}

onMounted(() => {
  metrics.forEach((metric, index) => {
    // Stagger the start of number animation slightly after the card appears
    setTimeout(() => {
      animateValue(index, metric.value)
    }, 600 + index * 200)
  })
})
</script>

<style scoped lang="scss">
.auth-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-top: 32px;
}

.auth-metric {
  padding: 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(12px);
  transform-origin: center;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  opacity: 0;
  animation: slideUpFade 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  
  /* 3D Hover Effect */
  &:hover {
    transform: translateY(-8px) scale(1.02) rotateX(2deg);
    background: rgba(255, 255, 255, 0.85);
    box-shadow: 
      0 20px 40px rgba(0, 0, 0, 0.1),
      0 0 0 1px rgba(var(--accent-color-rgb), 0.2);
    z-index: 10;
  }
}

.auth-metric__value {
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--text-color) 0%, var(--accent-color) 100%);
  -webkit-background-clip: text;
  color: transparent;
  font-feature-settings: "tnum";
  font-variant-numeric: tabular-nums;
}

.auth-metric__suffix {
  font-size: 20px;
  opacity: 0.6;
  margin-left: 2px;
  font-weight: 600;
}

.auth-metric__label {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--text-color);
}

.auth-metric__desc {
  font-size: 12px;
  line-height: 1.5;
  margin: 0;
  color: var(--text-color-muted);
}

/* Dark Mode Support */
:global([data-theme='dark']) .auth-metric {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  
  &:hover {
    background: rgba(30, 41, 59, 0.85);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  }
}

@keyframes slideUpFade {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Responsive */
@media (max-width: 1024px) {
  .auth-metrics {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .auth-metric {
    display: flex;
    align-items: center;
    padding: 16px;
    
    &__value {
      margin-bottom: 0;
      margin-right: 16px;
      font-size: 28px;
      min-width: 80px;
    }
    
    &__content {
      flex: 1;
    }
  }
}
</style>
