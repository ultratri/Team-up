import { request } from '@utils/request'
import type { ApiResult } from './auth'

export interface User {
  id: number
  studentId: string
  username: string
  email: string
  phone?: string
  status: string
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
  roles?: string[]
}

interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
  [key: string]: any
}

export interface UserListQuery {
  page?: number
  size?: number
  keyword?: string
  role?: string
  status?: string
  sortBy?: string
}

export interface UserCreateRequest {
  studentId: string
  username: string
  password: string
  email: string
  phone?: string
  role?: 'STUDENT' | 'MENTOR' | 'PLATFORM_ADMIN'
}

export interface UserUpdateRequest {
  studentId?: string
  username?: string
  password?: string
  email?: string
  phone?: string
  status?: string
  role?: 'STUDENT' | 'MENTOR' | 'PLATFORM_ADMIN'
}

/**
 * 获取用户列表（管理员）
 */
export async function getUserList(params?: UserListQuery) {
  const res = await request.get<any>('/users/list', { params })
  // request 拦截器已处理，res 可能是 Page 对象
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      const data = res.data as any
      return {
        records: data.records || [],
        total: data.total || 0,
        page: data.current || params?.page || 1,
        size: data.size || params?.size || 10
      }
    }
    if ('records' in res || 'list' in res) {
      return {
        records: (res as any).records || (res as any).list || [],
        total: (res as any).total || 0,
        page: (res as any).current || params?.page || 1,
        size: (res as any).size || params?.size || 10
      }
    }
  }
  return { records: [], total: 0, page: params?.page || 1, size: params?.size || 10 }
}

/**
 * 创建用户（管理员）
 */
export async function createUser(data: UserCreateRequest) {
  const res = await request.post<any>('/users', data)
  // request 拦截器已处理
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200 && res.data) return res.data
    throw new Error(res.message || '创建用户失败')
  }
  return res
}

/**
 * 更新用户（管理员）
 */
export async function updateUser(id: number, data: UserUpdateRequest) {
  const res = await request.put<any>(`/users/${id}`, data)
  // request 拦截器已处理
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200 && res.data) return res.data
    throw new Error(res.message || '更新用户失败')
  }
  return res
}

/**
 * 删除用户（管理员）
 */
export async function deleteUser(id: number) {
  const res = await request.delete<any>(`/users/${id}`)
  // request 拦截器已处理
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '删除用户失败')
  }
  return res
}

/**
 * 获取用户详情
 */
export async function getUserById(id: number) {
  const res = await request.get<any>(`/users/${id}`)
  // request拦截器可能已经提取了data，需要处理两种情况
  if (res && typeof res === 'object') {
    if ('code' in res) {
      if (res.code === 200 && res.data) return res.data
      throw new Error(res.message || '获取用户信息失败')
    }
    // 如果已经是数据对象，直接返回
    return res
  }
  throw new Error('获取用户信息失败')
}

/**
 * 搜索用户（用于选择导师/成员等）
 * 兼容后端返回：
 * - ApiResult<{ records: User[] }>
 * - ApiResult<User[]>
 * - { records: User[] }
 * - User[]
 */
export async function searchUsers(keyword: string, limit = 10): Promise<User[]> {
  const params = { keyword, page: 1, size: limit }

  // 优先尝试通用 list 接口（本文件已有使用）
  const res = await request.get<any>('/users/list', { params })

  if (!res) return []

  // ApiResult wrapper
  if (typeof res === 'object' && 'code' in res) {
    if ((res as any).code !== 200) throw new Error((res as any).message || '搜索用户失败')
    const data = (res as any).data
    if (Array.isArray(data)) return data
    if (data && typeof data === 'object') {
      if (Array.isArray((data as any).records)) return (data as any).records
      if (Array.isArray((data as any).list)) return (data as any).list
    }
    return []
  }

  // plain page or list
  if (Array.isArray(res)) return res
  if (typeof res === 'object') {
    if (Array.isArray((res as any).records)) return (res as any).records
    if (Array.isArray((res as any).list)) return (res as any).list
  }

  return []
}
