import { request } from '@/utils/request'

/**
 * 生态广场统计数据
 */
export interface EcosystemStats {
  projectCount: number
  talentCount: number
  competitionCount: number
}

/**
 * 资源信息
 */
export interface Resource {
  id: number
  title: string
  description: string
  type: string
  cover?: string
  content: string
  author: {
    id: number
    realName: string
    avatar?: string
  }
  tags: string[]
  views: number
  likes: number
  liked: boolean
  createdAt: string
}

/**
 * 动态信息
 */
export interface Moment {
  id: number
  type: string
  content: string
  user: {
    id: number
    realName: string
    avatar?: string
  }
  relatedProject?: {
    id: number
    title: string
  }
  likes: number
  comments: number
  liked: boolean
  createdAt: string
}

/**
 * 获取统计数据
 */
export async function getEcosystemStats(): Promise<EcosystemStats> {
  const res = await request.get<any>('/api/ecosystem/stats')
  if (res.code === 200 && res.data) {
    return res.data
  }
  return { projectCount: 0, talentCount: 0, competitionCount: 0 }
}

/**
 * 获取资源列表
 */
export async function getResources(params: {
  page?: number
  size?: number
  type?: string
  sortBy?: string
}): Promise<{ records: Resource[]; total: number }> {
  const res = await request.get<any>('/api/ecosystem/resources', { params })
  if (res.code === 200 && res.data) {
    return {
      records: res.data.records || [],
      total: res.data.total || 0
    }
  }
  return { records: [], total: 0 }
}

/**
 * 获取资源详情
 */
export async function getResourceDetail(id: number): Promise<Resource | null> {
  const res = await request.get<any>(`/api/ecosystem/resources/${id}`)
  if (res.code === 200 && res.data) {
    return res.data
  }
  return null
}

/**
 * 创建资源
 */
export async function createResource(data: {
  title: string
  description?: string
  type: string
  cover?: string
  content: string
  projectId?: number
  tags?: string[]
}): Promise<number> {
  const res = await request.post<any>('/api/ecosystem/resources', data)
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '创建资源失败')
}

/**
 * 点赞资源
 */
export async function likeResource(id: number): Promise<void> {
  const res = await request.post<any>(`/api/ecosystem/resources/${id}/like`)
  if (res.code !== 200) {
    throw new Error(res.message || '点赞失败')
  }
}

/**
 * 取消点赞资源
 */
export async function unlikeResource(id: number): Promise<void> {
  const res = await request.delete<any>(`/api/ecosystem/resources/${id}/like`)
  if (res.code !== 200) {
    throw new Error(res.message || '取消点赞失败')
  }
}

/**
 * 删除资源
 */
export async function deleteResource(id: number): Promise<void> {
  const res = await request.delete<any>(`/api/ecosystem/resources/${id}`)
  if (res.code !== 200) {
    throw new Error(res.message || '删除资源失败')
  }
}

/**
 * 获取动态列表
 */
export async function getMoments(params: {
  page?: number
  size?: number
  type?: string
}): Promise<{ records: Moment[]; total: number }> {
  const res = await request.get<any>('/api/ecosystem/moments', { params })
  if (res.code === 200 && res.data) {
    return {
      records: res.data.records || [],
      total: res.data.total || 0
    }
  }
  return { records: [], total: 0 }
}

/**
 * 创建动态
 */
export async function createMoment(data: {
  type: string
  content: string
  relatedProjectId?: number
}): Promise<number> {
  const res = await request.post<any>('/api/ecosystem/moments', data)
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '创建动态失败')
}

/**
 * 点赞动态
 */
export async function likeMoment(id: number): Promise<void> {
  const res = await request.post<any>(`/api/ecosystem/moments/${id}/like`)
  if (res.code !== 200) {
    throw new Error(res.message || '点赞失败')
  }
}

/**
 * 取消点赞动态
 */
export async function unlikeMoment(id: number): Promise<void> {
  const res = await request.delete<any>(`/api/ecosystem/moments/${id}/like`)
  if (res.code !== 200) {
    throw new Error(res.message || '取消点赞失败')
  }
}

/**
 * 删除动态
 */
export async function deleteMoment(id: number): Promise<void> {
  const res = await request.delete<any>(`/api/ecosystem/moments/${id}`)
  if (res.code !== 200) {
    throw new Error(res.message || '删除动态失败')
  }
}
