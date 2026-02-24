<template>
  <div class="team-settings">
    <div class="page-header">
      <div class="title-area">
        <h2 class="page-title">团队设置</h2>
        <div class="page-subtitle">配置团队常用工具与快捷入口</div>
      </div>

      <div class="actions">
        <el-button @click="resetForm" :disabled="loading || saving || !isDirty">重置</el-button>
        <el-button type="primary" @click="saveSettings" :loading="saving" :disabled="loading || !isDirty">
          保存设置
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="10" />

    <el-form
      v-else
      ref="formRef"
      class="settings-form"
      :model="settings"
      :rules="rules"
      label-position="top"
      @submit.prevent
    >
      <div class="layout">
        <div class="main">
          <el-card class="card" shadow="never">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Link /></el-icon>
                  <span>代码仓库</span>
                </div>
              </div>
            </template>

            <div class="grid">
              <el-form-item label="GitHub 仓库" prop="github_repo_url">
                <el-input v-model="settings.github_repo_url" placeholder="https://github.com/owner/repo" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.github_repo_url)" :disabled="!settings.github_repo_url" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="Gitee 仓库" prop="gitee_repo_url">
                <el-input v-model="settings.gitee_repo_url" placeholder="https://gitee.com/owner/repo" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.gitee_repo_url)" :disabled="!settings.gitee_repo_url" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </div>
          </el-card>

          <el-card class="card" shadow="never">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><VideoCamera /></el-icon>
                  <span>会议工具</span>
                </div>
              </div>
            </template>

            <div class="grid">
              <el-form-item label="腾讯会议" prop="meeting_link">
                <el-input v-model="settings.meeting_link" placeholder="https://meeting.tencent.com/xxx" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.meeting_link)" :disabled="!settings.meeting_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="钉钉群" prop="dingtalk_group">
                <el-input v-model="settings.dingtalk_group" placeholder="群号或链接" clearable />
              </el-form-item>
            </div>
          </el-card>

          <el-card class="card" shadow="never">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Document /></el-icon>
                  <span>文档工具</span>
                </div>
              </div>
            </template>

            <div class="grid">
              <el-form-item label="腾讯文档" prop="document_link">
                <el-input v-model="settings.document_link" placeholder="https://docs.qq.com/xxx" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.document_link)" :disabled="!settings.document_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="石墨文档" prop="shimo_doc_link">
                <el-input v-model="settings.shimo_doc_link" placeholder="https://shimo.im/xxx" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.shimo_doc_link)" :disabled="!settings.shimo_doc_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </div>
          </el-card>

          <el-card class="card" shadow="never">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Picture /></el-icon>
                  <span>设计工具</span>
                </div>
              </div>
            </template>

            <div class="grid">
              <el-form-item label="Figma 设计稿" prop="design_link">
                <el-input v-model="settings.design_link" placeholder="https://www.figma.com/xxx" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.design_link)" :disabled="!settings.design_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="蓝湖设计稿" prop="lanhu_link">
                <el-input v-model="settings.lanhu_link" placeholder="https://lanhuapp.com/xxx" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.lanhu_link)" :disabled="!settings.lanhu_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </div>
          </el-card>

          <el-card class="card" shadow="never">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><More /></el-icon>
                  <span>其他工具</span>
                </div>
              </div>
            </template>

            <div class="grid">
              <el-form-item label="项目管理工具" prop="project_tool_link">
                <el-input v-model="settings.project_tool_link" placeholder="Jira / Trello 等" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.project_tool_link)" :disabled="!settings.project_tool_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="API 文档" prop="api_doc_link">
                <el-input v-model="settings.api_doc_link" placeholder="Swagger / Apifox 等" clearable>
                  <template #append>
                    <el-tooltip content="打开链接" placement="top">
                      <el-button :icon="Link" @click="openLink(settings.api_doc_link)" :disabled="!settings.api_doc_link" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </div>
          </el-card>
        </div>

        <div class="side">
          <el-card class="card" shadow="never">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Grid /></el-icon>
                  <span>快捷入口</span>
                </div>
              </div>
            </template>

            <div class="shortcut-grid">
              <button
                v-for="shortcut in shortcuts"
                :key="shortcut.key"
                class="shortcut"
                type="button"
                :disabled="!settings[shortcut.key]"
                @click="openLink(settings[shortcut.key])"
              >
                <el-icon :size="22" :style="{ color: shortcut.color }"><component :is="shortcut.icon" /></el-icon>
                <div class="shortcut-name">{{ shortcut.name }}</div>
                <div class="shortcut-desc">{{ settings[shortcut.key] ? '已配置' : '未配置' }}</div>
              </button>
            </div>

            <el-divider />

            <div class="tips">
              <div class="tip">支持直接粘贴完整 URL，保存后团队成员可快速访问。</div>
              <div class="tip">未配置的快捷入口将自动置灰。</div>
            </div>
          </el-card>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  Link,
  VideoCamera,
  Document,
  Picture,
  More,
  Grid
} from '@element-plus/icons-vue'
import { request } from '@/utils/request'
import { useRoute } from 'vue-router'

