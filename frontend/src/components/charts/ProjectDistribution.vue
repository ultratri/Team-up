<template>
  <div class="project-distribution">
    <el-card>
      <template #header>
        <h3>项目类型分布</h3>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import type { StatusData } from '@/api/stats'

interface Props {
  data?: StatusData[]
}

const props = withDefaults(defineProps<Props>(), {
  data: () => []
})

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']

const updateChart = () => {
  if (!chartRef.value || !chartInstance) return

  const chartData = props.data.map((item, index) => ({
    value: item.value,
    name: item.name,
    itemStyle: { color: colors[index % colors.length] }
  }))

  const option: EChartsOption = {
    title: {
      text: '项目状态分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
      textStyle: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      data: props.data.map(item => item.name),
      textStyle: {
        fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
      },
    },
    series: [
      {
        name: '项目状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold',
            fontFamily: 'Microsoft YaHei, SimHei, Arial, sans-serif',
          },
        },
        labelLine: {
          show: false,
        },
        data: chartData.length > 0 ? chartData : [
          { value: 0, name: '暂无数据', itemStyle: { color: '#909399' } }
        ],
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
.project-distribution {
  .chart-container {
    width: 100%;
    height: 400px;
  }
}
</style>

