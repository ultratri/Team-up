<template>
  <div class="team-candidates">
    <el-card>
      <template #header>
        <div class="flex-between">
          <div>
            <h3>团队智能找成员</h3>
            <p class="subtitle">为团队“{{ teamName }}”推荐合适的候选人</p>
          </div>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="例如：UI设计师、前端开发、产品经理"
              clearable
              size="small"
              style="width: 260px"
              @keyup.enter.native="handleRefresh"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button
              style="margin-left: 12px"
              @click="handleRefresh"
              :loading="loading"
              type="primary"
            >
              <el-icon><Refresh /></el-icon>
              刷新推荐
            </el-button>
          </div>
        </div>
      </template>

      <div class="filter-bar">
        <el-alert
          title="匹配算法综合考虑技能匹配、协作历史、时间可用性、项目经验、信誉等多个维度（团队可通过关键词强调当前紧缺角色，例如“UI设计师”）"
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
            active-text="优先历史协作好"
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

                  <!-- 推荐理由（来自匹配服务或后端补充） -->
                  <p v-if="item.matchReason" class="match-reason">
                    推荐理由：{{ item.matchReason }}
                  </p>

                  <!-- 操作按钮：查看资料，交由其他页面处理邀请/沟通 -->
                  <div class="actions">
                    <el-button type="primary" size="small" @click="handleViewProfile(item.userId)">
                      查看资料
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
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMatchedCandidatesForTeam, getTeam } from '@/api/team'
import type { CandidateMatchItem } from '@/utils/fieldNormalizer'

const props = defineProps<{
  teamId?: number
}>()

const route = useRoute()
const router = useRouter()

const innerTeamId = computed(() => {
  return props.teamId ?? Number(route.params.id)
})

const teamName = ref<string>('')
const keyword = ref<string>('')
const loading = ref(false)
const candidateList = ref<CandidateMatchItem[]>([])
const matchLevel = ref<'ALL' | 'HIGH' | 'MEDIUM' | 'LOW'>('ALL')
const onlyHighCollaboration = ref(false)

// 加载团队信息
const loadTeamInfo = async () => {
  if (!innerTeamId.value || Number.isNaN(innerTeamId.value)) return
  try {
    const team = await getTeam(innerTeamId.value, true)
    // normalizeTeam 已经被 getTeam 调用，这里直接使用 name 字段
    // @ts-expect-error
    teamName.value = (team as any).name || (team as any).teamName || `团队#${innerTeamId.value}`
  } catch (error) {
    console.error('加载团队信息失败:', error)
  }
}

// 加载推荐候选人
const loadCandidates = async () => {
  if (!innerTeamId.value || Number.isNaN(innerTeamId.value)) {
    ElMessage.error('无效的团队 ID')
    return
  }
  loading.value = true
  try {
    const data = await getMatchedCandidatesForTeam(innerTeamId.value, keyword.value || undefined)

    if (data && Array.isArray(data)) {
      candidateList.value = data
      if (data.length > 0) {
        ElMessage.success(`为团队找到 ${data.length} 位匹配候选人`)
      } else {
        ElMessage.info('暂无匹配候选人，可尝试调整关键词或完善团队信息')
      }
    } else {
      candidateList.value = []
      ElMessage.warning('候选人数据格式异常')
    }
  } catch (error: any) {
    console.error('加载候选人失败:', error)
    candidateList.value = []
    if (error?.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('加载团队候选人失败')
    }
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  loadCandidates()
}

const handleViewProfile = (userId: number) => {
  router.push(`/profile/${userId}`)
}

// 颜色 & 维度工具与 ProjectCandidates 保持一致
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getDetailColor = (value: number | undefined) => {
  if (!value) return '#909399'
  const score = value * 100
  if (score >= 70) return '#67c23a'
  if (score >= 50) return '#e6a23c'
  return '#f56c6c'
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
  skill: '补充团队当前紧缺方向相关的技能标签，并提升熟练度。',
  collaboration: '多参与团队内部协作任务并提升协作评分，积累协作履历。',
  time: '补充更完整的可用时段，尽量覆盖团队核心协作时间。',
  experience: '完善项目经历描述，突出与本团队方向相关的实践经验。',
  goal: '在个人简介中明确个人目标，强化与团队方向的一致性。',
  mentor_rating: '积极参与导师指导任务，提升导师评分与反馈质量。',
  academic: '补充专业与课程背景信息，体现与团队领域的关联性。',
  credit: '保持稳定履约记录，减少爽约和超时行为，提升信誉分。',
  application: '保持高质量的申请与反馈记录，体现认真度与稳定性。'
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

  if (onlyHighCollaboration.value) {
    list = list.filter((item: any) => {
      const collaboration = (item.breakdown?.collaboration || 0) * 100
      return collaboration >= 70
    })
  }

  list.sort((a: any, b: any) => (b.score || 0) - (a.score || 0))

  return list
})

watch(
  () => innerTeamId.value,
  (n, o) => {
    if (n && n !== o) {
      loadTeamInfo()
      loadCandidates()
    }
  }
)

onMounted(() => {
  loadTeamInfo()
  loadCandidates()
})
</script>

<style scoped lang="scss">
.team-candidates {
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

.header-actions {
  display: flex;
  align-items: center;
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

      &.suggestion {
        margin-top: 2px;
      }
    }

    .actions {
      display: flex;
      gap: 12px;
      margin-top: 8px;
    }
  }
}
</style>

