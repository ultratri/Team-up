<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TaskCard from '@/components/team/TaskCard.vue'
import TaskDetail from '@/components/team/TaskDetail.vue'
import TaskFilters from '@/components/team/TaskFilters.vue'
import TaskStats from '@/components/team/TaskStats.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import { getTeamTasks, createTask, updateTask, deleteTask, filterTeamTasks, getTeamMembers } from '@/api/team'
import { getTeamSprints, type SprintVO } from '@/api/sprint'
import type { Task, TaskAssignee } from '@/types/team'
import { useAuthStore } from '@/store/auth'
import { useTeamStore } from '@/store/team'
import { debounce } from '@/utils/performance'

const props = defineProps<{
  teamId: number
}>()

const authStore = useAuthStore()
const teamStore = useTeamStore()
const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const selectedTaskId = ref<number>()
const loading = ref(false)
const dragLoading = ref(false)
const teamMembers = ref<TaskAssignee[]>([])
const sprints = ref<SprintVO[]>([])
const selectedSprintFilter = ref<number | null>(null)
const activeFilters = ref<any>({})
const taskStatsRef = ref<InstanceType<typeof TaskStats>>()
const showCompleted = ref(false)
const maxCompletedVisible = ref(20)

const completedHiddenCount = computed(() => {
  const total = columns.DONE?.length || 0
  if (showCompleted.value) return 0
  return total
})

const visibleTasksByStatus = computed(() => {
  return {
    TODO: columns.TODO,
    DOING: columns.DOING,
    REVIEW: columns.REVIEW,
    DONE: showCompleted.value ? (columns.DONE || []).slice(0, maxCompletedVisible.value) : [],
  } as Record<string, Task[]>
})

// Grouped tasks
const columns = reactive<Record<string, Task[]>>({
  TODO: [],
  DOING: [],
  REVIEW: [],
  DONE: []
})

const taskForm = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH',
  deadline: '',
  sprintId: null as number | null,
})

const columnConfig: Record<string, { title: string, color: string, icon: string }> = {
  TODO: { title: '待办', color: 'var(--text-color-secondary)', icon: 'i-ep-list' },
  DOING: { title: '进行中', color: 'var(--el-color-primary)', icon: 'i-ep-loading' },
  REVIEW: { title: '审核中', color: 'var(--el-color-warning)', icon: 'i-ep-view' },
  DONE: { title: '已完成', color: 'var(--el-color-success)', icon: 'i-ep-check' }
}

const loadTasks = async () => {
  if (!props.teamId) return
  loading.value = true
  try {
    let tasks: Task[] = []
    
    // Check if filters are active
    const hasActiveFilters = Object.keys(activeFilters.value).length > 0
    
    if (hasActiveFilters) {
      // Use filter API
      tasks = await filterTeamTasks(props.teamId, activeFilters.value)
    } else {
      // Use regular API
      const res = await getTeamTasks(props.teamId)
      tasks = res || []
    }
    
    // 应用 Sprint 筛选
    if (selectedSprintFilter.value !== null) {
      if (selectedSprintFilter.value === 0) {
        // 筛选未分配 Sprint 的任务
        tasks = tasks.filter(t => !t.sprintId)
      } else {
        // 筛选指定 Sprint 的任务
        tasks = tasks.filter(t => t.sprintId === selectedSprintFilter.value)
      }
    }
    
    // Reset columns
    columns.TODO = []
    columns.DOING = []
    columns.REVIEW = []
    columns.DONE = []
    
    // Distribute tasks
    tasks.forEach((task: Task) => {
      if (columns[task.status]) {
        columns[task.status].push(task)
      } else {
        columns.TODO.push(task)
      }
    })
  } catch (error: any) {
    console.error('Failed to load tasks:', error)
    ElMessage.error(error.message || '加载任务失败')
  } finally {
    loading.value = false
  }
}

const loadTeamMembers = async () => {
  if (!props.teamId) return
  try {
    const members = await getTeamMembers(props.teamId)
    // Convert team members to TaskAssignee format
    teamMembers.value = members.map(member => ({
      id: member.id,
      taskId: 0, // Not applicable for filter dropdown
      userId: member.userId,
      userName: member.username,
      avatar: member.avatar,
      assignedAt: ''
    }))
  } catch (error: any) {
    console.error('Failed to load team members:', error)
  }
}

const loadSprints = async () => {
  if (!props.teamId) return
  try {
    const allSprints = await getTeamSprints(props.teamId)
    // 只显示规划中和进行中的 Sprint
    sprints.value = allSprints.filter(s => s.status === 'PLANNING' || s.status === 'IN_PROGRESS')
  } catch (error: any) {
    console.error('Failed to load sprints:', error)
  }
}

