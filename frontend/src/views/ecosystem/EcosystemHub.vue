<template>
  <div class="ecosystem-hub">
    <!-- Hero 区域 -->
    <header class="eh-hero">
      <div class="eh-hero__inner">
        <div class="eh-hero__left">
          <div class="eh-badge">Ecosystem Hub</div>
          <h1 class="eh-title">生态广场</h1>
          <p class="eh-subtitle">探索项目、发现人才、参与比赛、分享成果</p>

          <div class="eh-search">
            <el-input
              v-model="globalSearchKeyword"
              placeholder="全局搜索：项目、人才、比赛、资源..."
              class="eh-search__input"
              clearable
              @keyup.enter="handleGlobalSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button type="primary" @click="handleGlobalSearch">搜索</el-button>
              </template>
            </el-input>
          </div>

          <div class="eh-kpis">
            <div class="eh-kpi">
              <div class="eh-kpi__label">活跃项目</div>
              <div class="eh-kpi__value">{{ stats.projectCount }}</div>
            </div>
            <div class="eh-kpi">
              <div class="eh-kpi__label">在线人才</div>
              <div class="eh-kpi__value">{{ stats.talentCount }}</div>
            </div>
            <div class="eh-kpi">
              <div class="eh-kpi__label">进行中比赛</div>
              <div class="eh-kpi__value">{{ stats.competitionCount }}</div>
            </div>
          </div>
        </div>

        <div class="eh-hero__right" aria-hidden="true">
          <div class="eh-orb eh-orb--a"></div>
          <div class="eh-orb eh-orb--b"></div>
          <div class="eh-orb eh-orb--c"></div>
        </div>
      </div>
    </header>

    <!-- Tab 导航 -->
    <section class="eh-tabs">
      <div class="eh-tabs__inner">
        <el-tabs v-model="activeTab" class="eh-tabs__nav" @tab-change="handleTabChange">
          <el-tab-pane label="项目大厅" name="projects">
            <template #label>
              <span class="tab-label">
                <el-icon><Briefcase /></el-icon>
                项目大厅
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="人才广场" name="talents">
            <template #label>
              <span class="tab-label">
                <el-icon><User /></el-icon>
                人才广场
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="赛事中心" name="competitions">
            <template #label>
              <span class="tab-label">
                <el-icon><Trophy /></el-icon>
                赛事中心
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="资源广场" name="resources">
            <template #label>
              <span class="tab-label">
                <el-icon><FolderOpened /></el-icon>
                资源广场
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane label="动态广场" name="moments">
            <template #label>
              <span class="tab-label">
                <el-icon><ChatLineSquare /></el-icon>
                动态广场
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>

    <!-- 内容区域 -->
    <main class="eh-content">
      <div class="eh-content__inner">
        <component :is="currentTabComponent" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Briefcase, User, Trophy, FolderOpened, ChatLineSquare } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getEcosystemStats } from '@/api/ecosystem'
import ProjectHall from './tabs/ProjectHall.vue'
import TalentSquare from './tabs/TalentSquare.vue'
import CompetitionCenter from './tabs/CompetitionCenter.vue'
import ResourceSquare from './tabs/ResourceSquare.vue'
import MomentSquare from './tabs/MomentSquare.vue'

const route = useRoute()
const router = useRouter()
const activeTab = ref('projects')
const globalSearchKeyword = ref('')

const stats = ref({
  projectCount: 0,
  talentCount: 0,
  competitionCount: 0
})

const currentTabComponent = computed(() => {
  const components: Record<string, any> = {
    projects: ProjectHall,
    talents: TalentSquare,
    competitions: CompetitionCenter,
    resources: ResourceSquare,
    moments: MomentSquare
  }
  return components[activeTab.value]
})

const handleGlobalSearch = () => {
  if (!globalSearchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  // TODO: 实现全局搜索逻辑
  ElMessage.info(`搜索: ${globalSearchKeyword.value}`)
}

const handleTabChange = (tabName: string) => {
  console.log('切换到:', tabName)
  // 更新URL参数，保持标签状态
  router.replace({ query: { ...route.query, tab: tabName } })
}

const loadStats = async () => {
  try {
    const data = await getEcosystemStats()
    stats.value = {
      projectCount: data.projectCount,
      talentCount: data.talentCount,
      competitionCount: data.competitionCount
    }
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
    // 使用默认值
    stats.value = {
      projectCount: 0,
      talentCount: 0,
      competitionCount: 0
    }
  }
}

// 监听路由变化，恢复标签状态
watch(() => route.query.tab, (newTab) => {
  if (newTab && typeof newTab === 'string') {
    const validTabs = ['projects', 'talents', 'competitions', 'resources', 'moments']
    if (validTabs.includes(newTab)) {
      activeTab.value = newTab
    }
  }
}, { immediate: true })

onMounted(() => {
  loadStats()
  // 从URL参数恢复标签状态
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery) {
    const validTabs = ['projects', 'talents', 'competitions', 'resources', 'moments']
    if (validTabs.includes(tabFromQuery)) {
      activeTab.value = tabFromQuery
    }
  }
})
</script>

