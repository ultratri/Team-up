import type { App } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import router from '@/router'

/**
 * 全局错误处理器
 */
export function setupErrorHandler(app: App) {
  // Vue 错误处理
  app.config.errorHandler = (err: any, _instance, info) => {
    console.error('Vue Error:', err)
    console.error('Error Info:', info)
    
    // 不显示开发环境的错误提示
    if (import.meta.env.DEV) {
      return
    }
    
    ElMessage.error({
      message: '应用发生错误，请刷新页面重试',
      duration: 3000,
      showClose: true
    })
  }

  // 全局警告处理
  app.config.warnHandler = (msg, _instance, trace) => {
    console.warn('Vue Warning:', msg)
    console.warn('Trace:', trace)
  }

  // 未捕获的Promise错误
  window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled Promise Rejection:', event.reason)
    
    if (import.meta.env.DEV) {
      return
    }
    
    ElMessage.error({
      message: '网络请求失败，请检查网络连接',
      duration: 3000,
      showClose: true
    })
    
    event.preventDefault()
  })

  // 全局错误捕获
  window.addEventListener('error', (event) => {
    console.error('Global Error:', event.error)
    
    if (import.meta.env.DEV) {
      return
    }
    
    event.preventDefault()
  })
}

/**
 * 处理 API 错误
 */
export function handleApiError(error: any, retryCallback?: () => Promise<any>): string {
  if (!error.response) {
    const message = '网络连接失败，请检查网络'
    
    // 如果提供了重试回调，显示带重试按钮的通知
    if (retryCallback) {
      showNetworkErrorWithRetry(message, retryCallback)
    } else {
      ElMessage.error({
        message,
        duration: 3000,
        showClose: true
      })
    }
    return message
  }

  const status = error.response.status
  const data = error.response.data
  const message = data?.message || '请求失败'
  const errorCode = data?.code

  // 处理特定错误码
  if (errorCode) {
    const errorMessage = getErrorMessage(errorCode)
    if (errorMessage) {
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      return errorMessage
    }
  }

  // 处理 HTTP 状态码
  let errorMessage = message
  switch (status) {
    case 400:
      errorMessage = `请求参数错误: ${message}`
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      break
    case 401:
      errorMessage = '登录已过期，请重新登录'
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      // 清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 跳转到登录页
      router.push('/login')
      break
    case 403:
      errorMessage = '没有权限访问该资源'
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      break
    case 404:
      errorMessage = '请求的资源不存在'
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      break
    case 500:
      errorMessage = '服务器内部错误，请稍后重试'
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      break
    case 502:
      errorMessage = '网关错误，请稍后重试'
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      break
    case 503:
      errorMessage = '服务暂时不可用，请稍后重试'
      ElMessage.error({
        message: errorMessage,
        duration: 3000,
        showClose: true
      })
      break
    default:
      ElMessage.error({
        message: message || '请求失败',
        duration: 3000,
        showClose: true
      })
  }

  return errorMessage
}

/**
 * 显示网络错误并提供重试选项
 */
export function showNetworkErrorWithRetry(message: string, retryCallback: () => Promise<any>) {
  ElNotification({
    title: '网络错误',
    message: message,
    type: 'error',
    duration: 0, // 不自动关闭
    showClose: true,
    dangerouslyUseHTMLString: true,
    customClass: 'network-error-notification',
    position: 'top-right',
    onClick: async () => {
      try {
        await retryCallback()
        ElMessage.success('重试成功')
      } catch (error) {
        ElMessage.error('重试失败，请稍后再试')
      }
    }
  })
}

/**
 * 显示带重试按钮的错误对话框
 */
export function showErrorWithRetry(message: string, retryCallback: () => Promise<any>) {
  return ElMessageBox.confirm(
    message,
    '操作失败',
    {
      confirmButtonText: '重试',
      cancelButtonText: '取消',
      type: 'error',
      distinguishCancelAndClose: true
    }
  ).then(async () => {
    try {
      await retryCallback()
      ElMessage.success('操作成功')
      return true
    } catch (error) {
      ElMessage.error('操作失败，请稍后再试')
      return false
    }
  }).catch(() => {
    // 用户取消
    return false
  })
}

/**
 * 团队管理相关错误码映射
 */
export const ERROR_MESSAGES: Record<number, string> = {
  40001: '团队名称为必填项',
  40002: '团队名称长度应在2-50字符之间',
  40003: '团队名称包含非法字符',
  40004: '团队描述不能超过500字符',
  40005: '该用户已是团队成员',
  40301: '您不是该团队成员，无权访问',
  40302: '只有团队所有者才能删除团队',
  40303: '无权移除该成员',
  40401: '团队不存在',
  40402: '用户不存在'
}

/**
 * 根据错误码获取错误消息
 */
export function getErrorMessage(code: number): string | null {
  return ERROR_MESSAGES[code] || null
}

/**
 * 显示确认对话框
 */
export function showConfirm(message: string, title: string = '提示') {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
}

/**
 * 显示成功提示
 */
export function showSuccess(message: string) {
  ElMessage.success(message)
}

/**
 * 显示错误提示
 */
export function showError(message: string) {
  ElMessage.error(message)
}

/**
 * 显示警告提示
 */
export function showWarning(message: string) {
  ElMessage.warning(message)
}

/**
 * 显示信息提示
 */
export function showInfo(message: string) {
  ElMessage.info(message)
}