const route = useRoute()
const teamId = computed(() => Number(route.params.id))

const formRef = ref<FormInstance>()
const loading = ref(true)
const saving = ref(false)

const settings = reactive<Record<string, string>>({
  github_repo_url: '',
  gitee_repo_url: '',
  meeting_link: '',
  dingtalk_group: '',
  document_link: '',
  shimo_doc_link: '',
  design_link: '',
  lanhu_link: '',
  project_tool_link: '',
  api_doc_link: ''
})

const originalSettings = ref<Record<string, string>>({
  github_repo_url: '',
  gitee_repo_url: '',
  meeting_link: '',
  dingtalk_group: '',
  document_link: '',
  shimo_doc_link: '',
  design_link: '',
  lanhu_link: '',
  project_tool_link: '',
  api_doc_link: ''
})

const rules = {
  github_repo_url: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  gitee_repo_url: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  meeting_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  document_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  shimo_doc_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  design_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  lanhu_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  project_tool_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  api_doc_link: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }]
}

const shortcuts = [
  { key: 'github_repo_url', name: 'GitHub', icon: Link, color: '#24292e' },
  { key: 'meeting_link', name: '腾讯会议', icon: VideoCamera, color: '#2d8cf0' },
  { key: 'document_link', name: '腾讯文档', icon: Document, color: '#409eff' },
  { key: 'design_link', name: 'Figma', icon: Picture, color: '#a259ff' }
]

const isDirty = computed(() => {
  return JSON.stringify(settings) !== JSON.stringify(originalSettings.value)
})

const loadSettings = async () => {
  loading.value = true
  try {
    const data = await request.get(`/teams/${teamId.value}/settings`)
    const merged = { ...settings, ...data }
    Object.assign(settings, merged)
    originalSettings.value = JSON.parse(JSON.stringify(merged))
  } catch (error) {
    console.error('加载设置失败', error)
    ElMessage.error('无法获取团队设置，请稍后重试')
  } finally {
    loading.value = false
  }
}

const saveSettings = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请检查输入的链接格式是否正确')
      return
    }

    saving.value = true
    try {
      await request.put(`/teams/${teamId.value}/settings`, settings)
      ElMessage.success('设置已成功更新')
      originalSettings.value = JSON.parse(JSON.stringify(settings))
    } catch (error) {
      ElMessage.error('保存设置失败')
    } finally {
      saving.value = false
    }
  })
}

const resetForm = () => {
  if (!isDirty.value) return
  ElMessageBox.confirm('确定要放弃当前的修改并重置吗？', '提示', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    Object.assign(settings, JSON.parse(JSON.stringify(originalSettings.value)))
    ElMessage.info('已恢复至初始状态')
  })
}

const openLink = (url: string) => {
  if (!url) return
  let targetUrl = url
  if (!url.startsWith('http')) {
    targetUrl = 'https://' + url
  }
  window.open(targetUrl, '_blank')
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped lang="scss">
.team-settings {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  .page-title {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    letter-spacing: -0.5px;
  }

  .page-subtitle {
    margin-top: 8px;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  .actions {
    display: flex;
    gap: 12px;
  }
}

.layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card {
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }

  .card-header {
    .card-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);

      .el-icon {
        font-size: 18px;
        color: var(--el-color-primary);
      }
    }
  }
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.shortcut-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;

  &:hover:not(:disabled) {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    transform: translateY(-2px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: var(--el-fill-color-light);
  }

  .shortcut-name {
    margin-top: 10px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .shortcut-desc {
    margin-top: 4px;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }
}

.tips {
  .tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 8px;
    line-height: 1.6;
    position: relative;
    padding-left: 14px;

    &::before {
      content: "•";
      position: absolute;
      left: 0;
      color: var(--el-color-primary);
    }
  }
}

:deep(.el-form-item__label) {
  font-weight: 500;
  padding-bottom: 8px;
}

:deep(.el-input-group__append) {
  padding: 0;
  background-color: var(--el-fill-color-light);
  position: relative;
  z-index: 2;
  border-left: none;
  border-top-right-radius: var(--el-input-border-radius);
  border-bottom-right-radius: var(--el-input-border-radius);
  overflow: hidden;
}

:deep(.el-input__wrapper.is-focus) {
  z-index: 1;
}

:deep(.el-input-group__append .el-button) {
  width: 40px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-left: 0;
}

:deep(.el-input-group__append) {
  border-left: 0;
}

:deep(.el-input-group__append .el-button .el-icon) {
  margin: 0;
}

:deep(.el-input-group__append .el-button:hover) {
  color: var(--el-color-primary);
}
</style>
