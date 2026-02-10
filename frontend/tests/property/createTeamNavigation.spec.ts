import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import * as fc from 'fast-check'
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

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

// Mock project API
vi.mock('@/api/project', () => ({
  getProjects: vi.fn()
}))

/**
 * Property Test 5: 创建后导航
 * 验证需求: 2.2.4
 * 
 * 属性: 成功创建团队后，应该导航到新创建的团队空间
 */
describe('Feature: team-management, Property 5: 创建后导航', () => {
  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const authStore = useAuthStore()
    authStore.user = {
      id: 1,
      username: 'testuser',
      email: 'test@example.com'
    }

    vi.mocked(projectApi.getProjects).mockResolvedValue({
      list: [],
      total: 0
    })

    vi.clearAllMocks()
  })

  it('should navigate to team space after successful creation', () => {
    fc.assert(
      fc.asyncProperty(
        // Generate random team data
        fc.record({
          id: fc.integer({ min: 1, max: 10000 }),
          name: fc.string({ minLength: 2, maxLength: 50 })
            .filter(s => /^[a-zA-Z0-9\u4e00-\u9fa5_]+$/.test(s) && s.trim().length >= 2),
          description: fc.option(fc.string({ maxLength: 500 })),
          creatorId: fc.constant(1),
          status: fc.constant('ACTIVE' as const),
          createdAt: fc.date(),
          updatedAt: fc.date()
        }),
        async (teamData) => {
          const pinia = createPinia()
          setActivePinia(pinia)

          const teamStore = useTeamStore()
          const authStore = useAuthStore()
          authStore.user = {
            id: 1,
            username: 'testuser',
            email: 'test@example.com'
          }

          // Mock createTeam to return the generated team
          teamStore.createTeam = vi.fn().mockResolvedValue(teamData)

          const wrapper = mount(CreateTeamDialog, {
            props: {
              modelValue: true
            },
            global: {
              stubs: {
                ElDialog: false,
                ElForm: false,
                ElFormItem: false,
                ElInput: false,
                ElSelect: false,
                ElButton: false
              }
            }
          })

          await flushPromises()

          // Fill form with generated data
          const nameInput = wrapper.find('input[placeholder*="团队名称"]')
          await nameInput.setValue(teamData.name)

          if (teamData.description) {
            const descInput = wrapper.find('textarea[placeholder*="团队描述"]')
            await descInput.setValue(teamData.description)
          }

          // Submit form
          const submitButton = wrapper.findAll('button').find((btn: any) => 
            btn.text().includes('创建')
          )
          await submitButton?.trigger('click')
          await flushPromises()

          // Verify navigation to the correct team space
          expect(mockPush).toHaveBeenCalledWith({
            name: 'TeamOverview',
            params: { id: teamData.id }
          })

          // Verify success event emitted with correct team ID
          expect(wrapper.emitted('success')).toBeTruthy()
          expect(wrapper.emitted('success')[0]).toEqual([teamData.id])

          wrapper.unmount()
        }
      ),
      { numRuns: 50 } // Reduced runs for async tests
    )
  })

  it('should not navigate when creation fails', () => {
    fc.assert(
      fc.asyncProperty(
        // Generate random valid team names
        fc.string({ minLength: 2, maxLength: 50 })
          .filter(s => /^[a-zA-Z0-9\u4e00-\u9fa5_]+$/.test(s) && s.trim().length >= 2),
        // Generate random error messages
        fc.string({ minLength: 1, maxLength: 100 }),
        async (teamName, errorMessage) => {
          const pinia = createPinia()
          setActivePinia(pinia)

          const teamStore = useTeamStore()
          const authStore = useAuthStore()
          authStore.user = {
            id: 1,
            username: 'testuser',
            email: 'test@example.com'
          }

          // Mock createTeam to fail
          teamStore.createTeam = vi.fn().mockRejectedValue(new Error(errorMessage))

          // Clear previous mock calls
          mockPush.mockClear()

          const wrapper = mount(CreateTeamDialog, {
            props: {
              modelValue: true
            },
            global: {
              stubs: {
                ElDialog: false,
                ElForm: false,
                ElFormItem: false,
                ElInput: false,
                ElSelect: false,
                ElButton: false
              }
            }
          })

          await flushPromises()

          // Fill and submit form
          const nameInput = wrapper.find('input[placeholder*="团队名称"]')
          await nameInput.setValue(teamName)

          const submitButton = wrapper.findAll('button').find((btn: any) => 
            btn.text().includes('创建')
          )
          await submitButton?.trigger('click')
          await flushPromises()

          // Verify no navigation occurred
          expect(mockPush).not.toHaveBeenCalled()

          // Verify no success event emitted
          expect(wrapper.emitted('success')).toBeFalsy()

          wrapper.unmount()
        }
      ),
      { numRuns: 50 }
    )
  })

  it('should emit success event with correct team ID', () => {
    fc.assert(
      fc.asyncProperty(
        // Generate random team IDs
        fc.integer({ min: 1, max: 100000 }),
        // Generate random valid team names
        fc.string({ minLength: 2, maxLength: 50 })
          .filter(s => /^[a-zA-Z0-9\u4e00-\u9fa5_]+$/.test(s) && s.trim().length >= 2),
        async (teamId, teamName) => {
          const pinia = createPinia()
          setActivePinia(pinia)

          const teamStore = useTeamStore()
          const authStore = useAuthStore()
          authStore.user = {
            id: 1,
            username: 'testuser',
            email: 'test@example.com'
          }

          const mockTeam = {
            id: teamId,
            name: teamName,
            creatorId: 1,
            status: 'ACTIVE' as const,
            createdAt: new Date(),
            updatedAt: new Date()
          }

          teamStore.createTeam = vi.fn().mockResolvedValue(mockTeam)

          const wrapper = mount(CreateTeamDialog, {
            props: {
              modelValue: true
            },
            global: {
              stubs: {
                ElDialog: false,
                ElForm: false,
                ElFormItem: false,
                ElInput: false,
                ElSelect: false,
                ElButton: false
              }
            }
          })

          await flushPromises()

          // Fill and submit form
          const nameInput = wrapper.find('input[placeholder*="团队名称"]')
          await nameInput.setValue(teamName)

          const submitButton = wrapper.findAll('button').find((btn: any) => 
            btn.text().includes('创建')
          )
          await submitButton?.trigger('click')
          await flushPromises()

          // Verify success event contains the correct team ID
          const successEvents = wrapper.emitted('success')
          expect(successEvents).toBeTruthy()
          expect(successEvents![0]).toEqual([teamId])

          wrapper.unmount()
        }
      ),
      { numRuns: 50 }
    )
  })

  it('should close dialog after successful creation', () => {
    fc.assert(
      fc.asyncProperty(
        // Generate random team data
        fc.record({
          id: fc.integer({ min: 1, max: 10000 }),
          name: fc.string({ minLength: 2, maxLength: 50 })
            .filter(s => /^[a-zA-Z0-9\u4e00-\u9fa5_]+$/.test(s) && s.trim().length >= 2),
          creatorId: fc.constant(1),
          status: fc.constant('ACTIVE' as const),
          createdAt: fc.date(),
          updatedAt: fc.date()
        }),
        async (teamData) => {
          const pinia = createPinia()
          setActivePinia(pinia)

          const teamStore = useTeamStore()
          const authStore = useAuthStore()
          authStore.user = {
            id: 1,
            username: 'testuser',
            email: 'test@example.com'
          }

          teamStore.createTeam = vi.fn().mockResolvedValue(teamData)

          const wrapper = mount(CreateTeamDialog, {
            props: {
              modelValue: true
            },
            global: {
              stubs: {
                ElDialog: false,
                ElForm: false,
                ElFormItem: false,
                ElInput: false,
                ElSelect: false,
                ElButton: false
              }
            }
          })

          await flushPromises()

          // Fill and submit form
          const nameInput = wrapper.find('input[placeholder*="团队名称"]')
          await nameInput.setValue(teamData.name)

          const submitButton = wrapper.findAll('button').find((btn: any) => 
            btn.text().includes('创建')
          )
          await submitButton?.trigger('click')
          await flushPromises()

          // Verify dialog close event
          const updateEvents = wrapper.emitted('update:modelValue')
          expect(updateEvents).toBeTruthy()
          expect(updateEvents![updateEvents!.length - 1]).toEqual([false])

          wrapper.unmount()
        }
      ),
      { numRuns: 50 }
    )
  })
})
