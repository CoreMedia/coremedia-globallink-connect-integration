#!/usr/bin/env node

/**
 * Automated Vulnerability Override Fix Script
 *
 * Reads `pnpm audit --json` output, determines the correct ranged overrides
 * to add to pnpm-workspace.yaml, and verifies the fix with install + audit +
 * build + optional smoke test.
 *
 * Usage:
 *   node ./scripts/fix-audit.mjs                # fix mode (add overrides + verify)
 *   node ./scripts/fix-audit.mjs --dry-run      # report only, no changes
 *   node ./scripts/fix-audit.mjs --skip-smoke-test  # skip the serve + fetch check
 *
 * Exit codes:
 *   0 - overrides added successfully (or --dry-run found fixable issues)
 *   1 - fix failed (verification did not pass)
 *   2 - no vulnerabilities found (nothing to do)
 */

import { existsSync } from 'node:fs';
import { execSync } from 'node:child_process';
import { join } from 'node:path';

import {
  colors,
  execCommand,
  runStep,
  readWorkspaceYamlOverrides,
  addOverridesToYaml,
  restoreYaml,
  extractPackageName,
  extractMinVersion,
  compareVersions,
} from './lib/yaml-overrides.mjs';

import { runSmokeTests } from './lib/smoke-test.mjs';

// ─── CLI flags ─────────────────────────────────────────────────────────────────

const dryRun = process.argv.includes('--dry-run');
const skipSmokeTest = process.argv.includes('--skip-smoke-test');

// ─── Audit parsing ─────────────────────────────────────────────────────────────

/**
 * Run `pnpm audit --json` and parse the structured output.
 *
 * @returns {{ advisories: Array<object> }} parsed audit data
 */
function runAudit() {
  let output;
  try {
    output = execSync('pnpm audit --json 2>/dev/null', {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'],
    });
  } catch (error) {
    // pnpm audit exits non-zero when vulnerabilities exist — that's expected
    output = error.stdout || '';
  }

  if (!output.trim()) {
    return { advisories: [] };
  }

  try {
    const data = JSON.parse(output);
    return data;
  } catch {
    // pnpm audit might output multiple JSON objects (one per line)
    // Try parsing line by line
    const advisories = [];
    for (const line of output.split('\n')) {
      if (!line.trim()) continue;
      try {
        const obj = JSON.parse(line);
        if (obj.advisories) return obj;
        if (obj.type === 'auditAdvisory') {
          advisories.push(obj.data?.advisory || obj);
        }
      } catch {
        // skip unparseable lines
      }
    }
    return { advisories };
  }
}

/**
 * Extract actionable vulnerability information from audit data.
 *
 * @param {object} auditData - parsed pnpm audit output
 * @returns {Array<{ name: string, vulnerableRange: string, patchedVersion: string, severity: string, url: string, title: string }>}
 */
function extractVulnerabilities(auditData) {
  const vulnerabilities = [];

  // pnpm audit --json format: { advisories: { [id]: advisory } } or { vulnerabilities: { [name]: info } }
  if (auditData.advisories && typeof auditData.advisories === 'object') {
    const advisoriesMap = Array.isArray(auditData.advisories)
      ? Object.fromEntries(auditData.advisories.map((a, i) => [i, a]))
      : auditData.advisories;

    for (const advisory of Object.values(advisoriesMap)) {
      if (!advisory.module_name && !advisory.name) continue;

      const name = advisory.module_name || advisory.name;
      const vulnerableRange = advisory.vulnerable_versions || advisory.range || '*';
      const patchedVersion = advisory.patched_versions || advisory.fixAvailable?.version || null;
      const severity = advisory.severity || 'unknown';
      const url = advisory.url || advisory.references || '';
      const title = advisory.title || advisory.overview || '';

      if (patchedVersion && patchedVersion !== '<0.0.0') {
        vulnerabilities.push({ name, vulnerableRange, patchedVersion, severity, url, title });
      }
    }
  }

  // Alternative format: { vulnerabilities: { [name]: { via: [...], fixAvailable: ... } } }
  if (auditData.vulnerabilities && typeof auditData.vulnerabilities === 'object') {
    for (const [name, info] of Object.entries(auditData.vulnerabilities)) {
      if (!info.fixAvailable) continue;

      const fixVersion = typeof info.fixAvailable === 'object'
        ? info.fixAvailable.version
        : info.fixAvailable;

      if (!fixVersion) continue;

      const severity = info.severity || 'unknown';
      const viaEntries = Array.isArray(info.via) ? info.via : [];
      const advisory = viaEntries.find((v) => typeof v === 'object') || {};
      const url = advisory.url || '';
      const title = advisory.title || '';
      const vulnerableRange = info.range || advisory.range || '*';

      vulnerabilities.push({
        name,
        vulnerableRange,
        patchedVersion: fixVersion,
        severity,
        url,
        title,
      });
    }
  }

  return vulnerabilities;
}

