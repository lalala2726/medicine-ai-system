import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  /** 当前模式下读取到的环境变量集合。 */
  const env = loadEnv(mode, process.cwd(), '')
  /** 业务服务反向代理目标地址。 */
  const apiProxyTarget = env.API_PROXY_TARGET?.trim() || 'http://localhost:8081'
  /** AI 服务反向代理目标地址。 */
  const aiAgentProxyTarget = env.AI_AGENT_PROXY_TARGET?.trim() || 'http://localhost:8000'
  /** Vite 开发服务器允许访问的自定义 Host 白名单。 */
  const allowedHosts = ['localhost']

  return {
    plugins: [react()],
    // 已移除 less 预处理配置，NutUI 使用 CSS 按需或全量样式
    resolve: {
      alias: {
        '@': '/src'
      }
    },
    server: {
      allowedHosts,
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, '')
        },
        '/ai_agent': {
          target: aiAgentProxyTarget,
          changeOrigin: true,
          ws: true,
          rewrite: path => path.replace(/^\/ai_agent/, '')
        }
      }
    }
  }
})
