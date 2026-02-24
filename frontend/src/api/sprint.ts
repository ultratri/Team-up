import { request } from '@/utils/request'

export interface Sprint {
  id?: number
  teamId: number
  name: string
  goal: string
  startDate: string
  endDate: string
  status: 'PLANNING' | 'IN_PROGRESS' | 'COMPLETED'
  createdBy: number
  createdAt?: string
  updatedAt?: string
}

export interface SprintVO extends Sprint {
  creatorName?: string
  totalTasks: number
  completedTasks: number
  inProgressTasks: number
}

/**
 * 创建 Sprint
 */
export async function createSprint(data: Sprint): Promise<Sprint> {
  return await request.post('/sprints', data)
}

/**
 * 更新 Sprint
 */
export async function updateSprint(id: number, data: Partial<Sprint>): Promise<Sprint> {
  return await request.put(`/sprints/${id}`, data)
}

/**
 * 删除 Sprint
 */
export async function deleteSprint(id: number): Promise<void> {
  await request.delete(`/sprints/${id}`)
}

/**
 * 获取团队的所有 Sprint
 */
export async function getTeamSprints(teamId: number): Promise<SprintVO[]> {
  return await request.get(`/sprints/team/${teamId}`)
}

/**
 * 获取 Sprint 详情
 */
export async function getSprintDetail(id: number): Promise<SprintVO> {
  return await request.get(`/sprints/${id}`)
}

/**
 * 开始 Sprint
 */
export async function startSprint(id: number): Promise<void> {
  await request.post(`/sprints/${id}/start`)
}

/**
 * 完成 Sprint
 */
export async function completeSprint(id: number): Promise<void> {
  await request.post(`/sprints/${id}/complete`)
}
