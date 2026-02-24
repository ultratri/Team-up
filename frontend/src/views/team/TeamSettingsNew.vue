<template>
  <div class="team-settings-new">
    <div class="page-header">
      <div class="title-area">
        <h2 class="page-title">团队设置</h2>
        <div class="page-subtitle">自定义团队工具、快捷入口和首页信息</div>
      </div>

      <div class="actions">
        <el-button @click="loadConfig" :disabled="loading">刷新</el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">保存设置</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="10" />

    <el-tabs v-else v-model="activeTab" class="settings-tabs">
      <!-- 快捷入口 -->
      <el-tab-pane label="快捷入口" name="shortcuts">
        <ShortcutsEditor
          v-model="config.shortcuts"
          :can-edit="config.canEditShortcuts"
        />
      </el-tab-pane>

      <!-- 工具分组 -->
      <el-tab-pane label="工具分组" name="groups">
        <ToolGroupsEditor
          v-model="config.groups"
          :can-edit="config.canEditShortcuts"
        />
      </el-tab-pane>

      <!-- 团队首页 -->
      <el-tab-pane label="团队首页" name="homepage">
        <TeamHomepageEditor
          v-model:announcement="config.teamAnnouncement"
          v-model:guidelines="config.teamGuidelines"
          v-model:checklist="config.onboardingChecklist"
          :can-edit="config.canEditAnnouncement"
        />
      </el-tab-pane>

      <!-- 权限设置 -->
      <el-tab-pane label="权限设置" name="permissions" v-if="isLeader">
        <PermissionsEditor
          v-model:shortcuts-permission="config.shortcutsEditPermission"
          v-model:announcement-permission="config.announcementEditPermission"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { getTeamCustomConfig, updateTeamCustomConfig } from '@/api/team-config'
import type { TeamCustomConfig } from '@/types/team-config'
import ShortcutsEditor from '@/components/team/ShortcutsEditor.vue'
import ToolGroupsEditor from '@/components/team/ToolGroupsEditor.vue'
import TeamHomepageEditor from '@/components/team/TeamHomepageEditor.vue'
import PermissionsEditor from '@/components/team/PermissionsEditor.vue'

const route = useRoute()
const teamId = computed(() => Number(route.params.id))

const loading = ref(true)
const saving = ref(false)
const activeTab = ref('shortcuts')
const isLeader = ref(false) // TODO: 从用户信息中获取

const config = ref<TeamCustomConfig>({
  teamId: teamId.value,
  shortcuts: [],
  groups: [],
  teamGuidelines: [],
  onboardingChecklist: [],
  shortcutsEditPermission: 'leader',
  announcementEditPermission: 'leader'
})

const loadConfig = async () => {
  loading.value = true
  try {
    const data = await getTeamCustomConfig(teamId.value)
    config.value = data
  } catch (error) {
    console.error('加载配置失败', error)
    ElMessage.error('无法获取团队配置')
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    await updateTeamCustomConfig(teamId.value, config.value)
    ElMessage.success('配置已保存')
  } catch (error: any) {
    console.error('保存配置失败', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped lang="scss">
.team-settings-new {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  .page-title {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }

  .page-subtitle {
    margin-top: 8px;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }

  .actions {
    display: flex;
    gap: 12px;
  }
}

.settings-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }
}
</style>
