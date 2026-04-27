const fs = require('fs')
const path = require('path')

const filesToProcess = [
  'src/pages/Cart/index.tsx',
  'src/pages/AddressList/index.tsx',
  'src/pages/Home/components/Search/index.tsx',
  'src/pages/NotFound/index.tsx',
  'src/pages/PatientList/index.tsx',
  'src/pages/AfterSale/index.tsx',
  'src/pages/viewHistor/index.tsx',
  'src/pages/Me/components/WalletBill/index.tsx',
  'src/pages/Orders/index.tsx',
  'src/components/AddressSelector/index.tsx'
]

filesToProcess.forEach(relPath => {
  const fullPath = path.join('/Users/zhangchuang/webStormProjects/medicine', relPath)
  if (!fs.existsSync(fullPath)) return

  let content = fs.readFileSync(fullPath, 'utf8')

  // 1. Remove Empty from @nutui/nutui-react imports
  // Case A: { Empty, Loading } -> { Loading }
  content = content.replace(
    /import\s+\{([^}]*)(?:,\s*Empty|\s+Empty\s*,|\s+Empty\s*)([^}]*)\}\s+from\s+['"]@nutui\/nutui-react['"]/g,
    (match, p1, p2) => {
      const remaining = [p1, p2]
        .map(s => s.trim())
        .filter(Boolean)
        .map(s => s.replace(/^,|,$/g, '').trim())
        .filter(Boolean)
        .join(', ')
      if (remaining === '') {
        // Should completely remove the line if it becomes empty
        return ''
      }
      return `import { ${remaining} } from '@nutui/nutui-react'`
    }
  )

  // Fallback for some strange cases, just in case
  content = content.replace(/Empty\s*,\s*/g, (match, offset, string) => {
    // only if it's on an import line
    const before = string.substring(0, offset)
    if (before.lastIndexOf('import') > before.lastIndexOf('\n')) {
      return ''
    }
    return match
  })

  // 2. Add import Empty from '@/components/Empty' after the last import
  if (!content.includes("import Empty from '@/components/Empty'")) {
    const importRegex = /^import.*$/gm
    let match
    let lastImportIndex = 0
    while ((match = importRegex.exec(content)) !== null) {
      lastImportIndex = match.index + match[0].length
    }

    if (lastImportIndex > 0) {
      content =
        content.slice(0, lastImportIndex) + "\nimport Empty from '@/components/Empty'" + content.slice(lastImportIndex)
    } else {
      content = "import Empty from '@/components/Empty'\n" + content
    }
  }

  // Rewrite file
  fs.writeFileSync(fullPath, content)
  console.log(`Processed ${relPath}`)
})
