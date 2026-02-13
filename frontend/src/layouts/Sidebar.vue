<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useSystemStore } from '@/store/system'
import { Compass, Connection, User, Setting, Expand, Fold, ChatDotRound, Trophy, School, Document, ArrowDown, ArrowRight, Bell, Tools, DocumentChecked } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const systemStore = useSystemStore()

// 从 localStorage 读取侧栏状态，默认为展开
const isCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true')
const expandedMenus = ref<Set<string>>(new Set())
const hoverMenuKey = ref<string | null>(null) // 当前悬停的菜单项
const hoverMenuPosition = ref({ top: 0, left: 0 }) // 悬浮菜单位置
const savedExpandedMenus = ref<Set<string>>(new Set()) // 保存收起前的展开状态
const isRestoringState = ref(false) // 标志：是否正在恢复状态

// 计算属性：检查菜单是否展开（确保响应式）
const isMenuExpanded = (key: string) => {
  return expandedMenus.value.has(key)
}

// 检查菜单组是否激活
const isCompetitionActive = computed(() => {
  const path = route.path
  return path === '/competition' || 
         path === '/competition/manage' || 
         path === '/competition/templates' ||
         path.startsWith('/competition/')
})

const isContentManageActive = computed(() => {
  const path = route.path
  return path === '/admin/reports' || 
         path === '/admin/tags' || 
         path === '/admin/announcements' ||
         path === '/admin/newbie'
})

const isSystemSettingsActive = computed(() => {
  const path = route.path
  return path === '/admin/settings' || 
         path === '/admin/audit-logs' || 
         path === '/admin/export'
})

const isMentorActive = computed(() => {
  const path = route.path
  return path.startsWith('/mentor/')
})

