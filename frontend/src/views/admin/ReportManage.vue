<template>
  <div class="report-manage">
    <el-card class="header-card">
      <div class="header-content">
        <h2>举报管理</h2>
        <div class="statistics">
          <el-statistic
            v-for="stat in statistics"
            :key="stat.label"
            :title="stat.label"
            :value="stat.value"
            :prefix="stat.prefix"
          />
        </div>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="举报状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable @change="handleFilter">
            <el-option label="待处理" value="PENDING" />
            <el-option label="审核中" value="REVIEWING" />
            <el-option label="已处理" value="RESOLVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="filterForm.targetType" placeholder="全部" clearable @change="handleFilter">
            <el-option label="项目" value="PROJECT" />
            <el-option label="团队" value="TEAM" />
            <el-option label="用户" value="USER" />
            <el-option label="评论" value="COMMENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="reportList"
        style="width: 100%"
      >
        <el-table-column prop="id" label="举报ID" width="80" />
        <el-table-column prop="reporterName" label="举报人" width="120" />
        <el-table-column label="举报目标" width="150">
          <template #default="{ row }">
            <el-tag :type="getTargetTypeTag(row.targetType)">
              {{ getTargetTypeName(row.targetType) }}
            </el-tag>
            <div class="target-name">{{ row.targetName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="举报原因" width="120">
          <template #default="{ row }">
            <el-tag>{{ getReasonName(row.reason) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="举报时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleView(row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'REVIEWING'"
              type="success"
              size="small"
              @click="handleProcess(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 举报详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="举报详情"
      width="800px"
    >
      <el-descriptions v-if="currentReport" :column="2" border>
        <el-descriptions-item label="举报ID">{{ currentReport.id }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ currentReport.reporterName }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">
          <el-tag :type="getTargetTypeTag(currentReport.targetType)">
            {{ getTargetTypeName(currentReport.targetType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标名称">{{ currentReport.targetName }}</el-descriptions-item>
        <el-descriptions-item label="举报原因">
          <el-tag>{{ getReasonName(currentReport.reason) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(currentReport.status)">
            {{ getStatusName(currentReport.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="详细描述" :span="2">
          {{ currentReport.description }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport.evidenceUrls && currentReport.evidenceUrls.length" label="证据" :span="2">
          <div class="evidence-list">
            <el-image
              v-for="(url, index) in currentReport.evidenceUrls"
              :key="index"
              :src="url"
              :preview-src-list="currentReport.evidenceUrls"
              fit="cover"
              style="width: 100px; height: 100px; margin-right: 10px"
            />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="举报时间" :span="2">
          {{ currentReport.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handlerName" label="处理人">
          {{ currentReport.handlerName }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handledAt" label="处理时间">
          {{ currentReport.handledAt }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handleResult" label="处理结果" :span="2">
          {{ currentReport.handleResult }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 处理举报对话框 -->
    <el-dialog
      v-model="processDialogVisible"
      title="处理举报"
      width="600px"
    >
      <el-form
        ref="processFormRef"
        :model="processForm"
        :rules="processRules"
        label-width="120px"
      >
        <el-form-item label="处理状态" prop="status">
          <el-radio-group v-model="processForm.status">
            <el-radio label="RESOLVED">确认违规</el-radio>
            <el-radio label="REJECTED">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理结果" prop="handleResult">
          <el-input
            v-model="processForm.handleResult"
            type="textarea"
            :rows="4"
            placeholder="请输入处理结果说明"
          />
        </el-form-item>
        <el-form-item v-if="processForm.status === 'RESOLVED'" label="是否惩罚">
          <el-switch v-model="processForm.punishTarget" />
        </el-form-item>
        <el-form-item v-if="processForm.punishTarget" label="惩罚类型" prop="punishmentType">
          <el-select v-model="processForm.punishmentType" placeholder="请选择惩罚类型">
            <el-option label="封禁用户" value="BAN_USER" />
            <el-option label="删除内容" value="DELETE_CONTENT" />
            <el-option label="扣除信誉分" value="DEDUCT_CREDIT" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="processForm.punishmentType === 'BAN_USER'" label="封禁天数" prop="punishmentDays">
          <el-input-number v-model="processForm.punishmentDays" :min="1" :max="365" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitProcess">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

// 数据定义
const loading = ref(false)
const reportList = ref([])
const statistics = ref([
  { label: '待处理', value: 0, prefix: '📋' },
  { label: '审核中', value: 0, prefix: '🔍' },
  { label: '已处理', value: 0, prefix: '✅' },
  { label: '已驳回', value: 0, prefix: '❌' }
])

const filterForm = reactive({
  status: '',
  targetType: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const detailDialogVisible = ref(false)
const currentReport = ref<any>(null)

const processDialogVisible = ref(false)
const processFormRef = ref<FormInstance>()
const processForm = reactive({
  reportId: null,
  status: 'RESOLVED',
  handleResult: '',
  punishTarget: false,
  punishmentType: '',
  punishmentDays: 7
})

const processRules: FormRules = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  handleResult: [{ required: true, message: '请输入处理结果', trigger: 'blur' }],
  punishmentType: [{ required: true, message: '请选择惩罚类型', trigger: 'change' }],
  punishmentDays: [{ required: true, message: '请输入封禁天数', trigger: 'blur' }]
}

// 方法
const fetchReportList = async () => {
  loading.value = true
  try {
    const { data } = await request.get('/api/admin/reports', {
      params: {
        page: pagination.page,
        size: pagination.size,
        status: filterForm.status || undefined,
        targetType: filterForm.targetType || undefined
      }
    })
    reportList.value = data.records
    pagination.total = data.total
  } catch (error) {
    ElMessage.error('获取举报列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const { data } = await request.get('/api/admin/reports/statistics')
    const statusMap = {
      PENDING: 0,
      REVIEWING: 1,
      RESOLVED: 2,
      REJECTED: 3
    }
    data.forEach((item: any) => {
      const index = statusMap[item.status]
      if (index !== undefined) {
        statistics.value[index].value = item.count
      }
    })
  } catch (error) {
    console.error('获取统计信息失败', error)
  }
}

const handleFilter = () => {
  pagination.page = 1
  fetchReportList()
}

const handleReset = () => {
  filterForm.status = ''
  filterForm.targetType = ''
  handleFilter()
}

const handleSizeChange = () => {
  fetchReportList()
}

const handlePageChange = () => {
  fetchReportList()
}

const handleView = async (row: any) => {
  try {
    const { data } = await request.get(`/api/admin/reports/${row.id}`)
    currentReport.value = data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取举报详情失败')
  }
}

const handleProcess = (row: any) => {
  processForm.reportId = row.id
  processForm.status = 'RESOLVED'
  processForm.handleResult = ''
  processForm.punishTarget = false
  processForm.punishmentType = ''
  processForm.punishmentDays = 7
  processDialogVisible.value = true
}

const handleSubmitProcess = async () => {
  if (!processFormRef.value) return
  
  await processFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.post('/api/admin/reports/handle', processForm)
        ElMessage.success('处理成功')
        processDialogVisible.value = false
        fetchReportList()
        fetchStatistics()
      } catch (error) {
        ElMessage.error('处理失败')
      }
    }
  })
}

// 辅助方法
const getTargetTypeName = (type: string) => {
  const map: Record<string, string> = {
    PROJECT: '项目',
    TEAM: '团队',
    USER: '用户',
    COMMENT: '评论'
  }
  return map[type] || type
}

const getTargetTypeTag = (type: string) => {
  const map: Record<string, string> = {
    PROJECT: 'primary',
    TEAM: 'success',
    USER: 'warning',
    COMMENT: 'info'
  }
  return map[type] || ''
}

const getReasonName = (reason: string) => {
  const map: Record<string, string> = {
    SPAM: '垃圾信息',
    FRAUD: '诈骗',
    INAPPROPRIATE: '不当内容',
    HARASSMENT: '骚扰',
    OTHER: '其他'
  }
  return map[reason] || reason
}

const getStatusName = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待处理',
    REVIEWING: '审核中',
    RESOLVED: '已处理',
    REJECTED: '已驳回'
  }
  return map[status] || status
}

const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    REVIEWING: 'primary',
    RESOLVED: 'success',
    REJECTED: 'info'
  }
  return map[status] || ''
}

// 生命周期
onMounted(() => {
  fetchReportList()
  fetchStatistics()
})
</script>

<style scoped lang="scss">
.report-manage {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

    .header-content {
      h2 {
        margin-bottom: 20px;
      }

      .statistics {
        display: flex;
        gap: 40px;
      }
    }
  }

  .filter-card {
    margin-bottom: 20px;
  }

  .table-card {
    .target-name {
      margin-top: 5px;
      font-size: 12px;
      color: #666;
    }

    .el-pagination {
      margin-top: 20px;
      justify-content: flex-end;
    }
  }

  .evidence-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
