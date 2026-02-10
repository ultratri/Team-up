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
                      :percentage="result.skill_match_score || result.skill_match || 0"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="value">{{ result.skill_match_score || result.skill_match || 0 }}%</span>
                  </div>
                  <!-- 可以增加语义匹配显示 -->
                  <div class="detail-item" v-if="result.semantic_match_score">
                     <span class="label">语义契合:</span>
                     <el-progress
                      :percentage="result.semantic_match_score"
                      :stroke-width="8"
                      :show-text="false"
                    />
                    <span class="value">{{ result.semantic_match_score }}%</span>
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
import { Refresh } from '@element-plus/icons-vue'
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

            .label {
              min-width: 80px;
              color: #606266;
              font-size: 14px;
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

