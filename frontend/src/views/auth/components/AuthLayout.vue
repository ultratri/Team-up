<template>
  <div class="auth-layout">
    <slot name="background" />
    
    <!-- Hero Section (First Screen) -->
    <div class="auth-hero-section">
      <div class="auth-container">
        <header class="auth-header">
          <slot name="header" />
        </header>
        
        <main class="auth-main-split">
          <section class="auth-split-left">
            <div class="auth-hero-content">
              <slot name="hero" />
            </div>
          </section>
          
          <section class="auth-split-right">
            <div class="auth-card-wrapper">
              <slot name="form" />
            </div>
          </section>
        </main>
        
        <!-- Scroll Indicator -->
        <div
          class="scroll-indicator"
          role="button"
          tabindex="0"
          @click="scrollToContent"
          @keydown.enter.prevent="scrollToContent"
          @keydown.space.prevent="scrollToContent"
        >
          <span>了解更多</span>
          <div class="scroll-arrow"></div>
        </div>
      </div>
    </div>
    
    <!-- Extended Content Section (Apple Style) -->
    <div class="auth-content-section" id="auth-content">
      <div class="auth-container">
        <slot name="features" />
      </div>
      
      <footer class="auth-global-footer">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
const scrollToContent = () => {
  const el = document.getElementById('auth-content')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' })
  }
}
</script>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

.auth-container {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 40px;
}

/* Hero Section */
.auth-hero-section {
  min-height: 100vh; /* Full Viewport */
  display: flex;
  flex-direction: column;
  position: relative;
}

.auth-header {
  padding-top: 40px;
  margin-bottom: 60px;
  z-index: 10;
}

.auth-main-split {
  flex: 1;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 80px;
  align-items: center;
  padding-bottom: 100px; /* Space for scroll indicator */
}

.auth-split-left {
  padding-right: 20px;
  animation: fadeSlideRight 1s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
}

.auth-split-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  animation: fadeSlideLeft 1s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  animation-delay: 0.1s;
  opacity: 0;
}

.auth-card-wrapper {
  width: 100%;
  max-width: 480px;
  perspective: 1000px;
}

/* Scroll Indicator */
.scroll-indicator {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.3s;
  animation: bounce 2s infinite;
  z-index: 5;
  
  span {
    font-size: 14px;
    font-weight: 500;
    letter-spacing: 0.05em;
    color: var(--text-color);
  }
  
  &:hover {
    opacity: 1;
  }
}

.scroll-arrow {
  width: 24px;
  height: 24px;
  border-right: 2px solid var(--text-color);
  border-bottom: 2px solid var(--text-color);
  transform: rotate(45deg);
}

/* Content Section */
.auth-content-section {
  padding: 120px 0 60px;
  background: linear-gradient(to bottom, transparent, var(--bg-body));
  position: relative;
  z-index: 1;
}

.auth-global-footer {
  margin-top: 120px;
  padding: 40px 0;
  text-align: center;
  border-top: 1px solid var(--border-subtle);
  color: var(--text-color-muted);
  font-size: 14px;
}

@keyframes fadeSlideRight {
  from { opacity: 0; transform: translateX(-30px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes fadeSlideLeft {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateX(-50%) translateY(0); }
  40% { transform: translateX(-50%) translateY(-10px); }
  60% { transform: translateX(-50%) translateY(-5px); }
}

/* Responsive */
@media (max-width: 1024px) {
  .auth-main-split {
    grid-template-columns: 1fr;
    gap: 60px;
    text-align: center;
    padding-top: 40px;
  }
  
  .auth-split-left {
    padding-right: 0;
    display: flex;
    justify-content: center;
  }
  
  .auth-split-right {
    align-items: center;
  }
  
  .auth-card-wrapper {
    max-width: 500px;
  }
}

@media (max-width: 768px) {
  .auth-container {
    padding: 0 24px;
  }
  
  .auth-header {
    margin-bottom: 40px;
  }
}
</style>
