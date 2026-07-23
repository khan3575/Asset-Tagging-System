import {defineConfig} from 'vite'
import {resolve} from 'path'


export default defineConfig({
    server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      input: {
        main: resolve(import.meta.dirname, 'index.html'),
        login: resolve(import.meta.dirname, 'src/pages/login-page.html'),
      },
    },
  },
})

        