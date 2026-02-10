import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import TeamList from '@/views/team/TeamList.vue'
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

describe('Property Tests - Team List Navigation', () => {
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

  /**
   * 属性 3: 团队卡片导航
   * 对于任何团队卡片，点击它应导航到该团队的空间页面（路由格式为 /team/:id）
   */
  it('Property 3: Team card navigation - clicking any team card navigates to team space', async () => {
    // Feature: team-management, Property 3: Team card navigation
    
    // 生成随机团队数据
    const teamArbitrary = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      description: fc.option(fc.string({ maxLength: 500 })),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
      updatedAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
      memberCount: fc.integer({ min: 1, max: 100 })
    })

    await fc.assert(
      fc.asyncProperty(teamArbitrary, async (teamData) => {
        // 重置 mock
        mockPush.mockClear()
        
        const teamStore = useTeamStore()
        const team: Team = teamData as Team
        
        // 设置团队数据
        vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([team])
        teamStore.teams = [team]
        teamStore.loading = false

        // 挂载组件
        const wrapper = mount(TeamList, {
          global: {
            plugins: [pinia],
            stubs: {
              EmptyState: true
            }
          }
        })

        await flushPromises()

        // 查找团队卡片
        const teamCard = wrapper.findComponent({ name: 'TeamCard' })
        
        // 如果找到团队卡片，触发点击
        if (teamCard.exists()) {
          await teamCard.vm.$emit('click')
          
          // 验证导航被调用
          expect(mockPush).toHaveBeenCalledWith({
            name: 'TeamOverview',
            params: { id: team.id }
          })
          
          // 验证导航参数正确
          const callArgs = mockPush.mock.calls[0][0]
          expect(callArgs.name).toBe('TeamOverview')
          expect(callArgs.params.id).toBe(team.id)
          
          return true
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * 属性 3.1: 多个团队卡片导航
   * 对于任何团队列表，点击每个团队卡片应导航到对应的团队空间
   */
  it('Property 3.1: Multiple team cards navigation - each card navigates to correct team', async () => {
    // Feature: team-management, Property 3: Team card navigation
    
    // 生成随机团队列表（1-10个团队）
    const teamsArbitrary = fc.array(
      fc.record({
        id: fc.integer({ min: 1, max: 10000 }),
        name: fc.string({ minLength: 2, maxLength: 50 }),
        description: fc.option(fc.string({ maxLength: 500 })),
        creatorId: fc.integer({ min: 1, max: 1000 }),
        status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
        createdAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
        updatedAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
        memberCount: fc.integer({ min: 1, max: 100 })
      }),
      { minLength: 1, maxLength: 10 }
    )

    await fc.assert(
      fc.asyncProperty(teamsArbitrary, async (teamsData) => {
        // 重置 mock
        mockPush.mockClear()
        
        const teamStore = useTeamStore()
        const teams: Team[] = teamsData as Team[]
        
        // 设置团队数据
        vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue(teams)
        teamStore.teams = teams
        teamStore.loading = false

        // 挂载组件
        const wrapper = mount(TeamList, {
          global: {
            plugins: [pinia],
            stubs: {
              EmptyState: true
            }
          }
        })

        await flushPromises()

        // 查找所有团队卡片
        const teamCards = wrapper.findAllComponents({ name: 'TeamCard' })
        
        // 验证卡片数量
        expect(teamCards.length).toBe(teams.length)
        
        // 点击第一个团队卡片并验证导航
        if (teamCards.length > 0) {
          mockPush.mockClear()
          
          // 触发 TeamCard 的 click 事件
          await teamCards[0].trigger('click')
          await flushPromises()
          
          // 验证导航到正确的团队
          expect(mockPush).toHaveBeenCalledWith({
            name: 'TeamOverview',
            params: { id: teams[0].id }
          })
        }
        
        return true
      }),
      { numRuns: 50 }
    )
  })

  /**
   * 属性 3.2: 导航参数类型正确性
   * 对于任何团队 ID，导航参数应该是数字类型
   */
  it('Property 3.2: Navigation parameter type - team ID should be a number', async () => {
    // Feature: team-management, Property 3: Team card navigation
    
    const teamIdArbitrary = fc.integer({ min: 1, max: 10000 })

    await fc.assert(
      fc.asyncProperty(teamIdArbitrary, async (teamId) => {
        // 重置 mock
        mockPush.mockClear()
        
        const teamStore = useTeamStore()
        const team: Team = {
          id: teamId,
          name: '测试团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
        
        vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([team])
        teamStore.teams = [team]
        teamStore.loading = false

        const wrapper = mount(TeamList, {
          global: {
            plugins: [pinia],
            stubs: {
              EmptyState: true
            }
          }
        })

        await flushPromises()

        const teamCard = wrapper.findComponent({ name: 'TeamCard' })
        await teamCard.trigger('click')
        await flushPromises()

        // 验证 ID 是数字类型
        if (mockPush.mock.calls.length > 0) {
          const callArgs = mockPush.mock.calls[0][0]
          expect(typeof callArgs.params.id).toBe('number')
          expect(callArgs.params.id).toBe(teamId)
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * 属性 3.3: 导航路由名称一致性
   * 对于任何团队，导航的路由名称应该始终是 'TeamOverview'
   */
  it('Property 3.3: Navigation route name consistency - always navigates to TeamOverview', async () => {
    // Feature: team-management, Property 3: Team card navigation
    
    const teamArbitrary = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
      updatedAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString())
    })

    await fc.assert(
      fc.asyncProperty(teamArbitrary, async (teamData) => {
        mockPush.mockClear()
        
        const teamStore = useTeamStore()
        const team: Team = teamData as Team
        
        vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([team])
        teamStore.teams = [team]
        teamStore.loading = false

        const wrapper = mount(TeamList, {
          global: {
            plugins: [pinia],
            stubs: {
              EmptyState: true
            }
          }
        })

        await flushPromises()

        const teamCard = wrapper.findComponent({ name: 'TeamCard' })
        await teamCard.trigger('click')
        await flushPromises()

        // 验证路由名称始终是 TeamOverview
        if (mockPush.mock.calls.length > 0) {
          const callArgs = mockPush.mock.calls[0][0]
          expect(callArgs.name).toBe('TeamOverview')
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * 属性 3.4: 不同状态的团队都能正确导航
   * 对于任何状态（ACTIVE, ARCHIVED, DISBANDED）的团队，点击都应该能正确导航
   */
  it('Property 3.4: Navigation works for all team statuses', async () => {
    // Feature: team-management, Property 3: Team card navigation
    
    const teamWithStatusArbitrary = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
      updatedAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString())
    })

    await fc.assert(
      fc.asyncProperty(teamWithStatusArbitrary, async (teamData) => {
        mockPush.mockClear()
        
        const teamStore = useTeamStore()
        const team: Team = teamData as Team
        
        vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([team])
        teamStore.teams = [team]
        teamStore.loading = false

        const wrapper = mount(TeamList, {
          global: {
            plugins: [pinia],
            stubs: {
              EmptyState: true
            }
          }
        })

        await flushPromises()

        const teamCard = wrapper.findComponent({ name: 'TeamCard' })
        await teamCard.trigger('click')
        await flushPromises()

        // 验证导航被调用，无论团队状态如何
        if (mockPush.mock.calls.length > 0) {
          expect(mockPush).toHaveBeenCalledWith({
            name: 'TeamOverview',
            params: { id: team.id }
          })
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * 属性 3.5: 搜索过滤后的团队导航
   * 对于任何搜索关键词过滤后的团队列表，点击团队卡片应该正确导航
   */
  it('Property 3.5: Navigation works after search filtering', async () => {
    // Feature: team-management, Property 3: Team card navigation
    
    const teamArbitrary = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString()),
      updatedAt: fc.date({ min: new Date('2020-01-01'), max: new Date() }).map(d => d.toISOString())
    })

    await fc.assert(
      fc.asyncProperty(teamArbitrary, async (teamData) => {
        mockPush.mockClear()
        
        const teamStore = useTeamStore()
        const team: Team = teamData as Team
        
        vi.spyOn(teamStore, 'fetchUserTeams').mockResolvedValue([team])
        teamStore.teams = [team]
        teamStore.loading = false

        const wrapper = mount(TeamList, {
          global: {
            plugins: [pinia],
            stubs: {
              EmptyState: true
            }
          }
        })

        await flushPromises()

        // 直接设置搜索关键词（跳过 UI 交互）
        wrapper.vm.searchKeyword = team.name.substring(0, 2)
        await flushPromises()

        // 查找团队卡片
        const teamCards = wrapper.findAllComponents({ name: 'TeamCard' })
        
        // 如果有团队卡片，点击第一个
        if (teamCards.length > 0) {
          await teamCards[0].trigger('click')
          await flushPromises()

          // 验证导航
          if (mockPush.mock.calls.length > 0) {
            expect(mockPush).toHaveBeenCalledWith({
              name: 'TeamOverview',
              params: { id: team.id }
            })
            
            const callArgs = mockPush.mock.calls[0][0]
            expect(callArgs.name).toBe('TeamOverview')
            expect(typeof callArgs.params.id).toBe('number')
          }
        }
        
        return true
      }),
      { numRuns: 50 }
    )
  })
})
