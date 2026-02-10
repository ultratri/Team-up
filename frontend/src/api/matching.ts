import axios from 'axios'

const matchingService = axios.create({
  baseURL: 'http://localhost:5000/api/matching',
  timeout: 10000,
})

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

// 计算匹配度
export function calculateMatch(data: MatchRequest) {
  return matchingService.post<MatchResult[]>('/calculate', data)
}

// 获取匹配服务健康状态
export function getMatchingHealth() {
  return matchingService.get('/health')
}

