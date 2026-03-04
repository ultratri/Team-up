<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/store/auth'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import { websocketService } from '@/utils/websocket'
import TeamChatFloating from '@/components/team/TeamChatFloating.vue'

const authStore = useAuthStore()

onMounted(() => {
  // 只有在已登录时才初始化 WebSocket 连接
  // WebSocket 用于实时消息和通知，如果服务未启动也不影响核心功能
  if (authStore.isAuthenticated) {
    try {
      websocketService.connect()
    } catch (error) {
      console.warn('WebSocket 连接失败，将使用轮询方式获取通知')
    }
  }
  
  // 预加载核心视图组件，利用空闲时间下载资源
  const prefetchViews = () => {
    const views = [
      () => import('../views/project/ProjectSquare.vue'),
      () => import('../views/team/TeamList.vue'),
      () => import('../views/team/TeamSpace.vue'),
      () => import('../views/team/TaskBoard.vue'),
      () => import('../views/team/FileShare.vue'),
      () => import('../views/team/Chat.vue'),
      () => import('../views/profile/ProfileView.vue'),
      () => import('../views/message/MessageCenter.vue'),
      () => import('../views/notification/NotificationCenter.vue'),
    ]
    
    views.forEach(importer => {
      importer().catch(() => {})
    })
  }

  // 优先保证首屏渲染，延迟执行预加载
  if ('requestIdleCallback' in window) {
    // @ts-ignore
    window.requestIdleCallback(prefetchViews)
  } else {
    setTimeout(prefetchViews, 2000)
  }
})

onUnmounted(() => {
  // 断开 WebSocket 连接
  websocketService.disconnect()
})
</script>

<template>
  <div class="app-layout">
    <Sidebar />
    <div class="main-container">
      <Header />
      <main class="content-wrapper">
        <router-view v-slot="{ Component, route }">
          <transition :name="route.meta.transition || 'fade'" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
      <!-- 全局悬浮团队聊天窗 -->
      <TeamChatFloating />
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: var(--bg-body);
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.content-wrapper {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  
  /* Custom Scrollbar for content */
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
  }
  &:hover::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.2);
  }
}

/* Transitions */
.fade-slide-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 1, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(1.02);
}

.fade-slide-enter-active .content-wrapper {
  animation: slideInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

<style>
/* 全局过渡动画 - 不使用 scoped，确保能应用到 router-view 子组件 */

/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 滑动淡入 */
.slide-fade-enter-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-fade-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 1, 1);
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}

/* 缩放效果 */
.zoom-enter-active,
.zoom-leave-active {
  transition: all 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.zoom-enter-from {
  opacity: 0;
  transform: scale(0.85);
}
.zoom-leave-to {
  opacity: 0;
  transform: scale(1.1);
}

/* 向右滑动 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-50px);
}
.slide-right-leave-to {
  opacity: 0;
  transform: translateX(50px);
}

/* PPT推入效果 */
.push-enter-active,
.push-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.push-enter-from {
  opacity: 0;
  transform: translateX(100%);
}
.push-leave-to {
  opacity: 0;
  transform: translateX(-30%);
}

/* PPT翻页效果 */
.flip-enter-active,
.flip-leave-active {
  transition: all 0.55s cubic-bezier(0.16, 1, 0.3, 1);
  transform-style: preserve-3d;
  backface-visibility: hidden;
}
.flip-enter-from {
  opacity: 0;
  transform: perspective(1000px) rotateY(-90deg);
}
.flip-leave-to {
  opacity: 0;
  transform: perspective(1000px) rotateY(90deg);
}

/* 溶解效果 */
.dissolve-enter-active,
.dissolve-leave-active {
  transition: all 0.5s ease;
}
.dissolve-enter-from {
  opacity: 0;
  filter: blur(8px);
}
.dissolve-leave-to {
  opacity: 0;
  filter: blur(8px);
}

/* 3D旋转 */
.rotate-3d-enter-active,
.rotate-3d-leave-active {
  transition: all 0.55s cubic-bezier(0.16, 1, 0.3, 1);
  transform-style: preserve-3d;
}
.rotate-3d-enter-from {
  opacity: 0;
  transform: perspective(1000px) rotateX(-30deg) translateY(30px);
}
.rotate-3d-leave-to {
  opacity: 0;
  transform: perspective(1000px) rotateX(30deg) translateY(-30px);
}

/* 弹跳效果 */
.bounce-enter-active {
  animation: bounceIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.bounce-leave-active {
  animation: bounceOut 0.4s ease-in;
}
@keyframes bounceIn {
  0% { opacity: 0; transform: scale(0.3); }
  50% { transform: scale(1.05); }
  70% { transform: scale(0.95); }
  100% { opacity: 1; transform: scale(1); }
}
@keyframes bounceOut {
  0% { opacity: 1; transform: scale(1); }
  100% { opacity: 0; transform: scale(0.3); }
}
</style>
