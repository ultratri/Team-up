export type UserRole =
  | 'STUDENT'
  | 'PROJECT_CREATOR'
  | 'TEAM_LEADER'
  | 'MENTOR'
  | 'PLATFORM_ADMIN'

export interface User {
  id: number
  studentId: string
  username: string
  email: string
  phone?: string
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED'
  roles: UserRole[]
  profile?: UserProfile
  createdAt: string
  updatedAt: string
}

export interface UserProfile {
  id: number
  userId: number
  realName?: string
  department?: string
  major?: string
  grade?: number
  avatarUrl?: string
  wechat?: string
  qq?: string
  bio?: string
  projectExperience?: string
}

export interface UserSkill {
  id: number
  userId: number
  skillName: string
  skillCategory?: string
  proficiencyLevel: 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT'
  isCustom: boolean
}

export interface UserCredit {
  id: number
  userId: number
  totalCredit: number
  creditLevel: 'NEWBIE' | 'RELIABLE' | 'EXCELLENT' | 'OUTSTANDING'
}

