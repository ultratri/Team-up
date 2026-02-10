<template>
  <!-- 主对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :show-close="true"
    :close-on-click-modal="false"
    @close="handleClose"
    role="dialog"
    aria-labelledby="create-team-dialog-title"
    aria-describedby="create-team-dialog-description"
    :style="dialogContainerStyle"
    custom-class="draggable-resizable-dialog"
    append-to-body
  >
    <!-- 自定义标题栏 -->
    <template #header>
      <div 
        class="dialog-header"
        @mousedown="handleDragStart"
        :style="{ cursor: isDragging ? 'grabbing' : 'grab' }"
      >
        <span class="dialog-title" id="create-team-dialog-title">创建团队</span>
      </div>
    </template>

    <div id="create-team-dialog-description" class="sr-only">
      填写团队信息以创建新团队
    </div>
    
    <!-- 调整大小的边框 -->
    <div class="resize-handle resize-n" @mousedown="handleResizeStart($event, 'n')"></div>
    <div class="resize-handle resize-s" @mousedown="handleResizeStart($event, 's')"></div>
    <div class="resize-handle resize-w" @mousedown="handleResizeStart($event, 'w')"></div>
    <div class="resize-handle resize-e" @mousedown="handleResizeStart($event, 'e')"></div>
    <div class="resize-handle resize-nw" @mousedown="handleResizeStart($event, 'nw')"></div>
    <div class="resize-handle resize-ne" @mousedown="handleResizeStart($event, 'ne')"></div>
    <div class="resize-handle resize-sw" @mousedown="handleResizeStart($event, 'sw')"></div>
    <div class="resize-handle resize-se" @mousedown="handleResizeStart($event, 'se')"></div>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      @submit.prevent="handleSubmit"
      role="form"
      aria-label="创建团队表单"
    >
      <el-form-item label="团队名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入团队名称（2-50个字符）"
          maxlength="50"
          show-word-limit
          clearable
          aria-label="团队名称"
          aria-required="true"
          aria-describedby="team-name-help"
        />
        <span id="team-name-help" class="sr-only">
          团队名称必填，长度在2到50个字符之间
        </span>
      </el-form-item>

      <el-form-item label="团队描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          placeholder="请输入团队描述（选填，最多500字）"
          :rows="4"
          maxlength="500"
          show-word-limit
          clearable
          aria-label="团队描述"
          aria-describedby="team-description-help"
        />
        <span id="team-description-help" class="sr-only">
          团队描述选填，最多500个字符
        </span>
      </el-form-item>

      <el-form-item label="团队头像" prop="avatar">
        <el-input
          v-model="formData.avatar"
          placeholder="请输入头像URL（选填）"
          clearable
          aria-label="团队头像URL"
        >
          <template #append>
            <el-button 
              :icon="Picture"
              @click="handleSelectAvatar"
              @keydown.enter="handleSelectAvatar"
              @keydown.space.prevent="handleSelectAvatar"
              aria-label="选择头像"
            >
              选择
            </el-button>
          </template>
        </el-input>
        <div v-if="formData.avatar" class="avatar-preview" role="img" :aria-label="`头像预览: ${formData.avatar}`">
          <el-avatar :src="formData.avatar" :size="60" />
        </div>
      </el-form-item>

      <el-form-item label="关联项目" prop="projectId">
        <el-select
          v-model="formData.projectId"
          placeholder="请选择关联项目（选填）"
          clearable
          filterable
          :loading="projectsLoading"
          class="full-width"
          aria-label="关联项目"
          :aria-busy="projectsLoading"
        >
          <el-option
            v-for="project in projects"
            :key="project.id"
            :label="project.title"
            :value="project.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer" role="group" aria-label="对话框操作按钮">
        <el-button 
          @click="handleCancel" 
          :disabled="submitting"
          aria-label="取消创建团队"
        >
          取消
        </el-button>
        <el-button 
          @click="handleReset" 
          :disabled="submitting"
          aria-label="重置表单"
        >
          重置
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          :aria-label="submitting ? '正在创建团队' : '创建团队'"
          :aria-busy="submitting"
        >
          {{ submitting ? '创建中...' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useTeamStore } from '@/store/team'
import { useAuthStore } from '@/store/auth'
import { getProjects } from '@/api/project'
import { teamNameRules, teamDescriptionRules } from '@/utils/validation'
import type { TeamCreateRequest } from '@/types/team'
import type { Project } from '@/types/project'

// Props & Emits
interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', teamId: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Stores & Router
const teamStore = useTeamStore()
const authStore = useAuthStore()
const router = useRouter()

// Refs
const formRef = ref<FormInstance>()
const submitting = ref(false)
const projectsLoading = ref(false)
const projects = ref<Project[]>([])

// Dialog state
const isDragging = ref(false)
const isResizing = ref(false)
const resizeDirection = ref('')
const dialogPosition = ref({ x: 0, y: 0 })
const dragStart = ref({ x: 0, y: 0 })
const dialogSize = ref({ width: 600, height: 500 })
const resizeStart = ref({ x: 0, y: 0, width: 0, height: 0, posX: 0, posY: 0 })
const animationFrameId = ref<number | null>(null)
const dragMoveHandler = ref<((e: MouseEvent) => void) | null>(null)
const resizeMoveHandler = ref<((e: MouseEvent) => void) | null>(null)

// Form Data
const formData = ref<TeamCreateRequest>({
  name: '',
  description: '',
  avatar: '',
  projectId: undefined
})

// Computed
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const dialogContainerStyle = computed(() => {
  const style: any = {
    width: `${dialogSize.value.width}px`,
    height: `${dialogSize.value.height}px`
  }
  
  if (dialogPosition.value.x !== 0 || dialogPosition.value.y !== 0) {
    const centerX = (window.innerWidth - dialogSize.value.width) / 2
    const centerY = (window.innerHeight - dialogSize.value.height) / 2
    style.left = `${centerX + dialogPosition.value.x}px`
    style.top = `${centerY + dialogPosition.value.y}px`
    style.transform = 'none'
  }
  
  return style
})

// Form Rules
const rules: FormRules = {
  name: teamNameRules,
  description: teamDescriptionRules
}

// Methods

/**
 * 开始拖动
 */
const handleDragStart = (e: MouseEvent) => {
  // 防止选中文本
  e.preventDefault()
  
  isDragging.value = true
  
  // 记录鼠标相对于对话框当前位置的偏移
  dragStart.value = {
    x: e.clientX,
    y: e.clientY
  }
  
  // 记录对话框当前位置
  const currentPos = { ...dialogPosition.value }
  
  // 创建拖动处理函数，使用闭包保存初始位置
  const handleMove = (moveEvent: MouseEvent) => {
    if (!isDragging.value) return
    
    moveEvent.preventDefault()
    
    // 取消之前的动画帧
    if (animationFrameId.value !== null) {
      cancelAnimationFrame(animationFrameId.value)
    }
    
    // 使用 requestAnimationFrame 优化性能
    animationFrameId.value = requestAnimationFrame(() => {
      // 计算鼠标移动的距离
      const deltaX = moveEvent.clientX - dragStart.value.x
      const deltaY = moveEvent.clientY - dragStart.value.y
      
      // 新位置 = 初始位置 + 鼠标移动距离
      const newX = currentPos.x + deltaX
      const newY = currentPos.y + deltaY
      
      // 边界检测
      const maxX = (window.innerWidth - dialogSize.value.width) / 2
      const maxY = (window.innerHeight - dialogSize.value.height) / 2
      const minX = -maxX
      const minY = -maxY + 50 // 至少保留50px标题栏可见
      
      dialogPosition.value = {
        x: Math.max(minX, Math.min(maxX, newX)),
        y: Math.max(minY, Math.min(maxY, newY))
      }
    })
  }
  
  // 保存处理函数引用以便后续移除
  dragMoveHandler.value = handleMove
  
  document.addEventListener('mousemove', handleMove, { passive: false })
  document.addEventListener('mouseup', handleDragEnd)
  
  // 添加拖拽样式
  document.body.style.cursor = 'grabbing'
  document.body.style.userSelect = 'none'
}

/**
 * 拖动中（已废弃，使用闭包版本）
 */
const handleDragMove = (e: MouseEvent) => {
  // 此函数已被闭包版本替代
}

/**
 * 结束拖动
 */
const handleDragEnd = () => {
  isDragging.value = false
  
  if (animationFrameId.value !== null) {
    cancelAnimationFrame(animationFrameId.value)
    animationFrameId.value = null
  }
  
  if (dragMoveHandler.value) {
    document.removeEventListener('mousemove', dragMoveHandler.value)
    dragMoveHandler.value = null
  }
  document.removeEventListener('mouseup', handleDragEnd)
  
  // 恢复样式
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

/**
 * 开始调整大小
 */
const handleResizeStart = (e: MouseEvent, direction: string) => {
  e.preventDefault()
  e.stopPropagation()
  
  isResizing.value = true
  resizeDirection.value = direction
  
  // 记录初始状态
  resizeStart.value = {
    x: e.clientX,
    y: e.clientY,
    width: dialogSize.value.width,
    height: dialogSize.value.height,
    posX: dialogPosition.value.x,
    posY: dialogPosition.value.y
  }
  
  // 创建调整大小处理函数，使用闭包保存初始状态
  const handleMove = (moveEvent: MouseEvent) => {
    if (!isResizing.value) return
    
    moveEvent.preventDefault()
    
    // 取消之前的动画帧
    if (animationFrameId.value !== null) {
      cancelAnimationFrame(animationFrameId.value)
    }
    
    // 使用 requestAnimationFrame 优化性能
    animationFrameId.value = requestAnimationFrame(() => {
      const deltaX = moveEvent.clientX - resizeStart.value.x
      const deltaY = moveEvent.clientY - resizeStart.value.y
      const dir = resizeDirection.value
      
      let newWidth = resizeStart.value.width
      let newHeight = resizeStart.value.height
      let newPosX = resizeStart.value.posX
      let newPosY = resizeStart.value.posY
      
      const minWidth = 400
      const minHeight = 300
      const maxWidth = window.innerWidth - 100
      const maxHeight = window.innerHeight - 100
      
      // 处理不同方向的调整
      if (dir.includes('e')) {
        newWidth = Math.max(minWidth, Math.min(maxWidth, resizeStart.value.width + deltaX))
      }
      if (dir.includes('w')) {
        const proposedWidth = resizeStart.value.width - deltaX
        if (proposedWidth >= minWidth && proposedWidth <= maxWidth) {
          newWidth = proposedWidth
          newPosX = resizeStart.value.posX + deltaX
        }
      }
      if (dir.includes('s')) {
        newHeight = Math.max(minHeight, Math.min(maxHeight, resizeStart.value.height + deltaY))
      }
      if (dir.includes('n')) {
        const proposedHeight = resizeStart.value.height - deltaY
        if (proposedHeight >= minHeight && proposedHeight <= maxHeight) {
          newHeight = proposedHeight
          newPosY = resizeStart.value.posY + deltaY
        }
      }
      
      dialogSize.value = { width: newWidth, height: newHeight }
      dialogPosition.value = { x: newPosX, y: newPosY }
    })
  }
  
  // 保存处理函数引用
  resizeMoveHandler.value = handleMove
  
  document.addEventListener('mousemove', handleMove, { passive: false })
  document.addEventListener('mouseup', handleResizeEnd)
  
  // 添加调整大小样式
  document.body.style.userSelect = 'none'
}

/**
 * 调整大小中（已废弃，使用闭包版本）
 */
const handleResizeMove = (e: MouseEvent) => {
  // 此函数已被闭包版本替代
}

/**
 * 结束调整大小
 */
const handleResizeEnd = () => {
  isResizing.value = false
  resizeDirection.value = ''
  
  if (animationFrameId.value !== null) {
    cancelAnimationFrame(animationFrameId.value)
    animationFrameId.value = null
  }
  
  if (resizeMoveHandler.value) {
    document.removeEventListener('mousemove', resizeMoveHandler.value)
    resizeMoveHandler.value = null
  }
  document.removeEventListener('mouseup', handleResizeEnd)
  
  // 恢复样式
  document.body.style.userSelect = ''
}

/**
 * 加载用户的项目列表
 */
const loadProjects = async () => {
  projectsLoading.value = true
  try {
    const response = await getProjects({
      page: 1,
      size: 100,
      status: 'RECRUITING' // 只显示招募中的项目
    })
    projects.value = response.list || []
  } catch (error: any) {
    console.error('Failed to load projects:', error)
    // 加载项目失败不影响创建团队流程
  } finally {
    projectsLoading.value = false
  }
}

/**
 * 处理选择头像
 */
const handleSelectAvatar = () => {
  // Placeholder for avatar selection functionality
  ElMessage.info('头像选择功能待实现')
}

/**
 * 处理表单提交
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    // 验证表单
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    // 准备提交数据
    const submitData: TeamCreateRequest = {
      name: formData.value.name.trim(),
      description: formData.value.description?.trim() || undefined,
      avatar: formData.value.avatar?.trim() || undefined,
      projectId: formData.value.projectId || undefined
    }

    // 调用 store 创建团队
    const team = await teamStore.createTeam(submitData)

    ElMessage.success('团队创建成功')

    // 确保 team.id 存在
    if (!team || !team.id) {
      console.error('Team creation response missing id:', team)
      ElMessage.error('团队创建成功，但无法获取团队ID')
      dialogVisible.value = false
      return
    }

    // 触发成功事件
    emit('success', team.id)

    // 关闭对话框
    dialogVisible.value = false

    // 导航到新创建的团队空间
    await router.push({
      name: 'TeamOverview',
      params: { id: String(team.id) }
    })
  } catch (error: any) {
    console.error('Failed to create team:', error)
    ElMessage.error(error.message || '创建团队失败，请重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 处理取消
 */
const handleCancel = () => {
  dialogVisible.value = false
}

/**
 * 处理重置
 */
const handleReset = () => {
  formRef.value?.resetFields()
  formData.value = {
    name: '',
    description: '',
    avatar: '',
    projectId: undefined
  }
}

/**
 * 处理对话框关闭
 */
const handleClose = () => {
  // 重置表单
  handleReset()
  // 清除验证状态
  formRef.value?.clearValidate()
  // 重置对话框状态
  dialogPosition.value = { x: 0, y: 0 }
  dialogSize.value = { width: 600, height: 500 }
}

// Lifecycle
onUnmounted(() => {
  // 清理事件监听器
  if (dragMoveHandler.value) {
    document.removeEventListener('mousemove', dragMoveHandler.value)
  }
  if (resizeMoveHandler.value) {
    document.removeEventListener('mousemove', resizeMoveHandler.value)
  }
  document.removeEventListener('mouseup', handleDragEnd)
  document.removeEventListener('mouseup', handleResizeEnd)
  
  // 取消动画帧
  if (animationFrameId.value !== null) {
    cancelAnimationFrame(animationFrameId.value)
  }
  
  // 恢复样式
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
})

// Watch dialog visibility to load projects
watch(
  () => props.modelValue,
  (visible) => {
    if (visible && projects.value.length === 0) {
      loadProjects()
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.full-width {
  width: 100%;
}

.avatar-preview {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

// 对话框标题栏
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  user-select: none;
  border-bottom: 1px solid var(--el-border-color-lighter);
  
  .dialog-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

// 调整大小的边框
.resize-handle {
  position: absolute;
  z-index: 10;
  background: transparent; // 确保透明
  
  &.resize-n, &.resize-s {
    left: 0;
    right: 0;
    height: 6px;
    cursor: ns-resize;
  }
  
  &.resize-n {
    top: 0;
  }
  
  &.resize-s {
    bottom: 0;
  }
  
  &.resize-w, &.resize-e {
    top: 0;
    bottom: 0;
    width: 6px;
    cursor: ew-resize;
  }
  
  &.resize-w {
    left: 0;
  }
  
  &.resize-e {
    right: 0;
  }
  
  &.resize-nw, &.resize-ne, &.resize-sw, &.resize-se {
    width: 10px;
    height: 10px;
    z-index: 11; // 角落优先级更高
  }
  
  &.resize-nw {
    top: 0;
    left: 0;
    cursor: nwse-resize;
  }
  
  &.resize-ne {
    top: 0;
    right: 0;
    cursor: nesw-resize;
  }
  
  &.resize-sw {
    bottom: 0;
    left: 0;
    cursor: nesw-resize;
  }
  
  &.resize-se {
    bottom: 0;
    right: 0;
    cursor: nwse-resize;
  }
  
  // 悬停时显示提示
  &:hover {
    &.resize-n, &.resize-s {
      background: linear-gradient(to bottom, transparent 40%, rgba(64, 158, 255, 0.2) 50%, transparent 60%);
    }
    
    &.resize-w, &.resize-e {
      background: linear-gradient(to right, transparent 40%, rgba(64, 158, 255, 0.2) 50%, transparent 60%);
    }
  }
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-textarea__inner) {
  resize: vertical;
}

:deep(.draggable-resizable-dialog) {
  position: fixed !important;
  margin: 0 !important;
  transition: none !important; // 移除过渡动画以提高响应速度
  will-change: transform; // 优化渲染性能
  
  .el-dialog__header {
    padding: 0;
    margin: 0;
  }
  
  .el-dialog__body {
    padding: 20px;
    max-height: calc(100% - 120px);
    overflow-y: auto;
  }
  
  .el-dialog__footer {
    padding: 16px 20px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

// Screen reader only class
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
</style>
