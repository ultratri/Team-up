<template>
  <div class="my-projects">
    <div class="my-projects__header">
      <div>
        <h1 class="my-projects__title">我的项目</h1>
        <p class="my-projects__subtitle">管理草稿与已发布项目，草稿需要发布后才会出现在项目广场</p>
      </div>
      <div style="display: flex; gap: 10px;">
        <el-button 
          v-if="!authStore.hasRole(['MENTOR'])" 
          type="primary" 
          @click="showCreateWizard = true"
        >
          创建项目
        </el-button>
        <el-button @click="handleRefresh" :loading="loading">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="草稿" name="DRAFT" />
      <el-tab-pane label="招募中" name="RECRUITING" />
      <el-tab-pane label="进行中" name="IN_PROGRESS" />
      <el-tab-pane label="已完成" name="COMPLETED" />
    </el-tabs>

    <el-skeleton :loading="loading" animated :count="4">
      <template #default>
        <div v-if="projects.length > 0" class="my-projects__grid">
          <el-card v-for="p in projects" :key="p.id" class="my-projects__card" shadow="hover">
            <div class="my-projects__card-header">
              <div class="my-projects__card-title">{{ p.title }}</div>
              <el-tag :type="getStatusType(p.status)" size="small">{{ getStatusText(p.status) }}</el-tag>
            </div>

            <div class="my-projects__card-desc" v-html="p.description" />

            <div class="my-projects__actions">
              <el-button size="small" @click="handleView(p.id)">查看</el-button>
              <el-button size="small" type="default" @click="handleEdit(p)">编辑</el-button>
              <el-button
                v-if="p.status === 'DRAFT'"
                type="primary"
                size="small"
                :loading="publishingId === p.id"
                @click="handlePublish(p.id)"
              >
                发布
              </el-button>
              <el-button
                v-if="p.status === 'DRAFT'"
                type="danger"
                size="small"
                @click="handleDelete(p.id)"
              >
                删除
              </el-button>
              <el-button
                v-if="p.status === 'COMPLETED'"
                type="warning"
                size="small"
                @click="handleArchive(p.id)"
              >
                归档
              </el-button>
            </div>
          </el-card>
        </div>

        <el-empty v-else description="暂无项目" />
      </template>
    </el-skeleton>

    <CreateProjectWizard v-model="showCreateWizard" @success="handleRefresh" />
    <EditProjectDialog v-model="editDialogVisible" :project="editingProject" @saved="handleRefresh" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { getMyProjects, publishProject, updateProject, deleteProject } from '@/api/project'
import CreateProjectWizard from '@/components/project/CreateProjectWizard.vue'
import EditProjectDialog from '@/components/project/EditProjectDialog.vue'
import type { Project } from '@/types/project'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<string>('')
const loading = ref(false)
const projects = ref<Project[]>([])
const publishingId = ref<number | null>(null)
const showCreateWizard = ref(false)
const editDialogVisible = ref(false)
const editingProject = ref<Project | null>(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getMyProjects({
      page: 1,
      size: 50,
      status: activeTab.value || undefined,
    })
    projects.value = res.list
  } catch (e) {
    projects.value = []
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => load()

const handleTabChange = () => load()

const handleView = (id: number) => {
  router.push({ name: 'ProjectDetail', params: { id } })
}

const handleEdit = (p: Project) => {
  editingProject.value = p
  editDialogVisible.value = true
}

const handlePublish = async (id: number) => {
  publishingId.value = id
  try {
    await publishProject(id)
    ElMessage.success('发布成功')
    await load()
  } finally {
    publishingId.value = null
  }
}

const handleArchive = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要归档此项目吗？归档后项目将不再显示在主列表中。',
      '确认归档',
      {
        confirmButtonText: '确定归档',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateProject(id, { status: 'ARCHIVED' })
    ElMessage.success('项目已归档')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('归档失败')
    }
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除此草稿项目吗？删除后不可恢复。',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await deleteProject(id)
    ElMessage.success('项目已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    DRAFT: 'info',
    RECRUITING: 'success',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
    ARCHIVED: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    DRAFT: '草稿',
    RECRUITING: '招募中',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    ARCHIVED: '已归档'
  }
  return texts[status] || status
}

onMounted(() => {
  // 预加载项目详情页组件，加快从“我的项目”跳转到详情的首屏速度
  // 不影响代码分包，只是在空闲时提前下载对应 chunk
  import('./ProjectDetail.vue').catch(() => {
    // 静默忽略预加载失败，实际跳转时再由路由正常加载
  })
  load()
})
</script>

<style scoped lang="scss">
.my-projects {
  padding: 20px;
  min-height: 100vh;
  background: var(--bg-body);
  color: var(--text-color);
}

.my-projects__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.my-projects__title {
  font-size: 28px;
  margin-bottom: 6px;
}

.my-projects__subtitle {
  margin: 0;
  color: var(--text-color-muted);
}

.my-projects__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.my-projects__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.my-projects__card-title {
  font-weight: 700;
}

.my-projects__card-desc {
  color: var(--text-color-secondary);
  font-size: 14px;
  max-height: 120px;
  overflow: hidden;
}

.my-projects__actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}
</style>
