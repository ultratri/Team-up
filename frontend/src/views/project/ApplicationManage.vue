<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Clock, User, Select } from '@element-plus/icons-vue'
import {
  getMyProjectApplications,
  getMyApplications,
  reviewApplication,
  batchReviewApplications,
  withdrawApplication,
} from '@/api/project'
import type { ProjectApplication } from '@/types/project'

type Application = ProjectApplication & { selected?: boolean }

const loading = ref(false)
const applications = ref<Application[]>([])
const viewMode = ref<'received' | 'mine'>('received')
const activeTab = ref('PENDING')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const reviewDialogVisible = ref(false)
const currentApplication = ref<Application | null>(null)
const reviewForm = ref({
  approved: true,
  comment: ''
})

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  WITHDRAWN: { label: '已撤回', type: 'info' }
}

// 计算选中的申请
const selectedApplications = computed(() => 
  applications.value.filter(app => app.selected)
)

const selectedCount = computed(() => selectedApplications.value.length)

// 全选/取消全选
const selectAll = ref(false)
const handleSelectAll = () => {
  applications.value.forEach(app => {
    if (app.status === 'PENDING') {
      app.selected = selectAll.value
    }
  })
}

const loadApplications = async () => {
  loading.value = true
  try {
    const status = activeTab.value === 'ALL' ? undefined : activeTab.value
    const res =
      viewMode.value === 'received'
        ? await getMyProjectApplications(currentPage.value, pageSize.value, status)
        : await getMyApplications(currentPage.value, pageSize.value)

    applications.value = (res.data.records || []).map((app: any) => ({
      ...app,
      selected: false,
    }))
    total.value = res.data.total ?? 0
  } catch (error) {
    console.error('加载申请失败:', error)
    ElMessage.error('加载申请失败')
  } finally {
    loading.value = false
  }
}

const openReviewDialog = (application: Application, approved: boolean) => {
  currentApplication.value = application
  reviewForm.value = {
    approved,
    comment: ''
  }
  reviewDialogVisible.value = true
}

