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
            v-if="canRecommendCandidates" 
            type="primary"
            @click="handleRecommendCandidates"
          >
            <el-icon><UserFilled /></el-icon>
            智能推荐候选人
          </el-button>
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
          <el-button
            v-if="canApplyProject"
            size="large"
            @click="handleFindTeammates"
          >
            <el-icon><Connection /></el-icon>
            找队友一起申请
          </el-button>
          <!-- 举报按钮 -->
          <el-button
            v-if="authStore.isLoggedIn && project.creatorId !== authStore.user?.id"
            text
            type="danger"
            @click="handleReportProject"
          >
            <el-icon><WarningFilled /></el-icon>
            举报
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

      <!-- 技能需求 -->
      <div class="skill-requirements-section">
        <h3>技能需求</h3>
        <div v-if="skillRequirementsLoading" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="skillRequirements.length > 0" class="skill-requirements-list">
          <div 
            v-for="req in skillRequirements" 
            :key="req.id" 
            class="skill-requirement-item"
            :class="{ required: req.required }"
          >
            <div class="skill-name">
              <el-tag 
                :type="req.required ? 'danger' : 'info'" 
                size="small"
                effect="plain"
              >
                {{ req.required ? '必需' : '可选' }}
              </el-tag>
              <span>{{ req.skillName }}</span>
            </div>
            <div v-if="req.proficiencyLevel" class="skill-level">
              <span class="level-label">要求等级:</span>
              <el-tag 
                :type="getLevelType(req.proficiencyLevel)" 
                size="small"
              >
                {{ getLevelText(req.proficiencyLevel) }}
              </el-tag>
            </div>
          </div>
        </div>
        <div v-else class="no-requirements">
          <el-empty description="暂无技能需求" :image-size="80" />
        </div>
      </div>

      <el-divider />

      <!-- 时间段需求 -->
      <div class="time-slots-section">
        <h3>可用时段需求</h3>
        <div v-if="timeSlotsLoading" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="timeSlots.length > 0" class="time-slots-list">
          <div class="time-slots-grid">
            <div 
              v-for="(slot, index) in timeSlots" 
              :key="index" 
              class="time-slot-item"
            >
              <div class="slot-day">
                <el-icon><Calendar /></el-icon>
                <span>{{ getDayLabel(slot.dayOfWeek) }}</span>
              </div>
              <div class="slot-time">
                <el-icon><Clock /></el-icon>
                <span>{{ slot.startTime }} - {{ slot.endTime }}</span>
              </div>
            </div>
          </div>
          <div class="time-slots-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>项目需要成员在以上时段有空闲时间，以便协作和沟通</span>
          </div>
        </div>
        <div v-else class="no-requirements">
          <el-empty description="暂无时间段要求" :image-size="80" />
        </div>
      </div>

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

      <!-- 智能推荐候选人区域 -->
      <div v-if="canRecommendCandidates" ref="candidatesSection" class="recommended-candidates">
        <div class="candidates-header">
          <div class="title-area">
            <h3>智能推荐候选人</h3>
            <span class="hint">基于8维度匹配算法为项目推荐最合适的候选人</span>
          </div>
          <el-button 
            v-if="!showCandidates"
            type="primary"
            @click="handleLoadCandidates"
            :loading="candidatesLoading"
          >
            <el-icon><UserFilled /></el-icon>
            查看推荐
          </el-button>
          <el-button 
            v-else
            @click="handleCollapseCandidates"
          >
            <el-icon><ArrowUp /></el-icon>
            收起
          </el-button>
        </div>

        <!-- 候选人列表 -->
        <transition name="slide-fade">
          <div v-if="showCandidates" class="candidates-content">
            <!-- 加载状态 -->
            <div v-if="candidatesLoading" class="loading-state">
              <el-skeleton :rows="3" animated />
            </div>

            <!-- 空状态 -->
            <div v-else-if="candidates.length === 0" class="empty-state">
              <el-empty description="暂无推荐候选人">
                <el-button type="primary" @click="handleLoadCandidates">刷新推荐</el-button>
              </el-empty>
            </div>

            <!-- 候选人卡片 -->
            <div v-else class="candidates-grid">
              <div v-for="candidate in candidates" :key="candidate.userId" class="candidate-card">
                <!-- 左侧：匹配度和操作 -->
                <div class="card-left">
                  <!-- 匹配度圆环 -->
                  <div class="match-score">
                    <el-progress
                      type="circle"
                      :percentage="Math.round((candidate.score || 0) * 100)"
                      :width="80"
                      :color="getScoreColor((candidate.score || 0) * 100)"
                    >
                      <template #default="{ percentage }">
                        <div class="percentage-content">
                          <span class="percentage-value">{{ percentage }}</span>
                          <span class="percentage-label">
                            匹配度
                            <el-tooltip placement="top" effect="light">
                              <template #content>
                                <div style="max-width: 220px; line-height: 1.5;">
                                  总分 = 多维加权结果（技能、协作、时间、目标、经验等），
                                  不等同于单一技能分。
                                </div>
                              </template>
                              <el-icon class="score-help-icon"><QuestionFilled /></el-icon>
                            </el-tooltip>
                          </span>
                        </div>
                      </template>
                    </el-progress>
                  </div>
                  
                  <!-- 操作按钮 -->
                  <div class="card-actions">
                    <el-button type="primary" size="small" @click="handleInviteCandidate(candidate)">
                      邀请加入
                    </el-button>
                    <el-button type="primary" plain size="small" @click="handleViewCandidateDetail(candidate)">
                      查看详情
                    </el-button>
                  </div>
                </div>

                <!-- 右侧：候选人信息 -->
                <div class="candidate-info">
                  <h4 class="candidate-name">{{ candidate.username || '未知用户' }}</h4>
                  
                  <div class="candidate-meta">
                    <span v-if="candidate.department">{{ candidate.department }}</span>
                    <span v-if="candidate.major">{{ candidate.major }}</span>
                    <span v-if="candidate.grade">{{ candidate.grade }}年级</span>
                  </div>

                  <p v-if="candidate.bio" class="candidate-bio">{{ candidate.bio }}</p>

                  <!-- 匹配详情 -->
                  <div class="match-details">
                    <div class="detail-item">
                      <span class="label">技能匹配</span>
                      <el-progress
                        :percentage="Math.round((candidate.breakdown?.skill || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                      />
                      <span class="value">{{ Math.round((candidate.breakdown?.skill || 0) * 100) }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">协作历史</span>
                      <el-progress
                        :percentage="Math.round((candidate.breakdown?.collaboration || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                      />
                      <span class="value">{{ formatCollaborationScore(candidate) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">时间匹配</span>
                      <el-progress
                        :percentage="Math.round((candidate.breakdown?.time || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                      />
                      <span class="value">{{ Math.round((candidate.breakdown?.time || 0) * 100) }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">目标契合</span>
                      <el-progress
                        :percentage="Math.round((candidate.breakdown?.goal || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                      />
                      <span class="value">{{ Math.round((candidate.breakdown?.goal || 0) * 100) }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">项目经验</span>
                      <el-progress
                        :percentage="Math.round((candidate.breakdown?.experience || 0) * 100)"
                        :stroke-width="6"
                        :show-text="false"
                      />
                      <span class="value">{{ Math.round((candidate.breakdown?.experience || 0) * 100) }}%</span>
                    </div>
                  </div>

                  <div class="candidate-badges">
                    <el-tag size="small" :type="getConfidenceTagType(candidate.confidenceLevel)">
                      置信度：{{ getConfidenceText(candidate.confidenceLevel) }}
                    </el-tag>
                    <el-tag size="small" :type="getRiskTagType(candidate.riskLevel)">
                      风险：{{ getRiskText(candidate.riskLevel) }}
                    </el-tag>
                    <el-tag v-if="candidate.creditScore != null" size="small" type="info">
                      信誉分：{{ candidate.creditScore }}
                    </el-tag>
                  </div>

                  <!-- 推荐理由 -->
                  <p v-if="candidate.matchReason" class="match-reason">
                    推荐理由：{{ candidate.matchReason }}
                  </p>
                  <p v-if="candidate.explainSummary" class="explain-summary">
                    {{ candidate.explainSummary }}
                  </p>

                  <div v-if="candidate.strengths?.length" class="explain-list">
                    <div class="list-title">优势项</div>
                    <ul>
                      <li v-for="(item, idx) in candidate.strengths" :key="`s-${idx}`">{{ item }}</li>
                    </ul>
                  </div>

                  <div v-if="candidate.weaknesses?.length" class="explain-list warn">
                    <div class="list-title">待关注项</div>
                    <ul>
                      <li v-for="(item, idx) in candidate.weaknesses" :key="`w-${idx}`">{{ item }}</li>
                    </ul>
                  </div>

                  <div v-if="candidate.improvementTips?.length" class="explain-list tips">
                    <div class="list-title">提升建议</div>
                    <ul>
                      <li v-for="(item, idx) in candidate.improvementTips" :key="`t-${idx}`">{{ item }}</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
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
                      <el-button
                        v-if="authStore.isLoggedIn && comment.userId !== authStore.user?.id"
                        link
                        size="small"
                        type="danger"
                        @click="handleReportComment(comment)"
                      >
                        举报
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
      :members="projectMembersForEvaluation"
      @success="handleProjectCompleted"
    />

    <!-- 找队友一起申请对话框 -->
    <el-dialog
      v-model="findTeammatesDialogVisible"
      title="找队友一起申请"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="find-teammates-dialog">
        <div class="dialog-header">
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>
              <div class="alert-content">
                <span>选择队友后，你们将作为一个团队一起申请该项目</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 300px; line-height: 1.6;">
                      <p style="margin: 0 0 8px 0; font-weight: 600;">团队申请流程：</p>
                      <p style="margin: 4px 0;">1. 你选择队友并提交申请</p>
                      <p style="margin: 4px 0;">2. 队友需要确认参与</p>
                      <p style="margin: 4px 0;">3. 所有队友确认后，项目创建者审核</p>
                      <p style="margin: 4px 0;">4. 审核通过后，所有人加入项目</p>
                    </div>
                  </template>
                  <el-icon style="cursor: help; margin-left: 8px;">
                    <QuestionFilled />
                  </el-icon>
                </el-tooltip>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 加载状态 -->
        <div v-if="teammatesLoading" class="loading-state">
          <div class="loading-content">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
            <p>正在为你推荐合适的队友...</p>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="recommendedTeammates.length === 0" class="empty-state">
          <el-empty description="暂无推荐队友">
            <template #image>
              <el-icon :size="80" color="#909399"><UserFilled /></el-icon>
            </template>
            <template #description>
              <p>暂时没有找到合适的队友</p>
              <p class="empty-hint">可能是因为：</p>
              <ul class="empty-reasons">
                <li>系统中符合条件的用户较少</li>
                <li>匹配算法正在优化中</li>
                <li>你可以稍后再试</li>
              </ul>
            </template>
            <el-button type="primary" @click="handleLoadTeammates">
              <el-icon><Refresh /></el-icon>
              刷新推荐
            </el-button>
          </el-empty>
        </div>

        <!-- 队友列表 -->
        <div v-else class="teammates-list">
          <div
            v-for="teammate in recommendedTeammates"
            :key="teammate.userId"
            class="teammate-item"
            :class="{ selected: selectedTeammates.has(teammate.userId) }"
            @click="toggleTeammateSelection(teammate)"
          >
            <el-checkbox
              :model-value="selectedTeammates.has(teammate.userId)"
              @click.stop
              @change="toggleTeammateSelection(teammate)"
            />
            
            <el-avatar
              :size="50"
              :src="teammate.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
            />

            <div class="teammate-info">
              <div class="name-row">
                <span class="name">{{ teammate.username || '未知用户' }}</span>
                <el-tag size="small" type="primary">
                  匹配度 {{ Math.round((teammate.score || 0) * 100) }}%
                </el-tag>
              </div>
              <div class="meta">
                <span v-if="teammate.department">{{ teammate.department }}</span>
                <span v-if="teammate.major">{{ teammate.major }}</span>
                <span v-if="teammate.grade">{{ teammate.grade }}年级</span>
              </div>
              <p v-if="teammate.bio" class="bio">{{ teammate.bio }}</p>
              
              <!-- 技能标签 -->
              <div v-if="teammate.skills && teammate.skills.length > 0" class="skills">
                <el-tag
                  v-for="skill in teammate.skills.slice(0, 3)"
                  :key="skill"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ skill }}
                </el-tag>
                <el-tag v-if="teammate.skills.length > 3" size="small" type="info" effect="plain">
                  +{{ teammate.skills.length - 3 }}
                </el-tag>
              </div>
            </div>

            <div class="match-score">
              <el-progress
                type="circle"
                :percentage="Math.round((teammate.score || 0) * 100)"
                :width="60"
                :color="getScoreColor((teammate.score || 0) * 100)"
              />
            </div>
          </div>
        </div>

        <!-- 申请说明 -->
        <div v-if="recommendedTeammates.length > 0" class="application-message">
          <el-form-item label="申请说明">
            <el-input
              v-model="teamApplicationMessage"
              type="textarea"
              :rows="3"
              placeholder="向项目创建者说明你们团队的优势和为什么想加入这个项目..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="selected-count">
            已选择 {{ selectedTeammates.size }} 人
          </div>
          <div>
            <el-button @click="findTeammatesDialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="selectedTeammates.size === 0"
              :loading="teamApplying"
              @click="handleTeamApply"
            >
              一起申请（{{ selectedTeammates.size + 1 }}人）
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 候选人详情对话框（与人才广场复用同一组件） -->
    <TalentDetailDialog
      v-model="showCandidateDetailDialog"
      :talent="selectedCandidate"
    />

    <!-- 举报对话框 -->
    <ReportDialog
      v-if="project"
      ref="reportDialogRef"
      :target-type="TargetType.PROJECT"
      :target-id="project.id"
      :target-name="project.title"
      @success="handleReportSuccess"
    />

    <!-- 举报评论对话框 -->
    <ReportDialog
      v-if="reportingComment"
      ref="commentReportDialogRef"
      :target-type="TargetType.COMMENT"
      :target-id="reportingComment.id"
      :target-name="`${reportingComment.nickname || reportingComment.username || '用户'}的评论`"
      @success="handleCommentReportSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Star, InfoFilled, WarningFilled, QuestionFilled, UserFilled, ArrowUp, Loading, Calendar, Clock, Connection, Refresh } from '@element-plus/icons-vue'
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
  updateProject,
  getMatchedCandidatesForProject,
  getProjectSkillRequirements,
  getProjectTimeSlots,
  teamApplyProject
} from '../../api/project'
import { getRecommendedTeammates } from '../../api/matching'
import type { TeamApplicationRequest } from '../../api/project'
import { useAuthStore } from '../../store/auth'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import EditProjectDialog from '@/components/project/EditProjectDialog.vue'
import CompleteProjectDialog from '@/components/project/CompleteProjectDialog.vue'
import TalentDetailDialog from '@/components/talent/TalentDetailDialog.vue'
import ReportDialog from '@/components/report/ReportDialog.vue'
import { TargetType } from '@/api/report'
import type { Project, ProjectComment, ProjectMilestone, ProjectSkillRequirement } from '../../types/project'
import type { TeamMember } from '../../types/team'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const project = ref<Project | null>(null)
const members = ref<TeamMember[]>([])
const membersLoading = ref(false)
const skillRequirements = ref<ProjectSkillRequirement[]>([])
const skillRequirementsLoading = ref(false)
const timeSlots = ref<any[]>([])
const timeSlotsLoading = ref(false)

const editProjectDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const reportDialogRef = ref<InstanceType<typeof ReportDialog> | null>(null)

// 举报评论相关
const commentReportDialogRef = ref<InstanceType<typeof ReportDialog> | null>(null)
const reportingComment = ref<ProjectComment | null>(null)

const canEditProject = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false

  // 已完成或已归档的项目不可编辑
  if (project.value.status === 'COMPLETED' || project.value.status === 'ARCHIVED') {
    return false
  }

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
  const userRoles = (user.roles || []) as string[]
  if (userRoles.includes('MENTOR') || userRoles.includes('ADMIN')) return false
  
  // 已经是项目成员的不能再申请
  const isMember = members.value.some((m: any) => (m.userId ?? m.id) === user.id)
  if (isMember) return false
  
  return true
})

