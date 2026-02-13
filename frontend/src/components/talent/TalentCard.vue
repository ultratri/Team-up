<template>
  <article class="talent-card" :class="departmentClass" @click="handleViewDetail">
    <div class="tc-surface">
      <!-- 头像和基本信息区域 -->
      <header class="tc-header">
        <div class="tc-header__top">
          <el-avatar 
            :size="60" 
            :src="lazyLoadedAvatar" 
            class="tc-avatar"
            @error="handleAvatarError"
          >
            {{ talent.realName?.charAt(0) || '学' }}
          </el-avatar>

          <div class="tc-ident">
            <div class="tc-name">{{ talent.realName }}</div>
            <div class="tc-sub">{{ talent.department }} · {{ talent.major }}</div>
          </div>

          <div class="tc-credit">
            <div class="tc-credit__value">{{ talent.creditScore || 0 }}</div>
            <div class="tc-credit__label">信誉分</div>
          </div>
        </div>
      </header>

      <section class="tc-body">
        <!-- 个人简介区域 -->
        <div class="tc-bio">
          {{ truncateText(talent.bio || '这位同学还没有填写个人简介。', 80) }}
        </div>

        <!-- 技能标签区域（最多显示5个，超出显示+N） -->
        <div class="tc-tags" v-if="displaySkills.length > 0">
          <span v-for="skill in displaySkills" :key="skill" class="tc-tag">{{ skill }}</span>
          <span v-if="remainingSkillsCount > 0" class="tc-tag tc-tag--more">
            +{{ remainingSkillsCount }}
          </span>
        </div>

        <!-- 组队意向标签区域 -->
        <div class="tc-intentions" v-if="talent.intentions && talent.intentions.length > 0">
          <el-tag 
            v-if="talent.intentions.includes('JOIN_PROJECT')" 
            type="primary" 
            size="small"
            class="tc-intention-tag"
          >
            寻找项目
          </el-tag>
          <el-tag 
            v-if="talent.intentions.includes('FIND_TEAMMATES')" 
            type="success" 
            size="small"
            class="tc-intention-tag"
          >
            寻找队友
          </el-tag>
          <el-tag 
            v-if="talent.intentions.includes('FIND_MENTOR')" 
            type="warning" 
            size="small"
            class="tc-intention-tag"
          >
            寻找导师
          </el-tag>
          <el-tag 
            v-if="talent.intentions.includes('HELP_NEWBIE')" 
            type="info" 
            size="small"
            class="tc-intention-tag"
          >
            帮助新手
          </el-tag>
        </div>
      </section>

      <!-- 可投入时间区域 -->
      <footer class="tc-footer" v-if="talent.weeklyHours">
        <div class="tc-time-info">
          <el-icon class="tc-time-icon"><Clock /></el-icon>
          <span>每周{{ talent.weeklyHours }}小时</span>
        </div>
      </footer>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Clock } from '@element-plus/icons-vue'

// 定义 TalentVO 接口
export interface TalentVO {
  id: number
  username: string
  realName: string
  avatarUrl?: string
  department: string
  major: string
  bio?: string
  creditScore?: number
  skills: string[]
  intentions: string[]
  weeklyHours?: number
  notes?: string
  lastLoginAt?: string
  status?: string
}

// 定义 props
interface Props {
  talent: TalentVO
}

const props = defineProps<Props>()

// 定义 emits（view事件）
const emit = defineEmits<{
  view: [talent: TalentVO]
}>()

// 图片懒加载状态
const lazyLoadedAvatar = ref<string | undefined>(undefined)
const avatarError = ref(false)

// 根据院系生成主题色
const departmentClass = computed(() => {
  const dept = props.talent.department || ''
  if (dept.includes('计算机') || dept.includes('软件')) return 'theme-blue'
  if (dept.includes('信息') || dept.includes('电子')) return 'theme-purple'
  return 'theme-green'
})

// 显示的技能标签（最多5个）
const displaySkills = computed(() => {
  return props.talent.skills.slice(0, 5)
})

// 剩余技能数量
const remainingSkillsCount = computed(() => {
  const total = props.talent.skills.length
  return total > 5 ? total - 5 : 0
})

