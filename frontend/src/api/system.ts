import { request } from '@utils/request'

export interface DepartmentMajor {
  id: number
  department: string
  major: string
  sortOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface DepartmentMajorTree {
  [department: string]: string[]
}

/**
 * 获取院系专业树（公共接口，只返回启用的数据）
 */
export async function getDepartmentMajorTree(): Promise<DepartmentMajorTree> {
  const res = await request.get<any>('/system/department-major/tree')
  
  // 处理响应
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return res.data
    }
    // 如果已经是数据对象，直接返回
    if (!('code' in res)) {
      return res
    }
  }
  
  return {}
}

/**
 * 获取所有院系专业（管理员接口，包含禁用的数据）
 */
export async function getDepartmentMajorList(): Promise<DepartmentMajor[]> {
  const res = await request.get<any>('/system/department-major/admin/list')
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return Array.isArray(res.data) ? res.data : []
    }
    if (Array.isArray(res)) {
      return res
    }
  }
  
  return []
}

/**
 * 创建院系专业（管理员）
 */
export async function createDepartmentMajor(data: Partial<DepartmentMajor>): Promise<DepartmentMajor> {
  const res = await request.post<any>('/system/department-major/admin', data)
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return res.data
    }
    throw new Error(res.message || '创建失败')
  }
  
  return res
}

/**
 * 更新院系专业（管理员）
 */
export async function updateDepartmentMajor(id: number, data: Partial<DepartmentMajor>): Promise<DepartmentMajor> {
  const res = await request.put<any>(`/system/department-major/admin/${id}`, data)
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return res.data
    }
    throw new Error(res.message || '更新失败')
  }
  
  return res
}

/**
 * 删除院系专业（管理员）
 */
export async function deleteDepartmentMajor(id: number): Promise<void> {
  const res = await request.delete<any>(`/system/department-major/admin/${id}`)
  
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '删除失败')
  }
}

// ==================== 系统设置相关 ====================

export interface SystemSettings {
  basic: BasicSettings
  notification: NotificationSettings
  security: SecuritySettings
}

export interface BasicSettings {
  siteName: string
  siteDescription: string
  maintenanceMode: boolean
  allowRegistration: boolean
  requireRegistrationApproval: boolean
}

export interface NotificationSettings {
  emailEnabled: boolean
  smtpHost: string
  smtpPort: number
  senderEmail: string
}

export interface SecuritySettings {
  minPasswordLength: number
  passwordRequirements: string[]
  loginLockEnabled: boolean
  maxLoginAttempts: number
  lockoutDuration: number
}

/**
 * 获取所有系统设置
 */
export async function getSystemSettings(): Promise<SystemSettings> {
  const res = await request.get<any>('/system/settings')
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return res.data
    }
    if (!('code' in res)) {
      return res
    }
  }
  
  throw new Error('获取系统设置失败')
}

/**
 * 获取指定分组的设置
 */
export async function getSettingsByGroup(group: string): Promise<any> {
  const res = await request.get<any>(`/system/settings/${group}`)
  
  if (res && typeof res === 'object') {
    if ('code' in res && res.code === 200 && res.data) {
      return res.data
    }
    if (!('code' in res)) {
      return res
    }
  }
  
  return {}
}

/**
 * 保存指定分组的设置
 */
export async function saveSettings(group: string, settings: any): Promise<void> {
  const res = await request.put<any>(`/system/settings/${group}`, settings)
  
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200) return
    throw new Error(res.message || '保存失败')
  }
}
