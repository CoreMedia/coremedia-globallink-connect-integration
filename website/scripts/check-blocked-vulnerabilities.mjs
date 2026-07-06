#!/usr/bin/env node

/**
 * Blocked vulnerability policy check.
 *
 * This script is a governance check for dependencies that are intentionally
 * held on a vulnerable line because no safe upgrade path exists yet.
 *
 * Exit codes:
 *   0 - all checked policies are still required
 *   1 - at least one policy appears removable and needs review
 *   2 - invalid arguments or configuration
 */

import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'

import {
  colors,
  compareVersions,
  parseVersion,
} from './lib/yaml-overrides.mjs'

function parseArgs(argv) {
  const args = {
    config: './scripts/blocked-vulnerabilities.json',
    dependency: null,
    blockedRange: null,
  }

  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i]
    switch (arg) {
      case '--config':
        args.config = argv[++i]
        break
      case '--dependency':
        args.dependency = argv[++i]
        break
      case '--blocked-range':
        args.blockedRange = argv[++i]
        break
      default:
        throw new Error(`Unknown argument: ${arg}`)
    }
  }

  if ((args.dependency && !args.blockedRange) || (!args.dependency && args.blockedRange)) {
    throw new Error('Use --dependency and --blocked-range together')
  }

  return args
}

function escapeRegex(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function findInstalledVersions(lockContent, dependencyName) {
  const versions = new Set()
  const escaped = escapeRegex(dependencyName)
  const regex = new RegExp(`^\\s{2}${escaped}@([^:\\s]+):`, 'gm')

  let match
  while ((match = regex.exec(lockContent)) !== null) {
    const version = match[1]
    if (parseVersion(version)) {
      versions.add(version)
    }
  }

  return [...versions].sort(compareVersions)
}

function parseBound(range, operator) {
  const escapedOperator = escapeRegex(operator)
  const regex = new RegExp(`${escapedOperator}\\s*(\\d+\\.\\d+\\.\\d+)`)
  return range.match(regex)?.[1] || null
}

function satisfiesRange(version, range) {
  const trimmed = range.trim()

  if (!parseVersion(version)) return false

  if (/^\d+\.\d+\.\d+$/.test(trimmed)) {
    return compareVersions(version, trimmed) === 0
  }

  const caretMatch = trimmed.match(/^\^(\d+\.\d+\.\d+)$/)
  if (caretMatch) {
    const min = caretMatch[1]
    const minParts = parseVersion(min)
    if (!minParts) return false

    const [major, minor, patch] = minParts
    if (major === 0 && minor === 0) {
      return compareVersions(version, min) >= 0 && compareVersions(version, `0.0.${patch + 1}`) < 0
    }
    if (major === 0) {
      return compareVersions(version, min) >= 0 && compareVersions(version, `0.${minor + 1}.0`) < 0
    }
    return compareVersions(version, min) >= 0 && compareVersions(version, `${major + 1}.0.0`) < 0
  }

  const tildeMatch = trimmed.match(/^~(\d+\.\d+\.\d+)$/)
  if (tildeMatch) {
    const min = tildeMatch[1]
    const minParts = parseVersion(min)
    if (!minParts) return false

    const [major, minor] = minParts
    return compareVersions(version, min) >= 0 && compareVersions(version, `${major}.${minor + 1}.0`) < 0
  }

  const gte = parseBound(trimmed, '>=')
  const gt = parseBound(trimmed, '>')
  const lte = parseBound(trimmed, '<=')
  const lt = parseBound(trimmed, '<')

  if (gte && compareVersions(version, gte) < 0) return false
  if (gt && compareVersions(version, gt) <= 0) return false
  if (lte && compareVersions(version, lte) > 0) return false
  if (lt && compareVersions(version, lt) >= 0) return false

  if (gte || gt || lte || lt) {
    return true
  }

  throw new Error(`Unsupported range format: ${range}`)
}

function loadPolicies(configPath, dependency, blockedRange) {
  if (dependency && blockedRange) {
    return [
      {
        name: dependency,
        blockedRange,
        ignoreGhsas: [],
        rationale: 'manual input',
      },
    ]
  }

  if (!existsSync(configPath)) {
    throw new Error(`Config file not found: ${configPath}`)
  }

  const raw = readFileSync(configPath, 'utf8')
  const parsed = JSON.parse(raw)
  const policies = parsed.blockedDependencies

  if (!Array.isArray(policies) || policies.length === 0) {
    throw new Error('blockedDependencies must be a non-empty array')
  }

  for (const policy of policies) {
    if (!policy.name || !policy.blockedRange) {
      throw new Error('Each policy needs name and blockedRange')
    }
  }

  return policies
}

function evaluatePolicy(policy, lockContent) {
  const versions = findInstalledVersions(lockContent, policy.name)
  const blockedVersions = versions.filter((version) =>
    satisfiesRange(version, policy.blockedRange),
  )

  const stillBlocked = blockedVersions.length > 0

  return {
    policy,
    versions,
    blockedVersions,
    stillBlocked,
  }
}

function printReport(results) {
  console.log('='.repeat(60))
  console.log('Blocked Vulnerability Policy Check')
  console.log('='.repeat(60))

  for (const result of results) {
    const { policy, versions, blockedVersions, stillBlocked } = result
    const status = stillBlocked
      ? `${colors.green}PASS${colors.reset}`
      : `${colors.red}FAIL${colors.reset}`

    console.log('')
    console.log(`${status} ${policy.name} (${policy.blockedRange})`)

    if (policy.rationale) {
      console.log(`  rationale: ${policy.rationale}`)
    }

    if (policy.ignoreGhsas?.length) {
      console.log(`  ignored GHSA(s): ${policy.ignoreGhsas.join(', ')}`)
    }

    console.log(
      `  installed versions: ${versions.length > 0 ? versions.join(', ') : '<none>'}`,
    )

    if (stillBlocked) {
      console.log(`  blocked versions still present: ${blockedVersions.join(', ')}`)
      continue
    }

    console.log('  no versions remain in the blocked range')
    console.log(
      '  action: review and remove related ignore rules in pnpm and Dependabot',
    )
  }
}

function main() {
  let args
  try {
    args = parseArgs(process.argv)
  } catch (error) {
    console.error(`${colors.red}Argument error: ${error.message}${colors.reset}`)
    process.exit(2)
  }

  const lockPath = join(process.cwd(), 'pnpm-lock.yaml')
  if (!existsSync(lockPath)) {
    console.error(`${colors.red}pnpm-lock.yaml not found${colors.reset}`)
    process.exit(2)
  }

  let policies
  try {
    policies = loadPolicies(args.config, args.dependency, args.blockedRange)
  } catch (error) {
    console.error(`${colors.red}Policy error: ${error.message}${colors.reset}`)
    process.exit(2)
  }

  const lockContent = readFileSync(lockPath, 'utf8')
  const results = policies.map((policy) => evaluatePolicy(policy, lockContent))

  printReport(results)

  const failed = results.some((result) => !result.stillBlocked)
  if (failed) {
    console.log('')
    console.log(
      `${colors.red}At least one blocked policy appears removable.${colors.reset}`,
    )
    process.exit(1)
  }

  console.log('')
  console.log(`${colors.green}All blocked policies are still required.${colors.reset}`)
  process.exit(0)
}

main()
