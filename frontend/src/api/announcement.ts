import { request } from '@utils/request'
import type { ApiResult } from './auth'

export interface Announcement {
  id: number
  title: string
  content: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  isActive: boolean
  publisherId: number
  publishedAt: string
  expiresAt?: string
}

interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
  [key: string]: any
}

export interface AnnouncementRequest {
  title: string
  content: string
  receiverType: 'ALL' | 'SPECIFIC'
  userIds?: number[]
  priority?: 'HIGH' | 'MEDIUM' | 'LOW'
  notificationType?: string  // 新增：通知类型
}

export interface AnnouncementListQuery {
  page?: number
  size?: number
  keyword?: string
}

/**
 * 创建公告
 */
export async function createAnnouncement(data: AnnouncementRequest) {
  await request.post<any>('/admin/announcements', data)
  // 如果没有抛出异常，说明成功
}

/**
 * 获取公告列表（管理员）
 */
export async function getAnnouncements(params?: AnnouncementListQuery) {
  const res = await request.get<any>('/admin/announcements', { params })
  const pageData = (res as any).data || res
  
  return {
    records: pageData?.records || pageData?.list || [],
    total: pageData?.total ?? 0,
    page: pageData?.current || params?.page || 1,
    size: pageData?.size || params?.size || 20
  }
}

/**
 * 删除公告
 */
export async function deleteAnnouncement(id: number) {
  await request.delete<any>(`/admin/announcements/${id}`)
  // 如果没有抛出异常，说明成功
}
