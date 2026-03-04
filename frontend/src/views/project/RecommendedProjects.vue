<template>
  <div class="recommended-projects">
    <el-card>
      <template #header>
        <div class="flex-between">
          <div>
            <h3>为我推荐的项目</h3>
            <p class="subtitle">基于你的技能、兴趣和时间，为你智能匹配最合适的项目</p>
          </div>
          <el-button @click="handleRefresh" :loading="loading" type="primary">
            <el-icon><Refresh /></el-icon>
            刷新推荐
          </el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-alert
          title="匹配算法会综合技能、协作历史、时间匹配、项目经验、目标契合、学术背景、信誉等多个维度进行评分（技能和协作维度权重更高）"
          type="info"
          :closable="false"
          show-icon
        />
        <div class="filter-controls">
          <span class="filter-label">排序优先：</span>
          <el-radio-group v-model="sortKey" size="small">
            <el-radio-button label="overall">综合匹配</el-radio-button>
            <el-radio-button label="skill">技能优先</el-radio-button>
            <el-radio-button label="collaboration">协作优先</el-radio-button>
            <el-radio-button label="time">时间优先</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="project-list">
        <el-skeleton :loading="loading" animated :count="3">
          <template #template>
            <div class="project-card" style="border: 1px solid #ebeef5; padding: 24px; margin-bottom: 16px;">
              <div style="display: flex; gap: 24px;">
                <div style="flex-shrink: 0;">
                  <el-skeleton-item variant="circle" style="width: 120px; height: 120px" />
                </div>
                <div style="flex: 1;">
                  <el-skeleton-item variant="h3" style="width: 40%; margin-bottom: 12px" />
                  <el-skeleton-item variant="text" style="width: 80%; margin-bottom: 8px" />
                  <el-skeleton-item variant="text" style="width: 60%; margin-bottom: 16px" />
                  <div style="display: flex; gap: 12px;">
                    <el-skeleton-item variant="button" style="width: 100px" />
                    <el-skeleton-item variant="button" style="width: 100px" />
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template #default>
            <div v-for="item in sortedProjects" :key="item.project.id" class="project-card">
              <div class="card-content">
                <!-- 匹配度圆环 -->
                <div class="match-score-section">
                  <el-progress
                    type="circle"
                    :percentage="Math.round(item.matchScore * 100)"
                    :width="120"
                    :color="getScoreColor(item.matchScore * 100)"
                  >
                    <template #default="{ percentage }">
                      <div class="percentage-content">
                        <span class="percentage-value">{{ percentage }}</span>
                        <span class="percentage-label">匹配度</span>
                      </div>
                    </template>
                  </el-progress>
                  <div class="match-reason">{{ item.matchReason }}</div>
                </div>

                <!-- 项目信息 -->
                <div class="project-info">
                  <div class="project-header">
                    <h4 class="project-title">{{ item.project.title }}</h4>
                    <el-tag :type="getProjectTypeTag(item.project.projectType)" size="small">
                      {{ getProjectTypeText(item.project.projectType) }}
                    </el-tag>
                    <el-tag :type="getStatusTypeTag(item.project.status)" size="small">
                      {{ getStatusText(item.project.status) }}
                    </el-tag>
                  </div>

                  <p class="project-desc">{{ item.project.description || '暂无描述' }}</p>

                  <!-- 匹配详情 -->
                  <div class="match-breakdown">
                    <div class="breakdown-item">
                      <span class="label">技能匹配</span>
                      <el-progress
                        :percentage="Math.round((item.breakdown?.skill || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getBreakdownColor(item.breakdown?.skill)"
                      />
                      <span class="value">{{ Math.round((item.breakdown?.skill || 0) * 100) }}%</span>
                    </div>
                    <div class="breakdown-item">
                      <span class="label">协作历史</span>
                      <el-progress
                        :percentage="Math.round((item.breakdown?.collaboration || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getBreakdownColor(item.breakdown?.collaboration)"
                      />
                      <span class="value">{{ Math.round((item.breakdown?.collaboration || 0) * 100) }}%</span>
                    </div>
                    <div class="breakdown-item">
                      <span class="label">时间匹配</span>
                      <el-progress
                        :percentage="Math.round((item.breakdown?.time || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getBreakdownColor(item.breakdown?.time)"
                      />
                      <span class="value">{{ Math.round((item.breakdown?.time || 0) * 100) }}%</span>
                    </div>
                    <div class="breakdown-item">
                      <span class="label">项目经验</span>
                      <el-progress
                        :percentage="Math.round((item.breakdown?.experience || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getBreakdownColor(item.breakdown?.experience)"
                      />
                      <span class="value">{{ Math.round((item.breakdown?.experience || 0) * 100) }}%</span>
                    </div>
                    <div class="breakdown-item">
                      <span class="label">目标契合</span>
                      <el-progress
                        :percentage="Math.round((item.breakdown?.goal || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getBreakdownColor(item.breakdown?.goal)"
                      />
                      <span class="value">{{ Math.round((item.breakdown?.goal || 0) * 100) }}%</span>
                    </div>
                    <div class="breakdown-item">
                      <span class="label">学术背景</span>
                      <el-progress
                        :percentage="Math.round((item.breakdown?.academic || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                        :color="getBreakdownColor(item.breakdown?.academic)"
                      />
                      <span class="value">{{ Math.round((item.breakdown?.academic || 0) * 100) }}%</span>
                    </div>
                  </div>

                  <!-- 如何提升匹配 -->
                  <div v-if="getImprovementHints(item).length" class="improvement-hints">
                    <div class="hint-title">如何提升与此类项目的匹配：</div>
                    <ul>
                      <li v-for="(hint, idx) in getImprovementHints(item)" :key="idx">
                        {{ hint }}
                      </li>
                    </ul>
                    <el-button type="primary" link @click="goToProfile">前往个人中心完善信息</el-button>
                  </div>

                  <!-- 项目基本信息 -->
                  <div class="project-meta">
                    <span><el-icon><User /></el-icon> {{ item.project.creatorName || '未知' }}</span>
                    <span><el-icon><Clock /></el-icon> 每周{{ item.project.weeklyHours || 0 }}小时</span>
                    <span><el-icon><Calendar /></el-icon> {{ item.project.expectedDuration || 0 }}天</span>
                  </div>

                  <!-- 操作按钮 -->
                  <div class="project-actions">
                    <el-button type="primary" @click="handleViewDetail(item.project.id)">
                      查看详情
                    </el-button>
                    <el-button @click="handleApply(item.project.id)">
                      申请加入
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <el-empty v-if="projectList.length === 0 && !loading" description="暂无推荐项目">
              <template #extra>
                <p style="color: #909399; font-size: 14px; margin-bottom: 12px;">
                  可能的原因：
                </p>
                <ul style="color: #909399; font-size: 14px; text-align: left; max-width: 400px; margin: 0 auto;">
                  <li>当前没有招募中的项目</li>
                  <li>你的技能信息不完整，请前往个人中心完善</li>
                  <li>你的兴趣标签未设置，请前往个人中心添加</li>
                </ul>
                <el-button type="primary" style="margin-top: 16px;" @click="goToProfile">
                  完善个人信息
                </el-button>
              </template>
            </el-empty>
          </template>
        </el-skeleton>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Refresh, User, Clock, Calendar } from '@element-plus/icons-vue'
