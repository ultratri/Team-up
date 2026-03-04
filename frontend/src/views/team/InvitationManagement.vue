<template>
  <div class="invitation-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>邀请管理</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 我收到的邀请 -->
        <el-tab-pane label="我收到的邀请" name="received">
          <div v-if="receivedLoading" class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          
          <div v-else-if="receivedInvitations.length === 0" class="empty-state">
            <el-empty description="暂无收到的邀请" />
          </div>
          
          <div v-else class="invitation-list">
            <div
              v-for="invitation in receivedInvitations"
              :key="invitation.id"
              class="invitation-item"
            >
              <div class="invitation-info">
                <el-avatar :size="50" :src="invitation.teamAvatar">
                  {{ invitation.teamName?.charAt(0) }}
                </el-avatar>
                
                <div class="info-content">
                  <div class="team-name">{{ invitation.teamName }}</div>
                  <div class="inviter-info">
                    <el-avatar :size="24" :src="invitation.inviterAvatar">
                      {{ invitation.inviterName?.charAt(0) }}
                    </el-avatar>
                    <span>{{ invitation.inviterName }} 邀请你加入</span>
                  </div>
                  <div class="time-info">
                    邀请时间：{{ formatTime(invitation.invitedAt) }}
                  </div>
                </div>
              </div>
              
              <div class="invitation-actions">
                <el-tag :type="getStatusType(invitation.status)">
                  {{ getStatusText(invitation.status) }}
                </el-tag>
                
                <div v-if="invitation.status === 'PENDING'" class="action-buttons">
                  <el-button
                    size="small"
                    @click="handleViewDetail(invitation.id)"
                  >
                    查看详情
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    @click="handleAccept(invitation.id)"
                  >
                    接受
                  </el-button>
                  <el-button
                    size="small"
                    @click="handleReject(invitation.id)"
                  >
                    拒绝
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 我发出的邀请 -->
        <el-tab-pane label="我发出的邀请" name="sent">
          <div v-if="sentLoading" class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          
          <div v-else-if="sentInvitations.length === 0" class="empty-state">
            <el-empty description="暂无发出的邀请" />
          </div>
          
          <div v-else class="invitation-list">
            <div
              v-for="invitation in sentInvitations"
              :key="invitation.id"
              class="invitation-item"
            >
              <div class="invitation-info">
                <el-avatar :size="50" :src="invitation.teamAvatar">
                  {{ invitation.teamName?.charAt(0) }}
                </el-avatar>
                
                <div class="info-content">
                  <div class="team-name">{{ invitation.teamName }}</div>
                  <div class="invitee-info">
                    <span>邀请</span>
                    <el-avatar :size="24" :src="invitation.inviteeAvatar">
                      {{ invitation.inviteeName?.charAt(0) }}
                    </el-avatar>
                    <span>{{ invitation.inviteeName }}</span>
                  </div>
                  <div class="time-info">
                    邀请时间：{{ formatTime(invitation.invitedAt) }}
                  </div>
                </div>
              </div>
              
              <div class="invitation-actions">
                <el-tag :type="getStatusType(invitation.status)">
                  {{ getStatusText(invitation.status) }}
                </el-tag>
                
                <div v-if="invitation.status === 'PENDING'" class="action-buttons">
                  <el-button
                    size="small"
                    type="danger"
                    @click="handleCancel(invitation.id)"
                  >
                    撤回
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <!-- 邀请详情对话框 -->
    <TeamInvitationDialog
      v-model="showDetailDialog"
      :invitation-id="selectedInvitationId"
      @accepted="handleInvitationAccepted"
      @rejected="handleInvitationRejected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'
import TeamInvitationDialog from '@/components/team/TeamInvitationDialog.vue'

interface Invitation {
  id: number
  teamId: number
  teamName: string
  teamAvatar?: string
  inviterId?: number
  inviterName?: string
  inviterAvatar?: string
  inviteeId?: number
  inviteeName?: string
  inviteeAvatar?: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED'
  message?: string
  invitedAt: string
  respondedAt?: string
  expiresAt?: string
}

