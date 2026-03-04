<template>
  <div class="mentor-scoring">
    <div class="page-header">
      <div>
        <h1>我的指导团队 & 评分</h1>
        <p class="subtitle">查看你指导的所有团队，快速进入团队空间并对团队进行评分，或评价导师</p>
      </div>
    </div>

    <el-alert
      class="tip-alert"
      type="info"
      show-icon
      :closable="false"
      title="导师可以为自己指导的团队打分，学员可以评价导师的指导质量"
      style="margin-bottom: 12px;"
    />

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="团队类型">
          <el-select
            v-model="query.teamType"
            placeholder="全部团队"
            @change="reload"
            style="min-width: 160px"
          >
            <el-option label="全部团队" value="ALL" />
            <el-option label="比赛团队" value="COMPETITION" />
            <el-option label="普通团队" value="NORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队关键词">
          <el-input
            v-model="query.keyword"
            placeholder="团队名称"
            clearable
            @clear="reload"
            @keyup.enter="reload"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="rows"
        v-loading="loading"
        style="width: 100%"
        stripe
        :border="false"
        :row-key="row => row.id"
        empty-text="暂无相关团队"
      >
        <el-table-column label="团队" min-width="220">
          <template #default="{ row }">
            <router-link :to="{ name: 'TeamOverview', params: { id: row.id } }" class="team-link">
              {{ row.teamName }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column label="团队类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.competitionId" type="success">比赛团队</el-tag>
            <el-tag v-else type="info">普通团队</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="我的角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.myRole === 'MENTOR'" type="warning">导师</el-tag>
            <el-tag v-else type="primary">学员</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <!-- 导师可以评分 -->
            <template v-if="row.myRole === 'MENTOR'">
              <el-button type="primary" link @click="openScoreDialog(row)">评分</el-button>
            </template>
            <!-- 学员可以评价导师 -->
            <template v-else>
              <el-button type="success" link @click="openReviewDialog(row)">评价导师</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </el-card>

    <!-- 导师评价成员对话框 -->
    <el-dialog v-model="scoreDialogVisible" title="评价团队成员" width="900px">
      <div v-loading="loadingMembers">
        <div class="team-info">
          <h3>{{ currentTeam?.teamName }}</h3>
          <p class="tip">请对每位成员进行评价，评价将影响成员的信誉分</p>
        </div>
        
        <el-table :data="teamMembers" border style="margin-top: 16px">
          <el-table-column label="成员" width="150">
            <template #default="{ row }">
              <div>
                <div>{{ row.realName || row.userName }}</div>
                <el-tag size="small" :type="row.role === 'LEADER' ? 'warning' : 'info'">
                  {{ row.role === 'LEADER' ? '队长' : '成员' }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="当前信誉分" width="100">
            <template #default="{ row }">
              {{ row.creditScore }}
            </template>
          </el-table-column>
          <el-table-column label="综合评分(0-100)" width="140">
            <template #default="{ row }">
              <el-input-number 
                v-model="row.score" 
                :min="0" 
                :max="100" 
                :step="1"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="技术能力" width="120">
            <template #default="{ row }">
              <el-rate v-model="row.technicalAbility" :max="5" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="协作能力" width="120">
            <template #default="{ row }">
              <el-rate v-model="row.collaboration" :max="5" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="学习态度" width="120">
            <template #default="{ row }">
              <el-rate v-model="row.learningAttitude" :max="5" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="任务完成" width="120">
            <template #default="{ row }">
              <el-rate v-model="row.taskCompletion" :max="5" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="评语" min-width="200">
            <template #default="{ row }">
              <el-input 
                v-model="row.comment" 
                type="textarea" 
                :rows="2" 
                placeholder="可选"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" fixed="right">
            <template #default="{ row }">
              <el-tag v-if="row.evaluated" type="success" size="small">已评价</el-tag>
              <el-tag v-else type="info" size="small">未评价</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMemberEvaluations">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- 学员评价导师对话框 -->
    <el-dialog v-model="reviewDialogVisible" title="评价导师" width="600px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="团队名称">
          <span>{{ currentTeam?.teamName }}</span>
        </el-form-item>
        <el-form-item label="导师">
          <span>{{ currentTeam?.mentorName || '未知' }}</span>
        </el-form-item>
        <el-form-item label="专业能力" required>
          <el-rate v-model="reviewForm.professionalAbility" :max="5" show-text />
        </el-form-item>
        <el-form-item label="指导态度" required>
          <el-rate v-model="reviewForm.guidanceAttitude" :max="5" show-text />
        </el-form-item>
        <el-form-item label="响应速度" required>
          <el-rate v-model="reviewForm.responseSpeed" :max="5" show-text />
        </el-form-item>
        <el-form-item label="帮助程度" required>
          <el-rate v-model="reviewForm.helpfulness" :max="5" show-text />
        </el-form-item>
        <el-form-item label="文字评价">
          <el-input 
            v-model="reviewForm.comment" 
            type="textarea" 
            :rows="4" 
            placeholder="请描述导师的指导情况（可选，最多500字）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyMentorCompetitionTeams } from '@/api/competition'
import { submitMentorReview, getTeamMembersForEvaluation, batchEvaluateMembers, type TeamMemberForEvaluation, type MentorMemberEvaluationDTO } from '@/api/mentor'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  teamType: 'ALL' as 'ALL' | 'COMPETITION' | 'NORMAL'
})