const handleFilterChange = (filters: any) => {
  activeFilters.value = filters
  // Performance optimization: Debounce filter changes to reduce API calls
  debouncedLoadTasks()
}

// Debounced version of loadTasks for filter changes
const debouncedLoadTasks = debounce(loadTasks, 300)

watch(() => props.teamId, (newId) => {
  if (newId) {
    loadTeamMembers()
    loadSprints()
    loadTasks()
  }
})

// vue-draggable-plus 基于 SortableJS：跨列拖拽会触发 onAdd，数据在 event.data
const handleDragAdd = async (event: any, status: string) => {
  const task = event?.data as Task | undefined
  if (!task) return

    const oldStatus = task.status

  // 已完成默认锁定：普通成员不允许把 DONE 拖回去（避免反复回流导致混乱）
  if (oldStatus === 'DONE' && status !== 'DONE' && !teamStore.isCurrentUserAdmin) {
    ElMessage.warning('已完成任务默认不可拖回；如需重开请联系队长/管理员')
    loadTasks().catch(() => {})
    return
  }

  // 审核规则 A：只有队长/管理员可以将任务从 REVIEW 置为 DONE
  if (oldStatus === 'REVIEW' && status === 'DONE' && !teamStore.isCurrentUserAdmin) {
    ElMessage.warning('只有队长/管理员可以将任务标记为已完成，请先提交到“审核中”等待审核')
    // 回滚：重新从服务端拉取，避免拖拽状态与列数组不一致
    loadTasks().catch(() => {})
    return
  }

  // 本地先更新状态（乐观更新）
  task.status = status as Task['status']
    
    dragLoading.value = true
    try {
    // 只提交必要字段，避免把旧数据/空字段覆盖到后端
    await updateTask({ id: task.id, status: status as Task['status'] }, authStore.user?.id)

    // 关键：以服务端为准重新拉取，保证刷新后不回滚、顶部统计也同步
    await loadTasks()
    refreshStats()
      ElMessage.success('任务状态已更新')
    } catch (error: any) {
      console.error('Failed to update task status:', error)
      ElMessage.error(error.message || '更新任务状态失败')
    // 回滚：重新从服务端拉取
    await loadTasks().catch(() => {})
    } finally {
      dragLoading.value = false
  }
}

const handleCreateTask = async () => {
  if (!taskForm.title) {
    ElMessage.warning('请输入任务标题')
    return
  }

  try {
    await createTask({
      ...taskForm,
      teamId: props.teamId,
      status: 'TODO',
      createdBy: authStore.user?.id
    })
    ElMessage.success('创建成功')
    showAddDialog.value = false
    loadTasks()
    // 刷新统计数据
    refreshStats()
    
    Object.assign(taskForm, {
      title: '',
      description: '',
      priority: 'MEDIUM',
      deadline: '',
      sprintId: null,
    })
  } catch (error: any) {
    console.error('Failed to create task:', error)
    ElMessage.error(error.message || '创建任务失败')
  }
}

const handleTaskClick = (task: Task) => {
  selectedTaskId.value = task.id
  showDetailDialog.value = true
}

const handleUpdateTask = async (task: Task) => {
  try {
    await updateTask(task, authStore.user?.id)
    loadTasks()
  } catch (error: any) {
    console.error('Failed to update task:', error)
    ElMessage.error(error.message || '更新任务失败')
  }
}

const handleDeleteTask = async (taskId: number) => {
  try {
    await deleteTask(taskId, authStore.user?.id)
    ElMessage.success('删除成功')
    loadTasks()
    // 刷新统计数据
    refreshStats()
  } catch (error: any) {
    console.error('Failed to delete task:', error)
    ElMessage.error(error.message || '删除任务失败')
  }
}

const handleDetailSaved = () => {
  // 任务详情保存后，重新加载任务列表
  loadTasks()
  // 刷新统计数据
  refreshStats()
}

const refreshStats = () => {
  if (taskStatsRef.value) {
    taskStatsRef.value.refresh()
  }
}

onMounted(() => {
  loadTeamMembers()
  loadSprints()
  loadTasks()
})
</script>

