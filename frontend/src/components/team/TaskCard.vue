<script setup lang="ts">
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { MoreFilled, Calendar, ChatDotRound, Paperclip } from '@element-plus/icons-vue'
import type { Task } from '../../types/team'

const props = defineProps<{
  task: Task
}>()

const emit = defineEmits<{
  click: [task: Task]
  update: [task: Task]
  delete: [taskId: number]
}>()

const priorityClass = computed(() => {
  return `priority-${props.task.priority.toLowerCase()}`
})

// 检查任务是否逾期
const isOverdue = computed(() => {
  if (!props.task.deadline || props.task.status === 'DONE') {
    return false
  }
  const deadline = new Date(props.task.deadline)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return deadline < today
})

// 显示的负责人（最多3个）
const displayedAssignees = computed(() => {
  if (!props.task.assignees) return []
  return props.task.assignees.slice(0, 3)
})

// 剩余负责人数量
const remainingAssignees = computed(() => {
  if (!props.task.assignees) return 0
  return Math.max(0, props.task.assignees.length - 3)
})

const getPriorityType = (priority: string) => {
  const types: Record<string, any> = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
  }
  return types[priority] || 'info'
}

const getPriorityText = (priority: string) => {
  const texts: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
  }
  return texts[priority] || priority
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const handleCardClick = () => {
  emit('click', props.task)
}

const handleEdit = () => {
  // TODO: 打开编辑对话框
  emit('update', props.task)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    emit('delete', props.task.id)
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <!-- Performance optimization: Use v-memo to cache rendering when task data hasn't changed -->
  <el-card 
    v-memo="[task.id, task.title, task.status, task.priority, task.deadline, task.assignees?.length, task.commentCount, task.attachmentCount]"
    class="task-card" 
    :class="[priorityClass, { 'overdue': isOverdue }]" 
    shadow="hover" 
    @click="handleCardClick"
  >
    <div class="task-header">
      <h4>{{ task.title }}</h4>
      <el-dropdown trigger="click" @click.native.stop>
        <el-icon class="more-icon" @click.stop><MoreFilled /></el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleEdit">编辑</el-dropdown-item>
            <el-dropdown-item @click="handleDelete">删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <p class="task-description">{{ task.description }}</p>

    <div class="task-meta">
      <!-- 负责人头像 - Performance: Lazy load images -->
      <div v-if="task.assignees && task.assignees.length > 0" class="assignees">
        <el-avatar
          v-for="(assignee, index) in displayedAssignees"
          :key="assignee.id"
          :src="assignee.avatar"
          :size="24"
          class="assignee-avatar"
          :style="{ zIndex: displayedAssignees.length - index }"
          loading="lazy"
        >
          {{ assignee.userName?.charAt(0) || '?' }}
        </el-avatar>
        <span v-if="remainingAssignees > 0" class="remaining-count">
          +{{ remainingAssignees }}
        </span>
      </div>

      <!-- 评论和附件数量 -->
      <div class="counts">
        <span v-if="task.commentCount && task.commentCount > 0" class="count-item">
          <el-icon><ChatDotRound /></el-icon>
          {{ task.commentCount }}
        </span>
        <span v-if="task.attachmentCount && task.attachmentCount > 0" class="count-item">
          <el-icon><Paperclip /></el-icon>
          {{ task.attachmentCount }}
        </span>
      </div>
    </div>

    <div class="task-footer">
      <el-tag :type="getPriorityType(task.priority)" size="small">
        {{ getPriorityText(task.priority) }}
      </el-tag>
      <span v-if="task.deadline" class="deadline" :class="{ 'overdue-text': isOverdue }">
        <el-icon><Calendar /></el-icon>
        {{ formatDate(task.deadline) }}
      </span>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.task-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: scale(1.02);
  }

  &.priority-high {
    border-left: 4px solid var(--el-color-danger);
  }

  &.priority-medium {
    border-left: 4px solid var(--el-color-warning);
  }

  &.priority-low {
    border-left: 4px solid var(--text-color-secondary);
  }

  &.overdue {
    border-left-color: var(--el-color-danger);
    background-color: var(--el-color-danger-light-9);
  }

  .task-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 8px;

    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
      flex: 1;
    }

    .more-icon {
      cursor: pointer;
      font-size: 18px;
      color: var(--text-color-muted);

      &:hover {
        color: var(--el-color-primary);
      }
    }
  }

  .task-description {
    font-size: 13px;
    color: var(--text-color-secondary);
    margin: 8px 0;
    line-height: 1.6;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .task-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 12px 0;
    min-height: 24px;

    .assignees {
      display: flex;
      align-items: center;
      gap: 4px;

      .assignee-avatar {
        margin-left: -8px;
        border: 2px solid var(--bg-card);
        box-shadow: var(--shadow-card);
        font-size: 12px;

        &:first-child {
          margin-left: 0;
        }
      }

      .remaining-count {
        margin-left: 4px;
        font-size: 12px;
        color: var(--text-color-muted);
        font-weight: 500;
      }
    }

    .counts {
      display: flex;
      align-items: center;
      gap: 12px;

      .count-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--text-color-muted);

        .el-icon {
          font-size: 14px;
        }
      }
    }
  }

  .task-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    font-size: 12px;

    .deadline {
      display: flex;
      align-items: center;
      gap: 4px;
      color: var(--text-color-muted);

      &.overdue-text {
        color: var(--el-color-danger);
        font-weight: 500;
      }
    }
  }
}
</style>

