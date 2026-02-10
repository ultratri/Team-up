<template>
  <div class="system-settings">
    <div class="page-header">
      <h1>系统设置</h1>
      <p class="subtitle">管理系统基本配置和参数</p>
    </div>

    <el-card shadow="never" v-loading="loading">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本设置" name="basic">
          <el-form :model="basicSettings" label-width="150px" style="max-width: 800px">
            <el-form-item label="站点名称">
              <el-input v-model="basicSettings.siteName" placeholder="请输入站点名称" />
            </el-form-item>
            <el-form-item label="站点描述">
              <el-input
                v-model="basicSettings.siteDescription"
                type="textarea"
                :rows="3"
                placeholder="请输入站点描述"
              />
            </el-form-item>
            <el-form-item label="维护模式">
              <el-switch v-model="basicSettings.maintenanceMode" />
              <div class="form-hint">开启后，非管理员用户将无法访问系统</div>
            </el-form-item>
            <el-form-item label="允许注册">
              <el-switch v-model="basicSettings.allowRegistration" />
              <div class="form-hint">关闭后，新用户将无法注册账号</div>
            </el-form-item>
            <el-form-item label="注册审核">
              <el-switch v-model="basicSettings.requireRegistrationApproval" />
              <div class="form-hint">开启后，新用户注册需要管理员审核</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings" :loading="saving">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="通知设置" name="notification">
          <el-form :model="notificationSettings" label-width="150px" style="max-width: 800px">
            <el-form-item label="邮件通知">
              <el-switch v-model="notificationSettings.emailEnabled" />
              <div class="form-hint">是否启用邮件通知功能</div>
            </el-form-item>
            <el-form-item label="SMTP服务器" v-if="notificationSettings.emailEnabled">
              <el-input v-model="notificationSettings.smtpHost" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item label="SMTP端口" v-if="notificationSettings.emailEnabled">
              <el-input-number v-model="notificationSettings.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="发件人邮箱" v-if="notificationSettings.emailEnabled">
              <el-input v-model="notificationSettings.senderEmail" type="email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNotificationSettings" :loading="saving">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securitySettings" label-width="150px" style="max-width: 800px">
            <el-form-item label="密码最小长度">
              <el-input-number v-model="securitySettings.minPasswordLength" :min="6" :max="20" />
            </el-form-item>
            <el-form-item label="密码复杂度要求">
              <el-checkbox-group v-model="securitySettings.passwordRequirements">
                <el-checkbox label="uppercase">包含大写字母</el-checkbox>
                <el-checkbox label="lowercase">包含小写字母</el-checkbox>
                <el-checkbox label="number">包含数字</el-checkbox>
                <el-checkbox label="special">包含特殊字符</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="登录失败锁定">
              <el-switch v-model="securitySettings.loginLockEnabled" />
            </el-form-item>
            <el-form-item label="最大失败次数" v-if="securitySettings.loginLockEnabled">
              <el-input-number v-model="securitySettings.maxLoginAttempts" :min="3" :max="10" />
            </el-form-item>
            <el-form-item label="锁定时长（分钟）" v-if="securitySettings.loginLockEnabled">
              <el-input-number v-model="securitySettings.lockoutDuration" :min="5" :max="60" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecuritySettings" :loading="saving">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemSettings, saveSettings } from '@/api/system'

const activeTab = ref('basic')
const saving = ref(false)
const loading = ref(false)

const basicSettings = reactive({
  siteName: 'Team Up',
  siteDescription: '团队协作平台',
  maintenanceMode: false,
  allowRegistration: true,
  requireRegistrationApproval: false
})

const notificationSettings = reactive({
  emailEnabled: false,
  smtpHost: '',
  smtpPort: 587,
  senderEmail: ''
})

const securitySettings = reactive({
  minPasswordLength: 6,
  passwordRequirements: ['lowercase', 'number'],
  loginLockEnabled: false,
  maxLoginAttempts: 5,
  lockoutDuration: 15
})

const loadSettings = async () => {
  loading.value = true
  try {
    const data = await getSystemSettings()
    
    // 更新基本设置
    if (data.basic) {
      Object.assign(basicSettings, data.basic)
    }
    
    // 更新通知设置
    if (data.notification) {
      Object.assign(notificationSettings, data.notification)
    }
    
    // 更新安全设置
    if (data.security) {
      Object.assign(securitySettings, data.security)
    }
    
    console.log('✅ 系统设置加载成功:', data)
  } catch (error: any) {
    console.error('❌ 加载设置失败:', error)
    ElMessage.error(error.message || '加载设置失败')
  } finally {
    loading.value = false
  }
}

const saveBasicSettings = async () => {
  saving.value = true
  try {
    await saveSettings('basic', basicSettings)
    ElMessage.success('基本设置已保存')
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const saveNotificationSettings = async () => {
  saving.value = true
  try {
    await saveSettings('notification', notificationSettings)
    ElMessage.success('通知设置已保存')
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const saveSecuritySettings = async () => {
  saving.value = true
  try {
    await saveSettings('security', securitySettings)
    ElMessage.success('安全设置已保存')
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped lang="scss">
.system-settings {
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

  .form-hint {
    margin-top: 4px;
    font-size: 12px;
    color: var(--text-color-muted);
  }
}
</style>
