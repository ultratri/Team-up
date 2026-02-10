<template>
  <div class="mentor-applications">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>导师申请审核</h2>
        </div>
      </template>

      <el-table 
        :data="applications" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column label="申请人" min-width="100">
          <template #default="{ row }">
            <div style="font-weight: 500;">{{ row.applicant?.realName || '-' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.applicant?.username || '-' }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="院系/专业" min-width="180">
          <template #default="{ row }">
            <div>{{ row.applicant?.department || '-' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.applicant?.major || '-' }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="项目经验" min-width="140" align="center">
          <template #default="{ row }">
            <div>
              <el-tag type="success" size="small">
                完成: {{ row.applicant?.completedProjects || 0 }}
              </el-tag>
            </div>
            <div style="margin-top: 4px;">
              <el-tag type="warning" size="small">
                信誉: {{ row.applicant?.reputationScore || 0 }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="申请理由" min-width="250">
          <template #default="{ row }">
            <el-popover
              placement="top"
              :width="400"
              trigger="hover"
              :content="row.reason || '无'"
            >
              <template #reference>
                <div class="reason-text">{{ row.reason || '无' }}</div>
              </template>
            </el-popover>
          </template>
        </el-table-column>
        
        <el-table-column label="申请时间" min-width="160">
          <template #default="{ row }">
            <div style="font-size: 13px;">{{ formatDate(row.createdAt) }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              link 
              type="success" 
              size="small"
              @click="handleApprove(row)"
            >
              <el-icon><Check /></el-icon>
              通过
            </el-button>
            <el-button 
              link 
              type="danger" 
              size="small"
              @click="handleReject(row)"
            >
              <el-icon><Close /></el-icon>
              拒绝
            </el-button>
            <el-button 
              link 
              type="primary" 
              size="small"
              @click="viewApplicantDetail(row)"
            >
              <el-icon><View /></el-icon>
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadApplications"
        @current-change="loadApplications"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 拒绝理由对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝导师申请"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="申请人">
          {{ currentApplication?.applicant.realName }}
        </el-form-item>
        <el-form-item label="拒绝理由" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝理由"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button 
          type="danger" 
          @click="confirmReject"
          :loading="submitting"
        >
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 申请人详情对话框 -->
    <MentorDetailDialog 
      v-model="detailDialogVisible" 
      :mentor-id="selectedMentorId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, View } from '@element-plus/icons-vue'
import { 
  getMentorApplications, 
  approveMentorApplication,
  type MentorApplication 
} from '@/api/mentor'
import MentorDetailDialog from '@/components/admin/MentorDetailDialog.vue'

const applications = ref<MentorApplication[]>([])
const loading = ref(false)
const submitting = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const selectedMentorId = ref<number | null>(null)

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const rejectDialogVisible = ref(false)
const currentApplication = ref<MentorApplication | null>(null)
const rejectForm = ref({
  reason: ''
})

const loadApplications = async () => {
  loading.value = true
  try {
    const result = await getMentorApplications(pagination.value.page, pagination.value.size)
    applications.value = result.records
    pagination.value.total = result.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载申请列表失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (application: MentorApplication) => {
  try {
    await ElMessageBox.confirm(
      `确认通过 ${application.applicant.realName} 的导师申请吗？`,
      '确认通过',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await approveMentorApplication({
      applicationId: application.id,
      approved: true
    })

    ElMessage.success('审核通过')
    loadApplications()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

const handleReject = (application: MentorApplication) => {
  currentApplication.value = application
  rejectForm.value.reason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.value.reason.trim()) {
    ElMessage.warning('请输入拒绝理由')
    return
  }

  submitting.value = true
  try {
    await approveMentorApplication({
      applicationId: currentApplication.value!.id,
      approved: false,
      rejectReason: rejectForm.value.reason
    })

    ElMessage.success('已拒绝申请')
    rejectDialogVisible.value = false
    loadApplications()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const viewApplicantDetail = (application: MentorApplication) => {
  selectedMentorId.value = application.applicant.id
  detailDialogVisible.value = true
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped lang="scss">
.mentor-applications {
  padding: 20px;

  .card-header {
    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
    }
  }

  .reason-text {
    max-width: 250px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: pointer;
    color: #606266;
    
    &:hover {
      color: #409eff;
    }
  }

  :deep(.el-button + .el-button) {
    margin-left: 8px;
  }
}
</style>
