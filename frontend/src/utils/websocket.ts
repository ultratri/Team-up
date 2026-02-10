import { io, Socket } from 'socket.io-client'
import { useAuthStore } from '@/store/auth'
import { ElNotification } from 'element-plus'

class WebSocketService {
  private socket: Socket | null = null
  private messageHandlers: Map<string, Function[]> = new Map()
  private heartbeatInterval: number | null = null
  private heartbeatTimeout: number | null = null
  private lastPongTime: number = Date.now()
  private reconnectAttempts: number = 0
  private maxReconnectAttempts: number = 10

  /**
   * 连接 WebSocket
   */
  connect() {
    const authStore = useAuthStore()
    if (!authStore.user?.id) {
      console.warn('用户未登录，无法连接 WebSocket')
      return
    }

    const userId = authStore.user.id
    const socketUrl = import.meta.env.VITE_SOCKET_URL || `${window.location.protocol}//${window.location.hostname}:9092`
    this.socket = io(socketUrl, {
      query: { userId: userId.toString() },
      // 允许 Socket.IO 自动选择传输方式（polling -> websocket）
      transports: ['polling', 'websocket'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: 10,
      timeout: 20000,
      // 跨域配置
      withCredentials: false,
      // 自动升级到 WebSocket
      upgrade: true
    })

    this.socket.on('connect', () => {
      console.log('WebSocket 连接成功')
      this.reconnectAttempts = 0
      this.lastPongTime = Date.now()
      this.startHeartbeat()
    })

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket 断开连接:', reason)
      this.stopHeartbeat()
      
      // 如果不是主动断开，尝试重连
      if (reason === 'io server disconnect') {
        // 服务器主动断开，需要手动重连
        this.socket?.connect()
      }
    })

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket 连接错误:', error)
      this.reconnectAttempts++
      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        console.error('WebSocket 重连次数已达上限，停止重连')
        ElNotification({
          title: '连接失败',
          message: 'WebSocket连接失败，请刷新页面重试',
          type: 'error',
          duration: 5000
        })
      }
    })

    // 监听pong响应
    this.socket.on('pong', () => {
      this.lastPongTime = Date.now()
      if (this.heartbeatTimeout) {
        clearTimeout(this.heartbeatTimeout)
        this.heartbeatTimeout = null
      }
    })

    // 监听新消息
    this.socket.on('new_message', (message) => {
      console.log('收到新消息:', message)
      this.trigger('new_message', message)
      
      // 显示通知
      ElNotification({
        title: '新消息',
        message: message.content || '收到一条新消息',
        type: 'info',
        duration: 3000
      })
    })

    // 监听新通知
    this.socket.on('new_notification', (notification) => {
      console.log('收到新通知:', notification)
      this.trigger('new_notification', notification)
      
      ElNotification({
        title: notification.title || '新通知',
        message: notification.content,
        type: 'info',
        duration: 4000
      })
    })

    // 监听会话更新
    this.socket.on('conversation_update', (data) => {
      this.trigger('conversation_update', data)
    })

    // 监听正在输入状态
    this.socket.on('typing_status', (data) => {
      this.trigger('typing_status', data)
    })

  }

  /**
   * 启动心跳机制
   */
  private startHeartbeat() {
    this.stopHeartbeat()
    
    // 每25秒发送一次ping
    this.heartbeatInterval = window.setInterval(() => {
      if (this.socket?.connected) {
        this.socket.emit('ping', Date.now())
        
        // 设置超时检测：如果30秒内没有收到pong，认为连接异常
        this.heartbeatTimeout = window.setTimeout(() => {
          const timeSinceLastPong = Date.now() - this.lastPongTime
          if (timeSinceLastPong > 60000) {
            console.warn('WebSocket心跳超时，尝试重连')
            if (this.socket) {
              this.socket.disconnect()
              this.socket.connect()
            }
          }
        }, 30000)
      }
    }, 25000)
  }

  /**
   * 停止心跳机制
   */
  private stopHeartbeat() {
    if (this.heartbeatInterval !== null) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
    if (this.heartbeatTimeout !== null) {
      clearTimeout(this.heartbeatTimeout)
      this.heartbeatTimeout = null
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    if (this.socket) {
      this.socket.disconnect()
      this.socket = null
      this.messageHandlers.clear()
    }
  }

  /**
   * 注册事件处理器
   */
  on(event: string, handler: Function) {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, [])
    }
    this.messageHandlers.get(event)?.push(handler)
  }

  /**
   * 移除事件处理器
   */
  off(event: string, handler?: Function) {
    if (!handler) {
      this.messageHandlers.delete(event)
    } else {
      const handlers = this.messageHandlers.get(event)
      if (handlers) {
        const index = handlers.indexOf(handler)
        if (index > -1) {
          handlers.splice(index, 1)
        }
      }
    }
  }

  /**
   * 触发事件
   */
  private trigger(event: string, data: any) {
    const handlers = this.messageHandlers.get(event)
    if (handlers) {
      handlers.forEach(handler => handler(data))
    }
  }

  /**
   * 发送事件
   */
  emit(event: string, data: any) {
    if (this.socket?.connected) {
      this.socket.emit(event, data)
    } else {
      console.warn('WebSocket 未连接，无法发送事件')
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.socket?.connected || false
  }
}

export const websocketService = new WebSocketService()
