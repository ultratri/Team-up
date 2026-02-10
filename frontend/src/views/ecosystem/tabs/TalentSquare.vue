<template>
  <div class="talent-square">
    <!-- Tab 切换 -->
    <section class="ts-tabs">
      <el-segmented v-model="activeTab" :options="tabOptions" size="large" />
    </section>

    <!-- 筛选控制栏 -->
    <section class="ts-controls">
      <div class="ts-controls__left">
        <div class="ts-filter">
          <div class="ts-filter__label">院系</div>
          <el-select v-model="filters.department" placeholder="全部" clearable @change="handleSearch" style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="计算机学院" value="计算机学院" />
            <el-option label="软件学院" value="软件学院" />
            <el-option label="信息学院" value="信息学院" />
          </el-select>
        </div>
        
        <div class="ts-filter">
          <div class="ts-filter__label">关键词</div>
          <el-input 
            v-model="filters.keyword" 
            placeholder="搜索姓名、技能..." 
            clearable 
            @keyup.enter="handleSearch"
            style="width: 200px;"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <div class="ts-controls__right">
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <!-- 导师列表 -->
    <div v-if="loading" class="ts-loading">
      <div class="ts-grid">
        <div v-for="i in 6" :key="i" class="ts-skeleton-card">
          <el-skeleton animated>
            <template #template>
              <div class="skeleton-header">
                <el-skeleton-item variant="circle" style="width: 60px; height: 60px" />
                <div class="skeleton-ident">
                  <el-skeleton-item variant="text" style="width: 40%" />
                  <el-skeleton-item variant="text" style="width: 60%" />
                </div>
              </div>
              <el-skeleton-item variant="p" style="margin-top: 16px" />
              <el-skeleton-item variant="button" style="width: 100%; height: 44px; margin-top: 20px" />
            </template>
          </el-skeleton>
        </div>
      </div>
    </div>

    <div v-else-if="mentors.length === 0" class="ts-empty">
      <el-empty description="暂无相关导师">
        <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
      </el-empty>
    </div>

    <TransitionGroup v-else name="ts-grid" tag="div" class="ts-grid">
      <MentorCard
        v-for="mentor in mentors"
        :key="mentor.id"
        :mentor="mentor"
        @view="handleViewDetail"
      />
    </TransitionGroup>

    <!-- 分页 -->
    <div v-if="total > 0" class="ts-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="prev, pager, next"
        background
        @current-change="loadMentors"
        @size-change="loadMentors"
      />
    </div>

    <!-- 导师详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      width="800px"
      :show-close="false"
      destroy-on-close
      class="mentor-detail-dialog"
    >
      <div v-if="selectedMentor" class="mentor-detail">
        <!-- 自定义关闭按钮 -->
        <button class="close-btn" @click="showDetailDialog = false" aria-label="关闭">
          <el-icon><Close /></el-icon>
        </button>

        <!-- 顶部卡片 -->
        <div class="mentor-card-top">
          <div class="mentor-avatar-section">
            <el-avatar :size="100" :src="selectedMentor.avatarUrl" class="mentor-avatar-large">
              {{ selectedMentor.realName?.charAt(0) }}
            </el-avatar>
            <div class="mentor-rating-badge">
              <el-icon><Star /></el-icon>
              <span>{{ selectedMentor.rating?.toFixed(1) || '5.0' }}</span>
            </div>
          </div>
          
          <div class="mentor-info-section">
            <h2 class="mentor-name-large">{{ selectedMentor.realName }}</h2>
            <p class="mentor-dept-large">{{ selectedMentor.department }} · {{ selectedMentor.major }}</p>
          </div>
        </div>

        <!-- 内容区域 -->
        <div class="mentor-content-area">
          <!-- 个人简介 -->
          <div class="content-block">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><User /></el-icon>
                <span>个人简介</span>
              </div>
            </div>
            <div class="block-content">
              <p class="text-content">{{ selectedMentor.bio || '这位导师还没有填写个人简介。' }}</p>
            </div>
          </div>

          <!-- 技能标签 -->
          <div class="content-block">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><MagicStick /></el-icon>
                <span>技能专长</span>
              </div>
              <span class="skill-count" v-if="!loadingSkills && mentorSkills.length > 0">
                {{ mentorSkills.length }} 项技能
              </span>
            </div>
            <div class="block-content">
              <!-- 加载中 -->
              <div v-if="loadingSkills" class="skills-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载技能标签中...</span>
              </div>
              
              <!-- 技能列表 -->
              <div v-else-if="mentorSkills.length > 0" class="skills-showcase">
                <div 
                  v-for="(skill, index) in mentorSkills" 
                  :key="index" 
                  class="skill-chip"
                  :data-level="skill.proficiencyLevel"
                  :style="{ animationDelay: `${index * 0.05}s` }"
                >
                  <i class="level-dot"></i>
                  <span class="skill-chip-text">{{ skill.skillName || skill.tagName }}</span>
                </div>
              </div>
              
              <!-- 无技能 -->
              <div v-else class="no-skills">
                <el-icon><InfoFilled /></el-icon>
                <span>该导师还未添加技能标签</span>
              </div>
            </div>
          </div>

          <!-- 兴趣领域 -->
          <div class="content-block">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><Connection /></el-icon>
                <span>兴趣领域</span>
              </div>
              <span class="skill-count" v-if="!loadingOtherTags && mentorInterests.length > 0">
                {{ mentorInterests.length }} 个兴趣
              </span>
            </div>
            <div class="block-content">
              <!-- 加载中 -->
              <div v-if="loadingOtherTags" class="skills-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载标签中...</span>
              </div>
              
              <!-- 兴趣列表 -->
              <div v-else-if="mentorInterests.length > 0" class="skills-showcase">
                <div 
                  v-for="(tag, index) in mentorInterests" 
                  :key="index" 
                  class="skill-chip interest-chip"
                  :style="{ animationDelay: `${index * 0.05}s` }"
                >
                  <span class="skill-chip-text">{{ tag.tagName }}</span>
                </div>
              </div>
              
              <!-- 无兴趣 -->
              <div v-else class="no-skills">
                <el-icon><InfoFilled /></el-icon>
                <span>该导师还未添加兴趣标签</span>
              </div>
            </div>
          </div>

          <!-- 个人特质 -->
          <div class="content-block">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><MagicStick /></el-icon>
                <span>个人特质</span>
              </div>
              <span class="skill-count" v-if="!loadingOtherTags && mentorPersonalities.length > 0">
                {{ mentorPersonalities.length }} 项特质
              </span>
            </div>
            <div class="block-content">
              <!-- 加载中 -->
              <div v-if="loadingOtherTags" class="skills-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载标签中...</span>
              </div>
              
              <!-- 特质列表 -->
              <div v-else-if="mentorPersonalities.length > 0" class="skills-showcase">
                <div 
                  v-for="(tag, index) in mentorPersonalities" 
                  :key="index" 
                  class="skill-chip personality-chip"
                  :style="{ animationDelay: `${index * 0.05}s` }"
                >
                  <span class="skill-chip-text">{{ tag.tagName }}</span>
                </div>
              </div>
              
              <!-- 无特质 -->
              <div v-else class="no-skills">
                <el-icon><InfoFilled /></el-icon>
                <span>该导师还未添加个人特质标签</span>
              </div>
            </div>
          </div>

          <!-- 偏好类型 -->
          <div class="content-block">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><Star /></el-icon>
                <span>偏好类型</span>
              </div>
              <span class="skill-count" v-if="!loadingOtherTags && mentorProjectTypes.length > 0">
                {{ mentorProjectTypes.length }} 种类型
              </span>
            </div>
            <div class="block-content">
              <!-- 加载中 -->
              <div v-if="loadingOtherTags" class="skills-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载标签中...</span>
              </div>
              
              <!-- 类型列表 -->
              <div v-else-if="mentorProjectTypes.length > 0" class="skills-showcase">
                <div 
                  v-for="(tag, index) in mentorProjectTypes" 
                  :key="index" 
                  class="skill-chip type-chip"
                  :style="{ animationDelay: `${index * 0.05}s` }"
                >
                  <span class="skill-chip-text">{{ tag.tagName }}</span>
                </div>
              </div>
              
              <!-- 无类型 -->
              <div v-else class="no-skills">
                <el-icon><InfoFilled /></el-icon>
                <span>该导师还未添加偏好类型标签</span>
              </div>
            </div>
          </div>

          <!-- 项目经验 -->
          <div class="content-block">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><Trophy /></el-icon>
                <span>项目经验</span>
              </div>
            </div>
            <div class="block-content">
              <div class="markdown-wrapper">
                <MarkdownViewer
                  :content="selectedMentor.projectExperience || '暂无项目经验展示'"
                />
              </div>
            </div>
          </div>

          <!-- 指导成就 -->
          <div class="content-block" v-if="selectedMentor.guidanceExperience">
            <div class="block-header">
              <div class="block-title">
                <el-icon class="block-icon"><Medal /></el-icon>
                <span>指导成就</span>
              </div>
            </div>
            <div class="block-content">
              <div class="markdown-wrapper">
                <MarkdownViewer
                  :content="selectedMentor.guidanceExperience"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="mentor-actions">
          <el-button size="large" @click="showDetailDialog = false">
            返回列表
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Close, Star, User, MagicStick, Trophy, Medal, Loading, InfoFilled, Connection } from '@element-plus/icons-vue'
import { getMentorPlaza, type MentorCard as MentorCardType } from '@/api/mentor'
import { getUserSkills } from '@/api/profile'
import { request } from '@/utils/request'
import type { UserSkill } from '@/types/user'
import MentorCard from '@/components/mentor/MentorCard.vue'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const loading = ref(false)
const mentors = ref<MentorCardType[]>([])
const total = ref(0)
const activeTab = ref('mentors')

