<template>
  <el-tooltip
    :content="tooltipContent"
    placement="top"
    :disabled="!showTooltip"
  >
    <div 
      class="member-avatar-wrapper"
      tabindex="0"
      role="button"
      :aria-label="`团队成员: ${member.username || member.nickname || '未知用户'}, 角色: ${roleText}`"
      @click="handleClick"
      @keydown.enter="handleClick"
      @keydown.space.prevent="handleClick"
    >
      <el-avatar
        :size="size"
        :src="member.avatar"
        class="member-avatar"
        lazy
        role="img"
        :aria-label="`${member.username || member.nickname || '未知用户'} 的头像`"
      >
        {{ fallbackText }}
      </el-avatar>
      <el-tag
        v-if="showRole"
        :type="roleTagType"
        size="small"
        class="role-tag"
        :aria-label="`角色: ${roleText}`"
      >
        {{ roleText }}
      </el-tag>
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TeamMember } from '@/types/team'

interface Props {
  member: TeamMember
  size?: number
  showRole?: boolean
  showTooltip?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 40,
  showRole: false,
  showTooltip: true
})

// Emits
const emit = defineEmits<{
  click: []
}>()

// Handle click event
const handleClick = () => {
  emit('click')
}

// 头像回退文本（显示用户名首字符）
const fallbackText = computed(() => {
  const name = props.member.username || props.member.nickname || '?'
  return name.charAt(0).toUpperCase()
})

// 角色文本
const roleText = computed(() => {
  const roleMap: Record<string, string> = {
    'OWNER': '所有者',
    'LEADER': '队长',
    'ADMIN': '管理员',
    'MEMBER': '成员'
  }
  return roleMap[props.member.role] || '成员'
})

// 角色标签类型
const roleTagType = computed(() => {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | ''> = {
    'OWNER': 'success',
    'LEADER': 'success',
    'ADMIN': 'warning',
    'MEMBER': 'info'
  }
  return typeMap[props.member.role] || 'info'
})

// Tooltip 内容
const tooltipContent = computed(() => {
  const name = props.member.username || props.member.nickname || '未知用户'
  const role = roleText.value
  const email = props.member.email ? `\n${props.member.email}` : ''
  const department = props.member.department ? `\n${props.member.department}` : ''
  
  return `${name} (${role})${email}${department}`
})
</script>

<style scoped lang="scss">
.member-avatar-wrapper {
  position: relative;
  display: inline-block;
  cursor: pointer;
  outline: none;

  &:focus {
    outline: 2px solid var(--accent-color);
    outline-offset: 2px;
    border-radius: 50%;
  }

  .member-avatar {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: scale(1.1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }

  .role-tag {
    position: absolute;
    bottom: -4px;
    right: -4px;
    font-size: 10px;
    padding: 0 4px;
    height: 16px;
    line-height: 16px;
    border-radius: 8px;
    border: 2px solid #fff;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
}
</style>
