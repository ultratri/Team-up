<script setup lang="ts">
// 主应用组件
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from './store/theme'

const themeStore = useThemeStore()
const router = useRouter()
const transitionName = ref('slide-fade')

onMounted(() => {
  themeStore.initTheme()
})

// 根据路由变化动态选择过渡动画
watch(() => router.currentRoute.value, (to, from) => {
  if (!from) {
    transitionName.value = 'fade'
    return
  }
  
  // 根据路由元信息选择动画
  if (to.meta.transition) {
    transitionName.value = to.meta.transition as string
  } else {
    // 默认使用滑动淡入效果
    transitionName.value = 'slide-fade'
  }
})
</script>

<template>
  <div id="app">
    <router-view v-slot="{ Component, route }">
      <transition :name="transitionName" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </div>
</template>

<style>
/* 全局过渡动画样式 - 不能使用 scoped，否则无法应用到 router-view 子组件 */
#app {
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  overflow-y: auto;
}

/* ==================== 基础动画 ==================== */

/* 淡入淡出 - Fade */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ==================== 滑动动画 ==================== */

/* 滑动淡入 - Slide Fade (默认) */
.slide-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 1, 1);
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 向左滑动 - Slide Left */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-100%);
}

/* 向右滑动 - Slide Right */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-100%);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

/* ==================== PPT风格动画 ==================== */

/* PPT推入效果 - Push */
.push-enter-active,
.push-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  position: absolute;
  width: 100%;
}

.push-enter-from {
  transform: translateX(100%);
}

.push-leave-to {
  transform: translateX(-30%);
  opacity: 0.5;
}

/* PPT擦除效果 - Wipe */
.wipe-enter-active,
.wipe-leave-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.wipe-enter-from {
  clip-path: inset(0 100% 0 0);
}

.wipe-leave-to {
  clip-path: inset(0 0 0 100%);
}

/* PPT翻页效果 - Flip */
.flip-enter-active,
.flip-leave-active {
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  transform-style: preserve-3d;
  backface-visibility: hidden;
}

.flip-enter-from {
  transform: perspective(1000px) rotateY(-90deg);
  opacity: 0;
}

.flip-leave-to {
  transform: perspective(1000px) rotateY(90deg);
  opacity: 0;
}

/* PPT立方体旋转 - Cube */
.cube-enter-active,
.cube-leave-active {
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  transform-style: preserve-3d;
}

.cube-enter-from {
  transform: perspective(1200px) rotateY(-90deg) translateZ(50%);
  opacity: 0;
}

.cube-leave-to {
  transform: perspective(1200px) rotateY(90deg) translateZ(-50%);
  opacity: 0;
}

/* PPT缩放效果 - Zoom */
.zoom-enter-active,
.zoom-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.zoom-enter-from {
  opacity: 0;
  transform: scale(0.8);
}

.zoom-leave-to {
  opacity: 0;
  transform: scale(1.2);
}

/* PPT溶解效果 - Dissolve */
.dissolve-enter-active,
.dissolve-leave-active {
  transition: all 0.5s ease;
}

.dissolve-enter-from {
  opacity: 0;
  filter: blur(10px);
}

.dissolve-leave-to {
  opacity: 0;
  filter: blur(10px);
}

/* ==================== 3D效果 ==================== */

/* 3D翻转 - Rotate 3D */
.rotate-3d-enter-active,
.rotate-3d-leave-active {
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  transform-style: preserve-3d;
}

.rotate-3d-enter-from {
  opacity: 0;
  transform: perspective(1000px) rotateX(-45deg) translateY(50px);
}

.rotate-3d-leave-to {
  opacity: 0;
  transform: perspective(1000px) rotateX(45deg) translateY(-50px);
}

/* 翻转卡片 - Flip Card */
.flip-card-enter-active,
.flip-card-leave-active {
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  transform-style: preserve-3d;
}

.flip-card-enter-from {
  opacity: 0;
  transform: perspective(1000px) rotateY(180deg) scale(0.8);
}

.flip-card-leave-to {
  opacity: 0;
  transform: perspective(1000px) rotateY(-180deg) scale(0.8);
}

/* ==================== 弹性效果 ==================== */

/* 弹跳进入 - Bounce */
.bounce-enter-active {
  animation: bounce-in 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.bounce-leave-active {
  animation: bounce-out 0.4s cubic-bezier(0.4, 0, 1, 1);
}

@keyframes bounce-in {
  0% {
    opacity: 0;
    transform: scale(0.3) translateY(50px);
  }
  50% {
    transform: scale(1.05) translateY(-10px);
  }
  70% {
    transform: scale(0.98) translateY(5px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes bounce-out {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(0.8) translateY(30px);
  }
}

/* ==================== 特殊效果 ==================== */

/* 百叶窗效果 - Blinds */
.blinds-enter-active,
.blinds-leave-active {
  transition: all 0.5s steps(5);
}

.blinds-enter-from {
  opacity: 0;
  clip-path: polygon(
    0 0, 100% 0,
    100% 20%, 0 20%,
    0 40%, 100% 40%,
    100% 60%, 0 60%,
    0 80%, 100% 80%,
    100% 100%, 0 100%
  );
}

.blinds-leave-to {
  opacity: 0;
}

/* 涟漪效果 - Ripple */
.ripple-enter-active {
  animation: ripple-in 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.ripple-leave-active {
  animation: ripple-out 0.4s cubic-bezier(0.4, 0, 1, 1);
}

@keyframes ripple-in {
  0% {
    opacity: 0;
    transform: scale(0);
    border-radius: 50%;
  }
  50% {
    border-radius: 20%;
  }
  100% {
    opacity: 1;
    transform: scale(1);
    border-radius: 0;
  }
}

@keyframes ripple-out {
  0% {
    opacity: 1;
    transform: scale(1);
    border-radius: 0;
  }
  100% {
    opacity: 0;
    transform: scale(1.2);
    border-radius: 50%;
  }
}

/* ==================== 响应式优化 ==================== */

/* 移动端减少动画复杂度 */
@media (max-width: 768px) {
  .flip-enter-active,
  .flip-leave-active,
  .cube-enter-active,
  .cube-leave-active,
  .rotate-3d-enter-active,
  .rotate-3d-leave-active {
    transition: all 0.3s ease;
  }
  
  .flip-enter-from,
  .cube-enter-from,
  .rotate-3d-enter-from {
    transform: none;
    opacity: 0;
  }
  
  .flip-leave-to,
  .cube-leave-to,
  .rotate-3d-leave-to {
    transform: none;
    opacity: 0;
  }
}

/* 减弱动画偏好设置 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
