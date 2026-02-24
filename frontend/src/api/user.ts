import { request } from '@utils/request'
import type { ApiResult } from './auth'

export interface User {
  id: number
  userCode: string
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
  userCode: string
  username: string
  password: string
  email: string
  phone?: string
  role?: 'STUDENT' | 'MENTOR' | 'PLATFORM_ADMIN'
}

export interface UserUpdateRequest {
  userCode?: string
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
 * 根据学号获取用户信息
 */
export async function getUserByUserCode(userCode: string) {
  const res = await request.get<any>(`/users/user-code/${userCode}`)
  if (res && typeof res === 'object') {
    if ('code' in res) {
      if (res.code === 200 && res.data) return res.data
      throw new Error(res.message || '获取用户信息失败')
    }
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

// 组队意向相关接口
export interface UserAvailabilityVO {
  isAvailable: boolean
  intentions: string[]
  visibility: string
  availableFrom?: string
  availableUntil?: string
  weeklyHours?: number
  notes?: string
}

export interface UserAvailabilityRequest {
  isAvailable: boolean
  intentions: string[]
  visibility: string
  availableFrom?: string
  availableUntil?: string
  weeklyHours?: number
  notes?: string
}

/**
 * 获取当前用户的组队意向
 */
export async function getUserAvailability(): Promise<UserAvailabilityVO> {
  const res = await request.get<any>('/user/availability')
  if (res && typeof res === 'object') {
    if ('code' in res) {
      if (res.code === 200 && res.data) return res.data
      throw new Error(res.message || '获取组队意向失败')
    }
    return res
  }
  throw new Error('获取组队意向失败')
}

/**
 * 获取指定用户的组队意向
 */
export async function getUserAvailabilityById(userId: number): Promise<UserAvailabilityVO> {
  const res = await request.get<any>(`/user/availability/${userId}`)
  if (res && typeof res === 'object') {
    if ('code' in res) {
      if (res.code === 200 && res.data) return res.data
      throw new Error(res.message || '获取用户组队意向失败')
    }
    return res
  }
  throw new Error('获取用户组队意向失败')
}

/**
 * 更新当前用户的组队意向
 */
export async function updateUserAvailability(data: UserAvailabilityRequest): Promise<void> {
  const res = await request.put<any>('/user/availability', data)
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '更新组队意向失败')
  }
}

// 人才墙相关接口
export interface TalentVO {
  id: number
  username: string
  realName: string
  avatarUrl?: string
  department: string
  major: string
  bio?: string
  projectExperience?: string
  creditScore?: number
  skills: string[]
  intentions: string[]
  weeklyHours?: number
  notes?: string
  lastLoginAt?: string
  status?: string
  visibility?: string
  availableFrom?: string
  availableUntil?: string
  email?: string
  phone?: string
  wechat?: string
  qq?: string
}

export interface TalentListQuery {
  page?: number
  size?: number
  department?: string
  keyword?: string
  intention?: string
}

/**
 * 获取人才列表
 */
export async function getTalentList(params?: TalentListQuery): Promise<PageResult<TalentVO>> {
  const res = await request.get<any>('/talents', { params })
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      const data = res.data as any
      return {
        records: data.records || [],
        total: data.total || 0,
        current: data.current || params?.page || 1,
        size: data.size || params?.size || 12
      }
    }
    if ('records' in res) {
      return {
        records: res.records || [],
        total: res.total || 0,
        current: res.current || params?.page || 1,
        size: res.size || params?.size || 12
      }
    }
  }
  return { records: [], total: 0, current: params?.page || 1, size: params?.size || 12 }
}

/**
 * 获取用户个人资料
 */
export async function getUserProfile(userId: number): Promise<any> {
  const res = await request.get<any>(`/profile/${userId}`)
  if (res && typeof res === 'object') {
    if ('code' in res) {
      if (res.code === 200 && res.data) return res.data
      throw new Error(res.message || '获取用户资料失败')
    }
    return res
  }
  throw new Error('获取用户资料失败')
}

/**
 * 获取用户技能列表
 */
export async function getUserSkills(userId: number): Promise<any[]> {
  const res = await request.get<any>(`/profile/${userId}/skills`)
  if (res && typeof res === 'object') {
    if ('code' in res) {
      if (res.code === 200 && res.data) return Array.isArray(res.data) ? res.data : []
      return []
    }
    if (Array.isArray(res)) return res
  }
  return []
}
