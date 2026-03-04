<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UserFilled,
  Document,
  Sunny,
  Picture,
  Paperclip,
  Position,
  Setting
} from '@element-plus/icons-vue'
import { useSocket } from '@/composables/useSocket'
import { useAuthStore } from '@/store/auth'
import { useTeamStore } from '@/store/team'
import GlassCard from '@/components/common/GlassCard.vue'

const props = defineProps<{
  teamId: number
}>()

const authStore = useAuthStore()
const teamStore = useTeamStore()
const { messages, onlineUsers, typingUsers, isConnected, joinTeam, leaveTeam, sendMessage, sendTyping, stopTyping } =
  useSocket()

const messageListRef = ref<HTMLDivElement>()
const inputRef = ref<HTMLTextAreaElement>()
const inputMessage = ref('')
const sending = ref(false)
let typingTimer: ReturnType<typeof setTimeout> | null = null

// 通知设置（本地、按用户保存）
type ChatNotificationSettings = {
  desktopEnabled: boolean
  onlyMention: boolean
  soundEnabled: boolean
}

const notificationSettings = ref<ChatNotificationSettings>({
  desktopEnabled: true,
  onlyMention: true,
  soundEnabled: true
})

const settingsStorageKey = computed(() => {
  const userId = authStore.user?.id || 'anonymous'
  return `teamChatNotifySettings_${userId}`
})

const currentUserId = computed(() => authStore.user?.id || 0)
const teamMembers = computed(() => teamStore.currentTeamMembers || [])

// @ 成员搜索
const mentionSearch = ref('')
const filteredMembers = computed(() => {
  const list = teamMembers.value || []
  const q = mentionSearch.value.trim().toLowerCase()
  if (!q) return list
  return list.filter((m: any) => {
    const name =
      m.nickname ||
      m.username ||
      (m as any).realName ||
      ''
    return name.toLowerCase().includes(q)
  })
})

// 历史消息搜索
const searchQuery = ref('')
const hasSearch = computed(() => searchQuery.value.trim().length > 0)

// 消息可见区域（简单分页：只显示最近 N 条，可手动“加载更多”）
const pageSize = 50
const visibleCount = ref(pageSize)
const visibleMessages = computed(() => {
  const all = messages.value || []
  if (all.length <= visibleCount.value) return all
  return all.slice(all.length - visibleCount.value)
})

// 本地已读逻辑：记录当前已看到的最后一条消息索引
const lastSeenIndex = ref(-1)

const markAllAsRead = () => {
  const total = messages.value?.length || 0
  if (total === 0) return
  lastSeenIndex.value = total - 1
}

// 搜索结果：在所有消息中按内容模糊匹配
const searchedMessages = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return []
  return (messages.value || []).filter((m: any) =>
    String(m.content || '').toLowerCase().includes(q)
  )
})

// 最终展示的消息列表：搜索时优先显示匹配结果，否则显示分页列表
const displayedMessages = computed(() => {
  return hasSearch.value ? searchedMessages.value : visibleMessages.value
})

const isMe = (senderId: number | string) => {
  return Number(senderId) === Number(currentUserId.value)
}

const isMentionedMe = (content: string | undefined | null) => {
  if (!content) return false
  const user = authStore.user as any
  if (!user) return false

  const username = user.username || ''
  const realName = user.profile?.realName || ''
  const nickname = user.profile?.nickname || ''
  const displayName = user.profile?.displayName || ''

  const candidates = [
    username && `@${username}`,
    realName && `@${realName}`,
    nickname && `@${nickname}`,
    displayName && `@${displayName}`,
  ].filter(Boolean) as string[]

  return candidates.some(token => content.includes(token))
}

// 高亮搜索匹配内容
const highlightContent = (message: any) => {
  const text = String(message.content || '')
  const q = searchQuery.value.trim()
  if (!q) return text

  const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const re = new RegExp(`(${escaped})`, 'gi')
  return text.replace(re, '<mark class="chat-highlight">$1</mark>')
}

