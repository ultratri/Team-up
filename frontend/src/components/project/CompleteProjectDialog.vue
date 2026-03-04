<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { completeProject } from '@/api/project'
import EvaluationDialog from './EvaluationDialog.vue'

const props = defineProps<{
  modelValue: boolean
  projectId: number
  projectTitle: string
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
const showEvaluationDialog = ref(false)
const form = reactive({
  teamAction: 'KEEP' as 'KEEP' | 'DISSOLVE',
  summary: ''
})

// 重置表单
watch(visible, (val) => {
  if (val) {
    form.teamAction = 'KEEP'
    form.summary = ''
  }
})

// 过滤掉当前用户自己
const membersToEvaluate = computed(() => {
  // 这里需要从外部传入当前用户ID，暂时返回所有成员
  return props.members || []
})

const handleSubmit = async () => {
  // 如果有团队成员需要评价，先打开评价对话框
  if (membersToEvaluate.value.length > 0) {
    showEvaluationDialog.value = true
  } else {
    // 没有成员需要评价，直接完成项目
    await doCompleteProject()
  }
}

// 评价完成后的回调
const handleEvaluationSuccess = async () => {
  await doCompleteProject()
}

// 执行项目完成
const doCompleteProject = async () => {
  loading.value = true
  try {
    await completeProject(props.projectId, form.teamAction, form.summary)
    ElMessage.success('项目已完成')
    visible.value = false
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="完成项目"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="complete-dialog">
      <el-alert
        title="恭喜！"
        :description="`项目「${projectTitle}」即将完成，请选择团队的后续安排。`"
        type="success"
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <el-form :model="form" label-width="100px">
        <el-form-item label="团队处理">
          <el-radio-group v-model="form.teamAction" size="large">
            <el-radio label="KEEP">
              <div class="radio-option">
                <div class="radio-title">保留团队</div>
                <div class="radio-desc">团队转为长期团队，可以接新项目</div>
              </div>
            </el-radio>
            <el-radio label="DISSOLVE">
              <div class="radio-option">
                <div class="radio-title">解散团队</div>
                <div class="radio-desc">团队将被解散，成员关系解除</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="项目总结">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="4"
            placeholder="总结一下项目的收获和经验（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          {{ membersToEvaluate.length > 0 ? '下一步：评价成员' : '确认完成' }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 成员互评对话框 -->
  <EvaluationDialog
    v-model="showEvaluationDialog"
    :project-id="projectId"
    :members="membersToEvaluate"
    @success="handleEvaluationSuccess"
  />
</template>

<style scoped lang="scss">
.complete-dialog {
  padding: 10px 0;
}

.radio-option {
  margin-left: 8px;
}

.radio-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.radio-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

:deep(.el-radio) {
  align-items: flex-start;
  margin-bottom: 16px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

:deep(.el-radio__input) {
  margin-top: 2px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