// 是否可以推荐候选人（只有项目创建者且项目在招募中）
const canRecommendCandidates = computed(() => {
  const userId = authStore.user?.id
  if (!userId || !project.value) return false
  
  // 只有项目创建者可以查看推荐候选人
  if (project.value.creatorId !== userId) return false
  
  // 只有招募中的项目可以推荐候选人
  if (project.value.status !== 'RECRUITING') return false
  
  return true
})

// 需要评价的成员（排除当前用户自己）
const projectMembersForEvaluation = computed(() => {
  const currentUserId = authStore.user?.id
  if (!currentUserId) return []
  
  return members.value
    .filter((m: any) => {
      const memberId = m.userId ?? m.id
      return memberId !== currentUserId
    })
    .map((m: any) => ({
      userId: m.userId ?? m.id,
      userName: m.username || m.userName || '未知用户',
      realName: m.realName
    }))
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

const formatDate = (date: string | Date) => {
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleString('zh-CN')
}

// 获取技能等级文本
const getLevelText = (level: string) => {
  const levelMap: Record<string, string> = {
    'BEGINNER': '入门',
    'INTERMEDIATE': '熟练',
    'ADVANCED': '高级',
    'EXPERT': '精通'
  }
  return levelMap[level] || level
}

// 获取技能等级标签类型
const getLevelType = (level: string): '' | 'success' | 'info' | 'warning' | 'danger' => {
  const typeMap: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
    'BEGINNER': 'info',      // 灰色 - 入门
    'INTERMEDIATE': '',      // 默认色 - 熟练
    'ADVANCED': 'warning',   // 橙色 - 高级
    'EXPERT': 'success'      // 绿色 - 精通
  }
  return typeMap[level] || ''
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
    await loadProjectDetail()
    // 重新加载技能需求
    if (project.value) {
      await loadSkillRequirements(project.value.id)
    }
    ElMessage.success('项目已更新')
  } catch (e) {
    // ignore
  }
}

