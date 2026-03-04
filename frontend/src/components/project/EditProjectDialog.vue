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
        <div class="skill-requirements-editor">
          <div v-for="(skill, index) in form.skillRequirements" :key="index" class="skill-item">
            <el-select
              v-model="skill.skillName"
              placeholder="选择技能"
              filterable
              style="width: 200px;"
            >
              <el-option
                v-for="tag in skillTags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.name"
              />
            </el-select>
            <el-select
              v-model="skill.proficiencyLevel"
              placeholder="要求等级"
              style="width: 120px; margin-left: 8px;"
            >
              <el-option label="入门" value="BEGINNER" />
              <el-option label="熟练" value="INTERMEDIATE" />
              <el-option label="高级" value="ADVANCED" />
              <el-option label="精通" value="EXPERT" />
            </el-select>
            <el-checkbox
              v-model="skill.required"
              style="margin-left: 8px;"
            >
              必需
            </el-checkbox>
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              style="margin-left: 8px;"
              @click="removeSkill(index)"
            />
          </div>
          <el-button
            type="primary"
            :icon="Plus"
            size="small"
            @click="addSkill"
          >
            添加技能
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="可用时段">
        <div class="time-slots-editor">
          <div v-for="(slot, index) in form.timeSlots" :key="index" class="time-slot-item">
            <el-select
              v-model="slot.dayOfWeek"
              placeholder="选择星期"
              style="width: 100px;"
            >
              <el-option
                v-for="(label, day) in dayLabels"
                :key="day"
                :label="label"
                :value="Number(day)"
              />
            </el-select>
            <el-time-select
              v-model="slot.startTime"
              placeholder="开始时间"
              start="00:00"
              step="00:30"
              end="23:30"
              style="width: 120px; margin-left: 8px;"
            />
            <span style="margin: 0 8px;">至</span>
            <el-time-select
              v-model="slot.endTime"
              placeholder="结束时间"
              start="00:00"
              step="00:30"
              end="23:30"
              :min-time="slot.startTime"
              style="width: 120px;"
            />
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              style="margin-left: 8px;"
              @click="removeTimeSlot(index)"
            />
          </div>
          <el-button
            type="primary"
            :icon="Plus"
            size="small"
            @click="addTimeSlot"
          >
            添加时段
          </el-button>
        </div>
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
import { computed, reactive, watch, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import { updateProjectWithSkills, getProjectSkillRequirements, getProjectTimeSlots } from '@/api/project'
import { getSkillTags } from '@/api/tag'
import type { Project, ProjectType, ProjectSkillRequirement } from '@/types/project'
import type { Tag } from '@/api/tag'

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
const skillTags = ref<Tag[]>([])

interface SkillRequirement {
  skillName: string
  proficiencyLevel: string
  required: boolean
}

const form = reactive<{
  id: number | null
  title: string
  projectType: ProjectType
  description: string
  requirements: string
  teamSize: number
  weeklyHours: number
  expectedDuration: number
  skillRequirements: SkillRequirement[]
  timeSlots: Array<{ dayOfWeek: number; startTime: string; endTime: string }>
}>({
  id: null,
  title: '',
  projectType: 'COMPETITION',
  description: '',
  requirements: '',
  teamSize: 5,
  weeklyHours: 10,
  expectedDuration: 30,
  skillRequirements: [],
  timeSlots: [],
})

const dayLabels: Record<number, string> = {
  1: '周一', 2: '周二', 3: '周三', 4: '周四',
  5: '周五', 6: '周六', 7: '周日'
}

// 加载技能标签
onMounted(async () => {
  try {
    skillTags.value = await getSkillTags()
  } catch (error) {
    console.error('加载技能标签失败:', error)
  }
})

// 添加技能
const addSkill = () => {
  form.skillRequirements.push({
    skillName: '',
    proficiencyLevel: 'INTERMEDIATE',
    required: false,
  })
}

// 删除技能
const removeSkill = (index: number) => {
  form.skillRequirements.splice(index, 1)
}

// 添加时间段
const addTimeSlot = () => {
  form.timeSlots.push({ dayOfWeek: 1, startTime: '09:00', endTime: '12:00' })
}

// 删除时间段
const removeTimeSlot = (index: number) => {
  form.timeSlots.splice(index, 1)
}

watch(
  () => props.project,
  async (p) => {
    if (!p) return
    form.id = p.id
    form.title = p.title || ''
    form.projectType = (p.projectType || 'COMPETITION') as ProjectType
    form.description = p.description || ''
    form.requirements = p.requirements || ''
    form.teamSize = p.teamSize ?? 5
    form.weeklyHours = p.weeklyHours ?? 10
    form.expectedDuration = p.expectedDuration ?? 30
    
    // 加载技能需求
    try {
      const skills = await getProjectSkillRequirements(p.id)
      form.skillRequirements = skills.map(s => ({
        skillName: s.skillName,
        proficiencyLevel: s.proficiencyLevel || 'INTERMEDIATE',
        required: s.required || false,
      }))
    } catch (error) {
      console.error('加载技能需求失败:', error)
      form.skillRequirements = []
    }

    // 加载时间段需求
    try {
      const ts = await getProjectTimeSlots(p.id)
      form.timeSlots = (ts || []).map(s => ({
        dayOfWeek: s.dayOfWeek,
        startTime: s.startTime,
        endTime: s.endTime
      }))
    } catch (error) {
      console.error('加载时间段需求失败:', error)
      form.timeSlots = []
    }
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

  // 验证技能需求
  const validSkills = form.skillRequirements.filter(s => s.skillName && s.skillName.trim())
  if (validSkills.some(s => !s.proficiencyLevel)) {
    ElMessage.warning('请为所有技能选择要求等级')
    return
  }

  const payload = {
    title: form.title,
    projectType: form.projectType,
    description: form.description,
    requirements: form.requirements,
    teamSize: form.teamSize,
    weeklyHours: form.weeklyHours,
    expectedDuration: form.expectedDuration,
    skillRequirements: validSkills.map(s => ({
      skillName: s.skillName,
      expectedLevel: s.proficiencyLevel,
      isRequired: s.required,
    })),
    timeSlots: form.timeSlots.map(t => ({
      dayOfWeek: t.dayOfWeek,
      startTime: t.startTime,
      endTime: t.endTime,
    })),
  }
  
  console.log('=== 保存项目数据 ===')
  console.log('项目ID:', form.id)
  console.log('技能需求数量:', validSkills.length)
  console.log('时间段数量:', form.timeSlots.length)
  console.log('时间段原始数据:', form.timeSlots)
  console.log('完整payload:', JSON.stringify(payload, null, 2))

  saving.value = true
  try {
    await updateProjectWithSkills(form.id, payload)
    ElMessage.success('保存成功')
    emit('saved')
    visible.value = false
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.skill-requirements-editor {
  width: 100%;
  
  .skill-item {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }
}

.time-slots-editor {
  width: 100%;
  
  .time-slot-item {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }
}
</style>