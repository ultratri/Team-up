<template>
  <div class="team-homepage-editor">
    <div v-if="!canEdit" class="permission-tip">
      <el-alert type="info" :closable="false">
        <template #title>
          <el-icon><Lock /></el-icon>
          <span>您没有编辑权限</span>
        </template>
      </el-alert>
    </div>

    <!-- 团队公告 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>团队公告</h3>
          <p>支持 Markdown 格式，用于发布重要通知</p>
        </div>
      </template>

      <el-input
        v-model="localAnnouncement"
        type="textarea"
        :rows="8"
        placeholder="输入团队公告内容，支持 Markdown..."
        :disabled="!canEdit"
      />

      <div class="preview-section" v-if="localAnnouncement">
        <el-divider content-position="left">预览</el-divider>
        <div class="markdown-preview" v-html="renderedMarkdown"></div>
      </div>
    </el-card>

    <!-- 常用规范链接 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>常用规范链接</h3>
          <p>代码规范、提交流程、命名规范等</p>
          <el-button 
            size="small" 
            :icon="Plus" 
            @click="addGuideline"
            :disabled="!canEdit"
          >
            添加规范
          </el-button>
        </div>
      </template>

      <div class="guidelines-list" v-if="localGuidelines.length > 0">
        <div 
          v-for="(guideline, index) in localGuidelines" 
          :key="guideline.id"
          class="guideline-item"
        >
          <div class="guideline-info">
            <el-tag size="small" type="info">{{ guideline.category }}</el-tag>
            <span class="guideline-name">{{ guideline.name }}</span>
            <a :href="guideline.url" target="_blank" class="guideline-url">
              {{ guideline.url }}
            </a>
          </div>
          <div class="guideline-actions" v-if="canEdit">
            <el-button size="small" :icon="Edit" @click="editGuideline(index)" />
            <el-button size="small" type="danger" :icon="Delete" @click="deleteGuideline(index)" />
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无规范链接" :image-size="60" />
    </el-card>

    <!-- 新人入队指引 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h3>新人入队指引</h3>
          <p>帮助新成员快速了解团队流程</p>
          <el-button 
            size="small" 
            :icon="Plus" 
            @click="addChecklistItem"
            :disabled="!canEdit"
          >
            添加步骤
          </el-button>
        </div>
      </template>

      <div class="checklist" v-if="localChecklist.length > 0">
        <div 
          v-for="(item, index) in localChecklist" 
          :key="item.id"
          class="checklist-item"
        >
          <div class="item-order">{{ index + 1 }}</div>
          <div class="item-content">
            <div class="item-title">
              {{ item.title }}
              <el-tag v-if="item.required" size="small" type="danger">必须</el-tag>
            </div>
            <div class="item-desc" v-if="item.description">{{ item.description }}</div>
          </div>
          <div class="item-actions" v-if="canEdit">
            <el-button size="small" :icon="Edit" @click="editChecklistItem(index)" />
            <el-button size="small" type="danger" :icon="Delete" @click="deleteChecklistItem(index)" />
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无入队指引" :image-size="60" />
    </el-card>

    <!-- 规范链接对话框 -->
    <el-dialog 
      v-model="guidelineDialogVisible" 
      :title="editingGuidelineIndex === -1 ? '添加规范' : '编辑规范'"
      width="500px"
    >
      <el-form :model="editingGuideline" label-width="80px">
        <el-form-item label="规范名称" required>
          <el-input v-model="editingGuideline.name" placeholder="如：代码规范" />
        </el-form-item>

        <el-form-item label="链接" required>
          <el-input v-model="editingGuideline.url" placeholder="https://..." />
        </el-form-item>

        <el-form-item label="分类" required>
          <el-select v-model="editingGuideline.category" placeholder="选择分类">
            <el-option label="代码规范" value="代码规范" />
            <el-option label="提交规范" value="提交规范" />
            <el-option label="命名规范" value="命名规范" />
            <el-option label="流程规范" value="流程规范" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="guidelineDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGuideline">确定</el-button>
      </template>
    </el-dialog>

    <!-- 检查清单对话框 -->
    <el-dialog 
      v-model="checklistDialogVisible" 
      :title="editingChecklistIndex === -1 ? '添加步骤' : '编辑步骤'"
      width="500px"
    >
      <el-form :model="editingChecklistItem" label-width="80px">
        <el-form-item label="步骤标题" required>
          <el-input v-model="editingChecklistItem.title" placeholder="如：配置开发环境" />
        </el-form-item>

        <el-form-item label="详细说明">
          <el-input 
            v-model="editingChecklistItem.description" 
            type="textarea"
            :rows="3"
            placeholder="详细说明（可选）"
          />
        </el-form-item>

        <el-form-item label="是否必须">
          <el-switch v-model="editingChecklistItem.required" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="checklistDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveChecklistItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Delete, Lock } from '@element-plus/icons-vue'
import { marked } from 'marked'
import type { Guideline, ChecklistItem } from '@/types/team-config'

interface Props {
  announcement?: string
  guidelines: Guideline[]
  checklist: ChecklistItem[]
  canEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  canEdit: true,
  announcement: ''
})

