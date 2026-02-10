import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ParallelDataLoader, type LoadRequest } from '@/utils/ParallelDataLoader'

describe('ParallelDataLoader', () => {
  let loader: ParallelDataLoader

  beforeEach(() => {
    loader = new ParallelDataLoader()
    vi.clearAllMocks()
  })

  describe('loadAll', () => {
    it('should execute all requests concurrently', async () => {
      const startTimes: number[] = []
      const requests: LoadRequest<string>[] = [
        {
          key: 'request1',
          loader: async () => {
            startTimes.push(Date.now())
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'data1'
          },
          priority: 'normal'
        },
        {
          key: 'request2',
          loader: async () => {
            startTimes.push(Date.now())
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'data2'
          },
          priority: 'normal'
        },
        {
          key: 'request3',
          loader: async () => {
            startTimes.push(Date.now())
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'data3'
          },
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      // All requests should start within 50ms of each other (concurrent execution)
      const maxStartTimeDiff = Math.max(...startTimes) - Math.min(...startTimes)
      expect(maxStartTimeDiff).toBeLessThan(50)

      // All results should be present
      expect(results.size).toBe(3)
      expect(results.get('request1')?.data).toBe('data1')
      expect(results.get('request2')?.data).toBe('data2')
      expect(results.get('request3')?.data).toBe('data3')
    })

    it('should handle individual request failures without blocking others', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'success1',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 10))
            return 'data1'
          },
          priority: 'normal'
        },
        {
          key: 'failure',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 10))
            throw new Error('Request failed')
          },
          priority: 'normal'
        },
        {
          key: 'success2',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 10))
            return 'data2'
          },
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      // All results should be present
      expect(results.size).toBe(3)
      
      // Successful requests should have data
      expect(results.get('success1')?.data).toBe('data1')
      expect(results.get('success1')?.error).toBeUndefined()
      expect(results.get('success2')?.data).toBe('data2')
      expect(results.get('success2')?.error).toBeUndefined()
      
      // Failed request should have error
      expect(results.get('failure')?.error).toBeDefined()
      expect(results.get('failure')?.error?.message).toBe('Request failed')
      expect(results.get('failure')?.data).toBeUndefined()
    })

    it('should use fallback data when request fails', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'failure',
          loader: async () => {
            throw new Error('Request failed')
          },
          priority: 'normal',
          fallback: 'fallback-data'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.get('failure')?.error).toBeDefined()
      expect(results.get('failure')?.data).toBe('fallback-data')
    })

    it('should return timing information for each request', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'fast',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 10))
            return 'fast-data'
          },
          priority: 'normal'
        },
        {
          key: 'slow',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'slow-data'
          },
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      const fastResult = results.get('fast')
      const slowResult = results.get('slow')

      expect(fastResult?.duration).toBeGreaterThan(0)
      expect(slowResult?.duration).toBeGreaterThan(0)
      expect(slowResult?.duration).toBeGreaterThan(fastResult?.duration!)
    })

    it('should handle empty request array', async () => {
      const requests: LoadRequest<string>[] = []

      const results = await loader.loadAll(requests)

      expect(results.size).toBe(0)
    })

    it('should handle all requests failing', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'failure1',
          loader: async () => {
            throw new Error('Error 1')
          },
          priority: 'normal'
        },
        {
          key: 'failure2',
          loader: async () => {
            throw new Error('Error 2')
          },
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.size).toBe(2)
      expect(results.get('failure1')?.error?.message).toBe('Error 1')
      expect(results.get('failure2')?.error?.message).toBe('Error 2')
    })

    it('should handle non-Error exceptions', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'string-error',
          loader: async () => {
            throw 'String error'
          },
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.get('string-error')?.error).toBeInstanceOf(Error)
      expect(results.get('string-error')?.error?.message).toBe('String error')
    })
  })

  describe('loadWithPriority', () => {
    it('should execute requests in priority order', async () => {
      const executionOrder: string[] = []
      
      const requests: LoadRequest<string>[] = [
        {
          key: 'low',
          loader: async () => {
            executionOrder.push('low')
            return 'low-data'
          },
          priority: 'low'
        },
        {
          key: 'critical',
          loader: async () => {
            executionOrder.push('critical')
            return 'critical-data'
          },
          priority: 'critical'
        },
        {
          key: 'normal',
          loader: async () => {
            executionOrder.push('normal')
            return 'normal-data'
          },
          priority: 'normal'
        },
        {
          key: 'high',
          loader: async () => {
            executionOrder.push('high')
            return 'high-data'
          },
          priority: 'high'
        }
      ]

      const results = await loader.loadWithPriority(requests)

      // Critical should start first, then high, then normal, then low
      expect(executionOrder[0]).toBe('critical')
      expect(executionOrder[1]).toBe('high')
      expect(executionOrder[2]).toBe('normal')
      expect(executionOrder[3]).toBe('low')

      // All results should be present
      expect(results.size).toBe(4)
      expect(results.get('critical')?.data).toBe('critical-data')
      expect(results.get('high')?.data).toBe('high-data')
      expect(results.get('normal')?.data).toBe('normal-data')
      expect(results.get('low')?.data).toBe('low-data')
    })

    it('should execute requests within same priority concurrently', async () => {
      const startTimes: Record<string, number> = {}
      
      const requests: LoadRequest<string>[] = [
        {
          key: 'high1',
          loader: async () => {
            startTimes['high1'] = Date.now()
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'high1-data'
          },
          priority: 'high'
        },
        {
          key: 'high2',
          loader: async () => {
            startTimes['high2'] = Date.now()
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'high2-data'
          },
          priority: 'high'
        },
        {
          key: 'high3',
          loader: async () => {
            startTimes['high3'] = Date.now()
            await new Promise(resolve => setTimeout(resolve, 50))
            return 'high3-data'
          },
          priority: 'high'
        }
      ]

      const results = await loader.loadWithPriority(requests)

      // All high priority requests should start within 50ms of each other
      const times = Object.values(startTimes)
      const maxDiff = Math.max(...times) - Math.min(...times)
      expect(maxDiff).toBeLessThan(50)

      // All results should be present
      expect(results.size).toBe(3)
    })

    it('should handle failures in priority-based loading', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'critical-success',
          loader: async () => 'critical-data',
          priority: 'critical'
        },
        {
          key: 'high-failure',
          loader: async () => {
            throw new Error('High priority failed')
          },
          priority: 'high'
        },
        {
          key: 'normal-success',
          loader: async () => 'normal-data',
          priority: 'normal'
        }
      ]

      const results = await loader.loadWithPriority(requests)

      expect(results.size).toBe(3)
      expect(results.get('critical-success')?.data).toBe('critical-data')
      expect(results.get('high-failure')?.error?.message).toBe('High priority failed')
      expect(results.get('normal-success')?.data).toBe('normal-data')
    })

    it('should handle empty priority groups', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'critical',
          loader: async () => 'critical-data',
          priority: 'critical'
        },
        {
          key: 'low',
          loader: async () => 'low-data',
          priority: 'low'
        }
      ]

      const results = await loader.loadWithPriority(requests)

      expect(results.size).toBe(2)
      expect(results.get('critical')?.data).toBe('critical-data')
      expect(results.get('low')?.data).toBe('low-data')
    })

    it('should return timing information for priority-based loading', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'critical',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 10))
            return 'critical-data'
          },
          priority: 'critical'
        },
        {
          key: 'normal',
          loader: async () => {
            await new Promise(resolve => setTimeout(resolve, 20))
            return 'normal-data'
          },
          priority: 'normal'
        }
      ]

      const results = await loader.loadWithPriority(requests)

      expect(results.get('critical')?.duration).toBeGreaterThan(0)
      expect(results.get('normal')?.duration).toBeGreaterThan(0)
    })

    it('should use fallback data in priority-based loading', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'critical-failure',
          loader: async () => {
            throw new Error('Critical failed')
          },
          priority: 'critical',
          fallback: 'critical-fallback'
        }
      ]

      const results = await loader.loadWithPriority(requests)

      expect(results.get('critical-failure')?.error).toBeDefined()
      expect(results.get('critical-failure')?.data).toBe('critical-fallback')
    })

    it('should handle empty request array', async () => {
      const requests: LoadRequest<string>[] = []

      const results = await loader.loadWithPriority(requests)

      expect(results.size).toBe(0)
    })
  })

  describe('Edge cases', () => {
    it('should handle requests with duplicate keys', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'duplicate',
          loader: async () => 'data1',
          priority: 'normal'
        },
        {
          key: 'duplicate',
          loader: async () => 'data2',
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      // Last one wins (Map behavior)
      expect(results.size).toBe(1)
      expect(results.get('duplicate')?.data).toBeDefined()
    })

    it('should handle very fast requests', async () => {
      const requests: LoadRequest<string>[] = [
        {
          key: 'instant',
          loader: async () => 'instant-data',
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.get('instant')?.data).toBe('instant-data')
      expect(results.get('instant')?.duration).toBeGreaterThanOrEqual(0)
    })

    it('should handle requests that return undefined', async () => {
      const requests: LoadRequest<undefined>[] = [
        {
          key: 'undefined-result',
          loader: async () => undefined,
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.get('undefined-result')?.data).toBeUndefined()
      expect(results.get('undefined-result')?.error).toBeUndefined()
    })

    it('should handle requests that return null', async () => {
      const requests: LoadRequest<null>[] = [
        {
          key: 'null-result',
          loader: async () => null,
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.get('null-result')?.data).toBeNull()
      expect(results.get('null-result')?.error).toBeUndefined()
    })

    it('should handle complex data types', async () => {
      interface ComplexData {
        id: number
        name: string
        nested: { value: string }
      }

      const complexData: ComplexData = {
        id: 1,
        name: 'test',
        nested: { value: 'nested-value' }
      }

      const requests: LoadRequest<ComplexData>[] = [
        {
          key: 'complex',
          loader: async () => complexData,
          priority: 'normal'
        }
      ]

      const results = await loader.loadAll(requests)

      expect(results.get('complex')?.data).toEqual(complexData)
    })
  })
})
