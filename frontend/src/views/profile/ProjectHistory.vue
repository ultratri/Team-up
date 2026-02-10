<template>
  <div class="project-history">
    <el-card>
      <template #header>
        <h3>项目履历墙</h3>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="project in projects"
          :key="project.id"
          :timestamp="project.createdAt"
          placement="top"
        >
          <el-card class="project-card">
            <div class="project-header">
              <div class="project-title">
                <h4>{{ project.title }}</h4>
                <el-tag :type="getStatusType(project.status)" size="small">
                  {{ getStatusText(project.status) }}
                </el-tag>
              </div>
              <el-tag size="small">{{ getProjectTypeText(project.projectType) }}</el-tag>
            </div>

            <div class="project-content">
              <p class="project-description">{{ project.description || '暂无描述' }}</p>

              <div class="project-meta">
                <span>
                  <el-icon><User /></el-icon>
                  团队规模: {{ project.currentMembers }}/{{ project.teamSize }}
                </span>
                <span v-if="project.startDate && project.endDate">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(project.startDate) }} - {{ formatDate(project.endDate) }}
                </span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-if="projects.length === 0" description="暂无项目履历" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Calendar } from '@element-plus/icons-vue'
import type { Project } from '../../types/project'

const projects = ref<Project[]>([])

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    DRAFT: 'info',
    RECRUITING: 'warning',
    IN_PROGRESS: 'primary',
    PENDING_REVIEW: 'warning',
    COMPLETED: 'success',
    ARCHIVED: 'info',
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    DRAFT: '构思中',
    RECRUITING: '招募中',
    IN_PROGRESS: '进行中',
    PENDING_REVIEW: '待审核',
    COMPLETED: '已完成',
    ARCHIVED: '已归档',
  }
  return texts[status] || status
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
  return dateStr ? new Date(dateStr).toLocaleDateString('zh-CN') : ''
}

const loadProjects = async () => {
  // TODO: 调用API获取用户的项目履历
  // const res = await getMyProjects()
  // projects.value = res.data
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped lang="scss">
.project-history {
  padding: 20px;

  .project-card {
    margin-bottom: 0;

    .project-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;

      .project-title {
        display: flex;
        align-items: center;
        gap: 12px;

        h4 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
        }
      }
    }

    .project-content {
      .project-description {
        color: #606266;
        font-size: 14px;
        margin-bottom: 12px;
        line-height: 1.6;
      }

      .project-meta {
        display: flex;
        gap: 20px;
        font-size: 13px;
        color: #909399;

        span {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }
}
</style>

