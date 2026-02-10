import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import TaskBoard from '@/views/team/TaskBoard.vue'
import { useAuthStore } from '@/store/auth'
import * as teamApi from '@/api/team'
import type { Task } from '@/types/team'

// Mock ElMessage
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn()
    }
  }
})

// Mock team API
vi.mock('@/api/team', () => ({
  getTeamTasks: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
  filterTeamTasks: vi.fn(),
  getTeamMembers: vi.fn()
}))

// Helper function to create wrapper with common stubs
const createWrapper = (pinia: ReturnType<typeof createPinia>, teamId: number = 123) => {
  return mount(TaskBoard, {
    props: {
      teamId
    },
    global: {
      plugins: [pinia],
      stubs: {
        'el-dialog': {
          template: '<div><slot /></div>',
          props: ['modelValue'],
          emits: ['update:modelValue']
        },
        'el-form': true,
        'el-form-item': true,
        'el-input': true,
        'el-radio-group': true,
        'el-radio-button': true,
        'el-date-picker': true,
        'el-button': true,
        'el-icon': true,
        'el-card': true,
        'el-dropdown': true,
        'el-dropdown-menu': true,
        'el-dropdown-item': true,
        'el-tag': true,
        'el-avatar': true,
        'el-badge': true,
        'VueDraggable': {
          template: '<div><slot /></div>',
          props: ['modelValue', 'group', 'animation', 'ghostClass'],
          emits: ['change']
        },
        'TaskCard': {
          template: '<div class="task-card" @click="$emit(\'click\', task)">{{ task.title }}</div>',
          props: ['task'],
          emits: ['click', 'update', 'delete']
        },
        'TaskDetail': {
          template: '<div class="task-detail"></div>',
          props: ['modelValue', 'taskId'],
          emits: ['update:modelValue', 'saved']
        },
        'TaskFilters': {
          template: '<div class="task-filters"></div>',
          props: ['teamMembers'],
          emits: ['filter-change']
        },
        'GlassCard': true
      }
    }
  })
}

