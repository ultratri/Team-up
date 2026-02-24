<template>
  <div class="shortcuts-editor">
    <div class="editor-header">
      <div class="header-info">
        <h3>自定义快捷入口</h3>
        <p>配置团队常用工具的快捷访问入口，支持拖拽排序</p>
      </div>
      <el-button 
        type="primary" 
        :icon="Plus" 
        @click="addShortcut"
        :disabled="!canEdit"
      >
        添加快捷入口
      </el-button>
    </div>

    <div v-if="!canEdit" class="permission-tip">
      <el-alert type="info" :closable="false">
        <template #title>
          <el-icon><Lock /></el-icon>
          <span>您没有编辑权限，请联系队长修改</span>
        </template>
      </el-alert>
    </div>

    <div class="shortcuts-grid" v-if="modelValue.length > 0">
      <VueDraggable 
        v-model="shortcuts" 
        :disabled="!canEdit"
        class="draggable-list"
        @end="onDragEnd"
      >
        <div 
          v-for="(element, index) in shortcuts" 
          :key="element.id"
          class="shortcut-card-wrapper"
        >
          <div class="shortcut-card">
            <div class="drag-handle" v-if="canEdit">
              <el-icon><Rank /></el-icon>
            </div>
            
            <div class="shortcut-preview">
              <div 
                class="icon-preview" 
                :style="{ backgroundColor: element.color + '20', color: element.color }"
              >
                <el-icon :size="24"><component :is="getIconComponent(element.icon)" /></el-icon>
              </div>
              <div class="shortcut-info">
                <div class="shortcut-name">{{ element.name }}</div>
                <div class="shortcut-url">{{ element.url }}</div>
              </div>
            </div>

            <div class="shortcut-actions" v-if="canEdit">
              <el-button 
                size="small" 
                :icon="Edit" 
                @click="editShortcut(index)"
              />
              <el-button 
                size="small" 
                type="danger" 
                :icon="Delete" 
                @click="deleteShortcut(index)"
              />
            </div>
          </div>
        </div>
      </VueDraggable>
    </div>

    <el-empty v-else description="暂无快捷入口，点击上方按钮添加" />

    <!-- 编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="editingIndex === -1 ? '添加快捷入口' : '编辑快捷入口'"
      width="500px"
    >
      <el-form :model="editingShortcut" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="editingShortcut.name" placeholder="如：GitHub" />
        </el-form-item>

        <el-form-item label="链接" required>
          <el-input v-model="editingShortcut.url" placeholder="https://..." />
        </el-form-item>

        <el-form-item label="图标" required>
          <el-select v-model="editingShortcut.icon" placeholder="选择图标">
            <el-option
              v-for="icon in BUILTIN_ICONS"
              :key="icon.value"
              :label="icon.label"
              :value="icon.value"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <div 
                  style="width: 20px; height: 20px; border-radius: 4px; display: flex; align-items: center; justify-content: center;"
                  :style="{ backgroundColor: icon.color + '20', color: icon.color }"
                >
                  <el-icon><Link /></el-icon>
                </div>
                <span>{{ icon.label }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="颜色" required>
          <el-color-picker v-model="editingShortcut.color" />
          <span style="margin-left: 12px; color: var(--el-text-color-secondary);">
            {{ editingShortcut.color }}
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveShortcut">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Delete, Lock, Rank, Link } from '@element-plus/icons-vue'
import { VueDraggable } from 'vue-draggable-plus'
import type { Shortcut } from '@/types/team-config'
import { BUILTIN_ICONS } from '@/types/team-config'

interface Props {
  modelValue: Shortcut[]
  canEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  canEdit: true
})

const emit = defineEmits<{
  'update:modelValue': [value: Shortcut[]]
}>()

const shortcuts = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const dialogVisible = ref(false)
const editingIndex = ref(-1)
const editingShortcut = ref<Shortcut>({
  id: '',
  name: '',
  url: '',
  icon: 'link',
  color: '#409eff',
  order: 0
})

const addShortcut = () => {
  editingIndex.value = -1
  editingShortcut.value = {
    id: `shortcut_${Date.now()}`,
    name: '',
    url: '',
    icon: 'link',
    color: '#409eff',
    order: shortcuts.value.length
  }
  dialogVisible.value = true
}

const editShortcut = (index: number) => {
  editingIndex.value = index
  editingShortcut.value = { ...shortcuts.value[index] }
  dialogVisible.value = true
}

const saveShortcut = () => {
  if (!editingShortcut.value.name || !editingShortcut.value.url) {
    return
  }

  const newShortcuts = [...shortcuts.value]
  if (editingIndex.value === -1) {
    newShortcuts.push(editingShortcut.value)
  } else {
    newShortcuts[editingIndex.value] = editingShortcut.value
  }
  
  emit('update:modelValue', newShortcuts)
  dialogVisible.value = false
}

const deleteShortcut = (index: number) => {
  const newShortcuts = shortcuts.value.filter((_, i) => i !== index)
  emit('update:modelValue', newShortcuts)
}

const onDragEnd = () => {
  // 更新 order 字段
  shortcuts.value.forEach((item, index) => {
    item.order = index
  })
}

const getIconComponent = (icon: string) => {
  return Link // 简化处理，实际可以根据 icon 返回不同组件
}
</script>

<style scoped lang="scss">
.shortcuts-editor {
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

  .shortcuts-grid {
    .draggable-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
    }

    .shortcut-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: var(--el-fill-color-blank);
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        border-color: var(--el-color-primary);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .drag-handle {
        cursor: move;
        color: var(--el-text-color-secondary);
        
        &:hover {
          color: var(--el-color-primary);
        }
      }

      .shortcut-preview {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;

        .icon-preview {
          width: 48px;
          height: 48px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .shortcut-info {
          flex: 1;
          min-width: 0;

          .shortcut-name {
            font-size: 14px;
            font-weight: 600;
            color: var(--el-text-color-primary);
            margin-bottom: 4px;
          }

          .shortcut-url {
            font-size: 12px;
            color: var(--el-text-color-secondary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }

      .shortcut-actions {
        display: flex;
        gap: 8px;
      }
    }
  }
}
</style>
