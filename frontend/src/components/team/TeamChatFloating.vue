<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useTeamStore } from '@/store/team'
import Chat from '@/views/team/Chat.vue'
import { ArrowDown } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const teamStore = useTeamStore()
const route = useRoute()
const router = useRouter()

// 是否展开悬浮聊天窗
const isOpen = ref(false)

// 全局未读计数（基于 Chat.vue 广播的事件）
const unreadCount = ref(0)

// 浮窗位置（整个容器：按钮 + 面板 一起移动）
const posX = ref(0)
const posY = ref(0)

// 面板尺寸（可拖拽调整）
const panelWidth = ref(360)
const panelHeight = ref(520)
const minWidth = 280
const maxWidth = 640
const minHeight = 360
const maxHeight = 720

const isResizing = ref(false)
let startX = 0
let startY = 0
let startWidth = 0
let startHeight = 0

// 拖拽移动
const isDragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let dragOriginX = 0
let dragOriginY = 0

// 当前浮窗占用宽高（拖动边界用）
const containerWidth = computed(() => {
  // 展开时用面板宽度，收起时用一个较小的按钮宽度
  return isOpen.value ? panelWidth.value : 140
})

const containerHeight = computed(() => {
  // 展开时用面板高度，收起时用按钮高度
  return isOpen.value ? panelHeight.value : 48
})

// 路由/最近访问得到的默认团队 ID
const effectiveTeamId = computed<number | null>(() => {
  const fromRoute = Number(route.params.id)
  if (!isNaN(fromRoute) && fromRoute > 0) return fromRoute

  const userId = authStore.user?.id
  if (!userId) return null

  const last = window.localStorage.getItem(`lastVisitedTeamId_${userId}`)
  const parsed = last ? Number(last) : NaN
  return !isNaN(parsed) && parsed > 0 ? parsed : null
})

// 悬浮窗内手动选择的团队 ID（优先级高于路由）
const selectedTeamId = ref<number | null>(null)

// 实际使用的团队 ID
const activeTeamId = computed<number | null>(() => {
  return selectedTeamId.value ?? effectiveTeamId.value
})

const hasTeamContext = computed(() => !!activeTeamId.value)

const currentTeamName = computed(() => {
  const id = activeTeamId.value
  if (!id) return '团队聊天'
  const inStore = teamStore.teams.find(t => t.id === id)
  return inStore?.name || '团队聊天'
})

const handleToggle = () => {
  if (!hasTeamContext.value) {
    // 如果没有团队上下文，引导用户去团队列表
    router.push({ name: 'TeamList' })
    return
  }
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    unreadCount.value = 0
  }
}

// 在悬浮窗内切换团队
const handleSwitchTeam = (teamId: number) => {
  selectedTeamId.value = teamId
  unreadCount.value = 0

  // 更新最近访问团队 ID，保持与主站逻辑一致
  if (authStore.user?.id) {
    window.localStorage.setItem(
      `lastVisitedTeamId_${authStore.user.id}`,
      String(teamId)
    )
  }
}

const handleTeamChatNewMessage = (event: Event) => {
  const customEvent = event as CustomEvent
  const detail: any = customEvent.detail || {}
  if (!detail) return

  // 只统计当前聊天团队的消息
  if (detail.teamId !== activeTeamId.value) return
  if (detail.isMe) return

  // 打开状态时不计未读
  if (isOpen.value) return

  unreadCount.value += 1
}

// 拖拽调整大小（从左上角方向拖拽，高度朝上、宽度朝左变化，保持右下角锚点）
const beginResize = (e: MouseEvent) => {
  e.preventDefault()
  isResizing.value = true
  startX = e.clientX
  startY = e.clientY
  startWidth = panelWidth.value
  startHeight = panelHeight.value
  window.addEventListener('mousemove', handleResizeMove)
  window.addEventListener('mouseup', endResize)
}

const handleResizeMove = (e: MouseEvent) => {
  if (!isResizing.value) return
  const dx = startX - e.clientX
  const dy = startY - e.clientY

  let nextW = startWidth + dx
  let nextH = startHeight + dy

  nextW = Math.min(Math.max(nextW, minWidth), maxWidth)
  nextH = Math.min(Math.max(nextH, minHeight), maxHeight)

  panelWidth.value = nextW
  panelHeight.value = nextH
}

const endResize = () => {
  if (!isResizing.value) return
  isResizing.value = false
  window.removeEventListener('mousemove', handleResizeMove)
  window.removeEventListener('mouseup', endResize)
}

// 拖拽移动整个浮窗
const beginDrag = (e: MouseEvent) => {
  // 右键或正在调整大小时不处理
  if (e.button !== 0 || isResizing.value) return
  e.preventDefault()
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragOriginX = posX.value
  dragOriginY = posY.value
  window.addEventListener('mousemove', handleDragMove)
  window.addEventListener('mouseup', endDrag)
}

const handleDragMove = (e: MouseEvent) => {
  if (!isDragging.value) return
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY

  let nextX = dragOriginX + dx
  let nextY = dragOriginY + dy

  const margin = 8
  const vw = window.innerWidth
  const vh = window.innerHeight

  const w = containerWidth.value
  const h = containerHeight.value

  // 限制在视口内（根据当前容器宽高计算）
  nextX = Math.min(Math.max(nextX, margin), vw - margin - w)
  nextY = Math.min(Math.max(nextY, margin), vh - margin - h)

  posX.value = nextX
  posY.value = nextY
}