const handleReview = async () => {
  if (!currentApplication.value) return
  
  try {
    await reviewApplication(
      currentApplication.value.id,
      reviewForm.value.approved,
      reviewForm.value.comment
    )
    
    ElMessage.success(reviewForm.value.approved ? '已通过申请' : '已拒绝申请')
    reviewDialogVisible.value = false
    await loadApplications()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleBatchReview = async (approved: boolean) => {
  const selectedIds = selectedApplications.value.map(app => app.id)
  
  if (selectedIds.length === 0) {
    ElMessage.warning('请先选择要审核的申请')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要${approved ? '批量通过' : '批量拒绝'}这 ${selectedIds.length} 个申请吗？`,
      '确认操作',
      { 
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )
    
    await batchReviewApplications(selectedIds, approved, '')
    
    ElMessage.success('批量操作成功')
    selectAll.value = false
    await loadApplications()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadApplications()
}

const handleTabChange = () => {
  currentPage.value = 1
  selectAll.value = false
  loadApplications()
}

const handleViewModeChange = () => {
  currentPage.value = 1
  activeTab.value = 'PENDING'
  selectAll.value = false
  loadApplications()
}

const handleWithdraw = async (application: Application) => {
  try {
    await ElMessageBox.confirm('确定要撤回该申请吗？撤回后将无法恢复。', '确认撤回', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await withdrawApplication(application.id)
    ElMessage.success('已撤回申请')
    await loadApplications()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤回失败')
    }
  }
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="application-manage">
    <div class="page-header">
      <div class="header-title">
        <h1 class="page-title">申请管理</h1>
        <p class="page-subtitle">集中处理团队申请，快速推进项目协作</p>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="viewMode" size="small" @change="handleViewModeChange">
          <el-radio-button label="received">收到的申请</el-radio-button>
          <el-radio-button label="mine">我的申请</el-radio-button>
        </el-radio-group>

        <template v-if="viewMode === 'received'">
          <el-badge :value="selectedCount" :hidden="selectedCount === 0" class="badge-item">
            <el-button 
              type="success" 
              :disabled="selectedCount === 0"
              @click="handleBatchReview(true)"
            >
              批量通过 ({{ selectedCount }})
            </el-button>
          </el-badge>
          <el-badge :value="selectedCount" :hidden="selectedCount === 0" class="badge-item">
            <el-button 
              type="danger"
              :disabled="selectedCount === 0"
              @click="handleBatchReview(false)"
            >
              批量拒绝 ({{ selectedCount }})
            </el-button>
          </el-badge>
        </template>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待审核" name="PENDING" />
      <el-tab-pane label="已通过" name="APPROVED" />
      <el-tab-pane label="已拒绝" name="REJECTED" />
      <el-tab-pane label="全部" name="ALL" />
    </el-tabs>

    <!-- 批量选择 -->
    <div v-if="viewMode === 'received' && activeTab === 'PENDING' && applications.length > 0" class="batch-select">
      <el-checkbox v-model="selectAll" @change="handleSelectAll">
        全选
      </el-checkbox>
      <span class="select-hint">已选择 {{ selectedCount }} 项</span>
    </div>

    <div v-loading="loading" class="applications-list">
      <div
        v-for="app in applications"
        :key="app.id"
        class="application-card"
        :class="{ selected: app.selected }"
      >
        <!-- 选择框 -->
        <div v-if="viewMode === 'received' && app.status === 'PENDING'" class="card-checkbox">
          <el-checkbox v-model="app.selected" />
        </div>

        <div class="card-header">
          <div class="applicant-info">
            <el-avatar v-if="viewMode === 'received'" :size="48" :src="app.applicantAvatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="info-text">
              <h4 v-if="viewMode === 'received'">{{ app.applicantName || '匿名用户' }}</h4>
              <h4 v-else>{{ app.projectTitle || `项目 #${app.projectId}` }}</h4>
              <span class="project-title">
                {{ viewMode === 'received' ? `申请加入：${app.projectTitle || `项目 #${app.projectId}`}` : `申请时间：${formatDate(app.appliedAt)}` }}
              </span>
              <div v-if="viewMode === 'received' && app.applicantSkills && app.applicantSkills.length" class="skills">
                <el-tag
                  v-for="skill in app.applicantSkills.slice(0, 3)"
                  :key="skill"
                  size="small"
                  type="info"
                >
                  {{ skill }}
                </el-tag>
                <span v-if="app.applicantSkills.length > 3" class="more-skills">
                  +{{ app.applicantSkills.length - 3 }}
                </span>
              </div>
            </div>
          </div>
          <el-tag :type="statusMap[app.status].type" size="large">
            {{ statusMap[app.status].label }}
          </el-tag>
        </div>

        <div class="card-body">
          <div class="reason-section">
            <label>{{ viewMode === 'received' ? '申请理由：' : '我的理由：' }}</label>
            <p>{{ app.applicationReason || '无' }}</p>
          </div>

          <div class="meta-info">
            <span>
              <el-icon><Clock /></el-icon>
              申请时间：{{ formatDate(app.appliedAt) }}
            </span>
          </div>

          <div v-if="app.reviewedAt" class="review-info">
            <span>审核时间：{{ formatDate(app.reviewedAt) }}</span>
            <span v-if="app.reviewerName">审核人：{{ app.reviewerName }}</span>
            <span v-if="app.reviewComment">审核意见：{{ app.reviewComment }}</span>
          </div>
        </div>

        <div v-if="app.status === 'PENDING'" class="card-footer">
          <el-button
            v-if="viewMode === 'received'"
            type="success"
            :icon="Check"
            @click="openReviewDialog(app, true)"
          >
            通过
          </el-button>
          <el-button
            v-if="viewMode === 'received'"
            type="danger"
            :icon="Close"
            @click="openReviewDialog(app, false)"
          >
            拒绝
          </el-button>
          <el-button
            v-if="viewMode === 'mine'"
            type="warning"
            :icon="Close"
            @click="handleWithdraw(app)"
          >
            撤回申请
          </el-button>
        </div>
      </div>

      <div v-if="applications.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无申请记录" />
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      :title="reviewForm.approved ? '通过申请' : '拒绝申请'"
      width="500px"
    >
      <el-form :model="reviewForm" label-position="top">
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            :placeholder="reviewForm.approved ? '可选填写通过理由' : '请填写拒绝原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleReview"
        >
          确认{{ reviewForm.approved ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.application-manage {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 24px;
  animation: fadeInDown 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.header-title {
  .page-title {
    margin: 0 0 8px 0;
    font-size: 32px;
    font-weight: 800;
    background: linear-gradient(135deg, var(--text-color) 0%, var(--text-color-muted) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .page-subtitle {
    margin: 0;
    font-size: 16px;
    color: var(--text-color-muted);
  }
}

.header-actions {
  display: flex;
  gap: 12px;

  .badge-item {
    :deep(.el-badge__content) {
      background-color: var(--el-color-primary);
    }
  }
}

.batch-select {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 16px;

  .select-hint {
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

.applications-list {
  margin-top: 20px;
  min-height: 400px;
}

.application-card {
  position: relative;
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  padding-left: 60px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  border: 2px solid transparent;
  animation: fadeInUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) backwards;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  }

  &.selected {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.card-checkbox {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.applicant-info {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;

  .info-text {
    flex: 1;

    h4 {
      margin: 0 0 4px 0;
      font-size: 16px;
      font-weight: 600;
    }

    .project-title {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      display: block;
      margin-bottom: 8px;
    }

    .skills {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      align-items: center;
      margin-top: 8px;

      .more-skills {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}

.card-body {
  .reason-section {
    margin-bottom: 12px;

    label {
      font-weight: 600;
      color: var(--el-text-color-secondary);
      display: block;
      margin-bottom: 8px;
      font-size: 14px;
    }

    p {
      margin: 0;
      line-height: 1.6;
      color: var(--el-text-color-regular);
      font-size: 14px;
    }
  }

  .meta-info {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-bottom: 8px;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .review-info {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
    font-size: 13px;
    color: var(--el-text-color-secondary);

    span {
      display: block;
      margin-bottom: 4px;
    }
  }
}

.card-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

[data-theme='dark'] {
  .application-card {
    background: var(--el-bg-color-overlay);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);

    &:hover {
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
    }

    &.selected {
      background: rgba(64, 158, 255, 0.1);
    }
  }
}
</style>
