<template>
  <div class="project-history-card">
    <div class="header">
      <h3>项目经验</h3>
      <el-tag v-if="experienceScore" type="success" size="small">
        <el-icon><CircleCheck /></el-icon>
        系统验证
      </el-tag>
    </div>

    <!-- 经验分数概览 -->
    <div v-if="experienceScore" class="experience-overview">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-statistic title="综合得分" :value="experienceScore.totalScore" :precision="1" suffix="/100" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="已完成项目" :value="experienceScore.completedProjects" suffix="个" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="担任负责人" :value="experienceScore.leaderProjects" suffix="次" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="项目类型" :value="experienceScore.projectTypeDiversity" suffix="种" />
        </el-col>
      </el-row>
    </div>

    <!-- 项目履历列表 -->
    <div v-if="history && history.length > 0" class="history-list">
      <el-timeline>
        <el-timeline-item
          v-for="item in history"
          :key="item.id"
          :timestamp="formatDate(item.completedAt)"
          placement="top"
        >
          <el-card>
            <div class="project-header">
              <div class="title-row">
                <h4>{{ item.projectTitle }}</h4>
                <el-tag :type="item.role === 'LEADER' ? 'danger' : 'primary'" size="small">
                  {{ item.role === 'LEADER' ? '项目负责人' : '团队成员' }}
                </el-tag>
              </div>
              <el-tag size="small">{{ getProjectTypeLabel(item.projectType) }}</el-tag>
            </div>

            <div class="project-desc" v-html="item.projectDescription"></div>

            <div class="project-stats">
              <div class="stat-item">
                <span class="label">参与天数</span>
                <span class="value">{{ item.durationDays }} 天</span>
              </div>
              <div v-if="item.evaluationCount > 0" class="stat-item">
                <span class="label">团队评分</span>
                <span class="value">
                  <el-rate
                    v-model="item.avgScore"
                    disabled
                    show-score
                    text-color="#ff9900"
                    :max="5"
                  />
                </span>
              </div>
              <div v-if="item.evaluationCount > 0" class="stat-item">
                <span class="label">收到评价</span>
                <span class="value">{{ item.evaluationCount }} 条</span>
              </div>
            </div>

            <!-- 详细评分 -->
            <div v-if="item.evaluationCount > 0" class="detailed-scores">
              <el-descriptions :column="3" size="small" border>
                <el-descriptions-item label="技术贡献">
                  {{ item.avgTechScore?.toFixed(1) || '-' }} / 5.0
                </el-descriptions-item>
                <el-descriptions-item label="协作能力">
                  {{ item.avgCollaborationScore?.toFixed(1) || '-' }} / 5.0
                </el-descriptions-item>
                <el-descriptions-item label="任务完成">
                  {{ item.avgTaskCompletionScore?.toFixed(1) || '-' }} / 5.0
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <!-- 无数据提示 -->
    <el-empty v-else description="暂无项目经验记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CircleCheck } from '@element-plus/icons-vue'
import { getUserProjectHistory, getUserExperienceScore } from '@/api/projectHistory'
import type { ProjectHistoryItem, ExperienceScore } from '@/api/projectHistory'

const props = defineProps<{
  userId: number
}>()

const history = ref<ProjectHistoryItem[]>([])
const experienceScore = ref<ExperienceScore | null>(null)
const loading = ref(false)

const projectTypeLabels: Record<string, string> = {
  COMPETITION: '竞赛',
  RESEARCH: '科研',
  STARTUP: '创业',
  OPENSOURCE: '开源',
  OTHER: '其他'
}

const getProjectTypeLabel = (type: string) => {
  return projectTypeLabels[type] || type
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const loadData = async () => {
  loading.value = true
  try {
    // 并行加载履历和经验分数
    const [historyData, scoreData] = await Promise.all([
      getUserProjectHistory(props.userId, true),
      getUserExperienceScore(props.userId)
    ])
    
    history.value = historyData
    experienceScore.value = scoreData
  } catch (error) {
    console.error('加载项目履历失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.project-history-card {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .experience-overview {
    margin-bottom: 24px;
    padding: 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 8px;
    color: white;

    :deep(.el-statistic__head) {
      color: rgba(255, 255, 255, 0.9);
      font-size: 13px;
    }

    :deep(.el-statistic__content) {
      color: white;
      font-weight: 600;
    }
  }

  .history-list {
    margin-top: 20px;
  }

  .project-header {
    margin-bottom: 12px;

    .title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      h4 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }
    }
  }

  .project-desc {
    margin: 12px 0;
    color: #666;
    font-size: 14px;
    line-height: 1.6;

    :deep(p) {
      margin: 0;
    }
  }

  .project-stats {
    display: flex;
    gap: 24px;
    margin: 16px 0;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;

    .stat-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .label {
        font-size: 12px;
        color: #909399;
      }

      .value {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }
    }
  }

  .detailed-scores {
    margin-top: 12px;
  }
}
</style>
