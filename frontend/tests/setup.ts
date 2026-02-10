/**
 * Vitest Setup File
 * 
 * Configures global test settings including property-based testing defaults.
 * As per design requirements, property tests must run with minimum 100 iterations.
 */

import { beforeAll, afterEach } from 'vitest'
import * as fc from 'fast-check'

// Configure fast-check global defaults
// Minimum 100 iterations for all property tests as per design requirements
export const propertyTestConfig = {
  numRuns: 100,
  verbose: true,
  seed: Date.now(), // Use timestamp for reproducibility
  endOnFailure: true
}

// Export configured fast-check for use in tests
export { fc }

// Global test setup
beforeAll(() => {
  // Any global setup needed before tests run
})

// Clean up after each test
afterEach(() => {
  // Reset any global state if needed
})
