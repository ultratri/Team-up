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
                <div class="setting-value">{{ userInfo.userCode || '-' }}</div>
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

        <!-- 组队意向设置 -->
        <el-card shadow="never" class="settings-card" style="margin-top: 20px">
          <template #header>
            <span class="card-title">组队意向设置</span>
          </template>
          <el-form :model="availabilityForm" label-width="140px" class="availability-form">
            <!-- 上墙开关 -->
            <el-form-item label="我正在寻找项目/队伍">
              <div class="switch-wrapper">
                <el-switch v-model="availabilityForm.isAvailable" />
                <span class="hint">勾选后会出现在人才墙，让项目发起人找到你</span>
              </div>
            </el-form-item>

            <!-- 组队意向（多选） -->
            <el-form-item label="组队意向" v-if="availabilityForm.isAvailable">
              <el-checkbox-group v-model="availabilityForm.intentions">
                <el-checkbox label="JOIN_PROJECT">寻找项目加入</el-checkbox>
                <el-checkbox label="FIND_TEAMMATES">寻找队友组队</el-checkbox>
                <el-checkbox label="FIND_MENTOR">寻找导师指导</el-checkbox>
                <el-checkbox label="HELP_NEWBIE">愿意帮助新手</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <!-- 可见范围 -->
            <el-form-item label="可见范围" v-if="availabilityForm.isAvailable">
              <el-radio-group v-model="availabilityForm.visibility">
                <el-radio label="PUBLIC">所有人可见</el-radio>
                <el-radio label="PROJECT_CREATOR">仅项目创建者可见</el-radio>
                <el-radio label="MENTOR">仅导师可见</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 可用时间段 -->
            <el-form-item label="可用时间段（可选）" v-if="availabilityForm.isAvailable">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="handleDateRangeChange"
                style="width: 100%"
              />
            </el-form-item>

            <!-- 每周可投入时间 -->
            <el-form-item label="每周可投入时间（可选）" v-if="availabilityForm.isAvailable">
              <el-input-number 
                v-model="availabilityForm.weeklyHours" 
                :min="0" 
                :max="168"
                placeholder="小时/周"
              />
              <span class="hint" style="margin-left: 10px">小时/周</span>
            </el-form-item>

            <!-- 补充说明 -->
            <el-form-item label="补充说明（可选）" v-if="availabilityForm.isAvailable">
              <el-input
                v-model="availabilityForm.notes"
                type="textarea"
                :rows="3"
                placeholder="例如：希望参与Web开发项目，有React经验"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <!-- 保存按钮 -->
            <el-form-item>
              <el-button type="primary" @click="handleSaveAvailability" :loading="savingAvailability">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：头像和安全提示 -->
      <el-col :span="8">
        <!-- 头像设置 -->
        <el-card shadow="never" class="avatar-card">
          <template #header>
            <span class="card-title">头像设置</span>
          </template>
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarChange"
              accept="image/*"
            >
              <div class="avatar-wrapper" v-loading="avatarUploading">
                <el-avatar :src="userInfo.avatar" :size="120">
                  {{ userInfo.username?.charAt(0) }}
                </el-avatar>
                <div class="avatar-overlay">
                  <el-icon><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </div>
            </el-upload>
            <div class="avatar-tips">
              推荐尺寸：200x200，最大2MB
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="tips-card" style="margin-top: 20px">
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
import { Warning, Lock, Message, Camera } from '@element-plus/icons-vue'
import { getUserById, updateUser, getUserAvailabilityById, updateUserAvailability, type UserAvailabilityRequest } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { request } from '@utils/request'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = ref<any>({})
const showUsernameDialog = ref(false)
const showEmailDialog = ref(false)
const showPhoneDialog = ref(false)
const showPasswordDialog = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const avatarUploading = ref(false)

