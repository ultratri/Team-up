/**
 * 认证调试工具
 */

import { useAuthStore } from '@/store/auth'

export interface AuthDebugInfo {
  hasToken: boolean
  tokenPreview: string
  tokenLocation: 'localStorage' | 'sessionStorage' | 'none'
  hasUser: boolean
  username: string
  roles: string[]
  isAuthenticated: boolean
  tokenExpired: boolean
  tokenPayload: any
}

/**
 * 获取认证调试信息
 */
export function getAuthDebugInfo(): AuthDebugInfo {
  const authStore = useAuthStore()
  
  // 检查token存储位置
  const localToken = localStorage.getItem('token')
  const sessionToken = sessionStorage.getItem('token')
  const storeToken = authStore.token
  
  const token = storeToken || localToken || sessionToken
  const tokenLocation = localToken ? 'localStorage' : sessionToken ? 'sessionStorage' : 'none'
  
  // 解析token payload
  let tokenPayload: any = null
  let tokenExpired = false
  
  if (token) {
    try {
      const parts = token.split('.')
      if (parts.length === 3) {
        const payload = JSON.parse(atob(parts[1]))
        tokenPayload = payload
        
        // 检查是否过期
        if (payload.exp) {
          const expTime = payload.exp * 1000 // 转换为毫秒
          tokenExpired = Date.now() > expTime
        }
      }
    } catch (e) {
      console.error('解析token失败:', e)
    }
  }
  
  return {
    hasToken: !!token,
    tokenPreview: token ? token.substring(0, 30) + '...' : '',
    tokenLocation,
    hasUser: !!authStore.user,
    username: authStore.user?.username || '',
    roles: authStore.roles || [],
    isAuthenticated: authStore.isAuthenticated,
    tokenExpired,
    tokenPayload
  }
}

/**
 * 打印认证调试信息到控制台
 */
export function printAuthDebugInfo() {
  const info = getAuthDebugInfo()
  
  console.group('🔐 认证调试信息')
  console.log('Token存在:', info.hasToken ? '✅' : '❌')
  console.log('Token位置:', info.tokenLocation)
  console.log('Token预览:', info.tokenPreview)
  console.log('Token过期:', info.tokenExpired ? '❌ 是' : '✅ 否')
  console.log('用户存在:', info.hasUser ? '✅' : '❌')
  console.log('用户名:', info.username)
  console.log('角色列表:', info.roles)
  console.log('是否认证:', info.isAuthenticated ? '✅' : '❌')
  
  if (info.tokenPayload) {
    console.log('Token载荷:', info.tokenPayload)
    if (info.tokenPayload.exp) {
      const expDate = new Date(info.tokenPayload.exp * 1000)
      console.log('Token过期时间:', expDate.toLocaleString('zh-CN'))
      const remainingTime = info.tokenPayload.exp * 1000 - Date.now()
      if (remainingTime > 0) {
        console.log('剩余有效时间:', Math.floor(remainingTime / 1000 / 60), '分钟')
      }
    }
  }
  
  console.groupEnd()
  
  return info
}

/**
 * 检查是否有指定角色
 */
export function checkRole(requiredRole: string): boolean {
  const info = getAuthDebugInfo()
  const hasRole = info.roles.includes(requiredRole)
  
  console.log(`🔍 检查角色 ${requiredRole}:`, hasRole ? '✅ 有' : '❌ 无')
  console.log('当前角色:', info.roles)
  
  return hasRole
}

/**
 * 修复认证状态（尝试从存储中恢复）
 */
export function fixAuthState() {
  const authStore = useAuthStore()
  
  console.group('🔧 尝试修复认证状态')
  
  // 检查token
  const localToken = localStorage.getItem('token')
  const sessionToken = sessionStorage.getItem('token')
  const token = localToken || sessionToken
  
  if (token && !authStore.token) {
    console.log('发现存储的token，恢复到store')
    authStore.token = token
  }
  
  // 检查用户信息
  const localUser = localStorage.getItem('user')
  const sessionUser = sessionStorage.getItem('user')
  const userStr = localUser || sessionUser
  
  if (userStr && !authStore.user) {
    try {
      const user = JSON.parse(userStr)
      console.log('发现存储的用户信息，恢复到store')
      authStore.user = user
      authStore.roles = user.roles || []
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  
  console.log('修复后状态:', getAuthDebugInfo())
  console.groupEnd()
}
