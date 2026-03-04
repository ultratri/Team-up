import { request } from '@/utils/request'

/**
 * 项目履历相关接口
 */

export interface ProjectHistoryItem {
  id: number
  projectId: number
  projectTitle: string
  projectType: string
  projectDescription: string
  role: string  // LEADER/MEMBER
  joinedAt: string
  completedAt: string
  durationDays: number
  avgTechScore: number | null
  avgCollaborationScore: number | null
  avgTaskCompletionScore: number | null
  evaluationCount: number
  avgScore: number | null
  isVerified: boolean
  verificationSource: string
}

export interface ExperienceScore {
  totalScore: number
  isVerified: boolean
  completedProjects: number
  leaderProjects: number
  projectTypeDiversity: number
  avgProjectScore: number | null
  breakdown: {
    project_count: number
    quality: number
    leadership: number
    diversity: number
  }
}

/**
 * 获取我的项目履历
 */
export function getMyProjectHistory(onlyCompleted: boolean = true) {
  return request.get<ProjectHistoryItem[]>('/user/project-history/my', {
    params: { onlyCompleted }
  })
}

/**
 * 获取指定用户的项目履历
 */
export function getUserProjectHistory(userId: number, onlyCompleted: boolean = true) {
  return request.get<ProjectHistoryItem[]>(`/user/project-history/user/${userId}`, {
    params: { onlyCompleted }
  })
}

/**
 * 获取我的经验分数
 */
export function getMyExperienceScore() {
  return request.get<ExperienceScore>('/user/project-history/my/experience-score')
}

/**
 * 获取指定用户的经验分数
 */
export function getUserExperienceScore(userId: number) {
  return request.get<ExperienceScore>(`/user/project-history/user/${userId}/experience-score`)
}
