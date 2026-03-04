<template>
  <el-dialog
    v-model="visible"
    title="团队邀请"
    width="500px"
    :close-on-click-modal="false"
  >
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    
    <div v-else-if="invitation" class="invitation-content">
      <!-- 团队信息 -->
      <div class="team-info">
        <el-avatar :size="60" :src="invitation.teamAvatar">
          {{ invitation.teamName?.charAt(0) }}
        </el-avatar>
        <div class="team-details">
          <h3>{{ invitation.teamName }}</h3>
          <p v-if="invitation.teamDescription">{{ invitation.teamDescription }}</p>
        </div>
      </div>
      
      <!-- 邀请人信息 -->
      <div class="inviter-info">
        <div class="info-label">邀请人</div>
        <div class="info-content">
          <el-avatar :size="40" :src="invitation.inviterAvatar">
            {{ invitation.inviterName?.charAt(0) }}
          </el-avatar>
          <span class="inviter-name">{{ invitation.inviterName }}</span>
        </div>
      </div>
      
      <!-- 邀请留言 -->
      <div v-if="invitation.message" class="invitation-message">
        <div class="info-label">邀请留言</div>
        <div class="message-content">{{ invitation.message }}</div>
      </div>
      
      <!-- 邀请时间 -->
      <div class="invitation-time">
        <div class="info-label">邀请时间</div>
        <div class="info-content">{{ formatTime(invitation.invitedAt) }}</div>
      </div>
      
      <!-- 过期时间 -->
      <div v-if="invitation.expiresAt" class="expiry-time">
        <div class="info-label">过期时间</div>
        <div class="info-content">{{ formatTime(invitation.expiresAt) }}</div>
      </div>
      
      <!-- 状态提示 -->
      <el-alert
        v-if="invitation.status !== 'PENDING'"
        :type="getStatusType(invitation.status)"
        :title="getStatusText(invitation.status)"
        :closable="false"
        style="margin-top: 16px"
      />
    </div>
    
    <template #footer>
      <div v-if="invitation && invitation.status === 'PENDING'" class="dialog-footer">
        <el-button @click="handleReject" :loading="rejecting">拒绝</el-button>
        <el-button type="primary" @click="handleAccept" :loading="accepting">
          接受邀请
        </el-button>
      </div>
      <div v-else class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface TeamInvitation {
  id: number
  teamId: number
  teamName: string
  teamAvatar?: string
  teamDescription?: string
  inviterId: number
  inviterName: string
  inviterAvatar?: string
  inviteeId: number
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED'
  message?: string
  invitedAt: string
  expiresAt?: string
}

const props = defineProps<{
  modelValue: boolean
  invitationId?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'accepted'): void
  (e: 'rejected'): void
}>()

const visible = ref(false)
const loading = ref(false)
const accepting = ref(false)
const rejecting = ref(false)
const invitation = ref<TeamInvitation | null>(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.invitationId) {
    loadInvitation()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载邀请详情
const loadInvitation = async () => {
  if (!props.invitationId) return
  
  loading.value = true
  try {
    const response = await request.get(`/teams/invitations/${props.invitationId}`)
    invitation.value = response
  } catch (error: any) {
    ElMessage.error(error.message || '加载邀请详情失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

// 接受邀请
const handleAccept = async () => {
  if (!props.invitationId) return
  
  try {
    await ElMessageBox.confirm(
      '确定要接受这个团队邀请吗？',
      '确认接受',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    accepting.value = true
    await request.post(`/teams/invitations/${props.invitationId}/accept`)
    
    ElMessage.success('已接受邀请，欢迎加入团队！')
    emit('accepted')
    visible.value = false
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '接受邀请失败')
    }
  } finally {
    accepting.value = false
  }
}

// 拒绝邀请
const handleReject = async () => {
  if (!props.invitationId) return
  
  try {
    await ElMessageBox.confirm(
      '确定要拒绝这个团队邀请吗？',
      '确认拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    rejecting.value = true
    await request.post(`/teams/invitations/${props.invitationId}/reject`)
    
    ElMessage.success('已拒绝邀请')
    emit('rejected')
    visible.value = false
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '拒绝邀请失败')
    }
  } finally {
    rejecting.value = false
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, any> = {
    ACCEPTED: 'success',
    REJECTED: 'warning',
    EXPIRED: 'info'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    ACCEPTED: '您已接受此邀请',
    REJECTED: '您已拒绝此邀请',
    EXPIRED: '此邀请已过期'
  }
  return textMap[status] || '未知状态'
}
</script>

<style scoped lang="scss">
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.invitation-content {
  .team-info {
    display: flex;
    gap: 16px;
    padding: 20px;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    margin-bottom: 20px;
    
    .team-details {
      flex: 1;
      
      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        color: var(--el-text-color-primary);
      }
      
      p {
        margin: 0;
        font-size: 14px;
        color: var(--el-text-color-secondary);
        line-height: 1.5;
      }
    }
  }
  
  .inviter-info,
  .invitation-message,
  .invitation-time,
  .expiry-time {
    margin-bottom: 16px;
    
    .info-label {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      margin-bottom: 8px;
    }
    
    .info-content {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 14px;
      color: var(--el-text-color-primary);
      
      .inviter-name {
        font-weight: 500;
      }
    }
    
    .message-content {
      padding: 12px;
      background: var(--el-fill-color-lighter);
      border-radius: 6px;
      font-size: 14px;
      color: var(--el-text-color-primary);
      line-height: 1.6;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