const tabOptions = [
  { label: '导师列表', value: 'mentors' }
]

const pagination = reactive({
  page: 1,
  size: 12
})

const filters = reactive({
  department: '',
  keyword: ''
})

const showDetailDialog = ref(false)
const selectedMentor = ref<MentorCardType | null>(null)
const mentorSkills = ref<UserSkill[]>([])
const loadingSkills = ref(false)

// 导师的其他标签
const mentorInterests = ref<any[]>([])
const mentorPersonalities = ref<any[]>([])
const mentorProjectTypes = ref<any[]>([])
const loadingOtherTags = ref(false)

const loadMentors = async () => {
  loading.value = true
  try {
    const res = await getMentorPlaza({
      page: pagination.page,
      size: pagination.size,
      department: filters.department || undefined,
      keyword: filters.keyword || undefined
    })
    
    mentors.value = res.records
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载导师列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadMentors()
}

const resetFilters = () => {
  filters.department = ''
  filters.keyword = ''
  handleSearch()
}

const handleViewDetail = async (mentor: MentorCardType) => {
  selectedMentor.value = mentor
  showDetailDialog.value = true
  
  // 获取导师的真实技能标签
  mentorSkills.value = []
  loadingSkills.value = true
  try {
    const res = await getUserSkills(mentor.id)
    if (res?.data) {
      mentorSkills.value = res.data
    }
  } catch (error) {
    console.error('获取导师技能失败:', error)
  } finally {
    loadingSkills.value = false
  }

  // 获取导师的其他标签（兴趣、性格、项目类型）
  mentorInterests.value = []
  mentorPersonalities.value = []
  mentorProjectTypes.value = []
  loadingOtherTags.value = true
  try {
    const [interestsRes, personalitiesRes, projectTypesRes] = await Promise.all([
      request.get(`/api/user-tags/${mentor.id}/tags/INTEREST`),
      request.get(`/api/user-tags/${mentor.id}/tags/PERSONALITY`),
      request.get(`/api/user-tags/${mentor.id}/tags/PROJECT_TYPE`)
    ])
    
    if (interestsRes?.data) mentorInterests.value = interestsRes.data
    if (personalitiesRes?.data) mentorPersonalities.value = personalitiesRes.data
    if (projectTypesRes?.data) mentorProjectTypes.value = projectTypesRes.data
  } catch (error) {
    console.error('获取导师其他标签失败:', error)
  } finally {
    loadingOtherTags.value = false
  }
}

onMounted(() => {
  loadMentors()
})
</script>

<style scoped lang="scss">
.talent-square {
  min-height: 60vh;
}

.ts-tabs {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
}

.ts-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px;
  background: var(--bg-elevated);
  border-radius: 16px;
  border: 1px solid var(--border-subtle);
}