const menuItems = computed(() => {
  const isAdmin = authStore.hasRole(['PLATFORM_ADMIN']);
  let baseItems: any[] = [];

  if (isAdmin) {
    // 管理员菜单
    const competitionChildren = [
      { key: 'competition-manage', title: '比赛管理', path: '/competition/manage' },
      { key: 'competition-templates', title: '比赛模板', path: '/competition/templates' }
    ];

    const managementChildren = [
      { key: 'admin-dashboard', title: '数据统计', path: '/admin' },
      { key: 'user-manage', title: '用户管理', path: '/admin/users' },
      { key: 'content-manage', title: '内容管理', path: '/admin/content' },
      { key: 'system-settings', title: '系统设置', path: '/admin/settings' },
    ];
    
    // 内容管理子菜单（合并举报、标签、公告、新手保护）
    const contentChildren = [
      { key: 'report-manage', title: '举报管理', path: '/admin/reports' },
      { key: 'tag-manage', title: '标签管理', path: '/admin/tags' },
      { key: 'announcement-manage', title: '公告管理', path: '/admin/announcements' },
      { key: 'newbie-protection', title: '新手保护', path: '/admin/newbie' },
    ];
    
    // 系统设置子菜单（合并审计日志、数据导出）
    const settingsChildren = [
      { key: 'system-config', title: '基础配置', path: '/admin/settings' },
      { key: 'audit-log', title: '操作日志', path: '/admin/audit-logs' },
      { key: 'data-export', title: '数据导出', path: '/admin/export' },
    ];

    baseItems = [
      {
        key: 'ecosystem',
        title: '综合广场',
        icon: Compass,
        path: '/ecosystem'
      },
      {
        key: 'admin-dashboard',
        title: '数据统计',
        icon: DocumentChecked,
        path: '/admin'
      },
      {
        key: 'user-manage',
        title: '用户管理',
        icon: User,
        path: '/admin/users'
      },
      {
        key: 'team-manage',
        title: '团队管理',
        icon: Connection,
        path: '/teams/manage'
      },
      {
        key: 'competition',
        title: '比赛',
        icon: Trophy,
        path: '/competition',
        children: competitionChildren
      },
      {
        key: 'content-manage',
        title: '内容管理',
        icon: Document,
        path: '/admin/content',
        children: contentChildren
      },
      {
        key: 'system-settings',
        title: '系统设置',
        icon: Setting,
        path: '/admin/settings',
        children: settingsChildren
      },
      {
        key: 'admin-profile',
        title: '账户管理',
        icon: User,
        path: '/admin/profile'
      }
    ];
  } else {
    // 普通用户菜单
    const isMentor = authStore.hasRole(['MENTOR'])
    
    baseItems = [
      { key: 'ecosystem', title: '综合广场', icon: Compass, path: '/ecosystem' },
      { key: 'my-projects', title: '我的项目', icon: Document, path: '/projects/my' },
      { key: 'team', title: '团队', icon: Connection, path: '/team' },
    ];
  }

  // 通用菜单 (管理员和导师的 '我的' 已在 '账户' 分组中)
  const isMentor = authStore.hasRole(['MENTOR']) && !isAdmin
  const commonItems = isAdmin
    ? [
        // 管理员不需要"我的项目"，因为管理员通常不参与项目
        { key: 'messages', title: '消息', icon: ChatDotRound, path: '/messages' }
      ]
    : isMentor
    ? [
        // 导师不能创建项目，所以不显示"我的项目"
        { key: 'messages', title: '消息', icon: ChatDotRound, path: '/messages' },
        { key: 'profile', title: '我的', icon: User, path: '/profile' }
      ]
    : [
        { key: 'messages', title: '消息', icon: ChatDotRound, path: '/messages' },
        { key: 'profile', title: '我的', icon: User, path: '/profile' }
      ];

  let finalItems = [...baseItems, ...commonItems];

  // 导师功能
  if (authStore.hasRole(['MENTOR', 'PLATFORM_ADMIN'])) {
    const mentorChildren = [
      { key: 'mentor-scoring', title: '导师评分', path: '/mentor/scoring' }
    ];
    
    // 如果是管理员，添加导师系统管理功能
    if (authStore.hasRole(['PLATFORM_ADMIN'])) {
      mentorChildren.push(
        { key: 'mentor-system-applications', title: '成为导师申请', path: '/mentor/system/applications' },
        { key: 'mentor-system-manage', title: '导师资格管理', path: '/mentor/system/manage' }
      );
    }
    
    const mentorItem = {
      key: 'mentor',
      title: '导师',
      icon: School,
      children: mentorChildren
    };
    
    const competitionIndex = finalItems.findIndex(item => item.key === 'competition');
    if (competitionIndex !== -1) {
      finalItems.splice(competitionIndex + 1, 0, mentorItem);
    } else {
      finalItems.push(mentorItem);
    }
  }

  return finalItems;
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(isCollapsed.value))
  
  if (isCollapsed.value) {
    // 收起侧栏：保存当前展开状态
    savedExpandedMenus.value = new Set(expandedMenus.value)
    expandedMenus.value = new Set()
  } else {
    // 展开侧栏：恢复之前保存的展开状态
    isRestoringState.value = true
    
    if (savedExpandedMenus.value.size > 0) {
      // 恢复保存的状态
      expandedMenus.value = new Set(savedExpandedMenus.value)
    } else {
      // 首次展开，自动展开当前所在的分组
      const nextExpanded = new Set<string>()
      if (isManagementActive.value) nextExpanded.add('management')
      if (isCompetitionActive.value) nextExpanded.add('competition')
      expandedMenus.value = nextExpanded
    }
    
    // 延迟重置标志，防止路由监听器覆盖
    setTimeout(() => {
      isRestoringState.value = false
    }, 100)
  }
}

const handleSelect = (path: string, item?: any) => {
  // 如果有子菜单且未折叠，切换展开状态
  if (item?.children && !isCollapsed.value) {
    const newExpanded = new Set(expandedMenus.value)
    
    if (newExpanded.has(item.key)) {
      newExpanded.delete(item.key)
    } else {
      newExpanded.add(item.key)
    }
    
    expandedMenus.value = newExpanded
    return
  }
  
  router.push(path)
}

