<template>
  <div class="project-detail">
    <el-page-header @back="handleBack" title="返回">
      <template #content>
        <span class="page-title">项目详情</span>
      </template>
    </el-page-header>

    <el-card class="detail-card" v-if="project">
      <div class="project-header">
        <div>
          <h1>{{ project.title }}</h1>
          <div class="tags">
            <el-tag :type="getStatusType(project.status)">
              {{ getStatusText(project.status) }}
            </el-tag>
            <el-tag type="info">{{ getProjectTypeText(project.projectType) }}</el-tag>
            <el-tag v-if="project.isRecommended" type="danger">推荐</el-tag>
          </div>
        </div>

        <div style="display: flex; gap: 12px; align-items: center;">
          <el-button v-if="canEditProject" @click="openEditProject">编辑项目</el-button>
          <el-button
            v-if="project.status === 'RECRUITING'"
            type="primary"
            size="large"
            @click="handleApply"
          >
            申请加入
          </el-button>
        </div>
      </div>

      <el-divider />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="创建者">
          <span>创建者ID: {{ project.creatorId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="团队规模">
          {{ project.currentMembers }} / {{ project.teamSize }} 人
        </el-descriptions-item>
        <el-descriptions-item label="每周投入">
          {{ project.weeklyHours }} 小时
        </el-descriptions-item>
        <el-descriptions-item label="预期周期">
          {{ project.expectedDuration }} 天
        </el-descriptions-item>
        <el-descriptions-item label="浏览次数">
          {{ project.views }} 次
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDate(project.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div class="project-progress">
        <div class="progress-header">
          <h3>项目进度</h3>
          <span class="duration">项目周期：{{ projectDurationText }}</span>
        </div>
        <el-progress
          :percentage="projectProgress"
          :status="progressStatus as any"
          :stroke-width="18"
          text-inside
        />
      </div>

      <el-divider />

      <!-- 项目里程碑 -->
      <div class="project-milestones">
        <div class="milestone-header">
          <div class="title-area">
            <h3>项目里程碑</h3>
            <span class="hint">按时间线查看关键节点，维护计划与实际完成时间</span>
          </div>
          <el-button
            v-if="isProjectOwner"
            type="primary"
            size="small"
            :icon="Plus"
            @click="openCreateMilestone"
          >
            新增里程碑
          </el-button>
        </div>

        <el-skeleton v-if="milestoneLoading" :rows="3" animated />

        <template v-else>
          <div v-if="milestones.length === 0" class="empty-milestone">
            <el-empty description="还没有里程碑，先添加一个计划节点吧" />
          </div>
          <el-timeline v-else>
            <el-timeline-item
              v-for="item in milestones"
              :key="item.id"
              :timestamp="item.plannedAt ? formatDate(item.plannedAt) : '未设置计划时间'"
              :type="milestoneStatusTag(item.status).type"
            >
              <div class="milestone-item">
                <div class="milestone-main">
                  <div class="title-row">
                    <span class="title">{{ item.title }}</span>
                    <el-tag size="small" :type="milestoneStatusTag(item.status).type">
                      {{ milestoneStatusTag(item.status).text }}
                    </el-tag>
                  </div>
                  <div class="meta">
                    <span v-if="item.ownerName">负责人：{{ item.ownerName }}</span>
                    <span v-if="item.actualAt">实际：{{ formatDate(item.actualAt) }}</span>
                    <span v-if="item.remark">备注：{{ item.remark }}</span>
                  </div>
                </div>
                <div class="milestone-actions" v-if="isProjectOwner">
                  <el-button link size="small" :icon="Edit" @click="openEditMilestone(item)">编辑</el-button>
                  <el-button link size="small" type="danger" :icon="Delete" @click="handleDeleteMilestone(item)">删除</el-button>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>

      <el-divider />
      <div class="project-content">
        <h3>项目描述</h3>
        <p>{{ project.description || '暂无描述' }}</p>

        <h3>需求描述</h3>
        <div v-html="project.requirements || '暂无需求描述'"></div>

        <h3>期望成果</h3>
        <p>{{ project.expectedOutcome || '暂无期望成果' }}</p>
      </div>

      <el-divider />


      <div class="project-members">
        <div class="members-header">
          <h3>项目成员</h3>
          <span class="count">
            当前成员：{{ members.length }} / {{ project.teamSize || '-' }}
          </span>
        </div>

        <el-skeleton v-if="membersLoading" :rows="3" animated />

        <template v-else>
          <div v-if="members.length === 0" class="empty-members">
            <el-empty description="暂无成员，快去招募小伙伴吧～" />
          </div>
          <div v-else class="members-grid">
            <div
              v-for="member in members"
              :key="member.id"
              class="member-card"
            >
              <el-avatar
                :size="56"
                :src="member.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
              />
              <div class="member-info">
                <div class="name-row">
                  <span class="name">
                    {{ member.nickname || member.username || '未命名成员' }}
                  </span>
                  <el-tag
                    size="small"
                    :type="getMemberTagType(member)"
                  >
                    {{ getMemberRoleText(member) }}
                  </el-tag>
                </div>
                <div class="meta">
                  加入时间：
                  <span>{{ member.joinedAt ? formatDate(member.joinedAt) : '未知' }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <el-divider />

      <div class="project-discussion">
        <div class="discussion-header">
          <h3>项目讨论区</h3>
          <span class="hint">欢迎就项目需求、进度、协作方式进行讨论</span>
        </div>

        <!-- 输入区域 -->
        <div class="discussion-input">
          <RichTextEditor
            v-model="newComment"
            placeholder="发一条评论，和项目成员交流想法..."
            :min-height="'120px'"
            :max-length="1000"
            :show-toolbar="true"
          />
          <div class="input-actions">
            <span class="tip">支持富文本格式，@用户名 进行提醒</span>
            <el-button
              type="primary"
              :loading="commentSubmitting"
              :disabled="!newComment || !newComment.trim() || newComment === '<p></p>'"
              @click="handleSubmitComment"
            >
              发表评论
            </el-button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div class="discussion-list">
          <el-skeleton v-if="commentsLoading" :rows="4" animated />

          <template v-else>
            <div v-if="comments.length === 0" class="empty-comments">
              <el-empty description="还没有评论，快来抢沙发吧～" />
            </div>
            <div v-else>
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment-item"
              >
                <el-avatar
                  :size="40"
                  :src="comment.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                />
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="author">
                      {{ comment.nickname || comment.username || '匿名用户' }}
                    </span>
                    <el-tag v-if="comment.userId === project.creatorId" size="small" type="success">
                      创建者
                    </el-tag>
                    <span class="time">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                  <div class="comment-text" v-html="comment.content"></div>
                  <div class="comment-actions">
                    <el-button
                      link
                      size="small"
                      @click="startReply(comment)"
                    >
                      回复
                    </el-button>
                  </div>

                  <!-- 子回复 -->
                  <div
                    v-if="comment.replies && comment.replies.length"
                    class="replies"
                  >
                    <div
                      v-for="reply in comment.replies"
                      :key="reply.id"
                      class="reply-item"
                    >
                      <el-avatar
                        :size="32"
                        :src="reply.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                      />
                      <div class="reply-content">
                        <div class="comment-header">
                          <span class="author">
                            {{ reply.nickname || reply.username || '匿名用户' }}
                          </span>
                          <span v-if="reply.replyToUsername" class="reply-to">
                            回复 {{ reply.replyToNickname || reply.replyToUsername }}
                          </span>
                          <span class="time">{{ formatDate(reply.createdAt) }}</span>
                        </div>
                        <div class="comment-text" v-html="reply.content"></div>
                        <div class="comment-actions">
                          <el-button
                            link
                            size="small"
                            @click="startReply(comment, reply)"
                          >
                            回复
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 回复输入框 -->
                  <div v-if="replyingTo && replyingTo.id === comment.id" class="reply-input">
                    <RichTextEditor
                      v-model="replyContent"
                      :placeholder="replyPlaceholder"
                      :min-height="'100px'"
                      :max-length="500"
                      :show-toolbar="true"
                    />
                    <div class="input-actions">
                      <el-button size="small" @click="cancelReply">取消</el-button>
                      <el-button
                        type="primary"
                        size="small"
                        :loading="commentSubmitting"
                        :disabled="!replyContent || !replyContent.trim() || replyContent === '<p></p>'"
                        @click="handleSubmitReply(comment)"
                      >
                        发送回复
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div class="pagination">
                <el-pagination
                  layout="prev, pager, next"
                  :page-size="commentPageSize"
                  :total="commentTotal"
                  :current-page="commentPage"
                  @current-change="handleCommentPageChange"
                />
              </div>
            </div>
          </template>
        </div>
      </div>

      <el-divider />

      <!-- 项目文件管理 -->
      <div class="project-files">
        <div class="files-header">
          <h3>项目文件</h3>
          <div class="header-actions">
            <el-select
              v-model="fileCategoryFilter"
              placeholder="筛选分类"
              clearable
              size="small"
              style="width: 150px; margin-right: 12px;"
              @change="handleFileCategoryChange"
            >
              <el-option label="全部" value="" />
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="代码" value="CODE" />
              <el-option label="设计" value="DESIGN" />
              <el-option label="其他" value="OTHER" />
            </el-select>
            <el-upload
              :action="''"
              :auto-upload="false"
              :show-file-list="false"
              :before-upload="handleFileUpload"
              multiple
            >
              <el-button type="primary" size="small">
                <el-icon><Upload /></el-icon>
                上传文件
              </el-button>
            </el-upload>
          </div>
        </div>

        <el-skeleton v-if="filesLoading" :rows="4" animated />

        <template v-else>
          <div v-if="projectFiles.length === 0" class="empty-files">
            <el-empty description="还没有文件，快来上传第一个文件吧～" />
          </div>
          <el-table v-else :data="projectFiles" style="width: 100%">
            <el-table-column label="文件名" min-width="300">
              <template #default="{ row }">
                <div class="file-name-cell">
                  <el-icon class="file-icon" :class="getFileTypeClass(row.fileType)">
                    <Document v-if="row.fileType === 'DOCUMENT'" />
                    <Picture v-else-if="row.fileType === 'IMAGE'" />
                    <Document v-else-if="row.fileType === 'CODE'" />
                    <Document v-else />
                  </el-icon>
                  <span>{{ row.fileName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ getCategoryText(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column label="上传者" width="150">
              <template #default="{ row }">
                <div class="uploader-info">
                  <el-avatar :size="24" :src="row.uploaderAvatar" />
                  <span>{{ row.uploaderName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" text @click="downloadFile(row)">下载</el-button>
                <el-button
                  v-if="canDeleteFile(row)"
                  size="small"
                  text
                  type="danger"
                  @click="handleDeleteFile(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 文件分页 -->
          <div class="file-pagination">
            <el-pagination
              layout="prev, pager, next"
              :page-size="filePageSize"
              :total="fileTotal"
              :current-page="filePage"
              @current-change="handleFilePageChange"
            />
          </div>
        </template>
      </div>
    </el-card>

    <!-- 里程碑弹窗 -->
    <el-dialog
      v-model="milestoneDialogVisible"
      :title="editingMilestoneId ? '编辑里程碑' : '新增里程碑'"
      width="520px"
    >
      <el-form label-position="top" :model="milestoneForm">
        <el-form-item label="标题" required>
          <el-input v-model="milestoneForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-select v-model="milestoneForm.status" placeholder="请选择状态">
            <el-option label="计划" value="PLANNED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker
            v-model="milestoneForm.plannedAt"
            type="datetime"
            placeholder="选择计划时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="实际完成时间">
          <el-date-picker
            v-model="milestoneForm.actualAt"
            type="datetime"
            placeholder="选择实际时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="milestoneForm.ownerId" placeholder="选择负责人" filterable clearable>
            <el-option
              v-for="member in members"
              :key="member.userId || member.id"
              :label="member.nickname || member.username || '成员'"
              :value="member.userId || member.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="milestoneForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="milestoneForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="补充说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="milestoneDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMilestone">保存</el-button>
      </template>
    </el-dialog>

    <el-skeleton v-if="!project" :rows="10" animated />
    <EditProjectDialog
      v-model="editProjectDialogVisible"
      :project="project"
      @saved="handleProjectUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Document, Picture, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { 
  getProjectDetail, 
  applyProject, 
  getProjectMembers, 
  getProjectComments, 
  addProjectComment,
  getProjectFiles,
  uploadProjectFile,
  deleteProjectFile,
  getProjectMilestones,
  createProjectMilestone,
  updateProjectMilestone,
  deleteProjectMilestone,
  updateProject
} from '../../api/project'
import { useAuthStore } from '../../store/auth'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import EditProjectDialog from '@/components/project/EditProjectDialog.vue'
import type { Project, ProjectComment, ProjectFile, ProjectMilestone } from '../../types/project'
import type { TeamMember } from '../../types/team'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const project = ref<Project | null>(null)
const members = ref<TeamMember[]>([])
const membersLoading = ref(false)

const editProjectDialogVisible = ref(false)
const canEditProject = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false

  // 项目创建者一定可编辑
  if (project.value.creatorId === userId) return true

  // 任何团队成员可编辑
  return members.value.some((m: any) => (m.userId ?? m.id) === userId)
})

// 里程碑
const milestones = ref<ProjectMilestone[]>([])
const milestoneLoading = ref(false)
const milestoneDialogVisible = ref(false)
const milestoneForm = ref<Partial<ProjectMilestone>>({
  title: '',
  status: 'PLANNED',
  plannedAt: '',
  actualAt: '',
  ownerId: undefined,
  remark: '',
  sortOrder: 0,
})
const editingMilestoneId = ref<number | null>(null)

// 文件管理相关状态
const projectFiles = ref<ProjectFile[]>([])
const filesLoading = ref(false)
const filePage = ref(1)
const filePageSize = ref(10)
const fileTotal = ref(0)
const fileCategoryFilter = ref('')
const comments = ref<ProjectComment[]>([])
const commentsLoading = ref(false)
const newComment = ref('')
const replyContent = ref('')
const replyingTo = ref<ProjectComment | null>(null)
const replyTarget = ref<ProjectComment | null>(null)
const commentSubmitting = ref(false)
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)

const projectProgress = computed(() => {
  if (!project.value) return 0

  // 已完成直接 100%
  if (project.value.status === 'COMPLETED') return 100

  // 根据开始/结束日期计算进度
  if (project.value.startDate && project.value.endDate) {
    const start = new Date(project.value.startDate).getTime()
    const end = new Date(project.value.endDate).getTime()
    const now = Date.now()

    if (end <= start) return 0
    if (now <= start) return 0
    if (now >= end) return 95 // 未标记完成但已到结束时间，给个高进度

    const progress = ((now - start) / (end - start)) * 100
    return Math.min(95, Math.max(5, Math.round(progress)))
  }

  // 无时间信息时，根据状态给一个大致进度
  const statusProgressMap: Record<string, number> = {
    DRAFT: 5,
    RECRUITING: 20,
    IN_PROGRESS: 60,
    PENDING_REVIEW: 80,
    COMPLETED: 100,
    ARCHIVED: 100,
  }

  return statusProgressMap[project.value.status] ?? 0
})

const projectDurationText = computed(() => {
  if (!project.value) return '未设置'
  if (project.value.startDate && project.value.endDate) {
    return `${project.value.startDate} ~ ${project.value.endDate}`
  }
  if (project.value.expectedDuration) {
    return `约 ${project.value.expectedDuration} 天`
  }
  return '未设置'
})

const progressStatus = computed(() => {
  if (!project.value) return 'active'
  if (project.value.status === 'COMPLETED') return 'success'
  if (project.value.status === 'DRAFT') return 'exception'
  return 'active'
})

const replyPlaceholder = computed(() => {
  if (!replyTarget.value) return '回复评论'
  const name = replyTarget.value.nickname || replyTarget.value.username || '该用户'
  return `回复 @${name}：`
})

const isCreator = (member: TeamMember) => {
  if (!project.value) return false
  return member.userId === project.value.creatorId
}

const getMemberRoleText = (member: TeamMember) => {
  if (isCreator(member)) return '创建者'
  if (member.role === 'LEADER') return '队长'
  return '成员'
}

const getMemberTagType = (member: TeamMember) => {
  if (isCreator(member)) return 'success'
  if (member.role === 'LEADER') return 'danger'
  return 'info'
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    DRAFT: 'info',
    RECRUITING: 'warning',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    DRAFT: '草稿',
    RECRUITING: '招募中',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
  }
  return texts[status] || status
}

const milestoneStatusTag = (status: string) => {
  const map: Record<string, { type: any; text: string }> = {
    PLANNED: { type: 'info', text: '计划' },
    IN_PROGRESS: { type: 'warning', text: '进行中' },
    DONE: { type: 'success', text: '已完成' },
  }
  return map[status] || { type: 'info', text: status }
}

const getProjectTypeText = (type: string) => {
  const texts: Record<string, string> = {
    COMPETITION: '竞赛',
    RESEARCH: '科研',
    STARTUP: '创业',
    OPENSOURCE: '开源',
    OTHER: '其他',
  }
  return texts[type] || type
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const isProjectOwner = computed(() => {
  if (!project.value) return false
  return project.value.creatorId === authStore.user?.id
})

const handleBack = () => {
  router.back()
}

const handleProjectUpdated = async () => {
  try {
    const projectId = ensureProjectId()
    await loadProjectDetail(projectId)
    ElMessage.success('项目已更新')
  } catch (e) {
    // ignore
  }
}

const openEditProject = () => {
  editProjectDialogVisible.value = true
}

const handleApply = async () => {
  if (!project.value) return

  try {
    await applyProject(project.value.id, '希望加入该项目')
    ElMessage.success('申请成功，等待审核')
  } catch (error) {
    console.error(error)
  }
}

const loadProjectDetail = async () => {
  const projectId = Number(route.params.id)
  if (!projectId) return

  try {
    const res = await getProjectDetail(projectId)
    project.value = res as unknown as Project

    // 加载项目成员
    membersLoading.value = true
    try {
      const memberList = await getProjectMembers(projectId)
      members.value = memberList || []
    } catch (e) {
      console.error(e)
    } finally {
      membersLoading.value = false
    }

    // 加载评论
    await loadComments(projectId)
    
    // 加载文件列表
    await loadProjectFiles(projectId)

    // 加载里程碑
    await loadMilestones(projectId)
  } catch (error) {
    console.error(error)
    ElMessage.error('加载项目详情失败')
  }
}

const loadMilestones = async (projectId: number) => {
  milestoneLoading.value = true
  try {
    const res: any = await getProjectMilestones(projectId)
    milestones.value = res.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('加载里程碑失败')
  } finally {
    milestoneLoading.value = false
  }
}

const loadComments = async (projectId: number, page = commentPage.value, size = commentPageSize.value) => {
  commentsLoading.value = true
  try {
    const pageData: any = await getProjectComments(projectId, page, size)
    comments.value = pageData?.records || []
    commentTotal.value = pageData?.total ?? 0
  } catch (error) {
    console.error(error)
    ElMessage.error('加载评论失败')
  } finally {
    commentsLoading.value = false
  }
}

const ensureProjectId = () => {
  const id = Number(route.params.id)
  if (!id || Number.isNaN(id)) {
    throw new Error('无效的项目ID')
  }
  return id
}

const handleSubmitComment = async () => {
  // 检查是否为空内容（可能是空的HTML标签）
  const textContent = newComment.value.replace(/<[^>]*>/g, '').trim()
  if (!textContent) return
  
  const projectId = ensureProjectId()

  commentSubmitting.value = true
  try {
    await addProjectComment({
      projectId,
      content: newComment.value, // 使用HTML内容
    })
    newComment.value = ''
    // 重新加载当前页，评论会出现在第一页顶部
    commentPage.value = 1
    await loadComments(projectId, 1, commentPageSize.value)
    ElMessage.success('评论已发布')
  } catch (error) {
    console.error(error)
    ElMessage.error('发布评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

const openCreateMilestone = () => {
  if (!isProjectOwner.value) {
    ElMessage.warning('仅项目创建者可维护里程碑')
    return
  }
  editingMilestoneId.value = null
  milestoneForm.value = {
    title: '',
    status: 'PLANNED',
    plannedAt: '',
    actualAt: '',
    ownerId: undefined,
    remark: '',
    sortOrder: 0,
  }
  milestoneDialogVisible.value = true
}

const openEditMilestone = (item: ProjectMilestone) => {
  if (!isProjectOwner.value) {
    ElMessage.warning('仅项目创建者可维护里程碑')
    return
  }
  editingMilestoneId.value = item.id
  milestoneForm.value = {
    ...item,
    plannedAt: item.plannedAt || '',
    actualAt: item.actualAt || '',
  }
  milestoneDialogVisible.value = true
}

const handleSaveMilestone = async () => {
  if (!project.value) return
  const { title, status } = milestoneForm.value
  if (!title || !title.trim()) {
    ElMessage.warning('请输入里程碑标题')
    return
  }
  if (!status) {
    ElMessage.warning('请选择状态')
    return
  }

  const payload = {
    ...milestoneForm.value,
  }

  try {
    if (editingMilestoneId.value) {
      await updateProjectMilestone(editingMilestoneId.value, payload)
      ElMessage.success('里程碑已更新')
    } else {
      await createProjectMilestone(project.value.id, payload)
      ElMessage.success('里程碑已创建')
    }
    milestoneDialogVisible.value = false
    await loadMilestones(project.value.id)
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

const handleDeleteMilestone = async (item: ProjectMilestone) => {
  if (!project.value) return
  try {
    await ElMessageBox.confirm('确定要删除该里程碑吗？', '确认删除', { type: 'warning' })
    await deleteProjectMilestone(item.id)
    ElMessage.success('已删除')
    await loadMilestones(project.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  }
}

const startReply = (comment: ProjectComment, target?: ProjectComment) => {
  replyingTo.value = comment
  replyTarget.value = target || comment
  // 使用富文本格式的@提醒
  const mentionText = target ? `@${target.nickname || target.username || ''} ` : ''
  replyContent.value = mentionText ? `<p>${mentionText}</p>` : ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyTarget.value = null
  replyContent.value = ''
}

const handleSubmitReply = async (comment: ProjectComment) => {
  // 检查是否为空内容（可能是空的HTML标签）
  const textContent = replyContent.value.replace(/<[^>]*>/g, '').trim()
  if (!textContent) return
  
  const projectId = ensureProjectId()
  const target = replyTarget.value || comment

  commentSubmitting.value = true
  try {
    await addProjectComment({
      projectId,
      content: replyContent.value, // 使用HTML内容
      parentId: comment.id,
      replyToUserId: target.userId,
    })
    replyContent.value = ''
    replyingTo.value = null
    replyTarget.value = null
    // 重新加载当前页
    await loadComments(projectId, commentPage.value, commentPageSize.value)
    ElMessage.success('回复已发送')
  } catch (error) {
    console.error(error)
    ElMessage.error('发送回复失败')
  } finally {
    commentSubmitting.value = false
  }
}

const handleCommentPageChange = async (page: number) => {
  commentPage.value = page
  const projectId = ensureProjectId()
  await loadComments(projectId, page, commentPageSize.value)
}

// 文件管理相关函数
const loadProjectFiles = async (projectId: number, page = filePage.value, size = filePageSize.value) => {
  filesLoading.value = true
  try {
    const pageData: any = await getProjectFiles(projectId, page, size)
    projectFiles.value = pageData?.records || []
    fileTotal.value = pageData?.total ?? 0
  } catch (error) {
    console.error(error)
    ElMessage.error('加载文件列表失败')
  } finally {
    filesLoading.value = false
  }
}

const handleFileCategoryChange = () => {
  filePage.value = 1
  loadProjectFiles(ensureProjectId(), 1)
}

const handleFilePageChange = (page: number) => {
  filePage.value = page
  loadProjectFiles(ensureProjectId(), page)
}

const handleFileUpload = async (file: File) => {
  const projectId = ensureProjectId()
  
  // 验证文件大小（50MB）
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }

  try {
    await uploadProjectFile(projectId, file, fileCategoryFilter.value || undefined)
    ElMessage.success('文件上传成功')
    // 重新加载第一页
    filePage.value = 1
    await loadProjectFiles(projectId, 1)
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '文件上传失败')
  }

  return false // 阻止默认上传
}

const downloadFile = (file: ProjectFile) => {
  const link = document.createElement('a')
  // 后端现在返回 filePath（对齐当前数据库结构）
  link.href = (file as any).fileUrl || (file as any).filePath
  link.download = file.fileName
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const canDeleteFile = (file: ProjectFile) => {
  if (!project.value) return false
  return file.uploaderId === authStore.user?.id || project.value.creatorId === authStore.user?.id
}

const handleDeleteFile = async (file: ProjectFile) => {
  try {
    await ElMessageBox.confirm('确定要删除这个文件吗？', '确认删除', {
      type: 'warning'
    })
    
    await deleteProjectFile(file.id)
    ElMessage.success('删除成功')
    await loadProjectFiles(ensureProjectId(), filePage.value)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('删除失败')
    }
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const getFileTypeClass = (fileType: string) => {
  const classMap: Record<string, string> = {
    DOCUMENT: 'file-document',
    IMAGE: 'file-image',
    CODE: 'file-code',
    OTHER: 'file-other'
  }
  return classMap[fileType] || 'file-other'
}

const getCategoryText = (category: string) => {
  const textMap: Record<string, string> = {
    DOCUMENT: '文档',
    CODE: '代码',
    DESIGN: '设计',
    OTHER: '其他'
  }
  return textMap[category] || category
}

onMounted(() => {
  loadProjectDetail()
})
</script>

<style scoped lang="scss">
.project-detail {
  padding: 20px;

  .page-title {
    font-size: 18px;
    font-weight: 500;
  }

  .detail-card {
    margin-top: 20px;

    .project-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      h1 {
        margin: 0 0 12px 0;
        font-size: 28px;
        font-weight: 600;
      }

      .tags {
        display: flex;
        gap: 8px;
      }
    }

    .project-content {
      margin-top: 20px;

      h3 {
        margin: 20px 0 12px 0;
        font-size: 16px;
        font-weight: 500;
      }

      p {
        color: #606266;
        line-height: 1.8;
      }
    }

    .project-progress {
      margin-top: 24px;

      .progress-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
        }

        .duration {
          font-size: 13px;
          color: #909399;
        }
      }
    }

    .project-milestones {
      margin-top: 24px;

      .milestone-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .title-area {
          h3 {
            margin: 0;
            font-size: 16px;
            font-weight: 500;
          }

          .hint {
            display: block;
            margin-top: 4px;
            font-size: 12px;
            color: #909399;
          }
        }
      }

      .milestone-item {
        display: flex;
        justify-content: space-between;
        gap: 12px;

        .title-row {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 4px;

          .title {
            font-weight: 600;
          }
        }

        .meta {
          display: flex;
          flex-wrap: wrap;
          gap: 12px;
          color: #606266;
          font-size: 13px;
        }

        .milestone-actions {
          display: flex;
          gap: 4px;
        }
      }

      .empty-milestone {
        padding: 12px 0;
      }
    }

    .project-members {
      margin-top: 24px;

      .members-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
        }

        .count {
          font-size: 13px;
          color: #909399;
        }
      }

      .members-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
        gap: 16px;
      }

      .member-card {
        display: flex;
        gap: 12px;
        padding: 12px;
        border-radius: 10px;
        background: var(--el-fill-color-lighter);
        transition: transform 0.15s ease, box-shadow 0.15s ease;

        &:hover {
          transform: translateY(-1px);
          box-shadow: var(--el-box-shadow-light);
        }
      }

      .member-info {
        flex: 1;
        min-width: 0;

        .name-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 8px;
          margin-bottom: 4px;
        }

        .name {
          font-weight: 500;
          font-size: 14px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .meta {
          font-size: 12px;
          color: #909399;
        }
      }

      .empty-members {
        margin-top: 8px;
      }
    }

    .project-discussion {
      margin-top: 24px;

      .discussion-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
        }

        .hint {
          font-size: 13px;
          color: #909399;
        }
      }

      .discussion-input {
        padding: 12px;
        border-radius: 10px;
        background: var(--el-fill-color-lighter);
        margin-bottom: 16px;

        .input-actions {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 8px;

          .tip {
            font-size: 12px;
            color: #909399;
          }
        }
      }

      .discussion-list {
        .comment-item {
          display: flex;
          gap: 12px;
          padding: 12px 0;
          border-bottom: 1px solid var(--el-border-color-lighter);
        }

        .comment-content {
          flex: 1;
          min-width: 0;
        }

        .comment-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 4px;

          .author {
            font-weight: 500;
            font-size: 14px;
          }

          .time {
            margin-left: auto;
            font-size: 12px;
            color: #909399;
          }

          .reply-to {
            font-size: 12px;
            color: #909399;
          }
        }

        .comment-text {
          font-size: 13px;
          color: #606266;
          line-height: 1.6;
          margin: 2px 0 4px;
          
          // 富文本样式
          :deep(p) {
            margin: 0.5em 0;
          }
          
          :deep(h1), :deep(h2), :deep(h3) {
            margin: 0.8em 0 0.4em;
            font-weight: 600;
          }
          
          :deep(ul), :deep(ol) {
            padding-left: 1.5em;
            margin: 0.5em 0;
          }
          
          :deep(code) {
            background: var(--el-fill-color-lighter);
            padding: 2px 4px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
          }
          
          :deep(pre) {
            background: var(--el-fill-color-lighter);
            padding: 8px;
            border-radius: 4px;
            overflow-x: auto;
            margin: 0.5em 0;
          }
          
          :deep(img) {
            max-width: 100%;
            height: auto;
            border-radius: 4px;
          }
          
          :deep(a) {
            color: var(--el-color-primary);
            text-decoration: underline;
          }
        }

        .comment-actions {
          font-size: 12px;
        }

        .replies {
          margin-top: 8px;
          border-left: 2px solid var(--el-border-color-lighter);
          padding-left: 10px;

          .reply-item {
            display: flex;
            gap: 10px;
            padding: 6px 0;
          }

          .reply-content {
            flex: 1;
          }
        }

        .reply-input {
          margin-top: 8px;

          .input-actions {
            margin-top: 6px;
            display: flex;
            justify-content: flex-end;
            gap: 8px;
          }
        }

        .pagination {
          margin-top: 12px;
          text-align: right;
        }

        .empty-comments {
          margin-top: 8px;
        }
      }
    }

    .project-files {
      margin-top: 24px;

      .files-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
        }

        .header-actions {
          display: flex;
          align-items: center;
        }
      }

      .empty-files {
        padding: 40px 0;
      }

      .file-name-cell {
        display: flex;
        align-items: center;
        gap: 8px;

        .file-icon {
          font-size: 18px;
          color: #409eff;

          &.file-document {
            color: #409eff;
          }

          &.file-image {
            color: #67c23a;
          }

          &.file-code {
            color: #e6a23c;
          }

          &.file-other {
            color: #909399;
          }
        }
      }

      .uploader-info {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .file-pagination {
        margin-top: 16px;
        display: flex;
        justify-content: center;
      }
    }
  }
}
</style>

