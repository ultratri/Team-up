<template>
  <article class="mentor-card" :class="departmentClass" @click="handleViewDetail">
    <div class="mc-surface">
      <header class="mc-header">
        <div class="mc-header__top">
          <el-avatar :size="60" :src="mentor.avatarUrl" class="mc-avatar">
            {{ (mentor.realName || mentor.username || '导').charAt(0) }}
          </el-avatar>

          <div class="mc-ident">
            <div class="mc-name">{{ mentor.realName }}</div>
            <div class="mc-sub">{{ mentor.department }} · {{ mentor.major }}</div>
          </div>

          <div class="mc-rating">
            <div class="mc-rating__value">{{ mentor.rating?.toFixed(1) || '0.0' }}</div>
            <div class="mc-rating__label">评分</div>
          </div>
        </div>
      </header>

      <section class="mc-body">
        <div class="mc-bio">
          {{ truncateText(mentor.bio || '这位导师很神秘，还没有填写简介。', 80) }}
        </div>

        <div class="mc-tags" v-if="skills.length > 0">
          <span v-for="tag in skills" :key="tag" class="mc-tag">{{ tag }}</span>
        </div>
      </section>

      <footer class="mc-footer">
        <el-button
          type="primary"
          class="mc-view-detail"
          @click.stop="handleViewDetail"
        >
          查看详情
        </el-button>
      </footer>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MentorCard } from '@/api/mentor'

interface Props {
  mentor: MentorCard
}

const props = defineProps<Props>()
const emit = defineEmits<{
  view: [mentor: MentorCard]
}>()

const departmentClass = computed(() => {
  const dept = props.mentor.department || ''
  if (dept.includes('计算机') || dept.includes('软件')) return 'theme-blue'
  if (dept.includes('信息') || dept.includes('电子')) return 'theme-purple'
  return 'theme-green'
})

const skills = computed(() => {
  // 从项目经验中提取技能标签，与用户资料页面保持一致
  if (props.mentor.projectExperience) {
    // 提取常见的技术关键词
    const keywords = props.mentor.projectExperience
      .split(/[、,，\s\n]/)
      .map(k => k.trim())
      .filter(k => k.length >= 2 && k.length <= 15)
      .filter(k => /^[a-zA-Z\u4e00-\u9fa5+#.]+$/.test(k)) // 只保留字母、中文、+、#、.
      .slice(0, 5) // 最多显示5个
    
    if (keywords.length > 0) return keywords
  }
  
  // 如果没有项目经验，返回空数组
  return []
})

const truncateText = (text: string, length: number) => {
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

const handleViewDetail = () => emit('view', props.mentor)
</script>

<style scoped lang="scss">
.mentor-card {
  height: 100%;
  cursor: pointer;
  transition: transform 0.3s var(--ease-out);
  position: relative;

  &.theme-blue { --mc-accent-rgb: 59, 130, 246; }
  &.theme-purple { --mc-accent-rgb: 139, 92, 246; }
  &.theme-green { --mc-accent-rgb: 16, 185, 129; }

  &:hover {
    transform: translateY(-8px);
    .mc-surface {
      box-shadow: var(--shadow-card-hover);
      border-color: rgba(var(--mc-accent-rgb), 0.4);
    }
    .mc-avatar { transform: scale(1.05); }
  }
}

.mc-surface {
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

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; height: 120px;
    background: linear-gradient(180deg, rgba(var(--mc-accent-rgb), 0.15) 0%, transparent 100%);
    pointer-events: none;
  }
}

/* 暗色模式适配 */
:global([data-theme='dark']) .mc-surface {
  background: var(--bg-elevated);
  &::before { background: linear-gradient(180deg, rgba(var(--mc-accent-rgb), 0.2) 0%, transparent 100%); }
}

.mc-header {
  position: relative;
  z-index: 1;
  padding: 24px 20px 20px;
}

.mc-header__top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mc-avatar {
  border: 3px solid var(--bg-card);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s var(--ease-out);
  flex-shrink: 0;
  background: var(--bg-body);
  color: var(--text-color);
  font-weight: 800;
}

.mc-ident {
  flex: 1;
  min-width: 0;
}

.mc-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-color);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mc-sub {
  font-size: 13px;
  color: var(--text-color-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mc-rating {
  text-align: right;
  flex-shrink: 0;
  .mc-rating__value { font-size: 20px; font-weight: 900; color: var(--accent-color); }
  .mc-rating__label { font-size: 11px; color: var(--text-color-secondary); }
}

.mc-body {
  flex: 1;
  padding: 12px 20px;
}

.mc-bio {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-color-muted);
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mc-tag {
  font-size: 12px;
  padding: 4px 10px;
  background: var(--accent-soft);
  color: var(--accent-color);
  border-radius: 8px;
  font-weight: 600;
}

.mc-footer {
  padding: 16px 20px 24px;
  .mc-view-detail {
    width: 100%;
    height: 44px;
    border-radius: 14px;
    font-weight: 700;
    font-size: 15px;
    box-shadow: 0 8px 20px -4px rgba(var(--accent-color-rgb), 0.3);
    transition: all 0.3s var(--ease-out);
    &:hover { transform: scale(1.02); box-shadow: 0 12px 24px -4px rgba(var(--accent-color-rgb), 0.4); }
  }
}
</style>
