<template>
  <div class="team-space" role="main" aria-label="团队协作空间">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container" role="status" aria-live="polite" aria-label="正在加载团队数据">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container" role="alert" aria-live="assertive">
      <el-result
        icon="error"
        title="加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="loadTeamData" aria-label="重新加载团队数据">重试</el-button>
          <el-button @click="router.push({ name: 'TeamList' })" aria-label="返回团队列表">返回团队列表</el-button>
        </template>
      </el-result>
    </div>

    <!-- 主内容 -->
    <el-container v-else class="team-space-container">
      <!-- 顶部栏 -->
      <el-header class="team-header" role="banner">
        <div class="header-left">
          <!-- 团队信息 -->
          <div class="team-info" role="group" aria-label="当前团队信息">
            <el-avatar
              :size="40"
              :src="currentTeam?.avatar"
              class="team-avatar"
              role="img"
              :aria-label="`${currentTeam?.name} 团队头像`"
            >
              {{ currentTeam?.name?.charAt(0) }}
            </el-avatar>
            <div class="team-details">
              <h2 class="team-name" id="current-team-name">{{ currentTeam?.name }}</h2>
              <span class="team-member-count" aria-label="`团队成员数量: ${currentTeamMembers.length} 名`">
                {{ currentTeamMembers.length }} 名成员
              </span>
            </div>
          </div>
        </div>

        <div class="header-right">
          <!-- 团队操作菜单 -->
          <el-dropdown 
            @command="handleTeamAction" 
            trigger="click"
            aria-label="团队操作菜单"
          >
            <el-button type="primary" text aria-label="打开团队操作菜单">
              <el-icon><Setting /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu role="menu" aria-label="团队操作列表">
                <el-dropdown-item 
                  v-if="isTeamLeader" 
                  command="delete" 
                  role="menuitem"
                  aria-label="删除团队"
                >
                  <el-icon><Delete /></el-icon>
                  删除团队
                </el-dropdown-item>
                <el-dropdown-item 
                  v-if="!isTeamLeader" 
                  command="leave" 
                  role="menuitem"
                  aria-label="退出团队"
                >
                  <el-icon><Close /></el-icon>
                  退出团队
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 团队切换下拉菜单 -->
          <el-dropdown 
            @command="handleTeamSwitch" 
            trigger="click"
            aria-label="切换团队菜单"
          >
            <el-button type="primary" link aria-label="打开团队切换菜单">
              切换团队
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu role="menu" aria-label="可用团队列表">
                <el-dropdown-item
                  v-for="team in teams"
                  :key="team.id"
                  :command="team.id"
                  :disabled="team.id === teamId"
                  role="menuitem"
                  :aria-label="`切换到 ${team.name} 团队${team.id === teamId ? ' (当前团队)' : ''}`"
                >
                  <div class="dropdown-team-item">
                    <el-avatar :size="24" :src="team.avatar">
                      {{ team.name?.charAt(0) }}
                    </el-avatar>
                    <span>{{ team.name }}</span>
                    <el-tag v-if="team.id === teamId" size="small" type="success">
                      当前
                    </el-tag>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="view-all" role="menuitem" aria-label="查看所有团队">
                  <el-icon><List /></el-icon>
                  查看所有团队
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container class="main-container">
        <!-- 侧边栏导航 -->
        <el-aside width="240px" class="team-sidebar" role="navigation" aria-label="团队功能导航">
          <el-menu
            :default-active="activeMenu"
            @select="handleMenuSelect"
            class="sidebar-menu"
            role="menu"
          >
            <el-menu-item 
              index="overview"
              tabindex="0"
              @keydown.enter="handleMenuSelect('overview')"
              @keydown.space.prevent="handleMenuSelect('overview')"
              role="menuitem"
              aria-label="团队概览"
            >
              <el-icon><House /></el-icon>
              <span>团队概览</span>
            </el-menu-item>
            <el-menu-item 
              index="tasks"
              tabindex="0"
              @keydown.enter="handleMenuSelect('tasks')"
              @keydown.space.prevent="handleMenuSelect('tasks')"
              role="menuitem"
              aria-label="任务看板"
            >
              <el-icon><List /></el-icon>
              <span>任务看板</span>
            </el-menu-item>
            <el-menu-item 
              index="files"
              tabindex="0"
              @keydown.enter="handleMenuSelect('files')"
              @keydown.space.prevent="handleMenuSelect('files')"
              role="menuitem"
              aria-label="文件共享"
            >
              <el-icon><Folder /></el-icon>
              <span>文件共享</span>
            </el-menu-item>
            <el-menu-item 
              index="chat"
              tabindex="0"
              @keydown.enter="handleMenuSelect('chat')"
              @keydown.space.prevent="handleMenuSelect('chat')"
              role="menuitem"
              aria-label="团队聊天"
            >
              <el-icon><ChatDotRound /></el-icon>
              <span>团队聊天</span>
            </el-menu-item>
            <el-menu-item 
              index="sprints"
              tabindex="0"
              @keydown.enter="handleMenuSelect('sprints')"
              @keydown.space.prevent="handleMenuSelect('sprints')"
              role="menuitem"
              aria-label="Sprint管理"
            >
              <el-icon><TrendCharts /></el-icon>
              <span>Sprint管理</span>
            </el-menu-item>
            <el-menu-item 
              index="standup"
              tabindex="0"
              @keydown.enter="handleMenuSelect('standup')"
              @keydown.space.prevent="handleMenuSelect('standup')"
              role="menuitem"
              aria-label="每日站会"
            >
              <el-icon><Calendar /></el-icon>
              <span>每日站会</span>
            </el-menu-item>
            <el-menu-item 
              index="settings"
              tabindex="0"
              @keydown.enter="handleMenuSelect('settings')"
              @keydown.space.prevent="handleMenuSelect('settings')"
              role="menuitem"
              aria-label="团队设置"
            >
              <el-icon><Tools /></el-icon>
              <span>团队设置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 主内容区 -->
        <el-main class="team-main" role="region" aria-labelledby="current-team-name">
          <router-view v-slot="{ Component, route }">
            <transition :name="route.meta.transition || 'page-slide'" mode="out-in">
              <component :is="Component" :key="route.path" :team-id="teamId" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  House,
  List,
  Folder,
  ChatDotRound,
  User,
  ArrowDown,
  Setting,
  Delete,
  Close,
  TrendCharts,
  Calendar,
  Tools
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()
const authStore = useAuthStore()

