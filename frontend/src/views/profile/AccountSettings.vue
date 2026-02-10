<template>
  <div class="account-settings">
    <div class="page-header">
      <h1>账户设置</h1>
      <p class="subtitle">管理您的账户安全和联系方式</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <!-- 基本信息 -->
        <el-card shadow="never" class="settings-card">
          <template #header>
            <span class="card-title">基本信息</span>
          </template>
          <div class="settings-list">
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-label">用户名</div>
                <div class="setting-value">{{ userInfo.username || '-' }}</div>
              </div>
              <el-button type="primary" text @click="showUsernameDialog = true">修改</el-button>
            </div>
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-label">学号/工号</div>
                <div class="setting-value">{{ userInfo.studentId || '-' }}</div>
                <div class="setting-desc">学号/工号不可修改</div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 联系方式 -->
        <el-card shadow="never" class="settings-card" style="margin-top: 20px">
          <template #header>
            <span class="card-title">联系方式</span>
          </template>
          <div class="settings-list">
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-label">邮箱</div>
                <div class="setting-value">{{ userInfo.email || '未设置' }}</div>
                <div class="setting-desc">用于接收系统通知和找回密码</div>
              </div>
              <el-button type="primary" text @click="showEmailDialog = true">
                {{ userInfo.email ? '修改' : '绑定' }}
              </el-button>
            </div>
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-label">手机号</div>
                <div class="setting-value">{{ userInfo.phone || '未设置' }}</div>
                <div class="setting-desc">用于接收重要通知</div>
              </div>
              <el-button type="primary" text @click="showPhoneDialog = true">
                {{ userInfo.phone ? '修改' : '绑定' }}
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 安全设置 -->
        <el-card shadow="never" class="settings-card" style="margin-top: 20px">
          <template #header>
            <span class="card-title">安全设置</span>
          </template>
          <div class="settings-list">
            <div class="setting-item">
              <div class="setting-info">
                <div class="setting-label">登录密码</div>
                <div class="setting-value">••••••••</div>
                <div class="setting-desc">定期更换密码可以保护账户安全</div>
              </div>
              <el-button type="primary" text @click="showPasswordDialog = true">修改密码</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：安全提示 -->
      <el-col :span="8">
        <el-card shadow="never" class="tips-card">
          <template #header>
            <span class="card-title">安全提示</span>
          </template>
          <div class="tips-list">
            <div class="tip-item">
              <el-icon class="tip-icon"><Warning /></el-icon>
              <div class="tip-content">
                <div class="tip-title">定期修改密码</div>
                <div class="tip-desc">建议每3个月更换一次密码</div>
              </div>
            </div>
            <div class="tip-item">
              <el-icon class="tip-icon"><Lock /></el-icon>
              <div class="tip-content">
                <div class="tip-title">使用强密码</div>
                <div class="tip-desc">密码应包含字母、数字和符号</div>
              </div>
            </div>
            <div class="tip-item">
              <el-icon class="tip-icon"><Message /></el-icon>
              <div class="tip-content">
                <div class="tip-title">绑定联系方式</div>
                <div class="tip-desc">便于接收通知和找回密码</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改用户名对话框 -->
    <el-dialog v-model="showUsernameDialog" title="修改用户名" width="500px">
      <el-form :model="usernameForm" :rules="usernameRules" ref="usernameFormRef" label-width="100px">
        <el-form-item label="新用户名" prop="username">
          <el-input v-model="usernameForm.username" placeholder="请输入新用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUsernameDialog = false">取消</el-button>
        <el-button type="primary" @click="updateUsername" :loading="saving">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 修改邮箱对话框 -->
    <el-dialog v-model="showEmailDialog" title="修改邮箱" width="500px">
      <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="100px">
        <el-form-item label="新邮箱" prop="email">
          <el-input v-model="emailForm.email" type="email" placeholder="请输入新邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" @click="updateEmail" :loading="saving">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 修改手机号对话框 -->
    <el-dialog v-model="showPhoneDialog" title="修改手机号" width="500px">
      <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef" label-width="100px">
        <el-form-item label="新手机号" prop="phone">
          <el-input v-model="phoneForm.phone" placeholder="请输入新手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button type="primary" @click="updatePhone" :loading="saving">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword" :loading="changingPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import { Warning, Lock, Message } from '@element-plus/icons-vue'
import { getUserById, updateUser } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = ref<any>({})
const showUsernameDialog = ref(false)
const showEmailDialog = ref(false)
const showPhoneDialog = ref(false)
const showPasswordDialog = ref(false)
const saving = ref(false)
const changingPassword = ref(false)

