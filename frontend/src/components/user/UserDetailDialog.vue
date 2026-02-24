<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    width="800px"
    :show-close="false"
    destroy-on-close
    class="mentor-detail-dialog"
  >
    <div v-if="userDetail" class="mentor-detail">
      <button class="close-btn" @click="handleClose">
        <el-icon><Close /></el-icon>
      </button>
      <div class="mentor-card-top">
        <div class="mentor-avatar-section">
          <el-avatar :size="100" :src="userDetail.avatarUrl" class="mentor-avatar-large">
            {{ userDetail.realName?.charAt(0) }}
          </el-avatar>
        </div>
        <div class="mentor-info-section">
          <h2 class="mentor-name-large">{{ userDetail.realName }}</h2>
          <p class="mentor-dept-large">{{ userDetail.department }} · {{ userDetail.major }}</p>
        </div>
      </div>
      <div class="mentor-content-area">
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><User /></el-icon>
              <span>个人简介</span>
            </div>
          </div>
          <div class="block-content">
            <p class="text-content">{{ userDetail.bio || '这位导师还没有填写个人简介。' }}</p>
          </div>
        </div>
      </div>
      <div class="mentor-actions">
        <el-button size="large" @click="handleClose">关闭</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Close, User } from '@element-plus/icons-vue'
import { getUserProfile } from '@/api/user'

interface Props {
  modelValue: boolean
  userId?: number | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const userDetail = ref<any>(null)

const handleClose = () => {
  emit('update:modelValue', false)
}

const loadUserDetail = async () => {
  if (!props.userId) return
  try {
    const profile = await getUserProfile(props.userId)
    userDetail.value = profile
  } catch (error) {
    console.error('加载用户详情失败:', error)
  }
}

watch(() => props.modelValue, (newVal) => {
  if (newVal && props.userId) {
    loadUserDetail()
  }
})
</script>

<style scoped lang="scss">
.mentor-detail-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    background: var(--bg-card);
  }
}

.mentor-detail {
  position: relative;
  
  .close-btn {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 10;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: none;
    background: rgba(0,0,0,0.05);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .mentor-card-top {
    padding: 40px;
    display: flex;
    gap: 24px;
    align-items: center;
  }
  
  .mentor-name-large { 
    margin: 0 0 8px 0; 
    font-size: 36px; 
    font-weight: 900; 
  }
  
  .mentor-dept-large { 
    margin: 0; 
    font-size: 16px; 
    color: var(--text-color-muted); 
  }
  
  .mentor-content-area {
    padding: 32px 40px;
  }
  
  .content-block {
    margin-bottom: 32px;
  }
  
  .block-header {
    display: flex;
    margin-bottom: 16px;
  }
  
  .block-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 18px;
    font-weight: 700;
  }
  
  .block-content {
    padding: 20px;
    background: var(--bg-card-hover);
    border-radius: 16px;
  }
  
  .text-content {
    margin: 0;
    line-height: 1.8;
  }
  
  .mentor-actions {
    padding: 24px 40px;
    display: flex;
    justify-content: center;
    border-top: 1px solid var(--border-card);
  }
}
</style>
