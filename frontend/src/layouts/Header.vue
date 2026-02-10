<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import NotificationBell from '@/components/common/NotificationBell.vue'
import GlobalSearch from '@/components/common/GlobalSearch.vue'
import ThemeSwitcher from '@/components/common/ThemeSwitcher.vue'

const router = useRouter()
const authStore = useAuthStore()

const searchQuery = ref('')
const globalSearchRef = ref()

const handleCommand = (command: string) => {
  if (command === 'logout') {
    // 立即清除本地状态
    authStore.logout()
    // 立即跳转，不等待
    router.replace('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/account/settings')
  }
}

const handleSearch = () => {
  // 打开全局搜索对话框
  if (globalSearchRef.value) {
    globalSearchRef.value.open()
  }
}
</script>

<template>
  <header class="glass-header">
    <div class="header-content">
      <!-- Search Bar -->
      <div class="search-container" @click="handleSearch">
        <el-input
          v-model="searchQuery"
          placeholder="搜索项目、团队或人才... (Ctrl+K)"
          class="search-input"
          :prefix-icon="Search"
          readonly
        />
      </div>
      
      <!-- Global Search Dialog -->
      <GlobalSearch ref="globalSearchRef" />

      <!-- Right Actions -->
      <div class="header-actions">
        <ThemeSwitcher />
        <!-- Notifications -->
        <NotificationBell />

        <!-- User Profile -->
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-profile-trigger">
            <el-avatar :size="36" :src="authStore.user?.profile?.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
            <span class="username">{{ authStore.user?.profile?.realName || authStore.user?.username || '用户' }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="glass-dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="settings">账户设置</el-dropdown-item>
              <el-dropdown-item divided command="logout" style="color: var(--el-color-danger)">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.glass-header {
  height: 64px;
  width: 100%;
  position: sticky;
  top: 0;
  z-index: 90;
  /* Transparent background to let the blur effect work if we had a global backdrop, 
     but usually header needs its own glass effect */
  background: transparent; 
  padding: 0 24px;
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--border-subtle);
  /* Make it float or just full width? 
     Plan says "Immersive top navigation". 
     Usually this means it blends with background or is glass.
     If Sidebar is floating, Header might be full width or also floating.
     Let's make it fill the space next to sidebar if sidebar pushes content,
     or if sidebar is floating ON TOP, header is behind?
     Usually Sidebar is left, Header is top of Main Area.
  */
  border-radius: 0 0 24px 24px; /* Rounded bottom corners? Or just flat? Let's try flat for full width feel or rounded for floating feel */
  padding: 0 24px;
  box-shadow: var(--shadow-card);
}

[data-theme='dark'] .header-content {
  background: var(--glass-bg-dark);
}

.search-container {
  width: 320px;
  
  :deep(.el-input__wrapper) {
    border-radius: 20px;
    background: rgba(0, 0, 0, 0.05);
    box-shadow: none;
    border: 1px solid transparent;
    transition: all 0.3s;
    
    &:hover, &.is-focus {
      background: var(--bg-elevated);
      box-shadow: var(--shadow-card);
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  
  &:hover {
    background: rgba(0,0,0,0.05);
  }
}

.user-profile-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &:hover {
    background: rgba(0,0,0,0.05);
    transform: translateY(-1px);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  .username {
    font-weight: 600;
    font-size: 14px;
    color: var(--text-color);
  }
  
  .el-icon {
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
}
</style>

<!-- 非 scoped 样式，用于 Element Plus 下拉菜单动画 -->
<style lang="scss">
// 统一的动画参数
$dropdown-duration: 0.3s;
$dropdown-timing: cubic-bezier(0.4, 0, 0.2, 1);
$dropdown-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// Element Plus 下拉菜单动画
.el-dropdown-menu {
  &.glass-dropdown {
    background: var(--glass-bg-light) !important;
    backdrop-filter: blur(var(--glass-blur)) !important;
    border: 1px solid var(--border-subtle) !important;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12) !important;
    border-radius: 12px !important;
    padding: 8px !important;
    
    .el-dropdown-menu__item {
      border-radius: 8px;
      transition: all 0.2s $dropdown-timing;
      
      &:hover {
        background: rgba(64, 158, 255, 0.08) !important;
        color: #409EFF !important;
        transform: translateX(4px);
      }
      
      &:active {
        transform: scale(0.98) translateX(4px);
      }
    }
  }
}

[data-theme='dark'] .el-dropdown-menu.glass-dropdown {
  background: rgba(30, 30, 30, 0.95) !important;
  border-color: rgba(255, 255, 255, 0.05) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4) !important;
}

// Element Plus 下拉菜单进入/退出动画
.el-dropdown__popper {
  &.el-popper {
    // 进入动画
    &[data-popper-placement^='bottom'] {
      .el-dropdown-menu {
        animation: dropdownSlideDown 0.3s $dropdown-spring !important;
      }
    }
    
    &[data-popper-placement^='top'] {
      .el-dropdown-menu {
        animation: dropdownSlideUp 0.3s $dropdown-spring !important;
      }
    }
  }
}

// 下拉菜单动画关键帧
@keyframes dropdownSlideDown {
  0% {
    opacity: 0;
    transform: translateY(-12px) scale(0.95);
  }
  60% {
    transform: translateY(2px) scale(1.02);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes dropdownSlideUp {
  0% {
    opacity: 0;
    transform: translateY(12px) scale(0.95);
  }
  60% {
    transform: translateY(-2px) scale(1.02);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

// Element Plus transition 类名覆盖
.el-zoom-in-top-enter-active,
.el-zoom-in-top-leave-active {
  transition: all 0.25s $dropdown-timing !important;
}

.el-zoom-in-top-enter-from {
  opacity: 0 !important;
  transform: translateY(-8px) scale(0.95) !important;
}

.el-zoom-in-top-enter-to {
  opacity: 1 !important;
  transform: translateY(0) scale(1) !important;
}

.el-zoom-in-top-leave-from {
  opacity: 1 !important;
  transform: translateY(0) scale(1) !important;
}

.el-zoom-in-top-leave-to {
  opacity: 0 !important;
  transform: translateY(-8px) scale(0.92) !important;
}
</style>