const openEditProject = () => {
  editProjectDialogVisible.value = true
}

// 举报项目
const handleReportProject = () => {
  if (reportDialogRef.value && project.value) {
    reportDialogRef.value.open()
  }
}

const handleReportSuccess = () => {
  ElMessage.success('举报提交成功，我们会尽快处理')
}

// 举报评论
const handleReportComment = (comment: ProjectComment) => {
  reportingComment.value = comment
  nextTick(() => {
    if (commentReportDialogRef.value) {
      commentReportDialogRef.value.open()
    }
  })
}

const handleCommentReportSuccess = () => {
  ElMessage.success('举报提交成功，我们会尽快处理')
  reportingComment.value = null
}

const loadMembers = async () => {
  const projectId = ensureProjectId()
  membersLoading.value = true
  try {
    const memberList = await getProjectMembers(projectId)
    members.value = memberList || []
  } catch (e) {
    console.error(e)
  } finally {
    membersLoading.value = false
  }
}

const openCompleteDialog = async () => {
  // 先加载团队成员
  if (!project.value) return
  
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
    
    // 加载团队成员（如果还没加载）
    if (members.value.length === 0) {
      await loadMembers()
    }
    
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

// ==================== 找队友一起申请功能 ====================
const findTeammatesDialogVisible = ref(false)
const recommendedTeammates = ref<any[]>([])
const teammatesLoading = ref(false)
const selectedTeammates = ref<Set<number>>(new Set())
const teamApplicationMessage = ref('')
const teamApplying = ref(false)

// 打开找队友对话框
const handleFindTeammates = async () => {
  findTeammatesDialogVisible.value = true
  selectedTeammates.value = new Set()
  teamApplicationMessage.value = ''
  
  // 加载推荐队友
  await handleLoadTeammates()
}

// 加载推荐队友
const handleLoadTeammates = async () => {
  teammatesLoading.value = true
  
  try {
    const data = await getRecommendedTeammates(20)
    
    if (data && Array.isArray(data)) {
      recommendedTeammates.value = data
      if (data.length > 0) {
        ElMessage.success(`为你找到 ${data.length} 位推荐队友`)
      } else {
        ElMessage.info('暂无推荐队友')
      }
    } else {
      recommendedTeammates.value = []
      ElMessage.warning('队友数据格式异常')
    }
  } catch (error) {
    console.error('加载推荐队友失败:', error)
    recommendedTeammates.value = []
    ElMessage.error('加载推荐队友失败')
  } finally {
    teammatesLoading.value = false
  }
}

// 切换队友选择
const toggleTeammateSelection = (teammate: any) => {
  const userId = teammate.userId
  if (selectedTeammates.value.has(userId)) {
    selectedTeammates.value.delete(userId)
  } else {
    selectedTeammates.value.add(userId)
  }
  // 触发响应式更新
  selectedTeammates.value = new Set(selectedTeammates.value)
}

// 团队申请
const handleTeamApply = async () => {
  if (!project.value) return
  if (selectedTeammates.value.size === 0) {
    ElMessage.warning('请至少选择一位队友')
    return
  }
  
  teamApplying.value = true
  
  try {
    const message = teamApplicationMessage.value || '希望和队友一起加入该项目'
    const teammateIds = Array.from(selectedTeammates.value)
    
    // 构建团队申请请求
    const request: TeamApplicationRequest = {
      applicantIds: [authStore.user!.id, ...teammateIds],
      message: message
    }
    
    // 调用真正的团队申请接口
    await teamApplyProject(project.value.id, request)
    
    ElMessage.success(`团队申请已提交！你和 ${selectedTeammates.value.size} 位队友的申请正在等待队友确认和项目审核`)
    findTeammatesDialogVisible.value = false
    
    // 清空选择
    selectedTeammates.value = new Set()
    teamApplicationMessage.value = ''
  } catch (error) {
    console.error('团队申请失败:', error)
    ElMessage.error('申请失败，请稍后重试')
  } finally {
    teamApplying.value = false
  }
}
// ==================== 找队友一起申请功能结束 ====================


const handleRecommendCandidates = () => {
  if (!project.value) return
  // 滚动到候选人区域并展开
  handleLoadCandidates()
  // 平滑滚动到候选人区域
  setTimeout(() => {
    const section = document.querySelector('.recommended-candidates')
    if (section) {
      section.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, 100)
}

// 候选人推荐相关状态
const showCandidates = ref(false)
const candidates = ref<any[]>([])
const candidatesLoading = ref(false)
const candidatesSection = ref<HTMLElement | null>(null)

// 加载候选人
const handleLoadCandidates = async () => {
  if (!project.value) return
  
  showCandidates.value = true
  candidatesLoading.value = true
  
  try {
    const data = await getMatchedCandidatesForProject(project.value.id)

    if (data && Array.isArray(data)) {
      candidates.value = data
      if (data.length > 0) {
        ElMessage.success(`为项目找到 ${data.length} 位匹配候选人`)
      } else {
        ElMessage.info('暂无匹配候选人')
      }
    } else {
      candidates.value = []
      ElMessage.warning('候选人数据格式异常')
    }
  } catch (error) {
    console.error('加载候选人失败:', error)
    candidates.value = []
    ElMessage.error('加载候选人失败')
  } finally {
    candidatesLoading.value = false
  }
}

// 收起候选人区域
const handleCollapseCandidates = () => {
  showCandidates.value = false
}

// 邀请候选人
const handleInviteCandidate = (candidate: any) => {
  ElMessage.info('邀请功能开发中...')
  console.log('邀请候选人:', candidate)
}

// 候选人详情对话框（与人才广场共用同一组件）
const showCandidateDetailDialog = ref(false)
const selectedCandidate = ref<any>(null)

// 查看候选人详情
const handleViewCandidateDetail = (candidate: any) => {
  selectedCandidate.value = candidate
  showCandidateDetailDialog.value = true
}

// 获取匹配度颜色
const getScoreColor = (score: number) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getConfidenceText = (level?: string) => {
  if (level === 'HIGH') return '高'
  if (level === 'MEDIUM') return '中'
  return '低'
}

const getConfidenceTagType = (level?: string) => {
  if (level === 'HIGH') return 'success'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

const getRiskText = (level?: string) => {
  if (level === 'HIGH') return '高'
  if (level === 'MEDIUM') return '中'
  return '低'
}

const getRiskTagType = (level?: string) => {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}

// 格式化协作历史得分（检查是否有真实数据）
const formatCollaborationScore = (candidate: any) => {
  const score = candidate.breakdown?.collaboration || 0
  const percentage = Math.round(score * 100)
  
  // 检查是否接近50%（默认值），且没有真实数据
  // 如果得分在48%-52%之间，且候选人数据中没有评价和协作历史，显示"暂无数据"
  if (percentage >= 48 && percentage <= 52) {
    // 检查候选人是否有真实的协作数据
    const hasEvaluations = candidate.evaluations && 
      (candidate.evaluations.avg_tech_contribution > 0 || 
       candidate.evaluations.avg_collaboration > 0 || 
       candidate.evaluations.avg_task_completion > 0)
    
    const hasCollabHistory = candidate.collaboration_history && 
      candidate.collaboration_history.length > 0
    
    if (!hasEvaluations && !hasCollabHistory) {
      return '暂无数据'
    }
  }
  
  return `${percentage}%`
}

// 带重试机制的数据加载
const loadProjectDetailWithRetry = async (retries = 2): Promise<void> => {
  const projectId = Number(route.params.id)
  if (!projectId) return

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      // 并行加载所有数据，提升加载速度
      const [projectRes, membersRes, commentsRes, milestonesRes, skillReqRes, timeSlotsRes] = await Promise.allSettled([
        getProjectDetail(projectId),
        loadMembers(),
        loadComments(projectId),
        loadMilestones(projectId),
        loadSkillRequirements(projectId),
        loadTimeSlots(projectId)
      ])

      // 处理项目详情加载结果
      if (projectRes.status === 'fulfilled') {
        project.value = projectRes.value as unknown as Project
      } else {
        throw projectRes.reason
      }

      // 如果其他数据加载失败，记录但不阻塞页面显示
      if (membersRes.status === 'rejected') {
        console.warn('加载项目成员失败:', membersRes.reason)
      }
      if (commentsRes.status === 'rejected') {
        console.warn('加载评论失败:', commentsRes.reason)
      }
      if (milestonesRes.status === 'rejected') {
        console.warn('加载里程碑失败:', milestonesRes.reason)
      }
      if (skillReqRes.status === 'rejected') {
        console.warn('加载技能需求失败:', skillReqRes.reason)
      }
      if (timeSlotsRes.status === 'rejected') {
        console.warn('加载时间段需求失败:', timeSlotsRes.reason)
      }

      return // 成功则返回
    } catch (error) {
      if (attempt === retries) {
        // 最后一次重试失败
        console.error('加载项目详情失败:', error)
        ElMessage.error('加载项目详情失败，请刷新页面重试')
        throw error
      }
      // 等待后重试（指数退避）
      await new Promise(resolve => setTimeout(resolve, 1000 * (attempt + 1)))
    }
  }
}

const loadProjectDetail = () => loadProjectDetailWithRetry()

// 加载技能需求
const loadSkillRequirements = async (projectId: number) => {
  console.log('=== 加载技能需求 ===')
  console.log('项目ID:', projectId)
  skillRequirementsLoading.value = true
  try {
    const data = await getProjectSkillRequirements(projectId)
    console.log('接收到的技能需求数据:', data)
    console.log('技能需求数量:', data?.length || 0)
    skillRequirements.value = data || []
  } catch (error) {
    console.error('加载技能需求失败:', error)
    skillRequirements.value = []
  } finally {
    skillRequirementsLoading.value = false
  }
}

// 加载时间段需求
const loadTimeSlots = async (projectId: number) => {
  console.log('=== 加载时间段需求 ===')
  console.log('项目ID:', projectId)
  timeSlotsLoading.value = true
  try {
    const data = await getProjectTimeSlots(projectId)
    console.log('接收到的时间段数据:', data)
    console.log('时间段数量:', data?.length || 0)
    timeSlots.value = data || []
  } catch (error) {
    console.error('加载时间段需求失败:', error)
    timeSlots.value = []
  } finally {
    timeSlotsLoading.value = false
  }
}

// 获取星期标签
const getDayLabel = (dayOfWeek: number): string => {
  const dayLabels: Record<number, string> = {
    1: '周一', 2: '周二', 3: '周三', 4: '周四',
    5: '周五', 6: '周六', 7: '周日', 0: '周日'
  }
  return dayLabels[dayOfWeek] || `星期${dayOfWeek}`
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

    .recommended-candidates {
      margin-top: 24px;

      .candidates-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .title-area {
          h3 {
            margin: 0;
            font-size: 18px;
            font-weight: 600;
          }

          .hint {
            display: block;
            margin-top: 4px;
            font-size: 12px;
            color: var(--el-text-color-secondary);
          }
        }
      }

      .candidates-content {
        margin-top: 16px;
      }

      .loading-state,
      .empty-state {
        padding: 40px 0;
        text-align: center;
      }

      .candidates-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
        gap: 20px;
      }

      .candidate-card {
        border: 2px solid #e8eaed;
        border-radius: 16px;
        padding: 24px;
        display: flex;
        gap: 24px;
        transition: all 0.3s ease;
        background: #ffffff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

        &:hover {
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
          transform: translateY(-4px);
          border-color: #409eff;
        }

        .card-left {
          flex-shrink: 0;
          display: flex;
          flex-direction: column;
          align-items: stretch;
          gap: 16px;
          width: 120px;

          .match-score {
            .percentage-content {
              display: flex;
              flex-direction: column;
              align-items: center;

              .percentage-value {
                font-size: 20px;
                font-weight: 800;
                color: var(--el-text-color-primary);
              }

              .percentage-label {
                font-size: 12px;
                color: var(--el-text-color-secondary);
                margin-top: 4px;
                font-weight: 600;
                display: inline-flex;
                align-items: center;
                gap: 4px;

                .score-help-icon {
                  font-size: 12px;
                  color: var(--el-text-color-placeholder);
                  cursor: help;
                }
              }
            }
          }
          
          .card-actions {
            display: flex;
            flex-direction: column;
            align-items: stretch;
            gap: 8px;
            margin-top: 4px;
            
            .el-button {
              width: 100%;
            }

            .el-button + .el-button {
              margin-left: 0;
            }
          }
        }

        .candidate-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 12px;

          .candidate-name {
            margin: 0;
            font-size: 18px;
            font-weight: 800;
            color: var(--el-text-color-primary);
          }

          .candidate-meta {
            display: flex;
            gap: 8px;
            font-size: 13px;
            flex-wrap: wrap;

            span {
              padding: 6px 14px;
              background: linear-gradient(135deg, #f5f7fa 0%, #e8eaed 100%);
              border-radius: 20px;
              color: #606266;
              font-weight: 600;
              border: 1px solid #e0e3e8;
            }
          }

          .candidate-bio {
            margin: 0;
            font-size: 14px;
            color: var(--el-text-color-regular);
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .match-details {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .detail-item {
              display: grid;
              grid-template-columns: 80px 1fr 50px;
              align-items: center;
              gap: 12px;

              .label {
                font-size: 13px;
                color: var(--el-text-color-secondary);
                font-weight: 600;
              }

              .value {
                font-size: 14px;
                font-weight: 700;
                color: var(--el-text-color-primary);
                text-align: right;
              }
            }
          }

          .candidate-badges {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-top: 2px;
          }

          .match-reason {
            margin: 0;
            font-size: 13px;
            color: var(--el-text-color-regular);
          }

          .explain-summary {
            margin: 0;
            font-size: 13px;
            color: var(--el-text-color-secondary);
            line-height: 1.6;
          }

          .explain-list {
            margin-top: 4px;
            padding: 10px 12px;
            border-radius: 8px;
            background: var(--el-fill-color-lighter);

            .list-title {
              font-size: 12px;
              font-weight: 700;
              margin-bottom: 6px;
              color: var(--el-color-success);
            }

            ul {
              margin: 0;
              padding-left: 16px;
            }

            li {
              font-size: 12px;
              color: var(--el-text-color-regular);
              line-height: 1.5;
              margin-bottom: 4px;
            }

            &.warn .list-title {
              color: var(--el-color-warning);
            }

            &.tips .list-title {
              color: var(--el-color-primary);
            }
          }

          .actions {
            display: flex;
            gap: 8px;
            margin-top: 4px;
          }
        }
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

// 过渡动画
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}
  /* ==================== 候选人详情对话框样式 ==================== */
  .mentor-detail-dialog {
    :deep(.el-dialog) {
      border-radius: 24px;
      overflow: hidden;
      background: var(--bg-card);
      padding: 0;
    }
    :deep(.el-dialog__header) { display: none; }
    :deep(.el-dialog__body) { padding: 0; }
  }

  .mentor-detail {
    display: flex;
    flex-direction: column;
    max-height: 90vh;
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
      background: rgba(0,0,0,0.1);
      backdrop-filter: blur(8px);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-color);
      transition: all 0.2s;
      &:hover { background: rgba(0,0,0,0.2); transform: scale(1.1); }
    }

    .mentor-card-top {
      padding: 40px;
      background: linear-gradient(135deg, rgba(var(--accent-color-rgb), 0.08), rgba(var(--accent-color-rgb), 0.03));
      display: flex;
      gap: 32px;
      align-items: center;
      border-bottom: 1px solid var(--border-subtle);
    }

    .mentor-avatar-section {
      position: relative;
    }

    .mentor-avatar-large {
      border: 4px solid var(--bg-card);
      box-shadow: 0 12px 30px rgba(0,0,0,0.1);
    }

    .mentor-rating-badge {
      position: absolute;
      bottom: -10px;
      right: -10px;
      background: #f59e0b;
      color: #fff;
      padding: 4px 12px;
      border-radius: 100px;
      font-weight: 800;
      display: flex;
      align-items: center;
      gap: 4px;
      box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
    }

    .mentor-info-section {
      flex: 1;
    }

    .mentor-name-large { 
      font-size: 36px; 
      font-weight: 900; 
      margin: 0 0 8px; 
      color: var(--text-color); 
    }
    
    .mentor-dept-large { 
      font-size: 16px; 
      color: var(--text-color-muted); 
      margin: 0; 
    }

    .mentor-card-content {
      padding: 32px 40px;
      overflow-y: auto;
      flex: 1;
    }

    .mentor-content-area {
      padding: 32px 40px;
      overflow-y: auto;
      flex: 1;
    }

    .content-block {
      margin-bottom: 32px;
      .block-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        .block-title {
          display: flex;
          align-items: center;
          gap: 10px;
          font-size: 18px;
          font-weight: 800;
          color: var(--text-color);
          .block-icon { color: var(--accent-color); }
        }
      }
      .block-content {
        .text-content { 
          font-size: 15px; 
          line-height: 1.8; 
          color: var(--text-color-muted); 
        }
      }
    }

    .skill-count {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-color-muted);
    }

    .skills-loading {
      display: flex;
      align-items: center;
      gap: 12px;
      color: var(--text-color-muted);
      font-size: 14px;
    }

    .skills-showcase {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      .skill-chip {
        padding: 6px 16px;
        border-radius: 100px;
        background: var(--bg-body);
        border: 1px solid var(--border-subtle);
        font-size: 14px;
        font-weight: 600;
        color: var(--text-color-muted);
        transition: all 0.2s;
        display: flex;
        align-items: center;
        gap: 8px;
        
        &:hover { 
          transform: translateY(-2px); 
        }
        
        .level-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #94a3b8;
          flex-shrink: 0;
        }
        
        &[data-level="EXPERT"] {
          .level-dot { background: #10b981; }
          border-color: rgba(16, 185, 129, 0.3);
          &:hover { 
            border-color: #10b981; 
            color: #10b981; 
          }
        }
        
        &[data-level="ADVANCED"] {
          .level-dot { background: var(--accent-color); }
          border-color: rgba(var(--accent-color-rgb), 0.3);
          &:hover { 
            border-color: var(--accent-color); 
            color: var(--accent-color); 
          }
        }
        
        &[data-level="INTERMEDIATE"] {
          .level-dot { background: #94a3b8; }
          border-color: rgba(148, 163, 184, 0.3);
          &:hover { 
            border-color: #94a3b8; 
            color: #94a3b8; 
          }
        }
        
        &[data-level="BEGINNER"] {
          .level-dot { background: #cbd5e1; }
          border-color: rgba(203, 213, 225, 0.3);
          &:hover { 
            border-color: #cbd5e1; 
            color: #cbd5e1; 
          }
        }
        
        /* 兴趣标签 */
        &.interest-chip {
          border-color: rgba(16, 185, 129, 0.3);
          background: rgba(16, 185, 129, 0.05);
          &:hover { 
            border-color: #10b981; 
            color: #10b981; 
            background: rgba(16, 185, 129, 0.1);
          }
        }
        
        /* 个人特质标签 */
        &.personality-chip {
          border-color: rgba(139, 92, 246, 0.3);
          background: rgba(139, 92, 246, 0.05);
          &:hover { 
            border-color: #8b5cf6; 
            color: #8b5cf6; 
            background: rgba(139, 92, 246, 0.1);
          }
        }
        
        /* 项目类型标签 */
        &.type-chip {
          border-color: rgba(245, 158, 11, 0.3);
          background: rgba(245, 158, 11, 0.05);
          &:hover { 
            border-color: #f59e0b; 
            color: #f59e0b; 
            background: rgba(245, 158, 11, 0.1);
          }
        }
      }
    }

    .no-skills {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--text-color-muted);
      font-size: 14px;
    }

    .tc-intentions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      
      .el-tag {
        font-weight: 600;
      }
    }

    .tc-time-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      color: var(--text-color-muted);
      
      .tc-time-icon {
        color: var(--accent-color);
      }
    }

    .markdown-wrapper {
      :deep(.markdown-body) {
        font-size: 15px;
        line-height: 1.8;
        color: var(--text-color-muted);
      }
    }

    .contact-info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      
      .contact-item {
        display: flex;
        align-items: center;
        padding: 12px 16px;
        background: var(--bg-body);
        border-radius: 8px;
        border: 1px solid var(--border-subtle);
        
        .contact-label {
          font-weight: 600;
          color: var(--text-color);
          margin-right: 8px;
          flex-shrink: 0;
        }
        
        .contact-value {
          color: var(--text-color-muted);
          flex: 1;
          word-break: keep-all;
          overflow-wrap: break-word;
        }
      }
    }

    .mentor-actions {
      padding: 24px 40px;
      display: flex;
      gap: 16px;
      border-top: 1px solid var(--border-subtle);
      background: var(--bg-card);
      .el-button { 
        flex: 1; 
        height: 52px; 
        border-radius: 14px; 
        font-weight: 700; 
        font-size: 16px; 
      }
    }
  }

  [data-theme='dark'] .mentor-detail {
    .mentor-card-top { background: linear-gradient(135deg, rgba(255,255,255,0.03), rgba(255,255,255,0.01)); }
    .close-btn { background: rgba(255,255,255,0.05); &:hover { background: rgba(255,255,255,0.1); } }
  }

  // 技能需求样式
  .skill-requirements-section {
    h3 {
      margin-bottom: 16px;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color-primary);
    }

    .loading-container {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 24px;
      color: var(--text-color-muted);
      font-size: 14px;
    }

    .skill-requirements-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .skill-requirement-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: var(--bg-body);
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      transition: all 0.2s;

      &:hover {
        border-color: var(--primary-color);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      }

      &.required {
        border-left: 3px solid var(--el-color-danger);
      }

      .skill-name {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 15px;
        font-weight: 500;
        color: var(--text-color-primary);
      }

      .skill-level {
        display: flex;
        align-items: center;
        gap: 8px;

        .level-label {
          font-size: 13px;
          color: var(--text-color-muted);
        }
      }
    }

    .no-requirements {
      padding: 24px;
      text-align: center;
    }
  }

  // 时间段需求样式
  .time-slots-section {
    h3 {
      margin-bottom: 16px;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-color-primary);
    }

    .time-slots-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .time-slots-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 12px;
    }

    .time-slot-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      background: var(--bg-body);
      border: 1px solid var(--border-subtle);
      border-radius: 8px;
      transition: all 0.2s;

      &:hover {
        border-color: var(--primary-color);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      }

      .slot-day {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 80px;
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color-primary);
      }

      .slot-time {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: var(--text-color-muted);
      }
    }

    .time-slots-tip {
      margin-top: 12px;
      padding: 12px;
      background: var(--bg-muted);
      border-radius: 6px;
      font-size: 13px;
      color: var(--text-color-muted);
      line-height: 1.6;
    }
  }
