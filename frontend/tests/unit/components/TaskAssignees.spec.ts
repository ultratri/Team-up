import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskAssignees from '@/components/team/TaskAssignees.vue'
import * as teamApi from '@/api/team'
import * as requestUtils from '@/utils/request'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

describe('TaskAssignees.vue', () => {
  let requestMock: any

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()

    // Mock request utility
    requestMock = {
      get: vi.fn(),
      post: vi.fn(),
      delete: vi.fn(),
    }
    vi.spyOn(requestUtils, 'request', 'get').mockReturnValue(requestMock)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const mockAssignees = [
    {
      id: 1,
      taskId: 100,
      userId: 1,
      userName: '张三',
      avatar: 'https://example.com/avatar1.jpg',
      assignedAt: '2026-01-26T10:00:00',
    },
    {
      id: 2,
      taskId: 100,
      userId: 2,
      userName: '李四',
      avatar: 'https://example.com/avatar2.jpg',
      assignedAt: '2026-01-26T11:00:00',
    },
  ]

  const mockTeamMembers = [
    {
      id: 1,
      teamId: 10,
      userId: 1,
      username: '张三',
      avatar: 'https://example.com/avatar1.jpg',
      role: 'MEMBER',
      joinedAt: '2026-01-20T10:00:00',
      updatedAt: '2026-01-20T10:00:00',
    },
    {
      id: 2,
      teamId: 10,
      userId: 2,
      username: '李四',
      avatar: 'https://example.com/avatar2.jpg',
      role: 'MEMBER',
      joinedAt: '2026-01-21T10:00:00',
      updatedAt: '2026-01-21T10:00:00',
    },
    {
      id: 3,
      teamId: 10,
      userId: 3,
      username: '王五',
      avatar: 'https://example.com/avatar3.jpg',
      role: 'MEMBER',
      joinedAt: '2026-01-22T10:00:00',
      updatedAt: '2026-01-22T10:00:00',
    },
  ]

  describe('Assignee List Rendering', () => {
    it('should display assignee list when assignees exist', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('李四')
    })

    it('should display empty state when no assignees', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('暂无负责人')
    })

    it('should display assignee avatars', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const avatars = wrapper.findAll('.el-avatar')
      expect(avatars.length).toBeGreaterThan(0)
    })

    it('should display assignee names', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const names = wrapper.findAll('.assignee-name')
      expect(names.length).toBe(2)
      expect(names[0].text()).toBe('张三')
      expect(names[1].text()).toBe('李四')
    })

    it('should display assigned time', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const times = wrapper.findAll('.assignee-time')
      expect(times.length).toBe(2)
      // Should display relative time
      expect(times[0].text()).toBeTruthy()
    })
  })

  describe('Add Assignee Interaction', () => {
    it('should open add dialog when add button is clicked', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const addButton = wrapper.find('.el-button')
      await addButton.trigger('click')
      await flushPromises()

      expect(wrapper.find('.el-dialog').exists()).toBe(true)
    })

    it('should show available team members in dropdown', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [mockAssignees[0]] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      // Open dialog
      const addButton = wrapper.find('.el-button')
      await addButton.trigger('click')
      await flushPromises()

      // Should show members not already assigned (李四 and 王五)
      const vm = wrapper.vm as any
      expect(vm.availableMembers.length).toBe(2)
      expect(vm.availableMembers.some((m: any) => m.username === '李四')).toBe(true)
      expect(vm.availableMembers.some((m: any) => m.username === '王五')).toBe(true)
    })

    it('should add assignee successfully', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockResolvedValueOnce({
        data: {
          id: 3,
          taskId: 100,
          userId: 3,
          userName: '王五',
          avatar: 'https://example.com/avatar3.jpg',
          assignedAt: '2026-01-26T12:00:00',
        },
      })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      // Open dialog and select user
      const vm = wrapper.vm as any
      vm.showAddDialog = true
      vm.selectedUserId = 3
      await flushPromises()

      // Click confirm
      await vm.handleAdd()
      await flushPromises()

      expect(requestMock.post).toHaveBeenCalledWith('/tasks/100/assignees', {
        userId: 3,
      })
      expect(ElMessage.success).toHaveBeenCalledWith('添加负责人成功')
      expect(wrapper.emitted('update')).toBeTruthy()
    })

    it('should show error when add fails', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockRejectedValueOnce(new Error('添加失败'))
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.selectedUserId = 3
      await vm.handleAdd()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should not add when no user is selected', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.selectedUserId = null
      await vm.handleAdd()
      await flushPromises()

      expect(requestMock.post).not.toHaveBeenCalled()
    })
  })

  describe('Remove Assignee Interaction', () => {
    it('should show confirmation dialog when remove button is clicked', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleRemove(mockAssignees[0])

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        '确定要移除负责人 张三 吗？',
        '提示',
        expect.any(Object)
      )
    })

    it('should remove assignee successfully', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      requestMock.delete.mockResolvedValueOnce({})
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleRemove(mockAssignees[0])
      await flushPromises()

      expect(requestMock.delete).toHaveBeenCalledWith('/tasks/100/assignees/1')
      expect(ElMessage.success).toHaveBeenCalledWith('移除负责人成功')
      expect(wrapper.emitted('update')).toBeTruthy()
    })

    it('should show error when remove fails', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      requestMock.delete.mockRejectedValueOnce(new Error('移除失败'))
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleRemove(mockAssignees[0])
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should not remove when user cancels confirmation', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)
      ;(ElMessageBox.confirm as any).mockRejectedValueOnce(new Error('cancel'))

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleRemove(mockAssignees[0])
      await flushPromises()

      expect(requestMock.delete).not.toHaveBeenCalled()
    })
  })

  describe('API Error Handling', () => {
    it('should handle fetch assignees error', async () => {
      requestMock.get.mockRejectedValueOnce(new Error('Network error'))
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should handle fetch team members error', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      vi.spyOn(teamApi, 'getTeamMembers').mockRejectedValue(
        new Error('Network error')
      )

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('获取团队成员失败')
    })
  })

  describe('Time Formatting', () => {
    it('should display "刚刚" for very recent time', async () => {
      const now = new Date().toISOString()
      const recentAssignee = {
        ...mockAssignees[0],
        assignedAt: now,
      }
      requestMock.get.mockResolvedValueOnce({ data: [recentAssignee] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const formatted = vm.formatTime(now)
      expect(formatted).toBe('刚刚')
    })

    it('should display minutes ago for recent time', async () => {
      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      const vm = wrapper.vm as any
      const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(tenMinutesAgo)
      expect(formatted).toContain('分钟前')
    })

    it('should display hours ago for time within a day', async () => {
      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      const vm = wrapper.vm as any
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(twoHoursAgo)
      expect(formatted).toContain('小时前')
    })

    it('should display days ago for recent days', async () => {
      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      const vm = wrapper.vm as any
      const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(threeDaysAgo)
      expect(formatted).toContain('天前')
    })

    it('should display date for old time', async () => {
      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      const vm = wrapper.vm as any
      const twoWeeksAgo = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(twoWeeksAgo)
      // Should be a date format
      expect(formatted).toBeTruthy()
    })
  })

  describe('Component Lifecycle', () => {
    it('should fetch assignees on mount', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      expect(requestMock.get).toHaveBeenCalledWith('/tasks/100/assignees')
    })

    it('should fetch team members on mount', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      const getTeamMembersSpy = vi
        .spyOn(teamApi, 'getTeamMembers')
        .mockResolvedValue(mockTeamMembers)

      mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      expect(getTeamMembersSpy).toHaveBeenCalledWith(10, false)
    })

    it('should expose refresh method', async () => {
      requestMock.get.mockResolvedValue({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(typeof vm.refresh).toBe('function')

      // Call refresh
      await vm.refresh()
      await flushPromises()

      // Should fetch assignees again
      expect(requestMock.get).toHaveBeenCalledTimes(2)
    })
  })

  describe('Edge Cases', () => {
    it('should handle assignee without avatar', async () => {
      const assigneeNoAvatar = {
        ...mockAssignees[0],
        avatar: undefined,
      }
      requestMock.get.mockResolvedValueOnce({ data: [assigneeNoAvatar] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(mockTeamMembers)

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      // Should still render avatar with fallback
      const avatars = wrapper.findAll('.el-avatar')
      expect(avatars.length).toBeGreaterThan(0)
    })

    it('should handle empty team members list', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue([])

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.availableMembers.length).toBe(0)
    })

    it('should handle all members already assigned', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockAssignees })
      vi.spyOn(teamApi, 'getTeamMembers').mockResolvedValue(
        mockTeamMembers.slice(0, 2)
      )

      const wrapper = mount(TaskAssignees, {
        props: {
          taskId: 100,
          teamId: 10,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.availableMembers.length).toBe(0)
    })
  })
})
