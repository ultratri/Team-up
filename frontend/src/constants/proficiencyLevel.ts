/**
 * 技能熟练度等级常量
 * 统一定义系统中所有技能等级相关的常量
 * 
 * @author TeamUp
 * @since 2026-03-03
 */

/**
 * 技能等级枚举
 */
export enum ProficiencyLevel {
  /** 入门 - 刚开始学习，了解基础概念 */
  BEGINNER = 'BEGINNER',
  /** 熟练 - 能够独立完成常规任务 */
  INTERMEDIATE = 'INTERMEDIATE',
  /** 高级 - 能够处理复杂问题，有深入理解 */
  ADVANCED = 'ADVANCED',
  /** 精通 - 专家水平，能够指导他人 */
  EXPERT = 'EXPERT'
}

/**
 * 技能等级显示文本映射
 */
export const PROFICIENCY_LEVEL_TEXT: Record<ProficiencyLevel, string> = {
  [ProficiencyLevel.BEGINNER]: '入门',
  [ProficiencyLevel.INTERMEDIATE]: '熟练',
  [ProficiencyLevel.ADVANCED]: '高级',
  [ProficiencyLevel.EXPERT]: '精通'
}

/**
 * 技能等级颜色映射（用于标签和图例）
 */
export const PROFICIENCY_LEVEL_COLOR: Record<ProficiencyLevel, string> = {
  [ProficiencyLevel.BEGINNER]: '#cbd5e1',      // 浅灰色
  [ProficiencyLevel.INTERMEDIATE]: '#94a3b8',  // 灰色
  [ProficiencyLevel.ADVANCED]: 'var(--accent-color)',  // 主题色
  [ProficiencyLevel.EXPERT]: '#10b981'         // 绿色
}

/**
 * 技能等级标签类型映射（Element Plus Tag 组件）
 * BEGINNER: info (灰色) - 入门级别
 * INTERMEDIATE: '' (默认色) - 熟练级别
 * ADVANCED: warning (橙色) - 高级级别
 * EXPERT: success (绿色) - 精通级别
 */
export const PROFICIENCY_LEVEL_TAG_TYPE: Record<ProficiencyLevel, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  [ProficiencyLevel.BEGINNER]: 'info',
  [ProficiencyLevel.INTERMEDIATE]: '',
  [ProficiencyLevel.ADVANCED]: 'warning',
  [ProficiencyLevel.EXPERT]: 'success'
}

/**
 * 技能等级权重（用于匹配算法）
 */
export const PROFICIENCY_LEVEL_WEIGHT: Record<ProficiencyLevel, number> = {
  [ProficiencyLevel.BEGINNER]: 0.4,
  [ProficiencyLevel.INTERMEDIATE]: 0.7,
  [ProficiencyLevel.ADVANCED]: 0.85,
  [ProficiencyLevel.EXPERT]: 1.0
}

/**
 * 获取技能等级显示文本
 */
export function getProficiencyLevelText(level: string): string {
  return PROFICIENCY_LEVEL_TEXT[level as ProficiencyLevel] || level
}

/**
 * 获取技能等级颜色
 */
export function getProficiencyLevelColor(level: string): string {
  return PROFICIENCY_LEVEL_COLOR[level as ProficiencyLevel] || '#94a3b8'
}

/**
 * 获取技能等级标签类型
 */
export function getProficiencyLevelTagType(level: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  return PROFICIENCY_LEVEL_TAG_TYPE[level as ProficiencyLevel] || 'info'
}

/**
 * 获取技能等级权重
 */
export function getProficiencyLevelWeight(level: string): number {
  return PROFICIENCY_LEVEL_WEIGHT[level as ProficiencyLevel] || 0.5
}

/**
 * 所有技能等级选项（用于下拉框）
 */
export const PROFICIENCY_LEVEL_OPTIONS = [
  { label: '入门', value: ProficiencyLevel.BEGINNER },
  { label: '熟练', value: ProficiencyLevel.INTERMEDIATE },
  { label: '高级', value: ProficiencyLevel.ADVANCED },
  { label: '精通', value: ProficiencyLevel.EXPERT }
]
