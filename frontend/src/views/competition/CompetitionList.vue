<template>
  <div class="competition-list">
    <div class="competition-list__header">
      <div>
        <h1 class="competition-list__title">比赛广场</h1>
        <p class="competition-list__subtitle">发现各类比赛，组建你的参赛队伍</p>
      </div>
    </div>

    <el-card v-if="recommended.length > 0" class="recommend-card" shadow="never">
      <div class="recommend-header">
        <h3>为你推荐</h3>
        <span class="hint">基于你的技能与比赛类型/关键词的简单匹配</span>
      </div>
      <div class="recommend-list">
        <el-button
          v-for="c in recommended"
          :key="c.id"
          type="primary"
          plain
          size="small"
          @click="handleViewDetail(c.id)"
        >
          {{ c.name }}
        </el-button>
      </div>
    </el-card>

    <el-card v-if="hot.length > 0" class="hot-card" shadow="never">
      <div class="recommend-header">
        <h3>热门比赛</h3>
        <span class="hint">按浏览量排序</span>
      </div>
      <div class="recommend-list">
        <el-button
          v-for="c in hot"
          :key="c.id"
          type="danger"
          plain
          size="small"
          @click="handleViewDetail(c.id)"
        >
          {{ c.name }}
        </el-button>
      </div>
    </el-card>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="searchParams" class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchParams.keyword"
            placeholder="搜索比赛名称、主办方..."
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable @change="handleSearch" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="searchParams.level" placeholder="全部" clearable @change="handleSearch" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="校级" value="SCHOOL" />
            <el-option label="省级" value="PROVINCE" />
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="国际级" value="INTERNATIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="competition-list__loading">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 比赛列表 -->
    <div v-else-if="competitions.length > 0" class="competition-list__grid">
      <el-card
        v-for="competition in competitions"
        :key="competition.id"
        class="competition-card"
        shadow="hover"
        @click="handleViewDetail(competition.id)"
      >
        <div class="competition-card__header">
          <h3 class="competition-card__title">{{ competition.name }}</h3>
          <div class="header-tags">
            <el-tag v-if="isClosingSoon(competition.signupEndAt)" type="danger" effect="light">
              即将截止
            </el-tag>
            <el-tag :type="getStatusTagType(competition.status)">
              {{ getStatusText(competition.status) }}
            </el-tag>
          </div>
        </div>
        <div class="competition-card__meta">
          <span class="meta-item">
            <el-icon><OfficeBuilding /></el-icon>
            {{ competition.organizer }}
          </span>
          <span class="meta-item">
            <el-icon><Trophy /></el-icon>
            {{ getLevelText(competition.level) }}
          </span>
          <span v-if="competition.type" class="meta-item type-pill">
            {{ competition.type }}
          </span>
        </div>
        <p class="competition-card__desc">
          {{ competition.description ? truncate(competition.description, 120) : '暂无描述' }}
        </p>
        <div class="competition-card__footer">
          <div class="time-info">
            <span v-if="competition.signupStartAt">
              <el-icon><Calendar /></el-icon>
              报名：{{ formatDate(competition.signupStartAt) }}
            </span>
          </div>
          <div class="team-info" v-if="competition.teamCount !== undefined">
            <el-icon><User /></el-icon>
            {{ competition.teamCount }} 支队伍
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无相关比赛" />

    <!-- 分页 -->
    <div v-if="total > 0" class="competition-list__pagination">
      <el-pagination
        v-model:current-page="searchParams.page"
        v-model:page-size="searchParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, OfficeBuilding, Trophy, Calendar, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCompetitions, getRecommendedCompetitions, getHotCompetitions } from '@/api/competition'
import type { Competition, CompetitionStatus, CompetitionLevel } from '@/types/competition'

const router = useRouter()

const competitions = ref<Competition[]>([])
const recommended = ref<Competition[]>([])
const hot = ref<Competition[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  page: 1,
  size: 10,
  status: 'PUBLISHED' as CompetitionStatus | string,
  level: '' as CompetitionLevel | string,
  keyword: ''
})

