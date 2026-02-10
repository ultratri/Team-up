<template>
  <div class="audit-log">
    <div class="page-header">
      <div>
        <h1>操作日志</h1>
        <p class="subtitle">查看系统所有敏感操作的审计日志</p>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="资源类型">
          <el-select v-model="query.resourceType" placeholder="全部" clearable @change="reload" style="width: 200px">
            <el-option label="全部" value="" />
            <el-option label="文件" value="FILE" />
            <el-option label="文件夹" value="FOLDER" />
            <el-option label="团队成员" value="TEAM_MEMBER" />
            <el-option label="比赛" value="COMPETITION" />
            <el-option label="比赛模板" value="COMPETITION_TEMPLATE" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源ID">
          <el-input
            v-model.number="query.resourceId"
            placeholder="资源ID"
            clearable
            @clear="reload"
            @keyup.enter="reload"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="rows" v-loading="loading" style="width: 100%" @sort-change="handleSortChange">
        <el-table-column label="操作者" width="150" sortable="custom" prop="username">
          <template #default="{ row }">
            <div>
              <div>{{ row.username || '未知' }}</div>
              <div style="font-size: 12px; color: var(--text-color-muted)">ID: {{ row.userId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="180" sortable="custom" />
        <el-table-column label="资源" width="150" sortable="custom" prop="resourceType">
          <template #default="{ row }">
            <div>
              <el-tag size="small">{{ row.resourceType }}</el-tag>
              <div v-if="row.resourceId" style="font-size: 12px; color: var(--text-color-muted); margin-top: 4px">
                ID: {{ row.resourceId }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="details" label="操作详情" min-width="250" show-overflow-tooltip />
        <el-table-column label="结果" width="100" sortable="custom" prop="result">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="140" sortable="custom" />
        <el-table-column label="操作时间" width="180" sortable="custom" prop="createdAt">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="200" v-if="hasErrors">
          <template #default="{ row }">
            <span v-if="row.errorMessage" style="color: var(--el-color-danger)">
              {{ row.errorMessage }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs, type AuditLog, type AuditLogQuery } from '@/api/audit'

const loading = ref(false)
const rows = ref<AuditLog[]>([])
const total = ref(0)

const query = reactive<AuditLogQuery>({
  page: 1,
  size: 20,
  resourceType: undefined,
  resourceId: undefined,
  sortBy: 'createdAt_desc' // 默认按创建时间降序
})

const hasErrors = computed(() => {
  return rows.value.some(row => row.errorMessage)
})

const reload = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs(query)
    rows.value = res.records
    total.value = res.total
  } catch (error: any) {
    console.error('加载操作日志失败:', error)
    ElMessage.error(error.message || '加载操作日志失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.resourceType = undefined
  query.resourceId = undefined
  query.sortBy = 'createdAt_desc'
  query.page = 1
  reload()
}

// 处理表格排序变化
const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  if (!order) {
    // 取消排序，恢复默认
    query.sortBy = 'createdAt_desc'
  } else {
    // 设置排序
    const direction = order === 'ascending' ? 'asc' : 'desc'
    query.sortBy = `${prop}_${direction}`
  }
  reload()
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  reload()
})
</script>

<style scoped lang="scss">
.audit-log {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    .subtitle {
      margin: 8px 0 0 0;
      color: var(--text-color-muted);
      font-size: 14px;
    }
  }

  .filter-card {
    margin-bottom: 20px;
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