const handleSubMenuSelect = (path: string, e: Event) => {
  e.stopPropagation()
  router.push(path)
  // 关闭悬浮菜单
  hoverMenuKey.value = null
}

let hideMenuTimeout: number | null = null

const handleMouseEnter = (item: any, event: MouseEvent) => {
  if (isCollapsed.value && item.children) {
    // 清除之前的隐藏定时器
    if (hideMenuTimeout) {
      clearTimeout(hideMenuTimeout)
      hideMenuTimeout = null
    }
    
    hoverMenuKey.value = item.key
    
    // 计算悬浮菜单的位置
    const target = event.currentTarget as HTMLElement
    const rect = target.getBoundingClientRect()
    hoverMenuPosition.value = {
      top: rect.top,
      left: rect.right + 20 // 增加到 20px，更清晰的间距
    }
  }
}

const handleMouseLeave = () => {
  if (isCollapsed.value) {
    // 延迟关闭，给用户时间移动到悬浮菜单
    hideMenuTimeout = window.setTimeout(() => {
      hoverMenuKey.value = null
      hideMenuTimeout = null
    }, 300)
  }
}

const handleHoverMenuEnter = () => {
  // 鼠标进入悬浮菜单，清除隐藏定时器
  if (hideMenuTimeout) {
    clearTimeout(hideMenuTimeout)
    hideMenuTimeout = null
  }
}

const handleHoverMenuLeave = () => {
  // 鼠标离开悬浮菜单，立即隐藏
  hoverMenuKey.value = null
  if (hideMenuTimeout) {
    clearTimeout(hideMenuTimeout)
    hideMenuTimeout = null
  }
}

// 精确匹配路径，避免 /admin/competitions 匹配到 /admin
const isActive = (path: string) => {
  const currentPath = route.path

  // 精确匹配
  if (currentPath === path) return true

  // 对于 /admin，只匹配精确路径，不匹配子路径
  const rootPaths = ['/admin', '/competition', '/project', '/team', '/mentor', '/profile']
  if (rootPaths.includes(path)) {
    return false
  }

  // 使用“路径边界”匹配，避免 /project 误匹配到 /projects/my
  return currentPath.startsWith(path + '/')
}

// 路由变化时：仅在侧栏展开状态下自动展开当前所在分组
watch(() => route.path, () => {
  if (isCollapsed.value || isRestoringState.value) {
    return
  }

  const newExpanded = new Set(expandedMenus.value)
  let hasChanges = false
  
  if (isContentManageActive.value && !newExpanded.has('content-manage')) {
    newExpanded.add('content-manage')
    hasChanges = true
  }
  if (isSystemSettingsActive.value && !newExpanded.has('system-settings')) {
    newExpanded.add('system-settings')
    hasChanges = true
  }
  if (isCompetitionActive.value && !newExpanded.has('competition')) {
    newExpanded.add('competition')
    hasChanges = true
  }
  if (isMentorActive.value && !newExpanded.has('mentor')) {
    newExpanded.add('mentor')
    hasChanges = true
  }
  
  if (hasChanges) {
    expandedMenus.value = newExpanded
  }
})

// 初始化展开状态（仅在侧栏展开时）
onMounted(() => {
  if (!isCollapsed.value) {
    const nextExpanded = new Set<string>()
    
    if (isContentManageActive.value) nextExpanded.add('content-manage')
    if (isSystemSettingsActive.value) nextExpanded.add('system-settings')
    if (isCompetitionActive.value) nextExpanded.add('competition')
    if (isMentorActive.value) nextExpanded.add('mentor')
    
    expandedMenus.value = nextExpanded
  }
})
</script>

