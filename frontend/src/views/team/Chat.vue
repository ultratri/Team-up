<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UserFilled,
  Document,
  Sunny,
  Picture,
  Paperclip,
  Position
} from '@element-plus/icons-vue'
import { useSocket } from '@/composables/useSocket'
import { useAuthStore } from '@/store/auth'
import GlassCard from '@/components/common/GlassCard.vue'

const props = defineProps<{
  teamId: number
}>()

const authStore = useAuthStore()
const { messages, onlineUsers, typingUsers, isConnected, joinTeam, sendMessage, sendTyping, stopTyping } =
  useSocket()

const messageListRef = ref<HTMLDivElement>()
const inputMessage = ref('')
const sending = ref(false)
let typingTimer: ReturnType<typeof setTimeout> | null = null

const currentUserId = computed(() => authStore.user?.id || 0)

const isMe = (senderId: number | string) => {
  return Number(senderId) === Number(currentUserId.value)
}

const formatTime = (date: Date | string) => {
  const d = new Date(date)
  const now = new Date()
  const diff = now.getTime() - d.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const handleSend = () => {
  if (!inputMessage.value.trim()) return
  if (!isConnected.value) {
    ElMessage.warning('连接已断开，请刷新页面')
    return
  }

  sending.value = true
  try {
    sendMessage(props.teamId, inputMessage.value)
    inputMessage.value = ''
    stopTyping(props.teamId)
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

const handleTyping = () => {
  if (typingTimer) clearTimeout(typingTimer)
  sendTyping(props.teamId)
  typingTimer = setTimeout(() => stopTyping(props.teamId), 3000)
}

onMounted(() => {
  joinTeam(props.teamId)
  scrollToBottom()
})

onUnmounted(() => {
  if (typingTimer) clearTimeout(typingTimer)
})
</script>

<template>
  <GlassCard class="chat-card" no-padding>
    <div class="chat-header">
      <div class="header-info">
        <h3>团队讨论</h3>
        <div class="online-status">
          <span class="status-dot"></span>
          <span>{{ onlineUsers.length }} 人在线</span>
        </div>
      </div>
    </div>

    <!-- Messages -->
    <div class="message-list" ref="messageListRef">
      <div
        v-for="(message, index) in messages"
        :key="index"
        :class="['message-item', { 'is-me': isMe(message.senderId) }]"
      >
        <div v-if="!isMe(message.senderId)" class="avatar">
          <el-avatar :size="36" :src="message.senderAvatar">{{ message.senderName?.charAt(0) }}</el-avatar>
        </div>

        <div class="message-content">
          <div class="sender-name" v-if="!isMe(message.senderId)">
            {{ message.senderName }}
          </div>
          <div class="message-bubble">
            <div v-if="message.messageType === 'TEXT'" class="text-message">
              {{ message.content }}
            </div>
            <div v-else-if="message.messageType === 'IMAGE'" class="image-message">
              <el-image :src="message.fileUrl" fit="cover" preview-teleported />
            </div>
            <div v-else-if="message.messageType === 'FILE'" class="file-message">
              <el-icon><Document /></el-icon>
              <span>{{ message.fileName }}</span>
            </div>
          </div>
          <div class="message-time">
            {{ formatTime(message.createdAt) }}
          </div>
        </div>

        <div v-if="isMe(message.senderId)" class="avatar">
          <el-avatar :size="36" :src="message.senderAvatar">{{ message.senderName?.charAt(0) }}</el-avatar>
        </div>
      </div>

      <div v-if="typingUsers.size > 0" class="typing-indicator">
        <div class="dots">
          <span></span><span></span><span></span>
        </div>
        <span>有人正在输入...</span>
      </div>
    </div>

    <!-- Input Area -->
    <div class="input-area">
      <div class="toolbar">
        <button class="tool-btn"><el-icon><Sunny /></el-icon></button>
        <button class="tool-btn"><el-icon><Picture /></el-icon></button>
        <button class="tool-btn"><el-icon><Paperclip /></el-icon></button>
      </div>
      
      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          rows="1"
          placeholder="发送消息..."
          @keydown.ctrl.enter="handleSend"
          @input="handleTyping"
          class="chat-input"
        ></textarea>
        <button class="send-btn" @click="handleSend" :disabled="!inputMessage.trim() || sending">
          <el-icon><Position /></el-icon>
        </button>
      </div>
    </div>
  </GlassCard>
</template>

<style scoped lang="scss">
.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-subtle);
  
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--text-color);
  }
  
  .online-status {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--text-color-muted);
    margin-top: 4px;
    
    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #10b981;
    }
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: rgba(0,0,0,0.01);
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 80%;
  
  &.is-me {
    align-self: flex-end;
    flex-direction: row-reverse;
    
    .message-content {
      align-items: flex-end;
      
      .message-bubble {
        background: var(--accent-color);
        color: white;
        border-bottom-right-radius: 4px;
        border-bottom-left-radius: 20px;
      }
      
      .message-time {
        text-align: right;
      }
    }
  }
  
  &:not(.is-me) {
    .message-bubble {
      background: var(--bg-elevated);
      color: var(--text-color);
      border-bottom-left-radius: 4px;
      border-bottom-right-radius: 20px;
      box-shadow: var(--shadow-card);
    }
  }
}

.message-content {
  display: flex;
  flex-direction: column;
}

.sender-name {
  font-size: 12px;
  color: var(--text-color-muted);
  margin-bottom: 4px;
  margin-left: 4px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 20px;
  border-top-left-radius: 20px;
  border-top-right-radius: 20px;
  font-size: 15px;
  line-height: 1.5;
  word-wrap: break-word;
  position: relative;
}

.message-time {
  font-size: 11px;
  color: var(--text-color-muted);
  margin-top: 4px;
  margin-left: 4px;
  opacity: 0.8;
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--border-subtle);
  background: rgba(255,255,255,0.02);
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  
  .tool-btn {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: none;
    background: transparent;
    color: var(--text-color-muted);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    
    &:hover {
      background: rgba(0,0,0,0.05);
      color: var(--text-color);
    }
  }
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: var(--bg-elevated);
  padding: 8px;
  border-radius: 24px;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  
  &:focus-within {
    border-color: var(--accent-color);
    box-shadow: 0 0 0 2px var(--accent-soft);
  }
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 12px;
  font-size: 15px;
  color: var(--text-color);
  outline: none;
  resize: none;
  max-height: 100px;
  font-family: inherit;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--accent-color);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  &:not(:disabled):hover {
    transform: scale(1.05);
  }
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-color-muted);
  padding-left: 20px;
  
  .dots {
    display: flex;
    gap: 2px;
    span {
      width: 4px;
      height: 4px;
      background: var(--text-color-muted);
      border-radius: 50%;
      animation: bounce 1.4s infinite ease-in-out both;
      
      &:nth-child(1) { animation-delay: -0.32s; }
      &:nth-child(2) { animation-delay: -0.16s; }
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
