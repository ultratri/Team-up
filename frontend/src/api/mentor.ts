import { request } from '@/utils/request'

/**
 * 导师申请信息
 */
export interface MentorApplication {
  id: number
  applicant: {
    id: number
    username: string
    realName: string
    department: string
    major: string
    completedProjects: number
    reputationScore: number
  }
  reason: string
  status: string
  reviewerName?: string
  reviewedAt?: string
  rejectReason?: string
  createdAt: string
}

/**
 * 导师信息
 */
export interface MentorInfo {
  id: number
  username: string
  realName: string
  department: string
  major: string
  totalMentees: number
  activeMentees: number
  completedMentees: number
  successfulMentees: number
  averageMenteeScore: number
  totalRewardPoints: number
  rating: number
}

/**
 * 导师排行
 */
export interface MentorRanking {
  rank: number
  mentorId: number
  mentorName: string
  department: string
  successfulMentees: number
  averageMenteeScore: number
  totalRewardPoints: number
  rating: number
}

/**
 * 导师详情
 */
export interface MentorDetail {
  id: number
  username: string
  realName: string
  department: string
  major: string
  email?: string
  phone?: string
  bio?: string
  totalMentees: number
  activeMentees: number
  completedMentees: number
  successfulMentees: number
  averageMenteeScore: number
  totalRewardPoints: number
  rating: number
  becameMentorAt?: string
  applicationReason?: string
}

/**
 * 审核导师申请DTO
 */
export interface ApproveApplicationDTO {
  applicationId: number
  approved: boolean
  rejectReason?: string
}

/**
 * 获取待审核的导师申请列表
 */
export async function getMentorApplications(page = 1, size = 20) {
  const res = await request.get<any>('/mentor-applications', {
    params: { page, size }
  })
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      const data = res.data as any
      return {
        records: data.records || [],
        total: data.total || 0,
        page: data.current || page,
        size: data.size || size
      }
    }
    if ('records' in res) {
      return {
        records: (res as any).records || [],
        total: (res as any).total || 0,
        page: (res as any).current || page,
        size: (res as any).size || size
      }
    }
  }
  
  return { records: [], total: 0, page, size }
}

/**
 * 审核导师申请
 */
export async function approveMentorApplication(dto: ApproveApplicationDTO) {
  const res = await request.post<any>(`/mentor-applications/${dto.applicationId}/review`, {
    approved: dto.approved,
    rejectReason: dto.rejectReason
  })
  
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '审核失败')
  }
}

/**
 * 获取导师列表
 */
export async function getMentorList(page = 1, size = 20) {
  const res = await request.get<any>('/admin/mentors', {
    params: { page, size }
  })
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      const data = res.data as any
      return {
        records: data.records || [],
        total: data.total || 0,
        page: data.current || page,
        size: data.size || size
      }
    }
    if ('records' in res) {
      return {
        records: (res as any).records || [],
        total: (res as any).total || 0,
        page: (res as any).current || page,
        size: (res as any).size || size
      }
    }
  }
  
  return { records: [], total: 0, page, size }
}

/**
 * 获取导师绩效排行榜
 */
export async function getMentorRanking(limit = 10): Promise<MentorRanking[]> {
  const res = await request.get<any>('/admin/mentors/ranking', {
    params: { limit }
  })
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return Array.isArray(res.data) ? res.data : []
    }
    if (Array.isArray(res)) {
      return res
    }
  }
  
  return []
}

/**
 * 撤销导师资格
 */
export async function revokeMentor(mentorId: number) {
  const res = await request.post<any>(`/admin/mentors/${mentorId}/revoke`)
  
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '撤销失败')
  }
}

/**
 * 获取导师详情
 */
export async function getMentorDetail(mentorId: number): Promise<MentorDetail | null> {
  const res = await request.get<any>(`/admin/mentors/${mentorId}`)
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return res.data
    }
    if ('id' in res) {
      return res as MentorDetail
    }
  }
  
  return null
}

/**
 * 更新所有导师评分
 */
export async function updateAllMentorRatings(): Promise<string> {
  const res = await request.post<any>('/admin/mentors/update-ratings')
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200) {
      return res.data || '评分更新成功'
    }
    throw new Error(res.message || '更新失败')
  }
  
  return '评分更新成功'
}

/**
 * 更新单个导师评分
 */
export async function updateMentorRating(mentorId: number): Promise<string> {
  const res = await request.post<any>(`/admin/mentors/${mentorId}/update-rating`)
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200) {
      return res.data || '评分更新成功'
    }
    throw new Error(res.message || '更新失败')
  }
  
  return '评分更新成功'
}

// ==================== 导师广场相关接口 ====================

/**
 * 导师卡片信息（用于导师广场）
 */
export interface MentorCard {
  id: number
  username: string
  realName: string
  avatar?: string
  department: string
  major: string
  bio?: string
  projectExperience?: string
  guidanceExperience?: string
  totalMentees: number
  activeMentees: number
  successfulMentees?: number
  totalRewardPoints?: number
  rating: number
  available?: boolean
  avatarUrl?: string
}

/**
 * 获取导师列表（学员可见）
 */
export async function getMentorPlaza(params?: {
  page?: number
  size?: number
  department?: string
  keyword?: string
}) {
  const res = await request.get<any>('/mentor-relationships/mentors', { params })
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      const data = res.data as any
      return {
        records: data.records || [],
        total: data.total || 0,
        page: data.current || params?.page || 1,
        size: data.size || params?.size || 20
      }
    }
    if ('records' in res) {
      return {
        records: (res as any).records || [],
        total: (res as any).total || 0,
        page: (res as any).current || params?.page || 1,
        size: (res as any).size || params?.size || 20
      }
    }
  }
  
  return { records: [], total: 0, page: params?.page || 1, size: params?.size || 20 }
}

// ==================== 团队导师申请相关接口 ====================

/**
 * 团队导师申请信息
 */
export interface TeamMentorApplication {
  id: number
  teamId: number
  teamName?: string
  competitionId?: number
  competitionName?: string
  mentorId: number
  requestedBy: number
  requesterName?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reason?: string
  decidedAt?: string
  createdAt: string
}

/**
 * 获取导师的申请列表
 */
export async function getMentorApplicationsList(params?: {
  page?: number
  size?: number
  status?: string
}) {
  const res = await request.get<any>('/mentor/applications', { params })
  
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
    if ('records' in res) {
      return {
        records: (res as any).records || [],
        total: (res as any).total || 0,
        page: (res as any).current || params?.page || 1,
        size: (res as any).size || params?.size || 10
      }
    }
  }
  
  return { records: [], total: 0, page: params?.page || 1, size: params?.size || 10 }
}

/**
 * 接受导师申请
 */
export async function acceptMentorApplication(applicationId: number) {
  const res = await request.post<any>(`/mentor/applications/${applicationId}/accept`)
  
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '接受申请失败')
  }
}

/**
 * 拒绝导师申请
 */
export async function rejectMentorApplication(applicationId: number, reason?: string) {
  const res = await request.post<any>(`/mentor/applications/${applicationId}/reject`, null, {
    params: { reason }
  })
  
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '拒绝申请失败')
  }
}