// 从路由参数获取团队 ID
const teamId = computed(() => Number(route.params.id))

// 从 store 获取数据
const currentTeam = computed(() => teamStore.currentTeam)
const currentTeamMembers = computed(() => teamStore.currentTeamMembers)
const teams = computed(() => teamStore.teams)
const loading = computed(() => teamStore.loading)
const error = computed(() => teamStore.error)

// 当前激活的菜单项
const activeMenu = computed(() => {
  const routeName = route.name as string
  if (!routeName) return 'overview'
  
  // 从路由名称提取模块名称
  const moduleMap: Record<string, string> = {
    'TeamOverview': 'overview',
    'TeamTasks': 'tasks',
    'TeamFiles': 'files',
    'TeamChat': 'chat',
    'TeamSprints': 'sprints',
    'TeamStandup': 'standup',
    'TeamSettings': 'settings'
  }
  
  return moduleMap[routeName] || 'overview'
})

/**
 * 加载团队数据
 */
const loadTeamData = async () => {
  if (!teamId.value || isNaN(teamId.value)) {
    ElMessage.error('无效的团队 ID')
    router.push({ name: 'TeamList' })
    return
  }

  // 保存当前访问的团队ID（与用户ID关联）
  if (authStore.user?.id) {
    localStorage.setItem(`lastVisitedTeamId_${authStore.user.id}`, String(teamId.value))
  }

  const start = performance.now()

  try {
    // 使用带缓存/优先级的加载（cache-then-refresh）来提升进入速度
    await teamStore.loadTeamPage(teamId.value, 'overview')
    
    // 如果用户团队列表为空，后台加载团队列表（用于团队切换），不阻塞首屏
    if (teams.value.length === 0 && authStore.user) {
      teamStore.fetchUserTeams({ userId: authStore.user.id }).catch(() => {})
    }
  } catch (err: any) {
    console.error('Failed to load team data:', err)
    
    // 如果是团队不存在的错误，自动跳转到团队列表
    if (err.message && (err.message.includes('不存在') || err.message.includes('404'))) {
      ElMessage.error('团队不存在或已被删除')
      router.push({ name: 'TeamList' })
    }
    // 错误已在 store 中设置，这里不需要额外处理
  }
}

/**
 * 处理菜单选择
 */
const handleMenuSelect = (key: string) => {
  const routeMap: Record<string, string> = {
    'overview': 'TeamOverview',
    'tasks': 'TeamTasks',
    'files': 'TeamFiles',
    'chat': 'TeamChat',
    'sprints': 'TeamSprints',
    'standup': 'TeamStandup',
    'settings': 'TeamSettings'
  }
  
  const routeName = routeMap[key]
  if (routeName && route.name !== routeName) {
    // 1. 立即导航，让UI先响应
    router.push({
      name: routeName,
      params: { id: teamId.value }
    })

    // 2. 后台预取数据，不阻塞UI
    if (key === 'tasks' || key === 'overview') {
      teamStore.loadTeamPage(teamId.value, key as 'overview' | 'tasks')
        .catch(err => {
          console.warn(`Prefetch for ${key} failed:`, err)
        })
    }
  }
}

