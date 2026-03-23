import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const configuredOrigin = String(env.VITE_PUBLIC_APP_ORIGIN || '').trim()
  let configuredHost = ''

  if (configuredOrigin) {
    try {
      configuredHost = new URL(configuredOrigin).hostname
    } catch {
      configuredHost = ''
    }
  }

  const allowedHosts = ['localhost', '127.0.0.1', '.trycloudflare.com']
  if (configuredHost) {
    allowedHosts.push(configuredHost)
  }

  return {
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      strictPort: true,
      // Allow localhost plus the configured stable public hostname for named Cloudflare tunnels.
      allowedHosts,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          secure: false,
        },
        '/uploads': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
          secure: false,
        },
      }
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    }
  }
})
