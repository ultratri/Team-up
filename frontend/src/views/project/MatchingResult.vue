<template>
  <div class="matching-result">
    <el-card>
      <template #header>
        <div class="flex-between">
          <h3>智能匹配推荐</h3>
          <el-button @click="handleRefresh" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新匹配
          </el-button>
        </div>
      </template>

      <div style="margin-bottom: 12px;">
        <el-alert
          title="这是演示页面：输入项目ID后点击『开始匹配』，会调用后端 /projects/{projectId}/recommendations 获取推荐队友列表。"
          type="info"
          show-icon
          :closable="false"
        />
        <el-alert
          title="匹配说明"
          type="success"
          :closable="false"
          style="margin-top: 12px;"
        >
          <template #default>
            <p style="margin: 0; font-size: 13px;">
              • 匹配度基于9个维度综合计算：技能匹配(40%)、协作能力(15%)、时间匹配(15%)等<br/>
              • 协作能力维度包含成员互评数据；若存在“导师对成员评价”，也会对协作能力做小幅加成<br/>
              • 新用户可能因评价数据不足而协作能力分数较低，这是正常现象
            </p>
          </template>
        </el-alert>
        <div style="display: flex; gap: 12px; margin-top: 12px; align-items: center; flex-wrap: wrap;">
          <el-input
            v-model="projectIdInput"
            placeholder="请输入项目ID（例如 1）"
            style="width: 240px;"
            clearable
          />
          <el-button type="primary" @click="handleStartMatch" :loading="loading">
            开始匹配
          </el-button>
          <el-button @click="handleRefresh" :loading="loading" :disabled="!resolvedProjectId">
            刷新匹配
          </el-button>
          <span v-if="resolvedProjectId" style="color: #909399;">当前项目ID：{{ resolvedProjectId }}</span>
        </div>
      </div>

      <div class="match-list">
        <el-skeleton :loading="loading" animated :count="3">
          <template #template>
            <div class="match-item" style="border: 1px solid #ebeef5; padding: 20px; margin-bottom: 16px; display: flex; gap: 24px;">
              <div class="match-score">
                <el-skeleton-item variant="circle" style="width: 126px; height: 126px" />
              </div>
              <div class="match-info" style="flex: 1">
                <div style="display: flex; gap: 12px; margin-bottom: 12px;">
                  <el-skeleton-item variant="h3" style="width: 30%" />
                  <el-skeleton-item variant="text" style="width: 60px" />
                </div>
                <div style="margin-bottom: 16px;">
                  <el-skeleton-item variant="text" style="width: 80%" />
                  <el-skeleton-item variant="text" style="width: 60%; margin-top: 8px" />
                </div>
                <div style="display: flex; gap: 12px;">
                  <el-skeleton-item variant="button" style="width: 80px" />
                  <el-skeleton-item variant="button" style="width: 80px" />
                </div>
              </div>
            </div>
          </template>
          <template #default>
            <div v-for="result in matchResults" :key="result.user_id" class="match-item">
              <div class="match-score">
                <el-progress
                  type="circle"
                  :percentage="result.match_score"
                  :color="getScoreColor(result.match_score)"
                >
                  <template #default="{ percentage }">
                    <span class="percentage-value">{{ percentage }}</span>
                    <span class="percentage-label">匹配度</span>
                  </template>
                </el-progress>
              </div>

              <div class="match-info">
                <div class="user-header">
                  <h4>{{ result.username }}</h4>
                  <el-tag :type="getCreditType(result.credit_level)" size="small">
                    {{ getCreditText(result.credit_level) }}
                  </el-tag>
                </div>

                <div class="match-detail">
                  <div class="detail-item">
                    <span class="label">技能匹配:</span>
                    <el-progress
                      :percentage="Math.round((result.breakdown?.skill || result.skill_match_score || result.skill_match || 0) * 100)"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="value">{{ Math.round((result.breakdown?.skill || result.skill_match_score || result.skill_match || 0) * 100) }}%</span>
                  </div>
                  
                  <!-- 协作能力（包含成员互评数据） -->
                  <div class="detail-item">
                    <span class="label">
                      协作能力:
                      <el-tooltip content="基于成员互评和协作历史计算" placement="top">
                        <el-icon class="info-icon"><InfoFilled /></el-icon>
                      </el-tooltip>
                    </span>
                    <el-progress
                      :percentage="Math.round((result.breakdown?.collaboration || 0) * 100)"
                      :stroke-width="8"
                      :show-text="false"
                      :color="getCollaborationColor(result.breakdown?.collaboration)"
                    />
                    <span class="value">{{ Math.round((result.breakdown?.collaboration || 0) * 100) }}%</span>
                    <span v-if="!hasEvaluationData(result)" class="data-hint" title="该用户暂无成员互评数据">
                      <el-icon><Warning /></el-icon>
                    </span>
                  </div>
                  
                  <!-- 时间匹配 -->
                  <div class="detail-item" v-if="result.breakdown?.time !== undefined">
                    <span class="label">时间匹配:</span>
                    <el-progress
                      :percentage="Math.round((result.breakdown.time || 0) * 100)"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="value">{{ Math.round((result.breakdown.time || 0) * 100) }}%</span>
                  </div>
                  
                  <!-- 可以增加语义匹配显示 -->
                  <div class="detail-item" v-if="result.semantic_match_score || result.breakdown?.goal">
                     <span class="label">目标契合:</span>
                     <el-progress
                      :percentage="Math.round((result.breakdown?.goal || result.semantic_match_score || 0) * 100)"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="value">{{ Math.round((result.breakdown?.goal || result.semantic_match_score || 0) * 100) }}%</span>
                  </div>
                  
                  <!-- 匹配原因 -->
                  <div v-if="result.match_reason" class="match-reason">
                    <el-tag size="small" type="info">{{ result.match_reason }}</el-tag>
                  </div>
                </div>

                <div class="match-actions">
                  <el-button type="primary" size="small" @click="handleViewProfile(result.user_id)">
                    查看档案
                  </el-button>
                  <el-button size="small" @click="handleInvite(result.user_id)">
                    邀请加入
                  </el-button>
                </div>
              </div>
            </div>

            <el-empty v-if="matchResults.length === 0" description="暂无匹配结果" />
          </template>
        </el-skeleton>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { Refresh, InfoFilled, Warning } from '@element-plus/icons-vue'
