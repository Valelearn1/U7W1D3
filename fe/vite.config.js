import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: {
    // In sviluppo /api lo inoltra Vite al backend sulla 8080.
    // In produzione il proxy non esiste: serve VITE_API_URL.
    proxy: {
      '/api': { target: 'http://localhost:8080' },
    },
  },
})
