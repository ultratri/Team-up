<template>
  <div class="mp-page">
    <header class="mp-hero">
      <div class="mp-hero__inner">
        <div class="mp-hero__left">
          <div class="mp-badge">Mentor Plaza</div>
          <h1 class="mp-title">导师广场</h1>
          <p class="mp-subtitle">汇聚资深专家与优秀学长，为你的项目保驾护航</p>

          <div class="mp-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索导师姓名、技能、研究方向..."
              class="mp-search__input"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button type="primary" @click="handleSearch">探索</el-button>
              </template>
            </el-input>
          </div>

          <div class="mp-kpis">
            <div class="mp-kpi">
              <div class="mp-kpi__label">入驻导师</div>
              <div class="mp-kpi__value">{{ total }}</div>
            </div>
            <div class="mp-kpi">
              <div class="mp-kpi__label">当前筛选</div>
              <div class="mp-kpi__value">{{ mentors.length }}</div>
            </div>
          </div>
        </div>

        <div class="mp-hero__right" aria-hidden="true">
          <div class="mp-orb mp-orb--a"></div>
          <div class="mp-orb mp-orb--b"></div>
          <div class="mp-orb mp-orb--c"></div>
        </div>
      </div>
    </header>

    <section class="mp-controls">
      <div class="mp-controls__inner">
        <div class="mp-controls__left">
          <div class="mp-filter">
            <div class="mp-filter__label">所属院系</div>
            <div class="mp-filter__chips">
              <el-segmented
                v-model="selectedDepartment"
                :options="departmentOptions"
                @change="handleSearch"
              />
            </div>
          </div>
        </div>

        <div class="mp-controls__right">
          <div class="mp-sort">
            <span class="mp-sort__label">排序方式</span>
            <el-select v-model="sortBy" style="width: 130px" @change="handleSearch">
              <el-option
                v-for="item in sortOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>

          <div class="mp-switch">
            <el-switch
              v-model="onlyAvailable"
              inline-prompt
              active-text="仅看可申请"
              inactive-text="全部导师"
              @change="handleSearch"
            />
          </div>

          <div class="mp-actions">
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
      </div>
    </section>

    <main class="mp-content">
      <div class="mp-content__inner">
        <div class="mp-list">
          <!-- 骨架屏 -->
          <div v-if="loading" class="mp-grid">
            <div v-for="i in 6" :key="i" class="mp-skeleton-card">
              <el-skeleton animated>
                <template #template>
                  <div class="skeleton-header">
                    <el-skeleton-item variant="circle" style="width: 56px; height: 56px" />
                    <div class="skeleton-ident">
                      <el-skeleton-item variant="text" style="width: 40%" />
                      <el-skeleton-item variant="text" style="width: 60%" />
                    </div>
                  </div>
                  <div class="skeleton-metrics">
                    <el-skeleton-item variant="rect" height="40px" />
                    <el-skeleton-item variant="rect" height="40px" />
                    <el-skeleton-item variant="rect" height="40px" />
                  </div>
                  <el-skeleton-item variant="p" style="margin-top: 16px" />
                  <el-skeleton-item variant="button" style="width: 100%; height: 40px; margin-top: 20px" />
                </template>
              </el-skeleton>
            </div>
          </div>

          <div v-else-if="mentors.length === 0" class="mp-empty">
            <el-empty description="没有找到匹配的导师">
              <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
            </el-empty>
          </div>

          <TransitionGroup v-else name="mp-grid" tag="div" class="mp-grid">
            <MentorCard
              v-for="mentor in mentors"
              :key="mentor.id"
              :mentor="mentor"
              @view="handleViewDetail"
            />
          </TransitionGroup>
        </div>

        <div v-if="total > 0" class="mp-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[12, 24, 36]"
            layout="prev, pager, next"
            background
            @current-change="fetchMentors"
            @size-change="fetchMentors"
          />
        </div>
      </div>
    </main>

    <!-- 导师详情对话框 - 全新设计 -->
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
            
            <div class="mentor-badges">
              <span class="badge badge-success">
                <el-icon><Medal /></el-icon>
                认证导师
              </span>
            </div>

            <div class="mentor-stats-inline">
              <div class="stat-inline-item">
                <span class="stat-inline-value">{{ selectedMentor.totalMentees || 0 }}</span>
                <span class="stat-inline-label">累计学员</span>
              </div>
              <div class="stat-inline-divider"></div>
              <div class="stat-inline-item">
                <span class="stat-inline-value">{{ selectedMentor.activeMentees || 0 }}</span>
                <span class="stat-inline-label">正在指导</span>
              </div>
            </div>
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
              <div class="skill-legend" v-if="!loadingSkills && allMentorSkills.length > 0">
                <span class="legend-item expert"><i class="dot"></i> 精通</span>
                <span class="legend-item advanced"><i class="dot"></i> 高级</span>
                <span class="legend-item intermediate"><i class="dot"></i> 熟练</span>
              </div>
            </div>
            <div class="block-content">
              <!-- 加载中 -->
              <div v-if="loadingSkills" class="skills-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载技能标签中...</span>
              </div>
              
              <!-- 技能列表 -->
              <div v-else-if="allMentorSkills.length > 0" class="skills-showcase">
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
        </div>

        <!-- 底部操作 -->
        <div class="mentor-actions">
          <el-button size="large" @click="showDetailDialog = false">
            返回广场
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Close, Star, User, MagicStick, Trophy, Medal, Connection, Loading, InfoFilled } from '@element-plus/icons-vue'
import { getMentorPlaza, type MentorCard as MentorCardType } from '@/api/mentor'
import { getUserSkills } from '@/api/profile'
import { request } from '@/utils/request'
import type { UserSkill } from '@/types/user'
import MentorCard from '@/components/mentor/MentorCard.vue'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

