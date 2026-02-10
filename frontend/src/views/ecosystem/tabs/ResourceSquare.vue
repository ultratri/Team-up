<template>
  <div class="resource-square">
    <!-- 筛选控制栏 -->
    <section class="rs-controls">
      <div class="rs-controls__left">
        <div class="rs-filter">
          <div class="rs-filter__label">资源类型</div>
          <el-segmented
            v-model="filters.type"
            :options="typeOptions"
            @change="handleSearch"
          />
        </div>
      </div>

      <div class="rs-controls__right">
        <div class="rs-sort">
          <span class="rs-sort__label">排序</span>
          <el-select v-model="filters.sortBy" style="width: 140px" @change="handleSearch">
            <el-option label="最新发布" value="latest" />
            <el-option label="最多浏览" value="views" />
            <el-option label="最多点赞" value="likes" />
          </el-select>
        </div>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <!-- 资源列表 -->
    <div v-if="loading" class="rs-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="resources.length === 0" class="rs-empty">
      <el-empty description="暂无相关资源">
        <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
      </el-empty>
    </div>

    <TransitionGroup v-else name="rs-grid" tag="div" class="rs-grid">
      <div
        v-for="resource in resources"
        :key="resource.id"
        class="rs-card"
        @click="handleViewResource(resource.id)"
      >
        <div class="rs-card__cover" v-if="resource.cover">
          <img :src="resource.cover" :alt="resource.title" />
        </div>
        
        <div class="rs-card__content">
          <div class="rs-card__header">
            <h3 class="rs-card__title">{{ resource.title }}</h3>
            <el-tag :type="getTypeTagType(resource.type)" size="small">
              {{ getTypeText(resource.type) }}
            </el-tag>
          </div>

          <p class="rs-card__desc">{{ truncateText(resource.description, 80) }}</p>

          <div class="rs-card__tags">
            <span v-for="tag in resource.tags?.slice(0, 4)" :key="tag" class="tag">{{ tag }}</span>
          </div>

          <div class="rs-card__meta">
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ resource.views || 0 }}
            </span>
            <span 
              class="meta-item clickable" 
              :class="{ liked: resource.liked }"
              @click.stop="handleLike(resource)"
            >
              <el-icon><Star :filled="resource.liked" /></el-icon>
              {{ resource.likes || 0 }}
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(resource.createdAt) }}
            </span>
          </div>

          <div class="rs-card__footer">
            <el-avatar :size="32" :src="resource.author?.avatar">
              {{ resource.author?.realName?.charAt(0) }}
            </el-avatar>
            <span class="author-name">{{ resource.author?.realName }}</span>
          </div>
        </div>
      </div>
    </TransitionGroup>

    <!-- 分页 -->
    <div v-if="total > 0" class="rs-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="prev, pager, next"
        background
        @current-change="loadResources"
        @size-change="loadResources"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, Star, Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getResources, likeResource, unlikeResource, type Resource } from '@/api/ecosystem'

const router = useRouter()

const loading = ref(false)
const resources = ref<Resource[]>([])
const total = ref(0)

const pagination = reactive({
  page: 1,
  size: 12
})

const filters = reactive({
  type: '',
  sortBy: 'latest'
})

const typeOptions = [
  { label: '全部', value: '' },
  { label: '项目成果', value: 'PROJECT' },
  { label: '技术文档', value: 'DOCUMENT' },
  { label: '面经分享', value: 'INTERVIEW' },
  { label: '学习资料', value: 'MATERIAL' }
]

const loadResources = async () => {
  loading.value = true
  try {
    const res = await getResources({
      page: pagination.page,
      size: pagination.size,
      type: filters.type || undefined,
      sortBy: filters.sortBy
    })
    resources.value = res.records
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载资源列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadResources()
}

const resetFilters = () => {
  filters.type = ''
  filters.sortBy = 'latest'
  handleSearch()
}

const handleViewResource = (id: number) => {
  ElMessage.info(`查看资源详情功能开发中`)
}

const handleLike = async (resource: any) => {
  try {
    if (resource.liked) {
      await unlikeResource(resource.id)
      resource.liked = false
      resource.likes = (resource.likes || 0) - 1
      ElMessage.success('已取消点赞')
    } else {
      await likeResource(resource.id)
      resource.liked = true
      resource.likes = (resource.likes || 0) + 1
      ElMessage.success('点赞成功')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    PROJECT: '项目成果',
    DOCUMENT: '技术文档',
    INTERVIEW: '面经分享',
    MATERIAL: '学习资料'
  }
  return map[type] || type
}

const getTypeTagType = (type: string) => {
  const map: Record<string, any> = {
    PROJECT: 'success',
    DOCUMENT: 'primary',
    INTERVIEW: 'warning',
    MATERIAL: 'info'
  }
  return map[type] || 'info'
}

const truncateText = (text: string, maxLength: number) => {
  if (!text) return ''
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

const formatDate = (date: string | Date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

onMounted(() => {
  loadResources()
})
</script>

<style scoped lang="scss">
.resource-square {
  min-height: 60vh;
}

.rs-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px;
  background: var(--bg-elevated);
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

.rs-controls__left,
.rs-controls__right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.rs-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .rs-filter__label {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-color);
  }
}

.rs-sort {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .rs-sort__label {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color-muted);
  }
}

.rs-loading {
  margin-top: 20px;
}

.rs-empty {
  margin-top: 40px;
}

.rs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.rs-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-card-hover);
    border-color: var(--accent-color);
  }
}

.rs-card__cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: var(--bg-body);
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.rs-card__content {
  padding: 20px;
}

.rs-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.rs-card__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  flex: 1;
}

.rs-card__desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-color-muted);
  margin: 0 0 16px 0;
  min-height: 44px;
}

.rs-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  
  .tag {
    padding: 4px 12px;
    border-radius: 8px;
    background: var(--accent-soft);
    color: var(--accent-color);
    font-size: 12px;
    font-weight: 600;
  }
}

.rs-card__meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: var(--text-color-muted);
    
    &.clickable {
      cursor: pointer;
      transition: color 0.2s;
      
      &:hover {
        color: var(--accent-color);
      }
      
      &.liked {
        color: #f56c6c;
        
        &:hover {
          color: #f78989;
        }
      }
    }
  }
}

.rs-card__footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
  
  .author-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color);
  }
}

.rs-pagination {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .rs-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .rs-controls__left,
  .rs-controls__right {
    flex-direction: column;
    align-items: stretch;
  }
  
  .rs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
