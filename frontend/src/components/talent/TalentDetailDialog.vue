<template>
  <el-dialog
    v-model="visible"
    width="800px"
    :show-close="false"
    destroy-on-close
    class="mentor-detail-dialog"
  >
    <div v-if="talent" class="mentor-detail">
      <!-- 自定义关闭按钮 -->
      <button class="close-btn" @click="visible = false" aria-label="关闭">
        <el-icon><Close /></el-icon>
      </button>

      <!-- 顶部卡片 -->
      <div class="mentor-card-top">
        <div class="mentor-avatar-section">
          <el-avatar :size="100" :src="talent.avatarUrl" class="mentor-avatar-large">
            {{ (talent.realName || talent.username || '用户').charAt(0) }}
          </el-avatar>
          <div class="mentor-rating-badge" style="background: var(--accent-color);">
            <el-icon><Medal /></el-icon>
            <span>{{ talent.creditScore || 0 }}</span>
          </div>
        </div>
        
        <div class="mentor-info-section">
          <h2 class="mentor-name-large">{{ talent.realName || talent.username }}</h2>
          <p class="mentor-dept-large">{{ talent.department }} · {{ talent.major }}</p>
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
            <p class="text-content">{{ talent.bio || '这位同学还没有填写个人简介。' }}</p>
          </div>
        </div>

        <!-- 技能标签 -->
        <div class="content-block">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><MagicStick /></el-icon>
              <span>技能专长</span>
            </div>
            <span class="skill-count" v-if="!loadingTalentSkills && talentSkills.length > 0">
              {{ talentSkills.length }} 项技能
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingTalentSkills" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载技能标签中...</span>
            </div>
            
            <!-- 技能列表 -->
            <div v-else-if="talentSkills.length > 0" class="skills-showcase">
              <div 
                v-for="(skill, index) in talentSkills" 
                :key="index" 
                class="skill-chip"
                :data-level="skill.proficiencyLevel"
                :style="{ animationDelay: `${index * 0.05}s` }"
              >
                <i class="level-dot"></i>
                <span class="skill-chip-text">{{ skill.skillName }}</span>
              </div>
            </div>
            
            <!-- 无技能 -->
            <div v-else class="no-skills">
              <el-icon><InfoFilled /></el-icon>
              <span>该同学还未添加技能标签</span>
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
            <span class="skill-count" v-if="!loadingTalentOtherTags && talentInterests.length > 0">
              {{ talentInterests.length }} 个兴趣
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingTalentOtherTags" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载标签中...</span>
            </div>
            
            <!-- 兴趣列表 -->
            <div v-else-if="talentInterests.length > 0" class="skills-showcase">
              <div 
                v-for="(tag, index) in talentInterests" 
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
              <span>该同学还未添加兴趣标签</span>
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
            <span class="skill-count" v-if="!loadingTalentOtherTags && talentPersonalities.length > 0">
              {{ talentPersonalities.length }} 项特质
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingTalentOtherTags" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载标签中...</span>
            </div>
            
            <!-- 特质列表 -->
            <div v-else-if="talentPersonalities.length > 0" class="skills-showcase">
              <div 
                v-for="(tag, index) in talentPersonalities" 
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
              <span>该同学还未添加个人特质标签</span>
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
            <span class="skill-count" v-if="!loadingTalentOtherTags && talentProjectTypes.length > 0">
              {{ talentProjectTypes.length }} 种类型
            </span>
          </div>
          <div class="block-content">
            <!-- 加载中 -->
            <div v-if="loadingTalentOtherTags" class="skills-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载标签中...</span>
            </div>
            
            <!-- 类型列表 -->
            <div v-else-if="talentProjectTypes.length > 0" class="skills-showcase">
              <div 
                v-for="(tag, index) in talentProjectTypes" 
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
              <span>该同学还未添加偏好类型标签</span>
            </div>
          </div>
        </div>

        <!-- 组队意向和可用时间 -->
        <div class="content-block" v-if="talent.intentions && talent.intentions.length > 0">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Connection /></el-icon>
              <span>组队意向</span>
            </div>
          </div>
          <div class="block-content">
            <div class="tc-intentions">
              <el-tag 
                v-if="talent.intentions.includes('JOIN_PROJECT')" 
                type="primary" 
                size="large"
              >
                寻找项目
              </el-tag>
              <el-tag 
                v-if="talent.intentions.includes('FIND_TEAMMATES')" 
                type="success" 
                size="large"
              >
                寻找队友
              </el-tag>
              <el-tag 
                v-if="talent.intentions.includes('FIND_MENTOR')" 
                type="warning" 
                size="large"
              >
                寻找导师
              </el-tag>
              <el-tag 
                v-if="talent.intentions.includes('HELP_NEWBIE')" 
                type="info" 
                size="large"
              >
                帮助新手
              </el-tag>
            </div>
            
            <div v-if="talent.weeklyHours" class="tc-time-info" style="margin-top: 16px;">
              <el-icon class="tc-time-icon"><Clock /></el-icon>
              <span>每周可投入 {{ talent.weeklyHours }} 小时</span>
            </div>
            
            <div v-if="talent.notes" class="text-content" style="margin-top: 16px;">
              <strong>补充说明：</strong>{{ talent.notes }}
            </div>
          </div>
        </div>

        <!-- 项目经验 -->
        <div class="content-block" v-if="talent.projectExperience">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Trophy /></el-icon>
              <span>项目经验</span>
            </div>
          </div>
          <div class="block-content">
            <div class="markdown-wrapper">
              <MarkdownViewer
                :content="talent.projectExperience || '暂无项目经验展示'"
              />
            </div>
          </div>
        </div>

        <!-- 联系方式（根据权限显示） -->
        <div class="content-block" v-if="canViewContactInfo">
          <div class="block-header">
            <div class="block-title">
              <el-icon class="block-icon"><Phone /></el-icon>
              <span>联系方式</span>
            </div>
          </div>
          <div class="block-content">
            <div class="contact-info-grid">
              <div class="contact-item" v-if="talent.wechat">
                <span class="contact-label">微信：</span>
                <span class="contact-value">{{ talent.wechat }}</span>
              </div>
              <div class="contact-item" v-if="talent.qq">
                <span class="contact-label">QQ：</span>
                <span class="contact-value">{{ talent.qq }}</span>
              </div>
              <div class="contact-item" v-if="talent.email">
                <span class="contact-label">邮箱：</span>
                <span class="contact-value">{{ talent.email }}</span>
              </div>
              <div class="contact-item" v-if="talent.phone">
                <span class="contact-label">电话：</span>
                <span class="contact-value">{{ talent.phone }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="mentor-actions">
        <el-button size="large" @click="visible = false">
          返回列表
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Close, User, MagicStick, Trophy, Medal, Loading, InfoFilled, Connection, Clock, Phone, Star } from '@element-plus/icons-vue'
import { getUserProfile, getUserAvailabilityById, getUserSkills } from '@/api/user'
import { getUserCredit } from '@/api/profile'
import { request } from '@/utils/request'
import type { UserSkill } from '@/types/user'
import MarkdownViewer from '@/components/common/MarkdownViewer.vue'

