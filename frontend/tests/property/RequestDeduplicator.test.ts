/**
 * Property-Based Tests for RequestDeduplicator
 * 
 * Feature: team-navigation-performance-optimization
 * 
 * These tests verify universal correctness properties of the RequestDeduplicator utility
 * across a wide range of inputs using property-based testing with fast-check.
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { fc, propertyTestConfig } from '../setup'
import { RequestDeduplicator, type RequestKey } from '../../src/utils/RequestDeduplicator'

describe('Feature: team-navigation-performance-optimization', () => {
  describe('RequestDeduplicator - Property Tests', () => {
    let deduplicator: RequestDeduplicator

    beforeEach(() => {
      deduplicator = new RequestDeduplicator()
    })

    /**
     * Property 21: Request Deduplication
     * 
     * **Validates: Requirements 6.1, 6.2, 6.3**
     * 
     * For any set of identical API requests (same endpoint, method, and parameters)
     * initiated simultaneously, only one network request should be executed, and all
     * callers should receive the same response or error.
     */
    it('Property 21: Request Deduplication', { timeout: 60000 }, async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.string({ minLength: 1, maxLength: 50 }), // endpoint
          fc.constantFrom('GET', 'POST'), // method
          fc.record({
            id: fc.string({ maxLength: 20 }),
            page: fc.integer({ min: 1, max: 10 })
          }), // params
          fc.integer({ min: 2, max: 3 }), // number of simultaneous requests
          fc.boolean(), // success or error
          async (endpoint, method, params, requestCount, shouldSucceed) => {
            // Track how many times the fetcher is actually called
            let fetcherCallCount = 0
            const expectedResult = { data: 'test-data' }
            const expectedError = new Error('Test error')

            // Create a fetcher that tracks calls
            const fetcher = async () => {
              fetcherCallCount++
              await new Promise(resolve => setTimeout(resolve, 1))
              
              if (!shouldSucceed) {
                throw expectedError
              }
              return expectedResult
            }

            // Create request key
            const requestKey: RequestKey = { endpoint, method, params }

            // Make multiple identical requests simultaneously
            const requests = Array(requestCount)
              .fill(null)
              .map(() => deduplicator.dedupe(requestKey, fetcher))

            // Wait for all requests to complete
            if (shouldSucceed) {
              const results = await Promise.all(requests)
              
              // Verify all requests received the same result
              for (const result of results) {
                expect(result).toBe(expectedResult)
              }
            } else {
              // All requests should fail with the same error
              const errors = await Promise.allSettled(requests)
              
              for (const error of errors) {
                expect(error.status).toBe('rejected')
                if (error.status === 'rejected') {
                  expect(error.reason).toBe(expectedError)
                }
              }
            }

            // Critical assertion: fetcher should only be called once
            // This validates Requirements 6.1, 6.2, 6.3
            expect(fetcherCallCount).toBe(1)
            
            // Verify the request was cleared from the map after completion
            expect(deduplicator.size).toBe(0)

            return true
          }
        ),
        { numRuns: 50 } // Reduced from 100 for faster execution
      )
    })

    /**
     * Property 22: Deduplication Key Uniqueness
     * 
     * **Validates: Requirements 6.4**
     * 
     * For any two API requests, they should be considered identical (and deduplicated)
     * if and only if they have the same endpoint, HTTP method, and parameter values.
     * 
     * This property verifies that:
     * 1. Requests with identical endpoint, method, and params generate the same key
     * 2. Requests with different endpoints generate different keys
     * 3. Requests with different methods generate different keys
     * 4. Requests with different params generate different keys
     * 5. Parameter order doesn't affect key generation (params are normalized)
     */
    it('Property 22: Deduplication Key Uniqueness', { timeout: 60000 }, async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.string({ minLength: 1, maxLength: 50 }), // endpoint1
          fc.string({ minLength: 1, maxLength: 50 }), // endpoint2
          fc.constantFrom('GET', 'POST', 'PUT', 'DELETE'), // method1
          fc.constantFrom('GET', 'POST', 'PUT', 'DELETE'), // method2
          fc.record({
            id: fc.string({ maxLength: 20 }),
            page: fc.integer({ min: 1, max: 10 }),
            filter: fc.option(fc.string({ maxLength: 10 }), { nil: undefined })
          }), // params1
          fc.record({
            id: fc.string({ maxLength: 20 }),
            page: fc.integer({ min: 1, max: 10 }),
            filter: fc.option(fc.string({ maxLength: 10 }), { nil: undefined })
          }), // params2
          async (endpoint1, endpoint2, method1, method2, params1, params2) => {
            // Create two request keys
            const key1: RequestKey = { endpoint: endpoint1, method: method1, params: params1 }
            const key2: RequestKey = { endpoint: endpoint2, method: method2, params: params2 }

            // Track fetcher calls for each request
            let fetcher1CallCount = 0
            let fetcher2CallCount = 0

            const fetcher1 = async () => {
              fetcher1CallCount++
              await new Promise(resolve => setTimeout(resolve, 1))
              return { data: 'result1' }
            }

            const fetcher2 = async () => {
              fetcher2CallCount++
              await new Promise(resolve => setTimeout(resolve, 1))
              return { data: 'result2' }
            }

            // Execute both requests
            const promise1 = deduplicator.dedupe(key1, fetcher1)
            const promise2 = deduplicator.dedupe(key2, fetcher2)

            await Promise.all([promise1, promise2])

            // Determine if requests should be considered identical
            const shouldBeIdentical = 
              endpoint1 === endpoint2 &&
              method1 === method2 &&
              JSON.stringify(sortObjectKeys(params1)) === JSON.stringify(sortObjectKeys(params2))

            if (shouldBeIdentical) {
              // If requests are identical, only one fetcher should be called
              // The second request should reuse the first request's promise
              expect(fetcher1CallCount + fetcher2CallCount).toBe(1)
            } else {
              // If requests are different, both fetchers should be called
              expect(fetcher1CallCount).toBe(1)
              expect(fetcher2CallCount).toBe(1)
            }

            // Verify all requests are cleared after completion
            expect(deduplicator.size).toBe(0)

            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * Additional test: Parameter order independence
     * 
     * Verifies that requests with the same parameters in different order
     * are correctly identified as identical.
     */
    it('Property 22 (Supplement): Parameter Order Independence', { timeout: 30000 }, async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.string({ minLength: 1, maxLength: 50 }), // endpoint
          fc.constantFrom('GET', 'POST'), // method
          fc.record({
            a: fc.string({ maxLength: 10 }),
            b: fc.integer({ min: 1, max: 100 }),
            c: fc.boolean()
          }), // params
          async (endpoint, method, params) => {
            // Create params with different key order
            const paramsOrdered1 = { a: params.a, b: params.b, c: params.c }
            const paramsOrdered2 = { c: params.c, a: params.a, b: params.b }
            const paramsOrdered3 = { b: params.b, c: params.c, a: params.a }

            let fetcherCallCount = 0
            const fetcher = async () => {
              fetcherCallCount++
              await new Promise(resolve => setTimeout(resolve, 1))
              return { data: 'test' }
            }

            // Make three requests with same params but different order
            const key1: RequestKey = { endpoint, method, params: paramsOrdered1 }
            const key2: RequestKey = { endpoint, method, params: paramsOrdered2 }
            const key3: RequestKey = { endpoint, method, params: paramsOrdered3 }

            const promise1 = deduplicator.dedupe(key1, fetcher)
            const promise2 = deduplicator.dedupe(key2, fetcher)
            const promise3 = deduplicator.dedupe(key3, fetcher)

            await Promise.all([promise1, promise2, promise3])

            // All three requests should be deduplicated to a single call
            expect(fetcherCallCount).toBe(1)
            expect(deduplicator.size).toBe(0)

            return true
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * Property 23: Deduplicated Error Propagation
     * 
     * **Validates: Requirements 6.5**
     * 
     * For any deduplicated request that fails, all waiting callers should receive
     * the same error object. This ensures consistent error handling across all
     * components that requested the same data.
     * 
     * This property verifies that:
     * 1. When a deduplicated request fails, all callers receive an error
     * 2. All callers receive the exact same error object (not copies)
     * 3. The error is properly propagated without modification
     * 4. The failed request is cleared from the deduplication map
     */
    it('Property 23: Deduplicated Error Propagation', { timeout: 60000 }, async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.string({ minLength: 1, maxLength: 50 }), // endpoint
          fc.constantFrom('GET', 'POST', 'PUT', 'DELETE'), // method
          fc.record({
            id: fc.string({ maxLength: 20 }),
            filter: fc.option(fc.string({ maxLength: 10 }), { nil: undefined })
          }), // params
          fc.integer({ min: 2, max: 5 }), // number of simultaneous requests
          fc.string({ minLength: 1, maxLength: 100 }), // error message
          fc.constantFrom('NetworkError', 'TimeoutError', 'ServerError', 'ValidationError'), // error type
          async (endpoint, method, params, requestCount, errorMessage, errorType) => {
            // Create a specific error object that will be thrown
            const expectedError = new Error(errorMessage)
            expectedError.name = errorType

            // Track how many times the fetcher is called
            let fetcherCallCount = 0

            // Create a fetcher that always fails
            const fetcher = async () => {
              fetcherCallCount++
              // Add small delay to simulate network request
              await new Promise(resolve => setTimeout(resolve, 1))
              throw expectedError
            }

            // Create request key
            const requestKey: RequestKey = { endpoint, method, params }

            // Make multiple identical requests simultaneously
            const requests = Array(requestCount)
              .fill(null)
              .map(() => deduplicator.dedupe(requestKey, fetcher))

            // Wait for all requests to settle
            const results = await Promise.allSettled(requests)

            // Verify all requests failed
            for (const result of results) {
              expect(result.status).toBe('rejected')
            }

            // Verify all requests received the exact same error object
            const errors = results
              .filter((r): r is PromiseRejectedResult => r.status === 'rejected')
              .map(r => r.reason)

            // All errors should be the same object reference
            for (const error of errors) {
              expect(error).toBe(expectedError)
              expect(error.message).toBe(errorMessage)
              expect(error.name).toBe(errorType)
            }

            // Verify all callers received an error (count should match request count)
            expect(errors.length).toBe(requestCount)

            // Critical assertion: fetcher should only be called once
            // This validates that deduplication works even for failing requests
            expect(fetcherCallCount).toBe(1)

            // Verify the failed request was cleared from the map
            expect(deduplicator.size).toBe(0)

            return true
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})

/**
 * Helper function to sort object keys recursively
 * This mirrors the internal sorting logic of RequestDeduplicator
 */
function sortObjectKeys(obj: Record<string, any>): Record<string, any> {
  if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) {
    return obj
  }

  const sorted: Record<string, any> = {}
  const keys = Object.keys(obj).sort()

  for (const key of keys) {
    const value = obj[key]
    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      sorted[key] = sortObjectKeys(value)
    } else {
      sorted[key] = value
    }
  }

  return sorted
}
