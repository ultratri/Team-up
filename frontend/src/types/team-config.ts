/**
 * 团队自定义配置相关类型定义
 */

// 快捷入口
export interface Shortcut {
  id: string
  name: string
  url: string
  icon: string
  color: string
  order: number
}

// 分组链接
export interface GroupLink {
  id: string
  name: string
  url: string
  icon: string
  description?: string
}

// 工具分组
export interface ToolGroup {
  id: string
  name: string
  order: number
  links: GroupLink[]
}

// 规范链接
export interface Guideline {
  id: string
  name: string
  url: string
  category: string
}

// 检查清单项
export interface ChecklistItem {
  id: string
  title: string
  description?: string
  order: number
  required: boolean
}

// 团队自定义配置
export interface TeamCustomConfig {
  id?: number
  teamId: number
  shortcuts: Shortcut[]
  groups: ToolGroup[]
  teamAnnouncement?: string
  teamGuidelines: Guideline[]
  onboardingChecklist: ChecklistItem[]
  shortcutsEditPermission: 'leader' | 'all'
  announcementEditPermission: 'leader' | 'all'
  canEditShortcuts?: boolean
  canEditAnnouncement?: boolean
}

// 内置图标选项
export const BUILTIN_ICONS = [
  { value: 'github', label: 'GitHub', color: '#24292e' },
  { value: 'gitee', label: 'Gitee', color: '#c71d23' },
  { value: 'gitlab', label: 'GitLab', color: '#fc6d26' },
  { value: 'feishu', label: '飞书', color: '#00d6b9' },
  { value: 'yuque', label: '语雀', color: '#25b864' },
  { value: 'notion', label: 'Notion', color: '#000000' },
  { value: 'jira', label: 'Jira', color: '#0052cc' },
  { value: 'trello', label: 'Trello', color: '#0079bf' },
  { value: 'apifox', label: 'Apifox', color: '#ff6a39' },
  { value: 'swagger', label: 'Swagger', color: '#85ea2d' },
  { value: 'figma', label: 'Figma', color: '#a259ff' },
  { value: 'lanhu', label: '蓝湖', color: '#3370ff' },
  { value: 'meeting', label: '会议', color: '#2d8cf0' },
  { value: 'document', label: '文档', color: '#409eff' },
  { value: 'link', label: '链接', color: '#909399' }
]
