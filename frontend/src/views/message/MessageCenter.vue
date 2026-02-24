<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getConversations, 
  getConversationMessages, 
  sendMessage, 
  markMessagesAsRead,
  pinConversation,
  muteConversation,
  deleteConversation,
  type Conversation,
  type Message
} from '@/api/message'
import { uploadMessageImage, uploadMessageFile } from '@/api/upload'
import { useAuthStore } from '@/store/auth'
import { websocketService } from '@/utils/websocket'
import { Search, MoreFilled, Picture, Document, Close } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const conversations = ref<Conversation[]>([])
const currentConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const messageInput = ref('')
const loading = ref(false)
const messagesContainer = ref<HTMLElement>()
const searchQuery = ref('')
const imagePreviewUrl = ref<string | null>(null)
const uploadingImage = ref(false)
const uploadingFile = ref(false)
const uploadProgress = ref(0)

const filteredConversations = computed(() => {
  if (!searchQuery.value) return conversations.value
  return conversations.value.filter(c => 
    c.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const loadConversations = async () => {
  try {
    const res: any = await getConversations()
    if (res && res.data) {
      conversations.value = res.data
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载会话列表失败')
  }
}

const selectConversation = async (conversation: Conversation) => {
  currentConversation.value = conversation
  await loadMessages()
  
  // 标记为已读
  if (conversation.lastMessage && conversation.unreadCount > 0) {
    try {
      await markMessagesAsRead(conversation.id, conversation.lastMessage.id)
      conversation.unreadCount = 0
    } catch (error) {
      console.error('标记已读失败', error)
    }
  }
}

const loadMessages = async () => {
  if (!currentConversation.value) return
  
  loading.value = true
  try {
    const res = await getConversationMessages(currentConversation.value.id)
    if (res && res.data && res.data.records) {
      messages.value = res.data.records.reverse() // 倒序显示
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const handleSendMessage = async () => {
  if (!messageInput.value.trim() || !currentConversation.value) return
  
  try {
    const res = await sendMessage(currentConversation.value.id, {
      messageType: 'TEXT',
      content: messageInput.value.trim()
    })
    
    if (res && res.data) {
      messages.value.push(res.data)
      messageInput.value = ''
      await nextTick()
      scrollToBottom()
      
      // 更新会话列表
      await loadConversations()
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('发送失败')
  }
}

const handleImageUpload = async (file: File) => {
  if (!currentConversation.value) {
    ElMessage.warning('请先选择一个会话')
    return false
  }

  // 验证图片类型
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('不支持的图片格式，请上传 JPG、PNG、GIF 或 WebP 格式')
    return false
  }

  // 验证文件大小（10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }

  uploadingImage.value = true
  uploadProgress.value = 0

  try {
    // 如果是大图（>1MB），先压缩预览
    let fileToUpload = file
    if (file.size > 1024 * 1024) {
      // 简单的压缩：使用canvas压缩
      fileToUpload = await compressImage(file)
    }

    // 预览图片
    const reader = new FileReader()
    reader.onload = (e) => {
      imagePreviewUrl.value = e.target?.result as string
    }
    reader.readAsDataURL(fileToUpload)

    // 上传图片
    const res: any = await uploadMessageImage(fileToUpload, (percentage) => {
      uploadProgress.value = percentage
    })
    if (res && res.data) {
      // 发送图片消息
      const messageRes: any = await sendMessage(currentConversation.value.id, {
        messageType: 'IMAGE',
        content: '[图片]',
        fileUrl: res.data.url,
        fileName: res.data.fileName,
        fileSize: res.data.fileSize
      })

      if (messageRes && messageRes.data) {
        messages.value.push(messageRes.data)
        await nextTick()
        scrollToBottom()
        await loadConversations()
        imagePreviewUrl.value = null
        ElMessage.success('图片发送成功')
      }
    }
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '图片上传失败')
    imagePreviewUrl.value = null
  } finally {
    uploadingImage.value = false
    uploadProgress.value = 0
  }

  return false // 阻止默认上传行为
}

const handleFileUpload = async (file: File) => {
  if (!currentConversation.value) {
    ElMessage.warning('请先选择一个会话')
    return false
  }

  // 验证文件大小（10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }

  uploadingFile.value = true
  uploadProgress.value = 0

  try {
    // 上传文件
    const res: any = await uploadMessageFile(file, (percentage) => {
      uploadProgress.value = percentage
    })
    if (res && res.data) {
      // 发送文件消息
      const messageRes: any = await sendMessage(currentConversation.value.id, {
        messageType: 'FILE',
        content: `[文件] ${res.data.fileName}`,
        fileUrl: res.data.url,
        fileName: res.data.fileName,
        fileSize: res.data.fileSize
      })

      if (messageRes && messageRes.data) {
        messages.value.push(messageRes.data)
        await nextTick()
        scrollToBottom()
        await loadConversations()
        ElMessage.success('文件发送成功')
      }
    }
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '文件上传失败')
  } finally {
    uploadingFile.value = false
    uploadProgress.value = 0
  }

  return false // 阻止默认上传行为
}

const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('无法创建canvas上下文'))
          return
        }

        // 计算压缩后的尺寸（最大宽度或高度为1920px）
        let width = img.width
        let height = img.height
        const maxDimension = 1920
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = (height * maxDimension) / width
            width = maxDimension
          } else {
            width = (width * maxDimension) / height
            height = maxDimension
          }
        }

        canvas.width = width
        canvas.height = height
        ctx.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (blob) {
              const compressedFile = new File([blob], file.name, {
                type: file.type,
                lastModified: Date.now()
              })
              resolve(compressedFile)
            } else {
              reject(new Error('图片压缩失败'))
            }
          },
          file.type,
          0.8 // 质量0.8
        )
      }
      img.onerror = () => reject(new Error('图片加载失败'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
  })
}