// 组队意向相关状态
const savingAvailability = ref(false)
const dateRange = ref<[Date, Date] | null>(null)
const availabilityForm = reactive({
  isAvailable: false,
  intentions: [] as string[],
  visibility: 'PUBLIC',
  availableFrom: undefined as string | undefined,
  availableUntil: undefined as string | undefined,
  weeklyHours: undefined as number | undefined,
  notes: ''
})

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
    { type: 'email' as const, message: '请输入正确的邮箱格式', trigger: 'blur' }
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
  // 直接使用 store 中的用户数据
  if (authStore.user) {
    userInfo.value = {
      username: authStore.user.username || '',
      userCode: authStore.user.userCode || '',
      email: authStore.user.email || '',
      phone: authStore.user.phone || '',
      avatar: authStore.user.profile?.avatarUrl || ''
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

// 加载组队意向设置
const loadAvailability = async () => {
  try {
    const userId = authStore.user?.id
    if (!userId) {
      console.error('用户ID不存在')
      return
    }
    const data = await getUserAvailabilityById(userId)
    availabilityForm.isAvailable = data.isAvailable
    availabilityForm.intentions = data.intentions || []
    availabilityForm.visibility = data.visibility || 'PUBLIC'
    availabilityForm.weeklyHours = data.weeklyHours
    availabilityForm.notes = data.notes || ''
    
    // 处理日期范围
    if (data.availableFrom && data.availableUntil) {
      dateRange.value = [
        new Date(data.availableFrom),
        new Date(data.availableUntil)
      ]
      availabilityForm.availableFrom = data.availableFrom
      availabilityForm.availableUntil = data.availableUntil
    }
  } catch (error: any) {
    console.error('加载组队意向失败:', error)
    // 使用默认值，不显示错误消息
    availabilityForm.isAvailable = false
    availabilityForm.intentions = []
    availabilityForm.weeklyHours = 0
    availabilityForm.notes = ''
  }
}

// 处理日期范围变化
const handleDateRangeChange = (value: [Date, Date] | null) => {
  if (value) {
    availabilityForm.availableFrom = value[0].toISOString().split('T')[0]
    availabilityForm.availableUntil = value[1].toISOString().split('T')[0]
  } else {
    availabilityForm.availableFrom = undefined
    availabilityForm.availableUntil = undefined
  }
}

// 保存组队意向设置
const handleSaveAvailability = async () => {
  // 验证
  if (availabilityForm.isAvailable && availabilityForm.intentions.length === 0) {
    ElMessage.warning('请至少选择一个组队意向')
    return
  }
  
  savingAvailability.value = true
  try {
    const requestData: UserAvailabilityRequest = {
      isAvailable: availabilityForm.isAvailable,
      intentions: availabilityForm.intentions,
      visibility: availabilityForm.visibility,
      availableFrom: availabilityForm.availableFrom,
      availableUntil: availabilityForm.availableUntil,
      weeklyHours: availabilityForm.weeklyHours,
      notes: availabilityForm.notes
    }
    
    await updateUserAvailability(requestData)
    ElMessage.success('保存成功')
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    savingAvailability.value = false
  }
}

// 处理头像上传
const handleAvatarChange = async (uploadFile: any) => {
  const file = uploadFile.raw
  if (!file) return

  // 验证文件类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return
  }

  // 验证文件大小（2MB）
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }

  avatarUploading.value = true
  try {
    const res: any = await uploadAvatar(file)
    const avatarUrl = res?.url || res?.data?.url
    
    if (!avatarUrl) {
      throw new Error('上传失败，未返回图片地址')
    }

    const userId = authStore.user?.id
    if (!userId) {
      throw new Error('用户ID不存在')
    }

    // 调用更新资料接口
    await request.put(`/profile/${userId}`, { avatarUrl })
    
    // 立即更新本地状态
    userInfo.value.avatar = avatarUrl
    
    // 刷新 store 中的用户信息
    await authStore.refreshUserInfo()
    
    ElMessage.success('头像上传成功')
  } catch (error: any) {
    console.error('头像上传失败:', error)
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
  loadAvailability()
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

.availability-form {
  padding: 20px 0;

  .switch-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;

    .hint {
      font-size: 12px;
      color: var(--text-color-muted);
    }
  }

  .el-checkbox-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .el-radio-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .hint {
    font-size: 12px;
    color: var(--text-color-muted);
  }
}

.avatar-card {
  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 0;

    .avatar-uploader {
      cursor: pointer;

      .avatar-wrapper {
        position: relative;
        width: 120px;
        height: 120px;
        border-radius: 50%;
        overflow: hidden;

        &:hover .avatar-overlay {
          opacity: 1;
        }

        .avatar-overlay {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(0, 0, 0, 0.6);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: white;
          opacity: 0;
          transition: opacity 0.3s;
          font-size: 14px;

          .el-icon {
            font-size: 24px;
            margin-bottom: 4px;
          }
        }
      }
    }

    .avatar-tips {
      margin-top: 12px;
      font-size: 12px;
      color: var(--text-color-muted);
      text-align: center;
    }
  }
}
</style>
