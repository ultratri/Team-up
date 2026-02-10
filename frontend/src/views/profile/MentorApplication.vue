<template>
  <div class="mentor-application">
    <el-card>
      <template #header>
        <h2>申请成为导师</h2>
      </template>

      <!-- 申请状态 -->
      <el-alert
        v-if="myApplication"
        :type="getAlertType(myApplication.status)"
        :title="getStatusText(myApplication.status)"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template v-if="myApplication.status === 'PENDING'">
          <p>您的导师申请正在审核中，请耐心等待。</p>
          <p style="color: #999; font-size: 12px;">
            申请时间：{{ formatDate(myApplication.createdAt) }}
          </p>
        </template>
        <template v-else-if="myApplication.status === 'APPROVED'">
          <p>恭喜！您的导师申请已通过，现在您可以指导学员了。</p>
          <p style="color: #999; font-size: 12px;">
            审核时间：{{ formatDate(myApplication.reviewedAt) }}
          </p>
        </template>
        <template v-else-if="myApplication.status === 'REJECTED'">
          <p>很抱歉，您的导师申请未通过。</p>
          <p style="color: #999; font-size: 12px;">
            拒绝理由：{{ myApplication.rejectReason }}
          </p>
          <p style="color: #999; font-size: 12px;">
            审核时间：{{ formatDate(myApplication.reviewedAt) }}
          </p>
          <el-button 
            type="primary" 
            size="small" 
            @click="showApplicationForm = true"
            style="margin-top: 10px;"
          >
            重新申请
          </el-button>
        </template>
      </el-alert>

      <!-- 申请表单 -->
      <div v-if="!myApplication || (myApplication.status === 'REJECTED' && showApplicationForm)">
        <el-alert
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
        >
          <template #title>
            <strong>申请条件</strong>
          </template>
          <ul style="margin: 10px 0; padding-left: 20px;">
            <li>完成项目数 ≥ 3</li>
            <li>信誉分 ≥ 70</li>
            <li>有丰富的项目经验和技术能力</li>
            <li>愿意指导和帮助新人成长</li>
          </ul>
        </el-alert>

        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          label-width="100px"
        >
          <el-form-item label="申请理由" prop="reason" required>
            <el-input
              v-model="form.reason"
              type="textarea"
              :rows="8"
              placeholder="请详细说明您的项目经验、技术能力，以及为什么想成为导师（至少20个字）"
              maxlength="500"
              show-word-limit
            />
            <div style="margin-top: 5px; font-size: 12px; color: #999;">
              建议包含：项目经验、技术专长、指导经验、申请动机等
            </div>
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              @click="handleSubmit"
              :loading="submitting"
            >
              提交申请
            </el-button>
            <el-button @click="$router.back()">
              返回
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 导师权益说明 -->
      <el-card shadow="never" style="margin-top: 20px; background: #f5f7fa;">
        <template #header>
          <strong>成为导师的权益</strong>
        </template>
        <ul style="margin: 0; padding-left: 20px;">
          <li>✨ 获得"导师"专属徽章</li>
          <li>🎁 指导学员完成项目可获得奖励积分</li>
          <li>🏆 优秀导师将在排行榜中展示</li>
          <li>📈 提升个人影响力和技术声誉</li>
          <li>🤝 结识更多优秀的学员和同行</li>
        </ul>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { request } from '@/utils/request'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const showApplicationForm = ref(false)

const form = ref({
  reason: ''
})

const rules = {
  reason: [
    { required: true, message: '请输入申请理由', trigger: 'blur' },
    { min: 20, max: 500, message: '申请理由长度为20-500个字符', trigger: 'blur' }
  ]
}

interface MentorApplication {
  id: number
  reason: string
  status: string
  reviewedAt?: string
  rejectReason?: string
  createdAt: string
}

const myApplication = ref<MentorApplication | null>(null)

const loadMyApplication = async () => {
  try {
    const res = await request.get<any>('/mentor/application/my')
    if (res && typeof res === 'object') {
      if ('code' in res && res.code === 200) {
        myApplication.value = res.data
      } else if ('id' in res) {
        myApplication.value = res as MentorApplication
      }
    }
  } catch (error: any) {
    // 如果没有申请记录，不显示错误
    if (error.response?.status !== 404) {
      console.error('加载申请状态失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    await ElMessageBox.confirm(
      '确认提交导师申请吗？提交后将由管理员审核。',
      '确认提交',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    submitting.value = true

    const res = await request.post<any>('/mentor/application', form.value)

    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        ElMessage.success('申请提交成功，请等待管理员审核')
        showApplicationForm.value = false
        loadMyApplication()
      } else {
        throw new Error(res.message || '提交失败')
      }
    } else {
      ElMessage.success('申请提交成功，请等待管理员审核')
      showApplicationForm.value = false
      loadMyApplication()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

const getAlertType = (status: string) => {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'error'
    default:
      return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING':
      return '申请审核中'
    case 'APPROVED':
      return '申请已通过'
    case 'REJECTED':
      return '申请未通过'
    default:
      return '未知状态'
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadMyApplication()
})
</script>

<style scoped lang="scss">
.mentor-application {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 500;
  }

  ul {
    line-height: 1.8;
  }
}
</style>
