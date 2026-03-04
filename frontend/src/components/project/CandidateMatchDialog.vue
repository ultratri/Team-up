<template>
  <el-drawer
    v-model="visible"
    title="智能推荐候选人"
    size="50%"
    direction="rtl"
    :close-on-click-modal="false"
    :modal="false"
    @close="handleClose"
  >
    <div class="candidate-match-container">
      <!-- 顶部说明 -->
      <el-alert
        title="匹配算法基于技能匹配、时间可用性、用户信誉等多个维度综合评估"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 空状态 -->
      <div v-else-if="candidates.length === 0" class="empty-state">
        <el-empty description="暂无推荐候选人">
          <el-button type="primary" @click="handleRefresh">刷新推荐</el-button>
        </el-empty>
      </div>

      <!-- 候选人列表 -->
      <div v-else class="candidates-grid">
        <div v-for="candidate in candidates" :key="candidate.userId" class="candidate-card">
          <!-- 匹配度圆环 -->
          <div class="match-score">
            <el-progress
              type="circle"
              :percentage="Math.round((candidate.score || 0) * 100)"
              :width="80"
              :color="getScoreColor((candidate.score || 0) * 100)"
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
            <h4 class="candidate-name">{{ candidate.username || '未知用户' }}</h4>
            
            <div class="candidate-meta">
              <span v-if="candidate.department">{{ candidate.department }}</span>
              <span v-if="candidate.major">{{ candidate.major }}</span>
              <span v-if="candidate.grade">{{ candidate.grade }}年级</span>
            </div>

            <p v-if="candidate.bio" class="candidate-bio">{{ candidate.bio }}</p>

            <!-- 匹配详情 -->
            <div class="match-details">
              <div class="detail-item">
                <span class="label">技能匹配</span>
                <el-progress
                  :percentage="Math.round((candidate.breakdown?.skill || 0) * 100)"
                  :stroke-width="6"
                  :show-text="false"
                />
                <span class="value">{{ Math.round((candidate.breakdown?.skill || 0) * 100) }}%</span>
              </div>
              <div class="detail-item">
                <span class="label">协作历史</span>
                <el-progress
                  :percentage="Math.round((candidate.breakdown?.collaboration || 0) * 100)"
                  :stroke-width="6"
                  :show-text="false"
                />
                <span class="value">{{ Math.round((candidate.breakdown?.collaboration || 0) * 100) }}%</span>
              </div>
            </div>

            <!-- 推荐理由 -->
            <p v-if="candidate.matchReason" class="match-reason">
              推荐理由：{{ candidate.matchReason }}
            </p>

            <!-- 操作按钮 -->
            <div class="actions">
              <el-button size="small" @click="handleViewDetail(candidate)">
                查看详情
              </el-button>
              <el-tag
                v-if="getInviteStatus(candidate)"
                size="small"
                type="warning"
              >
                {{ getInviteStatus(candidate) }}
              </el-tag>
              <el-button
                type="primary"
                size="small"
                :loading="invitingUsers[getCandidateUserId(candidate)]"
                :disabled="!!getInviteStatus(candidate)"
                @click="handleInvite(candidate)"
              >
                {{ getInviteStatus(candidate) ? '已邀请' : '邀请加入' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div style="display: flex; gap: 12px;">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="handleRefresh" :loading="loading">
          刷新推荐
        </el-button>
      </div>
    </template>
  </el-drawer>

  <!-- 候选人详情弹窗（完全复用人才墙样式） -->
  <el-drawer
    v-model="showDetailDialog"
    size="600px"
    direction="rtl"
    :modal="false"
    destroy-on-close
    class="talent-detail-drawer"
  >
    <div v-if="selectedCandidate" class="talent-detail">
      <!-- 自定义关闭按钮 -->
      <button class="close-btn" @click="showDetailDialog = false" aria-label="关闭">
        <el-icon><Close /></el-icon>
      </button>

      <!-- 顶部卡片 -->
      <div class="detail-header">
        <div class="avatar-section">
          <el-avatar :size="100" :src="selectedCandidate.avatar">
            {{ (selectedCandidate.username || '用户').charAt(0) }}
          </el-avatar>
        </div>
        
        <div class="info-section">
          <h2>{{ selectedCandidate.username }}</h2>
          <div class="meta-info">
            <span v-if="selectedCandidate.department">{{ selectedCandidate.department }}</span>
            <span v-if="selectedCandidate.major">{{ selectedCandidate.major }}</span>
            <span v-if="selectedCandidate.grade">{{ selectedCandidate.grade }}年级</span>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="mentor-content-area">
        <!-- 匹配度展示 -->
        <div class="match-section">
          <h3>
            <el-icon class="block-icon"><TrendCharts /></el-icon>
            <span>匹配度分析</span>
          </h3>
          <div class="match-breakdown">
            <div v-for="(value, key) in selectedCandidate.breakdown" :key="key" class="breakdown-item">
              <span class="breakdown-label">{{ getDimensionName(key) }}</span>
              <el-progress
                :percentage="Math.round(value * 100)"
                :stroke-width="8"
              />
            </div>
          </div>
        </div>

        <!-- 个人简介 -->
        <div v-if="selectedCandidate.bio" class="bio-section">
          <h3>
            <el-icon class="block-icon"><User /></el-icon>
            <span>个人简介</span>
          </h3>
          <p>{{ selectedCandidate.bio }}</p>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="detail-actions">
        <el-button size="large" @click="showDetailDialog = false">
          返回列表
        </el-button>
        <el-button type="primary" size="large" @click="handleInvite(selectedCandidate)">
          <el-icon><Message /></el-icon>
          邀请加入项目
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Message, TrendCharts, User } from '@element-plus/icons-vue'
import { getMatchedCandidatesForProject } from '@/api/project'
import { request } from '@/utils/request'

interface Props {
  modelValue: boolean
  projectId: number
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const candidates = ref<any[]>([])
const showDetailDialog = ref(false)
const selectedCandidate = ref<any>(null)
const invitingUsers = ref<Record<number, boolean>>({})

// 监听外部visible变化
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadCandidates()
  }
})

