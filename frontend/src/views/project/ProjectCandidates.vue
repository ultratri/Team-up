<template>
  <div class="project-candidates">
    <el-card>
      <template #header>
        <div class="flex-between">
          <div>
            <h3>智能推荐候选人</h3>
            <p class="subtitle">为项目"{{ projectTitle }}"推荐合适的候选人</p>
          </div>
          <el-button @click="handleRefresh" :loading="loading" type="primary">
            <el-icon><Refresh /></el-icon>
            刷新推荐
          </el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-alert
          title="匹配算法综合考虑技能匹配、协作历史、时间可用性和用户信誉等多个维度"
          type="info"
          :closable="false"
          show-icon
        />
        <div class="filter-controls">
          <span class="filter-label">匹配度：</span>
          <el-radio-group v-model="matchLevel" size="small">
            <el-radio-button label="ALL">全部</el-radio-button>
            <el-radio-button label="HIGH">高匹配（≥80）</el-radio-button>
            <el-radio-button label="MEDIUM">中匹配（60–80）</el-radio-button>
            <el-radio-button label="LOW">基础（<60）</el-radio-button>
          </el-radio-group>

          <el-switch
            v-model="onlyHighCollaboration"
            active-text="优先协作历史好"
            inline-prompt
            style="margin-left: 16px;"
          />
        </div>
      </div>

      <div class="candidates-list">
        <el-skeleton :loading="loading" animated :count="3">
          <template #template>
            <div class="candidate-card" style="border: 1px solid #ebeef5; padding: 24px; margin-bottom: 16px;">
              <div style="display: flex; gap: 24px;">
                <div style="flex-shrink: 0;">
                  <el-skeleton-item variant="circle" style="width: 80px; height: 80px" />
                </div>
                <div style="flex: 1;">
                  <el-skeleton-item variant="h3" style="width: 40%; margin-bottom: 12px" />
                  <el-skeleton-item variant="text" style="width: 60%; margin-bottom: 8px" />
                  <el-skeleton-item variant="text" style="width: 80%" />
                </div>
              </div>
            </div>
          </template>

          <template #default>
            <div v-if="filteredCandidates.length === 0" class="empty-state">
              <el-empty description="暂无推荐候选人">
                <el-button type="primary" @click="handleRefresh">刷新推荐</el-button>
              </el-empty>
            </div>

            <div v-for="item in filteredCandidates" :key="item.userId" class="candidate-card">
              <div class="card-content">
                <!-- 匹配度圆环 -->
                <div class="match-score-section">
                  <el-progress
                    type="circle"
                    :percentage="Math.round((item.score || 0) * 100)"
                    :width="80"
                    :color="getScoreColor((item.score || 0) * 100)"
                  >
                    <template #default="{ percentage }">
                      <div class="percentage-content">
                        <span class="percentage-value">{{ percentage }}</span>
                        <span class="percentage-label">匹配度</span>
                      </div>
                    </template>
                  </el-progress>
                </div>

                <!-- 候选人信息 -->
                <div class="candidate-info">
                  <div class="candidate-header">
                    <h4 class="candidate-name">{{ item.username || '未知用户' }}</h4>
                    <el-tag v-if="item.creditLevel" :type="getCreditTypeTag(item.creditLevel)" size="small">
                      {{ getCreditText(item.creditLevel) }}
                    </el-tag>
                  </div>

                  <!-- 9维度匹配详情 -->
                  <div class="match-details">
                    <div
                      v-for="dimension in getDimensionEntries(item.breakdown)"
                      :key="dimension.key"
                      class="detail-item"
                    >
                      <span class="label">{{ dimension.label }}</span>
                      <el-progress
                        :percentage="Math.round(dimension.value * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getDetailColor(dimension.value)"
                      />
                      <span class="value">{{ Math.round(dimension.value * 100) }}%</span>
                    </div>
                  </div>

                  <div class="match-summary">
                    <p class="match-reason">
                      <strong>Top优势：</strong>{{ getTopStrength(item.breakdown) }}
                    </p>
                    <p class="match-reason">
                      <strong>主要短板：</strong>{{ getMainWeakness(item.breakdown) }}
                    </p>
                    <p class="match-reason suggestion">
                      <strong>改进建议：</strong>{{ getImprovementSuggestion(item.breakdown) }}
                    </p>
                  </div>

                  <!-- 推荐理由 -->
                  <p v-if="item.matchReason" class="match-reason">
                    推荐理由：{{ item.matchReason }}
                  </p>

                  <!-- 操作按钮 -->
                  <div class="actions">
                    <el-button type="primary" size="small" @click="handleViewProfile(item.userId)">
                      查看资料
                    </el-button>
                    <el-button type="success" size="small" @click="handleInvite(item)">
                      邀请加入
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getMatchedCandidatesForProject, getProjectDetail } from '@/api/project'
import { request } from '@/utils/request'

