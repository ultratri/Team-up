/**
 * 字段标准化工具
 * 用于统一处理前后端字段名称不匹配的问题
 */

import type { Team, TeamMember, TeamActivity } from '@/types/team'
import type { ProjectFile } from '@/types/project'

/**
 * 标准化团队数据
 */
export function normalizeTeam(team: any): Team {
  if (!team) return team
  
  return {
    ...team,
    // 团队名称：后端 teamName -> 前端 name
    name: team.name || team.teamName,
    // 创建者ID：后端 leaderId -> 前端 creatorId
    creatorId: team.creatorId || team.leaderId,
    // 状态：如果后端没有返回，默认为 ACTIVE
    status: team.status || 'ACTIVE',
    // 时间字段：确保存在
    createdAt: team.createdAt || new Date().toISOString(),
    updatedAt: team.updatedAt || new Date().toISOString()
  }
}

/**
 * 标准化团队成员数据
 */
export function normalizeTeamMember(member: any): TeamMember {
  if (!member) return member
  
  return {
    ...member,
    // 头像：后端可能返回 avatar 或 avatarUrl
    avatar: member.avatar || member.avatarUrl,
    // 用户名：确保存在
    username: member.username || member.nickname || '未知用户',
    // 时间字段
    joinedAt: member.joinedAt || new Date().toISOString(),
    updatedAt: member.updatedAt || new Date().toISOString()
  }
}

/**
 * 标准化团队活动数据
 */
export function normalizeTeamActivity(activity: any): TeamActivity {
  if (!activity) return activity
  
  return {
    ...activity,
    // 头像：后端 avatarUrl -> 前端 avatar
    avatar: activity.avatar || activity.avatarUrl,
    // 活动类型：确保字段名一致
    type: activity.type || activity.activityType,
    // 时间字段
    createdAt: activity.createdAt || new Date().toISOString()
  }
}

/**
 * 标准化项目文件数据
 */
export function normalizeProjectFile(file: any): ProjectFile {
  if (!file) return file
  
  return {
    ...file,
    // 上传者头像：后端 uploaderAvatar -> 前端 uploaderAvatar（已匹配）
    uploaderAvatar: file.uploaderAvatar || file.avatar,
    // 文件大小：确保是数字类型
    fileSize: typeof file.fileSize === 'number' ? file.fileSize : parseInt(file.fileSize || '0'),
    // 时间字段
    createdAt: file.createdAt || new Date().toISOString()
  }
}

/**
 * 批量标准化团队数据
 */
export function normalizeTeams(teams: any[]): Team[] {
  if (!Array.isArray(teams)) return []
  return teams.map(normalizeTeam)
}

/**
 * 批量标准化团队成员数据
 */
export function normalizeTeamMembers(members: any[]): TeamMember[] {
  if (!Array.isArray(members)) return []
  return members.map(normalizeTeamMember)
}

/**
 * 批量标准化团队活动数据
 */
export function normalizeTeamActivities(activities: any[]): TeamActivity[] {
  if (!Array.isArray(activities)) return []
  return activities.map(normalizeTeamActivity)
}

/**
 * 批量标准化项目文件数据
 */
export function normalizeProjectFiles(files: any[]): ProjectFile[] {
  if (!Array.isArray(files)) return []
  return files.map(normalizeProjectFile)
}

/**
 * 通用字段映射函数
 * @param data 原始数据
 * @param fieldMap 字段映射表 { 前端字段名: 后端字段名 }
 */
export function mapFields<T = any>(data: any, fieldMap: Record<string, string | string[]>): T {
  if (!data) return data
  
  const result = { ...data }
  
  for (const [frontendField, backendField] of Object.entries(fieldMap)) {
    // 如果后端字段是数组，尝试多个可能的字段名
    if (Array.isArray(backendField)) {
      for (const field of backendField) {
        if (data[field] !== undefined) {
          result[frontendField] = data[field]
          break
        }
      }
    } else {
      // 单个字段映射
      if (data[backendField] !== undefined) {
        result[frontendField] = data[backendField]
      }
    }
  }
  
  return result as T
}

/**
 * 批量字段映射
 */
export function mapFieldsArray<T = any>(dataArray: any[], fieldMap: Record<string, string | string[]>): T[] {
  if (!Array.isArray(dataArray)) return []
  return dataArray.map(data => mapFields<T>(data, fieldMap))
}

/**
 * 常用字段映射配置
 */
export const FIELD_MAPS = {
  // 团队相关
  team: {
    name: ['name', 'teamName'],
    creatorId: ['creatorId', 'leaderId'],
    avatar: ['avatar', 'avatarUrl']
  },
  
  // 用户相关
  user: {
    avatar: ['avatar', 'avatarUrl'],
    nickname: ['nickname', 'realName', 'username']
  },
  
  // 活动相关
  activity: {
    type: ['type', 'activityType'],
    avatar: ['avatar', 'avatarUrl']
  }
} as const

/**
 * 深度标准化对象（递归处理嵌套对象）
 */
export function deepNormalize(data: any, normalizers: Record<string, (item: any) => any>): any {
  if (!data) return data
  
  if (Array.isArray(data)) {
    return data.map(item => deepNormalize(item, normalizers))
  }
  
  if (typeof data === 'object') {
    const result: any = {}
    
    for (const [key, value] of Object.entries(data)) {
      // 如果有对应的标准化函数，使用它
      if (normalizers[key]) {
        result[key] = normalizers[key](value)
      } else if (typeof value === 'object') {
        // 递归处理嵌套对象
        result[key] = deepNormalize(value, normalizers)
      } else {
        result[key] = value
      }
    }
    
    return result
  }
  
  return data
}

/**
 * 标准化分页响应
 */
export function normalizePaginatedResponse<T>(
  response: any,
  itemNormalizer: (item: any) => T
): { records: T[]; total: number; page?: number; size?: number } {
  if (!response) {
    return { records: [], total: 0 }
  }
  
  const records = Array.isArray(response.records) 
    ? response.records.map(itemNormalizer)
    : []
  
  return {
    records,
    total: response.total || 0,
    page: response.page,
    size: response.size
  }
}