const formatTime = (date: Date | string | any) => {
  // 处理后端返回的 LocalDateTime 数组格式 [year, month, day, hour, minute, second]
  let d: Date
  if (Array.isArray(date)) {
    // LocalDateTime 数组格式: [2026, 3, 3, 15, 40, 30]
    d = new Date(date[0], date[1] - 1, date[2], date[3], date[4], date[5] || 0)
  } else {
    d = new Date(date)
  }
  
  // 检查日期是否有效
  if (isNaN(d.getTime())) {
    return '刚刚'
  }
  
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
    // 滚动到底部时，将当前所有消息标记为已读
    markAllAsRead()
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

// 根据滚动位置更新已读（当接近底部时标记为已读）
const handleScroll = () => {
  const el = messageListRef.value
  if (!el) return
  const threshold = 24
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - threshold) {
    markAllAsRead()
  }
}

// 在当前光标位置插入文本
const insertAtCursor = (text: string) => {
  const el = inputRef.value
  if (!el) {
    inputMessage.value += text
    return
  }

  const start = el.selectionStart || 0
  const end = el.selectionEnd || 0
  const value = inputMessage.value
  inputMessage.value = value.slice(0, start) + text + value.slice(end)

  nextTick(() => {
    const pos = start + text.length
    el.focus()
    el.setSelectionRange(pos, pos)
  })
}

// 插入 @ 某个成员
const handleMentionMember = (member: any) => {
  const name = member?.nickname || member?.username || member?.realName || member?.name
  if (!name) return
  const prefix = inputMessage.value.trim().length === 0 ? '' : ' '
  insertAtCursor(`${prefix}@${name} `)
}

// 处理输入框按键：Enter 发送，Shift+Enter 换行，Ctrl/Cmd+Enter 发送
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    if (e.shiftKey) {
      // 保留默认换行
      return
    }
    if (e.ctrlKey || (e.metaKey as boolean)) {
      e.preventDefault()
      handleSend()
      return
    }
    // 单纯 Enter 也发送
    e.preventDefault()
    handleSend()
  }
}

onMounted(() => {
  if (props.teamId) {
    joinTeam(props.teamId)
  }
  scrollToBottom()

  // 读取本地通知设置
  try {
    const raw = window.localStorage.getItem(settingsStorageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      notificationSettings.value = {
        desktopEnabled: parsed.desktopEnabled ?? true,
        onlyMention: parsed.onlyMention ?? true,
        soundEnabled: parsed.soundEnabled ?? true
      }
    }
  } catch {
    // ignore
  }
})

onUnmounted(() => {
  if (typingTimer) clearTimeout(typingTimer)
})

// 当 teamId 变化时，切换加入的团队（用于悬浮窗内切换团队）
watch(
  () => props.teamId,
  (newId, oldId) => {
    if (!newId || newId === oldId) return
    if (oldId) {
      leaveTeam(oldId as number)
    }
    // 切换团队时清空当前消息列表，避免不同团队的消息混在一起
    messages.value = []
    joinTeam(newId)
    scrollToBottom()
  }
)

// 监听新消息，向父级广播事件用于未读数和通知
watch(
  () => messages.value.length,
  (newLen, oldLen) => {
    if (newLen <= oldLen) return
    const message = messages.value[messages.value.length - 1]
    if (!message) return

    const isMine = isMe(message.senderId)
    const mentionedMe = isMentionedMe(message.content)

    window.dispatchEvent(
      new CustomEvent('team-chat:new-message', {
        detail: {
          message,
          isMe: isMine,
          teamId: props.teamId,
          isMentioned: mentionedMe
        }
      })
    )
  }
)

// 持久化通知设置
watch(
  notificationSettings,
  (val) => {
    try {
      window.localStorage.setItem(settingsStorageKey.value, JSON.stringify(val))
    } catch {
      // ignore
    }
  },
  { deep: true }
)
</script>

