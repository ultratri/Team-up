<template>
  <div 
    class="team-card"
    tabindex="0"
    role="button"
    :aria-label="`团队: ${team.name || '未命名团队'}, ${team.memberCount || 0} 名成员, ${userRole ? roleLabel : ''}`"
    @click="handleClick"
    @keydown.enter="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <!-- 团队头像 -->
    <div class="team-avatar" role="img" :aria-label="`${team.name || '未命名团队'} 团队头像`">
      <el-avatar 
        :size="60" 
        :src="team.avatar"
        :alt="`${team.name || '未命名团队'} 团队头像`"
        lazy
      >
        <el-icon :size="30"><OfficeBuilding /></el-icon>
      </el-avatar>
    </div>

    <!-- 团队信息 -->
    <div class="team-info">
      <div class="team-header">
        <h3 class="team-name" id="`team-name-${team.id}`">{{ team.name || '未命名团队' }}</h3>
        <el-tag 
          v-if="userRole" 
          :type="roleTagType" 
          size="small"
          class="role-tag"
          :aria-label="`您的角色: ${roleLabel}`"
        >
          {{ roleLabel }}
        </el-tag>
      </div>

      <p class="team-description" :aria-label="`团队描述: ${(team.description && team.description.trim()) || '暂无描述'}`">
        {{ (team.description && team.description.trim()) || '暂无描述' }}
      </p>

      <div class="team-meta" role="group" aria-label="团队元信息">
        <span class="meta-item" aria-label="`成员数量: ${team.memberCount || 0} 人`">
          <el-icon><User /></el-icon>
          {{ team.memberCount || 0 }} 人
        </span>
        <span class="meta-item" :aria-label="`创建时间: ${formatDate(team.createdAt)}`">
          <el-icon><Calendar /></el-icon>
          {{ formatDate(team.createdAt) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { OfficeBuilding, User, Calendar } from '@element-plus/icons-vue'
import type { Team, TeamRole } from '@/types/team'
import { useAuthStore } from '@/store/auth'
import { useTeamStore } from '@/store/team'

// Props
interface Props {
  team: Team
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  click: []
}>()

// Stores
const authStore = useAuthStore()
const teamStore = useTeamStore()

// 获取当前用户在该团队的角色
const userRole = computed<TeamRole | null>(() => {
  if (!authStore.user) return null
  
  // 如果是创建者，返回 OWNER
  if (props.team.creatorId === authStore.user.id) {
    return 'OWNER'
  }
  
  // 否则需要从团队成员中查找（这里简化处理，实际可能需要额外的 API 调用）
  // 由于 TeamCard 通常在列表中使用，不应该为每个卡片都调用 API
  // 可以考虑在父组件中批量获取用户角色信息
  return null
})

// 角色标签类型
const roleTagType = computed(() => {
  switch (userRole.value) {
    case 'OWNER':
    case 'LEADER':
      return 'danger'
    case 'ADMIN':
      return 'warning'
    case 'MEMBER':
      return 'info'
    default:
      return 'info'
  }
})

// 角色标签文本
const roleLabel = computed(() => {
  switch (userRole.value) {
    case 'OWNER':
      return '所有者'
    case 'LEADER':
      return '队长'
    case 'ADMIN':
      return '管理员'
    case 'MEMBER':
      return '成员'
    default:
      return '成员'
  }
})

// 格式化日期
const formatDate = (date: string | Date): string => {
  const d = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return '今天'
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days} 天前`
  } else if (days < 30) {
    const weeks = Math.floor(days / 7)
    return `${weeks} 周前`
  } else if (days < 365) {
    const months = Math.floor(days / 30)
    return `${months} 个月前`
  } else {
    const years = Math.floor(days / 365)
    return `${years} 年前`
  }
}

// 处理点击事件
const handleClick = () => {
  emit('click')
}
</script>

<style scoped lang="scss">
.team-card {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: var(--radius-card);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-card);
  position: relative;
  overflow: hidden;
  
  // 悬停效果
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.05), transparent);
    opacity: 0;
    transition: opacity var(--transition-base);
    pointer-events: none;
  }
  
  &:hover {
    border-color: var(--border-card-hover);
    box-shadow: var(--shadow-card-hover);
    transform: translateY(-4px);
    
    &::before {
      opacity: 1;
    }
    
    .team-avatar {
      transform: scale(1.05);
    }
    
    .team-name {
      color: var(--accent-color);
    }
  }
  
  &:focus {
    outline: 2px solid var(--accent-color);
    outline-offset: 2px;
  }
  
  &:active {
    transform: translateY(-2px);
  }
}

.team-avatar {
  flex-shrink: 0;
  transition: transform var(--transition-smooth);
}

.team-info {
  flex: 1;
  min-width: 0; // 防止文本溢出
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.team-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.team-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  transition: color var(--transition-base);
}

.role-tag {
  flex-shrink: 0;
  animation: slideInRight 0.3s var(--ease-out);
}

.team-description {
  margin: 0;
  font-size: 14px;
  color: var(--text-color-muted);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.team-meta {
  display: flex;
  gap: var(--spacing-md);
  font-size: 13px;
  color: var(--text-color-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color var(--transition-fast);
  
  .el-icon {
    font-size: 14px;
  }
  
  &:hover {
    color: var(--accent-color);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .team-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .team-header {
    flex-direction: column;
    gap: 4px;
  }
  
  .team-meta {
    justify-content: center;
  }
}
</style>
