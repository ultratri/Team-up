<template>
  <div class="team-overview" role="main" aria-labelledby="team-overview-title">
    <h1 id="team-overview-title" class="sr-only">团队概览</h1>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container" role="status" aria-live="polite" aria-label="正在加载团队概览">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container" role="alert" aria-live="assertive">
      <el-result
        icon="error"
        title="加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="loadOverviewData" aria-label="重新加载团队概览">重试</el-button>
        </template>
      </el-result>
    </div>

    <!-- 主内容 -->
    <div v-else class="overview-content">
      <!-- 团队基本信息 -->
      <div class="info-card glass-card" role="region" aria-labelledby="team-info-title">
        <h2 id="team-info-title" class="sr-only">团队基本信息</h2>
        <div class="team-header">
          <div
            class="team-avatar"
            role="img"
            :aria-label="`${teamInfo.name} 团队头像`"
            v-loading="avatarUploading"
          >
            <el-image 
              :src="teamInfo.avatar || '/default-avatar.png'" 
              :alt="`${teamInfo.name} 团队头像`"
              lazy
              fit="cover"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <el-upload
              v-if="isMember"
              class="avatar-uploader"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarChange"
              :disabled="avatarUploading"
              accept="image/*"
            >
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </el-upload>
          </div>
          <div class="team-meta">
            <h1>{{ teamInfo.name }}</h1>
            <div class="description-section">
              <p v-if="!editingDescription" class="description" :aria-label="`团队描述: ${teamInfo.description || '暂无描述'}`">
                {{ teamInfo.description || '暂无描述' }}
              </p>
              <el-input
                v-else
                v-model="descriptionEdit"
                type="textarea"
                :rows="3"
                placeholder="请输入团队描述"
                maxlength="200"
                show-word-limit
              />
              <div v-if="isLeader" class="description-actions">
                <el-button
                  v-if="!editingDescription"
                  type="text"
                  :icon="Edit"
                  size="small"
                  @click="startEditDescription"
                >
                  编辑描述
                </el-button>
                <template v-else>
                  <el-button
                    type="primary"
                    size="small"
                    @click="saveDescription"
                    :loading="savingDescription"
                  >
                    保存
                  </el-button>
                  <el-button
                    size="small"
                    @click="cancelEditDescription"
                  >
                    取消
                  </el-button>
                </template>
              </div>
            </div>
            <div class="stats-row" role="group" aria-label="团队基本统计">
              <div class="stat-item">
                <span class="label">创建时间</span>
                <span class="value" :aria-label="`创建时间: ${formatDate(teamInfo.createdAt)}`">
                  {{ formatDate(teamInfo.createdAt) }}
                </span>
              </div>
              <div class="stat-item">
                <span class="label">成员数量</span>
                <span class="value" :aria-label="`成员数量: ${members.length} 人`">
                  {{ members.length }} 人
                  <span v-if="teamInfo.maxMembers" class="max-members">
                    / {{ teamInfo.maxMembers }}
                  </span>
                </span>
              </div>
              <div class="stat-item">
                <span class="label">团队状态</span>
                <span class="value" :aria-label="`团队状态: ${teamInfo.status === 'ACTIVE' ? '活跃' : '归档'}`">
                  {{ teamInfo.status === 'ACTIVE' ? '活跃' : '归档' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Mentor Card (for competition teams) -->
      <div
        v-if="teamInfo.type === 'COMPETITION'"
        class="mentor-card glass-card"
        role="region"
        aria-labelledby="mentor-title"
      >
        <div class="mentor-header">
          <h2 id="mentor-title">
            <el-icon><Avatar /></el-icon>
            指导老师
          </h2>
          <el-button
            v-if="!teamInfo.mentorId && isLeader"
            type="primary"
            size="small"
            @click="showMentorApplicationDialog = true"
          >
            申请导师
          </el-button>
        </div>
        <div v-if="teamInfo.mentor" class="mentor-info">
          <el-avatar :src="teamInfo.mentor.avatar" :size="60">
            {{ teamInfo.mentor.name?.charAt(0) || '?' }}
          </el-avatar>
          <div class="mentor-details">
            <h3>{{ teamInfo.mentor.name }}</h3>
            <p>指导老师</p>
          </div>
        </div>
        <div v-else class="mentor-empty">
          <el-empty description="暂无指导老师" :image-size="80" />
          <p v-if="isLeader" class="hint">点击上方按钮申请指导老师</p>
        </div>
      </div>

      <!-- Competition Info -->
      <div v-if="teamInfo.type === 'COMPETITION' && teamInfo.competition" class="competition-info-card glass-card">
        <div class="competition-badge">
          <el-icon><Trophy /></el-icon>
          <span>所属比赛：</span>
          <el-link
            type="primary"
            :href="`/competition/${teamInfo.competitionId}`"
            @click.prevent="handleViewCompetition"
          >
            {{ teamInfo.competition.name }}
          </el-link>
        </div>
      </div>

      <!-- Join Applications (leader) -->
      <div v-if="isLeader" class="join-applications glass-card">
        <div class="join-header">
          <h2>加入申请</h2>
          <el-select v-model="joinStatusFilter" placeholder="状态" size="small" style="width: 140px" @change="loadJoinApplications">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </div>
        <el-skeleton v-if="joinLoading" :rows="3" animated />
        <div v-else-if="joinApplications.length === 0" class="empty-join">
          <el-empty description="暂无加入申请" :image-size="80" />
        </div>
        <div v-else class="join-list">
          <el-card v-for="app in joinApplications" :key="app.id" class="join-item" shadow="never">
            <div class="join-item__main">
              <div class="join-item__meta">
                <el-tag :type="joinStatusTagType(app.status)">{{ joinStatusText(app.status) }}</el-tag>
                <span class="time">{{ formatTime(app.appliedAt) }}</span>
              </div>
              <div class="join-item__reason" v-if="app.reason">{{ app.reason }}</div>
              <div class="join-item__reason" v-else>（无理由）</div>
            </div>
            <div class="join-item__actions" v-if="app.status === 'PENDING'">
              <el-button type="success" size="small" @click="handleApproveJoin(app.id)">通过</el-button>
              <el-button type="danger" size="small" @click="handleRejectJoin(app.id)">拒绝</el-button>
            </div>
          </el-card>
        </div>
      </div>

      <!-- Members Wall -->
      <div class="members-wall glass-card" role="region" aria-labelledby="members-title">
        <h2 id="members-title">团队成员</h2>
        <div class="members-grid" role="list" aria-label="团队成员列表">
          <div 
            v-for="member in members" 
            :key="member.userId"
            class="member-avatar"
            role="listitem"
            tabindex="0"
            :aria-label="`成员: ${member.username}, 角色: ${getRoleLabel(member.role)}`"
            @mouseenter="showMemberTooltip(member, $event)"
            @mouseleave="hideMemberTooltip"
            @keydown.enter="showMemberTooltip(member, $event)"
            @keydown.space.prevent="showMemberTooltip(member, $event)"
          >
            <el-image 
              :src="member.avatar || '/default-avatar.png'" 
              :alt="`${member.username} 的头像`"
              lazy
              fit="cover"
            >
              <template #error>
                <div class="image-error">{{ member.username?.charAt(0) || '?' }}</div>
              </template>
            </el-image>
            <span class="role-badge" :class="member.role" :aria-label="`角色: ${getRoleLabel(member.role)}`">
              {{ getRoleLabel(member.role) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Stats Grid -->
      <div class="stats-grid" role="region" aria-labelledby="stats-title">
        <h2 id="stats-title" class="sr-only">团队统计数据</h2>
        <div class="stat-card glass-card" role="article" :aria-label="`任务完成率: ${statistics.taskCompletionRate}%`">
          <div class="stat-icon" style="background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark))" role="img" aria-label="任务完成率图标">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <h3 aria-live="polite">{{ statistics.taskCompletionRate }}%</h3>
            <p>任务完成率</p>
          </div>
        </div>

        <div class="stat-card glass-card" role="article" :aria-label="`活跃天数: ${statistics.activeDays} 天`">
          <div class="stat-icon" style="background: linear-gradient(135deg, var(--text-color), var(--text-color-muted))" role="img" aria-label="活跃天数图标">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stat-content">
            <h3>{{ statistics.activeDays }}</h3>
            <p>活跃天数</p>
          </div>
        </div>

        <div class="stat-card glass-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark))">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-content">
            <h3>{{ statistics.messageCount }}</h3>
            <p>消息数量</p>
          </div>
        </div>

        <div class="stat-card glass-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, var(--el-color-success), var(--el-color-success-light-3))">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <h3>{{ statistics.fileCount }}</h3>
            <p>共享文件</p>
          </div>
        </div>
      </div>

      <!-- Activity Timeline -->
      <div class="activity-timeline glass-card">
        <h2>最近活动</h2>
        <div v-if="activities.length === 0" class="empty-activities">
          <el-empty description="暂无活动记录" />
        </div>
        <el-timeline v-else>
          <el-timeline-item
            v-for="activity in activities"
            :key="activity.id"
            :timestamp="formatTime(activity.createdAt)"
            :color="getActivityColor(activity.activityType || activity.type)"
          >
            <div class="activity-content">
              <strong>{{ activity.username }}</strong>
              <span>{{ activity.detail }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- Mentor Application Dialog -->
      <el-dialog
        v-model="showMentorApplicationDialog"
        title="申请指导老师"
        width="600px"
        @close="resetMentorApplicationForm"
      >
        <el-form
          :model="mentorApplicationForm"
          :rules="mentorApplicationRules"
          ref="mentorApplicationFormRef"
          label-width="100px"
        >
          <el-form-item label="选择导师" prop="mentorId">
            <el-select
              v-model="mentorApplicationForm.mentorId"
              filterable
              remote
              :remote-method="searchMentors"
              :loading="searchingMentors"
              placeholder="搜索导师（输入用户名、学号或邮箱）"
              style="width: 100%"
              @change="handleMentorSelect"
            >
              <el-option
                v-for="user in mentorSearchResults"
                :key="user.id"
                :label="`${user.username} (${user.userCode})`"
                :value="user.id"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span>{{ user.username }}</span>
                  <span style="font-size: 12px; color: var(--text-color-muted);">
                    {{ user.userCode }}
                  </span>
                  <span v-if="user.email" style="font-size: 12px; color: var(--text-color-muted);">
                    · {{ user.email }}
                  </span>
                </div>
              </el-option>
            </el-select>
            <div class="form-tip">输入关键词搜索导师，支持用户名、学号、邮箱</div>
          </el-form-item>
          <el-form-item label="申请理由" prop="reason">
            <el-input
              v-model="mentorApplicationForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请说明申请该导师的理由（可选）"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showMentorApplicationDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitMentorApplication" :loading="submittingMentorApplication">
            提交申请
          </el-button>
        </template>
      </el-dialog>

      <!-- Member Tooltip -->
      <div 
        v-if="tooltipMember" 
        class="member-tooltip"
        :style="{ left: tooltipPosition.x + 'px', top: tooltipPosition.y + 'px' }"
      >
        <div class="tooltip-header">
          <el-image 
            :src="tooltipMember.avatar || '/default-avatar.png'" 
            alt=""
            lazy
            fit="cover"
            style="width: 48px; height: 48px; border-radius: 50%;"
          >
            <template #error>
              <div class="image-error">{{ tooltipMember.username?.charAt(0) || '?' }}</div>
            </template>
          </el-image>
          <div>
            <h4>{{ tooltipMember.username }}</h4>
            <span class="role">{{ getRoleLabel(tooltipMember.role) }}</span>
          </div>
        </div>
        <div class="tooltip-skills">
          <el-tag v-for="skill in tooltipMember.skills" :key="skill" size="small">
            {{ skill }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  TrendCharts,
  Calendar,
  ChatDotRound,
  Document,
  Picture,
  Camera,
  Trophy,
  Edit,
  Avatar
} from '@element-plus/icons-vue'
import { getTeam, getTeamMembers, getTeamStatistics, getTeamActivities, uploadTeamAvatar } from '@api/team'
import { getTeamJoinApplications, reviewTeamJoinApplication } from '@api/team'
import { createMentorApplication } from '@api/competition'
import { getCompetitionDetail } from '@api/competition'
import { searchUsers } from '@api/user'
import { request } from '@utils/request'
import { useAuthStore } from '@store/auth'
import type { Team, TeamMember, TeamActivity } from '@types/team'
import type { TeamJoinApplication } from '@types/team'
import type { Competition } from '@types/competition'
import type { User } from '@api/user'

const props = defineProps<{
  teamId?: number
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 从 props 或路由参数获取 teamId
const teamId = computed(() => {
  return props.teamId || Number(route.params.id)
})

const loading = ref(true)
const error = ref<string | null>(null)
const teamInfo = ref<Partial<Team>>({})
const members = ref<TeamMember[]>([])
const avatarUploading = ref(false)
const editingDescription = ref(false)
const descriptionEdit = ref('')
const savingDescription = ref(false)
const statistics = reactive({
  taskCompletionRate: 0,
  activeDays: 0,
  messageCount: 0,
  fileCount: 0
})
const activities = ref<TeamActivity[]>([])

const isMember = computed(() => {
  const userId = authStore.user?.id
  if (!userId) return false
  return members.value.some(member => member.userId === userId)
})

const isLeader = computed(() => {
  const userId = authStore.user?.id
  if (!userId) return false
  const member = members.value.find(m => m.userId === userId)
  return member?.role === 'LEADER' || member?.role === 'OWNER' || member?.role === 'ADMIN'
})

// 入队申请（队长）
const joinLoading = ref(false)
const joinApplications = ref<TeamJoinApplication[]>([])
const joinStatusFilter = ref('PENDING')

const loadJoinApplications = async () => {
  if (!isLeader.value) return
  if (!teamId.value || isNaN(teamId.value)) return
  joinLoading.value = true
  try {
    const res: any = await getTeamJoinApplications(teamId.value, { page: 1, size: 20, status: joinStatusFilter.value })
    const pageData = res?.data || res
    joinApplications.value = pageData?.records || []
  } catch (e: any) {
    console.warn('Failed to load join applications:', e)
    // 如果是 401 错误，可能是权限问题，不影响页面其他功能
    if (e.status === 401) {
      console.warn('无权查看入队申请列表，可能不是队长')
    }
    joinApplications.value = []
  } finally {
    joinLoading.value = false
  }
}

const handleApproveJoin = async (applicationId: number) => {
  try {
    await reviewTeamJoinApplication(applicationId, true)
    ElMessage.success('已通过')
    loadJoinApplications()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const handleRejectJoin = async (applicationId: number) => {
  try {
    await reviewTeamJoinApplication(applicationId, false)
    ElMessage.success('已拒绝')
    loadJoinApplications()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const joinStatusText = (status: string) =>
  ({ PENDING: '待处理', APPROVED: '已通过', REJECTED: '已拒绝', WITHDRAWN: '已撤回' } as any)[status] || status

const joinStatusTagType = (status: string): 'warning' | 'success' | 'danger' | 'info' =>
  ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', WITHDRAWN: 'info' } as any)[status] || 'info'

// 导师申请相关
const showMentorApplicationDialog = ref(false)
const submittingMentorApplication = ref(false)
const searchingMentors = ref(false)
const mentorApplicationFormRef = ref<FormInstance>()
const mentorApplicationForm = reactive({
  mentorId: undefined as number | undefined,
  reason: ''
})
const mentorSearchResults = ref<User[]>([])

const mentorApplicationRules: FormRules = {
  mentorId: [
    { required: true, message: '请选择导师', trigger: 'change' }
  ]
}

const searchMentors = async (keyword: string) => {
  if (!keyword || keyword.trim().length < 2) {
    mentorSearchResults.value = []
    return
  }
  searchingMentors.value = true
  try {
    const users = await searchUsers(keyword.trim(), 10)
    mentorSearchResults.value = users
  } catch (error: any) {
    console.error('Failed to search mentors:', error)
    ElMessage.error('搜索导师失败')
    mentorSearchResults.value = []
  } finally {
    searchingMentors.value = false
  }
}

const handleMentorSelect = (userId: number) => {
  // 可以在这里做一些额外的处理
}

const handleAvatarChange = async (file: any) => {
  if (!file?.raw) return
  if (!teamId.value || isNaN(teamId.value)) {
    ElMessage.error('无效的团队 ID')
    return
  }

  const raw = file.raw as File
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  const maxSize = 2
  if (raw.size / 1024 / 1024 > maxSize) {
    ElMessage.error(`图片大小不能超过 ${maxSize}MB`)
    return
  }

  avatarUploading.value = true
  try {
    const res = await uploadTeamAvatar(teamId.value, raw)
    if (res?.url) {
      teamInfo.value.avatar = res.url
    }
    ElMessage.success('团队头像已更新')
  } catch (error: any) {
    console.error('Failed to upload team avatar:', error)
    ElMessage.error(error?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const tooltipMember = ref<TeamMember | null>(null)
const tooltipPosition = reactive({ x: 0, y: 0 })

const handleViewCompetition = () => {
  if (teamInfo.value.competitionId) {
    router.push({ name: 'CompetitionDetail', params: { id: teamInfo.value.competitionId } })
  }
}

const handleSubmitMentorApplication = async () => {
  if (!mentorApplicationFormRef.value) return
  await mentorApplicationFormRef.value.validate(async (valid) => {
    if (!valid) return
    if (!mentorApplicationForm.mentorId) {
      ElMessage.error('请输入导师ID')
      return
    }
    submittingMentorApplication.value = true
    try {
      await createMentorApplication(teamId.value, {
        mentorId: mentorApplicationForm.mentorId,
        reason: mentorApplicationForm.reason || undefined
      })
      ElMessage.success('导师申请已提交，请等待老师审核')
      showMentorApplicationDialog.value = false
      resetMentorApplicationForm()
    } catch (error: any) {
      ElMessage.error(error.message || '提交申请失败')
    } finally {
      submittingMentorApplication.value = false
    }
  })
}

const resetMentorApplicationForm = () => {
  mentorApplicationForm.mentorId = undefined
  mentorApplicationForm.reason = ''
  mentorApplicationFormRef.value?.clearValidate()
}

const loadOverviewData = async () => {
  loading.value = true
  error.value = null
  
  if (!teamId.value || isNaN(teamId.value)) {
    error.value = '无效的团队 ID'
    loading.value = false
    return
  }
  
  try {
    // 使用 Promise.all 并行请求
    const [teamRes, membersRes, statisticsRes, activitiesRes] = await Promise.all([
      getTeam(teamId.value),
      getTeamMembers(teamId.value),
      getTeamStatistics(teamId.value),
      getTeamActivities(teamId.value, 10)
    ])
    
    teamInfo.value = teamRes
    members.value = membersRes

    // 队长加载入队申请
    loadJoinApplications().catch(() => {})
    
    // 如果是比赛队伍，加载比赛信息
    if (teamRes.competitionId) {
      try {
        const competition = await getCompetitionDetail(teamRes.competitionId)
        if (competition) {
          teamInfo.value.competition = competition
        }
      } catch (err) {
        console.warn('Failed to load competition info:', err)
      }
    }
    
    // 更新统计数据（使用真实 API 数据）
    if (statisticsRes) {
      statistics.taskCompletionRate = statisticsRes.taskCompletionRate || 0
      statistics.activeDays = statisticsRes.activeDays || 0
      statistics.messageCount = statisticsRes.messageCount || 0
      statistics.fileCount = statisticsRes.fileCount || 0
    }
    
    // 更新活动记录（使用真实 API 数据）
    activities.value = activitiesRes || []
    
  } catch (err: any) {
    console.error('Failed to load team data:', err)
    error.value = err.message || '加载团队数据失败'
  } finally {
    loading.value = false
  }
}

const formatDate = (date: any) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatTime = (date: any) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 编辑描述相关方法
const startEditDescription = () => {
  descriptionEdit.value = teamInfo.value.description || ''
  editingDescription.value = true
}

const cancelEditDescription = () => {
  editingDescription.value = false
  descriptionEdit.value = ''
}

const saveDescription = async () => {
  if (!teamId.value || isNaN(teamId.value)) {
    ElMessage.error('无效的团队 ID')
    return
  }

  savingDescription.value = true
  try {
    const response = await request.put(`/teams/${teamId.value}`, {
      description: descriptionEdit.value
    })
    
    // 处理响应
    const updatedTeam = (response && typeof response === 'object' && 'data' in response) 
      ? (response as any).data 
      : response
    
    teamInfo.value.description = updatedTeam.description || descriptionEdit.value
    editingDescription.value = false
    ElMessage.success('描述已更新')
  } catch (error: any) {
    console.error('Failed to update description:', error)
    ElMessage.error(error?.message || '更新描述失败')
  } finally {
    savingDescription.value = false
  }
}

const getRoleLabel = (role: string) => {
  const roleMap: Record<string, string> = {
    OWNER: '创建者',
    ADMIN: '管理员',
    MEMBER: '成员',
    LEADER: '队长'
  }
  return roleMap[role] || role
}

const getActivityColor = (type: string) => {
  const colorMap: Record<string, string> = {
    task: '#67c23a',
    file: '#409eff',
    message: '#e6a23c',
    member: '#f56c6c',
    setting: '#909399'
  }
  return colorMap[type] || '#909399'
}

const showMemberTooltip = (member: TeamMember, event: Event) => {
  tooltipMember.value = member
  if (event instanceof MouseEvent) {
    tooltipPosition.x = event.clientX + 10
    tooltipPosition.y = event.clientY + 10
  } else if (event.target instanceof HTMLElement) {
    const rect = event.target.getBoundingClientRect()
    tooltipPosition.x = rect.left + 10
    tooltipPosition.y = rect.bottom + 10
  }
}

const hideMemberTooltip = () => {
  tooltipMember.value = null
}

onMounted(() => {
  loadOverviewData()
})
</script>

<style scoped lang="scss">
.team-overview {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
  animation: fadeInUp 0.5s var(--ease-out);
}

.glass-card {
  background: var(--bg-card);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-card);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-lg);
  border: 1px solid var(--border-card);
  transition: all var(--transition-base);
  
  &:hover {
    box-shadow: var(--shadow-card-hover);
  }
}

.info-card {
  .team-header {
    display: flex;
    gap: var(--spacing-lg);
    align-items: flex-start;
  }

  .team-avatar {
    width: 120px;
    height: 120px;
    border-radius: 20px;
    overflow: hidden;
    flex-shrink: 0;
    position: relative;
    box-shadow: var(--shadow-card);
    transition: transform var(--transition-smooth);

    &:hover {
      transform: scale(1.05);
    }

    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .image-error {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-size: 48px;
    }

    .avatar-uploader {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      :deep(.el-upload) {
        width: 100%;
        height: 100%;
      }
    }

    .avatar-overlay {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 6px;
      background: rgba(0, 0, 0, 0.45);
      color: #fff;
      font-size: 12px;
      opacity: 0;
      transition: opacity 0.2s ease;
    }

    &:hover .avatar-overlay {
      opacity: 1;
    }
  }

  .team-meta {
    flex: 1;
    min-width: 0;

    h1 {
      margin: 0 0 12px 0;
      font-size: 32px;
      font-weight: 700;
      background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .description-section {
      margin-bottom: var(--spacing-lg);
      
      .description {
        color: var(--text-color-muted);
        line-height: 1.6;
        margin: 0 0 8px 0;
      }
      
      .description-actions {
        display: flex;
        gap: 8px;
        margin-top: 8px;
      }
      
      :deep(.el-textarea) {
        margin-bottom: 8px;
      }
    }

    .stats-row {
      display: flex;
      gap: var(--spacing-xl);
      flex-wrap: wrap;

      .stat-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .label {
          font-size: 13px;
          color: var(--text-color-secondary);
        }

        .value {
          font-size: 16px;
          font-weight: 600;
          color: var(--text-color);

          .max-members {
            font-size: 14px;
            color: var(--text-color-muted);
            font-weight: normal;
          }
        }
      }
    }
  }
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-color-muted);
}

.competition-info-card {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);

  .competition-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-color);

    .el-icon {
      font-size: 18px;
      color: var(--el-color-warning);
    }
  }
}

.mentor-card {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);

  .mentor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--spacing-md);

    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color);
      margin: 0;

      .el-icon {
        font-size: 20px;
        color: var(--el-color-primary);
      }
    }
  }

  .mentor-info {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: var(--spacing-md);
    background: var(--bg-elevated);
    border-radius: var(--radius-medium);
    border: 1px solid var(--border-subtle);

    .mentor-details {
      h3 {
        margin: 0 0 4px 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color);
      }

      p {
        margin: 0;
        font-size: 13px;
        color: var(--text-color-muted);
      }
    }
  }

  .mentor-empty {
    padding: var(--spacing-lg) 0;

    .hint {
      margin-top: 12px;
      text-align: center;
      font-size: 13px;
      color: var(--text-color-muted);
    }
  }
}

.join-applications {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);

  .join-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--spacing-md);

    h2 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color);
    }
  }

  .join-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .join-item {
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
  }

  .join-item__meta {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-bottom: 8px;

    .time {
      font-size: 12px;
      color: var(--text-color-muted);
    }
  }

  .join-item__reason {
    color: var(--text-color-secondary);
    font-size: 13px;
    line-height: 1.6;
  }

  .join-item__actions {
    margin-top: 10px;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

.members-wall {
  h2 {
    margin: 0 0 var(--spacing-lg) 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--text-color);
  }

  .members-grid {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-md);
  }

  .member-avatar {
    position: relative;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    transition: transform var(--transition-smooth);
    box-shadow: var(--shadow-card);

    &:hover {
      transform: scale(1.15);
      z-index: 10;
    }

    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .image-error {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-size: 24px;
      font-weight: 600;
    }

    .role-badge {
      position: absolute;
      bottom: -4px;
      left: 50%;
      transform: translateX(-50%);
      background: var(--accent-color);
      color: white;
      font-size: 10px;
      padding: 2px 8px;
      border-radius: 10px;
      white-space: nowrap;
      font-weight: 600;

      &.OWNER, &.LEADER {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }

      &.ADMIN {
        background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
      }
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);

  .stat-card {
    display: flex;
    align-items: center;
    gap: var(--spacing-lg);
    padding: var(--spacing-lg);
    transition: transform var(--transition-base);

    &:hover {
      transform: translateY(-2px);
    }

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: var(--radius-card);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 28px;
      flex-shrink: 0;
    }

    .stat-content {
      flex: 1;
      min-width: 0;
      
      h3 {
        margin: 0 0 4px 0;
        font-size: 28px;
        font-weight: 700;
        color: var(--text-color);
      }

      p {
        margin: 0;
        font-size: 14px;
        color: var(--text-color-secondary);
      }
    }
  }
}

