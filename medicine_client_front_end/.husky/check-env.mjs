#!/usr/bin/env node

const semver = v => v.replace(/^v/, '')

const nodeVersion = process.versions.node
const npmExecpath = process.env.npm_execpath || ''

const [major] = semver(nodeVersion).split('.')
if (Number(major) < 20) {
  console.error(`\n[ENV ERROR] Node ${nodeVersion} detected. Please use Node >= 20.\n`)
  process.exit(1)
}

if (!npmExecpath.includes('pnpm')) {
  console.error(`\n[ENV ERROR] Only pnpm is allowed. Use: pnpm i\n`)
  process.exit(1)
}
