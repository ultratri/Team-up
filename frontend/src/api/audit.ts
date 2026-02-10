import { request } from '@utils/request'
import type { ApiResult } from './auth'

export interface AuditLog {
  id: number
  userId: number
  username: string
  action: string
  resourceType: string
  resourceId?: number
  details: string
  ipAddress?: string
  userAgent?: string
  result: string
  errorMessage?: string
  createdAt: string
}

interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
  [key: string]: any
}

export interface AuditLogQuery {
  page?: number
  size?: number
  resourceType?: string
  resourceId?: number
  sortBy?: string
}

/**
 * 获取审计日志列表
 */
export async function getAuditLogs(params?: AuditLogQuery) {
  const res = await request.get<any>('/admin/audit-logs', { params })
  const pageData = (res as any).data || res
  
  return {
    records: pageData?.records || pageData?.list || [],
    total: pageData?.total ?? 0,
    page: pageData?.current || params?.page || 1,
    size: pageData?.size || params?.size || 20
  }
}
