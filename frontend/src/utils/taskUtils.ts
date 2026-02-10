/**
 * 任务数据处理工具函数
 */

import type { Task } from '@/types/team'

/**
 * 格式化任务日期为 ISO 8601 格式
 * @param date 日期对象或字符串
 * @returns ISO 8601 格式的日期字符串
 */
export function formatTaskDate(date: Date | string | null | undefined): string | null {
  if (!date) return null
  
  try {
    const dateObj = typeof date === 'string' ? new Date(date) : date
    
    // 检查日期是否有效
    if (isNaN(dateObj.getTime())) {
      return null
    }
    
    return dateObj.toISOString()
  } catch (error) {
    console.error('Error formatting task date:', error)
    return null
  }
}

/**
 * 解析 ISO 8601 格式的日期字符串
 * @param dateString ISO 8601 格式的日期字符串
 * @returns Date 对象或 null
 */
export function parseTaskDate(dateString: string | null | undefined): Date | null {
  if (!dateString) return null
  
  try {
    const date = new Date(dateString)
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      return null
    }
    
    return date
  } catch (error) {
    console.error('Error parsing task date:', error)
    return null
  }
}

/**
 * 验证任务标题
 * @param title 任务标题
 * @returns 是否有效
 */
export function validateTaskTitle(title: string | null | undefined): boolean {
  if (!title || typeof title !== 'string') {
    return false
  }
  
  const trimmed = title.trim()
  
  // 标题必须在 1-255 字符之间
  return trimmed.length > 0 && trimmed.length <= 255
}

/**
 * 验证任务描述
 * @param description 任务描述
 * @returns 是否有效
 */
export function validateTaskDescription(description: string | null | undefined): boolean {
  // 描述是可选的
  if (!description) return true
  
  if (typeof description !== 'string') {
    return false
  }
  
  // 描述最大 5000 字符
  return description.length <= 5000
}

/**
 * 验证任务状态
 * @param status 任务状态
 * @returns 是否有效
 */
export function validateTaskStatus(status: string | null | undefined): boolean {
  const validStatuses = ['TODO', 'DOING', 'REVIEW', 'DONE']
  return status ? validStatuses.includes(status) : false
}

/**
 * 验证任务优先级
 * @param priority 任务优先级
 * @returns 是否有效
 */
export function validateTaskPriority(priority: string | null | undefined): boolean {
  const validPriorities = ['LOW', 'MEDIUM', 'HIGH']
  return priority ? validPriorities.includes(priority) : false
}

/**
 * 标准化任务数据（处理空值）
 * @param task 原始任务数据
 * @returns 标准化后的任务数据
 */
export function normalizeTask(task: any): Task {
  if (!task) return task
  
  return {
    ...task,
    // 确保必填字段存在
    id: task.id,
    teamId: task.teamId,
    title: task.title || '',
    description: task.description || null,
    status: task.status || 'TODO',
    priority: task.priority || 'MEDIUM',
    deadline: task.deadline || null,
    createdBy: task.createdBy,
    createdAt: task.createdAt || new Date().toISOString(),
    updatedAt: task.updatedAt || new Date().toISOString(),
    // 可选字段
    assignees: task.assignees || [],
    commentCount: task.commentCount || 0,
    attachmentCount: task.attachmentCount || 0
  }
}

/**
 * 批量标准化任务数据
 * @param tasks 任务数组
 * @returns 标准化后的任务数组
 */
export function normalizeTasks(tasks: any[]): Task[] {
  if (!Array.isArray(tasks)) return []
  return tasks.map(normalizeTask)
}

/**
 * 检查任务是否逾期
 * @param task 任务对象
 * @returns 是否逾期
 */
export function isTaskOverdue(task: Task): boolean {
  if (!task.deadline || task.status === 'DONE') {
    return false
  }
  
  const deadline = parseTaskDate(task.deadline)
  if (!deadline) return false
  
  return deadline < new Date()
}

/**
 * 格式化任务截止日期显示
 * @param deadline 截止日期
 * @returns 格式化的日期字符串
 */
export function formatTaskDeadline(deadline: string | Date | null | undefined): string {
  if (!deadline) return '无截止日期'
  
  const date = parseTaskDate(typeof deadline === 'string' ? deadline : deadline.toISOString())
  if (!date) return '无效日期'
  
  const now = new Date()
  const diffTime = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays < 0) {
    return `已逾期 ${Math.abs(diffDays)} 天`
  } else if (diffDays === 0) {
    return '今天截止'
  } else if (diffDays === 1) {
    return '明天截止'
  } else if (diffDays <= 7) {
    return `${diffDays} 天后截止`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}
