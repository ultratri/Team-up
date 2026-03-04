import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useTeamStore } from '../store/team'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { requiresAuth: false, transition: 'fade' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
    meta: { requiresAuth: false, transition: 'fade' },
  },
  {
    path: '/need-confirm',
    name: 'NeedConfirm',
    component: () => import('../views/auth/NeedConfirm.vue'),
    meta: { requiresAuth: false, transition: 'fade' },
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: (to) => {
          const authStore = useAuthStore()
          return authStore.isAuthenticated ? '/project' : '/login'
        },
      },
      {
        path: 'project',
        name: 'Project',
        component: () => import('../views/project/ProjectSquare.vue'),
        meta: { requiresAuth: false, transition: 'slide-fade' },
      },
      {
        path: 'project/recommended',
        name: 'RecommendedProjects',
        component: () => import('../views/project/RecommendedProjects.vue'),
        meta: { requiresAuth: true, title: '为我推荐', transition: 'slide-fade' },
      },
      {
        path: 'project/:id/candidates',
        name: 'ProjectCandidates',
        component: () => import('../views/project/ProjectCandidates.vue'),
        meta: { requiresAuth: true, title: '推荐候选人', transition: 'slide-fade' },
      },
      {
        path: 'project/:id',
        name: 'ProjectDetail',
        component: () => import('../views/project/ProjectDetail.vue'),
        meta: { requiresAuth: false, transition: 'slide-fade' },
      }
      ,
      {
        path: 'competition',
        name: 'CompetitionList',
        component: () => import('../views/competition/CompetitionList.vue'),
        meta: { requiresAuth: false, title: '比赛广场', transition: 'slide-fade' },
      },
      {
        path: 'competition/:id',
        name: 'CompetitionDetail',
        component: () => import('../views/competition/CompetitionDetail.vue'),
        meta: { requiresAuth: false, transition: 'slide-fade' },
      },
      {
        path: 'ecosystem',
        name: 'EcosystemHub',
        component: () => import('../views/ecosystem/EcosystemHub.vue'),
        meta: { requiresAuth: false, title: '生态广场', transition: 'slide-fade' },
      },
      {
        path: 'mentor/plaza',
        name: 'MentorPlaza',
        component: () => import('../views/mentor/MentorPlaza.vue'),
        meta: { requiresAuth: true, title: '导师广场', transition: 'slide-fade' },
      },
      {
        path: 'mentor/scoring',
        name: 'MentorScoring',
        component: () => import('../views/competition/MentorScoring.vue'),
        meta: { requiresAuth: true, roles: ['MENTOR', 'PLATFORM_ADMIN'], title: '导师评分', transition: 'slide-fade' },
      },
      {
        path: 'mentor/applications',
        name: 'MentorApplications',
        component: () => import('../views/mentor/MentorApplications.vue'),
        meta: { requiresAuth: true, roles: ['MENTOR', 'PLATFORM_ADMIN'], title: '团队指导申请', transition: 'slide-fade' },
      },
      {
        path: 'team/join-applications/my',
        name: 'MyJoinApplications',
        component: () => import('../views/team/MyJoinApplications.vue'),
        meta: { requiresAuth: true, title: '我的入队申请', transition: 'slide-fade' },
      },
      {
        path: 'team/invitations',
        name: 'TeamInvitations',
        component: () => import('../views/team/InvitationManagement.vue'),
        meta: { requiresAuth: true, title: '邀请管理', transition: 'slide-fade' },
      },

      {
        path: 'team',
        name: 'TeamList',
        component: () => import('../views/team/TeamList.vue'),
        meta: { requiresAuth: true, title: '团队', transition: 'slide-fade' },
      },
      {
        path: 'team/:id',
        component: () => import('../views/team/TeamSpace.vue'),
        meta: { requiresAuth: true, requiresTeamMember: true, transition: 'slide-fade' },
        children: [
          {
            path: '',
            redirect: { name: 'TeamOverview' }
          },
          {
            path: 'overview',
            name: 'TeamOverview',
            component: () => import('../views/team/TeamOverview.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '团队概览', transition: 'slide-fade' }
          },
          {
            path: 'tasks',
            name: 'TeamTasks',
            component: () => import('../views/team/TaskBoard.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '任务看板', transition: 'slide-fade' }
          },
          {
            path: 'files',
            name: 'TeamFiles',
            component: () => import('../views/team/FileShare.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '文件共享', transition: 'slide-fade' }
          },
          {
            path: 'chat',
            name: 'TeamChat',
            component: () => import('../views/team/Chat.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '团队聊天', transition: 'slide-fade' }
          },
          {
            path: 'evaluation',
            name: 'TeamEvaluation',
            component: () => import('../views/team/Evaluation.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '成员互评', transition: 'slide-fade' }
          },
          {
            path: 'sprints',
            name: 'TeamSprints',
            component: () => import('../views/team/SprintManage.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: 'Sprint管理', transition: 'slide-fade' }
          },
          {
            path: 'standup',
            name: 'TeamStandup',
            component: () => import('../views/team/DailyStandup.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '每日站会', transition: 'slide-fade' }
          },
          {
            path: 'settings',
            name: 'TeamSettings',
            component: () => import('../views/team/TeamSettings.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '团队设置', transition: 'slide-fade' }
          },
          {
            path: 'candidates',
            name: 'TeamCandidates',
            component: () => import('../views/team/TeamCandidates.vue'),
            meta: { requiresAuth: true, requiresTeamMember: true, title: '团队找成员', transition: 'slide-fade' }
          }
        ]
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/profile/ProfileView.vue'),
        meta: { requiresAuth: true, transition: 'slide-fade' },
      },
      {
        path: 'account/settings',
        name: 'AccountSettings',
        component: () => import('../views/profile/AccountSettings.vue'),
        meta: { requiresAuth: true, title: '账户设置', transition: 'slide-fade' },
      },
      {
        path: 'profile/edit',
        name: 'ProfileEdit',
        component: () => import('../views/profile/ProfileEdit.vue'),
        meta: { requiresAuth: true, title: '编辑档案', transition: 'slide-fade' },
      },
      {
        path: 'profile/history',
        name: 'ProjectHistory',
        component: () => import('../views/profile/ProjectHistory.vue'),
        meta: { requiresAuth: true, title: '项目履历', transition: 'slide-fade' },
      },
      {
        path: 'profile/mentor-application',
        name: 'MentorApplication',
        component: () => import('../views/profile/MentorApplication.vue'),
        meta: { requiresAuth: true, title: '申请成为导师', transition: 'slide-fade' },
      },
      {
        path: 'account/settings',
        name: 'AccountSettings',
        component: () => import('../views/profile/AccountSettings.vue'),
        meta: { requiresAuth: true, title: '账户设置', transition: 'slide-fade' },
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('../views/notification/NotificationCenter.vue'),
        meta: { requiresAuth: true, transition: 'slide-fade' },
      },
      // 已废弃：消息中心功能已整合到团队聊天中
      // {
      //   path: 'messages',
      //   name: 'Messages',
      //   component: () => import('../views/message/MessageCenter.vue'),
      //   meta: { requiresAuth: true, transition: 'slide-fade' },
      // },
      {
        path: 'applications',
        name: 'Applications',
        component: () => import('../views/project/ApplicationManage.vue'),
        meta: { requiresAuth: true, transition: 'slide-fade' },
      },

      {
        path: 'projects/my',
        name: 'MyProjects',
        component: () => import('../views/project/MyProjects.vue'),
        meta: { requiresAuth: true, title: '我的项目', transition: 'slide-fade' },
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/StatsDashboard.vue'),
        meta: { requiresAuth: true, transition: 'slide-fade' },
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], transition: 'slide-fade' },
      },
      {
        path: 'competition/manage',
        name: 'AdminCompetitions',
        component: () => import('../views/admin/CompetitionManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN'], title: '比赛管理', transition: 'slide-fade' },
      },
      {
        path: 'competition/templates',
        name: 'AdminTemplates',
        component: () => import('../views/admin/CompetitionManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN', 'MENTOR'], title: '比赛模板', transition: 'slide-fade' },
      },
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN'], title: '用户管理', transition: 'slide-fade' },
      },
      {
        path: 'teams/manage',
        name: 'TeamManage',
        component: () => import('../views/admin/TeamManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '团队管理', transition: 'slide-fade' },
      },
      {
        path: 'admin/announcements',
        name: 'AnnouncementManage',
        component: () => import('../views/admin/AnnouncementManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN'], title: '公告管理', transition: 'slide-fade' },
      },
      {
        path: 'admin/audit-logs',
        name: 'AuditLog',
        component: () => import('../views/admin/AuditLog.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN'], title: '操作日志', transition: 'slide-fade' },
      },
      {
        path: 'admin/settings',
        name: 'SystemSettings',
        component: () => import('../views/admin/SystemSettings.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN'], title: '系统设置', transition: 'slide-fade' },
      },
      {
        path: 'admin/export',
        name: 'DataExport',
        component: () => import('../views/admin/DataExport.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN', 'DEPT_ADMIN'], title: '数据导出', transition: 'slide-fade' },
      },
      {
        path: 'admin/department-major',
        name: 'DepartmentMajorManage',
        component: () => import('../views/admin/DepartmentMajorManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '院系专业管理', transition: 'slide-fade' },
      },
      {
        path: 'admin/newbie',
        name: 'NewbieProtection',
        component: () => import('../views/admin/NewbieProtection.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '新手保护', transition: 'slide-fade' },
      },
      {
        path: 'admin/content',
        name: 'ContentManage',
        component: () => import('../views/admin/ContentManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '内容管理', transition: 'slide-fade' },
      },
      {
        path: 'admin/reports',
        name: 'ReportManage',
        component: () => import('../views/admin/ReportManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '举报管理', transition: 'slide-fade' },
      },
      {
        path: 'admin/tags',
        name: 'TagManage',
        component: () => import('../views/admin/TagManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '标签管理', transition: 'slide-fade' },
      },
      {
        path: 'test-auth',
        name: 'TestAuth',
        component: () => import('../views/admin/TestAuth.vue'),
        meta: { requiresAuth: true, title: '认证测试', transition: 'slide-fade' },
      },
      {
        path: 'mentor/system/applications',
        name: 'MentorSystemApplications',
        component: () => import('../views/admin/MentorApplications.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '成为导师申请', transition: 'slide-fade' },
      },
      {
        path: 'mentor/system/manage',
        name: 'MentorSystemManage',
        component: () => import('../views/admin/MentorManage.vue'),
        meta: { requiresAuth: true, roles: ['PLATFORM_ADMIN'], title: '导师资格管理', transition: 'slide-fade' },
      },
      {
        path: 'animation-demo',
        name: 'AnimationDemo',
        component: () => import('../views/AnimationDemo.vue'),
        meta: { requiresAuth: false, transition: 'slide-fade' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 优化滚动行为，提升用户体验
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置（如浏览器前进/后退），恢复它
    if (savedPosition) {
      return savedPosition
    }
    // 如果有 hash，滚动到对应元素
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    // 默认滚动到顶部，使用平滑滚动
    return { top: 0, behavior: 'smooth' }
  },
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  // 如果是登录或注册页面，直接放行
  if (to.meta.requiresAuth === false) {
    // 如果已登录且访问登录页，重定向到首页
    if ((to.name === 'Login' || to.name === 'Register') && authStore.isAuthenticated) {
      next({ name: 'Project' })
      return
    }
    next()
    return
  }
  
  // 需要认证的页面
  if (!authStore.isAuthenticated) {
    ElMessage.error('未认证，请先登录')
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  
  // 检查角色权限
  if (to.meta.roles && !authStore.hasRole(to.meta.roles as string[])) {
    ElMessage.warning('权限不足，无法访问该页面')
    next({ name: 'Project' })
    return
  }
  
  const isProfileComplete = () => {
    // 管理员不需要完善个人资料
    if (authStore.hasRole(['PLATFORM_ADMIN', 'DEPT_ADMIN'])) {
      return true
    }

    const u: any = authStore.user
    const p: any = u?.profile
    
    if (!p) {
      return false
    }

    // 要求“我的”页面资料全部填完：?UserProfile 字段为准
    const requiredKeys = [
      'realName',
      'department',
      'major',
      'grade',
      'wechat',
      'qq',
      'bio',
      'projectExperience'
    ]

    const isComplete = requiredKeys.every((k) => {
      const v = p?.[k]
      if (v === null || v === undefined) {
        return false
      }
      if (typeof v === 'string' && v.trim().length === 0) {
        return false
      }
      if (typeof v === 'number' && v === 0) {
        return false
      }
      return true
    })
    
    return isComplete
  }

  const allowBrowseRoutes = new Set([
    'Project',
    'ProjectDetail',
    'CompetitionList',
    'CompetitionDetail',
    'Profile',
    'ProfileEdit',
    'SkillManagement',
    'ProjectHistory',
    'NeedConfirm',
    'AccountSettings',
    'MentorSystemApplications',
    'MentorSystemManage',
    'MentorPlaza',
    'TestAuth'
  ])

  // 未完善资料：仅允许浏览类页面，其它功能一律引导去“我的”完善
  if (!isProfileComplete() && !allowBrowseRoutes.has(String(to.name || ''))) {

    ElMessage.warning('请先在“我的”页面完善个人资料后再使用该功能')
    next({ name: 'Profile' })
    return
  }
  
  // 检查团队成员权限
  if (to.meta.requiresTeamMember) {
    const teamId = Number(to.params.id)
    
    if (!teamId || isNaN(teamId)) {
      ElMessage.error('无效的团队 ID')
      next({ name: 'TeamList' })
      return
    }

    const teamStore = useTeamStore()
    
    // 如果 store 中已经有该团队的数据，直接放行（避免重复加载）
    if (teamStore.currentTeam?.id === teamId) {
      next()
      return
    }
    
    // 如果是从团队列表跳转过来，且团队在列表中，说明用户是成员，直接放行
    // 让组件自己加载详情，避免路由守卫阻塞
    const teamInList = teamStore.teams.find(t => t.id === teamId)
    if (teamInList && from.name === 'TeamList') {
      next()
      return
    }
    
    // 其他情况，异步验证但不阻塞导航
    teamStore.fetchTeamDetail(teamId).catch((error: any) => {
      if (error.response?.status === 403) {
        ElMessage.error('您不是该团队成员，无权访问')
        router.replace({ name: 'TeamList' })
      } else if (error.response?.status === 404) {
        ElMessage.error('团队不存在或已被删除')
        router.replace({ name: 'TeamList' })
      }
    })
    
    next()
    return
  }
  
  next()
})

export default router
