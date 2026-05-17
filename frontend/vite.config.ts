import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0', // Listen on all interfaces
    port: 5173,
    watch: {
      usePolling: true, // Required for Windows hosts to detect file changes in Docker
    },
    proxy: {
      '/api': {
        target: 'http://gateway:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/ws': {
        target: 'ws://gateway:8080',
        ws: true,
        rewrite: (path) => path.replace(/^\/ws/, '/ws')
      }
    }
  }
})
