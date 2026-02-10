/**
 * Property-Based Tests for ParallelDataLoader
 * 
 * Feature: team-navigation-performance-optimization
 * 
 * These tests verify the correctness properties of the ParallelDataLoader utility
 * using property-based testing with fast-check.
 */

import { describe, it, expect } from 'vitest'
import { fc, propertyTestConfig } from '../setup'
import { ParallelDataLoader, LoadRequest } from '../../src/utils/ParallelDataLoader'

describe('Feature: team-navigation-performance-optimization', () => {
  describe('ParallelDataLoader Property Tests', () => {
    
    /**
     * Property 1: Parallel Request Execution
     * 
     * **Validates: Requirements 1.1**
     * 
     * For any team page navigation, when multiple data requests are needed,
     * all requests should start within 50ms of each other, and total load time
     * should be approximately equal to the slowest individual request
     * (not the sum of all requests).
     */
    it('Property 1: Parallel Request Execution', { timeout: 60000 }, async () => {
      await fc.assert(
        fc.asyncProperty(
          // Generate 2-10 requests with varying durations (50ms to 300ms)
          // Using moderate durations to balance test time and meaningful measurements
          fc.array(
            fc.record({
              key: fc.string({ minLength: 1, maxLength: 20 }),
              duration: fc.integer({ min: 50, max: 300 })
            }),
            { minLength: 2, maxLength: 10 }
          ),
          async (requestConfigs) => {
            // Ensure unique keys
            const uniqueConfigs = Array.from(
              new Map(requestConfigs.map(config => [config.key, config])).values()
            )
            
            if (uniqueConfigs.length < 2) {
              // Skip if we don't have at least 2 unique requests
              return true
            }

            const loader = new ParallelDataLoader()
            const startTimes: Map<string, number> = new Map()
            const endTimes: Map<string, number> = new Map()

            // Create load requests that track their start and end times
            const requests: LoadRequest<string>[] = uniqueConfigs.map(config => ({
              key: config.key,
              priority: 'normal',
              loader: async () => {
                const startTime = performance.now()
                startTimes.set(config.key, startTime)
                
                // Simulate async work with the specified duration
                await new Promise(resolve => setTimeout(resolve, config.duration))
                
                const endTime = performance.now()
                endTimes.set(config.key, endTime)
                
                return `data-${config.key}`
              }
            }))

            // Execute all requests in parallel
            const overallStart = performance.now()
            const results = await loader.loadAll(requests)
            const overallEnd = performance.now()
            const totalDuration = overallEnd - overallStart

            // Verify all requests completed successfully
            expect(results.size).toBe(uniqueConfigs.length)
            
            // Property 1a: All requests should start within 50ms of each other
            const startTimesArray = Array.from(startTimes.values())
            const minStartTime = Math.min(...startTimesArray)
            const maxStartTime = Math.max(...startTimesArray)
            const startTimeSpread = maxStartTime - minStartTime
            
            expect(startTimeSpread).toBeLessThanOrEqual(50)

            // Property 1b: Total load time should be approximately equal to the slowest request
            // (not the sum of all requests)
            const slowestRequestDuration = Math.max(...uniqueConfigs.map(c => c.duration))
            const sumOfAllDurations = uniqueConfigs.reduce((sum, c) => sum + c.duration, 0)
            
            // Total duration should be close to the slowest request duration
            // Allow 100ms overhead for Promise.allSettled and other async operations
            const expectedMaxDuration = slowestRequestDuration + 100
            expect(totalDuration).toBeLessThanOrEqual(expectedMaxDuration)
            
            // Property 1b (key property): Total duration should be much closer to max than to sum
            // This proves parallel execution rather than sequential execution
            // 
            // For parallel execution: totalDuration ≈ max(durations)
            // For sequential execution: totalDuration ≈ sum(durations)
            // 
            // We measure which one the actual duration is closer to
            const distanceFromMax = Math.abs(totalDuration - slowestRequestDuration)
            const distanceFromSum = Math.abs(totalDuration - sumOfAllDurations)
            
            // The total duration should be much closer to max than to sum
            // This is the key indicator of parallel execution
            expect(distanceFromMax).toBeLessThan(distanceFromSum)

            // Verify all results have timing information
            results.forEach((result, key) => {
              expect(result.key).toBe(key)
              expect(result.duration).toBeGreaterThan(0)
              expect(result.data).toBe(`data-${key}`)
              expect(result.error).toBeUndefined()
            })

            return true
          }
        ),
        propertyTestConfig
      )
    })

    /**
     * Property 2: Partial Failure Resilience
     * 
     * **Validates: Requirements 1.3**
     * 
     * For any set of parallel data requests, when one or more requests fail,
     * the system should successfully render all sections with available data
     * and display error states only for the failed sections.
     */
    it('Property 2: Partial Failure Resilience', { timeout: 60000 }, async () => {
      await fc.assert(
        fc.asyncProperty(
          // Generate 2-20 requests with a mix of successes and failures
          fc.array(
            fc.record({
              key: fc.string({ minLength: 1, maxLength: 20 }),
              shouldFail: fc.boolean(),
              duration: fc.integer({ min: 10, max: 100 })
            }),
            { minLength: 2, maxLength: 20 }
          ),
          async (requestConfigs) => {
            // Ensure unique keys
            const uniqueConfigs = Array.from(
              new Map(requestConfigs.map(config => [config.key, config])).values()
            )
            
            if (uniqueConfigs.length < 2) {
              // Skip if we don't have at least 2 unique requests
              return true
            }

            // Ensure we have at least one success and one failure for meaningful test
            const hasSuccess = uniqueConfigs.some(c => !c.shouldFail)
            const hasFailure = uniqueConfigs.some(c => c.shouldFail)
            
            if (!hasSuccess || !hasFailure) {
              // Skip if all succeed or all fail - we need a mix
              return true
            }

            const loader = new ParallelDataLoader()

            // Create load requests with some that succeed and some that fail
            const requests: LoadRequest<string>[] = uniqueConfigs.map(config => ({
              key: config.key,
              priority: 'normal',
              loader: async () => {
                // Simulate async work
                await new Promise(resolve => setTimeout(resolve, config.duration))
                
                if (config.shouldFail) {
                  throw new Error(`Request failed: ${config.key}`)
                }
                
                return `data-${config.key}`
              }
            }))

            // Execute all requests in parallel
            const results = await loader.loadAll(requests)

            // Property 2a: All requests should complete (no blocking)
            // The results map should contain an entry for every request
            expect(results.size).toBe(uniqueConfigs.length)

            // Property 2b: Successful requests should return data
            const successfulConfigs = uniqueConfigs.filter(c => !c.shouldFail)
            for (const config of successfulConfigs) {
              const result = results.get(config.key)
              expect(result).toBeDefined()
              expect(result!.data).toBe(`data-${config.key}`)
              expect(result!.error).toBeUndefined()
              expect(result!.duration).toBeGreaterThan(0)
            }

            // Property 2c: Failed requests should have error information
            const failedConfigs = uniqueConfigs.filter(c => c.shouldFail)
            for (const config of failedConfigs) {
              const result = results.get(config.key)
              expect(result).toBeDefined()
              expect(result!.error).toBeDefined()
              expect(result!.error).toBeInstanceOf(Error)
              expect(result!.error!.message).toContain(`Request failed: ${config.key}`)
              expect(result!.duration).toBeGreaterThan(0)
            }

            // Property 2d: Failed requests should not have data (unless fallback provided)
            for (const config of failedConfigs) {
              const result = results.get(config.key)
              // Since we didn't provide fallback data, data should be undefined
              expect(result!.data).toBeUndefined()
            }

            // Property 2e: Verify that failures didn't block successful requests
            // All successful requests should have completed and returned data
            const successfulResults = successfulConfigs.map(c => results.get(c.key))
            expect(successfulResults.every(r => r?.data !== undefined)).toBe(true)

            return true
          }
        ),
        propertyTestConfig
      )
    })
  })
})
