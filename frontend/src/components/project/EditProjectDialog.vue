<template>
  <el-dialog
    v-model="visible"
    title="编辑项目"
    width="720px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form :model="form" label-width="96px">
      <el-form-item label="项目名称" required>
        <el-input v-model="form.title" placeholder="请输入项目名称" />
      </el-form-item>

      <el-form-item label="项目类型" required>
        <el-select v-model="form.projectType" placeholder="请选择项目类型" style="width: 240px;">
          <el-option label="竞赛" value="COMPETITION" />
          <el-option label="科研" value="RESEARCH" />
          <el-option label="创业" value="STARTUP" />
          <el-option label="开源" value="OPENSOURCE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>

      <el-form-item label="项目描述" required>
        <RichTextEditor v-model="form.description" :min-height="'160px'" :max-length="2000" />
      </el-form-item>

      <el-form-item label="需求描述">
        <RichTextEditor v-model="form.requirements" :min-height="'200px'" :max-length="5000" />
      </el-form-item>

      <el-form-item label="技能需求">
        <el-input
          v-model="form.requiredSkills"
          type="textarea"
          :rows="3"
          placeholder="请输入所需技能，用逗号分隔，例如：Java, Spring Boot, Vue.js"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="团队规模">
        <el-slider v-model="form.teamSize" :min="2" :max="20" show-input />
      </el-form-item>

      <el-form-item label="每周投入">
        <el-slider v-model="form.weeklyHours" :min="5" :max="60" show-input />
      </el-form-item>

      <el-form-item label="预计周期">
        <el-input-number v-model="form.expectedDuration" :min="7" :max="365" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import { updateProject } from '@/api/project'
import type { Project, ProjectType } from '@/types/project'

const props = defineProps<{
  modelValue: boolean
  project: Project | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const saving = ref(false)

const form = reactive<{
  id: number | null
  title: string
  projectType: ProjectType
  description: string
  requirements: string
  requiredSkills: string
  teamSize: number
  weeklyHours: number
  expectedDuration: number
}>({
  id: null,
  title: '',
  projectType: 'COMPETITION',
  description: '',
  requirements: '',
  requiredSkills: '',
  teamSize: 5,
  weeklyHours: 10,
  expectedDuration: 30,
})

watch(
  () => props.project,
  (p) => {
    if (!p) return
    form.id = p.id
    form.title = p.title || ''
    form.projectType = (p.projectType || 'COMPETITION') as ProjectType
    form.description = p.description || ''
    form.requirements = p.requirements || ''
    form.requiredSkills = p.requiredSkills || ''
    form.teamSize = p.teamSize ?? 5
    form.weeklyHours = p.weeklyHours ?? 10
    form.expectedDuration = p.expectedDuration ?? 30
  },
  { immediate: true }
)

const handleSave = async () => {
  if (!form.id) return
  if (!form.title || !form.title.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  const descText = String(form.description || '').replace(/<[^>]*>/g, '').trim()
  if (!descText) {
    ElMessage.warning('请输入项目描述')
    return
  }

  saving.value = true
  try {
    await updateProject(form.id, {
      title: form.title,
      projectType: form.projectType,
      description: form.description,
      requirements: form.requirements,
      requiredSkills: form.requiredSkills,
      teamSize: form.teamSize,
      weeklyHours: form.weeklyHours,
      expectedDuration: form.expectedDuration,
    })
    ElMessage.success('保存成功')
    emit('saved')
    visible.value = false
  } finally {
    saving.value = false
  }
}
</script>