const loading = ref(false)
const mentors = ref<MentorCardType[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

const searchKeyword = ref('')
const selectedDepartment = ref('')
const sortBy = ref('rating')
const onlyAvailable = ref(false)
const departments = ['计算机学院', '软件学院', '信息学院', '电子学院']

const sortOptions = [
  { label: '最高评分', value: 'rating' },
  { label: '学员人数', value: 'students' },
  { label: '贡献点', value: 'points' }
]

const departmentOptions = [
  { label: '全部', value: '' },
  ...departments.map(d => ({ label: d, value: d }))
]

const showDetailDialog = ref(false)
const selectedMentor = ref<MentorCardType | null>(null)
const activeTab = ref('bio')

// 导师的真实技能标签（从数据库获取）
const mentorSkills = ref<UserSkill[]>([])
const loadingSkills = ref(false)

// 导师的其他标签
const mentorInterests = ref<any[]>([])
const mentorPersonalities = ref<any[]>([])
const mentorProjectTypes = ref<any[]>([])
const loadingOtherTags = ref(false)

const fetchMentors = async () => {
  loading.value = true
  try {
    const res = await getMentorPlaza({
      page: currentPage.value,
      size: pageSize.value,
      department: selectedDepartment.value || undefined,
      keyword: searchKeyword.value || undefined,
      sortBy: sortBy.value,
      availableOnly: onlyAvailable.value
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
  currentPage.value = 1
  fetchMentors()
}

const resetFilters = () => {
  searchKeyword.value = ''
  selectedDepartment.value = ''
  sortBy.value = 'rating'
  onlyAvailable.value = false
  handleSearch()
}

const handleViewDetail = async (mentor: MentorCardType) => {
  selectedMentor.value = mentor
  showDetailDialog.value = true
  activeTab.value = 'bio'
  
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

// 导师的技能标签列表（从数据库获取的真实标签）
const allMentorSkills = computed(() => {
  return mentorSkills.value.map(skill => skill.skillName || skill.tagName).filter(Boolean)
})

onMounted(() => {
  fetchMentors()
})
</script>

<style scoped lang="scss">
.mp-page {
  --mp-bg-page: var(--bg-body);
  --mp-text-main: var(--text-color);
  --mp-text-muted: var(--text-color-muted);
  --mp-border: var(--border-subtle);
  --mp-accent: var(--accent-color);
  --mp-accent-rgb: var(--accent-color-rgb);

  --mp-hero-bg: linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #dbeafe 100%);
  --mp-hero-text: #0f172a;
  --mp-hero-text-muted: #64748b;
  --mp-hero-orb-opacity: 0.6;
  --mp-hero-badge-bg: rgba(var(--mp-accent-rgb), 0.08);
  --mp-search-bg: #ffffff;
  --mp-search-border: var(--border-subtle);

  min-height: 100vh;
  background: var(--mp-bg-page);
  transition: background var(--transition-base);
}

[data-theme='dark'] .mp-page {
  --mp-hero-bg: linear-gradient(135deg, #020617 0%, #0f172a 40%, #1e293b 80%, #020617 100%);
  --mp-hero-text: #f1f5f9;
  --mp-hero-text-muted: rgba(226, 232, 240, 0.65);
  --mp-hero-orb-opacity: 0.3;
  --mp-hero-badge-bg: rgba(255, 255, 255, 0.06);
  --mp-search-bg: rgba(30, 41, 59, 0.5);
  --mp-search-border: rgba(255, 255, 255, 0.08);
}

.mp-hero {
  position: relative;
  padding: 64px 0 52px;
  color: var(--mp-hero-text);
  background: var(--mp-hero-bg);
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: 
      radial-gradient(700px 400px at 30% 10%, rgba(var(--mp-accent-rgb), 0.1), transparent 60%),
      radial-gradient(700px 400px at 80% 40%, rgba(139, 92, 246, 0.08), transparent 60%);
    pointer-events: none;
  }
}

.mp-hero__inner {
  position: relative;
  z-index: 2;
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 32px;
  align-items: center;
}

.mp-badge {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--mp-hero-badge-bg);
  border: 1px solid rgba(var(--mp-accent-rgb), 0.1);
  font-size: 12px;
  font-weight: 700;
  color: var(--mp-accent);
}

.mp-title {
  margin: 12px 0;
  font-size: 48px;
  line-height: 1.1;
  font-weight: 900;
}

.mp-subtitle {
  margin-bottom: 32px;
  font-size: 18px;
  color: var(--mp-hero-text-muted);
}

.mp-search {
  max-width: 600px;
  :deep(.el-input__wrapper) {
    height: 54px;
    border-radius: 16px 0 0 16px;
    background: var(--mp-search-bg);
    border: 1px solid var(--mp-search-border);
  }
  :deep(.el-input-group__append) {
    border-radius: 0 16px 16px 0;
    background: var(--mp-accent);
    color: #fff;
    border: none;
    font-weight: 700;
  }
}

.mp-kpis {
  margin-top: 32px;
  display: flex;
  gap: 20px;
}

.mp-kpi {
  padding: 12px 24px;
  border-radius: 16px;
  background: var(--mp-hero-badge-bg);
  border: 1px solid rgba(var(--mp-accent-rgb), 0.1);
  backdrop-filter: blur(10px);
  .mp-kpi__label { font-size: 12px; color: var(--mp-hero-text-muted); }
  .mp-kpi__value { margin-top: 4px; font-size: 26px; font-weight: 900; color: var(--mp-hero-text); }
}

.mp-hero__right { position: relative; height: 320px; }

.mp-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: var(--mp-hero-orb-opacity);
  &.mp-orb--a { width: 240px; height: 240px; right: 0; top: 0; background: radial-gradient(circle, rgba(var(--mp-accent-rgb), 0.6), transparent 70%); }
  &.mp-orb--b { width: 180px; height: 180px; right: 120px; top: 100px; background: radial-gradient(circle, rgba(139, 92, 246, 0.5), transparent 70%); }
  &.mp-orb--c { width: 140px; height: 140px; right: 40px; top: 160px; background: radial-gradient(circle, rgba(16, 185, 129, 0.4), transparent 70%); }
}

.mp-controls {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-elevated-soft);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--mp-border);
}

.mp-controls__inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mp-controls__left, .mp-controls__right { display: flex; align-items: center; gap: 24px; }

.mp-filter {
  display: flex; align-items: center; gap: 12px;
  .mp-filter__label { font-size: 14px; font-weight: 700; color: var(--mp-text-main); }
}

.mp-content { padding: 40px 0 80px; }
.mp-content__inner { max-width: 1240px; margin: 0 auto; padding: 0 24px; }

.mp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 28px;
}