const emit = defineEmits<{
  'update:announcement': [value: string]
  'update:guidelines': [value: Guideline[]]
  'update:checklist': [value: ChecklistItem[]]
}>()

const localAnnouncement = computed({
  get: () => props.announcement,
  set: (value) => emit('update:announcement', value)
})

const localGuidelines = computed({
  get: () => props.guidelines,
  set: (value) => emit('update:guidelines', value)
})

const localChecklist = computed({
  get: () => props.checklist,
  set: (value) => emit('update:checklist', value)
})

const renderedMarkdown = computed(() => {
  return marked(localAnnouncement.value || '')
})

// 规范链接相关
const guidelineDialogVisible = ref(false)
const editingGuidelineIndex = ref(-1)
const editingGuideline = ref<Guideline>({
  id: '',
  name: '',
  url: '',
  category: '代码规范'
})

const addGuideline = () => {
  editingGuidelineIndex.value = -1
  editingGuideline.value = {
    id: `guideline_${Date.now()}`,
    name: '',
    url: '',
    category: '代码规范'
  }
  guidelineDialogVisible.value = true
}

const editGuideline = (index: number) => {
  editingGuidelineIndex.value = index
  editingGuideline.value = { ...localGuidelines.value[index] }
  guidelineDialogVisible.value = true
}

const saveGuideline = () => {
  if (!editingGuideline.value.name || !editingGuideline.value.url) return

  const newGuidelines = [...localGuidelines.value]
  if (editingGuidelineIndex.value === -1) {
    newGuidelines.push(editingGuideline.value)
  } else {
    newGuidelines[editingGuidelineIndex.value] = editingGuideline.value
  }
  
  emit('update:guidelines', newGuidelines)
  guidelineDialogVisible.value = false
}

const deleteGuideline = (index: number) => {
  const newGuidelines = localGuidelines.value.filter((_, i) => i !== index)
  emit('update:guidelines', newGuidelines)
}

// 检查清单相关
const checklistDialogVisible = ref(false)
const editingChecklistIndex = ref(-1)
const editingChecklistItem = ref<ChecklistItem>({
  id: '',
  title: '',
  description: '',
  order: 0,
  required: false
})

const addChecklistItem = () => {
  editingChecklistIndex.value = -1
  editingChecklistItem.value = {
    id: `checklist_${Date.now()}`,
    title: '',
    description: '',
    order: localChecklist.value.length,
    required: false
  }
  checklistDialogVisible.value = true
}

const editChecklistItem = (index: number) => {
  editingChecklistIndex.value = index
  editingChecklistItem.value = { ...localChecklist.value[index] }
  checklistDialogVisible.value = true
}

const saveChecklistItem = () => {
  if (!editingChecklistItem.value.title) return

  const newChecklist = [...localChecklist.value]
  if (editingChecklistIndex.value === -1) {
    newChecklist.push(editingChecklistItem.value)
  } else {
    newChecklist[editingChecklistIndex.value] = editingChecklistItem.value
  }
  
  emit('update:checklist', newChecklist)
  checklistDialogVisible.value = false
}

const deleteChecklistItem = (index: number) => {
  const newChecklist = localChecklist.value.filter((_, i) => i !== index)
  emit('update:checklist', newChecklist)
}
</script>

<style scoped lang="scss">
.team-homepage-editor {
  .permission-tip {
    margin-bottom: 20px;

    :deep(.el-alert__title) {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .section-card {
    margin-bottom: 24px;
    border: 1px solid var(--el-border-color-lighter);

    .card-header {
      h3 {
        margin: 0 0 4px 0;
        font-size: 16px;
        font-weight: 600;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }

      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .preview-section {
      margin-top: 16px;

      .markdown-preview {
        padding: 16px;
        background: var(--el-fill-color-light);
        border-radius: 6px;
        font-size: 14px;
        line-height: 1.8;
      }
    }

    .guidelines-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .guideline-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px;
        background: var(--el-fill-color-light);
        border-radius: 6px;

        .guideline-info {
          display: flex;
          align-items: center;
          gap: 12px;
          flex: 1;
          min-width: 0;

          .guideline-name {
            font-weight: 600;
          }

          .guideline-url {
            font-size: 12px;
            color: var(--el-color-primary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .guideline-actions {
          display: flex;
          gap: 8px;
        }
      }
    }

    .checklist {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .checklist-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px;
        background: var(--el-fill-color-light);
        border-radius: 6px;

        .item-order {
          width: 28px;
          height: 28px;
          background: var(--el-color-primary);
          color: white;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 600;
          flex-shrink: 0;
        }

        .item-content {
          flex: 1;
          min-width: 0;

          .item-title {
            font-weight: 600;
            margin-bottom: 4px;
            display: flex;
            align-items: center;
            gap: 8px;
          }

          .item-desc {
            font-size: 13px;
            color: var(--el-text-color-secondary);
          }
        }

        .item-actions {
          display: flex;
          gap: 8px;
        }
      }
    }
  }
}
</style>
