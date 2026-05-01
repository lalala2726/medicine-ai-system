import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { defineConfig } from 'vite';

/** 开发服务监听地址，允许同一局域网设备访问。 */
const LAN_ACCESS_HOST = '0.0.0.0';
/** Vite 开发服务允许访问的主机列表。 */
const ALLOWED_HOSTS = ['medicine-client.zhangchuangla.cn', 'medicine-admin.zhangchuangla.cn'];

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    host: LAN_ACCESS_HOST,
    port: 3000,
    allowedHosts: ALLOWED_HOSTS,
    open: true,
    proxy: {
      '/ai_api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        /**
         * 重写 AI 服务代理路径。
         * @param path 当前请求路径。
         * @returns 去掉 `/ai_api` 前缀后的目标路径。
         */
        rewrite: (path) => path.replace(/^\/ai_api/, ''),
      },
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        timeout: 120000,
        /**
         * 重写后端服务代理路径。
         * @param path 当前请求路径。
         * @returns 去掉 `/api` 前缀后的目标路径。
         */
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  preview: {
    host: LAN_ACCESS_HOST,
    port: 8000,
    allowedHosts: ALLOWED_HOSTS,
  },
  css: {
    modules: {
      localsConvention: 'camelCaseOnly',
    },
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          antd: ['antd', '@ant-design/icons'],
          proComponents: ['@ant-design/pro-components'],
        },
      },
    },
  },
});