<template>
  <aside 
    class="glass-sidebar" 
    :class="{ collapsed: isCollapsed }"
  >
    <div class="sidebar-header">
      <div class="logo-container" v-if="!isCollapsed">
        <span class="logo-text">{{ systemStore.siteName }}</span>
      </div>
      <button class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="20">
          <Expand v-if="isCollapsed" />
          <Fold v-else />
        </el-icon>
      </button>
    </div>

    <nav class="sidebar-nav">
      <template v-for="item in menuItems" :key="item.key">
      <div 
        class="nav-item"
          :class="{ 
            active: item.key === 'competition' ? isCompetitionActive
              : item.key === 'content-manage' ? isContentManageActive
              : item.key === 'system-settings' ? isSystemSettingsActive
              : item.key === 'mentor' ? isMentorActive
              : isActive(item.path),
            'has-children': item.children && !isCollapsed,
            expanded: item.children && isMenuExpanded(item.key) && !isCollapsed
          }"
          @click="handleSelect(item.path, item)"
          @mouseenter="handleMouseEnter(item, $event)"
          @mouseleave="handleMouseLeave"
      >
        <el-icon class="nav-icon" :size="20">
          <component :is="item.icon" />
        </el-icon>
        <span class="nav-label" v-show="!isCollapsed">{{ item.title }}</span>
        
          <!-- 展开/收起箭头 -->
          <el-icon 
            v-if="item.children && !isCollapsed" 
            class="nav-arrow"
            :size="16"
          >
            <ArrowDown v-if="isMenuExpanded(item.key)" />
            <ArrowRight v-else />
          </el-icon>
          
          <div class="active-indicator" v-if="item.children ? isCompetitionActive : isActive(item.path)"></div>
        </div>
        
        <!-- 二级菜单（展开状态） -->
        <transition name="submenu-slide">
          <div 
            v-if="item.children && isMenuExpanded(item.key) && !isCollapsed"
            class="submenu"
          >
            <div
              v-for="child in item.children"
              :key="child.key"
              class="submenu-item"
              :class="{ active: isActive(child.path) }"
              @click="handleSubMenuSelect(child.path, $event)"
            >
              <span class="submenu-label">{{ child.title }}</span>
              <div class="active-indicator" v-if="isActive(child.path)"></div>
            </div>
          </div>
        </transition>
      </template>
    </nav>
    
    <div class="sidebar-footer">
      <!-- Optional Footer Content -->
    </div>
  </aside>

  <!-- 收起状态下的悬浮菜单（使用 Teleport 移到 body） -->
  <Teleport to="body">
    <transition name="hover-menu" mode="out-in">
      <div 
        v-if="isCollapsed && hoverMenuKey"
        class="hover-submenu"
        :style="{
          top: hoverMenuPosition.top + 'px',
          left: hoverMenuPosition.left + 'px'
        }"
        @mouseenter="handleHoverMenuEnter"
        @mouseleave="handleHoverMenuLeave"
      >
        <template v-for="item in menuItems" :key="item.key">
          <template v-if="item.key === hoverMenuKey && item.children">
            <div class="hover-submenu-header">{{ item.title }}</div>
            <div
              v-for="child in item.children"
              :key="child.key"
              class="hover-submenu-item"
              :class="{ active: isActive(child.path) }"
              @click="handleSubMenuSelect(child.path, $event)"
            >
              <span class="hover-submenu-label">{{ child.title }}</span>
            </div>
          </template>
        </template>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped lang="scss">
// 统一的动画时长和缓动函数
$transition-duration: 0.3s;
$transition-timing: cubic-bezier(0.4, 0, 0.2, 1); // Material Design 标准缓动
$transition-spring: cubic-bezier(0.34, 1.56, 0.64, 1); // 弹性效果

