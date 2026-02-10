import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage } from 'element-plus'
import CreateTeamDialog from '@/views/team/components/CreateTeamDialog.vue'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
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

// Mock project API
vi.mock('@/api/project', () => ({
  getProjects: vi.fn()
}))

describe('CreateTeamDialog', () => {
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

    // Mock getProjects
    vi.mocked(projectApi.getProjects).mockResolvedValue({
      list: [
        { id: 1, title: 'Project 1', status: 'RECRUITING' },
        { id: 2, title: 'Project 2', status: 'RECRUITING' }
      ],
      total: 2
    })

    // Clear mocks
    vi.clearAllMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const createWrapper = (props = {}) => {
    return mount(CreateTeamDialog, {
      props: {
        modelValue: true,
        ...props
      },
      global: {
        stubs: {
          ElDialog: true,
          ElForm: true,
          ElFormItem: true,
          ElInput: true,
          ElSelect: true,
          ElOption: true,
          ElButton: true,
          ElAvatar: true
        }
      }
    })
  }

  describe('Form Validation', () => {
    it('should have validation rules for team name', async () => {
      wrapper = createWrapper()
      await flushPromises()

      // Check that rules are defined
      expect(wrapper.vm.rules.name).toBeDefined()
      expect(wrapper.vm.rules.name.length).toBeGreaterThan(0)
    })

    it('should have validation rules for description', async () => {
      wrapper = createWrapper()
      await flushPromises()

      // Check that rules are defined
      expect(wrapper.vm.rules.description).toBeDefined()
    })

    it('should initialize with empty form data', async () => {
      wrapper = createWrapper()
      await flushPromises()

      expect(wrapper.vm.formData.name).toBe('')
      expect(wrapper.vm.formData.description).toBe('')
      expect(wrapper.vm.formData.avatar).toBe('')
      expect(wrapper.vm.formData.projectId).toBeUndefined()
    })
  })

  describe('Submit Success', () => {
    it('should create team successfully with valid data', async () => {
      const mockTeam = {
        id: 123,
        name: 'Test Team',
        description: 'Test Description',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
      }

      // Mock createTeam to return success
      teamStore.createTeam = vi.fn().mockResolvedValue(mockTeam)

      wrapper = createWrapper()
      await flushPromises()

      // Mock form validation
      if (wrapper.vm.formRef) {
        wrapper.vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      // Set form data directly
      wrapper.vm.formData.name = 'Test Team'
      wrapper.vm.formData.description = 'Test Description'

      // Call submit method directly
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Verify createTeam was called with correct data
      expect(teamStore.createTeam).toHaveBeenCalledWith({
        name: 'Test Team',
        description: 'Test Description',
        avatar: undefined,
        projectId: undefined
      })

      // Verify success message
      expect(ElMessage.success).toHaveBeenCalledWith('团队创建成功')

      // Verify navigation
      expect(mockPush).toHaveBeenCalledWith({
        name: 'TeamOverview',
        params: { id: 123 }
      })

      // Verify success event emitted
      expect(wrapper.emitted('success')).toBeTruthy()
      expect(wrapper.emitted('success')[0]).toEqual([123])
    })

    it('should create team with all fields filled', async () => {
      const mockTeam = {
        id: 456,
        name: 'Full Team',
        description: 'Full Description',
        avatar: 'https://example.com/avatar.jpg',
        projectId: 1,
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
      }

      teamStore.createTeam = vi.fn().mockResolvedValue(mockTeam)

      wrapper = createWrapper()
      await flushPromises()

      // Mock form validation
      if (wrapper.vm.formRef) {
        wrapper.vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      // Set all form fields
      wrapper.vm.formData.name = 'Full Team'
      wrapper.vm.formData.description = 'Full Description'
      wrapper.vm.formData.avatar = 'https://example.com/avatar.jpg'
      wrapper.vm.formData.projectId = 1

      // Call submit
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Verify createTeam was called with all data
      expect(teamStore.createTeam).toHaveBeenCalledWith({
        name: 'Full Team',
        description: 'Full Description',
        avatar: 'https://example.com/avatar.jpg',
        projectId: 1
      })
    })

    it('should trim whitespace from inputs', async () => {
      const mockTeam = {
        id: 789,
        name: 'Trimmed Team',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
      }

      teamStore.createTeam = vi.fn().mockResolvedValue(mockTeam)

      wrapper = createWrapper()
      await flushPromises()

      // Mock form validation
      if (wrapper.vm.formRef) {
        wrapper.vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      // Set form with whitespace
      wrapper.vm.formData.name = '  Trimmed Team  '
      wrapper.vm.formData.description = '  Trimmed Description  '

      // Call submit
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Verify whitespace was trimmed
      expect(teamStore.createTeam).toHaveBeenCalledWith({
        name: 'Trimmed Team',
        description: 'Trimmed Description',
        avatar: undefined,
        projectId: undefined
      })
    })
  })

  describe('Submit Failure', () => {
    it('should show error message when creation fails', async () => {
      const errorMessage = 'Network error'
      teamStore.createTeam = vi.fn().mockRejectedValue(new Error(errorMessage))

      wrapper = createWrapper()
      await flushPromises()

      // Mock form validation
      if (wrapper.vm.formRef) {
        wrapper.vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      // Set form data
      wrapper.vm.formData.name = 'Test Team'

      // Call submit
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Verify error message
      expect(ElMessage.error).toHaveBeenCalled()

      // Verify dialog is still open (no close event)
      expect(wrapper.emitted('update:modelValue')).toBeFalsy()
    })

    it('should not navigate when creation fails', async () => {
      teamStore.createTeam = vi.fn().mockRejectedValue(new Error('Failed'))

      wrapper = createWrapper()
      await flushPromises()

      // Mock form validation
      if (wrapper.vm.formRef) {
        wrapper.vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      // Set form data and submit
      wrapper.vm.formData.name = 'Test Team'
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Verify no navigation
      expect(mockPush).not.toHaveBeenCalled()

      // Verify no success event
      expect(wrapper.emitted('success')).toBeFalsy()
    })

    it('should not call createTeam when form ref is missing', async () => {
      teamStore.createTeam = vi.fn()

      wrapper = createWrapper()
      await flushPromises()

      // Remove form ref
      wrapper.vm.formRef = undefined

      // Try to submit
      await wrapper.vm.handleSubmit()

      // Verify createTeam was not called
      expect(teamStore.createTeam).not.toHaveBeenCalled()
    })
  })

  describe('Cancel Operation', () => {
    it('should close dialog when cancel is called', async () => {
      wrapper = createWrapper()
      await flushPromises()

      // Call cancel method
      wrapper.vm.handleCancel()

      // Verify dialog close event
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')[0]).toEqual([false])
    })

    it('should not create team when cancelled', async () => {
      teamStore.createTeam = vi.fn()

      wrapper = createWrapper()
      await flushPromises()

      // Set form data
      wrapper.vm.formData.name = 'Test Team'

      // Call cancel
      wrapper.vm.handleCancel()

      // Verify createTeam was not called
      expect(teamStore.createTeam).not.toHaveBeenCalled()
    })
  })

  describe('Reset Operation', () => {
    it('should reset form when reset is called', async () => {
      wrapper = createWrapper()
      await flushPromises()

      // Set form data
      wrapper.vm.formData.name = 'Test Team'
      wrapper.vm.formData.description = 'Test Description'
      wrapper.vm.formData.avatar = 'https://example.com/avatar.jpg'
      wrapper.vm.formData.projectId = 1

      // Call reset
      wrapper.vm.handleReset()
      await flushPromises()

      // Verify form data is reset
      expect(wrapper.vm.formData.name).toBe('')
      expect(wrapper.vm.formData.description).toBe('')
      expect(wrapper.vm.formData.avatar).toBe('')
      expect(wrapper.vm.formData.projectId).toBeUndefined()
    })
  })

  describe('Loading State', () => {
    it('should show loading state during submission', async () => {
      // Mock slow createTeam
      const mockTeam = {
        id: 123,
        name: 'Test Team',
        creatorId: 1,
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
      }
      
      teamStore.createTeam = vi.fn().mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve(mockTeam), 100))
      )

      wrapper = createWrapper()
      await flushPromises()

      // Mock form validation
      if (wrapper.vm.formRef) {
        wrapper.vm.formRef.validate = vi.fn().mockResolvedValue(true)
      }

      // Set form data and submit
      wrapper.vm.formData.name = 'Test Team'
      const submitPromise = wrapper.vm.handleSubmit()

      // Check loading state
      expect(wrapper.vm.submitting).toBe(true)

      // Wait for completion
      await submitPromise
      await flushPromises()

      expect(wrapper.vm.submitting).toBe(false)
    })
  })

  describe('Project Loading', () => {
    it('should load projects when dialog opens', async () => {
      wrapper = createWrapper()
      await flushPromises()

      // Verify getProjects was called
      expect(projectApi.getProjects).toHaveBeenCalledWith({
        page: 1,
        size: 100,
        status: 'RECRUITING'
      })
    })

    it('should handle project loading failure gracefully', async () => {
      vi.mocked(projectApi.getProjects).mockRejectedValue(new Error('Failed to load'))

      wrapper = createWrapper()
      await flushPromises()

      // Should not throw error, just log
      expect(wrapper.vm.projects).toEqual([])
    })
  })
})
