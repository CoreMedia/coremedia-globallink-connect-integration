#!/usr/bin/env node

/**
 * Dependency Override Management Script
 *
 * Analyzes pnpm dependency overrides and determines which ones are still
 * needed, which can be safely removed, and — with {@code --fix} — removes
 * unnecessary overrides automatically with full verification.
 *
 * Features:
 * - Cross-platform (works on Windows, macOS, Linux)
 * - No external dependencies (jq, js-yaml, …)
 * - CI-friendly exit codes (exit 1 when removable overrides are detected)
 * - Automatic fix mode with safety verification (audit + build)
 *
 * Override source resolution order:
 *   1. pnpm-workspace.yaml  (recommended, pnpm ≥ 9)
 *   2. package.json → pnpm.overrides  (legacy fallback)
 *
 * Usage:
 *   node ./scripts/manage-overrides.mjs             # check mode (CI-friendly)
 *   node ./scripts/manage-overrides.mjs --fix       # auto-remove + verify
 *   node ./scripts/manage-overrides.mjs --verbose   # include full dependency trees
 *
 * Exit codes:
 *   0 - no removable overrides found (or --fix completed successfully)
 *   1 - removable overrides detected (check mode) or fix failed
 */

import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
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
  dim: '\x1b[2m',
  bold: '\x1b[1m',
};

const verbose = process.argv.includes('--verbose');
const fix = process.argv.includes('--fix');

/**
 * Execute a command and return output, or null if it fails
 *
 * @param command - shell command to execute
 * @param options - optional execSync options
 * @returns trimmed stdout or null on failure
 */
