<template>
  <div class="task-assignees">
    <div class="assignees-header">
      <h4>负责人</h4>
      <el-button 
        type="primary" 
        size="small" 
        :icon="Plus" 
        @click="showAddDialog = true"
        :disabled="loading"
      >
        添加
      </el-button>
    </div>

    <!-- 负责人列表 -->
    <div v-if="assignees.length > 0" class="assignees-list">
      <div 
        v-for="assignee in assignees" 
        :key="assignee.id" 
        class="assignee-item"
      >
        <div class="assignee-info">
          <el-avatar 
            :size="32" 
            :src="assignee.avatar" 
            :alt="assignee.userName"
          >
            {{ assignee.userName?.charAt(0) }}
          </el-avatar>
          <div class="assignee-details">
            <span class="assignee-name">{{ assignee.userName }}</span>
            <span class="assignee-time">{{ formatTime(assignee.assignedAt) }}</span>
          </div>

    <!-- 智能推荐负责人 -->
    <div class="recommend-section">
      <div class="recommend-header">
        <span class="recommend-title">智能推荐负责人</span>
        <el-button
          type="primary"
          link
          size="small"
          :loading="recommendLoading"
          @click="loadRecommendations"
        >
          刷新推荐
        </el-button>
      </div>

      <div v-if="recommended.length > 0" class="recommend-list">
        <div
          v-for="item in recommended"
          :key="item.userId"
          class="recommend-item"
        >
          <div class="recommend-info">
            <span class="name">{{ item.username || `用户#${item.userId}` }}</span>
            <span class="score">
              匹配度 {{ Math.round((item.score || 0) * 100) }}%
            </span>
          </div>
          <el-button
            type="primary"
            size="small"
            @click="handleAddRecommended(item)"
          >
            添加为负责人
          </el-button>
        </div>
      </div>

      <el-empty
        v-else
        :image-size="60"
        :description="recommendLoading ? '正在计算推荐...' : '暂无推荐，可尝试刷新或完善团队成员资料'"
      />
    </div>
        </div>
        <el-button
          type="danger"
          size="small"
          :icon="Close"
          circle
          @click="handleRemove(assignee)"
          :loading="removingId === assignee.userId"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty 
      v-else 
      description="暂无负责人" 
      :image-size="80"
    />

    <!-- 添加负责人对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加负责人"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-select
        v-model="selectedUserId"
        placeholder="请选择团队成员"
        filterable
        style="width: 100%"
      >
        <el-option
          v-for="member in availableMembers"
          :key="member.userId"
          :label="member.username"
          :value="member.userId"
        >
          <div class="member-option">
            <el-avatar :size="24" :src="member.avatar">
              {{ member.username?.charAt(0) }}
            </el-avatar>
            <span>{{ member.username }}</span>
          </div>
        </el-option>
      </el-select>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleAdd"
          :loading="adding"
          :disabled="!selectedUserId"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { request } from '@/utils/request'
import { getTeamMembers, getTaskAssigneeRecommendations } from '@/api/team'
import type { TeamMember } from '@/types/team'
import type { CandidateMatchItem } from '@/utils/fieldNormalizer'

interface TaskAssignee {
  id: number
  taskId: number
  userId: number
  userName: string
  avatar?: string
  assignedAt: string
}

const props = defineProps<{
  taskId: number
  teamId: number
}>()

const emit = defineEmits<{
  update: []
}>()

// State
const assignees = ref<TaskAssignee[]>([])
const teamMembers = ref<TeamMember[]>([])
const loading = ref(false)
const adding = ref(false)
const removingId = ref<number | null>(null)
const showAddDialog = ref(false)
const selectedUserId = ref<number | null>(null)
const recommendLoading = ref(false)
const recommended = ref<CandidateMatchItem[]>([])

// Computed
const availableMembers = computed(() => {
  const assignedUserIds = new Set(assignees.value.map(a => a.userId))
  return teamMembers.value.filter(m => !assignedUserIds.has(m.userId))
})

