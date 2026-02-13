<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  InfoFilled,
  School,
  Reading,
  Calendar,
  ChatDotRound,
  User,
  Connection,
  Star,
  Trophy,
  Cpu,
  MagicStick,
  CircleCloseFilled,
  Close,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { getProfile, updateProfile, getUserSkills, removeUserSkill } from '@/api/profile'
import { request } from '@/utils/request'
import type { UserProfile, UserSkill } from '@/types/user'

import ProfileHeader from '@/components/profile/ProfileHeader.vue'
import AbilityRadar from '@/components/charts/AbilityRadar.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import ProfileSkeleton from '@/components/skeleton/ProfileSkeleton.vue'
import ImageUpload from '@/components/common/ImageUpload.vue'
import { getDepartmentMajorPreset, type DepartmentMajorDict } from '@/utils/departmentMajorPreset'

import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const authStore = useAuthStore()
const loading = ref(false)
const pageLoading = ref(true)
const isEditing = ref(false)
const showSkillDialog = ref(false)
const showTagDialog = ref(false)
const currentTagCategory = ref<'INTEREST' | 'PERSONALITY' | 'PROJECT_TYPE'>('INTEREST')
const profileFormRef = ref<any>()

// Skills & Tags state
const availableTags = ref<any[]>([])
const selectedSkill = ref({
  tagId: null as number | null,
  proficiencyLevel: 'INTERMEDIATE'
})

const availableInterests = ref<any[]>([])
const availablePersonalities = ref<any[]>([])
const availableProjectTypes = ref<any[]>([])
const userInterests = ref<any[]>([])
const userPersonalities = ref<any[]>([])
const userProjectTypes = ref<any[]>([])
const selectedTag = ref<number | null>(null)

// Department/Major data
const departmentMajorData = ref<DepartmentMajorDict[]>([])

const userProfile = reactive<Partial<UserProfile>>({
  realName: '',
  department: '',
  major: '',
  grade: 1,
  wechat: '',
  qq: '',
  bio: '',
  projectExperience: ''
})

const userSkills = ref<UserSkill[]>([])

