import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/waveai/',
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/waveai/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../target/classes/static',
    emptyOutDir: true
  }
})
