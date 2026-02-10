<script setup lang="ts">
import { computed } from 'vue'
import { User, View, Right } from '@element-plus/icons-vue'
import type { Project } from '@/types/project'
import GlassCard from '@/components/common/GlassCard.vue'

const props = defineProps<{
  project: Project
}>()

const emit = defineEmits<{
  (e: 'click', id: number): void
  (e: 'apply', id: number): void
}>()

const statusColor = computed(() => {
  const map: Record<string, string> = {
    RECRUITING: '#10b981', // Emerald 500
    IN_PROGRESS: '#3b82f6', // Blue 500
    COMPLETED: '#8b5cf6', // Violet 500
    default: '#9ca3af'
  }
  return map[props.project.status] || map.default
})

const statusText = computed(() => {
  const map: Record<string, string> = {
    RECRUITING: '招募中',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    DRAFT: '草稿',
    PENDING_REVIEW: '审核中'
  }
  return map[props.project.status] || props.project.status
})

const typeTag = computed(() => {
  const map: Record<string, string> = {
    COMPETITION: '竞赛',
    RESEARCH: '科研',
    STARTUP: '创业',
    OPENSOURCE: '开源',
    OTHER: '其他'
  }
  return map[props.project.projectType] || props.project.projectType
})

// Progress mock - in real app might come from project data
const progress = computed(() => {
  if (props.project.status === 'COMPLETED') return 100
  if (props.project.status === 'IN_PROGRESS') return 45
  return 10
})
</script>

<template>
  <GlassCard class="project-card" hover-effect @click="emit('click', project.id)">
    <div class="card-content">
      <!-- Header -->
      <div class="card-header">
        <div class="project-icon">
          <img v-if="project.logo" :src="project.logo" alt="logo" loading="lazy" decoding="async" />
          <div v-else class="icon-placeholder">{{ project.title.charAt(0) }}</div>
        </div>
        <div class="header-info">
          <div class="title-row">
            <h3 class="title" :title="project.title">{{ project.title }}</h3>
            <span class="status-dot" :style="{ backgroundColor: statusColor }" :title="statusText"></span>
          </div>
          <div class="tags-row">
            <span class="type-tag">{{ typeTag }}</span>
            <span class="role-tag" v-for="role in project.roles?.slice(0, 2)" :key="role">{{ role }}</span>
          </div>
        </div>
      </div>

      <!-- Description -->
      <p class="description">{{ project.description || '暂无描述' }}</p>

      <!-- Progress Bar -->
      <div class="progress-section">
        <div class="progress-info">
          <span>项目进度</span>
          <span>{{ progress }}%</span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" :style="{ width: `${progress}%`, backgroundColor: statusColor }"></div>
        </div>
      </div>

      <!-- Footer -->
      <div class="card-footer">
        <div class="meta-group">
          <div class="meta-item">
            <el-icon><User /></el-icon>
            <span>{{ project.currentMembers }}/{{ project.teamSize }}</span>
          </div>
          <div class="meta-item">
            <el-icon><View /></el-icon>
            <span>{{ project.views }}</span>
          </div>
        </div>
        
        <button class="action-btn" @click.stop="emit('apply', project.id)">
          <span class="btn-text">加入</span>
          <el-icon class="btn-icon"><Right /></el-icon>
        </button>
      </div>
    </div>
  </GlassCard>
</template>

<style scoped lang="scss">
.project-card {
  height: 100%;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.card-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;

  .project-icon {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    overflow: hidden;
    flex-shrink: 0;
    background: linear-gradient(135deg, var(--bg-elevated), var(--bg-elevated-soft));
    box-shadow: inset 0 0 0 1px var(--border-subtle);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.3s ease;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .icon-placeholder {
      font-size: 24px;
      font-weight: 700;
      color: var(--accent-color);
    }
  }
  
  .header-info {
    flex: 1;
    min-width: 0;
    
    .title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;
      
      .title {
        font-size: 18px;
        font-weight: 700;
        color: var(--text-color);
        margin: 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      
      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        box-shadow: 0 0 0 2px var(--bg-elevated);
      }
    }
    
    .tags-row {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      
      span {
        font-size: 12px;
        padding: 2px 8px;
        border-radius: 6px;
        font-weight: 500;
      }
      
      .type-tag {
        background: var(--accent-soft);
        color: var(--accent-color);
      }
      
      .role-tag {
        background: rgba(0,0,0,0.04);
        color: var(--text-color-muted);
      }
    }
  }
}

.project-card:hover .project-icon {
  transform: scale(1.05);
}

.description {
  font-size: 14px;
  color: var(--text-color-muted);
  line-height: 1.6;
  margin: 0 0 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.progress-section {
  margin-bottom: 20px;
  
  .progress-info {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--text-color-muted);
    margin-bottom: 6px;
  }
  
  .progress-bar-bg {
    height: 6px;
    background: rgba(0,0,0,0.05);
    border-radius: 3px;
    overflow: hidden;
    position: relative;
    
    .progress-bar-fill {
      height: 100%;
      border-radius: 3px;
      transition: width 1s cubic-bezier(0.34, 1.56, 0.64, 1);
      position: relative;
      
      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
        background-image: linear-gradient(
          45deg,
          rgba(255, 255, 255, 0.15) 25%,
          transparent 25%,
          transparent 50%,
          rgba(255, 255, 255, 0.15) 50%,
          rgba(255, 255, 255, 0.15) 75%,
          transparent 75%,
          transparent
        );
        background-size: 1rem 1rem;
        opacity: 0.3;
      }
    }
  }
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
  
  .meta-group {
    display: flex;
    gap: 16px;
    
    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--text-color-muted);
      
      .el-icon {
        font-size: 16px;
      }
    }
  }
  
  .action-btn {
    border: none;
    background: var(--bg-elevated-soft);
    color: var(--accent-color);
    font-weight: 600;
    font-size: 13px;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      background: var(--accent-color);
      color: white;
      
      .btn-icon {
        transform: translateX(2px);
      }
    }
    
    .btn-icon {
      transition: transform 0.2s;
    }
  }
}

[data-theme='dark'] {
  .card-header .project-icon {
    box-shadow: inset 0 0 0 1px rgba(255,255,255,0.1);
  }
  
  .header-info .tags-row .role-tag {
    background: rgba(255,255,255,0.1);
    color: var(--text-color-muted);
  }
  
  .progress-section .progress-bar-bg {
    background: rgba(255,255,255,0.1);
  }
}
</style>
