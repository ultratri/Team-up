import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import TeamCard from '@/views/team/components/TeamCard.vue'
import { useAuthStore } from '@/store/auth'
import type { Team } from '@/types/team'
import { ElAvatar, ElTag, ElIcon } from 'element-plus'

describe('TeamCard.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  const createMockTeam = (overrides?: Partial<Team>): Team => ({
    id: 1,
    name: '前端开发团队',
    description: '负责前端项目开发',
    creatorId: 1,
    status: 'ACTIVE',
    createdAt: '2026-01-15T10:30:00',
    updatedAt: '2026-01-15T10:30:00',
    memberCount: 8,
    ...overrides
  })

  describe('Display Team Information', () => {
    it('should display team name', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('前端开发团队')
    })

    it('should display team description', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('负责前端项目开发')
    })

    it('should display member count', () => {
      const team = createMockTeam({ memberCount: 8 })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('8 人')
    })

    it('should display 0 members when memberCount is undefined', () => {
      const team = createMockTeam({ memberCount: undefined })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('0 人')
    })

    it('should display creation date', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      // Should display some date format (exact format depends on formatDate function)
      const metaItems = wrapper.findAll('.meta-item')
      expect(metaItems.length).toBeGreaterThan(0)
    })
  })

  describe('Empty Description Handling', () => {
    it('should display "暂无描述" when description is null', () => {
      const team = createMockTeam({ description: undefined })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('暂无描述')
    })

    it('should display "暂无描述" when description is empty string', () => {
      const team = createMockTeam({ description: '' })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('暂无描述')
    })

    it('should display actual description when provided', () => {
      const team = createMockTeam({ description: '这是一个测试团队' })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('这是一个测试团队')
      expect(wrapper.text()).not.toContain('暂无描述')
    })
  })

  describe('Role Tag Display', () => {
    it('should display OWNER tag when user is creator', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 1, username: 'user1' } as any, false)

      const team = createMockTeam({ creatorId: 1 })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('所有者')
    })

    it('should not display role tag when user is not creator and role is unknown', () => {
      const authStore = useAuthStore()
      authStore.setUser({ id: 2, username: 'user2' } as any, false)

      const team = createMockTeam({ creatorId: 1 })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).not.toContain('所有者')
      expect(wrapper.text()).not.toContain('管理员')
      expect(wrapper.text()).not.toContain('成员')
    })

    it('should not display role tag when user is not authenticated', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).not.toContain('所有者')
      expect(wrapper.text()).not.toContain('管理员')
    })
  })

  describe('Click Event', () => {
    it('should emit click event when card is clicked', async () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      await wrapper.find('.team-card').trigger('click')

      expect(wrapper.emitted('click')).toBeTruthy()
      expect(wrapper.emitted('click')).toHaveLength(1)
    })

    it('should emit click event when Enter key is pressed', async () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      await wrapper.find('.team-card').trigger('keydown.enter')

      expect(wrapper.emitted('click')).toBeTruthy()
      expect(wrapper.emitted('click')).toHaveLength(1)
    })

    it('should emit click event when Space key is pressed', async () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      await wrapper.find('.team-card').trigger('keydown.space')

      expect(wrapper.emitted('click')).toBeTruthy()
      expect(wrapper.emitted('click')).toHaveLength(1)
    })

    it('should not emit multiple click events for single click', async () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      await wrapper.find('.team-card').trigger('click')

      expect(wrapper.emitted('click')).toHaveLength(1)
    })
  })

  describe('Date Formatting', () => {
    it('should display "今天" for today\'s date', () => {
      const today = new Date()
      const team = createMockTeam({ createdAt: today.toISOString() })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('今天')
    })

    it('should display "昨天" for yesterday\'s date', () => {
      const yesterday = new Date()
      yesterday.setDate(yesterday.getDate() - 1)
      const team = createMockTeam({ createdAt: yesterday.toISOString() })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('昨天')
    })

    it('should display days ago for recent dates', () => {
      const threeDaysAgo = new Date()
      threeDaysAgo.setDate(threeDaysAgo.getDate() - 3)
      const team = createMockTeam({ createdAt: threeDaysAgo.toISOString() })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('3 天前')
    })

    it('should display weeks ago for dates within a month', () => {
      const twoWeeksAgo = new Date()
      twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14)
      const team = createMockTeam({ createdAt: twoWeeksAgo.toISOString() })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('周前')
    })

    it('should display months ago for dates within a year', () => {
      const twoMonthsAgo = new Date()
      twoMonthsAgo.setMonth(twoMonthsAgo.getMonth() - 2)
      const team = createMockTeam({ createdAt: twoMonthsAgo.toISOString() })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('个月前')
    })

    it('should display years ago for old dates', () => {
      const twoYearsAgo = new Date()
      twoYearsAgo.setFullYear(twoYearsAgo.getFullYear() - 2)
      const team = createMockTeam({ createdAt: twoYearsAgo.toISOString() })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('年前')
    })
  })

  describe('Accessibility', () => {
    it('should have tabindex="0" for keyboard navigation', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      const card = wrapper.find('.team-card')
      expect(card.attributes('tabindex')).toBe('0')
    })

    it('should have role="button"', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      const card = wrapper.find('.team-card')
      expect(card.attributes('role')).toBe('button')
    })

    it('should have alt text for avatar', () => {
      const team = createMockTeam({ name: '测试团队' })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      const avatar = wrapper.findComponent(ElAvatar)
      expect(avatar.props('alt')).toBe('测试团队')
    })
  })

  describe('Avatar Display', () => {
    it('should display team avatar when provided', () => {
      const team = createMockTeam({ avatar: 'https://example.com/avatar.jpg' })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      const avatar = wrapper.findComponent(ElAvatar)
      expect(avatar.props('src')).toBe('https://example.com/avatar.jpg')
    })

    it('should display default icon when avatar is not provided', () => {
      const team = createMockTeam({ avatar: undefined })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      const avatar = wrapper.findComponent(ElAvatar)
      expect(avatar.exists()).toBe(true)
    })

    it('should have correct avatar size', () => {
      const team = createMockTeam()
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      const avatar = wrapper.findComponent(ElAvatar)
      expect(avatar.props('size')).toBe(60)
    })
  })

  describe('Edge Cases', () => {
    it('should handle very long team names', () => {
      const longName = 'A'.repeat(100)
      const team = createMockTeam({ name: longName })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain(longName)
      // Should have ellipsis styling (checked via CSS class)
      expect(wrapper.find('.team-name').exists()).toBe(true)
    })

    it('should handle very long descriptions', () => {
      const longDesc = 'B'.repeat(500)
      const team = createMockTeam({ description: longDesc })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain(longDesc)
      // Should have line clamp styling (checked via CSS class)
      expect(wrapper.find('.team-description').exists()).toBe(true)
    })

    it('should handle zero member count', () => {
      const team = createMockTeam({ memberCount: 0 })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('0 人')
    })

    it('should handle large member count', () => {
      const team = createMockTeam({ memberCount: 9999 })
      const wrapper = mount(TeamCard, {
        props: { team },
        global: {
          components: { ElAvatar, ElTag, ElIcon }
        }
      })

      expect(wrapper.text()).toContain('9999 人')
    })
  })
})
