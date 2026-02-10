<template>
  <div class="animation-demo">
    <div class="demo-header">
      <h1>页面过渡动画演示</h1>
      <p>点击下方按钮查看不同的动画效果</p>
    </div>

    <div class="demo-controls">
      <el-select v-model="selectedAnimation" placeholder="选择动画效果" size="large">
        <el-option-group label="基础动画">
          <el-option label="淡入淡出 (fade)" value="fade" />
          <el-option label="滑动淡入 (slide-fade)" value="slide-fade" />
          <el-option label="向左滑动 (slide-left)" value="slide-left" />
          <el-option label="向右滑动 (slide-right)" value="slide-right" />
        </el-option-group>
        
        <el-option-group label="PPT风格">
          <el-option label="推入效果 (push) ⭐" value="push" />
          <el-option label="翻页效果 (flip) ⭐⭐" value="flip" />
          <el-option label="立方体旋转 (cube)" value="cube" />
          <el-option label="缩放效果 (zoom)" value="zoom" />
          <el-option label="溶解效果 (dissolve)" value="dissolve" />
          <el-option label="擦除效果 (wipe)" value="wipe" />
        </el-option-group>
        
        <el-option-group label="3D效果">
          <el-option label="3D旋转 (rotate-3d)" value="rotate-3d" />
          <el-option label="翻转卡片 (flip-card)" value="flip-card" />
        </el-option-group>
        
        <el-option-group label="特殊效果">
          <el-option label="弹跳效果 (bounce)" value="bounce" />
          <el-option label="涟漪效果 (ripple)" value="ripple" />
          <el-option label="百叶窗效果 (blinds)" value="blinds" />
        </el-option-group>
      </el-select>

      <el-button type="primary" size="large" @click="toggleContent">
        播放动画
      </el-button>

      <el-button size="large" @click="autoPlay">
        自动播放所有
      </el-button>
    </div>

    <div class="demo-stage">
      <transition :name="selectedAnimation" mode="out-in">
        <div v-if="showContent" :key="contentKey" class="demo-content" :style="contentStyle">
          <div class="content-inner">
            <el-icon :size="80" color="#409EFF">
              <component :is="currentIcon" />
            </el-icon>
            <h2>{{ contentTitle }}</h2>
            <p>{{ contentDescription }}</p>
            <div class="animation-info">
              <el-tag type="info">动画: {{ selectedAnimation }}</el-tag>
              <el-tag type="success">时长: {{ animationDuration }}</el-tag>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <div class="demo-info">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>当前动画信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="动画名称">{{ selectedAnimation }}</el-descriptions-item>
          <el-descriptions-item label="动画时长">{{ animationDuration }}</el-descriptions-item>
          <el-descriptions-item label="适用场景">{{ animationScenario }}</el-descriptions-item>
          <el-descriptions-item label="性能影响">{{ animationPerformance }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <div class="demo-code">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>使用方法</span>
            <el-button type="primary" text @click="copyCode">复制代码</el-button>
          </div>
        </template>
        <pre><code>{{ codeExample }}</code></pre>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Document, 
  Folder, 
  User, 
  ChatDotRound, 
  DataAnalysis,
  Setting,
  Star,
  Trophy,
  Promotion
} from '@element-plus/icons-vue'

const selectedAnimation = ref('flip')
const showContent = ref(true)
const contentKey = ref(0)
const isAutoPlaying = ref(false)

const icons = [Document, Folder, User, ChatDotRound, DataAnalysis, Setting, Star, Trophy, Promotion]
const currentIconIndex = ref(0)

const currentIcon = computed(() => icons[currentIconIndex.value])

const contentColors = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', 
  '#909399', '#00D7FF', '#FF6B9D', '#C71585'
]

const contentStyle = computed(() => ({
  background: `linear-gradient(135deg, ${contentColors[contentKey.value % contentColors.length]}, ${contentColors[(contentKey.value + 1) % contentColors.length]})`
}))

const contentTitle = computed(() => {
  const titles = [
    '团队概览', '任务看板', '文件共享', '团队聊天',
    '数据统计', '成员管理', '项目详情', '个人中心'
  ]
  return titles[contentKey.value % titles.length]
})

const contentDescription = computed(() => {
  return `这是使用 ${selectedAnimation.value} 动画效果的演示内容`
})

const animationDuration = computed(() => {
  const durations: Record<string, string> = {
    'fade': '0.3s',
    'slide-fade': '0.4s',
    'slide-left': '0.4s',
    'slide-right': '0.4s',
    'push': '0.5s',
    'flip': '0.6s',
    'cube': '0.6s',
    'zoom': '0.4s',
    'dissolve': '0.5s',
    'wipe': '0.5s',
    'rotate-3d': '0.6s',
    'flip-card': '0.6s',
    'bounce': '0.6s',
    'ripple': '0.6s',
    'blinds': '0.5s'
  }
  return durations[selectedAnimation.value] || '0.4s'
})