const endDrag = () => {
  if (!isDragging.value) return
  isDragging.value = false
  window.removeEventListener('mousemove', handleDragMove)
  window.removeEventListener('mouseup', endDrag)
}

onMounted(() => {
  // 默认位置：右下角附近
  const vw = window.innerWidth
  const vh = window.innerHeight
  const w = containerWidth.value
  const h = containerHeight.value
  posX.value = vw - w - 24
  posY.value = vh - h - 24

  window.addEventListener('team-chat:new-message', handleTeamChatNewMessage)

  // 确保有团队列表可供切换
  if (authStore.user?.id && !teamStore.teams.length) {
    teamStore.fetchUserTeams({ userId: authStore.user.id }).catch(() => {})
  }
})

onUnmounted(() => {
  window.removeEventListener('team-chat:new-message', handleTeamChatNewMessage)
  endResize()
  endDrag()
})
</script>

<template>
  <div
    v-if="authStore.isAuthenticated"
    class="team-chat-floating"
    :style="{ top: posY + 'px', left: posX + 'px' }"
  >
    <!-- 悬浮聊天窗：使用自定义的浮窗过渡效果 -->
    <transition name="chat-panel">
      <div
        v-if="isOpen && hasTeamContext"
        class="chat-panel"
        :style="{ width: panelWidth + 'px', height: panelHeight + 'px' }"
      >
        <div class="chat-panel-header" @mousedown.stop="beginDrag">
          <div class="chat-panel-title">
            <el-dropdown
              v-if="teamStore.teams.length"
              @command="handleSwitchTeam"
              trigger="click"
            >
              <span class="name team-switch">
                {{ currentTeamName }}
                <el-icon class="caret"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="team in teamStore.teams"
                    :key="team.id"
                    :command="team.id"
                    :disabled="team.id === activeTeamId"
                  >
                    {{ team.name }}
                    <span v-if="team.id === activeTeamId">（当前）</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <span v-else class="name">
              {{ currentTeamName }}
            </span>
            <span class="tag">团队聊天</span>
          </div>
          <button class="chat-panel-close" @click="isOpen = false" title="关闭">
            ×
          </button>
        </div>

        <!-- 左上角拖拽区域，用于调整大小 -->
        <div class="resize-handle" @mousedown="beginResize" title="拖动调整大小"></div>

        <div class="chat-panel-body">
          <Chat :team-id="activeTeamId!" />
        </div>
      </div>
    </transition>

    <!-- 悬浮按钮（始终贴着浮窗容器的右下角） -->
    <button
      class="chat-fab"
      :class="{ 'chat-fab-disabled': !hasTeamContext }"
      @click="handleToggle"
      @mousedown.stop="beginDrag"
      :title="hasTeamContext ? '打开团队聊天' : '进入团队后可使用团队聊天'"
    >
      <span class="fab-icon">💬</span>
      <span class="fab-label">聊天</span>
      <span v-if="unreadCount > 0" class="fab-badge">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>
  </div>
</template>

<style scoped lang="scss">
.team-chat-floating {
  position: fixed;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.chat-fab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  border: none;
  background: var(--accent-color);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.18);
  font-size: 14px;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 22px rgba(0, 0, 0, 0.22);
  }
}

.chat-fab-disabled {
  background: var(--el-color-info-light-5);
  cursor: pointer;

  &:hover {
    transform: none;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  }
}

.fab-icon {
  font-size: 16px;
}

.fab-label {
  font-weight: 500;
}

.fab-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff4d4f;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-panel {
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  margin-bottom: 8px;
}

.chat-panel-header {
  height: 44px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-subtle);
  background: linear-gradient(
    90deg,
    rgba(64, 158, 255, 0.12),
    rgba(64, 158, 255, 0)
  );
}

.chat-panel-title {
  display: flex;
  align-items: center;
  gap: 6px;

  .name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color);
  }

  .team-switch {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;

    .caret {
      font-size: 12px;
    }
  }

  .tag {
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 999px;
    background: rgba(64, 158, 255, 0.12);
    color: var(--el-color-primary);
  }
}

.chat-panel-close {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  color: var(--text-color-muted);
  transition: all 0.2s ease;

  &:hover {
    background: var(--el-fill-color-light);
    color: var(--text-color);
  }
}

.chat-panel-body {
  flex: 1;
  min-height: 0;
  /* 让内部 Chat.vue 自适应高度 */
  display: flex;
  flex-direction: column;

  /* 让内嵌的 Chat.vue 在浮窗内占满宽度，高度随容器变化 */
  :deep(.chat-card) {
    max-width: 100%;
    margin: 0;
    border-radius: 0;
    box-shadow: none;
  }
}

.resize-handle {
  position: absolute;
  left: 4px;
  top: 48px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid var(--border-subtle);
  background: var(--bg-card);
  cursor: nwse-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: var(--text-color-muted);
}

@media (max-width: 768px) {
  .team-chat-floating {
    right: 12px;
    bottom: 12px;
  }

  .chat-panel {
    width: calc(100vw - 32px) !important;
    height: 70vh !important;
  }
}
</style>

<!-- 悬浮窗专用过渡动画 -->
<style>
.chat-panel-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

.chat-panel-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 1, 1) !important;
}

.chat-panel-enter-from {
  opacity: 0 !important;
  transform: scale(0.85) translateY(20px) !important;
}

.chat-panel-leave-to {
  opacity: 0 !important;
  transform: scale(0.9) translateY(10px) !important;
}
</style>
