<template>
  <div class="department-major-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>院系专业管理</h3>
          <el-button type="primary" @click="handleAdd">添加院系专业</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe height="calc(100vh - 240px)">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="department" label="院系" min-width="200" />
        <el-table-column prop="major" label="专业" min-width="250" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="院系" prop="department">
          <el-input v-model="form.department" placeholder="请输入院系名称" />
        </el-form-item>

        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" placeholder="请输入专业名称" />
        </el-form-item>

        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>

        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getDepartmentMajorList,
  createDepartmentMajor,
  updateDepartmentMajor,
  deleteDepartmentMajor,
  type DepartmentMajor
} from '@/api/system'
import { clearDepartmentMajorCache } from '@/utils/departmentMajorPreset'

const tableData = ref<DepartmentMajor[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加院系专业')
const loading = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const form = reactive({
  id: 0,
  department: '',
  major: '',
  sortOrder: 0,
  enabled: true
})

const rules = {
  department: [{ required: true, message: '请输入院系名称', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

const fetchList = async () => {
  try {
    tableData.value = await getDepartmentMajorList()
  } catch (error) {
    console.error('获取列表失败:', error)
    ElMessage.error('获取列表失败')
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加院系专业'
  Object.assign(form, {
    id: 0,
    department: '',
    major: '',
    sortOrder: 0,
    enabled: true
  })
  dialogVisible.value = true
}

const handleEdit = (row: DepartmentMajor) => {
  isEdit.value = true
  dialogTitle.value = '编辑院系专业'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isEdit.value) {
      await updateDepartmentMajor(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createDepartmentMajor(form)
      ElMessage.success('添加成功')
    }
    
    // 清除缓存，让前端重新加载数据
    clearDepartmentMajorCache()
    
    dialogVisible.value = false
    await fetchList()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row: DepartmentMajor) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${row.department} - ${row.major} 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteDepartmentMajor(row.id)
    ElMessage.success('删除成功')
    
    // 清除缓存
    clearDepartmentMajorCache()
    
    await fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped lang="scss">
.department-major-manage {
  padding: 20px;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;

  .el-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    
    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 20px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
    }
  }
}
</style>
