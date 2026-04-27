/** @type {import('prettier').Config} */
export default {
  // 每行最大列,超过换行
  printWidth: 120,
  // 使用制表符,而不是空格缩进
  useTabs: false,
  // 缩进
  tabWidth: 2,
  // 结尾不用分号
  semi: false,
  // 使用单引号
  singleQuote: true,
  // JSX 使用单引号
  jsxSingleQuote: true,
  // 箭头函数里面如果只有一个参数，则省略括号
  arrowParens: 'avoid',
  // 对象数组括号与文件之间添加空格
  bracketSpacing: true,
  // 尾随逗号
  trailingComma: 'none'
}