import { getMatchedProjectsForMe } from '@/api/project'
import { applyProject } from '@/api/project'

const router = useRouter()

const projectList = ref<any[]>([])
const loading = ref(false)
const sortKey = ref<'overall' | 'skill' | 'collaboration' | 'time'>('overall')

// 获取匹配分数颜色
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#409eff'
  if (score >= 40) return '#e6a23c'
  return '#f56c6c'
}

// 获取breakdown颜色
const getBreakdownColor = (value?: number) => {
  if (!value) return '#f56c6c'
  const score = value * 100
  if (score >= 70) return '#67c23a'
  if (score >= 50) return '#409eff'
  if (score >= 30) return '#e6a23c'
  return '#f56c6c'
}

// 获取项目类型标签
const getProjectTypeTag = (type: string) => {
  const tags: Record<string, any> = {
    COMPETITION: 'danger',
    RESEARCH: 'primary',
    STARTUP: 'warning',
    OPENSOURCE: 'success',
    OTHER: 'info',
  }
  return tags[type] || 'info'
}

// 获取项目类型文本
const getProjectTypeText = (type: string) => {
  const texts: Record<string, string> = {
    COMPETITION: '竞赛',
    RESEARCH: '科研',
    STARTUP: '创业',
    OPENSOURCE: '开源',
    OTHER: '其他',
  }
  return texts[type] || type
}

// 获取状态标签类型
const getStatusTypeTag = (status: string) => {
  const tags: Record<string, any> = {
    DRAFT: 'info',
    RECRUITING: 'success',
    IN_PROGRESS: 'warning',
    COMPLETED: '',
    ARCHIVED: 'info',
  }
  return tags[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    DRAFT: '草稿',
    RECRUITING: '招募中',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    ARCHIVED: '已归档',
  }
  return texts[status] || status
}

// 根据用户偏好排序后的项目列表
const sortedProjects = computed(() => {
  const list = projectList.value.slice()

  const getDim = (item: any, key: string) => item.breakdown?.[key] ?? 0

  switch (sortKey.value) {
    case 'skill':
      return list.sort((a, b) => getDim(b, 'skill') - getDim(a, 'skill'))
    case 'collaboration':
      return list.sort((a, b) => getDim(b, 'collaboration') - getDim(a, 'collaboration'))
    case 'time':
      return list.sort((a, b) => getDim(b, 'time') - getDim(a, 'time'))
    default:
      // 综合匹配按总分排序
      return list.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
  }
})

