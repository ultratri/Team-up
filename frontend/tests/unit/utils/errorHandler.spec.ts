import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { handleApiError, getErrorMessage, ERROR_MESSAGES } from '@/utils/errorHandler'
import { ElMessage } from 'element-plus'
import router from '@/router'

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

// Mock router
vi.mock('@/router', () => ({
  default: {
    push: vi.fn()
  }
}))

describe('Error Handler Utils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock localStorage
    Object.defineProperty(window, 'localStorage', {
      value: {
        removeItem: vi.fn(),
        getItem: vi.fn(),
        setItem: vi.fn()
      },
      writable: true
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('handleApiError', () => {
    it('should handle network error (no response)', () => {
      const error = {
        message: 'Network Error'
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络')
      expect(result).toBe('网络连接失败，请检查网络')
    })

    it('should handle 400 Bad Request', () => {
      const error = {
        response: {
          status: 400,
          data: { message: '请求参数错误' }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('请求参数错误: 请求参数错误')
      expect(result).toBe('请求参数错误: 请求参数错误')
    })

    it('should handle 401 Unauthorized', () => {
      const error = {
        response: {
          status: 401,
          data: { message: '未授权' }
        }
      }

      handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('登录已过期，请重新登录')
      expect(localStorage.removeItem).toHaveBeenCalledWith('token')
      expect(localStorage.removeItem).toHaveBeenCalledWith('user')
      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('should handle 403 Forbidden', () => {
      const error = {
        response: {
          status: 403,
          data: { message: '无权访问' }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('没有权限访问该资源')
      expect(result).toBe('没有权限访问该资源')
    })

    it('should handle 404 Not Found', () => {
      const error = {
        response: {
          status: 404,
          data: { message: '资源不存在' }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('请求的资源不存在')
      expect(result).toBe('请求的资源不存在')
    })

    it('should handle 500 Internal Server Error', () => {
      const error = {
        response: {
          status: 500,
          data: { message: '服务器错误' }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('服务器内部错误，请稍后重试')
      expect(result).toBe('服务器内部错误，请稍后重试')
    })

    it('should handle 502 Bad Gateway', () => {
      const error = {
        response: {
          status: 502,
          data: { message: '网关错误' }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('网关错误，请稍后重试')
      expect(result).toBe('网关错误，请稍后重试')
    })

    it('should handle 503 Service Unavailable', () => {
      const error = {
        response: {
          status: 503,
          data: { message: '服务不可用' }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('服务暂时不可用，请稍后重试')
      expect(result).toBe('服务暂时不可用，请稍后重试')
    })

    it('should handle unknown status code', () => {
      const error = {
        response: {
          status: 418,
          data: { message: "I'm a teapot" }
        }
      }

      handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith("I'm a teapot")
    })

    it('should handle error with specific error code', () => {
      const error = {
        response: {
          status: 400,
          data: {
            code: 40002,
            message: '团队名称长度应在2-50字符之间'
          }
        }
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('团队名称长度应在2-50字符之间')
      expect(result).toBe('团队名称长度应在2-50字符之间')
    })

    it('should handle error without message', () => {
      const error = {
        response: {
          status: 400,
          data: {}
        }
      }

      handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalled()
    })
  })

  describe('getErrorMessage', () => {
    it('should return correct message for error code 40001', () => {
      const message = getErrorMessage(40001)
      expect(message).toBe('团队名称为必填项')
    })

    it('should return correct message for error code 40002', () => {
      const message = getErrorMessage(40002)
      expect(message).toBe('团队名称长度应在2-50字符之间')
    })

    it('should return correct message for error code 40003', () => {
      const message = getErrorMessage(40003)
      expect(message).toBe('团队名称包含非法字符')
    })

    it('should return correct message for error code 40004', () => {
      const message = getErrorMessage(40004)
      expect(message).toBe('团队描述不能超过500字符')
    })

    it('should return correct message for error code 40005', () => {
      const message = getErrorMessage(40005)
      expect(message).toBe('该用户已是团队成员')
    })

    it('should return correct message for error code 40301', () => {
      const message = getErrorMessage(40301)
      expect(message).toBe('您不是该团队成员，无权访问')
    })

    it('should return correct message for error code 40302', () => {
      const message = getErrorMessage(40302)
      expect(message).toBe('只有团队所有者才能删除团队')
    })

    it('should return correct message for error code 40303', () => {
      const message = getErrorMessage(40303)
      expect(message).toBe('无权移除该成员')
    })

    it('should return correct message for error code 40401', () => {
      const message = getErrorMessage(40401)
      expect(message).toBe('团队不存在')
    })

    it('should return correct message for error code 40402', () => {
      const message = getErrorMessage(40402)
      expect(message).toBe('用户不存在')
    })

    it('should return null for unknown error code', () => {
      const message = getErrorMessage(99999)
      expect(message).toBeNull()
    })

    it('should return null for negative error code', () => {
      const message = getErrorMessage(-1)
      expect(message).toBeNull()
    })

    it('should return null for zero error code', () => {
      const message = getErrorMessage(0)
      expect(message).toBeNull()
    })
  })

  describe('ERROR_MESSAGES', () => {
    it('should have all required error codes', () => {
      const requiredCodes = [
        40001, 40002, 40003, 40004, 40005,
        40301, 40302, 40303,
        40401, 40402
      ]

      requiredCodes.forEach(code => {
        expect(ERROR_MESSAGES[code]).toBeDefined()
        expect(typeof ERROR_MESSAGES[code]).toBe('string')
        expect(ERROR_MESSAGES[code].length).toBeGreaterThan(0)
      })
    })

    it('should have exactly 10 error messages', () => {
      const keys = Object.keys(ERROR_MESSAGES)
      expect(keys).toHaveLength(10)
    })

    it('should have all messages in Chinese', () => {
      Object.values(ERROR_MESSAGES).forEach(message => {
        expect(message).toMatch(/[\u4e00-\u9fa5]/)
      })
    })
  })

  describe('Edge cases', () => {
    it('should handle error with null response', () => {
      const error = {
        response: null
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络')
      expect(result).toBe('网络连接失败，请检查网络')
    })

    it('should handle error with undefined response', () => {
      const error = {
        response: undefined
      }

      const result = handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络')
      expect(result).toBe('网络连接失败，请检查网络')
    })

    it('should handle error with null data', () => {
      const error = {
        response: {
          status: 400,
          data: null
        }
      }

      handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should handle error with undefined data', () => {
      const error = {
        response: {
          status: 400,
          data: undefined
        }
      }

      handleApiError(error)

      expect(ElMessage.error).toHaveBeenCalled()
    })
  })
})
