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
            v-if="canStartProject" 
            type="primary" 
            @click="handleStartProject"
          >
            开始项目
          </el-button>
          <el-button 
            v-if="canCompleteProject" 
            type="success" 
            @click="openCompleteDialog"
          >
            完成项目
          </el-button>
          <el-button 
            v-if="canArchiveProject" 
            type="warning" 
            @click="handleArchiveProject"
          >
            归档项目
          </el-button>
          <el-button
            v-if="canApplyProject"
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
          <span>{{ project.creatorName || `用户 ${project.creatorId}` }}</span>
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
          <el-tooltip placement="top" effect="light">
            <template #content>
              <div style="max-width: 300px;">
                <p style="margin: 0 0 8px 0; font-weight: 600;">进度计算方式：</p>
                <p style="margin: 4px 0;">• 里程碑完成度（40%权重，需≥3个）</p>
                <p style="margin: 4px 0;">• 团队活跃度（30%权重）</p>
                <p style="margin: 4px 0;">• 时间进度（30%权重）</p>
                <p style="margin: 8px 0 0 0; font-size: 12px; color: #909399;">
                  综合多个维度评估，避免单一指标造假
                </p>
              </div>
            </template>
            <el-icon style="cursor: help; margin-left: 8px; color: var(--el-text-color-secondary);">
              <QuestionFilled />
            </el-icon>
          </el-tooltip>
        </div>
        <el-progress
          :percentage="projectProgress"
          :status="progressStatus as any"
          :stroke-width="18"
          text-inside
        />
        
        <!-- 里程碑详情 -->
        <div v-if="milestoneProgressDetail" class="progress-detail">
          <div class="detail-section">
            <div class="section-title">里程碑进度</div>
            <div class="detail-items">
              <div class="detail-item">
                <span class="label">已完成：</span>
                <span class="value success">{{ milestoneProgressDetail.done }} 个 ({{ milestoneProgressDetail.donePercent }}%)</span>
              </div>
              <div class="detail-item">
                <span class="label">进行中：</span>
                <span class="value warning">{{ milestoneProgressDetail.inProgress }} 个 ({{ milestoneProgressDetail.inProgressPercent }}%)</span>
              </div>
              <div class="detail-item">
                <span class="label">计划中：</span>
                <span class="value info">{{ milestoneProgressDetail.planned }} 个</span>
              </div>
            </div>
            <div v-if="milestoneProgressDetail.warning" class="detail-warning">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ milestoneProgressDetail.warning }}</span>
            </div>
          </div>
          
          <div class="detail-section">
            <div class="section-title">团队活跃度</div>
            <div class="detail-items">
              <div class="detail-item">
                <span class="label">成员数：</span>
                <span class="value">{{ members.length }} / {{ project.teamSize || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">讨论数：</span>
                <span class="value">{{ comments.length + pinnedComments.length }} 条</span>
              </div>
              <div class="detail-item">
                <span class="label">运行天数：</span>
                <span class="value">{{ Math.floor((Date.now() - new Date(project.createdAt).getTime()) / (1000 * 60 * 60 * 24)) }} 天</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="progress-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>进度由多个客观指标综合计算，包括里程碑完成度、团队活跃度、时间进度等，确保真实可信</span>
        </div>
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
                  <el-button 
                    v-if="item.status !== 'DONE'" 
                    link 
                    size="small" 
                    type="success"
                    @click="handleCompleteMilestone(item)"
                  >
                    完成
                  </el-button>
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
        <div v-html="project.description || '暂无描述'"></div>

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
          <span class="hint">{{ comments.length }} 条评论</span>
        </div>

        <!-- 评论列表 -->
        <div class="discussion-list">
          <el-skeleton v-if="commentsLoading" :rows="4" animated />

          <template v-else>
            <div v-if="comments.length === 0 && pinnedComments.length === 0" class="empty-comments">
              <el-empty description="还没有评论，快来发表第一条评论吧～" />
            </div>
            <div v-else>
              <!-- 置顶评论区 -->
              <div v-if="pinnedComments.length > 0" class="pinned-comments-section">
                <div class="section-header">
                  <el-icon><Star /></el-icon>
                  <span>置顶评论</span>
                </div>
                <div
                  v-for="comment in pinnedComments"
                  :key="comment.id"
                  class="comment-item pinned-comment"
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
                      <el-tag size="small" type="warning">置顶</el-tag>
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
                      <el-button
                        v-if="canPinComment"
                        link
                        size="small"
                        type="warning"
                        @click="handleUnpinComment(comment)"
                      >
                        取消置顶
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 普通评论区 -->
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
                    <el-button
                      v-if="canPinComment"
                      link
                      size="small"
                      type="primary"
                      @click="handlePinComment(comment)"
                    >
                      置顶
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
                    <div class="reply-input-header">
                      <span>{{ replyPlaceholder }}</span>
                      <el-button link size="small" @click="cancelReply">取消</el-button>
                    </div>
                    <RichTextEditor
                      v-model="replyContent"
                      :placeholder="replyPlaceholder"
                      :min-height="'100px'"
                      :max-length="500"
                      :show-toolbar="true"
                    />
                    <div class="input-actions">
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

        <!-- 输入区域 -->
        <div class="discussion-input">
          <div class="input-header">
            <span class="input-title">发表评论</span>
            <span class="input-hint">分享你的想法和建议</span>
          </div>
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
    <CompleteProjectDialog
      v-if="project"
      v-model="completeDialogVisible"
      :project-id="project.id"
      :project-title="project.title"
      @success="handleProjectCompleted"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Star, InfoFilled, WarningFilled, QuestionFilled } from '@element-plus/icons-vue'
import { 
  getProjectDetail, 
  applyProject, 
  getProjectMembers, 
  getProjectComments, 
  addProjectComment,
  getProjectMilestones,
  createProjectMilestone,
  updateProjectMilestone,
  deleteProjectMilestone,
  updateProject
} from '../../api/project'
import { useAuthStore } from '../../store/auth'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import EditProjectDialog from '@/components/project/EditProjectDialog.vue'
import CompleteProjectDialog from '@/components/project/CompleteProjectDialog.vue'
import type { Project, ProjectComment, ProjectMilestone } from '../../types/project'
import type { TeamMember } from '../../types/team'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const project = ref<Project | null>(null)
const members = ref<TeamMember[]>([])
const membersLoading = ref(false)

const editProjectDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const canEditProject = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false

  // 项目创建者一定可编辑
  if (project.value.creatorId === userId) return true

  // 任何团队成员可编辑
  return members.value.some((m: any) => (m.userId ?? m.id) === userId)
})