const animationScenario = computed(() => {
  const scenarios: Record<string, string> = {
    'fade': '登录/注册页面',
    'slide-fade': '大多数页面切换',
    'slide-left': '向左导航',
    'slide-right': '通知/消息中心',
    'push': '进入团队空间',
    'flip': '团队内部标签页切换（PPT风格）',
    'cube': '特殊展示页面',
    'zoom': '详情页面',
    'dissolve': '数据统计页面',
    'wipe': '特殊场景',
    'rotate-3d': '管理后台',
    'flip-card': '卡片式内容',
    'bounce': '活泼页面',
    'ripple': '特殊交互',
    'blinds': '特殊场景'
  }
  return scenarios[selectedAnimation.value] || '通用场景'
})

const animationPerformance = computed(() => {
  const performance: Record<string, string> = {
    'fade': '极佳 (GPU加速)',
    'slide-fade': '极佳 (GPU加速)',
    'slide-left': '极佳 (GPU加速)',
    'slide-right': '极佳 (GPU加速)',
    'push': '良好 (GPU加速)',
    'flip': '良好 (3D变换)',
    'cube': '中等 (复杂3D)',
    'zoom': '极佳 (GPU加速)',
    'dissolve': '良好 (滤镜)',
    'wipe': '良好 (clip-path)',
    'rotate-3d': '良好 (3D变换)',
    'flip-card': '良好 (3D变换)',
    'bounce': '极佳 (GPU加速)',
    'ripple': '良好 (变换)',
    'blinds': '中等 (clip-path)'
  }
  return performance[selectedAnimation.value] || '良好'
})

const codeExample = computed(() => {
  return `// 在路由配置中使用
{
  path: '/your-page',
  name: 'YourPage',
  component: () => import('./views/YourPage.vue'),
  meta: { 
    transition: '${selectedAnimation.value}'
  }
}

// 或在组件中使用
<template>
  <transition name="${selectedAnimation.value}" mode="out-in">
    <div v-if="show" key="content">
      <!-- 内容 -->
    </div>
  </transition>
</template>`
})

const toggleContent = () => {
  showContent.value = false
  setTimeout(() => {
    contentKey.value++
    currentIconIndex.value = (currentIconIndex.value + 1) % icons.length
    showContent.value = true
  }, 100)
}

const copyCode = () => {
  navigator.clipboard.writeText(codeExample.value)
  ElMessage.success('代码已复制到剪贴板')
}

const animations = [
  'fade', 'slide-fade', 'slide-left', 'slide-right',
  'push', 'flip', 'cube', 'zoom', 'dissolve', 'wipe',
  'rotate-3d', 'flip-card', 'bounce', 'ripple', 'blinds'
]

const autoPlay = async () => {
  if (isAutoPlaying.value) return
  
  isAutoPlaying.value = true
  ElMessage.info('开始自动播放所有动画效果')
  
  for (const animation of animations) {
    if (!isAutoPlaying.value) break
    
    selectedAnimation.value = animation
    await new Promise(resolve => setTimeout(resolve, 500))
    toggleContent()
    await new Promise(resolve => setTimeout(resolve, 2000))
  }
  
  isAutoPlaying.value = false
  ElMessage.success('自动播放完成')
}

onMounted(() => {
  // 初始动画
  setTimeout(() => {
    toggleContent()
  }, 500)
})
</script>

<style scoped lang="scss">
.animation-demo {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.demo-header {
  text-align: center;
  margin-bottom: 40px;
  
  h1 {
    font-size: 36px;
    font-weight: 700;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 10px;
  }
  
  p {
    font-size: 16px;
    color: var(--el-text-color-secondary);
  }
}

.demo-controls {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
  
  .el-select {
    min-width: 300px;
  }
}

.demo-stage {
  position: relative;
  height: 400px;
  margin-bottom: 40px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.demo-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  
  .content-inner {
    text-align: center;
    padding: 40px;
    
    h2 {
      font-size: 32px;
      margin: 20px 0 10px;
      font-weight: 600;
    }
    
    p {
      font-size: 16px;
      opacity: 0.9;
      margin-bottom: 20px;
    }
    
    .animation-info {
      display: flex;
      gap: 12px;
      justify-content: center;
      
      .el-tag {
        font-size: 14px;
        padding: 8px 16px;
      }
    }
  }
}

.demo-info {
  margin-bottom: 30px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
}

.demo-code {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
  
  pre {
    margin: 0;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    overflow-x: auto;
    
    code {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 14px;
      line-height: 1.6;
      color: #2c3e50;
    }
  }
}

@media (max-width: 768px) {
  .demo-header h1 {
    font-size: 28px;
  }
  
  .demo-controls {
    flex-direction: column;
    
    .el-select {
      width: 100%;
    }
    
    .el-button {
      width: 100%;
    }
  }
  
  .demo-stage {
    height: 300px;
  }
  
  .demo-content .content-inner {
    padding: 20px;
    
    h2 {
      font-size: 24px;
    }
  }
}
</style>
