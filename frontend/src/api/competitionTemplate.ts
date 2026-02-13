import { request } from '@utils/request'
import type { ApiResult } from './auth'
import type { Competition } from '@/types/competition'

export interface CompetitionTemplate {
  id: number
  name: string
  payload: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
  [key: string]: any
}

export async function getCompetitionTemplates(params?: { page?: number; size?: number; keyword?: string }) {
  try {
    const res = await request.get<any>('/competition-templates', { params })
    // request 拦截器返回 res.data（如果存在），否则返回 res
    // 后端返回 Result.success(Page)，拦截器会提取 Page 对象
    // MyBatis-Plus 的 Page 对象有 records 和 total 字段
    
    // 如果 res 有 code 字段，说明拦截器没有处理（不应该发生）
    if (res && typeof res === 'object' && 'code' in res && res.code === 200 && res.data) {
      const data = res.data as any
      return {
        records: data.records || [],
        total: data.total || 0,
        page: data.current || params?.page || 1,
        size: data.size || params?.size || 20
      }
    }
    
    // res 应该是 Page 对象（拦截器已提取 data）
    if (res && typeof res === 'object') {
      return {
        records: (res as any).records || [],
        total: (res as any).total || 0,
        page: (res as any).current || params?.page || 1,
        size: (res as any).size || params?.size || 20
      }
    }
    
    // 如果都没有，返回空结果
    return {
      records: [],
      total: 0,
      page: params?.page || 1,
      size: params?.size || 20
    }
  } catch (error: any) {
    console.error('加载模板失败:', error)
    // 即使出错也返回空结果，而不是抛出错误
    return {
      records: [],
      total: 0,
      page: params?.page || 1,
      size: params?.size || 20
    }
  }
}

export async function createCompetitionTemplate(data: { name: string; payload: any }) {
  try {
    const res = await request.post<CompetitionTemplate>('/competition-templates', data)
    return res
  } catch (error: any) {
    throw new Error(error.message || '创建模板失败')
  }
}

export async function updateCompetitionTemplate(id: number, data: { name: string; payload: any }) {
  try {
    const res = await request.put<CompetitionTemplate>(`/competition-templates/${id}`, data)
    return res
  } catch (error: any) {
    throw new Error(error.message || '更新模板失败')
  }
}

export async function deleteCompetitionTemplate(id: number) {
  try {
    await request.delete<void>(`/competition-templates/${id}`)
  } catch (error: any) {
    throw new Error(error.message || '删除模板失败')
  }
}

export async function createCompetitionFromTemplate(
  templateId: number,
  overrides?: { name?: string; signupStartAt?: string; signupEndAt?: string; startAt?: string; endAt?: string }
): Promise<Competition> {
  try {
    const res = await request.post<Competition>(`/competition-templates/${templateId}/create-competition`, overrides || {})
    return res
  } catch (error: any) {
    throw new Error(error.message || '从模板创建比赛失败')
  }
}

