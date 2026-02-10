import { request } from '@utils/request'

export interface Notification {
  id: number
  userId: number
  type: string
  title: string
  content: string
  relatedType?: string
  relatedId?: number
  isRead: boolean
  createdAt: string
  readAt?: string
}

// 获取通知列表
export function getNotifications(params: { page: number; size: number; isRead?: boolean; type?: string }) {
  return request.get('/notifications', { params })
}

// 获取未读数量
export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

// 标记为已读
export function markAsRead(id: number) {
  return request.put(`/notifications/${id}/read`)
}

// 全部标记为已读
export function markAllAsRead() {
  return request.put('/notifications/read-all')
}

// 删除通知
export function deleteNotification(id: number) {
  return request.delete(`/notifications/${id}`)
}
