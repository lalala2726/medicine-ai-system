import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import eslintConfigPrettier from 'eslint-config-prettier';

export default tseslint.config(
  // 忽略目录
  {
    ignores: [
      'dist',
      'node_modules',
      'public',
      'coverage',
      'src/.umi',
      'src/.umi-production',
      'src/.umi-test',
    ],
  },

  // 基础推荐规则
  js.configs.recommended,
  ...tseslint.configs.recommended,

  // React Hooks 规则
  {
    plugins: { 'react-hooks': reactHooks },
    rules: reactHooks.configs.recommended.rules,
  },

  // React Refresh 规则
  {
    plugins: { 'react-refresh': reactRefresh },
    rules: {
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },

  // 项目自定义规则
  {
    files: ['**/*.{ts,tsx}'],
    rules: {
      // 允许使用 any 类型（与原 Biome 配置一致）
      '@typescript-eslint/no-explicit-any': 'off',
      // 允许 namespace（兼容现有 API 类型定义）
      '@typescript-eslint/no-namespace': 'off',
      // 兼容已有 ts 注释用法
      '@typescript-eslint/ban-ts-comment': 'off',
      // 允许未使用变量以下划线开头
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      // 允许 require 导入
      '@typescript-eslint/no-require-imports': 'off',
      // 放宽 React Hooks 新增强约束，保持存量代码可通过
      'react-hooks/set-state-in-effect': 'off',
      'react-hooks/error-boundaries': 'off',
      'react-hooks/immutability': 'off',
    },
  },

  // Prettier 兼容（关闭与 Prettier 冲突的规则，必须放最后）
  eslintConfigPrettier,
);
