import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from './auth'
import {
  getUserTeams,
  getTeam,
  createTeam as createTeamApi,
  getTeamMembers,
  addTeamMember as addTeamMemberApi,
  removeTeamMember as removeTeamMemberApi,
  getTeamStatistics,
  getTeamActivities,
  leaveTeam as leaveTeamApi,
  deleteTeam as deleteTeamApi
} from '@/api/team'
import type {
  Team,
  TeamMember,
  TeamStatistics,
  TeamCreateRequest,
  TeamListQuery,
  TeamListResponse,
  TeamActivity
} from '@/types/team'
import { ParallelDataLoader } from '@/utils/ParallelDataLoader'
import { RequestDeduplicator } from '@/utils/RequestDeduplicator'
import { CacheManager } from '@/utils/CacheManager'
import type { LoadRequest } from '@/utils/ParallelDataLoader'

export const useTeamStore = defineStore('team', () => {
  // State
  const teams = ref<Team[]>([])
  const currentTeam = ref<Team | null>(null)
  const currentTeamMembers = ref<TeamMember[]>([])
  const currentTeamStatistics = ref<TeamStatistics | null>(null)
  const currentTeamActivities = ref<TeamActivity[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  
  // Section-specific loading states
  const sectionLoading = ref<Record<string, boolean>>({
    teamDetails: false,
    teamList: false,
    members: false,
    statistics: false,
    activities: false
  })
  
  // Section-specific errors
  const sectionErrors = ref<Map<string, Error>>(new Map())
  
  // Utilities
  const parallelLoader = new ParallelDataLoader()
  const requestDeduplicator = new RequestDeduplicator()
  const cacheManager = new CacheManager({
    maxSize: 50 * 1024 * 1024, // 50MB
    defaultTTL: 60000, // 60 seconds
    ttlByType: new Map([
      ['team-details', 60000],      // 60 seconds
      ['team-list', 120000],         // 120 seconds (2 minutes)
      ['team-members', 120000],      // 120 seconds
      ['team-statistics', 30000],    // 30 seconds
      ['team-activities', 30000]     // 30 seconds
    ])
  })

  // Getters
  const currentUserRole = computed(() => {
    const authStore = useAuthStore()
    if (!currentTeam.value || !authStore.user) return null
    
    const member = currentTeamMembers.value.find(
      m => m.userId === authStore.user?.id
    )
    return member?.role || null
  })

  const isCurrentUserOwner = computed(() => {
    const role = currentUserRole.value
    return role === 'OWNER' || role === 'LEADER' // 兼容旧角色
  })

  const isCurrentUserAdmin = computed(() => {
    const role = currentUserRole.value
    return role === 'ADMIN' || isCurrentUserOwner.value
  })

  const teamCount = computed(() => teams.value.length)

  const activeTeams = computed(() => 
    teams.value.filter(team => team.status === 'ACTIVE')
  )

  // Helper methods for section loading state
  const setSectionLoading = (section: string, isLoading: boolean) => {
    sectionLoading.value[section] = isLoading
  }

  const setSectionError = (section: string, err: Error | null) => {
    if (err) {
      sectionErrors.value.set(section, err)
    } else {
      sectionErrors.value.delete(section)
    }
  }

  const isLoading = (section: string): boolean => {
    return sectionLoading.value[section] || false
  }

  const hasError = (section: string): boolean => {
    return sectionErrors.value.has(section)
  }

  const getError = (section: string): Error | null => {
    return sectionErrors.value.get(section) || null
  }

  // Actions
  
  /**
   * Load team page with parallel data loading and priority-based execution
   * Implements cache-then-refresh pattern for optimal performance
   * @param teamId Team ID to load
   * @param page Page type: 'overview' | 'tasks' | 'members'
   */
  const loadTeamPage = async (teamId: number, page: 'overview' | 'tasks' | 'members' = 'overview') => {
    loading.value = true
    error.value = null
    
    // Clear previous section errors
    sectionErrors.value.clear()
    
    try {
      const authStore = useAuthStore()
      const userId = authStore.user?.id
      
      // Check cache first and serve immediately if available
      const cacheKeys = {
        teamDetails: `team-details:${teamId}`,
        teamList: userId ? `team-list:${userId}` : null,
        members: `team-members:${teamId}`,
        statistics: `team-statistics:${teamId}`,
        activities: `team-activities:${teamId}`
      }
      
      let hasAnyCachedData = false
      const needsRefresh: string[] = []
      
      // Serve cached data immediately
      Object.entries(cacheKeys).forEach(([key, cacheKey]) => {
        if (!cacheKey) return
        
        const cached = cacheManager.get(cacheKey)
        if (cached) {
          hasAnyCachedData = true
          
          // Update state with cached data
          switch (key) {
            case 'teamDetails':
              currentTeam.value = cached as Team
              break
            case 'teamList':
              teams.value = Array.isArray(cached) ? cached : (cached as TeamListResponse).records
              break
            case 'members':
              currentTeamMembers.value = cached as TeamMember[]
              break
            case 'statistics':
              currentTeamStatistics.value = cached as TeamStatistics
              break
            case 'activities':
              currentTeamActivities.value = cached as TeamActivity[]
              break
          }
          
          // Check if cache is stale (older than 30 seconds)
          if (cacheManager.isStale(cacheKey, 30000)) {
            needsRefresh.push(key)
          }
        }
      })
      
      // If we have cached data, render immediately and optionally refresh in background
      if (hasAnyCachedData) {
        loading.value = false
        
        // If cache is stale, initiate background refresh
        if (needsRefresh.length > 0) {
          // Background refresh without blocking UI
          setTimeout(() => {
            refreshTeamData(teamId, page, needsRefresh)
          }, 100)
        }
        
        // If we have all required data cached and it's fresh, we're done
        const requiredKeys = page === 'overview' 
          ? ['teamDetails', 'teamList', 'members', 'statistics', 'activities']
          : ['teamDetails', 'teamList', 'members']
        
        const hasAllRequired = requiredKeys.every(key => {
          const cacheKey = cacheKeys[key as keyof typeof cacheKeys]
          return cacheKey && cacheManager.get(cacheKey) && !needsRefresh.includes(key)
        })
        
        if (hasAllRequired) {
          return
        }
      }
      
      // Define requests with priorities
      const requests: LoadRequest<any>[] = [
        // Critical: Team details (highest priority)
        {
          key: 'teamDetails',
          priority: 'critical',
          loader: () => requestDeduplicator.dedupe(
            { endpoint: `/teams/${teamId}`, method: 'GET', params: {} },
            () => getTeam(teamId, false) // Don't use API cache, we'll manage our own
          )
        },
        // High: Team list for navigation
        {
          key: 'teamList',
          priority: 'high',
          loader: () => userId ? requestDeduplicator.dedupe(
            { endpoint: `/teams/user/${userId}`, method: 'GET', params: {} },
            () => getUserTeams(userId, {}, false)
          ) : Promise.resolve([])
        },
        // High: Team members
        {
          key: 'members',
          priority: 'high',
          loader: () => requestDeduplicator.dedupe(
            { endpoint: `/teams/${teamId}/members`, method: 'GET', params: {} },
            () => getTeamMembers(teamId, false)
          )
        }
      ]
      
      // Add page-specific requests
      if (page === 'overview') {
        // Normal: Statistics
        requests.push({
          key: 'statistics',
          priority: 'normal',
          loader: () => requestDeduplicator.dedupe(
            { endpoint: `/teams/${teamId}/statistics`, method: 'GET', params: {} },
            () => getTeamStatistics(teamId, false)
          )
        })
        
        // Low: Activities
        requests.push({
          key: 'activities',
          priority: 'low',
          loader: () => requestDeduplicator.dedupe(
            { endpoint: `/teams/${teamId}/activities`, method: 'GET', params: { limit: 10 } },
            () => getTeamActivities(teamId, 10)
          )
        })
      }
      
      // Set all sections to loading
      requests.forEach(req => setSectionLoading(req.key, true))
      
      // Execute requests with priority-based loading
      const results = await parallelLoader.loadWithPriority(requests)
      
      // Process results and update state + cache
      results.forEach((result, key) => {
        setSectionLoading(key, false)
        
        if (result.error) {
          setSectionError(key, result.error)
          console.error(`Failed to load ${key}:`, result.error)
        } else {
          setSectionError(key, null)
          
          // Get cache key and TTL for this data type
          const cacheKey = cacheKeys[key as keyof typeof cacheKeys]
          const ttl = cacheManager['config'].ttlByType.get(`team-${key.toLowerCase().replace(/([A-Z])/g, '-$1').toLowerCase()}`)
          
          // Update cache with fresh data
          if (cacheKey && result.data) {
            cacheManager.set(cacheKey, result.data, ttl)
          }
          
          // Update corresponding state based on key
          switch (key) {
            case 'teamDetails':
              currentTeam.value = result.data as Team
              break
            case 'teamList':
              if (Array.isArray(result.data)) {
                teams.value = result.data
              } else if (result.data && 'records' in result.data) {
                teams.value = (result.data as TeamListResponse).records
              }
              break
            case 'members':
              currentTeamMembers.value = result.data as TeamMember[]
              break
            case 'statistics':
              currentTeamStatistics.value = result.data as TeamStatistics
              break
            case 'activities':
              currentTeamActivities.value = result.data as TeamActivity[]
              break
          }
        }
      })
      
      // If critical data (team details) failed, throw error
      const teamDetailsResult = results.get('teamDetails')
      if (teamDetailsResult?.error) {
        throw teamDetailsResult.error
      }
      
    } catch (err: any) {
      error.value = err.message || '加载团队页面失败'
      console.error('Failed to load team page:', err)
      throw err
    } finally {
      loading.value = false
    }
  }
  
  /**
   * Background refresh for stale cached data
   * @param teamId Team ID
   * @param page Page type
   * @param sectionsToRefresh Sections that need refresh
   */
  const refreshTeamData = async (
    teamId: number, 
    page: 'overview' | 'tasks' | 'members',
    sectionsToRefresh: string[]
  ) => {
    const authStore = useAuthStore()
    const userId = authStore.user?.id
    
    const refreshPromises: Promise<void>[] = []
    
    sectionsToRefresh.forEach(section => {
      const promise = (async () => {
        try {
          let data: any
          let cacheKey: string
          let ttl: number | undefined
          
          switch (section) {
            case 'teamDetails':
              data = await getTeam(teamId, false)
              cacheKey = `team-details:${teamId}`
              ttl = cacheManager['config'].ttlByType.get('team-details')
              currentTeam.value = data
              break
            case 'teamList':
              if (userId) {
                data = await getUserTeams(userId, {}, false)
                cacheKey = `team-list:${userId}`
                ttl = cacheManager['config'].ttlByType.get('team-list')
                teams.value = Array.isArray(data) ? data : data.records
              }
              return
            case 'members':
              data = await getTeamMembers(teamId, false)
              cacheKey = `team-members:${teamId}`
              ttl = cacheManager['config'].ttlByType.get('team-members')
              currentTeamMembers.value = data
              break
            case 'statistics':
              data = await getTeamStatistics(teamId, false)
              cacheKey = `team-statistics:${teamId}`
              ttl = cacheManager['config'].ttlByType.get('team-statistics')
              currentTeamStatistics.value = data
              break
            case 'activities':
              data = await getTeamActivities(teamId, 10)
              cacheKey = `team-activities:${teamId}`
              ttl = cacheManager['config'].ttlByType.get('team-activities')
              currentTeamActivities.value = data
              break
            default:
              return
          }
          
          // Update cache with fresh data
          if (cacheKey! && data) {
            cacheManager.set(cacheKey, data, ttl)
          }
        } catch (err) {
          console.warn(`Background refresh failed for ${section}:`, err)
        }
      })()
      
      refreshPromises.push(promise)
    })
    
    await Promise.allSettled(refreshPromises)
  }
  
  /**
   * Retry a failed section
   * @param section Section key to retry
   * @param teamId Team ID
   */
  const retryFailedSection = async (section: string, teamId: number) => {
    setSectionLoading(section, true)
    setSectionError(section, null)
    
    try {
      let data: any
      
      switch (section) {
        case 'teamDetails':
          data = await getTeam(teamId, false)
          currentTeam.value = data
          break
        case 'teamList':
          const authStore = useAuthStore()
          if (authStore.user?.id) {
            data = await getUserTeams(authStore.user.id, {}, false)
            teams.value = Array.isArray(data) ? data : data.records
          }
          break
        case 'members':
          data = await getTeamMembers(teamId, false)
          currentTeamMembers.value = data
          break
        case 'statistics':
          data = await getTeamStatistics(teamId, false)
          currentTeamStatistics.value = data
          break
        case 'activities':
          data = await getTeamActivities(teamId, 10)
          currentTeamActivities.value = data
          break
        default:
          throw new Error(`Unknown section: ${section}`)
      }
    } catch (err: any) {
      setSectionError(section, err)
      throw err
    } finally {
      setSectionLoading(section, false)
    }
  }
  
  /**
   * 获取用户团队列表
   */
  const fetchUserTeams = async (params: TeamListQuery) => {
    loading.value = true
    error.value = null
    try {
      const response = await getUserTeams(params.userId, params)
      
      // 处理不同的响应格式
      if (Array.isArray(response)) {
        teams.value = response
      } else if ('records' in response) {
        teams.value = (response as TeamListResponse).records
      } else {
        teams.value = []
      }
      
      return teams.value
    } catch (err: any) {
      error.value = err.message || '获取团队列表失败'
      console.error('Failed to fetch user teams:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取团队详情
   * 使用并行请求优化，同时获取团队信息和成员列表
   */
  const fetchTeamDetail = async (teamId: number) => {
    loading.value = true
    error.value = null
    try {
      // 并行请求团队信息和成员列表，提升性能
      const [teamData, membersData] = await Promise.all([
        getTeam(teamId),
        getTeamMembers(teamId)
      ])
      
      currentTeam.value = teamData
      currentTeamMembers.value = membersData
      
      // 如果响应包含统计数据，使用它；否则尝试单独获取
      if (teamData && typeof teamData === 'object' && 'statistics' in teamData) {
        currentTeamStatistics.value = teamData.statistics
      } else {
        try {
          currentTeamStatistics.value = await getTeamStatistics(teamId)
        } catch (e) {
          // 统计数据获取失败不影响主流程
          console.warn('Failed to fetch team statistics:', e)
          currentTeamStatistics.value = null
        }
      }
      
      return { team: currentTeam.value, members: currentTeamMembers.value }
    } catch (err: any) {
      error.value = err.message || '获取团队详情失败'
      console.error('Failed to fetch team detail:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建团队
   */
  const createTeam = async (data: TeamCreateRequest): Promise<Team> => {
    loading.value = true
    error.value = null
    try {
      const authStore = useAuthStore()
      
      // 确保 leaderId 存在（兼容旧 API）
      const requestData = {
        ...data,
        leaderId: data.leaderId || authStore.user?.id
      }
      
      const team = await createTeamApi(requestData)
      
      // 将新团队添加到列表开头
      teams.value.unshift(team)
      
      return team
    } catch (err: any) {
      error.value = err.message || '创建团队失败'
      console.error('Failed to create team:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 添加团队成员
   */
  const addTeamMember = async (teamId: number, userId: number, role?: string) => {
    loading.value = true
    error.value = null
    try {
      await addTeamMemberApi(teamId, userId, role)
      
      // 如果是当前团队，重新加载成员列表
      if (currentTeam.value?.id === teamId) {
        const members = await getTeamMembers(teamId)
        currentTeamMembers.value = members
      }
    } catch (err: any) {
      error.value = err.message || '添加团队成员失败'
      console.error('Failed to add team member:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 移除团队成员
   */
  const removeTeamMember = async (teamId: number, userId: number) => {
    loading.value = true
    error.value = null
    try {
      await removeTeamMemberApi(teamId, userId)
      
      // 如果是当前团队，从成员列表中移除
      if (currentTeam.value?.id === teamId) {
        currentTeamMembers.value = currentTeamMembers.value.filter(
          m => m.userId !== userId
        )
      }
    } catch (err: any) {
      error.value = err.message || '移除团队成员失败'
      console.error('Failed to remove team member:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出团队（普通成员）
   */
  const leaveTeam = async (teamId: number) => {
    loading.value = true
    error.value = null
    try {
      await leaveTeamApi(teamId)
      
      // 从团队列表中移除
      teams.value = teams.value.filter(t => t.id !== teamId)
      
      // 如果是当前团队，清除当前团队数据
      if (currentTeam.value?.id === teamId) {
        clearCurrentTeam()
      }
    } catch (err: any) {
      error.value = err.message || '退出团队失败'
      console.error('Failed to leave team:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 删除团队（仅领导者）
   */
  const deleteTeam = async (teamId: number) => {
    loading.value = true
    error.value = null
    try {
      await deleteTeamApi(teamId)
      
      // 从团队列表中移除
      teams.value = teams.value.filter(t => t.id !== teamId)
      
      // 如果是当前团队，清除当前团队数据
      if (currentTeam.value?.id === teamId) {
        clearCurrentTeam()
      }
    } catch (err: any) {
      error.value = err.message || '删除团队失败'
      console.error('Failed to delete team:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新团队信息（本地）
   */
  const updateTeamLocal = (teamId: number, updates: Partial<Team>) => {
    // 更新列表中的团队
    const index = teams.value.findIndex(t => t.id === teamId)
    if (index !== -1) {
      teams.value[index] = { ...teams.value[index], ...updates }
    }
    
    // 更新当前团队
    if (currentTeam.value?.id === teamId) {
      currentTeam.value = { ...currentTeam.value, ...updates }
    }
  }

  /**
   * 清除当前团队数据
   */
  const clearCurrentTeam = () => {
    currentTeam.value = null
    currentTeamMembers.value = []
    currentTeamStatistics.value = null
  }

  /**
   * 清除所有数据
   */
  const clearAll = () => {
    teams.value = []
    clearCurrentTeam()
    error.value = null
  }

  /**
   * 重置错误状态
   */
  const resetError = () => {
    error.value = null
  }

  return {
    // State
    teams,
    currentTeam,
    currentTeamMembers,
    currentTeamStatistics,
    currentTeamActivities,
    loading,
    error,
    sectionLoading,
    sectionErrors,
    
    // Getters
    currentUserRole,
    isCurrentUserOwner,
    isCurrentUserAdmin,
    teamCount,
    activeTeams,
    isLoading,
    hasError,
    getError,
    
    // Actions
    loadTeamPage,
    refreshTeamData,
    retryFailedSection,
    fetchUserTeams,
    fetchTeamDetail,
    createTeam,
    addTeamMember,
    removeTeamMember,
    leaveTeam,
    deleteTeam,
    updateTeamLocal,
    clearCurrentTeam,
    clearAll,
    resetError
  }
})
