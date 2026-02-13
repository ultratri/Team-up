// 认证状态调试工具

export function debugAuthState() {
  const localToken = localStorage.getItem('token')
  const sessionToken = sessionStorage.getItem('token')
  const localUser = localStorage.getItem('user')
  const sessionUser = sessionStorage.getItem('user')
  
  console.group('🔍 认证状态调试')
  console.log('localStorage.token:', localToken ? `存在(${localToken.substring(0, 30)}...)` : '不存在')
  console.log('sessionStorage.token:', sessionToken ? `存在(${sessionToken.substring(0, 30)}...)` : '不存在')
  console.log('localStorage.user:', localUser ? '存在' : '不存在')
  console.log('sessionStorage.user:', sessionUser ? '存在' : '不存在')
  
  if (localUser) {
    try {
      const user = JSON.parse(localUser)
      console.log('localStorage.user内容:', {
        id: user.id,
        username: user.username,
        userCode: user.userCode,
        roles: user.roles
      })
    } catch (e) {
      console.error('localStorage.user解析失败:', e)
    }
  }
  
  if (sessionUser) {
    try {
      const user = JSON.parse(sessionUser)
      console.log('sessionStorage.user内容:', {
        id: user.id,
        username: user.username,
        userCode: user.userCode,
        roles: user.roles
      })
    } catch (e) {
      console.error('sessionStorage.user解析失败:', e)
    }
  }
  
  // 检查Pinia store
  try {
    const { useAuthStore } = require('@/store/auth')
    const authStore = useAuthStore()
    console.log('authStore状态:', {
      hasToken: !!authStore.token,
      tokenPreview: authStore.token?.substring(0, 30),
      hasUser: !!authStore.user,
      username: authStore.user?.username,
      isAuthenticated: authStore.isAuthenticated
    })
  } catch (e) {
    console.error('无法访问authStore:', e)
  }
  
  console.groupEnd()
}

// 在window上暴露调试函数
if (typeof window !== 'undefined') {
  (window as any).debugAuth = debugAuthState
}
