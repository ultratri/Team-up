import { request } from '@utils/request'
import type {
  Competition,
  CompetitionCreateRequest,
  CompetitionUpdateRequest,
  CompetitionListQuery,
  CompetitionListResponse,
  TeamMentorApplication,
  MentorApplicationCreateRequest,
  MentorApplicationListQuery
} from '../types/competition'
import type { Team, TeamCreateRequest, TeamListResponse } from '../types/team'
import type { ApiResult } from './auth'

interface PageResult<T> {
  records: T[]
  total: number
  page?: number
  size?: number
  [key: string]: any
}

export interface CompetitionStats {
  competitionId: number
  teamCount: number
  memberCount: number
  mentorTeamCount: number
  mentorCoverageRate: number
  viewCount?: number
}

export interface CompetitionTrendPoint {
  date: string
  teamCount: number
  memberCount: number
  viewCount: number
}

export interface CompetitionLeaderboardEntry {
  teamId: number
  teamName: string
  memberCount: number
  hasMentor: boolean
  totalTasks: number
  doneTasks: number
  completionRate: number
  score?: number | null
  comment?: string | null
  scoredBy?: number | null
  scoredByName?: string | null
}

/**
 * 获取比赛列表
 */
export async function getCompetitions(params?: CompetitionListQuery): Promise<CompetitionListResponse> {
  const res = await request.get<ApiResult<PageResult<Competition>>>('/api/competitions', { params })
  if (res.code === 200 && res.data) {
    // 修复：如果 total 为 0 但有 records，使用 records 长度
    const total = res.data.total || res.data.records?.length || 0
    return {
      records: res.data.records || [],
      total: total,
      page: res.data.page || params?.page || 1,
      size: res.data.size || params?.size || 10
    }
  }
  return { records: [], total: 0, page: 1, size: 10 }
}

/**
 * 获取比赛详情
 */
export async function getCompetitionDetail(id: number): Promise<Competition | null> {
  const res = await request.get<ApiResult<Competition>>(`/api/competitions/${id}`)
  if (res.code === 200 && res.data) {
    // 解析 attachments JSON 字符串
    if (res.data.attachments && typeof res.data.attachments === 'string') {
      try {
        res.data.attachments = JSON.parse(res.data.attachments)
      } catch (e) {
        console.warn('Failed to parse competition attachments:', e)
        res.data.attachments = []
      }
    }
    return res.data
  }
  return null
}

/**
 * 创建比赛（管理员/教师）
 */
export async function createCompetition(data: CompetitionCreateRequest): Promise<Competition> {
  const res = await request.post<ApiResult<Competition>>('/api/competitions', data)
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '创建比赛失败')
}

/**
 * 更新比赛（管理员/教师）
 */
export async function updateCompetition(id: number, data: Partial<CompetitionUpdateRequest>): Promise<Competition> {
  const res = await request.put<ApiResult<Competition>>(`/api/competitions/${id}`, data)
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '更新比赛失败')
}

/**
 * 发布比赛（管理员/教师）
 */
export async function publishCompetition(id: number): Promise<void> {
  const res = await request.post<ApiResult<void>>(`/api/competitions/${id}/publish`)
  if (res.code !== 200) {
    throw new Error(res.message || '发布比赛失败')
  }
}

/**
 * 归档比赛（管理员/教师）
 */
export async function archiveCompetition(id: number): Promise<void> {
  const res = await request.post<ApiResult<void>>(`/api/competitions/${id}/archive`)
  if (res.code !== 200) {
    throw new Error(res.message || '归档比赛失败')
  }
}

/**
 * 获取比赛下的队伍列表
 */
export async function getCompetitionTeams(
  competitionId: number,
  params?: { page?: number; size?: number }
): Promise<TeamListResponse> {
  const res = await request.get<ApiResult<PageResult<Team>>>(`/api/competitions/${competitionId}/teams`, { params })
  if (res.code === 200 && res.data) {
    // MyBatis-Plus Page 返回的字段名是 records, total, current, size
    return {
      records: (res.data as any).records || res.data.records || [],
      total: (res.data as any).total || res.data.total || 0,
      page: (res.data as any).current || params?.page || 1,
      size: (res.data as any).size || params?.size || 10
    }
  }
  return { records: [], total: 0, page: 1, size: 10 }
}

/**
 * 在比赛下创建队伍
 */
export async function createCompetitionTeam(
  competitionId: number,
  data: Omit<TeamCreateRequest, 'competitionId' | 'type'>
): Promise<Team> {
  const res = await request.post<ApiResult<Team>>(`/api/competitions/${competitionId}/teams`, data)
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '创建队伍失败')
}

/**
 * 申请加入比赛队伍
 */
export async function applyJoinCompetitionTeam(
  competitionId: number,
  teamId: number,
  reason?: string
): Promise<any> {
  const res = await request.post<ApiResult<any>>(
    `/api/competitions/${competitionId}/teams/${teamId}/join`,
    { reason }
  )
  if (res.code === 200) {
    return res.data
  }
  throw new Error(res.message || '申请加入失败')
}

/**
 * 队伍发起导师申请
 */
export async function createMentorApplication(
  teamId: number,
  data: Omit<MentorApplicationCreateRequest, 'teamId'>
): Promise<TeamMentorApplication> {
  const res = await request.post<ApiResult<TeamMentorApplication>>(
    `/api/teams/${teamId}/mentor-applications`,
    null,
    {
      params: {
        mentorId: data.mentorId,
        reason: data.reason
      }
    }
  )
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '申请导师失败')
}

