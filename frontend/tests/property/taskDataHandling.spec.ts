import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  formatTaskDate,
  parseTaskDate,
  validateTaskTitle,
  validateTaskDescription,
  validateTaskStatus,
  validateTaskPriority,
  normalizeTask,
  isTaskOverdue
} from '@/utils/taskUtils'

/**
 * Property Test 25: 日期序列化往返一致性
 * 验证需求: Requirements 10.2
 * 
 * 属性: 对于任何有效的日期值，序列化为 ISO 8601 格式后再反序列化应该产生等价的日期值
 */
describe('Feature: task-board-enhancement, Property 25: 日期序列化往返一致性', () => {
  it('should maintain date consistency through serialization and deserialization', () => {
    fc.assert(
      fc.property(
        // Generate valid dates (not too far in past or future to avoid edge cases)
        fc.date({ min: new Date('2000-01-01'), max: new Date('2100-12-31') }),
        (date) => {
          // Only test with valid dates - invalid dates should return null
          if (isNaN(date.getTime())) {
            const serialized = formatTaskDate(date)
            expect(serialized).toBeNull()
            return true // Return true to indicate test passed for this case
          }
          
          // Serialize to ISO 8601
          const serialized = formatTaskDate(date)
          expect(serialized).toBeTruthy()
          
          // Deserialize back to Date
          const deserialized = parseTaskDate(serialized!)
          expect(deserialized).toBeTruthy()
          
          // Check that the dates are equivalent (within 1 second tolerance for precision)
          const timeDiff = Math.abs(date.getTime() - deserialized!.getTime())
          expect(timeDiff).toBeLessThan(1000)
          
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should handle ISO 8601 string input correctly', () => {
    fc.assert(
      fc.property(
        fc.date({ min: new Date('2000-01-01'), max: new Date('2100-12-31') }),
        (date) => {
          const isoString = date.toISOString()
          
          // Format should accept ISO string and return it normalized
          const formatted = formatTaskDate(isoString)
          expect(formatted).toBeTruthy()
          
          // Parse should convert back to Date
          const parsed = parseTaskDate(formatted!)
          expect(parsed).toBeTruthy()
          
          // Should be equivalent to original date
          const timeDiff = Math.abs(date.getTime() - parsed!.getTime())
          expect(timeDiff).toBeLessThan(1000)
          
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should return null for invalid date inputs', () => {
    fc.assert(
      fc.property(
        fc.oneof(
          fc.constant(null),
          fc.constant(undefined),
          fc.constant('invalid-date'),
          fc.constant(''),
          fc.constant('not a date')
        ),
        (invalidDate) => {
          const formatted = formatTaskDate(invalidDate as any)
          expect(formatted).toBeNull()
          
          const parsed = parseTaskDate(invalidDate as any)
          expect(parsed).toBeNull()
          
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should be idempotent - formatting twice should give same result', () => {
    fc.assert(
      fc.property(
        fc.date({ min: new Date('2000-01-01'), max: new Date('2100-12-31') }),
        (date) => {
          const formatted1 = formatTaskDate(date)
          const formatted2 = formatTaskDate(formatted1!)
          
          expect(formatted1).toBe(formatted2)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Property Test 26: 空值处理一致性
 * 验证需求: Requirements 10.4
 * 
 * 属性: 系统应该在前后端一致地处理 null 和 undefined 值，不会导致错误
 */
describe('Feature: task-board-enhancement, Property 26: 空值处理一致性', () => {
  it('should handle null and undefined in task title validation', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(null, undefined, ''),
        (value) => {
          const result = validateTaskTitle(value as any)
          expect(result).toBe(false)
          // Should not throw error
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should handle null and undefined in task description validation', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(null, undefined, ''),
        (value) => {
          const result = validateTaskDescription(value as any)
          // Description is optional, so null/undefined should be valid
          expect(result).toBe(true)
          // Should not throw error
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should handle null and undefined in date formatting', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(null, undefined),
        (value) => {
          const formatted = formatTaskDate(value as any)
          expect(formatted).toBeNull()
          
          const parsed = parseTaskDate(value as any)
          expect(parsed).toBeNull()
          // Should not throw error
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should normalize tasks with missing optional fields', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          createdBy: fc.integer({ min: 1 }),
          // Optional fields intentionally omitted or null
          description: fc.constantFrom(null, undefined),
          deadline: fc.constantFrom(null, undefined),
          assignees: fc.constantFrom(null, undefined, [])
        }),
        (taskData) => {
          const normalized = normalizeTask(taskData)
          
          // Should have default values for missing fields
          expect(normalized).toBeTruthy()
          expect(normalized.id).toBe(taskData.id)
          expect(normalized.title).toBe(taskData.title)
          expect(normalized.status).toBe('TODO') // Default status
          expect(normalized.priority).toBe('MEDIUM') // Default priority
          expect(normalized.description).toBeNull()
          expect(normalized.deadline).toBeNull()
          expect(Array.isArray(normalized.assignees)).toBe(true)
          expect(normalized.commentCount).toBe(0)
          expect(normalized.attachmentCount).toBe(0)
          // Should not throw error
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should handle tasks with all null optional fields', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          createdBy: fc.integer({ min: 1 }),
          description: fc.constant(null),
          status: fc.constant(null),
          priority: fc.constant(null),
          deadline: fc.constant(null),
          assignees: fc.constant(null),
          commentCount: fc.constant(null),
          attachmentCount: fc.constant(null)
        }),
        (taskData) => {
          const normalized = normalizeTask(taskData)
          
          // Should provide sensible defaults
          expect(normalized.status).toBe('TODO')
          expect(normalized.priority).toBe('MEDIUM')
          expect(normalized.description).toBeNull()
          expect(normalized.deadline).toBeNull()
          expect(Array.isArray(normalized.assignees)).toBe(true)
          expect(normalized.commentCount).toBe(0)
          expect(normalized.attachmentCount).toBe(0)
          // Should not throw error
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should handle undefined vs null consistently', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          createdBy: fc.integer({ min: 1 })
        }),
        (baseTask) => {
          // Create two tasks: one with undefined, one with null
          const taskWithUndefined = { ...baseTask, description: undefined, deadline: undefined }
          const taskWithNull = { ...baseTask, description: null, deadline: null }
          
          const normalizedUndefined = normalizeTask(taskWithUndefined)
          const normalizedNull = normalizeTask(taskWithNull)
          
          // Both should be normalized to null
          expect(normalizedUndefined.description).toBeNull()
          expect(normalizedNull.description).toBeNull()
          expect(normalizedUndefined.deadline).toBeNull()
          expect(normalizedNull.deadline).toBeNull()
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Property Test: 任务标题验证正确性
 * 验证需求: Requirements 2.5, 10.3
 * 
 * 属性: 验证函数应该拒绝所有无效的任务标题，接受所有有效的任务标题
 */
describe('Feature: task-board-enhancement, Property: 任务标题验证', () => {
  it('should reject empty or whitespace-only titles', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 0, maxLength: 10 }).filter(s => s.trim().length === 0),
        (title) => {
          const result = validateTaskTitle(title)
          expect(result).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject titles longer than 255 characters after trimming', () => {
    fc.assert(
      fc.property(
        // Generate strings that will have trimmed length > 255
        fc.string({ minLength: 256, maxLength: 300 }).filter(s => s.trim().length > 255),
        (title) => {
          const result = validateTaskTitle(title)
          expect(result).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should accept valid titles (1-255 characters after trimming)', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 255 }).filter(s => s.trim().length > 0 && s.trim().length <= 255),
        (title) => {
          const result = validateTaskTitle(title)
          expect(result).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should be consistent - same input produces same result', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 0, maxLength: 300 }),
        (title) => {
          const result1 = validateTaskTitle(title)
          const result2 = validateTaskTitle(title)
          expect(result1).toBe(result2)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Property Test: 任务状态和优先级验证
 * 验证需求: Requirements 8.2
 * 
 * 属性: 只有预定义的状态和优先级值应该被接受
 */
describe('Feature: task-board-enhancement, Property: 任务状态和优先级验证', () => {
  it('should only accept valid task statuses', () => {
    const validStatuses = ['TODO', 'DOING', 'REVIEW', 'DONE']
    
    fc.assert(
      fc.property(
        fc.constantFrom(...validStatuses),
        (status) => {
          const result = validateTaskStatus(status)
          expect(result).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject invalid task statuses', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 20 }).filter(
          s => !['TODO', 'DOING', 'REVIEW', 'DONE'].includes(s)
        ),
        (status) => {
          const result = validateTaskStatus(status)
          expect(result).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should only accept valid task priorities', () => {
    const validPriorities = ['LOW', 'MEDIUM', 'HIGH']
    
    fc.assert(
      fc.property(
        fc.constantFrom(...validPriorities),
        (priority) => {
          const result = validateTaskPriority(priority)
          expect(result).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should reject invalid task priorities', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 20 }).filter(
          s => !['LOW', 'MEDIUM', 'HIGH'].includes(s)
        ),
        (priority) => {
          const result = validateTaskPriority(priority)
          expect(result).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Property Test: 任务逾期判断正确性
 * 验证需求: Requirements 6.3
 * 
 * 属性: 逾期判断应该基于截止日期和当前时间正确计算
 */
describe('Feature: task-board-enhancement, Property: 任务逾期判断', () => {
  it('should not mark tasks without deadline as overdue', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          status: fc.constantFrom('TODO', 'DOING', 'REVIEW'),
          deadline: fc.constant(null),
          createdBy: fc.integer({ min: 1 })
        }),
        (task) => {
          const normalized = normalizeTask(task)
          const overdue = isTaskOverdue(normalized)
          expect(overdue).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should not mark completed tasks as overdue', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          status: fc.constant('DONE'),
          deadline: fc.date({ min: new Date('2020-01-01'), max: new Date('2023-12-31') }).map(d => {
            // Ensure the date is valid before converting to ISO string
            if (isNaN(d.getTime())) {
              return new Date('2022-01-01').toISOString()
            }
            return d.toISOString()
          }),
          createdBy: fc.integer({ min: 1 })
        }),
        (task) => {
          const normalized = normalizeTask(task)
          const overdue = isTaskOverdue(normalized)
          expect(overdue).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should mark tasks with past deadline as overdue', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          status: fc.constantFrom('TODO', 'DOING', 'REVIEW'),
          // Generate dates in the past
          deadline: fc.date({ min: new Date('2020-01-01'), max: new Date('2023-12-31') }).map(d => {
            // Ensure the date is valid before converting to ISO string
            if (isNaN(d.getTime())) {
              return new Date('2022-01-01').toISOString()
            }
            return d.toISOString()
          }),
          createdBy: fc.integer({ min: 1 })
        }),
        (task) => {
          const normalized = normalizeTask(task)
          const overdue = isTaskOverdue(normalized)
          // Past dates should be overdue
          expect(overdue).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should not mark tasks with future deadline as overdue', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.integer({ min: 1 }),
          teamId: fc.integer({ min: 1 }),
          title: fc.string({ minLength: 1, maxLength: 100 }),
          status: fc.constantFrom('TODO', 'DOING', 'REVIEW'),
          // Generate dates in the future
          deadline: fc.date({ min: new Date('2027-01-01'), max: new Date('2030-12-31') }).map(d => {
            // Ensure the date is valid before converting to ISO string
            if (isNaN(d.getTime())) {
              return new Date('2028-01-01').toISOString()
            }
            return d.toISOString()
          }),
          createdBy: fc.integer({ min: 1 })
        }),
        (task) => {
          const normalized = normalizeTask(task)
          const overdue = isTaskOverdue(normalized)
          // Future dates should not be overdue
          expect(overdue).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})
