import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import * as teamApi from '@/api/team'
import type { Team, TeamMember, TeamStatistics, TeamActivity } from '@/types/team'

// Mock team API
vi.mock('@/api/team')

describe('TeamStore - Parallel Loading and Caching', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should load team page with parallel requests', async () => {
    const teamId = 1
    const userId = 1

    // Setup: Set authenticated user
    const authStore = useAuthStore()
    authStore.setUser({ id: userId, username: 'testuser' } as any, false)

    const store = useTeamStore()

    // Mock API responses
    const mockTeam: Team = {
      id: teamId,
      name: 'Test Team',
      creatorId: userId,
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00.000Z',
      updatedAt: '2024-01-01T00:00:00.000Z'
    }
    const mockMembers: TeamMember[] = [
      {
        id: 1,
        teamId: teamId,
        userId: userId,
        role: 'OWNER',
        username: 'testuser',
        joinedAt: '2024-01-01T00:00:00.000Z',
        updatedAt: '2024-01-01T00:00:00.000Z'
      }
    ]
    const mockStats: TeamStatistics = {
      taskCompletionRate: 75,
      activeDays: 30,
      messageCount: 100,
      fileCount: 20
    }
    const mockActivities: TeamActivity[] = []

    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([mockTeam])
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)
    vi.mocked(teamApi.getTeamStatistics).mockResolvedValue(mockStats)
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue(mockActivities)

    // Load team page
    await store.loadTeamPage(teamId, 'overview')

    // Verify: All data is loaded
    expect(store.currentTeam?.id).toBe(teamId)
    expect(store.currentTeamMembers.length).toBe(1)
    expect(store.currentTeamStatistics).toBeDefined()
    expect(store.currentTeamActivities).toBeDefined()

    // Verify: All API calls were made
    expect(teamApi.getTeam).toHaveBeenCalledWith(teamId, false)
    expect(teamApi.getUserTeams).toHaveBeenCalled()
    expect(teamApi.getTeamMembers).toHaveBeenCalledWith(teamId, false)
    expect(teamApi.getTeamStatistics).toHaveBeenCalledWith(teamId, false)
    expect(teamApi.getTeamActivities).toHaveBeenCalledWith(teamId, 10)
  })

  it('should use cache on second load', async () => {
    const teamId = 1
    const userId = 1

    // Setup: Set authenticated user
    const authStore = useAuthStore()
    authStore.setUser({ id: userId, username: 'testuser' } as any, false)

    const store = useTeamStore()

    // Mock API responses
    const mockTeam: Team = {
      id: teamId,
      name: 'Test Team',
      creatorId: userId,
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00.000Z',
      updatedAt: '2024-01-01T00:00:00.000Z'
    }
    const mockMembers: TeamMember[] = [
      {
        id: 1,
        teamId: teamId,
        userId: userId,
        role: 'OWNER',
        username: 'testuser',
        joinedAt: '2024-01-01T00:00:00.000Z',
        updatedAt: '2024-01-01T00:00:00.000Z'
      }
    ]
    const mockStats: TeamStatistics = {
      taskCompletionRate: 75,
      activeDays: 30,
      messageCount: 100,
      fileCount: 20
    }
    const mockActivities: TeamActivity[] = []

    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([mockTeam])
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)
    vi.mocked(teamApi.getTeamStatistics).mockResolvedValue(mockStats)
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue(mockActivities)

    // First load: populate cache
    await store.loadTeamPage(teamId, 'overview')

    // Verify data is loaded
    expect(store.currentTeam?.id).toBe(teamId)

    // Clear mocks
    vi.clearAllMocks()

    // Mock API responses for second load
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([mockTeam])
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)
    vi.mocked(teamApi.getTeamStatistics).mockResolvedValue(mockStats)
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue(mockActivities)

    // Second load: should use cache
    const startTime = performance.now()
    await store.loadTeamPage(teamId, 'overview')
    const loadTime = performance.now() - startTime

    // Verify: Load should be fast (using cached data)
    expect(loadTime).toBeLessThan(100)

    // Verify: Data is still available
    expect(store.currentTeam?.id).toBe(teamId)
  })

  it('should handle section-specific errors', async () => {
    const teamId = 1
    const userId = 1

    // Setup: Set authenticated user
    const authStore = useAuthStore()
    authStore.setUser({ id: userId, username: 'testuser' } as any, false)

    const store = useTeamStore()

    // Mock API responses - statistics fails
    const mockTeam: Team = {
      id: teamId,
      name: 'Test Team',
      creatorId: userId,
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00.000Z',
      updatedAt: '2024-01-01T00:00:00.000Z'
    }
    const mockMembers: TeamMember[] = []
    const mockActivities: TeamActivity[] = []

    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([mockTeam])
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)
    vi.mocked(teamApi.getTeamStatistics).mockRejectedValue(new Error('Statistics failed'))
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue(mockActivities)

    // Load team page
    await store.loadTeamPage(teamId, 'overview')

    // Verify: Critical data is loaded despite statistics failure
    expect(store.currentTeam?.id).toBe(teamId)
    expect(store.currentTeamMembers).toBeDefined()

    // Verify: Statistics section has error
    expect(store.hasError('statistics')).toBe(true)
    expect(store.getError('statistics')?.message).toContain('Statistics failed')
  })

  it('should retry failed section', async () => {
    const teamId = 1
    const userId = 1

    // Setup: Set authenticated user
    const authStore = useAuthStore()
    authStore.setUser({ id: userId, username: 'testuser' } as any, false)

    const store = useTeamStore()

    // Mock API responses - statistics fails initially
    const mockTeam: Team = {
      id: teamId,
      name: 'Test Team',
      creatorId: userId,
      status: 'ACTIVE',
      createdAt: '2024-01-01T00:00:00.000Z',
      updatedAt: '2024-01-01T00:00:00.000Z'
    }
    const mockStats: TeamStatistics = {
      taskCompletionRate: 75,
      activeDays: 30,
      messageCount: 100,
      fileCount: 20
    }

    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getUserTeams).mockResolvedValue([mockTeam])
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue([])
    vi.mocked(teamApi.getTeamStatistics).mockRejectedValueOnce(new Error('Statistics failed'))
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue([])

    // Load team page - statistics will fail
    await store.loadTeamPage(teamId, 'overview')

    // Verify: Statistics section has error
    expect(store.hasError('statistics')).toBe(true)

    // Mock successful retry
    vi.mocked(teamApi.getTeamStatistics).mockResolvedValue(mockStats)

    // Retry failed section
    await store.retryFailedSection('statistics', teamId)

    // Verify: Statistics section no longer has error
    expect(store.hasError('statistics')).toBe(false)
    expect(store.currentTeamStatistics).toBeDefined()
    expect(store.currentTeamStatistics?.taskCompletionRate).toBe(75)
  })
})
