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
  // request.ts 的响应拦截器已经解包了 res.data
  const data = await request.get<EcosystemStats>('/ecosystem/stats')
  return data || { projectCount: 0, talentCount: 0, competitionCount: 0 }
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
  const data = await request.get<any>('/ecosystem/resources', { params })
  return {
    records: data?.records || [],
    total: data?.total || 0
  }
}

/**
 * 获取资源详情
 */
export async function getResourceDetail(id: number): Promise<Resource | null> {
  const data = await request.get<Resource>(`/ecosystem/resources/${id}`)
  return data || null
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
  const result = await request.post<number>('/ecosystem/resources', data)
  return result
}

/**
 * 点赞资源
 */
export async function likeResource(id: number): Promise<void> {
  await request.post<void>(`/ecosystem/resources/${id}/like`)
}

/**
 * 取消点赞资源
 */
export async function unlikeResource(id: number): Promise<void> {
  await request.delete<void>(`/ecosystem/resources/${id}/like`)
}

/**
 * 删除资源
 */
export async function deleteResource(id: number): Promise<void> {
  await request.delete<void>(`/ecosystem/resources/${id}`)
}

/**
 * 获取动态列表
 */
export async function getMoments(params: {
  page?: number
  size?: number
  type?: string
}): Promise<{ records: Moment[]; total: number }> {
  const data = await request.get<any>('/ecosystem/moments', { params })
  return {
    records: data?.records || [],
    total: data?.total || 0
  }
}

/**
 * 创建动态
 */
export async function createMoment(data: {
  type: string
  content: string
  relatedProjectId?: number
}): Promise<number> {
  const result = await request.post<number>('/ecosystem/moments', data)
  return result
}

/**
 * 点赞动态
 */
export async function likeMoment(id: number): Promise<void> {
  await request.post<void>(`/ecosystem/moments/${id}/like`)
}

/**
 * 取消点赞动态
 */
export async function unlikeMoment(id: number): Promise<void> {
  await request.delete<void>(`/ecosystem/moments/${id}/like`)
}

/**
 * 删除动态
 */
export async function deleteMoment(id: number): Promise<void> {
  await request.delete<void>(`/ecosystem/moments/${id}`)
}
