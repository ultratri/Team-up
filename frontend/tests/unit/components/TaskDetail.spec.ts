import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import TaskDetail from '@/components/team/TaskDetail.vue'
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
  }
})

// Mock child components
vi.mock('@/components/team/TaskAssignees.vue', () => ({
  default: {
    name: 'TaskAssignees',
    template: '<div class="task-assignees-mock">TaskAssignees</div>',
    props: ['taskId', 'teamId'],
    emits: ['update'],
  },
}))

vi.mock('@/components/team/TaskComments.vue', () => ({
  default: {
    name: 'TaskComments',
    template: '<div class="task-comments-mock">TaskComments</div>',
    props: ['taskId'],
    emits: ['update'],
  },
}))

vi.mock('@/components/team/TaskAttachments.vue', () => ({
  default: {
    name: 'TaskAttachments',
    template: '<div class="task-attachments-mock">TaskAttachments</div>',
    props: ['taskId'],
    emits: ['update'],
  },
}))

describe('TaskDetail.vue', () => {
  let requestMock: any

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()

    // Mock request utility
    requestMock = {
      get: vi.fn(),
      put: vi.fn(),
      post: vi.fn(),
      delete: vi.fn(),
    }
    vi.spyOn(requestUtils, 'request', 'get').mockReturnValue(requestMock)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const mockTaskDetail = {
    id: 100,
    teamId: 10,
    title: '测试任务',
    description: '这是一个测试任务的描述',
    status: 'TODO' as const,
    priority: 'HIGH' as const,
    deadline: '2026-02-01',
    createdBy: 1,
    creatorName: '张三',
    createdAt: '2026-01-26T10:00:00',
    updatedAt: '2026-01-26T10:00:00',
    assignees: [],
    comments: [],
    attachments: [],
    commentCount: 0,
    attachmentCount: 0,
  }

  describe('Dialog Open and Close', () => {
    it('should open dialog when modelValue is true', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.find('.el-dialog').exists()).toBe(true)
      expect(wrapper.find('.task-detail-content').exists()).toBe(true)
    })

    it('should close dialog when modelValue is false', async () => {
      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: false,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.visible).toBe(false)
    })

    it('should emit update:modelValue when dialog closes', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.handleClose()
      await flushPromises()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false])
    })

    it('should fetch task detail when dialog opens', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(requestMock.get).toHaveBeenCalledWith('/tasks/100')
    })

    it('should not fetch task detail when taskId is not provided', async () => {
      mount(TaskDetail, {
        props: {
          modelValue: true,
        },
      })

      await flushPromises()

      expect(requestMock.get).not.toHaveBeenCalled()
    })

    it('should show warning when closing during edit', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.handleClose()

      expect(ElMessage.warning).toHaveBeenCalledWith('编辑未保存，已取消')
    })
  })

  describe('Task Information Display', () => {
    it('should display task title', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('测试任务')
    })

    it('should display task description', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('这是一个测试任务的描述')
    })

    it('should display task status', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.taskForm.status).toBe('TODO')
    })

    it('should display task priority', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.taskForm.priority).toBe('HIGH')
    })

    it('should display task deadline', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.taskForm.deadline).toBe('2026-02-01')
    })

    it('should display creator name', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('张三')
    })

    it('should display creation time', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('创建于')
    })

    it('should display comment count badge', async () => {
      const taskWithComments = {
        ...mockTaskDetail,
        commentCount: 5,
      }
      requestMock.get.mockResolvedValueOnce({ data: taskWithComments })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.taskDetail.commentCount).toBe(5)
    })

    it('should display attachment count badge', async () => {
      const taskWithAttachments = {
        ...mockTaskDetail,
        attachmentCount: 3,
      }
      requestMock.get.mockResolvedValueOnce({ data: taskWithAttachments })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.taskDetail.attachmentCount).toBe(3)
    })
  })

  describe('Task Information Edit', () => {
    it('should enable edit mode when edit button is clicked', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.isEditing).toBe(false)

      vm.isEditing = true
      await flushPromises()

      expect(vm.isEditing).toBe(true)
    })

    it('should allow editing task title', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.title = '修改后的标题'
      await flushPromises()

      expect(vm.taskForm.title).toBe('修改后的标题')
    })

    it('should allow editing task description', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.description = '修改后的描述'
      await flushPromises()

      expect(vm.taskForm.description).toBe('修改后的描述')
    })

    it('should allow changing task status', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.status = 'DOING'
      await flushPromises()

      expect(vm.taskForm.status).toBe('DOING')
    })

    it('should allow changing task priority', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.priority = 'LOW'
      await flushPromises()

      expect(vm.taskForm.priority).toBe('LOW')
    })

    it('should allow changing task deadline', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.deadline = '2026-03-01'
      await flushPromises()

      expect(vm.taskForm.deadline).toBe('2026-03-01')
    })

    it('should disable form fields when not editing', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.isEditing).toBe(false)
    })
  })

  describe('Save Functionality', () => {
    it('should save task successfully', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })
      requestMock.put.mockResolvedValueOnce({
        data: {
          ...mockTaskDetail,
          title: '修改后的标题',
        },
      })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.title = '修改后的标题'
      await vm.handleSave()
      await flushPromises()

      expect(requestMock.put).toHaveBeenCalledWith('/tasks', {
        id: 100,
        teamId: 10,
        title: '修改后的标题',
        description: mockTaskDetail.description,
        status: mockTaskDetail.status,
        priority: mockTaskDetail.priority,
        deadline: mockTaskDetail.deadline,
        createdBy: mockTaskDetail.createdBy,
      })
      expect(ElMessage.success).toHaveBeenCalledWith('保存成功')
      expect(wrapper.emitted('saved')).toBeTruthy()
    })

    it('should validate required fields before saving', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.title = ''
      
      // Mock form validation to fail
      if (vm.formRef) {
        vm.formRef.validate = vi.fn().mockRejectedValue(new Error('Validation failed'))
      }

      await vm.handleSave()
      await flushPromises()

      expect(requestMock.put).not.toHaveBeenCalled()
    })

    it('should show error when save fails', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })
      requestMock.put.mockRejectedValueOnce(new Error('保存失败'))

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      
      // Mock form validation to pass
      if (vm.formRef) {
        vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      await vm.handleSave()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should exit edit mode after successful save', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })
      requestMock.put.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      
      // Mock form validation to pass
      if (vm.formRef) {
        vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      await vm.handleSave()
      await flushPromises()

      expect(vm.isEditing).toBe(false)
    })

    it('should validate deadline is not in the past', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const pastDate = '2020-01-01'
      
      // Test the deadline validator
      const rule = vm.formRules.deadline[0]
      const callback = vi.fn()
      rule.validator(null, pastDate, callback)

      expect(callback).toHaveBeenCalledWith(expect.any(Error))
    })

    it('should allow deadline to be today or future', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const futureDate = '2026-12-31'
      
      // Test the deadline validator
      const rule = vm.formRules.deadline[0]
      const callback = vi.fn()
      rule.validator(null, futureDate, callback)

      expect(callback).toHaveBeenCalledWith()
    })
  })

  describe('Sub-component Integration', () => {
    it('should render TaskAssignees component', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.find('.task-assignees-mock').exists()).toBe(true)
    })

    it('should render TaskComments component', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.find('.task-comments-mock').exists()).toBe(true)
    })

    it('should render TaskAttachments component', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.find('.task-attachments-mock').exists()).toBe(true)
    })

    it('should refresh task detail when sub-component updates', async () => {
      requestMock.get.mockResolvedValue({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      // Clear previous calls
      requestMock.get.mockClear()

      const vm = wrapper.vm as any
      await vm.handleSubComponentUpdate()
      await flushPromises()

      // Should fetch task detail again
      expect(requestMock.get).toHaveBeenCalledWith('/tasks/100')
    })

    it('should pass correct props to TaskAssignees', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const assigneesComponent = wrapper.findComponent({ name: 'TaskAssignees' })
      expect(assigneesComponent.props('taskId')).toBe(100)
      expect(assigneesComponent.props('teamId')).toBe(10)
    })

    it('should pass correct props to TaskComments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const commentsComponent = wrapper.findComponent({ name: 'TaskComments' })
      expect(commentsComponent.props('taskId')).toBe(100)
    })

    it('should pass correct props to TaskAttachments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const attachmentsComponent = wrapper.findComponent({ name: 'TaskAttachments' })
      expect(attachmentsComponent.props('taskId')).toBe(100)
    })
  })

  describe('Tab Navigation', () => {
    it('should default to assignees tab', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.activeTab).toBe('assignees')
    })

    it('should switch to comments tab', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.activeTab = 'comments'
      await flushPromises()

      expect(vm.activeTab).toBe('comments')
    })

    it('should switch to attachments tab', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.activeTab = 'attachments'
      await flushPromises()

      expect(vm.activeTab).toBe('attachments')
    })
  })

  describe('Error Handling', () => {
    it('should handle fetch error and close dialog', async () => {
      requestMock.get.mockRejectedValueOnce(new Error('Network error'))

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
      const vm = wrapper.vm as any
      expect(vm.visible).toBe(false)
    })

    it('should handle missing task detail data', async () => {
      requestMock.get.mockResolvedValueOnce({ data: null })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.taskDetail).toBeNull()
    })
  })

  describe('Component Lifecycle', () => {
    it('should expose refresh method', async () => {
      requestMock.get.mockResolvedValue({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(typeof vm.refresh).toBe('function')

      // Call refresh
      await vm.refresh()
      await flushPromises()

      // Should fetch task detail again
      expect(requestMock.get).toHaveBeenCalledTimes(2)
    })

    it('should reset form when dialog closes', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.isEditing = true
      vm.taskForm.title = '修改后的标题'
      vm.handleClose()
      await flushPromises()

      expect(vm.isEditing).toBe(false)
      expect(vm.taskDetail).toBeNull()
    })

    it('should reset active tab when dialog closes', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.activeTab = 'comments'
      vm.handleClose()
      await flushPromises()

      expect(vm.activeTab).toBe('assignees')
    })
  })

  describe('Date Formatting', () => {
    it('should format date time correctly', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const formatted = vm.formatDateTime('2026-01-26T10:30:00')
      expect(formatted).toBeTruthy()
      expect(formatted).toContain('2026')
    })

    it('should handle undefined date time', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const formatted = vm.formatDateTime(undefined)
      expect(formatted).toBe('未知')
    })
  })

  describe('Form Validation Rules', () => {
    it('should require task title', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const titleRule = vm.formRules.title[0]
      expect(titleRule.required).toBe(true)
    })

    it('should validate title length', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const titleRule = vm.formRules.title[1]
      expect(titleRule.min).toBe(1)
      expect(titleRule.max).toBe(255)
    })

    it('should reject whitespace-only title', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const titleRule = vm.formRules.title[2]
      const callback = vi.fn()
      
      titleRule.validator(null, '   ', callback)
      expect(callback).toHaveBeenCalledWith(expect.any(Error))
      
      callback.mockClear()
      titleRule.validator(null, 'Valid Title', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('should require task status', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const statusRule = vm.formRules.status[0]
      expect(statusRule.required).toBe(true)
    })

    it('should require task priority', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const priorityRule = vm.formRules.priority[0]
      expect(priorityRule.required).toBe(true)
    })

    it('should validate deadline format', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const deadlineRule = vm.formRules.deadline[0]
      const callback = vi.fn()
      
      // Test invalid format
      deadlineRule.validator(null, '2026/01/01', callback)
      expect(callback).toHaveBeenCalledWith(expect.any(Error))
      
      // Test valid format
      callback.mockClear()
      deadlineRule.validator(null, '2026-01-01', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('should reject invalid date', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const deadlineRule = vm.formRules.deadline[0]
      const callback = vi.fn()
      
      deadlineRule.validator(null, '2026-13-45', callback)
      expect(callback).toHaveBeenCalledWith(expect.any(Error))
    })

    it('should reject past deadline', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const deadlineRule = vm.formRules.deadline[0]
      const callback = vi.fn()
      
      deadlineRule.validator(null, '2020-01-01', callback)
      expect(callback).toHaveBeenCalledWith(expect.any(Error))
    })

    it('should accept future deadline', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const deadlineRule = vm.formRules.deadline[0]
      const callback = vi.fn()
      
      deadlineRule.validator(null, '2026-12-31', callback)
      expect(callback).toHaveBeenCalledWith()
    })

    it('should allow empty deadline', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockTaskDetail })

      const wrapper = mount(TaskDetail, {
        props: {
          modelValue: true,
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      const deadlineRule = vm.formRules.deadline[0]
      const callback = vi.fn()
      
      deadlineRule.validator(null, '', callback)
      expect(callback).toHaveBeenCalledWith()
      
      callback.mockClear()
      deadlineRule.validator(null, null, callback)
      expect(callback).toHaveBeenCalledWith()
    })
  })
})
