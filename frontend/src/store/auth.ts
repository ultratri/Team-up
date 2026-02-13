import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, UserRole } from '../types/user'

export const useAuthStore = defineStore('auth', () => {
  // 从 localStorage 恢复用户与角色
  let initialUser: User | null = null
  let initialRoles: UserRole[] = []

  const getStoredValue = (key: string) =>
    localStorage.getItem(key) ?? sessionStorage.getItem(key)

  const storedUser = getStoredValue('user')
  if (storedUser) {
    try {
      const parsed = JSON.parse(storedUser) as User
      initialUser = parsed
      initialRoles = parsed.roles || []
    } catch (e) {
      console.error('❌ AuthStore初始化: 用户信息解析失败', e)
      localStorage.removeItem('user')
      sessionStorage.removeItem('user')
    }
  } else {
    console.warn('⚠️ AuthStore初始化: 没有存储的用户信息')
  }

  // State
  const user = ref<User | null>(initialUser)
  const token = ref<string | null>(getStoredValue('token'))
  const roles = ref<UserRole[]>(initialRoles)

  // Getters
  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const currentUser = computed(() => user.value)
  const userRoles = computed(() => roles.value)

  // Actions
  function setToken(newToken: string, remember: boolean = true) {
    token.value = newToken
    localStorage.removeItem('token')
    sessionStorage.removeItem('token')
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('token', newToken)
  }

  function setUser(newUser: User, remember: boolean = true) {
    user.value = newUser
    roles.value = newUser.roles || []
    localStorage.removeItem('user')
    sessionStorage.removeItem('user')
    const storage = remember ? localStorage : sessionStorage
    storage.setItem('user', JSON.stringify(newUser))
  }

  function logout() {
    // 立即清除状态，不等待异步操作
    user.value = null
    token.value = null
    roles.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    
    // 异步断开 WebSocket 连接，不阻塞退出
    setTimeout(() => {
      try {
        const { websocketService } = require('@/utils/websocket')
        websocketService.disconnect()
      } catch (e) {
        // 忽略错误
      }
    }, 0)
  }

  function hasRole(requiredRoles: string[]): boolean {
    if (!roles.value || roles.value.length === 0) return false
    return requiredRoles.some((role) => roles.value.includes(role as UserRole))
  }

  function hasAnyRole(requiredRoles: string[]): boolean {
    return hasRole(requiredRoles)
  }

  function hasAllRoles(requiredRoles: string[]): boolean {
    if (!roles.value || roles.value.length === 0) return false
    return requiredRoles.every((role) => roles.value.includes(role as UserRole))
  }

  /**
   * 更新当前用户信息（用于profile更新后刷新）
   */
  async function refreshUserInfo() {
    if (!user.value?.id) return
    
    try {
      // 重新获取用户信息
      const { getUserProfile } = await import('@/api/user')
      const profile = await getUserProfile(user.value.id)
      
      if (user.value && profile) {
        // 更新用户的profile
        user.value.profile = profile
        
        // 更新localStorage
        const storage = localStorage.getItem('user') ? localStorage : sessionStorage
        storage.setItem('user', JSON.stringify(user.value))
      }
    } catch (error) {
      console.error('❌ 刷新用户信息失败', error)
    }
  }

  return {
    // State
    user,
    token,
    roles,
    // Getters
    isAuthenticated,
    currentUser,
    userRoles,
    // Actions
    setToken,
    setUser,
    logout,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    refreshUserInfo,
  }
})

