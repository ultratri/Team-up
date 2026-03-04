import { request } from '@utils/request'

export interface Tag {
  id: number
  name: string
  category: string
  description?: string
  usageCount?: number
  isOfficial?: boolean
  status?: string
}

// 获取所有技能标签
export function getSkillTags() {
  return request.get<Tag[]>('/tags/skills')
}

// 获取所有兴趣标签
export function getInterestTags() {
  return request.get<Tag[]>('/tags/interests')
}

// 获取所有性格标签
export function getPersonalityTags() {
  return request.get<Tag[]>('/tags/personalities')
}

// 获取所有项目类型标签
export function getProjectTypeTags() {
  return request.get<Tag[]>('/tags/project-types')
}

// 获取热门标签
export function getPopularTags(category?: string, limit: number = 20) {
  return request.get<Tag[]>('/tags/popular', {
    params: { category, limit }
  })
}
