<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifications, markAsRead, markAllAsRead, deleteNotification, type Notification } from '@/api/notification'
import { useRouter } from 'vue-router'
import { getRecommendedCompetitions, getHotCompetitions, type Competition } from '@/api/competition'

const router = useRouter()
const loading = ref(false)
const notifications = ref<Notification[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const activeTab = ref<'all' | 'unread'>('all')
const typeFilter = ref<string>('')
const sortBy = ref<'createdAt' | 'type'>('createdAt')  // 排序字段
const sortOrder = ref<'asc' | 'desc'>('desc')           // 排序方向

const recommendedCompetitions = ref<Competition[]>([])
const hotCompetitions = ref<Competition[]>([])

const loadNotifications = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: size.value,
      isRead: activeTab.value === 'unread' ? false : undefined,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }
    if (typeFilter.value) {
      params.type = typeFilter.value
    }
    const res = await getNotifications(params)
    // 响应拦截器已经返回了 data，所以直接访问 records
    if (res) {
      notifications.value = res.records || []
      total.value = res.total || 0
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const loadCompetitionShortcuts = async () => {
  try {
    const token = window.localStorage.getItem('token')
    if (token) {
      const res = await getRecommendedCompetitions({ page: 1, size: 3 })
      recommendedCompetitions.value = res.records || []
    } else {
      recommendedCompetitions.value = []
    }
  } catch {
    recommendedCompetitions.value = []
  }

  try {
    hotCompetitions.value = await getHotCompetitions(3)
  } catch {
    hotCompetitions.value = []
  }
}

const handleRead = async (notification: Notification) => {
  if (!notification.isRead) {
    try {
      await markAsRead(notification.id)
      notification.isRead = true
      ElMessage.success('已标记为已读')
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }
  
  // 跳转到相关页面
  if (notification.relatedType && notification.relatedId) {
    switch (notification.relatedType) {
      case 'PROJECT':
        router.push(`/project/${notification.relatedId}`)
        break
      case 'TEAM':
        // 如果是导师申请通知，跳转到导师申请页面
        if (notification.type && notification.type.includes('MENTOR_APPLICATION')) {
          router.push('/mentor/applications')
        } else {
          router.push(`/team/${notification.relatedId}/overview`)
        }
        break
      case 'TASK':
        // 任务通知跳转到任务看板
        router.push(`/team/${notification.relatedId}/tasks`)
        break
      case 'COMPETITION':
        router.push(`/competition/${notification.relatedId}`)
        break
      case 'USER':
        router.push(`/profile`)
        break
    }
  } else if (isCompetitionNotification(notification.type)) {
    // 比赛相关通知，尝试从内容或标题中提取比赛ID
    const competitionId = extractCompetitionId(notification)
    if (competitionId) {
      router.push(`/competition/${competitionId}`)
    }
  } else if (notification.type && notification.type.includes('MENTOR_APPLICATION')) {
    // 导师申请通知，跳转到导师申请管理页面
    router.push('/mentor/applications')
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    total.value--
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleOpenCompetition = (id: number) => {
  router.push(`/competition/${id}`)
}

const handleTabChange = () => {
  page.value = 1
  loadNotifications()
}

const handleTypeFilterChange = () => {
  page.value = 1
  loadNotifications()
}

const handlePageChange = () => {
  loadNotifications()
}

const handleSort = (field: 'createdAt' | 'type') => {
  if (sortBy.value === field) {
    // 切换排序方向
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    // 新字段，默认降序
    sortBy.value = field
    sortOrder.value = 'desc'
  }
  page.value = 1
  loadNotifications()
}

const getSortIcon = (field: string) => {
  if (sortBy.value !== field) return '↕'
  return sortOrder.value === 'asc' ? '↑' : '↓'
}

const getNotificationIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    'APPLICATION_REVIEWED': '📝',
    'PROJECT_INVITATION': '🎯',
    'TEAM_ANNOUNCEMENT': '📢',
    'SYSTEM': '⚙️',
    'SYSTEM_ANNOUNCEMENT': '📢',
    'SYSTEM_NOTIFICATION': '⚙️',
    'COMPETITION_ANNOUNCEMENT': '🏆',
    'PROJECT_ANNOUNCEMENT': '📋',
    // 比赛相关
    'COMPETITION_PUBLISHED': '🏆',
    // 团队相关
    'TEAM_JOIN_APPLICATION': '👥',
    'TEAM_JOIN_APPROVED': '✅',
    'TEAM_JOIN_REJECTED': '❌',
    'TEAM_JOIN_WITHDRAWN': '↩️',
    'TEAM_MEMBER_JOINED': '👋',
    'TEAM_MEMBER_LEFT': '👋',
    'TEAM_MEMBER_REMOVED': '🚫',
    'TEAM_WELCOME': '🎉',
    'TEAM_DISBANDED': '💔',
    // 项目相关
    'PROJECT_APPLICATION': '📋',
    'PROJECT_APPLICATION_APPROVED': '✅',
    'PROJECT_APPLICATION_REJECTED': '❌',
    'PROJECT_PUBLISHED': '🚀',
    // 任务相关
    'TASK_ASSIGNED': '📌',
    'TASK_STATUS_CHANGED': '🔄',
    'TASK_COMMENTED': '💬',
    'TASK_DEADLINE_REMINDER': '⏰',
    // 评价相关
    'EVALUATION_REQUEST': '⭐',
    'EVALUATION_RECEIVED': '🎖️',
    // 文件相关
    'FILE_UPLOADED': '📎',
    // 导师申请相关
    'MENTOR_APPLICATION': '👨‍🏫',
    'MENTOR_APPLICATION_APPROVED': '✅',
    'MENTOR_APPLICATION_REJECTED': '❌'
  }
  return iconMap[type] || '📬'
}

const getNotificationTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'APPLICATION_REVIEWED': '申请审核',
    'PROJECT_INVITATION': '项目邀请',
    'TEAM_ANNOUNCEMENT': '团队公告',
    'SYSTEM': '系统通知',
    'SYSTEM_ANNOUNCEMENT': '系统公告',
    'SYSTEM_NOTIFICATION': '系统通知',
    'COMPETITION_ANNOUNCEMENT': '比赛公告',
    'PROJECT_ANNOUNCEMENT': '项目公告',
    // 比赛相关
    'COMPETITION_PUBLISHED': '比赛发布',
    // 团队相关
    'TEAM_JOIN_APPLICATION': '入队申请',
    'TEAM_JOIN_APPROVED': '入队通过',
    'TEAM_JOIN_REJECTED': '入队拒绝',
    'TEAM_JOIN_WITHDRAWN': '申请撤回',
    'TEAM_MEMBER_JOINED': '成员加入',
    'TEAM_MEMBER_LEFT': '成员离开',
    'TEAM_MEMBER_REMOVED': '成员移除',
    'TEAM_WELCOME': '欢迎加入',
    'TEAM_DISBANDED': '团队解散',
    // 项目相关
    'PROJECT_APPLICATION': '项目申请',
    'PROJECT_APPLICATION_APPROVED': '申请通过',
    'PROJECT_APPLICATION_REJECTED': '申请拒绝',
    'PROJECT_PUBLISHED': '项目发布',
    // 任务相关
    'TASK_ASSIGNED': '任务分配',
    'TASK_STATUS_CHANGED': '状态变更',
    'TASK_COMMENTED': '任务评论',
    'TASK_DEADLINE_REMINDER': '截止提醒',
    // 评价相关
    'EVALUATION_REQUEST': '评价请求',
    'EVALUATION_RECEIVED': '收到评价',
    // 文件相关
    'FILE_UPLOADED': '文件上传',
    // 导师申请相关
    'MENTOR_APPLICATION': '导师申请',
    'MENTOR_APPLICATION_APPROVED': '导师申请通过',
    'MENTOR_APPLICATION_REJECTED': '导师申请拒绝'
  }
  return typeMap[type] || '通知'
}

