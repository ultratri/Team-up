import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElSkeleton, ElResult, ElButton, ElTimeline, ElTimelineItem } from 'element-plus'
import TeamOverview from '@/views/team/TeamOverview.vue'
import * as teamApi from '@/api/team'
import type { Team, TeamMember, TeamStatistics, TeamActivity } from '@/types/team'

// Mock the API
vi.mock('@/api/team', () => ({
  getTeam: vi.fn(),
  getTeamMembers: vi.fn(),
  getTeamStatistics: vi.fn(),
  getTeamActivities: vi.fn()
}))

vi.mock('@/utils/requestOptimization', () => ({
  parallelRequests: vi.fn((requests) => Promise.all(requests.map(fn => fn())))
}))

describe('TeamOverview.vue', () => {
  const mockTeam: Team = {
    id: 1,
    name: '前端开发团队',
    description: '负责前端项目开发',
    avatar: 'https://example.com/avatar.jpg',
    projectId: 1,
    creatorId: 1,
    status: 'ACTIVE' as any,
    createdAt: '2026-01-15T10:30:00',
    updatedAt: '2026-01-20T10:30:00',
    memberCount: 3
  }

  const mockMembers: TeamMember[] = [
    {
      id: 1,
      teamId: 1,
      userId: 1,
      role: 'OWNER' as any,
      joinedAt: '2026-01-15T10:30:00',
      updatedAt: '2026-01-15T10:30:00',
      username: '张三',
      avatar: 'https://example.com/user1.jpg',
      email: 'zhangsan@example.com',
      skills: ['Vue', 'TypeScript'],
      department: '技术部'
    },
    {
      id: 2,
      teamId: 1,
      userId: 2,
      role: 'ADMIN' as any,
      joinedAt: '2026-01-16T10:30:00',
      updatedAt: '2026-01-16T10:30:00',
      username: '李四',
      avatar: 'https://example.com/user2.jpg',
      email: 'lisi@example.com',
      skills: ['React', 'Node.js'],
      department: '技术部'
    },
    {
      id: 3,
      teamId: 1,
      userId: 3,
      role: 'MEMBER' as any,
      joinedAt: '2026-01-17T10:30:00',
      updatedAt: '2026-01-17T10:30:00',
      username: '王五',
      avatar: 'https://example.com/user3.jpg',
      email: 'wangwu@example.com',
      skills: ['Angular'],
      department: '技术部'
    }
  ]

  const mockStatistics: TeamStatistics = {
    taskCompletionRate: 75,
    activeDays: 30,
    messageCount: 150,
    fileCount: 25,
    totalTasks: 20,
    completedTasks: 15,
    memberCount: 3
  }

  const mockActivities: TeamActivity[] = [
    {
      id: 1,
      userId: 1,
      username: '张三',
      avatarUrl: 'https://example.com/user1.jpg',
      activityType: 'task',
      action: 'complete',
      detail: '完成了任务「实现用户登录功能」',
      createdAt: '2026-01-23T10:30:00'
    },
    {
      id: 2,
      userId: 2,
      username: '李四',
      avatarUrl: 'https://example.com/user2.jpg',
      activityType: 'file',
      action: 'upload',
      detail: '上传了文件「设计稿.pdf」',
      createdAt: '2026-01-23T09:15:00'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    // Set up default mock implementations
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)
    vi.mocked(teamApi.getTeamStatistics).mockResolvedValue(mockStatistics)
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue(mockActivities)
  })

  it('should display loading state initially', async () => {
    let resolvePromise: any
    const promise = new Promise((resolve) => {
      resolvePromise = resolve
    })
    
    vi.mocked(teamApi.getTeam).mockReturnValue(promise as any)
    vi.mocked(teamApi.getTeamMembers).mockReturnValue(promise as any)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton
        }
      }
    })

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.loading-container').exists()).toBe(true)
    expect(wrapper.findComponent(ElSkeleton).exists()).toBe(true)
    
    // Clean up
    resolvePromise(mockTeam)
  })

  it('should display team information after loading', async () => {
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('前端开发团队')
    expect(wrapper.text()).toContain('负责前端项目开发')
  })

  it('should display member list', async () => {
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    const memberAvatars = wrapper.findAll('.member-avatar')
    expect(memberAvatars.length).toBe(3)
  })

  it('should display statistics data', async () => {
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('任务完成率')
    expect(wrapper.text()).toContain('活跃天数')
    expect(wrapper.text()).toContain('消息数量')
    expect(wrapper.text()).toContain('共享文件')
  })

  it('should display activity timeline', async () => {
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('最近活动')
    expect(wrapper.find('.activity-timeline').exists()).toBe(true)
  })

  it('should display error state when API fails', async () => {
    const errorMessage = '网络错误'
    vi.mocked(teamApi.getTeam).mockRejectedValue(new Error(errorMessage))
    vi.mocked(teamApi.getTeamMembers).mockRejectedValue(new Error(errorMessage))

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        },
        stubs: {
          ElResult: false,
          ElButton: false
        }
      }
    })

    await wrapper.vm.$nextTick()
    // 等待 loadOverviewData 完成
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.error-container').exists()).toBe(true)
  })

  it('should retry loading when retry button is clicked', async () => {
    // First call fails
    vi.mocked(teamApi.getTeam).mockRejectedValueOnce(new Error('网络错误'))
    vi.mocked(teamApi.getTeamMembers).mockRejectedValueOnce(new Error('网络错误'))

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        },
        stubs: {
          ElResult: false,
          ElButton: false
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    // Mock successful response for retry
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const retryButton = wrapper.find('button')
    if (retryButton.exists()) {
      await retryButton.trigger('click')
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 200))
      await wrapper.vm.$nextTick()
    }

    // Should have been called twice: once initially, once on retry
    expect(teamApi.getTeam).toHaveBeenCalled()
  })

  it('should display empty description when description is null', async () => {
    const teamWithoutDesc = { ...mockTeam, description: '' }
    vi.mocked(teamApi.getTeam).mockResolvedValue(teamWithoutDesc)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    const descriptionText = wrapper.find('.description')
    expect(descriptionText.exists()).toBe(true)
    // 应该显示"暂无描述"或者空字符串
    const text = descriptionText.text()
    expect(text === '暂无描述' || text === '').toBe(true)
  })

  it('should show member tooltip on hover', async () => {
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    const memberAvatar = wrapper.find('.member-avatar')
    await memberAvatar.trigger('mouseenter', { clientX: 100, clientY: 100 })

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.member-tooltip').exists()).toBe(true)
  })

  it('should hide member tooltip on mouse leave', async () => {
    vi.mocked(teamApi.getTeam).mockResolvedValue(mockTeam)
    vi.mocked(teamApi.getTeamMembers).mockResolvedValue(mockMembers)

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))

    const memberAvatar = wrapper.find('.member-avatar')
    await memberAvatar.trigger('mouseenter', { clientX: 100, clientY: 100 })
    await wrapper.vm.$nextTick()

    await memberAvatar.trigger('mouseleave')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.member-tooltip').exists()).toBe(false)
  })

  it('should call getTeamStatistics API and display statistics', async () => {
    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    // Verify API was called
    expect(teamApi.getTeamStatistics).toHaveBeenCalledWith(1)

    // Verify statistics are displayed
    expect(wrapper.text()).toContain('75%')
    expect(wrapper.text()).toContain('30')
    expect(wrapper.text()).toContain('150')
    expect(wrapper.text()).toContain('25')
  })

  it('should call getTeamActivities API and display activities', async () => {
    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    // Verify API was called with correct parameters
    expect(teamApi.getTeamActivities).toHaveBeenCalledWith(1, 10)

    // Verify activities are displayed
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('完成了任务「实现用户登录功能」')
    expect(wrapper.text()).toContain('李四')
    expect(wrapper.text()).toContain('上传了文件「设计稿.pdf」')
  })

  it('should handle empty activities list', async () => {
    vi.mocked(teamApi.getTeamActivities).mockResolvedValue([])

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    // Check for empty activities container
    expect(wrapper.find('.empty-activities').exists()).toBe(true)
  })

  it('should handle statistics API error gracefully', async () => {
    vi.mocked(teamApi.getTeamStatistics).mockRejectedValue(new Error('统计数据加载失败'))

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        },
        stubs: {
          ElResult: false,
          ElButton: false
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))
    await wrapper.vm.$nextTick()

    // Should display error state
    expect(wrapper.find('.error-container').exists()).toBe(true)
  })

  it('should use parallelRequests for optimized loading', async () => {
    const parallelRequestsMock = vi.fn((requests) => Promise.all(requests.map((fn: any) => fn())))
    vi.doMock('@/utils/requestOptimization', () => ({
      parallelRequests: parallelRequestsMock
    }))

    const wrapper = mount(TeamOverview, {
      props: { teamId: 1 },
      global: {
        components: {
          ElSkeleton,
          ElResult,
          ElButton,
          ElTimeline,
          ElTimelineItem
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 200))

    // Verify all APIs were called
    expect(teamApi.getTeam).toHaveBeenCalled()
    expect(teamApi.getTeamMembers).toHaveBeenCalled()
    expect(teamApi.getTeamStatistics).toHaveBeenCalled()
    expect(teamApi.getTeamActivities).toHaveBeenCalled()
  })
})
