import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@styles/main.scss'
import '@/styles/transitions.scss'
import { setupErrorHandler } from './utils/errorHandler'
import { useSystemStore } from './store/system'
import './utils/auth-debug' // 导入认证调试工具

const app = createApp(App)

// 确保先注册 Pinia，再注册 Router，这样路由守卫中使用 useAuthStore 时有可用的 Pinia 实例
setupErrorHandler(app)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 加载系统设置
const systemStore = useSystemStore()
systemStore.loadSettings()

app.mount('#app')
