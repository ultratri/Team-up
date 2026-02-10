import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TaskStats from '@/components/team/TaskStats.vue'

// Mock the request module
const mockGet = vi.fn()
vi.mock('@/utils/request', () => ({
  request: {
    get: mockGet
  }
}))

describe('TaskStats.vue', () => {
  const mockStats = {
    todoCount: 5,
    doingCount: 3,
    reviewCount: 2,
    doneCount: 10,
    totalCount: 20,
    completionRate: 50.0,
    overdueCount: 1
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue(mockStats)
  })

  afterEach(() => {
    vi.clearAllTimers()
  })

  // Test: 测试统计组件渲染
  describe('Component Rendering', () => {
    it('renders all status cards correctly', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check if all 4 status cards are rendered
      const statCards = wrapper.findAll('.stat-card')
      expect(statCards).toHaveLength(4)

      // Check card labels
      expect(wrapper.text()).toContain('待办')
      expect(wrapper.text()).toContain('进行中')
      expect(wrapper.text()).toContain('审核中')
      expect(wrapper.text()).toContain('已完成')
    })

    it('renders completion rate section', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check completion section exists
      const completionSection = wrapper.find('.completion-section')
      expect(completionSection.exists()).toBe(true)

      // Check completion rate title
      expect(wrapper.text()).toContain('任务完成率')
    })

    it('renders overdue alert when there are overdue tasks', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check overdue alert text
      expect(wrapper.text()).toContain('有 1 个任务已逾期')
    })

    it('does not render overdue alert when there are no overdue tasks', async () => {
      mockGet.mockResolvedValue({
        ...mockStats,
        overdueCount: 0
      })

      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check overdue alert does not exist
      const alert = wrapper.find('.overdue-alert')
      expect(alert.exists()).toBe(false)
    })
  })

  // Test: 测试统计数据显示
  describe('Statistics Data Display', () => {
    it('displays correct task counts for each status', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check if counts are displayed correctly
      const cardCounts = wrapper.findAll('.card-count')
      expect(cardCounts[0].text()).toBe('5') // TODO
      expect(cardCounts[1].text()).toBe('3') // DOING
      expect(cardCounts[2].text()).toBe('2') // REVIEW
      expect(cardCounts[3].text()).toBe('10') // DONE
    })

    it('displays correct completion rate percentage', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check completion rate display
      expect(wrapper.text()).toContain('50.0%')
    })

    it('displays correct completion detail text', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check completion detail
      expect(wrapper.text()).toContain('已完成 10 / 总计 20 个任务')
    })

    it('displays correct overdue count', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Check overdue count in alert
      expect(wrapper.text()).toContain('有 1 个任务已逾期')
    })

    it('updates display when stats data changes', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Initial state
      expect(wrapper.text()).toContain('50.0%')

      // Update mock data
      const newStats = {
        todoCount: 2,
        doingCount: 1,
        reviewCount: 1,
        doneCount: 16,
        totalCount: 20,
        completionRate: 80.0,
        overdueCount: 0
      }
      mockGet.mockResolvedValue(newStats)

      // Trigger refresh
      await wrapper.vm.refresh()
      await flushPromises()

      // Check updated state
      expect(wrapper.text()).toContain('80.0%')
      const cardCounts = wrapper.findAll('.card-count')
      expect(cardCounts[3].text()).toBe('16') // DONE count
    })
  })

  // Test: API call and data loading
  describe('Data Loading', () => {
    it('calls API with correct team ID on mount', async () => {
      mount(TaskStats, {
        props: {
          teamId: 123
        }
      })
      await flushPromises()

      expect(mockGet).toHaveBeenCalledWith('/tasks/team/123/stats')
    })

    it('shows loading state while fetching data', async () => {
      // Create a promise that we can control
      let resolvePromise: any
      const promise = new Promise((resolve) => {
        resolvePromise = resolve
      })
      mockGet.mockReturnValue(promise)

      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })

      // Should be loading
      expect(wrapper.vm.loading).toBe(true)

      // Resolve the promise
      resolvePromise(mockStats)
      await flushPromises()

      // Should not be loading anymore
      expect(wrapper.vm.loading).toBe(false)
    })

    it('emits refresh event after loading stats', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      expect(wrapper.emitted('refresh')).toBeTruthy()
      expect(wrapper.emitted('refresh')).toHaveLength(1)
    })

    it('reloads stats when teamId prop changes', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      // Clear previous calls
      mockGet.mockClear()

      // Change teamId
      await wrapper.setProps({ teamId: 2 })
      await flushPromises()

      expect(mockGet).toHaveBeenCalledWith('/tasks/team/2/stats')
    })
  })

  // Test: Auto-refresh functionality
  describe('Auto-refresh', () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('sets up auto-refresh when enabled', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1,
          autoRefresh: true,
          refreshInterval: 5000
        }
      })
      await flushPromises()

      // Clear initial call
      mockGet.mockClear()

      // Fast-forward time
      vi.advanceTimersByTime(5000)
      await flushPromises()

      // Should have called API again
      expect(mockGet).toHaveBeenCalledTimes(1)
    })

    it('clears auto-refresh on unmount', async () => {
      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1,
          autoRefresh: true,
          refreshInterval: 5000
        }
      })
      await flushPromises()

      // Unmount component
      wrapper.unmount()

      // Clear previous calls
      mockGet.mockClear()

      // Fast-forward time
      vi.advanceTimersByTime(5000)
      await flushPromises()

      // Should not have called API
      expect(mockGet).not.toHaveBeenCalled()
    })
  })

  // Test: Completion rate color
  describe('Completion Rate Color', () => {
    it('uses green color for high completion rate (>=80%)', async () => {
      mockGet.mockResolvedValue({
        ...mockStats,
        completionRate: 85.0
      })

      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      expect(wrapper.vm.completionRateColor).toBe('#67c23a')
    })

    it('uses blue color for medium completion rate (50-79%)', async () => {
      mockGet.mockResolvedValue({
        ...mockStats,
        completionRate: 60.0
      })

      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      expect(wrapper.vm.completionRateColor).toBe('#409eff')
    })

    it('uses orange color for low-medium completion rate (30-49%)', async () => {
      mockGet.mockResolvedValue({
        ...mockStats,
        completionRate: 40.0
      })

      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      expect(wrapper.vm.completionRateColor).toBe('#e6a23c')
    })

    it('uses red color for low completion rate (<30%)', async () => {
      mockGet.mockResolvedValue({
        ...mockStats,
        completionRate: 20.0
      })

      const wrapper = mount(TaskStats, {
        props: {
          teamId: 1
        }
      })
      await flushPromises()

      expect(wrapper.vm.completionRateColor).toBe('#f56c6c')
    })
  })
})
