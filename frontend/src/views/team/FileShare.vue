<template>
  <div class="file-share">
    <div class="file-header">
      <h2>文件共享</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件/文件夹"
          clearable
          style="width: 220px"
        />
        <el-button @click="viewMode = 'grid'" :type="viewMode === 'grid' ? 'primary' : ''">
          <el-icon><Grid /></el-icon>
        </el-button>
        <el-button @click="viewMode = 'list'" :type="viewMode === 'list' ? 'primary' : ''">
          <el-icon><List /></el-icon>
        </el-button>
        <el-button @click="showCreateFolderDialog">
          <el-icon><FolderAdd /></el-icon>
          新建文件夹
        </el-button>
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :show-file-list="false"
          :with-credentials="false"
          name="file"
          multiple
        >
          <el-button type="primary">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
        </el-upload>
      </div>
    </div>

    <!-- Folder Path -->
    <el-breadcrumb separator="/" class="folder-path">
      <el-breadcrumb-item @click="navigateToFolder(null)">根目录</el-breadcrumb-item>
      <el-breadcrumb-item 
        v-for="folder in currentPath" 
        :key="folder.id"
        @click="navigateToFolder(folder.id)"
      >
        {{ folder.name }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <!-- File List -->
    <div v-loading="loading" class="file-container">
      <!-- Grid View -->
      <div v-if="viewMode === 'grid'" class="file-grid">
        <div 
          v-for="file in displayFiles" 
          :key="file.id"
          class="file-card"
          @click="handleFileClick(file)"
        >
          <div class="file-icon" :class="getFileType(file)">
            <el-icon v-if="file.isFolder"><Folder /></el-icon>
            <el-icon v-else-if="isImage(file)"><Picture /></el-icon>
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="file-info">
            <p class="file-name">{{ file.fileName }}</p>
            <p class="file-meta">
              {{ formatFileSize(file.fileSize) }} · {{ formatDate(file.createdAt) }}
            </p>
          </div>
          <div class="file-actions">
            <el-button size="small" text @click.stop="downloadFile(file)">
              <el-icon><Download /></el-icon>
            </el-button>
            <el-button size="small" text type="danger" @click.stop="deleteFile(file)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- List View -->
      <el-table
        v-else
        :data="displayFiles"
        style="width: 100%"
        :default-sort="{ prop: 'createdAt', order: 'descending' }"
      >
        <el-table-column label="名称" min-width="300">
          <template #default="{ row }">
            <div class="file-name-cell" @click="handleFileClick(row)">
              <el-icon v-if="row.isFolder"><Folder /></el-icon>
              <el-icon v-else-if="isImage(row)"><Picture /></el-icon>
              <el-icon v-else><Document /></el-icon>
              <span>{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传者" width="140" />
        <el-table-column prop="createdAt" label="更新时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" text @click="downloadFile(row)">下载</el-button>
            <el-button v-if="!row.isFolder" size="small" text @click="openHistory(row)">历史</el-button>
            <el-button size="small" text type="danger" @click="deleteFile(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Preview Dialog (image / pdf / text) -->
    <el-dialog v-model="previewVisible" :title="previewTitle" width="80%" @closed="cleanupPreviewUrl">
      <img v-if="previewType === 'image'" :src="previewUrl" style="width: 100%" />
      <iframe
        v-else
        :src="previewUrl"
        style="width: 100%; height: 70vh; border: none;"
      />
    </el-dialog>

    <!-- Create Folder Dialog -->
    <el-dialog
      v-model="createFolderVisible"
      title="新建文件夹"
      width="30%"
      :close-on-click-modal="false"
    >
      <el-form
        ref="folderFormRef"
        :model="folderForm"
        :rules="folderRules"
        @submit.prevent
      >
        <el-form-item prop="name">
          <el-input 
            v-model="folderForm.name" 
            placeholder="请输入文件夹名称" 
            @keyup.enter="handleCreateFolder(folderFormRef)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createFolderVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            :loading="createFolderLoading"
            @click="handleCreateFolder(folderFormRef)"
          >
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- File History Drawer -->
    <el-drawer v-model="historyVisible" title="文件历史记录" size="420px">
      <div v-loading="historyLoading">
        <div v-if="historyTarget" style="margin-bottom: 12px; font-weight: 600;">
          {{ historyTarget.fileName }}
        </div>
        <el-timeline v-if="historyItems.length > 0">
          <el-timeline-item
            v-for="item in historyItems"
            :key="item.id"
            :timestamp="formatDate(item.createdAt)"
          >
            <div style="font-weight: 600;">{{ item.username }}</div>
            <div style="color: var(--el-text-color-secondary);">{{ renderHistoryDetail(item) }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无记录" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Grid, List, Upload, Folder, Picture, Document, Download, Delete, FolderAdd } from '@element-plus/icons-vue'
import { useAuthStore } from '@store/auth'
import { getTeamFiles, downloadFile as apiDownloadFile, deleteFile as apiDeleteFile, createTeamFolder, getFileActivities, previewFileBlob, type FileEntity, type ActivityVO } from '@api/file'

const props = defineProps<{
  teamId: number
}>()

const authStore = useAuthStore()
const viewMode = ref<'grid' | 'list'>('grid')
const loading = ref(false)
const files = ref<FileEntity[]>([])
const searchKeyword = ref('')
const currentPath = ref<any[]>([])
const currentFolderId = ref<number | null>(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewTitle = ref('预览')
const previewType = ref<'image' | 'other'>('other')

// History drawer
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyTarget = ref<FileEntity | null>(null)
const historyItems = ref<ActivityVO[]>([])

// Create Folder State
const createFolderVisible = ref(false)
const createFolderLoading = ref(false)
const folderFormRef = ref<FormInstance>()
const folderForm = reactive({
  name: ''
})
const folderRules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' },
    { pattern: /^[^\\/:*?"<>|]+$/, message: '文件名不能包含特殊字符', trigger: 'blur' }
  ]
})

const uploadUrl = computed(() => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const params = new URLSearchParams({ teamId: String(props.teamId) })
  if (currentFolderId.value) {
    params.append('folderId', String(currentFolderId.value))
  }
  return `${baseUrl}/files/upload?${params.toString()}`
})
const uploadHeaders = computed(() => {
  const token = authStore.token
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const loadFiles = async () => {
  loading.value = true
  try {
    const res = await getTeamFiles(props.teamId, currentFolderId.value)
    files.value = res
  } catch (error: any) {
    console.error('Failed to load files:', error)
    ElMessage.error(error.message || '加载文件列表失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.teamId,
  (newId) => {
    if (newId) {
      currentFolderId.value = null
      currentPath.value = []
      loadFiles()
    }
  }
)

const displayFiles = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const list = keyword
    ? files.value.filter(f => (f.fileName || '').toLowerCase().includes(keyword))
    : files.value

  // 文件管理器常见排序：文件夹优先，其次按时间倒序
  return [...list].sort((a, b) => {
    const af = a.isFolder ? 0 : 1
    const bf = b.isFolder ? 0 : 1
    if (af !== bf) return af - bf
    const at = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const bt = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return bt - at
  })
})

const navigateToFolder = (folderId: number | null) => {
  currentFolderId.value = folderId
  if (folderId === null) {
    currentPath.value = []
  } else {
    const folderIndex = currentPath.value.findIndex(f => f.id === folderId)
    if (folderIndex >= 0) {
      currentPath.value = currentPath.value.slice(0, folderIndex + 1)
    }
  }
  loadFiles()
}

const handleFileClick = (file: FileEntity) => {
  if (file.isFolder) {
    currentPath.value.push({ id: file.id, name: file.fileName })
    currentFolderId.value = file.id
    loadFiles()
  } else if (isImage(file)) {
    openPreview(file, 'image')
  } else if (isPreviewable(file)) {
    openPreview(file, 'other')
  }
}

const handleUploadSuccess = (response: any) => {
  // element-plus 的 el-upload：HTTP 200 会触发 success，但后端业务失败也可能是 200 + { code: 500 }
  const code = response?.code
  if (code && code !== 200) {
    ElMessage.error(response?.message || '文件上传失败')
    return
  }
  ElMessage.success('上传成功')
  loadFiles()
}

const handleUploadError = (error: any) => {
  console.error('Upload error:', error)
  const message = error?.message || '文件上传失败，请检查网络连接和登录状态'
  ElMessage.error(message)
}

const downloadFile = async (file: FileEntity) => {
  if (file.isFolder) return
  
  try {
    await apiDownloadFile(file.id)
    ElMessage.success('文件下载中...')
  } catch (error: any) {
    console.error('Failed to download file:', error)
    ElMessage.error(error.message || '文件下载失败')
  }
}

const isPreviewable = (file: FileEntity) => {
  if (file.isFolder) return false
  const ext = file.fileName.split('.').pop()?.toLowerCase() || ''
  if (ext === 'pdf') return true
  // 常见文本类型（像 GitHub 那样直接看）
  return ['txt', 'md', 'log', 'json', 'xml', 'yml', 'yaml', 'js', 'ts', 'css', 'html', 'java', 'py', 'go', 'rs', 'sql', 'sh'].includes(ext)
}

const openPreview = async (file: FileEntity, type: 'image' | 'other') => {
  previewTitle.value = `预览：${file.fileName}`
  previewType.value = type
  previewVisible.value = true
  try {
    const blob = await previewFileBlob(file.id)
    cleanupPreviewUrl()
    previewUrl.value = URL.createObjectURL(blob)
  } catch (error: any) {
    console.error('Failed to preview file:', error)
    ElMessage.error(error.message || '预览失败')
    previewVisible.value = false
  }
}

const cleanupPreviewUrl = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

const openHistory = async (file: FileEntity) => {
  historyTarget.value = file
  historyVisible.value = true
  historyLoading.value = true
  historyItems.value = []
  try {
    historyItems.value = await getFileActivities(file.id, 50)
  } catch (error: any) {
    console.error('Failed to load file history:', error)
    ElMessage.error(error.message || '加载历史记录失败')
  } finally {
    historyLoading.value = false
  }
}

const renderHistoryDetail = (item: ActivityVO) => {
  const action = item.action
  if (action === 'upload') return '上传了该文件'
  if (action === 'download') return '下载了该文件'
  if (action === 'delete') return '删除了该文件'
  return item.detail || action || '-'
}

const deleteFile = async (file: FileEntity) => {
  try {
    await ElMessageBox.confirm(`确定删除 ${file.fileName}?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    loading.value = true
    await apiDeleteFile(file.id)
    ElMessage.success('删除成功')
    await loadFiles()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete file:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

const showCreateFolderDialog = () => {
  folderForm.name = ''
  createFolderVisible.value = true
}

const handleCreateFolder = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  
  await formEl.validate(async (valid) => {
    if (valid) {
      createFolderLoading.value = true
      try {
        await createTeamFolder(props.teamId, folderForm.name, currentFolderId.value)
        ElMessage.success('文件夹创建成功')
        createFolderVisible.value = false
        loadFiles()
      } catch (error: any) {
        console.error('Failed to create folder:', error)
        ElMessage.error(error.message || '文件夹创建失败')
      } finally {
        createFolderLoading.value = false
      }
    }
  })
}

const isImage = (file: FileEntity) => {
  if (file.isFolder) return false
  const ext = file.fileName.split('.').pop()?.toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')
}

const getFileType = (file: FileEntity) => {
  if (file.isFolder) return 'folder'
  if (isImage(file)) return 'image'
  return 'document'
}

const formatFileSize = (bytes: number | undefined) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (date: any) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped lang="scss">
.file-share {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.folder-path {
  margin-bottom: 20px;
  
  :deep(.el-breadcrumb__item) {
    cursor: pointer;
    
    &:hover {
      color: var(--el-color-primary);
    }
  }
}

.file-container {
  flex: 1;
  overflow-y: auto;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.file-card {
  background: var(--bg-elevated);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border-subtle);

  &:hover {
    box-shadow: var(--shadow-card-hover);
    transform: translateY(-2px);
  }

  .file-icon {
    font-size: 48px;
    text-align: center;
    margin-bottom: 12px;

    &.folder {
      color: var(--el-color-warning);
    }

    &.image {
      color: var(--el-color-primary);
    }

    &.document {
      color: var(--text-color-secondary);
    }
  }

  .file-info {
    .file-name {
      margin: 0 0 4px 0;
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .file-meta {
      margin: 0;
      font-size: 12px;
      color: var(--text-color-muted);
    }
  }

  .file-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 12px;
  }
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
  }
}

[data-theme='dark'] {
  .file-card {
    background: var(--bg-elevated);
    border-color: var(--border-subtle);
  }
}
</style>
