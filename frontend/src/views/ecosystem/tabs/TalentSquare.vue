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
            @input="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 200px;"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="ts-filter" v-if="activeTab === 'talents'">
          <div class="ts-filter__label">组队意向</div>
          <el-select v-model="filters.intention" placeholder="全部" clearable @change="handleSearch" style="width: 150px;">
            <el-option label="全部" value="" />
            <el-option label="寻找项目" value="JOIN_PROJECT" />
            <el-option label="寻找队友" value="FIND_TEAMMATES" />
            <el-option label="寻找导师" value="FIND_MENTOR" />
            <el-option label="帮助新手" value="HELP_NEWBIE" />
          </el-select>
        </div>
      </div>

      <div class="ts-controls__right">
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <!-- 人才墙 -->
    <div v-if="activeTab === 'talents'" class="talent-wall">
      <!-- 加载骨架屏 -->
      <div v-if="talentLoading" class="ts-loading">
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

      <!-- 空状态 -->
      <div v-else-if="talents.length === 0" class="ts-empty">
        <el-empty description="暂无相关人才">
          <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
        </el-empty>
      </div>

      <!-- 人才列表 -->
      <TransitionGroup v-else name="ts-grid" tag="div" class="ts-grid">
        <TalentCard
          v-for="talent in talents"
          :key="talent.userId"
          :talent="talent"
          @view="handleViewTalentDetail"
        />
      </TransitionGroup>

      <!-- 分页 -->
      <div v-if="talentTotal > 0" class="ts-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="talentTotal"
          :page-sizes="[12, 24, 36]"
          layout="prev, pager, next"
          background
          @current-change="loadTalents"
          @size-change="loadTalents"
        />
      </div>
    </div>

    <!-- 导师列表 -->
    <div v-if="activeTab === 'mentors'" class="mentor-list">
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
    </div>

    <!-- 导师详情对话框 -->
    <MentorDetailDialog
      v-model="showDetailDialog"
      :mentor-detail="selectedMentor"
      close-button-text="返回列表"
    />

    <!-- 人才详情对话框 -->
    <TalentDetailDialog
      v-model="showTalentDetailDialog"
      :talent="selectedTalent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getMentorPlaza, type MentorCard as MentorCardType } from '@/api/mentor'
import { getTalentList, type TalentVO } from '@/api/user'
import MentorDetailDialog from '@/components/mentor/MentorDetailDialog.vue'
import TalentDetailDialog from '@/components/talent/TalentDetailDialog.vue'
import { useDebounce } from '@/composables/useLazyLoad'
import MentorCard from '@/components/mentor/MentorCard.vue'
import TalentCard from '@/components/talent/TalentCard.vue'

// 导师相关状态
const loading = ref(false)
const mentors = ref<MentorCardType[]>([])
const total = ref(0)
const activeTab = ref('talents')

const tabOptions = [
  { label: '人才墙', value: 'talents' },
  { label: '导师列表', value: 'mentors' }
]

const pagination = reactive({
  page: 1,
  size: 12
})

const filters = reactive({
  department: '',
  keyword: '',
  intention: ''
})

// 人才墙相关状态
const talents = ref<TalentVO[]>([])
const talentTotal = ref(0)
const talentLoading = ref(false)

const showDetailDialog = ref(false)
const selectedMentor = ref<MentorCardType | null>(null)

// 人才详情对话框
const showTalentDetailDialog = ref(false)
const selectedTalent = ref<TalentVO | null>(null)

// 查看导师详情
const handleViewDetail = async (mentor: MentorCardType) => {
  selectedMentor.value = mentor
  showDetailDialog.value = true
}

// 查看人才详情
const handleViewTalentDetail = (talent: TalentVO) => {
  selectedTalent.value = talent
  showTalentDetailDialog.value = true
}

// 加载人才列表
const loadTalents = async () => {
  talentLoading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      department: filters.department || undefined,
      keyword: filters.keyword || undefined,
      intention: filters.intention || undefined
    }
    
    const res = await getTalentList(params)
    // 过滤掉没有 userId 的数据，确保 TransitionGroup 的 key 有效
    talents.value = (res.records || []).filter(talent => talent && talent.userId)
    talentTotal.value = res.total || 0
  } catch (error) {
    console.error('加载人才列表失败:', error)
    ElMessage.error('加载人才列表失败')
  } finally {
    talentLoading.value = false
  }
}

// 加载导师列表
const loadMentors = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      department: filters.department || undefined,
      keyword: filters.keyword || undefined
    }
    
    const res = await getMentorPlaza(params)
    // 过滤掉没有 id 的数据，确保 TransitionGroup 的 key 有效
    mentors.value = (res.records || []).filter(mentor => mentor && mentor.id)
    total.value = res.total || 0
  } catch (error) {
    console.error('加载导师列表失败:', error)
    ElMessage.error('加载导师列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理（防抖）
const handleSearch = useDebounce(() => {
  pagination.page = 1
  if (activeTab.value === 'talents') {
    loadTalents()
  } else {
    loadMentors()
  }
}, 300)

// 重置筛选
const resetFilters = () => {
  filters.department = ''
  filters.keyword = ''
  filters.intention = ''
  pagination.page = 1
  
  if (activeTab.value === 'talents') {
    loadTalents()
  } else {
    loadMentors()
  }
}

// Tab切换监听
watch(activeTab, (newTab) => {
  // 重置分页
  pagination.page = 1
  
  if (newTab === 'talents') {
    loadTalents()
  } else {
    loadMentors()
  }
})

onMounted(() => {
  // 默认加载人才墙
  loadTalents()
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
}

/* ==================== 人才墙样式 ==================== */
.talent-wall {
  min-height: 60vh;
}

.tc-intentions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  
  .el-tag {
    font-weight: 600;
  }
}

.tc-time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: var(--text-color-muted);
  
  .tc-time-icon {
    color: var(--accent-color);
  }
}

.contact-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  
  .contact-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    background: var(--bg-body);
    border-radius: 8px;
    border: 1px solid var(--border-subtle);
    
    .contact-label {
      font-weight: 600;
      color: var(--text-color);
      margin-right: 8px;
      flex-shrink: 0;
    }
    
    .contact-value {
      color: var(--text-color-muted);
      flex: 1;
      word-break: keep-all;
      overflow-wrap: break-word;
    }
  }
}
</style>