const cancelImagePreview = () => {
  imagePreviewUrl.value = null
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getFileIcon = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const iconMap: Record<string, string> = {
    pdf: '📄',
    doc: '📝',
    docx: '📝',
    xls: '📊',
    xlsx: '📊',
    ppt: '📊',
    pptx: '📊',
    zip: '📦',
    rar: '📦',
    txt: '📄',
    json: '📄',
    xml: '📄'
  }
  return iconMap[ext || ''] || '📎'
}

const handlePinConversation = async (conversation: Conversation) => {
  try {
    await pinConversation(conversation.id, !conversation.isPinned)
    conversation.isPinned = !conversation.isPinned
    ElMessage.success(conversation.isPinned ? '已置顶' : '已取消置顶')
    await loadConversations()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleMuteConversation = async (conversation: Conversation) => {
  try {
    await muteConversation(conversation.id, !conversation.isMuted)
    conversation.isMuted = !conversation.isMuted
    ElMessage.success(conversation.isMuted ? '已开启免打扰' : '已关闭免打扰')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDeleteConversation = async (conversation: Conversation) => {
  try {
    await deleteConversation(conversation.id)
    conversations.value = conversations.value.filter(c => c.id !== conversation.id)
    if (currentConversation.value?.id === conversation.id) {
      currentConversation.value = null
      messages.value = []
    }
    ElMessage.success('已删除')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000)
    return `${days}天前`
  }
  return date.toLocaleDateString('zh-CN')
}

const isMyMessage = (message: Message) => {
  return message.senderId === authStore.user?.id
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = 'https://via.placeholder.com/300x200?text=图片加载失败'
}

const downloadFile = (url: string, fileName?: string) => {
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || 'download'
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 处理 WebSocket 新消息
const handleNewMessage = (message: Message) => {
  // 如果是当前会话的消息，添加到消息列表
  if (currentConversation.value && message.conversationId === currentConversation.value.id) {
    messages.value.push(message)
    nextTick(() => {
      scrollToBottom()
    })
    
    // 标记为已读
    markMessagesAsRead(message.conversationId, message.id)
  }
  
  // 刷新会话列表
  loadConversations()
}

onMounted(() => {
  loadConversations()
  
  // 监听 WebSocket 新消息
  websocketService.on('new_message', handleNewMessage)
})

onUnmounted(() => {
  // 移除 WebSocket 监听
  websocketService.off('new_message', handleNewMessage)
})

// 图片预览对话框
const showImagePreview = (url: string) => {
  // 使用新窗口打开图片
  window.open(url, '_blank')
}
</script>

<template>
  <div class="message-center">
    <!-- 左侧会话列表 -->
    <div class="conversation-list">
      <div class="list-header">
        <h3>消息</h3>
        <el-input
          v-model="searchQuery"
          placeholder="搜索会话"
          :prefix-icon="Search"
          size="small"
          clearable
        />
      </div>

      <div class="conversations">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ 
            active: currentConversation?.id === conversation.id,
            pinned: conversation.isPinned
          }"
          @click="selectConversation(conversation)"
        >
          <div class="conversation-avatar">
            <el-avatar :size="48" :src="conversation.avatar">
              {{ (conversation.name || '对话').charAt(0) }}
            </el-avatar>
            <el-badge v-if="conversation.unreadCount > 0" :value="conversation.unreadCount" :max="99" class="unread-badge" />
          </div>

          <div class="conversation-content">
            <div class="conversation-header">
              <span class="conversation-name">{{ conversation.name }}</span>
              <span class="conversation-time">{{ formatTime(conversation.lastMessageTime) }}</span>
            </div>
            <div class="last-message">
              {{ conversation.lastMessage?.content || '暂无消息' }}
            </div>
          </div>

          <el-dropdown trigger="click" @click.stop>
            <el-icon class="more-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handlePinConversation(conversation)">
                  {{ conversation.isPinned ? '取消置顶' : '置顶' }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleMuteConversation(conversation)">
                  {{ conversation.isMuted ? '取消免打扰' : '免打扰' }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleDeleteConversation(conversation)">
                  删除会话
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div v-if="filteredConversations.length === 0" class="empty-conversations">
          <p>暂无会话</p>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <template v-if="currentConversation">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-info">
            <el-avatar :size="40" :src="currentConversation.avatar">
              {{ (currentConversation.name || '对话').charAt(0) }}
            </el-avatar>
            <span class="chat-name">{{ currentConversation.name }}</span>
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="messagesContainer" v-loading="loading" class="messages-container">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="{ 'my-message': isMyMessage(message) }"
          >
            <el-avatar v-if="!isMyMessage(message)" :size="36" :src="message.senderAvatar">
              {{ (message.senderName || '用户').charAt(0) }}
            </el-avatar>
            <div class="message-content">
              <div v-if="!isMyMessage(message)" class="message-sender">{{ message.senderName }}</div>
              <div class="message-bubble">
                <template v-if="message.isRecalled">
                  <span class="recalled-text">消息已撤回</span>
                </template>
                <template v-else-if="message.messageType === 'IMAGE' && message.fileUrl">
                  <div class="message-image">
                    <img 
                      :src="message.fileUrl" 
                      :alt="message.content"
                      @click="showImagePreview(message.fileUrl!)"
                      @error="handleImageError"
                    />
                  </div>
                </template>
                <template v-else-if="message.messageType === 'FILE' && message.fileUrl">
                  <div class="message-file">
                    <div class="file-icon">{{ getFileIcon(message.fileName || '') }}</div>
                    <div class="file-info">
                      <div class="file-name">{{ message.fileName }}</div>
                      <div class="file-size" v-if="message.fileSize">
                        {{ formatFileSize(message.fileSize) }}
                      </div>
                    </div>
                    <el-button 
                      type="primary" 
                      size="small" 
                      link
                      @click="downloadFile(message.fileUrl, message.fileName)"
                    >
                      下载
                    </el-button>
                  </div>
                </template>
                <template v-else>
                  {{ message.content }}
                </template>
              </div>
              <div class="message-time">{{ formatTime(message.createdAt) }}</div>
            </div>
            <el-avatar v-if="isMyMessage(message)" :size="36" :src="authStore.user?.profile?.avatarUrl">
              {{ (authStore.user?.profile?.realName || authStore.user?.username || '我').charAt(0) }}
            </el-avatar>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="input-area">
          <div class="input-toolbar">
            <el-upload
              :action="''"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              :before-upload="handleImageUpload"
            >
              <el-button 
                :icon="Picture" 
                circle 
                text 
                title="发送图片"
                :loading="uploadingImage"
              />
            </el-upload>
            <el-upload
              :action="''"
              :auto-upload="false"
              :show-file-list="false"
              :before-upload="handleFileUpload"
            >
              <el-button 
                :icon="Document" 
                circle 
                text 
                title="发送文件"
                :loading="uploadingFile"
              />
            </el-upload>
            <div v-if="uploadingImage || uploadingFile" class="upload-progress">
              <el-progress 
                :percentage="uploadProgress" 
                :status="uploadProgress === 100 ? 'success' : undefined"
                :stroke-width="4"
              />
            </div>
          </div>
          
          <!-- 图片预览 -->
          <div v-if="imagePreviewUrl" class="image-preview-container">
            <div class="image-preview">
              <img :src="imagePreviewUrl" alt="预览" />
              <el-button
                :icon="Close"
                circle
                class="close-btn"
                @click="cancelImagePreview"
              />
            </div>
          </div>
          
          <div class="input-container">
            <el-input
              v-model="messageInput"
              type="textarea"
              :rows="3"
              placeholder="输入消息..."
              @keydown.enter.ctrl="handleSendMessage"
            />
            <el-button 
              type="primary" 
              @click="handleSendMessage"
              :disabled="uploadingImage || uploadingFile"
            >
              发送 (Ctrl+Enter)
            </el-button>
          </div>
        </div>
      </template>

      <!-- 未选择会话 -->
      <div v-else class="no-conversation">
        <div class="empty-icon">💬</div>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.message-center {
  display: flex;
  height: calc(100vh - 64px);
  background: var(--bg-color);
}

.conversation-list {
  width: 320px;
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
}

.list-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-subtle);
  
  h3 {
    margin: 0 0 12px 0;
    font-size: 22px;
    font-weight: 800;
    background: linear-gradient(135deg, var(--text-color) 0%, var(--text-color-muted) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.conversations {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s;
  position: relative;
  
  &:hover {
    background: var(--el-fill-color-light);
    
    .more-icon {
      opacity: 1;
    }
  }
  
  &.active {
    background: var(--el-color-primary-light-9);
  }
  
  &.pinned {
    background: var(--el-fill-color-lighter);
  }
}

.conversation-avatar {
  position: relative;
  flex-shrink: 0;
  
  .unread-badge {
    position: absolute;
    top: -5px;
    right: -5px;
  }
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conversation-name {
  font-weight: 600;
  font-size: 14px;
}

.conversation-time {
  font-size: 12px;
  color: var(--text-color-muted);
}

.last-message {
  font-size: 13px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-icon {
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.empty-conversations {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-color-muted);
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--card-bg);
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-name {
  font-size: 16px;
  font-weight: 600;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  
  &.my-message {
    flex-direction: row-reverse;
    
    .message-content {
      align-items: flex-end;
    }
    
    .message-bubble {
      background: var(--el-color-primary);
      color: white;
    }
  }
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 60%;
}

.message-sender {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.message-bubble {
  padding: 10px 14px;
  background: var(--card-bg);
  border-radius: 12px;
  word-wrap: break-word;
  box-shadow: var(--shadow-sm);
  
  .message-image {
    margin: -10px -14px;
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    
    img {
      max-width: 300px;
      max-height: 400px;
      display: block;
      transition: transform 0.2s;
      
      &:hover {
        transform: scale(1.02);
      }
    }
  }
  
  .message-file {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px;
    background: var(--el-fill-color-lighter);
    border-radius: 8px;
    
    .file-icon {
      font-size: 24px;
      flex-shrink: 0;
    }
    
    .file-info {
      flex: 1;
      min-width: 0;
      
      .file-name {
        font-weight: 500;
        font-size: 14px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .file-size {
        font-size: 12px;
        color: var(--text-color-muted);
        margin-top: 4px;
      }
    }
  }
}

.recalled-text {
  color: var(--text-color-muted);
  font-style: italic;
}

.message-time {
  font-size: 11px;
  color: var(--text-color-muted);
}

.input-area {
  border-top: 1px solid var(--border-subtle);
  background: var(--card-bg);
}

.input-toolbar {
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-progress {
  flex: 1;
  margin-left: 8px;
}

.image-preview-container {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--el-fill-color-lighter);
}

.image-preview {
  position: relative;
  display: inline-block;
  max-width: 300px;
  
  img {
    max-width: 100%;
    max-height: 200px;
    border-radius: 8px;
    display: block;
  }
  
  .close-btn {
    position: absolute;
    top: -8px;
    right: -8px;
    background: var(--el-color-danger);
    color: white;
    border: none;
    
    &:hover {
      background: var(--el-color-danger-light-3);
    }
  }
}

.input-container {
  padding: 16px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  
  :deep(.el-textarea) {
    flex: 1;
  }
}

.no-conversation {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-color-muted);
  
  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.5;
  }
  
  p {
    font-size: 16px;
  }
}
</style>

<style>
.image-preview-dialog {
  .el-message-box__content {
    padding: 0;
  }
}
</style>
