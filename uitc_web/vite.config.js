import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite dev server 通过代理转发到后端 http://localhost:8080，避免 CORS；
// SockJS 的 /ws-uit 既要 HTTP info 探测又要 WebSocket 升级，ws:true 都覆盖。
export default defineConfig({
  plugins: [vue()],
  // sockjs-client 的 UMD bundle 引用了浏览器没有的 global，注入 globalThis 兜底
  define: {
    global: 'globalThis'
  },
  server: {
    port: 5173,
    proxy: {
      '/user': 'http://localhost:8080',
      '/tasks': 'http://localhost:8080',
      '/socket': 'http://localhost:8080',
      '/ws-uit': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
