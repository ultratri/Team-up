import { request } from '@utils/request'

export interface StatsOverview {
  totalProjects: number
  totalUsers: number
  totalTeams: number
  totalMessages: number
  activeUsers: number
  completedProjects: number
  projectTrend: TrendData[]
  projectStatus: StatusData[]
  topActiveUsers: ActiveUser[]
  departmentStats: DepartmentStats[]
}

export interface TrendData {
  date: string
  count: number
}

export interface StatusData {
  name: string
  value: number
}

export interface ActiveUser {
  userId: number
  name: string
  count: number
}

export interface DepartmentStats {
  department: string
  userCount: number
  projectCount: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/**
 * 获取统计总览
 */
export async function getStatsOverview(): Promise<StatsOverview> {
  const res = await request.get<ApiResult<StatsOverview>>('/stats/overview')
  console.log('📊 统计数据原始响应:', res)
  
  // 提取 data 字段
  const data = (res as any).data || res
  console.log('📊 提取后的数据:', data)
  
  // 确保返回的数据结构完整
  return {
    totalProjects: data?.totalProjects ?? 0,
    totalUsers: data?.totalUsers ?? 0,
    totalTeams: data?.totalTeams ?? 0,
    totalMessages: data?.totalMessages ?? 0,
    activeUsers: data?.activeUsers ?? 0,
    completedProjects: data?.completedProjects ?? 0,
    projectTrend: data?.projectTrend ?? [],
    projectStatus: data?.projectStatus ?? [],
    topActiveUsers: data?.topActiveUsers ?? [],
    departmentStats: data?.departmentStats ?? []
  }
}
