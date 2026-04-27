import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')
const resolveBin = name =>
  path.join(repoRoot, 'node_modules', '.bin', process.platform === 'win32' ? `${name}.cmd` : name)

const run = (command, args, label) => {
  const result = spawnSync(command, args, { stdio: 'inherit' })

  if (result.status !== 0) {
    process.exit(result.status ?? 1)
  }

  if (label) {
    console.log(label)
  }
}

const read = (command, args) => {
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe']
  })

  if (result.status !== 0) {
    process.stderr.write(result.stderr ?? '')
    process.exit(result.status ?? 1)
  }

  return result.stdout
}

const unique = files => [...new Set(files)]

const cliFiles = process.argv.slice(2)
const isManualRun = cliFiles.length > 0
const stagedFiles = isManualRun
  ? cliFiles
  : read('git', ['diff', '--cached', '--name-only', '--diff-filter=ACMR', '-z']).split('\0').filter(Boolean)

if (stagedFiles.length === 0) {
  console.log('No staged files to process.')
  process.exit(0)
}

if (!isManualRun) {
  const partiallyStagedFiles = unique(
    read('git', ['diff', '--name-only', '-z', '--', ...stagedFiles])
      .split('\0')
      .filter(Boolean)
  )

  if (partiallyStagedFiles.length > 0) {
    console.error('Pre-commit auto-fix does not support partially staged files:')
    partiallyStagedFiles.forEach(file => console.error(`- ${file}`))
    console.error('Stage the full file or stash the unstaged changes, then try again.')
    process.exit(1)
  }
}

const eslintFiles = stagedFiles.filter(file => /\.(ts|tsx)$/i.test(file))

if (eslintFiles.length > 0) {
  console.log('Running ESLint auto-fix on staged files...')
  run(resolveBin('eslint'), ['--fix', ...eslintFiles])
}

console.log('Running Prettier on staged files...')
run(resolveBin('prettier'), ['--write', '--ignore-unknown', ...stagedFiles])

if (!isManualRun) {
  run('git', ['add', '--', ...stagedFiles], 'Restaged updated files.')
}