interface TalentLike {
  userId?: number
  user_id?: number
  realName?: string
  username?: string
  avatarUrl?: string
  department?: string
  major?: string
  bio?: string
  creditScore?: number
  intentions?: string[]
  weeklyHours?: number
  notes?: string
  projectExperience?: string
  wechat?: string
  qq?: string
  email?: string
  phone?: string
}

interface Props {
  modelValue: boolean
  talent: TalentLike | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)
const loadingTalentSkills = ref(false)
const loadingTalentOtherTags = ref(false)
const talentSkills = ref<UserSkill[]>([])
const talentInterests = ref<any[]>([])
const talentPersonalities = ref<any[]>([])
const talentProjectTypes = ref<any[]>([])
const canViewContactInfo = ref(true)

// 在弹窗内部维护一份标准化的人才数据，确保不同页面打开时数据来源一致
const talent = ref<TalentLike | null>(null)

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val && props.talent) {
      // 以父组件传入的数据为基础，后续会用统一接口补全/覆盖字段
      talent.value = { ...props.talent }
      loadTalentDetail()
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadTalentDetail = async () => {
  if (!talent.value) return
  const base = talent.value
  const userId = base.userId || base.user_id
  if (!userId) return

  loadingTalentSkills.value = true
  loadingTalentOtherTags.value = true
  try {
    const [profile, availability, credit, skills, interestsRes, personalitiesRes, projectTypesRes] = await Promise.all([
      getUserProfile(userId).catch(() => null),
      getUserAvailabilityById(userId).catch(() => null),
      getUserCredit(userId).catch(() => null),
      getUserSkills(userId).catch(() => []),
      request.get(`/user-tags/${userId}/tags/INTEREST`).catch(() => []),
      request.get(`/user-tags/${userId}/tags/PERSONALITY`).catch(() => []),
      request.get(`/user-tags/${userId}/tags/PROJECT_TYPE`).catch(() => [])
    ])
    
    // 统一使用用户资料接口补全/覆盖字段，保证两个页面看到的数据一致
    if (profile && talent.value) {
      Object.assign(talent.value, {
        realName: profile.realName ?? talent.value.realName,
        username: profile.username ?? talent.value.username,
        avatarUrl: profile.avatarUrl ?? talent.value.avatarUrl,
        department: profile.department ?? talent.value.department,
        major: profile.major ?? talent.value.major,
        bio: profile.bio ?? talent.value.bio,
        projectExperience: profile.projectExperience ?? talent.value.projectExperience,
        email: profile.email ?? talent.value.email,
        phone: profile.phone ?? talent.value.phone,
        wechat: profile.wechat ?? talent.value.wechat,
        qq: profile.qq ?? talent.value.qq
      })
    }

    // 统一使用信誉接口的总分作为信用分
    if (credit && talent.value) {
      Object.assign(talent.value, {
        creditScore: credit.totalCredit ?? talent.value.creditScore
      })
    }

    // 统一使用组队意向接口补全/覆盖意向相关字段
    if (availability && talent.value) {
      Object.assign(talent.value, {
        intentions: availability.intentions ?? talent.value.intentions,
        weeklyHours: availability.weeklyHours ?? talent.value.weeklyHours,
        notes: availability.notes ?? talent.value.notes
      })
    }

    talentSkills.value = (skills || []) as UserSkill[]
    talentInterests.value = Array.isArray(interestsRes) ? interestsRes : (interestsRes.data || [])
    talentPersonalities.value = Array.isArray(personalitiesRes) ? personalitiesRes : (personalitiesRes.data || [])
    talentProjectTypes.value = Array.isArray(projectTypesRes) ? projectTypesRes : (projectTypesRes.data || [])
  } catch (error) {
    console.error('加载人才详情失败:', error)
  } finally {
    loadingTalentSkills.value = false
    loadingTalentOtherTags.value = false
  }
}
</script>

<style scoped lang="scss">
/* ==================== 人才详情对话框样式 ==================== */
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
      }
      
      &[data-level="BEGINNER"] {
        .level-dot { background: #cbd5e1; }
        border-color: rgba(203, 213, 225, 0.3);
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

@media (max-width: 768px) {
  .mentor-detail {
    .mentor-card-top {
      flex-direction: column;
      text-align: center;
    }
    
    .mentor-content-area {
      padding: 24px 20px;
    }
    
    .mentor-actions {
      padding: 20px;
    }
  }
}
</style>

