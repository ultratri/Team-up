/**
 * 团队管理相关常量
 */

import { TeamStatus, TeamRole } from '@/types/team'

/**
 * 团队状态标签映射
 */
export const TEAM_STATUS_LABELS: Record<string, string> = {
  [TeamStatus.ACTIVE]: '活跃',
  [TeamStatus.ARCHIVED]: '已归档',
  [TeamStatus.DISBANDED]: '已解散'
}

/**
 * 团队状态颜色映射
 */
export const TEAM_STATUS_COLORS: Record<string, string> = {
  [TeamStatus.ACTIVE]: 'success',
  [TeamStatus.ARCHIVED]: 'info',
  [TeamStatus.DISBANDED]: 'danger'
}

/**
 * 团队角色标签映射
 */
export const TEAM_ROLE_LABELS: Record<string, string> = {
  [TeamRole.OWNER]: '所有者',
  [TeamRole.ADMIN]: '管理员',
  [TeamRole.MEMBER]: '成员',
  LEADER: '队长' // 兼容旧角色
}

/**
 * 团队角色颜色映射
 */
export const TEAM_ROLE_COLORS: Record<string, string> = {
  [TeamRole.OWNER]: 'danger',
  [TeamRole.ADMIN]: 'warning',
  [TeamRole.MEMBER]: 'primary',
  LEADER: 'danger' // 兼容旧角色
}

/**
 * 默认分页大小
 */
export const DEFAULT_PAGE_SIZE = 12

/**
 * 团队名称最小长度
 */
export const TEAM_NAME_MIN_LENGTH = 2

/**
 * 团队名称最大长度
 */
export const TEAM_NAME_MAX_LENGTH = 50

/**
 * 团队描述最大长度
 */
export const TEAM_DESCRIPTION_MAX_LENGTH = 500

/**
 * 活动类型标签映射
 */
export const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  task: '任务',
  file: '文件',
  message: '消息',
  member: '成员',
  setting: '设置'
}

/**
 * 活动类型图标映射
 */
export const ACTIVITY_TYPE_ICONS: Record<string, string> = {
  task: 'Document',
  file: 'Folder',
  message: 'ChatDotRound',
  member: 'User',
  setting: 'Setting'
}
