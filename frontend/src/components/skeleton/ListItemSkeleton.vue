<template>
  <div class="list-item-skeleton">
    <el-skeleton animated>
      <template #template>
        <div class="item-content">
          <el-skeleton-item variant="image" style="width: 48px; height: 48px; border-radius: 8px;" />
          <div class="item-info">
            <el-skeleton-item variant="h3" style="width: 70%;" />
            <el-skeleton-item variant="text" style="width: 50%; margin-top: 8px;" />
          </div>
          <div class="item-action">
            <el-skeleton-item variant="button" style="width: 80px; height: 32px;" />
          </div>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped lang="scss">
.list-item-skeleton {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--border-subtle);
  
  .item-content {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .item-info {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    
    .item-action {
      display: flex;
      align-items: center;
    }
  }
}
</style>
