import { describe, it, expect, vi, beforeEach } from 'vitest'
import { RequestDeduplicator, type RequestKey } from '@/utils/RequestDeduplicator'

describe('RequestDeduplicator', () => {
  let deduplicator: RequestDeduplicator

  beforeEach(() => {
    deduplicator = new RequestDeduplicator()
    vi.clearAllMocks()
  })

  describe('dedupe', () => {
    it('should execute fetcher for first request', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')
      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result).toBe('data')
    })

    it('should share promise for identical simultaneous requests', async () => {
      let callCount = 0
      const fetcher = vi.fn().mockImplementation(async () => {
        callCount++
        await new Promise(resolve => setTimeout(resolve, 50))
        return 'data'
      })

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      // Make 5 identical requests simultaneously
      const requests = Array(5).fill(null).map(() =>
        deduplicator.dedupe(key, fetcher)
      )

      const results = await Promise.all(requests)

      // Should only call fetcher once
      expect(callCount).toBe(1)
      expect(fetcher).toHaveBeenCalledTimes(1)

      // All results should be the same
      results.forEach(result => {
        expect(result).toBe('data')
      })
    })

    it('should clear request from map after completion', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')
      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      await deduplicator.dedupe(key, fetcher)

      // Map should be empty after request completes
      expect(deduplicator.size).toBe(0)
    })

    it('should allow new request after previous completes', async () => {
      const fetcher = vi.fn()
        .mockResolvedValueOnce('data1')
        .mockResolvedValueOnce('data2')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const result1 = await deduplicator.dedupe(key, fetcher)
      const result2 = await deduplicator.dedupe(key, fetcher)

      expect(fetcher).toHaveBeenCalledTimes(2)
      expect(result1).toBe('data1')
      expect(result2).toBe('data2')
    })

    it('should propagate errors to all waiting callers', async () => {
      const error = new Error('Request failed')
      const fetcher = vi.fn().mockRejectedValue(error)

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      // Make 3 identical requests simultaneously
      const requests = Array(3).fill(null).map(() =>
        deduplicator.dedupe(key, fetcher)
      )

      // All should reject with the same error
      await expect(Promise.all(requests)).rejects.toThrow('Request failed')

      // Should only call fetcher once
      expect(fetcher).toHaveBeenCalledTimes(1)
    })

    it('should clear request from map after error', async () => {
      const fetcher = vi.fn().mockRejectedValue(new Error('Failed'))
      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      await expect(deduplicator.dedupe(key, fetcher)).rejects.toThrow('Failed')

      // Map should be empty after error
      expect(deduplicator.size).toBe(0)
    })

    it('should treat different endpoints as different requests', async () => {
      const fetcher1 = vi.fn().mockResolvedValue('data1')
      const fetcher2 = vi.fn().mockResolvedValue('data2')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const key2: RequestKey = {
        endpoint: '/api/users',
        method: 'GET',
        params: { id: '123' }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher1),
        deduplicator.dedupe(key2, fetcher2)
      ])

      expect(fetcher1).toHaveBeenCalledTimes(1)
      expect(fetcher2).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data1')
      expect(result2).toBe('data2')
    })

    it('should treat different methods as different requests', async () => {
      const fetcher1 = vi.fn().mockResolvedValue('data1')
      const fetcher2 = vi.fn().mockResolvedValue('data2')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const key2: RequestKey = {
        endpoint: '/api/teams',
        method: 'POST',
        params: { id: '123' }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher1),
        deduplicator.dedupe(key2, fetcher2)
      ])

      expect(fetcher1).toHaveBeenCalledTimes(1)
      expect(fetcher2).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data1')
      expect(result2).toBe('data2')
    })

    it('should treat different params as different requests', async () => {
      const fetcher1 = vi.fn().mockResolvedValue('data1')
      const fetcher2 = vi.fn().mockResolvedValue('data2')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const key2: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '456' }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher1),
        deduplicator.dedupe(key2, fetcher2)
      ])

      expect(fetcher1).toHaveBeenCalledTimes(1)
      expect(fetcher2).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data1')
      expect(result2).toBe('data2')
    })

    it('should treat same params in different order as identical', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123', name: 'test', active: true }
      }

      const key2: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { active: true, name: 'test', id: '123' }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher),
        deduplicator.dedupe(key2, fetcher)
      ])

      // Should only call fetcher once (requests are identical)
      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data')
      expect(result2).toBe('data')
    })

    it('should handle nested params correctly', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {
          filter: { status: 'active', type: 'public' },
          sort: { field: 'name', order: 'asc' }
        }
      }

      const key2: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {
          sort: { order: 'asc', field: 'name' },
          filter: { type: 'public', status: 'active' }
        }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher),
        deduplicator.dedupe(key2, fetcher)
      ])

      // Should only call fetcher once (nested params are sorted)
      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data')
      expect(result2).toBe('data')
    })

    it('should handle empty params', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {}
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result).toBe('data')
    })

    it('should handle params with null values', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123', filter: null }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result).toBe('data')
    })

    it('should handle params with array values', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { ids: ['1', '2', '3'] }
      }

      const key2: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { ids: ['1', '2', '3'] }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher),
        deduplicator.dedupe(key2, fetcher)
      ])

      // Should deduplicate (same array values)
      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data')
      expect(result2).toBe('data')
    })

    it('should handle complex data types in response', async () => {
      interface ComplexData {
        id: string
        nested: { value: number }
        array: string[]
      }

      const complexData: ComplexData = {
        id: '123',
        nested: { value: 42 },
        array: ['a', 'b', 'c']
      }

      const fetcher = vi.fn().mockResolvedValue(complexData)

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toEqual(complexData)
    })

    it('should track multiple in-flight requests', async () => {
      const fetcher1 = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 50))
        return 'data1'
      })

      const fetcher2 = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 50))
        return 'data2'
      })

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const key2: RequestKey = {
        endpoint: '/api/users',
        method: 'GET',
        params: { id: '456' }
      }

      // Start both requests
      const promise1 = deduplicator.dedupe(key1, fetcher1)
      const promise2 = deduplicator.dedupe(key2, fetcher2)

      // Both should be in flight
      expect(deduplicator.size).toBe(2)

      await Promise.all([promise1, promise2])

      // Both should be cleared
      expect(deduplicator.size).toBe(0)
    })
  })

  describe('clear', () => {
    it('should clear specific request from map', async () => {
      const fetcher = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return 'data'
      })

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      // Start request
      const promise = deduplicator.dedupe(key, fetcher)

      // Request should be in flight
      expect(deduplicator.size).toBe(1)

      // Clear the specific request
      deduplicator.clear(key)

      // Map should be empty
      expect(deduplicator.size).toBe(0)

      // Original promise should still resolve
      await expect(promise).resolves.toBe('data')
    })

    it('should clear all requests when no key provided', async () => {
      const fetcher1 = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return 'data1'
      })

      const fetcher2 = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return 'data2'
      })

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const key2: RequestKey = {
        endpoint: '/api/users',
        method: 'GET',
        params: { id: '456' }
      }

      // Start both requests
      const promise1 = deduplicator.dedupe(key1, fetcher1)
      const promise2 = deduplicator.dedupe(key2, fetcher2)

      // Both should be in flight
      expect(deduplicator.size).toBe(2)

      // Clear all
      deduplicator.clear()

      // Map should be empty
      expect(deduplicator.size).toBe(0)

      // Original promises should still resolve
      await expect(promise1).resolves.toBe('data1')
      await expect(promise2).resolves.toBe('data2')
    })

    it('should not affect other requests when clearing specific key', async () => {
      const fetcher1 = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return 'data1'
      })

      const fetcher2 = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return 'data2'
      })

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const key2: RequestKey = {
        endpoint: '/api/users',
        method: 'GET',
        params: { id: '456' }
      }

      // Start both requests
      deduplicator.dedupe(key1, fetcher1)
      deduplicator.dedupe(key2, fetcher2)

      expect(deduplicator.size).toBe(2)

      // Clear only first request
      deduplicator.clear(key1)

      // Only second request should remain
      expect(deduplicator.size).toBe(1)
    })

    it('should handle clearing non-existent key', () => {
      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      // Should not throw
      expect(() => deduplicator.clear(key)).not.toThrow()
      expect(deduplicator.size).toBe(0)
    })
  })

  describe('size', () => {
    it('should return 0 for empty map', () => {
      expect(deduplicator.size).toBe(0)
    })

    it('should return correct count of in-flight requests', async () => {
      const fetcher = vi.fn().mockImplementation(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
        return 'data'
      })

      const keys: RequestKey[] = [
        { endpoint: '/api/teams', method: 'GET', params: { id: '1' } },
        { endpoint: '/api/teams', method: 'GET', params: { id: '2' } },
        { endpoint: '/api/teams', method: 'GET', params: { id: '3' } }
      ]

      // Start all requests
      keys.forEach(key => deduplicator.dedupe(key, fetcher))

      expect(deduplicator.size).toBe(3)
    })
  })

  describe('Edge cases', () => {
    it('should handle fetcher that returns undefined', async () => {
      const fetcher = vi.fn().mockResolvedValue(undefined)

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBeUndefined()
      expect(deduplicator.size).toBe(0)
    })

    it('should handle fetcher that returns null', async () => {
      const fetcher = vi.fn().mockResolvedValue(null)

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBeNull()
      expect(deduplicator.size).toBe(0)
    })

    it('should handle fetcher that throws synchronously', async () => {
      const fetcher = vi.fn().mockImplementation(() => {
        throw new Error('Sync error')
      })

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { id: '123' }
      }

      await expect(deduplicator.dedupe(key, fetcher)).rejects.toThrow('Sync error')
      expect(deduplicator.size).toBe(0)
    })

    it('should handle very long endpoint URLs', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')
      const longEndpoint = '/api/' + 'a'.repeat(1000)

      const key: RequestKey = {
        endpoint: longEndpoint,
        method: 'GET',
        params: { id: '123' }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBe('data')
    })

    it('should handle params with special characters', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {
          name: 'Team "Special" & <Characters>',
          query: 'search?term=test&filter=active'
        }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBe('data')
    })

    it('should handle params with unicode characters', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {
          name: '团队名称',
          emoji: '🚀💻'
        }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBe('data')
    })

    it('should handle deeply nested params', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {
          level1: {
            level2: {
              level3: {
                level4: {
                  value: 'deep'
                }
              }
            }
          }
        }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBe('data')
    })

    it('should handle params with boolean values', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key1: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { active: true, archived: false }
      }

      const key2: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: { archived: false, active: true }
      }

      const [result1, result2] = await Promise.all([
        deduplicator.dedupe(key1, fetcher),
        deduplicator.dedupe(key2, fetcher)
      ])

      // Should deduplicate
      expect(fetcher).toHaveBeenCalledTimes(1)
      expect(result1).toBe('data')
      expect(result2).toBe('data')
    })

    it('should handle params with number values', async () => {
      const fetcher = vi.fn().mockResolvedValue('data')

      const key: RequestKey = {
        endpoint: '/api/teams',
        method: 'GET',
        params: {
          page: 1,
          limit: 10,
          offset: 0,
          score: 3.14
        }
      }

      const result = await deduplicator.dedupe(key, fetcher)

      expect(result).toBe('data')
    })
  })
})
