import { ref, onMounted, onUnmounted } from 'vue'
import { io, Socket } from 'socket.io-client'
import { useAuthStore } from '../store/auth'

interface Message {
  id?: string
  teamId: number
  senderId: number
  senderName: string
  senderAvatar?: string
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'
  content: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
  mentionedUsers?: number[]
  createdAt: Date
}

export function useSocket() {
  const authStore = useAuthStore()
  const socket = ref<Socket | null>(null)
  const isConnected = ref(false)
  const messages = ref<Message[]>([])
  const onlineUsers = ref<number[]>([])
  const typingUsers = ref<Set<number>>(new Set())

  const connect = () => {
    if (socket.value) return

    const socketUrl = import.meta.env.VITE_SOCKET_URL || `${window.location.protocol}//${window.location.hostname}:9092`

    socket.value = io(socketUrl, {
      query: { userId: authStore.user?.id?.toString() || '' },
      transports: ['polling', 'websocket'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: 10,
      timeout: 20000,
      withCredentials: false,
      upgrade: true
    })

    // 连接成功
    socket.value.on('connect', () => {
      console.log('Socket.io连接成功')
      isConnected.value = true

      // 发送用户信息
      if (authStore.user) {
        socket.value?.emit('user:join', {
          userId: authStore.user.id,
          username: authStore.user.username,
          avatar: authStore.user.profile?.avatarUrl || '',
        })
      }
    })

    // 连接断开
    socket.value.on('disconnect', () => {
      console.log('Socket.io连接断开')
      isConnected.value = false
    })

    // 接收历史消息（按时间倒序排列，需要反转）
    socket.value.on('message:history', (history: Message[]) => {
      console.log('收到历史消息:', history.length, '条')
      // 后端返回的是倒序（最新的在前），需要反转为正序（最旧的在前）
      messages.value = [...history].reverse()
    })

    // 接收新消息
    socket.value.on('message:new', (message: Message) => {
      console.log('收到新消息:', message)
      messages.value.push(message)
    })

    // @提醒
    socket.value.on('message:mention', (data: any) => {
      console.log('收到@提醒:', data)
      // TODO: 显示通知
    })

    // 用户上线/在线人数更新
    socket.value.on('user:online', (data: { userId?: number; username?: string; onlineCount?: number }) => {
      if (data.onlineCount !== undefined) {
        // 更新在线人数
        onlineUsers.value = Array(data.onlineCount).fill(0).map((_, i) => i + 1)
      } else if (data.userId) {
        onlineUsers.value.push(data.userId)
      }
    })

    // 用户离线
    socket.value.on('user:offline', (data: { userId: number }) => {
      const index = onlineUsers.value.indexOf(data.userId)
      if (index > -1) {
        onlineUsers.value.splice(index, 1)
      }
    })

    // 正在输入
    socket.value.on('user:typing', (data: { userId: number; username: string }) => {
      typingUsers.value.add(data.userId)
    })

    // 停止输入
    socket.value.on('user:stop-typing', (data: { userId: number }) => {
      typingUsers.value.delete(data.userId)
    })
  }

  const disconnect = () => {
    if (socket.value) {
      socket.value.disconnect()
      socket.value = null
      isConnected.value = false
    }
  }

  const joinTeam = (teamId: number) => {
    if (!socket.value) return
    console.log('加入团队房间:', teamId)
    // 清空当前消息，等待接收历史消息
    messages.value = []
    socket.value.emit('join_team', teamId)
  }

  const leaveTeam = (teamId: number) => {
    if (!socket.value) return
    console.log('离开团队房间:', teamId)
    socket.value.emit('leave_team', teamId)
  }

  const sendMessage = (teamId: number, content: string, mentionedUsers: number[] = []) => {
    if (!socket.value || !authStore.user) return
    
    const message = {
      teamId,
      senderId: authStore.user.id,
      senderName: authStore.user.profile?.realName || authStore.user.username,
      senderAvatar: authStore.user.profile?.avatarUrl || '',
      content,
      messageType: 'TEXT',
      mentionedUsers: mentionedUsers.length > 0 ? JSON.stringify(mentionedUsers) : undefined,
    }
    
    console.log('发送消息:', message)
    socket.value.emit('send_message', message)
  }

  const sendTyping = (teamId: number) => {
    if (!socket.value) return
    socket.value.emit('typing_start', { teamId })
  }

  const stopTyping = (teamId: number) => {
    if (!socket.value) return
    socket.value.emit('typing_stop', { teamId })
  }

  const markAsRead = (messageId: string) => {
    if (!socket.value) return
    socket.value.emit('message:read', { messageId })
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    socket,
    isConnected,
    messages,
    onlineUsers,
    typingUsers,
    connect,
    disconnect,
    joinTeam,
    leaveTeam,
    sendMessage,
    sendTyping,
    stopTyping,
    markAsRead,
  }
}

