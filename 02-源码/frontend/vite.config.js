import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 通道 1：前端 5170，代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5170,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  }
})
