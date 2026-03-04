<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, CircleCloseFilled } from '@element-plus/icons-vue'
import type { Project, ProjectType } from '@/types/project'
import { createProject, publishProject } from '@/api/project'
import { getUserTeams } from '@/api/team'
import { useAuthStore } from '@/store/auth'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import { request } from '@/utils/request'
import type { Team } from '@/types/team'
import { normalizeCandidateMatchItem, type CandidateMatchItem } from '@/utils/fieldNormalizer'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const activeStep = ref(0)
const loading = ref(false)
const authStore = useAuthStore()
const myTeams = ref<Team[]>([])

const form = reactive<Partial<Project> & { 
  teamMode?: string
  existingTeamId?: number
  teamName?: string
  inviteTeammates?: boolean
  invitedUserIds?: number[]
}>({
  title: '',
  projectType: 'COMPETITION' as ProjectType,
  description: '',
  requirements: '',
  teamSize: 5,
  weeklyHours: 10,
  expectedDuration: 30,
  roles: [],
  techStack: [],
  teamMode: 'CREATE_NEW', // 默认创建新团队
  existingTeamId: undefined,
  teamName: '',
  inviteTeammates: false, // 是否邀请队友
  invitedUserIds: [] // 被邀请的用户ID列表
})

const steps = [
  { title: '基本信息', icon: 'Edit' },
  { title: '技能需求', icon: 'Connection' },
  { title: '团队设置', icon: 'User' },
  { title: '团队模式', icon: 'UserFilled' }
]

// Temporary tag inputs
const newRole = ref('')
const newStack = ref('')

// 技能标签相关
const availableSkills = ref<any[]>([])
const selectedSkill = ref<{
  tagId: number | null
  required: boolean
  proficiencyLevel: string
}>({
  tagId: null,
  required: true,
  proficiencyLevel: 'INTERMEDIATE'
})
const showSkillSelector = ref(false)

// 技能需求列表
const skillRequirements = ref<Array<{
  tagId: number
  skillName: string
  required: boolean
  proficiencyLevel: string
}>>([])

// 项目时间段需求
const timeSlots = ref<Array<{
  dayOfWeek: number
  startTime: string
  endTime: string
}>>([])
const showTimeSlotForm = ref(false)
const newTimeSlot = reactive({
  dayOfWeek: 1,
  startTime: '09:00',
  endTime: '12:00'
})
const dayLabels: Record<number, string> = {
  1: '周一', 2: '周二', 3: '周三', 4: '周四',
  5: '周五', 6: '周六', 7: '周日'
}

// 邀请队友相关
const recommendedTeammates = ref<any[]>([])
const teammatesLoading = ref(false)
const selectedTeammates = ref<Set<number>>(new Set())

// 根据匹配维度生成一句简短的解释文案
const getMatchExplanation = (raw: any) => {
  const teammate = raw as CandidateMatchItem
  const breakdown = teammate.breakdown || {}
  const parts: string[] = []

  const skill = Number(breakdown.skill ?? 0)
  if (skill >= 0.75) parts.push('技能匹配高')
  else if (skill >= 0.4) parts.push('技能匹配一般')
  else parts.push('技能匹配偏低')

  const time = Number(breakdown.time ?? 0)
  if (time >= 0.75) parts.push('时间重叠高')
  else if (time >= 0.4) parts.push('时间重叠一般')
  else parts.push('时间重叠较少')

  const credit = Number(breakdown.credit ?? 0)
  if (credit >= 0.75) parts.push('信誉良好')
  else if (credit >= 0.4) parts.push('信誉一般')
  else parts.push('信誉偏低')

  let text = parts.slice(0, 3).join(' · ')

  if (teammate.confidenceLevel && teammate.confidenceLevel !== 'HIGH') {
    if (teammate.confidenceLevel === 'MEDIUM') {
      text += '（置信度中等）'
    } else {
      text += '（置信度较低）'
    }
  }

  return text
}

// 加载可用技能标签
const loadAvailableSkills = async () => {
  try {
    const res = await request.get('/tags/skills')
    availableSkills.value = res || []
  } catch (error) {
    console.error('加载技能标签失败:', error)
  }
}

