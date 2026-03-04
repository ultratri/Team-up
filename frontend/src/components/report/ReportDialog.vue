<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { submitReport, TargetType, ReportReason, type SubmitReportDTO } from '@/api/report'

interface Props {
  targetType: TargetType
  targetId: number
  targetName: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  success: []
}>()

const visible = ref(false)
const loading = ref(false)

const formData = ref<SubmitReportDTO>({
  targetType: props.targetType,
  targetId: props.targetId,
  reason: ReportReason.SPAM,
  description: '',
  evidenceUrls: []
})

// 举报原因选项
const reasonOptions = [
  { label: '垃圾信息', value: ReportReason.SPAM },
  { label: '诈骗行为', value: ReportReason.FRAUD },
  { label: '不当内容', value: ReportReason.INAPPROPRIATE },
  { label: '骚扰行为', value: ReportReason.HARASSMENT },
  { label: '其他', value: ReportReason.OTHER }
]

// 目标类型显示文本
const targetTypeText = computed(() => {
  const map = {
    [TargetType.PROJECT]: '项目',
    [TargetType.TEAM]: '团队',
    [TargetType.USER]: '用户',
    [TargetType.COMMENT]: '评论'
  }
  return map[props.targetType] || '内容'
})

const open = () => {
  visible.value = true
  // 重置表单
  formData.value = {
    targetType: props.targetType,
    targetId: props.targetId,
    reason: ReportReason.SPAM,
    description: '',
    evidenceUrls: []
  }
}

const close = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formData.value.description.trim()) {
    ElMessage.warning('请填写举报描述')
    return
  }

  loading.value = true
  try {
    await submitReport(formData.value)
    ElMessage.success('举报提交成功，我们会尽快处理')
    emit('success')
    close()
  } catch (error: any) {
    ElMessage.error(error.message || '提交举报失败')
  } finally {
    loading.value = false
  }
}

defineExpose({
  open,
  close
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="`举报${targetTypeText}`"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form :model="formData" label-width="80px">
      <el-form-item label="举报对象">
        <el-text>{{ targetName }}</el-text>
      </el-form-item>

      <el-form-item label="举报原因" required>
        <el-select v-model="formData.reason" placeholder="请选择举报原因" style="width: 100%">
          <el-option
            v-for="option in reasonOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="详细描述" required>
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="5"
          placeholder="请详细描述举报原因，以便我们更好地处理"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="提示">
        <el-alert
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <div style="font-size: 12px; line-height: 1.6">
              • 请如实填写举报信息，恶意举报将受到处罚<br>
              • 我们会在3个工作日内处理您的举报<br>
              • 处理结果将通过站内通知告知您
            </div>
          </template>
        </el-alert>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        提交举报
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
:deep(.el-form-item__label) {
  font-weight: 500;
}
</style>
