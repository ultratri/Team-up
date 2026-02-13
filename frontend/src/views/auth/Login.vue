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
      <h1 class="auth-hero-title">连接校园灵感，<br>孵化无限可能。</h1>
      <p class="auth-hero-subtitle">
        不要让您的创意止步于想法。在这里，找到志同道合的伙伴，组建梦之队，将每一个灵感转化为现实。
      </p>
    </template>

    <template #form>
      <div class="auth-card">
        <div class="auth-header-text">
          <h2>账号登录</h2>
          <p>开启您的协作之旅</p>
        </div>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="auth-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="userCode" label="学号/工号">
            <el-input 
              v-model="loginForm.userCode" 
              placeholder="请输入学号或工号"
              :prefix-icon="User"
              autocomplete="username"
            />
          </el-form-item>
          
          <el-form-item prop="password" label="密码">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>

          <div class="flex-between mb-20">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <button type="button" class="auth-inline-link" @click="handleForgotPassword">忘记密码？</button>
          </div>

          <button 
            class="auth-submit-btn" 
            :class="{ 'is-loading': loading }"
            :disabled="loading"
            type="submit"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
          </button>
        </el-form>

        <div class="auth-footer-links">
          还没有账号？
          <button type="button" class="auth-inline-link" @click="handleGoRegister">立即注册</button>
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
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { useSystemStore } from '@/store/system'
import { login } from '@/api/auth'

// Components
import AuthLayout from './components/AuthLayout.vue'
import AuthBackground from './components/AuthBackground.vue'
import AuthBrand from './components/AuthBrand.vue'
import AuthFeatures from './components/AuthFeatures.vue'
import ThemeSwitcher from '@/components/common/ThemeSwitcher.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const systemStore = useSystemStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(localStorage.getItem('rememberMe') === 'true')

const loginForm = reactive({
  userCode: localStorage.getItem('rememberMe') === 'true' ? (localStorage.getItem('saveduserCode') || '') : '',
  password: ''
})

const rules = reactive<FormRules>({
  userCode: [
    { required: true, message: '请输入学号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
})

const handleLogin = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    console.log('🔐 开始登录...', loginForm.userCode)
    const res = await login(loginForm)
    console.log('✅ 登录响应:', res)
    
    if (!res?.token || !res?.user) {
      console.error('❌ 登录响应格式错误:', res)
      ElMessage.error('登录响应异常，请稍后重试')
      return
    }
    
    authStore.setToken(res.token, rememberMe.value)
    authStore.setUser(res.user, rememberMe.value)
    localStorage.setItem('rememberMe', rememberMe.value ? 'true' : 'false')
    if (rememberMe.value) {
      localStorage.setItem('saveduserCode', loginForm.userCode)
    } else {
      localStorage.removeItem('saveduserCode')
    }
    
    ElMessage.success('登录成功')
    console.log('🎉 登录成功，准备跳转...')
    
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    let target: string
    
    if (redirect && redirect.startsWith('/') && redirect !== '/login') {
      // 如果有重定向参数，使用重定向地址
      target = redirect
    } else {
      // 根据用户角色决定默认跳转页面
      const isAdmin = authStore.hasRole(['PLATFORM_ADMIN'])
      target = isAdmin ? '/admin' : '/ecosystem'  // 修改默认跳转到生态广场
    }
    
    console.log('🔀 跳转目标:', target)
    
    try {
      await router.replace(target)
      console.log('✅ 路由跳转成功')
    } catch (error) {
      console.error('❌ 路由跳转失败，尝试强制刷新:', error)
      window.location.assign(target)
    }
  } catch (error: any) {
    console.error('❌ 登录失败:', error)
    
    // 提取错误信息
    let errorMessage = '登录失败，请稍后重试'
    
    if (error.message) {
      // 如果是后端返回的业务错误（已经在拦截器中处理）
      errorMessage = error.message
    } else if (error.response?.data?.message) {
      // 备用：从响应中提取错误信息
      errorMessage = error.response.data.message
    }
    
    // 针对常见错误提供更友好的提示
    if (errorMessage.includes('密码')) {
      ElMessage({
        message: '❌ ' + errorMessage,
        type: 'error',
        duration: 4000,
        showClose: true,
        customClass: 'login-error-message'
      })
      // 清空密码输入框，让用户重新输入
      loginForm.password = ''
      // 聚焦到密码输入框
      setTimeout(() => {
        const passwordInput = document.querySelector('input[type="password"]') as HTMLInputElement
        if (passwordInput) {
          passwordInput.focus()
        }
      }, 100)
    } else if (errorMessage.includes('学号') || errorMessage.includes('工号') || errorMessage.includes('不存在')) {
      ElMessage({
        message: '❌ ' + errorMessage,
        type: 'error',
        duration: 4000,
        showClose: true,
        customClass: 'login-error-message'
      })
    } else if (errorMessage.includes('禁用') || errorMessage.includes('审核')) {
      ElMessage({
        message: '⚠️ ' + errorMessage,
        type: 'warning',
        duration: 5000,
        showClose: true,
        customClass: 'login-error-message'
      })
    } else if (!error.response) {
      // 网络错误已经在拦截器中处理，这里不再重复显示
      console.error('网络错误或请求被拦截')
    } else {
      // 其他错误
      ElMessage({
        message: errorMessage,
        type: 'error',
        duration: 3000,
        showClose: true
      })
    }
  } finally {
    loading.value = false
  }
}

const handleForgotPassword = () => {
  ElMessage.info('请联系管理员或导师协助重置密码。')
}

const handleGoRegister = () => {
  router.push('/register')
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
  
  /* Animate in */
  opacity: 0;
  animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
}

.auth-hero-subtitle {
  font-size: 22px;
  line-height: 1.5;
  color: var(--text-color-muted);
  max-width: 560px;
  margin-bottom: 0;
  
  /* Animate in */
  opacity: 0;
  animation: fadeInUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  animation-delay: 0.1s;
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

/* 登录错误提示样式 */
:deep(.login-error-message) {
  .el-message__content {
    font-size: 15px;
    font-weight: 500;
    line-height: 1.5;
  }
}

@media (max-width: 768px) {
  .auth-hero-title {
    font-size: 42px;
  }
  
  .auth-hero-subtitle {
    font-size: 18px;
  }
}
</style>