const activeTab = ref('received')
const receivedInvitations = ref<Invitation[]>([])
const sentInvitations = ref<Invitation[]>([])
const receivedLoading = ref(false)
const sentLoading = ref(false)
const showDetailDialog = ref(false)
const selectedInvitationId = ref<number>()

onMounted(() => {
  loadReceivedInvitations()
})

// 切换标签页
const handleTabChange = (tabName: string) => {
  if (tabName === 'received') {
    loadReceivedInvitations()
  } else if (tabName === 'sent') {
    loadSentInvitations()
  }
}

// 加载收到的邀请
const loadReceivedInvitations = async () => {
  receivedLoading.value = true
  try {
    const response = await request.get('/teams/invitations/received')
    receivedInvitations.value = response
  } catch (error: any) {
    ElMessage.error(error.message || '加载邀请列表失败')
  } finally {
    receivedLoading.value = false
  }
}

// 加载发出的邀请
const loadSentInvitations = async () => {
  sentLoading.value = true
  try {
    const response = await request.get('/teams/invitations/sent')
    sentInvitations.value = response
  } catch (error: any) {
    ElMessage.error(error.message || '加载邀请列表失败')
  } finally {
    sentLoading.value = false
  }
}

// 查看详情
const handleViewDetail = (invitationId: number) => {
  selectedInvitationId.value = invitationId
  showDetailDialog.value = true
}

// 接受邀请
const handleAccept = async (invitationId: number) => {
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
    
    await request.post(`/teams/invitations/${invitationId}/accept`)
    ElMessage.success('已接受邀请，欢迎加入团队！')
    loadReceivedInvitations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '接受邀请失败')
    }
  }
}

// 拒绝邀请
const handleReject = async (invitationId: number) => {
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
    
    await request.post(`/teams/invitations/${invitationId}/reject`)
    ElMessage.success('已拒绝邀请')
    loadReceivedInvitations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '拒绝邀请失败')
    }
  }
}

// 撤回邀请
const handleCancel = async (invitationId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要撤回这个邀请吗？',
      '确认撤回',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await request.post(`/teams/invitations/${invitationId}/cancel`)
    ElMessage.success('已撤回邀请')
    loadSentInvitations()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤回邀请失败')
    }
  }
}

// 邀请被接受
const handleInvitationAccepted = () => {
  loadReceivedInvitations()
}

// 邀请被拒绝
const handleInvitationRejected = () => {
  loadReceivedInvitations()
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
    PENDING: 'warning',
    ACCEPTED: 'success',
    REJECTED: 'info',
    EXPIRED: 'info',
    CANCELLED: 'info'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '待处理',
    ACCEPTED: '已接受',
    REJECTED: '已拒绝',
    EXPIRED: '已过期',
    CANCELLED: '已撤回'
  }
  return textMap[status] || '未知'
}
</script>

<style scoped lang="scss">
.invitation-management {
  padding: 20px;
  
  .card-header {
    h2 {
      margin: 0;
      font-size: 20px;
      color: var(--el-text-color-primary);
    }
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.empty-state {
  padding: 40px 0;
}

.invitation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.invitation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  transition: all 0.3s;
  
  &:hover {
    background: var(--el-fill-color);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  .invitation-info {
    display: flex;
    gap: 16px;
    flex: 1;
    
    .info-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      
      .team-name {
        font-size: 16px;
        font-weight: 500;
        color: var(--el-text-color-primary);
      }
      
      .inviter-info,
      .invitee-info {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: var(--el-text-color-secondary);
      }
      
      .time-info {
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
    }
  }
  
  .invitation-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 12px;
    
    .action-buttons {
      display: flex;
      gap: 8px;
    }
  }
}

@media (max-width: 768px) {
  .invitation-item {
    flex-direction: column;
    align-items: flex-start;
    
    .invitation-actions {
      width: 100%;
      align-items: flex-start;
      
      .action-buttons {
        width: 100%;
        justify-content: flex-start;
      }
    }
  }
}
</style>
