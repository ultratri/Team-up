<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { request } from '@/utils/request'

const props = defineProps<{
  modelValue: boolean
  projectId: number
  members: Array<{
    userId: number
    userName: string
    realName?: string
  }>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)

// 初始化评价数据
const evaluations = ref<Record<number, {
  techContribution: number
  collaboration: number
  taskCompletion: number
  comment: string
  anonymous: boolean
}>>({})

// 监听members变化,初始化评价数据
const initEvaluations = () => {
  const data: typeof evaluations.value = {}
  props.members.forEach(member => {
    data[member.userId] = {
      techContribution: 5,
      collaboration: 5,
      taskCompletion: 5,
      comment: '',
      anonymous: false
    }
  })
  evaluations.value = data
}

// 当对话框打开时初始化
const handleOpen = () => {
  initEvaluations()
}

// 提交评价
const handleSubmit = async () => {
  // 验证所有评价都已填写
  for (const member of props.members) {
    const evaluation = evaluations.value[member.userId]
    if (!evaluation.comment || evaluation.comment.trim() === '') {
      ElMessage.warning(`请填写对 ${member.userName} 的评价内容`)
      return
    }
  }
  
  loading.value = true
  try {
    // 构建提交数据
    const submitData = {
      projectId: props.projectId,
      evaluations: props.members.map(member => ({
        evaluateeId: member.userId,
        techContribution: evaluations.value[member.userId].techContribution,
        collaboration: evaluations.value[member.userId].collaboration,
        taskCompletion: evaluations.value[member.userId].taskCompletion,
        comment: evaluations.value[member.userId].comment,
        anonymous: evaluations.value[member.userId].anonymous
      }))
    }
    
    await request.post('/evaluations/batch', submitData)
    
    ElMessage.success('评价提交成功')
    visible.value = false
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="项目成员互评"
    width="700px"
    :close-on-click-modal="false"
    @open="handleOpen"
  >
    <div class="evaluation-container">
      <el-alert
        title="请对项目成员进行客观公正的评价"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #default>
          <p>评价将用于生成协作历史记录,影响成员的信誉分和匹配度</p>
        </template>
      </el-alert>
      
      <div
        v-for="member in members"
        :key="member.userId"
        class="member-evaluation"
      >
        <h4 class="member-name">
          {{ member.realName || member.userName }}
        </h4>
        
        <el-form label-position="top">
          <el-form-item label="技术贡献">
            <el-rate
              v-model="evaluations[member.userId].techContribution"
              :max="5"
              show-text
              :texts="['很差', '较差', '一般', '良好', '优秀']"
            />
          </el-form-item>
          
          <el-form-item label="协作能力">
            <el-rate
              v-model="evaluations[member.userId].collaboration"
              :max="5"
              show-text
              :texts="['很差', '较差', '一般', '良好', '优秀']"
            />
          </el-form-item>
          
          <el-form-item label="任务完成">
            <el-rate
              v-model="evaluations[member.userId].taskCompletion"
              :max="5"
              show-text
              :texts="['很差', '较差', '一般', '良好', '优秀']"
            />
          </el-form-item>
          
          <el-form-item label="评价内容" required>
            <el-input
              v-model="evaluations[member.userId].comment"
              type="textarea"
              :rows="3"
              placeholder="请描述该成员在项目中的表现..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item>
            <el-checkbox v-model="evaluations[member.userId].anonymous">
              匿名评价(对方看不到评价人)
            </el-checkbox>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
        >
          提交评价
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.evaluation-container {
  max-height: 600px;
  overflow-y: auto;
}

.member-evaluation {
  padding: 20px;
  margin-bottom: 20px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.member-name {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
