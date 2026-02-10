<template>
  <div class="evaluation-page">
    <el-card v-loading="loading">
      <template #header>
        <h3>团队成员互评</h3>
      </template>

      <el-alert
        title="匿名评价说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <p>本评价采用匿名机制，其他成员无法看到您的身份</p>
        <p>评价将影响成员的信誉积分和能力雷达图，请客观公正评价</p>
      </el-alert>

      <div v-if="teamMembers.length > 0" class="member-list">
        <div v-for="member in teamMembers" :key="member.userId" class="member-item">
          <div class="member-info">
            <el-avatar :size="50" :src="member.avatar">{{ member.username }}</el-avatar>
            <div class="member-detail">
              <h4>{{ member.username }}</h4>
              <span class="role">{{ member.role }}</span>
            </div>
          </div>

          <el-button
            v-if="member.userId !== currentUserId"
            type="primary"
            @click="handleEvaluate(member)"
          >
            评价
          </el-button>
        </div>
      </div>
      
      <el-empty v-else description="暂无团队成员" />
    </el-card>

    <!-- 评价对话框 -->
    <el-dialog 
      v-model="showDialog" 
      :title="`评价 ${currentMember?.username}`" 
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef"
        :model="evaluationForm" 
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="技术贡献" prop="techContributionScore">
          <el-rate v-model="evaluationForm.techContributionScore" :max="5" show-text />
        </el-form-item>

        <el-form-item label="协作态度" prop="collaborationScore">
          <el-rate v-model="evaluationForm.collaborationScore" :max="5" show-text />
        </el-form-item>

        <el-form-item label="任务完成度" prop="taskCompletionScore">
          <el-rate v-model="evaluationForm.taskCompletionScore" :max="5" show-text />
        </el-form-item>

        <el-form-item label="文字评价" prop="comment">
          <el-input
            v-model="evaluationForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请客观评价该成员的表现（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="匿名评价" prop="isAnonymous">
          <el-switch v-model="evaluationForm.isAnonymous" />
          <span style="margin-left: 10px; color: #909399; font-size: 13px">
            开启后，被评价者将无法看到您的身份
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false" :disabled="submitting">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../../store/auth'
import { useTeamStore } from '../../store/team'
import { submitEvaluation } from '../../api/team'
import type { TeamMember } from '../../types/team'

const route = useRoute()
const authStore = useAuthStore()
const teamStore = useTeamStore()

const currentUserId = authStore.user?.id || 0
const teamId = Number(route.params.id)

const loading = ref(false)
const teamMembers = ref<TeamMember[]>([])
const showDialog = ref(false)
const currentMember = ref<TeamMember | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const evaluationForm = reactive({
  techContributionScore: 4,
  collaborationScore: 4,
  taskCompletionScore: 4,
  comment: '',
  isAnonymous: true
})

// 表单验证规则
const formRules: FormRules = {
  techContributionScore: [
    { required: true, message: '请选择技术贡献分数', trigger: 'change' },
    { type: 'number', min: 1, max: 5, message: '分数必须在1-5之间', trigger: 'change' }
  ],
  collaborationScore: [
    { required: true, message: '请选择协作态度分数', trigger: 'change' },
    { type: 'number', min: 1, max: 5, message: '分数必须在1-5之间', trigger: 'change' }
  ],
  taskCompletionScore: [
    { required: true, message: '请选择任务完成度分数', trigger: 'change' },
    { type: 'number', min: 1, max: 5, message: '分数必须在1-5之间', trigger: 'change' }
  ]
}

// 加载团队成员
const loadTeamMembers = async () => {
  loading.value = true
  try {
    // 从 store 获取团队成员
    if (teamStore.currentTeamMembers.length > 0) {
      teamMembers.value = teamStore.currentTeamMembers
    } else {
      // 如果 store 中没有，重新加载
      await teamStore.fetchTeamDetail(teamId)
      teamMembers.value = teamStore.currentTeamMembers
    }
  } catch (error: any) {
    console.error('Failed to load team members:', error)
    ElMessage.error(error.response?.data?.message || '加载团队成员失败')
  } finally {
    loading.value = false
  }
}

const handleEvaluate = (member: TeamMember) => {
  currentMember.value = member
  showDialog.value = true
  
  // 重置表单
  Object.assign(evaluationForm, {
    techContributionScore: 4,
    collaborationScore: 4,
    taskCompletionScore: 4,
    comment: '',
    isAnonymous: true
  })
  
  // 清除表单验证
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!currentMember.value) return
  
  // 验证表单
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.warning('请完善评价信息')
    return
  }

  submitting.value = true
  try {
    // 调用 API 提交评价
    await submitEvaluation(teamId, {
      evaluatedId: currentMember.value.userId,
      techContributionScore: evaluationForm.techContributionScore,
      collaborationScore: evaluationForm.collaborationScore,
      taskCompletionScore: evaluationForm.taskCompletionScore,
      comment: evaluationForm.comment || undefined,
      isAnonymous: evaluationForm.isAnonymous
    })
    
    ElMessage.success('评价提交成功')
    showDialog.value = false
  } catch (error: any) {
    console.error('Failed to submit evaluation:', error)
    const errorMessage = error.response?.data?.message || '提交评价失败'
    ElMessage.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTeamMembers()
})
</script>

<style scoped lang="scss">
.evaluation-page {
  padding: 20px;

  .member-list {
    .member-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border: 1px solid #ebeef5;
      border-radius: 8px;
      margin-bottom: 12px;

      &:hover {
        background: #f5f7fa;
      }

      .member-info {
        display: flex;
        align-items: center;
        gap: 16px;

        .member-detail {
          h4 {
            margin: 0 0 4px 0;
            font-size: 16px;
          }

          .role {
            font-size: 13px;
            color: #909399;
          }
        }
      }
    }
  }
}
</style>

