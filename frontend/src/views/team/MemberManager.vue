<template>
  <div class="member-manager">
    <el-card>
      <template #header>
        <div class="flex-between">
          <h3>成员管理</h3>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            邀请成员
          </el-button>
        </div>
      </template>

      <el-table :data="members" style="width: 100%" v-loading="loading">
        <el-table-column label="用户" width="250">
          <template #default="scope">
            <div class="user-info">
              <el-avatar :size="32" :src="scope.row.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              <div class="text-info">
                <span class="nickname">{{ scope.row.nickname || '用户' + scope.row.userId }}</span>
                <span class="username">@{{ scope.row.username || scope.row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'LEADER' ? 'danger' : 'info'">
              {{ scope.row.role === 'LEADER' ? '队长' : '成员' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="joinedAt" label="加入时间">
          <template #default="scope">
            {{ new Date(scope.row.joinedAt).toLocaleDateString() }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="right">
          <template #default="scope">
            <el-button 
              v-if="scope.row.role !== 'LEADER'" 
              type="danger" 
              link 
              size="small"
              @click="handleRemoveMember(scope.row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 邀请成员对话框 -->
    <el-dialog v-model="showAddDialog" title="邀请成员" width="400px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="用户ID">
          <el-input v-model.number="addForm.userId" placeholder="请输入用户ID" />
          <div class="form-tip">请输入要邀请的用户的ID</div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddMember" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTeamMembers, addTeamMember, removeTeamMember } from '../../api/team'
import type { TeamMember } from '../../types/team'

const props = defineProps<{
  teamId: number
}>()

const members = ref<TeamMember[]>([])
const loading = ref(false)
const showAddDialog = ref(false)
const submitting = ref(false)

const addForm = reactive({
  userId: undefined as number | undefined
})

const loadMembers = async () => {
  if (!props.teamId) return
  loading.value = true
  try {
    const res = await getTeamMembers(props.teamId)
    members.value = res || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

watch(() => props.teamId, (newId) => {
  if (newId) {
    loadMembers()
  }
})

const handleAddMember = async () => {
  if (!addForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }
  
  submitting.value = true
  try {
    await addTeamMember(props.teamId, addForm.userId)
    ElMessage.success('邀请成功')
    showAddDialog.value = false
    addForm.userId = undefined
    loadMembers()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleRemoveMember = (member: TeamMember) => {
  ElMessageBox.confirm(
    `确定要将 ${member.nickname || member.username} 移除出团队吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await removeTeamMember(props.teamId, member.userId)
      ElMessage.success('移除成功')
      loadMembers()
    } catch (error) {
      console.error(error)
    }
  })
}

onMounted(() => {
  loadMembers()
})
</script>

<style scoped lang="scss">
.member-manager {
  padding: 20px;
  background: var(--bg-body);
  color: var(--text-color);

  :deep(.el-card) {
    background: var(--bg-elevated);
    border: 1px solid var(--border-card);
    box-shadow: var(--shadow-card);
  }
  
  :deep(.el-card__header) {
    border-bottom: 1px solid var(--border-card);
  }
  
  .flex-between {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h3 {
      margin: 0;
    }
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    
    .text-info {
      display: flex;
      flex-direction: column;
      
      .nickname {
        font-weight: 500;
      }
      
      .username {
        font-size: 12px;
        color: var(--text-color-muted);
      }
    }
  }
  
  .form-tip {
    font-size: 12px;
    color: var(--text-color-muted);
    margin-top: 4px;
  }
}
</style>
