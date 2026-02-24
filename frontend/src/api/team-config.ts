import { request } from '@/utils/request'
import type { TeamCustomConfig } from '@/types/team-config'

/**
 * 获取团队自定义配置
 */
export function getTeamCustomConfig(teamId: number) {
  return request.get<TeamCustomConfig>(`/teams/${teamId}/custom-config`)
}

/**
 * 更新团队自定义配置
 */
export function updateTeamCustomConfig(teamId: number, config: TeamCustomConfig) {
  return request.put(`/teams/${teamId}/custom-config`, config)
}
