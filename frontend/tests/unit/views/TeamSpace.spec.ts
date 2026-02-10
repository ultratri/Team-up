import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import TeamSpace from '@/views/team/TeamSpace.vue'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import type { Team, TeamMember } from '@/types/team'

// Mock router
const mockPush = vi.fn()
const mockRoute = {
  params: { id: '123' },
  name: 'TeamOverview'
}

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({
      push: mockPush
    }),
    useRoute: () => mockRoute
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
  return mount(TeamSpace, {
    global: {
      plugins: [pinia],
      stubs: {
        'router-view': true,
        'el-container': true,
        'el-header': true,
        'el-aside': true,
        'el-main': true,
        'el-menu': true,
        'el-menu-item': true,
        'el-avatar': true,
        'el-button': true,
        'el-dropdown': true,
        'el-dropdown-menu': true,
        'el-dropdown-item': true,
        'el-tag': true,
        'el-icon': true,
        'el-skeleton': true,
        'el-result': true
      }
    }
  })
}

describe('TeamSpace.vue', () => {
  let pinia: ReturnType<typeof createPinia>

  // Helper function to mock fetchTeamDetail with proper store updates
  const mockFetchTeamDetail = (teamStore: any, team: Team, members: TeamMember[] = []) => {
    vi.spyOn(teamStore, 'fetchTeamDetail').mockImplementation(async () => {
      teamStore.currentTeam = team
      teamStore.currentTeamMembers = members
      teamStore.loading = false
      return { team, members }
    })
  }

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

  describe('从路由获取团队 ID', () => {
    it('should get team ID from route params', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      expect(teamStore.fetchTeamDetail).toHaveBeenCalledWith(123)
    })

    it('should handle invalid team ID', async () => {
      mockRoute.params.id = 'invalid'

      createWrapper(pinia)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('无效的团队 ID')
      expect(mockPush).toHaveBeenCalledWith({ name: 'TeamList' })
    })

    it('should handle missing team ID', async () => {
      mockRoute.params.id = ''

      createWrapper(pinia)
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('无效的团队 ID')
      expect(mockPush).toHaveBeenCalledWith({ name: 'TeamList' })
    })
  })

  describe('团队数据加载', () => {
    it('should load team data on mount', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '前端开发团队',
        description: '负责前端项目开发',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00',
        memberCount: 8
      }

      const mockMembers: TeamMember[] = [
        {
          id: 1,
          teamId: 123,
          userId: 1,
          role: 'OWNER',
          username: 'testuser',
          joinedAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: mockMembers
      })

      mockRoute.params.id = '123'

      createWrapper(pinia)
      await flushPromises()

      expect(teamStore.fetchTeamDetail).toHaveBeenCalledWith(123)
    })

    it('should display loading state while fetching team', async () => {
      const teamStore = useTeamStore()
      teamStore.loading = true

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)

      expect(wrapper.find('.loading-container').exists()).toBe(true)
    })

    it('should display team info after loading', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      // Mock fetchTeamDetail to update store state
      vi.spyOn(teamStore, 'fetchTeamDetail').mockImplementation(async () => {
        teamStore.currentTeam = mockTeam
        teamStore.currentTeamMembers = []
        teamStore.loading = false
        return { team: mockTeam, members: [] }
      })

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('测试团队')
    })

    it('should load user teams if teams list is empty', async () => {
      const teamStore = useTeamStore()
      const authStore = useAuthStore()
      
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })
      vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([mockTeam])

      teamStore.teams = [] // 空团队列表

      mockRoute.params.id = '123'

      createWrapper(pinia)
      await flushPromises()

      expect(teamStore.fetchUserTeams).toHaveBeenCalledWith({
        userId: authStore.user?.id
      })
    })
  })

  describe('菜单导航', () => {
    it('should navigate to overview when selecting overview menu', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      // 触发菜单选择
      const vm = wrapper.vm as any
      vm.handleMenuSelect('overview')

      expect(mockPush).toHaveBeenCalledWith({
        name: 'TeamOverview',
        params: { id: 123 }
      })
    })

    it('should navigate to tasks when selecting tasks menu', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      const vm = wrapper.vm as any
      vm.handleMenuSelect('tasks')

      expect(mockPush).toHaveBeenCalledWith({
        name: 'TeamTasks',
        params: { id: 123 }
      })
    })

    it('should navigate to members when selecting members menu', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      const vm = wrapper.vm as any
      vm.handleMenuSelect('members')

      expect(mockPush).toHaveBeenCalledWith({
        name: 'TeamMembers',
        params: { id: 123 }
      })
    })

    it('should highlight active menu based on route name', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.loading = false

      mockRoute.params.id = '123'
      mockRoute.name = 'TeamTasks'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.activeMenu).toBe('tasks')
    })
  })

  describe('团队切换', () => {
    it('should switch to another team', async () => {
      const teamStore = useTeamStore()
      const mockTeams: Team[] = [
        {
          id: 123,
          name: '团队A',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-15T10:30:00',
          updatedAt: '2026-01-15T10:30:00'
        },
        {
          id: 456,
          name: '团队B',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-10T09:00:00',
          updatedAt: '2026-01-10T09:00:00'
        }
      ]

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeams[0],
        members: []
      })

      teamStore.currentTeam = mockTeams[0]
      teamStore.teams = mockTeams
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      // 切换到团队B
      const vm = wrapper.vm as any
      vm.handleTeamSwitch(456)

      expect(mockPush).toHaveBeenCalledWith({
        name: 'TeamOverview',
        params: { id: 456 }
      })
    })

    it('should navigate to team list when selecting view all', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      const vm = wrapper.vm as any
      vm.handleTeamSwitch('view-all')

      expect(mockPush).toHaveBeenCalledWith({ name: 'TeamList' })
    })
  })

  describe('错误处理', () => {
    it('should display error when team loading fails', async () => {
      const teamStore = useTeamStore()
      
      vi.spyOn(teamStore, 'fetchTeamDetail').mockImplementation(async () => {
        teamStore.error = '加载团队失败'
        teamStore.loading = false
        throw new Error('加载团队失败')
      })

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('加载失败')
    })

    it('should handle 403 error (forbidden)', async () => {
      const teamStore = useTeamStore()
      
      const error = new Error('Forbidden')
      ;(error as any).response = { status: 403 }
      
      vi.spyOn(teamStore, 'fetchTeamDetail').mockRejectedValue(error)

      mockRoute.params.id = '123'

      createWrapper(pinia)
      await flushPromises()

      // 错误处理在路由守卫中，这里只验证 store 调用
      expect(teamStore.fetchTeamDetail).toHaveBeenCalledWith(123)
    })

    it('should handle 404 error (not found)', async () => {
      const teamStore = useTeamStore()
      
      const error = new Error('Not Found')
      ;(error as any).response = { status: 404 }
      
      vi.spyOn(teamStore, 'fetchTeamDetail').mockRejectedValue(error)

      mockRoute.params.id = '123'

      createWrapper(pinia)
      await flushPromises()

      expect(teamStore.fetchTeamDetail).toHaveBeenCalledWith(123)
    })

    it('should allow retry after error', async () => {
      const teamStore = useTeamStore()
      
      vi.spyOn(teamStore, 'fetchTeamDetail')
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce({
          team: {
            id: 123,
            name: '测试团队',
            creatorId: 1,
            status: 'ACTIVE',
            createdAt: '2026-01-15T10:30:00',
            updatedAt: '2026-01-15T10:30:00'
          },
          members: []
        })

      teamStore.error = 'Network error'
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      // 点击重试按钮
      const vm = wrapper.vm as any
      await vm.loadTeamData()

      expect(teamStore.fetchTeamDetail).toHaveBeenCalledTimes(2)
    })
  })

  describe('路由参数监听', () => {
    it('should reload team data when team ID changes', async () => {
      const teamStore = useTeamStore()
      
      const mockTeam1: Team = {
        id: 123,
        name: '团队A',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      const mockTeam2: Team = {
        id: 456,
        name: '团队B',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-10T09:00:00',
        updatedAt: '2026-01-10T09:00:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail')
        .mockResolvedValueOnce({ team: mockTeam1, members: [] })
        .mockResolvedValueOnce({ team: mockTeam2, members: [] })

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      expect(teamStore.fetchTeamDetail).toHaveBeenCalledWith(123)

      // 改变路由参数
      mockRoute.params.id = '456'
      
      // 手动触发 watch（在实际应用中由路由变化触发）
      const vm = wrapper.vm as any
      await vm.loadTeamData()

      expect(teamStore.fetchTeamDetail).toHaveBeenCalledWith(456)
    })
  })

  describe('边缘情况', () => {
    it('should handle team with no avatar', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        avatar: undefined,
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.currentTeamMembers = []
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      // 应该显示团队名称首字母
      expect(wrapper.text()).toContain('测')
    })

    it('should handle team with no members', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.currentTeamMembers = []
      teamStore.loading = false

      mockRoute.params.id = '123'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      expect(wrapper.text()).toContain('0 名成员')
    })

    it('should handle unknown route name', async () => {
      const teamStore = useTeamStore()
      const mockTeam: Team = {
        id: 123,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-15T10:30:00',
        updatedAt: '2026-01-15T10:30:00'
      }

      vi.spyOn(teamStore, 'fetchTeamDetail').mockResolvedValue({
        team: mockTeam,
        members: []
      })

      teamStore.currentTeam = mockTeam
      teamStore.loading = false

      mockRoute.params.id = '123'
      mockRoute.name = 'UnknownRoute'

      const wrapper = createWrapper(pinia)
      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.activeMenu).toBe('overview') // 默认值
    })
  })
})
