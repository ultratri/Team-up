import { request } from '@utils/request'
import { cachedRequest, requestCache } from '@utils/requestOptimization'
import { 
  normalizeTeam, 
  normalizeTeams, 
  normalizeTeamMember,
  normalizeTeamMembers,
  normalizeTeamActivities,
  normalizeTeamMatchItem,
  normalizeCandidateMatchItem,
  type TeamMatchItem,
  type CandidateMatchItem
} from '@utils/fieldNormalizer'
import type {
  Team,
  Task,
  TeamCreateRequest,
  TeamMember,
  TeamListQuery,
  TeamListResponse,
  TeamDetailResponse,
  TeamStatistics,
  TeamJoinApplication
} from '../types/team'
import type { ApiResult } from './auth'

interface PageResult<T> {
  records: T[]
  total: number
  page?: number
  size?: number
  current?: number
  [key: string]: any
}

// 缓存时间配置（毫秒）
const CACHE_TTL = {
  TEAM_LIST: 2 * 60 * 1000,      // 团队列表缓存 2 分钟
  TEAM_DETAIL: 5 * 60 * 1000,    // 团队详情缓存 5 分钟
  TEAM_MEMBERS: 3 * 60 * 1000,   // 团队成员缓存 3 分钟
  TEAM_STATISTICS: 1 * 60 * 1000 // 统计数据缓存 1 分钟
}

/**
 * 评价提交数据接口
 */
export interface EvaluationSubmitDTO {
  evaluatedId: number
  techContributionScore: number
  collaborationScore: number
  taskCompletionScore: number
  comment?: string
  isAnonymous?: boolean
}

/**
 * 获取用户团队列表（带缓存）
 * @param userId 用户 ID
 * @param params 查询参数（关键词、状态、分页）
 * @param useCache 是否使用缓存
 */
export function getUserTeams(
  userId: number,
  params?: Omit<TeamListQuery, 'userId'>,
  useCache: boolean = true
): Promise<TeamListResponse | Team[]> {
  const requestFn = async () => {
    const response = await request.get(`/teams/user/${userId}`, { params })
    
    // 转换字段
    if (Array.isArray(response)) {
      return normalizeTeams(response)
    } else if (response && 'records' in response) {
      return {
        ...response,
        records: normalizeTeams(response.records)
      }
    }
    return response
  }
  
  if (useCache) {
    return cachedRequest(
      requestFn,
      `/teams/user/${userId}`,
      params,
      CACHE_TTL.TEAM_LIST
    )
  }
  
  return requestFn()
}

/**
 * 获取团队详情（带缓存）
 * @param teamId 团队 ID
 * @param useCache 是否使用缓存
 */
export function getTeam(
  teamId: number,
  useCache: boolean = true
): Promise<TeamDetailResponse | Team> {
  const requestFn = async () => {
    const response = await request.get(`/teams/${teamId}`)
    return normalizeTeam(response)
  }
  
  if (useCache) {
    return cachedRequest(
      requestFn,
      `/teams/${teamId}`,
      undefined,
      CACHE_TTL.TEAM_DETAIL
    )
  }
  
  return requestFn()
}

/**
 * 成员找团队（个人找长期团队）
 */
export async function getMatchedTeamsForMe(page = 1, size = 20): Promise<TeamMatchItem[]> {
  const list = await request.get<any[]>('/teams/match-for-me', {
    params: { page, size }
  })
  return (list || []).map(normalizeTeamMatchItem)
}

/**
 * 团队找成员：为团队匹配候选人
 */
export async function getMatchedCandidatesForTeam(
  teamId: number,
  keyword?: string
): Promise<CandidateMatchItem[]> {
  const list = await request.post<any[]>(`/teams/${teamId}/match`, null, {
    params: { keyword }
  })
  return (list || []).map(normalizeCandidateMatchItem)
}

/**
 * 获取队伍加入申请列表（队长）
 */
export async function getTeamJoinApplications(
  teamId: number,
  params?: { page?: number; size?: number; status?: string }
) {
  return request.get(`/teams/${teamId}/join-applications`, { params }) as any
}

/**
 * 审核队伍加入申请（队长）
 */
export async function reviewTeamJoinApplication(
  applicationId: number,
  approved: boolean,
  comment?: string
) {
  return request.post(`/teams/join-applications/${applicationId}/review`, null, {
    params: { approved, comment }
  }) as any
}

/**
 * 申请加入团队（长期团队）
 */
export async function applyJoinTeam(teamId: number, reason?: string): Promise<any> {
  return request.post(`/teams/${teamId}/join`, { reason }) as any
}

/**
 * 创建团队
 * @param data 团队创建数据
 */
export function createTeam(data: TeamCreateRequest): Promise<Team> {
  // 兼容旧 API：如果使用新字段，转换为旧字段
  const requestData = {
    teamName: data.name || data.teamName,
    projectId: data.projectId,
    leaderId: data.leaderId,
    description: data.description,
    avatar: data.avatar
  }
  
  // 创建后清除团队列表缓存
  return request.post('/teams', requestData).then((team) => {
    requestCache.invalidateByPrefix('/teams/user/')
    return normalizeTeam(team)
  })
}

