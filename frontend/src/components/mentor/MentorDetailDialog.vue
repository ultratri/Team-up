<template>
  <el-dialog
    v-model="visible"
    width="800px"
    :show-close="false"
    destroy-on-close
    class="mentor-detail-dialog"
    @close="handleClose"
  >
    <div v-if="mentorDetail" class="mentor-detail">
      <!-- 自定义关闭按钮 -->
      <button class="close-btn" @click="handleClose" aria-label="关闭">
        <el-icon><Close /></el-icon>
      </button>

      <!-- 顶部卡片 -->
      <div class="mentor-card-top">
        <div class="mentor-avatar-section">
          <el-avatar :size="100" :src="mentorDetail.avatarUrl || mentorDetail.avatar" class="mentor-avatar-large">
            {{ (mentorDetail.realName || mentorDetail.username || '导师').charAt(0) }}
          </el-avatar>
          <div class="mentor-rating-badge" style="background: var(--accent-color);">
            <el-icon><Medal /></el-icon>
            <span>{{ mentorDetail.rating?.toFixed(1) || '0.0' }}</span>
          </div>
        </div>
        
        <div class="mentor-info-section">
          <h2 class="mentor-name-large">{{ mentorDetail.realName }}</h2>
          <p class="mentor-dept-large">{{ mentorDetail.department }} · {{ mentorDetail.major }}</p>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="mentor-content-area">
        <!-- 个人简介 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><User /></el-icon>
              <span>个人简介</span>
            </div>
          </div>
          <div class="block-content">
            <p class="text-content">{{ mentorDetail.bio || '这位导师还没有填写个人简介。' }}</p>
          </div>
        </div>

        <!-- 技能标签 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><MagicStick /></el-icon>
              <span>技能专长</span>
            </div>
            <span class="skill-count" v-if="!loadingSkills && allMentorSkills.length > 0">
              {{ allMentorSkills.length }} 项技能
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingSkills" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载技能标签中...</span>
            </div>
            
            <!-- 技能列表 -->
            <div v-else-if="allMentorSkills.length > 0" class="skills-showcase">
              <div 
                v-for="(skill, index) in skills" 
                :key="index" 
                class="skill-chip"
                :data-level="skill.proficiencyLevel"
                :style="{ animationDelay: `${index * 0.05}s` }"
              >
                <i class="level-dot"></i>
                <span class="skill-chip-text">{{ skill.skillName || skill.tagName }}</span>
              </div>
            </div>
            
            <!-- 无技能 -->
            <div v-else class="no-skills">
              <el-icon><InfoFilled /></el-icon>
              <span>该导师还未添加技能标签</span>
            </div>
          </div>
        </div>

        <!-- 兴趣领域 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Connection /></el-icon>
              <span>兴趣领域</span>
            </div>
            <span class="skill-count" v-if="!loadingOtherTags && interests.length > 0">
              {{ interests.length }} 个兴趣
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingOtherTags" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载标签中...</span>
            </div>
            
            <!-- 兴趣列表 -->
            <div v-else-if="interests.length > 0" class="skills-showcase">
              <div 
                v-for="(tag, index) in interests" 
                :key="index" 
                class="skill-chip interest-chip"
                :style="{ animationDelay: `${index * 0.05}s` }"
              >
                <span class="skill-chip-text">{{ tag.tagName }}</span>
              </div>
            </div>
            
            <!-- 无兴趣 -->
            <div v-else class="no-skills">
              <el-icon><InfoFilled /></el-icon>
              <span>该导师还未添加兴趣标签</span>
            </div>
          </div>
        </div>

        <!-- 个人特质 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><MagicStick /></el-icon>
              <span>个人特质</span>
            </div>
            <span class="skill-count" v-if="!loadingOtherTags && personalities.length > 0">
              {{ personalities.length }} 项特质
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingOtherTags" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载标签中...</span>
            </div>
            
            <!-- 特质列表 -->
            <div v-else-if="personalities.length > 0" class="skills-showcase">
              <div 
                v-for="(tag, index) in personalities" 
                :key="index" 
                class="skill-chip personality-chip"
                :style="{ animationDelay: `${index * 0.05}s` }"
              >
                <span class="skill-chip-text">{{ tag.tagName }}</span>
              </div>
            </div>
            
            <!-- 无特质 -->
            <div v-else class="no-skills">
              <el-icon><InfoFilled /></el-icon>
              <span>该导师还未添加个人特质标签</span>
            </div>
          </div>
        </div>

        <!-- 偏好类型 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Star /></el-icon>
              <span>偏好类型</span>
            </div>
            <span class="skill-count" v-if="!loadingOtherTags && projectTypes.length > 0">
              {{ projectTypes.length }} 种类型
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingOtherTags" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载标签中...</span>
            </div>
            
            <!-- 类型列表 -->
            <div v-else-if="projectTypes.length > 0" class="skills-showcase">
              <div 
                v-for="(tag, index) in projectTypes" 
                :key="index" 
                class="skill-chip type-chip"
                :style="{ animationDelay: `${index * 0.05}s` }"
              >
                <span class="skill-chip-text">{{ tag.tagName }}</span>
              </div>
            </div>
            
            <!-- 无类型 -->
            <div v-else class="no-skills">
              <el-icon><InfoFilled /></el-icon>
              <span>该导师还未添加偏好类型标签</span>
            </div>
          </div>
        </div>

        <!-- 项目经验 -->
        <div class="content-block" v-if="mentorDetail.projectExperience">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Trophy /></el-icon>
              <span>项目经验</span>
            </div>
          </div>
          <div class="block-content">
            <div class="markdown-wrapper">
              <MarkdownViewer
                :content="mentorDetail.projectExperience"
              />
            </div>
          </div>
        </div>

        <!-- 指导成就 -->
        <div class="content-block" v-if="mentorDetail.guidanceExperience">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Medal /></el-icon>
              <span>指导成就</span>
            </div>
          </div>
          <div class="block-content">
            <div class="markdown-wrapper">
              <MarkdownViewer
                :content="mentorDetail.guidanceExperience"
              />
            </div>
          </div>
        </div>

        <!-- 联系方式 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Phone /></el-icon>
              <span>联系方式</span>
            </div>
          </div>
          <div class="block-content">
            <div class="contact-info-grid">
              <div class="contact-item" v-if="mentorDetail.wechat">
                <span class="contact-label">微信：</span>
                <span class="contact-value">{{ mentorDetail.wechat }}</span>
              </div>
              <div class="contact-item" v-if="mentorDetail.qq">
                <span class="contact-label">QQ：</span>
                <span class="contact-value">{{ mentorDetail.qq }}</span>
              </div>
              <div class="contact-item" v-if="mentorDetail.email">
                <span class="contact-label">邮箱：</span>
                <span class="contact-value">{{ mentorDetail.email }}</span>
              </div>
              <div class="contact-item" v-if="mentorDetail.phone">
                <span class="contact-label">电话：</span>
                <span class="contact-value">{{ mentorDetail.phone }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="mentor-actions">
        <el-button size="large" @click="handleClose">
          {{ closeButtonText }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Close, Star, User, MagicStick, Trophy, Medal, Connection, Loading, InfoFilled, Phone } from '@element-plus/icons-vue'
import { getUserSkills } from '@/api/profile'
import { request } from '@/utils/request'
import type { UserSkill } from '@/types/user'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

interface MentorDetail {
  id: number
  userId?: number
  realName: string
  avatar?: string
  avatarUrl?: string
  department: string
  major: string
  rating?: number
  totalMentees?: number
  activeMentees?: number
  bio?: string
  projectExperience?: string
  guidanceExperience?: string
  wechat?: string
  qq?: string
  email?: string
  phone?: string
}

interface Props {
  modelValue: boolean
  mentorDetail: MentorDetail | null
  closeButtonText?: string
}

const props = withDefaults(defineProps<Props>(), {
  closeButtonText: '返回广场'
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)
const loadingSkills = ref(false)
const loadingOtherTags = ref(false)
const skills = ref<UserSkill[]>([])
const interests = ref<any[]>([])
const personalities = ref<any[]>([])
const projectTypes = ref<any[]>([])

// 获取导师的实际 ID
const mentorId = computed(() => {
  return props.mentorDetail?.userId || props.mentorDetail?.id || (props.mentorDetail as any)?.mentorId
})

const allMentorSkills = computed(() => {
  return skills.value.map(skill => skill.skillName || skill.tagName).filter(Boolean)
})

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
  if (newVal && props.mentorDetail) {
    loadMentorTags()
  }
})