describe('TaskBoard.vue', () => {
  let pinia: ReturnType<typeof createPinia>

  const mockTasks: Task[] = [
    {
      id: 1,
      teamId: 123,
      title: '任务1',
      description: '描述1',
      status: 'TODO',
      priority: 'HIGH',
      deadline: '2026-02-01',
      createdBy: 1,
      createdAt: '2026-01-15T10:00:00',
      updatedAt: '2026-01-15T10:00:00',
      assignees: [
        { id: 1, userId: 1, userName: '用户1', avatar: 'avatar1.jpg' }
      ],
      commentCount: 2,
      attachmentCount: 1
    },
    {
      id: 2,
      teamId: 123,
      title: '任务2',
      description: '描述2',
      status: 'DOING',
      priority: 'MEDIUM',
      createdBy: 1,
      createdAt: '2026-01-16T11:00:00',
      updatedAt: '2026-01-16T11:00:00',
      assignees: [],
      commentCount: 0,
      attachmentCount: 0
    },
    {
      id: 3,
      teamId: 123,
      title: '任务3',
      description: '描述3',
      status: 'DONE',
      priority: 'LOW',
      createdBy: 1,
      createdAt: '2026-01-17T12:00:00',
      updatedAt: '2026-01-17T12:00:00',
      assignees: [],
      commentCount: 5,
      attachmentCount: 3
    }
  ]

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
    
    // 设置认证用户
    const authStore = useAuthStore()
    authStore.setUser({
      id: 1,
      username: 'testuser',
      email: 'test@example.com',
      roles: []
    }, false)

    // 默认 mock 返回任务列表
    vi.mocked(teamApi.getTeamTasks).mockResolvedValue(mockTasks)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue([
      {
        id: 1,
        teamId: 123,
        userId: 1,
        role: 'MEMBER',
        joinedAt: '2026-01-01T00:00:00',
        updatedAt: '2026-01-01T00:00:00',
        username: '用户1',
        avatar: 'avatar1.jpg',
        email: 'user1@example.com'
      }
    ])
  })

  afterEach(() => {
    vi.clearAllTimers()
  })

  describe('任务加载', () => {
    it('should load tasks on mount', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(teamApi.getTeamTasks).toHaveBeenCalledWith(123)
    })

    it('should distribute tasks to correct columns', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.columns.TODO).toHaveLength(1)
      expect(vm.columns.DOING).toHaveLength(1)
      expect(vm.columns.REVIEW).toHaveLength(0)
      expect(vm.columns.DONE).toHaveLength(1)
    })

    it('should display loading state while fetching tasks', async () => {
      vi.mocked(teamApi.getTeamTasks).mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve(mockTasks), 100))
      )

      const wrapper = createWrapper(pinia, 123)
      
      const vm = wrapper.vm as any
      expect(vm.loading).toBe(true)

      await flushPromises()
      
      expect(vm.loading).toBe(false)
    })

    it('should handle empty task list', async () => {
      vi.mocked(teamApi.getTeamTasks).mockResolvedValue([])

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.columns.TODO).toHaveLength(0)
      expect(vm.columns.DOING).toHaveLength(0)
      expect(vm.columns.REVIEW).toHaveLength(0)
      expect(vm.columns.DONE).toHaveLength(0)
    })

    it('should handle task loading error', async () => {
      const error = new Error('加载任务失败')
      vi.mocked(teamApi.getTeamTasks).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('加载任务失败')
    })

    it('should reload tasks when teamId changes', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(teamApi.getTeamTasks).toHaveBeenCalledWith(123)

      // 改变 teamId
      await wrapper.setProps({ teamId: 456 })
      await flushPromises()

      expect(teamApi.getTeamTasks).toHaveBeenCalledWith(456)
      expect(teamApi.getTeamTasks).toHaveBeenCalledTimes(2)
    })
  })

  describe('任务卡片点击打开详情', () => {
    it('should open task detail dialog when clicking task card', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.showDetailDialog).toBe(false)
      expect(vm.selectedTaskId).toBeUndefined()

      // 模拟点击任务卡片
      const taskCard = wrapper.find('.task-card')
      await taskCard.trigger('click')

      expect(vm.showDetailDialog).toBe(true)
      expect(vm.selectedTaskId).toBe(1)
    })

    it('should pass correct task ID to detail dialog', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      // 点击第二个任务
      vm.handleTaskClick(mockTasks[1])
      await wrapper.vm.$nextTick()

      expect(vm.selectedTaskId).toBe(2)
      expect(vm.showDetailDialog).toBe(true)
    })

    it('should reload tasks after detail dialog saves', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      vi.clearAllMocks()

      const vm = wrapper.vm as any
      vm.showDetailDialog = true
      vm.selectedTaskId = 1

      // 模拟详情对话框保存
      await vm.handleDetailSaved()

      expect(teamApi.getTeamTasks).toHaveBeenCalledWith(123)
    })
  })

  describe('任务拖拽更新状态', () => {
    it('should update task status when dragged to different column', async () => {
      vi.mocked(teamApi.updateTask).mockResolvedValue({
        ...mockTasks[0],
        status: 'DOING'
      } as Task)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      // 模拟拖拽事件
      const dragEvent = {
        added: {
          element: { ...mockTasks[0], status: 'TODO' }
        }
      }

      await vm.handleDragChange(dragEvent, 'DOING')
      await flushPromises()

      expect(teamApi.updateTask).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 1,
          status: 'DOING'
        })
      )
      expect(ElMessage.success).toHaveBeenCalledWith('任务状态已更新')
    })

    it('should show loading state during drag update', async () => {
      vi.mocked(teamApi.updateTask).mockImplementation(() =>
        new Promise(resolve => setTimeout(() => resolve(mockTasks[0] as Task), 100))
      )

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      const dragEvent = {
        added: {
          element: { ...mockTasks[0] }
        }
      }

      const updatePromise = vm.handleDragChange(dragEvent, 'DOING')
      
      expect(vm.dragLoading).toBe(true)

      await updatePromise
      await flushPromises()

      expect(vm.dragLoading).toBe(false)
    })

    it('should rollback task status on update failure', async () => {
      const error = new Error('更新失败')
      vi.mocked(teamApi.updateTask).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      // 初始状态：任务在 TODO 列
      expect(vm.columns.TODO).toHaveLength(1)
      expect(vm.columns.DOING).toHaveLength(1)

      const task = { ...mockTasks[0], status: 'TODO' }
      const dragEvent = {
        added: {
          element: task
        }
      }

      await vm.handleDragChange(dragEvent, 'DOING')
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('更新任务状态失败')
      
      // 验证任务状态被回滚
      expect(task.status).toBe('TODO')
    })

    it('should not update if drag event has no added element', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      const dragEvent = {
        removed: {
          element: mockTasks[0]
        }
      }

      await vm.handleDragChange(dragEvent, 'DOING')

      expect(teamApi.updateTask).not.toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('should display error message when task loading fails', async () => {
      const error = new Error('网络错误')
      vi.mocked(teamApi.getTeamTasks).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('网络错误')
    })

    it('should display generic error message when error has no message', async () => {
      vi.mocked(teamApi.getTeamTasks).mockRejectedValue({})

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('加载任务失败')
    })

    it('should handle task creation error', async () => {
      const error = new Error('创建失败')
      vi.mocked(teamApi.createTask).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      vm.taskForm.title = '新任务'
      
      await vm.handleCreateTask()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('创建失败')
    })

    it('should handle task update error', async () => {
      const error = new Error('更新失败')
      vi.mocked(teamApi.updateTask).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleUpdateTask(mockTasks[0])
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('更新失败')
    })

    it('should handle task deletion error', async () => {
      const error = new Error('删除失败')
      vi.mocked(teamApi.deleteTask).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleDeleteTask(1)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('删除失败')
    })
  })

  describe('边缘情况', () => {
    it('should handle tasks with unknown status', async () => {
      const tasksWithUnknownStatus = [
        {
          ...mockTasks[0],
          status: 'UNKNOWN' as any
        }
      ]

      vi.mocked(teamApi.getTeamTasks).mockResolvedValue(tasksWithUnknownStatus)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      // 未知状态的任务应该被放到 TODO 列
      expect(vm.columns.TODO).toHaveLength(1)
    })

    it('should handle tasks without assignees', async () => {
      const tasksWithoutAssignees = [
        {
          ...mockTasks[0],
          assignees: undefined
        }
      ]

      vi.mocked(teamApi.getTeamTasks).mockResolvedValue(tasksWithoutAssignees)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      // 应该正常加载，不抛出错误
      expect(teamApi.getTeamTasks).toHaveBeenCalled()
    })

    it('should handle tasks without counts', async () => {
      const tasksWithoutCounts = [
        {
          ...mockTasks[0],
          commentCount: undefined,
          attachmentCount: undefined
        }
      ]

      vi.mocked(teamApi.getTeamTasks).mockResolvedValue(tasksWithoutCounts)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      // 应该正常加载，不抛出错误
      expect(teamApi.getTeamTasks).toHaveBeenCalled()
    })

    it('should not load tasks if teamId is not provided', async () => {
      const wrapper = createWrapper(pinia, 0)
      await flushPromises()

      expect(teamApi.getTeamTasks).not.toHaveBeenCalled()
    })
  })

  describe('筛选功能集成', () => {
    it('should load team members on mount', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(teamApi.getTeamMembers).toHaveBeenCalledWith(123)
    })

    it('should use filter API when filters are active', async () => {
      const filteredTasks = [mockTasks[0]]
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue(filteredTasks)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      // 应用筛选条件
      await vm.handleFilterChange({ status: 'TODO' })
      await flushPromises()

      expect(teamApi.filterTeamTasks).toHaveBeenCalledWith(123, { status: 'TODO' })
      expect(vm.columns.TODO).toHaveLength(1)
    })

    it('should use regular API when no filters are active', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      vi.clearAllMocks()

      const vm = wrapper.vm as any
      
      // 清除筛选条件
      await vm.handleFilterChange({})
      await flushPromises()

      expect(teamApi.getTeamTasks).toHaveBeenCalledWith(123)
      expect(teamApi.filterTeamTasks).not.toHaveBeenCalled()
    })

    it('should filter by status', async () => {
      const filteredTasks = [mockTasks[1]] // DOING task
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue(filteredTasks)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({ status: 'DOING' })
      await flushPromises()

      expect(teamApi.filterTeamTasks).toHaveBeenCalledWith(123, { status: 'DOING' })
      expect(vm.columns.DOING).toHaveLength(1)
      expect(vm.columns.TODO).toHaveLength(0)
    })

    it('should filter by priority', async () => {
      const filteredTasks = [mockTasks[0]] // HIGH priority task
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue(filteredTasks)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({ priority: 'HIGH' })
      await flushPromises()

      expect(teamApi.filterTeamTasks).toHaveBeenCalledWith(123, { priority: 'HIGH' })
    })

    it('should filter by assignee', async () => {
      const filteredTasks = [mockTasks[0]]
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue(filteredTasks)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({ assigneeId: 1 })
      await flushPromises()

      expect(teamApi.filterTeamTasks).toHaveBeenCalledWith(123, { assigneeId: 1 })
    })

    it('should filter by keyword', async () => {
      const filteredTasks = [mockTasks[0]]
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue(filteredTasks)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({ keyword: '任务1' })
      await flushPromises()

      expect(teamApi.filterTeamTasks).toHaveBeenCalledWith(123, { keyword: '任务1' })
    })

    it('should filter by multiple criteria', async () => {
      const filteredTasks = [mockTasks[0]]
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue(filteredTasks)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({
        status: 'TODO',
        priority: 'HIGH',
        assigneeId: 1,
        keyword: '任务'
      })
      await flushPromises()

      expect(teamApi.filterTeamTasks).toHaveBeenCalledWith(123, {
        status: 'TODO',
        priority: 'HIGH',
        assigneeId: 1,
        keyword: '任务'
      })
    })

    it('should handle filter API error', async () => {
      const error = new Error('筛选失败')
      vi.mocked(teamApi.filterTeamTasks).mockRejectedValue(error)

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({ status: 'TODO' })
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('筛选失败')
    })

    it('should handle empty filter results', async () => {
      vi.mocked(teamApi.filterTeamTasks).mockResolvedValue([])

      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      const vm = wrapper.vm as any
      
      await vm.handleFilterChange({ status: 'REVIEW' })
      await flushPromises()

      expect(vm.columns.TODO).toHaveLength(0)
      expect(vm.columns.DOING).toHaveLength(0)
      expect(vm.columns.REVIEW).toHaveLength(0)
      expect(vm.columns.DONE).toHaveLength(0)
    })

    it('should reload team members when teamId changes', async () => {
      const wrapper = createWrapper(pinia, 123)
      await flushPromises()

      expect(teamApi.getTeamMembers).toHaveBeenCalledWith(123)

      // 改变 teamId
      await wrapper.setProps({ teamId: 456 })
      await flushPromises()

      expect(teamApi.getTeamMembers).toHaveBeenCalledWith(456)
      expect(teamApi.getTeamMembers).toHaveBeenCalledTimes(2)
    })
  })
})