const usernameFormRef = ref()
const emailFormRef = ref()
const phoneFormRef = ref()
const passwordFormRef = ref()

const usernameForm = reactive({
  username: ''
})

const emailForm = reactive({
  email: ''
})

const phoneForm = reactive({
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const usernameRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度在2-50个字符', trigger: 'blur' }
  ]
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadUserInfo = async () => {
  if (!authStore.user?.id) {
    // 如果没有用户ID，使用 store 中的数据
    userInfo.value = {
      username: authStore.user?.username || '',
      studentId: authStore.user?.studentId || '',
      email: authStore.user?.email || '',
      phone: authStore.user?.phone || ''
    }
    return
  }
  
  try {
    const data = await getUserById(authStore.user.id)
    userInfo.value = {
      username: data.username || authStore.user.username || '',
      studentId: data.studentId || authStore.user.studentId || '',
      email: data.email || authStore.user.email || '',
      phone: data.phone || authStore.user.phone || ''
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    // 失败时使用 store 中的数据
    userInfo.value = {
      username: authStore.user?.username || '',
      studentId: authStore.user?.studentId || '',
      email: authStore.user?.email || '',
      phone: authStore.user?.phone || ''
    }
  }
}

const updateUsername = async () => {
  if (!usernameFormRef.value) return
  await usernameFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    saving.value = true
    try {
      await updateUser(authStore.user!.id, { username: usernameForm.username })
      ElMessage.success('用户名修改成功')
      showUsernameDialog.value = false
      await loadUserInfo()
      // 更新store
      if (authStore.user) {
        authStore.user.username = usernameForm.username
      }
    } catch (error: any) {
      console.error('修改失败:', error)
      ElMessage.error(error.message || '修改失败')
    } finally {
      saving.value = false
    }
  })
}

const updateEmail = async () => {
  if (!emailFormRef.value) return
  await emailFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    saving.value = true
    try {
      await updateUser(authStore.user!.id, { email: emailForm.email })
      ElMessage.success('邮箱修改成功')
      showEmailDialog.value = false
      await loadUserInfo()
      // 更新store
      if (authStore.user) {
        authStore.user.email = emailForm.email
      }
    } catch (error: any) {
      console.error('修改失败:', error)
      ElMessage.error(error.message || '修改失败')
    } finally {
      saving.value = false
    }
  })
}

const updatePhone = async () => {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    saving.value = true
    try {
      await updateUser(authStore.user!.id, { phone: phoneForm.phone })
      ElMessage.success('手机号修改成功')
      showPhoneDialog.value = false
      await loadUserInfo()
      // 更新store
      if (authStore.user) {
        authStore.user.phone = phoneForm.phone
      }
    } catch (error: any) {
      console.error('修改失败:', error)
      ElMessage.error(error.message || '修改失败')
    } finally {
      saving.value = false
    }
  })
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    changingPassword.value = true
    try {
      // TODO: 调用修改密码API
      ElMessage.success('密码修改成功，请重新登录')
      showPasswordDialog.value = false
      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      // 登出
      setTimeout(() => {
        authStore.logout()
        router.push('/login')
      }, 1500)
    } catch (error: any) {
      console.error('修改密码失败:', error)
      ElMessage.error(error.message || '修改密码失败')
    } finally {
      changingPassword.value = false
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped lang="scss">
.account-settings {
  padding: 24px;

  .page-header {
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

  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .settings-list {
    .setting-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 0;
      border-bottom: 1px solid var(--border-subtle);

      &:last-child {
        border-bottom: none;
      }

      .setting-info {
        flex: 1;

        .setting-label {
          font-weight: 500;
          margin-bottom: 4px;
          font-size: 14px;
        }

        .setting-value {
          color: var(--text-color);
          margin-bottom: 4px;
          font-size: 14px;
        }

        .setting-desc {
          font-size: 12px;
          color: var(--text-color-muted);
        }
      }
    }
  }

  .tips-list {
    .tip-item {
      display: flex;
      align-items: flex-start;
      padding: 16px 0;
      border-bottom: 1px solid var(--border-subtle);

      &:last-child {
        border-bottom: none;
      }

      .tip-icon {
        font-size: 20px;
        color: var(--warning-color);
        margin-right: 12px;
        margin-top: 2px;
      }

      .tip-content {
        flex: 1;

        .tip-title {
          font-weight: 500;
          margin-bottom: 4px;
          font-size: 14px;
        }

        .tip-desc {
          font-size: 12px;
          color: var(--text-color-muted);
        }
      }
    }
  }
}
</style>
