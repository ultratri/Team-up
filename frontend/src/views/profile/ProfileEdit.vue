<template>
  <div class="profile-edit-container">
    <el-row :gutter="20">
      <!-- 左侧：档案编辑 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <h3>个人档案</h3>
          </template>

          <el-form :model="profileForm" label-width="100px">
            <el-form-item label="真实姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>

            <el-form-item label="院系">
              <el-input v-model="profileForm.department" placeholder="请输入院系" />
            </el-form-item>

            <el-form-item label="专业">
              <el-input v-model="profileForm.major" placeholder="请输入专业" />
            </el-form-item>

            <el-form-item label="年级">
              <el-input-number v-model="profileForm.grade" :min="1" :max="5" />
            </el-form-item>

            <el-form-item label="微信号">
              <el-input v-model="profileForm.wechat" placeholder="请输入微信号" />
            </el-form-item>

            <el-form-item label="QQ号">
              <el-input v-model="profileForm.qq" placeholder="请输入QQ号" />
            </el-form-item>

            <el-form-item label="个人简介">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己..."
              />
            </el-form-item>

            <el-form-item label="项目经验">
              <el-input
                v-model="profileForm.projectExperience"
                type="textarea"
                :rows="4"
                placeholder="描述你的项目经验..."
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
              <el-button @click="loadProfile">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 能力雷达图 -->
        <div class="mt-20">
          <AbilityRadar />
        </div>

        <!-- 项目履历 -->
        <div class="mt-20">
          <ProjectHistory />
        </div>
      </el-col>

      <!-- 右侧：技能和信誉 -->
      <el-col :span="8">
        <!-- 信誉积分 -->
        <CreditDisplay />

        <!-- 技能标签管理已移至 /profile 页面 -->
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { getProfile, updateProfile } from '../../api/profile'
import { useAuthStore } from '../../store/auth'
import AbilityRadar from '../../components/charts/AbilityRadar.vue'
import CreditDisplay from '../../components/profile/CreditDisplay.vue'
import ProjectHistory from './ProjectHistory.vue'

const authStore = useAuthStore()
const loading = ref(false)

const profileForm = reactive({
  realName: '',
  department: '',
  major: '',
  grade: 1,
  wechat: '',
  qq: '',
  bio: '',
  projectExperience: '',
})

const loadProfile = async () => {
  if (!authStore.user?.id) return

  try {
    const res = await getProfile(authStore.user.id)
    if (res) {
      Object.assign(profileForm, res)
    }
  } catch (error) {
    console.error(error)
  }
}

const handleSave = async () => {
  if (!authStore.user?.id) return

  loading.value = true
  try {
    await updateProfile(authStore.user.id, profileForm)
    ElMessage.success('保存成功')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-edit-container {
  padding: 20px;

  .skill-card-link {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
    }
  }
}
</style>