const route = useRoute()
const router = useRouter()

const projectId = ref<number>(Number(route.params.id))
const projectTitle = ref<string>('')
const loading = ref(false)
const candidateList = ref<any[]>([])
const matchLevel = ref<'ALL' | 'HIGH' | 'MEDIUM' | 'LOW'>('ALL')
const onlyHighCollaboration = ref(false)

// 加载项目信息
const loadProjectInfo = async () => {
  try {
    const project = await getProjectDetail(projectId.value)
    projectTitle.value = project.title || '未知项目'
  } catch (error) {
    console.error('加载项目信息失败:', error)
  }
}

// 加载推荐候选人
const loadCandidates = async () => {
  loading.value = true
  try {
    const data = await getMatchedCandidatesForProject(projectId.value)
    
    if (data && Array.isArray(data)) {
      candidateList.value = data
      if (data.length > 0) {
        ElMessage.success(`为项目找到 ${data.length} 位匹配候选人`)
      } else {
        ElMessage.info('暂无匹配候选人')
      }
    } else {
      candidateList.value = []
      ElMessage.warning('候选人数据格式异常')
    }
  } catch (error) {
    console.error('加载候选人失败:', error)
    candidateList.value = []
  } finally {
    loading.value = false
  }
}

// 刷新推荐
const handleRefresh = () => {
  loadCandidates()
}

// 查看候选人资料
const handleViewProfile = (userId: number) => {
  router.push(`/profile/${userId}`)
}

// 邀请候选人
const handleInvite = async (candidate: any) => {
  try {
    await ElMessageBox.prompt(
      `确定要邀请 ${candidate.username || '该候选人'} 加入项目吗？`,
      '邀请成员',
      {
        confirmButtonText: '发送邀请',
        cancelButtonText: '取消',
        inputPlaceholder: '可以添加一些邀请说明（可选）',
        inputType: 'textarea'
      }
    ).then(async ({ value }) => {
      const message = value || '邀请你加入项目'
      
      await request.post(`/projects/${projectId.value}/invite/${candidate.userId}`, {
        message,
        breakdown: candidate?.breakdown || {}
      })
      
      ElMessage.success('邀请已发送')
    })
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '邀请失败')
    }
  }
}

// 获取匹配度颜色
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 获取详情颜色
const getDetailColor = (value: number | undefined) => {
  if (!value) return '#909399'
  const score = value * 100
  if (score >= 70) return '#67c23a'
  if (score >= 50) return '#e6a23c'
  return '#f56c6c'
}

// 获取信誉标签类型
const getCreditTypeTag = (level: string) => {
  const tags: Record<string, any> = {
    NEWBIE: 'info',
    RELIABLE: '',
    EXCELLENT: 'success',
    OUTSTANDING: 'warning',
  }
  return tags[level] || 'info'
}

// 获取信誉文本
const getCreditText = (level: string) => {
  const texts: Record<string, string> = {
    NEWBIE: '新手',
    RELIABLE: '可靠',
    EXCELLENT: '优秀',
    OUTSTANDING: '杰出',
  }
  return texts[level] || level
}

const dimensionLabelMap: Record<string, string> = {
  skill: '技能匹配',
  collaboration: '协作历史',
  time: '时间匹配',
  experience: '项目经验',
  goal: '目标契合',
  mentor_rating: '导师评分',
  academic: '学术背景',
  credit: '信誉评分',
  application: '申请历史'
}

