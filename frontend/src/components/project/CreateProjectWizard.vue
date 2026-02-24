<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Project, ProjectType } from '@/types/project'
import { createProject, publishProject } from '@/api/project'
import { getUserTeams } from '@/api/team'
import { useAuthStore } from '@/store/auth'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import type { Team } from '@/types/team'

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

const form = reactive<Partial<Project> & { teamMode?: string; existingTeamId?: number; teamName?: string }>({
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
  teamName: ''
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
      teamMode: form.teamMode
    }

    // 根据团队模式添加相应字段
    if (form.teamMode === 'USE_EXISTING') {
      submitData.existingTeamId = form.existingTeamId
    } else {
      submitData.teamName = form.teamName
    }

    const created = await createProject(submitData)

    const projectId = (created as any)?.id

    ElMessage.success('创建成功（当前为草稿，不会出现在项目广场）')

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
      teamName: ''
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
                <label>技术栈要求</label>
                <div class="tag-input-wrapper">
                  <el-input
                    v-model="newStack"
                    placeholder="输入技术按回车添加 (如: Vue3)"
                    @keyup.enter="addStack"
                  />
                  <div class="tags-container">
                    <el-tag
                      v-for="tag in form.techStack"
                      :key="tag"
                      closable
                      @close="removeStack(tag)"
                      type="success"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
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
</style>
