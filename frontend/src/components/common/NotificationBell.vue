<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import {
  getUnreadCount,
  getNotifications,
  markAsRead,
  markAllAsRead,
  type Notification
} from '@/api/notification'

const router = useRouter()

const unreadCount = ref(0)
const previewVisible = ref(false)
const loading = ref(false)
const notifications = ref<Notification[]>([])
let timer: number | null = null

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    // request.ts 已把 Result<T> 解包为 T，这里拿到的就是数字
    unreadCount.value = Number(res) || 0
  } catch (error) {
    console.error('加载未读数量失败', error)
    unreadCount.value = 0
  }
}

const loadPreviewNotifications = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getNotifications({ page: 1, size: 5 })
    notifications.value = res?.records || []
  } catch (error) {
    console.error('加载预览通知失败', error)
    notifications.value = []
  } finally {
    loading.value = false
  }
}

const openPreview = async () => {
  await loadPreviewNotifications()
  previewVisible.value = true
}

const closePreview = () => {
  previewVisible.value = false
}

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.isRead) {
    try {
      await markAsRead(notification.id)
      notification.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记已读失败', error)
    }
  }

  // 团队邀请特殊处理：跳转到邀请管理页面
  if (notification.type === 'TEAM_INVITATION') {
    router.push('/team/invitations')
    closePreview()
    return
  }

  if (notification.relatedType && notification.relatedId) {
    switch (notification.relatedType) {
      case 'PROJECT':
        router.push(`/project/${notification.relatedId}`)
        break
      case 'TEAM':
        router.push(`/team/${notification.relatedId}/overview`)
        break
      case 'TASK':
        router.push(`/team/${notification.relatedId}/tasks`)
        break
      case 'COMPETITION':
        router.push(`/competition/${notification.relatedId}`)
        break
      case 'USER':
        router.push(`/profile`)
        break
      default:
        break
    }
  } else {
    router.push('/notifications')
  }

  closePreview()
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => (n.isRead = true))
    unreadCount.value = 0
  } catch (error) {
    console.error('全部标记已读失败', error)
  }
}

const goToNotifications = async () => {
  router.push('/notifications')
  closePreview()
  await loadUnreadCount()
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString('zh-CN')
}

const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    APPLICATION_REVIEWED: '申请审核',
    PROJECT_INVITATION: '项目邀请',
    TEAM_ANNOUNCEMENT: '团队公告',
    SYSTEM: '系统通知',
    SYSTEM_ANNOUNCEMENT: '系统公告',
    SYSTEM_NOTIFICATION: '系统通知',
    COMPETITION_ANNOUNCEMENT: '比赛公告',
    PROJECT_ANNOUNCEMENT: '项目公告',
    COMPETITION_PUBLISHED: '比赛发布',
    TEAM_JOIN_APPLICATION: '入队申请',
    TEAM_JOIN_APPROVED: '入队通过',
    TEAM_JOIN_REJECTED: '入队拒绝',
    TEAM_JOIN_WITHDRAWN: '申请撤回',
    TEAM_MEMBER_JOINED: '成员加入',
    TEAM_MEMBER_LEFT: '成员离开',
    TEAM_MEMBER_REMOVED: '成员移除',
    TEAM_WELCOME: '欢迎加入',
    TEAM_DISBANDED: '团队解散',
    PROJECT_APPLICATION: '项目申请',
    PROJECT_APPLICATION_APPROVED: '申请通过',
    PROJECT_APPLICATION_REJECTED: '申请拒绝',
    PROJECT_PUBLISHED: '项目发布',
    TASK_ASSIGNED: '任务分配',
    TASK_STATUS_CHANGED: '状态变更',
    TASK_COMMENTED: '任务评论',
    TASK_DEADLINE_REMINDER: '截止提醒',
    EVALUATION_REQUEST: '评价请求',
    EVALUATION_RECEIVED: '收到评价',
    FILE_UPLOADED: '文件上传',
    MENTOR_APPLICATION: '导师申请',
    MENTOR_APPLICATION_APPROVED: '导师申请通过',
    MENTOR_APPLICATION_REJECTED: '导师申请拒绝'
  }
  return typeMap[type] || '通知'
}

const getTypeIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    APPLICATION_REVIEWED: '📝',
    PROJECT_INVITATION: '🎯',
    TEAM_ANNOUNCEMENT: '📢',
    SYSTEM: '⚙️',
    SYSTEM_ANNOUNCEMENT: '📢',
    SYSTEM_NOTIFICATION: '⚙️',
    COMPETITION_ANNOUNCEMENT: '🏆',
    PROJECT_ANNOUNCEMENT: '📋',
    COMPETITION_PUBLISHED: '🏆',
    TEAM_JOIN_APPLICATION: '👥',
    TEAM_JOIN_APPROVED: '✅',
    TEAM_JOIN_REJECTED: '❌',
    TEAM_JOIN_WITHDRAWN: '↩️',
    TEAM_MEMBER_JOINED: '👋',
    TEAM_MEMBER_LEFT: '👋',
    TEAM_MEMBER_REMOVED: '🚫',
    TEAM_WELCOME: '🎉',
    TEAM_DISBANDED: '💔',
    PROJECT_APPLICATION: '📋',
    PROJECT_APPLICATION_APPROVED: '✅',
    PROJECT_APPLICATION_REJECTED: '❌',
    PROJECT_PUBLISHED: '🚀',
    TASK_ASSIGNED: '📌',
    TASK_STATUS_CHANGED: '🔄',
    TASK_COMMENTED: '💬',
    TASK_DEADLINE_REMINDER: '⏰',
    EVALUATION_REQUEST: '⭐',
    EVALUATION_RECEIVED: '🎖️',
    FILE_UPLOADED: '📎',
    MENTOR_APPLICATION: '👨‍🏫',
    MENTOR_APPLICATION_APPROVED: '✅',
    MENTOR_APPLICATION_REJECTED: '❌'
  }
  return iconMap[type] || '📬'
}

onMounted(() => {
  loadUnreadCount()
  timer = window.setInterval(loadUnreadCount, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="notification-bell" @click.stop>
    <el-popover
      placement="bottom"
      trigger="hover"
      width="360"
      :show-arrow="true"
      :visible="previewVisible"
      @show="openPreview"
      @hide="closePreview"
    >
      <template #reference>
        <div class="bell-ref" @click="goToNotifications">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
            <el-icon class="bell-icon" :size="22">
              <Bell />
            </el-icon>
          </el-badge>
        </div>
      </template>

      <div class="preview">
        <div class="preview-header">
          <div class="title">通知</div>
          <el-button
            text
            size="small"
            :disabled="unreadCount === 0"
            @click="handleMarkAllRead"
          >
            全部已读
          </el-button>
        </div>

        <el-scrollbar max-height="360">
          <div v-if="loading" class="loading">加载中...</div>

          <div v-else-if="notifications.length === 0" class="empty">暂无通知</div>

          <div v-else class="list">
            <div
              v-for="n in notifications"
              :key="n.id"
              class="item"
              :class="{ unread: !n.isRead }"
              @click="handleNotificationClick(n)"
            >
              <div class="icon">{{ getTypeIcon(n.type) }}</div>
              <div class="content">
                <div class="row1">
                  <div class="type">{{ getTypeText(n.type) }}</div>
                  <div class="time">{{ formatTime(n.createdAt) }}</div>
                </div>
                <div class="title">{{ n.title }}</div>
                <div class="text">{{ n.content }}</div>
              </div>
            </div>
          </div>
        </el-scrollbar>

        <div class="preview-footer">
          <el-button text size="small" @click="goToNotifications">查看全部</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<style scoped lang="scss">
.notification-bell {
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: var(--el-fill-color-light);
  }

  .bell-icon {
    color: var(--text-color);
  }

  :deep(.el-badge__content) {
    border: 2px solid var(--card-bg);
  }
}

.bell-ref {
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .title {
    font-weight: 700;
    font-size: 14px;
    color: var(--text-color);
  }
}

.loading,
.empty {
  padding: 14px 8px;
  color: var(--text-color-muted);
  font-size: 13px;
}

.list {
  display: flex;
  flex-direction: column;
}

.item {
  display: flex;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.unread {
    background: rgba(64, 158, 255, 0.08);
  }

  .icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--bg-elevated);
    flex-shrink: 0;
  }

  .content {
    flex: 1;
    min-width: 0;

    .row1 {
      display: flex;
      justify-content: space-between;
      gap: 10px;
      margin-bottom: 2px;

      .type {
        font-size: 12px;
        font-weight: 600;
        color: var(--el-color-primary);
      }

      .time {
        font-size: 12px;
        color: var(--text-color-muted);
        flex-shrink: 0;
      }
    }

    .title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-color);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 2px;
    }

    .text {
      font-size: 12px;
      color: var(--text-color-secondary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.preview-footer {
  padding-top: 6px;
  border-top: 1px solid var(--border-subtle);
  text-align: center;
}
</style>