// 获取熟练度标签
const getProficiencyLabel = (level: string) => {
  const map: Record<string, string> = {
    'BEGINNER': '入门',
    'INTERMEDIATE': '熟练',
    'ADVANCED': '高级',
    'EXPERT': '精通'
  }
  return map[level] || level
}

// 获取熟练度标签类型（根据等级显示不同颜色）
const getProficiencyLevelTagType = (level: string): '' | 'success' | 'info' | 'warning' | 'danger' => {
  const typeMap: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
    'BEGINNER': 'info',      // 灰色 - 入门
    'INTERMEDIATE': '',      // 默认色 - 熟练
    'ADVANCED': 'warning',   // 橙色 - 高级
    'EXPERT': 'success'      // 绿色 - 精通
  }
  return typeMap[level] || ''
}

// 添加技能需求
const handleAddSkillRequirement = () => {
  if (!selectedSkill.value.tagId) {
    ElMessage.warning('请选择技能')
    return
  }
  
  const skill = availableSkills.value.find(s => s.id === selectedSkill.value.tagId)
  if (!skill) return
  
  // 检查是否已添加
  if (skillRequirements.value.some(s => s.tagId === selectedSkill.value.tagId)) {
    ElMessage.warning('该技能已添加')
    return
  }
  
  skillRequirements.value.push({
    tagId: selectedSkill.value.tagId!,
    skillName: skill.name,
    required: selectedSkill.value.required,
    proficiencyLevel: selectedSkill.value.proficiencyLevel
  })
  
  // 重置选择器
  selectedSkill.value = {
    tagId: null,
    required: true,
    proficiencyLevel: 'INTERMEDIATE'
  }
  showSkillSelector.value = false
  ElMessage.success('技能需求已添加')
}

// 移除技能需求
const removeSkillRequirement = (tagId: number) => {
  skillRequirements.value = skillRequirements.value.filter(s => s.tagId !== tagId)
}

