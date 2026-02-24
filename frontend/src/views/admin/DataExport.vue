<template>
  <div class="data-export">
    <div class="page-header">
      <div>
        <h1>数据导入/导出</h1>
        <p class="subtitle">导出系统数据为Excel或CSV格式，或导入数据到系统</p>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="exportType in exportTypes" :key="exportType.key">
        <el-card shadow="hover" class="export-card">
          <div class="export-icon" :style="{ background: exportType.color }">
            <el-icon :size="32"><component :is="exportType.icon" /></el-icon>
          </div>
          <div class="export-content">
            <h3>{{ exportType.title }}</h3>
            <p class="export-desc">{{ exportType.description }}</p>
            <div class="export-stats">
              <span>共 {{ exportType.count }} 条记录</span>
            </div>
            <div class="button-group">
              <el-button
                type="primary"
                @click="handleExport(exportType.key)"
                :loading="exporting[exportType.key]"
                style="flex: 1"
              >
                <el-icon><Download /></el-icon>
                导出
              </el-button>
              <el-button
                v-if="exportType.importable"
                type="success"
                @click="openImport(exportType.key)"
                :loading="importing[exportType.key]"
                style="flex: 1"
              >
                <el-icon><Upload /></el-icon>
                导入
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" :title="`导入${currentImportType?.title || '数据'}`" width="600px">
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="注意：导入数据会覆盖或新增系统中的数据，请谨慎操作"
        style="margin-bottom: 16px"
      />
      <el-form label-width="100px">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            accept=".xlsx,.xls,.csv"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 Excel (.xlsx, .xls) 或 CSV (.csv) 格式，请使用系统导出的文件
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importingFile" @click="handleImport" :disabled="!importFile">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, markRaw } from 'vue'
import { ElMessage, genFileId } from 'element-plus'
import type { UploadInstance, UploadProps, UploadRawFile } from 'element-plus'
import { Download, Upload, UploadFilled, User, Document, Connection, Bell } from '@element-plus/icons-vue'
import { exportData, getDataCounts, importData } from '@/api/export'

const exporting = reactive<Record<string, boolean>>({})
const importing = reactive<Record<string, boolean>>({})
const importDialogVisible = ref(false)
const importingFile = ref(false)
const importType = ref('')
const importFile = ref<File | null>(null)
const uploadRef = ref<UploadInstance>()
const currentImportType = ref<any>(null)

const exportTypes = ref([
  {
    key: 'users',
    title: '用户数据',
    description: '导出所有用户的基本信息、角色、状态等',
    icon: markRaw(User),
    color: '#409EFF',
    format: 'Excel',
    count: 0,
    importable: true
  },
  {
    key: 'projects',
    title: '项目数据',
    description: '导出所有项目的详细信息、状态、创建时间等',
    icon: markRaw(Document),
    color: '#67C23A',
    format: 'Excel',
    count: 0,
    importable: true
  },
  {
    key: 'teams',
    title: '团队数据',
    description: '导出所有团队的成员、项目关联等信息',
    icon: markRaw(Connection),
    color: '#E6A23C',
    format: 'Excel',
    count: 0,
    importable: true
  },
  {
    key: 'competitions',
    title: '比赛数据',
    description: '导出所有比赛的基本信息、参与团队等',
    icon: markRaw(Document),
    color: '#F56C6C',
    format: 'Excel',
    count: 0,
    importable: true
  },
  {
    key: 'announcements',
    title: '公告数据',
    description: '导出所有系统公告和通知记录',
    icon: markRaw(Bell),
    color: '#909399',
    format: 'CSV',
    count: 0,
    importable: false
  },
  {
    key: 'audit-logs',
    title: '操作日志',
    description: '导出所有审计日志和操作记录',
    icon: markRaw(Document),
    color: '#606266',
    format: 'CSV',
    count: 0,
    importable: false
  }
])

const loadCounts = async () => {
  try {
    const counts = await getDataCounts()
    console.log('获取到的统计数据:', counts)
    exportTypes.value.forEach(type => {
      type.count = counts[type.key] || 0
    })
  } catch (error: any) {
    console.error('加载数据统计失败:', error)
    ElMessage.error('加载数据统计失败')
  }
}

const handleExport = async (type: string) => {
  exporting[type] = true
  try {
    await exportData(type, 'excel')
    ElMessage.success('导出成功')
  } catch (error: any) {
    console.error('导出失败:', error)
    ElMessage.error(error.message || '导出失败')
  } finally {
    exporting[type] = false
  }
}

const openImport = (type: string) => {
  importType.value = type
  currentImportType.value = exportTypes.value.find(t => t.key === type)
  importFile.value = null
  uploadRef.value?.clearFiles()
  importDialogVisible.value = true
}

const handleFileChange: UploadProps['onChange'] = (uploadFile) => {
  importFile.value = uploadFile.raw || null
}

const handleExceed: UploadProps['onExceed'] = (files) => {
  uploadRef.value!.clearFiles()
  const file = files[0] as UploadRawFile
  file.uid = genFileId()
  uploadRef.value!.handleStart(file)
  importFile.value = file
}

const handleImport = async () => {
  if (!importFile.value || !importType.value) {
    ElMessage.warning('请选择文件')
    return
  }

  importingFile.value = true
  try {
    const result = await importData(importType.value, importFile.value)
    ElMessage.success(`导入成功！共导入 ${result.successCount} 条记录${result.failCount > 0 ? `，失败 ${result.failCount} 条` : ''}`)
    importDialogVisible.value = false
    importFile.value = null
    importType.value = ''
    currentImportType.value = null
    uploadRef.value?.clearFiles()
    // 刷新统计数据
    await loadCounts()
  } catch (error: any) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败')
  } finally {
    importingFile.value = false
  }
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadCounts()
})
</script>

<style scoped lang="scss">
.data-export {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;

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

  .export-card {
    margin-bottom: 20px;
    text-align: center;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    .export-icon {
      width: 64px;
      height: 64px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px;
      color: white;
    }

    .export-content {
      h3 {
        margin: 0 0 8px 0;
        font-size: 16px;
        font-weight: 600;
      }

      .export-desc {
        margin: 0 0 12px 0;
        font-size: 13px;
        color: var(--text-color-muted);
        min-height: 40px;
      }

      .export-stats {
        font-size: 12px;
        color: var(--text-color-muted);
        margin-bottom: 8px;
      }

      .button-group {
        display: flex;
        gap: 8px;
        margin-top: 16px;
      }
    }
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
  }
}
</style>