.glass-sidebar {
  width: 260px;
  height: calc(100vh - 40px);
  margin: 20px 0 20px 20px;
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  overflow: visible;
  z-index: 100;
  transition: width 0.4s ease-in-out !important;

  &.collapsed {
    width: 80px;
    
    .nav-item {
      justify-content: center;
      padding: 0;
      
      .nav-icon {
        margin-right: 0;
        font-size: 24px;
        transition: all 0.4s ease-in-out;
      }
      
      .nav-label,
      .nav-arrow {
        opacity: 0;
        width: 0;
        overflow: hidden;
        transition: opacity 0.3s ease, width 0.4s ease;
      }
    }
    
    .submenu {
      display: none !important;
    }
    
    .logo-container {
      opacity: 0;
      width: 0;
      overflow: hidden;
    }
  }
  
  &:hover {
    box-shadow: var(--shadow-card), 0 8px 32px rgba(0, 0, 0, 0.08);
    transform: translateX(2px);
  }
}

[data-theme='dark'] .glass-sidebar {
  background: var(--glass-bg-dark);
  border-color: rgba(255, 255, 255, 0.05);
}

.sidebar-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  
  .logo-container {
    opacity: 1;
    transition: opacity 0.3s ease;
    overflow: hidden;
    white-space: nowrap;
  }
  
  .logo-text {
    font-size: 20px;
    font-weight: 800;
    background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    transition: all $transition-duration $transition-timing;
  }
  
  .collapse-btn {
    background: transparent;
    border: none;
    color: var(--text-color-muted);
    cursor: pointer;
    padding: 8px;
    border-radius: 8px;
    transition: all $transition-duration $transition-timing;
    will-change: transform, background-color;
    
    &:hover {
      background: var(--bg-elevated-soft);
      color: var(--text-color);
      transform: scale(1.15) rotate(5deg);
    }
    
    &:active {
      transform: scale(0.9) rotate(0deg);
    }
  }
}

.sidebar-nav {
  flex: 1;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  overflow-x: visible;
}

.nav-item {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-radius: 16px;
  cursor: pointer;
  color: var(--text-color-muted);
  transition: all $transition-duration $transition-timing;
  position: relative;
  will-change: transform, background-color;
  user-select: none; // 禁止选中文字
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  
  .nav-icon {
    font-size: 20px;
    margin-right: 12px;
    transition: all 0.4s ease-in-out;
    flex-shrink: 0;
  }
  
  .nav-label {
    font-weight: 600;
    font-size: 15px;
    white-space: nowrap;
    opacity: 1;
    transition: opacity 0.3s ease, width 0.4s ease;
    flex: 1;
  }
  
  .nav-arrow {
    margin-left: auto;
    color: var(--text-color-muted);
    transition: all 0.4s ease-in-out;
    flex-shrink: 0;
  }
  
  &:hover {
    background: rgba(var(--accent-color-rgb), 0.05);
    color: var(--accent-color);
    transform: translateX(4px);
    
    .nav-icon {
      transform: scale(1.15) rotate(5deg);
    }
    
    .nav-arrow {
      color: var(--accent-color);
      transform: translateX(3px);
    }
  }
  
  &.active {
    background: var(--accent-color);
    color: white;
    box-shadow: var(--shadow-button);
    transform: translateX(0);
    
    .nav-icon,
    .nav-arrow {
      color: white;
    }
    
    &:hover {
      box-shadow: var(--shadow-button), 0 4px 16px rgba(var(--accent-color-rgb), 0.3);
      transform: translateX(2px);
    }
  }
  
  &.expanded {
    .nav-arrow {
      transform: rotate(0deg);
      transition: transform 0.4s ease-in-out;
    }
  }
  
  &.has-children {
    .nav-arrow {
      transition: transform 0.4s ease-in-out;
    }
  }
}