/**
 * 处理团队切换
 */
const handleTeamSwitch = (command: number | string) => {
  if (command === 'view-all') {
    // 查看所有团队 - 添加 noRedirect 参数防止自动跳转
    router.push({ 
      name: 'TeamList',
      query: { noRedirect: 'true' }
    })
  } else if (typeof command === 'number') {
    // 保存最后访问的团队ID（与用户ID关联）
    if (authStore.user?.id) {
      localStorage.setItem(`lastVisitedTeamId_${authStore.user.id}`, String(command))
    }
    
    // 切换到指定团队
    router.push({
      name: 'TeamOverview',
      params: { id: command }
    })
  }
}

/**
 * 判断当前用户是否为团队领导者
 */
const isTeamLeader = computed(() => {
  if (!currentTeam.value || !authStore.user) return false
  return currentTeam.value.leaderId === authStore.user.id || 
         currentTeam.value.creatorId === authStore.user.id
})

/**
 * 处理团队操作
 */
const handleTeamAction = async (command: string) => {
  if (command === 'leave') {
    // 退出团队
    await handleLeaveTeam()
  } else if (command === 'delete') {
    // 删除团队
    await handleDeleteTeam()
  }
}

/**
 * 退出团队
 */
const handleLeaveTeam = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出该团队吗？退出后将无法访问团队资源。',
      '退出团队',
      {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const currentTeamId = teamId.value
    
    // 先跳转到团队列表，避免在退出后的页面上触发重新加载
    await router.push({ name: 'TeamList' })
    
    // 然后退出团队
    await teamStore.leaveTeam(currentTeamId)
    ElMessage.success('已退出团队')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to leave team:', error)
      ElMessage.error(error.message || '退出团队失败')
    }
  }
}

/**
 * 删除团队
 */
const handleDeleteTeam = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该团队吗？此操作将删除所有团队数据且不可恢复！',
      '删除团队',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    const currentTeamId = teamId.value
    
    // 先跳转到团队列表，避免在删除后的页面上触发重新加载
    await router.push({ name: 'TeamList' })
    
    // 然后删除团队
    await teamStore.deleteTeam(currentTeamId)
    ElMessage.success('团队已删除')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete team:', error)
      ElMessage.error(error.message || '删除团队失败')
    }
  }
}

// 监听 teamId 变化，重新加载数据
watch(teamId, (newTeamId, oldTeamId) => {
  if (newTeamId && newTeamId !== oldTeamId) {
    loadTeamData()
  }
}, { immediate: false })

// 组件挂载时加载数据
onMounted(() => {
  loadTeamData()
})
</script>