import { getProjectRecommendations } from '../../api/project'

const route = useRoute()

const props = defineProps<{
  projectId?: number
}>()

const matchResults = ref<any[]>([])
const loading = ref(false)

const projectIdInput = ref<string>('')
const resolvedProjectId = ref<number | null>(null)

const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

const getCreditType = (level: string) => {
  const types: Record<string, any> = {
    NEWBIE: 'info',
    RELIABLE: 'success',
    EXCELLENT: 'warning',
    OUTSTANDING: 'danger',
  }
  return types[level] || 'info'
}

const getCreditText = (level: string) => {
  const texts: Record<string, string> = {
    NEWBIE: '新手',
    RELIABLE: '可靠',
    EXCELLENT: '优秀',
    OUTSTANDING: '卓越',
  }
  return texts[level] || level
}

// 获取协作能力颜色（根据分数）
const getCollaborationColor = (score?: number) => {
  if (!score) return '#909399'
  const percentage = score * 100
  if (percentage >= 70) return '#67c23a'
  if (percentage >= 50) return '#409eff'
  if (percentage >= 30) return '#e6a23c'
  return '#f56c6c'
}

// 判断是否有评价数据
const hasEvaluationData = (result: any) => {
  // 如果协作分数为0或很低，可能是没有评价数据
  const collaboration = result.breakdown?.collaboration || 0
  // 通常有评价数据的用户协作分数会更高
  return collaboration > 0.2
}

const loadMatching = async (projectId: number) => {
  loading.value = true
  try {
    const res = await getProjectRecommendations(projectId)
    if (res.data) {
        matchResults.value = res.data
    } else {
        matchResults.value = []
    }
  } catch (error) {
    console.error(error)
    // ElMessage.error('加载匹配结果失败') // request.ts 已经处理了错误提示
  } finally {
    loading.value = false
  }
}

const handleStartMatch = async () => {
  const id = Number(projectIdInput.value)
  if (!id || Number.isNaN(id)) {
    ElMessage.warning('请输入有效的项目ID')
    return
  }

  resolvedProjectId.value = id
  await loadMatching(id)
}

const handleRefresh = () => {
  if (!resolvedProjectId.value) {
    ElMessage.info('请先输入项目ID并点击开始匹配')
    return
  }
  loadMatching(resolvedProjectId.value)
}

const handleViewProfile = (userId: number) => {
  // TODO: 跳转到用户档案页面
  ElMessage.info(`查看用户 ${userId} 的档案`)
}

const handleInvite = (_userId: number) => {
  // TODO: 邀请用户加入项目
  ElMessage.success('邀请已发送')
}

onMounted(() => {
  // 如果 URL 中有 projectId 参数，则自动加载
  if (route.query.projectId) {
    projectIdInput.value = String(route.query.projectId)
    handleStartMatch()
  } else if (props.projectId) {
    // 或者通过 props 传入
    projectIdInput.value = String(props.projectId)
    handleStartMatch()
  }
})
</script>

<style scoped lang="scss">
.matching-result {
  .match-list {
    .match-item {
      display: flex;
      gap: 24px;
      padding: 20px;
      border: 1px solid #ebeef5;
      border-radius: 8px;
      margin-bottom: 16px;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      }

      .match-score {
        flex-shrink: 0;

        :deep(.el-progress__text) {
          display: flex;
          flex-direction: column;
          align-items: center;

          .percentage-value {
            font-size: 24px;
            font-weight: bold;
          }

          .percentage-label {
            font-size: 12px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }

      .match-info {
        flex: 1;

        .user-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 12px;

          h4 {
            margin: 0;
            font-size: 16px;
          }
        }

        .match-detail {
          margin-bottom: 16px;

          .detail-item {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 8px;
            
            &:last-child {
              margin-bottom: 0;
            }
          }
          
          .match-reason {
            margin-top: 12px;
            padding-top: 12px;
            border-top: 1px solid #ebeef5;
          }

            .label {
              min-width: 100px;
              color: #606266;
              font-size: 14px;
              display: flex;
              align-items: center;
              gap: 4px;
              
              .info-icon {
                color: #909399;
                font-size: 14px;
                cursor: help;
              }
            }
            
            .data-hint {
              color: #e6a23c;
              font-size: 14px;
              margin-left: 4px;
              cursor: help;
            }

            :deep(.el-progress) {
              flex: 1;
            }

            .value {
              min-width: 50px;
              text-align: right;
              color: #303133;
              font-weight: 500;
            }
          }
        }

        .match-actions {
          display: flex;
          gap: 12px;
        }
      }
    }
  }
}
</style>