.activity-timeline {
  h2 {
    margin: 0 0 var(--spacing-lg) 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--text-color);
  }

  .empty-activities {
    padding: var(--spacing-xl) 0;
    text-align: center;
  }

  .activity-content {
    display: flex;
    gap: 6px;
    align-items: center;
    flex-wrap: wrap;

    strong {
      color: var(--text-color);
    }

    .activity-detail {
      color: var(--text-color-muted);
    }
  }
}

.member-tooltip {
  position: fixed;
  z-index: 9999;
  background: var(--bg-card);
  border-radius: 12px;
  padding: var(--spacing-md);
  box-shadow: var(--shadow-card-hover);
  min-width: 200px;
  pointer-events: none;
  border: 1px solid var(--border-card);

  .tooltip-header {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-bottom: 12px;

    :deep(.el-image) {
      flex-shrink: 0;
    }

    img {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      object-fit: cover;
    }

    .image-error {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      font-size: 20px;
      font-weight: 600;
    }

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color);
    }

    .role {
      font-size: 12px;
      color: var(--text-color-secondary);
    }
  }

  .tooltip-skills {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

// 响应式设计 - 平板
@media (max-width: 1024px) {
  .team-overview {
    padding: var(--spacing-md);
  }
  
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }
}

// 响应式设计 - 移动端
@media (max-width: 768px) {
  .team-overview {
    padding: var(--spacing-sm);
  }
  
  .glass-card {
    padding: var(--spacing-md);
    border-radius: var(--radius-small);
  }
  
  .info-card {
    .team-header {
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: var(--spacing-md);
    }
    
    .team-avatar {
      width: 100px;
      height: 100px;
    }
    
    .team-meta {
      h1 {
        font-size: 24px;
      }
      
      .stats-row {
        justify-content: center;
        gap: var(--spacing-lg);
      }
    }
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-md);
    
    .stat-card {
      padding: var(--spacing-md);
      
      .stat-icon {
        width: 50px;
        height: 50px;
        font-size: 24px;
      }
      
      .stat-content {
        h3 {
          font-size: 24px;
        }
      }
    }
  }
  
  .members-wall {
    .members-grid {
      justify-content: center;
    }
    
    .member-avatar {
      width: 56px;
      height: 56px;
    }
  }
}

// 响应式设计 - 小屏幕
@media (max-width: 480px) {
  .info-card {
    .team-avatar {
      width: 80px;
      height: 80px;
    }
    
    .team-meta {
      h1 {
        font-size: 20px;
      }
      
      .stats-row {
        gap: var(--spacing-md);
        
        .stat-item {
          .value {
            font-size: 14px;
          }
          
          .label {
            font-size: 12px;
          }
        }
      }
    }
  }
  
  .stats-grid {
    .stat-card {
      .stat-icon {
        width: 40px;
        height: 40px;
        font-size: 20px;
      }
      
      .stat-content {
        h3 {
          font-size: 20px;
        }
        
        p {
          font-size: 12px;
        }
      }
    }
  }
  
  .members-wall {
    .member-avatar {
      width: 48px;
      height: 48px;
      
      .role-badge {
        font-size: 8px;
        padding: 1px 6px;
      }
    }
  }
}

// 触摸设备优化
@media (hover: none) and (pointer: coarse) {
  .member-avatar {
    &:hover {
      transform: none;
    }
    
    &:active {
      transform: scale(0.95);
    }
  }
  
  .stats-grid .stat-card {
    &:hover {
      transform: none;
    }
  }
}

// 动画
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Screen reader only class
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
</style>