// 推荐项目改进建议（根据各维度短板给出操作指引）
const getImprovementHints = (item: any): string[] => {
  const hints: string[] = []
  const b = item.breakdown || {}

  if ((b.skill || 0) < 0.5) {
    hints.push('技能匹配偏低，建议在个人中心补充与该项目相关的技能标签。')
  }
  if ((b.time || 0) < 0.5) {
    hints.push('时间匹配偏低，建议在个人中心调整“每周可投入时间”等可用时间设置。')
  }
  if ((b.credit || 0) < 0.4) {
    hints.push('信誉评分较低，多参与项目并保持良好协作可以逐步提升匹配质量。')
  }

  return hints
}

// 加载推荐项目
const loadRecommendedProjects = async () => {
  loading.value = true
  try {
    const data = await getMatchedProjectsForMe(1, 20)
    
    if (data && Array.isArray(data)) {
      projectList.value = data
      if (data.length > 0) {
        ElMessage.success(`为您找到 ${data.length} 个匹配项目`)
      } else {
        ElMessage.info('暂无推荐项目，请完善个人信息以获得更好的推荐')
      }
    } else {
      projectList.value = []
      ElMessage.warning('推荐数据格式异常')
    }
  } catch (error) {
    console.error('加载推荐项目失败:', error)
    projectList.value = []
  } finally {
    loading.value = false
  }
}

// 刷新推荐
const handleRefresh = () => {
  loadRecommendedProjects()
}

// 预加载详情页组件，提升跳转速度
const preloadProjectDetail = () => {
  import('../project/ProjectDetail.vue').catch(() => {
    // 预加载失败时忽略，真实跳转再加载
  })
}

// 查看项目详情
const handleViewDetail = (projectId: number) => {
  router.push(`/project/${projectId}`)
}

// 申请加入项目
const handleApply = async (projectId: number) => {
  try {
    await applyProject(projectId)
    ElMessage.success('申请已提交，请等待项目创建者审核')
  } catch (error) {
    console.error('申请失败:', error)
  }
}

// 前往个人中心
const goToProfile = () => {
  router.push('/profile')
}

onMounted(() => {
  // 预加载详情页组件，提升跳转速度
  preloadProjectDetail()
  loadRecommendedProjects()
})
</script>

<style scoped lang="scss">
.recommended-projects {
  .subtitle {
    margin: 8px 0 0 0;
    font-size: 14px;
    color: #909399;
  }

  .filter-bar {
    margin-bottom: 20px;
  }

  .project-list {
    .project-card {
      border: 1px solid #ebeef5;
      border-radius: 8px;
      padding: 24px;
      margin-bottom: 16px;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
        border-color: #409eff;
      }

      .card-content {
        display: flex;
        gap: 24px;

        .match-score-section {
          flex-shrink: 0;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;

          :deep(.el-progress__text) {
            .percentage-content {
              display: flex;
              flex-direction: column;
              align-items: center;

              .percentage-value {
                font-size: 28px;
                font-weight: bold;
                line-height: 1;
              }

              .percentage-label {
                font-size: 12px;
                color: #909399;
                margin-top: 4px;
              }
            }
          }

          .match-reason {
            font-size: 12px;
            color: #67c23a;
            text-align: center;
            max-width: 120px;
          }
        }

        .project-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 12px;

          .project-header {
            display: flex;
            align-items: center;
            gap: 12px;

            .project-title {
              margin: 0;
              font-size: 18px;
              font-weight: 600;
              color: #303133;
            }
          }

          .project-desc {
            margin: 0;
            color: #606266;
            font-size: 14px;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .match-breakdown {
            display: flex;
            flex-direction: column;
            gap: 8px;
            padding: 12px;
            background: #f5f7fa;
            border-radius: 4px;

            .breakdown-item {
              display: flex;
              align-items: center;
              gap: 12px;

              .label {
                min-width: 70px;
                font-size: 13px;
                color: #606266;
              }

              :deep(.el-progress) {
                flex: 1;
              }

              .value {
                min-width: 45px;
                text-align: right;
                font-size: 13px;
                font-weight: 500;
                color: #303133;
              }
            }
          }

          .project-meta {
            display: flex;
            gap: 20px;
            font-size: 13px;
            color: #909399;

            span {
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }

          .project-actions {
            display: flex;
            gap: 12px;
            margin-top: 4px;
          }

            .improvement-hints {
              margin-top: 8px;
              padding: 8px 10px;
              background: #f5f7fa;
              border-radius: 4px;
              font-size: 12px;
              color: #606266;

              .hint-title {
                font-weight: 600;
                margin-bottom: 4px;
              }

              ul {
                padding-left: 18px;
                margin: 0 0 4px 0;
              }
            }
        }
      }
    }
  }
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
</style>
