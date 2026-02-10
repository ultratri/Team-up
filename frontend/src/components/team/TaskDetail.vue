<template>
  <el-dialog
    v-model="visible"
    :title="isEditing ? '编辑任务' : '任务详情'"
    width="900px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="task-detail-dialog"
  >
    <div v-loading="loading" class="task-detail-content">
      <!-- 任务基本信息 -->
      <div class="task-basic-info">
        <el-form
          ref="formRef"
          :model="taskForm"
          :rules="formRules"
          label-width="80px"
          :disabled="!isEditing"
        >
          <el-form-item label="任务标题" prop="title">
            <el-input
              v-model="taskForm.title"
              placeholder="请输入任务标题"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="任务描述" prop="description">
            <el-input
              v-model="taskForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入任务描述"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="状态" prop="status">
                <el-select v-model="taskForm.status" style="width: 100%">
                  <el-option label="待办" value="TODO" />
                  <el-option label="进行中" value="DOING" />
                  <el-option label="审核中" value="REVIEW" />
                  <el-option label="已完成" value="DONE" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="taskForm.priority" style="width: 100%">
                  <el-option label="低" value="LOW" />
                  <el-option label="中" value="MEDIUM" />
                  <el-option label="高" value="HIGH" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="截止日期" prop="deadline">
                <el-date-picker
                  v-model="taskForm.deadline"
                  type="date"
                  placeholder="选择日期"
                  style="width: 100%"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="创建者">
            <span class="creator-info">
              {{ taskDetail?.creatorName || '未知' }}
              <span class="created-time">
                创建于 {{ formatDateTime(taskDetail?.createdAt) }}
              </span>
            </span>
          </el-form-item>
        </el-form>
      </div>

      <!-- 分隔线 -->
      <el-divider />

      <!-- 任务详细信息 - 使用标签页 -->
      <el-tabs v-model="activeTab" class="task-tabs">
        <!-- 负责人标签页 -->
        <el-tab-pane label="负责人" name="assignees">
          <TaskAssignees
            v-if="taskDetail"
            ref="assigneesRef"
            :task-id="taskDetail.id"
            :team-id="taskDetail.teamId"
            @update="handleSubComponentUpdate"
          />
        </el-tab-pane>

        <!-- 评论标签页 -->
        <el-tab-pane name="comments">
          <template #label>
            <span>
              评论
              <el-badge
                v-if="taskDetail?.commentCount"
                :value="taskDetail.commentCount"
                :max="99"
                class="tab-badge"
              />
            </span>
          </template>
          <TaskComments
            v-if="taskDetail"
            ref="commentsRef"
            :task-id="taskDetail.id"
            @update="handleSubComponentUpdate"
          />
        </el-tab-pane>

        <!-- 附件标签页 -->
        <el-tab-pane name="attachments">
          <template #label>
            <span>
              附件
              <el-badge
                v-if="taskDetail?.attachmentCount"
                :value="taskDetail.attachmentCount"
                :max="99"
                class="tab-badge"
              />
            </span>
          </template>
          <TaskAttachments
            v-if="taskDetail"
            ref="attachmentsRef"
            :task-id="taskDetail.id"
            @update="handleSubComponentUpdate"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 对话框底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          v-if="!isEditing"
          type="primary"
          @click="isEditing = true"
        >
          编辑
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { request } from '@/utils/request'
import { useAuthStore } from '@/store/auth'
import TaskAssignees from './TaskAssignees.vue'
import TaskComments from './TaskComments.vue'
import TaskAttachments from './TaskAttachments.vue'

interface TaskDetailDTO {
  id: number
  teamId: number
  title: string
  description: string
  status: 'TODO' | 'DOING' | 'REVIEW' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  deadline?: string
  createdBy: number
  creatorName: string
  createdAt: string
  updatedAt: string
  assignees?: any[]
  comments?: any[]
  attachments?: any[]
  commentCount?: number
  attachmentCount?: number
}

interface TaskForm {
  title: string
  description: string
  status: 'TODO' | 'DOING' | 'REVIEW' | 'DONE'
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  deadline?: string
}

const props = defineProps<{
  modelValue: boolean
  taskId?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'saved': []
}>()

// Auth store
const authStore = useAuthStore()

// State
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const activeTab = ref('assignees')
const taskDetail = ref<TaskDetailDTO | null>(null)
const formRef = ref<FormInstance>()
const assigneesRef = ref()
const commentsRef = ref()
const attachmentsRef = ref()

// Form data
const taskForm = reactive<TaskForm>({
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  deadline: undefined
})