// 监听内部visible变化
watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载候选人
const loadCandidates = async () => {
  loading.value = true
  try {
    const data = await getMatchedCandidatesForProject(props.projectId)
    
    if (data && Array.isArray(data)) {
      // 默认按匹配度从高到低排序
      candidates.value = data.slice().sort((a: any, b: any) => (b.score || 0) - (a.score || 0))
      if (candidates.value.length > 0) {
        ElMessage.success(`为项目找到 ${candidates.value.length} 位匹配候选人`)
      } else {
        ElMessage.info('暂无匹配候选人')
      }
    } else {
      candidates.value = []
      ElMessage.warning('候选人数据格式异常')
    }
  } catch (error) {
    console.error('加载候选人失败:', error)
    candidates.value = []
    ElMessage.error('加载候选人失败')
  } finally {
    loading.value = false
  }
}

// 刷新推荐
const handleRefresh = () => {
  loadCandidates()
}

// 查看详情
const handleViewDetail = (candidate: any) => {
  selectedCandidate.value = candidate
  showDetailDialog.value = true
}

const getCandidateUserId = (candidate: any): number => Number(candidate?.userId || candidate?.user_id || 0)

const getInviteStatus = (candidate: any): string => {
  const status = String(candidate?.inviteStatus || candidate?.invite_status || '').toUpperCase()
  if (status === 'PENDING') return '待响应'
  if (status === 'INVITED') return '已邀请'
  return ''
}

const getInviteErrorMessage = (error: any): string => {
  const status = Number(error?.status || error?.response?.status || 0)
  const message = String(error?.message || error?.data?.message || '')

  if (status === 403 || message.includes('只有项目创建者')) return '邀请失败：仅项目创建者可邀请成员'
  if (status === 400 && message.includes('招募')) return '邀请失败：当前项目不在招募中'
  if (status === 404 && message.includes('用户不存在')) return '邀请失败：该用户不存在或已被删除'
  if (status === 404 && message.includes('项目不存在')) return '邀请失败：项目不存在'
  if (message) return `邀请失败：${message}`
  return '邀请失败，请稍后重试'
}

const markCandidateInvited = (candidate: any) => {
  const userId = getCandidateUserId(candidate)
  if (!userId) return
  const index = candidates.value.findIndex((c: any) => getCandidateUserId(c) === userId)
  if (index >= 0) {
    candidates.value[index] = {
      ...candidates.value[index],
      inviteStatus: 'PENDING'
    }
  }
}

// 邀请候选人
const handleInvite = async (candidate: any) => {
  const userId = getCandidateUserId(candidate)
  if (!userId) {
    ElMessage.error('邀请失败：候选人ID无效')
    return
  }

  try {
    const { value } = await ElMessageBox.prompt(
      `确定要邀请 ${candidate?.username || '该候选人'} 加入项目吗？`,
      '邀请成员',
      {
        confirmButtonText: '发送邀请',
        cancelButtonText: '取消',
        inputPlaceholder: '可以添加一些邀请说明（可选）',
        inputType: 'textarea'
      }
    )

    invitingUsers.value[userId] = true
    await request.post(`/projects/${props.projectId}/invite/${userId}`, {
      message: value || '邀请你加入项目',
      breakdown: candidate?.breakdown || {}
    })

    markCandidateInvited(candidate)
    ElMessage.success('邀请已发送，等待对方响应')
  } catch (error: any) {
    if (error === 'cancel') return
    ElMessage.error(getInviteErrorMessage(error))
  } finally {
    invitingUsers.value[userId] = false
  }
}

// 关闭弹窗
const handleClose = () => {
  visible.value = false
}