const canCompleteProject = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false
  
  // 只有项目创建者可以完成项目
  if (project.value.creatorId !== userId) return false
  
  // 只有进行中的项目可以完成
  return project.value.status === 'IN_PROGRESS' || project.value.status === 'RECRUITING'
})

const canStartProject = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false
  
  // 只有项目创建者可以开始项目
  if (project.value.creatorId !== userId) return false
  
  // 只有招募中的项目可以开始
  return project.value.status === 'RECRUITING'
})

const canArchiveProject = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false
  
  // 只有项目创建者可以归档项目
  if (project.value.creatorId !== userId) return false
  
  // 只有已完成的项目可以归档
  return project.value.status === 'COMPLETED'
})

const canApplyProject = computed(() => {
  const user = authStore.user
  if (!user || !project.value) return false
  
  // 只有招募中的项目可以申请
  if (project.value.status !== 'RECRUITING') return false
  
  // 项目创建者不能申请自己的项目
  if (project.value.creatorId === user.id) return false
  
  // 导师和管理员不能申请项目
  const userRoles = user.roles || []
  if (userRoles.includes('MENTOR') || userRoles.includes('ADMIN')) return false
  
  // 已经是项目成员的不能再申请
  const isMember = members.value.some((m: any) => (m.userId ?? m.id) === user.id)
  if (isMember) return false
  
  return true
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

// 评论相关状态
const comments = ref<ProjectComment[]>([])
const pinnedComments = ref<ProjectComment[]>([])
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

  // 多维度综合评估项目进度
  let totalProgress = 0
  let totalWeight = 0

  // 1. 里程碑进度（权重 40%）- 如果有里程碑
  if (milestones.value.length > 0) {
    const totalMilestones = milestones.value.length
    const completedMilestones = milestones.value.filter(m => m.status === 'DONE').length
    const inProgressMilestones = milestones.value.filter(m => m.status === 'IN_PROGRESS').length
    
    // 要求至少 3 个里程碑才计入权重，避免刷进度
    if (totalMilestones >= 3) {
      const milestoneProgress = ((completedMilestones + inProgressMilestones * 0.5) / totalMilestones) * 100
      totalProgress += milestoneProgress * 0.4
      totalWeight += 0.4
    } else {
      // 里程碑少于 3 个，降低权重到 20%
      const milestoneProgress = ((completedMilestones + inProgressMilestones * 0.5) / totalMilestones) * 100
      totalProgress += milestoneProgress * 0.2
      totalWeight += 0.2
    }
  }

  // 2. 活动度指标（权重 30%）- 基于评论、文件、成员活跃度
  const activityProgress = calculateActivityProgress()
  totalProgress += activityProgress * 0.3
  totalWeight += 0.3

  // 3. 时间进度（权重 30%）- 基于项目周期
  if (project.value.startDate && project.value.endDate) {
    const start = new Date(project.value.startDate).getTime()
    const end = new Date(project.value.endDate).getTime()
    const now = Date.now()

    if (end > start && now > start) {
      const timeProgress = Math.min(100, ((now - start) / (end - start)) * 100)
      totalProgress += timeProgress * 0.3
      totalWeight += 0.3
    }
  }

  // 如果没有足够的数据，使用状态作为兜底
  if (totalWeight < 0.5) {
    const statusProgressMap: Record<string, number> = {
      DRAFT: 5,
      RECRUITING: 20,
      IN_PROGRESS: 60,
      PENDING_REVIEW: 80,
      COMPLETED: 100,
      ARCHIVED: 100,
    }
    return statusProgressMap[project.value.status] ?? 0
  }

  // 计算加权平均进度
  const finalProgress = totalProgress / totalWeight
  
  // 限制进度范围：至少 5%，最多 95%（除非全部里程碑完成）
  if (milestones.value.length > 0 && 
      milestones.value.every(m => m.status === 'DONE')) {
    return 100
  }
  
  return Math.min(95, Math.max(5, Math.round(finalProgress)))
})