.ts-controls__left,
.ts-controls__right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.ts-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .ts-filter__label {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-color);
  }
}

.ts-loading {
  margin-top: 20px;
}

.ts-empty {
  margin-top: 40px;
}

.ts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 28px;
}

.ts-skeleton-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px;
  
  .skeleton-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }
  
  .skeleton-ident {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
}

.ts-pagination {
  margin-top: 56px;
  display: flex;
  justify-content: center;
}

/* ==================== 导师详情对话框样式 ==================== */
.mentor-detail-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
    background: var(--bg-card);
    padding: 0;
  }
  :deep(.el-dialog__header) { display: none; }
  :deep(.el-dialog__body) { padding: 0; }
}

.mentor-detail {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  position: relative;

  .close-btn {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 10;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    background: rgba(0,0,0,0.1);
    backdrop-filter: blur(8px);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-color);
    transition: all 0.2s;
    &:hover { background: rgba(0,0,0,0.2); transform: scale(1.1); }
  }

  .mentor-card-top {
    padding: 40px;
    background: linear-gradient(135deg, rgba(var(--accent-color-rgb), 0.08), rgba(var(--accent-color-rgb), 0.03));
    display: flex;
    gap: 32px;
    align-items: center;
    border-bottom: 1px solid var(--border-subtle);
  }

  .mentor-avatar-section {
    position: relative;
  }

  .mentor-avatar-large {
    border: 4px solid var(--bg-card);
    box-shadow: 0 12px 30px rgba(0,0,0,0.1);
  }

  .mentor-rating-badge {
    position: absolute;
    bottom: -10px;
    right: -10px;
    background: #f59e0b;
    color: #fff;
    padding: 4px 12px;
    border-radius: 100px;
    font-weight: 800;
    display: flex;
    align-items: center;
    gap: 4px;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  }

  .mentor-info-section {
    flex: 1;
  }

  .mentor-name-large { 
    font-size: 36px; 
    font-weight: 900; 
    margin: 0 0 8px; 
    color: var(--text-color); 
  }
  
  .mentor-dept-large { 
    font-size: 16px; 
    color: var(--text-color-muted); 
    margin: 0; 
  }

  .mentor-content-area {
    padding: 32px 40px;
    overflow-y: auto;
    flex: 1;
  }

  .content-block {
    margin-bottom: 32px;
    .block-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      .block-title {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 18px;
        font-weight: 800;
        color: var(--text-color);
        .block-icon { color: var(--accent-color); }
      }
    }
    .text-content { 
      font-size: 15px; 
      line-height: 1.8; 
      color: var(--text-color-muted); 
    }
  }

  .skill-count {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-color-muted);
  }

  .skills-loading {
    display: flex;
    align-items: center;
    gap: 12px;
    color: var(--text-color-muted);
    font-size: 14px;
  }

  .skills-showcase {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    .skill-chip {
      padding: 6px 16px;
      border-radius: 100px;
      background: var(--bg-body);
      border: 1px solid var(--border-subtle);
      font-size: 14px;
      font-weight: 600;
      color: var(--text-color-muted);
      transition: all 0.2s;
      display: flex;
      align-items: center;
      gap: 8px;
      
      &:hover { 
        transform: translateY(-2px); 
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
        border-color: rgba(16, 185, 129, 0.3);
        &:hover { 
          border-color: #10b981; 
          color: #10b981; 
        }
      }
      
      &[data-level="ADVANCED"] {
        .level-dot { background: var(--accent-color); }
        border-color: rgba(var(--accent-color-rgb), 0.3);
        &:hover { 
          border-color: var(--accent-color); 
          color: var(--accent-color); 
        }
      }
      
      &[data-level="INTERMEDIATE"] {
        .level-dot { background: #94a3b8; }
        border-color: rgba(148, 163, 184, 0.3);
        &:hover { 
          border-color: #94a3b8; 
          color: #94a3b8; 
        }
      }
      
      /* 兴趣标签 */
      &.interest-chip {
        border-color: rgba(16, 185, 129, 0.3);
        background: rgba(16, 185, 129, 0.05);
        &:hover { 
          border-color: #10b981; 
          color: #10b981; 
          background: rgba(16, 185, 129, 0.1);
        }
      }
      
      /* 个人特质标签 */
      &.personality-chip {
        border-color: rgba(139, 92, 246, 0.3);
        background: rgba(139, 92, 246, 0.05);
        &:hover { 
          border-color: #8b5cf6; 
          color: #8b5cf6; 
          background: rgba(139, 92, 246, 0.1);
        }
      }
      
      /* 项目类型标签 */
      &.type-chip {
        border-color: rgba(245, 158, 11, 0.3);
        background: rgba(245, 158, 11, 0.05);
        &:hover { 
          border-color: #f59e0b; 
          color: #f59e0b; 
          background: rgba(245, 158, 11, 0.1);
        }
      }
    }
  }

  .no-skills {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--text-color-muted);
    font-size: 14px;
  }

  .markdown-wrapper {
    :deep(.markdown-body) {
      font-size: 15px;
      line-height: 1.8;
      color: var(--text-color-muted);
    }
  }

  .mentor-actions {
    padding: 24px 40px;
    display: flex;
    gap: 16px;
    border-top: 1px solid var(--border-subtle);
    background: var(--bg-card);
    .el-button { 
      flex: 1; 
      height: 52px; 
      border-radius: 14px; 
      font-weight: 700; 
      font-size: 16px; 
    }
  }
}

[data-theme='dark'] .mentor-detail {
  .mentor-card-top { background: linear-gradient(135deg, rgba(255,255,255,0.03), rgba(255,255,255,0.01)); }
  .close-btn { background: rgba(255,255,255,0.05); &:hover { background: rgba(255,255,255,0.1); } }
}

@media (max-width: 768px) {
  .ts-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .ts-controls__left,
  .ts-controls__right {
    flex-direction: column;
    align-items: stretch;
  }
  
  .ts-grid {
    grid-template-columns: 1fr;
  }
  
  .mentor-detail {
    .mentor-card-top {
      flex-direction: column;
      text-align: center;
    }
    
    .mentor-content-area {
      padding: 24px 20px;
    }
    
    .mentor-actions {
      padding: 20px;
    }
  }
}
</style>
