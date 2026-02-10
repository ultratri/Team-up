<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Search, Filter, ArrowDown, ArrowUp } from '@element-plus/icons-vue'

const emit = defineEmits<{
  (e: 'search', params: any): void
  (e: 'create'): void
}>()

const isExpanded = ref(false)

const params = reactive({
  keyword: '',
  type: '',
  status: 'RECRUITING',
  roles: [] as string[],
  techStack: [] as string[]
})

const projectTypes = [
  { label: '全部', value: '' },
  { label: '竞赛', value: 'COMPETITION' },
  { label: '科研', value: 'RESEARCH' },
  { label: '创业', value: 'STARTUP' },
  { label: '开源', value: 'OPENSOURCE' },
  { label: '其他', value: 'OTHER' }
]

const statusOptions = [
  { label: '全部', value: '' },
  { label: '招募中', value: 'RECRUITING' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' }
]

const commonRoles = ['前端', '后端', '全栈', 'UI/UX', '产品经理', 'AI算法', '移动端']
const commonStack = ['Vue', 'React', 'Java', 'Python', 'Go', 'Node.js', 'Flutter']

const toggleRole = (role: string) => {
  const index = params.roles.indexOf(role)
  if (index > -1) params.roles.splice(index, 1)
  else params.roles.push(role)
  handleSearch()
}

const toggleStack = (tech: string) => {
  const index = params.techStack.indexOf(tech)
  if (index > -1) params.techStack.splice(index, 1)
  else params.techStack.push(tech)
  handleSearch()
}

const handleSearch = () => {
  emit('search', { ...params })
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}
</script>

<template>
  <div class="project-filter">
    <!-- Main Bar -->
    <div class="filter-bar glass-panel">
      <div class="search-input-wrapper">
        <el-icon class="search-icon"><Search /></el-icon>
        <input 
          v-model="params.keyword" 
          type="text" 
          placeholder="搜索项目名称、描述或关键词..." 
          @keyup.enter="handleSearch"
        />
      </div>
      
      <div class="filter-actions">
        <el-select 
          v-model="params.type" 
          placeholder="项目类型" 
          class="type-select"
          @change="handleSearch"
        >
          <el-option 
            v-for="item in projectTypes" 
            :key="item.value" 
            :label="item.label" 
            :value="item.value" 
          />
        </el-select>
        
        <button class="icon-btn" :class="{ active: isExpanded }" @click="toggleExpand" title="高级筛选">
          <el-icon><Filter /></el-icon>
        </button>
        
        <button class="primary-btn" @click="emit('create')">
          <span class="plus">+</span>
          <span>创建项目</span>
        </button>
      </div>
    </div>
    
    <!-- Expanded Filters -->
    <transition name="filter-expand">
      <div class="advanced-filters glass-panel" v-show="isExpanded">
        <div class="filter-group">
          <label>状态</label>
          <div class="tags-list">
            <span 
              v-for="item in statusOptions" 
              :key="item.value"
              class="filter-tag"
              :class="{ active: params.status === item.value }"
              @click="params.status = item.value; handleSearch()"
            >
              {{ item.label }}
            </span>
          </div>
        </div>
        
        <div class="filter-group">
          <label>招募角色</label>
          <div class="tags-list">
            <span 
              v-for="role in commonRoles" 
              :key="role"
              class="filter-tag"
              :class="{ active: params.roles.includes(role) }"
              @click="toggleRole(role)"
            >
              {{ role }}
            </span>
          </div>
        </div>
        
        <div class="filter-group">
          <label>技术栈</label>
          <div class="tags-list">
            <span 
              v-for="tech in commonStack" 
              :key="tech"
              class="filter-tag"
              :class="{ active: params.techStack.includes(tech) }"
              @click="toggleStack(tech)"
            >
              {{ tech }}
            </span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
.project-filter {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-expand-enter-active,
.filter-expand-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 500px;
  opacity: 1;
  overflow: hidden;
}

.filter-expand-enter-from,
.filter-expand-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: -12px;
  padding-top: 0;
  padding-bottom: 0;
}

.glass-panel {
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--border-subtle);
  border-radius: 20px;
  box-shadow: var(--shadow-card);
  padding: 12px 24px;
}

[data-theme='dark'] .glass-panel {
  background: var(--glass-bg-dark);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  gap: 20px;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  background: rgba(0,0,0,0.04);
  border-radius: 12px;
  padding: 0 16px;
  height: 48px;
  transition: all 0.3s;
  border: 1px solid transparent;
  
  &:focus-within {
    background: var(--bg-elevated);
    box-shadow: 0 0 0 2px var(--accent-soft);
    border-color: var(--accent-color);
  }
  
  .search-icon {
    color: var(--text-color-muted);
    margin-right: 12px;
    font-size: 18px;
  }
  
  input {
    border: none;
    background: transparent;
    height: 100%;
    width: 100%;
    outline: none;
    font-size: 15px;
    color: var(--text-color);
    
    &::placeholder {
      color: var(--text-color-muted);
      opacity: 0.7;
    }
  }
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.type-select {
  width: 140px;
  :deep(.el-input__wrapper) {
    background: transparent !important;
    box-shadow: none !important;
    border: 1px solid var(--border-subtle);
  }
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-color-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  
  &:hover {
    background: rgba(0,0,0,0.04);
    color: var(--text-color);
  }
  
  &.active {
    background: var(--accent-soft);
    color: var(--accent-color);
    border-color: var(--accent-color);
  }
}

.primary-btn {
  height: 48px;
  padding: 0 24px;
  border-radius: 24px;
  background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
  color: white;
  border: none;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: var(--shadow-button);
  transition: all 0.3s var(--ease-spring);
  
  &:hover {
    transform: translateY(-2px);
    filter: brightness(1.05);
    box-shadow: 0 15px 30px -5px rgba(var(--accent-color-rgb), 0.6);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  .plus {
    font-size: 20px;
    font-weight: 400;
  }
}

/* Advanced Filters */
.advanced-filters {
  padding: 24px;
  animation: slideDown 0.3s var(--ease-spring);
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.filter-group {
  margin-bottom: 16px;
  
  &:last-child {
    margin-bottom: 0;
  }
  
  label {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-color-muted);
    margin-bottom: 10px;
  }
  
  .tags-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .filter-tag {
    padding: 6px 16px;
    border-radius: 20px;
    background: rgba(0,0,0,0.04);
    color: var(--text-color-muted);
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;
    
    &:hover {
      background: rgba(0,0,0,0.08);
    }
    
    &.active {
      background: var(--accent-soft);
      color: var(--accent-color);
      border-color: rgba(var(--accent-color-rgb), 0.2);
      font-weight: 500;
    }
  }
}

[data-theme='dark'] {
  .search-input-wrapper {
    background: rgba(255,255,255,0.05);
  }
  
  .filter-tag {
    background: rgba(255,255,255,0.05);
    &:hover { background: rgba(255,255,255,0.1); }
  }
  
  .icon-btn:hover {
    background: rgba(255,255,255,0.05);
  }
}
</style>
