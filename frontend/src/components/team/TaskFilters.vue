<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import type { TaskAssignee } from '@/types/team'

interface FilterCriteria {
  status?: string
  priority?: string
  assigneeId?: number
  keyword?: string
}

const props = defineProps<{
  teamMembers?: TaskAssignee[]
}>()

const emit = defineEmits<{
  (e: 'filter-change', filters: FilterCriteria): void
}>()

const filters = ref<FilterCriteria>({
  status: undefined,
  priority: undefined,
  assigneeId: undefined,
  keyword: ''
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待办', value: 'TODO' },
  { label: '进行中', value: 'DOING' },
  { label: '审核中', value: 'REVIEW' },
  { label: '已完成', value: 'DONE' }
]

const priorityOptions = [
  { label: '全部优先级', value: '' },
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' }
]

// Watch for filter changes and emit
watch(filters, (newFilters) => {
  const cleanFilters: FilterCriteria = {}
  
  if (newFilters.status) {
    cleanFilters.status = newFilters.status
  }
  if (newFilters.priority) {
    cleanFilters.priority = newFilters.priority
  }
  if (newFilters.assigneeId) {
    cleanFilters.assigneeId = newFilters.assigneeId
  }
  if (newFilters.keyword && newFilters.keyword.trim()) {
    cleanFilters.keyword = newFilters.keyword.trim()
  }
  
  emit('filter-change', cleanFilters)
}, { deep: true })

const handleReset = () => {
  filters.value = {
    status: undefined,
    priority: undefined,
    assigneeId: undefined,
    keyword: ''
  }
}
</script>

<template>
  <div class="task-filters">
    <div class="filter-group">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索任务标题..."
        :prefix-icon="Search"
        clearable
        class="search-input"
      />
      
      <el-select
        v-model="filters.status"
        placeholder="状态"
        clearable
        class="filter-select"
      >
        <el-option
          v-for="option in statusOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      
      <el-select
        v-model="filters.priority"
        placeholder="优先级"
        clearable
        class="filter-select"
      >
        <el-option
          v-for="option in priorityOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      
      <el-select
        v-model="filters.assigneeId"
        placeholder="负责人"
        clearable
        filterable
        class="filter-select"
      >
        <el-option
          v-for="member in teamMembers"
          :key="member.userId"
          :label="member.userName"
          :value="member.userId"
        >
          <div class="member-option">
            <el-avatar :size="24" :src="member.avatar" />
            <span>{{ member.userName }}</span>
          </div>
        </el-option>
      </el-select>
      
      <el-button
        :icon="RefreshLeft"
        @click="handleReset"
        class="reset-btn"
      >
        重置
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.task-filters {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--bg-elevated-soft);
  border: 1px solid var(--border-card);
  border-radius: 12px;
}

.filter-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.search-input {
  flex: 1;
  min-width: 200px;
  max-width: 300px;
}

.filter-select {
  width: 150px;
}

.reset-btn {
  margin-left: auto;
}

.member-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

</style>
