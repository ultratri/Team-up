<template>
  <div class="daily-standup">
    <div class="board-header">
      <div class="header-left">
        <h2>每日站会</h2>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          @change="loadStandups"
          size="default"
          format="YYYY-MM-DD"
        />
      </div>
      <button class="primary-btn" @click="showSubmitDialog = true">
        <el-icon><Edit /></el-icon>
        <span>提交站会</span>
      </button>
    </div>

    <!-- 站会记录列表 -->
    <div class="standup-list" v-loading="loading">
      <div 
        v-for="standup in standups" 
        :key="standup.id"
        class="standup-card"
      >
        <div class="standup-header">
          <div class="user-info">
            <el-avatar :src="standup.userAvatar" :size="40">
              {{ standup.userName?.charAt(0) }}
            </el-avatar>
            <div class="user-details">
              <span class="user-name">{{ standup.userName }}</span>
              <span class="standup-time">{{ formatTime(standup.createdAt) }}</span>
            </div>
          </div>
          <el-tag v-if="standup.sprintName" type="info" size="small">
            {{ standup.sprintName }}
          </el-tag>
        </div>
        
        <div class="standup-content">
          <div class="content-section">
            <div class="section-header">
              <el-icon color="var(--el-color-success)"><CircleCheck /></el-icon>
              <span>昨日完成</span>
            </div>
            <p>{{ standup.yesterdayWork || '无' }}</p>
          </div>
          
          <div class="content-section">
            <div class="section-header">
              <el-icon color="var(--el-color-primary)"><Calendar /></el-icon>
              <span>今日计划</span>
            </div>
            <p>{{ standup.todayPlan || '无' }}</p>
          </div>
          
          <div class="content-section" v-if="standup.blockers">
            <div class="section-header">
              <el-icon color="var(--el-color-warning)"><Warning /></el-icon>
              <span>遇到的问题</span>
            </div>
            <p class="blockers">{{ standup.blockers }}</p>
          </div>
        </div>
      </div>
      
      <div v-if="standups.length === 0 && !loading" class="empty-state">
        <el-empty description="当天暂无站会记录" />
      </div>
    </div>

    <!-- 提交站会对话框 -->
    <el-dialog 
      v-model="showSubmitDialog" 
      title="提交每日站会"
      width="600px"
    >
      <el-form :model="standupForm" label-position="top">
        <el-form-item label="日期" required>
          <el-date-picker 
            v-model="standupForm.standupDate" 
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item label="关联Sprint">
          <el-select 
            v-model="standupForm.sprintId" 
            placeholder="选择Sprint（可选）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="sprint in activeSprints"
              :key="sprint.id"
              :label="sprint.name"
              :value="sprint.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="昨日完成" required>
          <el-input 
            v-model="standupForm.yesterdayWork" 
            type="textarea" 
            :rows="3"
            placeholder="描述昨日完成的工作"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="今日计划" required>
          <el-input 
            v-model="standupForm.todayPlan" 
            type="textarea" 
            :rows="3"
            placeholder="描述今日计划的工作"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="遇到的问题">
          <el-input 
            v-model="standupForm.blockers" 
            type="textarea" 
            :rows="2"
            placeholder="描述遇到的问题或阻碍（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" @click="submitStandup" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Calendar, Warning, CircleCheck } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { 
  getTeamStandups, 
  submitStandup as submitStandupAPI,
  type DailyStandupVO 
} from '@/api/standup'
import { getTeamSprints, type SprintVO } from '@/api/sprint'

const props = defineProps<{
  teamId: number
}>()

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id)

const standups = ref<DailyStandupVO[]>([])
const activeSprints = ref<SprintVO[]>([])
const selectedDate = ref(new Date())
const loading = ref(false)
const submitting = ref(false)
const showSubmitDialog = ref(false)
const standupForm = ref({
  standupDate: new Date().toISOString().split('T')[0],
  sprintId: null as number | null,
  yesterdayWork: '',
  todayPlan: '',
  blockers: ''
})

const loadStandups = async () => {
  loading.value = true
  try {
    const dateStr = selectedDate.value.toISOString().split('T')[0]
    standups.value = await getTeamStandups(props.teamId, dateStr)
  } catch (error) {
    console.error('加载站会记录失败:', error)
    ElMessage.error('加载站会记录失败')
  } finally {
    loading.value = false
  }
}

const loadActiveSprints = async () => {
  try {
    const allSprints = await getTeamSprints(props.teamId)
    activeSprints.value = allSprints.filter(s => s.status === 'IN_PROGRESS')
  } catch (error) {
    console.error('加载 Sprint 失败:', error)
  }
}

const submitStandup = async () => {
  if (!standupForm.value.yesterdayWork || !standupForm.value.todayPlan) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  if (!currentUserId.value) {
    ElMessage.error('用户未登录')
    return
  }
  
  submitting.value = true
  try {
    await submitStandupAPI({
      teamId: props.teamId,
      userId: currentUserId.value,
      standupDate: standupForm.value.standupDate,
      sprintId: standupForm.value.sprintId,
      yesterdayWork: standupForm.value.yesterdayWork,
      todayPlan: standupForm.value.todayPlan,
      blockers: standupForm.value.blockers
    })
    
    ElMessage.success('提交成功')
    showSubmitDialog.value = false
    resetForm()
    loadStandups()
  } catch (error: any) {
    console.error('提交失败:', error)
    if (error.message?.includes('已提交')) {
      ElMessage.warning('今日已提交站会记录')
    } else {
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  standupForm.value = {
    standupDate: new Date().toISOString().split('T')[0],
    sprintId: null,
    yesterdayWork: '',
    todayPlan: '',
    blockers: ''
  }
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

onMounted(() => {
  loadStandups()
  loadActiveSprints()
})
</script>

<style scoped lang="scss">
.daily-standup {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
    }
  }
}

.primary-btn {
  padding: 10px 20px;
  border-radius: 20px;
  background: var(--accent-color);
  color: white;
  border: none;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  
  &:hover {
    filter: brightness(1.1);
    transform: translateY(-1px);
  }
}

.standup-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.standup-card {
  background: var(--bg-elevated-soft);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.standup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .user-details {
      display: flex;
      flex-direction: column;
      gap: 2px;
      
      .user-name {
        font-weight: 600;
        font-size: 15px;
        color: var(--text-color);
      }
      
      .standup-time {
        color: var(--text-color-secondary);
        font-size: 13px;
      }
    }
  }
}

.standup-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  
  .content-section {
    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      font-weight: 600;
      font-size: 14px;
      color: var(--text-color);
    }
    
    p {
      margin: 0;
      padding-left: 28px;
      color: var(--text-color-secondary);
      line-height: 1.6;
      font-size: 14px;
      
      &.blockers {
        color: var(--el-color-warning);
      }
    }
  }
}

.empty-state {
  padding: 60px 20px;
}
</style>
