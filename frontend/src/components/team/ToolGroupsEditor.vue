<template>
  <div class="tool-groups-editor">
    <div class="editor-header">
      <div class="header-info">
        <h3>工具分组管理</h3>
        <p>将团队工具按类别分组，便于成员快速查找</p>
      </div>
      <el-button 
        type="primary" 
        :icon="Plus" 
        @click="addGroup"
        :disabled="!canEdit"
      >
        添加分组
      </el-button>
    </div>

    <div v-if="!canEdit" class="permission-tip">
      <el-alert type="info" :closable="false">
        <template #title>
          <el-icon><Lock /></el-icon>
          <span>您没有编辑权限</span>
        </template>
      </el-alert>
    </div>

    <div class="groups-list" v-if="modelValue.length > 0">
      <el-collapse v-model="activeGroups">
        <el-collapse-item 
          v-for="(group, groupIndex) in modelValue" 
          :key="group.id"
          :name="group.id"
        >
          <template #title>
            <div class="group-title">
              <el-icon v-if="canEdit"><Rank /></el-icon>
              <span class="group-name">{{ group.name }}</span>
              <el-tag size="small">{{ group.links.length }} 个链接</el-tag>
            </div>
          </template>

          <div class="group-content">
            <div class="group-actions" v-if="canEdit">
              <el-button size="small" :icon="Edit" @click="editGroup(groupIndex)">
                编辑分组
              </el-button>
              <el-button size="small" :icon="Plus" @click="addLink(groupIndex)">
                添加链接
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                :icon="Delete" 
                @click="deleteGroup(groupIndex)"
              >
                删除分组
              </el-button>
            </div>

            <div class="links-list" v-if="group.links.length > 0">
              <div 
                v-for="(link, linkIndex) in group.links" 
                :key="link.id"
                class="link-item"
              >
                <div class="link-icon">
                  <el-icon><Link /></el-icon>
                </div>
                <div class="link-info">
                  <div class="link-name">{{ link.name }}</div>
                  <div class="link-url">{{ link.url }}</div>
                  <div class="link-desc" v-if="link.description">{{ link.description }}</div>
                </div>
                <div class="link-actions" v-if="canEdit">
                  <el-button 
                    size="small" 
                    :icon="Edit" 
                    @click="editLink(groupIndex, linkIndex)"
                  />
                  <el-button 
                    size="small" 
                    type="danger" 
                    :icon="Delete" 
                    @click="deleteLink(groupIndex, linkIndex)"
                  />
                </div>
              </div>
            </div>

            <el-empty v-else description="暂无链接" :image-size="60" />
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <el-empty v-else description="暂无分组" />

    <!-- 编辑分组对话框 -->
    <el-dialog 
      v-model="groupDialogVisible" 
      :title="editingGroupIndex === -1 ? '添加分组' : '编辑分组'"
      width="400px"
    >
      <el-form :model="editingGroup" label-width="80px">
        <el-form-item label="分组名称" required>
          <el-input v-model="editingGroup.name" placeholder="如：研发工具" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑链接对话框 -->
    <el-dialog 
      v-model="linkDialogVisible" 
      :title="editingLinkIndex === -1 ? '添加链接' : '编辑链接'"
      width="500px"
    >
      <el-form :model="editingLink" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="editingLink.name" placeholder="如：项目仓库" />
        </el-form-item>

        <el-form-item label="链接" required>
          <el-input v-model="editingLink.url" placeholder="https://..." />
        </el-form-item>

        <el-form-item label="描述">
          <el-input 
            v-model="editingLink.description" 
            type="textarea" 
            :rows="2"
            placeholder="简短描述（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLink">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Delete, Lock, Rank, Link } from '@element-plus/icons-vue'
import type { ToolGroup, GroupLink } from '@/types/team-config'

interface Props {
  modelValue: ToolGroup[]
  canEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  canEdit: true
})

const emit = defineEmits<{
  'update:modelValue': [value: ToolGroup[]]
}>()

const activeGroups = ref<string[]>([])
const groupDialogVisible = ref(false)
const linkDialogVisible = ref(false)
const editingGroupIndex = ref(-1)
const editingLinkIndex = ref(-1)
const currentGroupIndex = ref(-1)

