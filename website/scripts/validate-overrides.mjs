#!/usr/bin/env node

/**
 * Dependency Override Validation Script (Node.js version)
 * This script checks if your pnpm overrides are still needed
 *
 * Advantages over shell script:
 * - Cross-platform (works on Windows, macOS, Linux)
 * - No external dependencies (jq, js-yaml, …)
 * - Better JSON parsing
 * - Easier to maintain for JavaScript/TypeScript developers
 *
 * Override source resolution order:
 *   1. pnpm-workspace.yaml  (recommended, pnpm ≥ 9)
 *   2. package.json → pnpm.overrides  (legacy fallback)
 */

import { readFileSync, existsSync } from 'node:fs';
import { execSync } from 'node:child_process';
import { join } from 'node:path';

// ANSI color codes
const colors = {
  reset: '\x1b[0m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
};

/**
 * Execute a command and return output, or null if it fails
 */
function execCommand(command, options = {}) {
  try {
    return execSync(command, {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'],
      ...options,
    }).trim();
  } catch (error) {
    return null;
  }
}

/**
 * Read the `overrides` map from pnpm-workspace.yaml without an external YAML
 * library.  The overrides block is a flat key→value section, so a small
 * purpose-built parser is sufficient.
 *
 * Supported key forms (as produced by pnpm):
 *   overrides:
 *     'ajv@>=8.0.0 <8.18.0': ^8.18.0
 *     baseline-browser-mapping: ^2.9.19
 */
function readWorkspaceYamlOverrides(filePath) {
  let content;
  try {
    content = readFileSync(filePath, 'utf8');
  } catch {
    return null;
  }

  const overrides = {};
  let inOverrides = false;

  for (const line of content.split('\n')) {
    // Detect the `overrides:` top-level key
    if (/^overrides\s*:/.test(line)) {
      inOverrides = true;
      continue;
    }

    if (inOverrides) {
      // Skip blank lines
      if (line.trim() === '') continue;

      // An un-indented non-empty line starts a new top-level section
      if (/^\S/.test(line)) {
        inOverrides = false;
        continue;
      }

      // Quoted key:  'key': value  or  "key": value
      const quotedMatch = line.match(/^\s+(?:'([^']*)'|"([^"]*)")?\s*:\s*(.+)$/);
      if (quotedMatch && (quotedMatch[1] !== undefined || quotedMatch[2] !== undefined)) {
        const key = quotedMatch[1] ?? quotedMatch[2];
        overrides[key] = quotedMatch[3].trim();
        continue;
      }

      // Unquoted key: word-chars, @, /, -  (no version range special chars)
      const unquotedMatch = line.match(/^\s+([\w@/.-][^:]*?)\s*:\s*(.+)$/);
      if (unquotedMatch) {
        overrides[unquotedMatch[1].trim()] = unquotedMatch[2].trim();
      }
    }
  }

  return Object.keys(overrides).length > 0 ? overrides : null;
}

/**
 * Extract package name from override key (e.g., "ajv@>=8.0.0 <8.18.0" -> "ajv")
 */
function extractPackageName(overrideKey) {
  // Handle scoped packages like "@babel/core"
  if (overrideKey.startsWith('@')) {
    const match = overrideKey.match(/^(@[^/]+\/[^@]+)/);
    return match ? match[1] : overrideKey;
  }
  // Handle regular packages
  return overrideKey.split('@')[0];
}

/**
 * Get unique package names from an overrides map
 */
function getOverriddenPackages(overrides) {
  if (!overrides || Object.keys(overrides).length === 0) {
    return [];
  }

  const packageNames = Object.keys(overrides)
    .map(extractPackageName)
    .filter(Boolean);

  // Remove duplicates and sort
  return [...new Set(packageNames)].sort();
}

/**
 * Check if pnpm is available
 */
function checkPnpm() {
  const version = execCommand('pnpm --version');
  if (!version) {
    console.error(`${colors.red}Error: pnpm is not installed or not in PATH${colors.reset}`);
    process.exit(1);
  }
  return version;
}

/**
 * Print section header
 */
function printHeader(title) {
  console.log('');
  console.log(title);
  console.log('-'.repeat(title.length));
}

/**
 * Analyze dependency tree for a package
 */
function analyzeDependencyTree(packageName) {
  console.log('');
  console.log(`📦 ${colors.cyan}${packageName}${colors.reset}`);
  console.log('-'.repeat(43));

  const output = execCommand(`pnpm why ${packageName}`);

  if (output) {
    console.log(output);
  } else {
    console.log(`  ℹ️  Package not found in dependency tree`);
  }

  console.log('');
}

/**
 * Run security audit
 */
