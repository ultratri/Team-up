<template>
  <div class="team-manage">
    <el-card class="header-card">
      <div class="header-content">
        <h2>团队管理</h2>
        <div class="stats">
          <el-statistic title="总团队数" :value="statistics.totalTeams || 0" />
          <el-statistic title="活跃团队" :value="statistics.activeTeams || 0" />
          <el-statistic 
            title="平均成员数" 
            :value="statistics.averageMemberCount || 0" 
            :precision="1"
          />
          <el-statistic title="总项目数" :value="statistics.totalProjects || 0" />
        </div>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="团队类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 150px">
            <el-option label="临时团队" value="TEMPORARY" />
            <el-option label="长期团队" value="PERMANENT" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="filters.isActive" placeholder="全部" clearable style="width: 150px">
            <el-option label="活跃" :value="true" />
            <el-option label="已解散" :value="false" />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索团队名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div class="table-header">
        <h3>团队列表</h3>
        <el-button
          type="danger"
          :disabled="selectedTeams.length === 0"
          @click="handleBatchDissolve"
        >
          <el-icon><Delete /></el-icon>
          批量解散
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="teams"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column prop="name" label="团队名称" min-width="150" />
        
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'PERMANENT' ? 'success' : 'info'">
              {{ row.type === 'PERMANENT' ? '长期' : '临时' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="leaderName" label="团队领导" width="120" />
        
        <el-table-column prop="memberCount" label="成员数" width="100" align="center" />
        
        <el-table-column prop="projectCount" label="项目数" width="100" align="center" />
        
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? '活跃' : '已解散' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">
              查看
            </el-button>
            <el-button
              v-if="row.isActive"
              link
              type="danger"
              @click="handleDissolve(row)"
            >
              解散
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 团队详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="团队详情"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="currentTeam" v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="团队名称">
            {{ currentTeam.name }}
          </el-descriptions-item>
          <el-descriptions-item label="团队类型">
            <el-tag :type="currentTeam.type === 'PERMANENT' ? 'success' : 'info'">
              {{ currentTeam.type === 'PERMANENT' ? '长期团队' : '临时团队' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="团队领导">
            {{ currentTeam.leader?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="领导邮箱">
            {{ currentTeam.leader?.email }}
          </el-descriptions-item>
          <el-descriptions-item label="院系">
            {{ currentTeam.leader?.department }}
          </el-descriptions-item>
          <el-descriptions-item label="专业">
            {{ currentTeam.leader?.major }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(currentTeam.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentTeam.isActive ? 'success' : 'danger'">
              {{ currentTeam.isActive ? '活跃' : '已解散' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="团队描述" :span="2">
            {{ currentTeam.description || '暂无描述' }}
          </el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top: 20px">团队成员 ({{ currentTeam.members?.length || 0 }})</h3>
        <el-table :data="currentTeam.members" style="margin-top: 10px">
          <el-table-column prop="userName" label="姓名" />
          <el-table-column prop="role" label="角色">
            <template #default="{ row }">
              <el-tag v-if="row.role === 'LEADER'" type="warning">领导者</el-tag>
              <el-tag v-else>成员</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="joinedAt" label="加入时间">
            <template #default="{ row }">
              {{ formatDate(row.joinedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                v-if="row.role !== 'LEADER'"
                link
                type="danger"
                @click="handleRemoveMember(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <h3 style="margin-top: 20px">团队项目 ({{ currentTeam.projects?.length || 0 }})</h3>
        <el-table :data="currentTeam.projects" style="margin-top: 10px">
          <el-table-column prop="name" label="项目名称" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
              <el-tag v-else-if="row.status === 'IN_PROGRESS'" type="primary">进行中</el-tag>
              <el-tag v-else type="info">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" label="开始时间">
            <template #default="{ row }">
              {{ formatDate(row.startedAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="completedAt" label="完成时间">
            <template #default="{ row }">
              {{ row.completedAt ? formatDate(row.completedAt) : '-' }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentTeam?.isActive"
          type="danger"
          @click="handleDissolveFromDetail"
        >
          解散团队
        </el-button>
      </template>
    </el-dialog>

    <!-- 解散团队对话框 -->
    <el-dialog
      v-model="dissolveDialogVisible"
      title="解散团队"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="dissolveForm" label-width="100px">
        <el-form-item label="团队名称">
          <span>{{ dissolveTeam?.name }}</span>
        </el-form-item>
        <el-form-item label="解散原因" required>
          <el-input
            v-model="dissolveForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入解散原因，将通知所有团队成员"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dissolveDialogVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="dissolving"
          @click="confirmDissolve"
        >
          确认解散
        </el-button>
      </template>
    </el-dialog>

    <!-- 移除成员对话框 -->
    <el-dialog
      v-model="removeMemberDialogVisible"
      title="移除团队成员"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="removeMemberForm" label-width="100px">
        <el-form-item label="成员姓名">
          <span>{{ removeMember?.userName }}</span>
        </el-form-item>
        <el-form-item label="移除原因">
          <el-input
            v-model="removeMemberForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入移除原因（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="removeMemberDialogVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="removing"
          @click="confirmRemoveMember"
        >
          确认移除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import {
  getAdminTeamList,
  getAdminTeamDetail,
  dissolveTeam as dissolveTeamApi,
  adminRemoveTeamMember,
  batchDissolveTeams,
  getAdminTeamStatistics
} from '@/api/team'

const statistics = ref({
  totalTeams: 0,
  activeTeams: 0,
  averageMemberCount: 0,
  totalProjects: 0
})

const filters = reactive({
  type: '',
  isActive: undefined as boolean | undefined,
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const teams = ref<any[]>([])
const loading = ref(false)
const selectedTeams = ref<any[]>([])

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const currentTeam = ref<any>(null)

const dissolveDialogVisible = ref(false)
const dissolveTeam = ref<any>(null)
const dissolveForm = reactive({
  reason: ''
})
const dissolving = ref(false)

const removeMemberDialogVisible = ref(false)
const removeMember = ref<any>(null)
const removeMemberForm = reactive({
  reason: ''
})
const removing = ref(false)

const loadStatistics = async () => {
  try {
    const data = await getAdminTeamStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadTeams = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      type: filters.type || undefined,
      isActive: filters.isActive,
      keyword: filters.keyword || undefined
    }
    const result = await getAdminTeamList(params)
    teams.value = result.records || []
    pagination.total = result.total || 0
  } catch (error) {
    console.error('加载团队列表失败:', error)
    ElMessage.error('加载团队列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadTeams()
}

const handleReset = () => {
  filters.type = ''
  filters.isActive = undefined
  filters.keyword = ''
  pagination.page = 1
  loadTeams()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadTeams()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadTeams()
}

const handleSelectionChange = (selection: any[]) => {
  selectedTeams.value = selection
}

const handleViewDetail = async (team: any) => {
  currentTeam.value = null
  detailDialogVisible.value = true
  detailLoading.value = true
  
  try {
    const detail = await getAdminTeamDetail(team.id)
    currentTeam.value = detail
  } catch (error) {
    console.error('加载团队详情失败:', error)
    ElMessage.error('加载团队详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleDissolve = (team: any) => {
  dissolveTeam.value = team
  dissolveForm.reason = ''
  dissolveDialogVisible.value = true
}

const handleDissolveFromDetail = () => {
  dissolveTeam.value = currentTeam.value
  dissolveForm.reason = ''
  detailDialogVisible.value = false
  dissolveDialogVisible.value = true
}

const confirmDissolve = async () => {
  if (!dissolveForm.reason.trim()) {
    ElMessage.warning('请输入解散原因')
    return
  }

  dissolving.value = true
  try {
    await dissolveTeamApi(dissolveTeam.value.id, dissolveForm.reason)
    ElMessage.success('团队已解散')
    dissolveDialogVisible.value = false
    loadTeams()
    loadStatistics()
  } catch (error) {
    console.error('解散团队失败:', error)
    ElMessage.error('解散团队失败')
  } finally {
    dissolving.value = false
  }
}

const handleBatchDissolve = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要解散选中的 ${selectedTeams.value.length} 个团队吗？此操作不可撤销。`,
      '批量解散团队',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const teamIds = selectedTeams.value.map(t => t.id)
    await batchDissolveTeams(teamIds)
    ElMessage.success('批量解散成功')
    loadTeams()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量解散失败:', error)
      ElMessage.error('批量解散失败')
    }
  }
}

const handleRemoveMember = (member: any) => {
  removeMember.value = member
  removeMemberForm.reason = ''
  removeMemberDialogVisible.value = true
}

const confirmRemoveMember = async () => {
  removing.value = true
  try {
    await adminRemoveTeamMember(
      currentTeam.value.id,
      removeMember.value.userId,
      removeMemberForm.reason
    )
    ElMessage.success('成员已移除')
    removeMemberDialogVisible.value = false
    const detail = await getAdminTeamDetail(currentTeam.value.id)
    currentTeam.value = detail
  } catch (error) {
    console.error('移除成员失败:', error)
    ElMessage.error('移除成员失败')
  } finally {
    removing.value = false
  }
}

const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadStatistics()
  loadTeams()
})
</script>

<style scoped lang="scss">
.team-manage {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

    .header-content {
      h2 {
        margin: 0 0 20px 0;
        font-size: 24px;
        font-weight: 600;
      }

      .stats {
        display: flex;
        gap: 40px;
      }
    }
  }

  .filter-card {
    margin-bottom: 20px;

    .filter-form {
      margin-bottom: 0;
    }
  }

  .table-card {
    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      h3 {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
      }
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
