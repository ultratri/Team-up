<template>
  <div class="auth-features">
    <div class="feature-grid">
      <!-- Feature 1: Large Card -->
      <div class="feature-card feature-card--large feature-card--gradient-1">
        <div class="feature-card__content">
          <span class="feature-tag">智能匹配</span>
          <h3>不仅仅是组队，<br>更是寻找灵魂契合的伙伴</h3>
          <p>基于深度学习的技能图谱分析，为您推荐最互补的合作伙伴。告别盲目寻找，让每一个团队都具备成功的基因。</p>
        </div>
        <div class="feature-card__visual visual-matching">
          <!-- Abstract visual representation of matching -->
          <div class="matching-node node-1"></div>
          <div class="matching-node node-2"></div>
          <div class="matching-line"></div>
        </div>
      </div>

      <!-- Feature 2: Medium Card -->
      <div class="feature-card feature-card--medium feature-card--dark">
        <div class="feature-card__content">
          <span class="feature-tag">实时协作</span>
          <h3>无缝沟通</h3>
          <p>集成的即时通讯与文件共享，让创意零延迟传递。</p>
        </div>
        <div class="feature-card__visual visual-chat">
          <div class="chat-bubble bubble-1"></div>
          <div class="chat-bubble bubble-2"></div>
        </div>
      </div>

      <!-- Feature 3: Medium Card -->
      <div class="feature-card feature-card--medium feature-card--light">
        <div class="feature-card__content">
          <span class="feature-tag">项目管理</span>
          <h3>全程把控</h3>
          <p>从立项到交付，全生命周期的任务追踪与进度管理。</p>
        </div>
        <div class="feature-card__visual visual-kanban">
          <div class="kanban-card card-1"></div>
          <div class="kanban-card card-2"></div>
        </div>
      </div>
    </div>
    
    <!-- Call to Action Section -->
    <div class="cta-section">
      <h2>准备好开始了吗？</h2>
      <p>加入数千名优秀开发者的行列，开启您的创新之旅。</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.auth-features {
  width: 100%;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 80px;
}

.feature-card {
  border-radius: 32px;
  overflow: hidden;
  position: relative;
  padding: 48px;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  }
}

.feature-card--large {
  grid-column: span 2;
  min-height: 500px;
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  
  h3 {
    font-size: 48px;
    line-height: 1.1;
    margin-bottom: 24px;
    background: linear-gradient(120deg, #1e293b, #475569);
    -webkit-background-clip: text;
    color: transparent;
  }
  
  p {
    font-size: 20px;
    max-width: 600px;
    color: #64748b;
  }
}

.feature-card--medium {
  min-height: 400px;
  
  h3 {
    font-size: 32px;
    margin-bottom: 16px;
  }
  
  p {
    font-size: 18px;
    opacity: 0.8;
  }
}

.feature-card--dark {
  background: #1e293b;
  color: white;
  
  .feature-tag {
    background: rgba(255, 255, 255, 0.1);
    color: white;
  }
  
  p {
    color: #94a3b8;
  }
}

.feature-card--light {
  background: white;
  border: 1px solid var(--border-subtle);
  
  h3 {
    color: #1e293b;
  }
  
  p {
    color: #64748b;
  }
}

.feature-card__content {
  position: relative;
  z-index: 2;
}

.feature-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 99px;
  background: rgba(0, 0, 0, 0.05);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 24px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.feature-card__visual {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
}

/* Visual Elements Animations */
.matching-node {
  position: absolute;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-color), #60a5fa);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.3);
}

.node-1 {
  right: 15%;
  top: 30%;
  animation: float 6s ease-in-out infinite;
}

.node-2 {
  right: 25%;
  bottom: 25%;
  background: linear-gradient(135deg, #f43f5e, #fb7185);
  animation: float 8s ease-in-out infinite reverse;
}

.visual-chat {
  .chat-bubble {
    position: absolute;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
  }
  
  .bubble-1 {
    width: 120px;
    height: 80px;
    right: -20px;
    bottom: 40px;
    transform: rotate(-10deg);
  }
  
  .bubble-2 {
    width: 100px;
    height: 60px;
    right: 60px;
    bottom: 100px;
    transform: rotate(5deg);
  }
}

.cta-section {
  text-align: center;
  margin-top: 100px;
  
  h2 {
    font-size: 48px;
    font-weight: 800;
    margin-bottom: 16px;
    background: linear-gradient(120deg, var(--text-color), var(--accent-color));
    -webkit-background-clip: text;
    color: transparent;
  }
  
  p {
    font-size: 20px;
    color: var(--text-color-muted);
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(0, 20px); }
}

/* Dark Mode Overrides */
[data-theme='dark'] {
  .feature-card--large {
    background: linear-gradient(135deg, #1e293b, #0f172a);
    border: 1px solid rgba(255, 255, 255, 0.05);
    
    h3 {
      background: linear-gradient(120deg, #fff, #94a3b8);
      -webkit-background-clip: text;
    }
    
    p {
      color: #94a3b8;
    }
    
    .feature-tag {
      background: rgba(255, 255, 255, 0.1);
      color: #e2e8f0;
    }
  }
  
  .feature-card--light {
    background: #0f172a;
    border-color: rgba(255, 255, 255, 0.1);
    
    h3 {
      color: white;
    }
    
    p {
      color: #94a3b8;
    }
    
    .feature-tag {
      background: rgba(255, 255, 255, 0.1);
      color: #e2e8f0;
    }
  }
}

/* Responsive */
@media (max-width: 768px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
  
  .feature-card--large {
    grid-column: auto;
    min-height: auto;
    padding: 32px;
    
    h3 {
      font-size: 32px;
    }
  }
  
  .feature-card--medium {
    min-height: 300px;
    padding: 32px;
  }
}
</style>
