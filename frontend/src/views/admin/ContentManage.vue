<template>
  <div class="content-manage-container">
    <el-card class="header-card">
      <h2>内容管理</h2>
      <p class="subtitle">管理平台的举报、标签、公告和新手保护功能</p>
    </el-card>

    <div class="quick-links">
      <el-card 
        v-for="item in quickLinks" 
        :key="item.path"
        class="quick-link-card"
        shadow="hover"
        @click="router.push(item.path)"
      >
        <div class="card-content">
          <el-icon :size="40" class="card-icon">
            <component :is="item.icon" />
          </el-icon>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <el-tag v-if="item.count !== undefined" type="info" size="large">
            {{ item.count }} 条
          </el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Warning, PriceTag, Bell, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()

const quickLinks = ref([
  {
    title: '举报管理',
    description: '处理用户举报的不当内容',
    icon: Warning,
    path: '/admin/reports',
    count: undefined
  },
  {
    title: '标签管理',
    description: '管理技能、兴趣、性格等标签',
    icon: PriceTag,
    path: '/admin/tags',
    count: undefined
  },
  {
    title: '公告管理',
    description: '发布和管理系统公告',
    icon: Bell,
    path: '/admin/announcements',
    count: undefined
  },
  {
    title: '新手保护',
    description: '管理新手保护期设置',
    icon: UserFilled,
    path: '/admin/newbie',
    count: undefined
  }
])
</script>

<style scoped lang="scss">
.content-manage-container {
  padding: 24px;
}

.header-card {
  margin-bottom: 24px;
  
  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    color: var(--text-color);
  }
  
  .subtitle {
    margin: 0;
    color: var(--text-color-muted);
    font-size: 14px;
  }
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.quick-link-card {
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
  
  .card-content {
    text-align: center;
    padding: 20px;
    
    .card-icon {
      color: var(--accent-color);
      margin-bottom: 16px;
    }
    
    h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      color: var(--text-color);
    }
    
    p {
      margin: 0 0 16px 0;
      color: var(--text-color-muted);
      font-size: 14px;
      min-height: 40px;
    }
  }
}
</style>