<template>
  <div class="task-board">
    <div class="board-header">
      <h2>任务看板</h2>
      <div class="header-actions">
        <el-select
          v-model="selectedSprintFilter"
          placeholder="筛选 Sprint"
          clearable
          style="width: 200px; margin-right: 12px"
          @change="loadTasks"
        >
          <el-option label="全部任务" :value="null" />
          <el-option label="未分配 Sprint" :value="0" />
          <el-option
            v-for="sprint in sprints"
            :key="sprint.id"
            :label="`${sprint.name} (${sprint.totalTasks || 0})`"
            :value="sprint.id"
          />
        </el-select>
        <button class="primary-btn" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          <span>新建任务</span>
        </button>
      </div>
    </div>

    <!-- Task Filters -->
    <TaskFilters
      :team-members="teamMembers"
      @filter-change="handleFilterChange"
    />

    <!-- Task Statistics -->
    <TaskStats
      ref="taskStatsRef"
      :team-id="teamId"
      :auto-refresh="false"
    />

    <div class="kanban-container" v-loading="loading || dragLoading">
      <div 
        v-for="(tasks, status) in columns" 
        :key="status" 
        class="kanban-column"
      >
        <div class="column-header" :style="{ borderColor: columnConfig[status].color }">
          <span class="status-dot" :style="{ background: columnConfig[status].color }"></span>
          <span class="title">{{ columnConfig[status].title }}</span>
          <span class="count">{{ (visibleTasksByStatus[status] || []).length }}</span>
          <button
            v-if="status === 'DONE' && (columns.DONE?.length || 0) > 0"
            type="button"
            class="toggle-completed"
            @click="showCompleted = !showCompleted"
          >
            {{ showCompleted ? '收起' : `展开(${columns.DONE.length})` }}
          </button>
        </div>
        
        <VueDraggable
          v-model="columns[status]"
          :group="{ name: 'tasks', pull: true, put: true }"
          :animation="200"
          ghost-class="ghost"
          class="task-list"
          :onAdd="(e) => handleDragAdd(e, status)"
        >
          <!-- vue-draggable-plus 组件用法：直接渲染子节点（不支持 vuedraggable 的 #item slot） -->
          <div
            v-for="task in (visibleTasksByStatus[status] || [])"
            :key="task.id"
            :data-id="task.id"
          >
            <TaskCard
            v-memo="[task.id, task.title, task.status, task.priority, task.deadline, task.assignees?.length, task.commentCount, task.attachmentCount]"
            :task="task"
            @click="handleTaskClick(task)"
            @update="handleUpdateTask"
            @delete="handleDeleteTask"
          />
          </div>
        </VueDraggable>
      </div>
    </div>

    <!-- Add Dialog -->
    <el-dialog v-model="showAddDialog" title="新建任务" width="500px">
      <el-form :model="taskForm" label-position="top">
        <el-form-item label="任务标题">
          <el-input v-model="taskForm.title" placeholder="请输入任务标题" />
        </el-form-item>

        <el-form-item label="任务描述">
          <el-input
            v-model="taskForm.description"
            type="textarea"
            :rows="3"
            placeholder="请描述任务详情..."
          />
        </el-form-item>

        <el-form-item label="优先级">
          <el-radio-group v-model="taskForm.priority">
            <el-radio-button label="LOW">低</el-radio-button>
            <el-radio-button label="MEDIUM">中</el-radio-button>
            <el-radio-button label="HIGH">高</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="截止日期">
          <el-date-picker
            v-model="taskForm.deadline"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="所属 Sprint">
          <el-select
            v-model="taskForm.sprintId"
            placeholder="选择 Sprint（可选）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="sprint in sprints"
              :key="sprint.id"
              :label="`${sprint.name} (${sprint.status === 'IN_PROGRESS' ? '进行中' : '规划中'})`"
              :value="sprint.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">创建</el-button>
      </template>
    </el-dialog>

    <!-- Task Detail Dialog -->
    <TaskDetail
      v-model="showDetailDialog"
      :task-id="selectedTaskId"
      @saved="handleDetailSaved"
    />
  </div>
</template>

<style scoped lang="scss">
.task-board {
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

  .header-actions {
    display: flex;
    align-items: center;
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

.kanban-container {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  padding-bottom: 20px;
  flex: 1;
  align-items: flex-start;
}

.kanban-column {
  flex: 1;
  min-width: 280px;
  background: var(--bg-elevated-soft);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

.column-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid transparent;
  gap: 8px;
  
  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 8px;
  }
  
  .title {
    font-weight: 700;
    font-size: 15px;
    color: var(--text-color);
    margin-right: 8px;
  }
  
  .count {
    background: rgba(var(--accent-color-rgb), 0.10);
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 12px;
    color: var(--text-color-muted);
  }
}

.toggle-completed {
  margin-left: auto;
  background: transparent;
  border: none;
  color: var(--accent-color);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 8px;
  transition: background 0.2s;
}

.toggle-completed:hover {
  background: rgba(var(--accent-color-rgb), 0.10);
}

.task-list {
  flex: 1;
  overflow-y: auto;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
  
  &::-webkit-scrollbar {
    width: 4px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(0,0,0,0.1);
    border-radius: 2px;
  }
}

.ghost {
  opacity: 0.5;
  background: var(--accent-soft);
  border: 2px dashed var(--accent-color);
}

[data-theme='dark'] {
  .kanban-column {
    background: var(--bg-elevated-soft);
  }
  
  .column-header .count {
    background: rgba(var(--accent-color-rgb), 0.18);
  }
}
</style>