<style scoped lang="scss">
.ecosystem-hub {
  --eh-bg-page: var(--bg-body);
  --eh-text-main: var(--text-color);
  --eh-text-muted: var(--text-color-muted);
  --eh-border: var(--border-subtle);
  --eh-accent: var(--accent-color);
  --eh-accent-rgb: var(--accent-color-rgb);

  --eh-hero-bg: linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #dbeafe 100%);
  --eh-hero-text: #0f172a;
  --eh-hero-text-muted: #64748b;
  --eh-hero-orb-opacity: 0.6;
  --eh-hero-badge-bg: rgba(var(--eh-accent-rgb), 0.08);
  --eh-search-bg: #ffffff;
  --eh-search-border: var(--border-subtle);

  min-height: 100vh;
  background: var(--eh-bg-page);
  transition: background var(--transition-base);
}

[data-theme='dark'] .ecosystem-hub {
  --eh-hero-bg: linear-gradient(135deg, #020617 0%, #0f172a 40%, #1e293b 80%, #020617 100%);
  --eh-hero-text: #f1f5f9;
  --eh-hero-text-muted: rgba(226, 232, 240, 0.65);
  --eh-hero-orb-opacity: 0.3;
  --eh-hero-badge-bg: rgba(255, 255, 255, 0.06);
  --eh-search-bg: rgba(30, 41, 59, 0.5);
  --eh-search-border: rgba(255, 255, 255, 0.08);
}

.eh-hero {
  position: relative;
  padding: 64px 0 52px;
  color: var(--eh-hero-text);
  background: var(--eh-hero-bg);
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: 
      radial-gradient(700px 400px at 30% 10%, rgba(var(--eh-accent-rgb), 0.1), transparent 60%),
      radial-gradient(700px 400px at 80% 40%, rgba(139, 92, 246, 0.08), transparent 60%);
    pointer-events: none;
  }
}

.eh-hero__inner {
  position: relative;
  z-index: 2;
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 32px;
  align-items: center;
}

.eh-badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--eh-hero-badge-bg);
  border: 1px solid rgba(var(--eh-accent-rgb), 0.1);
  font-size: 12px;
  font-weight: 700;
  color: var(--eh-accent);
}

.eh-title {
  margin: 12px 0;
  font-size: 48px;
  line-height: 1.1;
  font-weight: 900;
}

.eh-subtitle {
  margin-bottom: 32px;
  font-size: 18px;
  color: var(--eh-hero-text-muted);
}

.eh-search {
  max-width: 600px;
  :deep(.el-input__wrapper) {
    height: 54px;
    border-radius: 16px 0 0 16px;
    background: var(--eh-search-bg);
    border: 1px solid var(--eh-search-border);
  }
  :deep(.el-input-group__append) {
    border-radius: 0 16px 16px 0;
    background: var(--eh-accent);
    color: #fff;
    border: none;
    font-weight: 700;
  }
}

.eh-kpis {
  margin-top: 32px;
  display: flex;
  gap: 20px;
}

.eh-kpi {
  padding: 12px 24px;
  border-radius: 16px;
  background: var(--eh-hero-badge-bg);
  border: 1px solid rgba(var(--eh-accent-rgb), 0.1);
  backdrop-filter: blur(10px);
  .eh-kpi__label { font-size: 12px; color: var(--eh-hero-text-muted); }
  .eh-kpi__value { margin-top: 4px; font-size: 26px; font-weight: 900; color: var(--eh-hero-text); }
}

.eh-hero__right { position: relative; height: 320px; }

.eh-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: var(--eh-hero-orb-opacity);
  &.eh-orb--a { width: 240px; height: 240px; right: 0; top: 0; background: radial-gradient(circle, rgba(var(--eh-accent-rgb), 0.6), transparent 70%); }
  &.eh-orb--b { width: 180px; height: 180px; right: 120px; top: 100px; background: radial-gradient(circle, rgba(139, 92, 246, 0.5), transparent 70%); }
  &.eh-orb--c { width: 140px; height: 140px; right: 40px; top: 160px; background: radial-gradient(circle, rgba(16, 185, 129, 0.4), transparent 70%); }
}

.eh-tabs {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-elevated-soft);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--eh-border);
}

.eh-tabs__inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
}

.eh-tabs__nav {
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  
  :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 600;
    padding: 0 24px;
    height: 56px;
    line-height: 56px;
    color: var(--eh-text-muted);
    
    &.is-active {
      color: var(--eh-accent);
    }
  }
  
  .tab-label {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.eh-content {
  padding: 40px 0 80px;
}

.eh-content__inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
}

@media (max-width: 768px) {
  .eh-hero__inner {
    grid-template-columns: 1fr;
  }
  
  .eh-hero__right {
    display: none;
  }
  
  .eh-title {
    font-size: 32px;
  }
  
  .eh-kpis {
    flex-wrap: wrap;
  }
}
</style>
