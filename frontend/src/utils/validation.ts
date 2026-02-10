/**
 * 表单验证工具
 */

/**
 * 验证结果接口
 */
export interface ValidationResult {
  valid: boolean
  message?: string
}

/**
 * 验证团队名称
 * @param name 团队名称
 * @returns 验证结果
 */
export function validateTeamName(name: string): ValidationResult {
  // 检查是否为字符串类型
  if (typeof name !== 'string') {
    return {
      valid: false,
      message: '请输入团队名称'
    }
  }

  if (!name || name.trim().length === 0) {
    return {
      valid: false,
      message: '请输入团队名称'
    }
  }

  const trimmedName = name.trim()

  if (trimmedName.length < 2) {
    return {
      valid: false,
      message: '团队名称至少需要 2 个字符'
    }
  }

  if (trimmedName.length > 50) {
    return {
      valid: false,
      message: '团队名称不能超过 50 个字符'
    }
  }

  // 只允许中文、英文、数字和下划线
  const pattern = /^[\u4e00-\u9fa5a-zA-Z0-9_]+$/
  if (!pattern.test(trimmedName)) {
    return {
      valid: false,
      message: '团队名称只能包含中文、英文、数字和下划线'
    }
  }

  return { valid: true }
}

/**
 * 验证团队描述
 * @param description 团队描述
 * @returns 验证结果
 */
export function validateTeamDescription(description?: string): ValidationResult {
  if (!description) {
    return { valid: true }
  }

  if (description.length > 500) {
    return {
      valid: false,
      message: '团队描述不能超过 500 个字符'
    }
  }

  return { valid: true }
}

/**
 * Element Plus 表单验证规则
 */

/**
 * 团队名称验证规则
 */
export const teamNameRules = [
  {
    required: true,
    message: '请输入团队名称',
    trigger: 'blur'
  },
  {
    min: 2,
    max: 50,
    message: '长度在 2 到 50 个字符',
    trigger: 'blur'
  },
  {
    pattern: /^[\u4e00-\u9fa5a-zA-Z0-9_]+$/,
    message: '只能包含中文、英文、数字和下划线',
    trigger: 'blur'
  }
]

/**
 * 团队描述验证规则
 */
export const teamDescriptionRules = [
  {
    max: 500,
    message: '描述不能超过 500 个字符',
    trigger: 'blur'
  }
]
