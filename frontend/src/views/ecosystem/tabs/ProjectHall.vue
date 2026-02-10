<template>
  <div class="project-hall">
    <!-- 筛选控制栏 -->
    <section class="ph-controls">
      <div class="ph-controls__left">
        <div class="ph-filter">
          <div class="ph-filter__label">项目类型</div>
          <div class="ph-filter__chips">
            <el-segmented
              v-model="filters.type"
              :options="typeOptions"
              @change="handleSearch"
            />
          </div>
        </div>
        
        <div class="ph-filter">
          <div class="ph-filter__label">项目状态</div>
          <div class="ph-filter__chips">
            <el-segmented
              v-model="filters.status"
              :options="statusOptions"
              @change="handleSearch"
            />
          </div>
        </div>
      </div>

      <div class="ph-controls__right">
        <div class="ph-sort">
          <span class="ph-sort__label">排序</span>
          <el-select v-model="filters.sortBy" style="width: 140px" @change="handleSearch">
            <el-option label="最新发布" value="latest" />
            <el-option label="最热门" value="hot" />
            <el-option label="匹配度" value="match" />
          </el-select>
        </div>

        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <!-- 项目列表 -->
    <div v-if="loading" class="ph-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="projects.length === 0" class="ph-empty">
      <el-empty description="暂无相关项目">
        <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
      </el-empty>
    </div>

    <TransitionGroup v-else name="ph-grid" tag="div" class="ph-grid">
      <div
        v-for="project in projects"
        :key="project.id"
        class="ph-card"
        @click="handleViewProject(project.id)"
      >
        <div class="ph-card__header">
          <h3 class="ph-card__title">{{ project.title }}</h3>
          <div class="ph-card__badges">
            <el-tag v-if="project.isNewbieFriendly" type="success" size="small">新手友好</el-tag>
            <el-tag v-if="project.urgent" type="danger" size="small">急需成员</el-tag>
          </div>
        </div>

        <p class="ph-card__desc">{{ truncateText(project.description, 100) }}</p>

        <div class="ph-card__meta">
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ project.currentMembers }}/{{ project.maxMembers }}
          </span>
          <span class="meta-item">
            <el-icon><Calendar /></el-icon>
            {{ formatDate(project.createdAt) }}
          </span>
          <span v-if="project.matchScore" class="meta-item match-score">
            <el-icon><Star /></el-icon>
            匹配度 {{ project.matchScore }}%
          </span>
        </div>

        <div class="ph-card__tags">
          <span v-for="tag in project.tags?.slice(0, 4)" :key="tag" class="tag">{{ tag }}</span>
        </div>

        <div class="ph-card__footer">
          <el-avatar :size="32" :src="project.creator?.avatar">
            {{ project.creator?.realName?.charAt(0) }}
          </el-avatar>
          <span class="creator-name">{{ project.creator?.realName }}</span>
        </div>
      </div>
    </TransitionGroup>

    <!-- 分页 -->
    <div v-if="total > 0" class="ph-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="prev, pager, next"
        background
        @current-change="loadProjects"
        @size-change="loadProjects"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Calendar, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getProjects } from '@/api/project'
import type { Project } from '@/types/project'

const router = useRouter()

const loading = ref(false)
const projects = ref<Project[]>([])
const total = ref(0)

const pagination = reactive({
  page: 1,
  size: 12
})

const filters = reactive({
  type: '',
  status: 'RECRUITING',
  sortBy: 'latest'
})

const typeOptions = [
  { label: '全部', value: '' },
  { label: 'Web开发', value: 'WEB' },
  { label: '移动应用', value: 'MOBILE' },
  { label: '算法竞赛', value: 'ALGORITHM' },
  { label: '人工智能', value: 'AI' }
]

const statusOptions = [
  { label: '全部', value: '' },
  { label: '招募中', value: 'RECRUITING' },
  { label: '进行中', value: 'IN_PROGRESS' }
]

const loadProjects = async () => {
  loading.value = true
  try {
    const res = await getProjects({
      page: pagination.page,
      size: pagination.size,
      type: filters.type || undefined,
      status: filters.status || undefined,
      keyword: undefined
    })
    projects.value = res.list
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载项目列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadProjects()
}

const resetFilters = () => {
  filters.type = ''
  filters.status = 'RECRUITING'
  filters.sortBy = 'latest'
  handleSearch()
}

const handleViewProject = (id: number) => {
  router.push({ name: 'ProjectDetail', params: { id } })
}

const truncateText = (text: string, maxLength: number) => {
  if (!text) return ''
  const plainText = text.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim()
  return plainText.length > maxLength ? plainText.slice(0, maxLength) + '...' : plainText
}

const formatDate = (date: string | Date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped lang="scss">
.project-hall {
  min-height: 60vh;
}

.ph-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px;
  background: var(--bg-elevated);
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

.ph-controls__left,
.ph-controls__right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.ph-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .ph-filter__label {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-color);
  }
}

.ph-sort {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .ph-sort__label {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color-muted);
  }
}

.ph-loading {
  margin-top: 20px;
}

.ph-empty {
  margin-top: 40px;
}

.ph-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.ph-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-card-hover);
    border-color: var(--accent-color);
  }
}

.ph-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.ph-card__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  flex: 1;
}

.ph-card__badges {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.ph-card__desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-color-muted);
  margin: 0 0 16px 0;
  min-height: 44px;
}

.ph-card__meta {
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
    
    &.match-score {
      color: var(--accent-color);
      font-weight: 600;
    }
  }
}

.ph-card__tags {
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

.ph-card__footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
  
  .creator-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color);
  }
}

.ph-pagination {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

.ph-grid-enter-active,
.ph-grid-leave-active {
  transition: all 0.3s ease;
}

.ph-grid-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.ph-grid-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (max-width: 768px) {
  .ph-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .ph-controls__left,
  .ph-controls__right {
    flex-direction: column;
    align-items: stretch;
  }
  
  .ph-grid {
    grid-template-columns: 1fr;
  }
}
</style>
