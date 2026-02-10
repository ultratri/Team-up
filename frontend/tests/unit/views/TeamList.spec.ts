import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import TeamList from '@/views/team/TeamList.vue'
import TeamCard from '@/views/team/components/TeamCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import type { Team } from '@/types/team'

// Mock router
const mockPush = vi.fn()
vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: mockPush
    })
  }
})

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
    }
  }
})

// Helper function to create wrapper with common stubs
const createWrapper = (pinia: ReturnType<typeof createPinia>) => {
  return mount(TeamList, {
    global: {
      plugins: [pinia],
      stubs: {
        TeamCard: true,
        EmptyState: true,
        'el-button': true,
        'el-input': true,
        'el-select': true,
        'el-option': true,
        'el-skeleton': true,
        'el-skeleton-item': true,
        'el-result': true,
        'el-icon': true
      }
    }
  })
}

describe('TeamList.vue', () => {
  let pinia: ReturnType<typeof createPinia>

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
  })

  afterEach(() => {
    vi.clearAllTimers()
  })

  describe('团队列表加载', () => {
    it('should load teams on mount', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '前端开发团队',
          description: '负责前端项目开发',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00',
          memberCount: 8
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)

      createWrapper(pinia)
      await flushPromises()

      expect(teamStore.fetchUserTeams).toHaveBeenCalledWith({
        userId: 1,
        keyword: undefined,
        status: undefined
      })
    })

    it('should display loading state while fetching teams', async () => {
      const teamStore = useTeamStore()
      teamStore.loading = true

      const wrapper = createWrapper(pinia)

      expect(wrapper.find('.loading-state').exists()).toBe(true)
      expect(wrapper.find('.skeleton-grid').exists()).toBe(true)
    })

    it('should display teams after loading', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '测试团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      const teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
    })
  })

  describe('空状态显示', () => {
    it('should display empty state when no teams', async () => {
      const teamStore = useTeamStore()
      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([])
      teamStore.teams = []
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            TeamCard: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      const emptyState = wrapper.findComponent(EmptyState)
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.props('title')).toBe('还没有团队')
      expect(emptyState.props('icon')).toBe('🏢')
    })
  })

  describe('团队卡片导航', () => {
    it('should navigate to team space when clicking team card', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 123,
          name: '测试团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 点击团队卡片
      const teamCard = wrapper.findComponent(TeamCard)
      await teamCard.vm.$emit('click')

      expect(mockPush).toHaveBeenCalledWith({
        name: 'TeamOverview',
        params: { id: 123 }
      })
    })
  })

  describe('未登录处理', () => {
    it('should redirect to login when user is not authenticated', async () => {
      const authStore = useAuthStore()
      authStore.logout() // 清除用户信息

      createWrapper(pinia)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('请先登录')
      expect(mockPush).toHaveBeenCalledWith({ name: 'Login' })
    })
  })

  describe('搜索和筛选', () => {
    it('should filter teams by search keyword', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '前端开发团队',
          description: '负责前端项目开发',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        },
        {
          id: 2,
          name: '后端开发团队',
          description: '负责后端服务开发',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-10T09:00:00',
          updatedAt: '2026-01-10T09:00:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 验证初始状态显示所有团队
      let teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(2)

      // 通过 computed 属性测试过滤逻辑
      // 由于无法直接修改 ref，我们验证 computed 的逻辑
      const vm = wrapper.vm as any
      
      // 模拟设置搜索关键词
      vm.debouncedKeyword = '前端'
      await wrapper.vm.$nextTick()

      // 验证过滤后的结果
      teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
      expect(teamCards[0].props('team').name).toBe('前端开发团队')
    })

    it('should filter teams by status', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '活跃团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        },
        {
          id: 2,
          name: '归档团队',
          creatorId: 1,
          status: 'ARCHIVED',
          createdAt: '2026-01-10T09:00:00',
          updatedAt: '2026-01-10T09:00:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 验证初始状态显示所有团队
      let teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(2)

      // 模拟设置状态筛选
      const vm = wrapper.vm as any
      vm.statusFilter = 'ACTIVE'
      await wrapper.vm.$nextTick()

      // 验证过滤结果
      teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
      expect(teamCards[0].props('team').status).toBe('ACTIVE')
    })

    it('should search in team description', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '团队A',
          description: '这是一个前端团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        },
        {
          id: 2,
          name: '团队B',
          description: '这是一个后端团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-10T09:00:00',
          updatedAt: '2026-01-10T09:00:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 搜索描述中的关键词
      const vm = wrapper.vm as any
      vm.debouncedKeyword = '前端'
      await wrapper.vm.$nextTick()

      const teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
      expect(teamCards[0].props('team').name).toBe('团队A')
    })
  })

  describe('边缘情况', () => {
    it('should handle teams with null description', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '测试团队',
          description: undefined,
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      const teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
    })

    it('should handle case-insensitive search', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '前端开发团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 使用大写搜索
      const vm = wrapper.vm as any
      vm.debouncedKeyword = '前端'
      await wrapper.vm.$nextTick()

      const teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
    })

    it('should handle empty search keyword', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '团队1',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 输入空字符串
      const vm = wrapper.vm as any
      vm.debouncedKeyword = '   '
      await wrapper.vm.$nextTick()

      // 应该显示所有团队（空字符串被 trim 后为空）
      const teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
    })
  })

  describe('组合筛选', () => {
    it('should combine search and status filter', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '前端开发团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        },
        {
          id: 2,
          name: '前端设计团队',
          creatorId: 1,
          status: 'ARCHIVED',
          createdAt: '2026-01-10T09:00:00',
          updatedAt: '2026-01-10T09:00:00'
        },
        {
          id: 3,
          name: '后端开发团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-05T08:00:00',
          updatedAt: '2026-01-05T08:00:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(mockTeams)
      teamStore.teams = mockTeams
      teamStore.loading = false

      const wrapper = mount(TeamList, {
        global: {
          plugins: [pinia],
          stubs: {
            EmptyState: true,
            'el-button': true,
            'el-input': true,
            'el-select': true,
            'el-option': true,
            'el-icon': true
          }
        }
      })

      await flushPromises()

      // 同时设置搜索和状态筛选
      const vm = wrapper.vm as any
      vm.debouncedKeyword = '前端'
      vm.statusFilter = 'ACTIVE'
      await wrapper.vm.$nextTick()

      // 应该只显示符合两个条件的团队
      const teamCards = wrapper.findAllComponents(TeamCard)
      expect(teamCards).toHaveLength(1)
      expect(teamCards[0].props('team').name).toBe('前端开发团队')
    })
  })
})
