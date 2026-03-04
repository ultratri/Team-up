<template>
  <div class="user-growth-chart">
    <el-card>
      <template #header>
        <h3>用户增长趋势</h3>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import type { TrendData } from '@/api/stats'

interface Props {
  data?: TrendData[]
}

const props = withDefaults(defineProps<Props>(), {
  data: () => []
})

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

const updateChart = () => {
  if (!chartRef.value || !chartInstance) return

  const dates = props.data.map(item => item.date) || []
  const projectData = props.data.map(item => item.count) || []

  const option: EChartsOption = {
    title: {
      text: '最近7天项目增长趋势',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
      },
      textStyle: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    legend: {
      data: ['新增项目'],
      bottom: 0,
      textStyle: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    series: [
      {
        name: '新增项目',
        type: 'line',
        smooth: true,
        data: projectData,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' },
          ]),
        },
        lineStyle: {
          color: '#409EFF',
        },
        itemStyle: {
          color: '#409EFF',
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
.user-growth-chart {
  .chart-container {
    width: 100%;
    height: 400px;
  }
}
</style>

