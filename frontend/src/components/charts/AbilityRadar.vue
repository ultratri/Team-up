<template>
  <div class="ability-radar">
    <div ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

interface AbilityData {
  技术深度: number
  跨项协作: number
  实践经验: number
  活跃程度: number
  专业匹配: number
}

const props = defineProps<{
  data?: Partial<AbilityData>
}>()

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null
let themeObserver: MutationObserver | null = null

const getCssVar = (name: string) => {
  if (typeof window === 'undefined') return ''
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

const initChart = () => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)

  // A方案：固定指标
  const indicators = [
    { name: '技术深度', max: 100 },
    { name: '跨项协作', max: 100 },
    { name: '实践经验', max: 100 },
    { name: '活跃程度', max: 100 },
    { name: '专业匹配', max: 100 }
  ]

  const d = props.data || {}
  const values = [
    d['技术深度'] || 0,
    d['跨项协作'] || 0,
    d['实践经验'] || 0,
    d['活跃程度'] || 0,
    d['专业匹配'] || 0
  ]

  const textColor = getCssVar('--text-color') || '#333'
  const mutedColor = getCssVar('--text-color-muted') || '#8c8c8c'
  const borderColor = getCssVar('--border-subtle') || 'rgba(0,0,0,0.1)'
  const accentColor = getCssVar('--accent-color') || '#3b82f6'
  const accentColorRgb = getCssVar('--accent-color-rgb') || '59, 130, 246'

  const option: EChartsOption = {
    tooltip: {
      show: true,
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      textStyle: { color: '#333' }
    },
    radar: {
      indicator: indicators,
      // 进一步缩小半径并调整中心，给文字留足边距
      radius: '55%',
      center: ['50%', '52%'],
      splitNumber: 4,
      axisName: {
        color: textColor, // 使用主文字颜色确保清晰
        fontSize: 13,
        fontWeight: 700,
        padding: [10, 15] // 显著增加内边距防止遮挡
      },
      splitLine: {
        lineStyle: {
          color: borderColor,
          width: 1,
          opacity: 0.6
        },
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: [
            'rgba(128, 128, 128, 0.02)',
            'rgba(128, 128, 128, 0.05)'
          ],
        },
      },
      axisLine: {
        lineStyle: {
          color: borderColor,
          opacity: 0.4
        }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '能力概览',
            symbol: 'circle',
            symbolSize: 8,
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: `rgba(${accentColorRgb}, 0.45)` },
                { offset: 1, color: `rgba(${accentColorRgb}, 0.05)` }
              ]),
            },
            lineStyle: {
              color: accentColor,
              width: 3,
            },
            itemStyle: {
              color: accentColor,
              borderColor: getCssVar('--bg-card') || '#fff',
              borderWidth: 2
            },
          },
        ],
      },
    ],
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(() => props.data, () => {
  nextTick(initChart)
}, { deep: true })

onMounted(() => {
  setTimeout(() => {
    initChart()
    window.addEventListener('resize', handleResize)
    
    themeObserver = new MutationObserver(() => {
      setTimeout(initChart, 100) // 主题切换时稍作延迟确保 CSS 变量已更新
    })
    themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme']
    })
  }, 200)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  themeObserver?.disconnect()
  chartInstance?.dispose()
})
</script>

<style scoped lang="scss">
.ability-radar {
  width: 100%;
  height: 100%;
  min-height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;

  .chart-container {
    width: 100%;
    height: 340px;
    overflow: visible;
  }
}
</style>
