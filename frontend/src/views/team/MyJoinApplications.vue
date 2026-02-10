<template>
  <div class="my-join-applications">
    <div class="page-header">
      <h1>我的入队申请</h1>
      <p class="subtitle">查看和管理我提交的队伍加入申请</p>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="searchParams" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="searchParams.status" placeholder="全部" clearable @change="loadApplications">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadApplications">
            <el-icon><Search /></el-icon>
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 申请列表 -->
    <div v-else-if="applications.length > 0" class="applications-list">
      <el-card
        v-for="app in applications"
        :key="app.id"
        class="application-card"
        shadow="hover"
      >
        <div class="application-header">
          <div class="application-info">
            <h3>{{ app.team?.name || `队伍 #${app.teamId}` }}</h3>
            <div class="meta-info">
              <el-tag :type="getStatusTagType(app.status)">
                {{ getStatusText(app.status) }}
              </el-tag>
              <span class="time">{{ formatDateTime(app.appliedAt) }}</span>
            </div>
          </div>
          <div v-if="app.status === 'PENDING'" class="actions">
            <el-button type="danger" size="small" @click="handleWithdraw(app)">
              撤回申请
            </el-button>
          </div>
        </div>

        <el-divider />

        <div class="application-details">
          <div class="detail-item" v-if="app.competitionId">
            <span class="label">比赛：</span>
            <el-link
              type="primary"
              :href="`/competition/${app.competitionId}`"
              @click.prevent="handleViewCompetition(app.competitionId)"
            >
              {{ app.competition?.name || `比赛 #${app.competitionId}` }}
            </el-link>
          </div>
          <div class="detail-item">
            <span class="label">申请队伍：</span>
            <el-link
              type="primary"
              :href="`/team/${app.teamId}/overview`"
              @click.prevent="handleViewTeam(app.teamId)"
            >
              {{ app.team?.name || `队伍 #${app.teamId}` }}
            </el-link>
          </div>
          <div class="detail-item" v-if="app.reason">
            <span class="label">申请理由：</span>
            <span>{{ app.reason }}</span>
          </div>
          <div class="detail-item" v-if="app.reviewComment">
            <span class="label">审核意见：</span>
            <span>{{ app.reviewComment }}</span>
          </div>
          <div class="detail-item" v-if="app.reviewedAt">
            <span class="label">处理时间：</span>
            <span>{{ formatDateTime(app.reviewedAt) }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无入队申请" />

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="searchParams.page"
        v-model:page-size="searchParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadApplications"
        @current-change="loadApplications"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyTeamJoinApplications, withdrawTeamJoinApplication } from '@/api/team'
import type { TeamJoinApplication } from '@/types/team'

const router = useRouter()

const applications = ref<TeamJoinApplication[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  page: 1,
  size: 10,
  status: '' as string
})

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMyTeamJoinApplications({
      page: searchParams.page,
      size: searchParams.size,
      status: searchParams.status || undefined
    })
    applications.value = res.records
    total.value = res.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载申请列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleWithdraw = async (app: TeamJoinApplication) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤回对 ${app.team?.name || `队伍 #${app.teamId}`} 的申请吗？`,
      '确认撤回',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await withdrawTeamJoinApplication(app.id)
    ElMessage.success('已撤回申请')
    loadApplications()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤回申请失败')
    }
  }
}

const handleViewCompetition = (competitionId: number) => {
  router.push({ name: 'CompetitionDetail', params: { id: competitionId } })
}

const handleViewTeam = (teamId: number) => {
  router.push({ name: 'TeamOverview', params: { id: teamId } })
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const getStatusTagType = (status: string): 'warning' | 'success' | 'danger' => {
  const map: Record<string, 'warning' | 'success' | 'danger'> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'warning'
}

const formatDateTime = (date: string | Date | undefined) => {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped lang="scss">
.my-join-applications {
  padding: 20px;
  min-height: 100vh;
  background: var(--bg-body);

  .page-header {
    margin-bottom: 20px;

    h1 {
      font-size: 24px;
      font-weight: 600;
      color: var(--text-color);
      margin: 0 0 8px 0;
    }

    .subtitle {
      font-size: 14px;
      color: var(--text-color-muted);
      margin: 0;
    }
  }

  .filter-card {
    margin-bottom: 20px;
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
  }

  .filter-form {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .loading-container {
    margin-top: 20px;
  }

  .applications-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-top: 20px;
  }

  .application-card {
    background: var(--bg-elevated);
    border: 1px solid var(--border-subtle);
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .application-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      .application-info {
        flex: 1;

        h3 {
          font-size: 18px;
          font-weight: 600;
          color: var(--text-color);
          margin: 0 0 8px 0;
        }

        .meta-info {
          display: flex;
          align-items: center;
          gap: 12px;

          .time {
            font-size: 13px;
            color: var(--text-color-muted);
          }
        }
      }

      .actions {
        display: flex;
        gap: 8px;
      }
    }

    .application-details {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .detail-item {
        font-size: 14px;
        color: var(--text-color-secondary);

        .label {
          font-weight: 600;
          color: var(--text-color);
          margin-right: 8px;
        }
      }
    }
  }

  .pagination {
    margin-top: 30px;
    display: flex;
    justify-content: center;
  }
}
</style>