// 判断是否为比赛相关通知
const isCompetitionNotification = (type: string): boolean => {
  return type.includes('COMPETITION') || 
         type.includes('TEAM_JOIN') || 
         type.includes('MENTOR_APPLICATION')
}

// 从通知中提取比赛ID（如果可能）
const extractCompetitionId = (notification: Notification): number | null => {
  // 如果 relatedType 是 COMPETITION，直接返回 relatedId
  if (notification.relatedType === 'COMPETITION' && notification.relatedId) {
    return notification.relatedId
  }
  // 否则返回 null，让用户手动点击
  return null
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

onMounted(() => {
  loadNotifications().then(() => {
    // 自动标记全部已读
    handleMarkAllRead();
  });
  loadCompetitionShortcuts();
});
</script>

<template>
  <div class="notification-center">
    <div class="header">
      <h1 class="title">通知中心</h1>
      <el-button type="primary" plain @click="handleMarkAllRead">
        全部标记为已读
      </el-button>
    </div>

    <el-card
      v-if="recommendedCompetitions.length > 0 || hotCompetitions.length > 0"
      class="competition-shortcuts"
      shadow="never"
    >
      <div class="shortcuts-header">
        <h3>比赛快捷入口</h3>
        <span class="hint">
          推荐基于你的技能标签与浏览热度的简单规则，仅作参考
        </span>
      </div>
      <div class="shortcuts-body">
        <div v-if="recommendedCompetitions.length > 0" class="shortcut-block">
          <div class="block-title">为你推荐</div>
          <div class="shortcut-list">
            <el-button
              v-for="c in recommendedCompetitions"
              :key="c.id"
              type="primary"
              plain
              size="small"
              @click="handleOpenCompetition(c.id)"
            >
              {{ c.name }}
            </el-button>
          </div>
        </div>
        <div v-if="hotCompetitions.length > 0" class="shortcut-block">
          <div class="block-title">热门比赛</div>
          <div class="shortcut-list">
            <el-button
              v-for="c in hotCompetitions"
              :key="c.id"
              type="danger"
              plain
              size="small"
              @click="handleOpenCompetition(c.id)"
            >
              {{ c.name }}
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="类型筛选">
          <el-select v-model="typeFilter" placeholder="全部类型" clearable @change="handleTypeFilterChange" style="width: 200px">
            <el-option label="全部类型" value="" />
            <el-option label="系统公告" value="SYSTEM_ANNOUNCEMENT" />
            <el-option label="比赛相关" value="COMPETITION" />
            <el-option label="项目相关" value="PROJECT" />
            <el-option label="团队相关" value="TEAM" />
            <el-option label="入队申请" value="TEAM_JOIN" />
            <el-option label="导师申请" value="MENTOR_APPLICATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序方式">
          <el-button-group>
            <el-button 
              :type="sortBy === 'createdAt' ? 'primary' : 'default'"
              @click="handleSort('createdAt')"
            >
              时间 {{ getSortIcon('createdAt') }}
            </el-button>
            <el-button 
              :type="sortBy === 'type' ? 'primary' : 'default'"
              @click="handleSort('type')"
            >
              类型 {{ getSortIcon('type') }}
            </el-button>
          </el-button-group>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" style="margin-top: 16px">
      <el-tab-pane label="全部通知" name="all"></el-tab-pane>
      <el-tab-pane label="未读通知" name="unread"></el-tab-pane>
    </el-tabs>

    <div v-loading="loading" class="notification-list">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item"
        :class="{ 
          unread: !notification.isRead,
          'competition-related': isCompetitionNotification(notification.type)
        }"
        @click="handleRead(notification)"
      >
        <div class="notification-icon">
          {{ getNotificationIcon(notification.type) }}
        </div>
        
        <div class="notification-content">
          <div class="notification-header">
            <span class="notification-type">
              {{ getNotificationTypeText(notification.type) }}
            </span>
            <span class="notification-time">
              {{ formatTime(notification.createdAt) }}
            </span>
          </div>
          
          <h3 class="notification-title">{{ notification.title }}</h3>
          <p class="notification-text">{{ notification.content }}</p>
        </div>

        <div class="notification-actions">
          <el-button
            v-if="!notification.isRead"
            type="primary"
            size="small"
            text
            @click.stop="handleRead(notification)"
          >
            标为已读
          </el-button>
          <el-button
            type="danger"
            size="small"
            text
            @click.stop="handleDelete(notification.id)"
          >
            删除
          </el-button>
        </div>
      </div>

      <div v-if="!loading && notifications.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无通知</p>
      </div>
    </div>

    <div v-if="total > size" class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.notification-center {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.competition-shortcuts {
  margin-bottom: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);

  .shortcuts-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 8px;

    h3 {
      margin: 0;
      font-size: 16px;
      color: var(--text-color);
    }

    .hint {
      font-size: 12px;
      color: var(--text-color-muted);
    }
  }

  .shortcuts-body {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .shortcut-block {
    .block-title {
      font-size: 13px;
      color: var(--text-color-secondary);
      margin-bottom: 4px;
    }

    .shortcut-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .title {
    font-size: 28px;
    font-weight: 700;
    margin: 0;
  }
}

.notification-list {
  min-height: 400px;
  margin-top: 20px;
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &.unread {
    border-left: 4px solid var(--el-color-primary);
    background: rgba(64, 158, 255, 0.05);
  }

  &.competition-related {
    border-left: 4px solid #f56c6c;
    background: rgba(245, 108, 108, 0.05);
    
    &.unread {
      border-left: 4px solid #f56c6c;
      background: rgba(245, 108, 108, 0.1);
    }
  }
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.filter-card {
  margin-bottom: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.filter-form {
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.notification-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  
  .notification-type {
    font-size: 12px;
    color: var(--el-color-primary);
    font-weight: 600;
  }
  
  .notification-time {
    font-size: 12px;
    color: var(--text-color-muted);
  }
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-color);
}

.notification-text {
  font-size: 14px;
  color: var(--text-color-secondary);
  margin: 0;
  line-height: 1.6;
}

.notification-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-color-muted);
  
  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.5;
  }
  
  p {
    font-size: 16px;
    margin: 0;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
