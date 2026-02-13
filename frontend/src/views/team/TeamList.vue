<template>
  <div class="team-list-page" role="main" aria-label="团队列表页面">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title" id="page-title">团队</h1>
        <el-button 
          v-if="!authStore.hasRole(['MENTOR'])"
          type="primary" 
          :icon="Plus"
          @click="showCreateDialog"
          @keydown.enter="showCreateDialog"
          @keydown.space.prevent="showCreateDialog"
          aria-label="创建新团队"
        >
          创建团队
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选栏 -->
    <div class="search-bar" role="search" aria-label="搜索和筛选团队">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索团队名称..."
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
        @keydown.enter="handleSearch"
        class="search-input"
        aria-label="搜索团队名称"
      />
      
      <el-select
        v-model="statusFilter"
        placeholder="状态筛选"
        clearable
        @change="handleFilterChange"
        @keydown.enter="handleFilterChange"
        class="status-filter"
        aria-label="按状态筛选团队"
      >
        <el-option label="全部" value="" />
        <el-option label="活跃" value="ACTIVE" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      
      <el-select
        v-model="typeFilter"
        placeholder="类型筛选"
        clearable
        @change="handleFilterChange"
        @keydown.enter="handleFilterChange"
        class="type-filter"
        aria-label="按类型筛选团队"
      >
        <el-option label="全部" value="" />
        <el-option label="项目队伍" value="PROJECT" />
        <el-option label="比赛队伍" value="COMPETITION" />
      </el-select>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state" role="status" aria-live="polite" aria-label="正在加载团队列表">
      <div class="skeleton-grid">
        <div v-for="i in 6" :key="i" class="skeleton-item">
          <el-skeleton animated>
            <template #template>
              <div class="skeleton-card">
                <el-skeleton-item variant="image" style="width: 60px; height: 60px; border-radius: 50%;" />
                <div class="skeleton-content">
                  <el-skeleton-item variant="h3" style="width: 60%;" />
                  <el-skeleton-item variant="text" style="width: 80%; margin-top: 8px;" />
                  <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px;" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state" role="alert" aria-live="assertive">
      <el-result
        icon="error"
        title="加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="loadTeams" aria-label="重新加载团队列表">重试</el-button>
        </template>
      </el-result>
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="filteredTeams.length === 0 && !searchKeyword && !statusFilter"
      icon="🏢"
      title="还没有团队"
      :description="authStore.hasRole(['MENTOR']) ? '您还没有加入任何团队' : '创建一个团队开始协作吧'"
      :action-text="authStore.hasRole(['MENTOR']) ? undefined : '创建团队'"
      @action="showCreateDialog"
    />

    <!-- 搜索无结果 -->
    <EmptyState
      v-else-if="filteredTeams.length === 0"
      icon="🔍"
      title="未找到团队"
      description="尝试调整搜索条件或筛选条件"
    />

    <!-- 团队网格 -->
    <div v-else class="team-grid" role="list" aria-label="团队列表" aria-labelledby="page-title">
      <TeamCard
        v-for="team in filteredTeams"
        :key="team.id"
        :team="team"
        role="listitem"
        @click="handleTeamClick(team.id)"
      />
    </div>

    <!-- 创建团队对话框 -->
    <CreateTeamDialog
      v-model="createDialogVisible"
      @success="handleCreateSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import { debounce } from '@/utils/requestOptimization'
import TeamCard from './components/TeamCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import CreateTeamDialog from './components/CreateTeamDialog.vue'
import type { TeamStatus, TeamType } from '@/types/team'

// Stores
const teamStore = useTeamStore()
const authStore = useAuthStore()
const router = useRouter()

// State
const searchKeyword = ref('')
const statusFilter = ref<TeamStatus | ''>('')
const typeFilter = ref<TeamType | ''>('')
const debouncedKeyword = ref('')
const createDialogVisible = ref(false)

// Computed
const loading = computed(() => teamStore.loading)
const error = computed(() => teamStore.error)
const teams = computed(() => teamStore.teams)

// 过滤后的团队列表
const filteredTeams = computed(() => {
  let result = teams.value

  // 状态筛选
  if (statusFilter.value) {
    result = result.filter(team => team.status === statusFilter.value)
  }

  // 类型筛选
  if (typeFilter.value) {
    result = result.filter(team => team.type === typeFilter.value)
  }

  // 搜索关键词筛选
  if (debouncedKeyword.value) {
    const keyword = debouncedKeyword.value.toLowerCase().trim()
    result = result.filter(team => {
      const teamName = team.name || team.teamName || ''
      const teamDesc = team.description || ''
      return teamName.toLowerCase().includes(keyword) ||
             teamDesc.toLowerCase().includes(keyword)
    })
  }

  return result
})