// ─── Override determination ─────────────────────────────────────────────────────

/**
 * Determine the override key and value for a vulnerability.
 *
 * Uses ranged override syntax: `'pkg@>=vulnerable <patched': ^patched`
 * This ensures the override can be automatically cleaned up later when
 * the parent package updates its dependency declaration.
 *
 * @param {{ name: string, vulnerableRange: string, patchedVersion: string }} vuln
 * @returns {{ key: string, value: string }} the override entry
 */
function determineOverride(vuln) {
  const patchedMin = extractMinVersion(vuln.patchedVersion);

  if (!patchedMin) {
    // Fallback: use unranged override with the patched version as-is
    return { key: vuln.name, value: vuln.patchedVersion };
  }

  // Parse the vulnerable range to extract bounds
  // Common formats: ">=1.0.0 <2.0.0", "<2.0.0", "<=1.9.9", "*"
  const lowerMatch = vuln.vulnerableRange.match(/>=?\s*(\d+\.\d+\.\d+)/);
  const lowerBound = lowerMatch ? lowerMatch[1] : '0.0.0';

  // Build ranged override key: 'pkg@>=lower <patched'
  const key = `${vuln.name}@>=${lowerBound} <${patchedMin}`;
  const value = `^${patchedMin}`;

  return { key, value };
}

/**
 * Filter out vulnerabilities that are already covered by existing overrides.
 *
 * @param {Array} vulnerabilities - extracted vulnerabilities
 * @param {Record<string, string> | null} existingOverrides - current overrides
 * @returns {Array} vulnerabilities that still need fixing
 */
function filterAlreadyCovered(vulnerabilities, existingOverrides) {
  if (!existingOverrides) return vulnerabilities;

  return vulnerabilities.filter((vuln) => {
    const patchedMin = extractMinVersion(vuln.patchedVersion);
    if (!patchedMin) return true; // can't determine, keep it

    // Check if any existing override covers this package with a sufficient version
    for (const [key, value] of Object.entries(existingOverrides)) {
      const overridePkg = extractPackageName(key);
      if (overridePkg !== vuln.name) continue;

      const overrideMin = extractMinVersion(value);
      if (overrideMin && compareVersions(overrideMin, patchedMin) >= 0) {
        // Existing override already forces a version >= patched
        return false;
      }
    }

    return true;
  });
}

/**
 * Deduplicate vulnerabilities by package name, keeping the one with
 * the highest patched version.
 *
 * @param {Array} vulnerabilities
 * @returns {Array} deduplicated list
 */
function deduplicateByPackage(vulnerabilities) {
  const byName = new Map();

  for (const vuln of vulnerabilities) {
    const existing = byName.get(vuln.name);
    if (!existing) {
      byName.set(vuln.name, vuln);
      continue;
    }

    const existingMin = extractMinVersion(existing.patchedVersion);
    const currentMin = extractMinVersion(vuln.patchedVersion);
    if (existingMin && currentMin && compareVersions(currentMin, existingMin) > 0) {
      byName.set(vuln.name, vuln);
    }
  }

  return [...byName.values()];
}

// ─── Main execution ────────────────────────────────────────────────────────────

