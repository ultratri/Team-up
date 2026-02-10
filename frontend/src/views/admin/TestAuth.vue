<template>
  <div class="test-auth">
    <el-card>
      <template #header>
        <h2>认证测试</h2>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="是否已登录">
          <el-tag :type="authStore.isAuthenticated ? 'success' : 'danger'">
            {{ authStore.isAuthenticated ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        
        <el-descriptions-item label="用户名">
          {{ authStore.user?.username || '-' }}
        </el-descriptions-item>
        
        <el-descriptions-item label="用户ID">
          {{ authStore.user?.id || '-' }}
        </el-descriptions-item>
        
        <el-descriptions-item label="Token">
          <div style="word-break: break-all; max-width: 600px;">
            {{ authStore.token ? authStore.token.substring(0, 50) + '...' : '-' }}
          </div>
        </el-descriptions-item>
        
        <el-descriptions-item label="角色列表">
          <el-tag 
            v-for="role in authStore.user?.roles" 
            :key="role"
            style="margin-right: 8px;"
          >
            {{ role }}
          </el-tag>
          <span v-if="!authStore.user?.roles || authStore.user.roles.length === 0">-</span>
        </el-descriptions-item>
        
        <el-descriptions-item label="是否有PLATFORM_ADMIN角色">
          <el-tag :type="authStore.hasRole(['PLATFORM_ADMIN']) ? 'success' : 'danger'">
            {{ authStore.hasRole(['PLATFORM_ADMIN']) ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div style="margin-top: 20px;">
        <h3>API测试</h3>
        <el-space direction="vertical" style="width: 100%;">
          <el-button type="primary" @click="testMentorApplicationsAPI" :loading="testing">
            测试导师申请API
          </el-button>
          <el-button type="primary" @click="testMentorListAPI" :loading="testing">
            测试导师列表API
          </el-button>
        </el-space>

        <div v-if="testResult" style="margin-top: 20px;">
          <h4>测试结果：</h4>
          <el-alert 
            :title="testResult.success ? '成功' : '失败'" 
            :type="testResult.success ? 'success' : 'error'"
            :description="testResult.message"
            show-icon
            :closable="false"
          />
          <pre v-if="testResult.data" style="margin-top: 10px; background: #f5f5f5; padding: 10px; border-radius: 4px; overflow: auto;">{{ JSON.stringify(testResult.data, null, 2) }}</pre>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/store/auth'
import { getMentorApplications, getMentorList } from '@/api/mentor'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const testing = ref(false)
const testResult = ref<any>(null)

const testMentorApplicationsAPI = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    const result = await getMentorApplications(1, 10)
    testResult.value = {
      success: true,
      message: '导师申请API调用成功',
      data: result
    }
    ElMessage.success('API调用成功')
  } catch (error: any) {
    testResult.value = {
      success: false,
      message: error.message || '调用失败',
      data: error
    }
    ElMessage.error(error.message || '调用失败')
  } finally {
    testing.value = false
  }
}

const testMentorListAPI = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    const result = await getMentorList(1, 10)
    testResult.value = {
      success: true,
      message: '导师列表API调用成功',
      data: result
    }
    ElMessage.success('API调用成功')
  } catch (error: any) {
    testResult.value = {
      success: false,
      message: error.message || '调用失败',
      data: error
    }
    ElMessage.error(error.message || '调用失败')
  } finally {
    testing.value = false
  }
}
</script>

<style scoped lang="scss">
.test-auth {
  padding: 20px;
}
</style>
