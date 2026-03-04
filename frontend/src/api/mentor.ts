import request from '@/utils/request'
import type { ApiResult } from '@/types/common'

/**
 * 导师相关API
 */

/**
 * 导师卡片类型
 */
export interface MentorCard {
  id: number
  username: string
  realName?: string
  avatar?: string
  department?: string
  title?: string
  bio?: string
  rating: number
  totalStudents: number
  activeStudents: number
  contributionPoints: number
  specialties?: string[]
  available: boolean
}

/**
 * 获取导师广场列表
 */
export async function getMentorPlaza(params: {
  page?: number
  size?: number
  department?: string
  keyword?: string
  sortBy?: string
  availableOnly?: boolean
}): Promise<{ records: MentorCard[]; total: number }> {
  // 全局 request 已经将 ApiResult 解包为 data，这里直接拿分页对象
  const res = await request.get<any>('/mentor/plaza', { params })

  if (res && typeof res === 'object') {
    const records = (res.records || []) as MentorCard[]
    const total = typeof res.total === 'number' ? res.total : records.length
    return { records, total }
  }

  return { records: [], total: 0 }
}

/**
 * 获取导师的所有团队（包括比赛和非比赛团队）
 */
export async function getMyMentorTeams(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<{ records: any[]; total: number; current: number; size: number }> {
  const res = await request.get<any>('/mentor/teams', { params })

  if (res && typeof res === 'object') {
    if (Array.isArray(res.records)) {
      return {
        records: res.records,
        total: res.total ?? res.records.length ?? 0,
        current: res.current ?? res.page ?? params.page ?? 1,
        size: res.size ?? params.size ?? 20,
      }
    }
    if (Array.isArray(res)) {
      return {
        records: res,
        total: res.length,
        current: params.page ?? 1,
        size: params.size ?? res.length ?? 20,
      }
    }
  }

  return { records: [], total: 0, current: params.page || 1, size: params.size || 20 }
}

/**
 * 提交学员对导师的评价
 */
export async function submitMentorReview(data: {
  mentorId: number
  teamId: number
  professionalAbility: number
  guidanceAttitude: number
  responseSpeed: number
  helpfulness: number
  comment?: string
}): Promise<any> {
  // 拦截器已处理错误，只需返回解包后的数据
  const res = await request.post<any>('/mentor/reviews', data)
  return res
}

/**
 * 获取导师的评价列表
 */
export async function getMentorReviews(mentorId: number): Promise<any[]> {
  const res = await request.get<any>(`/mentor/reviews/mentor/${mentorId}`)
  if (Array.isArray(res)) return res
  if (res && Array.isArray(res.records)) return res.records
  return []
}

/**
 * 获取导师的评分统计
 */
export async function getMentorReviewStats(mentorId: number): Promise<{
  averageRating: number
  reviewCount: number
}> {
  const res = await request.get<any>(`/mentor/reviews/mentor/${mentorId}/stats`)
  if (res && typeof res === 'object') {
    return {
      averageRating: res.averageRating ?? 0,
      reviewCount: res.reviewCount ?? 0,
    }
  }
  return { averageRating: 0, reviewCount: 0 }
}

/**
 * 导师成员评价相关接口
 */

/**
 * 成员评价DTO
 */
export interface MentorMemberEvaluationDTO {
  memberId: number
  score: number
  technicalAbility?: number
  collaboration?: number
  learningAttitude?: number
  taskCompletion?: number
  comment?: string
}

/**
 * 团队成员信息（含评价状态）
 */
export interface TeamMemberForEvaluation {
  userId: number
  userName: string
  realName?: string
  role: string
  creditScore: number
  evaluated: boolean
  score?: number
  technicalAbility?: number
  collaboration?: number
  learningAttitude?: number
  taskCompletion?: number
  comment?: string
  evaluatedAt?: string
}

/**
 * 获取团队成员列表（供导师评价）
 */
export async function getTeamMembersForEvaluation(teamId: number): Promise<TeamMemberForEvaluation[]> {
  const res = await request.get<any>(`/mentor/evaluations/teams/${teamId}/members`)
  if (Array.isArray(res)) return res as TeamMemberForEvaluation[]
  if (res && Array.isArray(res.records)) return res.records as TeamMemberForEvaluation[]
  return []
}

/**
 * 提交或更新对单个成员的评价
 */
export async function evaluateMember(teamId: number, data: MentorMemberEvaluationDTO): Promise<void> {
  await request.post(`/mentor/evaluations/teams/${teamId}/members`, data)
}

/**
 * 批量提交评价
 */
export async function batchEvaluateMembers(teamId: number, evaluations: MentorMemberEvaluationDTO[]): Promise<void> {
  await request.post(`/mentor/evaluations/teams/${teamId}/members/batch`, evaluations)
}

/**
 * 团队导师申请相关接口
 */

/**
 * 团队导师申请类型
 */
export interface TeamMentorApplication {
  id: number
  teamId: number
  teamName?: string
  competitionId?: number
  competitionName?: string
  mentorId: number
  requesterId: number
  requesterName?: string
  reason?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  decidedAt?: string
}

/**
 * 获取导师的申请列表（团队向导师发出的申请）
 */
export async function getMentorApplicationsList(params: {
  page?: number
  size?: number
  status?: string
}): Promise<{ records: TeamMentorApplication[]; total: number }> {
  // 后端实际路径为 /mentor/applications，request 已解包 ApiResult
  const res = await request.get<any>('/mentor/applications', { params })

  if (res && typeof res === 'object') {
    const records = (res.records || []) as TeamMentorApplication[]
    const total = typeof res.total === 'number' ? res.total : records.length
    return { records, total }
  }

  return { records: [], total: 0 }
}

/**
 * 接受团队的导师申请
 */
export async function acceptMentorApplication(applicationId: number): Promise<void> {
  await request.post(`/mentor/applications/${applicationId}/accept`)
}

/**
 * 拒绝团队的导师申请
 */
export async function rejectMentorApplication(applicationId: number, reason?: string): Promise<void> {
  await request.post(`/mentor/applications/${applicationId}/reject`, { reason })
}