<template>
  <GlassCard class="chat-card" no-padding>
    <div class="chat-header">
      <div class="header-info">
        <div class="header-title-row">
          <div class="title-block">
            <h3>团队讨论</h3>
            <p class="subtitle">与成员实时协作沟通</p>
          </div>
          <div class="online-status">
            <span class="status-dot"></span>
            <span>{{ onlineUsers.length }} 人在线</span>
          </div>
        </div>
        <div class="search-row">
          <el-input
            v-model="searchQuery"
            size="small"
            clearable
            placeholder="搜索历史消息内容"
            class="chat-search-input"
          />
          <span v-if="hasSearch" class="search-count">
            共 {{ searchedMessages.length }} 条匹配
          </span>
        </div>
      </div>

      <div class="header-actions">
        <el-popover
          placement="bottom-end"
          width="260"
          trigger="click"
        >
          <template #reference>
            <el-button
              circle
              text
              class="settings-btn"
              title="通知设置"
            >
              <el-icon><Setting /></el-icon>
            </el-button>
          </template>

          <div class="notify-settings">
            <div class="notify-row">
              <span>桌面通知</span>
              <el-switch v-model="notificationSettings.desktopEnabled" />
            </div>
            <div class="notify-row">
              <span>仅 @ 我 时提醒</span>
              <el-switch v-model="notificationSettings.onlyMention" />
            </div>
            <div class="notify-row">
              <span>提示音</span>
              <el-switch v-model="notificationSettings.soundEnabled" />
            </div>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- Messages -->
    <div class="message-list" ref="messageListRef" @scroll="handleScroll">
      <div
        v-if="!hasSearch && messages.length > visibleCount"
        class="load-more"
        @click="visibleCount = Math.min(visibleCount + pageSize, messages.length)"
      >
        加载更多历史消息...
      </div>
      <div
        v-for="(message, index) in displayedMessages"
        :key="index"
        :class="['message-item', { 'is-me': isMe(message.senderId) }]"
      >
        <div v-if="!isMe(message.senderId)" class="avatar">
          <el-avatar
            :size="36"
            :src="message.senderAvatar"
          >
            {{ message.senderName?.charAt(0) }}
          </el-avatar>
        </div>

        <div class="message-content">
          <div class="sender-name" v-if="!isMe(message.senderId)">
            {{ message.senderName }}
          </div>
          <div
            class="message-bubble"
            :class="{ 'is-me-bubble': isMe(message.senderId) }"
          >
            <div v-if="message.messageType === 'TEXT'" class="text-message">
              <span v-html="highlightContent(message)"></span>
            </div>
            <div v-else-if="message.messageType === 'IMAGE'" class="image-message">
              <el-image :src="message.fileUrl" fit="cover" preview-teleported />
            </div>
            <div v-else-if="message.messageType === 'FILE'" class="file-message">
              <el-icon><Document /></el-icon>
              <span>{{ message.fileName }}</span>
            </div>
          </div>
          <div class="message-meta">
            <span
              class="message-time"
              :class="{ 'is-me-time': isMe(message.senderId) }"
            >
              {{ formatTime(message.createdAt) }}
            </span>
            <span
              v-if="!isMe(message.senderId)"
              class="message-read-state"
              :class="{ unread: index > lastSeenIndex }"
            >
              {{ index > lastSeenIndex ? '未读' : '已读' }}
            </span>
          </div>
        </div>

        <div v-if="isMe(message.senderId)" class="avatar">
          <el-avatar
            :size="36"
            :src="authStore.user?.profile?.avatarUrl || message.senderAvatar"
          >
            {{ (authStore.user?.profile?.realName || authStore.user?.username || message.senderName || '我')?.charAt(0) }}
          </el-avatar>
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
        <!-- @ 成员：从团队成员列表选择插入 -->
        <el-popover
          placement="top-start"
          width="220"
          trigger="click"
        >
          <template #reference>
            <button class="tool-btn" title="@ 成员">
              <el-icon><UserFilled /></el-icon>
            </button>
          </template>

          <div class="mention-list" v-if="teamMembers.length">
            <el-input
              v-model="mentionSearch"
              size="small"
              placeholder="搜索成员昵称或用户名"
              clearable
              class="mention-search"
            />
            <div
              v-for="member in filteredMembers"
              :key="member.userId"
              class="mention-item"
              @click="handleMentionMember(member)"
            >
              <el-avatar :size="24" :src="member.avatar || member.avatarUrl">
                {{ (member.nickname || member.username || '成').charAt(0) }}
              </el-avatar>
              <div class="mention-info">
                <div class="mention-name">
                  {{ member.nickname || member.username }}
                </div>
                <div class="mention-role" v-if="member.role">
                  {{ member.role }}
                </div>
              </div>
            </div>
          </div>
          <div v-else class="mention-empty">
            暂无团队成员信息
          </div>
        </el-popover>
      </div>
      
      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          ref="inputRef"
          rows="1"
          placeholder="发送消息..."
          @keydown="handleKeyDown"
          @input="handleTyping"
          class="chat-input"
        ></textarea>
        <button class="send-btn" @click="handleSend" :disabled="!inputMessage.trim() || sending">
          <el-icon><Position /></el-icon>
        </button>
      </div>
      <div class="input-hint">
        Enter 发送，Shift+Enter 换行
      </div>
    </div>
  </GlassCard>