function execCommand(command, options = {}) {
  try {
    return execSync(command, {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'],
      ...options,
    }).trim();
  } catch {
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
 *
 * @param filePath - absolute path to pnpm-workspace.yaml
 * @returns the overrides map or null if none found
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

      // Skip comment-only lines
      if (/^\s*#/.test(line)) continue;

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
 *
 * @param overrideKey - the override key, possibly containing a version selector
 * @returns the package name
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
 * Extract the version selector from a ranged override key
 * (e.g., "ajv@>=6.0.0 <6.14.0" -> ">=6.0.0 <6.14.0", "express" -> null)
 *
 * @param overrideKey - the override key
 * @returns the version selector string, or null for unranged overrides
 */
function extractOverrideSelector(overrideKey) {
  const pkgName = extractPackageName(overrideKey);
  const rest = overrideKey.slice(pkgName.length);
  if (rest.startsWith('@')) {
    return rest.slice(1).trim() || null;
  }
  return null;
}

/**
 * Check whether a parent's declared dependency range overlaps with
 * a ranged override selector (e.g., ">=6.0.0 <6.14.0").
 *
 * A parent is relevant when its declared range could resolve to
 * a version within the override's selector bounds.
 *
 * @param declaredRange - the parent's declared version range
 * @param overrideSelector - the override's version selector, or null
 * @returns true if the parent's range is relevant to this override
 */
function isParentRelevantToOverride(declaredRange, overrideSelector) {
  if (!overrideSelector) return true; // unranged override → all parents relevant

  const parentMin = extractMinVersion(declaredRange);
  if (!parentMin) return true; // cannot determine → assume relevant

  // Extract the bounds of the override selector (e.g., ">=6.0.0 <6.14.0")
  const selectorMin = extractMinVersion(overrideSelector);
  const upperMatch = overrideSelector.match(/<\s*(\d+\.\d+\.\d+)/);
  const selectorUpper = upperMatch ? upperMatch[1] : null;

  if (!selectorMin) return true; // can't parse → assume relevant

  // For caret ranges like ^X.Y.Z, compute the upper bound
  const parentUpper = computeRangeUpper(declaredRange);

  // Ranges overlap if: parentMin < selectorUpper AND parentUpper > selectorMin
  if (selectorUpper && parentUpper) {
    return (
      compareVersions(parentMin, selectorUpper) < 0 &&
      compareVersions(parentUpper, selectorMin) > 0
    );
  }

  // If we only have selectorMin, check if parentMin could be in range
  if (selectorUpper) {
    return compareVersions(parentMin, selectorUpper) < 0;
  }

  return true;
}

/**
 * Compute the exclusive upper bound of a common version range.
 *
 * @param range - a version range like "^6.12.5", "~6.12.5", "6.12.5"
 * @returns the exclusive upper bound version string, or null
 */
function computeRangeUpper(range) {
  const trimmed = range.trim();

  // Exact version: "6.12.5" → upper is "6.12.6" (practically, next patch)
  if (/^\d+\.\d+\.\d+$/.test(trimmed)) {
    const v = parseVersion(trimmed);
    return v ? `${v[0]}.${v[1]}.${v[2] + 1}` : null;
  }

  // Caret range: "^6.12.5" → upper is "7.0.0" (next major)
  const caretMatch = trimmed.match(/^\^(\d+)\.(\d+)\.(\d+)$/);
  if (caretMatch) {
    const major = parseInt(caretMatch[1]);
    if (major === 0) {
      const minor = parseInt(caretMatch[2]);
      if (minor === 0) return `0.0.${parseInt(caretMatch[3]) + 1}`;
      return `0.${minor + 1}.0`;
    }
    return `${major + 1}.0.0`;
  }

  // Tilde range: "~6.12.5" → upper is "6.13.0" (next minor)
  const tildeMatch = trimmed.match(/^~(\d+)\.(\d+)\.\d+$/);
  if (tildeMatch) {
    return `${parseInt(tildeMatch[1])}.${parseInt(tildeMatch[2]) + 1}.0`;
  }

  return null;
}

/**
 * Check whether a parent's declared range would naturally resolve to
 * a safe version (>= safeMinVersion) without the override.
 *
 * Three possible outcomes:
 * - 'always_safe': The parent's range minimum is already >= safe
 *   (all allowed versions are safe).
 * - 'includes_safe': The parent's range includes the safe version,
 *   so pnpm would resolve to it (or newer) naturally.
 * - 'unsafe': The parent's range does not include the safe version
 *   at all (e.g., the safe version is in a newer major).
 * - 'unknown': Cannot determine from the range.
 *
 * @param declaredRange - the parent's declared version range
 * @param safeMinVersion - the minimum safe version
 * @returns one of the outcome strings above
 */
function classifyParentSafety(declaredRange, safeMinVersion) {
  const parentMin = extractMinVersion(declaredRange);
  if (!parentMin) return 'unknown';

  // If parent's minimum is already >= safe, all versions in range are safe
  if (compareVersions(parentMin, safeMinVersion) >= 0) {
    return 'always_safe';
  }

  // Check if the safe version falls within the parent's range
  const result = satisfiesRange(safeMinVersion, declaredRange);
  if (result === true) {
    return 'includes_safe';
  }
  if (result === false) {
    return 'unsafe';
  }

  return 'unknown';
}

/**
 * Get unique package names from an overrides map
 *
 * @param overrides - the overrides map
 * @returns sorted array of unique package names
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
 *
 * @returns the pnpm version string
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
 *
 * @param title - section title text
 */
function printHeader(title) {
  console.log('');
  console.log(title);
  console.log('-'.repeat(title.length));
}

// ─── Version comparison utilities ──────────────────────────────────────────────

/**
 * Parse a version string into a numeric triple [major, minor, patch].
 *
 * @param v - version string like "6.14.0"
 * @returns tuple [major, minor, patch] or null if unparseable
 */
function parseVersion(v) {
  const match = v.match(/(\d+)\.(\d+)\.(\d+)/);
  if (!match) return null;
  return [parseInt(match[1]), parseInt(match[2]), parseInt(match[3])];
}

/**
 * Compare two version strings.
 *
 * @param a - first version
 * @param b - second version
 * @returns -1 if a < b, 0 if equal, 1 if a > b
 */
function compareVersions(a, b) {
  const pa = parseVersion(a);
  const pb = parseVersion(b);
  if (!pa || !pb) return 0;
  for (let i = 0; i < 3; i++) {
    if (pa[i] < pb[i]) return -1;
    if (pa[i] > pb[i]) return 1;
  }
  return 0;
}

/**
 * Extract the minimum version from a range specifier.
 *
 * Examples:
 *   "^6.14.0"     → "6.14.0"
 *   "~6.14.0"     → "6.14.0"
 *   ">=6.14.0"    → "6.14.0"
 *   "6.14.0"      → "6.14.0"
 *   "^6.14.0 <7"  → "6.14.0"
 *
 * @param range - a version range specifier
 * @returns the minimum version string or null
 */
function extractMinVersion(range) {
  const match = range.match(/(\d+\.\d+\.\d+)/);
  return match ? match[1] : null;
}

/**
 * Check whether a given version satisfies a dependency range specifier.
 *
 * This is a simplified implementation covering the most common patterns
 * used in package.json: caret (^), tilde (~), exact, and >=.  It does
 * NOT handle complex OR ranges, pre-release tags, or wildcard patterns
 * beyond these; for unrecognized patterns it returns 'unknown'.
 *
 * @param version - the concrete version to test (e.g., "6.14.0")
 * @param range - the range specifier (e.g., "^6.12.5")
 * @returns true if satisfied, false if not, 'unknown' if range is too complex
 */
function satisfiesRange(version, range) {
  const ver = parseVersion(version);
  if (!ver) return 'unknown';

  const trimmed = range.trim();

  // Exact version: "6.12.5"
  if (/^\d+\.\d+\.\d+$/.test(trimmed)) {
    return compareVersions(version, trimmed) === 0;
  }

  // Caret range: "^6.12.5"
  const caretMatch = trimmed.match(/^\^(\d+)\.(\d+)\.(\d+)$/);
  if (caretMatch) {
    const [, majorStr, minorStr, patchStr] = caretMatch;
    const major = parseInt(majorStr);
    const minor = parseInt(minorStr);
    const patch = parseInt(patchStr);

    // ^0.0.x → exact match on patch; ^0.y.z → same minor; ^X.y.z → same major
    if (major === 0 && minor === 0) {
      return ver[0] === 0 && ver[1] === 0 && ver[2] === patch;
    }
    if (major === 0) {
      return ver[0] === 0 && ver[1] === minor && ver[2] >= patch;
    }
    return ver[0] === major && compareVersions(version, `${major}.${minor}.${patch}`) >= 0;
  }

  // Tilde range: "~6.12.5"
  const tildeMatch = trimmed.match(/^~(\d+)\.(\d+)\.(\d+)$/);
  if (tildeMatch) {
    const major = parseInt(tildeMatch[1]);
    const minor = parseInt(tildeMatch[2]);
    const patch = parseInt(tildeMatch[3]);
    return ver[0] === major && ver[1] === minor && ver[2] >= patch;
  }

  // >= range: ">=6.12.5"  (no upper bound)
  const gteMatch = trimmed.match(/^>=\s*(\d+\.\d+\.\d+)$/);
  if (gteMatch) {
    return compareVersions(version, gteMatch[1]) >= 0;
  }

  return 'unknown';
}

// ─── Parent dependency analysis ────────────────────────────────────────────────

/**
 * Scan the pnpm store (node_modules/.pnpm) for packages that depend
 * on a given package and return their declared version ranges.
 *
 * @param packageName - the package to search for as a dependency
 * @returns a Map of "parent@version" → declared-range
 */
function findParentDeclarations(packageName) {
  const pnpmDir = join(process.cwd(), 'node_modules', '.pnpm');
  const result = new Map();

  let allDirs;
  try {
    allDirs = readdirSync(pnpmDir);
  } catch {
    return result;
  }

  for (const d of allDirs) {
    try {
      // Derive the package name from the store directory name
      // e.g., "schema-utils@3.3.0" → "schema-utils"
      // e.g., "@docusaurus+core@3.10.1_..." → "@docusaurus/core"
      const mainPkgName = d.replace(/@[\d].*$/, '').replace(/\+/g, '/');
      const pkgJsonPath = join(pnpmDir, d, 'node_modules', mainPkgName, 'package.json');
      if (!existsSync(pkgJsonPath)) continue;

      const pkg = JSON.parse(readFileSync(pkgJsonPath, 'utf8'));
      const allDeps = Object.assign(
        {},
        pkg.dependencies,
        pkg.peerDependencies,
        pkg.optionalDependencies,
      );
      if (allDeps[packageName]) {
        const key = `${pkg.name}@${pkg.version}`;
        if (!result.has(key)) {
          result.set(key, allDeps[packageName]);
        }
      }
    } catch {
      // Skip entries that don't parse
    }
  }

  return result;
}

/**
 * Extract resolved version(s) for a package from `pnpm why` output.
 *
 * @param packageName - the package name to look up
 * @returns array of resolved version strings
 */
function getResolvedVersions(packageName) {
  const output = execCommand(`pnpm why ${packageName} 2>/dev/null`);
  if (!output) return [];

  const versions = [];
  const regex = new RegExp(
    `^${packageName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}@(\\S+)`,
    'gm',
  );
  let match;
  while ((match = regex.exec(output)) !== null) {
    const ver = match[1].split(/\s/)[0];
    if (!versions.includes(ver)) {
      versions.push(ver);
    }
  }
  return versions;
}

// ─── Override analysis ─────────────────────────────────────────────────────────

/** Verdict constants for the removability analysis */
const Verdict = {
  REMOVABLE: 'LIKELY REMOVABLE',
  NEEDED: 'STILL NEEDED',
  NOT_IN_TREE: 'NOT IN TREE',
  REVIEW: 'REVIEW MANUALLY',
};

/**
 * Filter parent declarations to only those packages that are actually
 * referenced in the current dependency tree.
 *
 * The pnpm store may contain package.json files from previous installs
 * (e.g., old Docusaurus versions).  We filter by checking `pnpm why`
 * for each parent package name and comparing versions.
 *
 * @param parentDeclarations - Map of "parent@version" → declared-range
 * @returns filtered Map with only active parents
 */
function filterActiveParents(parentDeclarations) {
  const active = new Map();

  // Group by package name
  const byPkg = new Map();
  for (const [parentKey, range] of parentDeclarations) {
    const atIdx = parentKey.lastIndexOf('@');
    const name = parentKey.slice(0, atIdx);
    const version = parentKey.slice(atIdx + 1);
    if (!byPkg.has(name)) byPkg.set(name, []);
    byPkg.get(name).push({ key: parentKey, version, range });
  }

  for (const [name, entries] of byPkg) {
    // Check which versions of this parent are actually installed
    const installedVersions = getResolvedVersions(name);
    for (const entry of entries) {
      if (installedVersions.includes(entry.version)) {
        active.set(entry.key, entry.range);
      }
    }

    // If we couldn't determine installed versions, include all
    // (conservative: some might be stale but we won't miss any)
    if (installedVersions.length === 0) {
      for (const entry of entries) {
        active.set(entry.key, entry.range);
      }
    }
  }

  return active;
}

/**
 * Analyze a single override entry to determine if it is still needed.
 *
 * The analysis checks:
 *   1. Whether the package appears in the dependency tree at all.
 *   2. What version ranges parent packages declare for this dependency.
 *   3. Whether the safe (override target) version satisfies the parent
 *      ranges — meaning the parent would naturally resolve to a safe
 *      version without the override.
 *
 * @param overrideKey - the override key
 * @param overrideValue - the override target version/range
 * @returns the analysis result
 */
function analyzeOverride(overrideKey, overrideValue) {
  const packageName = extractPackageName(overrideKey);
  const overrideSelector = extractOverrideSelector(overrideKey);
  const resolvedVersions = getResolvedVersions(packageName);
  const parentDeclarations = findParentDeclarations(packageName);
  const safeMinVersion = extractMinVersion(overrideValue);

  const analysis = {
    overrideKey,
    overrideValue,
    packageName,
    resolvedVersions,
    parentDeclarations,
    overrideSelector,
    verdict: Verdict.REVIEW,
    reason: '',
  };

  // ── Case 1: Package is not in the dependency tree ──────────────────────
  if (resolvedVersions.length === 0) {
    analysis.verdict = Verdict.NOT_IN_TREE;
    analysis.reason = 'Package not found in the dependency tree — override has no effect.';
    return analysis;
  }

  if (!safeMinVersion) {
    analysis.verdict = Verdict.REVIEW;
    analysis.reason = `Cannot parse minimum version from override target "${overrideValue}".`;
    return analysis;
  }

  if (parentDeclarations.size === 0) {
    analysis.verdict = Verdict.REVIEW;
    analysis.reason = 'Could not find parent dependency declarations in node_modules.';
    return analysis;
  }

  // Filter parents to only those whose package is actually in the current
  // tree (ignore stale pnpm store entries from previous installs).
  const activeParents = filterActiveParents(parentDeclarations);

  if (activeParents.size === 0) {
    analysis.verdict = Verdict.REVIEW;
    analysis.reason =
      'Could not determine which parent packages are active in the current tree.';
    return analysis;
  }

  // For ranged overrides, filter to only parents whose declared range
  // intersects with the override selector range.
  const relevantParents = new Map();
  const skippedParents = [];
  for (const [parent, declaredRange] of activeParents) {
    if (isParentRelevantToOverride(declaredRange, overrideSelector)) {
      relevantParents.set(parent, declaredRange);
    } else {
      skippedParents.push({ parent, declaredRange });
    }
  }

  // Store relevant parents on analysis for display
  analysis.relevantParents = relevantParents;
  analysis.skippedParents = skippedParents;

  if (relevantParents.size === 0) {
    analysis.verdict = Verdict.REMOVABLE;
    analysis.reason =
      `No parent declares a version range that intersects with the ` +
      `override selector "${overrideSelector}". The override has no effect.`;
    return analysis;
  }

  const unsafe = [];
  const alwaysSafe = [];
  const includesSafe = [];
  const unknown = [];

  for (const [parent, declaredRange] of relevantParents) {
    const result = classifyParentSafety(declaredRange, safeMinVersion);
    switch (result) {
      case 'always_safe':
        alwaysSafe.push({ parent, declaredRange });
        break;
      case 'includes_safe':
        includesSafe.push({ parent, declaredRange });
        break;
      case 'unsafe':
        unsafe.push({ parent, declaredRange });
        break;
      default:
        unknown.push({ parent, declaredRange });
    }
  }

  if (unsafe.length > 0) {
    const details = unsafe
      .map(
        (u) =>
          `${u.parent} declares "${u.declaredRange}" ` +
          `(does not include safe version ${safeMinVersion})`,
      )
      .join('; ');
    analysis.verdict = Verdict.NEEDED;
    analysis.reason =
      `Override is still needed: ${details}. ` +
      `Without the override, a vulnerable version could be installed.`;
    return analysis;
  }

  if (unknown.length > 0 && alwaysSafe.length === 0 && includesSafe.length === 0) {
    const details = unknown.map((u) => `${u.parent}: "${u.declaredRange}"`).join('; ');
    analysis.verdict = Verdict.REVIEW;
    analysis.reason =
      `Cannot automatically determine if safe version ${safeMinVersion} ` +
      `satisfies all parent ranges: ${details}`;
    return analysis;
  }

  const safeCount = alwaysSafe.length + includesSafe.length;
  if (safeCount > 0 && unknown.length === 0) {
    const parts = [];
    if (alwaysSafe.length > 0) {
      parts.push(
        `${alwaysSafe.length} parent(s) already require >= ${safeMinVersion}`,
      );
    }
    if (includesSafe.length > 0) {
      parts.push(
        `${includesSafe.length} parent(s) allow versions >= ${safeMinVersion}`,
      );
    }
    analysis.verdict = Verdict.REMOVABLE;
    analysis.reason =
      `${parts.join(', ')}. ` +
      `The dependency would resolve to a safe version without this override.`;
    return analysis;
  }

  // Mix of safe and unknown
  const unknownDetails = unknown
    .map((u) => `${u.parent}: "${u.declaredRange}"`)
    .join('; ');
  analysis.verdict = Verdict.REVIEW;
  analysis.reason =
    `${safeCount} parent(s) safe, but ${unknown.length} could not ` +
    `be verified: ${unknownDetails}`;
  return analysis;
}

/**
 * Print override summary table
 *
 * @param overrides - the overrides map
 * @param packages - sorted array of package names
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

  packages.forEach((pkg) => {
    const overrideList = overridesByPackage.get(pkg) || [];
    overrideList.forEach(({ pattern, version }) => {
      console.log(`  ${pattern.padEnd(35)} → ${version}`);
    });
  });

  console.log('');
}

/**
 * Print the analysis summary table with verdicts.
 *
 * @param analyses - array of override analysis results
 */
function printAnalysisSummary(analyses) {
  console.log('');
  console.log('='.repeat(72));
  console.log('Override Removability Analysis');
  console.log('='.repeat(72));
  console.log('');

  const removable = analyses.filter((a) => a.verdict === Verdict.REMOVABLE);
  const needed = analyses.filter((a) => a.verdict === Verdict.NEEDED);
  const notInTree = analyses.filter((a) => a.verdict === Verdict.NOT_IN_TREE);
  const review = analyses.filter((a) => a.verdict === Verdict.REVIEW);

  // Print each analysis
  for (const analysis of analyses) {
    let icon;
    let color;
    switch (analysis.verdict) {
      case Verdict.REMOVABLE:
        icon = '✅';
        color = colors.green;
        break;
      case Verdict.NEEDED:
        icon = '⚠️ ';
        color = colors.yellow;
        break;
      case Verdict.NOT_IN_TREE:
        icon = '❌';
        color = colors.red;
        break;
      default:
        icon = '🔍';
        color = colors.blue;
    }

    console.log(
      `${icon} ${color}${analysis.verdict}${colors.reset}: ` +
        `${colors.cyan}${analysis.overrideKey}${colors.reset} → ${analysis.overrideValue}`,
    );

    // Show resolved versions
    if (analysis.resolvedVersions.length > 0) {
      console.log(`   Resolved: ${analysis.resolvedVersions.join(', ')}`);
    }

    // Show relevant parent declarations
    const relevantParents = analysis.relevantParents ?? filterActiveParents(analysis.parentDeclarations);
    const skippedCount = analysis.skippedParents?.length ?? 0;
    if (relevantParents.size > 0 && relevantParents.size <= 8) {
      for (const [parent, range] of relevantParents) {
        const safeMin = extractMinVersion(analysis.overrideValue);
        const safety = safeMin ? classifyParentSafety(range, safeMin) : 'unknown';
        const satIcon =
          safety === 'always_safe' ? '✓' :
          safety === 'includes_safe' ? '≈' :
          safety === 'unsafe' ? '✗' : '?';
        console.log(
          `   ${colors.dim}${satIcon} ${parent} declares: ${range}${colors.reset}`,
        );
      }
    } else if (relevantParents.size > 8) {
      console.log(
        `   ${colors.dim}${relevantParents.size} relevant parent package(s)${colors.reset}`,
      );
    }
    if (skippedCount > 0) {
      console.log(
        `   ${colors.dim}(${skippedCount} parent(s) skipped — their ranges do not intersect with the override selector)${colors.reset}`,
      );
    }

    console.log(`   ${colors.dim}→ ${analysis.reason}${colors.reset}`);
    console.log('');
  }

  // Print summary counts
  console.log('-'.repeat(72));
  console.log(
    `Summary: ${analyses.length} override(s) analyzed | ` +
      `${colors.green}${removable.length} likely removable${colors.reset} | ` +
      `${colors.yellow}${needed.length} still needed${colors.reset} | ` +
      `${colors.red}${notInTree.length} not in tree${colors.reset} | ` +
      `${colors.blue}${review.length} review manually${colors.reset}`,
  );

  if (removable.length > 0) {
    console.log('');
    console.log(
      `${colors.green}${colors.bold}Overrides that can likely be removed:${colors.reset}`,
    );
    removable.forEach((a) => {
      console.log(`  - ${a.overrideKey}`);
    });
    console.log('');
    console.log(
      `${colors.dim}To verify, remove each override from pnpm-workspace.yaml, run` +
        ` "pnpm install", then "pnpm build" to confirm the build still works.${colors.reset}`,
    );
  }

  if (notInTree.length > 0) {
    console.log('');
    console.log(
      `${colors.red}${colors.bold}Overrides for packages not in the tree (safe to remove):${colors.reset}`,
    );
    notInTree.forEach((a) => {
      console.log(`  - ${a.overrideKey}`);
    });
  }

  console.log('');
}

/**
 * Analyze dependency tree for a package (verbose mode only)
 *
 * @param packageName - the package name to analyze
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
        console.log(
          `${colors.yellow}⚠️  Vulnerabilities found - review output above${colors.reset}`,
        );
      }
    } else {
      console.log(`${colors.red}Error running audit: ${error.message}${colors.reset}`);
    }
  }
}

/**
 * Check for outdated packages
 *
 * @param packages - array of package names to check
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
  const relevantLines = lines.filter((line) =>
    packages.some((pkg) => line.includes(pkg)),
  );

  if (relevantLines.length === 0) {
    console.log('✅ No updates found for overridden packages');
  } else {
    relevantLines.forEach((line) => console.log(line));
  }
}

/**
 * Print review checklist (check mode only)
 */
function printReviewChecklist() {
  console.log('');
  console.log('='.repeat(60));
  console.log('Next Steps');
  console.log('='.repeat(60));
  console.log('');
  console.log(
    `Run ${colors.cyan}pnpm overrides:fix${colors.reset} to automatically ` +
      `remove safe overrides with verification.`,
  );
  console.log('');
  console.log('Or manually for each "LIKELY REMOVABLE" override:');
  console.log('  1. Remove the override from pnpm-workspace.yaml');
  console.log('  2. Run: pnpm install');
  console.log('  3. Run: pnpm audit   (verify no new vulnerabilities)');
  console.log('  4. Run: pnpm build   (verify build still passes)');
  console.log('  5. Commit the change if all checks pass');
  console.log('');
  console.log('For each "STILL NEEDED" override:');
  console.log('  Keep the override until the parent package updates');
  console.log('  its dependency declaration to a safe version range.');
  console.log('');
  console.log(
    `Tip: use ${colors.cyan}--verbose${colors.reset} flag to see full dependency trees.`,
  );
  console.log('');
  console.log('='.repeat(60));
  console.log('');
}

// ─── YAML manipulation ─────────────────────────────────────────────────────────

/**
 * Extract the override key from a YAML entry line.
 *
 * Supports both quoted keys ({@code 'ajv@>=6.0.0 <6.14.0': ^6.14.0})
 * and unquoted keys ({@code express: ^4.22.1}).
 *
 * @param line - a single YAML line from the overrides section
 * @returns the override key, or null if the line is not an entry
 */
function extractKeyFromYamlLine(line) {
  // Quoted key: '...' or "..."
  const quotedMatch = line.match(/^\s+(?:'([^']*)'|"([^"]*)")\s*:/);
  if (quotedMatch) return quotedMatch[1] ?? quotedMatch[2];

  // Unquoted key
  const unquotedMatch = line.match(/^\s+([\w@/.-][^:]*?)\s*:/);
  if (unquotedMatch) return unquotedMatch[1].trim();

  return null;
}

/**
 * Remove specific override entries from pnpm-workspace.yaml.
 *
 * This function handles:
 * - Removing the override entry line itself
 * - Removing comment lines immediately above the entry (e.g., CVE links)
 * - Removing the entire `overrides:` section if it becomes empty
 * - Preserving all other sections and formatting
 *
 * @param filePath - absolute path to pnpm-workspace.yaml
 * @param keysToRemove - array of override keys to remove
 * @returns object with removedCount and the original file content for rollback
 */
function removeOverridesFromYaml(filePath, keysToRemove) {
  const originalContent = readFileSync(filePath, 'utf8');
  const lines = originalContent.split('\n');
  const result = [];
  let inOverrides = false;
  let overrideHeaderIndex = -1;
  let removedCount = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Detect the `overrides:` top-level key
    if (/^overrides\s*:/.test(line)) {
      inOverrides = true;
      overrideHeaderIndex = result.length;
      result.push(line);
      continue;
    }

    if (inOverrides) {
      // Blank line — keep for now (cleaned up later if section becomes empty)
      if (line.trim() === '') {
        result.push(line);
        continue;
      }

      // An un-indented non-empty line starts a new top-level section
      if (/^\S/.test(line)) {
        inOverrides = false;
        result.push(line);
        continue;
      }

      // Comment line — keep for now, may be removed with next entry
      if (/^\s*#/.test(line)) {
        result.push(line);
        continue;
      }

      // Override entry — check if it should be removed
      const key = extractKeyFromYamlLine(line);
      if (key && keysToRemove.includes(key)) {
        // Remove any comment lines immediately preceding this entry
        while (
          result.length > 0 &&
          result.length > overrideHeaderIndex + 1 &&
          /^\s*#/.test(result[result.length - 1])
        ) {
          result.pop();
        }
        removedCount++;
        continue;
      }

      result.push(line);
    } else {
      result.push(line);
    }
  }

  // Check if the overrides section is now empty (only header + blanks/comments)
  if (overrideHeaderIndex >= 0) {
    let hasEntries = false;
    for (let i = overrideHeaderIndex + 1; i < result.length; i++) {
      const line = result[i];
      if (line.trim() === '') continue;
      if (/^\S/.test(line)) break; // next top-level section
      if (!/^\s*#/.test(line)) {
        hasEntries = true;
        break;
      }
    }

    if (!hasEntries) {
      // Find where the overrides section ends
      let sectionEnd = overrideHeaderIndex + 1;
      while (sectionEnd < result.length) {
        const line = result[sectionEnd];
        if (line.trim() !== '' && /^\S/.test(line)) break;
        sectionEnd++;
      }
      // Remove the entire section including trailing blank line
      result.splice(overrideHeaderIndex, sectionEnd - overrideHeaderIndex);
    }
  }

  writeFileSync(filePath, result.join('\n'));
  return { removedCount, originalContent };
}

/**
 * Restore pnpm-workspace.yaml to its previous state.
 *
 * @param filePath - absolute path to pnpm-workspace.yaml
 * @param originalContent - the content to restore
 */
function restoreYaml(filePath, originalContent) {
  writeFileSync(filePath, originalContent);
}

// ─── Fix mode ──────────────────────────────────────────────────────────────────

/**
 * Run a shell command with live output, returning true on success.
 *
 * @param label - human-readable label for the step
 * @param command - shell command to execute
 * @returns true if the command succeeded, false otherwise
 */
function runStep(label, command) {
  console.log(`\n${colors.cyan}▶ ${label}${colors.reset}`);
  console.log(`  ${colors.dim}$ ${command}${colors.reset}`);
  try {
    execSync(command, { stdio: 'inherit' });
    console.log(`  ${colors.green}✅ ${label} passed${colors.reset}`);
    return true;
  } catch {
    console.log(`  ${colors.red}❌ ${label} failed${colors.reset}`);
    return false;
  }
}

/**
 * Attempt to automatically remove overrides that analysis identified as
 * safe to remove.  The process is:
 *
 *   1. Remove all fixable overrides from pnpm-workspace.yaml
 *   2. Run `pnpm install` to update the lockfile
 *   3. Run `pnpm audit` to verify no new vulnerabilities
 *   4. Run `pnpm build` to verify the site still compiles
 *   5. If any step fails → revert to original YAML
 *
 * @param analyses - the analysis results from the check phase
 * @param workspaceYamlPath - path to pnpm-workspace.yaml
 * @returns true if fix was successful, false otherwise
 */
function attemptFix(analyses, workspaceYamlPath) {
  const fixable = analyses.filter(
    (a) => a.verdict === Verdict.REMOVABLE || a.verdict === Verdict.NOT_IN_TREE,
  );

  if (fixable.length === 0) {
    console.log(
      `\n${colors.green}✅ No overrides to fix — all overrides are still needed.${colors.reset}`,
    );
    return true;
  }

  const keysToRemove = fixable.map((a) => a.overrideKey);

  console.log('');
  console.log('='.repeat(60));
  console.log('Fix Mode: Removing Unnecessary Overrides');
  console.log('='.repeat(60));
  console.log('');
  console.log(`Will remove ${keysToRemove.length} override(s):`);
  keysToRemove.forEach((key) => {
    const analysis = fixable.find((a) => a.overrideKey === key);
    const label =
      analysis.verdict === Verdict.NOT_IN_TREE
        ? `${colors.red}NOT IN TREE${colors.reset}`
        : `${colors.green}LIKELY REMOVABLE${colors.reset}`;
    console.log(`  - ${key} (${label})`);
  });

  // Step 1: Remove overrides from YAML
  console.log(
    `\n${colors.cyan}▶ Removing overrides from ${workspaceYamlPath}${colors.reset}`,
  );
  const { removedCount, originalContent } = removeOverridesFromYaml(
    workspaceYamlPath,
    keysToRemove,
  );
  console.log(`  ${colors.green}✅ Removed ${removedCount} override(s)${colors.reset}`);

  // Step 2: pnpm install
  if (!runStep('pnpm install', 'pnpm install')) {
    console.log(
      `\n${colors.red}Reverting changes — pnpm install failed.${colors.reset}`,
    );
    restoreYaml(workspaceYamlPath, originalContent);
    console.log(`${colors.yellow}pnpm-workspace.yaml restored to original state.${colors.reset}`);
    return false;
  }

  // Step 3: pnpm audit
  if (!runStep('pnpm audit', 'pnpm audit --audit-level=moderate')) {
    console.log(
      `\n${colors.red}Reverting changes — security audit found vulnerabilities.${colors.reset}`,
    );
    restoreYaml(workspaceYamlPath, originalContent);
    // Re-install to restore original lockfile
    console.log(`${colors.dim}Restoring lockfile...${colors.reset}`);
    runStep('pnpm install (restore)', 'pnpm install');
    console.log(`${colors.yellow}pnpm-workspace.yaml restored to original state.${colors.reset}`);
    return false;
  }

  // Step 4: pnpm build
  if (!runStep('pnpm build', 'pnpm run build')) {
    console.log(
      `\n${colors.red}Reverting changes — build failed after removing overrides.${colors.reset}`,
    );
    restoreYaml(workspaceYamlPath, originalContent);
    // Re-install to restore original lockfile
    console.log(`${colors.dim}Restoring lockfile...${colors.reset}`);
    runStep('pnpm install (restore)', 'pnpm install');
    console.log(`${colors.yellow}pnpm-workspace.yaml restored to original state.${colors.reset}`);
    return false;
  }

  // All checks passed
  console.log('');
  console.log('='.repeat(60));
  console.log(
    `${colors.green}${colors.bold}Fix completed successfully!${colors.reset}`,
  );
  console.log('='.repeat(60));
  console.log('');
  console.log(`Removed ${removedCount} override(s) from pnpm-workspace.yaml:`);
  keysToRemove.forEach((key) => console.log(`  ${colors.green}✓${colors.reset} ${key}`));
  console.log('');
  console.log('Verification passed:');
  console.log('  ✅ pnpm install   — lockfile updated');
  console.log('  ✅ pnpm audit     — no new vulnerabilities');
  console.log('  ✅ pnpm build     — site compiles successfully');
  console.log('');

  const remaining = analyses.filter(
    (a) => a.verdict !== Verdict.REMOVABLE && a.verdict !== Verdict.NOT_IN_TREE,
  );
  if (remaining.length > 0) {
    const neededCount = remaining.filter((a) => a.verdict === Verdict.NEEDED).length;
    const reviewCount = remaining.filter((a) => a.verdict === Verdict.REVIEW).length;
    console.log(
      `Remaining overrides: ${neededCount} still needed, ${reviewCount} require manual review.`,
    );
  }

  console.log('');
  console.log(
    `${colors.dim}Don't forget to commit the updated pnpm-workspace.yaml ` +
      `and pnpm-lock.yaml.${colors.reset}`,
  );
  console.log('');

  return true;
}

/**
 * Main execution
 */
function main() {
  console.log('='.repeat(60));
  console.log(
    fix
      ? 'Dependency Override Fix Report'
      : 'Dependency Override Validation Report',
  );
  console.log('='.repeat(60));
  console.log(`Date: ${new Date().toISOString().split('T')[0]}`);
  console.log(`Mode: ${fix ? `${colors.yellow}fix${colors.reset}` : 'check'}`);
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
      `${colors.green}✅ No overrides found — nothing to check.${colors.reset}`,
    );
    process.exit(0);
  }

  console.log(`Override source: ${colors.cyan}${overrideSource}${colors.reset}`);

  // Get overridden packages
  const packages = getOverriddenPackages(overrides);

  console.log(
    `Found ${packages.length} overridden package(s): ${packages.join(', ')}`,
  );

  // Print override summary
  printOverrideSummary(overrides, packages);

  // 1. Analyze each override for removability
  printHeader('1. Override Removability Analysis');
  console.log('');
  console.log(`Analyzing ${Object.keys(overrides).length} override(s)...`);
  console.log('(checking parent dependency declarations in node_modules)');

  const analyses = Object.entries(overrides).map(([key, value]) =>
    analyzeOverride(key, value),
  );

  printAnalysisSummary(analyses);

  // Count fixable overrides for exit code
  const fixableCount = analyses.filter(
    (a) => a.verdict === Verdict.REMOVABLE || a.verdict === Verdict.NOT_IN_TREE,
  ).length;

  // ── Fix mode ─────────────────────────────────────────────────────────────
  if (fix) {
    if (overrideSource !== 'pnpm-workspace.yaml') {
      console.error(
        `${colors.red}Error: --fix only supports pnpm-workspace.yaml, ` +
          `not ${overrideSource}${colors.reset}`,
      );
      process.exit(1);
    }

    const success = attemptFix(analyses, workspaceYamlPath);
    process.exit(success ? 0 : 1);
  }

  // ── Check mode (default) ─────────────────────────────────────────────────
  // Run additional checks only in check mode (fix mode already runs them)

  // 2. Dependency trees (verbose only)
  if (verbose) {
    printHeader('2. Dependency Tree Analysis (verbose)');
    packages.forEach(analyzeDependencyTree);
  }

  // 3. Security audit
  printHeader(verbose ? '3. Security Audit' : '2. Security Audit');
  runSecurityAudit();

  // 4. Check for outdated packages
  printHeader(verbose ? '4. Outdated Packages Check' : '3. Outdated Packages Check');
  checkOutdatedPackages(packages);

  // 5. Print checklist (only when there are fixable items)
  if (fixableCount > 0) {
    printReviewChecklist();
  }

  // CI-friendly exit code: exit 1 if there are removable overrides
  if (fixableCount > 0) {
    console.log(
      `${colors.yellow}${fixableCount} override(s) can be removed. ` +
        `Run with --fix to auto-remove.${colors.reset}`,
    );
    process.exit(1);
  }
}

// Run the script
main();
