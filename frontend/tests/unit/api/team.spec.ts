import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getUserTeams,
  getTeam,
  createTeam,
  getTeamMembers,
  addTeamMember,
  removeTeamMember,
  getTeamStatistics
} from '@/api/team'
import { request } from '@/utils/request'
import type { Team, TeamMember, TeamStatistics } from '@/types/team'

// Mock request module
vi.mock('@/utils/request', () => ({
  request: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Team API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getUserTeams', () => {
    it('should fetch user teams successfully', async () => {
      const mockTeams: Team[] = [
        {
          id: 1,
          name: '前端开发团队',
          description: '负责前端开发',
          creatorId: 1,
          status: 'ACTIVE',
          createdAt: '2026-01-01',
          updatedAt: '2026-01-01',
          memberCount: 5
        }
      ]

      vi.mocked(request.get).mockResolvedValue(mockTeams)

      const result = await getUserTeams(1)

      expect(request.get).toHaveBeenCalledWith('/team/teams/user/1', { params: undefined })
      expect(result).toEqual(mockTeams)
    })

    it('should fetch user teams with query parameters', async () => {
      const mockResponse = {
        total: 10,
        page: 1,
        size: 12,
        records: []
      }

      vi.mocked(request.get).mockResolvedValue(mockResponse)

      const params = {
        keyword: '前端',
        status: 'ACTIVE',
        page: 1,
        size: 12
      }

      await getUserTeams(1, params)

      expect(request.get).toHaveBeenCalledWith('/team/teams/user/1', { params })
    })

    it('should handle 404 error when user has no teams', async () => {
      const error = {
        response: {
          status: 404,
          data: { message: '用户没有团队' }
        }
      }

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getUserTeams(999)).rejects.toEqual(error)
    })
  })

  describe('getTeam', () => {
    it('should fetch team details successfully', async () => {
      const mockTeam: Team = {
        id: 1,
        name: '测试团队',
        description: '这是一个测试团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }

      vi.mocked(request.get).mockResolvedValue(mockTeam)

      const result = await getTeam(1)

      expect(request.get).toHaveBeenCalledWith('/team/teams/1')
      expect(result).toEqual(mockTeam)
    })

    it('should handle 404 error when team not found', async () => {
      const error = {
        response: {
          status: 404,
          data: { message: '团队不存在' }
        }
      }

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getTeam(999)).rejects.toEqual(error)
    })

    it('should handle 403 error when user has no permission', async () => {
      const error = {
        response: {
          status: 403,
          data: { message: '无权访问该团队' }
        }
      }

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getTeam(1)).rejects.toEqual(error)
    })
  })

  describe('createTeam', () => {
    it('should create team with new field format', async () => {
      const teamData = {
        name: '新团队',
        description: '团队描述',
        avatar: 'avatar.jpg',
        projectId: 1
      }

      const mockResponse: Team = {
        id: 1,
        name: '新团队',
        description: '团队描述',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }

      vi.mocked(request.post).mockResolvedValue(mockResponse)

      const result = await createTeam(teamData)

      expect(request.post).toHaveBeenCalledWith('/team/teams', {
        teamName: '新团队',
        projectId: 1,
        leaderId: undefined,
        description: '团队描述',
        avatar: 'avatar.jpg'
      })
      expect(result).toEqual(mockResponse)
    })

    it('should create team with old field format', async () => {
      const teamData = {
        teamName: '旧格式团队',
        leaderId: 1,
        projectId: 1
      }

      const mockResponse: Team = {
        id: 2,
        name: '旧格式团队',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01'
      }

      vi.mocked(request.post).mockResolvedValue(mockResponse)

      const result = await createTeam(teamData)

      expect(request.post).toHaveBeenCalledWith('/team/teams', {
        teamName: '旧格式团队',
        projectId: 1,
        leaderId: 1,
        description: undefined,
        avatar: undefined
      })
      expect(result).toEqual(mockResponse)
    })

    it('should handle 400 error for invalid team name', async () => {
      const error = {
        response: {
          status: 400,
          data: {
            code: 40002,
            message: '团队名称长度应在2-50字符之间'
          }
        }
      }

      vi.mocked(request.post).mockRejectedValue(error)

      await expect(
        createTeam({ name: 'A' })
      ).rejects.toEqual(error)
    })
  })

  describe('getTeamMembers', () => {
    it('should fetch team members successfully', async () => {
      const mockMembers: TeamMember[] = [
        {
          id: 1,
          teamId: 1,
          userId: 1,
          role: 'OWNER',
          username: 'user1',
          joinedAt: '2026-01-01',
          updatedAt: '2026-01-01'
        },
        {
          id: 2,
          teamId: 1,
          userId: 2,
          role: 'MEMBER',
          username: 'user2',
          joinedAt: '2026-01-02',
          updatedAt: '2026-01-02'
        }
      ]

      vi.mocked(request.get).mockResolvedValue(mockMembers)

      const result = await getTeamMembers(1)

      expect(request.get).toHaveBeenCalledWith('/team/teams/1/members')
      expect(result).toEqual(mockMembers)
      expect(result).toHaveLength(2)
    })

    it('should handle empty member list', async () => {
      vi.mocked(request.get).mockResolvedValue([])

      const result = await getTeamMembers(1)

      expect(result).toEqual([])
      expect(result).toHaveLength(0)
    })
  })

  describe('addTeamMember', () => {
    it('should add team member without role', async () => {
      vi.mocked(request.post).mockResolvedValue(undefined)

      await addTeamMember(1, 2)

      expect(request.post).toHaveBeenCalledWith('/team/teams/1/members', {
        userId: 2,
        role: undefined
      })
    })

    it('should add team member with role', async () => {
      vi.mocked(request.post).mockResolvedValue(undefined)

      await addTeamMember(1, 2, 'ADMIN')

      expect(request.post).toHaveBeenCalledWith('/team/teams/1/members', {
        userId: 2,
        role: 'ADMIN'
      })
    })

    it('should handle 400 error when user already exists', async () => {
      const error = {
        response: {
          status: 400,
          data: {
            code: 40005,
            message: '该用户已是团队成员'
          }
        }
      }

      vi.mocked(request.post).mockRejectedValue(error)

      await expect(addTeamMember(1, 2)).rejects.toEqual(error)
    })
  })

  describe('removeTeamMember', () => {
    it('should remove team member successfully', async () => {
      vi.mocked(request.delete).mockResolvedValue(undefined)

      await removeTeamMember(1, 2)

      expect(request.delete).toHaveBeenCalledWith('/team/teams/1/members/2')
    })

    it('should handle 403 error when no permission', async () => {
      const error = {
        response: {
          status: 403,
          data: {
            code: 40303,
            message: '无权移除该成员'
          }
        }
      }

      vi.mocked(request.delete).mockRejectedValue(error)

      await expect(removeTeamMember(1, 2)).rejects.toEqual(error)
    })
  })

  describe('getTeamStatistics', () => {
    it('should fetch team statistics successfully', async () => {
      const mockStats: TeamStatistics = {
        taskCompletionRate: 75.5,
        activeDays: 30,
        messageCount: 150,
        fileCount: 25,
        totalTasks: 20,
        completedTasks: 15,
        memberCount: 8
      }

      vi.mocked(request.get).mockResolvedValue(mockStats)

      const result = await getTeamStatistics(1)

      expect(request.get).toHaveBeenCalledWith('/team/teams/1/statistics')
      expect(result).toEqual(mockStats)
      expect(result.taskCompletionRate).toBe(75.5)
      expect(result.memberCount).toBe(8)
    })

    it('should handle 404 error when team not found', async () => {
      const error = {
        response: {
          status: 404,
          data: { message: '团队不存在' }
        }
      }

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getTeamStatistics(999)).rejects.toEqual(error)
    })
  })

  describe('Error handling', () => {
    it('should handle 500 server error', async () => {
      const error = {
        response: {
          status: 500,
          data: { message: '服务器内部错误' }
        }
      }

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getUserTeams(1)).rejects.toEqual(error)
    })

    it('should handle network error', async () => {
      const error = new Error('Network Error')

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getUserTeams(1)).rejects.toThrow('Network Error')
    })

    it('should handle 401 unauthorized error', async () => {
      const error = {
        response: {
          status: 401,
          data: { message: '未授权' }
        }
      }

      vi.mocked(request.get).mockRejectedValue(error)

      await expect(getTeam(1)).rejects.toEqual(error)
    })
  })
})