// Methods

/**
 * 加载团队列表
 */
const loadTeams = async () => {
  console.log('🔍 loadTeams 开始')
  
  if (!authStore.user) {
    console.error('❌ authStore.user 为空，无法加载团队列表')
    ElMessage.error('请先登录')
    router.push({ name: 'Login' })
    return
  }

  console.log('✅ 用户已登录，user.id:', authStore.user.id)

  try {
    console.log('📡 调用 teamStore.fetchUserTeams...')
    await teamStore.fetchUserTeams({
      userId: authStore.user.id,
      keyword: debouncedKeyword.value || undefined,
      status: statusFilter.value || undefined
    })
    console.log('✅ fetchUserTeams 完成，teams:', teamStore.teams)
    console.log('  teams.length:', teamStore.teams.length)
  } catch (err: any) {
    console.error('❌ Failed to load teams:', err)
    ElMessage.error(err.message || '加载团队列表失败')
  }
}

/**
 * 处理搜索（带防抖）
 */
const debouncedSearch = debounce(() => {
  debouncedKeyword.value = searchKeyword.value
  loadTeams()
}, 300)

const handleSearch = () => {
  debouncedSearch()
}

/**
 * 处理筛选变化
 */
const handleFilterChange = () => {
  loadTeams()
}

/**
 * 显示创建团队对话框
 */
const showCreateDialog = () => {
  createDialogVisible.value = true
}

/**
 * 处理创建成功
 */
const handleCreateSuccess = (teamId: number) => {
  // 重新加载团队列表
  loadTeams()
  ElMessage.success('团队创建成功，正在跳转...')
}

/**
 * 处理团队卡片点击 - 优化：预加载团队数据
 */
const handleTeamClick = async (teamId: number) => {
  // 立即跳转，不等待数据加载
  router.push({
    name: 'TeamOverview',
    params: { id: teamId }
  })
  
  // 后台预加载团队详情（如果还没加载）
  if (teamStore.currentTeam?.id !== teamId) {
    teamStore.fetchTeamDetail(teamId).catch(err => {
      console.error('Failed to prefetch team detail:', err)
    })
  }
}

// Lifecycle
onMounted(() => {
  console.log('🔍 TeamList onMounted - 开始加载团队列表')
  console.log('  authStore.user:', authStore.user)
  console.log('  authStore.user.id:', authStore.user?.id)
  loadTeams()
})
</script>

<style scoped lang="scss">
.team-list-page {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
  animation: fadeInUp 0.5s var(--ease-out);
}

.page-header {
  margin-bottom: var(--spacing-lg);
  
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    animation: slideInRight 0.4s var(--ease-out);
  }
  
  .page-title {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--text-color);
    background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.search-bar {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  animation: fadeIn 0.6s ease;
  
  .search-input {
    flex: 1;
    max-width: 400px;
    transition: all var(--transition-base);
    
    &:focus-within {
      transform: scale(1.02);
    }
  }
  
  .status-filter {
    width: 150px;
  }
}

.loading-state {
  .skeleton-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: var(--spacing-lg);
  }
  
  .skeleton-item {
    animation: fadeIn 0.3s ease;
    animation-fill-mode: both;
    
    @for $i from 1 through 6 {
      &:nth-child(#{$i}) {
        animation-delay: #{$i * 0.1}s;
      }
    }
  }
  
  .skeleton-card {
    display: flex;
    gap: var(--spacing-md);
    padding: var(--spacing-lg);
    background: var(--bg-card);
    border-radius: var(--radius-card);
    border: 1px solid var(--border-card);
    
    .skeleton-content {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
  }
}

.error-state {
  padding: 60px 20px;
  animation: fadeInUp 0.5s var(--ease-out);
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-lg);
  
  > * {
    animation: fadeInUp 0.5s var(--ease-out);
    animation-fill-mode: both;
    
    @for $i from 1 through 12 {
      &:nth-child(#{$i}) {
        animation-delay: #{$i * 0.05}s;
      }
    }
  }
}

// 页面切换动画
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .team-list-page {
    padding: var(--spacing-md);
  }
  
  .page-header {
    .header-content {
      flex-direction: column;
      align-items: flex-start;
      gap: var(--spacing-md);
    }
  }
  
  .search-bar {
    flex-direction: column;
    
    .search-input {
      max-width: 100%;
    }
    
    .status-filter {
      width: 100%;
    }
  }
  
  .team-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 24px !important;
  }
}
</style>


// Screen reader only class
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