// 获取匹配度颜色
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 获取维度名称
const getDimensionName = (key: string) => {
  const names: Record<string, string> = {
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
  return names[key] || key
}
</script>

<style scoped lang="scss">
.candidate-match-container {
  min-height: 400px;
  max-height: 70vh;
  overflow-y: auto;
}

.loading-state,
.empty-state {
  padding: 40px 0;
  text-align: center;
}

.candidates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.candidate-card {
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 24px;
  transition: all 0.2s;
  background: var(--bg-card);

  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
    border-color: var(--accent-color);
  }

  .match-score {
    flex-shrink: 0;

    .percentage-content {
      display: flex;
      flex-direction: column;
      align-items: center;

      .percentage-value {
        font-size: 20px;
        font-weight: 800;
        color: var(--text-color);
      }

      .percentage-label {
        font-size: 12px;
        color: var(--text-color-muted);
        margin-top: 4px;
        font-weight: 600;
      }
    }
  }

  .candidate-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .candidate-name {
      margin: 0;
      font-size: 18px;
      font-weight: 800;
      color: var(--text-color);
    }

    .candidate-meta {
      display: flex;
      gap: 8px;
      font-size: 13px;
      flex-wrap: wrap;

      span {
        padding: 4px 12px;
        background: var(--bg-body);
        border-radius: 100px;
        color: var(--text-color-muted);
        font-weight: 600;
        border: 1px solid var(--border-subtle);
      }
    }

    .candidate-bio {
      margin: 0;
      font-size: 14px;
      color: var(--text-color-muted);
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .match-details {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .detail-item {
        display: grid;
        grid-template-columns: 80px 1fr 50px;
        align-items: center;
        gap: 12px;

        .label {
          font-size: 13px;
          color: var(--text-color-muted);
          font-weight: 600;
        }

        .value {
          font-size: 14px;
          font-weight: 700;
          color: var(--text-color);
          text-align: right;
        }
      }
    }

    .actions {
      display: flex;
      gap: 8px;
      margin-top: 4px;
    }
  }
}

/* 详情弹窗样式（完全复用人才墙） */
.talent-detail-drawer {
  :deep(.el-drawer) {
    border-radius: 0;
    background: var(--bg-card);
  }
  :deep(.el-drawer__header) {
    display: none;
  }
  :deep(.el-drawer__body) {
    padding: 0;
  }
}

.talent-detail {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  position: relative;

  .close-btn {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 10;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    background: rgba(0,0,0,0.1);
    backdrop-filter: blur(8px);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-color);
    transition: all 0.2s;
    &:hover { 
      background: rgba(0,0,0,0.2); 
      transform: scale(1.1); 
    }
  }

  .detail-header {
    padding: 40px;
    background: linear-gradient(135deg, rgba(var(--accent-color-rgb), 0.08), rgba(var(--accent-color-rgb), 0.03));
    display: flex;
    gap: 32px;
    align-items: center;
    border-bottom: 1px solid var(--border-subtle);

    .avatar-section {
      flex-shrink: 0;
      
      :deep(.el-avatar) {
        border: 4px solid var(--bg-card);
        box-shadow: 0 12px 30px rgba(0,0,0,0.1);
      }
    }

    .info-section {
      flex: 1;

      h2 {
        font-size: 36px;
        font-weight: 900;
        margin: 0 0 8px;
        color: var(--text-color);
      }

      .meta-info {
        display: flex;
        gap: 12px;
        font-size: 16px;
        color: var(--text-color-muted);
        flex-wrap: wrap;

        span {
          &:not(:last-child)::after {
            content: '·';
            margin-left: 12px;
            color: var(--border-subtle);
          }
        }
      }
    }
  }

  .match-section {
    margin-bottom: 32px;

    h3 {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 800;
      color: var(--text-color);
      margin: 0 0 16px;
      
      .block-icon {
        color: var(--accent-color);
      }
    }

    .match-breakdown {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .breakdown-item {
        .breakdown-label {
          display: inline-block;
          width: 100px;
          font-size: 14px;
          color: var(--text-color-muted);
          margin-bottom: 8px;
          font-weight: 600;
        }
      }
    }
  }

  .bio-section {
    margin-bottom: 32px;

    h3 {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 800;
      color: var(--text-color);
      margin: 0 0 16px;
      
      .block-icon {
        color: var(--accent-color);
      }
    }

    p {
      margin: 0;
      font-size: 15px;
      color: var(--text-color-muted);
      line-height: 1.8;
    }
  }

  .mentor-content-area {
    padding: 32px 40px;
    overflow-y: auto;
    flex: 1;
  }

  .detail-actions {
    padding: 24px 40px;
    display: flex;
    gap: 16px;
    border-top: 1px solid var(--border-subtle);
    background: var(--bg-card);
    
    .el-button {
      flex: 1;
      height: 52px;
      border-radius: 14px;
      font-weight: 700;
      font-size: 16px;
    }
  }
}

/* 确保抽屉不阻止左侧点击 */
:deep(.el-drawer__container) {
  pointer-events: none !important;
}

:deep(.el-drawer) {
  pointer-events: auto !important;
}
</style>
