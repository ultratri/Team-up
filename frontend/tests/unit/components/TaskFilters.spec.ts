import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TaskFilters from '@/components/team/TaskFilters.vue'
import type { TaskAssignee } from '@/types/team'

// Helper function to create wrapper with common stubs
const createWrapper = (props: any = {}) => {
  return mount(TaskFilters, {
    props,
    global: {
      stubs: {
        'el-input': {
          template: '<input v-model="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
          props: ['modelValue', 'placeholder', 'prefixIcon', 'clearable'],
          emits: ['update:modelValue']
        },
        'el-select': {
          template: '<select v-model="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
          props: ['modelValue', 'placeholder', 'clearable', 'filterable'],
          emits: ['update:modelValue']
        },
        'el-option': {
          template: '<option :value="value"><slot /></option>',
          props: ['label', 'value']
        },
        'el-button': {
          template: '<button @click="$emit(\'click\')"><slot /></button>',
          props: ['icon'],
          emits: ['click']
        },
        'el-avatar': true,
        'el-icon': true
      }
    }
  })
}

describe('TaskFilters.vue', () => {
  const mockTeamMembers: TaskAssignee[] = [
    {
      id: 1,
      taskId: 0,
      userId: 1,
      userName: '张三',
      avatar: 'avatar1.jpg',
      assignedAt: ''
    },
    {
      id: 2,
      taskId: 0,
      userId: 2,
      userName: '李四',
      avatar: 'avatar2.jpg',
      assignedAt: ''
    },
    {
      id: 3,
      taskId: 0,
      userId: 3,
      userName: '王五',
      avatar: 'avatar3.jpg',
      assignedAt: ''
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('组件渲染', () => {
    it('should render all filter controls', () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      // 验证搜索输入框存在
      const searchInput = wrapper.find('input')
      expect(searchInput.exists()).toBe(true)

      // 验证下拉选择器存在
      const selects = wrapper.findAll('select')
      expect(selects.length).toBeGreaterThanOrEqual(3) // 状态、优先级、负责人

      // 验证重置按钮存在
      const resetButton = wrapper.find('button')
      expect(resetButton.exists()).toBe(true)
    })

    it('should render team members in assignee dropdown', () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const options = wrapper.findAll('option')
      
      // 验证团队成员选项存在
      const memberNames = options.map(opt => opt.text())
      expect(memberNames.some(name => name.includes('张三'))).toBe(true)
      expect(memberNames.some(name => name.includes('李四'))).toBe(true)
      expect(memberNames.some(name => name.includes('王五'))).toBe(true)
    })

    it('should render without team members', () => {
      const wrapper = createWrapper({ teamMembers: [] })

      // 应该正常渲染，不抛出错误
      expect(wrapper.exists()).toBe(true)
    })

    it('should render with undefined team members', () => {
      const wrapper = createWrapper({ teamMembers: undefined })

      // 应该正常渲染，不抛出错误
      expect(wrapper.exists()).toBe(true)
    })
  })

  describe('筛选条件变化', () => {
    it('should emit filter-change when keyword changes', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.keyword = '测试任务'
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.keyword).toBe('测试任务')
    })

    it('should emit filter-change when status changes', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.status = 'DOING'
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.status).toBe('DOING')
    })

    it('should emit filter-change when priority changes', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.priority = 'HIGH'
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.priority).toBe('HIGH')
    })

    it('should emit filter-change when assignee changes', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.assigneeId = 1
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.assigneeId).toBe(1)
    })

    it('should emit filter-change with multiple filters', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.status = 'TODO'
      vm.filters.priority = 'HIGH'
      vm.filters.assigneeId = 2
      vm.filters.keyword = '重要任务'
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.status).toBe('TODO')
      expect(emittedFilters.priority).toBe('HIGH')
      expect(emittedFilters.assigneeId).toBe(2)
      expect(emittedFilters.keyword).toBe('重要任务')
    })

    it('should not include empty filters in emitted event', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.status = 'TODO'
      vm.filters.priority = undefined
      vm.filters.assigneeId = undefined
      vm.filters.keyword = ''
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.status).toBe('TODO')
      expect(emittedFilters.priority).toBeUndefined()
      expect(emittedFilters.assigneeId).toBeUndefined()
      expect(emittedFilters.keyword).toBeUndefined()
    })

    it('should trim keyword before emitting', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.keyword = '  测试任务  '
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.keyword).toBe('测试任务')
    })

    it('should not emit keyword if only whitespace', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.keyword = '   '
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.keyword).toBeUndefined()
    })
  })

  describe('重置筛选', () => {
    it('should reset all filters when reset button is clicked', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      
      // 设置一些筛选条件
      vm.filters.status = 'DOING'
      vm.filters.priority = 'HIGH'
      vm.filters.assigneeId = 1
      vm.filters.keyword = '测试'
      await wrapper.vm.$nextTick()

      // 点击重置按钮
      const resetButton = wrapper.find('button')
      await resetButton.trigger('click')
      await wrapper.vm.$nextTick()
      await flushPromises()

      // 验证所有筛选条件被重置
      expect(vm.filters.status).toBeUndefined()
      expect(vm.filters.priority).toBeUndefined()
      expect(vm.filters.assigneeId).toBeUndefined()
      expect(vm.filters.keyword).toBe('')
    })

    it('should emit filter-change with empty filters after reset', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      
      // 设置一些筛选条件
      vm.filters.status = 'DOING'
      vm.filters.priority = 'HIGH'
      await wrapper.vm.$nextTick()
      await flushPromises()

      // 清除之前的事件
      wrapper.vm.$emit('filter-change', {})
      
      // 点击重置按钮
      await vm.handleReset()
      await wrapper.vm.$nextTick()
      await flushPromises()

      // 验证发射了空的筛选条件
      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const lastEmitted = wrapper.emitted('filter-change')?.slice(-1)[0]?.[0] as any
      expect(Object.keys(lastEmitted).length).toBe(0)
    })

    it('should reset filters multiple times', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      
      // 第一次设置和重置
      vm.filters.status = 'TODO'
      await vm.handleReset()
      expect(vm.filters.status).toBeUndefined()

      // 第二次设置和重置
      vm.filters.priority = 'HIGH'
      await vm.handleReset()
      expect(vm.filters.priority).toBeUndefined()

      // 第三次设置和重置
      vm.filters.keyword = '测试'
      await vm.handleReset()
      expect(vm.filters.keyword).toBe('')
    })
  })

  describe('边缘情况', () => {
    it('should handle rapid filter changes', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      
      // 快速改变多个筛选条件
      vm.filters.status = 'TODO'
      vm.filters.status = 'DOING'
      vm.filters.status = 'DONE'
      await wrapper.vm.$nextTick()
      await flushPromises()

      // 应该发射最后的状态
      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const lastEmitted = wrapper.emitted('filter-change')?.slice(-1)[0]?.[0] as any
      expect(lastEmitted.status).toBe('DONE')
    })

    it('should handle null team members', () => {
      const wrapper = createWrapper({ teamMembers: null })

      // 应该正常渲染，不抛出错误
      expect(wrapper.exists()).toBe(true)
    })

    it('should handle very long keyword', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      const longKeyword = 'a'.repeat(1000)
      vm.filters.keyword = longKeyword
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.keyword).toBe(longKeyword)
    })

    it('should handle special characters in keyword', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      const specialKeyword = '测试<script>alert("xss")</script>'
      vm.filters.keyword = specialKeyword
      await wrapper.vm.$nextTick()
      await flushPromises()

      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.keyword).toBe(specialKeyword)
    })

    it('should handle invalid assigneeId', async () => {
      const wrapper = createWrapper({ teamMembers: mockTeamMembers })

      const vm = wrapper.vm as any
      vm.filters.assigneeId = -1
      await wrapper.vm.$nextTick()
      await flushPromises()

      // 应该仍然发射事件，由后端验证
      expect(wrapper.emitted('filter-change')).toBeTruthy()
      const emittedFilters = wrapper.emitted('filter-change')?.[0]?.[0] as any
      expect(emittedFilters.assigneeId).toBe(-1)
    })
  })
})
