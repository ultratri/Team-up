<template>
  <div class="competition-center">
    <!-- 筛选控制栏 -->
    <section class="cc-controls">
      <div class="cc-controls__left">
        <div class="cc-filter">
          <div class="cc-filter__label">比赛级别</div>
          <el-select v-model="filters.level" placeholder="全部" clearable @change="handleSearch" style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="校级" value="SCHOOL" />
            <el-option label="省级" value="PROVINCE" />
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="国际级" value="INTERNATIONAL" />
          </el-select>
        </div>
        
        <div class="cc-filter">
          <div class="cc-filter__label">比赛状态</div>
          <el-select v-model="filters.status" placeholder="全部" clearable @change="handleSearch" style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </div>
      </div>

      <div class="cc-controls__right">
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <!-- 比赛列表 -->
    <div v-if="loading" class="cc-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="competitions.length === 0" class="cc-empty">
      <el-empty description="暂无相关比赛">
        <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
      </el-empty>
    </div>

    <TransitionGroup v-else name="cc-grid" tag="div" class="cc-grid">
      <div
        v-for="competition in competitions"
        :key="competition.id"
        class="cc-card"
        @click="handleViewCompetition(competition.id)"
      >
        <div class="cc-card__header">
          <h3 class="cc-card__title">{{ competition.name }}</h3>
          <div class="cc-card__badges">
            <el-tag :type="getLevelTagType(competition.level)" size="small">
              {{ getLevelText(competition.level) }}
            </el-tag>
            <el-tag v-if="isClosingSoon(competition.signupEndAt)" type="danger" size="small">
              即将截止
            </el-tag>
          </div>
        </div>

        <div class="cc-card__meta">
          <span class="meta-item">
            <el-icon><OfficeBuilding /></el-icon>
            {{ competition.organizer }}
          </span>
          <span v-if="competition.type" class="meta-item">
            <el-icon><Trophy /></el-icon>
            {{ competition.type }}
          </span>
        </div>

        <p class="cc-card__desc">{{ truncateText(competition.description, 100) }}</p>

        <div class="cc-card__time">
          <div class="time-item">
            <span class="time-label">报名时间</span>
            <span class="time-value">{{ formatDate(competition.signupStartAt) }} - {{ formatDate(competition.signupEndAt) }}</span>
          </div>
        </div>

        <div class="cc-card__footer">
          <div class="team-count">
            <el-icon><User /></el-icon>
            {{ competition.teamCount || 0 }} 支队伍
          </div>
          <el-button type="primary" size="small" @click.stop="handleViewCompetition(competition.id)">
            查看详情
          </el-button>
        </div>
      </div>
    </TransitionGroup>

    <!-- 分页 -->
    <div v-if="total > 0" class="cc-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="prev, pager, next"
        background
        @current-change="loadCompetitions"
        @size-change="loadCompetitions"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Trophy, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCompetitions } from '@/api/competition'
import type { Competition } from '@/types/competition'

const router = useRouter()

const loading = ref(false)
const competitions = ref<Competition[]>([])
const total = ref(0)

const pagination = reactive({
  page: 1,
  size: 12
})

const filters = reactive({
  level: '',
  status: 'PUBLISHED'
})

const loadCompetitions = async () => {
  loading.value = true
  try {
    const res = await getCompetitions({
      page: pagination.page,
      size: pagination.size,
      level: filters.level || undefined,
      status: filters.status || undefined
    })
    competitions.value = res.records
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载比赛列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadCompetitions()
}

const resetFilters = () => {
  filters.level = ''
  filters.status = 'PUBLISHED'
  handleSearch()
}

const handleViewCompetition = (id: number) => {
  router.push({ name: 'CompetitionDetail', params: { id } })
}

const getLevelText = (level: string) => {
  const map: Record<string, string> = {
    SCHOOL: '校级',
    PROVINCE: '省级',
    NATIONAL: '国家级',
    INTERNATIONAL: '国际级'
  }
  return map[level] || level
}

const getLevelTagType = (level: string) => {
  const map: Record<string, any> = {
    SCHOOL: 'info',
    PROVINCE: 'success',
    NATIONAL: 'warning',
    INTERNATIONAL: 'danger'
  }
  return map[level] || 'info'
}

const isClosingSoon = (signupEndAt: any) => {
  if (!signupEndAt) return false
  const end = new Date(signupEndAt).getTime()
  if (Number.isNaN(end)) return false
  const diff = end - Date.now()
  return diff > 0 && diff <= 3 * 24 * 60 * 60 * 1000
}

const truncateText = (text: string, maxLength: number) => {
  if (!text) return '暂无描述'
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}

const formatDate = (date: string | Date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

onMounted(() => {
  loadCompetitions()
})
</script>

<style scoped lang="scss">
.competition-center {
  min-height: 60vh;
}

.cc-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px;
  background: var(--bg-elevated);
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

.cc-controls__left,
.cc-controls__right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.cc-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .cc-filter__label {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-color);
  }
}

.cc-loading {
  margin-top: 20px;
}

.cc-empty {
  margin-top: 40px;
}

.cc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.cc-card {
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

.cc-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.cc-card__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  flex: 1;
}

.cc-card__badges {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.cc-card__meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: var(--text-color-muted);
  }
}

.cc-card__desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-color-muted);
  margin: 0 0 16px 0;
  min-height: 44px;
}

.cc-card__time {
  padding: 12px;
  background: var(--bg-body);
  border-radius: 8px;
  margin-bottom: 16px;
  
  .time-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .time-label {
      font-size: 12px;
      color: var(--text-color-muted);
    }
    
    .time-value {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-color);
    }
  }
}

.cc-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
  
  .team-count {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: var(--text-color-muted);
  }
}

.cc-pagination {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .cc-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .cc-controls__left,
  .cc-controls__right {
    flex-direction: column;
    align-items: stretch;
  }
  
  .cc-grid {
    grid-template-columns: 1fr;
  }
}
</style>
