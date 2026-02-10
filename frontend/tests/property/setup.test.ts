/**
 * Property-Based Testing Setup Verification
 * 
 * This test verifies that the property-based testing infrastructure is correctly configured.
 */

import { describe, it, expect } from 'vitest'
import { fc, propertyTestConfig } from '../setup'

describe('Property-Based Testing Setup', () => {
  it('should run property tests with minimum 100 iterations', async () => {
    // Verify configuration
    expect(propertyTestConfig.numRuns).toBe(100)
    expect(propertyTestConfig.verbose).toBe(true)
    expect(propertyTestConfig.endOnFailure).toBe(true)
  })

  it('should execute a simple property test', async () => {
    // Simple property: reversing a string twice returns the original string
    await fc.assert(
      fc.property(
        fc.string(),
        (str) => {
          const reversed = str.split('').reverse().join('')
          const doubleReversed = reversed.split('').reverse().join('')
          expect(doubleReversed).toBe(str)
        }
      ),
      propertyTestConfig
    )
  })

  it('should execute an async property test', async () => {
    // Simple async property: Promise.resolve returns the same value
    await fc.assert(
      fc.asyncProperty(
        fc.integer(),
        async (num) => {
          const result = await Promise.resolve(num)
          expect(result).toBe(num)
        }
      ),
      propertyTestConfig
    )
  })

  it('should handle arrays in property tests', async () => {
    // Property: array length is preserved after mapping
    await fc.assert(
      fc.property(
        fc.array(fc.integer()),
        (arr) => {
          const mapped = arr.map(x => x * 2)
          expect(mapped.length).toBe(arr.length)
        }
      ),
      propertyTestConfig
    )
  })
})
