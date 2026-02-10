<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { request } from '@/utils/request'

interface TaskStats {
  todoCount: number
  doingCount: number
  reviewCount: number
  doneCount: number
  totalCount: number
  completionRate: number
  overdueCount: number
}

const props = defineProps<{
  teamId: number
  autoRefresh?: boolean
  refreshInterval?: number // in milliseconds
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const loading = ref(false)
const stats = ref<TaskStats>({
  todoCount: 0,
  doingCount: 0,
  reviewCount: 0,
  doneCount: 0,
  totalCount: 0,
  completionRate: 0,
  overdueCount: 0
})

let refreshTimer: number | null = null

const statusCards = computed(() => [
  {
    label: '待办',
    count: stats.value.todoCount,
    color: 'var(--text-color-secondary)',
    icon: 'i-ep-list'
  },
  {
    label: '进行中',
    count: stats.value.doingCount,
    color: 'var(--el-color-primary)',
    icon: 'i-ep-loading'
  },
  {
    label: '审核中',
    count: stats.value.reviewCount,
    color: 'var(--el-color-warning)',
    icon: 'i-ep-view'
  },
  {
    label: '已完成',
    count: stats.value.doneCount,
    color: 'var(--el-color-success)',
    icon: 'i-ep-check'
  }
])

const completionRateColor = computed(() => {
  const rate = stats.value.completionRate
  if (rate >= 80) return 'var(--el-color-success)'
  if (rate >= 50) return 'var(--el-color-primary)'
  if (rate >= 30) return 'var(--el-color-warning)'
  return 'var(--el-color-danger)'
})

const loadStats = async () => {
  if (!props.teamId) return
  
  loading.value = true
  try {
    const response = await request.get<TaskStats>(`/tasks/team/${props.teamId}/stats`)
    stats.value = response
    emit('refresh')
  } catch (error: any) {
    console.error('Failed to load task statistics:', error)
    ElMessage.error(error.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const setupAutoRefresh = () => {
  if (props.autoRefresh && props.refreshInterval) {
    refreshTimer = window.setInterval(() => {
      loadStats()
    }, props.refreshInterval)
  }
}

const clearAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

watch(() => props.teamId, (newId) => {
  if (newId) {
    loadStats()
  }
})

watch(() => [props.autoRefresh, props.refreshInterval], () => {
  clearAutoRefresh()
  setupAutoRefresh()
})

onMounted(() => {
  loadStats()
  setupAutoRefresh()
})

onUnmounted(() => {
  clearAutoRefresh()
})

defineExpose({
  refresh: loadStats
})
</script>

<template>
  <div class="task-stats" v-loading="loading">
    <!-- Status Cards -->
    <div class="stats-cards">
      <div 
        v-for="card in statusCards" 
        :key="card.label"
        class="stat-card"
        :style="{ borderColor: card.color }"
      >
        <div class="card-icon" :style="{ background: card.color }">
          <span :class="card.icon"></span>
        </div>
        <div class="card-content">
          <div class="card-label">{{ card.label }}</div>
          <div class="card-count">{{ card.count }}</div>
        </div>
      </div>
    </div>

    <!-- Completion Rate -->
    <div class="completion-section">
      <div class="section-header">
        <span class="section-title">任务完成率</span>
        <span class="section-value">{{ stats.completionRate.toFixed(1) }}%</span>
      </div>
      <el-progress 
        :percentage="stats.completionRate" 
        :color="completionRateColor"
        :stroke-width="12"
      />
      <div class="completion-detail">
        已完成 {{ stats.doneCount }} / 总计 {{ stats.totalCount }} 个任务
      </div>
    </div>

    <!-- Overdue Alert -->
    <el-alert
      v-if="stats.overdueCount > 0"
      :title="`有 ${stats.overdueCount} 个任务已逾期`"
      type="warning"
      :closable="false"
      show-icon
      class="overdue-alert"
    >
      <template #default>
        请及时处理逾期任务，避免影响项目进度
      </template>
    </el-alert>
  </div>
</template>

<style scoped lang="scss">
.task-stats {
  margin-bottom: 24px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-elevated);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 4px solid;
  transition: all 0.3s;
  border: 1px solid var(--border-card);
  box-shadow: var(--shadow-card);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-card-hover);
  }
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.card-content {
  flex: 1;
}

.card-label {
  font-size: 13px;
  color: var(--text-color-muted);
  margin-bottom: 4px;
}

.card-count {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-color);
}

.completion-section {
  background: var(--bg-elevated);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid var(--border-card);
  box-shadow: var(--shadow-card);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color);
}

.section-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-color);
}

.completion-detail {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-color-muted);
  text-align: center;
}

.overdue-alert {
  border-radius: 12px;
  
  :deep(.el-alert__title) {
    font-weight: 600;
  }
}

[data-theme='dark'] {
  .stat-card,
  .completion-section {
    background: var(--bg-elevated);
  }
}
</style>
