import { request } from '@/utils/request'

export interface DailyStandup {
  id?: number
  teamId: number
  sprintId?: number | null
  userId: number
  standupDate: string
  yesterdayWork: string
  todayPlan: string
  blockers?: string
  createdAt?: string
  updatedAt?: string
}

export interface DailyStandupVO extends DailyStandup {
  userName?: string
  userAvatar?: string
  sprintName?: string
}

/**
 * 提交站会记录
 */
export async function submitStandup(data: DailyStandup): Promise<DailyStandup> {
  return await request.post('/standups', data)
}

/**
 * 更新站会记录
 */
export async function updateStandup(id: number, data: Partial<DailyStandup>): Promise<DailyStandup> {
  return await request.put(`/standups/${id}`, data)
}

/**
 * 获取团队某日的站会记录
 */
export async function getTeamStandups(teamId: number, date: string): Promise<DailyStandupVO[]> {
  return await request.get(`/standups/team/${teamId}`, { params: { date } })
}

/**
 * 获取用户的站会记录
 */
export async function getUserStandups(userId: number, startDate: string, endDate: string): Promise<DailyStandupVO[]> {
  return await request.get(`/standups/user/${userId}`, { 
    params: { startDate, endDate } 
  })
}