</template>

<style scoped lang="scss">
.chat-card {
  height: 100%;
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-subtle);

  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 12px;

  .header-info {
    flex: 1;
  }

  .header-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    .title-block {
      display: flex;
      flex-direction: column;
      gap: 2px;

      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 700;
        color: var(--text-color);
      }

      .subtitle {
        margin: 0;
        font-size: 12px;
        color: var(--text-color-muted);
      }
    }

    .online-status {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: var(--text-color-muted);

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: #10b981;
      }
    }
  }

  .search-row {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 8px;

    .chat-search-input {
      flex: 1;
    }

    .search-count {
      font-size: 12px;
      color: var(--text-color-muted);
      white-space: nowrap;
    }
  }

  .header-actions {
    display: flex;
    align-items: flex-start;

    .settings-btn {
      margin-top: 2px;
      color: var(--text-color-muted);

      &:hover {
        background: var(--el-fill-color-light);
        color: var(--text-color);
      }
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
  align-items: flex-end;
  
  &.is-me {
    align-self: flex-end;
    
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

.message-bubble.is-me-bubble {
  /* 再额外强调一下“我”的消息颜色和阴影 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

.message-time.is-me-time {
  color: var(--accent-color);
  font-weight: 500;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  margin-left: 4px;
}

.message-read-state {
  font-size: 11px;
  color: var(--text-color-muted);
  &.unread {
    color: var(--el-color-primary);
  }
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

.mention-search {
  margin-bottom: 6px;
}

.mention-list {
  max-height: 260px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mention-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.mention-info {
  display: flex;
  flex-direction: column;
  font-size: 12px;

  .mention-name {
    color: var(--text-color);
  }

  .mention-role {
    color: var(--text-color-muted);
    font-size: 11px;
  }
}

.mention-empty {
  font-size: 12px;
  color: var(--text-color-muted);
  padding: 4px 0;
}

.load-more {
  align-self: center;
  font-size: 12px;
  color: var(--text-color-muted);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 12px;
  transition: background 0.2s ease, color 0.2s ease;

  &:hover {
    background: var(--el-fill-color-light);
    color: var(--text-color);
  }
}

.input-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-color-muted);
}

.chat-highlight {
  background: rgba(250, 173, 20, 0.18);
  color: inherit;
  padding: 0 1px;
  border-radius: 2px;
}

@media (max-width: 768px) {
  .chat-header {
    padding: 12px 16px;
  }

  .message-list {
    padding: 12px;
    gap: 16px;
  }

  .message-item {
    max-width: 100%;
  }

  .input-area {
    padding: 12px 16px;
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