/**
 * 上传团队头像
 * @param teamId 团队 ID
 * @param file 头像文件
 */
export function uploadTeamAvatar(teamId: number, file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ url: string }>(`/teams/${teamId}/avatar`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取团队成员（带缓存）
 * @param teamId 团队 ID
 * @param useCache 是否使用缓存
 */
export function getTeamMembers(
  teamId: number,
  useCache: boolean = true
): Promise<TeamMember[]> {
  const requestFn = async () => {
    const response = await request.get(`/teams/${teamId}/members`)
    return normalizeTeamMembers(response)
  }
  
  if (useCache) {
    return cachedRequest(
      requestFn,
      `/teams/${teamId}/members`,
      undefined,
      CACHE_TTL.TEAM_MEMBERS
    )
  }
  
  return requestFn()
}

/**
 * 添加团队成员
 * @param teamId 团队 ID
 * @param userId 用户 ID
 * @param role 角色（可选）
 */
export function addTeamMember(
  teamId: number,
  userId: number,
  role?: string
): Promise<void> {
  return request.post(`/teams/${teamId}/members`, { userId, role }).then((result) => {
    // 清除相关缓存
    requestCache.delete(`/teams/${teamId}/members`)
    requestCache.delete(`/teams/${teamId}`)
    return result
  })
}

/**
 * 移除团队成员
 * @param teamId 团队 ID
 * @param userId 用户 ID
 */
export function removeTeamMember(teamId: number, userId: number): Promise<void> {
  return request.delete(`/teams/${teamId}/members/${userId}`).then((result) => {
    // 清除相关缓存
    requestCache.delete(`/teams/${teamId}/members`)
    requestCache.delete(`/teams/${teamId}`)
    return result
  })
}

/**
 * 获取团队统计数据（带缓存）
 * 
 * 返回团队的统计指标，包括：
 * - taskCompletionRate: 任务完成率（百分比）
 * - activeDays: 活跃天数
 * - messageCount: 消息总数
 * - fileCount: 文件总数
 * 
 * @param teamId 团队 ID
 * @param useCache 是否使用缓存，默认 true
 * @returns Promise<TeamStatistics> 团队统计数据
 * @throws {Error} 当团队不存在或用户无权限访问时抛出错误
 * 
 * @example
 * ```typescript
 * const stats = await getTeamStatistics(123);
 * console.log(`任务完成率: ${stats.taskCompletionRate}%`);
 * ```
 */
export function getTeamStatistics(
  teamId: number,
  useCache: boolean = true
): Promise<TeamStatistics> {
  const requestFn = () => request.get<TeamStatistics>(`/teams/${teamId}/statistics`)
  
  if (useCache) {
    return cachedRequest(
      requestFn,
      `/teams/${teamId}/statistics`,
      undefined,
      CACHE_TTL.TEAM_STATISTICS
    )
  }
  
  return requestFn()
}

/**
 * 获取团队活动记录
 * 
 * 返回团队最近的活动记录列表，按时间倒序排列。
 * 活动类型包括：task（任务）、file（文件）、message（消息）、member（成员）、setting（设置）
 * 
 * @param teamId 团队 ID
 * @param limit 返回数量限制，默认10条
 * @returns Promise<TeamActivity[]> 活动记录列表
 * @throws {Error} 当团队不存在或用户无权限访问时抛出错误
 * 
 * @example
 * ```typescript
 * const activities = await getTeamActivities(123, 20);
 * activities.forEach(activity => {
 *   console.log(`${activity.username} ${activity.detail}`);
 * });
 * ```
 */
export function getTeamActivities(
  teamId: number,
  limit: number = 10
): Promise<import('../types/team').TeamActivity[]> {
  return request.get<import('../types/team').TeamActivity[]>(
    `/teams/${teamId}/activities`, 
    { params: { limit } }
  ).then(normalizeTeamActivities)
}

/**
 * 提交团队成员评价
 * 
 * 提交对团队成员的评价，包括技术贡献、协作能力和任务完成度三个维度的评分。
 * 评价可以选择匿名，匿名评价不会显示评价者身份。
 * 
 * @param teamId 团队 ID
 * @param data 评价数据
 * @returns Promise<void> 提交成功无返回值
 * @throws {Error} 当团队不存在、用户无权限、评价数据无效、重复评价或自我评价时抛出错误
 * 
 * @example
 * ```typescript
 * await submitEvaluation(123, {
 *   evaluatedId: 456,
 *   techContributionScore: 5,
 *   collaborationScore: 4,
 *   taskCompletionScore: 5,
 *   comment: '工作认真负责',
 *   isAnonymous: true
 * });
 * ```
 */
export function submitEvaluation(
  teamId: number,
  data: EvaluationSubmitDTO
): Promise<void> {
  return request.post(`/teams/${teamId}/evaluations`, data)
}

// 获取团队任务
export function getTeamTasks(teamId: number) {
  return request.get<Task[]>(`/tasks/team/${teamId}`)
}

// 筛选团队任务
export function filterTeamTasks(teamId: number, filters: {
  status?: string
  priority?: string
  assigneeId?: number
  keyword?: string
}) {
  return request.get<Task[]>(`/tasks/team/${teamId}/filter`, { params: filters })
}

/**
 * 为任务推荐负责人（任务内匹配负责人，仅在所属团队成员内）
 */
export async function getTaskAssigneeRecommendations(
  taskId: number,
  limit: number = 5
): Promise<CandidateMatchItem[]> {
  const list = await request.get<any[]>(`/tasks/${taskId}/match-assignees`, {
    params: { limit }
  })
  return (list || []).map(normalizeCandidateMatchItem)
}

// 创建任务
export function createTask(data: Partial<Task>) {
  return request.post<Task>('/tasks', data)
}

// 更新任务
export function updateTask(data: Partial<Task>, userId?: number) {
  return request.put<Task>(`/tasks?userId=${userId}`, data)
}

// 删除任务
export function deleteTask(taskId: number, userId?: number) {
  return request.delete(`/tasks/${taskId}?userId=${userId}`)
}

// 获取我创建的项目的所有申请
export function getMyProjectApplications(page: number, size: number, status?: string) {
  return request.get('/projects/applications/my-projects', { params: { page, size, status } })
}

// 获取我的申请历史
export function getMyApplications(page: number, size: number) {
  return request.get('/projects/applications/my-applications', { params: { page, size } })
}

// 批量审核申请
export function batchReviewApplications(applicationIds: number[], approved: boolean, comment?: string) {
  return request.post('/projects/applications/batch-review', { applicationIds, approved, comment })
}

/**
 * 退出团队（普通成员）
 * @param teamId 团队 ID
 */
export function leaveTeam(teamId: number): Promise<void> {
  return request.delete(`/teams/${teamId}/leave`).then((result) => {
    // 清除相关缓存
    requestCache.invalidateByPrefix(`/teams/user/`)
    requestCache.delete(`/teams/${teamId}`)
    requestCache.delete(`/teams/${teamId}/members`)
    return result
  })
}

/**
 * 删除团队（仅领导者）
 * @param teamId 团队 ID
 */
export function deleteTeam(teamId: number): Promise<void> {
  return request.delete(`/teams/${teamId}`).then((result) => {
    // 清除所有相关缓存
    requestCache.invalidateByPrefix(`/teams/user/`)
    requestCache.delete(`/teams/${teamId}`)
    requestCache.delete(`/teams/${teamId}/members`)
    requestCache.delete(`/teams/${teamId}/statistics`)
    return result
  })
}

/**
 * 获取我的入队申请列表
 */
export async function getMyTeamJoinApplications(params?: {
  page?: number
  size?: number
  status?: string
}): Promise<{ records: TeamJoinApplication[]; total: number; page: number; size: number }> {
  try {
    const res = await request.get<PageResult<TeamJoinApplication>>('/teams/join-applications/my', { params })
    // request拦截器已经解包了数据
    if (res && typeof res === 'object') {
      return {
        records: (res as any).records || [],
        total: (res as any).total || 0,
        page: (res as any).current || params?.page || 1,
        size: (res as any).size || params?.size || 10
      }
    }
    return { records: [], total: 0, page: 1, size: 10 }
  } catch (error) {
    console.error('Failed to get my team join applications:', error)
    return { records: [], total: 0, page: 1, size: 10 }
  }
}

/**
 * 撤回入队申请
 */
export async function withdrawTeamJoinApplication(applicationId: number): Promise<void> {
  await request.post<void>(`/teams/join-applications/${applicationId}/withdraw`)
}

// ==================== 管理员团队管理 API ====================

/**
 * 管理员获取团队列表（分页）
 */
export function getAdminTeamList(params: {
  page?: number
  size?: number
  type?: string
  isActive?: boolean
  keyword?: string
}): Promise<PageResult<any>> {
  return request.get('/admin/teams', { params }).then((res: any) => res.data || res)
}

/**
 * 管理员获取团队详情
 */
export function getAdminTeamDetail(teamId: number): Promise<any> {
  return request.get(`/admin/teams/${teamId}`).then((res: any) => res.data || res)
}

/**
 * 管理员解散团队
 */
export function dissolveTeam(teamId: number, reason: string): Promise<void> {
  return request.post(`/admin/teams/${teamId}/dissolve`, { reason })
}

/**
 * 管理员移除团队成员
 */
export function adminRemoveTeamMember(
  teamId: number,
  userId: number,
  reason?: string
): Promise<void> {
  return request.delete(`/admin/teams/${teamId}/members/${userId}`, {
    params: { reason }
  })
}

/**
 * 管理员批量解散团队
 */
export function batchDissolveTeams(teamIds: number[]): Promise<void> {
  return request.post('/admin/teams/batch-dissolve', teamIds)
}

/**
 * 获取管理员团队统计信息
 */
export function getAdminTeamStatistics(): Promise<{
  totalTeams: number
  activeTeams: number
  averageMemberCount: number
  totalProjects: number
}> {
  return request.get('/admin/teams/statistics').then((res: any) => res.data || res)
}