// 添加时间段
const handleAddTimeSlot = () => {
  if (newTimeSlot.startTime >= newTimeSlot.endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  timeSlots.value.push({
    dayOfWeek: newTimeSlot.dayOfWeek,
    startTime: newTimeSlot.startTime,
    endTime: newTimeSlot.endTime
  })
  showTimeSlotForm.value = false
  newTimeSlot.dayOfWeek = 1
  newTimeSlot.startTime = '09:00'
  newTimeSlot.endTime = '12:00'
}

// 移除时间段
const removeTimeSlot = (index: number) => {
  timeSlots.value.splice(index, 1)
}

// 加载推荐队友
const loadRecommendedTeammates = async () => {
  if (!form.title || skillRequirements.value.length === 0) {
    ElMessage.warning('请先填写项目基本信息和技能需求')
    return
  }

  teammatesLoading.value = true
  try {
    // 构建推荐请求参数
    const params = {
      projectTitle: form.title,
      projectType: form.projectType,
      skillRequirements: skillRequirements.value.map(s => ({
        tagId: s.tagId,
        required: s.required,
        proficiencyLevel: s.proficiencyLevel
      })),
      timeSlots: timeSlots.value,
      weeklyHours: form.weeklyHours,
      expectedDuration: form.expectedDuration
    }

    console.log('=== 推荐队友请求参数 ===', params)
    const response = await request.post('/matching/recommend-teammates-for-project', params)
    console.log('=== 推荐队友响应数据 ===', response)
    
    recommendedTeammates.value = (response.recommendations || []).map(normalizeCandidateMatchItem)
    console.log('=== 处理后的推荐队友列表 ===', recommendedTeammates.value)
    
    if (recommendedTeammates.value.length === 0) {
      ElMessage.info('暂无推荐队友，你可以稍后在项目详情页邀请')
    } else {
      ElMessage.success(`为你推荐了 ${recommendedTeammates.value.length} 位合适的队友`)
    }
  } catch (error) {
    console.error('加载推荐队友失败:', error)
    ElMessage.error('加载推荐队友失败，请稍后重试')
  } finally {
    teammatesLoading.value = false
  }
}

// 切换队友选择
const toggleTeammate = (userId: number) => {
  if (!userId) {
    console.warn('toggleTeammate: userId is null or undefined')
    return
  }
  
  console.log('toggleTeammate:', userId, 'current selected:', Array.from(selectedTeammates.value))
  
  if (selectedTeammates.value.has(userId)) {
    selectedTeammates.value.delete(userId)
  } else {
    selectedTeammates.value.add(userId)
  }
  
  console.log('after toggle:', Array.from(selectedTeammates.value))
}

const addRole = () => {
  if (newRole.value && !form.roles?.includes(newRole.value)) {
    form.roles = [...(form.roles || []), newRole.value]
    newRole.value = ''
  }
}

const removeRole = (tag: string) => {
  form.roles = form.roles?.filter(r => r !== tag)
}

const addStack = () => {
  if (newStack.value && !form.techStack?.includes(newStack.value)) {
    form.techStack = [...(form.techStack || []), newStack.value]
    newStack.value = ''
  }
}

const removeStack = (tag: string) => {
  form.techStack = form.techStack?.filter(t => t !== tag)
}

const handleNext = () => {
  if (activeStep.value === 0) {
    if (!form.title || !form.description || !form.description.trim()) {
      ElMessage.warning('请填写完整基本信息')
      return
    }
  }
  if (activeStep.value === 3) {
    // 最后一步，验证团队模式
    if (form.teamMode === 'USE_EXISTING' && !form.existingTeamId) {
      ElMessage.warning('请选择一个已有团队')
      return
    }
    if (form.teamMode === 'CREATE_NEW' && !form.teamName) {
      ElMessage.warning('请输入团队名称')
      return
    }
    handleSubmit()
  } else if (activeStep.value < 3) {
    activeStep.value++
  }
}

const handlePrev = () => {
  if (activeStep.value > 0) activeStep.value--
}

const handleSubmit = async () => {
  loading.value = true
  try {
    // 构建提交数据
    const submitData: any = {
      title: form.title,
      projectType: form.projectType,
      description: form.description,
      requirements: form.requirements,
      teamSize: form.teamSize,
      weeklyHours: form.weeklyHours,
      expectedDuration: form.expectedDuration,
      teamMode: form.teamMode,
      skillRequirements: skillRequirements.value.map(skill => ({
        tagId: skill.tagId,
        required: skill.required,
        proficiencyLevel: skill.proficiencyLevel
      })),
      timeSlots: timeSlots.value.map(slot => ({
        dayOfWeek: slot.dayOfWeek,
        startTime: slot.startTime,
        endTime: slot.endTime
      }))
    }

    // 根据团队模式添加相应字段
    if (form.teamMode === 'USE_EXISTING') {
      submitData.existingTeamId = form.existingTeamId
    } else {
      submitData.teamName = form.teamName
      // 如果邀请了队友，添加邀请信息
      if (form.inviteTeammates && selectedTeammates.value.size > 0) {
        submitData.inviteTeammates = true
        submitData.invitedUserIds = Array.from(selectedTeammates.value)
      }
    }

    const created = await createProject(submitData)

    const projectId = (created as any)?.id

    // 根据是否邀请队友显示不同的成功消息
    if (form.inviteTeammates && selectedTeammates.value.size > 0) {
      ElMessage.success(
        `项目创建成功！已向 ${selectedTeammates.value.size} 位队友发送邀请（当前为草稿状态）`
      )
    } else {
      ElMessage.success('创建成功（当前为草稿，不会出现在项目广场）')
    }

    if (projectId) {
      try {
        await ElMessageBox.confirm(
          '项目已创建为草稿（DRAFT），需要发布后才会出现在项目广场。是否现在发布？',
          '提示',
          {
            confirmButtonText: '立即发布',
            cancelButtonText: '稍后再说',
            type: 'warning'
          }
        )

        await publishProject(projectId)
        ElMessage.success('发布成功，已进入招募中')
        emit('success')
      } catch (e: any) {
        // 用户取消或发布失败：不阻塞创建流程
      }
    }

    visible.value = false
    emit('success')

    // Reset
    activeStep.value = 0
    skillRequirements.value = []
    timeSlots.value = []
    recommendedTeammates.value = []
    selectedTeammates.value.clear()
    Object.assign(form, {
      title: '',
      projectType: 'COMPETITION',
      description: '',
      requirements: '',
      teamSize: 5,
      weeklyHours: 10,
      expectedDuration: 30,
      roles: [],
      techStack: [],
      teamMode: 'CREATE_NEW',
      existingTeamId: undefined,
      teamName: '',
      inviteTeammates: false,
      invitedUserIds: []
    })
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 加载用户的团队列表
const loadMyTeams = async () => {
  try {
    const userId = authStore.user?.id
    if (!userId) return
    
    const result = await getUserTeams(userId, {}, false)
    if (Array.isArray(result)) {
      // 只显示活跃的团队
      myTeams.value = result.filter((t: Team) => t.status === 'ACTIVE')
    } else if (result && 'records' in result) {
      myTeams.value = result.records.filter((t: Team) => t.status === 'ACTIVE')
    }
  } catch (error) {
    console.error('加载团队列表失败:', error)
    myTeams.value = []
  }
}

onMounted(() => {
  loadAvailableSkills()
  loadMyTeams()
})
</script>

<template>
  <el-dialog
    v-model="visible"
    title="创建新项目"
    width="640px"
    class="wizard-dialog"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="wizard-content">
      <!-- Steps -->
      <div class="steps-wrapper">
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step v-for="step in steps" :key="step.title" :title="step.title" />
        </el-steps>
      </div>

      <!-- Form Content -->
      <div class="step-container">
        <transition name="step-fade" mode="out-in">
          <div :key="activeStep" class="step-panel-wrapper">
            <div class="step-panel" v-if="activeStep === 0">
              <div class="form-group">
                <label>项目名称</label>
                <el-input v-model="form.title" placeholder="给你的项目起个响亮的名字" size="large" />
              </div>
              
              <div class="form-group">
                <label>项目类型</label>
                <div class="type-options">
                  <el-radio-group v-model="form.projectType" size="large">
                    <el-radio-button label="COMPETITION">竞赛</el-radio-button>
                    <el-radio-button label="RESEARCH">科研</el-radio-button>
                    <el-radio-button label="STARTUP">创业</el-radio-button>
                    <el-radio-button label="OPENSOURCE">开源</el-radio-button>
                  </el-radio-group>
                </div>
              </div>

              <div class="form-group">
                <label>项目描述</label>
                <RichTextEditor
                  v-model="form.description"
                  placeholder="介绍一下项目的背景、目标和愿景..."
                  :min-height="'150px'"
                  :max-length="2000"
                />
              </div>

              <div class="form-group">
                <label>需求描述</label>
                <RichTextEditor
                  v-model="form.requirements"
                  placeholder="详细描述项目的具体需求、功能点和技术要求..."
                  :min-height="'200px'"
                  :max-length="5000"
                />
              </div>
            </div>

            <div class="step-panel" v-if="activeStep === 1">
              <div class="form-group">
                <label>招募角色</label>
                <div class="tag-input-wrapper">
                  <el-input
                    v-model="newRole"
                    placeholder="输入角色按回车添加 (如: 前端工程师)"
                    @keyup.enter="addRole"
                  />
                  <div class="tags-container">
                    <el-tag
                      v-for="tag in form.roles"
                      :key="tag"
                      closable
                      @close="removeRole(tag)"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
              </div>

              <div class="form-group">
                <label>技能需求</label>
                <p style="color: var(--el-color-info); font-size: 12px; margin-bottom: 12px;">
                  从技能库中选择项目所需的技能,并标记是否必需
                </p>
                
                <!-- 已添加的技能需求 -->
                <div class="skill-requirements-list">
                  <div 
                    v-for="skill in skillRequirements" 
                    :key="skill.tagId"
                    class="skill-requirement-item"
                  >
                    <div 
                      class="portfolio-tag skill-req"
                      :data-level="skill.proficiencyLevel"
                      :data-required="skill.required"
                    >
                      <i class="level-dot"></i>
                      <span class="skill-name">{{ skill.skillName }}</span>
                      <span class="skill-divider">·</span>
                      <span class="skill-level">{{ getProficiencyLabel(skill.proficiencyLevel) }}</span>
                      <span class="skill-divider">·</span>
                      <span class="skill-required">{{ skill.required ? '必需' : '加分' }}</span>
                      <el-icon @click="removeSkillRequirement(skill.tagId)" class="remove-icon">
                        <CircleCloseFilled />
                      </el-icon>
                    </div>
                  </div>
                  
                  <el-button 
                    class="add-skill-btn"
                    @click="showSkillSelector = true"
                    :icon="Plus"
                  >
                    添加技能需求
                  </el-button>
                </div>
              </div>
            </div>

            <div class="step-panel" v-if="activeStep === 2">
              <div class="form-group">
                <label>团队规模 (人)</label>
                <el-slider v-model="form.teamSize" :min="2" :max="20" show-input />
              </div>

              <div class="form-group">
                <label>每周投入 (小时)</label>
                <el-slider v-model="form.weeklyHours" :min="5" :max="60" show-input />
              </div>

              <div class="form-group">
                <label>预计周期 (天)</label>
                <el-input-number v-model="form.expectedDuration" :min="7" :max="365" />
              </div>

              <div class="form-group">
                <label>期望可用时间段（可选）</label>
                <p style="color: var(--el-color-info); font-size: 12px; margin-bottom: 12px;">
                  设置项目期望成员可用的时间段，用于匹配时计算时间重叠度
                </p>
                <div class="skill-requirements-list">
                  <div v-for="(slot, index) in timeSlots" :key="index" class="skill-requirement-item">
                    <el-tag type="success" closable @close="removeTimeSlot(index)" size="large">
                      <span class="skill-name">{{ dayLabels[slot.dayOfWeek] }}</span>
                      <el-divider direction="vertical" />
                      <span class="skill-level">{{ slot.startTime }} - {{ slot.endTime }}</span>
                    </el-tag>
                  </div>
                  <el-button class="add-skill-btn" @click="showTimeSlotForm = true" :icon="Plus">
                    添加时间段
                  </el-button>
                </div>
              </div>
            </div>

            <div class="step-panel" v-if="activeStep === 3">
              <div class="form-group">
                <label>团队模式</label>
                <el-radio-group v-model="form.teamMode" size="large">
                  <el-radio label="CREATE_NEW">创建新团队</el-radio>
                  <el-radio label="USE_EXISTING" :disabled="myTeams.length === 0">使用已有团队</el-radio>
                </el-radio-group>
                <p v-if="myTeams.length === 0" style="color: var(--el-color-info); font-size: 12px; margin-top: 8px;">
                  你还没有可用的团队，将自动创建新团队
                </p>
              </div>

              <div v-if="form.teamMode === 'CREATE_NEW'" class="form-group">
                <label>团队名称</label>
                <el-input 
                  v-model="form.teamName" 
                  placeholder="为你的团队起个名字" 
                  size="large"
                  maxlength="50"
                  show-word-limit
                />
                <p style="color: var(--el-color-info); font-size: 12px; margin-top: 8px;">
                  项目招募完成后，将自动创建团队
                </p>
              </div>

              <!-- 邀请队友选项 -->
              <div v-if="form.teamMode === 'CREATE_NEW'" class="form-group">
                <el-checkbox v-model="form.inviteTeammates">
                  创建时邀请队友加入
                </el-checkbox>
                <p style="color: var(--el-color-info); font-size: 12px; margin-top: 8px;">
                  邀请你认识的队友，快速组建核心团队
                </p>
              </div>

              <!-- 邀请队友界面 -->
              <div v-if="form.inviteTeammates && form.teamMode === 'CREATE_NEW'" class="invite-teammates-section">
                <div class="section-header">
                  <span class="section-title">选择要邀请的队友</span>
                  <el-button 
                    size="small" 
                    @click="loadRecommendedTeammates"
                    :loading="teammatesLoading"
                  >
                    智能推荐
                  </el-button>
                </div>

                <!-- 推荐队友列表 -->
                <div v-if="recommendedTeammates.length > 0" class="teammates-grid">
                  <div 
                    v-for="teammate in recommendedTeammates" 
                    :key="teammate.userId"
                    class="teammate-card"
                    :class="{ selected: selectedTeammates.has(teammate.userId) }"
                    @click="toggleTeammate(teammate.userId)"
                  >
                    <el-avatar 
                      :size="40" 
                      :src="teammate.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
                    />
                    <div class="teammate-info">
                      <span class="name">{{ teammate.username || '未设置昵称' }}</span>
                      <span class="match-score">匹配度: {{ Math.round((teammate.score || 0) * 100) }}%</span>
                      <span class="match-explanation">{{ getMatchExplanation(teammate) }}</span>
                    </div>
                    <el-icon v-if="selectedTeammates.has(teammate.userId)" class="check-icon" color="#67c23a">
                      <component :is="'Check'" />
                    </el-icon>
                  </div>
                </div>

                <!-- 空状态 -->
                <div v-else-if="!teammatesLoading" class="empty-teammates">
                  <p>点击"智能推荐"按钮获取推荐队友</p>
                </div>

                <!-- 已选择的队友 -->
                <div v-if="selectedTeammates.size > 0" class="selected-teammates">
                  <el-tag type="success">已选择 {{ selectedTeammates.size }} 位队友</el-tag>
                </div>
              </div>

              <div v-if="form.teamMode === 'USE_EXISTING'" class="form-group">
                <label>选择团队</label>
                <el-select 
                  v-model="form.existingTeamId" 
                  placeholder="选择一个已有团队" 
                  size="large"
                  style="width: 100%"
                >
                  <el-option
                    v-for="team in myTeams"
                    :key="team.id"
                    :label="team.teamName"
                    :value="team.id"
                  >
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                      <span>{{ team.teamName }}</span>
                      <el-tag v-if="team.teamNature === 'LONG_TERM'" type="success" size="small">长期团队</el-tag>
                      <el-tag v-else type="info" size="small">临时团队</el-tag>
                    </div>
                  </el-option>
                </el-select>
                <p style="color: var(--el-color-info); font-size: 12px; margin-top: 8px;">
                  选择一个已有团队来执行此项目
                </p>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <template #footer>
      <div class="wizard-footer">
        <el-button v-if="activeStep > 0" @click="handlePrev">上一步</el-button>
        <el-button type="primary" @click="handleNext" :loading="loading">
          {{ activeStep === 3 ? '完成创建' : '下一步' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
  
  <!-- 技能选择对话框 -->
  <el-dialog 
    v-model="showSkillSelector" 
    title="添加技能需求" 
    width="500px"
    append-to-body
  >
    <el-form :model="selectedSkill" label-position="top">
      <el-form-item label="选择技能">
        <el-select 
          v-model="selectedSkill.tagId" 
          filterable 
          placeholder="搜索并选择技能"
          style="width: 100%"
        >
          <el-option
            v-for="skill in availableSkills"
            :key="skill.id"
            :label="skill.name"
            :value="skill.id"
            :disabled="skillRequirements.some(s => s.tagId === skill.id)"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="是否必需">
        <el-radio-group v-model="selectedSkill.required">
          <el-radio :label="true">必需技能</el-radio>
          <el-radio :label="false">加分技能</el-radio>
        </el-radio-group>
        <p style="color: var(--el-color-info); font-size: 12px; margin-top: 8px;">
          必需技能会在匹配时占更高权重
        </p>
      </el-form-item>
      
      <el-form-item label="熟练度要求">
        <el-radio-group v-model="selectedSkill.proficiencyLevel">
          <el-radio-button label="BEGINNER">入门</el-radio-button>
          <el-radio-button label="INTERMEDIATE">熟练</el-radio-button>
          <el-radio-button label="ADVANCED">高级</el-radio-button>
          <el-radio-button label="EXPERT">精通</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="showSkillSelector = false">取消</el-button>
      <el-button type="primary" @click="handleAddSkillRequirement">
        确认添加
      </el-button>
    </template>
  </el-dialog>

  <!-- 时间段选择对话框 -->
  <el-dialog
    v-model="showTimeSlotForm"
    title="添加期望时间段"
    width="450px"
    append-to-body
  >
    <el-form :model="newTimeSlot" label-position="top">
      <el-form-item label="星期">
        <el-select v-model="newTimeSlot.dayOfWeek" style="width: 100%">
          <el-option v-for="d in 7" :key="d" :label="dayLabels[d]" :value="d" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间">
        <el-time-select
          v-model="newTimeSlot.startTime"
          :max-time="newTimeSlot.endTime"
          placeholder="开始时间"
          start="06:00" end="23:00" step="00:30"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-time-select
          v-model="newTimeSlot.endTime"
          :min-time="newTimeSlot.startTime"
          placeholder="结束时间"
          start="06:00" end="23:30" step="00:30"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showTimeSlotForm = false">取消</el-button>
      <el-button type="primary" @click="handleAddTimeSlot">确认添加</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.wizard-content {
  padding: 20px 10px;
  min-height: 400px;
}

.steps-wrapper {
  margin-bottom: 40px;
}

.form-group {
  margin-bottom: 24px;
  
  label {
    display: block;
    margin-bottom: 8px;
    font-weight: 600;
    color: var(--text-color);
  }
}

.type-options {
  display: flex;
  gap: 10px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.wizard-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Custom Dialog Styles Override */
:deep(.el-dialog__body) {
  padding-top: 10px;
  padding-bottom: 10px;
}

.step-fade-enter-active,
.step-fade-leave-active {
  transition: all 0.3s ease;
}

.step-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.step-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.skill-requirements-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 100px;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.skill-requirement-item {
  .portfolio-tag.skill-req {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    background: var(--bg-card);
    border: 1.5px solid var(--border-color);
    border-radius: 20px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-color);
    transition: all 0.2s ease;
    cursor: default;
    
    &:hover {
      background: var(--bg-card);
      border-color: var(--accent-color);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      
      .remove-icon { 
        opacity: 1; 
        transform: scale(1);
      }
    }

    .level-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #94a3b8;
      flex-shrink: 0;
    }

    &[data-level="EXPERT"] {
      .level-dot { background: #10b981; }
      border-color: rgba(16, 185, 129, 0.2);
    }
    
    &[data-level="ADVANCED"] {
      .level-dot { background: var(--accent-color); }
      border-color: rgba(var(--accent-color-rgb), 0.2);
    }
    
    &[data-level="INTERMEDIATE"] {
      .level-dot { background: #94a3b8; }
      border-color: rgba(148, 163, 184, 0.2);
    }
    
    &[data-level="BEGINNER"] {
      .level-dot { background: #cbd5e1; }
      border-color: rgba(203, 213, 225, 0.2);
    }

    // 必需技能加粗边框
    &[data-required="true"] {
      border-width: 2px;
      font-weight: 600;
    }

    .skill-name {
      font-weight: 600;
    }
    
    .skill-divider {
      opacity: 0.3;
      margin: 0 2px;
    }
    
    .skill-level {
      font-size: 12px;
      opacity: 0.8;
    }
    
    .skill-required {
      font-size: 12px;
      font-weight: 600;
      color: var(--accent-color);
    }

    .remove-icon {
      font-size: 14px;
      cursor: pointer;
      opacity: 0;
      transform: scale(0.8);
      transition: all 0.2s ease;
      color: var(--el-color-danger);
      margin-left: 2px;

      &:hover {
        color: #ff4d4f;
        transform: scale(1.2);
      }
    }
  }
}

.add-skill-btn {
  border-style: dashed;
}

/* 邀请队友样式 */
.invite-teammates-section {
  margin-top: 16px;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.teammates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.teammate-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 2px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  &.selected {
    border-color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }
}

.teammate-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .name {
    font-weight: 500;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }

  .match-score {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .match-explanation {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: normal;
  }
}

.check-icon {
  font-size: 20px;
}

.empty-teammates {
  text-align: center;
  padding: 32px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.selected-teammates {
  margin-top: 12px;
  text-align: center;
}
</style>
