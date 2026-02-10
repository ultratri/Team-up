<script setup lang="ts">
import { computed } from 'vue'
import { Edit } from '@element-plus/icons-vue'

const props = defineProps<{
  user: {
    name: string
    avatar?: string
    cover?: string
    tagline?: string
    role?: string
  }
  editable?: boolean
}>()

const emit = defineEmits<{
  (e: 'edit'): void
}>()

const defaultCover = 'https://images.unsplash.com/photo-1579546929518-9e396f3cc809?ixlib=rb-1.2.1&auto=format&fit=crop&w=1000&q=80'
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const coverStyle = computed(() => ({
  backgroundImage: `url(${props.user.cover || defaultCover})`
}))
</script>

<template>
  <div class="profile-header-poster">
    <div class="poster-hero">
      <!-- Background Layer -->
      <div class="hero-bg-layer" :style="coverStyle">
        <div class="poster-overlay"></div>
        <div class="poster-decoration">{{ user.name }}</div>
      </div>
      
      <!-- Content Layer: Directly pushes the height of .poster-hero -->
      <div class="poster-content">
        <div class="avatar-section">
          <div class="avatar-aura">
            <div class="avatar-container">
              <img :src="props.user.avatar || defaultAvatar" alt="avatar" class="avatar-img" />
              <div class="online-status"></div>
            </div>
          </div>
        </div>

        <div class="hero-info">
          <div class="role-chip">{{ user.role || 'MEMBER' }}</div>
          <h1 class="hero-name">{{ user.name }}</h1>
          <div class="hero-tagline-container">
            <p class="hero-tagline">{{ user.tagline || '探索无限可能，共建梦之队' }}</p>
          </div>
          
          <div class="hero-actions" v-if="editable">
            <button class="btn-primary-glass" @click="emit('edit')">
              <el-icon><Edit /></el-icon>
              <span>编辑个人资料</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.profile-header-poster {
  width: 100%;
  margin-bottom: 40px;
}

.poster-hero {
  position: relative;
  width: 100%;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  border-radius: 40px;
  overflow: hidden;
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.25);
}

.hero-bg-layer {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  z-index: 0;
}

.poster-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.02) 0%,
    rgba(0, 0, 0, 0.3) 40%,
    rgba(var(--accent-color-rgb), 0.15) 75%,
    var(--bg-card) 100%
  );
  z-index: 1;
}

[data-theme='dark'] .poster-overlay {
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.2) 0%,
    rgba(0, 0, 0, 0.6) 40%,
    rgba(var(--accent-color-rgb), 0.1) 75%,
    var(--bg-body) 100%
  );
}

.poster-decoration {
  position: absolute;
  top: 5%;
  right: -2%;
  font-size: 160px;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.04);
  text-transform: uppercase;
  white-space: nowrap;
  pointer-events: none;
  user-select: none;
  z-index: 0;
  line-height: 1;
}

[data-theme='dark'] .poster-decoration {
  color: rgba(255, 255, 255, 0.02);
}

.poster-content {
  position: relative;
  z-index: 2;
  padding: 160px 60px 60px; 
  display: flex;
  align-items: flex-start;
  gap: 48px;
  width: 100%;
}

.avatar-aura {
  position: relative;
  padding: 10px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(15px);
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 0 40px rgba(var(--accent-color-rgb), 0.3);
  animation: pulse-aura 4s infinite alternate;
  flex-shrink: 0;
}

[data-theme='dark'] .avatar-aura {
  background: rgba(var(--accent-color-rgb), 0.1);
  border-color: rgba(var(--accent-color-rgb), 0.22);
  box-shadow: 0 0 60px rgba(var(--accent-color-rgb), 0.22);
}

@keyframes pulse-aura {
  from { box-shadow: 0 0 30px rgba(var(--accent-color-rgb), 0.2); }
  to { box-shadow: 0 0 60px rgba(var(--accent-color-rgb), 0.5); }
}

.avatar-container {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  overflow: hidden;
  border: 6px solid var(--bg-card);
  background: var(--bg-card);
  position: relative;

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.8s ease;
  }

  &:hover .avatar-img {
    transform: scale(1.15);
  }
}

.online-status {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 24px;
  height: 24px;
  background: #10b981;
  border: 4px solid var(--bg-card);
  border-radius: 50%;
  z-index: 3;
}

.hero-info {
  flex: 1;
  min-width: 0;
  
  .role-chip {
    display: inline-block;
    padding: 4px 14px;
    background: var(--accent-color);
    color: white;
    border-radius: 100px;
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.1em;
    margin-bottom: 12px;
  }

  .hero-name {
    font-size: 52px;
    font-weight: 900;
    margin: 0 0 16px;
    line-height: 1;
    letter-spacing: -0.04em;
    background: linear-gradient(to right, var(--text-color) 20%, var(--accent-color));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .hero-tagline-container {
    max-width: 850px;
    margin-bottom: 32px;
  }

  .hero-tagline {
    font-size: 18px;
    font-weight: 500;
    color: var(--text-color-muted);
    margin: 0;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

[data-theme='dark'] {
  .hero-name {
    background: linear-gradient(to right, #ffffff 10%, var(--accent-color));
    -webkit-background-clip: text;
  }
  
  .hero-tagline {
    color: #ffffff;
    text-shadow: 0 2px 10px rgba(0,0,0,0.5);
  }
}

.btn-primary-glass {
  background: rgba(var(--accent-color-rgb), 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--accent-color-rgb), 0.2);
  color: var(--accent-color);
  padding: 14px 32px;
  border-radius: 100px;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background: var(--accent-color);
    color: white;
    transform: translateY(-2px);
    box-shadow: 0 10px 20px -5px rgba(var(--accent-color-rgb), 0.4);
  }
}

@media (max-width: 1024px) {
  .poster-content { 
    flex-direction: column; 
    align-items: center;
    text-align: center;
    padding: 60px 30px 40px;
  }
  .hero-name { font-size: 42px; }
  .poster-decoration { font-size: 100px; }
  .hero-tagline-container { margin-left: auto; margin-right: auto; }
}
</style>
