<template>
  <div class="competition-detail">
    <el-page-header @back="handleBack" title="返回">
      <template #content>
        <span class="page-title">比赛详情</span>
      </template>
    </el-page-header>

    <el-card class="detail-card" v-loading="loading" shadow="never">
      <!-- 骨架屏 -->
      <el-skeleton v-if="loading" :rows="8" animated />
      
      <!-- 比赛头部信息 -->
      <div v-else-if="competition" class="competition-header">
        <div>
          <h1>{{ competition.name }}</h1>
          <div class="tags">
            <el-tag :type="getStatusTagType(competition.status)">
              {{ getStatusText(competition.status) }}
            </el-tag>
            <el-tag type="info">{{ getLevelText(competition.level) }}</el-tag>
            <el-tag v-if="competition.type">{{ getTypeText(competition.type) }}</el-tag>
          </div>
        </div>
        <el-button
          v-if="canCreateTeam"
          type="primary"
          size="large"
          @click="showCreateTeamDialog = true"
        >
          创建队伍
        </el-button>
      </div>

      <el-divider />

      <!-- 比赛基本信息 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="主办方">
          {{ competition?.organizer }}
        </el-descriptions-item>
        <el-descriptions-item label="级别">
          {{ getLevelText(competition?.level) }}
        </el-descriptions-item>
        <el-descriptions-item label="范围">
          {{ getScopeText(competition?.scope) }}
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ getTypeText(competition?.type) }}
        </el-descriptions-item>
        <el-descriptions-item label="报名开始">
          {{ formatDateTime(competition?.signupStartAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="报名截止">
          {{ formatDateTime(competition?.signupEndAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="比赛开始">
          {{ formatDateTime(competition?.startAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="比赛结束">
          {{ formatDateTime(competition?.endAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="队伍人数">
          {{ competition?.minTeamMembers || '-' }} - {{ competition?.maxTeamMembers || '-' }} 人
        </el-descriptions-item>
        <el-descriptions-item label="是否需要导师">
          {{ competition?.requireMentor ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="个人队伍上限" v-if="competition?.maxTeamsPerUser !== null && competition?.maxTeamsPerUser !== undefined">
          每人最多可参加 {{ competition.maxTeamsPerUser }} 支队伍
        </el-descriptions-item>
        <el-descriptions-item label="个人队伍上限" v-else>
          不限制每人可参加的队伍数量
        </el-descriptions-item>
        <el-descriptions-item label="报名资格">
          <span v-if="competition?.eligibilityEnabled">
            本比赛仅面向配置的学院 / 专业 / 年级开放报名，不符合条件的同学无法创建队伍或申请加入
          </span>
          <span v-else>
            本比赛对报名对象无硬性限制，具体资格以实际通知为准
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="队伍数量" v-if="competition?.teamCount !== undefined">
          {{ competition.teamCount }} 支
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(competition?.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <!-- 比赛描述 -->
      <div class="competition-content" v-if="competition?.description">
        <h3>比赛描述</h3>
        <div class="content-text" v-html="competition.description"></div>
      </div>

      <!-- 附件 -->
      <div v-if="attachments && attachments.length > 0" class="competition-attachments">
        <h3>附件</h3>
        <div class="attachment-list">
          <div
            v-for="(att, idx) in attachments"
            :key="idx"
            class="attachment-item"
          >
            <el-icon><Document /></el-icon>
            <span class="attachment-name">{{ att.name }}</span>
            <div class="attachment-actions">
              <el-button
                link
                type="primary"
                size="small"
                @click="handleDownloadAttachment(att)"
              >
                下载
              </el-button>
              <el-button
                v-if="canPreview(att.name)"
                link
                type="primary"
                size="small"
                @click="handlePreviewAttachment(att)"
              >
                预览
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-divider />

      <!-- 排行榜 -->
      <div class="leaderboard-section">
        <div class="leaderboard-header">
          <h3>队伍排行榜</h3>
          <span class="hint">如有评分则按评分排序，否则按任务完成度（DONE/总任务）排序</span>
        </div>
        <el-skeleton v-if="leaderboardLoading" :rows="4" animated />
        <el-table v-else :data="leaderboard" size="small" style="width: 100%">
          <el-table-column label="#" width="60" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="teamName" label="队伍" min-width="200" show-overflow-tooltip />
          <el-table-column label="评分" width="90" align="right">
            <template #default="{ row }">
              <span v-if="row.score !== null && row.score !== undefined">{{ row.score }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="评分人" width="140">
            <template #default="{ row }">
              <span v-if="row.scoredByName">{{ row.scoredByName }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="成员" width="80" align="right">
            <template #default="{ row }">{{ row.memberCount }}</template>
          </el-table-column>
          <el-table-column label="导师" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.hasMentor ? 'success' : 'info'">
                {{ row.hasMentor ? '有' : '无' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="完成率" width="120" align="right">
            <template #default="{ row }">
              <span>{{ row.completionRate }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="任务" width="140" align="right">
            <template #default="{ row }">
              {{ row.doneTasks }}/{{ row.totalTasks }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-divider />

      <!-- 队伍列表 -->
      <div class="teams-section">
        <div class="teams-header">
          <h3>参赛队伍</h3>
          <span class="count" v-if="teams.total > 0">
            共 {{ teams.total }} 支队伍
          </span>
        </div>

        <el-skeleton v-if="teamsLoading" :rows="3" animated />

        <div v-else-if="teams.records.length === 0" class="empty-teams">
          <el-empty description="暂无队伍，快来创建第一支队伍吧！" />
        </div>

        <div v-else class="teams-grid">
          <el-card
            v-for="team in teams.records"
            :key="team.id"
            class="team-card"
            shadow="hover"
          >
            <div class="team-card__header">
              <h4 class="team-card__name">{{ team.name }}</h4>
              <el-tag v-if="team.status" :type="getTeamStatusType(team.status)">
                {{ getTeamStatusText(team.status) }}
              </el-tag>
            </div>
            <p v-if="team.description" class="team-card__desc">
              {{ truncate(team.description, 100) }}
            </p>
            <div class="team-card__footer">
              <span class="member-count">
                <el-icon><User /></el-icon>
                {{ team.memberCount || 0 }} 人
              </span>
              <span v-if="team.mentorId" class="has-mentor">
                <el-icon><Avatar /></el-icon>
                已有导师
              </span>
            </div>
            <div class="team-card__actions">
              <el-button size="small" @click.stop="handleViewTeam(team.id)">查看</el-button>
              <el-button 
                v-if="!isUserInTeam(team)"
                type="primary" 
                size="small" 
                @click.stop="handleOpenJoin(team.id)"
              >
                申请加入
              </el-button>
              <el-tag v-else type="success" size="small">已加入</el-tag>
            </div>
          </el-card>
        </div>

        <!-- 分页 -->
        <div v-if="teams.total > teamsPageSize" class="teams-pagination">
          <el-pagination
            v-model:current-page="teamsPage"
            v-model:page-size="teamsPageSize"
            :total="teams.total"
            :page-sizes="[6, 12, 24]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadTeams"
            @current-change="loadTeams"
          />
        </div>
      </div>
    </el-card>
    <el-dialog
      v-model="showCreateTeamDialog"
      title="创建比赛队伍"
      width="600px"
      @close="resetCreateTeamForm"
    >
      <el-form :model="createTeamForm" :rules="createTeamRules" ref="createTeamFormRef" label-width="100px">
        <el-form-item label="队伍名称" prop="teamName">
          <el-input v-model="createTeamForm.teamName" placeholder="请输入队伍名称" />
        </el-form-item>
        <el-form-item label="队伍描述" prop="description">
          <el-input
            v-model="createTeamForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入队伍描述"
          />
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
          <el-select
            v-model="createTeamForm.projectId"
            placeholder="选择关联项目（可选）"
            clearable
            filterable
          >
            <el-option
              v-for="project in userProjects"
              :key="project.id"
              :label="project.title"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="competition?.maxTeamMembers"
          label="最大人数"
        >
          <el-input-number
            v-model="createTeamForm.maxMembers"
            :min="competition?.minTeamMembers || 1"
            :max="competition?.maxTeamMembers"
            :disabled="true"
          />
          <span class="form-tip">（比赛限制：{{ competition?.minTeamMembers || 1 }} - {{ competition?.maxTeamMembers }} 人）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateTeamDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTeam" :loading="creatingTeam">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 申请加入对话框 -->
    <el-dialog v-model="showJoinDialog" title="申请加入队伍" width="520px">
      <el-form label-width="100px">
        <el-form-item label="申请理由">
          <el-input
            v-model="joinReason"
            type="textarea"
            :rows="4"
            placeholder="简单介绍一下你能带来的贡献（可选）"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" :loading="joining" @click="handleSubmitJoin">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, User, Avatar } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCompetitionDetail, getCompetitionTeams, createCompetitionTeam, applyJoinCompetitionTeam } from '@/api/competition'
import { getCompetitionLeaderboard } from '@/api/competition'
import { getProjects } from '@/api/project'
import type { Competition, CompetitionAttachment } from '@/types/competition'
import type { Team, TeamCreateRequest } from '@/types/team'
import type { Project } from '@/types/project'
import { useAuthStore } from '@/store/auth'
import { useTeamStore } from '@/store/team'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const teamStore = useTeamStore()

const competition = ref<Competition | null>(null)
const teams = ref<{ records: Team[]; total: number }>({ records: [], total: 0 })
const teamsLoading = ref(false)
const teamsPage = ref(1)
const teamsPageSize = ref(12)
const loading = ref(false)

const showCreateTeamDialog = ref(false)
const creatingTeam = ref(false)
const createTeamFormRef = ref<FormInstance>()
const createTeamForm = reactive<TeamCreateRequest>({
  teamName: '',
  description: '',
  projectId: undefined,
  maxMembers: undefined
})

const createTeamRules: FormRules = {
  teamName: [
    { required: true, message: '请输入队伍名称', trigger: 'blur' },
    { min: 2, max: 50, message: '队伍名称长度为 2-50 个字符', trigger: 'blur' }
  ]
}

const userProjects = ref<Project[]>([])
const showJoinDialog = ref(false)
const joiningTeamId = ref<number | null>(null)
const joinReason = ref('')
const joining = ref(false)

const leaderboardLoading = ref(false)
const leaderboard = ref<any[]>([])

const competitionId = computed(() => Number(route.params.id))

const attachments = computed<CompetitionAttachment[]>(() => {
  if (!competition.value?.attachments) return []
  if (Array.isArray(competition.value.attachments)) {
    return competition.value.attachments
  }
  try {
    return JSON.parse(competition.value.attachments as string)
  } catch {
    return []
  }
})

const canCreateTeam = computed(() => {
  if (!competition.value) return false
  if (competition.value.status !== 'PUBLISHED') return false
  const now = new Date()
  const signupStart = new Date(competition.value.signupStartAt)
  const signupEnd = new Date(competition.value.signupEndAt)
  return now >= signupStart && now <= signupEnd
})

const loadCompetition = async () => {
  loading.value = true
  try {
    const data = await getCompetitionDetail(competitionId.value)
    if (data) {
      competition.value = data
      // 比赛详情加载完成后，立即加载用户项目（如果需要）
      if (authStore.isAuthenticated && canCreateTeam.value) {
        loadUserProjects()
      }
    } else {
      ElMessage.error('比赛不存在')
      router.back()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载比赛详情失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadTeams = async () => {
  teamsLoading.value = true
  try {
    const res = await getCompetitionTeams(competitionId.value, {
      page: teamsPage.value,
      size: teamsPageSize.value
    })
    teams.value = res
  } catch (error: any) {
    ElMessage.error(error.message || '加载队伍列表失败')
    console.error(error)
  } finally {
    teamsLoading.value = false
  }
}

const loadLeaderboard = async () => {
  leaderboardLoading.value = true
  try {
    leaderboard.value = await getCompetitionLeaderboard(competitionId.value, 10)
  } catch (e) {
    leaderboard.value = []
  } finally {
    leaderboardLoading.value = false
  }
}
const loadUserProjects = async () => {
  try {
    const res = await getProjects({ page: 1, size: 100 })
    userProjects.value = res.list || []
  } catch (error) {
    console.warn('加载用户项目失败:', error)
  }
}

const handleCreateTeam = async () => {
  if (!createTeamFormRef.value) return
  await createTeamFormRef.value.validate(async (valid) => {
    if (!valid) return
    creatingTeam.value = true
    try {
      await createCompetitionTeam(competitionId.value, {
        ...createTeamForm,
        maxMembers: competition.value?.maxTeamMembers
      })
      ElMessage.success('创建队伍成功')
      showCreateTeamDialog.value = false
      resetCreateTeamForm()
      loadTeams()
    } catch (error: any) {
      ElMessage.error(error.message || '创建队伍失败')
    } finally {
      creatingTeam.value = false
    }
  })
}

const resetCreateTeamForm = () => {
  createTeamForm.teamName = ''
  createTeamForm.description = ''
  createTeamForm.projectId = undefined
  createTeamForm.maxMembers = undefined
  createTeamFormRef.value?.clearValidate()
}

const handleViewTeam = (teamId: number) => {
  router.push({ name: 'TeamOverview', params: { id: teamId } })
}

const isUserInTeam = (team: Team) => {
  if (!authStore.user?.id) return false
  // 检查是否是队长
  if (team.leaderId === authStore.user.id) return true
  // 检查是否是成员 - 通过teamStore中的teams列表判断
  const userTeams = teamStore.teams
  return userTeams.some(t => t.id === team.id)
}

const handleOpenJoin = (teamId: number) => {
  // 直接检查token而不是isAuthenticated
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token || !authStore.user) {
    ElMessage.warning('请先登录后再申请加入队伍')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  joiningTeamId.value = teamId
  joinReason.value = ''
  showJoinDialog.value = true
}

const handleSubmitJoin = async () => {
  if (!joiningTeamId.value) return
  joining.value = true
  try {
    await applyJoinCompetitionTeam(competitionId.value, joiningTeamId.value, joinReason.value || undefined)
    ElMessage.success('申请已提交，请等待队长审核')
    showJoinDialog.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '申请失败')
  } finally {
    joining.value = false
  }
}

const handleBack = () => {
  // 检查是否从综合广场进入
  const from = route.query.from as string
  if (from === 'ecosystem') {
    // 返回综合广场的赛事中心标签
    router.push({ path: '/ecosystem', query: { tab: 'competitions' } })
  } else {
    // 默认返回上一页
    router.back()
  }
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

const getStatusTagType = (status: string): 'info' | 'success' | 'warning' => {
  const map: Record<string, 'info' | 'success' | 'warning'> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ARCHIVED: 'warning'
  }
  return map[status] || 'info'
}

const getLevelText = (level: string) => {
  const map: Record<string, string> = {
    SCHOOL: '校级',
    PROVINCE: '省级',
    NATIONAL: '国家级',
    INTERNATIONAL: '国际级'
  }
  return map[level] || level
}

const getScopeText = (scope: string) => {
  const map: Record<string, string> = {
    SCHOOL: '校内',
    PROVINCE: '省内',
    NATIONAL: '全国',
    INTERNATIONAL: '国际'
  }
  return map[scope] || scope
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    PROGRAMMING: '编程',
    DESIGN: '设计',
    INNOVATION: '创新',
    RESEARCH: '科研',
    OTHER: '其他'
  }
  return map[type] || type
}

const getTeamStatusText = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    ARCHIVED: '已归档',
    DISBANDED: '已解散'
  }
  return map[status] || status
}

const getTeamStatusType = (status: string): 'success' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'info' | 'danger'> = {
    ACTIVE: 'success',
    ARCHIVED: 'info',
    DISBANDED: 'danger'
  }
  return map[status] || 'info'
}

const formatDateTime = (date: string | Date | undefined) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const truncate = (text: string, maxLength: number) => {
  if (!text || text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

const canPreview = (fileName: string): boolean => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'txt', 'md'].includes(ext || '')
}

const handleDownloadAttachment = (att: CompetitionAttachment) => {
  const link = document.createElement('a')
  link.href = att.url
  link.download = att.name
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handlePreviewAttachment = async (att: CompetitionAttachment) => {
  try {
    // 如果是图片，直接打开
    const ext = att.name.split('.').pop()?.toLowerCase()
    if (['jpg', 'jpeg', 'png', 'gif'].includes(ext || '')) {
      window.open(att.url, '_blank')
      return
    }
    
    // 如果是 PDF 或其他，尝试预览
    // 这里可以扩展预览功能，目前先打开新窗口
    window.open(att.url, '_blank')
  } catch (error: any) {
    ElMessage.error('预览失败: ' + (error.message || '未知错误'))
  }
}

watch(() => route.params.id, () => {
  if (route.name === 'CompetitionDetail') {
    loadCompetition()
    loadTeams()
    loadLeaderboard()
  }
})

onMounted(async () => {
  // 优先加载比赛详情，让页面快速显示
  await loadCompetition()
  
  // 如果用户已登录，加载用户的团队列表（用于判断是否已加入队伍）
  if (authStore.user?.id) {
    teamStore.fetchUserTeams({ userId: authStore.user.id }).catch(err => {
      console.warn('加载用户团队列表失败:', err)
    })
  }
  
  // 然后并行加载其他数据
  Promise.allSettled([
    loadTeams(),
    loadLeaderboard()
  ])
})
</script>

<style scoped lang="scss">
.competition-detail {
  padding: 20px;
  background: var(--bg-body);
  min-height: 100vh;
}

@media (max-width: 768px) {
  .competition-detail {
    padding: 12px;
  }

  .competition-header {
    flex-direction: column;
    gap: 12px;

    h1 {
      font-size: 20px;
    }
  }

  .detail-card {
    margin-top: 12px;
  }

  .teams-section .teams-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .team-card__actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .competition-attachments .attachment-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .competition-attachments .attachment-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.detail-card {
  margin-top: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}

.competition-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;

  h1 {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0 0 12px 0;
  }

  .tags {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.competition-content {
  margin: 20px 0;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color);
    margin-bottom: 12px;
  }

  .content-text {
    color: var(--text-color-secondary);
    line-height: 1.8;
  }
}

.competition-attachments {
  margin: 20px 0;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color);
    margin-bottom: 12px;
  }

  .attachment-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .attachment-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: var(--bg-card);
    border: 1px solid var(--border-subtle);
    border-radius: 6px;
    transition: all 0.3s;

    &:hover {
      background: var(--bg-card-hover);
      border-color: var(--accent-color);
    }

    .attachment-name {
      flex: 1;
      color: var(--text-color);
      font-size: 14px;
    }

    .attachment-actions {
      display: flex;
      gap: 8px;
    }
  }
}

.teams-section {
  margin-top: 20px;

  .teams-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color);
      margin: 0;
    }

    .count {
      font-size: 14px;
      color: var(--text-color-muted);
    }
  }

  .empty-teams {
    padding: 40px 0;
  }

  .teams-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }

  .teams-pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}

.leaderboard-section {
  margin-top: 10px;

  .leaderboard-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 10px;

    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color);
    }

    .hint {
      font-size: 12px;
      color: var(--text-color-muted);
    }
  }
}

.team-card {
  cursor: pointer;
  transition: all 0.3s;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  &__name {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0;
    flex: 1;
  }

  &__desc {
    font-size: 13px;
    color: var(--text-color-secondary);
    line-height: 1.6;
    margin: 0 0 12px 0;
    min-height: 40px;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid var(--border-subtle);
    font-size: 12px;
    color: var(--text-color-muted);

    .member-count,
    .has-mentor {
      display: flex;
      align-items: center;
      gap: 4px;

      .el-icon {
        font-size: 12px;
      }
    }
  }

  &__actions {
    margin-top: 12px;
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-color-muted);
}
</style>
