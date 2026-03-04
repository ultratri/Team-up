// 临时配置：强制重新加载模块
// 如果问题解决后可以删除此文件
import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    hmr: {
      overlay: true
    }
  },
  optimizeDeps: {
    force: true
  }
})
