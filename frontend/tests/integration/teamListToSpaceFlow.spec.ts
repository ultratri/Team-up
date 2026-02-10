import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import TeamList from '@/views/team/TeamList.vue'
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
  getTeamMembers: vi.fn(),
  createTeam: vi.fn()
}))

vi.mock('@/api/project', () => ({
  getProjects: vi.fn()
}))

/**
 * 集成测试: 团队列表到团队空间的完整流程
 * 
 * 测试用户从团队列表页面导航到团队空间的完整流程：
 * 1. 加载团队列表
 * 2. 点击团队卡片
 * 3. 导航到团队空间
 * 4. 加载团队详情
 * 5. 显示团队信息
 */
describe('Integration: Team List to Team Space Flow', () => {
  let router: any
  let teamStore: any
  let authStore: any

  const mockTeams = [
    {
      id: 1,
      name: 'Frontend Team',
      description: 'Frontend development team',
      creatorId: 1,
      status: 'ACTIVE',
      memberCount: 5,
      createdAt: '2026-01-15T10:00:00',
      updatedAt: '2026-01-15T10:00:00'
    },
    {
      id: 2,
      name: 'Backend Team',
      description: 'Backend development team',
      creatorId: 1,
      status: 'ACTIVE',
      memberCount: 3,
      createdAt: '2026-01-16T10:00:00',
      updatedAt: '2026-01-16T10:00:00'
    }
  ]

  const mockTeamDetail = {
    id: 1,
    name: 'Frontend Team',
    description: 'Frontend development team',
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
  }

  const mockMembers = [
    {
      id: 1,
      teamId: 1,
      userId: 1,
      role: 'OWNER',
      username: 'testuser',
      avatar: 'avatar1.jpg',
      joinedAt: '2026-01-15T10:00:00'
    },
    {
      id: 2,
      teamId: 1,
      userId: 2,
      role: 'MEMBER',
      username: 'member1',
      avatar: 'avatar2.jpg',
      joinedAt: '2026-01-16T10:00:00'
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
          path: '/team',
          name: 'TeamList',
          component: TeamList
        },
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
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeamDetail)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    // Clear mocks
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('should navigate from team list to team space', async () => {
    // Step 1: Start at team list
    await router.push('/team')
    await router.isReady()

    const listWrapper = mount(TeamList, {
      global: {
        plugins: [router],
        stubs: {
          TeamCard: false,
          CreateTeamDialog: true,
          EmptyState: true,
          ElSkeleton: true
        }
      }
    })

    await flushPromises()

    // Verify teams are loaded
    expect(teamApi.getUserTeams).toHaveBeenCalledWith(1, expect.any(Object))
    expect(listWrapper.vm.teams.length).toBe(2)

    // Step 2: Click on first team card
    const teamId = mockTeams[0].id
    await listWrapper.vm.handleTeamClick(teamId)

    // Step 3: Verify navigation occurred
    expect(router.currentRoute.value.path).toBe(`/team/${teamId}`)

    listWrapper.unmount()
  })

  it('should load team details after navigation', async () => {
    // Navigate directly to team space
    await router.push('/team/1')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
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

    // Verify team details are loaded
    expect(teamApi.getTeam).toHaveBeenCalledWith(1)
    expect(teamApi.getTeamMembers).toHaveBeenCalledWith(1)

    // Verify store is updated
    expect(teamStore.currentTeam).toBeTruthy()
    expect(teamStore.currentTeam?.id).toBe(1)
    expect(teamStore.currentTeamMembers.length).toBe(2)

    spaceWrapper.unmount()
  })

  it('should maintain team context when switching modules', async () => {
    await router.push('/team/1')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
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

    // Verify initial team is loaded
    expect(teamStore.currentTeam?.id).toBe(1)

    // Navigate to overview
    await router.push('/team/1/overview')
    await flushPromises()

    // Team context should be maintained
    expect(teamStore.currentTeam?.id).toBe(1)

    spaceWrapper.unmount()
  })

  it('should handle team not found error', async () => {
    // Mock 404 error
    vi.mocked(teamApi.getTeam).mockRejectedValue({
      response: { status: 404 }
    })

    await router.push('/team/999')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
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

    // Verify error is handled
    expect(spaceWrapper.vm.error).toBeTruthy()
    expect(spaceWrapper.vm.error).toContain('不存在')

    spaceWrapper.unmount()
  })

  it('should handle permission denied error', async () => {
    // Mock 403 error
    vi.mocked(teamApi.getTeam).mockRejectedValue({
      response: { status: 403 }
    })

    await router.push('/team/1')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
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

    // Verify error is handled
    expect(spaceWrapper.vm.error).toBeTruthy()
    expect(spaceWrapper.vm.error).toContain('权限')

    spaceWrapper.unmount()
  })

  it('should reload team data when team ID changes', async () => {
    await router.push('/team/1')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
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

    // Verify first team is loaded
    expect(teamApi.getTeam).toHaveBeenCalledWith(1)
    expect(teamStore.currentTeam?.id).toBe(1)

    // Clear mocks
    vi.clearAllMocks()

    // Mock second team
    const mockTeam2 = {
      ...mockTeamDetail,
      id: 2,
      name: 'Backend Team'
    }
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam2)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    // Navigate to second team
    await router.push('/team/2')
    await flushPromises()

    // Verify second team is loaded
    expect(teamApi.getTeam).toHaveBeenCalledWith(2)
    expect(teamStore.currentTeam?.id).toBe(2)

    spaceWrapper.unmount()
  })

  it('should display team information in header', async () => {
    await router.push('/team/1')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: false,
          RouterView: true
        }
      }
    })

    await flushPromises()

    // Verify team name is displayed
    expect(spaceWrapper.text()).toContain('Frontend Team')

    spaceWrapper.unmount()
  })

  it('should show loading state while fetching team data', async () => {
    // Mock slow API response
    vi.mocked(teamApi.getTeam).mockImplementation(() => 
      new Promise(resolve => setTimeout(() => resolve(mockTeamDetail), 100))
    )

    await router.push('/team/1')
    await router.isReady()

    const spaceWrapper = mount(TeamSpace, {
      global: {
        plugins: [router],
        stubs: {
          ElMenu: true,
          ElMenuItem: true,
          ElDropdown: true,
          ElAvatar: true,
          RouterView: true,
          ElSkeleton: false
        }
      }
    })

    // Verify loading state
    expect(spaceWrapper.vm.loading).toBe(true)

    await flushPromises()

    // Verify loading is complete
    expect(spaceWrapper.vm.loading).toBe(false)

    spaceWrapper.unmount()
  })
})
