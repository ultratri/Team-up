<template>
  <AuthLayout>
    <template #background>
      <AuthBackground />
    </template>

    <template #header>
      <div class="auth-header-content">
        <AuthBrand />
        <ThemeSwitcher />
      </div>
    </template>

    <template #hero>
      <h1 class="auth-hero-title">加入我们，<br>开启协作之旅。</h1>
      <p class="auth-hero-subtitle">
        数千名优秀的开发者正在等待与您组队。立即创建一个账户，让您的创意落地生根。
      </p>
    </template>

    <template #form>
      <div class="auth-card">
        <div class="auth-header-text">
          <h2>创建账号</h2>
          <p>填写以下信息完成注册</p>
        </div>

        <el-form
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          class="auth-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="studentId" :label="registerForm.role === 'MENTOR' ? '工号' : '学号'">
            <el-input 
              v-model="registerForm.studentId" 
              :placeholder="registerForm.role === 'MENTOR' ? '请输入工号' : '请输入学号'"
              :prefix-icon="Collection"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="username" label="用户名">
            <el-input 
              v-model="registerForm.username" 
              placeholder="请输入用户名"
              :prefix-icon="User"
              autocomplete="nickname"
            />
          </el-form-item>

          <el-form-item prop="email" label="邮箱地址">
            <el-input 
              v-model="registerForm.email" 
              placeholder="请输入邮箱地址"
              :prefix-icon="Message"
              autocomplete="email"
            />
          </el-form-item>

          <el-form-item prop="role" label="注册身份">
            <el-radio-group v-model="registerForm.role">
              <el-radio label="STUDENT">学生</el-radio>
              <el-radio label="MENTOR">导师</el-radio>
            </el-radio-group>
            <div class="role-hint" v-if="registerForm.role === 'MENTOR'">
              <el-alert
                type="warning"
                :closable="false"
                show-icon
                style="margin-top: 8px;"
              >
                <template #title>
                  <span style="font-size: 13px;">注册导师账号需要管理员审核，请确保您有导师资格</span>
                </template>
              </el-alert>
            </div>
          </el-form-item>
          
          <div class="form-row">
            <el-form-item prop="password" label="密码" class="half-width">
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                placeholder="设置密码"
                :prefix-icon="Lock"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>

            <el-form-item prop="confirmPassword" label="确认密码" class="half-width">
              <el-input 
                v-model="registerForm.confirmPassword" 
                type="password" 
                placeholder="确认密码"
                :prefix-icon="Lock"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
          </div>

          <el-form-item>
            <el-checkbox v-model="agreed">
              我已阅读并同意
              <button type="button" class="auth-inline-link" @click="handlePolicyClick('服务条款')">服务条款</button>
              和
              <button type="button" class="auth-inline-link" @click="handlePolicyClick('隐私政策')">隐私政策</button>
            </el-checkbox>
          </el-form-item>

          <button 
            class="auth-submit-btn" 
            :class="{ 'is-loading': loading }"
            :disabled="loading || !agreed"
            type="submit"
          >
            <span v-if="!loading">立即注册</span>
            <span v-else>注册中...</span>
          </button>
        </el-form>

        <div class="auth-footer-links">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </template>

    <template #features>
      <AuthFeatures />
    </template>

    <template #footer>
      <p>© 2024 {{ systemStore.siteName }}. All rights reserved.</p>
    </template>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Message, Collection } from '@element-plus/icons-vue'
import { useSystemStore } from '@/store/system'
import { register } from '@/api/auth'

// Components
import AuthLayout from './components/AuthLayout.vue'
import AuthBackground from './components/AuthBackground.vue'
import AuthBrand from './components/AuthBrand.vue'
import AuthFeatures from './components/AuthFeatures.vue'
import ThemeSwitcher from '@/components/common/ThemeSwitcher.vue'

const router = useRouter()
const systemStore = useSystemStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const agreed = ref(false)

const registerForm = reactive({
  studentId: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: 'STUDENT' as 'STUDENT' | 'MENTOR'
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = reactive<FormRules>({
  studentId: [
    { 
      required: true, 
      message: () => registerForm.role === 'MENTOR' ? '请输入工号' : '请输入学号', 
      trigger: 'blur' 
    }
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择注册身份', trigger: 'change' }],
})

const handleRegister = async () => {
  if (!formRef.value) return

  // 清理旧的选择，确保新注册用户能看到 need-confirm 页面
  localStorage.removeItem('userProjectPreference')

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 如果是注册导师，需要额外确认
  if (registerForm.role === 'MENTOR') {
    try {
      await ElMessageBox.confirm(
        '注册导师账号需要管理员审核，请确保您有导师资格。是否继续？',
        '确认注册导师账号',
        {
          type: 'warning',
          confirmButtonText: '确认注册',
          cancelButtonText: '取消'
        }
      )
    } catch {
      return // 用户取消
    }
  }

  loading.value = true
  try {
    // 只发送必要的字段，不发送 confirmPassword
    const { confirmPassword, ...registerData } = registerForm

    const loginResp = await register(registerData)

    // 导师账号注册成功后仍按原逻辑提示登录（需要审核）
    if (registerForm.role === 'MENTOR') {
      ElMessage.success('注册成功，等待管理员审核后即可登录')
    await router.replace('/login')
      return
    }

    // 暂存 token/user，待 need-confirm 确认后再真正写入 store（才算登录）
    sessionStorage.setItem('pendingRegisterToken', loginResp.token)
    sessionStorage.setItem('pendingRegisterUser', JSON.stringify(loginResp.user))

    ElMessage.success('注册成功，请完成需求确认后自动登录')

    await router.replace({ name: 'NeedConfirm' })
  } catch (error: any) {
    const msg = String(error?.message || '')

    if (msg.includes('邮箱已被使用')) {
      ElMessage.error('该邮箱已被使用，请更换邮箱或直接登录')
    } else if (msg.includes('用户名已存在')) {
      ElMessage.error('该用户名已被使用，请更换用户名')
    } else if (msg.includes('学号已存在') || msg.includes('工号已存在')) {
      ElMessage.error(msg)
    } else {
      ElMessage.error(msg || '注册失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handlePolicyClick = (label: string) => {
  ElMessage.info(`${label}暂未上线，可联系管理员获取详情。`)
}
</script>

<style scoped lang="scss">
@use './styles/auth-form.scss';

.auth-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.auth-hero-title {
  font-size: 72px;
  line-height: 1.05;
  font-weight: 800;
  letter-spacing: -0.03em;
  margin-bottom: 32px;
  background: linear-gradient(120deg, var(--text-color), var(--accent-color));
  -webkit-background-clip: text;
  color: transparent;
  
  opacity: 0;
  animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
}

.auth-hero-subtitle {
  font-size: 22px;
  line-height: 1.5;
  color: var(--text-color-muted);
  max-width: 560px;
  margin-bottom: 0;
  
  opacity: 0;
  animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  animation-delay: 0.1s;
}

.form-row {
  display: flex;
  gap: 20px;
}

.half-width {
  flex: 1;
}

/* Ensure half-width items handle the label alignment correctly */
.auth-form :deep(.half-width .el-form-item__label) {
  width: 100%;
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

@media (max-width: 768px) {
  .auth-hero-title {
    font-size: 42px;
  }
  
  .auth-hero-subtitle {
    font-size: 18px;
  }
  
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
