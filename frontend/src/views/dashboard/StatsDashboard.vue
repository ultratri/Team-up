<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TrendCharts, PieChart, User, Briefcase, Users, MessageBox } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const loading = ref(false)
const stats = ref({
  totalProjects: 128,
  totalUsers: 456,
  totalTeams: 32,
  totalMessages: 1234,
  projectTrend: [
    { date: '11-17', count: 12 },
    { date: '11-18', count: 15 },
    { date: '11-19', count: 18 },
    { date: '11-20', count: 22 },
    { date: '11-21', count: 25 },
    { date: '11-22', count: 28 },
    { date: '11-23', count: 32 }
  ],
  projectStatus: [
    { name: '招募中', value: 45 },
    { name: '进行中', value: 38 },
    { name: '已完成', value: 32 },
    { name: '草稿', value: 13 }
  ],
  activeUsers: [
    { name: '张三', count: 45 },
    { name: '李四', count: 38 },
    { name: '王五', count: 32 },
    { name: '赵六', count: 28 },
    { name: '钱七', count: 25 }
  ]
})

const initTrendChart = () => {
  const chartDom = document.getElementById('trend-chart')
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: stats.value.projectTrend.map(item => item.date)
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: '项目数',
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0,
          color: 'rgba(64, 158, 255, 0.3)'
        }, {
          offset: 1,
          color: 'rgba(64, 158, 255, 0.05)'
        }])
      },
      data: stats.value.projectTrend.map(item => item.count),
      itemStyle: {
        color: '#409EFF'
      }
    }]
  }
  
  myChart.setOption(option)
}

const initStatusChart = () => {
  const chartDom = document.getElementById('status-chart')
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [{
      name: '项目状态',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: stats.value.projectStatus
    }]
  }
  
  myChart.setOption(option)
}

onMounted(async () => {
  loading.value = true
  try {
    // 加载实际数据
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    const res = await fetch(`${baseUrl}/stats/overview`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const data = await res.json()
    
    if (data.code === 200 && data.data) {
      stats.value = {
        totalProjects: data.data.totalProjects,
        totalUsers: data.data.totalUsers,
        totalTeams: data.data.totalTeams,
        totalMessages: data.data.totalMessages,
        projectTrend: data.data.projectTrend,
        projectStatus: data.data.projectStatus,
        activeUsers: data.data.activeUsers
      }
    }
    
    // 初始化图表
    setTimeout(() => {
      initTrendChart()
      initStatusChart()
    }, 100)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="stats-dashboard">
    <h2 class="dashboard-title">数据统计</h2>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="card-icon projects">
          <el-icon :size="32"><Briefcase /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ stats.totalProjects }}</div>
          <div class="card-label">总项目数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-icon users">
          <el-icon :size="32"><User /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ stats.totalUsers }}</div>
          <div class="card-label">注册用户</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-icon teams">
          <el-icon :size="32"><Users /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ stats.totalTeams }}</div>
          <div class="card-label">团队数量</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-icon messages">
          <el-icon :size="32"><MessageBox /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ stats.totalMessages }}</div>
          <div class="card-label">消息总数</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 项目趋势图 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>项目增长趋势</h3>
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div id="trend-chart" class="chart-container"></div>
      </div>

      <!-- 项目状态分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>项目状态分布</h3>
          <el-icon><PieChart /></el-icon>
        </div>
        <div id="status-chart" class="chart-container"></div>
      </div>
    </div>

    <!-- 活跃用户榜 -->
    <div class="active-users-section">
      <h3>活跃用户排行</h3>
      <div class="users-list">
        <div
          v-for="(user, index) in stats.activeUsers"
          :key="user.name"
          class="user-item"
        >
          <div class="user-rank" :class="`rank-${index + 1}`">
            {{ index + 1 }}
          </div>
          <div class="user-name">{{ user.name }}</div>
          <div class="user-count">{{ user.count }} 次活动</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.stats-dashboard {
  padding: 24px;
}

.dashboard-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 700;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-card);
  transition: all 0.3s;

  &:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
  }
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;

  &.projects {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  &.users {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }

  &.teams {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }

  &.messages {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  }
}

.card-content {
  flex: 1;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: var(--text-color-secondary);
}

.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  box-shadow: var(--shadow-card);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
  }

  .el-icon {
    font-size: 20px;
    color: var(--text-color-muted);
  }
}

.chart-container {
  width: 100%;
  height: 300px;
}

.active-users-section {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  box-shadow: var(--shadow-card);

  h3 {
    margin: 0 0 16px 0;
    font-size: 16px;
    font-weight: 600;
  }
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  transition: all 0.3s;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.user-rank {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  background: var(--el-fill-color);
  color: var(--text-color);

  &.rank-1 {
    background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
    color: #333;
  }

  &.rank-2 {
    background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%);
    color: #333;
  }

  &.rank-3 {
    background: linear-gradient(135deg, #cd7f32 0%, #daa520 100%);
    color: #fff;
  }
}

.user-name {
  flex: 1;
  font-weight: 600;
  color: var(--text-color);
}

.user-count {
  font-size: 13px;
  color: var(--text-color-secondary);
}
</style>
