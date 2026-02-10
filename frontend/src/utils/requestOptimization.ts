/**
 * API 请求优化工具
 * 包含防抖、节流、请求缓存等功能
 */

/**
 * 防抖函数
 * @param fn 要防抖的函数
 * @param delay 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let timeoutId: number | null = null

  return function (this: any, ...args: Parameters<T>) {
    if (timeoutId !== null) {
      clearTimeout(timeoutId)
    }

    timeoutId = window.setTimeout(() => {
      fn.apply(this, args)
      timeoutId = null
    }, delay)
  }
}

/**
 * 节流函数
 * @param fn 要节流的函数
 * @param delay 延迟时间（毫秒）
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let lastCall = 0

  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now()

    if (now - lastCall >= delay) {
      lastCall = now
      fn.apply(this, args)
    }
  }
}

/**
 * 请求缓存接口
 */
interface CacheEntry<T> {
  data: T
  timestamp: number
  expiresAt: number
}

/**
 * 请求缓存管理器
 */
class RequestCache {
  private cache: Map<string, CacheEntry<any>> = new Map()
  private defaultTTL: number = 5 * 60 * 1000 // 默认 5 分钟

  /**
   * 生成缓存键
   */
  private generateKey(url: string, params?: any): string {
    const paramStr = params ? JSON.stringify(params) : ''
    return `${url}:${paramStr}`
  }

  /**
   * 获取缓存
   */
  get<T>(url: string, params?: any): T | null {
    const key = this.generateKey(url, params)
    const entry = this.cache.get(key)

    if (!entry) {
      return null
    }

    // 检查是否过期
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key)
      return null
    }

    return entry.data as T
  }

  /**
   * 设置缓存
   */
  set<T>(url: string, data: T, params?: any, ttl?: number): void {
    const key = this.generateKey(url, params)
    const now = Date.now()
    const expiresAt = now + (ttl || this.defaultTTL)

    this.cache.set(key, {
      data,
      timestamp: now,
      expiresAt,
    })
  }

  /**
   * 删除缓存
   */
  delete(url: string, params?: any): void {
    const key = this.generateKey(url, params)
    this.cache.delete(key)
  }

  /**
   * 清空所有缓存
   */
  clear(): void {
    this.cache.clear()
  }

  /**
   * 清理过期缓存
   */
  cleanup(): void {
    const now = Date.now()
    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiresAt) {
        this.cache.delete(key)
      }
    }
  }

  /**
   * 使缓存失效（通过 URL 前缀）
   */
  invalidateByPrefix(prefix: string): void {
    for (const key of this.cache.keys()) {
      if (key.startsWith(prefix)) {
        this.cache.delete(key)
      }
    }
  }
}

// 导出单例
export const requestCache = new RequestCache()

// 定期清理过期缓存（每 10 分钟）
if (typeof window !== 'undefined') {
  setInterval(() => {
    requestCache.cleanup()
  }, 10 * 60 * 1000)
}

/**
 * 带缓存的请求包装器
 * @param requestFn 请求函数
 * @param cacheKey 缓存键
 * @param params 请求参数
 * @param ttl 缓存时间（毫秒）
 * @returns Promise<T>
 */
export async function cachedRequest<T>(
  requestFn: () => Promise<T>,
  cacheKey: string,
  params?: any,
  ttl?: number
): Promise<T> {
  // 尝试从缓存获取
  const cached = requestCache.get<T>(cacheKey, params)
  if (cached !== null) {
    return cached
  }

  // 执行请求
  const data = await requestFn()

  // 存入缓存
  requestCache.set(cacheKey, data, params, ttl)

  return data
}

/**
 * 并行请求工具
 * @param requests 请求数组
 * @returns Promise<T[]>
 */
export async function parallelRequests<T>(
  requests: Array<() => Promise<T>>
): Promise<T[]> {
  return Promise.all(requests.map((req) => req()))
}

/**
 * 串行请求工具（按顺序执行）
 * @param requests 请求数组
 * @returns Promise<T[]>
 */
export async function serialRequests<T>(
  requests: Array<() => Promise<T>>
): Promise<T[]> {
  const results: T[] = []

  for (const request of requests) {
    const result = await request()
    results.push(result)
  }

  return results
}

/**
 * 请求重试工具
 * @param requestFn 请求函数
 * @param maxRetries 最大重试次数
 * @param delay 重试延迟（毫秒）
 * @returns Promise<T>
 */
export async function retryRequest<T>(
  requestFn: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: any

  for (let i = 0; i <= maxRetries; i++) {
    try {
      return await requestFn()
    } catch (error) {
      lastError = error

      // 如果是最后一次重试，直接抛出错误
      if (i === maxRetries) {
        throw error
      }

      // 等待后重试
      await new Promise((resolve) => setTimeout(resolve, delay * (i + 1)))
    }
  }

  throw lastError
}

/**
 * 请求取消令牌
 */
export class CancelToken {
  private cancelled = false
  private reason: string | null = null

  cancel(reason?: string): void {
    this.cancelled = true
    this.reason = reason || 'Request cancelled'
  }

  isCancelled(): boolean {
    return this.cancelled
  }

  getReason(): string | null {
    return this.reason
  }

  throwIfCancelled(): void {
    if (this.cancelled) {
      throw new Error(this.reason || 'Request cancelled')
    }
  }
}

/**
 * 带取消功能的请求包装器
 * @param requestFn 请求函数
 * @param cancelToken 取消令牌
 * @returns Promise<T>
 */
export async function cancellableRequest<T>(
  requestFn: () => Promise<T>,
  cancelToken: CancelToken
): Promise<T> {
  cancelToken.throwIfCancelled()

  const result = await requestFn()

  cancelToken.throwIfCancelled()

  return result
}
