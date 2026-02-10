import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskComments from '@/components/team/TaskComments.vue'
import * as requestUtils from '@/utils/request'
import { useAuthStore } from '@/store/auth'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

describe('TaskComments.vue', () => {
  let requestMock: any
  let authStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()

    // Setup auth store with mock user
    authStore = useAuthStore()
    authStore.user = {
      id: 1,
      username: '张三',
      avatar: 'https://example.com/avatar1.jpg',
    }

    // Mock request utility
    requestMock = {
      get: vi.fn(),
      post: vi.fn(),
      delete: vi.fn(),
    }
    vi.spyOn(requestUtils, 'request', 'get').mockReturnValue(requestMock)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const mockComments = [
    {
      id: 1,
      taskId: 100,
      userId: 1,
      userName: '张三',
      avatar: 'https://example.com/avatar1.jpg',
      content: '这是第一条评论',
      createdAt: '2026-01-26T10:00:00',
    },
    {
      id: 2,
      taskId: 100,
      userId: 2,
      userName: '李四',
      avatar: 'https://example.com/avatar2.jpg',
      content: '这是第二条评论',
      createdAt: '2026-01-26T11:00:00',
    },
  ]

  describe('Comment List Rendering', () => {
    it('should display comment list when comments exist', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('这是第一条评论')
      expect(wrapper.text()).toContain('李四')
      expect(wrapper.text()).toContain('这是第二条评论')
    })

    it('should display empty state when no comments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('暂无评论')
    })

    it('should display comment count in header', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('评论 (2)')
    })

    it('should display comment avatars', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const avatars = wrapper.findAll('.comment-avatar')
      expect(avatars.length).toBe(2)
    })

    it('should display comment author names', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const authors = wrapper.findAll('.comment-author')
      expect(authors.length).toBe(2)
      expect(authors[0].text()).toBe('张三')
      expect(authors[1].text()).toBe('李四')
    })

    it('should display comment content', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const contents = wrapper.findAll('.comment-text')
      expect(contents.length).toBe(2)
      expect(contents[0].text()).toBe('这是第一条评论')
      expect(contents[1].text()).toBe('这是第二条评论')
    })

    it('should display comment time', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const times = wrapper.findAll('.comment-time')
      expect(times.length).toBe(2)
      expect(times[0].text()).toBeTruthy()
    })

    it('should display delete button only for own comments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      // Should show delete button for first comment (userId: 1, current user)
      const deleteButtons = wrapper.findAll('.delete-btn')
      expect(deleteButtons.length).toBe(1)
    })
  })

  describe('Add Comment Interaction', () => {
    it('should enable send button when comment has content', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '新评论'
      await flushPromises()

      const sendButton = wrapper.findAll('.el-button').find(btn => 
        btn.text().includes('发送')
      )
      expect(sendButton?.attributes('disabled')).toBeUndefined()
    })

    it('should disable send button when comment is empty', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const sendButton = wrapper.findAll('.el-button').find(btn => 
        btn.text().includes('发送')
      )
      expect(sendButton?.attributes('disabled')).toBeDefined()
    })

    it('should add comment successfully', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockResolvedValueOnce({
        data: {
          id: 3,
          taskId: 100,
          userId: 1,
          userName: '张三',
          avatar: 'https://example.com/avatar1.jpg',
          content: '新评论',
          createdAt: '2026-01-26T12:00:00',
        },
      })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '新评论'
      await vm.handleSubmit()
      await flushPromises()

      expect(requestMock.post).toHaveBeenCalledWith('/tasks/100/comments', {
        userId: 1,
        content: '新评论',
      })
      expect(ElMessage.success).toHaveBeenCalledWith('评论发送成功')
      expect(wrapper.emitted('update')).toBeTruthy()
      expect(vm.newComment).toBe('')
    })

    it('should show warning when submitting empty comment', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '   '
      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('请输入评论内容')
      expect(requestMock.post).not.toHaveBeenCalled()
    })

    it('should show error when add fails', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockRejectedValueOnce(new Error('发送失败'))

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '新评论'
      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should show error when user is not logged in', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      authStore.user = null

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '新评论'
      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('请先登录')
      expect(requestMock.post).not.toHaveBeenCalled()
    })

    it('should clear input after successful submission', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockResolvedValueOnce({
        data: {
          id: 3,
          taskId: 100,
          userId: 1,
          userName: '张三',
          content: '新评论',
          createdAt: '2026-01-26T12:00:00',
        },
      })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '新评论'
      await vm.handleSubmit()
      await flushPromises()

      expect(vm.newComment).toBe('')
    })
  })

  describe('Delete Comment Interaction', () => {
    it('should show confirmation dialog when delete button is clicked', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockComments[0])

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        '确定要删除这条评论吗？',
        '提示',
        expect.any(Object)
      )
    })

    it('should delete comment successfully', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })
      requestMock.delete.mockResolvedValueOnce({})
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockComments[0])
      await flushPromises()

      expect(requestMock.delete).toHaveBeenCalledWith(
        '/tasks/100/comments/1',
        { params: { userId: 1 } }
      )
      expect(ElMessage.success).toHaveBeenCalledWith('删除评论成功')
      expect(wrapper.emitted('update')).toBeTruthy()
    })

    it('should show error when delete fails', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })
      requestMock.delete.mockRejectedValueOnce(new Error('删除失败'))
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockComments[0])
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('should not delete when user cancels confirmation', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })
      ;(ElMessageBox.confirm as any).mockRejectedValueOnce(new Error('cancel'))

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockComments[0])
      await flushPromises()

      expect(requestMock.delete).not.toHaveBeenCalled()
    })

    it('should show error when user is not logged in during delete', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })
      ;(ElMessageBox.confirm as any).mockResolvedValueOnce(true)
      authStore.user = null

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleDelete(mockComments[0])
      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalledWith('请先登录')
      expect(requestMock.delete).not.toHaveBeenCalled()
    })
  })

  describe('Permission Control', () => {
    it('should allow deleting own comments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.canDeleteComment(mockComments[0])).toBe(true)
    })

    it('should not allow deleting other users comments', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(vm.canDeleteComment(mockComments[1])).toBe(false)
    })
  })

  describe('API Error Handling', () => {
    it('should handle fetch comments error', async () => {
      requestMock.get.mockRejectedValueOnce(new Error('Network error'))

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(ElMessage.error).toHaveBeenCalled()
    })
  })

  describe('Time Formatting', () => {
    it('should display "刚刚" for very recent time', async () => {
      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const now = new Date().toISOString()
      const formatted = vm.formatTime(now)
      expect(formatted).toBe('刚刚')
    })

    it('should display minutes ago for recent time', async () => {
      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(tenMinutesAgo)
      expect(formatted).toContain('分钟前')
    })

    it('should display hours ago for time within a day', async () => {
      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(twoHoursAgo)
      expect(formatted).toContain('小时前')
    })

    it('should display days ago for recent days', async () => {
      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(threeDaysAgo)
      expect(formatted).toContain('天前')
    })

    it('should display date for old time', async () => {
      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      const vm = wrapper.vm as any
      const twoWeeksAgo = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString()
      const formatted = vm.formatTime(twoWeeksAgo)
      expect(formatted).toBeTruthy()
    })
  })

  describe('Component Lifecycle', () => {
    it('should fetch comments on mount', async () => {
      requestMock.get.mockResolvedValueOnce({ data: mockComments })

      mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(requestMock.get).toHaveBeenCalledWith('/tasks/100/comments')
    })

    it('should expose refresh method', async () => {
      requestMock.get.mockResolvedValue({ data: mockComments })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      expect(typeof vm.refresh).toBe('function')

      await vm.refresh()
      await flushPromises()

      expect(requestMock.get).toHaveBeenCalledTimes(2)
    })
  })

  describe('Edge Cases', () => {
    it('should handle comment without avatar', async () => {
      const commentNoAvatar = {
        ...mockComments[0],
        avatar: undefined,
      }
      requestMock.get.mockResolvedValueOnce({ data: [commentNoAvatar] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const avatars = wrapper.findAll('.comment-avatar')
      expect(avatars.length).toBeGreaterThan(0)
    })

    it('should handle multiline comment content', async () => {
      const multilineComment = {
        ...mockComments[0],
        content: '第一行\n第二行\n第三行',
      }
      requestMock.get.mockResolvedValueOnce({ data: [multilineComment] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      expect(wrapper.text()).toContain('第一行')
      expect(wrapper.text()).toContain('第二行')
      expect(wrapper.text()).toContain('第三行')
    })

    it('should trim whitespace from comment content before submission', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockResolvedValueOnce({
        data: {
          id: 3,
          taskId: 100,
          userId: 1,
          userName: '张三',
          content: '新评论',
          createdAt: '2026-01-26T12:00:00',
        },
      })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '  新评论  '
      await vm.handleSubmit()
      await flushPromises()

      expect(requestMock.post).toHaveBeenCalledWith('/tasks/100/comments', {
        userId: 1,
        content: '新评论',
      })
    })
  })

  describe('Comment Validation', () => {
    it('should reject empty comment content', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = ''
      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('请输入评论内容')
      expect(requestMock.post).not.toHaveBeenCalled()
    })

    it('should reject whitespace-only comment', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = '   '
      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('请输入评论内容')
      expect(requestMock.post).not.toHaveBeenCalled()
    })

    it('should reject comment exceeding 1000 characters', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = 'a'.repeat(1001)
      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('评论内容不能超过1000个字符')
      expect(requestMock.post).not.toHaveBeenCalled()
    })

    it('should accept valid comment with 1000 characters', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockResolvedValueOnce({
        data: {
          id: 3,
          taskId: 100,
          userId: 1,
          userName: '张三',
          content: 'a'.repeat(1000),
          createdAt: '2026-01-26T12:00:00',
        },
      })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = 'a'.repeat(1000)
      await vm.handleSubmit()
      await flushPromises()

      expect(requestMock.post).toHaveBeenCalled()
      expect(ElMessage.success).toHaveBeenCalledWith('评论发送成功')
    })

    it('should accept valid comment with minimum length', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })
      requestMock.post.mockResolvedValueOnce({
        data: {
          id: 3,
          taskId: 100,
          userId: 1,
          userName: '张三',
          content: 'a',
          createdAt: '2026-01-26T12:00:00',
        },
      })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const vm = wrapper.vm as any
      vm.newComment = 'a'
      await vm.handleSubmit()
      await flushPromises()

      expect(requestMock.post).toHaveBeenCalled()
      expect(ElMessage.success).toHaveBeenCalledWith('评论发送成功')
    })

    it('should show character limit in textarea', async () => {
      requestMock.get.mockResolvedValueOnce({ data: [] })

      const wrapper = mount(TaskComments, {
        props: {
          taskId: 100,
        },
      })

      await flushPromises()

      const textarea = wrapper.find('.el-textarea')
      expect(textarea.exists()).toBe(true)
      // Element Plus textarea with maxlength and show-word-limit should display character count
    })
  })
})
