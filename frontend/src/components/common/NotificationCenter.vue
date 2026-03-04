<template>
  <div class="notification-center" :class="{ 'is-preview': isPreview }">
    <div class="notification-panel">
      <div class="panel-header" v-if="!isPreview">
        <h4>通知中心</h4>
        <el-button text size="small" @click="handleMarkAllRead">全部已读</el-button>
      </div>

      <el-scrollbar max-height="400px">
        <div class="notification-list">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            :class="['notification-item', { unread: !notification.isRead }]"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon" :class="getNotificationType(notification.type)">
              <el-icon>
                <component :is="getNotificationIcon(notification.type)" />
              </el-icon>
            </div>

            <div class="notification-content">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-text">{{ notification.content }}</div>
              <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
            </div>

            <div v-if="!notification.isRead" class="unread-dot"></div>
          </div>

          <el-empty
            v-if="notifications.length === 0"
            description="暂无通知"
            :image-size="60"
          />
        </div>
      </el-scrollbar>

      <div class="panel-footer" v-if="!isPreview">
        <el-button text size="small" @click="handleViewAll">查看全部</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineProps, defineEmits } from 'vue';
import { useRouter } from 'vue-router';
import { getNotifications, markAsRead, markAllAsRead, type Notification } from '@/api/notification';
import { ElMessage } from 'element-plus';
import {
  Bell,
  CircleCheck,
  Warning,
  ChatDotRound,
  DocumentChecked,
  Promotion,
} from '@element-plus/icons-vue';

const props = defineProps({
  isPreview: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['refresh']);

const router = useRouter();
const notifications = ref<Notification[]>([]);

const loadNotifications = async () => {
  try {
    const params = props.isPreview ? { page: 1, size: 5 } : { page: 1, size: 20 };
    const res = await getNotifications(params);
    // request.ts 已把 Result<T> 解包为 T，这里拿到的就是 Page<Notification>
    notifications.value = (res as any)?.records || [];
  } catch (error) {
    console.error('获取通知列表失败', error);
  }
};

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.isRead) {
    try {
      await markAsRead(notification.id);
      notification.isRead = true; // Optimistic update
      emit('refresh');
    } catch (error) {
      console.error('标记已读失败', error);
    }
  }
  
  // 根据通知类型进行跳转
  if (notification.type === 'TEAM_INVITATION') {
    // 团队邀请 → 跳转到邀请管理页面
    router.push('/team/invitations');
  } else if (notification.relatedType && notification.relatedId) {
    // 其他类型根据 relatedType 跳转
    const routeMap: Record<string, string> = {
      'TEAM': `/team/${notification.relatedId}`,
      'PROJECT': `/project/${notification.relatedId}`,
      'TASK': `/task/${notification.relatedId}`,
      'USER': `/user/${notification.relatedId}`
    };
    
    const route = routeMap[notification.relatedType];
    if (route) {
      router.push(route);
    }
  }
};

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead();
    notifications.value.forEach(n => (n.isRead = true));
    emit('refresh');
    ElMessage.success('全部已读');
  } catch (error) {
    console.error('全部标记已读失败', error);
    ElMessage.error('操作失败');
  }
};

const handleViewAll = () => {
  router.push('/notifications');
};

const getNotificationType = (type: string) => {
  const types: Record<string, string> = {
    APPLICATION_RESULT: 'success',
    TASK_ASSIGNED: 'primary',
    DEADLINE_REMINDER: 'warning',
    EVALUATION_REQUEST: 'info',
    SYSTEM_ANNOUNCEMENT: 'danger',
  };
  return types[type] || 'info';
};

const getNotificationIcon = (type: string) => {
  const icons: Record<string, any> = {
    APPLICATION_RESULT: CircleCheck,
    TASK_ASSIGNED: DocumentChecked,
    DEADLINE_REMINDER: Warning,
    EVALUATION_REQUEST: ChatDotRound,
    SYSTEM_ANNOUNCEMENT: Promotion,
  };
  return icons[type] || Bell;
};

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`;

  return date.toLocaleDateString('zh-CN');
};

onMounted(() => {
  loadNotifications();
});
</script>

<style scoped lang="scss">
.notification-center {
  &.is-preview {
    .panel-header,
    .panel-footer {
      display: none;
    }
  }

  .notification-panel {
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 12px 12px;
      border-bottom: 1px solid #ebeef5;

      h4 {
        margin: 0;
        font-size: 16px;
      }
    }

    .notification-list {
      .notification-item {
        display: flex;
        gap: 12px;
        padding: 12px;
        cursor: pointer;
        border-bottom: 1px solid #f5f7fa;
        position: relative;
        transition: background 0.3s;

        &:last-child {
          border-bottom: none;
        }

        &:hover {
          background: #f5f7fa;
        }

        &.unread {
          background: #ecf5ff;
        }

        .notification-icon {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          flex-shrink: 0;

          &.success {
            background: #67c23a;
          }
          &.primary {
            background: #409eff;
          }
          &.warning {
            background: #e6a23c;
          }
          &.danger {
            background: #f56c6c;
          }
          &.info {
            background: #909399;
          }
        }

        .notification-content {
          flex: 1;
          min-width: 0;

          .notification-title {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
            margin-bottom: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .notification-text {
            font-size: 13px;
            color: #606266;
            margin-bottom: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .notification-time {
            font-size: 12px;
            color: #909399;
          }
        }

        .unread-dot {
          width: 8px;
          height: 8px;
          background: #f56c6c;
          border-radius: 50%;
          position: absolute;
          top: 16px;
          right: 12px;
        }
      }
    }

    .panel-footer {
      padding-top: 8px;
      text-align: center;
      border-top: 1px solid #ebeef5;
    }
  }
}
</style>