// Methods
const fetchAssignees = async () => {
  loading.value = true
  try {
    const response = await request.get<TaskAssignee[]>(
      `/tasks/${props.taskId}/assignees`
    )
    assignees.value = Array.isArray(response) ? response : []
  } catch (error: any) {
    console.error('Failed to fetch assignees:', error)
    ElMessage.error(error.message || '获取负责人列表失败')
  } finally {
    loading.value = false
  }
}

const fetchTeamMembers = async () => {
  try {
    teamMembers.value = await getTeamMembers(props.teamId, false)
  } catch (error: any) {
    console.error('Failed to fetch team members:', error)
    ElMessage.error('获取团队成员失败')
  }
}

const handleAdd = async () => {
  if (!selectedUserId.value) return
  await doAddAssignee(selectedUserId.value, true)
}

const doAddAssignee = async (userId: number, closeDialog: boolean) => {
  adding.value = true
  try {
    const response = await request.post<TaskAssignee>(
      `/tasks/${props.taskId}/assignees`,
      { userId }
    )
    
    if (response) {
      assignees.value.push(response)
      ElMessage.success('添加负责人成功')
      if (closeDialog) {
        showAddDialog.value = false
        selectedUserId.value = null
      }
      emit('update')
    }
  } catch (error: any) {
    console.error('Failed to add assignee:', error)
    ElMessage.error(error.message || '添加负责人失败')
  } finally {
    adding.value = false
  }
}

const handleAddRecommended = async (candidate: CandidateMatchItem) => {
  if (!candidate || !candidate.userId) return
  await doAddAssignee(candidate.userId, false)
  // 添加后从推荐列表中移除该用户
  recommended.value = recommended.value.filter(r => r.userId !== candidate.userId)
}

const handleRemove = async (assignee: TaskAssignee) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除负责人 ${assignee.userName} 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    removingId.value = assignee.userId
    try {
      await request.delete(
        `/tasks/${props.taskId}/assignees/${assignee.userId}`
      )
      
      assignees.value = assignees.value.filter(a => a.userId !== assignee.userId)
      ElMessage.success('移除负责人成功')
      emit('update')
    } catch (error: any) {
      console.error('Failed to remove assignee:', error)
      ElMessage.error(error.message || '移除负责人失败')
    } finally {
      removingId.value = null
    }
  } catch {
    // User cancelled
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes === 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const loadRecommendations = async () => {
  recommendLoading.value = true
  try {
    const data = await getTaskAssigneeRecommendations(props.taskId, 5)
    const assignedUserIds = new Set(assignees.value.map(a => a.userId))
    recommended.value = (data || []).filter(item => !assignedUserIds.has(item.userId))
  } catch (error: any) {
    console.error('Failed to load assignee recommendations:', error)
    ElMessage.error(error.message || '加载负责人推荐失败')
  } finally {
    recommendLoading.value = false
  }
}

// Lifecycle
onMounted(() => {
  fetchAssignees()
  fetchTeamMembers()
})

// Expose methods for parent component
defineExpose({
  refresh: fetchAssignees
})
</script>

<style scoped lang="scss">
.task-assignees {
  .assignees-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
      color: var(--text-color);
    }
  }

  .assignees-list {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .assignee-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      background-color: var(--bg-elevated-soft);
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        background-color: var(--accent-soft);
      }

      .assignee-info {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;

        .assignee-details {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .assignee-name {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-color);
          }

          .assignee-time {
            font-size: 12px;
            color: var(--text-color-muted);
          }
        }
      }
    }
  }

  .member-option {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.recommend-section {
  margin-top: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color-light);

  .recommend-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .recommend-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-color);
    }
  }

  .recommend-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .recommend-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 10px;
      border-radius: 8px;
      background: var(--bg-elevated-soft);

      .recommend-info {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .name {
          font-size: 13px;
          font-weight: 500;
          color: var(--text-color);
        }

        .score {
          font-size: 12px;
          color: var(--text-color-muted);
        }
      }
    }
  }
}
</style>