</style>

// ==================== 找队友一起申请对话框样式 ====================
.find-teammates-dialog {
  .dialog-header {
    margin-bottom: 20px;
    
    .alert-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  .loading-state {
    padding: 60px 0;
    text-align: center;
    
    .loading-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      
      p {
        margin: 0;
        font-size: 14px;
        color: var(--text-color-secondary);
      }
    }
  }

  .empty-state {
    padding: 40px 0;
    text-align: center;
    
    .empty-hint {
      margin: 16px 0 8px 0;
      font-size: 13px;
      color: var(--text-color-muted);
    }
    
    .empty-reasons {
      list-style: none;
      padding: 0;
      margin: 8px 0 20px 0;
      
      li {
        font-size: 13px;
        color: var(--text-color-secondary);
        line-height: 1.8;
        
        &:before {
          content: '•';
          margin-right: 8px;
          color: var(--text-color-muted);
        }
      }
    }
  }

  .teammates-list {
    max-height: 500px;
    overflow-y: auto;
    margin-bottom: 20px;

    .teammate-item {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      border: 2px solid var(--border-subtle);
      border-radius: 12px;
      margin-bottom: 12px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: var(--primary-color);
        background: var(--bg-elevated-soft);
        transform: translateX(4px);
      }

      &.selected {
        border-color: var(--primary-color);
        background: rgba(64, 158, 255, 0.05);
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
      }

      .teammate-info {
        flex: 1;
        min-width: 0;

        .name-row {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;

          .name {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-color);
          }
        }

        .meta {
          display: flex;
          gap: 12px;
          font-size: 13px;
          color: var(--text-color-muted);
          margin-bottom: 8px;

          span {
            &:not(:last-child)::after {
              content: '•';
              margin-left: 12px;
              color: var(--border-subtle);
            }
          }
        }

        .bio {
          font-size: 13px;
          color: var(--text-color-secondary);
          line-height: 1.5;
          margin: 0 0 8px 0;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }
        
        .skills {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          margin-top: 8px;
        }
      }

      .match-score {
        flex-shrink: 0;
      }
    }
  }

  .application-message {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid var(--border-subtle);
  }
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .selected-count {
    font-size: 14px;
    color: var(--text-color-secondary);
    font-weight: 500;
  }
}
