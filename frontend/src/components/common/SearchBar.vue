<template>
  <div class="search-bar">
    <el-autocomplete
      v-model="searchText"
      :fetch-suggestions="querySearchAsync"
      placeholder="搜索项目、用户或标签..."
      :trigger-on-focus="false"
      @select="handleSelect"
      clearable
      class="search-input"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
      <template #default="{ item }">
        <div class="suggestion-item">
          <el-icon>
            <component :is="getIcon(item.type)" />
          </el-icon>
          <span class="name">{{ item.name }}</span>
          <el-tag size="small" type="info">{{ item.type }}</el-tag>
        </div>
      </template>
    </el-autocomplete>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Document, User, CollectionTag } from '@element-plus/icons-vue'

interface SearchSuggestion {
  name: string
  type: string
  id: number
}

const router = useRouter()
const searchText = ref('')

const getIcon = (type: string) => {
  const icons: Record<string, any> = {
    项目: Document,
    用户: User,
    标签: CollectionTag,
  }
  return icons[type] || Document
}

const querySearchAsync = (queryString: string, callback: (results: SearchSuggestion[]) => void) => {
  if (!queryString) {
    callback([])
    return
  }

  // TODO: 调用API进行搜索
  // 模拟数据
  const results: SearchSuggestion[] = [
    { name: '校园外卖系统', type: '项目', id: 1 },
    { name: '张三', type: '用户', id: 2 },
    { name: 'Vue.js', type: '标签', id: 3 },
  ]

  setTimeout(() => {
    callback(results.filter((item) => item.name.includes(queryString)))
  }, 200)
}

const handleSelect = (item: SearchSuggestion) => {
  // 根据类型跳转到相应页面
  if (item.type === '项目') {
    router.push({ name: 'ProjectDetail', params: { id: item.id } })
  } else if (item.type === '用户') {
    router.push({ name: 'ProfileView', params: { id: item.id } })
  }
  
  searchText.value = ''
}
</script>

<style scoped lang="scss">
.search-bar {
  .search-input {
    width: 100%;
  }

  .suggestion-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 0;

    .name {
      flex: 1;
      font-size: 14px;
    }
  }
}
</style>