.submenu {
  margin-left: 24px;
  padding-left: 8px;
  padding-top: 12px;
  padding-bottom: 12px;
  margin-top: 12px;
  margin-bottom: 12px;
  border-left: 2px solid rgba(var(--accent-color-rgb), 0.2);
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

// Vue Transition 动画
.submenu-slide-enter-active,
.submenu-slide-leave-active {
  transition: all 0.4s ease-in-out !important;
  transform-origin: top;
}

.submenu-slide-enter-from {
  opacity: 0 !important;
  max-height: 0 !important;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  transform: scaleY(0) !important;
}

.submenu-slide-enter-to {
  opacity: 1 !important;
  max-height: 600px !important;
  margin-top: 12px !important;
  margin-bottom: 12px !important;
  padding-top: 12px !important;
  padding-bottom: 12px !important;
  transform: scaleY(1) !important;
}

.submenu-slide-leave-from {
  opacity: 1 !important;
  max-height: 600px !important;
  margin-top: 12px !important;
  margin-bottom: 12px !important;
  padding-top: 12px !important;
  padding-bottom: 12px !important;
  transform: scaleY(1) !important;
}

.submenu-slide-leave-to {
  opacity: 0 !important;
  max-height: 0 !important;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  transform: scaleY(0) !important;
}

.submenu-item {
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-radius: 12px;
  cursor: pointer;
  color: var(--text-color-muted);
  position: relative;
  user-select: none; // 禁止选中文字
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  // 移除单独的 opacity 和 transform 动画，让子项跟随父容器一起动画
  transition: background-color 0.3s ease,
              color 0.3s ease,
              transform 0.3s ease;
  
  .submenu-label {
    font-weight: 500;
    font-size: 14px;
    white-space: nowrap;
    transition: transform $transition-duration $transition-timing;
    display: inline-block;
  }
  
  &:hover {
    background: rgba(var(--accent-color-rgb), 0.08);
    color: var(--accent-color);
    transform: translateX(4px);
    
    .submenu-label {
      transform: translateX(3px);
    }
  }
  
  &.active {
    background: rgba(var(--accent-color-rgb), 0.15);
    color: var(--accent-color);
    font-weight: 600;
    
    .active-indicator {
      display: block;
    }
  }
  
  &:active {
    transform: scale(0.97);
  }
}
</style>

<!-- 非 scoped 样式，用于 Teleport 的悬浮菜单 -->
<style lang="scss">
// 统一的动画时长和缓动函数
$transition-duration: 0.3s;
$transition-timing: cubic-bezier(0.4, 0, 0.2, 1);
$transition-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

// 悬浮菜单样式（Teleport 到 body，需要非 scoped）
.hover-submenu {
  position: fixed !important;
  min-width: 200px;
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  padding: 8px;
  z-index: 10000;
  
  .hover-submenu-header {
    padding: 8px 12px;
    font-weight: 600;
    font-size: 14px;
    color: var(--text-color);
    border-bottom: 1px solid var(--border-subtle);
    margin-bottom: 4px;
  }
  
  .hover-submenu-item {
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    color: var(--text-color-muted);
    transition: all $transition-duration $transition-timing;
    font-size: 14px;
    
    .hover-submenu-label {
      font-weight: 500;
      transition: transform $transition-duration $transition-timing;
      display: inline-block;
    }
    
    &:hover {
      background: rgba(64, 158, 255, 0.08);
      color: #409EFF;
      transform: translateX(4px);
      
      .hover-submenu-label {
        transform: translateX(2px);
      }
    }
    
    &.active {
      background: rgba(64, 158, 255, 0.15);
      color: #409EFF;
      font-weight: 600;
    }
    
    &:active {
      transform: scale(0.98);
    }
  }
}

[data-theme='dark'] .hover-submenu {
  background: rgba(30, 30, 30, 0.95);
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

// 悬浮菜单动画 - 使用 transition 而不是 animation
.hover-menu-enter-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}

.hover-menu-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.hover-menu-enter-from {
  opacity: 0 !important;
  transform: translateX(-20px) translateY(-5px) scale(0.9) !important;
}

.hover-menu-enter-to {
  opacity: 1 !important;
  transform: translateX(0) translateY(0) scale(1) !important;
}

.hover-menu-leave-from {
  opacity: 1 !important;
  transform: translateX(0) translateY(0) scale(1) !important;
}

.hover-menu-leave-to {
  opacity: 0 !important;
  transform: translateX(-20px) translateY(-5px) scale(0.9) !important;
}
</style>