// 计算活动度进度（基于客观数据）
const calculateActivityProgress = () => {
  if (!project.value) return 0
  
  let score = 0
  let maxScore = 0
  
  // 评论活跃度（最高 30 分）
  const commentCount = comments.value.length + pinnedComments.value.length
  score += Math.min(30, commentCount * 2)
  maxScore += 30
  
  // 成员参与度（最高 40 分）
  const memberCount = members.value.length
  const targetMembers = project.value.teamSize || 5
  score += Math.min(40, (memberCount / targetMembers) * 40)
  maxScore += 40
  
  // 项目持续时间（最高 30 分）- 项目运行时间越长，说明越活跃
  if (project.value.createdAt) {
    const daysSinceCreation = Math.floor(
      (Date.now() - new Date(project.value.createdAt).getTime()) / (1000 * 60 * 60 * 24)
    )
    // 每 3 天得 10 分，最多 30 分
    score += Math.min(30, Math.floor(daysSinceCreation / 3) * 10)
    maxScore += 30
  }
  
  return maxScore > 0 ? (score / maxScore) * 100 : 0
}

// 计算里程碑进度详情
const milestoneProgressDetail = computed(() => {
  if (milestones.value.length === 0) return null
  
  const total = milestones.value.length
  const done = milestones.value.filter(m => m.status === 'DONE').length
  const inProgress = milestones.value.filter(m => m.status === 'IN_PROGRESS').length
  const planned = milestones.value.filter(m => m.status === 'PLANNED').length
  
  // 判断里程碑数量是否合理
  const isReasonable = total >= 3
  
  return {
    total,
    done,
    inProgress,
    planned,
    donePercent: Math.round((done / total) * 100),
    inProgressPercent: Math.round((inProgress / total) * 100),
    isReasonable,
    warning: !isReasonable ? '建议至少创建 3 个里程碑以获得更准确的进度评估' : null
  }
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
  if (!project.value) return ''
  if (project.value.status === 'COMPLETED') return 'success'
  if (project.value.status === 'DRAFT') return 'exception'
  return ''
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

const canPinComment = computed(() => {
  return isProjectOwner.value
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

const openCompleteDialog = async () => {
  try {
    await ElMessageBox.confirm(
      '完成项目后将无法撤销，项目状态将变为"已完成"。确定要完成此项目吗？',
      '确认完成项目',
      {
        confirmButtonText: '确定完成',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    completeDialogVisible.value = true
  } catch (error) {
    // 用户取消操作
  }
}

const handleProjectCompleted = async () => {
  await handleProjectUpdated()
  ElMessage.success('项目已完成！')
}

const handleStartProject = async () => {
  if (!project.value) return
  
  try {
    await ElMessageBox.confirm(
      '确定要开始此项目吗？开始后项目状态将变为"进行中"，将不再接受新成员申请。',
      '确认开始项目',
      {
        confirmButtonText: '确定开始',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    
    // 调用后端 API 更新项目状态
    await updateProject(project.value.id, {
      status: 'IN_PROGRESS'
    })
    
    await handleProjectUpdated()
    ElMessage.success('项目已开始！')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
      ElMessage.error('开始项目失败')
    }
  }
}

const handleArchiveProject = async () => {
  if (!project.value) return
  
  try {
    await ElMessageBox.confirm(
      '确定要归档此项目吗？归档后项目将不再显示在主列表中，但仍可通过"已归档"筛选查看。',
      '确认归档项目',
      {
        confirmButtonText: '确定归档',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    
    // 调用后端 API 更新项目状态
    await updateProject(project.value.id, {
      status: 'ARCHIVED'
    })
    
    await handleProjectUpdated()
    ElMessage.success('项目已归档')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
      ElMessage.error('归档项目失败')
    }
  }
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
    // request.ts 的响应拦截器已经解包了 res.data，所以这里直接得到 List<MilestoneVO>
    const data: any = await getProjectMilestones(projectId)
    milestones.value = Array.isArray(data) ? data : []
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
    const allComments = pageData?.records || []
    
    // 暂时使用本地存储模拟置顶功能（后端未实现）
    const pinnedIds = getPinnedCommentIds(projectId)
    pinnedComments.value = allComments.filter((c: ProjectComment) => pinnedIds.includes(c.id))
    comments.value = allComments.filter((c: ProjectComment) => !pinnedIds.includes(c.id))
    commentTotal.value = pageData?.total ?? 0
  } catch (error) {
    console.error(error)
    ElMessage.error('加载评论失败')
  } finally {
    commentsLoading.value = false
  }
}

// 本地存储置顶评论ID（临时方案，等待后端实现）
const getPinnedCommentIds = (projectId: number): number[] => {
  try {
    const key = `pinned_comments_${projectId}`
    const stored = localStorage.getItem(key)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

const savePinnedCommentIds = (projectId: number, ids: number[]) => {
  try {
    const key = `pinned_comments_${projectId}`
    localStorage.setItem(key, JSON.stringify(ids))
  } catch (error) {
    console.error('保存置顶信息失败', error)
  }
}

const handlePinComment = async (comment: ProjectComment) => {
  if (!project.value) return
  
  try {
    // TODO: 调用后端 API 置顶评论
    // await pinProjectComment(comment.id)
    
    // 临时方案：使用本地存储
    const pinnedIds = getPinnedCommentIds(project.value.id)
    if (!pinnedIds.includes(comment.id)) {
      pinnedIds.push(comment.id)
      savePinnedCommentIds(project.value.id, pinnedIds)
    }
    
    // 更新显示
    comments.value = comments.value.filter(c => c.id !== comment.id)
    pinnedComments.value.push(comment)
    
    ElMessage.success('评论已置顶')
  } catch (error) {
    console.error(error)
    ElMessage.error('置顶失败')
  }
}

const handleUnpinComment = async (comment: ProjectComment) => {
  if (!project.value) return
  
  try {
    // TODO: 调用后端 API 取消置顶
    // await unpinProjectComment(comment.id)
    
    // 临时方案：使用本地存储
    const pinnedIds = getPinnedCommentIds(project.value.id)
    const newIds = pinnedIds.filter(id => id !== comment.id)
    savePinnedCommentIds(project.value.id, newIds)
    
    // 更新显示
    pinnedComments.value = pinnedComments.value.filter(c => c.id !== comment.id)
    comments.value.unshift(comment)
    
    ElMessage.success('已取消置顶')
  } catch (error) {
    console.error(error)
    ElMessage.error('取消置顶失败')
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

const handleCompleteMilestone = async (item: ProjectMilestone) => {
  if (!project.value) return
  try {
    await ElMessageBox.confirm(
      `确定要将里程碑"${item.title}"标记为已完成吗？`,
      '完成里程碑',
      { 
        type: 'success',
        confirmButtonText: '确定完成',
        cancelButtonText: '取消'
      }
    )
    
    // 更新里程碑状态为已完成，并设置实际完成时间
    await updateProjectMilestone(item.id, {
      status: 'DONE',
      actualAt: new Date().toISOString().slice(0, 19)
    })
    
    ElMessage.success('里程碑已完成')
    await loadMilestones(project.value.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.response?.data?.message || '操作失败')
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
        color: var(--el-text-color-regular);
        line-height: 1.8;
      }
    }

    .project-progress {
      margin-top: 24px;

      .progress-header {
        display: flex;
        align-items: center;
        margin-bottom: 12px;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
        }
      }

      .progress-detail {
        display: flex;
        flex-direction: column;
        gap: 16px;
        margin-top: 16px;
        padding: 16px;
        background: var(--el-fill-color-lighter);
        border-radius: 8px;

        .detail-section {
          .section-title {
            font-size: 13px;
            font-weight: 600;
            color: var(--el-text-color-primary);
            margin-bottom: 8px;
            padding-bottom: 6px;
            border-bottom: 1px solid var(--el-border-color-lighter);
          }

          .detail-items {
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
          }

          .detail-item {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;

            .label {
              color: var(--el-text-color-secondary);
            }

            .value {
              font-weight: 600;

              &.success {
                color: var(--el-color-success);
              }

              &.warning {
                color: var(--el-color-warning);
              }

              &.info {
                color: var(--el-text-color-regular);
              }
            }
          }

          .detail-warning {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-top: 8px;
            padding: 6px 10px;
            font-size: 12px;
            color: var(--el-color-warning);
            background: var(--el-color-warning-light-9);
            border-radius: 4px;
            border-left: 2px solid var(--el-color-warning);

            .el-icon {
              font-size: 14px;
            }
          }
        }
      }

      .progress-tip {
        display: flex;
        align-items: flex-start;
        gap: 6px;
        margin-top: 12px;
        padding: 10px 12px;
        font-size: 12px;
        line-height: 1.5;
        color: var(--el-text-color-secondary);
        background: var(--el-fill-color-lighter);
        border-radius: 4px;
        border-left: 3px solid var(--el-color-primary);

        .el-icon {
          font-size: 14px;
          margin-top: 2px;
          flex-shrink: 0;
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
            color: var(--el-text-color-secondary);
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
          color: var(--el-text-color-regular);
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
          color: var(--el-text-color-secondary);
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
          color: var(--el-text-color-secondary);
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
        margin-bottom: 20px;

        h3 {
          margin: 0;
          font-size: 18px;
          font-weight: 600;
        }

        .hint {
          font-size: 13px;
          color: var(--el-text-color-secondary);
        }
      }

      .discussion-list {
        margin-bottom: 24px;

        .pinned-comments-section {
          margin-bottom: 24px;
          padding: 16px;
          background: var(--el-fill-color-lighter);
          border-radius: 8px;
          border: 1px solid var(--el-border-color);

          .section-header {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 12px;
            font-size: 14px;
            font-weight: 600;
            color: var(--el-color-warning);

            .el-icon {
              font-size: 16px;
            }
          }

          .pinned-comment {
            background: var(--el-bg-color);
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 12px;
            border: 1px solid var(--el-border-color-lighter);

            &:last-child {
              margin-bottom: 0;
            }
          }
        }

        .comment-item {
          display: flex;
          gap: 12px;
          padding: 16px 0;
          border-bottom: 1px solid var(--el-border-color-lighter);

          &:first-child {
            padding-top: 0;
          }
        }

        .comment-content {
          flex: 1;
          min-width: 0;
        }

        .comment-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;

          .author {
            font-weight: 500;
            font-size: 14px;
          }

          .time {
            margin-left: auto;
            font-size: 12px;
            color: var(--el-text-color-secondary);
          }

          .reply-to {
            font-size: 12px;
            color: var(--el-text-color-secondary);
          }
        }

        .comment-text {
          font-size: 14px;
          color: var(--el-text-color-regular);
          line-height: 1.6;
          margin: 4px 0 8px;
          
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
          margin-top: 12px;
          border-left: 2px solid var(--el-border-color-lighter);
          padding-left: 12px;

          .reply-item {
            display: flex;
            gap: 10px;
            padding: 8px 0;
          }

          .reply-content {
            flex: 1;
          }
        }

        .reply-input {
          margin-top: 12px;
          padding: 12px;
          background: var(--el-fill-color-lighter);
          border-radius: 8px;
          border: 1px solid var(--el-border-color);

          .reply-input-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
            font-size: 13px;
            color: var(--el-text-color-regular);
            font-weight: 500;
          }

          .input-actions {
            margin-top: 8px;
            display: flex;
            justify-content: flex-end;
            gap: 8px;
          }
        }

        .pagination {
          margin-top: 20px;
          display: flex;
          justify-content: center;
        }

        .empty-comments {
          padding: 40px 0;
        }
      }

      .discussion-input {
        padding: 20px;
        border-radius: 12px;
        background: var(--el-bg-color-overlay);
        border: 2px solid var(--el-border-color);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

        .input-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .input-title {
            font-size: 15px;
            font-weight: 600;
            color: var(--el-text-color-primary);
          }

          .input-hint {
            font-size: 12px;
            color: var(--el-text-color-secondary);
          }
        }

        .input-actions {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: 12px;

          .tip {
            font-size: 12px;
            color: var(--el-text-color-secondary);
          }
        }
      }
    }
  }
}
</style>

