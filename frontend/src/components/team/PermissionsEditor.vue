<template>
  <div class="permissions-editor">
    <el-alert type="warning" :closable="false" style="margin-bottom: 24px;">
      <template #title>
        <el-icon><Warning /></el-icon>
        <span>权限设置仅队长可修改</span>
      </template>
    </el-alert>

    <el-card class="permission-card" shadow="never">
      <template #header>
        <h3>快捷入口与工具分组编辑权限</h3>
      </template>

      <el-radio-group v-model="localShortcutsPermission">
        <el-radio label="leader">
          <div class="radio-content">
            <div class="radio-title">仅队长可编辑</div>
            <div class="radio-desc">只有团队队长可以添加、修改、删除快捷入口和工具分组</div>
          </div>
        </el-radio>
        <el-radio label="all">
          <div class="radio-content">
            <div class="radio-title">所有成员可编辑</div>
            <div class="radio-desc">团队所有成员都可以编辑快捷入口和工具分组</div>
          </div>
        </el-radio>
      </el-radio-group>
    </el-card>

    <el-card class="permission-card" shadow="never">
      <template #header>
        <h3>团队首页信息编辑权限</h3>
      </template>

      <el-radio-group v-model="localAnnouncementPermission">
        <el-radio label="leader">
          <div class="radio-content">
            <div class="radio-title">仅队长可编辑</div>
            <div class="radio-desc">只有团队队长可以编辑公告、规范链接和新人指引</div>
          </div>
        </el-radio>
        <el-radio label="all">
          <div class="radio-content">
            <div class="radio-title">所有成员可编辑</div>
            <div class="radio-desc">团队所有成员都可以编辑团队首页信息</div>
          </div>
        </el-radio>
      </el-radio-group>
    </el-card>

    <el-card class="info-card" shadow="never">
      <template #header>
        <h3>权限说明</h3>
      </template>

      <div class="info-list">
        <div class="info-item">
          <el-icon color="var(--el-color-primary)"><InfoFilled /></el-icon>
          <span>权限设置会立即生效，影响所有团队成员</span>
        </div>
        <div class="info-item">
          <el-icon color="var(--el-color-primary)"><InfoFilled /></el-icon>
          <span>队长始终拥有所有编辑权限</span>
        </div>
        <div class="info-item">
          <el-icon color="var(--el-color-primary)"><InfoFilled /></el-icon>
          <span>建议根据团队规模和管理需求合理设置权限</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Warning, InfoFilled } from '@element-plus/icons-vue'

interface Props {
  shortcutsPermission: 'leader' | 'all'
  announcementPermission: 'leader' | 'all'
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:shortcutsPermission': [value: 'leader' | 'all']
  'update:announcementPermission': [value: 'leader' | 'all']
}>()

const localShortcutsPermission = computed({
  get: () => props.shortcutsPermission,
  set: (value) => emit('update:shortcutsPermission', value)
})

const localAnnouncementPermission = computed({
  get: () => props.announcementPermission,
  set: (value) => emit('update:announcementPermission', value)
})
</script>

<style scoped lang="scss">
.permissions-editor {
  .permission-card {
    margin-bottom: 24px;
    border: 1px solid var(--el-border-color-lighter);

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    :deep(.el-radio-group) {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    :deep(.el-radio) {
      height: auto;
      align-items: flex-start;
      padding: 16px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        border-color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }

      &.is-checked {
        border-color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }

      .el-radio__input {
        margin-top: 2px;
      }

      .el-radio__label {
        padding-left: 12px;
      }
    }

    .radio-content {
      .radio-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 4px;
      }

      .radio-desc {
        font-size: 13px;
        color: var(--el-text-color-secondary);
        line-height: 1.6;
      }
    }
  }

  .info-card {
    border: 1px solid var(--el-border-color-lighter);

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }

    .info-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .info-item {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        font-size: 14px;
        color: var(--el-text-color-regular);

        .el-icon {
          margin-top: 2px;
          flex-shrink: 0;
        }
      }
    }
  }
}
</style>