/**
 * 老师查看自己收到的导师申请列表
 */
export async function getMentorApplications(
  params?: MentorApplicationListQuery
): Promise<{ records: TeamMentorApplication[]; total: number }> {
  const res = await request.get<ApiResult<PageResult<TeamMentorApplication>>>('/api/mentor/applications', { params })
  if (res.code === 200 && res.data) {
    return {
      records: res.data.records || [],
      total: res.data.total || 0
    }
  }
  return { records: [], total: 0 }
}

/**
 * 老师接受导师申请
 */
export async function acceptMentorApplication(applicationId: number): Promise<void> {
  const res = await request.post<ApiResult<void>>(`/api/mentor/applications/${applicationId}/accept`)
  if (res.code !== 200) {
    throw new Error(res.message || '接受申请失败')
  }
}

/**
 * 老师拒绝导师申请
 */
export async function rejectMentorApplication(applicationId: number, reason?: string): Promise<void> {
  const res = await request.post<ApiResult<void>>(`/api/mentor/applications/${applicationId}/reject`, null, {
    params: { reason }
  })
  if (res.code !== 200) {
    throw new Error(res.message || '拒绝申请失败')
  }
}

/**
 * 删除比赛（管理员）
 */
export async function deleteCompetition(id: number): Promise<void> {
  const res = await request.delete<ApiResult<void>>(`/api/competitions/${id}`)
  if (res.code !== 200) {
    throw new Error(res.message || '删除比赛失败')
  }
}

/**
 * 上传比赛附件
 */
export async function uploadCompetitionAttachment(
  competitionId: number,
  file: File
): Promise<{ name: string; url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const token = window.localStorage.getItem('token') || ''
  
  const res = await fetch(`${baseUrl}/api/competitions/${competitionId}/attachments`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData
  })
  
  const data = await res.json()
  if (data.code === 200 && data.data) {
    return data.data
  }
  throw new Error(data.message || '上传附件失败')
}

/**
 * 删除比赛附件
 */
export async function deleteCompetitionAttachment(competitionId: number, url: string): Promise<void> {
  const res = await request.delete<ApiResult<void>>(`/api/competitions/${competitionId}/attachments`, {
    params: { url }
  })
  if (res.code !== 200) {
    throw new Error(res.message || '删除附件失败')
  }
}

/**
 * 获取比赛统计
 */
export async function getCompetitionStats(competitionId: number): Promise<CompetitionStats> {
  const res = await request.get<ApiResult<CompetitionStats>>(`/api/competitions/${competitionId}/stats`)
  if (res.code === 200 && res.data) {
    return res.data
  }
  throw new Error(res.message || '加载统计失败')
}

/**
 * 获取比赛时间维度趋势数据（最近 N 天）
 */
export async function getCompetitionTrend(
  competitionId: number,
  days: number = 7
): Promise<CompetitionTrendPoint[]> {
  const res = await request.get<ApiResult<CompetitionTrendPoint[]>>(
    `/api/competitions/${competitionId}/stats/trend`,
    { params: { days } }
  )
  if (res.code === 200 && res.data) {
    return res.data
  }
  return []
}

/**
 * 获取推荐比赛（登录用户）
 */
export async function getRecommendedCompetitions(params?: { page?: number; size?: number }): Promise<CompetitionListResponse> {
  const res = await request.get<ApiResult<PageResult<Competition>>>('/api/competitions/recommendations', { params })
  if (res.code === 200 && res.data) {
    return {
      records: (res.data as any).records || [],
      total: (res.data as any).total || 0,
      page: (res.data as any).current || params?.page || 1,
      size: (res.data as any).size || params?.size || 6
    }
  }
  return { records: [], total: 0, page: 1, size: params?.size || 6 }
}

export async function getCompetitionLeaderboard(competitionId: number, limit: number = 10): Promise<CompetitionLeaderboardEntry[]> {
  const res = await request.get<ApiResult<CompetitionLeaderboardEntry[]>>(`/api/competitions/${competitionId}/leaderboard`, {
    params: { limit }
  })
  if (res.code === 200 && res.data) return res.data
  return []
}

export async function upsertCompetitionTeamScore(
  competitionId: number,
  data: { teamId: number; score: number; comment?: string }
) {
  const res = await request.post<ApiResult<any>>(`/api/competitions/${competitionId}/scores`, data)
  if (res.code === 200) return res.data
  throw new Error(res.message || '评分失败')
}

export interface ScoredTeamRef {
  competitionId: number
  teamId: number
}

export async function getScoredCompetitionTeams(): Promise<ScoredTeamRef[]> {
  const res = await request.get<ApiResult<ScoredTeamRef[]>>('/api/competitions/scores/scored-teams')
  if (res.code === 200 && res.data) return res.data
  return []
}

export async function getHotCompetitions(size: number = 6): Promise<Competition[]> {
  const res = await request.get<ApiResult<Competition[]>>('/api/competitions/hot', { params: { size } })
  if (res.code === 200 && res.data) return res.data
  return []
}

export async function getMyMentorCompetitionTeams(params: {
  page?: number
  size?: number
  competitionId?: number
  keyword?: string
}): Promise<any> {
  const res = await request.get<ApiResult<any>>('/api/mentor/competition-teams', { params })
  if (res.code === 200 && res.data) return res.data
  return { records: [], total: 0, current: params.page || 1, size: params.size || 20 }
}