const loadCompetitions = async () => {
  loading.value = true
  try {
    const res = await getCompetitions(searchParams)
    competitions.value = res.records
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载比赛列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadRecommended = async () => {
  try {
    const token = window.localStorage.getItem('token')
    if (!token) return
    const res = await getRecommendedCompetitions({ page: 1, size: 6 })
    recommended.value = res.records.slice(0, 6)
  } catch (e) {
    // 推荐失败不影响主流程
  }
}

const loadHot = async () => {
  try {
    hot.value = await getHotCompetitions(6)
  } catch (e) {
    hot.value = []
  }
}
const handleSearch = () => {
  searchParams.page = 1
  loadCompetitions()
}

const handleReset = () => {
  searchParams.page = 1
  searchParams.size = 10
  searchParams.status = 'PUBLISHED'
  searchParams.level = ''
  searchParams.keyword = ''
  loadCompetitions()
}

const handleViewDetail = (id: number) => {
  router.push({ name: 'CompetitionDetail', params: { id } })
}

const getStatusText = (status: CompetitionStatus | string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

const getStatusTagType = (status: CompetitionStatus | string) => {
  const map: Record<string, 'info' | 'success' | 'warning'> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ARCHIVED: 'warning'
  }
  return map[status] || 'info'
}

const getLevelText = (level: CompetitionLevel | string) => {
  const map: Record<string, string> = {
    SCHOOL: '校级',
    PROVINCE: '省级',
    NATIONAL: '国家级',
    INTERNATIONAL: '国际级'
  }
  return map[level] || level
}

const formatDate = (date: string | Date) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const isClosingSoon = (signupEndAt: any) => {
  if (!signupEndAt) return false
  const end = new Date(signupEndAt).getTime()
  if (Number.isNaN(end)) return false
  const diff = end - Date.now()
  return diff > 0 && diff <= 3 * 24 * 60 * 60 * 1000
}

const truncate = (text: string, maxLength: number) => {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

onMounted(() => {
  loadCompetitions()
  loadRecommended()
  loadHot()
})
</script>

<style scoped lang="scss">
.competition-list {
  padding: 20px;
  min-height: 100vh;
  background: var(--bg-body);

  &__header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__title {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0 0 8px 0;
  }

  &__subtitle {
    font-size: 14px;
    color: var(--text-color-muted);
    margin: 0;
  }

  &__loading {
    margin-top: 20px;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
    margin-top: 20px;
  }

  &__pagination {
    margin-top: 30px;
    display: flex;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .competition-list {
    padding: 12px;

    &__header {
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }

    &__title {
      font-size: 22px;
    }

    &__grid {
      grid-template-columns: 1fr;
      gap: 12px;
    }
  }

  .recommend-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .filter-card :deep(.el-form) {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .filter-card :deep(.el-form-item) {
    width: 100%;
  }

  .filter-card :deep(.el-input),
  .filter-card :deep(.el-select) {
    width: 100%;
  }
}

.filter-card {
  margin-bottom: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.recommend-card {
  margin-top: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.hot-card {
  margin-top: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.recommend-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10px;

  h3 {
    margin: 0;
    font-size: 16px;
    color: var(--text-color);
  }

  .hint {
    font-size: 12px;
    color: var(--text-color-muted);
  }
}

.recommend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-form {
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.competition-card {
  cursor: pointer;
  transition: all 0.3s;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .header-tags {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0;
    flex: 1;
    margin-right: 12px;
  }

  &__meta {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: var(--text-color-muted);

    .el-icon {
      font-size: 14px;
    }
  }

  &__desc {
    font-size: 14px;
    color: var(--text-color-secondary);
    line-height: 1.6;
    margin: 0 0 16px 0;
    min-height: 44px;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid var(--border-subtle);
  }

  .time-info {
    font-size: 12px;
    color: var(--text-color-muted);
    display: flex;
    align-items: center;
    gap: 4px;

    .el-icon {
      font-size: 12px;
    }
  }

  .team-info {
    font-size: 12px;
    color: var(--text-color-muted);
    display: flex;
    align-items: center;
    gap: 4px;

    .el-icon {
      font-size: 12px;
    }
  }
}

.type-pill {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-card-hover);
  font-size: 12px;
  color: var(--text-color-secondary);
}
</style>