const dimensionSuggestionMap: Record<string, string> = {
  skill: '补充项目所需技能并提升熟练度，可优先完善技能认证。',
  collaboration: '多参与团队协作任务并提升协作评分，积累协作履历。',
  time: '补充更完整的可用时段，尽量覆盖项目核心协作时间。',
  experience: '完善项目经历描述，突出与你申请方向相关的实践经验。',
  goal: '在个人简介中明确目标方向，强化与项目目标的一致性。',
  mentor_rating: '积极参与导师指导任务，提升导师评分与反馈质量。',
  academic: '补充专业与课程背景信息，体现与项目领域的关联性。',
  credit: '提升信誉分与稳定履约记录，减少爽约和超时行为。',
  application: '提高申请质量与通过率，保持持续稳定的申请表现。'
}

const toBreakdownValue = (value: unknown) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return 0
  return Math.max(0, Math.min(1, num))
}

const getDimensionEntries = (breakdown: Record<string, number> = {}) => {
  return Object.entries(dimensionLabelMap).map(([key, label]) => ({
    key,
    label,
    value: toBreakdownValue((breakdown as any)?.[key])
  }))
}

const sortDimensions = (breakdown: Record<string, number> = {}, asc = false) => {
  const entries = getDimensionEntries(breakdown)
  return entries.sort((a, b) => (asc ? a.value - b.value : b.value - a.value))
}

const getTopStrength = (breakdown: Record<string, number> = {}) => {
  const top = sortDimensions(breakdown)[0]
  if (!top) return '暂无可用数据'
  return `${top.label}表现较好（${Math.round(top.value * 100)}%）`
}

const getMainWeakness = (breakdown: Record<string, number> = {}) => {
  const weak = sortDimensions(breakdown, true)[0]
  if (!weak) return '暂无明显短板'
  return `${weak.label}相对偏弱（${Math.round(weak.value * 100)}%）`
}

const getImprovementSuggestion = (breakdown: Record<string, number> = {}) => {
  const weak = sortDimensions(breakdown, true)[0]
  if (!weak) return '建议继续完善个人信息与项目经历，提高匹配稳定性。'
  return dimensionSuggestionMap[weak.key] || '建议完善低分维度的相关信息与行为数据。'
}

// 根据筛选条件过滤候选人
const filteredCandidates = computed(() => {
  let list = candidateList.value.slice()

  // 匹配度过滤（使用整体 score）
  list = list.filter((item: any) => {
    const percentage = Math.round((item.score || 0) * 100)
    switch (matchLevel.value) {
      case 'HIGH':
        return percentage >= 80
      case 'MEDIUM':
        return percentage >= 60 && percentage < 80
      case 'LOW':
        return percentage < 60
      default:
        return true
    }
  })

  // 协作历史过滤
  if (onlyHighCollaboration.value) {
    list = list.filter((item: any) => {
      const collaboration = (item.breakdown?.collaboration || 0) * 100
      return collaboration >= 70
    })
  }

  // 默认按匹配度从高到低排序
  list.sort((a: any, b: any) => (b.score || 0) - (a.score || 0))

  return list
})

onMounted(() => {
  loadProjectInfo()
  loadCandidates()
})
</script>

<style scoped lang="scss">
.project-candidates {
  padding: 20px;
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subtitle {
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.filter-bar {
  margin-bottom: 20px;
}

.candidates-list {
  .empty-state {
    padding: 40px 0;
  }
}

.candidate-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .card-content {
    display: flex;
    gap: 24px;
  }

  .match-score-section {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;

    .percentage-content {
      display: flex;
      flex-direction: column;
      align-items: center;

      .percentage-value {
        font-size: 18px;
        font-weight: bold;
        color: #303133;
      }

      .percentage-label {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .candidate-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .candidate-header {
      display: flex;
      align-items: center;
      gap: 12px;

      .candidate-name {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }

    .match-details {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .detail-item {
        display: grid;
        grid-template-columns: 80px 1fr 60px;
        align-items: center;
        gap: 12px;

        .label {
          font-size: 14px;
          color: #606266;
        }

        .value {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          text-align: right;
        }
      }
    }

    .match-reason {
      margin: 4px 0 0 0;
      font-size: 12px;
      color: #909399;
    }

    .actions {
      display: flex;
      gap: 12px;
      margin-top: 8px;
    }
  }
}
</style>
