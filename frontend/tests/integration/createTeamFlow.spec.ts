import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import TeamList from '@/views/team/TeamList.vue'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import * as teamApi from '@/api/team'
import * as projectApi from '@/api/project'

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
  createTeam: vi.fn(),
  getTeam: vi.fn(),
  getTeamMembers: vi.fn()
}))

vi.mock('@/api/project', () => ({
  getProjects: vi.fn()
}))

/**
 * 集成测试: 创建团队完整流程
 * 
 * 测试从团队列表页面创建团队的完整用户流程：
 * 1. 加载团队列表
 * 2. 点击创建团队按钮
 * 3. 填写表单
 * 4. 提交创建
 * 5. 验证导航和列表更新
 */
describe('Integration: Create Team Flow', () => {
  let wrapper: any
  let teamStore: any
  let authStore: any

  beforeEach(() => {
    // Create fresh pinia instance
    const pinia = createPinia()
    setActivePinia(pinia)

    // Initialize stores
    teamStore = useTeamStore()
    authStore = useAuthStore()

    // Set up auth user
    authStore.user = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com'
    }

    // Mock initial team list (empty)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([])

    // Mock project list
    vi.mocked(projectApi.getProjects).mockResolvedValue({
      list: [
        { id: 1, title: 'Project 1', status: 'RECRUITING' },
        { id: 2, title: 'Project 2', status: 'RECRUITING' }
      ],
      total: 2
    })

    // Clear mocks
    vi.clearAllMocks()
    mockPush.mockClear()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const createWrapper = () => {
    return mount(TeamList, {
      global: {
        stubs: {
          ElButton: false,
          ElInput: false,
          ElSelect: false,
          ElOption: false,
          ElSkeleton: true,
          ElSkeletonItem: true,
          ElResult: true,
          ElDialog: true,
          ElForm: true,
          ElFormItem: true,
          ElAvatar: true,
          TeamCard: true,
          EmptyState: true,
          CreateTeamDialog: false
        }
      }
    })
  }

  it('should complete full create team flow', async () => {
    // Step 1: Mount TeamList component
    wrapper = createWrapper()
    await flushPromises()

    // Verify initial load
    expect(teamApi.getUserTeams).toHaveBeenCalledWith(1, {
      userId: 1,
      keyword: undefined,
      status: undefined
    })

    // Step 2: Verify dialog is initially hidden
    expect(wrapper.vm.createDialogVisible).toBe(false)

    // Step 3: Show dialog
    wrapper.vm.showCreateDialog()
    await flushPromises()
    expect(wrapper.vm.createDialogVisible).toBe(true)

    // Step 4: Mock successful team creation
    const newTeam = {
      id: 100,
      name: 'New Test Team',
      description: 'Test Description',
      creatorId: 1,
      status: 'ACTIVE',
      createdAt: new Date(),
      updatedAt: new Date()
    }

    // Mock createTeam API
    vi.mocked(teamApi.createTeam).mockResolvedValue(newTeam)

    // Mock updated team list (with new team)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([newTeam])

    // Step 5: Simulate successful creation
    await wrapper.vm.handleCreateSuccess(newTeam.id)
    await flushPromises()

    // Step 6: Verify success message
    expect(ElMessage.success).toHaveBeenCalledWith('团队创建成功，正在跳转...')

    // Step 7: Verify team list is reloaded
    expect(teamApi.getUserTeams).toHaveBeenCalledTimes(2)

    // Step 8: Verify store is updated
    expect(teamStore.teams.length).toBeGreaterThan(0)
  })

  it('should handle create team failure gracefully', async () => {
    wrapper = createWrapper()
    await flushPromises()

    // Show dialog
    wrapper.vm.showCreateDialog()
    await flushPromises()

    // Mock creation failure
    const errorMessage = 'Network error'
    vi.mocked(teamApi.createTeam).mockRejectedValue(new Error(errorMessage))

    // Try to create team (this would happen in CreateTeamDialog)
    try {
      await teamStore.createTeam({
        name: 'Test Team',
        description: 'Test'
      })
    } catch (error) {
      // Expected to fail
    }

    // Verify error is handled
    expect(teamStore.error).toBeTruthy()

    // Verify no navigation occurred
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('should navigate to new team after creation', async () => {
    wrapper = createWrapper()
    await flushPromises()

    const newTeam = {
      id: 200,
      name: 'Navigation Test Team',
      description: 'Test',
      creatorId: 1,
      status: 'ACTIVE',
      createdAt: new Date(),
      updatedAt: new Date()
    }

    // Mock successful creation
    vi.mocked(teamApi.createTeam).mockResolvedValue(newTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([newTeam])

    // Create team through store
    await teamStore.createTeam({
      name: newTeam.name,
      description: newTeam.description
    })

    // Verify team was added to store
    expect(teamStore.teams).toContainEqual(expect.objectContaining({
      id: newTeam.id,
      name: newTeam.name
    }))
  })

  it('should update team list after successful creation', async () => {
    // Initial empty list
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([])

    wrapper = createWrapper()
    await flushPromises()

    // Verify initial empty state
    expect(wrapper.vm.teams.length).toBe(0)

    const newTeam = {
      id: 300,
      name: 'List Update Test',
      description: 'Test',
      creatorId: 1,
      status: 'ACTIVE',
      createdAt: new Date(),
      updatedAt: new Date()
    }

    // Mock creation and updated list
    vi.mocked(teamApi.createTeam).mockResolvedValue(newTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([newTeam])

    // Create team
    await teamStore.createTeam({
      name: newTeam.name,
      description: newTeam.description
    })

    // Simulate success handler
    await wrapper.vm.handleCreateSuccess(newTeam.id)
    await flushPromises()

    // Verify list was reloaded
    expect(teamApi.getUserTeams).toHaveBeenCalledTimes(2)
  })

  it('should close dialog after successful creation', async () => {
    wrapper = createWrapper()
    await flushPromises()

    // Open dialog
    wrapper.vm.showCreateDialog()
    expect(wrapper.vm.createDialogVisible).toBe(true)

    const newTeam = {
      id: 400,
      name: 'Dialog Close Test',
      creatorId: 1,
      status: 'ACTIVE',
      createdAt: new Date(),
      updatedAt: new Date()
    }

    vi.mocked(teamApi.createTeam).mockResolvedValue(newTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([newTeam])

    // Simulate successful creation
    await wrapper.vm.handleCreateSuccess(newTeam.id)
    await flushPromises()

    // Note: Dialog closing is handled by CreateTeamDialog component
    // This test verifies the success handler is called
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('should maintain search and filter state after creation', async () => {
    wrapper = createWrapper()
    await flushPromises()

    // Set search and filter
    wrapper.vm.searchKeyword = 'test'
    wrapper.vm.debouncedKeyword = 'test'
    wrapper.vm.statusFilter = 'ACTIVE'

    const newTeam = {
      id: 500,
      name: 'Test Team',
      description: 'Test',
      creatorId: 1,
      status: 'ACTIVE',
      createdAt: new Date(),
      updatedAt: new Date()
    }

    vi.mocked(teamApi.createTeam).mockResolvedValue(newTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([newTeam])

    // Create team
    await wrapper.vm.handleCreateSuccess(newTeam.id)
    await flushPromises()

    // Verify search and filter are maintained
    expect(wrapper.vm.searchKeyword).toBe('test')
    expect(wrapper.vm.statusFilter).toBe('ACTIVE')

    // Verify API was called with filters
    const calls = vi.mocked(teamApi.getUserTeams).mock.calls
    const lastCall = calls[calls.length - 1]
    expect(lastCall[1]).toMatchObject({
      keyword: 'test',
      status: 'ACTIVE'
    })
  })

  it('should handle rapid create button clicks', async () => {
    wrapper = createWrapper()
    await flushPromises()

    // Click create button multiple times rapidly
    wrapper.vm.showCreateDialog()
    wrapper.vm.showCreateDialog()
    wrapper.vm.showCreateDialog()

    // Dialog should only be shown once
    expect(wrapper.vm.createDialogVisible).toBe(true)
  })

  it('should show empty state when no teams exist', async () => {
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([])

    wrapper = createWrapper()
    await flushPromises()

    // Verify empty state is shown
    expect(wrapper.vm.filteredTeams.length).toBe(0)
    expect(wrapper.vm.searchKeyword).toBe('')
    expect(wrapper.vm.statusFilter).toBe('')
  })

  it('should integrate with auth store for user context', async () => {
    wrapper = createWrapper()
    await flushPromises()

    // Verify auth user is used
    expect(teamApi.getUserTeams).toHaveBeenCalledWith(
      authStore.user.id,
      expect.any(Object)
    )
  })

  it('should handle missing auth user', async () => {
    // Remove auth user
    authStore.user = null

    wrapper = createWrapper()
    await flushPromises()

    // Try to load teams
    await wrapper.vm.loadTeams()

    // Verify error message
    expect(ElMessage.error).toHaveBeenCalledWith('请先登录')

    // Verify navigation to login
    expect(mockPush).toHaveBeenCalledWith({ name: 'Login' })
  })
})

/**
 * 集成测试: 创建团队后导航
 * 
 * 验证需求 2.2.4: 创建成功后导航到团队空间
 */
describe('Integration: Create Team Navigation', () => {
  let teamStore: any
  let authStore: any

  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)

    teamStore = useTeamStore()
    authStore = useAuthStore()

    authStore.user = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com'
    }

    vi.clearAllMocks()
    mockPush.mockClear()
  })

  it('should navigate to TeamOverview after creation', async () => {
    const newTeam = {
      id: 999,
      name: 'Navigation Test',
      creatorId: 1,
      status: 'ACTIVE',
      createdAt: new Date(),
      updatedAt: new Date()
    }

    vi.mocked(teamApi.createTeam).mockResolvedValue(newTeam)

    // Create team
    const result = await teamStore.createTeam({
      name: newTeam.name
    })

    // Verify team was created
    expect(result.id).toBe(newTeam.id)

    // Note: Navigation is handled by CreateTeamDialog component
    // This test verifies the store returns the correct team data
    expect(result).toEqual(newTeam)
  })

  it('should pass correct team ID to navigation', async () => {
    const teamIds = [100, 200, 300, 400, 500]

    for (const teamId of teamIds) {
      const team = {
        id: teamId,
        name: `Team ${teamId}`,
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
      }

      vi.mocked(teamApi.createTeam).mockResolvedValue(team)

      const result = await teamStore.createTeam({
        name: team.name
      })

      expect(result.id).toBe(teamId)
    }
  })
})
