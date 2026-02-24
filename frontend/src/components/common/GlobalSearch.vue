<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { request } from '@/utils/request'

const router = useRouter()
const searchVisible = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const loading = ref(false)
const activeCategory = ref('all')

const categories = [
  { key: 'all', label: '全部' },
  { key: 'projects', label: '项目' },
  { key: 'users', label: '用户' },
  { key: 'teams', label: '团队' }
]

const filteredResults = computed(() => {
  if (activeCategory.value === 'all') {
    return searchResults.value
  }
  return searchResults.value.filter(r => r.type === activeCategory.value)
})

// 监听 Ctrl/Cmd + K 快捷键
const handleKeyDown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    open()
  }
  if (e.key === 'Escape' && searchVisible.value) {
    e.preventDefault()
    close()
  }
}

const open = () => {
  searchVisible.value = true
  // 等待 DOM 更新后聚焦输入框
  setTimeout(() => {
    const input = document.querySelector('.search-dialog .search-input') as HTMLInputElement
    if (input) {
      input.focus()
    }
  }, 100)
}

const close = () => {
  searchVisible.value = false
  searchQuery.value = ''
  searchResults.value = []
}

// 防抖搜索
let searchTimer: number | null = null
watch(searchQuery, (newVal) => {
  if (!newVal.trim()) {
    searchResults.value = []
    return
  }

  if (searchTimer) clearTimeout(searchTimer)
  
  searchTimer = window.setTimeout(async () => {
    await performSearch(newVal)
  }, 300)
})

const performSearch = async (query: string) => {
  loading.value = true
  try {
    const res = await request.get('/search/global', { params: { keyword: query } })
    // 响应拦截器已经解包了 data，直接使用返回的数据
    if (res) {
      // 合并所有类型的搜索结果
      const results = [
        ...(res.projects || []),
        ...(res.users || []),
        ...(res.teams || [])
      ]
      searchResults.value = results
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const handleResultClick = (result: any) => {
  close()
  
  switch (result.type) {
    case 'projects':
      router.push(`/project/${result.id}`)
      break
    case 'users':
      router.push(`/profile/${result.id}`)
      break
    case 'teams':
      router.push(`/team/${result.id}`)
      break
  }
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    projects: '项目',
    users: '用户',
    teams: '团队'
  }
  return map[type] || type
}

// 挂载快捷键
import { onMounted, onUnmounted } from 'vue'
onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="searchVisible"
    :show-close="false"
    width="600px"
    top="10vh"
    class="search-dialog"
    :modal="true"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    append-to-body
  >
    <div class="search-container">
      <!-- 搜索输入框 -->
      <div class="search-input-wrapper">
        <el-icon class="search-icon"><Search /></el-icon>
        <input
          v-model="searchQuery"
          class="search-input"
          placeholder="搜索项目、用户、团队... (Ctrl+K)"
          autofocus
        />
        <kbd class="keyboard-hint">ESC</kbd>
      </div>

      <!-- 分类标签 -->
      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat.key"
          class="category-tab"
          :class="{ active: activeCategory === cat.key }"
          @click="activeCategory = cat.key"
        >
          {{ cat.label }}
          <span v-if="cat.key === 'all'" class="count">
            {{ searchResults.length }}
          </span>
        </button>
      </div>

      <!-- 搜索结果 -->
      <div v-loading="loading" class="search-results">
        <div
          v-for="result in filteredResults"
          :key="`${result.type}-${result.id}`"
          class="result-item"
          @click="handleResultClick(result)"
        >
          <div class="result-icon">{{ result.icon }}</div>
          <div class="result-content">
            <div class="result-title">{{ result.title }}</div>
            <div class="result-description">{{ result.description }}</div>
          </div>
          <el-tag size="small">{{ getTypeLabel(result.type) }}</el-tag>
        </div>

        <div v-if="searchQuery && !loading && filteredResults.length === 0" class="empty-results">
          <el-empty description="未找到相关结果" />
        </div>

        <div v-if="!searchQuery" class="search-tips">
          <p>💡 搜索提示：</p>
          <ul>
            <li>输入关键词搜索项目、用户、团队</li>
            <li>使用 Ctrl+K 快速打开搜索</li>
            <li>按 ESC 关闭搜索框</li>
          </ul>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.search-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
    box-shadow: var(--shadow-xl);
    background: var(--bg-elevated);
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }
  
  :deep(.el-overlay) {
    background-color: rgba(0, 0, 0, 0.5);
  }
}

[data-theme='dark'] .search-dialog {
  :deep(.el-dialog) {
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
  }
}

.search-container {
  display: flex;
  flex-direction: column;
  max-height: 60vh;
  background: var(--bg-elevated);
  border-radius: 16px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-subtle);
  gap: 12px;
  background: var(--bg-elevated);

  .search-icon {
    font-size: 20px;
    color: var(--text-color-muted);
  }

  .search-input {
    flex: 1;
    border: none;
    outline: none;
    font-size: 16px;
    background: transparent;
    color: var(--text-color);

    &::placeholder {
      color: var(--text-color-muted);
    }
  }

  .keyboard-hint {
    padding: 4px 8px;
    border: 1px solid var(--border-subtle);
    border-radius: 4px;
    font-size: 12px;
    color: var(--text-color-muted);
    background: var(--el-fill-color-lighter);
  }
}

.category-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
}

.category-tab {
  padding: 6px 14px;
  border: none;
  border-radius: 20px;
  background: transparent;
  color: var(--text-color-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.active {
    background: var(--el-color-primary);
    color: white;
  }

  .count {
    font-size: 12px;
    opacity: 0.8;
  }
}

.search-results {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
  background: var(--bg-elevated);
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.result-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-description {
  font-size: 13px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-results {
  padding: 40px 24px;
}

.search-tips {
  padding: 24px;
  color: var(--text-color-secondary);

  p {
    margin: 0 0 12px 0;
    font-weight: 600;
  }

  ul {
    margin: 0;
    padding-left: 20px;
    list-style: none;

    li {
      margin-bottom: 8px;
      position: relative;
      padding-left: 16px;

      &::before {
        content: '•';
        position: absolute;
        left: 0;
        color: var(--el-color-primary);
      }
    }
  }
}
</style>
