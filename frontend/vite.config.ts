import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const GATEWAY_URL = process.env.VITE_GATEWAY_URL || 'http://localhost:8080';
const WS_GATEWAY_URL = GATEWAY_URL.replace(/^http/, 'ws');

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
        target: GATEWAY_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/ws': {
        target: WS_GATEWAY_URL,
        ws: true,
        rewrite: (path) => path.replace(/^\/ws/, '/ws')
      }
    }
  }
})
