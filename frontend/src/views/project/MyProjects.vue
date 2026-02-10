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
      <el-tab-pane label="招募中" name="RECRUITING" />
      <el-tab-pane label="草稿" name="DRAFT" />
    </el-tabs>

    <el-skeleton :loading="loading" animated :count="4">
      <template #default>
        <div v-if="projects.length > 0" class="my-projects__grid">
          <el-card v-for="p in projects" :key="p.id" class="my-projects__card" shadow="hover">
            <div class="my-projects__card-header">
              <div class="my-projects__card-title">{{ p.title }}</div>
              <el-tag :type="p.status === 'RECRUITING' ? 'success' : 'info'" size="small">{{ p.status }}</el-tag>
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
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { getProjects, publishProject } from '@/api/project'
import CreateProjectWizard from '@/components/project/CreateProjectWizard.vue'
import EditProjectDialog from '@/components/project/EditProjectDialog.vue'
import type { Project } from '@/types/project'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<'DRAFT' | 'RECRUITING'>('RECRUITING')
const loading = ref(false)
const projects = ref<Project[]>([])
const publishingId = ref<number | null>(null)
const showCreateWizard = ref(false)
const editDialogVisible = ref(false)
const editingProject = ref<Project | null>(null)

const load = async () => {
  loading.value = true
  try {
    const res = await getProjects({
      page: 1,
      size: 50,
      status: activeTab.value,
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

onMounted(() => {
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
