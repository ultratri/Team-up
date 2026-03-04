import { request } from '@utils/request'
import { normalizeCandidateMatchItem, type CandidateMatchItem } from '@utils/fieldNormalizer'

/**
 * 智能组队推荐：为当前用户推荐可组队的同学
 * @deprecated 独立的智能组队推荐页面已移除，此接口保留供未来在项目流程中使用
 * 未来用途：
 * 1. 在项目详情页推荐适合一起申请的队友
 * 2. 在创建项目时推荐适合邀请的队友
 */
export async function getRecommendedTeammates(limit = 20): Promise<CandidateMatchItem[]> {
  const list = await request.get<any[]>('/matching/recommend-teammates', {
    params: { limit }
  })
  return (list || []).map(normalizeCandidateMatchItem)
}

/**
 * @deprecated 仅用于调试与兼容，业务接口请统一走 `api/project.ts`（Java网关）
 */

export interface MatchRequest {
  project_id: number
  project: any
  candidates: any[]
}

export interface MatchResult {
  user_id: number
  username: string
  match_score: number
  skill_match: number
  credit_level: string
}

// 调试用途：经 Java 网关转发匹配计算
export function calculateMatch(data: MatchRequest) {
  return request.post<MatchResult[]>('/matching/calculate', data)
}

// 调试用途：经 Java 网关检查匹配服务健康状态
export function getMatchingHealth() {
  return request.get('/matching/health')
}
