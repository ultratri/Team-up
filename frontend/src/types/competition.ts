/**
 * 比赛状态枚举
 */
export const CompetitionStatus = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  ARCHIVED: 'ARCHIVED'
} as const

export type CompetitionStatus = (typeof CompetitionStatus)[keyof typeof CompetitionStatus]

/**
 * 比赛级别枚举
 */
export const CompetitionLevel = {
  SCHOOL: 'SCHOOL',
  PROVINCE: 'PROVINCE',
  NATIONAL: 'NATIONAL',
  INTERNATIONAL: 'INTERNATIONAL'
} as const

export type CompetitionLevel = (typeof CompetitionLevel)[keyof typeof CompetitionLevel]

/**
 * 比赛范围枚举
 */
export const CompetitionScope = {
  SCHOOL: 'SCHOOL',
  PROVINCE: 'PROVINCE',
  NATIONAL: 'NATIONAL',
  INTERNATIONAL: 'INTERNATIONAL'
} as const

export type CompetitionScope = (typeof CompetitionScope)[keyof typeof CompetitionScope]

/**
 * 比赛类型枚举
 */
export const CompetitionType = {
  PROGRAMMING: 'PROGRAMMING',
  DESIGN: 'DESIGN',
  INNOVATION: 'INNOVATION',
  RESEARCH: 'RESEARCH',
  OTHER: 'OTHER'
} as const

export type CompetitionType = (typeof CompetitionType)[keyof typeof CompetitionType]

/**
 * 比赛附件
 */
export interface CompetitionAttachment {
  name: string
  url: string
}

/**
 * 比赛实体
 */
export interface Competition {
  id: number
  name: string
  organizer: string
  level: CompetitionLevel
  scope: CompetitionScope
  /**
   * JSON string: { departments?: string[], majors?: string[], grades?: number[] }
   * 为空/不传表示全体
   */
  audience?: string
  type: CompetitionType
  signupStartAt: string | Date
  signupEndAt: string | Date
  startAt: string | Date
  endAt: string | Date
  maxTeamMembers?: number
  minTeamMembers?: number
  /**
   * 同一比赛每个用户可参加的队伍上限，null/undefined 表示不限制
   */
  maxTeamsPerUser?: number | null
  /**
   * 是否启用报名/参赛资格限制（按 audience 校验）
   */
  eligibilityEnabled?: boolean
  requireMentor?: boolean
  status: CompetitionStatus
  description?: string
  attachments?: CompetitionAttachment[] | string // JSON string or parsed array
  createdBy: number
  createdAt: string | Date
  updatedAt: string | Date
  // 统计字段（可选）
  teamCount?: number
  participantCount?: number
}

/**
 * 创建比赛请求
 */
export interface CompetitionCreateRequest {
  name: string
  organizer: string
  level: CompetitionLevel
  scope: CompetitionScope
  audience?: string
  type: CompetitionType
  signupStartAt: string | Date
  signupEndAt: string | Date
  startAt: string | Date
  endAt: string | Date
  maxTeamMembers?: number
  minTeamMembers?: number
  requireMentor?: boolean
  description?: string
  attachments?: CompetitionAttachment[]
}

/**
 * 更新比赛请求
 */
export interface CompetitionUpdateRequest extends Partial<CompetitionCreateRequest> {
  id: number
}

/**
 * 比赛列表查询参数
 */
export interface CompetitionListQuery {
  page?: number
  size?: number
  status?: CompetitionStatus | string
  level?: CompetitionLevel | string
  scope?: CompetitionScope | string
  type?: CompetitionType | string
  keyword?: string
}

/**
 * 比赛列表响应
 */
export interface CompetitionListResponse {
  records: Competition[]
  total: number
  page: number
  size: number
}

/**
 * 导师申请状态枚举
 */
export const MentorApplicationStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
} as const

export type MentorApplicationStatus = (typeof MentorApplicationStatus)[keyof typeof MentorApplicationStatus]

/**
 * 导师申请实体
 */
export interface TeamMentorApplication {
  id: number
  teamId: number
  competitionId: number
  mentorId: number
  requestedBy: number
  status: MentorApplicationStatus
  reason?: string
  createdAt: string | Date
  decidedAt?: string | Date
  // 关联信息（可选）
  team?: {
    id: number
    name: string
  }
  competition?: {
    id: number
    name: string
  }
  mentor?: {
    id: number
    name: string
    avatar?: string
  }
  requester?: {
    id: number
    name: string
    avatar?: string
  }
}

/**
 * 创建导师申请请求
 */
export interface MentorApplicationCreateRequest {
  teamId: number
  mentorId: number
  reason?: string
}

/**
 * 导师申请列表查询参数
 */
export interface MentorApplicationListQuery {
  page?: number
  size?: number
  status?: MentorApplicationStatus | string
  mentorId?: number
  teamId?: number
}
