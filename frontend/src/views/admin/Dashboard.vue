<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>数据统计看板</h1>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #409eff">
              <el-icon size="32"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon size="32"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalProjects }}</div>
              <div class="stat-label">总项目数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon size="32"><UserFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon size="32"><SuccessFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.completedProjects }}</div>
              <div class="stat-label">已完成项目</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-section">
      <el-col :span="12">
        <UserGrowthChart :data="stats.projectTrend" />
      </el-col>

      <el-col :span="12">
        <ProjectDistribution :data="stats.projectStatus" />
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-section">
      <el-col :span="24">
        <DepartmentComparison :data="stats.departmentStats" />
      </el-col>
    </el-row>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Document, UserFilled, SuccessFilled, Trophy, School, DataAnalysis } from '@element-plus/icons-vue'
import UserGrowthChart from '../../components/charts/UserGrowthChart.vue'
import ProjectDistribution from '../../components/charts/ProjectDistribution.vue'
import DepartmentComparison from '../../components/charts/DepartmentComparison.vue'
import { getStatsOverview, type StatsOverview } from '@/api/stats'

const stats = ref<StatsOverview>({
  totalUsers: 0,
  totalProjects: 0,
  totalTeams: 0,
  totalMessages: 0,
  activeUsers: 0,
  completedProjects: 0,
  projectTrend: [],
  projectStatus: [],
  topActiveUsers: [],
  departmentStats: []
})

const loading = ref(false)

const loadStats = async () => {
  loading.value = true
  try {
    const data = await getStatsOverview()
    console.log('✅ 统计数据加载成功:', data)
    stats.value = data
    console.log('📈 更新后的 stats:', stats.value)
  } catch (error: any) {
    console.error('❌ 加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  padding: 20px;

  .dashboard-header {
    margin-bottom: 24px;

    h1 {
      font-size: 24px;
      font-weight: 500;
      margin: 0;
    }
  }

  .stat-cards {
    margin-bottom: 24px;

    .stat-card {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }

      .stat-content {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }

  .charts-section {
    margin-bottom: 24px;
  }

  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .quick-actions {
      display: flex;
      gap: 12px;
    }
  }

}
</style>