// 表单验证规则
const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请选择院系', trigger: 'change' }],
  major: [{ required: true, message: '请选择专业', trigger: 'change' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
  wechat: [{ required: true, message: '请输入微信号', trigger: 'blur' }],
  qq: [{ required: true, message: '请输入QQ号', trigger: 'blur' }],
  bio: [{ required: true, message: '请输入个人简介', trigger: 'blur' }],
  projectExperience: [{ required: true, message: '请输入项目经验', trigger: 'blur' }],
}

// Smart truncation that respects word boundaries
const truncateText = (text: string | undefined, maxLength: number): string | undefined => {
  if (!text) return text
  if (text.length <= maxLength) return text
  const truncated = text.substring(0, maxLength)
  const lastSpace = truncated.lastIndexOf(' ')
  if (lastSpace > maxLength * 0.6) {
    return truncated.substring(0, lastSpace) + '...'
  }
  return truncated + '...'
}

const departments = computed(() => departmentMajorData.value.map(d => d.department))

const majorsForDepartment = computed(() => {
  const dept = String(userProfile.department || '')
  const found = departmentMajorData.value.find(d => d.department === dept)
  return found ? found.majors : []
})

const handleDepartmentChange = () => {
  const majors = majorsForDepartment.value
  if (userProfile.major && !majors.includes(String(userProfile.major))) {
    userProfile.major = ''
  }
}

// Radar Chart Logic - 真实数据计算
const radarData = computed(() => {
  let techDepth = 0
  userSkills.value.forEach(skill => {
    const proficiencyWeight = {
      'BEGINNER': 0.25,
      'INTERMEDIATE': 0.5,
      'ADVANCED': 0.75,
      'EXPERT': 1.0
    }[skill.proficiencyLevel || 'INTERMEDIATE'] || 0.5
    techDepth += proficiencyWeight * 10
  })
  techDepth = Math.min(techDepth, 100)
  
  const totalInterests = userInterests.value.length
  const totalProjectTypes = userProjectTypes.value.length
  const collaboration = Math.min((totalInterests * 8 + totalProjectTypes * 12), 100)
  
  const experienceLength = (userProfile?.projectExperience?.length || 0)
  const skillCount = userSkills.value.length
  const experience = Math.min((experienceLength / 10) + (skillCount * 5), 100)
  
  const personalityCount = userPersonalities.value.length
  const bioLength = (userProfile?.bio?.length || 0)
  const hasContact = (userProfile?.wechat || userProfile?.qq) ? 20 : 0
  const activity = Math.min((personalityCount * 10) + (bioLength / 5) + hasContact, 100)
  
  let professionalMatch = 0
  if (userProfile?.department) professionalMatch += 25
  if (userProfile?.major) professionalMatch += 25
  if (userProfile?.grade) professionalMatch += 15
  if (userProfile?.realName) professionalMatch += 15
  professionalMatch += Math.min(userSkills.value.length * 2, 20)
  professionalMatch = Math.min(professionalMatch, 100)
  
  return {
    '技术深度': Math.round(techDepth),
    '跨项协作': Math.round(collaboration),
    '实践经验': Math.round(experience),
    '活跃程度': Math.round(activity),
    '专业匹配': Math.round(professionalMatch)
  }
})

const loadProfile = async () => {
  pageLoading.value = true
  try {
    if (!authStore.user?.id) return

    const [deptMajorData] = await Promise.all([
      getDepartmentMajorPreset(),
      loadAvailableTags(),
      loadAvailableInterests(),
      loadAvailablePersonalities(),
      loadAvailableProjectTypes()
    ])
    
    departmentMajorData.value = deptMajorData

    const [profileRes, skillsRes, interestsRes, personalitiesRes, projectTypesRes] = await Promise.all([
      getProfile(authStore.user.id),
      getUserSkills(authStore.user.id),
      request.get(`/user-tags/${authStore.user.id}/tags/INTEREST`),
      request.get(`/user-tags/${authStore.user.id}/tags/PERSONALITY`),
      request.get(`/user-tags/${authStore.user.id}/tags/PROJECT_TYPE`)
    ])
    
    if (profileRes?.data) Object.assign(userProfile, profileRes.data)
    if (skillsRes?.data) userSkills.value = skillsRes.data
    if (interestsRes?.data) userInterests.value = interestsRes.data
    if (personalitiesRes?.data) userPersonalities.value = personalitiesRes.data
    if (projectTypesRes?.data) userProjectTypes.value = projectTypesRes.data
    
  } catch (error) {
    console.error('Failed to load profile:', error)
  } finally {
    pageLoading.value = false
  }
}

const loadAvailableTags = () => request.get('/tags/skills').then(res => availableTags.value = res.data || [])
const loadAvailableInterests = () => request.get('/tags/interests').then(res => availableInterests.value = res.data || [])
const loadAvailablePersonalities = () => request.get('/tags/personalities').then(res => availablePersonalities.value = res.data || [])
const loadAvailableProjectTypes = () => request.get('/tags/project-types').then(res => availableProjectTypes.value = res.data || [])

const handleSave = async () => {
  if (!authStore.user?.id) return
  
  // 验证表单
  if (!profileFormRef.value) return
  
  try {
    await profileFormRef.value.validate()
  } catch (error) {
    ElMessage.warning('请填写所有必填项')
    return
  }

  loading.value = true
  try {
    const updatedProfile = await updateProfile(authStore.user.id, userProfile)
    if (updatedProfile.data) {
      // 更新 auth store 中的用户信息
      authStore.setUser({ ...authStore.user, profile: updatedProfile.data })
    }
    
    // 刷新用户信息（确保 localStorage 同步）
    await authStore.refreshUserInfo()
    
    ElMessage.success('个人资料已更新')
    isEditing.value = false
    await loadProfile()
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleCloseTag = async (skill: UserSkill) => {
  try {
    await removeUserSkill(skill.id!)
    userSkills.value = userSkills.value.filter(s => s.id !== skill.id)
    ElMessage.success('技能已移除')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleAddSkill = async () => {
  if (!selectedSkill.value.tagId || !authStore.user?.id) return
  try {
    await request.post(`/user-tags/${authStore.user.id}/skills`, selectedSkill.value)
    ElMessage.success('技能添加成功')
    showSkillDialog.value = false
    await loadProfile()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '添加失败')
  }
}

const handleAddTag = async () => {
  if (!selectedTag.value || !authStore.user?.id) return
  try {
    await request.post(`/user-tags/${authStore.user.id}/tags`, { tagId: selectedTag.value })
    ElMessage.success('标签添加成功')
    showTagDialog.value = false
    await loadProfile()
  } catch (error: any) {
    ElMessage.error('添加失败')
  }
}

const handleRemoveTag = async (tagId: number) => {
  try {
    await request.delete(`/user-tags/tags/${tagId}`)
    await loadProfile()
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

const isTagSelected = (id: number) => userSkills.value.some(s => s.tagId === id)
const isCurrentTagSelected = (id: number) => {
  if (currentTagCategory.value === 'INTEREST') return userInterests.value.some(t => t.tagId === id)
  if (currentTagCategory.value === 'PERSONALITY') return userPersonalities.value.some(t => t.tagId === id)
  return userProjectTypes.value.some(t => t.tagId === id)
}

const showAddTagDialog = (category: 'INTEREST' | 'PERSONALITY' | 'PROJECT_TYPE') => {
  currentTagCategory.value = category
  selectedTag.value = null
  showTagDialog.value = true
}

const getTagCategoryName = (category: string) => {
  const names: Record<string, string> = {
    INTEREST: '兴趣',
    PERSONALITY: '性格',
    PROJECT_TYPE: '项目类型'
  }
  return names[category] || category
}

const getCurrentAvailableTags = computed(() => {
  if (currentTagCategory.value === 'INTEREST') return availableInterests.value
  if (currentTagCategory.value === 'PERSONALITY') return availablePersonalities.value
  return availableProjectTypes.value
})

onMounted(loadProfile)
</script>

<template>
  <div class="portfolio-page">
    <div class="aura-background"></div>
    
    <div class="content-container">
      <ProfileSkeleton v-if="pageLoading" />
      
      <template v-else>
        <!-- Hero Section -->
        <ProfileHeader 
          :user="{
            name: userProfile.realName || authStore.user?.username || 'User',
            avatar: authStore.user?.avatar,
            tagline: userProfile.bio,
            role: authStore.user?.roles?.[0]
          }" 
          editable 
          @edit="isEditing = true" 
        />

        <!-- Asymmetric Masonry Grid -->
        <div class="portfolio-grid">
          
          <!-- Column 1: Main Story & Professional Path -->
          <div class="grid-col main-story">
            <GlassCard class="story-card intro-card animate-slide-up">
              <div class="section-label">
                <el-icon><User /></el-icon> 个人自述
              </div>
              <h2 class="editorial-title">Hello, <br/>I'm {{ userProfile.realName || authStore.user?.username }}.</h2>
              <p class="story-text">{{ userProfile.bio || '探索技术边界，寻找志同道合的创意伙伴。暂无个人简介。' }}</p>
            </GlassCard>

            <!-- Ability Radar Section: Split into its own card -->
            <GlassCard class="story-card radar-panel animate-slide-up" style="animation-delay: 0.1s">
              <div class="section-label">
                <el-icon><Cpu /></el-icon> 能力维度
              </div>
              <div class="radar-display">
                <AbilityRadar :data="radarData" />
              </div>
            </GlassCard>

            <!-- Core Skills Section: Split into its own card -->
            <GlassCard class="story-card skills-panel animate-slide-up" style="animation-delay: 0.15s">
              <div class="section-header">
                <div class="section-label">
                  <el-icon><MagicStick /></el-icon> 核心技能
                </div>
                <div class="skill-legend">
                  <span class="legend-item expert"><i class="dot"></i> 精通</span>
                  <span class="legend-item advanced"><i class="dot"></i> 高级</span>
                  <span class="legend-item intermediate"><i class="dot"></i> 熟练</span>
                </div>
              </div>
              <div class="skills-content">
                <div class="tags-row">
                  <div 
                    v-for="skill in userSkills" 
                    :key="skill.id" 
                    class="portfolio-tag skill"
                    :data-level="skill.proficiencyLevel"
                  >
                    <i class="level-dot"></i>
                    {{ skill.skillName }}
                    <el-icon @click="handleCloseTag(skill)" class="remove-icon"><CircleCloseFilled /></el-icon>
                  </div>
                  <button class="add-tag-btn" @click="showSkillDialog = true">+</button>
                </div>
              </div>
            </GlassCard>

            <GlassCard class="story-card experience-card animate-slide-up" style="animation-delay: 0.2s">
              <div class="section-label">
                <el-icon><Trophy /></el-icon> 项目履历
              </div>
              <div class="experience-text">
                <MarkdownViewer
                  :content="userProfile.projectExperience || '正在积累精彩的项目瞬间...'"
                />
              </div>
            </GlassCard>
          </div>

          <!-- Column 2: Specs & Personal Attributes -->
          <div class="grid-col side-specs">
            <GlassCard class="spec-card info-panel animate-slide-up" style="animation-delay: 0.15s">
              <div class="section-label">
                <el-icon><InfoFilled /></el-icon> 规格数据
              </div>
              <div class="spec-list">
                <div class="spec-item">
                  <span class="spec-icon"><School /></span>
                  <div class="spec-body">
                    <label>院系</label>
                    <span class="value">{{ userProfile.department || '未填写' }}</span>
                  </div>
                </div>
                <div class="spec-item">
                  <span class="spec-icon"><Reading /></span>
                  <div class="spec-body">
                    <label>专业</label>
                    <span class="value">{{ userProfile.major || '未填写' }}</span>
                  </div>
                </div>
                <div class="spec-item">
                  <span class="spec-icon"><Calendar /></span>
                  <div class="spec-body">
                    <label>年级</label>
                    <span class="value">{{ userProfile.grade }} Year</span>
                  </div>
                </div>
                <div class="spec-divider"></div>
                <div class="spec-item contact">
                  <span class="spec-icon"><ChatDotRound /></span>
                  <div class="spec-body">
                    <label>联系方式 (WeChat)</label>
                    <span class="value accent">{{ userProfile.wechat || 'Private' }}</span>
                  </div>
                </div>
              </div>
            </GlassCard>

            <GlassCard class="spec-card tags-panel animate-slide-up" style="animation-delay: 0.3s">
              <div class="section-header">
                <div class="section-label"><el-icon><Connection /></el-icon> 兴趣领域</div>
                <button class="mini-add-btn" @click="showAddTagDialog('INTEREST')">+</button>
              </div>
              <div class="tag-pool">
                <span v-for="tag in userInterests" :key="tag.id" class="pool-tag interest">
                  {{ tag.tagName }}
                  <el-icon @click="handleRemoveTag(tag.id)" class="tag-del"><Close /></el-icon>
                </span>
              </div>

              <div class="section-header mt-20">
                <div class="section-label"><el-icon><MagicStick /></el-icon> 个人特质</div>
                <button class="mini-add-btn" @click="showAddTagDialog('PERSONALITY')">+</button>
              </div>
              <div class="tag-pool">
                <span v-for="tag in userPersonalities" :key="tag.id" class="pool-tag personality">
                  {{ tag.tagName }}
                  <el-icon @click="handleRemoveTag(tag.id)" class="tag-del"><Close /></el-icon>
                </span>
              </div>

              <div class="section-header mt-20">
                <div class="section-label"><el-icon><Star /></el-icon> 偏好类型</div>
                <button class="mini-add-btn" @click="showAddTagDialog('PROJECT_TYPE')">+</button>
              </div>
              <div class="tag-pool">
                <span v-for="tag in userProjectTypes" :key="tag.id" class="pool-tag type">
                  {{ tag.tagName }}
                  <el-icon @click="handleRemoveTag(tag.id)" class="tag-del"><Close /></el-icon>
                </span>
              </div>
            </GlassCard>
          </div>

        </div>
      </template>
    </div>

    <!-- Modals (Preserved logic) -->
    <!-- Add Skill Dialog -->
    <el-dialog v-model="showSkillDialog" title="掌握新技能" width="460px" class="portfolio-dialog">
      <el-form :model="selectedSkill" label-position="top">
        <el-form-item label="选择技能标签">
          <el-select v-model="selectedSkill.tagId" filterable class="full-width">
            <el-option v-for="tag in availableTags" :key="tag.id" :label="tag.name" :value="tag.id" :disabled="isTagSelected(tag.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="熟练度等级">
          <el-radio-group v-model="selectedSkill.proficiencyLevel" class="level-radios">
            <el-radio-button label="BEGINNER">入门</el-radio-button>
            <el-radio-button label="INTERMEDIATE">熟练</el-radio-button>
            <el-radio-button label="ADVANCED">高级</el-radio-button>
            <el-radio-button label="EXPERT">精通</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showSkillDialog = false" round>放弃</el-button>
          <el-button type="primary" @click="handleAddSkill" round>确认添加</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Tag Dialog -->
    <el-dialog v-model="showTagDialog" :title="`添加${getTagCategoryName(currentTagCategory)}`" width="400px" class="portfolio-dialog">
      <el-select v-model="selectedTag" filterable class="full-width">
        <el-option v-for="tag in getCurrentAvailableTags" :key="tag.id" :label="tag.name" :value="tag.id" :disabled="isCurrentTagSelected(tag.id)" />
      </el-select>
      <template #footer>
        <el-button type="primary" @click="handleAddTag" round block>确认添加</el-button>
      </template>
    </el-dialog>

    <!-- Edit Profile Dialog -->
    <el-dialog v-model="isEditing" title="完善个人资料" width="620px" class="portfolio-dialog">
      <el-form :model="userProfile" :rules="profileRules" ref="profileFormRef" label-position="top" class="edit-form">
        <div class="form-hero">
          <ImageUpload v-model="authStore.user.avatar" type="avatar" />
          <div class="form-hero-info">
            <el-form-item label="真实姓名" prop="realName" required>
              <el-input v-model="userProfile.realName" placeholder="请输入你的真实姓名" />
            </el-form-item>
          </div>
        </div>
        
        <el-form-item label="个人简介" prop="bio" required>
          <el-input v-model="userProfile.bio" type="textarea" :rows="3" placeholder="简单介绍一下自己..." />
        </el-form-item>
        
        <div class="form-row">
          <el-form-item label="院系" prop="department" class="flex-1" required>
            <el-select v-model="userProfile.department" filterable @change="handleDepartmentChange">
              <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业" prop="major" class="flex-1" required>
            <el-select v-model="userProfile.major" filterable :disabled="!userProfile.department">
              <el-option v-for="major in majorsForDepartment" :key="major" :label="major" :value="major" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="年级" prop="grade" class="flex-1" required>
            <el-input-number v-model="userProfile.grade" :min="1" :max="5" class="full-width" />
          </el-form-item>
          <el-form-item label="微信号" prop="wechat" class="flex-1" required>
            <el-input v-model="userProfile.wechat" placeholder="方便队友联系你" />
          </el-form-item>
        </div>

        <el-form-item label="QQ号" prop="qq" required>
          <el-input v-model="userProfile.qq" placeholder="请输入QQ号" />
        </el-form-item>

        <el-form-item label="项目经历" prop="projectExperience" required>
          <el-input v-model="userProfile.projectExperience" type="textarea" :rows="4" placeholder="分享你参与过的项目经历..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isEditing = false" round>取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="loading" round>保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.portfolio-page {
  min-height: 100vh;
  position: relative;
  padding-bottom: 80px;
  overflow-x: hidden;
}

.aura-background {
  position: fixed;
  inset: 0;
  z-index: -1;
  background: var(--bg-body);
  
  &::before, &::after {
    content: '';
    position: absolute;
    width: 800px;
    height: 800px;
    border-radius: 50%;
    filter: blur(120px);
    opacity: 0.15;
    pointer-events: none;
  }

  &::before {
    top: -200px;
    left: -200px;
    background: radial-gradient(circle, var(--accent-color), transparent 70%);
    animation: flow 20s infinite alternate;
  }

  &::after {
    bottom: -200px;
    right: -200px;
    background: radial-gradient(circle, #8b5cf6, transparent 70%);
    animation: flow 25s infinite alternate-reverse;
  }
}

@keyframes flow {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(100px, 100px) scale(1.2); }
}

.content-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 0 24px;
}

.portfolio-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 32px;
  align-items: start;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.grid-col {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--accent-color);
  margin-bottom: 24px;
  opacity: 0.8;
}

.editorial-title {
  font-size: 56px;
  font-weight: 900;
  line-height: 1.1;
  margin: 0 0 24px;
  color: var(--text-color);
  letter-spacing: -0.03em;
  
  @media (max-width: 768px) {
    font-size: 42px;
  }
}

.story-text, .experience-text {
  font-size: 16px;
  line-height: 1.6;
  color: var(--text-color-muted);
  max-width: 95%;
  word-break: break-word;
}

.story-card {
  padding: 32px;
  border-radius: 24px;
  
  &.intro-card {
    background: transparent;
    border: none;
    box-shadow: none;
    backdrop-filter: none;
    padding-left: 0;
  }
}

.radar-display {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.skills-content {
  padding-top: 10px;
}

.skill-cloud {
  h3 {
    font-size: 14px;
    font-weight: 700;
    margin-bottom: 16px;
    color: var(--text-color);
  }
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.portfolio-tag {
  padding: 8px 18px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--bg-body);
  border: 1px solid var(--border-subtle);
  color: var(--text-color-muted);
  cursor: default;

  &:hover {
    background: var(--bg-card);
    border-color: var(--accent-color);
    color: var(--text-color);
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

.skill-legend {
  display: flex;
  gap: 16px;
  align-items: center;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-color-muted);

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    &.expert .dot { background: #10b981; }
    &.advanced .dot { background: var(--accent-color); }
    &.intermediate .dot { background: #94a3b8; }
  }
}

.add-tag-btn {
  width: 36px;
  height: 36px;
  border-radius: 100px;
  border: 2px dashed var(--border-subtle);
  background: transparent;
  color: var(--text-color-muted);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--accent-color);
    color: var(--accent-color);
    background: rgba(var(--accent-color-rgb), 0.05);
  }
}

/* Side Spec Panel */
.spec-card {
  padding: 32px;
  border-radius: 32px;
}

.spec-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.spec-item {
  display: flex;
  gap: 16px;
  align-items: center;

  .spec-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: rgba(var(--accent-color-rgb), 0.1);
    color: var(--accent-color);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
  }

  .spec-body {
    display: flex;
    flex-direction: column;
    
    label {
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      color: var(--text-color-muted);
      letter-spacing: 0.05em;
    }
    .value {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color);
      
      &.accent { color: var(--accent-color); }
    }
  }
}

.spec-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 10px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tag-pool {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pool-tag {
  @extend .portfolio-tag;
  padding: 6px 16px;
  
  .tag-del {
    @extend .remove-icon;
  }

  &:hover {
    .tag-del {
      opacity: 1;
      transform: scale(1);
    }
  }
}

.mini-add-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-color-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  
  &:hover {
    border-color: var(--accent-color);
    color: var(--accent-color);
  }
}

.mt-20 { margin-top: 24px; }

/* Animations */
.animate-slide-up {
  opacity: 0;
  animation: slideUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Modals & Inputs */
.full-width { width: 100%; }
.flex-1 { flex: 1; }
.form-row { display: flex; gap: 20px; }
.form-hero {
  display: flex;
  gap: 24px;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(var(--accent-color-rgb), 0.05);
  border-radius: 20px;
}
.form-hero-info { flex: 1; }

:deep(.portfolio-dialog) {
  border-radius: 20px;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.1);

  .el-dialog__header {
    margin-right: 0;
    padding: 24px 24px 12px;
    border-bottom: 1px solid var(--border-subtle);
    .el-dialog__title { 
      font-weight: 700; 
      font-size: 18px; 
      color: var(--text-color);
    }
  }
  
  .el-dialog__body { 
    padding: 24px; 
  }

  .el-form-item__label {
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--text-color-muted);
    margin-bottom: 4px;
  }
  
  .el-input__wrapper, .el-textarea__wrapper, .el-select__wrapper {
    border-radius: 8px;
    box-shadow: none !important;
    background: var(--bg-body);
    border: 1px solid var(--border-subtle);
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    
    &:hover { 
      border-color: var(--text-color-muted);
    }
    
    &.is-focus { 
      border-color: var(--accent-color);
      background: var(--bg-card);
      box-shadow: 0 0 0 3px rgba(var(--accent-color-rgb), 0.1) !important;
    }
  }

  .el-dialog__footer {
    padding: 16px 24px 24px;
    border-top: 1px solid var(--border-subtle);
  }

  .el-button {
    font-weight: 600;
    border-radius: 8px;
    padding: 10px 20px;
    
    &--primary {
      background: var(--accent-color);
      border-color: var(--accent-color);
      &:hover {
        opacity: 0.9;
        transform: translateY(-1px);
      }
    }
  }
}

[data-theme='dark'] :deep(.portfolio-dialog) {
  background: #1a1b1e; /* Deep solid charcoal like Linear/Notion */
  border-color: #2c2e33;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);

  .el-dialog__header, .el-dialog__footer {
    border-color: #2c2e33;
  }

  .el-input__wrapper, .el-textarea__wrapper, .el-select__wrapper {
    background: #141517;
    border-color: #2c2e33;
    
    &:hover { border-color: #3f4147; }
    &.is-focus { 
      background: #1a1b1e;
      border-color: var(--accent-color);
    }
  }
}
</style>
