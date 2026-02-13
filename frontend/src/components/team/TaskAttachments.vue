<template>
  <div class="task-attachments">
    <div class="attachments-header">
      <h4>附件 ({{ attachments.length }})</h4>
      <el-upload
        ref="uploadRef"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-progress="handleUploadProgress"
        :show-file-list="false"
        :disabled="uploading"
      >
        <el-button 
          type="primary" 
          :icon="Upload"
          size="small"
          :loading="uploading"
        >
          上传附件
        </el-button>
      </el-upload>
    </div>

    <div v-if="uploading" class="upload-progress">
      <el-progress 
        :percentage="uploadProgress" 
        :status="uploadProgress === 100 ? 'success' : undefined"
      />
      <span class="progress-text">正在上传: {{ uploadingFileName }}</span>
    </div>

    <div class="attachments-list">
      <div 
        v-for="attachment in attachments" 
        :key="attachment.id"
        class="attachment-item"
      >
        <div class="attachment-icon">
          <el-icon :size="24">
            <Document />
          </el-icon>
        </div>
        
        <div class="attachment-info">
          <div class="attachment-name">{{ attachment.fileName }}</div>
          <div class="attachment-meta">
            <span class="file-size">{{ formatFileSize(attachment.fileSize) }}</span>
            <span class="divider">•</span>
            <span class="uploader">{{ attachment.uploaderName }}</span>
            <span class="divider">•</span>
            <span class="upload-time">{{ formatTime(attachment.uploadedAt) }}</span>
          </div>
        </div>

        <div class="attachment-actions">
          <el-button
            type="primary"
            size="small"
            :icon="Download"
            link
            @click="handleDownload(attachment)"
            :loading="downloadingId === attachment.id"
          >
            下载
          </el-button>
          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            link
            @click="handleDelete(attachment)"
            :loading="deletingId === attachment.id"
          >
            删除
          </el-button>
        </div>
      </div>

      <el-empty 
        v-if="attachments.length === 0 && !loading" 
        description="暂无附件" 
        :image-size="80"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Download, Delete, Document } from '@element-plus/icons-vue'
import { request } from '@/utils/request'
import { useAuthStore } from '@/store/auth'
import type { UploadProgressEvent } from 'element-plus'

interface TaskAttachment {
  id: number
  taskId: number
  fileName: string
  fileSize: number
  uploadedBy: number
  uploaderName: string
  uploadedAt: string
}

const props = defineProps<{
  taskId: number
}>()

const emit = defineEmits<{
  update: []
}>()

const authStore = useAuthStore()

const attachments = ref<TaskAttachment[]>([])
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')
const deletingId = ref<number | null>(null)
const downloadingId = ref<number | null>(null)
const uploadRef = ref()

const uploadUrl = computed(() => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  return `${baseUrl}/tasks/${props.taskId}/attachments`
})

