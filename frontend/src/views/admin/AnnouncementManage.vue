<template>
  <div class="announcement-manage">
    <div class="page-header">
      <div>
        <h1>公告管理</h1>
        <p class="subtitle">发布系统公告，通知所有用户或指定用户</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        发布公告
      </el-button>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="标题/内容"
            clearable
            @clear="reload"
            @keyup.enter="reload"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="rows" v-loading="loading" style="width: 100%" @sort-change="handleSortChange">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="140" sortable="custom">
          <template #default="{ row }">
            <el-tag v-if="row.priority === 'HIGH'" type="danger">高</el-tag>
            <el-tag v-else-if="row.priority === 'MEDIUM'" type="warning">中</el-tag>
            <el-tag v-else type="info">低</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isActive" type="success">有效</el-tag>
            <el-tag v-else type="info">已失效</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="220" sortable="custom">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.publishedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </el-card>

    <!-- 发布公告对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="接收者" prop="receiverType">
          <el-radio-group v-model="form.receiverType">
            <el-radio label="ALL">所有用户</el-radio>
            <el-radio label="SPECIFIC">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="form.receiverType === 'SPECIFIC'"
          label="用户ID"
          prop="userIds"
        >
          <el-input
            v-model="userIdsInput"
            placeholder="请输入用户ID，多个用逗号分隔，如：1,2,3"
            @blur="parseUserIds"
          />
          <div class="form-hint">已选择 {{ form.userIds?.length || 0 }} 个用户</div>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="请选择优先级">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知类型" prop="notificationType">
          <el-select v-model="form.notificationType" placeholder="请选择通知类型">
            <el-option label="系统公告" value="SYSTEM_ANNOUNCEMENT" />
            <el-option label="比赛相关" value="COMPETITION_ANNOUNCEMENT" />
            <el-option label="项目相关" value="PROJECT_ANNOUNCEMENT" />
            <el-option label="团队公告" value="TEAM_ANNOUNCEMENT" />
            <el-option label="系统通知" value="SYSTEM_NOTIFICATION" />
          </el-select>
          <div class="form-hint">用户可以在通知中心按此类型筛选</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit" :loading="saving">发布</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createAnnouncement, getAnnouncements, deleteAnnouncement, type Announcement, type AnnouncementRequest } from '@/api/announcement'

const loading = ref(false)
const saving = ref(false)
const rows = ref<Announcement[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  sortBy: 'publishedAt_desc'  // 改为统一格式
})

const dialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const formRef = ref()
const userIdsInput = ref('')

const form = reactive<AnnouncementRequest>({
  title: '',
  content: '',
  receiverType: 'ALL',
  userIds: [],
  priority: 'MEDIUM',
  notificationType: 'SYSTEM_ANNOUNCEMENT'  // 默认为系统公告
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  receiverType: [{ required: true, message: '请选择接收者类型', trigger: 'change' }],
  userIds: [
    {
      validator: (rule: any, value: any, callback: any) => {
        if (form.receiverType === 'SPECIFIC' && (!value || value.length === 0)) {
          callback(new Error('请至少选择一个用户'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const parseUserIds = () => {
  if (!userIdsInput.value.trim()) {
    form.userIds = []
    return
  }
  const ids = userIdsInput.value
    .split(',')
    .map(id => id.trim())
    .filter(id => id)
    .map(id => parseInt(id))
    .filter(id => !isNaN(id))
  form.userIds = ids
}

const reload = async () => {
  loading.value = true
  try {
    const res = await getAnnouncements(query)
    rows.value = res.records
    total.value = res.total
  } catch (error: any) {
    console.error('加载公告列表失败:', error)
    ElMessage.error(error.message || '加载公告列表失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.keyword = ''
  query.page = 1
  query.sortBy = 'publishedAt_desc'
  reload()
}

// 处理表格排序变化
const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  if (!order) {
    // 取消排序，恢复默认
    query.sortBy = 'publishedAt_desc'
  } else {
    // 设置排序
    const direction = order === 'ascending' ? 'asc' : 'desc'
    query.sortBy = `${prop}_${direction}`
  }
  reload()
}

const openCreate = () => {
  dialogTitle.value = '发布公告'
  resetForm()
  dialogVisible.value = true
}

const resetForm = () => {
  form.title = ''
  form.content = ''
  form.receiverType = 'ALL'
  form.userIds = []
  form.priority = 'MEDIUM'
  form.notificationType = 'SYSTEM_ANNOUNCEMENT'
  userIdsInput.value = ''
  formRef.value?.clearValidate()
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    if (form.receiverType === 'SPECIFIC') {
      parseUserIds()
      if (!form.userIds || form.userIds.length === 0) {
        ElMessage.warning('请至少选择一个用户')
        return
      }
    }
    
    saving.value = true
    try {
      await createAnnouncement(form)
      ElMessage.success('公告发布成功')
      dialogVisible.value = false
      reload()
    } catch (error: any) {
      console.error('发布公告失败:', error)
      ElMessage.error(error.message || '发布公告失败')
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = async (row: Announcement) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？删除后所有用户将无法再看到此公告。', '确认删除', {
      type: 'warning'
    })
    
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    reload()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除公告失败:', error)
      ElMessage.error(error.message || '删除公告失败')
    }
  }
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  reload()
})
</script>

<style scoped lang="scss">
.announcement-manage {
  padding: 24px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    .subtitle {
      margin: 8px 0 0 0;
      color: var(--text-color-muted);
      font-size: 14px;
    }
  }

  .filter-card {
    margin-bottom: 20px;
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .form-hint {
    margin-top: 4px;
    font-size: 12px;
    color: var(--text-color-muted);
  }
}
</style>
