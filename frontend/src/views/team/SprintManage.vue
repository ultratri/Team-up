<template>
  <div class="sprint-manage">
    <div class="board-header">
      <h2>Sprint管理</h2>
      <button class="primary-btn" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        <span>创建Sprint</span>
      </button>
    </div>

    <!-- Sprint列表 -->
    <div class="sprint-grid" v-loading="loading">
      <div 
        v-for="sprint in sprints" 
        :key="sprint.id"
        class="sprint-card"
        :class="`status-${sprint.status.toLowerCase()}`"
      >
        <div class="sprint-header">
          <div class="sprint-title">
            <h3>{{ sprint.name }}</h3>
            <el-tag :type="getStatusType(sprint.status)" size="small">
              {{ getStatusText(sprint.status) }}
            </el-tag>
          </div>
          <el-dropdown @command="(cmd) => handleAction(cmd, sprint)" trigger="click">
            <button class="icon-btn">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item 
                  v-if="sprint.status === 'PLANNING'" 
                  command="start"
                >
                  开始Sprint
                </el-dropdown-item>
                <el-dropdown-item 
                  v-if="sprint.status === 'IN_PROGRESS'" 
                  command="complete"
                >
                  完成Sprint
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <p class="sprint-goal">{{ sprint.goal || '暂无目标描述' }}</p>

        <div class="sprint-dates">
          <el-icon><Calendar /></el-icon>
          <span>{{ formatDate(sprint.startDate) }} ~ {{ formatDate(sprint.endDate) }}</span>
        </div>

        <div class="sprint-stats">
          <div class="stat-item">
            <span class="label">总任务</span>
            <span class="value">{{ sprint.totalTasks || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">已完成</span>
            <span class="value success">{{ sprint.completedTasks || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">进行中</span>
            <span class="value warning">{{ sprint.inProgressTasks || 0 }}</span>
          </div>
        </div>

        <el-progress 
          :percentage="getProgress(sprint)" 
          :status="getProgress(sprint) === 100 ? 'success' : ''"
          :stroke-width="6"
        />

        <div class="sprint-actions">
          <el-button 
            size="small" 
            @click.stop="viewSprintTasks(sprint)"
            style="width: 100%"
          >
            查看任务
          </el-button>
        </div>
      </div>

      <div v-if="sprints.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无Sprint，点击右上角创建第一个Sprint吧！" />
      </div>
    </div>

    <!-- 创建/编辑Sprint对话框 -->
    <el-dialog 
      v-model="showCreateDialog" 
      :title="editingSprintId ? '编辑Sprint' : '创建Sprint'"
      width="500px"
    >
      <el-form :model="sprintForm" label-position="top">
        <el-form-item label="Sprint名称" required>
          <el-input 
            v-model="sprintForm.name" 
            placeholder="例如：Sprint 1"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="Sprint目标">
          <el-input 
            v-model="sprintForm.goal" 
            type="textarea" 
            :rows="3"
            placeholder="描述本次Sprint的目标"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="开始日期" required>
          <el-date-picker 
            v-model="sprintForm.startDate" 
            type="date"
            placeholder="选择开始日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="结束日期" required>
          <el-date-picker 
            v-model="sprintForm.endDate" 
            type="date"
            placeholder="选择结束日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSprint" :loading="saving">
          {{ editingSprintId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Sprint 任务查看对话框 -->
    <el-dialog
      v-model="showTasksDialog"
      title="Sprint 任务"
      width="700px"
    >
      <div v-if="currentSprint">
        <div class="sprint-info-header">
          <h3>{{ currentSprint.name }}</h3>
          <el-tag :type="getStatusType(currentSprint.status)">
            {{ getStatusText(currentSprint.status) }}
          </el-tag>
        </div>
        <p class="sprint-goal-text">{{ currentSprint.goal || '暂无目标描述' }}</p>
        
        <el-divider />
        
        <div class="tasks-hint">
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>
              提示：在任务看板中可以将任务分配到此 Sprint
            </template>
          </el-alert>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Calendar, MoreFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import {
  getTeamSprints,
  createSprint,
  updateSprint,
  deleteSprint,
  startSprint,
  completeSprint,
  type SprintVO,
  type Sprint
} from '@/api/sprint'

const props = defineProps<{
  teamId: number
}>()

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id)

const sprints = ref<SprintVO[]>([])
const loading = ref(false)
const saving = ref(false)
const showCreateDialog = ref(false)
const showTasksDialog = ref(false)
const editingSprintId = ref<number | null>(null)
const currentSprint = ref<SprintVO | null>(null)
const sprintForm = ref({
  name: '',
  goal: '',
  startDate: '',
  endDate: ''
})

const loadSprints = async () => {
  loading.value = true
  try {
    sprints.value = await getTeamSprints(props.teamId)
  } catch (error) {
    console.error('加载 Sprint 列表失败:', error)
    ElMessage.error('加载 Sprint 列表失败')
  } finally {
    loading.value = false
  }
}

const saveSprint = async () => {
  if (!sprintForm.value.name || !sprintForm.value.startDate || !sprintForm.value.endDate) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  if (!currentUserId.value) {
    ElMessage.error('用户未登录')
    return
  }
  
  saving.value = true
  try {
    const data: Sprint = {
      name: sprintForm.value.name,
      goal: sprintForm.value.goal,
      startDate: sprintForm.value.startDate,
      endDate: sprintForm.value.endDate,
      teamId: props.teamId,
      createdBy: currentUserId.value,
      status: 'PLANNING'
    }
    
    if (editingSprintId.value) {
      await updateSprint(editingSprintId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createSprint(data)
      ElMessage.success('创建成功')
    }
    
    showCreateDialog.value = false
    resetForm()
    loadSprints()
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleAction = async (command: string, sprint: SprintVO) => {
  switch (command) {
    case 'edit':
      editSprint(sprint)
      break
    case 'start':
      await handleStartSprint(sprint.id!)
      break
    case 'complete':
      await handleCompleteSprint(sprint.id!)
      break
    case 'delete':
      await handleDeleteSprint(sprint.id!)
      break
  }
}

const editSprint = (sprint: SprintVO) => {
  editingSprintId.value = sprint.id!
  sprintForm.value = {
    name: sprint.name,
    goal: sprint.goal,
    startDate: sprint.startDate,
    endDate: sprint.endDate
  }
  showCreateDialog.value = true
}

const handleDeleteSprint = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个 Sprint 吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteSprint(id)
    ElMessage.success('删除成功')
    loadSprints()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleStartSprint = async (id: number) => {
  try {
    await startSprint(id)
    ElMessage.success('Sprint 已开始')
    loadSprints()
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

const handleCompleteSprint = async (id: number) => {
  try {
    await completeSprint(id)
    ElMessage.success('Sprint 已完成')
    loadSprints()
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

const resetForm = () => {
  editingSprintId.value = null
  sprintForm.value = {
    name: '',
    goal: '',
    startDate: '',
    endDate: ''
  }
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    'PLANNING': 'info',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'success'
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'PLANNING': '规划中',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成'
  }
  return texts[status] || status
}

const getProgress = (sprint: SprintVO) => {
  if (sprint.totalTasks === 0) return 0
  return Math.round((sprint.completedTasks / sprint.totalTasks) * 100)
}

const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const viewSprintTasks = (sprint: SprintVO) => {
  currentSprint.value = sprint
  showTasksDialog.value = true
}

onMounted(() => {
  loadSprints()
})
</script>

<style scoped lang="scss">
.sprint-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
  }
}

.primary-btn {
  padding: 10px 20px;
  border-radius: 20px;
  background: var(--accent-color);
  color: white;
  border: none;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  
  &:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
  }
}

.sprint-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.sprint-card {
  background: var(--bg-elevated-soft);
  border-radius: 16px;
  padding: 20px;
  border-left: 4px solid var(--border-card);
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
  
  &.status-planning {
    border-left-color: #909399;
  }
  
  &.status-in_progress {
    border-left-color: var(--el-color-warning);
  }
  
  &.status-completed {
    border-left-color: var(--el-color-success);
  }
}

.sprint-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  
  .sprint-title {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;
    
    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color);
    }
  }
}

.icon-btn {
  background: transparent;
  border: none;
  color: var(--text-color-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: all 0.2s;
  
  &:hover {
    background: var(--bg-card-hover);
    color: var(--text-color);
  }
}

.sprint-goal {
  color: var(--text-color-secondary);
  margin: 0 0 16px 0;
  font-size: 14px;
  line-height: 1.6;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sprint-dates {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-color-secondary);
  font-size: 13px;
  margin-bottom: 16px;
}

.sprint-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
  
  .stat-item {
    text-align: center;
    padding: 8px;
    background: var(--bg-body);
    border-radius: 8px;
    
    .label {
      display: block;
      color: var(--text-color-secondary);
      font-size: 12px;
      margin-bottom: 4px;
    }
    
    .value {
      display: block;
      font-size: 20px;
      font-weight: 700;
      color: var(--text-color);
      
      &.success {
        color: var(--el-color-success);
      }
      
      &.warning {
        color: var(--el-color-warning);
      }
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 20px;
}

.sprint-actions {
  margin-top: 12px;
}

.sprint-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  
  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }
}

.sprint-goal-text {
  color: var(--text-color-secondary);
  margin: 0;
  line-height: 1.6;
}

.tasks-hint {
  margin-top: 20px;
}
</style>