const editingGroup = ref<ToolGroup>({
  id: '',
  name: '',
  order: 0,
  links: []
})

const editingLink = ref<GroupLink>({
  id: '',
  name: '',
  url: '',
  icon: 'link',
  description: ''
})

const addGroup = () => {
  editingGroupIndex.value = -1
  editingGroup.value = {
    id: `group_${Date.now()}`,
    name: '',
    order: props.modelValue.length,
    links: []
  }
  groupDialogVisible.value = true
}

const editGroup = (index: number) => {
  editingGroupIndex.value = index
  editingGroup.value = { ...props.modelValue[index] }
  groupDialogVisible.value = true
}

const saveGroup = () => {
  if (!editingGroup.value.name) return

  const newGroups = [...props.modelValue]
  if (editingGroupIndex.value === -1) {
    newGroups.push(editingGroup.value)
  } else {
    newGroups[editingGroupIndex.value] = editingGroup.value
  }
  
  emit('update:modelValue', newGroups)
  groupDialogVisible.value = false
}

const deleteGroup = (index: number) => {
  const newGroups = props.modelValue.filter((_, i) => i !== index)
  emit('update:modelValue', newGroups)
}

const addLink = (groupIndex: number) => {
  currentGroupIndex.value = groupIndex
  editingLinkIndex.value = -1
  editingLink.value = {
    id: `link_${Date.now()}`,
    name: '',
    url: '',
    icon: 'link',
    description: ''
  }
  linkDialogVisible.value = true
}

const editLink = (groupIndex: number, linkIndex: number) => {
  currentGroupIndex.value = groupIndex
  editingLinkIndex.value = linkIndex
  editingLink.value = { ...props.modelValue[groupIndex].links[linkIndex] }
  linkDialogVisible.value = true
}

const saveLink = () => {
  if (!editingLink.value.name || !editingLink.value.url) return

  const newGroups = [...props.modelValue]
  const group = { ...newGroups[currentGroupIndex.value] }
  const links = [...group.links]

  if (editingLinkIndex.value === -1) {
    links.push(editingLink.value)
  } else {
    links[editingLinkIndex.value] = editingLink.value
  }

  group.links = links
  newGroups[currentGroupIndex.value] = group
  
  emit('update:modelValue', newGroups)
  linkDialogVisible.value = false
}

const deleteLink = (groupIndex: number, linkIndex: number) => {
  const newGroups = [...props.modelValue]
  const group = { ...newGroups[groupIndex] }
  group.links = group.links.filter((_, i) => i !== linkIndex)
  newGroups[groupIndex] = group
  
  emit('update:modelValue', newGroups)
}
</script>

<style scoped lang="scss">
.tool-groups-editor {
  .editor-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;

    .header-info {
      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 600;
      }

      p {
        margin: 0;
        font-size: 14px;
        color: var(--el-text-color-secondary);
      }
    }
  }

  .permission-tip {
    margin-bottom: 20px;

    :deep(.el-alert__title) {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .groups-list {
    .group-title {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;

      .group-name {
        font-weight: 600;
      }
    }

    .group-content {
      padding: 16px 0;

      .group-actions {
        display: flex;
        gap: 8px;
        margin-bottom: 16px;
      }

      .links-list {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .link-item {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 12px;
          background: var(--el-fill-color-light);
          border-radius: 6px;

          .link-icon {
            width: 32px;
            height: 32px;
            background: var(--el-color-primary-light-9);
            color: var(--el-color-primary);
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
          }

          .link-info {
            flex: 1;
            min-width: 0;

            .link-name {
              font-size: 14px;
              font-weight: 600;
              margin-bottom: 4px;
            }

            .link-url {
              font-size: 12px;
              color: var(--el-color-primary);
              margin-bottom: 4px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .link-desc {
              font-size: 12px;
              color: var(--el-text-color-secondary);
            }
          }

          .link-actions {
            display: flex;
            gap: 8px;
          }
        }
      }
    }
  }
}
</style>
