<script setup lang="ts">
import { computed } from 'vue'
import { useSystemStore } from '@/store/system'

const systemStore = useSystemStore()

// 从站点名称生成缩写（取前两个字的首字母）
const brandMark = computed(() => {
  const name = systemStore.siteName
  if (!name) return 'TU'
  
  // 如果是英文，取前两个单词的首字母
  if (/^[A-Za-z\s]+$/.test(name)) {
    const words = name.trim().split(/\s+/)
    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase()
    }
    return name.substring(0, 2).toUpperCase()
  }
  
  // 如果是中文，取前两个字
  return name.substring(0, 2)
})
</script>

<template>
  <div class="auth-brand">
    <div class="auth-brand__mark">{{ brandMark }}</div>
    <div class="auth-brand__text">
      <span class="auth-brand__title">{{ systemStore.siteName }}</span>
      <span class="auth-brand__subtitle">{{ systemStore.siteDescription }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: default;
}

.auth-brand__mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: radial-gradient(circle at 20% 0%, #f8fafc, transparent 60%),
    radial-gradient(circle at 100% 100%, var(--accent-color), #1d4ed8);
  box-shadow: 
    0 10px 25px -5px rgba(37, 99, 235, 0.6), 
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #fff;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
  
  &::after {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent 45%, rgba(255,255,255,0.2) 50%, transparent 55%);
    transform: rotate(45deg) translateY(100%);
    transition: transform 0.6s;
  }
}

.auth-brand:hover .auth-brand__mark {
  transform: scale(1.1) rotate(3deg);
  box-shadow: 
    0 14px 35px -5px rgba(37, 99, 235, 0.75), 
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
    
  &::after {
    transform: rotate(45deg) translateY(-100%);
  }
}

.auth-brand__text {
  display: flex;
  flex-direction: column;
}

.auth-brand__title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-color);
}

.auth-brand__subtitle {
  font-size: 13px;
  color: var(--text-color-muted);
  font-weight: 500;
}
</style>
