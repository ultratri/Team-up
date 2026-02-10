<template>
  <div class="need-confirm-container">
    <div class="need-confirm-topbar">
      <ThemeSwitcher />
    </div>

    <div class="need-confirm-card">
      <div class="need-confirm-header">
        <h2>请告诉我们您的需求</h2>
        <p>我们将根据您的选择提供更精准的引导</p>
      </div>
      
      <div class="options-container">
        <div 
          v-for="option in options" 
          :key="option.value"
          class="option-card"
          :class="{ 'is-selected': selectedOption === option.value }"
          @click="selectOption(option.value)"
        >
          <div class="option-icon">
            <el-icon :size="32">
              <component :is="option.icon" />
            </el-icon>
          </div>
          <h3>{{ option.title }}</h3>
          <p class="option-desc">{{ option.description }}</p>
        </div>
      </div>
      
      <div class="action-buttons">
        <el-button 
          type="primary" 
          :disabled="!selectedOption" 
          @click="confirmSelection"
          :loading="loading"
        >
          确认并继续
        </el-button>
        <el-button 
          type="text" 
          @click="skip"
          :disabled="loading"
        >
          暂时跳过
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { DocumentAdd, Search } from '@element-plus/icons-vue'
import ThemeSwitcher from '@/components/common/ThemeSwitcher.vue'
import { useAuthStore } from '@/store/auth'
import type { User } from '@/types/user'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const selectedOption = ref<string | null>(null)

const finalizeLoginAndGoProfile = async () => {
  const token = sessionStorage.getItem('pendingRegisterToken')
  const userRaw = sessionStorage.getItem('pendingRegisterUser')

  if (!token || !userRaw) {
    // 未找到待确认的注册信息：回到注册页重新开始
    await router.replace({ name: 'Register' })
    return
  }

  let user: User
  try {
    user = JSON.parse(userRaw) as User
  } catch {
    await router.replace({ name: 'Register' })
    return
  }

  authStore.setToken(token, true)
  authStore.setUser(user, true)

  sessionStorage.removeItem('pendingRegisterToken')
  sessionStorage.removeItem('pendingRegisterUser')

  await router.replace({ name: 'Profile' })
}


const options = [
  {
    value: 'create',
    title: '创建项目',
    description: '我有项目想法，需要招募团队成员',
    icon: DocumentAdd
  },
  {
    value: 'find',
    title: '寻找项目',
    description: '我想加入已有项目，积累经验',
    icon: Search
  }
]

const selectOption = (value: string) => {
  selectedOption.value = value
}

const savePreference = (value: string | null) => {
  // 保存到 localStorage，仅用于教程分流，不影响系统功能
  // 需求：只在创建账户时出现一次，所以这里写入后就视为“已完成选择/已跳过”
  if (value) {
    localStorage.setItem(
      'userProjectPreference',
      JSON.stringify({ value, decidedAt: Date.now() })
    )
  } else {
    localStorage.setItem(
      'userProjectPreference',
      JSON.stringify({ value: 'skipped', decidedAt: Date.now() })
    )
  }
}

const confirmSelection = async () => {
  if (!selectedOption.value) return

  loading.value = true

  // 保存用户选择
  savePreference(selectedOption.value)

  await finalizeLoginAndGoProfile()

  loading.value = false
}

const skip = async () => {
  loading.value = true

  savePreference(null)

  await finalizeLoginAndGoProfile()

  loading.value = false
}
</script>

<style scoped lang="scss">
.need-confirm-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--bg-body);
  transition: background-color 0.3s ease;
}

.need-confirm-topbar {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.need-confirm-card {
  width: 100%;
  max-width: 700px;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: var(--shadow-card);
  padding: 40px;
  text-align: center;
  border: 1px solid var(--border-card);
}

.need-confirm-header {
  margin-bottom: 32px;
  
  h2 {
    font-size: 28px;
    font-weight: 700;
    color: var(--text-color);
    margin: 0 0 8px;
  }
  
  p {
    font-size: 16px;
    color: var(--text-color-muted);
    margin: 0;
  }
}

.options-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.option-card {
  padding: 32px 24px;
  border: 2px solid var(--border-card);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--bg-elevated);
  text-align: center;
  
  &:hover {
    border-color: var(--el-color-primary);
    transform: translateY(-5px);
    box-shadow: var(--shadow-card);
  }
  
  &.is-selected {
    border-color: var(--el-color-primary);
    background-color: rgba(var(--accent-color-rgb), 0.12);
    
    .option-icon {
      color: var(--el-color-primary);
      background: rgba(var(--accent-color-rgb), 0.18);
    }
  }
}

.option-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--bg-elevated-soft);
  margin-bottom: 20px;
  color: var(--text-color-secondary);
  transition: all 0.3s ease;
}

.option-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
  margin: 0 0 12px;
}

.option-desc {
  font-size: 14px;
  color: var(--text-color-muted);
  margin: 0;
  line-height: 1.5;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  
  .el-button {
    min-width: 200px;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
    border-radius: 8px;
  }
  
  .el-button--text {
    color: #666;
    
    &:hover {
      color: var(--el-color-primary);
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .need-confirm-card {
    padding: 32px 20px;
  }
  
  .options-container {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .option-card {
    padding: 24px 16px;
  }
  
  .need-confirm-header h2 {
    font-size: 24px;
  }
}
</style>