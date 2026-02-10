import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import TaskCard from '@/components/team/TaskCard.vue'
import type { Task } from '@/types/team'
import { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar } from 'element-plus'

describe('TaskCard.vue', () => {
  const createMockTask = (overrides?: Partial<Task>): Task => ({
    id: 1,
    teamId: 1,
    title: '测试任务',
    description: '这是一个测试任务描述',
    status: 'TODO',
    priority: 'MEDIUM',
    deadline: '2026-02-01',
    createdBy: 1,
    createdAt: '2026-01-15T10:00:00',
    updatedAt: '2026-01-15T10:00:00',
    ...overrides
  })

  describe('Basic Display', () => {
    it('should display task title', () => {
      const task = createMockTask({ title: '完成前端开发' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('完成前端开发')
    })

    it('should display task description', () => {
      const task = createMockTask({ description: '实现用户界面' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('实现用户界面')
    })

    it('should display priority tag', () => {
      const task = createMockTask({ priority: 'HIGH' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('高')
    })

    it('should display deadline', () => {
      const task = createMockTask({ deadline: '2026-02-15' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const deadlineText = wrapper.find('.deadline')
      expect(deadlineText.exists()).toBe(true)
    })
  })

  describe('Assignee Display', () => {
    it('should display assignee avatars when assignees exist', () => {
      const task = createMockTask({
        assignees: [
          { id: 1, taskId: 1, userId: 1, userName: '张三', avatar: 'avatar1.jpg', assignedAt: '2026-01-15T10:00:00' },
          { id: 2, taskId: 1, userId: 2, userName: '李四', avatar: 'avatar2.jpg', assignedAt: '2026-01-15T10:00:00' }
        ]
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const avatars = wrapper.findAllComponents(ElAvatar)
      expect(avatars.length).toBeGreaterThan(0)
    })

    it('should display maximum 3 assignee avatars', () => {
      const task = createMockTask({
        assignees: [
          { id: 1, taskId: 1, userId: 1, userName: '张三', assignedAt: '2026-01-15T10:00:00' },
          { id: 2, taskId: 1, userId: 2, userName: '李四', assignedAt: '2026-01-15T10:00:00' },
          { id: 3, taskId: 1, userId: 3, userName: '王五', assignedAt: '2026-01-15T10:00:00' },
          { id: 4, taskId: 1, userId: 4, userName: '赵六', assignedAt: '2026-01-15T10:00:00' }
        ]
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const avatars = wrapper.findAll('.assignee-avatar')
      expect(avatars.length).toBe(3)
    })

    it('should display +N for remaining assignees when more than 3', () => {
      const task = createMockTask({
        assignees: [
          { id: 1, taskId: 1, userId: 1, userName: '张三', assignedAt: '2026-01-15T10:00:00' },
          { id: 2, taskId: 1, userId: 2, userName: '李四', assignedAt: '2026-01-15T10:00:00' },
          { id: 3, taskId: 1, userId: 3, userName: '王五', assignedAt: '2026-01-15T10:00:00' },
          { id: 4, taskId: 1, userId: 4, userName: '赵六', assignedAt: '2026-01-15T10:00:00' },
          { id: 5, taskId: 1, userId: 5, userName: '孙七', assignedAt: '2026-01-15T10:00:00' }
        ]
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('+2')
    })

    it('should not display assignees section when no assignees', () => {
      const task = createMockTask({ assignees: [] })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const assigneesSection = wrapper.find('.assignees')
      expect(assigneesSection.exists()).toBe(false)
    })

    it('should not display assignees section when assignees is undefined', () => {
      const task = createMockTask({ assignees: undefined })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const assigneesSection = wrapper.find('.assignees')
      expect(assigneesSection.exists()).toBe(false)
    })
  })

  describe('Comment and Attachment Count Display', () => {
    it('should display comment count when comments exist', () => {
      const task = createMockTask({ commentCount: 5 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('5')
      const counts = wrapper.find('.counts')
      expect(counts.exists()).toBe(true)
    })

    it('should display attachment count when attachments exist', () => {
      const task = createMockTask({ attachmentCount: 3 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('3')
      const counts = wrapper.find('.counts')
      expect(counts.exists()).toBe(true)
    })

    it('should display both comment and attachment counts', () => {
      const task = createMockTask({ commentCount: 5, attachmentCount: 3 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('5')
      expect(wrapper.text()).toContain('3')
      const countItems = wrapper.findAll('.count-item')
      expect(countItems.length).toBe(2)
    })

    it('should not display comment count when zero', () => {
      const task = createMockTask({ commentCount: 0 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const countItems = wrapper.findAll('.count-item')
      expect(countItems.length).toBe(0)
    })

    it('should not display attachment count when zero', () => {
      const task = createMockTask({ attachmentCount: 0 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const countItems = wrapper.findAll('.count-item')
      expect(countItems.length).toBe(0)
    })

    it('should not display counts when undefined', () => {
      const task = createMockTask({ commentCount: undefined, attachmentCount: undefined })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const countItems = wrapper.findAll('.count-item')
      expect(countItems.length).toBe(0)
    })
  })

  describe('Overdue Task Styling', () => {
    it('should apply overdue class when task is past deadline and not done', () => {
      const pastDate = new Date()
      pastDate.setDate(pastDate.getDate() - 5)
      const task = createMockTask({
        deadline: pastDate.toISOString().split('T')[0],
        status: 'TODO'
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).toContain('overdue')
    })

    it('should not apply overdue class when task is done', () => {
      const pastDate = new Date()
      pastDate.setDate(pastDate.getDate() - 5)
      const task = createMockTask({
        deadline: pastDate.toISOString().split('T')[0],
        status: 'DONE'
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).not.toContain('overdue')
    })

    it('should not apply overdue class when deadline is in future', () => {
      const futureDate = new Date()
      futureDate.setDate(futureDate.getDate() + 5)
      const task = createMockTask({
        deadline: futureDate.toISOString().split('T')[0],
        status: 'TODO'
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).not.toContain('overdue')
    })

    it('should not apply overdue class when no deadline', () => {
      const task = createMockTask({ deadline: undefined, status: 'TODO' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).not.toContain('overdue')
    })

    it('should apply overdue-text class to deadline when overdue', () => {
      const pastDate = new Date()
      pastDate.setDate(pastDate.getDate() - 5)
      const task = createMockTask({
        deadline: pastDate.toISOString().split('T')[0],
        status: 'TODO'
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const deadline = wrapper.find('.deadline')
      expect(deadline.classes()).toContain('overdue-text')
    })
  })

  describe('Priority Styling', () => {
    it('should apply priority-high class for HIGH priority', () => {
      const task = createMockTask({ priority: 'HIGH' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).toContain('priority-high')
    })

    it('should apply priority-medium class for MEDIUM priority', () => {
      const task = createMockTask({ priority: 'MEDIUM' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).toContain('priority-medium')
    })

    it('should apply priority-low class for LOW priority', () => {
      const task = createMockTask({ priority: 'LOW' })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      const card = wrapper.find('.task-card')
      expect(card.classes()).toContain('priority-low')
    })
  })

  describe('Event Emissions', () => {
    it('should emit update event when edit is clicked', async () => {
      const task = createMockTask()
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      // Directly call the component's handleEdit method
      await (wrapper.vm as any).handleEdit()

      expect(wrapper.emitted('update')).toBeTruthy()
      expect(wrapper.emitted('update')?.[0]).toEqual([task])
    })
  })

  describe('Edge Cases', () => {
    it('should handle task with all optional fields populated', () => {
      const task = createMockTask({
        assignees: [
          { id: 1, taskId: 1, userId: 1, userName: '张三', avatar: 'avatar1.jpg', assignedAt: '2026-01-15T10:00:00' }
        ],
        commentCount: 10,
        attachmentCount: 5,
        deadline: '2026-03-01'
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.find('.assignees').exists()).toBe(true)
      expect(wrapper.text()).toContain('10')
      expect(wrapper.text()).toContain('5')
      expect(wrapper.find('.deadline').exists()).toBe(true)
    })

    it('should handle task with no optional fields', () => {
      const task = createMockTask({
        assignees: undefined,
        commentCount: undefined,
        attachmentCount: undefined,
        deadline: undefined
      })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.find('.assignees').exists()).toBe(false)
      expect(wrapper.findAll('.count-item').length).toBe(0)
      expect(wrapper.find('.deadline').exists()).toBe(false)
    })

    it('should handle very long task title', () => {
      const longTitle = 'A'.repeat(200)
      const task = createMockTask({ title: longTitle })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain(longTitle)
    })

    it('should handle very long task description', () => {
      const longDesc = 'B'.repeat(500)
      const task = createMockTask({ description: longDesc })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain(longDesc)
    })

    it('should handle large comment count', () => {
      const task = createMockTask({ commentCount: 999 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('999')
    })

    it('should handle large attachment count', () => {
      const task = createMockTask({ attachmentCount: 999 })
      const wrapper = mount(TaskCard, {
        props: { task },
        global: {
          components: { ElCard, ElTag, ElIcon, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar }
        }
      })

      expect(wrapper.text()).toContain('999')
    })
  })
})
