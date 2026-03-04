<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import { getProjects } from '@/api/project'
import CreateProjectWizard from '@/components/project/CreateProjectWizard.vue'
import type { Project } from '@/types/project'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isProfileComplete = computed(() => {
  const p: any = authStore.user?.profile
  if (!p) return false
  const requiredKeys = ['realName', 'department', 'major', 'grade', 'wechat', 'qq', 'bio', 'projectExperience']
  return requiredKeys.every((k) => {
    const v = p?.[k]
    if (v === null || v === undefined) return false
    if (typeof v === 'string') return v.trim().length > 0
    return true
  })
})

const handleCreateClick = () => {
  if (!isProfileComplete.value) {
    ElMessage.warning('请先在“我的”页面完善个人资料后再创建项目')
    return
  }
  showCreateWizard.value = true
}

const projects = ref<Project[]>([])
const total = ref(0)
const loading = ref(false)
const debugInfo = ref('页面已加载')
const showCreateWizard = ref(false)

const searchParams = reactive({
  page: 1,
  size: 12,
  type: '',
  status: 'RECRUITING',
  keyword: '',
  roles: [] as string[],
  techStack: [] as string[]
})

const getPlainDesc = (html: any) => {
  const text = String(html || '')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > 80 ? text.slice(0, 80) + '…' : text
}

const loadProjects = async () => {
  loading.value = true
  debugInfo.value = '正在加载项目...'
  try {
    const res = await getProjects(searchParams)
    debugInfo.value = `API 返回: ${JSON.stringify(res).slice(0, 100)}`
    projects.value = res.list
    total.value = res.total
  } catch (error) {
    debugInfo.value = `加载错误: ${error}`
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 预加载详情页组件，提升跳转速度
const preloadProjectDetail = () => {
  import('../project/ProjectDetail.vue').catch(() => {
    // 预加载失败时忽略，真实跳转再加载
  })
}

const handleOpenProject = (id: number) => {
  router.push({ name: 'ProjectDetail', params: { id } })
}

const readPreference = () => {
  try {
    const raw = localStorage.getItem('userProjectPreference')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.value as string | undefined
  } catch {
    return null
  }
}

const maybeTriggerGuide = () => {
  const q = route.query
  const fromQuery = q.showCreateGuide ? 'create' : q.showFindGuide ? 'find' : null
  // 只允许通过 query 触发一次性引导，避免以后每次进广场都自动弹创建向导
  if (!fromQuery) return

  if (fromQuery === 'create') {
    showCreateWizard.value = true
  }

  // 清理 query，避免刷新/再次进入时重复触发
  router.replace({ name: 'Project', query: {} })
}

onMounted(() => {
  // 预加载详情页组件，提升跳转速度
  preloadProjectDetail()
  maybeTriggerGuide()
  loadProjects()
})
</script>

<template>
  <div class="project-square">
    <div class="project-square__header">
      <div>
        <h1 class="project-square__title">探索项目</h1>
        <p class="project-square__subtitle">发现感兴趣的项目，组建你的梦之队</p>
      </div>
      <el-button 
        v-if="!authStore.hasRole(['MENTOR'])" 
        type="primary" 
        :disabled="!isProfileComplete" 
        @click="handleCreateClick"
      >
        创建项目
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="project-square__loading">
      <p>加载中...</p>
    </div>

    <!-- 项目列表 -->
    <div v-else-if="projects.length > 0" class="project-square__grid">
      <div 
        v-for="project in projects" 
        :key="project.id" 
        class="project-square__card"
        @click="handleOpenProject(project.id)"
      >
        <h3 class="project-square__card-title">{{ project.title }}</h3>
        <p class="project-square__card-desc">{{ getPlainDesc(project.description) }}</p>
        <div class="project-square__card-tags">
          <span class="tag tag--type">
            {{ project.projectType }}
          </span>
          <span class="tag tag--status">
            {{ project.status }}
          </span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="project-square__empty">
      <p class="emoji">🔍</p>
      <h3>暂无相关项目</h3>
      <p>试着调整筛选条件，或者创建一个新项目</p>
    </div>
    <CreateProjectWizard v-model="showCreateWizard" @success="loadProjects" />
  </div>
</template>

<style scoped lang="scss">
.project-square {
  padding: 20px;
  min-height: 100vh;
  background: var(--bg-body);
  color: var(--text-color);
}

.project-square__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.project-square__title {
  font-size: 28px;
  margin-bottom: 8px;
}

.project-square__subtitle {
  margin: 0;
  color: var(--text-color-muted);
}

.project-square__loading {
  text-align: center;
  padding: 40px;

  p {
    color: var(--text-color-muted);
  }
}

.project-square__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.project-square__card {
  background: var(--bg-elevated);
  padding: 20px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
}

.project-square__card-title {
  margin: 0 0 8px 0;
}

.project-square__card-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-secondary);
}

.project-square__card-tags {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.tag--type {
  background: rgba(59, 130, 246, 0.12);
  color: var(--el-color-primary);
}

.tag--status {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}

.project-square__empty {
  text-align: center;
  padding: 60px;
  background: var(--bg-elevated);
  border-radius: 12px;

  h3 {
    margin: 16px 0 8px;
  }

  p {
    color: var(--text-color-muted);
  }

  .emoji {
    font-size: 48px;
    margin: 0;
  }
}
</style>
