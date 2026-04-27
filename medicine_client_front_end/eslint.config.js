import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'

export default tseslint.config([
  {
    ignores: ['dist']
  },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs['recommended-latest'],
      reactRefresh.configs.vite
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser
    },
    rules: {
      'react/react-in-jsx-scope': 'off',
      //"no-console": "error", // 禁止使用 console
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_', // 允许以下划线开头的未使用参数
          varsIgnorePattern: '^_', // 允许以下划线开头的未使用变量
          ignoreRestSiblings: true // 允许解构赋值中的剩余参数
        }
      ],
      'no-debugger': 'error', // 禁止使用 debugger
      'no-var': 'error', // 要求使用 let 或 const 而不是 var
      '@typescript-eslint/no-namespace': 'off', // 允许使用命名空间
      '@typescript-eslint/no-explicit-any': 'off' // 允许使用 any 类型
    }
  }
])