// 文本截断工具函数
const truncateText = (text: string, length: number) => {
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

// 处理头像加载错误
const handleAvatarError = () => {
  avatarError.value = true
  lazyLoadedAvatar.value = undefined
}

// 点击卡片触发view事件
const handleViewDetail = () => {
  emit('view', props.talent)
}

// 组件挂载时延迟加载头像（简单的懒加载实现）
onMounted(() => {
  // 使用 requestIdleCallback 或 setTimeout 延迟加载头像
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      lazyLoadedAvatar.value = props.talent.avatarUrl
    })
  } else {
    setTimeout(() => {
      lazyLoadedAvatar.value = props.talent.avatarUrl
    }, 100)
  }
})
</script>

<style scoped lang="scss">
.talent-card {
  height: 100%;
  cursor: pointer;
  transition: transform 0.3s var(--ease-out);
  position: relative;

  // 主题色变量
  &.theme-blue { --tc-accent-rgb: 59, 130, 246; }
  &.theme-purple { --tc-accent-rgb: 139, 92, 246; }
  &.theme-green { --tc-accent-rgb: 16, 185, 129; }

  // 悬停效果
  &:hover {
    transform: translateY(-8px);
    .tc-surface {
      box-shadow: var(--shadow-card-hover);
      border-color: rgba(var(--tc-accent-rgb), 0.4);
    }
    .tc-avatar { 
      transform: scale(1.05); 
    }
  }
}

.tc-surface {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  position: relative;
  transition: all 0.3s var(--ease-out);

  // 顶部渐变背景
  &::before {
    content: '';
    position: absolute;
    top: 0; 
    left: 0; 
    right: 0; 
    height: 120px;
    background: linear-gradient(180deg, rgba(var(--tc-accent-rgb), 0.15) 0%, transparent 100%);
    pointer-events: none;
  }
}

// 暗色模式适配
:global([data-theme='dark']) .tc-surface {
  background: var(--bg-elevated);
  &::before { 
    background: linear-gradient(180deg, rgba(var(--tc-accent-rgb), 0.2) 0%, transparent 100%); 
  }
}

// 头部区域
.tc-header {
  position: relative;
  z-index: 1;
  padding: 24px 20px 20px;
}

.tc-header__top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tc-avatar {
  border: 3px solid var(--bg-card);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s var(--ease-out);
  flex-shrink: 0;
  background: var(--bg-body);
  color: var(--text-color);
  font-weight: 800;
}

.tc-ident {
  flex: 1;
  min-width: 0;
}

.tc-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-color);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tc-sub {
  font-size: 13px;
  color: var(--text-color-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tc-credit {
  text-align: right;
  flex-shrink: 0;
  
  .tc-credit__value { 
    font-size: 20px; 
    font-weight: 900; 
    color: var(--accent-color); 
  }
  
  .tc-credit__label { 
    font-size: 11px; 
    color: var(--text-color-secondary); 
  }
}

// 主体区域
.tc-body {
  flex: 1;
  padding: 12px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tc-bio {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-color-muted);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tc-tag {
  font-size: 12px;
  padding: 4px 10px;
  background: var(--accent-soft);
  color: var(--accent-color);
  border-radius: 8px;
  font-weight: 600;
  
  &.tc-tag--more {
    background: var(--bg-body);
    color: var(--text-color-muted);
    border: 1px solid var(--border-subtle);
  }
}

.tc-intentions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  
  .tc-intention-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 6px;
  }
}

// 底部区域
.tc-footer {
  padding: 12px 20px 20px;
  border-top: 1px solid var(--border-subtle);
}

.tc-time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-color-muted);
  
  .tc-time-icon {
    color: var(--accent-color);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .tc-header {
    padding: 20px 16px 16px;
  }
  
  .tc-body {
    padding: 10px 16px;
  }
  
  .tc-footer {
    padding: 10px 16px 16px;
  }
  
  .tc-avatar {
    width: 50px !important;
    height: 50px !important;
  }
  
  .tc-name {
    font-size: 16px;
  }
  
  .tc-credit__value {
    font-size: 18px;
  }
}
</style>
