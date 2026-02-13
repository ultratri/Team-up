<template>
  <div class="tag-manage">
    <el-card class="header-card">
      <div class="header-content">
        <h2>标签管理</h2>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          创建标签
        </el-button>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="标签分类">
          <el-select v-model="filterForm.category" placeholder="全部" clearable @change="handleFilter">
            <el-option label="技能标签" value="SKILL" />
            <el-option label="兴趣标签" value="INTEREST" />
            <el-option label="性格标签" value="PERSONALITY" />
            <el-option label="项目类型" value="PROJECT_TYPE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索标签名称"
            clearable
            @keyup.enter="handleFilter"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="handleViewStatistics">使用统计</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="tagList"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" width="150" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryTag(row.category)">
              {{ getCategoryName(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="使用次数" width="100">
          <template #default="{ row }">
            <el-tag type="info">{{ row.usageCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="官方标签" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isOfficial" type="success">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="warning"
              size="small"
              @click="handleMerge(row)"
            >
              合并
            </el-button>
            <el-button
              type="danger"
              size="small"
              :disabled="row.usageCount > 0"
              @click="handleDelete(row)"
            >
              删除
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

    <!-- 创建/编辑标签对话框 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="isEdit ? '编辑标签' : '创建标签'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="技能标签" value="SKILL" />
            <el-option label="兴趣标签" value="INTEREST" />
            <el-option label="性格标签" value="PERSONALITY" />
            <el-option label="项目类型" value="PROJECT_TYPE" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入标签描述"
          />
        </el-form-item>
        <el-form-item label="官方标签">
          <el-switch v-model="form.isOfficial" />
          <span class="form-tip">官方标签会在推荐时优先展示</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 合并标签对话框 -->
    <el-dialog
      v-model="mergeDialogVisible"
      title="合并标签"
      width="500px"
    >
      <el-alert
        title="提示"
        type="warning"
        :closable="false"
        style="margin-bottom: 20px"
      >
        合并后，源标签将被标记为已合并，所有使用源标签的记录将自动转移到目标标签
      </el-alert>
      <el-form
        ref="mergeFormRef"
        :model="mergeForm"
        :rules="mergeRules"
        label-width="100px"
      >
        <el-form-item label="源标签">
          <el-input :value="currentTag?.name" disabled />
        </el-form-item>
        <el-form-item label="目标标签" prop="targetTagId">
          <el-select
            v-model="mergeForm.targetTagId"
            placeholder="请选择目标标签"
            filterable
          >
            <el-option
              v-for="tag in similarTags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitMerge">确定</el-button>
      </template>
    </el-dialog>

    <!-- 使用统计对话框 -->
    <el-dialog
      v-model="statisticsDialogVisible"
      title="标签使用统计"
      width="800px"
    >
      <el-table :data="statisticsList" style="width: 100%">
        <el-table-column prop="name" label="标签名称" width="150" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryTag(row.category)">
              {{ getCategoryName(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="100" />
        <el-table-column prop="projectCount" label="项目数" width="100" />
        <el-table-column prop="totalUsage" label="总使用次数" width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

// 数据定义
const loading = ref(false)
const tagList = ref([])
const similarTags = ref([])
const statisticsList = ref([])

const filterForm = reactive({
  category: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const formDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null,
  name: '',
  category: '',
  description: '',
  isOfficial: false
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择标签分类', trigger: 'change' }]
}

const mergeDialogVisible = ref(false)
const currentTag = ref<any>(null)
const mergeFormRef = ref<FormInstance>()
const mergeForm = reactive({
  sourceTagId: null,
  targetTagId: null
})

const mergeRules: FormRules = {
  targetTagId: [{ required: true, message: '请选择目标标签', trigger: 'change' }]
}

const statisticsDialogVisible = ref(false)

// 方法
const fetchTagList = async () => {
  loading.value = true
  try {
    const { data } = await request.get('/admin/tags', {
      params: {
        page: pagination.page,
        size: pagination.size,
        category: filterForm.category || undefined,
        keyword: filterForm.keyword || undefined
      }
    })
    tagList.value = data.records
    pagination.total = data.total
  } catch (error) {
    ElMessage.error('获取标签列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  pagination.page = 1
  fetchTagList()
}

const handleReset = () => {
  filterForm.category = ''
  filterForm.keyword = ''
  handleFilter()
}

const handleSizeChange = () => {
  fetchTagList()
}

const handlePageChange = () => {
  fetchTagList()
}

const handleCreate = () => {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.category = ''
  form.description = ''
  form.isOfficial = false
  formDialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.category = row.category
  form.description = row.description
  form.isOfficial = row.isOfficial
  formDialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await request.put(`/admin/tags/${form.id}`, form)
          ElMessage.success('更新成功')
        } else {
          await request.post('/admin/tags', form)
          ElMessage.success('创建成功')
        }
        formDialogVisible.value = false
        fetchTagList()
      } catch (error) {
        ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
      }
    }
  })
}

const handleMerge = async (row: any) => {
  currentTag.value = row
  mergeForm.sourceTagId = row.id
  mergeForm.targetTagId = null
  
  // 获取相同分类的其他标签
  try {
    const { data } = await request.get('/admin/tags', {
      params: {
        page: 1,
        size: 100,
        category: row.category
      }
    })
    similarTags.value = data.records.filter((t: any) => t.id !== row.id)
    mergeDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取标签列表失败')
  }
}

const handleSubmitMerge = async () => {
  if (!mergeFormRef.value) return
  
  await mergeFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.post('/admin/tags/merge', mergeForm)
        ElMessage.success('合并成功')
        mergeDialogVisible.value = false
        fetchTagList()
      } catch (error) {
        ElMessage.error('合并失败')
      }
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除该标签吗？删除后无法恢复。',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.delete(`/admin/tags/${row.id}`)
      ElMessage.success('删除成功')
      fetchTagList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleViewStatistics = async () => {
  try {
    const { data } = await request.get('/admin/tags/statistics', {
      params: { limit: 50 }
    })
    statisticsList.value = data
    statisticsDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取统计信息失败')
  }
}

// 辅助方法
const getCategoryName = (category: string) => {
  const map: Record<string, string> = {
    SKILL: '技能标签',
    INTEREST: '兴趣标签',
    PERSONALITY: '性格标签',
    PROJECT_TYPE: '项目类型'
  }
  return map[category] || category
}

const getCategoryTag = (category: string) => {
  const map: Record<string, string> = {
    SKILL: 'primary',
    INTEREST: 'success',
    PERSONALITY: 'warning',
    PROJECT_TYPE: 'info'
  }
  return map[category] || ''
}

const getStatusName = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    DEPRECATED: '已废弃',
    MERGED: '已合并'
  }
  return map[status] || status
}

const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: 'success',
    DEPRECATED: 'info',
    MERGED: 'warning'
  }
  return map[status] || ''
}

// 生命周期
onMounted(() => {
  fetchTagList()
})
</script>

<style scoped lang="scss">
.tag-manage {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h2 {
        margin: 0;
      }
    }
  }

  .filter-card {
    margin-bottom: 20px;
  }

  .table-card {
    .el-pagination {
      margin-top: 20px;
      justify-content: flex-end;
    }
  }

  .form-tip {
    margin-left: 10px;
    font-size: 12px;
    color: #999;
  }
}
</style>