.mp-pagination { margin-top: 56px; display: flex; justify-content: center; }

/* ==================== 导师详情对话框 - 重构样式 ==================== */
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
    &:hover { background: rgba(0,0,0,0.2); }
  }

  .mentor-card-top {
    padding: 40px;
    background: linear-gradient(135deg, rgba(var(--mp-accent-rgb), 0.08), rgba(var(--mp-accent-rgb), 0.03));
    display: flex;
    gap: 32px;
    align-items: center;
    border-bottom: 1px solid var(--mp-border);
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

  .mentor-avatar-section { position: relative; }

  .mentor-name-large { font-size: 36px; font-weight: 900; margin: 0 0 8px; color: var(--text-color); }
  .mentor-dept-large { font-size: 16px; color: var(--text-color-muted); margin: 0 0 20px; }

  .mentor-badges {
    display: flex;
    gap: 10px;
    margin-bottom: 24px;
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 100px;
      font-size: 13px;
      font-weight: 600;
      &.badge-success { background: rgba(16, 185, 129, 0.1); color: #10b981; }
      &.badge-primary { background: rgba(var(--mp-accent-rgb), 0.1); color: var(--mp-accent); }
    }
  }

  .mentor-stats-inline {
    display: flex;
    gap: 32px;
    .stat-inline-item {
      display: flex;
      flex-direction: column;
      .stat-inline-value { font-size: 24px; font-weight: 900; color: var(--text-color); }
      .stat-inline-label { font-size: 12px; color: var(--text-color-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    }
    .stat-inline-divider { width: 1px; height: 40px; background: var(--mp-border); }
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
        .block-icon { color: var(--mp-accent); }
      }
    }
    .text-content { font-size: 15px; line-height: 1.8; color: var(--text-color-muted); }
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
      &.advanced .dot { background: var(--mp-accent); }
      &.intermediate .dot { background: #94a3b8; }
    }
  }

  .skill-count {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-color-muted);
  }

  .skills-showcase {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    .skill-chip {
      padding: 6px 16px;
      border-radius: 100px;
      background: var(--bg-body);
      border: 1px solid var(--mp-border);
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
      
      /* 熟练度等级颜色点 */
      .level-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #94a3b8;
        flex-shrink: 0;
      }
      
      /* 精通 - 绿色 */
      &[data-level="EXPERT"] {
        .level-dot { background: #10b981; }
        border-color: rgba(16, 185, 129, 0.3);
        &:hover { 
          border-color: #10b981; 
          color: #10b981; 
        }
      }
      
      /* 高级 - 主题色 */
      &[data-level="ADVANCED"] {
        .level-dot { background: var(--mp-accent); }
        border-color: rgba(var(--mp-accent-rgb), 0.3);
        &:hover { 
          border-color: var(--mp-accent); 
          color: var(--mp-accent); 
        }
      }
      
      /* 熟练 - 灰色 */
      &[data-level="INTERMEDIATE"] {
        .level-dot { background: #94a3b8; }
        border-color: rgba(148, 163, 184, 0.3);
        &:hover { 
          border-color: #94a3b8; 
          color: #94a3b8; 
        }
      }
      
      /* 入门 - 浅灰色 */
      &[data-level="BEGINNER"] {
        .level-dot { background: #cbd5e1; }
        border-color: rgba(203, 213, 225, 0.3);
        &:hover { 
          border-color: #cbd5e1; 
          color: #cbd5e1; 
        }
      }
      
      /* 不同类型标签的颜色 */
      &.interest-chip {
        border-color: rgba(16, 185, 129, 0.3);
        background: rgba(16, 185, 129, 0.05);
        &:hover { 
          border-color: #10b981; 
          color: #10b981; 
          background: rgba(16, 185, 129, 0.1);
        }
      }
      
      &.personality-chip {
        border-color: rgba(139, 92, 246, 0.3);
        background: rgba(139, 92, 246, 0.05);
        &:hover { 
          border-color: #8b5cf6; 
          color: #8b5cf6; 
          background: rgba(139, 92, 246, 0.1);
        }
      }
      
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

  .mentor-actions {
    padding: 24px 40px;
    display: flex;
    gap: 16px;
    border-top: 1px solid var(--mp-border);
    background: var(--bg-card);
    .el-button { flex: 1; height: 52px; border-radius: 14px; font-weight: 700; font-size: 16px; }
  }
}

[data-theme='dark'] .mentor-detail {
  .mentor-card-top { background: linear-gradient(135deg, rgba(255,255,255,0.03), rgba(255,255,255,0.01)); }
  .close-btn { background: rgba(255,255,255,0.05); }
}
</style>


/* 技能加载和空状态 */
.skills-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  color: var(--text-color-muted);
  font-size: 14px;
  justify-content: center;

  .el-icon {
    font-size: 20px;
  }
}

.no-skills {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px;
  color: var(--text-color-muted);
  font-size: 14px;
  justify-content: center;
  background: var(--bg-body);
  border-radius: 8px;

  .el-icon {
    font-size: 18px;
  }
}
