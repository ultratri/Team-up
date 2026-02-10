<template>
  <div class="mentor-scoring">
    <div class="page-header">
      <div>
        <h1>我的指导队伍 & 评分</h1>
        <p class="subtitle">查看你指导的比赛队伍，快速进入团队空间并对队伍进行评分</p>
      </div>
    </div>

    <el-alert
      class="tip-alert"
      type="info"
      show-icon
      :closable="false"
      title="导师仅能为自己指导的队伍打分，平台/院系管理员可对所有队伍评分"
      style="margin-bottom: 12px;"
    />

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="所属比赛">
          <el-select
            v-model="query.competitionId"
            placeholder="全部比赛"
            clearable
            filterable
            @change="reload"
            style="min-width: 220px"
          >
            <el-option
              v-for="c in competitions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="评分状态">
          <el-select
            v-model="query.scoredStatus"
            placeholder="全部"
            @change="reload"
            style="min-width: 160px"
          >
            <el-option label="全部" value="ALL" />
            <el-option label="已评分" value="SCORED" />
            <el-option label="未评分" value="UNSCORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="队伍关键词">
          <el-input
            v-model="query.keyword"
            placeholder="队伍名称"
            clearable
            @clear="reload"
            @keyup.enter="reload"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="rows" v-loading="loading" style="width: 100%">
        <el-table-column label="队伍" min-width="220">
          <template #default="{ row }">
            <router-link :to="{ name: 'TeamOverview', params: { id: row.id } }" class="team-link">
              {{ row.teamName }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column label="所属比赛" min-width="200">
          <template #default="{ row }">
            <span v-if="row.competitionId">
              <router-link
                :to="{ name: 'CompetitionDetail', params: { id: row.competitionId } }"
                class="competition-link"
              >
                比赛 #{{ row.competitionId }}
              </router-link>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row._score" :min="0" :max="100" :step="1" />
          </template>
        </el-table-column>
        <el-table-column label="评语" min-width="260">
          <template #default="{ row }">
            <el-input v-model="row._comment" placeholder="可选" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :loading="row._saving" @click="submit(row)">提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyMentorCompetitionTeams, upsertCompetitionTeamScore, getCompetitions, getScoredCompetitionTeams } from '@/api/competition'
import type { Competition } from '@/types/competition'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  competitionId: undefined as number | undefined,
  scoredStatus: 'ALL' as 'ALL' | 'SCORED' | 'UNSCORED'
})

const competitions = ref<Competition[]>([])
const scoredTeamSet = ref<Set<string>>(new Set())

const reload = async () => {
  loading.value = true
  try {
    const res = await getMyMentorCompetitionTeams(query)
    let list = (res.records || []).map((t: any) => ({
      ...t,
      _score: 0,
      _comment: '',
      _saving: false
    }))

    if (query.scoredStatus !== 'ALL') {
      list = list.filter((t: any) => {
        const key = `${t.competitionId || ''}:${t.id}`
        const hasScore = scoredTeamSet.value.has(key)
        return query.scoredStatus === 'SCORED' ? hasScore : !hasScore
      })
    }

    rows.value = list
    total.value = list.length
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.size = 20
  query.keyword = ''
  query.competitionId = undefined
  query.scoredStatus = 'ALL'
  reload()
}

const loadCompetitions = async () => {
  try {
    const res = await getCompetitions({ page: 1, size: 50, status: 'PUBLISHED' })
    competitions.value = res.records || []
  } catch {
    competitions.value = []
  }
}

const loadScoredTeams = async () => {
  try {
    const refs = await getScoredCompetitionTeams()
    const set = new Set<string>()
    for (const r of refs) {
      if (r.competitionId && r.teamId) {
        set.add(`${r.competitionId}:${r.teamId}`)
      }
    }
    scoredTeamSet.value = set
  } catch {
    scoredTeamSet.value = new Set()
  }
}

const submit = async (row: any) => {
  row._saving = true
  try {
    await upsertCompetitionTeamScore(Number(row.competitionId), {
      teamId: Number(row.id),
      score: Number(row._score),
      comment: row._comment || undefined
    })
    ElMessage.success('评分已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '评分失败')
  } finally {
    row._saving = false
  }
}

onMounted(async () => {
  await Promise.all([loadCompetitions(), loadScoredTeams()])
  reload()
})
</script>

<style scoped>
.mentor-scoring {
  padding: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 12px;
}
.subtitle {
  margin: 6px 0 0;
  color: var(--text-color-muted);
}
.filter-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  margin-bottom: 12px;
}
.table-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
}
.pagination {
  padding: 12px 0 0;
  display: flex;
  justify-content: flex-end;
}

.team-link,
.competition-link {
  color: var(--el-color-primary);
  text-decoration: none;
}
.team-link:hover,
.competition-link:hover {
  text-decoration: underline;
}
</style>

