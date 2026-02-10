<template>
  <div class="auth-carousel">
    <div class="auth-carousel__content">
      <transition name="fade" mode="out-in">
        <div :key="currentIndex" class="auth-carousel__slide">
          <h2 class="auth-carousel__title">{{ currentSlide.title }}</h2>
          <p class="auth-carousel__desc">{{ currentSlide.desc }}</p>
        </div>
      </transition>
    </div>
    
    <div class="auth-carousel__indicators">
      <span 
        v-for="(slide, index) in slides" 
        :key="index"
        class="auth-carousel__dot"
        :class="{ 'is-active': currentIndex === index }"
        @click="setSlide(index)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'

const slides = [
  {
    title: '寻找你的完美队友',
    desc: '基于智能算法和技能匹配，为您推荐最合适的项目合作伙伴。打破信息壁垒，让创意落地。'
  },
  {
    title: '实时沟通协作',
    desc: '内置即时通讯与团队协作工具，无缝连接每一个灵感瞬间。支持文件共享、任务管理与实时讨论。'
  },
  {
    title: '展示个人才华',
    desc: '打造专业的个人技能档案，量化您的项目贡献与能力。让每一次合作都成为您简历上的高光时刻。'
  }
]

const currentIndex = ref(0)
const currentSlide = computed(() => slides[currentIndex.value])
let timer: number | undefined

const startTimer = () => {
  timer = window.setInterval(() => {
    currentIndex.value = (currentIndex.value + 1) % slides.length
  }, 5000)
}

const setSlide = (index: number) => {
  currentIndex.value = index
  resetTimer()
}

const resetTimer = () => {
  if (timer) clearInterval(timer)
  startTimer()
}

onMounted(() => {
  startTimer()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.auth-carousel {
  position: relative;
  width: 100%;
  max-width: 500px;
  margin-top: 40px;
}

.auth-carousel__content {
  min-height: 160px;
}

.auth-carousel__slide {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.auth-carousel__title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(120deg, var(--text-color), var(--accent-color));
  -webkit-background-clip: text;
  color: transparent;
  letter-spacing: -0.02em;
}

.auth-carousel__desc {
  font-size: 16px;
  line-height: 1.6;
  color: var(--text-color-muted);
  margin: 0;
}

.auth-carousel__indicators {
  display: flex;
  gap: 8px;
  margin-top: 24px;
}

.auth-carousel__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-color-muted);
  opacity: 0.2;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &.is-active {
    width: 24px;
    border-radius: 4px;
    background: var(--accent-color);
    opacity: 1;
  }
  
  &:hover:not(.is-active) {
    opacity: 0.5;
  }
}

// Transitions
.fade-enter-active,
.fade-leave-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
