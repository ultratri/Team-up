<template>
  <div class="moment-square">
    <!-- 筛选控制栏 -->
    <section class="ms-controls">
      <div class="ms-controls__left">
        <div class="ms-filter">
          <div class="ms-filter__label">动态类型</div>
          <el-segmented
            v-model="filters.type"
            :options="typeOptions"
            @change="handleSearch"
          />
        </div>
      </div>

      <div class="ms-controls__right">
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <!-- 动态列表 -->
    <div v-if="loading" class="ms-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="moments.length === 0" class="ms-empty">
      <el-empty description="暂无动态">
        <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
      </el-empty>
    </div>

    <TransitionGroup v-else name="ms-list" tag="div" class="ms-list">
      <div
        v-for="moment in moments"
        :key="moment.id"
        class="ms-card"
      >
        <div class="ms-card__header">
          <el-avatar :size="48" :src="moment.user?.avatar">
            {{ moment.user?.realName?.charAt(0) }}
          </el-avatar>
          <div class="ms-card__user">
            <div class="user-name">{{ moment.user?.realName }}</div>
            <div class="user-time">{{ formatTime(moment.createdAt) }}</div>
          </div>
          <el-tag :type="getTypeTagType(moment.type)" size="small">
            {{ getTypeText(moment.type) }}
          </el-tag>
        </div>

        <div class="ms-card__content">
          <p class="ms-card__text">{{ moment.content }}</p>
          
          <div v-if="moment.relatedProject" class="ms-card__related">
            <div class="related-label">相关项目</div>
            <div class="related-item" @click="handleViewProject(moment.relatedProject.id)">
              <el-icon><Briefcase /></el-icon>
              <span>{{ moment.relatedProject.title }}</span>
            </div>
          </div>
        </div>

        <div class="ms-card__footer">
          <div 
            class="action-item" 
            :class="{ liked: moment.liked }"
            @click="handleLike(moment)"
          >
            <el-icon><Star :filled="moment.liked" /></el-icon>
            <span>{{ moment.likes || 0 }}</span>
          </div>
          <div class="action-item" @click="handleComment(moment)">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ moment.comments || 0 }}</span>
          </div>
        </div>
      </div>
    </TransitionGroup>

    <!-- 分页 -->
    <div v-if="total > 0" class="ms-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 30]"
        layout="prev, pager, next"
        background
        @current-change="loadMoments"
        @size-change="loadMoments"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Briefcase, Star, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMoments, likeMoment, unlikeMoment, type Moment } from '@/api/ecosystem'

const router = useRouter()

const loading = ref(false)
const moments = ref<Moment[]>([])
const total = ref(0)

const pagination = reactive({
  page: 1,
  size: 10
})

const filters = reactive({
  type: ''
})

const typeOptions = [
  { label: '全部', value: '' },
  { label: '项目更新', value: 'PROJECT_UPDATE' },
  { label: '成就分享', value: 'ACHIEVEMENT' },
  { label: '资源分享', value: 'SHARE' },
  { label: '团队活动', value: 'TEAM_ACTIVITY' }
]

const loadMoments = async () => {
  loading.value = true
  try {
    const res = await getMoments({
      page: pagination.page,
      size: pagination.size,
      type: filters.type || undefined
    })
    moments.value = res.records
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载动态列表失败')
  } finally {
    loading.value = false
  }
}

const getContentByType = (type: string) => {
  const map: Record<string, string> = {
    CREATE_PROJECT: '创建了一个新项目，欢迎大家加入！',
    JOIN_PROJECT: '加入了一个项目，期待与大家一起合作！',
    COMPLETE_PROJECT: '成功完成了一个项目，收获满满！',
    GET_BADGE: '获得了新徽章，继续加油！'
  }
  return map[type] || '发布了一条动态'
}

const handleSearch = () => {
  pagination.page = 1
  loadMoments()
}

const resetFilters = () => {
  filters.type = ''
  handleSearch()
}

const handleViewProject = (id: number) => {
  router.push({ name: 'ProjectDetail', params: { id } })
}

const handleLike = async (moment: any) => {
  try {
    if (moment.liked) {
      await unlikeMoment(moment.id)
      moment.liked = false
      moment.likes = (moment.likes || 0) - 1
      ElMessage.success('已取消点赞')
    } else {
      await likeMoment(moment.id)
      moment.liked = true
      moment.likes = (moment.likes || 0) + 1
      ElMessage.success('点赞成功')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleComment = (moment: any) => {
  ElMessage.info('评论功能开发中，敬请期待')
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    PROJECT_UPDATE: '项目更新',
    ACHIEVEMENT: '成就分享',
    SHARE: '资源分享',
    TEAM_ACTIVITY: '团队活动'
  }
  return map[type] || type
}

const getTypeTagType = (type: string) => {
  const map: Record<string, any> = {
    PROJECT_UPDATE: 'primary',
    ACHIEVEMENT: 'success',
    SHARE: 'warning',
    TEAM_ACTIVITY: 'info'
  }
  return map[type] || 'info'
}

const formatTime = (date: string | Date) => {
  if (!date) return '-'
  const d = new Date(date)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

onMounted(() => {
  loadMoments()
})
</script>

<style scoped lang="scss">
.moment-square {
  min-height: 60vh;
}

.ms-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px;
  background: var(--bg-elevated);
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

.ms-controls__left,
.ms-controls__right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.ms-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .ms-filter__label {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-color);
  }
}

.ms-loading {
  margin-top: 20px;
}

.ms-empty {
  margin-top: 40px;
}

.ms-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ms-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: var(--shadow-card-hover);
    border-color: var(--accent-color);
  }
}

.ms-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.ms-card__user {
  flex: 1;
  
  .user-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-color);
  }
  
  .user-time {
    font-size: 12px;
    color: var(--text-color-muted);
    margin-top: 2px;
  }
}

.ms-card__content {
  margin-bottom: 16px;
}

.ms-card__text {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-color);
  margin: 0 0 12px 0;
}

.ms-card__related {
  padding: 12px;
  background: var(--bg-body);
  border-radius: 8px;
  
  .related-label {
    font-size: 12px;
    color: var(--text-color-muted);
    margin-bottom: 8px;
  }
  
  .related-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--accent-color);
    cursor: pointer;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.ms-card__footer {
  display: flex;
  gap: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
  
  .action-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: var(--text-color-muted);
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

.ms-pagination {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

.ms-list-enter-active,
.ms-list-leave-active {
  transition: all 0.3s ease;
}

.ms-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.ms-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (max-width: 768px) {
  .ms-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .ms-controls__left,
  .ms-controls__right {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
