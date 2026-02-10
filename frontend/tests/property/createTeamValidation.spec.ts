import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { validateTeamName, validateTeamDescription } from '@/utils/validation'

/**
 * Property Test 4: 团队名称验证
 * 验证需求: 2.2.3
 * 
 * 属性: 验证函数应该拒绝所有无效的团队名称，接受所有有效的团队名称
 */
describe('Feature: team-management, Property 4: 团队名称验证', () => {
  it('should reject team names that are too short (< 2 characters)', () => {
    fc.assert(
      fc.property(
        // Generate strings with length 0-1
        fc.string({ minLength: 0, maxLength: 1 }),
        (name) => {
          const result = validateTeamName(name)
          expect(result.valid).toBe(false)
          expect(result.message).toBeTruthy()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject team names that are too long (> 50 characters)', () => {
    fc.assert(
      fc.property(
        // Generate strings with length 51-100
        fc.string({ minLength: 51, maxLength: 100 }),
        (name) => {
          const result = validateTeamName(name)
          expect(result.valid).toBe(false)
          // Message should indicate the length limit
          expect(result.message).toBeTruthy()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject team names with invalid characters', () => {
    fc.assert(
      fc.property(
        // Generate strings with special characters
        fc.string({ minLength: 2, maxLength: 50 }).filter(s => {
          // Only test strings that contain invalid characters
          return /[^a-zA-Z0-9_\u4e00-\u9fa5]/.test(s)
        }),
        (name) => {
          const result = validateTeamName(name)
          expect(result.valid).toBe(false)
          expect(result.message).toBeTruthy()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should accept valid team names (Chinese, English, numbers, underscore)', () => {
    fc.assert(
      fc.property(
        // Generate valid team names with alphanumeric and underscore
        fc.string({ minLength: 2, maxLength: 50 }).filter(s => {
          // Only test strings that match the valid pattern
          return /^[\u4e00-\u9fa5a-zA-Z0-9_]+$/.test(s) && s.trim().length >= 2
        }),
        (name) => {
          const result = validateTeamName(name)
          expect(result.valid).toBe(true)
          expect(result.message).toBeUndefined()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject empty or whitespace-only names', () => {
    fc.assert(
      fc.property(
        // Generate whitespace strings
        fc.string({ minLength: 0, maxLength: 10 }).filter(s => s.trim().length === 0),
        (name) => {
          const result = validateTeamName(name)
          expect(result.valid).toBe(false)
          expect(result.message).toBeTruthy()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should handle non-string inputs gracefully', () => {
    fc.assert(
      fc.property(
        // Generate various non-string values
        fc.oneof(
          fc.integer(),
          fc.boolean(),
          fc.constant(null),
          fc.constant(undefined),
          fc.object()
        ),
        (value) => {
          const result = validateTeamName(value as any)
          expect(result.valid).toBe(false)
          expect(result.message).toBeTruthy()
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Property Test: 团队描述验证
 * 验证需求: 2.2.3
 * 
 * 属性: 验证函数应该接受空描述和有效描述，拒绝过长的描述
 */
describe('Feature: team-management, Property: 团队描述验证', () => {
  it('should accept empty or undefined descriptions', () => {
    fc.assert(
      fc.property(
        fc.constantFrom('', undefined),
        (description) => {
          const result = validateTeamDescription(description)
          expect(result.valid).toBe(true)
          expect(result.message).toBeUndefined()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should accept descriptions within length limit (≤ 500 characters)', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 500 }),
        (description) => {
          const result = validateTeamDescription(description)
          expect(result.valid).toBe(true)
          expect(result.message).toBeUndefined()
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject descriptions that are too long (> 500 characters)', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 501, maxLength: 1000 }),
        (description) => {
          const result = validateTeamDescription(description)
          expect(result.valid).toBe(false)
          expect(result.message).toContain('500')
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Property Test: 验证规则一致性
 * 验证需求: 2.2.3
 * 
 * 属性: 相同的输入应该总是产生相同的验证结果（幂等性）
 */
describe('Feature: team-management, Property: 验证规则一致性', () => {
  it('should produce consistent results for the same team name', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 0, maxLength: 100 }),
        (name) => {
          const result1 = validateTeamName(name)
          const result2 = validateTeamName(name)
          
          expect(result1.valid).toBe(result2.valid)
          expect(result1.message).toBe(result2.message)
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should produce consistent results for the same description', () => {
    fc.assert(
      fc.property(
        fc.option(fc.string({ minLength: 0, maxLength: 1000 })),
        (description) => {
          const result1 = validateTeamDescription(description)
          const result2 = validateTeamDescription(description)
          
          expect(result1.valid).toBe(result2.valid)
          expect(result1.message).toBe(result2.message)
        }
      ),
      { numRuns: 100 }
    )
  })
})