// 导师评价成员对话框
const scoreDialogVisible = ref(false)
const currentTeam = ref<any>(null)
const teamMembers = ref<TeamMemberForEvaluation[]>([])
const loadingMembers = ref(false)

// 学员评价导师对话框
const reviewDialogVisible = ref(false)
const reviewForm = reactive({
  professionalAbility: 0,
  guidanceAttitude: 0,
  responseSpeed: 0,
  helpfulness: 0,
  comment: ''
})

const submitting = ref(false)

const reload = async () => {
  loading.value = true
  try {
    // 使用现有的比赛团队API
    const res = await getMyMentorCompetitionTeams({
      page: query.page,
      size: query.size,
      keyword: query.keyword
    })
    
    let list = res.records || []

    // 根据团队类型筛选
    if (query.teamType === 'COMPETITION') {
      list = list.filter((t: any) => t.competitionId)
    } else if (query.teamType === 'NORMAL') {
      list = list.filter((t: any) => !t.competitionId)
    }

    rows.value = list
    // 优先使用后端返回的总数，没有则退回当前列表长度
    total.value = typeof res.total === 'number' ? res.total : list.length
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.size = 20
  query.keyword = ''
  query.teamType = 'ALL'
  reload()
}

// 打开导师评价成员对话框
const openScoreDialog = async (row: any) => {
  currentTeam.value = row
  scoreDialogVisible.value = true
  
  // 加载团队成员
  loadingMembers.value = true
  try {
    const members = await getTeamMembersForEvaluation(row.id)
    // 初始化评分数据
    teamMembers.value = members.map(m => ({
      ...m,
      score: m.score || 0,
      technicalAbility: m.technicalAbility || 0,
      collaboration: m.collaboration || 0,
      learningAttitude: m.learningAttitude || 0,
      taskCompletion: m.taskCompletion || 0,
      comment: m.comment || ''
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '加载成员列表失败')
  } finally {
    loadingMembers.value = false
  }
}

// 提交成员评价
const submitMemberEvaluations = async () => {
  if (!currentTeam.value) return
  
  // 验证所有成员都已评分
  const unevaluated = teamMembers.value.filter(m => !m.score || m.score === 0)
  if (unevaluated.length > 0) {
    ElMessage.warning('请为所有成员打分')
    return
  }
  
  submitting.value = true
  try {
    // 构建评价数据
    const evaluations: MentorMemberEvaluationDTO[] = teamMembers.value.map(m => ({
      memberId: m.userId,
      score: m.score!,
      technicalAbility: m.technicalAbility || undefined,
      collaboration: m.collaboration || undefined,
      learningAttitude: m.learningAttitude || undefined,
      taskCompletion: m.taskCompletion || undefined,
      comment: m.comment || undefined
    }))
    
    await batchEvaluateMembers(currentTeam.value.id, evaluations)
    ElMessage.success('评价已提交')
    scoreDialogVisible.value = false
    reload()
  } catch (e: any) {
    ElMessage.error(e.message || '提交评价失败')
  } finally {
    submitting.value = false
  }
}

// 打开学员评价导师对话框
const openReviewDialog = (row: any) => {
  currentTeam.value = row
  reviewForm.professionalAbility = 0
  reviewForm.guidanceAttitude = 0
  reviewForm.responseSpeed = 0
  reviewForm.helpfulness = 0
  reviewForm.comment = ''
  reviewDialogVisible.value = true
}

// 提交学员评价
const submitReview = async () => {
  if (!currentTeam.value) return
  
  // 验证评分
  if (reviewForm.professionalAbility === 0 || 
      reviewForm.guidanceAttitude === 0 || 
      reviewForm.responseSpeed === 0 || 
      reviewForm.helpfulness === 0) {
    ElMessage.warning('请完成所有评分项')
    return
  }

  submitting.value = true
  try {
    await submitMentorReview({
      mentorId: currentTeam.value.mentorId,
      teamId: currentTeam.value.id,
      professionalAbility: reviewForm.professionalAbility,
      guidanceAttitude: reviewForm.guidanceAttitude,
      responseSpeed: reviewForm.responseSpeed,
      helpfulness: reviewForm.helpfulness,
      comment: reviewForm.comment || undefined
    })
    
    ElMessage.success('评价已提交')
    reviewDialogVisible.value = false
    reload()
  } catch (e: any) {
    ElMessage.error(e.message || '评价失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  reload()
})
</script>

<style scoped>
.mentor-scoring {
  padding: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 12px;
}
.subtitle {
  margin: 6px 0 0;
  color: var(--text-color-muted);
}
.filter-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  margin-bottom: 12px;
}
.table-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}
.pagination {
  padding: 12px 0 0;
  display: flex;
  justify-content: flex-end;
}

.team-link,
.competition-link {
  color: var(--el-color-primary);
  text-decoration: none;
}
.team-link:hover,
.competition-link:hover {
  text-decoration: underline;
}

.team-info {
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  margin-bottom: 16px;
}
.team-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}
.team-info .tip {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>

