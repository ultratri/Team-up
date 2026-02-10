export type ProjectType = 'COMPETITION' | 'RESEARCH' | 'STARTUP' | 'OPENSOURCE' | 'OTHER'

export type ProjectStatus =
  | 'DRAFT'
  | 'RECRUITING'
  | 'IN_PROGRESS'
  | 'PENDING_REVIEW'
  | 'COMPLETED'
  | 'ARCHIVED'

export interface Project {
  id: number
  creatorId: number
  title: string
  projectType: ProjectType
  description?: string
  requirements?: string
  teamSize: number
  currentMembers: number
  expectedDuration?: number
  weeklyHours?: number
  expectedOutcome?: string
  status: ProjectStatus
  isRecommended: boolean
  views: number
  startDate?: string
  endDate?: string
  createdAt: string
  updatedAt: string
}

export interface ProjectSkillRequirement {
  id: number
  projectId: number
  skillName: string
  isRequired: boolean
  expectedLevel?: 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT'
}

export interface ProjectMember {
  id: number
  projectId: number
  userId: number
  role?: string
  contribution?: string
  joinedAt: string
  leftAt?: string
  status: 'ACTIVE' | 'LEFT' | 'REMOVED'
}

export interface ProjectApplication {
  id: number
  projectId: number
  applicantId: number
  applicationReason?: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'
  reviewedBy?: number
  reviewComment?: string
  appliedAt: string
  reviewedAt?: string

  // VO 字段（后端 ApplicationVO 返回）
  projectTitle?: string
  applicantName?: string
  applicantAvatar?: string
  applicantSkills?: string[]
}

export interface ProjectComment {
  id: number
  projectId: number
  parentId?: number
  replyToUserId?: number
  userId: number
  content: string
  likeCount?: number
  createdAt: string

  // 用户信息
  username?: string
  nickname?: string
  avatar?: string

  // 回复目标用户信息
  replyToUsername?: string
  replyToNickname?: string

  // 子评论
  replies?: ProjectComment[]
}

export interface ProjectFile {
  id: number
  projectId: number
  uploaderId: number
  fileName: string
  originalFileName?: string
  fileUrl: string
  fileType: 'DOCUMENT' | 'IMAGE' | 'CODE' | 'OTHER'
  category: string
  fileSize: number
  mimeType?: string
  description?: string
  createdAt: string
  
  // 上传者信息
  uploaderName?: string
  uploaderAvatar?: string
}

export interface ProjectMilestone {
  id: number
  projectId: number
  title: string
  status: 'PLANNED' | 'IN_PROGRESS' | 'DONE'
  plannedAt?: string
  actualAt?: string
  ownerId?: number
  ownerName?: string
  remark?: string
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}
