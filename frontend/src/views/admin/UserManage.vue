<template>
  <div class="user-manage">
    <div class="page-header">
      <div>
        <h1>用户管理</h1>
        <p class="subtitle">管理学生和导师账号</p>
      </div>
      <div style="display:flex; gap: 10px;">
        <el-button type="primary" @click="openCreate">新建用户</el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable @change="reload">
            <el-option label="全部" value="" />
            <el-option label="学生" value="STUDENT" />
            <el-option label="导师" value="MENTOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="reload">
            <el-option label="全部" value="" />
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
            <el-option label="封禁" value="BANNED" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="query.sortBy" placeholder="默认排序" clearable @change="reload" style="width: 200px;">
            <el-option label="创建时间↓" value="createdAt_desc" />
            <el-option label="创建时间↑" value="createdAt_asc" />
            <el-option label="最后登录↓" value="lastLogin_desc" />
            <el-option label="最后登录↑" value="lastLogin_asc" />
            <el-option label="角色（管理员优先）" value="role_desc" />
            <el-option label="角色（学生优先）" value="role_asc" />
            <el-option label="学号/工号↑" value="userCode_asc" />
            <el-option label="学号/工号↓" value="userCode_desc" />
            <el-option label="用户名↑" value="username_asc" />
            <el-option label="用户名↓" value="username_desc" />
            <el-option label="邮箱↑" value="email_asc" />
            <el-option label="邮箱↓" value="email_desc" />
            <el-option label="状态↑" value="status_asc" />
            <el-option label="状态↓" value="status_desc" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="学号/工号、用户名、邮箱"
            clearable
            @clear="reload"
            @keyup.enter="reload"
            style="width: 220px;"
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
        <el-table-column prop="userCode" label="学号/工号" min-width="120" sortable="custom" />
        <el-table-column prop="username" label="用户名" min-width="120" sortable="custom" />
        <el-table-column prop="email" label="邮箱" min-width="180" sortable="custom" />
        <el-table-column prop="phone" label="手机号" min-width="120" sortable="custom" />
        <el-table-column label="角色" width="120" sortable="custom" prop="role">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(getPrimaryRole(row.roles))">
              {{ getRoleText(getPrimaryRole(row.roles)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="180" sortable="custom" prop="lastLoginAt">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.lastLoginAt) || '从未登录' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180" sortable="custom" prop="createdAt">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="!isPlatformAdmin(row)">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
            <template v-else>
              <el-tag type="info" size="small">系统保护</el-tag>
            </template>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学号/工号" prop="userCode">
          <el-input v-model="form.userCode" :placeholder="form.role === 'MENTOR' ? '请输入工号' : '请输入学号'" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item :label="editingId ? '新密码' : '密码'" :prop="editingId ? '' : 'password'" :rules="passwordRules">
          <el-input 
            v-model="form.password" 
            type="password" 
            :placeholder="editingId ? '留空则不修改密码' : '请输入密码'"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号（可选）" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="学生" value="STUDENT" />
            <el-option label="导师" value="MENTOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="editingId">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
            <el-option label="封禁" value="BANNED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, type User, type UserCreateRequest, type UserUpdateRequest } from '@/api/user'

const route = useRoute()
const loading = ref(false)
const rows = ref<User[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  role: '',
  status: '',
  sortBy: 'createdAt_desc', // 默认按创建时间降序
  sortField: '',
  sortOrder: ''
})

const reload = async () => {
  loading.value = true
  try {
    // 只传递后端需要的参数
    const params: any = {
      page: query.page,
      size: query.size
    }
    
    // 只有非空值才添加到参数中
    if (query.keyword && query.keyword.trim()) {
      params.keyword = query.keyword.trim()
    }
    if (query.role) {
      params.role = query.role
    }
    if (query.status) {
      params.status = query.status
    }
    if (query.sortBy) {
      params.sortBy = query.sortBy
    }
    
    console.log('Sending request with params:', params)
    const res = await getUserList(params)
    rows.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.size = 10
  query.keyword = ''
  query.role = ''
  query.status = ''
  query.sortBy = 'createdAt_desc'
  query.sortField = ''
  query.sortOrder = ''
  reload()
}

// 处理表格排序变化
const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  if (!order) {
    // 取消排序，恢复默认
    query.sortBy = 'createdAt_desc'
    query.sortField = ''
    query.sortOrder = ''
  } else {
    // 设置排序
    const direction = order === 'ascending' ? 'asc' : 'desc'
    query.sortBy = `${prop}_${direction}`
    query.sortField = prop
    query.sortOrder = order
  }
  reload()
}

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑用户' : '新建用户'))
const formRef = ref<FormInstance>()

const form = reactive<any>({
  userCode: '',
  username: '',
  password: '',
  email: '',
  phone: '',
  role: 'STUDENT',
  status: 'ACTIVE'
})

const rules: FormRules = {
  userCode: [{ required: true, message: '请输入学号/工号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 密码验证规则（编辑时可选，新建时必填）
const passwordRules = computed(() => {
  if (editingId.value) {
    // 编辑模式：密码可选，但如果填写则需要符合长度要求
    return [
      { min: 6, max: 20, message: '密码长度在6-20之间', trigger: 'blur' }
    ]
  } else {
    // 新建模式：密码必填
    return [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度在6-20之间', trigger: 'blur' }
    ]
  }
})

const openCreate = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row: User) => {
  editingId.value = row.id
  form.userCode = row.userCode
  form.username = row.username
  form.email = row.email
  form.phone = row.phone || ''
  form.status = row.status
  form.password = ''
  // 获取主要角色（排除管理员角色）
  const mainRole = row.roles?.find(r => r === 'STUDENT' || r === 'MENTOR') || 'STUDENT'
  form.role = mainRole
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.clearValidate()
  form.userCode = ''
  form.username = ''
  form.password = ''
  form.email = ''
  form.phone = ''
  form.role = 'STUDENT'
  form.status = 'ACTIVE'
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      if (editingId.value) {
        const updateData: UserUpdateRequest = {
          userCode: form.userCode,
          username: form.username,
          email: form.email,
          phone: form.phone || undefined,
          status: form.status,
          role: form.role
        }
        if (form.password) {
          updateData.password = form.password
        }
        await updateUser(editingId.value, updateData)
      } else {
        const createData: UserCreateRequest = {
          userCode: form.userCode,
          username: form.username,
          password: form.password,
          email: form.email,
          phone: form.phone || undefined,
          role: form.role as 'STUDENT' | 'MENTOR'
        }
        await createUser(createData)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await reload()
    } catch (e: any) {
      ElMessage.error(e.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = async (row: User) => {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '确认删除', {
      type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await reload()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: 'success',
    INACTIVE: 'info',
    BANNED: 'danger'
  }
  return map[status] || 'info'
}

const statusText = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    INACTIVE: '禁用',
    BANNED: '封禁'
  }
  return map[status] || status
}

const getRoleTagType = (role: string) => {
  const map: Record<string, string> = {
    STUDENT: '',
    MENTOR: 'warning',
    PLATFORM_ADMIN: 'danger'
  }
  return map[role] || ''
}

const getRoleText = (role: string) => {
  const map: Record<string, string> = {
    STUDENT: '学生',
    MENTOR: '导师',
    PLATFORM_ADMIN: '平台管理员'
  }
  return map[role] || role
}

const formatDateTime = (v: any) => {
  if (!v) return '-'
  const d = new Date(v)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// 获取用户的主要角色（优先级：PLATFORM_ADMIN > MENTOR > STUDENT）
const getPrimaryRole = (roles: string[] | undefined): string => {
  if (!roles || roles.length === 0) return 'STUDENT'
  
  // 按优先级返回角色
  if (roles.includes('PLATFORM_ADMIN')) return 'PLATFORM_ADMIN'
  if (roles.includes('MENTOR')) return 'MENTOR'
  return 'STUDENT'
}

// 判断是否是平台管理员
const isPlatformAdmin = (user: User): boolean => {
  return user.roles?.includes('PLATFORM_ADMIN') || false
}

// 监听路由变化，切换模式时重新加载
watch(() => route.path, () => {
  reload()
}, { immediate: false })

onMounted(() => {
  reload()
})
</script>

<style scoped lang="scss">
.user-manage {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: 700;
    margin: 0 0 8px 0;
    color: var(--text-color);
  }

  .subtitle {
    color: var(--text-color-muted);
    font-size: 14px;
    margin: 0;
  }
}

.filter-card {
  margin-bottom: 16px;
}

.table-card {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
