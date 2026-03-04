<template>
  <div class="mentor-applications-page">
    <div class="page-header">
      <h1 class="page-title">团队指导申请</h1>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadApplications">刷新</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true">
        <el-form-item label="状态筛选">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable @change="handleFilterChange" style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="PENDING" />
            <el-option label="已接受" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 申请列表 -->
    <div v-loading="loading" class="applications-container">
      <el-empty v-if="!loading && applications.length === 0" description="暂无申请" />
      
      <el-card
        v-for="app in applications"
        :key="app.id"
        class="application-card"
        shadow="hover"
      >
        <div class="application-header">
          <div class="team-info">
            <h3 class="team-name">{{ app.teamName || `团队 #${app.teamId}` }}</h3>
            <el-tag v-if="app.competitionName" type="info" size="small">
              {{ app.competitionName }}
            </el-tag>
          </div>
          <el-tag
            :type="getStatusType(app.status)"
            size="large"
          >
            {{ getStatusText(app.status) }}
          </el-tag>
        </div>

        <div class="application-body">
          <div class="info-row">
            <span class="label">申请人：</span>
            <span class="value">{{ app.requesterName || '未知' }}</span>
          </div>
          <div class="info-row">
            <span class="label">申请时间：</span>
            <span class="value">{{ formatTime(app.createdAt) }}</span>
          </div>
          <div v-if="app.reason" class="info-row">
            <span class="label">申请理由：</span>
            <span class="value">{{ app.reason }}</span>
          </div>
          <div v-if="app.decidedAt" class="info-row">
            <span class="label">处理时间：</span>
            <span class="value">{{ formatTime(app.decidedAt) }}</span>
          </div>
        </div>

        <div v-if="app.status === 'PENDING'" class="application-actions">
          <el-button type="success" @click="handleAccept(app)">
            接受申请
          </el-button>
          <el-button type="danger" @click="handleReject(app)">
            拒绝申请
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div v-if="total > size" class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadApplications"
        @size-change="loadApplications"
      />
    </div>

    <!-- 拒绝理由对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝申请"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝理由">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝理由（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="rejecting">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getMentorApplicationsList,
  acceptMentorApplication,
  rejectMentorApplication,
  type TeamMentorApplication
} from '@/api/mentor'

// 状态
const loading = ref(false)
const applications = ref<TeamMentorApplication[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')

// 拒绝对话框
const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const rejectForm = ref({
  applicationId: 0,
  reason: ''
})

// 加载申请列表
const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMentorApplicationsList({
      page: page.value,
      size: size.value,
      status: statusFilter.value || undefined
    })
    applications.value = res.records
    total.value = res.total
  } catch (error: any) {
    console.error('加载申请列表失败:', error)
    ElMessage.error(error.message || '加载申请列表失败')
  } finally {
    loading.value = false
  }
}

// 筛选变化
const handleFilterChange = () => {
  page.value = 1
  loadApplications()
}

// 接受申请
const handleAccept = async (app: TeamMentorApplication) => {
  try {
    await ElMessageBox.confirm(
      `确认接受团队"${app.teamName || app.teamId}"的导师申请吗？`,
      '确认接受',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await acceptMentorApplication(app.id)
    ElMessage.success('已接受申请')
    loadApplications()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('接受申请失败:', error)
      ElMessage.error(error.message || '接受申请失败')
    }
  }
}

// 拒绝申请
const handleReject = (app: TeamMentorApplication) => {
  rejectForm.value = {
    applicationId: app.id,
    reason: ''
  }
  rejectDialogVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  rejecting.value = true
  try {
    await rejectMentorApplication(
      rejectForm.value.applicationId,
      rejectForm.value.reason || undefined
    )
    ElMessage.success('已拒绝申请')
    rejectDialogVisible.value = false
    loadApplications()
  } catch (error: any) {
    console.error('拒绝申请失败:', error)
    ElMessage.error(error.message || '拒绝申请失败')
  } finally {
    rejecting.value = false
  }
}

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '待处理',
    APPROVED: '已接受',
    REJECTED: '已拒绝'
  }
  return textMap[status] || status
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped lang="scss">
.mentor-applications-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0;
    color: var(--text-color);
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.filter-card {
  margin-bottom: 24px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);

  :deep(.el-card__body) {
    padding: 16px;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.applications-container {
  min-height: 400px;
}

.application-card {
  margin-bottom: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);

  .team-info {
    flex: 1;

    .team-name {
      font-size: 18px;
      font-weight: 600;
      margin: 0 0 8px 0;
      color: var(--text-color);
    }
  }
}

.application-body {
  margin-bottom: 16px;

  .info-row {
    display: flex;
    margin-bottom: 8px;
    font-size: 14px;

    .label {
      color: var(--text-color-secondary);
      min-width: 80px;
      font-weight: 500;
    }

    .value {
      color: var(--text-color);
      flex: 1;
    }
  }
}

.application-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .mentor-applications-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .application-header {
    flex-direction: column;
    gap: 12px;
  }

  .application-actions {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>
