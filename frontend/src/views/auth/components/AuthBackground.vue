<template>
  <div class="auth-background" ref="bgRef" @mousemove="handleMouseMove">
    <div class="auth-orb auth-orb--primary" :style="parallaxStyle(0.05)" />
    <div class="auth-orb auth-orb--secondary" :style="parallaxStyle(0.08)" />
    <div class="auth-orb auth-orb--tertiary" :style="parallaxStyle(0.03)" />
    <div class="auth-grid" />
    <div class="auth-particles">
      <span 
        v-for="i in 12" 
        :key="i" 
        class="auth-particle" 
        :style="[getParticleStyle(i), parallaxStyle(0.1 + i * 0.01)]" 
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const bgRef = ref<HTMLElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)
const prefersReducedMotion = ref(false)

const handleMouseMove = (e: MouseEvent) => {
  if (typeof window === 'undefined' || prefersReducedMotion.value) return
  // Normalize coordinates from -1 to 1
  mouseX.value = (e.clientX / window.innerWidth) * 2 - 1
  mouseY.value = (e.clientY / window.innerHeight) * 2 - 1
}

const parallaxStyle = (factor: number) => {
  if (prefersReducedMotion.value) {
    return { transform: 'none' }
  }
  return {
    transform: `translate3d(${mouseX.value * factor * 50}px, ${mouseY.value * factor * 50}px, 0)`
  }
}

const getParticleStyle = (index: number) => {
  // Random positioning for particles but deterministic based on index
  const seed = index * 137.508 // Golden angle approximation
  const left = 10 + (seed % 80) + '%'
  const top = 10 + ((seed * 2) % 80) + '%'
  const delay = (index % 5) + 's'
  const duration = 15 + (index % 10) + 's'
  
  return {
    left,
    top,
    animationDelay: delay,
    animationDuration: duration
  }
}

// Add global mouse listener for smoother effect even when not directly over element
onMounted(() => {
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)')
    prefersReducedMotion.value = media.matches
    const handler = (event: MediaQueryListEvent) => {
      prefersReducedMotion.value = event.matches
    }
    if (typeof media.addEventListener === 'function') {
      media.addEventListener('change', handler)
    } else if (typeof media.addListener === 'function') {
      media.addListener(handler)
    }
  }

  window.addEventListener('mousemove', handleMouseMove)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style scoped lang="scss">
.auth-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  overflow: hidden;
  background: var(--bg-body);
  transition: background 0.5s ease;
}

.auth-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
  transition: transform 0.1s cubic-bezier(0, 0, 0.2, 1);
  will-change: transform;
  
  &--primary {
    top: -10%;
    left: -10%;
    width: 50vw;
    height: 50vw;
    background: radial-gradient(circle, var(--accent-color) 0%, transparent 70%);
    opacity: 0.4;
  }

  &--secondary {
    bottom: -10%;
    right: -10%;
    width: 45vw;
    height: 45vw;
    background: radial-gradient(circle, #3b82f6 0%, transparent 70%); // Blue-500
    opacity: 0.3;
  }

  &--tertiary {
    top: 40%;
    left: 40%;
    width: 30vw;
    height: 30vw;
    background: radial-gradient(circle, #8b5cf6 0%, transparent 70%); // Violet-500
    opacity: 0.25;
    mix-blend-mode: overlay;
  }
}

.auth-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(148, 163, 184, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
}

.auth-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.auth-particle {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--accent-color), transparent);
  opacity: 0.4;
  animation: particleFloat linear infinite;
  will-change: transform;
}

@keyframes particleFloat {
  0% { transform: translate3d(0, 0, 0); opacity: 0.2; }
  50% { transform: translate3d(20px, -20px, 0); opacity: 0.5; }
  100% { transform: translate3d(0, 0, 0); opacity: 0.2; }
}

@media (prefers-reduced-motion: reduce) {
  .auth-background {
    transition: none;
  }
  .auth-orb,
  .auth-particle {
    animation: none;
    transition: none;
    transform: none !important;
  }
}
</style>