const uploadHeaders = computed(() => {
  const token = authStore.token
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const uploadData = computed(() => ({
  uploadedBy: authStore.user?.id || 0
}))

const fetchAttachments = async () => {
  loading.value = true
  try {
    const response = await request.get<TaskAttachment[]>(
      `/tasks/${props.taskId}/attachments`
    )
    attachments.value = Array.isArray(response) ? response : []
  } catch (error: any) {
    console.error('Failed to fetch attachments:', error)
    ElMessage.error(error.message || '获取附件列表失败')
  } finally {
    loading.value = false
  }
}

const beforeUpload = (file: File) => {
  // 验证文件大小
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过 10MB，当前文件大小为 ${formatFileSize(file.size)}`)
    return false
  }

  // 验证文件名长度
  if (file.name.length > 255) {
    ElMessage.error('文件名长度不能超过 255 个字符')
    return false
  }

  // 验证文件类型
  const allowedTypes = [
    'image/jpeg', 'image/png', 'image/gif',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/zip',
    'application/x-rar-compressed'
  ]

  const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.zip', '.rar']
  const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()

  if (!allowedTypes.includes(file.type) && !allowedExtensions.includes(fileExtension)) {
    ElMessage.error('不支持的文件类型。支持的格式：图片(jpg, png, gif)、文档(pdf, doc, docx, xls, xlsx)、压缩包(zip, rar)')
    return false
  }

  // 验证文件不为空
  if (file.size === 0) {
    ElMessage.error('不能上传空文件')
    return false
  }

  uploadingFileName.value = file.name
  uploading.value = true
  uploadProgress.value = 0

  return true
}

const handleUploadProgress = (event: UploadProgressEvent) => {
  uploadProgress.value = Math.round(event.percent || 0)
}

const handleUploadSuccess = (response: any) => {
  uploading.value = false
  uploadProgress.value = 0
  uploadingFileName.value = ''

  if (response.code === 200 && response.data) {
    attachments.value.push(response.data)
    ElMessage.success('附件上传成功')
    emit('update')
  } else {
    ElMessage.error(response.message || '附件上传失败')
  }
}

const handleUploadError = (error: any) => {
  uploading.value = false
  uploadProgress.value = 0
  uploadingFileName.value = ''
  
  console.error('Upload error:', error)
  ElMessage.error('附件上传失败')
}

const handleDownload = async (attachment: TaskAttachment) => {
  downloadingId.value = attachment.id
  try {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    const url = `${baseUrl}/tasks/${props.taskId}/attachments/${attachment.id}/download`
    
    const token = authStore.token
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })

    if (!response.ok) {
      throw new Error('下载失败')
    }

    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = attachment.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    ElMessage.success('下载成功')
  } catch (error: any) {
    console.error('Failed to download attachment:', error)
    ElMessage.error(error.message || '下载附件失败')
  } finally {
    downloadingId.value = null
  }
}

const handleDelete = async (attachment: TaskAttachment) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除附件 "${attachment.fileName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    deletingId.value = attachment.id
    try {
      await request.delete(
        `/tasks/${props.taskId}/attachments/${attachment.id}`
      )
      
      attachments.value = attachments.value.filter(a => a.id !== attachment.id)
      ElMessage.success('删除附件成功')
      emit('update')
    } catch (error: any) {
      console.error('Failed to delete attachment:', error)
      ElMessage.error(error.message || '删除附件失败')
    } finally {
      deletingId.value = null
    }
  } catch {
    // User cancelled
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes === 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

onMounted(() => {
  fetchAttachments()
})

defineExpose({
  refresh: fetchAttachments
})
</script>

<style scoped lang="scss">
.task-attachments {
  display: flex;
  flex-direction: column;
  height: 100%;

  .attachments-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
      color: var(--text-color);
    }
  }

  .upload-progress {
    margin-bottom: 16px;
    padding: 12px;
    background-color: var(--bg-elevated-soft);
    border-radius: 8px;

    .progress-text {
      display: block;
      margin-top: 8px;
      font-size: 12px;
      color: var(--text-color-secondary);
    }
  }

  .attachments-list {
    flex: 1;
    overflow-y: auto;
    max-height: 400px;
    padding-right: 8px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: var(--border-card);
      border-radius: 3px;

      &:hover {
        background-color: var(--border-subtle);
      }
    }

    .attachment-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      margin-bottom: 8px;
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        background-color: var(--bg-card-hover);
        border-color: var(--border-card-hover);

        .attachment-actions {
          opacity: 1;
        }
      }

      .attachment-icon {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        background-color: var(--accent-soft);
        border-radius: 8px;
        color: var(--el-color-primary);
      }

      .attachment-info {
        flex: 1;
        min-width: 0;

        .attachment-name {
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color);
          margin-bottom: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .attachment-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12px;
          color: var(--text-color-muted);

          .divider {
            color: var(--border-subtle);
          }
        }
      }

      .attachment-actions {
        flex-shrink: 0;
        display: flex;
        gap: 8px;
        opacity: 0;
        transition: opacity 0.3s;
      }
    }
  }
}
</style>
