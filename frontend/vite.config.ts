import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import Icons from 'unplugin-icons/vite'
import viteCompression from 'vite-plugin-compression'

// https://vite.dev/config/
export default defineConfig(({ command }) => {
  const isBuild = command === 'build'

  return {
    plugins: [
      vue(),
      ...(isBuild
        ? [
            // Gzip压缩
            viteCompression({
              verbose: true,
              disable: false,
              threshold: 10240,
              algorithm: 'gzip',
              ext: '.gz',
            }),
          ]
        : []),
      // 自动导入Vue、Vue Router、Pinia等API
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/auto-imports.d.ts',
      }),
      // 自动导入组件
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/components.d.ts',
      }),
      // 图标自动导入
      Icons({
        autoInstall: true,
      }),
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
        '@api': resolve(__dirname, 'src/api'),
        '@components': resolve(__dirname, 'src/components'),
        '@views': resolve(__dirname, 'src/views'),
        '@store': resolve(__dirname, 'src/store'),
        '@router': resolve(__dirname, 'src/router'),
        '@utils': resolve(__dirname, 'src/utils'),
        '@composables': resolve(__dirname, 'src/composables'),
        '@types': resolve(__dirname, 'src/types'),
        '@assets': resolve(__dirname, 'src/assets'),
        '@styles': resolve(__dirname, 'src/assets/styles'),
      },
    },
    server: {
      port: 3000,
      host: '0.0.0.0',
      open: false,
      hmr: {
        overlay: true,
      },
      // 预热常用文件 - 扩展预热列表
      warmup: {
        clientFiles: [
          './src/layouts/AppLayout.vue',
          './src/views/auth/Login.vue',
          './src/views/project/ProjectSquare.vue',
          './src/views/ecosystem/EcosystemHub.vue',
          './src/views/competition/CompetitionList.vue',
          './src/components/common/**/*.vue',
          './src/store/**/*.ts',
          './src/router/index.ts',
        ],
      },
      proxy: {
        // 代理API请求到后端服务
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          // 不要重写路径，保持 /api 前缀
        },
        // 代理认证请求到后端服务
        '/auth': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        // 代理WebSocket连接到实时通信服务
        '/socket.io': {
          target: 'http://localhost:4000',
          changeOrigin: true,
          ws: true,
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          // 更细粒度的代码分割
          manualChunks: (id) => {
            // 第三方库分组
            if (id.includes('node_modules')) {
              if (id.includes('element-plus')) {
                return 'element-plus'
              }
              if (id.includes('vue') || id.includes('pinia') || id.includes('@vue')) {
                return 'vue-vendor'
              }
              if (id.includes('echarts') || id.includes('d3')) {
                return 'charts'
              }
              if (id.includes('axios') || id.includes('socket.io')) {
                return 'utils'
              }
              if (id.includes('@tiptap') || id.includes('lowlight')) {
                return 'editor'
              }
              // 其他第三方库
              return 'vendor'
            }
            
            // 按功能模块分组
            if (id.includes('/src/views/team/')) {
              return 'team-module'
            }
            if (id.includes('/src/views/project/')) {
              return 'project-module'
            }
            if (id.includes('/src/views/auth/')) {
              return 'auth-module'
            }
            if (id.includes('/src/components/')) {
              return 'components'
            }
          },
          // 为每个chunk生成有意义的文件名
          chunkFileNames: 'js/[name]-[hash].js',
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name?.split('.')
            const ext = info?.[info.length - 1]
            if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(assetInfo.name || '')) {
              return 'images/[name]-[hash][extname]'
            }
            if (/\.(woff2?|eot|ttf|otf)$/i.test(assetInfo.name || '')) {
              return 'fonts/[name]-[hash][extname]'
            }
            if (ext === 'css') {
              return 'css/[name]-[hash][extname]'
            }
            return 'assets/[name]-[hash][extname]'
          },
        },
      },
    },
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'axios',
        'element-plus',
        '@element-plus/icons-vue',
        'echarts',
        'vue-echarts',
        'd3',
        'socket.io-client',
        'gsap',
        '@vueuse/core',
        '@tiptap/vue-3',
        '@tiptap/starter-kit',
        'lowlight',
      ],
      // 移除 force: true，让 Vite 使用缓存
    },
    // 添加缓存配置
    cacheDir: 'node_modules/.vite',
  }
})