function runSecurityAudit() {
  console.log('');
  console.log('Running security audit...');

  // pnpm audit returns non-zero exit code when vulnerabilities are found,
  // so we need to capture both stdout and stderr regardless of exit code
  try {
    const auditOutput = execSync('pnpm audit --audit-level=moderate 2>&1', {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'],
    }).trim();

    console.log(auditOutput);

    if (auditOutput.includes('No known vulnerabilities')) {
      console.log(`${colors.green}✅ No vulnerabilities found${colors.reset}`);
    }
  } catch (error) {
    // When vulnerabilities are found, execSync throws but output is still available
    const output = error.stdout?.trim() || error.stderr?.trim() || '';

    if (output) {
      console.log(output);

      if (output.includes('vulnerabilities found')) {
        console.log(`${colors.yellow}⚠️  Vulnerabilities found - review output above${colors.reset}`);
      }
    } else {
      console.log(`${colors.red}Error running audit: ${error.message}${colors.reset}`);
    }
  }
}

/**
 * Check for outdated packages
 */
function checkOutdatedPackages(packages) {
  console.log('');
  console.log('Checking for updates to overridden packages...');

  const outdatedOutput = execCommand('pnpm outdated 2>&1');

  if (!outdatedOutput) {
    console.log('✅ No updates found for overridden packages');
    return;
  }

  // Filter output to only show overridden packages
  const lines = outdatedOutput.split('\n');
  const relevantLines = lines.filter(line =>
    packages.some(pkg => line.includes(pkg))
  );

  if (relevantLines.length === 0) {
    console.log('✅ No updates found for overridden packages');
  } else {
    relevantLines.forEach(line => console.log(line));
  }
}

/**
 * Print override summary table
 */
function printOverrideSummary(overrides, packages) {
  console.log('');
  console.log('Current Overrides:');
  console.log('-'.repeat(60));

  // Group overrides by package
  const overridesByPackage = new Map();

  Object.entries(overrides).forEach(([key, value]) => {
    const pkgName = extractPackageName(key);
    if (!overridesByPackage.has(pkgName)) {
      overridesByPackage.set(pkgName, []);
    }
    overridesByPackage.get(pkgName).push({ pattern: key, version: value });
  });

  packages.forEach(pkg => {
    const overrideList = overridesByPackage.get(pkg) || [];
    overrideList.forEach(({ pattern, version }) => {
      console.log(`  ${pattern.padEnd(35)} → ${version}`);
    });
  });

  console.log('');
}

/**
 * Print review checklist
 */
function printReviewChecklist() {
  console.log('');
  console.log('='.repeat(60));
  console.log('Override Review Checklist');
  console.log('='.repeat(60));
  console.log('');
  console.log('For each override in pnpm-workspace.yaml, verify:');
  console.log('  1. Is the vulnerable version still in the dependency tree?');
  console.log('  2. Is the override version still the minimum secure version?');
  console.log('  3. Does the build pass with the override?');
  console.log('  4. Are there any peer dependency warnings?');
  console.log('');
  console.log('Next steps:');
  console.log('  - Review the dependency trees above');
  console.log('  - Check if overrides can be removed');
  console.log('  - Update DEPENDENCY_OVERRIDES.md with findings');
  console.log('  - Test build: pnpm run clear && pnpm run build');
  console.log('');
  console.log('='.repeat(60));
  console.log('');
}

/**
 * Main execution
 */
function main() {
  console.log('='.repeat(60));
  console.log('Dependency Override Validation Report');
  console.log('='.repeat(60));
  console.log(`Date: ${new Date().toISOString().split('T')[0]}`);
  console.log('');

  // Check pnpm is available
  const pnpmVersion = checkPnpm();
  console.log(`Using pnpm version: ${pnpmVersion}`);

  // Resolve overrides: pnpm-workspace.yaml first, then package.json fallback
  let overrides = null;
  let overrideSource = null;

  const workspaceYamlPath = join(process.cwd(), 'pnpm-workspace.yaml');
  if (existsSync(workspaceYamlPath)) {
    overrides = readWorkspaceYamlOverrides(workspaceYamlPath);
    if (overrides) overrideSource = 'pnpm-workspace.yaml';
  }

  if (!overrides) {
    // Fallback: pnpm.overrides in package.json (legacy)
    try {
      const packageJsonPath = join(process.cwd(), 'package.json');
      const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
      const legacyOverrides = packageJson?.pnpm?.overrides;
      if (legacyOverrides && Object.keys(legacyOverrides).length > 0) {
        overrides = legacyOverrides;
        overrideSource = 'package.json (pnpm.overrides)';
      }
    } catch {
      // package.json is optional for this fallback
    }
  }

  if (!overrides) {
    console.log(
      `${colors.yellow}Warning: No overrides found in pnpm-workspace.yaml or package.json${colors.reset}`
    );
    process.exit(0);
  }

  console.log(`Override source: ${colors.cyan}${overrideSource}${colors.reset}`);

  // Get overridden packages
  const packages = getOverriddenPackages(overrides);

  console.log(`Found ${packages.length} overridden package(s): ${packages.join(', ')}`);

  // Print override summary
  printOverrideSummary(overrides, packages);

  // 1. Analyze dependency trees
  printHeader('1. Dependency Tree Analysis');
  packages.forEach(analyzeDependencyTree);

  // 2. Security audit
  printHeader('2. Security Audit');
  runSecurityAudit();

  // 3. Check for outdated packages
  printHeader('3. Outdated Packages Check');
  checkOutdatedPackages(packages);

  // 4. Print checklist
  printReviewChecklist();
}

// Run the script
main();
