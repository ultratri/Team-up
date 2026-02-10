import { request } from '@utils/request'

export interface Message {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  senderAvatar?: string
  messageType: string
  content: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
  isRead: boolean
  isRecalled: boolean
  createdAt: string
  readAt?: string
}

export interface Conversation {
  id: number
  conversationType: string
  name: string
  avatar?: string
  lastMessage?: Message
  unreadCount: number
  isMuted: boolean
  isPinned: boolean
  lastMessageTime: string
}

// 获取会话列表
export function getConversations() {
  return request.get<Conversation[]>('/messages/conversations')
}

// 创建私聊会话
export function createPrivateConversation(userId: number) {
  return request.post('/messages/conversations/private', { userId })
}

// 创建群聊会话
export function createGroupConversation(data: {
  name: string
  avatar?: string
  memberIds: number[]
}) {
  return request.post('/messages/conversations/group', data)
}

// 获取会话消息列表
export function getConversationMessages(conversationId: number, page: number = 1, size: number = 50) {
  return request.get(`/messages/conversations/${conversationId}/messages`, {
    params: { page, size }
  })
}

// 发送消息
export function sendMessage(conversationId: number, data: {
  messageType: string
  content?: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
}) {
  return request.post(`/messages/conversations/${conversationId}/messages`, data)
}

// 标记消息为已读
export function markMessagesAsRead(conversationId: number, messageId: number) {
  return request.put(`/messages/conversations/${conversationId}/read`, { messageId })
}

// 撤回消息
export function recallMessage(messageId: number) {
  return request.put(`/messages/${messageId}/recall`)
}

// 获取未读总数
export function getUnreadCount() {
  return request.get<number>('/messages/unread-count')
}

// 搜索消息
export function searchMessages(keyword: string) {
  return request.get<Message[]>('/messages/search', { params: { keyword } })
}

// 置顶会话
export function pinConversation(conversationId: number, pinned: boolean) {
  return request.put(`/messages/conversations/${conversationId}/pin`, { pinned })
}

// 免打扰会话
export function muteConversation(conversationId: number, muted: boolean) {
  return request.put(`/messages/conversations/${conversationId}/mute`, { muted })
}

// 删除会话
export function deleteConversation(conversationId: number) {
  return request.delete(`/messages/conversations/${conversationId}`)
}