// Form validation rules
const formRules: FormRules = {
  title: [
    { required: true, message: '请输入任务标题', trigger: 'blur' },
    { min: 1, max: 255, message: '标题长度在 1 到 255 个字符', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value.trim().length === 0) {
          callback(new Error('任务标题不能为空白字符'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  status: [
    { required: true, message: '请选择任务状态', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请选择任务优先级', trigger: 'change' }
  ],
  deadline: [
    {
      validator: (rule, value, callback) => {
        if (value) {
          // 验证日期格式
          const dateRegex = /^\d{4}-\d{2}-\d{2}$/
          if (!dateRegex.test(value)) {
            callback(new Error('日期格式不正确，应为 YYYY-MM-DD'))
            return
          }
          
          const deadline = new Date(value)
          
          // 验证日期是否有效
          if (isNaN(deadline.getTime())) {
            callback(new Error('无效的日期'))
            return
          }
          
          // 验证日期不能早于今天
          const today = new Date()
          today.setHours(0, 0, 0, 0)
          if (deadline < today) {
            callback(new Error('截止日期不能早于今天'))
            return
          }
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

// Watch modelValue changes
watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
  if (newVal && props.taskId) {
    fetchTaskDetail()
  }
})

watch(visible, (newVal) => {
  if (!newVal) {
    emit('update:modelValue', false)
  }
})

// Methods
const fetchTaskDetail = async () => {
  if (!props.taskId) return

  loading.value = true
  try {
    const response = await request.get<TaskDetailDTO>(
      `/tasks/${props.taskId}`
    )
    
    // request.ts 已经解包了 data，所以 response 就是 TaskDetailDTO
    if (response) {
      taskDetail.value = response
      
      // 填充表单数据
      taskForm.title = response.title || ''
      taskForm.description = response.description || ''
      taskForm.status = response.status || 'TODO'
      taskForm.priority = response.priority || 'MEDIUM'
      taskForm.deadline = response.deadline

      // 重置编辑状态
      isEditing.value = false
      
      // 刷新子组件
      await nextTick()
      refreshSubComponents()
    }
  } catch (error: any) {
    console.error('Failed to fetch task detail:', error)
    ElMessage.error(error.message || '获取任务详情失败')
    handleClose()
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value || !taskDetail.value) return

  try {
    // 验证表单
    await formRef.value.validate()

    saving.value = true
    
    // 构建更新数据
    const updateData = {
      id: taskDetail.value.id,
      teamId: taskDetail.value.teamId,
      title: taskForm.title,
      description: taskForm.description,
      status: taskForm.status,
      priority: taskForm.priority,
      deadline: taskForm.deadline,
      createdBy: taskDetail.value.createdBy
    }

    const response = await request.put<TaskDetailDTO>(
      `/tasks?userId=${authStore.user?.id}`,
      updateData
    )

    // 后端返回的是 Result<Task>，数据在 response 中
    ElMessage.success('保存成功')
    isEditing.value = false
    
    // 重新获取任务详情以更新数据
    await fetchTaskDetail()
    
    emit('saved')
  } catch (error: any) {
    if (error.errors) {
      // 表单验证错误
      return
    }
    console.error('Failed to save task:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleClose = () => {
  // 如果正在编辑，提示用户
  if (isEditing.value) {
    ElMessage.warning('编辑未保存，已取消')
  }
  
  visible.value = false
  isEditing.value = false
  activeTab.value = 'assignees'
  taskDetail.value = null
  
  // 重置表单
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const handleSubComponentUpdate = () => {
  // 子组件更新后，重新获取任务详情以更新计数
  fetchTaskDetail()
}

const refreshSubComponents = () => {
  // 刷新所有子组件
  if (assigneesRef.value?.refresh) {
    assigneesRef.value.refresh()
  }
  if (commentsRef.value?.refresh) {
    commentsRef.value.refresh()
  }
  if (attachmentsRef.value?.refresh) {
    attachmentsRef.value.refresh()
  }
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '未知'
  
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Expose methods for parent component
defineExpose({
  refresh: fetchTaskDetail
})
</script>

<style scoped lang="scss">
.task-detail-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
    max-height: 70vh;
    overflow-y: auto;
  }
}

.task-detail-content {
  .task-basic-info {
    .creator-info {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 14px;
        color: var(--text-color);

      .created-time {
        font-size: 12px;
          color: var(--text-color-muted);
      }
    }
  }

  .task-tabs {
    :deep(.el-tabs__content) {
      min-height: 300px;
      max-height: 400px;
      overflow-y: auto;
    }

    .tab-badge {
      margin-left: 8px;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
