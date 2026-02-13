<template>
  <div class="admin-profile">
    <div class="page-header">
      <h1>账户管理</h1>
      <p class="subtitle">管理您的管理员账户</p>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <!-- 左侧：账户信息 -->
      <el-col :span="16">
        <el-card shadow="never" class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">账户信息</span>
            </div>
          </template>
          <div class="info-list" v-if="userInfo.id">
            <div class="info-item">
              <span class="label">用户名</span>
              <span class="value">{{ userInfo.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">学号/工号</span>
              <span class="value">{{ userInfo.userCode || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">邮箱</span>
              <span class="value">{{ userInfo.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="label">电话</span>
              <span class="value">{{ userInfo.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="label">角色</span>
              <div class="value">
                <template v-if="userInfo.roles && userInfo.roles.length > 0">
                  <el-tag v-for="role in userInfo.roles" :key="role" :type="getRoleTagType(role)" style="margin-right: 8px">
                    {{ getRoleText(role) }}
                  </el-tag>
                </template>
                <span v-else class="text-muted">-</span>
              </div>
            </div>
            <div class="info-item">
              <span class="label">账户状态</span>
              <el-tag :type="userInfo.status === 'ACTIVE' ? 'success' : 'danger'">
                {{ userInfo.status === 'ACTIVE' ? '活跃' : userInfo.status || '未知' }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">注册时间</span>
              <span class="value">{{ formatDateTime(userInfo.createdAt) }}</span>
            </div>
            <div class="info-item" v-if="userInfo.lastLoginAt">
              <span class="label">最后登录</span>
              <span class="value">{{ formatDateTime(userInfo.lastLoginAt) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="100" />
        </el-card>

        <el-card shadow="never" class="security-card" style="margin-top: 20px">
          <template #header>
            <span class="card-title">账户设置</span>
          </template>
          <div class="security-list">
            <div class="security-item">
              <div class="security-info">
                <div class="security-title">修改用户名、密码、联系方式</div>
                <div class="security-desc">在账户设置页面管理您的账户信息</div>
              </div>
              <el-button type="primary" @click="goToSettings">前往设置</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：操作统计 -->
      <el-col :span="8">
        <el-card shadow="never" class="stats-card">
          <template #header>
            <span class="card-title">操作统计</span>
          </template>
          <div class="stats-list">
            <div class="stat-item">
              <div class="stat-label">今日操作</div>
              <div class="stat-value">{{ todayActions }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">本周操作</div>
              <div class="stat-value">{{ weekActions }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">本月操作</div>
              <div class="stat-value">{{ monthActions }}</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="tips-card" style="margin-top: 20px">
          <template #header>
            <span class="card-title">管理员提示</span>
          </template>
          <div class="tips-content">
            <p>作为管理员，您无需填写个人资料即可使用所有功能。</p>
            <p>如需修改账户信息，请前往账户设置页面。</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import { getUserById } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const userInfo = ref<any>({})

const todayActions = ref(0)
const weekActions = ref(0)
const monthActions = ref(0)

const loadUserInfo = async () => {
  if (!authStore.user?.id) {
    loading.value = false
    return
  }
  
  // 先使用store中的数据作为初始值
  if (authStore.user) {
    userInfo.value = {
      ...authStore.user,
      roles: authStore.user.roles || []
    }
  }
  
  loading.value = true
  try {
    const data = await getUserById(authStore.user.id)
    userInfo.value = {
      ...data,
      roles: data.roles || []
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    // 如果加载失败，至少显示store中的数据
    if (!userInfo.value.id && authStore.user) {
      userInfo.value = {
        ...authStore.user,
        roles: authStore.user.roles || []
      }
    }
    ElMessage.warning(error.message || '加载用户信息失败，显示缓存数据')
  } finally {
    loading.value = false
  }
}

const goToSettings = () => {
  router.push({ name: 'AccountSettings' })
}

const getRoleText = (role: string) => {
  const roleMap: Record<string, string> = {
    STUDENT: '学生',
    MENTOR: '导师',
    PLATFORM_ADMIN: '平台管理员'
  }
  return roleMap[role] || role
}

const getRoleTagType = (role: string) => {
  if (role.includes('ADMIN')) return 'danger'
  if (role === 'MENTOR') return 'warning'
  return 'info'
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadUserInfo()
  // TODO: 加载操作统计
})
</script>

<style scoped lang="scss">
.admin-profile {
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

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .info-list {
    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 0;
      border-bottom: 1px solid var(--border-subtle);

      &:last-child {
        border-bottom: none;
      }

      .label {
        color: var(--text-color-muted);
        font-size: 14px;
      }

      .value {
        font-weight: 500;
        color: var(--text-color);
        font-size: 14px;
      }
    }
  }

  .security-list {
    .security-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 0;

      .security-info {
        flex: 1;

        .security-title {
          font-weight: 500;
          margin-bottom: 4px;
        }

        .security-desc {
          font-size: 12px;
          color: var(--text-color-muted);
        }
      }
    }
  }

  .stats-list {
    .stat-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid var(--border-subtle);

      &:last-child {
        border-bottom: none;
      }

      .stat-label {
        color: var(--text-color-muted);
        font-size: 14px;
      }

      .stat-value {
        font-size: 18px;
        font-weight: 600;
        color: var(--accent-color);
      }
    }
  }

  .tips-card {
    .tips-content {
      p {
        margin: 0 0 12px 0;
        font-size: 14px;
        color: var(--text-color-muted);
        line-height: 1.6;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }
  }
}
</style>
