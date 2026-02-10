<template>
  <div class="newbie-protection-manage">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 新手保护配置 -->
      <el-tab-pane label="新手保护配置" name="config">
        <el-card v-loading="configLoading">
          <template #header>
            <span>新手保护参数设置</span>
          </template>
          
          <el-form :model="config" label-width="150px" style="max-width: 600px;">
            <el-form-item label="新手保护期">
              <el-input-number 
                v-model="config.protectionDays" 
                :min="7" 
                :max="90"
              />
              <span style="margin-left: 10px;">天</span>
              <div style="margin-top: 5px; font-size: 12px; color: #999;">
                注册后多少天内享受新手保护
              </div>
            </el-form-item>
            
            <el-form-item label="新手基础信誉分">
              <el-input-number 
                v-model="config.baseReputationScore" 
                :min="0" 
                :max="50"
              />
              <span style="margin-left: 10px;">分</span>
              <div style="margin-top: 5px; font-size: 12px; color: #999;">
                新手用户的初始信誉分数
              </div>
            </el-form-item>
            
            <el-form-item label="新手匹配加成">
              <el-input-number 
                v-model="config.matchingBonus" 
                :min="0" 
                :max="20"
              />
              <span style="margin-left: 10px;">分</span>
              <div style="margin-top: 5px; font-size: 12px; color: #999;">
                新手在匹配时额外获得的分数
              </div>
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="saveConfig" :loading="saving">
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 新手任务配置 -->
        <el-card style="margin-top: 20px;" v-loading="tasksLoading">
          <template #header>
            <span>新手任务配置</span>
          </template>
          
          <el-table :data="tasks" style="width: 100%">
            <el-table-column prop="taskName" label="任务名称" width="180" />
            <el-table-column prop="taskDescription" label="任务描述" />
            <el-table-column prop="rewardPoints" label="奖励积分" width="100">
              <template #default="{ row }">
                <el-tag type="success">{{ row.rewardPoints }} 分</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="isActive" label="状态" width="100">
              <template #default="{ row }">
                <el-switch 
                  v-model="row.isActive" 
                  @change="updateTaskStatus(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" @click="editTask(row)">
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      
      <!-- 技能认证审核 -->
      <el-tab-pane label="技能认证审核" name="certifications">
        <el-card v-loading="certificationsLoading">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>待审核的技能认证</span>
              <el-tag type="warning">{{ certifications.total }} 条待审核</el-tag>
            </div>
          </template>
          
          <el-table :data="certifications.records" style="width: 100%">
            <el-table-column prop="userName" label="用户" width="120" />
            <el-table-column prop="skillName" label="技能名称" width="150" />
            <el-table-column prop="skillCategory" label="分类" width="120" />
            <el-table-column prop="proficiencyLevelText" label="熟练度" width="100">
              <template #default="{ row }">
                <el-tag 
                  :type="getProficiencyTagType(row.proficiencyLevel)"
                  size="small"
                >
                  {{ row.proficiencyLevelText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="证明材料" width="120">
              <template #default="{ row }">
                <el-link 
                  v-if="row.proofUrl" 
                  :href="row.proofUrl" 
                  target="_blank"
                  type="primary"
                >
                  查看
                </el-link>
                <span v-else style="color: #999;">无</span>
              </template>
            </el-table-column>
            <el-table-column prop="proofDescription" label="说明" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="160">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button 
                  link 
                  type="success" 
                  @click="approveCertification(row)"
                  :loading="row.approving"
                >
                  通过
                </el-button>
                <el-button 
                  link 
                  type="danger" 
                  @click="showRejectDialog(row)"
                  :loading="row.rejecting"
                >
                  拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-pagination
            v-if="certifications.total > 0"
            style="margin-top: 20px; justify-content: center;"
            :current-page="certificationPage"
            :page-size="certificationSize"
            :total="certifications.total"
            layout="total, prev, pager, next"
            @current-change="handleCertificationPageChange"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 拒绝技能认证对话框 -->
    <el-dialog 
      v-model="rejectDialogVisible" 
      title="拒绝技能认证"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝理由">
          <el-input 
            v-model="rejectForm.reason" 
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝理由"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button 
          type="danger" 
          @click="confirmReject"
          :loading="rejecting"
        >
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 编辑任务对话框 -->
    <el-dialog 
      v-model="editTaskDialogVisible" 
      title="编辑新手任务"
      width="600px"
    >
      <el-form :model="editingTask" label-width="100px">
        <el-form-item label="任务名称">
          <el-input v-model="editingTask.taskName" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input 
            v-model="editingTask.taskDescription" 
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="奖励积分">
          <el-input-number v-model="editingTask.rewardPoints" :min="0" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="editingTask.displayOrder" :min="0" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editTaskDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="saveTask"
          :loading="savingTask"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';

const activeTab = ref('config');

// 新手保护配置
const config = ref({
  id: null,
  protectionDays: 30,
  baseReputationScore: 15,
  matchingBonus: 5
});
const configLoading = ref(false);
const saving = ref(false);

// 新手任务
const tasks = ref([]);
const tasksLoading = ref(false);
const editTaskDialogVisible = ref(false);
const editingTask = ref({});
const savingTask = ref(false);

// 技能认证
const certifications = ref({
  records: [],
  total: 0
});
const certificationsLoading = ref(false);
const certificationPage = ref(1);
const certificationSize = ref(20);
const rejectDialogVisible = ref(false);
const rejectForm = ref({
  certificationId: null,
  reason: ''
});
const rejecting = ref(false);

// 加载新手保护配置
const loadConfig = async () => {
  configLoading.value = true;
  try {
    const response = await request.get('/api/admin/newbie/config');
    if (response.code === 200) {
      config.value = response.data;
    }
  } catch (error) {
    ElMessage.error('加载配置失败');
  } finally {
    configLoading.value = false;
  }
};

// 保存配置
const saveConfig = async () => {
  saving.value = true;
  try {
    const response = await request.put('/api/admin/newbie/config', config.value);
    if (response.code === 200) {
      ElMessage.success('保存成功');
    }
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

// 加载新手任务
const loadTasks = async () => {
  tasksLoading.value = true;
  try {
    const response = await request.get('/api/admin/newbie/tasks');
    if (response.code === 200) {
      tasks.value = response.data;
    }
  } catch (error) {
    ElMessage.error('加载任务失败');
  } finally {
    tasksLoading.value = false;
  }
};

// 更新任务状态
const updateTaskStatus = async (task: any) => {
  try {
    const response = await request.put(`/api/admin/newbie/tasks/${task.id}`, task);
    if (response.code === 200) {
      ElMessage.success('更新成功');
    }
  } catch (error) {
    ElMessage.error('更新失败');
    task.isActive = !task.isActive; // 回滚
  }
};

// 编辑任务
const editTask = (task: any) => {
  editingTask.value = { ...task };
  editTaskDialogVisible.value = true;
};

// 保存任务
const saveTask = async () => {
  savingTask.value = true;
  try {
    const response = await request.put(
      `/api/admin/newbie/tasks/${editingTask.value.id}`, 
      editingTask.value
    );
    if (response.code === 200) {
      ElMessage.success('保存成功');
      editTaskDialogVisible.value = false;
      loadTasks();
    }
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    savingTask.value = false;
  }
};

// 加载待审核的技能认证
const loadCertifications = async () => {
  certificationsLoading.value = true;
  try {
    const response = await request.get('/api/admin/newbie/certifications/pending', {
      params: {
        page: certificationPage.value,
        size: certificationSize.value
      }
    });
    if (response.code === 200) {
      certifications.value = response.data;
    }
  } catch (error) {
    ElMessage.error('加载认证列表失败');
  } finally {
    certificationsLoading.value = false;
  }
};

// 通过技能认证
const approveCertification = async (cert: any) => {
  try {
    await ElMessageBox.confirm(
      `确认通过 ${cert.userName} 的 ${cert.skillName} 技能认证？`,
      '确认操作',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'success'
      }
    );
    
    cert.approving = true;
    const response = await request.post(
      `/api/admin/newbie/certifications/${cert.id}/approve`
    );
    
    if (response.code === 200) {
      ElMessage.success('审核通过');
      loadCertifications();
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败');
    }
  } finally {
    cert.approving = false;
  }
};

// 显示拒绝对话框
const showRejectDialog = (cert: any) => {
  rejectForm.value = {
    certificationId: cert.id,
    reason: ''
  };
  rejectDialogVisible.value = true;
};

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.value.reason) {
    ElMessage.warning('请输入拒绝理由');
    return;
  }
  
  rejecting.value = true;
  try {
    const response = await request.post(
      `/api/admin/newbie/certifications/${rejectForm.value.certificationId}/reject`,
      { reason: rejectForm.value.reason }
    );
    
    if (response.code === 200) {
      ElMessage.success('已拒绝');
      rejectDialogVisible.value = false;
      loadCertifications();
    }
  } catch (error) {
    ElMessage.error('操作失败');
  } finally {
    rejecting.value = false;
  }
};

// 分页变化
const handleCertificationPageChange = (page: number) => {
  certificationPage.value = page;
  loadCertifications();
};

// 获取熟练度标签类型
const getProficiencyTagType = (level: string) => {
  switch (level) {
    case 'BEGINNER': return 'info';
    case 'INTERMEDIATE': return 'warning';
    case 'EXPERT': return 'success';
    default: return 'info';
  }
};

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '';
  return new Date(dateTime).toLocaleString('zh-CN');
};

onMounted(() => {
  loadConfig();
  loadTasks();
  loadCertifications();
});
</script>

<style scoped>
.newbie-protection-manage {
  padding: 20px;
}
</style>