watch(visible, (newVal) => {
  emit('update:modelValue', newVal)
})

const handleClose = () => {
  visible.value = false
}

const loadMentorTags = async () => {
  if (!mentorId.value) return

  try {
    // 加载技能标签
    loadingSkills.value = true
    const skillsData = await getUserSkills(mentorId.value)
    skills.value = Array.isArray(skillsData) ? skillsData : []
  } catch (error) {
    console.error('Failed to load mentor skills:', error)
    skills.value = []
  } finally {
    loadingSkills.value = false
  }

  try {
    // 加载其他标签
    loadingOtherTags.value = true
    const tagsRes = await request.get(`/user-tags/user/${mentorId.value}`)
    // request.get 返回的直接就是数据，不需要访问 .data
    const allTags = Array.isArray(tagsRes) ? tagsRes : (tagsRes.data || [])
    
    interests.value = allTags.filter((tag: any) => tag.tagType === 'INTEREST')
    personalities.value = allTags.filter((tag: any) => tag.tagType === 'PERSONALITY')
    projectTypes.value = allTags.filter((tag: any) => tag.tagType === 'PROJECT_TYPE')
  } catch (error) {
    console.error('Failed to load mentor tags:', error)
    interests.value = []
    personalities.value = []
    projectTypes.value = []
  } finally {
    loadingOtherTags.value = false
  }
}
</script>

