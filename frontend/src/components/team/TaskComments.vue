<template>
  <div class="task-comments">
    <div class="comments-header">
      <h4>评论 ({{ comments.length }})</h4>
    </div>

    <!-- 评论列表 -->
    <div ref="commentsListRef" class="comments-list">
      <div 
        v-for="comment in comments" 
        :key="comment.id" 
        class="comment-item"
      >
        <el-avatar 
          :size="36" 
          :src="comment.avatar" 
          :alt="comment.userName"
          class="comment-avatar"
        >
          {{ comment.userName?.charAt(0) }}
        </el-avatar>
        
        <div class="comment-content">
          <div class="comment-header">
            <span class="comment-author">{{ comment.userName }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <div class="comment-text">{{ comment.content }}</div>
        </div>

        <!-- 删除按钮 - 只有评论作者可以删除 -->
        <el-button
          v-if="canDeleteComment(comment)"
          type="danger"
          size="small"
          :icon="Delete"
          circle
          @click="handleDelete(comment)"
          :loading="deletingId === comment.id"
          class="delete-btn"
        />
      </div>

      <!-- 空状态 -->
      <el-empty 
        v-if="comments.length === 0 && !loading" 
        description="暂无评论" 
        :image-size="80"
      />
    </div>

    <!-- 评论输入框 -->
    <div class="comment-input-wrapper">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="3"
        placeholder="写下你的评论..."
        :disabled="loading || submitting"
        maxlength="1000"
        show-word-limit
        @keydown.ctrl.enter="handleSubmit"
        @keydown.meta.enter="handleSubmit"
      />
      <div class="input-actions">
        <span class="input-hint">Ctrl+Enter 发送</span>
        <el-button 
          type="primary" 
          :icon="Promotion"
          @click="handleSubmit"
          :loading="submitting"
          :disabled="!newComment.trim() || loading"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Promotion } from '@element-plus/icons-vue'
import { request } from '@/utils/request'
import { useAuthStore } from '@/store/auth'

interface TaskComment {
  id: number
  taskId: number
  userId: number
  userName: string
  avatar?: string
  content: string
  createdAt: string
}

const props = defineProps<{
  taskId: number
}>()

const emit = defineEmits<{
  update: []
}>()

const authStore = useAuthStore()

// State
const comments = ref<TaskComment[]>([])
const newComment = ref('')
const loading = ref(false)
const submitting = ref(false)
const deletingId = ref<number | null>(null)
const commentsListRef = ref<HTMLElement | null>(null)

// Methods
const fetchComments = async () => {
  loading.value = true
  try {
    const response = await request.get<TaskComment[]>(
      `/tasks/${props.taskId}/comments`
    )
    comments.value = Array.isArray(response) ? response : []
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error: any) {
    console.error('Failed to fetch comments:', error)
    ElMessage.error(error.message || '获取评论列表失败')
  } finally {
    loading.value = false
  }
}

const validateComment = (content: string): { valid: boolean; message?: string } => {
  // 验证评论内容不为空
  if (!content || content.trim().length === 0) {
    return { valid: false, message: '请输入评论内容' }
  }

  // 验证评论内容不能只包含空白字符
  if (content.trim().length === 0) {
    return { valid: false, message: '评论内容不能为空白字符' }
  }

  // 验证评论长度（最大1000字符）
  if (content.length > 1000) {
    return { valid: false, message: '评论内容不能超过1000个字符' }
  }

  // 验证评论最小长度（至少1个字符）
  if (content.trim().length < 1) {
    return { valid: false, message: '评论内容至少需要1个字符' }
  }

  return { valid: true }
}

const handleSubmit = async () => {
  const content = newComment.value.trim()
  
  // 验证评论内容
  const validation = validateComment(content)
  if (!validation.valid) {
    ElMessage.warning(validation.message || '评论内容验证失败')
    return
  }

  if (!authStore.user?.id) {
    ElMessage.error('请先登录')
    return
  }

  submitting.value = true
  try {
    const response = await request.post<TaskComment>(
      `/tasks/${props.taskId}/comments`,
      {
        userId: authStore.user.id,
        content
      }
    )
    
    if (response) {
      comments.value.push(response)
      newComment.value = ''
      ElMessage.success('评论发送成功')
      emit('update')
      
      // 滚动到底部
      await nextTick()
      scrollToBottom()
    }
  } catch (error: any) {
    console.error('Failed to add comment:', error)
    ElMessage.error(error.message || '发送评论失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (comment: TaskComment) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    if (!authStore.user?.id) {
      ElMessage.error('请先登录')
      return
    }

    deletingId.value = comment.id
    try {
      await request.delete(
        `/tasks/${props.taskId}/comments/${comment.id}`,
        {
          params: { userId: authStore.user.id }
        }
      )
      
      comments.value = comments.value.filter(c => c.id !== comment.id)
      ElMessage.success('删除评论成功')
      emit('update')
    } catch (error: any) {
      console.error('Failed to delete comment:', error)
      ElMessage.error(error.message || '删除评论失败')
    } finally {
      deletingId.value = null
    }
  } catch {
    // User cancelled
  }
}

const canDeleteComment = (comment: TaskComment): boolean => {
  return authStore.user?.id === comment.userId
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

const scrollToBottom = () => {
  if (commentsListRef.value) {
    commentsListRef.value.scrollTop = commentsListRef.value.scrollHeight
  }
}

// Lifecycle
onMounted(() => {
  fetchComments()
})

// Expose methods for parent component
defineExpose({
  refresh: fetchComments
})
</script>

<style scoped lang="scss">
.task-comments {
  display: flex;
  flex-direction: column;
  height: 100%;

  .comments-header {
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
      color: var(--text-color);
    }
  }

  .comments-list {
    flex: 1;
    overflow-y: auto;
    margin-bottom: 16px;
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

    .comment-item {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
      position: relative;
      padding: 12px;
      border-radius: 8px;
      transition: background-color 0.3s;

      &:hover {
        background-color: var(--bg-card-hover);

        .delete-btn {
          opacity: 1;
        }
      }

      .comment-avatar {
        flex-shrink: 0;
      }

      .comment-content {
        flex: 1;
        min-width: 0;

        .comment-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 8px;

          .comment-author {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-color);
          }

          .comment-time {
            font-size: 12px;
            color: var(--text-color-muted);
          }
        }

        .comment-text {
          font-size: 14px;
          color: var(--text-color-secondary);
          line-height: 1.6;
          word-wrap: break-word;
          white-space: pre-wrap;
        }
      }

      .delete-btn {
        flex-shrink: 0;
        opacity: 0;
        transition: opacity 0.3s;
      }
    }
  }

  .comment-input-wrapper {
    border-top: 1px solid var(--border-card);
    padding-top: 16px;

    .input-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 12px;

      .input-hint {
        font-size: 12px;
        color: var(--text-color-muted);
      }
    }
  }
}
</style>
