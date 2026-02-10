<template>
  <div class="competition-manage">
    <div class="page-header">
      <div>
        <h1>{{ isTemplateMode ? '比赛模板管理' : '比赛管理' }}</h1>
        <p class="subtitle">{{ isTemplateMode ? '创建和管理比赛模板' : '创建、编辑、发布、归档比赛' }}</p>
      </div>
      <div style="display:flex; gap: 10px;">
        <el-button v-if="!isTemplateMode" @click="openTemplateDrawer">模板</el-button>
        <el-button type="primary" @click="openCreate">{{ isTemplateMode ? '新建模板' : '新建比赛' }}</el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never" v-if="!isTemplateMode">
      <el-form :inline="true" :model="query">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="() => nextTick(reload)" style="width: 120px;">
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="比赛名称关键字"
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

    <el-card class="filter-card" shadow="never" v-else>
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="模板名称关键字"
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
        <el-table-column prop="name" :label="isTemplateMode ? '模板名称' : '比赛名称'" min-width="200" sortable show-overflow-tooltip />
        <el-table-column label="主办方" min-width="150" sortable show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="isTemplateMode">{{ getTemplateOrganizer(row) }}</span>
            <span v-else>{{ row.organizer }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isTemplateMode" prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="!isTemplateMode" label="报名时间" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ formatDateTime(row.signupStartAt) }} ~ {{ formatDateTime(row.signupEndAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180" v-if="isTemplateMode" sortable>
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" v-if="isTemplateMode">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openCreateFromTemplate(row)">使用</el-button>
            <el-button link type="danger" @click="handleDeleteTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="450" fixed="right" v-if="!isTemplateMode">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: nowrap; gap: 4px;">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="info" size="small" @click="openStats(row)">统计</el-button>
              <el-button link type="warning" size="small" @click="openScoring(row)">评分</el-button>
              <el-button link type="info" size="small" @click="openHistory(row)">历史</el-button>
              <el-button link type="primary" size="small" @click="handleSaveAsTemplate(row)">存为模板</el-button>
              <el-button
                v-if="row.status !== 'PUBLISHED'"
                link
                type="success"
                size="small"
                @click="handlePublish(row)"
              >
                发布
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                link
                type="warning"
                size="small"
                @click="handleArchive(row)"
              >
                归档
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="isTemplateMode ? '暂无模板数据' : '暂无比赛数据'">
            <el-button v-if="!isTemplateMode && query.status" type="primary" @click="reset">
              清除筛选条件
            </el-button>
          </el-empty>
        </template>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="isTemplateMode ? '模板名称' : '比赛名称'" prop="name">
          <el-input v-model="form.name" :placeholder="isTemplateMode ? '请输入模板名称' : '请输入比赛名称'" />
        </el-form-item>
        <el-form-item label="主办方" prop="organizer">
          <el-input v-model="form.organizer" placeholder="请输入主办方" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" placeholder="请选择级别">
            <el-option label="校级" value="SCHOOL" />
            <el-option label="省级" value="PROVINCE" />
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="国际级" value="INTERNATIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围" prop="scope">
          <el-select v-model="form.scope" placeholder="请选择范围">
            <el-option label="校内" value="SCHOOL" />
            <el-option label="省内" value="PROVINCE" />
            <el-option label="全国" value="NATIONAL" />
            <el-option label="国际" value="INTERNATIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="面向范围" prop="audience">
          <div style="width: 100%">
            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="留空表示全校/全体；填写后会用于“发布比赛通知”的筛选（学院/专业/年级）"
              style="margin-bottom: 8px;"
            />
            <el-input v-model="audienceDepartments" placeholder="面向学院（逗号分隔，可空）例如：计算机学院,软件学院" />
            <div style="height: 8px;" />
            <el-input v-model="audienceMajors" placeholder="面向专业（逗号分隔，可空）例如：软件工程,计算机科学与技术" />
            <div style="height: 8px;" />
            <el-input v-model="audienceGrades" placeholder="面向年级（逗号分隔，可空）例如：2022,2023" />
          </div>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option label="编程" value="PROGRAMMING" />
            <el-option label="设计" value="DESIGN" />
            <el-option label="创新" value="INNOVATION" />
            <el-option label="科研" value="RESEARCH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="报名时间" required>
          <el-date-picker
            v-model="signupRange"
            type="datetimerange"
            start-placeholder="开始"
            end-placeholder="截止"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="比赛时间" required>
          <el-date-picker
            v-model="eventRange"
            type="datetimerange"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="队伍人数">
          <div class="inline-grid">
            <el-input-number v-model="form.minTeamMembers" :min="1" :max="100" />
            <span class="sep">-</span>
            <el-input-number v-model="form.maxTeamMembers" :min="1" :max="100" />
          </div>
        </el-form-item>
        <el-form-item label="个人队伍上限">
          <div style="width: 100%;">
            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="为空：不限制；1：每人最多参加 1 支队伍；N：每人最多参加 N 支队伍"
              style="margin-bottom: 8px;"
            />
            <el-input-number
              v-model="form.maxTeamsPerUser"
              :min="1"
              :max="20"
              :step="1"
              placeholder="留空表示不限制"
              style="width: 200px;"
            />
          </div>
        </el-form-item>
        <el-form-item label="需要导师">
          <el-switch v-model="form.requireMentor" />
        </el-form-item>
        <el-form-item label="资格限制">
          <div style="width: 100%;">
            <el-switch v-model="form.eligibilityEnabled" />
            <span style="margin-left: 8px; font-size: 13px; color: var(--text-color-muted);">
              启用后，仅符合上方“面向范围（学院/专业/年级）”配置的同学可以发起队伍或申请加入
            </span>
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="附件">
          <div class="attachment-section">
            <el-upload
              v-if="editingId"
              :http-request="handleCustomUpload"
              :before-upload="beforeUpload"
              :file-list="attachmentFileList"
              :on-remove="handleRemoveAttachment"
              multiple
            >
              <el-button type="primary" :icon="Upload" :loading="uploading">上传附件</el-button>
              <template #tip>
                <div class="el-upload__tip">支持上传多个文件，单个文件不超过100MB</div>
              </template>
            </el-upload>
            <div v-if="!editingId" class="upload-hint">
              <el-text type="info">保存比赛后可上传附件</el-text>
            </div>
            <div v-if="attachmentList.length > 0" class="attachment-list">
              <div
                v-for="(att, idx) in attachmentList"
                :key="idx"
                class="attachment-item"
              >
                <el-icon><Document /></el-icon>
                <span class="attachment-name">{{ att.name }}</span>
                <el-button
                  v-if="editingId"
                  link
                  type="danger"
                  size="small"
                  @click="handleDeleteAttachment(att.url)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 统计抽屉 -->
    <el-drawer v-model="statsVisible" title="比赛统计" size="420px">
      <div v-loading="statsLoading">
        <el-descriptions v-if="statsData" :column="1" border>
          <el-descriptions-item label="队伍数">{{ statsData.teamCount }}</el-descriptions-item>
          <el-descriptions-item label="参赛人数">{{ statsData.memberCount }}</el-descriptions-item>
          <el-descriptions-item label="已有导师队伍">{{ statsData.mentorTeamCount }}</el-descriptions-item>
          <el-descriptions-item label="导师覆盖率">{{ statsData.mentorCoverageRate }}%</el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ (statsData as any).viewCount ?? 0 }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无统计数据" />
        <div v-if="statsData" style="margin-top: 16px;">
          <h4 style="margin: 0 0 8px; font-size: 14px;">最近 7 天趋势</h4>
          <el-table :data="trendRows" size="small" border style="width: 100%;">
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column prop="teamCount" label="新增队伍数" width="120" />
            <el-table-column prop="memberCount" label="新增成员数" width="120" />
            <el-table-column prop="viewCount" label="浏览量" />
          </el-table>
        </div>
      </div>
    </el-drawer>

    <!-- 历史抽屉 -->
    <el-drawer v-model="historyVisible" title="操作历史" size="520px">
      <div v-loading="historyLoading">
        <el-timeline v-if="historyRecords.length > 0">
          <el-timeline-item
            v-for="log in historyRecords"
            :key="log.id"
            :timestamp="formatDateTime(log.createdAt)"
          >
            <div class="audit-item">
              <div class="audit-main">
                <strong>{{ log.username || '未知用户' }}</strong>
                <span class="audit-action">{{ formatAuditAction(log.action) }}</span>
                <el-tag size="small" :type="log.result === 'SUCCESS' ? 'success' : 'danger'">
                  {{ log.result }}
                </el-tag>
              </div>
              <div class="audit-detail" v-if="log.details">{{ log.details }}</div>
              <div class="audit-detail" v-if="log.errorMessage" style="color: var(--el-color-danger)">
                {{ log.errorMessage }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无操作记录" />
        <div class="pagination" v-if="historyTotal > historyQuery.size">
          <el-pagination
            v-model:current-page="historyQuery.page"
            v-model:page-size="historyQuery.size"
            :total="historyTotal"
            layout="prev, pager, next"
            background
            @current-change="loadHistory"
          />
        </div>
      </div>
    </el-drawer>

    <!-- 模板抽屉 -->
    <el-drawer v-model="templateVisible" title="比赛模板" size="560px">
      <div class="template-toolbar">
        <el-input v-model="templateQuery.keyword" placeholder="搜索模板名称" clearable @clear="loadTemplates" @keyup.enter="loadTemplates" />
        <el-button type="primary" @click="loadTemplates">刷新</el-button>
      </div>
      <el-table :data="templateRows" v-loading="templateLoading" style="width: 100%">
        <el-table-column prop="name" label="模板名称" show-overflow-tooltip />
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: nowrap; gap: 4px;">
              <el-button link type="primary" size="small" @click="openCreateFromTemplate(row)">用此创建</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteTemplate(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination" v-if="templateTotal > templateQuery.size">
        <el-pagination
          v-model:current-page="templateQuery.page"
          v-model:page-size="templateQuery.size"
          :total="templateTotal"
          layout="prev, pager, next"
          background
          @current-change="loadTemplates"
        />
      </div>
    </el-drawer>

    <!-- 从模板创建 -->
    <el-dialog v-model="createFromTemplateVisible" title="从模板创建比赛" width="640px">
      <el-form label-width="120px">
        <el-form-item label="比赛名称">
          <el-input v-model="createFromTemplateForm.name" placeholder="可选，不填则使用模板内名称" />
        </el-form-item>
        <el-form-item label="报名时间">
          <el-date-picker
            v-model="createFromTemplateSignupRange"
            type="datetimerange"
            start-placeholder="开始"
            end-placeholder="截止"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="比赛时间">
          <el-date-picker
            v-model="createFromTemplateEventRange"
            type="datetimerange"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFromTemplateVisible = false">取消</el-button>
        <el-button type="primary" :loading="createFromTemplateLoading" @click="submitCreateFromTemplate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 队伍评分 -->
    <el-dialog v-model="scoringVisible" title="队伍评分" width="860px">
      <div v-loading="scoringLoading">
        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="导师仅能为自己指导的队伍打分，平台/院系管理员可对所有队伍评分"
          style="margin-bottom: 10px;"
        />
        <el-table :data="scoringTeams" style="width: 100%" size="small">
          <el-table-column prop="name" label="队伍" min-width="200" />
          <el-table-column label="成员数" width="90">
            <template #default="{ row }">{{ row.memberCount || '-' }}</template>
          </el-table-column>
          <el-table-column label="评分" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row._score" :min="0" :max="100" :step="1" />
            </template>
          </el-table-column>
          <el-table-column label="评语" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row._comment" placeholder="可选" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :loading="row._saving" @click="submitScore(row)">提交</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination" v-if="scoringTotal > scoringQuery.size">
          <el-pagination
            v-model:current-page="scoringQuery.page"
            v-model:page-size="scoringQuery.size"
            :total="scoringTotal"
            layout="prev, pager, next"
            background
            @current-change="loadScoringTeams"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="scoringVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Document } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import type { Competition, CompetitionAttachment } from '@/types/competition'
import {
  getCompetitions,
  createCompetition,
  updateCompetition,
  publishCompetition,
  archiveCompetition,
  deleteCompetition,
  uploadCompetitionAttachment,
  deleteCompetitionAttachment,
  getCompetitionStats,
  getCompetitionTrend,
  type CompetitionStats,
  getCompetitionTeams,
  upsertCompetitionTeamScore
} from '@/api/competition'
import { getAuditLogs, type AuditLog } from '@/api/audit'
import {
  getCompetitionTemplates,
  createCompetitionTemplate,
  updateCompetitionTemplate,
  createCompetitionFromTemplate,
  deleteCompetitionTemplate,
  type CompetitionTemplate
} from '@/api/competitionTemplate'

const route = useRoute()
const router = useRouter()

// 判断是否为模板管理模式
const isTemplateMode = computed(() => route.path.includes('/templates'))

const loading = ref(false)
const rows = ref<Competition[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: ''
})

const reload = async () => {
  loading.value = true
  try {
    if (isTemplateMode.value) {
      const res = await getCompetitionTemplates({
        page: query.page,
        size: query.size,
        keyword: query.keyword
      })
      rows.value = res.records || []
      total.value = res.total || res.records?.length || 0
    } else {
      const params: any = {
        page: query.page,
        size: query.size
      }
      if (query.status) params.status = query.status
      if (query.keyword) params.keyword = query.keyword
      
      const res = await getCompetitions(params)
      rows.value = res.records || []
      total.value = res.total || res.records?.length || 0
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.page = 1
  query.size = 10
  query.status = ''
  query.keyword = ''
  reload()
}

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => {
  if (isTemplateMode.value) {
    return editingId.value ? '编辑模板' : '新建模板'
  }
  return editingId.value ? '编辑比赛' : '新建比赛'
})
const formRef = ref<FormInstance>()

const form = reactive<any>({
  name: '',
  organizer: '',
  level: 'SCHOOL',
  scope: 'SCHOOL',
  type: 'OTHER',
  minTeamMembers: 1,
  maxTeamMembers: 5,
  maxTeamsPerUser: undefined,
  requireMentor: false,
  eligibilityEnabled: false,
  description: ''
})

const audienceDepartments = ref('')
const audienceMajors = ref('')
const audienceGrades = ref('')

const signupRange = ref<[string, string] | null>(null)
const eventRange = ref<[string, string] | null>(null)
const attachmentList = ref<CompetitionAttachment[]>([])
const uploading = ref(false)

const attachmentFileList = computed(() => {
  return attachmentList.value.map(att => ({
    name: att.name,
    url: att.url,
    uid: att.url // 使用 url 作为唯一标识
  }))
})

// 统计面板
const statsVisible = ref(false)
const statsLoading = ref(false)
const statsData = ref<CompetitionStats | null>(null)
const trendRows = ref<any[]>([])

const openStats = async (row: Competition) => {
  statsVisible.value = true
  statsLoading.value = true
  statsData.value = null
  trendRows.value = []
  try {
    statsData.value = await getCompetitionStats(row.id)
    const trend = await getCompetitionTrend(row.id, 7)
    trendRows.value = trend.map(p => ({
      date: p.date,
      teamCount: p.teamCount,
      memberCount: p.memberCount,
      viewCount: p.viewCount
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计失败')
  } finally {
    statsLoading.value = false
  }
}

// 历史记录
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRecords = ref<AuditLog[]>([])
const historyTotal = ref(0)
const historyQuery = reactive({ page: 1, size: 20, competitionId: 0 })

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await getAuditLogs({
      page: historyQuery.page,
      size: historyQuery.size,
      resourceType: 'COMPETITION',
      resourceId: historyQuery.competitionId
    })
    historyRecords.value = res.records
    historyTotal.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

const openHistory = async (row: Competition) => {
  historyQuery.page = 1
  historyQuery.competitionId = row.id
  historyVisible.value = true
  await loadHistory()
}

const formatAuditAction = (action: string) => {
  const map: Record<string, string> = {
    CREATE_COMPETITION: '创建比赛',
    UPDATE_COMPETITION: '更新比赛',
    PUBLISH_COMPETITION: '发布比赛',
    ARCHIVE_COMPETITION: '归档比赛',
    UPLOAD_COMPETITION_ATTACHMENT: '上传附件',
    DELETE_COMPETITION_ATTACHMENT: '删除附件'
  }
  return map[action] || action
}

// 模板
const templateVisible = ref(false)
const templateLoading = ref(false)
const templateRows = ref<CompetitionTemplate[]>([])
const templateTotal = ref(0)
const templateQuery = reactive({ page: 1, size: 20, keyword: '' })

const openTemplateDrawer = async () => {
  templateVisible.value = true
  templateQuery.page = 1
  await loadTemplates()
}

const loadTemplates = async () => {
  templateLoading.value = true
  try {
    const res = await getCompetitionTemplates(templateQuery)
    templateRows.value = res.records
    templateTotal.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载模板失败')
  } finally {
    templateLoading.value = false
  }
}

const handleSaveAsTemplate = async (row: Competition) => {
  try {
    const name = await ElMessageBox.prompt('请输入模板名称', '保存为模板', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: row.name ? `模板-${row.name}` : '比赛模板'
    })
    const templateName = (name as any).value
    await createCompetitionTemplate({ name: templateName, payload: row })
    ElMessage.success('模板已保存')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '保存模板失败')
  }
}

const createFromTemplateVisible = ref(false)
const createFromTemplateLoading = ref(false)
const creatingTemplateId = ref<number | null>(null)
const createFromTemplateForm = reactive({ name: '' })
const createFromTemplateSignupRange = ref<[string, string] | null>(null)
const createFromTemplateEventRange = ref<[string, string] | null>(null)

const openCreateFromTemplate = (t: CompetitionTemplate) => {
  creatingTemplateId.value = t.id
  createFromTemplateForm.name = ''
  createFromTemplateSignupRange.value = null
  createFromTemplateEventRange.value = null
  createFromTemplateVisible.value = true
}

const submitCreateFromTemplate = async () => {
  if (!creatingTemplateId.value) return
  createFromTemplateLoading.value = true
  try {
    const overrides: any = {}
    if (createFromTemplateForm.name) overrides.name = createFromTemplateForm.name
    if (createFromTemplateSignupRange.value) {
      overrides.signupStartAt = createFromTemplateSignupRange.value[0]
      overrides.signupEndAt = createFromTemplateSignupRange.value[1]
    }
    if (createFromTemplateEventRange.value) {
      overrides.startAt = createFromTemplateEventRange.value[0]
      overrides.endAt = createFromTemplateEventRange.value[1]
    }
    const competition = await createCompetitionFromTemplate(creatingTemplateId.value, overrides)
    createFromTemplateVisible.value = false
    
    // 如果当前在模板管理页面，提示用户并询问是否跳转到比赛管理页面
    if (isTemplateMode.value) {
      ElMessageBox.confirm(
        '比赛已创建为草稿状态，是否跳转到比赛管理页面查看？',
        '创建成功',
        {
          confirmButtonText: '立即查看',
          cancelButtonText: '稍后查看',
          type: 'success'
        }
      ).then(() => {
        // 跳转到比赛管理页面
        router.push('/competition/manage')
      }).catch(() => {
        // 用户选择稍后查看，显示提示
        ElMessage.success('比赛已创建，请前往"比赛管理"页面查看')
      })
    } else {
      ElMessage.success('已从模板创建比赛（草稿）')
      await reload()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    createFromTemplateLoading.value = false
  }
}

const handleDeleteTemplate = async (row: CompetitionTemplate) => {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.name}」吗？此操作不可恢复。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteCompetitionTemplate(row.id)
    ElMessage.success('模板已删除')
    await reload()
    // 如果模板抽屉打开，也刷新模板列表
    if (templateVisible.value) {
      await loadTemplates()
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

// 评分
const scoringVisible = ref(false)
const scoringLoading = ref(false)
const scoringTeams = ref<any[]>([])
const scoringTotal = ref(0)
const scoringQuery = reactive({ page: 1, size: 20, competitionId: 0 })

const loadScoringTeams = async () => {
  scoringLoading.value = true
  try {
    const res = await getCompetitionTeams(scoringQuery.competitionId, { page: scoringQuery.page, size: scoringQuery.size })
    scoringTeams.value = (res.records || []).map((t: any) => ({
      ...t,
      _score: 0,
      _comment: '',
      _saving: false
    }))
    scoringTotal.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载队伍失败')
  } finally {
    scoringLoading.value = false
  }
}

const openScoring = async (row: Competition) => {
  scoringQuery.page = 1
  scoringQuery.competitionId = row.id
  scoringVisible.value = true
  await loadScoringTeams()
}

const submitScore = async (teamRow: any) => {
  teamRow._saving = true
  try {
    await upsertCompetitionTeamScore(scoringQuery.competitionId, {
      teamId: teamRow.id,
      score: Number(teamRow._score),
      comment: teamRow._comment || undefined
    })
    ElMessage.success('评分已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '评分失败')
  } finally {
    teamRow._saving = false
  }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入比赛名称', trigger: 'blur' }],
  organizer: [{ required: true, message: '请输入主办方', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
  scope: [{ required: true, message: '请选择范围', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const openCreate = () => {
  editingId.value = null
  
  // 重置表单
  Object.assign(form, {
    name: '',
    organizer: '',
    level: 'SCHOOL',
    scope: 'SCHOOL',
    type: 'PROGRAMMING',
    minTeamMembers: 1,
    maxTeamMembers: 5,
    maxTeamsPerUser: undefined,
    requireMentor: false,
    eligibilityEnabled: false,
    description: ''
  })
  
  audienceDepartments.value = ''
  audienceMajors.value = ''
  audienceGrades.value = ''
  signupRange.value = ['', '']
  eventRange.value = ['', '']
  attachmentList.value = []
  
  dialogVisible.value = true
}

const openEdit = (row: Competition | any) => {
  editingId.value = row.id
  
  // 如果是模板模式，需要从 payload 中解析数据
  if (isTemplateMode.value && row.payload) {
    try {
      const payload = typeof row.payload === 'string' ? JSON.parse(row.payload) : row.payload
      
      // 模板名称
      form.name = row.name
      
      // 从 payload 中加载比赛数据
      form.organizer = payload.organizer || ''
      form.level = payload.level || 'SCHOOL'
      form.scope = payload.scope || 'SCHOOL'
      form.type = payload.type || 'PROGRAMMING'
      form.minTeamMembers = payload.minTeamMembers ?? 1
      form.maxTeamMembers = payload.maxTeamMembers ?? 5
      form.maxTeamsPerUser = payload.maxTeamsPerUser ?? undefined
      form.requireMentor = !!payload.requireMentor
      form.eligibilityEnabled = !!payload.eligibilityEnabled
      form.description = payload.description || ''
      
      // audience
      try {
        const a = payload.audience ? (typeof payload.audience === 'string' ? JSON.parse(payload.audience) : payload.audience) : {}
        audienceDepartments.value = Array.isArray(a.departments) ? a.departments.join(',') : ''
        audienceMajors.value = Array.isArray(a.majors) ? a.majors.join(',') : ''
        audienceGrades.value = Array.isArray(a.grades) ? a.grades.join(',') : ''
      } catch (e) {
        audienceDepartments.value = ''
        audienceMajors.value = ''
        audienceGrades.value = ''
      }
      
      // 时间范围
      signupRange.value = payload.signupStartAt && payload.signupEndAt 
        ? [String(payload.signupStartAt), String(payload.signupEndAt)]
        : ['', '']
      eventRange.value = payload.startAt && payload.endAt
        ? [String(payload.startAt), String(payload.endAt)]
        : ['', '']
      
      // 附件
      if (payload.attachments) {
        if (Array.isArray(payload.attachments)) {
          attachmentList.value = payload.attachments
        } else if (typeof payload.attachments === 'string') {
          try {
            attachmentList.value = JSON.parse(payload.attachments)
          } catch {
            attachmentList.value = []
          }
        } else {
          attachmentList.value = []
        }
      } else {
        attachmentList.value = []
      }
    } catch (e) {
      console.error('解析模板数据失败:', e)
      ElMessage.error('加载模板数据失败')
      return
    }
  } else {
    // 比赛模式，直接使用 row 的数据
    form.name = row.name
    form.organizer = row.organizer
    form.level = row.level
    form.scope = row.scope
    form.type = row.type
    form.minTeamMembers = row.minTeamMembers ?? 1
    form.maxTeamMembers = row.maxTeamMembers ?? 5
    form.maxTeamsPerUser = row.maxTeamsPerUser ?? undefined
    form.requireMentor = !!row.requireMentor
    form.eligibilityEnabled = !!(row as any).eligibilityEnabled
    form.description = row.description || ''

    // audience JSON -> comma text
    try {
      const a = row.audience ? JSON.parse(row.audience as any) : {}
      audienceDepartments.value = Array.isArray(a.departments) ? a.departments.join(',') : ''
      audienceMajors.value = Array.isArray(a.majors) ? a.majors.join(',') : ''
      audienceGrades.value = Array.isArray(a.grades) ? a.grades.join(',') : ''
    } catch (e) {
      audienceDepartments.value = ''
      audienceMajors.value = ''
      audienceGrades.value = ''
    }
    signupRange.value = [String(row.signupStartAt), String(row.signupEndAt)]
    eventRange.value = [String(row.startAt), String(row.endAt)]
    
    // 加载附件列表
    if (row.attachments) {
      if (Array.isArray(row.attachments)) {
        attachmentList.value = row.attachments
      } else if (typeof row.attachments === 'string') {
        try {
          attachmentList.value = JSON.parse(row.attachments)
        } catch {
          attachmentList.value = []
        }
      } else {
        attachmentList.value = []
      }
    } else {
      attachmentList.value = []
    }
  }
  
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.clearValidate()
  attachmentList.value = []
}

const beforeUpload = (file: File) => {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过100MB')
    return false
  }
  return true
}

const handleCustomUpload = async (options: any) => {
  if (!editingId.value) {
    ElMessage.error('请先保存比赛')
    return
  }
  uploading.value = true
  try {
    const result = await uploadCompetitionAttachment(editingId.value, options.file)
    attachmentList.value.push({
      name: result.name,
      url: result.url
    })
    ElMessage.success('附件上传成功')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const handleRemoveAttachment = async (file: UploadFile) => {
  if (file.url && editingId.value) {
    await handleDeleteAttachment(file.url)
  }
}

const handleDeleteAttachment = async (url: string) => {
  if (!editingId.value) return
  try {
    await ElMessageBox.confirm('确定删除该附件吗？', '确认删除', {
      type: 'warning'
    })
    await deleteCompetitionAttachment(editingId.value, url)
    attachmentList.value = attachmentList.value.filter(att => att.url !== url)
    ElMessage.success('删除成功')
    await reload()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    // 模板模式下不需要时间验证
    if (!isTemplateMode.value && (!signupRange.value || !eventRange.value)) {
      ElMessage.error('请填写报名时间与比赛时间')
      return
    }
    
    saving.value = true
    try {
      const toList = (s: string) =>
        (s || '')
          .split(',')
          .map((x) => x.trim())
          .filter(Boolean)
      const departments = toList(audienceDepartments.value)
      const majors = toList(audienceMajors.value)
      const grades = toList(audienceGrades.value)
        .map((x) => Number(x))
        .filter((n) => Number.isFinite(n)) as number[]

      const audience =
        departments.length === 0 && majors.length === 0 && grades.length === 0
          ? undefined
          : JSON.stringify({ departments, majors, grades })

      if (isTemplateMode.value) {
        // 模板管理模式
        // 注意：模板名称和比赛名称是分开的
        // form.name 是模板名称，payload 中的 name 是比赛名称（模板内容）
        const templateName = form.name
        const { name: competitionName, ...restForm } = form
        
        const payload: any = {
          name: competitionName || '未命名比赛', // 比赛名称（模板内容）
          ...restForm,
          maxTeamsPerUser: form.maxTeamsPerUser || undefined,
          audience,
          signupStartAt: signupRange.value?.[0] || undefined,
          signupEndAt: signupRange.value?.[1] || undefined,
          startAt: eventRange.value?.[0] || undefined,
          endAt: eventRange.value?.[1] || undefined
        }

        if (editingId.value) {
          await updateCompetitionTemplate(editingId.value, {
            name: templateName,
            payload
          })
        } else {
          await createCompetitionTemplate({
            name: templateName,
            payload
          })
        }
      } else {
        // 比赛管理模式
        const payload: any = {
          ...form,
          maxTeamsPerUser: form.maxTeamsPerUser || undefined,
          audience,
          signupStartAt: signupRange.value[0],
          signupEndAt: signupRange.value[1],
          startAt: eventRange.value[0],
          endAt: eventRange.value[1]
        }
        if (editingId.value) {
          await updateCompetition(editingId.value, payload)
        } else {
          await createCompetition(payload)
        }
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await reload()
    } catch (e: any) {
      ElMessage.error(e.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handlePublish = async (row: Competition) => {
  try {
    await ElMessageBox.confirm(`确定发布比赛「${row.name}」吗？`, '确认发布', {
      type: 'warning'
    })
    await publishCompetition(row.id)
    ElMessage.success('已发布')
    reload()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '发布失败')
  }
}

const handleArchive = async (row: Competition) => {
  try {
    await ElMessageBox.confirm(`确定归档比赛「${row.name}」吗？`, '确认归档', {
      type: 'warning'
    })
    await archiveCompetition(row.id)
    ElMessage.success('已归档')
    reload()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '归档失败')
  }
}

const handleDelete = async (row: Competition) => {
  try {
    await ElMessageBox.confirm(
      `确定删除比赛「${row.name}」吗？此操作不可恢复。`,
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
    await deleteCompetition(row.id)
    ElMessage.success('删除成功')
    reload()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const statusText = (status: string) =>
  ({ DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' } as any)[status] || status

const statusTagType = (status: string): 'info' | 'success' | 'warning' =>
  ({ DRAFT: 'info', PUBLISHED: 'success', ARCHIVED: 'warning' } as any)[status] || 'info'

// 从模板的 payload 中获取主办方
const getTemplateOrganizer = (template: any) => {
  try {
    if (template.payload) {
      const payload = typeof template.payload === 'string' 
        ? JSON.parse(template.payload) 
        : template.payload
      return payload.organizer || '-'
    }
  } catch (e) {
    console.error('解析模板 payload 失败:', e)
  }
  return '-'
}

const formatDateTime = (v: any) => {
  if (!v) return '-'
  const d = new Date(v)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// 监听路由变化，切换模式时重新加载
watch(() => route.path, () => {
  query.page = 1
  query.status = ''
  query.keyword = ''
  reload()
})

onMounted(() => {
  reload()
})
</script>

<style scoped lang="scss">
.competition-manage {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: 700;
    margin: 0 0 8px 0;
    color: var(--text-color);
  }

  .subtitle {
    color: var(--text-color-muted);
    font-size: 14px;
    margin: 0;
  }
}

.filter-card {
  margin-bottom: 16px;
}

.table-card {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

.inline-grid {
  display: flex;
  align-items: center;
  gap: 10px;
  .sep {
    color: var(--text-color-muted);
  }
}

.attachment-section {
  width: 100%;
}

.upload-hint {
  padding: 12px;
  background: var(--bg-card-hover);
  border-radius: 4px;
  text-align: center;
}

.attachment-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 4px;

  .attachment-name {
    flex: 1;
    color: var(--text-color);
    font-size: 14px;
  }
}

.template-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.audit-item {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .audit-main {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
  }

  .audit-action {
    color: var(--text-color-secondary);
  }

  .audit-detail {
    color: var(--text-color-muted);
    font-size: 13px;
    line-height: 1.5;
  }
}
</style>

