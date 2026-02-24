<template>
  <div class="team-overview" role="main" aria-labelledby="team-overview-title">
    <h1 id="team-overview-title" class="sr-only">团队概览</h1>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container" role="status" aria-live="polite" aria-label="正在加载团队概览">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container" role="alert" aria-live="assertive">
      <el-result
        icon="error"
        title="加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="loadOverviewData" aria-label="重新加载团队概览">重试</el-button>
        </template>
      </el-result>
    </div>

    <!-- 主内容 -->
    <div v-else class="overview-content">
      <div class="overview-grid">
        <!-- Left Column: Primary Info & Members & Activity -->
        <div class="main-column">
          <!-- 团队基本信息卡片 -->
          <div class="info-card glass-card" role="region" aria-labelledby="team-info-title">
            <div class="team-header">
              <div
                class="team-avatar-wrapper"
                role="img"
                :aria-label="`${teamInfo.name} 团队头像`"
                v-loading="avatarUploading"
              >
                <el-image 
                  :src="teamInfo.avatar || '/default-avatar.png'" 
                  :alt="`${teamInfo.name} 团队头像`"
                  class="team-avatar-img"
                  lazy
                  fit="cover"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <el-upload
                  v-if="isMember"
                  class="avatar-uploader"
                  :show-file-list="false"
                  :auto-upload="false"
                  :on-change="handleAvatarChange"
                  :disabled="avatarUploading"
                  accept="image/*"
                >
                  <div class="avatar-overlay">
                    <el-icon><Camera /></el-icon>
                    <span>更换头像</span>
                  </div>
                </el-upload>
              </div>
              <div class="team-meta">
                <div class="title-row">
                  <h1 id="team-info-title">{{ teamInfo.name }}</h1>
                  <el-tag v-if="teamInfo.status" :type="teamInfo.status === 'ACTIVE' ? 'success' : 'info'" effect="light" round size="small">
                    {{ teamInfo.status === 'ACTIVE' ? '活跃' : '归档' }}
                  </el-tag>
                </div>
                
                <div class="description-section">
                  <div v-if="!editingDescription" class="description-wrapper">
                    <p class="description" :class="{ 'empty': !teamInfo.description }">
                      {{ teamInfo.description || '暂无描述' }}
                    </p>
                    <el-button
                      v-if="isLeader"
                      type="primary"
                      link
                      :icon="Edit"
                      class="edit-btn"
                      @click="startEditDescription"
                    >
                      编辑描述
                    </el-button>
                  </div>
                  <div v-else class="description-edit-wrapper">
                    <el-input
                      v-model="descriptionEdit"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入团队描述"
                      maxlength="200"
                      show-word-limit
                    />
                    <div class="edit-actions">
                      <el-button type="primary" size="small" @click="saveDescription" :loading="savingDescription">保存</el-button>
                      <el-button size="small" @click="cancelEditDescription">取消</el-button>
                    </div>
                  </div>
                </div>

                <div class="quick-stats-row">
                  <div class="q-stat">
                    <span class="q-label">成员</span>
                    <span class="q-value">{{ members.length }}<small>/{{ teamInfo.maxMembers || 5 }}</small></span>
                  </div>
                  <div class="v-divider"></div>
                  <div class="q-stat">
                    <span class="q-label">创建时间</span>
                    <span class="q-value">{{ formatDate(teamInfo.createdAt) }}</span>
                  </div>
                  <div class="v-divider"></div>
                  <div class="q-stat">
                    <span class="q-label">团队类型</span>
                    <span class="q-value">{{ teamInfo.type === 'COMPETITION' ? '竞赛团队' : '项目团队' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 团队成员墙 -->
          <div class="members-wall-card glass-card">
            <div class="section-header">
              <h2>团队成员 <span>({{ sortedMembers.length }})</span></h2>
              <el-button
                v-if="isLeader"
                type="primary"
                link
                size="small"
                :icon="Plus"
                @click="showAddMemberDialog = true"
              >
                添加成员
              </el-button>
            </div>
            <div class="members-list-grid" role="list">
              <div 
                v-for="(member, index) in sortedMembers" 
                :key="member.userId"
                class="member-item"
                :class="{ 
                  'has-actions': isLeader && !isSelfOrLeader(member),
                  'is-leader': member.role === 'LEADER' || member.role === 'OWNER',
                  'draggable': isLeader && !isSelfOrLeader(member)
                }"
                :draggable="isLeader && !isSelfOrLeader(member)"
                @dragstart="handleDragStart($event, index)"
                @dragover.prevent="handleDragOver($event, index)"
                @drop="handleDrop($event, index)"
                @dragend="handleDragEnd"
              >
                <div class="member-content" @click="handleViewMemberDetail(member)">
                  <div class="member-avatar-container">
                    <el-image :src="member.avatar || '/default-avatar.png'" lazy fit="cover">
                      <template #error>
                        <div class="image-error">{{ member.username?.charAt(0) }}</div>
                      </template>
                    </el-image>
                    <div class="role-dot" :class="member.role"></div>
                  </div>
                  <div class="member-info-mini">
                    <span class="name">{{ member.username }}</span>
                    <el-tag size="small" :type="member.role === 'LEADER' || member.role === 'OWNER' ? 'danger' : 'info'" effect="plain" class="role-tag">
                      {{ getRoleLabel(member.role) }}
                    </el-tag>
                  </div>
                </div>
                <div v-if="isLeader && !isSelfOrLeader(member)" class="member-actions">
                  <el-button
                    type="danger"
                    link
                    size="small"
                    :icon="Close"
                    @click.stop="handleRemoveMember(member)"
                  >
                    移除
                  </el-button>
                </div>
                <div v-if="isLeader && !isSelfOrLeader(member)" class="drag-handle">
                  <el-icon><Rank /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <!-- 最近活动 -->
          <div class="activity-card glass-card">
            <div class="section-header">
              <h2>最近活动</h2>
            </div>
            <div v-if="activities.length === 0" class="empty-state">
              <el-empty description="暂无活动记录" :image-size="80" />
            </div>
            <div v-else class="timeline-wrapper">
              <el-timeline>
                <el-timeline-item
                  v-for="activity in activities"
                  :key="activity.id"
                  :timestamp="formatTime(activity.createdAt)"
                  :color="getActivityColor(activity.activityType || activity.type)"
                  size="normal"
                >
                  <div class="activity-item-content">
                    <span class="user-name">{{ activity.username }}</span>
                    <span class="action-detail">{{ activity.detail }}</span>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>
        </div>

        <!-- Right Column: Stats & Side Modules -->
        <div class="side-column">
          <!-- 统计数据面板 -->
          <div class="stats-panel-grid">
            <div class="stat-box glass-card purple">
              <div class="stat-icon-wrapper">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stat-data">
                <h3>{{ statistics.taskCompletionRate }}%</h3>
                <p>任务完成率</p>
              </div>
            </div>
            <div class="stat-box glass-card blue">
              <div class="stat-icon-wrapper">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-data">
                <h3>{{ statistics.activeDays }}</h3>
                <p>活跃天数</p>
              </div>
            </div>
            <div class="stat-box glass-card orange">
              <div class="stat-icon-wrapper">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="stat-data">
                <h3>{{ statistics.messageCount }}</h3>
                <p>团队消息</p>
              </div>
            </div>
            <div class="stat-box glass-card green">
              <div class="stat-icon-wrapper">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-data">
                <h3>{{ statistics.fileCount }}</h3>
                <p>共享文件</p>
              </div>
            </div>
          </div>

          <!-- 关联比赛信息 -->
          <div class="competition-side-card glass-card">
            <div class="section-header">
              <h2><el-icon><Trophy /></el-icon> 关联比赛</h2>
              <el-button
                v-if="isLeader"
                type="primary"
                link
                size="small"
                :icon="Plus"
                @click="showAddCompetitionDialog = true"
              >
                添加比赛
              </el-button>
            </div>
            <div class="comp-info-content">
              <!-- 比赛列表 -->
              <div v-if="relatedCompetitions.length > 0" class="competition-list">
                <div 
                  v-for="comp in relatedCompetitions" 
                  :key="comp.id"
                  class="competition-item"
                >
                  <div class="comp-main">
                    <el-link 
                      type="primary" 
                      class="comp-name" 
                      @click="handleViewCompetitionDetail(comp.id)"
                    >
                      {{ comp.name }}
                    </el-link>
                    <div class="comp-meta">
                      <el-tag :type="getCompStatusType(comp.status)" size="small" effect="plain">
                        {{ getCompStatusText(comp.status) }}
                      </el-tag>
                      <span class="comp-date">{{ formatDate(comp.startDate) }}</span>
                    </div>
                  </div>
                  <el-button
                    v-if="isLeader"
                    type="danger"
                    link
                    :icon="Close"
                    size="small"
                    @click="handleRemoveCompetition(comp.id)"
                  />
                </div>
              </div>
              <div v-else class="empty-competitions">
                <p>暂无关联比赛</p>
              </div>
              
              <!-- 导师信息 -->
              <div v-if="teamInfo.type === 'COMPETITION'" class="mentor-section">
                <div class="mentor-header">
                  <span class="label">指导老师</span>
                  <el-button
                    v-if="!teamInfo.mentorId && isLeader"
                    type="primary"
                    link
                    size="small"
                    @click="showMentorApplicationDialog = true"
                  >
                    申请导师
                  </el-button>
                </div>
                
                <div 
                  v-if="teamInfo.mentor" 
                  class="mentor-profile-mini clickable"
                  @click="handleViewMentorDetail"
                >
                  <el-avatar :src="teamInfo.mentor.avatar" :size="40">
                    {{ teamInfo.mentor.name?.charAt(0) }}
                  </el-avatar>
                  <div class="m-details">
                    <span class="m-name">{{ teamInfo.mentor.name }}</span>
                    <span class="m-type">认证指导老师</span>
                  </div>
                </div>
                <div v-else class="mentor-none">
                  <p>暂无指导老师</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 加入申请（仅队长可见） -->
          <div v-if="isLeader" class="join-requests-card glass-card">
            <div class="section-header align-center">
              <h2>加入申请</h2>
              <el-select v-model="joinStatusFilter" size="small" class="status-select" @change="loadJoinApplications">
                <el-option label="待处理" value="PENDING" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已拒绝" value="REJECTED" />
              </el-select>
            </div>
            
            <div class="join-list-container">
              <el-skeleton v-if="joinLoading" :rows="2" animated />
              <div v-else-if="joinApplications.length === 0" class="empty-join-mini">
                <p>暂无申请记录</p>
              </div>
              <div v-else class="join-mini-items">
                <div v-for="app in joinApplications" :key="app.id" class="join-mini-item">
                  <div class="app-main">
                    <div class="app-header">
                      <span class="app-id">用户 ID: {{ app.userId }}</span>
                      <el-tag :type="joinStatusTagType(app.status)" size="small" effect="plain">{{ joinStatusText(app.status) }}</el-tag>
                    </div>
                    <p class="app-reason" :title="app.reason">{{ app.reason || '未填写理由' }}</p>
                    <span class="app-date">{{ formatTime(app.appliedAt) }}</span>
                  </div>
                  <div class="app-actions" v-if="app.status === 'PENDING'">
                    <el-button type="success" icon="Check" circle size="small" @click="handleApproveJoin(app.id)" />
                    <el-button type="danger" icon="Close" circle size="small" @click="handleRejectJoin(app.id)" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Mentor Application Dialog -->
      <el-dialog
        v-model="showMentorApplicationDialog"
        title="申请指导老师"
        width="540px"
        class="custom-dialog"
        @close="resetMentorApplicationForm"
      >
        <el-form
          :model="mentorApplicationForm"
          :rules="mentorApplicationRules"
          ref="mentorApplicationFormRef"
          label-position="top"
        >
          <el-form-item label="选择导师" prop="mentorId">
            <el-select
              v-model="mentorApplicationForm.mentorId"
              filterable
              remote
              :remote-method="searchMentors"
              :loading="searchingMentors"
              placeholder="请选择或搜索导师名称、工号或邮箱"
              style="width: 100%"
              @focus="loadAllMentors"
            >
              <el-option
                v-for="user in mentorSearchResults"
                :key="user.id"
                :label="`${user.username} (${user.userCode})`"
                :value="user.id"
              >
                <div class="mentor-search-option">
                  <span class="name">{{ user.username }}</span>
                  <span class="code">{{ user.userCode }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="申请理由" prop="reason">
            <el-input
              v-model="mentorApplicationForm.reason"
              type="textarea"
              :rows="4"
              placeholder="诚恳地说明申请该老师指导的理由，提高通过率"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showMentorApplicationDialog = false">取消</el-button>
            <el-button type="primary" @click="handleSubmitMentorApplication" :loading="submittingMentorApplication">
              确认提交
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 添加比赛对话框 -->
      <el-dialog
        v-model="showAddCompetitionDialog"
        title="添加关联比赛"
        width="540px"
        class="custom-dialog"
      >
        <el-form label-position="top">
          <el-form-item label="选择比赛">
            <el-select
              v-model="selectedCompetitionId"
              filterable
              remote
              :remote-method="searchCompetitions"
              :loading="searchingCompetitions"
              placeholder="请选择或搜索比赛名称"
              style="width: 100%"
              @focus="loadAllCompetitions"
            >
              <el-option
                v-for="comp in competitionSearchResults"
                :key="comp.id"
                :label="comp.name"
                :value="comp.id"
              >
                <div class="comp-search-option">
                  <span class="name">{{ comp.name }}</span>
                  <el-tag :type="getCompStatusType(comp.status)" size="small" effect="plain">
                    {{ getCompStatusText(comp.status) }}
                  </el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showAddCompetitionDialog = false">取消</el-button>
            <el-button type="primary" @click="handleAddCompetition" :loading="addingCompetition">
              确认添加
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 成员详情对话框 -->
      <el-dialog
        v-model="showMemberDetailDialog"
        width="800px"
        :show-close="false"
        destroy-on-close
        class="mentor-detail-dialog"
      >
        <div v-if="selectedMember" class="mentor-detail">
          <button class="close-btn" @click="showMemberDetailDialog = false" aria-label="关闭">
            <el-icon><Close /></el-icon>
          </button>

          <div class="mentor-card-top">
            <div class="mentor-avatar-section">
              <el-avatar :size="100" :src="selectedMember.avatarUrl || selectedMember.avatar" class="mentor-avatar-large">
                {{ selectedMember.realName?.charAt(0) || selectedMember.username?.charAt(0) }}
              </el-avatar>
              <div class="mentor-rating-badge" style="background: var(--accent-color);">
                <el-icon><Medal /></el-icon>
                <span>{{ selectedMember.creditScore || 0 }}</span>
              </div>
            </div>
            
            <div class="mentor-info-section">
              <h2 class="mentor-name-large">{{ selectedMember.realName || selectedMember.username }}</h2>
              <p class="mentor-dept-large">{{ selectedMember.department }} · {{ selectedMember.major }}</p>
            </div>
          </div>

          <div class="mentor-content-area">
            <div class="content-block">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><UserIcon /></el-icon>
                  <span>个人简介</span>
                </div>
              </div>
              <div class="block-content">
                <p class="text-content">{{ selectedMember.bio || '这位同学还没有填写个人简介。' }}</p>
              </div>
            </div>

            <div class="content-block">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><MagicStick /></el-icon>
                  <span>技能专长</span>
                </div>
                <span class="skill-count" v-if="!loadingMemberSkills && memberSkills.length > 0">
                  {{ memberSkills.length }} 项技能
                </span>
              </div>
              <div class="block-content">
                <div v-if="loadingMemberSkills" class="skills-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>加载技能标签中...</span>
                </div>
                
                <div v-else-if="memberSkills.length > 0" class="skills-showcase">
                  <div 
                    v-for="(skill, index) in memberSkills" 
                    :key="index" 
                    class="skill-chip"
                    :data-level="skill.proficiencyLevel"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                  >
                    <i class="level-dot"></i>
                    <span class="skill-chip-text">{{ skill.skillName }}</span>
                  </div>
                </div>
                
                <div v-else class="no-skills">
                  <el-icon><InfoFilled /></el-icon>
                  <span>该同学还未添加技能标签</span>
                </div>
              </div>
            </div>

            <div class="content-block">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><Connection /></el-icon>
                  <span>兴趣领域</span>
                </div>
                <span class="skill-count" v-if="!loadingMemberOtherTags && memberInterests.length > 0">
                  {{ memberInterests.length }} 个兴趣
                </span>
              </div>
              <div class="block-content">
                <div v-if="loadingMemberOtherTags" class="skills-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>加载标签中...</span>
                </div>
                
                <div v-else-if="memberInterests.length > 0" class="skills-showcase">
                  <div 
                    v-for="(tag, index) in memberInterests" 
                    :key="index" 
                    class="skill-chip interest-chip"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                  >
                    <span class="skill-chip-text">{{ tag.tagName }}</span>
                  </div>
                </div>
                
                <div v-else class="no-skills">
                  <el-icon><InfoFilled /></el-icon>
                  <span>该同学还未添加兴趣标签</span>
                </div>
              </div>
            </div>

            <div class="content-block">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><MagicStick /></el-icon>
                  <span>个人特质</span>
                </div>
                <span class="skill-count" v-if="!loadingMemberOtherTags && memberPersonalities.length > 0">
                  {{ memberPersonalities.length }} 项特质
                </span>
              </div>
              <div class="block-content">
                <div v-if="loadingMemberOtherTags" class="skills-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>加载标签中...</span>
                </div>
                
                <div v-else-if="memberPersonalities.length > 0" class="skills-showcase">
                  <div 
                    v-for="(tag, index) in memberPersonalities" 
                    :key="index" 
                    class="skill-chip personality-chip"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                  >
                    <span class="skill-chip-text">{{ tag.tagName }}</span>
                  </div>
                </div>
                
                <div v-else class="no-skills">
                  <el-icon><InfoFilled /></el-icon>
                  <span>该同学还未添加个人特质标签</span>
                </div>
              </div>
            </div>

            <div class="content-block">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><Star /></el-icon>
                  <span>偏好类型</span>
                </div>
                <span class="skill-count" v-if="!loadingMemberOtherTags && memberProjectTypes.length > 0">
                  {{ memberProjectTypes.length }} 种类型
                </span>
              </div>
              <div class="block-content">
                <div v-if="loadingMemberOtherTags" class="skills-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>加载标签中...</span>
                </div>
                
                <div v-else-if="memberProjectTypes.length > 0" class="skills-showcase">
                  <div 
                    v-for="(tag, index) in memberProjectTypes" 
                    :key="index" 
                    class="skill-chip type-chip"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                  >
                    <span class="skill-chip-text">{{ tag.tagName }}</span>
                  </div>
                </div>
                
                <div v-else class="no-skills">
                  <el-icon><InfoFilled /></el-icon>
                  <span>该同学还未添加偏好类型标签</span>
                </div>
              </div>
            </div>

            <div class="content-block" v-if="selectedMember.intentions && selectedMember.intentions.length > 0">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><Connection /></el-icon>
                  <span>组队意向</span>
                </div>
              </div>
              <div class="block-content">
                <div class="tc-intentions">
                  <el-tag v-if="selectedMember.intentions.includes('JOIN_PROJECT')" type="primary" size="large">
                    寻找项目
                  </el-tag>
                  <el-tag v-if="selectedMember.intentions.includes('FIND_TEAMMATES')" type="success" size="large">
                    寻找队友
                  </el-tag>
                  <el-tag v-if="selectedMember.intentions.includes('FIND_MENTOR')" type="warning" size="large">
                    寻找导师
                  </el-tag>
                  <el-tag v-if="selectedMember.intentions.includes('HELP_NEWBIE')" type="info" size="large">
                    帮助新手
                  </el-tag>
                </div>
                
                <div v-if="selectedMember.weeklyHours" class="tc-time-info" style="margin-top: 16px;">
                  <el-icon class="tc-time-icon"><Clock /></el-icon>
                  <span>每周可投入 {{ selectedMember.weeklyHours }} 小时</span>
                </div>
                
                <div v-if="selectedMember.notes" class="text-content" style="margin-top: 16px;">
                  <strong>补充说明：</strong>{{ selectedMember.notes }}
                </div>
              </div>
            </div>

            <div class="content-block" v-if="selectedMember.projectExperience">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><Trophy /></el-icon>
                  <span>项目经验</span>
                </div>
              </div>
              <div class="block-content">
                <div class="markdown-wrapper">
                  <MarkdownViewer :content="selectedMember.projectExperience || '暂无项目经验展示'" />
                </div>
              </div>
            </div>

            <div class="content-block" v-if="canViewContactInfo">
              <div class="block-header">
                <div class="block-title">
                  <el-icon class="block-icon"><Phone /></el-icon>
                  <span>联系方式</span>
                </div>
              </div>
              <div class="block-content">
                <div class="contact-info-grid">
                  <div class="contact-item" v-if="selectedMember.wechat">
                    <span class="contact-label">微信：</span>
                    <span class="contact-value">{{ selectedMember.wechat }}</span>
                  </div>
                  <div class="contact-item" v-if="selectedMember.qq">
                    <span class="contact-label">QQ：</span>
                    <span class="contact-value">{{ selectedMember.qq }}</span>
                  </div>
                  <div class="contact-item" v-if="selectedMember.email">
                    <span class="contact-label">邮箱：</span>
                    <span class="contact-value">{{ selectedMember.email }}</span>
                  </div>
                  <div class="contact-item" v-if="selectedMember.phone">
                    <span class="contact-label">电话：</span>
                    <span class="contact-value">{{ selectedMember.phone }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="mentor-actions">
            <el-button size="large" @click="showMemberDetailDialog = false">返回列表</el-button>
          </div>
        </div>
      </el-dialog>

      <!-- 导师详情对话框 - 使用可复用组件 -->
      <MentorDetailDialog
        v-model="showMentorDetailDialog"
        :mentor-detail="selectedMentorDetail"
        close-button-text="关闭"
      />

      <!-- 添加成员对话框 -->
      <el-dialog
        v-model="showAddMemberDialog"
        title="添加团队成员"
        width="540px"
        class="custom-dialog"
      >
        <el-form label-position="top">
          <el-form-item label="选择成员">
            <el-select
              v-model="selectedUserId"
              filterable
              remote
              :remote-method="searchUsersForAdd"
              :loading="searchingUsers"
              placeholder="请输入用户名、学号或邮箱搜索"
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="user in userSearchResults"
                :key="user.id"
                :label="`${user.username} (${user.userCode || user.email})`"
                :value="user.id"
                :disabled="isMemberAlready(user.id)"
              >
                <div class="user-search-option">
                  <span class="name">{{ user.username }}</span>
                  <span class="code">{{ user.userCode || user.email }}</span>
                  <el-tag v-if="isMemberAlready(user.id)" size="small" type="info">已加入</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showAddMemberDialog = false">取消</el-button>
            <el-button type="primary" @click="handleAddMember" :loading="addingMember" :disabled="!selectedUserId">
              确认添加
            </el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>


<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  TrendCharts,
  Calendar,
  ChatDotRound,
  Document,
  Picture,
  Camera,
  Trophy,
  Edit,
  Avatar,
  Check,
  Close,
  Plus,
  Star,
  User as UserIcon,
  MagicStick,
  Connection,
  Clock,
  Medal,
  Loading,
  InfoFilled,
  Phone,
  Rank
} from '@element-plus/icons-vue'
import { getTeam, getTeamMembers, getTeamStatistics, getTeamActivities, uploadTeamAvatar, addTeamMember, removeTeamMember } from '@api/team'
import { getTeamJoinApplications, reviewTeamJoinApplication } from '@api/team'
import { createMentorApplication, getCompetitions, getCompetitionDetail } from '@api/competition'
import { searchUsers, getUserProfile, getUserSkills, getUserAvailabilityById } from '@api/user'
import { getMentorPlaza } from '@api/mentor'
import { request } from '@utils/request'
import { useAuthStore } from '@store/auth'
import { ElMessageBox } from 'element-plus'
import type { Team, TeamMember, TeamActivity } from '@types/team'
import type { TeamJoinApplication } from '@types/team'
import type { User } from '@api/user'
import type { UserSkill } from '@/types/user'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'
import MentorDetailDialog from '@/components/mentor/MentorDetailDialog.vue'

const props = defineProps<{
  teamId?: number
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const teamId = computed(() => props.teamId || Number(route.params.id))

const loading = ref(true)
const error = ref<string | null>(null)
const teamInfo = ref<Partial<Team>>({})
const members = ref<TeamMember[]>([])
const memberOrder = ref<number[]>([]) // 存储成员顺序（userId数组）
const draggedIndex = ref<number | null>(null)
const avatarUploading = ref(false)
const editingDescription = ref(false)
const descriptionEdit = ref('')
const savingDescription = ref(false)
const statistics = reactive({
  taskCompletionRate: 0,
  activeDays: 0,
  messageCount: 0,
  fileCount: 0
})
const activities = ref<TeamActivity[]>([])

const isMember = computed(() => {
  const userId = authStore.user?.id
  return !!userId && members.value.some(member => member.userId === userId)
})

const isLeader = computed(() => {
  const userId = authStore.user?.id
  if (!userId) return false
  const member = members.value.find(m => m.userId === userId)
  return member?.role === 'LEADER' || member?.role === 'OWNER' || member?.role === 'ADMIN'
})

// 排序后的成员列表：队长固定第一，其他按自定义顺序
const sortedMembers = computed(() => {
  if (members.value.length === 0) return []
  
  // 找出队长
  const leader = members.value.find(m => m.role === 'LEADER' || m.role === 'OWNER')
  const otherMembers = members.value.filter(m => m.role !== 'LEADER' && m.role !== 'OWNER')
  
  // 如果有自定义顺序，按顺序排列
  if (memberOrder.value.length > 0) {
    const orderedMembers = otherMembers.sort((a, b) => {
      const indexA = memberOrder.value.indexOf(a.userId)
      const indexB = memberOrder.value.indexOf(b.userId)
      
      // 如果都在顺序列表中，按顺序排列
      if (indexA !== -1 && indexB !== -1) {
        return indexA - indexB
      }
      // 如果只有一个在列表中，在列表中的排前面
      if (indexA !== -1) return -1
      if (indexB !== -1) return 1
      // 都不在列表中，保持原顺序
      return 0
    })
    
    return leader ? [leader, ...orderedMembers] : orderedMembers
  }
  
  // 没有自定义顺序，保持原顺序
  return leader ? [leader, ...otherMembers] : otherMembers
})

// Join Applications
const joinLoading = ref(false)
const joinApplications = ref<TeamJoinApplication[]>([])
const joinStatusFilter = ref('PENDING')

const loadJoinApplications = async () => {
  if (!isLeader.value || !teamId.value) return
  joinLoading.value = true
  try {
    const res: any = await getTeamJoinApplications(teamId.value, { page: 1, size: 20, status: joinStatusFilter.value })
    joinApplications.value = (res?.data || res)?.records || []
  } catch (e: any) {
    console.warn('Failed to load join applications:', e)
    joinApplications.value = []
  } finally {
    joinLoading.value = false
  }
}

const handleApproveJoin = async (applicationId: number) => {
  try {
    await reviewTeamJoinApplication(applicationId, true)
    ElMessage.success('已通过该申请')
    loadJoinApplications()
    loadOverviewData() // Refresh members
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const handleRejectJoin = async (applicationId: number) => {
  try {
    await reviewTeamJoinApplication(applicationId, false)
    ElMessage.success('已拒绝该申请')
    loadJoinApplications()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const joinStatusText = (status: string) =>
  ({ PENDING: '待处理', APPROVED: '已通过', REJECTED: '已拒绝', WITHDRAWN: '已撤回' } as any)[status] || status

const joinStatusTagType = (status: string): 'warning' | 'success' | 'danger' | 'info' =>
  ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', WITHDRAWN: 'info' } as any)[status] || 'info'

// Mentor Application
const showMentorApplicationDialog = ref(false)
const submittingMentorApplication = ref(false)
const searchingMentors = ref(false)
const mentorApplicationFormRef = ref<FormInstance>()
const mentorApplicationForm = reactive({
  mentorId: undefined as number | undefined,
  reason: ''
})
const mentorSearchResults = ref<User[]>([])

const mentorApplicationRules: FormRules = {
  mentorId: [{ required: true, message: '请选择导师', trigger: 'change' }]
}

const searchMentors = async (keyword: string) => {
  if (!keyword || keyword.trim().length < 2) {
    // 如果关键词为空或太短，加载所有导师
    await loadAllMentors()
    return
  }
  searchingMentors.value = true
  try {
    mentorSearchResults.value = await searchUsers(keyword.trim(), 10)
  } catch (error: any) {
    console.error('Failed to search mentors:', error)
    mentorSearchResults.value = []
  } finally {
    searchingMentors.value = false
  }
}

// 加载所有导师（用于下拉选择）
const loadAllMentors = async () => {
  // 如果已经有数据，不重复加载
  if (mentorSearchResults.value.length > 0) return
  
  searchingMentors.value = true
  try {
    // 使用导师广场接口获取导师列表（学员可见）
    const res = await getMentorPlaza({ page: 1, size: 50 })
    
    // 转换数据格式以匹配 User 接口
    if (res && typeof res === 'object') {
      if ('records' in res && Array.isArray(res.records)) {
        mentorSearchResults.value = res.records.map((mentor: any) => ({
          id: mentor.id,
          username: mentor.username,
          userCode: mentor.userCode || mentor.email || `ID:${mentor.id}`,
          email: mentor.email || '',
          status: 'ACTIVE',
          roles: ['MENTOR'],
          createdAt: '',
          updatedAt: ''
        }))
      } else if (Array.isArray(res)) {
        mentorSearchResults.value = res.map((mentor: any) => ({
          id: mentor.id,
          username: mentor.username,
          userCode: mentor.userCode || mentor.email || `ID:${mentor.id}`,
          email: mentor.email || '',
          status: 'ACTIVE',
          roles: ['MENTOR'],
          createdAt: '',
          updatedAt: ''
        }))
      }
    }
  } catch (error: any) {
    console.error('Failed to load mentors:', error)
    mentorSearchResults.value = []
  } finally {
    searchingMentors.value = false
  }
}

const handleAvatarChange = async (file: any) => {
  if (!file?.raw || !teamId.value) return
  const raw = file.raw as File
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  if (raw.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }

  avatarUploading.value = true
  try {
    const res = await uploadTeamAvatar(teamId.value, raw)
    if (res?.url) teamInfo.value.avatar = res.url
    ElMessage.success('团队头像已更新')
  } catch (error: any) {
    ElMessage.error(error?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

// 关联比赛相关
const relatedCompetitions = ref<any[]>([])
const showAddCompetitionDialog = ref(false)
const selectedCompetitionId = ref<number>()
const competitionSearchResults = ref<any[]>([])
const searchingCompetitions = ref(false)
const addingCompetition = ref(false)

// 成员详情对话框
const showMemberDetailDialog = ref(false)
const selectedMember = ref<any>(null)
const memberSkills = ref<UserSkill[]>([])
const memberInterests = ref<any[]>([])
const memberPersonalities = ref<any[]>([])
const memberProjectTypes = ref<any[]>([])
const loadingMemberSkills = ref(false)
const loadingMemberOtherTags = ref(false)

// 导师详情对话框
const showMentorDetailDialog = ref(false)
const selectedMentorDetail = ref<any>(null)

// 添加成员对话框
const showAddMemberDialog = ref(false)
const selectedUserId = ref<number>()
const userSearchResults = ref<User[]>([])
const searchingUsers = ref(false)
const addingMember = ref(false)

const canViewContactInfo = computed(() => {
  if (!selectedMember.value) return false
  return !!(
    selectedMember.value.wechat ||
    selectedMember.value.qq ||
    selectedMember.value.email ||
    selectedMember.value.phone
  )
})

const handleViewCompetitionDetail = (competitionId: number) => {
  router.push({ name: 'CompetitionDetail', params: { id: competitionId } })
}

const searchCompetitions = async (keyword: string) => {
  if (!keyword || keyword.trim().length < 2) {
    // 如果关键词为空或太短，加载所有比赛
    await loadAllCompetitions()
    return
  }
  searchingCompetitions.value = true
  try {
    const res = await getCompetitions({ keyword: keyword.trim(), page: 1, size: 10 })
    competitionSearchResults.value = res.records || []
  } catch (error: any) {
    console.error('Failed to search competitions:', error)
    competitionSearchResults.value = []
  } finally {
    searchingCompetitions.value = false
  }
}

// 加载所有比赛（用于下拉选择）
const loadAllCompetitions = async () => {
  // 如果已经有数据，不重复加载
  if (competitionSearchResults.value.length > 0) return
  
  searchingCompetitions.value = true
  try {
    const res = await getCompetitions({ page: 1, size: 50 })
    competitionSearchResults.value = res.records || []
  } catch (error: any) {
    console.error('Failed to load competitions:', error)
    competitionSearchResults.value = []
  } finally {
    searchingCompetitions.value = false
  }
}

const handleAddCompetition = async () => {
  if (!selectedCompetitionId.value || !teamId.value) return
  
  addingCompetition.value = true
  try {
    // 调用后端API关联比赛
    await request.post(`/teams/${teamId.value}/competitions/${selectedCompetitionId.value}`)
    ElMessage.success('比赛关联成功')
    showAddCompetitionDialog.value = false
    selectedCompetitionId.value = undefined
    loadRelatedCompetitions()
  } catch (error: any) {
    ElMessage.error(error?.message || '关联比赛失败')
  } finally {
    addingCompetition.value = false
  }
}

const handleRemoveCompetition = async (competitionId: number) => {
  if (!teamId.value) return
  
  try {
    await ElMessageBox.confirm('确定要移除该比赛关联吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(`/teams/${teamId.value}/competitions/${competitionId}`)
    ElMessage.success('已移除比赛关联')
    loadRelatedCompetitions()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '移除失败')
    }
  }
}

const loadRelatedCompetitions = async () => {
  if (!teamId.value) return
  try {
    const res: any = await request.get(`/teams/${teamId.value}/competitions`)
    relatedCompetitions.value = res?.data || res || []
  } catch (error) {
    console.warn('Failed to load related competitions:', error)
    relatedCompetitions.value = []
  }
}

const handleViewMemberDetail = async (member: TeamMember) => {
  try {
    // 获取用户完整资料（包括bio、projectExperience等）
    const profile = await getUserProfile(member.userId)
    
    // 获取用户的组队意向数据（intentions、weeklyHours、notes等）
    let availability = null
    try {
      availability = await getUserAvailabilityById(member.userId)
    } catch (error) {
      console.warn('获取用户组队意向失败:', error)
    }
    
    // 合并所有数据
    selectedMember.value = {
      ...profile,
      ...member,
      // 确保使用profile中的完整数据
      id: member.userId,
      userId: member.userId,
      realName: profile.realName || member.username,
      avatarUrl: profile.avatarUrl || member.avatar,
      // 添加availability数据
      intentions: availability?.intentions || [],
      weeklyHours: availability?.weeklyHours,
      notes: availability?.notes
    }
    
    showMemberDetailDialog.value = true
    
    // 获取成员的技能标签
    memberSkills.value = []
    loadingMemberSkills.value = true
    try {
      const res = await getUserSkills(member.userId)
      if (res) {
        memberSkills.value = res
      }
    } catch (error) {
      console.error('获取成员技能失败:', error)
    } finally {
      loadingMemberSkills.value = false
    }

    // 获取成员的其他标签
    memberInterests.value = []
    memberPersonalities.value = []
    memberProjectTypes.value = []
    loadingMemberOtherTags.value = true
    try {
      // 使用统一的用户标签接口
      const allTags = await request.get(`/user-tags/user/${member.userId}`)
      
      // 按类型分组标签
      memberInterests.value = allTags.filter((tag: any) => tag.tagType === 'INTEREST')
      memberPersonalities.value = allTags.filter((tag: any) => tag.tagType === 'PERSONALITY')
      memberProjectTypes.value = allTags.filter((tag: any) => tag.tagType === 'PROJECT_TYPE')
      
      console.log('TeamOverview - Loaded member tags:', {
        interests: memberInterests.value,
        personalities: memberPersonalities.value,
        projectTypes: memberProjectTypes.value
      })
    } catch (error) {
      console.error('获取成员其他标签失败:', error)
    } finally {
      loadingMemberOtherTags.value = false
    }
  } catch (error: any) {
    console.error('Failed to load member detail:', error)
    ElMessage.error('加载成员详情失败')
  }
}

// 查看导师详情
const handleViewMentorDetail = async () => {
  if (!teamInfo.value.mentorId) return
  
  try {
    // 获取导师完整资料
    const profile = await getUserProfile(teamInfo.value.mentorId)
    console.log('TeamOverview - profile:', profile)
    selectedMentorDetail.value = profile
    showMentorDetailDialog.value = true
  } catch (error) {
    console.error('加载导师详情失败:', error)
    ElMessage.error('加载导师详情失败')
  }
}

const getCompStatusType = (status: string) => {
  const map: Record<string, any> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ONGOING: 'warning',
    ENDED: 'info',
    ARCHIVED: 'info'
  }
  return map[status] || 'info'
}

const getCompStatusText = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ONGOING: '进行中',
    ENDED: '已结束',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

const handleSubmitMentorApplication = async () => {
  if (!mentorApplicationFormRef.value) return
  await mentorApplicationFormRef.value.validate(async (valid) => {
    if (!valid || !mentorApplicationForm.mentorId || !teamId.value) return
    submittingMentorApplication.value = true
    try {
      await createMentorApplication(teamId.value, {
        mentorId: mentorApplicationForm.mentorId,
        reason: mentorApplicationForm.reason || undefined
      })
      ElMessage.success('导师申请已提交')
      showMentorApplicationDialog.value = false
      resetMentorApplicationForm()
    } catch (error: any) {
      ElMessage.error(error.message || '提交申请失败')
    } finally {
      submittingMentorApplication.value = false
    }
  })
}

const resetMentorApplicationForm = () => {
  mentorApplicationForm.mentorId = undefined
  mentorApplicationForm.reason = ''
  mentorApplicationFormRef.value?.clearValidate()
}

const loadOverviewData = async () => {
  if (!teamId.value || isNaN(teamId.value)) {
    error.value = '无效的团队 ID'
    loading.value = false
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    const [teamRes, membersRes, statisticsRes, activitiesRes] = await Promise.all([
      getTeam(teamId.value),
      getTeamMembers(teamId.value),
      getTeamStatistics(teamId.value),
      getTeamActivities(teamId.value, 10)
    ])
    
    teamInfo.value = teamRes
    members.value = membersRes
    activities.value = activitiesRes || []

    if (isLeader.value) loadJoinApplications()
    
    // 加载关联的比赛列表
    loadRelatedCompetitions()
    
    if (statisticsRes) {
      Object.assign(statistics, statisticsRes)
    }
    
    // 加载成员顺序
    loadMemberOrder()
  } catch (err: any) {
    error.value = err.message || '加载团队数据失败'
  } finally {
    loading.value = false
  }
}

const formatDate = (date: any) => date ? new Date(date).toLocaleDateString('zh-CN') : '-'
const formatTime = (date: any) => date ? new Date(date).toLocaleString('zh-CN', { hour12: false }) : '-'

const startEditDescription = () => {
  descriptionEdit.value = teamInfo.value.description || ''
  editingDescription.value = true
}

const cancelEditDescription = () => {
  editingDescription.value = false
}

const saveDescription = async () => {
  if (!teamId.value) return
  savingDescription.value = true
  try {
    const response: any = await request.put(`/teams/${teamId.value}`, {
      description: descriptionEdit.value
    })
    const updatedTeam = response?.data || response
    teamInfo.value.description = updatedTeam.description || descriptionEdit.value
    editingDescription.value = false
    ElMessage.success('描述已更新')
  } catch (error: any) {
    ElMessage.error(error?.message || '更新描述失败')
  } finally {
    savingDescription.value = false
  }
}

const getRoleLabel = (role: string) => {
  const map: Record<string, string> = { OWNER: '创建者', ADMIN: '管理员', MEMBER: '成员', LEADER: '队长' }
  return map[role] || role
}

const getActivityColor = (type: string) => {
  const map: Record<string, string> = { task: '#67c23a', file: '#409eff', message: '#e6a23c', member: '#f56c6c' }
  return map[type] || '#909399'
}

// 判断成员是否是自己或队长（不能移除）
const isSelfOrLeader = (member: TeamMember) => {
  const userId = authStore.user?.id
  return member.userId === userId || member.role === 'LEADER' || member.role === 'OWNER'
}

// 判断用户是否已经是成员
const isMemberAlready = (userId: number) => {
  return members.value.some(m => m.userId === userId)
}

// 搜索用户（用于添加成员）
const searchUsersForAdd = async (keyword: string) => {
  if (!keyword || keyword.trim().length < 2) {
    userSearchResults.value = []
    return
  }
  searchingUsers.value = true
  try {
    userSearchResults.value = await searchUsers(keyword.trim(), 10)
  } catch (error: any) {
    console.error('Failed to search users:', error)
    userSearchResults.value = []
  } finally {
    searchingUsers.value = false
  }
}

// 添加成员
const handleAddMember = async () => {
  if (!selectedUserId.value || !teamId.value) return
  
  // 检查是否已经是成员
  if (isMemberAlready(selectedUserId.value)) {
    ElMessage.warning('该用户已经是团队成员')
    return
  }
  
  addingMember.value = true
  try {
    await addTeamMember(teamId.value, selectedUserId.value)
    ElMessage.success('成员添加成功')
    showAddMemberDialog.value = false
    selectedUserId.value = undefined
    userSearchResults.value = []
    // 重新加载成员列表
    await loadOverviewData()
  } catch (error: any) {
    ElMessage.error(error?.message || '添加成员失败')
  } finally {
    addingMember.value = false
  }
}

// 移除成员
const handleRemoveMember = async (member: TeamMember) => {
  if (!teamId.value) return
  
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.username} 移出团队吗？`,
      '移除成员',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await removeTeamMember(teamId.value, member.userId)
    ElMessage.success('成员已移除')
    // 从顺序列表中移除
    memberOrder.value = memberOrder.value.filter(id => id !== member.userId)
    saveMemberOrder()
    // 重新加载成员列表
    await loadOverviewData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '移除成员失败')
    }
  }
}

// 拖拽相关函数
const handleDragStart = (event: DragEvent, index: number) => {
  draggedIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

const handleDragOver = (event: DragEvent, index: number) => {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const handleDrop = (event: DragEvent, dropIndex: number) => {
  event.preventDefault()
  
  if (draggedIndex.value === null || draggedIndex.value === dropIndex) return
  
  // 不能拖拽到队长位置（索引0）
  if (dropIndex === 0) return
  
  const newMembers = [...sortedMembers.value]
  const draggedMember = newMembers[draggedIndex.value]
  
  // 移除拖拽的元素
  newMembers.splice(draggedIndex.value, 1)
  // 插入到新位置
  newMembers.splice(dropIndex, 0, draggedMember)
  
  // 更新顺序（排除队长）
  memberOrder.value = newMembers
    .filter(m => m.role !== 'LEADER' && m.role !== 'OWNER')
    .map(m => m.userId)
  
  // 保存顺序到 localStorage
  saveMemberOrder()
}

const handleDragEnd = () => {
  draggedIndex.value = null
}

// 保存成员顺序到 localStorage
const saveMemberOrder = () => {
  if (!teamId.value) return
  localStorage.setItem(`team_${teamId.value}_member_order`, JSON.stringify(memberOrder.value))
}

// 加载成员顺序
const loadMemberOrder = () => {
  if (!teamId.value) return
  const saved = localStorage.getItem(`team_${teamId.value}_member_order`)
  if (saved) {
    try {
      memberOrder.value = JSON.parse(saved)
    } catch (error) {
      console.error('Failed to parse member order:', error)
      memberOrder.value = []
    }
  }
}

onMounted(() => loadOverviewData())
</script>

<style scoped lang="scss">
.team-overview {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  // 移除 min-height，使用父容器的滚动
}

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: sticky;
  top: 24px;
}

/* Glass Card Base */
.glass-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

/* Section Headers */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: nowrap;
  margin-bottom: 20px;

  &.align-center {
    align-items: center;
  }

  h2 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0;

    span {
      font-size: 14px;
      color: var(--text-color-muted);
      font-weight: normal;
      margin-left: 4px;
    }
  }
}

/* Info Card Style */
.info-card {
  .team-header {
    display: flex;
    gap: 28px;
    align-items: flex-start;
  }

  .team-avatar-wrapper {
    width: 140px;
    height: 140px;
    border-radius: 24px;
    overflow: hidden;
    position: relative;
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
    flex-shrink: 0;

    .team-avatar-img {
      width: 100%;
      height: 100%;
    }

    .image-error {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, var(--accent-color), var(--accent-color-dark));
      color: white;
      font-size: 48px;
    }

    .avatar-overlay {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.5);
      color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s ease;
      cursor: pointer;
      font-size: 12px;
      gap: 4px;

      .el-icon {
        font-size: 24px;
      }
    }

    &:hover .avatar-overlay {
      opacity: 1;
    }
  }

  .team-meta {
    flex: 1;
    min-width: 0;

    .title-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      h1 {
        margin: 0;
        font-size: 28px;
        font-weight: 700;
        color: var(--text-color);
      }
    }

    .description-section {
      margin-bottom: 20px;

      .description-wrapper {
        display: flex;
        align-items: flex-start;
        gap: 12px;

        .description {
          font-size: 15px;
          line-height: 1.6;
          color: var(--text-color-muted);
          margin: 0;
          flex: 1;

          &.empty {
            font-style: italic;
            opacity: 0.6;
          }
        }
      }

      .description-edit-wrapper {
        .edit-actions {
          display: flex;
          justify-content: flex-end;
          gap: 8px;
          margin-top: 8px;
        }
      }
    }

    .quick-stats-row {
      display: flex;
      align-items: center;
      gap: 24px;
      padding-top: 16px;
      border-top: 1px solid var(--border-card);

      .q-stat {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .q-label {
          font-size: 12px;
          color: var(--text-color-secondary);
        }
        .q-value {
          font-size: 16px;
          font-weight: 600;
          color: var(--text-color);

          small {
            font-size: 13px;
            color: var(--text-color-muted);
            font-weight: normal;
          }
        }
      }

      .v-divider {
        width: 1px;
        height: 24px;
        background: var(--border-card);
      }
    }
  }
}

/* Members Grid */
.members-list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 20px;

  .member-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px;
    border-radius: 12px;
    transition: all 0.2s ease;
    cursor: pointer;
    position: relative;

    &:hover {
      background: var(--bg-card-hover);
      transform: translateY(-2px);
    }

    &.has-actions {
      padding-bottom: 40px;
    }

    &.is-leader {
      border: 2px solid var(--accent-color);
      background: linear-gradient(135deg, rgba(var(--accent-color-rgb), 0.05), rgba(var(--accent-color-rgb), 0.02));
    }

    &.draggable {
      cursor: move;
      
      &:hover .drag-handle {
        opacity: 1;
      }
    }

    &:active {
      cursor: grabbing;
    }

    .member-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      flex: 1;
    }

    .member-avatar-container {
      position: relative;
      width: 56px;
      height: 56px;
      border-radius: 50%;
      overflow: hidden;
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);

      .el-image {
        width: 100%;
        height: 100%;
      }

      .image-error {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #eef2f7;
        color: var(--accent-color);
        font-weight: 600;
        font-size: 20px;
      }

      .role-dot {
        position: absolute;
        right: 2px;
        bottom: 2px;
        width: 12px;
        height: 12px;
        border-radius: 50%;
        border: 2px solid white;
        background: #94a3b8;

        &.LEADER, &.OWNER {
          background: #f43f5e;
        }
        &.ADMIN {
          background: #3b82f6;
        }
      }
    }

    .member-info-mini {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      text-align: center;

      .name {
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color);
        max-width: 100px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .role-tag {
        font-size: 11px;
        padding: 0 6px;
        height: 18px;
        line-height: 16px;
      }
    }

    .member-actions {
      position: absolute;
      bottom: 8px;
      left: 50%;
      transform: translateX(-50%);
      opacity: 0;
      transition: opacity 0.2s ease;
    }

    .drag-handle {
      position: absolute;
      top: 8px;
      right: 8px;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-card);
      border-radius: 6px;
      color: var(--text-color-secondary);
      opacity: 0;
      transition: all 0.2s ease;
      cursor: grab;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

      &:hover {
        background: var(--accent-color);
        color: white;
      }

      &:active {
        cursor: grabbing;
      }
    }

    &:hover .member-actions,
    &:hover .drag-handle {
      opacity: 1;
    }
  }
}

/* Activity & Timeline */
.timeline-wrapper {
  padding: 8px 12px;

  :deep(.el-timeline-item__content) {
    padding-left: 12px;
  }

  .activity-item-content {
    .user-name {
      font-weight: 600;
      color: var(--text-color);
      margin-right: 8px;
    }
    .action-detail {
      color: var(--text-color-muted);
      font-size: 14px;
    }
  }
}

/* Stats Box Side */
.stats-panel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  .stat-box {
    padding: 20px 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 12px;

    .stat-icon-wrapper {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: white;
    }

    &.purple .stat-icon-wrapper { background: linear-gradient(135deg, #a855f7, #7e22ce); }
    &.blue .stat-icon-wrapper { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
    &.orange .stat-icon-wrapper { background: linear-gradient(135deg, #f97316, #c2410c); }
    &.green .stat-icon-wrapper { background: linear-gradient(135deg, #10b981, #047857); }

    .stat-data {
      h3 {
        margin: 0;
        font-size: 22px;
        font-weight: 700;
        color: var(--text-color);
      }
      p {
        margin: 0;
        font-size: 12px;
        color: var(--text-color-muted);
      }
    }
  }
}

/* Side Cards Content */
.comp-info-content, .join-list-container {
  .comp-row, .mentor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .label {
    font-size: 13px;
    color: var(--text-color-secondary);
  }

  .comp-link {
    font-size: 14px;
    font-weight: 600;
  }

  .mentor-section {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px dashed var(--border-card);
  }

  .mentor-profile-mini {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: var(--bg-card-hover);
    border-radius: 12px;
    transition: all 0.3s ease;

    &.clickable {
      cursor: pointer;
      
      &:hover {
        background: var(--bg-card-active);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }
    }

    .m-details {
      display: flex;
      flex-direction: column;
      
      .m-name {
        font-size: 14px;
        font-weight: 600;
      }
      .m-type {
        font-size: 11px;
        color: var(--text-color-muted);
      }
    }
  }
}

.join-requests-card {
  .section-header {
    margin-bottom: 16px;
    align-items: center;
    display: flex;
    justify-content: space-between;
    
    h2 {
      white-space: nowrap;
      margin: 0;
      flex: 1;
    }

    .status-select {
      width: 100px !important;
      flex-shrink: 0;
    }
  }

  .empty-join-mini {
    padding: 12px 0;
    text-align: center;
    color: var(--text-color-muted);
    font-size: 13px;
    background: var(--bg-card-hover);
    border-radius: 8px;
    border: 1px dashed var(--border-card);
    margin-top: 8px;
  }
}

/* Join Requests Mini */
.join-mini-items {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .join-mini-item {
    display: flex;
    gap: 12px;
    padding: 12px;
    background: var(--bg-card-hover);
    border-radius: 12px;
    align-items: flex-start;

    .app-main {
      flex: 1;
      min-width: 0;

      .app-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 6px;

        .app-id {
          font-size: 12px;
          font-weight: 600;
        }
      }

      .app-reason {
        font-size: 13px;
        color: var(--text-color-muted);
        margin: 0 0 6px 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .app-date {
        font-size: 11px;
        color: var(--text-color-secondary);
      }
    }

    .app-actions {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
  }
}

/* 比赛列表样式 */
.competition-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;

  .competition-item {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 12px;
    background: var(--bg-card-hover);
    border-radius: 12px;
    transition: all 0.2s ease;

    &:hover {
      background: var(--bg-hover);
      transform: translateX(2px);
    }

    .comp-main {
      flex: 1;
      min-width: 0;

      .comp-name {
        font-size: 14px;
        font-weight: 600;
        display: block;
        margin-bottom: 8px;
      }

      .comp-meta {
        display: flex;
        align-items: center;
        gap: 8px;

        .comp-date {
          font-size: 12px;
          color: var(--text-color-secondary);
        }
      }
    }
  }
}

.empty-competitions {
  padding: 20px;
  text-align: center;
  color: var(--text-color-muted);
  font-size: 13px;
  background: var(--bg-card-hover);
  border-radius: 8px;
  border: 1px dashed var(--border-card);
  margin-bottom: 20px;
}

.comp-search-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .name {
    flex: 1;
    font-size: 14px;
  }
}

/* 成员卡片可点击样式 */
.members-list-grid .member-item {
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: var(--bg-card-hover);
    transform: translateY(-2px);
  }
}

/* 用户搜索选项样式 */
.user-search-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  .name {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
  }

  .code {
    font-size: 12px;
    color: var(--text-color-secondary);
  }
}

/* ==================== 成员详情对话框样式（与人才墙完全一致）==================== */
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
    .text-content { 
      font-size: 15px; 
      line-height: 1.8; 
      color: var(--text-color-muted); 
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

  .markdown-wrapper {
    :deep(.markdown-body) {
      font-size: 15px;
      line-height: 1.8;
      color: var(--text-color-muted);
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

/* ==================== 组队意向和联系方式样式 ==================== */
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
    }
    
    .contact-value {
      color: var(--text-color-muted);
      flex: 1;
      word-break: break-all;
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .side-column {
    position: static;
  }
  .stats-panel-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .info-card .team-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .stats-panel-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