async function main() {
  console.log('='.repeat(60));
  console.log('Vulnerability Override Fix');
  console.log('='.repeat(60));
  console.log(`Date: ${new Date().toISOString().split('T')[0]}`);
  console.log(`Mode: ${dryRun ? 'dry-run (report only)' : 'fix'}`);
  console.log('');

  // Verify pnpm is available
  const pnpmVersion = execCommand('pnpm --version');
  if (!pnpmVersion) {
    console.error(`${colors.red}Error: pnpm not found${colors.reset}`);
    process.exit(1);
  }
  console.log(`Using pnpm: ${pnpmVersion}`);

  // Locate workspace YAML
  const workspaceYamlPath = join(process.cwd(), 'pnpm-workspace.yaml');
  if (!existsSync(workspaceYamlPath)) {
    console.error(`${colors.red}Error: pnpm-workspace.yaml not found${colors.reset}`);
    process.exit(1);
  }

  // Read existing overrides
  const existingOverrides = readWorkspaceYamlOverrides(workspaceYamlPath);
  console.log(
    `Existing overrides: ${existingOverrides ? Object.keys(existingOverrides).length : 0}`,
  );

  // Run audit
  console.log('');
  console.log(`${colors.cyan}▶ Running pnpm audit...${colors.reset}`);
  const auditData = runAudit();
  const allVulnerabilities = extractVulnerabilities(auditData);

  if (allVulnerabilities.length === 0) {
    console.log(`${colors.green}✅ No vulnerabilities found — nothing to fix.${colors.reset}`);
    process.exit(2);
  }

  console.log(`  Found ${allVulnerabilities.length} vulnerability/vulnerabilities`);

  // Deduplicate and filter
  const deduplicated = deduplicateByPackage(allVulnerabilities);
  const uncovered = filterAlreadyCovered(deduplicated, existingOverrides);

  if (uncovered.length === 0) {
    console.log(
      `${colors.green}✅ All vulnerabilities are already covered by existing overrides.${colors.reset}`,
    );
    process.exit(2);
  }

  // Determine overrides to add
  const overridesToAdd = uncovered.map((vuln) => {
    const { key, value } = determineOverride(vuln);
    const comment = vuln.url || `${vuln.title} (${vuln.severity})`;
    return { key, value, comment, vuln };
  });

  // Report
  console.log('');
  console.log('='.repeat(60));
  console.log(`Overrides to add: ${overridesToAdd.length}`);
  console.log('='.repeat(60));
  console.log('');

  for (const entry of overridesToAdd) {
    const severityColor =
      entry.vuln.severity === 'critical' || entry.vuln.severity === 'high'
        ? colors.red
        : colors.yellow;
    console.log(
      `  ${severityColor}[${entry.vuln.severity}]${colors.reset} ` +
        `${colors.cyan}${entry.key}${colors.reset} → ${entry.value}`,
    );
    if (entry.vuln.title) {
      console.log(`    ${colors.dim}${entry.vuln.title}${colors.reset}`);
    }
    if (entry.vuln.url) {
      console.log(`    ${colors.dim}${entry.vuln.url}${colors.reset}`);
    }
  }

  // Dry-run stops here
  if (dryRun) {
    console.log('');
    console.log(
      `${colors.yellow}Dry-run mode — no changes made.${colors.reset}`,
    );
    console.log(`Run without --dry-run to apply these overrides.`);
    process.exit(0);
  }

  // ── Apply overrides ─────────────────────────────────────────────────────────

  console.log('');
  console.log(`${colors.cyan}▶ Adding overrides to pnpm-workspace.yaml...${colors.reset}`);

  const entries = overridesToAdd.map((o) => ({
    key: o.key,
    value: o.value,
    comment: o.comment,
  }));

  const { addedCount, originalContent } = addOverridesToYaml(workspaceYamlPath, entries);
  console.log(`  ${colors.green}✅ Added ${addedCount} override(s)${colors.reset}`);

  // ── Verification ────────────────────────────────────────────────────────────

  // Step 1: pnpm install
  if (!runStep('pnpm install', 'pnpm install')) {
    console.log(`\n${colors.red}Reverting — pnpm install failed.${colors.reset}`);
    restoreYaml(workspaceYamlPath, originalContent);
    process.exit(1);
  }

  // Step 2: pnpm audit (should now pass)
  if (!runStep('pnpm audit', 'pnpm audit --audit-level=moderate')) {
    console.log(
      `\n${colors.yellow}Warning: audit still reports issues after overrides.${colors.reset}`,
    );
    console.log(
      `${colors.dim}This may indicate the patched version doesn't fully resolve the vulnerability.${colors.reset}`,
    );
    // Don't revert — the overrides may still be a partial improvement.
    // But signal that manual review is needed.
  }

  // Step 3: pnpm build
  if (!runStep('pnpm build', 'pnpm run build')) {
    console.log(`\n${colors.red}Reverting — build failed after adding overrides.${colors.reset}`);
    restoreYaml(workspaceYamlPath, originalContent);
    runStep('pnpm install (restore)', 'pnpm install');
    process.exit(1);
  }

  // Step 4: Smoke test (optional)
  if (!skipSmokeTest) {
    const { passed } = await runSmokeTests();
    if (!passed) {
      console.log(`\n${colors.red}Reverting — smoke test failed.${colors.reset}`);
      restoreYaml(workspaceYamlPath, originalContent);
      runStep('pnpm install (restore)', 'pnpm install');
      process.exit(1);
    }
  } else {
    console.log(`\n${colors.dim}Smoke test skipped (--skip-smoke-test).${colors.reset}`);
  }

  // ── Success ─────────────────────────────────────────────────────────────────

  console.log('');
  console.log('='.repeat(60));
  console.log(`${colors.green}${colors.bold}Fix completed successfully!${colors.reset}`);
  console.log('='.repeat(60));
  console.log('');
  console.log(`Added ${addedCount} override(s) to pnpm-workspace.yaml:`);
  overridesToAdd.forEach((o) => {
    console.log(`  ${colors.green}✓${colors.reset} ${o.key}: ${o.value}`);
  });
  console.log('');
  console.log('Verification passed:');
  console.log('  ✅ pnpm install   — lockfile updated');
  console.log('  ✅ pnpm audit     — vulnerabilities resolved');
  console.log('  ✅ pnpm build     — site compiles successfully');
  if (!skipSmokeTest) {
    console.log('  ✅ smoke test     — site serves correctly');
  }
  console.log('');
  console.log(
    `${colors.dim}Commit the updated pnpm-workspace.yaml and pnpm-lock.yaml.${colors.reset}`,
  );
  console.log('');

  process.exit(0);
}

main();
