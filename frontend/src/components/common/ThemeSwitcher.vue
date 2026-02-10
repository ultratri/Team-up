<template>
  <div class="theme-switcher">
    <div class="theme-switcher__modes" role="radiogroup" aria-label="主题模式">
      <button
        type="button"
        class="theme-chip"
        :class="{ 'theme-chip--active': theme.mode === 'system' }"
        @click="changeMode('system', $event)"
      >
        系统
      </button>
      <button
        type="button"
        class="theme-chip"
        :class="{ 'theme-chip--active': theme.mode === 'light' }"
        @click="changeMode('light', $event)"
      >
        浅色
      </button>
      <button
        type="button"
        class="theme-chip"
        :class="{ 'theme-chip--active': theme.mode === 'dark' }"
        @click="changeMode('dark', $event)"
      >
        深色
      </button>
    </div>
    <label class="theme-switcher__color">
      <span class="theme-switcher__color-label">主题色</span>
      <input
        class="theme-switcher__color-input"
        type="color"
        :value="theme.primaryColor"
        @input="onColorChange"
        aria-label="选择主题主色"
      />
    </label>
    <button 
      type="button" 
      class="theme-reset-btn" 
      @click="theme.resetToDefault()"
      title="重置为默认主题"
    >
      重置
    </button>
  </div>
</template>

<script setup lang="ts">
import { useThemeStore } from '../../store/theme'

const theme = useThemeStore()

const changeMode = (mode: 'system' | 'light' | 'dark', event: MouseEvent) => {
  if (!document.startViewTransition) {
    theme.setMode(mode);
    return;
  }

  const x = event.clientX;
  const y = event.clientY;
  const endRadius = Math.hypot(
    Math.max(x, window.innerWidth - x),
    Math.max(y, window.innerHeight - y)
  );

  const transition = document.startViewTransition(async () => {
    theme.setMode(mode);
  });

  transition.ready.then(() => {
    const clipPath = [
      `circle(0px at ${x}px ${y}px)`,
      `circle(${endRadius}px at ${x}px ${y}px)`,
    ];
    
    document.documentElement.animate(
      {
        clipPath: clipPath,
      },
      {
        duration: 500,
        easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
        pseudoElement: '::view-transition-new(root)',
      }
    );
  });
};

const onColorChange = (event: Event) => {
  const target = event.target as HTMLInputElement | null
  if (target && target.value) {
    theme.setPrimaryColor(target.value)
  }
}
</script>

<style scoped lang="scss">
.theme-switcher {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(148, 163, 184, 0.4);
  box-shadow: 0 18px 35px rgba(15, 23, 42, 0.12);
  gap: 10px;
}

.theme-switcher__modes {
  display: inline-flex;
  background: rgba(15, 23, 42, 0.04);
  border-radius: 999px;
  padding: 2px;
}

.theme-chip {
  border: none;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  color: var(--text-color-muted);
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}

.theme-chip--active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.18);
  transform: translateY(-1px);
}

[data-theme='dark'] .theme-chip--active {
  background: rgba(15, 23, 42, 0.9);
  color: #e5e7eb;
}

.theme-chip:not(.theme-chip--active):hover {
  background: rgba(255, 255, 255, 0.22);
}

.theme-switcher__color {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-color-muted);
}

.theme-switcher__color-input {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  padding: 0;
  border-radius: 999px;
  border: 2px solid rgba(148, 163, 184, 0.7);
  background: transparent;
  cursor: pointer;
  overflow: hidden;
}

.theme-switcher__color-input::-webkit-color-swatch {
  border-radius: 999px;
  border: none;
}

.theme-switcher__color-input::-moz-color-swatch {
  border-radius: 999px;
  border: none;
}

.theme-reset-btn {
  border: none;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  color: var(--text-color-muted);
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(148, 163, 184, 0.2);

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    color: var(--accent-color);
    border-color: var(--accent-color);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}
</style>