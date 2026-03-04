<template>
  <div class="credit-display">
    <el-card>
      <template #header>
        <h3>信誉积分</h3>
      </template>

      <div class="credit-content">
        <div class="credit-score">
          <div class="score-number">{{ credit.totalCredit || 0 }}</div>
          <div class="score-label">总积分</div>
        </div>

        <div class="credit-level">
          <el-tag :type="getLevelType(credit.creditLevel)" size="large">
            {{ getLevelText(credit.creditLevel) }}
          </el-tag>
        </div>

        <div class="credit-progress">
          <el-progress
            :percentage="getCreditProgress(credit.totalCredit)"
            :color="getProgressColor(credit.creditLevel)"
          />
          <div class="progress-text">
            距离下一等级还需 {{ getNextLevelGap(credit.totalCredit) }} 分
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserCredit } from '../../api/profile'
import { useAuthStore } from '../../store/auth'
import type { UserCredit } from '../../types/user'

const authStore = useAuthStore()
const credit = ref<Partial<UserCredit>>({
  totalCredit: 0,
  creditLevel: 'NEWBIE',
})

const getLevelType = (level?: string) => {
  const types: Record<string, any> = {
    NEWBIE: 'info',
    RELIABLE: 'success',
    EXCELLENT: 'warning',
    OUTSTANDING: 'danger',
  }
  return types[level || 'NEWBIE']
}

const getLevelText = (level?: string) => {
  const texts: Record<string, string> = {
    NEWBIE: '新手 (<60)',
    RELIABLE: '可靠 (60-79)',
    EXCELLENT: '优秀 (80-94)',
    OUTSTANDING: '卓越 (95+)',
  }
  return texts[level || 'NEWBIE']
}

const getProgressColor = (level?: string) => {
  const colors: Record<string, string> = {
    NEWBIE: '#909399',
    RELIABLE: '#67C23A',
    EXCELLENT: '#E6A23C',
    OUTSTANDING: '#F56C6C',
  }
  return colors[level || 'NEWBIE']
}

const getCreditProgress = (totalCredit?: number) => {
  const score = totalCredit || 0
  // NEWBIE: 0-59 (60分满)
  if (score < 60) return (score / 60) * 100
  // RELIABLE: 60-79 (20分满)
  if (score < 80) return ((score - 60) / 20) * 100
  // EXCELLENT: 80-94 (15分满)
  if (score < 95) return ((score - 80) / 15) * 100
  // OUTSTANDING: 95+ (已满级)
  return 100
}

const getNextLevelGap = (totalCredit?: number) => {
  const score = totalCredit || 0
  if (score < 60) return 60 - score
  if (score < 80) return 80 - score
  if (score < 95) return 95 - score
  return 0 // 已达到最高等级
}

const loadCredit = async () => {
  if (!authStore.user?.id) return

  try {
    const res = await getUserCredit(authStore.user.id)
    credit.value = res as unknown as UserCredit
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadCredit()
})
</script>

<style scoped lang="scss">
.credit-display {
  .credit-content {
    text-align: center;
  }

  .credit-score {
    margin-bottom: 20px;

    .score-number {
      font-size: 48px;
      font-weight: bold;
      color: #409eff;
    }

    .score-label {
      font-size: 14px;
      color: #909399;
      margin-top: 8px;
    }
  }

  .credit-level {
    margin-bottom: 20px;
  }

  .credit-progress {
    margin-top: 20px;

    .progress-text {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>

