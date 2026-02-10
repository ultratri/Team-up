<template>
  <div class="department-comparison">
    <el-card>
      <template #header>
        <h3>院系数据对比</h3>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import type { DepartmentStats } from '@/api/stats'

interface Props {
  data?: DepartmentStats[]
}

const props = withDefaults(defineProps<Props>(), {
  data: () => []
})

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

const updateChart = () => {
  if (!chartRef.value || !chartInstance) return

  const departments = props.data.map(item => item.department) || []
  const userCounts = props.data.map(item => item.userCount) || []
  const projectCounts = props.data.map(item => item.projectCount) || []

  const option: EChartsOption = {
    title: {
      text: '各院系用户数和项目数',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    legend: {
      data: ['用户数', '项目数'],
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: departments.length > 0 ? departments : ['暂无数据'],
      axisLabel: {
        rotate: 30,
      },
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: '用户数',
        type: 'bar',
        data: userCounts.length > 0 ? userCounts : [0],
        itemStyle: {
          color: '#409EFF',
          borderRadius: [4, 4, 0, 0],
        },
      },
      {
        name: '项目数',
        type: 'bar',
        data: projectCounts.length > 0 ? projectCounts : [0],
        itemStyle: {
          color: '#67C23A',
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  }

  chartInstance.setOption(option)
}

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', handleResize)
}

watch(() => props.data, () => {
  updateChart()
}, { deep: true })

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped lang="scss">
.department-comparison {
  .chart-container {
    width: 100%;
    height: 400px;
  }
}
</style>

