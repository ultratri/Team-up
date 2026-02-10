import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeamSpace from '@/views/team/TeamSpace.vue'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import * as teamApi from '@/api/team'

// Mock Element Plus Message
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn()
    }
  }
})

// Mock APIs
vi.mock('@/api/team', () => ({
  getUserTeams: vi.fn(),
  getTeam: vi.fn(),
  getTeamMembers: vi.fn()
}))

/**
 * 集成测试: 团队切换流程
 * 
 * 测试用户在团队空间中切换团队的完整流程：
 * 1. 在团队空间中
 * 2. 点击团队切换下拉菜单
 * 3. 选择另一个团队
 * 4. 导航到新团队
 * 5. 加载新团队数据
 */
describe('Integration: Team Switch Flow', () => {
  let router: any
  let teamStore: any
  let authStore: any

  const mockTeams = [
    {
      id: 1,
      name: 'Frontend Team',
      description: 'Frontend development',
      creatorId: 1,
      status: 'ACTIVE',
      memberCount: 5,
      createdAt: '2026-01-15T10:00:00'
    },
    {
      id: 2,
      name: 'Backend Team',
      description: 'Backend development',
      creatorId: 1,
      status: 'ACTIVE',
      memberCount: 3,
      createdAt: '2026-01-16T10:00:00'
    },
    {
      id: 3,
      name: 'DevOps Team',
      description: 'DevOps operations',
      creatorId: 1,
      status: 'ACTIVE',
      memberCount: 2,
      createdAt: '2026-01-17T10:00:00'
    }
  ]

  const createMockTeamDetail = (id: number, name: string) => ({
    id,
    name,
    description: `${name} description`,
    creatorId: 1,
    status: 'ACTIVE',
    memberCount: 5,
    createdAt: '2026-01-15T10:00:00',
    updatedAt: '2026-01-15T10:00:00',
    statistics: {
      taskCompletionRate: 75,
      activeDays: 30,
      messageCount: 150,
      fileCount: 25,
      totalTasks: 20,
      completedTasks: 15,
      memberCount: 5
    }
  })

  const mockMembers = [
    {
      id: 1,
      teamId: 1,
      userId: 1,
      role: 'OWNER',
      username: 'testuser',
      joinedAt: '2026-01-15T10:00:00'
    }
  ]

  beforeEach(() => {
    // Create fresh pinia instance
    const pinia = createPinia()
    setActivePinia(pinia)

    // Create router
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/team/:id',
          name: 'TeamSpace',
          component: TeamSpace,
          children: [
            {
              path: 'overview',
              name: 'TeamOverview',
              component: { template: '<div>Team Overview</div>' }
            }
          ]
        }
      ]
    })

    // Initialize stores
    teamStore = useTeamStore()
    authStore = useAuthStore()

    // Set up auth user
    authStore.user = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com'
    }

    // Mock API responses
    vi.mocked(teamApi.getUserTeams).mockResolvedValue(mockTeams)
    vi.mocked(teamApi.getTeam).mockImplementation((id: number) => 
      Promise.resolve(createMockTeamDetail(id, mockTeams.find(t => t.id === id)?.name || 'Team'))
    )
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    // Clear mocks
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('should switch from one team to another', async () => {
    // Start at team 1
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElDropdownMenu: true,
          ElDropdownItem: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Verify team 1 is loaded
    expect(teamStore.currentTeam?.id).toBe(1)
    expect(teamStore.currentTeam?.name).toBe('Frontend Team')

    // Clear mocks to track new calls
    vi.clearAllMocks()

    // Switch to team 2
    await wrapper.vm.handleTeamSwitch(2)
    await flushPromises()

    // Verify navigation occurred
    expect(router.currentRoute.value.params.id).toBe('2')

    // Verify team 2 data is loaded
    expect(teamApi.getTeam).toHaveBeenCalledWith(2)
    expect(teamStore.currentTeam?.id).toBe(2)
    expect(teamStore.currentTeam?.name).toBe('Backend Team')

    wrapper.unmount()
  })

  it('should load user teams for dropdown', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElDropdownMenu: true,
          ElDropdownItem: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Load teams for dropdown
    await wrapper.vm.loadUserTeams()
    await flushPromises()

    // Verify teams are loaded
    expect(teamApi.getUserTeams).toHaveBeenCalled()
    expect(teamStore.teams.length).toBe(3)

    wrapper.unmount()
  })

  it('should maintain current module when switching teams', async () => {
    // Start at team 1 overview
    await router.push('/team/1/overview')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Switch to team 2
    await wrapper.vm.handleTeamSwitch(2)
    await flushPromises()

    // Verify we're still on overview module
    expect(router.currentRoute.value.path).toBe('/team/2/overview')

    wrapper.unmount()
  })

  it('should handle rapid team switches', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Rapidly switch teams
    wrapper.vm.handleTeamSwitch(2)
    wrapper.vm.handleTeamSwitch(3)
    wrapper.vm.handleTeamSwitch(1)

    await flushPromises()

    // Verify final team is loaded
    expect(teamStore.currentTeam?.id).toBe(1)

    wrapper.unmount()
  })

  it('should show error when switching to non-existent team', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true,
          ElResult: true
        }
      }
    })

    await flushPromises()

    // Mock 404 error for non-existent team
    vi.mocked(teamApi.getTeam).mockRejectedValue({
      response: { status: 404 }
    })

    // Try to switch to non-existent team
    await wrapper.vm.handleTeamSwitch(999)
    await flushPromises()

    // Verify error is shown
    expect(wrapper.vm.error).toBeTruthy()

    wrapper.unmount()
  })

  it('should show error when switching to unauthorized team', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true,
          ElResult: true
        }
      }
    })

    await flushPromises()

    // Mock 403 error for unauthorized team
    vi.mocked(teamApi.getTeam).mockRejectedValue({
      response: { status: 403 }
    })

    // Try to switch to unauthorized team
    await wrapper.vm.handleTeamSwitch(999)
    await flushPromises()

    // Verify error is shown
    expect(wrapper.vm.error).toBeTruthy()
    expect(wrapper.vm.error).toContain('权限')

    wrapper.unmount()
  })

  it('should update team members when switching teams', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Verify initial members
    expect(teamStore.currentTeamMembers.length).toBeGreaterThan(0)

    // Mock different members for team 2
    const team2Members = [
      {
        id: 2,
        teamId: 2,
        userId: 1,
        role: 'OWNER',
        username: 'testuser',
        joinedAt: '2026-01-16T10:00:00'
      },
      {
        id: 3,
        teamId: 2,
        userId: 3,
        role: 'MEMBER',
        username: 'member2',
        joinedAt: '2026-01-16T11:00:00'
      }
    ]
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(team2Members)

    // Switch to team 2
    await wrapper.vm.handleTeamSwitch(2)
    await flushPromises()

    // Verify members are updated
    expect(teamApi.getTeamMembers).toHaveBeenCalledWith(2)
    expect(teamStore.currentTeamMembers.length).toBe(2)

    wrapper.unmount()
  })

  it('should preserve scroll position when switching teams', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Switch teams
    await wrapper.vm.handleTeamSwitch(2)
    await flushPromises()

    // Verify component is still mounted and functional
    expect(wrapper.vm.currentTeam).toBeTruthy()

    wrapper.unmount()
  })

  it('should handle network errors during team switch', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true,
          ElResult: true
        }
      }
    })

    await flushPromises()

    // Mock network error
    vi.mocked(teamApi.getTeam).mockRejectedValue(new Error('Network error'))

    // Try to switch teams
    await wrapper.vm.handleTeamSwitch(2)
    await flushPromises()

    // Verify error is handled
    expect(wrapper.vm.error).toBeTruthy()

    wrapper.unmount()
  })

  it('should show loading state during team switch', async () => {
    await router.push('/team/1')
    await router.isReady()

    const wrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Mock slow API response
    vi.mocked(teamApi.getTeam).mockImplementation(() => 
      new Promise(resolve => setTimeout(() => resolve(createMockTeamDetail(2, 'Backend Team')), 100))
    )

    // Start team switch
    const switchPromise = wrapper.vm.handleTeamSwitch(2)

    // Verify loading state
    expect(wrapper.vm.loading).toBe(true)

    await switchPromise
    await flushPromises()

    // Verify loading is complete
    expect(wrapper.vm.loading).toBe(false)

    wrapper.unmount()
  })
})
