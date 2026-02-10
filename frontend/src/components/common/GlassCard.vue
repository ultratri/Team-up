<script setup lang="ts">
defineProps<{
  hoverEffect?: boolean
  noPadding?: boolean
}>()
</script>

<template>
  <div class="glass-card" :class="{ 'hover-effect': hoverEffect, 'no-padding': noPadding }">
    <slot />
  </div>
</template>

<style scoped lang="scss">
.glass-card {
  background: var(--glass-bg-light);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: 24px;
  transition: all 0.3s var(--ease-spring);
  position: relative;
  overflow: hidden;

  &.no-padding {
    padding: 0;
  }

  &.hover-effect:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-card-hover);
  }
}

[data-theme='dark'] .glass-card {
  background: var(--glass-bg-dark);
}
</style>
