import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import * as teamApi from '@/api/team'
import type { Team, TeamMember, TeamStatistics } from '@/types/team'

// Mock team API
vi.mock('@/api/team', () => ({
  getUserTeams: vi.fn(),
  getTeam: vi.fn(),
  createTeam: vi.fn(),
  getTeamMembers: vi.fn(),
  addTeamMember: vi.fn(),
  removeTeamMember: vi.fn(),
  getTeamStatistics: vi.fn()
}))

describe('Team Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Initial State', () => {
    it('should have empty teams array', () => {
      const store = useTeamStore()
      expect(store.teams).toEqual([])
    })

    it('should have null currentTeam', () => {
      const store = useTeamStore()
      expect(store.currentTeam).toBeNull()
    })

    it('should have empty currentTeamMembers', () => {
      const store = useTeamStore()
      expect(store.currentTeamMembers).toEqual([])
    })

    it('should have null currentTeamStatistics', () => {
      const store = useTeamStore()
      expect(store.currentTeamStatistics).toBeNull()
    })

    it('should have loading false', () => {
      const store = useTeamStore()
      expect(store.loading).toBe(false)
    })

    it('should have null error', () => {
      const store = useTeamStore()
      expect(store.error).toBeNull()
    })
  })

  describe('Getters', () => {
    it('should calculate teamCount correctly', () => {
      const store = useTeamStore()
      store.teams = [
        { id: 1, name: 'Team 1' } as Team,
        { id: 2, name: 'Team 2' } as Team
      ]
      expect(store.teamCount).toBe(2)
    })

    it('should filter activeTeams correctly', () => {
      const store = useTeamStore()
      store.teams = [
        { id: 1, name: 'Team 1', status: 'ACTIVE' } as Team,
        { id: 2, name: 'Team 2', status: 'ARCHIVED' } as Team,
        { id: 3, name: 'Team 3', status: 'ACTIVE' } as Team
      ]
      expect(store.activeTeams).toHaveLength(2)
      expect(store.activeTeams[0].id).toBe(1)
      expect(store.activeTeams[1].id).toBe(3)
    })

    it('should return null currentUserRole when no current team', () => {
      const store = useTeamStore()
      expect(store.currentUserRole).toBeNull()
    })

    it('should return null currentUserRole when user not authenticated', () => {
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      expect(store.currentUserRole).toBeNull()
    })

    it('should return correct currentUserRole', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)
      
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, userId: 1, role: 'OWNER' } as TeamMember,
        { id: 2, userId: 2, role: 'MEMBER' } as TeamMember
      ]
      
      expect(store.currentUserRole).toBe('OWNER')
    })

    it('should return true for isCurrentUserOwner when role is OWNER', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)
      
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, userId: 1, role: 'OWNER' } as TeamMember
      ]
      
      expect(store.isCurrentUserOwner).toBe(true)
    })

    it('should return true for isCurrentUserOwner when role is LEADER', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)
      
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, userId: 1, role: 'LEADER' } as TeamMember
      ]
      
      expect(store.isCurrentUserOwner).toBe(true)
    })

    it('should return false for isCurrentUserOwner when role is MEMBER', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)
      
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, userId: 1, role: 'MEMBER' } as TeamMember
      ]
      
      expect(store.isCurrentUserOwner).toBe(false)
    })

    it('should return true for isCurrentUserAdmin when role is ADMIN', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)
      
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, userId: 1, role: 'ADMIN' } as TeamMember
      ]
      
      expect(store.isCurrentUserAdmin).toBe(true)
    })

    it('should return true for isCurrentUserAdmin when user is owner', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)
      
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, userId: 1, role: 'OWNER' } as TeamMember
      ]
      
      expect(store.isCurrentUserAdmin).toBe(true)
    })
  })

  describe('fetchUserTeams', () => {
    it('should fetch user teams successfully with array response', async () => {
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '前端团队',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-01',
          updatedAt: '2026-01-01'
        }
      ]

      vi.mocked(teamApi.getUserTeams).mockResolvedValue(mockTeams)

      const store = useTeamStore()
      await store.fetchUserTeams({ userId: 1 })

      expect(store.teams).toEqual(mockTeams)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should fetch user teams successfully with paginated response', async () => {
      const mockResponse = {
        total: 10,
        page: 1,
        size: 12,
        records: [
          {
            id: 1,
            name: '前端团队',
            creatorId: 1,
            status: 'ACTIVE',
            createdAt: '2026-01-01',
            updatedAt: '2026-01-01'
          }
        ]
      }

      vi.mocked(teamApi.getUserTeams).mockResolvedValue(mockResponse)

      const store = useTeamStore()
      await store.fetchUserTeams({ userId: 1 })

      expect(store.teams).toEqual(mockResponse.records)
      expect(store.loading).toBe(false)
    })

    it('should handle fetch user teams error', async () => {
      const error = new Error('Network error')
      vi.mocked(teamApi.getUserTeams).mockRejectedValue(error)

      const store = useTeamStore()
      
      await expect(store.fetchUserTeams({ userId: 1 })).rejects.toThrow('Network error')
      expect(store.error).toBe('Network error')
      expect(store.loading).toBe(false)
    })

    it('should set loading state during fetch', async () => {
      vi.mocked(teamApi.getUserTeams).mockImplementation(() => {
        const store = useTeamStore()
        expect(store.loading).toBe(true)
        return Promise.resolve([])
      })

      const store = useTeamStore()
      await store.fetchUserTeams({ userId: 1 })
    })
  })

  describe('fetchTeamDetail', () => {
    it('should fetch team detail successfully', async () => {
      const mockTeam: Team = {
        id: 1,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }

      const mockMembers: TeamMember[] = [
        {
          id: 1,
          teamId: 1,
          userId: 1,
          role: 'OWNER',
          username: 'user1',
          joinedAt: '2026-01-01',
          updatedAt: '2026-01-01'
        }
      ]

      vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
      vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

      const store = useTeamStore()
      const result = await store.fetchTeamDetail(1)

      expect(store.currentTeam).toEqual(mockTeam)
      expect(store.currentTeamMembers).toEqual(mockMembers)
      expect(result.team).toEqual(mockTeam)
      expect(result.members).toEqual(mockMembers)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should fetch team detail with statistics', async () => {
      const mockStats: TeamStatistics = {
        taskCompletionRate: 75,
        activeDays: 30,
        messageCount: 100,
        fileCount: 20,
        totalTasks: 10,
        completedTasks: 7,
        memberCount: 5
      }

      const mockTeam = {
        id: 1,
        name: '测试团队',
        statistics: mockStats
      } as any

      vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
      vi.mocked(teamApi.getTeamMembers).mockResolvedValue([])

      const store = useTeamStore()
      await store.fetchTeamDetail(1)

      expect(store.currentTeamStatistics).toEqual(mockStats)
    })

    it('should fetch statistics separately if not in response', async () => {
      const mockTeam: Team = {
        id: 1,
        name: '测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }

      const mockStats: TeamStatistics = {
        taskCompletionRate: 75,
        activeDays: 30,
        messageCount: 100,
        fileCount: 20,
        totalTasks: 10,
        completedTasks: 7,
        memberCount: 5
      }

      vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
      vi.mocked(teamApi.getTeamMembers).mockResolvedValue([])
      vi.mocked(teamApi.getTeamStatistics).mockResolvedValue(mockStats)

      const store = useTeamStore()
      await store.fetchTeamDetail(1)

      expect(store.currentTeamStatistics).toEqual(mockStats)
      expect(teamApi.getTeamStatistics).toHaveBeenCalledWith(1)
    })

    it('should handle fetch team detail error', async () => {
      const error = new Error('Team not found')
      vi.mocked(teamApi.getTeam).mockRejectedValue(error)

      const store = useTeamStore()
      
      await expect(store.fetchTeamDetail(1)).rejects.toThrow('Team not found')
      expect(store.error).toBe('Team not found')
      expect(store.loading).toBe(false)
    })
  })

  describe('createTeam', () => {
    it('should create team successfully', async () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)

      const teamData = {
        name: '新团队',
        description: '团队描述'
      }

      const mockTeam: Team = {
        id: 1,
        name: '新团队',
        description: '团队描述',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }

      vi.mocked(teamApi.createTeam).mockResolvedValue(mockTeam)

      const store = useTeamStore()
      const result = await store.createTeam(teamData)

      expect(result).toEqual(mockTeam)
      expect(store.teams).toHaveLength(1)
      expect(store.teams[0]).toEqual(mockTeam)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should add leaderId from auth store if not provided', async () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)

      const teamData = {
        name: '新团队'
      }

      vi.mocked(teamApi.createTeam).mockResolvedValue({} as Team)

      const store = useTeamStore()
      await store.createTeam(teamData)

      expect(teamApi.createTeam).toHaveBeenCalledWith(
        expect.objectContaining({ leaderId: 1 })
      )
    })

    it('should handle create team error', async () => {
      const error = new Error('Invalid team name')
      vi.mocked(teamApi.createTeam).mockRejectedValue(error)

      const store = useTeamStore()
      
      await expect(store.createTeam({ name: 'A' })).rejects.toThrow('Invalid team name')
      expect(store.error).toBe('Invalid team name')
      expect(store.loading).toBe(false)
    })
  })

  describe('addTeamMember', () => {
    it('should add team member successfully', async () => {
      vi.mocked(teamApi.addTeamMember).mockResolvedValue()

      const store = useTeamStore()
      await store.addTeamMember(1, 2, 'MEMBER')

      expect(teamApi.addTeamMember).toHaveBeenCalledWith(1, 2, 'MEMBER')
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should reload members if adding to current team', async () => {
      const mockMembers: TeamMember[] = [
        { id: 1, teamId: 1, userId: 1, role: 'OWNER' } as TeamMember,
        { id: 2, teamId: 1, userId: 2, role: 'MEMBER' } as TeamMember
      ]

      vi.mocked(teamApi.addTeamMember).mockResolvedValue()
      vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      
      await store.addTeamMember(1, 2)

      expect(store.currentTeamMembers).toEqual(mockMembers)
    })

    it('should handle add team member error', async () => {
      const error = new Error('User already exists')
      vi.mocked(teamApi.addTeamMember).mockRejectedValue(error)

      const store = useTeamStore()
      
      await expect(store.addTeamMember(1, 2)).rejects.toThrow('User already exists')
      expect(store.error).toBe('User already exists')
    })
  })

  describe('removeTeamMember', () => {
    it('should remove team member successfully', async () => {
      vi.mocked(teamApi.removeTeamMember).mockResolvedValue()

      const store = useTeamStore()
      await store.removeTeamMember(1, 2)

      expect(teamApi.removeTeamMember).toHaveBeenCalledWith(1, 2)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('should remove member from current team members', async () => {
      vi.mocked(teamApi.removeTeamMember).mockResolvedValue()

      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [
        { id: 1, teamId: 1, userId: 1, role: 'OWNER' } as TeamMember,
        { id: 2, teamId: 1, userId: 2, role: 'MEMBER' } as TeamMember
      ]
      
      await store.removeTeamMember(1, 2)

      expect(store.currentTeamMembers).toHaveLength(1)
      expect(store.currentTeamMembers[0].userId).toBe(1)
    })

    it('should handle remove team member error', async () => {
      const error = new Error('No permission')
      vi.mocked(teamApi.removeTeamMember).mockRejectedValue(error)

      const store = useTeamStore()
      
      await expect(store.removeTeamMember(1, 2)).rejects.toThrow('No permission')
      expect(store.error).toBe('No permission')
    })
  })

  describe('Utility Actions', () => {
    it('should update team locally', () => {
      const store = useTeamStore()
      store.teams = [
        { id: 1, name: 'Team 1', status: 'ACTIVE' } as Team,
        { id: 2, name: 'Team 2', status: 'ACTIVE' } as Team
      ]
      store.currentTeam = { id: 1, name: 'Team 1', status: 'ACTIVE' } as Team

      store.updateTeamLocal(1, { name: 'Updated Team' })

      expect(store.teams[0].name).toBe('Updated Team')
      expect(store.currentTeam?.name).toBe('Updated Team')
    })

    it('should clear current team', () => {
      const store = useTeamStore()
      store.currentTeam = { id: 1, name: 'Team 1' } as Team
      store.currentTeamMembers = [{ id: 1 } as TeamMember]
      store.currentTeamStatistics = {} as TeamStatistics

      store.clearCurrentTeam()

      expect(store.currentTeam).toBeNull()
      expect(store.currentTeamMembers).toEqual([])
      expect(store.currentTeamStatistics).toBeNull()
    })

    it('should clear all data', () => {
      const store = useTeamStore()
      store.teams = [{ id: 1 } as Team]
      store.currentTeam = { id: 1 } as Team
      store.error = 'Some error'

      store.clearAll()

      expect(store.teams).toEqual([])
      expect(store.currentTeam).toBeNull()
      expect(store.error).toBeNull()
    })

    it('should reset error', () => {
      const store = useTeamStore()
      store.error = 'Some error'

      store.resetError()

      expect(store.error).toBeNull()
    })
  })
})