<style scoped lang="scss">
/* ==================== 导师详情对话框 - 重构样式 ==================== */
.mentor-detail-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
  background: var(--bg-card);
  padding: 0;
}

.mentor-detail-dialog :deep(.el-dialog__header) { 
  display: none; 
}

.mentor-detail-dialog :deep(.el-dialog__body) { 
  padding: 0; 
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
    &:hover { background: rgba(0,0,0,0.2); }
  }

  .mentor-card-top {
    padding: 40px;
    background: linear-gradient(135deg, rgba(var(--accent-color-rgb), 0.08), rgba(var(--accent-color-rgb), 0.03));
    display: flex;
    gap: 32px;
    align-items: center;
    border-bottom: 1px solid var(--mp-border);
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

  .mentor-avatar-section { position: relative; }

  .mentor-name-large { font-size: 36px; font-weight: 900; margin: 0 0 8px; color: var(--text-color); }
  .mentor-dept-large { font-size: 16px; color: var(--text-color-muted); margin: 0 0 20px; }

  .mentor-badges {
    display: flex;
    gap: 10px;
    margin-bottom: 24px;
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 100px;
      font-size: 13px;
      font-weight: 600;
      &.badge-success { background: rgba(16, 185, 129, 0.1); color: #10b981; }
      &.badge-primary { background: rgba(var(--mp-accent-rgb), 0.1); color: var(--mp-accent); }
    }
  }

  .mentor-stats-inline {
    display: flex;
    gap: 32px;
    .stat-inline-item {
      display: flex;
      flex-direction: column;
      .stat-inline-value { font-size: 24px; font-weight: 900; color: var(--text-color); }
      .stat-inline-label { font-size: 12px; color: var(--text-color-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    }
    .stat-inline-divider { width: 1px; height: 40px; background: var(--mp-border); }
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
        .block-icon { color: var(--mp-accent); }
      }
    }
    .text-content { font-size: 15px; line-height: 1.8; color: var(--text-color-muted); }
  }

  .skill-legend {
    display: flex;
    gap: 16px;
    align-items: center;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-color-muted);

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
      }

      &.expert .dot { background: #10b981; }
      &.advanced .dot { background: var(--mp-accent); }
      &.intermediate .dot { background: #94a3b8; }
    }
  }

  .skill-count {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-color-muted);
  }

  .skills-showcase {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    .skill-chip {
      padding: 6px 16px;
      border-radius: 100px;
      background: var(--bg-body);
      border: 1px solid var(--mp-border);
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
      
      /* 熟练度等级颜色点 */
      .level-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #94a3b8;
        flex-shrink: 0;
      }
      
      /* 精通 - 绿色 */
      &[data-level="EXPERT"] {
        .level-dot { background: #10b981; }
        border-color: rgba(16, 185, 129, 0.3);
        &:hover { 
          border-color: #10b981; 
          color: #10b981; 
        }
      }
      
      /* 高级 - 主题色 */
      &[data-level="ADVANCED"] {
        .level-dot { background: var(--mp-accent); }
        border-color: rgba(var(--mp-accent-rgb), 0.3);
        &:hover { 
          border-color: var(--mp-accent); 
          color: var(--mp-accent); 
        }
      }
      
      /* 熟练 - 灰色 */
      &[data-level="INTERMEDIATE"] {
        .level-dot { background: #94a3b8; }
        border-color: rgba(148, 163, 184, 0.3);
        &:hover { 
          border-color: #94a3b8; 
          color: #94a3b8; 
        }
      }
      
      /* 入门 - 浅灰色 */
      &[data-level="BEGINNER"] {
        .level-dot { background: #cbd5e1; }
        border-color: rgba(203, 213, 225, 0.3);
        &:hover { 
          border-color: #cbd5e1; 
          color: #cbd5e1; 
        }
      }
      
      /* 不同类型标签的颜色 */
      &.interest-chip {
        border-color: rgba(16, 185, 129, 0.3);
        background: rgba(16, 185, 129, 0.05);
        &:hover { 
          border-color: #10b981; 
          color: #10b981; 
          background: rgba(16, 185, 129, 0.1);
        }
      }
      
      &.personality-chip {
        border-color: rgba(139, 92, 246, 0.3);
        background: rgba(139, 92, 246, 0.05);
        &:hover { 
          border-color: #8b5cf6; 
          color: #8b5cf6; 
          background: rgba(139, 92, 246, 0.1);
        }
      }
      
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

  .skills-loading {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 20px;
    color: var(--text-color-muted);
    font-size: 14px;
    justify-content: center;
  }

  .no-skills {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 20px;
    color: var(--text-color-muted);
    font-size: 14px;
    justify-content: center;
    background: var(--bg-body);
    border-radius: 8px;
  }

  .mentor-actions {
    padding: 24px 40px;
    display: flex;
    gap: 16px;
    border-top: 1px solid var(--mp-border);
    background: var(--bg-card);
    .el-button { flex: 1; height: 52px; border-radius: 14px; font-weight: 700; font-size: 16px; }
  }
}

[data-theme='dark'] .mentor-detail {
  .mentor-card-top { background: linear-gradient(135deg, rgba(255,255,255,0.03), rgba(255,255,255,0.01)); }
  .close-btn { background: rgba(255,255,255,0.05); }
}

// 联系方式样式
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
    min-width: 0;
    
    .contact-label {
      font-weight: 600;
      color: var(--text-color);
      margin-right: 8px;
      flex-shrink: 0;
    }
    
    .contact-value {
      color: var(--text-color-muted);
      flex: 1;
      min-width: 0;
      word-break: break-all;
      overflow-wrap: break-word;
    }
  }
}
</style>
