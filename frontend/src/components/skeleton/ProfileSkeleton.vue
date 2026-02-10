<template>
  <div class="profile-skeleton">
    <!-- Header Skeleton -->
    <div class="skeleton-header">
      <el-skeleton animated>
        <template #template>
          <div class="header-content">
            <el-skeleton-item variant="image" style="width: 80px; height: 80px; border-radius: 50%;" />
            <div class="header-info">
              <el-skeleton-item variant="h1" style="width: 150px;" />
              <el-skeleton-item variant="text" style="width: 200px; margin-top: 12px;" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
    
    <!-- Grid Content Skeleton -->
    <div class="skeleton-grid">
      <!-- Left Column -->
      <div class="left-column">
        <div class="skeleton-card">
          <el-skeleton animated :rows="4" />
        </div>
        <div class="skeleton-card">
          <el-skeleton animated :rows="3" />
        </div>
      </div>
      
      <!-- Right Column -->
      <div class="right-column">
        <div class="skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 200px; border-radius: 8px;" />
            </template>
          </el-skeleton>
        </div>
        <div class="skeleton-card">
          <el-skeleton animated :rows="2" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.profile-skeleton {
  max-width: 1200px;
  margin: 0 auto;
  
  .skeleton-header {
    background: var(--card-bg);
    border-radius: 16px;
    padding: 32px;
    margin-bottom: 24px;
    border: 1px solid var(--border-subtle);
    
    .header-content {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .header-info {
        flex: 1;
      }
    }
  }
  
  .skeleton-grid {
    display: grid;
    grid-template-columns: 1fr 360px;
    gap: 24px;
    
    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }
  }
  
  .left-column,
  .right-column {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
  
  .skeleton-card {
    background: var(--card-bg);
    border-radius: 16px;
    padding: 24px;
    border: 1px solid var(--border-subtle);
  }
}
</style>
