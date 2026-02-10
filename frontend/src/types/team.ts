/**
 * 团队状态枚举
 */
export const TeamStatus = {
  ACTIVE: 'ACTIVE',
  ARCHIVED: 'ARCHIVED',
  DISBANDED: 'DISBANDED'
} as const

export type TeamStatus = (typeof TeamStatus)[keyof typeof TeamStatus]

/**
 * 团队成员角色枚举
 */
export const TeamRole = {
  OWNER: 'OWNER',
  ADMIN: 'ADMIN',
  MEMBER: 'MEMBER'
} as const

export type TeamRole = (typeof TeamRole)[keyof typeof TeamRole]

/**
 * 团队类型枚举
 */
export const TeamType = {
  PROJECT: 'PROJECT',
  COMPETITION: 'COMPETITION'
} as const

export type TeamType = (typeof TeamType)[keyof typeof TeamType]

/**
 * 团队实体
 */
export interface Team {
  id: number
  name: string
  description?: string
  avatar?: string
  projectId?: number
  creatorId: number
  status: TeamStatus
  createdAt: string | Date
  updatedAt: string | Date
  memberCount?: number
  creator?: {
    id: number
    name: string
    avatar?: string
  }
  project?: {
    id: number
    title: string
  }
  // 比赛相关字段
  type?: TeamType
  competitionId?: number
  maxMembers?: number
  mentorId?: number
  competition?: {
    id: number
    name: string
  }
  mentor?: {
    id: number
    name: string
    avatar?: string
  }
  // 兼容旧字段
  teamName?: string
  leaderId?: number
}

/**
 * 团队成员实体
 */
export interface TeamMember {
  id: number
  teamId: number
  userId: number
  role: TeamRole | 'LEADER' | 'MEMBER'
  joinedAt: string | Date
  updatedAt: string | Date
  username: string
  avatar?: string
  email?: string
  skills?: string[]
  department?: string
  // 兼容旧字段
  nickname?: string
}

/**
 * 入队申请状态枚举
 */
export const TeamJoinApplicationStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  WITHDRAWN: 'WITHDRAWN'
} as const

export type TeamJoinApplicationStatus = (typeof TeamJoinApplicationStatus)[keyof typeof TeamJoinApplicationStatus]

/**
 * 入队申请实体
 */
export interface TeamJoinApplication {
  id: number
  teamId: number
  competitionId?: number
  applicantId: number
  reason?: string
  status: TeamJoinApplicationStatus | string
  reviewedBy?: number
  reviewComment?: string
  appliedAt: string | Date
  reviewedAt?: string | Date
  // 关联信息（可选）
  team?: {
    id: number
    name: string
  }
  competition?: {
    id: number
    name: string
  }
  applicant?: {
    id: number
    name: string
    avatar?: string
  }
}

/**
 * 团队统计数据
 */
export interface TeamStatistics {
  taskCompletionRate: number
  activeDays: number
  messageCount: number
  fileCount: number
  totalTasks: number
  completedTasks: number
  memberCount: number
}

/**
 * 创建团队请求
 */
export interface TeamCreateRequest {
  name: string
  description?: string
  avatar?: string
  projectId?: number
  // 比赛相关字段
  type?: TeamType
  competitionId?: number
  maxMembers?: number
  // 兼容旧字段
  teamName?: string
  leaderId?: number
}

/**
 * 团队列表查询参数
 */
export interface TeamListQuery {
  userId: number
  keyword?: string
  status?: TeamStatus
  page?: number
  size?: number
}

/**
 * 团队列表响应
 */
export interface TeamListResponse {
  total: number
  page: number
  size: number
  records: Team[]
}

/**
 * 团队详情响应
 */
export interface TeamDetailResponse extends Team {
  members: TeamMember[]
  statistics: TeamStatistics
  recentActivities: TeamActivity[]
  currentUserRole: TeamRole
}

/**
 * 团队活动记录
 */
export interface TeamActivity {
  id: number
  teamId?: number
  userId: number
  username: string
  avatarUrl?: string
  avatar?: string
  activityType?: 'task' | 'file' | 'message' | 'member' | 'setting'
  type?: 'task' | 'file' | 'message' | 'member' | 'setting'
  action?: string
  detail: string
  createdAt: string | Date
}

/**
 * 任务负责人
 */
export interface TaskAssignee {
  id: number
  taskId: number
  userId: number
  userName: string
  avatar?: string
  assignedAt: string
}

/**
 * 任务实体
 */
export interface Task {
  id: number
  teamId: number
  title: string
  description: string
  status: 'TODO' | 'DOING' | 'REVIEW' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  deadline?: string
  createdBy: number
  createdAt: string
  updatedAt: string
  assignees?: TaskAssignee[]
  commentCount?: number
  attachmentCount?: number
}
