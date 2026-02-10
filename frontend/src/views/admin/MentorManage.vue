<template>
  <div class="mentor-manage">
    <!-- 导师绩效排行榜 -->
    <el-card class="ranking-card">
      <template #header>
        <div class="card-header">
          <h2>导师绩效排行榜</h2>
          <el-button type="primary" @click="loadRanking">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table 
        :data="ranking" 
        v-loading="rankingLoading"
        style="width: 100%"
      >
        <el-table-column label="排名" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              v-if="row.rank === 1" 
              type="danger" 
              effect="dark"
            >
              🥇 {{ row.rank }}
            </el-tag>
            <el-tag 
              v-else-if="row.rank === 2" 
              type="warning" 
              effect="dark"
            >
              🥈 {{ row.rank }}
            </el-tag>
            <el-tag 
              v-else-if="row.rank === 3" 
              type="success" 
              effect="dark"
            >
              🥉 {{ row.rank }}
            </el-tag>
            <span v-else>{{ row.rank }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="导师姓名" min-width="120">
          <template #default="{ row }">
            {{ row.mentorName || '-' }}
          </template>
        </el-table-column>
        
        <el-table-column label="院系" min-width="150">
          <template #default="{ row }">
            {{ row.department || '-' }}
          </template>
        </el-table-column>
        
        <el-table-column label="成功培养学员数" min-width="140" align="center">
          <template #default="{ row }">
            {{ row.successfulMentees || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column label="学员平均信誉分" min-width="140" align="center">
          <template #default="{ row }">
            {{ row.averageMenteeScore?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        
        <el-table-column label="累计奖励积分" min-width="130" align="center">
          <template #default="{ row }">
            {{ row.totalRewardPoints || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column label="导师评分" min-width="180">
          <template #default="{ row }">
            <el-rate 
              v-model="row.rating" 
              disabled 
              show-score 
              text-color="#ff9900"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 导师列表 -->
    <el-card class="mentor-list-card">
      <template #header>
        <div class="card-header">
          <h2>导师列表</h2>
          <el-button type="warning" @click="handleUpdateAllRatings">
            <el-icon><Star /></el-icon>
            更新所有评分
          </el-button>
        </div>
      </template>

      <el-table 
        :data="mentors" 
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column label="姓名" min-width="100">
          <template #default="{ row }">
            <div style="font-weight: 500;">{{ row.realName || '-' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.username || '-' }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="院系/专业" min-width="180">
          <template #default="{ row }">
            <div>{{ row.department || '-' }}</div>
            <div style="color: #909399; font-size: 12px;">{{ row.major || '-' }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="学员统计" min-width="220">
          <template #default="{ row }">
            <div class="mentee-stats">
              <el-tag size="small" type="info">总数: {{ row.totalMentees || 0 }}</el-tag>
              <el-tag size="small" type="success">活跃: {{ row.activeMentees || 0 }}</el-tag>
              <el-tag size="small" type="warning">完成: {{ row.completedMentees || 0 }}</el-tag>
            </div>
            <div style="margin-top: 4px;">
              <el-tag size="small" effect="dark" type="success">
                ✓ 成功培养: {{ row.successfulMentees || 0 }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="学员平均信誉分" min-width="140" align="center">
          <template #default="{ row }">
            <div style="padding: 0 10px;">
              <el-progress 
                :percentage="Math.round(row.averageMenteeScore || 0)" 
                :color="getScoreColor(row.averageMenteeScore || 0)"
                :stroke-width="8"
              />
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="奖励积分" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="dark">
              {{ row.totalRewardPoints || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="导师评分" min-width="160" align="center">
          <template #default="{ row }">
            <el-rate 
              :model-value="Number(row.rating) || 0" 
              disabled 
              show-score 
              text-color="#ff9900"
              :score-template="`${Number(row.rating || 0).toFixed(1)} 分`"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              link 
              type="primary" 
              size="small"
              @click="viewMentorDetail(row)"
            >
              查看详情
            </el-button>
            <el-button 
              link 
              type="danger" 
              size="small"
              @click="handleRevoke(row)"
            >
              撤销资格
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadMentors"
        @current-change="loadMentors"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 导师详情对话框 -->
    <MentorDetailDialog 
      v-model="detailDialogVisible" 
      :mentor-id="selectedMentorId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Star } from '@element-plus/icons-vue'
import { 
  getMentorList, 
  getMentorRanking,
  revokeMentor,
  updateAllMentorRatings,
  updateMentorRating,
  type MentorInfo,
  type MentorRanking 
} from '@/api/mentor'
import MentorDetailDialog from '@/components/admin/MentorDetailDialog.vue'

const mentors = ref<MentorInfo[]>([])
const ranking = ref<MentorRanking[]>([])
const loading = ref(false)
const rankingLoading = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const selectedMentorId = ref<number | null>(null)

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const loadMentors = async () => {
  loading.value = true
  try {
    const result = await getMentorList(pagination.value.page, pagination.value.size)
    mentors.value = result.records
    pagination.value.total = result.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载导师列表失败')
  } finally {
    loading.value = false
  }
}

const loadRanking = async () => {
  rankingLoading.value = true
  try {
    ranking.value = await getMentorRanking(10)
  } catch (error: any) {
    ElMessage.error(error.message || '加载排行榜失败')
  } finally {
    rankingLoading.value = false
  }
}

const handleRevoke = async (mentor: MentorInfo) => {
  try {
    await ElMessageBox.confirm(
      `确认撤销 ${mentor.realName} 的导师资格吗？此操作不可恢复。`,
      '确认撤销',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await revokeMentor(mentor.id)
    ElMessage.success('导师资格已撤销')
    loadMentors()
    loadRanking()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const viewMentorDetail = (mentor: MentorInfo) => {
  selectedMentorId.value = mentor.id
  detailDialogVisible.value = true
}

const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const handleUpdateAllRatings = async () => {
  try {
    await ElMessageBox.confirm(
      '确认更新所有导师的评分吗？评分将根据导师的绩效数据自动计算。',
      '更新评分',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    const loadingMsg = ElMessage({
      message: '正在更新评分...',
      type: 'info',
      duration: 0
    })

    const result = await updateAllMentorRatings()
    loadingMsg.close()
    
    ElMessage.success(result)
    
    // 重新加载数据
    await Promise.all([loadMentors(), loadRanking()])
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新失败')
    }
  }
}

onMounted(() => {
  loadMentors()
  loadRanking()
})
</script>

<style scoped lang="scss">
.mentor-manage {
  padding: 20px;

  .ranking-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
    }
  }

  .mentee-stats {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  :deep(.el-progress__text) {
    font-size: 13px !important;
    font-weight: 500;
  }

  :deep(.el-rate) {
    height: auto;
  }

  :deep(.el-rate__text) {
    font-size: 13px;
    font-weight: 500;
  }
}
</style>
