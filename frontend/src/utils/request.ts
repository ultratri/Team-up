import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import { useAuthStore } from '../store/auth'
import router from '../router'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8080`

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000, // 增加超时时间
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求计数器，用于显示全局 loading
let requestCount = 0

// 存储失败的请求配置，用于重试
interface FailedRequest {
  config: AxiosRequestConfig
  timestamp: number
}

const failedRequests: Map<string, FailedRequest> = new Map()

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    requestCount++
    
    // 添加token到请求头 - 直接从localStorage获取，确保可靠性
    const authStore = useAuthStore()
    const token = authStore.token || localStorage.getItem('token') || sessionStorage.getItem('token')
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('🔑 Request with token:', config.url, 'Token:', token.substring(0, 20) + '...')
    } else {
      console.warn('⚠️ Request without token:', config.url, 'authStore.token:', authStore.token, 'localStorage:', localStorage.getItem('token'))
    }
    
    return config
  },
  (error) => {
    requestCount--
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    requestCount--
    
    // 统一处理响应数据
    const res = response.data
    
    // 如果后端返回的 code 不是 200，认为是错误
    if (res.code && res.code !== 200) {
      // 根据不同的错误码显示不同的消息
      const errorMessage = res.message || '请求失败'
      
      // 检查是否是登录接口，如果是则不在拦截器中显示错误，让组件自己处理
      const isLoginRequest = response.config?.url?.includes('/auth/login')
      
      if (!isLoginRequest) {
        // 如果有详细的错误信息（如验证错误），显示详细信息
        if (res.errors && Array.isArray(res.errors)) {
          const errorDetails = res.errors.map((err: any) => 
            `${err.field}: ${err.message}`
          ).join('\n')
          
          ElMessage.error({
            message: `${errorMessage}\n${errorDetails}`,
            duration: 5000,
            showClose: true,
            dangerouslyUseHTMLString: true
          })
        } else {
          ElMessage.error({
            message: errorMessage,
            duration: 3000,
            showClose: true
          })
        }
      }
      
      return Promise.reject(new Error(errorMessage))
    }
    
    // 返回完整的响应对象（包含 code, message, data），让调用方自己处理
    return res
  },
  (error) => {
    requestCount--
    
    // 处理错误响应
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      const message = data?.message || '请求失败'
      
      // 检查是否是登录接口，如果是则不在拦截器中显示错误，让组件自己处理
      const isLoginRequest = error.config?.url?.includes('/auth/login')

      switch (status) {
        case 400:
          // 处理参数错误 - 显示详细的验证错误
          if (!isLoginRequest) {
            if (data?.errors && Array.isArray(data.errors)) {
              const errorDetails = data.errors.map((err: any) => 
                `• ${err.field || '字段'}: ${err.message}`
              ).join('<br/>')
              
              ElMessage.error({
                message: `<strong>请求参数错误：</strong><br/>${errorDetails}`,
                duration: 5000,
                showClose: true,
                dangerouslyUseHTMLString: true
              })
            } else {
              ElMessage.error({
                message: message || '请求参数错误，请检查输入',
                duration: 3000,
                showClose: true
              })
            }
          }
          break
        case 401:
          // 处理未认证错误 - 跳转到登录页
          ElMessage.error({
            message: '登录已过期，请重新登录',
            duration: 3000,
            showClose: true
          })
          // 清除本地存储
          const authStore = useAuthStore()
          authStore.logout()
          // 延迟跳转，确保消息显示
          setTimeout(() => {
            router.push({ 
              name: 'Login',
              query: { redirect: router.currentRoute.value.fullPath }
            })
          }, 500)
          break
        case 403:
          // 处理权限不足错误 - 显示警告消息
          ElMessage.warning({
            message: message || '权限不足，无法执行此操作',
            duration: 4000,
            showClose: true
          })
          break
        case 404:
          // 处理资源不存在错误
          ElMessage.error({
            message: message || '请求的资源不存在',
            duration: 3000,
            showClose: true
          })
          break
        case 500:
          // 处理服务器错误
          ElMessage.error({
            message: '服务器错误，请稍后重试',
            duration: 3000,
            showClose: true
          })
          // 记录错误日志
          console.error('Server error:', {
            url: error.config?.url,
            method: error.config?.method,
            status,
            data
          })
          break
        case 502:
          // 处理网关错误
          ElMessage.error({
            message: '网关错误，请稍后重试',
            duration: 3000,
            showClose: true
          })
          break
        case 503:
          // 处理服务不可用错误
          ElMessage.error({
            message: '服务暂时不可用，请稍后重试',
            duration: 3000,
            showClose: true
          })
          break
        default:
          ElMessage.error({
            message: message || `请求失败 (${status})`,
            duration: 3000,
            showClose: true
          })
      }
      
      // 返回包含错误信息的对象，便于组件处理
      return Promise.reject({
        status,
        message,
        errors: data?.errors,
        data
      })
    } else if (error.code === 'ECONNABORTED') {
      // 处理请求超时
      ElMessage.error({
        message: '请求超时，请检查网络连接后重试',
        duration: 3000,
        showClose: true
      })
    } else if (error.message === 'Network Error') {
      // 处理网络错误 - 显示带重试按钮的通知
      const requestKey = `${error.config?.method}_${error.config?.url}`
      failedRequests.set(requestKey, {
        config: error.config,
        timestamp: Date.now()
      })
      
      ElNotification({
        title: '网络错误',
        message: '网络连接失败，请检查您的网络连接',
        type: 'error',
        duration: 0, // 不自动关闭
        showClose: true,
        position: 'top-right',
        customClass: 'network-error-notification',
        dangerouslyUseHTMLString: true,
        onClick: async () => {
          const failedRequest = failedRequests.get(requestKey)
          if (failedRequest) {
            try {
              await service.request(failedRequest.config)
              ElMessage.success('重试成功')
              failedRequests.delete(requestKey)
            } catch (retryError) {
              ElMessage.error('重试失败，请稍后再试')
            }
          }
        }
      })
    } else {
      // 其他错误
      console.error('Request error:', error)
      ElMessage.error({
        message: error.message || '请求失败，请稍后重试',
        duration: 3000,
        showClose: true
      })
    }

    return Promise.reject(error)
  }
)

export default service

// 封装常用方法
export const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  },
}