<style scoped lang="scss">
.team-space {
  height: 100vh;
  background: var(--bg-body);

  .loading-container,
  .error-container {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100vh;
    padding: var(--spacing-xl);
    animation: fadeIn 0.3s ease;
  }

  .team-space-container {
    height: 100vh;
    flex-direction: column;
  }

  // 顶部栏样式
  .team-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--spacing-lg);
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-card);
    height: 64px;
    transition: all var(--transition-base);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .header-left {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      flex: 1;
      min-width: 0;
    }

    .team-info {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
      flex: 1;

      .team-avatar {
        background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
        color: #fff;
        font-weight: 600;
        flex-shrink: 0;
      }

      .team-details {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
        flex: 1;

        .team-name {
          margin: 0;
          font-size: 18px;
          font-weight: 600;
          color: var(--text-color);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .team-member-count {
          font-size: 12px;
          color: var(--text-color-secondary);
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      flex-shrink: 0;
    }
  }

  .main-container {
    flex: 1;
    overflow: hidden;
  }

  // 侧边栏样式
  .team-sidebar {
    background: var(--bg-card);
    border-right: 1px solid var(--border-card);
    overflow-y: auto;
    transition: all var(--transition-base);

    .sidebar-menu {
      background: transparent;
      border: none;
      padding: var(--spacing-sm) 0;

      /* el-menu 默认会自己渲染白底，这里显式改成透明/跟随主题 */
      :deep(.el-menu) {
        background: transparent;
        border-right: none;
      }

      :deep(.el-menu-item) {
        position: relative;
        height: 48px;
        line-height: 48px;
        margin: 4px 12px;
        border-radius: var(--radius-small);
        color: var(--text-color);
        transition: all var(--transition-base);

        &:hover {
          background: var(--bg-card-hover);
          transform: translateX(4px);
        }

        &.is-active {
          background: var(--accent-soft);
          color: var(--accent-color);
          font-weight: 600;
          
          &::before {
            content: '';
            position: absolute;
            left: 0;
            top: 50%;
            transform: translateY(-50%);
            width: 3px;
            height: 24px;
            background: var(--accent-color);
            border-radius: 0 2px 2px 0;
          }
        }

        .el-icon {
          margin-right: var(--spacing-sm);
          transition: transform var(--transition-base);
        }
        
        &:hover .el-icon {
          transform: scale(1.1);
        }
      }
    }
  }

  // 主内容区样式
  .team-main {
    background: var(--bg-body);
    padding: var(--spacing-lg);
    overflow-y: auto;
    animation: fadeInUp 0.4s var(--ease-out);
  }

  // 下拉菜单样式
  .dropdown-team-item {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    min-width: 200px;
    transition: all var(--transition-fast);

    span {
      flex: 1;
    }
    
    &:hover {
      color: var(--accent-color);
    }
  }
}

// 响应式设计 - 平板
@media (max-width: 1024px) {
  .team-space {
    .team-sidebar {
      width: 200px !important;
    }
    
    .team-main {
      padding: var(--spacing-md);
    }
  }
}

// 响应式设计 - 移动端
@media (max-width: 768px) {
  .team-space {
    .team-header {
      height: 56px;
      padding: 0 var(--spacing-md);
      
      .header-left {
        gap: var(--spacing-sm);
      }
      
      .team-info {
        gap: var(--spacing-sm);
        
        .team-avatar {
          width: 36px !important;
          height: 36px !important;
          font-size: 14px;
        }
      }

      .team-details {
        .team-name {
          font-size: 16px;
        }

        .team-member-count {
          display: none;
        }
      }
      
      .header-right {
        gap: var(--spacing-sm);
      }
    }
    
    .team-sidebar {
      width: 60px !important;

      .sidebar-menu {
        padding: var(--spacing-xs) 0;
        
        :deep(.el-menu-item) {
          margin: 4px 8px;
          justify-content: center;
          
          span {
            display: none;
          }
          
          .el-icon {
            margin-right: 0;
          }
          
          &:hover {
            transform: scale(1.05);
          }
          
          &.is-active::before {
            display: none;
          }
        }
      }
    }
    
    .team-main {
      padding: var(--spacing-sm);
    }
  }
}

// 响应式设计 - 小屏幕
@media (max-width: 480px) {
  .team-space {
    .team-header {
      height: 48px;
      
      .team-info {
        .team-avatar {
          width: 32px !important;
          height: 32px !important;
          font-size: 12px;
        }
        
        .team-details {
          .team-name {
            font-size: 14px;
          }
        }
      }
    }
    
    .team-sidebar {
      width: 50px !important;
      
      .sidebar-menu {
        :deep(.el-menu-item) {
          height: 40px;
          line-height: 40px;
          margin: 2px 4px;
          
          .el-icon {
            font-size: 18px;
          }
        }
      }
    }
  }
}

// 横屏模式优化
@media (orientation: landscape) and (max-height: 600px) {
  .team-space {
    .team-header {
      height: 48px;
    }
    
    .team-main {
      padding: var(--spacing-sm);
    }
  }
}

// 触摸设备优化
@media (hover: none) and (pointer: coarse) {
  .team-space {
    .team-sidebar {
      .sidebar-menu {
        :deep(.el-menu-item) {
          min-height: 48px;
          
          &:hover {
            transform: none;
          }
          
          &:active {
            transform: scale(0.95);
          }
        }
      }
    }
  }
}

// 动画
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeInUp {
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
/* TeamSpace 子页面过渡动画 - 不使用 scoped 确保能应用到 router-view 子组件 */

/* page-slide 动画 */
.page-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.page-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0.085, 0.68, 0.53);
}
.page-slide-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.page-slide-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}

/* flip 翻页动画 */
.flip-enter-active,
.flip-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
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

/* fade 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* slide-fade 滑动淡入 */
.slide-fade-enter-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-fade-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 1, 1);
}
.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(50px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-50px);
}

/* zoom 缩放 */
.zoom-enter-active,
.zoom-leave-active {
  transition: all 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.zoom-enter-from {
  opacity: 0;
  transform: scale(0.8);
}
.zoom-leave-to {
  opacity: 0;
  transform: scale(1.15);
}
</style>
