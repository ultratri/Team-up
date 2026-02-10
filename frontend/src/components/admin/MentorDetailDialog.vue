<template>
  <el-dialog
    v-model="visible"
    title="导师详情"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading" class="mentor-detail">
      <template v-if="detail">
        <!-- 基本信息 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </div>
          </template>
          
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">
              <el-tag type="primary">{{ detail.realName || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ detail.username || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="院系">
              {{ detail.department || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="专业">
              {{ detail.major || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ detail.email || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机">
              {{ detail.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="成为导师时间" :span="2">
              {{ formatDate(detail.becameMentorAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 个人简介 -->
        <el-card v-if="detail.bio" class="info-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><Document /></el-icon>
              <span>个人简介</span>
            </div>
          </template>
          <div class="bio-content">{{ detail.bio }}</div>
        </el-card>

        <!-- 申请理由 -->
        <el-card v-if="detail.applicationReason" class="info-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><ChatLineSquare /></el-icon>
              <span>申请理由</span>
            </div>
          </template>
          <div class="bio-content">{{ detail.applicationReason }}</div>
        </el-card>

        <!-- 绩效统计 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon><TrendCharts /></el-icon>
              <span>绩效统计</span>
            </div>
          </template>
          
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value">{{ detail.totalMentees || 0 }}</div>
                <div class="stat-label">总学员数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value" style="color: #67c23a;">{{ detail.activeMentees || 0 }}</div>
                <div class="stat-label">活跃学员</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value" style="color: #e6a23c;">{{ detail.completedMentees || 0 }}</div>
                <div class="stat-label">完成学员</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-value" style="color: #409eff;">{{ detail.successfulMentees || 0 }}</div>
                <div class="stat-label">成功培养</div>
              </div>
            </el-col>
          </el-row>

          <el-divider />

          <el-descriptions :column="2" border>
            <el-descriptions-item label="学员平均信誉分">
              <el-progress 
                :percentage="Math.round(detail.averageMenteeScore || 0)" 
                :color="getScoreColor(detail.averageMenteeScore || 0)"
                :stroke-width="10"
              />
            </el-descriptions-item>
            <el-descriptions-item label="累计奖励积分">
              <el-tag type="warning" effect="dark" size="large">
                {{ detail.totalRewardPoints || 0 }} 分
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="导师评分" :span="2">
              <el-rate 
                :model-value="Number(detail.rating) || 0" 
                disabled 
                show-score 
                text-color="#ff9900"
                :score-template="`${Number(detail.rating || 0).toFixed(1)} 分`"
              />
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </template>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Document, ChatLineSquare, TrendCharts } from '@element-plus/icons-vue'
import { getMentorDetail, type MentorDetail } from '@/api/mentor'
import { useAuthStore } from '@/store/auth'

interface Props {
  modelValue: boolean
  mentorId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(false)
const loading = ref(false)
const detail = ref<MentorDetail | null>(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.mentorId) {
    loadDetail()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadDetail = async () => {
  if (!props.mentorId) return
  
  // 调试：打印认证信息
  console.group('🔍 加载导师详情')
  console.log('导师ID:', props.mentorId)
  console.log('Token存在:', !!localStorage.getItem('token') || !!sessionStorage.getItem('token'))
  console.log('Token预览:', (localStorage.getItem('token') || sessionStorage.getItem('token') || '').substring(0, 30))
  
  // 检查认证状态
  const authStore = useAuthStore()
  console.log('AuthStore状态:', {
    hasToken: !!authStore.token,
    hasUser: !!authStore.user,
    roles: authStore.roles,
    isAuthenticated: authStore.isAuthenticated
  })
  console.groupEnd()
  
  loading.value = true
  try {
    detail.value = await getMentorDetail(props.mentorId)
    if (!detail.value) {
      ElMessage.error('获取导师详情失败')
      handleClose()
    }
  } catch (error: any) {
    console.error('❌ 获取导师详情失败:', error)
    
    // 如果是401错误，提示用户重新登录
    if (error.status === 401) {
      ElMessage.error({
        message: '登录已过期，请重新登录',
        duration: 3000
      })
      // 不自动关闭对话框，让用户看到错误信息
    } else {
      ElMessage.error(error.message || '获取导师详情失败')
    }
    handleClose()
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
  detail.value = null
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped lang="scss">
.mentor-detail {
  .info-card {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      font-size: 15px;
    }
  }

  .bio-content {
    line-height: 1.8;
    color: #606266;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .stat-item {
    text-align: center;
    padding: 16px 0;

    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 8px;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
    }
  }

  :deep(.el-descriptions__label) {
    width: 140px;
    font-weight: 500;
  }

  :deep(.el-progress__text) {
    font-size: 13px !important;
    font-weight: 500;
  }

  :deep(.el-rate__text) {
    font-size: 14px;
    font-weight: 500;
  }
}
</style>
