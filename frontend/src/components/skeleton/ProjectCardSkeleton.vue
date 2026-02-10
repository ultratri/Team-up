<template>
  <div class="project-card-skeleton">
    <el-skeleton animated>
      <template #template>
        <div class="skeleton-header">
          <el-skeleton-item variant="image" style="width: 60px; height: 60px; border-radius: 12px;" />
          <div class="skeleton-title-area">
            <el-skeleton-item variant="h3" style="width: 60%;" />
            <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px;" />
          </div>
        </div>
        
        <div class="skeleton-tags">
          <el-skeleton-item variant="text" style="width: 60px; height: 24px; border-radius: 12px;" />
          <el-skeleton-item variant="text" style="width: 80px; height: 24px; border-radius: 12px;" />
          <el-skeleton-item variant="text" style="width: 70px; height: 24px; border-radius: 12px;" />
        </div>
        
        <div class="skeleton-description">
          <el-skeleton-item variant="text" style="width: 100%;" />
          <el-skeleton-item variant="text" style="width: 80%;" />
          <el-skeleton-item variant="text" style="width: 90%;" />
        </div>
        
        <div class="skeleton-footer">
          <el-skeleton-item variant="text" style="width: 100px;" />
          <el-skeleton-item variant="button" style="width: 80px; height: 32px;" />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped lang="scss">
.project-card-skeleton {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-subtle);
  
  .skeleton-header {
    display: flex;
    gap: 16px;
    margin-bottom: 20px;
    
    .skeleton-title-area {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
  }
  
  .skeleton-tags {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }
  
  .skeleton-description {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 20px;
  }
  
  .skeleton-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
