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

    <!-- 导师详情对话框 - 使用可复用组件 -->
    <MentorDetailDialog
      v-model="showDetailDialog"
      :mentor-detail="selectedMentor"
      close-button-text="返回广场"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getMentorPlaza, type MentorCard as MentorCardType } from '@/api/mentor'
import { getUserSkills } from '@/api/profile'
import { request } from '@/utils/request'
import type { UserSkill } from '@/types/user'
import MentorCard from '@/components/mentor/MentorCard.vue'
import MentorDetailDialog from '@/components/mentor/MentorDetailDialog.vue'

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

const handleViewDetail = (mentor: MentorCardType) => {
  selectedMentor.value = mentor
  showDetailDialog.value = true
}


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
